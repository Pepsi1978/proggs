package de.frank.gedankenspeicher.ui

import androidx.compose.ui.text.input.TextFieldValue
import de.frank.gedankenspeicher.data.Auswertungsprofil
import de.frank.gedankenspeicher.data.Notiz
import de.frank.gedankenspeicher.data.Sitzung
import de.frank.gedankenspeicher.data.Suchtreffer
import de.frank.gedankenspeicher.data.Verlaufseintrag

/**
 * **Was die Oberfläche gerade zeigt.**
 *
 * Ein einziger Zustand statt vieler kleiner Flags: die Zustände aus `02-UI-SPEC.md` §6
 * schließen einander teilweise aus (es wird nicht gleichzeitig aufgenommen und ausgewertet),
 * und das lässt sich nur an einer Stelle sicherstellen.
 */
data class Verlaufszustand(
    val sitzung: Sitzung? = null,
    val sitzungen: List<Sitzung> = emptyList(),
    val eintraege: List<Verlaufseintrag> = emptyList(),
    val laedt: Boolean = true,
    val nimmtAuf: Boolean = false,
    val aufnahmeDauerMs: Long = 0,
    val pegel: Float = 0f,
    val mikrofonAbgelehnt: Boolean = false,
    val entwurf: String = "",
    /** Kennung dessen, was gerade vorgelesen wird: `notiz:12` oder `antwort:3`. */
    val liestVor: String? = null,
    val vorleseAbsatz: Int = -1,
    /** Notiz-Kennungen, an denen der Verbessern-Vorgang gerade läuft (F-07). */
    val verbessertGerade: Set<Long> = emptySet(),
    val wertetAus: Boolean = false,
    val meldung: String? = null,
    /** Sprungziel aus der Suche: diese Notiz leuchtet einmal auf (M-11). */
    val hebeHervor: Long? = null,
    /**
     * Die Notiz, die gerade **in ihrer Karte** bearbeitet wird — null, wenn keine.
     *
     * Der Zustand liegt im ViewModel und nicht in der Karte, damit er das Drehen und das
     * Auf- und Zuklappen übersteht: sonst wäre der halb getippte Satz beim Klappen weg.
     */
    val bearbeiteteNotiz: Long? = null,
    /** Der Stand des Feldes samt Cursorstelle. */
    val bearbeitungsEntwurf: TextFieldValue = TextFieldValue(),
)

/** Der Zustand des KI-Blattes (B-03). */
data class KiBlattzustand(
    val offen: Boolean = false,
    val kontextzahl: Int = 0,
    val ganzeSitzung: Boolean = false,
    val websuche: Boolean = false,
    val websucheKiEntscheidet: Boolean = false,
    val profil: Auswertungsprofil? = null,
    val holtFrage: Boolean = false,
    val rueckfrage: String = "",
    val antwort: String = "",
    val nimmtAntwortAuf: Boolean = false,
    val codexFehlt: Boolean = false,
    val fehler: String? = null,
    /** Steht auf true, wenn seit der letzten Auswertung nichts Neues dazugekommen ist. */
    val nichtsNeues: Boolean = false,
)

/** Der Zustand der Suche (B-07). */
data class Suchzustand(
    val begriff: String = "",
    val sucht: Boolean = false,
    val treffer: List<Suchtreffer> = emptyList(),
)

/** Der Zustand des Bearbeiten-Blattes (B-08). */
data class Bearbeitungszustand(
    val notiz: Notiz? = null,
    val ueberschrift: String = "",
    val text: String = "",
) {
    val geaendert: Boolean
        get() = notiz != null &&
            (ueberschrift.trim() != (notiz.ueberschrift ?: "").trim() || text.trim() != notiz.text.trim())
}

/** Der Zustand der Codex-Anmeldung (B-05). */
data class Anmeldezustand(
    val holtCode: Boolean = false,
    val code: String = "",
    val adresse: String = "",
    val wartet: Boolean = false,
    val abgelaufen: Boolean = false,
    val erfolgreich: Boolean = false,
    val fehler: String? = null,
)
