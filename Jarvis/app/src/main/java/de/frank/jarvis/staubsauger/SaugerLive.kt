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
 * Live-Karte des M7 Pro (Franks Wunsch vom 17.11.2026): Jarvis hält eine Verbindung zum TCP-Gateway der Proscenic-Cloud offen und
 * fragt alle [POS_TAKT_MS] den Status an (`cmd20001`, trägt die Position `pos` in mm) und alle [WEG_TAKT_MS] den Fahrweg (`21011`,
 * `posArray`). Die Antworten kommen verschlüsselt über dasselbe Gateway wie in [ProscenicMehr.live].
 *
 * Aufzeichnung ab Reinigungsbeginn (Franks Wunsch vom 17.11.2026): Sobald der Roboter losfährt (von Jarvis gestartet, vom Kartenwächter
 * oder einem Statusabruf erkannt), läuft die Sitzung im Hintergrund weiter, auch ohne offene Ansicht, und sammelt jede Position in
 * [eigen]. Die Cloud liefert den Fahrweg während der Fahrt nicht verlässlich (21011 kam leer); die eigene Spur ist darum maßgeblich,
 * sobald sie seit Reinigungsbeginn läuft. Sie liegt zusätzlich in einer Datei, damit ein WLAN-Aussetzer oder Neustart sie nicht verliert.
 * Ohne Betrachter endet die Sitzung, wenn der Roboter [RUHE_ENDE_MS] ruht oder [STILLE_ENDE_MS] lang keine Position kommt.
 *
 * Das Gateway verträgt nur eine Verbindung je Konto (Almanach proscenic-cloud Nr. 3): Andere Gateway-Nutzer in Jarvis laufen über
 * [gatewayFrei], das die Sitzung kurz trennt und danach neu verbindet.
 * Jede Neu-Anmeldung macht die des anderen Geräts ungültig (Almanach Nr. 2): Je Start meldet sich die Sitzung darum höchstens einmal neu an.
 */
object SaugerLive {
    private const val POS_TAKT_MS = 2_000L
    private const val POS_TAKT_HINTERGRUND_MS = 4_000L
    private const val WEG_TAKT_MS = 10_000L
    private const val NEU_VERBINDEN_MS = 120_000L
    /** So lange nach dem Schließen gilt der gesammelte Weg noch (Auf-/Zuklappen des Fold, Drehen). */
    private const val WEITER_NACH_MS = 60_000L
    private const val MAX_PUNKTE = 20_000
    /** Ohne Betrachter: so lange darf der Roboter ruhen (Station, Störung), bevor die Aufzeichnung endet. */
    private const val RUHE_ENDE_MS = 90_000L
    /** Ohne Betrachter: so lange ohne jede Position (WLAN weg), bevor die Sitzung aufgibt; der Kartenwächter startet sie wieder. */
    private const val STILLE_ENDE_MS = 5 * 60_000L
    /** So lange gilt eine gemerkte Spur als Fortsetzung derselben Reinigung. */
    private const val SPUR_GILT_MS = 20 * 60_000L

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

    /** Fahrweg laut Cloud (21011) und die seitdem gemeldeten Positionen. */
    @Volatile private var wolke: List<Pair<Int, Int>> = emptyList()
    @Volatile private var nachtrag: List<Pair<Int, Int>> = emptyList()
    /** Eigene Aufzeichnung aller Positionen der laufenden Reinigung. */
    @Volatile private var eigen: List<Pair<Int, Int>> = emptyList()
    /** Läuft [eigen] seit Reinigungsbeginn? Dann ist sie vollständig und geht dem Weg der Cloud vor. */
    @Volatile private var seitBeginn = false
    @Volatile private var wegGemeldet = 0L
    private val betrachter = java.util.concurrent.atomic.AtomicInteger(0)
    private val fremde = java.util.concurrent.atomic.AtomicInteger(0)
    @Volatile private var verbunden = false
    @Volatile private var ruhigSeit = 0L
    @Volatile private var lebenszeichen = 0L
    @Volatile private var gemerktUm = 0L
    @Volatile private var app: Context? = null

    /** Eine Ansicht (Vollbild oder eingebettete Karte) zeigt die Live-Karte; startet die Sitzung, falls sie noch nicht läuft. */
    fun oeffne(context: Context, start: ProscenicMehr.Live?) {
        betrachter.incrementAndGet()
        starte(context, start, beginn = false)
    }

