package com.emberr.presentation.shared.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.emberr.domain.util.isDesktopPlatform
import com.emberr.domain.util.openLinkInRunningBrowser
import com.emberr.domain.util.showNativeToast
import com.emberr.domain.util.triggerHapticFeedback
import com.emberr.presentation.shared.editor.components.DesktopCursor
import com.emberr.presentation.shared.editor.components.desktopPointerCursor
import com.emberr.ui.theme.LocalAppIsDark

const val WEB_LINK_TAG = "WEB_LINK"
const val NOTE_LINK_TAG = "NOTE_LINK"

private val LinkBlueOnLightBackground = Color(0xFF1A56DB)
private val LinkBlueOnDarkBackground = Color(0xFF74A9FF)

private const val SHORTEST_POSSIBLE_LINK = 4
private const val PUNCTUATION_THAT_ENDS_A_SENTENCE = ".,;:!?)]}\"'"

private val CommonTopLevelDomains = listOf(
    "com", "org", "net", "edu", "gov", "int", "mil", "io", "dev", "app", "co", "ai", "me",
    "xyz", "info", "online", "site", "blog", "cloud", "tech", "store", "shop", "news", "tv",
    "so", "sh", "gg", "ly", "cc", "fm", "biz", "pro", "live", "page", "link", "space",
    "world", "digital", "studio", "design", "wiki", "email", "work", "group", "media",
    "uk", "us", "in", "de", "fr", "es", "it", "nl", "jp", "cn", "ca", "au", "br", "ru"
)

private val WebLinkPattern = Regex(
    "(?:https?://|www\\.)[^\\s]+" +
        "|[a-zA-Z0-9][a-zA-Z0-9-]*(?:\\.[a-zA-Z0-9-]+)*\\.(?:" +
        CommonTopLevelDomains.joinToString("|") +
        ")(?:/[^\\s]*)?",
    RegexOption.IGNORE_CASE
)

@Immutable
data class WebLink(val start: Int, val end: Int, val url: String)

@Immutable
class WebLinkActions(val openLink: (String) -> Unit, val copyLink: (String) -> Unit)

@Immutable
sealed interface HoveredLink {
    val anchor: Rect

    @Immutable
    data class Web(override val anchor: Rect, val url: String) : HoveredLink

    @Immutable
    data class Note(override val anchor: Rect, val noteId: String) : HoveredLink
}

@Stable
class LinkHoverState {
    var hoveredLink by mutableStateOf<HoveredLink?>(null)
        private set

    var rightClickedLink by mutableStateOf<HoveredLink?>(null)
        private set

    var rightClickPosition by mutableStateOf(Offset.Zero)
        private set

    fun onHover(link: HoveredLink?) {
        hoveredLink = link
    }

    fun clear() = onHover(null)

    fun openMenuFor(link: HoveredLink, position: Offset) {
        hoveredLink = null
        rightClickPosition = position
        rightClickedLink = link
    }

    fun closeMenu() {
        rightClickedLink = null
    }
}

fun findWebLinks(text: String): List<WebLink> {
    if (text.length < SHORTEST_POSSIBLE_LINK) return emptyList()
    if (!text.contains('.') && !text.contains("://")) return emptyList()

    val foundLinks = mutableListOf<WebLink>()
    for (match in WebLinkPattern.findAll(text)) {
        val start = match.range.first
        val matchEnd = match.range.last + 1
        if (start > 0 && joinsTheWordBefore(text[start - 1])) continue
        if (matchEnd < text.length && joinsTheWordAfter(text[matchEnd])) continue

        var end = matchEnd
        while (end > start && text[end - 1] in PUNCTUATION_THAT_ENDS_A_SENTENCE) end--
        if (end - start < SHORTEST_POSSIBLE_LINK) continue

        foundLinks.add(WebLink(start, end, text.substring(start, end)))
    }
    return foundLinks
}

private fun joinsTheWordBefore(character: Char): Boolean =
    character.isLetterOrDigit() || character == '@' || character == '.' || character == '/' ||
        character == '-' || character == '_'

private fun joinsTheWordAfter(character: Char): Boolean =
    character.isLetterOrDigit() || character == '-'

fun toOpenableWebLink(url: String): String = if (url.contains("://")) url else "https://$url"

fun AnnotatedString.withWebLinksHighlighted(webLinkColor: Color): AnnotatedString {
    val webLinks = findWebLinks(text)
    if (webLinks.isEmpty()) return this

    val noteLinks = getStringAnnotations(NOTE_LINK_TAG, 0, text.length)
    val builder = AnnotatedString.Builder(this)
    webLinks.forEach { webLink ->
        val sitsInsideANoteLink = noteLinks.any { webLink.start < it.end && webLink.end > it.start }
        if (!sitsInsideANoteLink) {
            builder.addStyle(
                SpanStyle(
                    color = webLinkColor,
                    fontStyle = FontStyle.Italic,
                    textDecoration = TextDecoration.Underline
                ),
                webLink.start,
                webLink.end
            )
            builder.addStringAnnotation(WEB_LINK_TAG, webLink.url, webLink.start, webLink.end)
        }
    }
    return builder.toAnnotatedString()
}

