package com.app.traveldocs.presentation.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.app.traveldocs.debug.DebugLogger

@Composable
fun BiometricAuthScreen(activity: FragmentActivity, onAuthenticated: () -> Unit, onSkipped: () -> Unit) {

    LaunchedEffect(Unit) {
        launchBiometric(activity, onAuthenticated, onSkipped)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Fingerprint, null, modifier = Modifier.size(80.dp), tint = Color(0xFF1565C0))
        Spacer(Modifier.height(24.dp))
        Text("Authenticate", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Use fingerprint, face, or device PIN", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { launchBiometric(activity, onAuthenticated, onSkipped) }) {
            Text("Retry Authentication")
        }
    }
}

private fun launchBiometric(activity: FragmentActivity, onSuccess: () -> Unit, onFallback: () -> Unit) {
    val biometricManager = BiometricManager.from(activity)
    val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)

    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        DebugLogger.w("BiometricAuth", "Biometric not available (code=$canAuth), skipping auth")
        onFallback()
        return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            DebugLogger.i("BiometricAuth", "Authentication SUCCESS")
            onSuccess()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            DebugLogger.w("BiometricAuth", "Authentication error: $errorCode $errString")
            // Don't block — let user retry
        }
        override fun onAuthenticationFailed() {
            DebugLogger.w("BiometricAuth", "Authentication failed (wrong biometric)")
        }
    }

    val prompt = BiometricPrompt(activity, executor, callback)
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Document Manager")
        .setSubtitle("Authenticate to access your documents")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    prompt.authenticate(info)
}
