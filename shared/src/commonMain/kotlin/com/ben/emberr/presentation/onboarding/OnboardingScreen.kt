package com.ben.emberr.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ben.emberr.domain.util.isDesktopPlatform
import com.ben.emberr.presentation.shared.components.EmberrButtonPrimary
import com.ben.emberr.presentation.shared.stableStatusBarsPadding
import com.ben.emberr.ui.theme.LocalAppIsDark
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_left
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val steps = remember {
        buildList<@Composable () -> Unit> {
            add { OnboardingWelcomeStep() }
            add { OnboardingFeaturesStep() }
            add { OnboardingPrivacyStep() }
            add { OnboardingAppearanceStep(viewModel) }
            add { OnboardingAiStep(viewModel) }
            if (isDesktopPlatform) add { OnboardingDesktopPreferencesStep(viewModel) }
            add { OnboardingFinishStep() }
        }
    }

    var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
    val isLastStep = currentStepIndex == steps.lastIndex

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }
    val bottomBarHeightDp = with(density) { bottomBarHeightPx.toDp() }

    val finishScope = rememberCoroutineScope()
    var isFinishing by remember { mutableStateOf(false) }

    fun finishOnboarding() {
        if (isFinishing) return
        isFinishing = true

        finishScope.launch {
            viewModel.deletePreviewNoteIfExists()
            viewModel.completeOnboarding()
            onFinished()
        }
    }

    fun goToNextStep() {
        if (isLastStep) {
            finishOnboarding()
        } else {
            currentStepIndex++
        }
    }

    fun goToPreviousStep() {
        if (currentStepIndex > 0) currentStepIndex--
    }

    LaunchedEffect(Unit) {
        if (isDesktopPlatform) viewModel.ensurePreviewNoteCreated()
    }

    if (isDesktopPlatform) {
        val wizardPaneWeight by animateFloatAsState(
            targetValue = if (currentStepIndex == 0) 1f else 0.5f,
            animationSpec = tween(600),
            label = "wizard-pane-weight"
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val settledPaneWidth = maxWidth / 2
            val wizardWidth = maxWidth * wizardPaneWeight
            val editorAlpha = ((1f - wizardPaneWeight) / 0.5f).coerceIn(0f, 1f)

            OnboardingEditorPreview(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(settledPaneWidth)
                    .graphicsLayer(alpha = editorAlpha)
            )

            OnboardingWizardPane(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(wizardWidth)
                    .background(MaterialTheme.colorScheme.background),
                steps = steps,
                currentStepIndex = currentStepIndex,
                isLastStep = isLastStep,
                topBarHeightDp = topBarHeightDp,
                bottomBarHeightDp = bottomBarHeightDp,
                onTopBarHeightChanged = { topBarHeightPx = it },
                onBottomBarHeightChanged = { bottomBarHeightPx = it },
                onBack = ::goToPreviousStep,
                onNext = ::goToNextStep,
                onSkip = ::finishOnboarding
            )
        }
    } else {
        OnboardingWizardPane(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            steps = steps,
            currentStepIndex = currentStepIndex,
            isLastStep = isLastStep,
            topBarHeightDp = topBarHeightDp,
            bottomBarHeightDp = bottomBarHeightDp,
            onTopBarHeightChanged = { topBarHeightPx = it },
            onBottomBarHeightChanged = { bottomBarHeightPx = it },
            onBack = ::goToPreviousStep,
            onNext = ::goToNextStep,
            onSkip = ::finishOnboarding
        )
    }
}

@Composable
private fun OnboardingWizardPane(
    modifier: Modifier,
    steps: List<@Composable () -> Unit>,
    currentStepIndex: Int,
    isLastStep: Boolean,
    topBarHeightDp: Dp,
    bottomBarHeightDp: Dp,
    onTopBarHeightChanged: (Float) -> Unit,
    onBottomBarHeightChanged: (Float) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isDesktopPlatform) {
                        Modifier
                    } else {
                        Modifier.onboardingSwipeNavigation(
                            stepKey = currentStepIndex,
                            onSwipeForward = onNext,
                            onSwipeBackward = onBack
                        )
                    }
                )
        ) {
            CompositionLocalProvider(
                LocalOnboardingBarInsets provides OnboardingBarInsets(
                    top = topBarHeightDp,
                    bottom = bottomBarHeightDp
                )
            ) {
                AnimatedContent(
                    targetState = currentStepIndex,
                    transitionSpec = {
                        val movingForward = targetState >= initialState
                        val enter = fadeIn(tween(400, delayMillis = 50)) +
                                slideInHorizontally(tween(400)) { width ->
                                    if (movingForward) width / 8 else -width / 8
                                }
                        val exit = fadeOut(tween(200))
                        enter togetherWith exit
                    },
                    label = "onboarding-step"
                ) { stepIndex ->
                    steps[stepIndex]()
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> onTopBarHeightChanged(coordinates.size.height.toFloat()) }
        ) {
            OnboardingProgressBar(
                progress = (currentStepIndex + 1) / steps.size.toFloat(),
                showSkip = !isLastStep,
                onSkip = onSkip
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> onBottomBarHeightChanged(coordinates.size.height.toFloat()) }
        ) {
            OnboardingNavigationBar(
                showBack = currentStepIndex > 0,
                isLastStep = isLastStep,
                onBack = onBack,
                onNext = onNext
            )
        }
    }
}

private fun Modifier.reserveHeightOnlyWhen(isCollapsed: Boolean): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val usedWidth = if (isCollapsed) 0 else placeable.width
        layout(usedWidth, placeable.height) {
            if (!isCollapsed) placeable.place(0, 0)
        }
    }

private fun Modifier.onboardingSwipeNavigation(
    stepKey: Int,
    onSwipeForward: () -> Unit,
    onSwipeBackward: () -> Unit
): Modifier = pointerInput(stepKey) {
    val swipeThreshold = 72.dp.toPx()
    var horizontalDragTotal = 0f

    detectHorizontalDragGestures(
        onDragStart = { horizontalDragTotal = 0f },
        onDragCancel = { horizontalDragTotal = 0f },
        onDragEnd = {
            if (horizontalDragTotal <= -swipeThreshold) onSwipeForward()
            if (horizontalDragTotal >= swipeThreshold) onSwipeBackward()
            horizontalDragTotal = 0f
        },
        onHorizontalDrag = { _, dragAmount -> horizontalDragTotal += dragAmount }
    )
}

@Composable
private fun OnboardingProgressBar(
    progress: Float,
    showSkip: Boolean,
    onSkip: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "onboarding-progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDesktopPlatform) Modifier else Modifier.stableStatusBarsPadding())
            .padding(top = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = OnboardingContentMaxWidth)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(100)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
            )

            if (showSkip) {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .reserveHeightOnlyWhen(isCollapsed = !showSkip)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = showSkip) { onSkip() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun OnboardingNavigationBar(
    showBack: Boolean,
    isLastStep: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = if (isDesktopPlatform) 36.dp else 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = if (isDesktopPlatform) {
                Modifier.padding(horizontal = 20.dp)
            } else {
                Modifier
                    .padding(horizontal = 20.dp)
                    .widthIn(max = OnboardingContentMaxWidth)
                    .fillMaxWidth()
            },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                OnboardingBackButton(onClick = onBack)
            }

            EmberrButtonPrimary(
                text = if (isLastStep) "Start Using Emberr" else "Continue",
                onClick = onNext,
                modifier = if (isDesktopPlatform) Modifier else Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun OnboardingBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (LocalAppIsDark.current) Color(0xFF363636) else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.chevron_left),
            contentDescription = "Go back",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}
