package de.frank.newskompass.data

import android.content.Context
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.Block
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.observability.KompassLog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die fertigen Ausgaben als JSON-Dateien, die Bilder daneben im Ordner `bilder/`.
 *
 * Gehalten werden die letzten [BEHALTEN] Ausgaben; ältere fliegen samt Bildern raus, damit der
 * Speicher nicht mit jedem Tag wächst.
 */
class AusgabenSpeicher(context: Context) {

    private val ordner = File(context.filesDir, "ausgaben").apply { mkdirs() }
    val bilderOrdner = File(context.filesDir, "bilder").apply { mkdirs() }

    private val _ausgaben = MutableStateFlow<List<Ausgabe>>(emptyList())

    /** Neueste zuerst. */
    val ausgaben: StateFlow<List<Ausgabe>> = _ausgaben.asStateFlow()

    suspend fun lade() = withContext(Dispatchers.IO) {
        _ausgaben.value = ladeAlle()
    }

    private fun ladeAlle(): List<Ausgabe> = (ordner.listFiles { f -> f.extension == "json" } ?: emptyArray())
        .mapNotNull { datei ->
            runCatching { ausJson(JSONObject(datei.readText())) }.onFailure {
                KompassLog.warn("AusgabenSpeicher", "lade", "Ausgabe unlesbar", mapOf("datei" to datei.name, "grund" to it.message))
            }.getOrNull()
        }
        .sortedByDescending { it.erstelltUm }

    suspend fun speichere(ausgabe: Ausgabe) = withContext(Dispatchers.IO) {
        val ziel = File(ordner, "${ausgabe.id}.json")
        val zwischen = File(ordner, "${ausgabe.id}.tmp")
        zwischen.writeText(zuJson(ausgabe).toString())
        if (!zwischen.renameTo(ziel)) {
            ziel.writeText(zwischen.readText())
            zwischen.delete()
        }
        raeumeAuf()
        _ausgaben.value = ladeAlle()
    }

    suspend fun loesche(id: String) = withContext(Dispatchers.IO) {
        File(ordner, "$id.json").delete()
        raeumeBilderAuf()
        _ausgaben.value = ladeAlle()
    }

    private fun raeumeAuf() {
        ladeAlle().drop(BEHALTEN).forEach { File(ordner, "${it.id}.json").delete() }
        raeumeBilderAuf()
    }

    /** Löscht jedes Bild, auf das keine gespeicherte Ausgabe mehr zeigt. */
    private fun raeumeBilderAuf() {
        val benutzt = ladeAlle().flatMap { a -> a.bloecke.flatMap { b -> b.meldungen.mapNotNull { it.bildDatei } } }.toSet()
        bilderOrdner.listFiles()?.forEach { if (it.name !in benutzt) it.delete() }
    }

    fun bildDatei(name: String?): File? = name?.let { File(bilderOrdner, it) }?.takeIf(File::exists)

    companion object {
        const val BEHALTEN = 20

        fun zuJson(a: Ausgabe): JSONObject = JSONObject()
            .put("id", a.id)
            .put("erstelltUm", a.erstelltUm)
            .put("slot", a.slot)
            .put("bloecke", JSONArray().apply {
                a.bloecke.forEach { b ->
                    put(
                        JSONObject()
                            .put("themaId", b.themaId)
                            .put("titel", b.titel)
                            .put("fehler", b.fehler ?: JSONObject.NULL)
                            .put("meldungen", JSONArray().apply {
                                b.meldungen.forEach { m ->
                                    put(
                                        JSONObject()
                                            .put("id", m.id)
                                            .put("titel", m.titel)
                                            .put("absaetze", JSONArray(m.absaetze))
                                            .put("quellen", JSONArray(m.quellen))
                                            .put("bild", m.bildDatei ?: JSONObject.NULL)
                                            .put("bildKi", m.bildIstKi)
                                            .put("wann", m.wann)
                                            .put("update", m.istUpdate),
                                    )
                                }
                            }),
                    )
                }
            })

        fun ausJson(j: JSONObject): Ausgabe = Ausgabe(
            id = j.getString("id"),
            erstelltUm = j.getLong("erstelltUm"),
            slot = j.optString("slot"),
            bloecke = j.getJSONArray("bloecke").let { bl ->
                (0 until bl.length()).map { i ->
                    val b = bl.getJSONObject(i)
                    Block(
                        themaId = b.optString("themaId"),
                        titel = b.optString("titel"),
                        fehler = b.optString("fehler").takeIf { !b.isNull("fehler") && it.isNotBlank() },
                        meldungen = b.getJSONArray("meldungen").let { ml ->
                            (0 until ml.length()).map { k ->
                                val m = ml.getJSONObject(k)
                                Meldung(
                                    id = m.getString("id"),
                                    titel = m.optString("titel"),
                                    absaetze = m.getJSONArray("absaetze").let { a -> (0 until a.length()).map(a::getString) },
                                    quellen = m.optJSONArray("quellen")?.let { a -> (0 until a.length()).map(a::getString) } ?: emptyList(),
                                    bildDatei = m.optString("bild").takeIf { !m.isNull("bild") && it.isNotBlank() },
                                    bildIstKi = m.optBoolean("bildKi"),
                                    wann = m.optString("wann"),
                                    istUpdate = m.optBoolean("update"),
                                )
                            }
                        },
                    )
                }
            },
        )
    }
}
