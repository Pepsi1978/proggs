package de.frank.jarvis.live

import de.frank.jarvis.live.JarvisWeiche.Aktion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Abnahmefälle für „Jarvis direkt“ mit Gemini Live: jeder Beitrag genau einmal an Jarvis, gesprochen wird nur Jarvis. */
class JarvisWeicheTest {
    private val ruhe = 2_000L
    private val w = JarvisWeiche(ruhe)
    private val alle = mutableListOf<Aktion>()
    private fun mit(a: List<Aktion>) = a.also { alle += it }
    private fun anJarvis() = alle.filterIsInstance<Aktion.AnJarvis>()
    private fun takt(von: Long, bis: Long) { var t = von; while (t <= bis) { mit(w.takt(t)); t += 100 } }

    /** Begrüßung abschließen, damit die Tonsperre im Normalzustand ist. */
    private fun begruesst() = mit(w.zugEnde(0))

    @Test fun belangloseAussageOhneReaktionVonGeminiGehtTrotzdemAnJarvis() {
        begruesst()
        mit(w.nutzerText("Schönes Wetter heute.", 1_000))
        takt(1_000, 2_900)
        assertTrue("vor Ablauf der Ruhezeit noch nicht", anJarvis().isEmpty())
        takt(3_000, 3_100)
        assertEquals(1, anJarvis().size)
        assertEquals("Schönes Wetter heute.", anJarvis()[0].auftrag)
        assertTrue(alle.indexOf(Aktion.NutzerFertig) < alle.indexOf(anJarvis()[0]))
        val e = mit(w.jarvisAntwort(anJarvis()[0].kennung, "Ja, 22 Grad und sonnig.", 5_000)).filterIsInstance<Aktion.Einspielen>()
        assertEquals(JarvisWeiche.MARKE + "Ja, 22 Grad und sonnig.", e.single().text)
        assertTrue(w.tonFrei)
    }

    @Test fun werkzeugaufrufBekommtFranksWorteNichtGeminisFassung() {
        begruesst()
        mit(w.nutzerText("Wann hab ich morgen eigentlich Dienst?", 1_000))
        mit(w.werkzeugAufruf("c1", "Dienstplan für morgen abfragen", 1_100))
        assertTrue("Mitschrift darf noch nachkommen", anJarvis().isEmpty())
        takt(1_100, 1_400)
        assertEquals("Wann hab ich morgen eigentlich Dienst?", anJarvis().single().auftrag)
        val r = mit(w.jarvisAntwort(anJarvis()[0].kennung, "Morgen hast du Tagdienst ab 6 Uhr.", 4_000)).filterIsInstance<Aktion.Werkzeugantwort>()
        assertEquals(listOf("c1"), r.single().ids)
        assertEquals("Morgen hast du Tagdienst ab 6 Uhr.", r.single().text)
        assertTrue(w.tonFrei)
    }

    @Test fun eigeneSmalltalkAntwortVonGeminiBleibtStumm() {
        begruesst()
        mit(w.nutzerText("Wie geht's dir?", 1_000))
        mit(w.modellAusgabe(1_300))
        assertFalse("Geminis eigene Antwort ist nicht hörbar", w.tonFrei)
        takt(1_300, 1_400)
        assertEquals("Wie geht's dir?", anJarvis().single().auftrag)
        mit(w.zugEnde(2_000))
        val e = mit(w.jarvisAntwort(anJarvis()[0].kennung, "Mir geht's gut, danke.", 3_000)).filterIsInstance<Aktion.Einspielen>()
        assertEquals(1, e.size)
        assertTrue(w.tonFrei)
    }

    @Test fun eingespielteAntwortWartetBisGeminisStummeAntwortAbgebrochenIst() {
        begruesst()
        mit(w.nutzerText("Erzähl mir was.", 1_000))
        mit(w.modellAusgabe(1_200))
        takt(1_300, 1_400)
        mit(w.modellAusgabe(1_500))
        mit(w.jarvisAntwort(anJarvis().single().kennung, "Gern: …", 1_600))
        assertFalse("Rest der alten Antwort bleibt stumm", w.tonFrei)
        mit(w.unterbrochen(1_700))
        assertTrue(w.tonFrei)
    }

