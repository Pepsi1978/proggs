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

    /** Meldet, dass sich etwas geändert hat, das gesichert gehört. */
    fun melde(grund: String) {
        if (!istAn() || dienst.sicherungsOrdner == null) return
        offen = true
        wartend?.cancel()
        wartend = bereich.launch {
            delay(RUHE_MS)
            sichere(grund)
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
            if (!offen) return
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
