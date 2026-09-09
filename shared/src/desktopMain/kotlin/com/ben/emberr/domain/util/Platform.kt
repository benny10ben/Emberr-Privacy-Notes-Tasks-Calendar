package com.ben.emberr.domain.util

actual val isDesktopPlatform = true
actual fun showFeedback(message: String) {
    println("Feedback: $message")
}

actual fun triggerHapticFeedback() {}