package com.emberr.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.AppPermission
import com.emberr.domain.util.isDesktopPlatform
import com.emberr.domain.util.rememberAppPermissionCoordinator
import com.emberr.ui.theme.LocalAppIsDark
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.bell
import emberr.shared.generated.resources.calendar_clock
import emberr.shared.generated.resources.camera
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.cog
import emberr.shared.generated.resources.folder_sync
import emberr.shared.generated.resources.microphone
import emberr.shared.generated.resources.pen_square
import org.jetbrains.compose.resources.painterResource

private data class OnboardingPermissionItem(
    val permission: AppPermission,
    val color: Color,
    val icon: Painter,
    val title: String,
    val description: String
)

@Composable
fun OnboardingFinishStep() {
    if (isDesktopPlatform) {
        DesktopFinishStep()
    } else {
        MobileFinishStep()
    }
}

@Composable
private fun DesktopFinishStep() {
    OnboardingStepPage {
        OnboardingHeader(
            title = "You're All Set",
            subtitle = "Everything stays on this device."
        )

        OnboardingHighlightGrid(
            highlights = listOf(
                OnboardingHighlight(
                    color = OnboardingPastelColors.Green,
                    icon = painterResource(Res.drawable.pen_square),
                    title = "Start Writing",
                    description = "Blocks for text, tables, images and more."
                ),
                OnboardingHighlight(
                    color = OnboardingPastelColors.Blue,
                    icon = painterResource(Res.drawable.calendar_clock),
                    title = "Daily Reminders",
                    description = "Attach a time to any note or task."
                ),
                OnboardingHighlight(
                    color = OnboardingPastelColors.Amber,
                    icon = painterResource(Res.drawable.folder_sync),
                    title = "Pair Devices",
                    description = "Sync over your own network when you need it."
                ),
                OnboardingHighlight(
                    color = OnboardingPastelColors.Purple,
                    icon = painterResource(Res.drawable.cog),
                    title = "Tune It Later",
                    description = "Every choice here lives in Settings."
                )
            )
        )
    }
}

@Composable
private fun MobileFinishStep() {
    val permissionCoordinator = rememberAppPermissionCoordinator()
    val permissionItems = mobilePermissionItems()
    val grantedCount = permissionItems.count { permissionCoordinator.isGranted(it.permission) }

    OnboardingStepPage {
        OnboardingHeader(
            title = "You're All Set",
            subtitle = "Turn on only what you plan to use."
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            permissionItems.forEach { item ->
                OnboardingPermissionCard(
                    item = item,
                    isGranted = permissionCoordinator.isGranted(item.permission),
                    onRequest = { permissionCoordinator.request(item.permission) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "$grantedCount of ${permissionItems.size} enabled - everything works without them.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun mobilePermissionItems(): List<OnboardingPermissionItem> = listOf(
    OnboardingPermissionItem(
        permission = AppPermission.Notifications,
        color = OnboardingPastelColors.Green,
        icon = painterResource(Res.drawable.bell),
        title = "Notifications",
        description = "Deliver your daily reminders."
    ),
    OnboardingPermissionItem(
        permission = AppPermission.ExactAlarms,
        color = OnboardingPastelColors.Amber,
        icon = painterResource(Res.drawable.clock_circle),
        title = "Alarms & Reminders",
        description = "Fire reminders to the exact minute."
    ),
    OnboardingPermissionItem(
        permission = AppPermission.Microphone,
        color = OnboardingPastelColors.Blue,
        icon = painterResource(Res.drawable.microphone),
        title = "Microphone",
        description = "Record voice notes and dictate text."
    ),
    OnboardingPermissionItem(
        permission = AppPermission.Camera,
        color = OnboardingPastelColors.Rose,
        icon = painterResource(Res.drawable.camera),
        title = "Camera",
        description = "Capture photos and scan QR codes."
    )
)

@Composable
private fun OnboardingPermissionCard(
    item: OnboardingPermissionItem,
    isGranted: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val cardShape = RoundedCornerShape(18.dp)

    val cardBackgroundColor by animateColorAsState(
        targetValue = item.color.copy(
            alpha = when {
                isGranted && isDark -> 0.24f
                isGranted -> 0.28f
                isDark -> 0.14f
                else -> 0.18f
            }
        ),
        animationSpec = tween(220),
        label = "permission-card-background"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBackgroundColor)
            .clickable(enabled = !isGranted, onClick = onRequest)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = if (isDark) 0.32f else 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        OnboardingPermissionStatusBadge(color = item.color, isGranted = isGranted)
    }
}

@Composable
private fun OnboardingPermissionStatusBadge(
    color: Color,
    isGranted: Boolean
) {
    val badgeBackgroundColor by animateColorAsState(
        targetValue = if (isGranted) color else Color.Transparent,
        animationSpec = tween(220),
        label = "permission-badge-background"
    )
    val badgeBorderColor by animateColorAsState(
        targetValue = if (isGranted) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
        },
        animationSpec = tween(220),
        label = "permission-badge-border"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(badgeBackgroundColor)
            .border(width = 1.5.dp, color = badgeBorderColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isGranted,
            enter = scaleIn(tween(220)) + fadeIn(tween(220)),
            exit = scaleOut(tween(150)) + fadeOut(tween(150))
        ) {
            Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
