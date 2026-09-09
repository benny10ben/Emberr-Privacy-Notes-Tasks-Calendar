package com.ben.emberr.presentation.shared.components

import androidx.compose.runtime.Composable

/**
 * Intercepts platform-specific back navigation events (like hardware buttons or back gestures).
 */
@Composable
expect fun KmpBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)