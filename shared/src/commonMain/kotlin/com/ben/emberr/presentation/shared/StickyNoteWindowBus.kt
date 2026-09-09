package com.ben.emberr.presentation.shared

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object StickyNoteWindowBus {
    val openNoteIds: SnapshotStateList<String> = mutableStateListOf()

    fun open(noteId: String) {
        if (noteId !in openNoteIds) {
            openNoteIds.add(noteId)
        }
    }

    fun close(noteId: String) {
        openNoteIds.remove(noteId)
    }
}
