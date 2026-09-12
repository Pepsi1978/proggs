package de.frank.kompass.backup

import android.content.Context
import android.net.Uri
import de.frank.kompass.data.EinspielBericht
import de.frank.kompass.data.Einspielspur
import de.frank.kompass.data.KompassRepository
import de.frank.kompass.data.Sicherung
import de.frank.kompass.data.SicherungsAnzahl
import de.frank.kompass.data.SicherungsVorschau
import de.frank.kompass.data.local.KompassDatabase
import de.frank.kompass.data.model.SicherungsTeil
import de.frank.kompass.observability.KompassLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Was beim Einspielen herauskam — samt der Spur, mit der es sich zurücknehmen lässt. */
data class EinspielErgebnis(val bericht: EinspielBericht, val spur: Einspielspur)

/**
 * Die Sicherung als Datei in einem selbst gewählten Ordner — in der Praxis ein Google-Drive-Ordner,
 * weil die Drive-App sich Android als Speicherort anbietet.
 *
 * Der frühere Weg — bei jedem Export einen Dateinamen bestätigen, beim Import die Datei suchen —
 * verlangte jedes Mal dieselbe Handarbeit und hinterliess einen Ordner voller Dateien. Hier wird
 * der Ordner einmal gewählt und dauerhaft gemerkt; danach schreibt „Jetzt sichern" ohne Rückfrage
 * dorthin, und es liegen immer nur die aktuelle Sicherung und die eine davor.
 *
 * Geschrieben und gelesen wird satzweise: Es steht nie der ganze Bestand gleichzeitig im
 * Speicher. Nach dem Schreiben wird die Datei sofort einmal zurückgelesen und ihre Prüfsumme
 * nachgerechnet — eine Sicherung, die man erst im Ernstfall prüft, ist keine.
 */
