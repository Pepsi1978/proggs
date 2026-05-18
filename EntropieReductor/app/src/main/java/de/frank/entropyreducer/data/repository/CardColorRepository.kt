package de.frank.entropyreducer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistiert die per Pattern gewaehlte Hintergrundfarbe.
 *
 * Frank-Wunsch 2026-05-18: Jede Biomarker-Karte (HRV, Readiness, Schlaf,
 * VO2max, Schlafphasen, Tiefschlaf, ...) und perspektivisch auch der
 * Trainings-Hero, Resilienz und Gewicht koennen eine individuelle
 * Hintergrundfarbe aus einer 30er-Palette bekommen. Die Auswahl soll
 * App-Neustarts ueberleben.
 *
 * Format: Eine pipe-separierte Liste "cardId:colorIndex|cardId:colorIndex"
 * im einzigen String-Key des DataStore. Robust gegen Datenmuell — ungueltige
 * Eintraege werden beim Lesen ignoriert.
 */
private val Context.cardColorStore by preferencesDataStore(name = "biomarker_card_colors")

@Singleton
class CardColorRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Map cardId -> Color-Index (0-29). Leere Map = noch nie etwas gesetzt. */
    val cardColors: Flow<Map<String, Int>> =
        context.cardColorStore.data.map { prefs ->
            val raw = prefs[KEY_COLORS] ?: return@map emptyMap()
            raw.split('|')
                .mapNotNull { entry ->
                    val parts = entry.split(':')
                    if (parts.size != 2) return@mapNotNull null
                    val id = parts[0].trim()
                    val idx = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
                    if (id.isBlank()) null else id to idx
                }
                .toMap()
        }

    /**
     * Setzt die Farbe fuer eine Karte. Wenn [colorIndex] 0 ist, wird der
     * Eintrag entfernt (Standard = kein Override). Bei Index ausserhalb
     * 0..29 wird der Aufruf ignoriert.
     */
    suspend fun setCardColor(cardId: String, colorIndex: Int) {
        if (cardId.isBlank() || colorIndex < 0 || colorIndex > 29) return
        context.cardColorStore.edit { prefs ->
            val raw = prefs[KEY_COLORS] ?: ""
            val current =
                raw.split('|')
                    .mapNotNull { entry ->
                        val parts = entry.split(':')
                        if (parts.size != 2) null
                        else parts[0].trim() to (parts[1].trim().toIntOrNull() ?: return@mapNotNull null)
                    }
                    .toMap()
                    .toMutableMap()
            if (colorIndex == 0) {
                current.remove(cardId)
            } else {
                current[cardId] = colorIndex
            }
            prefs[KEY_COLORS] = current.entries.joinToString("|") { "${it.key}:${it.value}" }
        }
    }

    /** Setzt alle Farben zurueck (z.B. fuer einen "Farben zuruecksetzen"-Button). */
    suspend fun reset() {
        context.cardColorStore.edit { it.remove(KEY_COLORS) }
    }

    private companion object {
        val KEY_COLORS = stringPreferencesKey("card_color_indices")
    }
}
