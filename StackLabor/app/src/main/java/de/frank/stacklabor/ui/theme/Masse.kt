package de.frank.stacklabor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Die Masse des Entwurfs. Alle Werte sind gemessen (`02-UI-SPEC.md` §4).
 *
 * Zur Erinnerung: In der Messung sind die `stil`-Werte direkt dp; die `kasten`-Werte
 * sind doppelt, weil der Entwurf im Browser zweifach vergroessert dargestellt wird.
 */
object Masse {
    /** Seitlicher Bildschirmrand */
    val randSeitlich = 12.dp

    /** Breite einer Karte: 297 − 2 × 12 */
    val kartenBreite = 273.dp

    /** Innenabstand einer Karte */
    val kartenInnen = 12.dp

    /** Abstand zwischen Karten */
    val kartenAbstand = 8.dp

    /** Kopfleiste eines Unterbildschirms — Glasfläche mit 24 dp Weichzeichner */
    val kopfleiste = 57.dp

    /** Kopfbereich auf B-01 (mit Titelzeile und Dosis-Zeile) */
    val kopfB01 = 97.dp
    val titelzeileB01 = 52.dp
    val dosisZeileB01 = 26.dp

    /** Leiste „Alle Stacks zusammen pruefen" */
    val leisteGesamt = 49.dp

    /** Ziel-Streifen (zugeklappt) auf B-02 */
    val zielStreifen = 41.dp

    /** Sortier- und Suchleiste auf B-02 */
    val sortierleiste = 37.dp

    /** Fester Auswerten-Sockel unten auf B-02 */
    val sockel = 53.dp

    /** Verbleibende Listenhoehe auf B-02: 421 - 57 - 41 - 37 - 53 */
    val listeB02 = 233.dp

    /** Stack-Karte auf B-01 */
    val stackKarte = 78.dp

    /** Mittel-Eintrag; mit 1 dp Trenner ergibt das einen Takt von 57 dp */
    val mittelEintrag = 56.dp
    val mittelTakt = 57.dp

    /** Ziel-Eintrag */
    val zielEintrag = 40.dp

    /** Schwebender Plus-Knopf */
    val plusKnopf = 57.dp
    val plusRand = 16.dp

    /** Ampel-Kantenbalken links an jeder Zeile */
    val ampelBalken = 3.dp

    /** Loeslichkeits-Punkt */
    val loeslichPunkt = 8.dp
    val loeslichRand = 1.5.dp

    /** Zaehlpunkt auf der Stack-Karte */
    val zaehlPunkt = 6.dp

    /** Nummernkreis am Ziel */
    val nummernKreis = 20.dp

    /** Haekchen-Kaestchen */
    val haekchen = 22.dp

    /** Mindest-Tippflaeche — gilt fuer jedes Bedienelement */
    val tippflaeche = 44.dp

    /** Statusleiste und Gestenleiste des Geraets */
    val statusleiste = 24.dp
    val gestenleiste = 24.dp

    /** Hoehe der Ziel-Ueberlagerung auf B-02 (verdraengt die Liste nicht) */
    val zielUeberlagerungMax = 281.dp
}

/** Eckenradien je Bauteil (`02-UI-SPEC.md` §5). */
object Formen {
    val karte = RoundedCornerShape(12.dp)
    val knopf = RoundedCornerShape(12.dp)
    val feld = RoundedCornerShape(10.dp)
    val blatt = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    val chip = RoundedCornerShape(999.dp)
    val haekchen = RoundedCornerShape(6.dp)
    val geraet = RoundedCornerShape(24.dp)

    /** Der Ampel-Balken ist nur links gerundet. */
    val ampelBalken = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp)
}
