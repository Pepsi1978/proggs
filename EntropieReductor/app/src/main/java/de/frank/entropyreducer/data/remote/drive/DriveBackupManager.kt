package de.frank.entropyreducer.data.remote.drive

import android.content.Context
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "EREDriveBackup"

/**
 * Schreibt eine JSON-Backup-Datei in den `appDataFolder` des Google-Drive-Kontos. Wird der
 * File-Name bereits gefunden, ueberschreibt ein `update` den Inhalt — sonst wird ein `create` mit
 * `parents=appDataFolder` aufgerufen.
 *
 * Mehrfach-Vorkommen (passieren bei sehr seltenen Race-Conditions zwischen zwei Geraeten) werden
 * auf den juengsten konsolidiert; alle aelteren werden geloescht.
 */
@Singleton
class DriveBackupManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val session: DriveSession,
    private val secrets: EncryptedSecretsStore,
    private val diagnostics: DiagnosticLogger,
) {

    /** Haupt-Backup: Aufgaben, Insights, Memories, Hypothesen, Forscher-Sessions etc. */
    private val mainFileName = "entropy_reducer_entries_v1.json"

    /**
     * Frank-Wunsch 2026-05-19: SEPARATES Backup nur fuer Sport-Trainings inkl. der teuren
     * Detail-Streams (GPS-Track, Pulsverlauf, Pace, Splits). Eigene Datei damit das Haupt-Backup
     * schlank bleibt und Workouts unabhaengig restored werden koennen — auch nach `adb uninstall`
     * braucht es dann keinen erneuten Polar-ZIP-Import mehr.
     */
    private val workoutsFileName = "entropy_reducer_workouts_v1.json"

    /**
     * Frank-Wunsch 2026-05-19 (Erweiterung): SEPARATES Backup fuer Whoop- und Oura-Daten. Whoop
     * liefert nur ~90 Tage rueckwirkend ueber die API, Oura ~6 Monate — ohne Backup waeren aeltere
     * Recovery-, HRV- und Schlaf-Daten nach Reinstall fuer immer verloren.
     */
    private val healthFileName = "entropy_reducer_health_v1.json"

    /**
     * Frank-Wunsch 2026-07-03: Kleiner separater Zaehlerstand des TTS-Monatskontingents (Chirp 3 HD),
     * damit der Verbrauch eine Neuinstallation ueberlebt. Genau EIN Wert (Monat + Zeichen), wird bei
     * jeder 50k-Schwelle ueberschrieben.
     */
    private val ttsUsageFileName = "entropy_reducer_tts_usage_v1.json"

    /** Lade Haupt-Backup hoch (Aufgaben/Insights/Memories/etc.) */
    suspend fun upload(jsonContent: String): Result<Unit> =
        uploadFile(mainFileName, jsonContent, label = "main")

    /** Lade den TTS-Zaehlerstand hoch (separate kleine Datei). Frank-Wunsch 2026-07-03. */
    suspend fun uploadTtsUsage(jsonContent: String): Result<Unit> =
        uploadFile(ttsUsageFileName, jsonContent, label = "tts_usage")

    /** Lade Workouts-Backup hoch (separate Datei). Frank-Wunsch 2026-05-19. */
    suspend fun uploadWorkouts(jsonContent: String): Result<Unit> =
        uploadFile(workoutsFileName, jsonContent, label = "workouts")

    /** Lade Whoop+Oura-Health-Backup hoch (separate Datei). Frank-Wunsch 2026-05-19. */
    suspend fun uploadHealth(jsonContent: String): Result<Unit> =
        uploadFile(healthFileName, jsonContent, label = "health")

    private suspend fun uploadFile(
        fileName: String,
        jsonContent: String,
        label: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "upload($label) start, payloadChars=${jsonContent.length}")
            runUploadWithRetry(fileName, jsonContent, retryOn401 = true)
                .onSuccess {
                    diagnostics.success(
                        DiagnosticArea.DRIVE_BACKUP,
                        "Backup OK ($label) — ${jsonContent.length} Zeichen hochgeladen",
                    )
                }
                .onFailure {
                    diagnostics.error(
                        DiagnosticArea.DRIVE_BACKUP,
                        "Backup fehlgeschlagen ($label): ${it.message ?: it::class.java.simpleName}",
                        it,
                    )
                }
        }

    private suspend fun runUploadWithRetry(
        fileName: String,
        jsonContent: String,
        retryOn401: Boolean,
    ): Result<Unit> {
        return try {
            doUpload(fileName, jsonContent)
            // Frank-Hinweis: Wir nehmen das HAUPT-Backup als kanonischen Zeitstempel.
            // Das Workouts-Backup laeuft direkt danach im SyncCoordinator und nutzt
            // das Hauptbackup-Timestamp ohnehin als "Letzter Backup"-Anzeige.
            if (fileName == mainFileName) {
                secrets.driveLastBackupEpochMs = System.currentTimeMillis()
            }
            Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "upload($fileName) success")
            Result.success(Unit)
        } catch (e: GoogleJsonResponseException) {
            Diag.e(DiagnosticArea.DRIVE_BACKUP, TAG, "Google JSON error ($fileName): status=${e.statusCode} ${e.statusMessage}")
            Diag.e(DiagnosticArea.DRIVE_BACKUP, TAG, "Body: ${e.details?.toString() ?: e.content ?: "(no body)"}")
            if (e.statusCode == 401 && retryOn401) {
                Diag.w(DiagnosticArea.DRIVE_BACKUP, TAG, "401 Unauthorized ($fileName) — Token invalidieren und einmal retry")
                session.invalidateToken()
                runUploadWithRetry(fileName, jsonContent, retryOn401 = false)
            } else {
                Result.failure(e)
            }
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.DRIVE_BACKUP, TAG, "upload($fileName) failed: ${t.javaClass.simpleName}: ${t.message}", t)
            Result.failure(t)
        }
    }

    private suspend fun doUpload(fileName: String, jsonContent: String) {
        val drive = session.get()
        Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "Drive client erhalten, listing existing backup files for $fileName…")

        val matches =
            drive
                .files()
                .list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setOrderBy("modifiedTime desc")
                .setFields("files(id, modifiedTime)")
                .execute()
                .files
                .orEmpty()
        Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "list($fileName) ok, ${matches.size} existing match(es)")

        val media = ByteArrayContent("application/json", jsonContent.toByteArray(Charsets.UTF_8))
        val firstId = matches.firstOrNull()?.id

        if (firstId != null) {
            Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "update existing fileId=$firstId ($fileName)")
            drive.files().update(firstId, null, media).execute()
            matches.drop(1).forEach { stale ->
                runCatching { drive.files().delete(stale.id).execute() }
            }
        } else {
            Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "create new file $fileName in appDataFolder")
            val metadata =
                DriveFile().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                }
            drive.files().create(metadata, media).setFields("id").execute()
        }
    }
}
