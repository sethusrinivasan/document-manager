package com.app.traveldocs.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit, onDocumentClick: (Document) -> Unit = {}, viewModel: SearchViewModel = hiltViewModel()) {
    val results by viewModel.results.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Search Documents") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it; viewModel.search(it) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search by tag, name, or ask a question...") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true)
            Spacer(Modifier.height(16.dp))

            when (val r = results) {
                is SearchResult.DocumentResults -> {
                    if (r.documents.isEmpty() && query.isNotEmpty()) {
                        Text("No results found", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn {
                            items(r.documents) { doc ->
                                Card(modifier = Modifier.fillMaxWidth().clickable { onDocumentClick(doc) }) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(doc.originalFileName ?: "Document", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text("${doc.type.name} | ${doc.format.name}", fontSize = 12.sp, color = Color.Gray)
                                        if (doc.tags.isNotEmpty()) Text(doc.tags.joinToString(", ") { it.name }, fontSize = 11.sp, color = Color(0xFF1565C0))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
                is SearchResult.TravelChecklist -> {
                    Text("Travel Checklist", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    r.checklist.requiredDocuments.forEach { req ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${req.type.name} x${req.countNeeded}", fontWeight = FontWeight.Medium)
                                Text(req.description, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                is SearchResult.NeedMoreInfo -> {
                    Text("Need more details: ${r.missingParams.joinToString(", ")}", color = Color.Gray, fontSize = 13.sp)
                }
                null -> {
                    if (query.isEmpty()) Text("Try: \"passport\" or \"what documents do I need for Singapore?\"", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}
