package com.app.paperstow.presentation.about

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

data class SbomComponent(
    val group: String,
    val name: String,
    val version: String,
    val purl: String
)

data class SbomDocument(
    val specVersion: String,
    val timestamp: String,
    val appName: String,
    val appVersion: String,
    val variant: String,
    val components: List<SbomComponent>,
    val rawJson: String
)

fun loadBundledSbom(context: Context): SbomDocument? {
    return try {
        val json = context.assets.open("sbom.json").bufferedReader().use { it.readText() }
        parseSbom(json)
    } catch (_: Exception) {
        null
    }
}

fun parseSbom(json: String): SbomDocument {
    val root = JSONObject(json)
    val metadata = root.optJSONObject("metadata")
    val component = metadata?.optJSONObject("component")
    val list = root.optJSONArray("components")
    val components = buildList {
        if (list != null) {
            for (i in 0 until list.length()) {
                val c = list.getJSONObject(i)
                add(
                    SbomComponent(
                        group = c.optString("group"),
                        name = c.optString("name"),
                        version = c.optString("version"),
                        purl = c.optString("purl")
                    )
                )
            }
        }
    }
    return SbomDocument(
        specVersion = root.optString("specVersion", "1.5"),
        timestamp = metadata?.optString("timestamp").orEmpty(),
        appName = component?.optString("name").orEmpty(),
        appVersion = component?.optString("version").orEmpty(),
        variant = metadata?.optJSONObject("properties")?.optString("variant")
            ?: metadata?.optJSONArray("properties")?.let { props ->
                (0 until props.length()).firstNotNullOfOrNull { i ->
                    val p = props.getJSONObject(i)
                    if (p.optString("name") == "build:variant") p.optString("value") else null
                }
            }.orEmpty(),
        components = components,
        rawJson = json
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SbomScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val sbom = remember { loadBundledSbom(context) }
    var query by remember { mutableStateOf("") }
    var showRaw by remember { mutableStateOf(false) }
    val filtered = remember(sbom, query) {
        val q = query.trim().lowercase()
        sbom?.components.orEmpty().filter { c ->
            q.isEmpty() ||
                c.name.lowercase().contains(q) ||
                c.group.lowercase().contains(q) ||
                c.version.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SBOM") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    if (sbom != null) {
                        IconButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "Paperstow SBOM ${sbom.appVersion}")
                                putExtra(Intent.EXTRA_TEXT, sbom.rawJson)
                            }
                            context.startActivity(Intent.createChooser(send, "Share SBOM"))
                        }) { Icon(Icons.Filled.Share, "Share") }
                    }
                }
            )
        }
    ) { padding ->
        if (sbom == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text(
                    "No SBOM is bundled in this install. It is generated at compile time and packaged with the app.",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Text("CycloneDX ${sbom.specVersion}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${sbom.appName} ${sbom.appVersion}" + if (sbom.variant.isNotBlank()) " · ${sbom.variant}" else "",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )
                if (sbom.timestamp.isNotBlank()) {
                    Text("Generated ${sbom.timestamp}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${sbom.components.size} libraries in this build. This list is recalculated every compile.",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Filter libraries") }
                )
                TextButton(onClick = { showRaw = !showRaw }) {
                    Text(if (showRaw) "Hide JSON" else "Show JSON")
                }
            }
            if (showRaw) {
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
                    ) {
                        SelectionContainer {
                            Text(
                                sbom.rawJson,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            items(filtered, key = { "${it.group}:${it.name}:${it.version}" }) { c ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(c.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            listOf(c.group, c.version).filter { it.isNotBlank() }.joinToString(" · "),
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
