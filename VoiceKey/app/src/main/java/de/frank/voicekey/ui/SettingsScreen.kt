package de.frank.voicekey.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.voicekey.BuildConfig
import de.frank.voicekey.data.WakeLang
import de.frank.voicekey.data.WakeWord
import de.frank.voicekey.ui.theme.VK

/** Sheet-Zustand: neues Wort anlegen oder bestehendes bearbeiten. */
private sealed interface SheetMode {
    data class Add(val lang: WakeLang) : SheetMode

    data class Edit(val word: WakeWord) : SheetMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onToggleService: (Boolean) -> Unit,
    onTestTrigger: () -> Unit,
    onEndAssistant: () -> Unit,
    onToggleFavorit: (String) -> Unit,
    onAddWord: (String, WakeLang, Boolean) -> Unit,
    onUpdateWord: (String, String) -> Unit,
    onRemoveWord: (String) -> Unit,
    onRetryModels: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    var sheetMode by
        rememberSaveable(stateSaver = sheetSaver()) { mutableStateOf<SheetMode?>(null) }
    // Navigation: false = Ziel-Uebersicht (die Pattern-Karten), true = Detail-Untermenue eines
    // Ziels (Dienst + Wake-Woerter). Bewusst lokaler State statt navigation-compose — die App hat
    // genau zwei Ebenen, und das bestehende Muster (MainActivity steuert Setup/Settings ueber
    // State)
    // bleibt konsistent. Spaeter koennen weitere Ziele als weitere Karten in der Uebersicht stehen.
    var targetOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 30.dp)
    ) {
        if (targetOpen) {
            // --- Detail-Untermenue fuer das Ziel "ChatGPT Voice" ---
            TargetDetailHeader(title = "ChatGPT Voice", onBack = { targetOpen = false })
            ServiceCard(state, onToggleService, onRetryModels)
            Spacer(Modifier.height(8.dp))
            WordSection(
                lang = WakeLang.DE,
                words = state.words.filter { it.lang == WakeLang.DE },
                onToggleFavorit = onToggleFavorit,
                onEdit = { sheetMode = SheetMode.Edit(it) },
                onAdd = { sheetMode = SheetMode.Add(WakeLang.DE) },
            )
            WordSection(
                lang = WakeLang.EN,
                words = state.words.filter { it.lang == WakeLang.EN },
                onToggleFavorit = onToggleFavorit,
                onEdit = { sheetMode = SheetMode.Edit(it) },
                onAdd = { sheetMode = SheetMode.Add(WakeLang.EN) },
            )
            Spacer(Modifier.height(8.dp))
            HintCard()
        } else {
            // --- Ziel-Uebersicht: die Pattern-Karte(n) ---
            AppHeader(onOpenSetup)
            HeroCard(
                onOpenTarget = { targetOpen = true },
                onTestTrigger = onTestTrigger,
                onEndAssistant = onEndAssistant,
            )
            if (!state.targetAvailable) TargetWarningCard()
        }
        VersionFooter()
    }

    // Zurueck-Geste im Detail: zurueck zur Uebersicht, NICHT die App schliessen.
    BackHandler(enabled = targetOpen) { targetOpen = false }

    sheetMode?.let { mode ->
        WordSheet(
            mode = mode,
            onDismiss = { sheetMode = null },
            onSave = { text, favorit ->
                when (mode) {
                    is SheetMode.Add -> onAddWord(text, mode.lang, favorit)
                    is SheetMode.Edit -> onUpdateWord(mode.word.id, text)
                }
                sheetMode = null
            },
            onDelete = {
                (mode as? SheetMode.Edit)?.let { onRemoveWord(it.word.id) }
                sheetMode = null
            },
        )
    }
}

/* Saver: Sheet-Zustand ueberlebt Rotation nicht zwingend — bewusst simpel (null nach Rotation ist ok). */
private fun sheetSaver() =
    androidx.compose.runtime.saveable.Saver<SheetMode?, String>(save = { null }, restore = { null })

@Composable
private fun AppHeader(onOpenSetup: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(VK.Primary, Color(0xFFB45309)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Mic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text("VoiceKey", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenSetup) {
            Icon(Icons.Rounded.Settings, contentDescription = "Einrichtung", tint = VK.TextMid)
        }
    }
}

