package de.frank.kompass

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.ui.graphics.vector.ImageVector
import de.frank.kimikompass.BuildConfig

/**
 * Alles, worin sich die beiden Kompass-Apps unterscheiden, ausser dem Abgleich (update/).
 *
 * Der übrige Code (seit 27.09.2026 eine eigene Kopie je App, früher der gemeinsame KompassKern)
 * liest die App-Eigenheiten aus diesem Objekt. Datenbank-, Ablage- und Sicherungsnamen muessen byte-gleich
 * mit den frueheren Fassungen bleiben, sonst sind die eigenen Fragen nach dem Update weg.
 */
object AppProfil {
    const val PRODUKT = "Kimi Kompass"
    const val WERKZEUG = "Kimi Code CLI"
    const val KURZNAME = "Kimi"
    const val HERSTELLER = "Moonshot AI"
    const val DB_NAME = "kimi-kompass.db"
    const val OFFENE_ABLAGE = "kimi_kompass_prefs"
    const val GEHEIME_ABLAGE = "kimi_kompass_secure_prefs"
    const val DATEI_PRAEFIX = "kimi-kompass"
    /** Fruehere Namen dieser App — hier keine; die Liste haelt den Kern einheitlich. */
    val FRUEHERE_NAMEN = emptySet<String>()

    /** Fruehere Dateinamen-Teile — hier keine. */
    val FRUEHERE_DATEI_PRAEFIXE = emptyList<String>()

    /**
     * Der fünfte Reiter: das interaktive Einstellungs-Menü, das `/settings` bzw. `/config` in Kimi Code CLI öffnet.
     * Der Reiter „Config" daneben zeigt dagegen die Schlüssel aus config.toml und tui.toml.
     */
    const val PANEL_TITEL = "/config"
    const val PANEL_TITEL_LANG = "Settings-Panel"
    /** Wie ein einzelner Eintrag im Anweisungstext an das Modell heisst. */
    const val PANEL_ART_NAME = "Punkt aus dem Settings-Panel (/settings bzw. /config)"
    /** Fassung, aus der die mitgelieferte Menüliste stammt; die Ernte liest nur Neueres. */
    const val PANEL_STAND = "2.1.1"
    val PANEL_SYMBOL: ImageVector get() = Icons.Default.ToggleOn

    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_BUMPED_AT = BuildConfig.VERSION_BUMPED_AT
    const val SEEDED_CLI_VERSION = BuildConfig.SEEDED_CLI_VERSION
}
