// Tests the vault tool executor's size/count caps and its write-path traversal guard.

package com.emberr.domain.ai.tools

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VaultToolExecutorHardeningTest {

    private lateinit var vaultRootDirectory: File
    private lateinit var executor: VaultToolExecutor

    @BeforeTest
    fun setUp() {
        vaultRootDirectory = Files.createTempDirectory("vault-executor-hardening-test").toFile()
        executor = VaultToolExecutor(
            vaultRootDirectory = vaultRootDirectory,
            vaultImporter = FakeVaultNoteImporter(),
            pendingWriteEvents = VaultPendingWriteEvents(),
            toolCallEvents = VaultToolCallEvents()
        )
    }

    @AfterTest
    fun tearDown() {
        vaultRootDirectory.deleteRecursively()
    }

    @Test
    fun readNoteTruncatesVeryLargeNotes() {
        val hugeContent = "x".repeat(25_000)
        File(vaultRootDirectory, "Big.md").writeText(hugeContent)

        val result = assertIs<VaultToolResult.NoteContent>(executor.readNote("Big.md"))
        assertTrue(result.markdown.length < hugeContent.length)
        assertTrue(result.markdown.endsWith("characters total]"))
    }

    @Test
    fun readNoteDoesNotTruncateNormalNotes() {
        File(vaultRootDirectory, "Small.md").writeText("- milk")

        val result = assertIs<VaultToolResult.NoteContent>(executor.readNote("Small.md"))
        assertEquals("- milk", result.markdown)
    }

    @Test
    fun listNotesFlagsTruncationBeyondTheCap() {
        repeat(201) { index -> File(vaultRootDirectory, "Note$index.md").writeText("content") }

        val result = assertIs<VaultToolResult.Notes>(executor.listNotes())
        assertEquals(200, result.notes.size)
        assertTrue(result.truncated)
    }

    @Test
    fun listNotesDoesNotFlagTruncationUnderTheCap() {
        File(vaultRootDirectory, "OnlyOne.md").writeText("content")

        val result = assertIs<VaultToolResult.Notes>(executor.listNotes())
        assertTrue(!result.truncated)
    }

    @Test
    fun searchNotesFlagsTruncationBeyondTheCap() {
        repeat(201) { index -> File(vaultRootDirectory, "Note$index.md").writeText("plants need water") }

        val result = assertIs<VaultToolResult.Notes>(executor.searchNotes("plants"))
        assertEquals(200, result.notes.size)
        assertTrue(result.truncated)
    }

    @Test
    fun applyPendingWriteCannotEscapeTheVaultThroughATraversalPath() = runTest {
        val write = VaultPendingWrite(
            kind = VaultPendingWriteKind.CREATE,
            relativePath = "../outside.md",
            proposedContent = "malicious"
        )

        val result = executor.applyPendingWrite(write)

        assertIs<VaultToolResult.Failure>(result)
        assertTrue(!File(vaultRootDirectory.parentFile, "outside.md").exists())
    }
}
