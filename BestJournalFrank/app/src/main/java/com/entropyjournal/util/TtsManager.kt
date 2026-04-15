package com.entropyjournal.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Unified TTS manager: uses the user-selected TTS provider exclusively.
 * No fallback between providers — the selected one is the only one used.
 * Providers: ElevenLabs (cloud), Google Cloud Chirp 3 HD (cloud), Edge TTS (cloud free).
 */
class TtsManager(private val context: Context) {

    private val edgeTtsPlayer = EdgeTtsPlayer(context)
    private val elevenLabsPlayer = ElevenLabsTtsPlayer(context)
    private val googleCloudTtsPlayer = GoogleCloudTtsPlayer(context)

    companion object {
        private const val TAG = "TtsManager"
    }

    private val prefs: SharedPreferences? by lazy {
        try {
            val masterKey = androidx.security.crypto.MasterKeys.getOrCreate(
                androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
            )
            androidx.security.crypto.EncryptedSharedPreferences.create(
                Constants.ENCRYPTED_PREFS_NAME,
                masterKey,
                context,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to access encrypted prefs: ${e.message}")
            null
        }
    }

    private fun getSelectedProvider(): String =
        prefs?.getString(Constants.PREF_TTS_PROVIDER, Constants.TTS_PROVIDER_EDGE)
            ?: Constants.TTS_PROVIDER_EDGE

    private fun getElevenLabsKey(): String =
        prefs?.getString(Constants.PREF_ELEVENLABS_API_KEY, "") ?: ""

    private fun getElevenLabsVoiceId(): String =
        prefs?.getString(Constants.PREF_ELEVENLABS_VOICE_ID, "") ?: ""

    private fun getEdgeTtsVoice(): String =
        prefs?.getString(Constants.PREF_EDGE_TTS_VOICE, Constants.DEFAULT_EDGE_TTS_VOICE)
            ?: Constants.DEFAULT_EDGE_TTS_VOICE

    private fun getGoogleTtsKey(): String =
        prefs?.getString(Constants.PREF_GOOGLE_TTS_API_KEY, "") ?: ""

    private fun getGoogleTtsVoice(): String =
        prefs?.getString(Constants.PREF_GOOGLE_TTS_VOICE, Constants.DEFAULT_GOOGLE_TTS_VOICE)
            ?: Constants.DEFAULT_GOOGLE_TTS_VOICE

    private fun isTtsEnabled(): Boolean =
        prefs?.getBoolean(Constants.PREF_TTS_ENABLED, false) ?: false

    /**
     * Speaks text using the user-selected TTS provider.
     * Returns false if TTS is disabled (caller should show toast).
     */
    fun speak(
        text: String,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit,
    ): Boolean {
        if (!isTtsEnabled()) {
            onComplete()
            return false
        }
        if (text.isBlank()) {
            onComplete()
            return true
        }
        when (getSelectedProvider()) {
            Constants.TTS_PROVIDER_ELEVENLABS -> {
                val key = getElevenLabsKey()
                val voiceId = getElevenLabsVoiceId()
                if (key.isNotBlank() && voiceId.isNotBlank()) {
                    Log.d(TAG, "Using ElevenLabs TTS")
                    elevenLabsPlayer.speak(
                        text = text,
                        apiKey = key,
                        voiceId = voiceId,
                        onPlaybackStart = onPlaybackStart,
                        onComplete = onComplete,
                        onError = { e ->
                            Log.e(TAG, "ElevenLabs TTS error: ${e.message}")
                            onComplete()
                        },
                    )
                } else {
                    Log.w(TAG, "ElevenLabs selected but not configured")
                    onComplete()
                }
            }
            Constants.TTS_PROVIDER_GOOGLE -> {
                val key = getGoogleTtsKey()
                val voice = getGoogleTtsVoice()
                if (key.isNotBlank()) {
                    Log.d(TAG, "Using Google Cloud TTS voice: $voice")
                    googleCloudTtsPlayer.speak(
                        text = text,
                        apiKey = key,
                        voiceName = voice,
                        onPlaybackStart = onPlaybackStart,
                        onComplete = onComplete,
                        onError = { e ->
                            Log.e(TAG, "Google Cloud TTS error: ${e.message}")
                            onComplete()
                        },
                    )
                } else {
                    Log.w(TAG, "Google Cloud TTS selected but not configured")
                    onComplete()
                }
            }
            else -> {
                val voice = getEdgeTtsVoice()
                Log.d(TAG, "Using Edge TTS voice: $voice")
                edgeTtsPlayer.speak(
                    text = text,
                    voice = voice,
                    onPlaybackStart = onPlaybackStart,
                    onComplete = onComplete,
                )
            }
        }
        return true
    }

    fun stop() {
        elevenLabsPlayer.stop()
        edgeTtsPlayer.stop()
        googleCloudTtsPlayer.stop()
    }

    fun shutdown() {
        elevenLabsPlayer.shutdown()
        edgeTtsPlayer.shutdown()
        googleCloudTtsPlayer.shutdown()
    }
}
