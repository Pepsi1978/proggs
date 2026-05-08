package de.frank.entropyreducer.data.remote.drive

import android.util.Log
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EREDriveBackup"

/**
 * Schreibt eine JSON-Backup-Datei in den `appDataFolder` des Google-Drive-Kontos.
 * Wird der File-Name bereits gefunden, ueberschreibt ein `update` den Inhalt
 * — sonst wird ein `create` mit `parents=appDataFolder` aufgerufen.
 *
 * Mehrfach-Vorkommen (passieren bei sehr seltenen Race-Conditions zwischen zwei
 * Geraeten) werden auf den juengsten konsolidiert; alle aelteren werden geloescht.
 */
@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: DriveSession,
    private val secrets: EncryptedSecretsStore,
) {

    /** Datei-Name im appDataFolder. Versionierter Suffix erlaubt zukuenftige Migration. */
    private val fileName = "entropy_reducer_entries_v1.json"

    suspend fun upload(jsonContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "upload() start, payloadBytes=${jsonContent.toByteArray(Charsets.UTF_8).size}")
        runUploadWithRetry(jsonContent, retryOn401 = true)
    }

    private suspend fun runUploadWithRetry(jsonContent: String, retryOn401: Boolean): Result<Unit> {
        return try {
            doUpload(jsonContent)
            secrets.driveLastBackupEpochMs = System.currentTimeMillis()
            Log.d(TAG, "upload() success")
            Result.success(Unit)
        } catch (e: GoogleJsonResponseException) {
            Log.e(TAG, "Google JSON error: status=${e.statusCode} ${e.statusMessage}")
            Log.e(TAG, "Body: ${e.details?.toString() ?: e.content ?: "(no body)"}")
            if (e.statusCode == 401 && retryOn401) {
                Log.w(TAG, "401 Unauthorized — Token invalidieren und einmal retry")
                session.invalidateToken()
                runUploadWithRetry(jsonContent, retryOn401 = false)
            } else {
                Result.failure(e)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "upload() failed: ${t.javaClass.simpleName}: ${t.message}", t)
            Result.failure(t)
        }
    }

    private suspend fun doUpload(jsonContent: String) {
        val drive = session.get()
        Log.d(TAG, "Drive client erhalten, listing existing backup files…")

        val matches = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$fileName' and trashed = false")
            .setOrderBy("modifiedTime desc")
            .setFields("files(id, modifiedTime)")
            .execute()
            .files
            .orEmpty()
        Log.d(TAG, "list() ok, ${matches.size} existing match(es)")

        val media = ByteArrayContent("application/json", jsonContent.toByteArray(Charsets.UTF_8))
        val firstId = matches.firstOrNull()?.id

        if (firstId != null) {
            Log.d(TAG, "update existing fileId=$firstId")
            drive.files().update(firstId, null, media).execute()
            matches.drop(1).forEach { stale ->
                runCatching { drive.files().delete(stale.id).execute() }
            }
        } else {
            Log.d(TAG, "create new file in appDataFolder")
            val metadata = DriveFile().apply {
                name = fileName
                parents = listOf("appDataFolder")
            }
            drive.files().create(metadata, media).setFields("id").execute()
        }
    }
}
