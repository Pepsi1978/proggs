package de.frank.stacklabor.ui.blaetter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.daten.entitaeten.BewertungE
import de.frank.stacklabor.daten.entitaeten.BewertungMitInhalt
import de.frank.stacklabor.daten.entitaeten.BewertungszelleE
import de.frank.stacklabor.daten.entitaeten.Wirkung as WirkungE
import de.frank.stacklabor.logik.Ampel
import de.frank.stacklabor.logik.Ampelrechnung
import de.frank.stacklabor.logik.Wirkung
import de.frank.stacklabor.logik.Zelle
import de.frank.stacklabor.ui.bausteine.Haekchen
import de.frank.stacklabor.ui.bausteine.ampelFarbe
import de.frank.stacklabor.ui.bausteine.ampelTextFarbe
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift

/**
 * **B-15 — Auswertungs-Historie (F-29).**
 *
 * Fuenf Laeufe werden je Stack aufbewahrt; der aelteste faellt heraus. Der eigentliche Wert
 * steckt im **Vergleich**: Er beantwortet die Frage „was hat meine Aenderung bewirkt?" —
 * deshalb werden die Unterschiede je Ziel benannt („Ziel 4: gelb → grün") statt nur Zahlen
 * gegenueberzustellen.
 */
@Composable
fun HistorieBlatt(
    app: StackLaborApp,
    stackId: String,
    aufOeffnen: (bewertungId: String) -> Unit,
    aufSchliessen: () -> Unit,
) {
    val laeufe by app.datenbank.bewertungDao().historie(stackId, 5)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val ziele by app.datenbank.zielDao().zieleDesStacks(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val eintraege by app.datenbank.stackDao().eintraegeMitMittel(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Die Zellen aller fuenf Laeufe einmalig nachladen — die Kurzbilanz braucht sie.
    var inhalte by remember { mutableStateOf(mapOf<String, BewertungMitInhalt>()) }
    LaunchedEffect(laeufe) {
        val dao = app.datenbank.bewertungDao()
        val gesammelt = mutableMapOf<String, BewertungMitInhalt>()
        laeufe.forEach { lauf ->
            dao.bewertungMitInhaltEinmalig(lauf.id)?.let { gesammelt[lauf.id] = it }
        }
        inhalte = gesammelt
    }

    var markiert by remember { mutableStateOf(listOf<String>()) }
    var vergleichOffen by remember { mutableStateOf(false) }
    var grundGezeigt by remember { mutableStateOf(false) }

    val aktive = eintraege.filter { it.eintrag.aktiv }.map { it.eintrag.mittelId }.toSet()
    val vergleichMoeglich = laeufe.size >= 2 && markiert.size == 2

    BlattRahmen(
        titel = "Auswertungs-Historie",
        untertitel = when {
            laeufe.isEmpty() -> "kein Lauf"
            laeufe.size == 1 -> "1 Lauf"
            else -> "${laeufe.size} Läufe"
        },
        aufSchliessen = aufSchliessen,
        sockel = { _ ->
            Column {
                if (grundGezeigt && !vergleichMoeglich) {
                    Text(
                        if (laeufe.size < 2) "Für einen Vergleich braucht es zwei Läufe."
                        else "Bitte genau zwei Läufe markieren.",
                        style = Schrift.klein,
                        color = Farben.gelbText,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                BlattKnopf(
                    beschriftung = if (vergleichOffen) "Vergleich schließen" else "Zwei Läufe vergleichen",
                    gesperrt = !vergleichMoeglich && !vergleichOffen,
                ) {
                    when {
                        vergleichOffen -> vergleichOffen = false
                        vergleichMoeglich -> {
                            vergleichOffen = true
                            grundGezeigt = false
                        }
                        else -> grundGezeigt = true
                    }
                }
            }
        },
    ) { _ ->
        if (laeufe.isEmpty()) {
            BlattHinweis("Dieser Stack wurde noch nicht ausgewertet.")
            Spacer(Modifier.height(16.dp))
            return@BlattRahmen
        }

        BlattHinweis("Tippen öffnet den Lauf, das Kästchen markiert ihn für den Vergleich.")

        laeufe.forEach { lauf ->
            val zellen = inhalte[lauf.id]?.zellen.orEmpty()
            LaufZeile(
                lauf = lauf,
                bilanz = kurzbilanz(zellen, ziele.map { it.ziel.id }, aktive),
                markiert = lauf.id in markiert,
                aufMarkieren = {
                    markiert = when {
                        lauf.id in markiert -> markiert - lauf.id
                        markiert.size < 2 -> markiert + lauf.id
                        // Der dritte Griff verdraengt die aelteste Markierung.
                        else -> markiert.drop(1) + lauf.id
                    }
                    if (markiert.size != 2) vergleichOffen = false
                },
                aufOeffnen = { aufOeffnen(lauf.id) },
            )
        }

        if (laeufe.size < 2) {
            BlattHinweis("Der Vergleich wird ab dem zweiten Lauf möglich.")
        }

        if (vergleichOffen && markiert.size == 2) {
            BlattTrenner()
            BlattRubrik("Unterschiede")

            // Immer vom aelteren zum juengeren Lauf lesen — sonst stimmt der Pfeil nicht.
            val gewaehlt = laeufe.filter { it.id in markiert }.sortedBy { it.zeitpunkt }
            val alt = gewaehlt.firstOrNull()
            val neu = gewaehlt.lastOrNull()
            if (alt != null && neu != null) {
                BlattHinweis(
                    "${blattZeitstempel(alt.zeitpunkt)} → ${blattZeitstempel(neu.zeitpunkt)}",
                )
                val zellenAlt = inhalte[alt.id]?.zellen.orEmpty()
                val zellenNeu = inhalte[neu.id]?.zellen.orEmpty()
                var unterschiede = 0
                ziele.forEach { zr ->
                    val a = Ampelrechnung.ampelZiel(zr.ziel.id, zellenAlt.alsZellen(), aktive)
                    val b = Ampelrechnung.ampelZiel(zr.ziel.id, zellenNeu.alsZellen(), aktive)
                    if (a != b) {
                        unterschiede++
                        UnterschiedsZeile(zr.rang, zr.ziel.text, a, b)
                    }
                }
                if (unterschiede == 0) {
                    BlattHinweis("Kein Ziel hat seine Ampel geändert.")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Eine Zeile der Historie: Zeitpunkt, Modell, Kurzbilanz, Markierung. */
@Composable
private fun LaufZeile(
    lauf: BewertungE,
    bilanz: String,
    markiert: Boolean,
    aufMarkieren: () -> Unit,
    aufOeffnen: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .heightIn(min = Masse.tippflaeche + 12.dp)
            .clip(Formen.karte)
            .background(if (markiert) Farben.akzent.copy(alpha = 0.08f) else Farben.flaeche)
            .border(1.dp, if (markiert) Farben.akzent else Farben.rand, Formen.karte),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = aufOeffnen)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        ) {
            Text(
                "${blattZeitstempel(lauf.zeitpunkt)} · ${lauf.modell}",
                style = Schrift.grundMittel,
                color = Farben.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (lauf.denkstufe.isBlank()) bilanz else "$bilanz · ${lauf.denkstufe}",
                style = Schrift.klein,
                color = Farben.textSchwach,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Haekchen(
            gesetzt = markiert,
            aufKlick = aufMarkieren,
            beschreibung = if (markiert) "für den Vergleich markiert, Markierung aufheben"
            else "für den Vergleich markieren",
        )
    }
}

/** „Ziel 4: gelb → grün" — mit Kantenbalken in der neuen Farbe. */
@Composable
private fun UnterschiedsZeile(nummer: Int, zieltext: String, vorher: Ampel, nachher: Ampel) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .height(IntrinsicSize.Min)
            .clip(Formen.karte)
            .background(Farben.flaeche)
            .border(1.dp, Farben.rand, Formen.karte),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(Masse.ampelBalken)
                .background(ampelFarbe(nachher), Formen.ampelBalken),
        )
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                "Ziel $nummer: ${ampelWort(vorher)} → ${ampelWort(nachher)}",
                style = Schrift.grundMittel,
                color = ampelTextFarbe(nachher),
                maxLines = 1,
            )
            Text(
                zieltext,
                style = Schrift.klein,
                color = Farben.textSchwach,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Die Kurzbilanz eines Laufs: „4 grün · 1 gelb · 0 rot" (Graue nur, wenn es welche gibt). */
private fun kurzbilanz(
    zellen: List<BewertungszelleE>,
    zielIds: List<String>,
    aktiveMittel: Set<String>,
): String {
    val gerechnet = zellen.alsZellen()
    var gruen = 0
    var gelb = 0
    var rot = 0
    var grau = 0
    zielIds.forEach { zielId ->
        when (Ampelrechnung.ampelZiel(zielId, gerechnet, aktiveMittel)) {
            Ampel.GRUEN -> gruen++
            Ampel.GELB -> gelb++
            Ampel.ROT -> rot++
            Ampel.GRAU -> grau++
        }
    }
    val kern = "$gruen grün · $gelb gelb · $rot rot"
    return if (grau > 0) "$kern · $grau nicht bedient" else kern
}

/** Gespeicherte Zellen in die Rechengestalt bringen — dieselbe Umwandlung wie im Repository. */
private fun List<BewertungszelleE>.alsZellen(): List<Zelle> = map { z ->
    Zelle(
        mittelId = z.mittelId,
        zielId = z.zielId,
        wirkung = if (z.wirkung == WirkungE.STUETZT) Wirkung.STUETZT else Wirkung.STOERT,
        staerke = z.staerke,
        grund = z.grund,
    )
}

private fun ampelWort(ampel: Ampel): String = when (ampel) {
    Ampel.GRUEN -> "grün"
    Ampel.GELB -> "gelb"
    Ampel.ROT -> "rot"
    Ampel.GRAU -> "nicht bedient"
}
