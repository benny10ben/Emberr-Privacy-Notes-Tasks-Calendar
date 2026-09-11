// Checks how the reader handles edits, deletions, insertions, reordering and database rows.

package com.emberr.domain.vault

import com.emberr.domain.model.CellData
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.ColumnType
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumn
import com.emberr.domain.model.DatabaseRow
import com.emberr.domain.model.DatabaseView
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ViewType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val FRONT_MATTER = "---\nid: note-1\ntitle: Test\ncreated: 1\nupdated: 2\n---\n"
private const val NEW_TIMESTAMP = 9_999L

class NoteMarkdownReaderTest {

    @Test
    fun editingOneLineInsideABlockKeepsTheBlockAndStampsIt() {
        val existing = TextBlock(id = "para-one", text = "first\nsecond\nthird", updatedAt = 100L)

        val blocks = read("first\nchanged\nthird ^em-paraone", listOf(existing)).blocks

        assertEquals(1, blocks.size)
        val updated = blocks.first() as TextBlock
        assertEquals("para-one", updated.id)
        assertEquals("first\nchanged\nthird", updated.text)
        assertEquals(NEW_TIMESTAMP, updated.updatedAt)
    }

    @Test
    fun anUntouchedBlockKeepsItsOriginalTimestamp() {
        val existing = TextBlock(id = "para-one", text = "unchanged", updatedAt = 100L)

        val blocks = read("unchanged ^em-paraone", listOf(existing)).blocks

        assertEquals(existing, blocks.single())
    }

    @Test
    fun removingATaggedLineDropsThatBlock() {
        val existing = listOf(
            CheckboxBlock(id = "task-one", text = "one", updatedAt = 100L),
            CheckboxBlock(id = "task-two", text = "two", updatedAt = 101L)
        )

        val blocks = read("- [ ] two ^em-tasktwo", existing).blocks

        assertEquals(listOf("task-two"), blocks.map { it.id })
    }

    @Test
    fun anUntaggedLineBecomesABrandNewBlock() {
        val existing = listOf(
            CheckboxBlock(id = "task-one", text = "one", updatedAt = 100L),
            CheckboxBlock(id = "task-two", text = "two", updatedAt = 101L)
        )

        val body = "- [ ] one ^em-taskone\n- [ ] brand new\n- [ ] two ^em-tasktwo"
        val blocks = read(body, existing).blocks

        assertEquals(3, blocks.size)
        assertEquals("task-one", blocks[0].id)
        assertEquals("task-two", blocks[2].id)
        assertEquals("generated-0", blocks[1].id)
        assertEquals("brand new", (blocks[1] as CheckboxBlock).text)
        assertEquals(NEW_TIMESTAMP, blocks[1].updatedAt)
    }

    @Test
    fun reorderingLinesKeepsEveryBlockIdentity() {
        val existing = listOf(
            CheckboxBlock(id = "task-one", text = "one", updatedAt = 100L),
            CheckboxBlock(id = "task-two", text = "two", updatedAt = 101L)
        )

        val body = "- [ ] two ^em-tasktwo\n- [ ] one ^em-taskone"
        val blocks = read(body, existing).blocks

        assertEquals(listOf("task-two", "task-one"), blocks.map { it.id })
        assertEquals(existing[1], blocks[0])
        assertEquals(existing[0], blocks[1])
    }

    @Test
    fun aRepeatedTagMakesTheSecondCopyANewBlock() {
        val existing = listOf(CheckboxBlock(id = "task-one", text = "one", updatedAt = 100L))

        val body = "- [ ] one ^em-taskone\n- [ ] one ^em-taskone"
        val blocks = read(body, existing).blocks

        assertEquals(2, blocks.size)
        assertEquals("task-one", blocks[0].id)
        assertEquals("generated-0", blocks[1].id)
        assertNotEquals(blocks[0].id, blocks[1].id)
    }

    @Test
    fun tickingACheckboxKeepsTheDueDateSpelledOutBesideIt() {
        val updated = read(
            "- [x] Email finance {due: 1970-01-01 00:00:05; for: 45m; category: Work} ^em-taskone",
            listOf(taskWithADueDate())
        ).blocks.single() as CheckboxBlock

        assertEquals(true, updated.isChecked)
        assertEquals(5_000L, updated.reminderTimestamp)
        assertEquals("category-work", updated.categoryId)
        assertEquals(45, updated.durationMinutes)
        assertEquals(NEW_TIMESTAMP, updated.completedAt)
        assertEquals(NEW_TIMESTAMP, updated.updatedAt)
    }

    @Test
    fun deletingTheGroupClearsTheDueDateBecauseTheFileIsTheTruth() {
        val updated = read("- [ ] Email finance ^em-taskone", listOf(taskWithADueDate()))
            .blocks.single() as CheckboxBlock

        assertNull(updated.reminderTimestamp)
        assertNull(updated.categoryId)
        assertNull(updated.recurrenceRule)
        assertEquals(DEFAULT_TASK_DURATION_MINUTES, updated.durationMinutes)
    }

    @Test
    fun aDueDateCanBeAddedToATaskThatHadNone() {
        val existing = CheckboxBlock(id = "task-one", text = "Email finance", updatedAt = 100L)

        val updated = read(
            "- [ ] Email finance {due: 2026-09-14 09:30; repeat: weekly on mon,wed} ^em-taskone",
            listOf(existing)
        ).blocks.single() as CheckboxBlock

        assertEquals("Email finance", updated.text)
        assertEquals(1_789_378_200_000L, updated.reminderTimestamp)
        assertEquals(
            RecurrenceRule(
                frequency = RecurrenceFrequency.WEEKLY,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
            ),
            updated.recurrenceRule
        )
        assertEquals(NEW_TIMESTAMP, updated.updatedAt)
    }

