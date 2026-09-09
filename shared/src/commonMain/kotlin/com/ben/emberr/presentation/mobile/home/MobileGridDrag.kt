@file:OptIn(ExperimentalFoundationApi::class)

package com.ben.emberr.presentation.mobile.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

enum class MobileDropMode { REORDER, INTO }

class MobileGridDragState {
    var draggedKey by mutableStateOf<String?>(null)
        internal set
    var hoverKey by mutableStateOf<String?>(null)
        internal set
    var hoverMode by mutableStateOf(MobileDropMode.REORDER)
        internal set
    var floatingTopLeftInRoot by mutableStateOf(Offset.Zero)
        internal set
    var floatingSize by mutableStateOf(IntSize.Zero)
        internal set
    var pointerInGrid by mutableStateOf(Offset.Zero)
        internal set

    val isDragging: Boolean get() = draggedKey != null

    fun isDragged(itemKey: String) = draggedKey == itemKey

    fun isIntoTarget(itemKey: String) =
        isDragging && hoverMode == MobileDropMode.INTO && hoverKey == itemKey && draggedKey != itemKey

    internal fun reset() {
        draggedKey = null
        hoverKey = null
        hoverMode = MobileDropMode.REORDER
        floatingSize = IntSize.Zero
    }
}

@Composable
fun rememberMobileGridDragState() = remember { MobileGridDragState() }

@Composable
fun Modifier.mobileGridDragSource(
    itemKey: String,
    dragState: MobileGridDragState,
    gridState: LazyStaggeredGridState,
    gridOriginInRoot: Offset,
    dragEnabled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDrop: (hoverKey: String?, mode: MobileDropMode) -> Unit
): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentDragEnabled by rememberUpdatedState(dragEnabled)
    val currentGridOrigin by rememberUpdatedState(gridOriginInRoot)
    val haptics = LocalHapticFeedback.current

    var itemPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }

    return this
        .onGloballyPositioned {
            itemPositionInRoot = it.positionInRoot()
            itemSize = it.size
        }
        .pointerInput(itemKey) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

                var releasedEarly = false
                val tap = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    waitForUpOrCancellation().also { if (it == null) releasedEarly = true }
                }
                if (tap != null) {
                    currentOnClick()
                    return@awaitEachGesture
                }
                if (releasedEarly) return@awaitEachGesture

                haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                if (!currentDragEnabled) {
                    waitForUpOrCancellation()
                    currentOnLongPress()
                    return@awaitEachGesture
                }

                // Claims the pointer the instant the press wins, before the grid's scroll
                // gesture gets a chance to track slop on the next move.
                currentEvent.changes.forEach { it.consume() }

                // Snapshotted once: the live slot shifts while the preview order rearranges,
                // and the floating copy must follow the finger, not the slot.
                val grabOriginInRoot = itemPositionInRoot
                var travelled = Offset.Zero
                var pointerId = down.id

                // Hand-rolled instead of drag(): that helper drops the gesture the moment a
                // change arrives already consumed, which is exactly what the surrounding grid
                // scroll does. Here every move is claimed unconditionally, so once the long
                // press wins the item, scrolling can no longer steal it back.
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId }
                        ?: event.changes.firstOrNull { it.pressed }
                        ?: break
                    pointerId = change.id
                    if (!change.pressed) break

                    travelled += change.positionChange()
                    change.consume()

                    if (!dragState.isDragging && travelled.getDistance() > viewConfiguration.touchSlop) {
                        dragState.draggedKey = itemKey
                        dragState.floatingSize = itemSize
                    }

                    if (dragState.isDragged(itemKey)) {
                        dragState.floatingTopLeftInRoot = grabOriginInRoot + travelled
                        dragState.pointerInGrid =
                            grabOriginInRoot + down.position + travelled - currentGridOrigin
                        resolveGridHover(dragState.pointerInGrid, gridState, dragState)
                    }
                }

                val startedDragHere = dragState.isDragged(itemKey)
                val hoverKey = dragState.hoverKey
                val mode = dragState.hoverMode
                if (startedDragHere) dragState.reset()

                if (startedDragHere) currentOnDrop(hoverKey, mode) else currentOnLongPress()
            }
        }
}

