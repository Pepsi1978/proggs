package de.frank.experimente.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * ERZEUGT - die Symbole der unteren Leiste und des Erscheinungs-Schnellschalters, gezogen in
 * ihrer **gemessenen Reihenfolge** (nav[2]/a[0..4] bzw. header button[0]/svg[0..2]).
 *
 * Warum eigens neben `Symbole.kt`: der Symbol-Erzeuger benennt nach der naechstgelegenen
 * Beschriftung und fasst gleiche Pfade zusammen. Bei fuenf Feldern, die im Markup alle
 * "Hauptnavigation" heissen, verschiebt das die Zuordnung um eins - "Heute" trug dadurch das
 * Symbol des Nachbarn. Ueber die Stellung im Baum kann das nicht passieren.
 *
 * Quelle: `Specs/Experimente/v2/messung/`. Neu erzeugen: `scratchpad/leistensymbole.py`.
 */
private fun ausPfaden(name: String, breite: Float, hoehe: Float, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name, defaultWidth = breite.dp, defaultHeight = hoehe.dp,
        viewportWidth = breite, viewportHeight = hoehe,
    ).apply {
        pfade.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
    }.build()

object Leistensymbole {

    val Heute: ImageVector by lazy {
        ausPfaden(
            "Heute", 24f, 24f,
            "M5 22q-.825 0-1.412-.587T3 20V6q0-.825.588-1.412T5 4h1V2h2v2h8V2h2v2h1q.825 0 1.413.588T21 6v14q0 .825-.587 1.413T19 22H5Zm0-2h14V10H5v10Zm0-12h14V6H5v2Zm7 6q-.825 0-1.412-.587T10 12q0-.825.588-1.412T12 10q.825 0 1.413.588T14 12q0 .825-.587 1.413T12 14Z",
        )
    }

    val Ziele: ImageVector by lazy {
        ausPfaden(
            "Ziele", 24f, 24f,
            "M5 21V4h9l.4 2H20v10h-7l-.4-2H7v7H5Zm2-9h7.25l.4 2H18V8h-5.25l-.4-2H7v6Z",
        )
    }

    val Merkliste: ImageVector by lazy {
        ausPfaden(
            "Merkliste", 24f, 24f,
            "M7 3h10q.825 0 1.413.588T19 5v16l-7-3-7 3V5q0-.825.588-1.412T7 3Zm0 3v11.95l5-2.15 5 2.15V5H7Z",
        )
    }

    val Erkenntnisse: ImageVector by lazy {
        ausPfaden(
            "Erkenntnisse", 24f, 24f,
            "M9 21q-.425 0-.712-.288T8 20v-1h8v1q0 .425-.288.713T15 21H9Zm-1-4v-2.2q-1.4-.85-2.2-2.225T5 9.5q0-2.725 1.888-4.612T11.5 3q2.725 0 4.613 1.888T18 9.5q0 1.7-.8 3.075T15 14.8V17H8Zm2-2h3v-1.3l.95-.55q.95-.55 1.5-1.5T16 9.5q0-1.875-1.312-3.187T11.5 5Q9.625 5 8.313 6.313T7 9.5q0 1.2.55 2.15t1.5 1.5l.95.55V15Z",
        )
    }

    val Logbuch: ImageVector by lazy {
        ausPfaden(
            "Logbuch", 24f, 24f,
            "M13 3q3.325 0 5.663 2.338T21 11q0 3.325-2.337 5.663T13 19q-2.075 0-3.85-1T6.2 15.25L4 17.45V11H2q0-4.575 3.213-7.787T13 0v3Zm0 2Q9.675 5 7.338 7.338T5 13h1.8l-.05 1.65 1.1-1.1.7 1.1Q9.35 15.8 10.5 16.4T13 17q2.5 0 4.25-1.75T19 11q0-2.5-1.75-4.25T13 5Zm-1 2v5l4.25 2.5 1-1.65-3.25-1.9V7h-2Z",
        )
    }

    val Sonne: ImageVector by lazy {
        ausPfaden(
            "Sonne", 24f, 24f,
            "M11 1h2v3h-2V1Zm0 19h2v3h-2v-3ZM3.515 4.929l1.414-1.414L7.05 5.636 5.636 7.05 3.515 4.929Zm13.435 13.435 1.414-1.414 2.121 2.121-1.414 1.414-2.121-2.121ZM1 11h3v2H1v-2Zm19 0h3v2h-3v-2ZM3.515 19.071l2.121-2.121 1.414 1.414-2.121 2.121-1.414-1.414ZM16.95 5.636l2.121-2.121 1.414 1.414-2.121 2.121-1.414-1.414ZM12 7a5 5 0 1 1 0 10 5 5 0 0 1 0-10Zm0 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z",
        )
    }

    val Mond: ImageVector by lazy {
        ausPfaden(
            "Mond", 24f, 24f,
            "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z",
        )
    }

    val Automatik: ImageVector by lazy {
        ausPfaden(
            "Automatik", 24f, 24f,
            "M8.25 18 12 6l3.75 12M9.7 13.5h4.6M5.5 7.5 3.75 5.75M4.5 12H2M5.5 16.5l-1.75 1.75M18.5 7.5l1.75-1.75M19.5 12H22M18.5 16.5l1.75 1.75",
        )
    }
}
