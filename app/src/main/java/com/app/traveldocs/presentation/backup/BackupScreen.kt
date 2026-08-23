package com.app.traveldocs.presentation.backup

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.app.traveldocs.data.backup.BackupManager
import com.app.traveldocs.data.backup.BackupRestore
import com.app.traveldocs.data.backup.S3BackupUploader
import com.app.traveldocs.data.backup.S3Config
import com.app.traveldocs.data.drive.GoogleDriveServiceProvider
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.debug.UsageTelemetry
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.FileContent
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.api.services.drive.model.File as DriveFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager() }
    var screenState by remember { mutableStateOf("choose") }
    var resultMessage by remember { mutableStateOf("") }
    var resultIsError by remember { mutableStateOf(false) }
    var showS3Dialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var backupPin by remember { mutableStateOf("") }
    var pendingBackupUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestorePinDialog by remember { mutableStateOf(false) }
    var restorePin by remember { mutableStateOf("") }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            pendingBackupUri = uri; showPinDialog = true        }
    }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestorePinDialog = true
        }
    }

        // Restore PIN dialog - shown before restore to ask for backup password
    if (showRestorePinDialog && pendingRestoreUri != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRestorePinDialog = false; pendingRestoreUri = null },
            title = { androidx.compose.material3.Text("Backup Password") },
            text = { Column {
                androidx.compose.material3.Text("Enter the backup password to restore.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.Text("All backups are password-protected. This is required.", fontSize = 12.sp, color = Color(0xFFF44336))
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(value = restorePin, onValueChange = { restorePin = it }, label = { androidx.compose.material3.Text("Backup Password (required)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (restorePin.isNotEmpty()) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
            } },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                showRestorePinDialog = false
                val pin = restorePin
                val uri = pendingRestoreUri!!
                restorePin = ""
                pendingRestoreUri = null
                screenState = "running"
                scope.launch { doRestore(context, uri, pin) { msg, err -> resultMessage = msg; resultIsError = err; screenState = "done" } }
            }, enabled = restorePin.isNotBlank()) { androidx.compose.material3.Text("Restore") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showRestorePinDialog = false; pendingRestoreUri = null; restorePin = "" }) { androidx.compose.material3.Text("Cancel") } }
        )
    }

    val driveSignIn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            screenState = "running"
            scope.launch { driveBackup(context, backupManager) { msg, err -> resultMessage = msg; resultIsError = err; screenState = "done" } }
        } else { resultMessage = "Google Sign-In cancelled"; resultIsError = true; screenState = "done" }
    }


    // Backup PIN dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showPinDialog = false }, title = { Text("Set Backup PIN") }, text = {
            Column {
                Text("This PIN protects your backup. You will need it to restore on any device.", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = pinInput, onValueChange = { if (it.length <= 8) pinInput = it.filter { c -> c.isDigit() } }, label = { Text("PIN (4-8 digits)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            }
        }, confirmButton = { TextButton(onClick = {
            backupPin = pinInput; showPinDialog = false; screenState = "running"
            val uri = pendingBackupUri
            if (uri != null) scope.launch { localBackup(context, backupManager, uri, backupPin) { msg, err -> resultMessage = msg; resultIsError = err; screenState = "done" } }
        }, enabled = pinInput.length >= 4) { Text("Create Backup") } }, dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } })
    }    // S3 config dialog
    if (showS3Dialog) {
        var isEmulator by remember { mutableStateOf(true) }
        var usePresigned by remember { mutableStateOf(false) }
        var presignedUrl by remember { mutableStateOf("") }
        var endpoint by remember { mutableStateOf("http://10.0.0.126:9000") }
        var bucket by remember { mutableStateOf("test-backup-bucket") }
        var accessKey by remember { mutableStateOf("testkey") }
        var secretKey by remember { mutableStateOf("testsecret") }
        var region by remember { mutableStateOf("us-east-1") }
        AlertDialog(onDismissRequest = { showS3Dialog = false }, title = { Text("S3 Configuration") }, text = {
            Column {
                Text("Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(selected = isEmulator, onClick = {
                        isEmulator = true
                        endpoint = "http://10.0.0.126:9000"; bucket = "test-backup-bucket"; accessKey = "testkey"; secretKey = "testsecret"; region = "us-east-1"
                    })
                    Text("Local Emulator", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    androidx.compose.material3.RadioButton(selected = !isEmulator, onClick = {
                        isEmulator = false
                        endpoint = "https://s3.amazonaws.com"; bucket = ""; accessKey = ""; secretKey = ""
                    })
                    Text("AWS S3", fontSize = 13.sp)
                }
                if (isEmulator) { Text("Auto-configured for emulator at 10.0.0.126:9000", fontSize = 10.sp, color = Color(0xFF4CAF50)) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Use Presigned URL", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    androidx.compose.material3.Switch(checked = usePresigned, onCheckedChange = { usePresigned = it })
                }
                Spacer(Modifier.height(6.dp))
                if (usePresigned) {
                    Text("Paste a presigned PUT URL.", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(value = presignedUrl, onValueChange = { presignedUrl = it }, label = { Text("Presigned URL") }, singleLine = false, maxLines = 3, modifier = Modifier.fillMaxWidth())
                } else {
                    OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("Endpoint") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = bucket, onValueChange = { bucket = it }, label = { Text("Bucket") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Region") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = accessKey, onValueChange = { accessKey = it }, label = { Text("Access Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = secretKey, onValueChange = { secretKey = it }, label = { Text("Secret Key") }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                }
            }
        }, confirmButton = { TextButton(onClick = {
            showS3Dialog = false; screenState = "running"
            if (usePresigned) {
                scope.launch { presignedUpload(context, backupManager, presignedUrl.trim()) { msg, err -> resultMessage = msg; resultIsError = err; screenState = "done" } }
            } else {
                val cfg = S3Config(endpoint.trim(), bucket.trim(), accessKey.trim(), secretKey.trim(), region.trim())
                scope.launch { s3Backup(context, backupManager, cfg) { msg, err -> resultMessage = msg; resultIsError = err; screenState = "done" } }
            }
        }) { Text("Upload") } }, dismissButton = { TextButton(onClick = { showS3Dialog = false }) { Text("Cancel") } })
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Backup & Restore") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            when (screenState) {
                "choose" -> {
                    Text("Backup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Encrypted files packaged into ZIP. No plaintext leaves device.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    OptionCard(Icons.Filled.Folder, "Local Folder", "Save to phone storage", Color(0xFF4CAF50)) { folderPicker.launch(null) }
                    Spacer(Modifier.height(8.dp))
                    if (com.app.traveldocs.data.local.FeatureFlags.isGoogleDriveEnabled(context)) OptionCard(Icons.Filled.Cloud, "Google Drive", "Sign in and upload to Drive", Color(0xFF1565C0)) {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (account != null) { screenState = "running"; scope.launch { driveBackup(context, backupManager) { msg, err -> resultMessage = msg; resultIsError = err; screenState = "done" } } }
                        else {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(Scope(DriveScopes.DRIVE_FILE)).build()
                            driveSignIn.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (com.app.traveldocs.data.local.FeatureFlags.isS3Enabled(context)) OptionCard(Icons.Filled.Storage, "S3 Compatible", "AWS S3, MinIO, Backblaze B2", Color(0xFFFF9800)) { showS3Dialog = true }
                    Spacer(Modifier.height(24.dp))
                    Text("Restore", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Select a backup ZIP to restore encrypted documents.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    OptionCard(Icons.Filled.RestoreFromTrash, "Restore from Backup", "Pick a backup ZIP file", Color(0xFF795548)) { restorePicker.launch("application/zip") }
                }
                "running" -> Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(Modifier.size(48.dp)); Spacer(Modifier.height(16.dp)); Text("Working...") }
                "done" -> Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (resultIsError) Icons.Filled.Error else Icons.Filled.CheckCircle, null, Modifier.size(64.dp), tint = if (resultIsError) Color(0xFFF44336) else Color(0xFF4CAF50))
                    Spacer(Modifier.height(16.dp)); Text(if (resultIsError) "Failed" else "Done", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp)); Text(resultMessage, fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp)); Button(onClick = { screenState = "choose"; resultIsError = false }) { Text("OK") }
                }
            }
        }
    }
}

