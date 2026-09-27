package de.frank.wecker

import android.media.AudioManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AudioPauseTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun withQueue(pause: Long = 2000, check: (LinkedBlockingQueue<Long>, AlarmAudioQueue) -> Unit) {
        val audio = context.getSystemService(AudioManager::class.java)
        val volume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val starts = LinkedBlockingQueue<Long>()
        val errors = LinkedBlockingQueue<Exception>()
        val clip = PreparedAudio(Tones.file(AlarmStore.get(context).files, "chime").absolutePath, speed = 2f)
        lateinit var queue: AlarmAudioQueue
        try {
            audio.setStreamVolume(AudioManager.STREAM_ALARM, 1, 0)
            instrumentation.runOnMainSync {
                queue = AlarmAudioQueue(context, listOf(AlarmClip("Test", clip, pauseAfterMillis = pause)),
                    onPlaying = { starts.offer(SystemClock.elapsedRealtime()) }, onFailure = { errors.offer(it) })
                queue.start()
            }
            check(starts, queue)
            assertTrue(errors.toString(), errors.isEmpty())
        } finally {
            instrumentation.runOnMainSync { queue.close() }
            audio.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
        }
    }

    @Test fun twoSecondPauseIsIndependentOfDoublePlaybackSpeedAndRepeats() = withQueue { starts, _ ->
        var previous = starts.poll(5, TimeUnit.SECONDS) ?: error("Kein Audiostart")
        repeat(2) {
            val next = starts.poll(5, TimeUnit.SECONDS) ?: error("Kein Neustart nach der Pause")
            val elapsed = next - previous
            // 2 Sekunden Audiodatei mit Tempo 2 = 1 Sekunde Ton, anschließend 2 Sekunden Pause.
            assertTrue("Abstand der Starts: $elapsed ms", elapsed in 2900..4300)
            previous = next
        }
    }

    @Test fun closingDuringThePausePreventsAQueuedRestart() = withQueue { starts, queue ->
        assertNotNull(starts.poll(5, TimeUnit.SECONDS))
        SystemClock.sleep(1700)
        instrumentation.runOnMainSync { queue.close() }
        assertNull("Nach dem Stoppen darf kein vorgeladener Absatz anlaufen", starts.poll(2500, TimeUnit.MILLISECONDS))
    }

    @Test fun ideaPauseLastsOneAndAHalfSecondsAtDoubleSpeed() = withQueue(pause = 1500) { starts, _ ->
        val first = starts.poll(5, TimeUnit.SECONDS) ?: error("Kein Audiostart")
        val next = starts.poll(5, TimeUnit.SECONDS) ?: error("Kein Start nach der Ideenpause")
        assertTrue("Abstand der Starts: ${next - first} ms", next - first in 2400..3800)
    }
}
