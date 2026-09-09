package com.emberr.domain.selfhost.media

import com.emberr.domain.model.CellData
import com.emberr.domain.model.ColumnType
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumn
import com.emberr.domain.model.DatabaseRow
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.MediaItem
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.VoiceBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaReferenceScannerTest {

    private fun column(id: String, type: ColumnType) = DatabaseColumn(
        id = id,
        databaseId = "database-1",
        name = id,
        type = type
    )

    @Test
    fun aNoteWithNoMediaReferencesNothing() {
        val blocks = listOf(TextBlock(id = "text-1", text = "just words"))

        assertTrue(MediaReferenceScanner.extractMediaFileNames(blocks).isEmpty())
    }

    @Test
    fun imagesDocumentsAndVoiceNotesAreAllCollected() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = "photo.png"),
            DocumentBlock(id = "document-1", localFilePath = "invoice.pdf"),
            VoiceBlock(id = "voice-1", localFilePath = "memo.m4a")
        )

        assertEquals(
            setOf("photo.png", "invoice.pdf", "memo.m4a"),
            MediaReferenceScanner.extractMediaFileNames(blocks)
        )
    }

    @Test
    fun onlyTheFileNameIsKeptAndNotTheFolderItSatIn() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = "/data/user/0/com.emberr/files/photo.png"),
            DocumentBlock(id = "document-1", localFilePath = "media/nested/invoice.pdf")
        )

        assertEquals(
            setOf("photo.png", "invoice.pdf"),
            MediaReferenceScanner.extractMediaFileNames(blocks)
        )
    }

    @Test
    fun mediaBlocksWithNoFileYetAreIgnored() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = null),
            DocumentBlock(id = "document-1", localFilePath = null),
            VoiceBlock(id = "voice-1", localFilePath = null)
        )

        assertTrue(MediaReferenceScanner.extractMediaFileNames(blocks).isEmpty())
    }

    @Test
    fun deletedBlocksNoLongerCountAsUsingTheirMedia() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = "kept.png"),
            ImageBlock(id = "image-2", localFilePath = "removed.png", isDeleted = true)
        )

        assertEquals(setOf("kept.png"), MediaReferenceScanner.extractMediaFileNames(blocks))
    }

    @Test
    fun theSameFileUsedTwiceIsOnlyListedOnce() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = "shared.png"),
            ImageBlock(id = "image-2", localFilePath = "folder/shared.png")
        )

        assertEquals(setOf("shared.png"), MediaReferenceScanner.extractMediaFileNames(blocks))
    }

    @Test
    fun filesAndAudioColumnsInsideADatabaseAreCollected() {
        val filesColumn = column("column-files", ColumnType.FILES)
        val audioColumn = column("column-audio", ColumnType.AUDIO)
        val database = DatabaseBlock(
            id = "database-1",
            columns = listOf(filesColumn, audioColumn),
            rows = listOf(
                DatabaseRow(
                    id = "row-1",
                    databaseId = "database-1",
                    cells = mapOf(
                        filesColumn.id to CellData.MediaList(
                            listOf(MediaItem("attachment.pdf", "Invoice.pdf"))
                        ),
                        audioColumn.id to CellData.MediaList(
                            listOf(MediaItem("clip.m4a", "Recording.m4a"))
                        )
                    )
                )
            )
        )

        assertEquals(
            setOf("attachment.pdf", "clip.m4a"),
            MediaReferenceScanner.extractMediaFileNames(listOf(database))
        )
    }

    @Test
    fun mediaHeldInANonMediaColumnIsNotCollected() {
        val textColumn = column("column-text", ColumnType.TEXT)
        val database = DatabaseBlock(
            id = "database-1",
            columns = listOf(textColumn),
            rows = listOf(
                DatabaseRow(
                    id = "row-1",
                    databaseId = "database-1",
                    cells = mapOf(
                        textColumn.id to CellData.MediaList(
                            listOf(MediaItem("hidden.pdf", "Hidden.pdf"))
                        )
                    )
                )
            )
        )

        assertTrue(MediaReferenceScanner.extractMediaFileNames(listOf(database)).isEmpty())
    }

    @Test
    fun blankFileNamesInsideADatabaseAreSkipped() {
        val filesColumn = column("column-files", ColumnType.FILES)
        val database = DatabaseBlock(
            id = "database-1",
            columns = listOf(filesColumn),
            rows = listOf(
                DatabaseRow(
                    id = "row-1",
                    databaseId = "database-1",
                    cells = mapOf(
                        filesColumn.id to CellData.MediaList(
                            listOf(MediaItem("", "Empty"), MediaItem("   ", "Spaces"), MediaItem("real.pdf", "Real"))
                        )
                    )
                )
            )
        )

        assertEquals(
            setOf("real.pdf"),
            MediaReferenceScanner.extractMediaFileNames(listOf(database))
        )
    }

    @Test
    fun aDeletedDatabaseNoLongerCountsAsUsingItsAttachments() {
        val filesColumn = column("column-files", ColumnType.FILES)
        val database = DatabaseBlock(
            id = "database-1",
            columns = listOf(filesColumn),
            rows = listOf(
                DatabaseRow(
                    id = "row-1",
                    databaseId = "database-1",
                    cells = mapOf(
                        filesColumn.id to CellData.MediaList(
                            listOf(MediaItem("attachment.pdf", "Invoice.pdf"))
                        )
                    )
                )
            ),
            isDeleted = true
        )

        assertTrue(MediaReferenceScanner.extractMediaFileNames(listOf(database)).isEmpty())
    }
}
