package com.app.paperstow.presentation.documents

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
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
    var capturedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var cameraTags by remember { mutableStateOf("") }
    var cameraHelp by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, baos)
            capturedBytes = baos.toByteArray()
            showTagDialog = true
        }
    }
    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            importViewModel.importScanResult(result.data)
            mode = "single"
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try { cameraLauncher.launch(null) } catch (e: Exception) {
                com.app.paperstow.debug.DebugLogger.e("Import", "Camera launch failed", e)
                cameraHelp = "Camera is not available. You can re-enable it in system settings."
            }
        } else {
            cameraHelp = "Camera permission is required for the fallback camera. Open settings to grant it, or try Scan document again."
        }
    }
    fun startScan() {
        val activity = context as? Activity
        if (activity == null) {
            cameraPermission.launch(android.Manifest.permission.CAMERA)
            return
        }
        importViewModel.startDocumentScan(
            activity = activity,
            onReady = { sender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
            },
            onUnavailable = {
                cameraHelp = "On-device scanner is downloading or unavailable. Using the system camera instead."
                cameraPermission.launch(android.Manifest.permission.CAMERA)
            }
        )
    }
    LaunchedEffect(cameraHelp) {
        val msg = cameraHelp ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message = msg, actionLabel = "Settings")
        if (result == SnackbarResult.ActionPerformed) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
        cameraHelp = null
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
        AlertDialog(onDismissRequest = { showTagDialog = false; val fn = "camera_" + System.currentTimeMillis() + ".jpg"; importViewModel.importFromBytes(capturedBytes!!, com.app.paperstow.domain.model.DocumentFormat.JPG, fn); mode = "single" },
            title = { Text("Add Tags") },
            text = { Column { Text("Add comma-separated tags:", fontSize = 13.sp, color = Color.Gray); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = cameraTags, onValueChange = { cameraTags = it }, label = { Text("Tags") }, singleLine = true, modifier = Modifier.fillMaxWidth()) } },
            confirmButton = { TextButton(onClick = { showTagDialog = false; val fn = "camera_" + System.currentTimeMillis() + ".jpg"; importViewModel.importFromBytes(capturedBytes!!, com.app.paperstow.domain.model.DocumentFormat.JPG, fn); mode = "single" }) { Text("Import") } },
            dismissButton = { TextButton(onClick = { showTagDialog = false; val fn = "camera_" + System.currentTimeMillis() + ".jpg"; importViewModel.importFromBytes(capturedBytes!!, com.app.paperstow.domain.model.DocumentFormat.JPG, fn); mode = "single" }) { Text("Skip") } }
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Import") }, navigationIcon = { IconButton(onClick = { importViewModel.clearState(); batchViewModel.reset(); onDone() }) { Icon(Icons.Filled.ArrowBack, "Back") } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (mode) {
                "choose" -> ImportChooser(onSingleFile = { filePickerLauncher.launch("*/*") }, onLocalFolder = { folderPickerLauncher.launch(null) }, onCamera = { startScan() })
                "single" -> SingleImportResult(state = singleState, onDone = { importViewModel.clearState(); onDone() }, onRetry = { importViewModel.clearState(); mode = "choose" })
                "batch_progress" -> BatchImportProgressScreen(viewModel = batchViewModel, onDone = { batchViewModel.reset(); onDone() })
            }
        }
    }
}

@Composable
private fun ImportChooser(onSingleFile: () -> Unit, onLocalFolder: () -> Unit, onCamera: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Choose Import Method", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        ImportOptionCard(icon = Icons.Filled.UploadFile, title = "Single File", subtitle = "Pick a PDF, image, text, markdown, or GPX file", onClick = onSingleFile)
        Spacer(Modifier.height(12.dp))
        ImportOptionCard(icon = Icons.Filled.Folder, title = "Local Folder", subtitle = "Import all files from a phone folder", onClick = onLocalFolder)
        Spacer(Modifier.height(12.dp))
        ImportOptionCard(icon = Icons.Filled.PhotoCamera, title = "Scan document", subtitle = "On-device edge detect, crop, and enhance", onClick = onCamera)
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
            state.isImporting -> { CircularProgressIndicator(Modifier.size(64.dp)); Spacer(Modifier.height(16.dp)); Text("Importing..."); Text("Trying to read the page…", fontSize = 13.sp, color = Color.Gray) }
            state.importedDocument != null -> {
                val doc = state.importedDocument!!
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp)); Text("Imported!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)) {
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
