package com.xxyangyoulin.scrcpymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class WifiDebuggingToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val enabled =
            WifiDebuggingManager.getStatus(forceRefresh = true) != WifiDebuggingManager.Status.ENABLED
        val success = WifiDebuggingManager.setEnabled(context, enabled)
        val message = if (success) {
            if (enabled) {
                R.string.toast_wifi_debugging_enabled
            } else {
                R.string.toast_wifi_debugging_disabled
            }
        } else {
            R.string.toast_wifi_debugging_failed
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        ScrcpyMonitorService.refreshNotification(context, force = true)
    }
}
