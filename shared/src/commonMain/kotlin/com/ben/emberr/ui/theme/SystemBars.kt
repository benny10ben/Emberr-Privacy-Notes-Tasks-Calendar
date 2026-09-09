package com.ben.emberr.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Android-specific window inset management has been decoupled from the shared UI.
 * This acts as a multiplatform-safe no-op for Desktop/iOS targets.
 * (Actual Android status bar coloring will be handled at the MainActivity level).
 */
@Composable
fun SetSystemBars(
    statusBarColor: Color,
    darkIcons: Boolean
) {
    // No-op in commonMain
}