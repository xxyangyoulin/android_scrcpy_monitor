package com.xxyangyoulin.scrcpymonitor

import android.content.Context

object MonitorSettings {
    private const val PREFS_NAME = "monitor_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_DISABLE_ANIMATIONS = "disable_animations"
    private const val KEY_ANIMATIONS_DISABLED_APPLIED = "animations_disabled_applied"
    private const val KEY_LAST_CONNECTED_AT = "last_connected_at"
    private const val KEY_LAST_ENDPOINT = "last_endpoint"
    private const val KEY_LAST_DISPLAY_ENDPOINT = "last_display_endpoint"
    private const val KEY_WIFI_DEBUGGING_PORT = "wifi_debugging_port"
    private const val KEY_WIFI_ACCESS_LIMIT_ENABLED = "wifi_access_limit_enabled"
    private const val KEY_WIFI_ACCESS_LIMIT_IP = "wifi_access_limit_ip"

    const val DEFAULT_WIFI_DEBUGGING_PORT = 5555

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun shouldDisableAnimations(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DISABLE_ANIMATIONS, true)
    }

    fun setDisableAnimations(context: Context, enabled: Boolean) {
        prefs(context)
            .edit()
            .putBoolean(KEY_DISABLE_ANIMATIONS, enabled)
            .apply()
    }

    fun areAnimationsDisabledApplied(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ANIMATIONS_DISABLED_APPLIED, false)
    }

    fun setAnimationsDisabledApplied(context: Context, applied: Boolean) {
        prefs(context)
            .edit()
            .putBoolean(KEY_ANIMATIONS_DISABLED_APPLIED, applied)
            .apply()
    }

    fun setLastConnection(
        context: Context,
        timestamp: Long,
        endpoint: String?,
        displayEndpoint: String?
    ) {
        prefs(context)
            .edit()
            .putLong(KEY_LAST_CONNECTED_AT, timestamp)
            .putString(KEY_LAST_ENDPOINT, endpoint)
            .putString(KEY_LAST_DISPLAY_ENDPOINT, displayEndpoint)
            .apply()
    }

    fun getLastConnectedAt(context: Context): Long {
        return prefs(context).getLong(KEY_LAST_CONNECTED_AT, 0L)
    }

    fun getLastEndpoint(context: Context): String? {
        return prefs(context).getString(KEY_LAST_ENDPOINT, null)
    }

    fun getLastDisplayEndpoint(context: Context): String? {
        return prefs(context).getString(KEY_LAST_DISPLAY_ENDPOINT, null)
    }

    fun getWifiDebuggingPort(context: Context): Int {
        return prefs(context)
            .getInt(KEY_WIFI_DEBUGGING_PORT, DEFAULT_WIFI_DEBUGGING_PORT)
            .coerceIn(1, 65535)
    }

    fun setWifiDebuggingPort(context: Context, port: Int) {
        prefs(context)
            .edit()
            .putInt(KEY_WIFI_DEBUGGING_PORT, port.coerceIn(1, 65535))
            .apply()
    }

    fun isWifiAccessLimitEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_WIFI_ACCESS_LIMIT_ENABLED, false)
    }

    fun setWifiAccessLimitEnabled(context: Context, enabled: Boolean) {
        prefs(context)
            .edit()
            .putBoolean(KEY_WIFI_ACCESS_LIMIT_ENABLED, enabled)
            .apply()
    }

    fun getWifiAccessLimitIp(context: Context): String? {
        return prefs(context).getString(KEY_WIFI_ACCESS_LIMIT_IP, null)
    }

    fun setWifiAccessLimitIp(context: Context, ip: String) {
        prefs(context)
            .edit()
            .putString(KEY_WIFI_ACCESS_LIMIT_IP, ip)
            .apply()
    }
}
