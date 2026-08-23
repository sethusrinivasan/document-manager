package com.app.traveldocs.presentation.documents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Import a document from an HTTP/HTTPS URL.
 *
 * User pastes a URL, we download it in the background, detect the format
 * from the URL extension or content-type header, and feed it into the
 * standard import pipeline.
 *
 * This is gated behind the experimental features flag.
 */
@Composable
fun UrlImportSection(
    importViewModel: ImportViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var url by remember { mutableStateOf("https://github.com/robyoung/dicom-test-files/blob/master/data/pydicom/693_J2KI.dcm") }
    var downloading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (success) {
            Spacer(Modifier.height(40.dp))
            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Downloaded & Imported!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        } else {
            Text("Import from URL", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Paste a direct link to a file (PDF, image, DICOM, etc.)", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it; error = null },
                label = { Text("File URL") },
                placeholder = { Text("https://example.com/document.pdf") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = Color(0xFFF44336), fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))

            if (downloading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Downloading...", fontSize = 13.sp, color = Color.Gray)
            } else {
                Button(
                    onClick = {
                        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                            error = "Enter a valid HTTP or HTTPS URL"
                            return@Button
                        }
                        downloading = true
                        error = null
                        scope.launch {
                            val result = downloadAndImport(url.trim(), importViewModel)
                            downloading = false
                            if (result == null) {
                                success = true
                            } else {
                                error = result
                            }
                        }
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Download & Import") }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}

/**
 * Downloads a file from a URL and imports it.
 * Returns null on success, or an error message string on failure.
 */
private suspend fun downloadAndImport(urlStr: String, importViewModel: ImportViewModel): String? {
    return withContext(Dispatchers.IO) {
        try {
            DebugLogger.i("UrlImport", "Downloading: $urlStr")

            // Handle GitHub blob URLs — convert to raw
            val actualUrl = if (urlStr.contains("github.com") && urlStr.contains("/blob/")) {
                urlStr.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
            } else {
                urlStr
            }

            val connection = URL(actualUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                return@withContext "Server returned HTTP $responseCode"
            }

            val bytes = connection.inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return@withContext "Downloaded file is empty"
            }

            DebugLogger.i("UrlImport", "Downloaded ${bytes.size} bytes")

            // Determine format from URL extension or content-type
            val contentType = connection.contentType ?: ""
            val fileName = actualUrl.substringAfterLast('/').substringBefore('?').ifEmpty { "download" }
            val format = detectFormat(fileName, contentType)

            connection.disconnect()

            // Import via the standard pipeline (on main thread for ViewModel access)
            withContext(Dispatchers.Main) {
                importViewModel.importFromBytes(bytes, format, fileName)
            }

            DebugLogger.i("UrlImport", "Import triggered for: $fileName ($format)")
            null  // success
        } catch (e: Exception) {
            DebugLogger.e("UrlImport", "Download failed", e)
            "Download failed: ${e.message}"
        }
    }
}

private fun detectFormat(fileName: String, contentType: String): DocumentFormat {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        ext == "pdf" || contentType.contains("pdf") -> DocumentFormat.PDF
        ext == "png" || contentType.contains("png") -> DocumentFormat.PNG
        ext in listOf("jpg", "jpeg") || contentType.contains("jpeg") -> DocumentFormat.JPG
        ext == "webp" || contentType.contains("webp") -> DocumentFormat.WEBP
        ext in listOf("heic", "heif") -> DocumentFormat.HEIC
        ext == "bmp" -> DocumentFormat.BMP
        ext == "gif" || contentType.contains("gif") -> DocumentFormat.GIF
        ext == "dcm" || ext == "dicom" || contentType.contains("dicom") -> DocumentFormat.DICOM
        contentType.contains("video") -> DocumentFormat.VIDEO
        ext in listOf("mp3", "m4a", "wav", "ogg", "flac") || contentType.contains("audio") -> DocumentFormat.AUDIO
        else -> DocumentFormat.UNKNOWN // unsupported format, import as generic file
    }
}
