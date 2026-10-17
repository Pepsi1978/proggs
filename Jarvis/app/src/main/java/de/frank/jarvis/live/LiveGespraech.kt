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
 * Live-Gespräch mit OpenAI oder Google, PCM16 mit 24 kHz (Gemini: Mikrofon auf 16 kHz umgerechnet), über drei Protokolle:
 *
 * - **GPT-Live** (`gpt-live-1`, `wss://…/v1/live/sessions`): hört und spricht gleichzeitig. Denken übernimmt entweder ein
 *   Denk-Modell bei OpenAI (Responses-Delegation, das per Funktion `jarvis_fragen` den Jarvis-Agenten ruft) oder Jarvis
 *   selbst (Client-Delegation: Auftrag aus der Mitschrift, Antwort per `session.commentary.append`).
 * - **Realtime** (`gpt-realtime-*`, `wss://…/v1/realtime?model=`): ein Modell macht alles; `jarvis_fragen` als Funktion.
 * - **Gemini Live** (`gemini-*`, `BidiGenerateContent`): wie Realtime ein Modell mit `jarvis_fragen` als Funktion; Verbindungen
 *   enden nach etwa 10 Minuten (goAway) und werden mit der Fortsetzungskennung nahtlos erneuert.
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
    /** Nur Gemini: bezahlter Schlüssel, auf den Jarvis wechselt, wenn der kostenlose an sein Kontingent stößt (leer = keiner). */
    private val ersatzSchluessel: String = "",
    /** Nur Gemini: Der erste Schlüssel gehört zu einem Projekt ohne Abrechnung; seine Gespräche kosten nichts. */
    schluesselGratis: Boolean = false,
) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    private val live = st.istLive
    private val gemini = st.istGemini
    private val modell = st.modell
    private var ws: WebSocket? = null
    /** Gemini: Schlüssel der laufenden Verbindung und der bezahlte Ersatz, falls das kostenlose Kontingent ausgeht. */
    @Volatile private var aktuellerSchluessel = schluessel
    /** Gemini: Kennung zum Fortsetzen derselben Sitzung auf einer neuen Verbindung (goAway, Schlüsselwechsel). */
    @Volatile private var fortsetzKennung = ""
    @Volatile private var begruesst = false
    @Volatile private var gratis = schluesselGratis
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
    /**
     * Ein Stück Ausgabe; [hoerbar] = echter Ton statt Stille. Nur hörbarer Ton hält Sprechanzeige und Echosperre.
     * [ganzStill] = digitale Stille ohne jeden Ansatz (Spitze unter [STILLE_SPITZE]): darf in Pausen entfallen.
     */
    private class Stueck(val daten: ByteArray, val hoerbar: Boolean, val ganzStill: Boolean = false)
    private val ausgabe = LinkedBlockingQueue<Stueck>()
    private var alterModus = AudioManager.MODE_NORMAL
    /** Medienweg (A2DP/Kopfhörer, Handy-Mikrofon, kein Telefonmodus) statt Freisprechen; fest ab Gesprächsbeginn. */
    @Volatile private var medienweg = false
    /** Im Medienweg: Mikrofon zu, solange Jarvis spricht, damit er sich nicht selbst hört. */
    @Volatile private var echoSperre = false
    /** Zeitpunkt des letzten Schreibens in die Wiedergabe: für den Nachlauf der Echosperre (Puffer und Bluetooth-Verzögerung). */
    @Volatile private var letzteAusgabeMs = 0L
    /** Alle in die Wiedergabe geschriebenen Frames seit [kopfBasis], echte Sprache wie Füll-Stille. */
    private val geschriebeneFrames = java.util.concurrent.atomic.AtomicLong(0)
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
    @Volatile private var verwerfenSeitMs = 0L
    /** Letztes Schreiben von hörbarem Ton in die Wiedergabe (Ton-Faden): Deckel für Echosperre und Sprechanzeige. */
    @Volatile private var letzteHoerbarMs = 0L
    /** Letzter hörbarer Ton vom Dienst (OkHttp-Faden). */
    @Volatile private var letzteHoerbarEmpfangMs = 0L
    @Volatile private var letzterStatus = ""
    // Protokoll der GPT-Live-Ausgabe, alle 2 s: zeigt im Logcat, ob Ton kommt und ob er hörbar ist.
    private var statSeit = 0L
    private var statDeltas = 0
    private var statHoerbar = 0
    private var statBytes = 0L
    private val gemeldeteTypen = HashSet<String>()
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
        synchronized(this) {
            if (laeuft) return
            laeuft = true
            offene.incrementAndGet()
        }
        status("Verbinde …")
        if (gemini) { geminiVerbinden(schluessel); stilleWaechter(); return }
        val url = if (live) "wss://api.openai.com/v1/live/sessions" else "wss://api.openai.com/v1/realtime?model=$modell"
        val anfrage = Request.Builder().url(url).header("Authorization", "Bearer $schluessel").build()
        ws = client.newWebSocket(anfrage, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Derselbe Faden liest danach alle Ereignisse: Vorrang vor Hintergrundarbeit, damit der Ton pünktlich ankommt.
                runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO) }
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
                    if (laeuft) status("Ich höre zu")
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

    // ======================= Gemini Live =======================

    /**
     * Öffnet eine Verbindung zur Gemini Live API. Bei goAway oder einem Schlüsselwechsel kommt eine neue Verbindung mit der
     * Fortsetzungskennung; Ereignisse und das Schließen einer abgelösten Verbindung zählen dann nicht mehr ([ws] ist neu).
     */
    private fun geminiVerbinden(schluessel: String) {
        aktuellerSchluessel = schluessel
        bereit = false
        // Der Schlüssel gehört laut Google in die Adresse; ein Kopfzeilen-Schlüssel ist für die Live-Verbindung nicht belegt.
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.$apiVersion.GenerativeService.BidiGenerateContent?key=" +
            java.net.URLEncoder.encode(schluessel, "UTF-8")
        val anfrage = Request.Builder().url(url).build()
        // Die abgelöste Verbindung liefert den Rest der laufenden Antwort noch; geschlossen wird sie erst bei setupComplete der neuen.
        ws?.let { a -> abgeloest?.takeIf { it !== a }?.let { runCatching { it.close(1000, "Neue Verbindung") } }; abgeloest = a }
        ws = client.newWebSocket(anfrage, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO) }
                webSocket.send(geminiSetup().toString())
            }

            // Gemini schickt sein JSON meist als Binär-Frame.
            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) = onMessage(webSocket, bytes.utf8())

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (webSocket !== ws && webSocket !== abgeloest) return
                val e = runCatching { JSONObject(text) }.getOrNull() ?: return
                // Von der abgelösten Verbindung nur noch Ton, Mitschrift und Werkzeugaufrufe, kein neues Setup.
                if (webSocket !== ws && (e.has("setupComplete") || e.has("goAway") || e.has("sessionResumptionUpdate"))) return
                geminiEreignis(webSocket, e)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!laeuft || webSocket !== ws) return
                val code = response?.code
                val anlass = "HTTP ${code ?: "-"} ${t.message ?: ""}".trim()
                // Modell- und Einstellungsfehler (400, 404) träfen den bezahlten Schlüssel genauso.
                when (code) {
                    400, 404 -> {}
                    401, 403, 429 -> if (geminiErsatz(anlass)) return
                    // Netz weg oder Serverfehler: erst derselbe Schlüssel noch einmal, dann der bezahlte.
                    else -> if (geminiStoerung(anlass)) return
                }
                val grund = when (code) {
                    400 -> "Gemini hat den Verbindungsaufbau abgelehnt (Modell oder Einstellung)."
                    401, 403 -> "Der Gemini-Schlüssel wurde abgelehnt."
                    404 -> "Das Gemini-Modell „$modell“ gibt es nicht (mehr)."
                    429 -> "Gemini-Kontingent erschöpft oder zu viele Anfragen."
                    else -> "Gemini-Verbindung getrennt: " + (t.message ?: "unbekannt")
                }
                beiFehler(grund)
                beenden(senden = false)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (!laeuft || webSocket !== ws) return
                Log.w(LiveRouting.TAG, "Gemini schließt: $code $reason")
                if (code != 1000) {
                    if ((GeminiFehler.kontingent(reason) || GeminiFehler.schluessel(reason)) && geminiErsatz("$code $reason")) return
                    if (GeminiFehler.modell(code, reason)) {
                        // Extended Thinking zeigt Google teils über v1alpha: lehnt v1beta das Modell ab, einmal dort versuchen.
                        if (!bereit && apiVersion == "v1beta" && modell.contains("extended")) {
                            Log.w(LiveRouting.TAG, "Gemini: v1beta lehnt ab ($code $reason), versuche v1alpha")
                            apiVersion = "v1alpha"
                            geminiVerbinden(aktuellerSchluessel)
                            return
                        }
                    } else if (geminiStoerung("$code $reason")) return
                    beiFehler(geminiGrund(code, reason))
                }
                beenden(senden = false)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { if (laeuft && webSocket === ws && !wartetAufNeu) beenden(senden = false) }
        })
    }

    @Volatile private var apiVersion = "v1beta"
    /** Gemini: die Verbindung vor einem goAway- oder Schlüsselwechsel, bis die neue steht. */
    @Volatile private var abgeloest: WebSocket? = null
    /** Gemini: über welche Verbindung ein Werkzeugaufruf kam; das Ergebnis geht dorthin zurück, solange sie offen ist. */
    private val aufrufVerbindung = java.util.concurrent.ConcurrentHashMap<String, WebSocket>()
    /** Gemini: eine Antwort läuft (erster Ton bis turnComplete); nur dann verwirft das Antippen ihren Rest. */
    @Volatile private var geminiAntwortLaeuft = false

    private fun geminiGrund(code: Int, reason: String): String = when {
        GeminiFehler.kontingent(reason) -> "Gemini-Kontingent erschöpft: " + reason.take(160)
        GeminiFehler.schluessel(reason) -> "Der Gemini-Schlüssel wurde abgelehnt."
        GeminiFehler.modell(code, reason) -> "Gemini kennt das Modell oder eine Einstellung nicht: " + reason.take(160)
        reason.isNotBlank() -> "Gemini ($code): " + reason.take(200)
        else -> "Gemini hat die Verbindung beendet (Code $code)."
    }

    /** Ein Wiederholversuch mit demselben Schlüssel läuft schon; zurückgesetzt, sobald eine Verbindung steht. */
    @Volatile private var gleicherVersuch = false
    /** Der Wiederholversuch wartet kurz; bis dahin beendet das Schließen der alten Verbindung das Gespräch nicht. */
    @Volatile private var wartetAufNeu = false
    /** Mitteilung zum Schlüsselwechsel, die erst rausgeht, wenn die bezahlte Verbindung wirklich steht (Text, Anlass). */
    @Volatile private var wechselMeldung: Pair<String, String>? = null

    /**
     * Netz weg oder Serverfehler: Das muss nicht am kostenlosen Schlüssel liegen. Erst einmal mit demselben Schlüssel
     * und der Fortsetzungskennung neu verbinden; scheitert auch das, auf den bezahlten wechseln.
     */
    private fun geminiStoerung(anlass: String): Boolean {
        if (!laeuft) return false
        if (gleicherVersuch) return geminiErsatz(anlass)
        gleicherVersuch = true
        Log.w(LiveRouting.TAG, "Gemini: Störung ($anlass), verbinde mit demselben Schlüssel neu")
        geminiKosten()
        status("Verbinde neu …")
        wartetAufNeu = true
        Thread({ runCatching { Thread.sleep(2_000) }; wartetAufNeu = false; if (laeuft) geminiVerbinden(aktuellerSchluessel) }, "jarvis-gemini-neu").start()
        return true
    }

    /** Kostenloser Schlüssel gestört (Kontingent aus, abgelehnt, nicht erreichbar): einmal auf den bezahlten wechseln, das Gespräch läuft weiter. */
    private fun geminiErsatz(anlass: String): Boolean {
        val ersatz = ersatzSchluessel
        if (ersatz.isBlank() || ersatz == aktuellerSchluessel || !laeuft) return false
        Log.w(LiveRouting.TAG, "Gemini: kostenloser Schlüssel gestört ($anlass), wechsle auf den bezahlten Schlüssel")
        val grund = if (GeminiFehler.kontingent(anlass) || anlass.startsWith("HTTP 429")) "Das kostenlose Gemini-Kontingent ist erschöpft"
            else "Der kostenlose Gemini-Schlüssel ist gerade nicht nutzbar"
        wechselMeldung = "$grund. Jarvis spricht jetzt mit dem bezahlten Schlüssel weiter." to anlass
        // Was die kostenlose Verbindung schon verbraucht hat, kostet nichts: noch als kostenlos verbuchen.
        geminiKosten()
        gratis = false
        // Eine Fortsetzungskennung gehört zum Projekt des alten Schlüssels: frisch beginnen, ohne neue Begrüßung.
        fortsetzKennung = ""
        geminiVerbinden(ersatz)
        return true
    }

    /** Kleine Mitteilung von Jarvis, dass ab jetzt der bezahlte Gemini-Schlüssel läuft; im Gespräch ohne Ton. */
    private fun schluesselWechselMelden(text: String, anlass: String) = runCatching {
        de.frank.jarvis.dienst.JarvisDienst.kanalAnlegen(app)
        app.getSystemService(android.app.NotificationManager::class.java).notify(4320,
            androidx.core.app.NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(de.frank.jarvis.R.drawable.ic_stat_jarvis)
                .setContentTitle("Jarvis: bezahlter Gemini-Schlüssel aktiv")
                .setContentText(text)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText("$text\nAnlass: ${anlass.take(200)}"))
                .setAutoCancel(true)
                .setSilent(de.frank.jarvis.speech.GespraechsFokus.stilleHinweise)
                .build())
    }.onFailure { Log.w(LiveRouting.TAG, "Gemini: Mitteilung zum Schlüsselwechsel fehlgeschlagen: ${it.message}") }

    private fun geminiSetup(): JSONObject {
        val g = JSONObject().put("responseModalities", JSONArray().put("AUDIO"))
        val sprache = JSONObject().put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", st["stimme"])))
        g.put("speechConfig", sprache)
        LiveOptionen.geminiDenken(st)?.let { g.put("thinkingConfig", it) }
        st["g_temperatur"].toDoubleOrNull()?.let { g.put("temperature", it) }
        st["g_max"].toIntOrNull()?.takeIf { it > 0 }?.let { g.put("maxOutputTokens", it) }
        // Das bisherige Gespräch steht in der Anweisung: Das funktioniert bei allen Live-Modellen, auch ohne Verlaufs-Freigabe.
        val bisher = if (verlauf.isEmpty()) "" else verlauf.joinToString("\n", "\n# Bisheriges Gespräch (zum Anknüpfen)\n") { (mir, t) -> (if (mir) "Frank: " else "Jarvis: ") + t.take(1200) }
        val werkzeuge = JSONArray().put(JSONObject().put("functionDeclarations", JSONArray().put(JSONObject()
            .put("name", "jarvis_fragen")
            .put("description", jarvisWerkzeug().optString("description"))
            .put("parameters", JSONObject().put("type", "OBJECT")
                .put("properties", JSONObject().put("auftrag", JSONObject().put("type", "STRING").put("description", "Die vollständige Frage oder der Auftrag")))
                .put("required", JSONArray().put("auftrag")))
            .apply { if (LiveOptionen.geminiNebenbei(st)) put("behavior", "NON_BLOCKING") })))
        // Jarvis direkt: Gemini sucht nicht selbst, sonst beantwortet es Wissensfragen an Jarvis vorbei.
        if (st["g_suche"] == "an" && !st.jarvisDirekt) werkzeuge.put(JSONObject().put("googleSearch", JSONObject()))
        val erkennung = JSONObject()
            .put("startOfSpeechSensitivity", if (st["g_start"] == "hoch") "START_SENSITIVITY_HIGH" else "START_SENSITIVITY_LOW")
            .put("endOfSpeechSensitivity", if (st["g_ende"] == "hoch") "END_SENSITIVITY_HIGH" else "END_SENSITIVITY_LOW")
            .put("prefixPaddingMs", 20)
            .put("silenceDurationMs", st["g_stille"].toIntOrNull() ?: 500)
        val s = JSONObject()
            .put("model", "models/$modell")
            .put("generationConfig", g)
            // Native-Audio-Modelle nehmen keinen Sprachcode: Deutsch erzwingt die Anweisung (Googles Formulierung).
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                anweisung + bisher + "\n# Language\nRESPOND IN GERMAN. YOU MUST RESPOND UNMISTAKABLY IN GERMAN."))))
            .put("tools", werkzeuge)
            .put("realtimeInputConfig", JSONObject().put("automaticActivityDetection", erkennung)
                .put("activityHandling", if (st["unterbrechen"] == "an") "START_OF_ACTIVITY_INTERRUPTS" else "NO_INTERRUPTION"))
            .put("outputAudioTranscription", JSONObject())
            .put("contextWindowCompression", JSONObject().put("slidingWindow", JSONObject()).apply { LiveOptionen.geminiKuerzenAb(st)?.let { put("triggerTokens", it) } })
            .put("sessionResumption", JSONObject().apply { if (fortsetzKennung.isNotEmpty()) put("handle", fortsetzKennung) })
        if (st["g_mitschrift"] != "aus") s.put("inputAudioTranscription", JSONObject())
        return JSONObject().put("setup", s)
    }

    /** Gemini verwirft nach dem Antippen den Rest der laufenden Antwort bis zu ihrem Ende. */
    @Volatile private var verwerfenGemini = false
    /** Nutzungsdaten der laufenden Antwort: Gemini schickt sie oft mehrfach, verbucht wird der letzte Stand je Antwort. */
    private var geminiNutzung: JSONObject? = null

    private fun geminiEreignis(w: WebSocket, e: JSONObject) {
        // Je Antwort den größten Stand nehmen: Die letzte Meldung (mit turnComplete) ist teils leer.
        e.optJSONObject("usageMetadata")?.let { u -> if (u.optInt("totalTokenCount") >= (geminiNutzung?.optInt("totalTokenCount") ?: 0)) geminiNutzung = u }
        when {
            e.has("setupComplete") -> {
                bereit = true
                gleicherVersuch = false
                wechselMeldung?.let { (text, anlass) -> wechselMeldung = null; beiFehler(text); schluesselWechselMelden(text, anlass) }
                abgeloest?.let { a -> abgeloest = null; Thread({ runCatching { Thread.sleep(1_500); a.close(1000, "Neue Verbindung") } }, "jarvis-gemini-alt").start() }
                if (startMs == 0L) startMs = System.currentTimeMillis()
                tonStarten()
                if (!begruesst) {
                    begruesst = true
                    w.send(JSONObject().put("realtimeInput", JSONObject().put("text", "Begrüße Frank kurz auf Deutsch, höchstens ein Satz, dann hör zu.")).toString())
                }
                if (laeuft) status("Ich höre zu")
            }
            e.has("serverContent") -> {
                val c = e.optJSONObject("serverContent") ?: return
                letzteAktivitaet = System.currentTimeMillis()
                c.optJSONObject("inputTranscription")?.optString("text")?.takeIf { it.isNotEmpty() }?.let { d ->
                    // Frank spricht neu: Was danach kommt, ist die Antwort darauf und wird nie verworfen.
                    if (!geminiAntwortLaeuft) verwerfenGemini = false
                    synchronized(nutzerTeile) { nutzerTeile.append(d) }
                }
                if (c.optBoolean("interrupted")) {
                    // Frank redet dazwischen: Gemini hat die Antwort schon abgebrochen, der gepufferte Rest verstummt.
                    verwerfenGemini = false
                    geminiAntwortLaeuft = false
                    geminiKosten()
                    ausgabeLeeren()
                    jarvisFertig()
                    status("Ich höre zu")
                }
                c.optJSONObject("outputTranscription")?.optString("text")?.takeIf { it.isNotEmpty() }?.let { d ->
                    nutzerFertig()
                    if (!verwerfenGemini) synchronized(jarvisTeile) { jarvisTeile.append(d) }
                }
                val teile = c.optJSONObject("modelTurn")?.optJSONArray("parts")
                if (teile != null && !verwerfenGemini) for (i in 0 until teile.length()) {
                    val p = teile.optJSONObject(i) ?: continue
                    if (p.optBoolean("thought")) continue
                    val daten = p.optJSONObject("inlineData")?.optString("data")?.takeIf { it.isNotEmpty() } ?: continue
                    nutzerFertig()
                    geminiAntwortLaeuft = true
                    letzteHoerbarEmpfangMs = System.currentTimeMillis()
                    if (!sprichtGerade) { sprichtGerade = true; status(sprichtText()) }
                    ausgabe.offer(Stueck(Base64.decode(daten, Base64.DEFAULT), true))
                }
                if (c.optBoolean("turnComplete")) {
                    verwerfenGemini = false
                    geminiAntwortLaeuft = false
                    nutzerFertig(); jarvisFertig()
                    geminiKosten()
                    // Der Ton läuft noch aus dem Puffer: „Jarvis spricht“ setzt der Mikrofon-Faden zurück, wenn er verklungen ist.
                    if (laeuft && kennungArt.isEmpty() && !sprichtGerade) status("Ich höre zu")
                }
            }
            e.has("toolCall") -> {
                val aufrufe = e.optJSONObject("toolCall")?.optJSONArray("functionCalls") ?: return
                for (i in 0 until aufrufe.length()) {
                    val f = aufrufe.optJSONObject(i) ?: continue
                    if (f.optString("name") != "jarvis_fragen") continue
                    val id = f.optString("id")
                    kennungArt[id] = "gemini"
                    aufrufVerbindung[id] = w
                    nutzerFertig(); jarvisFertig()
                    bestaetigungVerschlucken = false
                    status("Jarvis schaut nach …")
                    beiWerkzeug(id, f.optJSONObject("args")?.optString("auftrag").orEmpty())
                }
            }
            e.has("toolCallCancellation") -> {
                val ids = e.optJSONObject("toolCallCancellation")?.optJSONArray("ids") ?: return
                // Die Kennung bleibt als „verworfen“ stehen: Ein spätes Ergebnis darf nie als anderes Protokoll hinausgehen.
                for (i in 0 until ids.length()) { kennungArt[ids.optString(i)] = "gemini_abgebrochen"; aufrufVerbindung.remove(ids.optString(i)) }
            }
            e.has("sessionResumptionUpdate") -> {
                val u = e.optJSONObject("sessionResumptionUpdate") ?: return
                if (u.optBoolean("resumable") && u.optString("newHandle").isNotEmpty()) fortsetzKennung = u.optString("newHandle")
            }
            e.has("goAway") -> {
                // Gemini trennt gleich (Verbindungen halten etwa 10 Minuten): mit der Kennung nahtlos auf eine neue Verbindung.
                Log.i(LiveRouting.TAG, "Gemini goAway, Rest ${e.optJSONObject("goAway")?.optString("timeLeft")}, Fortsetzung " + if (fortsetzKennung.isEmpty()) "nicht möglich" else "folgt")
                if (fortsetzKennung.isNotEmpty() && laeuft) { geminiKosten(); geminiVerbinden(aktuellerSchluessel) }
            }
        }
    }

    /** Verbucht die Nutzungsdaten der fertigen Antwort genau einmal, mit den Gemini-Preisen aus [LiveOptionen]. */
    private fun geminiKosten() {
        val u = geminiNutzung ?: return
        geminiNutzung = null
        val k = LiveOptionen.geminiKosten(modell, u) ?: run { ohnePreis++; return }
        // Kostenloser Schlüssel: Google berechnet nichts, nur fürs Protokoll der Listenpreis.
        if (gratis) { Log.i(LiveRouting.TAG, "Gemini-Antwort über das kostenlose Kontingent, Listenpreis wäre " + LiveOptionen.usdText(k)); return }
        tokenKosten += k
        kostenMelden()
    }

    private fun geminiAudio(pcm: ByteArray): String = JSONObject().put("realtimeInput", JSONObject().put("audio", JSONObject()
        .put("data", Base64.encodeToString(pcm, Base64.NO_WRAP)).put("mimeType", "audio/pcm;rate=16000"))).toString()

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
                if (laeuft) status("Ich höre zu")
            }
            "session.output_audio.delta" -> {
                val jetzt = System.currentTimeMillis()
                letzteAktivitaet = jetzt
                val daten = Base64.decode(e.optString("delta"), Base64.DEFAULT)
                // GPT-Live sagt nicht, wann es spricht: Nur hörbarer Ton zählt als Sprechen, Stille hält nichts fest.
                val hoerbar = hoerbar(daten)
                ausgabeStatistik(jetzt, daten.size, hoerbar)
                if (verwerfenLive) {
                    // Die abgebrochene Antwort läuft noch nach; erst nach 1,5 s ohne hörbaren Ton kommt wieder Neues, spätestens nach 10 s.
                    if (jetzt - verwerfenSeitMs < VERWERFEN_MAX_MS && (!hoerbar || jetzt - letzteVerworfenMs < 1_500)) {
                        if (hoerbar) letzteVerworfenMs = jetzt
                        return
                    }
                    verwerfenLive = false
                    Log.i(LiveRouting.TAG, "Verwerfen nach dem Antippen beendet, neue Ausgabe wird gespielt")
                }
                if (hoerbar) {
                    letzteHoerbarEmpfangMs = jetzt
                    if (!sprichtGerade) { sprichtGerade = true; status(sprichtText()) }
                }
                ausgabe.offer(Stueck(daten, hoerbar, !hoerbar && spitze(daten) < STILLE_SPITZE))
            }
            "session.input_transcript.delta" -> {
                letzteAktivitaet = System.currentTimeMillis()
                // GPT-Live meldet kein „Frank spricht“: Kommt seine Mitschrift, verstummt die gepufferte Ausgabe. Mit Echosperre
                // bekommt der Dienst während Jarvis' Rede nur Stille; eine Mitschrift ist dann die verspätete vom Satz davor und
                // darf die gerade beginnende Antwort nicht löschen. Unterbrechen geht dort per Antippen.
                if (sprichtGerade) {
                    if (echoSperre) Log.i(LiveRouting.TAG, "Mitschrift kommt, während Jarvis spricht: Echosperre an, Ausgabe bleibt")
                    else { ausgabeLeeren(); status("Ich höre zu") }
                }
                if (!echoSperre) verwerfenLive = false
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
                status("Jarvis schaut nach …")
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
                        status("Jarvis schaut nach …")
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
                Log.w(LiveRouting.TAG, "GPT-Live-Fehler: $text")
                beiFehler("GPT-Live: " + text.ifBlank { "unbekannter Fehler" })
                // Vor session.started ist die Sitzung nie zustande gekommen.
                if (!bereit) beenden(senden = false)
                // Sonst bliebe „Jarvis schaut nach …“ stehen, obwohl keine Antwort mehr kommt.
                else if (!sprichtGerade && laeuft) status("Ich höre zu")
            }
            else -> { val typ = e.optString("type"); if (gemeldeteTypen.add(typ)) Log.i(LiveRouting.TAG, "GPT-Live-Ereignis ohne Behandlung: $typ") }
        }
    }

    /** Echter Ton statt (fast) Stille: Effektivwert über der Schwelle. */
    private fun hoerbar(b: ByteArray): Boolean {
        val n = b.size - b.size % 2
        if (n < 2) return false
        var summe = 0.0
        var i = 0
        while (i + 1 < n) { val s = (b[i].toInt() and 0xFF) or (b[i + 1].toInt() shl 8); summe += s.toDouble() * s; i += 2 }
        return kotlin.math.sqrt(summe / (n / 2)) > HOERBAR_EFFEKTIV
    }

    /** Größter Ausschlag (PCM16): Auch ein leiser Wortansatz liegt weit über [STILLE_SPITZE]. */
    private fun spitze(b: ByteArray): Int {
        var max = 0
        var i = 0
        while (i + 1 < b.size) { val s = (b[i].toInt() and 0xFF) or (b[i + 1].toInt() shl 8); val a = if (s < 0) -s else s; if (a > max) max = a; i += 2 }
        return max
    }

    private var statLetzteMs = 0L
    private var statLuecke = 0L

    /** Nur im OkHttp-Faden. Größte Lücke zwischen zwei Stücken = Empfangsschwankung (Netz/Dienst), Soll 100 ms. */
    private fun ausgabeStatistik(jetzt: Long, bytes: Int, hoerbar: Boolean) {
        if (statSeit == 0L) statSeit = jetzt
        if (statLetzteMs > 0) statLuecke = maxOf(statLuecke, jetzt - statLetzteMs)
        statLetzteMs = jetzt
        statDeltas++; statBytes += bytes; if (hoerbar) statHoerbar++
        if (jetzt - statSeit < 2_000) return
        Log.i(LiveRouting.TAG, "GPT-Live-Ausgabe in ${jetzt - statSeit} ms: $statDeltas Stücke, ${statBytes / 48} ms Ton, davon $statHoerbar hörbar, " +
            "größte Lücke $statLuecke ms, Warteschlange ${ausgabe.size}, spricht=$sprichtGerade, verwerfen=$verwerfenLive")
        statSeit = jetzt; statDeltas = 0; statHoerbar = 0; statBytes = 0; statLuecke = 0
    }

    /** Status an die Leiste, Wechsel auch ins Logcat. */
    private fun status(s: String) {
        if (s != letzterStatus) { letzterStatus = s; Log.i(LiveRouting.TAG, "Status: $s") }
        beiStatus(s)
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
        // OpenAI nimmt bei Realtime als Zahl höchstens 4.096 an; darüber gilt „inf“ (in der Auswahl so beschrieben).
        st["rt_max"].toIntOrNull()?.let { s.put("max_output_tokens", if (it > REALTIME_MAX_ZAHL) "inf" else it) }
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
                if (!sprichtGerade) { sprichtGerade = true; status(sprichtText()) }
                // Realtime grenzt seine Antworten selbst ab: Jedes Stück zählt wie bisher als Ton.
                ausgabe.offer(Stueck(daten, true))
            }
            "input_audio_buffer.speech_started" -> {
                letzteAktivitaet = System.currentTimeMillis()
                if (sprichtGerade && antwortItem.isNotEmpty() && st["unterbrechen"] == "an") {
                    val ms = gehoertMs()
                    ws?.send(JSONObject().put("type", "conversation.item.truncate").put("item_id", antwortItem).put("content_index", 0).put("audio_end_ms", ms).toString())
                }
                if (st["unterbrechen"] == "an") { ausgabeLeeren(); status("Ich höre zu") }
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
                if (laeuft && !ruftWerkzeug) status("Ich höre zu")
            }
            "response.function_call_arguments.done" -> {
                val name = e.optString("name")
                if (name.isNotEmpty() && name != "jarvis_fragen") return
                val auftrag = runCatching { JSONObject(e.optString("arguments")).optString("auftrag") }.getOrNull().orEmpty()
                val callId = e.optString("call_id")
                kennungArt[callId] = "realtime"
                bestaetigungVerschlucken = false
                status("Jarvis schaut nach …")
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
            "gemini_abgebrochen" -> Log.i(LiveRouting.TAG, "Gemini hat den Werkzeugaufruf abgebrochen: Ergebnis verworfen")
            "gemini" -> {
                val r = JSONObject().put("id", kennung).put("name", "jarvis_fragen").put("response", JSONObject().put("result", text))
                if (LiveOptionen.geminiNebenbei(st) && modell == "gemini-3.8-live") r.put("scheduling", st["g_melden"])
                // An die Verbindung, über die der Aufruf kam, solange sie noch offen ist (goAway-Wechsel).
                val ziel = aufrufVerbindung.remove(kennung)?.takeIf { it === w || it === abgeloest } ?: w
                ziel.send(JSONObject().put("toolResponse", JSONObject().put("functionResponses", JSONArray().put(r))).toString())
                if (kennungArt.isEmpty() && !sprichtGerade && laeuft) status("Ich höre zu")
            }
            "auftrag" -> {
                // Höchstens 500 Tokens je Kommentar.
                w.send(JSONObject().put("type", "session.commentary.append").put("delegation_id", kennung).put("content", text.take(1500)).toString())
            }
            "funktion" -> {
                w.send(JSONObject().put("type", "response.item.create").put("item", JSONObject().put("type", "function_call_output").put("call_id", kennung).put("output", text)).toString())
                w.send(JSONObject().put("type", "response.create").toString())
            }
            else -> {
                if (gemini) { Log.w(LiveRouting.TAG, "Gemini: Ergebnis zu unbekannter Kennung verworfen"); return }
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
            // Medienweg auch am Handy-Lautsprecher: Der Telefonmodus klingt dort nach Anruf. Freisprechen nur, wenn es gewünscht
            // ist und ein Headset oder die Freisprechanlage ein eigenes Mikrofon mitbringt; ohne Bluetooth immer Medienton.
            val ausgaenge = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            medienweg = LiveRouting.medienweg(st["audioweg"], ausgaenge.map { it.type })
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
            // Gemini erwartet 16 kHz: das Mikrofon gleich so öffnen statt umzurechnen; die Wiedergabe bleibt bei 24 kHz.
            val mikroRate = if (gemini) 16000 else rate
            val minIn = AudioRecord.getMinBufferSize(mikroRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            // VOICE_COMMUNICATION auch im Medienweg: Nur diese Quelle bekommt die Echounterdrückung des Geräts.
            val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mikroRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minIn, mikroRate / 5 * 2) * 2)
            aufnahme = rec
            if (medienweg) {
                // Fest das Handy-Mikrofon: Das Bluetooth-Mikrofon ginge nur über das Telefonprofil und risse den Medienton mit.
                audio.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                    ?.let { m -> runCatching { rec.setPreferredDevice(m) } }
            }
            if (rec.state != AudioRecord.STATE_INITIALIZED) false
            else {
                tonAnlegen(rec, rate, mikroRate)
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
    private fun tonAnlegen(rec: AudioRecord, rate: Int, mikroRate: Int) {
        if (AcousticEchoCanceler.isAvailable()) echo = AcousticEchoCanceler.create(rec.audioSessionId)?.also { it.enabled = true }
        if (NoiseSuppressor.isAvailable()) rauschen = NoiseSuppressor.create(rec.audioSessionId)?.also { it.enabled = true }
        Log.i(LiveRouting.TAG, "Echounterdrückung des Geräts: verfügbar=${AcousticEchoCanceler.isAvailable()}, aktiv=${runCatching { echo?.enabled }.getOrNull() ?: false}")

        val minOut = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            // Medienweg: USAGE_MEDIA folgt sicher dem Medienton (A2DP); Freisprechen: Gesprächston über das Kommunikationsgerät.
            .setAudioAttributes(AudioAttributes.Builder().setUsage(if (medienweg) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            // Platz für Startpolster plus Empfangsschübe, ohne dass write() blockiert; die Verzögerung bestimmt der Füllstand, nicht die Größe.
            // Nur GPT-Live: Realtime liefert schneller als Echtzeit, ein großer Puffer verschöbe dort das Kürzen beim Dazwischenreden.
            .setBufferSizeInBytes(if (live) maxOf(minOut * 4, rate * 2) else minOut * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        wiedergabe = track
        // Vor play() Stille vorlegen: Ein leer gestarteter Strom wirft AudioFlinger nach einer Sekunde wegen Unterlauf
        // aus dem Mischer (gemessen: „BUFFER TIMEOUT … underrun“, danach „restartIfDisabled“ beim ersten Ton).
        val vorlauf = ByteArray((rate / 1000 * 2 * START_POLSTER_MS).toInt())
        geschriebeneFrames.addAndGet(maxOf(0, runCatching { track.write(vorlauf, 0, vorlauf.size) }.getOrDefault(0)) / 2L)
        track.play()
        Log.i(LiveRouting.TAG, "Wiedergabe: Mindestpuffer ${minOut / 48} ms, Puffer ${runCatching { track.bufferSizeInFrames }.getOrDefault(0) / 24} ms, " +
            "Startpolster $START_POLSTER_MS ms, Füllgrenze $FUELL_UNTER_MS ms")

        // Kam beenden() während des Anlegens, sofort alles wieder freigeben: sonst blieben Mikrofon und Audiomodus belegt.
        if (!laeuft) { tonAus(); return }

        // Zeigt, was das System tatsächlich nutzt, auch nach Verbinden oder Trennen im Gespräch.
        val waechter = android.media.AudioRouting.OnRoutingChangedListener { audiowegMelden() }
        runCatching { track.addOnRoutingChangedListener(waechter, hauptFaden); rec.addOnRoutingChangedListener(waechter, hauptFaden) }
        routenWaechter = waechter
        audiowegMelden()

        // 100-ms-Blöcke (4.800 Byte), wie OpenAI empfiehlt; Fehler in den Fäden enden als Meldung, nie als Absturz.
        Thread({
            runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO) }
            // 100 ms je Block: 4.800 Byte bei 24 kHz, 3.200 bei 16 kHz (Gemini).
            val puffer = ByteArray(mikroRate / 10 * 2)
            // Unter der Sperre: Hat tonAus() die Aufnahme schon freigegeben, startet sie nicht mehr.
            val ok = synchronized(tonSperre) {
                laeuft && aufnahme === rec && runCatching { rec.startRecording() }.isSuccess && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING
            }
            if (!ok) { if (laeuft) { beiFehler("Das Mikrofon ließ sich für das Live-Gespräch nicht starten."); beenden() }; return@Thread }
            // Das Mikrofon kennt sein Gerät erst nach dem Start.
            hauptFaden.post { audiowegMelden() }
            val anhaengen = if (live) "session.input_audio.append" else "input_audio_buffer.append"
            var warZu = false
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
                // Dieser Faden blockiert nie an der Wiedergabe: Hier werden Sperre und Sprechanzeige auch dann frei, wenn der
                // Ton-Faden hängt oder die Kopfposition nicht stimmt. Spätestens ECHO_SPERRE_MAX_MS nach dem letzten hörbaren Ton
                // ist das Mikrofon wieder offen.
                val jetzt = System.currentTimeMillis()
                val kopf = (runCatching { wiedergabe?.playbackHeadPosition ?: 0 }.getOrDefault(0).toLong() and 0xFFFFFFFFL) - kopfBasis
                val tonOffen = ausgabe.any { it.hoerbar } || kopf < echtBisFrame
                val echoZu = echoSperre && run {
                    if (tonOffen) echoFreiAb = jetzt + ECHO_NACHLAUF_MS
                    jetzt < echoFreiAb && jetzt - letzteHoerbarMs < ECHO_SPERRE_MAX_MS
                }
                if (echoZu != warZu) {
                    warZu = echoZu
                    Log.i(LiveRouting.TAG, "Echosperre " + (if (echoZu) "zu" else "auf") + ": Kopf $kopf, Ton bis $echtBisFrame, Warteschlange ${ausgabe.size}, " +
                        "letzter Ton vor ${jetzt - letzteHoerbarMs} ms")
                }
                // GPT-Live meldet kein Ende der Rede: Ist kein hörbarer Ton mehr offen, hört Jarvis wieder zu. Hängt die
                // Wiedergabe, gilt nach STATUS_MAX_MS ohne geschriebenen und empfangenen Ton dasselbe.
                if ((live || gemini) && sprichtGerade) {
                    val seit = jetzt - maxOf(letzteHoerbarMs, letzteHoerbarEmpfangMs)
                    if ((!tonOffen && seit >= 300) || seit >= STATUS_MAX_MS) {
                        if (tonOffen) Log.w(LiveRouting.TAG, "Wiedergabe kommt nicht voran: Sprechanzeige zurückgesetzt, Warteschlange ${ausgabe.size}")
                        sprichtGerade = false
                        if (laeuft) status("Ich höre zu")
                    }
                }
                val stueck = if ((stumm && !live) || echoZu) ByteArray(n) else puffer.copyOf(n)
                // Gemini: nur an eine fertig aufgebaute Verbindung, beim Wechsel (goAway, Schlüssel) kurz nichts.
                if (gemini) { if (bereit) ws?.send(geminiAudio(stueck)); continue }
                ws?.send(JSONObject().put("type", anhaengen).put("audio", Base64.encodeToString(stueck, Base64.NO_WRAP)).toString())
            }
        }, "jarvis-live-mikro").start()

        // Medienweg: In Sprechpausen Stille nachschieben. Läuft die Wiedergabe leer, geht der Ausgang nach wenigen Sekunden in
        // Bereitschaft, der Bluetooth-Medienstrom (A2DP) wird ausgesetzt und viele Lautsprecher verschlucken beim Wiederanlauf
        // den Satzanfang. Die Stille zählt weder als Ausgabe (Echosperre) noch als abgespielt (Kürzen beim Dazwischenreden).
        // Nachgeschoben wird nur, wenn die Wiedergabe gleich leerläuft: GPT-Live liefert in Echtzeit, ein verspätetes Stück
        // bekäme sonst Stille vorgesetzt, obwohl noch genug Ton im Puffer liegt, und die Rede stockte hörbar.
        val stille = ByteArray(rate / 1000 * 2 * STILLE_MS)
        val fuellPause = rate / 1000L * FUELL_UNTER_MS
        val fuellSprache = rate / 1000L * FUELL_SPRACHE_MS
        // Polster nur für GPT-Live: Realtime liefert schubweise schneller als Echtzeit und braucht keins.
        val startPolster = if (live) rate / 1000L * START_POLSTER_MS else 0L
        val nachfuellenUnter = startPolster - rate / 1000L * MISCHTAKT_MS
        val kappenUeber = startPolster + rate / 1000L * KAPPEN_UEBER_MS
        val polster = ByteArray((rate / 1000L * START_POLSTER_MS * 2).toInt())
        Thread({
            // Vorrang vor Hintergrundarbeit (Recherchen, Synchronisation): Der Ton darf nie auf die CPU warten.
            runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO) }
            // Messung alle 2 s, nur Zahlen: Füllstand, Füll-Stille, Startpolster, Unterläufe, längstes Schreiben.
            var mSeit = System.currentTimeMillis()
            var mMin = Long.MAX_VALUE; var mMax = 0L; var mFuell = 0L; var mFuellSprache = 0L; var mPolster = 0L; var mGekappt = 0L; var mSchreiben = 0L; var mTon = false
            var mUnterlauf = runCatching { track.underrunCount }.getOrDefault(0)
            fun schreiben(b: ByteArray, ab: Int, n: Int): Int {
                if (n <= 0) return 0
                val t0 = System.currentTimeMillis()
                val r = runCatching { wiedergabe?.write(b, ab, n) }.getOrNull() ?: 0
                mSchreiben = maxOf(mSchreiben, System.currentTimeMillis() - t0)
                if (r > 0) geschriebeneFrames.addAndGet(r / 2L)
                return r
            }
            // Zuletzt Geschriebenes war Stille (Vorlauf, Füll-Stille, Polster, ganz stilles Stück): Nur dahinter darf ein
            // Polster entstehen, sonst könnte es zwischen einen leisen Wortansatz und den Rest des Wortes geraten.
            var vorherStill = true
            while (laeuft) {
                val stueck = runCatching { ausgabe.poll(if (medienweg) WARTE_MS else 200L, TimeUnit.MILLISECONDS) }.getOrNull()
                val gepuffert = gepuffertFrames()
                mMin = minOf(mMin, gepuffert); mMax = maxOf(mMax, gepuffert)
                // Die Sprechanzeige von GPT-Live setzt der Mikrofon-Faden zurück; dieser Faden kann im Schreiben blockieren.
                if (stueck == null) {
                    // In Pausen zwei Mischtakte Reserve; beim Sprechen erst knapp über einem Takt, damit ein nur leicht verspätetes
                    // Stück keine Stille vorgesetzt bekommt.
                    if (medienweg && laeuft && gepuffert < (if (sprichtGerade) fuellSprache else fuellPause)) {
                        val r = schreiben(stille, 0, stille.size)
                        if (r > 0) { mFuell += r / 2; if (sprichtGerade) mFuellSprache += r / 2; vorherStill = true }
                    }
                } else {
                    // Polster gegen Empfangsschwankungen: GPT-Live liefert genau in Echtzeit, der Puffer wächst also nie von selbst.
                    // In Pausen wird er hinter Stille wieder auf START_POLSTER_MS aufgefüllt. Früher kam das Polster direkt vor das
                    // erste hörbare Stück; lag der leise Wortansatz („K“ von „Klar“) noch im Stück davor, riss die Stille das Wort auf.
                    val stilleSeit = System.currentTimeMillis() - letzteHoerbarMs
                    var ab = 0
                    var satz: String? = null
                    if (startPolster > 0 && stilleSeit > NEUER_SATZ_MS && gepuffert < nachfuellenUnter && (vorherStill || gepuffert == 0L)) {
                        val r = schreiben(polster, 0, ((startPolster - gepuffert) * 2).toInt())
                        if (r > 0) mPolster += r / 2
                        if (stueck.hoerbar) satz = "Satzanfang mit Polster direkt davor: Puffer ${gepuffert / 24} ms, Stille davor $stilleSeit ms, ${r / 48} ms vorgelegt"
                    } else if (startPolster > 0 && stueck.ganzStill && stilleSeit > KAPPEN_PAUSE_MS && gepuffert > kappenUeber) {
                        // Nach einem Empfangsschub läge sonst bis zur nächsten Antwort mehr Verzögerung als nötig im Puffer:
                        // nur den Überschuss aus digitaler Stille in einer längeren Pause schneiden.
                        ab = (minOf(gepuffert - kappenUeber, stueck.daten.size / 2L) * 2).toInt()
                        mGekappt += ab / 2
                    } else if (stueck.hoerbar && stilleSeit > NEUER_SATZ_MS) {
                        satz = "Satzanfang: Puffer ${gepuffert / 24} ms, Stille davor $stilleSeit ms"
                    }
                    // Vor und nach dem Schreiben: Ein großes Stück läuft schon hörbar an, während write() noch blockiert.
                    if (stueck.hoerbar) { letzteHoerbarMs = System.currentTimeMillis(); mTon = true }
                    letzteAusgabeMs = System.currentTimeMillis()
                    val geschrieben = schreiben(stueck.daten, ab, stueck.daten.size - ab)
                    if (geschrieben > 0 && stueck.hoerbar) echtBisFrame = geschriebeneFrames.get()
                    letzteAusgabeMs = System.currentTimeMillis()
                    if (stueck.hoerbar) letzteHoerbarMs = letzteAusgabeMs
                    vorherStill = stueck.ganzStill
                    abgespieltBytes += stueck.daten.size
                    // Erst nach dem Schreiben protokollieren: nichts zwischen Füllstandsmessung und write().
                    satz?.let { Log.i(LiveRouting.TAG, it) }
                }
                val jetzt = System.currentTimeMillis()
                if (jetzt - mSeit >= 2_000) {
                    val unterlauf = runCatching { track.underrunCount }.getOrDefault(mUnterlauf)
                    if (mTon || unterlauf != mUnterlauf || mFuellSprache > 0 || mGekappt > 0)
                        Log.i(LiveRouting.TAG, "Wiedergabe in ${jetzt - mSeit} ms: Puffer ${if (mMin == Long.MAX_VALUE) 0 else mMin / 24}–${mMax / 24} ms, " +
                            "Füll-Stille ${mFuell / 24} ms (beim Sprechen ${mFuellSprache / 24} ms), Polster ${mPolster / 24} ms, gekappte Stille ${mGekappt / 24} ms, " +
                            "Unterläufe +${unterlauf - mUnterlauf}, längstes Schreiben $mSchreiben ms, Warteschlange ${ausgabe.size}")
                    mSeit = jetzt; mMin = Long.MAX_VALUE; mMax = 0; mFuell = 0; mFuellSprache = 0; mPolster = 0; mGekappt = 0; mSchreiben = 0; mTon = false
                    mUnterlauf = unterlauf
                }
            }
        }, "jarvis-live-ton").start()
    }

    /** Realtime: wirklich gehörte Millisekunden der Antwort, also geschrieben minus das, was noch im Puffer wartet. */
    private fun gehoertMs(): Int = (minOf(abgespieltBytes, empfangenBytes) / 48 - gepuffertFrames() / 24).coerceAtLeast(0).toInt()

    /** Frames, die noch auf den Lautsprecher warten (echter Ton wie Stille); 0, wenn die Wiedergabe leer ist. */
    private fun gepuffertFrames(): Long {
        val kopf = (runCatching { wiedergabe?.playbackHeadPosition ?: 0 }.getOrDefault(0).toLong() and 0xFFFFFFFFL) - kopfBasis
        return (geschriebeneFrames.get() - kopf).coerceAtLeast(0)
    }

    private fun ausgabeLeeren() {
        ausgabe.clear()
        sprichtGerade = false
        wiedergabe?.let { runCatching { it.pause(); it.flush(); it.play() } }
        // Nach dem Leeren ist nichts Echtes mehr unterwegs: Die Echosperre öffnet sofort (nach ihrem kurzen Nachlauf).
        geschriebeneFrames.set(0)
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
        if (!laeuft) return
        // Nichts zu unterbrechen: Die Anzeige trotzdem auf den echten Zustand stellen, falls sie noch „spricht“ zeigt.
        if (!sprichtGerade && ausgabe.isEmpty()) {
            Log.i(LiveRouting.TAG, "Antippen ohne laufende Ausgabe, Status „$letzterStatus“")
            if (bereit && letzterStatus.startsWith("Jarvis spricht")) status("Ich höre zu")
            return
        }
        if (gemini) {
            // Gemini kennt kein Abbrechen von außen: Der Rest der Antwort wird bis zu ihrem Ende verworfen. Ist sie schon
            // vollständig da (nur noch im Puffer), genügt das Leeren; sonst verschluckte das Verwerfen die nächste Antwort.
            if (geminiAntwortLaeuft) verwerfenGemini = true
            synchronized(jarvisTeile) { jarvisTeile.setLength(0) }
        } else if (live) {
            verwerfenLive = true
            letzteVerworfenMs = System.currentTimeMillis()
            verwerfenSeitMs = letzteVerworfenMs
        } else if (antwortItem.isNotEmpty()) {
            verworfenItem = antwortItem
            val ms = gehoertMs()
            ws?.send(JSONObject().put("type", "response.cancel").toString())
            ws?.send(JSONObject().put("type", "conversation.item.truncate").put("item_id", antwortItem).put("content_index", 0).put("audio_end_ms", ms).toString())
        }
        ausgabeLeeren()
        Log.i(LiveRouting.TAG, "Jarvis per Antippen unterbrochen")
        status("Ich höre zu")
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
            offene.decrementAndGet()
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
        private val offene = java.util.concurrent.atomic.AtomicInteger(0)
        /** Ein Live-Gespräch läuft: Hintergrundarbeit meldet sich dann still, damit kein Hinweiston die Stimme absenkt. */
        val aktiv: Boolean get() = offene.get() > 0

        /** Echosperre bleibt so lange nach dem letzten Ton zu: Wiedergabepuffer plus Bluetooth-Verzögerung. */
        /** Größte Zahl, die OpenAI bei Realtime als max_output_tokens annimmt; darüber nur „inf“. */
        private const val REALTIME_MAX_ZAHL = 4096
        private const val ECHO_NACHLAUF_MS = 400L
        /** Höchstens so lange nach dem letzten hörbaren Ton bleibt die Echosperre zu, egal was die Wiedergabe meldet. */
        private const val ECHO_SPERRE_MAX_MS = 3_000L
        /** GPT-Live: so lange ohne hörbaren Ton (geschrieben und empfangen), dann gilt Jarvis als fertig. */
        private const val STATUS_MAX_MS = 4_000L
        /** Nach dem Antippen wird die alte GPT-Live-Antwort höchstens so lange verworfen. */
        private const val VERWERFEN_MAX_MS = 10_000L
        /** Effektivwert (PCM16), ab dem ein Ausgabestück als hörbarer Ton zählt; Sprache liegt weit darüber. */
        private const val HOERBAR_EFFEKTIV = 120.0
        /** Länge eines Stilleblocks in Sprechpausen (Medienweg); hält den Bluetooth-Medienstrom wach. */
        private const val STILLE_MS = 20
        /**
         * Stille nur nachschieben, wenn weniger als so viel Ton in der Wiedergabe wartet, also kurz vor dem Leerlaufen.
         * Zwei Mischtakte: Der Lautsprecher-Mischer des Fold 8 holt je Takt 40 ms (dumpsys: 1920 Frames bei 48 kHz);
         * mit 40 ms Grenze bekam er in Pausen teils weniger als einen Takt und warf den Strom aus dem Mischer.
         */
        private const val FUELL_UNTER_MS = 80L
        /** Füllgrenze, solange Jarvis spricht: knapp über einem Mischtakt, damit die Reserve gegen Verspätungen groß bleibt. */
        private const val FUELL_SPRACHE_MS = 45L
        /** Mischtakt des Lautsprecher-Ausgangs (Fold 8: 1920 Frames bei 48 kHz); so grob springt die Kopfposition. */
        private const val MISCHTAKT_MS = 40L
        /** Erst nach so langer Stille wird überschüssige Stille gekappt, damit kurze Sprechpausen unangetastet bleiben. */
        private const val KAPPEN_PAUSE_MS = 600L
        /** Takt, in dem der Ton-Faden im Medienweg auf neue Stücke wartet und den Füllstand prüft. */
        private const val WARTE_MS = 10L
        /**
         * Polster gegen Empfangsschwankungen, aufgebaut in Sprechpausen. Gemessen (1.8.107, WLAN): Stücke kamen mitten in
         * der Rede bis gut 200 ms zu spät, 150 ms Polster reichten nicht und es entstanden 40–80 ms Füll-Stille im Satz.
         */
        private const val START_POLSTER_MS = 250L
        /** In langen Pausen wird digitale Stille gekürzt, solange mehr als Polster plus so viel im Puffer liegt. */
        private const val KAPPEN_UEBER_MS = 30L
        /** Größter Ausschlag (PCM16, etwa −44 dBFS), bis zu dem ein Stück als Stille gilt; ein Wortansatz liegt weit darüber. */
        private const val STILLE_SPITZE = 200
        /** So lange ohne hörbaren Ton, dann gilt der nächste hörbare Ton als neuer Satzanfang. */
        private const val NEUER_SATZ_MS = 300L

        /** Die Stimmen der Realtime-API; Marin und Cedar klingen am natürlichsten. */
        val STIMMEN = listOf("marin" to "Marin", "cedar" to "Cedar", "alloy" to "Alloy", "ash" to "Ash", "ballad" to "Ballad",
            "coral" to "Coral", "echo" to "Echo", "sage" to "Sage", "shimmer" to "Shimmer", "verse" to "Verse")
    }
}

/** Einordnung der Gemini-Fehlergründe, gemeinsam für Live-Gespräch und Hörprobe. */
internal object GeminiFehler {
    fun kontingent(reason: String) = reason.contains("quota", true) || reason.contains("RESOURCE_EXHAUSTED", true) || reason.contains("rate limit", true)
    fun schluessel(reason: String) = reason.contains("API key", true) || reason.contains("PERMISSION_DENIED", true) ||
        reason.contains("UNAUTHENTICATED", true) || reason.contains("billing", true)
    /** Modell oder Einstellung abgelehnt: träfe den bezahlten Schlüssel genauso. 1007/1008 = ungültige Anfrage bzw. Richtlinie. */
    fun modell(code: Int, reason: String) = code == 1007 || code == 1008 || reason.contains("not found", true) ||
        reason.contains("not supported", true) || reason.contains("invalid argument", true) || reason.contains("INVALID_ARGUMENT", true)
}
