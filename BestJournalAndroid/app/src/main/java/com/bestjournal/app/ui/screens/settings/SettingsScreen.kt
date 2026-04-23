package com.bestjournal.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimeInput
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestjournal.app.R
import com.bestjournal.app.ui.components.AnimatedMicButton
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.components.ParticleBackground
import com.bestjournal.app.ui.components.TwinklingStars
import com.bestjournal.app.ui.theme.FeatureAccentOrange
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.DateTimeFormatter
import com.bestjournal.app.util.rememberHapticAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignOut: () -> Unit,
    onNavigateToPaywall: (String) -> Unit = {},
    onProfileChanged: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Click sound helper � plays only when sounds are enabled
    val clickPrefs = remember { com.bestjournal.app.util.EncryptedPrefsProvider.get(context) }
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
                                    1.0 *
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
                    // Release AudioTrack automatically after playback ends
                    t.setNotificationMarkerPosition(n)
                    t.setPlaybackPositionUpdateListener(
                        object : android.media.AudioTrack.OnPlaybackPositionUpdateListener {
                            override fun onMarkerReached(track: android.media.AudioTrack) {
                                track.release()
                            }

                            override fun onPeriodicNotification(track: android.media.AudioTrack) {}
                        }
                    )
                    t.play()
                } catch (e: Exception) {
                    android.util.Log.w("SettingsScreen", "playClick failed: ${e.message}")
                }
            }
        }
    }

    val doHaptic = rememberHapticAction()

    var showSubscriptionSheet by remember { mutableStateOf(false) }

    val consentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                viewModel.syncNow()
            } else {
                android.util.Log.d(
                    "SettingsScreen",
                    "Drive consent cancelled or failed, not retrying",
                )
            }
        }
    uiState.consentIntent?.let { intent ->
        androidx.compose.runtime.LaunchedEffect(intent) {
            consentLauncher.launch(intent)
            viewModel.clearConsentIntent()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (LocalIsDarkTheme.current) {
            ParticleBackground()
            TwinklingStars()
        }
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            // Fixed title bar (does not scroll)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.settings_title),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_account),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (uiState.userProfile != null) {
                            val profile = uiState.userProfile ?: return@Column
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
                                    onClick = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        viewModel.showLogoutDialog(true)
                                    },
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                                ) {
                                    Text(stringResource(R.string.settings_sign_out))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            uiState.lastSyncTimestamp?.let { ts ->
                                Text(
                                    stringResource(
                                        R.string.settings_last_sync,
                                        DateTimeFormatter.formatFull(ts),
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Text(
                                stringResource(R.string.settings_entries_auto_loaded),
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.settings_backup_photos),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Switch(
                                    checked = uiState.backupPhotos,
                                    onCheckedChange = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        viewModel.setBackupPhotos(it)
                                    },
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.settings_backup_videos),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Switch(
                                    checked = uiState.backupVideos,
                                    onCheckedChange = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        viewModel.setBackupVideos(it)
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Button(
                                    onClick = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        viewModel.syncNow()
                                    },
                                    enabled = !uiState.isSyncing,
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                ) {
                                    Text(
                                        if (uiState.isSyncing)
                                            stringResource(R.string.settings_syncing)
                                        else stringResource(R.string.settings_backup_entries)
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
                                        stringResource(R.string.settings_not_signed_in),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        stringResource(R.string.settings_entries_loaded_on_sign_in),
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
                                    onClick = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        viewModel.signIn(context)
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                ) {
                                    Text(stringResource(R.string.settings_sign_in_google))
                                }
                            }
                        }
                    }
                }

                // 2. Erscheinungsbild
                GlassCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Palette,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_appearance),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
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
                                SettingsSunMoonIcon(
                                    isDark = com.bestjournal.app.ui.theme.LocalIsDarkTheme.current,
                                    isActive = !uiState.followSystem && !uiState.followSun,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.settings_dark_mode),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        if (uiState.isDarkTheme)
                                            stringResource(R.string.settings_dark_active)
                                        else stringResource(R.string.settings_dark_off),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isDarkTheme,
                                onCheckedChange = {
                                    doHaptic(HapticFeedbackType.LongPress)
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
                                SettingsPhoneIcon(
                                    isDark = com.bestjournal.app.ui.theme.LocalIsDarkTheme.current,
                                    isActive = uiState.followSystem,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.settings_follow_system),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        stringResource(R.string.settings_automatic),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.followSystem,
                                onCheckedChange = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.updateFollowSystem(it)
                                },
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
                                    } catch (e: Exception) {
                                        android.util.Log.w(
                                            "SettingsScreen",
                                            "Failed to save location for Follow Sun",
                                            e,
                                        )
                                    }
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
                                SettingsSunMoonIcon(
                                    isDark = com.bestjournal.app.ui.theme.LocalIsDarkTheme.current,
                                    isActive = uiState.followSun,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.settings_sunrise_sunset),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        stringResource(R.string.settings_sunrise_sunset_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.followSun,
                                onCheckedChange = { enabled ->
                                    doHaptic(HapticFeedbackType.LongPress)
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

                // Töne / Haptik
                GlassCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_sounds_haptics_section),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val soundsPrefs = remember {
                            com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
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
                                        stringResource(R.string.settings_app_sounds),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        if (soundsEnabled)
                                            stringResource(R.string.settings_sounds_on)
                                        else stringResource(R.string.settings_sounds_off),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = soundsEnabled,
                                onCheckedChange = { enabled ->
                                    doHaptic(HapticFeedbackType.LongPress)
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
                                                            1.0 *
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

                        Spacer(modifier = Modifier.height(16.dp))

                        var hapticEnabled by remember {
                            mutableStateOf(
                                soundsPrefs.getBoolean(Constants.PREF_HAPTIC_ENABLED, true)
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
                                SettingsHapticIcon(isEnabled = hapticEnabled)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.settings_haptics),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        if (hapticEnabled)
                                            stringResource(R.string.settings_haptics_on)
                                        else stringResource(R.string.settings_haptics_off),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = hapticEnabled,
                                onCheckedChange = { enabled ->
                                    hapticEnabled = enabled
                                    soundsPrefs
                                        .edit()
                                        .putBoolean(Constants.PREF_HAPTIC_ENABLED, enabled)
                                        .commit()
                                    if (enabled) {
                                        doHaptic(HapticFeedbackType.LongPress)
                                    }
                                },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Stimmen (TTS) ──
                        var ttsEnabled by remember {
                            mutableStateOf(soundsPrefs.getBoolean(Constants.PREF_TTS_ENABLED, false))
                        }

                        // Keep the Sounds switch in sync with external changes to
                        // PREF_TTS_ENABLED (specifically: when the user toggles TTS inside
                        // Datenschutz → Angepasste Datenschutzeinstellungen, which writes
                        // the same key). Without this listener the switch would remain
                        // stuck at the value loaded at initial composition.
                        androidx.compose.runtime.DisposableEffect(soundsPrefs) {
                            val listener =
                                android.content.SharedPreferences.OnSharedPreferenceChangeListener {
                                    prefs,
                                    key ->
                                    if (key == Constants.PREF_TTS_ENABLED) {
                                        ttsEnabled =
                                            prefs.getBoolean(Constants.PREF_TTS_ENABLED, false)
                                    }
                                }
                            soundsPrefs.registerOnSharedPreferenceChangeListener(listener)
                            onDispose {
                                soundsPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                            }
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
                                SettingsTtsIcon(isEnabled = ttsEnabled)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.settings_voices),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        if (ttsEnabled) stringResource(R.string.settings_tts_on)
                                        else stringResource(R.string.settings_tts_off),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = ttsEnabled,
                                onCheckedChange = { enabled ->
                                    doHaptic(HapticFeedbackType.LongPress)
                                    ttsEnabled = enabled
                                    soundsPrefs
                                        .edit()
                                        .putBoolean(Constants.PREF_TTS_ENABLED, enabled)
                                        .apply()
                                    // The Sounds switch only toggles the Sounds preference.
                                    // The Edge-TTS consent stays under the user's control in
                                    // Settings → Datenschutz / the consent sheet — this switch
                                    // never grants or withdraws the consent on its own. When
                                    // TTS is first triggered without consent, the per-use
                                    // PrivacyGateDialog handles the legal consent step.
                                },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                            )
                        }

                        // Voice picker (only when TTS is enabled)
                        if (ttsEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))

                            val localeVoices = remember {
                                com.bestjournal.app.util.TtsVoiceRegistry.getLocaleVoices()
                            }
                            val voices = localeVoices.voices
                            var selectedVoiceId by remember {
                                mutableStateOf(
                                    soundsPrefs.getString(
                                        Constants.PREF_EDGE_TTS_VOICE,
                                        localeVoices.defaultVoiceId,
                                    ) ?: localeVoices.defaultVoiceId
                                )
                            }
                            // If saved voice doesn't match current locale, reset to default and
                            // persist
                            val effectiveVoiceId =
                                if (voices.any { it.id == selectedVoiceId }) {
                                    selectedVoiceId
                                } else {
                                    val fallback = localeVoices.defaultVoiceId
                                    selectedVoiceId = fallback
                                    soundsPrefs
                                        .edit()
                                        .putString(Constants.PREF_EDGE_TTS_VOICE, fallback)
                                        .commit()
                                    fallback
                                }
                            val selectedVoice =
                                voices.find { it.id == effectiveVoiceId } ?: voices.firstOrNull()
                            var voiceExpanded by remember { mutableStateOf(false) }

                            if (selectedVoice != null) {
                                Text(
                                    stringResource(R.string.settings_voice_select),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                ExposedDropdownMenuBox(
                                    expanded = voiceExpanded,
                                    onExpandedChange = { voiceExpanded = it },
                                ) {
                                    TextField(
                                        value =
                                            com.bestjournal.app.util.TtsVoiceRegistry.displayName(
                                                selectedVoice,
                                                localeVoices.localeCode,
                                            ),
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            Icon(
                                                Icons.Rounded.KeyboardArrowDown,
                                                stringResource(R.string.settings_voice_choose),
                                            )
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
                                                focusedTextColor =
                                                    MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor =
                                                    MaterialTheme.colorScheme.onSurface,
                                                focusedIndicatorColor =
                                                    MaterialTheme.colorScheme.primary,
                                                unfocusedIndicatorColor = Color.Transparent,
                                            ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = voiceExpanded,
                                        onDismissRequest = { voiceExpanded = false },
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ) {
                                        voices.forEach { voice ->
                                            val label =
                                                com.bestjournal.app.util.TtsVoiceRegistry
                                                    .displayName(voice, localeVoices.localeCode)
                                            DropdownMenuItem(
                                                text = {
                                                    val textColor =
                                                        if (voice.id == effectiveVoiceId)
                                                            MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface
                                                    if (label.startsWith("\u2605")) {
                                                        Text(
                                                            buildAnnotatedString {
                                                                withStyle(
                                                                    SpanStyle(
                                                                        color =
                                                                            MaterialTheme
                                                                                .colorScheme
                                                                                .onSurface
                                                                    )
                                                                ) {
                                                                    append("\u2605 ")
                                                                }
                                                                withStyle(
                                                                    SpanStyle(color = textColor)
                                                                ) {
                                                                    append(
                                                                        label.removePrefix(
                                                                            "\u2605 "
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    } else {
                                                        Text(label, color = textColor)
                                                    }
                                                },
                                                onClick = {
                                                    selectedVoiceId = voice.id
                                                    soundsPrefs
                                                        .edit()
                                                        .putString(
                                                            Constants.PREF_EDGE_TTS_VOICE,
                                                            voice.id,
                                                        )
                                                        .commit()
                                                    voiceExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            } // end if (selectedVoice != null)
                        }
                    }
                }

                // Erinnerung
                var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                val notificationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) {
                            pendingPermissionAction?.invoke()
                        }
                        pendingPermissionAction = null
                    }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Notifications,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_reminder_section),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        var showTimePicker by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_daily_reminder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (uiState.reminderEnabled) {
                                    Text(
                                        stringResource(R.string.settings_reminder_time)
                                            .format(uiState.reminderHour, uiState.reminderMinute),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Text(
                                        stringResource(R.string.settings_reminder_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.reminderEnabled,
                                onCheckedChange = { enabled ->
                                    doHaptic(HapticFeedbackType.LongPress)
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
                                                    viewModel.updateReminderEnabled(true)
                                                    showTimePicker = true
                                                }
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
                                    stringResource(R.string.settings_weekly_review),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (uiState.weeklyReviewEnabled)
                                        stringResource(R.string.settings_weekly_review_on)
                                    else stringResource(R.string.settings_weekly_review_off),
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
                                    doHaptic(HapticFeedbackType.LongPress)
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
                                    stringResource(R.string.settings_monthly_review),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (uiState.monthlyReviewEnabled)
                                        stringResource(R.string.settings_monthly_review_on)
                                    else stringResource(R.string.settings_monthly_review_off),
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
                                    doHaptic(HapticFeedbackType.LongPress)
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
                                    stringResource(R.string.settings_yearly_review),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (uiState.yearlyReviewEnabled)
                                        stringResource(R.string.settings_yearly_review_on)
                                    else stringResource(R.string.settings_yearly_review_off),
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
                                    doHaptic(HapticFeedbackType.LongPress)
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
                            stringResource(R.string.settings_timezone, currentTimezone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Sicherheit
                GlassCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Security,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_security),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
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
                                        stringResource(R.string.settings_fingerprint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        stringResource(R.string.settings_fingerprint_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.biometricLock,
                                onCheckedChange = { enabled ->
                                    doHaptic(HapticFeedbackType.LongPress)
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.settings_auto_lock),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                // KI-Dashboard Profile
                GlassCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Dashboard,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_ai_profiles_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.profile_select_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val scenarioNames =
                            listOf(
                                stringResource(R.string.profile_summary),
                                stringResource(R.string.profile_entropy),
                                stringResource(R.string.profile_insight),
                                stringResource(R.string.profile_goals),
                                stringResource(R.string.profile_custom),
                            )
                        val scenarioPrefs = remember {
                            com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
                        }
                        val selectedScenario =
                            scenarioPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                        var currentScenario by remember { mutableIntStateOf(selectedScenario) }
                        var previousScenario by remember { mutableIntStateOf(selectedScenario) }
                        var showCustomPromptDialog by remember { mutableStateOf(false) }
                        var showScenarioInfoIndex by remember { mutableIntStateOf(-1) }

                        scenarioNames.forEachIndexed { index, name ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            doHaptic(HapticFeedbackType.LongPress)
                                            previousScenario = currentScenario
                                            currentScenario = index
                                            scenarioPrefs
                                                .edit()
                                                .putInt(Constants.PREF_DASHBOARD_SCENARIO, index)
                                                .putBoolean(Constants.PREF_RETRO_NEEDS_REGEN, true)
                                                .apply()
                                            viewModel.notifyProfileChanged()
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
                                        doHaptic(HapticFeedbackType.LongPress)
                                        previousScenario = currentScenario
                                        currentScenario = index
                                        scenarioPrefs
                                            .edit()
                                            .putInt(Constants.PREF_DASHBOARD_SCENARIO, index)
                                            .putBoolean(Constants.PREF_RETRO_NEEDS_REGEN, true)
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
                                                stringResource(R.string.profile_summary_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        1 ->
                                            Text(
                                                stringResource(R.string.profile_entropy_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        2 ->
                                            Text(
                                                stringResource(R.string.profile_insight_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        3 ->
                                            Text(
                                                stringResource(R.string.profile_goals_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        4 ->
                                            Text(
                                                stringResource(R.string.profile_custom_desc),
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
                                    3 -> Icons.Rounded.Star
                                    else -> Icons.Rounded.Tune
                                }
                            val infoText =
                                when (showScenarioInfoIndex) {
                                    0 -> stringResource(R.string.profile_summary_long)
                                    1 -> stringResource(R.string.profile_entropy_long)
                                    2 -> stringResource(R.string.profile_insight_long)
                                    3 -> stringResource(R.string.profile_goals_long)
                                    else -> stringResource(R.string.profile_custom_long)
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
                                            stringResource(R.string.action_understood),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showScenarioInfoIndex = -1
                                            currentScenario = previousScenario
                                            scenarioPrefs
                                                .edit()
                                                .putInt(
                                                    Constants.PREF_DASHBOARD_SCENARIO,
                                                    previousScenario,
                                                )
                                                .putBoolean(Constants.PREF_RETRO_NEEDS_REGEN, true)
                                                .apply()
                                            viewModel.notifyProfileChanged()
                                            onProfileChanged()
                                        }
                                    ) {
                                        Text(
                                            stringResource(R.string.action_cancel),
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                },
                            )
                        }

                        if (showCustomPromptDialog) {
                            val savedPrompt =
                                scenarioPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                            var promptText by remember { mutableStateOf(savedPrompt) }
                            val focusRequester = remember { FocusRequester() }

                            // Dialog-local state for improve/original toggle.
                            var preImproveText by remember { mutableStateOf<String?>(null) }
                            var improvedText by remember { mutableStateOf<String?>(null) }
                            var useImproved by remember { mutableStateOf(false) }

                            // Consume transcription: APPEND to whatever is in the field.
                            LaunchedEffect(uiState.promptPendingTranscription) {
                                uiState.promptPendingTranscription?.let { t ->
                                    val separator = if (promptText.isBlank()) "" else " "
                                    val newFull = promptText + separator + t.text
                                    promptText = newFull
                                    // New content invalidates previous improvement
                                    preImproveText = null
                                    improvedText = null
                                    useImproved = false
                                    viewModel.consumePromptTranscription()

                                    val autoImprove =
                                        scenarioPrefs.getBoolean(
                                            Constants.PREF_TEXT_IMPROVEMENT_DEFAULT,
                                            false,
                                        )
                                    if (autoImprove && newFull.isNotBlank()) {
                                        viewModel.improvePromptText(newFull)
                                    }
                                }
                            }

                            // Consume improvement: snapshot field as "Original", switch to improved
                            LaunchedEffect(uiState.promptPendingImprovement) {
                                uiState.promptPendingImprovement?.let { imp ->
                                    preImproveText = promptText
                                    improvedText = imp
                                    promptText = imp
                                    useImproved = true
                                    viewModel.consumePromptImprovement()
                                }
                            }

                            // Audio permission launcher
                            val micPermissionLauncher =
                                rememberLauncherForActivityResult(
                                    androidx.activity.result.contract.ActivityResultContracts
                                        .RequestPermission()
                                ) { granted ->
                                    if (granted) viewModel.togglePromptRecording()
                                }

                            AlertDialog(
                                onDismissRequest = {
                                    viewModel.clearPromptVoiceState()
                                    showCustomPromptDialog = false
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                title = {
                                    Text(
                                        stringResource(R.string.profile_custom),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                },
                                text = {
                                    Column {
                                        Text(
                                            stringResource(R.string.profile_custom_prompt),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(
                                            value = promptText,
                                            onValueChange = { promptText = it },
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .height(280.dp)
                                                    .focusRequester(focusRequester),
                                            placeholder = {
                                                val isDark = LocalIsDarkTheme.current
                                                Text(
                                                    stringResource(
                                                        R.string.settings_custom_prompt_placeholder
                                                    ),
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                            .copy(
                                                                alpha = if (isDark) 0.25f else 0.35f
                                                            ),
                                                )
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Pen + Mic row — both visually 72dp-wide, so the
                                        // pen FAB sits in a 72dp Box to match AnimatedMicButton
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // Pen button → focus TextField / open keyboard
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(72.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    FloatingActionButton(
                                                        onClick = { focusRequester.requestFocus() },
                                                        modifier = Modifier.size(64.dp),
                                                        containerColor =
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor =
                                                            MaterialTheme.colorScheme.onSurface,
                                                        shape = CircleShape,
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Edit,
                                                            contentDescription =
                                                                stringResource(
                                                                    R.string.journal_write
                                                                ),
                                                            modifier = Modifier.size(28.dp),
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    stringResource(R.string.journal_write),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Box(
                                                modifier =
                                                    Modifier.height(40.dp)
                                                        .width(1.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.outlineVariant
                                                        )
                                            )

                                            Spacer(modifier = Modifier.width(16.dp))

                                            // Mic button → record via Whisper/Groq (72dp outer)
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                AnimatedMicButton(
                                                    isRecording =
                                                        uiState.promptRecState ==
                                                            PromptRecState.RECORDING,
                                                    onClick = {
                                                        val granted =
                                                            androidx.core.content.ContextCompat
                                                                .checkSelfPermission(
                                                                    context,
                                                                    android.Manifest.permission
                                                                        .RECORD_AUDIO,
                                                                ) ==
                                                                android.content.pm.PackageManager
                                                                    .PERMISSION_GRANTED
                                                        if (granted) {
                                                            viewModel.togglePromptRecording()
                                                        } else {
                                                            micPermissionLauncher.launch(
                                                                android.Manifest.permission
                                                                    .RECORD_AUDIO
                                                            )
                                                        }
                                                    },
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    stringResource(R.string.journal_speak),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        // State banner below the buttons
                                        val stateLabel =
                                            when (uiState.promptRecState) {
                                                PromptRecState.TRANSCRIBING ->
                                                    stringResource(
                                                        R.string.journal_state_transcribing
                                                    )
                                                PromptRecState.IMPROVING ->
                                                    stringResource(
                                                        R.string.journal_state_improving
                                                    )
                                                else -> null
                                            }
                                        if (stateLabel != null) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                androidx.compose.material3
                                                    .CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    stateLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        } else if (uiState.promptTranscriptionModel != null &&
                                            uiState.promptRecState == PromptRecState.IDLE) {
                                            // Persistent hint after transcription completed
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                stringResource(
                                                    R.string.prompt_transcription_model_hint,
                                                    uiState.promptTranscriptionModel ?: "",
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color =
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        // Improve / toggle row
                                        val canImprove =
                                            promptText.isNotBlank() &&
                                                uiState.promptRecState == PromptRecState.IDLE
                                        if (canImprove && improvedText == null) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.improvePromptText(promptText)
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.AutoAwesome,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        stringResource(R.string.journal_improve),
                                                        style =
                                                            MaterialTheme.typography.labelMedium,
                                                    )
                                                }
                                            }
                                        } else if (improvedText != null) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                androidx.compose.material3.FilterChip(
                                                    selected = !useImproved,
                                                    onClick = {
                                                        preImproveText?.let { promptText = it }
                                                        useImproved = false
                                                    },
                                                    label = {
                                                        Text(stringResource(R.string.label_original))
                                                    },
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                androidx.compose.material3.FilterChip(
                                                    selected = useImproved,
                                                    onClick = {
                                                        improvedText?.let { promptText = it }
                                                        useImproved = true
                                                    },
                                                    label = {
                                                        Text(stringResource(R.string.label_improved))
                                                    },
                                                )
                                            }
                                        }

                                        // Error display
                                        uiState.promptError?.let { err ->
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                err,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
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
                                            viewModel.clearPromptVoiceState()
                                            showCustomPromptDialog = false
                                        }
                                    ) {
                                        Text(
                                            stringResource(R.string.action_save),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.clearPromptVoiceState()
                                            showCustomPromptDialog = false
                                        }
                                    ) {
                                        Text(
                                            stringResource(R.string.action_cancel),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Tune,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_ai_automations),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_text_improvement),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    stringResource(R.string.settings_text_improvement_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.textImprovementDefault,
                                onCheckedChange = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.updateTextImprovementDefault(it)
                                },
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
                                    stringResource(R.string.settings_dashboard_section),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    stringResource(R.string.settings_auto_update),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.autoUpdateDashboard,
                                onCheckedChange = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.updateAutoUpdateDashboard(it)
                                },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                            )
                        }
                    }
                }

                // Premium
                GlassCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Star,
                                null,
                                tint = FeatureAccentOrange,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_premium_section),
                                style = MaterialTheme.typography.titleMedium,
                                color = FeatureAccentOrange,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (uiState.isSubscribed) {
                            val subType by viewModel.subscriptionType.collectAsStateWithLifecycle()
                            val isLifetime =
                                subType == com.bestjournal.app.billing.SubscriptionType.LIFETIME
                            Text(
                                text =
                                    if (isLifetime)
                                        stringResource(R.string.settings_premium_lifetime)
                                    else stringResource(R.string.settings_premium_active),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text =
                                    if (isLifetime)
                                        stringResource(R.string.settings_premium_lifetime_desc)
                                    else stringResource(R.string.settings_premium_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            var showChurnDialog by remember { mutableStateOf(false) }
                            if (!isLifetime) {
                                TextButton(
                                    onClick = {
                                        playClick()
                                        showChurnDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        stringResource(R.string.settings_manage_subscription),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (showChurnDialog) {
                                val activity = context as? android.app.Activity
                                ChurnFlowDialog(
                                    onDismiss = { showChurnDialog = false },
                                    onOfferAccepted = { showChurnDialog = false },
                                    onCancelConfirmed = { showChurnDialog = false },
                                    onSwitchToYearly = {
                                        showChurnDialog = false
                                        onNavigateToPaywall("churn_yearly_switch")
                                    },
                                    onRetentionAccepted = {
                                        showChurnDialog = false
                                        activity?.let { viewModel.launchRetentionOffer(it) }
                                    },
                                    subscriptionType = subType,
                                    currentPrice = viewModel.getCurrentPrice(),
                                    retentionPrice = viewModel.getRetentionPrice(),
                                    analyticsTracker = viewModel.analyticsTracker,
                                    context = context,
                                )
                            }
                        } else {
                            var benefitsTracked by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                if (!benefitsTracked) {
                                    benefitsTracked = true
                                    viewModel.analyticsTracker.trackPremiumBenefitsViewed()
                                }
                            }
                            val featureItems =
                                listOf(
                                    Triple(
                                        Icons.Rounded.Star,
                                        stringResource(R.string.settings_premium_feature_improve),
                                        stringResource(
                                            R.string.settings_premium_feature_improve_desc
                                        ),
                                    ),
                                    Triple(
                                        Icons.Rounded.Dashboard,
                                        stringResource(R.string.settings_premium_feature_dashboard),
                                        stringResource(
                                            R.string.settings_premium_feature_dashboard_desc
                                        ),
                                    ),
                                    Triple(
                                        Icons.Rounded.Favorite,
                                        stringResource(
                                            R.string.settings_premium_feature_5_perspectives
                                        ),
                                        stringResource(
                                            R.string.settings_premium_feature_5_perspectives_desc
                                        ),
                                    ),
                                    Triple(
                                        Icons.Rounded.AutoAwesome,
                                        stringResource(R.string.settings_premium_feature_reviews),
                                        stringResource(
                                            R.string.settings_premium_feature_reviews_desc
                                        ),
                                    ),
                                    Triple(
                                        Icons.Rounded.Tune,
                                        stringResource(R.string.settings_premium_feature_patterns),
                                        stringResource(
                                            R.string.settings_premium_feature_patterns_desc
                                        ),
                                    ),
                                    Triple(
                                        Icons.Rounded.PictureAsPdf,
                                        stringResource(R.string.settings_premium_feature_pdf),
                                        stringResource(R.string.settings_premium_feature_pdf_desc),
                                    ),
                                    Triple(
                                        Icons.Rounded.MusicNote,
                                        stringResource(R.string.settings_premium_feature_voice),
                                        stringResource(R.string.settings_premium_feature_voice_desc),
                                    ),
                                )
                            featureItems.forEachIndexed { idx, (icon, title, subtitle) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.12f
                                                    )
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (idx < featureItems.lastIndex)
                                    Spacer(modifier = Modifier.height(8.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Breathing animation on the Premium CTA
                            val premiumTransition = rememberInfiniteTransition(label = "premiumCta")
                            val premiumCtaScale by
                                premiumTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.03f,
                                    animationSpec =
                                        infiniteRepeatable(
                                            animation = tween(2000, easing = EaseInOutSine),
                                            repeatMode = RepeatMode.Reverse,
                                        ),
                                    label = "premiumCtaScale",
                                )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Button(
                                    onClick = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        onNavigateToPaywall("settings_tap")
                                    },
                                    modifier =
                                        Modifier.height(48.dp).graphicsLayer {
                                            scaleX = premiumCtaScale
                                            scaleY = premiumCtaScale
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                ) {
                                    Text(
                                        stringResource(R.string.settings_unlock_premium),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }

                // Daten exportieren (PDF-Export)
                var showExportDialog by remember { mutableStateOf(false) }
                var exportIncludeEntries by remember { mutableStateOf(true) }
                var exportIncludePhotos by remember { mutableStateOf(true) }

                val pdfLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("application/pdf")
                    ) { uri ->
                        if (uri != null) {
                            viewModel.exportToPdf(
                                context,
                                uri,
                                includePhotos = exportIncludePhotos && exportIncludeEntries,
                            )
                        }
                    }

                // Export options dialog
                if (showExportDialog) {
                    AlertDialog(
                        onDismissRequest = { showExportDialog = false },
                        title = {
                            Text(
                                stringResource(R.string.settings_export_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    stringResource(R.string.settings_export_what),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Checkbox: Tagebucheinträge
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clickable { /* entries always included */ }
                                            .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = exportIncludeEntries,
                                        onCheckedChange = { checked ->
                                            exportIncludeEntries = checked
                                            if (!checked) exportIncludePhotos = false
                                        },
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.settings_export_entries),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }

                                // Checkbox: Fotos
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clickable {
                                                if (exportIncludeEntries)
                                                    exportIncludePhotos = !exportIncludePhotos
                                            }
                                            .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = exportIncludePhotos,
                                        onCheckedChange = { checked ->
                                            if (exportIncludeEntries) exportIncludePhotos = checked
                                        },
                                        enabled = exportIncludeEntries,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.settings_export_photos),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color =
                                            if (exportIncludeEntries)
                                                MaterialTheme.colorScheme.onSurface
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.4f
                                                ),
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showExportDialog = false
                                    playClick()
                                    viewModel.analyticsTracker.trackExportInitiated()
                                    val timestamp =
                                        SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                                            .format(Date())
                                    pdfLauncher.launch("BestJournal_Export_$timestamp.pdf")
                                },
                                enabled = exportIncludeEntries,
                            ) {
                                Icon(
                                    Icons.Rounded.PictureAsPdf,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_export_action))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExportDialog = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        },
                    )
                }

                GlassCard(
                    modifier =
                        Modifier.fillMaxWidth()
                            .then(
                                if (!uiState.isSubscribed)
                                    Modifier.clickable {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        onNavigateToPaywall("pdf_export")
                                    }
                                else Modifier
                            )
                ) {
                    Column {
                        // Icon sits next to the text, but an invisible end-spacer of the same
                        // width as (icon + icon-spacer) keeps the text optically centered
                        // within the card — as if the icon weren't there.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Description,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_export_data),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (!uiState.isSubscribed) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = stringResource(R.string.label_premium),
                                    tint = FeatureAccentOrange,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.width(28.dp))
                            }
                        }
                        if (uiState.isSubscribed) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.settings_export_pdf_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    playClick()
                                    doHaptic(HapticFeedbackType.LongPress)
                                    showExportDialog = true
                                },
                                enabled = !uiState.isExporting,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                            ) {
                                if (uiState.isExporting) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.settings_exporting))
                                } else {
                                    Icon(
                                        Icons.Rounded.PictureAsPdf,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.settings_export_entries_photos_pdf)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.settings_export_entries_photos_pdf_full),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        uiState.exportMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                msg,
                                style = MaterialTheme.typography.labelMedium,
                                color =
                                    if (uiState.exportIsError) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Achievements (zwischen Daten exportieren und Feedback)
                val achievementsWithStatus = remember {
                    com.bestjournal.app.util.AchievementTracker.ALL_ACHIEVEMENTS.map { a ->
                        val ts = clickPrefs.getLong("achievement_unlocked_${a.id}", 0L)
                        a.copy(unlockedAt = if (ts > 0L) ts else null)
                    }
                }
                AchievementsSection(
                    achievements = achievementsWithStatus,
                    onSectionViewed = {
                        com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                            .logEvent("achievements_viewed", null)
                    },
                )

                // 7. Feedback
                var showFeedbackDialog by remember { mutableStateOf(false) }
                var feedbackSent by remember { mutableStateOf(false) }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Email,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_feedback),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_feedback_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
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
                                Text(stringResource(R.string.settings_feedback_send))
                            }
                        }
                        if (feedbackSent) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.settings_feedback_sent),
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

                // Datenschutz (Analytics-Toggle + Konto löschen)
                val privacyPrefs = remember {
                    com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
                }
                var analyticsEnabled by remember {
                    mutableStateOf(
                        privacyPrefs.getBoolean(Constants.PREF_ANALYTICS_ENABLED, false)
                    )
                }
                var driveBackupEnabled by remember {
                    mutableStateOf(
                        privacyPrefs.getBoolean(Constants.PREF_DRIVE_BACKUP_ENABLED, false)
                    )
                }
                var groqConsented by remember {
                    mutableStateOf(
                        com.bestjournal.app.util.PrivacyGateHelper.hasConsented(
                            context,
                            com.bestjournal.app.util.PrivacyGateHelper.CloudService.Groq,
                        )
                    )
                }
                var geminiConsented by remember {
                    mutableStateOf(
                        com.bestjournal.app.util.PrivacyGateHelper.hasConsented(
                            context,
                            com.bestjournal.app.util.PrivacyGateHelper.CloudService.Gemini,
                        )
                    )
                }
                var ttsConsented by remember {
                    mutableStateOf(
                        com.bestjournal.app.util.PrivacyGateHelper.hasConsented(
                            context,
                            com.bestjournal.app.util.PrivacyGateHelper.CloudService.EdgeTts,
                        )
                    )
                }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var showPrivacySheet by remember { mutableStateOf(false) }

                // H5 — Crisis-Intervention-Dialog
                var showCrisisDialog by remember { mutableStateOf(false) }

                // M1 — § 356a BGB Widerrufsbutton: Gmail-API statt mailto
                val revokeScope = androidx.compose.runtime.rememberCoroutineScope()
                val revokeUserEmail = uiState.userProfile?.email
                var isRevokeSending by remember { mutableStateOf(false) }
                var revokeSuccessShown by remember { mutableStateOf(false) }
                var revokeError by remember { mutableStateOf<String?>(null) }
                var revokeNeedsEmail by remember { mutableStateOf(false) }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Security,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_privacy_header),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Open privacy preferences sheet — layered consent UX (EDSA 03/2023).
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    showPrivacySheet = true
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Rounded.Tune,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.settings_open_privacy_sheet),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                )
                            }
                        }

                        // NH2: CCPA/CPRA 2026 "Do Not Sell My Personal Information" toggle
                        // is shown inside the "Customized Privacy Settings" sheet (see
                        // PrivacyPreferencesSheet, gated by showDoNotSell=true for en-US).
                        // The separate standalone entry that used to live here has been
                        // removed to avoid a duplicate row under Privacy on American English.

                        Spacer(modifier = Modifier.height(16.dp))

                        // Delete account button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    showDeleteDialog = true
                                },
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                            ) {
                                Text(stringResource(R.string.settings_delete_account_title))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_delete_account_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Report AI content (Google Play AI Policy 04/2024) — two-step via dialog.
                        var showReportAiDialog by remember { mutableStateOf(false) }
                        val reportAiSubject = stringResource(R.string.settings_report_ai_subject)
                        val reportAiBody = stringResource(R.string.settings_report_ai_body)
                        val reportAiNoEmail = stringResource(R.string.settings_report_ai_no_email)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    showReportAiDialog = true
                                },
                            ) {
                                Text(stringResource(R.string.settings_report_ai_title))
                            }
                        }

                        if (showReportAiDialog) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showReportAiDialog = false },
                                title = {
                                    Text(stringResource(R.string.settings_report_ai_confirm_title))
                                },
                                text = {
                                    Text(stringResource(R.string.settings_report_ai_confirm_body))
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            showReportAiDialog = false
                                            val mailtoUri =
                                                android.net.Uri.parse(
                                                    "mailto:dev.app.support@gmail.com" +
                                                        "?subject=" +
                                                        android.net.Uri.encode(reportAiSubject) +
                                                        "&body=" +
                                                        android.net.Uri.encode(reportAiBody)
                                                )
                                            val intent =
                                                android.content.Intent(
                                                        android.content.Intent.ACTION_SENDTO,
                                                        mailtoUri,
                                                    )
                                                    .apply {
                                                        putExtra(
                                                            android.content.Intent.EXTRA_SUBJECT,
                                                            reportAiSubject,
                                                        )
                                                        putExtra(
                                                            android.content.Intent.EXTRA_TEXT,
                                                            reportAiBody,
                                                        )
                                                    }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: android.content.ActivityNotFoundException) {
                                                android.widget.Toast.makeText(
                                                        context,
                                                        reportAiNoEmail,
                                                        android.widget.Toast.LENGTH_LONG,
                                                    )
                                                    .show()
                                            }
                                        }
                                    ) {
                                        Text(stringResource(R.string.settings_report_ai_confirm))
                                    }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { showReportAiDialog = false }
                                    ) {
                                        Text(stringResource(R.string.settings_report_ai_cancel))
                                    }
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_report_ai_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // M1 — § 356a BGB Widerrufsbutton (ab 19.06.2026): zweistufig + direkter Versand via Gmail-API.
                        var showRevokeDialog by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    showRevokeDialog = true
                                },
                                enabled = !isRevokeSending,
                            ) {
                                Text(stringResource(R.string.settings_revoke_title))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_revoke_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // H5 — Crisis-Intervention-Eintrag (Mental-Health-Disclaimer + Hotlines)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    showCrisisDialog = true
                                },
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.settings_crisis_title))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_crisis_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        if (showRevokeDialog) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = {
                                    if (!isRevokeSending) showRevokeDialog = false
                                },
                                title = {
                                    Text(stringResource(R.string.settings_revoke_confirm_title))
                                },
                                text = {
                                    if (isRevokeSending) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(stringResource(R.string.settings_revoke_sending))
                                        }
                                    } else {
                                        Text(stringResource(R.string.settings_revoke_confirm_body))
                                    }
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        enabled = !isRevokeSending,
                                        onClick = {
                                            val email = revokeUserEmail
                                            if (email.isNullOrBlank()) {
                                                revokeError = context.getString(R.string.settings_revoke_no_account)
                                                showRevokeDialog = false
                                                return@TextButton
                                            }
                                            isRevokeSending = true
                                            revokeScope.launch {
                                                val timestamp = java.text.SimpleDateFormat(
                                                    "yyyy-MM-dd HH:mm:ss z",
                                                    java.util.Locale.getDefault(),
                                                ).format(java.util.Date())
                                                val body = context.getString(
                                                    R.string.settings_revoke_email_body,
                                                    email,
                                                    timestamp,
                                                )
                                                val confirmSubject = context.getString(
                                                    R.string.settings_revoke_confirm_subject,
                                                )
                                                val confirmBody = context.getString(
                                                    R.string.settings_revoke_confirm_user_body,
                                                    timestamp,
                                                )
                                                val subject = context.getString(
                                                    R.string.settings_revoke_email_subject,
                                                )
                                                try {
                                                    val error =
                                                        com.bestjournal.app.data.remote.RevokeSender.send(
                                                            context = context,
                                                            accountEmail = email,
                                                            subject = subject,
                                                            devBody = body,
                                                            userSubject = confirmSubject,
                                                            userBody = confirmBody,
                                                        )
                                                    isRevokeSending = false
                                                    showRevokeDialog = false
                                                    if (error == null) {
                                                        revokeSuccessShown = true
                                                    } else {
                                                        revokeError = error
                                                    }
                                                } catch (
                                                    e: com.bestjournal.app.data.remote.FeedbackNeedConsentException
                                                ) {
                                                    isRevokeSending = false
                                                    showRevokeDialog = false
                                                    try {
                                                        context.startActivity(e.consentIntent)
                                                    } catch (_: Exception) {}
                                                    revokeError = context.getString(
                                                        R.string.settings_feedback_allow_gmail,
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Text(stringResource(R.string.settings_revoke_confirm))
                                    }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        enabled = !isRevokeSending,
                                        onClick = { showRevokeDialog = false },
                                    ) {
                                        Text(stringResource(R.string.settings_revoke_cancel))
                                    }
                                },
                            )
                        }

                        // Success dialog — "Widerruf empfangen"
                        if (revokeSuccessShown) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { revokeSuccessShown = false },
                                title = { Text(stringResource(R.string.settings_revoke_success_title)) },
                                text = { Text(stringResource(R.string.settings_revoke_success_body)) },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { revokeSuccessShown = false },
                                    ) {
                                        Text(stringResource(R.string.settings_revoke_success_close))
                                    }
                                },
                            )
                        }

                        // Error dialog — Fallback auf mailto wenn Gmail-API fehlschlaegt
                        revokeError?.let { err ->
                            val revokeSubjectFallback = stringResource(R.string.settings_revoke_email_subject)
                            val revokeBodyFallback = stringResource(
                                R.string.settings_revoke_email_body,
                                revokeUserEmail ?: "",
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss z",
                                    java.util.Locale.getDefault(),
                                ).format(java.util.Date()),
                            )
                            val revokeNoEmail = stringResource(R.string.settings_revoke_no_email)
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { revokeError = null },
                                title = { Text(stringResource(R.string.settings_revoke_error_title)) },
                                text = {
                                    Text(stringResource(R.string.settings_revoke_error_body, err))
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            revokeError = null
                                            val mailtoUri = android.net.Uri.parse(
                                                "mailto:dev.app.support@gmail.com" +
                                                    "?subject=" +
                                                    android.net.Uri.encode(revokeSubjectFallback) +
                                                    "&body=" +
                                                    android.net.Uri.encode(revokeBodyFallback)
                                            )
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_SENDTO,
                                                mailtoUri,
                                            )
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: android.content.ActivityNotFoundException) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    revokeNoEmail,
                                                    android.widget.Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        },
                                    ) {
                                        Text(stringResource(R.string.settings_revoke_error_email_fallback))
                                    }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { revokeError = null },
                                    ) {
                                        Text(stringResource(R.string.action_close))
                                    }
                                },
                            )
                        }

                        if (showCrisisDialog) {
                            com.bestjournal.app.ui.components.CrisisHelpDialog(
                                onDismiss = { showCrisisDialog = false },
                            )
                        }
                    }
                }

                // Layered-consent: Privacy Preferences Sheet
                com.bestjournal.app.ui.components.PrivacyPreferencesSheet(
                    visible = showPrivacySheet,
                    initial = com.bestjournal.app.ui.components.PrivacyPreferences(
                        analytics = analyticsEnabled,
                        groq = groqConsented,
                        gemini = geminiConsented,
                        tts = ttsConsented,
                        driveBackup = driveBackupEnabled,
                        doNotSell = privacyPrefs.getBoolean(Constants.PREF_DO_NOT_SELL, false),
                    ),
                    onDismiss = { showPrivacySheet = false },
                    onSave = { prefs ->
                        analyticsEnabled = prefs.analytics
                        driveBackupEnabled = prefs.driveBackup
                        groqConsented = prefs.groq
                        geminiConsented = prefs.gemini
                        ttsConsented = prefs.tts
                        privacyPrefs
                            .edit()
                            .putBoolean(Constants.PREF_ANALYTICS_ENABLED, prefs.analytics)
                            .putBoolean(Constants.PREF_DRIVE_BACKUP_ENABLED, prefs.driveBackup)
                            .putBoolean(Constants.PREF_DO_NOT_SELL, prefs.doNotSell)
                            .putLong(
                                Constants.PREF_CONSENT_TIMESTAMP,
                                System.currentTimeMillis(),
                            )
                            .apply()
                        com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                            .setAnalyticsCollectionEnabled(prefs.analytics)
                        com.bestjournal.app.util.PrivacyGateHelper.setConsent(
                            context,
                            com.bestjournal.app.util.PrivacyGateHelper.CloudService.Groq,
                            prefs.groq,
                        )
                        com.bestjournal.app.util.PrivacyGateHelper.setConsent(
                            context,
                            com.bestjournal.app.util.PrivacyGateHelper.CloudService.Gemini,
                            prefs.gemini,
                        )
                        com.bestjournal.app.util.PrivacyGateHelper.setConsent(
                            context,
                            com.bestjournal.app.util.PrivacyGateHelper.CloudService.EdgeTts,
                            prefs.tts,
                        )

                        // Mirror the TTS Datenschutz toggle into the Sounds switch — same
                        // direction as ConsentViewModel.persist() (#1694). Datenschutz is
                        // the master; flipping it here updates the Sounds/Haptik "Stimmen"
                        // switch and seeds the locale default voice when turning on.
                        try {
                            val encPrefs =
                                com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
                            encPrefs
                                .edit()
                                .putBoolean(Constants.PREF_TTS_ENABLED, prefs.tts)
                                .apply()
                            if (prefs.tts &&
                                encPrefs.getString(Constants.PREF_EDGE_TTS_VOICE, null)
                                    .isNullOrBlank()
                            ) {
                                val defaultVoice =
                                    com.bestjournal.app.util.TtsVoiceRegistry
                                        .getLocaleVoices()
                                        .defaultVoiceId
                                encPrefs
                                    .edit()
                                    .putString(Constants.PREF_EDGE_TTS_VOICE, defaultVoice)
                                    .apply()
                            }
                        } catch (_: Exception) {
                            // Silent fallback — user can still flip the Sounds switch later.
                        }
                    },
                    showDoNotSell = run {
                        val loc =
                            androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                        loc.language == "en" && loc.country == "US"
                    },
                )

                if (showDeleteDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = {
                            Text(stringResource(R.string.settings_delete_account_confirm_title))
                        },
                        text = {
                            Text(stringResource(R.string.settings_delete_account_confirm_body))
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    viewModel.deleteAccount(context)
                                }
                            ) {
                                Text(
                                    stringResource(R.string.settings_delete_account_confirm),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showDeleteDialog = false }
                            ) {
                                Text(stringResource(R.string.settings_delete_account_cancel))
                            }
                        },
                    )
                }

                // Progress dialog while account deletion is running (non-dismissible — the
                // app will restart itself when done).
                if (uiState.deleteAccountInProgress) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {},
                        title = {},
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.settings_delete_account_in_progress)
                                )
                            }
                        },
                        confirmButton = {},
                    )
                }

                // Honest error dialog when Drive deletion failed. User must actively choose
                // retry, local-only wipe, or abort — we never silently claim "unwiderruflich".
                uiState.deleteAccountDriveError?.let { reason ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.dismissDeleteAccountError() },
                        title = {
                            Text(
                                stringResource(
                                    R.string.settings_delete_account_drive_error_title
                                )
                            )
                        },
                        text = {
                            Text(
                                stringResource(
                                    R.string.settings_delete_account_drive_error_body,
                                    reason,
                                )
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.deleteAccount(context) }
                            ) {
                                Text(stringResource(R.string.settings_delete_account_retry))
                            }
                        },
                        dismissButton = {
                            Column {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        viewModel.deleteAccount(
                                            context,
                                            forceLocalDelete = true,
                                        )
                                    }
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.settings_delete_account_force_local
                                        ),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                androidx.compose.material3.TextButton(
                                    onClick = { viewModel.dismissDeleteAccountError() }
                                ) {
                                    Text(
                                        stringResource(R.string.settings_delete_account_abort)
                                    )
                                }
                            }
                        },
                    )
                }

                // 9. Ueber die App
                GlassCard(
                    modifier = Modifier.wrapContentWidth().align(Alignment.CenterHorizontally)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Info,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_about),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.settings_about_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(R.string.settings_about_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "© Barwandt Digital Labs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            if (uiState.showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showLogoutDialog(false) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            stringResource(R.string.settings_sign_out_confirm),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    text = {
                        Text(
                            stringResource(R.string.settings_sign_out_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                viewModel.signOut(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        ) {
                            Text(stringResource(R.string.settings_sign_out))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                viewModel.showLogoutDialog(false)
                            }
                        ) {
                            Text(
                                stringResource(R.string.action_cancel),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }

            // Fallback: if showSubscriptionSheet is triggered from elsewhere, navigate to paywall
            LaunchedEffect(showSubscriptionSheet) {
                if (showSubscriptionSheet) {
                    onNavigateToPaywall("settings_tap")
                    showSubscriptionSheet = false
                }
            }
        }
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
private fun SettingsPhoneIcon(isDark: Boolean, isActive: Boolean = true) {
    val glowYellow = Color(0xFFFFD54F)
    val mutedGray = Color(0xFF666666)
    val lightPhoneSize by
        animateDpAsState(
            targetValue = if (isActive && !isDark) 22.dp else if (!isActive) 18.dp else 14.dp,
            animationSpec = tween(300),
            label = "lightPhoneSize",
        )
    val darkPhoneSize by
        animateDpAsState(
            targetValue = if (isActive && isDark) 22.dp else if (!isActive) 18.dp else 14.dp,
            animationSpec = tween(300),
            label = "darkPhoneSize",
        )
    val lightTint = if (isActive && !isDark) glowYellow else mutedGray
    val darkTint = if (isActive && isDark) glowYellow else mutedGray

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
                stringResource(R.string.toggle_light),
                tint = lightTint,
                modifier = Modifier.size(lightPhoneSize),
            )
            Icon(
                Icons.Rounded.LightMode,
                null,
                tint = lightTint,
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
                stringResource(R.string.toggle_dark),
                tint = darkTint,
                modifier = Modifier.size(darkPhoneSize),
            )
            Icon(
                Icons.Rounded.DarkMode,
                null,
                tint = darkTint,
                modifier = Modifier.size(darkPhoneSize * 0.35f),
            )
        }
    }
}

