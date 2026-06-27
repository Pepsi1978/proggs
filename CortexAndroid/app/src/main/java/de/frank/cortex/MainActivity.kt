package de.frank.cortex

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.frank.cortex.ui.common.CortexTopBar
import de.frank.cortex.ui.navigation.AppNavGraph
import de.frank.cortex.ui.navigation.Screen
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager

class MainActivity : ComponentActivity() {

    // Android-VPN-Erlaubnis: Ergebnis des System-Dialogs (einmalig pro Installation).
    private val vpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch { WireGuardManager.connect() }
        } else {
            WireGuardManager.reportConsentDenied()
        }
    }

    /** Holt bei Bedarf die VPN-Erlaubnis (System-Dialog), dann verbindet sie der Tunnel. */
    private fun startVpn() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnConsentLauncher.launch(prepareIntent)
        } else {
            lifecycleScope.launch { WireGuardManager.connect() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDark by rememberSaveable { mutableStateOf(true) }
            val vpnState by WireGuardManager.state.collectAsState()

            CortexTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (isDark) DarkBg else LightBg,
                    topBar = {
                        CortexTopBar(
                            isDark = isDark,
                            onToggleTheme = { isDark = !isDark },
                            vpnState = vpnState,
                            onVpnToggle = { enabled ->
                                if (enabled) startVpn()
                                else lifecycleScope.launch { WireGuardManager.disconnect() }
                            }
                        )
                    },
                    bottomBar = {
                        // Bottom Nav: exact design — pill indicator for active tab
                        Surface(
                            color = if (isDark) DarkSurface else LightSurface,
                            border = BorderStroke(1.dp, if (isDark) DarkBorder else LightBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .navigationBarsPadding(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Screen.bottomScreens.forEach { screen ->
                                    val active = currentRoute == screen.route
                                    val pillColor = if (active) Iris.copy(alpha = 0.16f) else Color.Transparent
                                    val iconColor = if (active) Iris else if (isDark) DarkMuted else LightMuted
                                    val textColor = if (active) Iris else if (isDark) DarkMuted else LightMuted
                                    val fontWeight = if (active) FontWeight.Bold else FontWeight.Medium

                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable {
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        // Pill with icon (58x30, radius 999)
                                        Box(
                                            modifier = Modifier
                                                .width(58.dp)
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(pillColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                screen.icon,
                                                contentDescription = screen.label,
                                                tint = iconColor,
                                                modifier = Modifier.size(23.dp)
                                            )
                                        }
                                        Text(
                                            text = screen.label,
                                            fontSize = 11.sp,
                                            fontWeight = fontWeight,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        isDark = isDark,
                        onToggleTheme = { isDark = !isDark },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
