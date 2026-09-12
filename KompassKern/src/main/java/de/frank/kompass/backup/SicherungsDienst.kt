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
        val umfang = umfangGeber()
        var anzahl = SicherungsAnzahl()
        val geschrieben = datei.schreibe { ausgabe ->
            anzahl = Sicherung.schreibe(
                ziel = ausgabe,
                quelle = repository.sicherungsQuelle,
                erstelltAm = ZEIT.format(Date()),
                umfang = umfang,
                roomVersion = KompassDatabase.VERSION,
            )
        }

        // Sofort zurücklesen: Erst wenn die Datei einmal fehlerfrei gelesen und ihre Prüfsumme
        // nachgerechnet wurde, gilt sie als Sicherung. Ein Schreibfehler, der erst im Ernstfall
        // auffällt, ist schlimmer als gar keine Sicherung — dann weiss man wenigstens Bescheid.
        val geprueft = runCatching { datei.lies(geschrieben.uri) { quelle -> Sicherung.pruefe(quelle) } }
        geprueft.onFailure { fehler ->
            KompassLog.error(
                "SicherungsDienst",
                "sichere",
                "Geschriebene Sicherung liess sich nicht lesen",
                mapOf("name" to geschrieben.name, "grund" to fehler.message),
            )
        }
        BackupStatus.markBackedUp(context, geprueft.isSuccess)

        KompassLog.info(
            "SicherungsDienst",
            "sichere",
            "Sicherung geschrieben",
            mapOf("name" to geschrieben.name, "eintraege" to anzahl.eintraege, "geprueft" to geprueft.isSuccess),
        )
        if (geprueft.isFailure) {
            throw geprueft.exceptionOrNull()
                ?: IllegalStateException("Die geschriebene Sicherung liess sich nicht prüfen.")
        }
        BackupStatus.describe(context)
    }

    /**
     * Alle Sicherungen im gemerkten Ordner, die jüngste zuerst.
     *
     * Damit zeigt die App die Auswahl selbst an, statt den Dateiwähler von Android zu öffnen —
     * aus dem führt die Zurück-Geste Ordner für Ordner heraus statt zurück in die App.
     */
    suspend fun sicherungen(): List<Sicherungsdatei> = datei.sicherungen()

    /** Die jüngste Sicherung im Ordner — sie wird beim Wiederherstellen genommen. */
    suspend fun neuesteSicherung(): Sicherungsdatei? = datei.sicherungen().firstOrNull()

    /** Wie viele Sicherungen gerade im Ordner liegen. */
    suspend fun anzahlSicherungen(): Int = datei.sicherungen().size

    /**
     * Was in der Sicherung steckt — für die Rückfrage VOR dem Einspielen.
     *
     * Die Datei wird dabei vollständig durchlaufen und ihre Prüfsumme nachgerechnet. Eine
     * beschädigte Datei fällt hier auf, bevor irgendetwas in die Datenbank gelangt.
     */
    suspend fun vorschauVon(quelle: Uri): SicherungsVorschau = withContext(Dispatchers.IO) {
        datei.lies(quelle) { Sicherung.pruefe(it) }
    }

    /** Spielt ein, was in der App fehlt. Vorhandenes bleibt in jedem Fall erhalten. */
    suspend fun stelleWiederHerAus(quelle: Uri): EinspielErgebnis = withContext(Dispatchers.IO) {
        val senke = repository.EinspielSenke()
        datei.lies(quelle) { Sicherung.spieleEin(it, senke) }
        repository.schliesseEinspielenAb()
        EinspielErgebnis(senke.bericht(), senke.spur)
    }

    /** Nimmt ein Einspielen zurück — entfernt genau das, was dabei entstanden ist. */
    suspend fun nimmZurueck(spur: Einspielspur): Int = withContext(Dispatchers.IO) {
        repository.nimmEinspielenZurueck(spur)
    }

    private companion object {
        private val ZEIT = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)
    }
}
