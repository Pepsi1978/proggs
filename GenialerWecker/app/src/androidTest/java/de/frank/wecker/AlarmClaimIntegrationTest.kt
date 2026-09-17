package de.frank.wecker

import android.app.NotificationManager
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Nicht invasiv: keine Uhrzeit-, Netz-, Nicht-stören- oder Berechtigungsänderungen, nur eigene Test-IDs. */
@RunWith(AndroidJUnit4::class)
class AlarmClaimIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AlarmStore.get(context)
    private val scheduler = AlarmScheduler(context)
    private val ids = mutableListOf<String>()
    private var initialized = false

    private fun waitFor(message: String, timeout: Long = 15_000, condition: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + timeout
        while (!condition() && SystemClock.elapsedRealtime() < until) SystemClock.sleep(100)
        assertTrue("$message\n${AlarmService.diagnosticEvents()}", condition())
    }
    /** Always addressed to a test alarm; the service ignores it for any other ringing alarm. */
    private fun command(action: String, id: String) { context.startService(Intent(context, AlarmService::class.java).setAction(action).putExtra("id", id)) }
    private fun alarm(id: String, at: Long) = Alarm(id = id, name = "Annahme-Test", days = (1..7).toSet(), nextAt = at, volume = 1, vibrate = false)
        .also { ids += id; store.put(it) }
    private fun fire(id: String, at: Long, snooze: Boolean = false) = AlarmReceiver().onReceive(context,
        Intent(context, AlarmReceiver::class.java).putExtra("id", id).putExtra("snooze", snooze).putExtra("at", at))

    @Before fun requireQuietDevice() {
        assumeTrue("Es klingelt bereits ein echter Wecker", store.ringing().isEmpty() && AlarmService.state.value.alarm == null)
        initialized = true
    }

    @After fun cleanUp() {
        // Runs after an assumption abort too: then nothing was created and nothing may be touched.
        if (!initialized) return
        AlarmScheduler.failScheduleForTestId = null; AlarmRinging.failStartForTestId = null; AlarmStore.failRingingWriteForTestId = null
        AlarmService.state.value.alarm?.id?.takeIf { it in ids }?.let { command("STOP", it) }
        waitFor("Test-Dienst muss enden") { AlarmService.state.value.alarm?.id !in ids }
        if (TestSnooze.pendingForTest()?.id in ids) TestSnooze.cancel(context)
        ids.forEach { scheduler.cancel(it); store.delete(it) }
        store.ringing(store.ringing().filterNot { it in ids })
    }

    @Test fun followUpPlanningFailureStillRingsPersistsAtomicallyAndRetriesAfterStop() {
        val at = System.currentTimeMillis() - 1000
        // Launch first: the ViewModel restore() would otherwise treat the overdue test alarm as missed.
        ActivityScenario.launch(MainActivity::class.java).use {
            alarm("claim-plan-failure", at)
            AlarmScheduler.failScheduleForTestId = "claim-plan-failure"
            fire("claim-plan-failure", at)
            waitFor("Trotz Planungsfehler muss der aktuelle Wecker klingeln") { AlarmService.state.value.alarm?.id == "claim-plan-failure" && AlarmService.state.value.playing }
            val entry = store.ringingEntries().single { it.id == "claim-plan-failure" }
            assertEquals(at, entry.at)
            val advanced = store.get("claim-plan-failure")!!
            assertTrue("Folgezustand muss mit dem Klingelauftrag gespeichert sein", advanced.nextAt > System.currentTimeMillis())
            // Raw preferences prove both keys were committed, not just the in-memory state.
            val raw = context.createDeviceProtectedStorageContext().getSharedPreferences("alarms_v1", 0)
            assertTrue(raw.getString("alarms", "")!!.contains(advanced.nextAt.toString()))
            assertTrue(raw.getString("ringingMeta", "")!!.contains(at.toString()))
            assertTrue(store.issues.value["claim-plan-failure"].orEmpty().startsWith(AlarmScheduler.PLAN_ISSUE))
            AlarmScheduler.failScheduleForTestId = null
            command("STOP", "claim-plan-failure")
            waitFor("Stoppen muss beenden und die Planung erneut versuchen") {
                AlarmService.state.value.alarm == null && store.issues.value["claim-plan-failure"] == null
            }
            assertEquals(advanced.nextAt, store.get("claim-plan-failure")!!.nextAt)
            assertTrue("claim-plan-failure" !in store.ringing())
        }
    }

    @Test fun duplicateBroadcastRingsOnceAndAdvancesOnce() {
        val at = System.currentTimeMillis() - 1000
        // Launch first: the ViewModel restore() would otherwise treat the overdue test alarm as missed.
        ActivityScenario.launch(MainActivity::class.java).use {
            alarm("claim-duplicate", at)
            fire("claim-duplicate", at)
            val next = store.get("claim-duplicate")!!.nextAt
            fire("claim-duplicate", at)
            assertEquals(next, store.get("claim-duplicate")!!.nextAt)
            assertEquals(1, store.ringingEntries().count { it.id == "claim-duplicate" })
            waitFor("Wecker muss klingeln") { AlarmService.state.value.playing }
            command("STOP", "claim-duplicate")
            waitFor("Wecker muss enden") { AlarmService.state.value.alarm == null }
            SystemClock.sleep(1500)
            assertNull("Ein doppelter Broadcast darf kein zweites Klingeln erzeugen", AlarmService.state.value.alarm)
        }
    }

    @Test fun interruptedRingingIsRecoveredOnceWithoutTouchingRhythm() {
        val at = System.currentTimeMillis() - 1000
        alarm("claim-process", at)
        // Simulates a process death right after the atomic claim: no service was ever started.
        assertNotNull(scheduler.claim("claim-process", false, at))
        val claimed = store.get("claim-process")!!
        scheduler.restoreOne(claimed, store.ringingEntries().single { it.id == "claim-process" }, idle = true, clockChanged = false, now = System.currentTimeMillis())
        val restored = store.get("claim-process")!!
        assertEquals("nextAt darf bei der Wiederherstellung nicht verschoben werden", claimed.nextAt, restored.nextAt)
        assertTrue(restored.snoozeUntil > 0)
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("Genau ein Nachholen über den Schlummer-Slot") { AlarmService.state.value.alarm?.id == "claim-process" && AlarmService.state.value.playing }
            command("STOP", "claim-process")
            waitFor("Nachholen muss enden") { AlarmService.state.value.alarm == null }
            SystemClock.sleep(3000)
            assertNull(AlarmService.state.value.alarm)
            assertEquals(claimed.nextAt, store.get("claim-process")!!.nextAt)
        }
    }

    @Test fun snoozePlanningFailureKeepsRingingAndPersistsNothing() {
        val at = System.currentTimeMillis() - 1000
        // Launch first: the ViewModel restore() would otherwise treat the overdue test alarm as missed.
        ActivityScenario.launch(MainActivity::class.java).use {
            alarm("claim-snooze", at)
            fire("claim-snooze", at)
            waitFor("Wecker muss klingeln") { AlarmService.state.value.playing }
            AlarmScheduler.failScheduleForTestId = "claim-snooze"
            command("SNOOZE", "claim-snooze"); SystemClock.sleep(800)
            assertEquals("claim-snooze", AlarmService.state.value.alarm?.id)
            assertEquals(0L, store.get("claim-snooze")!!.snoozeUntil)
            assertEquals(0, store.get("claim-snooze")!!.snoozes)
            AlarmScheduler.failScheduleForTestId = null
            command("SNOOZE", "claim-snooze")
            waitFor("Schlummern muss beenden") { AlarmService.state.value.alarm == null }
            val snoozed = store.get("claim-snooze")!!
            assertTrue(snoozed.snoozeUntil > System.currentTimeMillis()); assertEquals(1, snoozed.snoozes)
        }
    }

    @Test fun blockedServiceStartShowsBoundedFallbackThatEndsWithRealRinging() {
        val manager = context.getSystemService(NotificationManager::class.java)
        assumeTrue("Benachrichtigungen sind nicht erlaubt; Ersatzsignal ist dann nicht prüfbar", manager.areNotificationsEnabled())
        val at = System.currentTimeMillis() - 1000
        // Launch first: the ViewModel restore() would otherwise treat the overdue test alarm as missed.
        ActivityScenario.launch(MainActivity::class.java).use {
            alarm("claim-fallback", at)
            try { fallbackScenario(manager, at) } finally { AlarmRinging.cancelFallback(context) }
        }
    }
    private fun fallbackScenario(manager: NotificationManager, at: Long) {
        run {
            AlarmRinging.failStartForTestId = "claim-fallback"
            fire("claim-fallback", at)
            waitFor("Ersatzsignal muss erscheinen", 5000) { manager.activeNotifications.any { it.id == AlarmRinging.FALLBACK_NOTIFICATION } }
            assertTrue("claim-fallback" in store.ringing())
            AlarmRinging.failStartForTestId = null
            context.startForegroundService(Intent(context, AlarmService::class.java).setAction("RING"))
            waitFor("Echter Dienst muss übernehmen und das Ersatzsignal beenden") {
                AlarmService.state.value.playing && manager.activeNotifications.none { it.id == AlarmRinging.FALLBACK_NOTIFICATION }
            }
        }
    }

    @Test fun stopStaysEffectiveWhenRingingRemovalCannotBePersisted() {
        val at = System.currentTimeMillis() - 1000
        ActivityScenario.launch(MainActivity::class.java).use {
            alarm("claim-stop-persist", at)
            fire("claim-stop-persist", at)
            waitFor("Wecker muss klingeln") { AlarmService.state.value.alarm?.id == "claim-stop-persist" && AlarmService.state.value.playing }
            AlarmStore.failRingingWriteForTestId = "claim-stop-persist"
            command("STOP", "claim-stop-persist")
            waitFor("Stoppen muss trotz Speicherfehler wirken") { AlarmService.state.value.alarm == null }
            assertTrue("Der Speicherfehler muss real simuliert sein", "claim-stop-persist" in store.ringing())
            SystemClock.sleep(2500)
            assertNull("Der gestoppte Auftrag darf nicht sofort erneut klingeln", AlarmService.state.value.alarm)
            // Opening the app runs restore(): it must not re-fire the stopped occurrence either.
            scheduler.restore(onlyIds = setOf("claim-stop-persist"))
            assertEquals(0L, store.get("claim-stop-persist")!!.snoozeUntil)
            AlarmStore.failRingingWriteForTestId = null
            scheduler.restore(onlyIds = setOf("claim-stop-persist"))
            assertTrue("Die Entfernung wird beim nächsten Schreibversuch nachgeholt", "claim-stop-persist" !in store.ringing())
            SystemClock.sleep(2500)
            assertNull(AlarmService.state.value.alarm)
        }
    }

    @Test fun testRingUsesRealPathWithVolatileSnoozeAndNeverTouchesStoredAlarm() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val before = alarm("test-one-to-one", System.currentTimeMillis() + 6 * 60 * 60_000L)
            context.startForegroundService(Intent(context, AlarmService::class.java).setAction("TEST").putExtra("id", before.id))
            waitFor("Test muss wie echt klingeln") { AlarmService.state.value.let { it.test && it.playing && it.alarm?.id == before.id } }
            command("SNOOZE", before.id)
            waitFor("Test-Schlummern muss wie echt beenden") { AlarmService.state.value.alarm == null }
            val pending = TestSnooze.pendingForTest()!!
            assertEquals(1, pending.snoozes)
            assertEquals("Gespeicherter Wecker darf sich nicht ändern", before, store.get(before.id))
            assertTrue(store.ringing().isEmpty())
            TestSnoozeReceiver().onReceive(context, Intent().putExtra("token", pending.token))
            waitFor("Test muss nach der Pause mit flüchtigem Zähler zurückkommen") {
                AlarmService.state.value.let { it.test && it.playing && it.alarm?.snoozes == 1 }
            }
            command("STOP", before.id)
            waitFor("Test muss enden") { AlarmService.state.value.alarm == null }
            assertNull(TestSnooze.pendingForTest())
            TestSnoozeReceiver().onReceive(context, Intent().putExtra("token", pending.token))
            SystemClock.sleep(1500)
            assertNull("Ein verbrauchter Test-Weckruf darf nicht erneut klingeln", AlarmService.state.value.alarm)
            assertEquals(before, store.get(before.id))
        }
    }
}
