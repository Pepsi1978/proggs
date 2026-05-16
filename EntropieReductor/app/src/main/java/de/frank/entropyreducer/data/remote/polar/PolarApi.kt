package de.frank.entropyreducer.data.remote.polar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Polar AccessLink v3 — offizielle REST-API fuer Polar-Flow-Daten.
 *
 * Frank-Wunsch 2026-05-16: Nachdem die Strava-Integration aus der App rausgenommen
 * wurde (sie hat externe Brustgurt-HR-Daten verloren), wird Polar zur ALLEINIGEN
 * Workout-Quelle. Polar's API liefert HR-Samples vom gekoppelten H10-Brustgurt
 * automatisch mit — genau das was bei Strava+Zepp gefehlt hat.
 *
 * Authentifizierung: OAuth2 mit HTTP-Basic-Auth beim Token-Endpoint (kein
 * client_secret im Body wie bei Strava). Tokens leben ~1 Jahr (`expires_in:
 * 31535999`), KEIN Refresh-Token. Bei Ablauf: User muss einmal neu autorisieren.
 *
 * Rate-Limits (Single-User-App):
 *  - 500 Requests / 15 Minuten (plus 20 pro registriertem User)
 *  - 5000 Requests / Tag (plus 100 pro User)
 *  → Bei ~10 neuen Workouts/Woche und ~4 Calls pro Workout (Detail, Samples,
 *    HR-Zonen, GPX): weit unter dem Limit.
 *
 * Transaction-Workflow (KRITISCH — anders als Strava):
 *  1. POST /v3/users/{uid}/exercise-transactions — startet Transaction, liefert tid
 *     ODER 204 No Content wenn keine neuen Daten verfuegbar.
 *  2. GET /v3/users/{uid}/exercise-transactions/{tid} — Liste der Exercise-URLs.
 *  3. Fuer jede URL: GET, dann Samples + Heart-Rate-Zonen + ggf. GPX.
 *  4. PUT /v3/users/{uid}/exercise-transactions/{tid} — Transaction COMMITTEN.
 *     Ohne diesen Schritt kommen beim naechsten POST dieselben Eintraege wieder.
 *
 * URLs der Exercise-Detail-Endpoints kommen vom Server als vollqualifizierte
 * absolute URLs — wir nehmen `@Url` damit Retrofit sie direkt benutzen kann
 * statt die Pfade selbst zu konstruieren.
 */
interface PolarApi {

    /* ===================== User-Management ===================== */

    /**
     * Registriert den OAuth-authentifizierten Benutzer in Polar AccessLink.
     * Muss einmalig NACH erstem OAuth-Login aufgerufen werden.
     *
     * - 200: User neu angelegt, User-Objekt im Body
     * - 204: User war bereits registriert, kein Body (kein Fehler!)
     * - 409: Doppelte member-id
     */
    @POST("v3/users")
    suspend fun registerUser(
        @Header("Authorization") bearer: String,
        @Body request: PolarRegisterRequest,
    ): Response<PolarUser>

    /** Holt aktuelle User-Info (Gewicht, Groesse, Geburtsdatum etc.). */
    @GET("v3/users/{userId}")
    suspend fun getUser(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: Long,
    ): PolarUser

    /* ===================== Exercise-Transaction-Workflow ===================== */

    /**
     * Startet eine neue Exercise-Transaction.
     *  - 201: Transaction angelegt, Location-Header + Body mit transactionId
     *  - 204: Keine neuen Daten verfuegbar
     */
    @POST("v3/users/{userId}/exercise-transactions")
    suspend fun createExerciseTransaction(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: Long,
    ): Response<PolarTransactionLocation>

    /** Liste der Exercise-URLs innerhalb einer Transaction. */
    @GET("v3/users/{userId}/exercise-transactions/{transactionId}")
    suspend fun listExercisesInTransaction(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: Long,
        @Path("transactionId") transactionId: Long,
    ): PolarExerciseList

    /**
     * Detail einer einzelnen Trainingseinheit. URL kommt vom Server als
     * absolute URL — wir uebergeben sie via @Url.
     */
    @GET
    suspend fun getExercise(
        @Header("Authorization") bearer: String,
        @Url url: String,
    ): PolarExercise

