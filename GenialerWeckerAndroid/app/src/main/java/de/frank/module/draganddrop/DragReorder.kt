// ──────────────────────────────────────────────────────────────────────
// Modul M1.2 — Drag-and-Drop · Stand v1
// Quelle: Module/Android/M1.2-Drag-and-Drop/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.2 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.draganddrop

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Finger und Karte bleiben im Koordinatensystem der Liste. Die tatsächlichen Lazy-Item-Plätze
 * sind die einzige Geometriequelle: keine aufaddierten Höhen, Abstände oder Scrollkorrekturen.
 * Nach einem Tausch wird erst mit dem neu gemessenen Layout weitergetauscht.
 */
@Stable
class ReorderState(private val listState: LazyListState, private val scope: CoroutineScope) {
    var draggedId by mutableStateOf<Long?>(null)
        private set
    var dragging by mutableStateOf(false)
        private set

    private var fingerY = 0f
    private var grabOffset = 0f
    private var visualTop by mutableFloatStateOf(0f)
    private var landingProgress by mutableFloatStateOf(0f)
    private var landing: Job? = null
    private var generation = 0
    private var viewportOrigin = Offset.Zero
    private val handles = mutableMapOf<Long, Rect>()
    private var order: () -> List<Long> = { emptyList() }
    private var move: (Int, Int) -> Unit = { _, _ -> }
    private var drop: () -> Unit = {}

    fun isDragging(id: Long): Boolean = draggedId == id

    internal fun setViewport(origin: Offset) { viewportOrigin = origin }
    internal fun measureHandle(id: Long, bounds: Rect) { handles[id] = bounds }
    internal fun removeHandle(id: Long) { handles.remove(id) }

    internal fun handleAt(position: Offset): Long? {
        val rootPosition = position + viewportOrigin
        return listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            handles[item.key]?.contains(rootPosition) == true
        }?.key as? Long
    }

    internal fun start(
        id: Long,
        position: Offset,
        order: () -> List<Long>,
        move: (Int, Int) -> Unit,
        drop: () -> Unit,
    ): Boolean {
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.key == id } ?: return false
        if (order().getOrNull(item.index) != id) return false
        val startTop = item.offset + translation(id)
        generation++
        landing?.cancel()
        this.order = order
        this.move = move
        this.drop = drop
        draggedId = id
        dragging = true
        landingProgress = 0f
        fingerY = position.y + layout.viewportStartOffset
        grabOffset = fingerY - startTop
        visualTop = startTop
        return true
    }

    internal fun drag(amountY: Float) {
        if (!dragging) return
        fingerY += amountY
        updateVisualTop()
        settle()
    }

    private fun updateVisualTop() {
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.key == draggedId } ?: return
        // Auch wenn der Finger über die Kopfleiste hinausgeht, bleibt die Karte sichtbar.
        var minimum = (layout.viewportStartOffset + layout.beforeContentPadding).toFloat()
        var maximum = (layout.viewportEndOffset - item.size).toFloat().coerceAtLeast(minimum)
        if (item.index == 0 && !listState.canScrollBackward) minimum = maxOf(minimum, item.offset.toFloat())
        if (item.index == layout.totalItemsCount - 1 && !listState.canScrollForward) {
            maximum = minOf(maximum, item.offset.toFloat()).coerceAtLeast(minimum)
        }
        visualTop = (fingerY - grabOffset).coerceIn(minimum, maximum)
    }

    /** Nur logische Zielplätze vergleichen, niemals die gerade animierten Nachbarkarten. */
    private fun settle() {
        val id = draggedId ?: return
        val current = order()
        val index = current.indexOf(id)
        val visible = listState.layoutInfo.visibleItemsInfo
        val item = visible.firstOrNull { it.key == id } ?: return
        // Daten geändert, aber Layout noch alt: kein zweiter Tausch mit veralteten Maßen.
        if (index < 0 || item.index != index) return
        val above = visible.firstOrNull { it.index == index - 1 && it.key == current.getOrNull(index - 1) }
        val below = visible.firstOrNull { it.index == index + 1 && it.key == current.getOrNull(index + 1) }
        val target = when {
            above != null && visualTop < above.offset + above.size / 2f -> index - 1
            below != null && visualTop + item.size > below.offset + below.size / 2f -> index + 1
            else -> return
        }
        // Sonst folgt LazyColumn dem Key der obersten Karte und verschiebt das ganze Sichtfenster.
        listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        move(index, target)
    }

    internal fun translation(id: Long): Float {
        if (draggedId != id) return 0f
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return 0f
        return (visualTop - item.offset) * (1f - landingProgress)
    }

    internal fun stop(reducedMotion: Boolean) {
        if (!dragging) return
        dragging = false
        drop()
        val releasedGeneration = generation
        landing = scope.launch {
            try {
                // Den letzten Tausch zuerst layouten, dann weich auf genau diesem Platz ablegen.
                withFrameNanos { }
                animate(0f, 1f, animationSpec = tween(if (reducedMotion) 0 else 180)) { value, _ ->
                    landingProgress = value
                }
            } finally {
                // Erneutes Greifen darf nicht vom Ende der vorherigen Ablegeanimation gelöscht werden.
                if (generation == releasedGeneration) {
                    draggedId = null
                    landingProgress = 0f
                }
            }
        }
    }

    internal fun dispose() {
        generation++
        landing?.cancel()
        draggedId = null
        dragging = false
        handles.clear()
    }

    /** Randscrollen richtet sich nach dem Finger, mit Geschwindigkeit pro Sekunde statt pro Bild. */
    internal suspend fun followEdges(margin: Float, maxSpeed: Float) {
        var previousFrame = withFrameNanos { it }
        while (dragging) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.032f)
            previousFrame = frame
            val layout = listState.layoutInfo
            val top = layout.viewportStartOffset.toFloat()
            val bottom = layout.viewportEndOffset.toFloat()
            val edge = margin.coerceAtMost((bottom - top) / 3f)
            if (edge <= 0f) continue
            val strength = when {
                fingerY < top + edge -> -((top + edge - fingerY) / edge).coerceIn(0f, 1f)
                fingerY > bottom - edge -> ((fingerY - bottom + edge) / edge).coerceIn(0f, 1f)
                else -> 0f
            }
            // Kein scroll{} über die ganze Geste: requestScrollToItem darf den Loop nicht abbrechen.
            if (strength != 0f) listState.dispatchRawDelta(strength * kotlin.math.abs(strength) * maxSpeed * seconds)
            updateVisualTop()
            settle()
        }
    }
}

