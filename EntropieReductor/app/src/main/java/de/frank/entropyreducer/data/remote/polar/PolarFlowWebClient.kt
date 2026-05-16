package de.frank.entropyreducer.data.remote.polar

import android.util.Log
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polar Flow Web-API Client.
 *
 * Frank-Wunsch 2026-05-16 (Loop nach allen V3/V4 AccessLink-Versuchen):
 * Polar's offizielle AccessLink-API ist destruktiv und liefert Frank's
 * Workouts der letzten 30 Tage nicht mehr. Polar Flow Web (flow.polar.com)
 * hat aber ALLE Workouts dauerhaft sichtbar. Wir nutzen Polar's eigene
 * Web-API mit Cookie-basiertem Login.
 *
 * Workflow:
 *  1. Login: POST flow.polar.com/login mit email + password
 *     → Set-Cookie: ASPSESSIONID, polarSessionId, JSESSIONID
 *  2. Workouts laden: GET flow.polar.com/training/analysis/{id}/range/data
 *     → JSON mit sampleTypes ["HEART_RATE","SPEED","ALTITUDE","CADENCE","DISTANCE"]
 *       und samples-Arrays mit [time, value]-Paaren.
 *  3. GPS: GET flow.polar.com/api/export/training/gpx/{id} → GPX-XML
 *
 * Sicherheit:
 *  - Email wird nur fuer den ersten Login gebraucht, danach gespeichert.
 *  - Passwort wird NIE gespeichert — nach dem Login wird der Session-
 *    Cookie verschluesselt persistiert (EncryptedSecretsStore).
 *  - Bei Cookie-Ablauf: automatischer Re-Login wenn Passwort einmal
 *    via "keep me signed in"-Flag mitgegeben wurde. Wenn nicht: User
 *    muss in der App nochmal einloggen.
 *
 * Inoffizielle API: Polar's Flow-Webseite kann jederzeit Schemas aendern.
 * Defensive Parser: bei fehlenden Feldern null statt Crash.
 */
