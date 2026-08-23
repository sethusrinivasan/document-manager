package com.app.traveldocs.presentation.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import com.app.traveldocs.data.local.TagColorStore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(onBack: () -> Unit, viewModel: TagManagementViewModel = hiltViewModel()) {
    val tags by viewModel.tags.collectAsState()
    val sortByCount by viewModel.sortByCount.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var tagToRename by remember { mutableStateOf<TagInfo?>(null) }
    var tagToDelete by remember { mutableStateOf<TagInfo?>(null) }
    var deleteUsageCount by remember { mutableIntStateOf(0) }

    // Create dialog
    if (showCreateDialog) {
        var newTag by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Tag") },
            text = { OutlinedTextField(value = newTag, onValueChange = { newTag = it }, label = { Text("Tag name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { viewModel.createTag(newTag); showCreateDialog = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    // Rename dialog
    if (tagToRename != null) {
        var newName by remember { mutableStateOf(tagToRename!!.name) }
        AlertDialog(
            onDismissRequest = { tagToRename = null },
            title = { Text("Rename Tag") },
            text = {
                Column {
                    Text("Current: \"${tagToRename!!.name}\"", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("New name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text("This will update the tag on all ${tagToRename!!.usageCount} documents.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.renameTag(tagToRename!!.name, newName); tagToRename = null }) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { tagToRename = null }) { Text("Cancel") } }
        )
    }

    // Delete confirmation dialog
    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            icon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFF44336)) },
            title = { Text("Delete Tag?") },
            text = {
                Column {
                    Text("Tag: \"${tagToDelete!!.name}\"", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (tagToDelete!!.usageCount > 0) {
                        Text(
                            "This tag is used by ${tagToDelete!!.usageCount} document(s). It will be removed from all of them before deletion.",
                            color = Color(0xFFF44336), fontSize = 13.sp
                        )
                    } else {
                        Text("This tag is not assigned to any documents.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.deleteTag(tagToDelete!!.name); tagToDelete = null }) { Text("Delete", color = Color(0xFFF44336)) } },
            dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Tags") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.toggleSort() }) {
                        Icon(Icons.Filled.SortByAlpha, "Sort", tint = if (sortByCount) Color(0xFF1565C0) else Color.Gray)
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, "Create tag", tint = Color(0xFF1565C0))
                    }
                }
            )
        }
    ) { padding ->
        if (tags.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.Label, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(Modifier.height(16.dp))
                Text("No tags yet", fontSize = 18.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text("Tags are created when you import documents or add them manually", fontSize = 13.sp, color = Color.LightGray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                item {
                    Text("${tags.size} tags (sorted ${if (sortByCount) "by usage" else "alphabetically"})", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(tags) { tag ->
                    TagListItem(tag = tag, onRename = { tagToRename = tag }, onDelete = { tagToDelete = tag })
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagListItem(tag: TagInfo, onRename: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val colorStore = remember { com.app.traveldocs.data.local.TagColorStore(context) }
    var showColorPicker by remember { mutableStateOf(false) }
    var currentColor by remember { mutableStateOf(Color(colorStore.getColor(tag.name))) }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Pick color for \"${tag.name}\"") },
            text = {
                // Simple palette grid
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TagColorStore.PALETTE.forEach { colorInt ->
                        val c = Color(colorInt)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp)
                                .background(c, shape = RoundedCornerShape(8.dp))
                                .clickable {
                                    colorStore.setColor(tag.name, colorInt)
                                    currentColor = c
                                    showColorPicker = false
                                }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("Cancel") } }
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Tap the color circle to change tag color
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(currentColor, shape = RoundedCornerShape(12.dp))
                    .clickable { showColorPicker = true }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tag.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(if (tag.isAutoGenerated) "Auto-generated" else "Custom", fontSize = 11.sp, color = Color.Gray)
            }
            Badge(containerColor = Color(0xFFE3F2FD)) { Text("${tag.usageCount}", fontSize = 11.sp) }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Edit, "Rename", tint = Color(0xFF757575), modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFBDBDBD), modifier = Modifier.size(18.dp)) }
        }
    }
}
