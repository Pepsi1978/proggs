package de.frank.jarvis.fernsteuerung

import androidx.compose.runtime.mutableStateOf
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fernwartungszustand für das Symbol unten rechts: welche Rechner der Fernwartungs-Dienst im Tunnel gerade wirklich
 * bedienen lässt. Bewusst nicht der Jarvis-Server: Dort meldet sich die Programmier-Brücke, und die läuft auch, wenn die
 * Fernwartung des Rechners nicht erreichbar ist (grün, obwohl das Verbinden scheiterte, 28.10.2026).
 *
 * Im Tunnel über den VPS gehen einzelne Verbindungsversuche verloren (29.10.2026: beim dauerhaft laufenden Mac kam nur
 * jede zweite bis dritte Prüfung an, das Symbol pendelte). Darum versucht [Fernziel.pruefen] es selbst ein zweites Mal,
 * und das Symbol bleibt grün, solange die letzte echte Antwort keine [SCHONFRIST_MS] her ist. Nach langer Zeit im
 * Hintergrund ist die Frist abgelaufen, die erste gescheiterte Prüfung färbt dann sofort rot.
 * [pruefen] liefert trotzdem das ungeglättete Ergebnis: Beim Antippen wird nur mit einem Rechner verbunden, der gerade
 * wirklich geantwortet hat.
 *
 * Logbuch (Art „netz“, Ereignisse „Fernwartung: …“): jede gescheiterte Prüfung mit Grund und Dauer, jeder Farbwechsel mit
 * Auslöser. Erfolgreiche Prüfungen ohne Wechsel bleiben draußen (alle 10 s). Alle Aufrufe laufen auf dem Main-Thread.
 */
object RechnerStatus {
    private const val SCHONFRIST_MS = 25_000L

    /** null = noch nicht geprüft, leer = kein Rechner fernbedienbar (geglättet, nur für die Anzeige). */
    val erreichbar = mutableStateOf<List<Fernziel>?>(null)

    private val zuletztDa = mutableMapOf<Fernziel, Long>()
    /** Zählt Sitzungsereignisse je Rechner: Eine Prüfung, während der eine Sitzung begann oder abriss, ist veraltet. */
    private val ereignisse = mutableMapOf<Fernziel, Int>()
    private val sperre = Mutex()

    /** Prüft jetzt alle Rechner, aktualisiert das Symbol und liefert die Rechner, die gerade wirklich geantwortet haben. */
    suspend fun pruefen(): List<Fernziel> = sperre.withLock {
        val stand = Fernziel.entries.associateWith { ereignisse[it] ?: 0 }
        val ergebnisse = Fernziel.pruefen()
        val jetzt = System.currentTimeMillis()
        val bisher = erreichbar.value
        val anzeige = ergebnisse.mapNotNull { p ->
            val z = p.ziel
            val warDa = bisher?.contains(z) == true
            if ((ereignisse[z] ?: 0) != stand[z]) return@mapNotNull z.takeIf { erreichbar.value?.contains(z) == true }
            if (p.ok) {
                zuletztDa[z] = jetzt
                if (!warDa) Logbuch.netz("Fernwartung: ${z.name} grün", "${p.grund}, ${p.ms} ms")
                return@mapNotNull z
            }
            val seit = zuletztDa[z]?.let { jetzt - it }
            val bleibt = warDa && seit != null && seit < SCHONFRIST_MS
            when {
                bleibt -> Logbuch.netz("Fernwartung: ${z.name} Prüfung gescheitert, bleibt grün", "letzte Antwort vor ${seit!! / 1000} s, ${p.ms} ms: ${p.grund}")
                warDa -> Logbuch.netz("Fernwartung: ${z.name} rot", "letzte Antwort vor ${seit?.let { "${it / 1000} s" } ?: "unbekannt"}, ${p.ms} ms: ${p.grund}")
                bisher == null -> Logbuch.netz("Fernwartung: ${z.name} rot", "erste Prüfung, ${p.ms} ms: ${p.grund}")
            }
            z.takeIf { bleibt }
        }
        erreichbar.value = anzeige
        ergebnisse.filter { it.ok }.map { it.ziel }
    }

    /**
     * Eine Sitzung ist gescheitert oder abgerissen. Kam gar keine Verbindung zustande ([sofort]), sofort rot. Riss eine
     * laufende ab, kann das derselbe Tunnel-Aussetzer sein: nicht sofort rot, aber ohne Schonfrist, die nächste
     * gescheiterte Prüfung färbt rot.
     */
    fun verloren(ziel: Fernziel, sofort: Boolean, grund: String) {
        ereignisse[ziel] = (ereignisse[ziel] ?: 0) + 1
        zuletztDa.remove(ziel)
        val warDa = erreichbar.value?.contains(ziel) == true
        if (sofort && warDa) erreichbar.value = erreichbar.value?.minus(ziel)
        Logbuch.netz("Fernwartung: ${ziel.name} " + if (sofort && warDa) "rot" else "Sitzung beendet", grund)
    }

    /** Eine Sitzung läuft: dieser Rechner ist sicher fernbedienbar. */
    fun aktiv(ziel: Fernziel) {
        ereignisse[ziel] = (ereignisse[ziel] ?: 0) + 1
        zuletztDa[ziel] = System.currentTimeMillis()
        if (erreichbar.value?.contains(ziel) != true) Logbuch.netz("Fernwartung: ${ziel.name} grün", "Sitzung läuft")
        erreichbar.value = ((erreichbar.value ?: emptyList()) + ziel).distinct()
    }
}
