package com.emberr.domain.backup.manual

import com.emberr.data.local.room.BookmarkBlockEntity
import com.emberr.data.local.room.CalendarTaskEntity
import com.emberr.data.local.room.DocumentBlockEntity
import com.emberr.data.local.room.FolderEntity
import com.emberr.data.local.room.ImageBlockEntity
import com.emberr.data.local.room.NoteBlockEntity
import com.emberr.data.local.room.NoteMetadataEntity
import com.emberr.data.local.room.TagEntity
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
