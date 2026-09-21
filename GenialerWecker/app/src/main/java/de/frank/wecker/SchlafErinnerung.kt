package de.frank.wecker

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import de.frank.genialeideen.R
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Reine Planung der Schlafenszeit-Erinnerung, ohne Android. */
object SchlafPlan {
    /** Vorgabe und Grenzen des Vorlaufs in Minuten. 0 heißt „zur Schlafenszeit“, nicht „aus“. */
    const val LEAD_DEFAULT_MINUTES = 15
    const val LEAD_MAX_MINUTES = 60
    /**
     * Ohne Vorlauf fallen Auslösung und Schlafenszeit auf dieselbe Millisekunde. Jede noch so kleine
     * Zustellverzögerung von Android würde die Erinnerung sonst stumm verwerfen, deshalb diese begrenzte
     * Karenz – und nur für diesen Fall, damit die Verfallsregeln bei echtem Vorlauf unverändert bleiben.
     */
    const val KARENZ_OHNE_VORLAUF_MS = 60_000L
    /** Bounded search: enough for a 24 h sleep duration before a daily alarm, never an endless loop. */
    private const val MAX_OCCURRENCES = 8

    /**
     * One reminder occurrence; [key] is its fingerprint (alarm, wake time, sleep duration).
     * Der Vorlauf gehört bewusst NICHT dazu: Fingerabdruck und Gruppe bleiben über eine Vorlaufänderung
     * hinweg stabil, damit eine bereits zugestellte Erinnerung nicht erneut ertönt.
     */
    data class Vorkommen(val alarmId: String, val wakeAt: Long, val sleepMinutes: Int) {
        val bedtime: Long get() = Schlaf.bedtime(wakeAt, sleepMinutes)
        fun trigger(leadMs: Long): Long = bedtime - leadMs
        val key: String get() = "$alarmId|$wakeAt|$sleepMinutes"
        /** Same bedtime minute = one shared notification. */
        val group: Long get() = bedtime / 60_000L
    }

    fun leadMs(minutes: Int): Long = minutes.coerceIn(0, LEAD_MAX_MINUTES) * 60_000L

    /** Wake times from the stored nextAt (which already reflects a skipped occurrence) onwards. */
    fun wakeTimes(alarm: Alarm, zone: ZoneId = ZoneId.systemDefault()): List<Long> {
        if (!alarm.enabled || alarm.sleepMinutes <= 0 || alarm.nextAt <= 0) return emptyList()
        val result = mutableListOf(alarm.nextAt)
        if (!alarm.repeats) return result
        while (result.size < MAX_OCCURRENCES) {
            val next = runCatching { AlarmTime.next(alarm, Instant.ofEpochMilli(result.last() + 1000), zone) }.getOrNull() ?: break
            if (next <= result.last()) break
            result += next
        }
        return result
    }

    private fun occurrences(alarm: Alarm, zone: ZoneId) = wakeTimes(alarm, zone).asSequence().map { Vorkommen(alarm.id, it, alarm.sleepMinutes) }

    /** First occurrence whose reminder still lies in the future; a missed reminder is never caught up. */
    fun next(alarm: Alarm, now: Long, enabledSetting: Boolean, zone: ZoneId = ZoneId.systemDefault(), leadMinutes: Int = LEAD_DEFAULT_MINUTES): Vorkommen? =
        if (!enabledSetting) null else leadMs(leadMinutes).let { lead -> occurrences(alarm, zone).firstOrNull { it.trigger(lead) > now } }

    /**
     * A delivery is shown only if its fingerprint is still planned and now lies between reminder and bedtime,
     * both inclusive. Ohne Vorlauf fallen beide Grenzen zusammen; dann verlängert [KARENZ_OHNE_VORLAUF_MS] das
     * Fenster um bis zu eine Minute, höchstens jedoch bis zur Weckzeit. Bei Vorlauf über 0 bleibt es beim
     * bisherigen Fenster bis zur Schlafenszeit.
     */
    fun gueltig(alarm: Alarm?, wakeAt: Long, sleepMinutes: Int, now: Long, enabledSetting: Boolean,
        zone: ZoneId = ZoneId.systemDefault(), leadMinutes: Int = LEAD_DEFAULT_MINUTES): Vorkommen? {
        if (alarm == null || !enabledSetting || alarm.sleepMinutes != sleepMinutes) return null
        if (wakeAt !in wakeTimes(alarm, zone)) return null
        val occurrence = Vorkommen(alarm.id, wakeAt, sleepMinutes)
        val lead = leadMs(leadMinutes)
        // Nur ohne Vorlauf greift die Karenz, und nie über die Weckzeit hinaus.
        val spaetestens = if (lead == 0L) minOf(occurrence.bedtime + KARENZ_OHNE_VORLAUF_MS, wakeAt) else occurrence.bedtime
        return occurrence.takeIf { now >= occurrence.trigger(lead) && now <= spaetestens }
    }

