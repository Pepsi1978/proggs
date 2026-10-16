package de.frank.jarvis.live

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Base64
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
 * Live-Gespräch über die OpenAI-Realtime-API (WebSocket, PCM16 mit 24 kHz in beide Richtungen).
 * Das Modell hört dauernd zu, auch während es spricht: Beginnt Frank zu reden, verstummt die Ausgabe sofort
 * (Server-Sprachaktivität mit `interrupt_response`). Die Echo-Unterdrückung des Handys verhindert, dass es sich selbst hört.
 * Für alles, was Franks Daten oder Aktionen braucht, ruft das Modell das Werkzeug `jarvis_fragen` auf; das beantwortet
 * der normale Jarvis-Agent mit allen Werkzeugen.
 */
class LiveGespraech(
    context: Context,
    private val schluessel: String,
    private val modell: String,
    private val stimme: String,
    private val anweisung: String,
    private val beiStatus: (String) -> Unit,
    private val beiPegel: (Float) -> Unit,
    private val beiNutzerText: (String) -> Unit,
    private val beiJarvisText: (String) -> Unit,
    private val beiWerkzeug: (callId: String, auftrag: String) -> Unit,
    private val beiFehler: (String) -> Unit,
    private val beiEnde: () -> Unit,
) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    private var ws: WebSocket? = null
    @Volatile private var laeuft = false
    @Volatile var stumm = false
    @Volatile private var sprichtGerade = false

    private var aufnahme: AudioRecord? = null
    private var echo: AcousticEchoCanceler? = null
    private var rauschen: NoiseSuppressor? = null
    private var wiedergabe: AudioTrack? = null
    private val ausgabe = LinkedBlockingQueue<ByteArray>()
    private var alterModus = AudioManager.MODE_NORMAL

    fun start() {
        laeuft = true
        beiStatus("Verbinde …")
        val anfrage = Request.Builder()
            .url("wss://api.openai.com/v1/realtime?model=$modell")
            .header("Authorization", "Bearer $schluessel")
            .build()
        ws = client.newWebSocket(anfrage, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(sitzung().toString())
                // Die Mitschrift kommt getrennt: Kennt der Dienst das Mitschrift-Modell nicht, scheitert nur sie, nicht das Gespräch.
                webSocket.send(JSONObject().put("type", "session.update").put("session", JSONObject().put("type", "realtime").put("audio",
                    JSONObject().put("input", JSONObject().put("transcription", JSONObject().put("model", "gpt-4o-mini-transcribe").put("language", "de"))))).toString())
                tonStarten()
                if (laeuft) beiStatus("Ich höre zu")
            }

            override fun onMessage(webSocket: WebSocket, text: String) = ereignis(text)

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!laeuft) return
                val grund = when (response?.code) {
                    401 -> "Der OpenAI-Schlüssel wurde abgelehnt."
                    403 -> "Der Schlüssel darf die Realtime-API nicht nutzen."
                    404 -> "Das Live-Modell „$modell“ gibt es nicht."
                    429 -> "Kein Guthaben oder zu viele Anfragen bei OpenAI."
                    else -> "Live-Verbindung getrennt: " + (t.message ?: "unbekannt")
                }
                beiFehler(grund)
                beenden()
            }

            // Schließt der Dienst die Verbindung (etwa am Sitzungsende), meldet OkHttp nur onClosing; onClosed käme erst nach
            // unserem eigenen close(). Ohne das hier liefe das Mikrofon weiter in eine tote Verbindung.
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { if (laeuft) beenden() }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { if (laeuft) beenden() }
        })
    }

    private fun sitzung(): JSONObject {
        val werkzeug = JSONObject()
            .put("type", "function")
            .put("name", "jarvis_fragen")
            .put("description", "Fragt Jarvis mit allen Werkzeugen und Franks Daten: Termine, Dienstplan, Aufgaben, Wecker, Biodaten, Ideen, Tagebuch, Mails, Wetter, " +
                "Gedächtnis, Programmieraufträge, Timer und alles, was etwas nachschlägt oder ändert. Gib den Auftrag vollständig in Franks Worten weiter.")
            .put("parameters", JSONObject().put("type", "object")
                .put("properties", JSONObject().put("auftrag", JSONObject().put("type", "string").put("description", "Die vollständige Frage oder der Auftrag")))
                .put("required", JSONArray().put("auftrag")))
        val format = JSONObject().put("type", "audio/pcm").put("rate", 24000)
        return JSONObject().put("type", "session.update").put("session", JSONObject()
            .put("type", "realtime")
            .put("model", modell)
            .put("output_modalities", JSONArray().put("audio"))
            .put("instructions", anweisung)
            .put("audio", JSONObject()
                .put("input", JSONObject()
                    .put("format", format)
                    .put("turn_detection", JSONObject().put("type", "server_vad").put("silence_duration_ms", 550).put("create_response", true).put("interrupt_response", true)))
                .put("output", JSONObject().put("format", format).put("voice", stimme)))
            .put("tools", JSONArray().put(werkzeug))
            .put("tool_choice", "auto"))
    }

    private val jarvisTeile = StringBuilder()

    private fun ereignis(text: String) {
        val e = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (e.optString("type")) {
            "response.output_audio.delta", "response.audio.delta" -> {
                val daten = Base64.decode(e.optString("delta"), Base64.DEFAULT)
                if (!sprichtGerade) { sprichtGerade = true; beiStatus("Jarvis spricht") }
                ausgabe.offer(daten)
            }
            "input_audio_buffer.speech_started" -> {
                // Frank redet dazwischen: sofort verstummen, der Dienst bricht die laufende Antwort selbst ab.
                ausgabeLeeren()
                beiStatus("Ich höre zu")
            }
            "conversation.item.input_audio_transcription.completed" -> e.optString("transcript").trim().takeIf { it.isNotEmpty() }?.let(beiNutzerText)
            "response.output_audio_transcript.delta", "response.audio_transcript.delta" -> jarvisTeile.append(e.optString("delta"))
            "response.output_audio_transcript.done", "response.audio_transcript.done" -> {
                val satz = e.optString("transcript").ifBlank { jarvisTeile.toString() }.trim()
                jarvisTeile.setLength(0)
                if (satz.isNotEmpty()) beiJarvisText(satz)
            }
            "response.done" -> {
                sprichtGerade = false
                // Endet die Antwort mit einem Werkzeugaufruf, schaut Jarvis noch nach: „Jarvis schaut nach …“ bleibt stehen.
                val ausgabe = e.optJSONObject("response")?.optJSONArray("output")
                val ruftWerkzeug = ausgabe != null && (0 until ausgabe.length()).any { ausgabe.optJSONObject(it)?.optString("type") == "function_call" }
                if (laeuft && !ruftWerkzeug) beiStatus("Ich höre zu")
            }
            "response.function_call_arguments.done" -> {
                // Nicht jede Fassung des Ereignisses nennt den Namen; es gibt ohnehin nur dieses eine Werkzeug.
                val name = e.optString("name")
                if (name.isNotEmpty() && name != "jarvis_fragen") return
                val auftrag = runCatching { JSONObject(e.optString("arguments")).optString("auftrag") }.getOrNull().orEmpty()
                beiStatus("Jarvis schaut nach …")
                beiWerkzeug(e.optString("call_id"), auftrag)
            }
            "error" -> {
                val f = e.optJSONObject("error")
                val meldung = f?.optString("message").orEmpty()
                // Abgebrochene Antworten (Frank hat dazwischengeredet) sind kein Fehler.
                if (f?.optString("code") == "response_cancel_not_active") return
                // Hat Frank während des Nachschlagens gesprochen, läuft schon eine Antwort; das eigene response.create entfällt dann.
                if (f?.optString("code") == "conversation_already_has_active_response") return
                if (meldung.contains("transcription", ignoreCase = true)) return
                beiFehler("Live: " + meldung.ifBlank { "unbekannter Fehler" })
            }
        }
    }

    /** Das Ergebnis von `jarvis_fragen` zurück an das Modell; danach spricht es die Antwort. */
    fun werkzeugErgebnis(callId: String, ergebnis: String) {
        val w = ws ?: return
        w.send(JSONObject().put("type", "conversation.item.create").put("item", JSONObject()
            .put("type", "function_call_output").put("call_id", callId).put("output", ergebnis)).toString())
        w.send(JSONObject().put("type", "response.create").toString())
    }

    @SuppressLint("MissingPermission")
    private fun tonStarten() {
        alterModus = audio.mode
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        lautsprecher(true)

        val rate = 24000
        val minIn = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minIn, 9600) * 2)
        aufnahme = rec
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            beiFehler("Das Mikrofon ließ sich für das Live-Gespräch nicht öffnen.")
            beenden()
            return
        }
        if (AcousticEchoCanceler.isAvailable()) echo = AcousticEchoCanceler.create(rec.audioSessionId)?.also { it.enabled = true }
        if (NoiseSuppressor.isAvailable()) rauschen = NoiseSuppressor.create(rec.audioSessionId)?.also { it.enabled = true }

        val minOut = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(minOut * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        wiedergabe = track
        track.play()

        // Fehler in den eigenen Fäden enden als Meldung, nie als Absturz der App.
        Thread({
            val puffer = ByteArray(1920) // 40 ms
            val ok = runCatching { rec.startRecording() }.isSuccess && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING
            if (!ok) { if (laeuft) { beiFehler("Das Mikrofon ließ sich für das Live-Gespräch nicht starten."); beenden() }; return@Thread }
            while (laeuft) {
                val n = runCatching { rec.read(puffer, 0, puffer.size) }.getOrDefault(AudioRecord.ERROR_INVALID_OPERATION)
                if (n == 0) continue
                if (n < 0) { if (laeuft) { beiFehler("Das Mikrofon ist ausgefallen."); beenden() }; break }
                var summe = 0.0
                var i = 0
                while (i + 1 < n) { val s = (puffer[i].toInt() and 0xFF) or (puffer[i + 1].toInt() shl 8); summe += s.toDouble() * s; i += 2 }
                beiPegel((kotlin.math.sqrt(summe / (n / 2)) / 6000.0).toFloat().coerceIn(0f, 1f))
                val stueck = if (stumm) ByteArray(n) else puffer.copyOf(n)
                ws?.send(JSONObject().put("type", "input_audio_buffer.append").put("audio", Base64.encodeToString(stueck, Base64.NO_WRAP)).toString())
            }
        }, "jarvis-live-mikro").start()

        Thread({
            while (laeuft) {
                val stueck = runCatching { ausgabe.poll(200, TimeUnit.MILLISECONDS) }.getOrNull() ?: continue
                runCatching { wiedergabe?.write(stueck, 0, stueck.size) }
            }
        }, "jarvis-live-ton").start()
    }

    private fun ausgabeLeeren() {
        ausgabe.clear()
        sprichtGerade = false
        wiedergabe?.let { runCatching { it.pause(); it.flush(); it.play() } }
    }

    private fun lautsprecher(an: Boolean) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                if (an) audio.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.let { audio.setCommunicationDevice(it) }
                else audio.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audio.isSpeakerphoneOn = an
            }
        }
    }

    @Synchronized
    fun beenden() {
        if (!laeuft) return
        laeuft = false
        runCatching { ws?.close(1000, "Ende") }
        ws = null
        runCatching { aufnahme?.stop() }
        runCatching { aufnahme?.release() }
        aufnahme = null
        runCatching { echo?.release(); rauschen?.release() }
        ausgabe.clear()
        runCatching { wiedergabe?.stop() }
        runCatching { wiedergabe?.release() }
        wiedergabe = null
        lautsprecher(false)
        runCatching { audio.mode = alterModus }
        client.dispatcher.executorService.shutdown()
        beiEnde()
    }

    companion object {
        /** Die Stimmen der Realtime-API; Marin und Cedar klingen am natürlichsten. */
        val STIMMEN = listOf("marin" to "Marin", "cedar" to "Cedar", "alloy" to "Alloy", "ash" to "Ash", "ballad" to "Ballad",
            "coral" to "Coral", "echo" to "Echo", "sage" to "Sage", "shimmer" to "Shimmer", "verse" to "Verse")
    }
}