// Auto-scrolling moves the cells out from under a finger that never moves, so the hover has
// to be recomputed from the last known pointer position after every scroll step.
fun MobileGridDragState.refreshHoverTarget(gridState: LazyStaggeredGridState) {
    if (!isDragging) return
    resolveGridHover(pointerInGrid, gridState, this)
}

// Distance-scaled edge scrolling: 0 at the edge zone boundary, full speed at the very edge.
fun MobileGridDragState.edgeScrollDelta(
    gridState: LazyStaggeredGridState,
    edgeSizePx: Float,
    maxStepPx: Float
): Float {
    if (!isDragging || edgeSizePx <= 0f) return 0f
    val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
    if (viewportHeight <= 0f) return 0f

    val y = pointerInGrid.y
    val fraction = when {
        y < edgeSizePx -> -(1f - y / edgeSizePx)
        y > viewportHeight - edgeSizePx -> (y - (viewportHeight - edgeSizePx)) / edgeSizePx
        else -> 0f
    }
    return fraction.coerceIn(-1f, 1f) * maxStepPx
}

private fun LazyStaggeredGridItemInfo.containsPointer(pointer: Offset): Boolean =
    pointer.x >= offset.x && pointer.x <= offset.x + size.width &&
            pointer.y >= offset.y && pointer.y <= offset.y + size.height

private fun LazyStaggeredGridItemInfo.centerOffset(): Offset =
    Offset(offset.x + size.width / 2f, offset.y + size.height / 2f)

// Cells keep fixed positions while only their contents move, so "the cell under the finger"
// is a stable target - it can't oscillate the way a before/after insert line does.
private fun resolveGridHover(
    pointerInGrid: Offset,
    gridState: LazyStaggeredGridState,
    dragState: MobileGridDragState
) {
    val candidates = gridState.layoutInfo.visibleItemsInfo.filter { info ->
        val key = info.key as? String ?: return@filter false
        HomeItemKey.isFolder(key) || HomeItemKey.isNote(key)
    }
    if (candidates.isEmpty()) {
        dragState.hoverKey = null
        dragState.hoverMode = MobileDropMode.REORDER
        return
    }

    val hovered = candidates.firstOrNull { it.containsPointer(pointerInGrid) }
    val target = hovered
        ?: candidates.minByOrNull { (it.centerOffset() - pointerInGrid).getDistanceSquared() }
        ?: return
    val targetKey = target.key as String

    val horizontalInset = target.size.width * 0.3f
    val verticalInset = target.size.height * 0.3f
    val isOverCore = hovered != null &&
            pointerInGrid.x > target.offset.x + horizontalInset &&
            pointerInGrid.x < target.offset.x + target.size.width - horizontalInset &&
            pointerInGrid.y > target.offset.y + verticalInset &&
            pointerInGrid.y < target.offset.y + target.size.height - verticalInset

    dragState.hoverKey = targetKey
    dragState.hoverMode =
        if (HomeItemKey.isFolder(targetKey) && targetKey != dragState.draggedKey && isOverCore)
            MobileDropMode.INTO
        else
            MobileDropMode.REORDER
}

@Composable
fun Modifier.mobileGridDropFeedback(
    isDragged: Boolean,
    isIntoTarget: Boolean,
    cornerRadiusPx: Float
): Modifier {
    val accent = MaterialTheme.colorScheme.primary
    val contentAlpha by animateFloatAsState(
        targetValue = if (isDragged) 0f else 1f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "home_item_alpha"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isIntoTarget) 1f else 0f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "home_item_border"
    )

    return this
        .graphicsLayer { alpha = contentAlpha }
        .drawWithContent {
            drawContent()
            if (borderAlpha > 0f) {
                drawRoundRect(
                    color = accent.copy(alpha = borderAlpha),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
}

@Composable
fun Modifier.mobileGridFloatingItem(dragState: MobileGridDragState, gridOriginInRoot: Offset): Modifier =
    this
        .graphicsLayer {
            val topLeft = dragState.floatingTopLeftInRoot - gridOriginInRoot
            translationX = topLeft.x
            translationY = topLeft.y
            alpha = 0.9f
            scaleX = 1.04f
            scaleY = 1.04f
            shadowElevation = 18f
        }
