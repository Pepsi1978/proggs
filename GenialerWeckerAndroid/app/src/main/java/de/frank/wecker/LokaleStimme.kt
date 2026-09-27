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

/** Die vier Vorlese- und Diktatsprachen als kurze Codes, wie sie im Alarm-JSON stehen. */
object Sprachen {
    val CODES = listOf("de", "en", "fr", "es")
    /** Unbekannt oder leer gilt als Deutsch – so bleiben Altbestände deutsch. */
    fun gueltig(code: String?): String = code?.lowercase()?.takeIf { it in CODES } ?: "de"
    fun name(code: String): String = when (code) { "en" -> "Englisch"; "fr" -> "Französisch"; "es" -> "Spanisch"; else -> "Deutsch" }
    fun kurz(code: String): String = gueltig(code).uppercase()
    fun diktat(code: String): DiktatSprache = DiktatSprache.entries.first { it.code == gueltig(code) }
    fun probe(code: String): String = when (code) {
        "en" -> "Good morning! Your alarm is ready."
        "fr" -> "Bonjour ! Ton réveil est prêt."
        "es" -> "¡Buenos días! Tu despertador está listo."
        else -> "Guten Morgen! Dein Wecker ist bereit."
    }
    fun keineStimme(code: String): String = "Für ${name(code)} ist auf diesem Gerät keine Offline-Stimme installiert. " +
        "Lade in den Android-Einstellungen unter „Sprachausgabe“ die Sprachdaten für ${name(code)} herunter. Bis dahin kommt beim Wecken der Ersatzweckton."
}

/**
 * Snapshot der Vorlese-Einstellung: eine lokal installierte Android-Stimme samt Tempo und Sprache.
 * [vorgaben] sind die bevorzugten Stimmen je Sprache aus den Einstellungen.
 * Es gibt bewusst keinen Netzweg — die App schickt keinen Text an einen Sprachdienst.
 */
data class SyntheseStimme(val stimme: String, val ttsSpeechRate: Float, val sprache: String = "de", val vorgaben: Map<String, String> = emptyMap()) {
    constructor(s: SecureSettings) : this(s.lokaleStimme, s.ttsSpeechRate, "de", Sprachen.CODES.associateWith { s.stimmeFuer(it) })
    fun withRate(rate: Float) = copy(ttsSpeechRate = rate)
    /** Standardstimme derselben Einstellungen für eine andere Sprache. */
    fun fuerSprache(code: String): SyntheseStimme = copy(sprache = code, stimme = if (vorgaben.isEmpty() && code == sprache) stimme else vorgaben[code].orEmpty())
    val ttsProvider: String get() = LokaleStimmen.PROVIDER
    /** Das Tempo steckt bereits in der erzeugten Datei. */
    val playbackSpeed: Float get() = 1f
}

/** Eine installierte, offline nutzbare Stimme; [nummer] zählt nur, wenn kein Geschlecht belegt ist. */
data class LokaleStimmeInfo(val name: String, val sprache: Locale, val qualitaet: Int, val nummer: Int = 1) {
    val anzeige: String get() {
        // Außer Deutsch keine Qualitätsbehauptung: alle Google-Stimmen melden „hoch“, auch die roboterhaften.
        if (sprache.language != "de") {
            val region = sprache.getDisplayCountry(Locale.GERMAN).takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            return "${Sprachen.name(sprache.language)}$region · Stimme $nummer"
        }
        val stufe = when {
            qualitaet >= Voice.QUALITY_VERY_HIGH -> "sehr hohe Qualität"
            qualitaet >= Voice.QUALITY_HIGH -> "hohe Qualität"
            qualitaet >= Voice.QUALITY_NORMAL -> "normale Qualität"
            else -> "einfache Qualität"
        }
        return "Deutsch · $stufe · ${StimmAuswahl.BEVORZUGT[name] ?: "Stimme $nummer"}"
    }
}

