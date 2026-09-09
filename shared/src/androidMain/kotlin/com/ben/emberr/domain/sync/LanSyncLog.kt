package com.ben.emberr.domain.sync

import android.util.Log

actual object LanSyncLog {
    private const val TAG = "EmberrLanSync"

    actual fun d(message: String) {
        Log.d(TAG, message)
    }

    actual fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}
