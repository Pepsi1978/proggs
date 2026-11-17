package de.frank.jarvis.staubsauger

import android.content.Context
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder

/**
 * Live-Karte des M7 Pro (Franks Wunsch vom 17.11.2026): Solange die Live-Ansicht offen ist, hält Jarvis eine Verbindung zum
 * TCP-Gateway der Proscenic-Cloud offen und fragt alle [POS_TAKT_MS] den Status an (`cmd20001`, trägt die Position `pos` in mm)
 * und alle [WEG_TAKT_MS] den Fahrweg (`21011`, `posArray`). Die Antworten kommen verschlüsselt über dasselbe Gateway wie in
 * [ProscenicMehr.live]. Zwischen zwei Fahrweg-Abrufen verlängert jede neue Position den Strich, damit er dem Roboter ohne Lücke
 * folgt; jeder Fahrweg der Cloud ersetzt den bisherigen (ob er während der Fahrt ganz oder nur stückweise kommt, ist nicht belegt;
 * das Logbuch hält den Aufbau fest). Die Karte (20002) schickt das Gateway nach der Anmeldung von selbst; damit sie während der
 * Reinigung mitwächst, baut die Sitzung alle [NEU_VERBINDEN_MS] neu auf.
 * Jede Neu-Anmeldung macht die des anderen Geräts ungültig (gemeinsames Konto, Almanach proscenic-cloud Nr. 2): Je Öffnen meldet
 * sich die Live-Karte darum höchstens einmal neu an und hört danach mit einem Hinweis auf, statt Handy und Tablet abwechselnd abzumelden.
 */
object SaugerLive {
    private const val POS_TAKT_MS = 2_000L
    private const val WEG_TAKT_MS = 10_000L
    private const val NEU_VERBINDEN_MS = 120_000L
    /** So lange nach dem Schließen gilt der gesammelte Weg noch (Auf-/Zuklappen des Fold, Drehen). */
    private const val WEITER_NACH_MS = 60_000L
    private const val MAX_NACHTRAG = 20_000

    data class Spur(
        val karte: ProscenicMehr.Karte? = null,
        val pfad: List<Pair<Int, Int>> = emptyList(),
        val roboter: Pair<Int, Int>? = null,
        val station: Pair<Int, Int>? = null,
        val zonen: List<ProscenicMehr.Zone> = emptyList(),
        val modus: String? = null,
        val akku: Int? = null,
        /** Wann zuletzt eine Position kam (ms seit 1970); 0 = noch keine. */
        val positionUm: Long = 0L,
        val fehler: String? = null,
    )

    private val _spur = MutableStateFlow(Spur())
    val spur: StateFlow<Spur> = _spur

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var lauf: Job? = null
    /** Zählt jeden Start; Rahmen einer älteren, noch auslaufenden Sitzung werden verworfen. */
    @Volatile private var generation = 0
    @Volatile private var gestopptUm = 0L

    /** Läuft gerade eine Live-Sitzung? Dann ruhen die stillen Gateway-Abfragen der Startkarte und des Kartenwächters. */
    val aktiv: Boolean get() = lauf?.isActive == true

    /** Fahrweg laut Cloud (21011) und die seitdem live gemeldeten Positionen; angezeigt wird beides hintereinander. */
    @Volatile private var wolke: List<Pair<Int, Int>> = emptyList()
    @Volatile private var nachtrag: List<Pair<Int, Int>> = emptyList()
    @Volatile private var wegGemeldet = 0L

    @Synchronized
    fun starte(context: Context, start: ProscenicMehr.Live?) {
        if (lauf?.isActive == true) return
        val gen = ++generation
        // Kurz nach dem Schließen (Fold auf-/zugeklappt) den gesammelten Weg behalten; sonst die bekannte Karte zeigen. Den gespeicherten
        // Fahrweg nur übernehmen, wenn der Roboter steht (dann ist es der der letzten Reinigung); fährt er, kommt der Weg frisch.
        if (System.currentTimeMillis() - gestopptUm > WEITER_NACH_MS || _spur.value.karte == null) {
            wolke = if (ProscenicMehr.aktiv(start?.modus)) emptyList() else start?.pfad.orEmpty()
            nachtrag = emptyList(); wegGemeldet = 0L
            _spur.value = Spur(karte = start?.karte, pfad = wolke, roboter = start?.roboter, station = start?.station,
                zonen = start?.zonen.orEmpty(), modus = start?.modus, akku = start?.akku)
        }
        val app = context.applicationContext
        val vorher = lauf
        lauf = bereich.launch {
            // Eine noch auslaufende Sitzung erst ganz beenden: zwei Gateway-Verbindungen verdrängen sich (Almanach Nr. 3).
            vorher?.cancelAndJoin()
            var neuAngemeldet = false
            var neu = false
            while (isActive) {
                try {
                    sitzung(ProscenicCloud.sitzung(app, neu), gen)
                    neu = false
                    // Kurze Pause vor dem Neuaufbau, damit ein geschlossenes Gateway keine enge Schleife erzeugt.
                    delay(2_000)
                } catch (e: ProscenicCloud.TokenUngueltig) {
                    if (neuAngemeldet) {
                        melde(gen, "Die Anmeldung bei Proscenic ist gerade auf einem anderen Gerät oder in der Proscenic-App aktiv. Schließe die Live-Karte und öffne sie später neu.")
                        return@launch
                    }
                    neuAngemeldet = true; neu = true
                } catch (e: ProscenicCloud.Abgewiesen) {
                    melde(gen, e.message.orEmpty()); return@launch
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logbuch.netz("Staubsauger live: Verbindung unterbrochen", e.toString().take(200))
                    melde(gen, "Verbindung zur Proscenic-Cloud unterbrochen, verbinde neu …")
                    delay(3_000)
                }
            }
        }
    }

