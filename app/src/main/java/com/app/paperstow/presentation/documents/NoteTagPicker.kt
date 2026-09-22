package com.app.paperstow.presentation.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.paperstow.data.local.InputSanitizer
import com.app.paperstow.data.local.TagColorStore
import com.app.paperstow.domain.model.Document

fun seedEditorTags(existing: Document?, extra: List<String>, defaultTag: String): List<String> {
    val fromDoc = existing?.tags?.map { it.name }?.filter { !it.startsWith("__") }.orEmpty()
    if (fromDoc.isNotEmpty()) return fromDoc.distinct()
    return (listOf(defaultTag) + extra).map { InputSanitizer.sanitizeTag(it) }
        .filter { it.isNotBlank() && !it.startsWith("__") }
        .distinct()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteTagPicker(
    selected: List<String>,
    catalog: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { TagColorStore(context) }
    var draft by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val suggestions = catalog.filter { name ->
        !name.startsWith("__") && selected.none { it.equals(name, ignoreCase = true) }
    }.take(12)

    fun add(raw: String) {
        val name = InputSanitizer.sanitizeTag(raw)
        if (name.isBlank() || name.startsWith("__")) return
        if (selected.any { it.equals(name, ignoreCase = true) }) return
        onChange(selected + name)
    }

    Column(modifier.fillMaxWidth()) {
        Text("Tags", fontSize = 12.sp, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        if (selected.isEmpty()) {
            Text("No tags — this paper will sit in Untagged.", fontSize = 12.sp, color = colors.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                selected.forEach { name ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text(name, fontSize = 12.sp) },
                        leadingIcon = {
                            com.app.paperstow.presentation.tags.TagMark(name, store, size = 18.dp)
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                "Remove $name",
                                modifier = Modifier.size(16.dp).clickable { onChange(selected - name) }
                            )
                        }
                    )
                }
            }
        }
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Add from your tags", fontSize = 11.sp, color = colors.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                suggestions.forEach { name ->
                    FilterChip(
                        selected = false,
                        onClick = { add(name) },
                        label = { Text(name, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(14.dp)) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("New tag") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = {
                    add(draft)
                    draft = ""
                },
                enabled = InputSanitizer.sanitizeTag(draft).isNotBlank()
            ) { Text("Add") }
        }
    }
}
