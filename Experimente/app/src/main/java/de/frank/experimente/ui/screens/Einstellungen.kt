package de.frank.experimente.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.BuildConfig
import de.frank.experimente.auth.CodexModell
import de.frank.experimente.auth.Effort
import de.frank.experimente.tts.TtsCatalog
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.Karte
import de.frank.experimente.ui.components.Kopfzeile
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.theme.Erscheinung
import de.frank.experimente.ui.theme.Formen
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole

/**
 * **B-08 — Einstellungen.** Die Abschnitte sind Karten, die Abschnittstitel stehen in
 * Großbuchstaben, und die Beschriftungen sitzen **neben** ihren Feldern — so ist der
 * Bildschirm bei 475 dp gemessen.
 *
 * Enthält F-22 (Modell und Effort, getrennt), F-23 (Stimme), F-24 (Zugänge),
 * F-25 (Erinnerungen), F-26 (Erscheinung) und den Weg zu B-09.
 */
@Composable
fun Einstellungen(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val einstellungen = modell.einstellungenZugriff()

    val erscheinung by modell.erscheinung.collectAsStateWithLifecycle()
    val angemeldet by modell.angemeldet.collectAsStateWithLifecycle()
    val geraetecode by modell.geraetecode.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()

    // Die Einstellungen liegen verschlüsselt; hier gespiegelt, damit die Anzeige mitgeht.
    var modellExp by remember { mutableStateOf(einstellungen.modellExperimente) }
    var effortExp by remember { mutableStateOf(einstellungen.effortExperimente) }
    var modellLog by remember { mutableStateOf(einstellungen.modellLogbuch) }
    var effortLog by remember { mutableStateOf(einstellungen.effortLogbuch) }
    var anbieter by remember { mutableStateOf(einstellungen.ttsAnbieter) }
    var stimmeGoogle by remember { mutableStateOf(einstellungen.stimmeGoogle) }
    var stimmeEdge by remember { mutableStateOf(einstellungen.stimmeEdge) }
    var tempo by remember { mutableStateOf(einstellungen.sprechtempo) }
    var groq by remember { mutableStateOf(einstellungen.groqSchluessel) }
    var googleKey by remember { mutableStateOf(einstellungen.googleTtsSchluessel) }
    var qwenKey by remember { mutableStateOf(einstellungen.qwenSchluessel) }
    var morgensAn by remember { mutableStateOf(einstellungen.erinnerungMorgensAn) }
    var morgensZeit by remember { mutableStateOf(einstellungen.erinnerungMorgensZeit) }
    var abendsAn by remember { mutableStateOf(einstellungen.erinnerungAbendsAn) }
    var abendsZeit by remember { mutableStateOf(einstellungen.erinnerungAbendsZeit) }

    Column(Modifier.fillMaxSize()) {
        Kopfzeile(
            titel = "Einstellungen",
            links = {
                Box(Modifier.padding(end = 12.dp)) {
                    Rundknopf(
                        symbol = Symbole.ZurueckZuHeute,
                        beschriftung = "Zurück zu Heute",
                        beiKlick = modell::zurueck,
                    )
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            meldung?.let { Karte { Text(it, style = schriften.fliesstext, color = farben.warnung) } }

            // --- F-22: KI ---------------------------------------------------------------
            Karte {
                Zwischenueberschrift("KI")
                Box(Modifier.padding(top = 16.dp)) {
                    Zeile("Experimente") {
                        Wahl(CodexModell.entries.map { it.bezeichnung to it.apiId }, modellExp) {
                            modellExp = it
                            einstellungen.modellExperimente = it
                        }
                    }
                }
                Zeile("Effort") {
                    Wahl(Effort.entries.map { it.bezeichnung to it.apiWert }, effortExp) {
                        effortExp = it
                        einstellungen.effortExperimente = it
                    }
                }
                Box(Modifier.padding(top = 20.dp)) {
                    Zeile("Logbuch") {
                        Wahl(CodexModell.entries.map { it.bezeichnung to it.apiId }, modellLog) {
                            modellLog = it
                            einstellungen.modellLogbuch = it
                        }
                    }
                }
                Zeile("Effort") {
                    Wahl(Effort.entries.map { it.bezeichnung to it.apiWert }, effortLog) {
                        effortLog = it
                        einstellungen.effortLogbuch = it
                    }
                }
                Text(
                    "Das Logbuch darf ein anderes Modell benutzen als die Experimente.",
                    style = schriften.fliesstextKlein,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            // --- F-23: Stimme -----------------------------------------------------------
            Karte {
                Zwischenueberschrift("Stimme")
                Box(Modifier.padding(top = 16.dp)) {
                    Zeile("Anbieter") {
                        Wahl(
                            listOf(
                                "Google Chirp 3 HD" to "google_cloud",
                                "Meine Stimme" to "qwen_clone",
                                "Microsoft Edge" to "edge",
                            ),
                            anbieter,
                        ) {
                            anbieter = it
                            einstellungen.ttsAnbieter = it
                        }
                    }
                }
                when (anbieter) {
                    "google_cloud" -> Zeile("Stimme") {
                        Wahl(TtsCatalog.googleVoices.map { it.name to it.id }, stimmeGoogle) {
                            stimmeGoogle = it
                            einstellungen.stimmeGoogle = it
                        }
                    }
                    "qwen_clone" -> Zeile("Meine Stimme") {
                        Textknopf("Stimme aufnehmen", { modell.sprechknopf(null) }, betont = false)
                    }
                    else -> Zeile("Stimme") {
                        Wahl(TtsCatalog.edgeVoices.map { it.name to it.id }, stimmeEdge) {
                            stimmeEdge = it
                            einstellungen.stimmeEdge = it
                        }
                    }
                }
                // F-23 Schritt 5: Sprechgeschwindigkeit 0,7 bis 1,3.
                Zeile("Tempo") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.7f, 0.85f, 1.0f, 1.15f, 1.3f).forEach { wert ->
                            Textknopf(
                                text = wert.toString(),
                                beiKlick = {
                                    tempo = wert
                                    einstellungen.sprechtempo = wert
                                },
                                betont = tempo == wert,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    Textknopf("Probe hören", modell::hoerProbe)
                }
            }

            // --- F-24: Zugänge ----------------------------------------------------------
            Karte {
                Zwischenueberschrift("Zugänge")
                Box(Modifier.padding(top = 16.dp)) {
                    Zeile("Codex") {
                        if (angemeldet) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    modell.angemeldetAls ?: "angemeldet",
                                    style = schriften.fliesstextKlein,
                                    color = farben.gedaempft,
                                    modifier = Modifier.padding(end = 12.dp),
                                )
                                Textknopf("Abmelden", modell::meldeAb, betont = false)
                            }
                        } else {
                            Textknopf("Anmelden", modell::meldeAn)
                        }
                    }
                }
                geraetecode?.let { code ->
                    Text(
                        "Code ${code.benutzercode} — die Seite ist offen: ${code.adresse}",
                        style = schriften.daten,
                        color = farben.aktion,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Zeile("Groq") {
                    Schluesselfeld(groq) {
                        groq = it
                        einstellungen.groqSchluessel = it
                    }
                }
                Zeile("Google Cloud") {
                    Schluesselfeld(googleKey) {
                        googleKey = it
                        einstellungen.googleTtsSchluessel = it
                    }
                }
                Zeile("DashScope") {
                    Schluesselfeld(qwenKey) {
                        qwenKey = it
                        einstellungen.qwenSchluessel = it
                    }
                }
            }

            // --- F-25: Erinnerungen -----------------------------------------------------
            Karte {
                Zwischenueberschrift("Erinnerungen")
                Box(Modifier.padding(top = 16.dp)) {
                    Zeile("Morgens") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Zeitfeld(morgensZeit) {
                                morgensZeit = it
                                einstellungen.erinnerungMorgensZeit = it
                                modell.erinnerungenNeuSetzen()
                            }
                            Schalter(morgensAn) {
                                morgensAn = it
                                einstellungen.erinnerungMorgensAn = it
                                modell.erinnerungenNeuSetzen()
                            }
                        }
                    }
                }
                Zeile("Abends") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Zeitfeld(abendsZeit) {
                            abendsZeit = it
                            einstellungen.erinnerungAbendsZeit = it
                            modell.erinnerungenNeuSetzen()
                        }
                        Schalter(abendsAn) {
                            abendsAn = it
                            einstellungen.erinnerungAbendsAn = it
                            modell.erinnerungenNeuSetzen()
                        }
                    }
                }
            }

            // --- F-26: Erscheinung ------------------------------------------------------
            Karte {
                Zwischenueberschrift("Erscheinung")
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Textknopf("Hell", { modell.setzeErscheinung(Erscheinung.HELL) },
                        Modifier.weight(1f), betont = erscheinung == Erscheinung.HELL)
                    Textknopf("Dunkel", { modell.setzeErscheinung(Erscheinung.DUNKEL) },
                        Modifier.weight(1f), betont = erscheinung == Erscheinung.DUNKEL)
                    Textknopf("Wie das System", { modell.setzeErscheinung(Erscheinung.SYSTEM) },
                        Modifier.weight(1f), betont = erscheinung == Erscheinung.SYSTEM)
                }
            }

            // --- Weg zu B-09 -------------------------------------------------------------
            Karte(beiKlick = { modell.gehe(Ziel.SELBSTBILD) }) {
                Zwischenueberschrift("Selbstbild")
                Text(
                    "Alles, was die App dauerhaft über dich wissen soll.",
                    style = schriften.fliesstextKlein,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // Die sichtbare Version — ohne sie wirkt eine geglückte Installation wie eine
            // fehlgeschlagene.
            Karte {
                Zwischenueberschrift("Über")
                Text(
                    "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_BUMPED_AT})",
                    style = schriften.daten,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Box(Modifier.padding(bottom = 20.dp))
        }
    }
}

/** Eine Zeile: Beschriftung links, Feld rechts — so ist B-08 gemessen. */
@Composable
private fun Zeile(beschriftung: String, feld: @Composable () -> Unit) {
    val farben = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = beschriftung,
            style = LocalSchriften.current.fliesstextKlein,
            color = farben.gedaempft,
            modifier = Modifier.width(112.dp),
        )
        Box(Modifier.weight(1f)) { feld() }
    }
}

