package com.app.traveldocs.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.traveldocs.data.local.FeatureFlags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var experimental by remember { mutableStateOf(FeatureFlags.isExperimentalEnabled(context)) }
    var driveEnabled by remember { mutableStateOf(FeatureFlags.isGoogleDriveEnabled(context)) }
    var s3Enabled by remember { mutableStateOf(FeatureFlags.isS3Enabled(context)) }
    var backupEnabled by remember { mutableStateOf(FeatureFlags.isBackupRestoreEnabled(context)) }
    var gpsEnabled by remember { mutableStateOf(FeatureFlags.isGpsTrackingEnabled(context)) }
    var extFormatsEnabled by remember { mutableStateOf(FeatureFlags.isExtendedFormatsEnabled(context)) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Experimental Features", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text("Enable features that are in development. May be unstable.", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleRow("Enable Experimental Features", experimental) { experimental = it; FeatureFlags.setExperimental(context, it) }
                    if (experimental) {
                        Spacer(Modifier.height(12.dp))
                        ToggleRow("Google Drive Support", driveEnabled) { driveEnabled = it; FeatureFlags.setGoogleDrive(context, it) }
                        Spacer(Modifier.height(8.dp))
                        ToggleRow("S3 Compatible Storage", s3Enabled) { s3Enabled = it; FeatureFlags.setS3(context, it) }
                        Spacer(Modifier.height(8.dp))
                        ToggleRow("Backup & Restore", backupEnabled) { backupEnabled = it; FeatureFlags.setBackupRestore(context, it) }
                        Spacer(Modifier.height(8.dp))
                        var wifiShareEnabled by remember { mutableStateOf(FeatureFlags.isWifiShareEnabled(context)) }
                        ToggleRow("WiFi Document Sharing (Preview)", wifiShareEnabled) { wifiShareEnabled = it; FeatureFlags.setWifiShare(context, it) }
                        Spacer(Modifier.height(8.dp))
                        var audioEnabled by remember { mutableStateOf(FeatureFlags.isAudioPlaybackEnabled(context)) }
                        ToggleRow("Audio Playback & Android Auto (Preview)", audioEnabled) { audioEnabled = it; FeatureFlags.setAudioPlayback(context, it) }
                        Spacer(Modifier.height(8.dp))
                        ToggleRow("Background GPS Tracking (Preview)", gpsEnabled) { gpsEnabled = it; FeatureFlags.setGpsTracking(context, it) }
                        Spacer(Modifier.height(8.dp))
                        ToggleRow("Extended Image Formats", extFormatsEnabled) { extFormatsEnabled = it; FeatureFlags.setExtendedFormats(context, it) }
                        if (extFormatsEnabled) {
                            Spacer(Modifier.height(4.dp))
                            var webpOn by remember { mutableStateOf(FeatureFlags.isFormatEnabled(context, "webp")) }
                            var heicOn by remember { mutableStateOf(FeatureFlags.isFormatEnabled(context, "heic")) }
                            var bmpOn by remember { mutableStateOf(FeatureFlags.isFormatEnabled(context, "bmp")) }
                            var gifOn by remember { mutableStateOf(FeatureFlags.isFormatEnabled(context, "gif")) }
                            var dicomOn by remember { mutableStateOf(FeatureFlags.isFormatEnabled(context, "dicom")) }
                            SubToggleRow("WebP", webpOn) { webpOn = it; FeatureFlags.setFormatEnabled(context, "webp", it) }
                            SubToggleRow("HEIC / HEIF (iOS photos)", heicOn) { heicOn = it; FeatureFlags.setFormatEnabled(context, "heic", it) }
                            SubToggleRow("BMP (legacy scans)", bmpOn) { bmpOn = it; FeatureFlags.setFormatEnabled(context, "bmp", it) }
                            SubToggleRow("GIF", gifOn) { gifOn = it; FeatureFlags.setFormatEnabled(context, "gif", it) }
                            SubToggleRow("DICOM (medical imaging)", dicomOn) { dicomOn = it; FeatureFlags.setFormatEnabled(context, "dicom", it) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Personalization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Home Page Title", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    var homeTitle by remember { mutableStateOf(context.getSharedPreferences("app_settings", 0).getString("home_title", "My Private Documents") ?: "My Private Documents") }
                    OutlinedTextField(
                        value = homeTitle,
                        onValueChange = { homeTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { context.getSharedPreferences("app_settings", 0).edit().putString("home_title", homeTitle).apply() }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Privacy", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var telemetryEnabled by remember { mutableStateOf(context.getSharedPreferences("app_settings", 0).getBoolean("telemetry_enabled", false)) }
                    ToggleRow("Collect usage telemetry (local)", telemetryEnabled) {
                        telemetryEnabled = it
                        context.getSharedPreferences("app_settings", 0).edit().putBoolean("telemetry_enabled", it).apply()
                        com.app.traveldocs.debug.UsageTelemetry.setConsent(it)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("When enabled, anonymous usage stats are stored locally on your device. Nothing is sent automatically.", fontSize = 11.sp, color = Color.Gray)
                    if (telemetryEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { shareTelemetry(context) }, modifier = Modifier.fillMaxWidth()) { Text("Send Telemetry via Email", fontSize = 13.sp) }
                        Text("Opens your email app with telemetry attached. You decide who receives it.", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var darkMode by remember { mutableStateOf(context.getSharedPreferences("app_settings", 0).getBoolean("dark_mode", false)) }
                    ToggleRow("Dark Theme", darkMode) {
                        darkMode = it
                        context.getSharedPreferences("app_settings", 0).edit().putBoolean("dark_mode", it).apply()
                        // Recreate activity to apply theme immediately
                        (context as? android.app.Activity)?.recreate()
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Theme changes apply immediately.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun SubToggleRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Color(0xFF616161))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}


private fun shareTelemetry(context: android.content.Context) {
    try {
        // Package telemetry summary into a text file
        val summary = com.app.traveldocs.debug.UsageTelemetry.getLocalSummary()
        val cacheDir = java.io.File(context.cacheDir, "shared_docs")
        cacheDir.mkdirs()
        val file = java.io.File(cacheDir, "telemetry_report.txt")
        file.writeText(summary)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Document Manager - Usage Telemetry Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Attached: anonymous usage telemetry from Document Manager app.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val resInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (ri in resInfoList) { context.grantUriPermission(ri.activityInfo.packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(android.content.Intent.createChooser(intent, "Send telemetry via").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        com.app.traveldocs.debug.DebugLogger.e("Settings", "Failed to share telemetry", e)
    }
}
