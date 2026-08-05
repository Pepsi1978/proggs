package com.entropyjournal.ui.screens.settings

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.entropyjournal.data.prefs.CustomAnalysesStore
import com.entropyjournal.util.rememberHapticAction
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.MobileOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.CircleShape
import com.entropyjournal.ui.components.AnimatedMicButton
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.components.NeonDivider
import com.entropyjournal.ui.theme.AppTheme
import com.entropyjournal.ui.theme.CustomPalette
import com.entropyjournal.ui.theme.DEFAULT_BODY_FONT_NAME
import com.entropyjournal.ui.theme.DEFAULT_HEADING_FONT_NAME
import com.entropyjournal.ui.theme.MAX_FONT_SCALE
import com.entropyjournal.ui.theme.MIN_FONT_SCALE
import com.entropyjournal.ui.theme.GoalPalette
import com.entropyjournal.ui.theme.InsightPalette
import com.entropyjournal.ui.theme.LocalIsDarkTheme
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.ui.theme.ProfileTheme
import com.entropyjournal.ui.theme.SummaryPalette
import com.entropyjournal.ui.theme.WarmCopper
import com.entropyjournal.ui.theme.bodyFontFamily
import com.entropyjournal.ui.theme.headingFontFamily
import com.entropyjournal.util.Constants
import com.entropyjournal.util.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private data class ThemeChoice(
    val label: String,
    val colors: List<Color>,
    val isNew: Boolean = false,
)

private val themeChoices =
    listOf(
        ThemeChoice("Goldener Faden", listOf(Color(0xFFE8B547), Color(0xFFDF741E)), true),
        ThemeChoice("Profilfarbe", listOf(Color(0xFF14B8A6), Color(0xFF22D3EE))),
        ThemeChoice("Neutral", listOf(Color(0xFFC25E00), Color(0xFFC25E00))),
        ThemeChoice("Sonnenwende", listOf(Color(0xFF268BD2), Color(0xFF268BD2))),
        ThemeChoice("Mitternacht", listOf(Color(0xFFBD93F9), Color(0xFFFF79C6))),
        ThemeChoice("Atelier", listOf(Color(0xFF61AFEF), Color(0xFF4078F2))),
        ThemeChoice("Polarnacht", listOf(Color(0xFF88C0D0), Color(0xFF5E81AC))),
        ThemeChoice("Bernstein", listOf(Color(0xFFD79921), Color(0xFFB57614))),
        ThemeChoice("Cosmos", listOf(Color(0xFF22D3EE), Color(0xFFA78BFA))),
        ThemeChoice("Aurora", listOf(Color(0xFF7DD3A4), Color(0xFFB5A8E8))),
        ThemeChoice("Polarlicht", listOf(Color(0xFF4CC9F0), Color(0xFF4361EE)), true),
        ThemeChoice("Nebula", listOf(Color(0xFFC084FC), Color(0xFFF472B6)), true),
        ThemeChoice("Smaragdwald", listOf(Color(0xFF34D399), Color(0xFFE8B547)), true),
        ThemeChoice("Sonnenglut", listOf(Color(0xFFFF8C42), Color(0xFFD93636)), true),
    )

private data class FontChoice(
    val name: String,
    val description: String,
)

private val headingFontChoices =
    listOf(
        FontChoice("Playfair Display", "Elegant & literarisch — Standard"),
        FontChoice("Great Vibes", "Kalligrafie — mit viel Schwung"),
        FontChoice("Caveat", "Handschrift — locker & persönlich"),
        FontChoice("Lora", "Ruhige, klassische Serife"),
        FontChoice("Sora", "Modern & klar"),
        FontChoice("Space Grotesk", "Technisch & präzise"),
        FontChoice("Nunito", "Freundlich & rund"),
    )

private val bodyFontChoices =
    listOf(
        FontChoice("Source Sans 3", "Klar & neutral — Standard"),
        FontChoice("Manrope", "Ruhig & lesbar"),
        FontChoice("IBM Plex Sans", "Sachlich & präzise"),
        FontChoice("Nunito Sans", "Weich & freundlich"),
        FontChoice("Lora", "Serife — wie ein Buch"),
        FontChoice("Caveat", "Handschrift — wie ein echtes Tagebuch"),
    )

private fun themeChoiceFor(theme: AppTheme): ThemeChoice =
    themeChoices[
        when (theme) {
            AppTheme.GoldenThread -> 0
            AppTheme.Profile -> 1
            AppTheme.Neutral -> 2
            AppTheme.Solarized -> 3
            AppTheme.Dracula -> 4
            AppTheme.OneDark -> 5
            AppTheme.Nord -> 6
            AppTheme.Gruvbox -> 7
            AppTheme.Cosmos -> 8
            AppTheme.Aurora -> 9
            AppTheme.PolarLight -> 10
            AppTheme.Nebula -> 11
            AppTheme.EmeraldForest -> 12
            AppTheme.SunEmber -> 13
        }
    ]

@Composable
private fun goldenSwitchColors() =
    SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )

