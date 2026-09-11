// Checks that an edit to a file and an edit in the app both survive being merged.

package com.emberr.domain.vault

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FRONT_MATTER = "---\nid: note-1\ntitle: Test\ncreated: 1\nupdated: 2\n---\n"

class VaultMarkdownMergeTest {

    @Test
    fun anEditOnlyInTheFileIsTakenFromTheFile() {
        val base = noteOf("one ^em-aaa", "two ^em-bbb")
        val file = noteOf("one changed by ai ^em-aaa", "two ^em-bbb")
        val current = noteOf("one ^em-aaa", "two ^em-bbb")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertTrue(!merged.hasRealConflict)
        assertEquals(listOf("one changed by ai ^em-aaa", "two ^em-bbb"), chunksOf(merged.mergedMarkdown))
    }

    @Test
    fun anEditOnlyInTheAppIsNotUndoneByTheStaleFile() {
        val base = noteOf("one ^em-aaa", "two ^em-bbb")
        val file = noteOf("one ^em-aaa", "two ^em-bbb")
        val current = noteOf("one ^em-aaa", "two changed in app ^em-bbb")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertTrue(!merged.hasRealConflict)
        assertEquals(listOf("one ^em-aaa", "two changed in app ^em-bbb"), chunksOf(merged.mergedMarkdown))
    }

    @Test
    fun editsToDifferentBlocksAreBothKept() {
        val base = noteOf("one ^em-aaa", "two ^em-bbb")
        val file = noteOf("one from ai ^em-aaa", "two ^em-bbb")
        val current = noteOf("one ^em-aaa", "two from app ^em-bbb")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertTrue(!merged.hasRealConflict)
        assertEquals(listOf("one from ai ^em-aaa", "two from app ^em-bbb"), chunksOf(merged.mergedMarkdown))
    }

    @Test
    fun theSameBlockChangedOnBothSidesIsAConflictAndTheFileWins() {
        val base = noteOf("one ^em-aaa")
        val file = noteOf("one from ai ^em-aaa")
        val current = noteOf("one from app ^em-aaa")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertTrue(merged.hasRealConflict)
        assertEquals(listOf("aaa"), merged.conflictedTags)
        assertEquals(listOf("one from ai ^em-aaa"), chunksOf(merged.mergedMarkdown))
    }

    @Test
    fun aBlockTheFileRemovedStaysRemoved() {
        val base = noteOf("one ^em-aaa", "two ^em-bbb")
        val file = noteOf("one ^em-aaa")
        val current = noteOf("one ^em-aaa", "two ^em-bbb")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertEquals(listOf("one ^em-aaa"), chunksOf(merged.mergedMarkdown))
    }

    @Test
    fun aBlockTheAppAddedAfterTheFileWasWrittenSurvives() {
        val base = noteOf("one ^em-aaa")
        val file = noteOf("one ^em-aaa")
        val current = noteOf("one ^em-aaa", "brand new from app ^em-ccc")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertTrue(!merged.hasRealConflict)
        assertEquals(listOf("one ^em-aaa", "brand new from app ^em-ccc"), chunksOf(merged.mergedMarkdown))
    }

    @Test
    fun anUntaggedBlockAddedByTheFileIsKept() {
        val base = noteOf("one ^em-aaa")
        val file = noteOf("one ^em-aaa", "written by ai with no tag")
        val current = noteOf("one ^em-aaa")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertEquals(
            listOf("one ^em-aaa", "written by ai with no tag"),
            chunksOf(merged.mergedMarkdown)
        )
    }

    @Test
    fun theFileOrderWinsWhenBlocksAreReordered() {
        val base = noteOf("one ^em-aaa", "two ^em-bbb")
        val file = noteOf("two ^em-bbb", "one ^em-aaa")
        val current = noteOf("one ^em-aaa", "two ^em-bbb")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertEquals(listOf("two ^em-bbb", "one ^em-aaa"), chunksOf(merged.mergedMarkdown))
    }

    @Test
    fun theMergedResultKeepsTheFrontMatter() {
        val merged = VaultMarkdownMerge.merge(
            baseMarkdown = noteOf("one ^em-aaa"),
            fileMarkdown = noteOf("one changed ^em-aaa"),
            currentMarkdown = noteOf("one ^em-aaa")
        )

        assertTrue(merged.mergedMarkdown.startsWith(FRONT_MATTER))
    }

    @Test
    fun aMultiLineBlockEditedInTheFileIsTakenWholesale() {
        val base = noteOf("first\nsecond\nthird ^em-aaa")
        val file = noteOf("first\nCHANGED\nthird ^em-aaa")
        val current = noteOf("first\nsecond\nthird ^em-aaa")

        val merged = VaultMarkdownMerge.merge(base, file, current)

        assertEquals(listOf("first\nCHANGED\nthird ^em-aaa"), chunksOf(merged.mergedMarkdown))
    }

    private fun noteOf(vararg blocks: String): String =
        FRONT_MATTER + "\n" + blocks.joinToString("\n\n") + "\n"

    private fun chunksOf(markdown: String): List<String> {
        val (_, body) = VaultMarkdownScanner.splitFrontMatter(markdown)
        return VaultMarkdownScanner.scanBody(body).map { it.rawText }
    }
}
