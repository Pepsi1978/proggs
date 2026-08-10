package de.frank.experimente.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Die Symbole des Entwurfs — erzeugt aus der Messung, nicht von Hand abgetippt und nicht
 * aus einer Standardsammlung ersetzt. Wer hier etwas aendert, aendert es an der falschen
 * Stelle: Quelle ist `Specs/<App>/v2/messung/`.
 *
 * Neu erzeugen: `design-umsetzer/references/symbole-erzeugen.ps1`
 */
private fun ausPfaden(name: String, breite: Float, hoehe: Float, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = breite.dp, defaultHeight = hoehe.dp,
        viewportWidth = breite, viewportHeight = hoehe,
    ).apply {
        pfade.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
    }.build()

object Symbole {

    /** Hellmodus einschalten */
    val HellmodusEinschalten: ImageVector by lazy {
        ausPfaden(
            "HellmodusEinschalten", 24f, 24f,
            "M11 1h2v3h-2V1Zm0 19h2v3h-2v-3ZM3.515 4.929l1.414-1.414L7.05 5.636 5.636 7.05 3.515 4.929Zm13.435 13.435 1.414-1.414 2.121 2.121-1.414 1.414-2.121-2.121ZM1 11h3v2H1v-2Zm19 0h3v2h-3v-2ZM3.515 19.071l2.121-2.121 1.414 1.414-2.121 2.121-1.414-1.414ZM16.95 5.636l2.121-2.121 1.414 1.414-2.121 2.121-1.414-1.414ZM12 7a5 5 0 1 1 0 10 5 5 0 0 1 0-10Zm0 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z",
        )
    }

    /** Hellmodus einschalten */
    val HellmodusEinschalten2: ImageVector by lazy {
        ausPfaden(
            "HellmodusEinschalten2", 24f, 24f,
            "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z",
        )
    }

    /** Hellmodus einschalten */
    val HellmodusEinschalten3: ImageVector by lazy {
        ausPfaden(
            "HellmodusEinschalten3", 24f, 24f,
            "M8.25 18 12 6l3.75 12M9.7 13.5h4.6M5.5 7.5 3.75 5.75M4.5 12H2M5.5 16.5l-1.75 1.75M18.5 7.5l1.75-1.75M19.5 12H22M18.5 16.5l1.75 1.75",
        )
    }

    /** Einstellungen */
    val Einstellungen: ImageVector by lazy {
        ausPfaden(
            "Einstellungen", 24f, 24f,
            "M19.14 12.94c.04-.31.06-.63.06-.94s-.02-.63-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96a7.13 7.13 0 0 0-1.62-.94L14.4 2.81A.48.48 0 0 0 13.92 2h-3.84a.48.48 0 0 0-.48.41l-.36 2.54c-.59.24-1.13.56-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.72 8.47a.48.48 0 0 0 .12.61l2.03 1.58c-.05.31-.09.65-.09.98s.03.66.08.98l-2.02 1.57a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.38.31.59.22l2.39-.96c.49.38 1.03.7 1.62.94l.36 2.54c.04.23.24.41.48.41h3.84c.24 0 .44-.18.48-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.09.47 0 .59-.22l1.92-3.32a.49.49 0 0 0-.12-.61l-2.02-1.57ZM12 15.5A3.5 3.5 0 1 1 12 8a3.5 3.5 0 0 1 0 7.5Z",
        )
    }

    /** Lage einsprechen */
    val LageEinsprechen: ImageVector by lazy {
        ausPfaden(
            "LageEinsprechen", 24f, 24f,
            "M12 14q-1.25 0-2.125-.875T9 11V5q0-1.25.875-2.125T12 2q1.25 0 2.125.875T15 5v6q0 1.25-.875 2.125T12 14Zm-1 7v-3.075q-2.6-.35-4.3-2.325T5 11h2q0 2.075 1.463 3.538T12 16q2.075 0 3.538-1.462T17 11h2q0 2.6-1.7 4.575T13 17.925V21Z",
        )
    }

    /** Auf die Merkliste legen */
    val AufDieMerklisteLegen: ImageVector by lazy {
        ausPfaden(
            "AufDieMerklisteLegen", 24f, 24f,
            "M7 3h10q.825 0 1.413.588T19 5v16l-7-3-7 3V5q0-.825.588-1.412T7 3Zm0 3v11.95l5-2.15 5 2.15V5H7Z",
        )
    }

    /** Von der Merkliste nehmen */
    val VonDerMerklisteNehmen: ImageVector by lazy {
        ausPfaden(
            "VonDerMerklisteNehmen", 24f, 24f,
            "M5 21V5q0-.825.588-1.412T7 3h10q.825 0 1.413.588T19 5v16l-7-3Z",
        )
    }

