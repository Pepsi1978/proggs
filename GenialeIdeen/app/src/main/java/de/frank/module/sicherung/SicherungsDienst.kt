// ──────────────────────────────────────────────────────────────────────
// Modul M1.1 — Sicherung · Stand v4
// Quelle: Module/Android/M1.1-Sicherung/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.1 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sicherung

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Was beim Einspielen herauskam — samt der Spur, mit der es sich zurücknehmen lässt. */
data class EinspielErgebnis(val vorschau: SicherungsVorschau, val spur: Einspielspur?)

/**
 * Die Sicherung als Datei in einem selbst gewählten Ordner — in der Praxis ein Google-Drive-Ordner,
 * weil die Drive-App sich Android als Speicherort anbietet.
 *
 * Der frühere Weg — bei jedem Export einen Dateinamen bestätigen, beim Import die Datei suchen —
 * verlangte jedes Mal dieselbe Handarbeit und hinterließ einen Ordner voller Dateien. Hier wird
 * der Ordner einmal gewählt und dauerhaft gemerkt; danach schreibt „Jetzt sichern" ohne Rückfrage
 * dorthin, und es liegen immer nur die aktuelle Sicherung und die eine davor.
 *
 * Geschrieben und gelesen wird satzweise: Es steht nie der ganze Bestand gleichzeitig im
 * Speicher. Nach dem Schreiben wird die Datei sofort einmal zurückgelesen und ihre Prüfsumme
 * nachgerechnet — eine Sicherung, die man erst im Ernstfall prüft, ist keine.
 */
