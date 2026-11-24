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
    fun schleifeVerlaengertLaufendePauseLive() = runBlocking {
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

        // Verlängerung während der laufenden 8-s-Pause: die restlichen 7 s werden live nachgewartet.
        assertEquals(listOf(8_000L, 7_000L), pausen)
        alleDateien.forEach { assertFalse("Datei ${it.name} blieb liegen", it.exists()) }
    }

    /** Spielt [schritte] Ausgaben mit veränderlichem Plan; [nachAusgabe] darf Plan oder Pause live ändern. */
    private fun spieleLive(
        startPlan: List<MentalSatz>,
        schritte: Int,
        pauseMs: () -> Long = { 0L },
        pausenTakt: Long = Long.MAX_VALUE,
        pausen: MutableList<Long> = CopyOnWriteArrayList(),
        nachAusgabe: (Int, (List<MentalSatz>) -> Unit) -> Unit = { _, _ -> },
    ): List<String> = runBlocking {
        val saetze = java.util.concurrent.atomic.AtomicReference(startPlan)
        val gesprochen = CopyOnWriteArrayList<String>()
        val dateiText = java.util.concurrent.ConcurrentHashMap<File, String>()
        val fertig = CompletableDeferred<Unit>()
        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                planAbrufen = { mentalitaetPlan(saetze.get()) },
                pauseMsAbrufen = pauseMs,
                pausenTaktMs = pausenTakt,
                erzeuge = { text ->
                    File.createTempFile("mental-live-", ".wav").also { dateiText[it] = text }
                },
                spiele = { datei ->
                    gesprochen += dateiText.getValue(datei)
                    nachAusgabe(gesprochen.size) { saetze.set(it) }
                    if (gesprochen.size >= schritte) {
                        fertig.complete(Unit)
                        throw CancellationException("Fertig")
                    }
                },
                loesche = { it.delete() },
                pause = { ms -> pausen += ms; delay(1) },
            )
        }
        fertig.await()
        lauf.join()
        dateiText.keys.forEach { assertFalse("Datei ${it.name} blieb liegen", it.exists()) }
        gesprochen.toList()
    }

    @Test
    fun liveVerschiebenNachObenGiltAbNaechstemSatz() {
        val a = MentalSatz(1L, "A", 1); val b = MentalSatz(2L, "B", 1); val c = MentalSatz(3L, "C", 1)
        // Während A gesprochen wird, zieht Frank C nach oben: C, A, B.
        val folge = spieleLive(listOf(a, b, c), 5) { nr, setze -> if (nr == 1) setze(listOf(c, a, b)) }
        assertEquals(listOf("A", "B", "C", "A", "B"), folge)
    }

    @Test
    fun liveVerschiebenNachUntenGiltAbNaechstemSatz() {
        val a = MentalSatz(1L, "A", 1); val b = MentalSatz(2L, "B", 1); val c = MentalSatz(3L, "C", 1)
        // Während B gesprochen wird, zieht Frank B ganz nach unten: A, C, B → danach neuer Durchlauf.
        val folge = spieleLive(listOf(a, b, c), 5) { nr, setze -> if (nr == 2) setze(listOf(a, c, b)) }
        assertEquals(listOf("A", "B", "A", "C", "B"), folge)
    }

    @Test
    fun liveAnzahlErhoehenUndSenkenGreiftSofort() {
        val a = MentalSatz(1L, "A", 2); val b = MentalSatz(2L, "B", 1)
        // Nach A 1/2 auf 3 erhöht → A dreimal; im nächsten Durchlauf nach A 1/3 auf 1 gesenkt → direkt B.
        val folge = spieleLive(listOf(a, b), 7) { nr, setze ->
            if (nr == 1) setze(listOf(a.copy(wiederholungen = 3), b))
            if (nr == 5) setze(listOf(a.copy(wiederholungen = 1), b))
        }
        assertEquals(listOf("A", "A", "A", "B", "A", "B", "A"), folge)
    }

    @Test
    fun livePauseVerkuerzenUndVerlaengernAuchInLaufenderPause() {
        val pause = java.util.concurrent.atomic.AtomicLong(1_000L)
        val pausen = CopyOnWriteArrayList<Long>()
        // Takt 200 ms: nach dem 1. Takt der 2. Pause wird auf 400 ms verkürzt, in der 3. Pause auf 1 600 ms verlängert.
        spieleLive(
            listOf(MentalSatz(1L, "A", 1)), 4,
            pauseMs = {
                if (pausen.size == 6) pause.set(400L)
                if (pausen.size == 8) pause.set(1_600L)
                pause.get()
            },
            pausenTakt = 200L,
            pausen = pausen,
        )
        // 1. Pause 5×200 = 1 000; 2. Pause 200 + 200 (verkürzt auf 400); 3. Pause 8×200 = 1 600.
        assertEquals(1_000L, pausen.take(5).sum())
        assertEquals(400L, pausen.subList(5, 7).sum())
        assertEquals(1_600L, pausen.drop(7).sum())
    }

    @Test
    fun schleifeNutztNeuePauseInDerNaechstenRundeVoll() {
        val pause = java.util.concurrent.atomic.AtomicLong(8_000L)
        val pausen = CopyOnWriteArrayList<Long>()
        // Nach der ersten Pause, während der 2. Ausgabe, auf 15 s verlängert → 2. Pause wartet volle 15 s.
        spieleLive(listOf(MentalSatz(1L, "A", 1)), 3, pauseMs = { pause.get() }, pausen = pausen) { nr, _ ->
            if (nr == 2) pause.set(15_000L)
        }
        assertEquals(listOf(8_000L, 15_000L), pausen)
    }

    @Test
    fun naechsterSchrittBeiGeloeschtemSatz() {
        val alt = mentalitaetPlan(listOf(MentalSatz(1L, "A", 1), MentalSatz(2L, "B", 2), MentalSatz(3L, "C", 1)))
        val neu = mentalitaetPlan(listOf(MentalSatz(1L, "A", 1), MentalSatz(3L, "C", 1)))
        assertEquals(1 to false, mentalitaetNaechster(neu, alt, alt[1]))
        assertEquals(0 to true, mentalitaetNaechster(neu, alt, neu[1]))
    }

    @Test
    fun verzoegerteVorbereitungSpieltJedenSchrittVollstaendigUndInReihenfolge() = runBlocking {
        // Erzeugung dauert länger als die Pause: Die Pause verlängert sich, kein Schritt fällt aus.
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 2), MentalSatz(2L, "B", 1)))
        val gespielt = CopyOnWriteArrayList<String>()
        val fertig = CompletableDeferred<Unit>()
        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMs = 10L,
                pausenTaktMs = 5L,
                erzeuge = { text ->
                    delay(60)
                    File.createTempFile("mental-langsam-", ".wav").apply { writeText(text) }
                },
                spiele = { datei ->
                    gespielt += datei.readText()
                    if (gespielt.size >= 6) fertig.complete(Unit)
                },
                loesche = { it.delete() },
            )
        }
        fertig.await()
        lauf.cancel()
        lauf.join()
        assertEquals(listOf("A", "A", "B", "A", "A", "B"), gespielt.take(6))
    }

    @Test
    fun fehlgeschlageneErzeugungWiederholtDenselbenSchrittUndMeldetInternetprobleme() = runBlocking {
        val plan = mentalitaetPlan(listOf(MentalSatz(1L, "A", 2), MentalSatz(2L, "B", 1)))
        val gespielt = CopyOnWriteArrayList<String>()
        val internet = CopyOnWriteArrayList<Boolean>()
        val versuche = AtomicInteger(0)
        val fertig = CompletableDeferred<Unit>()
        val lauf = launch(Dispatchers.Default) {
            mentalitaetSchleife(
                plan = plan,
                pauseMs = 1L,
                wiederholungMs = { 1L },
                beiInternet = { internet += it },
                erzeuge = { text ->
                    // Die zweite Wiederholung von A scheitert zweimal am Netz.
                    val n = versuche.incrementAndGet()
                    if (n == 2 || n == 3) throw java.io.IOException("offline")
                    File.createTempFile("mental-netz-", ".wav").apply { writeText(text) }
                },
                spiele = { datei ->
                    gespielt += datei.readText()
                    if (gespielt.size >= 3) fertig.complete(Unit)
                },
                loesche = { it.delete() },
            )
        }
        fertig.await()
        lauf.cancel()
        lauf.join()
        assertEquals(listOf("A", "A", "B"), gespielt.take(3))
        assertTrue(internet.contains(true))
        assertEquals(false, internet.last())
    }
}
