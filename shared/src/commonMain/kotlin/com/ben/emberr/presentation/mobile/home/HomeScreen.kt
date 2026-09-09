package com.ben.emberr.presentation.mobile.home

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.ben.emberr.presentation.shared.rememberStableStatusBarsPadding
import com.ben.emberr.presentation.shared.stableStatusBarsPadding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ben.emberr.data.local.prefs.SyncConstants
import com.ben.emberr.data.local.room.FolderEntity
import com.ben.emberr.data.local.room.NoteMetadataEntity
import com.ben.emberr.domain.model.NoteContent
import com.ben.emberr.domain.util.WidgetComposeRequest
import com.ben.emberr.domain.util.WidgetComposeRequestBus
import com.ben.emberr.domain.util.isDesktopPlatform
import com.ben.emberr.presentation.mobile.daily.DailyEditorViewModel
import com.ben.emberr.presentation.mobile.daily.TaskDaySection
import com.ben.emberr.presentation.shared.UserSettings
import com.ben.emberr.presentation.shared.components.EmberrBlur
import com.ben.emberr.presentation.shared.components.EmberrBottomSheet
import com.ben.emberr.presentation.shared.components.SelectedOptionBackground
import com.ben.emberr.presentation.shared.components.EmberrBottomSheetAction
import com.ben.emberr.presentation.shared.components.EmberrDesktopMenu
import com.ben.emberr.presentation.shared.components.KmpBackHandler
import com.ben.emberr.presentation.shared.components.TopBarIconButtonGroup
import com.ben.emberr.presentation.shared.components.TopBarIconButtonItem
import com.ben.emberr.presentation.shared.components.smoothWheelScroll
import com.ben.emberr.presentation.sync.SyncViewModel
import com.ben.emberr.domain.util.showNativeToast
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.isActive
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.ben.emberr.presentation.shared.components.EmberrButtonPrimary
import com.ben.emberr.presentation.shared.components.EmberrButtonSecondary
import com.ben.emberr.presentation.shared.components.EmberrTextField
import com.ben.emberr.presentation.shared.components.customEmberrShadow
import com.ben.emberr.presentation.shared.components.emberrBlur
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_up_down
import emberr.shared.generated.resources.calendar
import emberr.shared.generated.resources.circle_plus
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.pen_square
import emberr.shared.generated.resources.folder_plus
import emberr.shared.generated.resources.template
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.painterResource

private val HORIZONTAL_PADDING = 16.dp
private val DefaultCornerShape = RoundedCornerShape(12.dp)

