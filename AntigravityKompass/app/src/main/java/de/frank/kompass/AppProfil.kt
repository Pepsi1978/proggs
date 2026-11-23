package de.frank.kompass

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.ui.graphics.vector.ImageVector
import de.frank.antigravitykompass.BuildConfig

/**
 * Alles, worin sich die beiden Kompass-Apps unterscheiden, ausser dem Abgleich (update/).
 *
 * Der übrige Code (seit 27.09.2026 eine eigene Kopie je App, früher der gemeinsame KompassKern)
 * liest die App-Eigenheiten aus diesem Objekt. Datenbank-, Ablage- und Sicherungsnamen muessen byte-gleich
 * mit den frueheren Fassungen bleiben, sonst sind die eigenen Fragen nach dem Update weg.
 */
object AppProfil {
    const val PRODUKT = "Antigravity Kompass"
    const val WERKZEUG = "Antigravity CLI"
    const val KURZNAME = "Antigravity"
    const val HERSTELLER = "Google"
    const val DB_NAME = "antigravity-kompass.db"
    const val OFFENE_ABLAGE = "antigravity_kompass_prefs"
    const val GEHEIME_ABLAGE = "antigravity_kompass_secure_prefs"
    const val DATEI_PRAEFIX = "antigravity-kompass"
    /** Fruehere Namen dieser App — hier keine; die Liste haelt den Kern einheitlich. */
    val FRUEHERE_NAMEN = emptySet<String>()

    /** Fruehere Dateinamen-Teile — hier keine. */
    val FRUEHERE_DATEI_PRAEFIXE = emptyList<String>()

    /**
     * Der fünfte Reiter: das interaktive Einstellungs-Menü, das `/config` bzw. `/settings` in Antigravity CLI öffnet.
     * Der Reiter „Config" daneben zeigt dagegen die Schlüssel aus ~/.gemini/antigravity-cli/settings.json.
     */
    const val PANEL_TITEL = "/config"
    const val PANEL_TITEL_LANG = "Settings Editor"
    /** Wie ein einzelner Eintrag im Anweisungstext an das Modell heisst. */
    const val PANEL_ART_NAME = "Punkt aus dem Settings Editor (/config bzw. /settings)"
    /** Fassung, aus der die mitgelieferte Menüliste stammt; die Ernte liest nur Neueres. */
    const val PANEL_STAND = "1.5.15"
    val PANEL_SYMBOL: ImageVector get() = Icons.Default.ToggleOn

    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_BUMPED_AT = BuildConfig.VERSION_BUMPED_AT
    const val SEEDED_CLI_VERSION = BuildConfig.SEEDED_CLI_VERSION
}
