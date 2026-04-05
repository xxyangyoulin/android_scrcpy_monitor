package com.xxyangyoulin.scrcpymonitor

object StayAwakeManager {
    private const val ENABLED_VALUE = "7"
    private const val DISABLED_VALUE = "0"

    fun isEnabled(): Boolean? {
        val output =
            RootShell.outputOrNull(
                RootShell.run("settings get global stay_on_while_plugged_in")
            ) ?: return null
        val value = output.trim().toIntOrNull() ?: return null
        return value != 0
    }

    fun setEnabled(enabled: Boolean): Boolean {
        val value = if (enabled) ENABLED_VALUE else DISABLED_VALUE
        return RootShell.succeeded(
            RootShell.run("settings put global stay_on_while_plugged_in $value")
        )
    }
}
