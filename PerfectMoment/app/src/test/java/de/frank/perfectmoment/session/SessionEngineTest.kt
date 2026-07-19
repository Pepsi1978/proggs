package de.frank.perfectmoment.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEngineTest {
    @Test
    fun `startet stumm auf der ersten Frage`() = runTest {
        val fixture = fixture(listOf(question(1)))

        fixture.engine.start()
        runCurrent()

        assertEquals(Phase.IDLE_MUTED, fixture.engine.state.value.phase)
        assertFalse(fixture.engine.state.value.speakerOn)
        assertEquals(0, fixture.engine.state.value.currentIndex)
        assertTrue(fixture.tts.calls.isEmpty())
        fixture.engine.close()
    }

    @Test
    fun `Takt wartet ab onComplete zwischen Wiederholungen und Fragen`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1), question(2)),
            config = config(pauseRepMs = 1_000, pauseNextMs = 2_000, reps = 3),
        )
        fixture.startSpeaking()
        assertEquals(listOf("Frage 1"), fixture.tts.spokenTexts)

        fixture.tts.completeCurrent()
        runCurrent()
        assertEquals(Phase.PAUSE_REP, fixture.engine.state.value.phase)
        advanceTimeBy(999)
        assertEquals(1, fixture.tts.calls.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, fixture.tts.calls.size)

        fixture.tts.completeCurrent()
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(3, fixture.tts.calls.size)

        fixture.tts.completeCurrent()
        runCurrent()
        assertEquals(Phase.PAUSE_NEXT, fixture.engine.state.value.phase)
        advanceTimeBy(1_999)
        assertEquals(3, fixture.tts.calls.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Frage 1", "Frage 1", "Frage 1", "Frage 2"), fixture.tts.spokenTexts)
        fixture.engine.close()
    }

    @Test
    fun `fordert Nachschub beim Sprechbeginn an Index 19 und 49 an`() = runTest {
        val refill = FakeRefill().apply {
            responses += List(30) { "✨ Nachschub ${it + 1}" }
            responses += List(30) { "✨ Weiterer Nachschub ${it + 1}" }
        }
        val fixture = fixture(
            questions = List(30) { question(it + 1) },
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1),
            refill = refill,
        )
        fixture.startSpeaking()

        repeat(19) {
            fixture.tts.completeCurrent()
            runCurrent()
        }
        assertEquals(19, fixture.engine.state.value.currentIndex)
        assertEquals(1, refill.requests)
        assertEquals(60, fixture.engine.state.value.questions.size)

        repeat(30) {
            fixture.tts.completeCurrent()
            runCurrent()
        }
        assertEquals(49, fixture.engine.state.value.currentIndex)
        assertEquals(2, refill.requests)
        assertEquals(90, fixture.engine.state.value.questions.size)
        fixture.engine.close()
    }

    @Test
    fun `leerer Nachschub wird alle 15 Sekunden versucht und nach zwei Minuten offline`() = runTest {
        val refill = FakeRefill()
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 300_000),
            refill = refill,
        )
        fixture.startSpeaking()
        fixture.tts.completeCurrent()
        runCurrent()

        assertEquals(Phase.WAITING_NETWORK, fixture.engine.state.value.phase)
        assertEquals(1, refill.requests)
        advanceTimeBy(14_999)
        assertEquals(1, refill.requests)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, refill.requests)

        advanceTimeBy(105_000)
        runCurrent()
        assertTrue(fixture.engine.state.value.offline)
        assertEquals(SessionEngine.OFFLINE_MESSAGE, fixture.tts.spokenTexts.last())

        refill.responses += listOf("🌲 Eine neue Frage")
        advanceTimeBy(15_000)
        runCurrent()
        assertFalse(fixture.engine.state.value.offline)
        assertEquals("Eine neue Frage", fixture.engine.state.value.questions.last().text)
        fixture.tts.completeCurrent()
        runCurrent()
        assertEquals("Eine neue Frage", fixture.tts.spokenTexts.last())
        fixture.engine.close()
    }

    @Test
    fun `Timerende wartet auf das Ende der laufenden Sprache`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1), question(2)),
            config = config(durationMs = 1_000, reps = 1),
        )
        fixture.startSpeaking()

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(0L, fixture.engine.state.value.remainingMs)
        assertEquals(Phase.SPEAKING, fixture.engine.state.value.phase)

        fixture.tts.completeCurrent()
        runCurrent()
        assertEquals(Phase.ENDED, fixture.engine.state.value.phase)
        fixture.engine.close()
    }

    @Test
    fun `Lautsprecher aus nach Timerende beendet die Sitzung`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(durationMs = 1_000, reps = 1),
        )
        fixture.startSpeaking()

        advanceTimeBy(1_000)
        runCurrent()
        fixture.engine.setSpeakerOn(false)
        runCurrent()

        assertEquals(Phase.ENDED, fixture.engine.state.value.phase)
        assertFalse(fixture.engine.state.value.speakerOn)
        assertEquals(0L, fixture.engine.state.value.remainingMs)
        fixture.engine.close()
    }

    @Test
    fun `Offline Ansage erfolgt pro Offline Periode nur einmal`() = runTest {
        val refill = FakeRefill()
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 600_000),
            refill = refill,
        )
        fixture.startSpeaking()
        fixture.tts.completeCurrent()
        runCurrent()

        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(1, fixture.tts.offlineNoticeCount)

        fixture.tts.completeCurrent()
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(1, fixture.tts.offlineNoticeCount)

        refill.responses += listOf("🌲 Eine neue Frage")
        advanceTimeBy(15_000)
        runCurrent()
        assertFalse(fixture.engine.state.value.offline)
        assertEquals("Eine neue Frage", fixture.tts.spokenTexts.last())

        fixture.tts.completeCurrent()
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(2, fixture.tts.offlineNoticeCount)
        fixture.engine.close()
    }

    @Test
    fun `Lautsprecher aus und an startet die aktuelle Frage neu`() = runTest {
        val fixture = fixture(listOf(question(1), question(2)))
        fixture.startSpeaking()

        fixture.engine.setSpeakerOn(false)
        runCurrent()
        assertEquals(Phase.IDLE_MUTED, fixture.engine.state.value.phase)
        assertEquals(1, fixture.tts.stopCount)

        fixture.engine.setSpeakerOn(true)
        runCurrent()
        assertEquals(listOf("Frage 1", "Frage 1"), fixture.tts.spokenTexts)
        assertEquals(0, fixture.engine.state.value.currentIndex)
        fixture.engine.close()
    }

    @Test
    fun `drei TTS Fehler ueberspringen die Frage`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1), question(2)),
            config = config(pauseRepMs = 1_000, pauseNextMs = 2_000, reps = 1),
        )
        fixture.startSpeaking()

        repeat(2) {
            fixture.tts.errorCurrent()
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()
        }
        fixture.tts.errorCurrent()
        runCurrent()
        assertEquals(Phase.PAUSE_NEXT, fixture.engine.state.value.phase)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals("Frage 2", fixture.tts.spokenTexts.last())
        fixture.engine.close()
    }

    @Test
    fun `Replay endet direkt nach der letzten Frage ohne Nachschub`() = runTest {
        val refill = FakeRefill().apply { responses += listOf("✨ Darf nicht geladen werden") }
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(reps = 1),
            refill = refill,
            replay = true,
        )
        fixture.startSpeaking()

        fixture.tts.completeCurrent()
        runCurrent()

        assertEquals(Phase.ENDED, fixture.engine.state.value.phase)
        assertEquals(0, refill.requests)
        fixture.engine.close()
    }

    private fun TestScope.fixture(
        questions: List<Question>,
        config: SessionConfig = config(),
        refill: FakeRefill? = null,
        replay: Boolean = false,
    ): Fixture {
        val tts = FakeTts()
        val engine = SessionEngine(
            initialQuestions = questions,
            config = config,
            ttsPort = tts,
            refillPort = refill,
            coroutineScope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            clock = SessionClock { testScheduler.currentTime },
            replay = replay,
        )
        return Fixture(engine, tts, this)
    }

    private data class Fixture(
        val engine: SessionEngine,
        val tts: FakeTts,
        val testScope: TestScope,
    ) {
        fun startSpeaking() {
            engine.start()
            testScope.runCurrent()
            engine.setSpeakerOn(true)
            testScope.runCurrent()
        }
    }

    private class FakeTts : SessionTtsPort {
        data class Call(val text: String, val listener: SessionTtsPort.Listener)

        val calls = mutableListOf<Call>()
        val spokenTexts: List<String> get() = calls.map(Call::text)
        val offlineNoticeCount: Int
            get() = spokenTexts.count { it == SessionEngine.OFFLINE_MESSAGE }
        var stopCount = 0

        override fun speak(text: String, listener: SessionTtsPort.Listener) {
            calls += Call(text, listener)
            listener.onStart()
        }

        override fun stop() {
            stopCount++
        }

        fun completeCurrent() = calls.last().listener.onComplete()

        fun errorCurrent() = calls.last().listener.onError()
    }

    private class FakeRefill : QuestionRefillPort {
        val responses = ArrayDeque<List<String>>()
        var requests = 0

        override suspend fun requestQuestions(existingQuestions: List<Question>): List<String> {
            requests++
            return if (responses.isEmpty()) emptyList() else responses.removeFirst()
        }
    }

    companion object {
        private fun question(number: Int) = Question(emoji = "✨", text = "Frage $number")

        private fun config(
            pauseRepMs: Long = 100,
            pauseNextMs: Long = 100,
            reps: Int = 3,
            durationMs: Long = 60_000,
        ) = SessionConfig(
            pauseRepMs = pauseRepMs,
            pauseNextMs = pauseNextMs,
            repsPerQuestion = reps,
            durationMs = durationMs,
        )
    }
}
