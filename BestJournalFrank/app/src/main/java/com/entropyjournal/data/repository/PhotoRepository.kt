package com.entropyjournal.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.entropyjournal.data.local.dao.EntryPhotoDao
import com.entropyjournal.data.local.entity.EntryPhotoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class PhotoRepository
@Inject
constructor(
    private val entryPhotoDao: EntryPhotoDao,
    @ApplicationContext private val context: Context,
) {

    fun getPhotosForEntry(entryId: Long): Flow<List<EntryPhotoEntity>> {
        return entryPhotoDao.getPhotosForEntry(entryId)
    }

    fun createCameraUri(): Pair<Uri, File> {
        val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
        val file = File(photosDir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return uri to file
    }

    private fun isVideoUri(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true
    }

    suspend fun addMedia(entryId: Long, sourceUri: Uri): Long =
        withContext(Dispatchers.IO) {
            val isVideo = isVideoUri(sourceUri)
            val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
            val ext = if (isVideo) "mp4" else "jpg"
            val fileName = "${UUID.randomUUID()}.$ext"
            val destFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            entryPhotoDao.insert(
                EntryPhotoEntity(
                    entryId = entryId,
                    filePath = destFile.absolutePath,
                    timestamp = System.currentTimeMillis(),
                    isVideo = isVideo,
                )
            )
        }

    suspend fun addPhotoFromFile(entryId: Long, file: File): Long =
        withContext(Dispatchers.IO) {
            val isVideo = file.extension.lowercase() in listOf("mp4", "3gp", "mkv", "webm")
            entryPhotoDao.insert(
                EntryPhotoEntity(
                    entryId = entryId,
                    filePath = file.absolutePath,
                    timestamp = System.currentTimeMillis(),
                    isVideo = isVideo,
                )
            )
        }

    suspend fun deletePhoto(photoId: Long) =
        withContext(Dispatchers.IO) {
            val filePath = entryPhotoDao.getFilePath(photoId)
            filePath?.let { File(it).delete() }
            entryPhotoDao.deleteById(photoId)
        }
}
