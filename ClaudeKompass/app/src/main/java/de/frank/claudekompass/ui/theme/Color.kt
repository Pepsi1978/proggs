package de.frank.claudekompass.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Die Gold-Palette aus der Android-App-Referenz (Baustein A), Wert für Wert übernommen.
 *
 * Beide Modi sind vollständig durchgezeichnet — es gibt hier keine „schnelle Variante".
 * Material You (Dynamic Color) bleibt bewusst aus: Gold ist die Leitfarbe der App und darf
 * nicht vom Systemhintergrund überschrieben werden.
 */

// --- Dunkelmodus -------------------------------------------------------------------------
val DunkelHintergrund = Color(0xFF121212)
val DunkelFlaeche = Color(0xFF181818)
val DunkelFlaecheErhoeht = Color(0xFF282828)
val DunkelGold = Color(0xFFE3B341)
val DunkelGoldGedaempft = Color(0xFFC9922B)
val DunkelAufGold = Color(0xFF1A1408)
val DunkelKupfer = Color(0xFFC25E00)
val DunkelText = Color(0xFFEDE7DA)
val DunkelTextGedaempft = Color(0xFFA79C86)
val DunkelRahmen = Color(0xFF2C2620)
val DunkelEingabefeld = Color(0xFF141414)

// --- Hellmodus ---------------------------------------------------------------------------
val HellHintergrund = Color(0xFFFAF7F0)
val HellFlaeche = Color(0xFFFFFFFF)
val HellFlaecheErhoeht = Color(0xFFF4EFE3)
val HellGold = Color(0xFF8B6914)
val HellGoldGedaempft = Color(0xFFA9812A)
val HellAufGold = Color(0xFFFFFFFF)
val HellKupfer = Color(0xFFA34F00)
val HellText = Color(0xFF1B1710)
val HellTextGedaempft = Color(0xFF6B6151)
val HellRahmen = Color(0xFFE6DFCF)
val HellEingabefeld = Color(0xFFF7F3EA)

// --- Bedeutungsfarben, in beiden Modi gleich ---------------------------------------------
// Gold bleibt der Marke vorbehalten und wird hier bewusst nicht verwendet.
val Erfolg = Color(0xFF4CAF7D)
val Warnung = Color(0xFFFFB300)
val Fehler = Color(0xFFFF5252)
val Info = Color(0xFF4ECDC4)
