// ──────────────────────────────────────────────────────────────────────
// Modul M1.4 — Transkriptionsmodul · Stand v3
// Quelle: Module/Android/M1.4-Transkriptionsmodul/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.4 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

data class GroqTranscriptionResponse(
    val text: String,
    val segments: List<GroqSegment>? = null,
)

data class GroqSegment(
    val start: Double?,
    val end: Double?,
    val text: String?,
    val noSpeechProbability: Double?,
    val averageLogProbability: Double?,
    val compressionRatio: Double?,
)
