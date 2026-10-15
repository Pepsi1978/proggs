package de.frank.entropyreducer.data.remote.oauth

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.data.remote.oauth.WhoopRefreshFehler.Art
import de.frank.entropyreducer.data.repository.WhoopRepository
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Netzfehler und Abruflimits dürfen nie als abgelaufene Anmeldung gelten (Auftrag 559QH).
 * Typ-/Code-Werte wie in net.openid.appauth.AuthorizationException.
 */
class WhoopRefreshFehlerTest {

    @Test
    fun `Verbindungsabbruch ist ein Netzfehler und wird wiederholt`() {
        // 15.10. 13:37:49: type=0 code=3 "Network error" (Software caused connection abort)
        assertThat(WhoopRefreshFehler.art(0, 3, null)).isEqualTo(Art.NETZ)
    }

    @Test
    fun `verbrauchter Refresh-Token verlangt neue Anmeldung`() {
        // 15.10. 15:41:05: type=2 code=2000 error=invalid_request
        assertThat(WhoopRefreshFehler.art(2, 2000, "invalid_request")).isEqualTo(Art.ANMELDUNG_UNGUELTIG)
        assertThat(WhoopRefreshFehler.art(2, 2002, "invalid_grant")).isEqualTo(Art.ANMELDUNG_UNGUELTIG)
        assertThat(WhoopRefreshFehler.art(2, 2001, "invalid_client")).isEqualTo(Art.ANMELDUNG_UNGUELTIG)
    }

    @Test
    fun `Serverfehler, unlesbare Antwort und unbekannte Fehler sind voruebergehend`() {
        assertThat(WhoopRefreshFehler.art(0, 4, null)).isEqualTo(Art.VORUEBERGEHEND) // SERVER_ERROR
        assertThat(WhoopRefreshFehler.art(0, 5, null)).isEqualTo(Art.VORUEBERGEHEND) // HTML statt JSON (429/503)
        assertThat(WhoopRefreshFehler.art(2, 2005, "server_error")).isEqualTo(Art.VORUEBERGEHEND)
        assertThat(WhoopRefreshFehler.art(2, 2005, "temporarily_unavailable")).isEqualTo(Art.VORUEBERGEHEND)
    }

    private fun http(code: Int, vararg header: Pair<String, String>): HttpException {
        val raw = okhttp3.Response.Builder()
            .code(code).message("x").protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://api.prod.whoop.com/").build())
            .apply { header.forEach { header(it.first, it.second) } }
            .build()
        return HttpException(Response.error<Any>("".toResponseBody(null), raw))
    }

    @Test
    fun `429 - Retry-After bzw X-RateLimit-Reset wird gelesen`() {
        assertThat(WhoopRepository.wartezeitMs(http(429, "Retry-After" to "120"))).isEqualTo(120_000L)
        assertThat(WhoopRepository.wartezeitMs(http(429, "X-RateLimit-Reset" to "30"))).isEqualTo(30_000L)
        assertThat(WhoopRepository.wartezeitMs(http(429))).isNull()
        assertThat(WhoopRepository.wartezeitMs(http(429, "Retry-After" to "Wed, 21 Oct 2026 07:28:00 GMT"))).isNull()
    }
}
