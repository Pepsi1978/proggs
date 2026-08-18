package de.frank.denknotiz.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class AppTheme(val id: String, val label: String) {
    GOLD_DARK("gold_dark", "Gold-Dunkel"),
    LIGHT("light", "Hell"),
    DARK("dark", "Dunkel"),
    GOLD_LIGHT("gold_light", "Gold-Hell"),
}

enum class TtsProvider(val id: String, val label: String) {
    CHIRP("chirp", "Google Chirp 3 HD"),
    EDGE("edge", "Microsoft Edge"),
    QWEN("qwen", "Eigene Stimme"),
}

enum class CodexModel(val apiId: String, val label: String) {
    SOL("gpt-5.6-sol", "GPT 5.6 Sol"),
    TERRA("gpt-5.6-terra", "GPT 5.6 Terra"),
    LUNA("gpt-5.6-luna", "GPT 5.6 Luna"),
}

enum class ReasoningEffort(val apiValue: String, val label: String) {
    LOW("low", "Niedrig"),
    MEDIUM("medium", "Mittel"),
    HIGH("high", "Hoch"),
    XHIGH("xhigh", "Sehr hoch"),
    MAX("max", "Maximal"),
}

data class SettingsSnapshot(
    val groqKey: String = "",
    val googleKey: String = "",
    val qwenKey: String = "",
    val theme: AppTheme = AppTheme.GOLD_DARK,
    val model: CodexModel = CodexModel.TERRA,
    val reasoning: ReasoningEffort = ReasoningEffort.MEDIUM,
    val profileId: String = "normal",
    val profileNames: Map<String, String> = emptyMap(),
    val profileInstructions: Map<String, String> = emptyMap(),
    val ttsProvider: TtsProvider = TtsProvider.EDGE,
    val chirpVoice: String = "de-DE-Chirp3-HD-Kore",
    val edgeVoice: String = "de-DE-SeraphinaMultilingualNeural",
    val qwenVoiceId: String = "",
    val qwenVoiceNames: Map<String, String> = emptyMap(),
    val speechRate: Float = 1f,
    val reducedMotion: Boolean = false,
)

class SecureSettings(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "denknotiz_secure",
        MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsSnapshot> = _state.asStateFlow()

    fun update(transform: (SettingsSnapshot) -> SettingsSnapshot) {
        val next = transform(_state.value)
        _state.value = next
        preferences.edit()
            .putString("groq", next.groqKey.trim())
            .putString("google", next.googleKey.trim())
            .putString("qwen", next.qwenKey.filterNot(Char::isWhitespace))
            .putString("theme", next.theme.id)
            .putString("model", next.model.apiId)
            .putString("reasoning", next.reasoning.apiValue)
            .putString("profile", next.profileId)
            .putString("profile_names", JSONObject(next.profileNames.toMap<String, Any>()).toString())
            .putString("profile_instructions", JSONObject(next.profileInstructions.toMap<String, Any>()).toString())
            .putString("tts_provider", next.ttsProvider.id)
            .putString("chirp_voice", next.chirpVoice.trim())
            .putString("edge_voice", next.edgeVoice.trim())
            .putString("qwen_voice", next.qwenVoiceId.trim())
            .putString("qwen_names", JSONObject(next.qwenVoiceNames.toMap<String, Any>()).toString())
            .putFloat("speech_rate", next.speechRate.coerceIn(0.7f, 1.3f))
            .putBoolean("reduced_motion", next.reducedMotion)
            .apply()
    }

    private fun read(): SettingsSnapshot {
        val names = readStringMap("qwen_names")
        val profileNames = readStringMap("profile_names")
        val profileInstructions = readStringMap("profile_instructions", keepBlank = true)
        val storedProfile = preferences.getString("profile", "normal").orEmpty()
        return SettingsSnapshot(
            groqKey = preferences.getString("groq", "").orEmpty(),
            googleKey = preferences.getString("google", "").orEmpty(),
            qwenKey = preferences.getString("qwen", "").orEmpty(),
            theme = AppTheme.entries.firstOrNull { it.id == preferences.getString("theme", "") } ?: AppTheme.GOLD_DARK,
            model = CodexModel.entries.firstOrNull { it.apiId == preferences.getString("model", "") } ?: CodexModel.TERRA,
            reasoning = ReasoningEffort.entries.firstOrNull { it.apiValue == preferences.getString("reasoning", "") }
                ?: ReasoningEffort.MEDIUM,
            profileId = if (storedProfile in setOf("short", "normal", "detailed", "custom1", "custom2", "custom3")) storedProfile else "normal",
            profileNames = profileNames,
            profileInstructions = profileInstructions,
            ttsProvider = TtsProvider.entries.firstOrNull { it.id == preferences.getString("tts_provider", "") }
                ?: TtsProvider.EDGE,
            chirpVoice = preferences.getString("chirp_voice", "de-DE-Chirp3-HD-Kore").orEmpty(),
            edgeVoice = preferences.getString("edge_voice", "de-DE-SeraphinaMultilingualNeural").orEmpty(),
            qwenVoiceId = preferences.getString("qwen_voice", "").orEmpty(),
            qwenVoiceNames = names,
            speechRate = preferences.getFloat("speech_rate", 1f).coerceIn(0.7f, 1.3f),
            reducedMotion = preferences.getBoolean("reduced_motion", false),
        )
    }

    private fun readStringMap(key: String, keepBlank: Boolean = false): Map<String, String> = runCatching {
        val json = JSONObject(preferences.getString(key, "{}") ?: "{}")
        json.keys().asSequence().associateWith(json::optString).let { values ->
            if (keepBlank) values else values.filterValues(String::isNotBlank)
        }
    }.getOrDefault(emptyMap())
}
