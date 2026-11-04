package com.aj.obd.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.CarbonDark
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceVariantDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularGauge(
    value: Float,
    min: Float,
    max: Float,
    title: String,
    unit: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    peakValue: Float? = null,
    warningThreshold: Float? = null,
    accentColor: Color = AmberGlow,
    isActive: Boolean = true
) {
    val isWarning = warningThreshold != null && value >= warningThreshold && isActive

    val clamped = if (isActive) value.coerceIn(min, max) else min
    val percentage = if (max > min) ((clamped - min) / (max - min)).coerceIn(0f, 1f) else 0f

    val animatedPercent by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 140),
        label = "gauge_anim"
    )

    // Efeito pulsar em caso de aviso crítico (ex: temperatura acima de 102°C)
    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val activeColor by animateColorAsState(
        targetValue = if (isWarning) RacingRed else accentColor,
        label = "active_color"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = (size.toPx() * 0.09f).coerceIn(8.dp.toPx(), 15.dp.toPx())
            val diameter = this.size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            val startAngle = 135f
            val sweepAngle = 270f

            // Fundo do arco
            drawArc(
                color = SurfaceVariantDark.copy(alpha = if (isActive) 0.8f else 0.4f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Arco ativo graduado
            val activeSweep = sweepAngle * animatedPercent
            if (activeSweep > 1f && isActive) {
                val gaugeBrush = if (isWarning) {
                    Brush.sweepGradient(colors = listOf(RacingRed.copy(alpha = pulseAlpha), RacingRed))
                } else {
                    Brush.sweepGradient(
                        colors = listOf(ElectricCyan, AmberGlow, RacingRed)
                    )
                }

                drawArc(
                    brush = gaugeBrush,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Marcador de Pico Máximo (Peak Hold Indicator)
            if (peakValue != null && peakValue > min && isActive) {
                val peakClamped = peakValue.coerceIn(min, max)
                val peakPercent = (peakClamped - min) / (max - min)
                val peakAngleRad = Math.toRadians((startAngle + sweepAngle * peakPercent).toDouble())
                val radius = diameter / 2
                val center = Offset(this.size.width / 2, this.size.height / 2)

                val pStartX = center.x + (radius - strokeWidth / 2) * cos(peakAngleRad).toFloat()
                val pStartY = center.y + (radius - strokeWidth / 2) * sin(peakAngleRad).toFloat()
                val pEndX = center.x + (radius + strokeWidth / 2) * cos(peakAngleRad).toFloat()
                val pEndY = center.y + (radius + strokeWidth / 2) * sin(peakAngleRad).toFloat()

                drawLine(
                    color = RacingRed,
                    start = Offset(pStartX, pStartY),
                    end = Offset(pEndX, pEndY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Marcadores de escala (Ticks)
            val radius = diameter / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val numTicks = 9
            for (i in 0 until numTicks) {
                val tickPercent = i.toFloat() / (numTicks - 1)
                val angleRad = Math.toRadians((startAngle + sweepAngle * tickPercent).toDouble())
                val startX = center.x + (radius - 10.dp.toPx()) * cos(angleRad).toFloat()
                val startY = center.y + (radius - 10.dp.toPx()) * sin(angleRad).toFloat()
                val endX = center.x + (radius - 2.dp.toPx()) * cos(angleRad).toFloat()
                val endY = center.y + (radius - 2.dp.toPx()) * sin(angleRad).toFloat()

                drawLine(
                    color = if (isActive && tickPercent <= animatedPercent) activeColor else Color.Gray.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Informações centrais
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title.uppercase(),
                color = if (isWarning) RacingRed else TextSecondary,
                fontSize = if (size > 130.dp) 11.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (!isActive) "0" else if (clamped > 999) "%.0f".format(clamped) else "%.1f".format(clamped),
                color = if (isWarning) RacingRed else TextPrimary,
                fontSize = if (size > 130.dp) 24.sp else 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = unit,
                color = if (isWarning) RacingRed else activeColor,
                fontSize = if (size > 130.dp) 12.sp else 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Indicador de Pico numérico
            if (peakValue != null && peakValue > 0f && isActive && size > 130.dp) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "PICO: %.0f".format(peakValue),
                    color = RacingRed.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
