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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
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
import com.aj.obd.core.adapter.ConnectionState
import com.aj.obd.ui.MainViewModel
import com.aj.obd.ui.components.CircularGauge
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
fun DashboardScreen(viewModel: MainViewModel) {
    val sensors by viewModel.sensors.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val isSimActive by viewModel.isSimulatorActive.collectAsState()
    val isPaused by viewModel.isTelemetryPaused.collectAsState()

    val peakRpm by viewModel.peakRpm.collectAsState()
    val peakSpeed by viewModel.peakSpeed.collectAsState()
    val peakTurbo by viewModel.peakTurbo.collectAsState()
    val peakCoolant by viewModel.peakCoolant.collectAsState()

    val rpmSensor = sensors.find { it.id == "rpm" }
    val speedSensor = sensors.find { it.id == "speed" }
    val turboSensor = sensors.find { it.id == "turbo" }
    val coolantSensor = sensors.find { it.id == "coolant" }
    val batterySensor = sensors.find { it.id == "battery" }

    val isConnected = connState is ConnectionState.Connected
    val isEngineRunning = (rpmSensor?.currentValue ?: 0f) > 400f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabeçalho com Título e Botões de Controle (Zerar / Pausar)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COCKPIT EM TEMPO REAL",
                        color = AmberGlow,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isEngineRunning) "Motor em Funcionamento" else if (isConnected) "Chave Ligada / Motor Parado" else "Aguardando Conexão",
                        color = if (isEngineRunning) TelemetryGreen else if (isConnected) AmberGlow else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Botão Pausar / Retomar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPaused) AmberGlow else SurfaceVariantDark)
                            .clickable { viewModel.toggleTelemetryPause() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Retomar" else "Pausar",
                            tint = if (isPaused) Color.Black else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botão Zerar / Limpar Dados
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .clickable { viewModel.resetCockpitData() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Zerar Dados",
                            tint = RacingRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Alerta quando o Modo Simulador estiver rodando dados fictícios
        if (isSimActive) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C05)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Modo Simulação Ativo (Dados de Teste)",
                                color = AmberGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Parar & Zerar",
                            color = RacingRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clickable { viewModel.toggleSimulator(false) }
                        )
                    }
                }
            }
        } else if (!isConnected) {
            // Aviso quando desconectado
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Veículo Desconectado • Indicadores zerados",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Manômetros Principais: Rotação (RPM) e Velocidade
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircularGauge(
                    value = rpmSensor?.currentValue ?: 0f,
                    min = 0f,
                    max = 6000f,
                    title = "Rotação",
                    unit = "RPM",
                    size = 160.dp,
                    peakValue = peakRpm,
                    accentColor = AmberGlow,
                    isActive = isConnected
                )
                CircularGauge(
                    value = speedSensor?.currentValue ?: 0f,
                    min = 0f,
                    max = 220f,
                    title = "Velocidade",
                    unit = "KM/H",
                    size = 160.dp,
                    peakValue = peakSpeed,
                    accentColor = ElectricCyan,
                    isActive = isConnected
                )
            }
        }

        // Manômetros Secundários: Pressão Turbo/MAP, Temperatura da Água e Tensão da Bateria
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircularGauge(
                    value = turboSensor?.currentValue ?: 0f,
                    min = 0f,
                    max = 2.5f,
                    title = "Pressão / MAP",
                    unit = "BAR",
                    size = 110.dp,
                    peakValue = peakTurbo,
                    accentColor = RacingRed,
                    isActive = isConnected
                )
                CircularGauge(
                    value = coolantSensor?.currentValue ?: 0f,
                    min = 0f,
                    max = 130f,
                    title = "Arrefec.",
                    unit = "°C",
                    size = 110.dp,
                    warningThreshold = 102f, // Acima de 102°C dispara alarme visual vermelho
                    accentColor = TelemetryGreen,
                    isActive = isConnected
                )
                CircularGauge(
                    value = batterySensor?.currentValue ?: 0f,
                    min = 0f,
                    max = 16f,
                    title = "Bateria",
                    unit = "VOLTS",
                    size = 110.dp,
                    accentColor = AmberGlow,
                    isActive = isConnected
                )
            }
        }

        // Painel de Recordes / Picos Máximos (Peak Hold)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VALORES MÁXIMOS REGISTRADOS (PICOS)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zerar Picos",
                            color = RacingRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.resetCockpitData() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "RPM Máximo:", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "%.0f RPM".format(peakRpm), color = AmberGlow, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text(text = "Velocidade Máx:", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "%.0f km/h".format(peakSpeed), color = ElectricCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text(text = "Pressão Máx:", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "%.2f bar".format(peakTurbo), color = RacingRed, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Botão de Limpeza Rápida na base do Cockpit
        item {
            OutlinedButton(
                onClick = { viewModel.resetCockpitData() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RacingRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Zerar Todos os Indicadores do Cockpit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
