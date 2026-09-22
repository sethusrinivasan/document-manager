package com.app.paperstow.presentation.feedback

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.paperstow.data.feedback.FeedbackDraft
import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.domain.model.Document
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onNoteSaved: (Document) -> Unit = {},
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showShareConfirm by remember { mutableStateOf(false) }
    var shareDraft by remember { mutableStateOf<FeedbackDraft?>(null) }

    if (showShareConfirm && shareDraft != null) {
        AlertDialog(
            onDismissRequest = { showShareConfirm = false; shareDraft = null },
            title = { Text("Share this feedback?") },
            text = {
                Text(
                    "Paperstow will not upload it. Another app you pick — email or share — will send it. You can still keep the draft on this phone.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val draft = shareDraft!!
                    showShareConfirm = false
                    shareDraft = null
                    sendFeedback(context, draft)
                    viewModel.dismissMessage()
                }) { Text("Yes, share") }
            },
            dismissButton = {
                TextButton(onClick = { showShareConfirm = false; shareDraft = null }) { Text("Keep on phone") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("What worked, and what did not.", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Typed words are saved here as draft feedback. Nothing leaves this phone until you choose to share.",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.liked,
                onValueChange = viewModel::updateLiked,
                label = { Text("What I liked") },
                placeholder = { Text("A feature, a moment, something that helped") },
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.disliked,
                onValueChange = viewModel::updateDisliked,
                label = { Text("What I did not like") },
                placeholder = { Text("Something confusing, missing, or broken") },
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Draft saved on this phone.", fontSize = 12.sp, color = Color.Gray)
            if (state.message != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.message!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val draft = viewModel.currentDraft()
                    if (draft.isBlank()) return@Button
                    shareDraft = draft
                    showShareConfirm = true
                },
                enabled = !state.liked.isBlank() || !state.disliked.isBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Submit") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::newDraft, modifier = Modifier.fillMaxWidth()) {
                Text("New draft")
            }

            val drafts = state.drafts.filter { !it.isBlank() }
            if (drafts.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Draft feedback on this phone", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                drafts.forEach { draft ->
                    val current = draft.id == state.current?.id
                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { viewModel.openDraft(draft.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (current) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            val whenWritten = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(draft.updatedAt))
                            Text(if (current) "Current draft" else "Draft", fontSize = 12.sp, color = Color.Gray)
                            Text(whenWritten, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(draft.preview(), fontSize = 13.sp, maxLines = 2)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    viewModel.openDraft(draft.id)
                                    shareDraft = draft
                                    showShareConfirm = true
                                }) { Text("Send") }
                                TextButton(onClick = {
                                    viewModel.openDraft(draft.id)
                                    viewModel.saveAsNote(onNoteSaved)
                                }) { Text("Save as note") }
                                TextButton(onClick = { viewModel.deleteDraft(draft.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sendFeedback(context: Context, draft: FeedbackDraft) {
    val body = buildString {
        appendLine("Paperstow feedback")
        appendLine()
        append(draft.asNoteBody())
        appendLine()
        appendLine("--- Device (optional context) ---")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE}")
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            appendLine("App: ${info.versionName}")
        } catch (_: Exception) {
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Paperstow — User Feedback")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(
            Intent.createChooser(intent, "Share feedback").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        DebugLogger.i("Feedback", "Share sheet opened")
    } catch (e: Exception) {
        DebugLogger.e("Feedback", "Share failed", e)
    }
}
