package de.frank.wecker

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.work.*
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.speech.Synthese
import de.frank.genialeideen.speech.SyntheseStimme
import de.frank.genialeideen.tts.EdgeTtsPlayer
import de.frank.genialeideen.tts.TtsProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SpeechPreparation(private val context: Context, private val settings: SecureSettings,
    private val voiceFactory: () -> SyntheseStimme = { SyntheseStimme(settings) }) {
    private val store = AlarmStore.get(context)
    private fun voiceKey(voice: SyntheseStimme) = listOf("paragraph-variants-v2", voice.ttsProvider, voice.edgeTtsVoice,
        voice.googleTtsVoice, voice.qwenTtsVoiceId, voice.qwenStandardVoice, voice.ttsSpeechRate.toString(),
        voice.immerDeutschVorlesen.toString(), when (voice.ttsProvider) {
            TtsProvider.GOOGLE_CLOUD.id -> hash(voice.googleTtsApiKey)
            TtsProvider.QWEN.id, TtsProvider.QWEN_CLONE.id -> hash(voice.qwenTtsApiKey)
            else -> ""
        }).joinToString("|")
    val playbackSpeed: Float get() = voiceFactory().playbackSpeed

    suspend fun prepare(alarm: Alarm, progress: suspend (String) -> Unit = {}) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!alarm.needsSpeech) return@withContext
            try {
                val groups = mutableListOf<SpeechGroup>()
                if (Step.IDEAS in alarm.steps) {
                    progress("Offene Ideen werden abgeglichen …")
                    val ideas = IdeasBridge(context).refresh()
                    groups += ideas.map { SpeechGroup(Step.IDEAS.name, chunks("${it.title}.\n${it.text}")) }
                        .ifEmpty { listOf(SpeechGroup(Step.IDEAS.name, listOf("Es sind keine offenen Ideen vorhanden."))) }
                }
                if (Step.TEXT in alarm.steps) groups += SpeechGroup(Step.TEXT.name, chunks(alarm.text))
                val voice = alarm.resolveVoice(voiceFactory())
                val signature = hash(voiceKey(voice) + JSONArray(groups.map { JSONObject().put("step", it.step).put("paragraphs", JSONArray(it.paragraphs)) }).toString())
                val latest = store.get(alarm.id) ?: return@withContext
                if (!latest.sameSpeechAs(alarm)) return@withContext
                val cached = latest.voiceVariants
                // Alte Varianten besitzen noch keine Ideengrenzen. In diesem Fall die Gruppen
                // erneut zusammensetzen; render() verwendet vorhandene Audiodateien weiter.
                val ideaCount = groups.count { it.step == Step.IDEAS.name && it.paragraphs.isNotEmpty() }
                if (latest.preparedSignature == signature && cached.size == VoiceVariations.COUNT &&
                    cached.all { variant -> variant.steps[Step.IDEAS.name].orEmpty().count { it.endOfIdea } == ideaCount } &&
                    cached.flatMap { it.steps.values.flatten() }.all { File(it.path).length() > 44 } &&
                    (cached.none { it.steps.values.flatten().any(PreparedAudio::fallback) } || System.currentTimeMillis() - latest.preparedAt < 15 * 60_000)) {
                    if (cached.none { it.steps.values.flatten().any(PreparedAudio::fallback) }) store.update(alarm.id) { current ->
                        if (current.sameSpeechAs(alarm)) current.copy(preparationError = "") else current
                    }
                    return@withContext
                }
                var primaryFailed = false
                var primaryReason = ""
                store.update(alarm.id) { current ->
                    if (current.sameSpeechAs(alarm)) current.copy(preparationError = "Audio-Vorbereitung läuft …") else current
                }
                val variants = VoiceVariations.buildGroups(groups, render = { text, index ->
                    ensureActive()
                    val chosen = voice.withRate(VoiceVariations.rate(voice.ttsSpeechRate, index))
                    if (!primaryFailed) {
                        try {
                            render(text, chosen, index)
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) {
                            if (chosen.ttsProvider == TtsProvider.EDGE.id) throw e
                            primaryFailed = true; primaryReason = e.message ?: "Gewählte Stimme nicht erreichbar"
                            android.util.Log.w("WeckerTts", "Gewählte Stimme fehlgeschlagen; Edge-Notfallstimme wird vorbereitet", e)
                            render(text, chosen.edgeFallback(), index).copy(fallback = true)
                        }
                    } else render(text, chosen.edgeFallback(), index).copy(fallback = true)
                }, progress = { group, groupTotal, variation, part, total -> progress("Text/Idee $group/$groupTotal · Variante $variation/6 · Absatz $part/$total") })
                check(voiceKey(voice) == voiceKey(alarm.resolveVoice(voiceFactory()))) { "Die Stimme wurde während der Vorbereitung geändert. Bitte erneut vorbereiten." }
                store.update(alarm.id) { current ->
                    if (!current.sameSpeechAs(alarm)) current
                    else current.copy(voiceVariants = variants,
                        prepared = variants.first().steps.mapValues { (_, clips) -> clips.map(PreparedAudio::path) },
                        preparedAt = System.currentTimeMillis(), preparedSpeed = voice.playbackSpeed, preparedSignature = signature,
                        preparationError = if (primaryFailed) "Edge-Notfallstimme vorbereitet: $primaryReason" else "")
                }
            } catch (e: CancellationException) {
                store.update(alarm.id) { current ->
                    if (current.sameSpeechAs(alarm)) current.copy(preparationError = "Audio-Vorbereitung abgebrochen.") else current
                }
                throw e
            }
            catch (e: Exception) {
                store.update(alarm.id) { current ->
                    if (current.sameSpeechAs(alarm)) current.copy(preparationError = e.message ?: "Vorbereitung fehlgeschlagen") else current
                }
                throw e
            }
        }
    }

    suspend fun audio(text: String): File = File(render(text, voiceFactory(), 0).path)

    private suspend fun render(text: String, voice: SyntheseStimme, variation: Int): PreparedAudio = withContext(Dispatchers.IO) {
        // Jede Variante bekommt ihren eigenen Request/Cache-Eintrag, auch bei gleichem Text.
        val path = File(store.files, "speech_${hash(voiceKey(voice) + "|$variation|" + text)}.audio")
        if (path.length() > 44) return@withContext PreparedAudio(path.absolutePath, voice.playbackSpeed, voice.ttsProvider)
        val temporary = File(store.files, "${path.name}.${java.util.UUID.randomUUID()}.tmp")
        try {
            if (voice.ttsProvider == TtsProvider.EDGE.id) {
                withContext(Dispatchers.Main) {
                    val edge = EdgeTtsPlayer(context)
                    try {
                        withTimeout(120_000) {
                            suspendCancellableCoroutine<Unit> { continuation ->
                                edge.speak(text, voice.edgeTtsVoice, voice.ttsSpeechRate,
                                    erzwingeDeutsch = voice.immerDeutschVorlesen, onPlaybackStart = {},
                                    onComplete = { if (continuation.isActive) continuation.resume(Unit) },
                                    onError = { if (continuation.isActive) continuation.resumeWithException(it) },
                                    onAudioReady = { it.copyTo(temporary, overwrite = true) })
                                continuation.invokeOnCancellation { edge.stop() }
                            }
                        }
                    } finally { edge.shutdown() }
                }
            } else {
                val source = Synthese(context, voice).synthetisiere(text)
                try { source.copyTo(temporary, overwrite = true) } finally { source.delete() }
            }
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(temporary.absolutePath)
                check(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "Der Sprachdienst hat keine gültige Audiodatei geliefert." }
            } finally { metadata.release() }
            ensureActive()
            check(temporary.renameTo(path)) { "Das Offline-Audio konnte nicht gespeichert werden." }
            PreparedAudio(path.absolutePath, voice.playbackSpeed, voice.ttsProvider)
        } finally { temporary.delete() }
    }

    companion object {
        private val mutex = Mutex()
        fun hash(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        /** Cortex-Prinzip: vollständige Absätze, nur überlange Absätze an Wort-/Satzgrenzen teilen. */
        fun chunks(text: String): List<String> = text.replace("\r\n", "\n").replace('\r', '\n')
            .split(Regex("\n[ \t]*\n+"))
            .flatMap { paragraph -> splitLongParagraph(paragraph.replace('\n', ' ').trim()) }
        private fun splitLongParagraph(paragraph: String): List<String> {
            val result = mutableListOf<String>()
            var rest = paragraph
            while (rest.isNotEmpty()) {
                var end = minOf(rest.length, 900)
                if (end < rest.length) {
                    val sentenceEnd = rest.lastIndexOfAny(charArrayOf('.', '!', '?'), end - 1)
                    val wordEnd = rest.lastIndexOf(' ', end - 1)
                    val separator = if (sentenceEnd > end / 2) sentenceEnd else wordEnd
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
                catch (e: Exception) { failed = true; android.util.Log.w("WeckerTts", "Hintergrundvorbereitung fehlgeschlagen", e) }
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
