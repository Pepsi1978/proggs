package de.frank.wecker

import android.content.Context
import androidx.work.*
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.speech.Synthese
import de.frank.genialeideen.tts.EdgeTtsPlayer
import de.frank.genialeideen.tts.TtsProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SpeechPreparation(private val context: Context, private val settings: SecureSettings) {
    private val store = AlarmStore.get(context)
    private fun voiceKey() = listOf(settings.ttsProvider, settings.edgeTtsVoice, settings.googleTtsVoice,
        settings.qwenTtsVoiceId, settings.qwenStandardVoice, settings.ttsSpeechRate.toString(), settings.immerDeutschVorlesen.toString()).joinToString("|")
    val playbackSpeed: Float get() = if (settings.ttsProvider in setOf(TtsProvider.QWEN.id, TtsProvider.QWEN_CLONE.id)) settings.ttsSpeechRate else 1f

    suspend fun prepare(alarm: Alarm, progress: (String) -> Unit = {}) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!alarm.needsSpeech) return@withContext
            try {
                val texts = linkedMapOf<String, List<String>>()
                if (Step.IDEAS in alarm.steps) {
                    progress("Offene Ideen werden abgeglichen …")
                    val ideas = IdeasBridge(context).refresh()
                    texts[Step.IDEAS.name] = ideas.map { "${it.title}.\n${it.text}" }.ifEmpty { listOf("Es sind keine offenen Ideen vorhanden.") }
                }
                if (Step.TEXT in alarm.steps) texts[Step.TEXT.name] = listOf(alarm.text)
                val voice = voiceKey()
                val signature = hash(voice + texts.toString())
                if (alarm.preparedSignature == signature && alarm.prepared.values.flatten().all { File(it).isFile }) {
                    store.update(alarm.id) { it.copy(preparationError = "") }
                    return@withContext
                }
                val result = linkedMapOf<String, List<String>>()
                texts.forEach { (step, entries) ->
                    val parts = entries.flatMap(::chunks)
                    result[step] = parts.mapIndexed { index, text ->
                        ensureActive()
                        progress("${Step.valueOf(step).title}: Abschnitt ${index + 1} von ${parts.size}")
                        audio(text).absolutePath
                    }
                }
                check(voice == voiceKey()) { "Die Stimme wurde während der Vorbereitung geändert. Bitte erneut vorbereiten." }
                // Ein zwischenzeitliches Bearbeiten oder Löschen darf nicht rückgängig gemacht werden.
                store.update(alarm.id) { latest ->
                    if (latest.text == alarm.text && latest.steps == alarm.steps) latest.copy(prepared = result,
                        preparedAt = System.currentTimeMillis(), preparedSpeed = playbackSpeed,
                        preparedSignature = signature, preparationError = "") else latest
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                store.update(alarm.id) { it.copy(preparationError = e.message ?: "Vorbereitung fehlgeschlagen") }
                throw e
            }
        }
    }

    suspend fun audio(text: String): File = withContext(Dispatchers.IO) {
        val path = File(store.files, "speech_${hash(voiceKey() + text)}.audio")
        if (path.length() > 44) return@withContext path
        val temporary = File(store.files, "${path.name}.${java.util.UUID.randomUUID()}.tmp")
        try {
            if (settings.ttsProvider == TtsProvider.EDGE.id) {
                withContext(Dispatchers.Main) {
                    val edge = EdgeTtsPlayer(context)
                    try {
                        withTimeout(120_000) {
                            suspendCancellableCoroutine<Unit> { continuation ->
                                edge.speak(text, settings.edgeTtsVoice, settings.ttsSpeechRate,
                                    erzwingeDeutsch = settings.immerDeutschVorlesen,
                                    onPlaybackStart = {},
                                    onComplete = { if (continuation.isActive) continuation.resume(Unit) },
                                    onError = { if (continuation.isActive) continuation.resumeWithException(it) },
                                    onAudioReady = { it.copyTo(temporary, overwrite = true) })
                                continuation.invokeOnCancellation { edge.stop() }
                            }
                        }
                    } finally { edge.shutdown() }
                }
            } else {
                val synthesizer = Synthese(context, settings)
                val source = synthesizer.synthetisiere(text)
                try { source.copyTo(temporary, overwrite = true) } finally { source.delete() }
            }
            check(temporary.length() > 44) { "Der Sprachdienst hat keine Audiodatei geliefert." }
            check(temporary.renameTo(path)) { "Das Offline-Audio konnte nicht gespeichert werden." }
            path
        } finally { temporary.delete() }
    }
    companion object {
        private val mutex = Mutex()
        fun hash(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        /** Kleine Unicode-Abschnitte bleiben unter den Byte-Limits der Sprachdienste. */
        fun chunks(text: String): List<String> {
            val result = mutableListOf<String>()
            var rest = text.trim()
            while (rest.isNotEmpty()) {
                var end = minOf(rest.length, 900)
                if (end < rest.length) {
                    val separator = rest.lastIndexOfAny(charArrayOf('.', '!', '?', '\n', ' '), end - 1)
                    if (separator > end / 2) end = separator + 1
                    if (end > 0 && rest[end - 1].isHighSurrogate()) end--
                }
                result += rest.substring(0, end).trim()
                rest = rest.substring(end).trimStart()
            }
            return result.filter(String::isNotBlank)
        }
    }
}

class PreparationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        var failed = false
        SecureSettings(applicationContext).use { settings ->
            val preparation = SpeechPreparation(applicationContext, settings)
            AlarmStore.get(applicationContext).all().filter { it.enabled && it.needsSpeech }.forEach { alarm ->
                try { preparation.prepare(alarm) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { failed = true }
            }
        }
        return if (failed && runAttemptCount < 3) Result.retry() else Result.success()
    }
    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("speech-refresh", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PreparationWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        }
        fun periodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("speech-periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PreparationWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        }
    }
}