    @Synchronized
    fun stoppe() { lauf?.cancel(); gestopptUm = System.currentTimeMillis() }

    private fun melde(gen: Int, text: String) { if (gen == generation) _spur.value = _spur.value.copy(fehler = text) }

    private fun u(s: ProscenicCloud.Sitzung) = "?username=" + URLEncoder.encode(s.mail, "UTF-8")

    private val ENDE_BYTES = ProscenicCloud.ENDE.toByteArray()

    /** Position des Rahmenendes ab [von] in [b] (Länge [n]), -1 = keins. Auf Bytes, damit kein Zeichen zwischen zwei Lesungen zerbricht. */
    private fun ende(b: ByteArray, n: Int, von: Int): Int {
        var i = von
        while (i <= n - ENDE_BYTES.size) {
            var gleich = true
            for (k in ENDE_BYTES.indices) if (b[i + k] != ENDE_BYTES[k]) { gleich = false; break }
            if (gleich) return i
            i++
        }
        return -1
    }

    /** Eine Gateway-Sitzung: anmelden, dann im Takt anfragen und alle Rahmen lesen, bis [NEU_VERBINDEN_MS] um ist. */
    private suspend fun sitzung(s: ProscenicCloud.Sitzung, gen: Int) = coroutineScope {
        val adr = ProscenicCloud.post("/appInit/getSockAddr", s.token, FormBody.Builder().add("username", s.mail).add("sn", s.sn).build())
        if (adr.optInt("code") == 102) throw ProscenicCloud.TokenUngueltig()
        val e = adr.optJSONObject("data")?.optJSONArray("addr_list")?.optJSONObject(0)
        val ip = e?.optString("ip").orEmpty(); val port = e?.optInt("port") ?: 0
        if (ip.isBlank() || port <= 0) throw java.io.IOException("Die Proscenic-Cloud nennt kein Gateway (${adr.toString().take(120)})")
        val abgelehnt = java.util.concurrent.atomic.AtomicBoolean(false)
        Socket().use { sock ->
            sock.connect(InetSocketAddress(ip, port), 10_000)
            sock.soTimeout = 400
            val an = JSONObject().put("data", JSONObject().put("token", s.token).put("sn", s.sn)).put("infoType", 70001).toString() + ProscenicCloud.ENDE
            sock.getOutputStream().apply { write(an.toByteArray()); flush() }
            // Anfragen laufen neben dem Lesen her; die Antworten kommen über das Gateway.
            val anfragen = launch {
                delay(600)
                val leer = "{}".toRequestBody("application/json;charset=UTF-8".toMediaType())
                runCatching { ProscenicCloud.post("/instructions/cmd21004/${s.sn}${u(s)}", s.token, leer) }
                var naechsterWeg = 0L
                while (isActive) {
                    val r = try { ProscenicCloud.post("/instructions/cmd20001/${s.sn}${u(s)}", s.token, leer) }
                        catch (_: ProscenicCloud.TokenUngueltig) { JSONObject().put("code", 102) } catch (_: Exception) { null }
                    if (r?.optInt("code") == 102) { abgelehnt.set(true); runCatching { sock.close() }; return@launch }
                    if (System.currentTimeMillis() >= naechsterWeg) {
                        naechsterWeg = System.currentTimeMillis() + WEG_TAKT_MS
                        runCatching {
                            ProscenicCloud.post("/app/cleanRobot/21011/${s.sn}/0", s.token, FormBody.Builder().add("username", s.mail).add("pathId", "0").build())
                        }
                    }
                    delay(POS_TAKT_MS)
                }
            }
            try {
                val ein = sock.getInputStream(); val puffer = ByteArrayOutputStream(); val buf = ByteArray(65536)
                val schluss = System.currentTimeMillis() + NEU_VERBINDEN_MS
                while (System.currentTimeMillis() < schluss) {
                    ensureActive()
                    val n = try { ein.read(buf) } catch (_: java.net.SocketTimeoutException) { continue }
                        catch (ex: java.io.IOException) { if (abgelehnt.get()) throw ProscenicCloud.TokenUngueltig() else throw ex }
                    if (n < 0) break
                    // Nur die neuen Bytes nach Rahmenenden durchsuchen (plus Überlappung um die Länge der Endmarke).
                    val altLaenge = puffer.size()
                    puffer.write(buf, 0, n)
                    val daten = puffer.toByteArray(); val laenge = daten.size
                    var anfang = 0
                    var suche = maxOf(0, altLaenge - ENDE_BYTES.size + 1)
                    while (true) {
                        val i = ende(daten, laenge, suche); if (i < 0) break
                        val rahmen = String(daten, anfang, i - anfang, Charsets.UTF_8)
                        anfang = i + ENDE_BYTES.size; suche = anfang
                        val j = ProscenicCloud.nachricht(rahmen, s.token) ?: continue
                        if (gen != generation) continue
                        verarbeiten(j.optInt("infoType"), j.optJSONObject("data") ?: continue)
                    }
                    puffer.reset(); puffer.write(daten, anfang, laenge - anfang)
                }
            } finally { anfragen.cancel() }
        }
        if (abgelehnt.get()) throw ProscenicCloud.TokenUngueltig()
    }

