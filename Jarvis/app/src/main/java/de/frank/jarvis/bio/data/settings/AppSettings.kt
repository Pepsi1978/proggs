package de.frank.jarvis.bio.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.jarvis.bio.domain.usecase.WhoopAbrufPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.bioDataStore by preferencesDataStore(name = "bio_settings")

/**
 * Nicht geheime Stände des Biomarker-Bereichs (aus Entropie Reductor übernommen, dort `app_settings`): letzte Abrufe je
 * Quelle, Whoop-Abrufsteuerung, Ergebnis des letzten Abgleichs, gelöschte Trainings. Schlüsselnamen wie in Entropie Reductor,
 * damit die Übernahme sie 1:1 einspielen kann ([alsJson] / [uebernimm]).
 */
class AppSettings(context: Context) {
    private val ds = context.applicationContext.bioDataStore

    val lastWhoopSyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_WHOOP_SYNC] ?: 0L }
    val lastWhoopGapFillMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_WHOOP_GAP_FILL] ?: 0L }
    val lastWhoopAttemptMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_WHOOP_ATTEMPT] ?: 0L }
    val lastOuraSyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_OURA_SYNC] ?: 0L }
    val lastAmazfitSyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_AMAZFIT_SYNC] ?: 0L }
    val lastZeppBodySyncMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_ZEPP_BODY_SYNC] ?: 0L }
    val lastRefreshFooterFlow: Flow<String> = ds.data.map { it[KEY_LAST_REFRESH_FOOTER] ?: "" }.distinctUntilChanged()
    val lastRefreshFooterAtMsFlow: Flow<Long> = ds.data.map { it[KEY_LAST_REFRESH_FOOTER_AT] ?: 0L }.distinctUntilChanged()
    /** Bis zu diesem Tag (yyyy-MM-dd) wurden Whoop-Lücken schon einmal nachgefragt; "" = noch nie. */
    val whoopLueckenGeprueftBisFlow: Flow<String> = ds.data.map { it[KEY_WHOOP_LUECKEN_GEPRUEFT_BIS] ?: "" }
    /** Ergebnis des letzten Biomarker-Abgleichs je Quelle als JSON (siehe ApiSyncBericht). "" = noch keiner. */
    val biomarkerSyncStatusFlow: Flow<String> = ds.data.map { it[KEY_BIOMARKER_SYNC_STATUS] ?: "" }.distinctUntilChanged()

    suspend fun setLastWhoopSync(value: Long) = ds.edit { it[KEY_LAST_WHOOP_SYNC] = value }
    suspend fun setLastWhoopGapFill(value: Long) = ds.edit { it[KEY_LAST_WHOOP_GAP_FILL] = value }
    suspend fun setLastWhoopAttempt(value: Long) = ds.edit { it[KEY_LAST_WHOOP_ATTEMPT] = value }
    suspend fun setWhoopLueckenGeprueftBis(tag: String) = ds.edit { it[KEY_WHOOP_LUECKEN_GEPRUEFT_BIS] = tag }
    suspend fun setLastOuraSync(value: Long) = ds.edit { it[KEY_LAST_OURA_SYNC] = value }
    suspend fun setLastAmazfitSync(value: Long) = ds.edit { it[KEY_LAST_AMAZFIT_SYNC] = value }
    suspend fun setLastZeppBodySync(value: Long) = ds.edit { it[KEY_LAST_ZEPP_BODY_SYNC] = value }
    suspend fun setBiomarkerSyncStatus(json: String) = ds.edit { it[KEY_BIOMARKER_SYNC_STATUS] = json }
    suspend fun setLastRefreshFooter(text: String, atMs: Long) = ds.edit {
        it[KEY_LAST_REFRESH_FOOTER] = text
        it[KEY_LAST_REFRESH_FOOTER_AT] = atMs
    }

    /** Tombstones für manuell gelöschte Trainings: Der nächste Health-Connect-Abruf überspringt Sessions mit diesem Start (±5 min). */
    suspend fun addDeletedWorkoutStart(startMs: Long) = ds.edit { prefs ->
        prefs[KEY_DELETED_WORKOUTS] = (prefs[KEY_DELETED_WORKOUTS] ?: emptySet()) + startMs.toString()
    }
    suspend fun getDeletedWorkoutStarts(): Set<Long> =
        ds.data.first()[KEY_DELETED_WORKOUTS]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    suspend fun isPolarFullResyncV1Done(): Boolean = ds.data.map { it[KEY_POLAR_FULL_RESYNC_V1] ?: false }.first()
    suspend fun setPolarFullResyncV1Done(value: Boolean) = ds.edit { it[KEY_POLAR_FULL_RESYNC_V1] = value }

    /** Stand der zentralen Whoop-Abrufsteuerung ([WhoopAbrufPlan]). */
    val whoopAbrufStandFlow: Flow<WhoopAbrufPlan.Stand> = ds.data.map { whoopAbrufStandAus(it) }

    private fun whoopAbrufStandAus(prefs: Preferences): WhoopAbrufPlan.Stand {
        val text = prefs[KEY_WHOOP_ABRUF_STAND]
        return if (text.isNullOrBlank()) {
            WhoopAbrufPlan.Stand(
                letzterVersuchMs = maxOf(prefs[KEY_LAST_WHOOP_ATTEMPT] ?: 0L, prefs[KEY_LAST_WHOOP_SYNC] ?: 0L),
                letzterErfolgMs = prefs[KEY_LAST_WHOOP_SYNC] ?: 0L,
            )
        } else {
            WhoopAbrufPlan.Stand.ausJson(text)
        }
    }

    /** Ändert den Whoop-Abrufstand in einem Schritt (Lesen und Schreiben ohne fremden Schreiber dazwischen). */
    suspend fun aendereWhoopAbrufStand(aenderung: (WhoopAbrufPlan.Stand) -> WhoopAbrufPlan.Stand) {
        ds.edit { prefs -> prefs[KEY_WHOOP_ABRUF_STAND] = aenderung(whoopAbrufStandAus(prefs)).alsJson() }
    }

    /**
     * Neuester Messzeitpunkt je Quelle beim letzten Abgleich (JSON id → ms). Damit unterscheidet der Quellenstatus
     * „Abruf geglückt, aber nichts Neues“ von „neue Messwerte“.
     */
    val neuesteMessungFlow: Flow<String> = ds.data.map { it[KEY_NEUESTE_MESSUNG] ?: "" }
    suspend fun setNeuesteMessung(json: String) = ds.edit { it[KEY_NEUESTE_MESSUNG] = json }

    /**
     * Übernahme aus Entropie Reductor: deren Werte zu den Schlüsseln oben (Namen wie dort). Unbekanntes wird übergangen.
     */
    suspend fun uebernimm(werte: org.json.JSONObject) = ds.edit { prefs ->
        LANG.forEach { key -> if (werte.has(key.name)) prefs[key] = werte.optLong(key.name) }
        TEXT.forEach { key -> if (werte.has(key.name)) prefs[key] = werte.optString(key.name) }
        if (werte.has(KEY_POLAR_FULL_RESYNC_V1.name)) prefs[KEY_POLAR_FULL_RESYNC_V1] = werte.optBoolean(KEY_POLAR_FULL_RESYNC_V1.name)
        werte.optJSONArray(KEY_DELETED_WORKOUTS.name)?.let { a -> prefs[KEY_DELETED_WORKOUTS] = (0 until a.length()).map { a.optString(it) }.toSet() }
    }

    companion object {
        val KEY_LAST_WHOOP_SYNC = longPreferencesKey("last_whoop_sync_ms")
        val KEY_LAST_WHOOP_GAP_FILL = longPreferencesKey("last_whoop_gap_fill_ms")
        val KEY_LAST_WHOOP_ATTEMPT = longPreferencesKey("last_whoop_attempt_ms")
        val KEY_LAST_OURA_SYNC = longPreferencesKey("last_oura_sync_ms")
        val KEY_LAST_AMAZFIT_SYNC = longPreferencesKey("last_amazfit_sync_ms")
        val KEY_LAST_ZEPP_BODY_SYNC = longPreferencesKey("last_zepp_body_sync_ms")
        val KEY_LAST_REFRESH_FOOTER_AT = longPreferencesKey("last_refresh_footer_at_ms")
        val KEY_LAST_REFRESH_FOOTER = stringPreferencesKey("last_refresh_footer_text")
        val KEY_WHOOP_LUECKEN_GEPRUEFT_BIS = stringPreferencesKey("whoop_luecken_geprueft_bis")
        val KEY_BIOMARKER_SYNC_STATUS = stringPreferencesKey("biomarker_sync_status_json")
        val KEY_WHOOP_ABRUF_STAND = stringPreferencesKey("whoop_abruf_stand_json")
        val KEY_NEUESTE_MESSUNG = stringPreferencesKey("bio_neueste_messung_json")
        val KEY_DELETED_WORKOUTS = stringSetPreferencesKey("deleted_workout_starts")
        val KEY_POLAR_FULL_RESYNC_V1 = booleanPreferencesKey("polar_full_resync_v1_done")
        private val LANG = listOf(KEY_LAST_WHOOP_SYNC, KEY_LAST_WHOOP_GAP_FILL, KEY_LAST_WHOOP_ATTEMPT, KEY_LAST_OURA_SYNC, KEY_LAST_AMAZFIT_SYNC, KEY_LAST_ZEPP_BODY_SYNC, KEY_LAST_REFRESH_FOOTER_AT)
        private val TEXT = listOf(KEY_LAST_REFRESH_FOOTER, KEY_WHOOP_LUECKEN_GEPRUEFT_BIS, KEY_BIOMARKER_SYNC_STATUS, KEY_WHOOP_ABRUF_STAND)
    }
}
