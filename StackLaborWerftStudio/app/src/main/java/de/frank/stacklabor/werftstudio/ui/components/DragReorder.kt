package de.frank.stacklabor.werftstudio.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive

/**
 * Umsortieren per langem Druck und Ziehen.
 *
 * Der Ablauf: langer Druck greift eine Zeile, sie hebt sich sichtbar ab, die übrigen Zeilen
 * weichen währenddessen aus, und beim Loslassen liegt sie genau dort, wo sie angezeigt wurde.
 * Am oberen und unteren Rand rollt die Liste von selbst weiter, damit auch weite Wege von
 * unten nach oben in einem Zug gehen.
 *
 * Die Zeilen sind alle gleich hoch — deshalb genügt eine Zeilenhöhe als Maß, statt jede Zeile
 * einzeln zu vermessen.
 */
@Stable
class ReorderState<T : Any> internal constructor(
    internal val listState: LazyListState,
    private val keyOf: (T) -> String,
    private val rowHeightPx: Float,
) {
    /**
     * Die Reihenfolge von aussen. Wird bei jedem Durchlauf frisch gesetzt, damit die Anzeige
     * nie eine veraltete zweite Kopie zeigt (etwa nach dem Umschalten der Sortierung).
     */
    internal var quelle: List<T> by mutableStateOf(emptyList())

    /**
     * Die vorläufige Reihenfolge in der Hand. Nur während (und kurz nach) dem Ziehen gesetzt —
     * sonst gilt immer [quelle].
     */
    private var handOrdnung: List<T>? by mutableStateOf(null)

    /** Was angezeigt wird: die Hand gewinnt, solange sie etwas hält. */
    val items: List<T> get() = handOrdnung ?: quelle

    /** Nach dem Loslassen: sobald die neue Reihenfolge von aussen ankommt, gilt wieder [quelle]. */
    internal fun loesseHandOrdnung() {
        handOrdnung = null
    }

    /** Welche Zeile gerade in der Hand liegt (null = keine). */
    var draggedKey: String? by mutableStateOf(null)
        internal set

    /** Verschiebung der gezogenen Zeile gegenüber ihrer aktuellen Position. */
    internal var offsetY by mutableFloatStateOf(0f)

    /** Wo der Finger im Sichtfenster steht — für das Weiterrollen am Rand. */
    internal var fingerY by mutableFloatStateOf(0f)

    internal var onDropped: ((List<String>) -> Unit)? = null
    internal var onGrabbed: (() -> Unit)? = null

    fun isDragged(item: T): Boolean = draggedKey == keyOf(item)

    /** Hebt die gezogene Zeile optisch heraus. */
    fun liftModifier(item: T): Modifier = if (!isDragged(item)) {
        Modifier
    } else {
        Modifier.graphicsLayer {
            translationY = offsetY
            scaleX = 1.03f
            scaleY = 1.03f
            shadowElevation = 18.dp.toPx()
        }
    }

    /** Der lange Druck, der die Zeile greift. */
    fun dragModifier(item: T): Modifier = Modifier.pointerInput(keyOf(item), items.size) {
        detectDragGesturesAfterLongPress(
            onDragStart = { position ->
                handOrdnung = items
                draggedKey = keyOf(item)
                offsetY = 0f
                fingerY = position.y + itemTopInViewport(keyOf(item))
                onGrabbed?.invoke()
            },
            onDrag = { change, amount ->
                change.consume()
                offsetY += amount.y
                fingerY += amount.y
                verschiebeWennNoetig()
            },
            onDragCancel = { beenden(uebernehmen = false) },
            onDragEnd = { beenden(uebernehmen = true) },
        )
    }

    /**
     * Sobald die gezogene Zeile eine halbe Zeilenhöhe gewandert ist, tauscht sie mit der
     * Nachbarzeile den Platz. Die Verschiebung wird dabei um genau diese Zeilenhöhe
     * zurückgesetzt, damit die Zeile unter dem Finger stehen bleibt.
     */
    internal fun verschiebeWennNoetig() {
        val key = draggedKey ?: return
        if (rowHeightPx <= 0f) return
        val aktuell = handOrdnung ?: return
        var index = aktuell.indexOfFirst { keyOf(it) == key }
        if (index < 0) return
        var neu = aktuell
        while (offsetY > rowHeightPx / 2f && index < neu.lastIndex) {
            neu = neu.toMutableList().apply { add(index + 1, removeAt(index)) }
            offsetY -= rowHeightPx
            index++
        }
        while (offsetY < -rowHeightPx / 2f && index > 0) {
            neu = neu.toMutableList().apply { add(index - 1, removeAt(index)) }
            offsetY += rowHeightPx
            index--
        }
        handOrdnung = neu
    }

    private fun beenden(uebernehmen: Boolean) {
        val gezogen = draggedKey != null
        val ergebnis = handOrdnung
        draggedKey = null
        offsetY = 0f
        // Die Hand-Reihenfolge bleibt stehen, bis die neue von aussen ankommt — sonst
        // blitzt fuer einen Moment die alte Reihenfolge auf.
        if (!uebernehmen) handOrdnung = null
        if (uebernehmen && gezogen && ergebnis != null) onDropped?.invoke(ergebnis.map(keyOf))
    }

    private fun itemTopInViewport(key: String): Float =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.offset?.toFloat() ?: 0f

    /** Wie schnell am Rand weitergerollt wird — 0 heißt: stehen bleiben. */
    internal fun autoScrollSchritt(): Float {
        val info = listState.layoutInfo
        val rand = rowHeightPx.coerceAtLeast(1f)
        val oben = info.viewportStartOffset + rand
        val unten = info.viewportEndOffset - rand
        return when {
            fingerY < oben -> -((oben - fingerY) / rand).coerceAtMost(1f) * MAX_SCROLL_PRO_BILD
            fingerY > unten -> ((fingerY - unten) / rand).coerceAtMost(1f) * MAX_SCROLL_PRO_BILD
            else -> 0f
        }
    }

    private companion object {
        const val MAX_SCROLL_PRO_BILD = 22f
    }
}

