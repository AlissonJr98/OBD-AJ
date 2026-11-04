package com.aj.obd.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.obd.core.adapter.AdapterType
import com.aj.obd.core.adapter.ConnectionState
import com.aj.obd.ui.MainViewModel
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.CarbonDark
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary

@Composable
fun ConnectionScreen(viewModel: MainViewModel) {
    val connState by viewModel.connectionState.collectAsState()
    val isSimActive by viewModel.isSimulatorActive.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "CONEXÃO & ADAPTADOR",
                color = AmberGlow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Gerenciamento de interfaces Bluetooth ELM327 / OBDLink.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Toggle do Simulador Interno
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Simulador Interno",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Permite testar o app sem precisar estar fisicamente no carro",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isSimActive,
                        onCheckedChange = { enable ->
                            viewModel.toggleSimulator(enable)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AmberGlow
                        )
                    )
                }
            }
        }

        // Status Detalhado do Adaptador
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DETALHES DO DISPOSITIVO CONECTADO",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when (val s = connState) {
                        is ConnectionState.Connected -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (s.adapterType.isRecommendedForProxi) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (s.adapterType.isRecommendedForProxi) TelemetryGreen else RacingRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = s.adapterType.label,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Firmware: ${s.firmwareVersion}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Tensão no Conector OBD:", color = TextSecondary, fontSize = 12.sp)
                                Text(text = "%.1f Volts".format(s.batteryVoltage), color = AmberGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Suporte a High-Speed CAN:", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = if (s.isHighSpeedCapable) "Sim (500 kbps)" else "Limitado",
                                    color = if (s.isHighSpeedCapable) TelemetryGreen else RacingRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is ConnectionState.Connecting -> {
                            Text(text = "Conectando ao dispositivo...", color = AmberGlow, fontSize = 13.sp)
                        }
                        is ConnectionState.Error -> {
                            Text(text = "Erro: ${s.message}", color = RacingRed, fontSize = 13.sp)
                        }
                        ConnectionState.Disconnected -> {
                            Text(text = "Nenhum adaptador conectado.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Botões de Conexão
        item {
            if (connState is ConnectionState.Connected) {
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RacingRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Desconectar Adaptador", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.toggleSimulator(isSimActive) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Iniciar Conexão", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
