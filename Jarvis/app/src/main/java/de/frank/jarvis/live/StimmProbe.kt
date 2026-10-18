package de.frank.jarvis.live

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Hörproben der Live-Stimmen auf Deutsch. Eine Probe entsteht einmalig über OpenAI (GPT-Live: etwa 10 Sekunden, unter
 * 1 Cent; Realtime: über das günstige mini-Modell) oder Google (Gemini 3.8 Live, etwa 0,3 Cent, mit kostenlosem Schlüssel gratis)
 * und liegt danach unter `files/stimmproben/`; jedes weitere Anhören ist gratis.
 *
 * [art]: `live` (GPT-Live), `realtime` oder `gemini`.
 */
object StimmProbe {
    const val SATZ = "Hallo Frank, ich bin Jarvis. So klinge ich auf Deutsch. Was steht heute bei dir an?"

    private fun datei(context: Context, art: String, stimme: String) =
        File(File(context.filesDir, "stimmproben").apply { mkdirs() }, art + "_" + stimme + ".pcm")

    fun vorhanden(context: Context, art: String, stimme: String) = datei(context, art, stimme).let { it.exists() && it.length() > 4800 }

    /** Favoriten-Stimmen (langes Drücken) je Art als `art_stimme`; sie stehen in der Liste oben mit Stern. */
    fun favoriten(context: Context): Set<String> =
        context.getSharedPreferences("stimmproben", Context.MODE_PRIVATE).getStringSet("favoriten", emptySet())!!.toSet()

    /** Schlüssel einer Stimme in [favoriten]. */
    fun favoritSchluessel(art: String, stimme: String) = art + "_" + stimme

    fun favoritUmschalten(context: Context, art: String, stimme: String): Set<String> {
        val schluessel = favoritSchluessel(art, stimme)
        val neu = favoriten(context).let { if (schluessel in it) it - schluessel else it + schluessel }
        context.getSharedPreferences("stimmproben", Context.MODE_PRIVATE).edit().putStringSet("favoriten", neu).apply()
        return neu
    }

    /** Welche Hörproben-Art zum eingestellten Live-Modell gehört. */
    fun art(st: LiveOptionen.Stand) = if (st.istGemini) "gemini" else if (st.istLive) "live" else "realtime"

    /** Anfang der Meldung, wenn Gemini Modell oder Stimme ablehnt; dann hilft der bezahlte Schlüssel nicht. */
    private const val MODELL_FEHLER = "Hörprobe: Gemini lehnt Modell oder Stimme ab"

    @Volatile private var laufend: AudioTrack? = null

    /** Spielt die Probe ab; fehlt sie, wird sie zuerst erzeugt. Liefert eine Fehlermeldung oder null. */
    suspend fun abspielen(context: Context, schluessel: String, art: String, stimme: String, ersatz: String = ""): String? = withContext(Dispatchers.IO) {
        val f = datei(context, art, stimme)
        if (!vorhanden(context, art, stimme)) {
            if (schluessel.isBlank()) return@withContext if (art == "gemini") "Für Hörproben bitte zuerst einen Gemini-Schlüssel eintragen." else "Für Hörproben bitte zuerst den OpenAI-API-Schlüssel eintragen."
            var (pcm, fehler) = if (art == "gemini") erzeugenGemini(schluessel, stimme) else erzeugen(schluessel, art == "live", stimme)
            // Kostenloser Schlüssel gestört (Kontingent, abgelehnt, nicht erreichbar, keine Antwort): einmal mit dem bezahlten.
            // Nur Modell- oder Einstellungsfehler nicht, die träfen ihn genauso.
            if (pcm == null && art == "gemini" && ersatz.isNotBlank() && ersatz != schluessel && fehler?.startsWith(MODELL_FEHLER) != true) {
                val zweiter = erzeugenGemini(ersatz, stimme)
                pcm = zweiter.first; fehler = zweiter.second
            }
            if (pcm == null) return@withContext fehler ?: "Die Hörprobe kam nicht zustande."
            f.writeBytes(pcm)
        }
        spielen(f.readBytes())
        null
    }

    @Synchronized fun anhalten() { laufend?.let { runCatching { it.pause(); it.flush(); it.release() } }; laufend = null }

    /** Gibt die Probe nach ihrem Ende frei, aber nur, wenn inzwischen keine neue begonnen hat. */
    @Synchronized private fun freigeben(track: AudioTrack) { if (laufend === track) anhalten() }

