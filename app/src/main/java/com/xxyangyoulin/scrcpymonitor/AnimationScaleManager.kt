package com.xxyangyoulin.scrcpymonitor

object AnimationScaleManager {
    fun setEnabled(enabled: Boolean): Boolean {
        val scale = if (enabled) "1" else "0"
        val commands = listOf(
            "settings put global window_animation_scale $scale",
            "settings put global transition_animation_scale $scale",
            "settings put global animator_duration_scale $scale"
        )
        return RootShell.succeeded(RootShell.run(commands))
    }
}
