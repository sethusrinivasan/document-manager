package com.app.traveldocs.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Floating debug button that can be placed in any screen.
 * Tapping it opens the full-screen debug log viewer.
 */
@Composable
fun DebugFloatingButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        containerColor = Color(0xFF424242)
    ) {
        Icon(
            imageVector = Icons.Filled.BugReport,
            contentDescription = "View Debug Logs",
            tint = Color(0xFF76FF03),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Full-screen debug log viewer.
 * Shows all captured logs with color-coded severity, auto-scrolls to bottom.
 */
@Composable
fun DebugLogScreen(
    onClose: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val logs by DebugLogger.logFlow.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Debug Logs",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${logs.size} entries | ${DebugLogger.getLogFilePath()}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row {
                IconButton(onClick = {
                    scope.launch {
                        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
                    }
                }) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Scroll to bottom", tint = Color.White)
                }
                IconButton(onClick = { DebugLogger.clear() }) {
                    Icon(Icons.Filled.Delete, "Clear logs", tint = Color(0xFFFF5252))
                }
                IconButton(onClick = { shareDebugLogs(context) }) {
                    Icon(Icons.Filled.Share, "Share logs", tint = Color(0xFF64B5F6))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }
            }
        }

        // Log entries
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(logs) { entry ->
                LogEntryRow(entry)
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: DebugLogger.LogEntry) {
    val levelColor = when (entry.level) {
        DebugLogger.Level.DEBUG -> Color(0xFF82B1FF)
        DebugLogger.Level.INFO -> Color(0xFF69F0AE)
        DebugLogger.Level.WARN -> Color(0xFFFFD740)
        DebugLogger.Level.ERROR -> Color(0xFFFF5252)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            Text(
                entry.timestamp,
                color = Color(0xFF757575),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(6.dp))
            Text(
                entry.level.tag,
                color = levelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "[${entry.component}]",
                color = Color(0xFFCE93D8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(6.dp))
            Text(
                entry.message,
                color = Color(0xFFE0E0E0),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        if (entry.throwable != null) {
            Text(
                "  ${entry.throwable}",
                color = Color(0xFFFF8A80),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}


private fun shareDebugLogs(context: android.content.Context) {
    try {
        val logDir = java.io.File(context.filesDir, "debug_logs")
        val logFile = java.io.File(logDir, "traveldocs_debug.log")
        if (!logFile.exists()) return
        val cacheDir = java.io.File(context.cacheDir, "shared_docs")
        cacheDir.mkdirs()
        val shareFile = java.io.File(cacheDir, "document_manager_debug.log")
        logFile.copyTo(shareFile, overwrite = true)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Document Manager Debug Logs")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val resInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share debug logs").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        DebugLogger.i("DebugLog", "Sharing logs (${shareFile.length() / 1024}KB)")
    } catch (e: Exception) {
        DebugLogger.e("DebugLog", "Failed to share logs", e)
    }
}
