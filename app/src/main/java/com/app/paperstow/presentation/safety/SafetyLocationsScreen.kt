package com.app.paperstow.presentation.safety

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.paperstow.R
import com.app.paperstow.domain.safety.SafetyPlace
import com.app.paperstow.domain.safety.UniqueLocations
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyLocationsScreen(
    onBack: () -> Unit,
    viewModel: SafetyLocationsViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val places by viewModel.places.collectAsState()
    val running by viewModel.running.collectAsState()
    var showDisclosure by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.startTrail(context) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            permissionDenied = true
            return@rememberLauncherForActivityResult
        }
        permissionDenied = false
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startTrail(context)
        }
    }

    fun requestStart() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.startTrail(context)
            }
        } else {
            showDisclosure = true
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(stringResource(R.string.safety_disclosure_title)) },
            text = { Text(stringResource(R.string.safety_disclosure_body), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) { Text(stringResource(R.string.safety_allow_location)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.safety_clear_title)) },
            text = { Text(stringResource(R.string.safety_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearPlaces()
                }) { Text(stringResource(R.string.safety_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.safety_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, stringResource(R.string.btn_back)) } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                stringResource(R.string.safety_intro),
                fontSize = 14.sp,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (running) stringResource(R.string.safety_status_on) else stringResource(R.string.safety_status_off),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (running) stringResource(R.string.safety_status_on_hint) else stringResource(R.string.safety_status_off_hint),
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    if (running) {
                        OutlinedButton(onClick = { viewModel.stopTrail(context) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.safety_stop))
                        }
                    } else {
                        Button(onClick = { requestStart() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.safety_start))
                        }
                    }
                    if (permissionDenied) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.safety_permission_denied), fontSize = 12.sp, color = colors.error)
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            )
                        }) { Text(stringResource(R.string.safety_open_settings)) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val text = viewModel.shareLastKnownText() ?: return@OutlinedButton
                        shareText(context, text)
                    },
                    enabled = places.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.safety_share_last), fontSize = 13.sp) }
                OutlinedButton(
                    onClick = { shareText(context, viewModel.shareAllText()) },
                    enabled = places.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.safety_share_all), fontSize = 13.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.safety_list_title, places.size),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                if (places.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirm = true }) {
                        Text(stringResource(R.string.safety_clear))
                    }
                }
            }
            if (places.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.MyLocation, null, Modifier.size(48.dp), tint = colors.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.safety_empty), fontSize = 14.sp, color = colors.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    itemsIndexed(places) { index, place ->
                        PlaceCard(
                            place = place,
                            isLastKnown = index == 0,
                            onOpenMaps = { openMaps(context, place) },
                            onShare = { shareText(context, UniqueLocationsShare(place)) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun UniqueLocationsShare(place: SafetyPlace): String =
    com.app.paperstow.domain.safety.UniqueLocations.shareLastKnown(place)

@Composable
private fun PlaceCard(
    place: SafetyPlace,
    isLastKnown: Boolean,
    onOpenMaps: () -> Unit,
    onShare: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val time = remember(place.timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(place.timestamp))
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLastKnown) colors.primaryContainer else colors.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                if (isLastKnown) stringResource(R.string.safety_last_known) else stringResource(R.string.safety_earlier_place),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant
            )
            Text(time, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                "%.5f, %.5f".format(place.latitude, place.longitude),
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
            UniqueLocations.batteryLabel(place.batteryPercent)?.let { label ->
                Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onOpenMaps) { Text(stringResource(R.string.safety_open_maps)) }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.share_document))
                }
            }
        }
    }
}

private fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.safety_share_subject))
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.safety_share_subject)))
}

private fun openMaps(context: android.content.Context, place: SafetyPlace) {
    val geo = Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}")
    val maps = Intent(Intent.ACTION_VIEW, geo)
    try {
        context.startActivity(maps)
    } catch (_: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(com.app.paperstow.domain.safety.UniqueLocations.mapsUrl(place.latitude, place.longitude)))
        )
    }
}
