package com.ben.emberr.data.local.room

/**
 * =====================================================================
 * EMBER DATABASE MIGRATION CHEAT SHEET
 * =====================================================================
 * This project uses Room AutoMigrations to safely update the local SQLite
 * database without losing user data. Whenever you change an @Entity,
 * you MUST follow one of the scenarios below.
 * * CRITICAL RULE: NEVER delete old Specs from this file! Room needs the
 * entire history to upgrade users who are several versions behind.
 * =====================================================================
 *
 * SCENARIO 1: ADDING A NEW COLUMN (Safe)
 * Room considers adding data "safe". No Spec class is needed.
 * 1. Add the column to your Entity class.
 * (Use @ColumnInfo(defaultValue = "...") if it is non-nullable).
 * 2. Open AppDatabase.kt.
 * 3. Bump the `version` up by 1.
 * 4. Add the migration to the array: AutoMigration(from = X, to = Y)
 *
 * ---------------------------------------------------------------------
 * * SCENARIO 2: REMOVING A COLUMN (Dangerous)
 * Room panics if a column disappears to prevent accidental data loss.
 * You must provide an explicit permission slip (Spec).
 * 1. Delete the column from your Entity class.
 * 2. Add a new Spec class to THIS file:

 * @DeleteColumn(tableName = "table_name", columnName = "deleted_column")
 * class DropSomethingSpec : AutoMigrationSpec

 * 3. Open AppDatabase.kt.
 * 4. Bump the `version` up by 1.
 * 5. Add the migration: AutoMigration(from = X, to = Y, spec = DropSomethingSpec::class)
 *
 * ---------------------------------------------------------------------
 * * SCENARIO 3: RENAMING A COLUMN OR TABLE
 * Room thinks a rename is a "Delete + Add". You must tell it they are
 * the same column so it carries the user's data over.
 * 1. Rename the column in your Entity class.
 * 2. Add a new Spec class to THIS file:

 * @RenameColumn(tableName = "table_name", fromColumnName = "old", toColumnName = "new")
 * class RenameSomethingSpec : AutoMigrationSpec

 * 3. Open AppDatabase.kt.
 * 4. Bump the `version` up by 1.
 * 5. Add the migration: AutoMigration(from = X, to = Y, spec = RenameSomethingSpec::class)
 * =====================================================================
 */
