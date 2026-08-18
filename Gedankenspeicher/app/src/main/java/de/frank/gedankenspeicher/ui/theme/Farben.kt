package de.frank.gedankenspeicher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * **Die vier Erscheinungen aus `02-UI-SPEC.md` §2 — und zwar alle vier gleichrangig.**
 *
 * Keine ist eine abgedunkelte Kopie einer anderen: jede trägt ihre eigenen dreizehn Werte.
 * Deshalb steht hier eine eigene Farbrollen-Klasse statt Material-`ColorScheme` allein —
 * `kiKarte`, `kiKarteRand`, `hintergrundGlas` und `akzentGedeckt` haben in Material kein
 * Gegenstück, und ohne sie ließe sich der Entwurf nicht bauen.
 */
@Immutable
data class Farbrollen(
    val hintergrund: Color,
    val hintergrundErhoben: Color,
    val hintergrundGlas: Color,
    val rand: Color,
    val akzent: Color,
    val akzentGedeckt: Color,
    val textStark: Color,
    val textMittel: Color,
    val textSchwach: Color,
    val fehler: Color,
    val erfolg: Color,
    val kiKarte: Color,
    val kiKarteRand: Color,
    val istDunkel: Boolean,
)

/** §2.1 — die Vorbelegung. */
val GoldDunkel = Farbrollen(
    hintergrund = Color(0xFF0D0B07),
    hintergrundErhoben = Color(0xFF17140D),
    hintergrundGlas = Color(0xB817140D),
    rand = Color(0x38C9A227),
    akzent = Color(0xFFC9A227),
    akzentGedeckt = Color(0x24C9A227),
    textStark = Color(0xFFF4EFE2),
    textMittel = Color(0xFFB8AE97),
    textSchwach = Color(0xFF77705F),
    fehler = Color(0xFFE0645C),
    erfolg = Color(0xFF7FB069),
    kiKarte = Color(0xFF1C1710),
    kiKarteRand = Color(0x73C9A227),
    istDunkel = true,
)

/** §2.2 */
val GoldHell = Farbrollen(
    hintergrund = Color(0xFFFBF7EE),
    hintergrundErhoben = Color(0xFFFFFFFF),
    hintergrundGlas = Color(0xC7FBF7EE),
    rand = Color(0x33A67C00),
    akzent = Color(0xFFA67C00),
    akzentGedeckt = Color(0x1FA67C00),
    textStark = Color(0xFF231E14),
    textMittel = Color(0xFF5E5647),
    textSchwach = Color(0xFF938A76),
    fehler = Color(0xFFB3261E),
    erfolg = Color(0xFF4C7A34),
    kiKarte = Color(0xFFFDF9F0),
    kiKarteRand = Color(0x6BA67C00),
    istDunkel = false,
)

/** §2.3 */
val Dunkel = Farbrollen(
    hintergrund = Color(0xFF0B0C0E),
    hintergrundErhoben = Color(0xFF16181C),
    hintergrundGlas = Color(0xB816181C),
    rand = Color(0x1AFFFFFF),
    akzent = Color(0xFF3B82F6),
    akzentGedeckt = Color(0x293B82F6),
    textStark = Color(0xFFECEFF4),
    textMittel = Color(0xFFA8AFBA),
    textSchwach = Color(0xFF6B7280),
    fehler = Color(0xFFEF4444),
    erfolg = Color(0xFF22C55E),
    kiKarte = Color(0xFF111820),
    kiKarteRand = Color(0x733B82F6),
    istDunkel = true,
)

/** §2.4 */
val Hell = Farbrollen(
    hintergrund = Color(0xFFF6F7F9),
    hintergrundErhoben = Color(0xFFFFFFFF),
    hintergrundGlas = Color(0xC7F6F7F9),
    rand = Color(0x1A111827),
    akzent = Color(0xFF2563EB),
    akzentGedeckt = Color(0x1A2563EB),
    textStark = Color(0xFF111827),
    textMittel = Color(0xFF4B5563),
    textSchwach = Color(0xFF9CA3AF),
    fehler = Color(0xFFDC2626),
    erfolg = Color(0xFF16A34A),
    kiKarte = Color(0xFFF8FAFF),
    kiKarteRand = Color(0x662563EB),
    istDunkel = false,
)

/** Die vier Erscheinungen, wie sie in B-04 zur Wahl stehen (F-15). */
enum class Erscheinung(val id: String, val label: String, val farben: Farbrollen) {
    HELL("hell", "Hell", Hell),
    DUNKEL("dunkel", "Dunkel", Dunkel),
    GOLD_HELL("goldHell", "Gold-Hell", GoldHell),
    GOLD_DUNKEL("goldDunkel", "Gold-Dunkel", GoldDunkel),
    ;

    companion object {
        /** Unbekanntes fällt auf die Vorbelegung zurück, statt die App scheitern zu lassen. */
        fun vonId(wert: String): Erscheinung = entries.firstOrNull { it.id == wert } ?: GOLD_DUNKEL
    }
}

val LocalFarben = staticCompositionLocalOf { GoldDunkel }
