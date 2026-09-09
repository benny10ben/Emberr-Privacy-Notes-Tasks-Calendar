package com.ben.emberr.domain.util

class DesktopVoiceRecognizer : VoiceRecognizer {

    override fun startListening(
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPermissionNeeded: () -> Unit
    ) {
        // Desktop platforms don't have a native built-in speech recognizer API
        // like Android does. We immediately return an error so the UI handles it gracefully.
        onError("Voice recognition is not natively supported on Desktop.")
    }

    override fun stopListening() {
        // No-op
    }

    override fun destroy() {
        // No-op
    }
}