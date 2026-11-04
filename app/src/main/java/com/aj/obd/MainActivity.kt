package com.aj.obd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aj.obd.ui.MainViewModel
import com.aj.obd.ui.screens.ConnectionScreen
import com.aj.obd.ui.screens.DashboardScreen
import com.aj.obd.ui.screens.DiagnosticsScreen
import com.aj.obd.ui.screens.HomeScreen
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
    DIAGNOSTICS("Falhas", Icons.Default.Warning),
    PROXI("Proxi", Icons.Default.Build),
    CONNECTION("Conexão", Icons.Default.SettingsInputAntenna)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ObdAjTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

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
                    onNavigateToConnection = { currentScreen = Screen.CONNECTION }
                )
                Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                Screen.DIAGNOSTICS -> DiagnosticsScreen(viewModel = viewModel)
                Screen.PROXI -> ProxiScreen(viewModel = viewModel)
                Screen.CONNECTION -> ConnectionScreen(viewModel = viewModel)
            }
        }
    }
}
