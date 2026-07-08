package de.frank.entropyreducer.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.MentalSentenceDao
import de.frank.entropyreducer.data.local.entities.MentalEntity
import de.frank.entropyreducer.data.safety.PhoneContentGuard
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.presentation.mental.mentalsFromJsonFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ID-Architektur Etappe 4b (Frank-Wunsch 2026-06-19): einmaliger, idempotenter Umzug der Mental-Board-
 * Saetze aus dem DataStore "mental_board" (JSON "mentals_json") in die Tabelle `mental_sentences`
 * (position = Listen-Index, damit die manuelle Reihenfolge erhalten bleibt).
 *
 * Gleiche Sicherheits-Doktrin wie [IdeaTaskRoomMigrator]/[HabitRoomMigrator]: JSON wird NUR gelesen
 * (bleibt als Fallback), idempotent (Flag + Room-leer-Check), Anzahl-Abgleich-Sonde, Fehler werden
 * geloggt statt geworfen. Aufruf: [EntropyReducerApp.onCreate] via [migrateIfNeeded].
 */
@Singleton
class MentalRoomMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mentalSentenceDao: MentalSentenceDao,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun migrateIfNeeded() {
        applicationScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                        if (!prefs.getBoolean(KEY_DONE, false)) runMigration()
                        cleanupSecondBrainArtifacts()
                    }
                    .onFailure { ex ->
                        Diag.w(
                            DiagnosticArea.DATABASE,
                            TAG,
                            "Migration fehlgeschlagen -> Retry beim naechsten Start (JSON unangetastet)",
                            ex,
                        )
                    }
            }
        }
    }

    private suspend fun runMigration() {
        // Doppelschutz: nur kopieren wenn Room noch leer ist.
        val countBefore = mentalSentenceDao.count()
        if (countBefore > 0) {
            Diag.i(
                DiagnosticArea.DATABASE,
                TAG,
                "Uebersprungen: Room bereits befuellt (mental_sentences=$countBefore) -> Flag gesetzt",
            )
            prefs.edit { putBoolean(KEY_DONE, true) }
            return
        }

        // JSON lesen (nur lesen) — dedizierte Quelle, NICHT die in 4c auf Room umgestellte mentalsFlow.
        val mentals = mentalsFromJsonFlow(context).first()
        val entities =
            mentals.mapIndexed { index, m ->
                MentalEntity(
                    id = m.id,
                    text = m.text,
                    updatedAt = m.updatedAt,
                    position = index,
                )
            }.filterNot { entity -> PhoneContentGuard.isSecondBrainWorkArtifact(null, entity.text) }
        if (entities.isNotEmpty()) mentalSentenceDao.upsertAll(entities)

        // Anzahl-Abgleich (Live-Logik-Sonde / CHECKPOINT).
        val written = mentalSentenceDao.count()
        val ok = written == entities.size
        Diag.i(
            DiagnosticArea.DATABASE,
            TAG,
            "CHECKPOINT migration=mental_sentences erwartet=${entities.size} tatsaechlich=$written ok=$ok",
        )

        if (ok) {
            prefs.edit { putBoolean(KEY_DONE, true) }
            Diag.i(
                DiagnosticArea.DATABASE,
                TAG,
                "Migration erfolgreich abgeschlossen. JSON-Liste bleibt als Fallback erhalten.",
            )
        } else {
            Diag.w(
                DiagnosticArea.DATABASE,
                TAG,
                "Anzahl-Mismatch -> Flag NICHT gesetzt, Retry beim naechsten Start. JSON unangetastet.",
            )
        }
    }

    private suspend fun cleanupSecondBrainArtifacts() {
        var removed = 0
        for (mental in mentalSentenceDao.getAllForBackup()) {
            if (PhoneContentGuard.isSecondBrainWorkArtifact(null, mental.text)) {
                mentalSentenceDao.deleteById(mental.id)
                removed++
            }
        }
        if (removed > 0) {
            Diag.w(
                DiagnosticArea.DATABASE,
                TAG,
                "CHECKPOINT cleanup=mental_phone_artifacts expected=not_on_phone actual=removed=$removed ok=true",
            )
        }
    }

    companion object {
        private const val TAG = "EREMentalMigrator"
        private const val PREFS_NAME = "entropy_reducer_id_arch_migration"
        private const val KEY_DONE = "id_arch_mentals_v1_done"
    }
}
