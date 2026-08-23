package com.app.traveldocs.presentation.webshare

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.traveldocs.data.webserver.DocumentWebServer
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.repository.DocumentFileStorage
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.TagRepository

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WebShareEntryPoint {
    fun documentRepository(): DocumentRepository
    fun fileStorage(): DocumentFileStorage
    fun tagRepository(): TagRepository
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Access repositories via Hilt EntryPoint (no ViewModel needed for this screen)
    val entryPoint = remember { dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, WebShareEntryPoint::class.java) }
    val documentRepository = remember { entryPoint.documentRepository() }
    val fileStorage = remember { entryPoint.fileStorage() }
    val tagRepository = remember { entryPoint.tagRepository() }
    var serverRunning by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf<DocumentWebServer?>(null) }

    DisposableEffect(Unit) {
        onDispose { server?.stop(); DebugLogger.i("WebShare", "Server stopped (screen disposed)") }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Share via WiFi") }, navigationIcon = { IconButton(onClick = { server?.stop(); onBack() }) { Icon(Icons.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (serverRunning) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Wifi, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Sharing Active", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Spacer(Modifier.height(8.dp))
                        Text("Open this URL in any browser on your WiFi:", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text(serverUrl, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1565C0))
                        Spacer(Modifier.height(8.dp))
                        Text("Access token is embedded in the URL. Only share with trusted devices.", fontSize = 11.sp, color = Color(0xFFE65100))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("From the web browser you can:", fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                listOf("Browse by tags", "Download files (decrypted)", "Upload new files", "Rename documents", "Add/remove tags").forEach { Text("• $it", fontSize = 13.sp, color = Color.Gray) }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { server?.stop(); serverRunning = false; DebugLogger.i("WebShare", "Stopped by user") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Filled.WifiOff, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Stop Sharing", fontSize = 16.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("Stop to secure your documents.", fontSize = 11.sp, color = Color(0xFFF44336))
            } else {
                Spacer(Modifier.height(40.dp))
                Icon(Icons.Filled.Wifi, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("WiFi Document Sharing", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Start a local web server. Any device on your WiFi can browse and manage your documents.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Text("⚠️ While active, anyone on your WiFi can access documents (decrypted). Use only on trusted networks.", fontSize = 12.sp, color = Color(0xFFE65100), modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(20.dp))
                val wifiIp = getWifiIp(context)
                if (wifiIp != null) {
                    Text("WiFi IP: $wifiIp", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val s = DocumentWebServer(context, documentRepository, fileStorage, tagRepository)
                        s.start()
                        server = s
                        serverUrl = s.getServerUrl()
                        serverRunning = true
                        DebugLogger.i("WebShare", "Started at $serverUrl")
                    }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.Filled.Wifi, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Start Sharing", fontSize = 16.sp)
                    }
                } else {
                    Text("Not connected to WiFi.", fontSize = 14.sp, color = Color(0xFFF44336), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private fun getWifiIp(context: Context): String? {
    return try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        if (ip == 0) return null
        "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    } catch (e: Exception) {
        com.app.traveldocs.debug.DebugLogger.e("WebShare", "Failed to get WiFi IP", e)
        null
    }
}
