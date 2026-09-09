package com.ben.emberr.presentation.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Wraps every case-insensitive occurrence of [query] inside [text] in [highlightStyle].
 * Called straight off the live (non-debounced) query text on every recomposition, so the
 * highlighted span tracks each keystroke even while the underlying result list is still
 * waiting on the debounced DB query to catch up.
 */
fun highlightMatches(
    text: String,
    query: String,
    highlightStyle: SpanStyle
): AnnotatedString = buildAnnotatedString {
    append(text)
    if (query.isBlank()) return@buildAnnotatedString

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var startIndex = 0
    while (true) {
        val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
        if (matchIndex < 0) break
        addStyle(highlightStyle, matchIndex, matchIndex + query.length)
        startIndex = matchIndex + query.length
    }
}

fun defaultHighlightStyle(highlightColor: Color): SpanStyle =
    SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)
