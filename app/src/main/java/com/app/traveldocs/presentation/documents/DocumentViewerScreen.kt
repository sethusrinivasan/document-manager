package com.app.traveldocs.presentation.documents

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.traveldocs.data.local.SecureDocumentManager
import com.app.traveldocs.data.local.TagColorStore
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentViewerScreen(
    document: Document,
    onBack: () -> Unit,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    viewModel: DocumentViewerViewModel = hiltViewModel()
) {
    val fileBytes by viewModel.fileBytes.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val filePath by viewModel.filePath.collectAsState()
    val context = LocalContext.current

    var showRenameDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var docName by remember { mutableStateOf(document.originalFileName ?: "Document") }
    var detailsExpanded by remember { mutableStateOf(false) }
    val secureDocMgr = remember { SecureDocumentManager(context) }
    var hasPin by remember { mutableStateOf(secureDocMgr.hasPinSet(document.id)) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    var pinUnlocked by remember { mutableStateOf(!secureDocMgr.hasPinSet(document.id)) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }

    var boundaryMsg by remember { mutableStateOf<String?>(null) }
    // Auto-dismiss boundary message
    LaunchedEffect(boundaryMsg) { if (boundaryMsg != null) { kotlinx.coroutines.delay(1500); boundaryMsg = null } }
    LaunchedEffect(document.id) { if (pinUnlocked) viewModel.loadFile(document.id) }

    // Dialogs
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(docName) }
        AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text("Rename") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("New name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { viewModel.renameDocument(document, newName); docName = newName; showRenameDialog = false }) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } })
    }
    if (showAddTagDialog) {
        var tagInput by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddTagDialog = false }, title = { Text("Add Tag") },
            text = { OutlinedTextField(value = tagInput, onValueChange = { tagInput = it }, label = { Text("Tag name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { viewModel.addTag(com.app.traveldocs.data.local.InputSanitizer.sanitizeTag(tagInput)); showAddTagDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddTagDialog = false }) { Text("Cancel") } })
    }
    if (showPinUnlockDialog) {
        var pin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showPinUnlockDialog = false }, title = { Text("Enter PIN") },
            text = { Column { Text("This document is PIN-protected.", fontSize = 13.sp); Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = pin, onValueChange = { pin = it; pinError = null }, label = { Text("PIN") }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                if (pinError != null) { Spacer(Modifier.height(4.dp)); Text(pinError!!, color = Color(0xFFF44336), fontSize = 12.sp) } } },
            confirmButton = { TextButton(onClick = { if (secureDocMgr.verifyPin(document.id, pin) != null) { pinUnlocked = true; showPinUnlockDialog = false; viewModel.loadFile(document.id) } else pinError = "Incorrect PIN" }) { Text("Unlock") } },
            dismissButton = { TextButton(onClick = { showPinUnlockDialog = false; onBack() }) { Text("Cancel") } })
    }
    if (showSetPinDialog) {
        var pin by remember { mutableStateOf("") }; var confirmPin by remember { mutableStateOf("") }; var pinError by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showSetPinDialog = false }, title = { Text("Lock with PIN") },
            text = { Column { Text("PIN cannot be recovered if forgotten.", fontSize = 12.sp, color = Color(0xFFF44336)); Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = pin, onValueChange = { pin = it; pinError = null }, label = { Text("PIN (4+)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()); Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = confirmPin, onValueChange = { confirmPin = it; pinError = null }, label = { Text("Confirm") }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                if (pinError != null) { Spacer(Modifier.height(4.dp)); Text(pinError!!, color = Color(0xFFF44336), fontSize = 12.sp) } } },
            confirmButton = { TextButton(onClick = { when { pin.length < 4 -> pinError = "Min 4 characters"; pin != confirmPin -> pinError = "PINs don't match"; else -> { secureDocMgr.setPin(document.id, pin); viewModel.addTag(SecureDocumentManager.SECURE_TAG); hasPin = true; showSetPinDialog = false } } }) { Text("Lock") } },
            dismissButton = { TextButton(onClick = { showSetPinDialog = false }) { Text("Cancel") } })
    }
    if (showRemovePinDialog) {
        var pin by remember { mutableStateOf("") }; var pinError by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showRemovePinDialog = false }, title = { Text("Remove PIN") },
            text = { Column { OutlinedTextField(value = pin, onValueChange = { pin = it; pinError = null }, label = { Text("Current PIN") }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                if (pinError != null) { Spacer(Modifier.height(4.dp)); Text(pinError!!, color = Color(0xFFF44336), fontSize = 12.sp) } } },
            confirmButton = { TextButton(onClick = { if (secureDocMgr.verifyPin(document.id, pin) != null) { secureDocMgr.removePin(document.id); viewModel.removeTag(SecureDocumentManager.SECURE_TAG); hasPin = false; showRemovePinDialog = false } else pinError = "Incorrect PIN" }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { showRemovePinDialog = false }) { Text("Cancel") } })
    }

    Scaffold(topBar = { TopAppBar(title = { Text(docName, maxLines = 1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
        actions = { if (fileBytes != null && !loading && pinUnlocked) { IconButton(onClick = { shareDocument(context, fileBytes!!, docName, document.format) }) { Icon(Icons.Filled.Share, "Share") } }; IconButton(onClick = { showRenameDialog = true }) { Icon(Icons.Filled.Edit, "Rename") } }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White)
                .pointerInput(onPrev, onNext) {
                    var verticalAccum = 0f
                    detectTransformGestures { _, pan, _, _ ->
                        verticalAccum += pan.y
                        if (verticalAccum > 200) {
                            verticalAccum = 0f
                            if (onPrev != null) onPrev() else boundaryMsg = "No previous document"
                        } else if (verticalAccum < -200) {
                            verticalAccum = 0f
                            if (onNext != null) onNext() else boundaryMsg = "No next document"
                        }
                    }
                }, contentAlignment = Alignment.Center) {
                when {
                    !pinUnlocked -> { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Lock, null, tint = Color(0xFFE65100), modifier = Modifier.size(64.dp)); Spacer(Modifier.height(12.dp)); Text("PIN Protected", fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Enter PIN to view", fontSize = 13.sp, color = Color.Gray); Spacer(Modifier.height(16.dp)); Button(onClick = { showPinUnlockDialog = true }) { Text("Unlock") } } }
                    loading -> { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(modifier = Modifier.size(48.dp)); Spacer(Modifier.height(12.dp)); Text("Loading...", fontSize = 13.sp, color = Color.Gray) } }
                    fileBytes != null && document.format in listOf(DocumentFormat.JPG, DocumentFormat.PNG, DocumentFormat.WEBP, DocumentFormat.HEIC, DocumentFormat.BMP, DocumentFormat.GIF) -> {
                        val bitmapState = produceState<Bitmap?>(null, fileBytes) { value = withContext(Dispatchers.Default) { try { val b = fileBytes!!; val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(b, 0, b.size, o); val s = if (maxOf(o.outWidth, o.outHeight) > 4096) { var x = 1; while (maxOf(o.outWidth, o.outHeight) / x > 4096) x *= 2; x } else 1; BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = s }) } catch (e: Exception) { null } } }
                        val bmp = bitmapState.value
                        if (bmp != null) { var scale by remember { mutableFloatStateOf(1f) }; var ox by remember { mutableFloatStateOf(0f) }; var oy by remember { mutableFloatStateOf(0f) }
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = "Document", modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = ox, translationY = oy).pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); if (scale > 1f) { ox += pan.x; oy += pan.y } else { ox = 0f; oy = 0f } } }, contentScale = ContentScale.Fit)
                        } else { CircularProgressIndicator(Modifier.size(36.dp)) }
                    }
                    fileBytes != null && document.format == DocumentFormat.PDF -> {
                        val pdfInfoState = produceState<Triple<File, android.os.ParcelFileDescriptor, android.graphics.pdf.PdfRenderer>?>(null, fileBytes) { value = withContext(Dispatchers.IO) { try { val f = File.createTempFile("p", ".pdf", context.cacheDir); f.writeBytes(fileBytes!!); val fd = android.os.ParcelFileDescriptor.open(f, android.os.ParcelFileDescriptor.MODE_READ_ONLY); Triple(f, fd, android.graphics.pdf.PdfRenderer(fd)) } catch (e: Exception) { null } } }
                        val pdfInfo = pdfInfoState.value
                        if (pdfInfo != null) { val (_, _, renderer) = pdfInfo; val pageCount = renderer.pageCount; val pdfDispatcher = remember { Executors.newSingleThreadExecutor().asCoroutineDispatcher() }; var pdfScale by remember { mutableFloatStateOf(1f) }; var ox by remember { mutableFloatStateOf(0f) }; var oy by remember { mutableFloatStateOf(0f) }
                            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> pdfScale = (pdfScale * zoom).coerceIn(1f, 5f); if (pdfScale > 1f) { ox += pan.x; oy += pan.y } else { ox = 0f; oy = 0f } } }) {
                                LazyColumn(Modifier.fillMaxSize().background(Color.White).graphicsLayer(scaleX = pdfScale, scaleY = pdfScale, translationX = ox, translationY = oy)) {
                                    items(pageCount) { idx -> val bmpState = produceState<Bitmap?>(null, idx) { value = withContext(pdfDispatcher) { try { val page = renderer.openPage(idx); val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888); bmp.eraseColor(android.graphics.Color.WHITE); page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); page.close(); bmp } catch (e: Exception) { null } } }
                                        val bmp = bmpState.value; if (bmp != null) { Image(bitmap = bmp.asImageBitmap(), contentDescription = "Page ${idx+1}", modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), contentScale = ContentScale.FillWidth) } else { Box(Modifier.fillMaxWidth().height(300.dp).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } } } } }
                        } else { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.PictureAsPdf, null, tint = Color(0xFFF44336), modifier = Modifier.size(64.dp)); Text("PDF preview failed", color = Color.Gray) } }
                    }
                    fileBytes != null && (document.format == DocumentFormat.VIDEO || document.format == DocumentFormat.AUDIO) -> { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.OpenInNew, null, tint = Color(0xFF2196F3), modifier = Modifier.size(64.dp)); Spacer(Modifier.height(12.dp)); Text(if (document.format == DocumentFormat.AUDIO) "Audio" else "Video", fontSize = 16.sp); Text("${fileBytes!!.size / 1024} KB", color = Color.Gray, fontSize = 13.sp); Spacer(Modifier.height(16.dp)); Button(onClick = {
                            val mime = if (document.format == DocumentFormat.AUDIO) "audio/mpeg" else "video/mp4"
                            // Ensure file has proper extension for media players
                            val ext = if (document.format == DocumentFormat.AUDIO) ".mp3" else ".mp4"
                            val playName = if (docName.contains('.')) docName else docName + ext
                            openExternally(context, fileBytes!!, playName, mime)
                        }) { Text("Play") } } }
                    else -> { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.BrokenImage, null, tint = Color.Gray, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(8.dp)); Text("File not available", color = Color.Gray, fontSize = 14.sp) } }
                }
            }
            // Boundary message overlay
            if (boundaryMsg != null) {
                Text(boundaryMsg!!, fontSize = 12.sp, color = Color(0xFF757575),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(vertical = 4.dp, horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            // Bottom bar
            if (fileBytes != null && !loading && pinUnlocked) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { val mime = when (document.format) { DocumentFormat.PDF -> "application/pdf"; DocumentFormat.JPG -> "image/jpeg"; DocumentFormat.PNG -> "image/png"; DocumentFormat.AUDIO -> "audio/*"; else -> "application/octet-stream" }; openExternally(context, fileBytes!!, docName, mime) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Open External", fontSize = 12.sp) } } }
            // Properties toggle
            TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Icon(if (detailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(if (detailsExpanded) "Hide Properties" else "Show Properties", fontSize = 13.sp) }
            if (detailsExpanded) { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Tags
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Tags", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f)); IconButton(onClick = { showAddTagDialog = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, "Add", tint = Color(0xFF1565C0)) } }
                    if (tags.isEmpty()) { Text("No tags", fontSize = 12.sp, color = Color.Gray) } else { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { tags.filter { !it.name.startsWith("__") }.forEach { tag -> InputChip(selected = false, onClick = {}, label = { Text(tag.name, fontSize = 12.sp) }, trailingIcon = { Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(16.dp).clickable { viewModel.removeTag(tag.name) }) }, colors = InputChipDefaults.inputChipColors(containerColor = Color(TagColorStore(context).getColor(tag.name)))) } } } } }
                Spacer(Modifier.height(8.dp))
                // PIN
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (hasPin) Color(0xFFFFF3E0) else Color.White)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (hasPin) "PIN-locked" else "No PIN", modifier = Modifier.weight(1f), fontSize = 13.sp); if (hasPin) { TextButton(onClick = { showRemovePinDialog = true }) { Text("Remove", fontSize = 12.sp) } } else { TextButton(onClick = { showSetPinDialog = true }) { Text("Set PIN", fontSize = 12.sp, color = Color(0xFF1565C0)) } } } }
                Spacer(Modifier.height(8.dp))
                // Properties
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(12.dp)) { Text("Properties", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.height(6.dp)); MetaRow("Type", document.type.name); MetaRow("Format", document.format.name); MetaRow("Confidence", "${((document.extractionConfidence ?: 0f) * 100).toInt()}%"); MetaRow("Doc ID", document.id.take(8) + "..."); Text(filePath.ifEmpty { "" }, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF616161))
                    if (document.metadata.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text("Extracted", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); document.metadata.forEach { (f, v) -> MetaRow(f.name.replace("_", " "), v) } } } }
            } }
        }
    }
}

