// ──────────────────────────────────────────────────────────────────────
// Modul M1.4 — Transkriptionsmodul · Stand v2
// Quelle: Module/Android/M1.4-Transkriptionsmodul/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.4 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

/**
 * Protokoll-Anschluss der App. Das Modul schreibt seine Ereignisse hierher (Mitschrift-Dauer, Netzfehler beim
 * Vorwärmen, Schnitt langer Diktate); ohne Anbindung gehen sie ins Leere. Die App setzt [aktiv] einmal, bevor sie
 * das erste Mal aufnimmt.
 */
interface Protokoll {
    fun sprache(ereignis: String, details: String = "", ms: Long? = null) {}
    fun netz(ereignis: String, details: String = "") {}
    fun info(bereich: String, funktion: String, text: String, daten: Map<String, Any?> = emptyMap()) {}
    fun warn(bereich: String, funktion: String, text: String, daten: Map<String, Any?> = emptyMap()) {}

    companion object {
        @Volatile
        var aktiv: Protokoll = object : Protokoll {}
    }
}