    /** Eine Ansicht schließt. Arbeitet der Roboter noch, zeichnet die Sitzung im Hintergrund weiter. */
    @Synchronized
    fun schliesse() {
        if (betrachter.decrementAndGet() > 0) return
        betrachter.set(0)
        if (!ProscenicMehr.aktiv(_spur.value.modus)) stoppe()
    }

    /**
     * Der Roboter arbeitet (oder Jarvis hat ihn eben gestartet, [beginn] = true): Aufzeichnung im Hintergrund starten.
     * Nur auf dem Handy; Handy und Tablet teilen das Proscenic-Konto, jede Anmeldung macht die andere ungültig.
     */
    fun hintergrund(context: Context, stand: ProscenicMehr.Live?, beginn: Boolean) {
        if (de.frank.jarvis.data.Geraet.istTablet || !ProscenicCloud.konfiguriert(context)) return
        // Läuft die Aufzeichnung schon und arbeitet er bereits (etwa „Reinigen“ während er reinigt), bleibt die Spur, wie sie ist.
        if (aktiv && (!beginn || ProscenicMehr.aktiv(_spur.value.modus))) return
        val c = context.applicationContext
        bereich.launch {
            val start = stand?.takeIf { it.karte != null } ?: ProscenicMehr.ausSpeicher(c)?.let { k -> if (stand == null) k else k.mitZustand(stand) } ?: stand
            Logbuch.netz("Staubsauger live", if (beginn) "Reinigung gestartet, Aufzeichnung läuft" else "Roboter arbeitet, Aufzeichnung läuft im Hintergrund")
            starte(c, start, beginn)
        }
    }

    /**
     * Für andere Gateway-Nutzer in Jarvis (Statusabruf, Befehle): trennt die Live-Sitzung, solange [block] läuft, und lässt sie
     * danach neu verbinden. Ohne laufende Sitzung läuft [block] direkt.
     */
    suspend fun <T> gatewayFrei(block: suspend () -> T): T {
        if (!aktiv) return block()
        fremde.incrementAndGet()
        try {
            val bis = System.currentTimeMillis() + 2_000
            while (verbunden && System.currentTimeMillis() < bis) delay(100)
            return block()
        } finally { fremde.decrementAndGet() }
    }

    /** Angezeigter Weg: die eigene Spur, solange sie seit Reinigungsbeginn läuft; sonst der Weg der Cloud mit Nachtrag. */
    private fun anzeige(): List<Pair<Int, Int>> =
        if ((seitBeginn || wolke.isEmpty()) && eigen.isNotEmpty()) eigen else wolke + nachtrag

    private fun neueReinigung() {
        eigen = emptyList(); wolke = emptyList(); nachtrag = emptyList(); seitBeginn = true; gemerktUm = 0L
        app?.let { spurLoeschen(it) }
    }

