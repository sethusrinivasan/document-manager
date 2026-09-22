package com.app.paperstow.presentation.documents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.paperstow.domain.model.Document

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextNoteEditorScreen(
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
    var title by remember {
        mutableStateOf(existing?.originalFileName?.removeSuffix(".txt")?.removeSuffix(".TXT").orEmpty())
    }
    var body by remember { mutableStateOf(initialBody) }
    var tags by remember { mutableStateOf(seedEditorTags(existing, extraTags, "Notes")) }
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New note" else "Edit note") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(22.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = {
                            if (existing == null) viewModel.createNote(title, body, tags, onSaved)
                            else viewModel.updateNote(existing, title, body, tags, onSaved)
                        }) { Icon(Icons.Filled.Check, "Save") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
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
            if (error != null) {
                Text(error!!, color = colors.error, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            Text("Notes", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 4.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 16.sp,
                    color = colors.onSurface,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { inner ->
                    if (body.isEmpty()) {
                        Text("Write your note…", color = colors.onSurfaceVariant, fontSize = 16.sp)
                    }
                    inner()
                }
            )
        }
    }
}
