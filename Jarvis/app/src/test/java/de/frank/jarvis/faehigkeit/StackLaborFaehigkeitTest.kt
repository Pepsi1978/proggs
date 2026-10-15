package de.frank.jarvis.faehigkeit

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackLaborFaehigkeitTest {
    @Test fun stacksLesenWaehltDieRichtigeBrueckenMethode() {
        assertEquals("stacks", StackLaborFaehigkeit.leseAufruf(JSONObject()).first)
        assertFalse(StackLaborFaehigkeit.leseAufruf(JSONObject()).second.optBoolean("voll"))
        assertTrue(StackLaborFaehigkeit.leseAufruf(JSONObject().put("vollstaendig", true)).second.optBoolean("voll"))
        val stack = StackLaborFaehigkeit.leseAufruf(JSONObject().put("stack", "Sportmodus 2"))
        assertEquals("stack", stack.first)
        assertEquals("Sportmodus 2", stack.second.getString("stack"))
        assertEquals("mittel", StackLaborFaehigkeit.leseAufruf(JSONObject().put("mittel", "Glycin")).first)
        assertEquals("mittel", StackLaborFaehigkeit.leseAufruf(JSONObject().put("katalog", true)).first)
        assertEquals("ziele", StackLaborFaehigkeit.leseAufruf(JSONObject().put("ziele", true)).first)
        // Leere Felder (manche Modelle füllen alle aus) zählen nicht.
        assertEquals("stacks", StackLaborFaehigkeit.leseAufruf(JSONObject().put("stack", "").put("mittel", " ")).first)
    }

    @Test fun rueckfragenUndFehlerSindNieEineAusgefuehrteAenderung() {
        val rueckfrage = StackLaborFaehigkeit.alsErgebnis(JSONObject().put("fehler", "Mehrere Stack-Namen passen zu „Sport“").put("rueckfrage", true))
        assertTrue(rueckfrage.fehler)
        val gespeichert = StackLaborFaehigkeit.alsErgebnis(JSONObject().put("text", "Gespeichert und nachgelesen: Glycin ist jetzt in Sport 2."))
        assertFalse(gespeichert.fehler)
        assertTrue(gespeichert.text.startsWith("Gespeichert und nachgelesen"))
        assertTrue(StackLaborFaehigkeit.alsErgebnis(JSONObject()).text.contains("leer"))
    }

    @Test fun nurLesendeMethodenBekommenDasKurzeZeitlimit() {
        listOf("status", "stacks", "stack", "mittel", "ziele").forEach { assertTrue(it, StackLaborFaehigkeit.lesend(it)) }
        listOf("eintrag", "stack_speichern", "mittel_speichern", "loeschen").forEach { assertFalse(it, StackLaborFaehigkeit.lesend(it)) }
    }
}
