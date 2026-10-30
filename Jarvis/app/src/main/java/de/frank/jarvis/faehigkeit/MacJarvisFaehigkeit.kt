package de.frank.jarvis.faehigkeit

import android.content.Context
import androidx.core.app.NotificationCompat
import de.frank.jarvis.R
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.GespeicherteNachricht
import de.frank.jarvis.data.Gespraeche
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.meldung.Meldungen
import de.frank.jarvis.meldung.Meldungsarten
import de.frank.jarvis.observability.Logbuch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
 * Der Agentenkanal zwischen Jarvis am Handy und Jarvis Mac OS: zwei gleichberechtigte Agenten, die sich jederzeit schreiben,
 * solange der Mac verbunden ist. Der Relay hält die Post, bis die Gegenseite sie quittiert ([AgentenKanal] in relay.py); Post vom Mac
 * kommt als Signal über den Tunnel ([empfangen]). Fragen vom Mac beantwortet das Modell des Handys im Hintergrund, als Lauf ohne Frank
 * (lernen=false: kein Merken, keine Programmieraufträge, kein Kanal; Rückfragen stehen in der Antwort). Quittiert wird erst nach der
 * zugestellten Antwort: Stirbt der Prozess mittendrin, kommt die Frage beim nächsten Verbinden wieder. Alles steht im Gespräch
 * „Jarvis Mac OS (Agentenkanal)“, damit Frank mitlesen kann. Mitteilungen melden sich nicht: Die Abstimmung soll Frank nicht unterbrechen.
 */
class MacJarvisFaehigkeit(context: Context) : Faehigkeit {
    private val app = context.applicationContext
    private val e get() = Einstellungen.get(app)
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val speicher by lazy { app.getSharedPreferences("agentenkanal", Context.MODE_PRIVATE) }
    private val wartende = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val inArbeit = ConcurrentHashMap.newKeySet<String>()
    /** Eine Frage nach der anderen: Das Modell soll nicht parallel zu sich selbst am selben Thema arbeiten. */
    private val reihe = Mutex()

    override val id = "jarvis_mac"
    override val name = "Jarvis Mac OS"
    override val beschreibung = "Der Schwester-Agent auf Franks Mac: starten, Stand abfragen, begutachten lassen, abstimmen."
    override val hinweise =
        "Jarvis Mac OS ist dein Schwester-Agent auf Franks Mac (es gibt ihn nur dort; „Rechner“ meint hier immer den Mac) und hat Vollzugriff auf den ganzen Computer. " +
            "„Starte Jarvis Mac OS“ und im selben Zusammenhang auch das kurze „Starte Jarvis“ bedeuten jarvis_mac_starten. Sprich ihn von dir aus an, wenn etwas am Rechner zählt: " +
            "jarvis_mac_sprechen mit art=stand für den Stand von Programmieraufgaben, art=begutachten für eine Begutachtung des gesamten Geschehens, art=frage für Rückfragen, art=info für Mitteilungen. " +
            "Er fragt dich genauso. Stimmt euch laufend ab, wer was wann macht: Du hast Franks Alltag, das Handy und die Sprache im Blick, er den Mac, das Repo und die Sitzungen."

