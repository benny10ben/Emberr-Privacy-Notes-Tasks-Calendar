package com.ben.emberr.domain.util

import com.ben.emberr.domain.model.*

object ExportEngine {

    fun generatePlainText(blocks: List<NoteBlock>, title: String? = null): String {
        val builder = StringBuilder()
        if (!title.isNullOrBlank()) {
            builder.appendLine(title)
            builder.appendLine()
        }
        buildPlainText(blocks, builder)
        return builder.toString().trim()
    }

    fun generateMarkdown(blocks: List<NoteBlock>, title: String? = null): String {
        val builder = StringBuilder()
        if (!title.isNullOrBlank()) {
            builder.appendLine("# $title")
            builder.appendLine()
        }
        buildMarkdown(blocks, builder)
        return builder.toString().trim()
    }

    private fun buildPlainText(blocks: List<NoteBlock>, builder: StringBuilder) {
        blocks.filter { !it.isDeleted }.forEach { block ->
            val indent = "\t".repeat(block.indentationLevel)

            when (block) {
                is TextBlock -> {
                    if (block.text.isNotBlank()) builder.appendLine("$indent${block.text}")
                    else builder.appendLine()
                }
                is HeadingBlock -> builder.appendLine("$indent${block.text}")
                is CheckboxBlock -> builder.appendLine("$indent${if (block.isChecked) "[x]" else "[ ]"} ${block.text}")
                is BulletedListBlock -> builder.appendLine("$indent• ${block.text}")
                is NumberedListBlock -> builder.appendLine("$indent${block.number}. ${block.text}")
                is ToggleBlock -> builder.appendLine("$indent▶ ${block.text}")
                is QuoteBlock -> builder.appendLine("$indent\"${block.text}\"")
                is CodeBlock -> builder.appendLine("$indent${block.code}")
                is SolidDividerBlock, is ThreeDotDividerBlock -> builder.appendLine("$indent---")
                is BookmarkBlock -> builder.appendLine("$indent${block.title ?: block.url}\n$indent${block.url}")
                is ImageBlock -> builder.appendLine("${indent}[Image]")
                is DocumentBlock -> builder.appendLine("$indent[File: ${block.fileName}]")
                is DatabaseBlock -> {
                    builder.appendLine("$indent[Database: ${block.title.ifBlank { "Untitled" }}]")
                    block.rows.filter { !it.isDeleted }.forEach { row ->
                        val rowData = block.columns.filter { !it.isDeleted }.joinToString(" | ") { col ->
                            row.cells[col.id].displayText().replace("\n", " ")
                        }
                        builder.appendLine("$indent  $rowData")
                    }
                }
                is TableBlock -> {
                    block.rows.forEach { row ->
                        builder.appendLine("$indent  ${row.joinToString(" | ")}")
                    }
                }
                is VoiceBlock, is SketchBlock -> { /* Ignored */ }
                is LinkedNoteBlock -> { /* Ignored - only linkedNoteId is available here, no title to render */ }
            }
        }
    }

