package de.frank.cortex

import android.net.VpnService
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
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
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.ui.common.CortexTopBar
import de.frank.cortex.ui.chat.ChatCommands
import de.frank.cortex.ui.navigation.AppNavGraph
import de.frank.cortex.ui.navigation.Screen
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager

class MainActivity : FragmentActivity() {

    private var authenticatedThisForeground = false
    private var appUnlocked by mutableStateOf(false)

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
        if (SettingsStore.wgConfig.isBlank()) return
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnConsentLauncher.launch(prepareIntent)
        } else {
            lifecycleScope.launch { WireGuardManager.connect() }
        }
    }

    private fun authenticateThenConnect() {
        if (!SettingsStore.biometricLockEnabled) {
            appUnlocked = true
            startVpn()
            return
        }
        if (authenticatedThisForeground) {
            appUnlocked = true
            startVpn()
            return
        }
        appUnlocked = false
        val available = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (available != BiometricManager.BIOMETRIC_SUCCESS) return

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticatedThisForeground = true
                    appUnlocked = true
                    startVpn()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    appUnlocked = false
                    lifecycleScope.launch { WireGuardManager.disconnect() }
                }

                override fun onAuthenticationFailed() {
                    appUnlocked = false
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Cortex entsperren")
            .setSubtitle("Fingerabdruck oder Gerätesperre verwenden")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }

    // Als Feld gehalten, damit er in onDestroy wieder ABGEMELDET werden kann. Vorher wurde bei
    // jeder Activity-Recreation (Rotation, System-Theme, Prozess-Restore) ein NEUER anonymer
    // Observer am ProcessLifecycleOwner registriert und nie entfernt — jeder alte hielt die
    // zerstoerte Activity fest (Leak) und Auth/Disconnect liefen mehrfach.
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            authenticateThenConnect()
        }

        override fun onStop(owner: LifecycleOwner) {
            authenticatedThisForeground = false
            appUnlocked = false
            lifecycleScope.launch { WireGuardManager.disconnect() }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)

        setContent {
            var themeMode by rememberSaveable { mutableStateOf(SettingsStore.themeMode) }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "light" -> false
                "system" -> systemDark
                else -> true
            }
            val vpnState by WireGuardManager.state.collectAsStateWithLifecycle()

            CortexTheme(darkTheme = isDark) {
                if (SettingsStore.biometricLockEnabled && !appUnlocked) {
                    LockedScreen(isDark = isDark, onUnlock = { authenticateThenConnect() })
                    return@CortexTheme
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (isDark) DarkBg else LightBg,
                    topBar = {
                        CortexTopBar(
                            isDark = isDark,
                            themeMode = themeMode,
                            onSetThemeMode = { mode ->
                                themeMode = mode
                                SettingsStore.themeMode = mode
                            },
                            vpnState = vpnState,
                            onVpnToggle = { enabled ->
                                if (enabled) startVpn()
                                else lifecycleScope.launch { WireGuardManager.disconnect() }
                            },
                            showNewChat = currentRoute == Screen.Chat.route,
                            onNewChat = { ChatCommands.requestNewChat() },
                            onOpenSessions = { ChatCommands.requestOpenSessions() }
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
                                listOf(Screen.Chat, Screen.Dashboard, Screen.Settings).forEach { screen ->
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
                        themeMode = themeMode,
                        onSetThemeMode = { mode ->
                            themeMode = mode
                            SettingsStore.themeMode = mode
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedScreen(isDark: Boolean, onUnlock: () -> Unit) {
    val bg = if (isDark) DarkBg else LightBg
    val surface = if (isDark) DarkSurface else LightSurface
    val border = if (isDark) DarkBorder else LightBorder
    val text = if (isDark) Color.White else LightText
    val muted = if (isDark) DarkMuted else LightMuted

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Iris.copy(alpha = 0.20f), bg),
                    radius = 900f
                )
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 58.dp, y = (-70).dp)
                .size(190.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Mint.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 64.dp)
                .size(220.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Iris.copy(alpha = 0.13f))
        )

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, border),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Willkommen bei Cortex",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = text
                )
                Text(
                    "Bitte die App entsperren",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    letterSpacing = 1.2.sp,
                    color = muted
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Iris.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Iris.copy(alpha = 0.30f)),
                    modifier = Modifier.clickable(onClick = onUnlock)
                ) {
                    Text(
                        "Entsperren",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Iris
                    )
                }
            }
        }
    }
}
