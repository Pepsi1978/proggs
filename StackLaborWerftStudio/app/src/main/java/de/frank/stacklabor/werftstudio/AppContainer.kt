package de.frank.stacklabor.werftstudio

import android.content.Context
import android.provider.Settings
import de.frank.stacklabor.werftstudio.data.local.StackLaborDatabase
import de.frank.stacklabor.werftstudio.data.preferences.EinstellungenStore
import de.frank.stacklabor.werftstudio.data.preferences.stackLaborEinstellungenDataStore
import de.frank.stacklabor.werftstudio.data.repository.AssetStartbestandQuelle
import de.frank.stacklabor.werftstudio.data.repository.DateiRueckfallsicherungStore
import de.frank.stacklabor.werftstudio.data.repository.RoomStackLaborRepository
import de.frank.stacklabor.werftstudio.data.repository.StackLaborRepository
import de.frank.stacklabor.werftstudio.data.transfer.StackLaborJson
import de.frank.stacklabor.werftstudio.service.auth.CodexOAuthClient
import de.frank.stacklabor.werftstudio.service.auth.KeystoreCodexTokenStore
import de.frank.stacklabor.werftstudio.service.codex.CodexEvaluationService
import de.frank.stacklabor.werftstudio.service.codex.CodexResponsesClient
import de.frank.stacklabor.werftstudio.service.codex.PersistedCodexEvaluator
import de.frank.stacklabor.werftstudio.service.tts.EdgeTtsProvider
import de.frank.stacklabor.werftstudio.service.tts.GoogleCloudTtsProvider
import de.frank.stacklabor.werftstudio.service.tts.QwenTtsProvider
import de.frank.stacklabor.werftstudio.service.tts.QwenVoiceDirectory
import de.frank.stacklabor.werftstudio.service.tts.TtsAudioCache
import de.frank.stacklabor.werftstudio.service.tts.TtsCredentials
import de.frank.stacklabor.werftstudio.service.tts.TtsEngine
import de.frank.stacklabor.werftstudio.service.tts.TtsUsageStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Die drei Schlüssel, die der Benutzer selbst hinterlegen kann. */
data class TtsSchluessel(
    val google: String = "",
    val qwen: String = "",
    val qwenStimme: String = "",
)

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext
    private val appContext = applicationContext
    private val database = StackLaborDatabase.getInstance(appContext)

    val repository: StackLaborRepository = RoomStackLaborRepository(
        database = database,
        json = StackLaborJson(),
        startbestandQuelle = AssetStartbestandQuelle(appContext),
        rueckfallsicherungStore = DateiRueckfallsicherungStore(appContext),
    )

    val settings = EinstellungenStore(
        dataStore = appContext.stackLaborEinstellungenDataStore,
        systemBewegungReduziert = runCatching {
            Settings.Global.getFloat(appContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false),
    )

    val oauth = CodexOAuthClient(KeystoreCodexTokenStore(appContext))
    val evaluator = PersistedCodexEvaluator(
        repository = repository,
        settings = settings,
        oauth = oauth,
        service = CodexEvaluationService(CodexResponsesClient(oauth)),
    )

    val ttsUsage = TtsUsageStore(appContext)
    val ttsEngine: TtsEngine
    val qwenVoiceDirectory = QwenVoiceDirectory()

    /**
     * Was der Benutzer in den Einstellungen hinterlegt hat, gewinnt; ist dort nichts eingetragen,
     * gilt weiter der Schlüssel aus dem SK-Ordner, der beim Bauen eingebacken wurde.
     *
     * Die Werte werden hier gespiegelt, weil die TTS-Anbieter sie ohne Coroutine abfragen.
     */
    @Volatile private var eigeneSchluessel = TtsSchluessel()

    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Der zuletzt gelesene Stand — die Einstellungs-Oberfläche zeigt ihn an. */
    fun schluessel(): TtsSchluessel = eigeneSchluessel

    fun googleSchluesselAktiv(): String = eigeneSchluessel.google.ifBlank { BuildConfig.GOOGLE_TTS_API_KEY }

    fun qwenSchluesselAktiv(): String = eigeneSchluessel.qwen.ifBlank { BuildConfig.QWEN_TTS_API_KEY }

    fun qwenStimmeAktiv(): String = eigeneSchluessel.qwenStimme.ifBlank { BuildConfig.QWEN_TTS_VOICE_ID }

    init {
        containerScope.launch {
            settings.einstellungen.collect { gespeichert ->
                eigeneSchluessel = TtsSchluessel(
                    google = gespeichert.googleApiKey,
                    qwen = gespeichert.qwenApiKey,
                    qwenStimme = gespeichert.qwenStimmenId,
                )
            }
        }
        val credentials = object : TtsCredentials {
            override fun googleApiKey(): String? = googleSchluesselAktiv().takeIf(String::isNotBlank)
            override fun qwenApiKey(): String? = qwenSchluesselAktiv().takeIf(String::isNotBlank)
            override fun qwenVoiceId(): String? = qwenStimmeAktiv().takeIf(String::isNotBlank)
        }
        val cache = TtsAudioCache(appContext)
        ttsEngine = TtsEngine(
            listOf(
                EdgeTtsProvider(cache, ttsUsage),
                GoogleCloudTtsProvider(credentials, cache, ttsUsage),
                QwenTtsProvider(credentials, cache, ttsUsage),
            ),
        )
    }
}
