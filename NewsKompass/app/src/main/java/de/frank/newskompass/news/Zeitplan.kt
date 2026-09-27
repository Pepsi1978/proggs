package de.frank.newskompass.news

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.frank.newskompass.MainActivity
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.ai.CodexFehler
import de.frank.newskompass.ai.CodexFehlerArt
import de.frank.newskompass.data.model.Thema
import de.frank.newskompass.observability.KompassLog
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Ein Termin aus dem Zeitplan: der Zeitpunkt und die IDs der Themen, die dann dran sind. */
data class Termin(val zeit: ZonedDateTime, val themen: List<String>)

/**
 * Jedes Thema hat eigene Uhrzeiten (Standard 5 und 17 Uhr) und einen Rhythmus: täglich, alle
 * x Tage, wöchentlich, monatlich oder jährlich. Zu jedem Termin klingelt der Wecker und
 * recherchiert genau die Themen, die dann dran sind.
 *
 * Die Uhrzeit hält ein einziger AlarmManager-Wecker, der sich nach jedem Klingeln neu stellt
 * (Almanach A1/A10: One-shot plus Neuplanung, immer derselbe PendingIntent). Die eigentliche
 * Arbeit macht WorkManager — mit Netzbedingung, Wiederholung und Vordergrund-Hinweis.
 */
object Zeitplan {

    const val LAUF = "news-lauf"
    const val FRAGE = "news-frage"
    /** Eingabe des Laufs: IDs der Themen, die dran sind. Fehlt sie, laufen alle. */
    const val THEMEN = "themen"
    private const val EXTRA_THEMEN = "themen"

    /** So weit schaut die Planung voraus — reicht für jährliche Themen, auch über den 29. Februar. */
    private const val VORAUS_TAGE = 400L
    /** So weit schaut das Nachholen beim App-Start zurück. */
    private const val NACHHOLEN_STUNDEN = 48L

    /** Etikett am Frage-Auftrag, damit die Oberfläche die Frage schon vor dem Start zeigen kann. */
    const val FRAGE_ETIKETT = "frage:"
    /** Reihenfolge der Fragen in der Warteschlange (WorkInfo kennt keine Einreihzeit). */
    private const val NUMMER_ETIKETT = "nr:"
    private const val KANAL_LAUF = "lauf"
    private const val KANAL_FERTIG = "fertig"
    const val HINWEIS_LAUF = 17
    const val HINWEIS_FRAGE = 19
    const val HINWEIS_SICHERUNG = 21
    const val SICHERUNG = "archiv-sicherung"
    private const val HINWEIS_FERTIG = 18

    /** Alle Uhrzeiten, zu denen mindestens ein ausgefülltes Thema aktualisiert wird, aufsteigend. */
    fun uhrzeiten(themen: List<Thema>): List<Int> =
        themen.filter { it.text.isNotBlank() }.flatMap { it.uhrzeiten }.distinct().sorted()

    /** Die ausgefüllten Themen mit den [ids]; `null` heißt alle. */
    fun themenFuer(themen: List<Thema>, ids: Collection<String>?): List<Thema> =
        themen.filter { it.text.isNotBlank() && (ids == null || it.id in ids) }

    /** Alle Termine an einem Tag, frisch aus der Zeitzone gerechnet (Almanach A6), der Zeit nach. */
    private fun termineAm(themen: List<Thema>, datum: LocalDate, zone: ZoneId): List<Termin> =
        themen.filter { it.text.isNotBlank() && it.rhythmus.trifft(datum) }
            .flatMap { thema -> thema.uhrzeiten.map { m -> ZonedDateTime.of(datum, LocalTime.of(m / 60, m % 60), zone) to thema.id } }
            .groupBy({ it.first }, { it.second })
            .map { (zeit, ids) -> Termin(zeit, ids.distinct()) }
            .sortedBy { it.zeit }

    /** Nächster Termin nach [jetzt] über alle Themen — `null`, wenn keines je dran ist. */
    fun naechsterTermin(themen: List<Thema>, jetzt: ZonedDateTime = ZonedDateTime.now()): Termin? {
        val zone = ZoneId.systemDefault()
        val heute = jetzt.withZoneSameInstant(zone).toLocalDate()
        val ab = jetzt.plusSeconds(30)
        for (tag in 0..VORAUS_TAGE) {
            termineAm(themen, heute.plusDays(tag), zone).firstOrNull { it.zeit.isAfter(ab) }?.let { return it }
        }
        return null
    }

