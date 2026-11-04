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
import com.aj.obd.core.kwp.Kwp2000Client
import com.aj.obd.core.proxi.ProxiBackup
import com.aj.obd.core.proxi.ProxiManager
import com.aj.obd.core.uds.UdsClient
import com.aj.obd.data.VehicleCatalog
import com.aj.obd.data.database.AutomotiveDatabaseHelper
import com.aj.obd.domain.model.DtcFault
import com.aj.obd.domain.model.EcuCategory
import com.aj.obd.domain.model.EcuModule
import com.aj.obd.domain.model.LiveSensor
import com.aj.obd.domain.model.ProxiFeature
import com.aj.obd.domain.model.ProtocolType
import com.aj.obd.domain.model.Vehicle
import com.aj.obd.domain.model.VehicleEra
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    // Base de Conhecimento SQLite
    val dbHelper = AutomotiveDatabaseHelper(application)

    // Filtros de Veículo
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBrandFilter = MutableStateFlow("Todas")
    val selectedBrandFilter: StateFlow<String> = _selectedBrandFilter.asStateFlow()

    private val _selectedEraFilter = MutableStateFlow(VehicleEra.ALL)
    val selectedEraFilter: StateFlow<VehicleEra> = _selectedEraFilter.asStateFlow()

    val filteredVehicles: StateFlow<List<Vehicle>> = combine(
        _searchQuery,
        _selectedBrandFilter,
        _selectedEraFilter
    ) { query, brand, era ->
        VehicleCatalog.vehicles.filter { v ->
            val matchesQuery = query.isBlank() || 
                v.name.contains(query, ignoreCase = true) || 
                v.brand.contains(query, ignoreCase = true) ||
                v.engineOptions.contains(query, ignoreCase = true) ||
                v.years.contains(query, ignoreCase = true)

            val matchesBrand = brand == "Todas" || v.brand.equals(brand, ignoreCase = true)
            val matchesEra = era == VehicleEra.ALL || v.era == era

            matchesQuery && matchesBrand && matchesEra
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, VehicleCatalog.vehicles)

    // Veículo e ECU Selecionados
    private val _selectedVehicle = MutableStateFlow<Vehicle>(VehicleCatalog.vehicles.first())
    val selectedVehicle: StateFlow<Vehicle> = _selectedVehicle.asStateFlow()

    private val _selectedEcu = MutableStateFlow<EcuModule?>(null)
    val selectedEcu: StateFlow<EcuModule?> = _selectedEcu.asStateFlow()

    private val _autoDetectStatus = MutableStateFlow<String?>(null)
    val autoDetectStatus: StateFlow<String?> = _autoDetectStatus.asStateFlow()

    private val _isAutoDetecting = MutableStateFlow(false)
    val isAutoDetecting: StateFlow<Boolean> = _isAutoDetecting.asStateFlow()

    // Sensores ao Vivo
    private val _sensors = MutableStateFlow<List<LiveSensor>>(
        listOf(
            LiveSensor("rpm", "Rotação do Motor", "RPM", "010C", "RPM", 0f, 0f, 6000f),
            LiveSensor("speed", "Velocidade do Veículo", "VEL", "010D", "km/h", 0f, 0f, 220f),
            LiveSensor("coolant", "Temperatura da Água", "TEMP", "0105", "°C", 0f, 40f, 130f),
            LiveSensor("turbo", "Pressão do Coletor / Turbo", "MAP", "221000", "bar", 0f, 0f, 2.5f),
            LiveSensor("battery", "Tensão da Bateria", "BATT", "ATRV", "V", 13.8f, 10f, 16f)
        )
    )
    val sensors: StateFlow<List<LiveSensor>> = _sensors.asStateFlow()

    // DTCs
    private val _dtcFaults = MutableStateFlow<List<DtcFault>>(emptyList())
    val dtcFaults: StateFlow<List<DtcFault>> = _dtcFaults.asStateFlow()

    private val _isScanningDtc = MutableStateFlow(false)
    val isScanningDtc: StateFlow<Boolean> = _isScanningDtc.asStateFlow()

    // Clientes de Diagnóstico
    private var udsClient = UdsClient(currentConnection)
    private var kwpClient = Kwp2000Client(currentConnection)
    private val proxiManager = ProxiManager(udsClient, File(application.filesDir, "proxi_backups"))

    // Proxi
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
        viewModelScope.launch {
            dbHelper.initializeDatabase()
        }
        selectVehicle(VehicleCatalog.vehicles.first()) // Padrão Palio 2001
        _proxiFeatures.value = proxiManager.getAvailableFeatures(_currentProxiHex.value)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateBrandFilter(brand: String) {
        _selectedBrandFilter.value = brand
    }

    fun updateEraFilter(era: VehicleEra) {
        _selectedEraFilter.value = era
    }

    fun selectVehicle(vehicle: Vehicle) {
        _selectedVehicle.value = vehicle
        // Seleciona automaticamente a ECU primária (padrão)
        val defaultEcu = vehicle.supportedEcus.find { it.isDefault } 
            ?: vehicle.supportedEcus.firstOrNull()
        _selectedEcu.value = defaultEcu
        _autoDetectStatus.value = null

        defaultEcu?.let { configureEcuOnAdapter(it) }
    }

    fun selectEcu(ecu: EcuModule) {
        _selectedEcu.value = ecu
        _autoDetectStatus.value = null
        configureEcuOnAdapter(ecu)
    }

    private fun configureEcuOnAdapter(ecu: EcuModule) {
        viewModelScope.launch {
            if (currentConnection.state.value is ConnectionState.Connected) {
                Elm327Driver(currentConnection).configureForEcu(ecu)
            }
        }
    }

    /**
     * Auto-detecção inteligente de ECU:
     * Interroga a central via KWP2000 (0x1A 0x90) ou UDS, obtém o ISO-Code e busca
     * na base de dados para configurar a central automaticamente sem o usuário adivinhar.
     */
    fun autoDetectEcu() {
        viewModelScope.launch {
            _isAutoDetecting.value = true
            _autoDetectStatus.value = "Interrogando central via K-Line / CAN..."
            delay(400)

            // 1. Tenta KWP2000 (comum no Palio 2001 e linha Fiat antiga)
            val kwpRes = kwpClient.readEcuIdentification(0x90)
            val isoCode = kwpRes.getOrNull()

            if (!isoCode.isNullOrBlank()) {
                val foundEcu = dbHelper.findEcuByIsoCode(isoCode)
                if (foundEcu != null) {
                    _selectedEcu.value = foundEcu
                    configureEcuOnAdapter(foundEcu)
                    _autoDetectStatus.value = "✅ Detectado: ${foundEcu.name} (ISO: $isoCode)"
                } else {
                    _autoDetectStatus.value = "⚠️ ISO Code recebido ($isoCode), mas não mapeado no banco."
                }
            } else {
                // Se KWP falhar, mantém a ECU padrão e avisa
                _autoDetectStatus.value = "ℹ️ Central configurada: ${_selectedEcu.value?.name}"
            }
            _isAutoDetecting.value = false
        }
    }

    fun toggleSimulator(useSimulator: Boolean) {
        viewModelScope.launch {
            currentConnection.disconnect()
            currentConnection = if (useSimulator) simulator else realConnection
            udsClient = UdsClient(currentConnection)
            kwpClient = Kwp2000Client(currentConnection)
            if (useSimulator) {
                currentConnection.connect("virtual")
                _selectedEcu.value?.let { configureEcuOnAdapter(it) }
                startTelemetryLoop()
            } else {
                stopTelemetryLoop()
            }
        }
    }

    fun connectBluetooth(address: String) {
        viewModelScope.launch {
            currentConnection = realConnection
            udsClient = UdsClient(currentConnection)
            kwpClient = Kwp2000Client(currentConnection)
            val ok = currentConnection.connect(address)
            if (ok) {
                _selectedEcu.value?.let { configureEcuOnAdapter(it) }
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
                    val ecu = _selectedEcu.value
                    val isKwp = ecu?.protocol == ProtocolType.KLINE_KWP2000_FAST

                    val updated = _sensors.value.map { sensor ->
                        val cmd = if (isKwp && sensor.id == "rpm") "2101" else sensor.didHex
                        val rawRes = currentConnection.sendRawCommand(cmd).getOrNull() ?: ""
                        val parsedVal = parseSensorValue(sensor, rawRes, isKwp)
                        sensor.copy(currentValue = parsedVal)
                    }
                    _sensors.value = updated
                }
                delay(120)
            }
        }
    }

    private fun stopTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = null
    }

    private fun parseSensorValue(sensor: LiveSensor, rawRes: String, isKwp: Boolean): Float {
        val clean = rawRes.replace(" ", "").replace("\n", "").trim()
        if (isKwp && clean.startsWith("6101")) {
            // No Palio 2001 (KWP2000 0x21 0x01), bytes 0..1 são RPM e byte 2 é temperatura
            return when (sensor.id) {
                "rpm" -> {
                    if (clean.length >= 8) {
                        val hex = clean.substring(4, 8)
                        (hex.toIntOrNull(16) ?: 0) / 4f
                    } else sensor.currentValue
                }
                "coolant" -> {
                    if (clean.length >= 10) {
                        val hex = clean.substring(8, 10)
                        ((hex.toIntOrNull(16) ?: 40) - 40).toFloat()
                    } else sensor.currentValue
                }
                else -> sensor.currentValue
            }
        }

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
            "turbo" -> (0.3f + (sensor.currentValue * 0.95f)).coerceIn(0f, 2.1f)
            "battery" -> clean.replace("V", "").toFloatOrNull() ?: sensor.currentValue
            else -> sensor.currentValue
        }
    }

    fun scanAllDtcs() {
        viewModelScope.launch {
            _isScanningDtc.value = true
            val ecu = _selectedEcu.value
            val ecuName = ecu?.name ?: "Central Ativa"

            val faults = if (ecu?.protocol in listOf(ProtocolType.KLINE_KWP2000_FAST, ProtocolType.KLINE_ISO9141)) {
                // Diagnóstico K-Line / KWP2000 (Palio 2001)
                kwpClient.readDtcKwp(ecuName).getOrDefault(emptyList())
            } else {
                // Diagnóstico CAN / UDS
                udsClient.readDtcs(ecuName).getOrDefault(emptyList())
            }

            // Enriquece com as descrições da base SQLite se disponível
            val enriched = faults.map { f ->
                val dbDesc = dbHelper.getDtcDescriptionPt(f.code)
                if (!dbDesc.isNullOrBlank()) f.copy(description = dbDesc) else f
            }

            _dtcFaults.value = enriched
            _isScanningDtc.value = false
        }
    }

    fun clearAllDtcs() {
        viewModelScope.launch {
            val ecu = _selectedEcu.value
            if (ecu?.protocol in listOf(ProtocolType.KLINE_KWP2000_FAST, ProtocolType.KLINE_ISO9141)) {
                kwpClient.clearDtcKwp()
            } else {
                udsClient.clearDtcs()
            }
            _dtcFaults.value = emptyList()
        }
    }

    /**
     * Exclusivo para Fiat K-Line (ex: Palio 2001 Fire com Marelli 4AF):
     * Executa o Reset dos Parâmetros Autoadaptativos de Combustível/Mistura.
     */
    fun resetAutoadaptiveParameters() {
        viewModelScope.launch {
            val res = kwpClient.resetAutoadaptiveParameters()
            if (res.isSuccess) {
                _autoDetectStatus.value = "✅ Reset dos parâmetros autoadaptativos concluído com sucesso!"
            } else {
                _autoDetectStatus.value = "❌ Falha no reset: ${res.exceptionOrNull()?.message}"
            }
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
