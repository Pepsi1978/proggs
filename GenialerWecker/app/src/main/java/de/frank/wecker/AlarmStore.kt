package de.frank.wecker

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File

/** Weckdaten und fertiges Audio sind auch vor der ersten Entsperrung nach einem Neustart verfügbar. */
class AlarmStore private constructor(context: Context) {
    val context: Context = context.createDeviceProtectedStorageContext()
    val prefs = this.context.getSharedPreferences("alarms_v1", Context.MODE_PRIVATE)
    val files: File = File(this.context.filesDir, "alarm_audio").apply { mkdirs() }
    private val state = MutableStateFlow(read())
    val alarms = state.asStateFlow()
    private val issueState = MutableStateFlow(readIssues())
    /** Sichtbare Zuverlässigkeitshinweise je Wecker, z. B. eine fehlgeschlagene Folgeplanung. */
    val issues = issueState.asStateFlow()
    /**
     * Ein einzelner unlesbarer Eintrag darf weder den App-Start blockieren noch die übrigen Wecker
     * verdecken. Vor dem ersten Überschreiben wird der Rohstand einmalig gesichert.
     */
    private fun read(): List<Alarm> {
        val raw = prefs.getString("alarms", "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { backupRaw(raw, it); return emptyList() }
        return (0 until array.length()).mapNotNull { index ->
            runCatching { Alarm.from(array.getJSONObject(index)) }
                .onFailure { backupRaw(raw, it) }.getOrNull()
        }
    }
    private fun backupRaw(raw: String, error: Throwable) {
        android.util.Log.e("WeckerStore", "Unlesbarer Weckereintrag; Rohstand wird gesichert", error)
        if (!prefs.contains("alarms_unreadable_backup")) prefs.edit().putString("alarms_unreadable_backup", raw).commit()
    }
    @Synchronized fun all() = state.value
    @Synchronized fun get(id: String) = state.value.find { it.id == id }
    @Synchronized fun insertNew(alarm: Alarm) {
        check(get(alarm.id) == null) { "Dieser neue Wecker hat bereits eine Kennung. Öffne bitte einen neuen Entwurf." }
        put(alarm)
    }
    @Synchronized fun put(alarm: Alarm) {
        val list = state.value.toMutableList()
        val index = list.indexOfFirst { it.id == alarm.id }
        if (index < 0) list.add(alarm) else list[index] = alarm
        write(list)
    }
    @Synchronized fun update(id: String, transform: (Alarm) -> Alarm): Alarm? = get(id)?.let { transform(it).also(::put) }
    @Synchronized fun delete(id: String) { write(state.value.filterNot { it.id == id }); issue(id, null) }
    /** Rejected writes are rolled back in the preferences RAM too, so a later commit of another key cannot persist them. */
    private fun commitOrRestore(keys: Collection<String>, change: (android.content.SharedPreferences.Editor) -> Unit): Boolean =
        PrefsCommit.commitOrRestore(prefs, keys, { android.util.Log.w("WeckerStore", "Abgewiesene Änderung nicht zurückgesetzt", it) }, change)

    private fun write(list: List<Alarm>) {
        check(commitOrRestore(listOf("alarms")) { it.putString("alarms", JSONArray(list.map { a -> a.json() }).toString()) }) {
            "Der Wecker konnte nicht gespeichert werden. Prüfe den freien Speicher."
        }
        state.value = list
    }
    @Synchronized fun ringing(): List<String> = ringingEntries().map { it.id }
    @Synchronized fun ringingEntries(): List<RingEntry> = RingEntry.parse(prefs.getString("ringing", ""), prefs.getString("ringingMeta", "{}"))
    @Synchronized fun ringing(ids: List<String>) {
        val kept = ringingEntries().associateBy { it.id }
        writeRinging(ids.distinct().map { kept[it] ?: RingEntry(it) })
    }
    @Synchronized fun writeRinging(entries: List<RingEntry>) {
        failRingingWriteForTestId?.let { testId ->
            // Only a write that changes the test alarm's own entry fails; other alarms are never affected.
            if (ringingEntries().any { it.id == testId } != entries.any { it.id == testId }) throw IllegalStateException("Testfehler beim Speichern des Klingelauftrags")
        }
        check(commitOrRestore(listOf("ringing", "ringingMeta")) { it.putString("ringing", RingEntry.ids(entries)).putString("ringingMeta", RingEntry.meta(entries)) })
    }

    /**
     * Nimmt ein Vorkommen atomar an: Folgezustand und Klingelauftrag landen in EINEM Commit. Ein
     * Prozessabbruch kann so nie einen weitergerückten Wecker ohne Klingelauftrag hinterlassen.
     */
    @Synchronized fun claim(id: String, decide: (Alarm?, List<RingEntry>) -> AlarmClaim.Decision): AlarmClaim.Decision {
        val ringing = ringingEntries()
        val decision = decide(get(id), ringing)
        if (decision !is AlarmClaim.Accepted) return decision
        val entries = ringing.filterNot { it.id == id } + decision.entry
        val list = decision.next?.let { next -> state.value.map { if (it.id == id) next else it } }
        val issues = if (decision.error.isNotBlank()) issuesWith(id, decision.error).toString() else null
        // One commit as before; on failure exactly the touched keys are rolled back.
        val keys = listOfNotNull("ringing", "ringingMeta", if (list != null) "alarms" else null, if (issues != null) "alarmIssues" else null)
        check(commitOrRestore(keys) { edit ->
            edit.putString("ringing", RingEntry.ids(entries)).putString("ringingMeta", RingEntry.meta(entries))
            if (list != null) edit.putString("alarms", JSONArray(list.map { it.json() }).toString())
            if (issues != null) edit.putString("alarmIssues", issues)
        }) { "Der Weckauftrag konnte nicht gespeichert werden." }
        if (list != null) state.value = list
        if (decision.error.isNotBlank()) issueState.value = issueState.value + (id to decision.error)
        return decision
    }

    private fun issuesWith(id: String, message: String?) = org.json.JSONObject(prefs.getString("alarmIssues", "{}") ?: "{}").apply {
        if (message == null) remove(id) else put(id, message)
    }
    private fun readIssues(): Map<String, String> = runCatching {
        val json = org.json.JSONObject(prefs.getString("alarmIssues", "{}") ?: "{}")
        json.keys().asSequence().associateWith { json.getString(it) }
    }.getOrDefault(emptyMap())
    /** Setzt oder löscht einen Hinweis. Schreibfehler dürfen den Weckpfad nie unterbrechen. */
    @Synchronized fun issue(id: String, message: String?) {
        if (issueState.value[id] == message) return
        runCatching { commitOrRestore(listOf("alarmIssues")) { it.putString("alarmIssues", issuesWith(id, message).toString()) } }
        // Best effort as before: built from the visible state, so a hint that could not be stored (and was rolled back in the
        // preferences RAM) stays shown, also when another alarm's hint changes later.
        issueState.value = issueState.value.let { if (message == null) it - id else it + (id to message) }
    }
    companion object {
        @androidx.annotation.VisibleForTesting @Volatile var failRingingWriteForTestId: String? = null
        @Volatile private var instance: AlarmStore? = null
        fun get(context: Context): AlarmStore = instance ?: synchronized(this) {
            instance ?: AlarmStore(context.applicationContext).also { instance = it }
        }
    }
}
