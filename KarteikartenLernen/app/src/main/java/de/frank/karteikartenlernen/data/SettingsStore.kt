package de.frank.karteikartenlernen.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.karteikartenlernen.audio.TtsVoiceRegistry
import de.frank.karteikartenlernen.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val dark = booleanPreferencesKey("dark")
        val darkProfile = intPreferencesKey("dark_profile")
        val lightProfile = intPreferencesKey("light_profile")
        val sounds = booleanPreferencesKey("sounds")
        val volume = floatPreferencesKey("volume")
        val flip = booleanPreferencesKey("sound_flip")
        val known = booleanPreferencesKey("sound_known")
        val unknown = booleanPreferencesKey("sound_unknown")
        val transition = booleanPreferencesKey("sound_transition")
        val done = booleanPreferencesKey("sound_done")
        val voice = stringPreferencesKey("voice")
        val rate = floatPreferencesKey("speech_rate")
        val cardFont = intPreferencesKey("card_font")
        val model = stringPreferencesKey("model")
        val reasoning = stringPreferencesKey("reasoning")
        val cardsPerResearch = intPreferencesKey("cards_per_research")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            dark = p[Keys.dark] ?: true,
            darkProfile = p[Keys.darkProfile] ?: 0,
            lightProfile = p[Keys.lightProfile] ?: 0,
            sounds = p[Keys.sounds] ?: true,
            volume = p[Keys.volume] ?: 0.6f,
            soundFlip = p[Keys.flip] ?: true,
            soundKnown = p[Keys.known] ?: true,
            soundUnknown = p[Keys.unknown] ?: true,
            soundTransition = p[Keys.transition] ?: true,
            soundDone = p[Keys.done] ?: true,
            voice = TtsVoiceRegistry.resolveVoiceId(p[Keys.voice].orEmpty()),
            speechRate = p[Keys.rate] ?: 1f,
            cardFont = p[Keys.cardFont] ?: 20,
            model = p[Keys.model] ?: "GPT 5.6 Terra",
            reasoning = p[Keys.reasoning] ?: "Mittel",
            cardsPerResearch = p[Keys.cardsPerResearch] ?: 0,
        )
    }

    suspend fun save(value: AppSettings) {
        context.dataStore.edit { p ->
            p[Keys.dark] = value.dark
            p[Keys.darkProfile] = value.darkProfile
            p[Keys.lightProfile] = value.lightProfile
            p[Keys.sounds] = value.sounds
            p[Keys.volume] = value.volume
            p[Keys.flip] = value.soundFlip
            p[Keys.known] = value.soundKnown
            p[Keys.unknown] = value.soundUnknown
            p[Keys.transition] = value.soundTransition
            p[Keys.done] = value.soundDone
            p[Keys.voice] = value.voice
            p[Keys.rate] = value.speechRate
            p[Keys.cardFont] = value.cardFont
            p[Keys.model] = value.model
            p[Keys.reasoning] = value.reasoning
            p[Keys.cardsPerResearch] = value.cardsPerResearch
        }
    }
}
