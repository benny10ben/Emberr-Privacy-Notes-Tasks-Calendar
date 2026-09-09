package com.ben.emberr.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.emberr.ui.theme.LocalEmberrFontStyle
import com.ben.emberr.ui.theme.fontFamilyFor

@Composable
fun OnboardingWelcomeStep() {
    OnboardingStepPage {
        Text(
            text = "Emberr",
            fontFamily = fontFamilyFor(LocalEmberrFontStyle.current),
            fontWeight = FontWeight.Bold,
            fontSize = 52.sp,
            lineHeight = 58.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .width(40.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A quiet, offline-first home for your notes, tasks and calendars.",
            style = MaterialTheme.typography.bodyLarge,
            color = onboardingSubtitleColor(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "No accounts · Works offline · Encrypted on device",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
            textAlign = TextAlign.Center
        )
    }
}
