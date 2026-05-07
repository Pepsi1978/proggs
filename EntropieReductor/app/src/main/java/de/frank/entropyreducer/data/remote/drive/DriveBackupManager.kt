package de.frank.entropyreducer.data.remote.drive

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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
        runCatching {
            val drive = session.get()

            val matches = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setOrderBy("modifiedTime desc")
                .setFields("files(id, modifiedTime)")
                .execute()
                .files
                .orEmpty()

            val media = ByteArrayContent("application/json", jsonContent.toByteArray(Charsets.UTF_8))
            val firstId = matches.firstOrNull()?.id

            if (firstId != null) {
                drive.files().update(firstId, null, media).execute()
                // Stale Duplikate aufraeumen
                matches.drop(1).forEach { stale ->
                    runCatching { drive.files().delete(stale.id).execute() }
                }
            } else {
                val metadata = DriveFile().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                }
                drive.files().create(metadata, media).setFields("id").execute()
            }

            secrets.driveLastBackupEpochMs = System.currentTimeMillis()
            Unit
        }
    }
}
