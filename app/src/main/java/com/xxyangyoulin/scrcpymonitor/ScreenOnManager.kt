package com.xxyangyoulin.scrcpymonitor

object ScreenOnManager {
    private const val SCREEN_ALWAYS_ON_TIMEOUT = 2147483647
    private const val DEFAULT_SCREEN_TIMEOUT = 30000

    fun isEnabled(): Boolean? {
        val output =
            RootShell.outputOrNull(
                RootShell.run("settings get system screen_off_timeout")
            ) ?: return null
        val value = output.trim().toLongOrNull() ?: return null
        return value >= SCREEN_ALWAYS_ON_TIMEOUT
    }

    fun setEnabled(enabled: Boolean): Boolean {
        val value = if (enabled) SCREEN_ALWAYS_ON_TIMEOUT else DEFAULT_SCREEN_TIMEOUT
        return RootShell.succeeded(
            RootShell.run("settings put system screen_off_timeout $value")
        )
    }
}
