package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Prüft die in AlarmScheduler tatsächlich genutzte Koordination: Zustand wird INNERHALB des Tors gelesen und angewendet.
 * Die Abläufe werden über Latches erzwungen, nicht über Wartezeiten.
 */
class PlanungTest {
    /** Steht für den gespeicherten Stand (null = gelöscht). */
    private val store = AtomicReference<String?>("alt")
    /** Steht für den Slot im AlarmManager. */
    private val slot = AtomicReference<String?>(null)
    private val fehler = AtomicReference<Throwable?>(null)

    /** Genau die Produktionsform: lesen im Tor, danach anwenden — wie schedule() und cancel(). */
    private fun planen(vorLesen: (() -> Unit)? = null) = Planung.unter({ vorLesen?.invoke(); store.get() }) { current ->
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

    @Test fun lateOrderPlansTheCurrentStateInsteadOfItsOwnSnapshot() {
        val imTor = CountDownLatch(1)
        val weiter = CountDownLatch(1)
        // Alter Auftrag betritt das Tor und hält es, bevor er liest.
        val alt = thread("alt") { planen { imTor.countDown(); warte(weiter, "Freigabe") } }
        warte(imTor, "Alter Auftrag")
        // Währenddessen wird ein neuer Stand gespeichert; die neue Planung wartet am Tor.
        store.set("neu")
        val neu = thread("neu") { planen() }
        weiter.countDown()
        ende(alt, neu)
        // Der alte Auftrag hat den NEUEN Stand gelesen; der letzte Slot entspricht dem aktuellen Wunschzustand.
        assertEquals("neu", slot.get())
        assertEquals(store.get(), slot.get())
    }

    @Test fun oneHoldsTheGateWhileTheOtherWaitsAndNeverOverlaps() {
        val drin = java.util.concurrent.atomic.AtomicInteger(0)
        val hoechstens = java.util.concurrent.atomic.AtomicInteger(0)
        val imTor = CountDownLatch(1)
        val weiter = CountDownLatch(1)
        val zweiterGestartet = CountDownLatch(1)
        fun zaehlen() = Planung.unter({ store.get() }) { current ->
            val jetzt = drin.incrementAndGet()
            hoechstens.updateAndGet { maxOf(it, jetzt) }
            slot.set(current)
            drin.decrementAndGet()
        }
        val haltend = thread("haltend") { Planung.unter({ imTor.countDown(); warte(weiter, "Freigabe"); store.get() }) { drin.incrementAndGet(); slot.set(it); drin.decrementAndGet() } }
        warte(imTor, "Halter")
        val wartend = thread("wartend") { zweiterGestartet.countDown(); zaehlen() }
        warte(zweiterGestartet, "Zweiter Thread")
        // Der zweite Auftrag kann das Tor nicht betreten, solange der erste es hält.
        assertEquals(0, drin.get())
        weiter.countDown()
        ende(haltend, wartend)
        assertEquals("Es war nie mehr als ein Auftrag im Tor", 1, hoechstens.get())
    }

    @Test fun deletionEmptiesTheSlotButALateCancelKeepsANewlySavedAlarm() {
        // Wirklich gelöscht: der Slot wird geleert.
        store.set(null)
        assertNull(planen())
        assertNull(slot.get())
        // Verspäteter Abbruch: er hält das Tor, währenddessen wird ein Wecker neu gespeichert.
        val imTor = CountDownLatch(1)
        val weiter = CountDownLatch(1)
        val spaeterAbbruch = thread("abbruch") { planen { imTor.countDown(); warte(weiter, "Freigabe") } }
        warte(imTor, "Abbruch")
        store.set("neu gespeichert")
        weiter.countDown()
        ende(spaeterAbbruch)
        // Der veraltete Abbruch entfernt den neuen Slot nicht, er plant den aktuellen Stand.
        assertEquals("neu gespeichert", slot.get())
    }
}