class SicherungsDienst(
    private val context: Context,
    private val repository: KompassRepository,
    private val umfangGeber: () -> Set<SicherungsTeil> = { SicherungsTeil.ALLE },
) {
    private val datei = DateiSicherung(context)

    /**
     * Nur ein Zugriff auf den Ordner zur Zeit.
     *
     * Der Knopf „Jetzt sichern“ und die selbsttätige Sicherung können sich überschneiden —
     * etwa wenn während eines Aktualisieren-Laufs von Hand gesichert wird. Beide legen dann
     * eine Datei an und räumen danach auf; die eine könnte die frische Datei der anderen als
     * „die überzählige“ wegräumen. Auch das Einspielen gehört hierhin: Es darf nicht aus
     * einer Datei lesen, die gerade weggeräumt wird.
     */
    private val ordnerSchloss = Mutex()

    /** Der gemerkte Sicherungsordner — null, solange keiner gewählt wurde. */
    val sicherungsOrdner: Uri? get() = datei.ordner

    fun ordnerName(): String? = datei.ordnerName()

    fun merkeOrdner(uri: Uri) = datei.merkeOrdner(uri)

    fun vergissOrdner() = datei.vergissOrdner()

    fun standText(): String = BackupStatus.describe(context)

    fun istGeprueft(): Boolean = BackupStatus.istGeprueft(context)

    /**
     * Wie viel die nächste Sicherung umfassen würde und wie gross sie etwa wird.
     *
     * Die Schätzung nimmt einen Erfahrungswert je Satz. Sie soll die Grössenordnung zeigen —
     * „ein paar Kilobyte" gegen „über ein Megabyte" —, nicht auf das Byte genau sein.
     */
    suspend fun voraussichtlich(): Pair<SicherungsAnzahl, Long> = withContext(Dispatchers.IO) {
        val umfang = umfangGeber()
        val anzahl = repository.sicherungsUmfangZaehlen(
            bereiche = umfang.mapNotNull { it.bereich?.id },
            mitFragen = SicherungsTeil.FRAGEN in umfang,
            mitGespraechen = SicherungsTeil.GESPRAECHE in umfang,
        )
        val bytes = anzahl.eintraege * 600L + anzahl.fragen * 900L + anzahl.nachrichten * 700L
        anzahl to bytes
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
        var anzahl = SicherungsAnzahl()
        val (geschrieben, vorherige) = datei.schreibe { ausgabe ->
            anzahl = Sicherung.schreibe(
                ziel = ausgabe,
                quelle = repository.sicherungsQuelle,
                erstelltAm = zeit().format(Date()),
                umfang = umfang,
                roomVersion = KompassDatabase.VERSION,
            )
        }

        // Sofort zurücklesen: Erst wenn die Datei einmal fehlerfrei gelesen und ihre Prüfsumme
        // nachgerechnet wurde, gilt sie als Sicherung. Ein Schreibfehler, der erst im Ernstfall
        // auffällt, ist schlimmer als gar keine Sicherung — dann weiss man wenigstens Bescheid.
        val geprueft = runCatching { datei.lies(geschrieben.uri) { quelle -> Sicherung.pruefe(quelle) } }
        if (geprueft.isFailure) {
            val fehler = geprueft.exceptionOrNull()
            KompassLog.error(
                "SicherungsDienst",
                "sichere",
                "Geschriebene Sicherung liess sich nicht lesen",
                mapOf("name" to geschrieben.name, "grund" to fehler?.message),
            )
            // Weder aufräumen noch stempeln: Die alten Stände bleiben stehen, und der
            // Zeitpunkt der letzten geglückten Sicherung wird nicht überschrieben. Sonst
            // stünde da eine frische Uhrzeit für eine Datei, die niemand lesen kann.
            BackupStatus.markGescheitert(context)
            throw fehler ?: IllegalStateException("Die geschriebene Sicherung liess sich nicht prüfen.")
        }


        BackupStatus.markBackedUp(context, geprueft = true)
        // Erst jetzt: Eine gute Sicherung gegen eine ungeprüfte einzutauschen wäre der
        // Fehler, gegen den das Zurücklesen überhaupt schützt.
        datei.raeumeAlteWeg(vorherige)

        KompassLog.info(
            "SicherungsDienst",
            "sichere",
            "Sicherung geschrieben und geprüft",
            mapOf("name" to geschrieben.name, "eintraege" to anzahl.eintraege),
        )
        return BackupStatus.describe(context)
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
        ordnerSchloss.withLock { datei.lies(quelle) { Sicherung.pruefe(it) } }
    }

    /** Spielt ein, was in der App fehlt. Vorhandenes bleibt in jedem Fall erhalten. */
    suspend fun stelleWiederHerAus(quelle: Uri): EinspielErgebnis = withContext(Dispatchers.IO) {
        ordnerSchloss.withLock {
        // Erst ein Durchlauf ohne Senke. Prüfsumme und Zählwerk stehen am ENDE der Datei —
        // prüfte man erst beim Einspielen, wäre bei einer beschädigten Datei längst alles in
        // der Datenbank, bevor der Fehler auffällt. Der zweite Durchlauf kostet wenig; eine
        // halb eingespielte Sicherung käme teuer.
        datei.lies(quelle) { Sicherung.pruefe(it) }
        val senke = repository.EinspielSenke()
        datei.lies(quelle) { Sicherung.spieleEin(it, senke) }
        repository.schliesseEinspielenAb(senke.spur)
        EinspielErgebnis(senke.bericht(), senke.spur)
        }
    }

    /** Nimmt ein Einspielen zurück — entfernt genau das, was dabei entstanden ist. */
    suspend fun nimmZurueck(spur: Einspielspur): Int = withContext(Dispatchers.IO) {
        repository.nimmEinspielenZurueck(spur)
    }

    private companion object {
        /** Je Aufruf neu: SimpleDateFormat ist nicht threadsicher. */
        private fun zeit() = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)
    }
}
