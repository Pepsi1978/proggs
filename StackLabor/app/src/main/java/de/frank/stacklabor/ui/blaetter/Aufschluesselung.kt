package de.frank.stacklabor.ui.blaetter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.daten.entitaeten.BewertungszelleE
import de.frank.stacklabor.daten.entitaeten.Wirkung
import de.frank.stacklabor.ui.NummernKreis
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.delay

/**
 * Die zwei Leserichtungen der Aufschluesselung (F-15).
 *
 * Es ist bewusst **ein** Bildschirm mit einem Richtungsparameter und nicht zwei Bildschirme:
 * Aufbau, Urteilsmarken und Begruendungen sind identisch — nur die Achse dreht sich.
 */
enum class Richtung { MITTEL_ZU_ZIELEN, ZIEL_ZU_MITTELN }

/**
 * **B-06 — Aufschluesselung (F-15).**
 *
 * Zeigt die vollstaendige Begruendung, die hinter einer Ampel steht. Neutrale Paare bleiben
 * zunaechst verborgen — bei 30 Zielen waere die Liste sonst unlesbar, und die Aussage
 * „neutral" ist die uninteressanteste von dreien.
 */
@Composable
fun AufschluesselungsBlatt(
    app: StackLaborApp,
    stackId: String,
    richtung: Richtung,
    gegenstandId: String,
    aufSchliessen: () -> Unit,
) {
    val bewertung by app.datenbank.bewertungDao().juengsteBewertungMitInhalt(stackId)
        .collectAsStateWithLifecycle(initialValue = null)
    val ziele by app.datenbank.zielDao().zieleDesStacks(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val eintraege by app.datenbank.stackDao().eintraegeMitMittel(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var neutraleZeigen by remember { mutableStateOf(false) }

    // F-23: Passt der gespeicherte Lauf noch zum heutigen Stack?
    var veraltet by remember { mutableStateOf(false) }
    val laufId = bewertung?.bewertung?.id
    LaunchedEffect(laufId, eintraege, ziele) {
        val b = bewertung
        veraltet = b != null && app.repository.pruefsummeFuer(stackId) != b.bewertung.pruefsumme
    }

    // M-10: Beim Oeffnen leuchten der Gegenstand und die betroffenen Zeilen kurz auf.
    val reduziert = LocalBewegungReduziert.current
    val leuchten = remember { Animatable(0f) }
    LaunchedEffect(laufId) {
        if (reduziert || laufId == null) return@LaunchedEffect
        leuchten.animateTo(1f, tween(Dauer.VERBINDUNG_EIN, easing = Kurven.zustand))
        delay(Dauer.VERBINDUNG_HALT.toLong())
        leuchten.animateTo(0f, tween(Dauer.VERBINDUNG_AUS, easing = Kurven.zustand))
    }

    val gegenstandName = when (richtung) {
        Richtung.MITTEL_ZU_ZIELEN ->
            eintraege.firstOrNull { it.eintrag.mittelId == gegenstandId }?.mittel?.name.orEmpty()
        Richtung.ZIEL_ZU_MITTELN ->
            ziele.firstOrNull { it.ziel.id == gegenstandId }?.ziel?.text.orEmpty()
    }

    BlattRahmen(
        titel = "Aufschlüsselung",
        untertitel = when (richtung) {
            Richtung.MITTEL_ZU_ZIELEN -> "Mittel → Ziele"
            Richtung.ZIEL_ZU_MITTELN -> "Ziel → Mittel"
        },
        aufSchliessen = aufSchliessen,
    ) { _ ->
        val lauf = bewertung

        // Der Gegenstand, um den es geht — er leuchtet beim Oeffnen mit (M-10).
        GegenstandsZeile(gegenstandName.ifBlank { "Unbekannt" }, leuchten.value)

        if (lauf == null) {
            BlattHinweis("Dieser Stack wurde noch nicht ausgewertet.")
            Spacer(Modifier.height(8.dp))
            BlattHinweis(
                "Sobald der Stack ausgewertet wurde, steht hier zu jedem Paar das Urteil samt Begründung.",
            )
            Spacer(Modifier.height(16.dp))
            return@BlattRahmen
        }

        Metazeile(
            "ausgewertet ${blattZeitstempel(lauf.bewertung.zeitpunkt)} · ${lauf.bewertung.modell}",
        )
        if (veraltet) {
            BlattHinweis(
                "Dieser Stack hat sich seit der Auswertung geändert — die Urteile sind veraltet.",
                farbe = Farben.gelbText,
            )
        }

        BlattSchalterZeile("Auch neutrale zeigen", neutraleZeigen) { neutraleZeigen = it }
        BlattTrenner()

        val zellen = lauf.zellen
        var gezeigt = 0

        when (richtung) {
            Richtung.MITTEL_ZU_ZIELEN -> {
                ziele.forEach { zr ->
                    val zelle = zellen.firstOrNull {
                        it.mittelId == gegenstandId && it.zielId == zr.ziel.id
                    }
                    if (zelle != null || neutraleZeigen) {
                        gezeigt++
                        UrteilsBlock(
                            nummer = zr.rang,
                            text = zr.ziel.text,
                            zelle = zelle,
                            zusatz = "",
                            leuchten = if (zelle != null) leuchten.value else 0f,
                            abgeschaltet = false,
                        )
                    }
                }
                if (ziele.isEmpty()) BlattHinweis("Diesem Stack ist noch kein Ziel zugeordnet.")
            }

            Richtung.ZIEL_ZU_MITTELN -> {
                eintraege.forEach { em ->
                    val zelle = zellen.firstOrNull {
                        it.zielId == gegenstandId && it.mittelId == em.eintrag.mittelId
                    }
                    if (zelle != null || neutraleZeigen) {
                        gezeigt++
                        UrteilsBlock(
                            nummer = null,
                            text = em.mittel.name,
                            zelle = zelle,
                            zusatz = if (em.eintrag.aktiv) "" else "abgeschaltet",
                            leuchten = if (zelle != null) leuchten.value else 0f,
                            abgeschaltet = !em.eintrag.aktiv,
                        )
                    }
                }
                if (eintraege.isEmpty()) BlattHinweis("In diesem Stack liegt noch kein Mittel.")
            }
        }

        if (gezeigt == 0) {
            BlattHinweis(
                "Kein Paar ist auffällig — alle Urteile sind neutral. " +
                    "Mit „Auch neutrale zeigen“ erscheint die vollständige Liste.",
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Die Kopfzeile des Gegenstands — Mittel **oder** Ziel, je nach Richtung. */
@Composable
private fun GegenstandsZeile(name: String, leuchten: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(Formen.karte)
            .background(Farben.akzent.copy(alpha = 0.06f + 0.14f * leuchten))
            .border(1.dp, Farben.rand, Formen.karte)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            name,
            style = Schrift.name,
            color = Farben.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Ein Block je Gegenseite: Nummer, Text, Urteilsmarke, Begruendung (zwei Zeilen, 12 sp).
 *
 * Der Kantenbalken links traegt dieselbe Farbe wie die Marke — Farbe ist nie das einzige
 * Merkmal, das Wort steht daneben (§9 Barrierefreiheit).
 */
@Composable
private fun UrteilsBlock(
    nummer: Int?,
    text: String,
    zelle: BewertungszelleE?,
    zusatz: String,
    leuchten: Float,
    abgeschaltet: Boolean,
) {
    val farbe = urteilsFarbe(zelle)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min)
            .clip(Formen.karte)
            .background(
                if (leuchten > 0f) farbe.copy(alpha = 0.10f * leuchten) else Farben.flaeche,
            )
            .border(1.dp, Farben.rand, Formen.karte)
            .alpha(if (abgeschaltet) 0.38f else 1f),
    ) {
        Box(Modifier.fillMaxHeight().width(Masse.ampelBalken).background(farbe, Formen.ampelBalken))
        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nummer != null) {
                    NummernKreis(nummer)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    style = Schrift.grundMittel,
                    color = Farben.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                UrteilsMarke(zelle)
            }
            val begruendung = zelle?.grund.orEmpty()
            if (begruendung.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    begruendung,
                    style = Schrift.klein,
                    color = Farben.textSchwach,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (zusatz.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(zusatz, style = Schrift.winzig, color = Farben.textSchwach)
            }
        }
    }
}

/** *stützt* / *neutral* / *stört* — mit Stärkewort, damit die Farbe nicht allein steht. */
@Composable
private fun UrteilsMarke(zelle: BewertungszelleE?) {
    val farbe = urteilsFarbe(zelle)
    val wort = when (zelle?.wirkung) {
        Wirkung.STUETZT -> "stützt"
        Wirkung.STOERT -> "stört"
        null -> "neutral"
    }
    val staerke = when (zelle?.staerke) {
        1 -> " · schwach"
        2 -> " · mittel"
        3 -> " · stark"
        else -> ""
    }
    Box(
        Modifier
            .clip(Formen.chip)
            .background(farbe.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(wort + staerke, style = Schrift.winzigFett, color = farbe)
    }
}

@Composable
private fun urteilsFarbe(zelle: BewertungszelleE?): Color = when {
    zelle == null -> Farben.grau
    zelle.wirkung == Wirkung.STUETZT -> Farben.gruen
    zelle.staerke >= 3 -> Farben.rot
    else -> Farben.gelbText
}

/** Metazeile in 12 sp — die zweite Zeile jedes Kopfes im Entwurf. */
@Composable
private fun Metazeile(text: String) {
    Text(
        text,
        style = Schrift.klein,
        color = Farben.textSchwach,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
