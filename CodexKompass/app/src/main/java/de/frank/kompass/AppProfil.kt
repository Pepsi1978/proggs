package de.frank.kompass

import de.frank.codexkompass.BuildConfig

/**
 * Alles, worin sich die beiden Kompass-Apps unterscheiden, ausser dem Abgleich (update/).
 *
 * Der gemeinsame Code in KompassKern kennt nur dieses Objekt. Jede App liefert ihre eigene
 * Fassung unter demselben Namen. Datenbank-, Ablage- und Sicherungsnamen muessen byte-gleich
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

    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_BUMPED_AT = BuildConfig.VERSION_BUMPED_AT
    const val SEEDED_CLI_VERSION = BuildConfig.SEEDED_CLI_VERSION
}
