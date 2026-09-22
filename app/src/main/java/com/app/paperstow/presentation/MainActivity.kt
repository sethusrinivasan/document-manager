package com.app.paperstow.presentation

import java.io.File
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.debug.TempFileCleanup
import com.app.paperstow.domain.model.Document
import com.app.paperstow.domain.model.DocumentFormat
import com.app.paperstow.domain.model.DocumentType
import com.app.paperstow.presentation.documents.DocumentListScreen
import com.app.paperstow.presentation.documents.DocumentListViewModel
import com.app.paperstow.presentation.documents.DocumentViewerScreen
import com.app.paperstow.presentation.documents.ImportScreen
import com.app.paperstow.presentation.search.SearchScreen
import com.app.paperstow.presentation.settings.SettingsScreen
import com.app.paperstow.data.local.TagColorStore
import com.app.paperstow.presentation.tags.TagManagementScreen
import com.app.paperstow.presentation.tags.TagCover
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.CheckCircle
import com.app.paperstow.presentation.demo.DemoDataViewModel
import com.app.paperstow.presentation.documents.ImportViewModel
import com.app.paperstow.BuildConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DebugLogger.i("MainActivity", "onCreate")
        val telemetryConsented = getSharedPreferences("disclaimer_prefs", MODE_PRIVATE)
            .getBoolean("telemetry_consented", false)
        val settingsTelemetry = getSharedPreferences("app_settings", MODE_PRIVATE)
            .getBoolean("telemetry_enabled", false)
        com.app.paperstow.debug.UsageTelemetry.setConsent(telemetryConsented || settingsTelemetry)
        com.app.paperstow.debug.UsageTelemetry.resetSession()

        setContent {
            val eulaPrefs = remember { getSharedPreferences("eula_prefs", MODE_PRIVATE) }
            val splashPrefs = remember { getSharedPreferences("splash_prefs", MODE_PRIVATE) }
            val disclaimerPrefs = remember { getSharedPreferences("disclaimer_prefs", MODE_PRIVATE) }
            // Check if launched via share intent (another app shared a doc to us)
            val sharedUri = remember {
                val action = intent?.action
                if (action == android.content.Intent.ACTION_SEND) {
                    intent?.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
                } else if (action == android.content.Intent.ACTION_VIEW) {
                    intent?.data
                } else null
            }

            val initialScreen = when {
                !eulaPrefs.getBoolean("eula_accepted", false) -> "eula"
                !splashPrefs.getBoolean("trail_intro_shown", false) -> "splash"
                !disclaimerPrefs.getBoolean("disclaimer_accepted", false) -> "disclaimer"
                else -> "pin"
            }
            var screen by rememberSaveable { mutableStateOf(initialScreen) }
            var selectedDocId by rememberSaveable { mutableStateOf<String?>(null) }
            var noteDocId by rememberSaveable { mutableStateOf<String?>(null) }
            var currentDocIds by rememberSaveable { mutableStateOf(listOf<String>()) }
            var viewerOrigin by rememberSaveable { mutableStateOf("main") }
            var homeFolderTag by rememberSaveable { mutableStateOf<String?>(null) }
            var documentListFilterTag by rememberSaveable { mutableStateOf<String?>(null) }
            var noteBody by rememberSaveable { mutableStateOf("") }
            val listVm: DocumentListViewModel = hiltViewModel()
            val allDocs by listVm.documents.collectAsState()
            val selectedDoc = selectedDocId?.let { id -> allDocs.find { it.id == id } }
            val noteDoc = noteDocId?.let { id -> allDocs.find { it.id == id } }
            val currentDocList = currentDocIds.mapNotNull { id -> allDocs.find { it.id == id } }
            // Dynamic dark/light theme - reads from SharedPreferences, updates immediately
            val darkMode = remember { mutableStateOf(getSharedPreferences("app_settings", MODE_PRIVATE).getBoolean("dark_mode", false)) }
            val colorScheme = if (darkMode.value) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                when {
                    screen == "eula" -> com.app.paperstow.presentation.onboarding.EulaScreen(
                        onAccepted = {
                            val prefs = getSharedPreferences("eula_prefs", MODE_PRIVATE)
                            val timestamp = System.currentTimeMillis()
                            prefs.edit()
                                .putBoolean("eula_accepted", true)
                                .putLong("accepted_timestamp", timestamp)
                                .apply()
                            com.app.paperstow.debug.DebugLogger.i("EULA", "Accepted at $timestamp")
                            screen = if (!splashPrefs.getBoolean("trail_intro_shown", false)) "splash"
                                     else if (!disclaimerPrefs.getBoolean("disclaimer_accepted", false)) "disclaimer"
                                     else "pin"
                        },
                        onDeclined = {
                            com.app.paperstow.debug.DebugLogger.w("EULA", "Declined — closing app")
                            finishAffinity()
                        }
                    )
                    screen == "splash" -> com.app.paperstow.presentation.onboarding.SplashScreen(onContinue = { skipInFuture ->
                        getSharedPreferences("splash_prefs", MODE_PRIVATE).edit()
                            .putBoolean("trail_intro_shown", true)
                            .putBoolean("skip_splash", skipInFuture || splashPrefs.getBoolean("skip_splash", false))
                            .apply()
                        screen = if (!disclaimerPrefs.getBoolean("disclaimer_accepted", false)) "disclaimer"
                                 else "pin"
                    })
                    screen == "disclaimer" -> com.app.paperstow.presentation.onboarding.DisclaimerScreen(onAccepted = { telemetryConsent ->
                        getSharedPreferences("disclaimer_prefs", MODE_PRIVATE).edit().putBoolean("disclaimer_accepted", true).putLong("accepted_timestamp", System.currentTimeMillis()).putBoolean("telemetry_consented", telemetryConsent).apply()
                        getSharedPreferences("app_settings", MODE_PRIVATE).edit().putBoolean("telemetry_enabled", telemetryConsent).apply()
                        com.app.paperstow.debug.UsageTelemetry.setConsent(telemetryConsent)
                        screen = "pin"
                    })

                    screen == "pin" -> {
                        val fa = this@MainActivity as androidx.fragment.app.FragmentActivity
                        com.app.paperstow.presentation.auth.BiometricAuthScreen(
                            activity = fa,
                            onAuthenticated = { screen = if (sharedUri != null) "import_shared" else "main" },
                            onSkipped = { screen = if (sharedUri != null) "import_shared" else "main" }
                        )
                    }
                    screen == "import_shared" -> {
                        // Received a doc from another app — auto-import it
                        val importVm: com.app.paperstow.presentation.documents.ImportViewModel = hiltViewModel()
                        LaunchedEffect(sharedUri) {
                            if (sharedUri != null) {
                                com.app.paperstow.debug.DebugLogger.i("ShareTarget", "Auto-importing shared doc: $sharedUri")
                                importVm.importFile(sharedUri)
                            }
                        }
                        val sState by importVm.state.collectAsState()
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            when {
                                sState.isImporting -> { CircularProgressIndicator(Modifier.size(64.dp)); Spacer(Modifier.height(16.dp)); Text("Importing shared document...") }
                                sState.importedDocument != null -> {
                                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                                    Spacer(Modifier.height(16.dp)); Text("Imported!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(24.dp)); Button(onClick = { importVm.clearState(); screen = "main" }) { Text("Done") }
                                }
                                sState.error != null -> {
                                    Text("\u274C", fontSize = 48.sp)
                                    Spacer(Modifier.height(16.dp)); Text("Import Failed", color = Color(0xFFF44336))
                                    Text(sState.error ?: "", fontSize = 13.sp, color = Color.Gray)
                                    Spacer(Modifier.height(24.dp)); Button(onClick = { screen = "main" }) { Text("Go to Home") }
                                }
                                else -> { CircularProgressIndicator(); Text("Preparing...") }
                            }
                        }
                    }
                    screen == "import" -> ImportScreen(
                        onDone = { screen = "main" }
                    )
                    screen == "note" -> com.app.paperstow.presentation.documents.TextNoteEditorScreen(
                        existing = noteDoc,
                        initialBody = noteBody,
                        extraTags = listOfNotNull(homeFolderTag?.takeUnless { it.startsWith("__") }),
                        onBack = { screen = if (noteDocId != null) "viewer" else "main" },
                        onSaved = { saved ->
                            selectedDocId = saved.id
                            noteDocId = saved.id
                            screen = "viewer"
                        }
                    )
                    screen == "checklist" -> com.app.paperstow.presentation.documents.ChecklistEditorScreen(
                        existing = noteDoc,
                        initialBody = noteBody,
                        extraTags = listOfNotNull(homeFolderTag?.takeUnless { it.startsWith("__") }),
                        onBack = { screen = if (noteDocId != null) "viewer" else "main" },
                        onSaved = { saved ->
                            selectedDocId = saved.id
                            noteDocId = saved.id
                            screen = "viewer"
                        }
                    )
                    screen == "documents" -> DocumentListScreen(
                        filterTag = documentListFilterTag,
                        onBack = {
                            screen = if (documentListFilterTag != null) "tags" else "main"
                        },
                        onDocumentClick = { doc -> selectedDocId = doc.id; viewerOrigin = "documents"; screen = "viewer" },
                        onSetDocList = { currentDocIds = it.map { d -> d.id } }
                    )
                    screen == "viewer" && selectedDoc != null -> {
                        val currentIdx = currentDocList.indexOfFirst { it.id == selectedDoc!!.id }
                        DocumentViewerScreen(
                            document = selectedDoc!!,
                            onBack = { screen = viewerOrigin },
                            onPrev = if (currentIdx > 0) { { selectedDocId = currentDocList[currentIdx - 1].id } } else null,
                            onNext = if (currentIdx < currentDocList.size - 1) { { selectedDocId = currentDocList[currentIdx + 1].id } } else null,
                            onEditNote = { text ->
                                noteDocId = selectedDocId
                                noteBody = text
                                screen = if (selectedDoc?.format == DocumentFormat.MARKDOWN) "checklist" else "note"
                            }
                        )
                    }
                    screen == "viewer" -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    screen == "search" -> SearchScreen(onBack = { screen = "main" }, onDocumentClick = { doc -> selectedDocId = doc.id; viewerOrigin = "search"; screen = "viewer" })
                    screen == "tags" -> TagManagementScreen(
                        onBack = { screen = "main" },
                        onViewDocs = { tag ->
                            documentListFilterTag = tag
                            viewerOrigin = "documents"
                            screen = "documents"
                        }
                    )
                    screen == "settings" -> SettingsScreen(onBack = { screen = "main" }, onReset = { resetApp() })
                    screen == "about" -> com.app.paperstow.presentation.about.AboutScreen(onBack = { screen = "main" })
                    screen == "eula_view" -> com.app.paperstow.presentation.onboarding.EulaViewScreen(onBack = { screen = "main" })
                    screen == "review" -> com.app.paperstow.presentation.review.ReviewScreen(onBack = { screen = "main" })
                    screen == "feedback" -> com.app.paperstow.presentation.feedback.FeedbackScreen(
                        onBack = { screen = "main" },
                        onNoteSaved = { saved ->
                            selectedDocId = saved.id
                            viewerOrigin = "main"
                            homeFolderTag = com.app.paperstow.presentation.feedback.APP_FEEDBACK_TAG
                            screen = "viewer"
                        }
                    )
                    screen == "backup" -> com.app.paperstow.presentation.backup.BackupOnlyScreen(onBack = { screen = "main" })
                    screen == "restore" -> com.app.paperstow.presentation.backup.RestoreOnlyScreen(onBack = { screen = "main" })
                    screen == "safety" -> com.app.paperstow.presentation.safety.SafetyLocationsScreen(onBack = { screen = "main" })
                    else -> {
                        com.app.paperstow.presentation.safety.AutoStartTrailIfWanted()
                        MainScreen(
                        onImport = { com.app.paperstow.debug.UsageTelemetry.action("Main", "tap_import"); screen = "import" },
                        onNewNote = { noteDocId = null; noteBody = ""; screen = "note" },
                        onNewChecklist = { noteDocId = null; noteBody = ""; screen = "checklist" },
                        onAllDocs = {
                            com.app.paperstow.debug.UsageTelemetry.action("Main", "tap_documents")
                            documentListFilterTag = null
                            screen = "documents"
                        },
                        onSearch = { com.app.paperstow.debug.UsageTelemetry.action("Main", "tap_search"); screen = "search" },
                        onTags = { screen = "tags" },
                        onSettings = { screen = "settings" },
                        onBackup = { screen = "backup" },
                        onRestore = { screen = "restore" },
                        onAbout = { screen = "about" },
                        onFeedback = { screen = "feedback" },
                        onReview = { screen = "review" },
                        onSafety = { screen = "safety" },
                        selectedFolder = homeFolderTag,
                        onFolderChange = { homeFolderTag = it },
                        onDocClick = { doc, folderDocs ->
                            selectedDocId = doc.id
                            currentDocIds = folderDocs.map { it.id }
                            viewerOrigin = "main"
                            screen = "viewer"
                        },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); Thread { TempFileCleanup.cleanSharedDocs(applicationContext) }.start() }
    override fun onPause() { super.onPause(); com.app.paperstow.debug.UsageTelemetry.emitSessionSummary() }

    private fun resetApp(): String {
        DebugLogger.w("App", "!!! FACTORY RESET triggered by user")
        val report = StringBuilder()

        // Checkpoint WAL before any operations
        val dbPath = getDatabasePath("traveldocs.db")
        try {
            if (dbPath.exists()) {
                val flushDb = android.database.sqlite.SQLiteDatabase.openDatabase(dbPath.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
                flushDb.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
                flushDb.close()
                report.appendLine("WAL checkpoint: done")
            }
        } catch (e: Exception) { report.appendLine("WAL checkpoint: skipped (${e.message})") }

        // Archive old database
        if (dbPath.exists()) {
            val archiveDir = java.io.File(filesDir, "db_archive")
            archiveDir.mkdirs()
            for (i in 10 downTo 2) {
                val older = java.io.File(archiveDir, "traveldocs.db.${String.format("%03d", i - 1)}")
                val newer = java.io.File(archiveDir, "traveldocs.db.${String.format("%03d", i)}")
                if (older.exists()) older.renameTo(newer)
            }
            val archive = java.io.File(archiveDir, "traveldocs.db.001")
            dbPath.copyTo(archive, overwrite = true)
            report.appendLine("DB archived: ${archive.name} (${archive.length()/1024}KB)")
        }

        // Delete database + journals
        deleteDatabase("traveldocs.db")
        java.io.File(dbPath.path + "-wal").delete()
        java.io.File(dbPath.path + "-shm").delete()
        report.appendLine("Database deleted: traveldocs.db + WAL/SHM")

        // Clear preferences
        val prefsToWipe = listOf("traveldocs_stats", "encryption_consent", "disclaimer_prefs",
            "splash_prefs", "eula_prefs", "location_tracking_prefs", "safety_trail_prefs", "feature_flags",
            "app_settings", "tag_colors", "secure_doc_pins", "security_alert_prefs")
        prefsToWipe.forEach { getSharedPreferences(it, MODE_PRIVATE).edit().clear().commit() }
        report.appendLine("Preferences cleared: ${prefsToWipe.size} stores")

        // Delete document files
        val docsDir = java.io.File(filesDir, "docs")
        val docCount = if (docsDir.exists()) docsDir.walkTopDown().filter { it.isFile }.count() else 0
        docsDir.deleteRecursively()
        report.appendLine("Document files deleted: $docCount files")

        // Delete debug logs
        val logsDir = java.io.File(filesDir, "debug_logs")
        logsDir.deleteRecursively()
        report.appendLine("Debug logs deleted")

        // Delete crash report
        java.io.File(filesDir, "last_crash_report.txt").delete()
        report.appendLine("Crash report deleted")

        // Delete cache
        cacheDir.deleteRecursively()
        report.appendLine("Cache cleared")

        report.appendLine("")
        report.appendLine("Reset complete. App needs restart to initialize fresh.")
        return report.toString()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onImport: () -> Unit, onNewNote: () -> Unit, onNewChecklist: () -> Unit, onAllDocs: () -> Unit, onSearch: () -> Unit,
    onTags: () -> Unit, onSettings: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit, onAbout: () -> Unit, onFeedback: () -> Unit, onReview: () -> Unit,
    onSafety: () -> Unit,
    selectedFolder: String? = null,
    onFolderChange: (String?) -> Unit = {},
    onDocClick: (Document, List<Document>) -> Unit
) {
    val viewModel: DocumentListViewModel = hiltViewModel()
    val demoViewModel: DemoDataViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val tagLooks = remember { TagColorStore(context) }
    val documents by viewModel.documents.collectAsState()
    val demo by demoViewModel.state.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()
    var showGearMenu by remember { mutableStateOf(false) }
    var showLoadDemo by remember { mutableStateOf(false) }
    var showRemoveDemo by remember { mutableStateOf(false) }
    var showTips by remember {
        mutableStateOf(context.getSharedPreferences("app_settings", 0).getBoolean("show_tips", true))
    }
    
    if (showLoadDemo) {
        AlertDialog(
            onDismissRequest = { showLoadDemo = false },
            title = { Text("Load sample trip?") },
            text = {
                Text(
                    "Adds a fictional trip for John Doe and Jane Doe — papers, health records, a checklist, and a sample trail. They stay on this phone. Remove them anytime from the menu.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLoadDemo = false
                    demoViewModel.importSampleTrip()
                }) { Text("Load sample") }
            },
            dismissButton = { TextButton(onClick = { showLoadDemo = false }) { Text("Not now") } }
        )
    }
    if (showRemoveDemo) {
        AlertDialog(
            onDismissRequest = { showRemoveDemo = false },
            title = { Text("Remove sample trip?") },
            text = { Text("This deletes only the sample papers. Anything you imported yourself stays.", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDemo = false
                    demoViewModel.removeSampleTrip()
                }) { Text("Remove samples") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDemo = false }) { Text("Cancel") } }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { val customTitle = remember { context.getSharedPreferences("app_settings", 0).getString("home_title", "Paperstow") ?: "Paperstow" }; Text(customTitle) },
                actions = {
                    IconButton(onClick = { isRefreshing = true; viewModel.forceRefresh(); refreshScope.launch { kotlinx.coroutines.delay(800); isRefreshing = false } }) { Icon(Icons.Filled.Refresh, "Refresh") }
                    Box {
                        IconButton(onClick = { showGearMenu = true }) { Icon(Icons.Filled.Settings, "More") }
                        DropdownMenu(expanded = showGearMenu, onDismissRequest = { showGearMenu = false }) {
                            DropdownMenuItem(text = { Text("Tags") }, onClick = { showGearMenu = false; onTags() })
                            DropdownMenuItem(text = { Text("Review & Classify") }, onClick = { showGearMenu = false; onReview() })
                            DropdownMenuItem(text = { Text("My Trail") }, onClick = { showGearMenu = false; onSafety() })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Backup") }, onClick = { showGearMenu = false; onBackup() })
                            DropdownMenuItem(text = { Text("Restore") }, onClick = { showGearMenu = false; onRestore() })
                            HorizontalDivider()
                            if (!demo.loaded) {
                                DropdownMenuItem(text = { Text("Load sample trip") }, onClick = { showGearMenu = false; showLoadDemo = true })
                            } else {
                                DropdownMenuItem(text = { Text("Remove sample trip") }, onClick = { showGearMenu = false; showRemoveDemo = true })
                            }
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { showGearMenu = false; onSettings() })
                            DropdownMenuItem(text = { Text("Feedback") }, onClick = { showGearMenu = false; onFeedback() })
                            DropdownMenuItem(text = { Text("About") }, onClick = { showGearMenu = false; onAbout() })
                        }
                    }                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // Pull to refresh indicator
            if (isRefreshing) { androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) }
            androidx.compose.material3.TextButton(onClick = { isRefreshing = true; refreshScope.launch { delay(1000); isRefreshing = false } }, modifier = Modifier.fillMaxWidth().height(4.dp)) {}
            val landscapeHome = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (landscapeHome) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onImport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Import", fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onAllDocs, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Folder, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("All Docs", fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onSearch, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Search", fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onNewNote, modifier = Modifier.weight(1f)) {
                        Text("Note", fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onNewChecklist, modifier = Modifier.weight(1f)) {
                        Text("Checklist", fontSize = 13.sp)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onImport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Import", fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onAllDocs, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Folder, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("All Docs", fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onSearch, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Search", fontSize = 13.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onNewNote, modifier = Modifier.weight(1f)) {
                        Text("Write a note", fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onNewChecklist, modifier = Modifier.weight(1f)) {
                        Text("Checklist", fontSize = 13.sp)
                    }
                }
            }
            if (demo.busy) {
                androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                Text("Updating sample trip…", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            }
            // Folder view by tags. Internal/system tags (__ prefix) stay hidden.
            val allTags = documents.flatMap { it.tags }.map { it.name }.filter { !it.startsWith("__") }.distinct().sorted()
                .filter { tagLooks.isShownOnHome(it) }
            val untaggedDocs = documents.filter { it.tags.none { t -> !t.name.startsWith("__") } }
            val selectedTag = selectedFolder
            if (demo.message != null && selectedTag == null) {
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(demo.message!!, fontSize = 13.sp)
                        TextButton(onClick = { demoViewModel.dismissMessage() }) { Text("Got it") }
                    }
                }
            }

            if (demo.loaded && showTips && selectedTag == null && demo.message == null) {
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Walk through the sample trip", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("1. Open Passport, Health, or Plans", fontSize = 13.sp)
                        Text("2. Tap a PDF, checklist, or trail file to preview", fontSize = 13.sp)
                        Text("3. Search for “John Doe” or “Jane Doe”", fontSize = 13.sp)
                        Text("When you are ready, menu → Remove sample trip.", fontSize = 12.sp, color = Color.Gray)
                        TextButton(onClick = {
                            showTips = false
                            context.getSharedPreferences("app_settings", 0).edit().putBoolean("show_tips", false).apply()
                        }) { Text("Hide tips") }
                    }
                }
            }

            if (selectedTag != null) {
                // Show documents in selected folder
                val folderDocs = when (selectedTag) {
                    "__untagged__" -> untaggedDocs
                    else -> documents.filter { doc -> doc.tags.any { it.name == selectedTag } }
                }
                val folderTitle = when (selectedTag) {
                    "__untagged__" -> "Untagged"
                    else -> selectedTag!!
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onFolderChange(null) }) { Icon(Icons.Filled.ArrowBack, "Back to folders") }
                    Text(folderTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${folderDocs.size} docs", fontSize = 12.sp, color = Color.Gray)
                    IconButton(onClick = { viewModel.share(context, folderDocs) }) { Icon(Icons.Filled.Share, "Share this folder") }
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(folderDocs.take(50)) { doc ->
                        DocCard(doc = doc, onClick = { onDocClick(doc, folderDocs) })
                        Spacer(Modifier.height(6.dp))
                    }
                }
            } else {
                // Show folder grid
                Text("Organized by tags", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                val widthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                val columns = when {
                    widthDp >= 840 -> 4
                    widthDp >= 600 -> 3
                    else -> 2
                }
                val folders = allTags + (if (untaggedDocs.isNotEmpty()) listOf("__untagged__") else emptyList())
                if (folders.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.FlightTakeoff, null, Modifier.size(56.dp), tint = Color(0xFF1565C0))
                        Spacer(Modifier.height(12.dp))
                        Text("Nothing stowed yet", fontSize = 16.sp, color = Color.Gray)
                        Text("Try a sample family trip, or add your own papers.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showLoadDemo = true }, enabled = !demo.busy) { Text("Load sample trip") }
                        Spacer(Modifier.height(8.dp))
                        Text("PDFs, photos, and notes — remove them from the menu anytime.", fontSize = 12.sp, color = Color.LightGray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders, key = { it }) { tag ->
                            val label = if (tag == "__untagged__") "Untagged" else tag
                            val count = if (tag == "__untagged__") untaggedDocs.size else documents.count { d -> d.tags.any { it.name == tag } }
                            FolderTile(
                                tag = tag,
                                label = label,
                                count = count,
                                store = tagLooks,
                                onClick = { onFolderChange(tag) }
                            )
                        }
                    }
                }
            }        }
    }
}

