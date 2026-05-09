package de.frank.entropyreducer.data.remote.zepp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * Retrofit-Interface fuer die inoffizielle Zepp-Cloud-API.
 *
 * Die Zepp-API ist nicht offiziell dokumentiert — die Endpoints stammen aus dem
 * Open-Source-Projekt argrento/huami-token (Login-Flow) und dem Reverse-Engineering-
 * Bericht von bentasker.co.uk (Daten-Endpoints).
 *
 * Wichtig: Region-spezifisch! Frank ist in Deutschland → "de2"-Subdomain.
 *   - api-user-de2.zepp.com    fuer Login Schritt 1 (Token-Tausch)
 *   - api-mifit-de2.zepp.com   fuer Login Schritt 2 + Daten-Endpoints
 *   - api-mifit.zepp.com       fuer Devices und User-Endpoints (regionsneutral)
 *
 * Wir nutzen pro Aufruf die volle URL ueber `@Url`, damit dieselbe Retrofit-Instanz
 * mit unterschiedlichen Hosts arbeiten kann ohne mehrere Retrofit-Builder.
 */
interface ZeppApi {

    /**
     * Login-Schritt 1: Verschluesselten Body absenden, erwartet Status 303 mit
     * Location-Header der Refresh- + Access-Token enthaelt.
     *
     * Der Body ist KEIN Form-Encoded-Body, sondern AES-CBC-verschluesselte Bytes —
     * deshalb `@Body RequestBody` mit selbst gesetztem Content-Type.
     */
    @POST
    suspend fun loginStep1(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body encryptedBody: RequestBody,
    ): Response<ResponseBody>

    /**
     * Login-Schritt 2: Access-Token gegen App-Token + Login-Token + User-ID tauschen.
     * Form-Encoded, nicht verschluesselt.
     */
    @FormUrlEncoded
    @POST
    suspend fun loginStep2(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @FieldMap fields: Map<String, String>,
    ): ZeppLoginResponse

    /** Logout. */
    @FormUrlEncoded
    @POST
    suspend fun logout(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @FieldMap fields: Map<String, String>,
    ): Response<ResponseBody>

    /**
     * Tagessummary (Schritte, Schlafzeiten, Ruhepuls). Liefert je Tag einen Eintrag
     * mit base64-kodiertem Summary-JSON. Auswertung im Repository.
     */
    @GET
    suspend fun bandData(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): ZeppBandDataResponse

    /**
     * Workout-Liste (Sport-Sessions). Endpoint vermutet aus dem Reverse-Engineering;
     * kann Anpassung benoetigen wenn die API gewechselt hat.
     */
    @GET
    suspend fun workoutHistory(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): ZeppWorkoutHistoryResponse

    /** Generischer GET fuer noch nicht typisierte Endpoints (PAI, BioCharge, Stress). */
    @GET
    suspend fun rawGet(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): Response<ResponseBody>

    /** Geraete-Liste (zur Bestaetigung dass Login funktioniert hat + um die T-Rex 3 zu finden). */
    @GET("users/{user_id}/devices")
    suspend fun listDevices(
        @Path("user_id") userId: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): ZeppDevicesResponse
}

/* =================================== Login-DTOs =================================== */

@Serializable
data class ZeppLoginResponse(
    @SerialName("token_info") val tokenInfo: ZeppTokenInfo? = null,
    @SerialName("result") val result: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
)

@Serializable
data class ZeppTokenInfo(
    @SerialName("login_token") val loginToken: String? = null,
    @SerialName("app_token") val appToken: String? = null,
    @SerialName("user_id") val userId: String? = null,
)

/* =================================== Devices-DTOs =================================== */

@Serializable
data class ZeppDevicesResponse(
    @SerialName("items") val items: List<ZeppDevice> = emptyList(),
)

@Serializable
data class ZeppDevice(
    @SerialName("deviceid") val deviceId: String? = null,
    @SerialName("mac") val mac: String? = null,
    @SerialName("active") val active: Boolean? = null,
    @SerialName("source") val source: Int? = null,
    @SerialName("device_type") val deviceType: Int? = null,
    @SerialName("device_source") val deviceSource: Int? = null,
)

/* =================================== Daily-Data-DTOs =================================== */

/**
 * Antwort des Endpoints `/v1/data/band_data.json`. Liefert pro Tag einen Eintrag
 * mit `summary` als base64-kodiertem JSON-String. Im Detail-Modus zusaetzlich
 * `data_hr` (binaer, 1440 Java-Shorts pro Minute).
 */
@Serializable
data class ZeppBandDataResponse(
    @SerialName("code") val code: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: List<ZeppBandDataDay> = emptyList(),
    @SerialName("next") val next: String? = null,
)

@Serializable
data class ZeppBandDataDay(
    @SerialName("uid") val uid: String? = null,
    @SerialName("data_type") val dataType: Int? = null,
    @SerialName("date_time") val date: String? = null,
    @SerialName("source") val source: Int? = null,
    /** Base64-kodiertes JSON mit Schritte-/Schlaf-Daten. */
    @SerialName("summary") val summary: String? = null,
    /** Detail-Modus: Base64-kodierte Bytes (1440 Java-Shorts pro Tag). */
    @SerialName("data_hr") val dataHr: String? = null,
)

/* =================================== Workout-DTOs =================================== */

/**
 * Antwort des Workout-History-Endpoints. ACHTUNG: Diese Struktur ist eine
 * vorlaeufige Annahme aus dem Community-Wissen — beim ersten Live-Sync MUSS sie
 * gegen die echte Antwort verglichen und ggf. angepasst werden. Die Detail-Felder
 * pro Workout (GPS-Track, Pace-Splits, HR-Verlauf) werden separat geholt sobald
 * der Detail-Endpoint identifiziert ist.
 */
@Serializable
data class ZeppWorkoutHistoryResponse(
    @SerialName("code") val code: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: ZeppWorkoutData? = null,
)

@Serializable
data class ZeppWorkoutData(
    @SerialName("summary") val summary: List<ZeppWorkoutSummary> = emptyList(),
    @SerialName("next") val next: String? = null,
)

@Serializable
data class ZeppWorkoutSummary(
    @SerialName("trackid") val trackId: String? = null,
    @SerialName("source") val source: Int? = null,
    @SerialName("type") val type: Int? = null, // 1=Lauf, 6=Rad, 8=Wandern, 9=Schwimm-Pool, ...
    @SerialName("dis") val distanceMeters: Double? = null,
    @SerialName("calorie") val calories: Double? = null,
    @SerialName("end_time") val endTime: Long? = null,
    @SerialName("run_time") val durationSeconds: Long? = null,
    @SerialName("avg_pace") val avgPace: Double? = null,
    @SerialName("avg_speed") val avgSpeed: Double? = null,
    @SerialName("max_pace") val maxPace: Double? = null,
    @SerialName("avg_hrm") val avgHr: Int? = null,
    @SerialName("max_hrm") val maxHr: Int? = null,
)
