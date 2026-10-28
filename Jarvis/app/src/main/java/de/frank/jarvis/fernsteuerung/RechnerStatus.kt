package de.frank.jarvis.fernsteuerung

import androidx.compose.runtime.mutableStateOf

/**
 * Fernwartungszustand für das Symbol unten rechts: welche Rechner der Fernwartungs-Dienst im Tunnel gerade wirklich
 * bedienen lässt. Bewusst nicht der Jarvis-Server: Dort meldet sich die Programmier-Brücke, und die läuft auch, wenn die
 * Fernwartung des Rechners nicht erreichbar ist (grün, obwohl das Verbinden scheiterte, 28.10.2026).
 */
object RechnerStatus {
    /** null = noch nicht geprüft, leer = kein Rechner fernbedienbar. */
    val erreichbar = mutableStateOf<List<Fernziel>?>(null)

    suspend fun pruefen(): List<Fernziel> = Fernziel.erreichbar().also { erreichbar.value = it }

    /** Eine Sitzung ist gescheitert oder abgerissen: sofort rot für diesen Rechner, bis die nächste Prüfung ihn wieder findet. */
    fun verloren(ziel: Fernziel) { erreichbar.value = erreichbar.value?.minus(ziel) }

    /** Eine Sitzung läuft: dieser Rechner ist sicher fernbedienbar. */
    fun aktiv(ziel: Fernziel) { erreichbar.value = ((erreichbar.value ?: emptyList()) + ziel).distinct() }
}
