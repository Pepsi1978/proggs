// ──────────────────────────────────────────────────────────────────────
// Anbindung für Modul M1.4 — Transkriptionsmodul
//
// Diese Datei gehört der App, nicht der Bibliothek. Hier kommt alles hin,
// was sich von App zu App unterscheidet: das Protokoll, die Umlaut-Korrektur
// und ob ONNX Runtime auf diesem Gerät ohne KleidiAI rechnen muss.
// Beim Nachziehen des Moduls wird sie NICHT überschrieben.
//
// Die Moduldateien daneben sind tabu — Anpassungen gehören hierher.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

import android.content.Context
import de.frank.jarvis.observability.IdeenLog
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.text.UmlautKorrektur
import de.frank.jarvis.weckwort.WeckwortErkenner

object TranskriptionAnbindung {
    init {
        Protokoll.aktiv = object : Protokoll {
            override fun sprache(ereignis: String, details: String, ms: Long?) = Logbuch.sprache(ereignis, details, ms)
            override fun netz(ereignis: String, details: String) = Logbuch.netz(ereignis, details)
            override fun info(bereich: String, funktion: String, text: String, daten: Map<String, Any?>) =
                IdeenLog.info(bereich, funktion, text, daten)
            override fun warn(bereich: String, funktion: String, text: String, daten: Map<String, Any?>) =
                IdeenLog.warn(bereich, funktion, text, daten)
        }
    }

    /** Dieselbe Entscheidung wie beim Weckwort: Der Schalter wirkt auf die ganze ONNX Runtime. */
    fun mikrofon(context: Context) = MicRecorder(context, WeckwortErkenner.KLEIDIAI_ABSCHALTEN)

    /** Baustein M.2: Wo doch Ersatzschreibung ankommt, greift die Wörterbuch-Korrektur. */
    fun diktat(transcriber: GroqTranscriber) = Diktat(transcriber) { text ->
        UmlautKorrektur.korrigiere(text) { ersetzung ->
            IdeenLog.debug(
                "Diktat",
                "transkribiere",
                "Umlaut korrigiert",
                mapOf("vorher" to ersetzung.vorher, "nachher" to ersetzung.nachher),
            )
        }
    }
}
