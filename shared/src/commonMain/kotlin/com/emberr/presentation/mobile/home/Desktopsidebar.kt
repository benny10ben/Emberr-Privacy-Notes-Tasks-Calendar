package com.emberr.presentation.mobile.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
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
import emberr.shared.generated.resources.star
import org.jetbrains.compose.resources.painterResource

private val INDENT_STEP          = 16.dp
private val SIDEBAR_BASE_START   = 8.dp
private val CHEVRON_SLOT         = 26.dp
private val ROW_ICON_SLOT        = 24.dp
private val ROW_ICON_SIZE        = 22.dp
private val ROW_MIN_HEIGHT       = 42.dp
private val ROW_VERTICAL_PADDING = 2.dp

private val RowColorSpec = tween<Color>(durationMillis = 180, easing = FastOutSlowInEasing)
private val RowFloatSpec = tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing)
private val ChevronSpec  = spring<Float>(stiffness = Spring.StiffnessMediumLow)

@Composable
fun Modifier.sidebarNoRippleClickable(onClick: () -> Unit): Modifier =
    this.pointerInput(onClick) {
        detectTapGestures(onTap = { onClick() })
    }

@Composable
private fun DesktopContextMenuItem(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .sidebarNoRippleClickable(onClick)
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
    onDismiss: () -> Unit
) {
    var input by remember(initialValue) { mutableStateOf(initialValue) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 10.dp))
        EmberrTextField(value = input, onValueChange = { input = it }, placeholder = "Name...", modifier = Modifier.fillMaxWidth())
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
    isExpanded: Boolean,
    isSelected: Boolean,
    dragState: DesktopListDragState,
    onClick: () -> Unit,
    onAddNote: () -> Unit,
    onAddSubfolder: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowId = HomeItemKey.forFolder(folder.folderId)

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
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

    Box(modifier = modifier.fillMaxWidth()) {
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
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
                .pointerInput(rowStartPadding) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val pressOffset = event.changes.first().position
                                contextMenuOffset = with(density) {
                                    DpOffset(rowStartPadding + pressOffset.x.toDp(), ROW_VERTICAL_PADDING + pressOffset.y.toDp())
                                }
                                showContextMenu = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .heightIn(min = ROW_MIN_HEIGHT)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(ROW_ICON_SLOT), contentAlignment = Alignment.Center) {
                Icon(
                    if (isExpanded) painterResource(Res.drawable.folder_open) else painterResource(Res.drawable.folder),
                    contentDescription = null,
                    tint = if (isIntoTarget) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(ROW_ICON_SIZE)
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
                    SidebarHoverAction(Icons.Default.Add, "New note here", onAddNote)
                    Spacer(Modifier.width(4.dp))
                    SidebarHoverAction(Icons.Default.CreateNewFolder, "New subfolder") { showAddSubfolderPopup = true }
                    Spacer(Modifier.width(2.dp))
                }
            }

            Box {
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
                // For SidebarFolderRow:
                DesktopContextMenuItem(Icons.Default.CreateNewFolder, "Add Subfolder") { showContextMenu = false; showAddSubfolderPopup = true }
                DesktopContextMenuItem(Icons.Default.Edit, "Rename") { showContextMenu = false; showRenamePopup = true }
                DesktopContextMenuItem(Icons.Default.Delete, "Delete", isDestructive = true) { showContextMenu = false; onDelete() }

                // Note: For SidebarNoteRow, just omit the "Add Subfolder" menu item inside this block as you had it originally.
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
    isActive: Boolean,
    isSelected: Boolean,
    dragState: DesktopListDragState,
    onClick: () -> Unit,
    onRename: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    rowKey: String = HomeItemKey.forNote(note.noteId)
) {
    val rowId = HomeItemKey.forNote(note.noteId)

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var showRenamePopup by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val rowStartPadding = SIDEBAR_BASE_START + INDENT_STEP * level

    val isInsertBefore = dragState.dragging &&
            dragState.dropTargetId == rowId &&
            dragState.dropPosition == DropInsertPosition.BEFORE

    val isInsertAfter = dragState.dragging &&
            dragState.dropTargetId == rowId &&
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

    Box(modifier = modifier.fillMaxWidth()) {
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
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
                .pointerInput(rowStartPadding) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val pressOffset = event.changes.first().position
                                contextMenuOffset = with(density) {
                                    DpOffset(rowStartPadding + pressOffset.x.toDp(), ROW_VERTICAL_PADDING + pressOffset.y.toDp())
                                }
                                showContextMenu = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .heightIn(min = ROW_MIN_HEIGHT)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(ROW_ICON_SLOT), contentAlignment = Alignment.Center) {
                if (!note.icon.isNullOrEmpty()) {
                    Text(text = note.icon, fontSize = 18.sp, textAlign = TextAlign.Center)
                } else {
                    Icon(
                        painterResource(Res.drawable.file_text),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
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
        EmberrDesktopMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.width(200.dp),
            offset = contextMenuOffset
        ) {
            DesktopContextMenuItem(Icons.Default.Edit, "Rename") { showContextMenu = false; showRenamePopup = true }
            DesktopContextMenuItem(Icons.Default.Delete, "Delete", isDestructive = true) { showContextMenu = false; onDelete() }
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