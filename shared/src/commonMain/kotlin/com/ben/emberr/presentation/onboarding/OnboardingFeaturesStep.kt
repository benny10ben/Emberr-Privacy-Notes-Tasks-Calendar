package com.ben.emberr.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.ben.emberr.domain.util.isDesktopPlatform
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar
import emberr.shared.generated.resources.calendar_clock
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.timer_reset
import org.jetbrains.compose.resources.painterResource

@Composable
fun OnboardingFeaturesStep() {
    val highlights = listOf(
        OnboardingHighlight(
            color = OnboardingPastelColors.Blue,
            icon = rememberVectorPainter(Icons.AutoMirrored.Filled.Notes),
            title = "Block-Based Editor",
            description = "Mix text, checklists, tables, images, voice notes and documents in one note."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Amber,
            icon = painterResource(Res.drawable.calendar_clock),
            title = "Daily Journaling",
            description = "A running daily log that carries unfinished tasks into today."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Rose,
            icon = painterResource(Res.drawable.calendar),
            title = "Calendar",
            description = "See every task and reminder on a month view, repeats included."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Green,
            icon = painterResource(Res.drawable.timer_reset),
            title = "Universal Reminders",
            description = "Turn any checkbox into an exact-time reminder that notifies you."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Purple,
            icon = painterResource(Res.drawable.refresh_cw),
            title = "LAN Sync",
            description = "Sync directly between your own devices over your local network."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Teal,
            icon = rememberVectorPainter(Icons.Default.Storage),
            title = "Self-Host Sync",
            description = "Point Emberr at your own WebDAV server and keep full ownership."
        )
    )

    OnboardingStepPage {
        OnboardingHeader(
            title = "Everything In One Place",
            subtitle = null
        )

        if (isDesktopPlatform) {
            OnboardingHighlightGrid(highlights = highlights)
        } else {
            OnboardingHighlightSwipeDeck(highlights = highlights)
        }
    }
}
