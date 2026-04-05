package com.xxyangyoulin.scrcpymonitor

data class MainUiState(
    val monitorEnabled: Boolean = false,
    val monitorStatusText: String = "",
    val connectionStatusText: String = "",
    val connectionSubtitleText: String = "",
    val connected: Boolean = false,
    val infoPrimaryLabel: String = "",
    val infoPrimaryValue: String = "",
    val infoSecondaryLabel: String = "",
    val infoSecondaryValue: String = "",
    val wifiDebuggingEnabled: Boolean = false,
    val wifiDebuggingSwitchEnabled: Boolean = true,
    val wifiDebuggingPort: String = "",
    val stayAwakeEnabled: Boolean = false,
    val screenOnEnabled: Boolean = false,
    val wifiAccessLimitEnabled: Boolean = false,
    val wifiAccessLimitIp: String = "",
    val disableAnimationsEnabled: Boolean = true,
    val disconnectEnabled: Boolean = false,
    val rootAvailable: Boolean = false,
    val disableAnimationsSubtitle: String = ""
)
