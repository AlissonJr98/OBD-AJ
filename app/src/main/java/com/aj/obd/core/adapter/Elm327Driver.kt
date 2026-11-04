package com.aj.obd.core.adapter

import com.aj.obd.domain.model.EcuModule
import com.aj.obd.domain.model.ProtocolType

data class AdapterInfo(
    val type: AdapterType,
    val version: String,
    val voltage: Float,
    val isHighSpeedCapable: Boolean
)

class Elm327Driver(private val connection: ObdConnection) {

    suspend fun handshake(): Result<AdapterInfo> {
        // Reset do chip
        connection.sendRawCommand("ATWS") // Warm Start (mais rápido que ATZ)
        val atz = connection.sendRawCommand("ATZ").getOrDefault("")

        // Desliga eco e espaços
        connection.sendRawCommand("ATE0")
        connection.sendRawCommand("ATL0")
        connection.sendRawCommand("ATS0") // Sem espaços para agilizar transferência
        connection.sendRawCommand("ATH1") // Ativa headers para sabermos o emissor

        // Identifica versão do firmware
        val atiRes = connection.sendRawCommand("ATI").getOrDefault(atz)
        val voltage = connection.readBatteryVoltage()

        // Teste de capacidade ST (OBDLink / vLinker)
        val stRes = connection.sendRawCommand("STI").getOrDefault("")
        val isStn = stRes.contains("STN", ignoreCase = true)

        val adapterType = when {
            isStn -> AdapterType.OBD_LINK_GENUINE
            atiRes.contains("vLinker", ignoreCase = true) -> AdapterType.V_LINKER_MC
            atiRes.contains("ELM327 v1.4", ignoreCase = true) || atiRes.contains("v1.5", ignoreCase = true) -> AdapterType.ELM327_V14_GENUINE
            atiRes.contains("v2.1", ignoreCase = true) -> AdapterType.ELM327_CLONE_BAD
            else -> AdapterType.ELM327_V14_GENUINE
        }

        return Result.success(
            AdapterInfo(
                type = adapterType,
                version = if (isStn) stRes.lines().firstOrNull() ?: atiRes else atiRes.lines().firstOrNull() ?: "ELM327",
                voltage = voltage,
                isHighSpeedCapable = isStn || adapterType == AdapterType.V_LINKER_MC
            )
        )
    }

    suspend fun configureForEcu(ecu: EcuModule): Result<Unit> {
        val protocolCmd = when (ecu.protocol) {
            ProtocolType.CAN_11BIT_500K -> "ATSP6"  // ISO 15765-4 CAN (11 bit, 500 kbaud)
            ProtocolType.CAN_29BIT_500K -> "ATSP7"  // ISO 15765-4 CAN (29 bit, 500 kbaud)
            ProtocolType.CAN_11BIT_250K -> "ATSP8"  // ISO 15765-4 CAN (11 bit, 250 kbaud)
            ProtocolType.KLINE_KWP2000_FAST -> "ATSP5" // KWP2000 Fast Init (ISO 14230-4)
            ProtocolType.KLINE_ISO9141 -> "ATSP3"   // ISO 9141-2
        }

        connection.sendRawCommand(protocolCmd)
        // Define o Header CAN / K-Line de destino
        connection.sendRawCommand("ATSH ${ecu.requestHeader}")
        
        // Se for CAN, define filtro para escutar somente a resposta dessa central
        if (ecu.protocol in listOf(ProtocolType.CAN_11BIT_500K, ProtocolType.CAN_11BIT_250K)) {
            connection.sendRawCommand("ATCRA ${ecu.responseHeader}")
        }
        
        // Habilita mensagens longas (ISO-TP)
        connection.sendRawCommand("ATAL")
        connection.sendRawCommand("ATAT1") // Adaptive Timing 1

        return Result.success(Unit)
    }
}
