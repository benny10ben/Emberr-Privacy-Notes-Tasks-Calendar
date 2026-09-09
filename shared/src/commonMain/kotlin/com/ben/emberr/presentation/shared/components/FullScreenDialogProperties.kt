package com.ben.emberr.presentation.shared.components

import androidx.compose.ui.window.DialogProperties

/**
 * Properties for a near-full-screen dialog that has to react to the software keyboard itself.
 *
 * A default Android dialog window swallows the ime insets and resizes its own window, which makes
 * Modifier.imePadding() inside the dialog double-compensate. Turning decorFitsSystemWindows off
 * hands the insets to the content instead, so the dialog can pad itself exactly once - and that
 * flag only exists on Android's DialogProperties, hence the expect/actual.
 */
expect fun fullScreenDialogProperties(): DialogProperties
