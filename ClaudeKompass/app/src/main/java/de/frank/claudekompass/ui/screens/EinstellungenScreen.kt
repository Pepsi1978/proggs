package de.frank.claudekompass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.claudekompass.BuildConfig
import de.frank.claudekompass.ai.geraeteCodeGruppen
import de.frank.claudekompass.audio.GroqTranskribierer
import de.frank.claudekompass.data.model.Denktiefe
import de.frank.claudekompass.data.model.Geschlecht
import de.frank.claudekompass.data.model.KiModell
import de.frank.claudekompass.data.model.Stimme
import de.frank.claudekompass.data.model.TtsAnbieter
import de.frank.claudekompass.security.AppSperre
import de.frank.claudekompass.tts.StimmProbeText
import de.frank.claudekompass.tts.TtsCatalog
import de.frank.claudekompass.tts.VorleseManager
import de.frank.claudekompass.tts.VorleseStufe
import de.frank.claudekompass.ui.components.AuswahlPunkt
import de.frank.claudekompass.ui.components.FehlerStreifen
import de.frank.claudekompass.ui.components.Klappauswahl
import de.frank.claudekompass.ui.components.Mehrfachauswahl
import de.frank.claudekompass.ui.components.HinweisStreifen
import de.frank.claudekompass.ui.components.Merkzeichen
import de.frank.claudekompass.ui.theme.LocalKompassFarben
import de.frank.claudekompass.ui.theme.Mass
import de.frank.claudekompass.ui.theme.ThemeModus
import de.frank.claudekompass.vm.EinstellungenViewModel
import de.frank.claudekompass.vm.PruefErgebnis

/**
 * Der Einstellungs-Bildschirm (Referenz, Baustein G).
 *
 * Blockweise aufgebaut: Vorlesen, Eigene Stimme, Spracheingabe, KI-Modell, Darstellung,
 * Sicherheit, Sicherung, Über. Jeder Schlüssel hat einen Prüfknopf, der einen echten Aufruf
 * macht — ein gefülltes Feld beweist noch nicht, dass es funktioniert.
 */