    override fun stoerung(): String? = if (e.serverHost.isBlank()) "Kein Jarvis-Server eingetragen." else null

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = "jarvis_mac_starten",
                titel = "Jarvis Mac OS starten",
                beschreibung = "Jarvis: startet Jarvis Mac OS auf Franks Mac und prüft, dass die App wirklich läuft. Nutze es bei „Starte Jarvis Mac OS“, „Starte Jarvis“ (das meint in diesem " +
                    "Zusammenhang Jarvis Mac OS), „Mach Jarvis am Mac auf“. Das Ergebnis sagt, ob sie gestartet ist, schon lief oder der Mac nicht verbunden ist; sag Frank das in einem Satz.",
                schema = schema(),
                nurLesen = false,
            ) { starten() },
            Werkzeug(
                name = "jarvis_mac_sprechen",
                titel = "Mit Jarvis Mac OS sprechen",
                beschreibung = "Jarvis: spricht über den Agentenkanal mit deinem Schwester-Agenten Jarvis Mac OS, der den ganzen Mac einsehen und bedienen kann. art=stand: aktueller Stand der " +
                    "Programmieraufgaben am Rechner. art=begutachten: Er begutachtet das gesamte Geschehen auf dem Rechner zu einer Aufgabe und sagt seine Einschätzung. art=frage: Rückfrage oder freies " +
                    "Gespräch. Bei diesen dreien wartest du bis zu einer Minute auf die Antwort; dauert es länger, kommt sie später ins Gespräch „Jarvis Mac OS (Agentenkanal)“ und als Benachrichtigung " +
                    "(nachlesen mit jarvis_mac_lesen). art=info: Mitteilung ohne Antwort, zum Beispiel wer was wann übernimmt. Läuft die App am Mac nicht, startet sie von selbst.",
                schema = schema(
                    "art" to text("stand, begutachten, frage oder info.", listOf("stand", "begutachten", "frage", "info")),
                    "text" to text("Die Nachricht, vollständig und für sich verständlich, mit dem nötigen Kontext. Bei stand darf sie kurz sein (welche Aufgabe, sonst alle)."),
                    pflicht = listOf("art", "text"),
                ),
                nurLesen = false,
            ) { a -> sprechen(a.optString("art").trim().lowercase(), a.optString("text").trim()) },
            Werkzeug(
                name = "jarvis_mac_lesen",
                titel = "Agentenkanal mit Jarvis Mac OS lesen",
                beschreibung = "Jarvis: ob Jarvis Mac OS gerade läuft und die letzten Nachrichten des Agentenkanals in beide Richtungen, auch späte Antworten. Nutze es bei „Läuft Jarvis am Mac?“ oder wenn du auf eine Antwort wartest.",
                schema = schema(),
                nurLesen = true,
            ) { lesen() },
        )
    }

    private fun rumpf(j: JSONObject) = j.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

    private fun anfrage(pfad: String) = Request.Builder().url("https://${e.serverHost}$pfad").header("X-Jarvis-Token", e.serverToken)

    private fun antwortJson(anfrage: Request, http: OkHttpClient = HTTP): JSONObject =
        http.newCall(anfrage).execute().use { r ->
            check(r.isSuccessful) { "Server antwortet mit HTTP ${r.code}" }
            JSONObject(r.body?.string().orEmpty().ifEmpty { "{}" })
        }

    private suspend fun starten(): Ergebnis = withContext(Dispatchers.IO) {
        val antwort = runCatching { antwortJson(anfrage("/geraet/mac-start?vorn=1").post(rumpf(JSONObject())).build(), START_HTTP) }
            .getOrElse { return@withContext Ergebnis("Der Jarvis-Server ist nicht erreichbar (${it.message ?: it.javaClass.simpleName}); nichts wurde gestartet. Sag Frank das kurz.", fehler = true) }
        when (antwort.optString("ergebnis")) {
            "gestartet" -> Ergebnis("Jarvis Mac OS ist gestartet und auf dem Agentenkanal erreichbar.")
            "laeuft_schon" -> Ergebnis("Jarvis Mac OS lief schon und ist auf dem Agentenkanal erreichbar" + if (antwort.optBoolean("nach_vorn")) "; die App kommt am Mac nach vorn." else ".")
            "mac_nicht_verbunden" -> Ergebnis("Der Mac ist nicht verbunden (aus, im Ruhezustand oder die Jarvis-Brücke läuft nicht); Jarvis Mac OS wurde nicht gestartet. Sag Frank das kurz.", fehler = true)
            else -> Ergebnis("Der Mac hat den Start bekommen, aber Jarvis Mac OS hat sich nach 30 Sekunden nicht gemeldet. Sag Frank, dass er am Mac nachsehen möge.", fehler = true)
        }
    }

    private suspend fun lesen(): Ergebnis = withContext(Dispatchers.IO) {
        val stand = runCatching { antwortJson(anfrage("/geraet/kanal/status").get().build()) }.getOrNull()
        val kopf = when {
            stand == null -> "Der Jarvis-Server ist gerade nicht erreichbar."
            stand.optBoolean("mac_app") -> "Jarvis Mac OS läuft und ist erreichbar."
            stand.optBoolean("bruecke") -> "Der Mac ist verbunden, Jarvis Mac OS läuft gerade nicht (jarvis_mac_starten startet es)."
            else -> "Der Mac ist nicht verbunden."
        }
        val verlauf = verlaufText(12)
        Ergebnis(kopf + "\n\n" + verlauf.ifEmpty { "Auf dem Kanal steht noch nichts." })
    }

    private suspend fun sprechen(art: String, text: String): Ergebnis {
        if (art !in setOf("stand", "begutachten", "frage", "info")) return Ergebnis("art muss stand, begutachten, frage oder info sein.", fehler = true)
        if (text.isEmpty() && art != "stand") return Ergebnis("Der Text ist leer.", fehler = true)
        val nachricht = text.ifEmpty { "Wie ist der aktuelle Stand aller Programmieraufgaben am Rechner?" }
        val kennung = UUID.randomUUID().toString()
        val wartend = CompletableDeferred<String>().takeIf { art != "info" }?.also { wartende[kennung] = it }
        val beginn = System.currentTimeMillis()
        val stand = withContext(Dispatchers.IO) {
            runCatching { sende(kennung, art, nachricht, null, versuche = 2) }
        }.getOrElse { fehler ->
            wartende.remove(kennung)
            return Ergebnis("Die Nachricht ist nicht beim Jarvis-Server angekommen (${fehler.message ?: fehler.javaClass.simpleName}); nichts wurde zugestellt. Sag Frank das kurz.", fehler = true)
        }
        ins(GESPRAECH_JARVIS, "📱", nachricht)
        if (wartend == null) return Ergebnis("Mitteilung an Jarvis Mac OS abgelegt." + if (stand.optBoolean("mac_app")) "" else " Der Mac liest sie, sobald die App läuft.")
        if (!stand.optBoolean("mac_app") && !stand.optBoolean("bruecke")) {
            wartende.remove(kennung)
            return Ergebnis("Der Mac ist gerade nicht verbunden. Die Nachricht bleibt eingereiht und geht ein, sobald er wieder da ist; die Antwort kommt dann ins Gespräch und als Benachrichtigung. Sag Frank das offen.")
        }
        // Abgebrochen (Frank stoppt, Live endet): Die spätere Antwort soll sich melden, nicht einen toten Wartenden treffen.
        val antwort = try {
            withTimeoutOrNull((WARTEN_MS - (System.currentTimeMillis() - beginn)).coerceAtLeast(1_000L)) { wartend.await() }
        } finally {
            wartende.remove(kennung)
        }
        return if (antwort != null) Ergebnis("Antwort von Jarvis Mac OS:\n$antwort")
        else Ergebnis("Jarvis Mac OS arbeitet noch (keine Antwort nach einer Minute). Die Antwort kommt von selbst ins Gespräch „Jarvis Mac OS (Agentenkanal)“ und als Benachrichtigung. Sag Frank das kurz.")
    }

    /** Auch beim Wiederholen dieselbe Kennung: Der Relay legt sie nicht doppelt ab. */
    private suspend fun sende(kennung: String, art: String, text: String, antwortAuf: String?, versuche: Int = 3): JSONObject {
        val j = JSONObject().put("id", kennung).put("art", art).put("text", text)
        if (antwortAuf != null) j.put("antwort_auf", antwortAuf)
        var letzter: Throwable? = null
        repeat(versuche) { versuch ->
            runCatching { return antwortJson(anfrage("/geraet/kanal").post(rumpf(j)).build()) }.onFailure { letzter = it }
            if (versuch < versuche - 1) delay(2_000L shl versuch)
        }
        throw letzter ?: IllegalStateException("nicht gesendet")
    }

    /** Post vom Mac (Signal „agentenkanal“ über den Tunnel). Dieselbe Nachricht kann beim Neuverbinden wiederkommen. */
    fun empfangen(nachricht: JSONObject?) {
        val kennung = nachricht?.optString("id").orEmpty()
        if (kennung.isEmpty()) return
        if (erledigt().contains(kennung)) { bereich.launch { quittiere(kennung) }; return }
        if (!inArbeit.add(kennung)) return
        bereich.launch {
            try {
                val art = nachricht!!.optString("art")
                val text = nachricht.optString("text")
                when (art) {
                    "antwort" -> {
                        ins(GESPRAECH_MAC, "💻", text)
                        val wartend = wartende.remove(nachricht.optString("antwort_auf"))
                        if (wartend != null) wartend.complete(text) else melde("Antwort von Jarvis Mac OS", text)
                    }
                    "info" -> ins(GESPRAECH_MAC, "💻", text)
                    else -> reihe.withLock { beantworte(kennung, text, nachricht.optLong("zeit")) }
                }
                merkeErledigt(kennung)
                quittiere(kennung)
            } catch (fehler: Exception) {
                if (fehler is kotlinx.coroutines.CancellationException) throw fehler
                Logbuch.fehler("Agentenkanal: Nachricht vom Mac nicht verarbeitet", fehler)
            } finally {
                inArbeit.remove(kennung)
            }
        }
    }

    private suspend fun beantworte(kennung: String, text: String, zeit: Long) {
        val verlauf = verlaufText(8)
        ins(GESPRAECH_MAC, "💻", text)
        val antwort = if (zeit > 0 && System.currentTimeMillis() / 1000 - zeit > HOECHSTALTER_S) "Deine Nachricht lag länger als sechs Stunden, ich habe sie nicht mehr bearbeitet. Frag neu, falls sie noch gilt."
        else denke(text, verlauf)
        ins(GESPRAECH_JARVIS, "📱", antwort)
        // Dieselbe Kennung bei jedem Versuch: Der Relay legt die Antwort nicht doppelt ab. Unquittiert käme die Frage erst beim nächsten Verbinden wieder.
        repeat(SENDE_RUNDEN) { runde ->
            if (runCatching { sende("antwort-$kennung", "antwort", antwort, kennung, versuche = 3) }.isSuccess) return
            if (runde < SENDE_RUNDEN - 1) delay(60_000L)
        }
        error("Antwort nicht zugestellt")
    }

    private suspend fun denke(text: String, verlauf: String): String {
        val agent = JarvisAgent(app)
        if (!agent.verbunden) return "Jarvis am Handy kann gerade nicht denken: Das Modell ist nicht verbunden."
        var grund: String? = null
        return runCatching {
            agent.versuche(
                text, zeitlimitMs = ZEITLIMIT_MS, maxSchritte = JarvisAgent.STANDARD_SCHRITTE, abschluss = true, beiAbbruch = { grund = it },
                rolle = "Diese Nachricht kommt von deinem Schwester-Agenten Jarvis Mac OS, nicht von Frank. Antworte ihm als Kollege: kurz, sachlich, für sich verständlich, " +
                    "mit dem, was Handy und Franks Apps dazu hergeben. Rückfragen und Vorschläge, wer was wann macht, schreibst du in die Antwort." +
                    if (verlauf.isEmpty()) "" else "\n\nZuletzt auf dem Kanal:\n$verlauf",
            )
        }.getOrElse { fehler ->
            if (fehler is kotlinx.coroutines.CancellationException) throw fehler
            grund = fehler.message ?: fehler.javaClass.simpleName
            null
        } ?: agent.ersatzText(grund)
    }

    private suspend fun quittiere(kennung: String) {
        repeat(3) { versuch ->
            val ok = runCatching { antwortJson(anfrage("/geraet/kanal/quittung").post(rumpf(JSONObject().put("ids", JSONArray().put(kennung)))).build()) }.isSuccess
            if (ok) return
            if (versuch < 2) delay(2_000L shl versuch)
        }
    }

    private fun erledigt(): Set<String> = speicher.getString("erledigt", "").orEmpty().split(' ').filter { it.isNotEmpty() }.toSet()

    @Synchronized
    private fun merkeErledigt(kennung: String) {
        val liste = speicher.getString("erledigt", "").orEmpty().split(' ').filter { it.isNotEmpty() } + kennung
        speicher.edit().putString("erledigt", liste.takeLast(200).joinToString(" ")).apply()
    }

    @Synchronized
    private fun gespraech(): String {
        val bekannt = speicher.getString("gespraech", "").orEmpty()
        if (bekannt.isNotEmpty() && Gespraeche.laden(app, bekannt) != null) return bekannt
        val neu = Gespraeche.neueId()
        val erste = GespeicherteNachricht(false, "💻 Jarvis Mac OS · Hier steht alles mit, was die beiden Agenten über den Kanal austauschen.", ProgrammierChat.ART_STATUS)
        Gespraeche.speichern(app, neu, System.currentTimeMillis(), listOf(erste)) ?: return ""
        Gespraeche.umbenennen(app, neu, GESPRAECH_TITEL)
        speicher.edit().putString("gespraech", neu).apply()
        return neu
    }

    private fun ins(von: String, symbol: String, text: String) {
        runCatching {
            val id = gespraech()
            if (id.isNotEmpty()) ProgrammierChat.poste(app, id, "$symbol $von · $text", "antwort")
        }.onFailure { Logbuch.fehler("Agentenkanal: nicht ins Gespräch geschrieben", it) }
    }

    private fun verlaufText(anzahl: Int): String {
        val id = speicher.getString("gespraech", "").orEmpty()
        if (id.isEmpty()) return ""
        return Gespraeche.laden(app, id)?.second.orEmpty().filter { it.art != ProgrammierChat.ART_STATUS }.takeLast(anzahl).joinToString("\n") { it.text.take(1500) }
    }

    private fun melde(titel: String, text: String) {
        runCatching {
            JarvisDienst.kanalAnlegen(app)
            val kurz = text.replace("**", "")
            Meldungen.zeige(
                app, Meldungsarten.AGENT, MELDUNG + (text.hashCode() and 0xFFF),
                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis)
                    .setContentTitle(titel).setContentText(kurz).setStyle(NotificationCompat.BigTextStyle().bigText(kurz)).setAutoCancel(true),
                titel, kurz.take(400),
            )
        }.onFailure { Logbuch.fehler("Agentenkanal: Meldung", it) }
    }

    private companion object {
        const val GESPRAECH_TITEL = "Jarvis Mac OS (Agentenkanal)"
        const val GESPRAECH_MAC = "Jarvis Mac OS"
        const val GESPRAECH_JARVIS = "Jarvis"
        /** Samt Senden unter den 75 s des Relays: Der Aufruf kann auch aus ChatGPT kommen. */
        const val WARTEN_MS = 65_000L
        const val HOECHSTALTER_S = 6 * 3600L
        const val SENDE_RUNDEN = 30
        const val ZEITLIMIT_MS = 300_000L
        const val MELDUNG = 310_001
        val HTTP: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
        val START_HTTP: OkHttpClient = HTTP.newBuilder().readTimeout(40, TimeUnit.SECONDS).callTimeout(45, TimeUnit.SECONDS).build()
    }
}
