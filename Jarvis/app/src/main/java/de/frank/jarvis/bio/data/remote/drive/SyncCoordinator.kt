package de.frank.jarvis.bio.data.remote.drive

/**
 * Gegenstück zum Drive-Abgleich von Entropie Reductor. In Jarvis sichert und spiegelt der Geräteabgleich die Biomarker
 * (Handy → Server → Tablet): Jede Änderung, die der übernommene Code hier meldet, stößt ihn an ([de.frank.jarvis.bio.BioSpiegel]).
 */
class SyncCoordinator(private val aenderung: (String) -> Unit) {
    fun requestSync(grund: String, @Suppress("UNUSED_PARAMETER") verzoegerungMs: Long = 0L) = aenderung(grund)

    companion object {
        const val BIOMARKER_DEBOUNCE_MS = 5_000L
    }
}
