package de.frank.entropyreducer.data.remote.zepp

/**
 * Zentrale Konstanten und Helfer fuer Zepp-Endpoints.
 *
 * Die Zepp-Cloud betreibt mehrere regionale Subdomains. Frank ist in Deutschland,
 * deshalb ist der Default "de2" (Europa). Die Region wird in den Settings gespeichert
 * und kann bei Login-Fehlschlaegen automatisch auf "us2" (USA) umgestellt werden.
 *
 * App-Versionsnummern und Geraete-Strings imitieren die offizielle Zepp-Android-App
 * (Version 9.12.5 mit Build 151689). Die Werte stammen aus dem Open-Source-Projekt
 * argrento/huami-token und sind 1:1 uebernommen, damit Zepp uns als legitime App
 * akzeptiert.
 */
internal object ZeppEndpoints {

    // App-Identitaet imitiert offizielle Zepp-Android-App.
    const val APP_VERSION = "9.12.5"
    const val APP_BUILD = "151689"
    const val USER_AGENT = "Zepp/9.12.5 (Pixel 4; Android 12; Density/2.75)"
    const val APP_NAME = "com.huami.midong"
    const val CHANNEL = "a100900101016"

    // URL-Templates — {region} wird zur Laufzeit ersetzt (de2/us2).
    fun loginStep1Url(region: String): String =
        "https://api-user-$region.zepp.com/v2/registrations/tokens"

    fun loginStep2Url(region: String): String =
        "https://api-mifit-$region.zepp.com/v2/client/login"

    fun logoutUrl(region: String): String =
        "https://api-mifit-$region.zepp.com/v1/client/logout"

    /** Daily-Data-Endpoint (Schritte, Schlaf, HR-Summary). */
    fun bandDataUrl(region: String): String =
        "https://api-mifit-$region.zepp.com/v1/data/band_data.json"

    /** Workout-History (Sport-Sessions). Vorlaeufige URL — kann Anpassung brauchen. */
    fun sportHistoryUrl(region: String): String =
        "https://api-mifit-$region.zepp.com/v1/sport/run/history.json"

    /** Workout-Detail (GPS-Track + HR-Verlauf). Endpoint zur Laufzeit zu klaeren. */
    fun sportDetailUrl(region: String, trackId: String): String =
        "https://api-mifit-$region.zepp.com/v1/sport/run/detail.json?trackid=$trackId"

    /** PAI-Score. */
    fun paiUrl(region: String): String =
        "https://api-mifit-$region.zepp.com/v1/data/pai_data.json"

    /** Stress-Score. */
    fun stressUrl(region: String): String =
        "https://api-mifit-$region.zepp.com/v1/data/stress_data.json"

    /** SpO2 (Blutsauerstoff). */
    fun spo2Url(region: String): String =
        "https://api-mifit-$region.zepp.com/v1/data/blood_oxygen_data.json"

    /** Devices — regionsneutral. */
    const val DEVICES_BASE = "https://api-mifit.zepp.com/"

    /**
     * Standard-Header fuer Daten-Requests (nach erfolgreichem Login).
     * `apptoken`, `x-request-id` und Timezone werden pro Aufruf gesetzt.
     */
    fun dataHeaders(
        appToken: String,
        requestId: String,
        countryCode: String = "DE",
        timezone: String = "Europe/Berlin",
        language: String = "de_DE",
    ): Map<String, String> = mapOf(
        "hm-privacy-diagnostics" to "false",
        "country" to countryCode,
        "appplatform" to "android_phone",
        "hm-privacy-ceip" to "true",
        "x-request-id" to requestId,
        "timezone" to timezone,
        "channel" to CHANNEL,
        "vb" to "202509151347",
        "cv" to "${APP_BUILD}_$APP_VERSION",
        "appname" to APP_NAME,
        "v" to "2.0",
        "vn" to APP_VERSION,
        "apptoken" to appToken,
        "lang" to language,
        "user-agent" to USER_AGENT,
        "accept-encoding" to "gzip",
    )

    /** Header fuer Login-Schritt 1 (Token-Tausch mit verschluesseltem Body). */
    fun step1Headers(): Map<String, String> = mapOf(
        "app_name" to APP_NAME,
        "appname" to APP_NAME,
        "cv" to "${APP_BUILD}_$APP_VERSION",
        "v" to "2.0",
        "appplatform" to "android_phone",
        "vb" to "202509151347",
        "vn" to APP_VERSION,
        "user-agent" to USER_AGENT,
        "x-hm-ekv" to "1",
        "content-type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "accept-encoding" to "gzip",
    )

    /** Header fuer Login-Schritt 2 (Access-Token gegen App-Token tauschen). */
    fun step2Headers(): Map<String, String> = mapOf(
        "app_name" to "com.huami.webapp",
        "appname" to "com.huami.webapp",
        "origin" to "https://user.zepp.com",
        "referer" to "https://user.zepp.com/",
        "user-agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0",
        "content-type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "accept" to "application/json, text/plain, */*",
        "accept-language" to "de-DE,de;q=0.9,en;q=0.5",
    )
}