@Composable
private fun SectionToggleIcon(isExpanded: Boolean, contentDescription: String) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sectionToggleRotation"
    )
    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = contentDescription,
        modifier = Modifier.padding(start = 4.dp).size(20.dp).graphicsLayer { rotationZ = rotation },
        tint = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.cardGestures(
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
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onSelectionModeChange: (Boolean) -> Unit = {},
    onNavigateToEditor: (String) -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToReminders: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToImages: () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    onNavigateToTrash: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onToggleSidebar: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    syncViewModel: SyncViewModel = koinViewModel(),
    dailyEditorViewModel: DailyEditorViewModel = koinViewModel(),
) {
    val hazeState = remember { HazeState() }

    var showScheduledTasksSheet by remember { mutableStateOf(false) }
    val calendarTaskMap by dailyEditorViewModel.calendarTaskMap.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val subFolders by viewModel.currentSubFolders.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsState()
    val favoriteNotes by viewModel.favoriteNotes.collectAsState()

    val noteCountsByFolder by viewModel.noteCountsByFolder.collectAsState()

    val remindersCount by viewModel.remindersCount.collectAsState()
    val bookmarksCount by viewModel.bookmarksCount.collectAsState()
    val imagesCount by viewModel.imagesCount.collectAsState()
    val documentsCount by viewModel.documentsCount.collectAsState()

    val currentSortType by viewModel.sortType.collectAsState()
    val currentSortOrder by viewModel.sortOrder.collectAsState()

    val gridItems: List<HomeItem> = remember(subFolders, notes, currentSortType, currentSortOrder) {
        sortedHomeItems(subFolders, notes, currentSortType, currentSortOrder)
    }

    val gridState = rememberLazyStaggeredGridState()
    val gridDragState = rememberMobileGridDragState()
    val cardCornerRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
    var gridOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    // While a drag is in flight the grid renders this optimistic order, so cards slide out of
    // the way under the finger
    var previewItems by remember { mutableStateOf<List<HomeItem>?>(null) }
    val displayedItems = previewItems ?: gridItems

    LaunchedEffect(gridItems) { if (!gridDragState.isDragging) previewItems = null }

    LaunchedEffect(gridDragState.draggedKey) {
        if (gridDragState.isDragging && previewItems == null) previewItems = gridItems
    }

    val edgeScrollZonePx = with(LocalDensity.current) { 110.dp.toPx() }
    val edgeScrollStepPx = with(LocalDensity.current) { 14.dp.toPx() }

    LaunchedEffect(selectedFolderId) { gridState.scrollToItem(0) }

    LaunchedEffect(gridDragState.isDragging) {
        if (!gridDragState.isDragging) return@LaunchedEffect
        while (isActive) {
            withFrameNanos { }
            val delta = gridDragState.edgeScrollDelta(gridState, edgeScrollZonePx, edgeScrollStepPx)
            if (delta != 0f) {
                gridState.scrollBy(delta)
                gridDragState.refreshHoverTarget(gridState)
            }
        }
    }

    LaunchedEffect(gridDragState.hoverKey, gridDragState.hoverMode) {
        val draggedKey = gridDragState.draggedKey ?: return@LaunchedEffect
        val hoverKey = gridDragState.hoverKey ?: return@LaunchedEffect
        if (gridDragState.hoverMode != MobileDropMode.REORDER) return@LaunchedEffect
        previewItems = (previewItems ?: gridItems).movedTo(draggedKey, hoverKey)
    }

    val handleGridDrop: (String, String?, MobileDropMode) -> Unit = { draggedKey, hoverKey, mode ->
        if (mode == MobileDropMode.INTO && hoverKey != null && HomeItemKey.isFolder(hoverKey)) {
            previewItems = null
            val destinationFolderId = HomeItemKey.folderIdOf(hoverKey)
            when {
                HomeItemKey.isNote(draggedKey) ->
                    viewModel.moveNote(HomeItemKey.noteIdOf(draggedKey), destinationFolderId)

                HomeItemKey.isFolder(draggedKey) ->
                    viewModel.moveFolder(HomeItemKey.folderIdOf(draggedKey), destinationFolderId)
            }
        } else {
            viewModel.applyManualOrder(displayedItems.map { it.key })
        }
    }

    val templates by viewModel.filteredTemplates.collectAsState()
    val templateSearchQuery by viewModel.templateSearchQuery.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }

    var showAddNotePopup by remember { mutableStateOf(false) }
    var showAddFolderPopup by remember { mutableStateOf(false) }
    var addNoteInput by remember { mutableStateOf("") }
    var addFolderInput by remember { mutableStateOf("") }

    // Mobile sheet + desktop popup toggles for the Templates menu opened from the New Note flow.
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showTemplatesMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (WidgetComposeRequestBus.consume(WidgetComposeRequest.NEW_NOTE)) {
            if (isDesktopPlatform) {
                addNoteInput = ""
                showAddNotePopup = true
            } else {
                showAddNoteDialog = true
            }
        }
    }

    val isFavoritesExpanded by viewModel.isFavoritesSectionExpanded.collectAsState()
    val isNotesExpanded by viewModel.isNotesSectionExpanded.collectAsState()
    val isRecentsExpanded by viewModel.isRecentsSectionExpanded.collectAsState()

    val favListState = rememberLazyListState()
    val recentListState = rememberLazyListState()

    val isSelectionMode = selectedNoteIds.isNotEmpty() || selectedFolderIds.isNotEmpty()

    val syncState by syncViewModel.syncStatus.collectAsState()

    LaunchedEffect(syncState) {
        if (syncState != "Idle" && syncState != "Syncing...") {
            showNativeToast(syncState)
            syncViewModel.resetSyncStatus()
        }
    }

    KmpBackHandler(enabled = isSelectionMode) { viewModel.clearSelection() }
    KmpBackHandler(enabled = selectedFolderId != null) { viewModel.navigateUp() }

    LaunchedEffect(isSelectionMode) { onSelectionModeChange(isSelectionMode) }

    val handleCreateFolder = { name: String ->
        viewModel.createNewFolder(name)
        showAddFolderDialog = false
    }

    val handleCreateNote = { title: String ->
        viewModel.createNewNote(title = title, forceHomeFolder = false) { newNoteId ->
            onNavigateToEditor(newNoteId)
        }
        showAddNoteDialog = false
    }

    // Re-seeds any missing predefined template every time either Templates entry point opens.
    // Also closes the mobile New Note sheet it's invoked from - closeAnd() only runs the hide
    // animation, it doesn't flip showAddNoteDialog itself (see AddNoteBottomSheet's onCreate,
    // which does that inline), so this has to be the one to reset it, or the sheet is left
    // mounted (hidden but expanded = true) after the templates sheet opens on top of it.
    val handleOpenTemplates = {
        viewModel.onTemplatesMenuOpened()
        showAddNoteDialog = false
        if (isDesktopPlatform) showTemplatesMenu = true else showTemplatesSheet = true
    }
    val handleTemplateClick = { templateId: String ->
        viewModel.createNoteFromTemplate(templateId) { newNoteId -> onNavigateToEditor(newNoteId) }
    }
    // Opens the template's own note directly - unlike handleTemplateClick, this does NOT clone
    // it into a new note. The editor already renders the "Editing Template" pill for any note
    // with isTemplate = true, so no separate "template edit mode" is needed here.
    val handleEditTemplate = { templateId: String -> onNavigateToEditor(templateId) }
    val handleCreateNewTemplate = {
        viewModel.saveAsTemplate(title = "", content = NoteContent(blocks = emptyList())) { newTemplateId ->
            onNavigateToEditor(newTemplateId)
        }
    }

    val homeGridContent = @Composable {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { gridOriginInRoot = it.positionInRoot() }
        ) {
            val cardWidth = (maxWidth - (HORIZONTAL_PADDING * 2) - 10.dp) / 2

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        top = (if (isDesktopPlatform) 64.dp else 76.dp) + rememberStableStatusBarsPadding().calculateTopPadding(),
                        bottom = bottomContentPadding + 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                    verticalItemSpacing = 10.dp,
                    modifier = Modifier.fillMaxSize().hazeSource(state = hazeState).background(MaterialTheme.colorScheme.background)
                ) {
                    item {
                        Box(Modifier.padding(start = HORIZONTAL_PADDING)) {
                            OverviewCard("Tasks", "$remindersCount left", onClick = { onNavigateToReminders() })
                        }
                    }
                    item {
                        Box(Modifier.padding(end = HORIZONTAL_PADDING)) {
                            OverviewCard("Bookmarks", "$bookmarksCount saved", onClick = { onNavigateToBookmarks() })
                        }
                    }
                    item {
                        Box(Modifier.padding(start = HORIZONTAL_PADDING)) {
                            OverviewCard("Images", "$imagesCount saved", onClick = { onNavigateToImages() })
                        }
                    }
                    item {
                        Box(Modifier.padding(end = HORIZONTAL_PADDING)) {
                            OverviewCard(
                                "Documents",
                                "$documentsCount attached",
                                onClick = { onNavigateToDocuments() })
                        }
                    }

                    if (favoriteNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(
                                    start = HORIZONTAL_PADDING,
                                    end = HORIZONTAL_PADDING,
                                    top = 14.dp,
                                    bottom = 8.dp
                                ), verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                        .noRippleClickable {
                                            viewModel.toggleHomeSection(SyncConstants.HOME_SECTION_FAVORITES)
                                        }.padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Favorites",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    SectionToggleIcon(isFavoritesExpanded, "Toggle Favorites")
                                }
                            }
                        }
                        if (isFavoritesExpanded) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LazyRow(
                                    state = favListState,
                                    modifier = Modifier.fillMaxWidth()
                                        .smoothWheelScroll(favListState, horizontal = true),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING)
                                ) {
                                    items(favoriteNotes, key = { "fav_${it.noteId}" }) { note ->
                                        Box(Modifier.width(cardWidth)) {
                                            NoteCard(
                                                note = note,
                                                isSelected = selectedNoteIds.contains(note.noteId),
                                                onClick = {
                                                    if (isSelectionMode) viewModel.toggleNoteSelection(
                                                        note.noteId
                                                    ) else onNavigateToEditor(note.noteId)
                                                },
                                                onLongClick = { viewModel.toggleNoteSelection(note.noteId) })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (gridItems.isNotEmpty() || !isSelectionMode) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).noRippleClickable { viewModel.toggleHomeSection(SyncConstants.HOME_SECTION_NOTES) }.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Notes",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    SectionToggleIcon(isNotesExpanded, "Toggle Notes")
                                }
                                if (!isSelectionMode) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box {
                                            Icon(
                                                painterResource(Res.drawable.arrow_up_down),
                                                "Sort",
                                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                                    .noRippleClickable { showSortMenu = true },
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isDesktopPlatform) {
                                                EmberrDesktopMenu(
                                                    expanded = showSortMenu,
                                                    onDismissRequest = { showSortMenu = false }) {
                                                    DesktopSortMenu(
                                                        currentSortType = currentSortType,
                                                        currentSortOrder = currentSortOrder,
                                                        onDismiss = { showSortMenu = false },
                                                        onSortChanged = { type, order ->
                                                            viewModel.updateSort(
                                                                type,
                                                                order
                                                            ); showSortMenu = false
                                                        })
                                                }
                                            }
                                        }
                                        Box {
                                            Icon(
                                                painterResource(Res.drawable.folder_plus),
                                                "New Folder",
                                                modifier = Modifier.size(20.dp)
                                                    .noRippleClickable {
                                                        if (isDesktopPlatform) {
                                                            addFolderInput = ""; showAddFolderPopup = true
                                                        } else showAddFolderDialog = true
                                                    },
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isDesktopPlatform) {
                                                EmberrDesktopMenu(
                                                    expanded = showAddFolderPopup,
                                                    onDismissRequest = { showAddFolderPopup = false },
                                                    modifier = Modifier.width(280.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(
                                                            horizontal = 16.dp,
                                                            vertical = 12.dp
                                                        )
                                                    ) {
                                                        Text(
                                                            "New Folder",
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(bottom = 10.dp)
                                                        )
                                                        EmberrTextField(
                                                            value = addFolderInput,
                                                            onValueChange = { addFolderInput = it },
                                                            placeholder = "e.g. Personal, Work...",
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            EmberrButtonSecondary(
                                                                text = "Cancel",
                                                                onClick = { showAddFolderPopup = false },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            EmberrButtonPrimary(
                                                                text = "Create",
                                                                onClick = {
                                                                    if (addFolderInput.isNotBlank()) {
                                                                        handleCreateFolder(addFolderInput.trim())
                                                                        showAddFolderPopup = false
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Box {
                                            Icon(painterResource(Res.drawable.pen_square), "New Note", modifier = Modifier.size(22.dp).noRippleClickable { if (isDesktopPlatform) { addNoteInput = ""; showAddNotePopup = true } else showAddNoteDialog = true }, tint = MaterialTheme.colorScheme.onSurface)
                                            if (isDesktopPlatform) {
                                                EmberrDesktopMenu(expanded = showAddNotePopup, onDismissRequest = { showAddNotePopup = false }, modifier = Modifier.width(280.dp)) {
                                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth()
                                                                .padding(bottom = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                "New Note",
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Icon(
                                                                painter = painterResource(Res.drawable.template),
                                                                contentDescription = "Templates",
                                                                tint = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.size(24.dp)
                                                                    .noRippleClickable {
                                                                        showAddNotePopup = false
                                                                        handleOpenTemplates()
                                                                    }
                                                            )
                                                        }
                                                        EmberrTextField(
                                                            value = addNoteInput,
                                                            onValueChange = { addNoteInput = it },
                                                            placeholder = "Note title...",
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                8.dp
                                                            )
                                                        ) {
                                                            EmberrButtonSecondary(
                                                                text = "Cancel",
                                                                onClick = {
                                                                    showAddNotePopup = false
                                                                },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            EmberrButtonPrimary(
                                                                text = "Create",
                                                                onClick = {
                                                                    if (addNoteInput.isNotBlank()) {
                                                                        handleCreateNote(
                                                                            addNoteInput.trim()
                                                                        ); showAddNotePopup = false
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            TemplatesDesktopMenu(
                                                expanded = showTemplatesMenu,
                                                templates = templates,
                                                searchQuery = templateSearchQuery,
                                                onSearchQueryChange = { viewModel.updateTemplateSearchQuery(it) },
                                                onDismissRequest = { showTemplatesMenu = false },
                                                onTemplateClick = { id -> showTemplatesMenu = false; handleTemplateClick(id) },
                                                onEditTemplate = { id -> showTemplatesMenu = false; handleEditTemplate(id) },
                                                onDeleteTemplate = { id -> viewModel.deleteTemplate(id) },
                                                onCreateNewTemplate = { showTemplatesMenu = false; handleCreateNewTemplate() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isNotesExpanded) {
                        if (!isDesktopPlatform && !isSelectionMode && selectedFolderId != null) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                BreadcrumbTrail(
                                    selectedFolderId = selectedFolderId,
                                    breadcrumbs = breadcrumbs,
                                    onNavigate = { viewModel.selectFolder(it) },
                                    modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING)
                                )
                            }
                        }

                        if (displayedItems.isEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine, key = "home_empty_state") {
                                HomeEmptyState()
                            }
                        }

                        itemsIndexed(displayedItems, key = { _, row -> row.key }) { index, row ->
                            val sidePad = if (index % 2 == 0) Modifier.padding(start = HORIZONTAL_PADDING) else Modifier.padding(end = HORIZONTAL_PADDING)
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (gridDragState.isDragging) Modifier.animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = null,
                                            placementSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            )
                                        ) else Modifier
                                    )
                                    .then(sidePad)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .mobileGridDropFeedback(
                                            isDragged = gridDragState.isDragged(row.key),
                                            isIntoTarget = gridDragState.isIntoTarget(row.key),
                                            cornerRadiusPx = cardCornerRadiusPx
                                        )
                                        .mobileGridDragSource(
                                            itemKey = row.key,
                                            dragState = gridDragState,
                                            gridState = gridState,
                                            gridOriginInRoot = gridOriginInRoot,
                                            dragEnabled = !isSelectionMode,
                                            onClick = {
                                                when (row) {
                                                    is HomeItem.Folder ->
                                                        if (isSelectionMode) viewModel.toggleFolderSelection(row.folder.folderId)
                                                        else viewModel.selectFolder(row.folder.folderId)

                                                    is HomeItem.Note ->
                                                        if (isSelectionMode) viewModel.toggleNoteSelection(row.note.noteId)
                                                        else onNavigateToEditor(row.note.noteId)
                                                }
                                            },
                                            onLongPress = {
                                                when (row) {
                                                    is HomeItem.Folder -> viewModel.toggleFolderSelection(row.folder.folderId)
                                                    is HomeItem.Note -> viewModel.toggleNoteSelection(row.note.noteId)
                                                }
                                            },
                                            onDrop = { hoverKey, mode ->
                                                handleGridDrop(row.key, hoverKey, mode)
                                            }
                                        )
                                ) {
                                    when (row) {
                                        is HomeItem.Folder -> FolderCard(
                                            folder = row.folder,
                                            isSelected = selectedFolderIds.contains(row.folder.folderId),
                                            noteCount = noteCountsByFolder[row.folder.folderId] ?: 0,
                                            handlesGestures = false,
                                            onClick = {},
                                            onLongClick = {})

                                        is HomeItem.Note -> NoteCard(
                                            note = row.note,
                                            isSelected = selectedNoteIds.contains(row.note.noteId),
                                            handlesGestures = false,
                                            onClick = {},
                                            onLongClick = {})
                                    }
                                }
                            }
                        }
                    }

                    if (recentNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                        .noRippleClickable {
                                            viewModel.toggleHomeSection(SyncConstants.HOME_SECTION_RECENTS)
                                        }.padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Recents",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    SectionToggleIcon(isRecentsExpanded, "Toggle Recents")
                                }
                            }
                        }
                        if (isRecentsExpanded) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                LazyRow(
                                    state = recentListState,
                                    modifier = Modifier.fillMaxWidth()
                                        .smoothWheelScroll(recentListState, horizontal = true),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING)
                                ) {
                                    items(recentNotes, key = { "recent_${it.noteId}" }) { note ->
                                        Box(Modifier.width(cardWidth)) {
                                            NoteCard(
                                                note = note,
                                                isSelected = selectedNoteIds.contains(note.noteId),
                                                onClick = {
                                                    if (isSelectionMode) viewModel.toggleNoteSelection(
                                                        note.noteId
                                                    ) else onNavigateToEditor(note.noteId)
                                                },
                                                onLongClick = { viewModel.toggleNoteSelection(note.noteId) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val floatingItem = gridDragState.draggedKey?.let { key ->
                displayedItems.firstOrNull { it.key == key }
            }
            if (floatingItem != null && gridDragState.floatingSize.width > 0) {
                val density = LocalDensity.current
                Box(
                    modifier = Modifier
                        .size(
                            width = with(density) { gridDragState.floatingSize.width.toDp() },
                            height = with(density) { gridDragState.floatingSize.height.toDp() }
                        )
                        .mobileGridFloatingItem(gridDragState, gridOriginInRoot)
                ) {
                    when (floatingItem) {
                        is HomeItem.Folder -> FolderCard(
                            folder = floatingItem.folder,
                            isSelected = false,
                            noteCount = noteCountsByFolder[floatingItem.folder.folderId] ?: 0,
                            handlesGestures = false,
                            onClick = {},
                            onLongClick = {})

                        is HomeItem.Note -> NoteCard(
                            note = floatingItem.note,
                            isSelected = false,
                            handlesGestures = false,
                            onClick = {},
                            onLongClick = {})
                    }
                }
            }

            HomeTopBar(
                isSelectionMode = isSelectionMode,
                onToggleSidebar = onToggleSidebar,
                hazeState = hazeState,
                onNavigateToCalendar = onNavigateToCalendar,
                onOpenScheduledTasks = { showScheduledTasksSheet = true },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToTrash = onNavigateToTrash,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Scaffold
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {

            homeGridContent()

            NotesSelectionPill(
                isVisible = isSelectionMode,
                selectedCount = selectedNoteIds.size + selectedFolderIds.size,
                onClearSelection = { viewModel.clearSelection() },
                onDelete = { viewModel.deleteSelectedItems() },
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState
            )

            if (!isDesktopPlatform) {
                AddFolderBottomSheet(
                    expanded = showAddFolderDialog,
                    onDismiss = { showAddFolderDialog = false },
                    onCreate = handleCreateFolder
                )
                AddNoteBottomSheet(
                    expanded = showAddNoteDialog,
                    onDismiss = { showAddNoteDialog = false },
                    onCreate = handleCreateNote,
                    onOpenTemplates = handleOpenTemplates
                )
                SortBottomSheet(
                    expanded = showSortMenu,
                    currentSortType = currentSortType,
                    currentSortOrder = currentSortOrder,
                    onDismiss = { showSortMenu = false },
                    onSortChanged = { type, order ->
                        viewModel.updateSort(
                            type,
                            order
                        ); showSortMenu = false
                    })
                TemplatesBottomSheet(
                    expanded = showTemplatesSheet,
                    templates = templates,
                    searchQuery = templateSearchQuery,
                    onSearchQueryChange = { viewModel.updateTemplateSearchQuery(it) },
                    onDismiss = { showTemplatesSheet = false },
                    onTemplateClick = handleTemplateClick,
                    onEditTemplate = handleEditTemplate,
                    onDeleteTemplate = { id -> viewModel.deleteTemplate(id) },
                    onCreateNewTemplate = handleCreateNewTemplate
                )
            }


            if (showScheduledTasksSheet) {
                val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
                val todayTasks = calendarTaskMap[today] ?: emptyList()
                val tomorrowTasks = calendarTaskMap[today.plus(1, DateTimeUnit.DAY)] ?: emptyList()

                EmberrBottomSheet(
                    expanded = true,
                    onDismiss = { showScheduledTasksSheet = false },
                    title = "Upcoming Tasks",
                ) { _ ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (todayTasks.isEmpty() && tomorrowTasks.isEmpty()) {
                            Text(
                                "No tasks scheduled for today or tomorrow.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            val onTaskNoteLinkClick: (String) -> Unit = { noteId ->
                                showScheduledTasksSheet = false
                                onNavigateToEditor(noteId)
                            }

                            if (todayTasks.isNotEmpty()) {
                                TaskDaySection(
                                    "Today",
                                    todayTasks,
                                    dailyEditorViewModel,
                                    onTaskNoteLinkClick
                                )
                            }

                            if (tomorrowTasks.isNotEmpty()) {
                                TaskDaySection(
                                    "Tomorrow",
                                    tomorrowTasks,
                                    dailyEditorViewModel,
                                    onTaskNoteLinkClick
                                )
                            }
                        }

                        EmberrButtonPrimary(
                            text = "Close",
                            onClick = { showScheduledTasksSheet = false },
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    isSelectionMode: Boolean,
    onToggleSidebar: () -> Unit,
    hazeState: HazeState,
    onNavigateToCalendar: () -> Unit,
    onOpenScheduledTasks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNotesMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures {} }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .stableStatusBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                    top = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!isSelectionMode) {
                    if (isDesktopPlatform) {
                        IconButton(
                            onClick = onToggleSidebar,
                            modifier = Modifier.offset(x = (-8).dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        "Home",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Box {
                TopBarIconButtonGroup(
                    bgColor = Color.Transparent,
                    tint = MaterialTheme.colorScheme.primary,
                    hazeState = hazeState,
                    hazeStyle = EmberrBlur.Regular,
                    items = listOf(
                        TopBarIconButtonItem(
                            icon = painterResource(Res.drawable.calendar),
                            contentDescription = "Open Calendar",
                            onClick = onNavigateToCalendar
                        ),
//                        TopBarIconButtonItem(
//                            icon = painterResource(Res.drawable.inbox),
//                            contentDescription = "Notifications",
//                            onClick = onOpenScheduledTasks
//                        ),
                        TopBarIconButtonItem(
                            icon = painterResource(Res.drawable.ellipsis),
                            contentDescription = "Settings",
                            onClick = { showNotesMenu = true }
                        )
                    )
                )

                UserSettings(
                    expanded = showNotesMenu, onDismiss = { showNotesMenu = false },
                    onNavigateToSettings = {
                        onNavigateToSettings(); showNotesMenu = false
                    },
                    onNavigateToTrash = { onNavigateToTrash(); showNotesMenu = false }
                )
            }
        }
    }
}

@Composable
fun DesktopSortMenu(currentSortType: SortType, currentSortOrder: SortOrder, onDismiss: () -> Unit, onSortChanged: (SortType, SortOrder) -> Unit) {
    Column(modifier = Modifier.width(200.dp).padding(vertical = 4.dp)) {
        DesktopSortOptionItem(
            "Last Edited",
            currentSortType == SortType.LAST_EDITED
        ) { onDismiss(); onSortChanged(SortType.LAST_EDITED, currentSortOrder) }
        DesktopSortOptionItem(
            "Date Created",
            currentSortType == SortType.DATE_CREATED
        ) { onDismiss(); onSortChanged(SortType.DATE_CREATED, currentSortOrder) }
        DesktopSortOptionItem(
            "Name (A-Z)",
            currentSortType == SortType.NAME
        ) { onDismiss(); onSortChanged(SortType.NAME, currentSortOrder) }
        DesktopSortOptionItem(
            "Type",
            currentSortType == SortType.TYPE
        ) { onDismiss(); onSortChanged(SortType.TYPE, currentSortOrder) }
        DesktopSortOptionItem("Manual", currentSortType == SortType.MANUAL) {
            onDismiss(); onSortChanged(SortType.MANUAL, currentSortOrder)
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
        DesktopSortOptionItem(
            "Ascending",
            currentSortOrder == SortOrder.ASCENDING
        ) { onDismiss(); onSortChanged(currentSortType, SortOrder.ASCENDING) }
        DesktopSortOptionItem(
            "Descending",
            currentSortOrder == SortOrder.DESCENDING
        ) { onDismiss(); onSortChanged(currentSortType, SortOrder.DESCENDING) }
    }
}

@Composable
private fun DesktopSortOptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SelectedOptionBackground else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    val mutedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(Res.drawable.file_text),
            null,
            modifier = Modifier.size(26.dp),
            tint = mutedColor
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "No notes available",
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor
        )
    }
}

@Composable
fun OverviewCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(shape = DefaultCornerShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clip(DefaultCornerShape).noRippleClickable { onClick() }) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun BreadcrumbTrail(selectedFolderId: String?, breadcrumbs: List<FolderEntity>, onNavigate: (String?) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) {
        item {
            val isRoot = selectedFolderId == null
            Text(
                "Home",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium,
                color = if (isRoot) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.noRippleClickable { onNavigate(null) })
        }
        items(breadcrumbs) { folder ->
            Icon(
                Icons.Default.ChevronRight,
                null,
                modifier = Modifier.padding(horizontal = 6.dp).size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            val isLast = folder.folderId == selectedFolderId
            Text(
                folder.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                color = if (isLast) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.noRippleClickable { onNavigate(folder.folderId) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteMetadataEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    handlesGestures: Boolean = true
) {
    val mediaStorageHelper: com.ben.emberr.domain.util.MediaStorageHelper = koinInject()
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.onSurface; isDesktopPlatform -> MaterialTheme.colorScheme.background; else -> MaterialTheme.colorScheme.surface
    }
    val titleColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val mutedColor =
        if (isSelected) MaterialTheme.colorScheme.background.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val coverHeight = 72.dp
    val iconOverhang = 12.dp
    val hasCover = note.coverImagePath != null
    val hasIcon = !note.icon.isNullOrEmpty()
    val hasHeader = hasCover || hasIcon

    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(DefaultCornerShape)
            .background(bgColor)
            .cardGestures(handlesGestures, onClick, onLongClick)
    ) {
        Column(Modifier.fillMaxSize()) {
            if (hasHeader) {
                Box(modifier = Modifier.fillMaxWidth().height(coverHeight)) {
                    if (note.coverImagePath != null) {
                        val absolutePath =
                            mediaStorageHelper.getAbsoluteMediaPath(note.coverImagePath)
                        val context = coil3.compose.LocalPlatformContext.current
                        val request = remember(absolutePath) {
                            coil3.request.ImageRequest.Builder(context).data(absolutePath)
                                .memoryCacheKey(absolutePath).diskCacheKey(absolutePath).build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 0.12f else 0.05f))
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(
                    start = 12.dp,
                    end = if (note.isFavorite && !hasHeader) 26.dp else 12.dp,
                    top = if (hasIcon) iconOverhang + 10.dp else 10.dp,
                    bottom = 10.dp
                )
            ) {
                Text(
                    note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(text = note.snippet.takeIf { it.isNotBlank() } ?: "Empty note...",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        if (hasIcon) Text(
            text = note.icon!!,
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp)
                .offset(y = coverHeight - iconOverhang)
        )
        if (note.isFavorite) Icon(
            Icons.Default.Star,
            "Favorite",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp)
        )
        if (isSelected) Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
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

@Composable
fun NotesSelectionPill(isVisible: Boolean, selectedCount: Int, onClearSelection: () -> Unit, onDelete: () -> Unit, hazeState: HazeState, modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Surface(
            shape = DefaultCornerShape,
            color = Color.Transparent,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .customEmberrShadow(DefaultCornerShape)
                .clip(DefaultCornerShape)
                .emberrBlur(hazeState, EmberrBlur.Regular)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = DefaultCornerShape
                )
        ) {
            val pillScroll = rememberScrollState()
            Row(
                modifier = Modifier.horizontalScroll(pillScroll)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.x),
                    "Clear",
                    modifier = Modifier.size(18.dp).noRippleClickable { onClearSelection() },
                    tint = tint
                )
                Text(
                    "$selectedCount",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tint
                )
                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
                Icon(
                    painterResource(Res.drawable.trash),
                    "Move to Trash",
                    modifier = Modifier.size(18.dp).noRippleClickable { onDelete() },
                    tint = tint
                )
            }
        }
    }
}

@Composable
fun AddFolderBottomSheet(expanded: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "New Folder", subtitle = "Organize your notes.") { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp)) {
            EmberrTextField(
                value = folderName,
                onValueChange = { folderName = it },
                placeholder = "e.g. Personal, Work...",
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmberrButtonSecondary(
                    text = "Cancel",
                    onClick = { closeAnd(onDismiss) },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = "Create",
                    onClick = { if (folderName.isNotBlank()) closeAnd { onCreate(folderName.trim()) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AddNoteBottomSheet(expanded: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit, onOpenTemplates: () -> Unit = {}) {
    var noteTitle by remember { mutableStateOf("") }
    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "New Note",
        subtitle = "Give your note a fresh title.",
        headerAction = EmberrBottomSheetAction(
            icon = painterResource(Res.drawable.template),
            contentDescription = "Templates",
            onClick = onOpenTemplates
        )
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp,bottom = 16.dp)) {
            EmberrTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                placeholder = "Note title...",
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmberrButtonSecondary(
                    text = "Cancel",
                    onClick = { closeAnd(onDismiss) },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = "Create",
                    onClick = { closeAnd { onCreate(noteTitle.trim()) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TemplatesMenuContent(
    modifier: Modifier = Modifier,
    templates: List<NoteMetadataEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTemplateClick: (String) -> Unit,
    onEditTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onCreateNewTemplate: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        EmberrTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "Search templates...",
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).padding(
                horizontal = if (isDesktopPlatform) 12.dp else 0.dp,
                vertical = if (isDesktopPlatform) 12.dp else 0.dp,
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isDesktopPlatform) 8.dp else 0.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {(onCreateNewTemplate())}
                .padding(horizontal = if (isDesktopPlatform) 12.dp else 0.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.circle_plus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Create New Template", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        if (templates.isEmpty()) {
            Text(
                text = if (searchQuery.isBlank()) "No templates yet." else "No templates match \"$searchQuery\".",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 14.dp)
            )
        } else {
            templates.forEach { template ->
                TemplateRow(
                    template = template,
                    onClick = { onTemplateClick(template.noteId) },
                    onEdit = { onEditTemplate(template.noteId) },
                    onDelete = { onDeleteTemplate(template.noteId) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TemplateRow(
    template: NoteMetadataEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (isDesktopPlatform) 8.dp else 0.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {(onClick())}
            .padding(horizontal = if (isDesktopPlatform) 12.dp else 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (!template.icon.isNullOrEmpty()) {
                Text(template.icon, fontSize = 15.sp)
            } else {
                Icon(
                    painter = painterResource(Res.drawable.file_text),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                template.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            painter = painterResource(Res.drawable.pen),
            contentDescription = "Edit template",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.size(15.dp).noRippleClickable(onEdit)
        )
        Spacer(Modifier.width(14.dp))
        Icon(
            painter = painterResource(Res.drawable.trash),
            contentDescription = "Delete template",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.size(16.dp).noRippleClickable(onDelete)
        )
    }
}

// Mobile shell: same EmberrBottomSheet used by every other mobile menu in this file.
@Composable
fun TemplatesBottomSheet(
    expanded: Boolean,
    templates: List<NoteMetadataEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onTemplateClick: (String) -> Unit,
    onEditTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onCreateNewTemplate: () -> Unit
) {
    EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Templates", subtitle = "Start a new note from a template.") { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            TemplatesMenuContent(
                templates = templates,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onTemplateClick = { id -> closeAnd { onTemplateClick(id) } },
                onEditTemplate = { id -> closeAnd { onEditTemplate(id) } },
                onDeleteTemplate = onDeleteTemplate,
                onCreateNewTemplate = { closeAnd(onCreateNewTemplate) }
            )

            EmberrButtonPrimary(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
fun TemplatesDesktopMenu(
    expanded: Boolean,
    templates: List<NoteMetadataEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onTemplateClick: (String) -> Unit,
    onEditTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onCreateNewTemplate: () -> Unit
) {
    EmberrDesktopMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = Modifier.width(300.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                "Templates", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            TemplatesMenuContent(
                templates = templates,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onTemplateClick = { id -> onDismissRequest(); onTemplateClick(id) },
                onEditTemplate = { id -> onDismissRequest(); onEditTemplate(id) },
                onDeleteTemplate = onDeleteTemplate,
                onCreateNewTemplate = { onDismissRequest(); onCreateNewTemplate() },
            )
        }
    }
}

@Composable
fun SortBottomSheet(expanded: Boolean, currentSortType: SortType, currentSortOrder: SortOrder, onDismiss: () -> Unit, onSortChanged: (SortType, SortOrder) -> Unit) {
    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Sort by",
        contentHorizontalPadding = 0.dp
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            SortOptionItem(
                "Last Edited",
                currentSortType == SortType.LAST_EDITED
            ) { closeAnd { onSortChanged(SortType.LAST_EDITED, currentSortOrder) } }
            SortOptionItem(
                "Date Created",
                currentSortType == SortType.DATE_CREATED
            ) { closeAnd { onSortChanged(SortType.DATE_CREATED, currentSortOrder) } }
            SortOptionItem(
                "Name (A-Z)",
                currentSortType == SortType.NAME
            ) { closeAnd { onSortChanged(SortType.NAME, currentSortOrder) } }
            SortOptionItem(
                "Type",
                currentSortType == SortType.TYPE
            ) { closeAnd { onSortChanged(SortType.TYPE, currentSortOrder) } }
            SortOptionItem("Manual", currentSortType == SortType.MANUAL) {
                closeAnd { onSortChanged(SortType.MANUAL, currentSortOrder) }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
            SortOptionItem(
                "Ascending",
                currentSortOrder == SortOrder.ASCENDING
            ) { closeAnd { onSortChanged(currentSortType, SortOrder.ASCENDING) } }
            SortOptionItem(
                "Descending",
                currentSortOrder == SortOrder.DESCENDING
            ) { closeAnd { onSortChanged(currentSortType, SortOrder.DESCENDING) } }

            EmberrButtonPrimary(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun SortOptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SelectedOptionBackground else Color.Transparent)
            .noRippleClickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}