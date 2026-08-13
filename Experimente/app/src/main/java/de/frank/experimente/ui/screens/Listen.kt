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
import de.frank.experimente.data.local.AuswertungMitTitel
import de.frank.experimente.data.local.LogDay
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.components.Bildschirmgeruest
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.Einflug
import de.frank.experimente.ui.components.KnopfBetont
import de.frank.experimente.ui.components.KnopfUmrandet
import de.frank.experimente.ui.components.LeererZustand
import de.frank.experimente.ui.components.Meldungen
import de.frank.experimente.ui.components.SchwebenderPlusknopf
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.Titel
import de.frank.experimente.ui.components.Vorleseknopf
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.lichtsaum
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TAGESFORM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)

/** Datum **und** Uhrzeit — an einem Tag können mehrere Auswertungen entstehen. */
private val ZEITFORM: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMAN)

/**
 * **B-04 — Wünsche & Ziele** (F-20). Sie gehen als voller Text in jede Vorschlagsanfrage
 * ein. Kein Fortschrittsbalken, kein Fälligkeitsdatum — es sind Ziele, keine Aufgaben.
 */
@Composable
fun WuenscheUndZiele(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val ziele by modell.ziele.collectAsStateWithLifecycle()
    val hinweis by modell.hinweis.collectAsStateWithLifecycle()
    val stoerung by modell.stoerung.collectAsStateWithLifecycle()
    val feld by modell.zielFeld.collectAsStateWithLifecycle()
    var blattOffen by remember { mutableStateOf(false) }

    Bildschirmgeruest(
        kopf = { Titel("Wünsche & Ziele") },
        leiste = Ziel.ZIELE,
        beiLeistenwahl = modell::gehe,
        ueberlagerung = {
            SchwebenderPlusknopf("Ziel anlegen") { blattOffen = true }
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
            // Ueber der Anlegeflaeche, damit die Stoerung sichtbar und bedienbar bleibt.
            Meldungen(stoerung = stoerung, beiNochmal = modell::schliesseMeldung, hinweis = hinweis)
        },
    ) {
        if (ziele.isEmpty()) {
            item("leer") { LeererZustand("Noch keine Ziele. Sprich eines ein.") }
        }
        items(ziele, key = { it.id }) { ziel ->
            Einflug(ziele.indexOf(ziel)) {
                // F-20 — ändern und löschen waren im Modell fertig, aber von keinem Knopf
                // aus erreichbar: ein einmal eingesprochenes Ziel liess sich nie mehr
                // berichtigen, obwohl es in jede Anfrage eingeht.
                var bearbeitet by remember(ziel.id) { mutableStateOf<String?>(null) }
                Listenkarte {
                    if (bearbeitet == null) {
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
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "seit ${TAGESFORM.format(ziel.createdAt.atZone(java.time.ZoneId.systemDefault()))}",
                                style = schriften.stufe,
                                color = farben.blass,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Textknopf("Ändern", { bearbeitet = ziel.text })
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .clickable { modell.loescheZiel(ziel) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Symbole.Loeschen,
                                        contentDescription = "Ziel löschen",
                                        tint = farben.blass,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        Eingabefeld(
                            text = bearbeitet.orEmpty(),
                            beiAenderung = { bearbeitet = it },
                            platzhalter = "Was willst du erreichen?",
                            mindesthoehe = 120.dp,
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            KnopfUmrandet("Abbrechen", { bearbeitet = null }, Modifier.weight(1f))
                            KnopfBetont(
                                text = "Sichern",
                                beiKlick = {
                                    val neu = bearbeitet.orEmpty().trim()
                                    if (neu.isNotBlank()) modell.aendereZiel(ziel, neu)
                                    bearbeitet = null
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
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
    val hinweis by modell.hinweis.collectAsStateWithLifecycle()
    val stoerung by modell.stoerung.collectAsStateWithLifecycle()
    val feld by modell.merkFeld.collectAsStateWithLifecycle()
    val tage by modell.merkTage.collectAsStateWithLifecycle()
    var blattOffen by remember { mutableStateOf(false) }

    Bildschirmgeruest(
        kopf = { Titel("Merkliste") },
        leiste = Ziel.MERKLISTE,
        beiLeistenwahl = modell::gehe,
        ueberlagerung = {
            SchwebenderPlusknopf("Eigenes Experiment anlegen") { blattOffen = true }
            if (blattOffen) {
                Anlegeblatt(
                    titel = "Eigenes Experiment",
                    unterzeile = "Einsprechen oder tippen. Es liegt danach auf der Merkliste.",
                    platzhalter = "Was willst du ausprobieren?",
                    sprechbeschriftung = "Eigenes Experiment einsprechen",
                    feld = feld,
                    modell = modell,
                    tage = tage,
                    beiTage = modell::setzeMerkTage,
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
            // Ueber der Anlegeflaeche, damit die Stoerung sichtbar und bedienbar bleibt.
            Meldungen(stoerung = stoerung, beiNochmal = modell::schliesseMeldung, hinweis = hinweis)
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
    val hinweis by modell.hinweis.collectAsStateWithLifecycle()
    val stoerung by modell.stoerung.collectAsStateWithLifecycle()
    val liest by modell.liestKennung.collectAsStateWithLifecycle()

    Bildschirmgeruest(
        kopf = { Titel("Erkenntnisse") },
        leiste = Ziel.ERKENNTNISSE,
        beiLeistenwahl = modell::gehe,
        ueberlagerung = {
            Meldungen(stoerung = stoerung, beiNochmal = modell::schliesseMeldung, hinweis = hinweis)
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
                    // Datum links, Lautsprecher rechts — die Zeile trägt beides, ohne dass
                    // der Fließtext seine Ruhe verliert.
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = TAGESFORM.format(
                                erkenntnis.updatedAt.atZone(java.time.ZoneId.systemDefault()),
                            ),
                            style = schriften.stufe,
                            color = farben.blass,
                        )
                        Vorleseknopf(
                            spricht = liest == "erkenntnis-${erkenntnis.id}",
                            beiKlick = {
                                modell.liesVor("erkenntnis-${erkenntnis.id}", erkenntnis.text)
                            },
                            beschriftung = "Erkenntnis vorlesen",
                        )
                    }
                    Text(
                        text = erkenntnis.text,
                        style = schriften.fliesstext,
                        color = farben.text,
                        modifier = Modifier.padding(top = 4.dp),
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

/** Die drei Reiter von B-07. */
private enum class Logreiter(val beschriftung: String) {
    // Kurz gehalten: zu dritt bleibt je Reiter nur ein Drittel der Breite, und ein
    // abgeschnittenes „Letzte 15 Ta…" sagt weniger als „15 Tage".
    AKTUELL("15 Tage"),
    LANGZEIT("Langzeit"),
    AUSWERTUNGEN("Auswertungen"),
}

/**
 * **B-07 — Logbuch** (F-31, F-32). Drei Reiter: *Letzte 15 Tage* zeigt die ausführlichen
 * Tage, *Langzeit* die verdichteten, *Auswertungen* jede Auswertung im vollen Wortlaut.
 * Nichts fällt heraus.
 *
 * Der dritte Reiter kam dazu, weil eine abgeschlossene Auswertung sonst nirgends mehr zu
 * finden war: sie stand in der Ablage, aber mit dem Abschluss verschwand die Karte aus dem
 * Monitor — und über sie führte der einzige Weg dorthin. Die verdichtete Fassung im
 * Tageseintrag ersetzt den Wortlaut nicht.
 */
@Composable
fun Logbuch(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val ausfuehrlich by modell.logAusfuehrlich.collectAsStateWithLifecycle()
    val verdichtet by modell.logVerdichtet.collectAsStateWithLifecycle()
    val auswertungen by modell.alleAuswertungen.collectAsStateWithLifecycle()
    val hinweis by modell.hinweis.collectAsStateWithLifecycle()
    val stoerung by modell.stoerung.collectAsStateWithLifecycle()
    val liest by modell.liestKennung.collectAsStateWithLifecycle()
    var reiter by remember { mutableStateOf(Logreiter.AKTUELL) }

    val eintraege = if (reiter == Logreiter.LANGZEIT) verdichtet else ausfuehrlich

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
                Logreiter.entries.forEach { welcher ->
                    Reiter(welcher.beschriftung, reiter == welcher, Modifier.weight(1f)) {
                        reiter = welcher
                    }
                }
            }
        },
        ueberlagerung = {
            Meldungen(stoerung = stoerung, beiNochmal = modell::schliesseMeldung, hinweis = hinweis)
        },
    ) {
        if (reiter == Logreiter.AUSWERTUNGEN) {
            if (auswertungen.isEmpty()) {
                item("leer-auswertungen") {
                    LeererZustand("Noch keine Auswertung. Sie entsteht, wenn du erzählst, wie es gelaufen ist.")
                }
            }
            items(auswertungen, key = { "aus${it.auswertung.id}" }) { eintrag ->
                Einflug(auswertungen.indexOf(eintrag)) {
                    Auswertungseintrag(eintrag, modell, liest)
                }
            }
            return@Bildschirmgeruest
        }

        if (eintraege.isEmpty()) {
            item("leer") { LeererZustand("Das Logbuch beginnt mit dem ersten Tag.") }
        }
        items(eintraege, key = { it.date.toString() }) { tag ->
            Einflug(eintraege.indexOf(tag)) {
                Logeintrag(tag, reiter == Logreiter.LANGZEIT, modell, liest)
            }
        }
    }
}

/**
 * Ein Eintrag im Reiter *Auswertungen*: Zeitpunkt, Experiment, Franks Wortlaut und die
 * vollständige Einschätzung — ungekürzt. Aus diesem Bestand werden die Erkenntnisse gezogen.
 */
@Composable
private fun Auswertungseintrag(
    eintrag: AuswertungMitTitel,
    modell: AppViewModel,
    liest: String?,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val auswertung = eintrag.auswertung
    val zeitpunkt = auswertung.createdAt
        ?.atZone(java.time.ZoneId.systemDefault())
        ?.let { ZEITFORM.format(it) }
        // Auswertungen aus der Zeit vor dem Zeitstempel tragen nur ihren Kalendertag. Eine
        // erfundene Uhrzeit wäre schlimmer als keine.
        ?: TAGESFORM.format(auswertung.date)

    Listenkarte {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(zeitpunkt, style = schriften.stufe, color = farben.aktion)
            if (auswertung.isFinal) {
                Text("Abschluss", style = schriften.stufe, color = farben.erledigt)
            }
        }
        // Titel und der Lautsprecher für die ganze Auswertung — er liest beides vor, den
        // eigenen Text und die Einschätzung, in der Reihenfolge, in der sie dastehen.
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = eintrag.experimentTitel ?: "Gelöschtes Experiment",
                style = schriften.kartentitel,
                color = farben.text,
                modifier = Modifier.weight(1f),
            )
            Vorleseknopf(
                spricht = liest == "logaus-${auswertung.id}",
                beiKlick = {
                    modell.liesVor(
                        "logaus-${auswertung.id}",
                        buildString {
                            append(auswertung.ownText)
                            auswertung.aiText?.takeIf { it.isNotBlank() }?.let {
                                append("\n\nEinschätzung: ")
                                append(it)
                            }
                        },
                    )
                },
                beschriftung = "Ganze Auswertung vorlesen",
            )
        }

        Text(
            text = "Was ich erzählt habe".uppercase(),
            style = schriften.zwischenueberschrift,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = auswertung.ownText,
            style = schriften.kartentext,
            color = farben.text,
            modifier = Modifier.padding(top = 6.dp),
        )

        auswertung.aiText?.takeIf { it.isNotBlank() }?.let { einschaetzung ->
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Einschätzung".uppercase(),
                    style = schriften.zwischenueberschrift,
                    color = farben.gedaempft,
                )
                // Zweiter Knopf: nur die Einschätzung, ohne den eigenen Text davor.
                Vorleseknopf(
                    spricht = liest == "logki-${auswertung.id}",
                    beiKlick = { modell.liesVor("logki-${auswertung.id}", einschaetzung) },
                    beschriftung = "Nur die Einschätzung vorlesen",
                )
            }
            // Ungekürzt: der Reiter ist der Ort, an dem der volle Wortlaut steht.
            Text(
                text = einschaetzung,
                style = schriften.kartentext,
                color = farben.gedaempft,
                modifier = Modifier.padding(top = 6.dp),
            )
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

/**
 * Ein Logbuch-Eintrag: Datum in *Aktion*, Titel in Fraunces, Text in *Gedämpft*.
 *
 * **F-16 — Ändern und Löschen.** Beide Wege waren im Modell und in der Ablage fertig gebaut,
 * aber hier hing kein Knopf daran: das Logbuch war nur lesbar. Es geht in jede Anfrage ein,
 * eine falsche Zeile wirkte also dauerhaft weiter, ohne dass sie sich berichtigen liess.
 */
@Composable
private fun Logeintrag(tag: LogDay, langzeit: Boolean, modell: AppViewModel, liest: String?) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val text = (if (langzeit) tag.compactText else tag.detailText).orEmpty()
    var bearbeitet by remember(tag.date, langzeit) { mutableStateOf<String?>(null) }
    var fragtLoeschen by remember(tag.date) { mutableStateOf(false) }
    val kennung = "logtag-${tag.date}-${if (langzeit) "lang" else "kurz"}"

    Listenkarte {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(TAGESFORM.format(tag.date), style = schriften.stufe, color = farben.aktion)
            Vorleseknopf(
                spricht = liest == kennung,
                beiKlick = { modell.liesVor(kennung, text) },
                beschriftung = "Diesen Tag vorlesen",
            )
        }

        if (bearbeitet == null) {
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
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Textknopf("Ändern", { bearbeitet = text })
                Box(Modifier.weight(1f))
                if (fragtLoeschen) {
                    // Ein Logbuchtag ist endgültig weg — deshalb wird einmal nachgefragt.
                    Textknopf("Wirklich löschen", {
                        fragtLoeschen = false
                        modell.loescheLogtag(tag)
                    }, farbe = farben.warnung)
                    Textknopf("Behalten", { fragtLoeschen = false })
                } else {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable { fragtLoeschen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Symbole.Loeschen,
                            contentDescription = "Diesen Tag löschen",
                            tint = farben.blass,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        } else {
            Eingabefeld(
                text = bearbeitet.orEmpty(),
                beiAenderung = { bearbeitet = it },
                platzhalter = "Was an diesem Tag war.",
                mindesthoehe = 160.dp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KnopfUmrandet("Abbrechen", { bearbeitet = null }, Modifier.weight(1f))
                KnopfBetont(
                    text = "Sichern",
                    beiKlick = {
                        val neu = bearbeitet.orEmpty()
                        if (neu.isNotBlank()) modell.aendereLogtag(tag, neu)
                        bearbeitet = null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
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
