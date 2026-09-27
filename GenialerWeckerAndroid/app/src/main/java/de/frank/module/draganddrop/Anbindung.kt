// ──────────────────────────────────────────────────────────────────────
// Anbindung für Modul M1.2 — Drag-and-Drop
//
// Diese Datei gehört der App, nicht der Bibliothek. Hier kommt alles hin,
// was sich von App zu App unterscheidet: Aussehen, Texte, Schnittstellen.
// Beim Nachziehen des Moduls wird sie NICHT überschrieben.
//
// Die Moduldatei daneben ist tabu — Anpassungen gehören hierher.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.draganddrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.wecker.Alarm
import de.frank.wecker.Step

/**
 * Anbindung von M1.2 an den Genialen Wecker: Reihenfolge der Weckschritte per Drag-and-drop.
 * Gehört der App. Das Modul besitzt Geste und Geometrie, die App die Reihenfolge in [Alarm.steps].
 */
@Composable
fun WeckReihenfolgeListe(alarm: Alarm, onChange: (Alarm) -> Unit) {
    val gold = LocalGold.current
    val reduced = LocalBewegungReduziert.current
    val listState = rememberLazyListState()
    val zustand = rememberReorderState(listState, alarm.id)
    // Drop and accessibility moves always use the latest alarm and callback, never a stale draft captured earlier.
    val latestAlarm by rememberUpdatedState(alarm)
    val latestChange by rememberUpdatedState(onChange)

    // The dragged order lives locally, so a draft update during the gesture does not snap the row away.
    var reihenfolge by remember(alarm.id) { mutableStateOf(alarm.steps.map(::schluessel)) }
    LaunchedEffect(alarm.steps) {
        val keys = alarm.steps.map(::schluessel)
        reihenfolge = if (zustand.draggedId == null) keys else {
            // Selection changed while dragging: drop removed steps, append newly selected ones.
            val vorhanden = keys.toSet()
            val lokal = reihenfolge.toSet()
            reihenfolge.filter { it in vorhanden } + keys.filter { it !in lokal }
        }
    }

    fun uebernehmen(neu: List<Long>) {
        val current = latestAlarm
        val steps = neu.map(::schritt).filter { it in current.steps }
        val merged = steps + current.steps.filter { it !in steps }
        // Only the step order changes; all other draft fields come from the latest alarm.
        if (merged != current.steps) latestChange(current.copy(steps = merged))
    }

    ReorderAutoScroll(zustand)
    // Bounded height inside the editor's scroll column; with very large fonts the list may scroll internally.
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).reorderViewport(
            state = zustand,
            order = { reihenfolge },
            onMove = { von, nach -> reihenfolge = reihenfolge.toMutableList().apply { add(nach, removeAt(von)) } },
            onDrop = { uebernehmen(reihenfolge) },
            reducedMotion = reduced,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(reihenfolge, key = { _, key -> key }) { index, key ->
            val step = schritt(key)
            fun verschiebe(um: Int) {
                val liste = reihenfolge.toMutableList()
                val ziel = index + um
                if (ziel !in liste.indices) return
                liste.add(ziel, liste.removeAt(index))
                reihenfolge = liste
                uebernehmen(liste)
            }
            Box(reorderItem(zustand, key, reduced)) {
                // Opaque row, so a lifted row never shows its neighbours through.
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).background(gold.flaecheErhoeht)
                        // Merged: title, grip and move actions are reachable as one element.
                        .semantics(mergeDescendants = true) {
                            customActions = listOfNotNull(
                                if (index > 0) CustomAccessibilityAction("${step.title} nach oben verschieben") { verschiebe(-1); true } else null,
                                if (index < reihenfolge.lastIndex) CustomAccessibilityAction("${step.title} nach unten verschieben") { verschiebe(1); true } else null,
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Grip on the left like the reference; long press here and drag.
                    Box(reorderHandle(zustand, key).size(width = 40.dp, height = 48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DragIndicator, "Zum Sortieren lang drücken und ziehen", tint = gold.textGedaempft, modifier = Modifier.size(22.dp))
                    }
                    Text("${index + 1}. ${step.title}", Modifier.weight(1f).padding(end = 12.dp, top = 6.dp, bottom = 6.dp), color = gold.textPrimaer)
                }
            }
        }
    }
}

private fun schluessel(step: Step): Long = step.ordinal.toLong()
private fun schritt(key: Long): Step = Step.entries[key.toInt()]
