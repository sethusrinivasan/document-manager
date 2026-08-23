package com.app.traveldocs.presentation.about

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("About") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Document Manager", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Text("Version 1.0", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            Text("A secure, offline-first Android app for families to store, organize, and manage travel documents with OCR-based metadata extraction.", fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            SectionTitle("Security")
            InfoRow("File Encryption", "AES-256-GCM (Android KeyStore)")
            InfoRow("Authentication", "Device Biometrics (fingerprint/face/PIN)")
            InfoRow("Backup", "Encrypted ZIP (no plaintext leaves device)")
            Spacer(Modifier.height(16.dp))

            SectionTitle("Bill of Materials")
            InfoRow("UI Framework", "Jetpack Compose + Material 3")
            InfoRow("Language", "Kotlin 1.9.22")
            InfoRow("DI", "Hilt (Dagger) 2.50")
            InfoRow("Database", "Room 2.6.1")
            InfoRow("OCR Engine", "ML Kit Text Recognition 16.0.0")
            InfoRow("Document Scanner", "ML Kit Document Scanner 16.0.0-beta1")
            InfoRow("Crypto", "BouncyCastle 1.77 (Argon2id, HKDF)")
            InfoRow("Biometric", "AndroidX Biometric 1.1.0")
            InfoRow("Drive API", "Google API Services Drive v3")
            InfoRow("Build System", "Gradle 8.5, AGP 8.2.2, KSP 1.9.22")
            InfoRow("Testing", "JUnit 5, Kotest 5.8.0, MockK 1.13.9")
            Spacer(Modifier.height(16.dp))

            SectionTitle("Acknowledgments")
            Text("This app uses the following open-source libraries, all under Apache 2.0 or compatible licenses:", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            AckRow("Jetpack Compose", "Google", "Apache 2.0")
            AckRow("ML Kit", "Google", "Free, no API key")
            AckRow("BouncyCastle", "Legion of the Bouncy Castle", "MIT-like")
            AckRow("Room", "Google (AndroidX)", "Apache 2.0")
            AckRow("Hilt/Dagger", "Google", "Apache 2.0")
            AckRow("Kotest", "Kotest Team", "Apache 2.0")
            AckRow("MockK", "MockK Contributors", "Apache 2.0")
            AckRow("Material Icons", "Google Fonts", "Apache 2.0")
            Spacer(Modifier.height(16.dp))

            SectionTitle("License")
            Text("Apache License 2.0", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("Copyright 2026 Document Manager Contributors. Licensed under the Apache License, Version 2.0. You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0", fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            SectionTitle("Permissions Required")
            Text("This app requests the following permissions. All are optional and requested only when the feature is used:", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            PermRow("Camera", "Scan documents using phone camera (Take Photo)")
            PermRow("Location (Fine/Coarse)", "Tag documents with GPS coordinates on import; optional background GPS tracking")
            PermRow("Internet", "Cloud backup (Google Drive, S3); Import from URL; WiFi sharing")
            PermRow("Notifications", "GPS tracking foreground service indicator; crash report notification")
            PermRow("Foreground Service", "Background GPS location logging (when user enables it in Settings)")
            Spacer(Modifier.height(4.dp))
            Text("Denying any permission does not break core functionality. Documents can still be imported, viewed, searched, and shared without any permissions.", fontSize = 11.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Spacer(Modifier.height(16.dp))

            SectionTitle("Android Auto (Experimental)")
            Text("This app supports Android Auto for audio playback (experimental, enable in Settings). When connected to a car head unit:", fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            InfoRow("Browsing", "Audio files organized by tags appear as folders")
            InfoRow("Playback", "Play, pause, skip next/previous via car controls")
            InfoRow("Display", "Track name and tags shown on head unit screen")
            InfoRow("Requirement", "Import MP3/audio files and they auto-tag as playable media")
            Spacer(Modifier.height(8.dp))
            Text("Audio files are decrypted on-the-fly for playback. Only files tagged as audio appear in Android Auto.", fontSize = 11.sp, color = Color.Gray)
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
    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
        Column(Modifier.padding(10.dp)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AckRow(name: String, author: String, license: String) {
    Text("• $name — $author ($license)", fontSize = 12.sp, color = Color(0xFF424242), modifier = Modifier.padding(vertical = 1.dp))
}


@Composable
private fun PermRow(permission: String, purpose: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
        Row(Modifier.padding(10.dp)) {
            Text(permission, fontWeight = FontWeight.Medium, fontSize = 12.sp, modifier = Modifier.width(120.dp))
            Text(purpose, fontSize = 12.sp, color = Color(0xFF616161))
        }
    }
}
