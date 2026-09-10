package com.emberr.presentation.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.hypot
import kotlin.math.pow

private const val FOLDER_DESIGN_SIZE = 24f
private const val FRONT_PANEL_CLIP_GROWTH = 1.05f
private const val FLAP_HEAD_START = 0.7f
private val FOLDER_STROKE_WIDTH = 1.5.dp
private val FolderTintSpec = tween<Color>(durationMillis = 180, easing = FastOutSlowInEasing)
private val FolderOpacitySpec = tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing)

private data class FolderCorner(val x: Float, val y: Float, val radius: Float)

private data class FolderLine(val startX: Float, val startY: Float, val endX: Float, val endY: Float)

private val FRONT_PANEL_WHEN_CLOSED = listOf(
    FolderCorner(x = 2.0f, y = 2.0f, radius = 3.6f),
    FolderCorner(x = 8.0f, y = 2.0f, radius = 1.2f),
    FolderCorner(x = 13.6f, y = 6.75f, radius = 1.8f),
    FolderCorner(x = 22.0f, y = 6.75f, radius = 4.6f),
    FolderCorner(x = 22.0f, y = 13.0f, radius = 0f),
    FolderCorner(x = 22.0f, y = 17.5f, radius = 0f),
    FolderCorner(x = 22.0f, y = 22.0f, radius = 4.2f),
    FolderCorner(x = 2.0f, y = 22.0f, radius = 4.2f),
    FolderCorner(x = 2.0f, y = 17.5f, radius = 0f),
    FolderCorner(x = 2.0f, y = 13.0f, radius = 0f),
    FolderCorner(x = 2.0f, y = 9.0f, radius = 0f)
)

private val FRONT_PANEL_WHEN_OPEN = listOf(
    FolderCorner(x = 5.0f, y = 10.4f, radius = 2.1f),
    FolderCorner(x = 9.0f, y = 10.4f, radius = 0f),
    FolderCorner(x = 19.0f, y = 10.4f, radius = 2.1f),
    FolderCorner(x = 22.1f, y = 13.9f, radius = 2.7f),
    FolderCorner(x = 21.9f, y = 17.0f, radius = 0f),
    FolderCorner(x = 21.7f, y = 19.4f, radius = 1.4f),
    FolderCorner(x = 18.5f, y = 22.2f, radius = 2.7f),
    FolderCorner(x = 5.5f, y = 22.2f, radius = 2.7f),
    FolderCorner(x = 2.3f, y = 19.4f, radius = 1.4f),
    FolderCorner(x = 2.1f, y = 17.0f, radius = 0f),
    FolderCorner(x = 1.9f, y = 13.9f, radius = 2.7f)
)

private val BACK_WALL_WHEN_CLOSED = FRONT_PANEL_WHEN_CLOSED

private val BACK_WALL_WHEN_OPEN = listOf(
    FolderCorner(x = 2.8f, y = 2.0f, radius = 4.0f),
    FolderCorner(x = 8.8f, y = 2.0f, radius = 1.2f),
    FolderCorner(x = 13.7f, y = 5.6f, radius = 1.8f),
    FolderCorner(x = 21.2f, y = 5.6f, radius = 4.4f),
    FolderCorner(x = 21.2f, y = 11.0f, radius = 0f),
    FolderCorner(x = 21.2f, y = 13.5f, radius = 0f),
    FolderCorner(x = 21.2f, y = 15.8f, radius = 1.0f),
    FolderCorner(x = 2.8f, y = 15.8f, radius = 1.0f),
    FolderCorner(x = 2.8f, y = 13.5f, radius = 0f),
    FolderCorner(x = 2.8f, y = 11.0f, radius = 0f),
    FolderCorner(x = 2.8f, y = 8.0f, radius = 0f)
)

private val PAPER_EDGE_WHEN_CLOSED = FolderLine(startX = 13.0f, startY = 10.0f, endX = 18.0f, endY = 10.0f)
private val PAPER_EDGE_WHEN_OPEN = FolderLine(startX = 9.2f, startY = 17.2f, endX = 14.8f, endY = 17.2f)

