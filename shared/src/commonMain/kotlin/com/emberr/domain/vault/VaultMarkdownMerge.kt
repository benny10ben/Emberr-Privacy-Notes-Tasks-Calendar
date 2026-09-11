// Joins an edit made to a file together with an edit made in the app, one block at a time.

package com.emberr.domain.vault

data class VaultMergeResult(
    val mergedMarkdown: String,
    val hasRealConflict: Boolean,
    val conflictedTags: List<String>
)

//   base    - what the app last wrote to the file
//   file    - what is on disk now
//   current - what the database holds now
// Each block is compared against base, so a block only one side touched keeps that side's version.
object VaultMarkdownMerge {

    fun merge(baseMarkdown: String, fileMarkdown: String, currentMarkdown: String): VaultMergeResult {
        val baseTextByTag = rawTextByTag(baseMarkdown)
        val fileChunks = bodyChunksOf(fileMarkdown)
        val currentChunks = bodyChunksOf(currentMarkdown)
        val currentTextByTag = rawTextByTag(currentMarkdown)

        val chosenChunks = mutableListOf<String>()
        val tagsTakenFromFile = mutableSetOf<String>()
        val conflictedTags = mutableListOf<String>()

        for (chunk in fileChunks) {
            val tag = chunk.tag
            if (tag == null) {
                chosenChunks.add(chunk.rawText)
                continue
            }
            tagsTakenFromFile.add(tag)

            val baseText = baseTextByTag[tag]
            val currentText = currentTextByTag[tag]

            val fileChangedIt = baseText == null || chunk.rawText != baseText
            val appChangedIt = baseText != null && currentText != null && currentText != baseText

            when {
                fileChangedIt && appChangedIt -> {
                    conflictedTags.add(tag)
                    chosenChunks.add(chunk.rawText)
                }
                appChangedIt -> chosenChunks.add(currentText.orEmpty())
                else -> chosenChunks.add(chunk.rawText)
            }
        }

        for (chunk in currentChunks) {
            val tag = chunk.tag ?: continue
            if (tag in tagsTakenFromFile) continue
            // In base means the file removed it. Not in base means the app added it since.
            if (baseTextByTag.containsKey(tag)) continue
            chosenChunks.add(chunk.rawText)
        }

        return VaultMergeResult(
            mergedMarkdown = rebuildMarkdown(fileMarkdown, chosenChunks),
            hasRealConflict = conflictedTags.isNotEmpty(),
            conflictedTags = conflictedTags
        )
    }

    private fun rebuildMarkdown(fileMarkdown: String, chosenChunks: List<String>): String {
        val frontMatterText = frontMatterTextOf(fileMarkdown)
        val body = chosenChunks.joinToString("\n\n")

        return buildString {
            if (frontMatterText.isNotEmpty()) {
                append(frontMatterText)
                append('\n')
            }
            if (body.isNotEmpty()) {
                append(body)
                append('\n')
            }
        }
    }

    private fun bodyChunksOf(markdown: String): List<VaultMarkdownChunk> {
        val (_, body) = VaultMarkdownScanner.splitFrontMatter(markdown)
        return VaultMarkdownScanner.scanBody(body)
    }

    private fun rawTextByTag(markdown: String): Map<String, String> {
        val textByTag = LinkedHashMap<String, String>()
        for (chunk in bodyChunksOf(markdown)) {
            val tag = chunk.tag ?: continue
            if (!textByTag.containsKey(tag)) textByTag[tag] = chunk.rawText
        }
        return textByTag
    }

    private fun frontMatterTextOf(markdown: String): String {
        val normalised = markdown.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalised.split('\n')
        if (lines.firstOrNull()?.trim() != "---") return ""

        val closingIndex = (1 until lines.size).firstOrNull { lines[it].trim() == "---" } ?: return ""
        return lines.subList(0, closingIndex + 1).joinToString("\n") + "\n"
    }
}
