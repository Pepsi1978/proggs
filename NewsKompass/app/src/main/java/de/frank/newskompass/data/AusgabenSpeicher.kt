package de.frank.newskompass.data

import android.content.Context
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.Block
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.observability.KompassLog
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Kopfdaten einer gespeicherten Ausgabe — genug für Seitenleiste, Tagesauswahl und Aufräumen,
 * ohne die ganze Ausgabe zu laden.
 */
data class AusgabenEintrag(
    val id: String,
    val erstelltUm: Long,
    val slot: String,
    val regulaer: Boolean,
    val meldungen: Int,
    /** Meldungen aus regulären Blöcken, ohne gesprochene Fragen. */
    val regulaereMeldungen: Int,
    val fragen: Int,
    val bilder: List<String>,
    /** Änderungszeit der Datei — wächst, wenn eine Frage angehängt oder entfernt wird. */
    val stand: Long,
) {
    /** Lokaler Kalendertag, frisch aus der Zeitzone gerechnet. */
    val tag: LocalDate get() = Instant.ofEpochMilli(erstelltUm).atZone(ZoneId.systemDefault()).toLocalDate()
}

/**
 * Die fertigen Ausgaben als JSON-Dateien, die Bilder daneben im Ordner `bilder/`.
 *
 * Ein kleiner Index (`ausgaben-index.json`) hält die Kopfdaten aller Ausgaben; volle Ausgaben
 * werden erst geladen, wenn jemand sie anschaut, und bleiben in einem kleinen Zwischenspeicher.
 * Passt der Index nicht zu den Dateien oder ist er defekt, entsteht er neu aus den Dateien.
 * Ausgaben werden nie automatisch gelöscht — nur, wenn der Nutzer einen Frage-Block entfernt.
 */
class AusgabenSpeicher(context: Context) {

    private val ordner = File(context.filesDir, "ausgaben").apply { mkdirs() }
    val bilderOrdner = File(context.filesDir, "bilder").apply { mkdirs() }
    private val indexDatei = File(context.filesDir, "ausgaben-index.json")

    private val _index = MutableStateFlow<List<AusgabenEintrag>>(emptyList())

    /** Kopfdaten aller Ausgaben, neueste zuerst. */
    val index: StateFlow<List<AusgabenEintrag>> = _index.asStateFlow()

    /** Zeitplan-Lauf und Frage schreiben aus verschiedenen Aufträgen — nie gleichzeitig. */
    private val sperre = Mutex()

    /** Erst wenn der Index gegen die Dateien geprüft ist, darf anhand seiner aufgeräumt werden. */
    @Volatile private var geprueft = false

    /** Dateien, die sich nicht lesen ließen — ihre Bilder sind unbekannt, also wird dann nicht aufgeräumt. */
    @Volatile private var unlesbar: Set<String> = emptySet()

