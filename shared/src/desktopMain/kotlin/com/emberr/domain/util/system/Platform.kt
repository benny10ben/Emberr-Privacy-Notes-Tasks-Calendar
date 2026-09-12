package com.emberr.domain.util.system

actual val isDesktopPlatform = true
actual fun showFeedback(message: String) {
    println("Feedback: $message")
}

actual fun triggerHapticFeedback() {}