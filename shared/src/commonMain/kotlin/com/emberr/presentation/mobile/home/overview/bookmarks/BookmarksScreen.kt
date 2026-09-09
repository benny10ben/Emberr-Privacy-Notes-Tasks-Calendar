package com.emberr.presentation.mobile.home.overview.bookmarks

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.util.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.stableStatusBarsPadding
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.EmberrVerticalScrollbar
import com.emberr.presentation.shared.editor.BlockSelectionPill
import com.emberr.presentation.shared.editor.FocusRequest
import com.emberr.presentation.shared.editor.blockViews.BookmarkBlockView
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_left
import emberr.shared.generated.resources.circle_plus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds

private val InputContainerShape = RoundedCornerShape(12.dp)
private val SelectionHighlightShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    viewModel: BookmarksViewModel = koinViewModel()
) {
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val groupedBlocks by viewModel.groupedBlocks.collectAsState()

    val selectedBlockIds: Set<String> by viewModel.selectedBlockIds.collectAsState()
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val clipboardManager = LocalClipboardManager.current
    val localFocusManager = LocalFocusManager.current

    val focusRequest: FocusRequest? by viewModel.focusRequest.collectAsState()
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var titleTopPx by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var topBarBottomPx by remember { mutableFloatStateOf(0f) }
    var baselineDistancePx by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val collapseRangePx = with(density) { 32.dp.toPx() }
    val isAtScrollTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }
    LaunchedEffect(isAtScrollTop, titleTopPx, topBarBottomPx) {
        if (isAtScrollTop) baselineDistancePx = titleTopPx - topBarBottomPx
    }
    val titleCollapseProgress by remember {
        derivedStateOf {
            val distance = titleTopPx - topBarBottomPx
            val scrolledPx = (baselineDistancePx - distance).coerceAtLeast(0f)
            (scrolledPx / collapseRangePx).coerceIn(0f, 1f)
        }
    }
    val onCollapsedTitleClick: () -> Unit = {
        scope.launch { listState.animateScrollToItem(0) }
    }

    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var activeBlockId by remember { mutableStateOf<String?>(null) }

    var showAddUrlInput by remember { mutableStateOf(false) }
    var newUrlInput by remember { mutableStateOf("") }
    val inputFocusRequester = remember { FocusRequester() }

    KmpBackHandler(enabled = showAddUrlInput) {
        showAddUrlInput = false
        newUrlInput = ""
    }

    KmpBackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllBookmarks()
    }

    LaunchedEffect(showAddUrlInput) {
        if (showAddUrlInput) {
            delay(100.milliseconds)
            try { inputFocusRequester.requestFocus() } catch (e: Exception) {}
        } else {
            localFocusManager.clearFocus()
        }
    }

    LaunchedEffect(focusRequest) {
        focusRequest?.let { request ->
            val id = request.id
            activeBlockId = id
            var attempts = 0
            while (focusRequesters[id] == null && attempts < 50) {
                delay(20.milliseconds)
                attempts++
            }
            try { focusRequesters[id]?.requestFocus() } catch (_: Exception) {}
            viewModel.clearFocusRequest()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(
                    top = if (isDesktopPlatform) 80.dp else 110.dp,
                    bottom = 120.dp
                ),
            ) {
                item {
                    val titleStyle = MaterialTheme.typography.titleLarge.let {
                        it.copy(fontSize = it.fontSize * 1.5f, lineHeight = it.lineHeight * 1.2f)
                    }
                    Text(
                        text = "Bookmarks",
                        style = titleStyle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(horizontal = if (isDesktopPlatform) 40.dp else 16.dp)
                            .padding(bottom = 8.dp)
                            .onGloballyPositioned { titleTopPx = it.positionInRoot().y }
                    )
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (groupedBlocks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No saved links yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    items(groupedBlocks, key = { it.monthYear }) { group ->
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                            Text(
                                text = group.monthYear,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .padding(horizontal = if (isDesktopPlatform) 40.dp else 16.dp)
                                    .padding(bottom = 12.dp)
                            )
                            BookmarkGrid(
                                blocks = group.blocks,
                                selectedBlockIds = selectedBlockIds,
                                isSelectionMode = isSelectionMode,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            EmberrVerticalScrollbar(
                listState = listState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(top = 80.dp, bottom = 24.dp)
            )

            BookmarksTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                isSelectionMode = isSelectionMode,
                hazeState = hazeState,
                collapsedTitle = "Bookmarks",
                collapsedTitleProgress = titleCollapseProgress,
                onCollapsedTitleClick = onCollapsedTitleClick,
                onPositioned = { topBarBottomPx = it.positionInRoot().y + it.size.height },
                onBackClick = {
                    if (isSelectionMode) viewModel.clearSelection() else onNavigateBack()
                },
                onAddClick = { showAddUrlInput = true }
            )

            AnimatedVisibility(
                visible = showAddUrlInput,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val defaultBgColor = if (isDesktopPlatform) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background.copy(alpha = 0.65f)
                val defaultContentColor = MaterialTheme.colorScheme.onSurface
                val barSize = if (isDesktopPlatform) 46.dp else 52.dp

                Box(
                    modifier = Modifier
                        .then(if (isDesktopPlatform) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth())
                        .imePadding()
                        .then(if (isDesktopPlatform) Modifier else Modifier.navigationBarsPadding())
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        shape = InputContainerShape,
                        color = defaultBgColor,
                        contentColor = defaultContentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barSize)
                            .customEmberrShadow(InputContainerShape)
                            .clip(InputContainerShape)
                            .emberrBlur(hazeState, EmberrBlur.Regular)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Add Link",
                                modifier = Modifier.size(20.dp),
                                tint = defaultContentColor
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = newUrlInput,
                                onValueChange = { newUrlInput = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = defaultContentColor
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(defaultContentColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(inputFocusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    if (newUrlInput.isNotBlank()) {
                                        viewModel.insertBookmarkWithUrl(newUrlInput.trim())
                                        showAddUrlInput = false
                                        newUrlInput = ""
                                        localFocusManager.clearFocus()
                                    }
                                }),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (newUrlInput.isEmpty()) {
                                            Text(
                                                text = "Paste a link...",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = defaultContentColor.copy(0.5f)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            IconButton(
                                onClick = {
                                    if (newUrlInput.isNotEmpty()) {
                                        newUrlInput = ""
                                    } else {
                                        showAddUrlInput = false
                                        localFocusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(18.dp),
                                    tint = defaultContentColor.copy(0.6f)
                                )
                            }
                        }
                    }
                }
            }

            BlockSelectionPill(
                isVisible = isSelectionMode,
                selectedCount = selectedBlockIds.size,
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAllBlocks() },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(viewModel.getSelectedText()))
                    viewModel.clearSelection()
                },
                onCut = {
                    clipboardManager.setText(AnnotatedString(viewModel.cutSelectedBlocks()))
                },
                onAddBlockAbove = {},
                onAddBlockBelow = {},
                onDelete = { viewModel.deleteSelectedBlocks() },
                onTogglePin = {},
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .then(if (isDesktopPlatform) Modifier.padding(bottom = 16.dp) else Modifier.navigationBarsPadding())
            )
        }
    }
}

