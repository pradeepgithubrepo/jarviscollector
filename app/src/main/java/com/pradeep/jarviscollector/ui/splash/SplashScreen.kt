package com.pradeep.jarviscollector.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    ownerName: String,
    onNavigateToHome: () -> Unit
) {
    val messages = listOf(
        "Initializing Intelligence...",
        "Loading Signals...",
        "Preparing Daily Brief...",
        "Checking Priorities...",
        "Ready."
    )
    val messageIndex = remember { mutableIntStateOf(0) }
    val showGreeting = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (i in messages.indices) {
            delay(400L)
            messageIndex.intValue = i
        }
        showGreeting.value = true
        delay(800L)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OrbAnimation()

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(600)) + scaleIn(initialScale = 0.7f, animationSpec = tween(600))
            ) {
                Text(
                    text = "JARVIS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontSize = 36.sp
                    )
                )
            }

            AnimatedVisibility(
                visible = messageIndex.intValue < messages.size,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                Text(
                    text = messages[messageIndex.intValue],
                    color = Color(0xFFB0B0B3),
                    fontSize = 16.sp
                )
            }

            AnimatedVisibility(
                visible = showGreeting.value,
                enter = fadeIn(tween(500))
            ) {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 5..11 -> "Good Morning"
                    in 12..16 -> "Good Afternoon"
                    else -> "Good Evening"
                }
                Text(
                    text = "$greeting, ${ownerName.replaceFirstChar { it.uppercase() }}",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun OrbAnimation(
    size: Dp = 120.dp,
    color: Color = Color(0xFF4A90E2)
) {
    val infinite = rememberInfiniteTransition(label = "orbTransition")
    val scale = infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    ).value
    val rotation = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "orbRotation"
    ).value

    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = (this.size.minDimension / 2f) * scale
        withTransform(
            transformBlock = { rotate(degrees = rotation, pivot = center) }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.7f), color.copy(alpha = 0.0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            // Inner bright core
            drawCircle(
                color = color.copy(alpha = 0.4f),
                radius = radius * 0.35f,
                center = center
            )
        }
    }
}
