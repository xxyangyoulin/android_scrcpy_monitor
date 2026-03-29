package com.xxyangyoulin.scrcpymonitor

import android.content.Context

object WifiDebuggingManager {
    private const val TCP_DISABLED = "-1"
    private const val STATUS_CACHE_TTL_MS = 30_000L

    @Volatile
    private var cachedStatus: Status? = null

    @Volatile
    private var cachedPort: Int? = null

    @Volatile
    private var cachedAtMillis = 0L

    enum class Status {
        ENABLED,
        DISABLED,
        UNKNOWN
    }

    fun getCachedStatus(): Status {
        return cachedStatus ?: Status.UNKNOWN
    }

    fun getPort(forceRefresh: Boolean = false): Int? {
        getStatus(forceRefresh)
        return cachedPort
    }

    fun getStatus(forceRefresh: Boolean = false): Status {
        val now = System.currentTimeMillis()
        val cached = cachedStatus
        if (!forceRefresh && cached != null && now - cachedAtMillis < STATUS_CACHE_TTL_MS) {
            return cached
        }

        val wifiSetting = RootShell.run("settings get global adb_wifi_enabled")
        val runtimePort = RootShell.run("getprop service.adb.tcp.port")
        val persistentPort = RootShell.run("getprop persist.adb.tcp.port")
        val wifiSettingValue = RootShell.outputOrNull(wifiSetting)
        val runtimePortValue = RootShell.outputOrNull(runtimePort)
        val persistentPortValue = RootShell.outputOrNull(persistentPort)
        val port = runtimePortValue.toPortOrNull() ?: persistentPortValue.toPortOrNull()

        val enabledBySetting = wifiSettingValue == "1"
        val enabledByRuntimePort =
            runtimePortValue != null &&
                runtimePortValue.isNotBlank() &&
                runtimePortValue != TCP_DISABLED
        val enabledByPersistentPort =
            persistentPortValue != null &&
                persistentPortValue.isNotBlank() &&
                persistentPortValue != TCP_DISABLED

        val status = when {
            enabledBySetting || enabledByRuntimePort || enabledByPersistentPort -> Status.ENABLED
            wifiSettingValue != null &&
                runtimePortValue != null &&
                persistentPortValue != null -> Status.DISABLED
            else -> Status.UNKNOWN
        }
        cachedStatus = status
        cachedPort = port
        cachedAtMillis = now
        return status
    }

    fun setEnabled(enabled: Boolean, port: Int): Boolean {
        val wifiValue = if (enabled) "1" else "0"
        val tcpValue = if (enabled) port.toString() else TCP_DISABLED
        val commands = listOf(
            "settings put global adb_wifi_enabled $wifiValue",
            "setprop persist.adb.tcp.port $tcpValue",
            "setprop service.adb.tcp.port $tcpValue",
            "setprop ctl.stop adbd",
            "sleep 1",
            "setprop ctl.start adbd"
        )
        val success = RootShell.succeeded(RootShell.run(commands))
        if (success) {
            cachedStatus = if (enabled) Status.ENABLED else Status.DISABLED
            cachedPort = if (enabled) port else null
            cachedAtMillis = System.currentTimeMillis()
        } else {
            invalidateStatusCache()
        }
        return success
    }

    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        return setEnabled(enabled, MonitorSettings.getWifiDebuggingPort(context))
    }

    fun invalidateStatusCache() {
        cachedStatus = null
        cachedPort = null
        cachedAtMillis = 0L
    }

    private fun String?.toPortOrNull(): Int? {
        val value = this?.trim()?.toIntOrNull() ?: return null
        return value.takeIf { it in 1..65535 }
    }
}
