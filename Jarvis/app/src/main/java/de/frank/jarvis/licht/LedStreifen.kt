package de.frank.jarvis.licht

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks LED-Streifen (sonst App „KS Lite“ com.huaxin.smartlight, Gerätenamen „KS03~…“), direkt per Bluetooth, ohne die App.
 * Protokoll aus KS Lite 1.8 nachgebaut (unverschlüsselt, Befehlssatz DeviceCmd für „KS03~“): Dienst/Kanäle und Befehle je Namensvorsatz in [TYPEN].
 *
 * Gleicher Zustand überall: Jarvis merkt sich den zuletzt gewählten Zustand (Farbe, Helligkeit, Szene oder Mikrofon-Modus, prefs „licht_zustand“) und schickt
 * ihn bei jedem Einschalten an alle Streifen mit, denn jeder Streifen merkt sich sonst seinen eigenen letzten Modus. Ist noch nichts gemerkt, liest Jarvis die
 * Streifen (Abfrage 5F0100F5, Antwort 5F03…: an/aus, Farbe, Helligkeit; den Modus melden sie nicht) und übernimmt die Farbe der Mehrheit.
 *
 * Handy und Mac (Brücke, jarvis_licht.py über POST /geraet/licht) schalten gleichzeitig, jeder erreicht, was in seiner Reichweite ist; fertig, sobald beide
 * zusammen alle bekannten Streifen bestätigt haben. Adressen gefundener Streifen liegen in prefs „licht“.
 */
object LedStreifen {
    // ------------------------------------------------------------------ Zustand

    enum class Modus { FARBE, SZENE, RHYTHMUS }

    data class Zustand(
        val modus: Modus = Modus.FARBE, val rgb: String = "FF6000", val hell: Int = 100,
        val szene: Int = 0, val tempo: Int = 50, val rhythmus: Int = 1, val empfindlichkeit: Int = 7,
    ) {
        fun json(): String = JSONObject().put("modus", modus.name).put("rgb", rgb).put("hell", hell).put("szene", szene).put("tempo", tempo)
            .put("rhythmus", rhythmus).put("empf", empfindlichkeit).toString()

        /** Für Frank lesbar: „Orange, 80 Prozent“, „Szene bunter Sprung“, „Mikrofon-Modus Disco“. */
        fun text(): String = when (modus) {
            Modus.FARBE -> "${farbName(rgb)}, $hell Prozent"
            Modus.SZENE -> "Szene ${SZENEN.getOrElse(szene) { "${szene + 1}" }}, Tempo $tempo"
            Modus.RHYTHMUS -> "Mikrofon-Modus ${RHYTHMEN.getOrElse(rhythmus - 1) { "$rhythmus" }}"
        }

        companion object {
            fun aus(s: String): Zustand? = runCatching {
                val j = JSONObject(s)
                Zustand(Modus.valueOf(j.getString("modus")), j.getString("rgb"), j.getInt("hell"), j.getInt("szene"), j.getInt("tempo"), j.getInt("rhythmus"), j.getInt("empf"))
            }.getOrNull()
        }
    }

    /** Ergebnis eines Schaltvorgangs, für Sprache, Karte und Logbuch. */
    data class Bericht(val ok: Int, val gesamt: Int, val text: String, val fehler: Boolean, val zeit: Long = System.currentTimeMillis())

    /** Für die Startkarte: letzter Bericht und ob gerade geschaltet wird. */
    var letzterBericht by mutableStateOf<Bericht?>(null)
    var laeuft by mutableStateOf(false)
    /** Für die Karte: ob die Streifen zuletzt an (true) oder aus (false) geschaltet wurden; null = unbekannt. */
    var anAnzeige by mutableStateOf<Boolean?>(null)

    private fun zp(c: Context) = c.applicationContext.getSharedPreferences("licht_zustand", Context.MODE_PRIVATE)
    private fun adressen(c: Context) = c.applicationContext.getSharedPreferences("licht", Context.MODE_PRIVATE)

    fun zustand(c: Context): Zustand? = zp(c).getString("zustand", null)?.let { Zustand.aus(it) }
    @Synchronized fun merken(c: Context, z: Zustand) {
        aktuell = z
        zp(c).edit().putString("zustand", z.json()).apply { if (z.modus == Modus.FARBE) putBoolean("farbeGewaehlt", true) }.apply()
    }

    /** Wurde je eine Farbe gewählt oder gelesen? Sonst ist rgb nur die Vorgabe und darf nie gesendet werden. */
    fun farbeGewaehlt(c: Context): Boolean = zp(c).getBoolean("farbeGewaehlt", false) || zustand(c)?.modus == Modus.FARBE

    /** Ein Feld im gemerkten Zustand ändern (Regler), ohne einen parallel gesetzten Zustand mit einer alten Kopie zu überschreiben. */
    @Synchronized fun aendern(c: Context, f: (Zustand) -> Zustand) { zustand(c)?.let { merken(c, f(it)) } }

    /** Angezeigter Zustand (Karte): gemerkt, während des Ziehens auch der Live-Wunsch; jede Änderung über Sprache oder Automatik kommt hier an. */
    var aktuell by mutableStateOf<Zustand?>(null)
    fun aktuellLaden(c: Context) { if (aktuell == null) aktuell = zustand(c) }

