package com.aj.obd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CarbonDark = Color(0xFF090D12)
val SurfaceDark = Color(0xFF141A23)
val SurfaceVariantDark = Color(0xFF1E2633)
val AmberGlow = Color(0xFFFF9800)
val ElectricCyan = Color(0xFF00E5FF)
val RacingRed = Color(0xFFFF1744)
val TelemetryGreen = Color(0xFF00E676)
val TextPrimary = Color(0xFFF0F6FC)
val TextSecondary = Color(0xFF8B949E)

private val DarkColorScheme = darkColorScheme(
    primary = AmberGlow,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332000),
    onPrimaryContainer = AmberGlow,
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    background = CarbonDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = RacingRed,
    onError = Color.White
)

@Composable
fun ObdAjTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
