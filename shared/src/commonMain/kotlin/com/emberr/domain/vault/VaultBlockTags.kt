// Builds the short ^em-xxxx tag that gives every block a stable identity inside a markdown file.

package com.emberr.domain.vault

import com.emberr.domain.model.NoteBlock

const val VAULT_BLOCK_TAG_PREFIX = "^em-"

private const val SHORT_TAG_LENGTH = 8
private const val TAG_LENGTH_GROWTH_STEP = 4

object VaultBlockTags {

    fun buildTagsForBlocks(blocks: List<NoteBlock>): Map<String, String> {
        val takenTags = mutableSetOf<String>()
        val tagsByBlockId = LinkedHashMap<String, String>(blocks.size)

        for (block in blocks) {
            if (tagsByBlockId.containsKey(block.id)) continue
            val tag = buildUniqueTag(block.id, takenTags)
            takenTags.add(tag)
            tagsByBlockId[block.id] = tag
        }
        return tagsByBlockId
    }

    fun shortTagFor(rawId: String): String = normalise(rawId).take(SHORT_TAG_LENGTH)

    fun renderTag(tag: String): String = VAULT_BLOCK_TAG_PREFIX + tag

    private fun buildUniqueTag(rawId: String, takenTags: Set<String>): String {
        val normalised = normalise(rawId)

        var candidateLength = SHORT_TAG_LENGTH
        var candidate = normalised.take(candidateLength)
        while (candidate in takenTags && candidateLength < normalised.length) {
            candidateLength += TAG_LENGTH_GROWTH_STEP
            candidate = normalised.take(candidateLength)
        }

        var duplicateCounter = 2
        while (candidate in takenTags) {
            candidate = normalised.take(SHORT_TAG_LENGTH) + duplicateCounter
            duplicateCounter++
        }
        return candidate
    }

    private fun normalise(rawId: String): String {
        val lettersAndDigitsOnly = rawId.filter { it.isLetterOrDigit() }.lowercase()
        return lettersAndDigitsOnly.ifEmpty { "block" }
    }
}
