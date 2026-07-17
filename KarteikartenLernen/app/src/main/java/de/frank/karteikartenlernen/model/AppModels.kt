package de.frank.karteikartenlernen.model

enum class AppTab { RESEARCH, PROFILES, LEARN, SETTINGS }
enum class MicState { IDLE, RECORDING, TRANSCRIBING }
enum class GenerationPhase { ANSWER, CARDS, DONE }
enum class CardStatus { NEW, KNOWN, UNKNOWN }

data class Flashcard(
    val id: Long,
    val question: String,
    val answer: String,
    val explanation: String,
    val status: CardStatus = CardStatus.NEW,
)

data class StudySession(
    val id: String,
    val title: String,
    val count: Int,
    val known: Int,
    val date: String,
    val accent: Int,
)

data class CrossSuggestion(
    val sessionId: String,
    val title: String,
    val cardIds: List<Long>,
    val accepted: Boolean? = null,
) {
    val count: Int get() = cardIds.size
}

data class LearningState(
    val sessionId: String,
    val title: String,
    val deck: List<Flashcard>,
    val queue: List<Int>,
    val position: Int = 0,
    val flipped: Boolean = false,
    val known: Int = 0,
    val repeated: Int = 0,
    val done: Boolean = false,
    val entering: Boolean = true,
    val rating: Boolean? = null,
)

data class QueueAdvance(val queue: List<Int>, val nextPosition: Int, val done: Boolean)

fun advanceLearningQueue(queue: List<Int>, position: Int, known: Boolean): QueueAdvance {
    require(position in queue.indices) { "Position muss innerhalb der Lernqueue liegen." }
    val updated = if (known) queue else queue + queue[position]
    val next = position + 1
    return QueueAdvance(updated, next, next >= updated.size)
}

data class AppSettings(
    val dark: Boolean = true,
    val darkProfile: Int = 0,
    val lightProfile: Int = 0,
    val sounds: Boolean = true,
    val volume: Float = 0.6f,
    val soundFlip: Boolean = true,
    val soundKnown: Boolean = true,
    val soundUnknown: Boolean = true,
    val soundTransition: Boolean = true,
    val soundDone: Boolean = true,
    val voice: String = "de-DE-SeraphinaMultilingualNeural",
    val speechRate: Float = 1f,
    val cardFont: Int = 20,
    val model: String = "GPT 5.6 Terra",
    val reasoning: String = "Mittel",
    val cardsPerResearch: Int = 0,
)

data class AppUiState(
    val tab: AppTab = AppTab.RESEARCH,
    val mic: MicState = MicState.IDLE,
    val recordingSeconds: Int = 0,
    val input: String = "",
    val versions: List<String> = listOf(""),
    val versionIndex: Int = 0,
    val improving: Boolean = false,
    val answer: String? = null,
    val generationPhase: GenerationPhase? = null,
    val showCrossSheet: Boolean = false,
    val showModelSheet: Boolean = false,
    val showOAuth: Boolean = false,
    val model: String = "GPT 5.6 Terra",
    val reasoning: String = "Mittel",
    val connectedEmail: String? = null,
    val authBusy: Boolean = false,
    val authError: String? = null,
    val authUserCode: String? = null,
    val authVerificationUri: String? = null,
    val message: String? = null,
    val selectedSession: StudySession? = null,
    val detailResearchTab: Boolean = false,
    val learning: LearningState? = null,
    val cards: List<Flashcard> = sampleCards,
    val sessions: List<StudySession> = sampleSessions,
    val search: String = "",
    val settings: AppSettings = AppSettings(),
    val savedSessionTitle: String = "HRV & Schlafphasen",
    val savedCardCount: Int = 27,
    val detailQuestion: String = "Warum ist die HRV nachts höher und welche Rolle spielt der Vagusnerv?",
    val detailAnswer: String = SAMPLE_ANSWER,
    val crossSuggestions: List<CrossSuggestion> = emptyList(),
)

val sampleCards = listOf(
    Flashcard(1, "Was beschreibt die Herzratenvariabilität (HRV)?", "Die zeitlichen Schwankungen zwischen aufeinanderfolgenden Herzschlägen.", "Eine hohe HRV gilt als Zeichen für ein gut regulierendes autonomes Nervensystem und gute Erholung.", CardStatus.KNOWN),
    Flashcard(2, "Welcher Teil des Nervensystems erhöht die HRV?", "Der Parasympathikus über den Vagusnerv.", "Er dominiert in Ruhe und im Schlaf und bremst die Herzfrequenz."),
    Flashcard(3, "In welcher Schlafphase ist die HRV am höchsten?", "Im Tiefschlaf (N3).", "Dort überwiegt der Parasympathikus, Herzfrequenz und Blutdruck sinken."),
    Flashcard(4, "Was misst der RMSSD-Wert?", "Die kurzfristige HRV über aufeinanderfolgende RR-Intervalle.", "Er gilt als robuster Marker für die parasympathische Aktivität und wird oft nachts erhoben.", CardStatus.UNKNOWN),
    Flashcard(5, "Wie wirkt Alkohol am Abend auf die nächtliche HRV?", "Er senkt sie deutlich, oft über mehrere Stunden.", "Alkohol erhöht die sympathische Aktivität im Schlaf und verschlechtert den Tiefschlafanteil."),
    Flashcard(6, "Was kennzeichnet die REM-Phase kardiologisch?", "Erhöhte sympathische Aktivität und instabile Herzfrequenz.", "Die HRV schwankt stärker; Träume und schnelle Augenbewegungen treten auf.", CardStatus.KNOWN),
)

val sampleSessions = listOf(
    StudySession("hrv", "HRV & Schlafphasen", 27, 11, "Heute", 0),
    StudySession("drone", "Drohnen-Recht in der EU", 19, 19, "Gestern", 1),
    StudySession("vagus", "Cold Exposure & Vagusnerv", 15, 6, "vor 3 Tagen", 2),
    StudySession("zins", "Zinseszins & ETF-Sparpläne", 23, 0, "vor 5 Tagen", 0),
    StudySession("stoa", "Stoische Philosophie – Grundlagen", 31, 24, "letzte Woche", 1),
)

const val SAMPLE_RAW = "warum ist die herzratenvariabilität nachts höher und was hat der vagusnerv damit zu tun und wie wirkt sich alkohol da drauf aus"
const val SAMPLE_IMPROVED = "Warum ist die Herzratenvariabilität (HRV) nachts höher als am Tag, welche Rolle spielt dabei der Vagusnerv, und wie wirkt sich Alkohol am Abend darauf aus?"
const val SAMPLE_ANSWER = "Die Herzratenvariabilität (HRV) beschreibt die feinen zeitlichen Schwankungen zwischen aufeinanderfolgenden Herzschlägen. Nachts steigt sie typischerweise an, weil im Schlaf – besonders im Tiefschlaf – der Parasympathikus dominiert. Dieser Teil des autonomen Nervensystems wirkt über den Vagusnerv bremsend auf das Herz: Die Herzfrequenz sinkt, die Abstände zwischen den Schlägen werden variabler, und der Körper regeneriert.\nDer Vagusnerv ist also der zentrale Vermittler dieses Effekts. Je aktiver er in Ruhe ist, desto höher fällt die nächtliche HRV aus – ein Zeichen guter Erholungsfähigkeit. Alkohol am Abend stört genau diesen Mechanismus: Er erhöht die sympathische Aktivität im Schlaf, verschiebt die Schlafphasen und senkt die nächtliche HRV oft über Stunden deutlich messbar."
