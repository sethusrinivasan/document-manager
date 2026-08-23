package com.app.traveldocs.presentation.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.app.traveldocs.debug.TrackingSettingsPanel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit, onViewLogs: () -> Unit) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(-1) }
    var isCharging by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf("--") }
    var longitude by remember { mutableStateOf("--") }
    var accuracy by remember { mutableStateOf("--") }
    var isOnline by remember { mutableStateOf(false) }
    var networkType by remember { mutableStateOf("--") }

    @Suppress("MissingPermission")
    LaunchedEffect(Unit) {
        while (true) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            isCharging = bm?.isCharging ?: false
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
            networkType = when { caps == null -> "Offline"; caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"; caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"; else -> "Other" }
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val loc = lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (loc != null) { latitude = "%.6f".format(loc.latitude); longitude = "%.6f".format(loc.longitude); accuracy = "%.0fm".format(loc.accuracy) }
                }
            } catch (_: Exception) {}
            delay(5000)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Diagnostics") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("System Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            StatusRow(if (isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.Battery4Bar, when { batteryLevel > 50 -> Color(0xFF4CAF50); batteryLevel > 20 -> Color(0xFFFFC107); else -> Color(0xFFF44336) }, "Battery", "${batteryLevel}%", if (isCharging) "Charging" else "Discharging")
            Spacer(Modifier.height(8.dp))
            StatusRow(Icons.Filled.LocationOn, Color(0xFF2196F3), "GPS", "$latitude, $longitude", "Accuracy: $accuracy")
            Spacer(Modifier.height(8.dp))
            StatusRow(if (isOnline) Icons.Filled.SignalWifi4Bar else Icons.Filled.SignalWifiOff, if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336), "Network", networkType, if (isOnline) "Connected" else "Offline")
            Spacer(Modifier.height(20.dp))
            Text("GPS Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            TrackingSettingsPanel()
            Spacer(Modifier.height(20.dp))
            Text("Debug Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onViewLogs, modifier = Modifier.fillMaxWidth()) { Text("View Debug Logs") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { shareLogsAsZip(context) }, modifier = Modifier.fillMaxWidth()) { Text("Share Logs (ZIP)") }

            Spacer(Modifier.height(24.dp))
            Text("Database Diagnostics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            var diagResult by remember { mutableStateOf<String?>(null) }
            var diagIssues by remember { mutableStateOf<List<String>>(emptyList()) }
            var repairDone by remember { mutableStateOf(false) }

            Button(onClick = {
                val result = runDiagnostics(context)
                diagResult = result.first
                diagIssues = result.second
                repairDone = false
            }, modifier = Modifier.fillMaxWidth()) { Text("Run Diagnostics") }

            if (diagResult != null) {
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = if (diagIssues.isEmpty()) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                )) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (diagIssues.isEmpty()) "All checks passed" else "${diagIssues.size} issue(s) found",
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            color = if (diagIssues.isEmpty()) Color(0xFF2E7D32) else Color(0xFFE65100))
                        Spacer(Modifier.height(4.dp))
                        Text(diagResult!!, fontSize = 11.sp, color = Color.Gray)
                        if (diagIssues.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            diagIssues.forEach { issue ->
                                Text("• $issue", fontSize = 12.sp, color = Color(0xFFE65100))
                            }
                            Spacer(Modifier.height(8.dp))
                            if (!repairDone) {
                                Button(onClick = {
                                    repairDatabase(context)
                                    repairDone = true
                                    val recheck = runDiagnostics(context)
                                    diagResult = recheck.first
                                    diagIssues = recheck.second
                                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                                    modifier = Modifier.fillMaxWidth()) { Text("Repair") }
                            } else {
                                Text("Repair attempted. Re-run diagnostics to verify.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(icon: ImageVector, iconColor: Color, title: String, value: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = iconColor, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontSize = 11.sp, color = Color.Gray); Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 10.sp, color = Color.Gray) }
        }
    }
}


private fun shareLogsAsZip(context: android.content.Context) {
    try {
        val logDir = java.io.File(context.filesDir, "debug_logs")
        val logFile = java.io.File(logDir, "traveldocs_debug.log")
        if (!logFile.exists()) { com.app.traveldocs.debug.DebugLogger.w("Diagnostics", "No log file to share"); return }

        // Create ZIP in cache
        val cacheDir = java.io.File(context.cacheDir, "shared_docs")
        cacheDir.mkdirs()
        val zipFile = java.io.File(cacheDir, "document_manager_logs.zip")
        java.util.zip.ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("debug_log.txt"))
            logFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            // Also include telemetry summary if available
            val telemetry = com.app.traveldocs.debug.UsageTelemetry.getLocalSummary()
            zip.putNextEntry(java.util.zip.ZipEntry("telemetry_summary.txt"))
            zip.write(telemetry.toByteArray())
            zip.closeEntry()
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Document Manager - Debug Logs")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val resInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (ri in resInfoList) { context.grantUriPermission(ri.activityInfo.packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(android.content.Intent.createChooser(intent, "Share logs via").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        com.app.traveldocs.debug.DebugLogger.i("Diagnostics", "Logs ZIP shared (${zipFile.length() / 1024}KB)")
    } catch (e: Exception) {
        com.app.traveldocs.debug.DebugLogger.e("Diagnostics", "Failed to share logs ZIP", e)
    }
}


private fun runDiagnostics(context: android.content.Context): Pair<String, List<String>> {
    val issues = mutableListOf<String>()
    val report = StringBuilder()

    try {
        val dbFile = context.getDatabasePath("traveldocs.db")
        if (!dbFile.exists()) {
            return Pair("Database file not found", listOf("traveldocs.db does not exist"))
        }
        report.appendLine("DB file: ${dbFile.length() / 1024}KB")

        val db = android.database.sqlite.SQLiteDatabase.openDatabase(dbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)

        // Check required tables exist
        val requiredTables = listOf("documents", "document_metadata", "document_tags", "family_members", "gps_tracks")
        val existingTables = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        while (cursor.moveToNext()) { existingTables.add(cursor.getString(0)) }
        cursor.close()

        for (table in requiredTables) {
            if (table !in existingTables) {
                issues.add("Missing table: $table")
            }
        }
        report.appendLine("Tables: ${existingTables.filter { !it.startsWith("sqlite_") && !it.startsWith("room_") && !it.startsWith("android_") }.joinToString(", ")}")

        // Count records in each table
        for (table in requiredTables) {
            if (table in existingTables) {
                val countCursor = db.rawQuery("SELECT COUNT(*) FROM $table", null)
                val count = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
                countCursor.close()
                report.appendLine("$table: $count rows")
            }
        }

        // Check DB version
        val vCursor = db.rawQuery("PRAGMA user_version", null)
        val version = if (vCursor.moveToFirst()) vCursor.getInt(0) else 0
        vCursor.close()
        report.appendLine("DB version: $version (app expects: 2)")
        if (version != 2) {
            issues.add("DB version mismatch: found $version, expected 2")
        }

        // Check integrity
        val intCursor = db.rawQuery("PRAGMA integrity_check", null)
        val integrity = if (intCursor.moveToFirst()) intCursor.getString(0) else "unknown"
        intCursor.close()
        report.appendLine("Integrity: $integrity")
        if (integrity != "ok") {
            issues.add("Database integrity check failed: $integrity")
        }

        // Check for orphaned files (files on disk with no DB entry)
        val docsDir = java.io.File(context.filesDir, "docs")
        if (docsDir.exists()) {
            val fileCount = docsDir.walkTopDown().filter { it.isFile }.count()
            val docsCursor = db.rawQuery("SELECT COUNT(*) FROM documents", null)
            val dbCount = if (docsCursor.moveToFirst()) docsCursor.getInt(0) else 0
            docsCursor.close()
            report.appendLine("Files on disk: $fileCount, DB entries: $dbCount")
            if (fileCount != dbCount) {
                issues.add("File/DB mismatch: $fileCount files on disk, $dbCount in database")
            }
        }

        db.close()
        com.app.traveldocs.debug.DebugLogger.i("Diagnostics", report.toString())
    } catch (e: Exception) {
        issues.add("Diagnostics error: ${e.message}")
        com.app.traveldocs.debug.DebugLogger.e("Diagnostics", "Failed", e)
    }

    return Pair(report.toString(), issues)
}

private fun repairDatabase(context: android.content.Context) {
    com.app.traveldocs.debug.DebugLogger.i("Diagnostics", "=== REPAIR STARTED ===")
    try {
        val dbFile = context.getDatabasePath("traveldocs.db")
        if (!dbFile.exists()) {
            com.app.traveldocs.debug.DebugLogger.w("Diagnostics", "No DB file — nothing to repair")
            return
        }

        val db = android.database.sqlite.SQLiteDatabase.openDatabase(dbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)

        // Create missing tables
        db.execSQL("CREATE TABLE IF NOT EXISTS documents (id TEXT NOT NULL PRIMARY KEY, memberId TEXT NOT NULL, type TEXT NOT NULL, fileId TEXT NOT NULL, format TEXT NOT NULL, originalFileName TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, extractionConfidence REAL, requiresManualReview INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS document_metadata (id INTEGER PRIMARY KEY AUTOINCREMENT, documentId TEXT NOT NULL, field TEXT NOT NULL, value TEXT NOT NULL, confidence REAL NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS document_tags (documentId TEXT NOT NULL, tag TEXT NOT NULL, isAutoGenerated INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(documentId, tag))")
        db.execSQL("CREATE TABLE IF NOT EXISTS family_members (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, pinHash TEXT NOT NULL, pinSalt TEXT NOT NULL, createdAt INTEGER NOT NULL, failedAttempts INTEGER NOT NULL DEFAULT 0, lockedUntil INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS gps_tracks (id INTEGER PRIMARY KEY AUTOINCREMENT, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracy REAL NOT NULL, timestamp INTEGER NOT NULL, provider TEXT NOT NULL)")

        // Ensure correct version
        db.execSQL("PRAGMA user_version = 2")

        db.close()
        com.app.traveldocs.debug.DebugLogger.i("Diagnostics", "Repair complete — tables verified/created, version set to 2")
        com.app.traveldocs.debug.UsageTelemetry.action("Diagnostics", "repair_executed")
    } catch (e: Exception) {
        com.app.traveldocs.debug.DebugLogger.e("Diagnostics", "Repair failed", e)
    }
}
