package com.ben.emberr.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.emberr.presentation.shared.components.SelectedOptionBackground
import com.ben.emberr.ui.theme.LocalAppIsDark
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_up
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

val OnboardingContentMaxWidth = 470.dp

private const val OnboardingVisibleCardCount = 3
private const val OnboardingMaxCardRotationDegrees = 10f
private const val OnboardingCardFanDegrees = 1.5f
private val OnboardingDeckHeight = 252.dp
private val OnboardingCardSidePeek = 12.dp
private val OnboardingDeckSideInset = 20.dp

private fun peekDirectionForDepth(depth: Int): Float = when (depth) {
    0 -> 0f
    1 -> 1f
    else -> -1f
}

data class OnboardingBarInsets(val top: Dp, val bottom: Dp)

val LocalOnboardingBarInsets = compositionLocalOf { OnboardingBarInsets(top = 0.dp, bottom = 0.dp) }

object OnboardingPastelColors {
    val Blue = Color(0xFF5E8FEF)
    val Green = Color(0xFF4FAE7B)
    val Amber = Color(0xFFE0902E)
    val Purple = Color(0xFFA97BD1)
    val Rose = Color(0xFFDE7A88)
    val Teal = Color(0xFF3FA3A3)
}

data class OnboardingHighlight(
    val color: Color,
    val icon: Painter,
    val title: String,
    val description: String
)

@Composable
fun onboardingHeadlineStyle(): TextStyle {
    val baseStyle = MaterialTheme.typography.titleLarge
    return baseStyle.copy(
        fontSize = baseStyle.fontSize * 1.25f,
        lineHeight = baseStyle.fontSize * 1.55f
    )
}

@Composable
fun onboardingSubtitleColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

@Composable
fun onboardingHairlineColor(): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = if (LocalAppIsDark.current) 0.12f else 0.10f)

@Composable
fun onboardingSurfaceColor(): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = if (LocalAppIsDark.current) 0.07f else 0.05f)

@Composable
fun OnboardingStepPage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val barInsets = LocalOnboardingBarInsets.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = barInsets.top + 24.dp,
                    bottom = barInsets.bottom + 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = OnboardingContentMaxWidth)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