    /** Nächster Termin eines einzelnen Themas — für die Zusammenfassung in den Einstellungen. */
    fun naechsterTermin(thema: Thema, jetzt: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? =
        naechsterTermin(listOf(thema.copy(text = thema.text.ifBlank { "?" })), jetzt)?.zeit

    /**
     * Themen, die in den letzten [NACHHOLEN_STUNDEN] Stunden dran waren und seither nicht gelaufen
     * sind — für das Nachholen beim App-Start, samt dem jüngsten versäumten Zeitpunkt.
     * [letzterLauf] ist je Themen-ID der Zeitpunkt seiner jüngsten Ausgabe. Jedes Thema zählt für
     * sich: Ein späterer Lauf anderer Themen verdeckt keinen versäumten Termin.
     */
    fun versaeumt(themen: List<Thema>, letzterLauf: Map<String, Long>, jetzt: ZonedDateTime = ZonedDateTime.now()): Pair<List<String>, Long>? {
        val zone = ZoneId.systemDefault()
        val heute = jetzt.withZoneSameInstant(zone).toLocalDate()
        val grenze = jetzt.minusHours(NACHHOLEN_STUNDEN)
        val ids = linkedSetOf<String>()
        var juengster = 0L
        for (tag in -(NACHHOLEN_STUNDEN / 24 + 1)..0L) {
            termineAm(themen, heute.plusDays(tag), zone)
                .filter { it.zeit.isAfter(grenze) && !it.zeit.isAfter(jetzt) }
                .forEach { termin ->
                    val um = termin.zeit.toInstant().toEpochMilli()
                    val offen = termin.themen.filter { (letzterLauf[it] ?: 0L) < um }
                    if (offen.isNotEmpty()) {
                        ids += offen
                        juengster = maxOf(juengster, um)
                    }
                }
        }
        return if (ids.isEmpty()) null else ids.toList() to juengster
    }

    fun plane(context: Context) {
        val app = context.applicationContext as NewsApplication
        val wecker = context.getSystemService(AlarmManager::class.java) ?: return
        val stand = app.einstellungen.stand.value
        val termin = if (stand.zeitplanAktiv) naechsterTermin(stand.themen) else null
        if (termin == null) {
            wecker.cancel(weckerAbsicht(context, null))
            KompassLog.info("Zeitplan", "plane", "Kein automatischer Lauf geplant")
            return
        }
        val absicht = weckerAbsicht(context, termin.themen)
        val zeit = termin.zeit.toInstant().toEpochMilli()
        val genau = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || wecker.canScheduleExactAlarms()
        try {
            if (genau) {
                wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, absicht)
            } else {
                wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, absicht)
            }
        } catch (fehler: SecurityException) {
            wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, absicht)
        }
        KompassLog.info("Zeitplan", "plane", "Nächster Lauf geplant", mapOf("um" to termin.zeit.toString(), "themen" to termin.themen.size, "genau" to genau))
    }

    /** Immer derselbe PendingIntent (gleicher Code, gleiche Klasse); FLAG_UPDATE_CURRENT tauscht nur die Themen. */
    private fun weckerAbsicht(context: Context, themen: List<String>?): PendingIntent = PendingIntent.getBroadcast(
        context,
        4711,
        Intent(context, ZeitplanEmpfaenger::class.java).apply { if (themen != null) putExtra(EXTRA_THEMEN, themen.toTypedArray()) },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Themen aus dem klingelnden Wecker; fehlen sie (Wecker aus einer älteren Version), `null` = alle Themen. */
    fun themenAus(intent: Intent): List<String>? = intent.getStringArrayExtra(EXTRA_THEMEN)?.toList()

    /**
     * Startet einen Lauf. Läuft schon einer, bleibt es bei dem.
     *
     * [themen] sind die IDs der Themen, die recherchiert werden; `null` heißt alle Themen
     * (Knopf „Aktualisieren“).
     */
    fun starteLauf(context: Context, manuell: Boolean, themen: Collection<String>? = null) {
        val daten = if (themen == null) {
            workDataOf("manuell" to manuell)
        } else {
            workDataOf("manuell" to manuell, THEMEN to themen.toTypedArray())
        }
        val auftrag = OneTimeWorkRequestBuilder<NewsWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
            .setInputData(daten)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(LAUF, ExistingWorkPolicy.KEEP, auftrag)
    }

    /**
     * Recherchiert eine gesprochene Frage im Hintergrund und gibt die Auftragsnummer zurück.
     *
     * Kommt eine Frage, während die vorige noch läuft, stellt sie sich hinten an — keine geht
     * verloren, und Codex bekommt nie mehrere Fragen gleichzeitig.
     */
    fun starteFrage(context: Context, frage: String): UUID {
        val auftrag = OneTimeWorkRequestBuilder<FrageWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf("frage" to frage))
            .addTag(FRAGE_ETIKETT + frage)
            .addTag(NUMMER_ETIKETT + naechsteNummer())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(FRAGE, ExistingWorkPolicy.APPEND_OR_REPLACE, auftrag)
        return auftrag.id
    }

    private var letzteNummer = 0L

    @Synchronized
    private fun naechsteNummer(): Long {
        letzteNummer = maxOf(System.currentTimeMillis(), letzteNummer + 1)
        return letzteNummer
    }

    private fun nummer(info: WorkInfo): Long =
        info.tags.firstOrNull { it.startsWith(NUMMER_ETIKETT) }?.removePrefix(NUMMER_ETIKETT)?.toLongOrNull() ?: 0L

    /**
     * Verwirft eine wartende oder laufende Frage, ohne die dahinter wartenden zu verlieren.
     *
     * Die Fragen hängen in WorkManager als Kette aneinander (APPEND_OR_REPLACE). Ein Abbruch reißt
     * alle dahinter wartenden mit — deshalb werden genau die neu eingereiht, in alter Reihenfolge.
     * Gibt für jede neu eingereihte Frage alte → neue Auftragsnummer zurück.
     */
    suspend fun verwirfFrage(context: Context, id: UUID): Map<UUID, UUID> {
        val arbeit = WorkManager.getInstance(context)
        val offen = arbeit.getWorkInfosForUniqueWorkFlow(FRAGE).first().filter { !it.state.isFinished }
        val ziel = offen.firstOrNull { it.id == id }
        if (ziel == null) {
            withContext(Dispatchers.IO) { arbeit.cancelWorkById(id).result.get() }
            return emptyMap()
        }
        val dahinter = offen.filter { it.id != id && nummer(it) > nummer(ziel) }.sortedBy { nummer(it) }
        withContext(Dispatchers.IO) { arbeit.cancelWorkById(id).result.get() }
        val neu = dahinter.mapNotNull { info ->
            val text = info.tags.firstOrNull { it.startsWith(FRAGE_ETIKETT) }?.removePrefix(FRAGE_ETIKETT)
            if (text.isNullOrBlank()) null else info.id to starteFrage(context, text)
        }.toMap()
        KompassLog.info("Zeitplan", "verwirfFrage", "Frage verworfen", mapOf("neuEingereiht" to neu.size))
        return neu
    }

    fun legeKanaeleAn(context: Context) {
        val verwaltung = context.getSystemService(NotificationManager::class.java) ?: return
        verwaltung.createNotificationChannel(
            NotificationChannel(KANAL_LAUF, "Nachrichten werden geladen", NotificationManager.IMPORTANCE_LOW),
        )
        verwaltung.createNotificationChannel(
            NotificationChannel(KANAL_FERTIG, "Neue Ausgabe", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun oeffneApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        1,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun laufHinweis(context: Context, text: String, titel: String = "News Kompass recherchiert") = NotificationCompat.Builder(context, KANAL_LAUF)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle(titel)
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(oeffneApp(context))
        .build()

    fun meldeFertig(context: Context, titel: String, zeilen: List<String>) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val hinweis = NotificationCompat.Builder(context, KANAL_FERTIG)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(titel)
            .setContentText(zeilen.firstOrNull().orEmpty())
            .setStyle(NotificationCompat.InboxStyle().also { stil -> zeilen.take(6).forEach(stil::addLine) })
            .setAutoCancel(true)
            .setContentIntent(oeffneApp(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(HINWEIS_FERTIG, hinweis) }
    }
}

/** Der Wecker klingelt: Lauf anstoßen, nächsten Termin stellen. */
class ZeitplanEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val themen = Zeitplan.themenAus(intent)
        KompassLog.info("Zeitplan", "onReceive", "Wecker ausgelöst", mapOf("themen" to themen?.size))
        Zeitplan.starteLauf(context, manuell = false, themen = themen)
        Zeitplan.plane(context)
    }
}

