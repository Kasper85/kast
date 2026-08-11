"""Comprehensive test suite for Kast AirPlay Bridge.

Covers: models, RTSP protocol, API endpoints, AirPlay client, discovery.
Run: python -m pytest bridge/tests/ -v
"""

from __future__ import annotations

import json
import socket
from unittest.mock import MagicMock, patch, AsyncMock

import pytest
from fastapi.testclient import TestClient

# ── Models ──────────────────────────────────────────────────────────


class TestDeviceInfo:
    def test_defaults(self):
        from bridge.airplay.models import DeviceInfo, DeviceType
        d = DeviceInfo(name="Test", ip="192.168.1.1")
        assert d.port == 7000
        assert d.device_type == DeviceType.APPLE_TV
        assert d.model == ""
        assert len(d.id) == 8

    def test_json_roundtrip(self):
        from bridge.airplay.models import DeviceInfo
        d = DeviceInfo(name="ATV", ip="10.0.0.5", port=7100, model="AppleTV3,2")
        data = d.model_dump()
        d2 = DeviceInfo(**data)
        assert d2.ip == "10.0.0.5"
        assert d2.model == "AppleTV3,2"


class TestPlayRequest:
    def test_required_fields(self):
        from bridge.airplay.models import PlayRequest
        r = PlayRequest(url="http://example.com/video.mp4", device_ip="192.168.1.31")
        assert r.device_port == 7000
        assert r.title is None
        assert r.position == 0.0

    def test_missing_url_raises(self):
        from bridge.airplay.models import PlayRequest
        from pydantic import ValidationError
        with pytest.raises(ValidationError):
            PlayRequest(device_ip="192.168.1.31")  # type: ignore

    def test_missing_device_ip_raises(self):
        from bridge.airplay.models import PlayRequest
        from pydantic import ValidationError
        with pytest.raises(ValidationError):
            PlayRequest(url="http://example.com/v.mp4")  # type: ignore


class TestStopRequest:
    def test_required_fields(self):
        from bridge.airplay.models import StopRequest
        r = StopRequest(device_ip="192.168.1.31")
        assert r.device_ip == "192.168.1.31"


class TestPlaybackSession:
    def test_defaults(self):
        from bridge.airplay.models import PlaybackSession, SessionState
        s = PlaybackSession(device_ip="10.0.0.1")
        assert s.state == SessionState.IDLE
        assert s.ffmpeg_pid is None
        assert s.error is None
        assert len(s.session_id) == 12


class TestApiResponse:
    def test_success(self):
        from bridge.airplay.models import ApiResponse
        r = ApiResponse(success=True, message="ok")
        assert r.data is None

    def test_with_data(self):
        from bridge.airplay.models import ApiResponse
        r = ApiResponse(success=True, message="ok", data={"key": "value"})
        assert r.data["key"] == "value"


# ── RTSP Protocol ───────────────────────────────────────────────────


