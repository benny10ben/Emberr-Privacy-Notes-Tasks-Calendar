package com.emberr.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.emberr.domain.util.isDesktopPlatform
import com.emberr.presentation.navigation.Screen
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.ui.theme.LocalEmberrFontStyle
import com.emberr.ui.theme.fontFamilyFor
import dev.chrisbanes.haze.HazeState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.astroid
import emberr.shared.generated.resources.daily
import emberr.shared.generated.resources.house
import emberr.shared.generated.resources.microphone
import emberr.shared.generated.resources.search
import org.jetbrains.compose.resources.painterResource

internal fun Modifier.customEmberrShadow(shape: Shape, elevation: Dp = 14.dp): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    spotColor = Color.Black.copy(alpha = 0.35f),
    ambientColor = Color.Black.copy(alpha = 0.20f)
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EmberrBottomBar(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    hazeState: HazeState,
    sharedTransitionScope: SharedTransitionScope,
    bottomBarAnimatedVisibilityScope: AnimatedVisibilityScope,
    currentRoute: String?,
    activeTab: String,
    onSearchClick: () -> Unit,
    onMicClick: () -> Unit,
    onAiIconTap: () -> Unit = {},
    isAiEnabled: Boolean = true,
    isListening: Boolean = false,
    partialText: String = "",
    isCompact: Boolean = false
) {
    val defaultBgColor = MaterialTheme.colorScheme.background.copy(alpha = 0.65f)
    val defaultContentColor = MaterialTheme.colorScheme.onSurface

    val barAnimationSpec = tween<Dp>(durationMillis = 350, easing = FastOutSlowInEasing)
    val barSize by animateDpAsState(
        targetValue = if (isCompact) 44.dp else 52.dp,
        animationSpec = barAnimationSpec
    )
    val bottomInset by animateDpAsState(
        targetValue = if (isCompact) 0.dp else 6.dp,
        animationSpec = barAnimationSpec
    )
    val horizontalInset by animateDpAsState(
        targetValue = if (isCompact) 24.dp else 12.dp,
        animationSpec = barAnimationSpec
    )
    val navItemHeight = barSize - 12.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = bottomInset, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = currentRoute != Screen.Note.route,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            val isMorphing = bottomBarAnimatedVisibilityScope.transition.isRunning
            val shadowElevation by animateDpAsState(
                targetValue = if (isMorphing) 0.dp else 14.dp,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalInset)
                    .height(barSize)
                    .then(
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "calendarBottomBarPill"),
                                animatedVisibilityScope = bottomBarAnimatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(durationMillis = 300, easing = FastOutSlowInEasing) }
                            )
                        }
                    )
                    .customEmberrShadow(CircleShape, elevation = shadowElevation)
                    .clip(CircleShape)
                    .emberrBlur(hazeState, EmberrBlur.Regular)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Row(
                    modifier = with(sharedTransitionScope) {
                        Modifier
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .skipToLookaheadSize()
                    },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        icon = painterResource(Res.drawable.daily),
                        isSelected = activeTab == Screen.Daily.route,
                        modifier = Modifier.weight(1f).height(navItemHeight)
                    ) {
                        if (currentRoute != Screen.Daily.route) navController.navigate(Screen.Daily.createRoute()) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    BottomNavItem(
                        icon = painterResource(Res.drawable.house),
                        isSelected = activeTab == Screen.Home.route,
                        modifier = Modifier.weight(1f).height(navItemHeight)
                    ) {
                        if (currentRoute != Screen.Home.route) navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    if (isAiEnabled) {
                        BottomNavItem(
                            icon = painterResource(Res.drawable.astroid),
                            isSelected = false,
                            modifier = Modifier.weight(1f).height(navItemHeight),
                            iconModifier = with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "aiIcon"),
                                    animatedVisibilityScope = bottomBarAnimatedVisibilityScope,
                                    boundsTransform = { _, _ -> tween(durationMillis = 300, easing = FastOutSlowInEasing) }
                                )
                            },
                            onClick = onAiIconTap
                        )
                    }
                    BottomNavItem(
                        icon = painterResource(Res.drawable.search),
                        isSelected = false,
                        modifier = Modifier.weight(1f).height(navItemHeight),
                        onClick = onSearchClick
                    )
                    if (!isDesktopPlatform) {
                        Surface(
                            shape = CircleShape,
                            color = if (isListening) defaultContentColor else Color.Transparent,
                            contentColor = if (isListening) MaterialTheme.colorScheme.background else defaultContentColor.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .height(navItemHeight)
                                .clip(CircleShape)
                                .clickable { onMicClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(painterResource(Res.drawable.microphone), "Mic", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        if (!isDesktopPlatform) {
            AnimatedVisibility(
                visible = isListening || partialText.isNotEmpty(),
                enter = fadeIn(tween(200)) + expandHorizontally(
                    expandFrom = Alignment.CenterHorizontally,
                    animationSpec = tween(200)
                ),
                exit = fadeOut(tween(200)) + shrinkHorizontally(
                    shrinkTowards = Alignment.CenterHorizontally,
                    animationSpec = tween(200)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(barSize + 8.dp))
                    .wrapContentWidth(unbounded = true, align = Alignment.CenterHorizontally)
            ) {
                Surface(
                    shape = RoundedCornerShape(100f),
                    color = defaultBgColor,
                    contentColor = defaultContentColor,
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .clip(RoundedCornerShape(100f))
                        .emberrBlur(hazeState, EmberrBlur.Regular)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = partialText.ifBlank { "Listening..." },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontFamily = fontFamilyFor(LocalEmberrFontStyle.current),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: Painter,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.surface
        else -> Color.Transparent
    }
    val iconColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(0.6f)

    Surface(
        shape = CircleShape,
        color = bgColor,
        contentColor = iconColor,
        border = BorderStroke(1.dp, Color.Transparent),
        modifier = modifier
            .clip(CircleShape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp).then(iconModifier))
        }
    }
}