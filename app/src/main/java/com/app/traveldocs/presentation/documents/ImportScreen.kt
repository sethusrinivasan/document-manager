package com.app.traveldocs.presentation.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(onDone: () -> Unit, importViewModel: ImportViewModel = hiltViewModel(), batchViewModel: BatchImportViewModel = hiltViewModel()) {
    val singleState by importViewModel.state.collectAsState()
    val batchState by batchViewModel.state.collectAsState()
    var mode by remember { mutableStateOf("choose") } // choose, single, batch_progress
    var pendingFolderUri by remember { mutableStateOf<Uri?>(null) }
    var showSubfolderDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { importViewModel.importFile(uri); mode = "single" }
    }
    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) { pendingFolderUri = uri; showSubfolderDialog = true }
    }
    val drivePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val files = uris.map { uri -> FileToImport(uri, uri.lastPathSegment ?: "file", context.contentResolver.getType(uri)) }
            batchViewModel.importFromDriveFiles(files)
            mode = "batch_progress"
        }
    }

    var capturedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var cameraTags by remember { mutableStateOf("") }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, baos)
            capturedBytes = baos.toByteArray()
            showTagDialog = true
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { try { cameraLauncher.launch(null) } catch (e: Exception) { com.app.traveldocs.debug.DebugLogger.e("Import", "Camera launch failed", e) } }
    }
    // Duplicate dialog
    if (singleState.duplicateFound != null) {
        val dup = singleState.duplicateFound!!
        AlertDialog(onDismissRequest = { importViewModel.cancelDuplicate() }, icon = { Icon(Icons.Filled.ContentCopy, null, tint = Color(0xFFFFC107)) }, title = { Text("Duplicate Document") },
            text = { Column { Text("\"${dup.newFileName}\" already exists."); Spacer(Modifier.height(8.dp)); Text("Replace or cancel?", fontSize = 13.sp, color = Color.Gray) } },
            confirmButton = { Button(onClick = { importViewModel.confirmReplace() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) { Text("Replace") } },
            dismissButton = { TextButton(onClick = { importViewModel.cancelDuplicate() }) { Text("Cancel") } })
    }
    // Tag dialog after camera capture
    if (showTagDialog && capturedBytes != null) {
        AlertDialog(onDismissRequest = { showTagDialog = false; val fn = "camera_" + System.currentTimeMillis() + ".jpg"; importViewModel.importFromBytes(capturedBytes!!, com.app.traveldocs.domain.model.DocumentFormat.JPG, fn); mode = "single" },
            title = { Text("Add Tags") },
            text = { Column { Text("Add comma-separated tags:", fontSize = 13.sp, color = Color.Gray); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = cameraTags, onValueChange = { cameraTags = it }, label = { Text("Tags") }, singleLine = true, modifier = Modifier.fillMaxWidth()) } },
            confirmButton = { TextButton(onClick = { showTagDialog = false; val fn = "camera_" + System.currentTimeMillis() + ".jpg"; importViewModel.importFromBytes(capturedBytes!!, com.app.traveldocs.domain.model.DocumentFormat.JPG, fn); mode = "single" }) { Text("Import") } },
            dismissButton = { TextButton(onClick = { showTagDialog = false; val fn = "camera_" + System.currentTimeMillis() + ".jpg"; importViewModel.importFromBytes(capturedBytes!!, com.app.traveldocs.domain.model.DocumentFormat.JPG, fn); mode = "single" }) { Text("Skip") } }
        )
    }
    // Subfolder scan dialog
    if (showSubfolderDialog && pendingFolderUri != null) {
        AlertDialog(
            onDismissRequest = { showSubfolderDialog = false; pendingFolderUri = null },
            title = { Text("Scan Subfolders?") },
            text = { Column {
                Text("Should we scan subfolders recursively?", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("If yes, the first-level subfolder name will be added as a tag to each imported document.", fontSize = 12.sp, color = Color.Gray)
            } },
            confirmButton = { Button(onClick = { showSubfolderDialog = false; batchViewModel.importFromLocalFolder(pendingFolderUri!!, includeSubfolders = true); pendingFolderUri = null; mode = "batch_progress" }) { Text("Yes, include subfolders") } },
            dismissButton = { OutlinedButton(onClick = { showSubfolderDialog = false; batchViewModel.importFromLocalFolder(pendingFolderUri!!, includeSubfolders = false); pendingFolderUri = null; mode = "batch_progress" }) { Text("No, root only") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Import") }, navigationIcon = { IconButton(onClick = { importViewModel.clearState(); batchViewModel.reset(); onDone() }) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (mode) {
                "choose" -> ImportChooser(onSingleFile = { filePickerLauncher.launch("*/*") }, onLocalFolder = { folderPickerLauncher.launch(null) }, onCamera = { cameraPermission.launch(android.Manifest.permission.CAMERA) }, onDriveFolder = { drivePickerLauncher.launch(arrayOf("application/pdf", "image/*", "video/*")) }, onUrl = { mode = "url_import" })
                "single" -> SingleImportResult(state = singleState, onDone = { importViewModel.clearState(); onDone() }, onRetry = { importViewModel.clearState(); mode = "choose" })
                "batch_progress" -> BatchImportProgressScreen(viewModel = batchViewModel, onDone = { batchViewModel.reset(); onDone() })
                "url_import" -> UrlImportSection(importViewModel = importViewModel, onDone = { mode = "single" }, onBack = { mode = "choose" })
            }
        }
    }
}

@Composable
private fun ImportChooser(onSingleFile: () -> Unit, onLocalFolder: () -> Unit, onDriveFolder: () -> Unit, onCamera: () -> Unit, onUrl: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Choose Import Method", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        ImportOptionCard(icon = Icons.Filled.UploadFile, title = "Single File", subtitle = "Pick one PDF, image, or video", onClick = onSingleFile)
        Spacer(Modifier.height(12.dp))
        ImportOptionCard(icon = Icons.Filled.Folder, title = "Local Folder", subtitle = "Import all files from a phone folder", onClick = onLocalFolder)
        val ctx = androidx.compose.ui.platform.LocalContext.current
        if (com.app.traveldocs.data.local.FeatureFlags.isExperimentalEnabled(ctx) && com.app.traveldocs.data.local.FeatureFlags.isGoogleDriveEnabled(ctx)) { ImportOptionCard(icon = Icons.Filled.Cloud, title = "Google Drive Folder", subtitle = "Browse and import from Drive", onClick = onDriveFolder); Spacer(Modifier.height(12.dp)) }
        Spacer(Modifier.height(12.dp))
        ImportOptionCard(icon = Icons.Filled.Cloud, title = "Take Photo", subtitle = "Capture document with camera", onClick = onCamera)
        Spacer(Modifier.height(12.dp))
        ImportOptionCard(icon = Icons.Filled.Cloud, title = "Import from URL", subtitle = "Download a file from a web address", onClick = onUrl)
    }
}

