package de.frank.jarvis.faehigkeit

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks LED-Streifen (Bluetooth, sonst über die App „KS Lite“ com.huaxin.smartlight, Gerätenamen „KS03~…“), in der ganzen Wohnung verteilt.
 * Jarvis spricht die Streifen direkt an, ohne die App: Protokoll aus KS Lite 1.8 nachgebaut (unverschlüsselt), Dienst/Kanal und Befehle je Namensvorsatz in [TYPEN].
 * Handy und Mac (Brücke, jarvis_licht.py über POST /geraet/licht) schalten gleichzeitig; jeder erreicht, was in seiner Reichweite ist.
 * Die Streifen merken sich Farbe und Helligkeit selbst; „an“ stellt den letzten Zustand wieder her. Adressen gefundener Streifen liegen in prefs „licht“
 * (direkt erreichbar ohne Scan, auch bei dunklem Bildschirm), die zuletzt gewählte Farbe/Helligkeit in prefs „licht_zustand“.
 */
class LichtFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "licht"
    override val name = "Licht"
    override val beschreibung = "Franks LED-Streifen in der Wohnung (Bluetooth): alle an- oder ausschalten, Farbe und Helligkeit für alle gleichzeitig setzen."
    override val hinweise =
        "Sagt Frank „mach das Licht an“, „Licht an“, „mach die LEDs an“, „LED-Licht an“ oder „Licht aus“, nimm licht mit aktion an oder aus: " +
            "Es schaltet alle LED-Streifen der Wohnung (Wohnzimmer, Wohnzimmer 2, Wohnzimmer 3, Flur, Küche, Bad) gleichzeitig, Farbe und Helligkeit wie zuletzt. " +
            "„Mach das Licht blau“, „LEDs warmweiß“ → aktion farbe mit farbe (Name wie rot, blau, warmweiß oder #RRGGBB), optional helligkeit. " +
            "„Licht heller/dunkler/auf 30 Prozent“ → aktion helligkeit mit helligkeit 1 bis 100 (heller/dunkler: von der zuletzt gesetzten aus ±25). " +
            "Alle Streifen bekommen immer dieselbe Farbe. Melde danach kurz, wie viele Streifen geschaltet wurden; nennt das Ergebnis nicht erreichte Streifen oder einen Grund, sag das genau so."

    override fun stoerung(): String? = if (!erlaubt()) "Für das Licht fehlt Jarvis das Recht „Geräte in der Nähe“ (Bluetooth)." else null

    override val werkzeuge = listOf(
        Werkzeug(
            name = "licht",
            titel = "Licht schalten",
            beschreibung = "Jarvis: schaltet alle LED-Streifen in Franks Wohnung per Bluetooth direkt (ohne KS-Lite-App, Handy und Mac gleichzeitig). " +
                "Aktionen: an, aus (Farbe und Helligkeit wie zuletzt), farbe (alle in derselben Farbe, optional mit helligkeit), helligkeit (1 bis 100).",
            schema = schema(
                "aktion" to text("an, aus, farbe oder helligkeit.", listOf("an", "aus", "farbe", "helligkeit")),
                "farbe" to text("Nur farbe: " + FARBEN.keys.joinToString(", ") + " oder #RRGGBB."),
                "helligkeit" to text("Bei farbe oder helligkeit: 1 bis 100 Prozent, oder heller/dunkler."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
            taetigkeit = "schaltet das Licht",
        ) { a -> schalten(a) },
    )

    private class Typ(val vorsatz: String, val dienst: String, val kanal: String, val an: String, val aus: String, val alt: Boolean) {
        /** Befehle für Farbe [rgb] (6 Hex-Zeichen) und Helligkeit [h] 1–100; vorher einschalten, sonst bleibt ein ausgeschalteter Streifen dunkel. */
        fun farbe(rgb: String, h: Int): List<String> {
            val hh = "%02X".format(h.coerceIn(1, 100))
            return if (alt) listOf(an, "7E070503${rgb}00EF", "7E0401${hh}FFFFFF00EF") else listOf(an, "5A0001${rgb}00${hh}00A5")
        }
    }

    private fun erlaubt(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)

    private val prefs get() = context.getSharedPreferences("licht", Context.MODE_PRIVATE)
    private val zustand get() = context.getSharedPreferences("licht_zustand", Context.MODE_PRIVATE)

    /** Bekannte Streifen: Adresse → Gerätename. */
    private fun bekannt(): Map<String, String> = prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()

    private suspend fun schalten(a: JSONObject): Ergebnis {
        val aktion = a.optString("aktion").trim().lowercase()
        val hText = a.optString("helligkeit").trim().lowercase()
        val letzteH = zustand.getInt("helligkeit", 100)
        val h = when {
            hText.isEmpty() -> letzteH
            "hell" in hText || "mehr" in hText -> letzteH + 25
            "dunk" in hText || "weniger" in hText -> letzteH - 25
            "max" in hText || "voll" in hText -> 100
            else -> Regex("\\d+").find(hText)?.value?.toIntOrNull() ?: return Ergebnis("RÜCKFRAGE: Welche Helligkeit, 1 bis 100 Prozent?", true)
        }.coerceIn(1, 100)
        // befehl: je Typ die Bytefolgen; text: was Frank hört.
        val (befehl: (Typ) -> List<String>, text: String) = when (aktion) {
            "an", "ein", "einschalten", "on" -> Pair({ t: Typ -> listOf(t.an) }, "an")
            "aus", "ausschalten", "off" -> Pair({ t: Typ -> listOf(t.aus) }, "aus")
            "farbe" -> {
                val f = a.optString("farbe").trim()
                val rgb = farbe(f) ?: return Ergebnis("RÜCKFRAGE: Welche Farbe? Möglich: ${FARBEN.keys.joinToString(", ")} oder #RRGGBB.", true)
                zustand.edit().putString("farbe", rgb).putInt("helligkeit", h).apply()
                Pair({ t: Typ -> t.farbe(rgb, h) }, "auf ${f.ifEmpty { "die Farbe" }} gestellt" + if (hText.isNotEmpty()) " mit $h Prozent" else "")
            }
            "helligkeit" -> {
                val rgb = zustand.getString("farbe", null) ?: "FFFFFF"
                zustand.edit().putInt("helligkeit", h).apply()
                Pair({ t: Typ -> t.farbe(rgb, h) }, "auf $h Prozent")
            }
            else -> return Ergebnis("RÜCKFRAGE: Licht an, aus, eine Farbe oder eine Helligkeit?", true)
        }
        if (!erlaubt()) return Ergebnis("Jarvis fehlt das Recht „Geräte in der Nähe“ (Bluetooth). Bitte in den App-Infos von Jarvis erlauben.", true)
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter

        // Mac und Handy gleichzeitig: Jeder schaltet, was er erreicht.
        val (handy, mac) = coroutineScope {
            val m = async { runCatching { ueberMac(befehl) }.getOrElse { MacErgebnis(emptySet(), emptySet(), "nicht erreichbar (${it.javaClass.simpleName})") } }
            val hd = async { if (adapter?.isEnabled == true) handySchalten(adapter, befehl) else emptyMap() }
            hd.await() to m.await()
        }
        // Mac und Handy nennen denselben Streifen teils mit vertauschten Bytes (KS03~000F97 / KS03~97000F): über [schluessel] gleichsetzen.
        val alle = (handy.keys + mac.gesehen).map(::schluessel).toSortedSet()
        val ok = (handy.filterValues { it }.keys + mac.geschaltet).map(::schluessel).toSortedSet()
        val fehlt = alle.filter { it !in ok }
        Logbuch.netz("Licht $aktion", "Handy ${handy.count { it.value }}/${handy.size}, Mac ${mac.geschaltet.size}/${mac.gesehen.size}${mac.grund?.let { " ($it)" } ?: ""}" +
            if (fehlt.isNotEmpty()) ", fehlt: ${fehlt.joinToString()}" else "")
        val gesamt = maxOf(alle.size, BEKANNTE_NAMEN.size)
        return when {
            ok.isEmpty() && adapter?.isEnabled != true && mac.grund != null ->
                Ergebnis("Ich habe keinen LED-Streifen erreicht: Bluetooth am Handy ist aus und der Mac ${mac.grund}.", true)
            ok.isEmpty() -> Ergebnis("Ich habe keinen LED-Streifen erreicht. Ist die KS-Lite-App noch offen? Sie hält die Streifen besetzt; bitte schließen und noch einmal versuchen." +
                (mac.grund?.let { " Der Mac ist $it." } ?: ""), true)
            ok.size >= gesamt -> Ergebnis("Alle ${ok.size} LED-Streifen sind $text.")
            else -> Ergebnis("${ok.size} von $gesamt LED-Streifen sind $text; nicht erreicht: ${gesamt - ok.size} " +
                "(außer Reichweite von Handy und Mac oder von der KS-Lite-App besetzt)." + (mac.grund?.let { " Der Mac ist $it." } ?: ""))
        }
    }

    // ------------------------------------------------------------------ Handy

    /** Schaltet per Handy-Bluetooth; Ergebnis je Gerätename. Bekannte direkt, fehlt danach etwas, suchen und den Rest nachholen. */
    private suspend fun handySchalten(adapter: BluetoothAdapter, befehl: (Typ) -> List<String>): Map<String, Boolean> {
        val geraete = HashMap(bekannt())
        val ergebnis = ConcurrentHashMap<String, Boolean>()
        if (geraete.isNotEmpty()) senden(adapter, geraete, befehl, ergebnis)
        if (geraete.isEmpty() || ergebnis.values.any { !it } || geraete.size < BEKANNTE_NAMEN.size) {
            val gefunden = suchen(adapter)
            gefunden.forEach { (mac, n) -> prefs.edit().putString(mac, n).apply() }
            val offen = gefunden.filterKeys { ergebnis[it] != true }
            if (offen.isNotEmpty()) senden(adapter, offen, befehl, ergebnis)
            geraete.putAll(gefunden)
        }
        return geraete.entries.associate { (mac, n) -> n to (ergebnis[mac] == true) }
    }

    private suspend fun senden(adapter: BluetoothAdapter, ziele: Map<String, String>, befehl: (Typ) -> List<String>, ergebnis: ConcurrentHashMap<String, Boolean>) =
        coroutineScope {
            ziele.entries.mapIndexed { i, (mac, n) ->
                async {
                    delay(i * 150L)
                    val typ = typ(n)
                    var ok = false
                    for (versuch in 0..1) {
                        ok = runCatching { schreiben(adapter.getRemoteDevice(mac), typ, befehl(typ)) }
                            .onFailure { Logbuch.fehler("Licht: $n", it) }.getOrDefault(false)
                        if (ok) break
                        delay(400)
                    }
                    ergebnis[mac] = ok
                }
            }.awaitAll()
        }

    /** Verbinden, Dienste suchen, Befehle nacheinander schreiben, trennen. true = alle angenommen. */
    @SuppressLint("MissingPermission")
    private suspend fun schreiben(geraet: BluetoothDevice, typ: Typ, befehle: List<String>): Boolean {
        val bereit = CompletableDeferred<BluetoothGattCharacteristic?>()
        var geschrieben = CompletableDeferred<Boolean>()
        val rueckruf = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    if (!gatt.discoverServices()) bereit.complete(null)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    bereit.complete(null); geschrieben.complete(false)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                bereit.complete(runCatching { kanal(gatt, typ) }.getOrNull())
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                geschrieben.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }
        val gatt = geraet.connectGatt(context, false, rueckruf, BluetoothDevice.TRANSPORT_LE) ?: return false
        return try {
            withTimeoutOrNull(VERBINDEN_MS) {
                val k = bereit.await() ?: return@withTimeoutOrNull false
                val ohneAntwort = k.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0
                val art = if (ohneAntwort) BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                for (hex in befehle) {
                    geschrieben = CompletableDeferred()
                    val wert = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    val gesendet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(k, wert, art) == BluetoothGatt.GATT_SUCCESS
                    } else {
                        @Suppress("DEPRECATION") run { k.writeType = art; k.value = wert; gatt.writeCharacteristic(k) }
                    }
                    if (!gesendet) return@withTimeoutOrNull false
                    // Ohne Antwort meldet der Streifen nichts: kurz Luft lassen. Mit Antwort auf die Bestätigung warten.
                    if (ohneAntwort) delay(60) else if (!geschrieben.await()) return@withTimeoutOrNull false
                }
                delay(150) // Befehle aus dem Sendepuffer lassen, bevor getrennt wird.
                true
            } ?: false
        } finally {
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
        }
    }

    /** Schreib-Kanal laut Typ; sonst den ersten bekannten aus [TYPEN], den der Streifen anbietet. */
    private fun kanal(gatt: BluetoothGatt, typ: Typ): BluetoothGattCharacteristic? =
        (listOf(typ) + TYPEN).firstNotNullOfOrNull { t -> gatt.getService(uuid(t.dienst))?.getCharacteristic(uuid(t.kanal)) }

    /** Sucht [SUCHE_MS] nach Streifen (Name beginnt mit „KS“). Bei dunklem Bildschirm liefert Android nur gefilterte Scans: dann nach bekannten Namen. */
    @SuppressLint("MissingPermission")
    private suspend fun suchen(adapter: BluetoothAdapter): Map<String, String> {
        val scanner = adapter.bluetoothLeScanner ?: return emptyMap()
        val gefunden = ConcurrentHashMap<String, String>()
        val rueckruf = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                runCatching {
                    val n = result.scanRecord?.deviceName ?: result.device.name ?: return
                    if (n.startsWith("KS")) gefunden[result.device.address] = n
                }
            }

            override fun onScanFailed(errorCode: Int) { Logbuch.fehler("Licht: Bluetooth-Scan gescheitert", null, "Code $errorCode") }
        }
        val wach = context.getSystemService(PowerManager::class.java)?.isInteractive != false
        val namen = (BEKANNTE_NAMEN + bekannt().values).distinct()
        val filter = if (wach) null else namen.map { ScanFilter.Builder().setDeviceName(it).build() }
        scanner.startScan(filter, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), rueckruf)
        try {
            withTimeoutOrNull(SUCHE_MS) { while (!gefunden.values.containsAll(namen)) delay(200) }
        } finally {
            runCatching { scanner.stopScan(rueckruf) }
        }
        return gefunden
    }

    // ------------------------------------------------------------------ Mac

    private class MacErgebnis(val geschaltet: Set<String>, val gesehen: Set<String>, val grund: String?)

    /** Der Mac schaltet per Bluetooth (Brücke, jarvis_licht.py); er bekommt die Bytefolgen je Namensvorsatz. */
    private suspend fun ueberMac(befehl: (Typ) -> List<String>): MacErgebnis = withContext(Dispatchers.IO) {
        val e = Einstellungen.get(context)
        if (e.serverToken.isBlank()) return@withContext MacErgebnis(emptySet(), emptySet(), "nicht eingerichtet")
        val befehle = JSONObject().apply { TYPEN.forEach { t -> put(t.vorsatz, JSONArray(befehl(t))) } }
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/licht").header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung)
            .post(JSONObject().put("befehle", befehle).toString().toRequestBody("application/json".toMediaType())).build()
        CLIENT.newCall(anfrage).execute().use { a ->
            if (!a.isSuccessful) return@withContext MacErgebnis(emptySet(), emptySet(), "nicht erreichbar (Relay ${a.code})")
            val j = JSONObject(a.body?.string().orEmpty())
            fun menge(k: String) = j.optJSONArray(k)?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() } ?: emptySet()
            when (j.optString("ergebnis")) {
                "fertig" -> MacErgebnis(menge("geschaltet"), menge("gesehen"), j.optString("fehler").takeIf { it.isNotBlank() }?.let { "mit Fehler: ${it.take(120)}" })
                "mac_nicht_verbunden" -> MacErgebnis(emptySet(), emptySet(), "nicht verbunden")
                else -> MacErgebnis(emptySet(), emptySet(), "ohne Rückmeldung")
            }
        }
    }

    /** Gleicher Streifen, gleicher Schlüssel: Vorsatz plus die Bytes der Kennung sortiert, groß geschrieben. */
    private fun schluessel(n: String): String {
        val vor = n.takeWhile { it != '~' && it != '-' }
        val rest = n.drop(vor.length + 1).uppercase()
        return if (rest.length % 2 == 0 && rest.isNotEmpty()) vor + "~" + rest.chunked(2).sorted().joinToString("") else n.uppercase()
    }

    private fun farbe(f: String): String? {
        val k = f.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss").replace(Regex("[^a-z0-9#]"), "")
        Regex("#?([0-9a-f]{6})").matchEntire(k)?.let { return it.groupValues[1].uppercase() }
        return FARBEN.entries.firstOrNull { (n, _) -> n.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss").replace(" ", "") == k }?.value
            ?: FARBEN.entries.firstOrNull { (n, _) -> k.contains(n.lowercase().replace("ü", "ue").replace("ä", "ae").replace(" ", "")) }?.value
    }

    private companion object {
        const val VERBINDEN_MS = 8_000L
        const val SUCHE_MS = 5_000L
        val CLIENT: OkHttpClient = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).build()
        fun uuid(kurz: String): UUID = UUID.fromString("0000$kurz-0000-1000-8000-00805f9b34fb")

        /** Aus KS Lite 1.8 (UUIDBeanList, Cmd_KS0x, CmdBase); der erste passende Vorsatz gilt. alt = 7E-Befehlssatz (KS03-/KS04-). */
        val TYPEN = listOf(
            Typ("KS01-", "AE00", "AE01", "ACF000CA", "AC0F00CA", false),
            Typ("KS02-", "AE00", "AE01", "ACF000CA", "AC0F00CA", false),
            Typ("KS03-", "FFF0", "FFF3", "7E0404F00101FF00EF", "7E0404100100FF00EF", true),
            Typ("KS03~", "AFD0", "AFD1", "5BF000B5", "5B0F00B5", false),
            Typ("KS04-", "FFF0", "FFF3", "7E0404F00101FF00EF", "7E0404100100FF00EF", true),
            Typ("KS15~", "AFD0", "AFD1", "5BF000B5", "5B0F00B5", false),
            Typ("KS", "AE00", "AE01", "5BF000B5", "5B0F00B5", false),
        )
        fun typ(name: String): Typ = TYPEN.first { name.startsWith(it.vorsatz) || it.vorsatz == "KS" }

        /** Franks sechs Streifen (Stand 19.11.2026, aus der Bluetooth-Liste des Handys), damit sie auch bei dunklem Bildschirm gefunden werden. */
        val BEKANNTE_NAMEN = listOf("KS03~005EAD", "KS03~2b2a71", "KS03~000F97", "KS03~e52cc0", "KS03~3601E0", "KS03~1101E3")

        val FARBEN = linkedMapOf(
            "warmweiß" to "FFA03C", "kaltweiß" to "D0E0FF", "weiß" to "FFFFFF", "rot" to "FF0000", "orange" to "FF6000", "gelb" to "FFC800",
            "grün" to "00FF00", "türkis" to "00FFC0", "cyan" to "00FFFF", "hellblau" to "40A0FF", "blau" to "0000FF", "lila" to "8000FF",
            "violett" to "8000FF", "magenta" to "FF00FF", "pink" to "FF2080", "rosa" to "FF6080",
        )
    }
}
