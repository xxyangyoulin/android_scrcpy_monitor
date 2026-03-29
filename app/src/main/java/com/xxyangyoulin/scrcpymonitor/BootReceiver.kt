package com.xxyangyoulin.scrcpymonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        if (!MonitorSettings.isEnabled(context)) {
            return
        }
        if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScrcpyMonitorService::class.java)
            )
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            return
        }
        scheduleDelayedStart(context)
    }

    private fun scheduleDelayedStart(context: Context) {
        val pendingIntent = PendingIntent.getForegroundService(
            context,
            0,
            Intent(context, ScrcpyMonitorService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + BOOT_START_DELAY_MS,
            pendingIntent
        )
    }

    companion object {
        private const val BOOT_START_DELAY_MS = 12_000L
    }
}
