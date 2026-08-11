package com.kastlg.app.data.tv.target

/**
 * Abstraction for playback destinations.
 *
 * Each target knows how to:
 * 1. Connect/disconnect to its device
 * 2. Open a URL for playback
 * 3. Report its connection state
 *
 * Implementations:
 * - [LGWebOsTarget] — LG webOS TV via SSAP WebSocket
 * - [AppleTvTarget] — Apple TV via local AirPlay bridge HTTP
 */
interface PlaybackTarget {
    /** Unique identifier for this target (e.g., IP address or device ID). */
    val targetId: String

    /** Human-readable name for the target. */
    val displayName: String

    /** Type of target. */
    val targetType: TargetType

    /** Whether the target is currently connected/ready. */
    val isConnected: Boolean

    /**
     * Connect to the target device.
     * For LG: pairs via WebSocket.
     * For Apple TV: verifies bridge is reachable.
     */
    suspend fun connect(): Result<Unit>

    /**
     * Open a URL for playback on the target.
     * For LG: launches the browser via SSAP.
     * For Apple TV: calls the bridge API to start AirPlay.
     */
    suspend fun openUrl(url: String): Result<Unit>

    /**
     * Stop any active playback on the target.
     * For LG: no-op (browser manages playback).
     * For Apple TV: sends TEARDOWN via bridge API.
     */
    suspend fun stop(): Result<Unit>

    /**
     * Disconnect from the target.
     */
    fun disconnect()
}
