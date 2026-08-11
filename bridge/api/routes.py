"""FastAPI route handlers for Kast AirPlay bridge.

Endpoints:
  GET  /devices         — list discovered AirPlay devices
  POST /devices/discover — trigger device discovery
  POST /play            — start playback on a device
  POST /stop            — stop playback on a device
  GET  /health          — bridge health check
"""

from __future__ import annotations

import logging
from typing import Optional

from fastapi import APIRouter, HTTPException

from ..airplay.client import AirPlayClient
from ..airplay.models import (
    ApiResponse,
    DeviceInfo,
    DeviceListResponse,
    DeviceType,
    PlayRequest,
    PlaybackSession,
    SessionListResponse,
    SessionState,
    StopRequest,
)

logger = logging.getLogger("kast.api")

router = APIRouter(prefix="/api/v1", tags=["airplay"])

# Global AirPlay client instance (initialized in main.py)
airplay_client: Optional[AirPlayClient] = None
discovered_devices: dict[str, DeviceInfo] = {}


def init_client(client: AirPlayClient):
    """Set the global AirPlay client (called from main.py)."""
    global airplay_client
    airplay_client = client


def _get_client() -> AirPlayClient:
    if airplay_client is None:
        raise HTTPException(status_code=500, detail="AirPlay client not initialized")
    return airplay_client


@router.get("/health")
def health_check() -> dict:
    """Health check endpoint."""
    return {
        "status": "ok",
        "service": "kast-airplay-bridge",
        "devices_discovered": len(discovered_devices),
    }


@router.get("/devices")
def list_devices() -> list[DeviceInfo]:
    """List all discovered AirPlay devices."""
    return list(discovered_devices.values())


@router.post("/devices/discover")
def discover_devices(
    subnet: Optional[str] = None,
    timeout: float = 1.0,
) -> DeviceListResponse:
    """Discover AirPlay devices on the network.

    If subnet is not provided, auto-detects from the bridge's network interface.
    Scans common AirPlay ports (7000) with fast timeout.
    Total scan is capped at ~10 seconds to avoid long waits.
    """
    import time
    client = _get_client()
    found: list[DeviceInfo] = []
    max_total_time = 10.0  # Hard cap on total scan time

    # Auto-detect subnet if not provided
    if subnet is None:
        subnet = _detect_subnet()

    if subnet is None:
        return DeviceListResponse(
            success=False,
            message="Could not detect network subnet",
            devices=[],
        )

    logger.info("Scanning subnet %s for AirPlay devices...", subnet)

    # Scan each IP in the subnet — port 7000 only for speed
    # AirPlay 1 serves on 7000; 7100 is HTTP fallback, 5000 is RTSP legacy
    start = time.monotonic()
    per_ip_timeout = min(timeout, 1.0)

    for i in range(1, 255):
        # Hard time cap
        if time.monotonic() - start > max_total_time:
            logger.info("Discovery time cap reached after %.1fs", time.monotonic() - start)
            break

        ip = f"{subnet}.{i}"
        try:
            device = client.discover(ip, 7000, timeout=per_ip_timeout)
            if device and device.ip not in discovered_devices:
                discovered_devices[device.ip] = device
                found.append(device)
                logger.info("Discovered: %s at %s:%d", device.name, device.ip, device.port)
        except Exception:
            continue

    elapsed = time.monotonic() - start
    return DeviceListResponse(
        success=True,
        message=f"Found {len(found)} device(s) in {elapsed:.1f}s",
        devices=found,
    )


@router.post("/devices/add")
def add_device_manual(device: DeviceInfo) -> DeviceInfo:
    """Manually add an AirPlay device by IP."""
    discovered_devices[device.ip] = device
    logger.info("Manually added device: %s at %s", device.name, device.ip)
    return device


@router.post("/play")
def play(request: PlayRequest) -> ApiResponse:
    """Start playback on an AirPlay device.

    This performs the full AirPlay 1 RTSP handshake:
    OPTIONS → ANNOUNCE → SETUP → RECORD → ffmpeg streaming
    """
    client = _get_client()

    # Validate device exists or add it
    if request.device_ip not in discovered_devices:
        discovered_devices[request.device_ip] = DeviceInfo(
            name=f"Apple TV ({request.device_ip})",
            ip=request.device_ip,
            port=request.device_port,
            model="AppleTV3,2",
            device_type=DeviceType.APPLE_TV,
        )

    try:
        session = client.play(
            device_ip=request.device_ip,
            url=request.url,
            device_port=request.device_port,
            title=request.title or "Kast",
        )

        return ApiResponse(
            success=True,
            message="Playback started",
            data={
                "session_id": session.session_id,
                "device_ip": session.device_ip,
                "state": session.state.value,
            },
        )

    except ConnectionError as e:
        logger.error("Connection error: %s", e)
        raise HTTPException(status_code=502, detail=f"Device unreachable: {e}")
    except Exception as e:
        logger.error("Play error: %s", e)
        raise HTTPException(status_code=500, detail=f"Playback failed: {e}")


@router.post("/stop")
def stop(request: StopRequest) -> ApiResponse:
    """Stop playback on an AirPlay device.

    Sends TEARDOWN and kills any running ffmpeg process.
    """
    client = _get_client()

    session = client.stop(request.device_ip)
    if session is None:
        return ApiResponse(
            success=False,
            message=f"No active session for {request.device_ip}",
        )

    return ApiResponse(
        success=True,
        message="Playback stopped",
        data={
            "session_id": session.session_id,
            "device_ip": session.device_ip,
            "state": session.state.value,
        },
    )


@router.get("/sessions")
def list_sessions() -> SessionListResponse:
    """List all active playback sessions."""
    client = _get_client()
    return SessionListResponse(
        sessions=list(client.get_all_sessions().values()),
    )


def _detect_subnet() -> Optional[str]:
    """Detect the local network subnet for scanning."""
    import socket
    try:
        # Connect to a public IP to determine local interface
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
        return local_ip.rsplit(".", 1)[0]
    except Exception as e:
        logger.error("Subnet detection failed: %s", e)
        return None
