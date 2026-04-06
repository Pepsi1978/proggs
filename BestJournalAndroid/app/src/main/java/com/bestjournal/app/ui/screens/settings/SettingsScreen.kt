package com.bestjournal.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import com.bestjournal.app.util.Constants
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.util.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onSignOut: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Click sound helper � plays only when sounds are enabled
    val clickPrefs = remember {
        val mk = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
        androidx.security.crypto.EncryptedSharedPreferences.create(
            Constants.ENCRYPTED_PREFS_NAME, mk, context,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    val playClick = remember { {
        if (clickPrefs.getBoolean(Constants.PREF_SOUNDS_ENABLED, true)) {
            try {
                val sr = 44100; val n = sr * 15 / 1000
                val s = ShortArray(n)
                for (i in 0 until n) {
                    val env = if (i < 3) i.toDouble() / 3 else (n - i).toDouble() / n
                    s[i] = (Short.MAX_VALUE * 0.7 * env * kotlin.math.sin(2 * Math.PI * 2000.0 * i / sr)).toInt().toShort()
                }
                val t = android.media.AudioTrack(
                    android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    android.media.AudioFormat.Builder().setSampleRate(sr).setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).build(),
                    n * 2, android.media.AudioTrack.MODE_STATIC, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE)
                t.write(s, 0, n); t.play()
            } catch (_: Exception) {}
        }
    } }

    var showSubscriptionSheet by remember { mutableStateOf(false) }

    val consentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            viewModel.syncNow()
        }
    uiState.consentIntent?.let { intent ->
        androidx.compose.runtime.LaunchedEffect(intent) {
            consentLauncher.launch(intent)
            viewModel.clearConsentIntent()
        }
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Einstellungen",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // 1. Konto
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konto", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.userProfile != null) {
                    val profile = uiState.userProfile!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            GoogleLogo(modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    profile.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    profile.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.showLogoutDialog(true) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                        ) {
                            Text("Abmelden")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    uiState.lastSyncTimestamp?.let { ts ->
                        Text(
                            "Letzte Synchronisation: ${DateTimeFormatter.formatFull(ts)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Text(
                        "Eintr\u00e4ge werden bei der Anmeldung automatisch geladen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(
                            onClick = { viewModel.syncNow() },
                            enabled = !uiState.isSyncing,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        ) {
                            Text(
                                if (uiState.isSyncing) "Wird gesichert..."
                                else "Tagebucheintr\u00e4ge sichern"
                            )
                        }
                    }
                    uiState.syncMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    // Not logged in
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoogleLogo(modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Nicht angemeldet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Gesicherte Eintr\u00e4ge werden beim Anmelden geladen",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.signIn(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Text("Mit Google anmelden")
                    }
                }
            }
        }

        // 2. Erscheinungsbild
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Erscheinungsbild", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Dunkelmodus � Sun | Moon icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        SettingsSunMoonIcon(isDark = uiState.isDarkTheme)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Dunkelmodus",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (uiState.isDarkTheme) "Aktiv" else "Aus",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = uiState.isDarkTheme,
                        onCheckedChange = {
                            if (uiState.followSystem) viewModel.updateFollowSystem(false)
                            viewModel.updateDarkTheme(it)
                        },
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // System folgen � Light phone (sun) | divider | Dark phone (moon)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        SettingsPhoneIcon(isDark = uiState.isDarkTheme)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "System folgen",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Automatisch",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = uiState.followSystem,
                        onCheckedChange = { viewModel.updateFollowSystem(it) },
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val locationLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) {
                            try {
                                val lm =
                                    context.getSystemService(
                                        android.content.Context.LOCATION_SERVICE
                                    ) as android.location.LocationManager
                                @Suppress("MissingPermission")
                                val loc =
                                    lm.getLastKnownLocation(
                                        android.location.LocationManager.NETWORK_PROVIDER
                                    )
                                        ?: lm.getLastKnownLocation(
                                            android.location.LocationManager.GPS_PROVIDER
                                        )
                                if (loc != null) {
                                    viewModel.saveLocation(loc.latitude, loc.longitude)
                                    viewModel.updateFollowSun(true)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                // Sonnenauf-/untergang � Sun | Moon based on actual time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        SettingsSunMoonIcon(isDark = uiState.isDarkTheme)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Sonnenauf-/untergang",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Dunkel bei Nacht, hell bei Tag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = uiState.followSun,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val hasPerm =
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    try {
                                        val lm =
                                            context.getSystemService(
                                                android.content.Context.LOCATION_SERVICE
                                            ) as android.location.LocationManager
                                        @Suppress("MissingPermission")
                                        val loc =
                                            lm.getLastKnownLocation(
                                                android.location.LocationManager.NETWORK_PROVIDER
                                            )
                                                ?: lm.getLastKnownLocation(
                                                    android.location.LocationManager.GPS_PROVIDER
                                                )
                                        if (loc != null) {
                                            viewModel.saveLocation(loc.latitude, loc.longitude)
                                        }
                                    } catch (_: Exception) {}
                                    viewModel.updateFollowSun(true)
                                } else {
                                    locationLauncher.launch(
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                }
                            } else {
                                viewModel.updateFollowSun(false)
                            }
                        },
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                    )
                }

            }
        }

        // T�ne
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("T\u00f6ne", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                val soundsPrefs = remember {
                    val masterKey = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
                    androidx.security.crypto.EncryptedSharedPreferences.create(
                        Constants.ENCRYPTED_PREFS_NAME, masterKey, context,
                        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                }
                var soundsEnabled by remember { mutableStateOf(soundsPrefs.getBoolean(Constants.PREF_SOUNDS_ENABLED, true)) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        SettingsSoundIcon(isEnabled = soundsEnabled)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "App-T\u00f6ne",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (soundsEnabled) "T\u00f6ne sind eingeschaltet" else "T\u00f6ne sind ausgeschaltet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = soundsEnabled,
                        onCheckedChange = { enabled ->
                            soundsEnabled = enabled
                            soundsPrefs.edit().putBoolean(Constants.PREF_SOUNDS_ENABLED, enabled).apply()
                            if (enabled) {
                                try {
                                    // Clean click via AudioTrack � single instance, proper release
                                    val sr = 44100; val ms = 30; val n = sr * ms / 1000
                                    val s = ShortArray(n)
                                    for (i in 0 until n) {
                                        val pos = i.toDouble() / n
                                        val env = if (pos < 0.1) pos / 0.1 else kotlin.math.exp(-8.0 * (pos - 0.1))
                                        s[i] = (Short.MAX_VALUE * 0.6 * env * kotlin.math.sin(2 * Math.PI * 600.0 * i / sr)).toInt().toShort()
                                    }
                                    val t = android.media.AudioTrack(
                                        android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                                        android.media.AudioFormat.Builder().setSampleRate(sr).setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).build(),
                                        n * 2, android.media.AudioTrack.MODE_STATIC, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE)
                                    t.write(s, 0, n)
                                    t.setNotificationMarkerPosition(n)
                                    t.setPlaybackPositionUpdateListener(object : android.media.AudioTrack.OnPlaybackPositionUpdateListener {
                                        override fun onMarkerReached(track: android.media.AudioTrack?) { track?.release() }
                                        override fun onPeriodicNotification(track: android.media.AudioTrack?) {}
                                    })
                                    t.play()
                                } catch (_: Exception) {}
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }

        // Sicherheit
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sicherheit", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint =
                                if (uiState.biometricLock) MaterialTheme.colorScheme.primary
                                else Color(0xFF666666),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Fingerabdruck",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "App beim Start entsperren",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = uiState.biometricLock,
                        onCheckedChange = { enabled ->
                            val activity = context as? com.bestjournal.app.MainActivity
                            if (activity != null) {
                                activity.showBiometricPrompt {
                                    viewModel.updateBiometricLock(enabled)
                                }
                            } else {
                                viewModel.updateBiometricLock(enabled)
                            }
                        },
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                    )
                }
                if (uiState.biometricLock) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sperrt automatisch nach 60 Sekunden im Hintergrund",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        // KI Dashboard Profile
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Dashboard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KI Dashboard Profile:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "W\u00e4hle ein Profil aus. Tippe auf ein Profil f\u00fcr eine genauere Erkl\u00e4rung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                val scenarioNames = listOf(
                    "Zusammenfassung",
                    "R\u00e4ume dein Leben auf",
                    "Selbsterkenntnis",
                    "Pers\u00f6nliche Ziele",
                    "Individuelle Analyse"
                )
                val scenarioPrefs = remember {
                    val masterKey = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
                    androidx.security.crypto.EncryptedSharedPreferences.create(
                        Constants.ENCRYPTED_PREFS_NAME,
                        masterKey,
                        context,
                        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                }
                val selectedScenario = scenarioPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                var currentScenario by remember { mutableIntStateOf(selectedScenario) }
                var showCustomPromptDialog by remember { mutableStateOf(false) }
                var showScenarioInfoIndex by remember { mutableIntStateOf(-1) }

                scenarioNames.forEachIndexed { index, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                currentScenario = index
                                scenarioPrefs.edit().putInt(Constants.PREF_DASHBOARD_SCENARIO, index).apply()
                                showScenarioInfoIndex = index
                                if (index == 4) showCustomPromptDialog = true
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentScenario == index,
                            onClick = {
                                currentScenario = index
                                scenarioPrefs.edit().putInt(Constants.PREF_DASHBOARD_SCENARIO, index).apply()
                                showScenarioInfoIndex = index
                                if (index == 4) showCustomPromptDialog = true
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (currentScenario == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            when (index) {
                                0 -> Text("Fasst Themen, Muster und Erlebnisse zusammen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                1 -> Text("Erkennt Stress, Unordnung und Belastung", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                2 -> Text("Deckt verborgene Denk- und Gef\u00fchlsmuster auf", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                3 -> Text("Erkennt Ziele, W\u00fcnsche und Fortschritte", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                4 -> Text("Eigenen Analyse-Fokus festlegen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (showScenarioInfoIndex >= 0) {
                    val infoTitle = scenarioNames[showScenarioInfoIndex]
                    val infoIcon = when (showScenarioInfoIndex) {
                        0 -> Icons.Rounded.Info
                        1 -> Icons.Rounded.Dashboard
                        2 -> Icons.Rounded.Person
                        3 -> Icons.Rounded.Star
                        else -> Icons.Rounded.Tune
                    }
                    val infoText = when (showScenarioInfoIndex) {
                        0 -> "Deine Eintr\u00e4ge werden neutral zusammengefasst, ohne Bewertung oder Ratschl\u00e4ge.\n\nDu siehst auf einen Blick:\n\n\u2022 Welche Themen dich gerade besch\u00e4ftigen\n\u2022 Welche Muster sich wiederholen\n\u2022 Wie sich dein Leben entwickelt\n\nPerfekt als t\u00e4glicher \u00dcberblick \u00fcber alles, was in deinem Leben passiert."
                        1 -> "Die KI sucht gezielt nach Stress, Belastung und Unordnung in deinen Eintr\u00e4gen.\n\nDu bekommst:\n\n\u2022 Eine Analyse deiner gr\u00f6\u00dften Belastungsquellen\n\u2022 5 konkrete Ma\u00dfnahmen zum Aufr\u00e4umen\n\u2022 Tipps, die dir sofort helfen k\u00f6nnen\n\nIdeal wenn du das Gef\u00fchl hast, dass gerade alles zu viel wird."
                        2 -> "Die KI schaut tiefer als nur auf Ereignisse. Sie erkennt in deinen Eintr\u00e4gen:\n\n\u2022 Verborgene Denkmuster und \u00dcberzeugungen\n\u2022 Wiederkehrende Gef\u00fchle und Reaktionen\n\u2022 Pers\u00f6nliche St\u00e4rken, die dir nicht bewusst sind\n\u2022 Werte, die dein Handeln antreiben\n\nF\u00fcr alle, die sich selbst besser verstehen und innerlich wachsen wollen."
                        3 -> "Die KI findet alle Ziele, W\u00fcnsche und Vorhaben in deinen Eintr\u00e4gen, auch beil\u00e4ufig erw\u00e4hnte.\n\nDu siehst:\n\n\u2022 Welche Ziele du hast (auch versteckte)\n\u2022 Wie weit du bei jedem Ziel bist\n\u2022 Was dein n\u00e4chster Schritt sein k\u00f6nnte\n\nDein pers\u00f6nlicher Ziel-Tracker, der aus deinen eigenen Worten liest."
                        else -> "Du bestimmst selbst, worauf die KI achten soll.\n\nSchreibe deinen eigenen Analyse-Fokus, zum Beispiel:\n\n\u2022 \u201eFinde alle Erw\u00e4hnungen von Sport\u201c\n\u2022 \u201eAnalysiere meine Stimmungsschwankungen\u201c\n\u2022 \u201eZeige mir, wann ich am produktivsten bin\u201c\n\nVolle Kontrolle f\u00fcr alle, die genau wissen, was sie suchen."
                    }
                    AlertDialog(
                        onDismissRequest = { showScenarioInfoIndex = -1 },
                        containerColor = MaterialTheme.colorScheme.surface,
                        icon = {
                            Icon(
                                infoIcon, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                        },
                        title = {
                            Text(
                                infoTitle,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        text = {
                            Text(
                                infoText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showScenarioInfoIndex = -1 }) {
                                Text("Verstanden", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                    )
                }

                if (showCustomPromptDialog) {
                    val savedPrompt = scenarioPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                    var promptText by remember { mutableStateOf(savedPrompt) }
                    AlertDialog(
                        onDismissRequest = { showCustomPromptDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text("Individuelle Analyse", style = MaterialTheme.typography.titleLarge)
                        },
                        text = {
                            Column {
                                Text(
                                    "Was ist dir besonders wichtig? Worauf soll sich die KI bei der Analyse deiner Tagebucheintr\u00e4ge konzentrieren?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = promptText,
                                    onValueChange = { promptText = it },
                                    modifier = Modifier.fillMaxWidth().height(280.dp),
                                    placeholder = {
                                        Text(
                                            "z.B. Fokussiere dich auf meine Schlafqualit\u00e4t und Stresslevel. Zeige mir Muster in meiner Ern\u00e4hrung. Analysiere, wie sich meine Stimmung \u00fcber die Woche ver\u00e4ndert. Finde heraus, wann ich am produktivsten bin und was mich blockiert.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                scenarioPrefs.edit().putString(Constants.PREF_CUSTOM_PROMPT, promptText).apply()
                                showCustomPromptDialog = false
                            }) {
                                Text("Speichern", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCustomPromptDialog = false }) {
                                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )
                }
            }
        }

        // KI-Automatisierungen
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KI-Automatisierungen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Textverbesserung",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Standardm\u00e4\u00dfig aktivieren",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.textImprovementDefault,
                        onCheckedChange = { viewModel.updateTextImprovementDefault(it) },
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Dashboard",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Automatisch aktualisieren bei neuem Tagebucheintrag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.autoUpdateDashboard,
                        onCheckedChange = { viewModel.updateAutoUpdateDashboard(it) },
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                    )
                }
            }
        }

        // KI-Abo
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KI-Abo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.isSubscribed) {
                    Text(
                        text = "Premium-Abo aktiv",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Du nutzt Best Journal Premium mit voller KI-Qualit\u00e4t.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val intent =
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(
                                        "https://play.google.com/store/account/subscriptions"
                                    ),
                                )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Abo im Play Store verwalten")
                    }
                } else {
                    Text(
                        text = "Mit Premium bekommst du:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("\u2022  Unbegrenzte KI-Textverbesserung, jeder Eintrag wird klarer und ausdrucksst\u00e4rker", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("\u2022  5 intelligente Dashboard-Profile: Zusammenfassung, R\u00e4ume dein Leben auf, Selbsterkenntnis, Ziele und eigene Analyse", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("\u2022  Automatische Dashboard-Updates bei jedem neuen Eintrag", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("\u2022  Pers\u00f6nliche Muster erkennen, die KI findet verborgene Denk- und Gef\u00fchlsmuster", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("\u2022  Keine Werbung, ungest\u00f6rt schreiben und reflektieren", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSubscriptionSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                    ) {
                        Text("Premium freischalten")
                    }
                }
            }
        }

        // 6. Feedback
        var showFeedbackDialog by remember { mutableStateOf(false) }
        var feedbackSent by remember { mutableStateOf(false) }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Feedback", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Anregungen, W\u00fcnsche, Verbesserungsvorschl\u00e4ge, Bugs melden \uD83D\uDC1E",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = {
                            showFeedbackDialog = true
                            feedbackSent = false
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Icon(Icons.Rounded.Feedback, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Feedback senden")
                    }
                }
                if (feedbackSent) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Senden erfolgreich",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (feedbackSent) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                feedbackSent = false
            }
        }

        if (showFeedbackDialog) {
            FeedbackDialog(
                userEmail = uiState.userProfile?.email,
                onDismiss = { showFeedbackDialog = false },
                onSent = {
                    showFeedbackDialog = false
                    feedbackSent = true
                },
                context = context,
            )
        }

        // 7. Ueber die App
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("\u00dcber die App", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Best Journal v0.5.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Dein pers\u00f6nliches KI-Tagebuch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "\u00a9 Barwandt Digital Labs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Abmelden?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "M\u00f6chtest du dich wirklich abmelden?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.signOut(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                ) {
                    Text("Abmelden")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.showLogoutDialog(false) }) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (showSubscriptionSheet) {
        val activity = context as? android.app.Activity
        com.bestjournal.app.ui.components.AiLimitReachedSheet(
            monthlyPrice = com.bestjournal.app.util.Constants.MONTHLY_PRICE_DISPLAY,
            yearlyPrice = com.bestjournal.app.util.Constants.YEARLY_PRICE_DISPLAY,
            onSubscribeMonthly = {
                showSubscriptionSheet = false
                activity?.let { viewModel.launchSubscription(it, isYearly = false) }
            },
            onSubscribeYearly = {
                showSubscriptionSheet = false
                activity?.let { viewModel.launchSubscription(it, isYearly = true) }
            },
            onDismiss = { showSubscriptionSheet = false },
        )
    }
}

@Composable
private fun GoogleLogo(modifier: Modifier = Modifier) {
    // Google brand colors
    val googleBlue = Color(0xFF4285F4)
    val googleRed = Color(0xFFEA4335)
    val googleYellow = Color(0xFFFBBC05)
    val googleGreen = Color(0xFF34A853)

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = size.minDimension * 0.18f
        val radius = (size.minDimension - strokeW) / 2f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

        // Blue arc (right, top-right) � 315� to 85� (sweep 130�)
        drawArc(
            color = googleBlue,
            startAngle = -45f,
            sweepAngle = 130f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )
        // Green arc (bottom-right) � 85� to 175� (sweep 90�)
        drawArc(
            color = googleGreen,
            startAngle = 85f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )
        // Yellow arc (bottom-left) � 175� to 225� (sweep 50�)
        drawArc(
            color = googleYellow,
            startAngle = 175f,
            sweepAngle = 50f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )
        // Red arc (top-left, top) � 225� to 315� (sweep 90�)
        drawArc(
            color = googleRed,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )

        // Blue horizontal bar (the "crossbar" of the G)
        val barY = center.y
        val barLeft = center.x - strokeW * 0.1f
        val barRight = center.x + radius
        drawLine(
            color = googleBlue,
            start = androidx.compose.ui.geometry.Offset(barLeft, barY),
            end = androidx.compose.ui.geometry.Offset(barRight, barY),
            strokeWidth = strokeW,
        )
    }
}

@Composable
private fun SettingsPhoneIcon(isDark: Boolean) {
    val glowYellow = Color(0xFFFFD54F)
    val mutedGray = Color(0xFF666666)
    val lightPhoneSize by
        animateDpAsState(
            targetValue = if (!isDark) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "lightPhoneSize",
        )
    val darkPhoneSize by
        animateDpAsState(
            targetValue = if (isDark) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "darkPhoneSize",
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Light phone with mini sun
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Rounded.PhoneAndroid,
                "Hell",
                tint = if (!isDark) glowYellow else mutedGray,
                modifier = Modifier.size(lightPhoneSize),
            )
            Icon(
                Icons.Rounded.LightMode,
                null,
                tint = if (!isDark) glowYellow else mutedGray,
                modifier = Modifier.size(lightPhoneSize * 0.35f),
            )
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.height(16.dp).width(1.dp),
        )
        // Dark phone with mini moon
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Rounded.PhoneAndroid,
                "Dunkel",
                tint = if (isDark) glowYellow else mutedGray,
                modifier = Modifier.size(darkPhoneSize),
            )
            Icon(
                Icons.Rounded.DarkMode,
                null,
                tint = if (isDark) glowYellow else mutedGray,
                modifier = Modifier.size(darkPhoneSize * 0.35f),
            )
        }
    }
}

