// ──────────────────────────────────────────────────────────────────────
// Modul M1.1 — Sicherung · Stand v6
// Quelle: Module/Android/M1.1-Sicherung/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.1 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sicherung

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
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
 * Drei Dinge verhindern, dass daraus ein Dauerlauf wird:
 *
 *  - **Ruhezeit.** Nach einer Änderung wird [RUHE_MS] gewartet. Jede weitere Änderung setzt die
 *    Wartezeit zurück. Wer zehn Fragen hintereinander stellt, löst damit eine Sicherung aus,
 *    nicht zehn. Ohne das schriebe jede getippte Antwort eine ganze Datei in die Cloud.
 *  - **Späteste Frist.** Wer eine halbe Stunde am Stück tippt, setzt die Ruhezeit eine halbe
 *    Stunde lang immer wieder zurück — ohne [SPAETESTENS_MS] käme in dieser ganzen Zeit keine
 *    einzige Sicherung zustande. Spätestens so lange nach der ERSTEN offenen Änderung wird
 *    darum gesichert, egal wie lebhaft es weitergeht.
 *  - **Beim Verlassen sofort.** Geht die App in den Hintergrund, während noch etwas aussteht,
 *    wird ohne weiteres Warten gesichert. Sonst ginge die Änderung verloren, sobald Android den
 *    Vorgang beendet — und das tut es gern genau dann.
 *
 * Und weil auch das Sofort-Sichern beim Verlassen scheitern kann — Android friert den Vorgang
 * ein oder beendet ihn, bevor die Datei beim Speicheranbieter angekommen ist —, **merkt sich das
 * Modul über den Vorgangstod hinweg, dass noch etwas aussteht**: Der offene Stand liegt in der
 * Ablage, nicht nur im Arbeitsspeicher. Beim nächsten Start wird nachgeholt. Vorher war eine
 * Änderung, deren Sicherung der Vorgangstod erwischt hat, endgültig ungesichert — bis irgendwann
 * zufällig die nächste Änderung kam.
 *
 * Ein Fehlschlag bleibt ohne Aufhebens: Die Meldung steht im Protokoll, der Zeitstempel der
 * letzten geglückten Sicherung bleibt stehen. Ein rot blinkender Hinweis mitten im Tippen wäre
 * die falsche Antwort auf ein fehlendes WLAN.
 */
