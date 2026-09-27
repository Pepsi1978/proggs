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
    private fun voiceKey(voice: SyntheseStimme) = schluessel(voice)
    val playbackSpeed: Float get() = voiceFactory().playbackSpeed

    suspend fun prepare(alarm: Alarm, progress: suspend (String) -> Unit = {}) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!alarm.needsSpeech) return@withContext
            try {
                val groups = mutableListOf<SpeechGroup>()
                if (Step.TEXT in alarm.steps) groups += SpeechGroup(Step.TEXT.name, chunks(alarm.text))
                val textJson = JSONArray(groups.map { JSONObject().put("step", it.step).put("paragraphs", JSONArray(it.paragraphs)) }).toString()
                fun signatur(v: SyntheseStimme) = hash(voiceKey(v) + textJson)
                val wunsch = alarm.resolveVoice(voiceFactory())
                // Natürliche Stimme; ohne Netz (und als Rückfall) die beste Gerätestimme derselben Sprache.
                val premium = wunsch.istPremium
                val latest = store.get(alarm.id) ?: return@withContext
                if (!latest.sameSpeechAs(alarm)) return@withContext
                val cached = latest.voiceVariants
                fun bereit(sig: String) = latest.preparedSignature == sig && cached.size == VoiceVariations.COUNT &&
                    cached.flatMap { it.steps.values.flatten() }.all { File(it.path).length() > 44 }
                fun ohneFehler() = store.update(alarm.id) { current -> if (current.sameSpeechAs(alarm)) current.copy(preparationError = "") else current }
                if (premium && bereit(signatur(wunsch))) { ohneFehler(); return@withContext }
                val versuchePremium = premium && EdgeStimmen.netzDa(context)
                // Die wirklich klingende Stimme zählt: war eine inzwischen ausgeblendete Stimme gewählt, wird neu vorbereitet.
                suspend fun geraet(): SyntheseStimme = wunsch.alsGeraetestimme().let { v -> LokaleStimmen.wirksameStimme(context, v.stimme, v.sprache)?.let { v.copy(stimme = it) } ?: v }
                if (!versuchePremium) {
                    val lokal = geraet()
                    if (bereit(signatur(lokal))) { ohneFehler(); if (premium) PreparationWorker.sobaldNetz(context); return@withContext }
                }
                store.update(alarm.id) { current ->
                    if (current.sameSpeechAs(alarm)) current.copy(preparationError = "Audio-Vorbereitung läuft …") else current
                }
                suspend fun baue(voice: SyntheseStimme) = VoiceVariations.buildGroups(groups, render = { text, index ->
                    ensureActive()
                    render(text, voice.withRate(VoiceVariations.rate(voice.ttsSpeechRate, index)), index)
                }, progress = { _, _, variation, part, total -> progress("Variante $variation/6 · Absatz $part/$total") })
                var voice = if (versuchePremium) wunsch else geraet()
                // Premium scheitert (Netz weg, Dienst gestört): sofort mit der Gerätestimme fertig werden, später erneut versuchen.
                val variants = try { baue(voice) } catch (e: Exception) {
                    if (e is CancellationException || !versuchePremium) throw e
                    android.util.Log.w("WeckerTts", "Premium-Stimme nicht verfügbar, Gerätestimme übernimmt", e)
                    voice = geraet(); baue(voice)
                }
                if (premium && !voice.istPremium) PreparationWorker.sobaldNetz(context)
                val frisch = alarm.resolveVoice(voiceFactory())
                check(frisch.ttsSpeechRate == wunsch.ttsSpeechRate && frisch.stimme == wunsch.stimme) { "Die Stimme wurde während der Vorbereitung geändert. Bitte erneut vorbereiten." }
                val signature = signatur(voice)
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

    /** Probe: Premium, wenn gewählt und erlaubt; ohne Netz ehrlich melden statt still eine andere Stimme abzuspielen. */
    suspend fun audio(text: String): File {
        val voice = voiceFactory()
        if (!voice.istPremium) return File(render(text, voice.alsGeraetestimme(), 0).path)
        if (!EdgeStimmen.netzDa(context)) throw SyntheseAbbruch("Für die Probe einer Premium-Stimme braucht das Handy kurz Internet. " +
            "Deine Wecker klingeln trotzdem – ihre Ansagen liegen fertig auf dem Gerät.")
        return File(render(text, voice, 0).path)
    }

    private suspend fun render(text: String, voice: SyntheseStimme, variation: Int): PreparedAudio = withContext(Dispatchers.IO) {
        // Jede Variante bekommt ihren eigenen Request/Cache-Eintrag, auch bei gleichem Text.
        val path = File(store.files, "speech_${hash(voiceKey(voice) + "|$variation|" + text)}.audio")
        if (path.length() > 44) return@withContext PreparedAudio(path.absolutePath, voice.playbackSpeed, voice.ttsProvider)
        val temporary = File(store.files, "${path.name}.${java.util.UUID.randomUUID()}.tmp")
        try {
            if (voice.istPremium) {
                EdgeStimmen.synthetisiere(text, voice.stimme, voice.ttsSpeechRate, temporary)
                delay(250) // Almanach E7: Anfragen staffeln statt in schneller Folge
            } else LokaleStimmen.synthetisiere(context, text, voice, temporary)
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(temporary.absolutePath)
                check(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "Die Stimme hat keine gültige Audiodatei geliefert." }
            } finally { metadata.release() }
            ensureActive()
            check(temporary.renameTo(path)) { "Das Offline-Audio konnte nicht gespeichert werden." }
            PreparedAudio(path.absolutePath, voice.playbackSpeed, voice.ttsProvider)
        } finally { temporary.delete() }
    }

    companion object {
        private val mutex = Mutex()
        /**
         * Signatur der Stimme. Für Deutsch exakt das bisherige Format (lokal-v2), damit alte deutsche Wecker nicht neu
         * vorbereitet werden; andere Sprachen hängen ihren Code an.
         */
        fun schluessel(voice: SyntheseStimme): String = (listOf("lokal-v2", voice.ttsProvider, voice.stimme, voice.ttsSpeechRate.toString()) +
            listOfNotNull(Sprachen.gueltig(voice.sprache).takeIf { it != "de" },
                // Nur eine ausdrücklich andere Engine ändert die Signatur; Google und Altbestand bleiben byte-gleich.
                voice.engine.takeIf { it.isNotBlank() && it != LokaleStimmen.GOOGLE }?.let { "engine=$it" })).joinToString("|")
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
        // Ohne Netzbedingung: Gerätestimmen klappen auch im Flugmodus; Premium holt sobaldNetz() nach.
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("speech-refresh", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PreparationWorker>().build())
        }
        /** Premium-Ansage nachholen, sobald wieder Netz da ist; bis dahin klingelt die Gerätestimme. */
        fun sobaldNetz(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("speech-netz", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PreparationWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setInitialDelay(1, TimeUnit.MINUTES).build())
        }
        fun periodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("speech-periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PreparationWorker>(15, TimeUnit.MINUTES).build())
        }
    }
}
