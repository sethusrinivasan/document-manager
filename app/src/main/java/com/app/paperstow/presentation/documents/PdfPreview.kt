package com.app.paperstow.presentation.documents

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.paperstow.debug.DebugLogger
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

private sealed class PdfUi {
    data object Loading : PdfUi()
    data class Ready(val pages: List<Bitmap>) : PdfUi()
    data class Failed(val reason: String) : PdfUi()
}

/**
 * Renders a PDF on one background thread. PdfRenderer is not thread-safe;
 * creating it on IO and paging it on another thread is what broke preview.
 */
@Composable
fun PdfPreview(bytes: ByteArray, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ui by remember(bytes) { mutableStateOf<PdfUi>(PdfUi.Loading) }
    var scale by remember(bytes) { mutableFloatStateOf(1f) }
    var ox by remember(bytes) { mutableFloatStateOf(0f) }
    var oy by remember(bytes) { mutableFloatStateOf(0f) }

    DisposableEffect(bytes) {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val job = scope.launch {
            ui = PdfUi.Loading
            ui = withContext(dispatcher) {
                try {
                    val head = if (bytes.size >= 5) String(bytes, 0, 5, Charsets.ISO_8859_1) else ""
                    if (head != "%PDF-") {
                        return@withContext PdfUi.Failed("Not a PDF")
                    }
                    val file = File.createTempFile("preview", ".pdf", context.cacheDir)
                    file.writeBytes(bytes)
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    val pages = ArrayList<Bitmap>(renderer.pageCount)
                    try {
                        for (i in 0 until renderer.pageCount) {
                            val page = renderer.openPage(i)
                            val srcW = page.width.coerceAtLeast(1)
                            val srcH = page.height.coerceAtLeast(1)
                            val maxW = 1200
                            val factor = maxW.toFloat() / srcW
                            val w = (srcW * factor).toInt().coerceAtLeast(1)
                            val h = (srcH * factor).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            pages += bmp
                        }
                        PdfUi.Ready(pages)
                    } finally {
                        renderer.close()
                        fd.close()
                        file.delete()
                    }
                } catch (e: Exception) {
                    DebugLogger.e("PDF", "Preview failed: ${e.message}", e)
                    PdfUi.Failed(e.message ?: "Could not open PDF")
                }
            }
        }
        onDispose {
            job.cancel()
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    when (val state = ui) {
        PdfUi.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
            CircularProgressIndicator(Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Opening PDF…", fontSize = 13.sp, color = Color.Gray)
        }
        is PdfUi.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
            Icon(Icons.Filled.PictureAsPdf, null, tint = Color(0xFFF44336), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(8.dp))
            Text("PDF preview failed", fontSize = 13.sp, color = Color.Gray)
            Text(state.reason, fontSize = 11.sp, color = Color(0xFF9E9E9E))
            Text("Use Open External to try another viewer.", fontSize = 11.sp, color = Color(0xFF9E9E9E))
        }
        is PdfUi.Ready -> Box(
            modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            ox += pan.x
                            oy += pan.y
                        } else {
                            ox = 0f
                            oy = 0f
                        }
                    }
                }
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = ox, translationY = oy)
            ) {
                itemsIndexed(state.pages) { idx, bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page ${idx + 1}",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}
