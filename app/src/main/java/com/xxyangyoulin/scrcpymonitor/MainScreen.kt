package com.xxyangyoulin.scrcpymonitor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onToggleMonitor: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onDisconnect: () -> Unit,
    onWifiDebuggingChange: (Boolean) -> Unit,
    onWifiDebuggingPortChange: (String) -> Unit,
    onStayAwakeChange: (Boolean) -> Unit,
    onScreenOnChange: (Boolean) -> Unit,
    onWifiAccessLimitChange: (Boolean) -> Unit,
    onWifiAccessLimitIpChange: (String) -> Unit,
    onDisableAnimationsChange: (Boolean) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    colorResource(
                                        if (uiState.monitorEnabled) {
                                            R.color.statusMonitoringDot
                                        } else {
                                            R.color.statusDisconnected
                                        }
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.monitorStatusText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colorResource(R.color.settingsIconBackground))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (uiState.monitorEnabled) {
                                                R.string.action_stop_monitoring
                                            } else {
                                                R.string.action_start_monitoring
                                            }
                                        )
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleMonitor()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_open_dev_options)) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenDeveloperOptions()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            colorResource(
                                if (uiState.rootAvailable) {
                                    R.color.footerDot
                                } else {
                                    R.color.statusDisconnected
                                }
                            )
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (uiState.rootAvailable) {
                            R.string.status_root_ready
                        } else {
                            R.string.status_root_missing
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroCard(uiState = uiState, onDisconnect = onDisconnect)
            ToolsCard(
                uiState = uiState,
                onWifiDebuggingChange = onWifiDebuggingChange,
                onWifiDebuggingPortChange = onWifiDebuggingPortChange,
                onStayAwakeChange = onStayAwakeChange,
                onScreenOnChange = onScreenOnChange,
                onWifiAccessLimitChange = onWifiAccessLimitChange,
                onWifiAccessLimitIpChange = onWifiAccessLimitIpChange,
                onDisableAnimationsChange = onDisableAnimationsChange
            )
        }
    }
}

