package de.frank.jarvis.faehigkeit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Tenda-Mobilfunkrouter (5G03, O2-SIM) über die lokale Verwaltungsoberfläche (http://192.168.0.1, wie tendawifi.com).
 * Abruf: 1. der Mac zu Hause (Mac-Brücke, werkzeuge/pc-bruecke/jarvis_tenda.py, über den Relay: frische Momentabfrage, sonst sein
 * stündlicher Stand), 2. liefert er nicht und ist das Handy im Heim-WLAN, liest es den Router selbst, 3. sonst der letzte Stand,
 * deutlich als veraltet gekennzeichnet. Unterwegs ohne Mac gibt es keine frischen Werte: Die Router-Verwaltung ist bewusst nicht aus
 * dem Internet erreichbar, und die Tenda-App zeigt keine Messwerte.
 * Zugang: nur der MD5-Hash des Passworts (so meldet sich auch die Weboberfläche an) in [SecureSettings]; der Mac bekommt ihn einmal über den
 * Relay. Der Router sperrt nach 4 Fehlversuchen: Darum genau ein Anmeldeversuch je Lauf und nach „falsch“ keiner mehr bis zur Neueingabe.
 * Bewertung und Multiplayer-Einschätzung entstehen nur hier ([struktur]); Mac-, Windows-Jarvis und Sprache zeigen denselben Text.
 */
class TendaFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "tenda"
    override val name = "Tenda Internet"
    override val beschreibung = "Franks Mobilfunkrouter zu Hause (Tenda 5G03, O2): Modus, Ping, Empfangswerte, Moduswechsel, Neuverbinden, Neustart."
    override val hinweise =
        "Fragen zum Heim-Internet („In welchem Modus bin ich?“, „Kann ich zu Hause gerade gut Call of Duty spielen?“, „Wie ist der Empfang?“) " +
            "beantwortest du mit tenda_lesen aus den gemessenen Werten: Modus, dann die Multiplayer-Einschätzung aus Ping, Jitter und Paketverlust, " +
            "Empfangswerte nur bei Bedarf. Nenne Messzeit und Quelle, wenn der Stand nicht frisch ist; erfinde keine fehlenden Werte. " +
            "Moduswechsel (tenda_modus_setzen) und Neuverbinden (tenda_verbinden) unterbrechen das Internet kurz; sag das vorher. " +
            "Einen Neustart (tenda_neustart) nur nach Franks Ja auf „Willst du den Router wirklich neu starten?“."

    private val e get() = Einstellungen.get(context)
    private val letzterDatei get() = File(context.filesDir, "tenda-letzter.json")

    override fun stoerung(): String? = if (hash().isBlank()) "Router-Passwort noch nicht eingerichtet (Karte „Tenda Internet“ aufklappen)." else null

    // ------------------------------------------------------------------ Zustand für die Karte

    /** Was die Karte zeigt: Stand, ob gerade gelesen oder geschaltet wird, und die Meldung zur letzten Aktion. */
    data class Zustand(val stand: Stand? = null, val liest: Boolean = false, val schaltet: String? = null, val meldung: String? = null)

    private val _zustand = MutableStateFlow(Zustand())
    val zustand: StateFlow<Zustand> = _zustand.asStateFlow()
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ------------------------------------------------------------------ Werkzeuge

    override val werkzeuge = listOf(
        Werkzeug(
            name = "tenda_lesen",
            titel = "Heim-Internet lesen",
            beschreibung = "Frische Momentabfrage des Tenda-Routers zu Hause: aktiver und eingestellter Modus (5G SA, 5G NSA, 4G), Multiplayer-Einschätzung " +
                "aus Ping, Jitter und Paketverlust des Heimanschlusses, LTE- und 5G-Bänder und Empfangswerte (RSRP, RSRQ, SINR, CQI, RSSI, SS-Werte) mit Bewertung, " +
                "Messzeit und Quelle. Erst über den Mac zu Hause, sonst direkt im Heim-WLAN, sonst der letzte Stand (als veraltet markiert).",
            schema = JSONObject().put("type", "object").put("properties", JSONObject()),
            nurLesen = true,
            taetigkeit = "liest den Router",
        ) {
            val s = abrufen()
            if (s == null) Ergebnis("Vom Tenda-Router liegt noch kein Stand vor. " + (stoerung() ?: "Weder der Mac zu Hause noch das Handy im Heim-WLAN konnten ihn lesen."), fehler = true)
            else Ergebnis(antwort(s), struktur = struktur(s))
        },
        Werkzeug(
            name = "tenda_modus_setzen",
            titel = "Router-Modus wechseln",
            beschreibung = "Stellt den Mobilfunkmodus des Tenda-Routers um: 5g_sa (5G SA bevorzugt), 5g_nsa (5G NSA) oder 4g (nur 4G). Das Internet " +
                "ist dabei ein bis drei Minuten weg; Jarvis prüft danach selbst den neuen Zustand. Nicht erneut aufrufen, wenn das Ergebnis unklar ist.",
            schema = JSONObject().put("type", "object").put("properties", JSONObject().put("modus", JSONObject().put("type", "string")
                .put("enum", JSONArray(listOf("5g_sa", "5g_nsa", "4g"))))).put("required", JSONArray(listOf("modus"))),
            nurLesen = false,
            taetigkeit = "stellt den Router-Modus um",
        ) { a ->
            val wert = when (a.optString("modus")) { "5g_sa" -> "5"; "5g_nsa" -> "4"; "4g" -> "2"; else -> "" }
            if (wert.isEmpty()) Ergebnis("Unbekannter Modus.", fehler = true) else schalten("modus", wert).let { Ergebnis(it.first, fehler = !it.second) }
        },
        Werkzeug(
            name = "tenda_verbinden",
            titel = "Router neu verbinden",
            beschreibung = "Trennt die Mobilfunkverbindung des Tenda-Routers und verbindet sie mit seiner eingestellten Standardkonfiguration neu (Connect). " +
                "Das Internet ist dabei kurz weg.",
            schema = JSONObject().put("type", "object").put("properties", JSONObject()),
            nurLesen = false,
            taetigkeit = "verbindet den Router neu",
        ) { schalten("verbinden", "").let { Ergebnis(it.first, fehler = !it.second) } },
        Werkzeug(
            name = "tenda_neustart",
            titel = "Router neu starten",
            beschreibung = "Startet den Tenda-Router wirklich neu (zwei bis vier Minuten kein Internet). Vorher Frank fragen: „Willst du den Router wirklich " +
                "neu starten?“ und nur nach seinem Ja mit bestaetigt=true aufrufen.",
            schema = JSONObject().put("type", "object").put("properties", JSONObject().put("bestaetigt", JSONObject().put("type", "boolean")))
                .put("required", JSONArray(listOf("bestaetigt"))),
            nurLesen = false,
            taetigkeit = "startet den Router neu",
        ) { a ->
            if (!a.optBoolean("bestaetigt")) Ergebnis("Noch nicht ausgeführt. Frag Frank: „Willst du den Router wirklich neu starten?“ und ruf erst nach seinem Ja mit bestaetigt=true auf.")
            else schalten("neustart", "").let { Ergebnis(it.first, fehler = !it.second) }
        },
    )

    // ------------------------------------------------------------------ Stand

    /**
     * Ein Stand, wie ihn Mac (jarvis_tenda.py) oder Handy liefern: rohe Router-Felder ohne Kennungen ([simInfo], [simWan]),
     * Ping des Heimanschlusses, Messzeit (Sekunden seit 1970) und Quelle. [veraltetGrund] sagt, warum kein frischer Stand kam.
     */
    data class Stand(val roh: JSONObject, val quelle: String, val veraltetGrund: String? = null) {
        val zeit: Long get() = roh.optLong("zeit")
        val simInfo: JSONObject get() = roh.optJSONObject("sim_info") ?: JSONObject()
        val simWan: JSONObject get() = roh.optJSONObject("sim_wan") ?: JSONObject()
        val ping: JSONObject? get() = roh.optJSONObject("ping")
    }

    fun letzter(): Stand? = runCatching { JSONObject(letzterDatei.readText()) }.getOrNull()?.let { j ->
        j.optJSONObject("roh")?.let { Stand(it, j.optString("quelle", "zuletzt gespeichert")) }
    }

    private fun merken(s: Stand) = runCatching {
        val neu = File(context.filesDir, "tenda-letzter.json.neu")
        neu.writeText(JSONObject().put("roh", s.roh).put("quelle", s.quelle).toString())
        if (!neu.renameTo(letzterDatei)) { letzterDatei.writeText(neu.readText()); neu.delete() }
    }

    private val sperre = Mutex()

    /** Frische Momentabfrage (Karte, Werkzeug): Mac, sonst direkt im Heim-WLAN, sonst der letzte Stand mit Grund. */
    suspend fun abrufen(): Stand? = sperre.withLock {
        // Die Meldung zur letzten Aktion bleibt stehen (das Nachlesen danach soll sie nicht löschen).
        _zustand.update { it.copy(liest = true) }
        try {
            withContext(Dispatchers.IO) {
                val gruende = mutableListOf<String>()
                // 1. Mac zu Hause: frische Momentabfrage über den Relay.
                val mac = runCatching { relayBefehl("lesen", "") }.onFailure { gruende += "Server nicht erreichbar" }.getOrNull()
                mac?.optJSONObject("stand")?.let { st ->
                    return@withContext Stand(st, "Mac zu Hause").also { merken(it); fertig(it) }
                }
                when (mac?.optString("ergebnis")) {
                    "mac_nicht_verbunden" -> gruende += "Mac zu Hause nicht verbunden"
                    "keine_rueckmeldung" -> gruende += "Mac antwortet nicht"
                    "fehler" -> gruende += mac.optString("text").ifBlank { "Mac konnte den Router nicht lesen" }
                    null -> Unit
                    else -> gruende += "Mac lieferte keinen Stand"
                }
                // 2. Handy im Heim-WLAN direkt am Router.
                if (hash().isNotBlank() && zuHause()) {
                    val direkt = runCatching { direktLesen() }.onFailure { gruende += "direkt: " + (it.message ?: it.javaClass.simpleName) }.getOrNull()
                    if (direkt != null) return@withContext Stand(direkt, "Handy direkt im Heim-WLAN").also { merken(it); fertig(it) }
                } else if (hash().isNotBlank()) gruende += "Handy nicht im Heim-WLAN"
                // 3. Letzter bekannter Stand: der jüngere von Relay (stündlicher Takt des Macs) und Handy.
                val relayStand = runCatching { relayStand()?.optJSONObject("stand") }.getOrNull()?.let { Stand(it, "Mac zu Hause (Takt)") }
                val alt = listOfNotNull(relayStand, letzter()).maxByOrNull { it.zeit }
                alt?.copy(veraltetGrund = gruende.distinct().joinToString(", ").ifBlank { "keine frische Abfrage möglich" })?.also { fertig(it) }
            }
        } finally {
            _zustand.update { it.copy(liest = false) }
        }
    }

    private fun fertig(s: Stand) = _zustand.update { it.copy(stand = s) }

    /** Gespeicherten Stand sofort zeigen (erstes Bild der Karte), ohne Netz. */
    fun zeigen(s: Stand) = fertig(s)

    /** Für das Live-Gedächtnis: nur gespeicherte Stände (Relay-Takt des Macs oder zuletzt am Handy), nie eine Anmeldung am Router. */
    suspend fun gedaechtnis(): String = withContext(Dispatchers.IO) {
        val relayStand = runCatching { relayStand()?.optJSONObject("stand") }.getOrNull()?.let { Stand(it, "Mac zu Hause") }
        val s = listOfNotNull(relayStand, letzter()).maxByOrNull { it.zeit } ?: return@withContext "Noch kein Stand. ${stoerung() ?: ""}".trim()
        kurz(s) + " Frisch mit tenda_lesen."
    }

    // ------------------------------------------------------------------ Bewertung

    /** Fünf Stufen von sehr schlecht bis exzellent; null = nicht bewertbar. */
    enum class Stufe(val wort: String) { SEHR_SCHLECHT("sehr schlecht"), SCHLECHT("schlecht"), MITTEL("mittel"), GUT("gut"), EXZELLENT("exzellent") }

    data class Messwert(val name: String, val wert: Double?, val einheit: String, val stufe: Stufe?, val hinweis: String? = null) {
        fun text(): String = if (wert == null) "nicht geliefert" else zahl(wert) + (if (einheit.isNotEmpty()) " $einheit" else "")
    }

    data class Multiplayer(val stufe: Stufe?, val text: String, val begruendung: String, val veraltet: Boolean = false)

    /** Veraltet oder ohne frische Abfrage: Die Einschätzung gilt dann nur für die Messzeit („zuletzt …“), nicht für jetzt. */
    fun multiplayer(s: Stand): Multiplayer {
        val m = multiplayerRoh(s)
        return if (s.veraltetGrund != null || gueltigkeit(s) == "veraltet") m.copy(text = "zuletzt " + m.text + " (Stand ${uhrzeit(s.zeit)})", veraltet = true) else m
    }

    private fun multiplayerRoh(s: Stand): Multiplayer {
        val p = s.ping ?: return Multiplayer(null, "nicht bewertbar", "keine Ping-Messung am Heimanschluss")
        val lat = p.zahl("latenz_ms"); val verlust = p.zahl("verlust_prozent")
        if (verlust != null && verlust >= 100.0) return Multiplayer(Stufe.SEHR_SCHLECHT, "kein Internet", "keine Ping-Antwort")
        if (lat == null || verlust == null) return Multiplayer(null, "nicht bewertbar", p.optString("fehler").ifBlank { "Ping ohne Antwort" })
        // Nur eine Antwort: kein Jitter messbar; bei so viel Verlust ist die Leitung ohnehin ungeeignet.
        val jit = p.zahl("jitter_ms") ?: if (verlust >= 10.0) 999.0 else return Multiplayer(null, "nicht bewertbar", "Jitter nicht messbar")
        val stufe = when {
            lat <= 35 && jit <= 5 && verlust == 0.0 -> Stufe.EXZELLENT
            lat <= 50 && jit <= 10 && verlust == 0.0 -> Stufe.GUT
            lat <= 80 && jit <= 20 && verlust <= 5.0 -> Stufe.MITTEL
            lat <= 120 && jit <= 40 && verlust <= 10.0 -> Stufe.SCHLECHT
            else -> Stufe.SEHR_SCHLECHT
        }
        val text = when (stufe) {
            Stufe.EXZELLENT -> "sehr gut geeignet"
            Stufe.GUT -> "gut geeignet"
            Stufe.MITTEL -> "spielbar, mit möglichen Rucklern"
            Stufe.SCHLECHT -> "eher nicht geeignet"
            Stufe.SEHR_SCHLECHT -> "nicht geeignet"
        }
        val ziel = p.optString("ziel", "1.1.1.1")
        return Multiplayer(stufe, text, "Ping ${zahl(lat)} ms, Jitter ${zahl(jit)} ms, Verlust ${zahl(verlust)} % (zu $ziel)")
    }

    /** Aktiver Modus aus dem Router; fehlt ein klares Feld, aus den gelieferten LTE- und 5G-Werten abgeleitet (dann mit Zusatz). */
    fun aktiverModus(s: Stand): String? {
        val i = s.simInfo
        val roh = MODUS_FELDER.firstNotNullOfOrNull { k -> i.optString(k).takeIf { it.isNotBlank() && !it.equals("unknown", true) } }?.uppercase(Locale.ROOT)
        if (roh != null) {
            val mit5g = "5G" in roh || "NR" in roh
            when {
                "NSA" in roh || "ENDC" in roh || "EN-DC" in roh -> return "5G NSA"
                mit5g && ("LTE" in roh || "4G" in roh) -> return "5G NSA"
                "SA" in roh -> return "5G SA"
                "LTE" in roh || "4G" in roh -> return "4G"
                "3G" in roh || "WCDMA" in roh || "UMTS" in roh -> return "3G"
                // Nur „5G“: NSA, wenn zugleich LTE-Werte kommen (Anker im LTE), sonst SA.
                "5G" in roh || "NR" in roh -> return if (lte(s).any { it.name == "RSRP" && it.wert != null }) "5G NSA" else "5G SA"
            }
        }
        val lte = lte(s).any { it.name == "RSRP" && it.wert != null }
        val nr = nr(s).any { it.name == "SS-RSRP" && it.wert != null }
        return when {
            lte && nr -> "5G NSA (abgeleitet)"
            nr -> "5G SA (abgeleitet)"
            lte -> "4G (abgeleitet)"
            else -> null
        }
    }

    fun eingestellterModus(s: Stand): String? = s.simWan.optString("dataOptions").let { MODI[it] }

    /** LTE- und 5G-Bänder aus dem Router (accessBand und Verwandte), getrennt nach „B…“ und „n…“. */
    fun baender(s: Stand): Pair<List<String>, List<String>> {
        val text = BAND_FELDER.mapNotNull { k -> s.simInfo.optString(k).takeIf { it.isNotBlank() && it != "0" } }.joinToString(" ")
        val lte = Regex("(?i)(?:\\bB|band\\s*)(\\d{1,3})\\b").findAll(text).map { "B" + it.groupValues[1] }.distinct().toList()
        val nr = Regex("\\b[nN](\\d{1,3})\\b").findAll(text).map { "n" + it.groupValues[1] }.distinct().toList()
        return lte to nr
    }

    fun lte(s: Stand): List<Messwert> {
        val i = s.simInfo
        val rsrp = wert(i, "rsrp", "lte_rsrp").takeIf { it != null && it < 0 }
        val da = rsrp != null
        fun nurMit(v: Double?) = if (da) v else null
        return listOf(
            Messwert("RSRP", rsrp, "dBm", rsrp?.let { stufe(it, -110.0, -100.0, -90.0, -80.0) }),
            Messwert("RSRQ (Signalqualität)", nurMit(wert(i, "rsrq", "lte_rsrq")), "dB", nurMit(wert(i, "rsrq", "lte_rsrq"))?.let { stufe(it, -20.0, -15.0, -12.0, -10.0) },
                "Signalqualität (Störung und Last im Verhältnis zum Nutzsignal), keine direkte Auslastungsmessung"),
            Messwert("SINR", nurMit(wert(i, "sinr", "lte_sinr")), "dB", nurMit(wert(i, "sinr", "lte_sinr"))?.let { stufe(it, 0.0, 5.0, 13.0, 20.0) }),
            Messwert("CQI", nurMit(wert(i, "cqi", "lte_cqi")), "", nurMit(wert(i, "cqi", "lte_cqi"))?.let { stufe(it, 4.0, 7.0, 10.0, 12.0) }),
            Messwert("RSSI", nurMit(wert(i, "rssi", "lte_rssi")?.takeIf { it < 0 }), "dBm", nurMit(wert(i, "rssi", "lte_rssi")?.takeIf { it < 0 })?.let { stufe(it, -95.0, -85.0, -75.0, -65.0) },
                "grobe Einordnung: RSSI enthält auch Störungen"),
        )
    }

    fun nr(s: Stand): List<Messwert> {
        val i = s.simInfo
        val rsrp = wert(i, "ss_rsrp", "nr_rsrp", "ssRsrp", "nr5g_rsrp").takeIf { it != null && it < 0 }
        val da = rsrp != null
        fun nurMit(v: Double?) = if (da) v else null
        val rsrq = nurMit(wert(i, "ss_rsrq", "nr_rsrq", "ssRsrq", "nr5g_rsrq"))
        val sinr = nurMit(wert(i, "ss_sinr", "nr_sinr", "ssSinr", "nr5g_sinr"))
        val rssi = nurMit(wert(i, "ss_rssi", "nr_rssi", "ssRssi", "nr5g_rssi")?.takeIf { it < 0 })
        return listOf(
            Messwert("SS-RSRP", rsrp, "dBm", rsrp?.let { stufe(it, -110.0, -100.0, -90.0, -80.0) }),
            Messwert("SS-RSRQ (Signalqualität)", rsrq, "dB", rsrq?.let { stufe(it, -20.0, -15.0, -12.0, -10.0) }, "Signalqualität, keine direkte Auslastungsmessung"),
            Messwert("SS-SINR", sinr, "dB", sinr?.let { stufe(it, 0.0, 5.0, 13.0, 20.0) }),
            Messwert("SS-RSSI", rssi, "dBm", rssi?.let { stufe(it, -95.0, -85.0, -75.0, -65.0) }, "grobe Einordnung"),
        ) + run {
            // Im 5G SA gibt es keine LTE-Werte; ein CQI gehört dann zum 5G (laut Messung vom 19.11.2026 kommt er dort mit).
            val lteDa = wert(i, "rsrp", "lte_rsrp")?.let { it < 0 } == true
            val cqi = if (da && !lteDa) wert(i, "cqi", "nr_cqi") else null
            if (cqi != null) listOf(Messwert("CQI", cqi, "", stufe(cqi, 4.0, 7.0, 10.0, 12.0))) else emptyList()
        }
    }

    /** Wie aktuell ein Stand ist: bis 10 Minuten aktuell, bis 90 Minuten älter (der Mac liest stündlich), danach veraltet. */
    fun gueltigkeit(s: Stand): String {
        val alter = System.currentTimeMillis() / 1000 - s.zeit
        return when {
            s.zeit <= 0 -> "ohne Messzeit"
            alter <= 600 -> "aktuell"
            alter <= 5400 -> "älter"
            else -> "veraltet"
        }
    }

    private fun kopf(s: Stand): String {
        val alter = System.currentTimeMillis() / 1000 - s.zeit
        return "Stand ${uhrzeit(s.zeit)} (${alterText(alter)}, ${s.quelle}, ${gueltigkeit(s)})" +
            (s.veraltetGrund?.let { "; keine frische Abfrage: $it" } ?: "")
    }

    /** Kurzfassung für Gedächtnis und Sprache. */
    fun kurz(s: Stand): String {
        val m = multiplayer(s)
        val (bl, bn) = baender(s)
        val aktiv = aktiverModus(s) ?: "nicht geliefert"
        val eingestellt = eingestellterModus(s)
        val funk = (lte(s) + nr(s)).filter { it.wert != null && it.name in setOf("RSRP", "SINR", "SS-RSRP", "SS-SINR") }
            .joinToString(", ") { "${it.name} ${it.text()} (${it.stufe?.wort ?: "ohne Bewertung"})" }
        return kopf(s) + ". Modus aktiv: $aktiv" + (eingestellt?.let { ", eingestellt: $it" } ?: "") +
            ". Multiplayer: ${m.text} (${m.begruendung})." +
            (if (bl.isNotEmpty() || bn.isNotEmpty()) " Bänder: " + (bl + bn).joinToString("+") + "." else "") +
            (if (funk.isNotEmpty()) " Empfang: $funk." else "")
    }

    private fun antwort(s: Stand): String {
        val zeilen = mutableListOf(kurz(s))
        (lte(s) + nr(s)).filter { it.wert != null && it.name !in setOf("RSRP", "SINR", "SS-RSRP", "SS-SINR") }.takeIf { it.isNotEmpty() }?.let { w ->
            zeilen += "Weitere Werte: " + w.joinToString(", ") { "${it.name} ${it.text()} (${it.stufe?.wort ?: "ohne Bewertung"})" } + "."
        }
        val alle = lte(s) + nr(s)
        val lteDa = lte(s).any { it.wert != null }; val nrDa = nr(s).any { it.wert != null }
        if (!lteDa && nrDa) zeilen += "LTE ist im aktuellen Modus nicht aktiv."
        if (!nrDa && lteDa) zeilen += "5G ist im aktuellen Modus nicht aktiv."
        val fehlt = alle.filter { w -> w.wert == null && alle.none { it.name == w.name && it.wert != null } &&
            ((w in lte(s) && lteDa) || (w in nr(s) && nrDa)) }.map { it.name }.distinct()
        if (fehlt.isNotEmpty()) zeilen += "Vom Router nicht geliefert: ${fehlt.joinToString(", ")}."
        zeilen += "Die Multiplayer-Einschätzung misst den Weg bis ${s.ping?.optString("ziel") ?: "1.1.1.1"}, nicht den Spielserver selbst."
        return zeilen.joinToString(" ")
    }

    /** Für Mac- und Windows-Jarvis (Startkarte über das Werkzeug) und die Handy-Karte: alles fertig bewertet. */
    fun struktur(s: Stand): JSONObject {
        val m = multiplayer(s)
        val (bl, bn) = baender(s)
        fun abschnitt(titel: String, werte: List<Messwert>, baender: List<String>) = JSONObject().put("titel", titel).put("zeilen", JSONArray().apply {
            put(JSONObject().put("name", "Band").put("wert", baender.joinToString(", ").ifBlank { "nicht geliefert" }))
            werte.forEach { w -> put(JSONObject().put("name", w.name).put("wert", w.text()).put("bewertung", w.stufe?.wort ?: JSONObject.NULL).put("hinweis", w.hinweis ?: JSONObject.NULL)) }
        })
        val p = s.ping
        val verbindung = JSONObject().put("titel", "Verbindung").put("zeilen", JSONArray().apply {
            fun z(name: String, wert: Double?, einheit: String) = put(JSONObject().put("name", name).put("wert", wert?.let { zahl(it) + " " + einheit } ?: "nicht gemessen"))
            z("Ping", p?.zahl("latenz_ms"), "ms"); z("Jitter", p?.zahl("jitter_ms"), "ms"); z("Paketverlust", p?.zahl("verlust_prozent"), "%")
            put(JSONObject().put("name", "Messziel").put("wert", p?.optString("ziel")?.ifBlank { null } ?: "–"))
        })
        return JSONObject()
            .put("zeit", s.zeit).put("quelle", s.quelle).put("gueltigkeit", gueltigkeit(s)).put("stand_text", kopf(s))
            .put("veraltet_grund", s.veraltetGrund ?: JSONObject.NULL)
            .put("modus_aktiv", aktiverModus(s) ?: JSONObject.NULL).put("modus_eingestellt", eingestellterModus(s) ?: JSONObject.NULL)
            .put("multiplayer", JSONObject().put("stufe", if (m.veraltet) JSONObject.NULL else m.stufe?.wort ?: JSONObject.NULL).put("text", m.text).put("begruendung", m.begruendung).put("veraltet", m.veraltet))
            .put("abschnitte", JSONArray().put(verbindung).put(abschnitt("LTE (4G)", lte(s), bl)).put(abschnitt("5G (NR)", nr(s), bn)))
    }

    // ------------------------------------------------------------------ Schalten

    /**
     * Moduswechsel, Neuverbinden, Neustart: erst über den Mac; ist er nicht verbunden und das Handy im Heim-WLAN, direkt.
     * Ein Schreibvorgang wird nie wiederholt. Danach prüft Jarvis im Hintergrund den neuen Zustand (Karte und Logbuch).
     * Rückgabe: Text für Frank und ob der Befehl angenommen wurde.
     */
    suspend fun schalten(art: String, wert: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        if (hash().isBlank()) return@withContext "Das Router-Passwort ist noch nicht eingerichtet." to false
        _zustand.value.schaltet?.let { return@withContext "Es läuft noch: $it. Erst danach ist ein neuer Befehl möglich." to false }
        val was = when (art) { "modus" -> "Moduswechsel auf ${MODI[wert]}"; "verbinden" -> "Neuverbinden"; else -> "Neustart" }
        val pause = if (art == "neustart") "zwei bis vier Minuten" else "ein bis drei Minuten"
        _zustand.update { it.copy(schaltet = was, meldung = "$was läuft. Das Internet ist jetzt etwa $pause unterbrochen; danach prüfe ich den neuen Zustand.") }
        Logbuch.aktion("Tenda: $was")
        val antwort = runCatching { relayBefehl(art, wert) }.getOrNull()
        when (antwort?.optString("ergebnis")) {
            "angenommen" -> {
                bereich.launch { nachpruefen(art, wert, antwort.optString("kennung"), was) }
                "$was gestartet (über den Mac). Das Internet ist etwa $pause unterbrochen; ich prüfe danach selbst den neuen Zustand." to true
            }
            "fehler" -> ende(was, antwort.optString("text").ifBlank { "Der Mac konnte den Befehl nicht ausführen." }) to false
            "abgeloest" -> ende(was, "Durch einen neueren Befehl ersetzt.") to false
            "mac_nicht_verbunden" -> {
                // Mac nicht da oder Server nicht erreichbar: direkt, wenn das Handy im Heim-WLAN ist.
                if (!zuHause()) ende(was, "Weder der Mac zu Hause noch das Handy im Heim-WLAN erreichen den Router; nichts geändert.") to false
                else {
                    val r = runCatching { direktSchalten(art, wert) }
                    r.exceptionOrNull()?.let { f ->
                        if (f is UnklarFehler) { bereich.launch { delay(60_000); direktNachpruefen(art, wert, was) }; return@withContext ende(was, f.message ?: "unklar").let { it to false } }
                        return@withContext ende(was, f.message ?: f.javaClass.simpleName) to false
                    }
                    bereich.launch { direktNachpruefen(art, wert, was) }
                    "$was gestartet (direkt vom Handy). Das Internet ist etwa $pause unterbrochen; ich prüfe danach den neuen Zustand." to true
                }
            }
            // Unklar, Netzfehler oder unbekannte Antwort: Der Mac kann den Befehl schon ausführen. Nie zusätzlich direkt schalten, nur nachsehen.
            else -> ende(was, "Unklar, ob der Mac den Befehl angenommen hat. Ich löse ihn nicht erneut aus; in eineinhalb Minuten lese ich den Zustand.").also {
                bereich.launch { delay(90_000); abrufen() }
            } to false
        }
    }

    private fun ende(was: String, text: String): String {
        _zustand.update { it.copy(schaltet = null, meldung = "$was: $text") }
        Logbuch.aktion("Tenda: $was", text.take(200))
        return text
    }

    /** Wartet auf das Ergebnis des Macs (er meldet nach Wiedererreichbarkeit an den Relay) und liest dann neu. */
    private suspend fun nachpruefen(art: String, wert: String, kennung: String, was: String) {
        val ende = System.currentTimeMillis() + 6 * 60_000L
        delay(20_000)
        while (System.currentTimeMillis() < ende) {
            val r = runCatching { relayStand() }.getOrNull()
            val aktion = r?.optJSONObject("aktion")
            if (aktion != null && (kennung.isBlank() || aktion.optString("kennung") == kennung)) {
                r.optJSONObject("stand")?.let { fertig(Stand(it, "Mac zu Hause")); merken(Stand(it, "Mac zu Hause")) }
                val stand = _zustand.value.stand
                val pruefung = if (art == "modus" && stand != null) " Eingestellt: ${eingestellterModus(stand) ?: "nicht gelesen"}, aktiv: ${aktiverModus(stand) ?: "nicht gelesen"}." else ""
                // Gleich nach dem Wiederverbinden sind Technik und Ping teils noch nicht da: kurz danach einmal frisch lesen.
                bereich.launch { delay(30_000); abrufen() }
                ende(was, when (aktion.optString("ergebnis")) {
                    "fertig" -> "erledigt, Router wieder erreichbar." + pruefung
                    "unklar" -> aktion.optString("text").ifBlank { "Ausgang unklar." } + " Nicht erneut ausgelöst."
                    else -> aktion.optString("text").ifBlank { "gescheitert." }
                })
                return
            }
            delay(10_000)
        }
        ende(was, "Nach sechs Minuten keine Rückmeldung vom Mac. Nicht erneut ausgelöst; bitte Zustand prüfen.")
        abrufen()
    }

    private suspend fun direktNachpruefen(art: String, wert: String, was: String) {
        delay(if (art == "neustart") 60_000 else 20_000)
        val ende = System.currentTimeMillis() + 5 * 60_000L
        while (System.currentTimeMillis() < ende) {
            if (hash().isBlank()) { ende(was, "Router-Passwort ungültig; keine weiteren Anmeldeversuche."); return }
            val roh = runCatching { direktLesen() }.getOrNull()
            // Direkt nach dem Wiederverbinden meldet der Router kurz „unknown“ ohne Ping: dann weiter nachlesen.
            val steht = roh != null && !roh.optJSONObject("sim_info")?.optString("mobileNet").orEmpty().let { it.isBlank() || it.equals("unknown", true) } &&
                (roh.optJSONObject("ping")?.optInt("empfangen") ?: 0) > 0
            if (roh != null && (steht || System.currentTimeMillis() > ende - 60_000)) {
                val s = Stand(roh, "Handy direkt im Heim-WLAN"); merken(s); fertig(s)
                val pruefung = if (art == "modus") " Eingestellt: ${eingestellterModus(s) ?: "nicht gelesen"}, aktiv: ${aktiverModus(s) ?: "nicht gelesen"}." else ""
                ende(was, "erledigt, Router wieder erreichbar.$pruefung")
                return
            }
            delay(10_000)
        }
        ende(was, "Router nach fünf Minuten noch nicht wieder lesbar. Nicht erneut ausgelöst.")
    }

    // ------------------------------------------------------------------ Einrichten

    /**
     * Einrichtungsdialog: Passwort → MD5. Zu Hause genau ein Anmeldeversuch direkt am Router; nur bei Erfolg gespeichert. Danach bekommt
     * der Mac den Hash über den Relay (nur im Speicher bis zur Abholung). Unterwegs prüft der Mac ihn. Das Passwort selbst bleibt nirgends.
     */
    suspend fun einrichten(passwort: String): String = withContext(Dispatchers.IO) {
        if (passwort.isBlank()) return@withContext "Kein Passwort eingegeben."
        val h = md5(passwort)
        val daheim = zuHause(ohneSsid = true)
        if (daheim) {
            val router = DirektRouter(wlan() ?: return@withContext "Kein WLAN gefunden.")
            try { router.anmelden(h) } catch (f: Exception) { return@withContext f.message ?: "Anmeldung gescheitert." }
            SecureSettings(context).use { it.tendaHash = h; it.tendaSsid = ssid().orEmpty() }
        } else {
            SecureSettings(context).use { it.tendaHash = h }
        }
        val mac = runCatching { relayBefehl("zugang", h) }.getOrNull()
        val macText = when (mac?.optString("ergebnis")) {
            "fertig" -> { mac.optJSONObject("stand")?.let { val s = Stand(it, "Mac zu Hause"); merken(s); fertig(s) }; "Der Mac zu Hause hat den Zugang übernommen und den Router gelesen." }
            "fehler" -> {
                if (mac.optString("art") == "zugang" && !daheim) SecureSettings(context).use { it.tendaHash = "" }
                "Der Mac meldet: " + mac.optString("text")
            }
            "mac_nicht_verbunden" -> "Der Mac zu Hause ist gerade nicht verbunden; er bekommt den Zugang beim nächsten Speichern."
            else -> "Der Mac hat nicht rechtzeitig geantwortet."
        }
        Logbuch.aktion("Tenda: Zugang eingerichtet", if (daheim) "zu Hause geprüft" else "unterwegs")
        (if (daheim) "Passwort geprüft und gespeichert. " else "Gespeichert (unterwegs nicht direkt prüfbar). ") + macText
    }

    fun eingerichtet(): Boolean = hash().isNotBlank()

    private fun hash(): String = runCatching { SecureSettings(context).use { it.tendaHash } }.getOrDefault("")

    // ------------------------------------------------------------------ Relay

    private fun relayBefehl(art: String, wert: String): JSONObject? {
        if (e.serverToken.isBlank()) return null
        val rumpf = JSONObject().put("art", art).put("wert", wert).toString().toRequestBody(JSON)
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/tenda-befehl").post(rumpf)
            .header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung).build()
        return RELAY.newCall(anfrage).execute().use { a ->
            check(a.isSuccessful) { "Relay antwortet mit ${a.code}" }
            JSONObject(a.body?.string().orEmpty())
        }
    }

    private fun relayStand(): JSONObject? {
        if (e.serverToken.isBlank()) return null
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/tenda").header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung).build()
        return RELAY.newCall(anfrage).execute().use { a -> if (a.isSuccessful) JSONObject(a.body?.string().orEmpty()) else null }
    }

    // ------------------------------------------------------------------ Direkt im Heim-WLAN

    private fun wlan(): Network? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        @Suppress("DEPRECATION")
        return cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
    }

    @Suppress("DEPRECATION")
    private fun ssid(): String? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val s = runCatching { context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo?.ssid?.trim('"') }.getOrNull() ?: return null
        return s.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }

    /** Im Heim-WLAN: WLAN da, unter 192.168.0.1 antwortet ein Tenda 5G03 und (wenn bekannt und lesbar) der WLAN-Name stimmt. */
    private fun zuHause(ohneSsid: Boolean = false): Boolean {
        val netz = wlan() ?: return false
        if (!ohneSsid) {
            val gemerkt = runCatching { SecureSettings(context).use { it.tendaSsid } }.getOrDefault("")
            val jetzt = ssid()
            if (gemerkt.isNotBlank() && jetzt != null && jetzt != gemerkt) return false
        }
        return runCatching {
            DirektRouter(netz).client.newCall(Request.Builder().url("$ROUTER/js/macro_config.js").build()).execute().use { a ->
                a.isSuccessful && (a.body?.string().orEmpty()).contains("CONFIG_PRODUCT =\"5G03\"")
            }
        }.getOrDefault(false)
    }

    /** Fehler, nach dem offen ist, ob der Router geschaltet hat (Antwort verloren): nie wiederholen. */
    private class UnklarFehler(text: String) : Exception(text)

    private inner class DirektRouter(netz: Network) {
        private val kekse = mutableListOf<Cookie>()
        val client: OkHttpClient = OkHttpClient.Builder().socketFactory(netz.socketFactory).proxy(java.net.Proxy.NO_PROXY)
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) { synchronized(kekse) { kekse.removeAll { k -> cookies.any { it.name == k.name } }; kekse += cookies } }
                override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(kekse) { kekse.toList() }
            }).build()

        private fun rufe(pfad: String, rumpf: JSONObject? = null): JSONObject {
            val b = Request.Builder().url(ROUTER + pfad)
            if (rumpf != null) b.post(rumpf.toString().toRequestBody(JSON))
            return client.newCall(b.build()).execute().use { a -> JSONObject(a.body?.string().orEmpty().ifBlank { "{}" }) }
        }

        fun anmelden(h: String) {
            // Ohne gültigen Hash nie anmelden: Jeder Fehlversuch zählt zur Sperre des Routers.
            check(h.isNotBlank()) { "Passwort falsch oder nicht eingerichtet" }
            val a = rufe("/login/Auth", JSONObject().put("username", "admin").put("password", h))
            when (a.optInt("errCode", -1)) {
                0 -> return
                4 -> {
                    val rest = 4 - a.optInt("authErrNum", 0)
                    throw IllegalStateException("Passwort falsch" + (if (rest > 0) ", noch $rest Versuche, bevor der Router sperrt." else "; der Router ist gesperrt."))
                }
                5 -> throw IllegalStateException("Am Router sind schon drei Geräte angemeldet; später erneut versuchen.")
                else -> throw IllegalStateException("Anmeldung am Router gescheitert (Code ${a.optInt("errCode", -1)}).")
            }
        }

        fun module(namen: String): JSONObject {
            val pfad = "/goform/getModules?rand=${Math.random()}&modules=$namen"
            var a = rufe(pfad)
            if (a.optInt("errCode") == 1000) { anmelden(hash()); a = rufe(pfad) }
            check(a.optInt("errCode") != 1000) { "Router nimmt die Anmeldung nicht an" }
            return a
        }

        fun setzen(rumpf: JSONObject, pfad: String = "/goform/setModules"): Int {
            var a = rufe(pfad, rumpf)
            if (a.optInt("errCode") == 1000) { anmelden(hash()); a = rufe(pfad, rumpf) }
            return a.optInt("errCode", -1)
        }
    }

    /** Ein Router-Lauf zur Zeit (der Router lässt nur drei Anmeldungen zu); die Sitzung bleibt bis zum Ablauf im Speicher. */
    private var direkt: DirektRouter? = null
    private var direktNetz: Network? = null
    private val direktSperre = Mutex()

    private suspend fun router(): DirektRouter {
        val netz = wlan() ?: error("kein WLAN")
        // Nach einem WLAN-Wechsel wäre die alte Sitzung an ein verschwundenes Netz gebunden.
        if (netz != direktNetz) { direkt = null; direktNetz = netz }
        return direkt ?: DirektRouter(netz).also { direkt = it }
    }

    private suspend fun direktLesen(): JSONObject = direktSperre.withLock {
        val r = router()
        val roh = runCatching { r.module("simInfo,simWan,simStatus,wanCfg,internetStatus") }.getOrElse { f ->
            if (f.message?.startsWith("Passwort falsch") == true) SecureSettings(context).use { it.tendaHash = "" }
            direkt = null; throw f
        }
        val simInfo = roh.optJSONObject("simInfo") ?: JSONObject()
        val simWan = roh.optJSONObject("simWan") ?: JSONObject()
        // Unterwegs eingerichtet: den WLAN-Namen beim ersten erfolgreichen Direktabruf nachtragen (danach prüft zuHause() ihn mit).
        runCatching { SecureSettings(context).use { st -> if (st.tendaSsid.isBlank()) ssid()?.let { st.tendaSsid = it } } }
        val info = JSONObject()
        simInfo.keys().forEach { k -> val v = simInfo.opt(k); if (!GEHEIM.containsMatchIn(k) && (v is String || v is Number || v is Boolean) && v.toString().length <= 80) info.put(k, v) }
        val wan = JSONObject()
        listOf("dataOptions", "connectType", "internetStatus", "rssi", "bandAggregation", "selectBandResult", "action").forEach { k ->
            simWan.opt(k)?.takeIf { it is String || it is Number }?.let { wan.put(k, it) }
        }
        val felder = JSONArray(simInfo.keys().asSequence().filter { !GEHEIM.containsMatchIn(it) }.sorted().toList())
        if (!feldListeGemeldet) { feldListeGemeldet = true; Logbuch.netz("Tenda: Felder von simInfo", felder.join(",").replace("\"", "")) }
        JSONObject().put("zeit", System.currentTimeMillis() / 1000).put("quelle", "Handy direkt im Heim-WLAN")
            .put("sim_info", info).put("sim_wan", wan).put("sim_status", roh.optJSONObject("simStatus")?.optString("simStatus"))
            .put("felder", felder).put("ping", pingMessen(wlan()))
    }

    @Volatile private var feldListeGemeldet = false

    private suspend fun direktSchalten(art: String, wert: String) = direktSperre.withLock {
        val r = router()
        if (art == "neustart") {
            val code = try { r.setzen(JSONObject().put("reboot", JSONObject().put("action", "reboot"))) }
                catch (f: java.io.IOException) { throw UnklarFehler("Keine klare Antwort vom Router; ich prüfe den Zustand, ohne erneut zu schalten.") }
            check(code == 0 || code == 100) { "Der Router hat den Neustart abgelehnt (Code $code)." }
            direkt = null
            return@withLock
        }
        val simWan = r.module("simWan").optJSONObject("simWan")
        check(simWan != null && simWan.has("list")) { "Die Mobilfunk-Einstellungen ließen sich nicht vollständig lesen; nichts geändert." }
        try {
            if (art == "modus") {
                simWan.put("dataOptions", wert).put("action", "1")
                val code = r.setzen(JSONObject().put("simWan", simWan))
                check(code == 0) { "Der Router hat den Moduswechsel abgelehnt (Code $code)." }
            } else {
                val trennen = JSONObject(simWan.toString()).put("action", "0")
                var code = r.setzen(JSONObject().put("simWan", trennen))
                if (code == 0) {
                    delay(3_000)
                    // Getrennt ist er jetzt: Verbinden darf nie ausbleiben (ein zweites „verbinden“ trennt nicht erneut).
                    code = runCatching { r.setzen(JSONObject().put("simWan", JSONObject(simWan.toString()).put("action", "1"))) }.getOrDefault(-1)
                    if (code != 0) { delay(3_000); code = r.setzen(JSONObject().put("simWan", JSONObject(simWan.toString()).put("action", "1"))) }
                    check(code == 0) { "Getrennt, aber der Router nimmt das Verbinden nicht an (Code $code); das Heim-Internet ist gerade aus. Bitte Connect erneut drücken." }
                }
                check(code == 0) { "Der Router hat das Neuverbinden abgelehnt (Code $code)." }
            }
        } catch (f: java.io.IOException) {
            throw UnklarFehler("Keine klare Antwort vom Router; ich prüfe den Zustand, ohne erneut zu schalten.")
        }
    }

    /** Ping vom Handy aus (inklusive WLAN-Strecke): 20 Pakete an 1.1.1.1. */
    private fun pingMessen(netz: Network?): JSONObject {
        val ziel = "1.1.1.1"; val anzahl = 20
        val p = JSONObject().put("ziel", ziel).put("gesendet", anzahl).put("zeit", System.currentTimeMillis() / 1000).put("von", "Handy im WLAN")
        // Das ping-Programm nimmt Androids Standardnetz. Ist das nicht das Heim-WLAN mit Internet (etwa kurz nach einem Moduswechsel),
        // würde es den Mobilfunk des Handys messen statt den Heimanschluss: dann lieber nicht messen.
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val standard = cm?.activeNetwork
        val validiert = standard?.let { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } == true
        if (netz == null || standard != netz || !validiert) return p.put("fehler", "Heim-WLAN gerade ohne Internet oder nicht das Standardnetz; kein Ping")
        val aus = runCatching {
            val prozess = ProcessBuilder("ping", "-c", "$anzahl", "-i", "0.2", "-W", "2", ziel).redirectErrorStream(true).start()
            val text = prozess.inputStream.bufferedReader().readText()
            prozess.waitFor(15, TimeUnit.SECONDS); text
        }.getOrElse { return p.put("fehler", "Ping am Handy nicht möglich") }
        val zeiten = Regex("time[=<]\\s*([\\d.]+)\\s*ms").findAll(aus).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
        p.put("empfangen", zeiten.size).put("verlust_prozent", Math.round(1000.0 * (anzahl - zeiten.size) / anzahl) / 10.0)
        if (zeiten.isNotEmpty()) {
            p.put("latenz_ms", Math.round(zeiten.average() * 10) / 10.0).put("latenz_min_ms", zeiten.min()).put("latenz_max_ms", zeiten.max())
            if (zeiten.size >= 2) p.put("jitter_ms", Math.round(zeiten.zipWithNext { a, b -> abs(a - b) }.average() * 10) / 10.0)
        }
        return p
    }

    companion object {
        private const val ROUTER = "http://192.168.0.1"
        private val JSON = "application/json; charset=UTF-8".toMediaType()
        /** Der Relay wartet bis 15 s auf die Brücke und bis 40 s auf ihr Ergebnis: Zeitlimit darüber. */
        private val RELAY = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(75, TimeUnit.SECONDS).build()
        val MODI = mapOf("5" to "5G SA bevorzugt", "4" to "5G NSA", "2" to "nur 4G", "3" to "nur 3G")
        /** Auswahl für die Karte: Kennung des Routers → Name. 3G bietet Jarvis bewusst nicht an. */
        val WAEHLBAR = listOf("5" to "5G SA bevorzugt", "4" to "5G NSA", "2" to "nur 4G")
        /** mobileNet ist beim 5G03 das Feld für die aktive Technik (am 20.11.2026 im Modus „nur 4G“: „4G“). */
        private val MODUS_FELDER = listOf("mobileNet", "networkType", "network_type", "netType", "net_type", "sysMode", "sys_mode", "accessTech", "access_tech",
            "connectNet", "netMode", "nrMode", "nr_mode", "radioType", "rat", "workNet")
        private val BAND_FELDER = listOf("accessBand", "access_band", "band", "lteBand", "lte_band", "nrBand", "nr_band", "caBand", "ca_band", "scellBand", "nr5g_band")
        /** Wie jarvis_tenda.py: auch Zellkennungen, die mit Mastkarten den Standort verraten. */
        private val GEHEIM = Regex("(?i)imei|imsi|iccid|msisdn|phone|number|pin|puk|mac|ip|dns|gateway|apn|user|pwd|pass|lat|lon|location|cell|enode|enb|gnb|tac|lac|pci|eci")

        fun md5(text: String): String = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        private fun JSONObject.zahl(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null

        /** Erstes lesbares Zahlenfeld („-111“, „-111 dBm“, „-2.5“); leer, „--“ und „N/A“ zählen als nicht geliefert. */
        fun wert(o: JSONObject, vararg namen: String): Double? = namen.firstNotNullOfOrNull { n ->
            o.opt(n)?.toString()?.let { Regex("-?\\d+(?:[.,]\\d+)?").find(it)?.value?.replace(',', '.')?.toDoubleOrNull() }
        }

        /** [a] bis [d] sind die Untergrenzen von schlecht, mittel, gut und exzellent. */
        fun stufe(v: Double, a: Double, b: Double, c: Double, d: Double): Stufe = when {
            v >= d -> Stufe.EXZELLENT; v >= c -> Stufe.GUT; v >= b -> Stufe.MITTEL; v >= a -> Stufe.SCHLECHT; else -> Stufe.SEHR_SCHLECHT
        }

        fun zahl(v: Double): String = if (v == Math.floor(v) && abs(v) < 1e7) v.toLong().toString() else String.format(Locale.GERMANY, "%.1f", v)

        fun uhrzeit(zeit: Long): String {
            if (zeit <= 0) return "unbekannt"
            val uhr = java.time.Instant.ofEpochSecond(zeit).atZone(java.time.ZoneId.systemDefault())
            return if (uhr.toLocalDate() == java.time.LocalDate.now()) "%02d:%02d Uhr".format(uhr.hour, uhr.minute)
            else "%02d.%02d. %02d:%02d Uhr".format(uhr.dayOfMonth, uhr.monthValue, uhr.hour, uhr.minute)
        }

        fun alterText(s: Long): String = when {
            s < 90 -> "gerade eben"
            s < 7200 -> "vor ${s / 60} Minuten"
            s < 172800 -> "vor ${s / 3600} Stunden"
            else -> "vor ${s / 86400} Tagen"
        }
    }
}