    /** Heute */
    val Heute: ImageVector by lazy {
        ausPfaden(
            "Heute", 24f, 24f,
            "M12 20q-3.35 0-5.675-2.325T4 12q0-3.35 2.325-5.675T12 4q1.725 0 3.3.713T18 6.7V4h2v7h-7V9h3.8q-.95-1.4-2.15-2.2T12 6Q9.5 6 7.75 7.75T6 12q0 2.5 1.75 4.25T12 18q1.925 0 3.475-1.1T17.65 14H19.7q-.7 2.65-2.85 4.325T12 20Z",
        )
    }

    /** Gespräch zum Experiment */
    val GespraechZumExperiment: ImageVector by lazy {
        ausPfaden(
            "GespraechZumExperiment", 24f, 24f,
            "M4 20q-.825 0-1.412-.588T2 18V6q0-.825.588-1.412T4 4h16q.825 0 1.413.588T22 6v12q0 .825-.587 1.412T20 20H7l-5 4V6h2v13.125L6.3 18H20V6H4v14Zm4-7h8v-2H8v2Zm0-3h8V8H8v2Zm0 6h5v-2H8v2Z",
        )
    }

    /** Heute */
    val Heute2: ImageVector by lazy {
        ausPfaden(
            "Heute2", 24f, 24f,
            "M5 21q-.825 0-1.412-.587T3 19V5q0-.825.588-1.412T5 3h14q.825 0 1.413.588T21 5v14q0 .825-.587 1.413T19 21H5Zm0-2h14V5H5v14Z",
        )
    }

    /** Hauptnavigation */
    val Hauptnavigation: ImageVector by lazy {
        ausPfaden(
            "Hauptnavigation", 24f, 24f,
            "M5 22q-.825 0-1.412-.587T3 20V6q0-.825.588-1.412T5 4h1V2h2v2h8V2h2v2h1q.825 0 1.413.588T21 6v14q0 .825-.587 1.413T19 22H5Zm0-2h14V10H5v10Zm0-12h14V6H5v2Zm7 6q-.825 0-1.412-.587T10 12q0-.825.588-1.412T12 10q.825 0 1.413.588T14 12q0 .825-.587 1.413T12 14Z",
        )
    }

    /** Hauptnavigation */
    val Hauptnavigation2: ImageVector by lazy {
        ausPfaden(
            "Hauptnavigation2", 24f, 24f,
            "M5 21V4h9l.4 2H20v10h-7l-.4-2H7v7H5Zm2-9h7.25l.4 2H18V8h-5.25l-.4-2H7v6Z",
        )
    }

    /** Hauptnavigation */
    val Hauptnavigation3: ImageVector by lazy {
        ausPfaden(
            "Hauptnavigation3", 24f, 24f,
            "M9 21q-.425 0-.712-.288T8 20v-1h8v1q0 .425-.288.713T15 21H9Zm-1-4v-2.2q-1.4-.85-2.2-2.225T5 9.5q0-2.725 1.888-4.612T11.5 3q2.725 0 4.613 1.888T18 9.5q0 1.7-.8 3.075T15 14.8V17H8Zm2-2h3v-1.3l.95-.55q.95-.55 1.5-1.5T16 9.5q0-1.875-1.312-3.187T11.5 5Q9.625 5 8.313 6.313T7 9.5q0 1.2.55 2.15t1.5 1.5l.95.55V15Z",
        )
    }

    /** Hauptnavigation */
    val Hauptnavigation4: ImageVector by lazy {
        ausPfaden(
            "Hauptnavigation4", 24f, 24f,
            "M13 3q3.325 0 5.663 2.338T21 11q0 3.325-2.337 5.663T13 19q-2.075 0-3.85-1T6.2 15.25L4 17.45V11H2q0-4.575 3.213-7.787T13 0v3Zm0 2Q9.675 5 7.338 7.338T5 13h1.8l-.05 1.65 1.1-1.1.7 1.1Q9.35 15.8 10.5 16.4T13 17q2.5 0 4.25-1.75T19 11q0-2.5-1.75-4.25T13 5Zm-1 2v5l4.25 2.5 1-1.65-3.25-1.9V7h-2Z",
        )
    }

    /** Zurück zu Heute */
    val ZurueckZuHeute: ImageVector by lazy {
        ausPfaden(
            "ZurueckZuHeute", 24f, 24f,
            "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.42-1.41L7.83 13H20v-2Z",
        )
    }

    /** Gespräch aufnehmen */
    val GespraechAufnehmen: ImageVector by lazy {
        ausPfaden(
            "GespraechAufnehmen", 24f, 24f,
            "M12 14q-1.25 0-2.125-.875T9 11V5q0-1.25.875-2.125T12 2q1.25 0 2.125.875T15 5v6q0 1.25-.875 2.125T12 14Zm-1 7v-3.075q-2.6-.35-4.3-2.325T5 11h2q0 2.075 1.463 3.538T12 16q2.075 0 3.538-1.462T17 11h2q0 2.6-1.7 4.6T13 17.925V21h-2Z",
        )
    }

