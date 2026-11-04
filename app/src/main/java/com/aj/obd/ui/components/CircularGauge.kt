package com.aj.obd.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
    accentColor: Color = AmberGlow
) {
    val clamped = value.coerceIn(min, max)
    val percentage = (clamped - min) / (max - min)
    val animatedPercent by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 150),
        label = "gauge_anim"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = this.size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            val startAngle = 135f
            val sweepAngle = 270f

            // Fundo do arco
            drawArc(
                color = SurfaceVariantDark,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Arco ativo graduado
            val activeSweep = sweepAngle * animatedPercent
            val gaugeBrush = Brush.sweepGradient(
                colors = listOf(ElectricCyan, AmberGlow, RacingRed)
            )

            drawArc(
                brush = gaugeBrush,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Marcadores de escala (Ticks)
            val radius = diameter / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val numTicks = 9
            for (i in 0 until numTicks) {
                val tickPercent = i.toFloat() / (numTicks - 1)
                val angleRad = Math.toRadians((startAngle + sweepAngle * tickPercent).toDouble())
                val startX = center.x + (radius - 12.dp.toPx()) * cos(angleRad).toFloat()
                val startY = center.y + (radius - 12.dp.toPx()) * sin(angleRad).toFloat()
                val endX = center.x + (radius - 2.dp.toPx()) * cos(angleRad).toFloat()
                val endY = center.y + (radius - 2.dp.toPx()) * sin(angleRad).toFloat()

                drawLine(
                    color = if (tickPercent <= animatedPercent) accentColor else Color.Gray.copy(alpha = 0.4f),
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
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (clamped > 999) "%.0f".format(clamped) else "%.1f".format(clamped),
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = unit,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