@Composable
private fun ImportOptionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = Color(0xFF1565C0), modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp); Text(subtitle, fontSize = 13.sp, color = Color.Gray) }
        }
    }
}

@Composable
private fun SingleImportResult(state: ImportUiState, onDone: () -> Unit, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        when {
            state.isImporting -> { CircularProgressIndicator(Modifier.size(64.dp)); Spacer(Modifier.height(16.dp)); Text("Importing..."); Text("Running OCR", fontSize = 13.sp, color = Color.Gray) }
            state.importedDocument != null -> {
                val doc = state.importedDocument!!
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp)); Text("Imported!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Type", fontSize = 12.sp, color = Color.Gray); Text(doc.type.name, fontSize = 12.sp) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("File", fontSize = 12.sp, color = Color.Gray); Text(doc.originalFileName ?: "?", fontSize = 12.sp) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Confidence", fontSize = 12.sp, color = Color.Gray); Text("${((doc.extractionConfidence ?: 0f) * 100).toInt()}%", fontSize = 12.sp) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tags", fontSize = 12.sp, color = Color.Gray); Text(doc.tags.joinToString { it.name }.ifEmpty { "None" }, fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.height(24.dp)); Button(onClick = onDone) { Text("Done") }
            }
            state.error != null -> {
                Icon(Icons.Filled.Error, null, tint = Color(0xFFF44336), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp)); Text("Failed", fontSize = 20.sp, color = Color(0xFFF44336))
                Text(state.error!!, fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(24.dp)); OutlinedButton(onClick = onRetry) { Text("Try Again") }
            }
        }
    }
}
