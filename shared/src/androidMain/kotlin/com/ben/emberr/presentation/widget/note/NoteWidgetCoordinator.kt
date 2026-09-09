// Watches the notes currently on the home screen and pushes their edits out while the app runs.
package com.ben.emberr.presentation.widget.note

import android.content.Context
import com.ben.emberr.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.ben.emberr.presentation.widget.WidgetLog
import kotlin.time.Duration.Companion.milliseconds

private const val contentSettleDelayMillis = 120L

class NoteWidgetCoordinator(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val contentReader: WidgetContentReader
) {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val watchJobsByNoteId = mutableMapOf<String, Job>()
    private val watchJobsLock = Mutex()

    fun start() {
        requestResync()
    }

    fun requestResync() {
        coordinatorScope.launch { resyncWatchedNotes() }
    }

    private suspend fun resyncWatchedNotes() = watchJobsLock.withLock {
        val selectedNoteIds = readAllSelectedNoteIds(context)

        watchJobsByNoteId.keys.toList()
            .filterNot { noteId -> noteId in selectedNoteIds }
            .forEach { noteId -> watchJobsByNoteId.remove(noteId)?.cancel() }

        selectedNoteIds
            .filterNot { noteId -> watchJobsByNoteId.containsKey(noteId) }
            .forEach { noteId ->
                watchJobsByNoteId[noteId] = coordinatorScope.launch { watchNote(noteId) }
            }
    }

    @OptIn(FlowPreview::class)
    private suspend fun watchNote(noteId: String) {
        try {
            noteRepository.observeNoteContent(noteId)
                .filterNotNull()
                .distinctUntilChanged()
                .debounce(contentSettleDelayMillis.milliseconds)
                .collect { noteContent ->
                    val content = contentReader.buildContentFromBlocks(noteId, noteContent.blocks)
                    if (content != null) {
                        pushContentToWidgets(context, noteId, content)
                    }
                }
        } catch (cause: Exception) {
            WidgetLog.e("Stopped watching note $noteId", cause)
        }
    }
}
