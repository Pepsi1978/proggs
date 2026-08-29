package de.frank.claudekompass.tts

/** Fertig synthetisierter Absatz. [endung] bestimmt, als was die Datei geschrieben wird. */
data class SyntheseErgebnis(val audio: ByteArray, val endung: String) {

    // ByteArray hat keine sinnvolle Gleichheit; ohne diese beiden Methoden würde ein
    // versehentlicher Vergleich still immer false liefern.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SyntheseErgebnis) return false
        return endung == other.endung && audio.contentEquals(other.audio)
    }

    override fun hashCode(): Int = 31 * audio.contentHashCode() + endung.hashCode()
}

/** Wie schwer ein Fehler wiegt — davon hängt ab, ob die Pipeline weiterläuft oder anhält. */
enum class TtsFehlerArt {
    /** Schlüssel fehlt oder wurde abgelehnt. Betrifft die ganze Sitzung. */
    SCHLUESSEL,

    /** Kontingent erschöpft oder Anfragen zu schnell. Betrifft die ganze Sitzung. */
    KONTINGENT,

    /** Vorübergehende Störung. Erneut versuchen lohnt sich. */
    NETZ,

    /** Dieser eine Absatz wurde abgelehnt. Der Rest kann normal weiterlaufen. */
    INHALT,
}

/**
 * Fehler beim Vorlesen.
 *
 * [sitzungsweit] ist die wichtige Unterscheidung: Ein abgelehnter Schlüssel betrifft jeden
 * folgenden Absatz, also muss die Pipeline anhalten und den Grund zeigen. Ein einzelner
 * abgelehnter Absatz darf dagegen übersprungen werden, ohne dass der ganze Text stumm bleibt.
 */
class TtsFehler(
    val art: TtsFehlerArt,
    meldung: String,
    val wiederholbar: Boolean = false,
    ursache: Throwable? = null,
) : Exception(meldung, ursache) {

    val sitzungsweit: Boolean
        get() = art == TtsFehlerArt.SCHLUESSEL || art == TtsFehlerArt.KONTINGENT

    /** Wartezeit in Millisekunden, die der Dienst vorgegeben hat; 0, wenn keine kam. */
    var wartezeitMs: Long = 0L
}

/** Gemeinsame Form aller drei Vorlese-Dienste. */
interface TtsSynthesizer {
    /** Kennung des Anbieters, wie sie in den Einstellungen steht. */
    val anbieterId: String

    /**
     * Wandelt einen Absatz in Ton.
     *
     * [tempo] wird nur von Diensten berücksichtigt, die es kennen. Wo das nicht geht, regelt
     * der Abspieler die Geschwindigkeit — das Ergebnis ist dasselbe.
     */
    suspend fun synthetisiere(text: String, stimme: String, tempo: Float): SyntheseErgebnis

    fun beende()
}
