package com.app.traveldocs.presentation

import java.io.File
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.traveldocs.debug.DebugLogScreen
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.debug.SystemTelemetry
import com.app.traveldocs.debug.TempFileCleanup
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.presentation.diagnostics.DiagnosticsScreen
import com.app.traveldocs.presentation.documents.DocumentListScreen
import com.app.traveldocs.presentation.documents.DocumentListViewModel
import com.app.traveldocs.presentation.documents.DocumentViewerScreen
import com.app.traveldocs.presentation.documents.ImportScreen
import com.app.traveldocs.presentation.search.SearchScreen
import com.app.traveldocs.presentation.settings.SettingsScreen
import com.app.traveldocs.presentation.tags.TagManagementScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.CheckCircle
import com.app.traveldocs.presentation.documents.ImportViewModel
import com.app.traveldocs.BuildConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    private lateinit var telemetry: SystemTelemetry

    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { it }) { telemetry.captureSnapshot("permission_granted"); telemetry.startLocationPolling() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        telemetry = SystemTelemetry(applicationContext)
        DebugLogger.i("MainActivity", "onCreate")
        com.app.traveldocs.debug.UsageTelemetry.resetSession()
        telemetry.captureSnapshot("onCreate")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else { telemetry.startLocationPolling() }

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
                !splashPrefs.getBoolean("skip_splash", false) -> "splash"
                !disclaimerPrefs.getBoolean("disclaimer_accepted", false) -> "disclaimer"
                else -> "pin"
            }
            var screen by remember { mutableStateOf(initialScreen) }
            var showDebugLogs by remember { mutableStateOf(false) }
            var selectedDoc by remember { mutableStateOf<Document?>(null) }
            var currentDocList by remember { mutableStateOf<List<Document>>(emptyList()) }
            // Dynamic dark/light theme - reads from SharedPreferences, updates immediately
            val darkMode = remember { mutableStateOf(getSharedPreferences("app_settings", MODE_PRIVATE).getBoolean("dark_mode", false)) }
            val colorScheme = if (darkMode.value) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                when {
                    showDebugLogs -> DebugLogScreen(onClose = { showDebugLogs = false })
                    screen == "eula" -> com.app.traveldocs.presentation.onboarding.EulaScreen(
                        onAccepted = {
                            val prefs = getSharedPreferences("eula_prefs", MODE_PRIVATE)
                            val timestamp = System.currentTimeMillis()
                            val location = try {
                                val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
                                @Suppress("MissingPermission")
                                val loc = lm.getLastKnownLocation(android.location.LocationManager.FUSED_PROVIDER)
                                    ?: lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                if (loc != null) "${loc.latitude}, ${loc.longitude}" else "Not available"
                            } catch (_: Exception) { "Not available" }
                            prefs.edit()
                                .putBoolean("eula_accepted", true)
                                .putLong("accepted_timestamp", timestamp)
                                .putString("accepted_location", location)
                                .apply()
                            com.app.traveldocs.debug.DebugLogger.i("EULA", "Accepted at $timestamp, location=$location")
                            screen = if (!splashPrefs.getBoolean("skip_splash", false)) "splash"
                                     else if (!disclaimerPrefs.getBoolean("disclaimer_accepted", false)) "disclaimer"
                                     else "pin"
                        },
                        onDeclined = {
                            com.app.traveldocs.debug.DebugLogger.w("EULA", "Declined — closing app")
                            finishAffinity()
                        }
                    )
                    screen == "splash" -> com.app.traveldocs.presentation.onboarding.SplashScreen(onContinue = { skipInFuture ->
                        if (skipInFuture) { getSharedPreferences("splash_prefs", MODE_PRIVATE).edit().putBoolean("skip_splash", true).apply() }
                        screen = if (!disclaimerPrefs.getBoolean("disclaimer_accepted", false)) "disclaimer"
                                 else "pin"
                    })
                    screen == "disclaimer" -> com.app.traveldocs.presentation.onboarding.DisclaimerScreen(onAccepted = { telemetryConsent ->
                        getSharedPreferences("disclaimer_prefs", MODE_PRIVATE).edit().putBoolean("disclaimer_accepted", true).putLong("accepted_timestamp", System.currentTimeMillis()).putBoolean("telemetry_consented", telemetryConsent).apply()
                        com.app.traveldocs.debug.UsageTelemetry.setConsent(telemetryConsent)
                        screen = "consent"
                    })

                    screen == "pin" -> {
                        val fa = this@MainActivity as androidx.fragment.app.FragmentActivity
                        com.app.traveldocs.presentation.auth.BiometricAuthScreen(
                            activity = fa,
                            onAuthenticated = { screen = if (sharedUri != null) "import_shared" else "main" },
                            onSkipped = { screen = if (sharedUri != null) "import_shared" else "main" }
                        )
                    }
                    screen == "import_shared" -> {
                        // Received a doc from another app — auto-import it
                        val importVm: com.app.traveldocs.presentation.documents.ImportViewModel = hiltViewModel()
                        LaunchedEffect(sharedUri) {
                            if (sharedUri != null) {
                                com.app.traveldocs.debug.DebugLogger.i("ShareTarget", "Auto-importing shared doc: $sharedUri")
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
                    screen == "import" -> ImportScreen(onDone = { screen = "main" })
                    screen == "documents" -> DocumentListScreen(onBack = { screen = "main" }, onDocumentClick = { doc -> selectedDoc = doc; screen = "viewer" }, onSetDocList = { currentDocList = it })
                    screen == "viewer" && selectedDoc != null -> {
                        val currentIdx = currentDocList.indexOfFirst { it.id == selectedDoc!!.id }
                        DocumentViewerScreen(
                            document = selectedDoc!!,
                            onBack = { screen = "documents" },
                            onPrev = if (currentIdx > 0) { { selectedDoc = currentDocList[currentIdx - 1] } } else null,
                            onNext = if (currentIdx < currentDocList.size - 1) { { selectedDoc = currentDocList[currentIdx + 1] } } else null
                        )
                    }
                    screen == "search" -> SearchScreen(onBack = { screen = "main" }, onDocumentClick = { doc -> selectedDoc = doc; screen = "viewer" })
                    screen == "tags" -> TagManagementScreen(onBack = { screen = "main" })
                    screen == "settings" -> SettingsScreen(onBack = { screen = "main" })
                    screen == "about" -> com.app.traveldocs.presentation.about.AboutScreen(onBack = { screen = "main" })
                    screen == "eula_view" -> com.app.traveldocs.presentation.onboarding.EulaViewScreen(onBack = { screen = "main" })
                    screen == "review" -> com.app.traveldocs.presentation.review.ReviewScreen(onBack = { screen = "main" })
                    screen == "webshare" -> com.app.traveldocs.presentation.webshare.WebShareScreen(onBack = { screen = "main" })
                    screen == "feedback" -> {
                        // Launch GitHub Issues in browser for feedback
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/sethusrinivasan/document-manager/issues"))
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { this@MainActivity.startActivity(intent) } catch (_: Exception) {}
                        screen = "main"
                    }
                    screen == "backup" -> com.app.traveldocs.presentation.backup.BackupOnlyScreen(onBack = { screen = "main" })
                    screen == "restore" -> com.app.traveldocs.presentation.backup.RestoreOnlyScreen(onBack = { screen = "main" })
                    screen == "diagnostics" -> DiagnosticsScreen(onBack = { screen = "main" }, onViewLogs = { showDebugLogs = true })
                    else -> MainScreen(
                        onImport = { com.app.traveldocs.debug.UsageTelemetry.action("Main", "tap_import"); screen = "import" },
                        onAllDocs = { com.app.traveldocs.debug.UsageTelemetry.action("Main", "tap_documents"); screen = "documents" },
                        onSearch = { com.app.traveldocs.debug.UsageTelemetry.action("Main", "tap_search"); screen = "search" },
                        onTags = { screen = "tags" },
                        onSettings = { screen = "settings" },
                        onDiagnostics = { screen = "diagnostics" },
                        onBackup = { screen = "backup" },
                        onRestore = { screen = "restore" },
                        onAbout = { screen = "about" },
                        onFeedback = { screen = "feedback" },
                        onReview = { screen = "review" },
                        onWebShare = { screen = "webshare" },
                        onDocClick = { doc -> selectedDoc = doc; screen = "viewer" },
                        onReset = { resetApp() }
                    )
                }
            }
        }
    }

    override fun onResume() { super.onResume(); telemetry.captureSnapshot("onResume"); telemetry.startLocationPolling(); Thread { TempFileCleanup.cleanSharedDocs(applicationContext) }.start() }
    override fun onPause() { super.onPause(); com.app.traveldocs.debug.UsageTelemetry.emitSessionSummary() }
    override fun onDestroy() { super.onDestroy(); telemetry.stopLocationPolling() }

    private fun resetApp() {
        DebugLogger.w("App", "!!! FACTORY RESET triggered by user")
        // Archive old database (keep last 10 versions), then create fresh
        val dbPath = getDatabasePath("traveldocs.db")
        if (dbPath.exists()) {
            // Rotate archives: .010 -> delete, .009 -> .010, ... .001 -> .002, current -> .001
            val archiveDir = java.io.File(filesDir, "db_archive")
            archiveDir.mkdirs()
            for (i in 10 downTo 2) {
                val older = java.io.File(archiveDir, "traveldocs.db.${String.format("%03d", i - 1)}")
                val newer = java.io.File(archiveDir, "traveldocs.db.${String.format("%03d", i)}")
                if (older.exists()) older.renameTo(newer)
            }
            val archive001 = java.io.File(archiveDir, "traveldocs.db.001")
            dbPath.copyTo(archive001, overwrite = true)
            DebugLogger.i("Reset", "Archived DB as ${archive001.name} (${archive001.length()/1024}KB)")
        }
        // Checkpoint WAL → flush all pending transactions into main DB before archiving
        try {
            val flushDb = android.database.sqlite.SQLiteDatabase.openDatabase(dbPath.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
            flushDb.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
            flushDb.close()
            DebugLogger.i("Reset", "WAL checkpoint complete — all transactions flushed to main DB")
        } catch (e: Exception) {
            DebugLogger.w("Reset", "WAL checkpoint failed (DB may already be deleted): ${e.message}")
        }
        // Now safe to delete — no uncommitted data in WAL
        deleteDatabase("traveldocs.db")
        java.io.File(dbPath.path + "-wal").delete()
        java.io.File(dbPath.path + "-shm").delete()
        // Also archive GPS DB
        val gpsDb = getDatabasePath("gps_tracks.db")
        if (gpsDb.exists()) gpsDb.delete()
        // Clear ALL shared preferences
        val prefsToWipe = listOf("traveldocs_stats", "encryption_consent", "disclaimer_prefs",
            "splash_prefs", "eula_prefs", "location_tracking_prefs", "feature_flags",
            "app_settings", "tag_colors", "secure_doc_pins", "security_alert_prefs")
        prefsToWipe.forEach { getSharedPreferences(it, MODE_PRIVATE).edit().clear().commit() }
        // Delete all document files
        java.io.File(filesDir, "docs").deleteRecursively()
        // Delete debug logs
        java.io.File(filesDir, "debug_logs").deleteRecursively()
        // Delete crash reports
        java.io.File(filesDir, "last_crash_report.txt").delete()
        // Delete cache
        cacheDir.deleteRecursively()
        DebugLogger.w("App", "Reset complete. Killing process.")
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onImport: () -> Unit, onAllDocs: () -> Unit, onSearch: () -> Unit,
    onTags: () -> Unit, onSettings: () -> Unit, onDiagnostics: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit, onAbout: () -> Unit, onFeedback: () -> Unit, onReview: () -> Unit, onWebShare: () -> Unit,
    onDocClick: (Document) -> Unit, onReset: () -> Unit
) {
    val viewModel: DocumentListViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val documents by viewModel.documents.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()
    var showGearMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    var resetStep by remember { mutableIntStateOf(0) } // 0=hidden, 1=first warning, 2=final confirm
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false; resetStep = 0 },
            icon = { Icon(Icons.Filled.RestartAlt, null, tint = Color(0xFFF44336)) },
            title = { Text(if (resetStep < 2) "Reset App?" else "ARE YOU SURE?") },
            text = { Column {
                if (resetStep < 2) {
                    Text("This will PERMANENTLY DELETE:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("• All imported documents (encrypted files)", fontSize = 13.sp)
                    Text("• All tags and metadata", fontSize = 13.sp)
                    Text("• All settings and preferences", fontSize = 13.sp)
                    Text("• EULA acceptance (will show again)", fontSize = 13.sp)
                    Text("• Debug logs and telemetry", fontSize = 13.sp)
                    Text("• PIN-protected documents (UNRECOVERABLE)", fontSize = 13.sp, color = Color(0xFFF44336))
                    Spacer(Modifier.height(12.dp))
                    Text("This CANNOT be undone. The app will restart as freshly installed.", fontWeight = FontWeight.Bold, color = Color(0xFFF44336), fontSize = 13.sp)
                } else {
                    Text("Last chance. Tap 'Delete Everything' to proceed.", fontSize = 14.sp, color = Color(0xFFF44336))
                    Spacer(Modifier.height(8.dp))
                    Text("All data will be permanently erased.", fontSize = 13.sp)
                }
            } },
            confirmButton = {
                if (resetStep < 2) {
                    TextButton(onClick = { resetStep = 2 }) { Text("I understand, continue", color = Color(0xFFF44336)) }
                } else {
                    TextButton(onClick = { showResetDialog = false; resetStep = 0; onReset() }) { Text("DELETE EVERYTHING", color = Color(0xFFF44336), fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false; resetStep = 0 }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { val customTitle = remember { context.getSharedPreferences("app_settings", 0).getString("home_title", "My Private Documents") ?: "My Private Documents" }; Text(customTitle) },
                actions = {
                    IconButton(onClick = { isRefreshing = true; viewModel.forceRefresh(); refreshScope.launch { kotlinx.coroutines.delay(800); isRefreshing = false } }) { Icon(Icons.Filled.Refresh, "Refresh") }
                    Box {
                        IconButton(onClick = { showGearMenu = true }) { Icon(Icons.Filled.Settings, "More") }
                        DropdownMenu(expanded = showGearMenu, onDismissRequest = { showGearMenu = false }) {
                            DropdownMenuItem(text = { Text("About") }, onClick = { showGearMenu = false; onAbout() })
                            DropdownMenuItem(text = { Text("Feedback") }, onClick = { showGearMenu = false; onFeedback() })
                            DropdownMenuItem(text = { Text("Backup") }, onClick = { showGearMenu = false; onBackup() })
                            if (BuildConfig.DEBUG || com.app.traveldocs.data.local.FeatureFlags.isExperimentalEnabled(context)) DropdownMenuItem(text = { Text("Diagnostics") }, onClick = { showGearMenu = false; onDiagnostics() })
                            DropdownMenuItem(text = { Text("Restore") }, onClick = { showGearMenu = false; onRestore() })
                            DropdownMenuItem(text = { Text("Review & Classify") }, onClick = { showGearMenu = false; onReview() })
                            if (com.app.traveldocs.data.local.FeatureFlags.isExperimentalEnabled(context) && com.app.traveldocs.data.local.FeatureFlags.isWifiShareEnabled(context)) DropdownMenuItem(text = { Text("WiFi Share") }, onClick = { showGearMenu = false; onWebShare() })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { showGearMenu = false; onSettings() })
                            DropdownMenuItem(text = { Text("Tags") }, onClick = { showGearMenu = false; onTags() })
                            DropdownMenuItem(text = { Text("Reset App", color = Color(0xFFF44336)) }, onClick = { showGearMenu = false; showResetDialog = true })
                        }
                    }                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // Pull to refresh indicator
            if (isRefreshing) { androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) }
            androidx.compose.material3.TextButton(onClick = { isRefreshing = true; refreshScope.launch { delay(1000); isRefreshing = false } }, modifier = Modifier.fillMaxWidth().height(4.dp)) {}            // Action buttons row
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

            // Document count

            // Folder view by tags
            // Filter out the internal __PIN_PROTECTED tag from visible folders
            val allTags = documents.flatMap { it.tags }.map { it.name }.filter { it != "__PIN_PROTECTED" }.distinct().sorted()
            val untaggedDocs = documents.filter { it.tags.isEmpty() || it.tags.all { t -> t.name == "__PIN_PROTECTED" } }
            val protectedDocs = documents.filter { doc -> doc.tags.any { it.name == "__PIN_PROTECTED" } }
            var selectedTag by remember { mutableStateOf<String?>(null) }

            if (selectedTag != null) {
                // Show documents in selected folder
                val folderDocs = when (selectedTag) {
                    "__untagged__" -> untaggedDocs
                    "__protected__" -> protectedDocs
                    else -> documents.filter { doc -> doc.tags.any { it.name == selectedTag } }
                }
                val folderTitle = when (selectedTag) {
                    "__untagged__" -> "Untagged"
                    "__protected__" -> "Protected"
                    else -> selectedTag!!
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedTag = null }) { Icon(Icons.Filled.ArrowBack, "Back to folders") }
                    Text(folderTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${folderDocs.size} docs", fontSize = 12.sp, color = Color.Gray)
                }
                LazyColumn {
                    items(folderDocs.take(50)) { doc ->
                        DocCard(doc = doc, onClick = { onDocClick(doc) })
                        Spacer(Modifier.height(6.dp))
                    }
                }
            } else {
                // Show folder grid
                Text("Organized by tags", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                val columns = 3
                val folders = (if (protectedDocs.isNotEmpty()) listOf("__protected__") else emptyList()) + allTags + (if (untaggedDocs.isNotEmpty()) listOf("__untagged__") else emptyList())
                if (folders.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Description, null, Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(12.dp))
                        Text("No documents yet", fontSize = 16.sp, color = Color.Gray)
                        Text("Tap Import to add your first document", fontSize = 13.sp, color = Color.LightGray)
                    }
                } else {
                    LazyColumn {
                        items(folders.chunked(columns).size) { rowIdx ->
                            val rowFolders = folders.chunked(columns)[rowIdx]
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowFolders.forEach { tag ->
                                    val label = when (tag) { "__untagged__" -> "Untagged"; "__protected__" -> "Protected"; else -> tag }
                                    val count = when (tag) { "__untagged__" -> untaggedDocs.size; "__protected__" -> protectedDocs.size; else -> documents.count { d -> d.tags.any { it.name == tag } } }
                                    val isProtected = tag == "__protected__"
                                    val isUntagged = tag == "__untagged__"
                                    Card(
                                        Modifier.weight(1f).clickable { selectedTag = tag },
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(1.dp),
                                        colors = if (isProtected) CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) else CardDefaults.cardColors()
                                    ) {
                                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            val folderIcon = when {
                                                isProtected -> Icons.Filled.Lock
                                                isUntagged -> Icons.Filled.Folder
                                                else -> Icons.Filled.Folder
                                            }
                                            val folderTint = when {
                                                isProtected -> Color(0xFFE65100)
                                                isUntagged -> Color.Gray
                                                else -> Color(0xFF1565C0)
                                            }
                                            Icon(folderIcon, label, tint = folderTint, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.height(4.dp))
                                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                            Text("$count", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                repeat(columns - rowFolders.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }        }
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
            Text("${((doc.extractionConfidence ?: 0f) * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
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
