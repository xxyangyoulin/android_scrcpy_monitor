package com.xxyangyoulin.scrcpymonitor

import android.content.Context

object WifiAccessControlManager {
    private const val CHAIN_NAME = "SCRCPY_MONITOR_ADB"

    fun sync(context: Context, wifiEnabled: Boolean, port: Int?): Boolean {
        val limitEnabled = MonitorSettings.isWifiAccessLimitEnabled(context)
        val allowedIp = MonitorSettings.getWifiAccessLimitIp(context)
        return sync(limitEnabled, allowedIp, wifiEnabled, port)
    }

    fun sync(
        limitEnabled: Boolean,
        allowedIp: String?,
        wifiEnabled: Boolean,
        port: Int?
    ): Boolean {
        val commands = mutableListOf(
            "iptables -N $CHAIN_NAME 2>/dev/null || true",
            "iptables -C INPUT -p tcp -j $CHAIN_NAME 2>/dev/null || iptables -I INPUT -p tcp -j $CHAIN_NAME",
            "iptables -F $CHAIN_NAME"
        )
        if (limitEnabled && wifiEnabled && port != null && isValidIpv4(allowedIp)) {
            commands += "iptables -A $CHAIN_NAME -p tcp --dport $port -s $allowedIp -j RETURN"
            commands += "iptables -A $CHAIN_NAME -p tcp --dport $port -j REJECT"
        }
        commands += "iptables -A $CHAIN_NAME -j RETURN"
        return RootShell.succeeded(RootShell.run(commands))
    }

    fun isValidIpv4(ip: String?): Boolean {
        val parts = ip?.trim()?.split(".") ?: return false
        if (parts.size != 4) {
            return false
        }
        return parts.all { part ->
            val value = part.toIntOrNull()
            value != null && value in 0..255
        }
    }
}
