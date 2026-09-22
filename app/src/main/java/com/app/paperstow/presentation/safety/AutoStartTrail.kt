package com.app.paperstow.presentation.safety

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.app.paperstow.data.safety.SafetyTrailPreferences
import com.app.paperstow.data.safety.SafetyTrailService

@Composable
fun AutoStartTrailIfWanted() {
    val context = LocalContext.current
    val running by SafetyTrailPreferences.running.collectAsState()
    var requested by remember { mutableStateOf(false) }

    fun start() {
        SafetyTrailService.start(context.applicationContext)
    }

    fun hasLocation(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun needsNotification(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { start() }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            SafetyTrailPreferences.setWantsTrail(context, false)
            return@rememberLauncherForActivityResult
        }
        if (needsNotification()) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            start()
        }
    }

    LaunchedEffect(running) {
        if (requested || running || !SafetyTrailPreferences.wantsTrail(context)) return@LaunchedEffect
        requested = true
        when {
            hasLocation() && needsNotification() -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            hasLocation() -> start()
            else -> locationLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }
}
