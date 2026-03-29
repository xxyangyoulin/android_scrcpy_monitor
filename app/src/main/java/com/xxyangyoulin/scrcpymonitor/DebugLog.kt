package com.xxyangyoulin.scrcpymonitor

import android.util.Log

object DebugLog {
    private const val TAG = "ScrcpyMonitor"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }
}