@Composable
private fun SettingsSoundIcon(isEnabled: Boolean) {
    val activeColor = Color(0xFF4CAF50)
    val mutedGray = Color(0xFF666666)
    val onSize by animateDpAsState(
        targetValue = if (isEnabled) 22.dp else 14.dp,
        animationSpec = tween(300), label = "soundOnSize"
    )
    val offSize by animateDpAsState(
        targetValue = if (!isEnabled) 22.dp else 14.dp,
        animationSpec = tween(300), label = "soundOffSize"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Rounded.VolumeUp, "Ton an", tint = if (isEnabled) activeColor else mutedGray, modifier = Modifier.size(onSize))
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.height(16.dp).width(1.dp))
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Rounded.VolumeOff, "Ton aus", tint = if (!isEnabled) Color(0xFFEF4444) else mutedGray, modifier = Modifier.size(offSize))
        }
    }
}

@Composable
private fun SettingsSunMoonIcon(isDark: Boolean) {
    val glowYellow = Color(0xFFFFD54F)
    val mutedGray = Color(0xFF666666)
    val sunSize by
        animateDpAsState(
            targetValue = if (!isDark) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "settingSunSize",
        )
    val moonSize by
        animateDpAsState(
            targetValue = if (isDark) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "settingMoonSize",
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Rounded.LightMode,
                "Sonne",
                tint = if (!isDark) glowYellow else mutedGray,
                modifier = Modifier.size(sunSize),
            )
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.height(16.dp).width(1.dp),
        )
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Rounded.DarkMode,
                "Mond",
                tint = if (isDark) glowYellow else mutedGray,
                modifier = Modifier.size(moonSize),
            )
        }
    }
}

