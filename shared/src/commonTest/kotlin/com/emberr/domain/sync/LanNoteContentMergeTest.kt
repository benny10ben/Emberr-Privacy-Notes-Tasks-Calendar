package com.emberr.domain.sync

import com.emberr.domain.model.CellData
import com.emberr.domain.model.ColumnType
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumn
import com.emberr.domain.model.DatabaseRow
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.TextBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LanNoteContentMergeTest {

    private fun text(id: String, body: String, updatedAt: Long) =
        TextBlock(id = id, text = body, updatedAt = updatedAt)

    private fun contentOf(vararg blocks: TextBlock) = NoteContent(blocks = blocks.toList())

    private fun textAt(content: NoteContent, index: Int) = (content.blocks[index] as TextBlock).text

    @Test
    fun aDeviceWithNoLocalCopyJustTakesTheIncomingOne() {
        val remote = contentOf(text("block-1", "theirs", 100L))

        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = null,
            localUpdatedAt = 0L,
            remoteContent = remote,
            remoteUpdatedAt = 100L
        )

        assertEquals(remote, merged)
    }

    @Test
    fun theNewerSideDecidesTheBlockOrder() {
        val local = contentOf(text("block-1", "one", 100L), text("block-2", "two", 100L))
        val remote = contentOf(text("block-2", "two", 100L), text("block-1", "one", 100L))

        val merged = NoteMergeHelper.mergeNoteContent(local, 100L, remote, 200L)

        assertEquals(listOf("block-2", "block-1"), merged.blocks.map { it.id })
    }

    @Test
    fun onAnExactTieTheLocalOrderIsKept() {
        val local = contentOf(text("block-1", "one", 100L), text("block-2", "two", 100L))
        val remote = contentOf(text("block-2", "two", 100L), text("block-1", "one", 100L))

        val merged = NoteMergeHelper.mergeNoteContent(local, 100L, remote, 100L)

        assertEquals(listOf("block-1", "block-2"), merged.blocks.map { it.id })
    }

    @Test
    fun theNewerEditOfEachIndividualBlockWinsRegardlessOfWhichSideWonOverall() {
        val local = contentOf(text("block-1", "edited here last", 500L))
        val remote = contentOf(text("block-1", "edited there first", 100L))

        val merged = NoteMergeHelper.mergeNoteContent(local, 100L, remote, 200L)

        assertEquals("edited here last", textAt(merged, 0))
    }

    @Test
    fun aBlockEditedOnlyOnTheOtherSideIsPickedUp() {
        val local = contentOf(text("block-1", "stale", 100L))
        val remote = contentOf(text("block-1", "fresh", 500L))

        val merged = NoteMergeHelper.mergeNoteContent(local, 500L, remote, 100L)

        assertEquals("fresh", textAt(merged, 0))
    }

    @Test
    fun aBlockOnlyTheOtherSideHasIsSlottedInAfterTheBlockItFollowedThere() {
        val local = contentOf(
            text("block-a", "a", 100L),
            text("block-b", "b", 100L),
            text("block-c", "c", 100L),
            text("block-d", "d", 100L)
        )
        val remote = contentOf(text("block-a", "a", 100L), text("block-c", "c", 100L))

        val merged = NoteMergeHelper.mergeNoteContent(local, 100L, remote, 200L)

        assertEquals(
            listOf("block-a", "block-b", "block-c", "block-d"),
            merged.blocks.map { it.id }
        )
    }

    @Test
    fun aBlockOnlyTheOtherSideHasAtTheVeryTopLandsAtTheTop() {
        val local = contentOf(text("block-a", "a", 100L), text("block-b", "b", 100L))
        val remote = contentOf(text("block-b", "b", 100L))

        val merged = NoteMergeHelper.mergeNoteContent(local, 100L, remote, 200L)

        assertEquals(listOf("block-a", "block-b"), merged.blocks.map { it.id })
    }

    @Test
    fun nothingIsEverListedTwiceAfterAMerge() {
        val local = contentOf(text("block-1", "one", 100L), text("block-2", "two", 100L))
        val remote = contentOf(text("block-2", "two", 200L), text("block-3", "three", 200L))

        val merged = NoteMergeHelper.mergeNoteContent(local, 100L, remote, 200L)

        assertEquals(merged.blocks.size, merged.blocks.map { it.id }.distinct().size)
        assertEquals(setOf("block-1", "block-2", "block-3"), merged.blocks.map { it.id }.toSet())
    }

    @Test
    fun aTableAddedOnEachSideKeepsBothSetsOfColumnsAndRows() {
        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = NoteContent(blocks = listOf(olderLocalDatabase())),
            localUpdatedAt = 100L,
            remoteContent = NoteContent(blocks = listOf(newerRemoteDatabase())),
            remoteUpdatedAt = 200L
        )

        val database = merged.blocks.single() as DatabaseBlock

        assertEquals(listOf("column-a", "column-b", "column-c"), database.columns.map { it.id })
        assertEquals(listOf("row-1", "row-2"), database.rows.map { it.id })
    }

    @Test
    fun theNewerSideNamesAColumnButSettingsOnlyTheOlderSideHadAreNotLost() {
        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = NoteContent(blocks = listOf(olderLocalDatabase())),
            localUpdatedAt = 100L,
            remoteContent = NoteContent(blocks = listOf(newerRemoteDatabase())),
            remoteUpdatedAt = 200L
        )

        val columnA = (merged.blocks.single() as DatabaseBlock).columns.first { it.id == "column-a" }

        assertEquals("Renamed on the other device", columnA.name)
        assertEquals("sum", columnA.aggregationType)
        assertEquals("$", columnA.currencySymbol)
        assertTrue(columnA.isFormulaCurrency)
    }

    @Test
    fun cellsFilledInOnEitherSideAllSurviveWithTheNewerSideWinningClashes() {
        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = NoteContent(blocks = listOf(olderLocalDatabase())),
            localUpdatedAt = 100L,
            remoteContent = NoteContent(blocks = listOf(newerRemoteDatabase())),
            remoteUpdatedAt = 200L
        )

        val firstRow = (merged.blocks.single() as DatabaseBlock).rows.first { it.id == "row-1" }

        assertEquals(CellData.Text("filled in on the other device"), firstRow.cells["column-a"])
        assertEquals(CellData.Text("only filled in here"), firstRow.cells["column-b"])
    }

    @Test
    fun aColumnDeletedOnEitherSideStaysDeleted() {
        val localDatabase = olderLocalDatabase().let { database ->
            database.copy(
                columns = database.columns.map { column ->
                    if (column.id == "column-a") column.copy(isDeleted = true) else column
                }
            )
        }

        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = NoteContent(blocks = listOf(localDatabase)),
            localUpdatedAt = 100L,
            remoteContent = NoteContent(blocks = listOf(newerRemoteDatabase())),
            remoteUpdatedAt = 200L
        )

        val columnA = (merged.blocks.single() as DatabaseBlock).columns.first { it.id == "column-a" }

        assertTrue(columnA.isDeleted)
    }

    @Test
    fun aRowDeletedOnEitherSideStaysDeleted() {
        val localDatabase = olderLocalDatabase().let { database ->
            database.copy(rows = database.rows.map { it.copy(isDeleted = true) })
        }

        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = NoteContent(blocks = listOf(localDatabase)),
            localUpdatedAt = 100L,
            remoteContent = NoteContent(blocks = listOf(newerRemoteDatabase())),
            remoteUpdatedAt = 200L
        )

        val firstRow = (merged.blocks.single() as DatabaseBlock).rows.first { it.id == "row-1" }

        assertTrue(firstRow.isDeleted)
    }

    @Test
    fun aMergedTableCarriesTheMostRecentTimestampOfTheTwo() {
        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = NoteContent(blocks = listOf(olderLocalDatabase())),
            localUpdatedAt = 100L,
            remoteContent = NoteContent(blocks = listOf(newerRemoteDatabase())),
            remoteUpdatedAt = 200L
        )

        assertEquals(200L, (merged.blocks.single() as DatabaseBlock).updatedAt)
    }

    @Test
    fun aTableTheOtherSideDoesNotHaveAtAllIsLeftExactlyAsItIs() {
        val remoteDatabase = newerRemoteDatabase()

        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = NoteContent(blocks = listOf(text("block-1", "unrelated", 100L))),
            localUpdatedAt = 100L,
            remoteContent = NoteContent(blocks = listOf(remoteDatabase)),
            remoteUpdatedAt = 200L
        )

        assertEquals(remoteDatabase, merged.blocks.first { it is DatabaseBlock })
    }

    @Test
    fun aMergedNoteIsAlwaysStampedWithTheCurrentContentVersion() {
        val merged = NoteMergeHelper.mergeNoteContent(
            localContent = contentOf(text("block-1", "one", 100L)),
            localUpdatedAt = 100L,
            remoteContent = contentOf(text("block-1", "one", 100L)),
            remoteUpdatedAt = 200L
        )

        assertEquals(1, merged.version)
        assertNull(merged.blocks.firstOrNull { it.id != "block-1" })
    }

    private fun column(
        id: String,
        name: String,
        updatedAt: Long,
        aggregationType: String? = null,
        currencySymbol: String? = null,
        isFormulaCurrency: Boolean = false
    ) = DatabaseColumn(
        id = id,
        databaseId = "database-1",
        name = name,
        type = ColumnType.TEXT,
        aggregationType = aggregationType,
        currencySymbol = currencySymbol,
        isFormulaCurrency = isFormulaCurrency,
        updatedAt = updatedAt
    )

    private fun olderLocalDatabase() = DatabaseBlock(
        id = "database-1",
        title = "Older title",
        columns = listOf(
            column(
                id = "column-a",
                name = "Named here first",
                updatedAt = 100L,
                aggregationType = "sum",
                currencySymbol = "$",
                isFormulaCurrency = true
            ),
            column(id = "column-c", name = "Only here", updatedAt = 100L)
        ),
        rows = listOf(
            DatabaseRow(
                id = "row-1",
                databaseId = "database-1",
                cells = mapOf("column-b" to CellData.Text("only filled in here")),
                updatedAt = 100L
            ),
            DatabaseRow(
                id = "row-2",
                databaseId = "database-1",
                cells = emptyMap(),
                updatedAt = 100L
            )
        ),
        updatedAt = 100L
    )

    private fun newerRemoteDatabase() = DatabaseBlock(
        id = "database-1",
        title = "Newer title",
        columns = listOf(
            column(id = "column-a", name = "Renamed on the other device", updatedAt = 200L),
            column(id = "column-b", name = "Only there", updatedAt = 200L)
        ),
        rows = listOf(
            DatabaseRow(
                id = "row-1",
                databaseId = "database-1",
                cells = mapOf("column-a" to CellData.Text("filled in on the other device")),
                updatedAt = 200L
            )
        ),
        updatedAt = 200L
    )
}
