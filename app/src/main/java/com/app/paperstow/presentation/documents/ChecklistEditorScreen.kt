package com.app.paperstow.presentation.documents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.paperstow.domain.model.ChecklistItem
import com.app.paperstow.domain.model.Document
import com.app.paperstow.domain.model.MarkdownChecklist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistEditorScreen(
    existing: Document? = null,
    initialBody: String = "",
    extraTags: List<String> = emptyList(),
    onBack: () -> Unit,
    onSaved: (Document) -> Unit,
    viewModel: TextNoteViewModel = hiltViewModel()
) {
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val parsed = remember(existing?.id, initialBody) {
        if (existing == null && initialBody.isBlank()) MarkdownChecklist.starter()
        else MarkdownChecklist.parse(initialBody)
    }
    var title by remember {
        mutableStateOf(
            existing?.originalFileName
                ?.removeSuffix(".md")?.removeSuffix(".MD")
                ?.ifBlank { parsed.title }
                ?: parsed.title.ifBlank { "Trip plans" }
        )
    }
    var notes by remember { mutableStateOf(parsed.notes) }
    var tags by remember { mutableStateOf(seedEditorTags(existing, extraTags, "Plans")) }
    val items = remember {
        mutableStateListOf<ChecklistItem>().also { list ->
            list.addAll(parsed.items.ifEmpty { listOf(ChecklistItem("")) })
        }
    }
    val colors = MaterialTheme.colorScheme

    fun persist() {
        val doc = MarkdownChecklist(title = title, notes = notes, items = items.toList())
        val body = doc.toMarkdown()
        if (existing == null) viewModel.createChecklist(title, body, tags, onSaved)
        else viewModel.updateDocument(existing, title, body, tags, onSaved)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New checklist" else "Edit checklist") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(22.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { persist() }) { Icon(Icons.Filled.Check, "Save") }
                    }
                }
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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            NoteTagPicker(selected = tags, catalog = catalog, onChange = { tags = it })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(16.dp))
            Text("Items", fontSize = 13.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            items.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = item.done,
                        onCheckedChange = { checked -> items[index] = item.copy(done = checked) }
                    )
                    OutlinedTextField(
                        value = item.text,
                        onValueChange = { items[index] = item.copy(text = it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Action item") }
                    )
                    IconButton(onClick = { if (items.size > 1) items.removeAt(index) }) {
                        Icon(Icons.Filled.Close, "Remove")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = { items += ChecklistItem("") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Add item")
            }
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = colors.error, fontSize = 13.sp)
            }
        }
    }
}
