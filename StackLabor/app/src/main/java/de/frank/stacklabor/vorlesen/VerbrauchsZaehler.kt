package de.frank.stacklabor.vorlesen

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Zaehlt die vorgelesenen Zeichen je Anbieter — die Grundlage der Verbrauchsanzeige in B-10 (F-18).
 *
 * Uebernommen aus `TtsUsageStore.kt` (EntropieReductor), auf das Noetige gekuerzt: Google rechnet
 * pro gesendetem Eingabe-Zeichen ab (Leerzeichen zaehlen mit), also ist die Textlaenge die richtige
 * Groesse. Gezaehlt wird nur, was wirklich an einen Anbieter ging.
 *
 * Zwei Sichten werden gefuehrt:
 *  - **Monat**: laeuft beim Monatswechsel automatisch auf 0 zurueck (Googles Freikontingent von
 *    1 Million Zeichen erneuert sich monatlich).
 *  - **Gesamt**: laeuft weiter, damit in den Einstellungen sichtbar bleibt, wie viel insgesamt
 *    gesprochen wurde.
 */
private val Context.vorleseVerbrauchAblage: DataStore<Preferences> by preferencesDataStore(
    name = "vorlese_verbrauch",
)

class VerbrauchsZaehler(context: Context) {

    private val ablage = context.applicationContext.vorleseVerbrauchAblage

    /** Live-Stand fuer die Oberflaeche. */
    val verbrauch: Flow<Verbrauch> = ablage.data.map { werte -> lies(werte) }

    /**
     * Verbucht [zeichen] gesprochene Zeichen auf [anbieter]. Beim Monatswechsel wird der
     * Monatsstand vorher genullt, der Gesamtstand bleibt.
     */
    suspend fun buche(anbieter: Anbieter, zeichen: Int) {
        if (zeichen <= 0) return
        val jetzt = aktuellerMonat()
        ablage.edit { werte ->
            if (werte[SCHLUESSEL_MONAT] != jetzt) {
                Anbieter.entries.forEach { werte.remove(monatsSchluessel(it)) }
                werte[SCHLUESSEL_MONAT] = jetzt
            }
            val monatsSchluessel = monatsSchluessel(anbieter)
            val gesamtSchluessel = gesamtSchluessel(anbieter)
            werte[monatsSchluessel] = (werte[monatsSchluessel] ?: 0L) + zeichen
            werte[gesamtSchluessel] = (werte[gesamtSchluessel] ?: 0L) + zeichen
        }
    }

    /** Einmaliger Stand, wenn kein Flow gebraucht wird. */
    suspend fun stand(): Verbrauch = verbrauch.first()

    /** Setzt alle Zaehler zurueck (Einstellungen, "Verbrauch zuruecksetzen"). */
    suspend fun setzeZurueck() {
        ablage.edit { werte -> werte.clear() }
    }

    private fun lies(werte: Preferences): Verbrauch {
        val jetzt = aktuellerMonat()
        val monatPasst = werte[SCHLUESSEL_MONAT] == jetzt
        val imMonat = Anbieter.entries.associateWith { anbieter ->
            if (monatPasst) werte[monatsSchluessel(anbieter)] ?: 0L else 0L
        }
        val gesamt = Anbieter.entries.associateWith { anbieter ->
            werte[gesamtSchluessel(anbieter)] ?: 0L
        }
        return Verbrauch(monat = jetzt, imMonat = imMonat, insgesamt = gesamt)
    }

    private fun aktuellerMonat(): String {
        val monat = YearMonth.now()
        return "%04d-%02d".format(monat.year, monat.monthValue)
    }

    private companion object {
        val SCHLUESSEL_MONAT = stringPreferencesKey("monat")

        fun monatsSchluessel(anbieter: Anbieter) = longPreferencesKey("monat_${anbieter.kennung}")

        fun gesamtSchluessel(anbieter: Anbieter) = longPreferencesKey("gesamt_${anbieter.kennung}")
    }
}

/**
 * Stand der Zaehler. [monat] hat die Form `2026-08`.
 */
data class Verbrauch(
    val monat: String,
    val imMonat: Map<Anbieter, Long>,
    val insgesamt: Map<Anbieter, Long>,
) {
    val monatGesamt: Long get() = imMonat.values.sum()

    val alleGesamt: Long get() = insgesamt.values.sum()

    fun imMonat(anbieter: Anbieter): Long = imMonat[anbieter] ?: 0L

    fun insgesamt(anbieter: Anbieter): Long = insgesamt[anbieter] ?: 0L

    /**
     * Noch freier Anteil des Google-Freikontingents in Prozent (0..100). Nur Google hat ein
     * bezifferbares Monatslimit; Edge ist kostenfrei, der Stimmklon rechnet nach Aufrufen ab.
     */
    val googleFreiProzent: Int
        get() {
            val frei = (GOOGLE_MONATSLIMIT - imMonat(Anbieter.GOOGLE_CLOUD))
                .coerceIn(0L, GOOGLE_MONATSLIMIT)
            return ((frei * 100L) / GOOGLE_MONATSLIMIT).toInt()
        }

    companion object {
        /** Googles Gratis-Kontingent fuer Chirp 3 HD: 1 Million Zeichen pro Monat. */
        const val GOOGLE_MONATSLIMIT = 1_000_000L

        val LEER = Verbrauch(monat = "", imMonat = emptyMap(), insgesamt = emptyMap())
    }
}
