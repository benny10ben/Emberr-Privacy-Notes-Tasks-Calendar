package com.emberr.presentation.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.emberr.presentation.desktop.tray.StatusNotifierTray
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import javax.swing.SwingUtilities

class TrayMenuAction(val label: String, val onSelected: () -> Unit)

private fun onUiThread(action: () -> Unit) = SwingUtilities.invokeLater(action)

private fun startSystemDrawnTray(
    iconResourcePath: String,
    tooltip: String,
    onIconClick: () -> Unit,
    currentActions: () -> List<TrayMenuAction>
): TrayIcon? = runCatching {
    if (!SystemTray.isSupported()) return null
    val iconUrl = TrayMenuAction::class.java.classLoader.getResource(iconResourcePath) ?: return null

    val menu = PopupMenu()
    currentActions().forEach { action ->
        val item = MenuItem(action.label)
        item.addActionListener { onUiThread { action.onSelected() } }
        menu.add(item)
    }

    val trayIcon = TrayIcon(Toolkit.getDefaultToolkit().createImage(iconUrl), tooltip, menu)
    trayIcon.isImageAutoSize = true
    trayIcon.addActionListener { onUiThread { onIconClick() } }
    SystemTray.getSystemTray().add(trayIcon)

    trayIcon
}.getOrNull()

@Composable
fun EmberrSystemTray(
    iconResourcePath: String,
    tooltip: String,
    onIconClick: () -> Unit,
    actions: List<TrayMenuAction>
) {
    val latestOnIconClick by rememberUpdatedState(onIconClick)
    val latestActions by rememberUpdatedState(actions)

    DisposableEffect(iconResourcePath, tooltip) {
        val actionsOnUiThread = {
            latestActions.map { action ->
                TrayMenuAction(action.label) { onUiThread { action.onSelected() } }
            }
        }

        val desktopShellTray = StatusNotifierTray.start(
            iconResourcePath = iconResourcePath,
            tooltip = tooltip,
            onActivate = { onUiThread { latestOnIconClick() } },
            currentActions = actionsOnUiThread
        )

        val systemDrawnTray = if (desktopShellTray == null) {
            startSystemDrawnTray(
                iconResourcePath = iconResourcePath,
                tooltip = tooltip,
                onIconClick = { latestOnIconClick() },
                currentActions = { latestActions }
            )
        } else {
            null
        }

        onDispose {
            desktopShellTray?.stop()
            systemDrawnTray?.let { icon ->
                runCatching { SystemTray.getSystemTray().remove(icon) }
            }
        }
    }
}
