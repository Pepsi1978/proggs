package de.frank.stacklabor.vorlesen

/**
 * Alle Anbieter und Stimmen, die StackLabor vorlesen kann (F-16, F-18).
 *
 * Die Listen sind 1:1 aus dem Vorbildprojekt PerfectMoment (`TtsCatalog.kt`) uebernommen:
 * 6 deutsche Edge-Stimmen und 30 deutsche Google-Chirp-3-HD-Stimmen. Der Qwen-Stimmklon
 * hat keine feste Liste, weil dort Franks eigene, geklonte Stimme ueber eine Stimm-Kennung
 * angesprochen wird.
 */

/** Ein Sprachanbieter. [kennung] wird gespeichert, [anzeigename] steht in der Oberflaeche. */
enum class Anbieter(val kennung: String, val anzeigename: String) {
    EDGE("edge_tts", "Microsoft Edge"),
    GOOGLE_CLOUD("google_cloud", "Google Chirp 3 HD"),
    QWEN_KLON("qwen_clone", "Meine Stimme"),
    ;

    companion object {
        /** Liest einen gespeicherten Wert zurueck; unbekannte Werte fallen auf die Vorgabe zurueck. */
        fun ausKennung(kennung: String?): Anbieter =
            entries.firstOrNull { it.kennung == kennung } ?: StimmenKatalog.VORGABE_ANBIETER
    }
}

enum class Geschlecht { WEIBLICH, MAENNLICH }

/** Eine waehlbare Stimme. [id] ist die Anbieter-Kennung, [name] der kurze Anzeigename. */
data class Stimme(
    val id: String,
    val name: String,
    val geschlecht: Geschlecht,
)

/**
 * Ein Sprecher wandelt Text in Audiodaten um — mehr nicht. Die Wiedergabe, die Absatzfolge
 * und der Pegel liegen bewusst in [VorleseSteuerung], damit alle drei Anbieter identisch
 * abgespielt werden.
 */
interface Sprecher {
    val anbieter: Anbieter

    /**
     * Erzeugt die Audiodaten fuer [text] (MP3 bei Edge und Google, WAV bei Qwen).
     *
     * @param stimmeId Anbieter-Kennung der Stimme; bei Qwen die Kennung des Stimmklons.
     * @param tempo Sprechtempo, 1.0 ist normal. Anbieter ohne Tempo-Parameter ignorieren den Wert.
     * @throws VorleseFehler wenn der Anbieter nicht erreichbar ist, der Schluessel fehlt oder
     *         keine Audiodaten geliefert wurden.
     */
    suspend fun sprich(text: String, stimmeId: String, tempo: Float): ByteArray

    /** Gibt Netz-Ressourcen frei. Nach dem Aufruf ist der Sprecher nicht mehr verwendbar. */
    fun schliesse()
}

/** Klartext-Fehler des Vorlesens; die Meldung ist fuer die Oberflaeche gedacht. */
class VorleseFehler(meldung: String, ursache: Throwable? = null) : Exception(meldung, ursache)

object StimmenKatalog {

    /** Vorgaben laut F-18. */
    val VORGABE_ANBIETER = Anbieter.EDGE
    const val VORGABE_EDGE_STIMME = "de-DE-SeraphinaMultilingualNeural"
    const val VORGABE_GOOGLE_STIMME = "de-DE-Chirp3-HD-Kore"

    val anbieter: List<Anbieter> = Anbieter.entries

    val edgeStimmen: List<Stimme> = listOf(
        Stimme("de-DE-SeraphinaMultilingualNeural", "Seraphina", Geschlecht.WEIBLICH),
        Stimme("de-DE-FlorianMultilingualNeural", "Florian", Geschlecht.MAENNLICH),
        Stimme("de-DE-KatjaNeural", "Katja", Geschlecht.WEIBLICH),
        Stimme("de-DE-KillianNeural", "Killian", Geschlecht.MAENNLICH),
        Stimme("de-DE-ConradNeural", "Conrad", Geschlecht.MAENNLICH),
        Stimme("de-DE-AmalaNeural", "Amala", Geschlecht.WEIBLICH),
    )

    val googleStimmen: List<Stimme> = listOf(
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

    /** Stimmen zur Auswahl. Der Stimmklon hat nur die eine eingerichtete Stimme. */
    fun stimmenFuer(anbieter: Anbieter): List<Stimme> = when (anbieter) {
        Anbieter.EDGE -> edgeStimmen
        Anbieter.GOOGLE_CLOUD -> googleStimmen
        Anbieter.QWEN_KLON -> emptyList()
    }

    /** Vorgabe-Stimme des Anbieters; beim Stimmklon leer, weil die Kennung beim Klonen entsteht. */
    fun vorgabeStimme(anbieter: Anbieter): String = when (anbieter) {
        Anbieter.EDGE -> VORGABE_EDGE_STIMME
        Anbieter.GOOGLE_CLOUD -> VORGABE_GOOGLE_STIMME
        Anbieter.QWEN_KLON -> ""
    }

    /** Findet eine Stimme; unbekannte Kennungen ergeben `null` (die Oberflaeche zeigt dann die Vorgabe). */
    fun stimme(anbieter: Anbieter, id: String): Stimme? =
        stimmenFuer(anbieter).firstOrNull { it.id == id }

    /**
     * Sorgt dafuer, dass zu einem Anbieter immer eine passende Stimme gehoert: Wechselt Frank den
     * Anbieter, passt die alte Kennung nicht mehr und wird auf die Vorgabe zurueckgesetzt.
     */
    fun gueltigeStimme(anbieter: Anbieter, gewaehlt: String): String = when {
        anbieter == Anbieter.QWEN_KLON -> gewaehlt
        stimme(anbieter, gewaehlt) != null -> gewaehlt
        else -> vorgabeStimme(anbieter)
    }
}