    @Synchronized private fun spielen(pcm: ByteArray) {
        anhalten()
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(24000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(pcm.size.coerceAtLeast(4800))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        laufend = track
        track.play()
        // 24 kHz, 16 Bit mono = 48 Byte je Millisekunde; mit Reserve nach dem Ende freigeben, sonst bleibt der AudioTrack liegen.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ freigeben(track) }, pcm.size / 48L + 500)
    }

    /** Gemini: kurze Live-Sitzung mit 3.8 Live, Text hinein, Audio (24 kHz) bis turnComplete sammeln. */
    private suspend fun erzeugenGemini(schluessel: String, stimme: String): Pair<ByteArray?, String?> {
        // Kurzer Verbindungsaufbau: Ist der kostenlose Schlüssel nicht erreichbar, bleibt Zeit für den bezahlten.
        val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).connectTimeout(8, TimeUnit.SECONDS).build()
        val audio = ByteArrayOutputStream()
        val fertig = CompletableDeferred<String?>()
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=" +
            java.net.URLEncoder.encode(schluessel, "UTF-8")
        val ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("setup", JSONObject()
                    .put("model", "models/gemini-3.8-live")
                    .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("AUDIO"))
                        .put("speechConfig", JSONObject().put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", stimme)))))
                    .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                        "Du sprichst ausschließlich Deutsch. Sprich genau den Satz, den du bekommst, natürlich und freundlich, und danach nichts mehr. RESPOND IN GERMAN."))))).toString())
            }
            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) = onMessage(webSocket, bytes.utf8())
            override fun onMessage(webSocket: WebSocket, text: String) {
                val e = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (e.has("setupComplete")) webSocket.send(JSONObject().put("realtimeInput", JSONObject().put("text", "Sag jetzt genau diesen Satz: „$SATZ“")).toString())
                val c = e.optJSONObject("serverContent") ?: return
                val teile = c.optJSONObject("modelTurn")?.optJSONArray("parts")
                if (teile != null) for (i in 0 until teile.length()) {
                    teile.optJSONObject(i)?.optJSONObject("inlineData")?.optString("data")?.takeIf { it.isNotEmpty() }
                        ?.let { synchronized(audio) { audio.write(Base64.decode(it, Base64.DEFAULT)) } }
                }
                if (c.optBoolean("turnComplete")) fertig.complete(null)
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                fertig.complete(if (code == 1000) null else when {
                    GeminiFehler.kontingent(reason) -> "Gemini-Kontingent erschöpft: Hörprobe gerade nicht möglich."
                    GeminiFehler.schluessel(reason) -> "Der Gemini-Schlüssel wurde abgelehnt."
                    GeminiFehler.modell(code, reason) -> "$MODELL_FEHLER ($code): " + reason.take(160)
                    else -> "Hörprobe (Gemini $code): " + reason.take(160)
                })
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                fertig.complete(when (response?.code) {
                    401, 403 -> "Der Gemini-Schlüssel wurde abgelehnt."
                    429 -> "Gemini-Kontingent erschöpft: Hörprobe gerade nicht möglich."
                    400, 404 -> "$MODELL_FEHLER (HTTP ${response.code})."
                    else -> "Hörprobe: Verbindung zu Gemini fehlgeschlagen (" + (t.message ?: "unbekannt") + ")."
                })
            }
        })
        val fehler = withTimeoutOrNull(25_000) { fertig.await() }
        runCatching { ws.close(1000, "Probe fertig") }
        client.dispatcher.executorService.shutdown()
        val pcm = synchronized(audio) { audio.toByteArray() }
        return if (pcm.size > 4800) pcm.copyOf(pcm.size - pcm.size % 2) to null else null to (fehler ?: "Die Stimme hat nichts gesagt. Bitte nochmal versuchen.")
    }

    /** Baut eine kurze Sitzung auf, lässt den Satz sprechen und sammelt das Audio. */
    private suspend fun erzeugen(schluessel: String, live: Boolean, stimme: String): Pair<ByteArray?, String?> {
        val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
        val audio = ByteArrayOutputStream()
        val fertig = CompletableDeferred<String?>()
        val letztesAudio = java.util.concurrent.atomic.AtomicLong(0)
        val url = if (live) "wss://api.openai.com/v1/live/sessions" else "wss://api.openai.com/v1/realtime?model=gpt-realtime-2.1-mini"
        val ws = client.newWebSocket(Request.Builder().url(url).header("Authorization", "Bearer $schluessel").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (live) {
                    webSocket.send(JSONObject().put("type", "session.start").put("session", JSONObject()
                        .put("model", "gpt-live-1")
                        .put("instructions", "Du sprichst ausschließlich Deutsch. Sprich genau den Satz aus der Entwickler-Nachricht, natürlich und freundlich, und danach nichts mehr.")
                        .put("audio", JSONObject().put("format", JSONObject().put("type", "audio/pcm").put("rate", 24000)).put("output", JSONObject().put("voice", stimme)))
                        .put("input", JSONArray().put(JSONObject().put("type", "message").put("role", "developer")
                            .put("content", JSONArray().put(JSONObject().put("type", "input_text").put("text", "Sag jetzt genau diesen Satz: „$SATZ“")))))
                        .put("delegation", JSONObject().put("type", "client"))).toString())
                } else {
                    webSocket.send(JSONObject().put("type", "session.update").put("session", JSONObject()
                        .put("type", "realtime").put("output_modalities", JSONArray().put("audio"))
                        .put("instructions", "Du sprichst ausschließlich Deutsch.")
                        .put("audio", JSONObject().put("input", JSONObject().put("turn_detection", JSONObject.NULL))
                            .put("output", JSONObject().put("format", JSONObject().put("type", "audio/pcm").put("rate", 24000)).put("voice", stimme)))).toString())
                    webSocket.send(JSONObject().put("type", "response.create").put("response", JSONObject()
                        .put("instructions", "Sprich genau diesen Satz auf Deutsch und nichts weiter: „$SATZ“")).toString())
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val e = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (e.optString("type")) {
                    "session.started" -> {
                        // GPT-Live erwartet laufendes Audio: Stille schicken, bis die Probe gesprochen ist.
                        Thread({
                            val stille = Base64.encodeToString(ByteArray(4800), Base64.NO_WRAP)
                            while (!fertig.isCompleted) {
                                runCatching { webSocket.send(JSONObject().put("type", "session.input_audio.append").put("audio", stille).toString()) }
                                Thread.sleep(100)
                            }
                        }, "jarvis-stimmprobe-stille").start()
                    }
                    "session.output_audio.delta", "response.output_audio.delta" -> {
                        val daten = Base64.decode(e.optString("delta"), Base64.DEFAULT)
                        synchronized(audio) { audio.write(daten) }
                        // GPT-Live schickt auch nach dem Satz durchgehend Stille: Nur hörbarer Ton zählt, sonst griffe die
                        // 1,5-Sekunden-Regel nie und jede Probe liefe bis zum Zeitlimit (doppelte Kosten, 10 s Stille in der Datei).
                        if (hoerbar(daten)) letztesAudio.set(System.currentTimeMillis())
                    }
                    "response.done" -> fertig.complete(null)
                    "error" -> {
                        val f = e.optJSONObject("error")
                        val code = f?.optString("code").orEmpty()
                        fertig.complete(when {
                            code.contains("insufficient", true) || code.contains("quota", true) || code.contains("credit", true) -> "Kein Guthaben bei OpenAI: Hörproben gehen erst nach dem Aufladen."
                            else -> "Hörprobe: " + (f?.optString("message")?.ifBlank { null } ?: code.ifBlank { "unbekannter Fehler" })
                        })
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                fertig.complete(when (response?.code) {
                    401 -> "Der OpenAI-Schlüssel wurde abgelehnt."
                    429 -> "Kein Guthaben oder zu viele Anfragen bei OpenAI: Hörproben gehen erst nach dem Aufladen."
                    else -> "Hörprobe: Verbindung fehlgeschlagen (" + (t.message ?: "unbekannt") + ")."
                })
            }
        })
        // GPT-Live meldet kein Ende der Rede: fertig, wenn 1,5 Sekunden nach dem letzten Audio nichts mehr kommt.
        val fehler = withTimeoutOrNull(20_000) {
            while (!fertig.isCompleted) {
                kotlinx.coroutines.delay(200)
                if (letztesAudio.get() > 0 && System.currentTimeMillis() - letztesAudio.get() > 1_500) fertig.complete(null)
            }
            fertig.await()
        }
        if (live) runCatching { ws.send(JSONObject().put("type", "session.close").toString()) }
        kotlinx.coroutines.delay(300)
        runCatching { ws.close(1000, "Probe fertig") }
        client.dispatcher.executorService.shutdown()
        if (!fertig.isCompleted) fertig.complete(null)
        val pcm = synchronized(audio) { audio.toByteArray() }
        return if (pcm.size > 4800) pcm.copyOf(pcm.size - pcm.size % 2) to null else null to (fehler ?: "Die Stimme hat nichts gesagt. Bitte nochmal versuchen.")
    }

    /** Echter Ton statt (fast) Stille: Effektivwert (PCM16) über derselben Schwelle wie im Live-Gespräch. */
    private fun hoerbar(b: ByteArray): Boolean {
        val n = b.size - b.size % 2
        if (n < 2) return false
        var summe = 0.0
        var i = 0
        while (i + 1 < n) { val s = (b[i].toInt() and 0xFF) or (b[i + 1].toInt() shl 8); summe += s.toDouble() * s; i += 2 }
        return kotlin.math.sqrt(summe / (n / 2)) > 120.0
    }
}
