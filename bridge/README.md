# Kast AirPlay Bridge

Local bridge that translates HTTP API calls from Kast Android into AirPlay 1 RTSP communication with Apple TV 3 (A1469).

## Architecture

```
Kast Android (Kotlin)
        |
   HTTP API (port 8420)
        |
Python FastAPI Bridge
        |
   RTSP (port 7000)
        |
Apple TV 3 (A1469)
```

## Quick Start

```bash
# Install dependencies
pip install -r requirements.txt

# Run the bridge
python -m bridge.main
# or
uvicorn bridge.main:app --host 0.0.0.0 --port 8420 --reload
```

The bridge must run on the **same network** as both the Kast Android app and the Apple TV.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/health` | Health check |
| `GET` | `/api/v1/devices` | List discovered devices |
| `POST` | `/api/v1/devices/discover` | Scan network for AirPlay devices |
| `POST` | `/api/v1/devices/add` | Add device manually by IP |
| `POST` | `/api/v1/play` | Start AirPlay playback |
| `POST` | `/api/v1/stop` | Stop playback |
| `GET` | `/api/v1/sessions` | List active sessions |

Interactive API docs: `http://localhost:8420/docs`

## Usage

### 1. Discover devices

```bash
curl -X POST http://localhost:8420/api/v1/devices/discover
```

### 2. Add device manually

```bash
curl -X POST http://localhost:8420/api/v1/devices/add \
  -H "Content-Type: application/json" \
  -d '{"name": "Apple TV", "ip": "192.168.1.31", "port": 7000}'
```

### 3. Play content

```bash
curl -X POST http://localhost:8420/api/v1/play \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/video.mp4", "device_ip": "192.168.1.31"}'
```

### 4. Stop playback

```bash
curl -X POST http://localhost:8420/api/v1/stop \
  -H "Content-Type: application/json" \
  -d '{"device_ip": "192.168.1.31"}'
```

## AirPlay 1 RTSP Handshake

The bridge implements the AirPlay 1 protocol for Apple TV 3:

```
Client (Bridge)                 Apple TV 3
       |                              |
       |---- OPTIONS ---------------->|
       |<--- 200 OK (capabilities) ---|
       |                              |
       |---- ANNOUNCE (SDP) --------->|
       |<--- 200 OK ------------------|
       |                              |
       |---- SETUP (transport) ------->|
       |<--- 200 OK (session) --------|
       |                              |
       |---- RECORD ------------------>|
       |<--- 200 OK ------------------|
       |                              |
       |---- [ffmpeg RTP stream] ----->|
       |                              |
       |---- TEARDOWN ---------------->|
       |<--- 200 OK ------------------|
```

## Requirements

- Python 3.10+
- FFmpeg (for media streaming)
- Network access to Apple TV on port 7000

## Supported Devices

- Apple TV 3,2 (A1469) — Software 7.2
- Other Apple TV models with AirPlay 1 support
