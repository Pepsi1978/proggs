package de.frank.wecker

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SpracheTest {
    // ---- Migration und Persistenz ----

    @Test fun alterEintragOhneSprachfeldBleibtDeutsch() {
        val alt = Alarm(steps = listOf(Step.TEXT), text = "Hallo").json().apply { remove("sprache") }
        assertFalse(alt.has("sprache"))
        assertEquals("de", Alarm.from(alt).sprache)
    }

    @Test fun gespeicherteSpracheBleibtUndUnbekanntesWirdDeutsch() {
        assertEquals("fr", Alarm.from(Alarm(sprache = "fr").json()).sprache)
        assertEquals("de", Alarm.from(Alarm().json().put("sprache", "xx")).sprache)
        assertEquals("es", Alarm.from(JSONObject(Alarm(sprache = "es").json().toString())).sprache)
    }

    @Test fun sprachwechselGiltAlsGeaenderteAnsage() {
        val de = Alarm(steps = listOf(Step.TEXT), text = "Guten Morgen")
        assertFalse(de.sameSpeechAs(de.copy(sprache = "en")))
        assertTrue(de.sameSpeechAs(de.copy()))
    }

    @Test fun diktatUndVorlesenNutzenDieselbeWeckerSprache() {
        assertEquals(listOf("de", "en", "fr", "es"), DiktatSprache.entries.map { it.code })
        assertEquals(DiktatSprache.FR, Sprachen.diktat("fr"))
        assertEquals(DiktatSprache.DE, Sprachen.diktat(""))
    }

    // ---- Stimmauflösung und Signatur ----

    private val einstellungen = SyntheseStimme("de-de-x-nfh-local", 1f, "de",
        mapOf("de" to "de-de-x-nfh-local", "en" to "en-us-x-iol-local", "fr" to "", "es" to ""))

    @Test fun jedeSpracheNimmtIhreBevorzugteStimme() {
        assertEquals("de-de-x-nfh-local", Alarm(sprache = "de").resolveVoice(einstellungen).stimme)
        val en = Alarm(sprache = "en").resolveVoice(einstellungen)
        assertEquals("en-us-x-iol-local" to "en", en.stimme to en.sprache)
        // Französisch ohne installierte/gewählte Stimme: leer, die Synthese meldet dann ehrlich „keine Stimme“.
        assertEquals("", Alarm(sprache = "fr").resolveVoice(einstellungen).stimme)
    }

    @Test fun eigeneStimmeEinerAnderenSpracheWirdNieBenutzt() {
        val alarm = Alarm(sprache = "fr", voiceProvider = LokaleStimmen.PROVIDER, voiceId = "de-de-x-deg-local")
        assertEquals("fr", alarm.resolveVoice(einstellungen).sprache)
        assertNotEquals("de-de-x-deg-local", alarm.resolveVoice(einstellungen).stimme)
    }

    @Test fun deutscheSignaturBleibtByteGleichAndereSprachenUnterscheidenSich() {
        // Exakt das Format vor 1.0.4: alte deutsche Wecker werden nicht neu vorbereitet.
        assertEquals("lokal-v2|android_tts|de-de-x-deg-local|1.0", SpeechPreparation.schluessel(SyntheseStimme("de-de-x-deg-local", 1f)))
        assertEquals("lokal-v2|android_tts|de-de-x-deg-local|1.0", SpeechPreparation.schluessel(einstellungen.fuerSprache("de").copy(stimme = "de-de-x-deg-local")))
        assertEquals("lokal-v2|android_tts|en-us-x-iol-local|1.0|en", SpeechPreparation.schluessel(einstellungen.fuerSprache("en")))
    }

    @Test fun googleBindungLaesstAltbestandUndSignaturUnveraendert() {
        // Altbestand „android_tts“ und das neue „tts:com.google.android.tts“ meinen dieselbe Engine.
        assertTrue(LokaleStimmen.istEigeneEngine(LokaleStimmen.PROVIDER))
        assertTrue(LokaleStimmen.istEigeneEngine(LokaleStimmen.provider(LokaleStimmen.GOOGLE)))
        assertFalse(LokaleStimmen.istEigeneEngine("tts:com.samsung.SMT"))
        val google = Alarm(sprache = "de", voiceProvider = LokaleStimmen.provider(LokaleStimmen.GOOGLE), voiceId = "de-de-x-deg-local")
        assertEquals("de-de-x-deg-local", google.resolveVoice(einstellungen).stimme)
        // Eine (noch) nicht angebotene Fremd-Engine wird nie still benutzt: Standard der Sprache.
        val fremd = Alarm(sprache = "de", voiceProvider = "tts:com.samsung.SMT", voiceId = "de-DE-SMTm00")
        assertEquals("de-de-x-nfh-local", fremd.resolveVoice(einstellungen).stimme)
        // Signatur: Google explizit = leer = byte-gleich zu vc5; nur eine andere Engine hängt sich an.
        val deg = SyntheseStimme("de-de-x-deg-local", 1f)
        assertEquals("lokal-v2|android_tts|de-de-x-deg-local|1.0", SpeechPreparation.schluessel(deg.copy(engine = LokaleStimmen.GOOGLE)))
        assertEquals("lokal-v2|android_tts|en-us-x-iol-local|1.0|en", SpeechPreparation.schluessel(einstellungen.fuerSprache("en").copy(engine = LokaleStimmen.GOOGLE)))
        assertEquals("lokal-v2|android_tts|de-de-x-deg-local|1.0|engine=com.example.tts", SpeechPreparation.schluessel(deg.copy(engine = "com.example.tts")))
    }

    @Test fun nurDeutschFiltertAufDieZweiBevorzugten() {
        val en = listOf("en-us-x-iob-local", "en-us-x-iol-local")
        assertEquals(en, StimmAuswahl.angeboten(en, "en"))
        assertEquals("Englisch (Vereinigte Staaten) · Stimme 2", LokaleStimmeInfo("en-us-x-iol-local", java.util.Locale.US, 400, 2).anzeige)
        assertFalse(LokaleStimmeInfo("fr-fr-x-frb-local", java.util.Locale.FRANCE, 400, 1).anzeige.contains("Qualität"))
    }
}
