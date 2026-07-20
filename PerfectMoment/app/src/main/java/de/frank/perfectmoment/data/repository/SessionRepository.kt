package de.frank.perfectmoment.data.repository

import androidx.room.withTransaction
import de.frank.perfectmoment.data.local.PerfectMomentDatabase
import de.frank.perfectmoment.data.local.QuestionEntity
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.local.SessionWithQuestions
import de.frank.perfectmoment.session.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SessionRepository {
    fun observeSessions(): Flow<List<SessionEntity>>
    fun observeSession(sessionId: Long): Flow<SessionWithQuestions?>
    suspend fun getSession(sessionId: Long): SessionWithQuestions?
    suspend fun createSession(session: SessionEntity): Long
    suspend fun appendQuestions(sessionId: Long, questions: List<Question>)
    suspend fun deleteSession(sessionId: Long): Boolean
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

    override suspend fun deleteSession(sessionId: Long): Boolean =
        sessionDao.deleteSession(sessionId) > 0
}
