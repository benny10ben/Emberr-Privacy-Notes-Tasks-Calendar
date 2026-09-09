package com.ben.emberr.presentation.rag

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ben.emberr.domain.util.isDesktopPlatform
import com.ben.emberr.presentation.onboarding.LocalOnboardingBarInsets
import com.ben.emberr.presentation.onboarding.OnboardingBackButton
import com.ben.emberr.presentation.onboarding.OnboardingBarInsets
import com.ben.emberr.presentation.onboarding.OnboardingChatMock
import com.ben.emberr.presentation.onboarding.OnboardingContentMaxWidth
import com.ben.emberr.presentation.onboarding.OnboardingHeader
import com.ben.emberr.presentation.onboarding.OnboardingHighlight
import com.ben.emberr.presentation.onboarding.OnboardingHighlightGrid
import com.ben.emberr.presentation.onboarding.OnboardingHighlightSwipeDeck
import com.ben.emberr.presentation.onboarding.OnboardingPastelColors
import com.ben.emberr.presentation.onboarding.OnboardingStepPage
import com.ben.emberr.presentation.onboarding.onboardingSubtitleColor
import com.ben.emberr.presentation.onboarding.onboardingSurfaceColor
import com.ben.emberr.presentation.shared.components.EmberrButtonPrimary
import com.ben.emberr.presentation.shared.stableStatusBarsPadding
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.logo_chatgpt
import emberr.shared.generated.resources.logo_claude
import emberr.shared.generated.resources.logo_gemini
import emberr.shared.generated.resources.logo_grok
import emberr.shared.generated.resources.logo_llama
import emberr.shared.generated.resources.logo_mistral
import emberr.shared.generated.resources.shield_alert
import emberr.shared.generated.resources.timer_reset
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private enum class SetupSlide { Privacy, Models, Download }

private enum class ModelProvider(
    val label: String,
    val logo: DrawableResource,
    val isMonochromeMark: Boolean
) {
    Claude("Claude", Res.drawable.logo_claude, isMonochromeMark = false),
    ChatGpt("ChatGPT", Res.drawable.logo_chatgpt, isMonochromeMark = true),
    Gemini("Gemini", Res.drawable.logo_gemini, isMonochromeMark = false),
    Grok("Grok", Res.drawable.logo_grok, isMonochromeMark = true),
    Llama("Llama", Res.drawable.logo_llama, isMonochromeMark = true),
    Mistral("Mistral", Res.drawable.logo_mistral, isMonochromeMark = false)
}

private val TopBarIconButtonSize = 44.dp
private val TopBarProgressStartInset = 86.dp
private val TopBarProgressEndInset = 40.dp
private val ProviderTileSize = 60.dp
private val ProviderLogoSize = 26.dp

@Composable
internal fun EmbeddingSetupScreen(
    state: EmbeddingSetupState,
    sidePadding: Dp,
    isResumable: Boolean,
    onDownloadClick: () -> Unit,
    onPauseClick: () -> Unit,
    onProceedClick: () -> Unit
) {
    var slide by remember {
        mutableStateOf(
            if (state == EmbeddingSetupState.Required && !isResumable) {
                SetupSlide.Privacy
            } else {
                SetupSlide.Download
            }
        )
    }

    LaunchedEffect(state) {
        if (state is EmbeddingSetupState.Downloading ||
            state is EmbeddingSetupState.Indexing ||
            state == EmbeddingSetupState.DownloadComplete
        ) {
            slide = SetupSlide.Download
        }
    }

    val onBackClick: (() -> Unit)? = when {
        slide == SetupSlide.Models -> {
            { slide = SetupSlide.Privacy }
        }

        slide == SetupSlide.Download && state.allowsReturningToModels() -> {
            { slide = SetupSlide.Models }
        }

        else -> null
    }

    val onPrimaryClick: () -> Unit = {
        when (slide) {
            SetupSlide.Privacy -> slide = SetupSlide.Models
            SetupSlide.Models -> slide = SetupSlide.Download
            SetupSlide.Download -> when (state) {
                EmbeddingSetupState.Required -> onDownloadClick()
                is EmbeddingSetupState.Downloading -> onPauseClick()
                is EmbeddingSetupState.DownloadFailed -> onDownloadClick()
                EmbeddingSetupState.DownloadComplete -> onProceedClick()
                else -> Unit
            }
        }
    }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompositionLocalProvider(
            LocalOnboardingBarInsets provides OnboardingBarInsets(
                top = with(density) { topBarHeightPx.toDp() },
                bottom = with(density) { bottomBarHeightPx.toDp() }
            )
        ) {
            AnimatedContent(
                targetState = slide,
                modifier = Modifier.fillMaxSize(),
                label = "embedding-setup-slide",
                transitionSpec = {
                    val movingForward = targetState.ordinal >= initialState.ordinal
                    val enter = fadeIn(tween(400, delayMillis = 50)) +
                            slideInHorizontally(tween(400)) { width ->
                                if (movingForward) width / 8 else -width / 8
                            }
                    enter togetherWith fadeOut(tween(200))
                }
            ) { visibleSlide ->
                when (visibleSlide) {
                    SetupSlide.Privacy -> PrivacySlide()
                    SetupSlide.Models -> ModelsSlide()
                    SetupSlide.Download -> DownloadSlide(state = state, isResumable = isResumable)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates ->
                    topBarHeightPx = coordinates.size.height.toFloat()
                }
        ) {
            SetupProgressBar(
                progress = (slide.ordinal + 1) / SetupSlide.entries.size.toFloat()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates ->
                    bottomBarHeightPx = coordinates.size.height.toFloat()
                }
        ) {
            SetupBottomBar(
                state = state,
                label = primaryActionLabel(slide, state, isResumable),
                sidePadding = sidePadding,
                onPrimaryClick = onPrimaryClick,
                onBackClick = onBackClick
            )
        }
    }
}