@Composable
private fun FolderTile(
    tag: String,
    label: String,
    count: Int,
    store: TagColorStore,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.fillMaxWidth().aspectRatio(0.92f)) {
            TagCover(
                tagName = tag,
                store = store,
                untagged = tag == "__untagged__",
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Text(
                "$label($count)",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DocCard(doc: Document, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(iconFor(doc.type), doc.type.name, tint = colorFor(doc.type), modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.originalFileName ?: "Document", fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                Text(doc.type.name, fontSize = 12.sp, color = Color.Gray)
                if (doc.tags.isNotEmpty()) Text(doc.tags.joinToString(", ") { it.name }, fontSize = 11.sp, color = Color(0xFF1565C0), maxLines = 1)
            }
        }
    }
}

private fun iconFor(type: DocumentType): ImageVector = when (type) {
    DocumentType.PASSPORT -> Icons.Filled.AccountBox
    DocumentType.VISA -> Icons.Filled.CreditCard
    DocumentType.TICKET -> Icons.Filled.AirplanemodeActive
    DocumentType.HOTEL_BOOKING -> Icons.Filled.Hotel
    DocumentType.HEALTH_INSURANCE -> Icons.Filled.LocalHospital
    DocumentType.UNKNOWN -> Icons.Filled.InsertDriveFile
}

private fun colorFor(type: DocumentType): Color = when (type) {
    DocumentType.PASSPORT -> Color(0xFF1565C0)
    DocumentType.VISA -> Color(0xFF4CAF50)
    DocumentType.TICKET -> Color(0xFFFF9800)
    DocumentType.HOTEL_BOOKING -> Color(0xFF9C27B0)
    DocumentType.HEALTH_INSURANCE -> Color(0xFFF44336)
    DocumentType.UNKNOWN -> Color.Gray
}

private fun tagIcon(tag: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (tag.lowercase()) {
        "passport" -> androidx.compose.material.icons.Icons.Filled.AccountBox
        "visa" -> androidx.compose.material.icons.Icons.Filled.CreditCard
        "ticket", "flight" -> androidx.compose.material.icons.Icons.Filled.AirplanemodeActive
        "accommodation", "hotel" -> androidx.compose.material.icons.Icons.Filled.Hotel
        "health", "insurance" -> androidx.compose.material.icons.Icons.Filled.LocalHospital
        "travel", "trip" -> androidx.compose.material.icons.Icons.Filled.FlightTakeoff
        "family" -> androidx.compose.material.icons.Icons.Filled.Public
        "business", "work" -> androidx.compose.material.icons.Icons.Filled.Description
        else -> androidx.compose.material.icons.Icons.Filled.Sell
    }
}
