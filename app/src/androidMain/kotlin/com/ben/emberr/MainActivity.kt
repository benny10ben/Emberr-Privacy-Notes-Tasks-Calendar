package com.ben.emberr

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.ben.emberr.domain.model.PendingShare
import com.ben.emberr.domain.util.WidgetComposeRequest
import com.ben.emberr.domain.util.WidgetComposeRequestBus
import com.ben.emberr.domain.util.WidgetCalendarDateBus
import com.ben.emberr.domain.util.WidgetCalendarEventBus
import com.ben.emberr.domain.util.WidgetNavigationBus
import com.ben.emberr.domain.util.ShareEventBus
import com.ben.emberr.presentation.shared.editor.ActiveEditorRegistry
import com.ben.emberr.presentation.widget.calendar.refreshCalendarWidgets
import com.ben.emberr.presentation.widget.calendaragenda.refreshCalendarAgendaWidgets
import com.ben.emberr.presentation.widget.note.refreshNoteWidgets
import com.ben.emberr.presentation.widget.notelist.refreshNoteListWidgets
import com.ben.emberr.presentation.widget.noteshortcut.refreshNoteShortcutWidgets
import com.ben.emberr.presentation.widget.todaytasks.refreshTodayTasksWidgets
import com.ben.emberr.presentation.widget.tasks.refreshTaskWidgets
import com.ben.emberr.presentation.widget.calendarDateUriScheme
import com.ben.emberr.presentation.widget.calendarEventUriScheme
import com.ben.emberr.presentation.widget.widgetDailyDateExtra
import com.ben.emberr.presentation.widget.widgetDailyScreenExtra
import com.ben.emberr.presentation.widget.widgetCalendarScreenExtra
import com.ben.emberr.presentation.widget.widgetHomeScreenExtra
import com.ben.emberr.presentation.widget.widgetNewEventExtra
import com.ben.emberr.presentation.widget.widgetNewNoteExtra
import com.ben.emberr.presentation.widget.widgetNewTaskExtra
import com.ben.emberr.presentation.widget.widgetNoteIdExtra
import com.ben.emberr.presentation.widget.widgetTasksScreenExtra
import com.ben.emberr.presentation.EmberrApp
import com.ben.emberr.ui.theme.EmberrTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext
import java.util.UUID
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ben.emberr.domain.media.LocalMediaGarbageCollector
import com.ben.emberr.domain.media.LocalMediaGcTrigger
import com.ben.emberr.domain.sync.AutoSyncTrigger
import com.ben.emberr.domain.sync.discovery.SyncDiscoveryManager
import com.ben.emberr.presentation.sync.SyncViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ben.emberr.ui.theme.FontSizePreference
import com.ben.emberr.ui.theme.FontStylePreference
import com.ben.emberr.ui.theme.ThemePreference
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import com.ben.emberr.data.worker.BackupScheduler
import com.ben.emberr.domain.model.NoteBlock
import com.ben.emberr.data.local.prefs.SettingsManager
import com.ben.emberr.domain.util.generateAndSaveAndroidPdf
import com.ben.emberr.presentation.navigation.Screen
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.IntentCompat
import android.provider.OpenableColumns


