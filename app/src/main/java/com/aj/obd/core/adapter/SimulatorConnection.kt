package com.aj.obd.core.adapter

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class SimulatorConnection : ObdConnection {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var currentProtocol = "ATSP5" // Padrão KWP2000 para Fiat Palio
    private var currentHeader = "8110F1"
    private var simulatedRpm = 850f
    private var simulatedSpeed = 0f
    private var simulatedCoolant = 88f
    private var simulatedBattery = 13.8f

    // String simulada de Proxi BCM para Fiat Toro / Renegade
    private var simulatedProxiHex = "4F0092B086019420000000000000000000000000" +
            "0000000000000000000000000000000000000000" +
            "0000000000040000000000000000000000000000"

    override suspend fun connect(address: String): Boolean {
        _state.value = ConnectionState.Connecting("Simulador Fiat Palio 2001 & Linha Fiat", "Inicializando K-Line / CAN Virtual...")
        delay(500)
        _state.value = ConnectionState.Connected(
            deviceName = "Simulador Fiat Palio Fire / Marelli IAW 4AF",
            adapterType = AdapterType.SIMULATOR,
            firmwareVersion = "OBD AJ Virtual K-Line & CAN v2.0",
            batteryVoltage = simulatedBattery,
            isHighSpeedCapable = true
        )
        return true
    }

    override suspend fun disconnect() {
        _state.value = ConnectionState.Disconnected
    }

    override suspend fun sendRawCommand(cmd: String, timeoutMs: Long): Result<String> {
        delay(25) // Simula latência ágil do barramento
        val clean = cmd.trim().uppercase().replace(" ", "")

        val response = when {
            clean == "ATZ" || clean == "ATWS" -> "ELM327 v1.4"
            clean.startsWith("ATE") || clean.startsWith("ATL") || clean.startsWith("ATS") || 
            clean.startsWith("ATH") || clean.startsWith("ATAL") || clean.startsWith("ATAT") ||
            clean.startsWith("ATIB") || clean.startsWith("ATIIA") || clean.startsWith("ATWM") ||
            clean.startsWith("ATSW") || clean.startsWith("ATCRA") -> "OK"
            
            clean.startsWith("ATSP") -> {
                currentProtocol = clean
                "OK"
            }
            clean.startsWith("ATSH") -> {
                currentHeader = clean.removePrefix("ATSH").trim()
                "OK"
            }
            clean == "ATFI" || clean == "ATSI" -> "BUS INIT: OK"
            clean == "ATI" -> "OBD AJ Automotive Driver v2.0"
            clean == "STI" -> "STN2120 v5.1.0"
            clean == "ATRV" -> "%.1fV".format(simulatedBattery + (Random.nextFloat() * 0.2f - 0.1f))

            // =================================================================
            // KWP2000 (FIAT PALIO 2001 - MARELLI IAW 4AF / BOSCH ME7.3)
            // =================================================================
            // 0x81 - Start Communication (Handshake KWP2000 pino 7)
            clean.startsWith("8110F181") || clean == "81" -> "81F110C1EA8F"

            // 0x1A 90 - Read ECU Identification (ISO Code da Marelli IAW 4AF)
            clean.startsWith("1A90") -> "5A9045858394B6" // ISO Code cadastrado para Marelli 4AF

            // 0x1A 91 - Read Hardware & Calibration Number
            clean.startsWith("1A91") -> "5A9149415720344146422E5031" // "IAW 4AFB.P1"

            // 0x21 01 - Read Data By Local ID (Parâmetros ao vivo KWP2000)
            clean.startsWith("2101") -> {
                simulatedRpm = (simulatedRpm + Random.nextInt(-25, 35)).coerceIn(800f, 4800f)
                val rpmRaw = (simulatedRpm * 4).toInt()
                val tempRaw = (simulatedCoolant + 40).toInt()
                "6101%04X%02X".format(rpmRaw, tempRaw)
            }

            // 0x18 - Read DTCs KWP2000 (Simula falha comum de Sonda e MAP no Palio)
            clean.startsWith("18") -> "5801050130" // P0105 (MAP) e P0130 (Sonda)

            // 0x14 - Clear DTCs KWP2000
            clean.startsWith("14") -> "54"

            // 0x31 01 - Reset dos Parâmetros Autoadaptativos da Marelli 4AF
            clean.startsWith("3101") -> "7101"

            // 0x3E - Tester Present
            clean.startsWith("3E") -> "7E"

            // =================================================================
            // UDS (VEÍCULOS MODERNOS - TORO / RENEGADE)
            // =================================================================
            clean.startsWith("10") -> "5003003201F4"
            clean == "220103" -> "620103$simulatedProxiHex"
            clean.startsWith("2E0103") -> {
                simulatedProxiHex = clean.removePrefix("2E0103")
                "6E0103"
            }
            clean.startsWith("1902") -> "59020D03000008U17010008"

            // Leituras OBD-II padrão
            clean == "010C" || clean == "221000" -> {
                simulatedRpm = (simulatedRpm + Random.nextInt(-20, 30)).coerceIn(800f, 5000f)
                "410C%04X".format((simulatedRpm * 4).toInt())
            }
            clean == "010D" -> {
                simulatedSpeed = (simulatedSpeed + Random.nextInt(-1, 2)).coerceIn(0f, 160f)
                "410D%02X".format(simulatedSpeed.toInt())
            }
            clean == "0105" -> {
                "4105%02X".format((simulatedCoolant + 40).toInt())
            }

            else -> "OK"
        }

        return Result.success(response)
    }

    override suspend fun readBatteryVoltage(): Float = simulatedBattery
}
