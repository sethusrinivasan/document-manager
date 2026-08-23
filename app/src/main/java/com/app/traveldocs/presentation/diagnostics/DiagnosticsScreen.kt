package com.app.traveldocs.presentation.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
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
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.app.traveldocs.debug.TrackingSettingsPanel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit, onViewLogs: () -> Unit) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(-1) }
    var isCharging by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf("--") }
    var longitude by remember { mutableStateOf("--") }
    var accuracy by remember { mutableStateOf("--") }
    var isOnline by remember { mutableStateOf(false) }
    var networkType by remember { mutableStateOf("--") }

    @Suppress("MissingPermission")
    LaunchedEffect(Unit) {
        while (true) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            isCharging = bm?.isCharging ?: false
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
            networkType = when { caps == null -> "Offline"; caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"; caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"; else -> "Other" }
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val loc = lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (loc != null) { latitude = "%.6f".format(loc.latitude); longitude = "%.6f".format(loc.longitude); accuracy = "%.0fm".format(loc.accuracy) }
                }
            } catch (_: Exception) {}
            delay(5000)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Diagnostics") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("System Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            StatusRow(if (isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.Battery4Bar, when { batteryLevel > 50 -> Color(0xFF4CAF50); batteryLevel > 20 -> Color(0xFFFFC107); else -> Color(0xFFF44336) }, "Battery", "${batteryLevel}%", if (isCharging) "Charging" else "Discharging")
            Spacer(Modifier.height(8.dp))
            StatusRow(Icons.Filled.LocationOn, Color(0xFF2196F3), "GPS", "$latitude, $longitude", "Accuracy: $accuracy")
            Spacer(Modifier.height(8.dp))
            StatusRow(if (isOnline) Icons.Filled.SignalWifi4Bar else Icons.Filled.SignalWifiOff, if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336), "Network", networkType, if (isOnline) "Connected" else "Offline")
            Spacer(Modifier.height(20.dp))
            Text("GPS Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            TrackingSettingsPanel()
            Spacer(Modifier.height(20.dp))
            Text("Debug Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onViewLogs, modifier = Modifier.fillMaxWidth()) { Text("View Debug Logs") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { shareLogsAsZip(context) }, modifier = Modifier.fillMaxWidth()) { Text("Share Logs (ZIP)") }
        }
    }
}

@Composable
private fun StatusRow(icon: ImageVector, iconColor: Color, title: String, value: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = iconColor, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontSize = 11.sp, color = Color.Gray); Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 10.sp, color = Color.Gray) }
        }
    }
}


private fun shareLogsAsZip(context: android.content.Context) {
    try {
        val logDir = java.io.File(context.filesDir, "debug_logs")
        val logFile = java.io.File(logDir, "traveldocs_debug.log")
        if (!logFile.exists()) { com.app.traveldocs.debug.DebugLogger.w("Diagnostics", "No log file to share"); return }

        // Create ZIP in cache
        val cacheDir = java.io.File(context.cacheDir, "shared_docs")
        cacheDir.mkdirs()
        val zipFile = java.io.File(cacheDir, "document_manager_logs.zip")
        java.util.zip.ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("debug_log.txt"))
            logFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            // Also include telemetry summary if available
            val telemetry = com.app.traveldocs.debug.UsageTelemetry.getLocalSummary()
            zip.putNextEntry(java.util.zip.ZipEntry("telemetry_summary.txt"))
            zip.write(telemetry.toByteArray())
            zip.closeEntry()
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Document Manager - Debug Logs")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val resInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (ri in resInfoList) { context.grantUriPermission(ri.activityInfo.packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(android.content.Intent.createChooser(intent, "Share logs via").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        com.app.traveldocs.debug.DebugLogger.i("Diagnostics", "Logs ZIP shared (${zipFile.length() / 1024}KB)")
    } catch (e: Exception) {
        com.app.traveldocs.debug.DebugLogger.e("Diagnostics", "Failed to share logs ZIP", e)
    }
}
