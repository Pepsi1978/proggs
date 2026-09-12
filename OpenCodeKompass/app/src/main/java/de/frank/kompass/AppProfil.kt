package de.frank.kompass

import de.frank.opencodekompass.BuildConfig

/**
 * Alles, worin sich die Kompass-Apps unterscheiden, ausser dem Abgleich (update/).
 *
 * Der gemeinsame Code in KompassKern kennt nur dieses Objekt. Jede App liefert ihre eigene
 * Fassung unter demselben Namen. Datenbank-, Ablage- und Sicherungsnamen muessen byte-gleich
 * mit den frueheren Fassungen bleiben, sonst sind die eigenen Fragen nach dem Update weg.
 */
object AppProfil {
    const val PRODUKT = "OpenCode Kompass"
    const val WERKZEUG = "OpenCode CLI"
    const val KURZNAME = "OpenCode"
    const val HERSTELLER = "OpenCode"
    const val DB_NAME = "opencode-kompass.db"
    const val OFFENE_ABLAGE = "opencode_kompass_prefs"
    const val GEHEIME_ABLAGE = "opencode_kompass_secure_prefs"
    const val DATEI_PRAEFIX = "opencode-kompass"
    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_BUMPED_AT = BuildConfig.VERSION_BUMPED_AT
    const val SEEDED_CLI_VERSION = BuildConfig.SEEDED_CLI_VERSION
}
