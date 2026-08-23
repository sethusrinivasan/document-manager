package com.app.traveldocs.presentation.feedback

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.app.traveldocs.debug.DebugLogger

/**
 * In-app feedback form.
 *
 * User types their feedback, optionally includes device info, then taps Send.
 * This opens the system email client with the feedback pre-filled — nothing is
 * sent silently. The user sees exactly what will be sent and must hit Send in
 * their email app to actually transmit it.
 *
 * No data leaves the device without the user's explicit action in their email client.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var feedbackText by remember { mutableStateOf("") }
    var includeDeviceInfo by remember { mutableStateOf(true) }
    var sent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Feedback") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            if (sent) {
                Spacer(Modifier.height(32.dp))
                Text("Thank you!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your email app should have opened with the feedback. " +
                    "Please hit Send in your email app to deliver it to the developer.",
                    fontSize = 14.sp, color = Color.Gray
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            } else {
                Text(
                    "We'd love to hear from you",
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bug reports, feature ideas, or general feedback — all welcome. " +
                    "This will open your email app so you can review before sending.",
                    fontSize = 13.sp, color = Color.Gray
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    label = { Text("Your feedback") },
                    placeholder = { Text("Describe the issue or suggestion...") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 12
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeDeviceInfo, onCheckedChange = { includeDeviceInfo = it })
                    Text("Include device info (model, Android version, app version)", fontSize = 13.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Nothing is sent automatically. Your email app will open so you can review the message before sending.",
                    fontSize = 11.sp, color = Color.Gray
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        sendFeedbackEmail(context, feedbackText, includeDeviceInfo)
                        sent = true
                    },
                    enabled = feedbackText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Email to Send")
                }
            }
        }
    }
}

private fun sendFeedbackEmail(context: Context, feedback: String, includeDevice: Boolean) {
    val deviceInfo = if (includeDevice) {
        buildString {
            appendLine("\n\n--- Device Info ---")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App: Document Manager")
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                appendLine("Version: ${pInfo.versionName} (${pInfo.longVersionCode})")
            } catch (_: Exception) {}
        }
    } else ""

    val body = feedback + deviceInfo

    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("developer@documentmanager.app"))
        putExtra(Intent.EXTRA_SUBJECT, "Document Manager — User Feedback")
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        context.startActivity(Intent.createChooser(emailIntent, "Send feedback via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        DebugLogger.i("Feedback", "Email intent launched (${feedback.length} chars)")
    } catch (e: Exception) {
        DebugLogger.e("Feedback", "Failed to launch email", e)
    }
}