    /** Whether an alarm still belongs to a visible group: same bedtime minute, planned, wake time not yet passed. */
    fun inGruppe(alarm: Alarm, group: Long, now: Long, enabledSetting: Boolean, zone: ZoneId = ZoneId.systemDefault()): Vorkommen? =
        if (!enabledSetting) null else occurrences(alarm, zone).firstOrNull { it.group == group && it.wakeAt > now }

    data class Eintrag(val name: String, val wakeAt: Long, val sleepMinutes: Int)

    /** Notification text. With several alarms every one keeps its own wake time and sleep duration. */
    fun text(bedtime: Long, now: Long, entries: List<Eintrag>, zone: ZoneId = ZoneId.systemDefault()): String {
        fun wake(time: Long) = Instant.ofEpochMilli(time).atZone(zone).format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.GERMAN))
        val sorted = entries.sortedBy { it.wakeAt }
        val head = "Schlafenszeit ≈ ${Schlaf.tagLabel(bedtime, now, zone)}"
        val single = sorted.singleOrNull()
        return if (single != null) "$head · ${Schlaf.dauer(single.sleepMinutes)} Schlaf bis „${single.name}“ ${wake(single.wakeAt)}"
            else "$head · " + sorted.joinToString(" und ") { "„${it.name}“ ${wake(it.wakeAt)} (${Schlaf.dauer(it.sleepMinutes)})" }
    }

    /** Persistent delivery marks per group, so double broadcasts or restarts never sound twice. */
    data class Marken(val gruppen: Map<Long, Set<String>>) {
        fun enthaelt(v: Vorkommen) = gruppen[v.group]?.contains(v.key) == true
        fun gruppeGemeldet(v: Vorkommen) = gruppen[v.group].orEmpty().isNotEmpty()
        fun mit(v: Vorkommen) = Marken(gruppen + (v.group to (gruppen[v.group].orEmpty() + v.key)))
        /** Groups whose bedtime is more than two days old are dropped. */
        fun bereinigt(now: Long) = Marken(gruppen.filterKeys { it * 60_000L > now - 2 * 24 * 60 * 60_000L })
        fun json(): String = JSONObject().apply { gruppen.forEach { (group, keys) -> put(group.toString(), JSONArray(keys.sorted())) } }.toString()
        companion object {
            fun parse(raw: String?): Marken = runCatching {
                val j = JSONObject(raw ?: "{}")
                Marken(j.keys().asSequence().associate { k -> k.toLong() to j.getJSONArray(k).let { a -> (0 until a.length()).map(a::getString).toSet() } })
            }.getOrDefault(Marken(emptyMap()))
        }
    }
}

/**
 * Schlafenszeit-Erinnerung mit dem eingestellten Vorlauf vor der Schlafenszeit (0 bis 60 Minuten, Vorgabe 15;
 * 0 erinnert genau zur Schlafenszeit). Strikt getrennt vom Wecken: eigener Receiver,
 * eigene data-URI, eigener Kanal. Fehler landen nur im eigenen Status, nie an einem Wecker.
 * All read-modify-write of switch, marks and status runs under [lock]; receiver (Main) and scheduler (IO) may run in parallel.
 */
