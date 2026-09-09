package com.ben.emberr.presentation.shared.components

import androidx.compose.ui.window.DialogProperties

actual fun fullScreenDialogProperties(): DialogProperties = DialogProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = true,
    usePlatformDefaultWidth = false
)
