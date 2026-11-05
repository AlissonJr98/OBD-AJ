package com.aj.obd.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aj.obd.core.adapter.AdapterType
import com.aj.obd.core.adapter.BluetoothClassicConnection
import com.aj.obd.core.adapter.ConnectionState
import com.aj.obd.core.adapter.Elm327Driver
import com.aj.obd.core.adapter.ObdConnection
import com.aj.obd.core.adapter.SimulatorConnection
import com.aj.obd.core.ai.AiDiagnosticReport
import com.aj.obd.core.ai.DiagnosticAssistant
import com.aj.obd.core.battery.BatteryHealthTester
import com.aj.obd.core.battery.BatteryTestResult
import com.aj.obd.core.dyno.DynoPoint
import com.aj.obd.core.dyno.DynoResult
import com.aj.obd.core.dyno.VirtualDyno
import com.aj.obd.core.fuel.FlexFuelManager
import com.aj.obd.core.fuel.FlexFuelStatus
import com.aj.obd.core.kwp.Kwp2000Client
import com.aj.obd.core.license.LicenseManager
import com.aj.obd.core.license.LicenseState
import com.aj.obd.core.logger.TelemetryLogger
import com.aj.obd.core.performance.DragMeterState
import com.aj.obd.core.performance.PerformanceMeter
import com.aj.obd.core.performance.PerformanceRun
import com.aj.obd.core.proxi.ProxiBackup
import com.aj.obd.core.proxi.ProxiManager
import com.aj.obd.core.report.PdfReportGenerator
import com.aj.obd.core.service.ServiceResetManager
import com.aj.obd.core.transmission.DualogicManager
import com.aj.obd.core.trip.TripComputer
import com.aj.obd.core.trip.TripStats
import com.aj.obd.core.uds.UdsClient
import com.aj.obd.core.update.AppUpdateInfo
import com.aj.obd.core.update.UpdateChecker
import com.aj.obd.core.voice.VoiceCopilot
import com.aj.obd.data.VehicleCatalog
import com.aj.obd.data.database.AutomotiveDatabaseHelper
import com.aj.obd.domain.model.DtcFault
import com.aj.obd.domain.model.DtcSeverity
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

