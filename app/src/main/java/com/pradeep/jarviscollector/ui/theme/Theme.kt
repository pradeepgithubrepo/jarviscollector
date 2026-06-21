package com.pradeep.jarviscollector.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),          // Electric Violet / Purple
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6D28D9),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF10B981),        // Teal / Emerald Green
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF3B82F6),         // Electric Blue
    onTertiary = Color.White,
    background = Color(0xFF0B0B0E),       // Near Pitch Black
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF161622),          // Translucent dark slate surface
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF232333),
    onSurfaceVariant = Color(0xFF9CA3AF),
    error = Color(0xFFEF4444),            // Coral red
    onError = Color.White
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
