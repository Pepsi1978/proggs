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
        assertTrue("$message\n${AlarmService.diagnosticEvents()}", condition())
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
                shell("input keyevent KEYCODE_HOME") // Der Wecker muss auch außerhalb seiner Oberfläche weiterlaufen.
                SystemClock.sleep(22_000)
                assertEquals(AlarmService.diagnosticEvents(), Step.MUSIC.title, AlarmService.state.value.step)
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
                AlarmScheduler(context).cancel(alarm.id); store.delete(alarm.id); store.ringing(store.ringing().filterNot { it == alarm.id })
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
        try {
            val voice = de.frank.genialeideen.speech.SyntheseStimme(settings).edgeFallback()
            val file = SpeechPreparation(context, settings) { voice }.audio("Guten Morgen. Dies ist der Offline-Test des genialen Weckers.")
            try {
                assertTrue(file.length() > 1000)
                val metadata = android.media.MediaMetadataRetriever()
                try {
                    metadata.setDataSource(file.absolutePath)
                    assertEquals("yes", metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
                    assertTrue(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong() > 1000)
                } finally { metadata.release() }
            } finally { file.delete() }
        } finally { settings.close() }
    }

    @Test fun sixRealSpeechVariantsAreStoredSeparately() = runBlocking {
        val alarm = Alarm(id = "test-six-voices", name = "Varianten-Test", enabled = false,
            text = "Guten Morgen. Dies ist Variante meines Weckers.", steps = listOf(Step.TEXT))
        val settings = de.frank.genialeideen.data.settings.SecureSettings(context)
        try {
            store.put(alarm)
            val voice = de.frank.genialeideen.speech.SyntheseStimme(settings).edgeFallback()
            SpeechPreparation(context, settings) { voice }.prepare(alarm)
            val saved = store.get(alarm.id)!!
            assertEquals(6, saved.voiceVariants.size)
            val paths = saved.voiceVariants.flatMap { it.steps.getValue("TEXT") }.map { it.path }
            assertEquals(6, paths.distinct().size)
            assertTrue(paths.all { File(it).length() > 1000 })
        } finally { store.delete(alarm.id); settings.close() }
    }

    @Test fun twoAlarmsAtTheSameTimeRemainIndependent() {
        org.junit.Assume.assumeTrue(store.ringing().isEmpty())
        val at = System.currentTimeMillis() + 3000
        val alarms = listOf("test-parallel-a", "test-parallel-b").map { Alarm(id = it, name = "Mehrfach-Wecktest", nextAt = at, volume = 1, vibrate = false) }
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                alarms.forEach { alarm -> store.put(alarm); AlarmScheduler(context).schedule(alarm) }
                waitFor("Beide AlarmManager-Termine müssen eintreffen") { store.ringing().containsAll(alarms.map { it.id }) && AlarmService.state.value.playing }
                val first = AlarmService.state.value.alarm!!.id
                command("STOP")
                waitFor("Nach dem ersten Alarm muss der zweite separat klingeln") { AlarmService.state.value.alarm?.id in alarms.map { it.id } && AlarmService.state.value.alarm?.id != first && AlarmService.state.value.playing }
                command("STOP")
                waitFor("Beide Alarme müssen beendet sein") { AlarmService.state.value.alarm == null }
                assertTrue(alarms.all { store.get(it.id) != null })
            } finally {
                command("STOP")
                alarms.forEach { AlarmScheduler(context).cancel(it.id); store.delete(it.id) }
                store.ringing(store.ringing().filterNot { id -> alarms.any { it.id == id } })
            }
        }
    }
    @Test fun offlinePlaybackAdvancesThroughAllSixVariantsAndWraps() {
        val clip = Tones.file(store.files, "chime").absolutePath
        val alarm = Alarm(id = "test-variant-cycle", enabled = false, volume = 1, vibrate = false, steps = listOf(Step.TEXT),
            voiceVariants = List(6) { index -> VoiceVariant(mapOf("TEXT" to listOf(PreparedAudio(clip, VoiceVariations.rate(1f, index))))) })
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                store.put(alarm)
                context.startForegroundService(Intent(context, AlarmService::class.java).setAction("TEST").putExtra("id", alarm.id))
                (listOf(1, 2, 3, 4, 5, 6, 1)).forEach { variation ->
                    waitFor("Variante $variation muss abgespielt werden", 10_000) { AlarmService.state.value.alarm?.id == alarm.id && AlarmService.state.value.variation == variation && AlarmService.state.value.playing }
                }
                command("STOP")
                waitFor("Vorladekette muss vollständig stoppen") { AlarmService.state.value.alarm == null }
            } finally { command("STOP"); store.delete(alarm.id) }
        }
    }
}
