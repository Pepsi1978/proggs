package de.frank.jarvis.faehigkeit

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Victron-Solaranlage: BMV-712 Smart (Ladezustand, Spannung, Strom) und SmartSolar-Laderegler (Solarleistung, Tagesertrag).
 * Beide senden ihre Werte verschlüsselt im Bluetooth-Werbepaket („Instant Readout“, Hersteller 0x02E1, AES-CTR).
 * Ablauf: erst selbst per Bluetooth lauschen (bis 8 s); für jedes Gerät gilt danach der jüngste Wert aus drei Quellen: direkt gelesen,
 * vom Rechner in Reichweite (meldet jede Minute an den Relay, werkzeuge/pc-bruecke/jarvis_victron.py, GET /geraet/victron) oder der
 * zuletzt hier gespeicherte. Jeder Wert trägt seine eigene Messzeit („zeit“, Sekunden seit 1970); das Alter der Antwort ist das des ältesten.
 * Schlüssel: files/victron.json (aus ~/SK/Victron per werkzeuge/zugang-aufs-handy.ps1), nie im APK und nicht in der Datensicherung.
 */
class VictronFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "victron"
    override val name = "Solarbatterie"
    override val beschreibung = "Ladezustand, Spannung und Strom der Victron-Batterie und die Leistung des Solarladereglers."
    override val hinweise =
        "Fragt Frank nach seiner Batterie, Solarbatterie, Victron-Batterie, dem Batteriespeicher oder der Solaranlage („Wie voll ist meine Batterie?“, " +
            "„Welche Spannung hat die Batterie?“, „Wie viel Sonne kommt rein?“), nutze batterie_lesen. Antworte kurz: Ladezustand in Prozent, Spannung in Volt, " +
            "ob gerade geladen oder entladen wird, bei Sonne die Solarleistung. Nenne das Alter des Werts nur, wenn er älter als ein paar Minuten ist."

    private val e get() = Einstellungen.get(context)
    private val schluesselDatei get() = File(context.filesDir, "victron.json")
    private val letzterDatei get() = File(context.filesDir, "victron-letzter.json")

    override fun stoerung(): String? = if (!schluesselDatei.exists()) "Die Victron-Schlüssel fehlen auf diesem Gerät (werkzeuge/zugang-aufs-handy.ps1 ausführen)." else null

    override val werkzeuge = listOf(
        Werkzeug(
            name = "batterie_lesen",
            titel = "Solarbatterie lesen",
            beschreibung = "Liest Franks Victron-Solarbatterie (BMV-712: Ladezustand %, Spannung V, Strom A, Restlaufzeit) und den Solarladeregler " +
                "(Solarleistung W, Ertrag heute kWh, Ladephase). Erst direkt per Bluetooth, unterwegs über den Rechner zu Hause.",
            schema = JSONObject().put("type", "object").put("properties", JSONObject()),
            nurLesen = true,
            taetigkeit = "liest die Solarbatterie",
        ) { abrufen().let { s -> if (s.werte.isEmpty()) Ergebnis(s.fehler ?: "Keine Werte.", fehler = true) else Ergebnis(antwort(s), struktur = s.alsJson()) } },
    )

    private class Geraetschluessel(val name: String, val mac: String, val schluessel: ByteArray)

    /** Jedes Gerät einzeln: ein kaputter Eintrag (Schlüssel nicht 32 Hex-Zeichen) fällt allein weg. */
    private fun schluessel(): List<Geraetschluessel> {
        val liste = runCatching { JSONObject(schluesselDatei.readText()).getJSONArray("geraete") }.getOrNull() ?: return emptyList()
        return (0 until liste.length()).mapNotNull { i ->
            runCatching {
                val g = liste.getJSONObject(i)
                val hex = g.getString("schluessel").trim()
                require(hex.length == 32) { "Schlüssel nicht 32 Zeichen" }
                Geraetschluessel(g.optString("name", "Gerät ${i + 1}"), g.getString("mac").uppercase(Locale.ROOT),
                    hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
            }.onFailure { Logbuch.fehler("Victron: Eintrag ${i + 1} in victron.json unbrauchbar", null, it.javaClass.simpleName) }.getOrNull()
        }
    }

    /** Ein Stand: Werte je Art („batteriemonitor“, „laderegler“), jeder mit eigener Messzeit „zeit“. */
    data class Stand(val werte: Map<String, JSONObject>, val quelle: String, val fehler: String? = null) {
        /** Messzeit des ältesten Werts (Sekunden seit 1970), 0 ohne Werte. */
        val zeit: Long get() = werte.values.minOfOrNull { it.optLong("zeit") } ?: 0L
        fun alsJson(): JSONObject = JSONObject().put("quelle", quelle).put("alter_s", System.currentTimeMillis() / 1000 - zeit)
            .put("geraete", JSONArray(werte.values.toList()))
    }

    /** Der zuletzt gespeicherte Stand, ohne Bluetooth und Netz (für das erste Bild der Karte). */
    fun letzter(): Stand? = runCatching {
        val w = JSONObject(letzterDatei.readText()).getJSONObject("werte")
        Stand(w.keys().asSequence().associateWith { w.getJSONObject(it) }, "zuletzt gespeichert")
    }.getOrNull()?.takeIf { it.werte.isNotEmpty() }

    private val sperre = Mutex()

    /** Frisch lesen (Werkzeug, Startkarte, Aktualisieren-Knopf). Immer nur ein Lauf zur Zeit, sonst drosselt Android den Scan. */
    suspend fun abrufen(): Stand = sperre.withLock {
        withContext(Dispatchers.IO) {
            val geraete = schluessel()
            if (geraete.isEmpty()) return@withContext Stand(emptyMap(), "", stoerung() ?: "Keine Victron-Geräte eingerichtet.")
            val lokal = runCatching { scannen(geraete) }.onFailure { Log.w(TAG, "Scan", it) }.getOrDefault(emptyMap())
            val fern = if (lokal.size >= geraete.size) emptyMap()
                else runCatching { relay() }.onFailure { Logbuch.netz("Victron-Stand vom Relay", it.message ?: it.javaClass.simpleName) }.getOrDefault(emptyMap())
            val alt = letzter()?.werte ?: emptyMap()
            // Je Art der jüngste Wert; bei gleicher Zeit geht direkt vor Rechner vor gespeichert.
            val herkunft = mutableMapOf<String, String>()
            val werte = mutableMapOf<String, JSONObject>()
            for ((art, w) in alt) { werte[art] = w; herkunft[art] = "gespeichert" }
            for ((art, w) in fern) if (w.optLong("zeit") >= (werte[art]?.optLong("zeit") ?: -1)) { werte[art] = w; herkunft[art] = "rechner" }
            for ((art, w) in lokal) { werte[art] = w; herkunft[art] = "direkt" }
            if (werte.isEmpty()) return@withContext Stand(emptyMap(), "", "Die Solaranlage ist gerade nicht erreichbar: kein Victron-Gerät in Bluetooth-Reichweite, und der Rechner zu Hause hat keinen Stand gemeldet.")
            if (lokal.isNotEmpty() || fern.isNotEmpty()) runCatching {
                val neu = File(context.filesDir, "victron-letzter.json.neu")
                neu.writeText(JSONObject().put("werte", JSONObject(werte as Map<*, *>)).toString())
                if (!neu.renameTo(letzterDatei)) { letzterDatei.writeText(neu.readText()); neu.delete() }
            }
            val wege = herkunft.values.toSet()
            val quelle = when {
                wege == setOf("direkt") -> "direkt per Bluetooth (${Geraet.wort})"
                wege == setOf("rechner") -> "über den Rechner zu Hause"
                wege == setOf("gespeichert") -> "zuletzt gespeichert"
                else -> "teils direkt, teils über den Rechner oder gespeichert"
            }
            Stand(werte, quelle, if (lokal.isEmpty() && fern.isEmpty()) "Gerade nicht erreichbar." else null)
        }
    }

    private fun antwort(s: Stand): String {
        val alter = System.currentTimeMillis() / 1000 - s.zeit
        val kopf = when {
            s.fehler != null -> "${s.fehler} Zuletzt bekannt (${alterText(alter)}): "
            alter > 180 -> "Stand ${alterText(alter)}, ${s.quelle}: "
            else -> "Aktuell, ${s.quelle}: "
        }
        return kopf + text(s.werte)
    }

    private fun alterText(s: Long): String = when {
        s < 120 -> "vor $s Sekunden"
        s < 7200 -> "vor ${s / 60} Minuten"
        s < 172800 -> "vor ${s / 3600} Stunden"
        else -> "vor ${s / 86400} Tagen"
    }

    private fun text(werte: Map<String, JSONObject>): String {
        val teile = listOfNotNull(werte["batteriemonitor"]?.let { batterieZeilen(it).joinToString(", ") { (n, w) -> "$n $w" } },
            werte["laderegler"]?.let { reglerZeilen(it).joinToString(", ") { (n, w) -> "$n $w" } }).filter { it.isNotBlank() }
        return if (teile.isEmpty()) "Keine gültigen Messwerte." else teile.joinToString(". ") + "."
    }

    /** Relay-Stand: Werte je Art, jeder mit seiner Messzeit. */
    private fun relay(): Map<String, JSONObject> {
        if (e.serverToken.isBlank()) return emptyMap()
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/victron").header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung).build()
        val j = CLIENT.newCall(anfrage).execute().use { a ->
            check(a.isSuccessful) { "Relay antwortet mit ${a.code}" }
            JSONObject(a.body?.string().orEmpty())
        }
        val liste = j.optJSONArray("geraete") ?: return emptyMap()
        return (0 until liste.length()).mapNotNull { liste.optJSONObject(it) }.filter { it.optString("art").isNotEmpty() && it.optLong("zeit") > 0 }.associateBy { it.optString("art") }
    }

    @SuppressLint("MissingPermission")
    private suspend fun scannen(geraete: List<Geraetschluessel>): Map<String, JSONObject> {
        if (!erlaubt(context)) return emptyMap()
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyMap()
        if (!adapter.isEnabled) return emptyMap()
        val scanner = adapter.bluetoothLeScanner ?: return emptyMap()
        val nachMac = geraete.associateBy { it.mac }
        // Einen Cipher je Gerät statt je Werbepaket.
        val chiffren = geraete.associate { g -> g.mac to Cipher.getInstance("AES/ECB/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(g.schluessel, "AES")) } }
        val gefunden = ConcurrentHashMap<String, JSONObject>()
        val fertig = CompletableDeferred<Unit>()
        val rueckruf = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // Im Binder-Thread: nichts darf hier werfen, sonst stürzt die App ab.
                runCatching {
                    val mac = result.device.address.uppercase(Locale.ROOT)
                    if (gefunden.containsKey(mac)) return
                    val g = nachMac[mac] ?: return
                    val roh = result.scanRecord?.getManufacturerSpecificData(VICTRON) ?: return
                    val w = auswerten(roh, g.schluessel, chiffren.getValue(mac)) ?: return
                    gefunden[mac] = w.put("name", g.name).put("zeit", System.currentTimeMillis() / 1000)
                    if (gefunden.size >= geraete.size) fertig.complete(Unit)
                }.onFailure { Log.w(TAG, "Werbepaket", it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Logbuch.fehler("Victron: Bluetooth-Scan gescheitert", null, "Code $errorCode")
                fertig.complete(Unit)
            }
        }
        val filter = listOf(ScanFilter.Builder().setManufacturerData(VICTRON, byteArrayOf()).build())
        val einstellung = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(filter, einstellung, rueckruf)
        try { withTimeoutOrNull(8_000) { fertig.await() } } finally { runCatching { scanner.stopScan(rueckruf) } }
        return gefunden.values.associateBy { it.getString("art") }
    }

    companion object {
        private const val TAG = "JarvisVictron"
        private const val VICTRON = 0x02E1
        private val CLIENT = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
        private val ZUSTAND = mapOf(0 to "aus", 1 to "Schwachlast", 2 to "Fehler", 3 to "Bulk", 4 to "Absorption", 5 to "Erhaltung", 6 to "Speicher",
            7 to "Ausgleich", 9 to "Wechselrichter", 11 to "Netzteil", 245 to "Start", 247 to "Ausgleich", 252 to "extern gesteuert")

        /** Ohne „Geräte in der Nähe“ (BLUETOOTH_SCAN) bleibt nur der Weg über den Rechner. */
        fun erlaubt(context: Context): Boolean = context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

        private fun zahl(v: Double, stellen: Int = 1): String = String.format(Locale.GERMANY, "%.${stellen}f", v)
        private fun JSONObject.zahlOder(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null

        /** Zeilen (Name, Wert) des Batteriemonitors, für Sprache und Startkarte. */
        fun batterieZeilen(b: JSONObject): List<Pair<String, String>> = buildList {
            b.zahlOder("ladezustand_prozent")?.let { add("Ladezustand" to "${zahl(it)} %") }
            b.zahlOder("spannung_v")?.let { add("Spannung" to "${zahl(it, 2)} V") }
            b.zahlOder("strom_a")?.let { a ->
                add("Strom" to when {
                    a > 0.05 -> "lädt mit ${zahl(a)} A"
                    a < -0.05 -> "gibt ${zahl(-a)} A ab"
                    else -> "kein nennenswerter Strom"
                })
            }
            b.zahlOder("restlaufzeit_min")?.toInt()?.let { m -> add("Restlaufzeit" to if (m >= 1440) "etwa ${m / 1440} Tage" else "${m / 60} h ${m % 60} min") }
            b.zahlOder("verbraucht_ah")?.let { add("Entnommen" to "${zahl(-it)} Ah") }
        }

        /** Zeilen (Name, Wert) des Ladereglers, für Sprache und Startkarte. */
        fun reglerZeilen(l: JSONObject): List<Pair<String, String>> = buildList {
            l.zahlOder("solarleistung_w")?.let { add("Solarleistung" to "${it.toInt()} W") }
            l.zahlOder("ladestrom_a")?.let { add("Ladestrom" to "${zahl(it)} A") }
            l.zahlOder("ertrag_heute_kwh")?.let { add("Ertrag heute" to "${zahl(it, 2)} kWh") }
            l.zahlOder("spannung_v")?.let { add("Batteriespannung" to "${zahl(it, 2)} V") }
            l.optString("zustand").takeIf { it.isNotEmpty() }?.let { add("Ladephase" to it) }
        }

        private fun le(d: ByteArray, von: Int, laenge: Int): Long {
            var v = 0L
            for (i in laenge - 1 downTo 0) v = (v shl 8) or (d[von + i].toLong() and 0xFF)
            return v
        }

        private fun mitVorzeichen(v: Long, bits: Int): Long = if (v and (1L shl (bits - 1)) != 0L) v - (1L shl bits) else v

        /** Gleicher Aufbau wie jarvis_victron.py: 0x10, ?, Modell (2), Art, Zähler (2, LE), Schlüsselbyte, verschlüsselte Werte. */
        fun auswerten(daten: ByteArray, schluessel: ByteArray, aes: Cipher): JSONObject? {
            if (daten.size < 9 || daten[0] != 0x10.toByte() || daten[7] != schluessel[0]) return null
            val zaehler = (daten[5].toInt() and 0xFF) or ((daten[6].toInt() and 0xFF) shl 8)
            val roh = daten.copyOfRange(8, daten.size)
            val strom = ByteArray(((roh.size + 15) / 16) * 16)
            for (b in 0 until strom.size / 16) {
                // Zähler little endian, wie pycryptodome Counter(little_endian=True).
                val block = ByteArray(16)
                var z = (zaehler + b).toLong()
                for (i in 0 until 8) { block[i] = (z and 0xFF).toByte(); z = z shr 8 }
                aes.doFinal(block).copyInto(strom, b * 16)
            }
            val d = ByteArray(roh.size) { (roh[it].toInt() xor strom[it].toInt()).toByte() }
            return when (daten[4].toInt()) {
                0x01 -> if (d.size < 10) null else JSONObject().apply {
                    put("art", "laderegler")
                    put("zustand", ZUSTAND[d[0].toInt() and 0xFF] ?: "Zustand ${d[0].toInt() and 0xFF}")
                    put("fehler", d[1].toInt() and 0xFF)
                    mitVorzeichen(le(d, 2, 2), 16).let { put("spannung_v", if (it == 0x7FFFL) JSONObject.NULL else it / 100.0) }
                    mitVorzeichen(le(d, 4, 2), 16).let { put("ladestrom_a", if (it == 0x7FFFL) JSONObject.NULL else it / 10.0) }
                    le(d, 6, 2).let { put("ertrag_heute_kwh", if (it == 0xFFFFL) JSONObject.NULL else it / 100.0) }
                    le(d, 8, 2).let { put("solarleistung_w", if (it == 0xFFFFL) JSONObject.NULL else it) }
                }
                0x02 -> if (d.size < 15) null else JSONObject().apply {
                    put("art", "batteriemonitor")
                    ((le(d, 13, 2) shr 4) and 0x3FF).let { put("ladezustand_prozent", if (it == 0x3FFL) JSONObject.NULL else it / 10.0) }
                    mitVorzeichen(le(d, 2, 2), 16).let { put("spannung_v", if (it == 0x7FFFL) JSONObject.NULL else it / 100.0) }
                    // Ungültig ist wie bei 0x7FFF der größte positive Wert des 22-Bit-Felds.
                    (mitVorzeichen(le(d, 8, 3), 24) shr 2).let { put("strom_a", if (it == 0x1FFFFFL) JSONObject.NULL else it / 1000.0) }
                    (le(d, 11, 3) and 0xFFFFF).let { put("verbraucht_ah", if (it == 0xFFFFFL) JSONObject.NULL else -it / 10.0) }
                    le(d, 0, 2).let { put("restlaufzeit_min", if (it == 0xFFFFL) JSONObject.NULL else it) }
                }
                else -> null
            }
        }
    }
}
