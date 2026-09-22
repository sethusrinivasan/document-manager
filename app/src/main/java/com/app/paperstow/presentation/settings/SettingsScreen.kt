package com.app.paperstow.presentation.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.paperstow.R
import com.app.paperstow.debug.UsageTelemetry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onReset: () -> String, indexViewModel: SearchIndexViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val index by indexViewModel.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var resetStep by remember { mutableIntStateOf(0) }
    var resetReport by remember { mutableStateOf("") }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false; resetStep = 0 },
            icon = { Icon(Icons.Filled.RestartAlt, null, tint = Color(0xFFF44336)) },
            title = { Text(if (resetStep < 2) "Reset App?" else "ARE YOU SURE?") },
            text = {
                Column {
                    if (resetStep == 3) {
                        Text("Reset complete. Details:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(resetReport, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Text("Tap Restart App to start fresh.", fontSize = 13.sp)
                    } else if (resetStep < 2) {
                        Text("This will PERMANENTLY DELETE:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("• All imported documents (encrypted files)", fontSize = 13.sp)
                        Text("• All tags and metadata", fontSize = 13.sp)
                        Text("• All settings and preferences", fontSize = 13.sp)
                        Text("• My Trail places on this phone", fontSize = 13.sp)
                        Text("• EULA acceptance (will show again)", fontSize = 13.sp)
                        Text("• Debug logs and telemetry", fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "This CANNOT be undone. The app will restart as freshly installed.",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336),
                            fontSize = 13.sp
                        )
                    } else {
                        Text("Last chance. Tap Delete Everything to proceed.", fontSize = 14.sp, color = Color(0xFFF44336))
                        Spacer(Modifier.height(8.dp))
                        Text("All data will be permanently erased.", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                if (resetStep < 2) {
                    TextButton(onClick = { resetStep = 2 }) { Text("I understand, continue", color = Color(0xFFF44336)) }
                } else if (resetStep == 2) {
                    TextButton(onClick = { resetReport = onReset(); resetStep = 3 }) {
                        Text("DELETE EVERYTHING", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) { Text("Restart App") }
                }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false; resetStep = 0 }) { Text("Cancel") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Personalization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Home Page Title", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    var homeTitle by remember { mutableStateOf(context.getSharedPreferences("app_settings", 0).getString("home_title", "Paperstow") ?: "Paperstow") }
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
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var telemetryEnabled by remember {
                        mutableStateOf(
                            context.getSharedPreferences("app_settings", 0).getBoolean("telemetry_enabled", false) ||
                                context.getSharedPreferences("disclaimer_prefs", 0).getBoolean("telemetry_consented", false)
                        )
                    }
                    ToggleRow("Collect usage telemetry (local only)", telemetryEnabled) {
                        telemetryEnabled = it
                        context.getSharedPreferences("app_settings", 0).edit().putBoolean("telemetry_enabled", it).apply()
                        context.getSharedPreferences("disclaimer_prefs", 0).edit().putBoolean("telemetry_consented", it).apply()
                        UsageTelemetry.setConsent(it)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "When enabled, anonymous usage counts stay on this device. Nothing is sent automatically.",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    val privacyUrl = stringResource(R.string.privacy_policy_url)
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Privacy Policy") }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var darkMode by remember { mutableStateOf(context.getSharedPreferences("app_settings", 0).getBoolean("dark_mode", false)) }
                    ToggleRow("Dark Theme", darkMode) {
                        darkMode = it
                        context.getSharedPreferences("app_settings", 0).edit().putBoolean("dark_mode", it).apply()
                        (context as? android.app.Activity)?.recreate()
                    }
                    Spacer(Modifier.height(12.dp))
                    var showTips by remember { mutableStateOf(context.getSharedPreferences("app_settings", 0).getBoolean("show_tips", true)) }
                    ToggleRow("Show tips", showTips) {
                        showTips = it
                        context.getSharedPreferences("app_settings", 0).edit().putBoolean("show_tips", it).apply()
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Home walkthrough cards. Hide tips on Home remembers this.", fontSize = 11.sp, color = colors.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Search index", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Rebuild reads each paper on this phone, extracts page text (ML Kit for photos and PDFs), and keeps it for Search. Nothing is uploaded.",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { indexViewModel.rebuild() },
                        enabled = !index.running,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (index.running) "Rebuilding…" else "Rebuild search index") }
                    if (index.running || index.done || index.error != null) {
                        Spacer(Modifier.height(12.dp))
                        if (index.total > 0) {
                            LinearProgressIndicator(
                                progress = { index.current.toFloat() / index.total.coerceAtLeast(1) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("${index.current} / ${index.total}  ${index.fileName}", fontSize = 12.sp)
                        }
                        if (index.error != null) {
                            Text(index.error!!, fontSize = 12.sp, color = colors.error)
                        } else if (index.done && !index.running) {
                            Text("Index rebuilt.", fontSize = 12.sp, color = colors.primary)
                        }
                        if (index.excerpt.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Extracted text", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                index.excerpt.take(2000),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Danger zone", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF44336))
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reset App", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Permanently deletes papers, tags, settings, and My Trail on this phone. This cannot be undone.",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { resetStep = 0; resetReport = ""; showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ) { Text("Reset App") }
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
