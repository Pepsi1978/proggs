package de.frank.kompass

import de.frank.opencodekompass.BuildConfig

/**
 * Alles, worin sich die Kompass-Apps unterscheiden, ausser dem Abgleich (update/).
 *
 * Der gemeinsame Code in KompassKern kennt nur dieses Objekt. Jede App liefert ihre eigene
 * Fassung unter demselben Namen. Datenbank-, Ablage- und Sicherungsnamen muessen byte-gleich
 * mit den frueheren Fassungen bleiben, sonst sind die eigenen Fragen nach dem Update weg.
 *
 * Genau deshalb heissen Datenbank, Ablagen und die Paketkennung weiterhin "opencodekompass",
 * obwohl die App inzwischen "OCode Kompass" heisst: Der angezeigte Name ist frei, die Namen
 * der Ablagen sind es nicht. Eine Umbenennung dort waere ein stiller Datenverlust.
 */
object AppProfil {
    const val PRODUKT = "OCode Kompass"

    /**
     * Fruehere Namen dieser App.
     *
     * In jeder Sicherungsdatei steht, von welcher App sie stammt, und beim Einlesen wird das
     * geprueft. Ohne diese Liste haette die Umbenennung jede bis dahin geschriebene Sicherung
     * unlesbar gemacht — mit der Meldung, sie gehoere zu einer anderen App.
     */
    val FRUEHERE_NAMEN = setOf("OpenCode Kompass")

    /** Das dokumentierte Werkzeug — es heisst weiterhin OpenCode, nur die App wurde umbenannt. */
    const val WERKZEUG = "OpenCode CLI"
    const val KURZNAME = "OpenCode"
    const val HERSTELLER = "OpenCode"

    // Ablagenamen: unveraendert seit der ersten Fassung, siehe Klassenkommentar.
    const val DB_NAME = "opencode-kompass.db"
    const val OFFENE_ABLAGE = "opencode_kompass_prefs"
    const val GEHEIME_ABLAGE = "opencode_kompass_secure_prefs"

    const val DATEI_PRAEFIX = "ocode-kompass"

    /** Fruehere Dateinamen-Teile, damit bereits geschriebene Sicherungen gefunden werden. */
    val FRUEHERE_DATEI_PRAEFIXE = listOf("opencode-kompass")

    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_BUMPED_AT = BuildConfig.VERSION_BUMPED_AT
    const val SEEDED_CLI_VERSION = BuildConfig.SEEDED_CLI_VERSION
}
