package com.app.paperstow.presentation.tags

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.app.paperstow.data.local.TagColorStore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    onBack: () -> Unit,
    onViewDocs: (String) -> Unit = {},
    viewModel: TagManagementViewModel = hiltViewModel()
) {
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
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "Refresh tags")
                    }
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
                    Text("${tags.size} tags (sorted ${if (sortByCount) "by usage" else "alphabetically"}). Turn Home off to hide a folder on the start screen.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(tags, key = { it.name }) { tag ->
                    TagListItem(
                        tag = tag,
                        onRename = { tagToRename = tag },
                        onDelete = { tagToDelete = tag },
                        onViewDocs = { onViewDocs(tag.name) },
                        onShowOnHome = { viewModel.setShowOnHome(tag.name, it) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun TagListItem(
    tag: TagInfo,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onViewDocs: () -> Unit,
    onShowOnHome: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val colorStore = remember { TagColorStore(context) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var askPhotoConsent by remember { mutableStateOf(false) }
    var lookTick by remember { mutableIntStateOf(0) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && colorStore.saveImage(tag.name, uri)) lookTick++
    }

    if (askPhotoConsent) {
        AlertDialog(
            onDismissRequest = { askPhotoConsent = false },
            title = { Text("Use a photo?") },
            text = { Text("This picture stays on this phone and is only used as the folder image for “${tag.name}”.", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { askPhotoConsent = false; photoPicker.launch("image/*") }) { Text("Choose photo") }
            },
            dismissButton = { TextButton(onClick = { askPhotoConsent = false }) { Text("Cancel") } }
        )
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Folder image for “${tag.name}”") },
            text = {
                Column {
                    Text("A photo fills this tag’s folder on Home. It stays on this phone.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showPhotoDialog = false; askPhotoConsent = true }) {
                        Icon(Icons.Filled.Image, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Choose photo")
                    }
                    TextButton(onClick = {
                        colorStore.clearImage(tag.name)
                        lookTick++
                        showPhotoDialog = false
                    }) { Text("Remove photo") }
                }
            },
            confirmButton = { TextButton(onClick = { showPhotoDialog = false }) { Text("Done") } }
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { showPhotoDialog = true }) {
                key(lookTick) { TagMark(tag.name, colorStore, size = 36.dp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tag.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("Tap the mark to set a folder photo", fontSize = 11.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Home", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 6.dp))
                    Switch(checked = tag.showOnHome, onCheckedChange = onShowOnHome)
                }
            }
            Badge(containerColor = Color(0xFFE3F2FD)) { Text("${tag.usageCount}", fontSize = 11.sp) }
            IconButton(onClick = onViewDocs, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Visibility, "View documents", tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Edit, "Rename", tint = Color(0xFF757575), modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFBDBDBD), modifier = Modifier.size(18.dp)) }
        }
    }
}
