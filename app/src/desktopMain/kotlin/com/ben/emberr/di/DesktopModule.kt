package com.ben.emberr.di

import com.ben.emberr.core.security.AesGcmEncryptionManager
import com.ben.emberr.core.security.SyncEncryptionManager
import com.ben.emberr.data.local.prefs.DesktopSettingsManager
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
import com.ben.emberr.domain.backup.automatic.DesktopBackupRescheduler
import com.ben.emberr.domain.backup.automatic.BackupRescheduler
import com.ben.emberr.domain.backup.manual.DesktopManualBackupExporter
import com.ben.emberr.domain.backup.manual.DesktopManualBackupImporter
import com.ben.emberr.database.DatabaseDriverFactory
import com.ben.emberr.domain.ai.LocalAiEngine
import com.ben.emberr.domain.ai.RagRepository
import com.ben.emberr.domain.selfhost.crypto.KeyDerivationManager
import com.ben.emberr.domain.selfhost.crypto.Pbkdf2KeyDerivationManager
import com.ben.emberr.domain.selfhost.crypto.SecureSyncKeyStorage
import com.ben.emberr.domain.selfhost.sync.SelfHostSyncScheduler
import com.ben.emberr.domain.sync.SyncRepository
import com.ben.emberr.domain.util.AudioRecorder
import com.ben.emberr.domain.util.DesktopAudioRecorder
import com.ben.emberr.domain.util.DesktopImageDownloader
import com.ben.emberr.domain.util.DesktopMediaStorageHelper
import com.ben.emberr.domain.util.DesktopVoiceRecognizer
import com.ben.emberr.domain.util.ImageDownloader
import com.ben.emberr.domain.util.MediaStorageHelper
import com.ben.emberr.domain.util.VoiceRecognizer
import com.ben.emberr.presentation.rag.RagViewModel
import com.ben.emberr.presentation.reminders.DesktopReminderScheduler
import com.ben.emberr.presentation.reminders.ReminderScheduler
import com.ben.emberr.presentation.sync.SyncViewModel
import com.ben.emberr.domain.sync.discovery.DesktopDiscoveryManager
import com.ben.emberr.domain.sync.discovery.SyncDiscoveryManager
import com.emberr.database.EmberrDatabase
import org.koin.dsl.module

val desktopModule = module {

    // Room
    single<AppDatabase> {
        val builder = com.ben.emberr.data.local.room.getDatabaseBuilder()
        builder.fallbackToDestructiveMigration(dropAllTables = true)
        com.ben.emberr.data.local.room.getRoomDatabase(builder)
    }
    single<NoteDao> { get<AppDatabase>().noteDao() }
    single<FolderDao> { get<AppDatabase>().folderDao() }
    single<TagDao> { get<AppDatabase>().tagDao() }
    single<BlockDao> { get<AppDatabase>().blockDao() }
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
    single<VoiceRecognizer> { DesktopVoiceRecognizer() }

    // SQLDelight
    single { DatabaseDriverFactory().createDriver() }
    single { EmberrDatabase(get()) }

    // AI
    single { LocalAiEngine(aiSettingsRepository = get()) }
    single { RagRepository(get(), get(), get(), get()) }
    single<com.ben.emberr.domain.ai.external.SecureAiKeyStorage> { com.ben.emberr.domain.ai.external.SecureAiKeyStorage() }
    single { com.ben.emberr.domain.ai.models.LocalModelUploadManager() }
    single { com.ben.emberr.domain.ai.models.ModelDownloadScheduler(modelDownloadManager = get()) }
    factory { RagViewModel(get(), get(), get(), get(), get(), get()) }

    // Platform implementations
    single<SettingsManager> { DesktopSettingsManager() }
    single<ReminderScheduler> { DesktopReminderScheduler() }
    single<MediaStorageHelper> { DesktopMediaStorageHelper() }
    single<ImageDownloader> { DesktopImageDownloader() }
    single<AudioRecorder> { DesktopAudioRecorder() }

    // Self-hosted WebDAV sync
    single<KeyDerivationManager> { Pbkdf2KeyDerivationManager() }
    single<SecureSyncKeyStorage> { SecureSyncKeyStorage() }
    single { SelfHostSyncScheduler(selfHostSyncEngine = get()) }

    // Sync
    single<SyncEncryptionManager> { AesGcmEncryptionManager() }
    single<com.ben.emberr.core.security.SyncHmacSigner> { com.ben.emberr.core.security.HmacSha256Signer() }
    single<SyncDiscoveryManager> { DesktopDiscoveryManager() }
    single { com.ben.emberr.domain.sync.SyncServerAvailability() }
    single<com.ben.emberr.domain.sync.SyncClient> { com.ben.emberr.domain.sync.SyncClient(get(), get(), get()) }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { SyncViewModel(get(), get(), get(), get(), get(), get<com.ben.emberr.domain.sync.SyncServerAvailability>().status) }

    // Automatic Backup
    single<BackupRescheduler> { DesktopBackupRescheduler() }

    // Manual export/import
    single { DesktopManualBackupExporter(appDatabase = get(), settingsManager = get()) }
    single {
        DesktopManualBackupImporter(
            settingsManager = get(),
            backupRepository = get(),
            noteRepository = get(),
            backupRescheduler = get()
        )
    }
}