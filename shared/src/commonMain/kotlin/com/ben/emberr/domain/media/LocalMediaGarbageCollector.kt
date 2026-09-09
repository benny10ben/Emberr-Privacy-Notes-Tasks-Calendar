package com.ben.emberr.domain.media

import com.ben.emberr.domain.model.CellData
import com.ben.emberr.domain.model.ColumnType
import com.ben.emberr.domain.model.DatabaseBlock
import com.ben.emberr.domain.model.DocumentBlock
import com.ben.emberr.domain.model.ImageBlock
import com.ben.emberr.domain.model.NoteBlock
import com.ben.emberr.domain.model.VoiceBlock
import com.ben.emberr.domain.repository.NoteRepository
import com.ben.emberr.domain.util.MediaStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalMediaGarbageCollector(
    private val noteRepository: NoteRepository,
    private val mediaStorageHelper: MediaStorageHelper
) {
    suspend fun collectAndDeleteOrphanedMedia() = withContext(Dispatchers.IO) {
        try {
            val referencedFileNames = collectReferencedMediaFileNames()
            val nowMs = System.currentTimeMillis()
            var deletedCount = 0

            mediaStorageHelper.listAllMediaFileNames()
                .filterNot { it in referencedFileNames }
                .forEach { fileName ->
                    try {
                        val file = File(mediaStorageHelper.getAbsoluteMediaPath(fileName))
                        val isStaleEnoughIfTempFile = !fileName.endsWith(".tmp") ||
                                (nowMs - file.lastModified()) > STALE_TEMP_FILE_THRESHOLD_MS
                        if (file.exists() && isStaleEnoughIfTempFile && file.delete()) {
                            deletedCount++
                        }
                    } catch (e: Exception) {
                        LocalMediaGcLog.e("collectAndDeleteOrphanedMedia: failed to delete $fileName: ${e.message}", e)
                    }
                }

            LocalMediaGcLog.d("collectAndDeleteOrphanedMedia: deleted $deletedCount orphaned file(s)")
        } catch (e: Exception) {
            LocalMediaGcLog.e("collectAndDeleteOrphanedMedia: failed with ${e::class.simpleName}: ${e.message}", e)
        }
    }

    suspend fun deleteExpiredTempFiles() = withContext(Dispatchers.IO) {
        try {
            val nowMs = System.currentTimeMillis()
            var deletedCount = 0

            mediaStorageHelper.listAllMediaFileNames()
                .filter { it.endsWith(".tmp") }
                .forEach { fileName ->
                    try {
                        val file = File(mediaStorageHelper.getAbsoluteMediaPath(fileName))
                        if (file.exists() && (nowMs - file.lastModified()) > STALE_TEMP_FILE_THRESHOLD_MS && file.delete()) {
                            deletedCount++
                        }
                    } catch (e: Exception) {
                        LocalMediaGcLog.e("deleteExpiredTempFiles: failed to delete $fileName: ${e.message}", e)
                    }
                }

            LocalMediaGcLog.d("deleteExpiredTempFiles: deleted $deletedCount expired temp file(s)")
        } catch (e: Exception) {
            LocalMediaGcLog.e("deleteExpiredTempFiles: failed with ${e::class.simpleName}: ${e.message}", e)
        }
    }

    private companion object {
        const val STALE_TEMP_FILE_THRESHOLD_MS = 48L * 60 * 60 * 1000
    }

    private suspend fun collectReferencedMediaFileNames(): Set<String> {
        val fileNames = mutableSetOf<String>()
        noteRepository.getNotesModifiedSince(0L).forEach { meta ->
            meta.coverImagePath?.substringAfterLast("/")?.let { fileNames.add(it) }
            val content = if (meta.isDaily && meta.dateString != null) {
                noteRepository.getDailyNote(meta.dateString)
            } else {
                noteRepository.getNoteContent(meta.noteId)
            }
            content?.blocks?.forEach { block -> fileNames += extractMediaFileNames(block) }
        }
        return fileNames
    }

    private fun extractMediaFileNames(block: NoteBlock): List<String> {
        if (block.isDeleted) return emptyList()
        val fileNames = mutableListOf<String>()
        when (block) {
            is ImageBlock -> block.localFilePath?.substringAfterLast("/")?.let { fileNames.add(it) }
            is DocumentBlock -> block.localFilePath?.substringAfterLast("/")?.let { fileNames.add(it) }
            is VoiceBlock -> block.localFilePath?.substringAfterLast("/")?.let { fileNames.add(it) }
            is DatabaseBlock -> {
                val mediaColIds = block.columns
                    .filter { it.type == ColumnType.FILES || it.type == ColumnType.AUDIO }
                    .map { it.id }.toSet()
                block.rows.forEach { row ->
                    mediaColIds.forEach { colId ->
                        val files = (row.cells[colId] as? CellData.MediaList)?.files ?: emptyList()
                        files.forEach { media ->
                            val cleanLocalPath = media.fileName.substringAfterLast("/")
                            if (cleanLocalPath.isNotBlank()) fileNames.add(cleanLocalPath)
                        }
                    }
                }
            }
            else -> {}
        }
        return fileNames
    }
}