object SchlafErinnerung {
    private const val TAG = "WeckerSchlaf"
    const val SETTING_KEY = "notify_bedtime"
    const val LEAD_KEY = "notify_bedtime_lead_minutes"
    private const val MARKS_KEY = "sleep_reminder_marks"
    private const val PLAN_ISSUES_KEY = "sleep_reminder_plan_issues"
    private const val DELIVERY_ISSUE_KEY = "sleep_reminder_delivery_issue"
    /** v2 ist stumm: Den Ton spielt [SchlafTon] selbst, mit eigener Lautstärke. */
    const val CHANNEL = "sleep_reminder_v2"
    private const val ALTER_KANAL = "sleep_reminder_v1"
    private const val NOTIFICATION_ID = 6000
    private const val TAG_PREFIX = "schlaf:"
    private val lock = Any()
    /** Fallback marks for this process when the durable mark could not be written. */
    private val memoryMarks = mutableSetOf<String>()

    private fun prefs(context: Context) = AlarmStore.get(context).prefs
    /** Confirmed switch value; loaded once, changed only after a successful commit. */
    private var confirmedEnabled: Boolean? = null
    /** Ebenso bestätigt wie der Schalter: nur ein erfolgreiches commit() ändert den geltenden Vorlauf. */
    private var confirmedLead: Int? = null
    fun enabled(context: Context): Boolean = synchronized(lock) {
        confirmedEnabled ?: (try { prefs(context).getBoolean(SETTING_KEY, true) }
            catch (e: Exception) { Log.w(TAG, "Schalter unlesbar", e); true }).also { confirmedEnabled = it }
    }

    /** Vorlauf in ganzen Minuten, 0..60. 0 heißt „zur Schlafenszeit“ – die Aktivierung entscheidet allein der Schalter. */
    fun leadMinutes(context: Context): Int = synchronized(lock) {
        confirmedLead ?: (try { prefs(context).getInt(LEAD_KEY, SchlafPlan.LEAD_DEFAULT_MINUTES) }
            catch (e: Exception) { Log.w(TAG, "Vorlauf unlesbar", e); SchlafPlan.LEAD_DEFAULT_MINUTES })
            .coerceIn(0, SchlafPlan.LEAD_MAX_MINUTES).also { confirmedLead = it }
    }

    /**
     * Speichert den Vorlauf und plant danach alles um. Liefert false, wenn nicht dauerhaft gespeichert werden
     * konnte; dann gilt der bisherige Wert weiter – auch im Arbeitsspeicher, weil ein gescheitertes commit()
     * die Voreinstellungen im Speicher trotzdem verändert.
     */
    fun setLeadMinutes(context: Context, minutes: Int): Boolean = synchronized(lock) {
        val ziel = minutes.coerceIn(0, SchlafPlan.LEAD_MAX_MINUTES)
        val previous = leadMinutes(context)
        if (ziel == previous) return@synchronized true
        val stored = try { prefs(context).edit().putInt(LEAD_KEY, ziel).commit() }
            catch (e: Exception) { Log.w(TAG, "Vorlauf nicht gespeichert", e); false }
        if (!stored) {
            try { prefs(context).edit().putInt(LEAD_KEY, previous).commit() }
            catch (e: Exception) { Log.w(TAG, "Vorlauf nicht zurückgesetzt", e) }
            return@synchronized false
        }
        confirmedLead = ziel
        // Alle bestehenden Auftraege bekommen ihren neuen Zeitpunkt; derselbe PendingIntent ersetzt den alten.
        syncAll(context)
        true
    }

    // ---------- status, per path (best effort: never throws into scheduling or the UI) ----------
    /** RAM fallback when a status could not be stored; shown until it is written or cleared. */
    private val memoryPlanIssues = mutableMapOf<String, String?>()
    private var memoryDeliveryIssue: String? = null
    private var memoryDeliveryIssueSet = false

    private fun storedPlanIssues(context: Context): Map<String, String> = try {
        val j = JSONObject(prefs(context).getString(PLAN_ISSUES_KEY, "{}") ?: "{}")
        j.keys().asSequence().associateWith { j.getString(it) }
    } catch (e: Exception) { Log.w(TAG, "Planungsstatus unlesbar", e); emptyMap() }

    private fun planIssues(context: Context): Map<String, String> {
        val merged = storedPlanIssues(context).toMutableMap()
        memoryPlanIssues.forEach { (id, text) -> if (text == null) merged.remove(id) else merged[id] = text }
        return merged
    }

