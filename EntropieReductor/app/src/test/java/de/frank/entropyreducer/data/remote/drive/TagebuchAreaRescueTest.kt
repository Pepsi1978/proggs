package de.frank.entropyreducer.data.remote.drive

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TagebuchAreaRescueTest {

    @Test
    fun `learning entries do not block rescue of missing entropy entries`() {
        val localLearning = backupEntry(id = "learning-local", area = "LEARNING", timestamp = 20L)
        val remoteEntropy = backupEntry(id = "entropy-remote", area = "ENTROPY", timestamp = 10L)

        assertThat(
            rescueTagebuchAreasIfLocallyEmpty(
                listOf(localLearning),
                listOf(remoteEntropy),
            ).map { it.id }
        )
            .containsExactly("learning-local", "entropy-remote")
    }

    @Test
    fun `existing local area is not repopulated from remote`() {
        val localEntropy = backupEntry(id = "entropy-local", area = "ENTROPY", timestamp = 20L)
        val remoteEntropy = backupEntry(id = "entropy-remote", area = "ENTROPY", timestamp = 10L)

        assertThat(
            rescueTagebuchAreasIfLocallyEmpty(
                listOf(localEntropy),
                listOf(remoteEntropy),
            ).map { it.id }
        )
            .containsExactly("entropy-local")
    }

    private fun backupEntry(id: String, area: String, timestamp: Long) =
        BackupTagebuchEntry(
            id = id,
            timestampMs = timestamp,
            title = id,
            text = id,
            area = area,
        )
}
