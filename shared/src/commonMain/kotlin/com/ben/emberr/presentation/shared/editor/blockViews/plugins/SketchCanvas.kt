package com.ben.emberr.presentation.shared.editor.blockViews.plugins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke as GraphicsStroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.emberr.domain.model.Point
import com.ben.emberr.domain.model.SketchBlock
import com.ben.emberr.domain.model.Stroke

@Composable
fun SketchCanvasBlockView(
    block: SketchBlock,
    onStrokesChanged: (List<Stroke>) -> Unit,
    inSelectionMode: Boolean,
    onScrollEnabledChange: (Boolean) -> Unit
) {
    var currentStrokes by remember(block.strokes) { mutableStateOf(block.strokes) }
    var activeStroke by remember { mutableStateOf<Stroke?>(null) }

    var currentColor by remember { mutableStateOf("#FF000000") }
    var currentWidth by remember { mutableFloatStateOf(6f) }
    var isEraserMode by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), shape)
            .pointerInput(inSelectionMode) {
                if (inSelectionMode) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes

                        if (event.type == PointerEventType.Scroll) {
                            val scrollDelta = changes.firstOrNull()?.scrollDelta ?: Offset.Zero
                            val isZoom = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed

                            if (isZoom) {
                                val zoomFactor = if (scrollDelta.y > 0) 0.9f else 1.1f
                                scale = (scale * zoomFactor).coerceIn(0.5f, 5f)
                            } else {
                                pan -= Offset(scrollDelta.x * 30f, scrollDelta.y * 30f)
                            }
                            changes.forEach { it.consume() }
                            continue
                        }

                        if (changes.size >= 2) {
                            onScrollEnabledChange(false)
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                            pan += panChange
                            changes.forEach { it.consume() }
                        }
                        else if (changes.size == 1) {
                            val change = changes.first()

                            val isPanDrag = event.buttons.isSecondaryPressed || event.buttons.isTertiaryPressed

                            if (isPanDrag) {
                                onScrollEnabledChange(false)
                                if (change.pressed && change.previousPressed) {
                                    pan += change.positionChange()
                                    change.consume()
                                }
                            } else {
                                val rawX = change.position.x
                                val rawY = change.position.y
                                val canvasX = (rawX - pan.x) / scale
                                val canvasY = (rawY - pan.y) / scale

                                if (change.pressed && !change.previousPressed) {
                                    onScrollEnabledChange(false)
                                    change.consume()

                                    activeStroke = Stroke(
                                        points = listOf(Point(canvasX, canvasY)),
                                        colorHex = currentColor,
                                        strokeWidth = currentWidth / scale,
                                        isEraser = isEraserMode
                                    )
                                } else if (change.pressed && change.previousPressed) {
                                    change.consume()

                                    activeStroke?.let { stroke ->
                                        activeStroke = stroke.copy(
                                            points = stroke.points + Point(canvasX, canvasY)
                                        )
                                    }
                                } else if (!change.pressed && change.previousPressed) {
                                    onScrollEnabledChange(true)
                                    change.consume()

                                    activeStroke?.let {
                                        val newStrokes = currentStrokes + it
                                        currentStrokes = newStrokes
                                        onStrokesChanged(newStrokes)
                                    }
                                    activeStroke = null
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            translate(left = pan.x, top = pan.y) {
                scale(scale = scale, pivot = Offset.Zero) {
                    currentStrokes.forEach { drawComposePath(it) }
                    activeStroke?.let { drawComposePath(it) }
                }
            }
        }

        if (!inSelectionMode) {
            SketchToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                currentColor = currentColor,
                isEraserMode = isEraserMode,
                onColorSelected = { currentColor = it; isEraserMode = false },
                onToggleEraser = { isEraserMode = !isEraserMode },
                onResetView = { scale = 1f; pan = Offset.Zero }
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawComposePath(stroke: Stroke) {
    if (stroke.points.isEmpty()) return

    val path = Path().apply {
        val firstPoint = stroke.points.first()
        moveTo(firstPoint.x, firstPoint.y)
        for (i in 1 until stroke.points.size) {
            val point = stroke.points[i]
            lineTo(point.x, point.y)
        }
    }

    val cleanHex = stroke.colorHex.removePrefix("#")
    val colorLong = if (cleanHex.length == 6) "FF$cleanHex".toLong(16) else cleanHex.toLong(16)

    drawPath(
        path = path,
        color = if (stroke.isEraser) Color.Transparent else Color(colorLong),
        style = GraphicsStroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
    )
}

@Composable
fun SketchToolbar(
    modifier: Modifier = Modifier,
    currentColor: String,
    isEraserMode: Boolean,
    onColorSelected: (String) -> Unit,
    onToggleEraser: () -> Unit,
    onResetView: () -> Unit
) {
    val colors = listOf("#FF000000", "#FF1976D2", "#FFD32F2F", "#FF388E3C") // Black, Blue, Red, Green

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color Palette
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEach { hex ->
                val colorLong = "FF${hex.removePrefix("#")}".toLong(16)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(
                            width = if (!isEraserMode && currentColor == hex) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) }
                )
            }
        }

        // Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )

        // Tools (Icons Only)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Eraser Toggle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onToggleEraser() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (isEraserMode) "✏️" else "🧹", fontSize = 16.sp)
            }

            // Reset View
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onResetView() },
                contentAlignment = Alignment.Center
            ) {
                Text("🔍", fontSize = 16.sp)
            }
        }
    }
}