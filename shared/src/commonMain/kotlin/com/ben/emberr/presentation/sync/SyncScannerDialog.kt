package com.ben.emberr.presentation.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ben.emberr.domain.sync.SyncPairingData
import com.ben.emberr.presentation.shared.stableStatusBarsPadding
import kotlinx.serialization.json.Json

@Composable
fun SyncScannerDialog(
    onDismiss: () -> Unit,
    onScanned: (SyncPairingData) -> Unit
) {
    val lenientJson = remember { Json { ignoreUnknownKeys = true } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            QrScannerView(
                onQrScanned = { resultString ->
                    try {
                        val parsedData = lenientJson.decodeFromString<SyncPairingData>(resultString)
                        onScanned(parsedData)
                    } catch (e: Exception) {
                        println("Invalid QR Code ignored: $resultString")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .stableStatusBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = Color.White
                )
            }
        }
    }
}