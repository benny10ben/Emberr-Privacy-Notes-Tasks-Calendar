package com.ben.emberr.presentation.sync

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

actual @Composable fun QrCodeDisplay(
    data: String,
    size: Int,
    modifier: Modifier
) {
    val matrix = remember(data) {
        try {
            QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 33, 33)
        } catch (e: Exception) {
            null
        }
    }

    if (matrix != null) {
        Canvas(modifier = modifier.size(size.dp)) {
            val scale = this.size.width / matrix.width
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    if (matrix.get(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * scale, y * scale),
                            size = Size(scale, scale)
                        )
                    }
                }
            }
        }
    }
}

actual @Composable fun QrScannerView(
    onQrScanned: (String) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("Camera scanning is only supported on mobile devices.")
    }
}