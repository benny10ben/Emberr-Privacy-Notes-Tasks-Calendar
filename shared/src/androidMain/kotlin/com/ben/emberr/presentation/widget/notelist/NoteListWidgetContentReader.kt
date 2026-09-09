// Loads the note list from storage and turns it into rows the widget can draw.
package com.ben.emberr.presentation.widget.notelist

import com.ben.emberr.data.local.room.NoteDao
import com.ben.emberr.data.local.room.NoteMetadataEntity
import com.ben.emberr.presentation.widget.WidgetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private const val maximumNotesShown = 30
private const val maximumCharactersPerTitle = 120
private const val maximumCharactersPerSnippet = 120

class NoteListWidgetContentReader(private val noteDao: NoteDao) {

    fun observeNotes(): Flow<List<NoteMetadataEntity>> =
        noteDao.getAllNotes()
            .flowOn(Dispatchers.IO)
            .catch { cause -> WidgetLog.e("Stopped observing the note list", cause) }

    suspend fun readContentOnce(): NoteListWidgetContent? =
        withContext(Dispatchers.IO) {
            val notes = try {
                noteDao.getRecentNotes(maximumNotesShown)
            } catch (cause: Exception) {
                WidgetLog.e("Could not read the note list", cause)
                return@withContext null
            }
            buildContent(notes)
        }

    fun buildContent(notes: List<NoteMetadataEntity>): NoteListWidgetContent =
        NoteListWidgetContent(
            notes = notes.take(maximumNotesShown).map { note ->
                NoteListWidgetRow(
                    noteId = note.noteId,
                    title = note.title.trim().take(maximumCharactersPerTitle).ifBlank { "Untitled" },
                    snippet = note.snippet.trim().take(maximumCharactersPerSnippet)
                )
            }
        )
}
