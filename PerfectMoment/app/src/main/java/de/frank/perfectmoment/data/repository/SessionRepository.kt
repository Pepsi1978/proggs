package de.frank.perfectmoment.data.repository

import androidx.room.withTransaction
import de.frank.perfectmoment.data.local.PerfectMomentDatabase
import de.frank.perfectmoment.data.local.QuestionEntity
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.local.SessionWithQuestions
import de.frank.perfectmoment.session.Question
import de.frank.perfectmoment.session.SessionConfig
import de.frank.perfectmoment.session.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SessionRepository {
    fun observeSessions(): Flow<List<SessionEntity>>
    fun observeSession(sessionId: Long): Flow<SessionWithQuestions?>
    suspend fun getSession(sessionId: Long): SessionWithQuestions?
    suspend fun createSession(session: SessionEntity): Long
    suspend fun appendQuestions(sessionId: Long, questions: List<Question>)
    suspend fun saveProgress(sessionId: Long, state: SessionState, config: SessionConfig)
    suspend fun clearProgress(sessionId: Long)
    suspend fun markPlayed(sessionId: Long)
    /** Fills in the AI title; keeps its hands off entries that already carry one. */
    suspend fun fillSummaryIfEmpty(sessionId: Long, summary: String)

    /** Stores the title the user typed. Blank hands the entry back to the AI. */
    suspend fun setManualSummary(sessionId: Long, summary: String)
    suspend fun setVoiceOverride(sessionId: Long, providerId: String, voiceId: String)
    suspend fun deleteSession(sessionId: Long): Boolean

    /** Hängt eine von Hand geschriebene Frage an und liefert deren Kennung zurück. */
    suspend fun appendQuestion(sessionId: Long, emoji: String, text: String): Long
    suspend fun setQuestionText(questionId: Long, text: String)
    suspend fun setQuestionEmoji(questionId: Long, emoji: String)
    suspend fun removeQuestion(sessionId: Long, questionId: Long)
    suspend fun setSessionSettings(
        sessionId: Long,
        pauseRep: Int,
        pauseNext: Int,
        reps: Int,
        durationMin: Int,
    )
}

class RoomSessionRepository(
    private val database: PerfectMomentDatabase,
) : SessionRepository {
    private val sessionDao = database.sessionDao()

    override fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observeSessions()

    override fun observeSession(sessionId: Long): Flow<SessionWithQuestions?> =
        sessionDao.observeSession(sessionId).map { it?.sorted() }

    override suspend fun getSession(sessionId: Long): SessionWithQuestions? =
        sessionDao.getSession(sessionId)?.sorted()

    override suspend fun createSession(session: SessionEntity): Long =
        sessionDao.insertSession(session.copy(id = 0, questionCount = 0))

    override suspend fun appendQuestions(sessionId: Long, questions: List<Question>) {
        if (questions.isEmpty()) return
        database.withTransaction {
            val firstIndex = sessionDao.nextQuestionIndex(sessionId)
            sessionDao.insertQuestions(
                questions.mapIndexed { index, question ->
                    QuestionEntity(
                        sessionId = sessionId,
                        orderIndex = firstIndex + index,
                        emoji = question.emoji,
                        text = question.text,
                    )
                },
            )
            sessionDao.updateQuestionCount(sessionId, sessionDao.questionCount(sessionId))
        }
    }

    override suspend fun saveProgress(sessionId: Long, state: SessionState, config: SessionConfig) {
        require(state.questions.isNotEmpty()) { "A session without questions cannot be resumed" }
        database.withTransaction {
            sessionDao.deleteQuestions(sessionId)
            sessionDao.insertQuestions(
                state.questions.mapIndexed { index, question ->
                    QuestionEntity(
                        sessionId = sessionId,
                        orderIndex = index,
                        emoji = question.emoji,
                        text = question.text,
                    )
                },
            )
            sessionDao.saveProgress(
                sessionId = sessionId,
                durationMin = (config.durationMs / 60_000L).toInt(),
                pauseRep = (config.pauseRepMs / 1_000L).toInt(),
                pauseNext = (config.pauseNextMs / 1_000L).toInt(),
                reps = config.repsPerQuestion,
                questionCount = state.questions.size,
                // Bis hinter die letzte Frage erlaubt: Wer den Vorrat durchhat, wartet beim
                // Fortsetzen auf Nachschub, statt die letzte Frage ein zweites Mal zu hoeren.
                questionIndex = state.currentIndex.coerceIn(0, state.questions.size),
                repetition = state.currentRep.coerceIn(1, config.repsPerQuestion),
                remainingMs = state.remainingMs.coerceAtLeast(1L),
            )
        }
    }

    override suspend fun clearProgress(sessionId: Long) = sessionDao.clearProgress(sessionId)

    override suspend fun markPlayed(sessionId: Long) =
        sessionDao.markPlayed(sessionId, System.currentTimeMillis())

    override suspend fun fillSummaryIfEmpty(sessionId: Long, summary: String) {
        sessionDao.fillSummaryIfEmpty(sessionId, summary)
    }

    override suspend fun setManualSummary(sessionId: Long, summary: String) =
        sessionDao.updateManualSummary(sessionId, summary, manual = summary.isNotBlank())

    override suspend fun setVoiceOverride(sessionId: Long, providerId: String, voiceId: String) =
        sessionDao.updateVoiceOverride(sessionId, providerId, voiceId)

    override suspend fun deleteSession(sessionId: Long): Boolean =
        sessionDao.deleteSession(sessionId) > 0

    override suspend fun appendQuestion(sessionId: Long, emoji: String, text: String): Long =
        sessionDao.appendQuestion(sessionId, emoji, text)

    override suspend fun setQuestionText(questionId: Long, text: String) =
        sessionDao.updateQuestionText(questionId, text)

    override suspend fun setQuestionEmoji(questionId: Long, emoji: String) =
        sessionDao.updateQuestionEmoji(questionId, emoji)

    override suspend fun removeQuestion(sessionId: Long, questionId: Long) =
        sessionDao.removeQuestion(sessionId, questionId)

    override suspend fun setSessionSettings(
        sessionId: Long,
        pauseRep: Int,
        pauseNext: Int,
        reps: Int,
        durationMin: Int,
    ) = sessionDao.updateSessionSettings(sessionId, pauseRep, pauseNext, reps, durationMin)
}
