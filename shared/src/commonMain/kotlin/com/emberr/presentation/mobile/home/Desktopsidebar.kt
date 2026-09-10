package com.emberr.presentation.mobile.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.emberr.data.local.room.FolderEntity
import com.emberr.data.local.room.NoteMetadataEntity
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrTextField
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.folder
import emberr.shared.generated.resources.folder_open
import emberr.shared.generated.resources.pen_square
import emberr.shared.generated.resources.star
import org.jetbrains.compose.resources.painterResource

private val INDENT_STEP          = 16.dp
private val SIDEBAR_BASE_START   = 8.dp
private val CHEVRON_SLOT         = 26.dp
private val ROW_ICON_SLOT        = 26.dp
private val ROW_ICON_SIZE        = 24.dp
private val ROW_MIN_HEIGHT       = 42.dp
private val ROW_VERTICAL_PADDING = 2.dp
private val ROW_INNER_PADDING    = 4.dp
private val ROW_ICON_LEADING_GAP = 8.dp
private val ROW_ICON_START       = SIDEBAR_BASE_START + ROW_INNER_PADDING + ROW_ICON_LEADING_GAP
private val GUIDE_COLUMN_START   = ROW_ICON_START + 4.dp
private val GUIDE_WIDTH          = 1.5.dp
private val GUIDE_END_GAP        = 3.dp
private val GUIDE_CORNER         = 6.dp

private val RowColorSpec = tween<Color>(durationMillis = 180, easing = FastOutSlowInEasing)
private val RowFloatSpec = tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing)
private val ChevronSpec  = spring<Float>(stiffness = Spring.StiffnessMediumLow)

data class SidebarClickModifiers(
    val addToSelection: Boolean,
    val extendSelection: Boolean
)

private suspend fun AwaitPointerEventScope.awaitAnyPointerPress(): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        val press = event.changes.firstOrNull()
        if (event.type == PointerEventType.Press && press != null) return press
    }
}

@Composable
fun Modifier.sidebarNoRippleClickable(onClick: () -> Unit): Modifier =
    this.pointerInput(onClick) {
        detectTapGestures(onTap = { onClick() })
    }

@Composable
private fun DesktopContextMenuItem(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = if (isDestructive) 1f else 0.75f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = contentColor)
    }
}

@Composable
private fun DesktopNamePopup(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = "Name..."
) {
    var input by remember(initialValue) { mutableStateOf(initialValue) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 10.dp))
        EmberrTextField(value = input, onValueChange = { input = it }, placeholder = placeholder, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberrButtonSecondary(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
            EmberrButtonPrimary(text = confirmLabel, onClick = { if (input.isNotBlank()) onConfirm(input.trim()) }, modifier = Modifier.weight(1f))
        }
    }
}

// Tree data

fun flattenFolderTree(
    parentId: String?,
    level: Int,
    foldersByParent: Map<String?, List<FolderEntity>>,
    notesByFolder: Map<String?, List<NoteMetadataEntity>>,
    expandedFolderIds: Set<String>,
    sortType: SortType = SortType.LAST_EDITED,
    sortOrder: SortOrder = SortOrder.DESCENDING
): List<HomeItem> {
    val out = mutableListOf<HomeItem>()

    val combined = sortedHomeItems(
        folders = foldersByParent[parentId].orEmpty(),
        notes = notesByFolder[parentId].orEmpty(),
        sortType = sortType,
        sortOrder = sortOrder,
        level = level
    )

    combined.forEach { row ->
        out += row
        if (row is HomeItem.Folder && row.folder.folderId in expandedFolderIds) {
            out += flattenFolderTree(
                row.folder.folderId, level + 1,
                foldersByParent, notesByFolder, expandedFolderIds, sortType, sortOrder
            )
        }
    }
    return out
}

// Row composables

