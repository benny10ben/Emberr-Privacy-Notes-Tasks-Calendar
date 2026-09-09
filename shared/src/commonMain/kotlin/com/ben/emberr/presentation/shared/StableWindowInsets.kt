package com.ben.emberr.presentation.shared

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo

// Snapshots a WindowInsets to its current pixel values and holds that snapshot while the window is
// unfocused, so a system dialog (e.g. an Intent chooser) that transiently redispatches insets to our
// covered-but-still-visible window can't make status/nav bar padding flicker to zero and back.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberStableInsets(insets: WindowInsets): WindowInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused

    val left = insets.getLeft(density, layoutDirection)
    val top = insets.getTop(density)
    val right = insets.getRight(density, layoutDirection)
    val bottom = insets.getBottom(density)

    var snapshot by remember { mutableStateOf(WindowInsets(left, top, right, bottom)) }
    if (isWindowFocused) {
        snapshot = WindowInsets(left, top, right, bottom)
    }
    return snapshot
}

@Composable
fun Modifier.stableStatusBarsPadding(): Modifier =
    this.windowInsetsPadding(rememberStableInsets(WindowInsets.statusBars))

@Composable
fun rememberStableStatusBarsPadding(): PaddingValues =
    rememberStableInsets(WindowInsets.statusBars).asPaddingValues()
