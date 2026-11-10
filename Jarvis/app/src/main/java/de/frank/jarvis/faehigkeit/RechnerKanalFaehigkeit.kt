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
 * Der Agentenkanal zwischen Jarvis am Handy und einem Schwester-Agenten auf Franks Rechnern (Jarvis Mac OS, Jarvis Windows HP; was je Rechner
 * anders ist, steht in [RechnerArt]): zwei gleichberechtigte Agenten, die sich jederzeit schreiben, solange der Rechner verbunden ist. Der Relay
 * hält die Post, bis die Gegenseite sie quittiert ([AgentenKanal] in relay.py); Post vom Rechner kommt als Signal über den Tunnel ([empfangen]).
 * Fragen vom Rechner beantwortet das Modell des Handys im Hintergrund, als Lauf ohne Frank (lernen=false: kein Merken, keine Programmieraufträge,
 * kein Kanal; Rückfragen stehen in der Antwort). Die fertige Antwort liegt auf der Platte, bis sie zugestellt und quittiert ist: Stirbt der Prozess
 * danach, kommt die Frage beim nächsten Verbinden wieder und wird nicht neu berechnet. Alles steht im Gespräch „<Name> (Agentenkanal)“, damit Frank
 * mitlesen kann. Mitteilungen melden sich nicht: Die Abstimmung soll Frank nicht unterbrechen. Schlüssel wie mac_app kommen unverändert vom Relay.
 */
open class RechnerKanalFaehigkeit(context: Context, private val ziel: RechnerArt) : Faehigkeit {
    private val app = context.applicationContext
    private val e get() = Einstellungen.get(app)
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val speicher by lazy { app.getSharedPreferences(ziel.speicher, Context.MODE_PRIVATE) }
    private val wartende = ConcurrentHashMap<String, CompletableDeferred<String>>()
    /** Rückruf für eine späte Helfer-Antwort, mit Anlegezeit: Kommt keine mehr, räumt der nächste Aufruf ihn weg. */
    private val spaete = ConcurrentHashMap<String, Pair<Long, (String) -> Unit>>()
    private val inArbeit = ConcurrentHashMap.newKeySet<String>()
    /** Eine Frage nach der anderen: Das Modell soll nicht parallel zu sich selbst am selben Thema arbeiten. */
    private val reihe = Mutex()

    override val id = ziel.id
    override val name = ziel.name
    override val beschreibung = ziel.beschreibung
    override val hinweise = ziel.hinweise

