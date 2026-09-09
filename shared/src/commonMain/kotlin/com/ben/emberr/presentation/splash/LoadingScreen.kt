package com.ben.emberr.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.emberr.ui.theme.LocalEmberrFontStyle
import com.ben.emberr.ui.theme.fontFamilyFor
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LoadingScreen(onLoadingComplete: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1500.milliseconds) // Keeps the splash visible for 1.5 seconds
        onLoadingComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Emberr",
                fontFamily = fontFamilyFor(LocalEmberrFontStyle.current),
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Capture your thoughts",
                fontFamily = fontFamilyFor(LocalEmberrFontStyle.current),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .alpha(alphaAnim.value)
                    .clip(RoundedCornerShape(100f))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                val offset by infiniteTransition.animateFloat(
                    initialValue = -120f,
                    targetValue = 120f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "offset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = offset.dp)
                        .width(60.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(100f))
                        .background(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
    }
}