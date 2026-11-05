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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.obd.core.adapter.ConnectionState
import com.aj.obd.ui.MainViewModel
import com.aj.obd.ui.components.CircularGauge
import com.aj.obd.ui.components.LambdaOscilloscopeView
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
fun DashboardScreen(viewModel: MainViewModel) {
    val sensors by viewModel.sensors.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val isSimActive by viewModel.isSimulatorActive.collectAsState()
    val isPaused by viewModel.isTelemetryPaused.collectAsState()

    val peakRpm by viewModel.peakRpm.collectAsState()
    val peakSpeed by viewModel.peakSpeed.collectAsState()
    val peakTurbo by viewModel.peakTurbo.collectAsState()
    val peakCoolant by viewModel.peakCoolant.collectAsState()

    val isDatalogging by viewModel.isDatalogging.collectAsState()
    val lastLogFile by viewModel.lastLogFile.collectAsState()
    val isHudMode by viewModel.isHudMode.collectAsState()
    val voiceCopilotEnabled by viewModel.voiceCopilotEnabled.collectAsState()
    val tripStats by viewModel.tripStats.collectAsState()
    val lambdaHistory by viewModel.lambdaHistory.collectAsState()
    val currentLambdaVoltage by viewModel.currentLambdaVoltage.collectAsState()

    val rpmSensor = sensors.find { it.id == "rpm" }
    val speedSensor = sensors.find { it.id == "speed" }
    val turboSensor = sensors.find { it.id == "turbo" }
    val coolantSensor = sensors.find { it.id == "coolant" }
    val batterySensor = sensors.find { it.id == "battery" }

    val isConnected = connState is ConnectionState.Connected
    val isEngineRunning = (rpmSensor?.currentValue ?: 0f) > 400f
    val isOverheating = isConnected && (coolantSensor?.currentValue ?: 0f) >= 102f
    val isLowBattery = isConnected && isEngineRunning && (batterySensor?.currentValue ?: 0f) in 0.1f..11.8f

    if (isHudMode) {
        // === MODO HUD (HEAD-UP DISPLAY) PARA REFLEXO NO PARA-BRISA ===
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .graphicsLayer { scaleX = -1f } // Espelha horizontalmente para ficar legível no vidro
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "%.0f".format(speedSensor?.currentValue ?: 0f),
                    color = TelemetryGreen,
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "KM/H",
                    color = TelemetryGreen.copy(alpha = 0.8f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "%.0f RPM  •  %.0f°C".format(
                        rpmSensor?.currentValue ?: 0f,
                        coolantSensor?.currentValue ?: 0f
                    ),
                    color = AmberGlow,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariantDark)
                        .clickable { viewModel.toggleHudMode() }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Sair do Modo HUD",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

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
                    // Botão Co-Piloto por Voz
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (voiceCopilotEnabled) TelemetryGreen.copy(alpha = 0.2f) else SurfaceVariantDark)
                            .clickable { viewModel.toggleVoiceCopilot() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (voiceCopilotEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (voiceCopilotEnabled) "Desativar Voz" else "Ativar Co-piloto por Voz",
                            tint = if (voiceCopilotEnabled) TelemetryGreen else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botão Modo HUD (Para-brisa)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .clickable { viewModel.toggleHudMode() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Modo HUD Para-brisa",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

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

        // Alerta de Superaquecimento do Motor (> 102°C)
        if (isOverheating) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = RacingRed.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RacingRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "⚠️ ALERTA DE SUPERAQUECIMENTO: ${(coolantSensor?.currentValue ?: 0f).toInt()}°C!",
                                color = RacingRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Temperatura da água acima do limite seguro. Verifique a ventoinha e o nível de arrefecimento imediatamente.",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Alerta de Tensão da Bateria / Alternador (< 11.8V com motor ligado)
        if (isLowBattery) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberGlow.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "⚠️ TENSÃO BAIXA DA BATERIA: ${"%.1f".format(batterySensor?.currentValue ?: 0f)}V!",
                                color = AmberGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Com o motor em funcionamento, a tensão deve estar entre 13.5V e 14.5V. Possível defeito no alternador.",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Shift Light Bar (Luzes Sequenciais de Troca de Marcha)
        item {
            ShiftLightBar(
                rpm = rpmSensor?.currentValue ?: 0f,
                shiftRpm = 5300f,
                modifier = Modifier.fillMaxWidth()
            )
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

        // Gráfico do Osciloscópio da Sonda Lambda em Tempo Real
        item {
            LambdaOscilloscopeView(
                history = lambdaHistory,
                currentVoltage = currentLambdaVoltage
            )
        }

        // Computador de Bordo Financeiro (Custo em R$ e Consumo)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TelemetryGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = TelemetryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "COMPUTADOR DE BORDO FINANCEIRO",
                                color = TelemetryGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "Zerar Trajeto",
                            color = RacingRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.resetTrip() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Custo Estimado:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "R$ %.2f".format(tripStats.totalCostReais),
                                color = TelemetryGreen,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Consumo Médio:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "%.1f km/L".format(tripStats.averageConsumptionKmL),
                                color = AmberGlow,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Instantâneo:", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = "%.1f km/L".format(tripStats.instantConsumptionKmL),
                                color = ElectricCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text(text = "Distância:", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = "%.2f km".format(tripStats.totalDistanceKm),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text(text = "Combustível:", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = "%.2f L".format(tripStats.totalFuelUsedLiters),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Painel do Datalogger CSV (Gravação de Telemetria do Trajeto)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (isDatalogging) RacingRed else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DATALOGGER DE TELEMETRIA (CSV)",
                                color = if (isDatalogging) RacingRed else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isDatalogging) {
                            Text(
                                text = "GRAVANDO...",
                                color = RacingRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isDatalogging)
                            "Gravando leituras de RPM, velocidade, temperatura e pressão em arquivo CSV a cada 120ms."
                        else
                            "Grave trajetos completos, acelerações e parâmetros da injeção para analisar em planilha ou software de telemetria.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.toggleDatalogging() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDatalogging) RacingRed else AmberGlow,
                            contentColor = if (isDatalogging) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isDatalogging) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDatalogging) "Parar Gravação & Exportar CSV" else "Iniciar Gravação do Trajeto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
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