/** Nach Neustart, App-Update oder geänderter Wecker-Erlaubnis den Termin neu stellen (Almanach B1). */
class NeustartEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Zeitplan.plane(context)
    }
}

class NewsWorker(context: Context, parameter: WorkerParameters) : CoroutineWorker(context, parameter) {

    override suspend fun getForegroundInfo(): ForegroundInfo = vordergrund("Die Nachrichten werden zusammengestellt …")

    private fun vordergrund(text: String): ForegroundInfo {
        val hinweis = Zeitplan.laufHinweis(applicationContext, text)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(Zeitplan.HINWEIS_LAUF, hinweis, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(Zeitplan.HINWEIS_LAUF, hinweis)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as NewsApplication
        // Eine Recherche dauert Minuten — als Vordergrundarbeit überlebt sie die 10-Minuten-Grenze.
        runCatching { setForeground(vordergrund("Die Nachrichten werden zusammengestellt …")) }
            .onFailure { KompassLog.warn("NewsWorker", "doWork", "Kein Vordergrund möglich", mapOf("grund" to it.message)) }
        val themen = inputData.getStringArray(Zeitplan.THEMEN)?.toList()
        if (Zeitplan.themenFuer(app.einstellungen.stand.value.themen, themen).isEmpty()) {
            // Die Themen dieses Termins wurden inzwischen gelöscht oder geleert — nichts zu tun.
            KompassLog.info("NewsWorker", "doWork", "Kein Thema für diesen Termin", mapOf("themen" to themen?.size))
            return Result.success()
        }
        return try {
            val ausgabe = app.recherche.laufe(themen) { stand ->
                setProgress(workDataOf("text" to stand.text, "anteil" to stand.anteil))
                runCatching { setForeground(vordergrund(stand.text)) }
            }
            val zeilen = ausgabe.bloecke.flatMap { b -> b.meldungen.take(2).map { "${b.titel}: ${it.titel}" } }
            Zeitplan.meldeFertig(applicationContext, "Deine ${ausgabe.slot} ist da", zeilen)
            Result.success()
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.error("NewsWorker", "doWork", "Lauf gescheitert", mapOf("grund" to fehler.message, "versuch" to runAttemptCount))
            if ((fehler is CodexFehler && fehler.art != CodexFehlerArt.NETZ) || runAttemptCount >= 2) {
                Result.failure(workDataOf("fehler" to (fehler.message ?: "Unbekannter Fehler")))
            } else {
                Result.retry()
            }
        }
    }
}

/**
 * Recherchiert eine Frage vom Mikrofon-Knopf.
 *
 * Die Fragen hängen in einer Kette hintereinander. Ein gescheiterter Auftrag würde alle
 * wartenden mit in den Abgrund reißen — deshalb endet jeder Auftrag als Erfolg und trägt einen
 * Fehler als Ausgabe `fehler` weiter. Nur ein Netzproblem wird noch einmal versucht.
 */
class FrageWorker(context: Context, parameter: WorkerParameters) : CoroutineWorker(context, parameter) {