    @Test fun abgeloesteAntwortOhneAbbruchmeldungGibtTonSpaetestensNachWartezeitFrei() {
        begruesst()
        mit(w.nutzerText("Hallo.", 1_000))
        mit(w.modellAusgabe(1_200))
        takt(1_300, 1_400)
        mit(w.modellAusgabe(1_500))
        mit(w.jarvisAntwort(anJarvis().single().kennung, "Hallo Frank.", 1_600))
        assertFalse(w.tonFrei)
        takt(1_700, 1_600 + JarvisWeiche.WARTEN_MAX_MS)
        assertTrue(w.tonFrei)
    }

    @Test fun erneuterAufrufNachEinspielenFragtJarvisNichtNochEinmal() {
        begruesst()
        mit(w.nutzerText("Leg eine Aufgabe an: Milch kaufen.", 1_000))
        takt(1_000, 3_100)
        mit(w.jarvisAntwort(anJarvis().single().kennung, "Aufgabe „Milch kaufen“ angelegt.", 4_000))
        val r1 = mit(w.werkzeugAufruf("c2", "Aufgabe Milch kaufen angelegt", 4_200)).filterIsInstance<Aktion.Werkzeugantwort>()
        assertEquals("Aufgabe „Milch kaufen“ angelegt.", r1.single().text)
        mit(w.werkzeugAufruf("c3", "Aufgabe Milch kaufen angelegt", 4_400))
        val dritte = mit(w.werkzeugAufruf("c4", "Aufgabe Milch kaufen angelegt", 4_600))
        assertTrue(dritte.any { it is Aktion.Anzeigen })
        assertTrue(dritte.any { it is Aktion.Fehler })
        takt(4_600, 20_000)
        assertEquals("keine doppelte Aktion", 1, anJarvis().size)
        assertFalse(w.tonFrei)
    }

    @Test fun vieleSignaleDesselbenZugsGebenNurEinmalAb() {
        begruesst()
        mit(w.nutzerText("Wie spät ist es?", 1_000))
        mit(w.modellAusgabe(1_100))
        mit(w.werkzeugAufruf("c1", "Uhrzeit", 1_200))
        mit(w.zugEnde(1_250))
        takt(1_300, 10_000)
        mit(w.werkzeugAufruf("c2", "Uhrzeit", 10_100))
        takt(10_100, 12_000)
        assertEquals(1, anJarvis().size)
        val r = mit(w.jarvisAntwort(anJarvis()[0].kennung, "Es ist 14:05 Uhr.", 13_000)).filterIsInstance<Aktion.Werkzeugantwort>()
        assertEquals(listOf("c1", "c2"), r.single().ids)
    }

    @Test fun weitersprechenFaltetUngehoerteAntwortInDenNaechstenBeitrag() {
        begruesst()
        mit(w.nutzerText("Wie wird das Wetter in Berlin?", 1_000))
        takt(1_000, 3_100)
        val a = anJarvis().single()
        mit(w.nutzerText("Ich meinte Hamburg.", 3_500))
        takt(3_500, 5_600)
        mit(w.nutzerText("Und morgen.", 5_700))
        takt(5_700, 7_800)
        assertEquals("während Jarvis arbeitet, keine zweite Anfrage", 1, anJarvis().size)
        val nachA = mit(w.jarvisAntwort(a.kennung, "In Berlin 18 Grad.", 8_000))
        assertTrue("überholte Antwort wird nicht gesprochen", nachA.none { it is Aktion.Einspielen || it is Aktion.Werkzeugantwort })
        val b = anJarvis()[1]
        assertEquals("Ich meinte Hamburg.\nUnd morgen.", b.auftrag)
        assertTrue(b.hinweis.contains("In Berlin 18 Grad."))
        val e = mit(w.jarvisAntwort(b.kennung, "Morgen in Hamburg 15 Grad.", 9_000)).filterIsInstance<Aktion.Einspielen>()
        assertEquals(JarvisWeiche.MARKE + "Morgen in Hamburg 15 Grad.", e.single().text)
    }

