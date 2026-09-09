package com.ben.emberr.presentation.shared.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
private fun emberrScrollbarStyle(): ScrollbarStyle {
    val thumbColor = MaterialTheme.colorScheme.onSurface
    return remember(thumbColor) {
        ScrollbarStyle(
            minimalHeight = 24.dp,
            thickness = 4.dp,
            shape = RoundedCornerShape(4.dp),
            hoverDurationMillis = 300,
            unhoverColor = thumbColor.copy(alpha = 0.18f),
            hoverColor = thumbColor.copy(alpha = 0.42f)
        )
    }
}

@Composable
internal actual fun PlatformVerticalScrollbar(listState: LazyListState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(listState),
        modifier = modifier,
        style = emberrScrollbarStyle()
    )
}

@Composable
internal actual fun PlatformVerticalScrollbar(scrollState: ScrollState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        style = emberrScrollbarStyle()
    )
}

@Composable
internal actual fun PlatformVerticalScrollbar(gridState: LazyGridState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(gridState),
        modifier = modifier,
        style = emberrScrollbarStyle()
    )
}

@Composable
internal actual fun PlatformHorizontalScrollbar(scrollState: ScrollState, modifier: Modifier) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        style = emberrScrollbarStyle()
    )
}
