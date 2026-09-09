package com.ben.emberr.presentation.mobile.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

internal const val DRAG_PREFIX_NOTE   = "note:"
internal const val DRAG_PREFIX_FOLDER = "folder:"

class DesktopListDragState {
    var dragging     by mutableStateOf(false)
    var payload      by mutableStateOf<String?>(null)
    var dropTargetId by mutableStateOf<String?>(null)
    var dropPosition by mutableStateOf(DropInsertPosition.BEFORE)
    var cursorY      by mutableStateOf(0f)

    fun startDrag(p: String) { payload = p; dragging = true }
    fun endDrag()            { dragging = false; payload = null; dropTargetId = null; cursorY = 0f; dropPosition = DropInsertPosition.BEFORE }
}

@Composable
fun rememberDesktopListDragState() = remember { DesktopListDragState() }

// Drag chip

@Composable
fun DesktopListDragChip(
    dragState: DesktopListDragState,
    labelForPayload: (String) -> String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = dragState.dragging,
        enter = fadeIn(tween(120)) + expandVertically(tween(120)),
        exit  = fadeOut(tween(80)) + shrinkVertically(tween(80)),
        modifier = modifier.zIndex(100f)
    ) {
        val density     = LocalDensity.current
        val label       = dragState.payload?.let { labelForPayload(it) } ?: ""
        val isFolder    = dragState.payload?.startsWith(DRAG_PREFIX_FOLDER) == true
        val chipOffsetY = with(density) { dragState.cursorY.toDp() - 16.dp }

        Row(
            modifier = Modifier
                .offset(y = chipOffsetY.coerceAtLeast(0.dp))
                .padding(start = 12.dp)
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isFolder) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
fun Modifier.desktopListDragTracker(
    dragState: DesktopListDragState,
    listState: LazyListState,
    rowKeys: List<String?>,
    payloadForKey: (String?) -> String?,
    isDropTarget: (key: String?, payload: String) -> Boolean,
    onDrop: (payload: String, targetKey: String, insertBefore: Boolean) -> Unit,
    rowHeightPx: Float,
    dragThresholdPx: Float = 8f
): Modifier {
    val currentRowKeys       by rememberUpdatedState(rowKeys)
    val currentPayloadForKey by rememberUpdatedState(payloadForKey)
    val currentIsDropTarget  by rememberUpdatedState(isDropTarget)
    val currentOnDrop        by rememberUpdatedState(onDrop)

    return this.pointerInput(dragState, listState) {
        awaitPointerEventScope {
            while (true) {
                val press = awaitPointerEvent()
                if (press.type != PointerEventType.Press) continue
                val pressChange = press.changes.firstOrNull() ?: continue
                if (pressChange.isConsumed) continue
                val pressPos = pressChange.position

                var pressedKey: String? = keyAtY(pressPos.y, listState, currentRowKeys)
                var dragStarted = false

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break

                    when (event.type) {
                        PointerEventType.Move -> {
                            val dist = (change.position - pressPos).getDistance()

                            if (!dragStarted && change.isConsumed) {
                                pressedKey = null
                            }

                            if (!dragStarted && dist > dragThresholdPx && pressedKey != null) {
                                val payload = currentPayloadForKey(pressedKey)
                                if (payload != null) {
                                    dragState.startDrag(payload)
                                    dragStarted = true
                                }
                            }

                            if (dragStarted) {
                                change.consume()
                                val cursorY = change.position.y
                                dragState.cursorY = cursorY
                                val payload = dragState.payload ?: ""

                                resolveDropTarget(
                                    cursorY      = cursorY,
                                    listState    = listState,
                                    rowKeys      = currentRowKeys,
                                    payload      = payload,
                                    isDropTarget = currentIsDropTarget,
                                    dragState    = dragState
                                )
                            }
                        }

                        PointerEventType.Release -> {
                            if (dragStarted) {
                                val target  = dragState.dropTargetId
                                val payload = dragState.payload
                                if (target != null && payload != null) {
                                    currentOnDrop(
                                        payload,
                                        target,
                                        dragState.dropPosition == DropInsertPosition.BEFORE
                                    )
                                }
                            }
                            dragState.endDrag()
                            pressedKey  = null
                            dragStarted = false
                            break
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

private fun resolveDropTarget(
    cursorY: Float,
    listState: LazyListState,
    rowKeys: List<String?>,
    payload: String,
    isDropTarget: (key: String?, payload: String) -> Boolean,
    dragState: DesktopListDragState
) {
    val layoutItems = listState.layoutInfo.visibleItemsInfo
    if (layoutItems.isEmpty()) return

    data class DragItem(
        val key: String,
        val top: Float,
        val bottom: Float,
        val isFolder: Boolean
    ) {
        val center get() = (top + bottom) / 2f
        val height get() = bottom - top
    }

    val draggable = layoutItems
        .mapNotNull { item ->
            val key = rowKeys.getOrNull(item.index) ?: return@mapNotNull null
            if (!isDropTarget(key, payload)) return@mapNotNull null
            val top    = item.offset.toFloat()
            val bottom = top + item.size.toFloat()
            DragItem(key, top, bottom, HomeItemKey.isFolder(key))
        }
        .sortedBy { it.top }

    if (draggable.isEmpty()) return
    if (cursorY < draggable.first().top) {
        dragState.dropTargetId = draggable.first().key
        dragState.dropPosition = DropInsertPosition.BEFORE
        return
    }
    if (cursorY >= draggable.last().bottom) {
        dragState.dropTargetId = draggable.last().key
        dragState.dropPosition = DropInsertPosition.AFTER
        return
    }

    val hit = draggable.firstOrNull { cursorY >= it.top && cursorY < it.bottom }

    if (hit != null) {
        if (hit.isFolder) {
            // BEFORE: top 10% of the row
            // INTO:   middle 80% of the row
            // AFTER:  bottom 10% of the row
            val edgeZone = hit.height * 0.20f
            dragState.dropTargetId = hit.key
            dragState.dropPosition = when {
                cursorY < hit.top    + edgeZone -> DropInsertPosition.BEFORE
                cursorY > hit.bottom - edgeZone -> DropInsertPosition.AFTER
                else                            -> DropInsertPosition.INTO
            }
        } else {
            dragState.dropTargetId = hit.key
            dragState.dropPosition = if (cursorY < hit.center) DropInsertPosition.BEFORE
            else                      DropInsertPosition.AFTER
        }
        return
    }

    var bestItem = draggable.first()
    var bestDist = Float.MAX_VALUE
    for (item in draggable) {
        val d1 = kotlin.math.abs(cursorY - item.top)
        val d2 = kotlin.math.abs(cursorY - item.bottom)
        if (d1 < bestDist) { bestDist = d1; bestItem = item }
        if (d2 < bestDist) { bestDist = d2; bestItem = item }
    }
    dragState.dropTargetId = bestItem.key
    dragState.dropPosition = if (cursorY < bestItem.center) DropInsertPosition.BEFORE
    else                           DropInsertPosition.AFTER
}

private fun keyAtY(y: Float, listState: LazyListState, rowKeys: List<String?>): String? {
    for (item in listState.layoutInfo.visibleItemsInfo) {
        val top    = item.offset.toFloat()
        val bottom = top + item.size.toFloat()
        if (y >= top && y < bottom) return rowKeys.getOrNull(item.index)
    }
    return null
}