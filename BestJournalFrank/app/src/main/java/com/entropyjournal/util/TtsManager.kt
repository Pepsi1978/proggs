package com.entropyjournal.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Unified TTS manager: tries ElevenLabs first (if API key + voice configured),
 * automatically falls back to Edge TTS on any failure (quota, network, auth).
 */
class TtsManager(private val context: Context) {

    private val edgeTtsPlayer = EdgeTtsPlayer(context)
    private val elevenLabsPlayer = ElevenLabsTtsPlayer(context)

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

    private fun getElevenLabsKey(): String =
        prefs?.getString(Constants.PREF_ELEVENLABS_API_KEY, "") ?: ""

    private fun getElevenLabsVoiceId(): String =
        prefs?.getString(Constants.PREF_ELEVENLABS_VOICE_ID, "") ?: ""

    private fun isElevenLabsConfigured(): Boolean {
        val key = getElevenLabsKey()
        val voiceId = getElevenLabsVoiceId()
        return key.isNotBlank() && voiceId.isNotBlank()
    }

    /**
     * Speaks text using the best available TTS engine.
     * ElevenLabs if configured, Edge TTS as fallback.
     */
    fun speak(
        text: String,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit,
    ) {
        if (isElevenLabsConfigured()) {
            Log.d(TAG, "Using ElevenLabs TTS")
            elevenLabsPlayer.speak(
                text = text,
                apiKey = getElevenLabsKey(),
                voiceId = getElevenLabsVoiceId(),
                onPlaybackStart = onPlaybackStart,
                onComplete = onComplete,
                onError = { e ->
                    Log.w(TAG, "ElevenLabs failed, falling back to Edge TTS: ${e.message}")
                    edgeTtsPlayer.speak(
                        text = text,
                        onPlaybackStart = onPlaybackStart,
                        onComplete = onComplete,
                    )
                },
            )
        } else {
            Log.d(TAG, "Using Edge TTS (ElevenLabs not configured)")
            edgeTtsPlayer.speak(
                text = text,
                onPlaybackStart = onPlaybackStart,
                onComplete = onComplete,
            )
        }
    }

    fun stop() {
        elevenLabsPlayer.stop()
        edgeTtsPlayer.stop()
    }

    fun shutdown() {
        elevenLabsPlayer.shutdown()
        edgeTtsPlayer.shutdown()
    }
}