    /**
     * Liste der verfuegbaren Sample-Streams (HR, Speed, Cadence, etc.) fuer
     * eine Exercise. Polar liefert pro Exercise unterschiedlich viele Streams —
     * je nach Geraet und Sport.
     */
    @GET
    suspend fun listSamples(
        @Header("Authorization") bearer: String,
        @Url url: String,
    ): PolarSamplesList

    /**
     * Einzelner Sample-Stream. Wichtig: `data` ist ein KOMMAGETRENNTER STRING,
     * kein JSON-Array. Der Mapper splittet ihn anhand `recording-rate` zu einem
     * Sample-Array mit Zeitstempel.
     */
    @GET
    suspend fun getSample(
        @Header("Authorization") bearer: String,
        @Url url: String,
    ): PolarSample

    /** HR-Zonen-Verteilung pro Exercise (Zeit in jeder Zone). */
    @GET
    suspend fun getHeartRateZones(
        @Header("Authorization") bearer: String,
        @Url url: String,
    ): Response<List<PolarHeartRateZone>>

    /**
     * GPX-XML der Strecke. Liefert Pause-Zeiten optional via
     * `?includePauseTimes=true`, das verschoben wir vorerst (Standard reicht).
     */
    @GET
    suspend fun getGpx(
        @Header("Authorization") bearer: String,
        @Url url: String,
    ): Response<okhttp3.ResponseBody>

    /**
     * Schliesst die Transaction ab. PFLICHT — sonst liefert der naechste
     * createExerciseTransaction die gleichen Eintraege.
     */
    @PUT("v3/users/{userId}/exercise-transactions/{transactionId}")
    suspend fun commitExerciseTransaction(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: Long,
        @Path("transactionId") transactionId: Long,
    ): Response<Unit>

    /**
     * Listen-Endpoint: liefert alle Exercises der letzten 30 Tage MIT
     * hashed IDs — OHNE Transaction-Workflow, OHNE Webhook noetig.
     *
     * Researcher-Finding 2026-05-16 (Polar Swagger): Dieser Endpoint ist der
     * fehlende Baustein. Polar's Direct-Read-Endpoints (`/v3/exercises/{id}`)
     * brauchen die **hashed** Exercise-ID — die numerische ID gibt 404. Aber
     * Polar's Transaction-Workflow liefert NUR die numerische ID. Dieser
     * Listen-Endpoint ist die Bruecke: er gibt fuer jede Exercise sowohl die
     * numerische id (im body) als auch die hashed id (in der URL) zurueck.
     *
     * Mit Query-Parameter `samples=true` werden die Sample-Stream-URLs gleich
     * mitgeliefert (spart einen Round-Trip). `route=true` aktiviert die GPX-URL.
     *
     * Scope: accesslink.read_all (haben wir via OAuth schon).
     * Limit: 30 Tage. Aelteres muss via Transaction-Workflow (committed = weg).
     */
    @GET("v3/exercises")
    suspend fun listExercisesLast30Days(
        @Header("Authorization") bearer: String,
        @retrofit2.http.Query("samples") includeSamples: Boolean = true,
        @retrofit2.http.Query("zones") includeZones: Boolean = true,
        @retrofit2.http.Query("route") includeRoute: Boolean = true,
    ): Response<List<PolarExerciseListItem>>

    /* ===================== Direct-Read-Endpoints (Webhook-Hash-ID) =====================
     *
     * Frank-Befund 2026-05-16 + Researcher-Finding: Polar AccessLink V3 hat
     * NEBEN dem Transaction-Workflow eine zweite Pfad-Klasse — Direct-Read-
     * Endpoints, die KEINE aktive Transaction brauchen. Sie sind im Polar-
     * Swagger-Spec dokumentiert (siehe https://www.polar.com/accesslink-api/swagger.yaml)
     * und werden vom Webhook-Payload-Feld `url` referenziert.
     *
     * Pfad: /v3/exercises/{id} (NICHT /v3/users/{uid}/exercises/{eid} — das gibt 404!)
     *
     * Die ID wird in der Doku als "hashed" bezeichnet ("aQlC83"), aber Polar
     * akzeptiert in vielen Faellen auch die numerische ID die unsere PolarExercise.id
     * liefert. Wir probieren beide Formen. Bei 404 schlucken wir den Fehler
     * gracefully und fallen auf den Transaction-Pfad zurueck.
     */