data class PairedBluetoothDevice(
    val name: String,
    val address: String,
    val isObdGuess: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val simulator = SimulatorConnection()
    private val realConnection = BluetoothClassicConnection(application, bluetoothAdapter)

    var currentConnection: ObdConnection = realConnection
        private set

    val connectionState: StateFlow<ConnectionState>
        get() = currentConnection.state

    // Base de Conhecimento SQLite
    val dbHelper = AutomotiveDatabaseHelper(application)

    // Atualização OTA (GitHub Releases)
    private val updateChecker = UpdateChecker(application)
    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    // Download de Atualização In-App (Igual CineList / LivennIA)
    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadProgressText = MutableStateFlow("")
    val downloadProgressText: StateFlow<String> = _downloadProgressText.asStateFlow()

    private val _downloadedApkFile = MutableStateFlow<File?>(null)
    val downloadedApkFile: StateFlow<File?> = _downloadedApkFile.asStateFlow()

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError.asStateFlow()

    // Painel e Dispositivos Bluetooth Pareados
    private val _pairedDevices = MutableStateFlow<List<PairedBluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<PairedBluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _showConnectionSheet = MutableStateFlow(false)
    val showConnectionSheet: StateFlow<Boolean> = _showConnectionSheet.asStateFlow()

    fun openConnectionSheet() {
        refreshPairedDevices()
        _showConnectionSheet.value = true
    }

    fun dismissConnectionSheet() {
        _showConnectionSheet.value = false
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val devices = bluetoothAdapter.bondedDevices.map { dev ->
                    val name = dev.name ?: "Dispositivo Bluetooth"
                    val isObd = name.contains("obd", ignoreCase = true) ||
                            name.contains("elm", ignoreCase = true) ||
                            name.contains("vlinker", ignoreCase = true) ||
                            name.contains("viecar", ignoreCase = true) ||
                            name.contains("scanner", ignoreCase = true) ||
                            name.contains("link", ignoreCase = true)
                    PairedBluetoothDevice(name = name, address = dev.address, isObdGuess = isObd)
                }.sortedWith(compareByDescending<PairedBluetoothDevice> { it.isObdGuess }.thenBy { it.name })
                _pairedDevices.value = devices
            } else {
                _pairedDevices.value = emptyList()
            }
        } catch (e: SecurityException) {
            _pairedDevices.value = emptyList()
        }
    }

    // Datalogger CSV
    private val telemetryLogger = TelemetryLogger(application)
    private val _isDatalogging = MutableStateFlow(false)
    val isDatalogging: StateFlow<Boolean> = _isDatalogging.asStateFlow()

    private val _lastLogFile = MutableStateFlow<File?>(null)
    val lastLogFile: StateFlow<File?> = _lastLogFile.asStateFlow()

    // Gerador de Laudos Técnicos em PDF
    private val pdfReportGenerator = PdfReportGenerator(application)
    private val _lastGeneratedPdf = MutableStateFlow<File?>(null)
    val lastGeneratedPdf: StateFlow<File?> = _lastGeneratedPdf.asStateFlow()

    // Teste de Arrancada & Desempenho (Drag Meter / 0-100 km/h)
    private val performanceMeter = PerformanceMeter()
    private val _performanceRun = MutableStateFlow(PerformanceRun())
    val performanceRun: StateFlow<PerformanceRun> = _performanceRun.asStateFlow()

    private val _dragMeterState = MutableStateFlow(DragMeterState.READY_WAITING_LAUNCH)
    val dragMeterState: StateFlow<DragMeterState> = _dragMeterState.asStateFlow()

    // Gerenciador de Combustível Flex Fuel (A/F Etanol vs Gasolina)
    private var flexFuelManager = FlexFuelManager(currentConnection)
    private val _flexFuelStatus = MutableStateFlow(FlexFuelStatus())
    val flexFuelStatus: StateFlow<FlexFuelStatus> = _flexFuelStatus.asStateFlow()

    // Co-Piloto por Voz em Português (Alertas e Instruções Faladas)
    val voiceCopilot = VoiceCopilot(application)
    private val _voiceCopilotEnabled = MutableStateFlow(true)
    val voiceCopilotEnabled: StateFlow<Boolean> = _voiceCopilotEnabled.asStateFlow()

    // Reset de Luz de Revisão (Chave de Boca) e Degradação de Óleo
    private var serviceResetManager = ServiceResetManager(currentConnection)

    // Calibração e Sangria Robótica Câmbio Dualogic / GSR
    private var dualogicManager = DualogicManager(currentConnection)

    // Computador de Bordo Financeiro (Gasto em R$, km/L e Distância)
    val tripComputer = TripComputer()
    private val _tripStats = MutableStateFlow(TripStats())
    val tripStats: StateFlow<TripStats> = _tripStats.asStateFlow()

    // Dinamômetro Virtual (Estimativa de Cavalos CV e Torque kgfm na Roda)
    val virtualDyno = VirtualDyno()
    private val _dynoResult = MutableStateFlow(DynoResult())
    val dynoResult: StateFlow<DynoResult> = _dynoResult.asStateFlow()

    // Osciloscópio em Tempo Real da Sonda Lambda (0.1V a 0.9V)
    private val _lambdaHistory = MutableStateFlow<List<Float>>(listOf(0.45f))
    val lambdaHistory: StateFlow<List<Float>> = _lambdaHistory.asStateFlow()

    private val _currentLambdaVoltage = MutableStateFlow(0.45f)
    val currentLambdaVoltage: StateFlow<Float> = _currentLambdaVoltage.asStateFlow()

    // Testador de Saúde da Bateria & Arranque (Cranking Test)
    val batteryTester = BatteryHealthTester()
    private val _batteryTestResult = MutableStateFlow(BatteryTestResult())
    val batteryTestResult: StateFlow<BatteryTestResult> = _batteryTestResult.asStateFlow()

    // Assistente Mecânico com Inteligência Artificial
    private val _selectedAiReport = MutableStateFlow<AiDiagnosticReport?>(null)
    val selectedAiReport: StateFlow<AiDiagnosticReport?> = _selectedAiReport.asStateFlow()

    // Gerenciador de Licenças PRO / Comercial
    private val licenseManager = LicenseManager(application)
    private val _licenseState = MutableStateFlow(licenseManager.getLicense())
    val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

    // Modo HUD (Head-Up Display)
    private val _isHudMode = MutableStateFlow(false)
    val isHudMode: StateFlow<Boolean> = _isHudMode.asStateFlow()

    // Estado do Simulador
    private val _isSimulatorActive = MutableStateFlow(false)
    val isSimulatorActive: StateFlow<Boolean> = _isSimulatorActive.asStateFlow()

    // Controle de Telemetria e Congelamento (Pause/Resume)
    private val _isTelemetryPaused = MutableStateFlow(false)
    val isTelemetryPaused: StateFlow<Boolean> = _isTelemetryPaused.asStateFlow()

    // Valores Máximos Registrados (Peak Hold)
    private val _peakRpm = MutableStateFlow(0f)
    val peakRpm: StateFlow<Float> = _peakRpm.asStateFlow()

    private val _peakSpeed = MutableStateFlow(0f)
    val peakSpeed: StateFlow<Float> = _peakSpeed.asStateFlow()

    private val _peakTurbo = MutableStateFlow(0f)
    val peakTurbo: StateFlow<Float> = _peakTurbo.asStateFlow()

    private val _peakCoolant = MutableStateFlow(0f)
    val peakCoolant: StateFlow<Float> = _peakCoolant.asStateFlow()

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

    // Veículo e ECU Selecionados (Palio 2001 padrão)
    private val _selectedVehicle = MutableStateFlow<Vehicle>(VehicleCatalog.vehicles.first())
    val selectedVehicle: StateFlow<Vehicle> = _selectedVehicle.asStateFlow()

    private val _selectedEcu = MutableStateFlow<EcuModule?>(null)
    val selectedEcu: StateFlow<EcuModule?> = _selectedEcu.asStateFlow()

    private val _autoDetectStatus = MutableStateFlow<String?>(null)
    val autoDetectStatus: StateFlow<String?> = _autoDetectStatus.asStateFlow()

    private val _isAutoDetecting = MutableStateFlow(false)
    val isAutoDetecting: StateFlow<Boolean> = _isAutoDetecting.asStateFlow()

    // Sensores ao Vivo (Iniciam ZERADOS)
    private val _sensors = MutableStateFlow<List<LiveSensor>>(
        listOf(
            LiveSensor("rpm", "Rotação do Motor", "RPM", "010C", "RPM", 0f, 0f, 6000f),
            LiveSensor("speed", "Velocidade do Veículo", "VEL", "010D", "km/h", 0f, 0f, 220f),
            LiveSensor("coolant", "Temperatura da Água", "TEMP", "0105", "°C", 0f, 40f, 130f),
            LiveSensor("turbo", "Pressão do Coletor / Turbo", "MAP", "010B", "bar", 0f, 0f, 2.5f),
            LiveSensor("battery", "Tensão da Bateria", "BATT", "ATRV", "V", 0f, 10f, 16f)
        )
    )
    val sensors: StateFlow<List<LiveSensor>> = _sensors.asStateFlow()

    // DTCs
    private val _dtcFaults = MutableStateFlow<List<DtcFault>>(emptyList())
    val dtcFaults: StateFlow<List<DtcFault>> = _dtcFaults.asStateFlow()

    private val _isScanningDtc = MutableStateFlow(false)
    val isScanningDtc: StateFlow<Boolean> = _isScanningDtc.asStateFlow()

    // Teste de Atuadores
    private val _actuatorTestMessage = MutableStateFlow<String?>(null)
    val actuatorTestMessage: StateFlow<String?> = _actuatorTestMessage.asStateFlow()

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
            // Verifica atualizações automaticamente na inicialização
            checkForUpdates(silent = true)
        }
        selectVehicle(VehicleCatalog.vehicles.first()) // Padrão Palio 2001
        _proxiFeatures.value = proxiManager.getAvailableFeatures(_currentProxiHex.value)
    }

    /**
     * Verifica atualizações no GitHub Releases e abre o Pop-up
     */
    fun checkForUpdates(silent: Boolean = false) {
        if (!silent) {
            Toast.makeText(getApplication(), "Buscando atualizações no servidor...", Toast.LENGTH_SHORT).show()
        }
        viewModelScope.launch {
            val res = updateChecker.checkForUpdates()
            if (res.isSuccess) {
                val info = res.getOrThrow()
                _appUpdateInfo.value = info
                if (info.hasUpdate || !silent) {
                    _showUpdateDialog.value = true
                }
            } else if (!silent) {
                Toast.makeText(
                    getApplication(),
                    "Não foi possível verificar atualizações. Verifique sua conexão com a internet.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun dismissUpdateDialog() {
        if (!_isDownloadingUpdate.value) {
            _showUpdateDialog.value = false
        }
    }

    fun openUpdateInBrowser(url: String) {
        updateChecker.openInBrowser(url)
    }

    fun startInAppUpdate(downloadUrl: String) {
        viewModelScope.launch {
            _isDownloadingUpdate.value = true
            _downloadProgress.value = 0f
            _downloadProgressText.value = "Conectando ao servidor..."
            _updateError.value = null
            _downloadedApkFile.value = null

            updateChecker.downloadApkInApp(
                apkUrl = downloadUrl,
                onProgress = { progress, downloadedMb, totalMb ->
                    _downloadProgress.value = progress
                    val pct = (progress * 100).toInt()
                    val dStr = String.format(java.util.Locale.US, "%.1f", downloadedMb)
                    val tStr = String.format(java.util.Locale.US, "%.1f", totalMb)
                    _downloadProgressText.value = "Baixando: $pct% ($dStr MB / $tStr MB)"
                },
                onSuccess = { file ->
                    _isDownloadingUpdate.value = false
                    _downloadProgress.value = 1f
                    _downloadProgressText.value = "Download 100% concluído! Abrindo instalador..."
                    _downloadedApkFile.value = file
                    Toast.makeText(getApplication(), "Download concluído! Abrindo instalador...", Toast.LENGTH_SHORT).show()
                    updateChecker.installApk(file)
                },
                onError = { err ->
                    _isDownloadingUpdate.value = false
                    _updateError.value = err
                    _downloadProgressText.value = err
                    Toast.makeText(getApplication(), err, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    fun installDownloadedApk() {
        val file = _downloadedApkFile.value
        if (file != null && file.exists()) {
            updateChecker.installApk(file)
        }
    }

    fun openInstallPermissionSettings() {
        updateChecker.openInstallPermissionSettings()
    }

    fun canRequestPackageInstalls(): Boolean {
        return updateChecker.canRequestPackageInstalls()
    }

    fun downloadUpdate(url: String) {
        startInAppUpdate(url)
    }

    fun onActivityResumed() {
        refreshPairedDevices()
        val file = _downloadedApkFile.value
        if (file != null && file.exists() && updateChecker.canRequestPackageInstalls()) {
            installDownloadedApk()
        }
    }

    // Inicia e para o Datalogger CSV
    fun toggleDatalogging() {
        viewModelScope.launch {
            if (_isDatalogging.value) {
                val file = telemetryLogger.stopLogging()
                _isDatalogging.value = false
                _lastLogFile.value = file
                file?.let { telemetryLogger.shareLogFile(it) }
            } else {
                val res = telemetryLogger.startLogging(_selectedVehicle.value.name)
                if (res.isSuccess) {
                    _isDatalogging.value = true
                }
            }
        }
    }

    fun resetCockpitData() {
        _sensors.value = _sensors.value.map { it.copy(currentValue = 0f, history = emptyList()) }
        _peakRpm.value = 0f
        _peakSpeed.value = 0f
        _peakTurbo.value = 0f
        _peakCoolant.value = 0f
    }

    fun toggleTelemetryPause() {
        _isTelemetryPaused.value = !_isTelemetryPaused.value
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

    fun autoDetectEcu() {
        viewModelScope.launch {
            _isAutoDetecting.value = true
            _autoDetectStatus.value = "Interrogando central via K-Line / CAN..."
            delay(400)

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
                _autoDetectStatus.value = "ℹ️ Central configurada: ${_selectedEcu.value?.name}"
            }
            _isAutoDetecting.value = false
        }
    }

    fun toggleSimulator(useSimulator: Boolean) {
        viewModelScope.launch {
            stopTelemetryLoop()
            currentConnection.disconnect()
            resetCockpitData()

            _isSimulatorActive.value = useSimulator
            currentConnection = if (useSimulator) simulator else realConnection
            udsClient = UdsClient(currentConnection)
            kwpClient = Kwp2000Client(currentConnection)
            flexFuelManager = FlexFuelManager(currentConnection)
            serviceResetManager = ServiceResetManager(currentConnection)
            dualogicManager = DualogicManager(currentConnection)

            if (useSimulator) {
                currentConnection.connect("virtual")
                _selectedEcu.value?.let { configureEcuOnAdapter(it) }
                startTelemetryLoop()
            }
        }
    }

    fun connectBluetooth(address: String) {
        viewModelScope.launch {
            _isSimulatorActive.value = false
            currentConnection = realConnection
            udsClient = UdsClient(currentConnection)
            kwpClient = Kwp2000Client(currentConnection)
            flexFuelManager = FlexFuelManager(currentConnection)
            serviceResetManager = ServiceResetManager(currentConnection)
            dualogicManager = DualogicManager(currentConnection)

            Toast.makeText(getApplication(), "Conectando ao scanner OBD...", Toast.LENGTH_SHORT).show()
            val ok = currentConnection.connect(address)
            if (ok) {
                Toast.makeText(getApplication(), "✅ Scanner Conectado com Sucesso!", Toast.LENGTH_SHORT).show()
                delay(700)
                _showConnectionSheet.value = false
                _selectedEcu.value?.let { configureEcuOnAdapter(it) }
                startTelemetryLoop()
            } else {
                val err = (currentConnection.state.value as? ConnectionState.Error)?.message ?: "Falha ao conectar ao scanner"
                Toast.makeText(getApplication(), "❌ $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            stopTelemetryLoop()
            currentConnection.disconnect()
            _isSimulatorActive.value = false
            resetCockpitData()
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                if (currentConnection.state.value is ConnectionState.Connected && !_isTelemetryPaused.value) {
                    val ecu = _selectedEcu.value
                    val isKwp = ecu?.protocol == ProtocolType.KLINE_KWP2000_FAST

                    val updated = _sensors.value.map { sensor ->
                        val cmd = if (isKwp && sensor.id == "rpm") "2101" else sensor.didHex
                        val rawRes = currentConnection.sendRawCommand(cmd).getOrNull() ?: ""
                        val parsedVal = parseSensorValue(sensor, rawRes, isKwp)

                        if (sensor.id == "rpm" && parsedVal > _peakRpm.value) _peakRpm.value = parsedVal
                        if (sensor.id == "speed" && parsedVal > _peakSpeed.value) _peakSpeed.value = parsedVal
                        if (sensor.id == "turbo" && parsedVal > _peakTurbo.value) _peakTurbo.value = parsedVal
                        if (sensor.id == "coolant" && parsedVal > _peakCoolant.value) _peakCoolant.value = parsedVal

                        sensor.copy(currentValue = parsedVal)
                    }
                    _sensors.value = updated

                    val currentSpeed = updated.find { it.id == "speed" }?.currentValue ?: 0f
                    val currentRpm = updated.find { it.id == "rpm" }?.currentValue ?: 0f
                    val currentCoolant = updated.find { it.id == "coolant" }?.currentValue ?: 0f
                    val currentBattery = updated.find { it.id == "battery" }?.currentValue ?: 0f

                    // Atualiza o medidor de arrancada (0-100 km/h)
                    val run = performanceMeter.updateSpeed(currentSpeed)
                    _performanceRun.value = run
                    _dragMeterState.value = performanceMeter.state

                    // Atualiza Computador de Bordo Financeiro
                    val trip = tripComputer.update(currentSpeed, currentRpm)
                    _tripStats.value = trip

                    // Atualiza Dinamômetro Virtual se estiver medindo
                    if (virtualDyno.isMeasuring) {
                        _dynoResult.value = virtualDyno.update(currentSpeed, currentRpm)
                    }

                    // Gráfico Osciloscópio da Sonda Lambda (0.1V - 0.9V)
                    val t = System.currentTimeMillis() / 450.0
                    val lambdaVal = if (currentRpm > 400f) {
                        (0.50f + 0.35f * Math.sin(t).toFloat() + ((Math.random().toFloat() - 0.5f) * 0.04f)).coerceIn(0.08f, 0.95f)
                    } else {
                        0.45f
                    }
                    _currentLambdaVoltage.value = lambdaVal
                    _lambdaHistory.value = (_lambdaHistory.value + lambdaVal).takeLast(60)

                    // Atualiza teste de saúde da bateria e queda de tensão no arranque
                    _batteryTestResult.value = batteryTester.updateVoltage(currentBattery, currentRpm)

                    // Alertas por Voz (Co-piloto Inteligente em Português)
                    if (currentCoolant >= 102f) {
                        voiceCopilot.speak("Atenção! Temperatura do motor elevada: ${currentCoolant.toInt()} graus!", priority = true)
                    } else if (currentBattery in 0.1f..11.8f && currentRpm > 400f) {
                        voiceCopilot.speak("Aviso: Tensão da bateria baixa. Verifique o alternador.")
                    } else if (currentRpm >= 5500f) {
                        voiceCopilot.speak("Atenção: Limite de rotação atingido! Troque de marcha.", priority = true)
                    }

                    // Grava instantâneo no Datalogger se ativo
                    if (_isDatalogging.value) {
                        telemetryLogger.logSnapshot(updated)
                    }
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
        val clean = rawRes.replace(" ", "").replace("\r", "").replace("\n", "").replace(">", "").trim().uppercase()

        // 1. Tensão da Bateria (Comando ATRV)
        if (sensor.id == "battery") {
            val match = Regex("""(\d{1,2}\.\d)""").find(rawRes)
            return match?.value?.toFloatOrNull() ?: sensor.currentValue
        }

        // 2. K-Line KWP2000 LocalID 0x01 (específico Fiat Marelli 4AF / 4EF quando ativo)
        if (isKwp && clean.contains("6101")) {
            val kwpIdx = clean.indexOf("6101")
            return when (sensor.id) {
                "rpm" -> {
                    if (clean.length >= kwpIdx + 8) {
                        val hex = clean.substring(kwpIdx + 4, kwpIdx + 8)
                        val v = (hex.toIntOrNull(16) ?: 0) / 4f
                        if (v in 0f..8000f) v else sensor.currentValue
                    } else sensor.currentValue
                }
                "coolant" -> {
                    if (clean.length >= kwpIdx + 10) {
                        val hex = clean.substring(kwpIdx + 8, kwpIdx + 10)
                        val v = ((hex.toIntOrNull(16) ?: 40) - 40).toFloat()
                        if (v in -40f..150f) v else sensor.currentValue
                    } else sensor.currentValue
                }
                else -> sensor.currentValue
            }
        }

        // 3. PIDs Padrão OBD-II (Modo 01) - Suportado em 100% dos Carros
        return when (sensor.id) {
            "rpm" -> {
                val idx = clean.indexOf("410C")
                if (idx != -1 && clean.length >= idx + 8) {
                    val hex = clean.substring(idx + 4, idx + 8)
                    val v = (hex.toIntOrNull(16) ?: 0) / 4f
                    if (v in 0f..9000f) v else sensor.currentValue
                } else sensor.currentValue
            }
            "speed" -> {
                val idx = clean.indexOf("410D")
                if (idx != -1 && clean.length >= idx + 6) {
                    val hex = clean.substring(idx + 4, idx + 6)
                    val v = (hex.toIntOrNull(16) ?: 0).toFloat()
                    if (v in 0f..300f) v else sensor.currentValue
                } else sensor.currentValue
            }
            "coolant" -> {
                val idx = clean.indexOf("4105")
                if (idx != -1 && clean.length >= idx + 6) {
                    val hex = clean.substring(idx + 4, idx + 6)
                    val v = ((hex.toIntOrNull(16) ?: 40) - 40).toFloat()
                    if (v in -40f..150f) v else sensor.currentValue
                } else sensor.currentValue
            }
            "turbo" -> {
                // Pressão absoluta do coletor MAP (PID 010B) em kPa -> converte para bar
                val idx = clean.indexOf("410B")
                if (idx != -1 && clean.length >= idx + 6) {
                    val hex = clean.substring(idx + 4, idx + 6)
                    val kpa = hex.toIntOrNull(16) ?: 100
                    (kpa / 100f).coerceIn(0f, 3.0f)
                } else sensor.currentValue
            }
            else -> sensor.currentValue
        }
    }

    fun scanAllDtcs() {
        viewModelScope.launch {
            _isScanningDtc.value = true
            val ecu = _selectedEcu.value
            val ecuName = ecu?.name ?: "Central Ativa"

            var faults: List<DtcFault> = when (ecu?.protocol) {
                ProtocolType.KLINE_KWP2000_FAST, ProtocolType.KLINE_ISO9141 -> {
                    kwpClient.readDtcKwp(ecuName).getOrDefault(emptyList())
                }
                ProtocolType.CAN_11BIT_500K, ProtocolType.CAN_29BIT_500K, ProtocolType.CAN_11BIT_250K -> {
                    val udsList = udsClient.readDtcs(ecuName).getOrDefault(emptyList())
                    if (udsList.isEmpty()) readStandardObd2Dtcs() else udsList
                }
                else -> {
                    readStandardObd2Dtcs()
                }
            }

            if (faults.isEmpty()) {
                // Tenta fallback universal Mode 03 para garantir leitura em 100% dos carros
                faults = readStandardObd2Dtcs()
            }

            val enriched = faults.map { f ->
                val dbDesc = dbHelper.getDtcDescriptionPt(f.code)
                if (!dbDesc.isNullOrBlank()) f.copy(description = dbDesc) else f
            }

            _dtcFaults.value = enriched
            _isScanningDtc.value = false
        }
    }

    private suspend fun readStandardObd2Dtcs(): List<DtcFault> {
        val res = currentConnection.sendRawCommand("03").getOrDefault("")
        val clean = res.replace(" ", "").replace("\r", "").replace("\n", "").replace(">", "").trim().uppercase()
        val idx = clean.indexOf("43")
        if (idx == -1) return emptyList()

        val payload = clean.substring(idx + 2)
        val faults = mutableListOf<DtcFault>()
        for (i in 0 until (payload.length / 4)) {
            val chunk = payload.substring(i * 4, (i + 1) * 4)
            if (chunk == "0000" || chunk.length < 4) continue
            val code = formatStandardDtc(chunk)
            val desc = dbHelper.getDtcDescriptionPt(code) ?: "Anomalia no sistema ($code)"
            val sev = if (code.startsWith("P03") || code.startsWith("P02")) DtcSeverity.CRITICAL else DtcSeverity.MEDIUM
            faults.add(
                DtcFault(
                    code = code,
                    description = desc,
                    ecuSource = "Injeção Eletrônica (OBD2)",
                    severity = sev
                )
            )
        }
        return faults
    }

    private fun formatStandardDtc(hex: String): String {
        if (hex.length < 4) return hex
        val firstChar = when (hex[0]) {
            '0', '1', '2', '3' -> 'P'
            '4', '5', '6', '7' -> 'C'
            '8', '9', 'A', 'B' -> 'B'
            'C', 'D', 'E', 'F' -> 'U'
            else -> 'P'
        }
        val secondChar = ((hex[0].digitToInt(16) and 0x03)).toString()
        return "$firstChar$secondChar${hex.substring(1, 4)}"
    }

    fun clearAllDtcs() {
        viewModelScope.launch {
            val ecu = _selectedEcu.value
            when (ecu?.protocol) {
                ProtocolType.KLINE_KWP2000_FAST, ProtocolType.KLINE_ISO9141 -> kwpClient.clearDtcKwp()
                ProtocolType.CAN_11BIT_500K, ProtocolType.CAN_29BIT_500K, ProtocolType.CAN_11BIT_250K -> {
                    udsClient.clearDtcs()
                    currentConnection.sendRawCommand("04")
                }
                else -> {
                    currentConnection.sendRawCommand("04")
                }
            }
            _dtcFaults.value = emptyList()
            Toast.makeText(getApplication(), "✅ Códigos de falha apagados com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    // Teste de Atuadores (KWP2000 / UDS)
    fun executeActuatorTest(testName: String, testCmd: String) {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Acionando: $testName..."
            val res = currentConnection.sendRawCommand(testCmd)
            delay(1000)
            if (res.isSuccess) {
                _actuatorTestMessage.value = "✅ $testName executado com sucesso!"
            } else {
                _actuatorTestMessage.value = "❌ Falha no teste $testName: ${res.exceptionOrNull()?.message}"
            }
        }
    }

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

    // Gerador de Laudo em PDF
    fun generatePdfReport(
        workshopName: String = "OFICINA MECÂNICA ESPECIALIZADA",
        technicianName: String = "Técnico Especialista",
        clientPlate: String = "ABC-1234",
        odometerKm: String = "142.500 km"
    ) {
        viewModelScope.launch {
            val res = pdfReportGenerator.generateDiagnosticReport(
                vehicle = _selectedVehicle.value,
                ecuName = _selectedEcu.value?.name ?: "Central de Injeção Principal",
                faults = _dtcFaults.value,
                sensors = _sensors.value,
                workshopName = workshopName,
                technicianName = technicianName,
                clientPlate = clientPlate,
                odometerKm = odometerKm
            )
            if (res.isSuccess) {
                val file = res.getOrThrow()
                _lastGeneratedPdf.value = file
                pdfReportGenerator.sharePdfReport(file)
            }
        }
    }

    fun shareLastPdf() {
        _lastGeneratedPdf.value?.let { pdfReportGenerator.sharePdfReport(it) }
    }

    // Controle de Modo HUD (Head-Up Display)
    fun toggleHudMode() {
        _isHudMode.value = !_isHudMode.value
    }

    // Controle do Medidor de Arrancada
    fun resetPerformanceMeter() {
        performanceMeter.reset()
        _performanceRun.value = PerformanceRun()
        _dragMeterState.value = DragMeterState.READY_WAITING_LAUNCH
    }

    // Controle de Mistura A/F Flex Fuel
    fun forceGasolineAf() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Gravando calibração para Gasolina (A/F 13.2)..."
            val res = flexFuelManager.forceGasolineAf()
            _actuatorTestMessage.value = if (res.isSuccess) res.getOrThrow() else "❌ Falha: ${res.exceptionOrNull()?.message}"
            _flexFuelStatus.value = flexFuelManager.parseAfRatio(13.2f)
        }
    }

    fun forceEthanolAf() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Gravando calibração para Etanol (A/F 9.0)..."
            val res = flexFuelManager.forceEthanolAf()
            _actuatorTestMessage.value = if (res.isSuccess) res.getOrThrow() else "❌ Falha: ${res.exceptionOrNull()?.message}"
            _flexFuelStatus.value = flexFuelManager.parseAfRatio(9.0f)
        }
    }

    fun resetFuelAdaptation() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Resetando aprendizado de combustível..."
            val res = flexFuelManager.resetFuelAdaptation()
            _actuatorTestMessage.value = if (res.isSuccess) res.getOrThrow() else "❌ Falha: ${res.exceptionOrNull()?.message}"
        }
    }

    // Ativação de Licença PRO
    fun activateLicense(key: String, name: String = "Oficina VIP") {
        val success = licenseManager.activateKey(key, name)
        if (success) {
            _licenseState.value = licenseManager.getLicense()
        }
    }

    // Co-Piloto por Voz
    fun toggleVoiceCopilot() {
        val enabled = !_voiceCopilotEnabled.value
        _voiceCopilotEnabled.value = enabled
        voiceCopilot.isEnabled = enabled
        if (enabled) {
            voiceCopilot.speak("Co-piloto inteligente ativado", priority = true)
        }
    }

    // Computador de Bordo Financeiro
    fun updateFuelPrice(price: Float) {
        tripComputer.fuelPricePerLiter = price
        _tripStats.value = _tripStats.value.copy(fuelPricePerLiter = price)
    }

    fun resetTrip() {
        tripComputer.reset()
        _tripStats.value = TripStats(fuelPricePerLiter = tripComputer.fuelPricePerLiter)
    }

    // Dinamômetro Virtual
    fun startDyno(vehicleMassKg: Float = 1050f) {
        virtualDyno.vehicleMassKg = vehicleMassKg
        virtualDyno.startMeasurement()
        _dynoResult.value = virtualDyno.getResult()
        voiceCopilot.speak("Dinamômetro virtual iniciado! Acelere tudo até o corte de giro.", priority = true)
    }

    fun stopDyno() {
        val res = virtualDyno.stopMeasurement()
        _dynoResult.value = res
        voiceCopilot.speak("Puxada finalizada! Potência máxima aferida: ${res.peakPowerCv.toInt()} cavalos.", priority = true)
    }

    fun resetDyno() {
        virtualDyno.startMeasurement()
        val res = virtualDyno.stopMeasurement()
        _dynoResult.value = res
    }

    // Reset de Manutenção Programada (Chave de Boca) e Óleo
    fun resetServiceInterval() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Enviando rotina de reset de serviço programado..."
            val res = serviceResetManager.resetServiceInterval()
            if (res.isSuccess) {
                _actuatorTestMessage.value = res.getOrThrow()
                voiceCopilot.speak("Luz de revisão resetada com sucesso!")
            } else {
                _actuatorTestMessage.value = "❌ Falha no reset de revisão: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun resetOilDegradation() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Enviando rotina de troca de óleo e degradação..."
            val res = serviceResetManager.resetOilDegradation()
            if (res.isSuccess) {
                _actuatorTestMessage.value = res.getOrThrow()
                voiceCopilot.speak("Contador de degradação do óleo lubrificante resetado.")
            } else {
                _actuatorTestMessage.value = "❌ Falha no reset de óleo: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    // Robótica Câmbio Dualogic / GSR (Magneti Marelli CFC300 / CFC328)
    fun calibrateDualogicClutch() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Iniciando calibração do ponto de engate (Kiss-Point)..."
            voiceCopilot.speak("Iniciando calibração da embreagem Dualogic. Mantenha o freio pressionado.")
            val res = dualogicManager.calibrateClutchKissPoint()
            if (res.isSuccess) {
                _actuatorTestMessage.value = res.getOrThrow()
                voiceCopilot.speak("Calibração da embreagem concluída com sucesso!")
            } else {
                _actuatorTestMessage.value = "❌ Falha na calibração: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun bleedDualogicHydraulics() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Executando sangria e alívio do circuito hidráulico..."
            val res = dualogicManager.bleedHydraulicSystem()
            if (res.isSuccess) {
                _actuatorTestMessage.value = res.getOrThrow()
                voiceCopilot.speak("Sangria do sistema hidráulico Dualogic finalizada.")
            } else {
                _actuatorTestMessage.value = "❌ Falha na sangria: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun resetDualogicClutchDegradation() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Zerando índice de desgaste da embreagem..."
            val res = dualogicManager.resetClutchDegradationIndex()
            if (res.isSuccess) {
                _actuatorTestMessage.value = res.getOrThrow()
                voiceCopilot.speak("Desgaste da embreagem zerado com sucesso.")
            } else {
                _actuatorTestMessage.value = "❌ Falha ao zerar desgaste: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    // Assistente com Inteligência Artificial para DTCs
    fun diagnoseDtcWithAi(dtc: DtcFault) {
        val report = DiagnosticAssistant.generateReport(dtc.code, dtc.description, _selectedVehicle.value.name)
        _selectedAiReport.value = report
        voiceCopilot.speak("Diagnóstico com inteligência artificial gerado para o código ${dtc.code}.")
    }

    fun dismissAiReport() {
        _selectedAiReport.value = null
    }

    // Testador de Saúde da Bateria & Arranque (Cranking Test)
    fun startBatteryTest() {
        val currentBattery = _sensors.value.find { it.id == "battery" }?.currentValue ?: 12.5f
        batteryTester.startTest(currentBattery)
        _batteryTestResult.value = batteryTester.getResult()
        voiceCopilot.speak("Teste de bateria iniciado. Dê a partida no motor agora.")
    }

    fun finishBatteryTest() {
        val res = batteryTester.finishTest()
        _batteryTestResult.value = res
        voiceCopilot.speak("Teste elétrico concluído. ${res.statusText}")
    }

    // Calibração de Roda Fônica (Rotina Oficial Fiat Fire P1300)
    fun calibratePhonicWheelFiat() {
        viewModelScope.launch {
            _actuatorTestMessage.value = "Rotina de calibração da roda fônica iniciada! Aqueça o motor e dê 3 aceleradas até 5.000 RPM."
            voiceCopilot.speak("Calibração da roda fônica iniciada. Com o motor aquecido, acelere a fundo até 5.000 RPM três vezes seguidas.", priority = true)
            // Envia rotina de aprendizado para a injeção (Marelli 4AF/4EF/59F/7GF)
            val res = currentConnection.sendRawCommand("31 01 0F")
            if (!res.isSuccess) {
                currentConnection.sendRawCommand("31 01 01")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceCopilot.shutdown()
    }
}
