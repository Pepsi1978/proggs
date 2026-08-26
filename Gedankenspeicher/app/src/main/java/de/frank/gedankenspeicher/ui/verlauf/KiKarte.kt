package de.frank.gedankenspeicher.ui.verlauf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.VolumeUp
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.KiAntwort
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.ui.theme.Dauern
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Kurven
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.dauer
import de.frank.gedankenspeicher.ui.theme.schwebendeKarte
import de.frank.gedankenspeicher.ui.theme.wanderndesLeuchten

/**
 * **Die KI-Antwort im Verlauf** (`02-UI-SPEC.md` B-01).
 *
 * Sie hebt sich ab: breiterer Rand in der Akzentfarbe, eigener Grundton, eine Aura außen.
 * Das ist Absicht — sie ist das Ergebnis, auf das alle Notizen davor hinauslaufen, und muss
 * beim Scrollen sofort als solche zu erkennen sein.
 *
 * Rückfrage und Antwort stehen seit Fassung 0.5.12 zusammengeklappt in einer eigenen Zeile.
 * Sie sind der Anlass der Auswertung, nicht ihr Inhalt — offen geschoben sie das Ergebnis
 * bei jedem Blick auf die Karte ein Stück weiter nach unten.
 *
 * Der ganze Inhalt liegt in einem `SelectionContainer`: jeder Satz lässt sich mit langem
 * Druck markieren und herauskopieren — die Rückfrage und die eigene Antwort im aufgeklappten
 * Bereich genauso wie die Auswertung selbst. Weil das lange Drücken damit der Auswahl
 * gehört, trägt die Fußzeile einen sichtbaren Menüknopf: über ihn geht es zum Kopieren im
 * Ganzen, zum Bearbeiten und zum Löschen. Vorher lag das Menü allein auf dem langen Druck —
 * und dieser Weg wäre mit der Textauswahl verschwunden.
 */
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun KiKarte(
    antwort: KiAntwort,
    liestVor: Boolean,
    vorleseAbsatz: Int,
    beiVorlesen: () -> Unit,
    beiMenue: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften

    // Derselbe Riegel wie an der Notizkarte: die Vorlese-Hervorhebung bekommt erst dann
    // einen Animationslauf je Baustein, wenn an dieser Auswertung wirklich vorgelesen wird.
    // Eine lange Auswertung hat leicht fünfzig Bausteine — das sind fünfzig Läufe, die beim
    // Scrollen sonst mit jeder Karte entstehen und wieder vergehen.
    var wurdeVorgelesen by remember(antwort.id) { mutableStateOf(false) }
    if (liestVor && !wurdeVorgelesen) wurdeVorgelesen = true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .schwebendeKarte(
                farben = farben,
                radius = Masse.karteRadius,
                randfarbe = farben.kiKarteRand,
                randstaerke = 1.5.dp,
                grundfarbe = farben.kiKarte,
            )
            .combinedClickable(onLongClick = beiMenue, onClick = {})
            .padding(Masse.karteInnen),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp), tint = farben.akzent)
                Spacer(Modifier.width(8.dp))
                Text("Auswertung", style = schrift.kartenUeberschrift, color = farben.akzent)
                Spacer(Modifier.weight(1f))
                Text(
                    Repository.zeitpunkt(antwort.erstelltAm),
                    style = schrift.zeitstempel,
                    color = farben.textSchwach,
                )
            }

            Spacer(Modifier.height(12.dp))
            SelectionContainer {
                Column {
                    Aufklappbar(rueckfrage = antwort.rueckfrage, antwort = antwort.antwortDesNutzers)

                    Spacer(Modifier.height(14.dp))
                    ReichtextAnsicht(
                        text = antwort.text,
                        hervorgehobenerAbsatz = if (liestVor) vorleseAbsatz else -1,
                        stil = schrift.kiAntworttext,
                        animiert = wurdeVorgelesen,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bei großer Systemschrift bricht diese Zeile um, statt abgeschnitten zu
                // werden (`02-UI-SPEC.md` §9).
                Text(
                    text = buildString {
                        append(antwort.profilName.ifBlank { "ohne Profil" })
                        append(" · ").append(antwort.modell)
                        append(" · ").append(antwort.effort)
                        append(" · Websuche ").append(if (antwort.websucheAn) "an" else "aus")
                        if (antwort.ganzeSitzung) append(" · ganze Sitzung")
                    },
                    style = schrift.zeitstempel,
                    color = farben.textSchwach,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                Spacer(Modifier.width(8.dp))
                Kartenknopf(
                    symbol = if (liestVor) Icons.Filled.StopCircle else Icons.Outlined.VolumeUp,
                    beschreibung = if (liestVor) "Vorlesen anhalten" else "Auswertung vorlesen",
                    farbe = if (liestVor) farben.akzent else farben.textMittel,
                    beiDruck = beiVorlesen,
                )
                Kartenknopf(
                    symbol = Icons.Outlined.MoreVert,
                    beschreibung = "Menü der Auswertung",
                    farbe = farben.textMittel,
                    beiDruck = beiMenue,
                )
            }
        }
    }
}

