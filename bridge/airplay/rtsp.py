"""Low-level RTSP protocol handler for AirPlay 1 communication.

Handles:
- Socket connection management
- RTSP message serialization/deserialization
- CSeq tracking
- Header parsing
"""

from __future__ import annotations

import logging
import socket
import ssl
import time
from typing import Optional

logger = logging.getLogger("kast.airplay.rtsp")


class RTSPMessage:
    """Represents an RTSP request or response."""

    def __init__(
        self,
        method: Optional[str] = None,
        uri: Optional[str] = None,
        status_code: Optional[int] = None,
        reason: Optional[str] = None,
        headers: Optional[dict[str, str]] = None,
        body: Optional[str] = None,
    ):
        self.method = method
        self.uri = uri
        self.status_code = status_code
        self.reason = reason
        self.headers = headers or {}
        self.body = body

    @property
    def is_request(self) -> bool:
        return self.method is not None

    @property
    def is_response(self) -> bool:
        return self.status_code is not None

    @property
    def cseq(self) -> Optional[int]:
        val = self.headers.get("CSeq")
        return int(val) if val else None

    def serialize(self) -> bytes:
        """Serialize to RTSP wire format."""
        lines = []

        if self.is_request:
            lines.append(f"{self.method} {self.uri} RTSP/1.0")
        else:
            lines.append(f"RTSP/1.0 {self.status_code} {self.reason}")

        for key, value in self.headers.items():
            lines.append(f"{key}: {value}")

        if self.body:
            lines.append(f"Content-Length: {len(self.body)}")
            lines.append("")
            lines.append(self.body)
        else:
            lines.append("")

        raw = "\r\n".join(lines)
        logger.debug("RTSP TX:\n%s", raw)
        return raw.encode("utf-8")

    _VALID_METHODS = frozenset({
        "OPTIONS", "ANNOUNCE", "SETUP", "RECORD", "TEARDOWN",
        "DESCRIBE", "PLAY", "PAUSE", "GET_PARAMETER", "SET_PARAMETER",
    })

    @classmethod
    def parse(cls, data: bytes) -> Optional["RTSPMessage"]:
        """Parse raw bytes into an RTSPMessage.

        Returns None for data that is not a valid RTSP request or response.
        """
        try:
            text = data.decode("utf-8", errors="replace")
            logger.debug("RTSP RX:\n%s", text[:2000])

            # Split headers and body
            parts = text.split("\r\n\r\n", 1)
            header_section = parts[0]
            body = parts[1] if len(parts) > 1 else None

            lines = header_section.split("\r\n")
            if not lines:
                return None

            # Parse status line
            first_line = lines[0]
            headers: dict[str, str] = {}

            if first_line.startswith("RTSP/"):
                # Response: "RTSP/1.0 <status_code> <reason>"
                parts = first_line.split(" ", 2)
                if len(parts) < 2:
                    return None
                try:
                    status_code = int(parts[1])
                except ValueError:
                    return None
                reason = parts[2] if len(parts) > 2 else ""

                for line in lines[1:]:
                    if ":" in line:
                        key, value = line.split(":", 1)
                        headers[key.strip()] = value.strip()

                return cls(
                    status_code=status_code,
                    reason=reason,
                    headers=headers,
                    body=body,
                )
            else:
                # Request: "<METHOD> <uri> RTSP/1.0"
                parts = first_line.split(" ")
                if len(parts) < 3:
                    return None
                method = parts[0]
                uri = parts[1]

                # Validate: method must be a known RTSP method
                if method not in cls._VALID_METHODS:
                    return None
                # Validate: last token must be RTSP version
                if parts[2] != "RTSP/1.0":
                    return None

                for line in lines[1:]:
                    if ":" in line:
                        key, value = line.split(":", 1)
                        headers[key.strip()] = value.strip()

                return cls(
                    method=method,
                    uri=uri,
                    headers=headers,
                    body=body,
                )
        except Exception as e:
            logger.error("RTSP parse error: %s", e)
            return None


