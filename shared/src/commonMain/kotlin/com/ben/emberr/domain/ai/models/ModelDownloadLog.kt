package com.ben.emberr.domain.ai.models

expect object ModelDownloadLog {
    fun e(message: String, throwable: Throwable? = null)
}
