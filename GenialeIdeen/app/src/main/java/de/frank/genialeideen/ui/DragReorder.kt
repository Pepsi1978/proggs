package de.frank.genialeideen.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Das Sortieren per Ziehen — geteilt von den eigenen Sessions und ihren Fragen.
 *
 * Der Zustand liegt bewusst über den Zeilen: Beim Umsortieren wechseln die Zeilen ihren Platz,
 * ein Zustand in der Zeile selbst ginge dabei verloren und die gegriffene Karte fiele mitten im
 * Ziehen an ihren alten Platz zurück. Getauscht wird erst, wenn die gezogene Karte ihre Nachbarin
 * fast ganz verdeckt — so lässt sie sich in Ruhe darüber legen, ohne gleich wegzuspringen.
 */
@Stable
class ReorderState {
    var draggedId by mutableStateOf<Long?>(null)
        private set

    /** Wie weit die gegriffene Karte gerade von ihrem Platz weg liegt. */
    var offsetY by mutableStateOf(0f)
        private set

    private val heights = mutableStateMapOf<Long, Float>()
    private val tops = mutableStateMapOf<Long, Float>()
    private var viewportTop = 0f
    private var viewportBottom = 0f
    private var order: () -> List<Long> = { emptyList() }
    private var move: (Int, Int) -> Unit = { _, _ -> }

    fun isDragging(id: Long): Boolean = draggedId == id

    /** Höhe und Platz einer Zeile — beides vor der Verschiebung, also ihr echter Platz in der Liste. */
    internal fun measure(id: Long, height: Float, top: Float) {
        heights[id] = height
        tops[id] = top
    }

    internal fun setViewport(top: Float, bottom: Float) {
        viewportTop = top
        viewportBottom = bottom
    }

    internal fun start(id: Long, order: () -> List<Long>, move: (Int, Int) -> Unit) {
        draggedId = id
        offsetY = 0f
        this.order = order
        this.move = move
    }

    internal fun drag(amountY: Float) {
        offsetY += amountY
        settle()
    }

    internal fun stop() {
        draggedId = null
        offsetY = 0f
    }

    /** Tauscht mit den Nachbarn, bis die gezogene Karte wieder über ihrem eigenen Platz liegt. */
    private fun settle() {
        val id = draggedId ?: return
        while (true) {
            val current = order()
            val index = current.indexOf(id)
            if (index < 0) return
            val below = current.getOrNull(index + 1)?.let { heights[it] } ?: 0f
            if (below > 0f && offsetY > below * SWAP_SHARE) {
                move(index, index + 1)
                offsetY -= below
                continue
            }
            val above = current.getOrNull(index - 1)?.let { heights[it] } ?: 0f
            if (above > 0f && offsetY < -above * SWAP_SHARE) {
                move(index, index - 1)
                offsetY += above
                continue
            }
            return
        }
    }

    /** Wie weit die Liste mitwandern soll, wenn die Karte an den oberen oder unteren Rand kommt. */
    private fun edgeScroll(margin: Float, maxStep: Float): Float {
        val id = draggedId ?: return 0f
        val top = tops[id] ?: return 0f
        val height = heights[id] ?: return 0f
        if (viewportBottom <= viewportTop || margin <= 0f) return 0f
        val visualTop = top + offsetY
        val overBottom = visualTop + height - (viewportBottom - margin)
        if (overBottom > 0f) return (overBottom / margin).coerceAtMost(1f) * maxStep
        val overTop = viewportTop + margin - visualTop
        if (overTop > 0f) return -(overTop / margin).coerceAtMost(1f) * maxStep
        return 0f
    }

    /**
     * Lässt die Liste mitwandern, solange die Karte am Rand hängt. Was die Liste dabei wegscrollt,
     * gibt die Karte als Verschiebung zurück — so bleibt sie unter dem Finger liegen.
     */
    internal suspend fun followEdges(listState: LazyListState, margin: Float, maxStep: Float) {
        listState.scroll {
            while (true) {
                withFrameNanos { }
                val wanted = edgeScroll(margin, maxStep)
                if (wanted != 0f) {
                    offsetY += scrollBy(wanted)
                    settle()
                }
            }
        }
    }

    private companion object {
        /** Wie weit die gezogene Karte ihre Nachbarin verdecken muss, bevor getauscht wird. */
        const val SWAP_SHARE = 0.92f
    }
}

@Composable
fun rememberReorderState(): ReorderState = remember { ReorderState() }

/** Merkt sich den sichtbaren Ausschnitt der Liste — damit am Rand mitgescrollt werden kann. */
fun Modifier.reorderViewport(state: ReorderState): Modifier = onGloballyPositioned {
    val top = it.positionInRoot().y
    state.setViewport(top, top + it.size.height)
}

/**
 * Hebt die gezogene Zeile über die anderen und legt Höhe und Platz jeder Zeile ab. Das Messen
 * sitzt bewusst vor der Verschiebung, damit der abgelegte Platz der echte Platz in der Liste ist.
 */
@Composable
fun Modifier.reorderRow(state: ReorderState, id: Long): Modifier {
    val dragging = state.isDragging(id)
    val rowAlpha by animateFloatAsState(
        if (state.draggedId == null || dragging) 1f else 0.5f,
        label = "Zeile Deckkraft",
    )
    val rowScale by animateFloatAsState(if (dragging) 1.04f else 1f, label = "Zeile Größe")
    // Das gegriffene Element hebt sich an: grösser, leicht geneigt, mit wachsendem Schatten (N.7).
    val rowTilt by animateFloatAsState(if (dragging) 2f else 0f, label = "Zeile Neigung")
    val rowShadow by androidx.compose.animation.core.animateDpAsState(
        if (dragging) 20.dp else 0.dp,
        label = "Zeile Schatten",
    )
    return this
        .onGloballyPositioned { state.measure(id, it.size.height.toFloat(), it.positionInRoot().y) }
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            translationY = if (dragging) state.offsetY else 0f
            alpha = rowAlpha
            scaleX = rowScale
            scaleY = rowScale
            rotationZ = rowTilt
            shadowElevation = rowShadow.toPx()
        }
}

/** Der Griff: lang drücken, dann zieht die Zeile mit — bis zum Loslassen. */
fun reorderHandle(
    state: ReorderState,
    id: Long,
    order: () -> List<Long>,
    onMove: (Int, Int) -> Unit,
    onDrop: () -> Unit,
): Modifier = Modifier.pointerInput(id) {
    detectDragGesturesAfterLongPress(
        onDragStart = { state.start(id, order, onMove) },
        onDragEnd = {
            state.stop()
            onDrop()
        },
        onDragCancel = {
            state.stop()
            onDrop()
        },
    ) { change, amount ->
        change.consume()
        state.drag(amount.y)
    }
}

/** Hält die Liste am Wandern, solange etwas am unteren oder oberen Rand gezogen wird. */
@Composable
fun ReorderAutoScroll(state: ReorderState, listState: LazyListState) {
    val density = LocalDensity.current
    val dragged = state.draggedId
    LaunchedEffect(dragged) {
        if (dragged == null) return@LaunchedEffect
        val margin = with(density) { 120.dp.toPx() }
        val maxStep = with(density) { 8.dp.toPx() }
        state.followEdges(listState, margin, maxStep)
    }
}
