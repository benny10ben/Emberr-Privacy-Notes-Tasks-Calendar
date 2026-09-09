package com.ben.emberr.domain.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

enum class MediaTransferPhase { DOWNLOADING, UPLOADING }

// Tracks real-time media transfer states (downloading/uploading/failed) keyed by file name
// so UI components can reflect in-flight status directly.
object MediaTransferStatusBus {
    // Requires multiple consecutive failures before flagging a file as failed in the UI
    // to prevent transient transfer delays or race conditions from flashing false errors.
    private const val CONSECUTIVE_FAILURES_BEFORE_SHOWING_FAILED = 2
    private val consecutiveFailureCounts = ConcurrentHashMap<String, Int>()

    private val _inProgress = MutableStateFlow<Map<String, MediaTransferPhase>>(emptyMap())
    val inProgress: StateFlow<Map<String, MediaTransferPhase>> = _inProgress.asStateFlow()

    private val _failed = MutableStateFlow<Set<String>>(emptySet())
    val failed: StateFlow<Set<String>> = _failed.asStateFlow()

    fun markStarted(fileName: String, phase: MediaTransferPhase) {
        _inProgress.update { it + (fileName to phase) }
    }

    fun markFinished(fileName: String, succeeded: Boolean) {
        _inProgress.update { it - fileName }
        if (succeeded) {
            consecutiveFailureCounts.remove(fileName)
            _failed.update { it - fileName }
            return
        }
        val failureCount = consecutiveFailureCounts.merge(fileName, 1) { previous, one -> previous + one } ?: 1
        if (failureCount >= CONSECUTIVE_FAILURES_BEFORE_SHOWING_FAILED) {
            _failed.update { it + fileName }
        }
    }

    // Handles deferred transfers (e.g. peer still uploading via HTTP 425),
    // resetting the failure streak so the UI maintains active transfer status.
    fun markDeferred(fileName: String) {
        _inProgress.update { it - fileName }
        consecutiveFailureCounts.remove(fileName)
    }
}