/** Kopfzeile des Ziel-Untermenues: Zurueck-Pfeil + Ziel-Name. */
@Composable
private fun TargetDetailHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Zurück zur Übersicht",
                tint = VK.TextMid,
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun HeroCard(
    onOpenTarget: () -> Unit,
    onTestTrigger: () -> Unit,
    onEndAssistant: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .drawBehind {
                    // Basis: linearer Verlauf surface2 -> surface1 (Mockup 160°)
                    drawRect(Brush.linearGradient(listOf(VK.Surface2, VK.Surface1)))
                    // Oranger Schein oben rechts
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(VK.Primary.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(size.width * 0.85f, -size.height * 0.2f),
                            radius = size.width * 0.85f,
                        )
                    )
                }
                .border(1.dp, VK.Primary.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                // Ganze Karte tippbar -> Detail-Untermenue. Die zwei Buttons unten haben ein
                // eigenes clickable und schlucken ihren Tap, propagieren also NICHT hierher
                // (Compose-Verhalten) — genau wie gewuenscht.
                .clickable(onClick = onOpenTarget)
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(VK.Primary, CircleShape))
            Spacer(Modifier.size(6.dp))
            Text(
                "AKTIVES ZIEL",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = VK.PrimarySoft,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingOrb()
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ChatGPT Voice",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Antippen für Wörter & Dienst — die Knöpfe starten/beenden direkt",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = VK.TextLo)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // Not-Aus-Rückfallebene: beendet die ChatGPT-Voice-Session WIRKLICH
            // (Rück-Geste/Wegwischen lassen sie sonst im Hintergrund weiterlaufen).
            Button(
                onClick = onEndAssistant,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = VK.Surface3,
                        contentColor = Color(0xFFF87171),
                    ),
                shape = RoundedCornerShape(100.dp),
            ) {
                Icon(
                    Icons.Rounded.StopCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Voice beenden",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.size(10.dp))
            Button(
                onClick = onTestTrigger,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = VK.PrimaryContainer,
                        contentColor = VK.OnPrimaryContainer,
                    ),
                shape = RoundedCornerShape(100.dp),
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Jetzt testen",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Orange Kugel mit zwei pulsierenden Ringen — 1:1 die Mockup-Animation (2,4 s, versetzt). */
@Composable
private fun PulsingOrb() {
    val transition = rememberInfiniteTransition(label = "orb")
    val ring1 by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "ring1",
        )
    val ring2 by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    tween(2400, easing = LinearEasing),
                    initialStartOffset = StartOffset(1200),
                ),
            label = "ring2",
        )

    Box(Modifier.size(74.dp), contentAlignment = Alignment.Center) {
        listOf(ring1, ring2).forEach { value ->
            val p = (value / 0.7f).coerceAtMost(1f)
            val scale = 0.78f + (1.25f - 0.78f) * p
            val alpha = 0.9f * (1f - p)
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .border(2.dp, VK.Primary.copy(alpha = 0.5f), CircleShape)
            )
        }
        Box(
            Modifier.size(60.dp).drawBehind {
                // Glow
                drawCircle(
                    Brush.radialGradient(
                        listOf(VK.Primary.copy(alpha = 0.55f), Color.Transparent),
                        radius = size.width * 0.95f,
                    ),
                    radius = size.width * 0.95f,
                )
                // Kugel mit Licht oben links (Mockup: 32 % / 28 %)
                drawCircle(
                    Brush.radialGradient(
                        colorStops =
                            arrayOf(
                                0f to VK.OrbLight,
                                0.42f to VK.OrbMid,
                                0.75f to VK.OrbDeep,
                                1f to VK.OrbDark,
                            ),
                        center = Offset(size.width * 0.32f, size.height * 0.28f),
                        radius = size.width * 0.85f,
                    ),
                    radius = size.width / 2f,
                )
            }
        )
    }
}

