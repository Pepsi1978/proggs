package de.frank.wecker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import de.frank.genialeideen.data.settings.SecureSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Ein Fehler, den Nutzer selbst beheben können (z. B. fehlende Offline-Stimme). */
class SyntheseAbbruch(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Snapshot der Vorlese-Einstellung: eine lokal installierte Android-Stimme samt Tempo.
 * Es gibt bewusst keinen Netzweg — die App schickt keinen Text an einen Sprachdienst.
 */
data class SyntheseStimme(val stimme: String, val ttsSpeechRate: Float) {
    constructor(s: SecureSettings) : this(s.lokaleStimme, s.ttsSpeechRate)
    fun withRate(rate: Float) = copy(ttsSpeechRate = rate)
    val ttsProvider: String get() = LokaleStimmen.PROVIDER
    /** Das Tempo steckt bereits in der erzeugten Datei. */
    val playbackSpeed: Float get() = 1f
}

/** Eine installierte, offline nutzbare Stimme. */
data class LokaleStimmeInfo(val name: String, val sprache: Locale, val qualitaet: Int) {
    val anzeige: String get() {
        val region = sprache.getDisplayCountry(Locale.GERMAN).ifBlank { sprache.getDisplayLanguage(Locale.GERMAN) }
        val stufe = when {
            qualitaet >= Voice.QUALITY_VERY_HIGH -> "sehr hohe Qualität"
            qualitaet >= Voice.QUALITY_HIGH -> "hohe Qualität"
            qualitaet >= Voice.QUALITY_NORMAL -> "normale Qualität"
            else -> "einfache Qualität"
        }
        return "$region · $stufe · ${name.substringAfterLast('#').substringAfterLast('-').ifBlank { name }}"
    }
}

/**
 * Android-TextToSpeech als einziger Vorlese-Weg. Stimmen, die Netz brauchen oder noch nicht
 * installiert sind, werden nie verwendet. Der Aufbau des Dienstes ist teuer, darum eine
 * gemeinsame Instanz, die nach kurzer Ruhe wieder freigegeben wird.
 */
object LokaleStimmen {
    const val PROVIDER = "android_tts"
    private const val RUHE_MS = 30_000L
    private const val ZEITLIMIT_MS = 90_000L

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var freigabe: Job? = null
    private val offen = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /** Alle installierten deutschen Offline-Stimmen, beste zuerst. Leer = Handlung nötig. */
    suspend fun deutscheStimmen(context: Context): List<LokaleStimmeInfo> = mutex.withLock {
        val engine = instanz(context)
        try { lokaleDeutsche(engine).map { LokaleStimmeInfo(it.name, it.locale, it.quality) } } finally { spaeterFreigeben() }
    }

    /** Erzeugt [text] als Audiodatei [ziel], ausschließlich mit einer Offline-Stimme. */
    suspend fun synthetisiere(context: Context, text: String, stimme: SyntheseStimme, ziel: File) = mutex.withLock {
        val engine = instanz(context)
        try {
            val stimmen = lokaleDeutsche(engine)
            val wahl = stimmen.firstOrNull { it.name == stimme.stimme } ?: stimmen.firstOrNull()
                ?: throw SyntheseAbbruch(KEINE_STIMME)
            withContext(Dispatchers.Main) {
                check(engine.setVoice(wahl) == TextToSpeech.SUCCESS) { "Die Stimme „${wahl.name}“ ließ sich nicht aktivieren." }
                engine.setSpeechRate(stimme.ttsSpeechRate.coerceIn(.5f, 2f))
            }
            val id = UUID.randomUUID().toString()
            val fertig = CompletableDeferred<Unit>()
            offen[id] = fertig
            try {
                val start = withContext(Dispatchers.Main) {
                    engine.synthesizeToFile(text, Bundle(), ziel, id)
                }
                if (start != TextToSpeech.SUCCESS) throw SyntheseAbbruch("Die Gerätestimme konnte den Text nicht vorbereiten.")
                withTimeout(ZEITLIMIT_MS) { fertig.await() }
            } finally { offen.remove(id) }
        } finally { spaeterFreigeben() }
    }

    /** Öffnet die Android-Seite zum Nachladen der Sprachdaten, sonst die Vorlese-Einstellungen. */
    fun sprachdatenIntents(): List<Intent> = listOf(
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
        Intent("com.android.settings.TTS_SETTINGS"),
    ).map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    const val KEINE_STIMME = "Auf diesem Gerät ist keine deutsche Offline-Stimme installiert. " +
        "Lade in den Android-Einstellungen unter „Sprachausgabe“ die deutschen Sprachdaten herunter."

    private fun lokaleDeutsche(engine: TextToSpeech): List<Voice> = runCatching { engine.voices.orEmpty() }.getOrDefault(emptySet())
        .filter { it.locale.language == Locale.GERMAN.language }
        .filter { !it.isNetworkConnectionRequired }
        .filter { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() }
        .sortedWith(compareByDescending<Voice> { it.locale.country == "DE" }.thenByDescending { it.quality }.thenBy { it.name })

    private suspend fun instanz(context: Context): TextToSpeech {
        freigabe?.cancel(); freigabe = null
        tts?.let { return it }
        val bereit = CompletableDeferred<Int>()
        val neu = withContext(Dispatchers.Main) {
            TextToSpeech(context.applicationContext) { status -> bereit.complete(status) }
        }
        val status = withTimeoutOrNull(15_000) { bereit.await() }
        if (status != TextToSpeech.SUCCESS) {
            withContext(Dispatchers.Main) { neu.shutdown() }
            throw SyntheseAbbruch("Die Sprachausgabe des Geräts ist nicht verfügbar. Prüfe in den Android-Einstellungen die „Sprachausgabe“.")
        }
        neu.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { offen[utteranceId]?.complete(Unit) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onError(utteranceId, TextToSpeech.ERROR) }
            override fun onError(utteranceId: String?, errorCode: Int) {
                offen[utteranceId]?.completeExceptionally(SyntheseAbbruch(
                    if (errorCode == TextToSpeech.ERROR_NOT_INSTALLED_YET) KEINE_STIMME
                    else "Die Gerätestimme meldete einen Fehler ($errorCode)."))
            }
        })
        tts = neu
        return neu
    }

    private fun spaeterFreigeben() {
        freigabe?.cancel()
        freigabe = scope.launch {
            delay(RUHE_MS)
            mutex.withLock { tts?.shutdown(); tts = null }
        }
    }
}
