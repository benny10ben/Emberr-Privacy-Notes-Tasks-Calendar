package com.ben.emberr.domain.selfhost.media

import com.ben.emberr.domain.model.CellData
import com.ben.emberr.domain.model.ColumnType
import com.ben.emberr.domain.model.DatabaseBlock
import com.ben.emberr.domain.model.DocumentBlock
import com.ben.emberr.domain.model.ImageBlock
import com.ben.emberr.domain.model.NoteBlock
import com.ben.emberr.domain.model.VoiceBlock

object MediaReferenceScanner {

    fun extractMediaFileNames(blocks: List<NoteBlock>): Set<String> {
        val fileNames = mutableSetOf<String>()
        blocks.forEach { block ->
            if (block.isDeleted) return@forEach
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
                else -> Unit
            }
        }
        return fileNames
    }
}