package de.frank.wecker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Eine offene Aufgabe aus der App „Aufgaben“; [zeit] ist "HH:MM" oder leer. */
data class OpenTask(val id: Long, val zeit: String, val titel: String, val vorlesetext: String) {
    /** Was beim Wecken gesprochen wird: zuerst die Uhrzeit, dann der Titel. */
    val gesprochen: String get() = vorlesetext.ifBlank { if (zeit.isBlank()) "$titel." else "Um $zeit Uhr: $titel." }
}

/**
 * Nur lesende Brücke zur App „Aufgaben“ (de.frank.aufgaben), Authority `de.frank.aufgaben.wecker`.
 * Gelesen werden die Aufgaben des Klingeltags: Wird der Wecker abends gestellt, sind das die
 * Aufgaben aus „Morgen“. Sortierung liefert die Aufgaben-App (Termine nach Uhrzeit, dann Priorität).
 */
class TasksBridge(private val context: Context) {
    private val store = AlarmStore.get(context)

    /**
     * Liest die offenen Aufgaben von [tag]. Mit [abMinute] (Weckzeit in Minuten ab Mitternacht) fallen Aufgaben dieses
     * Tages weg, deren Uhrzeit vor der Weckzeit liegt; Aufgaben ohne Uhrzeit und überfällige aus früheren Tagen bleiben.
     */
    suspend fun refresh(tag: LocalDate, abMinute: Int? = null): List<OpenTask> = withContext(Dispatchers.IO) {
        val rows: List<OpenTask> = context.contentResolver.query(uriFuer(tag), null, null, null, null)?.use { cursor ->
            val id = cursor.getColumnIndex("_id")
            val zeit = cursor.getColumnIndex("zeit")
            val titel = cursor.getColumnIndex("titel")
            val vorlesen = cursor.getColumnIndex("vorlesetext")
            val uhrzeit = cursor.getColumnIndex("uhrzeit")
            val aufgabenTag = cursor.getColumnIndex("tag")
            buildList {
                while (cursor.moveToNext()) {
                    val t = if (titel >= 0) cursor.getString(titel).orEmpty().trim() else ""
                    if (t.isBlank()) continue
                    val minuten = if (uhrzeit >= 0 && !cursor.isNull(uhrzeit)) cursor.getInt(uhrzeit) else -1
                    val vonTag = if (aufgabenTag >= 0 && !cursor.isNull(aufgabenTag)) cursor.getLong(aufgabenTag) else tag.toEpochDay()
                    if (!nachWeckzeit(minuten, vonTag, tag.toEpochDay(), abMinute)) continue
                    add(OpenTask(if (id >= 0) cursor.getLong(id) else 0L, if (zeit >= 0) cursor.getString(zeit).orEmpty() else "", t,
                        if (vorlesen >= 0) cursor.getString(vorlesen).orEmpty().trim() else ""))
                }
            }
        } ?: error("Die App „Aufgaben“ ist nicht installiert oder braucht das Brücken-Update.")
        check(store.prefs.edit().putString("tasks", JSONArray(rows.map {
            JSONObject().put("id", it.id).put("zeit", it.zeit).put("titel", it.titel).put("vorlesetext", it.vorlesetext)
        }).toString()).putString("tasksDay", tag.toString()).putLong("tasksAt", System.currentTimeMillis()).commit())
        rows
    }

    fun cached(): List<OpenTask> = runCatching {
        val array = JSONArray(store.prefs.getString("tasks", "[]"))
        (0 until array.length()).map { array.getJSONObject(it).let { j ->
            OpenTask(j.optLong("id"), j.optString("zeit"), j.optString("titel"), j.optString("vorlesetext"))
        } }
    }.getOrDefault(emptyList())

    /** Der Tag, für den [cached] gelesen wurde, oder null. */
    fun cachedDay(): LocalDate? = store.prefs.getString("tasksDay", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    companion object {
        const val AUTHORITY = "de.frank.aufgaben.wecker"
        fun uriFuer(tag: LocalDate): Uri = Uri.parse("content://$AUTHORITY/tag/${tag.toEpochDay()}")

        /**
         * Der Kalendertag, an dem der Wecker als Nächstes klingelt, in lokaler Zeit. Ohne künftigen
         * geplanten Termin (ausgeschaltet, neu) wird er aus der Weckzeit berechnet; scheitert auch das, gilt morgen.
         * [ausPlan] rechnet immer aus Uhrzeit und Wiederholung, etwa für den noch nicht gespeicherten Entwurf im Editor.
         */
        fun klingeltag(alarm: Alarm, ausPlan: Boolean = false): LocalDate {
            val at = alarm.nextAt.takeIf { !ausPlan && it > System.currentTimeMillis() } ?: runCatching { AlarmTime.nextRespectingSkip(alarm) }.getOrDefault(0L)
            return if (at > 0) AlarmTime.localDate(at) else LocalDate.now().plusDays(1)
        }

        /** Weckzeit des Weckers in Minuten ab Mitternacht, die Grenze für [nachWeckzeit]. */
        fun weckMinute(alarm: Alarm): Int = alarm.hour * 60 + alarm.minute

        /**
         * Ob eine Aufgabe noch vorgelesen wird: ohne Grenze, ohne Uhrzeit ([minuten] < 0) oder überfällig aus einem früheren
         * Tag immer; sonst nur, wenn ihre Uhrzeit gleich der Weckzeit [abMinute] ist oder danach liegt.
         */
        fun nachWeckzeit(minuten: Int, aufgabenTag: Long, klingeltag: Long, abMinute: Int?): Boolean =
            abMinute == null || minuten < 0 || aufgabenTag != klingeltag || minuten >= abMinute
    }
}

/**
 * Die Aufgaben-App meldet jede Änderung mit `de.frank.aufgaben.AUFGABEN_GEAENDERT` (an dieses Paket
 * adressiert, ohne Berechtigung). Absichtlich ohne Schutz exportiert: Der Empfang stößt nur die
 * idempotente Audio-Vorbereitung an und liest oder verändert selbst keine Daten.
 */
class TasksChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) PreparationWorker.enqueue(context)
    }
    companion object { const val ACTION = "de.frank.aufgaben.AUFGABEN_GEAENDERT" }
}
