package com.app.traveldocs.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinEntryScreen(onPinSubmit: (String) -> Unit, error: String? = null, isLocked: Boolean = false, lockSeconds: Int = 0) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, null, modifier = Modifier.size(64.dp), tint = Color(0xFF1565C0))
        Spacer(Modifier.height(24.dp))
        Text("Enter PIN", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Unlock your documents", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))

        if (isLocked) {
            Text("Account locked", color = Color.Red, fontWeight = FontWeight.Bold)
            Text("Try again in ${lockSeconds}s", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isLocked,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp)
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color.Red, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = { onPinSubmit(pin); pin = "" }, enabled = pin.length >= 4 && !isLocked, modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
            Text("Unlock")
        }
    }
}
