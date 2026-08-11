"""AirPlay 1 client for Apple TV 3 (A1469).

Implements the RTSP handshake:
  OPTIONS → ANNOUNCE → SETUP → RECORD → (streaming) → TEARDOWN

References:
- AirPlay 1 protocol (reverse-engineered)
- Apple TV 3,2 (A1469) — AirPlay 1 only, no FairPlay for basic HTTP streams
"""

from __future__ import annotations

import logging
import random
import socket
import subprocess
import time
from typing import Optional

from .models import DeviceInfo, DeviceType, PlaybackSession, SessionState
from .rtsp import RTSPClient, RTSPMessage

logger = logging.getLogger("kast.airplay.client")

# AirPlay 1 session ID format
AIRPLAY_SESSION_ID = "DEADBEEF"
AIRPLAY_TIMING_PORT = 0  # Let server choose
AIRPLAY_CONTROL_PORT = 0


def _generate_session_id() -> str:
    """Generate a random AirPlay session ID."""
    return f"{random.randint(0, 0xFFFFFFFF):08X}"


def _build_sdp(
    url: str,
    title: str = "Kast",
    session_id: str = AIRPLAY_SESSION_ID,
) -> str:
    """Build SDP (Session Description Protocol) for ANNOUNCE.

    For AirPlay 1, the SDP describes the media that will be streamed.
    The 'a=rtpmap' lines describe RTP parameters.
    For HTTP URL relay, we describe the media format with port 0
    (actual port will be set in SETUP).
    """
    from urllib.parse import urlparse
    parsed = urlparse(url)
    address = parsed.hostname or "0.0.0.0"

    sdp = (
        "v=0\r\n"
        f"o=Kast 0 0 IN IP4 {address}\r\n"
        f"s={title}\r\n"
        "i=Kast AirPlay Stream\r\n"
        "t=0 0\r\n"
        f"a=x-apple-session-id:{session_id}\r\n"
        "m=audio 0 RTP/AVP 96\r\n"
        "c=IN IP4 0.0.0.0\r\n"
        "a=rtpmap:96 MPEG4-GENERIC/44100/2\r\n"
        "a=fmtp:96 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3\r\n"
        "a=control:rtsp://0.0.0.0/audio\r\n"
    )
    return sdp


