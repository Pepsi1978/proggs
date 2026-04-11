package com.entropyjournal.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.theme.LocalIsDarkTheme
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.util.Constants
import com.entropyjournal.util.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignOut: () -> Unit,
    onProfileChanged: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val clickPrefs = remember {
        val mk =
            androidx.security.crypto.MasterKeys.getOrCreate(
                androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
            )
        androidx.security.crypto.EncryptedSharedPreferences.create(
            Constants.ENCRYPTED_PREFS_NAME,
            mk,
            context,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    val playClick = remember {
        {
            if (clickPrefs.getBoolean(Constants.PREF_SOUNDS_ENABLED, true)) {
                try {
                    val sr = 44100
                    val n = sr * 15 / 1000
                    val s = ShortArray(n)
                    for (i in 0 until n) {
                        val env = if (i < 3) i.toDouble() / 3 else (n - i).toDouble() / n
                        s[i] =
                            (Short.MAX_VALUE *
                                    0.7 *
                                    env *
                                    kotlin.math.sin(2 * Math.PI * 2000.0 * i / sr))
                                .toInt()
                                .toShort()
                    }
                    val t =
                        android.media.AudioTrack(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(
                                    android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
                                )
                                .build(),
                            android.media.AudioFormat.Builder()
                                .setSampleRate(sr)
                                .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                            n * 2,
                            android.media.AudioTrack.MODE_STATIC,
                            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
                        )
                    t.write(s, 0, n)
                    t.play()
                } catch (_: Exception) {}
            }
        }
    }

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

    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingPermissionAction?.invoke()
            }
            pendingPermissionAction = null
        }

    val pdfLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null) {
                viewModel.exportToPdf(context, uri)
            }
        }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            // Fixed title bar (does not scroll)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Einstellungen",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // Scrollable content
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // 1. Konto
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Person,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Konto",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.PhotoCamera,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Fotos mitsichern",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Switch(
                                    checked = uiState.backupPhotos,
                                    onCheckedChange = { viewModel.setBackupPhotos(it) },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Videocam,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Videos mitsichern",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Switch(
                                    checked = uiState.backupVideos,
                                    onCheckedChange = { viewModel.setBackupVideos(it) },
                                )
                            }
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Button(
                                    onClick = { viewModel.signIn(context) },
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
                }

                // 2. Erscheinungsbild
                GlassCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Palette,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Erscheinungsbild",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
                        Spacer(modifier = Modifier.height(10.dp))

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
                        Spacer(modifier = Modifier.height(10.dp))
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
                                            ) ==
                                                android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            try {
                                                val lm =
                                                    context.getSystemService(
                                                        android.content.Context.LOCATION_SERVICE
                                                    ) as android.location.LocationManager
                                                @Suppress("MissingPermission")
                                                val loc =
                                                    lm.getLastKnownLocation(
                                                        android.location.LocationManager
                                                            .NETWORK_PROVIDER
                                                    )
                                                        ?: lm.getLastKnownLocation(
                                                            android.location.LocationManager
                                                                .GPS_PROVIDER
                                                        )
                                                if (loc != null) {
                                                    viewModel.saveLocation(
                                                        loc.latitude,
                                                        loc.longitude,
                                                    )
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
                            Icon(
                                Icons.Rounded.MusicNote,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "T\u00f6ne",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val soundsPrefs = remember {
                            val masterKey =
                                androidx.security.crypto.MasterKeys.getOrCreate(
                                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                                )
                            androidx.security.crypto.EncryptedSharedPreferences.create(
                                Constants.ENCRYPTED_PREFS_NAME,
                                masterKey,
                                context,
                                androidx.security.crypto.EncryptedSharedPreferences
                                    .PrefKeyEncryptionScheme
                                    .AES256_SIV,
                                androidx.security.crypto.EncryptedSharedPreferences
                                    .PrefValueEncryptionScheme
                                    .AES256_GCM,
                            )
                        }
                        var soundsEnabled by remember {
                            mutableStateOf(
                                soundsPrefs.getBoolean(Constants.PREF_SOUNDS_ENABLED, true)
                            )
                        }

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
                                        if (soundsEnabled) "T\u00f6ne sind eingeschaltet"
                                        else "T\u00f6ne sind ausgeschaltet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = soundsEnabled,
                                onCheckedChange = { enabled ->
                                    soundsEnabled = enabled
                                    soundsPrefs
                                        .edit()
                                        .putBoolean(Constants.PREF_SOUNDS_ENABLED, enabled)
                                        .apply()
                                    if (enabled) {
                                        try {
                                            // Clean click via AudioTrack � single instance, proper
                                            // release
                                            val sr = 44100
                                            val ms = 30
                                            val n = sr * ms / 1000
                                            val s = ShortArray(n)
                                            for (i in 0 until n) {
                                                val pos = i.toDouble() / n
                                                val env =
                                                    if (pos < 0.1) pos / 0.1
                                                    else kotlin.math.exp(-8.0 * (pos - 0.1))
                                                s[i] =
                                                    (Short.MAX_VALUE *
                                                            0.6 *
                                                            env *
                                                            kotlin.math.sin(
                                                                2 * Math.PI * 600.0 * i / sr
                                                            ))
                                                        .toInt()
                                                        .toShort()
                                            }
                                            val t =
                                                android.media.AudioTrack(
                                                    android.media.AudioAttributes.Builder()
                                                        .setUsage(
                                                            android.media.AudioAttributes
                                                                .USAGE_ASSISTANCE_SONIFICATION
                                                        )
                                                        .setContentType(
                                                            android.media.AudioAttributes
                                                                .CONTENT_TYPE_SONIFICATION
                                                        )
                                                        .build(),
                                                    android.media.AudioFormat.Builder()
                                                        .setSampleRate(sr)
                                                        .setEncoding(
                                                            android.media.AudioFormat
                                                                .ENCODING_PCM_16BIT
                                                        )
                                                        .setChannelMask(
                                                            android.media.AudioFormat
                                                                .CHANNEL_OUT_MONO
                                                        )
                                                        .build(),
                                                    n * 2,
                                                    android.media.AudioTrack.MODE_STATIC,
                                                    android.media.AudioManager
                                                        .AUDIO_SESSION_ID_GENERATE,
                                                )
                                            t.write(s, 0, n)
                                            t.setNotificationMarkerPosition(n)
                                            t.setPlaybackPositionUpdateListener(
                                                object :
                                                    android.media.AudioTrack.OnPlaybackPositionUpdateListener {
                                                    override fun onMarkerReached(
                                                        track: android.media.AudioTrack?
                                                    ) {
                                                        track?.release()
                                                    }

                                                    override fun onPeriodicNotification(
                                                        track: android.media.AudioTrack?
                                                    ) {}
                                                }
                                            )
                                            t.play()
                                        } catch (_: Exception) {}
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

                // Erinnerung / Rückblick
                GlassCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Notifications,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Erinnerung / R\u00fcckblick",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Daily reminder
                        var showTimePicker by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "T\u00e4gliche Erinnerung",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (uiState.reminderEnabled) {
                                    Text(
                                        "Uhrzeit: %02d:%02d Uhr"
                                            .format(uiState.reminderHour, uiState.reminderMinute),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Text(
                                        "Erinnert dich ans Tagebuchschreiben",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.reminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (
                                            android.os.Build.VERSION.SDK_INT >=
                                                android.os.Build.VERSION_CODES.TIRAMISU
                                        ) {
                                            val hasPermission =
                                                androidx.core.content.ContextCompat
                                                    .checkSelfPermission(
                                                        context,
                                                        android.Manifest.permission
                                                            .POST_NOTIFICATIONS,
                                                    ) ==
                                                    android.content.pm.PackageManager
                                                        .PERMISSION_GRANTED
                                            if (!hasPermission) {
                                                notificationPermissionLauncher.launch(
                                                    android.Manifest.permission.POST_NOTIFICATIONS
                                                )
                                                return@Switch
                                            }
                                        }
                                        viewModel.updateReminderEnabled(true)
                                        showTimePicker = true
                                    } else {
                                        viewModel.updateReminderEnabled(false)
                                    }
                                },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                            )
                        }

                        if (showTimePicker) {
                            ReminderTimePickerDialog(
                                initialHour = uiState.reminderHour,
                                initialMinute = uiState.reminderMinute,
                                onConfirm = { h, m ->
                                    viewModel.updateReminderTime(h, m)
                                    showTimePicker = false
                                },
                                onDismiss = { showTimePicker = false },
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Wöchentlicher Rückblick (Sonntag 15:00 Uhr lokal)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Wöchentlicher Rückblick",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (uiState.weeklyReviewEnabled) "Erinnerung an"
                                    else "Sonntags um 15:00 Uhr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (uiState.weeklyReviewEnabled)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.weeklyReviewEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (
                                            android.os.Build.VERSION.SDK_INT >=
                                                android.os.Build.VERSION_CODES.TIRAMISU
                                        ) {
                                            val hasPermission =
                                                androidx.core.content.ContextCompat
                                                    .checkSelfPermission(
                                                        context,
                                                        android.Manifest.permission
                                                            .POST_NOTIFICATIONS,
                                                    ) ==
                                                    android.content.pm.PackageManager
                                                        .PERMISSION_GRANTED
                                            if (!hasPermission) {
                                                pendingPermissionAction = {
                                                    viewModel.updateWeeklyReviewEnabled(true)
                                                }
                                                notificationPermissionLauncher.launch(
                                                    android.Manifest.permission.POST_NOTIFICATIONS
                                                )
                                                return@Switch
                                            }
                                        }
                                        viewModel.updateWeeklyReviewEnabled(true)
                                    } else {
                                        viewModel.updateWeeklyReviewEnabled(false)
                                    }
                                },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Monatsrückblick (letzter Tag des Monats 15:00 Uhr lokal)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Monatsrückblick",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (uiState.monthlyReviewEnabled) "Erinnerung an"
                                    else "Am letzten Tag des Monats um 15:00 Uhr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (uiState.monthlyReviewEnabled)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.monthlyReviewEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (
                                            android.os.Build.VERSION.SDK_INT >=
                                                android.os.Build.VERSION_CODES.TIRAMISU
                                        ) {
                                            val hasPermission =
                                                androidx.core.content.ContextCompat
                                                    .checkSelfPermission(
                                                        context,
                                                        android.Manifest.permission
                                                            .POST_NOTIFICATIONS,
                                                    ) ==
                                                    android.content.pm.PackageManager
                                                        .PERMISSION_GRANTED
                                            if (!hasPermission) {
                                                pendingPermissionAction = {
                                                    viewModel.updateMonthlyReviewEnabled(true)
                                                }
                                                notificationPermissionLauncher.launch(
                                                    android.Manifest.permission.POST_NOTIFICATIONS
                                                )
                                                return@Switch
                                            }
                                        }
                                        viewModel.updateMonthlyReviewEnabled(true)
                                    } else {
                                        viewModel.updateMonthlyReviewEnabled(false)
                                    }
                                },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Jahresrückblick (31. Dezember 23:00 Uhr lokal)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Jahresrückblick",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (uiState.yearlyReviewEnabled) "Erinnerung an"
                                    else "Am letzten Tag des Jahres um 15:00 Uhr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (uiState.yearlyReviewEnabled)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.yearlyReviewEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (
                                            android.os.Build.VERSION.SDK_INT >=
                                                android.os.Build.VERSION_CODES.TIRAMISU
                                        ) {
                                            val hasPermission =
                                                androidx.core.content.ContextCompat
                                                    .checkSelfPermission(
                                                        context,
                                                        android.Manifest.permission
                                                            .POST_NOTIFICATIONS,
                                                    ) ==
                                                    android.content.pm.PackageManager
                                                        .PERMISSION_GRANTED
                                            if (!hasPermission) {
                                                pendingPermissionAction = {
                                                    viewModel.updateYearlyReviewEnabled(true)
                                                }
                                                notificationPermissionLauncher.launch(
                                                    android.Manifest.permission.POST_NOTIFICATIONS
                                                )
                                                return@Switch
                                            }
                                        }
                                        viewModel.updateYearlyReviewEnabled(true)
                                    } else {
                                        viewModel.updateYearlyReviewEnabled(false)
                                    }
                                },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                            )
                        }

                        // Zeitzone immer aktuell halten (ändert sich bei Reisen)
                        val currentTimezone = java.util.TimeZone.getDefault().id
                        androidx.compose.runtime.LaunchedEffect(currentTimezone) {
                            if (uiState.userTimezone != currentTimezone) {
                                viewModel.setUserTimezone(currentTimezone)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Zeitzone: $currentTimezone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Daten
                GlassCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Description,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Daten",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                onClick = { pdfLauncher.launch("EntropyJournal_Export.pdf") },
                                enabled = !uiState.isExporting,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                            ) {
                                Icon(
                                    Icons.Rounded.Description,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (uiState.isExporting) "Wird exportiert..."
                                    else "Tagebucheinträge als PDF exportieren"
                                )
                            }
                        }
                        uiState.exportMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                msg,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Sicherheit
                GlassCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Security,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sicherheit",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
                                    val activity = context as? com.entropyjournal.MainActivity
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Sperrt automatisch nach 60 Sekunden im Hintergrund",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                // KI-Dashboard Profile
                GlassCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Dashboard,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "KI-Dashboard Profile:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "W\u00e4hle ein Profil aus. Tippe auf ein Profil f\u00fcr eine genauere Erkl\u00e4rung.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val scenarioNames =
                            listOf(
                                "Zusammenfassung",
                                "R\u00e4ume dein Leben auf",
                                "Selbsterkenntnis",
                                "Pers\u00f6nliche Ziele",
                                "Individuelle Analyse",
                            )
                        val scenarioPrefs = remember {
                            val masterKey =
                                androidx.security.crypto.MasterKeys.getOrCreate(
                                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                                )
                            androidx.security.crypto.EncryptedSharedPreferences.create(
                                Constants.ENCRYPTED_PREFS_NAME,
                                masterKey,
                                context,
                                androidx.security.crypto.EncryptedSharedPreferences
                                    .PrefKeyEncryptionScheme
                                    .AES256_SIV,
                                androidx.security.crypto.EncryptedSharedPreferences
                                    .PrefValueEncryptionScheme
                                    .AES256_GCM,
                            )
                        }
                        val selectedScenario =
                            scenarioPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                        var currentScenario by remember { mutableIntStateOf(selectedScenario) }
                        var showCustomPromptDialog by remember { mutableStateOf(false) }
                        var showScenarioInfoIndex by remember { mutableIntStateOf(-1) }

                        scenarioNames.forEachIndexed { index, name ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            currentScenario = index
                                            scenarioPrefs
                                                .edit()
                                                .putInt(Constants.PREF_DASHBOARD_SCENARIO, index)
                                                .apply()
                                            showScenarioInfoIndex = index
                                            if (index == 4) showCustomPromptDialog = true
                                            onProfileChanged()
                                        }
                                        .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = currentScenario == index,
                                    onClick = {
                                        currentScenario = index
                                        scenarioPrefs
                                            .edit()
                                            .putInt(Constants.PREF_DASHBOARD_SCENARIO, index)
                                            .apply()
                                        showScenarioInfoIndex = index
                                        if (index == 4) showCustomPromptDialog = true
                                        onProfileChanged()
                                    },
                                    colors =
                                        RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        ),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color =
                                            if (currentScenario == index)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                    )
                                    when (index) {
                                        0 ->
                                            Text(
                                                "Fasst Themen, Muster und Erlebnisse zusammen",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        1 ->
                                            Text(
                                                "Erkennt Stress, Unordnung und Belastung",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        2 ->
                                            Text(
                                                "Deckt verborgene Denk- und Gef\u00fchlsmuster auf",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        3 ->
                                            Text(
                                                "Erkennt Ziele, W\u00fcnsche und Fortschritte",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        4 ->
                                            Text(
                                                "Eigenen Analyse-Fokus festlegen",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                    }
                                }
                            }
                        }

                        if (showScenarioInfoIndex >= 0) {
                            val infoTitle = scenarioNames[showScenarioInfoIndex]
                            val infoIcon =
                                when (showScenarioInfoIndex) {
                                    0 -> Icons.Rounded.Info
                                    1 -> Icons.Rounded.Dashboard
                                    2 -> Icons.Rounded.Person
                                    3 -> Icons.Rounded.Fingerprint
                                    else -> Icons.Rounded.Tune
                                }
                            val infoText =
                                when (showScenarioInfoIndex) {
                                    0 ->
                                        "Deine Eintr\u00e4ge werden neutral zusammengefasst, ohne Bewertung oder Ratschl\u00e4ge.\n\nDu siehst auf einen Blick:\n\n\u2022 Welche Themen dich gerade besch\u00e4ftigen\n\u2022 Welche Muster sich wiederholen\n\u2022 Wie sich dein Leben entwickelt\n\nPerfekt als t\u00e4glicher \u00dcberblick \u00fcber alles, was in deinem Leben passiert."
                                    1 ->
                                        "Die KI sucht gezielt nach Stress, Belastung und Unordnung in deinen Eintr\u00e4gen.\n\nDu bekommst:\n\n\u2022 Eine Analyse deiner gr\u00f6\u00dften Belastungsquellen\n\u2022 5 konkrete Ma\u00dfnahmen zum Aufr\u00e4umen\n\u2022 Tipps, die dir sofort helfen k\u00f6nnen\n\nIdeal wenn du das Gef\u00fchl hast, dass gerade alles zu viel wird."
                                    2 ->
                                        "Die KI schaut tiefer als nur auf Ereignisse. Sie erkennt in deinen Eintr\u00e4gen:\n\n\u2022 Verborgene Denkmuster und \u00dcberzeugungen\n\u2022 Wiederkehrende Gef\u00fchle und Reaktionen\n\u2022 Pers\u00f6nliche St\u00e4rken, die dir nicht bewusst sind\n\u2022 Werte, die dein Handeln antreiben\n\nF\u00fcr alle, die sich selbst besser verstehen und innerlich wachsen wollen."
                                    3 ->
                                        "Die KI findet alle Ziele, W\u00fcnsche und Vorhaben in deinen Eintr\u00e4gen, auch beil\u00e4ufig erw\u00e4hnte.\n\nDu siehst:\n\n\u2022 Welche Ziele du hast (auch versteckte)\n\u2022 Wie weit du bei jedem Ziel bist\n\u2022 Was dein n\u00e4chster Schritt sein k\u00f6nnte\n\nDein pers\u00f6nlicher Ziel-Tracker, der aus deinen eigenen Worten liest."
                                    else ->
                                        "Du bestimmst selbst, worauf die KI achten soll.\n\nSchreibe deinen eigenen Analyse-Fokus, zum Beispiel:\n\n\u2022 \u201eFinde alle Erw\u00e4hnungen von Sport\u201c\n\u2022 \u201eAnalysiere meine Stimmungsschwankungen\u201c\n\u2022 \u201eZeige mir, wann ich am produktivsten bin\u201c\n\nVolle Kontrolle f\u00fcr alle, die genau wissen, was sie suchen."
                                }
                            AlertDialog(
                                onDismissRequest = { showScenarioInfoIndex = -1 },
                                containerColor = MaterialTheme.colorScheme.surface,
                                icon = {
                                    Icon(
                                        infoIcon,
                                        null,
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
                                        Text(
                                            "Verstanden",
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            )
                        }

                        if (showCustomPromptDialog) {
                            val savedPrompt =
                                scenarioPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                            var promptText by remember { mutableStateOf(savedPrompt) }
                            AlertDialog(
                                onDismissRequest = { showCustomPromptDialog = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                title = {
                                    Text(
                                        "Individuelle Analyse",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
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
                                            modifier = Modifier.fillMaxWidth().height(350.dp),
                                            placeholder = {
                                                val isDark = LocalIsDarkTheme.current
                                                Text(
                                                    "z.B. Fokussiere dich auf meine Schlafqualit\u00e4t und Stresslevel. Zeige mir Muster in meiner Ern\u00e4hrung. Analysiere, wie sich meine Stimmung \u00fcber die Woche ver\u00e4ndert. Finde heraus, wann ich am produktivsten bin und was mich blockiert.\n\nJe gr\u00fcndlicher du beschreibst was dein Fokus ist, desto besser werden die Ergebnisse.",
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                            .copy(
                                                                alpha = if (isDark) 0.25f else 0.35f
                                                            ),
                                                )
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val previousPrompt =
                                                scenarioPrefs.getString(
                                                    Constants.PREF_CUSTOM_PROMPT,
                                                    "",
                                                ) ?: ""
                                            val editor =
                                                scenarioPrefs
                                                    .edit()
                                                    .putString(
                                                        Constants.PREF_CUSTOM_PROMPT,
                                                        promptText,
                                                    )
                                            if (promptText != previousPrompt) {
                                                editor.putLong(
                                                    "custom_prompt_saved_at",
                                                    System.currentTimeMillis(),
                                                )
                                            }
                                            editor.apply()
                                            showCustomPromptDialog = false
                                        }
                                    ) {
                                        Text("Speichern", color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCustomPromptDialog = false }) {
                                        Text(
                                            "Abbrechen",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
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
                            Icon(
                                Icons.Rounded.Tune,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "KI-Automatisierungen",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
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
                        Spacer(modifier = Modifier.height(10.dp))
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
                                    "Automatisch aktualisieren",
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

                // API-Schluessel
                GlassCard {
                    Column {
                        Text(
                            "API-Schl\u00fcssel",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ApiKeyField(
                            label = "Groq API-Key",
                            value = uiState.groqApiKey,
                            onValueChange = { viewModel.updateGroqApiKey(it) },
                            requireBiometric = uiState.biometricLock,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ApiKeyField(
                            label = "Gemini API-Key",
                            value = uiState.geminiApiKey,
                            onValueChange = { viewModel.updateGeminiApiKey(it) },
                            requireBiometric = uiState.biometricLock,
                        )
                    }
                }

                // Gemini-Modell
                GlassCard {
                    Column {
                        Text(
                            "Gemini-Modell",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "F\u00fcr Textverbesserung und Dashboard-Analyse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        var expanded by remember { mutableStateOf(false) }
                        val selectedModel =
                            Constants.GEMINI_FLASH_MODELS.find { it.id == uiState.selectedModel }
                                ?: Constants.GEMINI_FLASH_MODELS.first()
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        ) {
                            TextField(
                                value = "${selectedModel.displayName}   ${selectedModel.price}",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    Icon(Icons.Rounded.KeyboardArrowDown, "Modell w\u00e4hlen")
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors =
                                    TextFieldDefaults.colors(
                                        focusedContainerColor =
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor =
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                Constants.GEMINI_FLASH_MODELS.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    model.displayName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color =
                                                        if (model.id == uiState.selectedModel)
                                                            MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    "Input ${model.price.substringBefore("/")} \u2022 Output ${model.price.substringAfter("/")}  pro 1M Token",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.updateSelectedModel(model.id)
                                            expanded = false
                                        },
                                    )
                                }
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
                            Icon(
                                Icons.Rounded.Email,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Feedback",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
                        Text(
                            "\u00dcber die App",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Entropy Journal v0.5.0",
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
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    requireBiometric: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MaterialTheme.colorScheme.outline) },
        visualTransformation =
            if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(
                onClick = {
                    if (visible) {
                        // Always allow hiding
                        visible = false
                    } else if (requireBiometric) {
                        // Require biometric to reveal
                        val activity = context as? com.entropyjournal.MainActivity
                        if (activity != null) {
                            activity.showBiometricPrompt { visible = true }
                        } else {
                            visible = true
                        }
                    } else {
                        visible = true
                    }
                }
            ) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    if (visible) "Verbergen" else "Anzeigen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
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
    val onSize by
        animateDpAsState(
            targetValue = if (isEnabled) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "soundOnSize",
        )
    val offSize by
        animateDpAsState(
            targetValue = if (!isEnabled) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "soundOffSize",
        )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Rounded.VolumeUp,
                "Ton an",
                tint = if (isEnabled) activeColor else mutedGray,
                modifier = Modifier.size(onSize),
            )
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.height(16.dp).width(1.dp),
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Rounded.VolumeOff,
                "Ton aus",
                tint = if (!isEnabled) Color(0xFFEF4444) else mutedGray,
                modifier = Modifier.size(offSize),
            )
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
                Icons.Rounded.Email,
                null,
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
                                    com.entropyjournal.data.remote.FeedbackSender.send(
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
                                e: com.entropyjournal.data.remote.FeedbackNeedConsentException) {
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

private fun weekDayName(calendarDay: Int): String =
    when (calendarDay) {
        java.util.Calendar.MONDAY -> "Montag"
        java.util.Calendar.TUESDAY -> "Dienstag"
        java.util.Calendar.WEDNESDAY -> "Mittwoch"
        java.util.Calendar.THURSDAY -> "Donnerstag"
        java.util.Calendar.FRIDAY -> "Freitag"
        java.util.Calendar.SATURDAY -> "Samstag"
        java.util.Calendar.SUNDAY -> "Sonntag"
        else -> "Sonntag"
    }

private val weekDays =
    listOf(
        java.util.Calendar.MONDAY,
        java.util.Calendar.TUESDAY,
        java.util.Calendar.WEDNESDAY,
        java.util.Calendar.THURSDAY,
        java.util.Calendar.FRIDAY,
        java.util.Calendar.SATURDAY,
        java.util.Calendar.SUNDAY,
    )
private val weekDayLabels = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState =
        androidx.compose.material3.rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.Notifications,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Wann m\u00f6chtest du erinnert werden?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.TimeInput(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                colors =
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyReviewPickerDialog(
    initialDay: Int,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedDayIndex by remember {
        mutableIntStateOf(weekDays.indexOf(initialDay).coerceAtLeast(0))
    }
    val timePickerState =
        androidx.compose.material3.rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.DateRange,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Wann soll dein R\u00fcckblick kommen?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Wochentag",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    weekDayLabels.forEachIndexed { index, label ->
                        val isSelected = index == selectedDayIndex
                        androidx.compose.material3.Surface(
                            onClick = { selectedDayIndex = index },
                            shape = RoundedCornerShape(12.dp),
                            color =
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color =
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Uhrzeit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.material3.TimeInput(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        weekDays[selectedDayIndex],
                        timePickerState.hour,
                        timePickerState.minute,
                    )
                },
                colors =
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
