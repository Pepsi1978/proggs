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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    /**
     * Eingabe des Laufs: je Thema „ID|Zeitpunkt“ des Termins, für den es dran ist. Fehlt sie,
     * laufen alle. Der Zeitpunkt wird beim Start erneut geprüft — wartete der Lauf aufs Netz und
     * wurde der Termin inzwischen geändert oder gestrichen, fällt das Thema weg.
     */
    const val THEMEN = "themen"
    private const val EXTRA_THEMEN = "themen"
    private const val EXTRA_UM = "um"

    /** So weit schaut die Planung voraus — reicht für jährliche Themen, auch über den 29. Februar. */
    private const val VORAUS_TAGE = 400L
    /** So weit schaut das Nachholen beim App-Start zurück. */
    private const val NACHHOLEN_STUNDEN = 48L
    /**
     * Ein Lauf, der höchstens so lange vor einem Termin begann, deckt ihn mit ab — etwa ein Tipp auf
     * Aktualisieren um 4:58 den Termin um 5 Uhr. Sonst liefe dasselbe Thema gleich noch einmal.
     */
    const val SCHON_ERLEDIGT_MS = 15 * 60_000L

    /** Etikett am Frage-Auftrag, damit die Oberfläche die Frage schon vor dem Start zeigen kann. */
    const val FRAGE_ETIKETT = "frage:"
    /** Reihenfolge der Fragen in der Warteschlange (WorkInfo kennt keine Einreihzeit). */
    const val NUMMER_ETIKETT = "nr:"
    private const val KANAL_LAUF = "lauf"
    private const val KANAL_FERTIG = "fertig"
    const val HINWEIS_LAUF = 17
    const val HINWEIS_FRAGE = 19
    const val HINWEIS_SICHERUNG = 21
    private const val HINWEIS_GESCHEITERT = 22
    const val SICHERUNG = "archiv-sicherung"
    private const val HINWEIS_FERTIG = 18

    /** Alle Uhrzeiten, zu denen mindestens ein ausgefülltes Thema aktualisiert wird, aufsteigend. */
    fun uhrzeiten(themen: List<Thema>): List<Int> =
        themen.filter { it.text.isNotBlank() }.flatMap { it.uhrzeiten }.distinct().sorted()

    /**
     * Die ausgefüllten Themen, die laut [auftrag] (Themen-ID → Zeitpunkt des Termins) nach dem
     * aktuellen Zeitplan wirklich dran sind; `null` heißt alle.
     */
    fun themenFuer(themen: List<Thema>, auftrag: Map<String, Long>?): List<Thema> =
        themen.filter { thema ->
            thema.text.isNotBlank() && (auftrag == null || auftrag[thema.id]?.let { istDran(thema, it) } == true)
        }

    /** Hat [thema] nach seinen aktuellen Einstellungen genau um [um] einen Termin? */
    fun istDran(thema: Thema, um: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val datum = Instant.ofEpochMilli(um).atZone(zone).toLocalDate()
        return termineAm(listOf(thema), datum, zone).any { it.zeit.toInstant().toEpochMilli() == um }
    }

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
        // Streng nach jetzt: Ein Puffer würde einen gleich anstehenden Termin überspringen, wenn kurz
        // vorher neu geplant wird (App-Start, geänderte Einstellung). Der Wecker selbst klingelt nie zu früh.
        for (tag in 0..VORAUS_TAGE) {
            termineAm(themen, heute.plusDays(tag), zone).firstOrNull { it.zeit.isAfter(jetzt) }?.let { return it }
        }
        return null
    }

    /** Nächster Termin eines einzelnen Themas — für die Zusammenfassung in den Einstellungen. */
    fun naechsterTermin(thema: Thema, jetzt: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? =
        naechsterTermin(listOf(thema.copy(text = thema.text.ifBlank { "?" })), jetzt)?.zeit

    /**
     * Themen, die in den letzten [NACHHOLEN_STUNDEN] Stunden dran waren und seither nicht gelaufen
     * sind — je Thema mit seinem jüngsten versäumten Termin, für das Nachholen beim App-Start.
     * [letzterLauf] ist je Themen-ID der Zeitpunkt seiner jüngsten Ausgabe. Jedes Thema zählt für
     * sich: Ein späterer Lauf anderer Themen verdeckt keinen versäumten Termin.
     */
    fun versaeumt(themen: List<Thema>, letzterLauf: Map<String, Long>, jetzt: ZonedDateTime = ZonedDateTime.now()): Map<String, Long> =
        faelligZwischen(themen, jetzt.minusHours(NACHHOLEN_STUNDEN), jetzt)
            .filter { (id, um) -> (letzterLauf[id] ?: 0L) + SCHON_ERLEDIGT_MS < um }

    /**
     * Je Thema sein jüngster Termin nach [von] (ausschließlich) bis [bis] (einschließlich) — etwa
     * die Termine, die ein verspäteter Wecker übersprungen hat.
     */
    fun faelligZwischen(themen: List<Thema>, von: ZonedDateTime, bis: ZonedDateTime): Map<String, Long> {
        val zone = ZoneId.systemDefault()
        val offen = linkedMapOf<String, Long>()
        var tag = von.withZoneSameInstant(zone).toLocalDate()
        val letzterTag = bis.withZoneSameInstant(zone).toLocalDate()
        while (!tag.isAfter(letzterTag)) {
            termineAm(themen, tag, zone)
                .filter { it.zeit.isAfter(von) && !it.zeit.isAfter(bis) }
                .forEach { termin -> termin.themen.forEach { offen[it] = termin.zeit.toInstant().toEpochMilli() } }
            tag = tag.plusDays(1)
        }
        return offen
    }

    /**
     * Holt beim App-Start und nach einem Neustart des Handys nach, was versäumt wurde: Termine, deren
     * Thema seither nicht erfolgreich lief, und offene Termine, deren Lauf nie zustande kam.
     */
    suspend fun holeNach(context: Context) {
        val app = context.applicationContext as NewsApplication
        app.speicher.bereit()
        if (!app.codex.istVerbunden) return
        val stand = app.einstellungen.stand.value
        val auftrag = if (stand.zeitplanAktiv) {
            // Jüngster erfolgreicher Lauf je Thema; gesprochene Fragen und gescheiterte Blöcke zählen nicht.
            val letzterLauf = mutableMapOf<String, Long>()
            app.speicher.ausgabenSeit(System.currentTimeMillis() - (NACHHOLEN_STUNDEN + 2) * 3_600_000L).forEach { ausgabe ->
                ausgabe.bloecke.filter { NewsRecherche.istGelaufen(it) }.forEach { block ->
                    letzterLauf[block.themaId] = maxOf(letzterLauf[block.themaId] ?: 0L, ausgabe.erstelltUm)
                }
            }
            versaeumt(stand.themen, letzterLauf)
        } else {
            emptyMap()
        }
        val letzter = auftrag.values.maxOrNull()
        // Scheiterte seit dem letzten Termin schon ein Lauf an Kontingent oder Anmeldung, nicht bei
        // jedem Öffnen erneut anstoßen — der nächste Termin oder ein Tipp auf Aktualisieren holt es nach.
        if (letzter != null && app.einstellungen.harterFehlerUm < letzter) {
            KompassLog.info("Zeitplan", "holeNach", "Versäumte Termine werden nachgeholt", mapOf("themen" to auftrag.size))
            starteLauf(context, manuell = false, auftrag = auftrag)
        } else if (app.einstellungen.hatOffeneLaeufe) {
            reiheLaufEin(context)
        }
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
        val absicht = weckerAbsicht(context, termin)
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

    /** Immer derselbe PendingIntent (gleicher Code, gleiche Klasse); FLAG_UPDATE_CURRENT tauscht nur den Termin. */
    private fun weckerAbsicht(context: Context, termin: Termin?): PendingIntent = PendingIntent.getBroadcast(
        context,
        4711,
        Intent(context, ZeitplanEmpfaenger::class.java).apply {
            if (termin != null) {
                putExtra(EXTRA_THEMEN, termin.themen.toTypedArray())
                putExtra(EXTRA_UM, termin.zeit.toInstant().toEpochMilli())
            }
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Auftrag aus dem klingelnden Wecker; fehlt er (Wecker aus einer älteren Version), `null` = alle Themen. */
    fun auftragAus(intent: Intent): Map<String, Long>? {
        val themen = intent.getStringArrayExtra(EXTRA_THEMEN) ?: return null
        val um = intent.getLongExtra(EXTRA_UM, -1L).takeIf { it > 0 } ?: return null
        return themen.associateWith { um }
    }

    /** Liest die Eingabe [THEMEN] eines Laufs zurück; `null` heißt alle Themen. */
    fun auftragAus(eingabe: Array<String>?): Map<String, Long>? = eingabe?.mapNotNull { eintrag ->
        val trenner = eintrag.lastIndexOf('|')
        val um = eintrag.substring(trenner + 1).toLongOrNull()
        if (trenner <= 0 || um == null) null else eintrag.substring(0, trenner) to um
    }?.toMap()

    /**
     * Merkt die Termine vor und startet den Lauf. Läuft oder wartet schon einer, arbeitet er die
     * vorgemerkten Termine vor seinem Ende mit ab.
     *
     * [auftrag] ordnet jedem Thema den Zeitpunkt seines Termins zu; `null` heißt alle Themen
     * (Knopf „Aktualisieren“).
     */
    fun starteLauf(context: Context, manuell: Boolean, auftrag: Map<String, Long>? = null) {
        // Erst dauerhaft vormerken, dann anstoßen: Wartet oder läuft schon ein Lauf, verwirft KEEP den
        // neuen Auftrag — der laufende nimmt die vorgemerkten Termine aber vor seinem Ende noch mit.
        (context.applicationContext as NewsApplication).einstellungen.merkeOffenenLauf(auftrag)
        KompassLog.info("Zeitplan", "starteLauf", "Lauf vorgemerkt", mapOf("manuell" to manuell, "themen" to auftrag?.size))
        reiheLaufEin(context)
    }

    /** Stößt den Lauf an, der die vorgemerkten Termine abarbeitet. */
    private fun reiheLaufEin(context: Context) {
        val auftrag = OneTimeWorkRequestBuilder<NewsWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
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

    private const val FRAGEN_ABLAGE = "fragen"
    private const val VERWORFEN = "verworfen"
    private val verworfenFluss = MutableStateFlow<Set<String>>(emptySet())
    private var verworfenGeladen = false

    /**
     * Auftragsnummern verworfener Fragen. Sie bleiben in der Kette stehen und enden beim Start sofort —
     * ein Abbruch per WorkManager würde die dahinter wartenden mitreißen, und das nächste Anhängen
     * (APPEND_OR_REPLACE) löschte dann die ganze Kette samt der laufenden Frage.
     */
    @Synchronized
    fun verworfen(context: Context): StateFlow<Set<String>> {
        if (!verworfenGeladen) {
            verworfenFluss.value = fragenAblage(context).getStringSet(VERWORFEN, emptySet()).orEmpty().toSet()
            verworfenGeladen = true
        }
        return verworfenFluss
    }

    @Synchronized
    private fun aendereVerworfen(context: Context, aendern: (Set<String>) -> Set<String>) {
        verworfen(context)
        verworfenFluss.value = aendern(verworfenFluss.value)
        fragenAblage(context).edit().putStringSet(VERWORFEN, verworfenFluss.value).commit()
    }

    private fun fragenAblage(context: Context) =
        context.applicationContext.getSharedPreferences(FRAGEN_ABLAGE, Context.MODE_PRIVATE)

    /** Verwirft eine wartende oder laufende Frage, ohne die dahinter wartenden zu berühren. */
    suspend fun verwirfFrage(context: Context, id: UUID) {
        val info = withContext(Dispatchers.IO) { WorkManager.getInstance(context).getWorkInfoById(id).get() }
        if (info == null || info.state.isFinished) return
        withContext(Dispatchers.IO) { aendereVerworfen(context) { it + id.toString() } }
        KompassLog.info("Zeitplan", "verwirfFrage", "Frage verworfen", mapOf("laeuft" to (info.state == WorkInfo.State.RUNNING)))
    }

    /** Die Frage [id] ist beendet — ihre Marke „verworfen“ wird nicht mehr gebraucht. */
    fun vergissVerworfen(context: Context, id: UUID) = aendereVerworfen(context) { it - id.toString() }

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

    /** Jede neue Ausgabe bekommt ihre eigene Benachrichtigung ([nummer] je Ausgabe) — keine überschreibt die vorige. */
    fun meldeFertig(context: Context, titel: String, zeilen: List<String>, nummer: Int = HINWEIS_FERTIG) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val hinweis = NotificationCompat.Builder(context, KANAL_FERTIG)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(titel)
            .setContentText(zeilen.firstOrNull().orEmpty())
            .setStyle(NotificationCompat.InboxStyle().also { stil -> zeilen.take(6).forEach(stil::addLine) })
            .setAutoCancel(true)
            .setContentIntent(oeffneApp(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(nummer, hinweis) }
    }

    /** Ein automatischer Lauf ist endgültig gescheitert — sonst bliebe er ganz still aus. */
    fun meldeGescheitert(context: Context, grund: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val hinweis = NotificationCompat.Builder(context, KANAL_FERTIG)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Automatisches Update gescheitert")
            .setContentText(grund)
            .setStyle(NotificationCompat.BigTextStyle().bigText(grund))
            .setAutoCancel(true)
            .setContentIntent(oeffneApp(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(HINWEIS_GESCHEITERT, hinweis) }
    }

    /** Nach einem erfolgreichen Lauf ist ein alter Fehlerhinweis überholt. */
    fun vergissFehlerHinweis(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(HINWEIS_GESCHEITERT) }
    }

    /** Eigene Benachrichtigungsnummer je Ausgabe oder Frage, fern von den festen Nummern oben. */
    fun hinweisNummer(schluessel: String): Int = (schluessel.hashCode() and 0x3fffffff) or 0x40000000
}

/** Der Wecker klingelt: Lauf anstoßen, nächsten Termin stellen. */
class ZeitplanEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val auftrag = Zeitplan.auftragAus(intent)
        // Klingelte der Wecker verspätet, kämen Termine anderer Themen dazwischen nie dran — sie laufen gleich mit.
        val stand = (context.applicationContext as NewsApplication).einstellungen.stand.value
        val um = auftrag?.values?.firstOrNull()
        val verspaetet = if (um != null && stand.zeitplanAktiv) {
            Zeitplan.faelligZwischen(stand.themen, Instant.ofEpochMilli(um).atZone(ZoneId.systemDefault()), ZonedDateTime.now())
        } else {
            emptyMap()
        }
        KompassLog.info("Zeitplan", "onReceive", "Wecker ausgelöst", mapOf("themen" to auftrag?.size, "verspaetet" to verspaetet.size))
        Zeitplan.starteLauf(context, manuell = false, auftrag = auftrag?.let { it + verspaetet })
        Zeitplan.plane(context)
    }
}

/**
 * Nach Neustart, App-Update, geänderter Uhrzeit oder Zeitzone und geänderter Wecker-Erlaubnis den
 * Termin neu stellen (Almanach B1) und Versäumtes nachholen — nicht erst beim nächsten Öffnen der App.
 */
class NeustartEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Zeitplan.plane(context)
        val app = context.applicationContext as NewsApplication
        val fertig = goAsync()
        app.bereich.launch {
            try {
                // Ein Empfänger hat nur begrenzt Zeit; was hier nicht fertig wird, holt das Öffnen der App nach.
                withTimeoutOrNull(8_000L) { Zeitplan.holeNach(context) }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                KompassLog.warn("Zeitplan", "NeustartEmpfaenger", "Nachholen gescheitert", mapOf("grund" to fehler.message))
            } finally {
                fertig.finish()
            }
        }
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
        try {
            setForeground(vordergrund("Die Nachrichten werden zusammengestellt …"))
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.warn("NewsWorker", "doWork", "Kein Vordergrund möglich", mapOf("grund" to fehler.message))
        }
        // Aufträge aus älteren Versionen tragen ihre Themen noch in der Eingabe — nur beim ersten Versuch übernehmen.
        if (runAttemptCount == 0) {
            inputData.getStringArray(Zeitplan.THEMEN)?.let { app.einstellungen.merkeOffenenLauf(Zeitplan.auftragAus(it)) }
        }
        var ergebnis: Result = Result.success()
        // Themen, die dieser Auftrag schon recherchiert hat, mit Beginn und Ende ihres Laufs.
        val erledigt = mutableMapOf<String, Pair<Long, Long>>()
        // Ende des letzten Laufs über alle Themen — ein zweiter Tipp auf Aktualisieren währenddessen löst keinen weiteren aus.
        var alleErledigtUm = 0L
        // Solange Termine vorgemerkt sind, weiterarbeiten — auch die, die während des Laufs dazukamen.
        while (true) {
            // Lesen, nicht herausnehmen: Die Termine bleiben vorgemerkt, bis sie erledigt oder endgültig gescheitert sind.
            val (alle, termine) = app.einstellungen.offeneLaeufe() ?: break
            if (alle && System.currentTimeMillis() - alleErledigtUm < Zeitplan.SCHON_ERLEDIGT_MS) {
                KompassLog.info("NewsWorker", "doWork", "Alle Themen liefen gerade erst — kein zweiter Lauf")
                app.einstellungen.entferneOffeneLaeufe(alle, termine)
                continue
            }
            val stand = app.einstellungen.stand.value
            val faellig = when {
                alle -> Zeitplan.themenFuer(stand.themen, null)
                // Der Zeitplan wurde ausgeschaltet, während der Lauf aufs Netz wartete.
                !stand.zeitplanAktiv -> emptyList()
                else -> Zeitplan.themenFuer(
                    stand.themen,
                    // Abgedeckt ist ein Termin, der während des vorigen Laufs lag oder kurz vor dessen Beginn.
                    termine.filter { (id, um) ->
                        erledigt[id]?.let { (beginn, ende) -> um > ende && um - beginn > Zeitplan.SCHON_ERLEDIGT_MS } ?: true
                    },
                )
            }
            if (faellig.isEmpty() && !alle) {
                // Die Termine wurden inzwischen geändert, gestrichen oder die Themen gelöscht — nichts zu tun.
                KompassLog.info("NewsWorker", "doWork", "Kein Thema mehr für diese Termine", mapOf("themen" to termine.size))
                app.einstellungen.entferneOffeneLaeufe(alle, termine)
                continue
            }
            val beginn = System.currentTimeMillis()
            try {
                val ausgabe = app.recherche.laufe(if (alle) null else faellig.map { it.id }) { fortschritt ->
                    setProgress(workDataOf("text" to fortschritt.text, "anteil" to fortschritt.anteil))
                    try {
                        setForeground(vordergrund(fortschritt.text))
                    } catch (abbruch: CancellationException) {
                        throw abbruch
                    } catch (fehler: Exception) {
                        // Der Hinweis bleibt dann eben beim vorigen Text.
                    }
                }
                val ende = System.currentTimeMillis()
                // Einzelne Themen können gescheitert sein (etwa Netz weg), während andere durchkamen.
                val gescheitert = ausgabe.bloecke.filter { it.frage == null && !NewsRecherche.istGelaufen(it) }
                val gescheiterteIds = gescheitert.map { it.themaId }.toSet()
                // Automatische Termine gescheiterter Themen noch einmal versuchen — nicht nach Kontingent- oder Anmeldefehler.
                val nochmal = !alle && gescheiterteIds.isNotEmpty() && runAttemptCount < 2 &&
                    app.einstellungen.harterFehlerUm < beginn
                app.einstellungen.entferneOffeneLaeufe(alle, if (nochmal) termine.filterKeys { it !in gescheiterteIds } else termine)
                faellig.filter { it.id !in gescheiterteIds }.forEach { erledigt[it.id] = beginn to ende }
                if (alle) alleErledigtUm = ende
                if (gescheiterteIds.isEmpty()) {
                    Zeitplan.vergissFehlerHinweis(applicationContext)
                } else if (!alle && !nochmal) {
                    // Endgültig: nicht bei jedem Öffnen der App erneut nachholen.
                    app.einstellungen.merkeHartenFehler(ende)
                }
                val zeilen = gescheitert.map { "Nicht aktualisiert: ${it.titel}" } +
                    ausgabe.bloecke.flatMap { b -> b.meldungen.take(2).map { "${b.titel}: ${it.titel}" } }
                Zeitplan.meldeFertig(applicationContext, "Deine ${ausgabe.slot} ist da", zeilen, Zeitplan.hinweisNummer(ausgabe.id))
                if (nochmal) return Result.retry()
                ergebnis = Result.success()
            } catch (abbruch: CancellationException) {
                // Android stoppt den Auftrag (etwa Netz weg) — die Termine bleiben für den nächsten Versuch vorgemerkt.
                throw abbruch
            } catch (fehler: Exception) {
                KompassLog.error("NewsWorker", "doWork", "Lauf gescheitert", mapOf("grund" to fehler.message, "versuch" to runAttemptCount))
                // Kontingent, Anmeldung oder eine dauerhafte Ablehnung (etwa Fehler 400) bessern sich durch Warten nicht.
                val endgueltig = (fehler is CodexFehler && (fehler.art != CodexFehlerArt.NETZ || !fehler.wiederholbar)) ||
                    fehler is IllegalStateException || runAttemptCount >= 2
                if (!endgueltig) return Result.retry()
                app.einstellungen.entferneOffeneLaeufe(alle, termine)
                val grund = fehler.message ?: "Unbekannter Fehler"
                if (!alle || termine.isNotEmpty()) {
                    // Nicht bei jedem Öffnen der App erneut nachholen und genauso scheitern — der nächste Termin
                    // oder ein Tipp auf Aktualisieren versucht es wieder.
                    app.einstellungen.merkeHartenFehler(System.currentTimeMillis())
                    Zeitplan.meldeGescheitert(applicationContext, grund)
                }
                ergebnis = Result.failure(workDataOf("fehler" to grund))
            }
        }
        return ergebnis
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
        val verworfen = Zeitplan.verworfen(applicationContext)
        if (id.toString() in verworfen.value) {
            Zeitplan.vergissVerworfen(applicationContext, id)
            return Result.success(workDataOf("verworfen" to true))
        }
        try {
            setForeground(vordergrund("Deine Frage wird recherchiert …"))
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.warn("FrageWorker", "doWork", "Kein Vordergrund möglich", mapOf("grund" to fehler.message))
        }
        return try {
            // Wird die Frage verworfen, während sie läuft, bricht nur ihre Recherche ab — der Auftrag endet als
            // Erfolg, damit die Kette der wartenden Fragen weiterläuft.
            val (ausgabe, block) = coroutineScope {
                val recherche = async {
                    app.recherche.beantworteFrage(frage) { stand ->
                        setProgress(workDataOf("text" to stand.text, "anteil" to stand.anteil))
                    }
                }
                val waechter = launch {
                    verworfen.first { id.toString() in it }
                    recherche.cancel()
                }
                try {
                    recherche.await()
                } finally {
                    waechter.cancel()
                }
            }
            Zeitplan.meldeFertig(
                applicationContext,
                "Deine Antwort ist da: ${block.titel}",
                block.meldungen.take(6).map { it.titel },
                Zeitplan.hinweisNummer(block.themaId),
            )
            Zeitplan.vergissVerworfen(applicationContext, id)
            Result.success(workDataOf("ausgabeId" to ausgabe.id, "themaId" to block.themaId))
        } catch (abbruch: CancellationException) {
            if (currentCoroutineContext().isActive && id.toString() in verworfen.value) {
                Zeitplan.vergissVerworfen(applicationContext, id)
                return Result.success(workDataOf("verworfen" to true))
            }
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.error("FrageWorker", "doWork", "Frage gescheitert", mapOf("grund" to fehler.message, "versuch" to runAttemptCount))
            val netz = fehler !is CodexFehler || fehler.art == CodexFehlerArt.NETZ
            if (netz && fehler !is IllegalStateException && runAttemptCount < 2) {
                Result.retry()
            } else {
                Zeitplan.vergissVerworfen(applicationContext, id)
                Result.success(workDataOf("fehler" to (fehler.message ?: "Die Frage konnte nicht recherchiert werden.")))
            }
        }
    }
}
