package com.xxyangyoulin.scrcpymonitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateUtils
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private val uiHandler = Handler(Looper.getMainLooper())
    private var uiState by mutableStateOf(MainUiState())
    private var ignoreWifiSwitchChanges = false
    private var ignoreAnimationSwitchChanges = false
    private var uiRefreshRunning = false
    private var monitorRunning = false
    private var scrcpyStatus = ScrcpyStateDetector.Status.UNKNOWN
    private var wifiDebuggingPort = MonitorSettings.DEFAULT_WIFI_DEBUGGING_PORT
    private var lastWifiDebuggingRefreshAt = 0L
    private var currentDisplayEndpoint: String? = null
    private var disconnectInProgress = false
    private var connectedAtMillis: Long? = null
    private val uiRefreshRunnable = object : Runnable {
        override fun run() {
            refreshConnectionState()
            val now = System.currentTimeMillis()
            if (now - lastWifiDebuggingRefreshAt >= WIFI_DEBUGGING_REFRESH_INTERVAL_MS) {
                refreshWifiDebuggingState()
            }
            if (uiRefreshRunning) {
                uiHandler.postDelayed(this, UI_REFRESH_INTERVAL_MS)
            }
        }
    }

    private val monitorStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            applyRuntimeState(
                MonitorRuntimeState.update(
                    running = intent?.getBooleanExtra(
                        ScrcpyMonitorService.EXTRA_IS_RUNNING,
                        false
                    ) == true,
                    snapshot = snapshotFromIntent(intent)
                )
            )
            renderState()
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                startMonitor()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateSystemBarAppearance()
        applyRuntimeState(MonitorRuntimeState.current())
        renderAnimationToggle()
        renderState()
        setContent {
            ScrcpyMonitorTheme {
                MainScreen(
                    uiState = uiState,
                    onToggleMonitor = ::toggleMonitor,
                    onOpenDeveloperOptions = ::openDeveloperOptions,
                    onDisconnect = {
                        if (uiState.disconnectEnabled) {
                            disconnectScrcpy()
                        }
                    },
                    onWifiDebuggingChange = { enabled ->
                        if (!ignoreWifiSwitchChanges) {
                            setWifiDebuggingEnabled(enabled)
                        }
                    },
                    onWifiDebuggingPortChange = ::setWifiDebuggingPort,
                    onDisableAnimationsChange = { enabled ->
                        if (!ignoreAnimationSwitchChanges) {
                            setDisableAnimationsEnabled(enabled)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ensureMonitorRunningIfEnabled()
        applyRuntimeState(MonitorRuntimeState.current())
        refreshConnectionState()
        renderState()
        refreshWifiDebuggingState()
        renderAnimationToggle()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            monitorStateReceiver,
            IntentFilter(ScrcpyMonitorService.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        uiRefreshRunning = true
        uiHandler.post(uiRefreshRunnable)
        updateMonitorUiActiveState(true)
    }

    override fun onStop() {
        uiRefreshRunning = false
        uiHandler.removeCallbacks(uiRefreshRunnable)
        unregisterReceiver(monitorStateReceiver)
        updateMonitorUiActiveState(false)
        super.onStop()
    }

    private fun startMonitor() {
        MonitorSettings.setEnabled(this, true)
        ContextCompat.startForegroundService(
            this,
            Intent(this, ScrcpyMonitorService::class.java)
        )
        showToast(R.string.toast_started)
        renderState()
    }

    private fun stopMonitor() {
        MonitorSettings.setEnabled(this, false)
        stopService(Intent(this, ScrcpyMonitorService::class.java))
        val snapshot = ScrcpyStateDetector.getSnapshot()
        if (snapshot.status != ScrcpyStateDetector.Status.UNKNOWN) {
            applyRuntimeState(
                MonitorRuntimeState.update(running = false, snapshot = snapshot)
            )
        }
        showToast(R.string.toast_stopped)
        renderState()
    }

    private fun toggleMonitor() {
        if (MonitorSettings.isEnabled(this)) {
            stopMonitor()
        } else if (needsNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitor()
        }
    }

    private fun openDeveloperOptions() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        } catch (_: Exception) {
            showToast(R.string.toast_open_settings_failed)
        }
    }

    private fun disconnectScrcpy() {
        disconnectInProgress = true
        renderState()
        runInBackground {
            val disconnected = RootDisconnect.tryDisableAdb()
            runOnUiThread {
                disconnectInProgress = false
                showToast(
                    if (disconnected) {
                        R.string.toast_disconnect_success
                    } else {
                        R.string.toast_disconnect_failed
                    }
                )
                refreshStateAfterDisconnect()
            }
        }
    }

    private fun refreshStateAfterDisconnect() {
        renderState()
        refreshWifiDebuggingState()
        scheduleStateRefresh(1200)
        scheduleStateRefresh(2500)
    }

    private fun renderState() {
        uiState = buildUiState()
    }

    private fun refreshConnectionState() {
        runInBackground {
            val snapshot = ScrcpyStateDetector.getSnapshot()
            val displayEndpoint = EndpointFormatter.format(snapshot.endpoint)
            val runtimeState = MonitorRuntimeState.update(
                running = ScrcpyMonitorService.isRunning(this),
                snapshot = snapshot
            )
            runOnUiThread {
                applyRuntimeState(runtimeState, displayEndpoint)
                renderState()
            }
        }
    }

    private fun ensureMonitorRunningIfEnabled() {
        if (!MonitorSettings.isEnabled(this) || ScrcpyMonitorService.isRunning(this)) {
            return
        }
        monitorRunning = false
        scrcpyStatus = ScrcpyStateDetector.Status.UNKNOWN
        ContextCompat.startForegroundService(
            this,
            Intent(this, ScrcpyMonitorService::class.java)
        )
    }

    private fun refreshWifiDebuggingState() {
        lastWifiDebuggingRefreshAt = System.currentTimeMillis()
        runInBackground {
            val status = WifiDebuggingManager.getStatus(forceRefresh = true)
            val port = WifiDebuggingManager.getPort()
            runOnUiThread {
                renderWifiDebuggingState(status, port)
            }
        }
    }

    private fun setWifiDebuggingEnabled(enabled: Boolean) {
        renderWifiDebuggingSwitchEnabled(false)
        runInBackground {
            val success = WifiDebuggingManager.setEnabled(this, enabled)
            val status = WifiDebuggingManager.getStatus(forceRefresh = true)
            val port = WifiDebuggingManager.getPort()
            runOnUiThread {
                renderWifiDebuggingState(status, port)
                renderWifiDebuggingSwitchEnabled(true)
                showToast(
                    if (success) {
                        if (enabled) {
                            R.string.toast_wifi_debugging_enabled
                        } else {
                            R.string.toast_wifi_debugging_disabled
                        }
                    } else {
                        R.string.toast_wifi_debugging_failed
                    }
                )
            }
        }
    }

    private fun renderWifiDebuggingState(status: WifiDebuggingManager.Status, port: Int?) {
        ignoreWifiSwitchChanges = true
        wifiDebuggingPort = port ?: MonitorSettings.getWifiDebuggingPort(this)
        uiState =
            uiState.copy(
                wifiDebuggingEnabled = status == WifiDebuggingManager.Status.ENABLED,
                wifiDebuggingPort = wifiDebuggingPort.toString()
            )
        ignoreWifiSwitchChanges = false
    }

    private fun renderWifiDebuggingSwitchEnabled(enabled: Boolean) {
        uiState = uiState.copy(wifiDebuggingSwitchEnabled = enabled)
    }

    private fun renderAnimationToggle() {
        ignoreAnimationSwitchChanges = true
        uiState = uiState.copy(disableAnimationsEnabled = MonitorSettings.shouldDisableAnimations(this))
        ignoreAnimationSwitchChanges = false
    }

    private fun setDisableAnimationsEnabled(enabled: Boolean) {
        MonitorSettings.setDisableAnimations(this, enabled)
        syncAnimationPreference(enabled)
        renderAnimationToggle()
        ScrcpyMonitorService.refreshNotification(this, force = true)
    }

    private fun setWifiDebuggingPort(portText: String) {
        val port = portText.toIntOrNull()
        if (port == null || port !in 1..65535) {
            showToast(R.string.toast_wifi_debugging_port_invalid)
            return
        }
        MonitorSettings.setWifiDebuggingPort(this, port)
        wifiDebuggingPort = port
        renderState()
        runInBackground {
            val success =
                if (uiState.wifiDebuggingEnabled) {
                    WifiDebuggingManager.setEnabled(enabled = true, port = port)
                } else {
                    true
                }
            val status = WifiDebuggingManager.getStatus(forceRefresh = true)
            val currentPort = WifiDebuggingManager.getPort()
            runOnUiThread {
                renderWifiDebuggingState(status, currentPort)
                if (success) {
                    showToast(R.string.toast_wifi_debugging_port_updated)
                } else {
                    showToast(R.string.toast_wifi_debugging_failed)
                }
            }
        }
    }

    private fun syncAnimationPreference(disableAnimations: Boolean) {
        if (disableAnimations && scrcpyStatus != ScrcpyStateDetector.Status.CONNECTED) {
            return
        }
        runInBackground {
            if (AnimationScaleManager.setEnabled(!disableAnimations)) {
                MonitorSettings.setAnimationsDisabledApplied(this, disableAnimations)
            }
        }
    }

    private fun getMonitorStatusText(monitoringEnabled: Boolean): String {
        return if (monitorRunning) {
            getString(R.string.monitor_running)
        } else if (monitoringEnabled) {
            getString(R.string.monitor_enabled)
        } else {
            getString(R.string.monitor_disabled)
        }
    }

    private fun getConnectionStatusText(monitoringEnabled: Boolean): String {
        return when (scrcpyStatus) {
            ScrcpyStateDetector.Status.CONNECTED -> getString(R.string.notification_title_connected)
            ScrcpyStateDetector.Status.DISCONNECTED ->
                if (monitoringEnabled) {
                    getString(R.string.notification_title_monitoring)
                } else {
                    getString(R.string.status_idle)
                }
            ScrcpyStateDetector.Status.UNKNOWN ->
                if (monitoringEnabled || monitorRunning) {
                    getString(R.string.notification_title_monitoring)
                } else {
                    getString(R.string.status_idle)
                }
        }
    }

    private fun buildUiState(): MainUiState {
        val monitoringEnabled = MonitorSettings.isEnabled(this)
        val connected = scrcpyStatus == ScrcpyStateDetector.Status.CONNECTED
        val now = System.currentTimeMillis()
        val lastConnectedAt = MonitorSettings.getLastConnectedAt(this)
        val secondaryLabelRes =
            if (connected) {
                R.string.info_label_connected_duration
            } else {
                R.string.info_label_last_connection
            }
        val secondaryValue =
            if (connected) {
                formatConnectionDuration(now)
            } else {
                formatLastSeen(lastConnectedAt)
            }
        return uiState.copy(
            monitorEnabled = monitoringEnabled,
            monitorStatusText = getMonitorStatusText(monitoringEnabled),
            connectionStatusText = getConnectionStatusText(monitoringEnabled),
            connectionSubtitleText = getConnectionSubtitleText(connected),
            connected = connected,
            infoPrimaryLabel = getString(R.string.info_label_source),
            infoPrimaryValue =
                if (connected) {
                    currentDisplayEndpoint ?: getString(R.string.info_value_unavailable)
                } else {
                    getString(R.string.info_value_unavailable)
                },
            infoSecondaryLabel = getString(secondaryLabelRes),
            infoSecondaryValue = secondaryValue,
            disconnectEnabled = connected && !disconnectInProgress,
            wifiDebuggingPort = wifiDebuggingPort.toString(),
            disableAnimationsSubtitle = getString(R.string.summary_disable_animations)
        )
    }

    private fun applyConnectionSnapshot(
        snapshot: ScrcpyStateDetector.Snapshot,
        displayEndpoint: String? = EndpointFormatter.format(snapshot.endpoint)
    ) {
        scrcpyStatus = snapshot.status
        currentDisplayEndpoint = displayEndpoint
    }

    private fun applyRuntimeState(
        runtimeState: MonitorRuntimeState.State,
        displayEndpoint: String? = EndpointFormatter.format(runtimeState.snapshot.endpoint)
    ) {
        monitorRunning = runtimeState.running
        connectedAtMillis = runtimeState.connectedAtMillis
        applyConnectionSnapshot(runtimeState.snapshot, displayEndpoint)
    }

    private fun getConnectionSubtitleText(connected: Boolean): String {
        return getString(
            if (connected) {
                R.string.connection_subtitle_connected
            } else {
                R.string.connection_subtitle_waiting
            }
        )
    }

    private fun formatConnectionDuration(now: Long): String {
        val connectedAt = connectedAtMillis ?: return getString(R.string.info_value_unavailable)
        val durationMillis = (now - connectedAt).coerceAtLeast(0L)
        val totalSeconds = durationMillis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun formatLastSeen(lastConnectedAt: Long): String {
        if (lastConnectedAt <= 0L) {
            return getString(R.string.info_value_unavailable)
        }
        return DateUtils.getRelativeTimeSpanString(
            lastConnectedAt,
            System.currentTimeMillis(),
            MINUTE_IN_MILLIS
        ).toString()
    }

    private fun snapshotFromIntent(intent: Intent?): ScrcpyStateDetector.Snapshot {
        return ScrcpyStateDetector.Snapshot(
            status =
                intent?.getStringExtra(ScrcpyMonitorService.EXTRA_SCRCPY_STATUS)
                    ?.let { runCatching { ScrcpyStateDetector.Status.valueOf(it) }.getOrNull() }
                    ?: ScrcpyStateDetector.Status.UNKNOWN,
            endpoint = intent?.getStringExtra(ScrcpyMonitorService.EXTRA_ENDPOINT)
        )
    }

    private fun scheduleStateRefresh(delayMillis: Long) {
        uiHandler.postDelayed({
            refreshConnectionState()
            refreshWifiDebuggingState()
        }, delayMillis)
    }

    private fun updateMonitorUiActiveState(active: Boolean) {
        if (MonitorSettings.isEnabled(this)) {
            ScrcpyMonitorService.refreshNotification(this, force = active, uiActive = active)
        }
    }

    private fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
    }

    private fun showToast(@StringRes messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun updateSystemBarAppearance() {
        val useLightSystemBars =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = useLightSystemBars
            isAppearanceLightNavigationBars = useLightSystemBars
        }
    }

    private fun runInBackground(action: () -> Unit) {
        Thread(action).start()
    }

    companion object {
        private const val UI_REFRESH_INTERVAL_MS = 1_000L
        private const val WIFI_DEBUGGING_REFRESH_INTERVAL_MS = 5_000L
    }
}
