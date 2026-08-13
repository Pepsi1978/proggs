package de.frank.experimente.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.KnopfBetont
import de.frank.experimente.ui.components.KnopfUmrandet
import de.frank.experimente.ui.components.Tagewahl
import de.frank.experimente.ui.components.merkeDruck
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.glas
import de.frank.experimente.ui.theme.lichtsaum

/**
 * **F-35 — die Anlegefläche des Monitors** (`ANLEGEN`).
 *
 * Sie fährt von unten herein (`M-80`, 320 ms, `cubic-bezier(.2, 0, 0, 1)`) und legt sich
 * über den Bildschirm: dahinter `color-mix(Grund 62%)` mit 18 dp Weichzeichnen (`E-03`),
 * die Fläche selbst hat oben Radius 24 dp, Fläche *Fläche*, Lichtsaum und
 * `0 -20px 50px rgba(0,0,0,.4)`.
 *
 * Sprechknopf (56 dp) und Textfeld stehen nebeneinander; darunter „Abbrechen“ (F-33),
 * „Verbessern“ (F-02) und „Speichern“.
 */
@Composable
fun BoxScope.Anlegeflaeche(modell: AppViewModel) {
    val feld by modell.anlegeFeld.collectAsStateWithLifecycle()
    val tage by modell.anlegeTage.collectAsStateWithLifecycle()
    Anlegeblatt(
        titel = "Eigenes Experiment",
        unterzeile = "Einsprechen oder tippen. Es steht danach unter „Steht an“.",
        platzhalter = "Was willst du ausprobieren?",
        sprechbeschriftung = "Eigenes Experiment einsprechen",
        feld = feld,
        modell = modell,
        tage = tage,
        beiTage = modell::setzeAnlegeTage,
        beiSprechen = { modell.sprechknopf(modell.anlegeFeldFluss()) },
        beiAenderung = { modell.setzeText(modell.anlegeFeldFluss(), it) },
        beiVerbessern = {
            if (feld.kannZurueck) modell.nimmZurueck(modell.anlegeFeldFluss())
            else modell.verbessere(modell.anlegeFeldFluss())
        },
        beiSpeichern = modell::legeEigenesImMonitorAn,
        beiSchliessen = modell::schliesseAnlegen,
        beiDaneben = modell::legeAnlegenBeiseite,
    )
}

/**
 * Das gemeinsame Blatt hinter allen Anlegeflächen — B-10 (F-35), B-04 (F-20) und
 * B-05 (F-18) benutzen dasselbe, wie im Entwurf.
 */
@Composable
fun BoxScope.Anlegeblatt(
    titel: String,
    unterzeile: String,
    platzhalter: String,
    sprechbeschriftung: String,
    feld: de.frank.experimente.ui.Feld,
    modell: AppViewModel,
    beiSprechen: () -> Unit,
    beiAenderung: (String) -> Unit,
    beiVerbessern: () -> Unit,
    beiSpeichern: () -> Unit,
    beiSchliessen: () -> Unit,
    beiDaneben: () -> Unit = beiSchliessen,
    /** Die selbst gewählte Dauer. `null` bei Blättern ohne Dauer (Ziele). */
    tage: Int? = null,
    beiTage: ((Int) -> Unit)? = null,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val stufe = LocalEffektstufe.current
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()

    var drin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { drin = true }
    val weg by animateFloatAsState(
        targetValue = if (drin) 0f else 1f,
        animationSpec = tween(dauer(Bewegung.ANLEGEN, stufe), easing = Bewegung.ruhig),
        label = "anlegen",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(farben.grund.copy(alpha = 0.62f))
            // Ein Druck auf die Fläche darüber legt die Anlegefläche nur beiseite — der
            // eingesprochene Text bleibt erhalten und steht beim nächsten Öffnen wieder da.
            //
            // F-33 („der eingesprochene Text wird verworfen") gilt für den ausdrücklichen
            // Knopf „Abbrechen", nicht für einen Druck daneben. Genau daran ging eine
            // fertige Transkription verloren: der Druck galt eigentlich der Störung, die
            // unsichtbar dahinter lag.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = beiDaneben,
            ),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = weg * 320.dp.toPx() }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(farben.flaeche)
                .lichtsaum(farben.text, 0.14f)
                // Der Druck auf die Fläche selbst darf nicht durchschlagen.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 28.dp),
        ) {
            Text(titel, style = schriften.abschnittstitel, color = farben.text)
            Text(
                text = unterzeile,
                style = schriften.fliesstextKlein,
                color = farben.gedaempft,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val quelle = merkeDruck()
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(farben.aktion)
                        .clickable(interactionSource = quelle, indication = null, onClick = beiSprechen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (nimmtAuf) Symbole.Stopp else Symbole.Mikrofon,
                        contentDescription = sprechbeschriftung,
                        tint = farben.aufAktion,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Eingabefeld(
                    text = feld.text,
                    beiAenderung = beiAenderung,
                    platzhalter = platzhalter,
                    mindesthoehe = 120.dp,
                    innen = 14.dp,
                    modifier = Modifier.weight(1f),
                )
            }

            // Die Dauer bestimmt Frank, nicht die Schätzung: sie stand vorher gar nicht zur
            // Wahl und liess sich hinterher nirgends berichtigen.
            if (tage != null && beiTage != null) {
                Tagewahl(
                    tage = tage,
                    beiAenderung = beiTage,
                    beschriftung = "Wie viele Tage soll es laufen?",
                    modifier = Modifier.padding(top = 18.dp),
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KnopfUmrandet("Abbrechen", beiSchliessen, Modifier.weight(1f))
                KnopfUmrandet(
                    text = if (feld.kannZurueck) "Zurücknehmen" else "Verbessern",
                    beiKlick = beiVerbessern,
                    modifier = Modifier.weight(1f),
                    farbe = farben.aktion,
                )
                KnopfBetont(
                    text = "Speichern",
                    beiKlick = beiSpeichern,
                    modifier = Modifier.weight(1.2f),
                )
            }
        }
    }
}
