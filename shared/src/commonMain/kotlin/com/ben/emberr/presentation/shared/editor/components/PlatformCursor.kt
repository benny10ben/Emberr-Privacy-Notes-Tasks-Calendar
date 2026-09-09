package com.ben.emberr.presentation.shared.editor.components

import androidx.compose.ui.Modifier

enum class DesktopCursor { HAND, RESIZE_HORIZONTAL }
expect fun Modifier.desktopPointerCursor(cursor: DesktopCursor): Modifier