@Composable
fun AnimatedFolderIcon(isExpanded: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val openProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "folder_open_progress"
    )
    val strokeColor by animateColorAsState(tint.copy(alpha = 1f), FolderTintSpec, label = "folder_stroke_color")
    val strokeOpacity by animateFloatAsState(tint.alpha, FolderOpacitySpec, label = "folder_stroke_opacity")

    val backWallPath = remember { Path() }
    val frontPanelPath = remember { Path() }
    val frontPanelClipPath = remember { Path() }

    Canvas(modifier = modifier.alpha(strokeOpacity)) {
        val scale = minOf(size.width, size.height) / FOLDER_DESIGN_SIZE
        val flapProgress = openProgress.coerceAtLeast(0f).pow(FLAP_HEAD_START)
        val backWall = blendFolderShape(BACK_WALL_WHEN_CLOSED, BACK_WALL_WHEN_OPEN, openProgress, scale)
        val frontPanel = blendFolderShape(FRONT_PANEL_WHEN_CLOSED, FRONT_PANEL_WHEN_OPEN, flapProgress, scale)

        writeRoundedShape(backWallPath, backWall)
        writeRoundedShape(frontPanelPath, frontPanel)
        writeRoundedShape(frontPanelClipPath, growAroundCenter(frontPanel, FRONT_PANEL_CLIP_GROWTH))

        val strokeWidth = FOLDER_STROKE_WIDTH.toPx()
        val outline = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        clipPath(frontPanelClipPath, clipOp = ClipOp.Difference) {
            drawPath(path = backWallPath, color = strokeColor, style = outline)
        }

        val paperEdge = blendFolderLine(PAPER_EDGE_WHEN_CLOSED, PAPER_EDGE_WHEN_OPEN, flapProgress, scale)
        drawLine(
            color = strokeColor,
            start = Offset(paperEdge.startX, paperEdge.startY),
            end = Offset(paperEdge.endX, paperEdge.endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawPath(path = frontPanelPath, color = strokeColor, style = outline)
    }
}

private fun blendFolderShape(
    closedShape: List<FolderCorner>,
    openShape: List<FolderCorner>,
    openProgress: Float,
    scale: Float
): List<FolderCorner> = List(closedShape.size) { index ->
    val closed = closedShape[index]
    val open = openShape[index]
    FolderCorner(
        x = lerp(closed.x, open.x, openProgress) * scale,
        y = lerp(closed.y, open.y, openProgress) * scale,
        radius = lerp(closed.radius, open.radius, openProgress).coerceAtLeast(0f) * scale
    )
}

private fun blendFolderLine(
    closedLine: FolderLine,
    openLine: FolderLine,
    openProgress: Float,
    scale: Float
): FolderLine = FolderLine(
    startX = lerp(closedLine.startX, openLine.startX, openProgress) * scale,
    startY = lerp(closedLine.startY, openLine.startY, openProgress) * scale,
    endX = lerp(closedLine.endX, openLine.endX, openProgress) * scale,
    endY = lerp(closedLine.endY, openLine.endY, openProgress) * scale
)

private fun growAroundCenter(shape: List<FolderCorner>, growth: Float): List<FolderCorner> {
    val centerX = (shape.minOf { it.x } + shape.maxOf { it.x }) / 2f
    val centerY = (shape.minOf { it.y } + shape.maxOf { it.y }) / 2f
    return shape.map { corner ->
        FolderCorner(
            x = centerX + (corner.x - centerX) * growth,
            y = centerY + (corner.y - centerY) * growth,
            radius = corner.radius * growth
        )
    }
}

private fun writeRoundedShape(path: Path, shape: List<FolderCorner>) {
    path.rewind()
    val trims = fittedCornerTrims(shape)
    shape.forEachIndexed { index, corner ->
        val previous = shape[(index + shape.size - 1) % shape.size]
        val next = shape[(index + 1) % shape.size]
        val entry = travelFrom(corner, previous, trims[index])
        val exit = travelFrom(corner, next, trims[index])
        if (index == 0) path.moveTo(entry.x, entry.y) else path.lineTo(entry.x, entry.y)
        if (trims[index] > 0f) path.quadraticTo(corner.x, corner.y, exit.x, exit.y)
    }
    path.close()
}

private fun fittedCornerTrims(shape: List<FolderCorner>): FloatArray {
    val trims = FloatArray(shape.size) { shape[it].radius }
    shape.indices.forEach { index ->
        val next = (index + 1) % shape.size
        val requested = shape[index].radius + shape[next].radius
        if (requested <= 0f) return@forEach
        val edgeLength = distanceBetween(shape[index], shape[next])
        if (requested <= edgeLength) return@forEach
        val shrink = edgeLength / requested
        trims[index] = minOf(trims[index], shape[index].radius * shrink)
        trims[next] = minOf(trims[next], shape[next].radius * shrink)
    }
    return trims
}

private fun distanceBetween(from: FolderCorner, to: FolderCorner): Float =
    hypot(to.x - from.x, to.y - from.y)

private fun travelFrom(from: FolderCorner, towards: FolderCorner, travel: Float): Offset {
    val deltaX = towards.x - from.x
    val deltaY = towards.y - from.y
    val length = hypot(deltaX, deltaY)
    if (travel <= 0f || length == 0f) return Offset(from.x, from.y)
    val fraction = travel / length
    return Offset(from.x + deltaX * fraction, from.y + deltaY * fraction)
}