class SicherungsDienst(
    private val context: Context,
    private val inhalt: SicherungsInhalt,
    namen: SicherungsNamen,
    private val ruecknahme: SicherungsRuecknahme? = null,
    private val protokoll: SicherungsProtokoll = StillesProtokoll,
    private val umfangGeber: () -> Set<SicherungsTeil> = { inhalt.teile.toSet() },
) {
    private val datei = DateiSicherung(context, namen, protokoll)

    /** Der Stand liegt in der Ablage DIESER App — siehe [SicherungsNamen.einstellungenDatei]. */
    private val stand = BackupStatus(namen.einstellungenDatei)
    private val rahmen = Sicherungsrahmen(inhalt, protokoll)

    /**
     * Nur ein Zugriff auf den Ordner zur Zeit.
     *
     * Der Knopf „Jetzt sichern" und die selbsttätige Sicherung können sich überschneiden —
     * etwa wenn während eines Aktualisieren-Laufs von Hand gesichert wird. Beide legen dann
     * eine Datei an und räumen danach auf; die eine könnte die frische Datei der anderen als
     * „die überzählige" wegräumen. Auch das Einspielen gehört hierhin: Es darf nicht aus
     * einer Datei lesen, die gerade weggeräumt wird.
     */
    private val ordnerSchloss = Mutex()

    /** Der gemerkte Sicherungsordner — null, solange keiner gewählt wurde. */
    val sicherungsOrdner: Uri? get() = datei.ordner

    fun ordnerName(): String? = datei.ordnerName()

    fun merkeOrdner(uri: Uri) = datei.merkeOrdner(uri)

    fun vergissOrdner() = datei.vergissOrdner()

    fun standText(): String = stand.describe(context)

    fun istGeprueft(): Boolean = stand.istGeprueft(context)

    private val _standFluss = MutableStateFlow(stand.describe(context))

    /**
     * Der angezeigte Stand, laufend nachgeführt.
     *
     * Vorher las die Oberfläche den Stand EINMAL beim Aufbau und danach nur noch nach einem
     * Druck auf „Jetzt sichern". Eine selbsttätige Sicherung änderte die Anzeige nicht — dort
     * stand weiter die Uhrzeit von vorhin, obwohl längst neu gesichert war. Wer daraufhin
     * schliesst, die selbsttätige Sicherung sei tot, hat recht gehandelt und unrecht gehabt:
     * Eine Anzeige, die eine tote Sicherung vortäuscht, ist so schädlich wie eine, die eine
     * lebende vortäuscht.
     */
    val standFluss: StateFlow<String> = _standFluss.asStateFlow()

    private val _geprueftFluss = MutableStateFlow(stand.istGeprueft(context))

    /** Ob die zuletzt geschriebene Sicherung auch fehlerfrei zurückgelesen wurde. */
    val geprueftFluss: StateFlow<Boolean> = _geprueftFluss.asStateFlow()

    /** Nach jedem Lauf — geglückt oder nicht — die Anzeige nachziehen. */
    private fun meldeStand() {
        _standFluss.value = stand.describe(context)
        _geprueftFluss.value = stand.istGeprueft(context)
    }

    /** Merkt über den Vorgangstod hinweg, dass noch eine Änderung ungesichert aussteht. */
    fun merkeOffen(offen: Boolean) = stand.merkeOffen(context, offen)

    /** Ob beim letzten Mal eine Änderung ungesichert liegen geblieben ist. */
    fun istOffen(): Boolean = stand.istOffen(context)

    /**
     * Wie viel die nächste Sicherung umfassen würde und wie groß sie etwa wird.
     *
     * Die Schätzung nimmt einen Erfahrungswert je Satz. Sie soll die Größenordnung zeigen —
     * „ein paar Kilobyte" gegen „über ein Megabyte" —, nicht auf das Byte genau sein.
     */
    suspend fun voraussichtlich(): Pair<Nutzlastzahlen, Long> = withContext(Dispatchers.IO) {
        val zahlen = inhalt.zaehle(umfangGeber())
        zahlen to inhalt.schaetzeGroesse(zahlen)
    }

    /**
     * Schreibt den gewählten Umfang als neue Datei in den gemerkten Ordner, prüft sie und räumt
     * auf: Es bleiben nur die aktuelle Sicherung und die eine davor.
     */
    suspend fun sichere(): String = withContext(Dispatchers.IO) {
        ordnerSchloss.withLock { sichereGeschuetzt() }
    }

    private suspend fun sichereGeschuetzt(): String {
        val umfang = umfangGeber()
        var zahlen = Nutzlastzahlen()
        val (geschrieben, vorherige) = datei.schreibe { ausgabe ->
            zahlen = rahmen.schreibe(
                ziel = ausgabe,
                erstelltAm = zeit().format(Date()),
                umfang = umfang,
            )
        }

        // Sofort zurücklesen: Erst wenn die Datei einmal fehlerfrei gelesen und ihre Prüfsumme
        // nachgerechnet wurde, gilt sie als Sicherung. Ein Schreibfehler, der erst im Ernstfall
        // auffällt, ist schlimmer als gar keine Sicherung — dann weiß man wenigstens Bescheid.
        val geprueft = runCatching { datei.lies(geschrieben.uri) { quelle -> rahmen.pruefe(quelle) } }
        if (geprueft.isFailure) {
            val fehler = geprueft.exceptionOrNull()
            protokoll.warn(
                "SicherungsDienst",
                "sichere",
                "Geschriebene Sicherung ließ sich nicht lesen",
                mapOf("name" to geschrieben.name, "grund" to fehler?.message),
            )
            // Weder aufräumen noch stempeln: Die alten Stände bleiben stehen, und der
            // Zeitpunkt der letzten geglückten Sicherung wird nicht überschrieben. Sonst
            // stünde da eine frische Uhrzeit für eine Datei, die niemand lesen kann.
            stand.markGescheitert(context)
            meldeStand()
            throw fehler ?: IllegalStateException("Die geschriebene Sicherung ließ sich nicht prüfen.")
        }

        stand.markBackedUp(context, geprueft = true)
        meldeStand()
        // Erst jetzt: Eine gute Sicherung gegen eine ungeprüfte einzutauschen wäre der
        // Fehler, gegen den das Zurücklesen überhaupt schützt.
        datei.raeumeAlteWeg(vorherige)

        protokoll.info(
            "SicherungsDienst",
            "sichere",
            "Sicherung geschrieben und geprüft",
            buildMap {
                put("name", geschrieben.name)
                zahlen.anzahl.forEach { (art, wieViele) -> put(art, wieViele) }
            },
        )
        return stand.describe(context)
    }

    /**
     * Alle Sicherungen im gemerkten Ordner, die jüngste zuerst.
     *
     * Damit zeigt die App die Auswahl selbst an, statt den Dateiwähler von Android zu öffnen —
     * aus dem führt die Zurück-Geste Ordner für Ordner heraus statt zurück in die App.
     */
    suspend fun sicherungen(): List<Sicherungsdatei> = ordnerSchloss.withLock { datei.sicherungen() }

    /** Die jüngste Sicherung im Ordner — sie wird beim Wiederherstellen genommen. */
    suspend fun neuesteSicherung(): Sicherungsdatei? =
        ordnerSchloss.withLock { datei.sicherungen().firstOrNull() }

    /** Wie viele Sicherungen gerade im Ordner liegen. */
    suspend fun anzahlSicherungen(): Int = ordnerSchloss.withLock { datei.sicherungen().size }

    /**
     * Was in der Sicherung steckt — für die Rückfrage VOR dem Einspielen.
     *
     * Die Datei wird dabei vollständig durchlaufen und ihre Prüfsumme nachgerechnet. Eine
     * beschädigte Datei fällt hier auf, bevor irgendetwas in die Datenbank gelangt.
     */
    suspend fun vorschauVon(quelle: Uri): SicherungsVorschau = withContext(Dispatchers.IO) {
        // Auch das Lesen gehört unter den Riegel: Die Datei, aus der hier gelesen wird, könnte
        // sonst gerade von der selbsttätigen Sicherung weggeräumt werden.
        ordnerSchloss.withLock { datei.lies(quelle) { rahmen.pruefe(it) } }
    }

    /** Spielt ein, was in der App fehlt. Vorhandenes bleibt in jedem Fall erhalten. */
    suspend fun stelleWiederHerAus(quelle: Uri): EinspielErgebnis = withContext(Dispatchers.IO) {
        ordnerSchloss.withLock {
            // Erst ein Durchlauf ohne Einspielen. Prüfsumme und Zählwerk stehen am ENDE der
            // Datei — prüfte man erst beim Einspielen, wäre bei einer beschädigten Datei längst
            // alles in der Datenbank, bevor der Fehler auffällt. Der zweite Durchlauf kostet
            // wenig; eine halb eingespielte Sicherung käme teuer.
            datei.lies(quelle) { rahmen.pruefe(it) }
            val spur = ruecknahme?.beginne()
            val vorschau = datei.lies(quelle) { rahmen.spieleEin(it) }
            spur?.let { ruecknahme?.schliesseAb(it) }
            EinspielErgebnis(vorschau, spur)
        }
    }

    /** Nimmt ein Einspielen zurück — entfernt genau das, was dabei entstanden ist. */
    suspend fun nimmZurueck(spur: Einspielspur): Int = withContext(Dispatchers.IO) {
        ruecknahme?.nimmZurueck(spur) ?: 0
    }

    private companion object {
        /** Je Aufruf neu: SimpleDateFormat ist nicht threadsicher. */
        private fun zeit() = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)
    }
}
