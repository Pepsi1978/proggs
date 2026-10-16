package de.frank.jarvis.live

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Live-Gespräch mit OpenAI, PCM16 mit 24 kHz in beide Richtungen, über zwei getrennte Protokolle:
 *
 * - **GPT-Live** (`gpt-live-1`, `wss://…/v1/live/sessions`): hört und spricht gleichzeitig. Denken übernimmt entweder ein
 *   Denk-Modell bei OpenAI (Responses-Delegation, das per Funktion `jarvis_fragen` den Jarvis-Agenten ruft) oder Jarvis
 *   selbst (Client-Delegation: Auftrag aus der Mitschrift, Antwort per `session.commentary.append`).
 * - **Realtime** (`gpt-realtime-*`, `wss://…/v1/realtime?model=`): ein Modell macht alles; `jarvis_fragen` als Funktion.
 *
 * Die Einstellungen kommen aus [LiveOptionen]. Die Kosten laufen mit ([beiKosten], in USD) und stehen in der Live-Leiste.
 */
class LiveGespraech(
    context: Context,
    private val schluessel: String,
    private val st: LiveOptionen.Stand,
    private val anweisung: String,
    /** Bisheriges Gespräch (true = Frank), damit das Live-Gespräch anknüpft. */
    private val verlauf: List<Pair<Boolean, String>>,
    private val beiStatus: (String) -> Unit,
    private val beiPegel: (Float) -> Unit,
    /** Was das System tatsächlich nutzt: Tonweg, Ausgabegerät und Mikrofon, für die Live-Leiste. */
    private val beiAudioweg: (String) -> Unit,
    private val beiNutzerText: (String) -> Unit,
    private val beiJarvisText: (String) -> Unit,
    /** Jarvis soll etwas nachschlagen oder erledigen; die Antwort kommt über [ergebnis] mit derselben Kennung zurück. */
    private val beiWerkzeug: (kennung: String, auftrag: String) -> Unit,
    private val beiKosten: (Double) -> Unit,
    /** Genau einmal je Gespräch, sobald die Kosten feststehen: für [LiveKostenBuch]. */
    private val beiAbrechnung: (LiveKostenBuch.Abrechnung) -> Unit,
    private val beiFehler: (String) -> Unit,
    private val beiEnde: () -> Unit,
) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    private val live = st.istLive
    private val modell = st.modell
    private var ws: WebSocket? = null
    @Volatile private var laeuft = false
    @Volatile private var bereit = false
    @Volatile var stumm = false
        set(v) { field = v; if (live) ws?.send(JSONObject().put("type", if (v) "session.input_audio.mute" else "session.input_audio.unmute").toString()) }
    @Volatile private var sprichtGerade = false
    @Volatile private var letzteAktivitaet = System.currentTimeMillis()

    private var aufnahme: AudioRecord? = null
    private var echo: AcousticEchoCanceler? = null
    private var rauschen: NoiseSuppressor? = null
    private var wiedergabe: AudioTrack? = null
    private val ausgabe = LinkedBlockingQueue<ByteArray>()
    private var alterModus = AudioManager.MODE_NORMAL
    /** Medienweg (A2DP/Kopfhörer, Handy-Mikrofon, kein Telefonmodus) statt Freisprechen; fest ab Gesprächsbeginn. */
    @Volatile private var medienweg = false
    /** Im Medienweg: Mikrofon zu, solange Jarvis spricht, damit er sich nicht selbst hört. */
    @Volatile private var echoSperre = false
    /** Zeitpunkt des letzten Schreibens in die Wiedergabe: für den Nachlauf der Echosperre (Puffer und Bluetooth-Verzögerung). */
    @Volatile private var letzteAusgabeMs = 0L
    /** Alle in die Wiedergabe geschriebenen Frames seit [kopfBasis], echte Sprache wie Füll-Stille. */
    @Volatile private var geschriebeneFrames = 0L
    /** Frame-Grenze hinter dem letzten echten Ton: Bis der Wiedergabekopf sie passiert, spricht Jarvis noch hörbar. */
    @Volatile private var echtBisFrame = 0L
    /** Kopfposition beim letzten Leeren, damit die Rechnung nach flush() stimmt, ob der Kopf zurückspringt oder nicht. */
    @Volatile private var kopfBasis = 0L
    /** Echosperre: Mikrofon frühestens ab hier wieder offen. */
    @Volatile private var echoFreiAb = 0L
    /** Nach Antippen: Reste der abgebrochenen Antwort nicht mehr abspielen. Realtime über die Kennung, GPT-Live über die Lücke. */
    @Volatile private var verworfenItem = ""
    @Volatile private var verwerfenLive = false
    @Volatile private var letzteVerworfenMs = 0L
    private var routenWaechter: android.media.AudioRouting.OnRoutingChangedListener? = null

    // ---- Kosten ----
    @Volatile private var liveSekunden = 0.0
    @Volatile private var tokenKosten = 0.0
    private val gezaehlt = HashSet<String>()
    /** GPT-Live: `session.closed` hat die endgültige Dauer gemeldet; vorher ist `usage.seconds` nur ein Zwischenstand. */
    @Volatile private var dauerBestaetigt = false
    /** Antworten mit Nutzungsdaten, deren Modell keinen Preis in [LiveOptionen] hat. */
    @Volatile private var ohnePreis = 0
    @Volatile private var startMs = 0L
    @Volatile private var endeMs = 0L
    private val abgerechnet = java.util.concurrent.atomic.AtomicBoolean(false)
    private fun kostenMelden() = beiKosten(liveSekunden / 60.0 * LiveOptionen.LIVE_PRO_MINUTE + tokenKosten)

    /**
     * Verbucht das Gespräch einmal. Berechnet = Nutzungsdaten der API mal Listenpreis. Geschätzt = bei GPT-Live die Zeit,
     * die die App gemessen, OpenAI aber nicht bestätigt hat (Abbruch vor `session.closed`), mit 0,05 $/Min.
     */
    private fun abrechnen() {
        if (startMs == 0L || !abgerechnet.compareAndSet(false, true)) return
        val gemessen = ((if (endeMs > 0) endeMs else System.currentTimeMillis()) - startMs) / 1000.0
        val berechnet = liveSekunden / 60.0 * LiveOptionen.LIVE_PRO_MINUTE + tokenKosten
        val a = if (live) {
            val offen = if (dauerBestaetigt) 0.0 else (gemessen - liveSekunden).coerceAtLeast(0.0)
            LiveKostenBuch.Abrechnung(if (dauerBestaetigt) liveSekunden else maxOf(liveSekunden, gemessen), dauerBestaetigt,
                berechnet, offen / 60.0 * LiveOptionen.LIVE_PRO_MINUTE, ohnePreis)
        } else {
            // Realtime rechnet nur nach Tokens ab; die Dauer misst die App und dient nur der Zeitbasis.
            LiveKostenBuch.Abrechnung(gemessen, false, berechnet, 0.0, ohnePreis)
        }
        beiAbrechnung(a)
    }

    fun start() {
        laeuft = true
        beiStatus("Verbinde …")
        val url = if (live) "wss://api.openai.com/v1/live/sessions" else "wss://api.openai.com/v1/realtime?model=$modell"
        val anfrage = Request.Builder().url(url).header("Authorization", "Bearer $schluessel").build()
        ws = client.newWebSocket(anfrage, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (live) {
                    // GPT-Live: erst session.start, Audio erst nach session.started.
                    webSocket.send(liveStart().toString())
                } else {
                    webSocket.send(realtimeSitzung().toString())
                    val mitschrift = st["mitschrift"]
                    // Getrennt: Lehnt der Dienst die Mitschrift ab, scheitert nur sie, nicht das Gespräch.
                    if (mitschrift != "aus") webSocket.send(JSONObject().put("type", "session.update").put("session", JSONObject().put("type", "realtime").put("audio",
                        JSONObject().put("input", JSONObject().put("transcription", JSONObject().put("model", mitschrift).put("language", "de"))))).toString())
                    realtimeVerlauf(webSocket)
                    bereit = true
                    startMs = System.currentTimeMillis()
                    tonStarten()
                    if (laeuft) beiStatus("Ich höre zu")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val e = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (live) liveEreignis(e) else realtimeEreignis(e)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!laeuft) return
                val grund = when (response?.code) {
                    401 -> "Der OpenAI-Schlüssel wurde abgelehnt."
                    403 -> "Der Schlüssel darf dieses Live-Modell nicht nutzen."
                    404 -> "Das Live-Modell „$modell“ gibt es nicht (mehr)."
                    429 -> "Kein Guthaben oder zu viele Anfragen bei OpenAI."
                    else -> "Live-Verbindung getrennt: " + (t.message ?: "unbekannt")
                }
                beiFehler(grund)
                beenden(senden = false)
            }

            // Schließt der Dienst die Verbindung, meldet OkHttp nur onClosing; ohne das liefe das Mikrofon in eine tote Verbindung.
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { if (laeuft) beenden(senden = false) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { if (laeuft) beenden(senden = false) }
        })
        stilleWaechter()
    }

    // ======================= GPT-Live =======================

    private fun liveStart(): JSONObject {
        val sitzung = JSONObject()
            .put("model", modell)
            .put("instructions", anweisung)
            .put("audio", JSONObject().put("format", JSONObject().put("type", "audio/pcm").put("rate", 24000)).put("output", JSONObject().put("voice", st["stimme"])))
            .put("store", st["speichern"] == "an")
        val eingabe = JSONArray()
        verlauf.forEach { (mir, text) ->
            eingabe.put(JSONObject().put("type", "message").put("role", if (mir) "user" else "assistant")
                .put("content", JSONArray().put(JSONObject().put("type", if (mir) "input_text" else "output_text").put("text", text.take(1200)))))
        }
        eingabe.put(JSONObject().put("type", "message").put("role", "developer")
            .put("content", JSONArray().put(JSONObject().put("type", "input_text").put("text", "Begrüße Frank kurz auf Deutsch, höchstens ein Satz, dann hör zu."))))
        sitzung.put("input", eingabe)
        if (st.denktBeiOpenAi) {
            val r = JSONObject()
                .put("model", st["backend"])
                .put("instructions", "Du bist das Denk-Backend von Jarvis, Franks persönlichem Assistenten. Antworte auf Deutsch, knapp und zum Vorlesen geeignet, ohne Aufzählungen und Links. " +
                    "Für alles, was Franks Daten, Apps oder Aktionen betrifft (Termine, Dienstplan, Aufgaben, Wecker, Timer, Biodaten, Schlaf, Ideen, Tagebuch, Mails, Wetter, Fahrzeit, " +
                    "Gedächtnis, Programmieraufträge, Recherchen) und für jeden Wechsel zwischen Sessions (etwa „Wechsle in die Whoop-Session“, „Geh zurück in die letzte Session“, " +
                    "„Öffne eine neue Session“, auch Franks Antwort auf „Welche Session meinst du?“), rufe die Funktion jarvis_fragen mit dem vollständigen Auftrag auf, " +
                    "Sessionbefehle wörtlich in Franks Worten. Erfinde nie Daten. Melde Erfolg erst nach dem Werkzeugergebnis.")
                .put("reasoning", JSONObject().put("effort", st["backend_effort"]))
                .put("text", JSONObject().put("verbosity", st["ausfuehrlich"]))
                .put("service_tier", st["tarif"])
                .put("tool_choice", "auto")
                .put("tools", JSONArray().put(jarvisWerkzeug()).apply { if (st["websuche"] == "an") put(JSONObject().put("type", "web_search")) })
            st["backend_max"].toIntOrNull()?.takeIf { it > 0 }?.let { r.put("max_output_tokens", it) }
            sitzung.put("delegation", JSONObject().put("type", "responses").put("responses", r))
        } else {
            sitzung.put("delegation", JSONObject().put("type", "client"))
        }
        return JSONObject().put("type", "session.start").put("session", sitzung)
    }

    private val nutzerTeile = StringBuilder()
    private val jarvisTeile = StringBuilder()
    /** Was Frank seit der letzten Client-Delegation gesagt hat: daraus erschließt Jarvis den Auftrag. */
    private val seitAuftrag = StringBuilder()
    // OkHttp-Faden schreibt, ergebnis() und beenden() kommen von anderen Fäden: daher threadsicher bzw. unter Sperre.
    private val kennungArt = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Liest den Puffer und leert ihn in einem Schritt; der Rückruf läuft außerhalb der Sperre. */
    private fun abholen(teile: StringBuilder) = synchronized(teile) { teile.toString().also { teile.setLength(0) } }
    private fun nutzerFertig() { abholen(nutzerTeile).trim().takeIf { it.isNotEmpty() }?.let(beiNutzerText) }
    private fun jarvisFertig() { abholen(jarvisTeile).trim().takeIf { it.isNotEmpty() }?.let(::jarvisSatz) }

    /**
     * Die gesprochene Bestätigung eines Sessionwechsels: Wie im Mikrofonpfad steht sie nur oben in der App, nicht im Gespräch,
     * und landet so nie in der Session, in die gerade gewechselt wurde. Gilt für genau die nächste Jarvis-Mitschrift.
     */
    @Volatile private var bestaetigungVerschlucken = false

    private fun jarvisSatz(text: String) {
        if (bestaetigungVerschlucken) { bestaetigungVerschlucken = false; return }
        beiJarvisText(text)
    }

    private fun liveEreignis(e: JSONObject) {
        when (e.optString("type")) {
            "session.started" -> {
                bereit = true
                startMs = System.currentTimeMillis()
                tonStarten()
                if (laeuft) beiStatus("Ich höre zu")
            }
            "session.output_audio.delta" -> {
                letzteAktivitaet = System.currentTimeMillis()
                if (verwerfenLive) {
                    // Die abgebrochene Antwort läuft noch nach; erst nach einer Pause von 1,5 s kommt wieder Neues.
                    if (letzteAktivitaet - letzteVerworfenMs < 1_500) { letzteVerworfenMs = letzteAktivitaet; return }
                    verwerfenLive = false
                }
                if (!sprichtGerade) { sprichtGerade = true; beiStatus(sprichtText()) }
                ausgabe.offer(Base64.decode(e.optString("delta"), Base64.DEFAULT))
            }
            "session.input_transcript.delta" -> {
                letzteAktivitaet = System.currentTimeMillis()
                // GPT-Live meldet kein „Frank spricht“: Kommt seine Mitschrift, verstummt die gepufferte Ausgabe.
                if (sprichtGerade) { ausgabeLeeren(); beiStatus("Ich höre zu") }
                verwerfenLive = false
                jarvisFertig()
                val d = e.optString("delta")
                synchronized(nutzerTeile) { nutzerTeile.append(d) }
                synchronized(seitAuftrag) { seitAuftrag.append(d) }
            }
            "session.output_transcript.delta" -> {
                nutzerFertig()
                val d = e.optString("delta")
                synchronized(jarvisTeile) { jarvisTeile.append(d) }
            }
            "session.delegation.created" -> {
                val d = e.optJSONObject("delegation") ?: return
                val id = d.optString("id")
                beiStatus("Jarvis schaut nach …")
                // Was Jarvis bis hier gesagt hat („Moment, ich schaue nach“), gehört noch in die Session vor einem möglichen Wechsel.
                jarvisFertig()
                bestaetigungVerschlucken = false
                if (d.optString("target") == "client") {
                    nutzerFertig()
                    val auftrag = abholen(seitAuftrag).trim()
                    kennungArt[id] = "auftrag"
                    beiWerkzeug(id, auftrag)
                }
            }
            "response.event" -> {
                val inner = e.optJSONObject("event") ?: return
                when (inner.optString("type")) {
                    "response.output_item.done" -> {
                        val item = inner.optJSONObject("item") ?: return
                        if (item.optString("type") != "function_call" || item.optString("name") != "jarvis_fragen") return
                        val callId = item.optString("call_id")
                        val auftrag = runCatching { JSONObject(item.optString("arguments")).optString("auftrag") }.getOrNull().orEmpty()
                        kennungArt[callId] = "funktion"
                        jarvisFertig()
                        bestaetigungVerschlucken = false
                        beiStatus("Jarvis schaut nach …")
                        beiWerkzeug(callId, auftrag)
                    }
                    "response.completed", "response.done" -> {
                        val resp = inner.optJSONObject("response") ?: return
                        val rid = resp.optString("id")
                        if (rid.isNotEmpty() && !gezaehlt.add(rid)) return
                        val u = resp.optJSONObject("usage") ?: return
                        val p = LiveOptionen.BACKENDS[st["backend"]] ?: run { ohnePreis++; return }
                        val f = LiveOptionen.TARIF_FAKTOR[st["tarif"]] ?: 1.0
                        val cached = u.optJSONObject("input_tokens_details")?.optInt("cached_tokens") ?: 0
                        val ein = u.optInt("input_tokens") - cached
                        tokenKosten += (ein * p.ein + cached * p.cache + u.optInt("output_tokens") * p.aus) / 1_000_000 * f
                        kostenMelden()
                    }
                }
            }
            "session.usage.updated", "session.closed" -> {
                // Kumulativ: immer den neuesten Wert nehmen, nie addieren.
                e.optJSONObject("usage")?.optDouble("seconds")?.takeIf { !it.isNaN() }?.let {
                    liveSekunden = it; kostenMelden()
                    if (e.optString("type") == "session.closed") dauerBestaetigt = true
                }
                if (e.optString("type") == "session.closed") {
                    nutzerFertig(); jarvisFertig()
                    val grund = e.optString("reason")
                    if (grund == "expired") beiFehler("Das Live-Gespräch hat die Höchstdauer erreicht.")
                    synchronized(this) { schliessenWartet = false }
                    beenden(senden = false)
                }
            }
            "error" -> {
                val f = e.optJSONObject("error")
                val text = listOfNotNull(f?.optString("message")?.takeIf { it.isNotBlank() }, f?.optString("code")?.takeIf { it.isNotBlank() }?.let { "Code $it" },
                    f?.optString("param")?.takeIf { it.isNotBlank() }?.let { "Feld $it" }).joinToString(", ")
                beiFehler("GPT-Live: " + text.ifBlank { "unbekannter Fehler" })
                // Vor session.started ist die Sitzung nie zustande gekommen.
                if (!bereit) beenden(senden = false)
            }
        }
    }

    // ======================= Realtime =======================

    private fun jarvisWerkzeug(): JSONObject = JSONObject()
        .put("type", "function")
        .put("name", "jarvis_fragen")
        .put("description", "Fragt Jarvis mit allen Werkzeugen und Franks Daten: Termine, Dienstplan, Aufgaben, Wecker, Biodaten, Ideen, Tagebuch, Mails, Wetter, " +
            "Gedächtnis, Programmieraufträge, Timer und alles, was etwas nachschlägt oder ändert, auch den Wechsel zwischen Sessions (Gesprächen). " +
            "Gib den Auftrag vollständig in Franks Worten weiter, Sessionbefehle und Antworten auf „Welche Session meinst du?“ wörtlich.")
        .put("parameters", JSONObject().put("type", "object")
            .put("properties", JSONObject().put("auftrag", JSONObject().put("type", "string").put("description", "Die vollständige Frage oder der Auftrag")))
            .put("required", JSONArray().put("auftrag")))

    private fun realtimeSitzung(): JSONObject {
        val format = JSONObject().put("type", "audio/pcm").put("rate", 24000)
        val unterbrechen = st["unterbrechen"] == "an"
        val erkennung = st["erkennung"].let { e ->
            if (e.startsWith("semantic_")) JSONObject().put("type", "semantic_vad").put("eagerness", e.removePrefix("semantic_"))
            else JSONObject().put("type", "server_vad").put("silence_duration_ms", e.removePrefix("server_").toIntOrNull() ?: 500)
        }.put("create_response", true).put("interrupt_response", unterbrechen)
        val eingabe = JSONObject().put("format", format).put("turn_detection", erkennung)
        st["rauschfilter"].takeIf { it != "aus" }?.let { eingabe.put("noise_reduction", JSONObject().put("type", it)) }
        val s = JSONObject()
            .put("type", "realtime")
            .put("model", modell)
            .put("output_modalities", JSONArray().put("audio"))
            .put("instructions", anweisung)
            .put("audio", JSONObject().put("input", eingabe)
                .put("output", JSONObject().put("format", format).put("voice", st["stimme"]).put("speed", st["tempo"].toDoubleOrNull() ?: 1.0)))
            .put("tools", JSONArray().put(jarvisWerkzeug()))
            .put("tool_choice", "auto")
        if (alle(st).any { it.id == "rt_effort" }) s.put("reasoning", JSONObject().put("effort", st["rt_effort"]))
        when (val k = st["kuerzen"]) {
            "auto", "disabled" -> s.put("truncation", k)
            else -> s.put("truncation", JSONObject().put("type", "retention_ratio").put("retention_ratio", k.toDoubleOrNull() ?: 0.8))
        }
        st["rt_max"].toIntOrNull()?.let { s.put("max_output_tokens", it) }
        return JSONObject().put("type", "session.update").put("session", s)
    }

    private fun alle(stand: LiveOptionen.Stand) = LiveOptionen.alle.filter { it.gilt(stand) }

    private fun realtimeVerlauf(w: WebSocket) {
        verlauf.forEach { (mir, text) ->
            w.send(JSONObject().put("type", "conversation.item.create").put("item", JSONObject().put("type", "message").put("role", if (mir) "user" else "assistant")
                .put("content", JSONArray().put(JSONObject().put("type", if (mir) "input_text" else "output_text").put("text", text.take(1200))))).toString())
        }
    }

    /** Die gerade gesprochene Antwort: zum Kürzen beim Dazwischenreden (sonst glaubt das Modell, Frank habe alles gehört). */
    @Volatile private var antwortItem = ""
    @Volatile private var empfangenBytes = 0L
    @Volatile private var abgespieltBytes = 0L

    private fun realtimeEreignis(e: JSONObject) {
        when (e.optString("type")) {
            "response.output_audio.delta", "response.audio.delta" -> {
                letzteAktivitaet = System.currentTimeMillis()
                val item = e.optString("item_id")
                if (item.isNotEmpty() && item == verworfenItem) return
                if (item != antwortItem) { antwortItem = item; empfangenBytes = 0; abgespieltBytes = 0 }
                val daten = Base64.decode(e.optString("delta"), Base64.DEFAULT)
                empfangenBytes += daten.size
                if (!sprichtGerade) { sprichtGerade = true; beiStatus(sprichtText()) }
                ausgabe.offer(daten)
            }
            "input_audio_buffer.speech_started" -> {
                letzteAktivitaet = System.currentTimeMillis()
                if (sprichtGerade && antwortItem.isNotEmpty() && st["unterbrechen"] == "an") {
                    val ms = (minOf(abgespieltBytes, empfangenBytes) / 48).toInt()
                    ws?.send(JSONObject().put("type", "conversation.item.truncate").put("item_id", antwortItem).put("content_index", 0).put("audio_end_ms", ms).toString())
                }
                if (st["unterbrechen"] == "an") { ausgabeLeeren(); beiStatus("Ich höre zu") }
            }
            "conversation.item.input_audio_transcription.completed" -> {
                e.optString("transcript").trim().takeIf { it.isNotEmpty() }?.let(beiNutzerText)
                // Mitschrift wird nach Dauer abgerechnet, sofern der Dienst sie meldet.
                e.optJSONObject("usage")?.optDouble("seconds")?.takeIf { !it.isNaN() }?.let {
                    tokenKosten += it / 60.0 * (LiveOptionen.MITSCHRIFT_PRO_MINUTE[st["mitschrift"]] ?: 0.0); kostenMelden()
                }
            }
            "response.output_audio_transcript.delta", "response.audio_transcript.delta" -> {
                val d = e.optString("delta")
                synchronized(jarvisTeile) { jarvisTeile.append(d) }
            }
            "response.output_audio_transcript.done", "response.audio_transcript.done" -> {
                val gesammelt = abholen(jarvisTeile)
                val satz = e.optString("transcript").ifBlank { gesammelt }.trim()
                if (satz.isNotEmpty()) jarvisSatz(satz)
            }
            "response.done" -> {
                sprichtGerade = false
                val resp = e.optJSONObject("response")
                resp?.optJSONObject("usage")?.let { realtimeKosten(it) }
                val out = resp?.optJSONArray("output")
                val ruftWerkzeug = out != null && (0 until out.length()).any { out.optJSONObject(it)?.optString("type") == "function_call" }
                if (laeuft && !ruftWerkzeug) beiStatus("Ich höre zu")
            }
            "response.function_call_arguments.done" -> {
                val name = e.optString("name")
                if (name.isNotEmpty() && name != "jarvis_fragen") return
                val auftrag = runCatching { JSONObject(e.optString("arguments")).optString("auftrag") }.getOrNull().orEmpty()
                val callId = e.optString("call_id")
                kennungArt[callId] = "realtime"
                bestaetigungVerschlucken = false
                beiStatus("Jarvis schaut nach …")
                beiWerkzeug(callId, auftrag)
            }
            "error" -> {
                val f = e.optJSONObject("error")
                val code = f?.optString("code").orEmpty()
                if (code == "response_cancel_not_active" || code == "conversation_already_has_active_response") return
                val meldung = f?.optString("message").orEmpty()
                if (meldung.contains("transcription", ignoreCase = true)) return
                beiFehler("Live: " + meldung.ifBlank { "unbekannter Fehler" } + (f?.optString("param")?.takeIf { it.isNotBlank() }?.let { " (Feld $it)" } ?: ""))
            }
        }
    }

    private fun realtimeKosten(u: JSONObject) {
        val r = LiveOptionen.REALTIME[modell] ?: run { ohnePreis++; return }
        val ein = u.optJSONObject("input_token_details")
        val aus = u.optJSONObject("output_token_details")
        val cache = ein?.optJSONObject("cached_tokens_details")
        val audioCache = cache?.optInt("audio_tokens") ?: 0
        val textCache = cache?.optInt("text_tokens") ?: 0
        val audioEin = (ein?.optInt("audio_tokens") ?: 0) - audioCache
        val textEin = (ein?.optInt("text_tokens") ?: 0) - textCache + (ein?.optInt("image_tokens") ?: 0)
        tokenKosten += (audioEin.coerceAtLeast(0) * r.audio.ein + audioCache * r.audio.cache + textEin.coerceAtLeast(0) * r.text.ein + textCache * r.text.cache +
            (aus?.optInt("audio_tokens") ?: 0) * r.audio.aus + (aus?.optInt("text_tokens") ?: 0) * r.text.aus) / 1_000_000
        kostenMelden()
    }

    // ======================= Gemeinsam =======================

    /**
     * Jarvis' Antwort auf einen Auftrag zurück: als Funktionsergebnis oder (Client-Delegation) als Kommentar zum Vorsprechen.
     * [navigation] = Bestätigung eines Sessionwechsels: wird gesprochen, aber nicht ins Gespräch geschrieben.
     */
    fun ergebnis(kennung: String, text: String, navigation: Boolean = false) {
        val w = ws ?: return
        if (navigation) bestaetigungVerschlucken = true
        when (kennungArt.remove(kennung)) {
            "auftrag" -> {
                // Höchstens 500 Tokens je Kommentar.
                w.send(JSONObject().put("type", "session.commentary.append").put("delegation_id", kennung).put("content", text.take(1500)).toString())
            }
            "funktion" -> {
                w.send(JSONObject().put("type", "response.item.create").put("item", JSONObject().put("type", "function_call_output").put("call_id", kennung).put("output", text)).toString())
                w.send(JSONObject().put("type", "response.create").toString())
            }
            else -> {
                w.send(JSONObject().put("type", "conversation.item.create").put("item", JSONObject()
                    .put("type", "function_call_output").put("call_id", kennung).put("output", text)).toString())
                w.send(JSONObject().put("type", "response.create").toString())
            }
        }
    }

    /** Beendet das Gespräch nach der eingestellten Stille; GPT-Live kostet sonst im Leerlauf weiter. */
    private fun stilleWaechter() {
        val minuten = st["stille_ende"].toIntOrNull() ?: 0
        Thread({
            while (laeuft) {
                runCatching { Thread.sleep(5_000) }
                if (!laeuft) break
                if (minuten > 0 && System.currentTimeMillis() - letzteAktivitaet > minuten * 60_000L) {
                    beiFehler("Live-Gespräch nach $minuten Minuten Stille beendet.")
                    beenden()
                    break
                }
            }
        }, "jarvis-live-stille").start()
    }

    /**
     * Schützt Anlegen und Freigeben von Mikrofon, Wiedergabe und Audiomodus: tonStarten() läuft im OkHttp-Faden,
     * beenden()/tonAus() in beliebigen anderen. Unter der Sperre wird nie auf einen anderen Faden gewartet.
     */
    private val tonSperre = Any()

    @SuppressLint("MissingPermission")
    private fun tonStarten() {
        val mikroOffen = synchronized(tonSperre) {
            if (aufnahme != null || !laeuft) return
            // Medienweg nur mit Bluetooth-Medien oder Kopfhörer: Am Handy-Lautsprecher bringt er keinen besseren Klang,
            // nimmt aber die Echounterdrückung des Telefonmodus weg. Dann bleibt es beim bisherigen Weg.
            val ausgaenge = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            medienweg = st["audioweg"] == "medien" && LiveRouting.medienAusgang(ausgaenge.map { it.type })
            echoSperre = medienweg && st["echo_sperre"] == "an"
            Log.i(LiveRouting.TAG, "Gesprächsbeginn: Tonweg " + (if (medienweg) "Medien (kein Telefonmodus), Echosperre=$echoSperre" else "Freisprechen (Telefonmodus)") +
                ", Wunsch ${st["audioweg"]}, Ausgänge " + ausgaenge.joinToString { "${LiveRouting.name(it.type)} „${it.productName}“" })
            if (!medienweg) {
                alterModus = audio.mode
                audio.mode = AudioManager.MODE_IN_COMMUNICATION
                routingStarten()
            }
            // Das Autoradio schickt nach dem Freisprechen ein Play; es soll hier landen, nicht bei Poweramp.
            MedienTastenRiegel.sperren(app)

            val rate = 24000
            val minIn = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            // VOICE_COMMUNICATION auch im Medienweg: Nur diese Quelle bekommt die Echounterdrückung des Geräts.
            val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minIn, 9600) * 2)
            aufnahme = rec
            if (medienweg) {
                // Fest das Handy-Mikrofon: Das Bluetooth-Mikrofon ginge nur über das Telefonprofil und risse den Medienton mit.
                audio.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                    ?.let { m -> runCatching { rec.setPreferredDevice(m) } }
            }
            if (rec.state != AudioRecord.STATE_INITIALIZED) false
            else {
                tonAnlegen(rec, rate)
                true
            }
        }
        // Meldung und Beenden außerhalb der Sperre: beenden() ruft tonAus() und damit selbst die Sperre.
        if (!mikroOffen) {
            if (laeuft) beiFehler("Das Mikrofon ließ sich für das Live-Gespräch nicht öffnen.")
            beenden()
        }
    }

    /** Nur unter [tonSperre] aufrufen. */
    private fun tonAnlegen(rec: AudioRecord, rate: Int) {
        if (AcousticEchoCanceler.isAvailable()) echo = AcousticEchoCanceler.create(rec.audioSessionId)?.also { it.enabled = true }
        if (NoiseSuppressor.isAvailable()) rauschen = NoiseSuppressor.create(rec.audioSessionId)?.also { it.enabled = true }

        val minOut = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            // Medienweg: USAGE_MEDIA folgt sicher dem Medienton (A2DP); Freisprechen: Gesprächston über das Kommunikationsgerät.
            .setAudioAttributes(AudioAttributes.Builder().setUsage(if (medienweg) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(minOut * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        wiedergabe = track
        track.play()

        // Kam beenden() während des Anlegens, sofort alles wieder freigeben: sonst blieben Mikrofon und Audiomodus belegt.
        if (!laeuft) { tonAus(); return }

        // Zeigt, was das System tatsächlich nutzt, auch nach Verbinden oder Trennen im Gespräch.
        val waechter = android.media.AudioRouting.OnRoutingChangedListener { audiowegMelden() }
        runCatching { track.addOnRoutingChangedListener(waechter, hauptFaden); rec.addOnRoutingChangedListener(waechter, hauptFaden) }
        routenWaechter = waechter
        audiowegMelden()

        // 100-ms-Blöcke (4.800 Byte), wie OpenAI empfiehlt; Fehler in den Fäden enden als Meldung, nie als Absturz.
        Thread({
            val puffer = ByteArray(4800)
            // Unter der Sperre: Hat tonAus() die Aufnahme schon freigegeben, startet sie nicht mehr.
            val ok = synchronized(tonSperre) {
                laeuft && aufnahme === rec && runCatching { rec.startRecording() }.isSuccess && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING
            }
            if (!ok) { if (laeuft) { beiFehler("Das Mikrofon ließ sich für das Live-Gespräch nicht starten."); beenden() }; return@Thread }
            // Das Mikrofon kennt sein Gerät erst nach dem Start.
            hauptFaden.post { audiowegMelden() }
            val anhaengen = if (live) "session.input_audio.append" else "input_audio_buffer.append"
            while (laeuft) {
                var n = 0
                while (n < puffer.size && laeuft) {
                    val r = runCatching { rec.read(puffer, n, puffer.size - n) }.getOrDefault(AudioRecord.ERROR_INVALID_OPERATION)
                    if (r < 0) { if (laeuft) { beiFehler("Das Mikrofon ist ausgefallen."); beenden() }; return@Thread }
                    n += r
                }
                if (n == 0) continue
                n -= n % 2
                var summe = 0.0
                var i = 0
                while (i + 1 < n) { val s = (puffer[i].toInt() and 0xFF) or (puffer[i + 1].toInt() shl 8); summe += s.toDouble() * s; i += 2 }
                beiPegel((kotlin.math.sqrt(summe / (n / 2)) / 6000.0).toFloat().coerceIn(0f, 1f))
                // GPT-Live hat eigenes Stummschalten (session.input_audio.mute); Realtime bekommt Stille.
                // Echosperre: Solange Jarvis spricht (samt Nachlauf), bekommt der Dienst Stille statt des eigenen Echos.
                // Zu, solange echter Ton wartet oder der Wiedergabekopf ihn noch nicht abgespielt hat; danach der Nachlauf für die
                // Bluetooth-Strecke hinter dem Kopf. Die Füll-Stille zählt nicht, nur [echtBisFrame].
                val jetzt = System.currentTimeMillis()
                val echoZu = echoSperre && run {
                    val kopf = (runCatching { wiedergabe?.playbackHeadPosition ?: 0 }.getOrDefault(0).toLong() and 0xFFFFFFFFL) - kopfBasis
                    if (ausgabe.isNotEmpty() || kopf < echtBisFrame) echoFreiAb = jetzt + ECHO_NACHLAUF_MS
                    jetzt < echoFreiAb
                }
                val stueck = if ((stumm && !live) || echoZu) ByteArray(n) else puffer.copyOf(n)
                ws?.send(JSONObject().put("type", anhaengen).put("audio", Base64.encodeToString(stueck, Base64.NO_WRAP)).toString())
            }
        }, "jarvis-live-mikro").start()

        // Medienweg: In Sprechpausen Stille nachschieben. Läuft die Wiedergabe leer, geht der Ausgang nach wenigen Sekunden in
        // Bereitschaft, der Bluetooth-Medienstrom (A2DP) wird ausgesetzt und viele Lautsprecher verschlucken beim Wiederanlauf
        // den Satzanfang. Die Stille zählt weder als Ausgabe (Echosperre) noch als abgespielt (Kürzen beim Dazwischenreden).
        val stille = ByteArray(rate / 1000 * 2 * STILLE_MS)
        Thread({
            while (laeuft) {
                val stueck = runCatching { ausgabe.poll(if (medienweg) STILLE_MS.toLong() else 200L, TimeUnit.MILLISECONDS) }.getOrNull()
                // GPT-Live meldet kein Ende der Rede: Ist die Ausgabe leer gelaufen, hört Jarvis wieder zu.
                if (stueck == null) {
                    // Wie bisher erst nach 200 ms ohne Ton, auch wenn der Medienweg kürzer wartet.
                    if (live && sprichtGerade && ausgabe.isEmpty() && System.currentTimeMillis() - letzteAusgabeMs >= 200) { sprichtGerade = false; if (laeuft) beiStatus("Ich höre zu") }
                    if (medienweg && laeuft) runCatching { wiedergabe?.write(stille, 0, stille.size) }.getOrNull()?.takeIf { it > 0 }?.let { geschriebeneFrames += it / 2 }
                    continue
                }
                letzteAusgabeMs = System.currentTimeMillis()
                val geschrieben = runCatching { wiedergabe?.write(stueck, 0, stueck.size) }.getOrNull() ?: 0
                if (geschrieben > 0) { geschriebeneFrames += geschrieben / 2; echtBisFrame = geschriebeneFrames }
                letzteAusgabeMs = System.currentTimeMillis()
                abgespieltBytes += stueck.size
            }
        }, "jarvis-live-ton").start()
    }

    private fun ausgabeLeeren() {
        ausgabe.clear()
        sprichtGerade = false
        wiedergabe?.let { runCatching { it.pause(); it.flush(); it.play() } }
        // Nach dem Leeren ist nichts Echtes mehr unterwegs: Die Echosperre öffnet sofort (nach ihrem kurzen Nachlauf).
        geschriebeneFrames = 0
        echtBisFrame = 0
        kopfBasis = runCatching { wiedergabe?.playbackHeadPosition ?: 0 }.getOrDefault(0).toLong() and 0xFFFFFFFFL
    }

    /** Im Medienweg mit Echosperre hört das Mikrofon nicht, solange Jarvis spricht: Dann den Tipp-Weg nennen. */
    private fun sprichtText() = if (echoSperre) "Jarvis spricht · Blase antippen zum Unterbrechen" else "Jarvis spricht"

    /**
     * Unterbricht Jarvis per Antippen: nötig im Medienweg mit Echosperre, wo Dazwischenreden nicht geht, und überall sonst
     * ein sicherer Weg. Realtime bricht die Antwort beim Dienst ab und kürzt sie auf das Gehörte; GPT-Live verwirft den Rest.
     */
    fun unterbrechen() {
        if (!laeuft || (!sprichtGerade && ausgabe.isEmpty())) return
        if (live) {
            verwerfenLive = true
            letzteVerworfenMs = System.currentTimeMillis()
        } else if (antwortItem.isNotEmpty()) {
            verworfenItem = antwortItem
            val ms = (minOf(abgespieltBytes, empfangenBytes) / 48).toInt()
            ws?.send(JSONObject().put("type", "response.cancel").toString())
            ws?.send(JSONObject().put("type", "conversation.item.truncate").put("item_id", antwortItem).put("content_index", 0).put("audio_end_ms", ms).toString())
        }
        ausgabeLeeren()
        Log.i(LiveRouting.TAG, "Jarvis per Antippen unterbrochen")
        beiStatus("Ich höre zu")
    }

    /** Meldet Tonweg, Ausgabe und Mikrofon, wie das System sie gerade tatsächlich geroutet hat. */
    private fun audiowegMelden() {
        val track = wiedergabe ?: return
        val raus = runCatching { track.routedDevice }.getOrNull()
        val rein = runCatching { aufnahme?.routedDevice }.getOrNull()
        fun geraet(d: AudioDeviceInfo?) = d?.let { LiveRouting.name(it.type) + (it.productName?.toString()?.takeIf { n -> n.isNotBlank() && !n.equals(android.os.Build.MODEL, true) }?.let { n -> " „$n“" } ?: "") } ?: "Systemwahl"
        val weg = if (medienweg) "Medienton" + (if (echoSperre) " · Echosperre" else "") else "Freisprechen (Telefonklang)"
        val text = "$weg · Ton: ${geraet(raus)} · Mikro: ${geraet(rein)}"
        Log.i(LiveRouting.TAG, "Tatsächlich: $text")
        if (laeuft) beiAudioweg(text)
    }

    // ---- Ausgabegerät: Auto/Headset vor Handy-Lautsprecher, auch beim Verbinden und Trennen im Gespräch ----
    private val hauptFaden = Handler(Looper.getMainLooper())
    private var geraeteWaechter: AudioDeviceCallback? = null
    private var kommWaechter: Any? = null
    @Volatile private var routingAktiv = false
    private var scoGestartet = false
    private val nachpruefen = Runnable { synchronized(tonSperre) { if (routingAktiv) routen("Nachprüfung") } }

    /** Nur unter [tonSperre]: meldet die Wächter an und wählt das Gerät. */
    private fun routingStarten() {
        routingAktiv = true
        routen("Gesprächsbeginn")
        val waechter = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(neu: Array<out AudioDeviceInfo>) = geaendert("verbunden", neu)
            override fun onAudioDevicesRemoved(weg: Array<out AudioDeviceInfo>) = geaendert("getrennt", weg)
        }
        runCatching { audio.registerAudioDeviceCallback(waechter, hauptFaden) }.onSuccess { geraeteWaechter = waechter }
        if (Build.VERSION.SDK_INT >= 31) {
            val l = AudioManager.OnCommunicationDeviceChangedListener { d ->
                Log.i(LiveRouting.TAG, "System meldet Kommunikationsgerät: ${d?.let { "${LiveRouting.name(it.type)} „${it.productName}“" } ?: "keins"}")
            }
            runCatching { audio.addOnCommunicationDeviceChangedListener(app.mainExecutor, l) }.onSuccess { kommWaechter = l }
        }
    }

    private fun geaendert(was: String, geraete: Array<out AudioDeviceInfo>) {
        Log.i(LiveRouting.TAG, "Gerät $was: " + geraete.joinToString { "${LiveRouting.name(it.type)} „${it.productName}“" })
        synchronized(tonSperre) { if (routingAktiv) routen("Gerät $was") }
        // Bluetooth-Freisprechen taucht teils erst kurz nach der Gerätemeldung als Kommunikationsgerät auf.
        hauptFaden.removeCallbacks(nachpruefen)
        hauptFaden.postDelayed(nachpruefen, 1500)
    }

    /** Nur unter [tonSperre]. Wählt Auto/Headset, sonst den Lautsprecher; nie die Hörmuschel. */
    private fun routen(anlass: String) {
        runCatching {
            val ausgaenge = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            if (Build.VERSION.SDK_INT >= 31) {
                val geraete = audio.availableCommunicationDevices
                val i = LiveRouting.wahl(geraete.map { it.type })
                Log.i(LiveRouting.TAG, "$anlass: verfügbar " + geraete.joinToString { "${LiveRouting.name(it.type)} „${it.productName}“" })
                if (geraete.none { LiveRouting.istBluetooth(it.type) } && ausgaenge.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP })
                    Log.w(LiveRouting.TAG, "Nur Bluetooth-Medien (A2DP) verbunden, kein Freisprechprofil: Live-Ton bleibt am Handy. In den Bluetooth-Einstellungen „Anrufe“ für das Gerät einschalten.")
                if (i < 0) { audio.clearCommunicationDevice(); Log.w(LiveRouting.TAG, "$anlass: kein passendes Gerät, Systemwahl"); return@runCatching }
                val ziel = geraete[i]
                if (audio.communicationDevice?.id == ziel.id) { Log.i(LiveRouting.TAG, "$anlass: bleibt bei ${LiveRouting.name(ziel.type)} „${ziel.productName}“"); return@runCatching }
                val ok = audio.setCommunicationDevice(ziel)
                Log.i(LiveRouting.TAG, "$anlass: gewählt ${LiveRouting.name(ziel.type)} „${ziel.productName}“, angenommen=$ok")
            } else {
                @Suppress("DEPRECATION")
                val sco = ausgaenge.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                val kabel = ausgaenge.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
                @Suppress("DEPRECATION")
                if (sco) {
                    if (!scoGestartet) { audio.startBluetoothSco(); scoGestartet = true }
                    audio.isBluetoothScoOn = true
                    audio.isSpeakerphoneOn = false
                } else {
                    if (scoGestartet) { audio.isBluetoothScoOn = false; audio.stopBluetoothSco(); scoGestartet = false }
                    audio.isSpeakerphoneOn = !kabel
                }
                Log.i(LiveRouting.TAG, "$anlass: sco=$sco kabel=$kabel (vor Android 12)")
            }
        }.onFailure { Log.w(LiveRouting.TAG, "$anlass: Routing fehlgeschlagen", it) }
    }

    /** Nur unter [tonSperre]: Wächter ab, Gerätewahl zurück an das System. */
    private fun routingBeenden() {
        routingAktiv = false
        hauptFaden.removeCallbacks(nachpruefen)
        geraeteWaechter?.let { runCatching { audio.unregisterAudioDeviceCallback(it) } }
        geraeteWaechter = null
        if (Build.VERSION.SDK_INT >= 31) {
            (kommWaechter as? AudioManager.OnCommunicationDeviceChangedListener)?.let { runCatching { audio.removeOnCommunicationDeviceChangedListener(it) } }
            kommWaechter = null
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) audio.clearCommunicationDevice()
            else {
                @Suppress("DEPRECATION")
                if (scoGestartet) { audio.isBluetoothScoOn = false; audio.stopBluetoothSco(); scoGestartet = false }
                @Suppress("DEPRECATION")
                audio.isSpeakerphoneOn = false
            }
        }
        Log.i(LiveRouting.TAG, "Gesprächsende: Gerätewahl freigegeben")
    }

    @Volatile private var schliessenWartet = false

    /**
     * Beendet das Gespräch. Bei GPT-Live erst `session.close` senden und kurz auf `session.closed` warten: Erst dann steht die
     * abgerechnete Dauer fest. Ton und Mikrofon enden sofort.
     */
    fun beenden(senden: Boolean = true) {
        val w: WebSocket?
        synchronized(this) {
            if (!laeuft) return
            laeuft = false
            endeMs = System.currentTimeMillis()
            w = ws
            ws = null
        }
        tonAus()
        nutzerFertig(); jarvisFertig()
        if (senden && live && w != null && bereit) {
            schliessenWartet = true
            runCatching { w.send(JSONObject().put("type", "session.close").toString()) }
            Thread({
                val bis = System.currentTimeMillis() + 5_000
                while (schliessenWartet && System.currentTimeMillis() < bis) runCatching { Thread.sleep(100) }
                abrechnen()
                runCatching { w.close(1000, "Ende") }
                client.dispatcher.executorService.shutdown()
            }, "jarvis-live-ende").start()
            // Den Endwert noch annehmen, solange der Socket offen ist.
            ws = w
        } else {
            runCatching { w?.close(1000, "Ende") }
            client.dispatcher.executorService.shutdown()
            abrechnen()
        }
        beiEnde()
    }

    private fun tonAus() { synchronized(tonSperre) {
        routenWaechter?.let { w -> runCatching { wiedergabe?.removeOnRoutingChangedListener(w) }; runCatching { aufnahme?.removeOnRoutingChangedListener(w) } }
        routenWaechter = null
        runCatching { aufnahme?.stop() }
        runCatching { aufnahme?.release() }
        aufnahme = null
        runCatching { echo?.release(); rauschen?.release() }
        echo = null
        rauschen = null
        ausgabe.clear()
        runCatching { wiedergabe?.stop() }
        runCatching { wiedergabe?.release() }
        wiedergabe = null
        if (routingAktiv || geraeteWaechter != null) routingBeenden()
        // Den Modus nur zurückstellen, wenn das Gespräch ihn gesetzt hat; der Medienweg fasst ihn nie an (etwa während eines Anrufs).
        if (!medienweg) runCatching { audio.mode = alterModus }
        MedienTastenRiegel.freigebenSpaeter()
    } }

    companion object {
        /** Echosperre bleibt so lange nach dem letzten Ton zu: Wiedergabepuffer plus Bluetooth-Verzögerung. */
        private const val ECHO_NACHLAUF_MS = 400L
        /** Länge eines Stilleblocks in Sprechpausen (Medienweg); hält den Bluetooth-Medienstrom wach. */
        private const val STILLE_MS = 20

        /** Die Stimmen der Realtime-API; Marin und Cedar klingen am natürlichsten. */
        val STIMMEN = listOf("marin" to "Marin", "cedar" to "Cedar", "alloy" to "Alloy", "ash" to "Ash", "ballad" to "Ballad",
            "coral" to "Coral", "echo" to "Echo", "sage" to "Sage", "shimmer" to "Shimmer", "verse" to "Verse")
    }
}