    /** Was Jarvis zuletzt selbst geschaltet hat (true = an); null = noch nie. Nur Hinweis, wenn die Streifen nicht lesbar sind. */
    fun zuletztAn(c: Context): Boolean? = zp(c).takeIf { it.contains("an") }?.getBoolean("an", false)
    private fun anMerken(c: Context, an: Boolean) { anAnzeige = an; zp(c).edit().putBoolean("an", an).putLong("anZeit", System.currentTimeMillis()).apply() }

    // ------------------------------------------------------------------ Befehle

    class Typ(val vorsatz: String, val dienst: String, val kanal: String, val melde: String, val an: String, val aus: String, val alt: Boolean)

    private fun hex(n: Int) = "%02X".format(n.coerceIn(0, 255))

    /** Bytefolgen für den Zustand [z]: einschalten, Mikrofon-Modus beenden (außer er ist gewünscht), dann Farbe, Szene oder Mikrofon. */
    fun befehle(t: Typ, z: Zustand): List<String> {
        val h = hex(z.hell.coerceIn(1, 100))
        if (t.alt) return listOf(t.an) + when (z.modus) {
            Modus.SZENE -> listOf("7E0503${hex(z.szene + 128)}03FFFF00EF", "7E0402${hex(z.tempo.coerceIn(0, 100))}FFFFFF00EF", "7E0401${h}FFFFFF00EF")
            Modus.RHYTHMUS -> emptyList()
            Modus.FARBE -> listOf("7E070503${z.rgb}00EF", "7E0401${h}FFFFFF00EF")
        }
        val modus = when (z.modus) {
            Modus.FARBE -> listOf("5A0001${z.rgb}00${h}00A5")
            Modus.SZENE -> listOf("5C00${hex(z.szene.coerceIn(0, SZENEN.size - 1) + 128)}${hex(z.tempo.coerceIn(0, 100))}${h}00C5")
            Modus.RHYTHMUS -> listOf("5A0AF00${z.rhythmus.coerceIn(1, 4)}0${8 - z.empfindlichkeit.coerceIn(1, 7)}A5")
        }
        return listOf(t.an) + modus
    }

    // ------------------------------------------------------------------ Bedienen

    private val sperre = Mutex()

    /** Ausgangszustand für Änderungen (Helligkeit, Tempo): gemerkt, sonst die echte Mehrheit der Streifen (wird gemerkt), sonst null = nachfragen. */
    suspend fun basis(c: Context): Zustand? = zustand(c) ?: sperre.withLock { zustand(c) ?: mehrheit(c)?.also { merken(c, it) } }

    private val vorgaenge = java.util.concurrent.atomic.AtomicInteger(0)

    private suspend fun <T> beschaeftigt(arbeit: suspend () -> T): T = belegt { sperre.withLock { arbeit() } }

    private suspend fun <T> belegt(arbeit: suspend () -> T): T {
        vorgaenge.incrementAndGet(); laeuft = true
        try { return arbeit() } finally { laeuft = vorgaenge.decrementAndGet() > 0 }
    }

    /** Einschalten mit dem gemerkten Zustand; ist keiner gemerkt, vorher die Streifen lesen und die Farbe der Mehrheit übernehmen. */
    suspend fun an(c: Context): Bericht = beschaeftigt {
        // Ohne gemerkten und ohne lesbaren Zustand nur einschalten: nie eine geratene Farbe über Franks Einstellung schreiben.
        val z = zustand(c) ?: mehrheit(c)?.also { merken(c, it) }
        lauf(c, "an", { t -> if (z != null) befehle(t, z) else listOf(t.an) }) { if (z != null) "an (${z.text()})" else "an" }.also { if (it.ok > 0) anMerken(c, true) }
    }

    suspend fun aus(c: Context): Bericht = beschaeftigt {
        lauf(c, "aus", { t -> listOf(t.aus) }) { "aus" }.also { if (it.ok > 0) anMerken(c, false) }
    }

    /** Neuen Zustand merken und an alle schicken (schaltet ein). */
    suspend fun setzen(c: Context, z: Zustand): Bericht = beschaeftigt {
        merken(c, z)
        lauf(c, "setzen", { t -> befehle(t, z) }) { "auf ${z.text()}" }.also { if (it.ok > 0) anMerken(c, true) }
    }

    /** Liest die Streifen: true nur, wenn alle eigenen gelesen wurden und an sind; false sonst; null, wenn keiner lesbar war. */
    suspend fun istAn(c: Context): Boolean? = sperre.withLock {
        val gelesen = lesenAlle(c)
        when {
            gelesen.isEmpty() -> null
            gelesen.keys.map(::schluessel).toSet().containsAll(eigene(c)) && gelesen.values.all { it.an } -> true
            else -> false
        }
    }

    private class Meldung(val an: Boolean, val rgb: String, val hell: Int)

