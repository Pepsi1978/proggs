package de.frank.perfectmoment.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
    fun `Fragen werden auch bei Wiederholungen mit Variation gesprochen`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 100, reps = 2),
        )
        fixture.startSpeaking()

        fixture.tts.completeCurrent()
        advanceTimeBy(100)
        runCurrent()

        assertEquals(listOf("Frage 1", "Frage 1"), fixture.tts.spokenTexts)
        assertTrue(fixture.tts.calls.all { it.varied })
        fixture.engine.close()
    }

    @Test
    fun `erste Stream Frage ist sofort sichtbar und vorlesbar`() = runTest {
        val refill = FakeRefill()
        val fixture = fixture(
            questions = emptyList(),
            config = config(reps = 1),
            refill = refill,
            initialGenerationInFlight = true,
        )
        fixture.engine.start()
        runCurrent()
        fixture.engine.setSpeakerOn(true)
        runCurrent()

        fixture.engine.appendQuestions(listOf(question(1)))
        runCurrent()

        assertEquals(listOf("Frage 1"), fixture.tts.spokenTexts)
        assertEquals(0, refill.requests)
        fixture.engine.close()
    }

    @Test
    fun `Stream Ende wartet auf naechste Frage statt zweiten Request zu starten`() = runTest {
        val refill = FakeRefill()
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseNextMs = 0, reps = 1),
            refill = refill,
            initialGenerationInFlight = true,
        )
        fixture.startSpeaking()
        fixture.tts.completeCurrent()
        runCurrent()

        assertEquals(Phase.WAITING_NETWORK, fixture.engine.state.value.phase)
        assertEquals(0, refill.requests)

        fixture.engine.appendQuestions(listOf(question(2)))
        runCurrent()

        assertEquals(listOf("Frage 1", "Frage 2"), fixture.tts.spokenTexts)
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
    fun `fordert Nachschub beim Sprechbeginn an Index 0 und 30 an`() = runTest {
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

        assertEquals(0, fixture.engine.state.value.currentIndex)
        assertEquals(1, refill.requests)
        assertEquals(60, fixture.engine.state.value.questions.size)

        repeat(30) {
            fixture.tts.completeCurrent()
            runCurrent()
        }
        assertEquals(30, fixture.engine.state.value.currentIndex)
        assertEquals(2, refill.requests)
        assertEquals(90, fixture.engine.state.value.questions.size)
        fixture.engine.close()
    }

    @Test
    fun `Nachschub waehrend der Erstgenerierung wird nach deren Ende nachgeholt`() = runTest {
        val refill = FakeRefill().apply { responses += List(30) { "✨ Nachschub ${it + 1}" } }
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1),
            refill = refill,
            initialGenerationInFlight = true,
        )
        fixture.startSpeaking()

        assertEquals(0, refill.requests)

        fixture.engine.completeInitialGeneration()
        runCurrent()

        assertEquals(1, refill.requests)
        assertEquals(31, fixture.engine.state.value.questions.size)
        fixture.engine.close()
    }

    @Test
    fun `abgebrochener Nachschub blockiert die Sitzung nicht dauerhaft`() = runTest {
        val refill = FakeRefill().apply { cancelNextRequest = true }
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 300_000),
            refill = refill,
        )
        fixture.startSpeaking()

        assertEquals(1, refill.requests)
        assertFalse(fixture.engine.state.value.refillInFlight)

        refill.responses += listOf("🌲 Eine neue Frage")
        advanceTimeBy(15_000)
        runCurrent()

        assertEquals(2, refill.requests)
        assertEquals("Eine neue Frage", fixture.engine.state.value.questions.last().text)
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
        // Ein Versuch beim Sprechbeginn, ein zweiter am Listenende.
        assertEquals(2, refill.requests)
        advanceTimeBy(14_999)
        assertEquals(2, refill.requests)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(3, refill.requests)

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
    fun `Endlosmodus zaehlt 30 Minuten wiederholt herunter und uebersteht Pause`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1), question(2)),
            config = config(pauseNextMs = 0, reps = 1, durationMs = 0),
        )
        fixture.startSpeaking()
        assertEquals(SessionConfig.ENDLESS_CYCLE_MS, fixture.engine.state.value.remainingMs)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(SessionConfig.ENDLESS_CYCLE_MS - 1_000, fixture.engine.state.value.remainingMs)
        fixture.engine.togglePause()
        runCurrent()
        val pausedRemaining = fixture.engine.state.value.remainingMs
        advanceTimeBy(1_000)
        assertEquals(pausedRemaining, fixture.engine.state.value.remainingMs)
        fixture.engine.togglePause()
        runCurrent()
        fixture.tts.completeCurrent()
        runCurrent()

        assertEquals(Phase.SPEAKING, fixture.engine.state.value.phase)
        assertEquals("Frage 2", fixture.tts.spokenTexts.last())
        fixture.engine.close()
    }

    @Test
    fun `Endlosmodus startet nach Ablauf wieder bei 30 Minuten`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(durationMs = 0),
            checkpoint = SessionCheckpoint(currentIndex = 0, currentRep = 1, remainingMs = 100),
        )
        fixture.engine.start()
        runCurrent()
        fixture.engine.togglePause()
        runCurrent()

        advanceTimeBy(100)
        runCurrent()

        assertEquals(SessionConfig.ENDLESS_CYCLE_MS, fixture.engine.state.value.remainingMs)
        assertEquals(Phase.SPEAKING, fixture.engine.state.value.phase)
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
    fun `Pause friert Audio und Sitzungstimer bis Play ein`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(durationMs = 10_000, reps = 1),
        )
        fixture.startSpeaking()
        advanceTimeBy(500)
        runCurrent()

        fixture.engine.togglePause()
        runCurrent()
        val pausedRemaining = fixture.engine.state.value.remainingMs
        advanceTimeBy(5_000)
        runCurrent()

        assertTrue(fixture.engine.state.value.paused)
        assertEquals(pausedRemaining, fixture.engine.state.value.remainingMs)
        assertEquals(1, fixture.tts.pauseCount)

        fixture.engine.togglePause()
        runCurrent()
        assertFalse(fixture.engine.state.value.paused)
        assertEquals(1, fixture.tts.resumeCount)
        assertEquals(listOf("Frage 1"), fixture.tts.spokenTexts)
        fixture.engine.close()
    }

    @Test
    fun `Pause setzt Rest der Zwischenpause positionsgetreu fort`() = runTest {
        val fixture = fixture(
            questions = listOf(question(1), question(2)),
            config = config(pauseNextMs = 2_000, reps = 1),
        )
        fixture.startSpeaking()
        fixture.tts.completeCurrent()
        runCurrent()
        advanceTimeBy(500)

        fixture.engine.togglePause()
        runCurrent()
        advanceTimeBy(5_000)
        fixture.engine.togglePause()
        runCurrent()
        advanceTimeBy(1_499)
        runCurrent()
        assertEquals(listOf("Frage 1"), fixture.tts.spokenTexts)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Frage 1", "Frage 2"), fixture.tts.spokenTexts)
        fixture.engine.close()
    }

    @Test
    fun `Pause vor Audiostart beginnt aktuelle Frage bei Play erneut`() = runTest {
        val fixture = fixture(listOf(question(1)))
        fixture.tts.canPause = false
        fixture.startSpeaking()

        fixture.engine.togglePause()
        runCurrent()
        fixture.engine.togglePause()
        runCurrent()

        assertEquals(listOf("Frage 1", "Frage 1"), fixture.tts.spokenTexts)
        fixture.engine.close()
    }

    @Test
    fun `gespeicherter Stand startet pausiert bei gleicher Frage und Wiederholung`() = runTest {
        val tts = FakeTts()
        val engine = SessionEngine(
            initialQuestions = listOf(question(1), question(2)),
            config = config(durationMs = 10_000, reps = 3),
            ttsPort = tts,
            coroutineScope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            clock = SessionClock { testScheduler.currentTime },
            checkpoint = SessionCheckpoint(currentIndex = 1, currentRep = 2, remainingMs = 4_200),
        )

        engine.start()
        runCurrent()

        assertTrue(engine.state.value.paused)
        assertTrue(engine.state.value.speakerOn)
        assertEquals(1, engine.state.value.currentIndex)
        assertEquals(2, engine.state.value.currentRep)
        assertEquals(4_200, engine.state.value.remainingMs)
        assertTrue(tts.spokenTexts.isEmpty())

        engine.togglePause()
        runCurrent()
        assertEquals(listOf("Frage 2"), tts.spokenTexts)
        engine.close()
    }

    @Test
    fun `fortgesetzte Sitzung laedt nach und endet nicht nach der letzten Frage`() = runTest {
        val refill = FakeRefill().apply { responses += List(30) { "✨ Nachschub ${it + 1}" } }
        val fixture = fixture(
            questions = List(30) { question(it + 1) },
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 600_000),
            refill = refill,
            checkpoint = SessionCheckpoint(currentIndex = 29, currentRep = 1, remainingMs = 600_000),
        )

        fixture.engine.start()
        runCurrent()

        assertEquals(1, refill.requests)
        assertEquals(60, fixture.engine.state.value.questions.size)

        fixture.engine.togglePause()
        runCurrent()
        assertEquals("Frage 30", fixture.tts.spokenTexts.last())

        fixture.tts.completeCurrent()
        runCurrent()

        assertEquals(Phase.SPEAKING, fixture.engine.state.value.phase)
        assertEquals("Nachschub 1", fixture.tts.spokenTexts.last())
        fixture.engine.close()
    }

    @Test
    fun `fortgesetzte Sitzung mit vollem Vorrat laedt erst am Blocktrigger nach`() = runTest {
        val refill = FakeRefill().apply { responses += List(30) { "✨ Nachschub ${it + 1}" } }
        val fixture = fixture(
            questions = List(60) { question(it + 1) },
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 600_000),
            refill = refill,
            checkpoint = SessionCheckpoint(currentIndex = 5, currentRep = 1, remainingMs = 600_000),
        )

        fixture.engine.start()
        runCurrent()

        assertEquals(0, refill.requests)

        fixture.engine.togglePause()
        runCurrent()
        repeat(25) {
            fixture.tts.completeCurrent()
            runCurrent()
        }

        assertEquals(30, fixture.engine.state.value.currentIndex)
        assertEquals(1, refill.requests)
        assertEquals(90, fixture.engine.state.value.questions.size)
        fixture.engine.close()
    }

    @Test
    fun `ergebnisloser Nachschub wird nach vier Minuten abgebrochen und neu gestartet`() = runTest {
        val refill = FakeRefill().apply { hangForever = true }
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 3_600_000),
            refill = refill,
        )
        fixture.startSpeaking()

        assertEquals(1, refill.requests)
        assertTrue(fixture.engine.state.value.refillInFlight)

        advanceTimeBy(SessionEngine.REFILL_STALL_MS - 1)
        runCurrent()
        assertTrue(fixture.engine.state.value.refillInFlight)

        advanceTimeBy(1)
        runCurrent()
        assertFalse(fixture.engine.state.value.refillInFlight)
        assertEquals(SessionEngine.REFILL_STALLED_MESSAGE, fixture.engine.state.value.refillError)

        refill.hangForever = false
        refill.responses += listOf("🌲 Endlich eine Frage")
        advanceTimeBy(15_000)
        runCurrent()

        assertEquals(2, refill.requests)
        assertEquals("Endlich eine Frage", fixture.engine.state.value.questions.last().text)
        fixture.engine.close()
    }

    @Test
    fun `laufende Lieferung wird von der Zeitgrenze nicht abgebrochen`() = runTest {
        val refill = FakeRefill().apply {
            responses += listOf("✨ Nachschub 1")
            hangAfterQuestions = true
        }
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 3_600_000),
            refill = refill,
        )
        fixture.startSpeaking()

        assertEquals(2, fixture.engine.state.value.questions.size)

        advanceTimeBy(SessionEngine.REFILL_STALL_MS + 1_000)
        runCurrent()

        // Der Versuch liefert, also greift die Grenze nicht und es wird nichts verworfen.
        assertEquals(1, refill.requests)
        assertEquals("Nachschub 1", fixture.engine.state.value.questions.last().text)
        fixture.engine.close()
    }

    @Test
    fun `wartende Sitzung spricht die erste Nachschubfrage vor dem Blockende`() = runTest {
        val refill = FakeRefill().apply { responses += List(30) { "✨ Nachschub ${it + 1}" } }
        val fixture = fixture(
            questions = List(30) { question(it + 1) },
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 600_000),
            refill = refill,
            checkpoint = SessionCheckpoint(currentIndex = 30, currentRep = 1, remainingMs = 600_000),
        )

        fixture.engine.start()
        runCurrent()
        fixture.engine.togglePause()
        runCurrent()

        // Gesprochen wird die erste gelieferte Frage, nicht erst die dreissigste.
        assertEquals("Nachschub 1", fixture.tts.spokenTexts.last())
        assertEquals(Phase.SPEAKING, fixture.engine.state.value.phase)
        fixture.engine.close()
    }

    @Test
    fun `angekommene Fragen bleiben erhalten wenn der Block danach scheitert`() = runTest {
        val refill = FakeRefill().apply {
            responses += List(30) { "✨ Nachschub ${it + 1}" }
            failAfterFirstQuestion = "OpenAI hat 29 statt genau 30 Fragen geliefert."
        }
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 600_000),
            refill = refill,
        )
        fixture.startSpeaking()

        assertEquals(2, fixture.engine.state.value.questions.size)
        assertEquals("Nachschub 1", fixture.engine.state.value.questions.last().text)
        assertEquals(
            "OpenAI hat 29 statt genau 30 Fragen geliefert.",
            fixture.engine.state.value.refillError,
        )
        fixture.engine.close()
    }

    @Test
    fun `leerer Nachschub nennt einen Grund und loescht ihn beim naechsten Erfolg`() = runTest {
        val refill = FakeRefill()
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 600_000),
            refill = refill,
        )
        fixture.startSpeaking()

        assertEquals(SessionEngine.REFILL_FAILED_MESSAGE, fixture.engine.state.value.refillError)

        refill.responses += listOf("🌲 Eine neue Frage")
        advanceTimeBy(15_000)
        runCurrent()

        assertEquals(null, fixture.engine.state.value.refillError)
        fixture.engine.close()
    }

    @Test
    fun `fortgesetzte Sitzung erholt sich von einem abgebrochenen Nachschub`() = runTest {
        val refill = FakeRefill().apply { cancelNextRequest = true }
        val fixture = fixture(
            questions = List(30) { question(it + 1) },
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 600_000),
            refill = refill,
            checkpoint = SessionCheckpoint(currentIndex = 30, currentRep = 1, remainingMs = 600_000),
        )

        fixture.engine.start()
        runCurrent()

        assertEquals(1, refill.requests)
        assertFalse(fixture.engine.state.value.refillInFlight)

        refill.responses += listOf("✨ Frage 31")
        fixture.engine.togglePause()
        runCurrent()

        // Zweiter Versuch liefert Frage 31, deren Sprechbeginn gleich den nächsten Block anfordert.
        assertEquals(3, refill.requests)
        assertEquals("Frage 31", fixture.tts.spokenTexts.last())
        assertEquals(Phase.SPEAKING, fixture.engine.state.value.phase)
        fixture.engine.close()
    }

    @Test
    fun `Fortsetzen hinter der letzten Frage wiederholt sie nicht sondern wartet auf Nachschub`() = runTest {
        val refill = FakeRefill().apply { responses += listOf("✨ Nachschub 1") }
        val fixture = fixture(
            questions = List(3) { question(it + 1) },
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 600_000),
            refill = refill,
            checkpoint = SessionCheckpoint(currentIndex = 3, currentRep = 1, remainingMs = 600_000),
        )

        fixture.engine.start()
        runCurrent()
        fixture.engine.togglePause()
        runCurrent()

        assertEquals(listOf("Nachschub 1"), fixture.tts.spokenTexts)
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
    fun `Sitzung aus dem Verlauf laeuft nach der letzten Frage mit Nachschub weiter`() = runTest {
        val refill = FakeRefill().apply { responses += listOf("✨ Nachgeladene Frage") }
        val fixture = fixture(
            questions = listOf(question(1)),
            config = config(pauseNextMs = 0, reps = 1),
            refill = refill,
        )
        fixture.startSpeaking()

        fixture.tts.completeCurrent()
        runCurrent()

        assertEquals(1, refill.requests)
        assertEquals(2, fixture.engine.state.value.questions.size)
        assertEquals("Nachgeladene Frage", fixture.tts.spokenTexts.last())
        assertEquals(Phase.SPEAKING, fixture.engine.state.value.phase)
        fixture.engine.close()
    }

    @Test
    fun `Sitzung ohne Nachschubquelle endet nicht sondern wartet`() = runTest {
        val fixture = fixture(questions = listOf(question(1)), config = config(reps = 1))
        fixture.startSpeaking()

        fixture.tts.completeCurrent()
        runCurrent()

        assertEquals(Phase.WAITING_NETWORK, fixture.engine.state.value.phase)
        fixture.engine.close()
    }

    @Test
    fun `Wiedergabe aus dem Verlauf spielt den gesamten Bestand und endet ohne Nachschub`() = runTest {
        val fixture = fixture(
            questions = List(270) { question(it + 1) },
            config = config(pauseRepMs = 0, pauseNextMs = 0, reps = 1, durationMs = 0),
            endWhenQuestionsExhausted = true,
        )
        fixture.startSpeaking()

        advanceTimeBy(600_001)
        runCurrent()

        repeat(270) {
            fixture.tts.completeCurrent()
            runCurrent()
        }

        assertEquals(List(270) { "Frage ${it + 1}" }, fixture.tts.spokenTexts)
        assertEquals(270, fixture.engine.state.value.questions.size)
        assertEquals(Phase.ENDED, fixture.engine.state.value.phase)
        fixture.engine.close()
    }

    private fun TestScope.fixture(
        questions: List<Question>,
        config: SessionConfig = config(),
        refill: FakeRefill? = null,
        initialGenerationInFlight: Boolean = false,
        checkpoint: SessionCheckpoint? = null,
        endWhenQuestionsExhausted: Boolean = false,
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
            initialGenerationInFlight = initialGenerationInFlight,
            checkpoint = checkpoint,
            endWhenQuestionsExhausted = endWhenQuestionsExhausted,
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
        data class Call(
            val text: String,
            val listener: SessionTtsPort.Listener,
            val varied: Boolean = false,
        )

        val calls = mutableListOf<Call>()
        val spokenTexts: List<String> get() = calls.map(Call::text)
        val offlineNoticeCount: Int
            get() = spokenTexts.count { it == SessionEngine.OFFLINE_MESSAGE }
        var stopCount = 0
        var pauseCount = 0
        var resumeCount = 0
        var canPause = true

        override fun speak(text: String, listener: SessionTtsPort.Listener, varied: Boolean) {
            calls += Call(text, listener, varied)
            listener.onStart()
        }

        override fun stop() {
            stopCount++
        }

        override fun pause(): Boolean {
            pauseCount++
            return canPause
        }

        override fun resume(): Boolean {
            resumeCount++
            return canPause
        }

        fun completeCurrent() = calls.last().listener.onComplete()

        fun errorCurrent() = calls.last().listener.onError()
    }

    private class FakeRefill : QuestionRefillPort {
        val responses = ArrayDeque<List<String>>()
        var requests = 0

        /** Bildet einen von aussen abgebrochenen OpenAI-Aufruf nach. */
        var cancelNextRequest = false

        /** Bildet einen Abbruch nach, der erst nach der ersten gelieferten Frage auftritt. */
        var failAfterFirstQuestion: String? = null

        /** Bildet eine Anfrage nach, die ohne jede Antwort offen bleibt. */
        var hangForever = false

        /** Bildet eine Anfrage nach, die liefert und danach offen bleibt. */
        var hangAfterQuestions = false

        override suspend fun requestQuestions(
            existingQuestions: List<Question>,
            onQuestion: suspend (Question) -> Unit,
        ): List<Question> {
            requests++
            if (cancelNextRequest) {
                cancelNextRequest = false
                throw CancellationException("Die OpenAI-Anfrage wurde abgebrochen.")
            }
            if (hangForever) awaitCancellation()
            val raw = if (responses.isEmpty()) emptyList() else responses.removeFirst()
            val delivered = mutableListOf<Question>()
            for (rawQuestion in raw) {
                val parsed = EmojiParser.parse(rawQuestion)
                if (parsed.text.isBlank()) continue
                val question = Question(emoji = parsed.emoji, text = parsed.text)
                delivered += question
                onQuestion(question)
                failAfterFirstQuestion?.let { message ->
                    failAfterFirstQuestion = null
                    throw IllegalStateException(message)
                }
            }
            if (hangAfterQuestions) awaitCancellation()
            return delivered
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
