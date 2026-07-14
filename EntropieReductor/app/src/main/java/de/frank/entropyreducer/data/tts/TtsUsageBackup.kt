package de.frank.entropyreducer.data.tts

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.remote.drive.DriveBackupManager
import de.frank.entropyreducer.data.remote.drive.DriveRestoreManager
import de.frank.entropyreducer.util.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Bindeglied zwischen dem geraetelokalen [TtsUsageStore] und dem Google-Drive-Backup
 * (Frank-Wunsch 2026-07-03). Haelt den Store selbst Drive-agnostisch.
 *
 * Zwei Aufgaben:
 *  1. **Sichern bei 50k-Schwelle** ([backupNow]): Immer wenn [TtsUsageStore.add] meldet, dass eine
 *     neue 50.000er-Grenze ueberschritten wurde, wird der aktuelle Stand als EINE kleine Datei
 *     (`entropy_reducer_tts_usage_v1.json`) in den Drive-appDataFolder geschrieben — ueberschreibend,
 *     also immer nur EIN Wert. NICHT bei jedem Zeichen (sonst liefe das Backup beim Vorlesen dauernd).
 *  2. **Wiederherstellen beim App-Start** ([restoreOnStart]): Ist der Drive-Wert groesser als der
 *     lokale (und vom selben Monat), wird der lokale Zaehler hochgezogen — so ueberlebt der Stand
 *     eine Neuinstallation (Genauigkeit +-50.000, bewusst akzeptiert).
 *
 * Fehler (kein Netz, nicht bei Drive angemeldet) werden bewusst geschluckt: Der Zaehler ist ein
 * Komfort-Feature; ein fehlgeschlagenes Backup/Restore darf weder das Vorlesen noch den App-Start
 * stoeren (Direktive #3 — funktionserhaltend, nichts crasht). Die Datei ist kanonisch und wird immer
 * ueberschrieben (der DriveBackupManager macht list-then-update) — keine Waisen, kein Quota-Thema.
 */
@Singleton
class TtsUsageBackup
@Inject
constructor(
    private val usageStore: TtsUsageStore,
    private val backupManager: DriveBackupManager,
    private val restoreManager: DriveRestoreManager,
) {

    private companion object {
        const val TAG = "TtsUsageBackup"
    }

    // Eigener, app-weiter Scope: das 50k-Backup laeuft fire-and-forget, damit das Vorlesen nicht
    // auf den Netz-Roundtrip wartet. SupervisorJob -> ein Fehler killt den Scope nicht.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json { ignoreUnknownKeys = true }

    /** Fire-and-forget: schreibt den aktuellen Zaehlerstand ins Drive (bei 50k-Schwelle aufgerufen). */
    fun backupNow() {
        scope.launch {
            runCatching {
                val snap = usageStore.snapshot()
                val payload = json.encodeToString(
                    TtsUsageBackupPayload.serializer(),
                    TtsUsageBackupPayload(month = snap.month, chars = snap.chars),
                )
                backupManager.uploadTtsUsage(payload).getOrThrow()
                Diag.d(
                    DiagnosticArea.DRIVE_BACKUP,
                    TAG,
                    "TTS-Usage-Backup geschrieben: ${snap.chars} Zeichen (Monat ${snap.month})",
                )
            }.onFailure {
                Diag.w(
                    DiagnosticArea.DRIVE_BACKUP,
                    TAG,
                    "TTS-Usage-Backup fehlgeschlagen (ignoriert): ${it.message}",
                )
            }
        }
    }

    /**
     * Beim App-Start aufrufen: holt den Drive-Wert und hebt den lokalen Zaehler an, falls groesser
     * (gleicher Monat). Suspend — der Aufrufer startet es in einem eigenen Scope (z.B.
     * `lifecycleScope`). Schluckt alle Fehler.
     */
    suspend fun restoreOnStart() {
        runCatchingCancellable {
            val content = restoreManager.fetchTtsUsage().getOrThrow()
            if (content.isNullOrBlank()) {
                Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "Kein TTS-Usage-Backup auf Drive — nichts wiederherzustellen")
                return
            }
            val payload = json.decodeFromString(TtsUsageBackupPayload.serializer(), content)
            usageStore.applyRestored(payload.month, payload.chars)
        }.onFailure {
            Diag.w(
                DiagnosticArea.DRIVE_BACKUP,
                TAG,
                "TTS-Usage-Restore fehlgeschlagen (ignoriert): ${it.message}",
            )
        }
    }
}

/** Drive-Persistenzformat des TTS-Zaehlers — genau EIN Wert (Monat + kumulierte Zeichen). */
@Serializable
data class TtsUsageBackupPayload(
    val month: String,
    val chars: Long,
)