/** Eine Auswahl: der aktuelle Wert steht da, ein Druck schaltet auf den nächsten. */
@Composable
private fun Wahl(
    moeglichkeiten: List<Pair<String, String>>,
    jetzt: String,
    beiWahl: (String) -> Unit,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val nr = moeglichkeiten.indexOfFirst { it.second == jetzt }.coerceAtLeast(0)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(Formen.eingabefeld)
            .background(farben.flaeche)
            .border(1.dp, farben.rand, Formen.eingabefeld)
            .clickable { beiWahl(moeglichkeiten[(nr + 1) % moeglichkeiten.size].second) }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Text(moeglichkeiten[nr].first, style = schriften.fliesstext, color = farben.text, maxLines = 1)
    }
}

/** Ein Schlüsselfeld — verdeckt angezeigt, kein Schlüssel steht im Quellcode. */
@Composable
private fun Schluesselfeld(wert: String, beiAenderung: (String) -> Unit) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(Formen.eingabefeld)
            .background(farben.flaeche)
            .border(1.dp, farben.rand, Formen.eingabefeld)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (wert.isEmpty()) {
            Text("nicht gesetzt", style = schriften.fliesstextKlein, color = farben.blass)
        }
        BasicTextField(
            value = wert,
            onValueChange = beiAenderung,
            textStyle = schriften.daten.copy(color = farben.text),
            cursorBrush = SolidColor(farben.aktion),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Zeitfeld(wert: String, beiAenderung: (String) -> Unit) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Box(
        Modifier
            .width(84.dp)
            .clip(Formen.eingabefeld)
            .background(farben.flaeche)
            .border(1.dp, farben.rand, Formen.eingabefeld)
            .padding(horizontal = 12.dp, vertical = 13.dp),
    ) {
        BasicTextField(
            value = wert,
            onValueChange = { if (it.length <= 5) beiAenderung(it) },
            textStyle = schriften.daten.copy(color = farben.text),
            cursorBrush = SolidColor(farben.aktion),
            singleLine = true,
        )
    }
}

