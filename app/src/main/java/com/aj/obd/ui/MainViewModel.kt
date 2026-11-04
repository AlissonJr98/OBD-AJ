package com.aj.obd.ui

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aj.obd.core.adapter.AdapterType
import com.aj.obd.core.adapter.BluetoothClassicConnection
import com.aj.obd.core.adapter.ConnectionState
import com.aj.obd.core.adapter.Elm327Driver
import com.aj.obd.core.adapter.ObdConnection
import com.aj.obd.core.adapter.SimulatorConnection
import com.aj.obd.core.proxi.ProxiBackup
import com.aj.obd.core.proxi.ProxiManager
import com.aj.obd.core.uds.UdsClient
import com.aj.obd.data.VehicleCatalog
import com.aj.obd.domain.model.DtcFault
import com.aj.obd.domain.model.EcuModule
import com.aj.obd.domain.model.LiveSensor
import com.aj.obd.domain.model.ProxiFeature
import com.aj.obd.domain.model.Vehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val simulator = SimulatorConnection()
    private val realConnection = BluetoothClassicConnection(application, bluetoothAdapter)

    var currentConnection: ObdConnection = simulator
        private set

    val connectionState: StateFlow<ConnectionState>
        get() = currentConnection.state

    private val _selectedVehicle = MutableStateFlow<Vehicle>(VehicleCatalog.vehicles.first())
    val selectedVehicle: StateFlow<Vehicle> = _selectedVehicle.asStateFlow()

    private val _selectedEcu = MutableStateFlow<EcuModule?>(null)
    val selectedEcu: StateFlow<EcuModule?> = _selectedEcu.asStateFlow()

    // Sensores ao Vivo
    private val _sensors = MutableStateFlow<List<LiveSensor>>(
        listOf(
            LiveSensor("rpm", "Rotação do Motor", "RPM", "010C", "RPM", 0f, 0f, 6000f),
            LiveSensor("speed", "Velocidade do Veículo", "VEL", "010D", "km/h", 0f, 0f, 220f),
            LiveSensor("coolant", "Temperatura da Água", "TEMP", "0105", "°C", 0f, 40f, 130f),
            LiveSensor("turbo", "Pressão do Turbo", "TURBO", "221000", "bar", 0f, 0f, 2.5f),
            LiveSensor("battery", "Tensão da Bateria", "BATT", "ATRV", "V", 12.6f, 10f, 16f)
        )
    )
    val sensors: StateFlow<List<LiveSensor>> = _sensors.asStateFlow()

    // DTCs
    private val _dtcFaults = MutableStateFlow<List<DtcFault>>(emptyList())
    val dtcFaults: StateFlow<List<DtcFault>> = _dtcFaults.asStateFlow()

    private val _isScanningDtc = MutableStateFlow(false)
    val isScanningDtc: StateFlow<Boolean> = _isScanningDtc.asStateFlow()

    // Proxi
    private val udsClient = UdsClient(currentConnection)
    private val proxiManager = ProxiManager(udsClient, File(application.filesDir, "proxi_backups"))

    private val _currentProxiHex = MutableStateFlow("4F0092B086019420000000000000000000000000" +
            "0000000000000000000000000000000000000000" +
            "0000000000040000000000000000000000000000")
    val currentProxiHex: StateFlow<String> = _currentProxiHex.asStateFlow()

    private val _proxiFeatures = MutableStateFlow<List<ProxiFeature>>(emptyList())
    val proxiFeatures: StateFlow<List<ProxiFeature>> = _proxiFeatures.asStateFlow()

    private val _lastBackup = MutableStateFlow<ProxiBackup?>(null)
    val lastBackup: StateFlow<ProxiBackup?> = _lastBackup.asStateFlow()

    private val _isProxiRunning = MutableStateFlow(false)
    val isProxiRunning: StateFlow<Boolean> = _isProxiRunning.asStateFlow()

    private val _proxiStatusMessage = MutableStateFlow<String?>(null)
    val proxiStatusMessage: StateFlow<String?> = _proxiStatusMessage.asStateFlow()

    private var telemetryJob: Job? = null

    init {
        _selectedEcu.value = _selectedVehicle.value.supportedEcus.firstOrNull()
        _proxiFeatures.value = proxiManager.getAvailableFeatures(_currentProxiHex.value)
    }

    fun selectVehicle(vehicle: Vehicle) {
        _selectedVehicle.value = vehicle
        _selectedEcu.value = vehicle.supportedEcus.firstOrNull()
    }

    fun selectEcu(ecu: EcuModule) {
        _selectedEcu.value = ecu
        viewModelScope.launch {
            if (currentConnection.state.value is ConnectionState.Connected) {
                Elm327Driver(currentConnection).configureForEcu(ecu)
            }
        }
    }

    fun toggleSimulator(useSimulator: Boolean) {
        viewModelScope.launch {
            currentConnection.disconnect()
            currentConnection = if (useSimulator) simulator else realConnection
            if (useSimulator) {
                currentConnection.connect("virtual")
                startTelemetryLoop()
            } else {
                stopTelemetryLoop()
            }
        }
    }

    fun connectBluetooth(address: String) {
        viewModelScope.launch {
            currentConnection = realConnection
            val ok = currentConnection.connect(address)
            if (ok) {
                startTelemetryLoop()
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            stopTelemetryLoop()
            currentConnection.disconnect()
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                if (currentConnection.state.value is ConnectionState.Connected) {
                    val updated = _sensors.value.map { sensor ->
                        val rawRes = currentConnection.sendRawCommand(sensor.didHex).getOrNull() ?: ""
                        val parsedVal = parseSensorValue(sensor, rawRes)
                        sensor.copy(currentValue = parsedVal)
                    }
                    _sensors.value = updated
                }
                delay(120) // Atualiza ~8 vezes por segundo
            }
        }
    }

    private fun stopTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = null
    }

    private fun parseSensorValue(sensor: LiveSensor, rawRes: String): Float {
        val clean = rawRes.replace(" ", "").replace("\n", "").trim()
        return when (sensor.id) {
            "rpm" -> {
                if (clean.length >= 8 && clean.startsWith("410C")) {
                    val hex = clean.substring(4, 8)
                    (hex.toIntOrNull(16) ?: 0) / 4f
                } else sensor.currentValue
            }
            "speed" -> {
                if (clean.length >= 6 && clean.startsWith("410D")) {
                    val hex = clean.substring(4, 6)
                    (hex.toIntOrNull(16) ?: 0).toFloat()
                } else sensor.currentValue
            }
            "coolant" -> {
                if (clean.length >= 6 && clean.startsWith("4105")) {
                    val hex = clean.substring(4, 6)
                    ((hex.toIntOrNull(16) ?: 40) - 40).toFloat()
                } else sensor.currentValue
            }
            "turbo" -> {
                // Simulação dinâmica
                (0.2f + (sensor.currentValue * 0.95f)).coerceIn(0f, 2.1f)
            }
            "battery" -> {
                clean.replace("V", "").toFloatOrNull() ?: sensor.currentValue
            }
            else -> sensor.currentValue
        }
    }

    fun scanAllDtcs() {
        viewModelScope.launch {
            _isScanningDtc.value = true
            val ecu = _selectedEcu.value?.name ?: "Módulo Selecionado"
            val res = udsClient.readDtcs(ecu)
            _dtcFaults.value = res.getOrDefault(emptyList())
            _isScanningDtc.value = false
        }
    }

    fun clearAllDtcs() {
        viewModelScope.launch {
            udsClient.clearDtcs()
            _dtcFaults.value = emptyList()
        }
    }

    fun toggleProxiFeature(feature: ProxiFeature, enable: Boolean) {
        val newHex = proxiManager.toggleFeature(_currentProxiHex.value, feature, enable)
        _currentProxiHex.value = newHex
        _proxiFeatures.value = proxiManager.getAvailableFeatures(newHex)
    }

    fun executeProxiAlignment() {
        viewModelScope.launch {
            _isProxiRunning.value = true
            _proxiStatusMessage.value = "1/3 Criando backup de segurança do Proxi original..."
            val backup = proxiManager.createSafetyBackup(_selectedVehicle.value.id, _currentProxiHex.value)
            _lastBackup.value = backup
            delay(500)

            _proxiStatusMessage.value = "2/3 Gravando nova configuração no Body Computer (BCM)..."
            delay(800)

            _proxiStatusMessage.value = "3/3 Enviando broadcast de sincronização para a rede CAN..."
            val result = proxiManager.executeProxiAlignment(_currentProxiHex.value)
            delay(600)

            _isProxiRunning.value = false
            if (result.isSuccess) {
                _proxiStatusMessage.value = "✅ Alinhamento de Proxi concluído com sucesso! Desligue e ligue a ignição."
            } else {
                _proxiStatusMessage.value = "❌ Falha no alinhamento: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
