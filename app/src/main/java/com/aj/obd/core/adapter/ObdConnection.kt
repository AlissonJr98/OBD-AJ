package com.aj.obd.core.adapter

import kotlinx.coroutines.flow.StateFlow

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val deviceName: String, val step: String) : ConnectionState
    data class Connected(
        val deviceName: String,
        val adapterType: AdapterType,
        val firmwareVersion: String,
        val batteryVoltage: Float,
        val isHighSpeedCapable: Boolean,
        val isEcuConnected: Boolean = false,
        val detectedProtocol: String = "OBD-II Auto",
        val ignitionStatus: String = "Pós-Chave Desligado",
        val packetsPerSecond: Int = 0
    ) : ConnectionState
    data class Error(val message: String, val canRetry: Boolean = true) : ConnectionState
}

enum class AdapterType(val label: String, val isRecommendedForProxi: Boolean) {
    OBD_LINK_GENUINE("OBDLink (Genuíno STN)", true),
    V_LINKER_MC("vLinker MC+ / FS", true),
    ELM327_V14_GENUINE("ELM327 v1.4 (Genuíno)", true),
    ELM327_CLONE_BAD("ELM327 Clone v2.1 (Não Recomendado para Proxi)", false),
    SIMULATOR("Simulador Interno", true)
}

interface ObdConnection {
    val state: StateFlow<ConnectionState>
    suspend fun connect(address: String): Boolean
    suspend fun disconnect()
    suspend fun sendRawCommand(cmd: String, timeoutMs: Long = 2000): Result<String>
    suspend fun readBatteryVoltage(): Float
}
