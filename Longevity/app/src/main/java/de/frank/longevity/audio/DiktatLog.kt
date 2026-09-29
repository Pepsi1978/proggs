package de.frank.longevity.audio

import android.util.Log

/** Schmales Protokoll für das Diktat — schreibt nur ins Logcat. */
internal object IdeenLog {
    fun info(modul: String, funktion: String, nachricht: String, kontext: Map<String, Any?> = emptyMap()) {
        Log.i(modul, "$funktion: $nachricht $kontext")
    }

    fun warn(modul: String, funktion: String, nachricht: String, kontext: Map<String, Any?> = emptyMap()) {
        Log.w(modul, "$funktion: $nachricht $kontext")
    }

    fun debug(modul: String, funktion: String, nachricht: String, kontext: Map<String, Any?> = emptyMap()) {
        Log.d(modul, "$funktion: $nachricht $kontext")
    }
}
