package com.ben.emberr.data.local.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [
        NoteMetadataEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteBlockEntity::class,
        CalendarTaskEntity::class,
        ImageBlockEntity::class,
        DocumentBlockEntity::class,
        BookmarkBlockEntity::class,
        DatabaseTemplateEntity::class,
        CategoryEntity::class,
        SelfHostDeletedNoteEntity::class,
        ChatSessionEntity::class,
        SelfHostDeletedApiConfigEntity::class,
        CalendarEventExceptionEntity::class
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun blockDao(): BlockDao
    abstract fun calendarTaskDao(): CalendarTaskDao
    abstract fun calendarEventExceptionDao(): CalendarEventExceptionDao
    abstract fun imageBlockDao(): ImageBlockDao
    abstract fun documentBlockDao(): DocumentBlockDao
    abstract fun bookmarkBlockDao(): BookmarkBlockDao
    abstract fun databaseTemplateDao(): DatabaseTemplateDao
    abstract fun categoryDao(): CategoryDao
    abstract fun selfHostDeletedNoteDao(): SelfHostDeletedNoteDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun selfHostDeletedApiConfigDao(): SelfHostDeletedApiConfigDao
}

val APP_DATABASE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE calendar_tasks ADD COLUMN recurrenceFrequency TEXT")
        connection.execSQL("ALTER TABLE calendar_tasks ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE calendar_tasks ADD COLUMN recurrenceDaysOfWeek TEXT")
        connection.execSQL("ALTER TABLE calendar_tasks ADD COLUMN recurrenceUntil TEXT")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS calendar_event_exceptions (
                blockId TEXT NOT NULL,
                occurrenceDate TEXT NOT NULL,
                isCancelled INTEGER NOT NULL DEFAULT 0,
                isChecked INTEGER NOT NULL DEFAULT 0,
                completedAt INTEGER,
                overrideTimestamp INTEGER,
                overrideDurationMinutes INTEGER,
                overrideText TEXT,
                overrideCategoryId TEXT,
                overrideUrl TEXT,
                overrideDescription TEXT,
                PRIMARY KEY(blockId, occurrenceDate)
            )
            """.trimIndent()
        )
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>