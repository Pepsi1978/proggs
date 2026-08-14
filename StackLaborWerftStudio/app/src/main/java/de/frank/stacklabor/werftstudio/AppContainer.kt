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
import de.frank.stacklabor.werftstudio.service.tts.TtsAudioCache
import de.frank.stacklabor.werftstudio.service.tts.TtsCredentials
import de.frank.stacklabor.werftstudio.service.tts.TtsEngine
import de.frank.stacklabor.werftstudio.service.tts.TtsUsageStore

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

    init {
        val credentials = object : TtsCredentials {
            override fun googleApiKey(): String? = BuildConfig.GOOGLE_TTS_API_KEY.takeIf(String::isNotBlank)
            override fun qwenApiKey(): String? = BuildConfig.QWEN_TTS_API_KEY.takeIf(String::isNotBlank)
            override fun qwenVoiceId(): String? = BuildConfig.QWEN_TTS_VOICE_ID.takeIf(String::isNotBlank)
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