fun TextLayoutResult.webLinkAtPosition(position: Offset): String? =
    (hoveredLinkAt(position, emptySet()) as? HoveredLink.Web)?.url

fun TextLayoutResult.hoveredLinkAt(position: Offset, validNoteIds: Set<String>): HoveredLink? {
    val line = getLineForVerticalPosition(position.y)
    if (position.x < getLineLeft(line) || position.x > getLineRight(line)) return null

    val laidOutText = layoutInput.text
    if (laidOutText.isEmpty()) return null

    val offset = getOffsetForPosition(position)
    val start = maxOf(0, offset - 1)
    val end = minOf(laidOutText.length, offset + 1)

    laidOutText.getStringAnnotations(WEB_LINK_TAG, start, end).firstOrNull()?.let { webLink ->
        return HoveredLink.Web(anchorAround(webLink.start, webLink.end), webLink.item)
    }

    laidOutText.getStringAnnotations(NOTE_LINK_TAG, start, end).firstOrNull()?.let { noteLink ->
        if (validNoteIds.contains(noteLink.item)) {
            return HoveredLink.Note(anchorAround(noteLink.start, noteLink.end), noteLink.item)
        }
    }

    return null
}

private fun TextLayoutResult.anchorAround(start: Int, end: Int): Rect {
    val lastCharacter = maxOf(0, layoutInput.text.length - 1)
    val firstCharacterBox = getBoundingBox(start.coerceIn(0, lastCharacter))
    val lastCharacterBox = getBoundingBox((end - 1).coerceIn(0, lastCharacter))

    return Rect(
        left = minOf(firstCharacterBox.left, lastCharacterBox.left),
        top = minOf(firstCharacterBox.top, lastCharacterBox.top),
        right = maxOf(firstCharacterBox.right, lastCharacterBox.right),
        bottom = maxOf(firstCharacterBox.bottom, lastCharacterBox.bottom)
    )
}

fun webLinkHost(url: String): String = url
    .substringAfter("://")
    .substringBefore('/')
    .removePrefix("www.")
    .ifBlank { url }

@Composable
fun rememberLinkHoverState(): LinkHoverState = remember { LinkHoverState() }

@Composable
fun Modifier.linkHover(
    hoverState: LinkHoverState,
    validNoteIds: Set<String> = emptySet(),
    currentTextLayout: () -> TextLayoutResult?
): Modifier {
    if (!isDesktopPlatform) return this

    return this
        .pointerInput(validNoteIds) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    when (event.type) {
                        PointerEventType.Exit, PointerEventType.Press -> hoverState.clear()
                        PointerEventType.Enter, PointerEventType.Move -> {
                            val position = event.changes.firstOrNull()?.position
                            val layout = currentTextLayout()
                            hoverState.onHover(
                                if (position == null || layout == null) null
                                else layout.hoveredLinkAt(position, validNoteIds)
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
        .then(
            if (hoverState.hoveredLink != null) {
                Modifier.desktopPointerCursor(DesktopCursor.HAND, overrideDescendants = true)
            } else {
                Modifier
            }
        )
}

@Composable
fun Modifier.openLinksOnPress(
    validNoteIds: Set<String> = emptySet(),
    onOpenWebLink: (String) -> Unit,
    onOpenNoteLink: (String) -> Unit = {},
    onRightClickLink: (HoveredLink, Offset) -> Unit = { _, _ -> },
    currentTextLayout: () -> TextLayoutResult?
): Modifier {
    if (!isDesktopPlatform) return this

    return this.pointerInput(validNoteIds) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Press) continue

                val opensTheLink = event.buttons.isPrimaryPressed
                val opensTheMenu = event.buttons.isSecondaryPressed
                if (!opensTheLink && !opensTheMenu) continue

                val press = event.changes.firstOrNull() ?: continue
                if (press.isConsumed) continue

                val link = currentTextLayout()?.hoveredLinkAt(press.position, validNoteIds) ?: continue
                press.consume()

                if (opensTheMenu) {
                    onRightClickLink(link, press.position)
                } else {
                    when (link) {
                        is HoveredLink.Web -> onOpenWebLink(link.url)
                        is HoveredLink.Note -> onOpenNoteLink(link.noteId)
                    }
                }
            }
        }
    }
}

@Composable
fun rememberWebLinkColor(): Color =
    if (LocalAppIsDark.current) LinkBlueOnDarkBackground else LinkBlueOnLightBackground

@Composable
fun rememberWebLinkActions(): WebLinkActions {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    return remember(uriHandler, clipboardManager) {
        WebLinkActions(
            openLink = { url ->
                val link = toOpenableWebLink(url)
                if (!openLinkInRunningBrowser(link)) {
                    try {
                        uriHandler.openUri(link)
                    } catch (_: Exception) {
                        showNativeToast("could not open link")
                    }
                }
            },
            copyLink = { url ->
                try {
                    clipboardManager.setText(AnnotatedString(url))
                    triggerHapticFeedback()
                    showNativeToast("copied link")
                } catch (_: Exception) {
                    showNativeToast("could not copy link")
                }
            }
        )
    }
}

data class WebLinkVisualTransformation(private val webLinkColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(text.withWebLinksHighlighted(webLinkColor), OffsetMapping.Identity)
}
