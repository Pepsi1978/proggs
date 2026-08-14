package de.frank.stacklabor.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Die Bewegungen des Entwurfs — Dauern und Kurven exakt aus `03-MOTION-SPEC.md`,
 * jede auf einen gemessenen `@keyframes` bzw. eine `transition` zurueckfuehrbar.
 *
 * Eine `cubic-bezier` aus dem Entwurf wird NIE durch eine eingebaute Standardkurve
 * ersetzt — das waere eine andere Bewegung.
 */
object Kurven {
    /** cubic-bezier(0.2, 0, 0, 1) — die Leitkurve: Ausweichen, Umordnen, Bildschirmwechsel */
    val leit: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** cubic-bezier(0.4, 0, 0.2, 1) — Zustandswechsel: Ampeln, Flaechen, Erscheinung */
    val zustand: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** cubic-bezier(0.05, 0.7, 0.1, 1) — Blaetter und Vollbild von unten */
    val blatt: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** cubic-bezier(0.4, 0, 0.6, 1) — weiches Atmen (Dauerbewegung) */
    val atem: Easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

    /** cubic-bezier(0, 0, 0, 1) — der einmalige Puls an geaenderten Ampeln */
    val puls: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

    val linear: Easing = LinearEasing
}

/** Dauern in Millisekunden — gemessen. */
object Dauer {
    /** Ausweichen, Umordnen, Listen-Einblendung (`@keyframes rein`) */
    const val ANTWORT = 220

    /** Ampel-Ueberblendung (`transition: background-color .32s`) — M-07 */
    const val ZUSTAND = 320

    /** Bildschirmwechsel (`@keyframes vor` / `zurueck`) */
    const val WECHSEL = 300

    /** Blatt von unten (`@keyframes blatt`) — M-20 */
    const val BLATT = 300

    /** Vollbild von unten (`@keyframes hoch`) */
    const val VOLLBILD = 320

    /** Hell/Dunkel (`transition` auf background-color, color, border-color) — M-19 */
    const val ERSCHEINUNG = 420

    /** Aufnehmen beim Ziehen — M-01 */
    const val AUFNEHMEN = 140

    /** Langes Druecken, bis das Ziehen beginnt */
    const val LANG_DRUECKEN = 300L

    /** Nummern laufen live mit — M-03 */
    const val NUMMER = 120

    /** Einmaliger Puls an geaenderten Ampeln (`@keyframes puls`) — M-09 */
    const val PULS = 520

    /** Verbindungsfarbe in der Aufschluesselung — M-10 */
    const val VERBINDUNG_EIN = 180
    const val VERBINDUNG_HALT = 520
    const val VERBINDUNG_AUS = 400

    /** Warte-Schimmer (`@keyframes schimmer`) — M-11 */
    const val SCHIMMER = 1400

    /** Entsaettigtes Pulsieren waehrend der Auswertung (`@keyframes m12`) — M-12 */
    const val WARTE_PULS = 1600

    /** Streamender Text, je Wort — M-13 */
    const val WORT = 180
    const val WORT_ABSTAND = 60L
    const val ERZAEHLER_WECHSEL = 240

    /** Ziel-Ueberlagerung aufklappen — M-14 */
    const val AUFKLAPPEN = 380

    /** Begruendungszeile aufklappen */
    const val BEGRUENDUNG = 200

    /** Aura an roter Ampel (`@keyframes atem`) — M-21 */
    const val AURA = 2400

    /** Glanzkante (`@keyframes glanz`) — M-16 */
    const val GLANZ = 8000

    /** Plus-Knopf atmet (`@keyframes fabatem`) — M-16 */
    const val FAB_ATEM = 3200

    /** Kopfverlauf wandert (`@keyframes wandern`) — M-16 */
    const val WANDERN = 30_000

    /** Pulsring an der Wartezeile (`@keyframes ring`) — B-11 */
    const val RING = 1600

    /** Faltvorgang — M-17 */
    const val FALTEN = 400
    const val FALTEN_SPALTE = 300
    const val FALTEN_VERZUG = 100

    /** Rueckgaengig-Leiste bleibt sichtbar */
    const val RUECKGAENGIG_MS = 6000L

    /** Hinweis nach der Konkurrenzpruefung bleibt sichtbar */
    const val HINWEIS_MS = 20_000L

    /** Ruhefenster, bevor die Konkurrenzpruefung startet — F-02 */
    const val RUHEFENSTER_MS = 3000L

    /** Versatz je Zeile beim gestaffelten Einblenden — M-22 / M-08 */
    const val STAFFEL = 30L
    const val STAFFEL_MAX = 10
}

/** Fertige Spezifikationen, damit an der Aufrufstelle nichts neu erfunden wird. */
object Bewegungen {
    fun <T> antwort() = tween<T>(Dauer.ANTWORT, easing = Kurven.leit)
    fun <T> zustand() = tween<T>(Dauer.ZUSTAND, easing = Kurven.zustand)
    fun <T> wechsel() = tween<T>(Dauer.WECHSEL, easing = Kurven.leit)
    fun <T> blatt() = tween<T>(Dauer.BLATT, easing = Kurven.blatt)
    fun <T> vollbild() = tween<T>(Dauer.VOLLBILD, easing = Kurven.blatt)
    fun <T> erscheinung() = tween<T>(Dauer.ERSCHEINUNG, easing = Kurven.zustand)
    fun <T> aufnehmen() = tween<T>(Dauer.AUFNEHMEN, easing = Kurven.blatt)
    fun <T> aufklappen() = tween<T>(Dauer.AUFKLAPPEN, easing = Kurven.leit)

    /** Einrasten beim Loslassen — Feder, Daempfung 0,75 / Steifigkeit 380 (M-04) */
    fun <T> einrasten() = spring<T>(dampingRatio = 0.75f, stiffness = 380f)

    fun <T> sofort() = tween<T>(0)
}

/**
 * „Bewegung reduzieren".
 *
 * Wird sie gemeldet, bleiben genau die drei Bewegungen erhalten, die Bedeutung tragen
 * (Ampel-Ueberblendung, Ausweichen beim Ziehen, der streamende Text). Alles andere geht
 * auf 0 ms oder wird abgeschaltet — siehe `03-MOTION-SPEC.md` §8.
 */
val LocalBewegungReduziert = staticCompositionLocalOf { false }
