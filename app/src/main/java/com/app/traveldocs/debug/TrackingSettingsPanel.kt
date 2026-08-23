package com.app.traveldocs.debug

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class IntervalOption(val label: String, val ms: Long)

val intervalOptions = listOf(
    IntervalOption("30s", 30_000L),
    IntervalOption("1 min", 60_000L),
    IntervalOption("5 min", 300_000L),
    IntervalOption("15 min", 900_000L),
    IntervalOption("30 min", 1_800_000L),
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TrackingSettingsPanel() {
    val context = LocalContext.current
    var isTracking by remember { mutableStateOf(isServiceRunning(context)) }
    var selectedInterval by remember { mutableLongStateOf(LocationTrackingService.getInterval(context)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Background GPS Tracking", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isTracking) "Active — logging when moving" else "Stopped",
                        fontSize = 11.sp,
                        color = if (isTracking) Color(0xFF4CAF50) else Color.Gray
                    )
                }
                Switch(
                    checked = isTracking,
                    onCheckedChange = { enabled ->
                        isTracking = enabled
                        if (enabled) {
                            LocationTrackingService.start(context)
                            DebugLogger.i("Settings", "GPS tracking started")
                        } else {
                            LocationTrackingService.stop(context)
                            DebugLogger.i("Settings", "GPS tracking stopped")
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Log interval:", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                intervalOptions.forEach { option ->
                    FilterChip(
                        selected = selectedInterval == option.ms,
                        onClick = {
                            selectedInterval = option.ms
                            LocationTrackingService.setInterval(context, option.ms)
                            DebugLogger.d("Settings", "GPS interval changed to ${option.label}")
                            if (isTracking) {
                                // Restart service to pick up new interval
                                LocationTrackingService.stop(context)
                                LocationTrackingService.start(context)
                            }
                        },
                        label = { Text(option.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1565C0),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

private fun isServiceRunning(context: Context): Boolean {
    val prefs = context.getSharedPreferences(LocationTrackingService.PREFS_NAME, Context.MODE_PRIVATE)
    // Simple heuristic — if we set tracking on, assume it's running
    // A more robust check would use ActivityManager but it's restricted on modern Android
    return prefs.getBoolean("is_tracking", false)
}