@Composable
private fun SetupProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "embedding-setup-progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDesktopPlatform) Modifier else Modifier.stableStatusBarsPadding())
            .padding(
                start = TopBarProgressStartInset,
                end = TopBarProgressEndInset,
                top = if (isDesktopPlatform) 16.dp else 10.dp,
                bottom = 8.dp
            )
            .height(TopBarIconButtonSize),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(100)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
        )
    }
}

@Composable
private fun SetupBottomBar(
    state: EmbeddingSetupState,
    label: String,
    sidePadding: Dp,
    onPrimaryClick: () -> Unit,
    onBackClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = if (isDesktopPlatform) 36.dp else 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = sidePadding)
                .widthIn(max = OnboardingContentMaxWidth)
                .fillMaxWidth()
        ) {
            if (state is EmbeddingSetupState.Indexing) {
                IndexingProgress(state = state)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackClick != null) {
                        OnboardingBackButton(onClick = onBackClick)
                    }

                    EmberrButtonPrimary(
                        text = label,
                        onClick = onPrimaryClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IndexingProgress(state: EmbeddingSetupState.Indexing) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Indexing notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (state.total > 0) "${state.completed} of ${state.total}" else "Starting",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = onboardingSubtitleColor()
            )
        }
        Spacer(Modifier.height(12.dp))
        ProgressTrack(
            progress = state.progressFraction() ?: 0f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PrivacySlide() {
    val highlights = listOf(
        OnboardingHighlight(
            color = OnboardingPastelColors.Green,
            icon = painterResource(Res.drawable.shield_alert),
            title = "On-Device Answers",
            description = "Your question and the notes it reads never leave this device."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Blue,
            icon = rememberVectorPainter(Icons.Default.Lock),
            title = "Local Index",
            description = "Notes are turned into a searchable index stored on your disk."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Purple,
            icon = rememberVectorPainter(Icons.Default.VisibilityOff),
            title = "Nothing Logged",
            description = "No analytics, no query history sent anywhere."
        ),
        OnboardingHighlight(
            color = OnboardingPastelColors.Amber,
            icon = painterResource(Res.drawable.timer_reset),
            title = "Works Offline",
            description = "Once the model is installed, search needs no network."
        )
    )

    OnboardingStepPage {
        OnboardingHeader(
            title = "Ask Your Notes Anything",
            subtitle = "Your questions are answered on this device."
        )

        if (isDesktopPlatform) {
            OnboardingHighlightGrid(highlights = highlights)
        } else {
            OnboardingHighlightSwipeDeck(highlights = highlights)
        }
    }
}

@Composable
private fun ModelsSlide() {
    OnboardingStepPage {
        OnboardingHeader(
            title = "Local First, Cloud Optional",
            subtitle = null
        )

        ProviderTileGrid()

        Spacer(modifier = Modifier.height(20.dp))

        OnboardingHighlightGrid(
            highlights = listOf(
                OnboardingHighlight(
                    color = OnboardingPastelColors.Green,
                    icon = rememberVectorPainter(Icons.Default.Check),
                    title = "Local Model Included",
                    description = "Works out of the box with no key and no account."
                ),
                OnboardingHighlight(
                    color = OnboardingPastelColors.Teal,
                    icon = rememberVectorPainter(Icons.Default.CloudQueue),
                    title = "Bring Your Own Key",
                    description = "Add a cloud provider later in Settings, entirely optional."
                )
            )
        )
    }
}

@Composable
private fun ProviderTileGrid() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModelProvider.entries.chunked(3).forEach { rowProviders ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowProviders.forEach { provider ->
                    ProviderTile(provider = provider, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProviderTile(provider: ModelProvider, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(ProviderTileSize)
            .clip(RoundedCornerShape(16.dp))
            .background(onboardingSurfaceColor()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(provider.logo),
            contentDescription = provider.label,
            modifier = Modifier.size(ProviderLogoSize),
            contentScale = ContentScale.Fit,
            colorFilter = if (provider.isMonochromeMark) {
                ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            } else {
                null
            }
        )
    }
}

@Composable
private fun DownloadSlide(state: EmbeddingSetupState, isResumable: Boolean) {
    val failureMessage = (state as? EmbeddingSetupState.DownloadFailed)?.message

    OnboardingStepPage {
        OnboardingHeader(
            title = state.downloadHeadline(isResumable),
            subtitle = failureMessage
        )

        OnboardingChatMock(
            question = "What did I write about my research last month?",
            answer = "You outlined three experiments and flagged the second one as blocked.",
            placeholder = "Ask about your notes"
        )

        Spacer(modifier = Modifier.height(18.dp))

        EmbeddingModelCard(state = state)
    }
}

@Composable
private fun EmbeddingModelCard(state: EmbeddingSetupState) {
    val progress = state.progressFraction()
    val isComplete = state == EmbeddingSetupState.DownloadComplete
    val isIndexing = state is EmbeddingSetupState.Indexing
    val isFailed = state is EmbeddingSetupState.DownloadFailed
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(onboardingSurfaceColor())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isComplete) Icons.Default.Check else Icons.Default.Download,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Embedding model",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Runs offline once installed",
                    style = MaterialTheme.typography.labelSmall,
                    color = onboardingSubtitleColor()
                )
            }

            if (progress != null) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        }

        AnimatedVisibility(
            visible = progress != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(120))
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                ProgressTrack(progress = progress ?: 0f, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(16.dp))

        SetupStepRow(
            label = "Model downloaded",
            isDone = isComplete || isIndexing,
            isActive = state is EmbeddingSetupState.Downloading,
            isFailed = isFailed
        )

        Spacer(Modifier.height(10.dp))

        SetupStepRow(
            label = "Notes indexed",
            isDone = false,
            isActive = isIndexing,
            isFailed = false
        )
    }
}

