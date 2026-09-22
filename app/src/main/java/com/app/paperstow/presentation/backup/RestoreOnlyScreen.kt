package com.app.paperstow.presentation.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.paperstow.data.backup.BackupRestore
import com.app.paperstow.debug.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreOnlyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf("choose") }
    var logLines by remember { mutableStateOf(listOf<String>()) }
    var isError by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var restorePin by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { pendingUri = uri; showPinDialog = true }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Backup password") },
            text = { Column {
                Text("Only needed if this backup was created with a password. Leave it blank to skip.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = restorePin,
                    onValueChange = { restorePin = it },
                    label = { Text("Password (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            } },
            confirmButton = { TextButton(onClick = {
                showPinDialog = false; state = "running"; logLines = listOf("Starting restore...")
                val pin = restorePin.trim().ifEmpty { null }; restorePin = ""
                scope.launch {
                    val ok = doRestoreVerbose(context, pendingUri!!, pin) { line ->
                        logLines = logLines + line
                    }
                    isError = !ok
                    state = "done"
                }
            }) { Text(if (restorePin.isBlank()) "Restore without password" else "Restore") } },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Restore") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (state) {
                "choose" -> {
                    Spacer(Modifier.height(32.dp))
                    Text("Restore from Backup", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Pick a backup ZIP file to restore your documents from.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { filePicker.launch("application/zip") }, modifier = Modifier.fillMaxWidth()) { Text("Choose Backup File") }
                    Spacer(Modifier.height(12.dp))
                    Text("A password is only needed if one was set when the backup was created.", fontSize = 12.sp, color = Color.Gray)
                }
                "running", "done" -> {
                    // Verbose log output
                    Text(
                        if (state == "running") "Restoring..."
                        else if (isError || logLines.any { it.startsWith("ERROR") || it.contains("STATUS: FAILED") }) "Restore Failed"
                        else "Restore Complete",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state == "done" && (isError || logLines.any { it.startsWith("ERROR") || it.contains("STATUS: FAILED") })) Color(0xFFF44336)
                        else if (state == "done") Color(0xFF4CAF50)
                        else Color.Unspecified
                    )
                    Spacer(Modifier.height(8.dp))

                    if (state == "running") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)) }

                    Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                            logLines.forEach { line ->
                                val color = when {
                                    line.startsWith("ERROR") || line.startsWith("FAIL") -> Color(0xFFF44336)
                                    line.startsWith("OK") || line.startsWith("SUCCESS") -> Color(0xFF4CAF50)
                                    line.startsWith("WARN") -> Color(0xFFE65100)
                                    else -> Color(0xFF424242)
                                }
                                Text(line, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = color)
                            }
                        }
                    }

                    if (state == "done") {
                        Spacer(Modifier.height(12.dp))
                        if (isError) {
                            Text("Nothing was marked complete. If the backup itself is empty or corrupt, this phone’s current papers were left as they were.", fontSize = 13.sp, color = Color(0xFFF44336))
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Go Back") }
                        } else {
                            Text("Restart the app so it can open the restored papers.", fontSize = 12.sp, color = Color(0xFFF44336), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Restart App Now") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Go Back (papers won’t show until restart)") }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun doRestoreVerbose(context: android.content.Context, uri: Uri, password: String?, log: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
    try {
        log("Reading backup file...")
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) { log("ERROR: Cannot read backup file"); return@withContext false }
        log("OK: Read ${bytes.size / 1024}KB from source")

        val tempZip = java.io.File(context.cacheDir, "restore_temp.zip")
        tempZip.writeBytes(bytes)
        log("Wrote temp file for processing")

        // Step 1: Inspect
        log("")
        log("--- STEP 1: Inspecting backup ---")
        val inspection = BackupRestore.inspectBackup(context, tempZip, password)
        if (!inspection.valid) {
            log("ERROR: ${inspection.errorMessage}")
            tempZip.delete()
            return@withContext false
        }
        log("OK: Schema version ${inspection.schemaVersion}")
        log("OK: Backup timestamp: ${inspection.timestamp}")
        log("OK: Expected files: ${inspection.fileCount}")
        log("OK: Total size: ${inspection.totalSizeBytes / 1024}KB")
        if (inspection.files.isNotEmpty()) {
            log("Files in backup:")
            inspection.files.take(20).forEach { f -> log("  ${f.path} (${f.size/1024}KB)") }
            if (inspection.files.size > 20) log("  ... and ${inspection.files.size - 20} more")
        }

        // Step 2: Restore
        log("")
        log("--- STEP 2: Restoring ---")
        val result = BackupRestore.restoreFromZip(context, tempZip, password)
        tempZip.delete()

        // Step 3: Report
        log("")
        log("--- STEP 3: File Verification ---")
        log("Processed: ${result.filesProcessed}")
        log("Restored: ${result.filesRestored}")
        log("Failed verification: ${result.filesFailedVerification}")
        if (result.filesRestored == result.filesProcessed) {
            log("OK: All ${result.filesRestored} document files restored")
        } else {
            log("WARN: ${result.filesProcessed - result.filesRestored} files not restored")
        }

        log("")
        log("--- STEP 4: Database check ---")
        val dbCheck = com.app.paperstow.data.backup.RoomDbFiles.inspect(
            com.app.paperstow.data.backup.RoomDbFiles.liveFile(context)
        )
        if (!dbCheck.hasDocumentsTable) {
            log("ERROR: documents table not found — ${dbCheck.error ?: dbCheck.tables}")
            log("STATUS: FAILED — backup database is empty or corrupt")
            DebugLogger.e("Restore", "Post-restore DB missing documents table")
            return@withContext false
        }
        log("OK: documents table present (${dbCheck.documentCount} rows, v${dbCheck.userVersion})")
        log("Tables: ${dbCheck.tables.joinToString()}")

        val ok = result.success
        log("")
        log(if (ok) "STATUS: COMPLETE" else "STATUS: FAILED — ${result.message}")
        DebugLogger.i("Restore", "Verbose restore finished ok=$ok ${result.filesRestored}/${result.filesProcessed}")
        return@withContext ok
    } catch (e: Exception) {
        log("ERROR: ${e.message}")
        DebugLogger.e("Restore", "Verbose restore failed", e)
        return@withContext false
    }
}