    @Test fun weitersprechenVorDemErstenTonHaeltAbgegebeneAntwortZurueck() {
        begruesst()
        mit(w.nutzerText("Wie wird das Wetter?", 1_000))
        mit(w.werkzeugAufruf("c1", "Wetter", 1_100))
        takt(1_100, 1_400)
        mit(w.jarvisAntwort(anJarvis()[0].kennung, "Morgen Regen.", 3_000))
        assertTrue(w.tonFrei)
        mit(w.nutzerText("Ach, und übermorgen?", 3_200))
        assertFalse("überholte Antwort wird nicht mehr hörbar", w.tonFrei)
        mit(w.modellAusgabe(3_300))
        assertFalse(w.tonFrei)
        takt(3_300, 5_600)
        assertEquals("Ach, und übermorgen?", anJarvis()[1].auftrag)
        assertTrue(anJarvis()[1].hinweis.contains("Morgen Regen."))
    }

    @Test fun unterbrechenSchliesstTonUndWirdJarvisGesagt() {
        begruesst()
        mit(w.nutzerText("Erzähl mir von meinem Schlaf.", 1_000))
        takt(1_000, 3_100)
        mit(w.jarvisAntwort(anJarvis()[0].kennung, "Du hast sechs Stunden geschlafen …", 4_000))
        mit(w.modellAusgabe(4_300))
        mit(w.unterbrochen(5_000))
        assertFalse(w.tonFrei)
        mit(w.nutzerText("Stopp, nur die Erholung.", 5_200))
        takt(5_200, 7_300)
        assertTrue(anJarvis()[1].hinweis.contains("unterbrochen"))
        assertEquals("Stopp, nur die Erholung.", anJarvis()[1].auftrag)
    }

    @Test fun verspaeteteAntwortWirdVerworfen() {
        begruesst()
        val a = mit(w.jarvisAntwort("weiche-99", "alt", 1_000))
        assertTrue(a.all { it is Aktion.Protokoll })
    }

    @Test fun ausfallVonJarvisWirdEhrlichGemeldet() {
        begruesst()
        mit(w.nutzerText("Was steht heute an?", 1_000))
        takt(1_000, 3_100)
        val start = 3_100L
        takt(start, start + JarvisWeiche.JARVIS_MAX_MS + 100)
        val e = alle.filterIsInstance<Aktion.Einspielen>().single()
        assertTrue(e.text.contains("nicht rechtzeitig geantwortet"))
        // Kommt die echte Antwort danach noch, wird sie nicht zusätzlich gesprochen.
        assertTrue(mit(w.jarvisAntwort(anJarvis()[0].kennung, "spät", start + JarvisWeiche.JARVIS_MAX_MS + 200)).all { it is Aktion.Protokoll })
    }

    @Test fun schweigtGeminiZurAntwortStehtSieImChat() {
        begruesst()
        mit(w.nutzerText("Danke.", 1_000))
        takt(1_000, 3_100)
        mit(w.jarvisAntwort(anJarvis()[0].kennung, "Gern geschehen.", 4_000))
        takt(4_000, 4_000 + JarvisWeiche.STUMM_MAX_MS + 100)
        assertEquals("Gern geschehen.", alle.filterIsInstance<Aktion.Anzeigen>().single().text)
        assertFalse(w.tonFrei)
    }

    @Test fun mitschriftOhneTonGiltAlsNichtGesprochen() {
        begruesst()
        mit(w.nutzerText("Wie war mein Schlaf?", 1_000))
        takt(1_000, 3_100)
        mit(w.jarvisAntwort(anJarvis()[0].kennung, "Sieben Stunden, Erholung 60 Prozent.", 4_000))
        mit(w.modellAusgabe(4_500, ton = false))
        takt(4_500, 4_000 + JarvisWeiche.STUMM_MAX_MS + 100)
        assertEquals("Sieben Stunden, Erholung 60 Prozent.", alle.filterIsInstance<Aktion.Anzeigen>().single().text)
    }

    @Test fun mitschriftNachDemAufrufWirdAbgewartet() {
        begruesst()
        mit(w.werkzeugAufruf("c1", "Termine heute", 1_000))
        mit(w.nutzerText("Was hab ich heute für Termine?", 1_500))
        takt(1_500, 1_800)
        assertTrue(anJarvis().isEmpty())
        takt(1_900, 2_000)
        assertEquals("Was hab ich heute für Termine?", anJarvis().single().auftrag)
        val r = mit(w.jarvisAntwort(anJarvis()[0].kennung, "Um 15 Uhr Zahnarzt.", 3_000)).filterIsInstance<Aktion.Werkzeugantwort>()
        assertEquals(listOf("c1"), r.single().ids)
    }

