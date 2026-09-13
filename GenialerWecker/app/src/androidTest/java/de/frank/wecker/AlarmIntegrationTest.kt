package de.frank.wecker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AlarmIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val store = AlarmStore.get(context)
    private fun waitFor(message: String, timeout: Long = 15_000, condition: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + timeout
        while (!condition() && SystemClock.elapsedRealtime() < until) SystemClock.sleep(100)
        assertTrue(message, condition())
    }
    private fun command(action: String, id: String? = null) {
        context.startService(Intent(context, AlarmService::class.java).setAction(action).putExtra("id", id))
    }
    private fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText().trim()
    }

    @Test fun exactAlarmRingsOfflineAndRestoresVolume() {
        val audio = context.getSystemService(AudioManager::class.java)
        val original = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val alarm = Alarm(id = "test-exact", name = "Automatischer Wecktest", volume = 1, vibrate = false,
            nextAt = System.currentTimeMillis() + 3000)
        val wifi = shell("settings get global wifi_on")
        val data = shell("settings get global mobile_data")
        val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
        val originalFilter = notificationManager.currentInterruptionFilter
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                shell("svc wifi disable"); shell("svc data disable")
                shell("cmd notification set_dnd alarms")
                assertEquals(android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS, notificationManager.currentInterruptionFilter)
                store.put(alarm); AlarmScheduler(context).schedule(alarm)
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                waitFor("Der echte AlarmManager muss den Dienst starten und Audio abspielen") {
                    AlarmService.state.value.alarm?.id == alarm.id && AlarmService.state.value.playing
                }
                assertEquals(1, audio.getStreamVolume(AudioManager.STREAM_ALARM))
                assertFalse("Nicht stören muss den Weckkanal hörbar lassen", audio.isStreamMute(AudioManager.STREAM_ALARM))
                assertFalse("Einmaliger Alarm muss verbraucht sein", store.get(alarm.id)!!.enabled)
                command("STOP")
                waitFor("Der Dienst muss stoppen") { AlarmService.state.value.alarm == null }
                assertEquals(original, audio.getStreamVolume(AudioManager.STREAM_ALARM))
            } finally {
                command("STOP")
                AlarmScheduler(context).cancel(alarm.id); store.delete(alarm.id)
                if (wifi == "1") shell("svc wifi enable")
                if (data == "1") shell("svc data enable")
                shell("cmd notification set_dnd " + when (originalFilter) { 2 -> "priority"; 3 -> "none"; 4 -> "alarms"; else -> "all" })
            }
        }
    }

    @Test fun fullSongPlaysPastTwentySecondsAndThenAdvances() {
        val file = File(store.files, "test-long-song.mp3").apply {
            instrumentation.context.assets.open("test-song.mp3").use { input -> outputStream().use { input.copyTo(it) } }
        }
        val alarm = Alarm(id = "test-long", name = "Langer Song", enabled = false, volume = 1, vibrate = false,
            music = file.absolutePath, steps = listOf(Step.MUSIC, Step.TONE))
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                store.put(alarm)
                context.startForegroundService(Intent(context, AlarmService::class.java).setAction("TEST").putExtra("id", alarm.id))
                waitFor("Die Musik muss beginnen") { AlarmService.state.value.playing }
                SystemClock.sleep(22_000)
                assertEquals(Step.MUSIC.title, AlarmService.state.value.step)
                assertTrue(AlarmService.state.value.playing)
                waitFor("Erst nach dem vollständigen 30-Sekunden-Song folgt das Klingelzeichen", 15_000) { AlarmService.state.value.step == Step.TONE.title }
                command("STOP")
                waitFor("Test muss beendet sein") { AlarmService.state.value.alarm == null }
            } finally { command("STOP"); store.delete(alarm.id); file.delete() }
        }
    }

    @Test fun photoAlarmRejectsNormalStopAndSnoozeKeepsShiftAnchor() {
        val alarm = Alarm(id = "test-photo", name = "Foto-Test", volume = 1, vibrate = false, photoRequired = true,
            color = "blue", startDate = "2026-09-13", intervalDays = 35, snoozeLimit = 1)
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                store.put(alarm); store.ringing(listOf(alarm.id))
                context.startForegroundService(Intent(context, AlarmService::class.java).setAction("RING"))
                waitFor("Fotoalarm muss starten") { AlarmService.state.value.playing }
                command("STOP"); SystemClock.sleep(500)
                assertEquals(alarm.id, AlarmService.state.value.alarm?.id)
                command("SNOOZE")
                waitFor("Schlummern muss den aktuellen Alarm beenden") { AlarmService.state.value.alarm == null }
                val updated = store.get(alarm.id)!!
                assertTrue(updated.snoozeUntil > System.currentTimeMillis())
                assertEquals(1, updated.snoozes)
                assertEquals(35, updated.intervalDays)
                assertEquals(alarm.startDate, updated.startDate)
            } finally {
                command("PHOTO_OK", alarm.id)
                AlarmScheduler(context).cancel(alarm.id); store.delete(alarm.id); store.ringing(emptyList())
            }
        }
    }

    @Test fun ideasBridgeReadsOnlyOpenIdeasInOriginalOrder() = runBlocking {
        val ideas = IdeasBridge(context).refresh()
        assertEquals(ideas.map { it.id }, IdeasBridge(context).cached().map { it.id })
        context.contentResolver.query(IdeasBridge.URI, null, null, null, null)!!.use { cursor ->
            var previous = Int.MIN_VALUE
            while (cursor.moveToNext()) {
                val order = cursor.getInt(3)
                assertTrue(order >= previous); previous = order
            }
        }
    }

    @Test fun offlinePhotoConditionsAcceptBlueAndRejectBlack() {
        val file = File(context.cacheDir, "test-photo.jpg")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.BLUE)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            val alarm = Alarm(photoRequired = true, color = "blue", colorPercent = 70)
            assertTrue(PhotoCheck.check(file, alarm).accepted)
            assertFalse(PhotoCheck.check(file, alarm.copy(color = "red")).accepted)
            bitmap.eraseColor(Color.BLACK)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            assertFalse(PhotoCheck.check(file, alarm).accepted)
        } finally { bitmap.recycle(); file.delete() }
    }

    @Test fun edgeProducesPlayableOfflineAudio() = runBlocking {
        val settings = de.frank.genialeideen.data.settings.SecureSettings(context)
        val provider = settings.ttsProvider
        try {
            settings.ttsProvider = de.frank.genialeideen.tts.TtsProvider.EDGE.id
            val file = SpeechPreparation(context, settings).audio("Guten Morgen. Dies ist der Offline-Test des genialen Weckers.")
            try {
                assertTrue(file.length() > 1000)
                val metadata = android.media.MediaMetadataRetriever()
                try {
                    metadata.setDataSource(file.absolutePath)
                    assertEquals("yes", metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
                    assertTrue(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong() > 1000)
                } finally { metadata.release() }
            } finally { file.delete() }
        } finally { settings.ttsProvider = provider; settings.close() }
    }
}
