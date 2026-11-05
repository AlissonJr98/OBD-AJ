package com.aj.obd.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aj.obd.ui.components.FuturisticRadarView
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aj.obd.core.adapter.ConnectionState
import com.aj.obd.domain.model.EcuModule
import com.aj.obd.domain.model.ProtocolType
import com.aj.obd.domain.model.Vehicle
import com.aj.obd.domain.model.VehicleEra
import com.aj.obd.ui.MainViewModel
import com.aj.obd.ui.components.AdapterCableBadge
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.CarbonDark
import com.aj.obd.ui.theme.ElectricCyan
import com.aj.obd.ui.theme.RacingRed
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.SurfaceVariantDark
import com.aj.obd.ui.theme.TelemetryGreen
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToDtc: () -> Unit,
    onNavigateToProxi: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onNavigateToCables: () -> Unit,
    onNavigateToConnection: () -> Unit
) {
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val selectedEcu by viewModel.selectedEcu.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val autoDetectStatus by viewModel.autoDetectStatus.collectAsState()
    val isAutoDetecting by viewModel.isAutoDetecting.collectAsState()
    val licenseState by viewModel.licenseState.collectAsState()

    var showVehicleSelectorSheet by remember { mutableStateOf(false) }
    var showEcuSelectorSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logotipo e Identidade da Marca
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.aj.obd.R.drawable.logo_obdaj),
                    contentDescription = "Logo OBD AJ",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OBD AJ",
                            color = AmberGlow,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(AmberGlow, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(
                        text = "Scanner & Diagnóstico Automotivo Especializado",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Status da Conexão / Adaptador (Design 3D Futurista com Holograma e Radar)
        item {
            val isConnected = connState is ConnectionState.Connected
            val isConnecting = connState is ConnectionState.Connecting

            // Transição infinita para pulsação luminosa de neon
            val infiniteTransition = rememberInfiniteTransition(label = "pulse_neon")
            val neonAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "neon_alpha"
            )

            val statusColor = when (connState) {
                is ConnectionState.Connected -> TelemetryGreen
                is ConnectionState.Connecting -> AmberGlow
                is ConnectionState.Error -> RacingRed
                ConnectionState.Disconnected -> AmberGlow
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                statusColor.copy(alpha = if (isConnected) 0.9f else neonAlpha),
                                Color(0xFF202028),
                                ElectricCyan.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onNavigateToConnection() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    // Mini Radar 3D com Varredura e Pulso
                    FuturisticRadarView(
                        size = 52.dp,
                        isActive = isConnected || isConnecting
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SCANNER AUTOMOTIVO",
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = when (val s = connState) {
                                is ConnectionState.Connected -> s.deviceName
                                is ConnectionState.Connecting -> "Conectando..."
                                is ConnectionState.Error -> "Falha na Conexão"
                                ConnectionState.Disconnected -> "Desconectado"
                            },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = when (val s = connState) {
                                is ConnectionState.Connected -> "${s.adapterType.label} • ${"%.1f".format(s.batteryVoltage)}V"
                                is ConnectionState.Connecting -> s.step
                                is ConnectionState.Error -> s.message
                                ConnectionState.Disconnected -> "Toque aqui para conectar via Bluetooth"
                            },
                            color = if (isConnected) ElectricCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Botão Visual 3D
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isConnected) TelemetryGreen.copy(alpha = 0.2f)
                                else AmberGlow.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (isConnected) TelemetryGreen else AmberGlow,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isConnected) "ONLINE" else "CONECTAR",
                            color = if (isConnected) TelemetryGreen else AmberGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Seção: Veículo Selecionado com Botão de Filtro
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VEÍCULO SELECIONADO",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Filtrar / Trocar",
                    color = AmberGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showVehicleSelectorSheet = true }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVehicleSelectorSheet = true }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberGlow.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = AmberGlow,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedVehicle.brand} ${selectedVehicle.name}",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedVehicle.years} • ${selectedVehicle.era.label}",
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Trocar",
                            tint = TextSecondary
                        )
                    }

                    if (selectedVehicle.engineOptions.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Motorizações: ${selectedVehicle.engineOptions}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Seção: ECU / Central Ativa & Auto-Detecção
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CENTRAL ELETRÔNICA (ECU)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Selecionar Módulo",
                    color = AmberGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showEcuSelectorSheet = true }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedEcu?.name ?: "Nenhum módulo selecionado",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sistema: ${selectedEcu?.category?.label ?: ""}",
                                color = AmberGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Protocolo: ${selectedEcu?.protocol?.name} • Baud: ${selectedEcu?.baudRate ?: 10400} bps",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    selectedEcu?.let { ecu ->
                        AdapterCableBadge(cable = ecu.adapterCable)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botão Auto-Detectar Central
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.autoDetectEcu() },
                            enabled = !isAutoDetecting,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isAutoDetecting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isAutoDetecting) "Interrogando..." else "Auto-Detectar ECU", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Se for Fiat K-Line (ex: Palio 2001), exibe botão de Reset Autoadaptativos
                        if (selectedEcu?.protocol == ProtocolType.KLINE_KWP2000_FAST) {
                            OutlinedButton(
                                onClick = { viewModel.resetAutoadaptiveParameters() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGlow),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Reset Parâmetros", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Mensagem de status da auto-detecção
                    if (!autoDetectStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceVariantDark, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = autoDetectStatus ?: "",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Ações Rápidas
        item {
            Text(
                text = "AÇÕES DE DIAGNÓSTICO",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Cockpit",
                    subtitle = "Gauges & HUD",
                    icon = Icons.Default.Speed,
                    tint = AmberGlow,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDashboard
                )
                QuickActionCard(
                    title = "Falhas & Laudo",
                    subtitle = "DTC & PDF",
                    icon = Icons.Default.Warning,
                    tint = RacingRed,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDtc
                )
                QuickActionCard(
                    title = "Proxi Fácil",
                    subtitle = "Ativações BCM",
                    icon = Icons.Default.Build,
                    tint = ElectricCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToProxi
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Arrancada",
                    subtitle = "0-100 & Drag",
                    icon = Icons.Default.Timer,
                    tint = TelemetryGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPerformance
                )
                QuickActionCard(
                    title = "Cabos Fiat",
                    subtitle = "Pinos A1/A2/A3",
                    icon = Icons.Default.Cable,
                    tint = Color(0xFFAB47BC),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCables
                )
            }
        }

        // Rodapé com Botão de Verificar Atualizações
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { viewModel.checkForUpdates(silent = false) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGlow),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verificar Atualizações no App (v${com.aj.obd.BuildConfig.VERSION_NAME})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal Bottom Sheet: Filtro e Seleção Completa de Veículos
    if (showVehicleSelectorSheet) {
        VehicleSelectionBottomSheet(
            viewModel = viewModel,
            onDismiss = { showVehicleSelectorSheet = false },
            onVehicleSelected = { vehicle ->
                viewModel.selectVehicle(vehicle)
                showVehicleSelectorSheet = false
            }
        )
    }

    // Modal Bottom Sheet: Seleção de ECU do Veículo
    if (showEcuSelectorSheet) {
        EcuSelectionBottomSheet(
            vehicle = selectedVehicle,
            selectedEcu = selectedEcu,
            onDismiss = { showEcuSelectorSheet = false },
            onEcuSelected = { ecu ->
                viewModel.selectEcu(ecu)
                showEcuSelectorSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSelectionBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onVehicleSelected: (Vehicle) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vehicles by viewModel.filteredVehicles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBrand by viewModel.selectedBrandFilter.collectAsState()
    val selectedEra by viewModel.selectedEraFilter.collectAsState()

    val brands = listOf("Todas", "Fiat", "Jeep", "RAM", "Alfa Romeo")
    val eras = VehicleEra.values()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "ESCOLHA O VEÍCULO",
                color = AmberGlow,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Campo de Busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Buscar modelo (ex: Palio 2001, Uno, Marea, Toro)...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AmberGlow) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberGlow,
                    unfocusedBorderColor = SurfaceVariantDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filtro de Marcas (Chips)
            Text(text = "Marca / Fabricante:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(brands) { brand ->
                    val isSel = brand == selectedBrand
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) AmberGlow else SurfaceVariantDark)
                            .clickable { viewModel.updateBrandFilter(brand) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = brand,
                            color = if (isSel) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtro de Épocas (Chips)
            Text(text = "Época / Tecnologia:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(eras) { era ->
                    val isSel = era == selectedEra
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) ElectricCyan else SurfaceVariantDark)
                            .clickable { viewModel.updateEraFilter(era) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = era.label.substringBefore("(").trim(),
                            color = if (isSel) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista Filtrada de Veículos
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vehicles) { v ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVehicleSelected(v) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${v.brand} ${v.name}",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = v.years,
                                    color = AmberGlow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (v.engineOptions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = v.engineOptions,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcuSelectionBottomSheet(
    vehicle: Vehicle,
    selectedEcu: EcuModule?,
    onDismiss: () -> Unit,
    onEcuSelected: (EcuModule) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "MÓDULOS DE ${vehicle.name.uppercase()}",
                color = AmberGlow,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Escolha o sistema eletrônico que deseja diagnosticar:",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vehicle.supportedEcus) { ecu ->
                    val isSel = ecu.id == selectedEcu?.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) AmberGlow.copy(alpha = 0.15f) else SurfaceVariantDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEcuSelected(ecu) }
                            .then(
                                if (isSel) Modifier.border(1.dp, AmberGlow, RoundedCornerShape(8.dp)) else Modifier
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ecu.name,
                                    color = if (isSel) AmberGlow else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (ecu.isDefault) {
                                    Box(
                                        modifier = Modifier
                                            .background(TelemetryGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "PADRÃO", color = TelemetryGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${ecu.category.label} • ${ecu.protocol.name} • ${ecu.adapterCable.label}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Animação de respiração suave e divertida para os ícones
    val infiniteTransition = rememberInfiniteTransition(label = "icon_fun_anim")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.6f),
                        Color(0xFF22222E),
                        tint.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Círculo 3D com Halo Neon e Escala Animada
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tint.copy(alpha = 0.35f),
                                tint.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(1.dp, tint.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(iconScale)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