/**
 * **Die Rückfrage und die Antwort darauf — eingeklappt.**
 *
 * Zugeklappt ist nur zu sehen, dass es sie gibt; ein Druck auf die Zeile zeigt beides. Der
 * Pfeil dreht sich dabei mit, damit erkennbar bleibt, dass die Zeile weiterhin ein Schalter
 * ist und nicht zur Überschrift geworden.
 */
@Composable
private fun Aufklappbar(rueckfrage: String, antwort: String) {
    val farben = Farben
    val schrift = Schriften
    if (rueckfrage.isBlank() && antwort.isBlank()) return

    var offen by remember { mutableStateOf(false) }
    val drehung by animateFloatAsState(
        targetValue = if (offen) 180f else 0f,
        animationSpec = tween(dauer(Dauern.STANDARD), easing = Kurven.standard),
        label = "pfeil",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(farben.akzentGedeckt.copy(alpha = farben.akzentGedeckt.alpha * 0.6f))
            .border(1.dp, farben.rand, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Der Schalter sitzt allein auf der Kopfzeile, nicht auf dem ganzen Kasten: sonst
        // klappte jeder Griff nach einem Wort im aufgeklappten Text ihn wieder zu.
        Row(
            modifier = Modifier.fillMaxWidth().clickable { offen = !offen },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.QuestionAnswer,
                null,
                Modifier.size(16.dp),
                tint = farben.textMittel,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Rückfrage und deine Antwort",
                style = schrift.zeitstempel,
                color = farben.textMittel,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.ExpandMore,
                if (offen) "Zuklappen" else "Aufklappen",
                Modifier.size(20.dp).rotate(drehung),
                tint = farben.textMittel,
            )
        }

        AnimatedVisibility(
            visible = offen,
            enter = fadeIn(tween(dauer(Dauern.STANDARD))) + expandVertically(tween(dauer(Dauern.STANDARD))),
            exit = fadeOut(tween(dauer(Dauern.KURZ))) + shrinkVertically(tween(dauer(Dauern.KURZ))),
        ) {
            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (rueckfrage.isNotBlank()) {
                    Beschriftetes("Die KI fragte", rueckfrage, kursiv = true)
                }
                if (antwort.isNotBlank()) {
                    Beschriftetes("Deine Antwort", antwort, kursiv = false)
                }
            }
        }
    }
}

@Composable
private fun Beschriftetes(titel: String, text: String, kursiv: Boolean) {
    val farben = Farben
    val schrift = Schriften
    Column {
        Text(titel, style = schrift.zeitstempel, color = farben.textSchwach)
        Spacer(Modifier.height(2.dp))
        Text(
            text,
            style = schrift.notiztext.copy(fontStyle = if (kursiv) FontStyle.Italic else FontStyle.Normal),
            color = if (kursiv) farben.textMittel else farben.textStark,
        )
    }
}

/**
 * Die noch leere Karte, während Codex arbeitet (`02-UI-SPEC.md` B-01, Zustand
 * "Auswertung läuft"). Sie trägt das wandernde Leuchten (M-07).
 */
@Composable
fun KiKarteEntsteht() {
    val farben = Farben
    val schrift = Schriften
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .schwebendeKarte(
                farben = farben,
                radius = Masse.karteRadius,
                randfarbe = farben.kiKarteRand,
                randstaerke = 1.5.dp,
                grundfarbe = farben.kiKarte,
            )
            .wanderndesLeuchten(farben.akzentGedeckt, aktiv = true)
            .padding(Masse.karteInnen),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp), tint = farben.akzent)
                Spacer(Modifier.width(8.dp))
                Text("Auswertung", style = schrift.kartenUeberschrift, color = farben.akzent)
            }
            WanderndePunkte(farben.akzent)
            Text("Die KI denkt nach …", style = schrift.zeitstempel, color = farben.textSchwach)
            Spacer(Modifier.height(24.dp))
        }
    }
}
