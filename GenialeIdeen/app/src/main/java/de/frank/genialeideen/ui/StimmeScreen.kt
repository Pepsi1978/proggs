package de.frank.genialeideen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.audio.MicRecorder
import de.frank.genialeideen.audio.VoiceSampleScript
import de.frank.genialeideen.tts.ClonedVoice
import de.frank.genialeideen.tts.QwenVoiceDirectory
import de.frank.genialeideen.tts.QwenVoiceEnrollment
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Semantisch
import kotlinx.coroutines.launch

/**
 * Baustein E: eine eigene Stimme aufnehmen, benennen, auswählen und wieder löschen.
 */
@Composable
fun StimmeScreen(
    viewModel: IdeenViewModel,
    mikrofonErlaubt: Boolean,
    aufMikrofonFragen: () -> Unit,
    aufZurueck: () -> Unit,
) {
    val gold = LocalGold.current
    val context = LocalContext.current
    val bereich = rememberCoroutineScope()
    val settings = viewModel.settings
    val theme by viewModel.theme.collectAsState()

    val recorder = remember { MicRecorder(context) }
    val verzeichnis = remember { QwenVoiceDirectory() }
    val anmeldung = remember { QwenVoiceEnrollment() }

    var stimmen by remember { mutableStateOf<List<ClonedVoice>>(emptyList()) }
    var laeuftAufnahme by remember { mutableStateOf(false) }
    var laedt by remember { mutableStateOf(false) }
    var aufgenommen by remember { mutableStateOf<ByteArray?>(null) }
    var name by remember { mutableStateOf("") }
    var gewaehlt by remember { mutableStateOf(settings.qwenTtsVoiceId) }

    val skript = remember { VoiceSampleScript.script(VoiceSampleScript.fallback) }

    LaunchedEffect(Unit) {
        if (settings.qwenTtsApiKey.isNotBlank()) {
            runCatching { verzeichnis.list(settings.qwenTtsApiKey) }
                .onSuccess { liste -> stimmen = liste }
                .onFailure { fehler ->
                    viewModel.zeige(
                        Meldung("Die eigenen Stimmen liessen sich nicht laden: ${fehler.message}", istFehler = true),
                    )
                }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(gold.hintergrund).imePadding(),
    ) {
        IdeenKopfleiste(
            titel = "Meine Stimme",
            themeWahl = theme,
            aufEinstellungen = aufZurueck,
            voran = {
                Box(
                    modifier = Modifier.size(38.dp).druckEffekt(aufZurueck),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = gold.primaer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (settings.qwenTtsApiKey.isBlank()) {
                GoldKarte(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Für die eigene Stimme fehlt der DashScope-Schlüssel",
                            style = MaterialTheme.typography.titleSmall,
                            color = gold.textPrimaer,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Trag ihn in den Einstellungen unter Vorlesen ein, dann geht es hier weiter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = gold.textGedaempft,
                        )
                    }
                }
            }

            GoldKarte(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Lies diesen Text ruhig vor",
                        style = MaterialTheme.typography.titleSmall,
                        color = gold.textPrimaer,
                    )
                    Spacer(Modifier.height(10.dp))
                    skript.forEach { zeile ->
                        Text(
                            zeile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = gold.textGedaempft,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Rund 50 Sekunden reichen. Sprich in normaler Lautstärke, ohne Hall.",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.textGedaempft,
                    )
                    Spacer(Modifier.height(14.dp))
                    GoldKnopf(
                        text = when {
                            laeuftAufnahme -> "Aufnahme beenden"
                            aufgenommen != null -> "Noch einmal aufnehmen"
                            else -> "Aufnahme starten"
                        },
                        aufTipp = {
                            if (!mikrofonErlaubt) {
                                aufMikrofonFragen()
                                return@GoldKnopf
                            }
                            if (laeuftAufnahme) {
                                bereich.launch {
                                    aufgenommen = recorder.stop()
                                    laeuftAufnahme = false
                                }
                            } else {
                                aufgenommen = null
                                laeuftAufnahme = recorder.start(bereich, MicRecorder.CLONING_SAMPLE_RATE)
                                if (!laeuftAufnahme) {
                                    viewModel.zeige(
                                        Meldung("Die Aufnahme ging nicht los — prüf die Mikrofon-Freigabe.", istFehler = true),
                                    )
                                }
                            }
                        },
                    )
                }
            }

            if (aufgenommen != null) {
                GoldKarte(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Wie soll die Stimme heissen?",
                            style = MaterialTheme.typography.titleSmall,
                            color = gold.textPrimaer,
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(gold.eingabefeld)
                                .border(1.dp, gold.rahmen, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                        ) {
                            if (name.isEmpty()) {
                                Text(
                                    "zum Beispiel: Meine Stimme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = gold.textGedaempft,
                                )
                            }
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = gold.textPrimaer),
                                cursorBrush = SolidColor(gold.primaer),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        GoldKnopf(
                            text = "Stimme anlegen",
                            laedt = laedt,
                            aktiviert = name.isNotBlank() && settings.qwenTtsApiKey.isNotBlank(),
                            aufTipp = {
                                val daten = aufgenommen ?: return@GoldKnopf
                                laedt = true
                                bereich.launch {
                                    runCatching {
                                        anmeldung.create(settings.qwenTtsApiKey, name.trim(), daten)
                                    }.onSuccess { id ->
                                        settings.qwenVoiceNames = settings.qwenVoiceNames + (id to name.trim())
                                        settings.qwenTtsVoiceId = id
                                        gewaehlt = id
                                        aufgenommen = null
                                        name = ""
                                        stimmen = runCatching { verzeichnis.list(settings.qwenTtsApiKey) }
                                            .getOrDefault(stimmen)
                                        viewModel.zeige(Meldung("Die Stimme steht bereit."))
                                    }.onFailure { fehler ->
                                        viewModel.zeige(
                                            Meldung(
                                                "Die Stimme konnte nicht angelegt werden: ${fehler.message}",
                                                istFehler = true,
                                            ),
                                        )
                                    }
                                    laedt = false
                                }
                            },
                        )
                    }
                }
            }

            if (stimmen.isEmpty()) {
                Leerzustand(
                    symbol = "🎙️",
                    ueberschrift = "Noch keine eigene Stimme",
                    satz = "Nimm oben eine auf — danach liest die App mit deiner Stimme vor.",
                )
            } else {
                Text(
                    "Meine Stimmen",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                )
                stimmen.forEach { eintrag ->
                    val anzeigeName = settings.qwenVoiceNames[eintrag.id] ?: eintrag.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (eintrag.id == gewaehlt) gold.primaer.copy(alpha = 0.14f) else gold.flaeche,
                            )
                            .border(
                                1.dp,
                                if (eintrag.id == gewaehlt) gold.primaer else gold.rahmen,
                                RoundedCornerShape(14.dp),
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).druckEffekt {
                                settings.qwenTtsVoiceId = eintrag.id
                                settings.ttsProvider = de.frank.genialeideen.tts.TtsProvider.QWEN_CLONE.id
                                gewaehlt = eintrag.id
                            },
                        ) {
                            Text(anzeigeName, style = MaterialTheme.typography.bodyMedium, color = gold.textPrimaer)
                            Text(
                                eintrag.createdAt,
                                style = MaterialTheme.typography.labelSmall,
                                color = gold.textGedaempft,
                            )
                        }
                        Box(
                            modifier = Modifier.size(34.dp).druckEffekt {
                                bereich.launch {
                                    runCatching { anmeldung.delete(settings.qwenTtsApiKey, eintrag.id) }
                                        .onSuccess {
                                            stimmen = stimmen.filterNot { it.id == eintrag.id }
                                            if (gewaehlt == eintrag.id) {
                                                settings.qwenTtsVoiceId = ""
                                                gewaehlt = ""
                                            }
                                            viewModel.zeige(Meldung("Stimme gelöscht."))
                                        }
                                        .onFailure { fehler ->
                                            viewModel.zeige(
                                                Meldung("Löschen ging nicht: ${fehler.message}", istFehler = true),
                                            )
                                        }
                                }
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Stimme löschen",
                                tint = Semantisch.fehler,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
