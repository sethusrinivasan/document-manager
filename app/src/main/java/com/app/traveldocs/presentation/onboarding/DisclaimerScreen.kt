package com.app.traveldocs.presentation.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DisclaimerScreen(onAccepted: (telemetryConsent: Boolean) -> Unit) {
    var telemetryChecked by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("Terms of Use", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
        Spacer(Modifier.height(20.dp))

        // Section 1: No Warranty
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
            Column(Modifier.padding(14.dp)) {
                Text("No Warranty", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("This application is provided \"as-is\" without any warranties, express or implied. The developer offers no additional support, service level guarantees, or fitness for any particular purpose. It is the user's responsibility to verify functionality and suitability before relying on this application for important documents.", fontSize = 12.sp, color = Color(0xFF5D4037))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Section 2: Data Responsibility
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
            Column(Modifier.padding(14.dp)) {
                Text("Your Data, Your Responsibility", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Certain features such as sharing, cloud backup, and external viewing can move your documents off your phone to third-party services or other apps. Please exercise caution when using these features and understand that once data leaves this device, it is subject to the privacy policies of those external services.", fontSize = 12.sp, color = Color(0xFF1A237E))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Section 3: Privacy
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
            Column(Modifier.padding(14.dp)) {
                Text("Privacy", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("This application has no intent to collect, access, or transmit your personal documents or their content. All documents are stored locally on your device with encryption. No server, cloud service, or third party receives your document content unless you explicitly use sharing or backup features.", fontSize = 12.sp, color = Color(0xFF1B5E20))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Section 4: Telemetry
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
            Column(Modifier.padding(14.dp)) {
                Text("Usage Telemetry (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("To improve the app experience, the developer may collect anonymous usage telemetry such as: which screens are visited, which features are used, and timing of operations. This data contains NO document content, personal information, or file names. Telemetry is entirely optional and requires your explicit consent below.", fontSize = 12.sp, color = Color(0xFF4A148C))
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = telemetryChecked, onCheckedChange = { telemetryChecked = it })
            Column {
                Text("I consent to anonymous usage telemetry", fontSize = 13.sp)
                Text("You can change this anytime in Settings", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = { onAccepted(telemetryChecked) }, modifier = Modifier.fillMaxWidth()) {
            Text("I Understand & Continue")
        }

        Spacer(Modifier.height(40.dp))
    }
}
