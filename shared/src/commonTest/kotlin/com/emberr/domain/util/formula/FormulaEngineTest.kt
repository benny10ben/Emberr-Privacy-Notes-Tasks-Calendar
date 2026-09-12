package com.emberr.domain.util.formula

import com.emberr.domain.model.CellData
import com.emberr.domain.model.ColumnType
import com.emberr.domain.model.DatabaseColumn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormulaEngineTest {

    private val priceColumn = DatabaseColumn(
        id = "column-price",
        databaseId = "database-1",
        name = "Price",
        type = ColumnType.NUMBER
    )

    private val quantityColumn = DatabaseColumn(
        id = "column-quantity",
        databaseId = "database-1",
        name = "Quantity",
        type = ColumnType.NUMBER
    )

    private val columns = listOf(priceColumn, quantityColumn)

    private fun evaluate(expression: String, cells: Map<String, CellData> = emptyMap()): String =
        FormulaEngine.evaluate(expression, cells, columns).result

    @Test
    fun anEmptyFormulaProducesNothingRatherThanZero() {
        assertEquals("", evaluate(""))
        assertEquals("", evaluate("   "))
    }

    @Test
    fun plainArithmeticIsCalculated() {
        assertEquals("5", evaluate("2 + 3"))
        assertEquals("6", evaluate("10 - 4"))
        assertEquals("12", evaluate("3 * 4"))
        assertEquals("4", evaluate("12 / 3"))
    }

    @Test
    fun multiplicationHappensBeforeAddition() {
        assertEquals("14", evaluate("2 + 3 * 4"))
        assertEquals("20", evaluate("(2 + 3) * 4"))
        assertEquals("7", evaluate("1 + 2 * (4 - 1)"))
    }

    @Test
    fun leadingSignsAreUnderstood() {
        assertEquals("-3", evaluate("-5 + 2"))
        assertEquals("5", evaluate("+5"))
        assertEquals("7", evaluate("2 - -5"))
    }

    @Test
    fun numbersMayStartWithADecimalPoint() {
        assertEquals("1", evaluate(".5 + .5"))
        assertEquals("1.25", evaluate(".25 + 1"))
    }

    @Test
    fun aColumnReferenceIsReplacedByThatRowsNumber() {
        val cells = mapOf(priceColumn.id to CellData.Number(12.5))

        assertEquals("25", evaluate("prop(\"Price\") * 2", cells))
    }

    @Test
    fun aColumnReferenceWorksWithSingleQuotesToo() {
        val cells = mapOf(priceColumn.id to CellData.Number(12.5))

        assertEquals("25", evaluate("prop('Price') * 2", cells))
    }

    @Test
    fun aColumnNameIsMatchedRegardlessOfCapitalisation() {
        val cells = mapOf(priceColumn.id to CellData.Number(4.0))

        assertEquals("8", evaluate("prop(\"price\") * 2", cells))
        assertEquals("8", evaluate("prop(\"PRICE\") * 2", cells))
    }

    @Test
    fun severalColumnsCanBeCombinedInOneFormula() {
        val cells = mapOf(
            priceColumn.id to CellData.Number(3.0),
            quantityColumn.id to CellData.Number(4.0)
        )

        assertEquals("12", evaluate("prop(\"Price\") * prop(\"Quantity\")", cells))
    }

    @Test
    fun aColumnThatDoesNotExistCountsAsZeroInsteadOfFailing() {
        assertEquals("5", evaluate("prop(\"Nonexistent\") + 5"))
    }

    @Test
    fun anEmptyCellCountsAsZero() {
        val cells = mapOf(priceColumn.id to CellData.Number(null))

        assertEquals("5", evaluate("prop(\"Price\") + 5", cells))
        assertEquals("5", evaluate("prop(\"Price\") + 5", emptyMap()))
    }

    @Test
    fun aCellHoldingWordsCountsAsZero() {
        val cells = mapOf(priceColumn.id to CellData.Text("not a number"))

        assertEquals("5", evaluate("prop(\"Price\") + 5", cells))
    }

    @Test
    fun aFormulaCellCanFeedAnotherFormula() {
        val cells = mapOf(priceColumn.id to CellData.Formula("7"))

        assertEquals("14", evaluate("prop(\"Price\") * 2", cells))
    }

    @Test
    fun aFormulaCellThatFailedEarlierCountsAsZero() {
        val cells = mapOf(priceColumn.id to CellData.Formula("Error"))

        assertEquals("5", evaluate("prop(\"Price\") + 5", cells))
    }

    @Test
    fun wholeResultsAreShownWithoutADecimalPoint() {
        assertEquals("25", evaluate("12.5 * 2"))
        assertEquals("0", evaluate("5 - 5"))
    }

    @Test
    fun fractionalResultsAreShownWithExactlyTwoDecimalPlaces() {
        assertEquals("3.33", evaluate("10 / 3"))
        assertEquals("0.67", evaluate("2 / 3"))
        assertEquals("2.50", evaluate("10 / 4"))
    }

    @Test
    fun aFormulaThatCannotBeParsedReportsAnError() {
        assertEquals("Error", evaluate("2 +"))
        assertEquals("Error", evaluate("not a formula"))
        assertEquals("Error", evaluate("1 2"))
        assertEquals("Error", evaluate("* 5"))
    }

    @Test
    fun negativeFractionalResultsKeepTheirMinusSign() {
        assertEquals("-2.50", evaluate("-10 / 4"))
        assertEquals("-3.33", evaluate("-10 / 3"))
        assertEquals("-0.50", evaluate("-1 / 2"))
    }

    @Test
    fun dividingByZeroReportsInfinityRatherThanCrashing() {
        assertEquals("Infinity", evaluate("5 / 0"))
        assertEquals("-Infinity", evaluate("-5 / 0"))
        assertEquals("NaN", evaluate("0 / 0"))
    }

    @Test
    fun decimalResultsUseAFullStopNoMatterWhereTheUserLives() {
        val decimalResults = listOf(
            evaluate("10 / 4"),
            evaluate("10 / 3"),
            evaluate("-1 / 2")
        )

        decimalResults.forEach { result ->
            assertTrue("." in result, "expected a full stop in $result")
            assertFalse("," in result, "expected no comma in $result")
        }
    }
}
