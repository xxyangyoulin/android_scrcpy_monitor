package com.xxyangyoulin.scrcpymonitor

object RootDisconnect {
    fun tryDisableAdb(): Boolean {
        val commands = listOf(
            "setprop persist.adb.tcp.port -1",
            "setprop ctl.stop adbd",
            "sleep 1",
            "setprop ctl.start adbd"
        )
        return RootShell.succeeded(RootShell.run(commands))
    }
}
