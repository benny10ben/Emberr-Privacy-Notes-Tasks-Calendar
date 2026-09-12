package com.emberr.domain.util.eventbus

import com.emberr.domain.model.CheckboxBlock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class VoiceTaskEvent(
    val dateString: String,
    val block: CheckboxBlock
)

object VoiceTaskEventBus {
    private val _taskAddedEvent = MutableSharedFlow<VoiceTaskEvent>()
    val taskAddedEvent: SharedFlow<VoiceTaskEvent> = _taskAddedEvent.asSharedFlow()

    suspend fun emitTaskAdded(dateString: String, block: CheckboxBlock) {
        _taskAddedEvent.emit(VoiceTaskEvent(dateString, block))
    }
}