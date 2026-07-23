package com.pradeep.jarviscollector.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

private val BgDeep = Color(0xFF0A0F1E)
private val SurfaceGlass = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.08f)
private val AccentViolet = Color(0xFF8B5CF6)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentEmerald = Color(0xFF10B981)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)

data class EngineStatus(
    val name: String,
    val isReady: Boolean = false
)

@Composable
fun SplashScreen(
    ownerName: String,
    onNavigateToHome: () -> Unit
) {
    // ── Animation Stages ──────────────────────────────────────────────────────
    var stage by remember { mutableIntStateOf(0) } // 0: Start, 1: Orb, 2: Title, 3: Engines, 4: Greeting, 5: Finish

    // Engine Readiness checklist
    var activeEngineIndex by remember { mutableIntStateOf(-1) }
    val engineList = remember {
        listOf(
            "Signal Collector",
            "Knowledge Engine",
            "Financial Engine",
            "Lifecycle Engine",
            "Vault"
        )
    }

    // Dynamic greeting calculation
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timeGreeting = remember(currentHour) {
        when {
            currentHour in 0..11 -> "Good Morning"
            currentHour in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    val displayName = remember(ownerName) { ownerName.trim().ifBlank { "Pradeep" } }

    // ── Launch Timeline Controller (Total ~1.5 Seconds) ──────────────────────
    LaunchedEffect(Unit) {
        // Stage 1: Fade & Scale in Logo Orb (300 ms)
        stage = 1
        delay(250L)

        // Stage 2: Fade up "J A R V I S" Title (350 ms)
        stage = 2
        delay(300L)

        // Stage 3: Sequential Agentic Engine Statuses (500 ms)
        stage = 3
        for (i in engineList.indices) {
            activeEngineIndex = i
            delay(100L)
        }
        delay(150L)

        // Stage 4: Ready Greeting (300 ms)
        stage = 4
        delay(350L)

        // Stage 5: Transition to Home
        stage = 5
        delay(100L)
        onNavigateToHome()
    }

    // Smooth Scale & Alpha Transitions for Logo & Title
    val logoScale by animateFloatAsState(
        targetValue = if (stage >= 1) 1.0f else 0.92f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (stage >= 1) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 350),
        label = "logoAlpha"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (stage >= 2) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 350),
        label = "titleAlpha"
    )

    val screenAlpha by animateFloatAsState(
        targetValue = if (stage == 5) 0.0f else 1.0f,
        animationSpec = tween(durationMillis = 250),
        label = "screenAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .alpha(screenAlpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Stage 1: Glowing Assistant Orb Logo
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center
            ) {
                OrbAnimation(size = 110.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stage 2: Branded Title "J A R V I S"
            Box(
                modifier = Modifier.alpha(titleAlpha),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "J A R V I S",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "INTELLIGENT PERSONAL ASSISTANT",
                        color = AccentIndigo,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Stage 3 & 4: Agentic Platform Engines Readiness Stream & Greeting
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(0.85f),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (stage in 3..4) {
                        engineList.forEachIndexed { index, engineName ->
                            val isVisible = activeEngineIndex >= index
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(150)) + expandVertically(tween(150))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceGlass)
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(AccentEmerald)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = engineName,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "Ready",
                                        color = AccentEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Stage 4: Workspace Ready Greeting
                    if (stage >= 4) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.95f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "✨ $timeGreeting, $displayName",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Your workspace is ready.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrbAnimation(
    size: Dp = 110.dp,
    color: Color = Color(0xFF6366F1)
) {
    val infinite = rememberInfiniteTransition(label = "orbTransition")
    val scale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing)
        ),
        label = "orbRotation"
    )

    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = (this.size.minDimension / 2f) * scale
        withTransform(
            transformBlock = { rotate(degrees = rotation, pivot = center) }
        ) {
            // Ambient outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.6f),
                        Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            // Core energetic orb center
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.25f,
                center = center
            )
        }
    }
}
