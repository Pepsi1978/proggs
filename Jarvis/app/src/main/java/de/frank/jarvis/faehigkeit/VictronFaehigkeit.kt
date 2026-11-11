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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Victron-Solaranlage: BMV-712 Smart (Ladezustand, Spannung, Strom) und SmartSolar-Laderegler (Solarleistung, Tagesertrag).
 * Beide senden ihre Werte verschlüsselt im Bluetooth-Werbepaket („Instant Readout“, Hersteller 0x02E1, AES-CTR).
 * Ablauf: erst selbst per Bluetooth lauschen (bis 8 s); fehlt ein Gerät (unterwegs), den Stand nehmen, den der Rechner in Reichweite
 * jede Minute an den Relay meldet (werkzeuge/pc-bruecke/jarvis_victron.py, GET /geraet/victron); sonst den zuletzt bekannten.
 * Schlüssel: files/victron.json (aus ~/SK/Victron per werkzeuge/zugang-aufs-handy.ps1), nie im APK.
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
        ) { lesen() },
    )

    private class Geraetschluessel(val name: String, val art: String, val mac: String, val schluessel: ByteArray)

    private fun schluessel(): List<Geraetschluessel> = runCatching {
        val liste = JSONObject(schluesselDatei.readText()).getJSONArray("geraete")
        (0 until liste.length()).map { i ->
            val g = liste.getJSONObject(i)
            Geraetschluessel(g.getString("name"), g.getString("art"), g.getString("mac").uppercase(Locale.ROOT),
                g.getString("schluessel").chunked(2).map { it.toInt(16).toByte() }.toByteArray())
        }
    }.getOrDefault(emptyList())

    private suspend fun lesen(): Ergebnis = withContext(Dispatchers.IO) {
        val geraete = schluessel()
        if (geraete.isEmpty()) return@withContext Ergebnis(stoerung() ?: "Keine Victron-Geräte eingerichtet.", fehler = true)
        val jetzt = System.currentTimeMillis() / 1000
        val lokal = runCatching { scannen(geraete) }.onFailure { Log.w(TAG, "Scan", it) }.getOrDefault(emptyMap())
        var quelle = "direkt per Bluetooth (${Geraet.wort})"
        var werte: Map<String, JSONObject> = lokal
        var alter = 0L
        if (lokal.size < geraete.size) {
            val fern = runCatching { relay() }.onFailure { Logbuch.netz("Victron-Stand vom Relay", it.message ?: it.javaClass.simpleName) }.getOrNull()
            if (fern != null && fern.second.isNotEmpty()) {
                // Lokal Gefundenes geht vor; der Rest kommt vom Rechner.
                werte = fern.second + lokal
                alter = if (lokal.isEmpty()) fern.first else 0
                quelle = if (lokal.isEmpty()) "über den Rechner zu Hause" else "teils direkt, teils über den Rechner"
            }
        }
        if (werte.isEmpty()) {
            val alt = runCatching { JSONObject(letzterDatei.readText()) }.getOrNull()
                ?: return@withContext Ergebnis("Die Solaranlage ist gerade nicht erreichbar: kein Victron-Gerät in Bluetooth-Reichweite, und der Rechner zu Hause hat keinen Stand gemeldet.", fehler = true)
            val zeit = alt.optLong("zeit")
            return@withContext Ergebnis("Gerade nicht erreichbar. Zuletzt bekannt (${alterText(jetzt - zeit)}): " + text(alt.getJSONObject("werte").let { j -> j.keys().asSequence().associateWith { j.getJSONObject(it) } }))
        }
        if (alter == 0L || lokal.isNotEmpty()) {
            runCatching { letzterDatei.writeText(JSONObject().put("zeit", jetzt).put("werte", JSONObject(werte)).toString()) }
        }
        val kopf = if (alter > 180) "Stand ${alterText(alter)}, $quelle: " else "Aktuell, $quelle: "
        Ergebnis(kopf + text(werte), struktur = JSONObject().put("quelle", quelle).put("alter_s", alter).put("geraete", JSONArray(werte.values.toList())))
    }

    private fun alterText(s: Long): String = when {
        s < 120 -> "vor ${s} Sekunden"
        s < 7200 -> "vor ${s / 60} Minuten"
        s < 172800 -> "vor ${s / 3600} Stunden"
        else -> "vor ${s / 86400} Tagen"
    }

    private fun zahl(v: Double, stellen: Int = 1): String = String.format(Locale.GERMANY, "%.${stellen}f", v)

    private fun text(werte: Map<String, JSONObject>): String {
        val teile = mutableListOf<String>()
        werte.values.firstOrNull { it.optString("art") == "batteriemonitor" }?.let { b ->
            val s = mutableListOf<String>()
            if (b.has("ladezustand_prozent") && !b.isNull("ladezustand_prozent")) s += "Batterie ${zahl(b.getDouble("ladezustand_prozent"))} %"
            if (b.has("spannung_v") && !b.isNull("spannung_v")) s += "${zahl(b.getDouble("spannung_v"), 2)} V"
            if (b.has("strom_a") && !b.isNull("strom_a")) {
                val a = b.getDouble("strom_a")
                s += when {
                    a > 0.05 -> "wird mit ${zahl(a)} A geladen"
                    a < -0.05 -> "gibt ${zahl(-a)} A ab"
                    else -> "kein nennenswerter Strom"
                }
            }
            if (b.has("restlaufzeit_min") && !b.isNull("restlaufzeit_min")) {
                val m = b.getInt("restlaufzeit_min")
                s += if (m >= 1440) "reicht noch etwa ${m / 1440} Tage" else "reicht noch etwa ${m / 60} Stunden ${m % 60} Minuten"
            }
            if (b.has("verbraucht_ah") && !b.isNull("verbraucht_ah")) s += "entnommen ${zahl(-b.getDouble("verbraucht_ah"))} Ah"
            teile += s.joinToString(", ")
        }
        werte.values.firstOrNull { it.optString("art") == "laderegler" }?.let { l ->
            val s = mutableListOf<String>()
            if (l.has("solarleistung_w") && !l.isNull("solarleistung_w")) s += "Solarleistung ${l.getInt("solarleistung_w")} W"
            if (l.has("ladestrom_a") && !l.isNull("ladestrom_a")) s += "Ladestrom ${zahl(l.getDouble("ladestrom_a"))} A"
            if (l.has("ertrag_heute_kwh") && !l.isNull("ertrag_heute_kwh")) s += "heute ${zahl(l.getDouble("ertrag_heute_kwh"), 2)} kWh erzeugt"
            if (l.optString("zustand").isNotEmpty()) s += "Laderegler: ${l.getString("zustand")}"
            teile += s.joinToString(", ")
        }
        return teile.joinToString(". ") + "."
    }

    /** Stand vom Relay: Alter in Sekunden und Werte je MAC (ohne Doppelpunkte, klein). */
    private fun relay(): Pair<Long, Map<String, JSONObject>>? {
        if (e.serverToken.isBlank()) return null
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/victron").header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung).build()
        val j = CLIENT.newCall(anfrage).execute().use { a ->
            check(a.isSuccessful) { "Relay antwortet mit ${a.code}" }
            JSONObject(a.body?.string().orEmpty())
        }
        val liste = j.optJSONArray("geraete") ?: return null
        val werte = (0 until liste.length()).map { liste.getJSONObject(it) }.associateBy { it.optString("art") }
        return (if (j.isNull("alter_s")) Long.MAX_VALUE else j.optLong("alter_s")) to werte
    }

    @SuppressLint("MissingPermission")
    private suspend fun scannen(geraete: List<Geraetschluessel>): Map<String, JSONObject> {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Logbuch.fehler("Victron: Bluetooth-Erlaubnis fehlt", null, "BLUETOOTH_SCAN")
            return emptyMap()
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyMap()
        if (!adapter.isEnabled) return emptyMap()
        val scanner = adapter.bluetoothLeScanner ?: return emptyMap()
        val nachMac = geraete.associateBy { it.mac }
        val gefunden = ConcurrentHashMap<String, JSONObject>()
        val fertig = CompletableDeferred<Unit>()
        val rueckruf = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val g = nachMac[result.device.address.uppercase(Locale.ROOT)] ?: return
                val roh = result.scanRecord?.getManufacturerSpecificData(VICTRON) ?: return
                val w = auswerten(roh, g.schluessel) ?: return
                w.put("name", g.name).put("zeit", System.currentTimeMillis() / 1000)
                // Schlüssel wie beim Relay: nach Art, damit beide Quellen sich mischen lassen.
                gefunden[w.getString("art")] = w
                if (gefunden.size >= geraete.size) fertig.complete(Unit)
            }
        }
        val filter = listOf(ScanFilter.Builder().setManufacturerData(VICTRON, byteArrayOf()).build())
        val einstellung = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(filter, einstellung, rueckruf)
        try { withTimeoutOrNull(8_000) { fertig.await() } } finally { runCatching { scanner.stopScan(rueckruf) } }
        return gefunden.toMap()
    }

    companion object {
        private const val TAG = "JarvisVictron"
        private const val VICTRON = 0x02E1
        private val CLIENT = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
        private val ZUSTAND = mapOf(0 to "aus", 1 to "Schwachlast", 2 to "Fehler", 3 to "Bulk", 4 to "Absorption", 5 to "Erhaltung", 6 to "Speicher",
            7 to "Ausgleich", 9 to "Wechselrichter", 11 to "Netzteil", 245 to "Start", 247 to "Ausgleich", 252 to "extern gesteuert")

        private fun le(d: ByteArray, von: Int, laenge: Int): Long {
            var v = 0L
            for (i in laenge - 1 downTo 0) v = (v shl 8) or (d[von + i].toLong() and 0xFF)
            return v
        }

        private fun mitVorzeichen(v: Long, bits: Int): Long = if (v and (1L shl (bits - 1)) != 0L) v - (1L shl bits) else v

        /** Gleicher Aufbau wie jarvis_victron.py: 0x10, ?, Modell (2), Art, Zähler (2, LE), Schlüsselbyte, verschlüsselte Werte. */
        fun auswerten(daten: ByteArray, schluessel: ByteArray): JSONObject? {
            if (daten.size < 9 || daten[0] != 0x10.toByte() || daten[7] != schluessel[0]) return null
            val zaehler = (daten[5].toInt() and 0xFF) or ((daten[6].toInt() and 0xFF) shl 8)
            val roh = daten.copyOfRange(8, daten.size)
            val aes = Cipher.getInstance("AES/ECB/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(schluessel, "AES")) }
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
                    (mitVorzeichen(le(d, 8, 3), 24) shr 2).let { put("strom_a", if (it == -0x200000L) JSONObject.NULL else it / 1000.0) }
                    (le(d, 11, 3) and 0xFFFFF).let { put("verbraucht_ah", if (it == 0xFFFFFL) JSONObject.NULL else -it / 10.0) }
                    le(d, 0, 2).let { put("restlaufzeit_min", if (it == 0xFFFFL) JSONObject.NULL else it) }
                }
                else -> null
            }
        }
    }
}