@Composable
fun BookmarkGrid(
    blocks: List<BookmarkBlock>,
    selectedBlockIds: Set<String>,
    isSelectionMode: Boolean,
    viewModel: BookmarksViewModel
) {
    val columns = if (isDesktopPlatform) 3 else 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = if (isDesktopPlatform) 40.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val chunkedBlocks = blocks.chunked(columns)

        chunkedBlocks.forEach { rowBlocks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowBlocks.forEach { block ->
                    val isSelected = selectedBlockIds.contains(block.id)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        BookmarkBlockView(
                            block = block,
                            inSelectionMode = isSelectionMode,
                            onToggleSelection = { viewModel.toggleSelection(block.id) },
                            onUrlSubmit = { url -> viewModel.handleUrlSubmit(block.id, url) }
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .border(3.dp, MaterialTheme.colorScheme.primary, SelectionHighlightShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            )
                        }
                    }
                }

                val emptySpaces = columns - rowBlocks.size
                repeat(emptySpaces) {
                    Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }
}

@Composable
private fun BookmarksTopBar(
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean,
    hazeState: HazeState? = null,
    collapsedTitle: String = "",
    collapsedTitleProgress: Float = 0f,
    onCollapsedTitleClick: () -> Unit = {},
    onPositioned: (LayoutCoordinates) -> Unit = {},
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val defaultContentColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDesktopPlatform) Modifier else Modifier.stableStatusBarsPadding())
            .padding(top = if (isDesktopPlatform) 16.dp else 10.dp, start = 16.dp, end = 16.dp)
            .onGloballyPositioned(onPositioned),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarIconButton(
            icon = painterResource(Res.drawable.chevron_left),
            contentDescription = "Back",
            bgColor = Color.Transparent,
            tint = MaterialTheme.colorScheme.primary,
            hazeState = hazeState,
            hazeStyle = EmberrBlur.Regular,
            onClick = onBackClick
        )

        if (collapsedTitleProgress > 0f) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .graphicsLayer { alpha = collapsedTitleProgress }
                    .clickable(onClick = onCollapsedTitleClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = collapsedTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = defaultContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (!isSelectionMode) {
            TopBarIconButton(
                icon = painterResource(Res.drawable.circle_plus),
                contentDescription = "Add Bookmark",
                bgColor = Color.Transparent,
                tint = MaterialTheme.colorScheme.primary,
                hazeState = hazeState,
                hazeStyle = EmberrBlur.Regular,
                onClick = onAddClick
            )
        } else {
            Spacer(Modifier.size(1.dp))
        }
    }
}

