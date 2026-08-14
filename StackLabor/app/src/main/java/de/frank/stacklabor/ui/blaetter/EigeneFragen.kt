package de.frank.stacklabor.ui.blaetter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.daten.entitaeten.EigeneFrageE
import de.frank.stacklabor.ui.bausteine.Chip
import de.frank.stacklabor.ui.bausteine.PlusKnopf
import de.frank.stacklabor.ui.theme.Bewegungen
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * **B-08 — Eigene Fragen (F-11).**
 *
 * Jede Frage geht bei **jeder** kuenftigen Auswertung dieses Stacks mit; ihre Antwort
 * erscheint dort als eigener Abschnitt. Es gibt bewusst keine Obergrenze fuer die Anzahl —
 * begrenzt ist nur der einzelne Text (300 Zeichen), weil er woertlich in die Anfrage geht.
 */
@Composable
fun EigeneFragenBlatt(
    app: StackLaborApp,
    stackId: String,
    aufSchliessen: () -> Unit,
) {
    val bereich = rememberCoroutineScope()
    val fragen by app.datenbank.frageDao().fragenDesStacks(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var eingabeOffen by remember { mutableStateOf(false) }
    var neuerText by remember { mutableStateOf("") }
    var zuletztGeloescht by remember { mutableStateOf("") }

    // Der Sockel traegt das Eingabefeld, der schwebende Plus-Knopf oeffnet es (B-08).
    val eingabeSockel: @Composable (() -> Unit) -> Unit = { _ ->
        Column {
            BlattFeld(
                wert = neuerText,
                aufAenderung = { neuerText = it },
                platzhalter = "Was soll die Auswertung zusätzlich beantworten?",
                modifier = Modifier.fillMaxWidth(),
                zeilen = 3,
                maxZeichen = 300,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${neuerText.length}/300",
                    style = Schrift.winzig,
                    color = Farben.textSchwach,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.heightIn(min = Masse.tippflaeche),
                    contentAlignment = Alignment.Center,
                ) {
                    Chip("Abbrechen", false) {
                        neuerText = ""
                        eingabeOffen = false
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(128.dp)) {
                    BlattKnopf("Sichern", gesperrt = neuerText.isBlank()) {
                        if (neuerText.isBlank()) return@BlattKnopf
                        val text = neuerText
                        neuerText = ""
                        eingabeOffen = false
                        zuletztGeloescht = ""
                        bereich.launch { app.repository.legeFrageAn(stackId, text) }
                    }
                }
            }
        }
    }

    val plusSchwebend: @Composable BoxScope.(() -> Unit) -> Unit = { _ ->
        PlusKnopf(
            beschreibung = "Neue Frage anlegen",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Masse.plusRand, bottom = Masse.plusRand),
        ) { eingabeOffen = true }
    }

    BlattRahmen(
        titel = "Eigene Fragen",
        untertitel = if (fragen.isEmpty()) "keine" else "${fragen.size} Fragen",
        aufSchliessen = aufSchliessen,
        sockel = if (eingabeOffen) eingabeSockel else null,
        schwebend = if (eingabeOffen) null else plusSchwebend,
    ) { _ ->
        BlattHinweis("Jede Frage geht in jede künftige Auswertung dieses Stacks ein.")

        if (fragen.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            BlattHinweis("Ohne eigene Fragen antwortet die Auswertung allgemein.")
        }

        if (zuletztGeloescht.isNotBlank()) {
            BlattHinweis("Entfernt: „$zuletztGeloescht“", farbe = Farben.gelbText)
        }

        fragen.forEach { frage ->
            key(frage.id) {
                FrageZeile(frage) {
                    zuletztGeloescht = frage.text.take(40)
                    bereich.launch { app.repository.loescheFrage(frage.id) }
                }
            }
        }

        // Platz, damit der schwebende Plus-Knopf die letzte Zeile nicht verdeckt.
        Spacer(Modifier.height(if (fragen.isEmpty()) 24.dp else Masse.plusKnopf + 24.dp))
    }
}

/**
 * Eine Frage-Zeile: bis zwei Zeilen Text, **Wischen nach links loescht**.
 *
 * Die Geste ist von Hand gebaut statt aus dem Baukasten genommen, weil sie dieselbe
 * Kurve und dieselbe Dauer tragen soll wie der Rest des Entwurfs (`Kurven.leit`, 220 ms) —
 * und weil das Loeschen erst ab einem Drittel der Breite ausloest, nie beim Streifen.
 */
@Composable
private fun FrageZeile(frage: EigeneFrageE, aufLoeschen: () -> Unit) {
    val bereich = rememberCoroutineScope()
    val reduziert = LocalBewegungReduziert.current
    val versatz = remember { Animatable(0f) }
    var breitePx by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .onSizeChanged { breitePx = it.width },
    ) {
        // Der Untergrund zeigt an, was das Wischen bewirkt.
        Row(
            Modifier
                .matchParentSize()
                .clip(Formen.karte)
                .background(Farben.rot.copy(alpha = 0.14f))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Text("Löschen", style = Schrift.klein, color = Farben.rot)
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.DeleteSweep,
                contentDescription = null,
                tint = Farben.rot,
                modifier = Modifier.size(20.dp),
            )
        }

        Box(
            Modifier
                .offset { IntOffset(versatz.value.roundToInt(), 0) }
                .fillMaxWidth()
                .heightIn(min = Masse.tippflaeche)
                .clip(Formen.karte)
                .background(Farben.flaeche)
                .border(1.dp, Farben.rand, Formen.karte)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { verschiebung ->
                        bereich.launch {
                            val ziel = (versatz.value + verschiebung)
                                .coerceIn(-breitePx.toFloat(), 0f)
                            versatz.snapTo(ziel)
                        }
                    },
                    onDragStopped = {
                        val schwelle = breitePx * 0.33f
                        if (breitePx > 0 && versatz.value < -schwelle) {
                            versatz.animateTo(
                                -breitePx.toFloat(),
                                tween(if (reduziert) 0 else Dauer.ANTWORT, easing = Kurven.leit),
                            )
                            aufLoeschen()
                        } else {
                            versatz.animateTo(0f, Bewegungen.einrasten())
                        }
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                frage.text,
                style = Schrift.grund,
                color = Farben.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