    private fun buildMarkdown(blocks: List<NoteBlock>, builder: StringBuilder) {
        blocks.filter { !it.isDeleted }.forEach { block ->
            val indent = "  ".repeat(block.indentationLevel)
            val isBold = when(block) { is TextBlock -> block.isBold; is CheckboxBlock -> block.isBold; is BulletedListBlock -> block.isBold; is NumberedListBlock -> block.isBold; is QuoteBlock -> block.isBold; else -> false }
            val isItalic = when(block) { is TextBlock -> block.isItalic; is CheckboxBlock -> block.isItalic; is BulletedListBlock -> block.isItalic; is NumberedListBlock -> block.isItalic; is QuoteBlock -> block.isItalic; else -> false }
            val isStrike = when(block) { is TextBlock -> block.isStrikeThrough; is CheckboxBlock -> block.isStrikeThrough; is BulletedListBlock -> block.isStrikeThrough; is NumberedListBlock -> block.isStrikeThrough; is QuoteBlock -> block.isStrikeThrough; else -> false }

            val formattedText = formatMarkdownSpans(extractText(block), isBold, isItalic, isStrike)

            when (block) {
                is TextBlock -> {
                    if (formattedText.isNotBlank()) {
                        builder.appendLine("$indent$formattedText")
                        builder.appendLine()
                    } else {
                        builder.appendLine()
                    }
                }
                is HeadingBlock -> {
                    val hashes = "#".repeat(block.level)
                    builder.appendLine("$indent$hashes $formattedText")
                    builder.appendLine()
                }
                is CheckboxBlock -> {
                    val box = if (block.isChecked) "[x]" else "[ ]"
                    builder.appendLine("$indent- $box $formattedText")
                }
                is BulletedListBlock -> builder.appendLine("$indent- $formattedText")
                is NumberedListBlock -> builder.appendLine("$indent${block.number}. $formattedText")
                is ToggleBlock -> {
                    builder.appendLine("$indent<details>")
                    builder.appendLine("$indent  <summary>$formattedText</summary>")
                    builder.appendLine("$indent</details>")
                    builder.appendLine()
                }
                is QuoteBlock -> {
                    builder.appendLine("$indent> $formattedText")
                    builder.appendLine()
                }
                is CodeBlock -> {
                    builder.appendLine("$indent```")
                    builder.appendLine(block.code.prependIndent(indent))
                    builder.appendLine("$indent```")
                    builder.appendLine()
                }
                is SolidDividerBlock, is ThreeDotDividerBlock -> {
                    builder.appendLine()
                    builder.appendLine("$indent---")
                    builder.appendLine()
                }
                is BookmarkBlock -> {
                    val title = block.title?.ifBlank { block.url } ?: block.url
                    builder.appendLine("$indent[$title](${block.url})")
                    builder.appendLine()
                }
                is ImageBlock -> {
                    val path = block.localFilePath ?: "image"
                    builder.appendLine("$indent![Image]($path)")
                    builder.appendLine()
                }
                is DocumentBlock -> {
                    builder.appendLine("$indent[📄 ${block.fileName}](${block.localFilePath})")
                    builder.appendLine()
                }
                is DatabaseBlock -> {
                    val validCols = block.columns.filter { !it.isDeleted }
                    val headers = validCols.joinToString(" | ", prefix = "$indent| ", postfix = " |") { it.name }
                    val separator = validCols.joinToString("|", prefix = "$indent|", postfix = "|") { "---" }

                    builder.appendLine("$indent**${block.title.ifBlank { "Untitled Database" }}**")
                    builder.appendLine(headers)
                    builder.appendLine(separator)

                    block.rows.filter { !it.isDeleted }.forEach { row ->
                        val rowData = validCols.joinToString(" | ", prefix = "$indent| ", postfix = " |") { col ->
                            row.cells[col.id].displayText().replace("\n", " ")
                        }
                        builder.appendLine(rowData)
                    }
                    builder.appendLine()
                }
                is TableBlock -> {
                    val separator = block.rows.firstOrNull()?.joinToString("|", prefix = "$indent|", postfix = "|") { "---" } ?: ""
                    block.rows.forEachIndexed { index, row ->
                        builder.appendLine(row.joinToString(" | ", prefix = "$indent| ", postfix = " |"))
                        if (index == 0) builder.appendLine(separator)
                    }
                    builder.appendLine()
                }
                is VoiceBlock, is SketchBlock -> { /* Ignored */ }
                is LinkedNoteBlock -> { /* Ignored - only linkedNoteId is available here, no title to render */ }
            }
        }
    }

    private fun extractText(block: NoteBlock): String {
        return when (block) {
            is TextBlock -> block.text; is HeadingBlock -> block.text; is CheckboxBlock -> block.text
            is BulletedListBlock -> block.text; is NumberedListBlock -> block.text; is ToggleBlock -> block.text
            is QuoteBlock -> block.text; else -> ""
        }
    }

    private fun formatMarkdownSpans(text: String, isBold: Boolean, isItalic: Boolean, isStrikeThrough: Boolean): String {
        var result = text
        if (result.isBlank()) return result
        if (isItalic) result = "*$result*"
        if (isBold) result = "**$result**"
        if (isStrikeThrough) result = "~~$result~~"
        return result
    }
}