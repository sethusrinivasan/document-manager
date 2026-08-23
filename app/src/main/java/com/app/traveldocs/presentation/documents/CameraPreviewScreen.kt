package com.app.traveldocs.presentation.documents

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraPreviewScreen(
    imageBytes: ByteArray,
    suggestedName: String,
    isNaming: Boolean,
    onRetake: () -> Unit,
    onAccept: (String) -> Unit
) {
    var fileName by remember { mutableStateOf(suggestedName) }
    val bitmap = remember(imageBytes) { BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) }

    LaunchedEffect(suggestedName) { if (suggestedName.isNotBlank()) fileName = suggestedName }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Preview", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).background(Color(0xFF2D2D2D)), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Scanned", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Text("Preview unavailable", color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Filename", fontSize = 13.sp, color = Color.Gray)
        if (isNaming) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Detecting document type...", fontSize = 12.sp, color = Color.Gray)
            }
        }
        OutlinedTextField(value = fileName, onValueChange = { fileName = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Edit filename") })

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Retake")
            }
            Button(onClick = { onAccept(fileName) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Icon(Icons.Filled.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Accept & Import")
            }
        }
    }
}
