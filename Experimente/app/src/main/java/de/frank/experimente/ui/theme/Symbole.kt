package de.frank.experimente.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Die Symbole des Entwurfs — jeder Pfad **wörtlich** aus `Experimente Fold-Aussendisplay.dc.html`
 * übernommen (`const ICONS` und die eingebetteten `<svg><path d="…">`).
 *
 * Nicht aus einer Standardsammlung: ein gleichnamiges Material-Symbol hat andere
 * Proportionen und Strichstärken und fällt gegen den Rest des Entwurfs auf.
 */
private fun gefuellt(name: String, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        pfade.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
    }.build()

/** Für Symbole, die im Entwurf als Strich gezeichnet sind, nicht als Fläche. */
private fun gestrichelt(name: String, staerke: Float, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        pfade.forEach {
            addPath(
                PathParser().parsePathString(it).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = staerke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

object Symbole {

    // --- Obere Leiste ------------------------------------------------------------------

    /** Das Zahnrad, das von B-10 und B-01 nach B-08 führt. */
    val Einstellungen: ImageVector by lazy {
        gefuellt(
            "Einstellungen",
            "M19.14 12.94c.04-.31.06-.63.06-.94s-.02-.63-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96a7.13 7.13 0 0 0-1.62-.94L14.4 2.81A.48.48 0 0 0 13.92 2h-3.84a.48.48 0 0 0-.48.41l-.36 2.54c-.59.24-1.13.56-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.72 8.47a.48.48 0 0 0 .12.61l2.03 1.58c-.05.31-.09.65-.09.98s.03.66.08.98l-2.02 1.57a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.38.31.59.22l2.39-.96c.49.38 1.03.7 1.62.94l.36 2.54c.04.23.24.41.48.41h3.84c.24 0 .44-.18.48-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.09.47 0 .59-.22l1.92-3.32a.49.49 0 0 0-.12-.61l-2.02-1.57ZM12 15.5A3.5 3.5 0 1 1 12 8a3.5 3.5 0 0 1 0 7.5Z",
        )
    }

    /** Der Pfeil zurück (B-02, B-03, B-08, B-09). */
    val Zurueck: ImageVector by lazy {
        gefuellt("Zurueck", "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2Z")
    }

    // --- Aufnahme ----------------------------------------------------------------------

    /** Das Mikrofon — Sprechknopf, Gespräch, Anlegefläche, Selbstbild. */
    val Mikrofon: ImageVector by lazy {
        gefuellt(
            "Mikrofon",
            "M12 14q-1.25 0-2.125-.875T9 11V5q0-1.25.875-2.125T12 2q1.25 0 2.125.875T15 5v6q0 1.25-.875 2.125T12 14Zm-1 7v-3.075q-2.6-.35-4.3-2.325T5 11h2q0 2.075 1.463 3.538T12 16q2.075 0 3.538-1.462T17 11h2q0 2.6-1.7 4.575T13 17.925V21Z",
        )
    }

    /** Das Quadrat, das die laufende Aufnahme beendet. */
    val Stopp: ImageVector by lazy { gefuellt("Stopp", "M7 7h10v10H7Z") }

    /** Der Aufnahmepunkt in der Stimm-Registrierung (B-08). */
    val AufnahmePunkt: ImageVector by lazy {
        gefuellt(
            "AufnahmePunkt",
            "M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8Zm0-6a10 10 0 1 1 0 20 10 10 0 0 1 0-20Zm0 2a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z",
        )
    }

    // --- Aufgaben ----------------------------------------------------------------------

    /** Das leere Kästchen einer offenen Aufgabe (F-08). */
    val KastenLeer: ImageVector by lazy {
        gefuellt(
            "KastenLeer",
            "M5 21q-.825 0-1.412-.587T3 19V5q0-.825.588-1.412T5 3h14q.825 0 1.413.588T21 5v14q0 .825-.587 1.413T19 21H5Zm0-2h14V5H5v14Z",
        )
    }

    /** Das abgehakte Kästchen (M-06, E-17). */
    val KastenVoll: ImageVector by lazy {
        gefuellt(
            "KastenVoll",
            "M10.6 16.6 17.65 9.55l-1.4-1.4L10.6 13.8l-2.85-2.85-1.4 1.4 4.25 4.25ZM5 21q-.825 0-1.412-.587T3 19V5q0-.825.588-1.412T5 3h14q.825 0 1.413.588T21 5v14q0 .825-.587 1.413T19 21H5Z",
        )
    }

    // --- Merken ------------------------------------------------------------------------

    /** Das Lesezeichen, offen — noch nicht gemerkt (F-05). */
    val Merken: ImageVector by lazy {
        gefuellt(
            "Merken",
            "M7 3h10q.825 0 1.413.588T19 5v16l-7-3-7 3V5q0-.825.588-1.412T7 3Zm0 3v11.95l5-2.15 5 2.15V5H7Z",
        )
    }

    /** Dasselbe Lesezeichen, gefüllt — gemerkt. */
    val Gemerkt: ImageVector by lazy {
        gefuellt("Gemerkt", "M5 21V5q0-.825.588-1.412T7 3h10q.825 0 1.413.588T19 5v16l-7-3Z")
    }

    // --- Karten und Listen --------------------------------------------------------------

    /** Der Griff zum Umsortieren einer anstehenden Karte (F-38). */
    val Griff: ImageVector by lazy {
        gefuellt(
            "Griff",
            "M9 20a2 2 0 1 1 0-4 2 2 0 0 1 0 4Zm6 0a2 2 0 1 1 0-4 2 2 0 0 1 0 4Zm-6-6a2 2 0 1 1 0-4 2 2 0 0 1 0 4Zm6 0a2 2 0 1 1 0-4 2 2 0 0 1 0 4ZM9 8a2 2 0 1 1 0-4 2 2 0 0 1 0 4Zm6 0a2 2 0 1 1 0-4 2 2 0 0 1 0 4Z",
        )
    }

    /** Das Kreuz — aus dem Monitor nehmen (F-39). */
    val Schliessen: ImageVector by lazy {
        gefuellt(
            "Schliessen",
            "M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41Z",
        )
    }

    /** Der Papierkorb der Merkliste (F-19). */
    val Loeschen: ImageVector by lazy {
        gefuellt(
            "Loeschen",
            "M6 19q0 .825.588 1.413T8 21h8q.825 0 1.413-.587T18 19V7H6v12ZM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4Z",
        )
    }

    /** Das Plus des schwebenden Knopfs (F-35, F-20). */
    val Plus: ImageVector by lazy { gefuellt("Plus", "M11 13H5v-2h6V5h2v6h6v2h-6v6h-2v-6Z") }

    /** Der Pfeil, der weiterführt — „Selbstbild“ in B-08. */
    val Weiter: ImageVector by lazy { gefuellt("Weiter", "m9 18-1.4-1.4 4.6-4.6-4.6-4.6L9 6l6 6-6 6Z") }

    /** Das kleine Dreieck eines Auswahlfelds. */
    val Aufklappen: ImageVector by lazy { gefuellt("Aufklappen", "M12 15 7 10h10l-5 5Z") }

    // --- Handlungen ---------------------------------------------------------------------

    /** Der Kreispfeil — „Andere Vorschläge“ (F-04) und „Nochmal versuchen“. */
    val Auffrischen: ImageVector by lazy {
        gefuellt(
            "Auffrischen",
            "M12 20q-3.35 0-5.675-2.325T4 12q0-3.35 2.325-5.675T12 4q1.725 0 3.3.713T18 6.7V4h2v7h-7V9h3.8q-.95-1.4-2.15-2.2T12 6Q9.5 6 7.75 7.75T6 12q0 2.5 1.75 4.25T12 18q1.925 0 3.475-1.1T17.65 14H19.7q-.7 2.65-2.85 4.325T12 20Z",
        )
    }

    /** Der Lautsprecher — Auswertung vorlesen (F-12) und Probe hören (F-23). */
    val Vorlesen: ImageVector by lazy {
        gefuellt(
            "Vorlesen",
            "M3 9v6h4l5 5V4L7 9H3Zm13.5 3c0-1.77-1-3.29-2.5-4.03v8.05c1.5-.73 2.5-2.25 2.5-4.02ZM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77Z",
        )
    }

    /** Der Papierflieger — eine Nachricht abschicken (B-02). */
    val Senden: ImageVector by lazy { gefuellt("Senden", "M3 20v-6l8-2-8-2V4l19 8-19 8Z") }

    /** Das Ausrufezeichen im Kreis — die Störungsmeldung. */
    val Hinweis: ImageVector by lazy {
        gefuellt(
            "Hinweis",
            "M12 17q.425 0 .713-.288T13 16q0-.425-.288-.712T12 15q-.425 0-.712.288T11 16q0 .425.288.713T12 17Zm-1-4h2V7h-2v6Zm1 9q-2.075 0-3.9-.788t-3.175-2.137Q3.575 17.725 2.788 15.9T2 12q0-2.075.788-3.9t2.137-3.175Q6.275 3.575 8.1 2.788T12 2q2.075 0 3.9.788t3.175 2.137q1.35 1.35 2.138 3.175T22 12q0 2.075-.788 3.9t-2.137 3.175q-1.35 1.35-3.175 2.138T12 22Z",
        )
    }

    // --- Zugänge ------------------------------------------------------------------------

    /** Das Auge — einen Schlüssel sichtbar machen (F-24). */
    val Auge: ImageVector by lazy {
        gefuellt(
            "Auge",
            "M12 16q1.875 0 3.188-1.312T16.5 11.5q0-1.875-1.312-3.187T12 7q-1.875 0-3.187 1.313T7.5 11.5q0 1.875 1.313 3.188T12 16Zm0-1.8q-1.125 0-1.912-.787T9.3 11.5q0-1.125.788-1.912T12 8.8q1.125 0 1.913.788T14.7 11.5q0 1.125-.787 1.913T12 14.2Zm0 4.8q-3.65 0-6.65-2.037T1 11.5q1.35-3.425 4.35-5.462T12 4q3.65 0 6.65 2.038T23 11.5q-1.35 3.425-4.35 5.463T12 19Z",
        )
    }

    /** Das durchgestrichene Auge — den Schlüssel wieder verbergen. */
    val AugeAus: ImageVector by lazy {
        gefuellt(
            "AugeAus",
            "m19.8 22.6-4.2-4.15q-.875.275-1.762.412T12 19q-3.775 0-6.725-2.087T1 11.5q.525-1.325 1.325-2.463T4.15 7L1.4 4.2l1.4-1.4 18.4 18.4-1.4 1.4ZM12 16q.275 0 .525-.025t.475-.1L8.625 11.5q-.075.225-.1.475T8.5 12.5q0 1.45 1.025 2.475T12 16Zm7.3.45-3.175-3.15q.175-.425.275-.862t.1-.938q0-1.875-1.312-3.187T12 7q-.5 0-.937.1t-.863.275L7.65 4.85q1-.425 2.087-.638T12 4q3.775 0 6.725 2.087T23 11.5q-.575 1.475-1.512 2.712T19.3 16.45Z",
        )
    }
}

/**
 * Die sechs Felder der unteren Leiste und die drei Symbole des Erscheinungs-Schnellschalters.
 *
 * Die Reihenfolge ist verbindlich: **Monitor · Heute · Ziele · Merkliste · Erkenntnisse ·
 * Logbuch** — auf jedem Hauptbildschirm gleich (UI-Spec §6).
 */
object Leistensymbole {

    /** `B-10` — vier Kacheln, das Gesicht des Monitors. */
    val Monitor: ImageVector by lazy {
        gefuellt("Monitor", "M4 4h7v7H4V4Zm0 9h7v7H4v-7Zm9-9h7v4h-7V4Zm0 6h7v10h-7V10Z")
    }

    /** `B-01` — der Kalenderblock. */
    val Heute: ImageVector by lazy {
        gefuellt(
            "Heute",
            "M5 22q-.825 0-1.412-.587T3 20V6q0-.825.588-1.412T5 4h1V2h2v2h8V2h2v2h1q.825 0 1.413.588T21 6v14q0 .825-.587 1.413T19 22H5Zm0-2h14V10H5v10Zm0-12h14V6H5v2Z",
        )
    }

    /** `B-04` — die Fahne. */
    val Ziele: ImageVector by lazy {
        gefuellt("Ziele", "M5 21V4h9l.4 2H20v10h-7l-.4-2H7v7H5Zm2-9h7.25l.4 2H18V8h-5.25l-.4-2H7v6Z")
    }

    /** `B-05` — das Lesezeichen. */
    val Merkliste: ImageVector by lazy {
        gefuellt(
            "Merkliste",
            "M7 3h10q.825 0 1.413.588T19 5v16l-7-3-7 3V5q0-.825.588-1.412T7 3Zm0 3v11.95l5-2.15 5 2.15V5H7Z",
        )
    }

    /** `B-06` — die Glühbirne. */
    val Erkenntnisse: ImageVector by lazy {
        gefuellt(
            "Erkenntnisse",
            "M9 21q-.425 0-.712-.288T8 20v-1h8v1q0 .425-.288.713T15 21H9Zm-1-4v-2.2q-1.4-.85-2.2-2.225T5 9.5q0-2.725 1.888-4.612T11.5 3q2.725 0 4.613 1.888T18 9.5q0 1.7-.8 3.075T15 14.8V17H8Z",
        )
    }

    /** `B-07` — die Uhr mit dem Rückwärtspfeil. */
    val Logbuch: ImageVector by lazy {
        gefuellt(
            "Logbuch",
            "M13 3q3.325 0 5.663 2.338T21 11q0 3.325-2.337 5.663T13 19q-2.075 0-3.85-1T6.2 15.25L4 17.45V11H2q0-4.575 3.213-7.787T13 0v3Zm0 2Q9.675 5 7.338 7.338T5 13h1.8l-.05 1.65 1.1-1.1.7 1.1Q9.35 15.8 10.5 16.4T13 17q2.5 0 4.25-1.75T19 11q0-2.5-1.75-4.25T13 5Zm-1 2v5l4.25 2.5 1-1.65-3.25-1.9V7h-2Z",
        )
    }

    /** Der Schnellschalter kündigt **Hell** an. */
    val Sonne: ImageVector by lazy {
        gefuellt(
            "Sonne",
            "M11 1h2v3h-2V1Zm0 19h2v3h-2v-3ZM3.5 4.9l1.4-1.4L7 5.6 5.6 7 3.5 4.9Zm13.4 13.5 1.4-1.4 2.1 2.1-1.4 1.4-2.1-2.1ZM1 11h3v2H1v-2Zm19 0h3v2h-3v-2ZM3.5 19.1l2.1-2.1L7 18.4l-2.1 2.1-1.4-1.4ZM17 5.6l2.1-2.1 1.4 1.4L18.4 7 17 5.6ZM12 7a5 5 0 1 1 0 10 5 5 0 0 1 0-10Zm0 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z",
        )
    }

    /** Der Schnellschalter kündigt **Dunkel** an. */
    val Mond: ImageVector by lazy {
        gefuellt("Mond", "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z")
    }

    /**
     * Der Schnellschalter kündigt **Wie das System** an: ein „A“ mit je drei seitlichen
     * Strahlen (F-26 Schritt 2). Als Strich gezeichnet, nicht als Fläche — sonst bliebe von
     * den Strahlen nur ein Splitter stehen.
     */
    val Automatik: ImageVector by lazy {
        gestrichelt(
            "Automatik", 1.8f,
            "M8.25 18 12 6l3.75 12", "M9.7 13.5h4.6",
            "M5.5 7.5 3.75 5.75", "M4.5 12H2", "M5.5 16.5l-1.75 1.75",
            "M18.5 7.5l1.75-1.75", "M19.5 12H22", "M18.5 16.5l1.75 1.75",
        )
    }
}
