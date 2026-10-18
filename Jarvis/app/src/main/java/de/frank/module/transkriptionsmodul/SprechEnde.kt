// ──────────────────────────────────────────────────────────────────────
// Modul M1.4 — Transkriptionsmodul · Stand v2
// Quelle: Module/Android/M1.4-Transkriptionsmodul/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.4 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

import kotlin.math.max

/**
 * Merkt nach „Hey Jarvis“, wann Frank fertig gesprochen hat, damit niemand tippen muss.
 *
 * Vorrang hat die Sprachaktivitätserkennung (`MicRecorder.sprachRahmen`, Silero): Nur erkannte Stimme hält die
 * Aufnahme offen. Motor-, Reifen- und Windgeräusch im Auto sind oft lauter als leise Sprache und schwanken; am Pegel
 * gemessen setzte jede Spitze den Stille-Timer zurück, und das Mikrofon blieb offen.
 *
 * Ohne Spracherkennung (Modell nicht geladen) gilt wie früher der Pegel aus `MicRecorder.pegel` (0 bis 1, Wurzel des
 * Effektivwerts) mit einer Schwelle, die sich dem Grundrauschen anpasst. Der Piepton am Anfang zählt nie mit.
 */
class SprechEnde(
    /** So lange nach dem Start wird nichts gewertet: Hier läuft der Piepton. */
    private val anlaufMs: Long = 250,
    /** Kommt bis hierhin keine Sprache, war es ein Fehlalarm oder Frank hat es sich anders überlegt. */
    private val wartenMs: Long = 7_000,
    /** So lange Stille nach dem letzten Wort beendet die Aufnahme. Großzügig, damit Frank mitten im Satz nachdenken kann. */
    private val stilleMs: Long = 4_500,
    /** Höchstdauer einer Frage. */
    private val hoechstMs: Long = 120_000,
) {
    enum class Lage { WEITER, FERTIG, NICHTS_GEHOERT }

    /** Grundrauschen; null bis zum ersten Wert nach dem Anlauf. */
    private var boden: Float? = null
    var gesprochen = false
        private set
    /** Wann (ms seit Start) zuletzt Sprache kam; für eine vorgezogene Mitschrift während der Nachdenkpause. */
    var letzteSpracheMs = 0L
        private set

    /** Zuletzt gesehener Stand von `sprachRahmen`; null bis zum ersten Wert. */
    private var sprachStand: Long? = null

    /**
     * Ein neuer Messwert, [ms] seit dem Start der Aufnahme. [sprachRahmen] ist der Zähler der Spracherkennung
     * (wächst nur, solange Stimme erkannt wird), null ohne sie.
     */
    fun neu(pegel: Float, ms: Long, sprachRahmen: Long? = null): Lage {
        if (ms >= hoechstMs) return if (gesprochen) Lage.FERTIG else Lage.NICHTS_GEHOERT
        // Boden fällt sofort auf leisere Werte und steigt nur langsam (über einige Sekunden) nach. Spricht Frank
        // schon beim ersten Wert, fällt er in der ersten Wortpause auf das echte Rauschen. Läuft auch mit
        // Spracherkennung mit, damit der Pegel einspringen kann, falls sie unterwegs ausfällt.
        val imAnlauf = ms < anlaufMs
        if (!imAnlauf) {
            val alt = boden
            boden = if (alt == null || pegel < alt) pegel else alt + (pegel - alt) * BODEN_ANSTIEG
        }
        val sprache = if (sprachRahmen != null) {
            // Im Anlauf (Piepton) nur den Stand merken; danach zählt jeder neue Sprachrahmen seit dem letzten Blick.
            val neuGesprochen = sprachStand?.let { sprachRahmen > it } ?: false
            sprachStand = sprachRahmen
            neuGesprochen && !imAnlauf
        } else {
            !imAnlauf && pegel > schwelle()
        }
        if (imAnlauf) return Lage.WEITER
        if (sprache) {
            gesprochen = true
            letzteSpracheMs = ms
        }
        if (!gesprochen) return if (ms >= wartenMs) Lage.NICHTS_GEHOERT else Lage.WEITER
        return if (ms - letzteSpracheMs >= stilleMs) Lage.FERTIG else Lage.WEITER
    }

    internal fun schwelle(): Float = max(MIN_SCHWELLE, (boden ?: 0f) * 1.5f + 0.05f)

    companion object {
        /** Darunter gilt nichts als Sprache, auch in sehr stiller Umgebung. */
        private const val MIN_SCHWELLE = 0.15f
        private const val BODEN_ANSTIEG = 0.01f
    }
}