class TestRTSPMessage:
    def test_request_serialize(self):
        from bridge.airplay.rtsp import RTSPMessage
        msg = RTSPMessage(
            method="OPTIONS",
            uri="rtsp://192.168.1.31:7000",
            headers={"CSeq": "1", "User-Agent": "Kast/1.0"},
        )
        raw = msg.serialize()
        assert b"OPTIONS rtsp://192.168.1.31:7000 RTSP/1.0" in raw
        assert b"CSeq: 1" in raw
        assert b"User-Agent: Kast/1.0" in raw

    def test_response_serialize(self):
        from bridge.airplay.rtsp import RTSPMessage
        msg = RTSPMessage(
            status_code=200,
            reason="OK",
            headers={"CSeq": "1", "Server": "AirPlay/1.0"},
        )
        raw = msg.serialize()
        assert b"RTSP/1.0 200 OK" in raw
        assert b"Server: AirPlay/1.0" in raw

    def test_request_parse(self):
        from bridge.airplay.rtsp import RTSPMessage
        raw = (
            b"OPTIONS rtsp://192.168.1.31:7000 RTSP/1.0\r\n"
            b"CSeq: 1\r\n"
            b"User-Agent: Kast/1.0\r\n"
            b"\r\n"
        )
        msg = RTSPMessage.parse(raw)
        assert msg is not None
        assert msg.is_request
        assert not msg.is_response
        assert msg.method == "OPTIONS"
        assert msg.uri == "rtsp://192.168.1.31:7000"
        assert msg.cseq == 1

    def test_response_parse(self):
        from bridge.airplay.rtsp import RTSPMessage
        raw = (
            b"RTSP/1.0 200 OK\r\n"
            b"CSeq: 1\r\n"
            b"Server: AirPlay/1.0\r\n"
            b"Public: ANNOUNCE, SETUP, RECORD, TEARDOWN\r\n"
            b"\r\n"
        )
        msg = RTSPMessage.parse(raw)
        assert msg is not None
        assert msg.is_response
        assert msg.status_code == 200
        assert msg.reason == "OK"
        assert msg.headers["Server"] == "AirPlay/1.0"
        assert msg.headers["Public"] == "ANNOUNCE, SETUP, RECORD, TEARDOWN"

    def test_response_parse_with_body(self):
        from bridge.airplay.rtsp import RTSPMessage
        body = "v=0\r\no=Test 0 0 IN IP4 1.0.0.1\r\n"
        raw = (
            b"RTSP/1.0 200 OK\r\n"
            b"CSeq: 2\r\n"
            b"Content-Type: application/sdp\r\n"
            b"Content-Length: " + str(len(body)).encode() + b"\r\n"
            b"\r\n" + body.encode()
        )
        msg = RTSPMessage.parse(raw)
        assert msg is not None
        assert msg.body is not None
        assert "v=0" in msg.body

    def test_parse_empty_data(self):
        from bridge.airplay.rtsp import RTSPMessage
        assert RTSPMessage.parse(b"") is None

    def test_parse_garbage(self):
        from bridge.airplay.rtsp import RTSPMessage
        assert RTSPMessage.parse(b"not rtsp at all") is None


class TestRTSPClient:
    def test_not_connected_send(self):
        from bridge.airplay.rtsp import RTSPClient
        c = RTSPClient("127.0.0.1", 9999)
        assert c.send_request("OPTIONS") is None

    def test_is_connected_initial(self):
        from bridge.airplay.rtsp import RTSPClient
        c = RTSPClient("127.0.0.1", 9999)
        assert not c.is_connected

    @patch("socket.socket")
    def test_connect_success(self, mock_sock_cls):
        from bridge.airplay.rtsp import RTSPClient
        mock_sock = MagicMock()
        mock_sock_cls.return_value = mock_sock
        c = RTSPClient("192.168.1.31", 7000)
        result = c.connect()
        assert result is True
        assert c.is_connected
        mock_sock.connect.assert_called_once_with(("192.168.1.31", 7000))

    @patch("socket.socket")
    def test_connect_failure(self, mock_sock_cls):
        from bridge.airplay.rtsp import RTSPClient
        mock_sock = MagicMock()
        mock_sock.connect.side_effect = ConnectionRefusedError
        mock_sock_cls.return_value = mock_sock
        c = RTSPClient("192.168.1.31", 7000)
        result = c.connect()
        assert result is False
        assert not c.is_connected

    def test_disconnect(self):
        from bridge.airplay.rtsp import RTSPClient
        c = RTSPClient("127.0.0.1", 9999)
        c.disconnect()
        assert not c.is_connected

    @patch("socket.socket")
    def test_send_request_no_response(self, mock_sock_cls):
        from bridge.airplay.rtsp import RTSPClient
        mock_sock = MagicMock()
        mock_sock.recv.return_value = b""  # Connection closed
        mock_sock_cls.return_value = mock_sock
        c = RTSPClient("192.168.1.31", 7000)
        c.connect()
        result = c.send_request("OPTIONS", "rtsp://192.168.1.31:7000")
        assert result is None