class RTSPClient:
    """Low-level RTSP client for AirPlay communication."""

    def __init__(
        self,
        host: str,
        port: int = 7000,
        timeout: float = 10.0,
        use_ssl: bool = False,
    ):
        self.host = host
        self.port = port
        self.timeout = timeout
        self.use_ssl = use_ssl
        self._cseq = 0
        self._socket: Optional[socket.socket] = None
        self._connected = False

    @property
    def cseq(self) -> int:
        self._cseq += 1
        return self._cseq

    def connect(self) -> bool:
        """Establish TCP connection to the RTSP server."""
        try:
            raw_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            raw_sock.settimeout(self.timeout)

            if self.use_ssl:
                ctx = ssl.create_default_context()
                ctx.check_hostname = False
                ctx.verify_mode = ssl.CERT_NONE
                self._socket = ctx.wrap_socket(raw_sock, server_hostname=self.host)
            else:
                self._socket = raw_sock

            self._socket.connect((self.host, self.port))
            self._connected = True
            logger.info("Connected to %s:%d (ssl=%s)", self.host, self.port, self.use_ssl)
            return True
        except Exception as e:
            logger.error("Connection failed to %s:%d: %s", self.host, self.port, e)
            self._connected = False
            return False

    def disconnect(self):
        """Close the RTSP connection."""
        if self._socket:
            try:
                self._socket.close()
            except Exception:
                pass
            self._socket = None
        self._connected = False

    def send_request(
        self,
        method: str,
        uri: str = "*",
        headers: Optional[dict[str, str]] = None,
        body: Optional[str] = None,
        timeout: Optional[float] = None,
    ) -> Optional[RTSPMessage]:
        """Send an RTSP request and wait for the response."""
        if not self._connected or not self._socket:
            logger.error("Not connected")
            return None

        all_headers = {
            "CSeq": str(self.cseq),
            "User-Agent": "Kast/1.0 AirPlay/1.0",
            "Connection": "keep-alive",
        }
        if headers:
            all_headers.update(headers)

        msg = RTSPMessage(
            method=method,
            uri=uri,
            headers=all_headers,
            body=body,
        )

        try:
            self._socket.sendall(msg.serialize())
        except Exception as e:
            logger.error("Send failed: %s", e)
            self._connected = False
            return None

        # Read response
        return self._read_response(timeout or self.timeout)

    def _read_response(self, timeout: float) -> Optional[RTSPMessage]:
        """Read a complete RTSP response from the socket."""
        if not self._socket:
            return None

        self._socket.settimeout(timeout)
        buffer = b""
        header_end = False
        content_length = 0

        try:
            # Read until we have complete headers
            while not header_end:
                chunk = self._socket.recv(4096)
                if not chunk:
                    logger.warning("Connection closed by server")
                    self._connected = False
                    return None
                buffer += chunk
                if b"\r\n\r\n" in buffer:
                    header_end = True
                    # Extract Content-Length
                    header_text = buffer.split(b"\r\n\r\n")[0].decode("utf-8", errors="replace")
                    for line in header_text.split("\r\n"):
                        if line.lower().startswith("content-length:"):
                            content_length = int(line.split(":", 1)[1].strip())
                            break

            # Read body if present
            body_start = buffer.index(b"\r\n\r\n") + 4
            body_bytes = buffer[body_start:]
            while len(body_bytes) < content_length:
                chunk = self._socket.recv(4096)
                if not chunk:
                    break
                body_bytes += chunk

            return RTSPMessage.parse(buffer[:body_start] + body_bytes)

        except socket.timeout:
            logger.warning("RTSP read timeout")
            return None
        except Exception as e:
            logger.error("RTSP read error: %s", e)
            return None

    def send_interleaved_data(self, data: bytes) -> bool:
        """Send raw data (e.g., RTP packets) over the RTSP connection."""
        if not self._connected or not self._socket:
            return False
        try:
            self._socket.sendall(data)
            return True
        except Exception as e:
            logger.error("Interleaved data send failed: %s", e)
            return False

    @property
    def is_connected(self) -> bool:
        return self._connected