@Composable
fun EinstellungenScreen(
    viewModel: EinstellungenViewModel,
    beiExport: (String) -> Unit,
    beiImport: () -> Unit,
    beiLogAnsehen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zustand by viewModel.zustand.collectAsStateWithLifecycle()
    val vorlesen by viewModel.vorleseZustand.collectAsStateWithLifecycle()
    val kontext = LocalContext.current
    val activity = kontext as? FragmentActivity

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Mass.randSchmal,
            end = Mass.randSchmal,
            top = Mass.abstandKlein,
            bottom = Mass.rand * 3,
        ),
        verticalArrangement = Arrangement.spacedBy(Mass.abstandKlein),
    ) {
        item {
            if (zustand.schluesselAblageFehler != null) {
                FehlerStreifen(
                    text = zustand.schluesselAblageFehler!!,
                    beiSchliessen = viewModel::loescheMeldungen,
                )
            }
            if (zustand.fehler.isNotBlank() || vorlesen.fehler.isNotBlank()) {
                FehlerStreifen(
                    text = zustand.fehler.ifBlank { vorlesen.fehler },
                    beiSchliessen = viewModel::loescheMeldungen,
                )
            }
            if (zustand.meldung.isNotBlank()) {
                HinweisStreifen(text = zustand.meldung, beiSchliessen = viewModel::loescheMeldungen)
            }
        }

        // --- Vorlesen ---------------------------------------------------------------------
        item {
            Block("Vorlesen") {
                Klappauswahl(
                    beschriftung = "Womit vorgelesen wird",
                    punkte = TtsAnbieter.entries.map {
                        AuswahlPunkt(it, it.label, anbieterHinweis(it))
                    },
                    gewaehlt = zustand.ttsAnbieter,
                    beiWahl = viewModel::setzeAnbieter,
                )

                Spacer(Modifier.height(Mass.abstandKlein))
                Untertitel("Sprechtempo: ${"%.2f".format(zustand.tempo)}-fach")
                Slider(
                    value = zustand.tempo,
                    onValueChange = viewModel::setzeTempo,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                )

                Spacer(Modifier.height(Mass.abstand))
                Untertitel("Schlüssel")
                SchluesselFeld(
                    beschriftung = "Google- bzw. Gemini-Schlüssel (Chirp 3 HD)",
                    wert = zustand.googleSchluessel,
                    beiWert = viewModel::setzeGoogleSchluessel,
                    ergebnis = zustand.pruefungen[EinstellungenViewModel.SCHLUESSEL_GOOGLE],
                    beiPruefen = viewModel::pruefeGoogle,
                )
                SchluesselFeld(
                    beschriftung = "Alibaba-DashScope-Schlüssel (eigene Stimme)",
                    wert = zustand.alibabaSchluessel,
                    beiWert = viewModel::setzeAlibabaSchluessel,
                    ergebnis = zustand.pruefungen[EinstellungenViewModel.SCHLUESSEL_ALIBABA],
                    beiPruefen = viewModel::pruefeAlibaba,
                )
            }
        }

        // --- Stimmenliste -----------------------------------------------------------------
        item {
            val probeLaeuft = vorlesen.quelleId == VorleseManager.PROBE_ID &&
                vorlesen.stufe != VorleseStufe.AUS

            when (zustand.ttsAnbieter) {
                TtsAnbieter.GOOGLE, TtsAnbieter.EDGE -> {
                    val stimmen = if (zustand.ttsAnbieter == TtsAnbieter.GOOGLE) {
                        TtsCatalog.googleStimmen
                    } else {
                        TtsCatalog.edgeStimmen
                    }
                    val gewaehlte = if (zustand.ttsAnbieter == TtsAnbieter.GOOGLE) {
                        zustand.googleStimme
                    } else {
                        zustand.edgeStimme
                    }
                    Block("Stimme — ${zustand.ttsAnbieter.label}") {
                        Zeilentext(
                            "Wähl eine Stimme aus und tipp auf den Abspielknopf, um sie zu " +
                                "hören. Lieblinge stehen in der Liste oben.",
                        )
                        Klappauswahl(
                            beschriftung = "Stimme",
                            // Lieblinge nach oben, danach nach Geschlecht: So findet man seine
                            // Stimme wieder, ohne durch dreißig Namen zu suchen.
                            punkte = stimmen
                                .sortedWith(
                                    compareByDescending<Stimme> { it.id in zustand.lieblingsStimmen }
                                        .thenBy { it.geschlecht }
                                        .thenBy { it.name },
                                )
                                .map { stimme ->
                                    AuswahlPunkt(
                                        wert = stimme.id,
                                        text = stimme.name +
                                            if (stimme.id in zustand.lieblingsStimmen) "  ★" else "",
                                        zusatz = if (stimme.geschlecht == Geschlecht.WEIBLICH) {
                                            "weiblich"
                                        } else {
                                            "männlich"
                                        },
                                    )
                                },
                            gewaehlt = gewaehlte,
                            beiWahl = { viewModel.setzeStimme(zustand.ttsAnbieter, it) },
                            beiProbe = { viewModel.probeAbspielen(zustand.ttsAnbieter, gewaehlte) },
                            probeLaeuft = probeLaeuft,
                            beiProbeStopp = viewModel::stoppeProbe,
                        )
                        Aktionsknopf(
                            text = if (gewaehlte in zustand.lieblingsStimmen) {
                                "Aus den Lieblingen nehmen"
                            } else {
                                "Zu den Lieblingen"
                            },
                            zurueckhaltend = true,
                            symbol = if (gewaehlte in zustand.lieblingsStimmen) {
                                Icons.Default.Star
                            } else {
                                Icons.Default.StarBorder
                            },
                        ) {
                            viewModel.schalteLieblingsStimme(gewaehlte)
                        }
                    }
                }

                TtsAnbieter.QWEN -> Unit // Auswahl und Aufnahme stehen im Block darunter.
            }
        }

        // --- Eigene Stimme ----------------------------------------------------------------
        item {
            Block("Meine eigene Stimme") {
                // Die Auswahl steht oben: Wer mehrere Stimmen hat, will zuerst sehen und
                // aendern koennen, mit welcher gerade vorgelesen wird. Eine neue aufzunehmen
                // ist der seltenere Fall und steht deshalb darunter.
                if (zustand.eigeneStimmen.isNotEmpty()) {
                    Klappauswahl(
                        beschriftung = "Wird zum Vorlesen benutzt",
                        punkte = zustand.eigeneStimmen.map { stimme ->
                            AuswahlPunkt(
                                wert = stimme.id,
                                text = stimme.name,
                                zusatz = "angelegt am ${stimme.angelegtAm}",
                            )
                        },
                        gewaehlt = zustand.qwenStimme.takeIf { it.isNotBlank() },
                        beiWahl = { viewModel.setzeStimme(TtsAnbieter.QWEN, it) },
                        platzhalter = "Noch keine ausgewählt",
                        beiProbe = {
                            viewModel.probeAbspielen(TtsAnbieter.QWEN, zustand.qwenStimme)
                        },
                        probeLaeuft = vorlesen.quelleId == VorleseManager.PROBE_ID &&
                            vorlesen.stufe != VorleseStufe.AUS,
                        beiProbeStopp = viewModel::stoppeProbe,
                    )
                    if (zustand.ttsAnbieter != TtsAnbieter.QWEN) {
                        Zeilentext(
                            "Vorgelesen wird gerade mit ${zustand.ttsAnbieter.label}. Stell " +
                                "oben um, damit diese Stimme zum Zug kommt.",
                        )
                    }
                    Spacer(Modifier.height(Mass.abstand))
                }

                Zeilentext(
                    "Nimm einmal etwa eine Minute auf. Danach liest die App mit deiner Stimme " +
                        "vor. Die Aufnahme geht zu Alibaba Model Studio — dafür braucht es den " +
                        "Schlüssel von oben.",
                )
                Spacer(Modifier.height(Mass.abstandKlein))

                if (zustand.aufnahmeLaeuft) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                RoundedCornerShape(Mass.radiusKlein),
                            )
                            .padding(Mass.abstand),
                    ) {
                        Text(
                            text = zustand.aufnahmeSchritt,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(Mass.abstandKlein))
                        StimmProbeText.abschnitte.forEach { satz ->
                            Text(
                                text = satz,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                        Spacer(Modifier.height(Mass.abstand))
                        Row {
                            Aktionsknopf("Fertig — Stimme anlegen") {
                                viewModel.beendeStimmAufnahme("Frank")
                            }
                            Spacer(Modifier.width(Mass.abstandKlein))
                            Aktionsknopf("Abbrechen", zurueckhaltend = true) {
                                viewModel.brichStimmAufnahmeAb()
                            }
                        }
                    }
                } else {
                    Row {
                        Aktionsknopf("Stimme aufnehmen", symbol = Icons.Default.Mic) {
                            viewModel.starteStimmAufnahme()
                        }
                        Spacer(Modifier.width(Mass.abstandKlein))
                        Aktionsknopf("Meine Stimmen laden", zurueckhaltend = true) {
                            viewModel.ladeEigeneStimmen()
                        }
                    }
                }

                if (zustand.stimmenLaden) {
                    Spacer(Modifier.height(Mass.abstandKlein))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(Mass.abstandKlein))
                        Zeilentext("Die eigenen Stimmen werden geladen …")
                    }
                }

                if (zustand.eigeneStimmen.isNotEmpty()) {
                    Spacer(Modifier.height(Mass.abstand))
                    Untertitel("Vorhandene Stimmen")
                    zustand.eigeneStimmen.forEach { stimme ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stimme.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (stimme.id == zustand.qwenStimme) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    color = if (stimme.id == zustand.qwenStimme) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                Text(
                                    text = if (stimme.id == zustand.qwenStimme) {
                                        "ausgewählt - angelegt am ${stimme.angelegtAm}"
                                    } else {
                                        "angelegt am ${stimme.angelegtAm}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalKompassFarben.current.textGedaempft,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Die Stimme ${stimme.name} löschen",
                                tint = LocalKompassFarben.current.textGedaempft,
                                modifier = Modifier
                                    .size(Mass.tippflaeche)
                                    .clickable { viewModel.loescheEigeneStimme(stimme.id) }
                                    .padding(13.dp),
                            )
                        }
                    }
                }
            }
        }

        // --- Spracheingabe ----------------------------------------------------------------
        item {
            Block("Spracheingabe") {
                Zeilentext(
                    "Gesprochenes wird \u00fcber Groq abgeschrieben. Die vier Filter darunter " +
                        "verhindern, dass bei Stille S\u00e4tze erfunden werden.",
                )
                Spacer(Modifier.height(Mass.abstandKlein))
                SchluesselFeld(
                    beschriftung = "Groq-Schl\u00fcssel",
                    wert = zustand.groqSchluessel,
                    beiWert = viewModel::setzeGroqSchluessel,
                    ergebnis = zustand.pruefungen[EinstellungenViewModel.SCHLUESSEL_GROQ],
                    beiPruefen = viewModel::pruefeGroq,
                )

                Spacer(Modifier.height(Mass.abstandKlein))
                Klappauswahl(
                    beschriftung = "Modell",
                    punkte = GroqTranskribierer.MODELLE.map {
                        AuswahlPunkt(
                            wert = it,
                            text = it,
                            zusatz = if (it.endsWith("turbo")) {
                                "schneller, empfohlen"
                            } else {
                                "etwas genauer, daf\u00fcr langsamer"
                            },
                        )
                    },
                    gewaehlt = zustand.groqModell,
                    beiWahl = viewModel::setzeGroqModell,
                )

                Mehrfachauswahl(
                    beschriftung = "Schutz vor erfundenen S\u00e4tzen",
                    punkte = FILTER_TEXTE.mapIndexed { index, (titel, erklaerung) ->
                        AuswahlPunkt(index + 1, titel, erklaerung)
                    },
                    aktiv = zustand.filterSchichten
                        .mapIndexedNotNull { index, an -> if (an) index + 1 else null }
                        .toSet(),
                    beiWechsel = viewModel::schalteFilterSchicht,
                )
            }
        }

        // --- KI-Modell --------------------------------------------------------------------
        item {
            Block("KI-Modell") {
                if (zustand.codexVerbunden) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Merkzeichen("verbunden", farbe = LocalKompassFarben.current.erfolg)
                        Spacer(Modifier.width(Mass.abstandKlein))
                        Zeilentext(zustand.codexEmail ?: "bei Codex angemeldet")
                    }
                    Spacer(Modifier.height(Mass.abstandKlein))
                    Aktionsknopf("Abmelden", zurueckhaltend = true) { viewModel.meldeAb() }
                } else if (zustand.meldeAnLaeuft) {
                    AnmeldeCode(
                        code = zustand.geraeteCode,
                        beiAbbrechen = viewModel::brichAnmeldungAb,
                    )
                } else {
                    Zeilentext(
                        "Zum Fragen, für ausführlichere Erklärungen und für das Gespräch " +
                            "braucht es die Anmeldung. Sie läuft über einen kurzen Code im " +
                            "Browser — ein Passwort tippst du hier nie ein.",
                    )
                    Spacer(Modifier.height(Mass.abstandKlein))
                    Aktionsknopf("Bei Codex anmelden") {
                        activity?.let { viewModel.meldeAn(it) }
                    }
                }

                Spacer(Modifier.height(Mass.abstand))
                Klappauswahl(
                    beschriftung = "Modell",
                    punkte = KiModell.bekannt.map { AuswahlPunkt(it.apiId, it.label, it.apiId) } +
                        zustand.eigeneModelle.map { AuswahlPunkt(it, it, "selbst erg\u00e4nzt") },
                    gewaehlt = zustand.modellId,
                    beiWahl = { gewaehlt ->
                        val bekannt = KiModell.bekannt.firstOrNull { it.apiId == gewaehlt }
                        viewModel.setzeModell(bekannt?.label ?: gewaehlt, gewaehlt)
                    },
                )
                if (zustand.modellId in zustand.eigeneModelle) {
                    Aktionsknopf(
                        text = "Dieses Modell aus der Liste nehmen",
                        zurueckhaltend = true,
                        symbol = Icons.Default.Delete,
                    ) {
                        viewModel.entferneModell(zustand.modellId)
                    }
                }
                NeuesModellFeld { viewModel.ergaenzeModell(it) }

                Spacer(Modifier.height(Mass.abstandKlein))
                Klappauswahl(
                    beschriftung = "Denktiefe",
                    punkte = Denktiefe.entries.map {
                        AuswahlPunkt(it, it.label, denktiefeHinweis(it))
                    },
                    gewaehlt = zustand.denktiefe,
                    beiWahl = viewModel::setzeDenktiefe,
                )
            }
        }

        // --- Darstellung ------------------------------------------------------------------
        item {
            Block("Darstellung") {
                Klappauswahl(
                    beschriftung = "Erscheinungsbild",
                    punkte = ThemeModus.entries.map {
                        AuswahlPunkt(it, it.label, themeHinweis(it))
                    },
                    gewaehlt = zustand.themeModus,
                    beiWahl = viewModel::setzeTheme,
                )
            }
        }

        // --- Sicherheit -------------------------------------------------------------------
        item {
            Block("Sicherheit") {
                Schalterzeile(
                    titel = "App-Sperre",
                    erklaerung = "Die App fragt beim \u00d6ffnen nach Fingerabdruck, Gesicht oder " +
                        "dem Ger\u00e4tecode.",
                    an = zustand.appSperre,
                    beiWechsel = { an -> activity?.let { viewModel.schalteAppSperre(it, an) } },
                )
                if (zustand.appSperre) {
                    Klappauswahl(
                        beschriftung = "Wieder sperren",
                        punkte = AppSperre.WARTEZEITEN.map { (sekunden, text) ->
                            AuswahlPunkt(sekunden, text)
                        },
                        gewaehlt = zustand.sperreNach,
                        beiWahl = viewModel::setzeSperreNach,
                    )
                }
            }
        }

        // --- Sicherung --------------------------------------------------------------------
        item {
            Block("Sicherung") {
                Zeilentext(
                    "Gesichert werden deine Fragen samt Antworten, die vertieften Erklärungen " +
                        "und die Gespräche. Schlüssel kommen ausdrücklich NICHT mit in die " +
                        "Datei — die landet schnell in einer Cloud.",
                )
                Spacer(Modifier.height(Mass.abstandKlein))
                Row {
                    Aktionsknopf("Exportieren") { beiExport("") }
                    Spacer(Modifier.width(Mass.abstandKlein))
                    Aktionsknopf("Importieren", zurueckhaltend = true) { beiImport() }
                }
            }
        }

        // --- Über -------------------------------------------------------------------------
        item {
            Block("Über die App") {
                Zeilentext("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_BUMPED_AT})")
                Zeilentext(
                    "Mitgelieferte Wissensbasis: Claude Code ${BuildConfig.SEEDED_CLI_VERSION}",
                )
                Spacer(Modifier.height(Mass.abstandKlein))
                Aktionsknopf("Protokoll ansehen", zurueckhaltend = true) { beiLogAnsehen() }
            }
        }

        item { Spacer(Modifier.height(Mass.rand).navigationBarsPadding()) }
    }
}

