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
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
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
        animationSpec = tween(durationMillis = 180),
        label = "gauge_anim"
    )

    // Efeito pulsar em caso de aviso crítico (ex: temperatura acima de 102°C)
    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Leve pulso de respiração do LED neon de fundo
    val ambientGlow by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_glow"
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
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.minDimension / 2f

            // === 1. BISEL EXTERNO 3D (ANEL METÁLICO USINADO COM CHANFRO) ===
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF282832),
                        Color(0xFF16161C),
                        Color(0xFF0A0A0D)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Borda Chanfrada Iluminada Superior (Efeito Luz Incidente 3D)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF505060),
                        Color(0xFF14141A),
                        Color(0xFF353545)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height)
                ),
                radius = outerRadius - 1.5.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Miolo Central em Fibra de Carbono Fumê Escura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isActive) activeColor.copy(alpha = ambientGlow) else Color(0xFF1A1A22),
                        Color(0xFF101016),
                        Color(0xFF0C0C10)
                    ),
                    center = center,
                    radius = outerRadius * 0.78f
                ),
                radius = outerRadius * 0.78f,
                center = center
            )

            // === 2. ARCOS DA ESCALA DE MEDIÇÃO (ESCALA 270°) ===
            val strokeWidth = (size.toPx() * 0.085f).coerceIn(7.dp.toPx(), 13.dp.toPx())
            val gaugeRadius = outerRadius * 0.74f
            val arcSize = Size(gaugeRadius * 2, gaugeRadius * 2)
            val arcTopLeft = Offset(center.x - gaugeRadius, center.y - gaugeRadius)

            val startAngle = 135f
            val sweepAngle = 270f

            // Trilho Guia Traseiro Escuro
            drawArc(
                color = Color(0xFF22222E),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Arco Ativo com Efeito Neon Gradiente 3D
            val activeSweep = sweepAngle * animatedPercent
            if (activeSweep > 1f && isActive) {
                val gaugeBrush = if (isWarning) {
                    Brush.sweepGradient(
                        colors = listOf(RacingRed.copy(alpha = pulseAlpha), RacingRed),
                        center = center
                    )
                } else {
                    Brush.sweepGradient(
                        colors = listOf(
                            TelemetryGreen,
                            ElectricCyan,
                            AmberGlow,
                            RacingRed
                        ),
                        center = center
                    )
                }

                // Efeito Glow / Halo Suave atrás do arco
                drawArc(
                    color = activeColor.copy(alpha = 0.35f),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Arco Principal
                drawArc(
                    brush = gaugeBrush,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // === 3. TICKS / MARCADORES DE GRADUAÇÃO ESPORTIVOS ===
            val numTicks = 9
            for (i in 0 until numTicks) {
                val tickPct = i.toFloat() / (numTicks - 1)
                val angleRad = Math.toRadians((startAngle + sweepAngle * tickPct).toDouble())
                val innerR = gaugeRadius - strokeWidth * 0.8f
                val outerR = innerR + (if (i % 2 == 0) 6.dp.toPx() else 3.5.dp.toPx())

                val tStartX = center.x + innerR * cos(angleRad).toFloat()
                val tStartY = center.y + innerR * sin(angleRad).toFloat()
                val tEndX = center.x + outerR * cos(angleRad).toFloat()
                val tEndY = center.y + outerR * sin(angleRad).toFloat()

                val isLit = isActive && (tickPct <= animatedPercent)
                drawLine(
                    color = if (isLit) activeColor.copy(alpha = 0.9f) else Color(0xFF383848),
                    start = Offset(tStartX, tStartY),
                    end = Offset(tEndX, tEndY),
                    strokeWidth = if (i % 2 == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // === 4. MARCADOR DE PICO MÁXIMO (PEAK HOLD) ===
            if (peakValue != null && peakValue > min && isActive) {
                val peakClamped = peakValue.coerceIn(min, max)
                val peakPct = (peakClamped - min) / (max - min)
                val peakAngleRad = Math.toRadians((startAngle + sweepAngle * peakPct).toDouble())

                val pStartX = center.x + (gaugeRadius - strokeWidth * 0.7f) * cos(peakAngleRad).toFloat()
                val pStartY = center.y + (gaugeRadius - strokeWidth * 0.7f) * sin(peakAngleRad).toFloat()
                val pEndX = center.x + (gaugeRadius + strokeWidth * 0.7f) * cos(peakAngleRad).toFloat()
                val pEndY = center.y + (gaugeRadius + strokeWidth * 0.7f) * sin(peakAngleRad).toFloat()

                drawLine(
                    color = RacingRed,
                    start = Offset(pStartX, pStartY),
                    end = Offset(pEndX, pEndY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // === 5. AGULHA / PONTEIRO DE TELEMETRIA ILUMINADO (NEEDLE) ===
            if (isActive) {
                val currentAngleRad = Math.toRadians((startAngle + activeSweep).toDouble())
                val needleLen = gaugeRadius * 0.85f
                val needleEndX = center.x + needleLen * cos(currentAngleRad).toFloat()
                val needleEndY = center.y + needleLen * sin(currentAngleRad).toFloat()

                // Feixe luminoso da agulha
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, activeColor),
                        start = center,
                        end = Offset(needleEndX, needleEndY)
                    ),
                    start = center,
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 2.dp.toPx()
                )

                // Ponta incandescente da agulha
                drawCircle(
                    color = activeColor,
                    radius = 3.dp.toPx(),
                    center = Offset(needleEndX, needleEndY)
                )
            }

            // Miolo Central do Pivô da Agulha (Hub Metálico)
            drawCircle(
                color = Color(0xFF14141A),
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = activeColor.copy(alpha = if (isActive) 0.8f else 0.2f),
                radius = 3.dp.toPx(),
                center = center
            )

            // === 6. REFLEXO CURVO DE VIDRO POLARIZADO (LENS FLARE 3D) ===
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, center.y)
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius * 0.88f, center.y - outerRadius * 0.88f),
                size = Size(outerRadius * 1.76f, outerRadius * 1.76f),
                style = Stroke(width = outerRadius * 0.3f)
            )
        }

        // Display Digital Central
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Spacer(modifier = Modifier.height((size * 0.12f)))

            // Título
            Text(
                text = title.uppercase(),
                color = if (isActive) TextSecondary else Color.Gray,
                fontSize = if (size > 140.dp) 11.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            // Valor Numérico
            val displayText = when {
                !isActive -> "---"
                unit.lowercase() == "bar" -> "%.2f".format(value)
                unit.lowercase() == "volts" || unit.lowercase() == "v" -> "%.1f".format(value)
                else -> "%.0f".format(value)
            }

            Text(
                text = displayText,
                color = if (!isActive) TextSecondary else if (isWarning) RacingRed else TextPrimary,
                fontSize = if (size > 140.dp) 24.sp else 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )

            // Unidade
            Text(
                text = unit.uppercase(),
                color = activeColor,
                fontSize = if (size > 140.dp) 11.sp else 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}
