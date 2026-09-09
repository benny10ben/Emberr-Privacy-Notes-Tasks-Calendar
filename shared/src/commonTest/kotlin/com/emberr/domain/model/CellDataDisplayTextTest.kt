package com.emberr.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CellDataDisplayTextTest {

    @Test
    fun anEmptyCellReadsAsAnEmptyString() {
        val missingCell: CellData? = null

        assertEquals("", missingCell.displayText())
    }

    @Test
    fun aTextCellReadsAsItsOwnValue() {
        assertEquals("Buy milk", CellData.Text("Buy milk").displayText())
        assertEquals("", CellData.Text("").displayText())
    }

    @Test
    fun aWholeNumberLosesItsTrailingDecimalPoint() {
        assertEquals("12", CellData.Number(12.0).displayText())
        assertEquals("0", CellData.Number(0.0).displayText())
        assertEquals("-5", CellData.Number(-5.0).displayText())
    }

    @Test
    fun aFractionalNumberKeepsItsDecimals() {
        assertEquals("12.5", CellData.Number(12.5).displayText())
        assertEquals("-0.25", CellData.Number(-0.25).displayText())
    }

    @Test
    fun anEmptyNumberReadsAsAnEmptyString() {
        assertEquals("", CellData.Number(null).displayText())
    }

    @Test
    fun aCheckboxCellReadsAsTrueOrFalse() {
        assertEquals("true", CellData.Boolean(true).displayText())
        assertEquals("false", CellData.Boolean(false).displayText())
    }

    @Test
    fun aDateCellReadsAsAPlainDateInUniversalTime() {
        assertEquals("2023-11-14", CellData.Date(1_700_000_000_000L).displayText())
        assertEquals("1970-01-01", CellData.Date(0L).displayText())
    }

    @Test
    fun anEmptyDateReadsAsAnEmptyString() {
        assertEquals("", CellData.Date(null).displayText())
    }

    @Test
    fun aTagCellJoinsEveryTagWithCommas() {
        assertEquals("tag-a,tag-b", CellData.TagList(listOf("tag-a", "tag-b")).displayText())
        assertEquals("tag-a", CellData.TagList(listOf("tag-a")).displayText())
        assertEquals("", CellData.TagList(emptyList()).displayText())
    }

    @Test
    fun aMediaCellPairsEachStoredFileWithItsOriginalName() {
        val cell = CellData.MediaList(
            listOf(
                MediaItem("stored-1.png", "holiday.png"),
                MediaItem("stored-2.pdf", "invoice.pdf")
            )
        )

        assertEquals("stored-1.png|holiday.png,stored-2.pdf|invoice.pdf", cell.displayText())
        assertEquals("", CellData.MediaList(emptyList()).displayText())
    }

    @Test
    fun aLinkedNoteCellShowsOnlyTheFirstNote() {
        assertEquals("note-a", CellData.NoteRelation(listOf("note-a", "note-b")).displayText())
        assertEquals("", CellData.NoteRelation(emptyList()).displayText())
    }

    @Test
    fun aFormulaCellReadsAsItsAlreadyCalculatedResult() {
        assertEquals("25.00", CellData.Formula("25.00").displayText())
        assertEquals("Error", CellData.Formula("Error").displayText())
    }
}