/** Ein Satz je Anbieter, damit die Wahl im Klappmenue ohne Vorwissen zu treffen ist. */
private fun anbieterHinweis(anbieter: TtsAnbieter): String = when (anbieter) {
    TtsAnbieter.GOOGLE -> "beste Qualit\u00e4t, braucht den Google-Schl\u00fcssel"
    TtsAnbieter.EDGE -> "kostenlos, braucht keinen Schl\u00fcssel"
    TtsAnbieter.QWEN -> "deine eigene Stimme, braucht den Alibaba-Schl\u00fcssel"
}

private fun denktiefeHinweis(tiefe: Denktiefe): String = when (tiefe) {
    Denktiefe.LOW -> "schnell und g\u00fcnstig, f\u00fcr einfache Fragen"
    Denktiefe.MEDIUM -> "guter Ausgangspunkt"
    Denktiefe.HIGH -> "gr\u00fcndlicher, dauert l\u00e4nger"
    Denktiefe.XHIGH -> "f\u00fcr knifflige Fragen"
    Denktiefe.MAX -> "die tiefste Stufe, entsprechend langsam"
}

private fun themeHinweis(modus: ThemeModus): String = when (modus) {
    ThemeModus.HELL -> "immer hell"
    ThemeModus.DUNKEL -> "immer dunkel"
}

