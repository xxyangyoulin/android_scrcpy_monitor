package com.xxyangyoulin.scrcpymonitor

object ScrcpyStateDetector {
    data class Snapshot(
        val status: Status,
        val endpoint: String?
    )

    enum class Status {
        CONNECTED,
        DISCONNECTED,
        UNKNOWN
    }

    fun getStatus(): Status {
        return getSnapshot().status
    }

    fun getSnapshot(): Snapshot {
        val script = """
            endpoint="$(ss -tn 2>/dev/null | awk '$1 == "ESTAB" && $4 ~ /:5555$/ {print $5; exit}')"
            if ps -A -o NAME,ARGS | grep -F 'com.genymobile.scrcpy.Server' | grep -Fv 'grep -F' >/dev/null; then
              echo "connected|${'$'}endpoint"
            elif cat /proc/net/unix | grep -q '@scrcpy_'; then
              echo "connected|${'$'}endpoint"
            else
              echo "disconnected|${'$'}endpoint"
            fi
        """.trimIndent()

        return when (val result = RootShell.run(script)) {
            is RootShell.Result.Success -> {
                val parts = RootShell.outputOrNull(result)?.split("|", limit = 2).orEmpty()
                val status = when (parts.firstOrNull()) {
                    "connected" -> Status.CONNECTED
                    "disconnected" -> Status.DISCONNECTED
                    "" -> Status.UNKNOWN
                    else -> Status.UNKNOWN
                }
                Snapshot(status, parts.getOrNull(1)?.ifBlank { null })
            }
            RootShell.Result.Failure -> Snapshot(Status.UNKNOWN, null)
        }
    }
}
