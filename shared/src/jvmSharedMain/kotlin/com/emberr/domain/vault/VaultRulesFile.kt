// Writes the CLAUDE.md rulebook into the vault so an AI tool knows the format before it edits anything.

package com.emberr.domain.vault

import java.io.File
import java.io.IOException

// Written only when missing, so edits made to it survive.
object VaultRulesFile {

    fun writeIfMissing(vaultRootDirectory: File): Boolean {
        try {
            if (!vaultRootDirectory.isDirectory && !vaultRootDirectory.mkdirs()) return false

            val rulesFile = File(vaultRootDirectory, VaultPaths.VAULT_RULES_FILE_NAME)
            if (rulesFile.exists()) return false

            rulesFile.writeText(RULES_TEXT)
            VaultLog.d("wrote ${VaultPaths.VAULT_RULES_FILE_NAME}")
            return true
        } catch (cause: IOException) {
            VaultLog.e("Could not write ${VaultPaths.VAULT_RULES_FILE_NAME}: ${cause.message}")
            return false
        } catch (cause: SecurityException) {
            VaultLog.e("Could not write ${VaultPaths.VAULT_RULES_FILE_NAME}: ${cause.message}")
            return false
        }
    }

    private val RULES_TEXT = """
        # Emberr notes

        This folder mirrors the notes in the Emberr app. Editing a file here changes the real
        note, usually within a second. Emberr has to be running for that to happen.

        ## Rules

        1. Never remove or change a `^em-xxxx` tag. It is that block's permanent id, and it is how
           an edit reaches the right block instead of rewriting the whole note.
        2. Never change `id:` in the block at the top of a file.
        3. New content does not need a tag. Emberr adds one.
        4. A block can be several lines long. The tag sits at the end of the whole block, not at
           the end of every line.

        ## How to do things

        | Goal | Do this |
        |---|---|
        | edit a block | change the text, leave the tag alone |
        | delete a block | delete the whole block including its tag |
        | add a block | write it with no tag |
        | reorder blocks | move the lines, the tags travel with them |
        | rename a note | change `title:` at the top, not the file name |
        | favourite a note | set `favorite: true` at the top |
        | create a note | make a new `.md` file in any directory - a new directory becomes a folder |
        | delete a note | delete the file, which moves the note to Trash rather than destroying it |

        Renaming the file itself does nothing. Emberr builds the file name from `title:` and will
        rename it back.

        `Daily/` and `Subnotes/` belong to Emberr. Do not create folders with those names, and do
        not put new notes in them.

        ## Formatting

        Use `**bold**`, `*italic*`, `~~strikethrough~~` and `<u>underline</u>`.

        Do not use `_` for emphasis. It is left as a literal underscore on purpose, so file names
        and snake_case survive untouched.

        ## Lists, quotes, code

        ```markdown
        - [ ] a task ^em-c3d5
        - [x] a finished task ^em-d4e6
        - a bullet ^em-e5f7
        1. a numbered item ^em-f6a8
        - ▸ a collapsible toggle ^em-a7b9
        > a quote ^em-b8c1
        ```

        Indent a list item by two spaces per level. A code block puts its tag on its own line
        after the closing fence.

        ## Task due dates and repeats

        A checkbox can carry a small group at the end of the line, before the tag:

        ```markdown
        - [ ] Email finance {due: 2026-09-12 14:00} ^em-c3d5
        - [ ] Standup {due: 2026-09-14 09:30; for: 15m; category: Work; repeat: weekly on mon,wed} ^em-d4e6
        ```

        | Key | Meaning |
        |---|---|
        | `due` | `YYYY-MM-DD HH:MM` in local time. The date on its own means midnight |
        | `for` | how long it takes, like `45m`. Left out when it is the default 30 minutes |
        | `category` | a calendar category by name, which has to already exist |
        | `repeat` | see below |
        | `link` | a web address to open from the task |
        | `details` | a longer note about the task |

        Inside a value, write `\;` for a semicolon, `\{` and `\}` for braces, and `\n` for a line
        break, so the whole group stays on one line.

        Repeat reads the way you would say it:

        ```
        daily
        every 3 days
        weekly
        weekly on mon,wed,fri
        every 2 weeks on mon
        monthly
        yearly until 2030-01-01
        ```

        Drop the whole group to clear all of it. Drop one key to clear just that one. Any key we do
        not recognise makes the braces ordinary text, so `{some note}` at the end of a line stays
        exactly as typed.

        ## Databases and tables

        A database is a config fence followed by a normal markdown table. The `id` column holds
        row ids.

        ````markdown
        ```emberr-database
        title: Q3 Budget
        view: kanban by Status
        columns:
          Item: text
          Cost: money
          Status: status
        ```

        | id   | Item   | Cost | Status      |
        |------|--------|------|-------------|
        | r-01 | Server | 240  | Done        |
        | r-02 | Domain | 12   | In Progress |
        ^em-9c02
        ````

        - delete a row by deleting its line
        - add a row by adding a line and leaving the `id` cell blank
        - change a cell by editing it
        - never invent or reuse a row id
        - column types are `text`, `number`, `money`, `checkbox`, `date`, `tags`, `url`, `email`,
          `phone`, `priority`, `status`, `files`, `audio`, `notes`, `formula`
        - the `view:` line is read-only for now, so changing it does nothing

        A plain table is just a markdown table with its tag on the line below.

        ## Things you cannot change from here

        - drawings, which appear only as a stroke count
        - voice recordings
        - cell colours, column widths, kanban settings

        None of this is at risk. Emberr keeps it all. It simply is not written into these files,
        so leave those fences and settings alone.

        ## Conflict files

        If you and someone using the app change the same block at the same time, your version is
        kept and theirs is saved beside it as `<name>.conflict.md`. Nothing is lost. Emberr
        ignores those files, so read one and delete it when you are done with it.
    """.trimIndent() + "\n"
}