    /** Sets or clears the planning issue of exactly this alarm; other alarms keep theirs. */
    private fun setPlanIssue(context: Context, alarmId: String, text: String?) = synchronized(lock) {
        try {
            val current = storedPlanIssues(context)
            val updated = if (text == null) current - alarmId else current + (alarmId to text)
            if (updated == current || prefs(context).edit().putString(PLAN_ISSUES_KEY, JSONObject(updated).toString()).commit()) memoryPlanIssues.remove(alarmId)
            else { Log.w(TAG, "Planungsstatus nicht gespeichert"); memoryPlanIssues[alarmId] = text }
        } catch (e: Exception) {
            Log.w(TAG, "Planungsstatus nicht gespeichert", e); memoryPlanIssues[alarmId] = text
        }
    }

    private fun setDeliveryIssue(context: Context, text: String?) = synchronized(lock) {
        try {
            val edit = prefs(context).edit()
            if (text == null) edit.remove(DELIVERY_ISSUE_KEY) else edit.putString(DELIVERY_ISSUE_KEY, text)
            if (edit.commit()) { memoryDeliveryIssueSet = false; memoryDeliveryIssue = null }
            else { Log.w(TAG, "Zustellstatus nicht gespeichert"); memoryDeliveryIssueSet = true; memoryDeliveryIssue = text }
        } catch (e: Exception) {
            Log.w(TAG, "Zustellstatus nicht gespeichert", e); memoryDeliveryIssueSet = true; memoryDeliveryIssue = text
        }
    }

