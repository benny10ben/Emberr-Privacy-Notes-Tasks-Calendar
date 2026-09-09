// The single log tag every widget writes its failures to.
package com.ben.emberr.presentation.widget

import android.util.Log

object WidgetLog {
    private const val TAG = "EmberrWidget"

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
