package de.frank.denknotiz.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class MicRecorder(private val context: Context) {
    private val bytes = ByteArrayOutputStream()
    private val recording = AtomicBoolean(false)
    private var recorder: AudioRecord? = null
    private var job: Job? = null
    private var activeSampleRate = SAMPLE_RATE

    fun start(scope: CoroutineScope, requestedSampleRate: Int = SAMPLE_RATE): Boolean {
        if (recording.get()) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false
        val sampleRate = listOf(requestedSampleRate, SAMPLE_RATE).distinct().firstOrNull {
            AudioRecord.getMinBufferSize(it, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) > 0
        } ?: return false
        activeSampleRate = sampleRate
        val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return false
        val next = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, min * 2)
        } catch (_: Exception) { return false }
        if (next.state != AudioRecord.STATE_INITIALIZED) { next.release(); return false }
        bytes.reset()
        try { next.startRecording() } catch (_: Exception) { next.release(); return false }
        recorder = next
        recording.set(true)
        job = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(min)
            while (isActive && recording.get()) {
                val read = next.read(buffer, 0, buffer.size)
                if (read <= 0) break
                if (bytes.size() + read > MAX_BYTES) { recording.set(false); break }
                bytes.write(buffer, 0, read)
            }
        }
        return true
    }

    suspend fun stop(): ByteArray? {
        if (!recording.getAndSet(false) && recorder == null) return null
        val active = recorder; recorder = null
        runCatching { active?.stop() }
        job?.cancelAndJoin(); job = null
        runCatching { active?.release() }
        val pcm = bytes.toByteArray(); bytes.reset()
        return pcm.takeIf(ByteArray::isNotEmpty)?.let(::wav)
    }

    fun release() {
        recording.set(false); job?.cancel(); job = null
        val active = recorder; recorder = null
        runCatching { active?.stop() }; runCatching { active?.release() }
    }

    fun isRecording() = recording.get()

    private fun wav(pcm: ByteArray): ByteArray {
        val rate = activeSampleRate
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".encodeToByteArray()); putInt(pcm.size + 36); put("WAVE".encodeToByteArray())
            put("fmt ".encodeToByteArray()); putInt(16); putShort(1); putShort(1); putInt(rate)
            putInt(rate * 2); putShort(2); putShort(16); put("data".encodeToByteArray()); putInt(pcm.size)
        }.array()
        return header + pcm
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CLONING_SAMPLE_RATE = 24_000
        private const val MAX_BYTES = CLONING_SAMPLE_RATE * 2 * 60 * 10
    }
}

data class SpeechAnalysis(val voicedMs: Int, private val frames: BooleanArray) {
    fun hasSpeech(start: Double, end: Double): Boolean {
        if (frames.isEmpty() || end <= start) return true
        val first = (start * 50).toInt().coerceIn(0, frames.lastIndex)
        val last = (end * 50).toInt().coerceIn(first, frames.lastIndex)
        return (first..last).count { frames[it] }.toDouble() / (last - first + 1) >= 0.10
    }
}

object SpeechAnalyzer {
    fun analyze(wav: ByteArray): SpeechAnalysis? = runCatching { decode(wav) }.getOrNull()

    private fun decode(wav: ByteArray): SpeechAnalysis? {
        if (wav.size <= 48) return null
        val frameBytes = 320 * 2
        val count = (wav.size - 44) / frameBytes
        if (count <= 0) return null
        val frames = BooleanArray(count)
        for (frame in 0 until count) {
            var sum = 0.0
            for (sample in 0 until 320) {
                val index = 44 + frame * frameBytes + sample * 2
                val value = ((wav[index].toInt() and 0xff) or (wav[index + 1].toInt() shl 8)).toShort() / 32768.0
                sum += value * value
            }
            frames[frame] = sqrt(sum / 320) >= 0.015
        }
        return SpeechAnalysis(frames.count { it } * 20, frames)
    }
}

data class Segment(val start: Double?, val end: Double?, val text: String, val noSpeech: Double?, val logProb: Double?, val compression: Double?)

object WhisperHallucinationFilter {
    fun filter(topText: String, segments: List<Segment>, analysis: SpeechAnalysis?): String {
        if (segments.isEmpty()) return blockFloskel(topText.trim(), analysis)
        val confident = segments.filterNot {
            ((it.noSpeech ?: 0.0) > 0.6 && (it.logProb ?: 0.0) < -1.0) ||
                (it.compression ?: 0.0) > 2.4 ||
                (((it.end ?: 0.0) - (it.start ?: 0.0)) in 0.0001..0.3999 && (it.noSpeech ?: 0.0) > 0.6)
        }
        if (confident.isEmpty()) return ""
        val aligned = analysis?.let { value -> confident.filter { segment ->
            val start = segment.start; val end = segment.end
            start == null || end == null || value.hasSpeech(start, end)
        } }.orEmpty()
        val kept = if (analysis != null) aligned else confident
        return blockFloskel(kept.joinToString(" ") { it.text.trim() }.trim(), analysis)
    }

    private fun blockFloskel(text: String, analysis: SpeechAnalysis?): String {
        if (analysis == null || analysis.voicedMs >= 600 || text.length > 64) return text
        val normalized = text.lowercase().map { if (it.isLetter()) it else ' ' }.joinToString("")
            .split(' ').filter(String::isNotBlank).joinToString(" ")
        return if (normalized in BLOCKLIST) "" else text
    }

    private val BLOCKLIST = setOf("vielen dank", "vielen dank fürs zuschauen", "vielen dank für ihre aufmerksamkeit",
        "bis zum nächsten mal", "untertitel", "untertitel des zdf", "untertitel der amara org community",
        "thank you", "thank you for watching", "thanks for watching", "please subscribe")
}

data class TranscriptionResult(val text: String, val fullyFiltered: Boolean)

class GroqTranscriber(private val apiKey: String) {
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS).build()

    suspend fun transcribe(wav: ByteArray): TranscriptionResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Groq-Schlüssel fehlt." }
        val analysis = SpeechAnalyzer.analyze(wav)
        if (analysis != null && analysis.voicedMs < 150) return@withContext TranscriptionResult("", true)
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", wav.toRequestBody("audio/wav".toMediaType()))
            .addFormDataPart("model", "whisper-large-v3-turbo").addFormDataPart("language", "de")
            .addFormDataPart("response_format", "verbose_json").addFormDataPart("temperature", "0").build()
        val request = Request.Builder().url("https://api.groq.com/openai/v1/audio/transcriptions")
            .header("Authorization", "Bearer ${apiKey.trim()}").post(multipart).build()
        client.newCall(request).awaitAudio().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Groq-Fehler ${response.code}: ${JSONObject(raw).optJSONObject("error")?.optString("message").orEmpty()}")
            val json = JSONObject(raw)
            val array = json.optJSONArray("segments")
            val segments = if (array == null) emptyList() else (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { Segment(it.number("start"), it.number("end"), it.optString("text"),
                    it.number("no_speech_prob"), it.number("avg_logprob"), it.number("compression_ratio")) }
            }
            val text = WhisperHallucinationFilter.filter(json.optString("text"), segments, analysis)
            TranscriptionResult(text, text.isBlank())
        }
    }
}

private fun JSONObject.number(name: String): Double? = optDouble(name).takeUnless(Double::isNaN)
private suspend fun Call.awaitAudio(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) { _, value, _ -> value.close() } else response.close()
        }
    })
}