@Composable
fun SidebarFolderRow(
    folder: FolderEntity,
    level: Int,
    guideLines: TreeGuideLines = ROOT_TREE_GUIDE_LINES,
    isExpanded: Boolean,
    isSelected: Boolean,
    dragState: DesktopListDragState,
    menu: TreeSelectionMenu = SINGLE_ITEM_TREE_MENU,
    onClick: (SidebarClickModifiers) -> Unit,
    onToggleFavorite: () -> Unit = {},
    onAddNote: (String) -> Unit,
    onAddSubfolder: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowId = HomeItemKey.forFolder(folder.folderId)
    val currentOnClick by rememberUpdatedState(onClick)

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var showAddNotePopup by remember { mutableStateOf(false) }
    var showAddSubfolderPopup by remember { mutableStateOf(false) }
    var showRenamePopup by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val rowStartPadding = SIDEBAR_BASE_START + INDENT_STEP * level

    val isInsertBefore = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.BEFORE

    val isInsertAfter = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.AFTER

    val isIntoTarget = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.INTO &&
            dragState.payload != "$DRAG_PREFIX_FOLDER${folder.folderId}"

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgTarget: Color = when {
        isIntoTarget -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        isSelected   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        isHovered    -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        else         -> Color.Transparent
    }
    val bgColor by animateColorAsState(bgTarget, RowColorSpec, label = "fbg_${folder.folderId}")

    val borderAlpha by animateFloatAsState(if (isIntoTarget) 1f else 0f, RowFloatSpec, label = "fborder_${folder.folderId}")
    val beforeAlpha by animateFloatAsState(if (isInsertBefore) 1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "fbefore_${folder.folderId}")
    val afterAlpha  by animateFloatAsState(if (isInsertAfter)  1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "fafter_${folder.folderId}")

    val chevronRotation by animateFloatAsState(if (isExpanded) 90f else 0f, ChevronSpec, label = "chevron_${folder.folderId}")

    val shape = RoundedCornerShape(10.dp)

    val guideColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind { drawSidebarGuideLines(level, guideLines, guideColor) }
    ) {
        // Insert line above row
        if (beforeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = beforeAlpha))
                    .scale(scaleX = beforeAlpha, scaleY = 1f)
                    .zIndex(10f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SIDEBAR_BASE_START + INDENT_STEP * level,
                    end = 8.dp,
                    top = ROW_VERTICAL_PADDING,
                    bottom = ROW_VERTICAL_PADDING
                )
                .clip(shape)
                .background(bgColor)
                .then(
                    if (borderAlpha > 0f) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                        shape
                    ) else Modifier
                )
                .hoverable(interactionSource)
                .pointerInput(rowStartPadding) {
                    awaitEachGesture {
                        val press = awaitAnyPointerPress()

                        if (currentEvent.buttons.isSecondaryPressed) {
                            press.consume()
                            contextMenuOffset = with(density) {
                                DpOffset(rowStartPadding + press.position.x.toDp(), ROW_VERTICAL_PADDING + press.position.y.toDp())
                            }
                            showContextMenu = true
                            waitForUpOrCancellation()?.consume()
                            return@awaitEachGesture
                        }

                        if (currentEvent.buttons.isTertiaryPressed) return@awaitEachGesture

                        val pressedModifiers = currentEvent.keyboardModifiers
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        up.consume()
                        currentOnClick(
                            SidebarClickModifiers(
                                addToSelection = pressedModifiers.isCtrlPressed || pressedModifiers.isMetaPressed,
                                extendSelection = pressedModifiers.isShiftPressed
                            )
                        )
                    }
                }
                .heightIn(min = ROW_MIN_HEIGHT)
                .padding(horizontal = ROW_INNER_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(ROW_ICON_LEADING_GAP))
            Box(Modifier.width(ROW_ICON_SLOT), contentAlignment = Alignment.Center) {
                Icon(
                    if (isExpanded) painterResource(Res.drawable.folder_open) else painterResource(Res.drawable.folder),
                    contentDescription = null,
                    tint = if (isIntoTarget) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(ROW_ICON_SIZE - 1.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isIntoTarget) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            when {
                isSelected -> SidebarTrailingCheck()
                isHovered && !dragState.dragging -> {
                    SidebarHoverAction(painterResource(Res.drawable.pen_square), "New note here") { showAddNotePopup = true }
                    Spacer(Modifier.width(4.dp))
                    SidebarHoverAction(Icons.Default.CreateNewFolder, "New subfolder") { showAddSubfolderPopup = true }
                    Spacer(Modifier.width(2.dp))
                }
            }

            Box {
                EmberrDesktopMenu(expanded = showAddNotePopup, onDismissRequest = { showAddNotePopup = false }, modifier = Modifier.width(260.dp)) {
                    DesktopNamePopup(
                        title = "New Note in ${folder.name}",
                        initialValue = "",
                        confirmLabel = "Create",
                        onConfirm = { title -> onAddNote(title); showAddNotePopup = false },
                        onDismiss = { showAddNotePopup = false },
                        placeholder = "Note title..."
                    )
                }
                EmberrDesktopMenu(expanded = showAddSubfolderPopup, onDismissRequest = { showAddSubfolderPopup = false }, modifier = Modifier.width(260.dp)) {
                    DesktopNamePopup(
                        title = "New Subfolder",
                        initialValue = "",
                        confirmLabel = "Create",
                        onConfirm = { name -> onAddSubfolder(name); showAddSubfolderPopup = false },
                        onDismiss = { showAddSubfolderPopup = false }
                    )
                }
                EmberrDesktopMenu(expanded = showRenamePopup, onDismissRequest = { showRenamePopup = false }, modifier = Modifier.width(260.dp)) {
                    DesktopNamePopup(
                        title = "Rename Folder",
                        initialValue = folder.name,
                        confirmLabel = "Save",
                        onConfirm = { name -> onRename(name); showRenamePopup = false },
                        onDismiss = { showRenamePopup = false }
                    )
                }
            }
        }

        // Right-click context menu, anchored at the exact press position
        Box(modifier = Modifier.offset(x = contextMenuOffset.x, y = contextMenuOffset.y)) {
            EmberrDesktopMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.width(200.dp),
                offset = DpOffset.Zero
            ) {
                if (menu.showRename) {
                    DesktopContextMenuItem(Icons.Default.CreateNewFolder, "Add Subfolder") { showContextMenu = false; showAddSubfolderPopup = true }
                    DesktopContextMenuItem(Icons.Default.Edit, "Rename") { showContextMenu = false; showRenamePopup = true }
                }
                DesktopContextMenuItem(Icons.Default.Delete, menu.deleteLabel, isDestructive = true) { showContextMenu = false; onDelete() }
            }
        }

        // Insert line below row (last item)
        if (afterAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = afterAlpha))
                    .scale(scaleX = afterAlpha, scaleY = 1f)
                    .zIndex(10f)
            )
        }
    }
}

