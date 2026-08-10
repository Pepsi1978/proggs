package de.frank.experimente.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.data.local.LogDay
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.Etikett
import de.frank.experimente.ui.components.Karte
import de.frank.experimente.ui.components.Kopfzeile
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.UntereLeiste
import de.frank.experimente.ui.components.Wartezustand
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Formen
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.sprechknopfVerlauf
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Das Gerüst der vier Listenbildschirme: Kopfzeile, Untertitel, Inhalt, untere Leiste — und
 * der Plus-Knopf **schwebend** in der rechten unteren Ecke.
 *
 * Der Plus-Knopf sitzt bewusst in einer `Box` mit `align(BottomEnd)`, nicht als letztes Kind
 * der Spalte: dort bekäme er neben einem `fillMaxSize`-Geschwister die Höhe 0 und wäre
 * unsichtbar — grün kompiliert, zur Laufzeit weg (Lauf 01, V-41).
 */
@Composable
private fun Listenbildschirm(
    titel: String,
    untertitel: String?,
    jetzt: Ziel,
    modell: AppViewModel,
    plus: (@Composable () -> Unit)? = null,
    inhalt: @Composable () -> Unit,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Column(Modifier.fillMaxSize()) {
        Kopfzeile(titel)
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                untertitel?.let {
                    Text(it, style = schriften.fliesstextKlein, color = farben.gedaempft)
                }
                inhalt()
                Box(Modifier.padding(bottom = 20.dp))
            }
            plus?.let {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                ) { it() }
            }
        }
        UntereLeiste(jetzt = jetzt, beiWahl = modell::gehe)
    }
}