# ── SDP Generation ──────────────────────────────────────────────────


class TestSDP:
    def test_build_sdp_basic(self):
        from bridge.airplay.client import _build_sdp
        sdp = _build_sdp("http://example.com/video.mp4", "Test Movie", "ABCDEF01")
        assert "v=0" in sdp
        assert "s=Test Movie" in sdp
        assert "a=x-apple-session-id:ABCDEF01" in sdp
        assert "m=audio 0 RTP/AVP 96" in sdp
        assert "a=rtpmap:96 MPEG4-GENERIC/44100/2" in sdp

    def test_build_sdp_url_parsing(self):
        from bridge.airplay.client import _build_sdp
        sdp = _build_sdp("http://192.168.1.50:8080/stream", "Stream")
        assert "o=Kast 0 0 IN IP4 192.168.1.50" in sdp

    def test_build_sdp_connection_address(self):
        from bridge.airplay.client import _build_sdp
        # SDP c= line should always be 0.0.0.0 (let the receiver decide)
        sdp = _build_sdp("http://example.com/v", "T")
        assert "c=IN IP4 0.0.0.0" in sdp


# ── AirPlay Client (Mocked) ─────────────────────────────────────────


class TestAirPlayClient:
    @patch("bridge.airplay.client.RTSPClient")
    def test_discover_via_rtsp(self, MockRTSPClient):
        from bridge.airplay.client import AirPlayClient
        mock_rtsp = MagicMock()
        MockRTSPClient.return_value = mock_rtsp
        mock_rtsp.connect.return_value = True
        mock_rtsp.is_connected = True

        resp = MagicMock()
        resp.headers = {"Server": "AirPlay/220.68"}
        mock_rtsp.send_request.return_value = resp

        client = AirPlayClient()
        device = client.discover("192.168.1.31", 7000)
        assert device is not None
        assert device.ip == "192.168.1.31"
        mock_rtsp.connect.assert_called_once()

    @patch("bridge.airplay.client.RTSPClient")
    def test_discover_connection_failure(self, MockRTSPClient):
        from bridge.airplay.client import AirPlayClient
        mock_rtsp = MagicMock()
        MockRTSPClient.return_value = mock_rtsp
        mock_rtsp.connect.return_value = False

        client = AirPlayClient()
        device = client.discover("192.168.1.99", 7000)
        assert device is None

    @patch("bridge.airplay.client.RTSPClient")
    def test_play_full_handshake(self, MockRTSPClient):
        from bridge.airplay.client import AirPlayClient
        mock_rtsp = MagicMock()
        MockRTSPClient.return_value = mock_rtsp
        mock_rtsp.connect.return_value = True
        mock_rtsp.is_connected = True

        # Mock all RTSP responses
        ok_resp = MagicMock()
        ok_resp.status_code = 200
        ok_resp.reason = "OK"
        ok_resp.headers = {"Session": "DEADBEEF;timeout=30"}
        ok_resp.body = None
        mock_rtsp.send_request.return_value = ok_resp

        client = AirPlayClient()
        with patch.object(client, "_start_streaming", return_value=12345):
            session = client.play(
                device_ip="192.168.1.31",
                url="http://example.com/video.mp4",
                device_port=7000,
            )
            assert session is not None
            assert session.state.value == "streaming"
            assert session.ffmpeg_pid == 12345
            # Verify all 4 RTSP methods were called
            calls = mock_rtsp.send_request.call_args_list
            methods = [c.args[0] for c in calls]
            assert "OPTIONS" in methods
            assert "ANNOUNCE" in methods
            assert "SETUP" in methods
            assert "RECORD" in methods

    @patch("bridge.airplay.client.RTSPClient")
    def test_play_options_failure(self, MockRTSPClient):
        from bridge.airplay.client import AirPlayClient
        mock_rtsp = MagicMock()
        MockRTSPClient.return_value = mock_rtsp
        mock_rtsp.connect.return_value = True
        mock_rtsp.is_connected = True
        mock_rtsp.send_request.return_value = None

        client = AirPlayClient()
        with pytest.raises(ConnectionError, match="OPTIONS failed"):
            client.play(
                device_ip="192.168.1.31",
                url="http://example.com/video.mp4",
            )

    @patch("bridge.airplay.client.RTSPClient")
    def test_stop_with_session(self, MockRTSPClient):
        from bridge.airplay.client import AirPlayClient
        mock_rtsp = MagicMock()
        MockRTSPClient.return_value = mock_rtsp
        mock_rtsp.connect.return_value = True
        mock_rtsp.is_connected = True

        ok_resp = MagicMock()
        ok_resp.status_code = 200
        ok_resp.reason = "OK"
        ok_resp.headers = {"Session": "DEADBEEF"}
        mock_rtsp.send_request.return_value = ok_resp

        client = AirPlayClient()
        with patch.object(client, "_start_streaming", return_value=None):
            session = client.play(
                device_ip="192.168.1.31",
                url="http://example.com/video.mp4",
            )

        stopped = client.stop("192.168.1.31")
        assert stopped is not None
        assert stopped.session_id == session.session_id

    def test_stop_no_session(self):
        from bridge.airplay.client import AirPlayClient
        client = AirPlayClient()
        result = client.stop("192.168.1.99")
        assert result is None

    def test_get_all_sessions_empty(self):
        from bridge.airplay.client import AirPlayClient
        client = AirPlayClient()
        assert len(client.get_all_sessions()) == 0

    def test_stop_all(self):
        from bridge.airplay.client import AirPlayClient
        client = AirPlayClient()
        client.stop_all()
        assert len(client.get_all_sessions()) == 0


