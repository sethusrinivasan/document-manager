package com.app.paperstow.presentation.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.app.paperstow.BuildConfig
import com.app.paperstow.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    var showSbom by remember { mutableStateOf(false) }
    if (showSbom) {
        SbomScreen(onBack = { showSbom = false })
        return
    }
    Scaffold(topBar = { TopAppBar(title = { Text("About") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Paperstow", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.primary)
            Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 14.sp, color = colors.onSurfaceVariant)
            Text("Package ${ctx.packageName}", fontSize = 13.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                "Keep a copy of your family travel papers on this device.",
                fontSize = 14.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Text("Passports, visas, tickets, hotel bookings, and insurance stay encrypted here. The app may suggest a name or tag after a scan — treat that as a helper and always check the original page. Nothing is uploaded unless you share a file or export an archive.", fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            SectionTitle("Security")
            InfoRow("File Encryption", "AES-256-GCM (Android KeyStore)")
            InfoRow("Authentication", "This device's fingerprint, face, or screen lock")
            InfoRow("Archive", "Password-protected ZIP to a folder you pick")
            Spacer(Modifier.height(16.dp))

            SectionTitle("Privacy")
            InfoRow("Storage", "Documents stay encrypted on this phone. Only you can open them.")
            InfoRow("Reading pages", "On-device helper only. Results can be wrong or incomplete. Always check the original.")
            InfoRow("Scan", "On-device ML Kit document scanner (edge detect, crop, enhance). Play services camera; Paperstow does not need camera permission for this.")
            InfoRow("My Trail", "Optional unique places for 24 hours on this phone, with battery level at each save. Share Maps links yourself.")
            InfoRow("Telemetry", "Optional, local usage counts. Off by default.")
            InfoRow("Tag pictures", "Optional folder photo you pick. The photo stays on this phone.")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Privacy Policy") }
            Spacer(Modifier.height(16.dp))

            SectionTitle("Permissions")
            Text("Permissions are requested only when a feature needs them. Denying a permission does not break core import, view, search, or share.", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            PermRow("Camera", "Fallback only if the on-device scanner is unavailable")
            PermRow("Location", "Optional My Trail. Requested when you start it. Not used in the background after you stop.")
            PermRow("Notifications", "Shown while My Trail is recording")
            PermRow("Internet", "Optional ML Kit OCR / scanner model refresh. Documents are not uploaded.")
            Spacer(Modifier.height(16.dp))

            SectionTitle("License")
            Text("Apache License 2.0", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("Copyright 2026 Paperstow Contributors.", fontSize = 11.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                "Third-party libraries keep their own terms: Apache 2.0 (AndroidX, Compose, Hilt, Room, Zip4j, Gson), Bouncy Castle Licence, and Google ML Kit / Play services terms for on-device OCR and scanning.",
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sethusrinivasan/document-manager/blob/main/LICENSE")))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("View License", fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sethusrinivasan/document-manager/blob/main/NOTICE")))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Third-party notices", fontSize = 13.sp) }
            Spacer(Modifier.height(16.dp))

            SectionTitle("Software bill of materials")
            Text(
                "Libraries compiled into this build. Recalculated every time the app is built and stored on this device.",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showSbom = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("View SBOM", fontSize = 13.sp) }
            Spacer(Modifier.height(16.dp))

            SectionTitle("Contact")
            Text("Questions and issues: https://github.com/sethusrinivasan/document-manager/issues", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PermRow(permission: String, purpose: String) {
    val colors = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
        Row(Modifier.padding(10.dp)) {
            Text(permission, fontWeight = FontWeight.Medium, fontSize = 12.sp, modifier = Modifier.width(120.dp))
            Text(purpose, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
    }
}
