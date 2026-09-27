package de.frank.wecker

import kotlin.math.ceil
import kotlin.math.max

/**
 * Sieben Tage voller Funktionsumfang ab Ersteinrichtung – reine Logik mit übergebener Uhr, ohne Android.
 *
 * Grenzen ohne Server (bewusst akzeptiert):
 * - Vorgestellte Uhr verkürzt den Test höchstens; zurückgestellte verlängert ihn nie (höchste gesehene Zeit zählt).
 * - Wer App-Daten löscht UND kein Google-Backup hat, bekommt einen neuen Test (firstInstallTime springt).
 */
object Testzeitraum {
    const val TAGE = 7
    private const val TAG_MS = 24L * 60 * 60 * 1000
    const val DAUER_MS = TAGE * TAG_MS

    data class Stand(val start: Long, val wirksameZeit: Long) {
        val ende: Long get() = start + DAUER_MS
        val aktiv: Boolean get() = wirksameZeit < ende
        /** Angebrochene Tage zählen voll: am ersten Tag „7“, im letzten Rest „1“, danach 0. */
        val restTage: Int get() = if (!aktiv) 0 else ceil((ende - wirksameZeit).toDouble() / TAG_MS).toInt().coerceIn(1, TAGE)
    }

    /**
     * @param ersteInstallation `PackageManager.firstInstallTime` (springt bei Neuinstallation)
     * @param anker gespeicherter Beginn, per Backup wiederherstellbar; null beim allerersten Start
     * @param hoechsteZeit höchste bisher gesehene Wanduhrzeit
     */
    fun berechne(ersteInstallation: Long, anker: Long?, jetzt: Long, hoechsteZeit: Long): Stand {
        val start = listOfNotNull(ersteInstallation.takeIf { it > 0 }, anker?.takeIf { it > 0 }).minOrNull() ?: jetzt
        return Stand(start, max(jetzt, hoechsteZeit))
    }
}

/** Kauf-Auswertung ohne Billing-Typen, damit sie ohne Gerät prüfbar ist. */
object KaufLogik {
    enum class Zustand { GEKAUFT, BESTAETIGUNG_NOETIG, AUSSTEHEND, KEINER }
    data class Kauf(val produkte: List<String>, val gekauft: Boolean, val ausstehend: Boolean, val bestaetigt: Boolean, val token: String)

    fun bewerte(kaeufe: List<Kauf>, produkt: String): Pair<Zustand, List<String>> {
        val passend = kaeufe.filter { produkt in it.produkte }
        val offen = passend.filter { it.gekauft && !it.bestaetigt }.map { it.token }
        return when {
            passend.any { it.gekauft && it.bestaetigt } -> Zustand.GEKAUFT to offen
            offen.isNotEmpty() -> Zustand.BESTAETIGUNG_NOETIG to offen
            passend.any { it.ausstehend } -> Zustand.AUSSTEHEND to emptyList()
            else -> Zustand.KEINER to emptyList()
        }
    }
}
