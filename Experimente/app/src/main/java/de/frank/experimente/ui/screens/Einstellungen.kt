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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.rotate
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
import kotlin.math.roundToInt

/**
 * **B-08 — Einstellungen.**
 *
 * Aufgebaut nach dem Entwurf **in Handybreite**: jede Beschriftung steht **über** ihrem Feld,
 * jedes Feld nimmt die volle Breite. Die Messung stammt aus einem 475 dp breiten Fenster, in
 * dem Beschriftung und Feld nebeneinander liegen — bei Handybreite bricht der Entwurf um, und
 * das ist die Fassung, die auf dem Gerät gilt. Wer die breite Messung übernimmt, baut für die
 * falsche Breite.
 *
 * Die Bedienelemente sind die des Entwurfs, nicht ersetzt: **sieben Auswahllisten** mit
 * Chevron (`select` mit 34 Einträgen), **ein Schieberegler** mit Skalenstrichen und Wertanzeige
 * (`input type=range`, 0,70 bis 1,30 in Schritten von 0,05), und die Erinnerungen als **je
 * eigene Unterkarte** mit Schalter oben und Zeitfeld samt Uhr-Symbol darunter.
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
            // Der Entwurf setzt hier keinen Abschnittstitel: die Karte beginnt direkt mit
            // "Modell". Die Zwischenüberschrift "Logbuch" steht mitten in der Karte und
            // trennt die beiden Paare.
            Karte {
                Auswahl(
                    beschriftung = "Modell",
                    moeglichkeiten = CodexModell.entries.map { it.bezeichnung to it.apiId },
                    jetzt = modellExp,
                ) {
                    modellExp = it
                    einstellungen.modellExperimente = it
                }
                Auswahl(
                    beschriftung = "Effort",
                    moeglichkeiten = Effort.entries.map { it.bezeichnung to it.apiWert },
                    jetzt = effortExp,
                ) {
                    effortExp = it
                    einstellungen.effortExperimente = it
                }
                Text(
                    text = "Logbuch",
                    style = schriften.fliesstext,
                    color = farben.text,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Auswahl(
                    beschriftung = "Modell",
                    moeglichkeiten = CodexModell.entries.map { it.bezeichnung to it.apiId },
                    jetzt = modellLog,
                ) {
                    modellLog = it
                    einstellungen.modellLogbuch = it
                }
                Auswahl(
                    beschriftung = "Effort",
                    moeglichkeiten = Effort.entries.map { it.bezeichnung to it.apiWert },
                    jetzt = effortLog,
                ) {
                    effortLog = it
                    einstellungen.effortLogbuch = it
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
                Auswahl(
                    beschriftung = "Anbieter",
                    moeglichkeiten = listOf(
                        "Google Chirp 3 HD" to "google_cloud",
                        "Meine Stimme" to "qwen_clone",
                        "Microsoft Edge" to "edge",
                    ),
                    jetzt = anbieter,
                    obenAbstand = 16.dp,
                ) {
                    anbieter = it
                    einstellungen.ttsAnbieter = it
                }
                when (anbieter) {
                    // Der Entwurf zeigt die Stimmkennung selbst, nicht den Kurznamen.
                    "google_cloud" -> Auswahl(
                        beschriftung = "Stimme",
                        moeglichkeiten = TtsCatalog.googleVoices.map { it.id to it.id },
                        jetzt = stimmeGoogle,
                    ) {
                        stimmeGoogle = it
                        einstellungen.stimmeGoogle = it
                    }
                    "qwen_clone" -> Column(Modifier.padding(top = 12.dp)) {
                        Beschriftung("Stimme")
                        Textknopf("Stimme aufnehmen", { modell.sprechknopf(null) }, betont = false)
                    }
                    else -> Auswahl(
                        beschriftung = "Stimme",
                        moeglichkeiten = TtsCatalog.edgeVoices.map { it.id to it.id },
                        jetzt = stimmeEdge,
                    ) {
                        stimmeEdge = it
                        einstellungen.stimmeEdge = it
                    }
                }

                // F-23 Schritt 5: der Schieberegler, 0,70 bis 1,30 in Schritten von 0,05 —
                // 12 Schritte, mit Skalenstrichen und dem Wert rechts daneben.
                Box(Modifier.padding(top = 12.dp)) { Beschriftung("Sprechgeschwindigkeit") }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        value = tempo,
                        onValueChange = {
                            tempo = (it * 20f).roundToInt() / 20f
                            einstellungen.sprechtempo = tempo
                        },
                        valueRange = 0.7f..1.3f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = farben.aktion,
                            activeTrackColor = farben.aktion,
                            inactiveTrackColor = farben.rand,
                            activeTickColor = farben.aktion,
                            inactiveTickColor = farben.blass,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = String.format(java.util.Locale.GERMAN, "%.2f", tempo),
                        style = schriften.daten,
                        color = farben.gedaempft,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }

                // Im Entwurf steht "Probe hören" linksbündig unter dem Regler.
                Box(Modifier.padding(top = 12.dp)) {
                    Textknopf("Probe hören", modell::hoerProbe, betont = false)
                }
            }

            // --- F-24: Zugänge ----------------------------------------------------------
            Karte {
                Zwischenueberschrift("Zugänge")
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Codex", style = schriften.fliesstext, color = farben.text)
                        Text(
                            text = if (angemeldet) modell.angemeldetAls ?: "angemeldet" else "nicht angemeldet",
                            style = schriften.fliesstextKlein,
                            color = farben.gedaempft,
                        )
                    }
                    if (angemeldet) {
                        Textknopf("Abmelden", modell::meldeAb, betont = false)
                    } else {
                        Textknopf("Anmelden", modell::meldeAn, betont = false)
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
                Schluessel("Groq", groq) {
                    groq = it
                    einstellungen.groqSchluessel = it
                }
                Schluessel("Google Cloud", googleKey) {
                    googleKey = it
                    einstellungen.googleTtsSchluessel = it
                }
                // Der Entwurf nennt den Dienst "Alibaba", nicht "DashScope".
                Schluessel("Alibaba", qwenKey) {
                    qwenKey = it
                    einstellungen.qwenSchluessel = it
                }
            }

            // --- F-25: Erinnerungen -----------------------------------------------------
            // Jede Erinnerung ist im Entwurf eine eigene Unterkarte: Name und Schalter in
            // einer Zeile, darunter das Zeitfeld mit dem Uhr-Symbol rechts.
            Karte {
                Zwischenueberschrift("Erinnerungen")
                Box(Modifier.padding(top = 16.dp)) {
                    Erinnerung(
                        name = "Morgens",
                        an = morgensAn,
                        zeit = morgensZeit,
                        beiSchalter = {
                            morgensAn = it
                            einstellungen.erinnerungMorgensAn = it
                            modell.erinnerungenNeuSetzen()
                        },
                        beiZeit = {
                            morgensZeit = it
                            einstellungen.erinnerungMorgensZeit = it
                            modell.erinnerungenNeuSetzen()
                        },
                    )
                }
                Box(Modifier.padding(top = 12.dp)) {
                    Erinnerung(
                        name = "Abends",
                        an = abendsAn,
                        zeit = abendsZeit,
                        beiSchalter = {
                            abendsAn = it
                            einstellungen.erinnerungAbendsAn = it
                            modell.erinnerungenNeuSetzen()
                        },
                        beiZeit = {
                            abendsZeit = it
                            einstellungen.erinnerungAbendsZeit = it
                            modell.erinnerungenNeuSetzen()
                        },
                    )
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
                    Textknopf("System", { modell.setzeErscheinung(Erscheinung.SYSTEM) },
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

/** Eine Beschriftung, wie der Entwurf sie setzt: Fließtext, über dem Feld. */
@Composable
private fun Beschriftung(text: String) {
    Text(
        text = text,
        style = LocalSchriften.current.fliesstext,
        color = LocalFarben.current.text,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/**
 * Eine Auswahlliste: Beschriftung darüber, Feld über die volle Breite, Chevron rechts, beim
 * Antippen klappt die Liste **aller** Möglichkeiten auf.
 *
 * Ein Feld, das bei jedem Druck zur nächsten Möglichkeit weiterschaltet, wäre ein anderes
 * Bedienelement — der Entwurf zeigt `select` mit sichtbaren Einträgen.
 */
@Composable
private fun Auswahl(
    beschriftung: String,
    moeglichkeiten: List<Pair<String, String>>,
    jetzt: String,
    obenAbstand: androidx.compose.ui.unit.Dp = 12.dp,
    beiWahl: (String) -> Unit,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    var offen by remember { mutableStateOf(false) }
    val nr = moeglichkeiten.indexOfFirst { it.second == jetzt }.coerceAtLeast(0)

    Column(Modifier.fillMaxWidth().padding(top = obenAbstand)) {
        Beschriftung(beschriftung)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(Formen.eingabefeld)
                    .background(farben.flaeche)
                    .border(1.dp, farben.rand, Formen.eingabefeld)
                    .clickable { offen = true }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = moeglichkeiten.getOrNull(nr)?.first.orEmpty(),
                    style = schriften.fliesstext,
                    color = farben.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                // Das Chevron des Entwurfs — ein nach unten zeigender Winkel am rechten Rand.
                Text(
                    text = "⌄",
                    style = schriften.fliesstext,
                    color = farben.gedaempft,
                )
            }
            DropdownMenu(
                expanded = offen,
                onDismissRequest = { offen = false },
                modifier = Modifier.background(farben.erhoeht),
            ) {
                moeglichkeiten.forEach { (bezeichnung, wert) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = bezeichnung,
                                style = schriften.fliesstext,
                                color = if (wert == jetzt) farben.aktion else farben.text,
                            )
                        },
                        onClick = {
                            beiWahl(wert)
                            offen = false
                        },
                    )
                }
            }
        }
    }
}

