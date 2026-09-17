package de.frank.wecker

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EditorRegressionTest {
    private fun withModel(block: (WeckerViewModel) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as android.app.Application
        val prefs = AlarmStore.get(app).prefs
        val draft = prefs.getString("draft", null)
        val mode = prefs.getBoolean("draft_is_new", false)
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        lateinit var vm: WeckerViewModel
        instrumentation.runOnMainSync { vm = ViewModelProvider(owner, ViewModelProvider.AndroidViewModelFactory(app))[WeckerViewModel::class.java] }
        try { block(vm) } finally {
            instrumentation.runOnMainSync { owner.viewModelStore.clear() }
            prefs.edit().putString("draft", draft).putBoolean("draft_is_new", mode).commit()
        }
    }
    @Test fun aLateEditorCallbackCannotReplaceTheNewDraft() = withModel { vm ->
        val old = Alarm(name = "Alter Entwurf")
        val fresh = Alarm(name = "Neuer Entwurf")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            vm.edit(old); vm.edit(fresh)
            vm.change(old.copy(hour = 19)) // verspäteter Picker / ausblendender alter Editor
            assertEquals(fresh.id, vm.draft.value?.id)
        }
    }
    @Test fun savingTwoAlarmsPreservesBothAndActivatesBoth() = withModel { vm ->
        val first = Alarm(id = "test-editor-first", enabled = false, volume = 1, vibrate = false)
        val second = Alarm(id = "test-editor-second", enabled = false, volume = 1, vibrate = false)
        val idsBefore = vm.store.all().map { it.id }.toSet()
        try {
            listOf(first, second).forEach { alarm ->
                val saved = CountDownLatch(1)
                InstrumentationRegistry.getInstrumentation().runOnMainSync { vm.edit(alarm); vm.save { saved.countDown() } }
                assertTrue("Speichern muss abschließen", saved.await(10, TimeUnit.SECONDS))
            }
            assertTrue(vm.store.get(first.id)!!.enabled)
            assertTrue(vm.store.get(second.id)!!.enabled)
            assertEquals(idsBefore + setOf(first.id, second.id), vm.store.all().map { it.id }.toSet())
        } finally {
            listOf(first, second).forEach { vm.store.delete(it.id); vm.scheduler.cancel(it.id) }
        }
    }
    @Test fun uploadedOwnVoicesLoadWithoutPressingRefresh() = withModel { vm ->
        org.junit.Assume.assumeTrue("Kein Alibaba-Schlüssel eingerichtet", vm.settings.qwenTtsApiKey.isNotBlank())
        val until = android.os.SystemClock.elapsedRealtime() + 40_000
        while (vm.voicesLoading.value && android.os.SystemClock.elapsedRealtime() < until) android.os.SystemClock.sleep(100)
        assertFalse("Stimmenliste muss fertig geladen sein", vm.voicesLoading.value)
        assertTrue(vm.voiceLoadError.value, vm.voiceLoadError.value.isBlank())
        assertTrue("Eigene hochgeladene Stimmen erwartet", vm.clonedVoices.value.isNotEmpty())
        assertTrue(vm.clonedVoices.value.all { it.id.startsWith("qwen-") })
        println("Eigene Alibaba-Stimmen geladen: ${vm.clonedVoices.value.size}")
    }
    @Test fun editingWhileSavingKeepsTheLatestDraft() = withModel { vm ->
        val alarm = Alarm(id = "test-editor-inflight", enabled = false, volume = 1, vibrate = false)
        val closed = java.util.concurrent.atomic.AtomicBoolean(false)
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                vm.edit(alarm)
                vm.save { closed.set(true) }
                vm.change(alarm.copy(name = "Änderung während des Speicherns"))
            }
            val until = android.os.SystemClock.elapsedRealtime() + 10_000
            while ((vm.store.get(alarm.id) == null || vm.busy.value.isNotBlank()) && android.os.SystemClock.elapsedRealtime() < until) android.os.SystemClock.sleep(50)
            assertFalse(closed.get())
            assertEquals("Änderung während des Speicherns", vm.draft.value?.name)
            val saved = CountDownLatch(1)
            InstrumentationRegistry.getInstrumentation().runOnMainSync { vm.save { saved.countDown() } }
            assertTrue(saved.await(10, TimeUnit.SECONDS))
            assertEquals("Änderung während des Speicherns", vm.store.get(alarm.id)?.name)
        } finally { vm.store.delete(alarm.id); vm.scheduler.cancel(alarm.id) }
    }
}
