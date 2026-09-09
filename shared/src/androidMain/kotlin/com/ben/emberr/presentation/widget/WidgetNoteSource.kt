// Supplies the list of notes a widget's setup screen offers for selection.
package com.ben.emberr.presentation.widget

import com.ben.emberr.data.local.room.NoteDao
import com.ben.emberr.data.local.room.NoteMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

class WidgetNoteSource(private val noteDao: NoteDao) {

    fun observeSelectableNotes(): Flow<List<NoteMetadataEntity>> =
        noteDao.getAllNotes()
            .flowOn(Dispatchers.IO)
            .catch { cause ->
                WidgetLog.e("Could not list notes for a widget setup screen", cause)
                emit(emptyList())
            }
}
