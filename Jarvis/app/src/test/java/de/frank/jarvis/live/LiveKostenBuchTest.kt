package de.frank.jarvis.live

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveKostenBuchTest {

    /** SharedPreferences im Speicher; `clear()` wirkt wie bei Android vor den übrigen Änderungen. */
    private class Speicher : SharedPreferences {
        val werte = HashMap<String, Any?>()
        override fun getAll(): MutableMap<String, *> = werte
        override fun getString(k: String, d: String?) = werte[k] as String? ?: d
        override fun getStringSet(k: String, d: MutableSet<String>?) = d
        override fun getInt(k: String, d: Int) = werte[k] as Int? ?: d
        override fun getLong(k: String, d: Long) = werte[k] as Long? ?: d
        override fun getFloat(k: String, d: Float) = werte[k] as Float? ?: d
        override fun getBoolean(k: String, d: Boolean) = werte[k] as Boolean? ?: d
        override fun contains(k: String) = k in werte
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            val neu = HashMap<String, Any?>()
            var leeren = false
            override fun putString(k: String, v: String?) = apply { neu[k] = v }
            override fun putStringSet(k: String, v: MutableSet<String>?) = apply { neu[k] = v }
            override fun putInt(k: String, v: Int) = apply { neu[k] = v }
            override fun putLong(k: String, v: Long) = apply { neu[k] = v }
            override fun putFloat(k: String, v: Float) = apply { neu[k] = v }
            override fun putBoolean(k: String, v: Boolean) = apply { neu[k] = v }
            override fun remove(k: String) = apply { neu[k] = null }
            override fun clear() = apply { leeren = true }
            override fun commit(): Boolean { if (leeren) werte.clear(); werte.putAll(neu); return true }
            override fun apply() { commit() }
        }
    }

    @Test
    fun buchenSummiertGetrenntUndRechnetDurchschnitt() {
        val p = Speicher()
        val leer = LiveKostenBuch.lesen(p)
        assertEquals(0, leer.gespraeche)
        assertNull(leer.proMinute)
        assertTrue(leer.seit > 0)

        // GPT-Live, 2 Minuten bestätigt: 0,10 $ Sitzung + 0,02 $ Denk-Modell.
        LiveKostenBuch.buchen(p, LiveKostenBuch.Abrechnung(120.0, true, 0.12, 0.0, 0))
        // Abgebrochen: 1 Minute bestätigt, 1 Minute nur gemessen (geschätzt 0,05 $).
        val s = LiveKostenBuch.buchen(p, LiveKostenBuch.Abrechnung(120.0, false, 0.05, 0.05, 1))

        assertEquals(2, s.gespraeche)
        assertEquals(1, s.bestaetigt)
        assertEquals(1, s.ohnePreis)
        assertEquals(240.0, s.dauerSek, 1e-9)
        assertEquals(0.17, s.berechnetUsd, 1e-9)
        assertEquals(0.05, s.geschaetztUsd, 1e-9)
        assertEquals(0.22, s.gesamtUsd, 1e-9)
        assertEquals(0.055, s.proMinute!!, 1e-9)
        assertEquals(3.3, s.proStunde!!, 1e-9)
        assertEquals(leer.seit, s.seit)
    }

    @Test
    fun zuruecksetzenLeertAllesUndStartetZeitbasisNeu() {
        val p = Speicher()
        p.edit().putLong("seit", 1_000L).commit()
        LiveKostenBuch.buchen(p, LiveKostenBuch.Abrechnung(600.0, true, 0.5, 0.0, 0))
        val s = LiveKostenBuch.zuruecksetzen(p)
        assertEquals(0, s.gespraeche)
        assertEquals(0.0, s.gesamtUsd, 0.0)
        assertEquals(0.0, s.dauerSek, 0.0)
        assertEquals(0L, s.zuletzt)
        assertNull(s.proMinute)
        assertTrue(s.seit > 1_000L)
        // Nach dem Zurücksetzen zählt es normal weiter.
        assertEquals(1, LiveKostenBuch.buchen(p, LiveKostenBuch.Abrechnung(60.0, true, 0.05, 0.0, 0)).gespraeche)
    }
}