@Composable
private fun TargetWarningCard() {
    Spacer(Modifier.height(14.dp))
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = VK.Surface1,
        border =
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = Color(0xFFF87171),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                "ChatGPT-Voice-Einstieg nicht gefunden. Vermutlich hat ein ChatGPT-Update die " +
                    "interne Activity umbenannt — bitte melden, dann ermitteln wir den neuen Namen.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ServiceCard(
    state: UiState,
    onToggleService: (Boolean) -> Unit,
    onRetryModels: () -> Unit,
) {
    val modelsReady = state.models.values.all { it is ModelUiState.Ready }
    val downloading = state.models.values.filterIsInstance<ModelUiState.Downloading>()
    val anyError = state.models.values.any { it is ModelUiState.Error }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = VK.Surface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, VK.OutlineSoft),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(VK.PrimaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = VK.PrimarySoft,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Wake-Word-Dienst",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    ServiceStatusLine(
                        state.serviceEnabled,
                        modelsReady,
                        downloading.isNotEmpty(),
                        anyError,
                    )
                }
                Switch(
                    checked = state.serviceEnabled,
                    onCheckedChange = onToggleService,
                    colors =
                        SwitchDefaults.colors(
                            checkedTrackColor = VK.Primary,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = VK.Surface3,
                            uncheckedThumbColor = VK.TextLo,
                            uncheckedBorderColor = VK.Outline,
                        ),
                )
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = VK.OutlineSoft)
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (modelsReady) Icons.Rounded.DownloadDone else Icons.Rounded.Downloading,
                    contentDescription = null,
                    tint = VK.TextLo,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    when {
                        modelsReady -> "Modelle DE + EN"
                        downloading.isNotEmpty() ->
                            "Modelle laden… ${downloading.minOf { it.percent }} %"
                        anyError -> "Modell-Download fehlgeschlagen"
                        else -> "Modelle fehlen noch"
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = if (anyError) Color(0xFFF87171) else VK.TextMid,
                )
                Spacer(Modifier.size(16.dp))
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = VK.TextLo,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    if (state.batteryUnrestricted) "Akku uneingeschränkt" else "Akku eingeschränkt",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = VK.TextMid,
                )
            }
            if (anyError) {
                TextButton(onClick = onRetryModels) {
                    Text("Download erneut versuchen", color = VK.PrimarySoft)
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusLine(
    enabled: Boolean,
    modelsReady: Boolean,
    downloading: Boolean,
    error: Boolean,
) {
    val (text, color, blink) =
        when {
            !enabled -> Triple("Gestoppt", VK.TextLo, false)
            downloading -> Triple("Modelle werden geladen…", VK.PrimarySoft, true)
            error -> Triple("Modell-Fehler, Download wiederholen", Color(0xFFF87171), false)
            modelsReady -> Triple("Lauscht im Hintergrund", VK.Green, true)
            else -> Triple("Wartet auf Modelle", VK.PrimarySoft, false)
        }
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse),
            label = "liveAlpha",
        )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(7.dp)
                .graphicsLayer { this.alpha = if (blink) alpha else 1f }
                .background(color, CircleShape)
        )
        Spacer(Modifier.size(7.dp))
        Text(text, fontSize = 12.5.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun WordSection(
    lang: WakeLang,
    words: List<WakeWord>,
    onToggleFavorit: (String) -> Unit,
    onEdit: (WakeWord) -> Unit,
    onAdd: () -> Unit,
) {
    val (badgeBg, badgeFg, badgeBorder) =
        when (lang) {
            WakeLang.DE -> Triple(VK.DeBg, VK.DeFg, VK.DeBorder)
            WakeLang.EN -> Triple(VK.EnBg, VK.EnFg, VK.EnBorder)
        }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp)
                .background(badgeBg, CircleShape)
                .border(1.dp, badgeBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(lang.name, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = badgeFg)
        }
        Spacer(Modifier.size(10.dp))
        Text(lang.label, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        Text(
            "${words.size} ${if (words.size == 1) "Wort" else "Wörter"}",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        // Stabiler key nicht noetig: FlowRow ist kein Lazy-Layout, Liste ist klein.
        words.forEach { word ->
            WordChip(
                word = word,
                onClick = { onToggleFavorit(word.id) },
                onLongClick = { onEdit(word) },
            )
        }
        AddChip(onAdd)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordChip(word: WakeWord, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    val background = if (word.favorit) VK.PrimaryContainer else Color.Transparent
    val borderColor = if (word.favorit) VK.Primary.copy(alpha = 0.6f) else VK.Outline
    val textColor = if (word.favorit) VK.OnPrimaryContainer else VK.TextMid

    Row(
        Modifier.clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (word.favorit) Icons.Rounded.Star else Icons.Rounded.StarOutline,
            contentDescription = if (word.favorit) "Favorit" else "Kein Favorit",
            tint = if (word.favorit) VK.PrimarySoft else VK.TextLo,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.size(7.dp))
        Text(
            word.text,
            fontSize = 13.5.sp,
            color = textColor,
            fontWeight = if (word.favorit) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun AddChip(onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    val dashColor = VK.Primary.copy(alpha = 0.45f)
    Row(
        Modifier.clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    style =
                        Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f)),
                        ),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = VK.PrimarySoft,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.size(7.dp))
        Text(
            "Neues Wort",
            fontSize = 13.5.sp,
            color = VK.PrimarySoft,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HintCard() {
    Surface(shape = RoundedCornerShape(18.dp), color = VK.Surface1) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = VK.TextLo,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                "Solange der Dienst lauscht, zeigt Android dauerhaft den grünen Mikrofon-Punkt. " +
                    "Das ist normal und gewollt. Tipp auf ein Wort: Favorit umschalten. Lange drücken: ändern oder löschen.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun VersionFooter() {
    Text(
        "VoiceKey v${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}",
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = VK.TextLo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordSheet(
    mode: SheetMode,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val isEdit = mode is SheetMode.Edit
    val lang =
        when (mode) {
            is SheetMode.Add -> mode.lang
            is SheetMode.Edit -> mode.word.lang
        }
    var text by rememberSaveable {
        mutableStateOf(if (mode is SheetMode.Edit) mode.word.text else "")
    }
    var favorit by rememberSaveable {
        mutableStateOf(if (mode is SheetMode.Edit) mode.word.favorit else true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = VK.Surface2) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 34.dp)) {
            Text(
                when {
                    isEdit -> "Wake-Wort ändern"
                    lang == WakeLang.DE -> "Neues deutsches Wake-Wort"
                    else -> "Neues englisches Wake-Wort"
                },
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontSize = 19.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tippe das Wort genau so ein, wie du es sprichst.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = VK.TextMid,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Wake-Wort") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Leeren",
                                tint = VK.TextLo,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VK.Primary,
                        unfocusedBorderColor = VK.Outline,
                        focusedLabelColor = VK.PrimarySoft,
                        unfocusedLabelColor = VK.TextLo,
                        cursorColor = VK.Primary,
                        focusedTextColor = VK.TextHi,
                        unfocusedTextColor = VK.TextHi,
                    ),
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "2 bis 4 Silben funktionieren am zuverlässigsten",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp),
            )
            Spacer(Modifier.height(20.dp))

            if (!isEdit) {
                Surface(shape = RoundedCornerShape(16.dp), color = VK.Surface1) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = VK.PrimarySoft,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Als Favorit setzen",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = VK.TextHi,
                            )
                            Text(
                                "Favoriten sind aktiv und werden erkannt",
                                style =
                                    androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = favorit,
                            onCheckedChange = { favorit = it },
                            colors =
                                SwitchDefaults.colors(
                                    checkedTrackColor = VK.Primary,
                                    checkedThumbColor = Color.White,
                                    uncheckedTrackColor = VK.Surface3,
                                    uncheckedThumbColor = VK.TextLo,
                                    uncheckedBorderColor = VK.Outline,
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Row(Modifier.fillMaxWidth()) {
                if (isEdit) {
                    TextButton(onClick = onDelete) {
                        Text(
                            "Löschen",
                            color = Color(0xFFF87171),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(
                        "Abbrechen",
                        color = VK.PrimarySoft,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.size(10.dp))
                Button(
                    onClick = { if (text.isNotBlank()) onSave(text, favorit) },
                    enabled = text.isNotBlank(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = VK.Primary,
                            contentColor = VK.OnPrimary,
                            disabledContainerColor = VK.Surface3,
                            disabledContentColor = VK.TextLo,
                        ),
                    shape = RoundedCornerShape(100.dp),
                ) {
                    Text(
                        "Speichern",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