@Composable
fun SidebarNoteRow(
    modifier: Modifier = Modifier,
    note: NoteMetadataEntity,
    level: Int,
    guideLines: TreeGuideLines = ROOT_TREE_GUIDE_LINES,
    isActive: Boolean,
    isSelected: Boolean,
    dragState: DesktopListDragState,
    menu: TreeSelectionMenu = SINGLE_ITEM_TREE_MENU,
    onClick: (SidebarClickModifiers) -> Unit,
    onToggleFavorite: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    rowKey: String = HomeItemKey.forNote(note.noteId)
) {
    val currentOnClick by rememberUpdatedState(onClick)

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var showRenamePopup by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val rowStartPadding = SIDEBAR_BASE_START + INDENT_STEP * level

    val isInsertBefore = dragState.dragging &&
            dragState.dropTargetId == rowKey &&
            dragState.dropPosition == DropInsertPosition.BEFORE

    val isInsertAfter = dragState.dragging &&
            dragState.dropTargetId == rowKey &&
            dragState.dropPosition == DropInsertPosition.AFTER

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgTarget: Color = when {
        isActive   -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        isSelected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        isHovered  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        else       -> Color.Transparent
    }
    val bgColor by animateColorAsState(bgTarget, RowColorSpec, label = "nbg_${note.noteId}")

    val beforeAlpha by animateFloatAsState(if (isInsertBefore) 1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "nbefore_${note.noteId}")
    val afterAlpha  by animateFloatAsState(if (isInsertAfter)  1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "nafter_${note.noteId}")

    val guideColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind { drawSidebarGuideLines(level, guideLines, guideColor) }
    ) {
        if (beforeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = beforeAlpha))
                    .scale(scaleX = beforeAlpha, scaleY = 1f)
                    .zIndex(10f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SIDEBAR_BASE_START + INDENT_STEP * level,
                    end = 8.dp,
                    top = ROW_VERTICAL_PADDING,
                    bottom = ROW_VERTICAL_PADDING
                )
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .hoverable(interactionSource)
                .pointerInput(rowStartPadding) {
                    awaitEachGesture {
                        val press = awaitAnyPointerPress()

                        if (currentEvent.buttons.isSecondaryPressed) {
                            press.consume()
                            contextMenuOffset = with(density) {
                                DpOffset(rowStartPadding + press.position.x.toDp(), ROW_VERTICAL_PADDING + press.position.y.toDp())
                            }
                            showContextMenu = true
                            waitForUpOrCancellation()?.consume()
                            return@awaitEachGesture
                        }

                        if (currentEvent.buttons.isTertiaryPressed) return@awaitEachGesture

                        val pressedModifiers = currentEvent.keyboardModifiers
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        up.consume()
                        currentOnClick(
                            SidebarClickModifiers(
                                addToSelection = pressedModifiers.isCtrlPressed || pressedModifiers.isMetaPressed,
                                extendSelection = pressedModifiers.isShiftPressed
                            )
                        )
                    }
                }
                .heightIn(min = ROW_MIN_HEIGHT)
                .padding(horizontal = ROW_INNER_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(ROW_ICON_LEADING_GAP))
            Box(Modifier.width(ROW_ICON_SLOT), contentAlignment = Alignment.Center) {
                if (!note.icon.isNullOrEmpty()) {
                    Text(text = note.icon, fontSize = 18.sp, textAlign = TextAlign.Center)
                } else {
                    Icon(
                        painterResource(Res.drawable.file_text),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier.size(ROW_ICON_SIZE)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = note.title.ifEmpty { "Untitled" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            when {
                isSelected -> SidebarTrailingCheck()
                note.isFavorite -> {
                    Icon(
                        painterResource(Res.drawable.star),
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp).size(14.dp)
                    )
                }
            }

            Box {
                EmberrDesktopMenu(expanded = showRenamePopup, onDismissRequest = { showRenamePopup = false }, modifier = Modifier.width(260.dp)) {
                    DesktopNamePopup(
                        title = "Rename Note",
                        initialValue = note.title,
                        confirmLabel = "Save",
                        onConfirm = { name -> onRename(name); showRenamePopup = false },
                        onDismiss = { showRenamePopup = false }
                    )
                }
            }
        }

        // Right-click context menu, anchored at the exact press position
        Box(modifier = Modifier.offset(x = contextMenuOffset.x, y = contextMenuOffset.y)) {
            EmberrDesktopMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.width(200.dp),
                offset = DpOffset.Zero
            ) {
                if (menu.showRename) {
                    DesktopContextMenuItem(Icons.Default.Edit, "Rename") { showContextMenu = false; showRenamePopup = true }
                }
                if (menu.showFavorite) {
                    DesktopContextMenuItem(Icons.Default.Star, menu.favoriteLabel) { showContextMenu = false; onToggleFavorite() }
                }
                DesktopContextMenuItem(Icons.Default.Delete, menu.deleteLabel, isDestructive = true) { showContextMenu = false; onDelete() }
            }
        }

        if (afterAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = afterAlpha))
                    .scale(scaleX = afterAlpha, scaleY = 1f)
                    .zIndex(10f)
            )
        }
    }
}