    /** All open problems: planning per alarm and the last delivery problem, kept apart. Never throws. */
    fun statusFehler(context: Context): List<String> = synchronized(lock) {
        try {
            val names = AlarmStore.get(context).all().associate { it.id to it.name }
            val plan = planIssues(context).mapNotNull { (id, text) -> names[id]?.let { "„$it“: $text" } }
            val delivery = if (memoryDeliveryIssueSet) memoryDeliveryIssue else prefs(context).getString(DELIVERY_ISSUE_KEY, null)
            plan + listOfNotNull(delivery)
        } catch (e: Exception) {
            Log.w(TAG, "Status nicht lesbar", e)
            listOfNotNull(memoryDeliveryIssue) + memoryPlanIssues.values.filterNotNull()
        }
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // Der Klang eines Kanals ist nach dem Anlegen unveränderlich, deshalb ein neuer, stummer Kanal.
        if (manager.getNotificationChannel(ALTER_KANAL) != null) runCatching { manager.deleteNotificationChannel(ALTER_KANAL) }
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Schlafenszeit-Erinnerung", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Zum eingestellten Vorlauf vor der berechneten Schlafenszeit. Den Klingelton wählst du in der App."
                setSound(null, null)
                enableVibration(false)
            })
    }

    /** "app" or "kanal" when Android would not show the reminder, otherwise null. */
    fun blockiert(context: Context): String? {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return "app"
        val channel = manager.getNotificationChannel(CHANNEL)
        if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return "kanal"
        return null
    }

    /** True when only inexact timing is possible, i.e. the reminder may come late. */
    fun exaktFehlt(context: Context) = Build.VERSION.SDK_INT >= 31 && !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    // ---------- planning: every operation runs under [lock] and never throws ----------
    private fun operation(context: Context, alarmId: String, extras: Intent.() -> Unit = {}) = PendingIntent.getBroadcast(
        context, 0, Intent(context, SchlafErinnerungReceiver::class.java)
            .setData(Uri.parse("wecker://schlaf/$alarmId")).putExtra("id", alarmId).apply(extras),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Cancels only the planned broadcast of this alarm. */
    private fun cancelPlan(context: Context, alarmId: String) {
        context.getSystemService(AlarmManager::class.java).cancel(operation(context, alarmId))
    }

    /**
     * Replaces the plan of one alarm from the CURRENT stored state (the argument only names the alarm), then aligns
     * visible reminders. A deleted alarm is cancelled, so a stale call can never re-register it.
     */
    fun sync(context: Context, alarm: Alarm) = sync(context, alarm.id)

    /** Wie oben, aber nur mit der Kennung: der Stand wird ohnehin frisch gelesen. */
    fun sync(context: Context, alarmId: String) = synchronized(lock) {
        try {
            val current = AlarmStore.get(context).get(alarmId)
            val vorlauf = leadMinutes(context)
            val occurrence = current?.let { SchlafPlan.next(it, System.currentTimeMillis(), enabled(context), leadMinutes = vorlauf) }
            if (occurrence == null) cancelPlan(context, alarmId)
            else {
                val manager = context.getSystemService(AlarmManager::class.java)
                val pending = operation(context, alarmId) { putExtra("wakeAt", occurrence.wakeAt); putExtra("sleep", occurrence.sleepMinutes) }
                // Without the exact-alarm grant an inexact reminder is still better than none; the settings name the possible delay.
                val zeitpunkt = occurrence.trigger(SchlafPlan.leadMs(vorlauf))
                if (exaktFehlt(context)) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeitpunkt, pending)
                else manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeitpunkt, pending)
            }
            setPlanIssue(context, alarmId, null)
        } catch (e: Exception) {
            Log.w(TAG, "Erinnerung nicht geplant für ${alarmId}", e)
            setPlanIssue(context, alarmId, "Schlafenszeit-Erinnerung nicht geplant (${e.javaClass.simpleName}).")
        }
        abgleichen(context, excludeId = null)
    }

    /** Alarm deleted or cancelled: remove its plan and take it out of visible groups. */
    fun entferneWecker(context: Context, alarmId: String) = synchronized(lock) {
        try { cancelPlan(context, alarmId) } catch (e: Exception) { Log.w(TAG, "Erinnerung nicht storniert", e) }
        setPlanIssue(context, alarmId, null)
        abgleichen(context, excludeId = alarmId)
    }

    fun syncAll(context: Context) = synchronized(lock) {
        try {
            val alarms = AlarmStore.get(context).all()
            alarms.forEach { sync(context, it) }
            // Status of alarms that no longer exist is dropped.
            val ids = alarms.map { it.id }.toSet()
            planIssues(context).keys.filter { it !in ids }.forEach { setPlanIssue(context, it, null) }
            // Also without any alarm left: visible reminders must follow the current state.
            abgleichen(context, excludeId = null)
        } catch (e: Exception) {
            Log.w(TAG, "Erinnerungen nicht synchronisiert", e)
        }
    }

    /**
     * Global switch. Returns false if it could not be stored durably; then the previous value is restored in memory too,
     * because a failed commit() still changes the in-memory preferences.
     */
    fun setEnabled(context: Context, on: Boolean): Boolean = synchronized(lock) {
        val previous = enabled(context)
        val stored = try { prefs(context).edit().putBoolean(SETTING_KEY, on).commit() }
            catch (e: Exception) { Log.w(TAG, "Schalter nicht gespeichert", e); false }
        if (!stored) {
            // enabled() reads the confirmed RAM value, so the old state holds even if this rollback fails too.
            try { prefs(context).edit().putBoolean(SETTING_KEY, previous).commit() }
            catch (e: Exception) { Log.w(TAG, "Schalter nicht zurückgesetzt", e) }
            return@synchronized false
        }
        confirmedEnabled = on
        // With the switch off every plan is cancelled and the alignment removes all visible reminders.
        syncAll(context)
        true
    }

    /**
     * Aligns every visible reminder group with the currently valid alarms: an empty group is removed, a changed one is
     * updated silently, an unchanged one is left alone (a harmless reschedule keeps it). Only our own tags are touched.
     */
    private fun abgleichen(context: Context, excludeId: String?) = synchronized(lock) {
        try {
            val manager = context.getSystemService(NotificationManager::class.java)
            val visible = manager.activeNotifications.filter { it.id == NOTIFICATION_ID && it.tag?.startsWith(TAG_PREFIX) == true }
            if (visible.isEmpty()) return@synchronized
            val now = System.currentTimeMillis()
            val setting = enabled(context)
            val alarms = AlarmStore.get(context).all().filter { it.id != excludeId }
            visible.forEach { sbn ->
                val group = sbn.tag.removePrefix(TAG_PREFIX).toLongOrNull() ?: return@forEach
                val members = alarms.mapNotNull { alarm -> SchlafPlan.inGruppe(alarm, group, now, setting)?.let { alarm to it } }
                if (members.isEmpty()) { manager.cancel(sbn.tag, sbn.id); return@forEach }
                val text = SchlafPlan.text(members.first().second.bedtime, now, members.map { (a, v) -> SchlafPlan.Eintrag(a.name, v.wakeAt, v.sleepMinutes) })
                if (sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() != text) post(context, group, text, silent = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sichtbare Erinnerungen nicht abgeglichen", e)
        }
    }

    // ---------- delivery ----------
    /** Validates the fingerprint, checks Android's blocks, marks durably, then shows the group text of all valid alarms. */
    /** Liefert true, wenn die Erinnerung neu angezeigt wurde und deshalb klingeln soll. */
    fun deliver(context: Context, alarmId: String, wakeAt: Long, sleepMinutes: Int): Boolean {
        val store = AlarmStore.get(context)
        var klingeln = false
        try {
            synchronized(lock) {
                val now = System.currentTimeMillis()
                val setting = enabled(context)
                // Gegen das mit dem aktuellen Vorlauf gültige Zeitfenster geprüft: Ausgeliefertes, das nicht mehr
                // hineinfällt, wird verworfen. Eine Vorlaufänderung allein macht eine Auslieferung nicht ungültig.
                val occurrence = SchlafPlan.gueltig(store.get(alarmId), wakeAt, sleepMinutes, now, setting, leadMinutes = leadMinutes(context)) ?: return false
                val marks = SchlafPlan.Marken.parse(store.prefs.getString(MARKS_KEY, null)).bereinigt(now)
                val memoryKey = "${occurrence.group}|${occurrence.key}"
                if (marks.enthaelt(occurrence) || memoryKey in memoryMarks) return false
                // Blocked by Android: do not pretend it was shown, and do not mark it.
                blockiert(context)?.let { reason ->
                    setDeliveryIssue(context, if (reason == "app") "Eine Schlafenszeit-Erinnerung wurde nicht angezeigt: Benachrichtigungen der App sind aus."
                        else "Eine Schlafenszeit-Erinnerung wurde nicht angezeigt: Der Kanal „Schlafenszeit-Erinnerung“ ist gesperrt.")
                    return false
                }
                val alreadyAnnounced = marks.gruppeGemeldet(occurrence) || memoryMarks.any { it.startsWith("${occurrence.group}|") }
                memoryMarks += memoryKey
                val marked = store.prefs.edit().putString(MARKS_KEY, marks.mit(occurrence).json()).commit()
                val members = store.all().mapNotNull { alarm -> SchlafPlan.inGruppe(alarm, occurrence.group, now, setting)?.let { alarm to it } }
                    .ifEmpty { listOf(store.get(alarmId)!! to occurrence) }
                val text = SchlafPlan.text(occurrence.bedtime, now, members.map { (a, v) -> SchlafPlan.Eintrag(a.name, v.wakeAt, v.sleepMinutes) })
                post(context, occurrence.group, text, silent = alreadyAnnounced)
                klingeln = !alreadyAnnounced
                setDeliveryIssue(context, if (marked) null
                    else "Die Zustellmarke einer Schlafenszeit-Erinnerung konnte nicht gespeichert werden. Nach einem Neustart kann sie erneut klingeln.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erinnerung nicht angezeigt", e)
            setDeliveryIssue(context, "Eine Schlafenszeit-Erinnerung konnte nicht angezeigt werden (${e.javaClass.simpleName}).")
        } finally {
            store.get(alarmId)?.let { sync(context, it) }
        }
        return klingeln
    }

    private fun post(context: Context, group: Long, text: String, silent: Boolean) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_wecker)
            .setContentTitle("Zeit, dich auf den Schlaf vorzubereiten").setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setAutoCancel(true).setContentIntent(open)
            .setOnlyAlertOnce(true).setSilent(silent).build()
        context.getSystemService(NotificationManager::class.java).notify("$TAG_PREFIX$group", NOTIFICATION_ID, notification)
    }
}

class SchlafErinnerungReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val klingeln = SchlafErinnerung.deliver(context, id, intent.getLongExtra("wakeAt", 0), intent.getIntExtra("sleep", 0))
        // Bei „Nicht stören“ bleibt die Erinnerung stumm; sonst klingt der gewählte Ton in der gewählten Lautstärke.
        if (klingeln && !SchlafTon.nichtStoerenAktiv(context)) {
            val auftrag = goAsync()
            SchlafTon.spielen(context.applicationContext) { runCatching { auftrag.finish() } }
        }
    }
}