    @Test fun ohneMitschriftNimmtSieGeminisFassungGekennzeichnet() {
        begruesst()
        mit(w.werkzeugAufruf("c1", "Termine heute", 1_000))
        takt(1_000, 2_900)
        val a = anJarvis().single()
        assertTrue(a.auftrag.startsWith(JarvisWeiche.MITSCHRIFT_FEHLT))
        assertTrue(a.auftrag.endsWith("Termine heute"))
    }

    @Test fun aufrufInDerBegruessungOhneWorteFragtJarvisNicht() {
        assertTrue(w.tonFrei)
        mit(w.werkzeugAufruf("c1", "Begrüße Frank", 100))
        takt(100, 1_700)
        assertTrue(anJarvis().isEmpty())
        assertEquals(listOf("c1"), alle.filterIsInstance<Aktion.Werkzeugantwort>().single().ids)
    }

    @Test fun vorlaeufigeMitschriftVerlaengertDieRuhezeit() {
        begruesst()
        mit(w.nutzerText("Erstens", 1_000))
        for (t in listOf(1_800L, 2_600L, 3_400L)) w.nutzerSpricht(t)
        takt(1_000, 5_300)
        assertTrue("Frank spricht laut vorläufiger Mitschrift noch", anJarvis().isEmpty())
        takt(5_400, 5_500)
        assertEquals(1, anJarvis().size)
    }

    @Test fun abgebrochenerAufrufLaesstAntwortEinspielen() {
        begruesst()
        mit(w.nutzerText("Stell einen Timer auf fünf Minuten.", 1_000))
        mit(w.werkzeugAufruf("c1", "Timer 5 Minuten", 1_100))
        takt(1_100, 1_400)
        w.werkzeugAbgebrochen(listOf("c1"))
        val a = mit(w.jarvisAntwort(anJarvis().single().kennung, "Timer läuft, fünf Minuten.", 3_000))
        assertTrue(a.none { it is Aktion.Werkzeugantwort })
        assertEquals(1, a.filterIsInstance<Aktion.Einspielen>().size)
    }

    // ---- Neutrale Signale: Gemini darf überbrücken, aber nie selbst antworten ----

    private fun signale() = alle.filterIsInstance<Aktion.Signal>()
    private val ton = ByteArray(9_600)

    /** Gemini sagt bei gesperrtem Ton etwas: Ton und Mitschrift wie aus [LiveGespraech]. */
    private fun geminiSagt(text: String, jetzt: Long) {
        mit(w.modellAusgabe(jetzt))
        w.eigenerTon(ton, jetzt)
        w.eigeneMitschrift(text, jetzt + 50)
    }

    @Test fun neutraleSignaleErkennen() {
        for (t in listOf("Mhm.", "Okay, einen kleinen Moment.", "Alles klar.", "Moment.", "Ich schau mal kurz nach.", "Lass mich kurz nachsehen."))
            assertTrue(t, JarvisWeiche.neutralesSignal(t))
        for (t in listOf("", "Klar.", "Ja.", "Nein.", "Genau.", "Gut.", "Erledigt.", "Okay, der Wecker ist gestellt.", "Mir geht's gut, danke!",
                "Alles klar, ist gemacht.", "Moment, morgen sind es 22 Grad.", "42", "Okay, 14:30", "Mhm, 5 %", "Alles", "…"))
            assertFalse(t, JarvisWeiche.neutralesSignal(t))
        for (s in JarvisWeiche.SIGNALE) assertTrue("Anweisung nennt nur erlaubte Signale: $s", JarvisWeiche.neutralesSignal(s))
    }

    @Test fun smalltalkNeutralesSignalWirdHoerbarUndBeitragGehtUnveraendertAnJarvis() {
        begruesst()
        mit(w.nutzerText("Na, wie läuft's bei dir?", 1_000))
        geminiSagt("Mhm.", 1_300)
        mit(w.zugEnde(1_600))
        takt(1_600, 2_100)
        assertEquals("Na, wie läuft's bei dir?", anJarvis().single().auftrag)
        assertEquals("Mhm.", signale().single().text)
        assertEquals(1, signale().single().ton.size)
        assertFalse("Signal öffnet den Ton nicht für Gemini", w.tonFrei)
        mit(w.jarvisAntwort(anJarvis()[0].kennung, "Läuft gut, danke. Und bei dir?", 4_000))
        assertTrue(w.tonFrei)
    }

