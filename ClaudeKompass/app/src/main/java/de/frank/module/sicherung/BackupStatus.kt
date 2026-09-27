// ──────────────────────────────────────────────────────────────────────
// Modul M1.1 — Sicherung · Stand v7
// Quelle: Module/Android/M1.1-Sicherung/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.1 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sicherung

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Merkt sich, wann zuletzt wirklich eine Sicherung geschrieben wurde.
 *
 * Ein Sicherungsschalter, der nur „an" sagt, ist wertlos — er kann eingeschaltet sein, ohne dass
 * je etwas geschrieben wurde. Deshalb wird der Zeitpunkt jedes abgeschlossenen Laufs gestempelt
 * und im Einstellungsbildschirm angezeigt. Damit wird eine tote Sicherung sichtbar, statt still
 * zu beruhigen.
 *
 * Bewusst in der offenen Ablage: Das ist eine örtliche Tatsache über dieses Gerät und hat in der
 * Sicherungsdatei selbst nichts verloren.
 */
class BackupStatus(
    /**
     * Die `SharedPreferences`-Datei, in der der Stand liegt.
     *
     * Kommt von aussen, weil jede App ihre eigene hat. Stand hier ein fester Name, trüge jede
     * fremde App die Ablage der Ursprungs-App mit sich herum — und eine bestehende App verlöre
     * beim Umstieg auf das Modul ihren bisherigen Zeitstempel, ohne dass es jemandem auffällt.
     */
    private val prefsDatei: String,
) {

    /**
     * Je Aufruf eine eigene Instanz: SimpleDateFormat ist nicht threadsicher, und [describe]
     * wird sowohl aus der Oberfläche als auch aus der selbsttätigen Sicherung gerufen. Eine
     * geteilte Instanz liefert bei gleichzeitigem Zugriff stillschweigend falsche Zeiten.
     */
    private fun format() = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)

    /**
     * Stempelt den Zeitpunkt — und ob die geschriebene Datei danach fehlerfrei gelesen wurde.
     *
     * Die beiden sind nicht dasselbe: Eine Datei kann angelegt und trotzdem unbrauchbar sein.
     * Nur der Prüfvermerk rechtfertigt das grüne Häkchen.
     */
    fun markBackedUp(context: Context, geprueft: Boolean) {
        // `commit()`, nicht `apply()`: Beides hier wird am Ende eines Laufs geschrieben, der
        // typischerweise beim Verlassen der App stattfindet — also kurz bevor Android den
        // Vorgang beendet. Ein `apply()` schreibt später auf einem eigenen Faden und kommt dann
        // womöglich nicht mehr dazu. Dann steht in der Anzeige weiter die alte Uhrzeit, obwohl
        // längst gesichert wurde: genau die Lüge, gegen die es diesen Stempel gibt. Beide
        // Aufrufe kommen aus dem Hintergrund, blockieren also niemanden.
        context.applicationContext
            .getSharedPreferences(prefsDatei, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP, System.currentTimeMillis())
            .putBoolean(KEY_GEPRUEFT, geprueft)
            .putBoolean(KEY_GESCHEITERT, false)
            .commit()
    }

    /**
     * Vermerkt einen Fehlschlag, ohne den Zeitpunkt der letzten geglückten Sicherung zu
     * überschreiben.
     *
     * Sonst stünde nach einem gescheiterten Lauf eine frische Uhrzeit da — für eine Datei,
     * die niemand lesen kann. Die alte Angabe ist die ehrlichere.
     */
    fun markGescheitert(context: Context) {
        context.applicationContext
            .getSharedPreferences(prefsDatei, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GESCHEITERT, true)
            .commit()
    }

    private fun istGescheitert(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(prefsDatei, Context.MODE_PRIVATE)
            .getBoolean(KEY_GESCHEITERT, false)

    fun istGeprueft(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(prefsDatei, Context.MODE_PRIVATE)
            .getBoolean(KEY_GEPRUEFT, false)

    private fun lastBackupAt(context: Context): Long =
        context.applicationContext
            .getSharedPreferences(prefsDatei, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP, 0L)

    /** Behauptet nie eine Sicherung, die es nicht gab. */
    fun describe(context: Context): String {
        val last = lastBackupAt(context)
        if (last == 0L) {
            return if (istGescheitert(context)) {
                "Noch keine Sicherung — der letzte Versuch schlug fehl"
            } else {
                "Noch nicht gesichert"
            }
        }
        val zeit = "Zuletzt: ${format().format(Date(last))} Uhr"
        val stand = if (istGeprueft(context)) "$zeit — geprüft" else "$zeit — UNGEPRÜFT"
        // Der letzte Versuch und die letzte geglückte Sicherung sind zwei verschiedene Dinge.
        // Ein Fehlschlag darf den Haken an der guten Sicherung davor nicht entwerten — aber
        // er muss sichtbar sein, sonst hält man den alten Stand für den aktuellen.
        return if (istGescheitert(context)) "$stand; der letzte Versuch schlug fehl" else stand
    }

    fun hasBackup(context: Context): Boolean = lastBackupAt(context) > 0L

    /**
     * Merkt sich, dass eine Änderung noch ungesichert ist — über den Vorgangstod hinweg.
     *
     * Bewusst `commit()` und nicht `apply()`: `apply()` schreibt später auf einem eigenen Faden.
     * Genau der Fall, um den es hier geht — Android beendet den Vorgang, während die Sicherung
     * noch läuft —, ist auch der Fall, in dem ein `apply()` es nicht mehr auf die Platte
     * schafft. Dann stünde beim nächsten Start nichts Offenes da, und die Änderung wäre
     * endgültig ungesichert. Die Aufrufe kommen aus dem Hintergrund, nicht vom Hauptfaden.
     */
    fun merkeOffen(context: Context, offen: Boolean) {
        context.applicationContext
            .getSharedPreferences(prefsDatei, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OFFEN, offen)
            .commit()
    }

    /** Steht seit dem letzten Mal noch eine ungesicherte Änderung aus? */
    fun istOffen(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(prefsDatei, Context.MODE_PRIVATE)
            .getBoolean(KEY_OFFEN, false)

    private companion object {
        private const val KEY_LAST_BACKUP = "zuletzt_gesichert"
        private const val KEY_GEPRUEFT = "zuletzt_geprueft"
        private const val KEY_GESCHEITERT = "letzter_versuch_gescheitert"
        private const val KEY_OFFEN = "aenderung_offen"
    }
}
