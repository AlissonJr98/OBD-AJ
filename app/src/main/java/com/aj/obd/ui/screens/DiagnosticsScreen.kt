package com.aj.obd.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
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

    var selectedTab by remember { mutableStateOf(0) } // 0: DTCs, 1: Atuadores & Procedimentos

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
                        DtcItemCard(dtc = dtc)
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
fun DtcItemCard(dtc: DtcFault) {
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
        }
    }
}
