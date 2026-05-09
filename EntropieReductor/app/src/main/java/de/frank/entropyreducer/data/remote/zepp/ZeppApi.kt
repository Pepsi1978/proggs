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
     *
     * Rueckgabe als Response<ResponseBody> (raw bytes), damit das Repository
     * HTTP-Status (401/403/5xx) UND leere Bodies vor der Deserialisierung
     * pruefen kann. Der retrofit2-kotlinx-serialization-converter wirft sonst
     * eine JsonDecodingException SCHON beim Bauen der Response wenn der Body
     * leer ist (Issue #55).
     */
    @GET
    suspend fun bandData(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): Response<ResponseBody>

    /**
     * Workout-Liste (Sport-Sessions). Endpoint vermutet aus dem Reverse-Engineering;
     * kann Anpassung benoetigen wenn die API gewechselt hat.
     * Raw-Body aus dem gleichen Grund wie bei bandData.
     */
    @GET
    suspend fun workoutHistory(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): Response<ResponseBody>

    /**
     * Workout-Detail pro Trainings-ID. Verifizierter Endpoint aus
     * rolandsz/Mi-Fit-and-Zepp-workout-exporter:
     *   GET /v1/sport/run/detail.json?trackid=X&source=Y
     * Liefert GPS-Track (longitude_latitude), Pulsverlauf (heart_rate),
     * Pace pro km (kilo_pace), Splits (lap), Hoehenmeter-Stream (altitude)
     * — alles als pipe-separierte Strings die das Repository parst.
     * Raw-Body weil das Format viele Pipe-getrennte String-Felder hat.
     */
    @GET
    suspend fun workoutDetail(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): Response<ResponseBody>

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
    /**
     * Server liefert je nach Endpoint mal Int (z.B. -1) mal String — daher als
     * JsonElement-aequivalent String? halten und im Repository nicht weiter
     * verwenden ausser fuer Pagination (zur Zeit nicht implementiert).
     */
    @SerialName("next") val next: kotlinx.serialization.json.JsonElement? = null,
)

/**
 * Workout-Summary. ACHTUNG: Die Zepp-Cloud liefert numerische Felder
 * UNERWARTETERWEISE als JSON-Strings (z.B. `"dis":"7162.0"`, `"end_time":"1778334106"`).
 * Wir deklarieren daher ALLE potentiell-numerischen Felder als String? und
 * konvertieren im Repository defensiv mit toLongOrNull()/toDoubleOrNull()/...
 *
 * `source` ist im echten Server-JSON ein String wie "run.8716545.huami.com"
 * (Geraet-Identifier), keine numerische Sportart-Kennung — die Sportart
 * kommt im Feld `type`.
 */
@Serializable
data class ZeppWorkoutSummary(
    @SerialName("trackid") val trackId: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("dis") val distanceMeters: String? = null,
    @SerialName("calorie") val calories: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("run_time") val durationSeconds: String? = null,
    /** ACHTUNG: Zepp liefert Pace in Sekunden pro METER, nicht pro Kilometer!
     *  0.5042761 sec/m = 504 sec/km = 8:24 min/km. Repository multipliziert *1000. */
    @SerialName("avg_pace") val avgPace: String? = null,
    @SerialName("avg_speed") val avgSpeed: String? = null,
    @SerialName("max_pace") val maxPace: String? = null,
    @SerialName("min_pace") val minPace: String? = null,
    /** Korrekte Schluesselnamen aus Live-Sonde 2026-05-09 — die haengen "_heart_rate"
     *  Variante hinten dran, nicht "_hrm" wie ich anfangs vermutet hatte. */
    @SerialName("avg_heart_rate") val avgHr: String? = null,
    @SerialName("max_heart_rate") val maxHr: String? = null,
    /** Schrittfrequenz (Cadence) waehrend des Trainings — Schritte pro Minute. */
    @SerialName("avg_frequency") val avgFrequency: String? = null,
    @SerialName("max_frequency") val maxFrequency: String? = null,
    @SerialName("total_step") val totalSteps: String? = null,
    /** Schrittlaenge in cm. */
    @SerialName("avg_stride_length") val avgStrideLength: String? = null,
    @SerialName("altitude_ascend") val altitudeAscendMeters: String? = null,
    @SerialName("altitude_descend") val altitudeDescendMeters: String? = null,
    @SerialName("max_altitude") val maxAltitude: String? = null,
    @SerialName("min_altitude") val minAltitude: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("bind_device") val bindDevice: String? = null,
    /** Trainingseffekt aerob (0-50, dividiert durch 10 = Anzeige 0.0-5.0). */
    @SerialName("te") val trainingEffect: String? = null,
    /** Trainingseffekt anaerob (gleiche Skala). */
    @SerialName("anaerobic_te") val anaerobicTrainingEffect: String? = null,
    /** Schwimm-Spezifisch: SWOLF (niedriger = besser). */
    @SerialName("swolf") val swolf: String? = null,
    @SerialName("total_strokes") val totalStrokes: String? = null,
    @SerialName("swim_pool_length") val swimPoolLength: String? = null,
    @SerialName("swim_style") val swimStyle: String? = null,
    /** Etagen-Zahl. */
    @SerialName("floor_number") val floorNumber: String? = null,
    @SerialName("upstairs_height") val upstairsHeight: String? = null,
    /** SpO2-Werte waehrend des Workouts. */
    @SerialName("spo2_max") val spo2Max: String? = null,
    @SerialName("spo2_min") val spo2Min: String? = null,
    /** Pulswerte erweitert. */
    @SerialName("min_heart_rate") val minHr: String? = null,
    /** Auto-Recognition Flag (1 = Workout wurde automatisch erkannt). */
    @SerialName("auto_recognition") val autoRecognition: String? = null,
)

/* =================================== Workout-Detail-DTOs =================================== */

/**
 * Antwort des Workout-Detail-Endpoints. Felder enthalten pipe-separierte Strings
 * (z.B. heart_rate = "85|86|87|...|95"). Repository parst die Strings on-demand
 * und schreibt sie als JSON-Arrays in die WorkoutEntity.
 */
@Serializable
data class ZeppWorkoutDetailResponse(
    @SerialName("code") val code: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: ZeppWorkoutDetailData? = null,
)

@Serializable
data class ZeppWorkoutDetailData(
    @SerialName("trackid") val trackId: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("longitude_latitude") val longitudeLatitude: String? = null,
    @SerialName("altitude") val altitude: String? = null,
    @SerialName("accuracy") val accuracy: String? = null,
    @SerialName("time") val time: String? = null,
    @SerialName("gait") val gait: String? = null,
    @SerialName("pace") val pace: String? = null,
    @SerialName("pause") val pause: String? = null,
    @SerialName("spo2") val spo2: String? = null,
    @SerialName("flag") val flag: String? = null,
    @SerialName("kilo_pace") val kiloPace: String? = null,
    @SerialName("mile_pace") val milePace: String? = null,
    @SerialName("heart_rate") val heartRate: String? = null,
    @SerialName("speed") val speed: String? = null,
    @SerialName("bearing") val bearing: String? = null,
    @SerialName("distance") val distance: String? = null,
    @SerialName("lap") val lap: String? = null,
    @SerialName("air_pressure_altitude") val airPressureAltitude: String? = null,
    @SerialName("course") val course: String? = null,
    @SerialName("correct_altitude") val correctAltitude: String? = null,
    @SerialName("cadence") val cadence: String? = null,
    @SerialName("weather_info") val weatherInfo: String? = null,
)
