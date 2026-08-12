package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.data.repository.KiTriggerRepository
import de.frank.entropyreducer.domain.notifications.AppNotification
import kotlinx.coroutines.flow.first

/**
 * Trigger-Polling-Worker (Spec §16.2) — alle 15 Min.
 *
 * Iteriert über alle aktiven (genehmigten) KI-Trigger und prueft, ob ihre
 * Klartext-Bedingung gerade zutrifft. Aktuelle Auswertungs-Schritte:
 *  - Phrasen-basiertes Matching auf den letzten Biomarker (HRV, Recovery,
 *    Sleep). Beispiel: "HRV unter 35 ms am Morgen" → Schwellwert-Vergleich.
 *  - Wird ein Trigger erfuellt UND seit dem letzten Feuern sind >= 6h
 *    vergangen, gilt er als gefeuert: triggerCount + lastTriggeredAt werden
 *    aktualisiert und eine Notification erinnert den Benutzer.
 *
 * Komplexere Bedingungen (mehrere Variablen, kombinierte Regeln) sind ein
 * Folge-Refactoring — das Pattern hier reicht für die haeufigsten Vorschlaege
 * der Trigger-Engine.
 */
@HiltWorker
class TriggerPollingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val triggerRepo: KiTriggerRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val notifier: ShiftAwareNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val active = triggerRepo.observeActive().first()
            if (active.isEmpty()) return Result.success()
            val latest = biomarkerDao.getLatest().first()
            if (latest == null) {
                Diag.i(DiagnosticArea.AGENTIC, TAG, "Kein Biomarker-Snapshot vorhanden, überspringe Polling.")
                return Result.success()
            }
            val now = System.currentTimeMillis()
            active.forEach { trigger ->
                val cooldownExpired = trigger.lastTriggeredAt == null ||
                    now - trigger.lastTriggeredAt >= COOLDOWN_MS
                if (!cooldownExpired) return@forEach
                if (matchesCondition(trigger.condition, latest)) {
                    Diag.i(DiagnosticArea.AGENTIC, TAG, "Trigger '${trigger.name}' feuert: ${trigger.condition}")
                    triggerRepo.markFired(trigger, now)
                    try {
                        notifier.postOrDelay(
                            kind = AppNotification.TRIGGER_FEUERT,
                            notificationId = NOTIFICATION_BASE + trigger.id.hashCode().mod(1000),
                            title = trigger.name,
                            body = trigger.proposedAction,
                        )
                    } catch (cancellation: kotlinx.coroutines.CancellationException) {
                        throw cancellation
                    } catch (t: Throwable) {
                        Diag.w(DiagnosticArea.AGENTIC, TAG, "Trigger gespeichert, Benachrichtigung fehlgeschlagen", t)
                    }
                }
            }
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "TriggerPollingWorker fehlgeschlagen", t)
            retryOrFailure()
        }
    }

    /**
     * Phrasen-Matcher für typische Bedingungen.
     * Erkennt Muster wie:
     *   - "HRV unter 35 ms"
     *   - "Recovery unter 50%"
     *   - "Sleep Performance unter 70%"
     * Groß-/Kleinschreibung wird ignoriert. Bei nicht erkannter Phrase: false.
     */
    private fun matchesCondition(condition: String, latest: BiomarkerSnapshotEntity): Boolean {
        val c = condition.lowercase()
        val numberRegex = Regex("(\\d+)([\\.,]\\d+)?")
        val number = numberRegex.find(c)?.value?.replace(',', '.')?.toDoubleOrNull() ?: return false
        return when {
            "hrv" in c && ("unter" in c || "<" in c) ->
                latest.hrvMs?.let { it < number } == true
            "hrv" in c && ("über" in c || "über" in c || ">" in c) ->
                latest.hrvMs?.let { it > number } == true
            "recovery" in c && ("unter" in c || "<" in c) ->
                latest.recoveryScore?.let { it < number.toInt() } == true
            "recovery" in c && ("über" in c || "über" in c || ">" in c) ->
                latest.recoveryScore?.let { it > number.toInt() } == true
            "sleep" in c && ("unter" in c || "<" in c) ->
                latest.sleepPerformance?.let { it < number.toInt() } == true
            "sleep" in c && ("über" in c || "über" in c || ">" in c) ->
                latest.sleepPerformance?.let { it > number.toInt() } == true
            else -> false
        }
    }

    companion object {
        private const val TAG = "TriggerPollingWorker"
        private const val NOTIFICATION_BASE = 5000
        private const val COOLDOWN_MS = 6 * 60 * 60 * 1000L
        const val UNIQUE_NAME_PERIODIC = "trigger-polling-periodic"
    }
}