    @Test
    fun bracesThatAreNotAttributesStayPartOfTheText() {
        val existing = CheckboxBlock(id = "task-one", text = "old", updatedAt = 100L)

        val updated = read("- [ ] rename {old} to {new} ^em-taskone", listOf(existing))
            .blocks.single() as CheckboxBlock

        assertEquals("rename {old} to {new}", updated.text)
        assertNull(updated.reminderTimestamp)
    }

    private fun taskWithADueDate() = CheckboxBlock(
        id = "task-one",
        text = "Email finance",
        isChecked = false,
        reminderTimestamp = 5_000L,
        categoryId = "category-work",
        durationMinutes = 45,
        updatedAt = 100L
    )

    @Test
    fun changingATextBlockIntoAHeadingReusesTheSameBlockId() {
        val existing = TextBlock(id = "para-one", text = "Q3 Budget", updatedAt = 100L)

        val block = read("## Q3 Budget ^em-paraone", listOf(existing)).blocks.single()

        assertEquals("para-one", block.id)
        val heading = assertIs<HeadingBlock>(block)
        assertEquals(2, heading.level)
    }

    @Test
    fun deletingADatabaseRowTombstonesIt() {
        val existing = databaseBlock()

        val body = """
            ```emberr-database
            title: Budget
            columns:
              Item: text
            ```

            | id | Item |
            | --- | --- |
            | rowa | Server |
            ^em-dbone
        """.trimIndent()

        val database = read(body, listOf(existing)).blocks.single() as DatabaseBlock

        assertEquals(2, database.rows.size)
        assertEquals("row-a", database.rows[0].id)
        assertEquals(false, database.rows[0].isDeleted)
        assertEquals("row-b", database.rows[1].id)
        assertEquals(true, database.rows[1].isDeleted)
        assertEquals(NEW_TIMESTAMP, database.rows[1].updatedAt)
    }

    @Test
    fun addingADatabaseRowCreatesItWithAFreshId() {
        val existing = databaseBlock()

        val body = """
            ```emberr-database
            title: Budget
            columns:
              Item: text
            ```

            | id | Item |
            | --- | --- |
            | rowa | Server |
            | rowb | Domain |
            |  | Backups |
            ^em-dbone
        """.trimIndent()

        val database = read(body, listOf(existing)).blocks.single() as DatabaseBlock
        val liveRows = database.rows.filter { !it.isDeleted }

        assertEquals(3, liveRows.size)
        assertEquals("generated-0", liveRows[2].id)
        assertEquals("db-one", liveRows[2].databaseId)
        assertEquals(CellData.Text("Backups"), liveRows[2].cells["col-item"])
    }

    @Test
    fun aNewDatabaseGetsColumnIdsWiredToTheBlockId() {
        val body = """
            ```emberr-database
            title: Fresh
            columns:
              Name: text
              Amount: money
            ```

            | id | Name | Amount |
            | --- | --- | --- |
            |  | Server | 240 |
        """.trimIndent()

        val database = read(body, emptyList()).blocks.single() as DatabaseBlock

        assertEquals(2, database.columns.size)
        database.columns.forEach { column ->
            assertEquals(database.id, column.databaseId)
        }
        assertEquals(ColumnType.MONEY, database.columns[1].type)
        assertEquals(database.id, database.rows.single().databaseId)
        assertEquals(CellData.Number(240.0), database.rows.single().cells[database.columns[1].id])
    }

    @Test
    fun anUnknownWikiLinkIsReportedAndDropped() {
        val result = read("[[Missing Note]] ^em-linkone", emptyList())

        assertTrue(result.blocks.isEmpty())
        assertEquals(1, result.problems.size)
        assertTrue(result.problems.single().contains("Missing Note"))
    }

    private fun databaseBlock(): DatabaseBlock {
        val databaseId = "db-one"
        return DatabaseBlock(
            id = databaseId,
            title = "Budget",
            columns = listOf(
                DatabaseColumn(
                    id = "col-item",
                    databaseId = databaseId,
                    name = "Item",
                    type = ColumnType.TEXT,
                    updatedAt = 90L
                )
            ),
            rows = listOf(
                DatabaseRow(
                    id = "row-a",
                    databaseId = databaseId,
                    cells = mapOf("col-item" to CellData.Text("Server")),
                    updatedAt = 91L
                ),
                DatabaseRow(
                    id = "row-b",
                    databaseId = databaseId,
                    cells = mapOf("col-item" to CellData.Text("Domain")),
                    updatedAt = 92L
                )
            ),
            views = listOf(DatabaseView(id = "view-1", name = "Table", type = ViewType.TABLE)),
            activeViewId = "view-1",
            updatedAt = 100L
        )
    }

    private fun read(body: String, existingBlocks: List<NoteBlock>): VaultNoteReadResult {
        var generatedIdCount = 0
        return NoteMarkdownReader.readNote(
            VaultNoteReadRequest(
                markdown = FRONT_MATTER + "\n" + body + "\n",
                existingBlocks = existingBlocks,
                timestamp = NEW_TIMESTAMP,
                generateBlockId = { "generated-${generatedIdCount++}" },
                categoryIdsByLowercaseName = mapOf("work" to "category-work"),
                timeZone = TimeZone.UTC
            )
        )
    }
}
