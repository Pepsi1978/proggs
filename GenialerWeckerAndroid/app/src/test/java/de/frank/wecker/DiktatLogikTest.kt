package de.frank.wecker

import android.speech.SpeechRecognizer
import org.junit.Assert.*
import org.junit.Test

class DiktatLogikTest {
    @Test fun vierSprachenMitFestenBcp47TagsDeutschZuerst() {
        assertEquals(listOf("de-DE", "en-US", "fr-FR", "es-ES"), DiktatSprache.entries.map { it.tag })
        assertEquals(DiktatSprache.DE, DiktatSprache.entries.first())
    }

    @Test fun spracheFolgtGeraetesprachUndRegion() {
        assertEquals(DiktatSprache.DE to true, DiktatLogik.spracheFuer("de", "AT"))
        assertEquals(DiktatSprache.FR to true, DiktatLogik.spracheFuer("fr", "CH"))
        assertEquals(DiktatSprache.ES to true, DiktatLogik.spracheFuer("es", "MX"))
        assertEquals(DiktatSprache.EN to true, DiktatLogik.spracheFuer("en", "GB"))
        // Unbekannte Sprache: Region entscheidet, sonst Deutsch mit Hinweis.
        assertEquals(DiktatSprache.DE to true, DiktatLogik.spracheFuer("gsw", "CH"))
        assertEquals(DiktatSprache.EN to true, DiktatLogik.spracheFuer("", "US"))
        assertEquals(DiktatSprache.DE to false, DiktatLogik.spracheFuer("ja", "JP"))
    }

    @Test fun installiertesPaketWirdErkanntAuchInAndererSchreibweise() {
        assertTrue(DiktatLogik.passt(listOf("en-US", "DE-de"), "de-DE"))
        assertTrue(DiktatLogik.passt(listOf("de-AT"), "de-DE"))
        assertFalse(DiktatLogik.passt(listOf("en-US", "fr-FR"), "es-ES"))
        assertFalse(DiktatLogik.passt(emptyList(), "de-DE"))
    }

    @Test fun unterstuetzungFolgtDerReihenfolgeInstalliertLadendLadbar() {
        assertEquals(DiktatLogik.Unterstuetzung.INSTALLIERT, DiktatLogik.unterstuetzung(listOf("de-DE"), listOf("de-DE"), listOf("de-DE"), "de-DE"))
        assertEquals(DiktatLogik.Unterstuetzung.WIRD_GELADEN, DiktatLogik.unterstuetzung(emptyList(), listOf("fr-FR"), listOf("fr-FR"), "fr-FR"))
        assertEquals(DiktatLogik.Unterstuetzung.LADBAR, DiktatLogik.unterstuetzung(listOf("de-DE"), emptyList(), listOf("es-ES"), "es-ES"))
        assertEquals(DiktatLogik.Unterstuetzung.NICHT_UNTERSTUETZT, DiktatLogik.unterstuetzung(listOf("de-DE"), emptyList(), emptyList(), "es-ES"))
    }

    @Test fun fehlermeldungenVerweisenAufsTippenNieAufCloud() {
        val codes = listOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, 999)
        codes.forEach { code ->
            val text = DiktatLogik.fehlertext(code, DiktatSprache.FR)
            assertTrue(text, text.isNotBlank())
            assertFalse(text, listOf("Cloud", "Google", "Groq", "Whisper", "Internet verbinden").any { text.contains(it, ignoreCase = true) })
        }
        assertTrue(DiktatLogik.fehlertext(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, DiktatSprache.FR).contains("Französisch"))
    }
}
