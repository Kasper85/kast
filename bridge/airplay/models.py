"""Data models for AirPlay bridge API and internal state."""

from __future__ import annotations

import uuid
from enum import Enum
from typing import Optional
from urllib.parse import urlparse

from pydantic import BaseModel, Field, field_validator


class DeviceType(str, Enum):
    APPLE_TV = "apple_tv"


class DeviceInfo(BaseModel):
    """Discovered AirPlay device."""

    id: str = Field(default_factory=lambda: uuid.uuid4().hex[:8])
    name: str
    ip: str
    port: int = 7000
    model: str = ""
    device_type: DeviceType = DeviceType.APPLE_TV
    protocol_version: str = ""
    airplay_version: str = ""


class PlayRequest(BaseModel):
    """Request to play content on an AirPlay device."""

    url: str = Field(..., min_length=1, description="Media URL to play")
    device_ip: str = Field(..., min_length=1, description="IP of the target AirPlay device")
    device_port: int = Field(default=7000, description="RTSP port (default 7000)")
    title: Optional[str] = Field(default=None, description="Media title for metadata")
    position: float = Field(default=0.0, description="Start position in seconds")

    @field_validator("url")
    @classmethod
    def validate_url(cls, v: str) -> str:
        parsed = urlparse(v)
        if parsed.scheme not in ("http", "https", "rtmp", "rtsp"):
            raise ValueError(f"Unsupported URL scheme: {parsed.scheme}")
        if not parsed.hostname:
            raise ValueError("URL must have a hostname")
        return v

    @field_validator("device_ip")
    @classmethod
    def validate_ip(cls, v: str) -> str:
        parts = v.split(".")
        if len(parts) != 4:
            raise ValueError("Invalid IPv4 address format")
        for part in parts:
            if not part.isdigit() or not 0 <= int(part) <= 255:
                raise ValueError(f"Invalid IPv4 octet: {part}")
        return v


class StopRequest(BaseModel):
    """Request to stop playback on an AirPlay device."""

    device_ip: str = Field(..., description="IP of the target AirPlay device")


class SessionState(str, Enum):
    IDLE = "idle"
    CONNECTING = "connecting"
    STREAMING = "streaming"
    ERROR = "error"


class PlaybackSession(BaseModel):
    """Active playback session with an AirPlay device."""

    session_id: str = Field(default_factory=lambda: uuid.uuid4().hex[:12])
    device_ip: str
    device_port: int = 7000
    state: SessionState = SessionState.IDLE
    current_url: Optional[str] = None
    rtsp_session_id: Optional[str] = None
    ffmpeg_pid: Optional[int] = None
    error: Optional[str] = None


class ApiResponse(BaseModel):
    """Generic API response."""

    success: bool
    message: str
    data: Optional[dict] = None


class DeviceListResponse(BaseModel):
    """Response for device listing endpoints."""

    success: bool
    message: str
    devices: list[DeviceInfo] = []


class SessionListResponse(BaseModel):
    """Response for session listing endpoint."""

    sessions: list[PlaybackSession] = []