    override fun stoerung(): String? = if (e.serverHost.isBlank()) "Kein Jarvis-Server eingetragen." else null

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = ziel.id + "_starten",
                titel = ziel.name + " starten",
                beschreibung = ziel.startBeschreibung,
                schema = schema(),
                nurLesen = false,
            ) { starten() },
            Werkzeug(
                name = ziel.id + "_sprechen",
                titel = "Mit " + ziel.name + " sprechen",
                beschreibung = "Jarvis: spricht über den Agentenkanal mit deinem Schwester-Agenten ${ziel.name}, der den ganzen ${ziel.rechner} einsehen und bedienen kann. art=stand: aktueller Stand der " +
                    "Programmieraufgaben am Rechner. art=begutachten: Er begutachtet das gesamte Geschehen auf dem Rechner zu einer Aufgabe und sagt seine Einschätzung. art=frage: Rückfrage, Auftrag oder freies " +
                    "Gespräch. Bei diesen dreien wartest du bis zu einer Minute auf die Antwort; dauert es länger, kommt sie später ins Gespräch „${ziel.titel}“ und als Benachrichtigung " +
                    "(nachlesen mit ${ziel.id}_lesen). art=info: Mitteilung ohne Antwort, zum Beispiel wer was wann übernimmt. Läuft die App dort nicht, startet sie von selbst.",
                schema = schema(
                    "art" to text("stand, begutachten, frage oder info.", listOf("stand", "begutachten", "frage", "info")),
                    "text" to text("Die Nachricht, vollständig und für sich verständlich, mit dem nötigen Kontext. Bei stand darf sie kurz sein (welche Aufgabe, sonst alle)."),
                    pflicht = listOf("art", "text"),
                ),
                nurLesen = false,
            ) { a -> sprechen(a.optString("art").trim().lowercase(), a.optString("text").trim()) },
            Werkzeug(
                name = ziel.id + "_lesen",
                titel = "Agentenkanal mit " + ziel.name + " lesen",
                beschreibung = "Jarvis: ob ${ziel.name} gerade läuft und die letzten Nachrichten des Agentenkanals in beide Richtungen, auch späte Antworten. Nutze es bei „Läuft ${ziel.name}?“ oder wenn du auf eine Antwort wartest.",
                schema = schema(),
                nurLesen = true,
            ) { lesen() },
        )
    }

    /**
     * Hört der Rechner gerade? Auch die laufende Brücke zählt: Der Relay startet die App dann für jede Anfrage von selbst.
     * Kurzes Zeitlimit: Die Antwort darf den Katalog nicht aufhalten.
     */
    suspend fun erreichbar(): Boolean = withContext(Dispatchers.IO) {
        runCatching { antwortJson(anfrage(ziel.kanalWeg + "/status").get().build(), KURZ_HTTP).let { it.optBoolean("mac_app") || it.optBoolean("bruecke") } }.getOrDefault(false)
    }

    /**
     * Anfrage an den Rechner ohne Modell ([art] nach JarvisHelfer/README.md): null = keine Antwort in [warteMs]; dann kommt sie später an, wird aber
     * nie ein zweites Mal ausgeführt (dieselbe Kennung, Quittung, fertige Antwort am Rechner). Taucht nicht im Kanalgespräch auf.
     * [spaet] steht schon vor dem Senden bereit: So geht keine Antwort verloren, die genau beim Ablauf der Frist eintrifft.
     */
    suspend fun helferAnfrage(art: String, inhalt: JSONObject, warteMs: Long, kennung: String = "helfer-" + UUID.randomUUID(), spaet: ((String) -> Unit)? = null): String? {
        val jetzt = System.currentTimeMillis()
        spaete.entries.removeIf { jetzt - it.value.first > SPAET_FRIST_MS }
        val wartend = CompletableDeferred<String>().also { wartende[kennung] = it }
        if (spaet != null) spaete[kennung] = jetzt to spaet
        val ok = withContext(Dispatchers.IO) { runCatching { sende(kennung, art, inhalt.toString(), null, versuche = 2) }.isSuccess }
        if (!ok) { wartende.remove(kennung); spaete.remove(kennung); return null }
        return try {
            // Hat [empfangen] den Wartenden schon genommen, ist die Antwort da oder gleich da.
            withTimeoutOrNull(warteMs) { wartend.await() } ?: if (wartende.remove(kennung) == null) wartend.await() else null
        } finally {
            wartende.remove(kennung)
        }
    }

    private fun rumpf(j: JSONObject) = j.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

    private fun anfrage(pfad: String) = Request.Builder().url("https://${e.serverHost}$pfad").header("X-Jarvis-Token", e.serverToken).header(de.frank.jarvis.data.Geraet.KOPFZEILE, de.frank.jarvis.data.Geraet.kennung)

    private fun antwortJson(anfrage: Request, http: OkHttpClient = HTTP): JSONObject =
        http.newCall(anfrage).execute().use { r ->
            check(r.isSuccessful) { "Server antwortet mit HTTP ${r.code}" }
            JSONObject(r.body?.string().orEmpty().ifEmpty { "{}" })
        }

    private suspend fun starten(): Ergebnis = withContext(Dispatchers.IO) {
        val antwort = runCatching { antwortJson(anfrage(ziel.startWeg + "?vorn=1").post(rumpf(JSONObject())).build(), START_HTTP) }
            .getOrElse { return@withContext Ergebnis("Der Jarvis-Server ist nicht erreichbar (${it.message ?: it.javaClass.simpleName}); nichts wurde gestartet. Sag Frank das kurz.", fehler = true) }
        when (antwort.optString("ergebnis")) {
            "gestartet" -> Ergebnis("${ziel.name} ist gestartet und auf dem Agentenkanal erreichbar.")
            "laeuft_schon" -> Ergebnis("${ziel.name} lief schon und ist auf dem Agentenkanal erreichbar" + if (antwort.optBoolean("nach_vorn")) "; die App kommt am Rechner nach vorn." else ".")
            "mac_nicht_verbunden" -> Ergebnis("Der ${ziel.rechner} ist nicht verbunden (aus, im Ruhezustand oder der Dienst dort läuft nicht); ${ziel.name} wurde nicht gestartet. Sag Frank das kurz.", fehler = true)
            else -> Ergebnis("Der ${ziel.rechner} hat den Start bekommen, aber ${ziel.name} hat sich nach 30 Sekunden nicht gemeldet. Sag Frank, dass er dort nachsehen möge.", fehler = true)
        }
    }

    private suspend fun lesen(): Ergebnis = withContext(Dispatchers.IO) {
        val stand = runCatching { antwortJson(anfrage(ziel.kanalWeg + "/status").get().build()) }.getOrNull()
        val kopf = when {
            stand == null -> "Der Jarvis-Server ist gerade nicht erreichbar."
            stand.optBoolean("mac_app") -> "${ziel.name} läuft und ist erreichbar."
            stand.optBoolean("bruecke") -> "Der ${ziel.rechner} ist verbunden, ${ziel.name} läuft gerade nicht (${ziel.id}_starten startet es)."
            else -> "Der ${ziel.rechner} ist nicht verbunden."
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
        // Abgebrochen (Frank stoppt, Live endet), auch schon beim Senden: Die spätere Antwort soll sich melden, nicht einen toten Wartenden treffen.
        try {
            val stand = withContext(Dispatchers.IO) {
                runCatching { sende(kennung, art, nachricht, null, versuche = 2) }
            }.getOrElse { fehler ->
                return Ergebnis("Die Nachricht ist nicht beim Jarvis-Server angekommen (${fehler.message ?: fehler.javaClass.simpleName}); nichts wurde zugestellt. Sag Frank das kurz.", fehler = true)
            }
            ins(GESPRAECH_JARVIS, "📱", nachricht)
            if (wartend == null) return Ergebnis("Mitteilung an ${ziel.name} abgelegt." + if (stand.optBoolean("mac_app")) "" else " Der ${ziel.rechner} liest sie, sobald die App läuft.")
            if (!stand.optBoolean("mac_app") && !stand.optBoolean("bruecke")) {
                return Ergebnis("Der ${ziel.rechner} ist gerade nicht verbunden. Die Nachricht bleibt eingereiht und geht ein, sobald er wieder da ist; die Antwort kommt dann ins Gespräch und als Benachrichtigung. Sag Frank das offen.")
            }
            // Hat [empfangen] den Wartenden beim Ablauf der Frist schon genommen, ist die Antwort da oder gleich da.
            val antwort = withTimeoutOrNull((WARTEN_MS - (System.currentTimeMillis() - beginn)).coerceAtLeast(1_000L)) { wartend.await() }
                ?: if (wartende.remove(kennung) == null) wartend.await() else null
            return if (antwort != null) Ergebnis("Antwort von ${ziel.name}:\n$antwort")
            else Ergebnis("${ziel.name} arbeitet noch (keine Antwort nach einer Minute). Die Antwort kommt von selbst ins Gespräch „${ziel.titel}“ und als Benachrichtigung. Sag Frank das kurz.")
        } finally {
            wartende.remove(kennung)
        }
    }

    /** Auch beim Wiederholen dieselbe Kennung: Der Relay legt sie nicht doppelt ab. */
    private suspend fun sende(kennung: String, art: String, text: String, antwortAuf: String?, versuche: Int = 3): JSONObject {
        val j = JSONObject().put("id", kennung).put("art", art).put("text", text)
        if (antwortAuf != null) j.put("antwort_auf", antwortAuf)
        var letzter: Throwable? = null
        repeat(versuche) { versuch ->
            runCatching { return antwortJson(anfrage(ziel.kanalWeg).post(rumpf(j)).build()) }.onFailure { letzter = it }
            if (versuch < versuche - 1) delay(2_000L shl versuch)
        }
        throw letzter ?: IllegalStateException("nicht gesendet")
    }

    /** Post vom Rechner (Signal „agentenkanal“ über den Tunnel). Dieselbe Nachricht kann beim Neuverbinden wiederkommen. */
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
                    // Der Rechner meldet seinen Helferkatalog: nur das Abbild aktualisieren, ohne Modell und ohne Eintrag im Gespräch.
                    "helfer_katalog" -> if (ziel.helferKatalog) runCatching { JSONObject(text).optJSONArray("helfer")?.let { de.frank.jarvis.helfer.HelferKatalog.macKatalogUebernehmen(app, it) } }
                        .onFailure { Logbuch.fehler("Agentenkanal: Helferkatalog", it) }
                    "antwort" -> if (nachricht.optString("antwort_auf").startsWith("helfer-")) {
                        // Ergebnis ohne Wartenden (der Lauf dauerte länger): nichts wiederholen, nur Bescheid sagen.
                        val wartend = wartende.remove(nachricht.optString("antwort_auf"))
                        val nachher = spaete.remove(nachricht.optString("antwort_auf"))
                        if (wartend != null) wartend.complete(text) else if (nachher != null) nachher.second(text) else melde("${ziel.rechner}-Helfer fertig", runCatching { JSONObject(text).optString("text") }.getOrDefault("").ifEmpty { "Das späte Ergebnis ist da." }.take(300))
                    } else {
                        ins(ziel.name, "💻", text)
                        val wartend = wartende.remove(nachricht.optString("antwort_auf"))
                        if (wartend != null) wartend.complete(text) else melde("Antwort von ${ziel.name}", text)
                    }
                    "info" -> ins(ziel.name, "💻", text)
                    else -> {
                        val antwort = fertigeAntwort(kennung) ?: reihe.withLock { beantworte(kennung, text, nachricht.optLong("zeit")) }
                        // Außerhalb der Reihe: Ein kurz fehlender Relay soll die nächsten Fragen nicht aufhalten.
                        stelleZu(kennung, antwort)
                    }
                }
                merkeErledigt(kennung)
                vergissAntwort(kennung)
                quittiere(kennung)
            } catch (fehler: Exception) {
                if (fehler is kotlinx.coroutines.CancellationException) throw fehler
                Logbuch.fehler("Agentenkanal: Nachricht von ${ziel.name} nicht verarbeitet", fehler)
            } finally {
                inArbeit.remove(kennung)
            }
        }
    }

    private suspend fun beantworte(kennung: String, text: String, zeit: Long): String {
        // Während der Lauf wartete, kann eine zweite Zustellung derselben Frage ihn schon erledigt haben.
        fertigeAntwort(kennung)?.let { return it }
        val verlauf = verlaufText(8)
        ins(ziel.name, "💻", text)
        val antwort = if (zeit > 0 && System.currentTimeMillis() / 1000 - zeit > HOECHSTALTER_S) "Deine Nachricht lag länger als sechs Stunden, ich habe sie nicht mehr bearbeitet. Frag neu, falls sie noch gilt."
        else denke(text, verlauf)
        ins(GESPRAECH_JARVIS, "📱", antwort)
        merkeAntwort(kennung, antwort)
        return antwort
    }

    private suspend fun stelleZu(kennung: String, antwort: String) {
        // Dieselbe Kennung bei jedem Versuch: Der Relay legt die Antwort nicht doppelt ab. Unquittiert käme die Frage beim nächsten Verbinden wieder; dann geht nur die gesicherte Antwort noch einmal raus.
        repeat(SENDE_RUNDEN) { runde ->
            if (runCatching { sende("antwort-$kennung", "antwort", antwort, kennung, versuche = 3) }.isSuccess) return
            if (runde < SENDE_RUNDEN - 1) delay(60_000L)
        }
        error("Antwort nicht zugestellt")
    }

    private fun fertigeAntwort(kennung: String): String? =
        speicher.getString(ANTWORT + kennung, null)?.let { runCatching { JSONObject(it).getString("text") }.getOrNull() }

    /** Synchron auf die Platte: Genau für das Prozessende zwischen Denken und Quittung. Alles, was der Relay nicht mehr nachreichen kann, räumt sich hier mit weg. */
    @Synchronized
    private fun merkeAntwort(kennung: String, antwort: String) {
        val jetzt = System.currentTimeMillis()
        val aendern = speicher.edit()
        for ((schluessel, wert) in speicher.all) {
            if (!schluessel.startsWith(ANTWORT)) continue
            val zeit = runCatching { JSONObject(wert as String).getLong("zeit") }.getOrDefault(0L)
            if (jetzt - zeit > SPAET_FRIST_MS) aendern.remove(schluessel)
        }
        aendern.putString(ANTWORT + kennung, JSONObject().put("zeit", jetzt).put("text", antwort).toString()).commit()
    }

    @Synchronized
    private fun vergissAntwort(kennung: String) {
        speicher.edit().remove(ANTWORT + kennung).apply()
    }

    private suspend fun denke(text: String, verlauf: String): String {
        val agent = JarvisAgent(app)
        if (!agent.verbunden) return "Jarvis am Handy kann gerade nicht denken: Das Modell ist nicht verbunden."
        var grund: String? = null
        return runCatching {
            agent.versuche(
                text, zeitlimitMs = ZEITLIMIT_MS, maxSchritte = JarvisAgent.STANDARD_SCHRITTE, abschluss = true, beiAbbruch = { grund = it },
                rolle = "Diese Nachricht kommt von deinem Schwester-Agenten ${ziel.name}, nicht von Frank. Antworte ihm als Kollege: kurz, sachlich, für sich verständlich, " +
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
            val ok = runCatching { antwortJson(anfrage(ziel.kanalWeg + "/quittung").post(rumpf(JSONObject().put("ids", JSONArray().put(kennung)))).build()) }.isSuccess
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
        val erste = GespeicherteNachricht(false, "💻 ${ziel.name} · Hier steht alles mit, was die beiden Agenten über den Kanal austauschen.", ProgrammierChat.ART_STATUS)
        Gespraeche.speichern(app, neu, System.currentTimeMillis(), listOf(erste)) ?: return ""
        Gespraeche.umbenennen(app, neu, ziel.titel)
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
                app, Meldungsarten.AGENT, ziel.meldung + (text.hashCode() and 0xFFF),
                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis)
                    .setContentTitle(titel).setContentText(kurz).setStyle(NotificationCompat.BigTextStyle().bigText(kurz)).setAutoCancel(true),
                titel, kurz.take(400),
            )
        }.onFailure { Logbuch.fehler("Agentenkanal: Meldung", it) }
    }

    private companion object {
        const val GESPRAECH_JARVIS = "Jarvis"
        const val ANTWORT = "antwort:"
        /** So lange hält der Relay Post (KANAL_ALTER_S): Später kommt nichts mehr nach. */
        const val SPAET_FRIST_MS = 7 * 86_400_000L
        /** Samt Senden unter den 75 s des Relays: Der Aufruf kann auch aus ChatGPT kommen. */
        const val WARTEN_MS = 65_000L
        const val HOECHSTALTER_S = 6 * 3600L
        const val SENDE_RUNDEN = 30
        const val ZEITLIMIT_MS = 300_000L
        val HTTP: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
        val KURZ_HTTP: OkHttpClient = HTTP.newBuilder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(4, TimeUnit.SECONDS).callTimeout(5, TimeUnit.SECONDS).build()
        val START_HTTP: OkHttpClient = HTTP.newBuilder().readTimeout(40, TimeUnit.SECONDS).callTimeout(45, TimeUnit.SECONDS).build()
    }
}

