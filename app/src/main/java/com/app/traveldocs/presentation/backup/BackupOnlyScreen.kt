package com.app.traveldocs.presentation.backup

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
import com.app.traveldocs.data.backup.BackupManager
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.debug.UsageTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupOnlyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager() }
    var state by remember { mutableStateOf("choose") } // choose, pin, running, done
    var resultMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var backupPin by remember { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { pendingUri = uri; state = "pin" }
    }

    // PIN dialog
    if (state == "pin") {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { state = "choose" },
            title = { Text("Set Backup Password") },
            text = { Column {
                Text("Protect your backup with a password (min 4 characters).", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = pinInput, onValueChange = { pinInput = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            } },
            confirmButton = { TextButton(onClick = {
                backupPin = pinInput; state = "running"
                scope.launch { doBackup(context, backupManager, pendingUri!!, backupPin) { msg, err -> resultMessage = msg; isError = err; state = "done" } }
            }, enabled = pinInput.length >= 4) { Text("Create Backup") } },
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
                    Text("Creates an encrypted ZIP file with all your documents and metadata.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("Choose Backup Location") }
                    Spacer(Modifier.height(12.dp))
                    Text("The backup will be password-protected. Keep your password safe — it cannot be recovered.", fontSize = 12.sp, color = Color(0xFFF44336))
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

private suspend fun doBackup(context: android.content.Context, mgr: BackupManager, uri: Uri, pin: String, done: (String, Boolean) -> Unit) = withContext(Dispatchers.IO) {
    try {
        DebugLogger.i("Backup", "=== BACKUP STARTED ===")
        UsageTelemetry.funnelStart("backup_local")
        val r = mgr.createBackupZip(context, pin)
        val folder = DocumentFile.fromTreeUri(context, uri)
        val out = folder?.createFile("application/zip", mgr.suggestedFileName())
        if (out != null) {
            context.contentResolver.openOutputStream(out.uri)?.use { os -> r.zipFile.inputStream().use { it.copyTo(os) } }
            r.zipFile.delete()
            UsageTelemetry.funnelComplete("backup_local")
            val report = "Backed up ${r.fileCount} documents (${r.totalBytes / 1024}KB)\nSaved to: ${folder.name}/"
            DebugLogger.i("Backup", report)
            done(report, false)
        } else { done("Failed to create file in folder", true) }
    } catch (e: Exception) { DebugLogger.e("Backup", "Failed", e); done("Error: ${e.message}", true) }
}
