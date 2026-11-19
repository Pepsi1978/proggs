package de.frank.jarvis.faehigkeit

import android.Manifest
import android.annotation.SuppressLint
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
import de.frank.jarvis.observability.Logbuch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Franks LED-Streifen (Bluetooth, App „KS Lite“ com.huaxin.smartlight, Gerätenamen „KS03~…“), in der ganzen Wohnung verteilt.
 * Protokoll aus der KS-Lite-App 1.8 (unverschlüsselt): Dienst/Kanal je Namensvorsatz siehe [TYPEN], An/Aus als feste Bytes.
 * Die Streifen merken sich Farbe, Helligkeit und Modus selbst; „an“ stellt also den letzten Zustand aus der App wieder her.
 * Adressen der gefundenen Streifen liegen in prefs „licht“, damit Jarvis sie ohne Scan (auch bei aus­geschaltetem Bildschirm) direkt erreicht.
 */
class LichtFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "licht"
    override val name = "Licht"
    override val beschreibung = "Franks LED-Streifen in der Wohnung (Bluetooth, KS Lite): alle an- oder ausschalten, mit Farbe und Helligkeit wie zuletzt in der App."
    override val hinweise =
        "Sagt Frank „mach das Licht an“, „Licht an“, „mach die LEDs an“ oder „Licht aus“, nimm licht mit aktion an oder aus. Es schaltet alle LED-Streifen der Wohnung " +
            "(Wohnzimmer, Wohnzimmer 2, Wohnzimmer 3, Flur, Küche, Bad) gleichzeitig; Farbe und Helligkeit bleiben wie zuletzt in der KS-Lite-App eingestellt. " +
            "Melde danach kurz, wie viele Streifen geschaltet wurden; nennt das Ergebnis nicht erreichte Streifen oder einen Grund (KS-Lite-App offen, Bluetooth aus), sag das genau so."

    override fun stoerung(): String? = when {
        !erlaubt() -> "Für das Licht fehlt Jarvis das Recht „Geräte in der Nähe“ (Bluetooth)."
        else -> null
    }

    override val werkzeuge = listOf(
        Werkzeug(
            name = "licht",
            titel = "Licht schalten",
            beschreibung = "Jarvis: schaltet alle LED-Streifen in Franks Wohnung per Bluetooth an oder aus (Farbe und Helligkeit wie zuletzt in der KS-Lite-App).",
            schema = schema("aktion" to text("an oder aus.", listOf("an", "aus")), pflicht = listOf("aktion")),
            nurLesen = false,
            taetigkeit = "schaltet das Licht",
        ) { a -> schalten(a) },
    )

    private class Typ(val vorsatz: String, val dienst: String, val kanal: String, val an: String, val aus: String)

    private fun erlaubt(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)

    private val prefs get() = context.getSharedPreferences("licht", Context.MODE_PRIVATE)

    /** Bekannte Streifen: Adresse → Gerätename. */
    private fun bekannt(): Map<String, String> = prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()

    private suspend fun schalten(a: JSONObject): Ergebnis {
        val an = when (a.optString("aktion").trim().lowercase()) {
            "an", "ein", "einschalten", "on" -> true
            "aus", "ausschalten", "off" -> false
            else -> return Ergebnis("RÜCKFRAGE: Licht an oder aus?", true)
        }
        if (!erlaubt()) return Ergebnis("Jarvis fehlt das Recht „Geräte in der Nähe“ (Bluetooth). Bitte in den App-Infos von Jarvis erlauben.", true)
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return Ergebnis("Dieses Gerät hat kein Bluetooth.", true)
        if (!adapter.isEnabled) return Ergebnis("Bluetooth ist ausgeschaltet, deshalb kann ich das Licht nicht schalten.", true)

        val geraete = HashMap(bekannt())
        val ergebnis = ConcurrentHashMap<String, Boolean>()
        // Erst die bekannten direkt; fehlt danach etwas oder ist noch keiner bekannt, suchen und nur die übrigen nachholen.
        if (geraete.isNotEmpty()) senden(adapter, geraete, an, ergebnis)
        if (geraete.isEmpty() || ergebnis.values.any { !it }) {
            val gefunden = suchen(adapter)
            gefunden.forEach { (mac, n) -> prefs.edit().putString(mac, n).apply() }
            val offen = gefunden.filterKeys { ergebnis[it] != true }
            if (offen.isNotEmpty()) senden(adapter, offen, an, ergebnis)
            geraete.putAll(gefunden)
        }

        val ok = ergebnis.count { it.value }
        val fehlt = geraete.filterKeys { ergebnis[it] != true }.values.sorted()
        Logbuch.netz("Licht ${if (an) "an" else "aus"}", "$ok von ${geraete.size} geschaltet" + if (fehlt.isNotEmpty()) ", fehlt: ${fehlt.joinToString()}" else "")
        val wort = if (an) "an" else "aus"
        return when {
            geraete.isEmpty() -> Ergebnis("Ich habe keinen LED-Streifen gefunden. Sind sie in Reichweite und ist die KS-Lite-App geschlossen? Die App hält die Streifen sonst besetzt.", true)
            ok == 0 -> Ergebnis("Ich habe keinen der ${geraete.size} LED-Streifen erreicht. Ist die KS-Lite-App noch offen? Sie hält die Streifen besetzt; bitte schließen und noch einmal versuchen.", true)
            fehlt.isEmpty() -> Ergebnis("Alle $ok LED-Streifen sind $wort.")
            else -> Ergebnis("$ok von ${geraete.size} LED-Streifen sind $wort; nicht erreicht: ${fehlt.joinToString()} (außer Reichweite oder von der KS-Lite-App besetzt).")
        }
    }

    /** Sendet an alle [ziele] gleichzeitig (leicht versetzt) und trägt je Adresse den Erfolg in [ergebnis] ein. */
    private suspend fun senden(adapter: android.bluetooth.BluetoothAdapter, ziele: Map<String, String>, an: Boolean, ergebnis: ConcurrentHashMap<String, Boolean>) =
        coroutineScope {
            ziele.entries.mapIndexed { i, (mac, n) ->
                async {
                    delay(i * 150L)
                    val typ = TYPEN.firstOrNull { n.startsWith(it.vorsatz) } ?: STANDARD
                    var ok = false
                    for (versuch in 0..1) {
                        ok = runCatching { schreiben(adapter.getRemoteDevice(mac), typ, if (an) typ.an else typ.aus) }
                            .onFailure { Logbuch.fehler("Licht: $n", it) }.getOrDefault(false)
                        if (ok) break
                        delay(400)
                    }
                    ergebnis[mac] = ok
                }
            }.awaitAll()
        }

    /** Verbinden, Dienste suchen, Befehl schreiben, trennen. true = der Streifen hat den Befehl angenommen. */
    @SuppressLint("MissingPermission")
    private suspend fun schreiben(geraet: BluetoothDevice, typ: Typ, hex: String): Boolean {
        val fertig = CompletableDeferred<Boolean>()
        val wert = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val rueckruf = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    if (!gatt.discoverServices()) fertig.complete(false)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) fertig.complete(false)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                runCatching {
                    val k = kanal(gatt, typ) ?: return@runCatching fertig.complete(false)
                    val ohneAntwort = k.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0
                    val art = if (ohneAntwort) BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    val gesendet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(k, wert, art) == BluetoothGatt.GATT_SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        k.writeType = art
                        @Suppress("DEPRECATION")
                        k.value = wert
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(k)
                    }
                    // Ohne Antwort meldet der Streifen nichts zurück: angenommen ist, was abgeschickt wurde.
                    if (!gesendet || ohneAntwort) fertig.complete(gesendet)
                }.onFailure { fertig.complete(false) }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                fertig.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }
        val gatt = geraet.connectGatt(context, false, rueckruf, BluetoothDevice.TRANSPORT_LE) ?: return false
        return try {
            val ok = withTimeoutOrNull(VERBINDEN_MS) { fertig.await() } ?: false
            if (ok) delay(150) // Befehl aus dem Sendepuffer lassen, bevor getrennt wird.
            ok
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
    private suspend fun suchen(adapter: android.bluetooth.BluetoothAdapter): Map<String, String> {
        val scanner = adapter.bluetoothLeScanner ?: return emptyMap()
        val gefunden = ConcurrentHashMap<String, String>()
        val rueckruf = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                runCatching {
                    val n = result.scanRecord?.deviceName ?: result.device.name ?: return
                    if (TYPEN.any { n.startsWith(it.vorsatz) }) gefunden[result.device.address] = n
                }
            }

            override fun onScanFailed(errorCode: Int) { Logbuch.fehler("Licht: Bluetooth-Scan gescheitert", null, "Code $errorCode") }
        }
        val wach = context.getSystemService(PowerManager::class.java)?.isInteractive != false
        val namen = (BEKANNTE_NAMEN + bekannt().values).distinct()
        val filter = if (wach) null else namen.map { ScanFilter.Builder().setDeviceName(it).build() }
        scanner.startScan(filter, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), rueckruf)
        try {
            // Früher fertig, sobald alle bekannten Namen gesehen wurden.
            withTimeoutOrNull(SUCHE_MS) { while (!gefunden.values.containsAll(namen)) delay(200) }
        } finally {
            runCatching { scanner.stopScan(rueckruf) }
        }
        return gefunden
    }

    private companion object {
        const val VERBINDEN_MS = 8_000L
        const val SUCHE_MS = 5_000L
        fun uuid(kurz: String): UUID = UUID.fromString("0000$kurz-0000-1000-8000-00805f9b34fb")

        /** Aus der KS-Lite-App (UUIDBeanList, Cmd_KS0x, CmdBase); Reihenfolge wie dort: der erste passende Vorsatz gilt. */
        val TYPEN = listOf(
            Typ("KS01-", "AE00", "AE01", "ACF000CA", "AC0F00CA"),
            Typ("KS02-", "AE00", "AE01", "ACF000CA", "AC0F00CA"),
            Typ("KS03-", "FFF0", "FFF3", "7E0404F00101FF00EF", "7E0404100100FF00EF"),
            Typ("KS03~", "AFD0", "AFD1", "5BF000B5", "5B0F00B5"),
            Typ("KS04-", "FFF0", "FFF3", "7E0404F00101FF00EF", "7E0404100100FF00EF"),
            Typ("KS15~", "AFD0", "AFD1", "5BF000B5", "5B0F00B5"),
            Typ("KS", "AE00", "AE01", "5BF000B5", "5B0F00B5"),
        )
        val STANDARD = TYPEN.last()

        /** Franks sechs Streifen (Stand 19.11.2026, aus der Bluetooth-Liste des Handys), damit sie auch bei dunklem Bildschirm gefunden werden. */
        val BEKANNTE_NAMEN = listOf("KS03~005EAD", "KS03~2b2a71", "KS03~000F97", "KS03~e52cc0", "KS03~3601E0", "KS03~1101E3")
    }
}