private val FILTER_TEXTE = listOf(
    "Schicht 1 — Stille vorab erkennen" to
        "Eine Aufnahme ohne Sprache wird gar nicht erst hochgeladen. Das spart Geld und " +
        "verhindert die Erfindung an der Wurzel.",
    "Schicht 2 — Kennzahlen prüfen" to
        "Abschnitte mit den typischen Werten einer Erfindung werden verworfen — etwa wenn das " +
        "Modell selbst angibt, dort sei keine Sprache gewesen.",
    "Schicht 3 — Zeitstempel abgleichen" to
        "Abschnitte, deren Zeitfenster im Ton still war, fallen weg. Würden alle fallen, " +
        "bleibt das Ergebnis von Schicht 2 stehen.",
    "Schicht 4 — Floskeln sperren" to
        "„Vielen Dank fürs Zuschauen“ und Ähnliches wird nur dann verworfen, wenn die Ausgabe " +
        "kurz ist UND ringsum Stille war. Ein bewusst gesagtes „Vielen Dank“ bleibt.",
)

/** Ein abgesetzter Block mit Überschrift — die Gliederung des Bildschirms. */
@Composable
private fun Block(titel: String, inhalt: @Composable ColumnScope.() -> Unit) {
    val farben = LocalKompassFarben.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Mass.radius))
            .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radius))
            .padding(Mass.abstand),
        content = {
            Text(
                text = titel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Mass.abstandKlein))
            inhalt()
        },
    )
}

