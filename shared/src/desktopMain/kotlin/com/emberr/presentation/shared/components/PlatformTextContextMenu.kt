package com.emberr.presentation.shared.components

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalFoundationApi::class)
private object EmberrContextMenuRepresentation : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status !is ContextMenuState.Status.Open) return

        val menuItems = items()
        if (menuItems.isEmpty()) return

        val positionProvider = remember(status.rect) {
            MenuAtClickPositionProvider(
                IntOffset(status.rect.left.toInt(), status.rect.top.toInt())
            )
        }

        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = { state.status = ContextMenuState.Status.Closed },
            properties = PopupProperties(focusable = true)
        ) {
            EmberrPopupMenuSurface {
                menuItems.forEach { item ->
                    EmberrPopupMenuRow(item.label) {
                        state.status = ContextMenuState.Status.Closed
                        item.onClick()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun EmberrTextContextMenu(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContextMenuRepresentation provides EmberrContextMenuRepresentation,
        content = content
    )
}