class AirPlayClient:
    """High-level AirPlay 1 client for communicating with Apple TV 3."""

    def __init__(self):
        self._sessions: dict[str, PlaybackSession] = {}
        self._rtsp_clients: dict[str, RTSPClient] = {}

    def discover(self, ip: str, port: int = 7000, timeout: float = 3.0) -> Optional[DeviceInfo]:
        """Probe an IP for AirPlay server-info."""
        try:
            client = RTSPClient(ip, port, timeout=timeout)
            if not client.connect():
                return None

            resp = client.send_request("OPTIONS", f"rtsp://{ip}:{port}", timeout=timeout)
            client.disconnect()

            if resp is None:
                # Try HTTP server-info as fallback
                return self._discover_http(ip, port, timeout)

            # Parse server info from headers
            server = resp.headers.get("Server", "")
            if "AirPlay" not in server and "Apple" not in server:
                # Still might be an Apple TV — check via HTTP
                return self._discover_http(ip, port, timeout)

            return DeviceInfo(
                name=server,
                ip=ip,
                port=port,
                model="AppleTV",
                device_type=DeviceType.APPLE_TV,
                protocol_version="1.0",
                airplay_version=server,
            )
        except Exception as e:
            logger.debug("Discovery failed for %s:%d: %s", ip, port, e)
            return self._discover_http(ip, port, timeout)

    def _discover_http(self, ip: str, port: int, timeout: float) -> Optional[DeviceInfo]:
        """Fallback: discover via HTTP server-info endpoint."""
        try:
            import urllib.request
            import urllib.error

            url = f"http://{ip}:{port}/server-info"
            req = urllib.request.Request(url)
            resp = urllib.request.urlopen(req, timeout=timeout)
            body = resp.read().decode("utf-8", errors="replace")

            # Parse key-value response
            info = {}
            for line in body.strip().split("\n"):
                if ":" in line:
                    key, value = line.split(":", 1)
                    info[key.strip()] = value.strip()

            model = info.get("model", "AppleTV")
            srcvers = info.get("srcvers", "")
            protovers = info.get("protovers", "")

            return DeviceInfo(
                name=f"Apple TV ({model})",
                ip=ip,
                port=port,
                model=model,
                device_type=DeviceType.APPLE_TV,
                protocol_version=protovers,
                airplay_version=srcvers,
            )
        except Exception as e:
            logger.debug("HTTP discovery failed for %s:%d: %s", ip, port, e)
            return None

    def play(
        self,
        device_ip: str,
        url: str,
        device_port: int = 7000,
        title: str = "Kast",
    ) -> PlaybackSession:
        """Initiate AirPlay playback on an Apple TV.

        Performs: OPTIONS → ANNOUNCE → SETUP → RECORD
        Then starts ffmpeg to stream the media.
        """
        session_id = _generate_session_id()
        session = PlaybackSession(
            device_ip=device_ip,
            device_port=device_port,
            state=SessionState.CONNECTING,
            current_url=url,
            rtsp_session_id=session_id,
        )

        client = RTSPClient(device_ip, device_port)
        try:
            # Step 1: OPTIONS — discover capabilities
            logger.info("[%s] OPTIONS → %s:%d", session_id[:8], device_ip, device_port)
            resp = client.send_request(
                "OPTIONS",
                f"rtsp://{device_ip}:{device_port}",
                headers={
                    "Apple-ET": "3",
                    "Content-Type": "application/x-apple-binary-plist",
                },
            )
            if resp is None:
                raise ConnectionError("OPTIONS failed — no response from Apple TV")

            logger.info(
                "[%s] OPTIONS response: %d %s",
                session_id[:8],
                resp.status_code,
                resp.reason,
            )

            # Step 2: ANNOUNCE — describe the stream
            logger.info("[%s] ANNOUNCE → %s:%d", session_id[:8], device_ip, device_port)
            sdp = _build_sdp(url, title, session_id)
            sdp_len = len(sdp.encode("utf-8"))

            resp = client.send_request(
                "ANNOUNCE",
                f"rtsp://{device_ip}:{device_port}",
                headers={
                    "Content-Type": "application/sdp",
                    "X-Apple-Session-ID": session_id,
                },
                body=sdp,
            )
            if resp is None:
                raise ConnectionError("ANNOUNCE failed — no response")
            if resp.status_code == 401:
                # Apple TV 3 may require auth — try without first
                logger.warning("[%s] ANNOUNCE got 401, retrying with auth headers", session_id[:8])
                resp = client.send_request(
                    "ANNOUNCE",
                    f"rtsp://{device_ip}:{device_port}",
                    headers={
                        "Content-Type": "application/sdp",
                        "X-Apple-Session-ID": session_id,
                        "Authorization": "Bearer ",
                    },
                    body=sdp,
                )
                if resp and resp.status_code >= 400:
                    raise ConnectionError(f"ANNOUNCE auth failed: {resp.status_code}")

            logger.info("[%s] ANNOUNCE: %d %s", session_id[:8], resp.status_code, resp.reason)

            # Step 3: SETUP — configure transport
            logger.info("[%s] SETUP → %s:%d", session_id[:8], device_ip, device_port)
            resp = client.send_request(
                "SETUP",
                f"rtsp://{device_ip}:{device_port}/audio",
                headers={
                    "Transport": "RTP/AVP/TCP;unicast;interleaved=0-1",
                    "X-Apple-Session-ID": session_id,
                    "X-Apple-Client-Session-Id": session_id,
                },
            )
            if resp is None:
                raise ConnectionError("SETUP failed — no response")
            if resp.status_code >= 400:
                raise ConnectionError(f"SETUP failed: {resp.status_code} {resp.reason}")

            # Parse session ID from response
            session_header = resp.headers.get("Session", "")
            if session_header:
                session.rtsp_session_id = session_header.split(";")[0]

            logger.info("[%s] SETUP: %d %s (session=%s)",
                        session_id[:8], resp.status_code, resp.reason,
                        session.rtsp_session_id)

            # Step 4: RECORD — start streaming
            logger.info("[%s] RECORD → %s:%d", session_id[:8], device_ip, device_port)
            resp = client.send_request(
                "RECORD",
                f"rtsp://{device_ip}:{device_port}",
                headers={
                    "Range": "npt=0.000-",
                    "X-Apple-Session-ID": session_id,
                    "Session": session.rtsp_session_id or session_id,
                },
            )
            if resp is None:
                raise ConnectionError("RECORD failed — no response")

            logger.info("[%s] RECORD: %d %s", session_id[:8], resp.status_code, resp.reason)

            # Step 5: Start ffmpeg to stream the media
            ffmpeg_pid = self._start_streaming(
                url, device_ip, device_port, session_id
            )

            session.state = SessionState.STREAMING
            session.ffmpeg_pid = ffmpeg_pid
            session._rtsp_client = client  # type: ignore

            self._sessions[session_id] = session
            self._rtsp_clients[session_id] = client

            logger.info("[%s] ✅ STREAMING started (pid=%s)", session_id[:8], ffmpeg_pid)
            return session

        except Exception as e:
            logger.error("[%s] Play failed: %s", session_id[:8], e)
            session.state = SessionState.ERROR
            session.error = str(e)
            client.disconnect()
            raise

    def stop(self, device_ip: str) -> Optional[PlaybackSession]:
        """Stop playback on a device.

        Performs: TEARDOWN and kills ffmpeg process.
        """
        # Find session for this device
        session = None
        for s in self._sessions.values():
            if s.device_ip == device_ip:
                session = s
                break

        if session is None:
            logger.warning("No active session for %s", device_ip)
            return None

        # Kill ffmpeg process
        if session.ffmpeg_pid:
            try:
                import os
                os.kill(session.ffmpeg_pid, 9)
                logger.info("[%s] ffmpeg process %d killed", session.session_id[:8], session.ffmpeg_pid)
            except (OSError, ProcessLookupError):
                pass

        # Send TEARDOWN
        client = self._rtsp_clients.get(session.session_id)
        if client and client.is_connected:
            try:
                client.send_request(
                    "TEARDOWN",
                    f"rtsp://{device_ip}:{session.device_port}",
                    headers={
                        "X-Apple-Session-ID": session.session_id,
                        "Session": session.rtsp_session_id or session.session_id,
                    },
                )
                logger.info("[%s] TEARDOWN sent", session.session_id[:8])
            except Exception as e:
                logger.warning("[%s] TEARDOWN failed: %s", session.session_id[:8], e)
            finally:
                client.disconnect()

        # Cleanup
        session.state = SessionState.IDLE
        self._sessions.pop(session.session_id, None)
        self._rtsp_clients.pop(session.session_id, None)

        return session

    def _start_streaming(
        self,
        url: str,
        device_ip: str,
        device_port: int,
        session_id: str,
    ) -> Optional[int]:
        """Start ffmpeg to stream media to the Apple TV.

        For AirPlay 1, ffmpeg converts the source to AAC and outputs RTP.
        Falls back to HTTP streaming if direct RTP fails.
        """
        try:
            # Validate URL to prevent command injection
            from urllib.parse import urlparse
            parsed = urlparse(url)
            if parsed.scheme not in ("http", "https", "rtmp", "rtsp"):
                logger.error("Invalid URL scheme: %s", parsed.scheme)
                return None
            if not parsed.hostname:
                logger.error("URL has no hostname: %s", url)
                return None

            # Use AAC for AirPlay 1 compatibility
            # Apple TV 3 expects AAC or ALAC, NOT raw PCM
            cmd = [
                "ffmpeg",
                "-i", url,
                # Audio output — AAC for AirPlay 1
                "-f", "rtp",
                "-ar", "44100",
                "-ac", "2",
                "-acodec", "aac",
                "-ab", "128k",
                # RTP payload type 96
                "-payload_type", "96",
                # Send via UDP to device
                f"rtp://{device_ip}:{device_port}",
            ]

            logger.info("[%s] Starting ffmpeg: %s", session_id[:8], " ".join(cmd))
            proc = subprocess.Popen(
                cmd,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.PIPE,
            )
            return proc.pid

        except FileNotFoundError:
            logger.warning("ffmpeg not found — URL playback only")
            return None
        except Exception as e:
            logger.error("ffmpeg start failed: %s", e)
            return None

    def get_session(self, session_id: str) -> Optional[PlaybackSession]:
        return self._sessions.get(session_id)

    def get_session_by_device(self, device_ip: str) -> Optional[PlaybackSession]:
        for s in self._sessions.values():
            if s.device_ip == device_ip:
                return s
        return None

    def get_all_sessions(self) -> dict[str, PlaybackSession]:
        return self._sessions.copy()

    def stop_all(self):
        """Stop all active sessions."""
        for session_id in list(self._sessions.keys()):
            session = self._sessions[session_id]
            self.stop(session.device_ip)
