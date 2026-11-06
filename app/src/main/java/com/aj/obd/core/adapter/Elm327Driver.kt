package com.aj.obd.core.adapter

import com.aj.obd.domain.model.EcuModule
import com.aj.obd.domain.model.ProtocolType
import kotlinx.coroutines.delay

data class AdapterInfo(
    val type: AdapterType,
    val version: String,
    val voltage: Float,
    val isHighSpeedCapable: Boolean,
    val isEcuConnected: Boolean = false,
    val detectedProtocol: String = "OBD-II Auto",
    val ignitionStatus: String = "Pós-Chave Desligado"
)

class Elm327Driver(private val connection: ObdConnection) {

    suspend fun handshake(): Result<AdapterInfo> {
        // 1. Envia ATZ para reiniciar o chip ELM/STN
        val atzRes = connection.sendRawCommand("ATZ", timeoutMs = 2000)
        delay(400) // Aguarda o microcontrolador do ELM327 inicializar o clock após o reset

        // 2. Desliga eco
        val ateRes = connection.sendRawCommand("ATE0", timeoutMs = 1500)
        
        val atz = atzRes.getOrDefault("")
        val ate = ateRes.getOrDefault("")
        val atiRes = connection.sendRawCommand("ATI", timeoutMs = 1500)
        val ati = atiRes.getOrDefault("")

        // Validação REAL de hardware:
        // Se o chip Bluetooth não for um ELM/OBD ou não respondeu a nenhum dos 3 comandos, a conexão é falsa!
        val combined = "$atz\n$ate\n$ati".uppercase()
        val isValidElm = combined.contains("ELM") || 
                         combined.contains("OBD") || 
                         combined.contains("OK") || 
                         combined.contains("V1.") || 
                         combined.contains("V2.") ||
                         combined.contains("STN") ||
                         combined.contains("?")

        if (!isValidElm && combined.isBlank()) {
            return Result.failure(
                IllegalStateException("O scanner Bluetooth não respondeu aos comandos ELM327. Certifique-se de que o adaptador está conectado à tomada do carro e com o LED aceso.")
            )
        }

        // Configurações essenciais de alta velocidade (inspiradas no Car Scanner e AlfaOBD)
        connection.sendRawCommand("ATL0")  // Desativa line feeds extras
        connection.sendRawCommand("ATS0")  // Desativa espaços para economizar largura de banda
        connection.sendRawCommand("ATH0")  // Headers desligados para leitura direta dos PIDs
        connection.sendRawCommand("ATAT1") // Adaptive Timing nível 1 (ajuste inteligente de clock)
        connection.sendRawCommand("ATST32") // Timeout dinâmico calibrado para alta velocidade (32 x 4ms = 128ms)
        connection.sendRawCommand("ATSP0") // Auto-busca universal de protocolo OBD-II

        // Lê versão do firmware e tensão da bateria na porta OBD (pino 16)
        val voltage = connection.readBatteryVoltage()

        // Teste de capacidade ST (OBDLink / vLinker)
        val stRes = connection.sendRawCommand("STI").getOrDefault("")
        val isStn = stRes.contains("STN", ignoreCase = true)

        val adapterType = when {
            isStn -> AdapterType.OBD_LINK_GENUINE
            ati.contains("vLinker", ignoreCase = true) -> AdapterType.V_LINKER_MC
            ati.contains("ELM327 v1.4", ignoreCase = true) || ati.contains("v1.5", ignoreCase = true) -> AdapterType.ELM327_V14_GENUINE
            ati.contains("v2.1", ignoreCase = true) -> AdapterType.ELM327_CLONE_BAD
            else -> AdapterType.ELM327_V14_GENUINE
        }

        val versionText = if (isStn) {
            stRes.lines().firstOrNull() ?: ati
        } else {
            ati.lines().firstOrNull() ?: atz.lines().firstOrNull() ?: "ELM327 v1.5"
        }

        // FASE 2: TESTE DO PRIMEIRO ESTÁGIO DA IGNIÇÃO (PÓS-CHAVE / ECU TEST)
        // Dispara PID 0100 com timeout de até 6 segundos para permitir a busca de protocolo
        var isEcuConnected = false
        var ignitionStatus = "Chave Desligada (Gire a chave no 1º estágio)"
        var detectedProtocol = "Aguardando Pós-Chave"

        val ecuTestRes = connection.sendRawCommand("0100", timeoutMs = 6000).getOrDefault("")
        val cleanEcuTest = ecuTestRes.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()

        if (cleanEcuTest.contains("4100") || cleanEcuTest.contains("410C") || cleanEcuTest.contains("4105")) {
            isEcuConnected = true
            ignitionStatus = "Chave Ligada (ECU Ativa)"
            val dpRes = connection.sendRawCommand("ATDP").getOrDefault("OBD-II").trim()
            detectedProtocol = dpRes.lines().firstOrNull() ?: "OBD-II Padrão"
        }

        return Result.success(
            AdapterInfo(
                type = adapterType,
                version = versionText,
                voltage = voltage,
                isHighSpeedCapable = isStn || adapterType == AdapterType.V_LINKER_MC,
                isEcuConnected = isEcuConnected,
                detectedProtocol = detectedProtocol,
                ignitionStatus = ignitionStatus
            )
        )
    }

    /**
     * Configura o adaptador especificamente para a ECU escolhida,
     * tratando adequadamente a rede K-Line (Palio 2001, Uno, Marea) e rede CAN (Toro, Renegade).
     */
    suspend fun configureForEcu(ecu: EcuModule): Result<Unit> {
        when (ecu.protocol) {
            ProtocolType.OBD2_AUTO -> {
                connection.sendRawCommand("ATSP0")  // Auto-busca do protocolo correto da ECU (CAN / KWP / ISO)
                connection.sendRawCommand("ATH0")   // Respostas limpas sem cabeçalho
                connection.sendRawCommand("ATAT1")
                connection.sendRawCommand("ATST32")
            }
            ProtocolType.CAN_11BIT_500K -> {
                connection.sendRawCommand("ATSP6")  // ISO 15765-4 CAN (11 bit, 500 kbaud)
                connection.sendRawCommand("ATSH ${ecu.requestHeader}")
                connection.sendRawCommand("ATCRA ${ecu.responseHeader}")
                connection.sendRawCommand("ATAL")
                connection.sendRawCommand("ATAT1")
                connection.sendRawCommand("ATST32")
            }
            ProtocolType.CAN_29BIT_500K -> {
                connection.sendRawCommand("ATSP7")  // ISO 15765-4 CAN (29 bit, 500 kbaud)
                connection.sendRawCommand("ATSH ${ecu.requestHeader}")
                connection.sendRawCommand("ATAL")
                connection.sendRawCommand("ATST32")
            }
            ProtocolType.CAN_11BIT_250K -> {
                connection.sendRawCommand("ATSP8")  // ISO 15765-4 CAN (11 bit, 250 kbaud)
                connection.sendRawCommand("ATSH ${ecu.requestHeader}")
                connection.sendRawCommand("ATCRA ${ecu.responseHeader}")
                connection.sendRawCommand("ATST32")
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