@Composable
private fun Schalter(an: Boolean, beiWechsel: (Boolean) -> Unit) {
    val farben = LocalFarben.current
    Switch(
        checked = an,
        onCheckedChange = beiWechsel,
        colors = SwitchDefaults.colors(
            checkedThumbColor = farben.grund,
            checkedTrackColor = farben.aktion,
            uncheckedThumbColor = farben.gedaempft,
            uncheckedTrackColor = farben.flaeche,
            uncheckedBorderColor = farben.rand,
        ),
        modifier = Modifier.padding(start = 12.dp),
    )
}

/**
 * **B-09 — Selbstbild** (F-21). Ein großes, frei beschreibbares Feld: kein Raster, keine
 * Felder, keine Fragen. Wird beim Verlassen gespeichert.
 */
@Composable
fun Selbstbild(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val feld by modell.selbstbildFeld.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) { modell.ladeSelbstbildInsFeld() }

    Column(Modifier.fillMaxSize()) {
        Kopfzeile(
            titel = "Selbstbild",
            links = {
                Box(Modifier.padding(end = 12.dp)) {
                    Rundknopf(
                        symbol = Symbole.ZurueckZuEinstellungen,
                        beschriftung = "Zurück zu Einstellungen",
                        beiKlick = {
                            modell.speichereSelbstbild()
                            modell.zurueck()
                        },
                    )
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Alles, was die App dauerhaft über dich wissen soll. Je mehr hier steht, desto " +
                    "genauer treffen die Vorschläge.",
                style = schriften.fliesstextKlein,
                color = farben.gedaempft,
            )
            Eingabefeld(
                text = feld.text,
                beiAenderung = { modell.setzeText(modell.selbstbildFeldFluss(), it) },
                platzhalter = "Wer bist du? Was prägt dich? Was war? Sprich einfach drauflos.",
                zeilen = 14,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Sprechknopf(
                    nimmtAuf = nimmtAuf,
                    beiKlick = { modell.sprechknopf(modell.selbstbildFeldFluss()) },
                    beschriftung = "Selbstbild einsprechen",
                )
                Textknopf(
                    text = if (feld.kannZurueck) "Zurücknehmen" else "Text mit KI verbessern",
                    beiKlick = {
                        if (feld.kannZurueck) modell.nimmZurueck(modell.selbstbildFeldFluss())
                        else modell.verbessere(modell.selbstbildFeldFluss())
                    },
                    modifier = Modifier.weight(1f),
                    betont = false,
                    aktiv = !feld.laeuft,
                )
            }
            Textknopf("Speichern", modell::speichereSelbstbild, Modifier.fillMaxWidth())
            Box(Modifier.padding(bottom = 20.dp))
        }
    }
}