/** Ein Schlüsselfeld: Beschriftung darüber, verdeckte Eingabe über die volle Breite. */
@Composable
private fun Schluessel(beschriftung: String, wert: String, beiAenderung: (String) -> Unit) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Beschriftung(beschriftung)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(Formen.eingabefeld)
                .background(farben.flaeche)
                .border(1.dp, farben.rand, Formen.eingabefeld)
                .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            if (wert.isEmpty()) {
                Text("nicht gesetzt", style = schriften.fliesstextKlein, color = farben.blass)
            }
            BasicTextField(
                value = wert,
                onValueChange = beiAenderung,
                textStyle = schriften.fliesstext.copy(color = farben.text),
                cursorBrush = SolidColor(farben.aktion),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Eine Erinnerung als eigene Unterkarte: Name und Schalter in einer Zeile, darunter das
 * Zeitfeld mit dem Uhr-Symbol in einem runden, gedeckten Feld rechts.
 */
@Composable
private fun Erinnerung(
    name: String,
    an: Boolean,
    zeit: String,
    beiSchalter: (Boolean) -> Unit,
    beiZeit: (String) -> Unit,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Formen.karte)
            .background(farben.flaeche)
            .border(1.dp, farben.randWeich, Formen.karte)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = schriften.fliesstext, color = farben.text, modifier = Modifier.weight(1f))
            Switch(
                checked = an,
                onCheckedChange = beiSchalter,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = farben.grund,
                    checkedTrackColor = farben.aktion,
                    uncheckedThumbColor = farben.gedaempft,
                    uncheckedTrackColor = farben.flaeche,
                    uncheckedBorderColor = farben.rand,
                ),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(Formen.eingabefeld)
                .border(1.dp, farben.randWeich, Formen.eingabefeld)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = zeit,
                onValueChange = { if (it.length <= 5) beiZeit(it) },
                textStyle = schriften.daten.copy(color = farben.text),
                cursorBrush = SolidColor(farben.aktion),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(40.dp)
                    .clip(Formen.vollrund)
                    .background(farben.aktionGedeckt),
                contentAlignment = Alignment.Center,
            ) {
                Text("🕐", style = schriften.fliesstextKlein, color = farben.text)
            }
        }
    }
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
