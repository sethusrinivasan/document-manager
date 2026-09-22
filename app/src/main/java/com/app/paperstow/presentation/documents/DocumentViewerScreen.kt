package com.app.paperstow.presentation.documents

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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import com.app.paperstow.data.local.TagColorStore
import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.domain.model.Document
import com.app.paperstow.domain.model.DocumentFormat
import com.app.paperstow.domain.model.MarkdownChecklist
import com.app.paperstow.domain.safety.GpxTrail
import com.app.paperstow.domain.safety.UniqueLocations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentViewerScreen(
    document: Document,
    onBack: () -> Unit,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onEditNote: ((String) -> Unit)? = null,
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

    var boundaryMsg by remember { mutableStateOf<String?>(null) }
    // Auto-dismiss boundary message
    LaunchedEffect(boundaryMsg) { if (boundaryMsg != null) { kotlinx.coroutines.delay(1500); boundaryMsg = null } }
    LaunchedEffect(document.id, document.updatedAt) { viewModel.loadFile(document.id) }

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
            confirmButton = { TextButton(onClick = { viewModel.addTag(com.app.paperstow.data.local.InputSanitizer.sanitizeTag(tagInput)); showAddTagDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddTagDialog = false }) { Text("Cancel") } })
    }

    Scaffold(topBar = { TopAppBar(title = { Text(docName, maxLines = 1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
        actions = {
            if (document.format in listOf(DocumentFormat.TEXT, DocumentFormat.MARKDOWN) && fileBytes != null && onEditNote != null) {
                IconButton(onClick = {
                    val text = try { String(fileBytes!!, Charsets.UTF_8) } catch (_: Exception) { "" }
                    onEditNote(text)
                }) { Icon(Icons.Filled.Edit, "Edit note") }
            } else {
                IconButton(onClick = { showRenameDialog = true }) { Icon(Icons.Filled.Edit, "Rename") }
            }
            if (fileBytes != null && !loading) { IconButton(onClick = { shareDocument(context, fileBytes!!, docName, document.format) }) { Icon(Icons.Filled.Share, "Share") } }
        }) }
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
                    loading -> { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(modifier = Modifier.size(48.dp)); Spacer(Modifier.height(12.dp)); Text("Loading...", fontSize = 13.sp, color = Color.Gray) } }
                    fileBytes != null && document.format in listOf(DocumentFormat.JPG, DocumentFormat.PNG, DocumentFormat.WEBP, DocumentFormat.HEIC, DocumentFormat.BMP, DocumentFormat.GIF) -> {
                        val bitmapState = produceState<Bitmap?>(null, fileBytes) { value = withContext(Dispatchers.Default) { try { val b = fileBytes!!; val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(b, 0, b.size, o); val s = if (maxOf(o.outWidth, o.outHeight) > 4096) { var x = 1; while (maxOf(o.outWidth, o.outHeight) / x > 4096) x *= 2; x } else 1; BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = s }) } catch (e: Exception) { null } } }
                        val bmp = bitmapState.value
                        if (bmp != null) { var scale by remember { mutableFloatStateOf(1f) }; var ox by remember { mutableFloatStateOf(0f) }; var oy by remember { mutableFloatStateOf(0f) }
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = "Document", modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = ox, translationY = oy).pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); if (scale > 1f) { ox += pan.x; oy += pan.y } else { ox = 0f; oy = 0f } } }, contentScale = ContentScale.Fit)
                        } else { CircularProgressIndicator(Modifier.size(36.dp)) }
                    }
                    fileBytes != null && document.format == DocumentFormat.PDF -> {
                        PdfPreview(fileBytes!!, Modifier.fillMaxSize())
                    }
                    fileBytes != null && document.format == DocumentFormat.MARKDOWN -> {
                        val text = remember(fileBytes) {
                            try { String(fileBytes!!, Charsets.UTF_8) } catch (_: Exception) { "" }
                        }
                        val list = remember(text) { MarkdownChecklist.parse(text) }
                        LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                            if (list.title.isNotBlank()) {
                                item { Text(list.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp); Spacer(Modifier.height(8.dp)) }
                            }
                            if (list.notes.isNotBlank()) {
                                item { Text(list.notes, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)) }
                            }
                            items(list.items.size) { idx ->
                                val item = list.items[idx]
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Checkbox(
                                        checked = item.done,
                                        onCheckedChange = { checked ->
                                            val updated = list.items.toMutableList()
                                            updated[idx] = item.copy(done = checked)
                                            viewModel.replaceBytes(list.copy(items = updated).toMarkdown().toByteArray(Charsets.UTF_8))
                                        }
                                    )
                                    Text(item.text, fontSize = 16.sp)
                                }
                            }
                            if (list.items.isEmpty()) {
                                item { Text(text.ifBlank { "Empty checklist" }, fontFamily = FontFamily.SansSerif, fontSize = 14.sp) }
                            }
                        }
                    }
                    fileBytes != null && document.format == DocumentFormat.GPX -> {
                        val places = remember(fileBytes) {
                            try { GpxTrail.parse(String(fileBytes!!, Charsets.UTF_8)) } catch (_: Exception) { emptyList() }
                        }
                        if (places.isEmpty()) {
                            Text("No trail points in this GPX file", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                                item {
                                    Text("Trail · ${places.size} places", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Spacer(Modifier.height(8.dp))
                                }
                                items(places.size) { idx ->
                                    val place = places[idx]
                                    val time = if (place.timestamp > 0) {
                                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(place.timestamp))
                                    } else "Time unknown"
                                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                        Column(Modifier.padding(12.dp).clickable {
                                            val geo = android.net.Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}")
                                            try {
                                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, geo))
                                            } catch (_: Exception) {
                                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(UniqueLocations.mapsUrl(place.latitude, place.longitude))))
                                            }
                                        }) {
                                            Text(time, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Text("%.5f, %.5f".format(place.latitude, place.longitude), fontSize = 12.sp, color = Color.Gray)
                                            UniqueLocations.batteryLabel(place.batteryPercent)?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    fileBytes != null && document.format == DocumentFormat.TEXT -> {
                        val text = remember(fileBytes) {
                            try { String(fileBytes!!, Charsets.UTF_8) } catch (_: Exception) { null }
                        }
                        if (text != null) {
                            LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                                item { Text(text, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) }
                            }
                        } else {
                            Text("Could not read as text", color = Color.Gray, fontSize = 14.sp)
                        }
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
            if (fileBytes != null && !loading) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (document.format in listOf(DocumentFormat.TEXT, DocumentFormat.MARKDOWN) && onEditNote != null) {
                    Button(onClick = {
                        val text = try { String(fileBytes!!, Charsets.UTF_8) } catch (_: Exception) { "" }
                        onEditNote(text)
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Edit", fontSize = 12.sp)
                    }
                }
                Button(onClick = { openExternally(context, fileBytes!!, docName, mimeFor(document.format)) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Open External", fontSize = 12.sp) }
            } }
            // Properties toggle
            TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Icon(if (detailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(if (detailsExpanded) "Hide Properties" else "Show Properties", fontSize = 13.sp) }
            if (detailsExpanded) { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Tags
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Tags", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f)); IconButton(onClick = { showAddTagDialog = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, "Add", tint = Color(0xFF1565C0)) } }
                    if (tags.isEmpty()) { Text("No tags", fontSize = 12.sp, color = Color.Gray) } else { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { tags.filter { !it.name.startsWith("__") }.forEach { tag -> val store = TagColorStore(context); InputChip(selected = false, onClick = {}, label = { Text(tag.name, fontSize = 12.sp) }, leadingIcon = { com.app.paperstow.presentation.tags.TagMark(tag.name, store, size = 18.dp) }, trailingIcon = { Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(16.dp).clickable { viewModel.removeTag(tag.name) }) }) } } } } }
                Spacer(Modifier.height(8.dp))
                // Properties
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(12.dp)) { Text("Properties", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.height(6.dp)); MetaRow("Type", document.type.name); MetaRow("Format", document.format.name); MetaRow("Confidence", "${((document.extractionConfidence ?: 0f) * 100).toInt()}%"); Text(filePath.ifEmpty { "" }, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF616161))
                    if (document.metadata.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text("Extracted", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); document.metadata.forEach { (f, v) -> MetaRow(f.name.replace("_", " "), v) } } } }
            } }
        }
    }
}