    @Test fun smalltalkSachantwortVonGeminiBleibtTrotzPufferStumm() {
        begruesst()
        mit(w.nutzerText("Wie geht's dir?", 1_000))
        geminiSagt("Mir geht's prima, danke der Nachfrage!", 1_300)
        mit(w.zugEnde(2_500))
        takt(2_500, 3_000)
        assertTrue(signale().isEmpty())
        assertTrue(alle.any { it is Aktion.Protokoll && it.text.contains("kein neutrales Signal") })
        assertEquals("Wie geht's dir?", anJarvis().single().auftrag)
    }

    @Test fun persoenlicheDatenMomentVorDemWerkzeugaufrufWirdHoerbarAntwortKommtVonJarvis() {
        begruesst()
        mit(w.nutzerText("Wie habe ich heute geschlafen?", 1_000))
        geminiSagt("Moment.", 1_200)
        mit(w.werkzeugAufruf("c1", "Schlafdaten von heute", 1_400))
        takt(1_400, 2_000)
        assertEquals("Wie habe ich heute geschlafen?", anJarvis().single().auftrag)
        assertEquals("Moment.", signale().single().text)
        val r = mit(w.jarvisAntwort(anJarvis()[0].kennung, "Rund 6,7 Stunden, Erholung 30 Prozent.", 6_000)).filterIsInstance<Aktion.Werkzeugantwort>()
        assertEquals("Rund 6,7 Stunden, Erholung 30 Prozent.", r.single().text)
        assertTrue(w.tonFrei)
    }

    @Test fun appAktionUnbestaetigteErledigungVonGeminiBleibtStumm() {
        begruesst()
        mit(w.nutzerText("Stell mir einen Wecker auf sieben Uhr.", 1_000))
        geminiSagt("Klar, dein Wecker ist gestellt.", 1_200)
        mit(w.werkzeugAufruf("c1", "Wecker 7 Uhr", 1_500))
        takt(1_500, 2_200)
        assertTrue("Erledigung nur von Jarvis", signale().isEmpty())
        assertEquals("Stell mir einen Wecker auf sieben Uhr.", anJarvis().single().auftrag)
    }

    @Test fun ohneMitschriftBleibtGepufferterTonStumm() {
        begruesst()
        mit(w.nutzerText("Was steht heute an?", 1_000))
        mit(w.modellAusgabe(1_200))
        w.eigenerTon(ton, 1_200)
        mit(w.werkzeugAufruf("c1", "Termine heute", 1_400))
        takt(1_400, 2_200)
        assertTrue(signale().isEmpty())
    }

    @Test fun zuLangeEigeneAeusserungBleibtStumm() {
        begruesst()
        mit(w.nutzerText("Erzähl mal.", 1_000))
        mit(w.modellAusgabe(1_200))
        repeat(20) { w.eigenerTon(ton, 1_200L + it * 10) }
        w.eigeneMitschrift("Okay.", 1_500)
        mit(w.zugEnde(1_600))
        takt(1_600, 2_200)
        assertTrue(signale().isEmpty())
    }

    @Test fun unterbrechungVerwirftGepuffertesSignal() {
        begruesst()
        mit(w.nutzerText("Sag mal …", 1_000))
        geminiSagt("Mhm.", 1_300)
        mit(w.unterbrochen(1_500))
        mit(w.zugEnde(1_600))
        takt(1_600, 2_500)
        assertTrue(signale().isEmpty())
    }

    @Test fun weitersprechenVerwirftSignalAberNichtDieNachlaufendeMitschrift() {
        begruesst()
        mit(w.nutzerText("Wie wird das Wetter", 1_000))
        geminiSagt("Moment.", 1_100)
        // Mitschrift von vorher läuft nach: gehört noch zum selben Beitrag, das Signal bleibt.
        mit(w.nutzerText(" morgen?", 1_300))
        mit(w.werkzeugAufruf("c1", "Wetter morgen", 1_400))
        takt(1_400, 2_000)
        assertEquals("Wie wird das Wetter morgen?", anJarvis().single().auftrag)
        assertEquals(1, signale().size)
        // Frank spricht neu, während Gemini eine eigene Äußerung beginnt: Sie ist überholt.
        mit(w.zugEnde(2_100))
        val vorher = signale().size
        mit(w.jarvisAntwort(anJarvis()[0].kennung, "Morgen 18 Grad, Regen am Nachmittag.", 3_000))
        mit(w.zugEnde(5_000))
        mit(w.nutzerText("Und übermorgen?", 6_000))
        geminiSagt("Okay.", 6_100)
        mit(w.nutzerText(" Am Wochenende auch?", 6_800))
        mit(w.zugEnde(6_900))
        takt(6_900, 9_500)
        assertEquals(vorher, signale().size)
        assertEquals("Und übermorgen? Am Wochenende auch?", anJarvis()[1].auftrag)
    }

