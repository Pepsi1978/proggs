package de.frank.codexkompass.tts

import de.frank.codexkompass.data.model.Geschlecht
import de.frank.codexkompass.data.model.Stimme

/**
 * Der vollständige Stimmen-Katalog (Referenz, Baustein D).
 *
 * Die Listen sind bewusst vollständig und nicht auf „die drei schönsten" gekürzt: Welche
 * Stimme angenehm klingt, entscheidet sich beim Hören, nicht beim Programmieren. Deshalb
 * bekommt jede Stimme in den Einstellungen einen Probe-Knopf.
 */
object TtsCatalog {

    const val STANDARD_EDGE_STIMME = "de-DE-SeraphinaMultilingualNeural"
    const val STANDARD_GOOGLE_STIMME = "de-DE-Chirp3-HD-Kore"

    val edgeStimmen = listOf(
        Stimme("de-DE-SeraphinaMultilingualNeural", "Seraphina", Geschlecht.WEIBLICH),
        Stimme("de-DE-KatjaNeural", "Katja", Geschlecht.WEIBLICH),
        Stimme("de-DE-AmalaNeural", "Amala", Geschlecht.WEIBLICH),
        Stimme("de-DE-FlorianMultilingualNeural", "Florian", Geschlecht.MAENNLICH),
        Stimme("de-DE-KillianNeural", "Killian", Geschlecht.MAENNLICH),
        Stimme("de-DE-ConradNeural", "Conrad", Geschlecht.MAENNLICH),
    )

    val googleStimmen = listOf(
        Stimme("de-DE-Chirp3-HD-Achernar", "Achernar", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Aoede", "Aoede", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Autonoe", "Autonoe", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Callirrhoe", "Callirrhoe", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Despina", "Despina", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Erinome", "Erinome", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Gacrux", "Gacrux", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Kore", "Kore", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Laomedeia", "Laomedeia", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Leda", "Leda", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Pulcherrima", "Pulcherrima", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Sulafat", "Sulafat", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Vindemiatrix", "Vindemiatrix", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Zephyr", "Zephyr", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Achird", "Achird", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Algenib", "Algenib", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Algieba", "Algieba", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Alnilam", "Alnilam", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Charon", "Charon", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Enceladus", "Enceladus", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Fenrir", "Fenrir", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Iapetus", "Iapetus", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Orus", "Orus", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Puck", "Puck", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Rasalgethi", "Rasalgethi", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Sadachbia", "Sadachbia", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Sadaltager", "Sadaltager", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Schedar", "Schedar", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Umbriel", "Umbriel", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Zubenelgenubi", "Zubenelgenubi", Geschlecht.MAENNLICH),
    )

    /** Kurzer Satz zum Probehören — lang genug, um die Stimme zu beurteilen. */
    const val PROBETEXT =
        "Guten Tag. So klinge ich, wenn ich dir einen Befehl aus Codex CLI vorlese."
}
