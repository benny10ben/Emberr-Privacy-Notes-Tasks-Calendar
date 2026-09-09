package com.ben.emberr.presentation.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingAiStep(viewModel: OnboardingViewModel) {
    val aiFeaturesDisabled by viewModel.aiFeaturesDisabled.collectAsState()

    OnboardingStepScaffold(
        title = "Your AI Assistant",
        description = "Ask questions and get answers drawn from what you have already written."
    ) {
        OnboardingChatMock(
            question = "What did I decide about the trip?",
            answer = "You settled on the last week of March and booked the coastal route.",
            placeholder = "Ask about your notes"
        )

        Spacer(modifier = Modifier.height(18.dp))

        OnboardingToggleCard(
            title = "Enable AI Assistant",
            description = "Runs on this device. Nothing is sent anywhere unless you add your own API key.",
            checked = !aiFeaturesDisabled,
            onCheckedChange = { enabled -> viewModel.setAiFeaturesDisabled(!enabled) }
        )
    }
}
