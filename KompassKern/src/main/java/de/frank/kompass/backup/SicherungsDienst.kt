package de.frank.kompass.backup

import android.content.Context
import android.net.Uri
import de.frank.kompass.data.EinspielBericht
import de.frank.kompass.data.KompassRepository
import de.frank.kompass.data.Sicherung
import de.frank.kompass.data.SicherungsVorschau
import de.frank.kompass.data.model.SicherungsTeil
import de.frank.kompass.observability.KompassLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Die Sicherung als Datei in einem selbst gewählten Ordner — in der Praxis ein Google-Drive-Ordner,
 * weil die Drive-App sich Android als Speicherort anbietet.
 *
 * Der frühere Weg — bei jedem Export einen Dateinamen bestätigen, beim Import die Datei suchen —
 * verlangte jedes Mal dieselbe Handarbeit und hinterliess einen Ordner voller Dateien. Hier wird
 * der Ordner einmal gewählt und dauerhaft gemerkt; danach schreibt „Jetzt sichern" ohne Rückfrage
 * dorthin, und es liegen immer nur die aktuelle Sicherung und die eine davor.
 *
 * Was in der Datei steht, bestimmt weiterhin [Sicherung]: die selbst gestellten Fragen samt
 * Antworten, die vertieften Erklärungen und die Gespräche. Schlüssel gehören ausdrücklich NICHT
 * hinein — eine Sicherungsdatei landet schnell in einer Cloud.
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

    /** Der Abzug als JSON — enthält, was im Umfang angehakt ist. */
    suspend fun alsJson(): String = withContext(Dispatchers.IO) {
        val eintraege = repository.ladeKomplett()
        // Die Flüsse werden einmalig ausgelesen; für eine Momentaufnahme genügt das.
        val fragen = repository.beobachteAlleFragen().first()
        val sitzungen = repository.beobachteSitzungen().first()
        val nachrichten = sitzungen.flatMap { repository.ladeNachrichten(it.id) }
        Sicherung.schreibe(
            eintraege,
            fragen,
            sitzungen,
            nachrichten,
            ZEIT.format(Date()),
            umfangGeber(),
        )
    }

    /**
     * Schreibt den ganzen Bestand als neue Datei in den gemerkten Ordner und räumt dabei auf:
     * Es bleiben nur die aktuelle Sicherung und die eine davor.
     */
    suspend fun sichere(): String {
        val geschrieben = datei.schreibe(alsJson())
        KompassLog.info("SicherungsDienst", "sichere", "Sicherung geschrieben", mapOf("name" to geschrieben.name))
        return BackupStatus.describe(context)
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

    /** Was in der Sicherung steckt — für die Rückfrage VOR dem Einspielen. */
    suspend fun vorschauVon(quelle: Uri): SicherungsVorschau = withContext(Dispatchers.IO) {
        Sicherung.lies(datei.lies(quelle)).first
    }

    /** Spielt ein, was in der App fehlt. Vorhandenes bleibt in jedem Fall erhalten. */
    suspend fun stelleWiederHerAus(quelle: Uri): EinspielBericht = withContext(Dispatchers.IO) {
        val (_, json) = Sicherung.lies(datei.lies(quelle))
        repository.spieleSicherungEin(json)
    }

    private companion object {
        private val ZEIT = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)
    }
}
