package de.frank.jarvis.fernsteuerung

import androidx.compose.runtime.mutableStateOf

/**
 * Fernwartungszustand für das Symbol unten rechts: welche Rechner der Fernwartungs-Dienst im Tunnel gerade wirklich
 * bedienen lässt. Bewusst nicht der Jarvis-Server: Dort meldet sich die Programmier-Brücke, und die läuft auch, wenn die
 * Fernwartung des Rechners nicht erreichbar ist (grün, obwohl das Verbinden scheiterte, 28.10.2026).
 *
 * Ein einzelner verlorener Prüfversuch macht nicht rot: Im Tunnel über den VPS gehen einzelne Verbindungsversuche
 * verloren (am 29.10.2026 kam beim dauerhaft laufenden Mac nur jede zweite bis dritte Prüfung an, das Symbol pendelte).
 * Erst [FEHLVERSUCHE] Fehlschläge in Folge zählen als weg; eine gescheiterte Sitzung ([verloren]) bleibt sofort rot.
 */
object RechnerStatus {
    private const val FEHLVERSUCHE = 3

    /** null = noch nicht geprüft, leer = kein Rechner fernbedienbar. */
    val erreichbar = mutableStateOf<List<Fernziel>?>(null)

    private val fehlschlaege = mutableMapOf<Fernziel, Int>()

    suspend fun pruefen(): List<Fernziel> {
        val geantwortet = Fernziel.erreichbar()
        val bisher = erreichbar.value ?: emptyList()
        val stabil = Fernziel.entries.filter { z ->
            if (z in geantwortet) { fehlschlaege[z] = 0; true }
            else {
                val n = (fehlschlaege[z] ?: FEHLVERSUCHE) + 1
                fehlschlaege[z] = n
                z in bisher && n < FEHLVERSUCHE
            }
        }
        erreichbar.value = stabil
        return stabil
    }

    /** Eine Sitzung ist gescheitert oder abgerissen: sofort rot für diesen Rechner, bis die nächste Prüfung ihn wieder findet. */
    fun verloren(ziel: Fernziel) { fehlschlaege[ziel] = FEHLVERSUCHE; erreichbar.value = erreichbar.value?.minus(ziel) }

    /** Eine Sitzung läuft: dieser Rechner ist sicher fernbedienbar. */
    fun aktiv(ziel: Fernziel) { fehlschlaege[ziel] = 0; erreichbar.value = ((erreichbar.value ?: emptyList()) + ziel).distinct() }
}
