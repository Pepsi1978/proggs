package de.frank.cortex.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.frank.cortex.data.SettingsStore

/**
 * Stimm-Favoriten fuer ALLE Stimmlisten (Chirp, Gemini, Edge, Azure, eigene Stimmen): per langem Druecken
 * oder Tippen auf den Stern markieren; Favoriten stehen in der Liste oben, mit gelbem Stern.
 * Chirp und Gemini teilen sich die Stimmnamen, ein Favorit gilt deshalb in beiden Listen.
 */
object VoiceFavorites {
    private val state = mutableStateOf<Set<String>?>(null)

    /** Liest im Compose-Zustand, damit ein Umschalten die Listen sofort neu sortiert. */
    val current: Set<String>
        get() = state.value ?: SettingsStore.voiceFavorites.also { state.value = it }

    fun isFavorite(id: String): Boolean = id in current

    fun toggle(id: String) {
        val now = current
        val next = if (id in now) now - id else now + id
        SettingsStore.voiceFavorites = next
        state.value = next
    }
}

val FavoriteYellow = Color(0xFFFFC107)

/** Tippen waehlt, langes Druecken schaltet den Favoriten um. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.voiceClick(id: String, onSelect: () -> Unit): Modifier = combinedClickable(
    onClick = onSelect,
    onLongClick = { VoiceFavorites.toggle(id) },
    onLongClickLabel = "Favorit an/aus",
)

@Composable
fun FavoriteStar(id: String) {
    val favorite = VoiceFavorites.isFavorite(id)
    IconButton(onClick = { VoiceFavorites.toggle(id) }, modifier = Modifier.size(30.dp)) {
        Icon(
            if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            if (favorite) "Favorit entfernen" else "Als Favorit markieren",
            tint = if (favorite) FavoriteYellow else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
