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
import androidx.compose.material3.OutlinedTextField
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
fun ConsentScreen(onConsented: (pin: String, phone: String, email: String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var pinWarningAcked by remember { mutableStateOf(false) }

    val pinValid = pin.length in 4..8 && pin == pinConfirm
    val canProceed = pinValid && termsAccepted && pinWarningAcked

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Welcome", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
        Text("Document Manager", fontSize = 16.sp, color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your documents will be encrypted with AES-256 and protected by a PIN.", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("All data stays on this device. No cloud, no servers.", fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Create your PIN", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } }, label = { Text("PIN (4-8 digits)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = pinConfirm, onValueChange = { if (it.length <= 8) pinConfirm = it.filter { c -> c.isDigit() } }, label = { Text("Confirm PIN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (pin.isNotEmpty() && pinConfirm.isNotEmpty() && pin != pinConfirm) {
            Text("PINs do not match", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))
        Text("Recovery contacts (for security alerts)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        com.app.traveldocs.presentation.common.PhoneInputWithCountryCode(fullPhone = phone, onPhoneChange = { phone = it }, label = "Phone number")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email address") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("WARNING", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("If you forget your PIN, your documents CANNOT be recovered. There is no reset option. Please write down your PIN and store it securely.", fontSize = 12.sp, color = Color(0xFFBF360C))
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = pinWarningAcked, onCheckedChange = { pinWarningAcked = it })
            Text("I understand my PIN cannot be recovered", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = termsAccepted, onCheckedChange = { termsAccepted = it })
            Text("I agree to use encrypted storage", fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = { onConsented(pin, phone, email) }, enabled = canProceed, modifier = Modifier.fillMaxWidth()) {
            Text("Set Up & Continue")
        }
        Spacer(Modifier.height(40.dp))
    }
}