@Composable
private fun SettingsSoundIcon(isEnabled: Boolean) {
    val activeColor = MaterialTheme.colorScheme.primary
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
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Rounded.VolumeUp,
                stringResource(R.string.toggle_sound_on),
                tint = if (isEnabled) activeColor else mutedGray,
                modifier = Modifier.size(onSize),
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
                Icons.Rounded.VolumeUp,
                stringResource(R.string.toggle_sound_off),
                tint = if (!isEnabled) Color(0xFFEF4444) else mutedGray,
                modifier = Modifier.size(offSize),
            )
        }
    }
}

@Composable
private fun SettingsHapticIcon(isEnabled: Boolean) {
    val activeColor = MaterialTheme.colorScheme.primary
    val mutedGray = Color(0xFF666666)
    val onSize by
        animateDpAsState(
            targetValue = if (isEnabled) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "hapticOnSize",
        )
    val offSize by
        animateDpAsState(
            targetValue = if (!isEnabled) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "hapticOffSize",
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
                Icons.Rounded.Vibration,
                stringResource(R.string.toggle_haptic_on),
                tint = if (isEnabled) activeColor else mutedGray,
                modifier = Modifier.size(onSize),
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
                Icons.Rounded.Vibration,
                stringResource(R.string.toggle_haptic_off),
                tint = if (!isEnabled) Color(0xFFEF4444) else mutedGray,
                modifier = Modifier.size(offSize),
            )
        }
    }
}

