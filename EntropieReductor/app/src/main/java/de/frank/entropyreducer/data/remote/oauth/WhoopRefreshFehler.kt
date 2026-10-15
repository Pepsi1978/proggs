package de.frank.entropyreducer.data.remote.oauth

/**
 * Ordnet einen gescheiterten Whoop-Token-Refresh ein (2026-10-15). Reine Logik ohne AppAuth-Typen,
 * damit sie in Unit-Tests geprüft werden kann. Die Zahlen sind die Typ-Konstanten von
 * net.openid.appauth.AuthorizationException.
 *
 * Nur eine ausdrückliche Absage von Whoop zu genau diesem Refresh-Token zählt als „neu anmelden“.
 * Netzfehler, Serverfehler, unlesbare Antworten (etwa eine HTML-Seite bei 429 oder 503) und unbekannte
 * OAuth-Fehler sind vorübergehend: Die Anmeldung bleibt gespeichert, der nächste Versuch kommt später.
 */
object WhoopRefreshFehler {

    enum class Art {
        /** Keine Antwort von Whoop (Verbindung abgerissen, kein Netz, Zeitüberschreitung). Sofort wiederholbar. */
        NETZ,

        /** Whoop lehnt den Refresh-Token ab — nur eine neue Anmeldung hilft. */
        ANMELDUNG_UNGUELTIG,

        /** Whoop hat geantwortet, aber nicht endgültig (Serverfehler, Abruflimit, unbekannter Fehler). */
        VORUEBERGEHEND,
    }

    const val TYPE_GENERAL_ERROR = 0
    const val TYPE_OAUTH_TOKEN_ERROR = 2

    /** AppAuth GeneralErrors: 3 = NETWORK_ERROR, 4 = SERVER_ERROR, 5 = JSON_DESERIALIZATION_ERROR. */
    const val CODE_NETWORK_ERROR = 3

    /** RFC 6749 §5.2: Diese Fehler betreffen den Refresh-Token bzw. den Client selbst. */
    private val ENDGUELTIG = setOf("invalid_grant", "invalid_request", "invalid_client", "unauthorized_client")

    fun art(type: Int, code: Int, error: String?): Art = when {
        type == TYPE_GENERAL_ERROR && code == CODE_NETWORK_ERROR -> Art.NETZ
        type == TYPE_OAUTH_TOKEN_ERROR && error?.lowercase() in ENDGUELTIG -> Art.ANMELDUNG_UNGUELTIG
        else -> Art.VORUEBERGEHEND
    }
}
