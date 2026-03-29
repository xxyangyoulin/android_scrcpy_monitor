package com.xxyangyoulin.scrcpymonitor

object MonitorRuntimeState {
    data class State(
        val running: Boolean,
        val snapshot: ScrcpyStateDetector.Snapshot,
        val connectedAtMillis: Long?,
        val updatedAtMillis: Long
    )

    @Volatile
    private var state = State(
        running = false,
        snapshot = ScrcpyStateDetector.Snapshot(ScrcpyStateDetector.Status.UNKNOWN, null),
        connectedAtMillis = null,
        updatedAtMillis = 0L
    )

    fun current(): State {
        return state
    }

    fun update(running: Boolean, snapshot: ScrcpyStateDetector.Snapshot): State {
        val previousState = state
        val connectedAtMillis = when {
            snapshot.status != ScrcpyStateDetector.Status.CONNECTED -> null
            previousState.snapshot.status == ScrcpyStateDetector.Status.CONNECTED ->
                previousState.connectedAtMillis ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        return State(
            running = running,
            snapshot = snapshot,
            connectedAtMillis = connectedAtMillis,
            updatedAtMillis = System.currentTimeMillis()
        ).also { state = it }
    }

    fun markStopped(): State {
        return update(
            running = false,
            snapshot = ScrcpyStateDetector.Snapshot(ScrcpyStateDetector.Status.DISCONNECTED, null)
        )
    }
}
