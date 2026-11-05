package com.aj.obd

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aj.obd.ui.MainViewModel
import com.aj.obd.ui.components.BluetoothConnectionSheet
import com.aj.obd.ui.components.UpdateDialog
import com.aj.obd.ui.screens.CableGuideScreen
import com.aj.obd.ui.screens.ConnectionScreen
import com.aj.obd.ui.screens.DashboardScreen
import com.aj.obd.ui.screens.DiagnosticsScreen
import com.aj.obd.ui.screens.HomeScreen
import com.aj.obd.ui.screens.PerformanceScreen
import com.aj.obd.ui.screens.ProxiScreen
import com.aj.obd.ui.theme.AmberGlow
import com.aj.obd.ui.theme.CarbonDark
import com.aj.obd.ui.theme.ObdAjTheme
import com.aj.obd.ui.theme.SurfaceDark
import com.aj.obd.ui.theme.TextPrimary
import com.aj.obd.ui.theme.TextSecondary

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("Início", Icons.Default.DirectionsCar),
    DASHBOARD("Cockpit", Icons.Default.Speed),
    PERFORMANCE("0-100", Icons.Default.Timer),
    DIAGNOSTICS("Falhas", Icons.Default.Warning),
    CABLES("Cabos", Icons.Default.Cable),
    PROXI("Proxi", Icons.Default.Build)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestBluetoothPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshPairedDevices()
    }

    private fun checkBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestBluetoothPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBluetoothPermissions()
        setContent {
            ObdAjTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkBluetoothPermissions()
        viewModel.onActivityResumed()
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsState()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadProgressText by viewModel.downloadProgressText.collectAsState()
    val downloadedApkFile by viewModel.downloadedApkFile.collectAsState()
    val updateError by viewModel.updateError.collectAsState()
    val showConnectionSheet by viewModel.showConnectionSheet.collectAsState()

    // Painel Futurista de Conexão Bluetooth
    if (showConnectionSheet) {
        BluetoothConnectionSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissConnectionSheet() }
        )
    }

    // Pop-up de Atualização Oficial
    if (showUpdateDialog && appUpdateInfo != null) {
        UpdateDialog(
            updateInfo = appUpdateInfo!!,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onDownloadBrowser = { url -> viewModel.downloadViaBrowser(url) },
            onDownloadManager = { url, version -> viewModel.downloadViaDownloadManager(url, version) }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CarbonDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(text = screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = AmberGlow,
                            indicatorColor = AmberGlow,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = { currentScreen = Screen.DASHBOARD },
                    onNavigateToDtc = { currentScreen = Screen.DIAGNOSTICS },
                    onNavigateToProxi = { currentScreen = Screen.PROXI },
                    onNavigateToPerformance = { currentScreen = Screen.PERFORMANCE },
                    onNavigateToCables = { currentScreen = Screen.CABLES },
                    onNavigateToConnection = { viewModel.openConnectionSheet() }
                )
                Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                Screen.PERFORMANCE -> PerformanceScreen(viewModel = viewModel)
                Screen.DIAGNOSTICS -> DiagnosticsScreen(viewModel = viewModel)
                Screen.CABLES -> CableGuideScreen()
                Screen.PROXI -> ProxiScreen(viewModel = viewModel)
            }
        }
    }
}
