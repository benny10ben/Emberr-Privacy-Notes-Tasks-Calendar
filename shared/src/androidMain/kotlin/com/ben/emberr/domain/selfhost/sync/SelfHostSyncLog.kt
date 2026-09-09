package com.ben.emberr.domain.selfhost.sync

import android.util.Log

actual object SelfHostSyncLog {
    private const val TAG = "EmberrSyncEngine"

    actual fun d(message: String) {
        Log.d(TAG, message)
    }

    actual fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}