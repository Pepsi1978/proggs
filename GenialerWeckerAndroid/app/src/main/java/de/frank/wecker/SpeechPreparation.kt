package de.frank.wecker

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.work.*
import de.frank.genialeideen.data.settings.SecureSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class SpeechPreparation(private val context: Context, private val settings: SecureSettings,
    private val voiceFactory: () -> SyntheseStimme = { SyntheseStimme(settings) }) {
    private val store = AlarmStore.get(context)
    private fun voiceKey(voice: SyntheseStimme) = listOf("lokal-v1", voice.ttsProvider, voice.stimme,
        voice.ttsSpeechRate.toString()).joinToString("|")
    val playbackSpeed: Float get() = voiceFactory().playbackSpeed

    suspend fun prepare(alarm: Alarm, progress: suspend (String) -> Unit = {}) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!alarm.needsSpeech) return@withContext
            try {
                val groups = mutableListOf<SpeechGroup>()
                if (Step.TEXT in alarm.steps) groups += SpeechGroup(Step.TEXT.name, chunks(alarm.text))
                val voice = alarm.resolveVoice(voiceFactory())
                val signature = hash(voiceKey(voice) + JSONArray(groups.map { JSONObject().put("step", it.step).put("paragraphs", JSONArray(it.paragraphs)) }).toString())
                val latest = store.get(alarm.id) ?: return@withContext
                if (!latest.sameSpeechAs(alarm)) return@withContext
                val cached = latest.voiceVariants
                if (latest.preparedSignature == signature && cached.size == VoiceVariations.COUNT &&
                    cached.flatMap { it.steps.values.flatten() }.all { File(it.path).length() > 44 }) {
                    store.update(alarm.id) { current -> if (current.sameSpeechAs(alarm)) current.copy(preparationError = "") else current }
                    return@withContext
                }
                store.update(alarm.id) { current ->
                    if (current.sameSpeechAs(alarm)) current.copy(preparationError = "Audio-Vorbereitung läuft …") else current
                }
                // Kein Netz-Rückfall: Fehlt die Offline-Stimme, bleibt der Fehler sichtbar und der Wecker nutzt seinen Ersatzton.
                val variants = VoiceVariations.buildGroups(groups, render = { text, index ->
                    ensureActive()
                    render(text, voice.withRate(VoiceVariations.rate(voice.ttsSpeechRate, index)), index)
                }, progress = { _, _, variation, part, total -> progress("Variante $variation/6 · Absatz $part/$total") })
                check(voiceKey(voice) == voiceKey(alarm.resolveVoice(voiceFactory()))) { "Die Stimme wurde während der Vorbereitung geändert. Bitte erneut vorbereiten." }
                store.update(alarm.id) { current ->
                    if (!current.sameSpeechAs(alarm)) current
                    else current.copy(voiceVariants = variants,
                        prepared = variants.first().steps.mapValues { (_, clips) -> clips.map(PreparedAudio::path) },
                        preparedAt = System.currentTimeMillis(), preparedSpeed = voice.playbackSpeed, preparedSignature = signature,
                        preparationError = "")
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
            LokaleStimmen.synthetisiere(context, text, voice, temporary)
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(temporary.absolutePath)
                check(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "Die Gerätestimme hat keine gültige Audiodatei geliefert." }
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
        // Alles läuft lokal: keine Netzbedingung, die Vorbereitung klappt auch im Flugmodus.
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("speech-refresh", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PreparationWorker>().build())
        }
        fun periodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("speech-periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PreparationWorker>(15, TimeUnit.MINUTES).build())
        }
    }
}
