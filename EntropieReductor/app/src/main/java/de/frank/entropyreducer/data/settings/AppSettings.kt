package de.frank.entropyreducer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Drei Theme-Modi: dem System folgen, immer hell, immer dunkel. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

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

    /** Letzter erfolgreicher Whoop-Sync (Epoch-Millisekunden). 0L = noch nie gesynct. */
    val lastWhoopSyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_WHOOP_SYNC] ?: 0L }
    /** Letzter erfolgreicher Calendar-Sync (Epoch-Millisekunden). */
    val lastCalendarSyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_CALENDAR_SYNC] ?: 0L }
    /** Frank-Wunsch 2026-05-10: Pro Biomarker-Quelle eigener Sync-Zeitstempel —
     *  damit der Header "Zuletzt synchronisiert" das ALTESTE Sync-Datum aller
     *  vier Quellen anzeigen kann (= "ist alles aktuell?"-Zeitpunkt). */
    val lastOuraSyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_OURA_SYNC] ?: 0L }
    val lastAmazfitSyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_AMAZFIT_SYNC] ?: 0L }
    val lastHealthConnectSyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_HEALTH_CONNECT_SYNC] ?: 0L }
    /** Letzter Lauf der KI-Frage-des-Moments (Epoch-Millisekunden). */
    val lastKiQuestionCheckMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_KI_QUESTION] ?: 0L }

    /** Zwischengespeicherte Markdown-Analyse aus Dashboard 2 (Spec §11.1.7). */
    val cachedAnalysisMarkdownFlow: Flow<String> = ds.data.map { it[KEY_CACHED_ANALYSIS] ?: "" }
    val cachedAnalysisAtMsFlow: Flow<Long> = ds.data.map { it[KEY_CACHED_ANALYSIS_AT] ?: 0L }
    /** Letzte Genie-Codex-Synthese (Epoch-Millisekunden). */
    val lastCodexSyntheseMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_CODEX_SYNTHESE] ?: 0L }

    /** Aktuelles Tagesbriefing (Markdown). Wird vom DailyBriefingWorker geschrieben. */
    val dailyBriefingTextFlow: Flow<String> = ds.data.map { it[KEY_DAILY_BRIEFING_TEXT] ?: "" }
    /** Datum des aktuellen Tagesbriefings im ISO-Format (yyyy-MM-dd). Leer wenn keins. */
    val dailyBriefingDateFlow: Flow<String> = ds.data.map { it[KEY_DAILY_BRIEFING_DATE] ?: "" }
    /** Zeitstempel der letzten Generierung des Tagesbriefings (Epoch-Millisekunden). */
    val dailyBriefingGeneratedAtMsFlow: Flow<Long> = ds.data.map { it[KEY_DAILY_BRIEFING_AT] ?: 0L }

    /** Letzter Wochenrueckblick (Markdown), gespeichert vom WeeklyReviewWorker. */
    val lastWeeklyReviewTextFlow: Flow<String> = ds.data.map { it[KEY_WEEKLY_REVIEW_TEXT] ?: "" }
    val lastWeeklyReviewAtMsFlow: Flow<Long> = ds.data.map { it[KEY_WEEKLY_REVIEW_AT] ?: 0L }
    /** Letzter Monatsrueckblick (Markdown). */
    val lastMonthlyReviewTextFlow: Flow<String> = ds.data.map { it[KEY_MONTHLY_REVIEW_TEXT] ?: "" }
    val lastMonthlyReviewAtMsFlow: Flow<Long> = ds.data.map { it[KEY_MONTHLY_REVIEW_AT] ?: 0L }

    /**
     * Theme-Modus als Flow. Default: SYSTEM (folgt der Hell-/Dunkel-Einstellung
     * des Geraets). Manueller Override via Toggle in der Top-Bar.
     */
    val themeModeFlow: Flow<ThemeMode> = ds.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setWhisperModel(value: String) = ds.edit { it[KEY_WHISPER_MODEL] = value }
    suspend fun setGeminiModel(value: String) = ds.edit { it[KEY_GEMINI_MODEL] = value }
    suspend fun setTranscriptionLanguage(value: String) = ds.edit { it[KEY_LANGUAGE] = value }
    suspend fun setTtsVoice(value: String) = ds.edit { it[KEY_TTS_VOICE] = value }
    suspend fun setProfileText(value: String) = ds.edit { it[KEY_PROFILE_TEXT] = value }
    suspend fun setThemeMode(value: ThemeMode) = ds.edit { it[KEY_THEME_MODE] = value.name }

    suspend fun setLastWhoopSync(value: Long) = ds.edit { it[KEY_LAST_WHOOP_SYNC] = value }
    suspend fun setLastCalendarSync(value: Long) = ds.edit { it[KEY_LAST_CALENDAR_SYNC] = value }
    suspend fun setLastOuraSync(value: Long) = ds.edit { it[KEY_LAST_OURA_SYNC] = value }
    suspend fun setLastAmazfitSync(value: Long) = ds.edit { it[KEY_LAST_AMAZFIT_SYNC] = value }
    suspend fun setLastHealthConnectSync(value: Long) = ds.edit { it[KEY_LAST_HEALTH_CONNECT_SYNC] = value }
    suspend fun setLastKiQuestionCheck(value: Long) = ds.edit { it[KEY_LAST_KI_QUESTION] = value }

    suspend fun setCachedAnalysis(markdown: String, atMs: Long) = ds.edit {
        it[KEY_CACHED_ANALYSIS] = markdown
        it[KEY_CACHED_ANALYSIS_AT] = atMs
    }
    suspend fun setLastCodexSynthese(value: Long) = ds.edit { it[KEY_LAST_CODEX_SYNTHESE] = value }

    /** Speichert ein neues Tagesbriefing — atomar Text + Datum + Zeitstempel. */
    suspend fun setDailyBriefing(text: String, isoDate: String, atMs: Long) = ds.edit {
        it[KEY_DAILY_BRIEFING_TEXT] = text
        it[KEY_DAILY_BRIEFING_DATE] = isoDate
        it[KEY_DAILY_BRIEFING_AT] = atMs
    }

    suspend fun setWeeklyReview(text: String, atMs: Long) = ds.edit {
        it[KEY_WEEKLY_REVIEW_TEXT] = text
        it[KEY_WEEKLY_REVIEW_AT] = atMs
    }

    suspend fun setMonthlyReview(text: String, atMs: Long) = ds.edit {
        it[KEY_MONTHLY_REVIEW_TEXT] = text
        it[KEY_MONTHLY_REVIEW_AT] = atMs
    }

    companion object {
        private val KEY_WHISPER_MODEL = stringPreferencesKey("whisper_model")
        private val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        private val KEY_LANGUAGE = stringPreferencesKey("transcription_lang")
        private val KEY_TTS_VOICE = stringPreferencesKey("tts_voice")
        private val KEY_PROFILE_TEXT = stringPreferencesKey("profile_text")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LAST_WHOOP_SYNC = longPreferencesKey("last_whoop_sync_ms")
        private val KEY_LAST_CALENDAR_SYNC = longPreferencesKey("last_calendar_sync_ms")
        private val KEY_LAST_OURA_SYNC = longPreferencesKey("last_oura_sync_ms")
        private val KEY_LAST_AMAZFIT_SYNC = longPreferencesKey("last_amazfit_sync_ms")
        private val KEY_LAST_HEALTH_CONNECT_SYNC = longPreferencesKey("last_health_connect_sync_ms")
        private val KEY_LAST_KI_QUESTION = longPreferencesKey("last_ki_question_check_ms")
        private val KEY_CACHED_ANALYSIS = stringPreferencesKey("cached_analysis_markdown")
        private val KEY_CACHED_ANALYSIS_AT = longPreferencesKey("cached_analysis_at_ms")
        private val KEY_LAST_CODEX_SYNTHESE = longPreferencesKey("last_codex_synthese_ms")
        private val KEY_DAILY_BRIEFING_TEXT = stringPreferencesKey("daily_briefing_text")
        private val KEY_DAILY_BRIEFING_DATE = stringPreferencesKey("daily_briefing_date")
        private val KEY_DAILY_BRIEFING_AT = longPreferencesKey("daily_briefing_at_ms")
        private val KEY_WEEKLY_REVIEW_TEXT = stringPreferencesKey("weekly_review_text")
        private val KEY_WEEKLY_REVIEW_AT = longPreferencesKey("weekly_review_at_ms")
        private val KEY_MONTHLY_REVIEW_TEXT = stringPreferencesKey("monthly_review_text")
        private val KEY_MONTHLY_REVIEW_AT = longPreferencesKey("monthly_review_at_ms")

        const val DEFAULT_WHISPER = "whisper-large-v3-turbo"
        // Frank-Wunsch 2026-05-09: Default-Modell ist Gemini 3.1 Flash-Lite. Greift
        // bei jeder Neuinstallation (frischer DataStore = Fallback auf diesen Wert)
        // und bei bestehenden Installationen wo der Nutzer das Modell nicht aktiv
        // im Modell-Picker geaendert hat. Wer es einmal selbst umstellt, behaelt
        // seine Auswahl ueber App-Updates hinweg — nur Neuinstallation oder Datenwipe
        // setzt zurueck auf diesen Default.
        const val DEFAULT_GEMINI = "gemini-3.1-flash-lite"
    }
}
