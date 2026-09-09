package com.ben.emberr.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.ben.emberr.domain.util.isDesktopPlatform
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.folder_sync
import emberr.shared.generated.resources.shield_alert
import org.jetbrains.compose.resources.painterResource

@Composable
fun OnboardingPrivacyStep() {
    val highlights = listOf(
        OnboardingHighlight(
            color = OnboardingPastelColors.Green,
            icon = painterResource(Res.drawable.shield_alert),
            title = "Local-First",
            description = "Every note is processed and stored on this device, never uploaded by default."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Purple,
            icon = rememberVectorPainter(Icons.Default.VisibilityOff),
            title = "No Trackers, No Ads",
            description = "Emberr collects no analytics and shows no ads."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Blue,
            icon = rememberVectorPainter(Icons.Default.Lock),
            title = "Encrypted Storage",
            description = "Your note content is encrypted at rest on disk."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Amber,
            icon = painterResource(Res.drawable.folder_sync),
            title = "Private Sync",
            description = "LAN sync is secured with a key derived only on your own devices."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Rose,
            icon = rememberVectorPainter(Icons.Default.VpnKey),
            title = "Self-Host Stays Sealed",
            description = "Encrypted on your device before upload - your server only ever holds ciphertext."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Teal,
            icon = rememberVectorPainter(Icons.Default.PersonOff),
            title = "No Account Needed",
            description = "No sign-up and no cloud identity. Emberr works the moment you open it."
        )
    )

    OnboardingStepPage {
        OnboardingHeader(
            title = "Private By Design",
            subtitle = null
        )

        if (isDesktopPlatform) {
            OnboardingHighlightGrid(highlights = highlights)
        } else {
            OnboardingHighlightSwipeDeck(highlights = highlights)
        }
    }
}