private suspend fun localBackup(context: Context, mgr: BackupManager, uri: Uri, pin: String, done: (String, Boolean) -> Unit) = withContext(Dispatchers.IO) {
    try {
        UsageTelemetry.funnelStart("backup_local")
        DebugLogger.i("Backup", "=== LOCAL BACKUP STARTED ===")
        DebugLogger.i("Backup", "Target folder URI: $uri")
        DebugLogger.i("Backup", "PIN protected: ${pin.isNotEmpty()}")

        val r = mgr.createBackupZip(context, if (pin.isNotEmpty()) pin else null)
        DebugLogger.i("Backup", "ZIP created: ${r.fileCount} files, ${r.totalBytes/1024}KB")

        val folder = DocumentFile.fromTreeUri(context, uri)
        val fileName = mgr.suggestedFileName()
        val out = folder?.createFile("application/zip", fileName)
        if (out != null) {
            context.contentResolver.openOutputStream(out.uri)?.use { os -> r.zipFile.inputStream().use { it.copyTo(os) } }
            r.zipFile.delete()
            UsageTelemetry.funnelComplete("backup_local")

            val report = buildString {
                appendLine("=== Backup Report ===")
                appendLine("File: $fileName")
                appendLine("Documents: ${r.fileCount}")
                appendLine("Total size: ${r.totalBytes / 1024}KB")
                appendLine("Protected: ${if (pin.isNotEmpty()) "Yes (PIN-encrypted ZIP)" else "No"}")
                appendLine("Location: ${folder.name}/")
                appendLine("Completed: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            }
            DebugLogger.i("Backup", report)
            done(report, false)
        } else {
            DebugLogger.e("Backup", "Failed to create file in target folder")
            done("Failed to create file in folder", true)
        }
    } catch (e: Exception) {
        DebugLogger.e("Backup", "Backup failed", e)
        done("Backup error: ${e.message}", true)
    }
}

private suspend fun driveBackup(context: Context, mgr: BackupManager, done: (String, Boolean) -> Unit) = withContext(Dispatchers.IO) {
    try {
        UsageTelemetry.funnelStart("backup_drive")
        val provider = GoogleDriveServiceProvider(context)
        val service = provider.getDriveService()
        if (service == null) { done("Not signed in to Google Drive", true); return@withContext }
        val r = mgr.createBackupZip(context)
        val metadata = DriveFile().apply { name = mgr.suggestedFileName(); mimeType = "application/zip" }
        val content = FileContent("application/zip", r.zipFile)
        val uploaded = service.files().create(metadata, content).setFields("id,name").execute()
        r.zipFile.delete()
        DebugLogger.i("DriveBackup", "Uploaded: ${uploaded.name} (id=${uploaded.id})")
        UsageTelemetry.funnelComplete("backup_drive")
        done("Uploaded to Drive: ${uploaded.name}", false)
    } catch (e: Exception) { DebugLogger.e("DriveBackup", "Failed", e); done("Drive error: ${e.message}", true) }
}

private suspend fun s3Backup(context: Context, mgr: BackupManager, config: S3Config, done: (String, Boolean) -> Unit) = withContext(Dispatchers.IO) {
    try {
        UsageTelemetry.funnelStart("backup_s3")
        val r = mgr.createBackupZip(context)
        val result = S3BackupUploader.upload(r.zipFile, mgr.suggestedFileName(), config)
        r.zipFile.delete()
        result.onSuccess { UsageTelemetry.funnelComplete("backup_s3"); done(it, false) }
        result.onFailure { done("S3 error: ${it.message}", true) }
    } catch (e: Exception) { done("S3 error: ${e.message}", true) }
}

private suspend fun doRestore(context: Context, uri: Uri, password: String?, done: (String, Boolean) -> Unit) = withContext(Dispatchers.IO) {
    try {
        DebugLogger.i("Restore", "=== RESTORE STARTED ===")
        DebugLogger.i("Restore", "Source URI: $uri")
        DebugLogger.i("Restore", "Password provided: ${password != null}")

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) { DebugLogger.e("Restore", "Cannot read backup file from URI"); done("Cannot read backup file", true); return@withContext }
        DebugLogger.i("Restore", "Read ${bytes.size / 1024}KB from source")

        val tempZip = java.io.File(context.cacheDir, "restore_temp.zip")
        tempZip.writeBytes(bytes)

        // Step 1: Inspect manifest BEFORE restoring
        DebugLogger.i("Restore", "--- Step 1: Inspecting backup ---")
        val inspection = BackupRestore.inspectBackup(context, tempZip, password)
        if (!inspection.valid) {
            tempZip.delete()
            DebugLogger.e("Restore", "Inspection failed: ${inspection.errorMessage}")
            done("Invalid backup: ${inspection.errorMessage}", true)
            return@withContext
        }
        DebugLogger.i("Restore", "Inspection OK: schema=${inspection.schemaVersion}, files=${inspection.fileCount}, size=${inspection.totalSizeBytes/1024}KB, pinProtected=${inspection.pinProtectedCount}")

        // Step 2: Execute restore
        DebugLogger.i("Restore", "--- Step 2: Restoring files ---")
        val result = BackupRestore.restoreFromZip(context, tempZip, password)
        tempZip.delete()

        // Step 3: Report
        DebugLogger.i("Restore", "--- Step 3: Report ---")
        val report = buildString {
            appendLine("=== Restore Report ===")
            appendLine("Backup: schema v${inspection.schemaVersion}, from ${inspection.timestamp}")
            appendLine("Expected: ${inspection.fileCount} documents (${inspection.totalSizeBytes/1024}KB)")
            appendLine("PIN-protected docs: ${inspection.pinProtectedCount}")
            appendLine("")
            appendLine("Processed: ${result.filesProcessed}")
            appendLine("Restored: ${result.filesRestored}")
            appendLine("Failed: ${result.filesFailedVerification}")
            appendLine("Status: ${if (result.success) "SUCCESS" else "FAILED"}")
            if (result.filesRestored < result.filesProcessed) {
                appendLine("")
                appendLine("WARNING: ${result.filesProcessed - result.filesRestored} of ${result.filesProcessed} document files not restored")
            }
            appendLine("")
            appendLine("Tap Refresh on home page to see restored documents.")
        }
        DebugLogger.i("Restore", report)

        if (result.success) {
            done(report, false)
        } else {
            done("Restore failed: ${result.message}", true)
        }
    } catch (e: Exception) {
        DebugLogger.e("Restore", "Restore exception: ${e.message}", e)
        done("Restore error: ${e.message}", true)
    }
}

private suspend fun presignedUpload(context: android.content.Context, mgr: com.app.traveldocs.data.backup.BackupManager, presignedUrl: String, done: (String, Boolean) -> Unit) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        com.app.traveldocs.debug.DebugLogger.i("S3Presigned", "Uploading to presigned URL")
        com.app.traveldocs.debug.UsageTelemetry.funnelStart("backup_s3_presigned")
        val r = mgr.createBackupZip(context)
        val bytes = r.zipFile.readBytes()
        val conn = java.net.URL(presignedUrl).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Content-Type", "application/zip")
        conn.setRequestProperty("Content-Length", bytes.size.toString())
        conn.doOutput = true
        conn.outputStream.use { it.write(bytes) }
        val code = conn.responseCode
        r.zipFile.delete()
        if (code in 200..299) {
            com.app.traveldocs.debug.UsageTelemetry.funnelComplete("backup_s3_presigned")
            done("Uploaded via presigned URL (HTTP $code)", false)
        } else { done("Upload failed: HTTP $code", true) }
    } catch (e: Exception) { done("Error: ${e.message}", true) }
}

@Composable
private fun OptionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, iconColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    androidx.compose.material3.Card(androidx.compose.ui.Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(androidx.compose.ui.Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = iconColor, modifier = androidx.compose.ui.Modifier.size(32.dp)); Spacer(androidx.compose.ui.Modifier.width(14.dp))
            Column { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp); Text(subtitle, fontSize = 12.sp, color = Color.Gray) }
        }
    }
}
