package com.ben.emberr.domain.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
actual fun rememberAppPermissionCoordinator(): AppPermissionCoordinator {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val grantedPermissions = remember(context) {
        mutableStateMapOf<AppPermission, Boolean>().apply {
            AppPermission.entries.forEach { permission ->
                put(permission, context.isPermissionGranted(permission))
            }
        }
    }

    val alreadyRequestedPermissions = remember(context) { mutableSetOf<AppPermission>() }

    val refreshGrantedPermissions = remember(context, grantedPermissions) {
        {
            AppPermission.entries.forEach { permission ->
                grantedPermissions[permission] = context.isPermissionGranted(permission)
            }
        }
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { refreshGrantedPermissions() }
    )

    val systemSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { refreshGrantedPermissions() }
    )

    DisposableEffect(lifecycleOwner, refreshGrantedPermissions) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshGrantedPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }

    return remember(context) {
        object : AppPermissionCoordinator {

            override fun isGranted(permission: AppPermission): Boolean =
                grantedPermissions[permission] == true

            override fun request(permission: AppPermission) {
                if (isGranted(permission)) return

                if (permission == AppPermission.ExactAlarms) {
                    openScreen(context.buildExactAlarmSettingsIntent())
                    return
                }

                if (permission == AppPermission.UnrestrictedBackground) {
                    openScreen(context.buildBatteryExemptionIntent())
                    return
                }

                val permissionNames = runtimePermissionNamesFor(permission)
                val isFirstRequest = alreadyRequestedPermissions.add(permission)

                if (permissionNames.isEmpty() || !isFirstRequest) {
                    openScreen(context.buildAppDetailsIntent())
                    return
                }

                runCatching { runtimePermissionLauncher.launch(permissionNames.toTypedArray()) }
                    .onFailure { openScreen(context.buildAppDetailsIntent()) }
            }

            private fun openScreen(intent: Intent) {
                val didOpenScreen = runCatching { systemSettingsLauncher.launch(intent) }.isSuccess
                if (!didOpenScreen) {
                    runCatching { systemSettingsLauncher.launch(context.buildAppDetailsIntent()) }
                }
            }
        }
    }
}

private fun runtimePermissionNamesFor(permission: AppPermission): List<String> = when (permission) {
    AppPermission.Microphone -> listOf(Manifest.permission.RECORD_AUDIO)
    AppPermission.Camera -> listOf(Manifest.permission.CAMERA)
    AppPermission.Notifications -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyList()
    }
    AppPermission.ExactAlarms -> emptyList()
    AppPermission.UnrestrictedBackground -> emptyList()
}

private fun Context.isPermissionGranted(permission: AppPermission): Boolean = when (permission) {
    AppPermission.Microphone -> hasRuntimePermission(Manifest.permission.RECORD_AUDIO)
    AppPermission.Camera -> hasRuntimePermission(Manifest.permission.CAMERA)
    AppPermission.Notifications -> NotificationManagerCompat.from(this).areNotificationsEnabled()
    AppPermission.ExactAlarms -> canScheduleExactReminders()
    AppPermission.UnrestrictedBackground -> isIgnoringBatteryOptimizations()
}

private fun Context.hasRuntimePermission(permissionName: String): Boolean =
    ContextCompat.checkSelfPermission(this, permissionName) == PackageManager.PERMISSION_GRANTED

private fun Context.canScheduleExactReminders(): Boolean =
    runCatching { getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }
        .getOrDefault(false)

private fun Context.isIgnoringBatteryOptimizations(): Boolean =
    runCatching { getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName) }
        .getOrDefault(false)

private fun Context.buildExactAlarmSettingsIntent(): Intent =
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, appPackageUri())

private fun Context.buildBatteryExemptionIntent(): Intent =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, appPackageUri())

private fun Context.buildAppDetailsIntent(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appPackageUri())

private fun Context.appPackageUri(): Uri = Uri.fromParts("package", packageName, null)