@Composable
private fun HeroCard(
    uiState: MainUiState,
    onDisconnect: () -> Unit
) {
    val connected = uiState.connected
    val cardBackground = colorResource(
        if (connected) {
            R.color.heroConnectedBackground
        } else {
            R.color.heroDisconnectedBackground
        }
    )
    val cardBorder = colorResource(
        if (connected) {
            R.color.heroConnectedBorder
        } else {
            R.color.heroDisconnectedBorder
        }
    )
    val titleColor = colorResource(
        if (connected) {
            R.color.statusConnected
        } else {
            R.color.statusDisconnected
        }
    )
    val buttonBackground = colorResource(
        if (connected) {
            R.color.heroButtonConnectedBackground
        } else {
            R.color.heroButtonDisconnectedBackground
        }
    )
    val buttonTextColor = colorResource(
        if (connected) {
            R.color.heroButtonConnectedText
        } else {
            R.color.heroButtonDisconnectedText
        }
    )
    val buttonBorderWidth = if (connected) 1.5.dp else 0.dp
    val buttonBorderColor =
        if (connected) {
            colorResource(R.color.heroButtonConnectedBorder)
        } else {
            Color.Transparent
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            HeroIcon(connected = connected, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(
                text = uiState.connectionStatusText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                color = titleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
            Text(
                text = uiState.connectionSubtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = titleColor.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    label = uiState.infoPrimaryLabel,
                    value = uiState.infoPrimaryValue,
                    connected = connected,
                    modifier = Modifier.weight(1f)
                )
                InfoChip(
                    label = uiState.infoSecondaryLabel,
                    value = uiState.infoSecondaryValue,
                    connected = connected,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = onDisconnect,
                enabled = uiState.disconnectEnabled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBackground,
                    contentColor = buttonTextColor,
                    disabledContainerColor = colorResource(R.color.heroButtonDisconnectedBackground),
                    disabledContentColor = colorResource(R.color.heroButtonDisconnectedText)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(52.dp)
                    .border(
                        width = buttonBorderWidth,
                        color = buttonBorderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Text(
                    text = stringResource(
                        if (connected) {
                            R.string.action_disconnect
                        } else {
                            R.string.action_waiting
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun HeroIcon(connected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 20.dp)
            .size(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                colorResource(
                    if (connected) {
                        R.color.iconConnectedBackground
                    } else {
                        R.color.iconDisconnectedBackground
                    }
                )
            )
            .border(
                width = if (connected) 1.5.dp else 1.dp,
                color = colorResource(
                    if (connected) {
                        R.color.heroConnectedBorder
                    } else {
                        R.color.heroDisconnectedBorder
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                if (connected) {
                    R.drawable.ic_hero_connected
                } else {
                    R.drawable.ic_hero_disconnected
                }
            ),
            contentDescription = null,
            tint = colorResource(
                if (connected) {
                    R.color.statusConnected
                } else {
                    R.color.statusDisconnected
                }
            ),
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = colorResource(
            if (connected) {
                R.color.infoChipConnectedBackground
            } else {
                R.color.infoChipDisconnectedBackground
            }
        ),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        modifier = modifier.border(
            width = 1.dp,
            color = colorResource(
                if (connected) {
                    R.color.heroConnectedBorder
                } else {
                    R.color.heroDisconnectedBorder
                }
            ),
            shape = RoundedCornerShape(14.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = colorResource(
                    if (connected) {
                        R.color.statusConnected
                    } else {
                        R.color.statusDisconnected
                    }
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorResource(R.color.infoLabelText),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun ToolsCard(
    uiState: MainUiState,
    onWifiDebuggingChange: (Boolean) -> Unit,
    onWifiDebuggingPortChange: (String) -> Unit,
    onStayAwakeChange: (Boolean) -> Unit,
    onScreenOnChange: (Boolean) -> Unit,
    onWifiAccessLimitChange: (Boolean) -> Unit,
    onWifiAccessLimitIpChange: (String) -> Unit,
    onDisableAnimationsChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.settingsCardBackground)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorResource(R.color.heroDisconnectedBorder), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingRow(
                iconRes = R.drawable.ic_wifi_debug,
                title = stringResource(R.string.title_wifi_debugging),
                subtitle = "",
                detailText = stringResource(R.string.label_wifi_debugging_port, uiState.wifiDebuggingPort),
                checked = uiState.wifiDebuggingEnabled,
                enabled = uiState.wifiDebuggingSwitchEnabled,
                detailDialogTitle = stringResource(R.string.dialog_wifi_debugging_port_title),
                detailDialogLabel = stringResource(R.string.dialog_wifi_debugging_port_label),
                detailKeyboardType = KeyboardType.Number,
                onDetailChange = onWifiDebuggingPortChange,
                onCheckedChange = onWifiDebuggingChange
            )
            DividerSpacer()
            SettingRow(
                iconRes = R.drawable.ic_stay_awake,
                title = stringResource(R.string.title_stay_awake),
                subtitle = stringResource(R.string.summary_stay_awake),
                detailText = null,
                checked = uiState.stayAwakeEnabled,
                enabled = true,
                detailDialogTitle = null,
                detailDialogLabel = null,
                detailKeyboardType = KeyboardType.Text,
                onDetailChange = null,
                onCheckedChange = onStayAwakeChange
            )
            DividerSpacer()
            SettingRow(
                iconRes = R.drawable.ic_stay_awake,
                title = stringResource(R.string.title_screen_on),
                subtitle = stringResource(R.string.summary_screen_on),
                detailText = null,
                checked = uiState.screenOnEnabled,
                enabled = true,
                detailDialogTitle = null,
                detailDialogLabel = null,
                detailKeyboardType = KeyboardType.Text,
                onDetailChange = null,
                onCheckedChange = onScreenOnChange
            )
            DividerSpacer()
            SettingRow(
                iconRes = R.drawable.ic_wifi_debug,
                title = stringResource(R.string.title_wifi_access_limit),
                subtitle = stringResource(R.string.summary_wifi_access_limit),
                detailText = stringResource(R.string.label_wifi_access_limit_ip, uiState.wifiAccessLimitIp),
                checked = uiState.wifiAccessLimitEnabled,
                enabled = true,
                detailDialogTitle = stringResource(R.string.dialog_wifi_access_limit_ip_title),
                detailDialogLabel = stringResource(R.string.dialog_wifi_access_limit_ip_label),
                detailKeyboardType = KeyboardType.Uri,
                onDetailChange = onWifiAccessLimitIpChange,
                onCheckedChange = onWifiAccessLimitChange
            )
            DividerSpacer()
            SettingRow(
                iconRes = R.drawable.ic_latency_reduce,
                title = stringResource(R.string.title_disable_animations),
                subtitle = uiState.disableAnimationsSubtitle,
                detailText = null,
                checked = uiState.disableAnimationsEnabled,
                enabled = true,
                detailDialogTitle = null,
                detailDialogLabel = null,
                detailKeyboardType = KeyboardType.Text,
                onDetailChange = null,
                onCheckedChange = onDisableAnimationsChange
            )
        }
    }
}

@Composable
private fun DividerSpacer() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
    )
}

@Composable
private fun SettingRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    detailText: String?,
    checked: Boolean,
    enabled: Boolean,
    detailDialogTitle: String?,
    detailDialogLabel: String?,
    detailKeyboardType: KeyboardType,
    onDetailChange: ((String) -> Unit)?,
    onCheckedChange: (Boolean) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var portDialogVisible by remember { mutableStateOf(false) }
    var detailInput by remember(detailText) { mutableStateOf(detailText.orEmpty()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colorResource(R.color.settingsIconBackground)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            if (detailText != null && onDetailChange != null) {
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    modifier = Modifier
                        .padding(top = if (subtitle.isNotBlank()) 6.dp else 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            detailInput = detailText
                            portDialogVisible = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = primaryColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    if (portDialogVisible && onDetailChange != null) {
        AlertDialog(
            onDismissRequest = { portDialogVisible = false },
            title = { Text(detailDialogTitle.orEmpty()) },
            text = {
                OutlinedTextField(
                    value = detailInput,
                    onValueChange = { value ->
                        detailInput =
                            if (detailKeyboardType == KeyboardType.Number) {
                                value.filter { it.isDigit() }.take(5)
                            } else {
                                value.filter { it.isDigit() || it == '.' }.take(15)
                            }
                    },
                    singleLine = true,
                    label = { Text(detailDialogLabel.orEmpty()) },
                    keyboardOptions = KeyboardOptions(keyboardType = detailKeyboardType)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        portDialogVisible = false
                        onDetailChange(detailInput)
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { portDialogVisible = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