@Composable
private fun Untertitel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = LocalKompassFarben.current.goldGedaempft,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun Zeilentext(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalKompassFarben.current.textGedaempft,
    )
}

@Composable
private fun Auswahlzeile(text: String, gewaehlt: Boolean, beiKlick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Mass.tippflaeche)
            .clickable(onClick = beiKlick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(
                    2.dp,
                    if (gewaehlt) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalKompassFarben.current.rahmen
                    },
                    RoundedCornerShape(9.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (gewaehlt) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
                )
            }
        }
        Spacer(Modifier.width(Mass.abstandKlein))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (gewaehlt) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun Schalterzeile(
    titel: String,
    erklaerung: String,
    an: Boolean,
    beiWechsel: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = erklaerung,
                style = MaterialTheme.typography.bodySmall,
                color = LocalKompassFarben.current.textGedaempft,
            )
        }
        Spacer(Modifier.width(Mass.abstandKlein))
        Switch(
            checked = an,
            onCheckedChange = beiWechsel,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

/**
 * Ein Feld für einen Schlüssel.
 *
 * Verdeckt angezeigt mit Augenknopf, dazu ein Einfügen-Knopf aus der Zwischenablage und ein
 * Prüfknopf. Beim Einfügen werden Leerzeichen entfernt: Ein mitkopiertes Leerzeichen ist die
 * häufigste Ursache für ein abgelehntes 401, und man sieht es dem Feld nicht an.
 */
@Composable
private fun SchluesselFeld(
    beschriftung: String,
    wert: String,
    beiWert: (String) -> Unit,
    ergebnis: PruefErgebnis?,
    beiPruefen: () -> Unit,
) {
    val farben = LocalKompassFarben.current
    val zwischenablage = LocalClipboardManager.current
    var sichtbar by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            text = beschriftung,
            style = MaterialTheme.typography.labelMedium,
            color = farben.textGedaempft,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(farben.eingabefeld, RoundedCornerShape(Mass.radiusKlein))
                    .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radiusKlein))
                    .padding(horizontal = Mass.abstandKlein, vertical = 12.dp),
            ) {
                if (wert.isEmpty()) {
                    Text(
                        text = "Noch kein Schl\u00fcssel hinterlegt",
                        style = MaterialTheme.typography.bodySmall,
                        color = farben.textGedaempft,
                    )
                }
                BasicTextField(
                    value = wert,
                    onValueChange = { beiWert(it.filterNot(Char::isWhitespace)) },
                    singleLine = true,
                    visualTransformation = if (sichtbar) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Icon(
                imageVector = if (sichtbar) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (sichtbar) "Schl\u00fcssel verbergen" else "Schl\u00fcssel anzeigen",
                tint = farben.textGedaempft,
                modifier = Modifier
                    .size(Mass.tippflaeche)
                    .clickable { sichtbar = !sichtbar }
                    .padding(13.dp),
            )
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = "Aus der Zwischenablage einf\u00fcgen",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(Mass.tippflaeche)
                    .clickable {
                        zwischenablage.getText()?.text?.let { beiWert(it.filterNot(Char::isWhitespace)) }
                    }
                    .padding(13.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Aktionsknopf(
            text = if (ergebnis?.laeuft == true) "Wird gepr\u00fcft \u2026" else "Schl\u00fcssel pr\u00fcfen",
            zurueckhaltend = true,
            laedt = ergebnis?.laeuft == true,
            beiKlick = beiPruefen,
        )

        // Das Ergebnis steht direkt unter dem Knopf, der es ausgel\u00f6st hat. Am Kopf des
        // Bildschirms w\u00e4re es zwar auch sichtbar \u2014 nur steht der beim Tippen l\u00e4ngst
        // ausserhalb des Sichtfelds, und dann liest es niemand.
        if (ergebnis != null && ergebnis.hatErgebnis) {
            Spacer(Modifier.height(6.dp))
            val ton = if (ergebnis.geglueckt) farben.erfolg else farben.fehler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ton.copy(alpha = 0.12f), RoundedCornerShape(Mass.radiusKlein))
                    .border(1.dp, ton.copy(alpha = 0.45f), RoundedCornerShape(Mass.radiusKlein))
                    .padding(horizontal = Mass.abstand, vertical = Mass.abstandKlein),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = if (ergebnis.geglueckt) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.ErrorOutline
                    },
                    contentDescription = null,
                    tint = ton,
                    modifier = Modifier.padding(top = 2.dp).size(17.dp),
                )
                Spacer(Modifier.width(Mass.abstandKlein))
                Text(
                    text = ergebnis.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AnmeldeCode(code: String, beiAbbrechen: () -> Unit) {
    val farben = LocalKompassFarben.current
    val zwischenablage = LocalClipboardManager.current
    val gruppen = geraeteCodeGruppen(code)
    var kopiert by remember(code) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                RoundedCornerShape(Mass.radiusKlein),
            )
            .padding(Mass.abstand),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Tipp diesen Code im Browser ein",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Mass.abstandKlein))
        Row(horizontalArrangement = Arrangement.spacedBy(Mass.abstandKlein)) {
            gruppen.forEach { gruppe ->
                Text(
                    text = gruppe,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(farben.eingabefeld, RoundedCornerShape(Mass.radiusKlein))
                        .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radiusKlein))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }

        // Ohne diesen Knopf muss man den Code von Hand abtippen: Er steht als Text da, und im
        // Browser ist das Feld weit weg. Kopieren ist der k\u00fcrzere und fehlerfreie Weg.
        if (code.isNotBlank()) {
            Spacer(Modifier.height(Mass.abstandKlein))
            Aktionsknopf(
                text = if (kopiert) "Kopiert" else "Code kopieren",
                zurueckhaltend = !kopiert,
                symbol = if (kopiert) Icons.Default.Check else Icons.Default.ContentCopy,
            ) {
                zwischenablage.setText(AnnotatedString(code))
                kopiert = true
            }
        }

        Spacer(Modifier.height(Mass.abstandKlein))
        Text(
            text = "Die Seite hat sich im Browser ge\u00f6ffnet. Komm nach dem Best\u00e4tigen hierher " +
                "zur\u00fcck \u2014 die App holt den Zugang dann von selbst.",
            style = MaterialTheme.typography.bodySmall,
            color = farben.textGedaempft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Mass.abstandKlein))
        Aktionsknopf("Anmeldung abbrechen", zurueckhaltend = true, beiKlick = beiAbbrechen)
    }
}

