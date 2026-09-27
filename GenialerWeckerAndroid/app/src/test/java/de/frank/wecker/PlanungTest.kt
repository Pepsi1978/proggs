package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Prüft die in AlarmScheduler tatsächlich genutzte Koordination: der Zustand wird INNERHALB des Tors gelesen und dort
 * auch angewendet. Die Abläufe werden über Latches erzwungen, nicht über Wartezeiten.
 */
class PlanungTest {
    /** Steht für den gespeicherten Stand (null = gelöscht). */
    private val store = AtomicReference<String?>("alt")
    /** Steht für den Slot im AlarmManager. */
    private val slot = AtomicReference<String?>(null)
    private val fehler = AtomicReference<Throwable?>(null)

    /**
     * Genau die Produktionsform von schedule() und cancel(): lesen im Tor, danach anwenden.
     * [beimAnwenden] hält den Auftrag mitten im Anwenden fest, nachdem er seinen Stand bereits gelesen hat.
     */
    private fun planen(beimAnwenden: (() -> Unit)? = null) = Planung.unter({ store.get() }) { current ->
        beimAnwenden?.invoke()
        slot.set(current)
        current
    }

    private fun thread(name: String, block: () -> Unit) = Thread({
        try { block() } catch (t: Throwable) { fehler.compareAndSet(null, t) }
    }, name).apply { start() }

    private fun warte(latch: CountDownLatch, was: String) =
        assertTrue("$was kam nicht innerhalb der Zeitgrenze", latch.await(5, TimeUnit.SECONDS))

    private fun ende(vararg threads: Thread) {
        threads.forEach { it.join(5000); assertTrue("${it.name} läuft noch", !it.isAlive) }
        fehler.get()?.let { throw it }
    }

    @Test fun lateOrderAfterACompletedPlanningKeepsTheCurrentState() {
        // Die neue Planung läuft vollständig durch.
        store.set("neu")
        planen()
        assertEquals("neu", slot.get())
        // Erst danach trifft der verspätete Auftrag ein — er plant den aktuellen Stand, nicht seinen eigenen alten Wunsch.
        val spaet = thread("spaet") { planen() }
        ende(spaet)
        assertEquals("neu", slot.get())
        assertEquals(store.get(), slot.get())
    }

    @Test fun anOrderHoldingTheGateWhileANewerStateIsSavedLeavesTheNewerSlot() {
        val imAnwenden = CountDownLatch(1)
        val weiter = CountDownLatch(1)
        // Der alte Auftrag hat "alt" bereits gelesen und blockiert mitten im Anwenden.
        val alt = thread("alt") { planen { imAnwenden.countDown(); warte(weiter, "Freigabe") } }
        warte(imAnwenden, "Alter Auftrag")
        // Währenddessen wird ein neuer Stand gespeichert und dafür eine neue Planung angestoßen; sie wartet am Tor.
        store.set("neu")
        val neu = thread("neu") { planen() }
        weiter.countDown()
        ende(alt, neu)
        // Der alte Auftrag hat "alt" gesetzt, der wartende danach "neu": der Endslot entspricht dem aktuellen Zustand.
        assertEquals("neu", slot.get())
        assertEquals(store.get(), slot.get())
    }

    @Test fun deletionEmptiesTheSlotButALateCancelKeepsANewlySavedAlarm() {
        // Wirklich gelöscht: der Slot wird geleert.
        store.set(null)
        assertNull(planen())
        assertNull(slot.get())
        // Verspäteter Abbruch: er blockiert im Anwenden, währenddessen wird ein Wecker neu gespeichert.
        val imAnwenden = CountDownLatch(1)
        val weiter = CountDownLatch(1)
        val spaeterAbbruch = thread("abbruch") { planen { imAnwenden.countDown(); warte(weiter, "Freigabe") } }
        warte(imAnwenden, "Abbruch")
        store.set("neu gespeichert")
        val neu = thread("neu") { planen() }
        weiter.countDown()
        ende(spaeterAbbruch, neu)
        // Der veraltete Abbruch räumt den neuen Slot nicht ab: am Ende steht der aktuelle Stand.
        assertEquals("neu gespeichert", slot.get())
    }
}
