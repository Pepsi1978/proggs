package de.frank.entropyreducer.domain.usecase

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.domain.usecase.WhoopAbrufPlan.Anlass
import de.frank.entropyreducer.domain.usecase.WhoopAbrufPlan.ErholungRoh
import de.frank.entropyreducer.domain.usecase.WhoopAbrufPlan.SchlafRoh
import de.frank.entropyreducer.domain.usecase.WhoopAbrufPlan.Stand
import org.junit.Test

/** Prüft die zentrale Whoop-Abrufsteuerung (Auftrag 559QH, 15.10.2026). */
class WhoopAbrufPlanTest {

    private val min = 60_000L
    private val h = 60L * min
    private val t0 = 1_800_000_000_000L // fester Bezugszeitpunkt
    private val heute = "2026-10-15"

    /** Schlaf endete um t0, ist fertig berechnet; zuletzt erfolgreich abgerufen kurz danach. */
    private fun fertig(abrufMs: Long = t0 + 30 * min) = Stand(
        letzterVersuchMs = abrufMs,
        letzterErfolgMs = abrufMs,
        schlafEndeMs = t0,
        schlafVollstaendig = true,
    )

    private fun entscheide(stand: Stand, anlass: Anlass, jetzt: Long, hinweis: Long = 0L, reauth: Boolean = false, tag: String = heute) =
        WhoopAbrufPlan.entscheide(stand, anlass, jetzt, tag, hinweis, reauth)

    // ---------------------------------------------------------------- Jarvis liest nur

    @Test
    fun `wiederholtes Jarvis-Lesen loest nie einen Whoop-Abruf aus`() {
        val staende = listOf(Stand(), fertig(), fertig().copy(schlafVollstaendig = false), fertig().copy(fehlversuche = 3))
        for (stand in staende) for (i in 0 until 50) {
            assertThat(entscheide(stand, Anlass.OHNE_WHOOP, t0 + i * h).abrufen).isFalse()
        }
    }

    // ---------------------------------------------------------------- parallele Auslöser

    @Test
    fun `nach einem Versuch blockieren sofort folgende Ausloeser`() {
        val start = Stand()
        assertThat(entscheide(start, Anlass.AUTOMATIK, t0).abrufen).isTrue()
        val nach = WhoopAbrufPlan.nachVersuch(start, t0)
        // Hintergrund, Biomarker-Reiter und Knopf direkt danach: keiner fragt Whoop erneut
        assertThat(entscheide(nach, Anlass.AUTOMATIK, t0 + 1_000).abrufen).isFalse()
        assertThat(entscheide(nach, Anlass.VORDERGRUND, t0 + 1_000).abrufen).isFalse()
        assertThat(entscheide(nach, Anlass.AUSDRUECKLICH, t0 + 1_000).abrufen).isFalse()
        assertThat(entscheide(nach, Anlass.AUSDRUECKLICH, t0 + 11 * min).abrufen).isTrue()
    }

    @Test
    fun `Tagesbudget begrenzt nur automatische Abrufe, neuer Tag setzt es zurueck`() {
        var voll = fertig().copy(schlafVollstaendig = false)
        repeat(WhoopAbrufPlan.TAGESBUDGET_AUTOMATIK) { voll = WhoopAbrufPlan.nachAutomatischemAbruf(voll, heute) }
        assertThat(entscheide(voll, Anlass.AUTOMATIK, t0 + 5 * h).abrufen).isFalse()
        assertThat(entscheide(voll, Anlass.VORDERGRUND, t0 + 5 * h).abrufen).isTrue()
        assertThat(entscheide(voll, Anlass.AUTOMATIK, t0 + 5 * h, tag = "2026-10-16").abrufen).isTrue()
        assertThat(entscheide(voll, Anlass.AUSDRUECKLICH, t0 + 5 * h).abrufen).isTrue()
    }

    // ---------------------------------------------------------------- vollständiger Schlaf

    @Test
    fun `vollstaendiger Schlaf - kein erneuter Abruf beim Oeffnen oder im Hintergrund`() {
        val stand = fertig()
        for (jetzt in listOf(t0 + 1 * h, t0 + 2 * h, t0 + 5 * h, t0 + 7 * h + 50 * min)) {
            assertThat(entscheide(stand, Anlass.AUTOMATIK, jetzt).abrufen).isFalse()
        }
        // Frank öffnet die App: nur die Tageswerte nach 3 h, nicht ständig
        assertThat(entscheide(stand, Anlass.VORDERGRUND, t0 + 2 * h).abrufen).isFalse()
        assertThat(entscheide(stand, Anlass.VORDERGRUND, t0 + 4 * h).abrufen).isTrue()
    }