    /** Auswertung einsprechen */
    val AuswertungEinsprechen: ImageVector by lazy {
        ausPfaden(
            "AuswertungEinsprechen", 24f, 24f,
            "M12 14q1.25 0 2.125-.875T15 11V5q0-1.25-.875-2.125T12 2 9.875 2.875 9 5v6q0 1.25.875 2.125T12 14Zm-1 7v-3.075q-2.6-.35-4.3-2.325T5 11h2q0 2.075 1.463 3.538T12 16q2.075 0 3.538-1.463T17 11h2q0 2.6-1.7 4.575T13 17.925V21h-2Z",
        )
    }

    /** Symbol */
    val Symbol: ImageVector by lazy {
        ausPfaden(
            "Symbol", 24f, 24f,
            "m19 9-1.25-2.75L15 5l2.75-1.25L19 1l1.25 2.75L23 5l-2.75 1.25L19 9Zm-7 12-2.5-5.5L4 13l5.5-2.5L12 5l2.5 5.5L20 13l-5.5 2.5L12 21ZM5 8 3.75 5.25 1 4l2.75-1.25L5 0l1.25 2.75L9 4 6.25 5.25 5 8Z",
        )
    }

    /** Auswertung vorlesen */
    val AuswertungVorlesen: ImageVector by lazy {
        ausPfaden(
            "AuswertungVorlesen", 24f, 24f,
            "M3 15V9h4l5-5v16l-5-5H3Zm11.5 1.5v-2q1.05-.475 1.775-1.375T17 11q0-1.225-.725-2.125T14.5 7.5v-2q1.9.55 3.2 2.075T19 11q0 1.9-1.3 3.425T14.5 16.5Zm0 4.1v-2.05q2.75-.625 4.625-2.75T21 11q0-2.675-1.875-4.8T14.5 3.45V1.4q3.6.65 6.05 3.35T23 11q0 3.55-2.45 6.25T14.5 20.6Z",
        )
    }

    /** Symbol */
    val Symbol2: ImageVector by lazy {
        ausPfaden(
            "Symbol2", 960f, 960f,
            "M480-160 160-480l320-320 57 56-224 224h487v80H313l224 224-57 56Z",
        )
    }

    /** Zurück zu Einstellungen */
    val ZurueckZuEinstellungen: ImageVector by lazy {
        ausPfaden(
            "ZurueckZuEinstellungen", 24f, 24f,
            "M20 11H7.83l5.59-5.59c.39-.39.39-1.03 0-1.42-.39-.39-1.03-.39-1.42 0l-7.29 7.3c-.39.39-.39 1.02 0 1.41L12 20c.39.39 1.03.39 1.42 0 .39-.39.39-1.03 0-1.42L7.83 13H20c.55 0 1-.45 1-1s-.45-1-1-1z",
        )
    }

    /** Selbstbild einsprechen */
    val SelbstbildEinsprechen: ImageVector by lazy {
        ausPfaden(
            "SelbstbildEinsprechen", 24f, 24f,
            "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z",
        )
    }

    /** Selbstbild einsprechen */
    val SelbstbildEinsprechen2: ImageVector by lazy {
        ausPfaden(
            "SelbstbildEinsprechen2", 24f, 24f,
            "M8 19q-.825 0-1.413-.588T6 17V7q0-.825.587-1.413T8 5h8q.825 0 1.413.587T18 7v10q0 .825-.587 1.412T16 19H8Z",
        )
    }

    /** Eigenes Experiment einsprechen */
    val EigenesExperimentEinsprechen: ImageVector by lazy {
        ausPfaden(
            "EigenesExperimentEinsprechen", 24f, 24f,
            "M12 14q-1.25 0-2.125-.875T9 11V5q0-1.25.875-2.125T12 2q1.25 0 2.125.875T15 5v6q0 1.25-.875 2.125T12 14Zm-1 7v-3.075q-2.6-.35-4.3-2.325Q5 13.625 5 11h2q0 2.075 1.463 3.538Q9.925 16 12 16t3.538-1.462Q17 13.075 17 11h2q0 2.625-1.7 4.6-1.7 1.975-4.3 2.325V21h-2Z",
        )
    }

    /** Eigenes Experiment einsprechen */
    val EigenesExperimentEinsprechen2: ImageVector by lazy {
        ausPfaden(
            "EigenesExperimentEinsprechen2", 24f, 24f,
            "M7 17V7h10v10H7Z",
        )
    }

    /** Eigenes Experiment anlegen */
    val EigenesExperimentAnlegen: ImageVector by lazy {
        ausPfaden(
            "EigenesExperimentAnlegen", 24f, 24f,
            "M11 13H5v-2h6V5h2v6h6v2h-6v6h-2v-6Z",
        )
    }

    /** Ziel anlegen */
    val ZielAnlegen: ImageVector by lazy {
        ausPfaden(
            "ZielAnlegen", 24f, 24f,
            "M11 19v-6H5v-2h6V5h2v6h6v2h-6v6Z",
        )
    }
}
