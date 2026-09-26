package de.frank.newskompass.news

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.data.ArchivImport
import de.frank.newskompass.data.ArchivSicherung
import de.frank.newskompass.data.ImportErgebnis
import de.frank.newskompass.data.SicherungsErgebnis
import de.frank.newskompass.observability.KompassLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Archivsicherung im Hintergrund: Export in eine selbst angelegte Datei mit anschließendem
 * vollständigem Zurücklesen, oder die Prüfung einer vorhandenen Sicherung als Trockenlauf.
 *
 * Erfolg meldet der Export erst, wenn die geschriebene Datei vollständig zurückgelesen und jeder
 * Eintrag geprüft ist. Scheitert etwas oder bricht der Nutzer ab, wird nur die eben neu angelegte
 * Zieldatei entfernt, soweit der Anbieter das erlaubt. Lokale Originale bleiben unangetastet.
 */
class SicherungWorker(context: Context, parameter: WorkerParameters) : CoroutineWorker(context, parameter) {

    override suspend fun getForegroundInfo(): ForegroundInfo = vordergrund("Archivsicherung läuft …")

    private fun vordergrund(text: String): ForegroundInfo {
        val hinweis = Zeitplan.laufHinweis(applicationContext, text, titel = "News Kompass sichert das Archiv")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(Zeitplan.HINWEIS_SICHERUNG, hinweis, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(Zeitplan.HINWEIS_SICHERUNG, hinweis)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as NewsApplication
        val uri = inputData.getString(K_URI)?.let(Uri::parse) ?: return Result.failure(workDataOf(K_FEHLER to "Keine Datei gewählt."))
        val art = inputData.getString(K_ART)
        val start = when (art) {
            ART_EXPORT -> "Archiv wird gesichert …"
            ART_IMPORT -> "Archiv wird importiert …"
            else -> "Sicherung wird geprüft …"
        }
        runCatching { setForeground(vordergrund(start)) }
        val resolver = applicationContext.contentResolver
        val name = anzeigeName(uri)
        try {
            return when (art) {
                ART_EXPORT -> exportiere(app, uri, name)
                ART_IMPORT -> importiere(app, uri, name)
                else -> pruefe(uri, name)
            }
        } finally {
            // Die dauerhafte Freigabe war nur für diesen Auftrag nötig.
            runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    private suspend fun exportiere(app: NewsApplication, uri: Uri, name: String): Result {
        val resolver = applicationContext.contentResolver
        val geschrieben: SicherungsErgebnis
        try {
            geschrieben = ArchivSicherung.exportiere(app.speicher, resolver, uri) { text, anteil -> melde(text, anteil) }
            melde("Lese die Sicherung zur Prüfung zurück …", 0.65f)
            val geprueft = ArchivSicherung.pruefe(resolver, uri) { text -> melde(text, 0.8f) }
            val abweichung = when {
                geprueft.fehler != null -> geprueft.fehler
                geprueft.eintraege != geschrieben.eintraege -> "Die zurückgelesene Sicherung weicht vom Geschriebenen ab."
                else -> null
            }
            if (abweichung != null) return scheitere(uri, name, "Die Prüfung nach dem Schreiben ist gescheitert: $abweichung")
        } catch (abbruch: CancellationException) {
            withContext(NonCancellable) { entferne(uri) }
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.error("SicherungWorker", "exportiere", "Sicherung gescheitert", mapOf("grund" to fehler.javaClass.simpleName))
            return scheitere(uri, name, "Die Sicherung ist gescheitert: ${fehler.message ?: fehler.javaClass.simpleName}")
        }
        val text = beschreibe(name, geschrieben)
        // Fehlten Bilder schon lokal, ist die Datei nützlich, aber keine vollständige Sicherung.
        if (geschrieben.fehlendeBilder > 0) {
            return Result.success(
                workDataOf(
                    K_ART to ART_EXPORT,
                    K_VOLLSTAENDIG to false,
                    K_TEXT to "Sicherung geprüft, aber unvollständig: ${geschrieben.fehlendeBilder} Bilder fehlten bereits lokal. $text",
                ),
            )
        }
        app.einstellungen.merkeSicherung(geschrieben.erstelltUm, text)
        return Result.success(workDataOf(K_ART to ART_EXPORT, K_VOLLSTAENDIG to true, K_TEXT to "Sicherung vollständig und geprüft: $text"))
    }

    private suspend fun pruefe(uri: Uri, name: String): Result {
        val ergebnis = try {
            ArchivSicherung.pruefe(applicationContext.contentResolver, uri) { text -> melde(text, 0.5f) }
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            SicherungsErgebnis("Die Datei ließ sich nicht lesen (${fehler.javaClass.simpleName}).")
        }
        if (ergebnis.fehler != null) {
            return Result.failure(workDataOf(K_ART to ART_PRUEFEN, K_FEHLER to "$name ist nicht in Ordnung: ${ergebnis.fehler}"))
        }
        val erstellt = if (ergebnis.erstelltUm > 0) ", erstellt ${datum(ergebnis.erstelltUm)}" else ""
        val vollstaendig = ergebnis.fehlendeBilder == 0
        val urteil = if (vollstaendig) {
            "Sicherung lesbar und vollständig"
        } else {
            "Sicherung geprüft, aber unvollständig: ${ergebnis.fehlendeBilder} Bilder fehlten bereits lokal beim Sichern"
        }
        return Result.success(
            workDataOf(
                K_ART to ART_PRUEFEN,
                K_VOLLSTAENDIG to vollstaendig,
                K_TEXT to "$urteil (Format ${ArchivSicherung.FORMAT}$erstellt): ${beschreibe(name, ergebnis)}",
            ),
        )
    }

    /** Importiert eine Sicherung, ohne lokal etwas zu überschreiben oder zu löschen. */
    private suspend fun importiere(app: NewsApplication, uri: Uri, name: String): Result {
        val e = try {
            ArchivImport.importiere(app.speicher, applicationContext.contentResolver, uri) { text, anteil -> melde(text, anteil) }
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.error("SicherungWorker", "importiere", "Import gescheitert", mapOf("grund" to fehler.javaClass.simpleName))
            ImportErgebnis(
                fehler = "Der Import ist gescheitert (${fehler.message ?: fehler.javaClass.simpleName}). Bereits übernommene Ausgaben sind vollständig; " +
                    "ein erneuter Import setzt fort. Lokal wurde nichts überschrieben oder gelöscht.",
            )
        }
        e.fehler?.let { return Result.failure(workDataOf(K_ART to ART_IMPORT, K_FEHLER to it)) }
        val teile = mutableListOf("${e.neu} Ausgaben übernommen", "${e.doppelt} schon vorhanden")
        teile += "Bilder: ${e.bilderNeu} neu, ${e.bilderVorhanden} vorhanden" + if (e.bilderUmbenannt > 0) ", ${e.bilderUmbenannt} unter neuem Namen" else ""
        if (e.konflikte > 0) teile += "${e.konflikte} Konflikte: lokale Fassung behalten, die importierte liegt unter ${e.quarantaene}"
        if (e.konflikteLokalBeschaedigt > 0) {
            teile += "${e.konflikteLokalBeschaedigt} lokal beschädigte Ausgaben: die gute Fassung aus der Sicherung liegt unter ${e.quarantaene}"
        }
        if (e.beschaedigtGesichert > 0) teile += "${e.beschaedigtGesichert} beschädigte Rohdateien getrennt gesichert"
        if (e.beiseiteNeu + e.beiseiteVorhanden + e.beiseiteUmgelegt > 0) {
            teile += "Beiseitegelegtes aus der Sicherung: ${e.beiseiteNeu} neu getrennt abgelegt, ${e.beiseiteVorhanden} schon vorhanden" +
                if (e.beiseiteUmgelegt > 0) ", ${e.beiseiteUmgelegt} wegen gleichen Namens in eigenem Ordner" else ""
        }
        // Zwei getrennte Sachverhalte: Was im Archiv jetzt fehlt, und ob die gewählte Datei selbst vollständig ist.
        val kopf = if (e.fehlendeBilder == 0) {
            "Import fertig aus $name"
        } else {
            "Import unvollständig aus $name: ${e.fehlendeBilder} Bilder übernommener Ausgaben fehlen in der Sicherung und lokal"
        }
        val hinweis = if (e.sicherungFehlendeBilder > 0) {
            " Hinweis: Die gewählte Sicherung selbst ist unvollständig — ${e.sicherungFehlendeBilder} Bilder fehlten schon beim Sichern."
        } else {
            ""
        }
        val vollstaendig = e.fehlendeBilder == 0 && e.sicherungFehlendeBilder == 0
        return Result.success(
            workDataOf(K_ART to ART_IMPORT, K_VOLLSTAENDIG to vollstaendig, K_TEXT to "$kopf: " + teile.joinToString(" · ") + "." + hinweis),
        )
    }

    /** Entfernt die unvollständige, eben selbst angelegte Datei und meldet, ob das geklappt hat. */
    private fun scheitere(uri: Uri, name: String, grund: String): Result {
        val entfernt = entferne(uri)
        val zusatz = if (entfernt) " Die unvollständige Datei wurde entfernt." else " Bitte die unvollständige Datei „$name“ selbst löschen."
        return Result.failure(workDataOf(K_ART to ART_EXPORT, K_FEHLER to grund + zusatz))
    }

    private fun entferne(uri: Uri): Boolean =
        runCatching { DocumentsContract.deleteDocument(applicationContext.contentResolver, uri) }.getOrDefault(false)

    private suspend fun melde(text: String, anteil: Float) {
        setProgress(workDataOf(K_TEXT to text, K_ANTEIL to anteil))
        runCatching { setForeground(vordergrund(text)) }
    }

    private fun anzeigeName(uri: Uri): String = runCatching {
        applicationContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: "die gewählte Datei"

    private fun beschreibe(name: String, e: SicherungsErgebnis): String {
        val teile = mutableListOf(name, "${e.ausgaben} Ausgaben", "${e.bilder} Bilder")
        if (e.beschaedigt > 0) teile += "${e.beschaedigt} beschädigt (roh gesichert)"
        val beiseite = e.quarantaeneKonflikte + e.quarantaeneBilder + e.quarantaeneBeschaedigt
        if (beiseite > 0) {
            teile += "beiseitegelegt: ${e.quarantaeneKonflikte} Konfliktfassungen, ${e.quarantaeneBilder} Bilder dazu, ${e.quarantaeneBeschaedigt} beschädigte Rohdateien"
        }
        if (e.uebersprungeneTemp > 0) teile += "${e.uebersprungeneTemp} eigene Zwischendateien ausgelassen"
        teile += String.format(Locale.GERMANY, "%.1f MB", e.bytes / 1_048_576.0)
        return teile.joinToString(" · ")
    }

    companion object {
        const val K_URI = "uri"
        const val K_ART = "art"
        const val K_TEXT = "text"
        const val K_ANTEIL = "anteil"
        const val K_FEHLER = "fehler"
        const val K_VOLLSTAENDIG = "vollstaendig"
        const val ART_EXPORT = "export"
        const val ART_PRUEFEN = "pruefen"
        const val ART_IMPORT = "import"

        /** Etikett mit der Art — die Oberfläche braucht sie auch bei abgebrochenen Aufträgen ohne Ausgabe. */
        const val ETIKETT = "sicherung:"

        fun datum(zeit: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date(zeit))

        /** Startet Export oder Prüfung. Läuft schon eine Sicherung, bleibt es bei der. */
        fun starte(context: Context, uri: Uri, art: String) {
            val auftrag = OneTimeWorkRequestBuilder<SicherungWorker>()
                .setInputData(workDataOf(K_URI to uri.toString(), K_ART to art))
                .addTag(ETIKETT + art)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(Zeitplan.SICHERUNG, ExistingWorkPolicy.KEEP, auftrag)
        }
    }
}