class MainActivity : ComponentActivity() {

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imagePickerCallback?.invoke(uri?.toString() ?: "") }

    private val pickDocument = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> documentPickerCallback?.invoke(uri?.toString() ?: "") }

    private var imagePickerCallback: ((String) -> Unit)? = null
    private var documentPickerCallback: ((String) -> Unit)? = null
    private var takePhotoCallback: ((String) -> Unit)? = null
    private var currentPhotoUri: Uri? = null

    private val mediaStorageHelper: com.ben.emberr.domain.util.MediaStorageHelper by inject()

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            takePhotoCallback?.invoke(currentPhotoUri.toString())
        }
    }

    private val settingsViewModel: com.ben.emberr.presentation.settings.SettingsViewModel by inject()
    private val settingsManager: SettingsManager by inject()
    private val backupScheduler: BackupScheduler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !EmberrApplication.isReady }
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        enableEdgeToEdge()
        (application as? EmberrApplication)?.warmUpAiEngineOnce()

        val routeForThisLaunch = consumeWidgetRoute(intent) ?: if (settingsManager.isOnboardingCompleted()) {
            Screen.Daily.route
        } else {
            Screen.Onboarding.route
        }

        handleIntent(intent)
        backupScheduler.toString()

        val syncViewModel: SyncViewModel by inject()
        val discoveryManager: SyncDiscoveryManager by inject()

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    syncViewModel.triggerAutoSync(discoveryManager)
                    syncViewModel.startForegroundWatchdog()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    syncViewModel.stopForegroundWatchdog()
                    lifecycleScope.launch(Dispatchers.Default) {
                        ActiveEditorRegistry.flushAllPending()
                        refreshNoteWidgets(this@MainActivity)
                        refreshTaskWidgets(this@MainActivity)
                        refreshNoteListWidgets(this@MainActivity)
                        refreshNoteShortcutWidgets(this@MainActivity)
                        refreshTodayTasksWidgets(this@MainActivity)
                        refreshCalendarWidgets(this@MainActivity)
                        refreshCalendarAgendaWidgets(this@MainActivity)
                    }
                }
                else -> {}
            }
        })

        @OptIn(FlowPreview::class)
        lifecycleScope.launch {
            AutoSyncTrigger.syncRequests
                .debounce(1500L.milliseconds)
                .collect {
                    syncViewModel.triggerFastSync()
                }
        }

        val localMediaGarbageCollector: LocalMediaGarbageCollector by inject()
        @OptIn(FlowPreview::class)
        lifecycleScope.launch {
            LocalMediaGcTrigger.cleanupRequests
                .debounce(2000L.milliseconds)
                .collect {
                    localMediaGarbageCollector.deleteExpiredTempFiles()
                }
        }

        setContent {
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .components {
                        add(KtorNetworkFetcherFactory(HttpClient(OkHttp)))
                    }
                    .crossfade(true)
                    .build()
            }

            val fontSizePreferenceName by settingsViewModel.fontSizePreference.collectAsState()
            val fontSizePreference = runCatching { FontSizePreference.valueOf(fontSizePreferenceName) }
                .getOrDefault(FontSizePreference.DEFAULT)

            val fontStylePreferenceName by settingsViewModel.fontStylePreference.collectAsState()
            val fontStylePreference = runCatching { FontStylePreference.valueOf(fontStylePreferenceName) }
                .getOrDefault(FontStylePreference.POPPINS)

            val themePreferenceName by settingsViewModel.themePreference.collectAsState()
            val themePreference = runCatching { ThemePreference.valueOf(themePreferenceName) }
                .getOrDefault(ThemePreference.SYSTEM)
            val darkTheme = when (themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            EmberrTheme(darkTheme = darkTheme, fontSizePreference = fontSizePreference, fontStylePreference = fontStylePreference) {
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    KoinAndroidContext {
                        val context = LocalContext.current

                        val backupFolderPickerLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocumentTree()
                        ) { uri ->
                            uri?.let {
                                try {
                                    // Take persistable permission so the background worker can use it forever
                                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    context.contentResolver.takePersistableUriPermission(it, takeFlags)

                                    // Save the URI and turn on the toggle in the ViewModel
                                    settingsViewModel.setBackupDirectory(it.toString())
                                    settingsViewModel.setAutoBackupEnabled(true)

                                    Toast.makeText(context, "Backup folder linked!", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Failed to link folder.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // State to hold payloads while the OS file picker is open
                        var pendingMarkdownContent by remember { mutableStateOf("") }
                        var pendingPdfTitle by remember { mutableStateOf("") }
                        var pendingPdfBlocks by remember { mutableStateOf(emptyList<NoteBlock>()) }

                        // Markdown Saver
                        val exportMarkdownLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("text/markdown")
                        ) { uri ->
                            uri?.let {
                                try {
                                    context.contentResolver.openOutputStream(it)?.use { stream ->
                                        stream.write(pendingMarkdownContent.toByteArray())
                                    }
                                    Toast.makeText(context, "Markdown saved", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // PDF Saver
                        val exportPdfLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/pdf")
                        ) { uri ->
                            uri?.let { generateAndSaveAndroidPdf(context, it, pendingPdfTitle, pendingPdfBlocks, mediaStorageHelper) }
                        }

                        var pendingBackupJson by remember { mutableStateOf("") }
                        val exportBackupLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/zip")
                        ) { uri ->
                            uri?.let { destinationUri ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val filesDir = context.filesDir
                                        val exporter = com.ben.emberr.domain.util.AndroidBackupExporter(context)
                                        exporter.exportToZip(destinationUri, pendingBackupJson, filesDir)

                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Backup saved!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }

                        // BACKUP IMPORT LAUNCHER
                        val importBackupLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            uri?.let { sourceUri ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val filesDir = context.filesDir
                                        val exporter = com.ben.emberr.domain.util.AndroidBackupExporter(context)

                                        // Unzip and extract JSON
                                        val jsonString = exporter.importFromZip(sourceUri, filesDir)

                                        if (jsonString != null) {
                                            // Send JSON directly to our commonMain ViewModel to handle the merge!
                                            settingsViewModel.mergeBackupJson(jsonString)

                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Invalid backup file.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        }

                        EmberrApp(
                            startRoute = routeForThisLaunch,
                            onExitApp = { finish() },
                            onPickImage = { callback ->
                                imagePickerCallback = callback
                                pickImage.launch("image/*")
                            },
                            onTakePhoto = { callback ->
                                takePhotoCallback = callback

                                val photoFile = java.io.File(this@MainActivity.filesDir, "camera_${UUID.randomUUID()}.jpg")
                                if (!photoFile.exists()) {
                                    photoFile.createNewFile()
                                }

                                currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                                    this@MainActivity,
                                    "${applicationContext.packageName}.fileprovider",
                                    photoFile
                                )
                                takePhoto.launch(currentPhotoUri!!)
                            },
                            onPickDocument = { callback ->
                                documentPickerCallback = callback
                                pickDocument.launch("*/*")
                            },
                            onOpenFile = { filePath, mimeType ->
                                try {
                                    // Use our smart helper to perfectly locate the file!
                                    val absolutePath = mediaStorageHelper.getAbsoluteMediaPath(filePath)
                                    val file = java.io.File(absolutePath)

                                    if (!file.exists()) {
                                        Toast.makeText(this@MainActivity, "This file is no longer available on this device.", Toast.LENGTH_LONG).show()
                                    } else {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            this@MainActivity,
                                            "${applicationContext.packageName}.fileprovider",
                                            file
                                        )

                                        var finalMimeType = mimeType
                                        if (finalMimeType == "*/*" || finalMimeType.isBlank()) {
                                            val extension = file.extension.lowercase()
                                            finalMimeType = android.webkit.MimeTypeMap.getSingleton()
                                                .getMimeTypeFromExtension(extension) ?: "*/*"
                                        }

                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, finalMimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        startActivity(viewIntent)
                                    }
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(this@MainActivity, "No app found to open this document type.", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@MainActivity, "Failed to open file: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            onExportMarkdown = { fileName, content ->
                                pendingMarkdownContent = content
                                exportMarkdownLauncher.launch(fileName)
                            },
                            onExportPdf = { fileName, title, blocks ->
                                pendingPdfTitle = title
                                pendingPdfBlocks = blocks
                                exportPdfLauncher.launch(fileName)
                            },
                            onExportBackup = { jsonContent ->
                                pendingBackupJson = jsonContent
                                val fileName = "EmberrBackup_${System.currentTimeMillis()}.emberr"
                                exportBackupLauncher.launch(fileName)
                            },
                            onImportBackupClick = {
                                importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
                            },
                            onRequestBackupFolder = {
                                backupFolderPickerLauncher.launch(null)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeWidgetRoute(intent)?.let { route -> WidgetNavigationBus.requestRoute(route) }
        handleIntent(intent)
    }

    private fun consumeWidgetRoute(intent: Intent?): String? {
        intent ?: return null

        val noteId = intent.getStringExtra(widgetNoteIdExtra)?.takeIf { it.isNotBlank() }
        if (noteId != null) {
            intent.removeExtra(widgetNoteIdExtra)
            return Screen.Note.createRoute(noteId)
        }

        if (intent.getBooleanExtra(widgetTasksScreenExtra, false)) {
            intent.removeExtra(widgetTasksScreenExtra)
            if (intent.getBooleanExtra(widgetNewTaskExtra, false)) {
                intent.removeExtra(widgetNewTaskExtra)
                WidgetComposeRequestBus.request(WidgetComposeRequest.NEW_TASK)
            }
            return Screen.Reminders.route
        }

        val calendarEvent = intent.data
            ?.takeIf { uri -> uri.scheme == calendarEventUriScheme }
            ?.lastPathSegment
            ?.takeIf { it.isNotBlank() }

        if (calendarEvent != null) {
            WidgetCalendarEventBus.requestEvent(calendarEvent)
            return Screen.Calendar.route
        }

        val calendarDate = intent.data
            ?.takeIf { uri -> uri.scheme == calendarDateUriScheme }
            ?.lastPathSegment
            ?.takeIf { it.isNotBlank() }

        if (calendarDate != null) {
            WidgetCalendarDateBus.requestDate(calendarDate)
            return Screen.Calendar.route
        }

        if (intent.getBooleanExtra(widgetCalendarScreenExtra, false)) {
            intent.removeExtra(widgetCalendarScreenExtra)
            if (intent.getBooleanExtra(widgetNewEventExtra, false)) {
                intent.removeExtra(widgetNewEventExtra)
                WidgetComposeRequestBus.request(WidgetComposeRequest.NEW_EVENT)
            }
            return Screen.Calendar.route
        }

        if (intent.getBooleanExtra(widgetDailyScreenExtra, false)) {
            intent.removeExtra(widgetDailyScreenExtra)
            val dateString = intent.getStringExtra(widgetDailyDateExtra)
            intent.removeExtra(widgetDailyDateExtra)
            return Screen.Daily.createRoute(dateString)
        }

        if (intent.getBooleanExtra(widgetHomeScreenExtra, false)) {
            intent.removeExtra(widgetHomeScreenExtra)
            if (intent.getBooleanExtra(widgetNewNoteExtra, false)) {
                intent.removeExtra(widgetNewNoteExtra)
                WidgetComposeRequestBus.request(WidgetComposeRequest.NEW_NOTE)
            }
            return Screen.Home.route
        }

        return null
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return

        when (intent.action) {
            Intent.ACTION_SEND -> handleSingleShare(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleMultipleShare(intent)
        }
    }

    private fun handleSingleShare(intent: Intent) {
        when {
            intent.type == "text/plain" -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    val extractedUrl = android.util.Patterns.WEB_URL.matcher(sharedText).let {
                        if (it.find()) it.group() else sharedText
                    }.trim()
                    ShareEventBus.postPendingShare(PendingShare.Link(extractedUrl))
                }
            }
            else -> {
                val uri = extractSingleUri(intent) ?: return
                val mimeType = resolveMimeType(uri, intent.type)
                if (mimeType.startsWith("image/")) {
                    ShareEventBus.postPendingShare(PendingShare.Image(uri.toString()))
                } else {
                    val fileName = queryDisplayName(uri) ?: "Shared file"
                    ShareEventBus.postPendingShare(PendingShare.Document(uri.toString(), mimeType, fileName))
                }
            }
        }
    }

    private fun handleMultipleShare(intent: Intent) {
        val uris = extractMultipleUris(intent)
        if (uris.isEmpty()) return

        val items: List<PendingShare> = uris.map { uri ->
            val mimeType = resolveMimeType(uri, intent.type)
            if (mimeType.startsWith("image/")) {
                PendingShare.Image(uri.toString())
            } else {
                val fileName = queryDisplayName(uri) ?: "Shared file"
                PendingShare.Document(uri.toString(), mimeType, fileName)
            }
        }

        ShareEventBus.postPendingShare(PendingShare.Multiple(items))
    }

    private fun extractSingleUri(intent: Intent): Uri? {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { return it }
        return intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    private fun extractMultipleUris(intent: Intent): List<Uri> {
        val streamUris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        if (streamUris.isNotEmpty()) return streamUris

        val clipData = intent.clipData ?: return emptyList()
        return (0 until clipData.itemCount).mapNotNull { index -> clipData.getItemAt(index)?.uri }
    }

    private fun resolveMimeType(uri: Uri, fallback: String?): String {
        return try {
            contentResolver.getType(uri) ?: fallback ?: "*/*"
        } catch (e: Exception) {
            e.printStackTrace()
            fallback ?: "*/*"
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}