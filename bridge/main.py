#!/usr/bin/env python3
"""Kast AirPlay Bridge — FastAPI entry point.

A local bridge that translates HTTP API calls from Kast Android
into AirPlay 1 RTSP communication with Apple TV 3 (A1469).

Usage:
    python -m bridge.main
    # or
    uvicorn bridge.main:app --host 0.0.0.0 --port 8420 --reload

The bridge must run on the same network as both:
- The Kast Android app (sender)
- The Apple TV 3 (receiver)
"""

from __future__ import annotations

import logging
import sys
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .airplay.client import AirPlayClient
from .api.routes import router, init_client

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("kast.bridge")


@asynccontextmanager
async def lifespan(application: FastAPI):
    """Manage AirPlay client lifecycle."""
    client = AirPlayClient()
    init_client(client)
    logger.info("Kast AirPlay Bridge started")
    logger.info("API docs: http://localhost:8420/docs")
    yield
    from .api.routes import airplay_client
    if airplay_client:
        airplay_client.stop_all()
    logger.info("Kast AirPlay Bridge stopped")


# Create FastAPI app
app = FastAPI(
    title="Kast AirPlay Bridge",
    description="Local bridge for AirPlay 1 communication with Apple TV 3",
    version="0.1.0",
    lifespan=lifespan,
)

# CORS — allow Kast Android app on same network
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount API routes
app.include_router(router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "bridge.main:app",
        host="0.0.0.0",
        port=8420,
        reload=True,
        log_level="info",
    )
