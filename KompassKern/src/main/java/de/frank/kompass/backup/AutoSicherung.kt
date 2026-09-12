package de.frank.kompass.backup

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import de.frank.kompass.observability.KompassLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sichert von allein, sobald sich etwas geändert hat.
 *
 * Eine Sicherung, die man von Hand anstossen muss, sichert genau bis zu dem Tag, an dem man es
 * vergisst — und das merkt man erst, wenn das Gerät weg ist. Deshalb meldet jede Stelle, die
 * etwas Eigenes anlegt, hier eine Änderung an.
 *
 * Zwei Dinge verhindern, dass daraus ein Dauerlauf wird:
 *
 *  - **Ruhezeit.** Nach einer Änderung wird [RUHE_MS] gewartet. Jede weitere Änderung setzt die
 *    Wartezeit zurück. Wer zehn Fragen hintereinander stellt, löst damit eine Sicherung aus,
 *    nicht zehn. Ohne das schriebe jede getippte Antwort eine ganze Datei in die Cloud.
 *  - **Beim Verlassen sofort.** Geht die App in den Hintergrund, während noch etwas aussteht,
 *    wird ohne weiteres Warten gesichert. Sonst ginge die Änderung verloren, sobald Android den
 *    Vorgang beendet — und das tut es gern genau dann.
 *
 * Ein Fehlschlag bleibt ohne Aufhebens: Die Meldung steht im Protokoll, der Zeitstempel der
 * letzten geglückten Sicherung bleibt stehen. Ein rot blinkender Hinweis mitten im Tippen wäre
 * die falsche Antwort auf ein fehlendes WLAN.
 */
class AutoSicherung(
    private val dienst: SicherungsDienst,
    private val istAn: () -> Boolean,
) : DefaultLifecycleObserver {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val schloss = Mutex()
    private var wartend: Job? = null

    @Volatile
    private var offen = false

    /** Wann zuletzt etwas gemeldet wurde — daran hängt die Ruhezeit. */
    @Volatile
    private var letzteMeldung = 0L

    /**
     * Meldet, dass sich etwas geändert hat, das gesichert gehört.
     *
     * Es läuft immer höchstens ein wartender Auftrag. Eine weitere Meldung schiebt nur den
     * Zeitpunkt nach hinten, den der wartende Auftrag abliest — sie wirft ihn nicht weg und
     * setzt keinen neuen auf. Vorher geschah genau das: Ein Aktualisieren-Lauf mit
     * vierhundert Einträgen legte vierhundert Aufträge an und brach neunundneunzig Prozent
     * davon sofort wieder ab. Das Verhalten nach aussen bleibt dasselbe — gesichert wird
     * [RUHE_MS] nach der letzten Meldung.
     */
    fun melde(grund: String) {
        if (!istAn() || dienst.sicherungsOrdner == null) return
        offen = true
        letzteMeldung = System.currentTimeMillis()
        if (wartend?.isActive == true) return
        wartend = bereich.launch { warteUndSichere(grund) }
    }

    /**
     * Wartet, bis seit der letzten Meldung [RUHE_MS] Ruhe war, und sichert dann.
     *
     * Die Schleife ist nötig, weil eine Meldung während des Wartens den Zeitpunkt nach hinten
     * schiebt: Dann wird die verbleibende Zeit neu gerechnet und weitergewartet. Nach dem
     * Sichern geht es nur dann noch einmal von vorn los, wenn währenddessen etwas Neues
     * gemeldet wurde — sonst endet der Auftrag. Ein Fehlschlag allein startet keinen neuen
     * Versuch; das bliebe sonst bei fehlendem WLAN endlos im Kreis.
     */
    private suspend fun warteUndSichere(grund: String) {
        while (true) {
            val rest = RUHE_MS - (System.currentTimeMillis() - letzteMeldung)
            if (rest > 0) {
                delay(rest)
                continue
            }
            val standVorher = letzteMeldung
            sichere(grund)
            if (letzteMeldung == standVorher) return
        }
    }

    /** Die App geht in den Hintergrund — was aussteht, wird jetzt geschrieben. */
    override fun onStop(owner: LifecycleOwner) {
        if (!offen || !istAn() || dienst.sicherungsOrdner == null) return
        wartend?.cancel()
        wartend = bereich.launch { sichere("App im Hintergrund") }
    }

    private suspend fun sichere(grund: String) {
        // Nur eine zur Zeit: Zwei gleichzeitige Läufe legen zwei Dateien an und räumen sich
        // gegenseitig die jeweils andere weg.
        schloss.withLock {
            // Noch einmal fragen: Zwischen dem Anstoss und diesem Punkt liegen zwei Minuten.
            // Wer in der Zeit den Schalter umgelegt oder den Ordner vergessen hat, will keine
            // Sicherung mehr — der wartende Auftrag darf sich darüber nicht hinwegsetzen.
            if (!offen || !istAn() || dienst.sicherungsOrdner == null) return
            offen = false
            runCatching { dienst.sichere() }
                .onSuccess {
                    KompassLog.info("AutoSicherung", "sichere", "Selbsttätig gesichert", mapOf("grund" to grund))
                }
                .onFailure { fehler ->
                    // Wieder offen: Der nächste Anlass soll es erneut versuchen.
                    offen = true
                    KompassLog.warn(
                        "AutoSicherung",
                        "sichere",
                        "Selbsttätige Sicherung fehlgeschlagen",
                        mapOf("grund" to grund, "fehler" to fehler.message),
                    )
                }
        }
    }

    fun beende() {
        wartend?.cancel()
    }

    companion object {
        /**
         * So lange muss Ruhe sein, bevor gesichert wird.
         *
         * Zwei Minuten: lang genug, dass eine zusammenhängende Arbeit — Frage stellen, Antwort
         * lesen, nachhaken — eine einzige Sicherung ergibt; kurz genug, dass man beim Weglegen
         * des Geräts nicht ungesichert dasteht.
         */
        const val RUHE_MS = 120_000L
    }
}