@Composable
fun rememberReorderState(listState: LazyListState, listKey: Any): ReorderState {
    val scope = rememberCoroutineScope()
    val state = remember(listState, listKey) { ReorderState(listState, scope) }
    DisposableEffect(state) { onDispose { state.dispose() } }
    return state
}

/** Die Geste gehört zum festen Viewport, nicht zum Griff, der beim Tauschen seinen Platz wechselt. */
@Composable
fun Modifier.reorderViewport(
    state: ReorderState,
    order: () -> List<Long>,
    onMove: (Int, Int) -> Unit,
    onDrop: () -> Unit,
    reducedMotion: Boolean,
): Modifier {
    val currentOrder by rememberUpdatedState(order)
    val currentMove by rememberUpdatedState(onMove)
    val currentDrop by rememberUpdatedState(onDrop)
    val currentReducedMotion by rememberUpdatedState(reducedMotion)
    return onGloballyPositioned { state.setViewport(it.positionInRoot()) }
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val id = state.handleAt(down.position) ?: return@awaitEachGesture
                val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                if (!state.start(id, longPress.position, currentOrder, currentMove, currentDrop)) return@awaitEachGesture
                try {
                    drag(longPress.id) { change ->
                        state.drag(change.positionChange().y)
                        change.consume()
                    }
                } finally {
                    state.stop(currentReducedMotion)
                }
            }
        }
}

/** Direkt am Lazy-Item anwenden, damit zIndex auch über den Nachbarkarten liegt. */
fun Modifier.reorderRow(state: ReorderState, id: Long): Modifier =
    zIndex(if (state.isDragging(id)) 1f else 0f)
        .graphicsLayer { translationY = state.translation(id) }

/**
 * Die Zeile mit der weichen Platzwechsel-Animation der Nachbarkarten — genau das macht das
 * Verschieben flüssig. Die Karte am Finger bleibt davon ausgenommen, sie folgt der Geste.
 */
fun LazyItemScope.reorderItem(state: ReorderState, id: Long, reducedMotion: Boolean): Modifier =
    Modifier
        .reorderRow(state, id)
        .animateItem(
            fadeInSpec = null,
            fadeOutSpec = null,
            placementSpec = if (state.isDragging(id) || reducedMotion) null
            else spring(dampingRatio = 1f, stiffness = 450f),
        )

@Composable
fun reorderHandle(state: ReorderState, id: Long): Modifier {
    DisposableEffect(state, id) { onDispose { state.removeHandle(id) } }
    return Modifier.onGloballyPositioned { state.measureHandle(id, it.boundsInRoot()) }
}

@Composable
fun ReorderAutoScroll(state: ReorderState) {
    val density = LocalDensity.current
    LaunchedEffect(state, state.dragging, density) {
        if (!state.dragging) return@LaunchedEffect
        state.followEdges(with(density) { 88.dp.toPx() }, with(density) { 560.dp.toPx() })
    }
}