fun OnboardingStepScaffold(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    OnboardingStepPage(modifier = modifier) {
        Text(
            text = title,
            style = onboardingHeadlineStyle(),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = onboardingSubtitleColor(),
            textAlign = TextAlign.Center
        )

        if (content != null) {
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
fun OnboardingHeader(title: String, subtitle: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = onboardingHeadlineStyle(),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = onboardingSubtitleColor(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OnboardingHighlightGrid(
    highlights: List<OnboardingHighlight>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        highlights.chunked(2).forEach { rowHighlights ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowHighlights.forEach { highlight ->
                    OnboardingHighlightCard(
                        highlight = highlight,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                if (rowHighlights.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OnboardingHighlightCard(
    highlight: OnboardingHighlight,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val cardShape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(highlight.color.copy(alpha = if (isDark) 0.16f else 0.20f))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(highlight.color.copy(alpha = if (isDark) 0.32f else 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = highlight.icon,
                contentDescription = null,
                tint = highlight.color,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = highlight.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = highlight.description,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
    }
}

@Composable
fun OnboardingHighlightSwipeDeck(
    highlights: List<OnboardingHighlight>,
    modifier: Modifier = Modifier
) {
    var topCardIndex by remember(highlights.size) { mutableIntStateOf(0) }
    val swipeOffsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(OnboardingDeckHeight)
        ) {
            val deckWidthPx = with(density) { maxWidth.toPx() }
            val dismissThreshold = deckWidthPx * 0.26f

            for (depth in (OnboardingVisibleCardCount - 1) downTo 0) {
                val highlight = highlights[(topCardIndex + depth) % highlights.size]
                val isTopCard = depth == 0

                val depthModifier = if (isTopCard) {
                    Modifier
                        .graphicsLayer {
                            translationX = swipeOffsetX.value
                            rotationZ = if (size.width <= 0f) {
                                0f
                            } else {
                                (swipeOffsetX.value / size.width) * OnboardingMaxCardRotationDegrees
                            }
                        }
                        .pointerInput(topCardIndex, deckWidthPx) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (abs(swipeOffsetX.value) >= dismissThreshold) {
                                            val flyOutTarget = if (swipeOffsetX.value > 0f) {
                                                deckWidthPx * 1.5f
                                            } else {
                                                -deckWidthPx * 1.5f
                                            }
                                            swipeOffsetX.animateTo(flyOutTarget, tween(200))
                                            topCardIndex = (topCardIndex + 1) % highlights.size
                                            swipeOffsetX.snapTo(0f)
                                        } else {
                                            swipeOffsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.55f,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch { swipeOffsetX.animateTo(0f, tween(180)) }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        swipeOffsetX.snapTo(swipeOffsetX.value + dragAmount)
                                    }
                                }
                            )
                        }
                } else {
                    Modifier.graphicsLayer {
                        val swipeProgress = if (dismissThreshold <= 0f) {
                            0f
                        } else {
                            (abs(swipeOffsetX.value) / dismissThreshold).coerceIn(0f, 1f)
                        }
                        val peekPx = OnboardingCardSidePeek.toPx()
                        val restingPeek = peekPx * peekDirectionForDepth(depth)
                        val promotedPeek = peekPx * peekDirectionForDepth(depth - 1)
                        val restingFan = OnboardingCardFanDegrees * peekDirectionForDepth(depth)
                        val promotedFan = OnboardingCardFanDegrees * peekDirectionForDepth(depth - 1)
                        val restingScale = 1f - 0.03f * depth
                        val promotedScale = 1f - 0.03f * (depth - 1)

                        translationX = restingPeek + (promotedPeek - restingPeek) * swipeProgress
                        rotationZ = restingFan + (promotedFan - restingFan) * swipeProgress
                        scaleX = restingScale + (promotedScale - restingScale) * swipeProgress
                        scaleY = scaleX
                    }
                }

                OnboardingDeckCard(
                    highlight = highlight,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = OnboardingDeckSideInset)
                        .then(depthModifier)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            highlights.forEachIndexed { index, highlight ->
                val isActive = index == topCardIndex
                Box(
                    modifier = Modifier
                        .size(if (isActive) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) {
                                highlight.color
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Swipe the card to see more",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
        )
    }
}

@Composable
private fun OnboardingDeckCard(
    highlight: OnboardingHighlight,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val solidCardColor = highlight.color
        .copy(alpha = if (isDark) 0.18f else 0.22f)
        .compositeOver(MaterialTheme.colorScheme.background)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(solidCardColor)
            .padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(highlight.color.copy(alpha = if (isDark) 0.32f else 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = highlight.icon,
                contentDescription = null,
                tint = highlight.color,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = highlight.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = highlight.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun OnboardingSegmentedControl(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = options.indexOfFirst { it.first == selectedKey }.coerceAtLeast(0)
    val segmentHeight = 42.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(onboardingSurfaceColor())
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / options.size
        val highlightOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
            label = "onboarding-segment-offset"
        )

        Box(
            modifier = Modifier
                .offset(x = highlightOffset)
                .width(segmentWidth)
                .height(segmentHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEach { (key, label) ->
                val isSelected = key == selectedKey
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                    animationSpec = tween(200),
                    label = "onboarding-segment-label"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(segmentHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(key) }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    val backgroundColor by animateColorAsState(
        targetValue = onboardingSurfaceColor(),
        animationSpec = tween(200),
        label = "onboarding-toggle-background"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun OnboardingChatMock(
    question: String,
    answer: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, onboardingHairlineColor(), cardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .widthIn(max = 230.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Text(
            text = answer,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(onboardingSurfaceColor())
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_up),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun OnboardingSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        textAlign = TextAlign.Start,
        modifier = modifier.fillMaxWidth()
    )
}
