package com.ben.emberr.presentation.shared.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PlatformVerticalScrollbar(listState: LazyListState, modifier: Modifier) = Unit

@Composable
internal actual fun PlatformVerticalScrollbar(scrollState: ScrollState, modifier: Modifier) = Unit

@Composable
internal actual fun PlatformVerticalScrollbar(gridState: LazyGridState, modifier: Modifier) = Unit

@Composable
internal actual fun PlatformHorizontalScrollbar(scrollState: ScrollState, modifier: Modifier) = Unit
