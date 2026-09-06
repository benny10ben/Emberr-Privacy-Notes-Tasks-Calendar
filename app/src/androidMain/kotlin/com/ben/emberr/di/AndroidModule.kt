package com.ben.emberr.di

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.cash.sqldelight.db.SqlDriver
import com.ben.emberr.core.security.AesGcmEncryptionManager
import com.ben.emberr.core.security.EncryptionManager
import com.ben.emberr.core.security.SyncEncryptionManager
import com.ben.emberr.data.local.prefs.AndroidSettingsManager
import com.ben.emberr.data.local.prefs.SettingsManager
import com.ben.emberr.data.local.room.AppDatabase
import com.ben.emberr.data.local.room.BlockDao
import com.ben.emberr.data.local.room.BookmarkBlockDao
import com.ben.emberr.data.local.room.CalendarTaskDao
import com.ben.emberr.data.local.room.CategoryDao
import com.ben.emberr.data.local.room.DatabaseTemplateDao
import com.ben.emberr.data.local.room.DocumentBlockDao
import com.ben.emberr.data.local.room.FolderDao
import com.ben.emberr.data.local.room.ImageBlockDao
import com.ben.emberr.data.local.room.NoteDao
import com.ben.emberr.data.local.room.SelfHostDeletedNoteDao
import com.ben.emberr.data.local.room.TagDao
import com.ben.emberr.domain.sync.SyncRepositoryImpl
import com.ben.emberr.domain.backup.automatic.AndroidBackupRescheduler
import com.ben.emberr.domain.backup.automatic.BackupNotifier
import com.ben.emberr.domain.backup.automatic.BackupScheduler
import com.ben.emberr.domain.backup.automatic.BackupSnapshotExporter
import com.ben.emberr.domain.backup.automatic.BackupWorker
import com.ben.emberr.domain.backup.automatic.BackupRescheduler
import com.ben.emberr.domain.backup.manual.AndroidManualBackupExporter
import com.ben.emberr.domain.backup.manual.AndroidManualBackupImporter
import com.ben.emberr.database.DatabaseDriverFactory
import com.ben.emberr.domain.ai.RagRepository
import com.ben.emberr.domain.selfhost.crypto.KeyDerivationManager
import com.ben.emberr.domain.selfhost.crypto.Pbkdf2KeyDerivationManager
import com.ben.emberr.domain.selfhost.crypto.SecureSyncKeyStorage
import com.ben.emberr.domain.selfhost.sync.SelfHostSyncScheduler
import com.ben.emberr.domain.selfhost.sync.SelfHostSyncWorker
import com.ben.emberr.domain.sync.SyncRepository
import com.ben.emberr.domain.util.AndroidAudioRecorder
import com.ben.emberr.domain.util.AndroidImageDownloader
import com.ben.emberr.domain.util.AndroidMediaStorageHelper
import com.ben.emberr.domain.util.AudioRecorder
import com.ben.emberr.domain.util.ImageDownloader
import com.ben.emberr.domain.util.MediaStorageHelper
import com.ben.emberr.domain.util.NativeVoiceRecognizer
import com.ben.emberr.domain.util.VoiceRecognizer
import com.ben.emberr.presentation.rag.RagViewModel
import com.ben.emberr.presentation.reminders.AndroidReminderScheduler
import com.ben.emberr.presentation.reminders.ReminderScheduler
import com.ben.emberr.presentation.sync.SyncViewModel
import com.ben.emberr.domain.sync.discovery.AndroidDiscoveryManager
import com.ben.emberr.domain.sync.discovery.SyncDiscoveryManager
import com.emberr.database.EmberrDatabase
import net.sqlcipher.database.SupportFactory
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {

    // Platform implementations
    single<MediaStorageHelper> { AndroidMediaStorageHelper(androidContext()) }
    single<ImageDownloader> { AndroidImageDownloader(androidContext()) }
    single<VoiceRecognizer> { NativeVoiceRecognizer(androidContext()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single<AudioRecorder> { AndroidAudioRecorder(androidContext()) }

    single<SharedPreferences> {
        val masterKey = MasterKey.Builder(androidContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            androidContext(),
            "emberr_settings_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    single<SettingsManager> { AndroidSettingsManager(sharedPreferences = get()) }

    // Room
    single<ByteArray> { EncryptionManager.getDatabasePassphrase(androidContext()) }

    single<AppDatabase> {
        val passphrase = get<ByteArray>()
        val supportFactory = SupportFactory(passphrase)

        val builder = com.ben.emberr.data.local.room.getDatabaseBuilder(androidContext())
        builder
            .openHelperFactory(supportFactory)
            .fallbackToDestructiveMigration(dropAllTables = true)

        com.ben.emberr.data.local.room.getRoomDatabase(builder)
    }

    single<NoteDao> { get<AppDatabase>().noteDao() }
    single<FolderDao> { get<AppDatabase>().folderDao() }
    single<TagDao> { get<AppDatabase>().tagDao() }
    single<BlockDao> { get<AppDatabase>().blockDao() }

    single {
        com.ben.emberr.presentation.widget.note.WidgetContentReader(
            noteDao = get(),
            blockDao = get(),
            noteRepository = get()
        )
    }

    single { com.ben.emberr.presentation.widget.WidgetNoteSource(noteDao = get()) }

    single {
        com.ben.emberr.presentation.widget.calendaragenda.CalendarAgendaWidgetContentReader(
            calendarTaskDao = get(),
            categoryDao = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.calendaragenda.CalendarAgendaWidgetCoordinator(
            context = androidContext(),
            calendarTaskDao = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.calendar.CalendarWidgetContentReader(
            calendarTaskDao = get(),
            categoryDao = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.calendar.CalendarWidgetCoordinator(
            context = androidContext(),
            calendarTaskDao = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.todaytasks.TodayTasksWidgetContentReader(
            calendarTaskDao = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.todaytasks.TodayTasksWidgetCoordinator(
            context = androidContext(),
            calendarTaskDao = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.upcomingevents.UpcomingEventsWidgetContentReader(
            calendarTaskDao = get(),
            categoryDao = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.upcomingevents.UpcomingEventsWidgetCoordinator(
            context = androidContext(),
            calendarTaskDao = get()
        )
    }

    single { com.ben.emberr.presentation.widget.notelist.NoteListWidgetContentReader(noteDao = get()) }

    single { com.ben.emberr.presentation.widget.noteshortcut.NoteShortcutWidgetContentReader(noteDao = get()) }

    single {
        com.ben.emberr.presentation.widget.noteshortcut.NoteShortcutWidgetCoordinator(
            context = androidContext(),
            noteSource = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.notelist.NoteListWidgetCoordinator(
            context = androidContext(),
            contentReader = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.tasks.TasksWidgetContentReader(
            calendarTaskDao = get(),
            noteDao = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.tasks.TasksWidgetCoordinator(
            context = androidContext(),
            contentReader = get()
        )
    }

    single {
        com.ben.emberr.presentation.widget.note.NoteWidgetCoordinator(
            context = androidContext(),
            noteRepository = get(),
            contentReader = get()
        )
    }
    single<CalendarTaskDao> { get<AppDatabase>().calendarTaskDao() }
    single<com.ben.emberr.data.local.room.CalendarEventExceptionDao> { get<AppDatabase>().calendarEventExceptionDao() }
    single<ImageBlockDao> { get<AppDatabase>().imageBlockDao() }
    single<DocumentBlockDao> { get<AppDatabase>().documentBlockDao() }
    single<BookmarkBlockDao> { get<AppDatabase>().bookmarkBlockDao() }
    single<DatabaseTemplateDao> { get<AppDatabase>().databaseTemplateDao() }
    single<CategoryDao> { get<AppDatabase>().categoryDao() }
    single<SelfHostDeletedNoteDao> { get<AppDatabase>().selfHostDeletedNoteDao() }
    single<com.ben.emberr.data.local.room.ChatSessionDao> { get<AppDatabase>().chatSessionDao() }
    single<com.ben.emberr.data.local.room.SelfHostDeletedApiConfigDao> { get<AppDatabase>().selfHostDeletedApiConfigDao() }

    // SQLDelight
    single<SqlDriver> { DatabaseDriverFactory(androidContext(), get<ByteArray>()).createDriver() }
    single { EmberrDatabase(get()) }

    // AI
    single { com.ben.emberr.domain.ai.LocalAiEngine(aiSettingsRepository = get()) }
    single {
        RagRepository(
            database = get(),
            localAiEngine = get(),
            externalAiEngine = get(),
            aiSettingsRepository = get()
        )
    }
    single<com.ben.emberr.domain.ai.external.SecureAiKeyStorage> { com.ben.emberr.domain.ai.external.SecureAiKeyStorage(androidContext()) }
    single { com.ben.emberr.domain.ai.models.LocalModelUploadManager(androidContext()) }
    single { com.ben.emberr.domain.ai.models.ModelDownloadScheduler(androidContext()) }
    worker {
        com.ben.emberr.domain.ai.models.ModelDownloadWorker(
            appContext = get(),
            workerParams = get(),
            modelDownloadManager = get()
        )
    }
    viewModel {
        RagViewModel(
            ragRepository = get(),
            aiSettingsRepository = get(),
            chatSessionRepository = get(),
            modelDownloadScheduler = get(),
            reindexAllNotesUseCase = get(),
            localModelUploadManager = get()
        )
    }

    // Self-hosted WebDAV sync
    single<KeyDerivationManager> { Pbkdf2KeyDerivationManager() }
    single<SecureSyncKeyStorage> { SecureSyncKeyStorage(androidContext()) }
    single { SelfHostSyncScheduler(androidContext(), get(), get()) }
    worker {
        SelfHostSyncWorker(
            appContext = get(),
            workerParams = get(),
            selfHostSyncEngine = get()
        )
    }

    // Sync
    single<SyncEncryptionManager> { AesGcmEncryptionManager() }
    single<com.ben.emberr.core.security.SyncHmacSigner> { com.ben.emberr.core.security.HmacSha256Signer() }
    single<SyncDiscoveryManager> { AndroidDiscoveryManager(androidContext()) }
    single<com.ben.emberr.domain.sync.SyncClient> { com.ben.emberr.domain.sync.SyncClient(get(), get(), get()) }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SyncViewModel(get(), get(), get(), get(), get()) }

    // Manual export/import (unrelated to automatic backups below)
    single {
        AndroidManualBackupExporter(
            context = androidContext(),
            appDatabase = get(),
            settingsManager = get()
        )
    }
    single {
        AndroidManualBackupImporter(
            context = androidContext(),
            settingsManager = get(),
            backupRepository = get(),
            noteRepository = get(),
            backupRescheduler = get()
        )
    }

    // Automatic backups
    single { BackupSnapshotExporter(context = androidContext(), appDatabase = get(), settingsManager = get()) }
    worker {
        BackupWorker(
            appContext = get(),
            workerParams = get(),
            settingsManager = get(),
            backupNotifier = get(),
            backupExporter = get(),
            backupScheduler = get()
        )
    }
    single { BackupScheduler(context = get(), settingsManager = get()) }
    single { BackupNotifier(context = get()) }
    single<BackupRescheduler> { AndroidBackupRescheduler(backupScheduler = get()) }
}