    /** Direct-Read der Exercise — funktioniert auch fuer committed Workouts. */
    @GET("v3/exercises/{exerciseId}")
    suspend fun getExerciseDirect(
        @Header("Authorization") bearer: String,
        @Path("exerciseId") exerciseId: String,
    ): Response<PolarExercise>

    /** Direct-Read der Sample-Liste. */
    @GET("v3/exercises/{exerciseId}/samples")
    suspend fun listSamplesDirect(
        @Header("Authorization") bearer: String,
        @Path("exerciseId") exerciseId: String,
    ): Response<PolarSamplesList>

    /** Direct-Read der HR-Zonen. */
    @GET("v3/exercises/{exerciseId}/heart-rate-zones")
    suspend fun getHeartRateZonesDirect(
        @Header("Authorization") bearer: String,
        @Path("exerciseId") exerciseId: String,
    ): Response<List<PolarHeartRateZone>>

    /** Direct-Read GPX (XML). */
    @GET("v3/exercises/{exerciseId}/gpx")
    suspend fun getGpxDirect(
        @Header("Authorization") bearer: String,
        @Path("exerciseId") exerciseId: String,
    ): Response<okhttp3.ResponseBody>

    /** Direct-Read TCX (XML, hat oft MEHR Daten als GPX — z.B. HR-Stream eingebettet). */
    @GET("v3/exercises/{exerciseId}/tcx")
    suspend fun getTcxDirect(
        @Header("Authorization") bearer: String,
        @Path("exerciseId") exerciseId: String,
    ): Response<okhttp3.ResponseBody>

    /** Direct-Read FIT (binary, vollstaendiger Stream-Datensatz). */
    @GET("v3/exercises/{exerciseId}/fit")
    suspend fun getFitDirect(
        @Header("Authorization") bearer: String,
        @Path("exerciseId") exerciseId: String,
    ): Response<okhttp3.ResponseBody>
}

/* ===================== DTOs ===================== */

@Serializable
data class PolarRegisterRequest(
    @SerialName("member-id") val memberId: String,
)

@Serializable
data class PolarUser(
    @SerialName("polar-user-id") val polarUserId: Long? = null,
    @SerialName("member-id") val memberId: String? = null,
    @SerialName("registration-date") val registrationDate: String? = null,
    @SerialName("first-name") val firstName: String? = null,
    @SerialName("last-name") val lastName: String? = null,
    val birthdate: String? = null,
    val gender: String? = null,
    /** Gewicht in kg. Live-Sonde 2026-05-16: Polar liefert Floats wie 88.2 */
    val weight: Float? = null,
    /** Groesse in cm. Live-Sonde 2026-05-16: Polar liefert Floats wie 178.0 —
     *  NICHT Int wie ich initial annahm. JSON-Parser warf "Unexpected symbol '.'
     *  in numeric literal" weil 178.0 nicht in Int passt. Float ist sicher. */
    val height: Float? = null,
)

/** Antwort auf POST /exercise-transactions — enthaelt die Transaction-ID. */
@Serializable
data class PolarTransactionLocation(
    @SerialName("transaction-id") val transactionId: Long? = null,
    @SerialName("resource-uri") val resourceUri: String? = null,
)

/** Liste der Exercise-URLs in einer Transaction. */
@Serializable
data class PolarExerciseList(
    val exercises: List<String> = emptyList(),
)

/**
 * Detail-Daten einer einzelnen Trainingseinheit.
 *
 * Wichtige Felder fuer die Recovery-Berechnung:
 *  - heart-rate.average / maximum: stammen vom externen Brustgurt wenn gekoppelt
 *  - training-load: Polar-eigene Belastungs-Metrik
 *  - training-load-pro.cardio-load / muscle-load: detailliertere Belastung
 *  - running-index: Laufeffizienz (entspricht VO2Max-Schaetzung)
 */
