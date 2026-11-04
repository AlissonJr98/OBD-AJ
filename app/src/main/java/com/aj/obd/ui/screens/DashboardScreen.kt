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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.obd.ui.MainViewModel
import com.aj.obd.ui.components.CircularGauge
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.CarbonDark
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val sensors by viewModel.sensors.collectAsState()

    val rpmSensor = sensors.find { it.id == "rpm" }
    val speedSensor = sensors.find { it.id == "speed" }
    val turboSensor = sensors.find { it.id == "turbo" }
    val coolantSensor = sensors.find { it.id == "coolant" }
    val batterySensor = sensors.find { it.id == "battery" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "COCKPIT EM TEMPO REAL",
                color = AmberGlow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        // Manômetros Principais: RPM e Velocidade
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
                    accentColor = AmberGlow
                )
                CircularGauge(
                    value = speedSensor?.currentValue ?: 0f,
                    min = 0f,
                    max = 220f,
                    title = "Velocidade",
                    unit = "KM/H",
                    size = 160.dp,
                    accentColor = ElectricCyan
                )
            }
        }

        // Manômetros Secundários: Turbo, Temperatura e Bateria
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircularGauge(
                    value = turboSensor?.currentValue ?: 0f,
                    min = 0f,
                    max = 2.5f,
                    title = "Turbo",
                    unit = "BAR",
                    size = 110.dp,
                    accentColor = RacingRed
                )
                CircularGauge(
                    value = coolantSensor?.currentValue ?: 90f,
                    min = 40f,
                    max = 130f,
                    title = "Arrefec.",
                    unit = "°C",
                    size = 110.dp,
                    accentColor = TelemetryGreen
                )
                CircularGauge(
                    value = batterySensor?.currentValue ?: 12.6f,
                    min = 10f,
                    max = 16f,
                    title = "Bateria",
                    unit = "VOLTS",
                    size = 110.dp,
                    accentColor = AmberGlow
                )
            }
        }

        // Barra de Informações Complementares
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TELEMETRIA ATIVA",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Taxa de Atualização CAN:", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "8.3 Hz (~120ms)", color = ElectricCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Linha de Comunicação:", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "ISO 15765-4 (CAN 500k)", color = TelemetryGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