/** Was je Rechner anders ist: Name, Wege beim Relay, Speicher. Der Kanal selbst ist für alle gleich. */
class RechnerArt(
    val id: String, val name: String, val rechner: String, val speicher: String, val kanalWeg: String, val startWeg: String, val meldung: Int,
    val beschreibung: String, val hinweise: String, val startBeschreibung: String, val helferKatalog: Boolean,
) {
    val titel get() = "$name (Agentenkanal)"
}

class MacJarvisFaehigkeit(context: Context) : RechnerKanalFaehigkeit(context, ART) {
    private companion object {
        val ART = RechnerArt(
            id = "jarvis_mac", name = "Jarvis Mac OS", rechner = "Mac", speicher = "agentenkanal", kanalWeg = "/geraet/kanal", startWeg = "/geraet/mac-start", meldung = 310_001,
            beschreibung = "Der Schwester-Agent auf Franks Mac: starten, Stand abfragen, begutachten lassen, abstimmen.",
            hinweise = "Jarvis Mac OS ist dein Schwester-Agent auf Franks Mac und hat Vollzugriff auf den ganzen Computer. Daneben gibt es Jarvis Windows HP auf Franks HP-Laptop; „Rechner“ ohne weitere Angabe meint den Mac, „Windows“ oder „HP“ meint Jarvis Windows HP. " +
                "„Starte Jarvis Mac OS“ und im selben Zusammenhang auch das kurze „Starte Jarvis“ bedeuten jarvis_mac_starten. Sprich ihn von dir aus an, wenn etwas am Rechner zählt: " +
                "jarvis_mac_sprechen mit art=stand für den Stand von Programmieraufgaben, art=begutachten für eine Begutachtung des gesamten Geschehens, art=frage für Rückfragen, art=info für Mitteilungen. " +
                "Er fragt dich genauso. Stimmt euch laufend ab, wer was wann macht: Du hast Franks Alltag, das Handy und die Sprache im Blick, er den Mac, das Repo und die Sitzungen.",
            startBeschreibung = "Jarvis: startet Jarvis Mac OS auf Franks Mac und prüft, dass die App wirklich läuft. Nutze es bei „Starte Jarvis Mac OS“, „Starte Jarvis“ (das meint in diesem " +
                "Zusammenhang Jarvis Mac OS), „Mach Jarvis am Mac auf“. Das Ergebnis sagt, ob sie gestartet ist, schon lief oder der Mac nicht verbunden ist; sag Frank das in einem Satz.",
            helferKatalog = true,
        )
    }
}

