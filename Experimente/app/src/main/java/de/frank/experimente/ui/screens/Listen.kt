package de.frank.experimente.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.data.local.LogDay
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.components.Bildschirmgeruest
import de.frank.experimente.ui.components.Einflug
import de.frank.experimente.ui.components.LeererZustand
import de.frank.experimente.ui.components.Meldungen
import de.frank.experimente.ui.components.SchwebenderPlusknopf
import de.frank.experimente.ui.components.Titel
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.lichtsaum
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TAGESFORM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)

/**
 * **B-04 — Wünsche & Ziele** (F-20). Sie gehen als voller Text in jede Vorschlagsanfrage
 * ein. Kein Fortschrittsbalken, kein Fälligkeitsdatum — es sind Ziele, keine Aufgaben.
 */
@Composable
fun WuenscheUndZiele(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val ziele by modell.ziele.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()
    val feld by modell.zielFeld.collectAsStateWithLifecycle()
    var blattOffen by remember { mutableStateOf(false) }

    Bildschirmgeruest(
        kopf = { Titel("Wünsche & Ziele") },
        leiste = Ziel.ZIELE,
        beiLeistenwahl = modell::gehe,
        ueberlagerung = {
            SchwebenderPlusknopf("Ziel anlegen") { blattOffen = true }
            Meldungen(stoerung = null, beiNochmal = modell::schliesseMeldung, hinweis = meldung)
            if (blattOffen) {
                Anlegeblatt(
                    titel = "Neues Ziel",
                    unterzeile = "Ein Wunsch oder ein Ziel, an dem sich die Vorschläge ausrichten.",
                    platzhalter = "Was willst du erreichen?",
                    sprechbeschriftung = "Ziel einsprechen",
                    feld = feld,
                    modell = modell,
                    beiSprechen = { modell.sprechknopf(modell.zielFeldFluss()) },
                    beiAenderung = { modell.setzeText(modell.zielFeldFluss(), it) },
                    beiVerbessern = {
                        if (feld.kannZurueck) modell.nimmZurueck(modell.zielFeldFluss())
                        else modell.verbessere(modell.zielFeldFluss())
                    },
                    // F-20 Schritt 2: nach dem Speichern steht der Sprechknopf sofort wieder
                    // bereit — deshalb bleibt das Blatt offen.
                    beiSpeichern = { modell.legeZielAn() },
                    beiSchliessen = { blattOffen = false },
                )
            }
        },
    ) {
        if (ziele.isEmpty()) {
            item("leer") { LeererZustand("Noch keine Ziele. Sprich eines ein.") }
        }
        items(ziele, key = { it.id }) { ziel ->
            Einflug(ziele.indexOf(ziel)) {
                Listenkarte {
                    Text(
                        text = ziel.text.lineSequence().first().take(80),
                        style = schriften.kartentitel,
                        color = farben.text,
                    )
                    Text(
                        text = ziel.text,
                        style = schriften.kartentext,
                        color = farben.gedaempft,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "seit ${TAGESFORM.format(ziel.createdAt.atZone(java.time.ZoneId.systemDefault()))}",
                        style = schriften.stufe,
                        color = farben.blass,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

/**
 * **B-05 — Merkliste** (F-18, F-19). Der Ort für Ideen *ohne* Vorsatz; der Monitor ist der
 * für Vorhaben *mit* Vorsatz.
 */
@Composable
fun Merkliste(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val merkliste by modell.merkliste.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()
    val feld by modell.merkFeld.collectAsStateWithLifecycle()
    var blattOffen by remember { mutableStateOf(false) }

    Bildschirmgeruest(
        kopf = { Titel("Merkliste") },
        leiste = Ziel.MERKLISTE,
        beiLeistenwahl = modell::gehe,
        ueberlagerung = {
            SchwebenderPlusknopf("Eigenes Experiment anlegen") { blattOffen = true }
            Meldungen(stoerung = null, beiNochmal = modell::schliesseMeldung, hinweis = meldung)
            if (blattOffen) {
                Anlegeblatt(
                    titel = "Eigenes Experiment",
                    unterzeile = "Einsprechen oder tippen. Es liegt danach auf der Merkliste.",
                    platzhalter = "Was willst du ausprobieren?",
                    sprechbeschriftung = "Eigenes Experiment einsprechen",
                    feld = feld,
                    modell = modell,
                    beiSprechen = { modell.sprechknopf(modell.merkFeldFluss()) },
                    beiAenderung = { modell.setzeText(modell.merkFeldFluss(), it) },
                    beiVerbessern = {
                        if (feld.kannZurueck) modell.nimmZurueck(modell.merkFeldFluss())
                        else modell.verbessere(modell.merkFeldFluss())
                    },
                    beiSpeichern = { modell.legeEigenesAn(); blattOffen = false },
                    beiSchliessen = { blattOffen = false },
                )
            }
        },
    ) {
        items(merkliste, key = { it.id }) { eintrag ->
            Einflug(merkliste.indexOf(eintrag)) {
                Listenkarte {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(eintrag.title, style = schriften.kartentitel, color = farben.text)
                            Text(
                                text = modell.merkMeta(eintrag),
                                style = schriften.stufe,
                                color = farben.gedaempft,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable { modell.loescheMerkliste(eintrag) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Symbole.Loeschen,
                                contentDescription = "Löschen",
                                tint = farben.blass,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
        if (merkliste.isEmpty()) {
            item("leer") { LeererZustand("Die Merkliste ist leer.") }
        }
    }
}

/**
 * **B-06 — Erkenntnisse** (F-17). Sätze über Frank, nicht über seine Leistung — am Stück
 * lesbar, ohne Karten, nur durch feine Linien getrennt.
 */
@Composable
fun Erkenntnisse(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val erkenntnisse by modell.erkenntnisse.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()

    Bildschirmgeruest(
        kopf = { Titel("Erkenntnisse") },
        leiste = Ziel.ERKENNTNISSE,
        beiLeistenwahl = modell::gehe,
        ueberlagerung = {
            Meldungen(stoerung = null, beiNochmal = modell::schliesseMeldung, hinweis = meldung)
        },
    ) {
        if (erkenntnisse.isEmpty()) {
            item("leer") {
                LeererZustand("Noch keine Erkenntnisse. Sie wachsen aus den Auswertungen.")
            }
        }
        items(erkenntnisse, key = { it.id }) { erkenntnis ->
            Einflug(erkenntnisse.indexOf(erkenntnis)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
                    Text(
                        text = TAGESFORM.format(
                            erkenntnis.updatedAt.atZone(java.time.ZoneId.systemDefault()),
                        ),
                        style = schriften.stufe,
                        color = farben.blass,
                    )
                    Text(
                        text = erkenntnis.text,
                        style = schriften.fliesstext,
                        color = farben.text,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .height(1.dp)
                            .background(farben.randWeich),
                    )
                }
            }
        }
    }
}

/**
 * **B-07 — Logbuch** (F-31, F-32). Zwei Reiter: *Letzte 15 Tage* zeigt die ausführlichen
 * Tage, *Langzeit* die verdichteten. Nichts fällt heraus.
 */
@Composable
fun Logbuch(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val ausfuehrlich by modell.logAusfuehrlich.collectAsStateWithLifecycle()
    val verdichtet by modell.logVerdichtet.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()
    var langzeit by remember { mutableStateOf(false) }

    val eintraege = if (langzeit) verdichtet else ausfuehrlich

    Bildschirmgeruest(
        kopf = { Titel("Logbuch") },
        leiste = Ziel.LOGBUCH,
        beiLeistenwahl = modell::gehe,
        unterKopf = {
            // Die Reiter liegen im Entwurf direkt unter der Kopfleiste, 16 dp vom Rand.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(farben.text.copy(alpha = 0.08f))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Reiter("Letzte 15 Tage", !langzeit, Modifier.weight(1f)) { langzeit = false }
                Reiter("Langzeit", langzeit, Modifier.weight(1f)) { langzeit = true }
            }
        },
        ueberlagerung = {
            Meldungen(stoerung = null, beiNochmal = modell::schliesseMeldung, hinweis = meldung)
        },
    ) {
        if (eintraege.isEmpty()) {
            item("leer") { LeererZustand("Das Logbuch beginnt mit dem ersten Tag.") }
        }
        items(eintraege, key = { it.date.toString() }) { tag ->
            Einflug(eintraege.indexOf(tag)) { Logeintrag(tag, langzeit) }
        }
    }
}

/** Ein Reiter: 40 dp hoch, vollrund, Inter 14/20; aktiv trägt er die Fläche *Fläche*. */
@Composable
private fun Reiter(text: String, aktiv: Boolean, modifier: Modifier = Modifier, beiKlick: () -> Unit) {
    val farben = LocalFarben.current
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (aktiv) farben.flaeche else Color.Transparent)
            .clickable(onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalSchriften.current.reiter,
            color = if (aktiv) farben.text else farben.gedaempft,
            maxLines = 1,
        )
    }
}

/** Ein Logbuch-Eintrag: Datum in *Aktion*, Titel in Fraunces, Text in *Gedämpft*. */
@Composable
private fun Logeintrag(tag: LogDay, langzeit: Boolean) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val text = (if (langzeit) tag.compactText else tag.detailText).orEmpty()
    Listenkarte {
        Text(TAGESFORM.format(tag.date), style = schriften.stufe, color = farben.aktion)
        Text(
            text = text.lineSequence().first().take(90),
            style = schriften.kartentitel,
            color = farben.text,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = text,
            style = schriften.kartentext,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Die schlichte Listenkarte des Entwurfs: 1 dp Rand, Radius 20 dp, Innenabstand 18 dp. */
@Composable
private fun Listenkarte(inhalt: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val farben = LocalFarben.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp))
            .lichtsaum(farben.text, 0.12f)
            .padding(18.dp),
        content = inhalt,
    )
}
