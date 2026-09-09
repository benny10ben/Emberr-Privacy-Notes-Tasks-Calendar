package com.ben.emberr.domain.util

import com.ben.emberr.domain.model.CellData
import com.ben.emberr.domain.model.DatabaseColumn

/**
 * A lightweight engine used to parse and calculate math formulas inside database blocks.
 */
object FormulaEngine {

    /**
     * Finds column references like prop("Price") in a user's formula, swaps them out
     * for the actual numbers in that row, and then evaluates the final math equation.
     */
    fun evaluate(expression: String, rowCells: Map<String, CellData>, columns: List<DatabaseColumn>): CellData.Formula {
        if (expression.isBlank()) return CellData.Formula("")

        var parsedExpression = expression
        val propRegex = """prop\(['"]([^'"]+)['"]\)""".toRegex()

        parsedExpression = propRegex.replace(parsedExpression) { matchResult ->
            val colName = matchResult.groupValues[1]
            val colId = columns.find { it.name.equals(colName, ignoreCase = true) }?.id
            val referencedCell = if (colId != null) rowCells[colId] else null
            val numericValue = when (referencedCell) {
                is CellData.Number -> referencedCell.value
                is CellData.Formula -> referencedCell.result.toDoubleOrNull()
                else -> null
            }

            (numericValue ?: 0.0).toString()
        }

        val resultText = try {
            val result = evalMath(parsedExpression)
            if (result % 1.0 == 0.0) result.toLong().toString() else "%.2f".format(result)
        } catch (_: Exception) {
            "Error"
        }
        return CellData.Formula(resultText)
    }

    /**
     * A native recursive descent parser.
     * This evaluates standard math string expressions (addition, subtraction, multiplication, division, parentheses)
     * without needing to import a massive, heavy external math library.
     */
    private fun evalMath(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() { ch = if (++pos < str.length) str[pos].code else -1 }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }
}