/**
 * Welche Offline-Stimmen die App anbietet. Android liefert kein Geschlecht; belegt ist es nur für die zwei
 * vom Nutzer gewählten Google-TTS-Stimmen, gemessen auf dem SM-F971B am 27.09.2026 (Grundfrequenz eines
 * Probesatzes: deg 106 Hz, nfh 158 Hz; zum Vergleich deb 96 Hz, dea 164 Hz). Gibt es diese Stimmen auf einem
 * Gerät nicht, bleiben alle Offline-Stimmen wählbar – hochwertige Stimmen anderer Geräte werden nie ausgeschlossen.
 */
object StimmAuswahl {
    val BEVORZUGT = linkedMapOf("de-de-x-deg-local" to "männlich", "de-de-x-nfh-local" to "weiblich")

    /** Offline-Stimmen in Anzeige-Reihenfolge; für Deutsch nur die bevorzugten, wenn mindestens eine vorhanden ist. */
    fun angeboten(namen: List<String>, sprache: String = "de"): List<String> {
        if (sprache != "de") return namen
        val bevorzugt = BEVORZUGT.keys.filter { it in namen }
        return bevorzugt.ifEmpty { namen }
    }

    /** Die tatsächlich benutzte Stimme: die gewünschte, wenn angeboten, sonst die erste angebotene. */
    fun wirksam(gewuenscht: String, angeboten: List<String>): String? = gewuenscht.takeIf { it in angeboten } ?: angeboten.firstOrNull()
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

    /** Angebotene Offline-Stimmen aller vier Sprachen in einem Durchgang. Leere Liste = für diese Sprache nichts installiert. */
    suspend fun alleSprachen(context: Context): Map<String, List<LokaleStimmeInfo>> = mutex.withLock {
        val engine = instanz(context)
        try {
            Sprachen.CODES.associateWith { code -> offlineStimmen(engine, code).mapIndexed { i, v -> LokaleStimmeInfo(v.name, v.locale, v.quality, i + 1) } }
        } finally { spaeterFreigeben() }
    }

    /** Erzeugt [text] als Audiodatei [ziel], ausschließlich mit einer Offline-Stimme. */
    suspend fun synthetisiere(context: Context, text: String, stimme: SyntheseStimme, ziel: File) = mutex.withLock {
        val engine = instanz(context)
        try {
            // Nur Stimmen DER Sprache des Textes – nie ein französischer Text mit deutscher Stimme.
            val stimmen = offlineStimmen(engine, Sprachen.gueltig(stimme.sprache))
            val name = StimmAuswahl.wirksam(stimme.stimme, stimmen.map { it.name }) ?: throw SyntheseAbbruch(Sprachen.keineStimme(Sprachen.gueltig(stimme.sprache)))
            val wahl = stimmen.first { it.name == name }
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

    /** Hauptregion je Sprache zuerst (Deutschland, USA, Frankreich, Spanien). */
    private val HAUPTREGION = mapOf("de" to "DE", "en" to "US", "fr" to "FR", "es" to "ES")

    /** Strikt offline (kein Netz, installiert), danach nur die angebotenen – gilt für Auswahl UND Synthese. */
    private fun offlineStimmen(engine: TextToSpeech, sprache: String): List<Voice> {
        val offline = runCatching { engine.voices.orEmpty() }.getOrDefault(emptySet())
            .filter { it.locale.language == sprache }
            .filter { !it.isNetworkConnectionRequired }
            .filter { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() }
            .sortedWith(compareByDescending<Voice> { it.locale.country == HAUPTREGION[sprache] }.thenByDescending { it.quality }.thenBy { it.name })
        val angeboten = StimmAuswahl.angeboten(offline.map { it.name }, sprache)
        return angeboten.mapNotNull { name -> offline.firstOrNull { it.name == name } }
    }

    /** Welche Stimme für [gewuenscht] in [sprache] wirklich erklingt (für die Vorbereitungssignatur); null ohne Offline-Stimme. */
    suspend fun wirksameStimme(context: Context, gewuenscht: String, sprache: String = "de"): String? = mutex.withLock {
        val engine = instanz(context)
        try { StimmAuswahl.wirksam(gewuenscht, offlineStimmen(engine, Sprachen.gueltig(sprache)).map { it.name }) } finally { spaeterFreigeben() }
    }

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
                    if (errorCode == TextToSpeech.ERROR_NOT_INSTALLED_YET) "Die Sprachdaten dieser Stimme sind noch nicht installiert."
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