/** Jarvis Windows HP: derselbe Agent auf Franks HP-Laptop, eigener Kanal beim Relay. Seine Helfer spricht das Handy über art=frage an, nicht über den Helferkatalog. */
class WindowsHpJarvisFaehigkeit(context: Context) : RechnerKanalFaehigkeit(context, ART) {
    private companion object {
        val ART = RechnerArt(
            id = "jarvis_windows_hp", name = "Jarvis Windows HP", rechner = "HP-Laptop", speicher = "agentenkanal_hp", kanalWeg = "/geraet/hp/kanal", startWeg = "/geraet/hp/start", meldung = 320_001,
            beschreibung = "Der Schwester-Agent auf Franks HP-Laptop (Windows): starten, Aufgaben geben, Stand abfragen, begutachten lassen, abstimmen.",
            hinweise = "Jarvis Windows HP ist dein Schwester-Agent auf Franks HP-Laptop mit Windows und hat Vollzugriff auf den ganzen Computer: Computer Use mit allen Programmen, Terminal und CLI-Sitzungen. Frank nennt ihn auch „Windows HP“, „Jarvis Windows“ oder „Jarvis auf dem HP“. " +
                "„Starte Windows HP“ bedeutet jarvis_windows_hp_starten. Aufgaben und Fragen für den Laptop gibst du mit jarvis_windows_hp_sprechen: art=frage für Aufträge, Rückfragen und freies Gespräch, art=stand für den Stand von Programmieraufgaben, art=begutachten für eine Begutachtung des gesamten Geschehens, art=info für Mitteilungen. " +
                "Er fragt dich genauso. Stimmt euch laufend ab: Du hast Franks Alltag, das Handy und die Sprache im Blick, er den Laptop, das Repo und die Sitzungen dort.",
            startBeschreibung = "Jarvis: startet Jarvis Windows HP auf Franks HP-Laptop und prüft, dass die App wirklich läuft. Nutze es bei „Starte Windows HP“, „Starte Jarvis Windows“, „Mach Jarvis auf dem HP auf“. " +
                "Das Ergebnis sagt, ob sie gestartet ist, schon lief oder der Laptop nicht verbunden ist; sag Frank das in einem Satz.",
            helferKatalog = false,
        )
    }
}
