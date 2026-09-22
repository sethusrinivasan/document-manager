package com.app.paperstow.presentation.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.app.paperstow.data.backup.BackupManager
import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.debug.UsageTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupOnlyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager() }
    var state by remember { mutableStateOf("choose") } // choose, password, running, done
    var resultMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var backupPin by remember { mutableStateOf("") }
    var includeTrail by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { pendingUri = uri; state = "password" }
    }

    if (state == "password") {
        var pinInput by remember { mutableStateOf("") }
        val pinReady = pinInput.isBlank() || pinInput.length >= 4
        fun startBackup(pin: String) {
            backupPin = pin
            val trail = includeTrail
            state = "running"
            scope.launch { doBackup(context, backupManager, pendingUri!!, backupPin, trail) { msg, err -> resultMessage = msg; isError = err; state = "done" } }
        }
        AlertDialog(
            onDismissRequest = { state = "choose" },
            title = { Text("Backup password") },
            text = { Column {
                Text("A password is optional. If you set one, you will need it to restore. Leave it blank to skip.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    label = { Text("Password (optional)") },
                    supportingText = {
                        if (pinInput.isNotBlank() && pinInput.length < 4) Text("At least 4 characters if you set one")
                    },
                    isError = pinInput.isNotBlank() && pinInput.length < 4,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeTrail, onCheckedChange = { includeTrail = it })
                    Text("Also include My Trail as a .gpx file", fontSize = 13.sp)
                }
            } },
            confirmButton = {
                TextButton(onClick = { startBackup(pinInput.trim()) }, enabled = pinReady) {
                    Text(if (pinInput.isBlank()) "Create without password" else "Create Backup")
                }
            },
            dismissButton = { TextButton(onClick = { state = "choose" }) { Text("Cancel") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Backup") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (state) {
                "choose" -> {
                    Spacer(Modifier.height(32.dp))
                    Text("Backup Documents", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Saves a ZIP of your papers to a folder you pick. You can also add My Trail as a .gpx file.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("Choose Backup Location") }
                    Spacer(Modifier.height(12.dp))
                    Text("A password is optional. If you set one, keep it safe — it cannot be recovered.", fontSize = 12.sp, color = Color.Gray)
                }
                "running" -> {
                    Spacer(Modifier.height(64.dp))
                    CircularProgressIndicator(Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Creating backup...", fontSize = 16.sp)
                }
                "done" -> {
                    Spacer(Modifier.height(32.dp))
                    Text(if (isError) "Backup Failed" else "Backup Complete", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isError) Color(0xFFF44336) else Color(0xFF4CAF50))
                    Spacer(Modifier.height(12.dp))
                    Text(resultMessage, fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }
}

private suspend fun doBackup(context: android.content.Context, mgr: BackupManager, uri: Uri, pin: String, includeTrail: Boolean, done: (String, Boolean) -> Unit) = withContext(Dispatchers.IO) {
    try {
        DebugLogger.i("Backup", "=== BACKUP STARTED ===")
        UsageTelemetry.funnelStart("backup_local")
        val password = pin.trim().ifEmpty { null }
        val r = mgr.createBackupZip(context, password, includeTrailGpx = includeTrail)
        val folder = DocumentFile.fromTreeUri(context, uri)
        val out = folder?.createFile("application/zip", mgr.suggestedFileName())
        if (out != null) {
            context.contentResolver.openOutputStream(out.uri)?.use { os -> r.zipFile.inputStream().use { it.copyTo(os) } }
            r.zipFile.delete()
            UsageTelemetry.funnelComplete("backup_local")
            val protection = if (password == null) "No password" else "Password-protected"
            val report = "Backed up ${r.fileCount} documents (${r.totalBytes / 1024}KB)\n$protection\nSaved to: ${folder.name}/"
            DebugLogger.i("Backup", report)
            done(report, false)
        } else { done("Failed to create file in folder", true) }
    } catch (e: Exception) { DebugLogger.e("Backup", "Failed", e); done("Error: ${e.message}", true) }
}
