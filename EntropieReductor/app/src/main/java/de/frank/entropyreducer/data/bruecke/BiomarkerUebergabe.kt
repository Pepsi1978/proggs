package de.frank.entropyreducer.data.bruecke

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Der Biomarker-Bereich ist an Jarvis übergeben (Franks Vorgabe vom 11.11.2026: Jarvis ruft Whoop, Oura, Waage und
 * Trainings selbst ab, Entropie Reductor nicht mehr).
 *
 * Ab dem Moment, in dem Jarvis die Übergabe vorbereitet, tauscht Entropie Reductor KEINEN Whoop-Token mehr und fragt keine
 * Biomarker-Quelle mehr ab: Whoop entwertet bei jedem Refresh den alten Refresh-Token, zwei Apps mit demselben Zugang würden
 * sich gegenseitig abmelden. Der Merker liegt in einer eigenen Datei und wird mit commit() geschrieben, damit er vor dem Export
 * sicher auf der Platte steht. Scheitert die Übernahme in Jarvis, hebt Jarvis ihn wieder auf ([zurueck]).
 */
object BiomarkerUebergabe {
    private const val DATEI = "biomarker_uebergabe"
    private const val ABGEGEBEN = "abgegeben_ms"
    private const val BESTAETIGT = "bestaetigt_ms"

    private fun prefs(context: Context) = context.applicationContext.also { app = it }.getSharedPreferences(DATEI, Context.MODE_PRIVATE)

    @Volatile private var app: Context? = null

    /** Beim Start der App (EntropyReducerApp.onCreate), damit auch Klassen ohne Context den Merker lesen können. */
    fun einrichten(context: Context) { app = context.applicationContext }

    /** Für die Datenquellen (Zepp, Oura, Whoop, Health Connect): nach der Übergabe nie mehr abrufen, auch nicht per Knopf. */
    fun abgegeben(): Boolean = app?.let { abgegeben(it) } ?: false

    /** Fehler für einen gesperrten Abruf. */
    fun gesperrt() = IllegalStateException("Die Biomarker laufen jetzt in Jarvis; Entropie Reductor ruft nichts mehr ab.")

    /** true = Jarvis führt die Biomarker; hier wird nichts mehr abgerufen und kein Whoop-Token getauscht. */
    fun abgegeben(context: Context): Boolean = prefs(context).getLong(ABGEGEBEN, 0L) > 0L

    fun bestaetigt(context: Context): Boolean = prefs(context).getLong(BESTAETIGT, 0L) > 0L

    fun einfrieren(context: Context) {
        if (!abgegeben(context)) prefs(context).edit().putLong(ABGEGEBEN, System.currentTimeMillis()).commit()
    }

    /** Jarvis hat alles übernommen: Die Whoop-Rückmeldeadresse gehört ab jetzt Jarvis (sonst fragte Android bei jeder Anmeldung, welche App). */
    fun bestaetigen(context: Context) {
        prefs(context).edit().putLong(BESTAETIGT, System.currentTimeMillis()).commit()
        runCatching {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, "net.openid.appauth.RedirectUriReceiverActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP,
            )
        }
    }

    /** Die Übernahme in Jarvis ist gescheitert: Entropie Reductor führt die Biomarker weiter wie bisher. */
    fun zurueck(context: Context) {
        if (bestaetigt(context)) return
        prefs(context).edit().remove(ABGEGEBEN).commit()
    }
}