    @Test fun verzoegerteAntwortHoechstensEinSignalJeAntwortUndKeineWiederholung() {
        begruesst()
        mit(w.nutzerText("Recherchier mal was zu Magnesium.", 1_000))
        geminiSagt("Okay, Moment.", 1_200)
        mit(w.werkzeugAufruf("c1", "Magnesium", 1_400))
        takt(1_400, 2_000)
        assertEquals(1, signale().size)
        // Jarvis braucht lange: keine Warteschleife, weitere eigene Äußerungen bleiben stumm.
        mit(w.zugEnde(8_000))
        geminiSagt("Mhm.", 12_000)
        mit(w.zugEnde(12_400))
        takt(12_400, 13_000)
        assertEquals(1, signale().size)
        mit(w.jarvisAntwort(anJarvis()[0].kennung, "Magnesium hilft beim Schlaf, die Studienlage ist gemischt.", 30_000))
        mit(w.modellAusgabe(30_200))
        mit(w.zugEnde(33_000))
        // Nächste Antwort: wieder ein Signal erlaubt, aber nicht dasselbe wie zuletzt.
        mit(w.nutzerText("Und Zink?", 35_000))
        geminiSagt("Okay, Moment.", 35_200)
        mit(w.werkzeugAufruf("c2", "Zink", 35_400))
        takt(35_400, 36_000)
        assertEquals(1, signale().size)
        assertTrue(alle.any { it is Aktion.Protokoll && it.text.contains("wiederholt") })
        mit(w.jarvisAntwort(anJarvis()[1].kennung, "Zink: eher kein Effekt.", 40_000))
        mit(w.modellAusgabe(40_200))
        mit(w.zugEnde(42_000))
        mit(w.nutzerText("Und Eisen?", 44_000))
        geminiSagt("Ich schau nach.", 44_200)
        mit(w.werkzeugAufruf("c3", "Eisen", 44_400))
        takt(44_400, 45_000)
        assertEquals(2, signale().size)
    }

    @Test fun antwortVorDerSignalEntscheidungGewinnt() {
        begruesst()
        mit(w.nutzerText("Danke dir.", 1_000))
        geminiSagt("Mhm.", 1_100)
        mit(w.werkzeugAufruf("c1", "Danke", 1_300))
        takt(1_300, 1_600)
        val r = mit(w.jarvisAntwort(anJarvis().single().kennung, "Gern.", 1_650))
        takt(1_700, 2_500)
        assertTrue("Antwort kam vor dem Ende des Nachlaufs", signale().isEmpty())
        assertEquals("Gern.", r.filterIsInstance<Aktion.Werkzeugantwort>().single().text)
    }

    @Test fun ueberholteAntwortWirdNachSignalNichtNachtraeglichGesprochen() {
        begruesst()
        mit(w.nutzerText("Was gibt's Neues?", 1_000))
        geminiSagt("Moment.", 1_200)
        mit(w.werkzeugAufruf("c1", "Neuigkeiten", 1_400))
        takt(1_400, 2_000)
        assertEquals(1, signale().size)
        mit(w.jarvisAntwort(anJarvis()[0].kennung, "Drei neue Mails.", 4_000))
        // Frank spricht weiter, bevor Gemini die Antwort hörbar spricht: Sie bleibt stumm und geht mit an Jarvis.
        mit(w.nutzerText("Ach, lass mal, wie spät ist es?", 4_200))
        assertFalse(w.tonFrei)
        takt(4_200, 7_000)
        assertTrue(anJarvis()[1].hinweis.contains("Drei neue Mails."))
    }

    @Test fun ohneBeitragKeinSignal() {
        begruesst()
        geminiSagt("Mhm.", 1_000)
        mit(w.zugEnde(1_300))
        takt(1_300, 2_000)
        assertTrue(signale().isEmpty())
        assertTrue(anJarvis().isEmpty())
    }
}
