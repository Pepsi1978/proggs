package de.frank.entropyreducer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    val ttsAutoStopMinutesFlow: Flow<Int> = ds.data
        .map { (it[KEY_TTS_AUTO_STOP_MINUTES] ?: DEFAULT_TTS_AUTO_STOP_MINUTES).coerceIn(15, 120) }
        .distinctUntilChanged()
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

    suspend fun readHealthConnectPermissionSignature(): String =
        ds.data.first()[KEY_HEALTH_CONNECT_PERMISSION_SIGNATURE].orEmpty()
    /** SHA-256 des zuletzt hochgeladenen Workouts-Backups. Leer = nie. */
    val workoutsBackupFingerprintFlow: Flow<String> =
        ds.data.map { it[KEY_WORKOUTS_BACKUP_SHA256] ?: "" }
    /** Fingerprint des zuletzt hochgeladenen Haupt-Backups (Performance 2026-05-23, Vorschlag 2) —
     *  inhaltsbasierter Hash des Payloads OHNE exportedAt. Aendert er sich nicht, wird der
     *  Haupt-Upload uebersprungen. 0 = noch nie hochgeladen. */
    val mainBackupFingerprintFlow: Flow<String> =
        ds.data.map { it[KEY_MAIN_BACKUP_SHA256] ?: "" }
    /** Fingerprint des zuletzt hochgeladenen Health-Backups (Whoop/Oura, Performance 2026-05-24) —
     *  inhaltsbasierter Hash OHNE exportedAt. Unveraendert -> Health-Upload uebersprungen. 0 = nie. */
    val healthBackupFingerprintFlow: Flow<String> =
        ds.data.map { it[KEY_HEALTH_BACKUP_SHA256] ?: "" }
    /** Letzter Lauf der KI-Frage-des-Moments (Epoch-Millisekunden). */
    val lastKiQuestionCheckMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_KI_QUESTION] ?: 0L }

    /**
     * Frank-Wunsch 2026-05-17: Persistenter Footer-Text fuer den Biomarker-
     * Sync-Status. Wird vom BiomarkerViewModel nach jedem refreshNow() in
     * DataStore geschrieben — beim App-Neustart liest der ViewModel den letzten
     * Stand wieder rein, damit der Footer NICHT verschwindet.
     */
    val lastRefreshFooterFlow: Flow<String> = ds.data.map { it[KEY_LAST_REFRESH_FOOTER] ?: "" }.distinctUntilChanged()
    val lastRefreshFooterAtMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_REFRESH_FOOTER_AT] ?: 0L }.distinctUntilChanged()

    // Performance-Audit Loop 2 (2026-05-10): distinctUntilChanged auf alle
    // Markdown-Flows. Vorher emittierte ds.data bei jeder beliebigen DataStore-
    // Schreiboperation einen neuen Wert auf ALLEN map-Flows — ein Sync-Timestamp-
    // Update auf KEY_LAST_OURA_SYNC triggerte z.B. eine Re-Emission auf
    // dailyBriefingTextFlow mit dem identischen 2-10 KB Markdown. Mit
    // distinctUntilChanged emittiert nur der wirklich geaenderte Flow.
    /** Zwischengespeicherte Markdown-Analyse aus Dashboard 2 (Spec §11.1.7). */
    val cachedAnalysisMarkdownFlow: Flow<String> = ds.data.map { it[KEY_CACHED_ANALYSIS] ?: "" }.distinctUntilChanged()
    val cachedAnalysisAtMsFlow: Flow<Long> = ds.data.map { it[KEY_CACHED_ANALYSIS_AT] ?: 0L }.distinctUntilChanged()
    /** Letzte Genie-Codex-Synthese (Epoch-Millisekunden). */
    val lastCodexSyntheseMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_CODEX_SYNTHESE] ?: 0L }.distinctUntilChanged()

    /** Aktuelles Tagesbriefing (Markdown). Wird vom DailyBriefingWorker geschrieben. */
    val dailyBriefingTextFlow: Flow<String> = ds.data.map { it[KEY_DAILY_BRIEFING_TEXT] ?: "" }.distinctUntilChanged()
    /** Datum des aktuellen Tagesbriefings im ISO-Format (yyyy-MM-dd). Leer wenn keins. */
    val dailyBriefingDateFlow: Flow<String> = ds.data.map { it[KEY_DAILY_BRIEFING_DATE] ?: "" }.distinctUntilChanged()
    /** Zeitstempel der letzten Generierung des Tagesbriefings (Epoch-Millisekunden). */
    val dailyBriefingGeneratedAtMsFlow: Flow<Long> = ds.data.map { it[KEY_DAILY_BRIEFING_AT] ?: 0L }.distinctUntilChanged()

    /** Letzter Wochenrueckblick (Markdown), gespeichert vom WeeklyReviewWorker. */
    val lastWeeklyReviewTextFlow: Flow<String> = ds.data.map { it[KEY_WEEKLY_REVIEW_TEXT] ?: "" }.distinctUntilChanged()
    val lastWeeklyReviewAtMsFlow: Flow<Long> = ds.data.map { it[KEY_WEEKLY_REVIEW_AT] ?: 0L }.distinctUntilChanged()
    /** Letzter Monatsrueckblick (Markdown). */
    val lastMonthlyReviewTextFlow: Flow<String> = ds.data.map { it[KEY_MONTHLY_REVIEW_TEXT] ?: "" }.distinctUntilChanged()
    val lastMonthlyReviewAtMsFlow: Flow<Long> = ds.data.map { it[KEY_MONTHLY_REVIEW_AT] ?: 0L }.distinctUntilChanged()

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

    /**
     * Theme-Modus speziell fuer das Home-Screen-Widget (Frank-Wunsch 2026-05-11).
     * Bewusst entkoppelt vom App-Theme: Frank kann das Widget hell stellen
     * obwohl die App dunkel ist, und umgekehrt. Default SYSTEM = folgt
     * Geraete-Einstellung (also nicht zwingend an App-Theme gekoppelt, weil
     * Widget oft ausserhalb der App sichtbar ist).
     */
    val widgetThemeModeFlow: Flow<ThemeMode> = ds.data.map { prefs ->
        when (prefs[KEY_WIDGET_THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }.distinctUntilChanged()

    suspend fun setWidgetThemeMode(value: ThemeMode) = ds.edit {
        it[KEY_WIDGET_THEME_MODE] = value.name
    }

    /**
     * Suspend-Read fuer den Widget-Worker (provideGlance laeuft in einem
     * Coroutine-Scope, kein collect-Flow noetig).
     */
    suspend fun readWidgetThemeModeOnce(): ThemeMode {
        val prefs = ds.data.first()
        return when (prefs[KEY_WIDGET_THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    /**
     * Prioritätsfilter im Widget. Wenn true, zeigt das Aufgaben-Widget nur den
     * höchsten Prioritätsbereich. Default false = alle Bereiche.
     */
    val widgetOnlyTodayFlow: Flow<Boolean> = ds.data
        .map { it[KEY_WIDGET_ONLY_TODAY] ?: false }
        .distinctUntilChanged()

    suspend fun readWidgetOnlyTodayOnce(): Boolean =
        ds.data.first()[KEY_WIDGET_ONLY_TODAY] ?: false

    suspend fun setWidgetOnlyToday(value: Boolean) = ds.edit {
        it[KEY_WIDGET_ONLY_TODAY] = value
    }

    /**
     * Hintergrund-Deckkraft des Widgets (Frank-Wunsch 2026-05-11).
     * Float 0.0 (komplett transparent) bis 1.0 (volle Deckkraft). Default 1.0
     * = wie vorher (kein Transparenz-Effekt).
     */
    val widgetBgAlphaFlow: Flow<Float> = ds.data
        .map { (it[KEY_WIDGET_BG_ALPHA] ?: 1.0f).coerceIn(0.0f, 1.0f) }
        .distinctUntilChanged()

    suspend fun readWidgetBgAlphaOnce(): Float =
        (ds.data.first()[KEY_WIDGET_BG_ALPHA] ?: 1.0f).coerceIn(0.0f, 1.0f)

    suspend fun setWidgetBgAlpha(value: Float) = ds.edit {
        it[KEY_WIDGET_BG_ALPHA] = value.coerceIn(0.0f, 1.0f)
    }

    /**
     * Atomarer Toggle des Widget-Prioritätsfilters (Frank-Wunsch 2026-05-11, Bugfix).
     *
     * Vorher: read-then-write in zwei separaten Calls (readWidgetOnlyTodayOnce
     * + setWidgetOnlyToday). Bei zwei schnellen Klicks lasen beide Activity-
     * Instanzen den GLEICHEN Wert (DataStore hatte den ersten Write noch nicht
     * committed) → beide setzten true → keine sichtbare Änderung.
     *
     * Jetzt: ein einziger ds.edit { }-Block — DataStore garantiert dort
     * Mutual Exclusion. Zwei parallele Toggle-Aufrufe togglen sauber den Wert
     * (false → true → false), statt sich gegenseitig zu überschreiben.
     *
     * @return der NEUE Wert nach dem Toggle (für Logging/Tests)
     */
    suspend fun toggleWidgetOnlyToday(): Boolean {
        var newValue = false
        ds.edit { prefs ->
            val current = prefs[KEY_WIDGET_ONLY_TODAY] ?: false
            newValue = !current
            prefs[KEY_WIDGET_ONLY_TODAY] = newValue
        }
        return newValue
    }

    suspend fun setWhisperModel(value: String) = ds.edit { it[KEY_WHISPER_MODEL] = value }
    suspend fun setGeminiModel(value: String) = ds.edit { it[KEY_GEMINI_MODEL] = value }
    suspend fun setTranscriptionLanguage(value: String) = ds.edit { it[KEY_LANGUAGE] = value }
    suspend fun setTtsVoice(value: String) = ds.edit { it[KEY_TTS_VOICE] = value }
    suspend fun setTtsAutoStopMinutes(value: Int) = ds.edit {
        it[KEY_TTS_AUTO_STOP_MINUTES] = value.coerceIn(15, 120)
    }
    suspend fun setProfileText(value: String) = ds.edit { it[KEY_PROFILE_TEXT] = value }
    suspend fun setThemeMode(value: ThemeMode) = ds.edit { it[KEY_THEME_MODE] = value.name }

    suspend fun setLastWhoopSync(value: Long) = ds.edit { it[KEY_LAST_WHOOP_SYNC] = value }
    suspend fun setLastCalendarSync(value: Long) = ds.edit { it[KEY_LAST_CALENDAR_SYNC] = value }
    suspend fun setLastOuraSync(value: Long) = ds.edit { it[KEY_LAST_OURA_SYNC] = value }
    suspend fun setLastAmazfitSync(value: Long) = ds.edit { it[KEY_LAST_AMAZFIT_SYNC] = value }

    /**
     * Frank-Bugfix 2026-07-04: Tombstones fuer manuell geloeschte Trainings. Ohne diese Liste
     * wuerde der naechste Health-Connect-Sync ein geloeschtes Training sofort wieder importieren.
     * Gespeichert wird der Start-Zeitstempel (Epoch-ms); der Sync ueberspringt Sessions, deren
     * Start (+/- 5 Min) hier steht.
     */
    suspend fun addDeletedWorkoutStart(startMs: Long) = ds.edit { prefs ->
        prefs[KEY_DELETED_WORKOUTS] = (prefs[KEY_DELETED_WORKOUTS] ?: emptySet()) + startMs.toString()
    }

    suspend fun getDeletedWorkoutStarts(): Set<Long> =
        ds.data.first()[KEY_DELETED_WORKOUTS]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    suspend fun setHealthConnectSyncState(value: Long, permissionSignature: String) = ds.edit {
        it[KEY_LAST_HEALTH_CONNECT_SYNC] = value
        it[KEY_HEALTH_CONNECT_PERMISSION_SIGNATURE] = permissionSignature
    }

    /** Reset-/Override-Pfad für bestehende UI-Aktionen; die Permission-Signatur bleibt erhalten. */
    suspend fun setLastHealthConnectSync(value: Long) = ds.edit {
        it[KEY_LAST_HEALTH_CONNECT_SYNC] = value
    }
    suspend fun setWorkoutsBackupFingerprint(value: String) =
        ds.edit { it[KEY_WORKOUTS_BACKUP_SHA256] = value }
    suspend fun setMainBackupFingerprint(value: String) =
        ds.edit { it[KEY_MAIN_BACKUP_SHA256] = value }
    suspend fun setHealthBackupFingerprint(value: String) =
        ds.edit { it[KEY_HEALTH_BACKUP_SHA256] = value }
    suspend fun setLastKiQuestionCheck(value: Long) = ds.edit { it[KEY_LAST_KI_QUESTION] = value }

    /**
     * Frank-Wunsch 2026-05-17: Persistenten Footer-Text + Zeitstempel atomar
     * speichern. Wird vom BiomarkerViewModel.refreshNow() nach jedem Sync
     * aufgerufen — beim App-Neustart sieht Frank den letzten Sync-Status
     * weiterhin, statt eines leeren Footers.
     */
    suspend fun setLastRefreshFooter(text: String, atMs: Long) = ds.edit {
        it[KEY_LAST_REFRESH_FOOTER] = text
        it[KEY_LAST_REFRESH_FOOTER_AT] = atMs
    }

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

    /**
     * Frank-Wunsch 2026-05-16: einmalige Workout-Cleanup-Migration. Wird beim
     * App-Start ausgefuehrt — loescht alle bestehenden amazfit_workouts und
     * triggert einen Drive-Sync damit das Backup mit dem leeren Stand ueberschrieben
     * wird. Backup-Logik selbst bleibt erhalten — Polar-Integration kann spaeter
     * sauber einsteigen.
     */
    suspend fun isWorkoutCleanupV1Done(): Boolean = ds.data.map { it[KEY_WORKOUT_CLEANUP_V1] ?: false }.first()
    suspend fun setWorkoutCleanupV1Done(value: Boolean) = ds.edit { it[KEY_WORKOUT_CLEANUP_V1] = value }

    /**
     * Frank-Wunsch 2026-05-17: Zweite Workout-Cleanup-Migration. Reduziert die
     * Trainings auf "nur die letzten 2 Jahre bis 30.03.2026 17:25" — alle aelteren
     * und alle neueren (Polar-Duplikate vom 17.05., 14.05., 09.05., 08.05., 01.05.)
     * werden geloescht. Idempotent: laeuft genau EINMAL pro App-Installation.
     */
    suspend fun isWorkoutCleanupV2Done(): Boolean = ds.data.map { it[KEY_WORKOUT_CLEANUP_V2] ?: false }.first()
    suspend fun setWorkoutCleanupV2Done(value: Boolean) = ds.edit { it[KEY_WORKOUT_CLEANUP_V2] = value }

    /**
     * Frank-Wunsch 2026-05-17: einmalige Sportarten-Umbenennung.
     * "Indoor-Rudern" / "Rudergeraet" → "Crosstrainer" (Polar zeichnet Crosstrainer
     * unter diesen Codes auf). Laeuft genau EINMAL pro App-Installation.
     */
    suspend fun isSportRenameV1Done(): Boolean = ds.data.map { it[KEY_SPORT_RENAME_V1] ?: false }.first()
    suspend fun setSportRenameV1Done(value: Boolean) = ds.edit { it[KEY_SPORT_RENAME_V1] = value }

    /**
     * Frank-Wunsch 2026-05-17 (zweite Iteration): "Funktionelles Training" ist
     * bei Frank's Polar tatsaechlich Trailrunning (alle Eintraege haben Distanz,
     * GPS und Pulsverlauf). Migration setzt sportName um → VO2max-Anzeige greift
     * automatisch dank isVo2MaxSport("trail").
     */
    suspend fun isSportRenameV2Done(): Boolean = ds.data.map { it[KEY_SPORT_RENAME_V2] ?: false }.first()
    suspend fun setSportRenameV2Done(value: Boolean) = ds.edit { it[KEY_SPORT_RENAME_V2] = value }

    /**
     * Frank-Wunsch 2026-05-17: Polar-Quellen (V3 AccessLink, V4, Flow Web, TCX,
     * JSON-Bulk) kurzfristig komplett deaktivieren — Health Connect bleibt aktive
     * Trainings-Quelle. Default true. Wenn false: Polar-Workers laufen wieder
     * (periodischer Sync, Foreground-Trigger, manueller Refresh-Button).
     *
     * Code bleibt vollstaendig erhalten — Frank kann Polar jederzeit per
     * setDisablePolarSync(false) oder per ADB-DataStore-Edit wieder anschalten.
     */
    val disablePolarSyncFlow: Flow<Boolean> = ds.data
        .map { it[KEY_DISABLE_POLAR_SYNC] ?: true }
        .distinctUntilChanged()

    suspend fun isPolarSyncDisabled(): Boolean = ds.data.map { it[KEY_DISABLE_POLAR_SYNC] ?: true }.first()
    suspend fun setDisablePolarSync(value: Boolean) = ds.edit { it[KEY_DISABLE_POLAR_SYNC] = value }

    /**
     * Frank-Wunsch 2026-05-31: einmalige Titel-Migration. Alle bestehenden Aufgaben
     * mit mehr als 3 Woertern im Titel werden EINMAL per KI auf max. 3 Woerter
     * gekuerzt (neue Aufgaben sind ohnehin schon auf 3 Woerter begrenzt). Laeuft
     * genau EINMAL pro App-Installation, sobald ein Gemini-Key vorhanden ist.
     */
    suspend fun isTitleShortenV1Done(): Boolean = ds.data.map { it[KEY_TITLE_SHORTEN_V1] ?: false }.first()
    suspend fun setTitleShortenV1Done(value: Boolean) = ds.edit { it[KEY_TITLE_SHORTEN_V1] = value }

    /**
     * Prioritaets-Gedaechtnis (Frank-Wunsch 2026-06-19): An/Aus + einstellbares Limit.
     * enabled steuert Lernen UND Anwenden (Default an). limit = wie viele neueste Eintraege die KI
     * beim Abgleich beruecksichtigt (Default 300, eingegrenzt 10..2000 — aelteste darueber werden
     * beim Abgleich ignoriert, bleiben aber gespeichert).
     */
    val priorityMemoryEnabledFlow: Flow<Boolean> = ds.data
        .map { it[KEY_PRIO_MEMORY_ENABLED] ?: true }
        .distinctUntilChanged()

    suspend fun setPriorityMemoryEnabled(value: Boolean) = ds.edit {
        it[KEY_PRIO_MEMORY_ENABLED] = value
    }

    val priorityMemoryLimitFlow: Flow<Int> = ds.data
        .map { (it[KEY_PRIO_MEMORY_LIMIT] ?: 300).coerceIn(10, 2000) }
        .distinctUntilChanged()

    suspend fun setPriorityMemoryLimit(value: Int) = ds.edit {
        it[KEY_PRIO_MEMORY_LIMIT] = value.coerceIn(10, 2000)
    }

    /** Second-Brain-Connector: schreibt App-Bereiche automatisch in ihre Brain-Kategorie. */
    fun secondBrainConnectorEnabledFlow(areaKey: String): Flow<Boolean> = ds.data
        .map { prefs ->
            prefs[secondBrainEnabledKey(areaKey)]
                ?: (if (areaKey == "learning") prefs[secondBrainEnabledKey("entropy")] else null)
                ?: false
        }
        .distinctUntilChanged()

    val secondBrainIdeasConnectorEnabledFlow: Flow<Boolean> = secondBrainConnectorEnabledFlow("ideas")

    suspend fun setSecondBrainConnectorEnabled(areaKey: String, value: Boolean) = ds.edit {
        it[secondBrainEnabledKey(areaKey)] = value
    }

    suspend fun setSecondBrainIdeasConnectorEnabled(value: Boolean) = ds.edit {
        it[KEY_SECOND_BRAIN_IDEAS_ENABLED] = value
    }

    suspend fun readSecondBrainSyncStamps(areaKey: String): Set<String> =
        ds.data.first()[secondBrainSyncStampsKey(areaKey)] ?: emptySet()

    suspend fun markSecondBrainSynced(areaKey: String, rowId: String, stamp: String) = ds.edit { prefs ->
        val key = secondBrainSyncStampsKey(areaKey)
        val withoutOldStamp = (prefs[key] ?: emptySet())
            .filterNot { it.startsWith("$rowId:") }
            .toSet()
        prefs[key] = withoutOldStamp + stamp
    }

    suspend fun readSecondBrainTitles(areaKey: String): Map<String, String> =
        (ds.data.first()[secondBrainTitlesKey(areaKey)] ?: emptySet())
            .mapNotNull { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()

    suspend fun setSecondBrainTitle(areaKey: String, rowId: String, title: String) = ds.edit { prefs ->
        val key = secondBrainTitlesKey(areaKey)
        val without = (prefs[key] ?: emptySet())
            .filterNot { it.startsWith("$rowId=") }
            .toSet()
        prefs[key] = without + "$rowId=$title"
    }

    /** Stamp und Remote-Titel gehoeren zu derselben bestaetigten Server-Version. */
    suspend fun markSecondBrainSynced(
        areaKey: String,
        rowId: String,
        stamp: String,
        title: String,
    ) = ds.edit { prefs ->
        val stampKey = secondBrainSyncStampsKey(areaKey)
        val titleKey = secondBrainTitlesKey(areaKey)
        prefs[stampKey] = (prefs[stampKey] ?: emptySet())
            .filterNot { it.startsWith("$rowId:") }
            .toSet() + stamp
        prefs[titleKey] = (prefs[titleKey] ?: emptySet())
            .filterNot { it.startsWith("$rowId=") }
            .toSet() + "$rowId=$title"
    }

    suspend fun clearSecondBrainSync(areaKey: String, rowId: String) = ds.edit { prefs ->
        val stampKey = secondBrainSyncStampsKey(areaKey)
        val titleKey = secondBrainTitlesKey(areaKey)
        prefs[stampKey] = (prefs[stampKey] ?: emptySet()).filterNot { it.startsWith("$rowId:") }.toSet()
        prefs[titleKey] = (prefs[titleKey] ?: emptySet()).filterNot { it.startsWith("$rowId=") }.toSet()
    }

    suspend fun clearAllSecondBrainSync(areaKey: String) = ds.edit { prefs ->
        prefs.remove(secondBrainSyncStampsKey(areaKey))
        prefs.remove(secondBrainTitlesKey(areaKey))
    }

    suspend fun readSecondBrainIdeaSyncStamps(): Set<String> =
        ds.data.first()[KEY_SECOND_BRAIN_IDEA_SYNC_STAMPS] ?: emptySet()

    suspend fun markSecondBrainIdeaSynced(ideaId: String, stamp: String) = ds.edit { prefs ->
        val withoutOldStamp = (prefs[KEY_SECOND_BRAIN_IDEA_SYNC_STAMPS] ?: emptySet())
            .filterNot { it.startsWith("$ideaId:") }
            .toSet()
        prefs[KEY_SECOND_BRAIN_IDEA_SYNC_STAMPS] = withoutOldStamp + stamp
    }

    // Frank-Wunsch 2026-07-04: Pro Idee den zuletzt ins Second Brain hochgeladenen TITEL merken.
    // Noetig, um beim Loeschen einer Idee (oder bei Titel-Aenderung) den alten Brain-Eintrag per
    // Titel (DELETE /by-title) gezielt zu entfernen — der Brain nutzt den Titel als Schluessel.
    // Format je Element: "<ideaId>=<titel>" — die ideaId ist eine UUID (enthaelt kein '='), der
    // Titel danach darf '=' enthalten (split limit=2 trennt nur am ersten '=').
    suspend fun readSecondBrainIdeaTitles(): Map<String, String> =
        (ds.data.first()[KEY_SECOND_BRAIN_IDEA_TITLES] ?: emptySet())
            .mapNotNull { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()

    suspend fun setSecondBrainIdeaTitle(ideaId: String, title: String) = ds.edit { prefs ->
        val without = (prefs[KEY_SECOND_BRAIN_IDEA_TITLES] ?: emptySet())
            .filterNot { it.startsWith("$ideaId=") }
            .toSet()
        prefs[KEY_SECOND_BRAIN_IDEA_TITLES] = without + "$ideaId=$title"
    }

    /** Entfernt Sync-Marke UND gemerkten Titel einer Idee (nach Loeschung aus dem Brain). */
    suspend fun clearSecondBrainIdeaSync(ideaId: String) = ds.edit { prefs ->
        prefs[KEY_SECOND_BRAIN_IDEA_SYNC_STAMPS] =
            (prefs[KEY_SECOND_BRAIN_IDEA_SYNC_STAMPS] ?: emptySet())
                .filterNot { it.startsWith("$ideaId:") }.toSet()
        prefs[KEY_SECOND_BRAIN_IDEA_TITLES] =
            (prefs[KEY_SECOND_BRAIN_IDEA_TITLES] ?: emptySet())
                .filterNot { it.startsWith("$ideaId=") }.toSet()
    }

    /** Setzt ALLE Ideen-Sync-Marken + Titel zurueck — fuer den vollstaendigen Neu-Sync
     *  (danach gilt jede Idee als „noch nicht hochgeladen" und wird frisch geschrieben). */
    suspend fun clearAllSecondBrainIdeaSync() = ds.edit { prefs ->
        prefs.remove(KEY_SECOND_BRAIN_IDEA_SYNC_STAMPS)
        prefs.remove(KEY_SECOND_BRAIN_IDEA_TITLES)
    }

    // ------------------------------------------------------------------
    // Benachrichtigungen (Frank-Wunsch 2026-08-12)
    // ------------------------------------------------------------------
    // Pro Benachrichtigungs-Art ein Schalter. Der Schluessel kommt aus
    // AppNotification.key und darf sich NIE aendern. Default ist bewusst true —
    // eine neu eingebaute Benachrichtigung verhaelt sich damit wie bisher, der
    // Benutzer schaltet sie bei Bedarf ab.

    /** Beobachtbarer Schalter einer Benachrichtigungs-Art (Default: an). */
    fun notificationEnabledFlow(notificationKey: String): Flow<Boolean> = ds.data
        .map { it[notificationEnabledKey(notificationKey)] ?: true }
        .distinctUntilChanged()

    /** Einmal-Lesung fuer Worker/Notifier kurz vor dem Senden. */
    suspend fun isNotificationEnabled(notificationKey: String): Boolean =
        ds.data.first()[notificationEnabledKey(notificationKey)] ?: true

    suspend fun setNotificationEnabled(notificationKey: String, value: Boolean) = ds.edit {
        it[notificationEnabledKey(notificationKey)] = value
    }

    companion object {
        private val KEY_WHISPER_MODEL = stringPreferencesKey("whisper_model")
        private val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        private val KEY_LANGUAGE = stringPreferencesKey("transcription_lang")
        private val KEY_TTS_VOICE = stringPreferencesKey("tts_voice")
        private val KEY_TTS_AUTO_STOP_MINUTES = intPreferencesKey("tts_auto_stop_minutes")
        private val KEY_PROFILE_TEXT = stringPreferencesKey("profile_text")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_WIDGET_THEME_MODE = stringPreferencesKey("widget_theme_mode")
        private val KEY_WIDGET_ONLY_TODAY = booleanPreferencesKey("widget_only_today")
        private val KEY_WIDGET_BG_ALPHA = floatPreferencesKey("widget_bg_alpha")
        private val KEY_LAST_WHOOP_SYNC = longPreferencesKey("last_whoop_sync_ms")
        private val KEY_LAST_CALENDAR_SYNC = longPreferencesKey("last_calendar_sync_ms")
        private val KEY_LAST_OURA_SYNC = longPreferencesKey("last_oura_sync_ms")
        private val KEY_LAST_AMAZFIT_SYNC = longPreferencesKey("last_amazfit_sync_ms")
        private val KEY_DELETED_WORKOUTS = stringSetPreferencesKey("deleted_workout_starts")
        private val KEY_LAST_HEALTH_CONNECT_SYNC = longPreferencesKey("last_health_connect_sync_ms")
        private val KEY_HEALTH_CONNECT_PERMISSION_SIGNATURE =
            stringPreferencesKey("health_connect_body_permission_signature")
        private val KEY_WORKOUTS_BACKUP_SHA256 =
            stringPreferencesKey("workouts_backup_fingerprint_sha256")
        private val KEY_MAIN_BACKUP_SHA256 =
            stringPreferencesKey("main_backup_fingerprint_sha256")
        private val KEY_HEALTH_BACKUP_SHA256 =
            stringPreferencesKey("health_backup_fingerprint_sha256")
        private val KEY_LAST_REFRESH_FOOTER = stringPreferencesKey("last_refresh_footer_text")
        private val KEY_LAST_REFRESH_FOOTER_AT = longPreferencesKey("last_refresh_footer_at_ms")
        private val KEY_WORKOUT_CLEANUP_V1 = booleanPreferencesKey("workout_cleanup_v1_done")
        private val KEY_WORKOUT_CLEANUP_V2 = booleanPreferencesKey("workout_cleanup_v2_done")
        private val KEY_SPORT_RENAME_V1 = booleanPreferencesKey("sport_rename_v1_done")
        private val KEY_SPORT_RENAME_V2 = booleanPreferencesKey("sport_rename_v2_done")
        private val KEY_DISABLE_POLAR_SYNC = booleanPreferencesKey("disable_polar_sync")
        private val KEY_TITLE_SHORTEN_V1 = booleanPreferencesKey("title_shorten_v1_done")
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
        // Prioritaets-Gedaechtnis (Frank-Wunsch 2026-06-19)
        private val KEY_PRIO_MEMORY_ENABLED = booleanPreferencesKey("prio_memory_enabled")
        private val KEY_PRIO_MEMORY_LIMIT = intPreferencesKey("prio_memory_limit")
        private val KEY_SECOND_BRAIN_IDEAS_ENABLED = booleanPreferencesKey("second_brain_ideas_enabled")
        private val KEY_SECOND_BRAIN_IDEA_SYNC_STAMPS = stringSetPreferencesKey("second_brain_idea_sync_stamps")
        private val KEY_SECOND_BRAIN_IDEA_TITLES = stringSetPreferencesKey("second_brain_idea_titles")

        private fun secondBrainEnabledKey(areaKey: String) =
            if (areaKey == "ideas") KEY_SECOND_BRAIN_IDEAS_ENABLED
            else booleanPreferencesKey("second_brain_${areaKey}_enabled")

        private fun secondBrainSyncStampsKey(areaKey: String) =
            if (areaKey == "ideas") KEY_SECOND_BRAIN_IDEA_SYNC_STAMPS
            else stringSetPreferencesKey("second_brain_${areaKey}_sync_stamps")

        private fun secondBrainTitlesKey(areaKey: String) =
            if (areaKey == "ideas") KEY_SECOND_BRAIN_IDEA_TITLES
            else stringSetPreferencesKey("second_brain_${areaKey}_titles")

        /** Schalter je Benachrichtigungs-Art (siehe AppNotification.key). */
        private fun notificationEnabledKey(notificationKey: String) =
            booleanPreferencesKey("notification_enabled_$notificationKey")

        const val DEFAULT_WHISPER = "whisper-large-v3-turbo"
        const val DEFAULT_TTS_AUTO_STOP_MINUTES = 15
        // Frank-Wunsch 2026-05-09: Default-Modell ist Gemini 3.1 Flash-Lite. Greift
        // bei jeder Neuinstallation (frischer DataStore = Fallback auf diesen Wert)
        // und bei bestehenden Installationen wo der Nutzer das Modell nicht aktiv
        // im Modell-Picker geaendert hat. Wer es einmal selbst umstellt, behaelt
        // seine Auswahl ueber App-Updates hinweg — nur Neuinstallation oder Datenwipe
        // setzt zurueck auf diesen Default.
        const val DEFAULT_GEMINI = "gemini-3.1-flash-lite"
    }
}
