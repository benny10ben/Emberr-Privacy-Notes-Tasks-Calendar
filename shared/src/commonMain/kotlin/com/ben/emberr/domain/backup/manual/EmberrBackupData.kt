package com.ben.emberr.domain.backup.manual

import com.ben.emberr.data.local.room.BookmarkBlockEntity
import com.ben.emberr.data.local.room.CalendarTaskEntity
import com.ben.emberr.data.local.room.DocumentBlockEntity
import com.ben.emberr.data.local.room.FolderEntity
import com.ben.emberr.data.local.room.ImageBlockEntity
import com.ben.emberr.data.local.room.NoteBlockEntity
import com.ben.emberr.data.local.room.NoteMetadataEntity
import com.ben.emberr.data.local.room.TagEntity
import kotlinx.serialization.Serializable

@Serializable
data class EmberrBackupData(
    val version: Int = 1,
    val exportTimestamp: Long,
    val notes: List<NoteMetadataEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val blocks: List<NoteBlockEntity> = emptyList(),
    val calendarTasks: List<CalendarTaskEntity> = emptyList(),
    val imageBlocks: List<ImageBlockEntity> = emptyList(),
    val documentBlocks: List<DocumentBlockEntity> = emptyList(),
    val bookmarkBlocks: List<BookmarkBlockEntity> = emptyList()
)
