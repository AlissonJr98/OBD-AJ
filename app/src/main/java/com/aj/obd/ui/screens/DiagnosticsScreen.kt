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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aj.obd.core.ai.AiDiagnosticReport
import com.aj.obd.domain.model.DtcFault
import com.aj.obd.ui.MainViewModel
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
fun DiagnosticsScreen(viewModel: MainViewModel) {
    val dtcs by viewModel.dtcFaults.collectAsState()
    val isScanning by viewModel.isScanningDtc.collectAsState()
    val selectedEcu by viewModel.selectedEcu.collectAsState()
    val actuatorMsg by viewModel.actuatorTestMessage.collectAsState()
    val autoDetectStatus by viewModel.autoDetectStatus.collectAsState()
    val flexFuelStatus by viewModel.flexFuelStatus.collectAsState()
    val batteryTestResult by viewModel.batteryTestResult.collectAsState()
    val selectedAiReport by viewModel.selectedAiReport.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: DTCs, 1: Atuadores & Procedimentos

    // Diálogo com Laudo da Inteligência Artificial do Mecânico
    if (selectedAiReport != null) {
        AiDiagnosticDialog(
            report = selectedAiReport!!,
            onDismiss = { viewModel.dismissAiReport() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(16.dp)
    ) {
        // Título e Central Ativa
        Text(
            text = "DIAGNÓSTICO AVANÇADO",
            color = AmberGlow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            text = "Central ativa: ${selectedEcu?.name ?: "Injeção Eletrônica Geral"}",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Abas: Falhas (DTC) vs Atuadores & Calibrações
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = AmberGlow,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AmberGlow
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "FALHAS (DTC)",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) AmberGlow else TextSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "ATUADORES & RESET",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) AmberGlow else TextSecondary
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // === ABA 1: CÓDIGOS DE FALHA (DTC) ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.scanAllDtcs() },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isScanning) "Varrendo..." else "Buscar Falhas", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.clearAllDtcs() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RacingRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Apagar Falhas", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão Gerar Laudo Técnico em PDF
            OutlinedButton(
                onClick = { viewModel.generatePdfReport() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Gerar Laudo Técnico em PDF (WhatsApp / Oficina)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (dtcs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TelemetryGreen,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhuma falha gravada na central!",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toque em 'Buscar Falhas' para realizar a varredura.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dtcs) { dtc ->
                        DtcItemCard(
                            dtc = dtc,
                            onDiagnoseWithAi = { viewModel.diagnoseDtcWithAi(it) }
                        )
                    }
                }
            }
        } else {
            // === ABA 2: TESTES DE ATUADORES & CALIBRAÇÃO (FIAT PALIO / KWP2000 / UDS) ===
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mensagem de Status do Atuador
                if (!actuatorMsg.isNullOrBlank() || !autoDetectStatus.isNullOrBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = actuatorMsg ?: autoDetectStatus ?: "",
                                color = AmberGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Procedimento Especial: Reset Autoadaptativo (Marelli 4AF/4EF/59F)
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ajuste Forçado de Combustível A/F (Flex)",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mistura atual detectada: %.1f : 1 (%s)".format(flexFuelStatus.currentAfRatio, flexFuelStatus.statusDescription),
                                color = ElectricCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Evite problemas de partida a frio quando a central perde o aprendizado após abastecer com Etanol ou Gasolina.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.forceGasolineAf() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Forçar Gasolina (13.2)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.forceEthanolAf() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TelemetryGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Forçar Etanol (9.0)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Procedimento Especial: Reset de Revisão (Chave de Boca) & Troca de Óleo
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reset de Revisão & Troca de Óleo (Painel)",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Apague a luz da 'chave de boca' no painel de instrumentos após realizar a revisão e redefina o contador de degradação do óleo lubrificante para 100% novo.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.resetServiceInterval() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Reset Chave de Boca", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.resetOilDegradation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Reset Óleo 100%", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Procedimento Especial: Calibração Robótica Dualogic / GSR
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
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
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Câmbio Dualogic / GSR (Marelli CFC)",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Especial Fiat",
                                    color = ElectricCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Calibre o ponto de engate da embreagem (Kiss-Point) para eliminar trancos na saída e marchas. Realize sangria antes da troca do fluido Tutela CS Speed e zere o desgaste da embreagem.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.calibrateDualogicClutch() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Calibrar Embreagem (Kiss-Point)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.bleedDualogicHydraulics() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Sangria Hidráulica", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetDualogicClutchDegradation() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TelemetryGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Zerar Desgaste", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Procedimento Especial: Reset Geral dos Autoadaptativos
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reset Parâmetros Autoadaptativos",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reconhecimento de combustível (A/F), aprendizado do corpo de borboleta (TBI) e adaptação de marcha lenta. Essencial para Fiat Palio Fire 2001 (Magneti Marelli IAW 4AF / 4EF / 59F).",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.resetAutoadaptiveParameters() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Executar Reset da Injeção", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Procedimento Especial: Teste de Bateria & Queda no Arranque (Cranking Test)
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TelemetryGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = TelemetryGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Teste de Partida & Saúde da Bateria",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${batteryTestResult.healthPercentage}% Saúde",
                                    color = if (batteryTestResult.healthPercentage >= 75) TelemetryGreen else AmberGlow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mede a queda instantânea de tensão durante a partida do motor. Queda abaixo de 9.6V indica bateria esgotada ou motor de arranque pesado.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Repouso:", color = TextSecondary, fontSize = 10.sp)
                                    Text(
                                        text = "%.1fV".format(batteryTestResult.restingVoltage),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Queda Arranque:", color = TextSecondary, fontSize = 10.sp)
                                    Text(
                                        text = "%.1fV".format(batteryTestResult.crankingVoltage),
                                        color = if (batteryTestResult.crankingVoltage >= 9.6f) TelemetryGreen else RacingRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Alternador:", color = TextSecondary, fontSize = 10.sp)
                                    Text(
                                        text = "%.1fV".format(batteryTestResult.chargingVoltage),
                                        color = ElectricCyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = batteryTestResult.statusText,
                                color = if (batteryTestResult.healthPercentage >= 75) TelemetryGreen else AmberGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = batteryTestResult.recommendations,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.startBatteryTest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TelemetryGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "1. Iniciar Teste", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.finishBatteryTest() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "2. Ver Laudo", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Procedimento Especial: Calibração de Roda Fônica (Rotina 5.000 RPM Fiat Fire)
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Calibração de Roda Fônica (Fiat Fire P1300)",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Procedimento obrigatório após troca de correia dentada, sensor de rotação ou reset da injeção. Com o motor aquecido, acelere 3 vezes a 5.000 RPM para apagar a luz de injeção piscando.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.calibratePhonicWheelFiat() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Acionar Rotina de Roda Fônica", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Teste de Atuador: Ventoinha do Radiador (Velocidade Baixa / Relé 1)
                item {
                    ActuatorCard(
                        title = "Eletroventilador - 1ª Velocidade (Baixa)",
                        description = "Aciona o relé da ventoinha do radiador em velocidade baixa por 5 segundos para testar o circuito elétrico e resistência.",
                        buttonText = "Testar Ventoinha 1",
                        onTest = { viewModel.executeActuatorTest("Ventoinha 1ª Velocidade", "31 01 02") }
                    )
                }

                // Teste de Atuador: Ventoinha do Radiador (Velocidade Alta / Relé 2)
                item {
                    ActuatorCard(
                        title = "Eletroventilador - 2ª Velocidade (Alta)",
                        description = "Aciona a velocidade máxima da ventoinha para verificar o relé de potência e o fusível de alta amperagem.",
                        buttonText = "Testar Ventoinha 2",
                        onTest = { viewModel.executeActuatorTest("Ventoinha 2ª Velocidade", "31 01 03") }
                    )
                }

                // Teste de Atuador: Bomba de Combustível
                item {
                    ActuatorCard(
                        title = "Bomba de Combustível & Relé Principal",
                        description = "Energiza a bomba de combustível por 4 segundos para testar pressurização da linha e estanqueidade.",
                        buttonText = "Testar Bomba de Combustível",
                        onTest = { viewModel.executeActuatorTest("Bomba de Combustível", "31 01 01") }
                    )
                }

                // Teste de Atuador: Lâmpada de Avaria (MIL) / Painel
                item {
                    ActuatorCard(
                        title = "Lâmpada de Injeção no Painel (MIL)",
                        description = "Pisca a luz de advertência da injeção no quadro de instrumentos para testar a lâmpada/LED.",
                        buttonText = "Testar Luz de Injeção",
                        onTest = { viewModel.executeActuatorTest("Luz da Injeção (MIL)", "31 01 05") }
                    )
                }

                // Teste de Atuador: Bicos Injetores
                item {
                    ActuatorCard(
                        title = "Pulsar Bicos Injetores (Cilindros 1-4)",
                        description = "Envia pulsos de teste para conferir acionamento dos injetores e chicote de injeção.",
                        buttonText = "Testar Bicos Injetores",
                        onTest = { viewModel.executeActuatorTest("Bicos Injetores", "31 01 04") }
                    )
                }
            }
        }
    }
}

@Composable
fun ActuatorCard(
    title: String,
    description: String,
    buttonText: String,
    onTest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
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
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onTest,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = buttonText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun DtcItemCard(
    dtc: DtcFault,
    onDiagnoseWithAi: (DtcFault) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(10.dp),
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
                    text = dtc.code,
                    color = RacingRed,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .background(Color(dtc.severity.colorHex).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dtc.severity.label.uppercase(),
                        color = Color(dtc.severity.colorHex),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = dtc.description,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Módulo de Origem: ${dtc.ecuSource}",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onDiagnoseWithAi(dtc) },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Diagnosticar com AJ IA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AiDiagnosticDialog(
    report: AiDiagnosticReport,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .border(1.5.dp, AmberGlow.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DIAGNÓSTICO AJ IA",
                            color = AmberGlow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = report.dtcCode,
                        color = RacingRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = report.dtcDescription,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Resumo do Problema
                Text(
                    text = "O QUE SIGNIFICA:",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.summary,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Causas mais prováveis
                Text(
                    text = "CAUSAS MAIS PROVÁVEIS:",
                    color = AmberGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                report.probableCauses.forEach { cause ->
                    Text(
                        text = "• $cause",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Passo-a-passo de testes
                Text(
                    text = "GUIA DE TESTES DO MECÂNICO:",
                    color = TelemetryGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                report.testSteps.forEach { step ->
                    Text(
                        text = step,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Peças recomendadas
                Text(
                    text = "PEÇAS PARA REVISAR / SUBSTITUIR:",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                report.recommendedParts.forEach { part ->
                    Text(
                        text = "🔧 $part",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Entendido / Fechar Laudo", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
