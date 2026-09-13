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
    private fun read(): List<Alarm> {
        val array = JSONArray(prefs.getString("alarms", "[]"))
        return (0 until array.length()).map { Alarm.from(array.getJSONObject(it)) }
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
    @Synchronized fun delete(id: String) = write(state.value.filterNot { it.id == id })
    private fun write(list: List<Alarm>) {
        check(prefs.edit().putString("alarms", JSONArray(list.map { it.json() }).toString()).commit()) {
            "Der Wecker konnte nicht gespeichert werden. Prüfe den freien Speicher."
        }
        state.value = list
    }
    @Synchronized fun ringing(): List<String> = prefs.getString("ringing", "").orEmpty().split(',').filter { it.isNotBlank() }
    @Synchronized fun ringing(ids: List<String>) {
        check(prefs.edit().putString("ringing", ids.distinct().joinToString(",")).commit())
    }
    companion object {
        @Volatile private var instance: AlarmStore? = null
        fun get(context: Context): AlarmStore = instance ?: synchronized(this) {
            instance ?: AlarmStore(context.applicationContext).also { instance = it }
        }
    }
}
