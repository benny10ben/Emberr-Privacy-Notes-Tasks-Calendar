package com.ben.emberr.domain.ai

sealed interface LocalAiSupport {
    data object Supported : LocalAiSupport
    data class Unsupported(val reason: String) : LocalAiSupport
}

class LocalAiUnsupportedException(reason: String) : IllegalStateException(reason)

expect fun detectLocalAiSupport(): LocalAiSupport
