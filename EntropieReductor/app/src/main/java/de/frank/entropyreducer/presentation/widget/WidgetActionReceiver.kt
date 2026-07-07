package de.frank.entropyreducer.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Empfaengt die Taps auf die Widget-ListView-Items (Frank-Wunsch 2026-05-31).
 *
 * Das ListView-PendingIntentTemplate (im EntropyReducerWidgetReceiver) zeigt auf
 * diesen BroadcastReceiver. Je nach Action im FillInIntent:
 *  - ACTION_COMPLETE → Aufgabe DIREKT als erledigt (REDUZIERT) markieren, OHNE die
 *    App zu oeffnen. Danach das Widget refreshen, damit die Aufgabe verschwindet.
 *  - sonst (FOCUS / RESCHEDULE / OPEN / SETTINGS) → MainActivity oeffnen und die
 *    Extras durchreichen (Deep-Link wie zuvor ueber das Activity-Template).
 *
 * Warum ein BroadcastReceiver statt eines direkten Activity-Templates: pro ListView
 * gibt es nur EIN PendingIntentTemplate. Damit das Haekchen abhaken kann, OHNE die
 * App zu oeffnen, muss das Template ein Broadcast sein, der selbst entscheidet was
 * zu tun ist. Die App-oeffnenden Aktionen bleiben dabei vollstaendig erhalten.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ActionEntryPoint {
        fun entryDao(): EntropyEntryDao

        fun entryRepository(): de.frank.entropyreducer.data.repository.EntryRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(WidgetIntents.EXTRA_ACTION) ?: return
        val taskId = intent.getStringExtra(WidgetIntents.EXTRA_TASK_ID).orEmpty()

        if (action == WidgetIntents.ACTION_COMPLETE) {
            if (taskId.isBlank()) return
            completeTask(context.applicationContext, taskId)
        } else if (action == WidgetIntents.ACTION_TOGGLE_BUCKET) {
            // Bucket-Sektion auf-/zuklappen (Akkordeon) — OHNE die App zu oeffnen.
            val bucket = intent.getStringExtra(WidgetIntents.EXTRA_BUCKET).orEmpty()
            if (bucket.isBlank()) return
            WidgetExpandState.toggle(bucket)
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    WidgetUpdater.updateAll(context.applicationContext)
                } finally {
                    pending.finish()
                }
            }
        } else {
            // App oeffnen und Deep-Link durchreichen (wie das fruehere Activity-Template).
            val launch = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(WidgetIntents.EXTRA_TASK_ID, taskId)
                putExtra(WidgetIntents.EXTRA_ACTION, action)
            }
            context.startActivity(launch)
        }
    }

    /**
     * Hakt die Aufgabe ab — mit Mini-Animation (Frank-Wunsch 2026-05-31):
     *  Phase 1: ID im WidgetCheckState markieren + Widget refreshen → kurz erscheint
     *           ein gefuelltes gruenes Haekchen.
     *  (kurze Pause, damit das Haekchen wahrnehmbar ist)
     *  Phase 2: Aufgabe als REDUZIERT markieren (identisch zu markEntryResolved),
     *           ID wieder freigeben + Widget refreshen → die Aufgabe verschwindet.
     * goAsync haelt den Receiver am Leben, bis beide Phasen fertig sind.
     */
    private fun completeTask(appContext: Context, taskId: String) {
        val pending = goAsync()
        val entryPoint = EntryPointAccessors
            .fromApplication(appContext, ActionEntryPoint::class.java)
        val dao = entryPoint.entryDao()
        val repo = entryPoint.entryRepository()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Phase 1: Haekchen sofort sichtbar machen.
                WidgetCheckState.mark(taskId)
                WidgetUpdater.updateAll(appContext)
                delay(450)
                // Phase 2: Aufgabe wirklich erledigen.
                val entry = dao.getActive().first().firstOrNull { it.id == taskId }
                if (entry != null) {
                    val now = System.currentTimeMillis()
                    // Sync-Etappe 1.5: ueber das Repository (loest Drive-Backup-Sync aus) statt direkt
                    // am DAO — sonst synchronisiert ein Abhaken ueber das Home-Widget NICHT.
                    repo.update(
                        entry.copy(
                            status = EntryStatus.REDUZIERT,
                            resolvedAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                Diag.e(DiagnosticArea.APP, "WidgetActionReceiver", "completeTask fehlgeschlagen", e)
            } finally {
                // Haekchen-Markierung loesen und Widget final refreshen (Aufgabe ist weg).
                WidgetCheckState.clear(taskId)
                WidgetUpdater.updateAll(appContext)
                pending.finish()
            }
        }
    }
}