@Composable
private fun FeedbackDialog(
    userEmail: String?,
    onDismiss: () -> Unit,
    onSent: () -> Unit,
    context: android.content.Context,
) {
    var feedbackText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                Icons.Rounded.Email, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        },
        title = {
            Text(
                "Feedback senden",
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column {
                Text(
                    "Deine Nachricht an die Entwickler \u2014 wir lesen alles und antworten pers\u00f6nlich!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    placeholder = {
                        Text(
                            "Schreib uns dein Feedback...",
                            color = MaterialTheme.colorScheme.outline,
                        )
                    },
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                    shape = RoundedCornerShape(12.dp),
                )
                if (isSending) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Wird gesendet...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = NeonRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (feedbackText.isNotBlank() && !isSending) {
                        if (userEmail == null) {
                            errorMessage = "Bitte zuerst mit Google anmelden"
                            return@Button
                        }
                        isSending = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val error =
                                    com.bestjournal.app.data.remote.FeedbackSender.send(
                                        context = context,
                                        accountEmail = userEmail,
                                        feedbackText = feedbackText,
                                    )
                                if (error == null) {
                                    onSent()
                                } else {
                                    isSending = false
                                    errorMessage = error
                                }
                            } catch (
                                e: com.bestjournal.app.data.remote.FeedbackNeedConsentException) {
                                // Gmail permission needed � show consent screen
                                isSending = false
                                try {
                                    context.startActivity(e.consentIntent)
                                } catch (_: Exception) {}
                                errorMessage = "Bitte Gmail-Zugriff erlauben und erneut versuchen."
                            }
                        }
                    }
                },
                enabled = feedbackText.isNotBlank() && !isSending,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text("Senden")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { if (!isSending) onDismiss() }) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