class AutoSicherung(
    private val dienst: SicherungsDienst,
    private val istAn: () -> Boolean,
    private val protokoll: SicherungsProtokoll = StillesProtokoll,
) : DefaultLifecycleObserver {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val schloss = Mutex()

    /** Der wartende Auftrag. Wird aus der Oberfläche UND aus dem Datenbank-Faden gelesen. */
    @Volatile
    private var wartend: Job? = null

    /** Der Auftrag aus dem Lebenslauf — Nachholen beim Start, Sofort-Sicherung beim Verlassen. */
    @Volatile
    private var ausDemLebenslauf: Job? = null

    /**
     * Steht noch etwas aus?
     *
     * Im Arbeitsspeicher **und** in der Ablage. Der Wert im Speicher beantwortet die Frage
     * schnell — er wird bei jeder Datenbankänderung gelesen. Der Wert in der Ablage überlebt den
     * Vorgangstod und ist das Einzige, woran der nächste Start erkennt, dass eine Änderung
     * ungesichert liegen geblieben ist.
     */
    @Volatile
    private var offen = dienst.istOffen()

    init {
        // Auch eine von Hand angestossene Sicherung erledigt, was aussteht. Ohne das schriebe
        // die selbsttätige Sicherung zwei Minuten nach jedem Druck auf „Jetzt sichern" dieselbe
        // Datei ein zweites Mal — und der nächste Start holte etwas nach, das längst dasteht.
        dienst.beiGeglueckterSicherung(::quittiere)
    }

    /**
     * Wann zuletzt etwas gemeldet wurde — daran hängt die Ruhezeit.
     *
     * Gemessen mit [SystemClock.elapsedRealtime], **nicht** mit der Wanduhr. Die Wanduhr springt:
     * Zeitumstellung, ein Abgleich mit der Funkzeit, ein von Hand verstelltes Datum. Springt sie
     * rückwärts, rechnet sich die verbleibende Ruhezeit auf den Sprung auf, und es wird eine
     * halbe Stunde lang nicht gesichert; springt sie vorwärts, wird mitten im Tippen gesichert.
     * Hier werden ausschliesslich Zeitspannen gebraucht, und dafür ist die Wanduhr das falsche
     * Werkzeug. `0L` steht für „noch nichts gemeldet" — die Laufzeituhr steht nach dem Hochfahren
     * nie wieder auf null, also ist das ein gefahrloses Kennzeichen.
     */
    @Volatile
    private var letzteMeldung = 0L

    /** Wann die ÄLTESTE noch offene Änderung kam — daran hängt die späteste Frist. Laufzeituhr. */
    @Volatile
    private var ersteMeldung = 0L

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
        val jetzt = SystemClock.elapsedRealtime()
        if (!offen || ersteMeldung == 0L) ersteMeldung = jetzt
        setzeOffen(true)
        letzteMeldung = jetzt
        if (wartend?.isActive == true) return
        wartend = bereich.launch { warteUndSichere(grund) }
    }

    /**
     * Wartet, bis seit der letzten Meldung [RUHE_MS] Ruhe war — längstens aber bis
     * [SPAETESTENS_MS] nach der ersten offenen Änderung — und sichert dann.
     *
     * Die Schleife ist nötig, weil eine Meldung während des Wartens den Zeitpunkt nach hinten
     * schiebt: Dann wird die verbleibende Zeit neu gerechnet und weitergewartet. Nach dem
     * Sichern geht es nur dann noch einmal von vorn los, wenn währenddessen etwas Neues
     * gemeldet wurde — sonst endet der Auftrag. Ein Fehlschlag allein startet keinen neuen
     * Versuch; das bliebe sonst bei fehlendem WLAN endlos im Kreis.
     */
    private suspend fun warteUndSichere(grund: String) {
        while (true) {
            val jetzt = SystemClock.elapsedRealtime()
            val ruhe = RUHE_MS - (jetzt - letzteMeldung)
            val seitErster = ersteMeldung.takeIf { it > 0L } ?: jetzt
            val frist = SPAETESTENS_MS - (jetzt - seitErster)
            val rest = minOf(ruhe, frist)
            if (rest > 0) {
                delay(rest)
                continue
            }
            val standVorher = letzteMeldung
            sichere(if (ruhe > 0) "$grund (späteste Frist)" else grund)
            if (letzteMeldung == standVorher) return
        }
    }

    /**
     * Die App kommt nach vorn — was vom letzten Mal liegen geblieben ist, wird jetzt geholt.
     *
     * Das ist der Gegenpart zu [onStop]: Wurde der Vorgang beendet, bevor die Sofort-Sicherung
     * beim Verlassen durch war, steht der offene Stand noch in der Ablage. Ohne diesen Punkt
     * bliebe die Änderung ungesichert, bis zufällig die nächste kommt.
     */
    override fun onStart(owner: LifecycleOwner) {
        // Geprüft wird hier nur der Merker im Speicher. Ob die Sicherung überhaupt an ist und ob
        // ein Ordner dasteht, fragt [sichere] gleich noch einmal — im Hintergrundfaden, wo die
        // Antwort herkommt: aus einer Datei und aus einer entschlüsselten Ablage. Beides gehört
        // nicht auf den Hauptfaden, schon gar nicht im Start- und Verlassen-Pfad.
        if (!offen) return
        ausDemLebenslauf = bereich.launch { sichere("Nachgeholt nach Neustart") }
    }

    /** Die App geht in den Hintergrund — was aussteht, wird jetzt geschrieben. */
    override fun onStop(owner: LifecycleOwner) {
        if (!offen) return
        // Der wartende Auftrag wird NICHT abgebrochen. Er könnte gerade mitten im Schreiben
        // stecken; ein Abbruch löschte die halb geschriebene Datei und finge von vorn an —
        // ausgerechnet in dem Augenblick, in dem Android den Vorgang gleich einfriert. Das
        // Schloss und der offene Stand sorgen ohnehin dafür, dass nur einer schreibt: Wacht
        // der Wartende später auf, findet er nichts Offenes mehr vor und endet still.
        ausDemLebenslauf = bereich.launch { sichere("App im Hintergrund") }
    }

    private suspend fun sichere(grund: String) {
        // Nur eine zur Zeit: Zwei gleichzeitige Läufe legen zwei Dateien an und räumen sich
        // gegenseitig die jeweils andere weg.
        schloss.withLock {
            // Noch einmal fragen: Zwischen dem Anstoss und diesem Punkt liegen zwei Minuten.
            // Wer in der Zeit den Schalter umgelegt oder den Ordner vergessen hat, will keine
            // Sicherung mehr — der wartende Auftrag darf sich darüber nicht hinwegsetzen.
            if (!offen || !istAn() || dienst.sicherungsOrdner == null) return

            // Der Merker wird ERST GELÖSCHT, WENN GESCHRIEBEN UND GEPRÜFT IST — nie vorher.
            // Vorher gelöscht hiesse: Genau während des Schreibens, also in der einen Phase, um
            // derentwillen es diesen Merker überhaupt gibt, stünde in der Ablage „es steht
            // nichts aus". Stirbt der Vorgang dort — und dort stirbt er —, holt der nächste
            // Start nichts nach, und die Änderung ist endgültig weg. Scheitert es, bleibt der
            // Merker unangetastet stehen; es ist nichts zurückzunehmen.
            // Gelöscht wird der Merker nicht hier, sondern in [quittiere] — das ruft der Dienst
            // am Ende jedes geglückten Laufs, gleich wer ihn angestossen hat.
            runCatching { dienst.sichere() }
                .onSuccess {
                    protokoll.info("AutoSicherung", "sichere", "Selbsttätig gesichert", mapOf("grund" to grund))
                }
                .onFailure { fehler ->
                    // Ein Abbruch von aussen ist kein Fehlschlag und gehört weitergereicht.
                    // `runCatching` fängt auch ihn, und die Coroutine liefe sonst weiter, als
                    // wäre nichts gewesen.
                    if (fehler is CancellationException) throw fehler
                    // Kein Zurücksetzen nötig: Der Merker stand die ganze Zeit auf „offen".
                    // Der nächste Anlass versucht es erneut — spätestens der nächste Start.
                    protokoll.warn(
                        "AutoSicherung",
                        "sichere",
                        "Selbsttätige Sicherung fehlgeschlagen",
                        mapOf("grund" to grund, "fehler" to fehler.message),
                    )
                }
        }
    }

    /**
     * Eine Sicherung ist geglückt — was bis zu ihrem Beginn gemeldet war, steht jetzt in ihr.
     *
     * Gerufen vom Dienst, für **jeden** geglückten Lauf: den selbsttätigen wie den von Hand
     * angestossenen. [begonnenAm] ist der Zeitpunkt, zu dem der Lauf anfing, aus der Datenbank
     * zu lesen. Alles, was danach gemeldet wurde, steckt nicht mehr sicher in der Datei und
     * bleibt offen — lieber einmal zu viel gesichert als eine Änderung stillschweigend als
     * erledigt abgehakt.
     */
    private fun quittiere(begonnenAm: Long) {
        if (letzteMeldung <= begonnenAm) {
            ersteMeldung = 0L
            setzeOffen(false)
        } else {
            // Während des Schreibens kam etwas dazu. Die späteste Frist zählt ab JETZT: Die
            // alte ist mit dieser Sicherung erfüllt; bliebe sie stehen, liefe sofort eine
            // zweite hinterher.
            ersteMeldung = SystemClock.elapsedRealtime()
        }
    }

    /**
     * Schreibt den offenen Stand fort — in den Speicher und, wenn er sich ändert, in die Ablage.
     *
     * Der Vergleich davor ist kein Geiz: [melde] läuft bei JEDER Datenbankänderung, und in die
     * Ablage wird mit `commit()` geschrieben. Ohne den Vergleich stünde in einer Import-Schleife
     * ein blockierender Schreibvorgang je Satz.
     */
    private fun setzeOffen(neu: Boolean) {
        if (offen == neu) return
        offen = neu
        dienst.merkeOffen(neu)
    }

    fun beende() {
        wartend?.cancel()
        ausDemLebenslauf?.cancel()
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

        /**
         * So lange darf eine offene Änderung längstens ungesichert bleiben.
         *
         * Zehn Minuten: Wer ununterbrochen arbeitet, setzt die Ruhezeit sonst beliebig lange
         * immer wieder zurück — und gerade die lange, ununterbrochene Arbeit ist die, deren
         * Verlust am meisten weh tut.
         */
        const val SPAETESTENS_MS = 600_000L
    }
}
