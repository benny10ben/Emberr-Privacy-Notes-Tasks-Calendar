package com.ben.emberr.presentation.mobile.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ben.emberr.data.local.room.FolderEntity

private val FolderCardShape = RoundedCornerShape(16.dp)
private val FolderTabHeight = 24.dp
private val FolderTabCornerRadius = 8.dp
private val FolderTabSlopeWidth = 20.dp
private const val FOLDER_TAB_WIDTH_FRACTION = 0.62f
private const val FOLDER_FRONT_HEIGHT_FRACTION = 0.70f
private const val CIRCLE_CONTROL_POINT_RATIO = 0.5523f

private val FolderBorderWidth = 4.dp
private val FolderSelectedBorderWidth = 2.dp

private class FolderFrontShape(
    private val tabHeight: Dp,
    private val tabCornerRadius: Dp,
    private val slopeWidth: Dp,
    private val tabWidthFraction: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val tab = with(density) { tabHeight.toPx() }
        val corner = with(density) { tabCornerRadius.toPx() }
        val slope = with(density) { slopeWidth.toPx() }
        val tabEnd = (size.width * tabWidthFraction).coerceAtMost(size.width - slope)

        val path = Path().apply {
            moveTo(0f, corner)
            cubicTo(
                0f, corner - corner * CIRCLE_CONTROL_POINT_RATIO,
                corner - corner * CIRCLE_CONTROL_POINT_RATIO, 0f,
                corner, 0f
            )
            lineTo(tabEnd, 0f)
            cubicTo(
                tabEnd + slope * 0.6f, 0f,
                tabEnd + slope * 0.4f, tab,
                tabEnd + slope, tab
            )
            lineTo(size.width, tab)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.folderCardGestures(
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier = if (!enabled) this else this.combinedClickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
    onLongClick = onLongClick
)

@Composable
fun FolderCard(
    folder: FolderEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    handlesGestures: Boolean = true,
    noteCount: Int = 0,
    itemCount: Int? = null,
    caption: String? = null
) {
    val bodyColor = MaterialTheme.colorScheme.surface
    val frontShape = remember {
        FolderFrontShape(
            tabHeight = FolderTabHeight,
            tabCornerRadius = FolderTabCornerRadius,
            slopeWidth = FolderTabSlopeWidth,
            tabWidthFraction = FOLDER_TAB_WIDTH_FRACTION
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .folderCardGestures(handlesGestures, onClick, onLongClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(FolderCardShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .fillMaxHeight(FOLDER_FRONT_HEIGHT_FRACTION)
                    .clip(frontShape)
                    .background(bodyColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        folder.name.ifEmpty { "Untitled" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Notes: $noteCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (itemCount != null || caption != null) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (itemCount != null) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        itemCount.toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(0.dp))
                            }
                            if (caption != null) {
                                Text(
                                    caption,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = if (isSelected) FolderSelectedBorderWidth else FolderBorderWidth,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = FolderCardShape
                )
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}