@Composable
private fun SettingsTtsIcon(isEnabled: Boolean) {
    val activeColor = MaterialTheme.colorScheme.primary
    val mutedGray = Color(0xFF666666)
    val onSize by
        animateDpAsState(
            targetValue = if (isEnabled) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "ttsOnSize",
        )
    val offSize by
        animateDpAsState(
            targetValue = if (!isEnabled) 22.dp else 14.dp,
            animationSpec = tween(300),
            label = "ttsOffSize",
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
                Icons.Rounded.RecordVoiceOver,
                stringResource(R.string.toggle_voice_on),
                tint = if (isEnabled) activeColor else mutedGray,
                modifier = Modifier.size(onSize),
            )
        }
        VerticalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.height(16.dp),
        )
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Rounded.RecordVoiceOver,
                stringResource(R.string.toggle_voice_off),
                tint = if (!isEnabled) Color(0xFFEF4444) else mutedGray,
                modifier = Modifier.size(offSize),
            )
        }
    }
}

@Composable
private fun SettingsSunMoonIcon(isDark: Boolean, isActive: Boolean = true) {
    val glowYellow = Color(0xFFFFD54F)
    val mutedGray = Color(0xFF666666)
    // When inactive both icons stay small and gray (no highlight in this row).
    val sunSize by
        animateDpAsState(
            targetValue = if (isActive && !isDark) 22.dp else if (!isActive) 18.dp else 14.dp,
            animationSpec = tween(300),
            label = "settingSunSize",
        )
    val moonSize by
        animateDpAsState(
            targetValue = if (isActive && isDark) 22.dp else if (!isActive) 18.dp else 14.dp,
            animationSpec = tween(300),
            label = "settingMoonSize",
        )
    val sunTint = if (isActive && !isDark) glowYellow else mutedGray
    val moonTint = if (isActive && isDark) glowYellow else mutedGray

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
                stringResource(R.string.toggle_sun),
                tint = sunTint,
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
                stringResource(R.string.toggle_moon),
                tint = moonTint,
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
                stringResource(R.string.settings_feedback_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.settings_feedback_dialog_desc),
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
                            stringResource(R.string.settings_feedback_placeholder),
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
                        stringResource(R.string.settings_feedback_sending),
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
                            errorMessage =
                                context.getString(R.string.settings_feedback_sign_in_first)
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
                                errorMessage =
                                    context.getString(R.string.settings_feedback_allow_gmail)
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
                Text(stringResource(R.string.action_send))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { if (!isSending) onDismiss() }) {
                Text(
                    stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState =
        rememberTimePickerState(
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    stringResource(R.string.settings_reminder_when),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimeInput(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                colors =
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

// ── Weekly Review Picker Dialog ─────────────────────────────────────────────

@Composable
private fun weekDayName(calendarDay: Int): String =
    when (calendarDay) {
        java.util.Calendar.MONDAY -> stringResource(R.string.day_monday)
        java.util.Calendar.TUESDAY -> stringResource(R.string.day_tuesday)
        java.util.Calendar.WEDNESDAY -> stringResource(R.string.day_wednesday)
        java.util.Calendar.THURSDAY -> stringResource(R.string.day_thursday)
        java.util.Calendar.FRIDAY -> stringResource(R.string.day_friday)
        java.util.Calendar.SATURDAY -> stringResource(R.string.day_saturday)
        java.util.Calendar.SUNDAY -> stringResource(R.string.day_sunday)
        else -> stringResource(R.string.day_sunday)
    }

// Map UI index (0=Monday) to Calendar constant
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyReviewPickerDialog(
    initialDay: Int,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val weekDayLabels = stringArrayResource(R.array.weekday_abbreviations)
    var selectedDayIndex by remember {
        mutableIntStateOf(weekDays.indexOf(initialDay).coerceAtLeast(0))
    }
    val timePickerState =
        rememberTimePickerState(
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    stringResource(R.string.settings_review_when),
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
                    stringResource(R.string.settings_review_weekday),
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
                        Surface(
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
                    stringResource(R.string.settings_review_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                TimeInput(state = timePickerState)
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
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

// ── Churn / Retention Dialog ────────────────────────────────────────────────
// ChurnFlowDialog is now in its own file: ChurnFlowDialog.kt
// Old ChurnRetentionDialog removed — replaced by 3-step ChurnFlowDialog