    override suspend fun getForegroundInfo(): ForegroundInfo = vordergrund("Deine Frage wird recherchiert …")

    private fun vordergrund(text: String): ForegroundInfo {
        val hinweis = Zeitplan.laufHinweis(applicationContext, text)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(Zeitplan.HINWEIS_FRAGE, hinweis, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(Zeitplan.HINWEIS_FRAGE, hinweis)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as NewsApplication
        val frage = inputData.getString("frage").orEmpty()
        runCatching { setForeground(vordergrund("Deine Frage wird recherchiert …")) }
            .onFailure { KompassLog.warn("FrageWorker", "doWork", "Kein Vordergrund möglich", mapOf("grund" to it.message)) }
        return try {
            val (ausgabe, block) = app.recherche.beantworteFrage(frage) { stand ->
                setProgress(workDataOf("text" to stand.text, "anteil" to stand.anteil))
            }
            Zeitplan.meldeFertig(applicationContext, "Deine Antwort ist da: ${block.titel}", block.meldungen.take(6).map { it.titel })
            Result.success(workDataOf("ausgabeId" to ausgabe.id, "themaId" to block.themaId))
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.error("FrageWorker", "doWork", "Frage gescheitert", mapOf("grund" to fehler.message, "versuch" to runAttemptCount))
            val netz = fehler !is CodexFehler || fehler.art == CodexFehlerArt.NETZ
            if (netz && fehler !is IllegalStateException && runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.success(workDataOf("fehler" to (fehler.message ?: "Die Frage konnte nicht recherchiert werden.")))
            }
        }
    }
}
