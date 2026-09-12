package com.emberr.domain.util.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

enum class AppPermission {
    Microphone,
    Camera,
    Notifications,
    ExactAlarms,
    UnrestrictedBackground
}

@Stable
interface AppPermissionCoordinator {
    fun isGranted(permission: AppPermission): Boolean
    fun request(permission: AppPermission)
}

@Composable
expect fun rememberAppPermissionCoordinator(): AppPermissionCoordinator