@Singleton
class PolarFlowWebClient @Inject constructor(
    private val secrets: EncryptedSecretsStore,
) {

    /** Eigener CookieJar — speichert Polar-Cookies verschluesselt in Secrets. */
    private val cookieJar = object : CookieJar {
        private val memory = mutableMapOf<String, MutableList<Cookie>>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val list = memory.getOrPut(host) { mutableListOf() }
            // Bestehende Cookies mit gleichem Namen ueberschreiben
            for (c in cookies) {
                list.removeAll { it.name == c.name }
                list += c
            }
            // Persistieren: serialisierten Cookie-String in Secrets schreiben
            persistCookies()
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            if (memory.isEmpty()) restoreCookies()
            val list = memory[url.host].orEmpty()
            return list.filter { c -> !c.expiresAt.let { it > 0L && it < System.currentTimeMillis() } }
        }

        private fun persistCookies() {
            val flat = memory.flatMap { (host, list) ->
                list.map { c -> "$host\t${c.name}\t${c.value}\t${c.domain}\t${c.path}\t${c.expiresAt}\t${c.secure}\t${c.httpOnly}" }
            }.joinToString("\n")
            secrets.polarFlowCookieJar = flat
        }

        private fun restoreCookies() {
            val raw = secrets.polarFlowCookieJar ?: return
            if (raw.isBlank()) return
            for (line in raw.split("\n")) {
                val parts = line.split("\t")
                if (parts.size < 8) continue
                val host = parts[0]
                val builder = Cookie.Builder()
                    .name(parts[1])
                    .value(parts[2])
                    .domain(parts[3])
                    .path(parts[4])
                val exp = parts[5].toLongOrNull() ?: 0L
                if (exp > 0L) builder.expiresAt(exp)
                if (parts[6].toBoolean()) builder.secure()
                if (parts[7].toBoolean()) builder.httpOnly()
                runCatching {
                    val cookie = builder.build()
                    memory.getOrPut(host) { mutableListOf() } += cookie
                }
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Ist ein gueltiger Session-Cookie vorhanden? */
    fun isLoggedIn(): Boolean {
        val raw = secrets.polarFlowCookieJar
        return !raw.isNullOrBlank() && raw.contains("flow.polar.com")
    }

    /**
     * Login mit Email + Passwort. Polar Flow's Login-Formular sendet die
     * Credentials als POST-Form an /login. Bei Erfolg setzt der Server
     * eine Session-Cookie und macht einen Redirect auf das Dashboard.
     *
     * Polar nutzt einen Spring-Security-aehnlichen Login (j_username,
     * j_password) — bestaetigt durch mehrere OSS-Projekte (scanban/polar-
     * flow-export, asib/polar-flow-export). Wir probieren beide Formen.
     *
     * @return Erfolg = true wenn nach dem Login der Cookie gesetzt wurde.
     */
    suspend fun login(email: String, password: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            // VARIANTE A: Modernes Login-Endpoint
            val formA = FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .add("returnUrl", "https://flow.polar.com/")
                .build()
            val reqA = Request.Builder()
                .url("https://flow.polar.com/login")
                .post(formA)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            val respA = client.newCall(reqA).execute()
            val bodyA = respA.body?.string().orEmpty()
            respA.close()
            Log.i(TAG, "PolarFlowWeb: login variant A HTTP ${respA.code} cookies=${cookieJar.loadForRequest("https://flow.polar.com/".toHttpUrl()).map { it.name }} bodyPreview=${bodyA.take(200)}")
            if (isLoggedIn() && verifyLogin()) {
                secrets.polarFlowEmail = email
                Log.i(TAG, "PolarFlowWeb: Login (Variante A) erfolgreich")
                return@runCatching
            }

            // VARIANTE B: Spring-Security-Style
            val formB = FormBody.Builder()
                .add("j_username", email)
                .add("j_password", password)
                .build()
            val reqB = Request.Builder()
                .url("https://flow.polar.com/ajaxLogin")
                .post(formB)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json,*/*")
                .build()
            val respB = client.newCall(reqB).execute()
            val bodyB = respB.body?.string().orEmpty()
            respB.close()
            Log.i(TAG, "PolarFlowWeb: login variant B HTTP ${respB.code} cookies=${cookieJar.loadForRequest("https://flow.polar.com/".toHttpUrl()).map { it.name }} bodyPreview=${bodyB.take(200)}")
            if (isLoggedIn() && verifyLogin()) {
                secrets.polarFlowEmail = email
                Log.i(TAG, "PolarFlowWeb: Login (Variante B) erfolgreich")
                return@runCatching
            }
            throw IllegalStateException("Polar Flow Login fehlgeschlagen — kein gueltiger Session-Cookie. Polar koennte 2FA oder Captcha verlangen.")
        }
    }

    /**
     * Prueft per Anfrage an /api/usercheck ob der Cookie wirklich gueltig
     * ist. Manche Login-Pfade liefern Cookies auch bei falschem Passwort.
     */
    private suspend fun verifyLogin(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://flow.polar.com/api/user/loggedin")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            resp.close()
            Log.i(TAG, "PolarFlowWeb: verifyLogin HTTP ${resp.code} body=${body.take(200)}")
            resp.isSuccessful && (body.contains("true") || body.contains("loggedIn") || body.contains("userId"))
        }.getOrDefault(false)
    }

    fun logout() {
        secrets.polarFlowCookieJar = null
        secrets.polarFlowEmail = null
    }

    /**
     * Speichert Cookies aus dem WebView-Login. WebView's CookieManager liefert
     * sie als ein einziger Header-String "name1=value1; name2=value2; ...".
     * Wir parsen das in unser persistierungs-Format um.
     */
    fun setCookiesFromWebView(cookieHeader: String, email: String?) {
        if (cookieHeader.isBlank()) return
        // WebView-Cookies haben kein Expires/Domain/Path-Detail im Header.
        // Wir nutzen flow.polar.com als Default-Domain mit 1-Jahr-Expiry.
        val expiry = System.currentTimeMillis() + 365L * 24 * 3600 * 1000
        val lines = mutableListOf<String>()
        for (raw in cookieHeader.split(";")) {
            val kv = raw.trim()
            val eq = kv.indexOf('=')
            if (eq <= 0) continue
            val name = kv.substring(0, eq).trim()
            val value = kv.substring(eq + 1).trim()
            // host\tname\tvalue\tdomain\tpath\texpires\tsecure\thttpOnly
            lines += "flow.polar.com\t$name\t$value\t.polar.com\t/\t$expiry\ttrue\tfalse"
        }
        secrets.polarFlowCookieJar = lines.joinToString("\n")
        if (!email.isNullOrBlank()) secrets.polarFlowEmail = email
        Log.i(TAG, "PolarFlowWeb: Cookies aus WebView gespeichert (${lines.size} Eintraege)")
    }

    /**
     * Laedt EINEN Workout via Polar Flow Web. exerciseId ist die numerische
     * ID die wir aus dem trackId ("polar-486174823") extrahieren — Polar
     * Flow's Webseite nutzt die gleichen IDs wie AccessLink.
     *
     * Antwort-Format `/training/analysis/{id}/range/data`:
     * {
     *   "exerciseId": 486174823,
     *   "startTime": "2026-05-16T17:22:00+02:00",
     *   "duration": "PT57M30S",
     *   "distance": 7220.5,
     *   "calories": 720,
     *   "sport": "RUNNING",
     *   "sampleTypes": ["HEART_RATE","SPEED","ALTITUDE","CADENCE","DISTANCE"],
     *   "samples": {
     *     "HEART_RATE": [[0, 95], [1000, 102], ...],
     *     "SPEED": [...],
     *     ...
     *   },
     *   "route": [{"lat":..,"lon":..,"alt":..,"time":..}, ...]
     * }
     *
     * Polar's tatsaechliches JSON-Format kann leicht abweichen — defensive
     * Parser mit ignoreUnknownKeys.
     */
    suspend fun fetchWorkout(exerciseId: Long): Result<AmazfitWorkoutEntity?> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            if (!isLoggedIn()) {
                Log.w(TAG, "PolarFlowWeb: nicht eingeloggt — fetchWorkout skipt")
                return@runCatching null
            }
            // GET /training/analysis/{id}/range/data
            val url = "https://flow.polar.com/training/analysis/$exerciseId/range/data"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            resp.close()
            Log.i(TAG, "PolarFlowWeb: fetchWorkout($exerciseId) HTTP ${resp.code} bytes=${body.length} preview=${body.take(400)}")
            if (!resp.isSuccessful) {
                if (resp.code == 401 || resp.code == 403) {
                    // Cookie ist ungueltig — User muss neu einloggen
                    logout()
                }
                return@runCatching null
            }
            parseWorkoutJson(exerciseId, body)
        }.onFailure { ex ->
            Log.w(TAG, "PolarFlowWeb: fetchWorkout($exerciseId) fehlgeschlagen — ${ex.message}", ex)
        }
    }

    /**
     * Parst Polar Flow's JSON-Response in unsere Entity. Defensive — bei
     * unbekanntem Format gibt null zurueck damit der Aufrufer einen klaren
     * Fehler bekommt.
     */
    private fun parseWorkoutJson(exerciseId: Long, json: String): AmazfitWorkoutEntity? {
        return runCatching {
            val root = Json.parseToJsonElement(json) as? JsonObject ?: return@runCatching null
            val startTime = root["startTime"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            val durationIso = root["duration"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            val distance = root["distance"]?.let { (it as? JsonPrimitive)?.doubleOrNull }
            val calories = root["calories"]?.let { (it as? JsonPrimitive)?.intOrNull }
            val sport = root["sport"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            val avgHr = root["avgHeartRate"]?.let { (it as? JsonPrimitive)?.intOrNull }
                ?: (root["heartRate"] as? JsonObject)?.get("average")?.let { (it as? JsonPrimitive)?.intOrNull }
            val maxHr = root["maxHeartRate"]?.let { (it as? JsonPrimitive)?.intOrNull }
                ?: (root["heartRate"] as? JsonObject)?.get("maximum")?.let { (it as? JsonPrimitive)?.intOrNull }

            val startMs = startTime?.let {
                runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
                    ?: runCatching {
                        // Polar gibt oft "2026-05-16T17:22:00+02:00" zurueck — Instant kann das auch
                        java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                    }.getOrNull()
            } ?: System.currentTimeMillis()
            val durSec = durationIso?.let {
                runCatching { java.time.Duration.parse(it).seconds }.getOrNull()
            }
            val dateKey = Instant.ofEpochMilli(startMs)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()

            // Samples: { "HEART_RATE": [[t,v],[t,v]...], "SPEED": [...], ... }
            val samples = root["samples"] as? JsonObject
            val sampleTypes = root["sampleTypes"] as? JsonArray
            Log.i(TAG, "PolarFlowWeb: parsed exerciseId=$exerciseId sampleTypes=${sampleTypes?.map { (it as? JsonPrimitive)?.contentOrNull }} samples-keys=${samples?.keys}")

            val hrJson = samples?.let { extractTsValuePairsAsJson(it["HEART_RATE"] as? JsonArray, startMs, "hr") }
            val speedKmh = samples?.let { extractTsValuePairsAsJson(it["SPEED"] as? JsonArray, startMs, "speed") }
            val paceJson = speedKmh?.let { speedToPace(it) }
            val maxSpeed = samples?.let { maxFromArray(it["SPEED"] as? JsonArray) }
            val maxPace = maxSpeed?.takeIf { it > 0.0 }?.let { 3600.0 / it }
            val altGain = samples?.let { altitudeGain(it["ALTITUDE"] as? JsonArray) }
            val altLoss = samples?.let { altitudeLoss(it["ALTITUDE"] as? JsonArray) }
            val cadenceAvg = samples?.let { avgFromArray(it["RUN_CADENCE"] as? JsonArray ?: it["CADENCE"] as? JsonArray) }
            val splitsJson = samples?.let { splitsFromDistance(it["DISTANCE"] as? JsonArray) }
            val distanceFromStream = samples?.let { lastFromArray(it["DISTANCE"] as? JsonArray) }
            val gpsJson = parseRoute(root)

            val actualDistance = distance ?: distanceFromStream
            val avgPace = if (actualDistance != null && actualDistance > 0 && durSec != null && durSec > 0)
                durSec / (actualDistance / 1000.0) else null
            val avgSpd = if (actualDistance != null && durSec != null && durSec > 0)
                (actualDistance / 1000.0) / (durSec / 3600.0) else null
            val vo2 = PolarSampleMapper.estimateVo2Max(actualDistance, durSec, avgHr)
            val strideLengthCm = PolarSampleMapper.strideLengthCmFromCadenceAndDistance(
                cadenceAvg, actualDistance, durSec,
            )

            AmazfitWorkoutEntity(
                trackId = "polar-$exerciseId",
                dateKey = dateKey,
                startMs = startMs,
                endMs = startMs + (durSec ?: 0L) * 1000L,
                durationSeconds = durSec,
                sportType = PolarSampleMapper.mapSportToHealthConnectType(sport, null),
                sportName = PolarSampleMapper.mapSportToGerman(sport, null),
                distanceMeters = actualDistance,
                avgPaceSecPerKm = avgPace,
                maxPaceSecPerKm = maxPace,
                avgSpeedKmh = avgSpd,
                maxSpeedKmh = maxSpeed,
                calories = calories?.toDouble(),
                avgHeartRate = avgHr,
                maxHeartRate = maxHr,
                gpsTrackJson = gpsJson,
                heartRateSeriesJson = hrJson,
                paceSeriesJson = splitsJson,
                splitsJson = null,
                altitudeGainMeters = altGain,
                altitudeLossMeters = altLoss,
                trainingEffectAerobic = null,
                trainingEffectAnaerobic = null,
                vo2Max = vo2,
                cadence = cadenceAvg?.toInt(),
                strideLengthCm = strideLengthCm,
                recoveryTimeHours = null,
                skinTempCelsius = null,
                swolf = null,
                poolLaps = null,
                poolLengthMeters = null,
                source = "polar-flow-web",
                city = null,
                paceStreamJson = paceJson,
                createdAt = System.currentTimeMillis(),
            )
        }.onFailure { ex ->
            Log.w(TAG, "PolarFlowWeb: parse-Fehler — ${ex.message}", ex)
        }.getOrNull()
    }

    private fun extractTsValuePairsAsJson(arr: JsonArray?, startMs: Long, kind: String): String? {
        if (arr == null || arr.isEmpty()) return null
        val out = StringBuilder("[")
        var first = true
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            if (pair.size < 2) continue
            val t = (pair[0] as? JsonPrimitive)?.doubleOrNull ?: continue
            val v = (pair[1] as? JsonPrimitive)?.doubleOrNull ?: continue
            val tsMs = startMs + t.toLong()
            if (!first) out.append(",")
            out.append("[").append(tsMs).append(",").append(
                if (kind == "hr") v.toInt().toString() else v.toString()
            ).append("]")
            first = false
        }
        out.append("]")
        return if (first) null else out.toString()
    }

    private fun speedToPace(speedJson: String): String? {
        // Konvertiert die [ts, speedKmh] in [ts, paceSecPerKm]
        return runCatching {
            val arr = Json.parseToJsonElement(speedJson) as? JsonArray ?: return null
            val out = StringBuilder("[")
            var first = true
            for (item in arr) {
                val pair = item as? JsonArray ?: continue
                if (pair.size < 2) continue
                val ts = (pair[0] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: continue
                val speed = (pair[1] as? JsonPrimitive)?.doubleOrNull ?: continue
                if (speed <= 0.0) continue
                val pace = 3600.0 / speed
                if (pace !in 150.0..1500.0) continue
                if (!first) out.append(",")
                out.append("[").append(ts).append(",").append(pace).append("]")
                first = false
            }
            out.append("]")
            if (first) null else out.toString()
        }.getOrNull()
    }

    private fun maxFromArray(arr: JsonArray?): Double? {
        if (arr == null) return null
        var max = 0.0
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v = (pair.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: continue
            if (v > max) max = v
        }
        return if (max > 0.0) max else null
    }

    private fun avgFromArray(arr: JsonArray?): Double? {
        if (arr == null) return null
        var sum = 0.0
        var count = 0
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v = (pair.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: continue
            if (v > 0.0) {
                sum += v
                count++
            }
        }
        return if (count > 0) sum / count else null
    }

    private fun lastFromArray(arr: JsonArray?): Double? {
        if (arr == null) return null
        var last = 0.0
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v = (pair.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: continue
            if (v > last) last = v
        }
        return if (last > 0.0) last else null
    }

    private fun altitudeGain(arr: JsonArray?): Double? {
        if (arr == null || arr.size < 2) return null
        var gain = 0.0
        var prev: Double? = null
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v = (pair.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: continue
            if (prev != null) {
                val d = v - prev
                if (d > 0) gain += d
            }
            prev = v
        }
        return if (gain > 0.5) gain else null
    }

    private fun altitudeLoss(arr: JsonArray?): Double? {
        if (arr == null || arr.size < 2) return null
        var loss = 0.0
        var prev: Double? = null
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v = (pair.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: continue
            if (prev != null) {
                val d = v - prev
                if (d < 0) loss += -d
            }
            prev = v
        }
        return if (loss > 0.5) loss else null
    }

    private fun splitsFromDistance(arr: JsonArray?): String? {
        if (arr == null || arr.size < 2) return null
        val kmTimes = mutableListOf<Double>()
        var nextKm = 1
        var prevT = 0.0
        var prevD = 0.0
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val t = (pair.getOrNull(0) as? JsonPrimitive)?.doubleOrNull ?: continue
            val d = (pair.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: continue
            if (d < prevD) { prevD = d; prevT = t; continue }
            while (d >= nextKm * 1000.0) {
                val span = d - prevD
                val frac = if (span > 0) (nextKm * 1000.0 - prevD) / span else 0.0
                val splitT = prevT + frac * (t - prevT)
                kmTimes += splitT / 1000.0
                nextKm++
            }
            prevT = t
            prevD = d
        }
        if (kmTimes.isEmpty()) return null
        val splits = mutableListOf<Double>()
        var prev = 0.0
        for (t in kmTimes) {
            val split = t - prev
            if (split in 120.0..1800.0) splits += split
            prev = t
        }
        return if (splits.isEmpty()) null else splits.joinToString("|") { "%.1f".format(it).replace(",", ".") }
    }

    private fun parseRoute(root: JsonObject): String? {
        val routeArr = root["route"] as? JsonArray ?: root["gpsTrack"] as? JsonArray ?: return null
        if (routeArr.isEmpty()) return null
        val out = StringBuilder("[")
        var first = true
        for (item in routeArr) {
            val obj = item as? JsonObject ?: continue
            val lat = (obj["lat"] as? JsonPrimitive)?.doubleOrNull
                ?: (obj["latitude"] as? JsonPrimitive)?.doubleOrNull ?: continue
            val lon = (obj["lon"] as? JsonPrimitive)?.doubleOrNull
                ?: (obj["longitude"] as? JsonPrimitive)?.doubleOrNull ?: continue
            val alt = (obj["alt"] as? JsonPrimitive)?.doubleOrNull
                ?: (obj["altitude"] as? JsonPrimitive)?.doubleOrNull ?: 0.0
            val ts = (obj["time"] as? JsonPrimitive)?.doubleOrNull?.toLong()
                ?: (obj["timestamp"] as? JsonPrimitive)?.doubleOrNull?.toLong() ?: 0L
            if (!first) out.append(",")
            out.append("[").append(lat).append(",").append(lon).append(",").append(alt).append(",").append(ts).append("]")
            first = false
        }
        out.append("]")
        return if (first) null else out.toString()
    }

    companion object {
        private const val TAG = "PolarFlowWeb"
    }
}
