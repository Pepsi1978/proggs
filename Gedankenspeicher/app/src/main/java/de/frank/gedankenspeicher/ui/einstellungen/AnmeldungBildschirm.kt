package de.frank.gedankenspeicher.ui.einstellungen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.auth.deviceCodeGroups
import de.frank.gedankenspeicher.ui.Anmeldezustand
import de.frank.gedankenspeicher.ui.ki.GefuellterKnopf
import de.frank.gedankenspeicher.ui.ki.GeranderterKnopf
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.schimmer

/**
 * **B-05 — die Codex-Anmeldung per Gerätecode.**
 *
 * Der Code steht groß und in zwei Blöcken — vier Zeichen, Trennstrich, fünf Zeichen —, weil
 * er von diesem Bildschirm abgelesen und in einem anderen Gerät eingetippt wird. Die
 * Formatierung übernimmt [deviceCodeGroups], damit sie dieselbe ist wie in PerfectMoment.
 */
@Composable
fun AnmeldungBildschirm(
    zustand: Anmeldezustand,
    beiOeffnen: () -> Unit,
    beiKopieren: () -> Unit,
    beiNeuerCode: () -> Unit,
    beiZurueck: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften

    Column(Modifier.fillMaxSize().background(farben.hintergrund)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(Masse.kopfleiste).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = beiZurueck) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = farben.textMittel)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = Masse.seitenrand * 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Codex verbinden", style = schrift.bildschirmtitel, color = farben.textStark)
            Spacer(Modifier.height(28.dp))

            when {
                zustand.erfolgreich -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Check, null, Modifier.size(48.dp), tint = farben.erfolg)
                    Spacer(Modifier.height(12.dp))
                    Text("Verbunden", style = schrift.bildschirmtitel, color = farben.erfolg)
                }

                zustand.holtCode -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .schimmer(farben, RoundedCornerShape(Masse.profilRadius)),
                )

                zustand.code.isNotBlank() -> Text(
                    // Vier Zeichen, Trennstrich, fünf — die Gruppierung bestimmt der Server,
                    // nicht die App: eine feste Länge würde einen längeren Code abschneiden.
                    text = deviceCodeGroups(zustand.code).joinToString(" – "),
                    style = schrift.geraetecode,
                    color = farben.akzent,
                    textAlign = TextAlign.Center,
                    textDecoration = if (zustand.abgelaufen) TextDecoration.LineThrough else null,
                )

                zustand.fehler != null -> Text(
                    zustand.fehler,
                    style = schrift.notiztext,
                    color = farben.fehler,
                    textAlign = TextAlign.Center,
                )
            }

            if (zustand.code.isNotBlank() && !zustand.erfolgreich) {
                Spacer(Modifier.height(16.dp))
                Text(
                    zustand.adresse,
                    style = schrift.einstellungErklaerung,
                    color = farben.textSchwach,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))

                if (zustand.abgelaufen) {
                    GefuellterKnopf("Neuen Code holen", beiNeuerCode)
                } else {
                    GefuellterKnopf("Im Browser öffnen", beiOeffnen)
                    Spacer(Modifier.height(10.dp))
                    GeranderterKnopf("Code kopieren", beiKopieren, modifier = Modifier.fillMaxWidth())
                }
            }

            if (zustand.wartet && !zustand.erfolgreich && !zustand.abgelaufen) {
                Spacer(Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = farben.akzent,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Warte auf Bestätigung …", style = schrift.zeitstempel, color = farben.textSchwach)
                }
            }
        }
    }
}