# ── API Endpoints (FastAPI TestClient) ───────────────────────────────


class TestAPIHealth:
    def test_health(self):
        from bridge.main import app
        client = TestClient(app)
        resp = client.get("/api/v1/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "ok"
        assert data["service"] == "kast-airplay-bridge"
        assert "devices_discovered" in data


class TestAPIDevices:
    def test_list_empty(self):
        from bridge.main import app
        client = TestClient(app)
        resp = client.get("/api/v1/devices")
        assert resp.status_code == 200
        assert resp.json() == []

    def test_add_manual(self):
        from bridge.main import app
        client = TestClient(app)
        resp = client.post("/api/v1/devices/add", json={
            "name": "Test Apple TV",
            "ip": "192.168.1.99",
            "port": 7000,
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["ip"] == "192.168.1.99"
        assert data["name"] == "Test Apple TV"

    def test_list_after_add(self):
        from bridge.main import app
        client = TestClient(app)
        client.post("/api/v1/devices/add", json={
            "name": "ATV",
            "ip": "10.0.0.5",
        })
        resp = client.get("/api/v1/devices")
        assert resp.status_code == 200
        devices = resp.json()
        assert len(devices) >= 1


class TestAPIPlay:
    def test_play_no_device_reachable(self):
        from bridge.main import app
        client = TestClient(app)
        resp = client.post("/api/v1/play", json={
            "url": "http://example.com/video.mp4",
            "device_ip": "192.168.1.99",
        })
        # Should fail because device is unreachable
        assert resp.status_code in (400, 500, 502)


class TestAPIStop:
    def test_stop_no_session(self):
        from bridge.main import app
        client = TestClient(app)
        resp = client.post("/api/v1/stop", json={
            "device_ip": "192.168.1.99",
        })
        assert resp.status_code == 200
        data = resp.json()
        assert data["success"] is False


class TestAPISessions:
    def test_list_sessions(self):
        from bridge.main import app
        client = TestClient(app)
        resp = client.get("/api/v1/sessions")
        assert resp.status_code == 200
        data = resp.json()
        assert "sessions" in data
        assert isinstance(data["sessions"], list)


class TestAPIDiscover:
    def test_discover_with_invalid_subnet(self):
        """Discover with a non-routable subnet should return quickly."""
        from bridge.main import app
        client = TestClient(app)
        # 192.0.2.0/24 is TEST-NET-3 (RFC 5737) — no real devices
        resp = client.post("/api/v1/devices/discover?timeout=0.02&subnet=192.0.2")
        assert resp.status_code == 200
        data = resp.json()
        assert "success" in data
        assert "devices" in data

    def test_discover_manual_subnet(self):
        """Discover with explicit subnet parameter."""
        from bridge.main import app
        client = TestClient(app)
        # Use TEST-NET-3 range for fast, guaranteed-empty scan
        resp = client.post("/api/v1/devices/discover?subnet=192.0.2&timeout=0.02")
        assert resp.status_code == 200
        data = resp.json()
        assert "success" in data


# ── Pydantic Validation ─────────────────────────────────────────────


class TestPydanticValidation:
    def test_play_request_rejects_empty_url(self):
        from bridge.airplay.models import PlayRequest
        from pydantic import ValidationError
        with pytest.raises(ValidationError):
            PlayRequest(url="", device_ip="10.0.0.1")

    def test_play_request_rejects_invalid_scheme(self):
        from bridge.airplay.models import PlayRequest
        from pydantic import ValidationError
        with pytest.raises(ValidationError):
            PlayRequest(url="ftp://example.com/v.mp4", device_ip="10.0.0.1")

    def test_play_request_rejects_no_hostname(self):
        from bridge.airplay.models import PlayRequest
        from pydantic import ValidationError
        with pytest.raises(ValidationError):
            PlayRequest(url="http:///v.mp4", device_ip="10.0.0.1")

    def test_play_request_rejects_invalid_ip(self):
        from bridge.airplay.models import PlayRequest
        from pydantic import ValidationError
        with pytest.raises(ValidationError):
            PlayRequest(url="http://example.com/v.mp4", device_ip="999.999.999.999")

    def test_play_request_rejects_non_ip_string(self):
        from bridge.airplay.models import PlayRequest
        from pydantic import ValidationError
        with pytest.raises(ValidationError):
            PlayRequest(url="http://example.com/v.mp4", device_ip="not-an-ip")

    def test_play_request_accepts_valid_url(self):
        from bridge.airplay.models import PlayRequest
        r = PlayRequest(url="https://example.com/v.mp4", device_ip="10.0.0.1")
        assert r.url.startswith("https://")

    def test_device_info_invalid_port(self):
        from bridge.airplay.models import DeviceInfo
        d = DeviceInfo(name="T", ip="1.2.3.4", port=-1)
        # Pydantic doesn't enforce port range by default, but model should still work
        assert d.port == -1

    def test_play_request_negative_position(self):
        from bridge.airplay.models import PlayRequest
        r = PlayRequest(url="http://x.com/v", device_ip="1.2.3.4", position=-5.0)
        assert r.position == -5.0  # No validation on negative position


# ── Edge Cases ───────────────────────────────────────────────────────


class TestEdgeCases:
    def test_session_id_uniqueness(self):
        from bridge.airplay.models import PlaybackSession
        s1 = PlaybackSession(device_ip="1.1.1.1")
        s2 = PlaybackSession(device_ip="1.1.1.1")
        assert s1.session_id != s2.session_id

    def test_device_id_uniqueness(self):
        from bridge.airplay.models import DeviceInfo
        d1 = DeviceInfo(name="T", ip="1.1.1.1")
        d2 = DeviceInfo(name="T", ip="1.1.1.1")
        assert d1.id != d2.id

    def test_rtsp_cseq_increment(self):
        from bridge.airplay.rtsp import RTSPClient
        c = RTSPClient("127.0.0.1", 9999)
        c1 = c.cseq
        c2 = c.cseq
        c3 = c.cseq
        assert c1 < c2 < c3

    def test_session_state_values(self):
        from bridge.airplay.models import SessionState
        assert SessionState.IDLE.value == "idle"
        assert SessionState.CONNECTING.value == "connecting"
        assert SessionState.STREAMING.value == "streaming"
        assert SessionState.ERROR.value == "error"
