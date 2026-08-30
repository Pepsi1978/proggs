package de.frank.genialeideen.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.genialeideen.BuildConfig
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.tts.TtsCatalog
import de.frank.genialeideen.tts.TtsProvider
import de.frank.genialeideen.tts.VoiceGender
import de.frank.genialeideen.ui.theme.IdeenSchriftFest
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Semantisch

@Composable
fun EinstellungenScreen(
    viewModel: IdeenViewModel,
    aufZurueck: () -> Unit,
    aufDiagnose: () -> Unit,
    aufEigeneStimme: () -> Unit,
    aufAnmelden: () -> Unit,
    aufExport: () -> Unit,
    aufImport: () -> Unit,
    aufAppSperreUmschalten: (Boolean) -> Unit,
    aufSeiteOeffnen: (String?) -> Unit,
) {
    val gold = LocalGold.current
    val zwischenablage = LocalClipboardManager.current
    val settings = viewModel.settings
    val theme by viewModel.theme.collectAsState()
    val schrift by viewModel.schriftgroesse.collectAsState()
    val anmeldung by viewModel.anmeldung.collectAsState()
    val eigeneStimmen by viewModel.eigeneStimmen.collectAsState()
    val stimmenLaden by viewModel.stimmenLaden.collectAsState()
    val stimmenEintraege by viewModel.stimmenEintraege.collectAsState()
    val gewaehlteStimme by viewModel.gewaehlteStimme.collectAsState()
    val favoriten by viewModel.favoriten.collectAsState()
    val vorlese by viewModel.vorleseStand.collectAsState()

    var googleKey by remember { mutableStateOf(settings.googleTtsApiKey) }
    var qwenKey by remember { mutableStateOf(settings.qwenTtsApiKey) }
    var groqKey by remember { mutableStateOf(settings.groqApiKey) }
    var geminiKey by remember { mutableStateOf(settings.geminiApiKey) }
    var tempo by remember { mutableStateOf(settings.ttsSpeechRate) }
    var immerDeutsch by remember { mutableStateOf(settings.immerDeutschVorlesen) }
    var sperreAn by remember { mutableStateOf(settings.appLockEnabled) }
    var sperreVerzoegerung by remember { mutableStateOf(settings.appLockDelayMinutes) }
    var kiZugang by remember { mutableStateOf(settings.kiZugang) }
    var modell by remember { mutableStateOf(settings.model) }
    var denktiefe by remember { mutableStateOf(settings.reasoning) }
    var pruefErgebnis by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    /** Bis wann der Kopier-Knopf „Kopiert ✓" zeigt (Baustein O.1). */
    var kopiertBis by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gold.hintergrund)
            .imePadding(),
    ) {
        IdeenKopfleiste(
            titel = "Einstellungen",
            themeWahl = theme,
            // Hier steht statt des Zahnrads der Hell-Dunkel-Schalter: In den Einstellungen
            // will man das Aussehen umlegen, nicht die Einstellungen nochmal öffnen.
            aufThemeTipp = viewModel::themeWeiterschalten,
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
            // Der erste Block klebt sonst an der Kopfleiste.
            Spacer(Modifier.height(4.dp))

            // ---- Vorlesen ----
            Klappblock("Vorlesen", "Stimme, Tempo und Schlüssel", offenAnfangs = true) {
                // Genau eine Stimmenauswahl über alle Engines — kein vorgelagerter
                // Engine-Umschalter (Kapitel 4.6).
                StimmenDropdown(
                    eintraege = stimmenEintraege,
                    gewaehlt = gewaehlteStimme,
                    favoriten = favoriten,
                    laedt = stimmenLaden,
                    spricht = if (vorlese.quelle == "probe") gewaehlteStimme else null,
                    aufWahl = viewModel::waehleStimme,
                    aufProbe = viewModel::probeStimme,
                    aufFavorit = viewModel::schalteFavorit,
                    aufNeuLaden = { viewModel.ladeEigeneStimmen(zeigeFehler = true) },
                    aufAufnehmen = aufEigeneStimme,
                    aufFehlendenSchluessel = {
                        viewModel.zeige(
                            Meldung("Trag den fehlenden Schlüssel hier im Block „Vorlesen“ ein."),
                        )
                    },
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    "Sprechtempo: ${"%.2f".format(tempo)}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                )
                Slider(
                    value = tempo,
                    onValueChange = {
                        tempo = it
                        settings.ttsSpeechRate = it
                    },
                    valueRange = 0.5f..2.0f,
                    colors = goldRegler(),
                )

                Spacer(Modifier.height(8.dp))
                SchalterZeile("Immer auf Deutsch aussprechen", immerDeutsch) {
                    immerDeutsch = it
                    settings.immerDeutschVorlesen = it
                }
                Text(
                    "Mehrsprachige Stimmen springen sonst mitten im Satz ins Englische, sobald " +
                        "ein Wort englisch aussieht. Mit diesem Schalter bleibt alles deutsch.",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                )

                SchluesselFeld(
                    beschriftung = "Google- oder Gemini-Schlüssel",
                    wert = googleKey,
                    aufWert = {
                        googleKey = it
                        settings.googleTtsApiKey = it.trim()
                        // Der Schlüssel entscheidet, welche Stimmen nutzbar sind.
                        viewModel.stimmenlisteAuffrischen()
                    },
                    ergebnis = pruefErgebnis["google"],
                    aufPruefen = {
                        viewModel.pruefeGoogleSchluessel { text ->
                            pruefErgebnis = pruefErgebnis + ("google" to text)
                        }
                    },
                )
                SchluesselFeld(
                    beschriftung = "Alibaba-DashScope-Schlüssel (eigene Stimme)",
                    wert = qwenKey,
                    aufWert = {
                        qwenKey = it
                        settings.qwenTtsApiKey = it.filterNot(Char::isWhitespace)
                        // Sobald der Schlüssel steht, holt die App die vorhandenen Stimmen —
                        // sie sollen nicht erst nach einem Neustart auftauchen.
                        viewModel.stimmenlisteAuffrischen()
                        viewModel.ladeEigeneStimmen()
                    },
                    ergebnis = when {
                        qwenKey.isBlank() -> null
                        stimmenLaden -> "Die Stimmen werden geholt …"
                        eigeneStimmen.isEmpty() -> null
                        else -> "${eigeneStimmen.size} eigene Stimmen gefunden."
                    },
                    aufPruefen = { viewModel.ladeEigeneStimmen(zeigeFehler = true) },
                )
                Spacer(Modifier.height(10.dp))
                GoldKnopf(text = "Eigene Stimme aufnehmen", aufTipp = aufEigeneStimme)
            }

            // ---- Spracheingabe ----
            Klappblock("Spracheingabe", "Diktat und Filter") {
                SchluesselFeld(
                    beschriftung = "Groq-Schlüssel",
                    wert = groqKey,
                    aufWert = {
                        groqKey = it
                        settings.groqApiKey = it.trim()
                    },
                    ergebnis = pruefErgebnis["groq"],
                    aufPruefen = {
                        viewModel.pruefeGroqSchluessel { text ->
                            pruefErgebnis = pruefErgebnis + ("groq" to text)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Modell: whisper-large-v3-turbo",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Filter gegen erfundene Sätze bei Stille",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                )
                Spacer(Modifier.height(6.dp))
                SchalterZeile("Stille vor dem Senden erkennen", settings.filterStilleVorabAn) {
                    settings.filterStilleVorabAn = it
                }
                SchalterZeile("Segmentwerte prüfen", settings.filterSegmentmetrikenAn) {
                    settings.filterSegmentmetrikenAn = it
                }
                SchalterZeile("Zeitstempel gegen die Stille abgleichen", settings.filterZeitstempelAn) {
                    settings.filterZeitstempelAn = it
                }
                SchalterZeile("Floskeln bei Stille verwerfen", settings.filterFloskelnAn) {
                    settings.filterFloskelnAn = it
                }
            }

            // ---- KI ----
            Klappblock("KI", "Zugang, Modell und Antworten") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Auswahlchip("Über mein ChatGPT-Abo", kiZugang == "abo") {
                        kiZugang = "abo"
                        settings.kiZugang = "abo"
                    }
                    Auswahlchip("Eigener Schlüssel", kiZugang == "schluessel") {
                        kiZugang = "schluessel"
                        settings.kiZugang = "schluessel"
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (kiZugang == "abo") {
                    Text(
                        if (viewModel.chatGptVerbunden) {
                            "Angemeldet als ${viewModel.chatGptKonto ?: "ChatGPT-Konto"}."
                        } else {
                            "Noch nicht angemeldet. Ohne Zugang bleibt das Gespräch zur Idee stumm."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = gold.textGedaempft,
                    )
                    Spacer(Modifier.height(10.dp))
                    AnimatedVisibility(visible = anmeldung.code != null) {
                        Column {
                            Text(
                                "Gib diesen Code im Browser ein:",
                                style = MaterialTheme.typography.labelSmall,
                                color = gold.textGedaempft,
                            )
                            Spacer(Modifier.height(6.dp))
                            val kopieren = {
                                val code = anmeldung.code.orEmpty()
                                if (code.isNotBlank()) {
                                    zwischenablage.setText(AnnotatedString(code))
                                    kopiertBis = System.currentTimeMillis() + 2_000L
                                }
                            }
                            // Der Code selbst ist antippbar, der Knopf ist der offensichtliche
                            // Weg — beide kopieren dasselbe (Baustein O.1).
                            Text(
                                anmeldung.code.orEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .druckEffekt { kopieren() }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(gold.eingabefeld)
                                    .border(1.dp, gold.primaer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = IdeenSchriftFest,
                                    letterSpacing = 4.sp,
                                ),
                                color = gold.primaer,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GoldKnopf(
                                    text = if (kopiertBis > System.currentTimeMillis()) {
                                        "Kopiert ✓"
                                    } else {
                                        "Code kopieren"
                                    },
                                    aufTipp = kopieren,
                                    symbol = {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = gold.aufPrimaer,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                                StillerKnopf(
                                    "Seite öffnen",
                                    { aufSeiteOeffnen(anmeldung.adresse) },
                                    hervorgehoben = true,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Der Code gilt rund 15 Minuten.",
                                style = MaterialTheme.typography.labelSmall,
                                color = gold.textGedaempft,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldKnopf(
                            text = if (viewModel.chatGptVerbunden) "Neu anmelden" else "Jetzt anmelden",
                            laedt = anmeldung.laeuft,
                            aufTipp = aufAnmelden,
                        )
                        // Eine laufende Anmeldung wartet bis zu 15 Minuten auf die Bestätigung im
                        // Browser — ohne diesen Knopf käme man aus dem Warten nicht mehr heraus.
                        if (anmeldung.laeuft) {
                            Auswahlchip("Abbrechen", false) { viewModel.brichAnmeldungAb() }
                        } else if (viewModel.chatGptVerbunden) {
                            Auswahlchip("Abmelden", false) { viewModel.meldeAb() }
                        }
                    }
                } else {
                    SchluesselFeld(
                        beschriftung = "Gemini-Schlüssel",
                        wert = geminiKey,
                        aufWert = {
                            geminiKey = it
                            settings.geminiApiKey = it.trim()
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Modell und Denktiefe gehören zusammen: Beides entscheidet, wie gründlich und
                // wie schnell die Antwort zur Idee kommt.
                Klappmenue(
                    beschriftung = "Modell",
                    eintraege = CodexModel.entries.map { eintrag ->
                        KlappEintrag(
                            id = eintrag.apiId,
                            name = eintrag.label,
                            zusatz = when (eintrag) {
                                CodexModel.SOL -> "gründlich"
                                CodexModel.TERRA -> "ausgewogen"
                                CodexModel.LUNA -> "schnell"
                            },
                        )
                    },
                    gewaehlt = modell,
                    aufWahl = {
                        modell = it
                        settings.model = it
                    },
                )

                Spacer(Modifier.height(14.dp))

                Klappmenue(
                    beschriftung = "Denktiefe",
                    eintraege = ReasoningEffort.entries.map { eintrag ->
                        KlappEintrag(
                            id = eintrag.apiValue,
                            name = eintrag.label,
                            zusatz = when (eintrag) {
                                ReasoningEffort.LOW -> "kommt sofort"
                                ReasoningEffort.MEDIUM -> "guter Mittelweg"
                                ReasoningEffort.HIGH -> "denkt länger nach"
                                ReasoningEffort.XHIGH -> "für harte Nüsse"
                                ReasoningEffort.MAX -> "dauert am längsten"
                            },
                        )
                    },
                    gewaehlt = denktiefe,
                    aufWahl = {
                        denktiefe = it
                        settings.reasoning = it
                    },
                )
            }

            // ---- Darstellung ----
            Klappblock("Darstellung", "Hell, dunkel und Schriftgrösse") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Auswahlchip("Hell", theme == "light") { viewModel.setzeTheme("light") }
                    Auswahlchip("Dunkel", theme == "dark") { viewModel.setzeTheme("dark") }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Schriftgrösse: ${"%.0f".format(schrift * 100)} %",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                )
                Slider(
                    value = schrift,
                    onValueChange = viewModel::setzeSchriftgroesse,
                    valueRange = 0.85f..1.4f,
                    colors = goldRegler(),
                )
            }

            // ---- Sicherheit ----
            Klappblock("Sicherheit", "App-Sperre per Fingerabdruck") {
                SchalterZeile("App-Sperre", sperreAn) { an ->
                    sperreAn = an
                    aufAppSperreUmschalten(an)
                }
                if (sperreAn) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Sperren, wenn die App im Hintergrund war:",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.textGedaempft,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "sofort", 1 to "1 Min", 5 to "5 Min", -1 to "nie").forEach { (wert, text) ->
                            Auswahlchip(text, sperreVerzoegerung == wert) {
                                sperreVerzoegerung = wert
                                settings.appLockDelayMinutes = wert
                            }
                        }
                    }
                }
            }

            // ---- Sicherung ----
            Klappblock("Sicherung", "Datei und Google Drive") {
                Text(
                    viewModel.sicherungsStand(),
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Auswahlchip("Exportieren", false, aufExport)
                    Auswahlchip("Importieren", false, aufImport)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Google Drive — die App sieht dabei ausschliesslich ihren eigenen versteckten " +
                        "Ordner, nie deine übrigen Dateien.",
                    style = MaterialTheme.typography.bodySmall,
                    color = gold.textGedaempft,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Auswahlchip("Jetzt sichern", false) { viewModel.sichereNachDrive() }
                    Auswahlchip("Wiederherstellen", false) { viewModel.holeVonDrive(ersetzen = false) }
                }
            }

            // ---- Diagnose ----
            Klappblock("Diagnose", "Protokoll und Absturzberichte") {
                Text(
                    "Hier steht, was die App zuletzt gemacht hat — ohne Schlüssel und ohne " +
                        "deine Ideentexte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = gold.textGedaempft,
                )
                Spacer(Modifier.height(12.dp))
                GoldKnopf(text = "Diagnose öffnen", aufTipp = aufDiagnose)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_BUMPED_AT})",
                style = MaterialTheme.typography.labelSmall,
                color = gold.textGedaempft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun Klappblock(
    titel: String,
    untertitel: String,
    offenAnfangs: Boolean = false,
    inhalt: @Composable () -> Unit,
) {
    val gold = LocalGold.current
    var offen by remember { mutableStateOf(offenAnfangs) }
    GoldKarte(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().druckEffekt { offen = !offen },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(titel, style = MaterialTheme.typography.titleSmall, color = gold.textPrimaer)
                    Text(untertitel, style = MaterialTheme.typography.labelSmall, color = gold.textGedaempft)
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (offen) "Zuklappen" else "Aufklappen",
                    tint = gold.primaer,
                    modifier = Modifier.size(22.dp).rotate(if (offen) 180f else 0f),
                )
            }
            AnimatedVisibility(visible = offen) {
                Column(Modifier.padding(top = 14.dp)) { inhalt() }
            }
        }
    }
}

@Composable
private fun Auswahlchip(text: String, gewaehlt: Boolean, aufTipp: () -> Unit) {
    val gold = LocalGold.current
    Box(
        modifier = Modifier
            .druckEffekt(aufTipp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (gewaehlt) gold.primaer.copy(alpha = 0.20f) else Color.Transparent)
            .border(1.dp, if (gewaehlt) gold.primaer else gold.rahmen, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (gewaehlt) gold.primaer else gold.textGedaempft,
        )
    }
}

@Composable
private fun SchalterZeile(text: String, wert: Boolean, aufWert: (Boolean) -> Unit) {
    val gold = LocalGold.current
    var an by remember(wert) { mutableStateOf(wert) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = gold.textPrimaer,
        )
        Switch(
            checked = an,
            onCheckedChange = {
                an = it
                aufWert(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = gold.aufPrimaer,
                checkedTrackColor = gold.primaer,
                uncheckedTrackColor = gold.flaecheErhoeht,
            ),
        )
    }
}

@Composable
private fun SchluesselFeld(
    beschriftung: String,
    wert: String,
    aufWert: (String) -> Unit,
    ergebnis: String? = null,
    aufPruefen: (() -> Unit)? = null,
) {
    val gold = LocalGold.current
    val zwischenablage = LocalClipboardManager.current
    var sichtbar by remember { mutableStateOf(false) }
    Column(Modifier.padding(top = 12.dp)) {
        Text(beschriftung, style = MaterialTheme.typography.labelSmall, color = gold.textGedaempft)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(gold.eingabefeld)
                    .border(1.dp, gold.rahmen, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                if (wert.isEmpty()) {
                    Text("noch leer", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                }
                BasicTextField(
                    value = wert,
                    onValueChange = aufWert,
                    singleLine = true,
                    visualTransformation = if (sichtbar) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = gold.textPrimaer),
                    cursorBrush = SolidColor(gold.primaer),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.size(38.dp).druckEffekt { sichtbar = !sichtbar },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (sichtbar) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (sichtbar) "Schlüssel verbergen" else "Schlüssel anzeigen",
                    tint = gold.textGedaempft,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier = Modifier.size(38.dp).druckEffekt {
                    zwischenablage.getText()?.text?.let { aufWert(it.trim()) }
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.ContentPaste,
                    contentDescription = "Aus der Zwischenablage einfügen",
                    tint = gold.textGedaempft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (aufPruefen != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Auswahlchip("Schlüssel prüfen", false, aufPruefen)
                Spacer(Modifier.width(10.dp))
                if (ergebnis != null) {
                    Text(
                        ergebnis,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ergebnis.startsWith("Abgelehnt")) Semantisch.fehler else Semantisch.erfolg,
                    )
                }
            }
        }
    }
}

@Composable
private fun StimmenListe(
    stimmen: List<de.frank.genialeideen.tts.TtsVoice>,
    gewaehlt: String,
    favoriten: Set<String>,
    aufWahl: (String) -> Unit,
    aufProbe: (String) -> Unit,
    aufFavorit: (String) -> Unit,
) {
    val gold = LocalGold.current
    Column {
        listOf(VoiceGender.FEMALE to "Weibliche Stimmen", VoiceGender.MALE to "Männliche Stimmen")
            .forEach { (geschlecht, ueberschrift) ->
                Text(
                    ueberschrift,
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                stimmen.filter { it.gender == geschlecht }.forEach { eintrag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (eintrag.id == gewaehlt) gold.primaer.copy(alpha = 0.14f) else Color.Transparent,
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            eintrag.name,
                            modifier = Modifier.weight(1f).druckEffekt { aufWahl(eintrag.id) },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (eintrag.id == gewaehlt) gold.primaer else gold.textPrimaer,
                        )
                        Box(
                            modifier = Modifier.size(32.dp).druckEffekt { aufFavorit(eintrag.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (eintrag.id in favoriten) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Als Favorit merken",
                                tint = if (eintrag.id in favoriten) gold.primaer else gold.textGedaempft,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .druckEffekt { aufProbe(eintrag.id) }
                                .clip(CircleShape)
                                .border(1.dp, gold.rahmen, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Probe abspielen",
                                tint = gold.primaer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
    }
}

/** Die Überschrift, unter der eine Stimme im Klappmenü einsortiert wird. */
private fun geschlechtsGruppe(geschlecht: VoiceGender): String = when (geschlecht) {
    VoiceGender.FEMALE -> "Weibliche Stimmen"
    VoiceGender.MALE -> "Männliche Stimmen"
}

/** Regler in Gold — die Vorgabefarbe von Material passt nicht zur Marke (Baustein A). */
@Composable
private fun goldRegler() = LocalGold.current.let { gold ->
    SliderDefaults.colors(
        thumbColor = gold.primaer,
        activeTrackColor = gold.primaer,
        inactiveTrackColor = gold.rahmen,
        activeTickColor = gold.aufPrimaer,
        inactiveTickColor = gold.textGedaempft,
    )
}
