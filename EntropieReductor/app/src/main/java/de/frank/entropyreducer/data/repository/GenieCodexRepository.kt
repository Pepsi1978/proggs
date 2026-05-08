package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.GenieCodexDao
import de.frank.entropyreducer.data.local.entities.GenieCodexVersionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Versionierte Synthese „Was die KI über Frank verstanden hat". Spec §6.6, §16.5.
 */
@Singleton
class GenieCodexRepository @Inject constructor(
    private val dao: GenieCodexDao,
) {
    fun observeLatest(): Flow<GenieCodexVersionEntity?> = dao.getLatest()
    fun observeRecent(): Flow<List<GenieCodexVersionEntity>> = dao.getRecentVersions()

    suspend fun insert(version: GenieCodexVersionEntity) {
        dao.insert(version)
        dao.pruneOldVersions()
    }
}