@Serializable
data class PolarExercise(
    val id: Long,
    @SerialName("upload-time") val uploadTime: String,
    @SerialName("polar-user") val polarUser: String,
    @SerialName("transaction-id") val transactionId: Long? = null,
    val device: String? = null,
    @SerialName("device-id") val deviceId: String? = null,
    /** Lokale Startzeit OHNE Zeitzone, z.B. "2024-11-15T10:40:02". */
    @SerialName("start-time") val startTime: String,
    /** UTC-Offset in MINUTEN (z.B. 60 fuer MEZ, 120 fuer MESZ). */
    @SerialName("start-time-utc-offset") val startTimeUtcOffset: Int,
    /** ISO-8601-Dauer, z.B. "PT1H30M45S". */
    val duration: String,
    val calories: Int? = null,
    /** Strecke in Metern. */
    val distance: Float? = null,
    @SerialName("heart-rate") val heartRate: PolarHeartRateSummary? = null,
    @SerialName("training-load") val trainingLoad: Double? = null,
    val sport: String? = null,
    @SerialName("has-route") val hasRoute: Boolean = false,
    @SerialName("club-id") val clubId: Int? = null,
    @SerialName("club-name") val clubName: String? = null,
    /** Detaillierterer Sport-String, z.B. "WATERSPORTS_WATERSKI". */
    @SerialName("detailed-sport-info") val detailedSportInfo: String? = null,
    @SerialName("fat-percentage") val fatPercentage: Int? = null,
    @SerialName("carbohydrate-percentage") val carbohydratePercentage: Int? = null,
    @SerialName("protein-percentage") val proteinPercentage: Int? = null,
    @SerialName("running-index") val runningIndex: Int? = null,
    @SerialName("training-load-pro") val trainingLoadPro: PolarTrainingLoadPro? = null,
)

@Serializable
data class PolarHeartRateSummary(
    val average: Int? = null,
    val maximum: Int? = null,
)

/**
 * Listen-Item aus `GET /v3/exercises`. Hat alle PolarExercise-Felder PLUS
 * eine `id` die NICHT der Long aus PolarExercise ist sondern die **hashed**
 * String-ID (z.B. "aQlC83"). Polar's Swagger nennt das Feld `id` in beiden
 * Faellen — wir mappen es hier separat damit der Typ als String bleibt.
 *
 * Optional sind `samples`, `heart-rate-zones`, `route` direkt eingebettet
 * wenn man die entsprechenden Query-Parameter setzt.
 */
@Serializable
data class PolarExerciseListItem(
    /** Hashed ID (string) — fuer Direct-Read-Endpoints. */
    val id: String,
    @SerialName("upload-time") val uploadTime: String? = null,
    @SerialName("polar-user") val polarUser: String? = null,
    @SerialName("transaction-id") val transactionId: Long? = null,
    val device: String? = null,
    @SerialName("device-id") val deviceId: String? = null,
    @SerialName("start-time") val startTime: String? = null,
    @SerialName("start-time-utc-offset") val startTimeUtcOffset: Int? = null,
    val duration: String? = null,
    val calories: Int? = null,
    val distance: Float? = null,
    @SerialName("heart-rate") val heartRate: PolarHeartRateSummary? = null,
    @SerialName("training-load") val trainingLoad: Double? = null,
    val sport: String? = null,
    @SerialName("has-route") val hasRoute: Boolean = false,
    @SerialName("club-id") val clubId: Int? = null,
    @SerialName("club-name") val clubName: String? = null,
    @SerialName("detailed-sport-info") val detailedSportInfo: String? = null,
    @SerialName("fat-percentage") val fatPercentage: Int? = null,
    @SerialName("carbohydrate-percentage") val carbohydratePercentage: Int? = null,
    @SerialName("protein-percentage") val proteinPercentage: Int? = null,
    @SerialName("running-index") val runningIndex: Int? = null,
    @SerialName("training-load-pro") val trainingLoadPro: PolarTrainingLoadPro? = null,
    /**
     * Numerische Exercise-ID. Polar's Listen-Endpoint zeigt sie NICHT direkt
     * an — die hashed `id` ist die einzige ID. Die numerische ID muessen wir
     * ueber den `polar-user`-Pfad oder die `start-time` matchen.
     *
     * Wenn Polar in einer zukuenftigen API-Version doch eine numerische ID
     * im Listen-Item liefert, kommt sie hier rein.
     */
    @SerialName("exercise-id") val exerciseId: Long? = null,
)

