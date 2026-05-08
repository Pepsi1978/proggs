package de.frank.entropyreducer.data.remote.drive

import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holt die juengste Backup-Datei aus dem appDataFolder zurück.
 * Liefert `null`, wenn auf Drive nichts liegt — der Aufrufer entscheidet,
 * ob das ein erwarteter "frischer" Zustand oder ein Fehler ist.
 */
@Singleton
class DriveRestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: DriveSession,
) {

    private val fileName = "entropy_reducer_entries_v1.json"

    /**
     * Gibt das letzte JSON-Backup zurück — oder null, wenn kein Backup vorhanden.
     * Wirft `DriveNotSignedInException` / `DriveConsentRequiredException` wenn
     * der Account-Zugang noch nicht gesetzt ist.
     */
    suspend fun fetchLatest(): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = session.get()

            val match = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setOrderBy("modifiedTime desc")
                .setFields("files(id, modifiedTime)")
                .setPageSize(1)
                .execute()
                .files
                ?.firstOrNull()
                ?: return@runCatching null

            val sink = ByteArrayOutputStream()
            drive.files().get(match.id).executeMediaAndDownloadTo(sink)
            // ByteArrayOutputStream.toString(Charset) braucht API 33+ — auf API 28-32
            // würde das mit NoSuchMethodError crashen. String(ByteArray, Charset) gibt's
            // seit API 1, gleicher Output, kompatibel auf allen unterstuetzten Versionen.
            String(sink.toByteArray(), Charsets.UTF_8)
        }
    }

    /** Existiert ein Backup auf Drive? */
    suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val drive = session.get()
            drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("files(id)")
                .setPageSize(1)
                .execute()
                .files
                ?.isNotEmpty() == true
        }.getOrDefault(false)
    }
}
