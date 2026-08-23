package com.app.traveldocs.presentation.documents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentListScreen(onBack: () -> Unit, onDocumentClick: (Document) -> Unit, onSetDocList: (List<Document>) -> Unit = {}, viewModel: DocumentListViewModel = hiltViewModel()) {
    val documents by viewModel.documents.collectAsState()
    val recycleBin by viewModel.recycleBin.collectAsState()
    val showingTrash by viewModel.showingTrash.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val bulkState by viewModel.bulkDeleteState.collectAsState()
    var showBulkConfirm by remember { mutableStateOf(false) }

    // Bulk delete confirmation
    if (showBulkConfirm) {
        AlertDialog(onDismissRequest = { showBulkConfirm = false }, icon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFF44336)) },
            title = { Text("Delete ${selectedIds.size} documents?") },
            text = { Text("Selected documents will be moved to the recycle bin.") },
            confirmButton = { TextButton(onClick = { showBulkConfirm = false; viewModel.bulkDeleteSelected() }) { Text("Delete All", color = Color(0xFFF44336)) } },
            dismissButton = { TextButton(onClick = { showBulkConfirm = false }) { Text("Cancel") } })
    }

    // Bulk delete progress overlay
    if (bulkState.isRunning || bulkState.isComplete) {
        AlertDialog(onDismissRequest = { if (bulkState.isComplete) viewModel.dismissBulkDelete() },
            title = { Text(if (bulkState.isComplete) "Done" else "Deleting...") },
            text = {
                Column {
                    if (bulkState.isRunning) {
                        LinearProgressIndicator(progress = { if (bulkState.total > 0) bulkState.processed.toFloat() / bulkState.total else 0f }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("${bulkState.processed} / ${bulkState.total}", fontSize = 14.sp)
                        Text(bulkState.currentName, fontSize = 12.sp, color = Color.Gray)
                    } else {
                        Text("${bulkState.total} documents moved to recycle bin.", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = { if (bulkState.isComplete) TextButton(onClick = { viewModel.dismissBulkDelete() }) { Text("OK") } })
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (selectionMode) "${selectedIds.size} selected" else if (showingTrash) "Recycle Bin" else "My Documents") },
            navigationIcon = {
                IconButton(onClick = { if (selectionMode) viewModel.clearSelection() else if (showingTrash) viewModel.toggleTrashView() else onBack() }) {
                    Icon(if (selectionMode) Icons.Filled.Close else Icons.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                if (selectionMode) {
                    IconButton(onClick = { viewModel.selectAll() }) { Icon(Icons.Filled.SelectAll, "Select all") }
                    IconButton(onClick = { showBulkConfirm = true }) { Icon(Icons.Filled.Delete, "Delete selected", tint = Color(0xFFF44336)) }
                } else {
                    if (showingTrash && recycleBin.isNotEmpty()) IconButton(onClick = { viewModel.emptyTrash() }) { Icon(Icons.Filled.DeleteForever, "Empty", tint = Color(0xFFF44336)) }
                    BadgedBox(badge = { if (recycleBin.isNotEmpty()) Badge { Text("${recycleBin.size}") } }) {
                        IconButton(onClick = { viewModel.toggleTrashView() }) { Icon(if (showingTrash) Icons.Filled.Description else Icons.Filled.DeleteSweep, "Trash") }
                    }
                }
            }
        )
    }) { padding ->
        if (showingTrash) {
            if (recycleBin.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.DeleteSweep, null, Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp)); Text("Recycle bin empty", color = Color.Gray)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    items(recycleBin) { doc ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(iconFor(doc.type), null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(doc.originalFileName ?: "Doc", Modifier.weight(1f), fontSize = 14.sp, color = Color.Gray)
                                IconButton(onClick = { viewModel.restoreFromTrash(doc) }) { Icon(Icons.Filled.RestoreFromTrash, "Restore", tint = Color(0xFF4CAF50)) }
                                IconButton(onClick = { viewModel.permanentlyDelete(doc) }) { Icon(Icons.Filled.DeleteForever, "Delete", tint = Color(0xFFF44336)) }
                            }
                        }
                    }
                }
            }
        } else {
            if (documents.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Description, null, Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp)); Text("No documents yet", fontSize = 18.sp, color = Color.Gray)
                    Text("Import a document to get started", fontSize = 13.sp, color = Color.LightGray)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    item { Text("Long-press to select multiple", fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp)) }
                    items(documents) { doc ->
                        val isSelected = doc.id in selectedIds
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .combinedClickable(onClick = { if (selectionMode) viewModel.toggleSelection(doc.id) else { onSetDocList(documents); onDocumentClick(doc) } }, onLongClick = { viewModel.toggleSelection(doc.id) }),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Icon(if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (isSelected) Color(0xFF1565C0) else Color.Gray, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(iconFor(doc.type), null, tint = colorFor(doc.type), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(doc.originalFileName ?: "Document", fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                                    Text(doc.type.name, fontSize = 11.sp, color = Color.Gray)
                                    if (doc.tags.isNotEmpty()) Text(doc.tags.joinToString(", ") { it.name }, fontSize = 10.sp, color = Color(0xFF1565C0), maxLines = 1)
                                }
                                if (!selectionMode) {
                                    IconButton(onClick = { viewModel.moveToTrash(doc) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFBDBDBD), modifier = Modifier.size(18.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(type: DocumentType): ImageVector = when (type) { DocumentType.PASSPORT -> Icons.Filled.Public; DocumentType.VISA -> Icons.Filled.Description; DocumentType.TICKET -> Icons.Filled.FlightTakeoff; DocumentType.HOTEL_BOOKING -> Icons.Filled.Hotel; DocumentType.HEALTH_INSURANCE -> Icons.Filled.HealthAndSafety; DocumentType.UNKNOWN -> Icons.Filled.QuestionMark }
private fun colorFor(type: DocumentType): Color = when (type) { DocumentType.PASSPORT -> Color(0xFF1565C0); DocumentType.VISA -> Color(0xFF4CAF50); DocumentType.TICKET -> Color(0xFFFF9800); DocumentType.HOTEL_BOOKING -> Color(0xFF9C27B0); DocumentType.HEALTH_INSURANCE -> Color(0xFFF44336); DocumentType.UNKNOWN -> Color.Gray }
