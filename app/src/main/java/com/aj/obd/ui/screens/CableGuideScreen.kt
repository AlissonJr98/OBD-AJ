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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.CarbonDark
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.SurfaceVariantDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary

enum class FiatCable(
    val title: String,
    val colorName: String,
    val badgeColor: Color,
    val pins: List<Int>,
    val description: String,
    val modulesCovered: String,
    val jumperTip: String
) {
    STANDARD(
        "Cabo Padrão (Sem Adaptador)",
        "Preto",
        Color(0xFF9E9E9E),
        listOf(4, 5, 7, 16),
        "Conexão direta do scanner ELM327 na porta OBD2 do veículo sem nenhum cabo intermediário.",
        "Injeção Eletrônica (Motor / ECU Magneti Marelli ou Bosch), Câmbio Automatizado Dualogic.",
        "Pino 7 é a Linha K padrão de injeção em todos os veículos pós-2000."
    ),
    ADAPTER_A1(
        "Adaptador A1 (Verde)",
        "Verde",
        TelemetryGreen,
        listOf(1, 4, 5, 7, 16),
        "Redireciona a linha de comunicação K-Line do Pino 7 do scanner para o Pino 1 do conector do veículo.",
        "Módulo de Freios ABS (Bosch 5.3 / 5.7), Sistema de Controle de Tração e Direção Elétrica.",
        "Jumper caseiro: una o pino 7 do scanner ao pino 1 do conector do carro com um pequeno pedaço de fio de cobre encapado."
    ),
    ADAPTER_A2(
        "Adaptador A2 (Vermelho)",
        "Vermelho",
        RacingRed,
        listOf(3, 4, 5, 7, 16),
        "Redireciona a linha K-Line do Pino 7 do scanner para o Pino 3 do veículo.",
        "Módulo de Airbag (TRW, AlliedSignal, Continental, Siemens).",
        "Jumper caseiro: conecte o pino 7 do scanner ao pino 3 do conector OBD do veículo para apagar a luz de airbag."
    ),
    ADAPTER_A3(
        "Adaptador A3 (Amarelo)",
        "Amarelo",
        AmberGlow,
        listOf(6, 14, 1, 9, 12),
        "Cruza os sinais CAN-High e CAN-Low para acessar a rede de baixa velocidade (CAN Low Speed / B-CAN).",
        "Body Computer (BCM), Painel de Instrumentos, Alinhamento de Proxi, Direção Elétrica Dualdrive e Ar Condicionado.",
        "Essencial para Fiat Punto, Bravo, Palio G3/G4, Novo Uno, Palio Attractive, Grand Siena e Toro."
    ),
    ADAPTER_A4(
        "Adaptador A4 (Roxo)",
        "Roxo",
        Color(0xFFAB47BC),
        listOf(9, 4, 5, 16),
        "Redireciona a comunicação para o Pino 9 da porta OBD2.",
        "Módulo da Capota Elétrica (Stilo / 500 Cabrio) e Faróis de Xenon com nivelamento automático.",
        "Jumper caseiro: pino 7 do scanner conectado diretamente ao pino 9 da tomada OBD2."
    )
}

@Composable
fun CableGuideScreen() {
    var selectedCable by remember { mutableStateOf(FiatCable.STANDARD) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Título da Tela
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cable, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GUIA DE PINOS E CABOS FIAT",
                        color = AmberGlow,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Saiba exatamente qual adaptador ou jumper usar para acessar ABS, Airbag, Motor e BCM",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Seletor de Cabos em Carrossel / Botões
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiatCable.values().forEach { cable ->
                    val isSelected = selectedCable == cable
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) cable.badgeColor.copy(alpha = 0.25f) else SurfaceDark)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) cable.badgeColor else SurfaceVariantDark,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCable = cable }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(cable.badgeColor)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (cable) {
                                    FiatCable.STANDARD -> "Padrão"
                                    FiatCable.ADAPTER_A1 -> "A1 Verde"
                                    FiatCable.ADAPTER_A2 -> "A2 Verm."
                                    FiatCable.ADAPTER_A3 -> "A3 Amar."
                                    FiatCable.ADAPTER_A4 -> "A4 Roxo"
                                },
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Diagrama Interativo do Conector OBD2 (16 Pinos)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CONECTOR OBD2 FEMEA DO VEÍCULO (VISTA FRONTAL)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Linha Superior: Pinos 1 a 8
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (pin in 1..8) {
                            ObdPinBadge(
                                pinNumber = pin,
                                isHighlighted = selectedCable.pins.contains(pin),
                                highlightColor = selectedCable.badgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Linha Inferior: Pinos 9 a 16
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (pin in 9..16) {
                            ObdPinBadge(
                                pinNumber = pin,
                                isHighlighted = selectedCable.pins.contains(pin),
                                highlightColor = selectedCable.badgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pinos iluminados representam as conexões ativas do ${selectedCable.colorName}",
                        color = selectedCable.badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Detalhes do Cabo Selecionado
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCable.title,
                            color = selectedCable.badgeColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .background(selectedCable.badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "COR: ${selectedCable.colorName.uppercase()}",
                                color = selectedCable.badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = selectedCable.description,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Módulos Acessados por este Cabo:",
                        color = AmberGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedCable.modulesCovered,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dica Profissional de Bancada / Jumper
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Dica Profissional de Bancada (Jumper Rápido):",
                                    color = AmberGlow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedCable.jumperTip,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ObdPinBadge(
    pinNumber: Int,
    isHighlighted: Boolean,
    highlightColor: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isHighlighted) highlightColor else SurfaceVariantDark)
            .border(
                width = if (isHighlighted) 1.5.dp else 0.5.dp,
                color = if (isHighlighted) Color.White else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pinNumber.toString(),
            color = if (isHighlighted) Color.Black else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
