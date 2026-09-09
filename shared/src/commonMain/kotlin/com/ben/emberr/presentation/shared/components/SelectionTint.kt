package com.ben.emberr.presentation.shared.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SelectedOptionBackground: Color
    @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
