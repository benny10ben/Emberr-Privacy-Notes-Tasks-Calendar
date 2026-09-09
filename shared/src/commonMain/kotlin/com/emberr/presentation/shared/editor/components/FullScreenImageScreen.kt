package com.emberr.presentation.shared.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.presentation.shared.stableStatusBarsPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_left
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.download
import emberr.shared.generated.resources.trash

@Composable
fun FullScreenImageScreen(
    request: Any?,
    hasLocalFile: Boolean,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    KmpBackHandler(enabled = true) { onBack() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val hazeState = remember { HazeState() }
    val tint = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize().haze(state = hazeState).background(MaterialTheme.colorScheme.background)) {
            AsyncImage(
                model = request,
                contentDescription = "Full Screen Image",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else scale = 2.5f
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxX = (size.width * (scale - 1)) / 2
                                val maxY = (size.height * (scale - 1)) / 2
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            } else offset = Offset.Zero
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .stableStatusBarsPadding()
                .padding(top = 18.dp, start = 18.dp, end = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TopBarIconButton(
                icon = painterResource(Res.drawable.chevron_left),
                contentDescription = "Back",
                bgColor = Color.Transparent,
                tint = MaterialTheme.colorScheme.primary,
                hazeState = hazeState,
                hazeStyle = EmberrBlur.Regular,
                onClick = onBack
            )
        }

        // Bottom Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .clip(DefaultBlockShape)
                .emberrBlur(hazeState, EmberrBlur.Regular)
                .background(Color.Transparent)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = DefaultBlockShape
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                val iconSize = 18.dp

                Icon(
                    painter = painterResource(Res.drawable.download),
                    contentDescription = "Download",
                    modifier = Modifier.size(iconSize).clickable {
                        if (hasLocalFile) onDownload()
                    },
                    tint = tint
                )

                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))

                Icon(
                    painter = painterResource(Res.drawable.copy),
                    contentDescription = "Copy Image",
                    modifier = Modifier.size(iconSize).clickable {
                        if (hasLocalFile) onCopy()
                    },
                    tint = tint
                )

                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))

                Icon(
                    painter = painterResource(Res.drawable.trash),
                    contentDescription = "Delete",
                    modifier = Modifier.size(iconSize).clickable {
                        onDelete()
                        onBack()
                    },
                    tint = tint
                )
            }
        }
    }
}