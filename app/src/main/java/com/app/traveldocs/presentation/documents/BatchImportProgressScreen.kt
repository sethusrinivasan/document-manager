package com.app.traveldocs.presentation.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BatchImportProgressScreen(viewModel: BatchImportViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        when {
            state.isRunning -> {
                Text("Importing Documents...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                if (state.totalFiles > 0) {
                    val progress = state.processedCount.toFloat() / state.totalFiles
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("${state.processedCount} of ${state.totalFiles}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    // Still scanning the folder - show indeterminate progress
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Scanning folder...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Text(state.currentFileName, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                Spacer(Modifier.height(32.dp))
                OutlinedButton(onClick = { viewModel.cancel() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))) {
                    Icon(Icons.Filled.Cancel, null, Modifier.size(18.dp)); Spacer(Modifier.padding(4.dp)); Text("Cancel")
                }
            }
            state.isComplete || state.isCancelled -> {
                Icon(Icons.Filled.CheckCircle, null, tint = if (state.isCancelled) Color(0xFFFFC107) else Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(if (state.isCancelled) "Import Cancelled" else "Import Complete!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Imported"); Text("${state.importedCount}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) }
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Skipped / Failed"); Text("${state.skippedCount}", fontWeight = FontWeight.Bold, color = if (state.skippedCount > 0) Color(0xFFF44336) else Color.Gray) }
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total"); Text("${state.totalFiles}") }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { viewModel.reset(); onDone() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}
