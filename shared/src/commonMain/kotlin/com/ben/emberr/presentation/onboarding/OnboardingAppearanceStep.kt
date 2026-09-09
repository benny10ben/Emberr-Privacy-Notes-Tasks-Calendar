package com.ben.emberr.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.emberr.presentation.shared.components.SelectedOptionBackground
import com.ben.emberr.ui.theme.FontStylePreference
import com.ben.emberr.ui.theme.fontFamilyFor

private val OnboardingFontSizeOptions = listOf(
    "SMALL" to "Small",
    "DEFAULT" to "Default",
    "LARGE" to "Large"
)

@Composable
fun OnboardingAppearanceStep(viewModel: OnboardingViewModel) {
    val fontStyleName by viewModel.fontStylePreference.collectAsState()
    val fontSizeName by viewModel.fontSizePreference.collectAsState()
    val selectedFontStyle = runCatching { FontStylePreference.valueOf(fontStyleName) }
        .getOrDefault(FontStylePreference.POPPINS)

    OnboardingStepScaffold(
        title = "Make It Yours",
        description = "Pick the font and text size you'll enjoy reading the most."
    ) {
        OnboardingSectionLabel(text = "Typeface")

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FontStylePreference.entries.chunked(3).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEach { option ->
                        OnboardingFontStyleOption(
                            option = option,
                            isSelected = option == selectedFontStyle,
                            onClick = { viewModel.setFontStylePreference(option.name) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OnboardingSectionLabel(text = "Text size")

        Spacer(modifier = Modifier.height(10.dp))

        OnboardingSegmentedControl(
            options = OnboardingFontSizeOptions,
            selectedKey = fontSizeName,
            onSelect = viewModel::setFontSizePreference
        )
    }
}

@Composable
private fun OnboardingFontStyleOption(
    option: FontStylePreference,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(16.dp)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) SelectedOptionBackground else onboardingSurfaceColor(),
        animationSpec = tween(200),
        label = "onboarding-font-background"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "onboarding-font-border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        },
        animationSpec = tween(200),
        label = "onboarding-font-content"
    )

    Column(
        modifier = modifier
            .clip(tileShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, tileShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Aa",
            fontFamily = fontFamilyFor(option),
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            color = contentColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = option.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