    @Test
    fun `nach 8 Stunden langsam nach neuem Schlaf sehen, mit Oura-Hinweis schneller`() {
        val stand = fertig(abrufMs = t0 + 7 * h) // letzter Abruf 7 h nach Schlafende
        assertThat(entscheide(stand, Anlass.AUTOMATIK, t0 + 9 * h).abrufen).isFalse() // erst 2 h seit Versuch
        assertThat(entscheide(stand, Anlass.AUTOMATIK, t0 + 11 * h).abrufen).isTrue() // 4 h Takt
        val hinweis = t0 + 8 * h + 30 * min // Oura kennt einen neueren Schlaf
        assertThat(entscheide(stand, Anlass.AUTOMATIK, t0 + 8 * h + 45 * min, hinweis).abrufen).isTrue()
    }

    // ---------------------------------------------------------------- unvollständiger Schlaf

    @Test
    fun `unvollstaendiger Schlaf wird begrenzt nachgeladen`() {
        var stand = WhoopAbrufPlan.nachErfolg(Stand(), t0, WhoopAbrufPlan.Schlaf(t0 - 10 * min, vollstaendig = false))
        assertThat(stand.offeneVersuche).isEqualTo(0)
        // Erster Nachlade-Versuch nach 30 Minuten, nicht erst nach einer Stunde
        assertThat(entscheide(stand.copy(letzterVersuchMs = t0), Anlass.AUTOMATIK, t0 + 31 * min).abrufen).isTrue()
        var jetzt = t0
        var abrufe = 0
        // 7 Stunden lang alle 5 Minuten fragen: nur die begrenzten Nachlade-Versuche gehen durch
        while (jetzt < t0 + 7 * h) {
            jetzt += 5 * min
            if (entscheide(stand, Anlass.AUTOMATIK, jetzt).abrufen) {
                abrufe++
                stand = WhoopAbrufPlan.nachVersuch(stand, jetzt)
                stand = WhoopAbrufPlan.nachErfolg(stand, jetzt, WhoopAbrufPlan.Schlaf(t0 - 10 * min, vollstaendig = false))
            }
        }
        assertThat(abrufe).isAtMost(WhoopAbrufPlan.OFFEN_ABSTAENDE_MS.size)
        assertThat(abrufe).isAtLeast(2)
        // Fertig berechnet: danach Ruhe
        stand = WhoopAbrufPlan.nachErfolg(stand, jetzt, WhoopAbrufPlan.Schlaf(t0 - 10 * min, vollstaendig = true))
        assertThat(stand.offeneVersuche).isEqualTo(0)
        assertThat(entscheide(stand, Anlass.AUTOMATIK, jetzt + 40 * min).abrufen).isFalse()
    }

    // ---------------------------------------------------------------- Schicht- und Tageswechsel

    @Test
    fun `Tagschlaf nach Nachtdienst zaehlt wie Nachtschlaf - es gibt keine Uhrzeit-Regel`() {
        // Schlaf 07:00–13:30 nach dem Nachtdienst; um 15:00 und um Mitternacht soll nichts passieren
        val schlafEnde = t0
        val stand = Stand(t0 + 20 * min, t0 + 20 * min, schlafEnde, true)
        assertThat(entscheide(stand, Anlass.AUTOMATIK, schlafEnde + 90 * min).abrufen).isFalse()
        // Tageswechsel (anderes Datum) ändert nichts, solange kein neuer Schlaf möglich ist
        assertThat(entscheide(stand, Anlass.AUTOMATIK, schlafEnde + 7 * h, tag = "2026-10-16").abrufen).isFalse()
        // Wechsel auf Tagdienst: nächster Schlaf endet ~15 h später – wird dann geholt
        assertThat(entscheide(stand, Anlass.AUTOMATIK, schlafEnde + 15 * h, tag = "2026-10-16").abrufen).isTrue()
    }

    @Test
    fun `neuer Schlaf wird erkannt und setzt den Nachlade-Zaehler`() {
        val alt = fertig()
        val neu = WhoopAbrufPlan.nachErfolg(alt, t0 + 20 * h, WhoopAbrufPlan.Schlaf(t0 + 19 * h, vollstaendig = false))
        assertThat(neu.schlafEndeMs).isEqualTo(t0 + 19 * h)
        assertThat(neu.schlafVollstaendig).isFalse()
        assertThat(neu.offeneVersuche).isEqualTo(0)
    }

