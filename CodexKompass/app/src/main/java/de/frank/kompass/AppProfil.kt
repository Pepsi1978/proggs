package de.frank.kompass

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.ui.graphics.vector.ImageVector
import de.frank.codexkompass.BuildConfig

/**
 * Alles, worin sich die beiden Kompass-Apps unterscheiden, ausser dem Abgleich (update/).
 *
 * Der übrige Code (seit 27.09.2026 eine eigene Kopie je App, früher der gemeinsame KompassKern)
 * liest die App-Eigenheiten aus diesem Objekt. Datenbank-, Ablage- und Sicherungsnamen muessen byte-gleich
 * mit den frueheren Fassungen bleiben, sonst sind die eigenen Fragen nach dem Update weg.
 */
object AppProfil {
    const val PRODUKT = "Codex Kompass"
    const val WERKZEUG = "Codex CLI"
    const val KURZNAME = "Codex"
    const val HERSTELLER = "OpenAI"
    const val DB_NAME = "codex-kompass.db"
    const val OFFENE_ABLAGE = "codex_kompass_prefs"
    const val GEHEIME_ABLAGE = "codex_kompass_secure_prefs"
    const val DATEI_PRAEFIX = "codex-kompass"
    /** Fruehere Namen dieser App — hier keine; die Liste haelt den Kern einheitlich. */
    val FRUEHERE_NAMEN = emptySet<String>()

    /** Fruehere Dateinamen-Teile — hier keine. */
    val FRUEHERE_DATEI_PRAEFIXE = emptyList<String>()

    /**
     * Der fünfte Reiter: was man in Codex über die Auswahlmenüs der Slash-Befehle einstellt
     * (`/model`, `/permissions`, `/experimental` …). Codex hat kein eigenes `/config`-Menü;
     * der Reiter „Config" daneben zeigt die Schlüssel der config.toml.
     */
    const val PANEL_TITEL = "Einstellen"
    const val PANEL_TITEL_LANG = "Einstell-Menüs"
    /** Wie ein einzelner Eintrag im Anweisungstext an das Modell heisst. */
    const val PANEL_ART_NAME = "Punkt aus einem Einstell-Menü"
    val PANEL_SYMBOL: ImageVector get() = Icons.Default.ToggleOn

    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_BUMPED_AT = BuildConfig.VERSION_BUMPED_AT
    const val SEEDED_CLI_VERSION = BuildConfig.SEEDED_CLI_VERSION
}