    private val zwischenspeicher = object : LinkedHashMap<String, Ausgabe>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Ausgabe>?) = size > ZWISCHENSPEICHER
    }

    suspend fun lade() = sperre.withLock { withContext(Dispatchers.IO) { pruefeIndex(erzwingen = true) } }

    /** Wartet, bis der Index einmal gegen die Dateien geprüft ist. */
    suspend fun bereit() {
        if (!geprueft) sperre.withLock { withContext(Dispatchers.IO) { pruefeIndex(erzwingen = false) } }
    }

    /** Lädt eine ganze Ausgabe; [merken] = false lässt den Zwischenspeicher für die Anzeige frei. */
    suspend fun ausgabe(id: String, merken: Boolean = true): Ausgabe? {
        synchronized(zwischenspeicher) { zwischenspeicher[id] }?.let { return it }
        return withContext(Dispatchers.IO) { leseDatei(id) }?.also { a ->
            if (merken) synchronized(zwischenspeicher) { zwischenspeicher[id] = a }
        }
    }

    /** Alle Ausgaben, die seit [zeit] entstanden sind, neueste zuerst. */
    suspend fun ausgabenSeit(zeit: Long): List<Ausgabe> {
        bereit()
        return _index.value.filter { it.erstelltUm >= zeit }.mapNotNull { ausgabe(it.id) }
    }

    /** Die neueste Ausgabe überhaupt. */
    suspend fun neueste(): Ausgabe? {
        bereit()
        return _index.value.firstOrNull()?.let { ausgabe(it.id) }
    }

    suspend fun speichere(ausgabe: Ausgabe) = sperre.withLock { schreibe(ausgabe) }

    /**
     * Hängt einen Frage-Block unten an die neueste Ausgabe, aber nur, wenn sie vom selben lokalen
     * Kalendertag stammt. Sonst entsteht eine kleine Ausgabe nur mit dieser Frage — eine Frage
     * um 4 Uhr früh landet so nicht im Vortag. Liefert die Ausgabe, in der der Block jetzt steht.
     */
    suspend fun haengeAn(block: Block, jetzt: Long): Ausgabe = sperre.withLock {
        withContext(Dispatchers.IO) { pruefeIndex(erzwingen = false) }
        val heute = tagVon(jetzt)
        val kandidat = _index.value.firstOrNull()?.takeIf { it.tag == heute }
        val ziel = kandidat?.let { withContext(Dispatchers.IO) { leseDatei(it.id) } }
        val neu = ziel?.copy(bloecke = ziel.bloecke + block)
            ?: Ausgabe(id = "a$jetzt", erstelltUm = jetzt, slot = "Deine Fragen", bloecke = listOf(block))
        schreibe(neu)
        neu
    }

    /** Nimmt einen Frage-Block wieder heraus; bleibt die Ausgabe leer, verschwindet sie ganz. */
    suspend fun entferneBlock(ausgabeId: String, themaId: String) = sperre.withLock {
        val ausgabe = withContext(Dispatchers.IO) { leseDatei(ausgabeId) } ?: return@withLock
        val rest = ausgabe.bloecke.filterNot { it.themaId == themaId }
        if (rest.isEmpty()) loescheOhneSperre(ausgabeId) else schreibe(ausgabe.copy(bloecke = rest))
    }

    private suspend fun schreibe(ausgabe: Ausgabe) = withContext(Dispatchers.IO) {
        // Vor dem ersten Schreiben den Index prüfen, sonst würde ein unvollständiger Index gespeichert.
        pruefeIndex(erzwingen = false)
        val ziel = File(ordner, "${ausgabe.id}.json")
        val zwischen = File(ordner, "${ausgabe.id}.tmp")
        zwischen.writeText(zuJson(ausgabe).toString())
        if (!zwischen.renameTo(ziel)) {
            ziel.writeText(zwischen.readText())
            zwischen.delete()
        }
        synchronized(zwischenspeicher) { zwischenspeicher[ausgabe.id] = ausgabe }
        setzeIndex(_index.value.filterNot { it.id == ausgabe.id } + eintragAus(ausgabe, ziel.lastModified()))
        raeumeBilderAuf()
    }

    suspend fun loesche(id: String) = sperre.withLock { loescheOhneSperre(id) }

    private suspend fun loescheOhneSperre(id: String) = withContext(Dispatchers.IO) {
        pruefeIndex(erzwingen = false)
        File(ordner, "$id.json").delete()
        synchronized(zwischenspeicher) { zwischenspeicher.remove(id) }
        setzeIndex(_index.value.filterNot { it.id == id })
        raeumeBilderAuf()
    }

    // --- Index ---------------------------------------------------------------------------------

    /**
     * Liest den gespeicherten Index und prüft ihn gegen die Dateien: dieselben Ausgaben, dieselben
     * Änderungszeiten. Weicht etwas ab oder ist er unlesbar, entsteht er neu aus den Dateien —
     * die Dateien selbst bleiben dabei unangetastet.
     */
    private fun pruefeIndex(erzwingen: Boolean) {
        if (geprueft && !erzwingen) return
        val dateien = (ordner.listFiles { f -> f.extension == "json" } ?: emptyArray()).associateBy { it.nameWithoutExtension }
        val gespeichert = runCatching { leseIndexDatei() }.onFailure {
            if (indexDatei.exists()) KompassLog.warn("AusgabenSpeicher", "pruefeIndex", "Index unlesbar, baue neu", mapOf("grund" to it.message))
        }.getOrNull()
        val passt = gespeichert != null &&
            (gespeichert.first.map { it.id } + gespeichert.second).toSet() == dateien.keys &&
            gespeichert.first.all { dateien[it.id]?.lastModified() == it.stand }
        if (passt) {
            _index.value = gespeichert!!.first.sortedByDescending { it.erstelltUm }
            unlesbar = gespeichert.second
        } else {
            val defekt = mutableSetOf<String>()
            val neu = dateien.values.mapNotNull { datei ->
                runCatching { eintragAus(ausJson(JSONObject(datei.readText())), datei.lastModified()) }.onFailure {
                    defekt += datei.nameWithoutExtension
                    KompassLog.warn("AusgabenSpeicher", "pruefeIndex", "Ausgabe unlesbar", mapOf("datei" to datei.name, "grund" to it.message))
                }.getOrNull()
            }
            unlesbar = defekt
            setzeIndex(neu)
            KompassLog.info("AusgabenSpeicher", "pruefeIndex", "Index aus Dateien aufgebaut", mapOf("ausgaben" to neu.size, "unlesbar" to defekt.size))
        }
        geprueft = true
    }

    /** Setzt den Index im Speicher und schreibt ihn atomar: erst in eine Zwischendatei, dann umbenennen. */
    private fun setzeIndex(eintraege: List<AusgabenEintrag>) {
        val sortiert = eintraege.sortedByDescending { it.erstelltUm }
        _index.value = sortiert
        val json = JSONObject()
            .put("format", 1)
            .put("unlesbar", JSONArray(unlesbar.toList()))
            .put("eintraege", JSONArray().apply {
                sortiert.forEach { e ->
                    put(
                        JSONObject().put("id", e.id).put("erstelltUm", e.erstelltUm).put("slot", e.slot)
                            .put("regulaer", e.regulaer).put("meldungen", e.meldungen)
                            .put("regulaereMeldungen", e.regulaereMeldungen).put("fragen", e.fragen)
                            .put("bilder", JSONArray(e.bilder)).put("stand", e.stand),
                    )
                }
            })
        runCatching {
            val zwischen = File(indexDatei.parentFile, "${indexDatei.name}.tmp")
            zwischen.writeText(json.toString())
            if (!zwischen.renameTo(indexDatei)) {
                indexDatei.writeText(zwischen.readText())
                zwischen.delete()
            }
        }.onFailure {
            // Kein Beinbruch: Beim nächsten Start passt der Index nicht und entsteht neu.
            KompassLog.warn("AusgabenSpeicher", "setzeIndex", "Index nicht geschrieben", mapOf("grund" to it.message))
        }
    }

    private fun leseIndexDatei(): Pair<List<AusgabenEintrag>, Set<String>> {
        val j = JSONObject(indexDatei.readText())
        val liste = j.getJSONArray("eintraege")
        val eintraege = (0 until liste.length()).map { i ->
            val e = liste.getJSONObject(i)
            val bilder = e.getJSONArray("bilder")
            AusgabenEintrag(
                id = e.getString("id"),
                erstelltUm = e.getLong("erstelltUm"),
                slot = e.optString("slot"),
                regulaer = e.getBoolean("regulaer"),
                meldungen = e.getInt("meldungen"),
                // Fehlt das Feld (älterer Index), scheitert das Lesen und der Index entsteht neu.
                regulaereMeldungen = e.getInt("regulaereMeldungen"),
                fragen = e.optInt("fragen"),
                bilder = (0 until bilder.length()).map(bilder::getString),
                stand = e.getLong("stand"),
            )
        }
        val defekt = j.optJSONArray("unlesbar")?.let { a -> (0 until a.length()).map(a::getString).toSet() } ?: emptySet()
        return eintraege to defekt
    }

    private fun leseDatei(id: String): Ausgabe? {
        val datei = File(ordner, "$id.json")
        if (!datei.exists()) return null
        return runCatching { ausJson(JSONObject(datei.readText())) }.onFailure {
            KompassLog.warn("AusgabenSpeicher", "leseDatei", "Ausgabe unlesbar", mapOf("datei" to datei.name, "grund" to it.message))
        }.getOrNull()
    }

    private fun eintragAus(a: Ausgabe, stand: Long) = AusgabenEintrag(
        id = a.id,
        erstelltUm = a.erstelltUm,
        slot = a.slot,
        regulaer = a.istRegulaer,
        meldungen = a.bloecke.sumOf { it.meldungen.size },
        regulaereMeldungen = a.bloecke.filter { it.frage == null }.sumOf { it.meldungen.size },
        fragen = a.bloecke.count { it.frage != null },
        bilder = a.bloecke.flatMap { b -> b.meldungen.mapNotNull { it.bildDatei } },
        stand = stand,
    )

    /**
     * Löscht jedes Bild, auf das keine gespeicherte Ausgabe mehr zeigt — nur mit geprüftem Index
     * und nur, wenn jede Ausgabe lesbar war.
     *
     * Junge Bilder bleiben: Ein Zeitplan-Lauf und eine gesprochene Frage können gleichzeitig
     * laufen, und die Bilder des einen liegen schon im Ordner, bevor seine Ausgabe gespeichert ist.
     */
    private fun raeumeBilderAuf() {
        if (!geprueft || unlesbar.isNotEmpty()) return
        val benutzt = _index.value.flatMap { it.bilder }.toSet()
        val grenze = System.currentTimeMillis() - BILD_SCHONFRIST_MS
        bilderOrdner.listFiles()?.forEach { if (it.name !in benutzt && it.lastModified() < grenze) it.delete() }
    }

    fun bildDatei(name: String?): File? = name?.let { File(bilderOrdner, it) }?.takeIf(File::exists)

    companion object {
        private const val ZWISCHENSPEICHER = 8
        private const val BILD_SCHONFRIST_MS = 2 * 3_600_000L

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
                            .put("frage", b.frage ?: JSONObject.NULL)
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
                        frage = b.optString("frage").takeIf { !b.isNull("frage") && it.isNotBlank() },
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

        fun tagVon(zeit: Long): LocalDate = Instant.ofEpochMilli(zeit).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