private fun DrawScope.drawSidebarGuideLines(level: Int, guideLines: TreeGuideLines, color: Color) {
    if (level == 0) return

    val indentStep = INDENT_STEP.toPx()
    val guideColumnStart = GUIDE_COLUMN_START.toPx()
    val lineWidth = GUIDE_WIDTH.toPx()

    guideLines.ancestorVerticalLines.forEachIndexed { depth, isVisible ->
        if (!isVisible) return@forEachIndexed
        val x = depth * indentStep + guideColumnStart
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
    }

    val elbowX = (level - 1) * indentStep + guideColumnStart
    val middleY = size.height / 2f
    val rowIconStartX = level * indentStep + ROW_ICON_START.toPx() - GUIDE_END_GAP.toPx()
    val cornerRadius = minOf(GUIDE_CORNER.toPx(), middleY, rowIconStartX - elbowX)

    if (!guideLines.isLastChildOfParent) {
        drawLine(
            color = color,
            start = Offset(elbowX, 0f),
            end = Offset(elbowX, size.height),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
    }

    val elbowStartY = if (guideLines.isLastChildOfParent) 0f else middleY - cornerRadius
    val elbow = Path().apply {
        moveTo(elbowX, elbowStartY)
        lineTo(elbowX, middleY - cornerRadius)
        quadraticTo(elbowX, middleY, elbowX + cornerRadius, middleY)
        lineTo(rowIconStartX, middleY)
    }
    drawPath(path = elbow, color = color, style = Stroke(width = lineWidth, cap = StrokeCap.Round))
}

@Composable
fun SidebarSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp)
            .padding(top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .sidebarNoRippleClickable { onToggle() }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle $title",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(start = 2.dp).size(22.dp)
            )
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                content = trailing
            )
        }
    }
}

@Composable
private fun SidebarHoverAction(painter: Painter, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.Transparent)
            .sidebarNoRippleClickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SidebarHoverAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.Transparent)
            .sidebarNoRippleClickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SidebarTrailingCheck() {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(20.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
    }
}