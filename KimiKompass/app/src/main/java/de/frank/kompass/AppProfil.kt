package de.frank.kompass

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.ui.graphics.vector.ImageVector
import de.frank.claudekompass.BuildConfig

/**
 * Alles, worin sich die beiden Kompass-Apps unterscheiden, ausser dem Abgleich (update/).
 *
 * Der übrige Code (seit 27.09.2026 eine eigene Kopie je App, früher der gemeinsame KompassKern)
 * liest die App-Eigenheiten aus diesem Objekt. Datenbank-, Ablage- und Sicherungsnamen muessen byte-gleich
 * mit den frueheren Fassungen bleiben, sonst sind die eigenen Fragen nach dem Update weg.
 */
object AppProfil {
    const val PRODUKT = "Claude Kompass"
    const val WERKZEUG = "Claude Code"
    const val KURZNAME = "Claude"
    const val HERSTELLER = "Anthropic"
    const val DB_NAME = "claude-kompass.db"
    const val OFFENE_ABLAGE = "claude_kompass_prefs"
    const val GEHEIME_ABLAGE = "claude_kompass_secure_prefs"
    const val DATEI_PRAEFIX = "claude-kompass"
    /** Fruehere Namen dieser App — hier keine; die Liste haelt den Kern einheitlich. */
    val FRUEHERE_NAMEN = emptySet<String>()

    /** Fruehere Dateinamen-Teile — hier keine. */
    val FRUEHERE_DATEI_PRAEFIXE = emptyList<String>()

    /**
     * Der fünfte Reiter: das interaktive Einstellungs-Menü, das `/config` in Claude Code öffnet.
     * Der Reiter „Config" daneben zeigt dagegen die Schlüssel der settings.json.
     */
    const val PANEL_TITEL = "/config"
    const val PANEL_TITEL_LANG = "/config-Menü"
    /** Wie ein einzelner Eintrag im Anweisungstext an das Modell heisst. */
    const val PANEL_ART_NAME = "Punkt aus dem /config-Menü"
    /** Fassung, aus der die mitgelieferte Menüliste stammt; die Ernte liest nur Neueres. */
    const val PANEL_STAND = "2.1.285"
    val PANEL_SYMBOL: ImageVector get() = Icons.Default.ToggleOn

    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_BUMPED_AT = BuildConfig.VERSION_BUMPED_AT
    const val SEEDED_CLI_VERSION = BuildConfig.SEEDED_CLI_VERSION
}