    @Synchronized
    private fun starte(context: Context, start: ProscenicMehr.Live?, beginn: Boolean) {
        val c = context.applicationContext
        app = c
        if (lauf?.isActive == true) {
            if (beginn) { neueReinigung(); _spur.value = _spur.value.copy(pfad = anzeige()) }
            val alt = _spur.value
            if (alt.karte == null && start?.karte != null)
                _spur.value = alt.copy(karte = start.karte, station = alt.station ?: start.station, zonen = alt.zonen.ifEmpty { start.zonen })
            return
        }
        val gen = ++generation
        // Kurz nach dem Schließen (Fold auf-/zugeklappt) den gesammelten Weg behalten; sonst neu aufsetzen: Fährt er, mit der gemerkten
        // Spur derselben Reinigung (falls vorhanden), steht er, mit dem gespeicherten Fahrweg der letzten Reinigung.
        if (beginn || System.currentTimeMillis() - gestopptUm > WEITER_NACH_MS || _spur.value.karte == null) {
            val arbeitet = beginn || ProscenicMehr.aktiv(start?.modus)
            val gemerkt = if (!beginn && (arbeitet || start?.modus == null)) gemerkteSpur(c) else null
            eigen = gemerkt?.first.orEmpty(); seitBeginn = beginn || gemerkt?.second == true
            wolke = if (arbeitet || gemerkt != null) emptyList() else start?.pfad.orEmpty()
            nachtrag = emptyList(); wegGemeldet = 0L; gemerktUm = 0L
            if (beginn) spurLoeschen(c)
            _spur.value = Spur(karte = start?.karte, pfad = anzeige(), roboter = start?.roboter, station = start?.station,
                zonen = start?.zonen.orEmpty(), modus = start?.modus, akku = start?.akku)
        }
        ruhigSeit = 0L; lebenszeichen = System.currentTimeMillis()
        val vorher = lauf
        lauf = bereich.launch {
            // Eine noch auslaufende Sitzung erst ganz beenden: zwei Gateway-Verbindungen verdrängen sich (Almanach Nr. 3).
            vorher?.cancelAndJoin()
            var neuAngemeldet = false
            var neu = false
            while (isActive) {
                if (sollEnden()) { beenden(c); return@launch }
                // Ein anderer Gateway-Nutzer in Jarvis ist dran: warten, dann neu verbinden.
                if (fremde.get() > 0) { delay(300); continue }
                try {
                    sitzung(ProscenicCloud.sitzung(c, neu), gen)
                    neu = false
                    // Kurze Pause vor dem Neuaufbau, damit ein geschlossenes Gateway keine enge Schleife erzeugt.
                    if (fremde.get() == 0) delay(2_000)
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

    /** Ohne Betrachter aufhören, wenn der Roboter ruht oder lange nichts mehr kam. */
    private fun sollEnden(): Boolean {
        if (betrachter.get() > 0) return false
        val jetzt = System.currentTimeMillis()
        return (ruhigSeit > 0 && jetzt - ruhigSeit > RUHE_ENDE_MS) || jetzt - lebenszeichen > STILLE_ENDE_MS
    }

    @Synchronized
    private fun beenden(c: Context) {
        gestopptUm = System.currentTimeMillis()
        // Reinigung vorbei: die gemerkte Spur gilt nicht mehr (die nächste Reinigung beginnt neu).
        if (ruhigSeit > 0) spurLoeschen(c)
        Logbuch.netz("Staubsauger live", "Aufzeichnung beendet (${eigen.size} Punkte)")
    }

    @Synchronized
    private fun stoppe() { lauf?.cancel(); gestopptUm = System.currentTimeMillis() }

    // ---- Gemerkte Spur (übersteht WLAN-Aussetzer und Neustarts während einer Reinigung) ----

    private fun spurDatei(c: Context) = java.io.File(c.filesDir, "staubsauger_spur.json")

    private fun spurLoeschen(c: Context) { runCatching { spurDatei(c).delete() } }

    private fun spurMerken(c: Context) {
        runCatching {
            val a = org.json.JSONArray(); eigen.forEach { a.put(it.first); a.put(it.second) }
            val j = JSONObject().put("um", System.currentTimeMillis()).put("beginn", seitBeginn).put("p", a)
            val ziel = spurDatei(c); val tmp = java.io.File(ziel.path + ".tmp")
            tmp.writeText(j.toString()); if (!tmp.renameTo(ziel)) tmp.delete()
        }
    }

    private fun gemerkteSpur(c: Context): Pair<List<Pair<Int, Int>>, Boolean>? = runCatching {
        val d = spurDatei(c).takeIf { it.exists() } ?: return null
        val j = JSONObject(d.readText())
        if (System.currentTimeMillis() - j.optLong("um") > SPUR_GILT_MS) return null
        val a = j.optJSONArray("p") ?: return null
        (0 until a.length() / 2).map { a.optInt(2 * it) to a.optInt(2 * it + 1) } to j.optBoolean("beginn")
    }.getOrNull()

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
            verbunden = true
            val an = JSONObject().put("data", JSONObject().put("token", s.token).put("sn", s.sn)).put("infoType", 70001).toString() + ProscenicCloud.ENDE
            sock.getOutputStream().apply { write(an.toByteArray()); flush() }
            // Anfragen laufen neben dem Lesen her; die Antworten kommen über das Gateway.
            val anfragen = launch {
                delay(600)
                val leer = "{}".toRequestBody("application/json;charset=UTF-8".toMediaType())
                runCatching { ProscenicCloud.post("/instructions/cmd21004/${s.sn}${u(s)}", s.token, leer) }
                var naechsterWeg = 0L
                var letzteOnlinePruefung = 0L
                while (isActive) {
                    val r = try { ProscenicCloud.post("/instructions/cmd20001/${s.sn}${u(s)}", s.token, leer) }
                        catch (_: ProscenicCloud.TokenUngueltig) { JSONObject().put("code", 102) } catch (_: Exception) { null }
                    if (r?.optInt("code") == 102) { abgelehnt.set(true); runCatching { sock.close() }; return@launch }
                    // „failure“ heißt meist: Roboter gerade nicht verbunden. Höchstens alle 15 Sekunden nachfragen und es anzeigen.
                    if (r != null && r.optInt("code", 0) != 0 && System.currentTimeMillis() - letzteOnlinePruefung > 15_000L) {
                        letzteOnlinePruefung = System.currentTimeMillis()
                        if (runCatching { ProscenicCloud.online(s) }.getOrNull() == false) melde(gen, ProscenicCloud.OFFLINE_TEXT)
                    }
                    // Den Weg der Cloud nur holen, solange die eigene Spur nicht seit Reinigungsbeginn läuft (dann wird er nicht gebraucht).
                    if (System.currentTimeMillis() >= naechsterWeg && !(seitBeginn && eigen.isNotEmpty())) {
                        naechsterWeg = System.currentTimeMillis() + WEG_TAKT_MS
                        runCatching {
                            ProscenicCloud.post("/app/cleanRobot/21011/${s.sn}/0", s.token, FormBody.Builder().add("username", s.mail).add("pathId", "0").build())
                        }
                    }
                    delay(if (betrachter.get() > 0) POS_TAKT_MS else POS_TAKT_HINTERGRUND_MS)
                }
            }
            try {
                val ein = sock.getInputStream(); val puffer = ByteArrayOutputStream(); val buf = ByteArray(65536)
                val schluss = System.currentTimeMillis() + NEU_VERBINDEN_MS
                while (System.currentTimeMillis() < schluss) {
                    ensureActive()
                    if (fremde.get() > 0 || sollEnden()) break
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
            } finally { anfragen.cancel(); verbunden = false }
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
                val jetzt = System.currentTimeMillis()
                // Ein Status kommt nur von einem verbundenen Roboter: Erreichbarkeit auch während der Live-Karte aktuell halten.
                if (SaugerStand.erreichbar != true || jetzt - SaugerStand.erreichbarUm > 30_000L) SaugerStand.erreichbar = true
                // Vom Ruhezustand in die Arbeit: eine neue Reinigung beginnt, die Spur fängt neu an.
                if (alt.modus != null && !ProscenicMehr.aktiv(alt.modus) && ProscenicMehr.aktiv(modus)) neueReinigung()
                if (ProscenicMehr.aktiv(modus)) ruhigSeit = 0L else if (ruhigSeit == 0L) ruhigSeit = jetzt
                if (pos != null) lebenszeichen = jetzt
                // Neue Position verlängert den Strich, wenn der Roboter arbeitet und sich merklich bewegt hat (> 2 cm).
                fun weit(l: Pair<Int, Int>?) = l == null || kotlin.math.hypot((l.first - pos!!.first).toDouble(), (l.second - pos.second).toDouble()) > 20
                if (pos != null && ProscenicMehr.aktiv(modus)) {
                    if (eigen.size < MAX_PUNKTE && weit(eigen.lastOrNull())) eigen = eigen + pos
                    if (nachtrag.size < MAX_PUNKTE && weit(nachtrag.lastOrNull() ?: wolke.lastOrNull())) nachtrag = nachtrag + pos
                    if (jetzt - gemerktUm > 30_000) { gemerktUm = jetzt; app?.let { spurMerken(it) } }
                }
                _spur.value = alt.copy(roboter = pos ?: alt.roboter, modus = modus, akku = d.optInt("elec", -1).takeIf { it in 0..100 } ?: alt.akku,
                    pfad = anzeige(), positionUm = if (pos != null) jetzt else alt.positionUm, fehler = null)
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
                // Läuft die eigene Spur seit Reinigungsbeginn, bleibt sie maßgeblich: Ein Teilweg der Cloud würde sie sonst überschreiben.
                if (seitBeginn && eigen.isNotEmpty() && ProscenicMehr.aktiv(alt.modus)) return
                // Sonst ersetzt der Weg der Cloud den bisherigen; die aktuelle Position hängt hinten an, damit der Strich bis zum Roboter reicht.
                wolke = weg
                nachtrag = alt.roboter?.takeIf { ProscenicMehr.aktiv(alt.modus) }?.let { listOf(it) }.orEmpty()
                if (!ProscenicMehr.aktiv(alt.modus)) { eigen = emptyList(); seitBeginn = false }
                _spur.value = alt.copy(pfad = anzeige())
            }
        }
    }
}
