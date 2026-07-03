package de.frank.entropyreducer.data.remote.drive

import android.content.Context
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "EREDriveRestore"

/**
 * Holt die juengste Backup-Datei aus dem appDataFolder zurück. Liefert `null`, wenn auf Drive
 * nichts liegt — der Aufrufer entscheidet, ob das ein erwarteter "frischer" Zustand oder ein Fehler
 * ist.
 */
@Singleton
class DriveRestoreManager
@Inject
constructor(@ApplicationContext private val context: Context, private val session: DriveSession) {

    private val mainFileName = "entropy_reducer_entries_v1.json"
    private val workoutsFileName = "entropy_reducer_workouts_v1.json"
    private val healthFileName = "entropy_reducer_health_v1.json"

    /** TTS-Zaehlerstand (Monat + kumulierte Zeichen). Frank-Wunsch 2026-07-03. */
    private val ttsUsageFileName = "entropy_reducer_tts_usage_v1.json"

    /** Haupt-Backup (Aufgaben/Insights/Memories/etc.). */
    suspend fun fetchLatest(): Result<String?> = fetchFile(mainFileName)

    /** Holt den TTS-Zaehlerstand aus dem appDataFolder (oder `null`, wenn keiner existiert). */
    suspend fun fetchTtsUsage(): Result<String?> = fetchFile(ttsUsageFileName)

    /**
     * Frank-Wunsch 2026-05-19: Separates Workouts-Backup mit den teuren Detail-Streams (GPS-Track,
     * Pulsverlauf, Pace, Splits). Eigene Datei `entropy_reducer_workouts_v1.json` im appDataFolder.
     */
    suspend fun fetchWorkouts(): Result<String?> = fetchFile(workoutsFileName)

    /**
     * Frank-Wunsch 2026-05-19 (Erweiterung): Separates Whoop+Oura-Health-Backup. Eigene Datei
     * `entropy_reducer_health_v1.json` mit Daily-Recovery/HRV, Whoop-Workouts und allen 6
     * Oura-Tabellen (Readiness, Sleep, Activity, Resilience, SleepDetail, PersonalInfo).
     */
    suspend fun fetchHealth(): Result<String?> = fetchFile(healthFileName)

    private suspend fun fetchFile(fileName: String): Result<String?> =
        withContext(Dispatchers.IO) { fetchFileInternal(fileName, retryOn401 = true) }

    private suspend fun fetchFileInternal(fileName: String, retryOn401: Boolean): Result<String?> {
        return try {
            val drive = session.get()

            val match =
                drive
                    .files()
                    .list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$fileName' and trashed = false")
                    .setOrderBy("modifiedTime desc")
                    .setFields("files(id, modifiedTime)")
                    .setPageSize(1)
                    .execute()
                    .files
                    ?.firstOrNull() ?: return Result.success(null)

            val sink = ByteArrayOutputStream()
            drive.files().get(match.id).executeMediaAndDownloadTo(sink)
            // ByteArrayOutputStream.toString(Charset) braucht API 33+ — auf API 28-32
            // würde das mit NoSuchMethodError crashen. String(ByteArray, Charset) gibt's
            // seit API 1, gleicher Output, kompatibel auf allen unterstuetzten Versionen.
            Result.success(String(sink.toByteArray(), Charsets.UTF_8))
        } catch (e: GoogleJsonResponseException) {
            Diag.e(DiagnosticArea.DRIVE_BACKUP, TAG, "Google JSON error ($fileName): status=${e.statusCode} ${e.statusMessage}")
            if (e.statusCode == 401 && retryOn401) {
                Diag.w(DiagnosticArea.DRIVE_BACKUP, TAG, "401 Unauthorized ($fileName) — Token invalidieren und einmal retry")
                session.invalidateToken()
                fetchFileInternal(fileName, retryOn401 = false)
            } else {
                Result.failure(e)
            }
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.DRIVE_BACKUP, TAG, "fetchFile($fileName) failed: ${t.javaClass.simpleName}: ${t.message}", t)
            Result.failure(t)
        }
    }

    /** Existiert ein Haupt-Backup auf Drive? */
    suspend fun hasBackup(): Boolean =
        withContext(Dispatchers.IO) { hasBackupInternal(mainFileName, retryOn401 = true) }

    private suspend fun hasBackupInternal(fileName: String, retryOn401: Boolean): Boolean {
        return try {
            val drive = session.get()
            drive
                .files()
                .list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("files(id)")
                .setPageSize(1)
                .execute()
                .files
                ?.isNotEmpty() == true
        } catch (e: GoogleJsonResponseException) {
            if (e.statusCode == 401 && retryOn401) {
                Diag.w(DiagnosticArea.DRIVE_BACKUP, TAG, "hasBackup 401 ($fileName) — Token invalidieren und einmal retry")
                session.invalidateToken()
                hasBackupInternal(fileName, retryOn401 = false)
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }
}
