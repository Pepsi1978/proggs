package com.bestjournal.app.data.repository

import android.content.Context
import android.net.Uri
import com.bestjournal.app.data.local.dao.EntryPhotoDao
import com.bestjournal.app.data.local.entity.EntryPhotoEntity
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

    suspend fun addPhoto(entryId: Long, sourceUri: Uri): Long =
        withContext(Dispatchers.IO) {
            val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
            val fileName = "${UUID.randomUUID()}.jpg"
            val destFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            entryPhotoDao.insert(
                EntryPhotoEntity(
                    entryId = entryId,
                    filePath = destFile.absolutePath,
                    timestamp = System.currentTimeMillis(),
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
