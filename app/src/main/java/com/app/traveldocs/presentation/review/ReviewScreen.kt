package com.app.traveldocs.presentation.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.traveldocs.domain.model.Document

/**
 * Two-tab review screen:
 * 1. Classify Untagged — Batch-assign tags to documents that have no user tags
 * 2. OCR Review — Review documents where extraction confidence was low and correct metadata
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(onBack: () -> Unit, viewModel: ReviewViewModel = hiltViewModel()) {
    val untaggedDocs by viewModel.untaggedDocs.collectAsState()
    val reviewDocs by viewModel.reviewDocs.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTagDialog by remember { mutableStateOf(false) }
    var selectedDocIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var tagInput by remember { mutableStateOf("") }

    // Tag assignment dialog
    if (showTagDialog && selectedDocIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Assign Tag to ${selectedDocIds.size} document(s)") },
            text = { Column {
                Text("Pick an existing tag or type a new one:", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = tagInput, onValueChange = { tagInput = it }, label = { Text("Tag name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Existing tags:", fontSize = 12.sp, color = Color.Gray)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        allTags.take(15).forEach { tag ->
                            AssistChip(onClick = { tagInput = tag }, label = { Text(tag, fontSize = 11.sp) })
                        }
                    }
                }
            } },
            confirmButton = { TextButton(onClick = {
                if (tagInput.isNotBlank()) {
                    viewModel.assignTagToDocuments(selectedDocIds.toList(), tagInput.trim())
                    selectedDocIds = emptySet()
                    tagInput = ""
                    showTagDialog = false
                }
            }) { Text("Assign") } },
            dismissButton = { TextButton(onClick = { showTagDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review & Classify") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Untagged (${untaggedDocs.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("OCR Review (${reviewDocs.size})") })
            }

            when (selectedTab) {
                0 -> {
                    // Classify untagged documents
                    if (untaggedDocs.isEmpty()) {
                        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("All documents are tagged!", color = Color.Gray)
                        }
                    } else {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${selectedDocIds.size} selected", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                                if (selectedDocIds.isNotEmpty()) {
                                    Button(onClick = { showTagDialog = true }, modifier = Modifier.height(36.dp)) {
                                        Icon(Icons.Filled.Label, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Assign Tag", fontSize = 12.sp)
                                    }
                                }
                                TextButton(onClick = { selectedDocIds = if (selectedDocIds.size == untaggedDocs.size) emptySet() else untaggedDocs.map { it.id }.toSet() }) {
                                    Text(if (selectedDocIds.size == untaggedDocs.size) "Deselect All" else "Select All", fontSize = 12.sp)
                                }
                            }
                        }
                        LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                            items(untaggedDocs) { doc ->
                                val isSelected = doc.id in selectedDocIds
                                Card(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                        selectedDocIds = if (isSelected) selectedDocIds - doc.id else selectedDocIds + doc.id
                                    },
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White)
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isSelected, onCheckedChange = {
                                            selectedDocIds = if (isSelected) selectedDocIds - doc.id else selectedDocIds + doc.id
                                        })
                                        Column(Modifier.weight(1f)) {
                                            Text(doc.originalFileName ?: "Document", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            Text("${doc.format.name} • ${doc.type.name}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // OCR review - documents with low confidence or flagged for manual review
                    if (reviewDocs.isEmpty()) {
                        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No documents need OCR review!", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(Modifier.padding(16.dp)) {
                            items(reviewDocs) { doc ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Warning, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(doc.originalFileName ?: "Document", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                            Text("${((doc.extractionConfidence ?: 0f) * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFFF44336))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("Type: ${doc.type.name} • Format: ${doc.format.name}", fontSize = 11.sp, color = Color.Gray)
                                        if (doc.metadata.isNotEmpty()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text("Extracted fields:", fontSize = 11.sp, color = Color.Gray)
                                            doc.metadata.entries.take(5).forEach { (field, value) ->
                                                Text("  ${field.name.replace("_", " ")}: $value", fontSize = 11.sp)
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row {
                                            TextButton(onClick = { viewModel.markReviewed(doc.id) }) { Text("Mark as Reviewed", fontSize = 12.sp, color = Color(0xFF4CAF50)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
