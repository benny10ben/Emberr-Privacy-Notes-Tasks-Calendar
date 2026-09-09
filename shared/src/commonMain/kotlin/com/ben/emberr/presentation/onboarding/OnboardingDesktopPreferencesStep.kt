package com.ben.emberr.presentation.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ben.emberr.presentation.shared.SubNoteOpenMode

@Composable
fun OnboardingDesktopPreferencesStep(viewModel: OnboardingViewModel) {
    val subNoteOpenModeName by viewModel.subNoteOpenMode.collectAsState()
    val showScrollbar by viewModel.showScrollbar.collectAsState()
    val selectedSubNoteOpenMode = runCatching { SubNoteOpenMode.valueOf(subNoteOpenModeName) }
        .getOrDefault(SubNoteOpenMode.SIDE_PANEL)

    OnboardingStepScaffold(
        title = "Desktop Layout",
        description = "Choose how a linked sub-note opens next to whatever you are already writing."
    ) {
        OnboardingWindowMock(mode = selectedSubNoteOpenMode)

        Spacer(modifier = Modifier.height(22.dp))

        OnboardingSegmentedControl(
            options = SubNoteOpenMode.entries.map { it.name to it.compactLabel() },
            selectedKey = subNoteOpenModeName,
            onSelect = viewModel::setSubNoteOpenMode
        )

        Spacer(modifier = Modifier.height(18.dp))

        OnboardingToggleCard(
            title = "Show Scrollbar",
            description = "Shows scrollbars in the sidebar, editor and note lists.",
            checked = showScrollbar,
            onCheckedChange = viewModel::setShowScrollbar
        )
    }
}

private const val MockSidebarWidthFraction = 0.24f
private const val MockFullPanelWidthFraction = 1f - MockSidebarWidthFraction

private fun SubNoteOpenMode.compactLabel(): String = when (this) {
    SubNoteOpenMode.SIDE_PANEL -> "Side Panel"
    SubNoteOpenMode.CENTER_DIALOG -> "Dialog"
    SubNoteOpenMode.FULL_RIGHT_PANEL -> "Full Panel"
}

@Composable
private fun OnboardingWindowMock(mode: SubNoteOpenMode) {
    val windowShape = RoundedCornerShape(16.dp)
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(198.dp)
            .clip(windowShape)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, onboardingHairlineColor(), windowShape)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            OnboardingMockSidebar()
            OnboardingMockEditor()
        }

        Crossfade(
            targetState = mode,
            animationSpec = tween(320),
            label = "onboarding-subnote-mode"
        ) { currentMode ->
            val panelColor = accent.copy(alpha = 0.16f)
            val panelBorder = accent.copy(alpha = 0.35f)

            Box(modifier = Modifier.fillMaxSize()) {
                when (currentMode) {
                    SubNoteOpenMode.SIDE_PANEL -> OnboardingMockPanel(
                        fillColor = panelColor,
                        borderColor = panelBorder,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(0.34f)
                    )

                    SubNoteOpenMode.CENTER_DIALOG -> OnboardingMockPanel(
                        fillColor = panelColor,
                        borderColor = panelBorder,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(0.68f)
                            .fillMaxWidth(0.56f),
                        cornerRadius = 10.dp
                    )

                    SubNoteOpenMode.FULL_RIGHT_PANEL -> OnboardingMockPanel(
                        fillColor = panelColor,
                        borderColor = panelBorder,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(MockFullPanelWidthFraction)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingMockSidebar() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(MockSidebarWidthFraction)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        repeat(5) { index ->
            OnboardingMockLine(widthFraction = if (index == 0) 0.9f else 0.72f, height = 7.dp)
        }
    }
}

@Composable
private fun OnboardingMockEditor() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        OnboardingMockLine(widthFraction = 0.55f, height = 12.dp, opacity = 0.22f)
        Spacer(modifier = Modifier.height(3.dp))
        repeat(6) { index ->
            OnboardingMockLine(widthFraction = if (index % 3 == 2) 0.6f else 0.94f, height = 7.dp)
        }
    }
}

@Composable
private fun OnboardingMockLine(
    widthFraction: Float,
    height: Dp,
    opacity: Float = 0.12f
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = opacity))
    )
}

@Composable
private fun OnboardingMockPanel(
    fillColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp
) {
    val panelShape = RoundedCornerShape(cornerRadius)

    Column(
        modifier = modifier
            .clip(panelShape)
            .background(MaterialTheme.colorScheme.background)
            .background(fillColor)
            .border(1.dp, borderColor, panelShape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OnboardingMockLine(widthFraction = 0.7f, height = 9.dp, opacity = 0.26f)
        repeat(3) {
            OnboardingMockLine(widthFraction = 0.9f, height = 6.dp, opacity = 0.16f)
        }
    }
}
