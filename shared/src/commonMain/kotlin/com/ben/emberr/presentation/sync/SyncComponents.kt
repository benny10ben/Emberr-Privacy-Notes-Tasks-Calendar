package com.ben.emberr.presentation.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect @Composable fun QrCodeDisplay(
    data: String,
    size: Int,
    modifier: Modifier = Modifier
)

expect @Composable fun QrScannerView(
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier
)