private fun shareDocument(context: Context, bytes: ByteArray, fileName: String, format: DocumentFormat) {
    try {
        val cacheDir = File(context.cacheDir, "shared_docs"); cacheDir.mkdirs()
        val ext = extensionFor(format)
        val baseName = if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
        val safeName = baseName + ext
        val file = File(cacheDir, safeName); file.writeBytes(bytes)
        DebugLogger.i("Viewer", "Share file written: ${file.absolutePath} (${file.length()} bytes)")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = mimeFor(format)
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

private fun extensionFor(format: DocumentFormat): String = when (format) {
    DocumentFormat.PDF -> ".pdf"
    DocumentFormat.JPG -> ".jpg"
    DocumentFormat.PNG -> ".png"
    DocumentFormat.TEXT -> ".txt"
    DocumentFormat.MARKDOWN -> ".md"
    DocumentFormat.GPX -> ".gpx"
    DocumentFormat.WEBP -> ".webp"
    DocumentFormat.HEIC -> ".heic"
    DocumentFormat.BMP -> ".bmp"
    DocumentFormat.GIF -> ".gif"
    DocumentFormat.VIDEO -> ".mp4"
    DocumentFormat.AUDIO -> ".mp3"
    else -> ""
}

private fun mimeFor(format: DocumentFormat): String = when (format) {
    DocumentFormat.PDF -> "application/pdf"
    DocumentFormat.JPG -> "image/jpeg"
    DocumentFormat.PNG -> "image/png"
    DocumentFormat.TEXT -> "text/plain"
    DocumentFormat.MARKDOWN -> "text/markdown"
    DocumentFormat.GPX -> "application/gpx+xml"
    DocumentFormat.WEBP -> "image/webp"
    DocumentFormat.HEIC -> "image/heic"
    DocumentFormat.BMP -> "image/bmp"
    DocumentFormat.GIF -> "image/gif"
    DocumentFormat.VIDEO -> "video/mp4"
    DocumentFormat.AUDIO -> "audio/mpeg"
    DocumentFormat.DICOM -> "application/dicom"
    DocumentFormat.UNKNOWN -> "application/octet-stream"
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 11.sp, color = Color.Gray); Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
}
