package de.frank.gedankenspeicher.ui.verlauf

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.KiAntwort
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.schwebendeKarte
import de.frank.gedankenspeicher.ui.theme.wanderndesLeuchten

/**
 * **Die KI-Antwort im Verlauf** (`02-UI-SPEC.md` B-01).
 *
 * Sie hebt sich ab: breiterer Rand in der Akzentfarbe, eigener Grundton, eine Aura außen.
 * Das ist Absicht — sie ist das Ergebnis, auf das alle Notizen davor hinauslaufen, und muss
 * beim Scrollen sofort als solche zu erkennen sein.
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
            Text(
                antwort.rueckfrage,
                style = schrift.notiztext.copy(fontStyle = FontStyle.Italic),
                color = farben.textMittel,
            )
            Spacer(Modifier.height(4.dp))
            Text(antwort.antwortDesNutzers, style = schrift.zeitstempel, color = farben.textSchwach)

            Spacer(Modifier.height(14.dp))
            AbsatzText(
                text = antwort.text,
                hervorgehobenerAbsatz = if (liestVor) vorleseAbsatz else -1,
                stil = schrift.kiAntworttext,
            )

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
            }
        }
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
