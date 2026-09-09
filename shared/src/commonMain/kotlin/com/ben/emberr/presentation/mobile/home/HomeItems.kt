package com.ben.emberr.presentation.mobile.home

import com.ben.emberr.data.local.room.FolderEntity
import com.ben.emberr.data.local.room.NoteMetadataEntity

/*
 * Shared between the mobile home grid and the desktop sidebar tree.
 *
 * A HomeItem is one row/card - either a folder or a note. Both screens build their list from
 * these, so sorting lives here and only here: change homeItemComparator and both platforms
 * follow. Item keys (HomeItemKey) are the ids used for lazy list keys, drag payloads and the
 * reorder calls into HomeViewModel.
 *
 * Nothing here is Compose or platform specific - keep it that way.
 */

enum class DropInsertPosition { BEFORE, INTO, AFTER }

object HomeItemKey {
    const val FOLDER_PREFIX = "home_folder_"
    const val NOTE_PREFIX = "home_note_"

    fun forFolder(folderId: String) = "$FOLDER_PREFIX$folderId"
    fun forNote(noteId: String) = "$NOTE_PREFIX$noteId"

    fun isFolder(key: String) = key.startsWith(FOLDER_PREFIX)
    fun isNote(key: String) = key.startsWith(NOTE_PREFIX)

    fun folderIdOf(key: String) = key.removePrefix(FOLDER_PREFIX)
    fun noteIdOf(key: String) = key.removePrefix(NOTE_PREFIX)
}

sealed interface HomeItem {
    val key: String
    val level: Int

    data class Folder(val folder: FolderEntity, override val level: Int = 0) : HomeItem {
        override val key: String get() = HomeItemKey.forFolder(folder.folderId)
    }

    data class Note(val note: NoteMetadataEntity, override val level: Int = 0) : HomeItem {
        override val key: String get() = HomeItemKey.forNote(note.noteId)
    }
}

private val HomeItem.typeRank: Int
    get() = when (this) {
        is HomeItem.Folder -> 0
        is HomeItem.Note   -> 1
    }

private val HomeItem.sortName: String
    get() = when (this) {
        is HomeItem.Folder -> folder.name.lowercase()
        is HomeItem.Note   -> note.title.ifEmpty { "Untitled" }.lowercase()
    }

private val HomeItem.sortCreatedAt: Long
    get() = when (this) {
        is HomeItem.Folder -> folder.createdAt
        is HomeItem.Note   -> note.createdAt
    }

private val HomeItem.sortEditedAt: Long
    get() = when (this) {
        is HomeItem.Folder -> folder.lastEditedAt
        is HomeItem.Note   -> note.updatedAt
    }

private val HomeItem.manualOrder: Int
    get() = when (this) {
        is HomeItem.Folder -> if (folder.sortOrder == 0) Int.MAX_VALUE else folder.sortOrder
        is HomeItem.Note   -> if (note.sortOrder == 0)   Int.MAX_VALUE else note.sortOrder
    }

private fun homeItemNameComparator(descending: Boolean): Comparator<HomeItem> =
    if (descending) compareByDescending<HomeItem> { it.sortName }
    else compareBy<HomeItem> { it.sortName }

private fun homeItemTimestampComparator(
    descending: Boolean,
    selector: (HomeItem) -> Long
): Comparator<HomeItem> =
    if (descending) compareByDescending<HomeItem> { selector(it) }
    else compareBy<HomeItem> { selector(it) }

fun homeItemComparator(sortType: SortType, sortOrder: SortOrder): Comparator<HomeItem> {
    val descending = sortOrder == SortOrder.DESCENDING
    return when (sortType) {
        SortType.MANUAL       -> compareBy<HomeItem> { it.manualOrder }
            .then(homeItemTimestampComparator(true) { it.sortCreatedAt })

        SortType.TYPE         -> compareBy<HomeItem> { it.typeRank }.then(homeItemNameComparator(descending))
        SortType.NAME         -> homeItemNameComparator(descending)
        SortType.DATE_CREATED -> homeItemTimestampComparator(descending) { it.sortCreatedAt }.then(homeItemNameComparator(false))
        SortType.LAST_EDITED  -> homeItemTimestampComparator(descending) { it.sortEditedAt }.then(homeItemNameComparator(false))
    }
}

fun List<HomeItem>.movedTo(draggedKey: String, targetKey: String): List<HomeItem> {
    val from = indexOfFirst { it.key == draggedKey }
    val to = indexOfFirst { it.key == targetKey }
    if (from == -1 || to == -1 || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

fun sortedHomeItems(
    folders: List<FolderEntity>,
    notes: List<NoteMetadataEntity>,
    sortType: SortType,
    sortOrder: SortOrder,
    level: Int = 0
): List<HomeItem> =
    (folders.map { HomeItem.Folder(it, level) } + notes.map { HomeItem.Note(it, level) })
        .sortedWith(homeItemComparator(sortType, sortOrder))
