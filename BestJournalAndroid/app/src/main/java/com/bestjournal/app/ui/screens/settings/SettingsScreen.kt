package com.bestjournal.app.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bestjournal.app.R
import com.bestjournal.app.ui.components.AiLimitsDisclaimerRow
import com.bestjournal.app.ui.components.AnimatedMicButton
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.components.ParticleBackground
import com.bestjournal.app.ui.components.TwinklingStars
import com.bestjournal.app.ui.theme.CustomPalette
import com.bestjournal.app.ui.theme.FeatureAccentOrange
import com.bestjournal.app.ui.theme.GoalPalette
import com.bestjournal.app.ui.theme.InsightPalette
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.ui.theme.SummaryPalette
import com.bestjournal.app.ui.theme.WarmCopper
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
    onNavigateToLegal: (String) -> Unit = {},
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

    // Loop-5 fix: re-query Google Play whenever the Settings screen becomes
    // visible AND every time the activity returns to RESUMED — so a Play
    // Store cancellation made via the external "Abos verwalten" page is
    // picked up immediately when the user returns to BestJournal, instead
    // of leaving the dialog frozen on the pre-cancellation state for hours.
    val settingsLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(settingsLifecycleOwner) {
        settingsLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshSubscriptionStatus()
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
                                    isActive = !uiState.followSystem,
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
                        Spacer(modifier = Modifier.height(14.dp))
                        // Trenner innerhalb der Bubble — separiert Hell/Dunkel-Modus von der
                        // Theme-Auswahl darunter.
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp,
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Themes Manager — Dropdown, aktuell zwei Optionen (Neutral / Profilfarbe);
                        // weitere Themes werden spaeter als Enum-Eintraege in AppTheme
                        // hinzugefuegt.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Palette,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.settings_themes_manager),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        var themeExpanded by remember { mutableStateOf(false) }
                        val currentTheme =
                            com.bestjournal.app.ui.theme.AppTheme.fromKey(uiState.selectedThemeKey)
                        val themeDisplayName =
                            when (currentTheme) {
                                com.bestjournal.app.ui.theme.AppTheme.Neutral ->
                                    stringResource(R.string.theme_neutral)
                                com.bestjournal.app.ui.theme.AppTheme.Profile ->
                                    stringResource(R.string.theme_profile)
                                com.bestjournal.app.ui.theme.AppTheme.Solarized ->
                                    stringResource(R.string.theme_solarized)
                                com.bestjournal.app.ui.theme.AppTheme.Dracula ->
                                    stringResource(R.string.theme_dracula)
                                com.bestjournal.app.ui.theme.AppTheme.OneDark ->
                                    stringResource(R.string.theme_one_dark)
                                com.bestjournal.app.ui.theme.AppTheme.Nord ->
                                    stringResource(R.string.theme_nord)
                                com.bestjournal.app.ui.theme.AppTheme.Gruvbox ->
                                    stringResource(R.string.theme_gruvbox)
                            }

                        ExposedDropdownMenuBox(
                            expanded = themeExpanded,
                            onExpandedChange = { themeExpanded = it },
                        ) {
                            TextField(
                                value = themeDisplayName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        Icons.Rounded.KeyboardArrowDown,
                                        stringResource(R.string.settings_theme),
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
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                            )
                            ExposedDropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                com.bestjournal.app.ui.theme.AppTheme.entries.forEach { theme ->
                                    val label =
                                        when (theme) {
                                            com.bestjournal.app.ui.theme.AppTheme.Neutral ->
                                                stringResource(R.string.theme_neutral)
                                            com.bestjournal.app.ui.theme.AppTheme.Profile ->
                                                stringResource(R.string.theme_profile)
                                            com.bestjournal.app.ui.theme.AppTheme.Solarized ->
                                                stringResource(R.string.theme_solarized)
                                            com.bestjournal.app.ui.theme.AppTheme.Dracula ->
                                                stringResource(R.string.theme_dracula)
                                            com.bestjournal.app.ui.theme.AppTheme.OneDark ->
                                                stringResource(R.string.theme_one_dark)
                                            com.bestjournal.app.ui.theme.AppTheme.Nord ->
                                                stringResource(R.string.theme_nord)
                                            com.bestjournal.app.ui.theme.AppTheme.Gruvbox ->
                                                stringResource(R.string.theme_gruvbox)
                                        }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                color =
                                                    if (theme == currentTheme)
                                                        MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = {
                                            doHaptic(HapticFeedbackType.LongPress)
                                            viewModel.updateSelectedTheme(theme.storageKey)
                                            themeExpanded = false
                                        },
                                    )
                                }
                            }
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
                            mutableStateOf(
                                soundsPrefs.getBoolean(Constants.PREF_TTS_ENABLED, false)
                            )
                        }

                        // Keep the Sounds switch in sync with external changes to
                        // PREF_TTS_ENABLED (specifically: when the user toggles TTS inside
                        // Datenschutz → Angepasste Datenschutzeinstellungen, which writes
                        // the same key). Without this listener the switch would remain
                        // stuck at the value loaded at initial composition.
                        androidx.compose.runtime.DisposableEffect(soundsPrefs) {
                            val listener =
                                android.content.SharedPreferences
                                    .OnSharedPreferenceChangeListener { prefs, key ->
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
                        Spacer(modifier = Modifier.height(12.dp))

                        val fixedScenarioNames =
                            listOf(
                                stringResource(R.string.profile_summary),
                                stringResource(R.string.profile_entropy),
                                stringResource(R.string.profile_insight),
                                stringResource(R.string.profile_goals),
                            )
                        val defaultCustomName = stringResource(R.string.profile_custom)
                        val scenarioPrefs = remember {
                            com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
                        }
                        val selectedScenario =
                            scenarioPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                        var currentScenario by remember { mutableIntStateOf(selectedScenario) }
                        var previousScenario by remember { mutableIntStateOf(selectedScenario) }
                        var showCustomPromptDialog by remember { mutableStateOf(false) }
                        var showScenarioInfoIndex by remember { mutableIntStateOf(-1) }

                        // Dynamic list of user-defined custom analyses. Kept in sync with
                        // encrypted prefs — every add/remove/rename reloads from the store so
                        // the UI, the dashboard and the Drive backup stay consistent.
                        var customList by remember {
                            mutableStateOf(
                                com.bestjournal.app.data.prefs.CustomAnalysesStore.load(
                                    scenarioPrefs,
                                    defaultCustomName,
                                )
                            )
                        }
                        // Remembers which custom entry the dialog is editing so renames and
                        // prompt saves always write to the right id, even after the list
                        // changes underneath.
                        var editingCustomId by remember { mutableStateOf<String?>(null) }
                        // Free-tier gate: adding a third custom analysis (customList.size >= 2
                        // before the add) opens the Premium upsell sheet instead.
                        var showProfilesPremiumSheet by remember { mutableStateOf(false) }

                        // Fall back to the current `profile_custom` translation for any
                        // entry the user has not renamed. Keeps the list in sync with the
                        // device language even after an entry was created in a different
                        // locale (the stored name in prefs is a frozen literal).
                        val scenarioNames =
                            fixedScenarioNames +
                                customList.map {
                                    com.bestjournal.app.data.prefs.CustomAnalysesStore.displayName(
                                        it,
                                        defaultCustomName,
                                    )
                                }

                        fun selectScenario(index: Int) {
                            doHaptic(HapticFeedbackType.LongPress)
                            previousScenario = currentScenario
                            currentScenario = index
                            // ProfileTheme.update() schreibt das Pref UND aktualisiert den
                            // Compose-State sofort — die App-Farbe greift in derselben Frame.
                            com.bestjournal.app.ui.theme.ProfileTheme.update(context, index)
                            scenarioPrefs
                                .edit()
                                .putBoolean(Constants.PREF_RETRO_NEEDS_REGEN, true)
                                .apply()
                            viewModel.notifyProfileChanged()
                            // For custom profiles, remember which entry is being
                            // edited so the prompt dialog (opened later, after
                            // the explanation has been confirmed) targets the
                            // right id even if the list changes underneath.
                            if (index >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                                editingCustomId =
                                    customList
                                        .getOrNull(index - Constants.FIRST_CUSTOM_SCENARIO_INDEX)
                                        ?.id
                            }
                            // Always show the explanation dialog first. For custom
                            // profiles, the prompt dialog opens only after the user
                            // taps "Verstanden" in the explanation — see the
                            // confirmButton below.
                            showScenarioInfoIndex = index
                            onProfileChanged()
                        }

                        scenarioNames.forEachIndexed { index, name ->
                            val isCustom = index >= Constants.FIRST_CUSTOM_SCENARIO_INDEX
                            val localCustomIndex = index - Constants.FIRST_CUSTOM_SCENARIO_INDEX
                            val customEntry =
                                if (isCustom) customList.getOrNull(localCustomIndex) else null
                            val canDelete = isCustom && localCustomIndex > 0
                            // Onboarding-Profile: icon + accent color per profile (1:1 from
                            // OnboardingScreen.kt). All custom profiles share the Custom
                            // palette, identical to how the onboarding card represents the
                            // single "Custom" entry there.
                            val profileAccent =
                                when (index) {
                                    0 -> SummaryPalette.accent
                                    1 -> WarmCopper
                                    2 -> InsightPalette.primary
                                    3 -> GoalPalette.primary
                                    else -> CustomPalette.primary
                                }
                            val profileIcon =
                                when (index) {
                                    0 -> Icons.Rounded.AutoStories
                                    1 -> Icons.Rounded.Whatshot
                                    2 -> Icons.Rounded.SelfImprovement
                                    3 -> Icons.Rounded.RocketLaunch
                                    else -> Icons.Rounded.Science
                                }
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectScenario(index) }
                                        .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Coloured accent bar — same dimensions as in OnboardingScreen
                                Box(
                                    modifier =
                                        Modifier.width(4.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(profileAccent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                // Profile icon in a soft circular badge in the same accent
                                // colour — mirrors the onboarding card layout.
                                Box(
                                    modifier =
                                        Modifier.size(40.dp)
                                            .clip(CircleShape)
                                            .background(profileAccent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = profileIcon,
                                        contentDescription = null,
                                        tint = profileAccent,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                RadioButton(
                                    selected = currentScenario == index,
                                    onClick = { selectScenario(index) },
                                    colors =
                                        RadioButtonDefaults.colors(selectedColor = profileAccent),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color =
                                            if (currentScenario == index) profileAccent
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
                                        else ->
                                            Text(
                                                stringResource(R.string.profile_custom_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                    }
                                }
                                if (isCustom) {
                                    IconButton(
                                        onClick = {
                                            doHaptic(HapticFeedbackType.LongPress)
                                            // Free tier: the default entry and one extra
                                            // custom analysis are free. Trying to add a
                                            // third opens the Premium upsell sheet instead.
                                            val limitReached =
                                                !uiState.isSubscribed && customList.size >= 2
                                            if (limitReached) {
                                                showProfilesPremiumSheet = true
                                            } else {
                                                com.bestjournal.app.data.prefs.CustomAnalysesStore
                                                    .add(scenarioPrefs, defaultCustomName)
                                                customList =
                                                    com.bestjournal.app.data.prefs
                                                        .CustomAnalysesStore
                                                        .load(scenarioPrefs, defaultCustomName)
                                                viewModel.backupCustomAnalysesToDrive()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription =
                                                stringResource(R.string.profile_custom_add),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    if (canDelete && customEntry != null) {
                                        IconButton(
                                            onClick = {
                                                doHaptic(HapticFeedbackType.LongPress)
                                                val removed =
                                                    com.bestjournal.app.data.prefs
                                                        .CustomAnalysesStore
                                                        .remove(scenarioPrefs, customEntry.id)
                                                if (removed) {
                                                    customList =
                                                        com.bestjournal.app.data.prefs
                                                            .CustomAnalysesStore
                                                            .load(scenarioPrefs, defaultCustomName)
                                                    val total =
                                                        fixedScenarioNames.size + customList.size
                                                    if (currentScenario >= total) {
                                                        previousScenario = currentScenario
                                                        currentScenario = 0
                                                        scenarioPrefs
                                                            .edit()
                                                            .putInt(
                                                                Constants.PREF_DASHBOARD_SCENARIO,
                                                                0,
                                                            )
                                                            .putBoolean(
                                                                Constants.PREF_RETRO_NEEDS_REGEN,
                                                                true,
                                                            )
                                                            .apply()
                                                        viewModel.notifyProfileChanged()
                                                        onProfileChanged()
                                                    }
                                                    viewModel.backupCustomAnalysesToDrive()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Remove,
                                                contentDescription =
                                                    stringResource(R.string.profile_custom_remove),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showScenarioInfoIndex >= 0) {
                            val infoTitle = scenarioNames[showScenarioInfoIndex]
                            // Match the per-profile accent + icon used on the
                            // selectable profile cards above (1:1 from
                            // OnboardingScreen.kt). All custom profiles share
                            // the Custom palette + Science icon.
                            val infoIcon =
                                when (showScenarioInfoIndex) {
                                    0 -> Icons.Rounded.AutoStories
                                    1 -> Icons.Rounded.Whatshot
                                    2 -> Icons.Rounded.SelfImprovement
                                    3 -> Icons.Rounded.RocketLaunch
                                    else -> Icons.Rounded.Science
                                }
                            val infoAccent =
                                when (showScenarioInfoIndex) {
                                    0 -> SummaryPalette.accent
                                    1 -> WarmCopper
                                    2 -> InsightPalette.primary
                                    3 -> GoalPalette.primary
                                    else -> CustomPalette.primary
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
                                        tint = infoAccent,
                                        modifier = Modifier.size(36.dp),
                                    )
                                },
                                title = {
                                    Text(
                                        infoTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = infoAccent,
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
                                    TextButton(
                                        onClick = {
                                            val wasCustom =
                                                showScenarioInfoIndex >=
                                                    Constants.FIRST_CUSTOM_SCENARIO_INDEX
                                            showScenarioInfoIndex = -1
                                            // For custom profiles, only NOW open the
                                            // prompt-input dialog — after the user has
                                            // read and confirmed the explanation.
                                            if (wasCustom) {
                                                showCustomPromptDialog = true
                                            }
                                        }
                                    ) {
                                        Text(
                                            stringResource(R.string.action_understood),
                                            color = infoAccent,
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
                            // Resolve the entry being edited. If editingCustomId was lost
                            // (e.g. entry deleted under us), fall back to the first entry so
                            // the dialog stays usable instead of crashing.
                            val activeEntry =
                                customList.firstOrNull { it.id == editingCustomId }
                                    ?: customList.firstOrNull()
                            val activeEntryId = activeEntry?.id
                            val savedPrompt = activeEntry?.prompt.orEmpty()
                            // Mirror the list display: entries the user has not renamed
                            // show the current locale's default, not the frozen literal.
                            val savedName =
                                activeEntry?.let {
                                    com.bestjournal.app.data.prefs.CustomAnalysesStore.displayName(
                                        it,
                                        defaultCustomName,
                                    )
                                } ?: defaultCustomName
                            var promptText by
                                remember(activeEntryId) { mutableStateOf(savedPrompt) }
                            var titleText by remember(activeEntryId) { mutableStateOf(savedName) }
                            var titleEditing by remember(activeEntryId) { mutableStateOf(false) }
                            val titleFocus = remember(activeEntryId) { FocusRequester() }
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

                            // Clear-text confirmation
                            var showClearConfirm by remember { mutableStateOf(false) }
                            if (showClearConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showClearConfirm = false },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    title = {
                                        Text(
                                            stringResource(R.string.prompt_clear_confirm_title),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    },
                                    text = {
                                        Text(
                                            stringResource(R.string.prompt_clear_confirm_text),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                promptText = ""
                                                preImproveText = null
                                                improvedText = null
                                                useImproved = false
                                                viewModel.clearPromptVoiceState()
                                                showClearConfirm = false
                                            }
                                        ) {
                                            Text(
                                                stringResource(R.string.action_yes),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showClearConfirm = false }) {
                                            Text(
                                                stringResource(R.string.action_no),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                )
                            }

                            AlertDialog(
                                onDismissRequest = {
                                    viewModel.clearPromptVoiceState()
                                    showCustomPromptDialog = false
                                    editingCustomId = null
                                    titleEditing = false
                                },
                                modifier = Modifier.fillMaxWidth(0.95f),
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                                containerColor = MaterialTheme.colorScheme.surface,
                                // Same icon + accent the custom profile uses on the
                                // selectable list and in the explanation dialog —
                                // mirrors the onboarding card visual identity.
                                icon = {
                                    Icon(
                                        Icons.Rounded.Science,
                                        null,
                                        tint = CustomPalette.primary,
                                        modifier = Modifier.size(36.dp),
                                    )
                                },
                                title = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (titleEditing && activeEntryId != null) {
                                            BasicTextField(
                                                value = titleText,
                                                onValueChange = { titleText = it },
                                                singleLine = true,
                                                modifier =
                                                    Modifier.weight(1f).focusRequester(titleFocus),
                                                textStyle =
                                                    MaterialTheme.typography.titleLarge.copy(
                                                        color = CustomPalette.primary
                                                    ),
                                                cursorBrush = SolidColor(CustomPalette.primary),
                                            )
                                            LaunchedEffect(activeEntryId) {
                                                titleFocus.requestFocus()
                                            }
                                            IconButton(
                                                onClick = {
                                                    doHaptic(HapticFeedbackType.LongPress)
                                                    com.bestjournal.app.data.prefs
                                                        .CustomAnalysesStore
                                                        .rename(
                                                            scenarioPrefs,
                                                            activeEntryId,
                                                            titleText,
                                                            defaultCustomName,
                                                        )
                                                    customList =
                                                        com.bestjournal.app.data.prefs
                                                            .CustomAnalysesStore
                                                            .load(scenarioPrefs, defaultCustomName)
                                                    titleText =
                                                        customList
                                                            .firstOrNull { it.id == activeEntryId }
                                                            ?.name ?: titleText
                                                    titleEditing = false
                                                    viewModel.backupCustomAnalysesToDrive()
                                                },
                                                modifier = Modifier.size(32.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription =
                                                        stringResource(
                                                            R.string.profile_custom_rename_save
                                                        ),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        } else {
                                            Text(
                                                titleText,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = CustomPalette.primary,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (activeEntryId != null) {
                                                IconButton(
                                                    onClick = {
                                                        doHaptic(HapticFeedbackType.LongPress)
                                                        titleEditing = true
                                                    },
                                                    modifier = Modifier.size(32.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription =
                                                            stringResource(
                                                                R.string.profile_custom_rename
                                                            ),
                                                        tint = CustomPalette.primary,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                text = {
                                    Column {
                                        Text(
                                            stringResource(R.string.profile_custom_prompt),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .height(420.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .border(
                                                        width = 1.dp,
                                                        color =
                                                            MaterialTheme.colorScheme
                                                                .outlineVariant,
                                                        shape = RoundedCornerShape(4.dp),
                                                    )
                                        ) {
                                            val promptScroll =
                                                androidx.compose.foundation.rememberScrollState()
                                            BasicTextField(
                                                value = promptText,
                                                onValueChange = { promptText = it },
                                                modifier =
                                                    Modifier.fillMaxSize()
                                                        .padding(
                                                            start = 14.dp,
                                                            end = 14.dp,
                                                            top = 40.dp,
                                                            bottom = 12.dp,
                                                        )
                                                        .verticalScroll(promptScroll)
                                                        .focusRequester(focusRequester),
                                                textStyle =
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                cursorBrush =
                                                    SolidColor(MaterialTheme.colorScheme.primary),
                                                decorationBox = { innerTextField ->
                                                    if (promptText.isEmpty()) {
                                                        val isDark = LocalIsDarkTheme.current
                                                        Text(
                                                            stringResource(
                                                                R.string
                                                                    .settings_custom_prompt_placeholder
                                                            ),
                                                            style =
                                                                MaterialTheme.typography.bodyMedium,
                                                            color =
                                                                MaterialTheme.colorScheme
                                                                    .onSurfaceVariant
                                                                    .copy(
                                                                        alpha =
                                                                            if (isDark) 0.25f
                                                                            else 0.35f
                                                                    ),
                                                        )
                                                    }
                                                    innerTextField()
                                                },
                                            )

                                            if (promptText.isNotBlank()) {
                                                IconButton(
                                                    onClick = { showClearConfirm = true },
                                                    modifier =
                                                        Modifier.align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(28.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription =
                                                            stringResource(
                                                                R.string.prompt_clear_content_desc
                                                            ),
                                                        tint =
                                                            MaterialTheme.colorScheme
                                                                .onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }

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
                                                            MaterialTheme.colorScheme
                                                                .surfaceVariant,
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

                                            Spacer(modifier = Modifier.width(32.dp))

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
                                                    stringResource(R.string.journal_state_improving)
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
                                        } else if (
                                            uiState.promptTranscriptionModel != null &&
                                                uiState.promptRecState == PromptRecState.IDLE
                                        ) {
                                            // Persistent hint after transcription completed
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                stringResource(
                                                    R.string.prompt_transcription_model_hint,
                                                    uiState.promptTranscriptionModel ?: "",
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
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
                                                        style = MaterialTheme.typography.labelMedium,
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
                                                        Text(
                                                            stringResource(R.string.label_original)
                                                        )
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
                                                        Text(
                                                            stringResource(R.string.label_improved)
                                                        )
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
                                            if (activeEntryId != null) {
                                                // Commit any unsaved rename first so the name and
                                                // prompt land together on disk and on Drive.
                                                if (titleEditing) {
                                                    com.bestjournal.app.data.prefs
                                                        .CustomAnalysesStore
                                                        .rename(
                                                            scenarioPrefs,
                                                            activeEntryId,
                                                            titleText,
                                                            defaultCustomName,
                                                        )
                                                }
                                                val previousPrompt =
                                                    customList
                                                        .firstOrNull { it.id == activeEntryId }
                                                        ?.prompt
                                                        .orEmpty()
                                                com.bestjournal.app.data.prefs.CustomAnalysesStore
                                                    .setPrompt(
                                                        scenarioPrefs,
                                                        activeEntryId,
                                                        promptText,
                                                    )
                                                customList =
                                                    com.bestjournal.app.data.prefs
                                                        .CustomAnalysesStore
                                                        .load(scenarioPrefs, defaultCustomName)
                                                val promptChanged = promptText != previousPrompt
                                                // Refresh the local save timestamp on EVERY Save,
                                                // even if nothing changed, so both devices agree
                                                // that this was the last authoritative save and
                                                // the same device does not later pull an
                                                // identical Drive copy as "newer".
                                                val editor =
                                                    scenarioPrefs
                                                        .edit()
                                                        .putLong(
                                                            "custom_prompt_saved_at",
                                                            System.currentTimeMillis(),
                                                        )
                                                if (promptChanged) {
                                                    editor.putBoolean(
                                                        Constants.PREF_RETRO_NEEDS_REGEN,
                                                        true,
                                                    )
                                                }
                                                editor.apply()
                                                // Always push to Drive on save — even if the text
                                                // did not change — so all other devices pick up
                                                // the refreshed timestamp and re-download if
                                                // their local copy was stale.
                                                viewModel.backupCustomAnalysesToDrive()
                                                if (promptChanged) {
                                                    viewModel.notifyProfileChanged()
                                                    onProfileChanged()
                                                }
                                            }
                                            viewModel.clearPromptVoiceState()
                                            showCustomPromptDialog = false
                                            editingCustomId = null
                                            titleEditing = false
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
                                            editingCustomId = null
                                            titleEditing = false
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

                        if (showProfilesPremiumSheet) {
                            CustomAnalysesPremiumSheet(
                                onSubscribe = {
                                    showProfilesPremiumSheet = false
                                    onNavigateToPaywall("custom_analyses_limit")
                                },
                                onDismiss = { showProfilesPremiumSheet = false },
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
                        // Loop-4 fix: collectAsStateWithLifecycle MUST be called
                        // outside the if-branch so the StateFlow is permanently
                        // subscribed — otherwise on every isSubscribed flip the
                        // collector restarts and emits a stale NONE for 1-2
                        // frames, briefly showing the wrong UI.
                        val subType by viewModel.subscriptionType.collectAsStateWithLifecycle()
                        // Loop-7 (Frank, 2026-04-30): pull the live cancel /
                        // expiry state straight from the BillingManager so
                        // the headline text in the Premium card shows
                        // "Gekuendigt — laeuft bis 30.04.2026, 21:01" the
                        // moment Google reports the cancellation, instead of
                        // a flat "Premium-Abo aktiv" until expiry.
                        val premiumAutoRenewing by
                            viewModel.autoRenewingState.collectAsStateWithLifecycle()
                        val premiumExpiryRaw by
                            viewModel.expiryTimeState.collectAsStateWithLifecycle()
                        val premiumExpiryFormatted =
                            remember(premiumExpiryRaw) {
                                formatPremiumExpiryDateTime(premiumExpiryRaw)
                            }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (uiState.isSubscribed) {
                            val isLifetime =
                                subType == com.bestjournal.app.billing.SubscriptionType.LIFETIME
                            val isCancelled = !isLifetime && !premiumAutoRenewing
                            Text(
                                text =
                                    when {
                                        isLifetime ->
                                            stringResource(R.string.settings_premium_lifetime)
                                        isCancelled && premiumExpiryFormatted != null ->
                                            stringResource(
                                                R.string.settings_premium_cancelled_until,
                                                premiumExpiryFormatted!!,
                                            )
                                        isCancelled ->
                                            stringResource(R.string.settings_premium_cancelled)
                                        else -> stringResource(R.string.settings_premium_active)
                                    },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text =
                                    when {
                                        isLifetime ->
                                            stringResource(R.string.settings_premium_lifetime_desc)
                                        isCancelled ->
                                            stringResource(R.string.settings_premium_cancelled_desc)
                                        else -> stringResource(R.string.settings_premium_desc)
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            var showChurnDialog by remember { mutableStateOf(false) }
                            if (!isLifetime) {
                                TextButton(
                                    onClick = {
                                        // Loop-7 (Frank, 2026-04-30): no
                                        // click beep on "Abo verwalten" —
                                        // the dialog opens silently. The
                                        // refresh below pulls the freshest
                                        // Google state so the dialog
                                        // renders correct price + auto-
                                        // renew + expiry from the start.
                                        viewModel.refreshSubscriptionStatus()
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
                                // Reactive props — re-renders the dialog whenever
                                // BillingManager updates price, type, promo counter etc.
                                val churnCurrentPrice by
                                    viewModel.currentPriceState.collectAsStateWithLifecycle()
                                val churnRetentionPrice by
                                    viewModel.retentionPriceState.collectAsStateWithLifecycle()
                                val churnPromoInfo by
                                    viewModel.promoInfoState.collectAsStateWithLifecycle()
                                val churnAutoRenewing by
                                    viewModel.autoRenewingState.collectAsStateWithLifecycle()
                                val churnExpiryTime by
                                    viewModel.expiryTimeState.collectAsStateWithLifecycle()
                                val churnIsOnRetention by
                                    viewModel.isOnRetentionPlanState.collectAsStateWithLifecycle()
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
                                    currentPrice = churnCurrentPrice,
                                    retentionPrice = churnRetentionPrice,
                                    analyticsTracker = viewModel.analyticsTracker,
                                    context = context,
                                    promoInfo = churnPromoInfo,
                                    autoRenewing = churnAutoRenewing,
                                    expiryTime = churnExpiryTime,
                                    isAlreadyOnRetentionPlan = churnIsOnRetention,
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
                                        Icons.Rounded.Category,
                                        stringResource(R.string.settings_premium_feature_profiles),
                                        stringResource(
                                            R.string.settings_premium_feature_profiles_desc
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
                                    Triple(
                                        Icons.Rounded.EditNote,
                                        stringResource(R.string.settings_premium_feature_followups),
                                        stringResource(
                                            R.string.settings_premium_feature_followups_desc
                                        ),
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
                            Spacer(modifier = Modifier.height(12.dp))
                            AiLimitsDisclaimerRow()
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
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
                                            stringResource(
                                                R.string.settings_export_entries_photos_pdf
                                            )
                                        )
                                    }
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
                    mutableStateOf(privacyPrefs.getBoolean(Constants.PREF_ANALYTICS_ENABLED, false))
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
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // T-002: Android System-Backup transparency link (DSGVO Art. 13)
                        // Opens Android backup settings so users can manage automatic backup.
                        LegalDocumentRow(
                            label = stringResource(R.string.settings_open_system_backup_settings_label),
                            onClick = {
                                try {
                                    // ACTION_BACKUP_SETTINGS ist @hide in der Android-API,
                                    // deshalb der direkte Action-String. Funktioniert seit API 8 stabil.
                                    val backupIntent: Intent = Intent("android.settings.BACKUP_SETTINGS")
                                    backupIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(backupIntent)
                                } catch (e: Exception) {
                                    try {
                                        val privacyIntent: Intent = Intent(android.provider.Settings.ACTION_PRIVACY_SETTINGS)
                                        privacyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(privacyIntent)
                                    } catch (e2: Exception) {
                                        Toast.makeText(
                                            context,
                                            R.string.error_no_backup_settings_available,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )

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

                        // Report AI content (Google Play AI Policy 04/2024) — in-app dialog
                        // matching FeedbackDialog UX. Sends via Gmail API to dev support
                        // with subject pre-filled. Body is pre-filled from a localized
                        // template and the user can edit it before sending.
                        var showReportAiDialog by remember { mutableStateOf(false) }
                        var reportAiSent by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    showReportAiDialog = true
                                    reportAiSent = false
                                }
                            ) {
                                Text(stringResource(R.string.settings_report_ai_title))
                            }
                        }

                        if (reportAiSent) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.settings_feedback_sent),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(3000)
                                reportAiSent = false
                            }
                        }

                        if (showReportAiDialog) {
                            ReportAiDialog(
                                userEmail = uiState.userProfile?.email,
                                onDismiss = { showReportAiDialog = false },
                                onSent = {
                                    showReportAiDialog = false
                                    reportAiSent = true
                                },
                                context = context,
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

                        // M1 — § 356a BGB Widerrufsbutton (ab 19.06.2026): zweistufig + direkter
                        // Versand via Gmail-API.
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
                                        contentColor = MaterialTheme.colorScheme.primary
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
                                                revokeError =
                                                    context.getString(
                                                        R.string.settings_revoke_no_account
                                                    )
                                                showRevokeDialog = false
                                                return@TextButton
                                            }
                                            isRevokeSending = true
                                            revokeScope.launch {
                                                val timestamp =
                                                    java.text
                                                        .SimpleDateFormat(
                                                            "yyyy-MM-dd HH:mm:ss z",
                                                            java.util.Locale.getDefault(),
                                                        )
                                                        .format(java.util.Date())
                                                val body =
                                                    context.getString(
                                                        R.string.settings_revoke_email_body,
                                                        email,
                                                        timestamp,
                                                    )
                                                val confirmSubject =
                                                    context.getString(
                                                        R.string.settings_revoke_confirm_subject
                                                    )
                                                val confirmBody =
                                                    context.getString(
                                                        R.string.settings_revoke_confirm_user_body,
                                                        timestamp,
                                                    )
                                                val subject =
                                                    context.getString(
                                                        R.string.settings_revoke_email_subject
                                                    )
                                                try {
                                                    val error =
                                                        com.bestjournal.app.data.remote.RevokeSender
                                                            .send(
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
                                                    e:
                                                        com.bestjournal.app.data.remote.FeedbackNeedConsentException) {
                                                    isRevokeSending = false
                                                    showRevokeDialog = false
                                                    try {
                                                        context.startActivity(e.consentIntent)
                                                    } catch (_: Exception) {}
                                                    revokeError =
                                                        context.getString(
                                                            R.string.settings_feedback_allow_gmail
                                                        )
                                                }
                                            }
                                        },
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
                                title = {
                                    Text(stringResource(R.string.settings_revoke_success_title))
                                },
                                text = {
                                    Text(stringResource(R.string.settings_revoke_success_body))
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { revokeSuccessShown = false }
                                    ) {
                                        Text(stringResource(R.string.settings_revoke_success_close))
                                    }
                                },
                            )
                        }

                        // Error dialog — Fallback auf mailto wenn Gmail-API fehlschlaegt
                        revokeError?.let { err ->
                            val revokeSubjectFallback =
                                stringResource(R.string.settings_revoke_email_subject)
                            val revokeBodyFallback =
                                stringResource(
                                    R.string.settings_revoke_email_body,
                                    revokeUserEmail ?: "",
                                    java.text
                                        .SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss z",
                                            java.util.Locale.getDefault(),
                                        )
                                        .format(java.util.Date()),
                                )
                            val revokeNoEmail = stringResource(R.string.settings_revoke_no_email)
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { revokeError = null },
                                title = {
                                    Text(stringResource(R.string.settings_revoke_error_title))
                                },
                                text = {
                                    Text(stringResource(R.string.settings_revoke_error_body, err))
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            revokeError = null
                                            val mailtoUri =
                                                android.net.Uri.parse(
                                                    "mailto:dev.app.support@gmail.com" +
                                                        "?subject=" +
                                                        android.net.Uri.encode(
                                                            revokeSubjectFallback
                                                        ) +
                                                        "&body=" +
                                                        android.net.Uri.encode(revokeBodyFallback)
                                                )
                                            val intent =
                                                android.content.Intent(
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
                                                    )
                                                    .show()
                                            }
                                        }
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.settings_revoke_error_email_fallback
                                            )
                                        )
                                    }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { revokeError = null }
                                    ) {
                                        Text(stringResource(R.string.action_close))
                                    }
                                },
                            )
                        }

                        if (showCrisisDialog) {
                            com.bestjournal.app.ui.components.CrisisHelpDialog(
                                onDismiss = { showCrisisDialog = false }
                            )
                        }
                    }
                }

                // Layered-consent: Privacy Preferences Sheet
                com.bestjournal.app.ui.components.PrivacyPreferencesSheet(
                    visible = showPrivacySheet,
                    initial =
                        com.bestjournal.app.ui.components.PrivacyPreferences(
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
                            .putLong(Constants.PREF_CONSENT_TIMESTAMP, System.currentTimeMillis())
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
                            if (
                                prefs.tts &&
                                    encPrefs
                                        .getString(Constants.PREF_EDGE_TTS_VOICE, null)
                                        .isNullOrBlank()
                            ) {
                                val defaultVoice =
                                    com.bestjournal.app.util.TtsVoiceRegistry.getLocaleVoices()
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
                    showDoNotSell =
                        run {
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
                                Text(stringResource(R.string.settings_delete_account_in_progress))
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
                            Text(stringResource(R.string.settings_delete_account_drive_error_title))
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
                                        viewModel.deleteAccount(context, forceLocalDelete = true)
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
                                    Text(stringResource(R.string.settings_delete_account_abort))
                                }
                            }
                        },
                    )
                }

                // 9. Ueber die App
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
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
                            stringResource(R.string.settings_about_copyright),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )

                        // 9b. Rechtliche Dokumente — In-App + Online
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.settings_legal_section_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 3 In-App buttons (offline, file:///android_asset/...)
                        // Order: Datenschutz, Nutzungsbedingungen, Impressum
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LegalDocumentRow(
                                label = stringResource(R.string.legal_title_datenschutz),
                                onClick = { onNavigateToLegal("legal/datenschutz") },
                            )
                            LegalDocumentRow(
                                label = stringResource(R.string.legal_title_nutzungsbedingungen),
                                onClick = { onNavigateToLegal("legal/nutzungsbedingungen") },
                            )
                            LegalDocumentRow(
                                label = stringResource(R.string.legal_title_impressum),
                                onClick = { onNavigateToLegal("legal/impressum") },
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.settings_legal_online_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val onlineUrl = stringResource(R.string.settings_legal_online_url)
                        LegalDocumentRow(
                            label = stringResource(R.string.settings_legal_section_header),
                            onClick = {
                                val intent =
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(onlineUrl),
                                    )
                                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            },
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

@Composable
private fun ReportAiDialog(
    userEmail: String?,
    onDismiss: () -> Unit,
    onSent: () -> Unit,
    context: android.content.Context,
) {
    // Pre-fill the textfield with a localized template that the user can edit.
    val initialBody = stringResource(R.string.settings_report_ai_body)
    val subjectLine = stringResource(R.string.settings_report_ai_subject)
    var reportText by remember { mutableStateOf(initialBody) }
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
                stringResource(R.string.settings_report_ai_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.settings_report_ai_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = reportText,
                    onValueChange = { reportText = it },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
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
                    if (reportText.isNotBlank() && !isSending) {
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
                                        feedbackText = reportText,
                                        subject = subjectLine,
                                    )
                                if (error == null) {
                                    onSent()
                                } else {
                                    isSending = false
                                    errorMessage = error
                                }
                            } catch (
                                e: com.bestjournal.app.data.remote.FeedbackNeedConsentException) {
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
                enabled = reportText.isNotBlank() && !isSending,
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

/**
 * Premium upsell sheet shown when a free user tries to add a third custom analysis profile. Mirrors
 * the layout of ReviewPremiumSheet (monthly-review paywall) so the visual language is consistent
 * across the app.
 *
 * The sheet does not subscribe the user directly — it routes to the full PaywallScreen on "Abo
 * starten" so the normal billing flow runs.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CustomAnalysesPremiumSheet(onSubscribe: () -> Unit, onDismiss: () -> Unit) {
    val sheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val infiniteTransition = rememberInfiniteTransition(label = "profilesCta")
    val ctaScale by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "profilesCtaScale",
        )

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Rounded.Category,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.profiles_premium_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.profiles_premium_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CustomAnalysesBenefitPoint(stringResource(R.string.profiles_benefit_many))
                CustomAnalysesBenefitPoint(stringResource(R.string.profiles_benefit_switch))
                CustomAnalysesBenefitPoint(stringResource(R.string.profiles_benefit_named))
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onSubscribe,
                modifier =
                    Modifier.fillMaxWidth().height(54.dp).graphicsLayer {
                        scaleX = ctaScale
                        scaleY = ctaScale
                    },
                shape = RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    stringResource(R.string.retro_start_sub),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.retro_decide_later),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CustomAnalysesBenefitPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
            tint = FeatureAccentOrange,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegalDocumentRow(label: String, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * Loop-7 (Frank, 2026-04-30): parse the cloud function's ISO-8601 expiryTime (UTC, e.g.
 * "2026-04-30T19:01:04.944Z") into a localised "30.04.2026, 21:01" string for the Premium-card
 * headline. Returns null when the input is missing or unparseable so the caller can fall back to
 * the plain "Gekuendigt" label.
 */
private fun formatPremiumExpiryDateTime(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso) ?: return null
        val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        formatter.timeZone = java.util.TimeZone.getDefault()
        formatter.format(date)
    } catch (_: Exception) {
        try {
            val fallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            fallback.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = fallback.parse(iso) ?: return null
            val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
            formatter.timeZone = java.util.TimeZone.getDefault()
            formatter.format(date)
        } catch (_: Exception) {
            null
        }
    }
}
