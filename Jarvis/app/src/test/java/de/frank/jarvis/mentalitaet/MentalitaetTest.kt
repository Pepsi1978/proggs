package de.frank.jarvis.mentalitaet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun schleifeBereitetWaehrendDesAbspielensVorUndSpieltAlleEchtenDateienAABBBADurch() = runBlocking {
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 2), MentalSatz(2L, "B", 3)))
        val erzeugteTexte = CopyOnWriteArrayList<String>()
        val erzeugteDateien = CopyOnWriteArrayList<File>()
        val gespielteDateien = CopyOnWriteArrayList<File>()
        val pausenMs = CopyOnWriteArrayList<Long>()
        val zaehler = AtomicInteger(0)
        val zweiterSchonWaehrendErstemVorbereitet = CompletableDeferred<Unit>()
        val fertig = CompletableDeferred<Unit>()

        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMs = 5_000L,
                erzeuge = { text ->
                    val nr = zaehler.incrementAndGet()
                    erzeugteTexte += text
                    File.createTempFile("mental-test-$nr-", ".wav").apply {
                        writeText("ton-$nr-$text")
                        erzeugteDateien += this
                        if (nr == 2) zweiterSchonWaehrendErstemVorbereitet.complete(Unit)
                    }
                },
                spiele = { datei ->
                    // KRITISCH: Jede echte Datei muss beim Abspielen noch vorhanden sein (nie vorab gelöscht!).
                    assertTrue("Datei ${datei.name} wurde vor dem Abspielen gelöscht!", datei.exists())
                    if (gespielteDateien.isEmpty()) {
                        // Belegt, dass Ausgabe 2 bereits im Hintergrund erzeugt wird, während Ausgabe 1 noch läuft.
                        zweiterSchonWaehrendErstemVorbereitet.await()
                    }
                    gespielteDateien += datei
                    if (gespielteDateien.size == 6) {
                        fertig.complete(Unit)
                        throw CancellationException("Abnahme-Grenze erreicht")
                    }
                },
                loesche = { datei -> datei.delete() },
                pause = { ms ->
                    pausenMs += ms
                    // Echte Wartezeit, damit die im Hintergrund vorbereitete Datei die Pause überstehen muss.
                    delay(30)
                },
            )
        }
        fertig.await()
        lauf.join()

        // Genau A, A, B, B, B, A — 6 verschiedene, frisch erzeugte echte Dateien.
        assertEquals(listOf("A", "A", "B", "B", "B", "A"), erzeugteTexte.take(6))
        assertEquals(6, gespielteDateien.size)
        assertEquals(6, gespielteDateien.map { it.name }.toSet().size)
        // Genau 5 Pausen à 5000 ms zwischen den 6 Ausgaben.
        assertEquals(listOf(5_000L, 5_000L, 5_000L, 5_000L, 5_000L), pausenMs)
        // Alle erzeugten Dateien (inkl. einer schon vorab vorbereiteten 7. Datei) sind nach Ende gelöscht.
        erzeugteDateien.forEach { assertFalse("Temp-Datei ${it.name} blieb liegen", it.exists()) }
    }

    @Test
    fun stoppWaehrendAnsageBeendetSofortUndLoeschtAlleDateien() = runBlocking {
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 3)))
        val inAnsage = CompletableDeferred<Unit>()
        val zweiterBereit = CompletableDeferred<Unit>()
        val blockiert = CompletableDeferred<Unit>()
        val alleDateien = CopyOnWriteArrayList<File>()
        val pausen = AtomicInteger(0)
        val nr = AtomicInteger(0)

        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMs = 5_000L,
                erzeuge = {
                    val i = nr.incrementAndGet()
                    File.createTempFile("mental-ansage-$i-", ".wav").apply {
                        writeText("ton-$i")
                        alleDateien += this
                        if (i == 2) zweiterBereit.complete(Unit)
                    }
                },
                spiele = { datei ->
                    assertTrue(datei.exists())
                    inAnsage.complete(Unit)
                    blockiert.await()
                },
                loesche = { it.delete() },
                pause = { pausen.incrementAndGet() },
            )
        }
        inAnsage.await()
        zweiterBereit.await()
        lauf.cancel()
        lauf.join()

        assertEquals(0, pausen.get())
        assertTrue(alleDateien.size >= 2)
        alleDateien.forEach { assertFalse("Datei ${it.name} nicht aufgeräumt", it.exists()) }
    }

    @Test
    fun stoppWaehrendPauseBeendetSofortUndLoeschtVorabErzeugteDatei() = runBlocking {
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 2), MentalSatz(2L, "B", 1)))
        val inPause = CompletableDeferred<Unit>()
        val blockiert = CompletableDeferred<Unit>()
        val gespielte = CopyOnWriteArrayList<File>()
        val alleDateien = CopyOnWriteArrayList<File>()
        val nr = AtomicInteger(0)

        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMs = 5_000L,
                erzeuge = {
                    val i = nr.incrementAndGet()
                    File.createTempFile("mental-pause-$i-", ".wav").apply {
                        writeText("ton-$i")
                        alleDateien += this
                    }
                },
                spiele = { datei ->
                    assertTrue(datei.exists())
                    gespielte += datei
                },
                loesche = { it.delete() },
                pause = {
                    delay(20)
                    inPause.complete(Unit)
                    blockiert.await()
                },
            )
        }
        inPause.await()
        lauf.cancel()
        lauf.join()

        assertEquals(1, gespielte.size)
        alleDateien.forEach { assertFalse("Datei ${it.name} nach Pausen-Stopp nicht gelöscht", it.exists()) }
    }

    @Test
    fun pauseSekundenParserAkzeptiertGueltigeWerteUndLehntUngueltigeAb() {
        assertEquals(3, parseMentalitaetPauseSekunden("3"))
        assertEquals(5, parseMentalitaetPauseSekunden("standard"))
        assertEquals(5, parseMentalitaetPauseSekunden("zurücksetzen"))
        assertEquals(3, parseMentalitaetPauseSekunden("minimal"))
        assertEquals(30, parseMentalitaetPauseSekunden("maximal"))
        assertEquals(12, parseMentalitaetPauseSekunden("12 s"))
        assertEquals(18, parseMentalitaetPauseSekunden("18 Sekunden"))
        assertEquals(30, parseMentalitaetPauseSekunden("30 sek."))

        for (ungueltig in listOf("", "2", "31", "-5", "5,5", "3 Minuten", "schnell")) {
            val geworfen = runCatching { parseMentalitaetPauseSekunden(ungueltig) }.isFailure
            assertTrue("Hätte '$ungueltig' ablehnen müssen", geworfen)
        }
    }

    @Test
    fun schleifeNutztAktuellEingestelltePauseFuerJedeRunde() = runBlocking {
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 3)))
        val pausen = CopyOnWriteArrayList<Long>()
        val alleDateien = CopyOnWriteArrayList<File>()
        val aktuell = AtomicInteger(8)
        val fertig = CompletableDeferred<Unit>()
        val zaehler = AtomicInteger(0)

        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMsAbrufen = { aktuell.get() * 1_000L },
                erzeuge = {
                    File.createTempFile("mental-dyn-${zaehler.incrementAndGet()}-", ".wav").apply {
                        writeText("ton")
                        alleDateien += this
                    }
                },
                spiele = { datei ->
                    assertTrue(datei.exists())
                },
                loesche = { it.delete() },
                pause = { ms ->
                    pausen += ms
                    if (pausen.size == 1) {
                        aktuell.set(15)
                    } else if (pausen.size == 2) {
                        fertig.complete(Unit)
                        throw CancellationException("Fertig")
                    }
                },
            )
        }
        fertig.await()
        lauf.join()

        assertEquals(listOf(8_000L, 15_000L), pausen)
        alleDateien.forEach { assertFalse("Datei ${it.name} blieb liegen", it.exists()) }
    }
}
