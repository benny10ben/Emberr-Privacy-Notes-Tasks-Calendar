package com.ben.emberr.presentation.shared.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

object EmberrBlur {

    /** Chrome floating over content — icon buttons, pills, top bars. */
    val Regular: HazeStyle
        @Composable
        @ReadOnlyComposable
        get() = HazeStyle(
            backgroundColor = MaterialTheme.colorScheme.background,
            tints = listOf(
                HazeTint(MaterialTheme.colorScheme.background.copy(alpha = 0.45f))
            ),
            blurRadius = 32.dp,
            noiseFactor = 0f,
            fallbackTint = HazeTint(MaterialTheme.colorScheme.background)
        )

    /** Heavier separation — sheets, dialogs, anything that must hold long-form text. */
    val Thick: HazeStyle
        @Composable
        @ReadOnlyComposable
        get() = HazeStyle(
            backgroundColor = MaterialTheme.colorScheme.background,
            tints = listOf(
                HazeTint(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
            ),
            blurRadius = 40.dp,
            noiseFactor = 0f,
            fallbackTint = HazeTint(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
        )

    /** Glass over a dark photo, where a surface-coloured tint would fight the image. */
    val OnImage: HazeStyle
        @Composable
        @ReadOnlyComposable
        get() = HazeStyle(
            backgroundColor = Color.Black,
            tints = listOf(
                HazeTint(Color.White.copy(alpha = 0.10f)),
                HazeTint(Color.Black.copy(alpha = 0.18f))
            ),
            blurRadius = 36.dp,
            noiseFactor = 0f,
            fallbackTint = HazeTint(Color.Black.copy(alpha = 0.45f))
        )
}

/**
 * No-op when [hazeState] is null, so callers can pass it through unconditionally.
 */
fun Modifier.emberrBlur(
    hazeState: HazeState?,
    style: HazeStyle
): Modifier = if (hazeState == null) this else hazeEffect(hazeState, style)