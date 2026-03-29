package com.xxyangyoulin.scrcpymonitor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource

@Composable
fun ScrcpyMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scrcpyMonitorColorScheme(),
        content = content
    )
}

@Composable
private fun scrcpyMonitorColorScheme(): ColorScheme {
    return if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = colorResource(R.color.colorPrimary),
            onPrimary = colorResource(R.color.colorOnPrimary),
            surface = colorResource(R.color.colorSurface),
            surfaceVariant = colorResource(R.color.colorSurfaceVariant),
            onSurface = colorResource(R.color.colorOnSurface),
            onSurfaceVariant = colorResource(R.color.colorOnSurfaceVariant)
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.colorPrimary),
            onPrimary = colorResource(R.color.colorOnPrimary),
            surface = colorResource(R.color.colorSurface),
            surfaceVariant = colorResource(R.color.colorSurfaceVariant),
            onSurface = colorResource(R.color.colorOnSurface),
            onSurfaceVariant = colorResource(R.color.colorOnSurfaceVariant)
        )
    }
}
