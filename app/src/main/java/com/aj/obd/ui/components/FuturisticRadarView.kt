package com.aj.obd.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.TelemetryGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FuturisticRadarView(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    isActive: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "radar_anim")

    // Rotação contínua do feixe de radar (0° a 360°)
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    // Expansão contínua da onda de rádio (pulsos concêntricos)
    val pulseProgress1 by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse_1"
    )

    val pulseProgress2 by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse_2"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.minDimension / 2f

            // Anéis concêntricos estáticos de grade futurista
            val rings = listOf(0.33f, 0.66f, 1.0f)
            rings.forEach { ratio ->
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    radius = maxRadius * ratio,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Mira / Eixos em cruz (Crosshair)
            drawLine(
                color = ElectricCyan.copy(alpha = 0.25f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, this.size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = ElectricCyan.copy(alpha = 0.25f),
                start = Offset(0f, center.y),
                end = Offset(this.size.width, center.y),
                strokeWidth = 1.dp.toPx()
            )

            if (isActive) {
                // Ondas de rádio pulsantes
                val alpha1 = (1f - pulseProgress1).coerceIn(0f, 1f)
                drawCircle(
                    color = AmberGlow.copy(alpha = alpha1 * 0.7f),
                    radius = maxRadius * pulseProgress1,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                val alpha2 = (1f - pulseProgress2).coerceIn(0f, 1f)
                drawCircle(
                    color = TelemetryGreen.copy(alpha = alpha2 * 0.6f),
                    radius = maxRadius * pulseProgress2,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Feixe de varredura rotativo
                val rad = Math.toRadians(rotationAngle.toDouble())
                val sweepEnd = Offset(
                    (center.x + maxRadius * cos(rad)).toFloat(),
                    (center.y + maxRadius * sin(rad)).toFloat()
                )

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, ElectricCyan.copy(alpha = 0.8f)),
                        start = center,
                        end = sweepEnd
                    ),
                    start = center,
                    end = sweepEnd,
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        // Ícone central com leve brilho
        Icon(
            imageVector = Icons.Default.SettingsInputAntenna,
            contentDescription = null,
            tint = if (isActive) AmberGlow else Color.Gray,
            modifier = Modifier.size(28.dp)
        )
    }
}
