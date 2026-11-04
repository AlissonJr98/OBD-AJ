package com.aj.obd.core.adapter

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class SimulatorConnection : ObdConnection {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var currentProtocol = "ATSP6"
    private var currentHeader = "7E0"
    private var simulatedRpm = 850f
    private var simulatedSpeed = 0f
    private var simulatedCoolant = 88f
    private var simulatedBattery = 14.2f

    // String simulada de Proxi BCM para Fiat Toro / Renegade (120 bytes)
    private var simulatedProxiHex = "4F0092B086019420000000000000000000000000" +
            "0000000000000000000000000000000000000000" +
            "0000000000040000000000000000000000000000" +
            "0000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000"

    override suspend fun connect(address: String): Boolean {
        _state.value = ConnectionState.Connecting("Simulador Fiat Toro 2.0 / Renegade", "Iniciando ECU Virtual...")
        delay(600)
        _state.value = ConnectionState.Connected(
            deviceName = "Simulador Fiat/Jeep Virtual",
            adapterType = AdapterType.SIMULATOR,
            firmwareVersion = "OBD AJ Virtual STN2120 v5.1",
            batteryVoltage = simulatedBattery,
            isHighSpeedCapable = true
        )
        return true
    }

    override suspend fun disconnect() {
        _state.value = ConnectionState.Disconnected
    }

    override suspend fun sendRawCommand(cmd: String, timeoutMs: Long): Result<String> {
        delay(35) // Simula latência de rede CAN rápida
        val clean = cmd.trim().uppercase()

        val response = when {
            clean == "ATZ" || clean == "ATWS" -> "ELM327 v1.4"
            clean == "ATE0" || clean == "ATL0" || clean == "ATS0" || clean == "ATH1" || clean == "ATAL" || clean == "ATAT1" -> "OK"
            clean.startsWith("ATSP") -> {
                currentProtocol = clean
                "OK"
            }
            clean.startsWith("ATSH") -> {
                currentHeader = clean.removePrefix("ATSH").trim()
                "OK"
            }
            clean.startsWith("ATCRA") -> "OK"
            clean == "ATI" -> "OBD AJ v1.0.0"
            clean == "STI" -> "STN2120 v5.1.0"
            clean == "ATRV" -> "%.1fV".format(simulatedBattery + (Random.nextFloat() * 0.2f - 0.1f))

            // UDS 0x10 - Diagnostic Session Control
            clean.startsWith("10") -> "5003003201F4"

            // UDS 0x22 0103 - Read Proxi Configuration DID
            clean == "220103" -> "620103$simulatedProxiHex"

            // UDS 0x2E 0103 - Write Proxi Configuration
            clean.startsWith("2E0103") -> {
                simulatedProxiHex = clean.removePrefix("2E0103")
                "6E0103"
            }

            // UDS 0x19 02 0D - Read DTCs
            clean.startsWith("1902") -> {
                // Simula 2 falhas comuns (P0300 e U1701)
                "59020D03000008U17010008"
            }

            // UDS 0x14 - Clear DTCs
            clean.startsWith("14") -> "54"

            // Sensores ao vivo
            clean == "010C" || clean == "221000" -> { // RPM
                simulatedRpm = (simulatedRpm + Random.nextInt(-30, 45)).coerceIn(800f, 4500f)
                val raw = (simulatedRpm * 4).toInt()
                "410C%04X".format(raw)
            }
            clean == "010D" -> { // Velocidade
                simulatedSpeed = (simulatedSpeed + Random.nextInt(-1, 3)).coerceIn(0f, 160f)
                "410D%02X".format(simulatedSpeed.toInt())
            }
            clean == "0105" -> { // Temperatura do líquido de arrefecimento (+40 offset)
                val raw = (simulatedCoolant + 40).toInt()
                "4105%02X".format(raw)
            }

            else -> "OK"
        }

        return Result.success(response)
    }

    override suspend fun readBatteryVoltage(): Float = simulatedBattery
}
