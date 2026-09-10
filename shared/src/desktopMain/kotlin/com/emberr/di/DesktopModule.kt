package com.emberr.di

import com.emberr.core.security.AesGcmEncryptionManager
import com.emberr.core.security.secrets.DesktopSecretStore
import com.emberr.core.security.secrets.PlaintextSecretBackend
import com.emberr.core.security.secrets.SecretBackendProbe
import com.emberr.core.security.secrets.SecretBackendSelector
import com.emberr.core.security.SyncEncryptionManager
import com.emberr.data.local.prefs.DesktopSettingsManager
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.BlockDao
import com.emberr.data.local.room.BookmarkBlockDao
import com.emberr.data.local.room.CalendarTaskDao
import com.emberr.data.local.room.CategoryDao
import com.emberr.data.local.room.DatabaseTemplateDao
import com.emberr.data.local.room.DocumentBlockDao
import com.emberr.data.local.room.FolderDao
import com.emberr.data.local.room.ImageBlockDao
import com.emberr.data.local.room.NoteDao
import com.emberr.data.local.room.SelfHostDeletedNoteDao
import com.emberr.data.local.room.TagDao
import com.emberr.domain.sync.SyncRepositoryImpl
import com.emberr.domain.backup.automatic.DesktopBackupRescheduler
import com.emberr.domain.backup.automatic.BackupRescheduler
import com.emberr.domain.backup.manual.DesktopManualBackupExporter
import com.emberr.domain.backup.manual.DesktopManualBackupImporter
import com.emberr.database.DatabaseDriverFactory
import com.emberr.domain.ai.LocalAiEngine
import com.emberr.domain.ai.RagRepository
import com.emberr.domain.selfhost.crypto.KeyDerivationManager
import com.emberr.domain.selfhost.crypto.Pbkdf2KeyDerivationManager
import com.emberr.domain.selfhost.crypto.SecureSyncKeyStorage
import com.emberr.domain.selfhost.sync.SelfHostSyncScheduler
import com.emberr.domain.sync.SyncRepository
import com.emberr.domain.util.AudioRecorder
import com.emberr.domain.util.DesktopAudioRecorder
import com.emberr.domain.util.DesktopImageDownloader
import com.emberr.domain.util.DesktopMediaStorageHelper
import com.emberr.domain.util.DesktopVoiceRecognizer
import com.emberr.domain.util.ImageDownloader
import com.emberr.domain.util.MediaStorageHelper
import com.emberr.domain.util.VoiceRecognizer
import com.emberr.presentation.rag.RagViewModel
import com.emberr.presentation.reminders.DesktopReminderScheduler
import com.emberr.presentation.reminders.ReminderScheduler
import com.emberr.presentation.sync.SyncViewModel
import com.emberr.domain.sync.discovery.DesktopDiscoveryManager
import com.emberr.domain.sync.discovery.SyncDiscoveryManager
import com.emberr.database.EmberrDatabase
import org.koin.dsl.module

val desktopModule = module {

    // Room
    single<AppDatabase> {
        val builder = com.emberr.data.local.room.getDatabaseBuilder()
        builder.fallbackToDestructiveMigration(dropAllTables = true)
        com.emberr.data.local.room.getRoomDatabase(builder)
    }
    single<NoteDao> { get<AppDatabase>().noteDao() }
    single<FolderDao> { get<AppDatabase>().folderDao() }
    single<TagDao> { get<AppDatabase>().tagDao() }
    single<BlockDao> { get<AppDatabase>().blockDao() }
    single<CalendarTaskDao> { get<AppDatabase>().calendarTaskDao() }
    single<com.emberr.data.local.room.CalendarEventExceptionDao> { get<AppDatabase>().calendarEventExceptionDao() }
    single<ImageBlockDao> { get<AppDatabase>().imageBlockDao() }
    single<DocumentBlockDao> { get<AppDatabase>().documentBlockDao() }
    single<BookmarkBlockDao> { get<AppDatabase>().bookmarkBlockDao() }
    single<DatabaseTemplateDao> { get<AppDatabase>().databaseTemplateDao() }
    single<CategoryDao> { get<AppDatabase>().categoryDao() }
    single<SelfHostDeletedNoteDao> { get<AppDatabase>().selfHostDeletedNoteDao() }
    single<com.emberr.data.local.room.ChatSessionDao> { get<AppDatabase>().chatSessionDao() }
    single<com.emberr.data.local.room.SelfHostDeletedApiConfigDao> { get<AppDatabase>().selfHostDeletedApiConfigDao() }
    single<VoiceRecognizer> { DesktopVoiceRecognizer() }

    // SQLDelight
    single { DatabaseDriverFactory().createDriver() }
    single { EmberrDatabase(get()) }

    // AI
    single { LocalAiEngine(aiSettingsRepository = get()) }
    single { RagRepository(get(), get(), get(), get()) }
    single<com.emberr.domain.ai.external.SecureAiKeyStorage> {
        com.emberr.domain.ai.external.SecureAiKeyStorage(get())
    }
    single { com.emberr.domain.ai.models.LocalModelUploadManager() }
    single { com.emberr.domain.ai.models.ModelDownloadScheduler(modelDownloadManager = get()) }
    factory { RagViewModel(get(), get(), get(), get(), get(), get()) }

    // Secret storage
    single { SecretBackendProbe() }
    single { PlaintextSecretBackend(java.io.File(System.getProperty("user.home"), ".emberr")) }
    single { SecretBackendSelector(probe = get(), plaintextBackend = get()) }
    single { DesktopSecretStore(backendSelector = get(), plaintextBackend = get()) }

    // Platform implementations
    single<SettingsManager> { DesktopSettingsManager(get()) }
    single<ReminderScheduler> { DesktopReminderScheduler() }
    single<MediaStorageHelper> { DesktopMediaStorageHelper() }
    single<ImageDownloader> { DesktopImageDownloader() }
    single<AudioRecorder> { DesktopAudioRecorder() }

    // Self-hosted WebDAV sync
    single<KeyDerivationManager> { Pbkdf2KeyDerivationManager() }
    single<SecureSyncKeyStorage> { SecureSyncKeyStorage(get()) }
    single { SelfHostSyncScheduler(selfHostSyncEngine = get()) }

    // Sync
    single<SyncEncryptionManager> { AesGcmEncryptionManager() }
    single<com.emberr.core.security.SyncHmacSigner> { com.emberr.core.security.HmacSha256Signer() }
    single<SyncDiscoveryManager> { DesktopDiscoveryManager() }
    single { com.emberr.domain.sync.SyncServerAvailability() }
    single<com.emberr.domain.sync.SyncClient> { com.emberr.domain.sync.SyncClient(get(), get(), get()) }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { SyncViewModel(get(), get(), get(), get(), get(), get<com.emberr.domain.sync.SyncServerAvailability>().status) }

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