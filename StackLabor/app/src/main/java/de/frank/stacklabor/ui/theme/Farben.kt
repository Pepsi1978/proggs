package de.frank.stacklabor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Die Farbrollen des Entwurfs.
 *
 * Alle Werte sind in `Specs/StackLabor/v2/messung/` gemessen und in
 * `02-UI-SPEC.md` §2 lesbar festgehalten. Sie werden hier NICHT gerundet und NICHT
 * auf Material-3-Rollen verbogen — das Design hat mehr Rollen, als M3 kennt
 * (Glasflaeche, Ampel-Textfarbe, zwei Loeslichkeits-Toene, Deaktiviert-Ton).
 */
@Immutable
data class StackFarben(
    val grund: Color,
    val flaeche: Color,
    val erhoeht: Color,
    val rand: Color,
    val text: Color,
    val textSchwach: Color,
    val akzent: Color,
    val akzent2: Color,
    val gruen: Color,
    val gelb: Color,
    val gelbText: Color,
    val rot: Color,
    val rotKraeftig: Color,
    val grau: Color,
    val loeslichWasser: Color,
    val loeslichFettFuellung: Color,
    val loeslichFettRand: Color,
    val deaktiviert: Color,
    val schatten: Color,
    val schatten2: Color,
    val glas: Color,
    val istHell: Boolean,
)

/** Helle Erscheinung — der Standard (Entscheidung 9 im Grilling). */
val FarbenHell = StackFarben(
    grund = Color(0xFFF5F7FA),
    flaeche = Color(0xFFFFFFFF),
    erhoeht = Color(0xFFF1F5F9),
    rand = Color(0xFFE2E8F0),
    text = Color(0xFF0F172A),
    textSchwach = Color(0xFF64748B),
    akzent = Color(0xFF4F46E5),
    akzent2 = Color(0xFF0EA5E9),
    gruen = Color(0xFF047857),
    gelb = Color(0xFFD97706),
    gelbText = Color(0xFFB45309),
    rot = Color(0xFFDC2626),
    rotKraeftig = Color(0xFFB91C1C),
    grau = Color(0xFF94A3B8),
    loeslichWasser = Color(0xFF059669),
    loeslichFettFuellung = Color(0xFFFFFFFF),
    // Ohne diesen Rand waere der fettloesliche Punkt auf weisser Karte unsichtbar.
    loeslichFettRand = Color(0xFF64748B),
    deaktiviert = Color(0xFFCBD5E1),
    schatten = Color(0x1A0F172A),   // rgba(15,23,42,.10)
    schatten2 = Color(0x380F172A),  // rgba(15,23,42,.22)
    glas = Color(0xB8F1F5F9),       // rgba(241,245,249,.72)
    istHell = true,
)

/** Dunkle Erscheinung — gleichwertig, ueber den Umschalter auf B-01 erreichbar. */
val FarbenDunkel = StackFarben(
    grund = Color(0xFF0B0E14),
    flaeche = Color(0xFF141A24),
    erhoeht = Color(0xFF1B2330),
    rand = Color(0xFF243040),
    text = Color(0xFFE6EAF2),
    textSchwach = Color(0xFF9AA6B8),
    akzent = Color(0xFF22D3EE),
    akzent2 = Color(0xFF0EA5E9),
    gruen = Color(0xFF34D399),
    gelb = Color(0xFFFBBF24),
    gelbText = Color(0xFFFBBF24),
    rot = Color(0xFFF87171),
    rotKraeftig = Color(0xFFF87171),
    grau = Color(0xFF64748B),
    loeslichWasser = Color(0xFF34D399),
    loeslichFettFuellung = Color(0xFFFFFFFF),
    // Auf dunklem Grund traegt der weisse Punkt sich selbst — kein Rand noetig.
    loeslichFettRand = Color.Transparent,
    deaktiviert = Color(0xFF334155),
    schatten = Color(0x73000000),   // rgba(0,0,0,.45)
    schatten2 = Color(0x99000000),  // rgba(0,0,0,.60)
    glas = Color(0xB81B2330),       // rgba(27,35,48,.72)
    istHell = false,
)

val LocalStackFarben = staticCompositionLocalOf { FarbenHell }
