package de.frank.perfectmoment.backup

import android.content.Context
import android.os.Build
import de.frank.perfectmoment.data.local.PerfectMomentDatabase
import de.frank.perfectmoment.data.local.QuestionEntity
import de.frank.perfectmoment.data.settings.SecureSettings
import de.frank.perfectmoment.tts.TtsProvider
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient

/**
 * Carries hooks, skills and sessions between the device and the user's Google Drive.
 *
 * Restoring merges rather than replaces: everything from the backup is written, anything only
 * present on this device stays. That way pulling a backup can never cost data — the situation this
 * whole feature exists to prevent.
 */
class BackupRepository(
    private val context: Context,
    private val database: PerfectMomentDatabase,
    private val settings: SecureSettings,
    http: OkHttpClient,
) {

    private val auth = DriveAuth(context)
    private val drive = DriveClient(http)
    val files = FileBackup(context)

    data class Summary(val hooks: Int, val skills: Int, val sessions: Int)

    /** Schreibt die Sicherung an den gewählten Ort — in der Regel im Google Drive des Nutzers. */
    suspend fun backupToFile(uri: android.net.Uri): Summary {
        val payload = collect()
        files.write(uri, payload.toJson())
        return Summary(payload.hooks.size, payload.skills.size, payload.sessions.size)
    }

    /** Liest eine Sicherungsdatei und mischt sie ein. */
    suspend fun restoreFromFile(uri: android.net.Uri): Summary {
        val payload = BackupPayload.fromJson(files.read(uri)) ?: throw UnreadableBackup()
        return apply(payload)
    }

    suspend fun isConnected(): Boolean = auth.isConnected()

    suspend fun disconnect() = auth.disconnect()

    /** Reicht Googles Antwort auf den Freigabe-Bildschirm weiter. */
    fun readConsentResult(data: android.content.Intent?) = auth.readConsentResult(data)

    /** Collects the current state and writes it to Drive. */
    suspend fun backupNow(): Summary {
        val token = auth.accessToken()
        val payload = collect()
        val existing = drive.findBackup(token)
        drive.upload(token, existing?.id, payload.toJson())
        BackupStatus.markBackedUp(context)
        return Summary(payload.hooks.size, payload.skills.size, payload.sessions.size)
    }

    /** Pulls the backup and merges it into the local database. */
    suspend fun restoreNow(): Summary {
        val token = auth.accessToken()
        val remote = drive.findBackup(token) ?: throw NoBackupFound()
        val raw = drive.download(token, remote.id)
        val payload = BackupPayload.fromJson(raw) ?: throw UnreadableBackup()
        return apply(payload)
    }

    class NoBackupFound : Exception("In deinem Google Drive liegt noch keine Sicherung.")

    class UnreadableBackup : Exception("Die Sicherung stammt aus einer neueren App-Version.")

    private suspend fun collect(): BackupPayload {
        val hooks = database.hookDao().observeHooks().first()
        val skills = database.skillDao().observeSkills().first()
        val sessionHeads = database.sessionDao().observeSessions().first()
        val sessions = sessionHeads.mapNotNull { head -> database.sessionDao().getSession(head.id) }
        return BackupPayload(
            dataVersion = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            hooks = hooks,
            skills = skills,
            sessions = sessions,
            voiceProfiles = VoiceProfiles(
                names = settings.qwenVoiceNames,
                selectedVoiceId = settings.qwenTtsVoiceId,
                providerId = settings.ttsProvider,
            ),
        )
    }

    private suspend fun apply(payload: BackupPayload): Summary {
        restoreVoiceProfiles(payload.voiceProfiles)
        payload.hooks.forEach { database.hookDao().upsertHook(it) }
        payload.skills.forEach { database.skillDao().upsertSkill(it) }

        var restoredSessions = 0
        payload.sessions.forEach { entry ->
            // Sessions carry their questions, so only add ones this device does not have yet.
            if (database.sessionDao().getSession(entry.session.id) == null) {
                val newId = database.sessionDao().insertSession(entry.session)
                if (entry.questions.isNotEmpty()) {
                    database.sessionDao().insertQuestions(
                        entry.questions.map { question ->
                            QuestionEntity(
                                sessionId = newId,
                                orderIndex = question.orderIndex,
                                emoji = question.emoji,
                                text = question.text,
                            )
                        },
                    )
                }
                restoredSessions++
            }
        }
        return Summary(payload.hooks.size, payload.skills.size, restoredSessions)
    }

    /**
     * Brings the own voices over. Names merge, so a title given on this phone is never lost, and
     * an existing choice stays — only a device without one adopts the backup's.
     */
    private fun restoreVoiceProfiles(profiles: VoiceProfiles) {
        if (profiles.names.isNotEmpty()) {
            settings.qwenVoiceNames = profiles.names + settings.qwenVoiceNames
        }
        if (settings.qwenTtsVoiceId.isBlank() && profiles.selectedVoiceId.isNotBlank()) {
            settings.qwenTtsVoiceId = profiles.selectedVoiceId
            // Only follow the backup into the cloned provider once the key is there as well,
            // otherwise the app would start with a voice it cannot speak with.
            if (profiles.providerId == TtsProvider.QWEN_CLONE.id && settings.qwenTtsApiKey.isNotBlank()) {
                settings.ttsProvider = profiles.providerId
            }
        }
    }
}