@Composable
private fun FontSelector(
    label: String,
    selectedName: String,
    expanded: Boolean,
    choices: List<FontChoice>,
    fontFamily: (String) -> FontFamily,
    onToggle: () -> Unit,
    onSelected: (String) -> Unit,
) {
    Text(
        label,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    RoundedCornerShape(14.dp),
                )
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            selectedName,
            modifier = Modifier.weight(1f),
            fontFamily = fontFamily(selectedName),
            fontWeight = FontWeight.Bold,
            fontSize = if (label == "Überschriften") 17.sp else 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Schließen" else "Öffnen",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
    if (expanded) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEach { choice ->
                val active = choice.name == selectedName
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                            .border(
                                1.dp,
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelected(choice.name) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier.size(18.dp)
                                .border(
                                    2.dp,
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (active) {
                            Box(
                                modifier =
                                    Modifier.size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            choice.name,
                            fontFamily = fontFamily(choice.name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            choice.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Rasterweite des Groessen-Reglers: 5-Prozent-Schritte zwischen [MIN_FONT_SCALE] und [MAX_FONT_SCALE]. */
private val fontScaleSteps = ((MAX_FONT_SCALE - MIN_FONT_SCALE) / 0.05f).roundToInt() - 1

@Composable
private fun FontSizeSlider(
    label: String,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onScaleCommitted: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${(scale * 100).roundToInt()} %",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Slider(
        value = scale,
        onValueChange = onScaleChange,
        onValueChangeFinished = onScaleCommitted,
        valueRange = MIN_FONT_SCALE..MAX_FONT_SCALE,
        steps = fontScaleSteps,
        colors =
            SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignOut: () -> Unit,
    onProfileChanged: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    val doHaptic = rememberHapticAction()

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

    var showExportDialog by remember { mutableStateOf(false) }
    var exportIncludeEntries by remember { mutableStateOf(true) }
    var exportIncludePhotos by remember { mutableStateOf(true) }

    val pdfLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null) {
                viewModel.exportToPdf(context, uri, includePhotos = exportIncludePhotos && exportIncludeEntries)
            }
        }

    // Export options dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    "PDF-Export",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    Text(
                        "Was soll exportiert werden?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            "Tagebucheinträge",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (exportIncludeEntries) exportIncludePhotos = !exportIncludePhotos
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
                            "Fotos",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (exportIncludeEntries)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                        pdfLauncher.launch("EntropyJournal_Export_$timestamp.pdf")
                    },
                    enabled = exportIncludeEntries,
                ) {
                    Icon(
                        Icons.Rounded.Description,
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportieren")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            // Fixed title bar (does not scroll)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    "Einstellungen",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 27.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(10.dp))
                NeonDivider(horizontalPadding = 0.dp, thickness = 2.dp)
            }

            // Scrollable content
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                                "Konto",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
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
                                    onClick = { doHaptic(HapticFeedbackType.LongPress); viewModel.showLogoutDialog(true) },
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Fotos sichern",
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
                                    colors = goldenSwitchColors(),
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
                                        "Videos sichern",
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
                                    colors = goldenSwitchColors(),
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Button(
                                    onClick = { doHaptic(HapticFeedbackType.LongPress); viewModel.syncNow() },
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
                                    onClick = { doHaptic(HapticFeedbackType.LongPress); viewModel.signIn(context) },
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
                                "Erscheinungsbild",
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
                                SettingsSunMoonIcon(isDark = LocalIsDarkTheme.current, isActive = !uiState.followSystem && !uiState.followSun)
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
                                    doHaptic(HapticFeedbackType.LongPress)
                                    if (uiState.followSystem) viewModel.updateFollowSystem(false)
                                    viewModel.updateDarkTheme(it)
                                },
                                colors = goldenSwitchColors(),
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
                                SettingsPhoneIcon(isDark = LocalIsDarkTheme.current, isActive = uiState.followSystem)
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
                                onCheckedChange = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.updateFollowSystem(it)
                                },
                                colors = goldenSwitchColors(),
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
                                SettingsSunMoonIcon(isDark = LocalIsDarkTheme.current, isActive = uiState.followSun)
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
                                colors = goldenSwitchColors(),
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                    }
                }

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
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Themes Manager",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        val currentTheme =
                            com.entropyjournal.ui.theme.ProfileTheme.currentAppTheme.value
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            themeChoices.forEach { choice ->
                                val matchingTheme =
                                    AppTheme.entries.firstOrNull {
                                        themeChoiceFor(it).label == choice.label
                                    }
                                val active = matchingTheme == currentTheme
                                Surface(
                                    modifier = Modifier.clickable {
                                        matchingTheme?.let { theme ->
                                            doHaptic(HapticFeedbackType.LongPress)
                                            com.entropyjournal.ui.theme.ProfileTheme.updateTheme(
                                                context,
                                                theme,
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    color =
                                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border =
                                        BorderStroke(
                                            1.dp,
                                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            else Color(0x47E8B547),
                                        ),
                                    shadowElevation = if (active) 4.dp else 0.dp,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier.size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.linearGradient(choice.colors))
                                        )
                                        Text(
                                            choice.label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                            color =
                                                if (active) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                        if (choice.isNew) {
                                            Text(
                                                "NEU",
                                                fontSize = 9.sp,
                                                color = Color(0xFF9CBF7E),
                                                modifier =
                                                    Modifier.border(
                                                        1.dp,
                                                        Color(0xFF9CBF7E),
                                                        RoundedCornerShape(4.dp),
                                                    )
                                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                            )
                                        }
                                        if (active) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Ausgewählt",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GlassCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.AutoStories,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Schriftart",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        var headingOpen by remember { mutableStateOf(false) }
                        var bodyOpen by remember { mutableStateOf(false) }
                        val selectedHeading = ProfileTheme.currentHeadingFont.value
                        val selectedBody = ProfileTheme.currentBodyFont.value
                        FontSelector(
                            label = "Überschriften",
                            selectedName = selectedHeading.ifBlank { DEFAULT_HEADING_FONT_NAME },
                            expanded = headingOpen,
                            choices = headingFontChoices,
                            fontFamily = ::headingFontFamily,
                            onToggle = {
                                headingOpen = !headingOpen
                                bodyOpen = false
                            },
                            onSelected = { name ->
                                doHaptic(HapticFeedbackType.LongPress)
                                ProfileTheme.updateFonts(context, name, selectedBody)
                                headingOpen = false
                            },
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FontSelector(
                            label = "Fließtext",
                            selectedName = selectedBody.ifBlank { DEFAULT_BODY_FONT_NAME },
                            expanded = bodyOpen,
                            choices = bodyFontChoices,
                            fontFamily = ::bodyFontFamily,
                            onToggle = {
                                bodyOpen = !bodyOpen
                                headingOpen = false
                            },
                            onSelected = { name ->
                                doHaptic(HapticFeedbackType.LongPress)
                                ProfileTheme.updateFonts(context, selectedHeading, name)
                                bodyOpen = false
                            },
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Schriftgröße",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Vorschau: liest die Theme-Typografie, waechst also live mit den Reglern.
                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        RoundedCornerShape(14.dp),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "So sieht eine Überschrift aus",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Und so liest sich der Fließtext in deinen Einträgen — " +
                                    "zieh an den Reglern, bis beides angenehm groß ist.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val headingScale = ProfileTheme.currentHeadingScale.floatValue
                        val bodyScale = ProfileTheme.currentBodyScale.floatValue
                        FontSizeSlider(
                            label = "Überschriften",
                            scale = headingScale,
                            onScaleChange = { ProfileTheme.setHeadingScale(it) },
                            onScaleCommitted = {
                                doHaptic(HapticFeedbackType.LongPress)
                                ProfileTheme.persistFontScales(context)
                            },
                        )
                        FontSizeSlider(
                            label = "Fließtext",
                            scale = bodyScale,
                            onScaleChange = { ProfileTheme.setBodyScale(it) },
                            onScaleCommitted = {
                                doHaptic(HapticFeedbackType.LongPress)
                                ProfileTheme.persistFontScales(context)
                            },
                        )
                        Text(
                            "Schriftart und Größe wirken sofort auf alle Screens.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
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
                                "T\u00f6ne / Haptik",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
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
                                colors = goldenSwitchColors(),
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
                                        "Haptik",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        if (hapticEnabled) "Haptik ist eingeschaltet"
                                        else "Haptik ist ausgeschaltet",
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
                                colors = goldenSwitchColors(),
                            )
                        }

                        // ── Stimmen (TTS) ──
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        var ttsEnabled by remember {
                            mutableStateOf(
                                soundsPrefs.getBoolean(Constants.PREF_TTS_ENABLED, false)
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
                                if (ttsEnabled) {
                                    Icon(
                                        Icons.Rounded.VolumeUp,
                                        "Stimmen",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                } else {
                                    StrikethroughIcon(
                                        icon = Icons.Rounded.VolumeUp,
                                        description = "Stimmen aus",
                                        tint = Color(0xFF666666),
                                        iconSize = 22.dp,
                                        boxSize = 22.dp,
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Stimmen",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        if (ttsEnabled) "Vorlesen ist eingeschaltet"
                                        else "Vorlesen ist ausgeschaltet",
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
                                        .commit()
                                },
                                colors = goldenSwitchColors(),
                            )
                        }

                        if (ttsEnabled) {
                            val currentProvider = uiState.ttsProvider

                            // Favorites system
                            var favorites by remember {
                                mutableStateOf(
                                    soundsPrefs.getString(Constants.PREF_TTS_FAVORITES, "")
                                        ?.split(",")?.filter { it.isNotBlank() }?.toSet()
                                        ?: emptySet()
                                )
                            }
                            var showFavDialog by remember { mutableStateOf(false) }
                            var favDialogVoiceId by remember { mutableStateOf("") }
                            var favDialogVoiceName by remember { mutableStateOf("") }

                            if (showFavDialog) {
                                val isFav = favDialogVoiceId in favorites
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { showFavDialog = false },
                                    title = {
                                        Text(favDialogVoiceName)
                                    },
                                    text = {
                                        Text(
                                            if (isFav) "Favorit entfernen?"
                                            else "Als Favorit markieren?",
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val newFavs = if (isFav) favorites - favDialogVoiceId
                                                else favorites + favDialogVoiceId
                                            favorites = newFavs
                                            soundsPrefs.edit()
                                                .putString(Constants.PREF_TTS_FAVORITES, newFavs.joinToString(","))
                                                .commit()
                                            viewModel.backupFavoritesToDrive(newFavs)
                                            showFavDialog = false
                                        }) {
                                            Text(if (isFav) "Entfernen" else "★ Favorit setzen")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showFavDialog = false }) {
                                            Text("Abbrechen")
                                        }
                                    },
                                )
                            }

                            val dropdownColors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledIndicatorColor = Color.Transparent,
                            )
                            val activeAlpha = 1f
                            val inactiveAlpha = 0.4f

                            // ── ElevenLabs ──
                            val isElevenLabsActive = currentProvider == Constants.TTS_PROVIDER_ELEVENLABS
                            Spacer(modifier = Modifier.height(16.dp))
                            val elevenLabsKey = uiState.elevenLabsApiKey
                            val elevenLabsAvailable = elevenLabsKey.isNotBlank()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isElevenLabsActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else Color.Transparent,
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .alpha(if (isElevenLabsActive) activeAlpha else inactiveAlpha),
                            ) {
                            Text(
                                if (isElevenLabsActive) "\u25C9 ElevenLabs" else "\u25CB ElevenLabs",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isElevenLabsActive)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                if (elevenLabsAvailable) "Beste Qualit\u00e4t \u2022 Cloud \u2022 20 Stimmen"
                                else "API-Schl\u00fcssel erforderlich",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (elevenLabsAvailable) {
                                val voices = Constants.ELEVENLABS_VOICES.filter { it.id.isNotBlank() }
                                val selectedVoiceId = uiState.elevenLabsVoiceId
                                val selectedVoice = voices.find { it.id == selectedVoiceId } ?: voices.first()
                                var voiceExpanded by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.fillMaxWidth().clickable {
                                    voiceExpanded = !voiceExpanded
                                    viewModel.updateTtsProvider(Constants.TTS_PROVIDER_ELEVENLABS)
                                }) {
                                    TextField(
                                        value = selectedVoice.name + (if (selectedVoice.id in favorites) " \u2605" else ""),
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        trailingIcon = { Icon(if (voiceExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = dropdownColors,
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                }
                                if (voiceExpanded) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                        voices.forEach { voice ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                                    onLongClick = { favDialogVoiceId = voice.id; favDialogVoiceName = voice.name; showFavDialog = true },
                                                    onClick = { viewModel.updateElevenLabsVoiceId(voice.id); viewModel.updateTtsProvider(Constants.TTS_PROVIDER_ELEVENLABS); voiceExpanded = false },
                                                ).padding(horizontal = 16.dp, vertical = 12.dp),
                                            ) {
                                                Text(voice.name, color = if (voice.id == selectedVoiceId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                                if (voice.id in favorites) { Text("\u2605", color = Color(0xFFFFB300), fontSize = 16.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                            } // end ElevenLabs Column

                            // ── Google Chirp 3 HD ──
                            val isGoogleActive = currentProvider == Constants.TTS_PROVIDER_GOOGLE
                            Spacer(modifier = Modifier.height(8.dp))
                            val googleKey = uiState.googleTtsApiKey
                            val googleAvailable = googleKey.isNotBlank()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isGoogleActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else Color.Transparent,
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .alpha(if (isGoogleActive) activeAlpha else inactiveAlpha),
                            ) {
                            Text(
                                if (isGoogleActive) "\u25C9 Google Chirp 3 HD" else "\u25CB Google Chirp 3 HD",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isGoogleActive)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                if (googleAvailable) "Sehr hohe Qualit\u00e4t \u2022 1M Zeichen/Monat gratis"
                                else "API-Schl\u00fcssel erforderlich",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (googleAvailable) {
                                val googleVoices = Constants.GOOGLE_TTS_VOICES
                                val selectedGoogleVoiceId = uiState.googleTtsVoice
                                val selectedGoogleVoice = googleVoices.find { it.id == selectedGoogleVoiceId } ?: googleVoices.first()
                                var googleVoiceExpanded by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.fillMaxWidth().clickable {
                                    googleVoiceExpanded = !googleVoiceExpanded
                                    viewModel.updateTtsProvider(Constants.TTS_PROVIDER_GOOGLE)
                                }) {
                                    TextField(
                                        value = selectedGoogleVoice.name + (if (selectedGoogleVoice.id in favorites) " \u2605" else ""),
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        trailingIcon = { Icon(if (googleVoiceExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = dropdownColors,
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                }
                                if (googleVoiceExpanded) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                        googleVoices.forEach { voice ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                                    onLongClick = { favDialogVoiceId = voice.id; favDialogVoiceName = voice.name; showFavDialog = true },
                                                    onClick = { viewModel.updateGoogleTtsVoice(voice.id); viewModel.updateTtsProvider(Constants.TTS_PROVIDER_GOOGLE); googleVoiceExpanded = false },
                                                ).padding(horizontal = 16.dp, vertical = 12.dp),
                                            ) {
                                                Text(voice.name, color = if (voice.id == selectedGoogleVoiceId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                                if (voice.id in favorites) { Text("\u2605", color = Color(0xFFFFB300), fontSize = 16.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                            } // end Google Column

                            // ── Edge TTS ──
                            val isEdgeActive = currentProvider == Constants.TTS_PROVIDER_EDGE
                            Spacer(modifier = Modifier.height(8.dp))
                            val edgeVoices = Constants.EDGE_TTS_VOICES
                            val selectedEdgeVoiceId = uiState.edgeTtsVoice
                            val selectedEdgeVoice = edgeVoices.find { it.id == selectedEdgeVoiceId } ?: edgeVoices.first()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isEdgeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else Color.Transparent,
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .alpha(if (isEdgeActive) activeAlpha else inactiveAlpha),
                            ) {
                            Text(
                                if (isEdgeActive) "\u25C9 Edge TTS" else "\u25CB Edge TTS",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isEdgeActive)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Kostenlos \u2022 Kein Limit \u2022 Deutsch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            var edgeVoiceExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth().clickable {
                                edgeVoiceExpanded = !edgeVoiceExpanded
                                viewModel.updateTtsProvider(Constants.TTS_PROVIDER_EDGE)
                            }) {
                                TextField(
                                    value = selectedEdgeVoice.name + (if (selectedEdgeVoice.id in favorites) " \u2605" else ""),
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    trailingIcon = { Icon(if (edgeVoiceExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = dropdownColors,
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                            if (edgeVoiceExpanded) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    edgeVoices.forEach { voice ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                                onLongClick = { favDialogVoiceId = voice.id; favDialogVoiceName = voice.name; showFavDialog = true },
                                                onClick = { viewModel.updateEdgeTtsVoice(voice.id); viewModel.updateTtsProvider(Constants.TTS_PROVIDER_EDGE); edgeVoiceExpanded = false },
                                            ).padding(horizontal = 16.dp, vertical = 12.dp),
                                        ) {
                                            val voiceColor = if (voice.id == selectedEdgeVoiceId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            if (voice.name.startsWith("\u2605")) {
                                                Text(
                                                    buildAnnotatedString {
                                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) { append("\u2605 ") }
                                                        withStyle(SpanStyle(color = voiceColor)) { append(voice.name.removePrefix("\u2605 ")) }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                )
                                            } else {
                                                Text(voice.name, color = voiceColor, modifier = Modifier.weight(1f))
                                            }
                                            if (voice.id in favorites) { Text("\u2605", color = Color(0xFFFFB300), fontSize = 16.sp) }
                                        }
                                    }
                                }
                            }
                            } // end Edge Column
                        }
                    }
                }

                // Erinnerung / Rückblick
                GlassCard {
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
                                "Erinnerung / R\u00fcckblick",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
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
                                colors = goldenSwitchColors(),
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
                                colors = goldenSwitchColors(),
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
                                colors = goldenSwitchColors(),
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
                                colors = goldenSwitchColors(),
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

                // Daten exportieren
                GlassCard {
                    Column {
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
                                "Daten exportieren",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(
                                onClick = { showExportDialog = true },
                                enabled = !uiState.isExporting,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                            ) {
                                if (uiState.isExporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Wird exportiert...")
                                } else {
                                    Icon(
                                        Icons.Rounded.Description,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tagebucheinträge als PDF exportieren")
                                }
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
                                "KI-Dashboard Profile:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val fixedScenarioNames =
                            listOf(
                                "Zusammenfassung",
                                "R\u00e4ume dein Leben auf",
                                "Selbsterkenntnis",
                                "Pers\u00f6nliche Ziele",
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
                        var previousScenario by remember { mutableIntStateOf(selectedScenario) }
                        var showCustomPromptDialog by remember { mutableStateOf(false) }
                        var showScenarioInfoIndex by remember { mutableIntStateOf(-1) }

                        // Dynamic list of user-defined custom analyses. Kept in sync with
                        // encrypted prefs \u2014 every add/remove/rename reloads from the store so
                        // the UI, the dashboard and the Drive backup stay consistent.
                        var customList by remember {
                            mutableStateOf(CustomAnalysesStore.load(scenarioPrefs))
                        }
                        // Remembers which custom entry the dialog is editing so renames and
                        // prompt saves always write to the right id, even after the list
                        // changes underneath.
                        var editingCustomId by remember { mutableStateOf<String?>(null) }

                        val scenarioNames = fixedScenarioNames + customList.map { it.name }

                        fun selectScenario(index: Int) {
                            doHaptic(HapticFeedbackType.LongPress)
                            previousScenario = currentScenario
                            currentScenario = index
                            // Compose-State-Update faerbt die ganze App live um (alle Screens,
                            // alle Menus, alle Sub-Menus reagieren auf MaterialTheme.colorScheme).
                            com.entropyjournal.ui.theme.ProfileTheme.update(context, index)
                            showScenarioInfoIndex = index
                            if (index >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                                editingCustomId =
                                    customList
                                        .getOrNull(index - Constants.FIRST_CUSTOM_SCENARIO_INDEX)
                                        ?.id
                                showCustomPromptDialog = true
                            }
                            onProfileChanged()
                        }

                        scenarioNames.forEachIndexed { index, name ->
                            val isCustom = index >= Constants.FIRST_CUSTOM_SCENARIO_INDEX
                            val localCustomIndex =
                                index - Constants.FIRST_CUSTOM_SCENARIO_INDEX
                            val customEntry =
                                if (isCustom) customList.getOrNull(localCustomIndex) else null
                            val canDelete = isCustom && localCustomIndex > 0
                            // Onboarding-Profile: icon + accent color per profile (1:1 from
                            // BestJournalAndroid). All custom profiles share the Custom palette.
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
                                        else ->
                                            Text(
                                                "Eigenen Analyse-Fokus festlegen",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                    }
                                }
                                if (isCustom) {
                                    IconButton(
                                        onClick = {
                                            doHaptic(HapticFeedbackType.LongPress)
                                            CustomAnalysesStore.add(scenarioPrefs)
                                            customList = CustomAnalysesStore.load(scenarioPrefs)
                                            viewModel.backupCustomAnalysesToDrive()
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Individuelle Analyse hinzuf\u00fcgen",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    if (canDelete && customEntry != null) {
                                        IconButton(
                                            onClick = {
                                                doHaptic(HapticFeedbackType.LongPress)
                                                val removed =
                                                    CustomAnalysesStore.remove(
                                                        scenarioPrefs,
                                                        customEntry.id,
                                                    )
                                                if (removed) {
                                                    customList =
                                                        CustomAnalysesStore.load(scenarioPrefs)
                                                    // If the active scenario pointed at the
                                                    // deleted entry, fall back to the summary
                                                    // profile so the dashboard does not end
                                                    // up on an empty index.
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
                                                            .apply()
                                                        onProfileChanged()
                                                    }
                                                    viewModel.backupCustomAnalysesToDrive()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Remove,
                                                contentDescription = "Individuelle Analyse entfernen",
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
                            // selectable profile cards above (1:1 from BestJournalAndroid).
                            // All custom profiles share the Custom palette + Science icon.
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
                                    TextButton(onClick = { showScenarioInfoIndex = -1 }) {
                                        Text(
                                            "Verstanden",
                                            color = infoAccent,
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showScenarioInfoIndex = -1
                                        currentScenario = previousScenario
                                        // Auch beim Abbrechen die App-Farbe zurueckdrehen.
                                        com.entropyjournal.ui.theme.ProfileTheme.update(
                                            context,
                                            previousScenario,
                                        )
                                        onProfileChanged()
                                    }) {
                                        Text(
                                            "Abbrechen",
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
                            val savedName =
                                activeEntry?.name
                                    ?: Constants.DEFAULT_CUSTOM_ANALYSIS_NAME
                            var promptText by
                                remember(activeEntryId) { mutableStateOf(savedPrompt) }
                            var titleText by
                                remember(activeEntryId) { mutableStateOf(savedName) }
                            var titleEditing by
                                remember(activeEntryId) { mutableStateOf(false) }
                            val titleFocus = remember(activeEntryId) { FocusRequester() }
                            val focusRequester = remember { FocusRequester() }

                            var preImproveText by remember { mutableStateOf<String?>(null) }
                            var improvedText by remember { mutableStateOf<String?>(null) }
                            var useImproved by remember { mutableStateOf(false) }

                            androidx.compose.runtime.LaunchedEffect(
                                uiState.promptPendingTranscription
                            ) {
                                uiState.promptPendingTranscription?.let { t ->
                                    val separator = if (promptText.isBlank()) "" else " "
                                    val newFull = promptText + separator + t.text
                                    promptText = newFull
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

                            androidx.compose.runtime.LaunchedEffect(
                                uiState.promptPendingImprovement
                            ) {
                                uiState.promptPendingImprovement?.let { imp ->
                                    preImproveText = promptText
                                    improvedText = imp
                                    promptText = imp
                                    useImproved = true
                                    viewModel.consumePromptImprovement()
                                }
                            }

                            val micPermissionLauncher =
                                rememberLauncherForActivityResult(
                                    ActivityResultContracts.RequestPermission()
                                ) { granted ->
                                    if (granted) viewModel.togglePromptRecording()
                                }

                            var showClearConfirm by remember { mutableStateOf(false) }
                            if (showClearConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showClearConfirm = false },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    title = {
                                        Text(
                                            "Text löschen?",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Die Individuelle Analyse wird unwiderruflich gelöscht. Möchtest du wirklich fortfahren?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant,
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
                                                "Ja",
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showClearConfirm = false }) {
                                            Text(
                                                "Nein",
                                                color =
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
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
                                properties =
                                    DialogProperties(usePlatformDefaultWidth = false),
                                containerColor = MaterialTheme.colorScheme.surface,
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
                                                    Modifier.weight(1f)
                                                        .focusRequester(titleFocus),
                                                textStyle =
                                                    MaterialTheme.typography.titleLarge.copy(
                                                        color =
                                                            MaterialTheme.colorScheme.onSurface
                                                    ),
                                                cursorBrush =
                                                    SolidColor(
                                                        MaterialTheme.colorScheme.primary
                                                    ),
                                            )
                                            androidx.compose.runtime.LaunchedEffect(
                                                activeEntryId
                                            ) {
                                                titleFocus.requestFocus()
                                            }
                                            IconButton(
                                                onClick = {
                                                    doHaptic(HapticFeedbackType.LongPress)
                                                    CustomAnalysesStore.rename(
                                                        scenarioPrefs,
                                                        activeEntryId,
                                                        titleText,
                                                    )
                                                    customList =
                                                        CustomAnalysesStore.load(scenarioPrefs)
                                                    titleText =
                                                        customList
                                                            .firstOrNull { it.id == activeEntryId }
                                                            ?.name
                                                            ?: titleText
                                                    titleEditing = false
                                                    viewModel.backupCustomAnalysesToDrive()
                                                },
                                                modifier = Modifier.size(32.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Name übernehmen",
                                                    tint =
                                                        MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        } else {
                                            Text(
                                                titleText,
                                                style = MaterialTheme.typography.titleLarge,
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
                                                        contentDescription = "Umbenennen",
                                                        tint =
                                                            MaterialTheme.colorScheme.primary,
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
                                            "Was ist dir besonders wichtig? Worauf soll sich die KI bei der Analyse deiner Tagebucheintr\u00e4ge konzentrieren?",
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
                                                androidx.compose.foundation
                                                    .rememberScrollState()
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
                                                        color =
                                                            MaterialTheme.colorScheme.onSurface
                                                    ),
                                                cursorBrush =
                                                    SolidColor(
                                                        MaterialTheme.colorScheme.primary
                                                    ),
                                                decorationBox = { innerTextField ->
                                                    if (promptText.isEmpty()) {
                                                        val isDark = LocalIsDarkTheme.current
                                                        Text(
                                                            "z.B. Fokussiere dich auf meine Schlafqualit\u00e4t und Stresslevel. Zeige mir Muster in meiner Ern\u00e4hrung. Analysiere, wie sich meine Stimmung \u00fcber die Woche ver\u00e4ndert. Finde heraus, wann ich am produktivsten bin und was mich blockiert.\n\nJe gr\u00fcndlicher du beschreibst was dein Fokus ist, desto besser werden die Ergebnisse.",
                                                            style =
                                                                MaterialTheme.typography
                                                                    .bodyMedium,
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
                                                        contentDescription = "Text l\u00f6schen",
                                                        tint =
                                                            MaterialTheme.colorScheme
                                                                .onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
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
                                                            contentDescription = "Schreiben",
                                                            modifier = Modifier.size(28.dp),
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Schreiben",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(32.dp))

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
                                                    "Einsprechen",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        val stateLabel =
                                            when (uiState.promptRecState) {
                                                PromptRecState.TRANSCRIBING ->
                                                    "Wird transkribiert\u2026"
                                                PromptRecState.IMPROVING ->
                                                    "Wird verbessert\u2026"
                                                else -> null
                                            }
                                        if (stateLabel != null) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                CircularProgressIndicator(
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
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Transkribiert mit ${uiState.promptTranscriptionModel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color =
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }

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
                                                        "Text verbessern",
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
                                                FilterChip(
                                                    selected = !useImproved,
                                                    onClick = {
                                                        preImproveText?.let { promptText = it }
                                                        useImproved = false
                                                    },
                                                    label = { Text("Original") },
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                FilterChip(
                                                    selected = useImproved,
                                                    onClick = {
                                                        improvedText?.let { promptText = it }
                                                        useImproved = true
                                                    },
                                                    label = { Text("Verbessert") },
                                                )
                                            }
                                        }

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
                                                    CustomAnalysesStore.rename(
                                                        scenarioPrefs,
                                                        activeEntryId,
                                                        titleText,
                                                    )
                                                }
                                                CustomAnalysesStore.setPrompt(
                                                    scenarioPrefs,
                                                    activeEntryId,
                                                    promptText,
                                                )
                                                customList =
                                                    CustomAnalysesStore.load(scenarioPrefs)
                                                // Refresh the local save timestamp on EVERY Save,
                                                // even if nothing changed, so both devices agree
                                                // that this was the last authoritative save and
                                                // the same device does not later pull an
                                                // identical Drive copy as "newer".
                                                scenarioPrefs
                                                    .edit()
                                                    .putLong(
                                                        "custom_prompt_saved_at",
                                                        System.currentTimeMillis(),
                                                    )
                                                    .apply()
                                                // Always push to Drive on save — even if the text
                                                // did not change — so all other devices pick up
                                                // the refreshed timestamp and re-download if
                                                // their local copy was stale.
                                                viewModel.backupCustomAnalysesToDrive()
                                            }
                                            viewModel.clearPromptVoiceState()
                                            showCustomPromptDialog = false
                                            editingCustomId = null
                                            titleEditing = false
                                        }
                                    ) {
                                        Text("Speichern", color = MaterialTheme.colorScheme.primary)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Mic,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Aufnahme",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Maximale Aufnahmedauer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 5, 10).forEach { minutes ->
                                val active = uiState.maxRecordingDuration == minutes
                                Surface(
                                    onClick = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        viewModel.updateMaxRecordingDuration(minutes)
                                    },
                                    shape = RoundedCornerShape(50),
                                    color =
                                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border =
                                        BorderStroke(
                                            1.dp,
                                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            else Color(0x47E8B547),
                                        ),
                                ) {
                                    Text(
                                        "$minutes Min",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        color =
                                            if (active) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "KI-Textverbesserung",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "Standard für neue Einträge",
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
                                colors = goldenSwitchColors(),
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
                                    "Dashboard-Auto-Update",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "Nach jedem neuen Eintrag",
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
                                colors = goldenSwitchColors(),
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
                                    "Längere Version",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (uiState.verboseDashboard)
                                        "Ausführliche Version"
                                    else "Standardversion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.verboseDashboard,
                                onCheckedChange = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.updateVerboseDashboard(it)
                                },
                                colors = goldenSwitchColors(),
                            )
                        }
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
                                "Sicherheit",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
                                        "Fingerabdruck / PIN",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Sperrt nach 60 s im Hintergrund",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.biometricLock,
                                onCheckedChange = { enabled ->
                                    doHaptic(HapticFeedbackType.LongPress)
                                    val activity = context as? com.entropyjournal.MainActivity
                                    if (activity != null) {
                                        activity.showBiometricPrompt {
                                            viewModel.updateBiometricLock(enabled)
                                        }
                                    } else {
                                        viewModel.updateBiometricLock(enabled)
                                    }
                                },
                                colors = goldenSwitchColors(),
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
                        Spacer(modifier = Modifier.height(10.dp))
                        ApiKeyField(
                            label = "ElevenLabs API-Key",
                            value = uiState.elevenLabsApiKey,
                            onValueChange = { viewModel.updateElevenLabsApiKey(it) },
                            requireBiometric = uiState.biometricLock,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ApiKeyField(
                            label = "Google Cloud TTS API-Key",
                            value = uiState.googleTtsApiKey,
                            onValueChange = { viewModel.updateGoogleTtsApiKey(it) },
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

                // 6. Extras
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Lightbulb,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Extras",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Schreibimpuls des Tages",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Zeigt jeden Tag eine kleine Frage als Inspiration zum Schreiben",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.dailyPromptEnabled,
                                onCheckedChange = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.updateDailyPromptEnabled(it)
                                },
                                colors = goldenSwitchColors(),
                            )
                        }
                    }
                }

                GlassCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Feedback,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Feedback",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Geht an dev.app.support@gmail.com — du erhältst eine Kopie",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showExportDialog = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                            ) {
                                Text("Als PDF exportieren", fontSize = 13.sp)
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                                onClick = {
                                    val intent =
                                        android.content.Intent(
                                            android.content.Intent.ACTION_SENDTO,
                                            android.net.Uri.parse(
                                                "mailto:dev.app.support@gmail.com?subject=Entropy%20Journal%20Feedback"
                                            ),
                                        )
                                    runCatching { context.startActivity(intent) }
                                        .onFailure {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Keine E-Mail-App verfügbar.",
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                }
                            ) {
                                Text("Feedback senden", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 7. Ueber die App
                GlassCard(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterHorizontally),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "\u00dcber die App",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            // Invisible counterbalance for icon+spacer so text is visually centered
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Entropy Journal v0.21.2 - 18.07.2026 22:48 Uhr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Dein persönliches KI-Tagebuch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "© Frank Barwandt",
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
                    title = { Text("Abmelden?", color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        Text(
                            "M\u00f6chtest du dich wirklich abmelden?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { doHaptic(HapticFeedbackType.LongPress); viewModel.signOut(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        ) {
                            Text("Abmelden")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { doHaptic(HapticFeedbackType.LongPress); viewModel.showLogoutDialog(false) }) {
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
                "Hell",
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
                "Dunkel",
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
        StrikethroughIcon(
            icon = Icons.Rounded.VolumeUp,
            description = "Ton aus",
            tint = if (!isEnabled) Color(0xFFEF4444) else mutedGray,
            iconSize = offSize,
            boxSize = 24.dp,
        )
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
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Rounded.Vibration,
                "Haptik an",
                tint = if (isEnabled) activeColor else mutedGray,
                modifier = Modifier.size(onSize),
            )
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.height(16.dp).width(1.dp),
        )
        StrikethroughIcon(
            icon = Icons.Rounded.Vibration,
            description = "Haptik aus",
            tint = if (!isEnabled) Color(0xFFEF4444) else mutedGray,
            iconSize = offSize,
            boxSize = 24.dp,
        )
    }
}

@Composable
private fun StrikethroughIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    boxSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(boxSize)) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(iconSize))
        androidx.compose.foundation.Canvas(modifier = Modifier.size(iconSize)) {
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f),
                strokeWidth = size.minDimension * 0.12f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SettingsSunMoonIcon(isDark: Boolean, isActive: Boolean = true) {
    val glowYellow = Color(0xFFFFD54F)
    val mutedGray = Color(0xFF666666)
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
                "Sonne",
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
                "Mond",
                tint = moonTint,
                modifier = Modifier.size(moonSize),
            )
        }
    }
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
