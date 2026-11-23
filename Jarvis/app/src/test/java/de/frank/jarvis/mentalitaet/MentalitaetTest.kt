package de.frank.jarvis.mentalitaet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class MentalitaetTest {

    @Test
    fun planWiederholtSaetzeInReihenfolgeUndSchleiftKorrekt() {
        val a = MentalSatz(1L, "Ich bleibe ruhig und klar.", wiederholungen = 2)
        val b = MentalSatz(2L, "Ich handle entschlossen.", wiederholungen = 3)
        val plan = mentalitaetPlan(listOf(a, b))

        assertEquals(5, plan.size)
        assertEquals(
            listOf(
                "Ich bleibe ruhig und klar.",
                "Ich bleibe ruhig und klar.",
                "Ich handle entschlossen.",
                "Ich handle entschlossen.",
                "Ich handle entschlossen.",
            ),
            plan.map { it.text },
        )
        assertEquals(listOf(1, 2, 1, 2, 3), plan.map { it.wiederholung })
        assertEquals(listOf(2, 2, 3, 3, 3), plan.map { it.wiederholungenGesamt })
        // Übergang zum nächsten Durchlauf: Index 5 % 5 == 0 -> wieder Satz A
        assertEquals(a.id, plan[5 % plan.size].satzId)
    }

    @Test
    fun planUeberspringtLeereListeUndKlemmtWiederholungen() {
        assertTrue(mentalitaetPlan(emptyList()).isEmpty())
        assertTrue(mentalitaetPlan(listOf(MentalSatz(1L, "   ", 3))).isEmpty())

        val unten = mentalitaetPlan(listOf(MentalSatz(1L, "A", 0)))
        assertEquals(1, unten.size)
        assertEquals(1, unten.single().wiederholungenGesamt)

        val oben = mentalitaetPlan(listOf(MentalSatz(1L, "A", 99)))
        assertEquals(10, oben.size)
        assertEquals(10, oben.last().wiederholung)
    }

    @Test
    fun umsortierenBestimmtNeuenPlanUndBehaeltUnbekannteHinten() {
        val a = MentalSatz(10L, "A", 2)
        val b = MentalSatz(20L, "B", 3)
        val c = MentalSatz(30L, "C", 1)
        val neu = ordneNachIds(listOf(a, b, c), listOf(20L, 10L))
        assertEquals(listOf(20L, 10L, 30L), neu.map { it.id })
        val plan = mentalitaetPlan(neu)
        assertEquals(listOf("B", "B", "B", "A", "A", "C"), plan.map { it.text })
    }

    @Test
    fun serialisierungUndParsingBehaltenTextReihenfolgeUndWiederholungen() {
        val ur = listOf(
            MentalSatz(101L, "Satz eins", 2),
            MentalSatz(202L, "Satz zwei", 7),
        )
        val json = MentalitaetSpeicher.serialisiereListe(ur)
        val geladen = MentalitaetSpeicher.parseListe(json)
        assertEquals(ur, geladen)
    }

    @Test
    fun zusammenfuehrenBehaeltBeidseitigeAenderungenUndNeueReihenfolge() {
        val basis = MentalitaetSpeicher.serialisiereListe(listOf(MentalSatz(1L, "A", 2), MentalSatz(2L, "B", 3)))
        val lokal = MentalitaetSpeicher.serialisiereListe(listOf(MentalSatz(2L, "B", 3), MentalSatz(1L, "A", 4)))
        val fern = MentalitaetSpeicher.serialisiereListe(listOf(MentalSatz(1L, "A", 2), MentalSatz(2L, "B neu", 5), MentalSatz(3L, "C", 1)))

        val gemischt = MentalitaetSpeicher.parseListe(MentalitaetSpeicher.fuehreZusammen(basis, lokal, fern, fernGewinnt = true))
        // Lokal hat 2 vor 1 umsortiert; neuer Satz 3 folgt hinter seinem Vorgänger 2.
        assertEquals(listOf(2L, 3L, 1L), gemischt.map { it.id })
        assertEquals("B neu", gemischt[0].text)
        assertEquals(5, gemischt[0].wiederholungen)
        assertEquals("C", gemischt[1].text)
        assertEquals(4, gemischt[2].wiederholungen)
    }

    @Test
    fun schleifeErzeugtJedeAusgabeFrischMitFuenfSekundenPauseUndLoestAABBBADurchlauf() = runBlocking {
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 2), MentalSatz(2L, "B", 3)))
        val erzeugteTexte = CopyOnWriteArrayList<String>()
        val erzeugteDateien = CopyOnWriteArrayList<String>()
        val gespielteDateien = CopyOnWriteArrayList<String>()
        val geloeschteDateien = CopyOnWriteArrayList<String>()
        val pausenMs = CopyOnWriteArrayList<Long>()
        val zaehler = AtomicInteger(0)
        val fertig = CompletableDeferred<Unit>()

        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMs = 5_000L,
                erzeuge = { text ->
                    val nr = zaehler.incrementAndGet()
                    erzeugteTexte += text
                    File("/tmp/mental-$nr-$text.wav").also { erzeugteDateien += it.name }
                },
                spiele = { datei ->
                    gespielteDateien += datei.name
                    if (gespielteDateien.size == 6) {
                        fertig.complete(Unit)
                        throw CancellationException("Abnahme-Grenze erreicht")
                    }
                },
                loesche = { datei -> geloeschteDateien += datei.name },
                pause = { ms -> pausenMs += ms },
            )
        }
        fertig.await()
        lauf.join()

        // Genau A, A, B, B, B, A — 6 verschiedene, frisch erzeugte Dateien, keine Wiederverwendung.
        assertEquals(listOf("A", "A", "B", "B", "B", "A"), erzeugteTexte.take(6))
        assertEquals(6, gespielteDateien.size)
        assertEquals(6, gespielteDateien.toSet().size)
        // Genau 5 Pausen à 5000 ms zwischen den 6 Ausgaben (Wiederholung, Satzwechsel, Durchlaufübergang).
        assertEquals(listOf(5_000L, 5_000L, 5_000L, 5_000L, 5_000L), pausenMs)
        // Jede erzeugte Datei wurde gelöscht.
        assertTrue(geloeschteDateien.containsAll(gespielteDateien))
    }

    @Test
    fun stoppWaehrendAnsageBeendetSofortUndLoeschtDatei() = runBlocking {
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 2)))
        val inAnsage = CompletableDeferred<Unit>()
        val blockiert = CompletableDeferred<Unit>()
        val geloescht = CopyOnWriteArrayList<String>()
        val pausen = AtomicInteger(0)

        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMs = 5_000L,
                erzeuge = { File("/tmp/mental-ansage.wav") },
                spiele = {
                    inAnsage.complete(Unit)
                    blockiert.await()
                },
                loesche = { geloescht += it.name },
                pause = { pausen.incrementAndGet() },
            )
        }
        inAnsage.await()
        lauf.cancel()
        lauf.join()

        assertEquals(0, pausen.get())
        assertEquals(listOf("mental-ansage.wav"), geloescht)
    }

    @Test
    fun stoppWaehrendPauseBeendetSofortUndVerwirftVorabErzeugteDatei() = runBlocking {
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 2), MentalSatz(2L, "B", 1)))
        val inPause = CompletableDeferred<Unit>()
        val blockiert = CompletableDeferred<Unit>()
        val gespielte = CopyOnWriteArrayList<String>()
        val geloeschte = CopyOnWriteArrayList<String>()
        val nr = AtomicInteger(0)

        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMs = 5_000L,
                erzeuge = { File("/tmp/mental-pause-${nr.incrementAndGet()}.wav") },
                spiele = { gespielte += it.name },
                loesche = { geloeschte += it.name },
                pause = {
                    inPause.complete(Unit)
                    blockiert.await()
                },
            )
        }
        inPause.await()
        lauf.cancel()
        lauf.join()

        // Nur der erste Satz lief; nach dem Stopp in der Pause lief kein zweiter mehr, und alle erzeugten Dateien wurden gelöscht.
        assertEquals(listOf("mental-pause-1.wav"), gespielte)
        assertTrue(geloeschte.contains("mental-pause-1.wav"))
    }
}