    // ---------------------------------------------------------------- Fehler, 429, Anmeldung

    @Test
    fun `Fehlversuche verlaengern den Abstand, Erfolg setzt zurueck`() {
        var stand = WhoopAbrufPlan.nachVersuch(fertig(), t0 + 10 * h)
        stand = WhoopAbrufPlan.nachFehler(stand)
        assertThat(entscheide(stand, Anlass.AUTOMATIK, t0 + 10 * h + 20 * min).abrufen).isFalse() // 30 min Mindestabstand
        assertThat(entscheide(stand, Anlass.AUTOMATIK, t0 + 10 * h + 31 * min).abrufen).isTrue()
        stand = WhoopAbrufPlan.nachFehler(WhoopAbrufPlan.nachFehler(stand))
        assertThat(stand.fehlversuche).isEqualTo(3)
        assertThat(entscheide(stand, Anlass.AUTOMATIK, t0 + 10 * h + 50 * min).abrufen).isFalse() // 1 h Pause
        assertThat(WhoopAbrufPlan.backoff(10)).isEqualTo(4 * h)
        stand = WhoopAbrufPlan.nachErfolg(stand, t0 + 12 * h, null)
        assertThat(stand.fehlversuche).isEqualTo(0)
    }

    @Test
    fun `429 mit Retry-After sperrt auch den Knopf bis zum genannten Zeitpunkt`() {
        val stand = WhoopAbrufPlan.nachFehler(fertig(), sperreBisMs = t0 + 20 * h)
        assertThat(entscheide(stand, Anlass.AUSDRUECKLICH, t0 + 19 * h).abrufen).isFalse()
        assertThat(entscheide(stand, Anlass.AUSDRUECKLICH, t0 + 20 * h + 1).abrufen).isTrue()
    }

    @Test
    fun `abgelaufene Anmeldung - Hintergrund prueft nur alle 6 Stunden, ausdruecklich darf geprueft werden`() {
        val stand = Stand(letzterVersuchMs = t0)
        assertThat(entscheide(stand, Anlass.AUTOMATIK, t0 + 2 * h, reauth = true).abrufen).isFalse()
        assertThat(entscheide(stand, Anlass.VORDERGRUND, t0 + 2 * h, reauth = true).abrufen).isFalse()
        assertThat(entscheide(stand, Anlass.OHNE_WHOOP, t0 + 7 * h, reauth = true).abrufen).isFalse()
        assertThat(entscheide(stand, Anlass.AUTOMATIK, t0 + 6 * h + 1, reauth = true).abrufen).isTrue()
        assertThat(entscheide(stand, Anlass.AUSDRUECKLICH, t0 + 11 * min, reauth = true).abrufen).isTrue()
    }

    // ---------------------------------------------------------------- Vollständigkeit aus Whoop-Rohdaten

    @Test
    fun `neuester Hauptschlaf - Nickerchen zaehlt nicht, fertig nur mit fertiger Erholung`() {
        val schlaefe = listOf(
            SchlafRoh("a", t0 - 24 * h, nap = false, scoreState = "SCORED"),
            SchlafRoh("b", t0, nap = false, scoreState = "SCORED"),
            SchlafRoh("n", t0 + 5 * h, nap = true, scoreState = "SCORED"),
        )
        val ohneErholung = WhoopAbrufPlan.neuesterSchlaf(schlaefe, listOf(ErholungRoh("a", "SCORED")))
        assertThat(ohneErholung).isEqualTo(WhoopAbrufPlan.Schlaf(t0, vollstaendig = false))
        val ausstehend = WhoopAbrufPlan.neuesterSchlaf(schlaefe, listOf(ErholungRoh("b", "PENDING_SCORE")))
        assertThat(ausstehend!!.vollstaendig).isFalse()
        val fertig = WhoopAbrufPlan.neuesterSchlaf(schlaefe, listOf(ErholungRoh("b", "SCORED")))
        assertThat(fertig).isEqualTo(WhoopAbrufPlan.Schlaf(t0, vollstaendig = true))
        // Band nicht getragen: endgültig, nicht ewig nachfragen
        val unberechenbar = WhoopAbrufPlan.neuesterSchlaf(
            listOf(SchlafRoh("c", t0, false, "UNSCORABLE")), listOf(ErholungRoh("c", "UNSCORABLE")),
        )
        assertThat(unberechenbar!!.vollstaendig).isTrue()
        assertThat(WhoopAbrufPlan.neuesterSchlaf(emptyList(), emptyList())).isNull()
    }
}
