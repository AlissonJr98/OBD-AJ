package com.aj.obd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.obd.core.performance.DragMeterState
import com.aj.obd.ui.MainViewModel
import com.aj.obd.ui.components.ShiftLightBar
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.CarbonDark
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.SurfaceVariantDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary

@Composable
fun PerformanceScreen(viewModel: MainViewModel) {
    val runData by viewModel.performanceRun.collectAsState()
    val meterState by viewModel.dragMeterState.collectAsState()
    val dynoResult by viewModel.dynoResult.collectAsState()
    val sensors by viewModel.sensors.collectAsState()
    val speedKmh = sensors.find { it.id == "speed" }?.currentValue ?: 0f
    val currentRpm = sensors.find { it.id == "rpm" }?.currentValue ?: 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabeçalho
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TESTE DE ARRANCADA (0-100 KM/H)",
                        color = AmberGlow,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Medição precisa de arrancada e aceleração via telemetria",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                        .clickable { viewModel.resetPerformanceMeter() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Zerar Teste",
                        tint = RacingRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Status da Arrancada (Pronto / Medindo / Concluído)
        item {
            val (statusText, statusColor) = when (meterState) {
                DragMeterState.READY_WAITING_LAUNCH -> "🟢 PRONTO • PARE O VEÍCULO E ACELERE!" to TelemetryGreen
                DragMeterState.MEASURING -> "🔥 ACELERANDO! MEDIÇÃO EM ANDAMENTO..." to AmberGlow
                DragMeterState.COMPLETED -> "🏁 PUXADA FINALIZADA COM SUCESSO!" to ElectricCyan
            }

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Shift Light Bar (F1 Sequencial de Troca de Marcha)
        item {
            ShiftLightBar(
                rpm = currentRpm,
                shiftRpm = 5300f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Velocímetro Gigante & Força G
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VELOCIDADE ATUAL",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%.0f".format(speedKmh),
                        color = TextPrimary,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "KM/H",
                        color = AmberGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Força G Máx:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "%.2f G".format(runData.maxAccelG),
                                color = ElectricCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Velocidade Máx:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "%.0f km/h".format(runData.topSpeedKmh),
                                color = RacingRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Tabela de Tempos Medidos
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CRONOMETRAGEM POR ETAPA",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TimeRow(
                        label = "0 a 60 km/h",
                        timeMs = runData.time0to60Ms,
                        accentColor = TelemetryGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeRow(
                        label = "0 a 100 km/h",
                        timeMs = runData.time0to100Ms,
                        accentColor = AmberGlow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeRow(
                        label = "100 metros",
                        timeMs = runData.time100mMs,
                        accentColor = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeRow(
                        label = "201 metros (1/8 milha)",
                        timeMs = runData.time201mMs,
                        accentColor = Color(0xFFAB47BC)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeRow(
                        label = "402 metros (1/4 de milha)",
                        timeMs = runData.time402mMs,
                        accentColor = RacingRed
                    )
                }
            }
        }

        // Botão de Nova Puxada
        item {
            Button(
                onClick = { viewModel.resetPerformanceMeter() },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Preparar para Nova Arrancada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Dinamômetro Virtual (Curva de Potência e Torque na Roda)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmberGlow.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DINAMÔMETRO VIRTUAL (POTÊNCIA & TORQUE)",
                                color = AmberGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (dynoResult.isRecording) {
                            Text(
                                text = "MEDINDO...",
                                color = RacingRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Acelere em 2ª ou 3ª marcha com pé no fundo até o corte de rotação para calcular a potência em CV e o torque em kgfm nas rodas.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Potência Máxima:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "%.0f CV".format(dynoResult.peakPowerCv),
                                color = AmberGlow,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "@ %.0f RPM".format(dynoResult.peakPowerRpm),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Torque Máximo:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "%.1f kgfm".format(dynoResult.peakTorqueKgfm),
                                color = ElectricCyan,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "@ %.0f RPM".format(dynoResult.peakTorqueRpm),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (dynoResult.isRecording) {
                        Button(
                            onClick = { viewModel.stopDyno() },
                            colors = ButtonDefaults.buttonColors(containerColor = RacingRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Finalizar Puxada & Ver Resultado", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startDyno() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Iniciar Puxada Dinamômetro", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetDyno() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "Zerar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeRow(
    label: String,
    timeMs: Long?,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariantDark, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (timeMs != null) "%.2f s".format(timeMs / 1000f) else "--.-- s",
            color = if (timeMs != null) accentColor else TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
