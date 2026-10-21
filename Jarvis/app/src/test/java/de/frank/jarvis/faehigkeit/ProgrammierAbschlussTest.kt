package de.frank.jarvis.faehigkeit

import de.frank.jarvis.faehigkeit.ProgrammierAbschluss.Stand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammierAbschlussTest {
    /** Nachbau des Berichts von Auftrag 8J2YK (4.404 Zeichen), dessen letzter Vorschlag bei 4.000 Zeichen „… neu verbindet o“ endete. */
    private val langerBericht = "🏁 " + "Ein spätes Ergebnis auf eine gesprochene Frage geht nicht mehr verloren. ".repeat(52) +
        "\n\n**Verbesserungsvorschläge**\n1. Die Ruhezeit-Logik vereinheitlichen.\n" +
        "2. Ein Schalter in den Einstellungen, ob Live nach einem Ergebnis automatisch neu verbindet oder lieber im Hey-Jarvis-Modus weitermacht.\n" +
        "3. Die Zustellung an einer Stelle zusammenführen.\n\n- Committet: ja (db2e7c75d)\n- Gepusht: ja\n" +
        "- Installiert: ja, Fold 8 per WLAN 192.168.0.155\n- APK-Update: ja, versionCode 210\n\n⏱️ Dauer seit Start: 10 min 26 s"

    @Test fun langerAbschlussberichtKommtVollstaendigMitLetztemSatz() {
        assertTrue(langerBericht.length > 4_000)
        val ankunft = ProgrammierAbschluss.kuerzen(langerBericht, ProgrammierAbschluss.MAX_BERICHT)
        assertEquals(langerBericht, ankunft)
        assertTrue(ankunft.contains("automatisch neu verbindet oder lieber im Hey-Jarvis-Modus weitermacht."))
        assertTrue(ankunft.endsWith("⏱️ Dauer seit Start: 10 min 26 s"))
    }

    @Test fun gekuerztWirdNurMarkiertAmSatzOderZeilenende() {
        val kurz = ProgrammierAbschluss.kuerzen(langerBericht, ProgrammierAbschluss.MAX_MELDUNG)
        assertTrue(kurz.length <= ProgrammierAbschluss.MAX_MELDUNG)
        assertTrue(kurz.endsWith(ProgrammierAbschluss.GEKUERZT))
        val vorDemHinweis = kurz.removeSuffix(ProgrammierAbschluss.GEKUERZT).trimEnd()
        // Kein Satzfragment wie „… automatisch neu verbindet o“: Schluss an Satz- oder Zeilenende.
        assertTrue(vorDemHinweis.last() in ".!?:" || langerBericht.contains(vorDemHinweis + "\n"))
        assertFalse(vorDemHinweis.endsWith(" o"))
    }

    @Test fun kurzeMeldungUnveraendertUndEmojiNichtZerteilt() {
        assertEquals("Fertig.", ProgrammierAbschluss.kuerzen("Fertig.", 400))
        val emojis = "🏁".repeat(300)
        val k = ProgrammierAbschluss.kuerzen(emojis, 100, "…")
        assertFalse(Character.isHighSurrogate(k.removeSuffix("\n…").last()))
    }

    @Test fun faktenAusHookMeldungenUndBericht() {
        val f = ProgrammierAbschluss.fakten(listOf("✏️ Geändert: A.kt · 🔨 Bau erfolgreich", "📦 Committet", "⬆️ Gepusht",
            "📲 Auf dem Handy installiert (WLAN)", "☁️ APK-Update für UpdateStation bereit"))
        assertEquals(ProgrammierAbschluss.Fakten(Stand.JA, Stand.JA, Stand.JA, "WLAN", Stand.JA), f)
        assertEquals(ProgrammierAbschluss.Fakten(Stand.JA, Stand.JA, Stand.JA, "WLAN", Stand.JA), ProgrammierAbschluss.fakten(listOf(langerBericht)))
    }

    @Test fun fehlgeschlagenAusstehendUndUnbekanntGetrennt() {
        val f = ProgrammierAbschluss.fakten(listOf("📦 Committet · 🔴 Push fehlgeschlagen", "Installiert: nein (kein Gerät)"))
        assertEquals(Stand.JA, f.commit)
        assertEquals(Stand.FEHLGESCHLAGEN, f.push)
        assertEquals(Stand.NEIN, f.installiert)
        assertNull(f.weg)
        assertEquals(Stand.UNBEKANNT, f.apk)
        val text = ProgrammierAbschluss.vorlesetext("Jarvis 1.8.200 kann jetzt X.", f, cloud = false, fertig = true)
        assertTrue(text.contains("Der Push ist fehlgeschlagen."))
        assertTrue(text.contains("Auf dem Handy ist es noch nicht installiert."))
        assertTrue(text.contains("Ob eine APK bei UpdateStation bereitliegt, ist nicht bestätigt."))
    }

    @Test fun spaeterErfolgHebtFrueherenFehlerAuf() {
        val f = ProgrammierAbschluss.fakten(listOf("🔴 Installation fehlgeschlagen", "📲 Auf dem Handy installiert (Tunnel)"))
        assertEquals(Stand.JA, f.installiert)
        assertEquals("Tunnel", f.weg)
    }

    @Test fun fertigIstKeineInstallation() {
        // Nur „fertig“ ohne Belege: nichts wird als erledigt angesagt.
        val f = ProgrammierAbschluss.fakten(listOf("Gestartet: PC.", "Jarvis 1.8.200 ist fertig."))
        assertEquals(ProgrammierAbschluss.Fakten(Stand.UNBEKANNT, Stand.UNBEKANNT, Stand.UNBEKANNT, null, Stand.UNBEKANNT), f)
        val text = ProgrammierAbschluss.vorlesetext("Jarvis 1.8.200 ist fertig.", f, cloud = false, fertig = true)
        assertTrue(text.contains("Ob es auf dem Handy installiert ist, ist nicht bestätigt."))
        assertFalse(text.contains("ist installiert"))
    }

    @Test fun kurzeAnsageOhneTechnischenBerichtUndStatuszeilen() {
        val meldung = "**Jarvis 1.8.200:** Programmieraufträge aus dem Sprachgespräch werden nach dem Abschluss kurz vorgelesen. " +
            "Committet: ja · Gepusht: ja · Installiert: ja (WLAN) · APK-Update: ja"
        val f = ProgrammierAbschluss.fakten(listOf(meldung))
        val text = ProgrammierAbschluss.vorlesetext(meldung, f, cloud = false, fertig = true)
        assertTrue(text.startsWith("Jarvis 1.8.200: Programmieraufträge aus dem Sprachgespräch werden nach dem Abschluss kurz vorgelesen."))
        assertFalse(text.contains("**"))
        assertFalse(text.contains("Committet: ja"))
        assertTrue(text.contains("Committet und gepusht ist es. Auf deinem Handy ist es installiert, über WLAN. Die APK liegt bei UpdateStation bereit."))
        assertTrue(text.endsWith("Den vollständigen Bericht findest du im Chat der Programmier-Session."))
        // Der Kern bleibt kurz, auch bei einem langen Bericht, und endet nie mitten im Satz.
        val lang = ProgrammierAbschluss.kern(langerBericht)
        assertTrue(lang.length <= 700)
        assertTrue(lang.endsWith("Mehr steht im Bericht."))
    }

    @Test fun cloudInstalliertNieSelbst() {
        val f = ProgrammierAbschluss.fakten(listOf("PR gemergt. Committet: ja · Gepusht: ja · APK-Update: ja"))
        val text = ProgrammierAbschluss.vorlesetext("Fertig.", f, cloud = true, fertig = true)
        assertTrue(text.contains("Direkt installiert ist es nicht, aufs Handy kommt es über UpdateStation."))
    }
}