@Composable
private fun NeuesModellFeld(beiErgaenzen: (String) -> Unit) {
    val farben = LocalKompassFarben.current
    var eingabe by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(top = Mass.abstandKlein)) {
        Text(
            text = "Späteres Modell ergänzen",
            style = MaterialTheme.typography.labelMedium,
            color = farben.textGedaempft,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(farben.eingabefeld, RoundedCornerShape(Mass.radiusKlein))
                    .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radiusKlein))
                    .padding(horizontal = Mass.abstandKlein, vertical = 12.dp),
            ) {
                if (eingabe.isEmpty()) {
                    Text(
                        text = "z. B. gpt-5.7-sol",
                        style = MaterialTheme.typography.bodySmall,
                        color = farben.textGedaempft,
                    )
                }
                BasicTextField(
                    value = eingabe,
                    onValueChange = { eingabe = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(Mass.abstandKlein))
            Aktionsknopf("Hinzufügen") {
                beiErgaenzen(eingabe)
                eingabe = ""
            }
        }
    }
}

@Composable
private fun Aktionsknopf(
    text: String,
    zurueckhaltend: Boolean = false,
    laedt: Boolean = false,
    symbol: androidx.compose.ui.graphics.vector.ImageVector? = null,
    beiKlick: () -> Unit,
) {
    val ton = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .heightIn(min = Mass.tippflaeche)
            .background(
                color = if (zurueckhaltend) ton.copy(alpha = 0.10f) else ton,
                shape = RoundedCornerShape(Mass.radiusKlein),
            )
            .border(1.dp, ton.copy(alpha = 0.5f), RoundedCornerShape(Mass.radiusKlein))
            .clickable(enabled = !laedt, onClick = beiKlick)
            .padding(horizontal = Mass.abstand, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (laedt) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = if (zurueckhaltend) ton else MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(6.dp))
        } else if (symbol != null) {
            Icon(
                imageVector = symbol,
                contentDescription = null,
                tint = if (zurueckhaltend) ton else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (zurueckhaltend) ton else MaterialTheme.colorScheme.onPrimary,
        )
    }
}