/** Der runde Plus-Knopf: 56 dp, vollrund, Verlauf wie der Sprechknopf. */
@Composable
private fun Plusknopf(beschriftung: String, beiKlick: () -> Unit) {
    val farben = LocalFarben.current
    Box(
        Modifier
            .size(56.dp)
            .clip(Formen.vollrund)
            .background(sprechknopfVerlauf(farben))
            .border(1.dp, farben.text.copy(alpha = 0.28f), Formen.vollrund)
            .clickable(onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Symbole.ZielAnlegen,
            contentDescription = beschriftung,
            tint = farben.grund,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** **B-04 — Wünsche & Ziele** (F-20). Mehrere hintereinander weg, je mit KI-Verbesserung. */
@Composable
fun WuenscheUndZiele(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val ziele by modell.ziele.collectAsStateWithLifecycle()
    val feld by modell.zielFeld.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    var erfassen by remember { mutableStateOf(false) }

    Listenbildschirm(
        titel = "Wünsche & Ziele",
        untertitel = "Was möchtest du erreichen? Die Vorschläge tasten dich Schritt für Schritt heran.",
        jetzt = Ziel.ZIELE,
        modell = modell,
        plus = if (erfassen) null else ({ Plusknopf("Ziel anlegen") { erfassen = true } }),
    ) {
        if (erfassen) {
            Karte {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Sprechknopf(
                        nimmtAuf = nimmtAuf,
                        beiKlick = { modell.sprechknopf(modell.zielFeldFluss()) },
                        beschriftung = "Ziel einsprechen",
                    )
                }
                if (feld.text.isNotBlank()) {
                    Box(Modifier.padding(top = 20.dp)) {
                        Eingabefeld(
                            text = feld.text,
                            beiAenderung = { modell.setzeText(modell.zielFeldFluss(), it) },
                            platzhalter = "Was möchtest du erreichen?",
                            zeilen = 3,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Textknopf(
                            text = if (feld.kannZurueck) "Zurücknehmen" else "Text mit KI verbessern",
                            beiKlick = {
                                if (feld.kannZurueck) modell.nimmZurueck(modell.zielFeldFluss())
                                else modell.verbessere(modell.zielFeldFluss())
                            },
                            modifier = Modifier.weight(1f),
                            betont = false,
                            aktiv = !feld.laeuft,
                        )
                        // F-20 Schritt 2: nach dem Speichern steht der Sprechknopf sofort
                        // wieder bereit — der Erfassungsbereich bleibt offen.
                        Textknopf("Speichern", modell::legeZielAn, Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    Textknopf("Fertig", { erfassen = false }, betont = false)
                }
            }
        }

        if (ziele.isEmpty() && !erfassen) {
            Karte {
                Text("Noch keine Ziele. Sprich das erste ein.", style = schriften.fliesstext, color = farben.gedaempft)
            }
        }

        ziele.forEach { ziel ->
            var bearbeiten by remember(ziel.id) { mutableStateOf(false) }
            var text by remember(ziel.id) { mutableStateOf(ziel.text) }
            Karte {
                if (bearbeiten) {
                    Eingabefeld(text = text, beiAenderung = { text = it }, platzhalter = "", zeilen = 3)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Textknopf("Löschen", { modell.loescheZiel(ziel) }, Modifier.weight(1f), betont = false)
                        Textknopf(
                            text = "Speichern",
                            beiKlick = {
                                modell.aendereZiel(ziel, text)
                                bearbeiten = false
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Text(
                        text = ziel.text,
                        style = schriften.fliesstext,
                        color = farben.text,
                        modifier = Modifier.clickable { bearbeiten = true },
                    )
                }
            }
        }
    }
}

/** **B-05 — Merkliste** (F-18, F-19). Löschen nach Rückfrage; endgültig. */
@Composable
fun Merkliste(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val merkliste by modell.merkliste.collectAsStateWithLifecycle()
    val feld by modell.merkFeld.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    val wartet by modell.wartet.collectAsStateWithLifecycle()
    var erfassen by remember { mutableStateOf(false) }
    var loeschen by remember { mutableStateOf<Long?>(null) }

    Listenbildschirm(
        titel = "Merkliste",
        untertitel = null,
        jetzt = Ziel.MERKLISTE,
        modell = modell,
        plus = if (erfassen) null else ({ Plusknopf("Eigenes Experiment anlegen") { erfassen = true } }),
    ) {
        if (erfassen) {
            Karte {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Sprechknopf(
                        nimmtAuf = nimmtAuf,
                        beiKlick = { modell.sprechknopf(modell.merkFeldFluss()) },
                        beschriftung = "Eigenes Experiment einsprechen",
                    )
                }
                if (feld.text.isNotBlank()) {
                    Box(Modifier.padding(top = 20.dp)) {
                        Eingabefeld(
                            text = feld.text,
                            beiAenderung = { modell.setzeText(modell.merkFeldFluss(), it) },
                            platzhalter = "Was möchtest du ausprobieren?",
                            zeilen = 3,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Textknopf(
                            text = if (feld.kannZurueck) "Zurücknehmen" else "Text mit KI verbessern",
                            beiKlick = {
                                if (feld.kannZurueck) modell.nimmZurueck(modell.merkFeldFluss())
                                else modell.verbessere(modell.merkFeldFluss())
                            },
                            modifier = Modifier.weight(1f),
                            betont = false,
                            aktiv = !feld.laeuft,
                        )
                        Textknopf(
                            text = "Speichern",
                            beiKlick = {
                                modell.legeEigenesAn()
                                erfassen = false
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        wartet?.let { Karte { Wartezustand(it) } }

        if (merkliste.isEmpty() && !erfassen) {
            Karte {
                Text(
                    "Nichts gemerkt. Wenn dir ein Vorschlag gefällt, tipp auf das Lesezeichen.",
                    style = schriften.fliesstext,
                    color = farben.gedaempft,
                )
            }
        }

        merkliste.forEach { eintrag ->
            Karte(
                modifier = Modifier.pointerInput(eintrag.id) {
                    // F-19: Löschen über langen Druck. Das Wischen bleibt dem Bildschirm-
                    // wechsel (F-27) vorbehalten, damit sich die Gesten nicht beissen.
                    detectTapGestures(onLongPress = { loeschen = eintrag.id })
                },
            ) {
                Text(eintrag.title, style = schriften.kartentitel, color = farben.text)
                Text(
                    eintrag.description,
                    style = schriften.fliesstextKlein,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 8.dp),
                )
                eintrag.note?.let {
                    Text(
                        "Beim letzten Mal im Weg: $it",
                        style = schriften.fliesstextKlein,
                        color = farben.warnung,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Etikett(eintrag.level.name.lowercase())
                    Etikett(if (eintrag.days > 1) "${eintrag.days} Tage" else "1 Tag")
                }

                if (loeschen == eintrag.id) {
                    Text(
                        "Eintrag löschen?",
                        style = schriften.fliesstext,
                        color = farben.warnung,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Textknopf("Abbrechen", { loeschen = null }, Modifier.weight(1f), betont = false)
                        Textknopf(
                            text = "Löschen",
                            beiKlick = {
                                modell.loescheMerkliste(eintrag)
                                loeschen = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** **B-06 — Erkenntnisse** (F-17). Am Stück lesbar, keine Bewertung. */
@Composable
fun Erkenntnisse(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val erkenntnisse by modell.erkenntnisse.collectAsStateWithLifecycle()

    Listenbildschirm(
        titel = "Erkenntnisse",
        untertitel = "Was sich aus deinen Auswertungen ergeben hat.",
        jetzt = Ziel.ERKENNTNISSE,
        modell = modell,
    ) {
        if (erkenntnisse.isEmpty()) {
            Karte {
                Text(
                    "Noch nichts. Das wächst mit deinen Auswertungen.",
                    style = schriften.fliesstext,
                    color = farben.gedaempft,
                )
            }
        }
        erkenntnisse.forEach { erkenntnis ->
            Karte {
                Text(erkenntnis.text, style = schriften.fliesstext, color = farben.text)
                Text(
                    text = erkenntnis.updatedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    style = schriften.daten,
                    color = farben.blass,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** **B-07 — Logbuch** (F-16). Zwei Reiter: *Letzte 15 Tage* und *Langzeit*. */
@Composable
fun Logbuch(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val ausfuehrlich by modell.logAusfuehrlich.collectAsStateWithLifecycle()
    val verdichtet by modell.logVerdichtet.collectAsStateWithLifecycle()
    var langzeit by remember { mutableStateOf(false) }

    Listenbildschirm(
        titel = "Logbuch",
        untertitel = null,
        jetzt = Ziel.LOGBUCH,
        modell = modell,
    ) {
        // Die zwei Reiter: vollrund, das aktive Feld in der gedeckten Aktionsfläche.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Reiter("Letzte 15 Tage", !langzeit, Modifier.weight(1f)) { langzeit = false }
            Reiter("Langzeit", langzeit, Modifier.weight(1f)) { langzeit = true }
        }

        val tage = if (langzeit) verdichtet else ausfuehrlich
        if (tage.isEmpty()) {
            Karte {
                Text("Noch nichts aufgeschrieben.", style = schriften.fliesstext, color = farben.gedaempft)
            }
        }
        tage.forEach { tag -> Logeintrag(tag, modell) }
    }
}

@Composable
private fun Reiter(text: String, aktiv: Boolean, modifier: Modifier = Modifier, beiKlick: () -> Unit) {
    val farben = LocalFarben.current
    val lang = dauer(Bewegung.LANG)
    val flaeche by animateColorAsState(
        targetValue = if (aktiv) farben.aktionGedeckt else Color.Transparent,
        animationSpec = tween(lang, easing = Bewegung.ruhig),
        label = "reiterflaeche",
    )
    val schrift by animateColorAsState(
        targetValue = if (aktiv) farben.aktion else farben.gedaempft,
        animationSpec = tween(lang, easing = Bewegung.ruhig),
        label = "reiterschrift",
    )
    Box(
        modifier
            .clip(Formen.vollrund)
            .background(flaeche)
            .border(1.dp, if (aktiv) farben.aktion.copy(alpha = 0.55f) else farben.rand, Formen.vollrund)
            .clickable(onClick = beiKlick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LocalSchriften.current.knopf, color = schrift)
    }
}

/**
 * Ein Logbuch-Tag. Langer Druck macht ihn bearbeitbar (F-16); Löschen fragt zurück und ist
 * dann endgültig.
 *
 * Das Design zeigt für B-07 nur den leeren Zustand — die Zeilenform ist deshalb **nicht**
 * durch den Entwurf gedeckt. Gebaut in der Kartensprache der App: Datum in *Daten*,
 * Text in *Fließtext*.
 */
@Composable
private fun Logeintrag(tag: LogDay, modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    var bearbeiten by remember(tag.date) { mutableStateOf(false) }
    var loeschen by remember(tag.date) { mutableStateOf(false) }
    var text by remember(tag.date) { mutableStateOf(tag.detailText ?: tag.compactText.orEmpty()) }

    Karte(
        modifier = Modifier.pointerInput(tag.date) {
            detectTapGestures(onLongPress = { bearbeiten = true })
        },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tag.date.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)).uppercase(),
                style = schriften.daten,
                color = farben.gedaempft,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Symbole.AuswertungVorlesen,
                contentDescription = "Vorlesen",
                tint = farben.gedaempft,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { modell.lies(text) },
            )
        }
        if (bearbeiten) {
            Box(Modifier.padding(top = 12.dp)) {
                Eingabefeld(text = text, beiAenderung = { text = it }, platzhalter = "", zeilen = 8)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Textknopf("Löschen", { loeschen = true }, Modifier.weight(1f), betont = false)
                Textknopf(
                    text = "Speichern",
                    beiKlick = {
                        modell.aendereLogtag(tag, text)
                        bearbeiten = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            if (loeschen) {
                Text(
                    "Eintrag löschen? Das ist endgültig.",
                    style = schriften.fliesstext,
                    color = farben.warnung,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Textknopf("Abbrechen", { loeschen = false }, Modifier.weight(1f), betont = false)
                    Textknopf("Löschen", { modell.loescheLogtag(tag) }, Modifier.weight(1f))
                }
            }
        } else {
            Text(
                text = text,
                style = schriften.fliesstext,
                color = farben.text,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (tag.compactText != null) {
                Box(Modifier.padding(top = 12.dp)) { Zwischenueberschrift("verdichtet") }
            }
        }
    }
}
