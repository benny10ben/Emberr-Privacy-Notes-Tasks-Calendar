package com.ben.emberr.domain.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAppPermissionCoordinator(): AppPermissionCoordinator = remember {
    object : AppPermissionCoordinator {
        override fun isGranted(permission: AppPermission): Boolean = true
        override fun request(permission: AppPermission) = Unit
    }
}
