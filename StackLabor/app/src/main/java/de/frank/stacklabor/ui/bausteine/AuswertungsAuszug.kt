package de.frank.stacklabor.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Schrift
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Die Auswertungs-Karte auf B-02: **drei Zeilen Auszug**, nicht mehr.
 *
 * Auf 297 dp Breite und unter zwei anderen Bereichen trägt kein Langtext — der volle Text
 * steht im Vollbild B-07. Läuft gerade eine Auswertung, wächst der Text hier wortweise mit
 * (M-13); ist die Bewertung veraltet, trägt die Karte einen bernsteinfarbenen Rahmen (F-23).
 */
@Composable
fun AuswertungsAuszug(
    text: String,
    laeuft: Boolean,
    veraltet: Boolean,
    zeitstempel: String,
    modell: String,
    modifier: Modifier = Modifier,
    aufKlick: () -> Unit,
) {
    val rahmen = if (veraltet) Farben.gelbText else Farben.rand
    Column(
        modifier
            .fillMaxWidth()
            .clip(Formen.karte)
            .background(Farben.flaeche)
            .border(if (veraltet) 1.5.dp else 1.dp, rahmen, Formen.karte)
            .clickable(onClick = aufKlick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    laeuft -> "Auswertung läuft"
                    veraltet -> "Stand veraltet"
                    else -> "Auswertung"
                },
                style = Schrift.rubrik,
                color = if (veraltet) Farben.gelbText else Farben.textSchwach,
            )
            Spacer(Modifier.width(8.dp))
            if (zeitstempel.isNotBlank()) {
                Text(
                    listOf(zeitstempel, modell).filter { it.isNotBlank() }.joinToString(" · "),
                    style = Schrift.winzig,
                    color = Farben.textSchwach,
                    maxLines = 1,
                )
            }
        }
        if (laeuft && text.isBlank()) {
            // Solange noch kein Wort da ist, zeigt ein Schimmer, wo Text erscheinen wird (M-11).
            SchimmerZeilen(anzahl = 3)
        } else {
            Text(
                text.ifBlank { "Noch nicht ausgewertet." },
                style = Schrift.klein,
                color = Farben.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Drei Platzhalterzeilen mit Schimmer — der Wartezustand, bevor das erste Wort eintrifft. */
@Composable
private fun SchimmerZeilen(anzahl: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(anzahl) { i ->
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxWidth(if (i == anzahl - 1) 0.6f else 1f)
                    .height(10.dp)
                    .clip(Formen.chip)
                    .background(Farben.erhoeht)
                    .glanzkante(),
            )
        }
    }
}

/** Zeitstempel im Format des Entwurfs: `14.08. 07:44`. */
fun zeitpunktKurz(millis: Long): String =
    SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(Date(millis))
