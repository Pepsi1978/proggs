package de.frank.jarvis.atem

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Ein Atemmuster als Karte: Zeiten in Zehntelsekunden (4,2 s = 42), damit die 0,1-Schritte exakt bleiben.
 * Sätze: je Zeile ein Satz; mehrere Zeilen wechseln sich von Zyklus zu Zyklus ab.
 */
data class AtemProfil(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val ein: Int = 40,
    val halt1: Int = 0,
    val aus: Int = 60,
    val halt2: Int = 0,
    val zyklen: Int = 10,
    val farbe: Long = AtemFarben.KARTEN[0],
    val vorlesen: Boolean = true,
    val geraeusch: Boolean = true,
    val intro: Boolean = true,
    val outro: Boolean = true,
    val saetzeAn: Boolean = false,
    val satzEin: String = "",
    val satzAus: String = "",
) {
    val zyklusZehntel: Int get() = ein + halt1 + aus + halt2
    val dauerMs: Long get() = zyklusZehntel * 100L * zyklen
    val saetzeEin: List<String> get() = satzEin.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val saetzeAus: List<String> get() = satzAus.lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun json(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("ein", ein).put("halt1", halt1).put("aus", aus).put("halt2", halt2)
        .put("zyklen", zyklen).put("farbe", farbe).put("vorlesen", vorlesen).put("geraeusch", geraeusch)
        .put("intro", intro).put("outro", outro).put("saetzeAn", saetzeAn).put("satzEin", satzEin).put("satzAus", satzAus)

    companion object {
        fun aus(o: JSONObject) = AtemProfil(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() }, name = o.optString("name"),
            ein = o.optInt("ein", 40), halt1 = o.optInt("halt1", 0), aus = o.optInt("aus", 60), halt2 = o.optInt("halt2", 0),
            zyklen = o.optInt("zyklen", 10).coerceAtLeast(1), farbe = o.optLong("farbe", AtemFarben.KARTEN[0]),
            vorlesen = o.optBoolean("vorlesen", true), geraeusch = o.optBoolean("geraeusch", true),
            intro = o.optBoolean("intro", true), outro = o.optBoolean("outro", true),
            saetzeAn = o.optBoolean("saetzeAn", false), satzEin = o.optString("satzEin"), satzAus = o.optString("satzAus"),
        )
    }
}

object AtemFarben {
    /** Kartenfarben zur Wahl (ARGB). */
    val KARTEN = listOf(0xFF3DDC97, 0xFF4FC8FF, 0xFFB98CFF, 0xFFFF8A3D, 0xFFFF6B9A, 0xFFFFC94A, 0xFF2EC4B6, 0xFF7A8CFF, 0xFFE76F51, 0xFF9BE15D)
    const val EIN = 0xFF3DDC97
    const val HALTEN = 0xFFFFD25A
    const val AUS = 0xFFFF8A3D
}

/** Zeit in Zehntelsekunden als „4,2“ bzw. „5“. */
fun zehntelText(z: Int): String = if (z % 10 == 0) "${z / 10}" else "${z / 10},${z % 10}"

fun dauerText(ms: Long): String {
    val s = (ms + 500) / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

/** Profile in files/atem/profile.json; beim ersten Öffnen mit Franks Startprofilen gefüllt. */
object AtemProfile {
    private val _stand = MutableStateFlow<List<AtemProfil>>(emptyList())
    val stand: StateFlow<List<AtemProfil>> = _stand
    private val _hinweis = MutableStateFlow<String?>(null)
    /** Hinweis für die Übersicht, etwa wenn eine beschädigte Datei beiseitegelegt wurde. */
    val hinweis: StateFlow<String?> = _hinweis
    @Volatile private var geladen = false
    /** Lesen, Ändern und Schreiben nacheinander: Schnelles Löschen und Speichern überschreiben sich sonst gegenseitig. */
    private val sperre = Mutex()

    private fun datei(context: Context) = File(context.filesDir, "atem/profile.json")

    suspend fun sicher(context: Context) {
        if (geladen) return
        sperre.withLock { laden(context) }
    }

    private suspend fun laden(context: Context) {
        if (geladen) return
        withContext(Dispatchers.IO) {
            val d = datei(context)
            val liste = if (d.exists()) runCatching {
                val a = JSONArray(d.readText())
                (0 until a.length()).map { AtemProfil.aus(a.getJSONObject(it)) }
            }.getOrElse {
                // Unlesbare Datei nie überschreiben: beiseitelegen, damit die Profile darin von Hand zu retten bleiben.
                val sicherung = File(d.parentFile, "profile.defekt-${System.currentTimeMillis()}.json")
                d.renameTo(sicherung)
                _hinweis.value = "Die Profildatei war beschädigt und liegt als ${sicherung.name} beiseite. Neue Profile werden in einer frischen Datei gespeichert."
                emptyList()
            } else startProfile().also { liste ->
                // Speicher voll: die Startprofile trotzdem zeigen, statt Jarvis abstürzen zu lassen.
                runCatching { schreibe(context, liste) }.onFailure { _hinweis.value = "Die Startprofile ließen sich nicht speichern (Speicher voll?)." }
            }
            _stand.value = liste
            geladen = true
        }
    }

    private fun startProfile() = listOf(
        AtemProfil(name = "Baroreflex", ein = 50, halt1 = 0, aus = 60, halt2 = 0, zyklen = 55, farbe = 0xFF4FC8FF),
        AtemProfil(name = "4-7-8", ein = 40, halt1 = 70, aus = 80, halt2 = 0, zyklen = 8, farbe = 0xFFB98CFF),
        AtemProfil(name = "Whoop", ein = 90, halt1 = 10, aus = 200, halt2 = 0, zyklen = 50, farbe = 0xFF3DDC97),
    )

    private fun schreibe(context: Context, liste: List<AtemProfil>) {
        val d = datei(context).apply { parentFile?.mkdirs() }
        val tmp = File(d.parentFile, "profile.json.tmp")
        tmp.writeText(JSONArray().apply { liste.forEach { put(it.json()) } }.toString(2))
        if (!tmp.renameTo(d)) { d.delete(); tmp.renameTo(d) }
    }

    suspend fun speichern(context: Context, p: AtemProfil) = sperre.withLock {
        laden(context)
        withContext(Dispatchers.IO) { schreibeNeu(context, p) }
    }

    private fun schreibeNeu(context: Context, p: AtemProfil) {
        val alt = _stand.value
        val neu = if (alt.any { it.id == p.id }) alt.map { if (it.id == p.id) p else it } else alt + p
        schreibe(context, neu); _stand.value = neu
    }

    suspend fun loeschen(context: Context, id: String) = sperre.withLock {
        laden(context)
        withContext(Dispatchers.IO) { entferne(context, id) }
    }

    private fun entferne(context: Context, id: String) {
        val neu = _stand.value.filterNot { it.id == id }
        schreibe(context, neu); _stand.value = neu
    }
}
