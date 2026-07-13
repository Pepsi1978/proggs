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

/** Prozessweiter Sperr-Zustand: als Activity-Felder gingen diese Flags bei jeder Rotation/
 *  Theme-Aenderung verloren — der BiometricPrompt poppte dann bei JEDER Rotation erneut auf,
 *  obwohl die App nie den Vordergrund verlassen hatte. */
internal object AppLockState {
    var authenticatedThisForeground = false
    var appUnlocked by mutableStateOf(false)
    // true = fuer DIESEN Vordergrund-Aufenthalt lief authenticateThenConnect bereits.
    // Der ProcessLifecycleOwner liefert neuen Observern bei Activity-Recreation (Rotation/
    // Theme-Wechsel) ein Catch-up-onStart nach — ohne dieses Flag poppte der BiometricPrompt
    // bei jeder Rotation erneut auf und ein bewusst getrenntes VPN verband sich ungefragt neu.
    var foregroundAuthHandled = false
    // true, solange der eigene Auth-Prompt laeuft. Auf API 26-28 oeffnet der Geraetesperre-
    // Pfad eine FREMDE Fullscreen-Activity (Keyguard) — der ProcessLifecycle-onStop wertete
    // das als "App verlassen", setzte alle Flags zurueck und trennte das VPN: Endlosschleife
    // der PIN-Abfrage. Der CortexApp-Observer ueberspringt Reset+Disconnect, solange true.
    var authInProgress = false
}

class MainActivity : FragmentActivity() {

    // Android-VPN-Erlaubnis: Ergebnis des System-Dialogs (einmalig pro Installation).
    private val vpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // connect() ist nicht-suspendierend (launcht intern) — der fruehere
            // lifecycleScope-Wrapper war unnoetig und lief auf einer zerstoerten
            // Activity (Rotation waehrend des Prompts) nie.
            WireGuardManager.connect()
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
            WireGuardManager.connect()
        }
    }

    // In onCreate (re-)erstellt statt ad-hoc: androidx.biometric legt den Callback im
    // Activity-scoped (Rotation UEBERLEBENDEN) BiometricViewModel ab — ein ad-hoc erzeugter
    // Prompt hielt nach einer Rotation die ZERSTOERTE Activity fest: der Success-Callback
    // lief dann auf deren gecanceltem lifecycleScope (VPN verband nie) bzw. crashte am
    // deregistrierten Consent-Launcher. Die Neu-Erstellung in onCreate bindet den Callback
    // immer an die aktuelle Activity (dokumentiertes androidx-Muster).
    private lateinit var biometricPrompt: BiometricPrompt

    private fun authenticateThenConnect() {
        if (!SettingsStore.biometricLockEnabled) {
            AppLockState.appUnlocked = true
            startVpn()
            return
        }
        if (AppLockState.authenticatedThisForeground) {
            AppLockState.appUnlocked = true
            startVpn()
            return
        }
        AppLockState.appUnlocked = false
        val available = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (available != BiometricManager.BIOMETRIC_SUCCESS) {
            // Keine Biometrie/Geraetesperre (mehr) verfuegbar: NICHT stumm aussperren — vorher
            // blieb die App dauerhaft auf der LockedScreen haengen ("Entsperren" tat nichts).
            // Ohne einrichtbare Sperre schuetzt der Lock ohnehin nichts -> entsperren + melden.
            de.frank.cortex.observability.CortexLog.warn(
                "MainActivity", "auth",
                "Biometrie/Geraetesperre nicht verfuegbar (Code $available) — Sperre uebersprungen"
            )
            android.widget.Toast.makeText(
                this,
                "Biometrie/Gerätesperre nicht verfügbar — Sperre übersprungen",
                android.widget.Toast.LENGTH_LONG
            ).show()
            AppLockState.appUnlocked = true
            startVpn()
            return
        }

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Cortex entsperren")
            .setSubtitle("Fingerabdruck oder Gerätesperre verwenden")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        AppLockState.authInProgress = true
        biometricPrompt.authenticate(info)
    }

    // Als Feld gehalten, damit er in onDestroy wieder ABGEMELDET werden kann. Vorher wurde bei
    // jeder Activity-Recreation (Rotation, System-Theme, Prozess-Restore) ein NEUER anonymer
    // Observer am ProcessLifecycleOwner registriert und nie entfernt — jeder alte hielt die
    // zerstoerte Activity fest (Leak) und Auth/Disconnect liefen mehrfach.
    // NUR onStart lebt hier (braucht die Activity fuer BiometricPrompt/VpnService.prepare);
    // der Hintergrund-Disconnect liegt in CortexApp: Der ProcessLifecycle-onStop kommt ~700 ms
    // NACH Activity.onStop — wurde die Activity gefinished (Back auf Android 8-11), war dieser
    // Observer schon entfernt und der Tunnel blieb dauerhaft verbunden.
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // Nur beim ECHTEN Vordergrund-Eintritt (CortexApp-onStop setzt das Flag zurueck),
            // nicht beim Catch-up-onStart nach einer Activity-Recreation.
            if (!AppLockState.foregroundAuthHandled) {
                AppLockState.foregroundAuthHandled = true
                authenticateThenConnect()
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mit aktiver Biometrie-Sperre auch das Recents-Thumbnail schuetzen: der System-
        // Screenshot entsteht beim Verlassen der App (appUnlocked ist da noch true) und zeigte
        // sonst den kompletten entsperrten Inhalt in der Task-Uebersicht.
        if (SettingsStore.biometricLockEnabled) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLockState.authInProgress = false
                    AppLockState.authenticatedThisForeground = true
                    AppLockState.appUnlocked = true
                    startVpn()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    AppLockState.authInProgress = false
                    AppLockState.appUnlocked = false
                    lifecycleScope.launch { WireGuardManager.disconnect() }
                }

                override fun onAuthenticationFailed() {
                    // Einzelner Fehlversuch — der Prompt laeuft weiter (authInProgress bleibt true).
                    AppLockState.appUnlocked = false
                }
            }
        )

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

            // Statusbar-Icons dem APP-Theme folgen lassen: enableEdgeToEdge() mit Defaults nahm
            // das SYSTEM-Theme — bei hellem System-Theme und dunkler App (App-Default!) waren
            // Uhr/Batterie auf dunklem Hintergrund praktisch unsichtbar.
            LaunchedEffect(isDark) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) androidx.activity.SystemBarStyle.dark(transparent)
                    else androidx.activity.SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (isDark) androidx.activity.SystemBarStyle.dark(transparent)
                    else androidx.activity.SystemBarStyle.light(transparent, transparent)
                )
            }

            CortexTheme(darkTheme = isDark) {
                // LockedScreen als OVERLAY statt frueher Return: der fruehe Return warf den
                // kompletten NavHost samt Back-Stack und Screen-State weg — nach jedem
                // Hintergrund-Wechsel landete man wieder auf "Chat" mit leerem Zustand.
                Box(Modifier.fillMaxSize()) {

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
                            showSessions = currentRoute == Screen.Chat.route,
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

                if (SettingsStore.biometricLockEnabled && !AppLockState.appUnlocked) {
                    LockedScreen(isDark = isDark, onUnlock = { authenticateThenConnect() })
                }
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
            // Klick-Schlucker: als Overlay ueber dem NavHost muessen Touches hier enden,
            // sonst waere die gesperrte App darunter weiter bedienbar.
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            // OPAKE Grundflaeche ZUERST: der radiale Verlauf beginnt mit Alpha 0,20 — als
            // Overlay schien der zu schuetzende App-Inhalt sonst in der Bildschirmmitte durch.
            .background(bg)
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
