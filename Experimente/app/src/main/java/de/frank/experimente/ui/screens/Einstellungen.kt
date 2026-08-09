package de.frank.experimente.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.frank.experimente.auth.CodexModel
import de.frank.experimente.auth.ReasoningEffort
import de.frank.experimente.tts.TtsCatalog
import de.frank.experimente.tts.TtsProvider
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Sprechziel
import de.frank.experimente.ui.components.Fehlerkarte
import de.frank.experimente.ui.components.ObereLeiste
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.SymbolKnopf
import de.frank.experimente.ui.components.TextKnopf
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.theme.AppForm
import de.frank.experimente.ui.theme.AppTypo
import de.frank.experimente.ui.theme.Erscheinung
import de.frank.experimente.ui.theme.LocalAppColors
import de.frank.experimente.ui.theme.Mass
import de.frank.experimente.ui.theme.schattenKarte

/** Der Satz, den „Probe hören“ vorliest (F-23, Schritt 6). */
private const val PROBE = "So klinge ich, wenn ich dir etwas vorlese."

// ---------------------------------------------------------------------------
// B-08 — Einstellungen (F-22 bis F-26)
// ---------------------------------------------------------------------------

@Composable
fun EinstellungenBildschirm(
    vm: AppViewModel,
    aufZurueck: () -> Unit,
    aufSelbstbild: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalAppColors.current
    val einstellungen = vm.einstellungen
    val konto by vm.codexKonto.collectAsState()
    val geraetecode by vm.geraetecode.collectAsState()
    val nimmtAuf by vm.nimmtAuf.collectAsState()
    val liestVor by vm.liestVor.collectAsState()
    val fehler by vm.fehler.collectAsState()
    val erscheinung by vm.erscheinung.collectAsState()
    val activity = LocalContext.current as? ComponentActivity

    Column(modifier = modifier.fillMaxSize().background(farben.grund)) {
        // Die Messung zeigt auf B-08 nur den Titel bei x = 20 — keinen Zurück-Knopf.
        // Zurück führt die Systemgeste, die der NavHost bedient.
        ObereLeiste(titel = "Einstellungen")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Mass.seitenrand)
                .padding(top = 20.dp, bottom = Mass.abschnittAbstand),
            verticalArrangement = Arrangement.spacedBy(Mass.abschnittAbstand),
        ) {
            if (fehler != null) {
                Fehlerkarte(text = fehler!!, onNochmal = vm::fehlerVerwerfen)
            }

            // --- KI (F-22) ---
            Abschnitt("KI") {
                var modell by remember { mutableStateOf(CodexModel.fromLabel(einstellungen.modellExperimente)) }
                var effort by remember { mutableStateOf(ReasoningEffort.fromLabel(einstellungen.effortExperimente)) }
                Auswahl("Modell", CodexModel.entries, modell, { it.label }) {
                    modell = it
                    einstellungen.modellExperimente = it.apiId
                }
                Auswahl("Effort", ReasoningEffort.entries, effort, { it.label }) {
                    effort = it
                    einstellungen.effortExperimente = it.apiValue
                }

                Zwischenueberschrift("Logbuch", Modifier.padding(top = Mass.kartenAbstand))
                var modellLog by remember { mutableStateOf(CodexModel.fromLabel(einstellungen.modellLogbuch)) }
                var effortLog by remember { mutableStateOf(ReasoningEffort.fromLabel(einstellungen.effortLogbuch)) }
                Auswahl("Modell", CodexModel.entries, modellLog, { it.label }) {
                    modellLog = it
                    einstellungen.modellLogbuch = it.apiId
                }
                Auswahl("Effort", ReasoningEffort.entries, effortLog, { it.label }) {
                    effortLog = it
                    einstellungen.effortLogbuch = it.apiValue
                }
                Text(
                    text = "Das Logbuch darf ein anderes Modell benutzen als die Experimente.",
                    style = AppTypo.fliesstextKlein,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = Mass.kartenAbstand),
                )
            }

            // --- Stimme (F-23) ---
            Abschnitt("Stimme") {
                var anbieter by remember {
                    mutableStateOf(
                        TtsProvider.entries.firstOrNull { it.id == einstellungen.ttsAnbieter }
                            ?: TtsProvider.GOOGLE_CLOUD,
                    )
                }
                Auswahl("Anbieter", TtsProvider.entries, anbieter, { it.label }) {
                    anbieter = it
                    einstellungen.ttsAnbieter = it.id
                }

                when (anbieter) {
                    TtsProvider.GOOGLE_CLOUD -> {
                        var stimme by remember { mutableStateOf(einstellungen.stimmeGoogle) }
                        Auswahl(
                            beschriftung = "Stimme",
                            werte = TtsCatalog.googleVoices.map { it.id },
                            gewaehlt = stimme,
                            anzeige = { it },
                        ) {
                            stimme = it
                            einstellungen.stimmeGoogle = it
                        }
                    }

                    TtsProvider.EDGE -> {
                        var stimme by remember { mutableStateOf(einstellungen.stimmeEdge) }
                        Auswahl(
                            beschriftung = "Stimme",
                            werte = TtsCatalog.edgeVoices.map { it.id },
                            gewaehlt = stimme,
                            anzeige = { it },
                        ) {
                            stimme = it
                            einstellungen.stimmeEdge = it
                        }
                    }

                    TtsProvider.QWEN_CLONE -> Zeile("Stimme") {
                        TextKnopf(
                            text = if (nimmtAuf) "Aufnahme beenden" else "Stimme aufnehmen",
                            onClick = vm::eigeneStimmeUmschalten,
                        )
                    }
                }

                var tempo by remember { mutableStateOf(einstellungen.sprechtempo) }
                Zeile("Sprechgeschwindigkeit") {
                    Text(
                        text = String.format(java.util.Locale.GERMAN, "%.2f", tempo),
                        style = AppTypo.daten,
                        color = farben.gedaempft,
                    )
                }
                Slider(
                    value = tempo,
                    onValueChange = { tempo = it },
                    onValueChangeFinished = { einstellungen.sprechtempo = tempo },
                    valueRange = 0.7f..1.3f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = farben.aktion,
                        activeTrackColor = farben.aktion,
                        inactiveTrackColor = farben.erhoeht,
                    ),
                )

                TextKnopf(
                    text = if (liestVor) "Probe anhalten" else "Probe hören",
                    onClick = { vm.vorlesen(PROBE) },
                )
            }

            // --- Zugänge (F-24) ---
            Abschnitt("Zugänge") {
                Zeile("Codex") {
                    Text(
                        text = konto ?: "nicht angemeldet",
                        style = AppTypo.daten,
                        color = farben.gedaempft,
                        modifier = Modifier.padding(end = Mass.kartenAbstand),
                    )
                    TextKnopf(
                        text = if (konto == null) "Anmelden" else "Abmelden",
                        onClick = {
                            if (konto == null) activity?.let(vm::anmelden) else vm.abmelden()
                        },
                    )
                }
                geraetecode?.let { code ->
                    Text(
                        text = "Öffne ${code.verificationUri} und gib ${code.userCode} ein.",
                        style = AppTypo.fliesstext,
                        color = farben.text,
                    )
                }
                Geheimnis("Groq", einstellungen.groqSchluessel) { einstellungen.groqSchluessel = it }
                Geheimnis("Google Cloud", einstellungen.googleTtsSchluessel) {
                    einstellungen.googleTtsSchluessel = it
                }
                Geheimnis("Alibaba", einstellungen.qwenSchluessel) { einstellungen.qwenSchluessel = it }
            }

            // --- Erinnerungen (F-25) ---
            Abschnitt("Erinnerungen") {
                var morgensAn by remember { mutableStateOf(einstellungen.erinnerungMorgensAn) }
                var morgensZeit by remember { mutableStateOf(einstellungen.erinnerungMorgensZeit) }
                Erinnerung("Morgens", morgensZeit, morgensAn, { zeit ->
                    morgensZeit = zeit
                    einstellungen.erinnerungMorgensZeit = zeit
                    vm.erinnerungenNeuSetzen()
                }) { an ->
                    morgensAn = an
                    einstellungen.erinnerungMorgensAn = an
                    vm.erinnerungenNeuSetzen()
                }

                var abendsAn by remember { mutableStateOf(einstellungen.erinnerungAbendsAn) }
                var abendsZeit by remember { mutableStateOf(einstellungen.erinnerungAbendsZeit) }
                Erinnerung("Abends", abendsZeit, abendsAn, { zeit ->
                    abendsZeit = zeit
                    einstellungen.erinnerungAbendsZeit = zeit
                    vm.erinnerungenNeuSetzen()
                }) { an ->
                    abendsAn = an
                    einstellungen.erinnerungAbendsAn = an
                    vm.erinnerungenNeuSetzen()
                }
            }

            // --- Erscheinung (F-26) ---
            Abschnitt("Erscheinung") {
                Row(horizontalArrangement = Arrangement.spacedBy(Mass.kartenAbstand)) {
                    listOf(
                        Erscheinung.HELL to "Hell",
                        Erscheinung.DUNKEL to "Dunkel",
                        Erscheinung.SYSTEM to "Wie das System",
                    ).forEach { (wert, beschriftung) ->
                        Wahlfeld(
                            text = beschriftung,
                            gewaehlt = erscheinung == wert,
                            onClick = { vm.erscheinungSetzen(wert) },
                        )
                    }
                }
            }

            // --- Über mich ---
            Abschnitt("Über mich") {
                Zeile("Selbstbild") {
                    TextKnopf(text = "Öffnen", onClick = aufSelbstbild)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// B-09 — Selbstbild (F-21)
// ---------------------------------------------------------------------------

@Composable
fun SelbstbildBildschirm(
    vm: AppViewModel,
    aufZurueck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalAppColors.current
    val gespeichert by vm.selbstbild.collectAsState()
    val nimmtAuf by vm.nimmtAuf.collectAsState()
    var text by remember(gespeichert) { mutableStateOf(gespeichert) }

    Column(modifier = modifier.fillMaxSize().background(farben.grund)) {
        ObereLeiste(
            titel = "Selbstbild",
            links = {
                SymbolKnopf(
                    symbol = Icons.AutoMirrored.Filled.ArrowBack,
                    beschreibung = "Zurück zu Einstellungen",
                    onClick = {
                        // F-21 — wird beim Verlassen gespeichert.
                        vm.selbstbildSpeichern(text)
                        aufZurueck()
                    },
                    tint = farben.text,
                )
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = Mass.seitenrand),
        ) {
            Text(
                text = "Alles, was die App dauerhaft über dich wissen soll. " +
                    "Je mehr hier steht, desto genauer treffen die Vorschläge.",
                style = AppTypo.fliesstextKlein,
                color = farben.gedaempft,
                modifier = Modifier.padding(top = 20.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = Mass.abschnittAbstand, bottom = 20.dp)
                    .clip(AppForm.eingabefeld)
                    .background(farben.erhoeht)
                    .border(Mass.randstaerke, farben.rand, AppForm.eingabefeld)
                    .padding(Mass.karteInnen),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Wer bist du? Was prägt dich? Was war?\nSprich einfach drauflos.",
                        style = AppTypo.fliesstext,
                        color = farben.blass,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = AppTypo.fliesstext.copy(color = farben.text),
                    cursorBrush = SolidColor(farben.aktion),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hatFassung = vm.hatVerbesserung(Sprechziel.SELBSTBILD)
                TextKnopf(
                    text = if (hatFassung) "Zurücknehmen" else "Text mit KI verbessern",
                    onClick = {
                        if (hatFassung) {
                            vm.verbesserungZuruecknehmen(Sprechziel.SELBSTBILD) { text = it }
                        } else {
                            vm.textVerbessern(Sprechziel.SELBSTBILD, text) { text = it }
                        }
                    },
                )
                Sprechknopf(
                    laeuftAufnahme = nimmtAuf,
                    onKlick = {
                        vm.aufnahmeUmschalten(Sprechziel.SELBSTBILD) { neu ->
                            text = if (text.isBlank()) neu else "$text\n$neu"
                        }
                    },
                )
                Box(Modifier.width(Mass.sprechknopfGross))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bauteile von B-08
// ---------------------------------------------------------------------------

@Composable
private fun Abschnitt(titel: String, inhalt: @Composable () -> Unit) {
    val farben = LocalAppColors.current
    // Gemessen: jeder Abschnitt ist eine Karte — Fläche/82 %, 1 dp Rand/84 %, Radius 20,
    // Kartenschatten, 20 dp innen. Der Titel darin in Großbuchstaben, 13 sp, halbfett.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .schattenKarte(farben, AppForm.karte)
            .clip(AppForm.karte)
            .background(farben.flaeche.copy(alpha = 0.82f))
            .border(Mass.randstaerke, farben.rand.copy(alpha = 0.84f), AppForm.karte)
            .padding(Mass.karteInnen),
        verticalArrangement = Arrangement.spacedBy(Mass.kartenAbstand),
    ) {
        Zwischenueberschrift(titel)
        inhalt()
    }
}

/** Beschriftung links, Inhalt rechts — die Zeilenform aller Einstellungen. */
@Composable
private fun Zeile(beschriftung: String, inhalt: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(beschriftung, style = AppTypo.fliesstext, color = LocalAppColors.current.text)
        Row(verticalAlignment = Alignment.CenterVertically) { inhalt() }
    }
}

/** Eine Auswahl: der gewählte Wert steht in der Fläche, ein Tipp schaltet zum nächsten. */
@Composable
private fun <T> Auswahl(
    beschriftung: String,
    werte: List<T>,
    gewaehlt: T,
    anzeige: (T) -> String,
    onWahl: (T) -> Unit,
) {
    val farben = LocalAppColors.current
    Zeile(beschriftung) {
        Row(
            modifier = Modifier
                .clip(AppForm.eingabefeld)
                .background(farben.erhoeht)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    val naechster = (werte.indexOf(gewaehlt) + 1).mod(werte.size)
                    onWahl(werte[naechster])
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(anzeige(gewaehlt), style = AppTypo.fliesstext, color = farben.text)
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = farben.gedaempft,
                modifier = Modifier.padding(start = 8.dp).size(Mass.symbolListe),
            )
        }
    }
}

/** Ein Zugang — der Schlüssel bleibt verdeckt. */
@Composable
private fun Geheimnis(beschriftung: String, vorgabe: String, onWert: (String) -> Unit) {
    val farben = LocalAppColors.current
    var wert by remember { mutableStateOf(vorgabe) }
    Zeile(beschriftung) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(Mass.tippflaeche)
                .clip(AppForm.eingabefeld)
                .background(farben.erhoeht)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = wert,
                onValueChange = { wert = it; onWert(it) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = AppTypo.fliesstext.copy(color = farben.text),
                cursorBrush = SolidColor(farben.aktion),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Eine Erinnerungszeile: Name, Uhrzeit, Schalter. */
@Composable
private fun Erinnerung(
    name: String,
    zeit: String,
    an: Boolean,
    onZeit: (String) -> Unit,
    onAn: (Boolean) -> Unit,
) {
    val farben = LocalAppColors.current
    Zeile(name) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(Mass.tippflaeche)
                .clip(AppForm.eingabefeld)
                .background(farben.erhoeht)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = zeit,
                onValueChange = { if (it.length <= 5) onZeit(it) },
                singleLine = true,
                textStyle = AppTypo.daten.copy(color = farben.text),
                cursorBrush = SolidColor(farben.aktion),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Schalter(an = an, onAn = onAn, modifier = Modifier.padding(start = Mass.kartenAbstand))
    }
}

/** **M-27/M-28** — der Knopf gleitet in 200 ms `ruhig` von links nach rechts. */
@Composable
private fun Schalter(an: Boolean, onAn: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(AppForm.vollrund)
            .background(if (an) farben.aktion else farben.erhoeht)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onAn(!an) },
        contentAlignment = if (an) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .size(24.dp)
                .clip(AppForm.vollrund)
                .background(if (an) farben.grund else farben.blass),
        )
    }
}

/** Ein Wahlfeld der Erscheinung. */
@Composable
private fun Wahlfeld(text: String, gewaehlt: Boolean, onClick: () -> Unit) {
    val farben = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(AppForm.vollrund)
            .background(if (gewaehlt) farben.aktionGedeckt else farben.erhoeht)
            .border(
                width = Mass.randstaerke,
                color = if (gewaehlt) farben.aktion else farben.rand,
                shape = AppForm.vollrund,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            style = AppTypo.knopf,
            color = if (gewaehlt) farben.aktion else farben.gedaempft,
        )
    }
}
