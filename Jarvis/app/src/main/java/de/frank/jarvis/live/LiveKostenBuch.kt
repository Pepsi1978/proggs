package de.frank.jarvis.live

import android.content.Context
import android.content.SharedPreferences

/**
 * Dauerhafte Summe aller Live-Gespräche seit dem letzten Zurücksetzen. Jedes Gespräch wird am Ende genau einmal verbucht
 * ([LiveGespraech] meldet dafür eine [Abrechnung]).
 *
 * Zwei Töpfe, nie vermischt:
 * - **berechnet**: aus den Nutzungsdaten, die OpenAI im Gespräch liefert (`usage.seconds` bei GPT-Live, Tokens aus
 *   `response.done`/`response.completed`), mal Listenpreis aus [LiveOptionen]. Das ist keine Rechnung, aber nachvollziehbar.
 * - **geschätzt**: Gesprächszeit, für die OpenAI keine Dauer bestätigt hat (Abbruch vor `session.closed`), mit 0,05 $/Min.
 */
object LiveKostenBuch {

    /** Ein beendetes Gespräch. [dauerSek] = von OpenAI bestätigte Dauer, sonst die von der App gemessene. */
    class Abrechnung(
        val dauerSek: Double,
        val dauerBestaetigt: Boolean,
        val berechnetUsd: Double,
        val geschaetztUsd: Double,
        /** Antworten mit Nutzungsdaten, für deren Modell Jarvis keinen Preis kennt: fehlen in der Summe. */
        val ohnePreis: Int,
    )

    data class Stand(
        val seit: Long,
        val gespraeche: Int,
        val dauerSek: Double,
        val bestaetigt: Int,
        val berechnetUsd: Double,
        val geschaetztUsd: Double,
        val ohnePreis: Int,
        val zuletzt: Long,
    ) {
        val gesamtUsd get() = berechnetUsd + geschaetztUsd
        val proMinute get() = if (dauerSek >= 1) gesamtUsd / (dauerSek / 60.0) else null
        val proStunde get() = proMinute?.times(60)

        /**
         * Gesamtsumme samt laufendem Gespräch für die Live-Leiste: Solange [laufendUsd] noch nicht verbucht ist, kommt er
         * dazu; danach steckt das Gespräch schon in der Summe und zählt nicht ein zweites Mal.
         */
        fun mitLaufendem(laufendUsd: Double, verbucht: Boolean) = gesamtUsd + if (verbucht) 0.0 else laufendUsd
    }

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("live_kosten", Context.MODE_PRIVATE)
    private fun SharedPreferences.zahl(k: String) = getString(k, null)?.toDoubleOrNull() ?: 0.0

    fun lesen(c: Context) = lesen(prefs(c))
    fun buchen(c: Context, a: Abrechnung) = buchen(prefs(c), a)
    fun zuruecksetzen(c: Context) = zuruecksetzen(prefs(c))

    @Synchronized
    fun lesen(p: SharedPreferences): Stand {
        // Erster Aufruf: die Zeitbasis beginnt jetzt, nicht 1970.
        val seit = p.getLong("seit", 0L).takeIf { it > 0 } ?: System.currentTimeMillis().also { p.edit().putLong("seit", it).apply() }
        return Stand(seit, p.getInt("gespraeche", 0), p.zahl("dauer"), p.getInt("bestaetigt", 0),
            p.zahl("berechnet"), p.zahl("geschaetzt"), p.getInt("ohne_preis", 0), p.getLong("zuletzt", 0L))
    }

    @Synchronized
    fun buchen(p: SharedPreferences, a: Abrechnung): Stand {
        val s = lesen(p)
        p.edit()
            .putInt("gespraeche", s.gespraeche + 1)
            .putString("dauer", (s.dauerSek + a.dauerSek).toString())
            .putInt("bestaetigt", s.bestaetigt + if (a.dauerBestaetigt) 1 else 0)
            .putString("berechnet", (s.berechnetUsd + a.berechnetUsd).toString())
            .putString("geschaetzt", (s.geschaetztUsd + a.geschaetztUsd).toString())
            .putInt("ohne_preis", s.ohnePreis + a.ohnePreis)
            .putLong("zuletzt", System.currentTimeMillis())
            .commit()
        return lesen(p)
    }

    @Synchronized
    fun zuruecksetzen(p: SharedPreferences): Stand {
        p.edit().clear().putLong("seit", System.currentTimeMillis()).commit()
        return lesen(p)
    }
}