@Composable
private fun ProgressTrack(progress: Float, modifier: Modifier = Modifier) {
    val filled by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessLow),
        label = "embedding-progress-fill"
    )

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    ) {
        if (filled > 0.004f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(filled)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun SetupStepRow(
    label: String,
    isDone: Boolean,
    isActive: Boolean,
    isFailed: Boolean
) {
    val accent = MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            when {
                isDone -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(13.dp)
                )

                isActive -> Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )

                else -> Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFailed) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            }
                        )
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDone || isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            },
            textAlign = TextAlign.Start
        )
    }
}

private fun EmbeddingSetupState.progressFraction(): Float? = when (this) {
    is EmbeddingSetupState.Downloading -> progress.coerceIn(0f, 1f)
    is EmbeddingSetupState.Indexing ->
        if (total > 0) (completed / total.toFloat()).coerceIn(0f, 1f) else 0f

    else -> null
}

private fun EmbeddingSetupState.allowsReturningToModels(): Boolean =
    this == EmbeddingSetupState.Required || this is EmbeddingSetupState.DownloadFailed

private fun EmbeddingSetupState.downloadHeadline(isResumable: Boolean): String = when (this) {
    EmbeddingSetupState.Required ->
        if (isResumable) "Finish The Download" else "Add The On-Device Model"

    is EmbeddingSetupState.Downloading -> "Getting The Model Ready"
    is EmbeddingSetupState.DownloadFailed -> "The Download Stopped"
    EmbeddingSetupState.DownloadComplete -> "The Model Is Ready"
    is EmbeddingSetupState.Indexing -> "Reading Through Your Notes"
    else -> ""
}

private fun primaryActionLabel(
    slide: SetupSlide,
    state: EmbeddingSetupState,
    isResumable: Boolean
): String = if (slide != SetupSlide.Download) "Continue" else when (state) {
    EmbeddingSetupState.Required -> if (isResumable) "Resume download" else "Download model"
    is EmbeddingSetupState.Downloading -> "Pause download"
    is EmbeddingSetupState.DownloadFailed -> if (isResumable) "Resume download" else "Try again"
    EmbeddingSetupState.DownloadComplete -> "Index my notes"
    else -> "Continue"
}