@Serializable
data class PolarTrainingLoadPro(
    val date: String? = null,
    @SerialName("cardio-load") val cardioLoad: Double? = null,
    @SerialName("muscle-load") val muscleLoad: Double? = null,
    @SerialName("perceived-load") val perceivedLoad: Double? = null,
    @SerialName("cardio-load-interpretation") val cardioLoadInterpretation: String? = null,
    @SerialName("muscle-load-interpretation") val muscleLoadInterpretation: String? = null,
    @SerialName("perceived-load-interpretation") val perceivedLoadInterpretation: String? = null,
    @SerialName("user-rpe") val userRpe: String? = null,
)

@Serializable
data class PolarHeartRateZone(
    val index: Int,
    @SerialName("lower-limit") val lowerLimit: Int? = null,
    @SerialName("upper-limit") val upperLimit: Int? = null,
    /** ISO-8601-Dauer in der Zone, z.B. "PT1H2M30S". */
    @SerialName("in-zone") val inZone: String? = null,
)

/** Liste der Sample-Stream-URLs. */
@Serializable
data class PolarSamplesList(
    val samples: List<String> = emptyList(),
)

/**
 * Ein einzelner Sample-Stream.
 *
 * STOLPERFALLE: `data` ist ein kommagetrennter String, KEIN JSON-Array.
 * Beispiel: `"0,100,102,97,97,101,103"` — pro Wert ein Sample.
 * `recording-rate` = Sekunden zwischen Samples (z.B. 1 oder 5).
 * `sample-type` = numerische ID als String, siehe PolarSampleType.
 */
@Serializable
data class PolarSample(
    @SerialName("recording-rate") val recordingRate: Int,
    @SerialName("sample-type") val sampleType: String,
    val data: String,
)

/**
 * Sample-Typ-IDs aus der offiziellen Polar AccessLink V3 Doku
 * (https://www.polar.com/accesslink-api/#samples).
 *
 * KRITISCH (Frank-Befund 2026-05-16): Die alten IDs 5-9 waren falsch — der
 * Bulk-Export nutzt String-Identifier ("DISTANCE", "RUN_CADENCE"), die REST-
 * API aber numerische Codes. Die Reihenfolge ist nicht konsekutiv 0..9
 * sondern hat Luecken (POWER_PEDALING_INDEX, POWER_LR_BALANCE, etc.). Frueher
 * wurde z.B. Run-Cadence als "6" gesucht und dabei stattdessen POWER_LR_BALANCE
 * eingelesen, Distance als "8" hat in Wirklichkeit RUN_CADENCE getroffen.
 */
object PolarSampleType {
    const val HEART_RATE = "0"               // bpm
    const val SPEED = "1"                    // km/h (Frank-Live-Sonde 2026-05-16)
    const val CADENCE = "2"                  // rpm — Cycling-Pedaltritte
    const val ALTITUDE = "3"                 // m
    const val POWER = "4"                    // W
    const val POWER_PEDALING_INDEX = "5"     // % (Cycling)
    const val POWER_LR_BALANCE = "6"         // % (Cycling)
    const val AIR_PRESSURE = "7"             // hPa
    const val RUN_CADENCE = "8"              // Schritte/min — KORRIGIERT von "6"
    const val TEMPERATURE = "9"              // C — KORRIGIERT von "7"
    const val DISTANCE = "10"                // m (kumuliert) — KORRIGIERT von "8"
    const val RR_INTERVAL = "11"             // ms — KORRIGIERT von "9"
    // Stride-Length ist NICHT als API-Sample verfuegbar — wird aus
    // distance/(run_cadence_avg * duration/60) berechnet.
}
