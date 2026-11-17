package de.frank.jarvis.staubsauger

import android.content.Context
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
 * und alle [WEG_TAKT_MS] den Fahrweg der laufenden Reinigung (`21011`, `posArray`). Die Antworten kommen verschlüsselt über
 * dasselbe Gateway wie in [ProscenicMehr.live]. Zwischen zwei Fahrweg-Abrufen verlängert jede neue Position den Strich, damit
 * er dem Roboter ohne Lücke folgt. Die Karte (20002) schickt das Gateway nach der Anmeldung von selbst; damit sie während der
 * Reinigung mitwächst, baut die Sitzung alle [NEU_VERBINDEN_MS] neu auf.
 * Läuft nur, solange jemand die Live-Ansicht ansieht ([starte]/[stoppe]); die stillen Abfragen der Startkarte ruhen so lange.
 */
object SaugerLive {
    private const val POS_TAKT_MS = 2_000L
    private const val WEG_TAKT_MS = 10_000L
    private const val NEU_VERBINDEN_MS = 120_000L

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
    private var lauf: Job? = null

    /** Läuft gerade eine Live-Sitzung? Dann ruhen die stillen Gateway-Abfragen der Startkarte und des Kartenwächters. */
    val aktiv: Boolean get() = lauf?.isActive == true

    @Synchronized
    fun starte(context: Context, start: ProscenicMehr.Live?) {
        if (lauf?.isActive == true) return
        // Sofort die bekannte Karte zeigen; Weg und Position kommen live.
        _spur.value = Spur(karte = start?.karte, pfad = start?.pfad.orEmpty(), roboter = start?.roboter, station = start?.station,
            zonen = start?.zonen.orEmpty(), modus = start?.modus, akku = start?.akku)
        val app = context.applicationContext
        lauf = bereich.launch {
            var neu = false
            while (isActive) {
                try {
                    sitzung(ProscenicCloud.sitzung(app, neu))
                    neu = false
                } catch (e: ProscenicCloud.TokenUngueltig) {
                    neu = true
                } catch (e: ProscenicCloud.Abgewiesen) {
                    _spur.value = _spur.value.copy(fehler = e.message); delay(15_000)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logbuch.netz("Staubsauger live: Verbindung unterbrochen", e.toString().take(200))
                    _spur.value = _spur.value.copy(fehler = "Verbindung zur Proscenic-Cloud unterbrochen, verbinde neu …")
                    delay(3_000)
                }
            }
        }
    }

    @Synchronized
    fun stoppe() { lauf?.cancel(); lauf = null }

    private fun u(s: ProscenicCloud.Sitzung) = "?username=" + URLEncoder.encode(s.mail, "UTF-8")

    /** Eine Gateway-Sitzung: anmelden, dann im Takt anfragen und alle Rahmen lesen, bis [NEU_VERBINDEN_MS] um ist. */
    private suspend fun sitzung(s: ProscenicCloud.Sitzung) = coroutineScope {
        val adr = ProscenicCloud.post("/appInit/getSockAddr", s.token, FormBody.Builder().add("username", s.mail).add("sn", s.sn).build())
        if (adr.optInt("code") == 102) throw ProscenicCloud.TokenUngueltig()
        val e = adr.optJSONObject("data")?.optJSONArray("addr_list")?.optJSONObject(0)
            ?: throw java.io.IOException("Gateway-Adresse fehlt")
        val abgelehnt = java.util.concurrent.atomic.AtomicBoolean(false)
        Socket().use { sock ->
            sock.connect(InetSocketAddress(e.optString("ip"), e.optInt("port")), 10_000)
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
                    val r = runCatching { ProscenicCloud.post("/instructions/cmd20001/${s.sn}${u(s)}", s.token, leer) }.getOrNull()
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
                val ende = System.currentTimeMillis() + NEU_VERBINDEN_MS
                while (System.currentTimeMillis() < ende) {
                    ensureActive()
                    val n = try { ein.read(buf) } catch (_: java.net.SocketTimeoutException) { continue }
                        catch (e: java.io.IOException) { if (abgelehnt.get()) throw ProscenicCloud.TokenUngueltig() else throw e }
                    if (n < 0) break
                    puffer.write(buf, 0, n)
                    var text = puffer.toString("UTF-8")
                    while (true) {
                        val i = text.indexOf(ProscenicCloud.ENDE); if (i < 0) break
                        val rahmen = text.substring(0, i); text = text.substring(i + ProscenicCloud.ENDE.length)
                        val j = ProscenicCloud.nachricht(rahmen, s.token) ?: continue
                        verarbeiten(j.optInt("infoType"), j.optJSONObject("data") ?: continue)
                    }
                    puffer.reset(); puffer.write(text.toByteArray())
                }
            } finally { anfragen.cancel() }
        }
        if (abgelehnt.get()) throw ProscenicCloud.TokenUngueltig()
    }

    private fun verarbeiten(typ: Int, d: JSONObject) {
        val alt = _spur.value
        when (typ) {
            20001 -> {
                val pos = d.optJSONArray("pos")?.let { it.optInt(0) to it.optInt(1) }
                val modus = d.optString("mode").ifBlank { alt.modus }
                // Neue Position verlängert den Strich, wenn der Roboter arbeitet und sich merklich bewegt hat (> 2 cm).
                val pfad = if (pos != null && ProscenicMehr.aktiv(modus) && alt.pfad.lastOrNull()?.let { (x, y) ->
                        kotlin.math.hypot((x - pos.first).toDouble(), (y - pos.second).toDouble()) > 20 } != false) alt.pfad + pos else alt.pfad
                _spur.value = alt.copy(roboter = pos ?: alt.roboter, modus = modus, akku = d.optInt("elec", -1).takeIf { it in 0..100 } ?: alt.akku,
                    pfad = pfad, positionUm = if (pos != null) System.currentTimeMillis() else alt.positionUm, fehler = null)
            }
            20002 -> {
                val k = runCatching { ProscenicMehr.karte(d) }.getOrNull() ?: return
                _spur.value = alt.copy(karte = k, station = d.optJSONArray("chargeHandlePos")?.let { it.optInt(0) to it.optInt(1) } ?: alt.station,
                    zonen = d.optJSONArray("area")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(ProscenicMehr::Zone) } }?.takeIf { alt.zonen.isEmpty() } ?: alt.zonen)
            }
            21004 -> d.optJSONArray("value")?.let { a ->
                _spur.value = alt.copy(zonen = (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(ProscenicMehr::Zone) })
            }
            21011 -> {
                val a = d.optJSONArray("posArray") ?: return
                if (a.length() == 0) return
                val weg = (0 until a.length()).mapNotNull { a.optJSONArray(it)?.let { p -> p.optInt(0) to p.optInt(1) } }
                // Der Fahrweg der Cloud ist maßgeblich; die aktuelle Position hängt hinten an, damit der Strich bis zum Roboter reicht.
                _spur.value = alt.copy(pfad = alt.roboter?.takeIf { ProscenicMehr.aktiv(alt.modus) }?.let { weg + it } ?: weg)
            }
        }
    }
}
