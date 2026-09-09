package com.ben.emberr.presentation.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.ben.emberr.domain.util.isDesktopPlatform
import com.ben.emberr.presentation.shared.components.EmberrBottomSheet
import com.ben.emberr.presentation.shared.components.EmberrButtonPrimary
import com.ben.emberr.presentation.shared.components.EmberrDesktopMenu
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.cog
import emberr.shared.generated.resources.trash
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserSettings(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
) {
    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            UserSettingsDesktopMenu(
                onDismiss = onDismiss,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToTrash = onNavigateToTrash,
            )
        }
    } else {
        UserSettingsBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToTrash = onNavigateToTrash
        )
    }
}

// Desktop Popup Menu
@Composable
private fun UserSettingsDesktopMenu(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
) {
    Column(modifier = Modifier.width(220.dp).padding(vertical = 4.dp)) {

        DesktopMenuItem(
            icon = painterResource(Res.drawable.cog),
            text = "Settings",
            onClick = {
                onDismiss()
                onNavigateToSettings()
            }
        )

        DesktopMenuItem(
            icon = painterResource(Res.drawable.trash),
            text = "Trash",
            onClick = {
                onDismiss()
                onNavigateToTrash()
            }
        )
    }
}

@Composable
private fun DesktopMenuItem(
    icon: Painter,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

// Mobile Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSettingsBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit
) {
    EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "More") { closeAnd ->

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            BottomSheetItem(
                "Settings",
                painterResource(Res.drawable.cog)
            ) { closeAnd { onNavigateToSettings() } }

            BottomSheetItem(
                "Trash",
                painterResource(Res.drawable.trash)
            ) { closeAnd { onNavigateToTrash() } }

            EmberrButtonPrimary(
                text = "Close",
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun BottomSheetItem(text: String, icon: Painter, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}