package de.frank.perfectmoment.session

data class Question(
    val id: Long = 0,
    val emoji: String = EmojiParser.FALLBACK_EMOJI,
    val text: String,
)

enum class Phase {
    IDLE_MUTED,
    SPEAKING,
    PAUSE_REP,
    PAUSE_NEXT,
    WAITING_NETWORK,
    ENDED,
}

data class SessionState(
    val questions: List<Question>,
    val currentIndex: Int = 0,
    val currentRep: Int = 1,
    val phase: Phase = Phase.IDLE_MUTED,
    val speakerOn: Boolean = false,
    val remainingMs: Long,
    val refillInFlight: Boolean = false,
    val offline: Boolean = false,
)

data class SessionConfig(
    val pauseRepMs: Long,
    val pauseNextMs: Long,
    val repsPerQuestion: Int,
    val durationMs: Long,
) {
    init {
        require(pauseRepMs >= 0) { "pauseRepMs must not be negative" }
        require(pauseNextMs >= 0) { "pauseNextMs must not be negative" }
        require(repsPerQuestion > 0) { "repsPerQuestion must be positive" }
        require(durationMs > 0) { "durationMs must be positive" }
    }

    companion object {
        fun fromSeconds(
            pauseRepSeconds: Int,
            pauseNextSeconds: Int,
            repsPerQuestion: Int,
            durationMinutes: Int,
        ) = SessionConfig(
            pauseRepMs = pauseRepSeconds * 1_000L,
            pauseNextMs = pauseNextSeconds * 1_000L,
            repsPerQuestion = repsPerQuestion,
            durationMs = durationMinutes * 60_000L,
        )
    }
}

interface SessionTtsPort {
    fun speak(text: String, listener: Listener)
    fun stop()

    interface Listener {
        fun onStart() = Unit
        fun onComplete()
        fun onError(error: Throwable? = null)
    }
}

fun interface QuestionRefillPort {
    suspend fun requestQuestions(existingQuestions: List<Question>): List<String>
}

fun interface QuestionPersistencePort {
    suspend fun persistQuestions(questions: List<Question>)
}

fun interface SessionClock {
    fun nowMillis(): Long
}

object MonotonicSessionClock : SessionClock {
    override fun nowMillis(): Long = System.nanoTime() / 1_000_000L
}