/**
 * Hält die Reihenfolge während des Ziehens fest und meldet sie beim Loslassen einmal nach oben.
 *
 * Solange gezogen wird, gewinnt die Reihenfolge in der Hand; kommt in dieser Zeit eine neue
 * Liste von außen (etwa weil eine Auswertung fertig wurde), wird sie erst danach übernommen —
 * sonst würde die Zeile mitten in der Bewegung wegspringen.
 */
@Composable
fun <T : Any> rememberReorder(
    source: List<T>,
    keyOf: (T) -> String,
    listState: LazyListState,
    rowHeight: Dp,
    onGrabbed: () -> Unit = {},
    onDropped: (List<String>) -> Unit,
): ReorderState<T> {
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    val state = remember(listState, rowHeightPx) { ReorderState(listState, keyOf, rowHeightPx) }
    state.onDropped = onDropped
    state.onGrabbed = onGrabbed
    // Die Quelle wird bei JEDEM Durchlauf gesetzt, nicht in einem Effekt. Damit kann die
    // Anzeige gar nicht erst hinter der Wahrheit zurueckbleiben — etwa wenn die Sortierung
    // umgeschaltet wird, waehrend der Bildschirm offen ist.
    state.quelle = source
    // Nach dem Loslassen haelt die Hand-Reihenfolge noch, bis die neue von aussen ankommt.
    LaunchedEffect(source) {
        if (state.draggedKey == null) state.loesseHandOrdnung()
    }
    LaunchedEffect(state.draggedKey) {
        if (state.draggedKey == null) return@LaunchedEffect
        while (isActive && state.draggedKey != null) {
            val schritt = state.autoScrollSchritt()
            if (schritt != 0f) {
                val gerollt = listState.scrollBy(schritt)
                // Die Liste wandert unter der Zeile weg — die Verschiebung zieht mit,
                // damit die Zeile am Finger bleibt und weiter Plaetze tauscht.
                state.offsetY += gerollt
                state.verschiebeWennNoetig()
            }
            withFrameNanos { }
        }
    }
    return state
}
