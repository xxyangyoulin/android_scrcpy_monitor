package com.xxyangyoulin.scrcpymonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ScrcpyMonitorService : Service() {
    private val pollThread = HandlerThread("scrcpy-monitor")
    private lateinit var handler: Handler
    private var pendingForceRefresh = false
    private var uiActive = false
    private var lastSnapshot: ScrcpyStateDetector.Snapshot? = null
    private var idlePollCount = 0
    private val pollRunnable = object : Runnable {
        override fun run() {
            var status = ScrcpyStateDetector.Status.UNKNOWN
            try {
                val forceRefresh = pendingForceRefresh
                pendingForceRefresh = false
                status = updateNotification(forceRefresh)
            } catch (t: Throwable) {
                DebugLog.e("Monitor poll failed", t)
            } finally {
                val delay = getPollDelayMillis(status)
                DebugLog.d("poll scheduled in ${delay}ms, status=$status, uiActive=$uiActive")
                handler.postDelayed(this, delay)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        pollThread.start()
        handler = Handler(pollThread.looper)
        createChannels()
        startForeground(ONGOING_NOTIFICATION_ID, buildBootstrapNotification(this))
        val initialSnapshot = ScrcpyStateDetector.Snapshot(ScrcpyStateDetector.Status.UNKNOWN, null)
        MonitorRuntimeState.update(running = true, snapshot = initialSnapshot)
        notifyStateChanged(initialSnapshot)
        handler.post(pollRunnable)
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        pollThread.quitSafely()
        restoreAnimationsIfNeeded()
        notifyStopped()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_FORCE_REFRESH, false) == true) {
            pendingForceRefresh = true
        }
        if (intent?.hasExtra(EXTRA_UI_ACTIVE) == true) {
            uiActive = intent.getBooleanExtra(EXTRA_UI_ACTIVE, false)
            DebugLog.d("uiActive changed to $uiActive")
        }
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(forceRefresh: Boolean): ScrcpyStateDetector.Status {
        val manager = getSystemService(NotificationManager::class.java)
        val snapshot = ScrcpyStateDetector.getSnapshot()
        val previousSnapshot = lastSnapshot
        syncAnimationState(snapshot.status)
        storeLastConnection(snapshot, previousSnapshot)
        updateIdlePollCount(snapshot.status, previousSnapshot?.status)
        MonitorRuntimeState.update(running = true, snapshot = snapshot)
        if (forceRefresh || snapshot != previousSnapshot) {
            DebugLog.d("state changed to ${snapshot.status}, endpoint=${snapshot.endpoint ?: "-"}")
            manager.notify(ONGOING_NOTIFICATION_ID, buildOngoingNotification(this, snapshot))
            notifyStateChanged(snapshot)
            lastSnapshot = snapshot
        }
        return snapshot.status
    }

    private fun storeLastConnection(
        snapshot: ScrcpyStateDetector.Snapshot,
        previousSnapshot: ScrcpyStateDetector.Snapshot?
    ) {
        if (
            snapshot.status != ScrcpyStateDetector.Status.CONNECTED ||
            (
                previousSnapshot?.status == ScrcpyStateDetector.Status.CONNECTED &&
                    previousSnapshot.endpoint == snapshot.endpoint
                )
        ) {
            return
        }
        MonitorSettings.setLastConnection(
            this,
            System.currentTimeMillis(),
            snapshot.endpoint,
            EndpointFormatter.format(snapshot.endpoint)
        )
    }

    private fun syncAnimationState(
        currentStatus: ScrcpyStateDetector.Status
    ) {
        val shouldDisableAnimations =
            MonitorSettings.shouldDisableAnimations(this) &&
                currentStatus == ScrcpyStateDetector.Status.CONNECTED
        val applied = MonitorSettings.areAnimationsDisabledApplied(this)
        if (shouldDisableAnimations == applied) {
            return
        }
        if (AnimationScaleManager.setEnabled(!shouldDisableAnimations)) {
            MonitorSettings.setAnimationsDisabledApplied(this, shouldDisableAnimations)
        }
    }

    private fun restoreAnimationsIfNeeded() {
        if (!MonitorSettings.areAnimationsDisabledApplied(this)) {
            return
        }
        Thread {
            if (AnimationScaleManager.setEnabled(true)) {
                MonitorSettings.setAnimationsDisabledApplied(this, false)
            }
        }.start()
    }

    private fun notifyStateChanged(snapshot: ScrcpyStateDetector.Snapshot) {
        sendBroadcast(
            Intent(ACTION_STATE_CHANGED).apply {
                setPackage(packageName)
                putExtra(EXTRA_IS_RUNNING, true)
                putExtra(EXTRA_SCRCPY_STATUS, snapshot.status.name)
                putExtra(EXTRA_ENDPOINT, snapshot.endpoint)
            }
        )
    }

    private fun notifyStopped() {
        MonitorRuntimeState.markStopped()
        sendBroadcast(
            Intent(ACTION_STATE_CHANGED).apply {
                setPackage(packageName)
                putExtra(EXTRA_IS_RUNNING, false)
                putExtra(EXTRA_SCRCPY_STATUS, ScrcpyStateDetector.Status.DISCONNECTED.name)
            }
        )
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val ongoingChannel = NotificationChannel(
            ONGOING_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(ongoingChannel)
    }

    private fun getPollDelayMillis(status: ScrcpyStateDetector.Status): Long {
        return if (uiActive) {
            UI_POLL_INTERVAL_MS
        } else if (status == ScrcpyStateDetector.Status.CONNECTED) {
            CONNECTED_POLL_INTERVAL_MS
        } else if (idlePollCount < IDLE_POLL_FAST_WINDOW_COUNT) {
            RECENT_IDLE_POLL_INTERVAL_MS
        } else {
            IDLE_POLL_INTERVAL_MS
        }
    }

    private fun updateIdlePollCount(
        currentStatus: ScrcpyStateDetector.Status,
        previousStatus: ScrcpyStateDetector.Status?
    ) {
        idlePollCount = when {
            currentStatus == ScrcpyStateDetector.Status.CONNECTED -> 0
            previousStatus == ScrcpyStateDetector.Status.CONNECTED -> 0
            else -> (idlePollCount + 1).coerceAtMost(IDLE_POLL_FAST_WINDOW_COUNT)
        }
    }

    companion object {
        const val ACTION_STATE_CHANGED =
            "com.xxyangyoulin.scrcpymonitor.action.MONITOR_STATE_CHANGED"
        const val EXTRA_IS_RUNNING = "extra_is_running"
        const val EXTRA_SCRCPY_STATUS = "extra_scrcpy_status"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        private const val EXTRA_FORCE_REFRESH = "extra_force_refresh"
        private const val EXTRA_UI_ACTIVE = "extra_ui_active"
        private const val ONGOING_CHANNEL_ID = "scrcpy_monitor_ongoing"
        private const val ONGOING_NOTIFICATION_ID = 1001
        private const val UI_POLL_INTERVAL_MS = 1_000L
        private const val CONNECTED_POLL_INTERVAL_MS = 4_000L
        private const val RECENT_IDLE_POLL_INTERVAL_MS = 5_000L
        private const val IDLE_POLL_INTERVAL_MS = 15_000L
        private const val IDLE_POLL_FAST_WINDOW_COUNT = 6

        @Volatile
        var isRunning: Boolean = false
            private set

        fun isRunning(context: Context): Boolean {
            if (isRunning) {
                return true
            }
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            return manager.getRunningServices(Int.MAX_VALUE).any {
                it.service.className == ScrcpyMonitorService::class.java.name
            }
        }

        fun refreshNotification(
            context: Context,
            force: Boolean = false,
            uiActive: Boolean? = null
        ) {
            val intent = Intent(context, ScrcpyMonitorService::class.java).apply {
                putExtra(EXTRA_FORCE_REFRESH, force)
                if (uiActive != null) {
                    putExtra(EXTRA_UI_ACTIVE, uiActive)
                }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        private fun buildOngoingNotification(
            context: Context,
            snapshot: ScrcpyStateDetector.Snapshot = ScrcpyStateDetector.getSnapshot()
        ): Notification {
            val status = snapshot.status
            val connected = status == ScrcpyStateDetector.Status.CONNECTED
            val wifiStatus = WifiDebuggingManager.getCachedStatus()
            val openAppIntent = createOpenAppPendingIntent(context)

            val builder = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_scrcpy)
                .setContentTitle(context.getString(getNotificationTitleRes(status)))
                .setContentText(getNotificationContentText(context, snapshot))
                .setContentIntent(openAppIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)

            if (connected) {
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.action_disconnect),
                    createBroadcastPendingIntent(context, 1, DisconnectReceiver::class.java)
                )
            }
            builder.addAction(
                android.R.drawable.ic_menu_manage,
                context.getString(
                    if (wifiStatus == WifiDebuggingManager.Status.ENABLED) {
                        R.string.action_disable_wifi_debugging
                    } else {
                        R.string.action_enable_wifi_debugging
                    }
                ),
                createBroadcastPendingIntent(context, 2, WifiDebuggingToggleReceiver::class.java)
            )

            return builder.build()
        }

        private fun buildBootstrapNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_scrcpy)
                .setContentTitle(context.getString(R.string.notification_title_monitoring))
                .setContentText(context.getString(R.string.notification_text_bootstrap))
                .setContentIntent(createOpenAppPendingIntent(context))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .build()
        }

        private fun createOpenAppPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createBroadcastPendingIntent(
            context: Context,
            requestCode: Int,
            receiverClass: Class<out android.content.BroadcastReceiver>
        ): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, receiverClass),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun getNotificationTitleRes(status: ScrcpyStateDetector.Status): Int {
            return when (status) {
                ScrcpyStateDetector.Status.CONNECTED -> R.string.notification_title_connected
                ScrcpyStateDetector.Status.DISCONNECTED,
                ScrcpyStateDetector.Status.UNKNOWN -> R.string.notification_title_monitoring
            }
        }

        private fun getNotificationContentText(
            context: Context,
            snapshot: ScrcpyStateDetector.Snapshot
        ): String {
            return when (snapshot.status) {
                ScrcpyStateDetector.Status.CONNECTED ->
                    if (snapshot.endpoint == null) {
                        context.getString(R.string.notification_text_connected)
                    } else {
                        context.getString(
                            R.string.notification_text_connected_with_endpoint,
                            EndpointFormatter.format(snapshot.endpoint)
                        )
                    }
                ScrcpyStateDetector.Status.DISCONNECTED ->
                    context.getString(R.string.notification_text_monitoring_idle)
                ScrcpyStateDetector.Status.UNKNOWN ->
                    context.getString(R.string.notification_text_monitoring_unknown)
            }
        }
    }
}
