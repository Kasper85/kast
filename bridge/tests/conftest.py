"""Shared test fixtures for bridge tests."""

from __future__ import annotations

import pytest
from bridge.airplay.client import AirPlayClient
from bridge.api.routes import init_client


@pytest.fixture(autouse=True)
def _init_airplay_client():
    """Ensure airplay_client is initialized for every test."""
    client = AirPlayClient()
    init_client(client)
    yield
    # Cleanup: stop any sessions created during the test
    client.stop_all()