private fun shareDocument(context: Context, bytes: ByteArray, fileName: String, format: DocumentFormat) {
    try {
        val cacheDir = File(context.cacheDir, "shared_docs"); cacheDir.mkdirs()
        val ext = when (format) { DocumentFormat.PDF -> ".pdf"; DocumentFormat.JPG -> ".jpg"; DocumentFormat.PNG -> ".png"; DocumentFormat.VIDEO -> ".mp4"; DocumentFormat.AUDIO -> ".mp3"; else -> "" }
        val baseName = if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
        val safeName = baseName + ext
        val file = File(cacheDir, safeName); file.writeBytes(bytes)
        DebugLogger.i("Viewer", "Share file written: ${file.absolutePath} (${file.length()} bytes)")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = when (format) { DocumentFormat.PDF -> "application/pdf"; DocumentFormat.JPG -> "image/jpeg"; DocumentFormat.PNG -> "image/png"; DocumentFormat.VIDEO -> "video/mp4"; DocumentFormat.AUDIO -> "audio/mpeg"; else -> "application/octet-stream" }
        val intent = Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val resInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (ri in resInfoList) { context.grantUriPermission(ri.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { DebugLogger.e("Viewer", "Share failed", e) }
}

private fun openExternally(context: Context, bytes: ByteArray, fileName: String, mimeType: String) {
    try {
        val cacheDir = File(context.cacheDir, "shared_docs"); cacheDir.mkdirs()
        // Ensure correct extension for the target MIME type
        val ext = when {
            mimeType.contains("audio") && !fileName.endsWith(".mp3") && !fileName.endsWith(".m4a") -> ".mp3"
            mimeType.contains("video") && !fileName.endsWith(".mp4") -> ".mp4"
            mimeType.contains("pdf") && !fileName.endsWith(".pdf") -> ".pdf"
            else -> ""
        }
        val safeName = if (ext.isNotEmpty() && !fileName.contains('.')) fileName + ext else fileName
        val file = File(cacheDir, safeName); file.writeBytes(bytes)
        DebugLogger.i("Viewer", "Share file written: ${file.absolutePath} (${file.length()} bytes)")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        // Grant permission to all potential receivers
        val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mimeType); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) }
        val resInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (ri in resInfoList) { context.grantUriPermission(ri.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        if (resInfoList.isNotEmpty()) {
            context.startActivity(intent)
        } else {
            // Fallback to chooser if no direct handler found
            context.startActivity(Intent.createChooser(intent, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        DebugLogger.i("Viewer", "Opened externally: $safeName ($mimeType)")
    } catch (e: Exception) { DebugLogger.e("Viewer", "Open failed", e) }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 11.sp, color = Color.Gray); Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
}