    /** Die Farbe, die die meisten erreichbaren Streifen gerade haben, als Zustand; null, wenn keiner lesbar war. */
    private suspend fun mehrheit(c: Context): Zustand? {
        val gelesen = lesenAlle(c).values.filter { it.rgb != "000000" }
        val zaehlung = gelesen.groupingBy { it.rgb to it.hell }.eachCount()
        val top = zaehlung.maxByOrNull { it.value } ?: return null
        if (top.value < 2 || zaehlung.count { it.value == top.value } > 1) return null
        val beste = top.key
        Logbuch.netz("Licht: Zustand übernommen", "${farbName(beste.first)} ${beste.second} % (${gelesen.size} Streifen gelesen)")
        return Zustand(Modus.FARBE, beste.first, beste.second.coerceIn(1, 100))
    }

    private suspend fun lauf(c: Context, art: String, befehl: (Typ) -> List<String>, wort: () -> String): Bericht =
        schalten(c.applicationContext, befehl, wort()).also { letzterBericht = it }

    fun erlaubt(c: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            c.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)

    /** Mac und Handy gleichzeitig; fertig, sobald zusammen alle bekannten Streifen bestätigt sind oder beide Wege durch sind. */
    private suspend fun schalten(c: Context, befehl: (Typ) -> List<String>, wort: String): Bericht = coroutineScope {
        val adapter = if (erlaubt(c)) c.getSystemService(BluetoothManager::class.java)?.adapter?.takeIf { it.isEnabled } else null
        val okHandy = ConcurrentHashMap.newKeySet<String>()
        val gesehenHandy = ConcurrentHashMap.newKeySet<String>()
        // Losgelöst: Die Anfrage blockiert bis zur Antwort des Macs; ein frühes Ende soll nicht auf sie warten.
        val mac = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).async { runCatching { ueberMac(c, befehl) }.getOrElse { MacErgebnis(emptySet(), emptySet(), "nicht erreichbar") } }
        val handy = async {
            if (adapter != null) withTimeoutOrNull(HANDY_FRIST_MS) {
                runCatching { handySchalten(c, adapter, befehl, okHandy, gesehenHandy) }
                    .onFailure { if (it !is kotlinx.coroutines.CancellationException) Logbuch.fehler("Licht: Handy-Weg", it) }
            }
        }
        val alleBekannt = eigene(c)
        while (!(handy.isCompleted && mac.isCompleted)) {
            val ok = okHandy.map(::schluessel) + (if (mac.isCompleted) mac.await().geschaltet.map(::schluessel) else emptyList())
            if (ok.toSet().containsAll(alleBekannt)) break
            delay(150)
        }
        handy.cancel()
        val m = (if (mac.isCompleted) mac.await() else MacErgebnis(emptySet(), emptySet(), null).also { mac.cancel() })
            .let { r -> MacErgebnis(r.geschaltet.filter { schluessel(it) in eigene(c) }.toSet(), r.gesehen.filter { schluessel(it) in eigene(c) }.toSet(), r.grund) }
        val ok = (okHandy + m.geschaltet).map(::schluessel).toSortedSet()
        val gesehen = (gesehenHandy + m.gesehen).map(::schluessel).toSet() + alleBekannt
        val gesamt = gesehen.size
        Logbuch.netz("Licht $wort", "Handy ${okHandy.size}/${gesehenHandy.size}, Mac ${m.geschaltet.size}/${m.gesehen.size}${m.grund?.let { " ($it)" } ?: ""}" +
            ", fehlt: ${(gesehen - ok).joinToString().ifEmpty { "nichts" }}")
        val macText = m.grund?.let { " Der Mac ist $it." } ?: ""
        when {
            ok.isEmpty() && adapter == null -> Bericht(0, gesamt, "Ich habe keinen LED-Streifen erreicht: Bluetooth am Handy ist aus oder nicht erlaubt.$macText", true)
            ok.isEmpty() -> Bericht(0, gesamt, "Ich habe keinen LED-Streifen erreicht. Ist die KS-Lite-App noch offen? Sie hält die Streifen besetzt; bitte schließen und noch einmal versuchen.$macText", true)
            ok.size >= gesamt -> Bericht(ok.size, gesamt, "Alle ${ok.size} LED-Streifen sind $wort.", false)
            else -> Bericht(ok.size, gesamt, "${ok.size} von $gesamt LED-Streifen sind $wort; ${gesamt - ok.size} waren weder vom Handy noch vom Mac erreichbar.$macText", false)
        }
    }

    // ------------------------------------------------------------------ Handy

    /** Nur Franks Streifen, nie fremde in Bluetooth-Reichweite (Nachbarn). */
    private fun eigene(c: Context): Set<String> = BEKANNTE_NAMEN.map(::schluessel).toSet()

    private fun bekannt(c: Context): Map<String, String> {
        val alle = adressen(c).all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
        val fremd = alle.filterValues { schluessel(it) !in eigene(c) }
        if (fremd.isNotEmpty()) adressen(c).edit().apply { fremd.keys.forEach { remove(it) } }.apply()
        return alle - fremd.keys
    }

    @Volatile private var letzteSuche = 0L

    /** Neue Adresse merken und ältere Adressen desselben Streifens (getauscht, zurückgesetzt) entfernen. */
    private fun adresseMerken(c: Context, mac: String, n: String) {
        val k = schluessel(n)
        adressen(c).edit().apply {
            adressen(c).all.forEach { (alt, name) -> if (alt != mac && name is String && schluessel(name) == k) remove(alt) }
            putString(mac, n)
        }.apply()
    }

    /** Nach 6 Fehlschlägen in Folge ist eine Adresse tot (der Streifen meldet sich bei der nächsten Suche neu). */
    private fun fehlversuche(c: Context, mac: String, fehler: Boolean) {
        val f = c.applicationContext.getSharedPreferences("licht_fehl", Context.MODE_PRIVATE)
        val n = if (fehler) f.getInt(mac, 0) + 1 else 0
        if (n >= 6) { adressen(c).edit().remove(mac).apply(); f.edit().remove(mac).apply(); Logbuch.netz("Licht: Adresse entfernt", "$mac nach 6 Fehlschlägen") }
        else f.edit().putInt(mac, n).apply()
    }

    private suspend fun handySchalten(c: Context, adapter: BluetoothAdapter, befehl: (Typ) -> List<String>, ok: MutableSet<String>, gesehen: MutableSet<String>) {
        val geraete = HashMap(bekannt(c))
        val erledigt = ConcurrentHashMap<String, Boolean>()
        suspend fun senden(ziele: Map<String, String>) = coroutineScope {
            ziele.entries.mapIndexed { i, (mac, n) ->
                async {
                    delay(i * 120L)
                    val t = typ(n)
                    for (versuch in 0..1) {
                        val gut = runCatching { sitzung(c, adapter.getRemoteDevice(mac), t) { s -> s.schreiben(befehl(t)) } ?: false }
                            .onFailure { if (it !is kotlinx.coroutines.CancellationException) Logbuch.fehler("Licht: $n", it) }.getOrDefault(false)
                        if (gut) { ok.add(n); erledigt[mac] = true; fehlversuche(c, mac, false); return@async }
                        delay(300)
                    }
                    erledigt[mac] = false
                    fehlversuche(c, mac, true)
                }
            }.awaitAll()
        }
        gesehen.addAll(geraete.values)
        if (geraete.isNotEmpty()) senden(geraete)
        val fehlt = geraete.isEmpty() || erledigt.values.any { !it } || geraete.values.map(::schluessel).toSet().size < eigene(c).size
        if (fehlt && (geraete.isEmpty() || System.currentTimeMillis() - letzteSuche > SUCHE_ABSTAND_MS)) {
            letzteSuche = System.currentTimeMillis()
            val neu = suchen(c, adapter)
            neu.forEach { (mac, n) -> adresseMerken(c, mac, n) }
            gesehen.addAll(neu.values)
            val offen = neu.filterKeys { !erledigt.containsKey(it) }
            if (offen.isNotEmpty()) senden(offen)
        }
    }

    /** Liest alle erreichbaren Streifen übers Handy (bekannte direkt, sonst nach kurzer Suche). */
    private suspend fun lesenAlle(c: Context): Map<String, Meldung> {
        if (!erlaubt(c)) return emptyMap()
        val adapter = c.getSystemService(BluetoothManager::class.java)?.adapter?.takeIf { it.isEnabled } ?: return emptyMap()
        val ziele = bekannt(c).ifEmpty { suchen(c, adapter).also { it.forEach { (mac, n) -> adressen(c).edit().putString(mac, n).apply() } } }
        val ergebnis = ConcurrentHashMap<String, Meldung>()
        coroutineScope {
            ziele.entries.mapIndexed { i, (mac, n) ->
                async {
                    delay(i * 120L)
                    runCatching { sitzung(c, adapter.getRemoteDevice(mac), typ(n)) { s -> s.abfragen() } }.getOrNull()?.let { roh -> melden(roh)?.let { ergebnis[n] = it } }
                }
            }.awaitAll()
        }
        return ergebnis
    }

    /** Antwort auf 5F0100F5 (KS Lite CmdTime.getLedBeanState): 5F03 + Art + F0 (an) + R G B W + Helligkeit + Tempo. */
    private fun melden(roh: String): Meldung? {
        if (roh.length < 18 || roh.take(4) !in setOf("5F03", "5F04", "5F06", "5F08")) return null
        return Meldung(roh.substring(6, 8) == "F0", roh.substring(8, 14), roh.substring(16, 18).toInt(16))
    }

    /** Eine Verbindung: verbinden, Dienste suchen, [arbeit] mit der Sitzung, trennen. null = nicht verbunden. */
    @SuppressLint("MissingPermission")
    private suspend fun <T> sitzung(c: Context, geraet: BluetoothDevice, typ: Typ, arbeit: suspend (Sitzung) -> T): T? {
        val s = Sitzung()
        return try {
            withTimeoutOrNull(VERBINDEN_MS) { if (oeffnen(c, geraet, typ, s)) arbeit(s) else null }
        } finally { s.schliessen() }
    }

    /** Verbinden und Kanäle suchen; false = nicht verbunden (die Sitzung muss trotzdem geschlossen werden). */
    @SuppressLint("MissingPermission")
    private suspend fun oeffnen(c: Context, geraet: BluetoothDevice, typ: Typ, s: Sitzung): Boolean {
        val gatt = geraet.connectGatt(c, false, s.rueckruf, BluetoothDevice.TRANSPORT_LE) ?: return false
        s.gatt = gatt
        if (!s.bereit.await()) return false
        s.kanal = (listOf(typ) + TYPEN).firstNotNullOfOrNull { t -> gatt.getService(uuid(t.dienst))?.getCharacteristic(uuid(t.kanal)) } ?: return false
        s.melder = (listOf(typ) + TYPEN).firstNotNullOfOrNull { t -> gatt.getService(uuid(t.dienst))?.getCharacteristic(uuid(t.melde)) }
        return true
    }

    // ------------------------------------------------------------------ Live (Farbkreis, Helligkeit)

    /**
     * Solange Frank am Farbkreis oder Helligkeitsregler zieht, bleiben die Verbindungen zu den erreichbaren Streifen offen und jede Änderung geht sofort
     * hinaus (zwischendurch Übersprungenes zählt nicht, es gilt immer der neueste Wunsch). Beim Loslassen [liveEnde]: Verbindungen zu, dann der
     * normale Weg über Handy und Mac für alle Streifen, und der Zustand wird gemerkt. Nach 20 s ohne Änderung schließt sich alles von selbst.
     */
    private val liveBereich = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val liveSitzungen = ConcurrentHashMap<String, Pair<Sitzung, Typ>>()
    private val liveWunsch = kotlinx.coroutines.flow.MutableStateFlow<Zustand?>(null)
    @Volatile private var liveJob: kotlinx.coroutines.Job? = null
    @Volatile private var macAbbruch: kotlinx.coroutines.Job? = null
    @Volatile private var liveZuletzt = 0L

    fun liveStart(c: Context) {
        liveZuletzt = System.currentTimeMillis()
        if (liveJob?.isActive == true || liveEndet || !erlaubt(c)) return
        val adapter = c.getSystemService(BluetoothManager::class.java)?.adapter?.takeIf { it.isEnabled } ?: return
        val app = c.applicationContext
        val erstellt = System.currentTimeMillis()
        macAbbruch = liveBereich.launch { runCatching { macAbbrechen(app, erstellt) } }
        liveJob = liveBereich.launch {
            bekannt(app).entries.forEachIndexed { i, (mac, n) ->
                launch {
                    delay(i * 120L)
                    val s = Sitzung()
                    val t = typ(n)
                    var uebergeben = false
                    try {
                        if (withTimeoutOrNull(VERBINDEN_MS) { oeffnen(app, adapter.getRemoteDevice(mac), t, s) } == true && isActive) {
                            liveSitzungen[mac] = s to t
                            uebergeben = true
                            liveWunsch.value?.let { z -> runCatching { s.schreiben(befehle(t, z), nachlauf = false) } }
                        }
                    } finally {
                        // Abgebrochen (losgelassen) oder nicht verbunden: nie eine offene Verbindung zurücklassen.
                        if (!uebergeben || !isActive) { liveSitzungen.remove(mac); s.schliessen() }
                    }
                }
            }
            launch {
                liveWunsch.collect { z ->
                    if (z == null) return@collect
                    coroutineScope { liveSitzungen.values.map { (s, t) -> async { runCatching { s.schreiben(befehle(t, z), nachlauf = false) } } }.awaitAll() }
                }
            }
            while (System.currentTimeMillis() - liveZuletzt < 20_000L) delay(1_000)
            // Ohne Loslassen beendet (Karte zugeklappt, Geste abgebrochen): letzten Wunsch noch fest übernehmen.
            val letzter = liveWunsch.value
            liveSitzungenSchliessen()
            liveWunsch.value = null
            liveJob = null
            if (letzter != null) liveBereich.launch { runCatching { setzen(app, letzter) } }
            coroutineContext.cancelChildren()
        }
    }

    @Volatile private var liveEndet = false

    fun live(z: Zustand) { liveZuletzt = System.currentTimeMillis(); aktuell = z; liveWunsch.value = z }

    private fun liveSitzungenSchliessen() {
        liveSitzungen.values.forEach { it.first.schliessen() }
        liveSitzungen.clear()
    }

    /** Loslassen: erst den Live-Job beenden (keine Verbindung kommt mehr hinzu), dann alle schließen, den Mac-Abbruch abwarten, dann [z] wie gewohnt. */
    suspend fun liveEnde(c: Context, z: Zustand): Bericht = belegt {
        liveEndet = true
        try {
            liveJob?.let { j -> liveJob = null; j.cancelAndJoin() }
            liveSitzungenSchliessen()
            liveWunsch.value = null
            // Kommt der Abbruch spät an, verwirft der Relay ihn (älter als dieser Befehl, „erstellt“); hier nur kurz warten.
            withTimeoutOrNull(1_500) { macAbbruch?.join() }
            delay(300) // die Streifen kurz freigeben, bevor der normale Weg neu verbindet
            setzen(c, z)
        } finally { liveEndet = false }
    }

    // ------------------------------------------------------------------ Farbplätze

    /** Franks 16 Farbplätze (tippen = einstellen, lange drücken = aktuelle Farbe speichern); Platz 1 ist ab Werk sein Orange aus KS Lite. */
    fun plaetze(c: Context): List<String> {
        val gespeichert = runCatching { JSONArray(zp(c).getString("plaetze", "[]")) }.getOrNull()
        return (0 until PLAETZE).map { i -> gespeichert?.optString(i)?.takeIf { it.matches(Regex("[0-9A-Fa-f]{6}")) }?.uppercase() ?: STANDARD_PLAETZE[i] }
    }

    fun platzSpeichern(c: Context, platz: Int, rgb: String) {
        val neu = plaetze(c).toMutableList().also { it[platz.coerceIn(0, PLAETZE - 1)] = rgb.uppercase() }
        zp(c).edit().putString("plaetze", JSONArray(neu).toString()).apply()
    }

    @SuppressLint("MissingPermission")
    private class Sitzung {
        var gatt: BluetoothGatt? = null
        fun schliessen() { gatt?.let { g -> runCatching { g.disconnect() }; runCatching { g.close() } }; gatt = null }
        var kanal: BluetoothGattCharacteristic? = null
        var melder: BluetoothGattCharacteristic? = null
        val bereit = CompletableDeferred<Boolean>()
        @Volatile var geschrieben = CompletableDeferred<Boolean>()
        @Volatile var beschrieben = CompletableDeferred<Boolean>()
        @Volatile var antwort = CompletableDeferred<String>()

        val rueckruf = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    bereit.complete(false); geschrieben.complete(false); beschrieben.complete(false)
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (!g.discoverServices()) bereit.complete(false)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    bereit.complete(false); geschrieben.complete(false); beschrieben.complete(false)
                }
            }
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) { bereit.complete(status == BluetoothGatt.GATT_SUCCESS) }
            override fun onCharacteristicWrite(g: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int) { geschrieben.complete(status == BluetoothGatt.GATT_SUCCESS) }
            override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) { beschrieben.complete(status == BluetoothGatt.GATT_SUCCESS) }
            override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray) { melde(value) }
            @Deprecated("Vor Android 13")
            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ch.value?.let { melde(it) }
            }
        }

        fun melde(v: ByteArray) { v.joinToString("") { "%02X".format(it) }.takeIf { it.startsWith("5F0") }?.let { antwort.complete(it) } }

        /** Ein Schreibvorgang zur Zeit je Verbindung (Live: Erstwunsch und Sammelschleife dürfen sich nicht überholen). */
        private val schreibSperre = Mutex()

        suspend fun schreiben(liste: List<String>, nachlauf: Boolean = true): Boolean = schreibSperre.withLock { schreibenOhneSperre(liste, nachlauf) }

        private suspend fun schreibenOhneSperre(liste: List<String>, nachlauf: Boolean): Boolean {
            val gatt = gatt ?: return false
            val k = kanal ?: return false
            val ohneAntwort = k.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0
            val art = if (ohneAntwort) BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            for (hex in liste) {
                geschrieben = CompletableDeferred()
                val wert = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val gesendet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) gatt.writeCharacteristic(k, wert, art) == BluetoothGatt.GATT_SUCCESS
                    else @Suppress("DEPRECATION") run { k.writeType = art; k.value = wert; gatt.writeCharacteristic(k) }
                if (!gesendet) return false
                // Ohne Antwort meldet der Streifen nichts: kurz Luft lassen. Mit Antwort auf die Bestätigung warten.
                if (ohneAntwort) { if (withTimeoutOrNull(300) { geschrieben.await() } == null) delay(40) else delay(15) }
                else if (withTimeoutOrNull(800) { geschrieben.await() } != true) return false
            }
            if (nachlauf) delay(150) // Befehle aus dem Sendepuffer lassen, bevor getrennt wird.
            return true
        }

        /** Meldungen einschalten, Zustand abfragen, bis 1,5 s auf die Antwort warten. */
        suspend fun abfragen(): String? {
            val gatt = gatt ?: return null
            val m = melder ?: return null
            if (!gatt.setCharacteristicNotification(m, true)) return null
            val d = m.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (d != null) {
                beschrieben = CompletableDeferred()
                val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) gatt.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothGatt.GATT_SUCCESS
                    else @Suppress("DEPRECATION") run { d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; gatt.writeDescriptor(d) }
                if (ok) withTimeoutOrNull(1500) { beschrieben.await() }
            }
            antwort = CompletableDeferred()
            if (!schreiben(listOf(ABFRAGE))) return null
            return withTimeoutOrNull(1500) { antwort.await() }
        }
    }

    /** Sucht [SUCHE_MS] nach Streifen (Name beginnt mit „KS“). Bei dunklem Bildschirm liefert Android nur gefilterte Scans: dann nach bekannten Namen. */
    @SuppressLint("MissingPermission")
    private suspend fun suchen(c: Context, adapter: BluetoothAdapter): Map<String, String> {
        val scanner = adapter.bluetoothLeScanner ?: return emptyMap()
        val gefunden = ConcurrentHashMap<String, String>()
        val rueckruf = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                runCatching {
                    val n = result.scanRecord?.deviceName ?: result.device.name ?: return
                    if (schluessel(n) in eigene(c)) gefunden[result.device.address] = n
                }
            }
            override fun onScanFailed(errorCode: Int) { Logbuch.fehler("Licht: Bluetooth-Scan gescheitert", null, "Code $errorCode") }
        }
        val wach = c.getSystemService(PowerManager::class.java)?.isInteractive != false
        val namen = (BEKANNTE_NAMEN + bekannt(c).values).distinct()
        val filter = if (wach) null else namen.map { ScanFilter.Builder().setDeviceName(it).build() }
        if (runCatching { scanner.startScan(filter, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), rueckruf) }
                .onFailure { Logbuch.fehler("Licht: Bluetooth-Scan nicht gestartet", it) }.isFailure) return gefunden
        try {
            withTimeoutOrNull(SUCHE_MS) { while (!gefunden.values.map(::schluessel).toSet().containsAll(eigene(c))) delay(200) }
        } finally { runCatching { scanner.stopScan(rueckruf) } }
        return gefunden
    }

    // ------------------------------------------------------------------ Mac

    private class MacErgebnis(val geschaltet: Set<String>, val gesehen: Set<String>, val grund: String?)

    private fun macFehler(roh: String): String {
        Logbuch.netz("Licht: Fehler am Mac", roh.take(300))
        val t = roh.lowercase()
        return when {
            "turned off" in t || "powered off" in t || "poweredoff" in t -> "ohne Bluetooth (am Mac ausgeschaltet)"
            "bleak" in t && ("no module" in t || "modulenotfound" in t) -> "ohne Bluetooth-Baustein (bleak fehlt)"
            "not authorized" in t || "unauthorized" in t || "tcc" in t -> "ohne Bluetooth-Freigabe"
            "timeout" in t || "timed out" in t -> "zu langsam (Zeitüberschreitung)"
            else -> "mit einem Fehler (Einzelheiten im Logbuch)"
        }
    }

    /** Der Mac schaltet per Bluetooth (Brücke, jarvis_licht.py); er bekommt die Bytefolgen je Namensvorsatz. */
    private suspend fun ueberMac(c: Context, befehl: (Typ) -> List<String>): MacErgebnis = withContext(Dispatchers.IO) {
        val e = Einstellungen.get(c)
        if (e.serverToken.isBlank()) return@withContext MacErgebnis(emptySet(), emptySet(), "nicht eingerichtet")
        val befehle = JSONObject().apply { TYPEN.forEach { t -> put(t.vorsatz, JSONArray(befehl(t))) }; put("_namen", JSONArray(BEKANNTE_NAMEN)) }
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/licht").header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung)
            .post(JSONObject().put("befehle", befehle).put("erstellt", System.currentTimeMillis()).toString().toRequestBody("application/json".toMediaType())).build()
        CLIENT.newCall(anfrage).execute().use { a ->
            if (!a.isSuccessful) return@withContext MacErgebnis(emptySet(), emptySet(), "nicht erreichbar (Relay ${a.code})")
            val j = JSONObject(a.body?.string().orEmpty())
            fun menge(k: String) = j.optJSONArray(k)?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() } ?: emptySet()
            when (j.optString("ergebnis")) {
                "fertig" -> MacErgebnis(menge("geschaltet"), menge("gesehen"), j.optString("fehler").takeIf { it.isNotBlank() }?.let { macFehler(it) })
                "mac_nicht_verbunden" -> MacErgebnis(emptySet(), emptySet(), "nicht verbunden")
                "abgeloest" -> MacErgebnis(emptySet(), emptySet(), null)
                else -> MacErgebnis(emptySet(), emptySet(), "ohne Rückmeldung")
            }
        }
    }

    /** Laufenden Licht-Auftrag am Mac beenden (leere Befehle: die Brücke bricht ab und meldet sofort). */
    private suspend fun macAbbrechen(c: Context, erstellt: Long) = withContext(Dispatchers.IO) {
        val e = Einstellungen.get(c)
        if (e.serverToken.isBlank()) return@withContext
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/licht").header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung)
            .post(JSONObject().put("befehle", JSONObject()).put("erstellt", erstellt).toString().toRequestBody("application/json".toMediaType())).build()
        CLIENT.newCall(anfrage).execute().close()
    }

    // ------------------------------------------------------------------ Namen und Tabellen

    /** Mac und Handy nennen denselben Streifen teils mit vertauschten Bytes (KS03~000F97 / KS03~97000F): Vorsatz plus sortierte Bytes. */
    fun schluessel(n: String): String {
        val vor = n.takeWhile { it != '~' && it != '-' }
        val rest = n.drop(vor.length + 1).uppercase()
        return if (rest.length % 2 == 0 && rest.isNotEmpty()) vor + "~" + rest.chunked(2).sorted().joinToString("") else n.uppercase()
    }

    fun typ(name: String): Typ = TYPEN.first { name.startsWith(it.vorsatz) || it.vorsatz == "KS" }

    /** Farbe aus Franks Worten: Name aus [FARBEN] oder #RRGGBB. */
    fun farbe(f: String): String? {
        fun norm(s: String) = s.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss").replace(Regex("[^a-z0-9#]"), "")
        val k = norm(f)
        Regex("#?([0-9a-f]{6})").matchEntire(k)?.let { return it.groupValues[1].uppercase() }
        val k2 = k.replace("warmes", "warm").replace("kaltes", "kalt")
        return FARBEN.entries.firstOrNull { norm(it.key) == k2 }?.value
            ?: FARBEN.entries.sortedByDescending { it.key.length }.firstOrNull { k2.contains(norm(it.key)) }?.value
    }

    fun farbName(rgb: String): String = FARBEN.entries.firstOrNull { it.value.equals(rgb, true) }?.key?.replaceFirstChar { it.uppercase() } ?: "#$rgb"

    /** Szene aus Franks Worten: Nummer 1–23 oder Name. */
    fun szene(s: String): Int? {
        val t = s.lowercase().trim()
        t.filter(Char::isDigit).toIntOrNull()?.let { if (it in 1..SZENEN.size) return it - 1 }
        return SZENEN.indexOfFirst { it.lowercase() == t }.takeIf { it >= 0 } ?: SZENEN.indexOfFirst { t.contains(it.lowercase()) || it.lowercase().contains(t) }.takeIf { it >= 0 && t.length >= 3 }
    }

    const val VERBINDEN_MS = 8_000L
    const val SUCHE_MS = 5_000L
    private const val SUCHE_ABSTAND_MS = 120_000L
    private const val HANDY_FRIST_MS = 22_000L
    private const val ABFRAGE = "5F0100F5"
    private val CLIENT: OkHttpClient = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).build()
    fun uuid(kurz: String): UUID = UUID.fromString("0000$kurz-0000-1000-8000-00805f9b34fb")

    /** Aus KS Lite 1.8 (UUIDBeanList, Cmd_KS0x, CmdBase); der erste passende Vorsatz gilt. alt = 7E-Befehlssatz (KS03-/KS04-). */
    val TYPEN = listOf(
        Typ("KS01-", "AE00", "AE01", "AE02", "ACF000CA", "AC0F00CA", false),
        Typ("KS02-", "AE00", "AE01", "AE01", "ACF000CA", "AC0F00CA", false),
        Typ("KS03-", "FFF0", "FFF3", "FFF3", "7E0404F00101FF00EF", "7E0404100100FF00EF", true),
        Typ("KS03~", "AFD0", "AFD1", "AFD2", "5BF000B5", "5B0F00B5", false),
        Typ("KS04-", "FFF0", "FFF3", "FFF3", "7E0404F00101FF00EF", "7E0404100100FF00EF", true),
        Typ("KS15~", "AFD0", "AFD1", "AFD2", "5BF000B5", "5B0F00B5", false),
        Typ("KS", "AE00", "AE01", "AE02", "5BF000B5", "5B0F00B5", false),
    )

    /** Franks sechs Streifen (Stand 19.11.2026, Bluetooth-Liste des Handys): Wohnzimmer, Wohnzimmer 2, Wohnzimmer 3, Flur, Küche, Bad. */
    val BEKANNTE_NAMEN = listOf("KS03~005EAD", "KS03~2b2a71", "KS03~000F97", "KS03~e52cc0", "KS03~3601E0", "KS03~1101E3")

    val FARBEN = linkedMapOf(
        "orange" to "FF2800", "warmweiß" to "FFA03C", "kaltweiß" to "D0E0FF", "weiß" to "FFFFFF", "rot" to "FF0000", "gelb" to "FFC800",
        "grün" to "00FF00", "türkis" to "00FFC0", "cyan" to "00FFFF", "hellblau" to "40A0FF", "blau" to "0000FF", "lila" to "8000FF",
        "violett" to "8000FF", "magenta" to "FF00FF", "pink" to "FF2080", "rosa" to "FF6080",
    )

    const val PLAETZE = 16
    /** Vorgaben der 16 Plätze, bis Frank eigene speichert (lange drücken). Die Streifen zeigen Codes anders als der Bildschirm: Frank stellt Farben live am Farbkreis ein. */
    val STANDARD_PLAETZE = listOf("FF2800", "FF1400", "FFA03C", "D0E0FF", "FFFFFF", "FF0000", "FFC800", "00FF00",
        "00FFC0", "00FFFF", "40A0FF", "0000FF", "8000FF", "FF00FF", "FF2080", "FF6080")

    /** Szenen 128–150 aus KS Lite (deutsche Namen der App). */
    val SZENEN = listOf(
        "bunter Sprung", "dreifarbiger Sprung", "bunte Farbverläufe", "dreifarbige Farbverläufe", "rote Farbverläufe", "grüne Farbverläufe",
        "blaue Farbverläufe", "gelbe Farbverläufe", "blaugrüne Farbverläufe", "lila Farbverläufe", "weiße Farbverläufe", "rot-grüne Farbverläufe",
        "rot-blaue Farbverläufe", "grün-blaue Farbverläufe", "bunte Strobe", "dreifarbige Strobe", "rote Strobe", "grüne Strobe", "blaue Strobe",
        "gelbe Strobe", "blaugrüne Strobe", "lila Strobe", "weiße Strobe",
    )

    /** Mikrofon-Modi der Streifen (eingebautes Mikrofon, KS Lite „Rhythmus“): 1 bis 4. */
    val RHYTHMEN = listOf("Klassisch", "Sanft", "Dynamisch", "Disco")
}
