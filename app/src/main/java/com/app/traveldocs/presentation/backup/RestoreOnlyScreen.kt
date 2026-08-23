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
import com.app.traveldocs.data.backup.BackupRestore
import com.app.traveldocs.debug.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreOnlyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf("choose") }
    var resultMessage by remember { mutableStateOf("") }
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
            title = { Text("Backup Password") },
            text = { Column {
                Text("Enter the password used when creating this backup.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = restorePin, onValueChange = { restorePin = it }, label = { Text("Password (required)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            } },
            confirmButton = { TextButton(onClick = {
                showPinDialog = false; state = "running"
                val pin = restorePin; restorePin = ""
                scope.launch { doRestore(context, pendingUri!!, pin) { msg, err -> resultMessage = msg; isError = err; state = "done" } }
            }, enabled = restorePin.isNotBlank()) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Restore") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (state) {
                "choose" -> {
                    Spacer(Modifier.height(32.dp))
                    Text("Restore from Backup", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Pick a backup ZIP file to restore your documents from.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { filePicker.launch("application/zip") }, modifier = Modifier.fillMaxWidth()) { Text("Choose Backup File") }
                    Spacer(Modifier.height(12.dp))
                    Text("You will need the password that was set during backup.", fontSize = 12.sp, color = Color.Gray)
                }
                "running" -> {
                    Spacer(Modifier.height(64.dp))
                    CircularProgressIndicator(Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Restoring...", fontSize = 16.sp)
                    Text("Inspecting backup and restoring files", fontSize = 13.sp, color = Color.Gray)
                }
                "done" -> {
                    Spacer(Modifier.height(32.dp))
                    Text(if (isError) "Restore Failed" else "Restore Complete", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isError) Color(0xFFF44336) else Color(0xFF4CAF50))
                    Spacer(Modifier.height(12.dp))
                    Text(resultMessage, fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }
}

private suspend fun doRestore(context: android.content.Context, uri: Uri, password: String, done: (String, Boolean) -> Unit) = withContext(Dispatchers.IO) {
    try {
        DebugLogger.i("Restore", "=== RESTORE STARTED ===")
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) { done("Cannot read backup file", true); return@withContext }

        val tempZip = java.io.File(context.cacheDir, "restore_temp.zip")
        tempZip.writeBytes(bytes)

        // Inspect first
        val inspection = BackupRestore.inspectBackup(context, tempZip, password)
        if (!inspection.valid) { tempZip.delete(); done("Invalid backup: ${inspection.errorMessage}", true); return@withContext }
        DebugLogger.i("Restore", "Inspection: ${inspection.fileCount} files, ${inspection.totalSizeBytes/1024}KB")

        // Restore
        val result = BackupRestore.restoreFromZip(context, tempZip, password)
        tempZip.delete()

        if (result.success) {
            val report = "Restored ${result.filesRestored} of ${result.filesProcessed} documents.\nApp will restart to load data."
            done(report, false)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
            }, 3000)
        } else {
            done("Failed: ${result.message}", true)
        }
    } catch (e: Exception) { DebugLogger.e("Restore", "Error", e); done("Error: ${e.message}", true) }
}