    private fun punkte(a: org.json.JSONArray?): List<Pair<Int, Int>> =
        if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONArray(it)?.let { p -> p.optInt(0) to p.optInt(1) } }

    private fun zonen(a: org.json.JSONArray?): List<ProscenicMehr.Zone>? =
        a?.let { (0 until it.length()).mapNotNull { i -> it.optJSONObject(i)?.let(ProscenicMehr::Zone) } }

    @Synchronized
    private fun verarbeiten(typ: Int, d: JSONObject) {
        val alt = _spur.value
        when (typ) {
            20001 -> {
                val pos = d.optJSONArray("pos")?.let { it.optInt(0) to it.optInt(1) }
                val modus = d.optString("mode").ifBlank { alt.modus }
                // Neue Position verlängert den Strich, wenn der Roboter arbeitet und sich merklich bewegt hat (> 2 cm).
                val letzte = nachtrag.lastOrNull() ?: wolke.lastOrNull()
                if (pos != null && ProscenicMehr.aktiv(modus) && nachtrag.size < MAX_NACHTRAG && (letzte == null ||
                        kotlin.math.hypot((letzte.first - pos.first).toDouble(), (letzte.second - pos.second).toDouble()) > 20)) nachtrag = nachtrag + pos
                _spur.value = alt.copy(roboter = pos ?: alt.roboter, modus = modus, akku = d.optInt("elec", -1).takeIf { it in 0..100 } ?: alt.akku,
                    pfad = wolke + nachtrag, positionUm = if (pos != null) System.currentTimeMillis() else alt.positionUm, fehler = null)
            }
            20002 -> {
                val k = runCatching { ProscenicMehr.karte(d) }.getOrNull() ?: return
                _spur.value = alt.copy(karte = k, station = d.optJSONArray("chargeHandlePos")?.let { it.optInt(0) to it.optInt(1) } ?: alt.station,
                    zonen = if (alt.zonen.isEmpty()) zonen(d.optJSONArray("area")) ?: alt.zonen else alt.zonen)
            }
            21004 -> zonen(d.optJSONArray("value"))?.let { _spur.value = alt.copy(zonen = it) }
            21011 -> {
                val weg = punkte(d.optJSONArray("posArray"))
                if (weg.isEmpty()) return
                if (System.currentTimeMillis() - wegGemeldet > 60_000) {
                    wegGemeldet = System.currentTimeMillis()
                    Logbuch.netz("Staubsauger live: Fahrweg", "21011 mit ${weg.size} Punkten (bisher ${wolke.size}), Felder ${d.keys().asSequence().toList()}".take(300))
                }
                // Der Weg der Cloud ersetzt den bisherigen; die aktuelle Position hängt hinten an, damit der Strich bis zum Roboter reicht.
                wolke = weg
                nachtrag = alt.roboter?.takeIf { ProscenicMehr.aktiv(alt.modus) }?.let { listOf(it) }.orEmpty()
                _spur.value = alt.copy(pfad = wolke + nachtrag)
            }
        }
    }
}
