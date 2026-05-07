package de.frank.entropyreducer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.domain.kiquestion.KiQuestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Speichert die aktuell aktive KI-Frage und den letzten Trigger-Check. */
private val Context.kiQuestionStore by preferencesDataStore(name = "ki_question_state")

@Singleton
class KiQuestionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val currentQuestion: Flow<KiQuestion?> = context.kiQuestionStore.data.map { prefs ->
        val key = prefs[KEY_TRIGGER_KEY] ?: return@map null
        val text = prefs[KEY_TEXT] ?: return@map null
        val rationale = prefs[KEY_RATIONALE].orEmpty()
        val ids = prefs[KEY_RELATED_IDS]?.split('|')?.filter { it.isNotBlank() }.orEmpty()
        KiQuestion(triggerKey = key, text = text, rationale = rationale, relatedEntryIds = ids)
    }

    val dismissedUntilMs: Flow<Long> =
        context.kiQuestionStore.data.map { it[KEY_DISMISSED_UNTIL] ?: 0L }

    suspend fun setCurrent(question: KiQuestion?) {
        context.kiQuestionStore.edit { prefs ->
            if (question == null) {
                prefs.remove(KEY_TRIGGER_KEY)
                prefs.remove(KEY_TEXT)
                prefs.remove(KEY_RATIONALE)
                prefs.remove(KEY_RELATED_IDS)
            } else {
                prefs[KEY_TRIGGER_KEY] = question.triggerKey
                prefs[KEY_TEXT] = question.text
                prefs[KEY_RATIONALE] = question.rationale
                prefs[KEY_RELATED_IDS] = question.relatedEntryIds.joinToString("|")
            }
        }
    }

    /** Verschiebt die aktuelle Frage um 24h. */
    suspend fun snoozeFor24Hours() {
        context.kiQuestionStore.edit { prefs ->
            prefs[KEY_DISMISSED_UNTIL] = System.currentTimeMillis() + 24L * 60 * 60 * 1000
            prefs.remove(KEY_TRIGGER_KEY)
            prefs.remove(KEY_TEXT)
            prefs.remove(KEY_RATIONALE)
            prefs.remove(KEY_RELATED_IDS)
        }
    }

    companion object {
        private val KEY_TRIGGER_KEY = stringPreferencesKey("trigger_key")
        private val KEY_TEXT = stringPreferencesKey("question_text")
        private val KEY_RATIONALE = stringPreferencesKey("rationale")
        private val KEY_RELATED_IDS = stringPreferencesKey("related_ids")
        private val KEY_DISMISSED_UNTIL = longPreferencesKey("dismissed_until_ms")
    }
}
