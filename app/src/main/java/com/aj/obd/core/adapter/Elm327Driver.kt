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
        // Reset rápido do chip ELM/STN
        connection.sendRawCommand("ATWS")
        val atz = connection.sendRawCommand("ATZ").getOrDefault("")

        // Desliga eco, espaços e quebras de linha extras
        connection.sendRawCommand("ATE0")
        connection.sendRawCommand("ATL0")
        connection.sendRawCommand("ATS0")
        connection.sendRawCommand("ATH1") // Ativa cabeçalhos para sabermos remetente e destinatário

        // Lê versão do firmware e tensão da bateria
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

    /**
     * Configura o adaptador especificamente para a ECU escolhida,
     * tratando adequadamente a rede K-Line (Palio 2001, Uno, Marea) e rede CAN (Toro, Renegade).
     */
    suspend fun configureForEcu(ecu: EcuModule): Result<Unit> {
        when (ecu.protocol) {
            ProtocolType.CAN_11BIT_500K -> {
                connection.sendRawCommand("ATSP6")  // ISO 15765-4 CAN (11 bit, 500 kbaud)
                connection.sendRawCommand("ATSH ${ecu.requestHeader}")
                connection.sendRawCommand("ATCRA ${ecu.responseHeader}")
                connection.sendRawCommand("ATAL")
                connection.sendRawCommand("ATAT1")
            }
            ProtocolType.CAN_29BIT_500K -> {
                connection.sendRawCommand("ATSP7")  // ISO 15765-4 CAN (29 bit, 500 kbaud)
                connection.sendRawCommand("ATSH ${ecu.requestHeader}")
                connection.sendRawCommand("ATAL")
            }
            ProtocolType.CAN_11BIT_250K -> {
                connection.sendRawCommand("ATSP8")  // ISO 15765-4 CAN (11 bit, 250 kbaud)
                connection.sendRawCommand("ATSH ${ecu.requestHeader}")
                connection.sendRawCommand("ATCRA ${ecu.responseHeader}")
            }
            ProtocolType.KLINE_KWP2000_FAST -> {
                // Protocolo do Palio 2001 Fire (Marelli 4AF / 49F / ME7.3H4)
                connection.sendRawCommand("ATSP5")  // ISO 14230-4 KWP (Fast Init)
                connection.sendRawCommand("ATIB 10") // 10.400 bps baud rate
                connection.sendRawCommand("ATIIA %02X".format(ecu.physicalAddress)) // Endereço físico (0x10)
                connection.sendRawCommand("ATSH 81 %02X F1".format(ecu.physicalAddress))
                connection.sendRawCommand("ATWM 81 %02X F1 3E".format(ecu.physicalAddress)) // Keep-alive K-Line
                connection.sendRawCommand("ATSW 20") // Acorda a cada 2 segundos
                connection.sendRawCommand("ATFI")   // Executa a inicialização rápida
            }
            ProtocolType.KLINE_ISO9141 -> {
                // Protocolo do ABS Bosch 5.3 e Airbag MY97/MY99 (Palio 2001)
                connection.sendRawCommand("ATSP3")  // ISO 9141-2
                val baudCmd = if (ecu.baudRate == 4800) "ATIB 48" else "ATIB 10"
                connection.sendRawCommand(baudCmd)
                connection.sendRawCommand("ATSH 68 %02X F1".format(ecu.physicalAddress))
                connection.sendRawCommand("ATSI")   // Inicialização lenta 5-baud
            }
        }

        return Result.success(Unit)
    }
}
