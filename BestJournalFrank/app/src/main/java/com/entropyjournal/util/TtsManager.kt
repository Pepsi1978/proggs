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

    // Sequenz-ID fuer Absatz-Pipeline. Inkrementiert beim Start einer neuen
    // speak()-Sequenz und beim Stop. Laufende speakAbsatzAt-Calls vergleichen
    // ihre erfasste ID gegen den aktuellen Wert — wenn sich der unterscheidet,
    // wurde die Sequenz unterbrochen und sie geben auf, ohne den naechsten
    // Absatz zu starten.
    @Volatile private var currentSequenceId: Int = 0

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
     *
     * Absatz-Pipeline: Wenn der Text mehrere Absaetze enthaelt (getrennt
     * durch eine oder mehrere Leerzeilen), wird er an genau diesen Absatz-
     * grenzen aufgeteilt und sequenziell vorgelesen — nach dem letzten Wort
     * eines Absatzes startet automatisch der naechste. So vermeiden wir das
     * TTS-Versagen bei sehr langen Wochen-/Monats-/Jahresrueckblicken, ohne
     * Absaetze willkuerlich mitten im Text zu zerschneiden.
     *
     * Bei Text ohne Absatztrenner laeuft alles wie zuvor in einem Stueck.
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

        val absaetze = splitIntoAbsaetze(text)
        Log.d(TAG, "speak: ${text.length} chars -> ${absaetze.size} absatz/absaetze")

        // Neue Sequenz-ID — laufende Absatz-Ketten brechen ab beim Vergleich.
        val sequenceId = ++currentSequenceId
        speakAbsatzAt(
            absaetze = absaetze,
            index = 0,
            sequenceId = sequenceId,
            firstStartCallback = onPlaybackStart,
            allDone = onComplete,
        )
        return true
    }

    /**
     * Trennt den Text an Absaetzen — eine oder mehrere Leerzeilen zwischen
     * Bloecken zaehlen als Trenner. Reine Zeilenumbrueche (Soft-Wraps inner-
     * halb desselben Absatzes) werden NICHT geteilt. Bei nur einem Absatz
     * gibt die Methode eine 1-Element-Liste mit dem getrimmten Originaltext
     * zurueck — das alte Verhalten bleibt also fuer kurze Eintraege erhalten.
     */
    private fun splitIntoAbsaetze(text: String): List<String> {
        val parts = text.split(Regex("(\\r?\\n){2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return if (parts.isEmpty()) listOf(text.trim()) else parts
    }

    private fun speakAbsatzAt(
        absaetze: List<String>,
        index: Int,
        sequenceId: Int,
        firstStartCallback: (() -> Unit)?,
        allDone: () -> Unit,
    ) {
        if (sequenceId != currentSequenceId) {
            Log.d(TAG, "speakAbsatzAt: sequence cancelled (was=$sequenceId now=$currentSequenceId)")
            return
        }
        if (index >= absaetze.size) {
            allDone()
            return
        }

        val absatz = absaetze[index]
        val isFirst = index == 0
        val isLast = index == absaetze.size - 1
        Log.d(TAG, "speakAbsatzAt: ${index + 1}/${absaetze.size} (${absatz.length} chars)")

        // onPlaybackStart nur beim ersten Absatz feuern (das Spinner-UI
        // soll genau einmal von 'lade' auf 'spricht' wechseln). Folgende
        // Absaetze starten still — der Benutzer hoert ja den naechsten
        // Block direkt nach dem letzten Wort des vorigen.
        val startCallback: (() -> Unit)? = if (isFirst) firstStartCallback else null

        speakSingle(
            text = absatz,
            onPlaybackStart = startCallback,
            onComplete = {
                if (sequenceId != currentSequenceId) {
                    // Inzwischen abgebrochen — nichts mehr aufrufen.
                    return@speakSingle
                }
                if (isLast) {
                    allDone()
                } else {
                    speakAbsatzAt(absaetze, index + 1, sequenceId,
                                  firstStartCallback, allDone)
                }
            },
        )
    }

    /**
     * Spricht einen einzelnen Text-Block (Absatz oder kompletter Originaltext)
     * mit dem aktuell gewaehlten Provider. Routing-Logik wie zuvor — nur
     * direkt aufgerufen statt aus speak(), damit die Absatz-Pipeline darueber
     * laufen kann.
     */
    private fun speakSingle(
        text: String,
        onPlaybackStart: (() -> Unit)?,
        onComplete: () -> Unit,
    ) {
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
    }

    fun stop() {
        // Sequenz-ID inkrementieren — die laufende speakAbsatzAt-Kette merkt
        // beim naechsten Schritt dass sie abgebrochen wurde und startet keinen
        // weiteren Absatz mehr.
        currentSequenceId++
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
