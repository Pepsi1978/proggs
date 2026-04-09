package com.entropyjournal.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

    private fun saveToGallery(file: File, isVideo: Boolean) {
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val collection =
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) Environment.DIRECTORY_DCIM + "/Camera"
                        else Environment.DIRECTORY_DCIM + "/Camera",
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

        val uri = context.contentResolver.insert(collection, values) ?: return
        context.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
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
            // Save a copy to the phone gallery (DCIM/Camera)
            try {
                saveToGallery(file, isVideo)
            } catch (_: Exception) {
                // Gallery save is best-effort, don't fail the whole operation
            }
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
