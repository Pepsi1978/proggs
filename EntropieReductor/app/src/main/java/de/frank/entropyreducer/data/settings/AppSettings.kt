package de.frank.entropyreducer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_settings")

/**
 * UI-Einstellungen + Profil-Text. Nicht-sensible Daten in DataStore.
 */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val ds = context.dataStore

    val whisperModelFlow: Flow<String> = ds.data.map { it[KEY_WHISPER_MODEL] ?: DEFAULT_WHISPER }
    val geminiModelFlow: Flow<String> = ds.data.map { it[KEY_GEMINI_MODEL] ?: DEFAULT_GEMINI }
    val transcriptionLanguageFlow: Flow<String> = ds.data.map { it[KEY_LANGUAGE] ?: "de" }
    val ttsVoiceFlow: Flow<String> = ds.data.map { it[KEY_TTS_VOICE] ?: "" }
    val profileTextFlow: Flow<String> = ds.data.map { it[KEY_PROFILE_TEXT] ?: "" }
    val themeIsDarkFlow: Flow<Boolean?> = ds.data.map { it[KEY_THEME_DARK] }

    suspend fun setWhisperModel(value: String) = ds.edit { it[KEY_WHISPER_MODEL] = value }
    suspend fun setGeminiModel(value: String) = ds.edit { it[KEY_GEMINI_MODEL] = value }
    suspend fun setTranscriptionLanguage(value: String) = ds.edit { it[KEY_LANGUAGE] = value }
    suspend fun setTtsVoice(value: String) = ds.edit { it[KEY_TTS_VOICE] = value }
    suspend fun setProfileText(value: String) = ds.edit { it[KEY_PROFILE_TEXT] = value }
    suspend fun setThemeDark(value: Boolean?) = ds.edit {
        if (value == null) it.remove(KEY_THEME_DARK) else it[KEY_THEME_DARK] = value
    }

    companion object {
        private val KEY_WHISPER_MODEL = stringPreferencesKey("whisper_model")
        private val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        private val KEY_LANGUAGE = stringPreferencesKey("transcription_lang")
        private val KEY_TTS_VOICE = stringPreferencesKey("tts_voice")
        private val KEY_PROFILE_TEXT = stringPreferencesKey("profile_text")
        private val KEY_THEME_DARK = booleanPreferencesKey("theme_dark")

        const val DEFAULT_WHISPER = "whisper-large-v3-turbo"
        const val DEFAULT_GEMINI = "gemini-2.5-flash"
    }
}
