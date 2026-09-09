package com.ben.emberr.presentation.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Bullet(val text: String) : MarkdownBlock()
    data class Numbered(val index: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

private val numberedListRegex = Regex("^(\\d+)\\.\\s+(.*)")

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphBuffer.toString().trim()))
            paragraphBuffer.clear()
        }
    }

    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        when {
            trimmed.startsWith("```") -> {
                flushParagraph()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n")))
            }

            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                blocks.add(MarkdownBlock.Heading(level, trimmed.dropWhile { it == '#' }.trim()))
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Bullet(trimmed.drop(2).trim()))
            }

            numberedListRegex.matches(trimmed) -> {
                flushParagraph()
                val match = numberedListRegex.find(trimmed)!!
                val (numberText, content) = match.destructured
                blocks.add(MarkdownBlock.Numbered(numberText.toIntOrNull() ?: 1, content.trim()))
            }

            trimmed.isEmpty() -> flushParagraph()

            else -> {
                if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append(" ")
                paragraphBuffer.append(trimmed)
            }
        }
        i++
    }
    flushParagraph()
    return blocks
}

private fun buildInlineAnnotatedString(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    builder.append(text[i])
                    i++
                }
            }

            text.startsWith("__", i) -> {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    builder.append(text[i])
                    i++
                }
            }

            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    builder.withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Gray.copy(alpha = 0.2f)
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }

            text[i] == '*' || text[i] == '_' -> {
                val delimiter = text[i]
                val end = text.indexOf(delimiter, i + 1)
                if (end != -1 && end > i + 1) {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }

            else -> {
                builder.append(text[i])
                i++
            }
        }
    }
    return builder.toAnnotatedString()
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Text(
                        text = buildInlineAnnotatedString(block.text),
                        color = color,
                        style = style.copy(
                            fontSize = when (block.level) {
                                1 -> 22.sp
                                2 -> 19.sp
                                else -> 17.sp
                            },
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                is MarkdownBlock.Bullet -> {
                    Row {
                        Text("•  ", color = color, style = style)
                        Text(buildInlineAnnotatedString(block.text), color = color, style = style)
                    }
                }

                is MarkdownBlock.Numbered -> {
                    Row {
                        Text("${block.index}.  ", color = color, style = style)
                        Text(buildInlineAnnotatedString(block.text), color = color, style = style)
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = block.text,
                            color = color,
                            style = style.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(buildInlineAnnotatedString(block.text), color = color, style = style)
                }
            }
        }
    }
}
