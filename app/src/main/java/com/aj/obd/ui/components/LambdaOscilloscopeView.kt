package com.aj.obd.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary

@Composable
fun LambdaOscilloscopeView(
    history: List<Float>,
    currentVoltage: Float = 0.45f,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        currentVoltage >= 0.75f -> "Mistura Rica (Excesso de Combustível)"
        currentVoltage <= 0.25f -> "Mistura Pobre (Falta de Combustível / Entrada de Ar)"
        else -> "Oscilação Perfeita (Mistura Estequiométrica)"
    }

    val statusColor = when {
        currentVoltage >= 0.75f -> AmberGlow
        currentVoltage <= 0.25f -> RacingRed
        else -> TelemetryGreen
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OSCILOSCÓPIO DA SONDA LAMBDA (O2)",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "%.3f V".format(currentVoltage),
                    color = statusColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Diagnóstico: $statusText",
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tela do Osciloscópio (Grid verde/ciano tipo bancada automotiva)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color(0xFF0C1014), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF1E2832), RoundedCornerShape(6.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(95.dp)) {
                    val w = this.size.width
                    val h = this.size.height

                    // Grade horizontal (0.1V a 0.9V)
                    for (i in 1..4) {
                        val y = h * (i / 5f)
                        drawLine(
                            color = Color(0xFF142028),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    // Linha média ideal estequiométrica (0.45V)
                    val midY = h * 0.5f
                    drawLine(
                        color = TelemetryGreen.copy(alpha = 0.35f),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1.5f
                    )

                    // Traçado da Onda Senoidal em Tempo Real
                    if (history.isNotEmpty()) {
                        val path = Path()
                        val stepX = w / (history.size - 1).coerceAtLeast(1)

                        history.forEachIndexed { index, volt ->
                            val clamped = volt.coerceIn(0.05f, 0.95f)
                            // 0.95V = topo (y = 5), 0.05V = base (y = h - 5)
                            val y = h - ((clamped - 0.05f) / (0.95f - 0.05f)) * (h - 10f) - 5f
                            val x = index * stepX

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = ElectricCyan,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0.1V (Pobre)", color = TextSecondary, fontSize = 9.sp)
                Text(text = "0.45V (Estequiométrica)", color = TelemetryGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "0.9V (Rica)", color = TextSecondary, fontSize = 9.sp)
            }
        }
    }
}
