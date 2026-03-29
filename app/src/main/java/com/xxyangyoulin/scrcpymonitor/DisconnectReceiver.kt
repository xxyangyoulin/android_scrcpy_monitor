package com.xxyangyoulin.scrcpymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DisconnectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val disconnected = RootDisconnect.tryDisableAdb()
        val message = if (disconnected) {
            R.string.toast_disconnect_success
        } else {
            R.string.toast_disconnect_failed
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        WifiDebuggingManager.invalidateStatusCache()
        ScrcpyMonitorService.refreshNotification(context, force = true)
    }
}
