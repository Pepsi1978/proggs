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
        // Hauptquelle: WebView's CookieManager. Persistiert ueber App-Restarts.
        val webViewCookies = runCatching {
            android.webkit.CookieManager.getInstance().getCookie("https://flow.polar.com/")
        }.getOrNull()
        if (!webViewCookies.isNullOrBlank() && webViewCookies.length > 50) return true
        // Fallback: SharedPrefs (alte Persistierung)
        val raw = secrets.polarFlowCookieJar
        return !raw.isNullOrBlank()
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
        // Auch WebView's CookieManager komplett leeren — sonst zeigt der
        // naechste Login die alte Session und Frank kann nicht umschalten.
        runCatching {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        }
        Log.i(TAG, "PolarFlowWeb: logout — alle Cookies geloescht")
    }

    /**
     * Speichert den RAWEN Cookie-Header aus dem WebView. Wir persistieren
     * den String unveraendert und nutzen ihn direkt in jedem Request als
     * Cookie-Header. So umgehen wir das CookieJar-Domain-Matching.
     */
    fun setCookiesFromWebView(cookieHeader: String, email: String?) {
        if (cookieHeader.isBlank()) return
        // Format-Praefix "RAW:" damit wir wissen es ist Direkt-Header-String
        secrets.polarFlowCookieJar = "RAW:$cookieHeader"
        if (!email.isNullOrBlank()) secrets.polarFlowEmail = email
        val cookieCount = cookieHeader.split(";").count { it.contains("=") }
        Log.i(TAG, "PolarFlowWeb: Raw-Cookies aus WebView gespeichert (${cookieCount} Eintraege, ${cookieHeader.length} bytes)")
    }

    /**
     * Parst einen ROHEN Body (JSON oder XML) der vom WebView-JS-fetch
     * geliefert wurde. Selbe Logik wie fetchWorkout, nur ohne HTTP-Aufruf.
     */
    suspend fun parseAndStoreWorkoutBody(exerciseId: Long, rawBody: String): Result<AmazfitWorkoutEntity?> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val trimmed = rawBody.trimStart()
            Log.i(TAG, "PolarFlowWeb: parseAndStoreWorkoutBody bytes=${rawBody.length} starts=${trimmed.take(50)}")
            when {
                trimmed.startsWith("{") || trimmed.startsWith("[") -> parseWorkoutJson(exerciseId, rawBody)
                trimmed.contains("TrainingCenterDatabase") -> parseTcx(exerciseId, rawBody)
                trimmed.startsWith("<gpx") || trimmed.contains("<gpx ") -> parseGpxOnly(exerciseId, rawBody)
                trimmed.startsWith("<") -> {
                    // HTML — versuche inline-JSON zu extrahieren
                    val extracted = extractTrainingJsonFromHtml(rawBody, exerciseId)
                    if (extracted != null) {
                        Log.i(TAG, "PolarFlowWeb: HTML-Inline-JSON extrahiert (${extracted.length} bytes)")
                        parseWorkoutJson(exerciseId, extracted)
                    } else {
                        Log.w(TAG, "PolarFlowWeb: HTML enthielt kein verwertbares Trainings-JSON")
                        null
                    }
                }
                else -> {
                    Log.w(TAG, "PolarFlowWeb: unbekanntes Body-Format — preview=${rawBody.take(200)}")
                    null
                }
            }
        }
    }

    /**
     * Sucht in HTML nach eingebetteten Trainings-JSON-Bloecken. Polar Flow
     * rendert die Daten oft als `var trainingData = {...};` oder als
     * `window.__INITIAL_STATE__ = {...};`. Plus: regex auf JSON-Bloecke
     * mit sport+distance.
     */
    private fun extractTrainingJsonFromHtml(html: String, exerciseId: Long): String? {
        val patterns = listOf(
            "window\\.__INITIAL_STATE__\\s*=\\s*(\\{[\\s\\S]*?\\});".toRegex(),
            "window\\.__PRELOADED_STATE__\\s*=\\s*(\\{[\\s\\S]*?\\});".toRegex(),
            "var\\s+trainingData\\s*=\\s*(\\{[\\s\\S]*?\\});".toRegex(),
            "var\\s+trainingSession\\s*=\\s*(\\{[\\s\\S]*?\\});".toRegex(),
            "data-training\\s*=\\s*['\"](\\{[\\s\\S]*?\\})['\"]".toRegex(),
        )
        for (re in patterns) {
            val m = re.find(html) ?: continue
            return m.groupValues[1]
        }
        // Suche nach JSON-Bloecken mit sport + distance
        val genericRe = "(\\{[^{}]{200,}?\"sport\"[^{}]*?\"distance\"[^{}]*?\\})".toRegex()
        val m = genericRe.find(html)
        if (m != null) return m.groupValues[1]
        return null
    }

    /**
     * Gibt den rohen Cookie-Header-String fuer Direct-Injection zurueck.
     *
     * Strategie: PRIMAERE Quelle ist Android's WebView CookieManager — der
     * persistiert Cookies automatisch und ist immer frisch nach dem Login.
     * Fallback: alte SharedPrefs-Persistierung (RAW:-prefix).
     */
    private fun rawCookieHeader(): String? {
        // 1. WebView's CookieManager (Live-Quelle, immer aktuell)
        val webViewCookies = runCatching {
            android.webkit.CookieManager.getInstance().getCookie("https://flow.polar.com/")
        }.getOrNull()
        if (!webViewCookies.isNullOrBlank() && webViewCookies.length > 50) {
            Log.d(TAG, "rawCookieHeader: nutze WebView CookieManager (len=${webViewCookies.length})")
            return webViewCookies
        }
        // 2. SharedPrefs-Fallback
        val raw = secrets.polarFlowCookieJar ?: return null
        return when {
            raw.startsWith("RAW:") -> raw.removePrefix("RAW:")
            else -> null
        }
    }

    /**
     * Polar's Play-Framework setzt den CSRF-Token in das PLAY_SESSION_FLOW-
     * Cookie als JWT-Payload eingebettet (HS256-signiert, NICHT verschluesselt).
     *
     * Cookie-Wert (typisch):
     *   eyJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7ImNzcmZUb2tlbiI6IjEyMzQ1Ni0xMjM..."}.MAC
     *
     * Wir base64-decoden den mittleren Teil und extrahieren `data.csrfToken`.
     */
    private fun extractXsrfToken(cookieHeader: String?): String? {
        if (cookieHeader.isNullOrBlank()) return null
        // 1. Direkte Cookie-Namen (falls Polar einen separaten setzt)
        val candidates = listOf("XSRF-TOKEN", "Polar-CSRF-Token", "csrfToken", "CSRF-TOKEN")
        for (raw in cookieHeader.split(";")) {
            val kv = raw.trim()
            val eq = kv.indexOf('=')
            if (eq <= 0) continue
            val name = kv.substring(0, eq).trim()
            if (candidates.any { it.equals(name, ignoreCase = true) }) {
                return kv.substring(eq + 1).trim()
            }
        }
        // 2. JWT in PLAY_SESSION_FLOW-Cookie
        for (raw in cookieHeader.split(";")) {
            val kv = raw.trim()
            val eq = kv.indexOf('=')
            if (eq <= 0) continue
            val name = kv.substring(0, eq).trim()
            if (!name.equals("PLAY_SESSION_FLOW", ignoreCase = true) &&
                !name.equals("PLAY_SESSION", ignoreCase = true)) continue
            val jwt = kv.substring(eq + 1).trim()
            val parts = jwt.split(".")
            if (parts.size < 2) continue
            try {
                val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
                val payload = String(payloadBytes)
                Log.d(TAG, "PolarFlowWeb: PLAY_SESSION JWT-payload: ${payload.take(300)}")
                val match = "\"csrfToken\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(payload)
                if (match != null) {
                    val token = match.groupValues[1]
                    Log.i(TAG, "PolarFlowWeb: CSRF-Token aus JWT extrahiert (len=${token.length})")
                    return token
                }
            } catch (t: Throwable) {
                Log.w(TAG, "PolarFlowWeb: JWT-Decode-Fehler — ${t.message}")
            }
        }
        return null
    }

    /**
     * Parst TCX-XML (Garmin Training Center Database). Polar's Export-
     * Endpoint /api/export/training/tcx/{id} liefert dieses Format mit
     * komplettem Activity-Datensatz inkl. HR, GPS, Speed, Altitude, Cadence.
     */
    private fun parseTcx(exerciseId: Long, xml: String): AmazfitWorkoutEntity? {
        return runCatching {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(xml))
            var startTime: String? = null
            var sport: String? = null
            var totalDistance: Double? = null
            var totalSeconds: Long? = null
            var calories: Int? = null
            var maxHr: Int? = null
            var avgHr: Int? = null
            val hrPairs = mutableListOf<Pair<Long, Int>>()
            val speedPairs = mutableListOf<Pair<Long, Double>>()
            val altPairs = mutableListOf<Pair<Long, Double>>()
            val cadencePairs = mutableListOf<Pair<Long, Int>>()
            val distPairs = mutableListOf<Pair<Long, Double>>()
            val gpsPoints = mutableListOf<DoubleArray>()
            var lat: Double? = null
            var lon: Double? = null
            var alt: Double? = null
            var time: Long? = null
            var hr: Int? = null
            var distM: Double? = null
            var cadence: Int? = null
            var speedMs: Double? = null
            var currentTag = ""
            var startMs = 0L
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (event) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("Activity", true)) {
                            sport = parser.getAttributeValue(null, "Sport")
                        }
                        if (currentTag.equals("Trackpoint", true)) {
                            lat = null; lon = null; alt = null; time = null; hr = null
                            distM = null; cadence = null; speedMs = null
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            when (currentTag.lowercase()) {
                                "id" -> if (startTime == null) startTime = text
                                "time" -> time = runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
                                "latitudedegrees" -> lat = text.toDoubleOrNull()
                                "longitudedegrees" -> lon = text.toDoubleOrNull()
                                "altitudemeters" -> alt = text.toDoubleOrNull()
                                "distancemeters" -> distM = text.toDoubleOrNull()
                                "value" -> hr = text.toIntOrNull()
                                "cadence", "runcadence" -> cadence = text.toIntOrNull()
                                "speed" -> speedMs = text.toDoubleOrNull()
                                "totaltimeseconds" -> totalSeconds = (text.toDoubleOrNull() ?: 0.0).toLong()
                                "distancemeters_total" -> totalDistance = text.toDoubleOrNull()
                                "calories" -> calories = text.toIntOrNull()
                                "maximumheartratebpm", "maxheartratebpm" -> maxHr = text.toIntOrNull()
                                "averageheartratebpm", "avgheartratebpm" -> avgHr = text.toIntOrNull()
                            }
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name.equals("Trackpoint", true)) {
                            val t = time
                            if (t != null) {
                                if (startMs == 0L) startMs = t
                                val rel = t
                                if (lat != null && lon != null) {
                                    gpsPoints += doubleArrayOf(lat!!, lon!!, alt ?: 0.0, rel.toDouble())
                                }
                                if (hr != null) hrPairs += rel to hr!!
                                if (alt != null) altPairs += rel to alt!!
                                if (distM != null) distPairs += rel to distM!!
                                if (cadence != null) cadencePairs += rel to cadence!!
                                if (speedMs != null) speedPairs += rel to speedMs!! * 3.6
                            }
                        }
                        currentTag = ""
                    }
                }
                event = parser.next()
            }
            if (startTime == null && startMs > 0L) startTime = Instant.ofEpochMilli(startMs).toString()
            if (totalDistance == null && distPairs.isNotEmpty()) totalDistance = distPairs.last().second
            Log.i(TAG, "TCX: trackpoints hr=${hrPairs.size} gps=${gpsPoints.size} alt=${altPairs.size} cadence=${cadencePairs.size} speed=${speedPairs.size} dist=${totalDistance} totalSec=$totalSeconds avgHr=$avgHr maxHr=$maxHr")
            val startEpochMs = startTime?.let {
                runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
            } ?: startMs.takeIf { it > 0 } ?: System.currentTimeMillis()
            val dateKey = Instant.ofEpochMilli(startEpochMs).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val durSec = totalSeconds ?: (if (hrPairs.size >= 2) (hrPairs.last().first - hrPairs.first().first) / 1000L else null)

            val hrJson = if (hrPairs.isNotEmpty()) hrPairs.joinToString(",", "[", "]") { "[${it.first},${it.second}]" } else null
            val paceStreamJson = if (speedPairs.isNotEmpty()) speedPairs.filter { it.second > 0 }.joinToString(",", "[", "]") { "[${it.first},${3600.0 / it.second}]" } else null
            val gpsJson = if (gpsPoints.isNotEmpty()) gpsPoints.joinToString(",", "[", "]") { "[${it[0]},${it[1]},${it[2]},${it[3].toLong()}]" } else null
            val maxSpeed = speedPairs.maxOfOrNull { it.second }
            val maxPace = maxSpeed?.takeIf { it > 0 }?.let { 3600.0 / it }
            val altGain = altPairs.zipWithNext().sumOf { (a, b) -> (b.second - a.second).coerceAtLeast(0.0) }.takeIf { it > 0.5 }
            val altLoss = altPairs.zipWithNext().sumOf { (a, b) -> (-(b.second - a.second)).coerceAtLeast(0.0) }.takeIf { it > 0.5 }
            val cadenceAvg = if (cadencePairs.isNotEmpty()) cadencePairs.filter { it.second > 0 }.map { it.second.toDouble() }.average() else null
            val splitsJson = if (distPairs.size >= 2) splitsFromDistanceRaw(distPairs) else null
            val computedAvgHr = avgHr ?: if (hrPairs.isNotEmpty()) hrPairs.map { it.second }.average().toInt() else null
            val computedMaxHr = maxHr ?: hrPairs.maxOfOrNull { it.second }
            val avgPace = if (totalDistance != null && totalDistance!! > 0 && durSec != null && durSec > 0)
                durSec / (totalDistance!! / 1000.0) else null
            val avgSpd = if (totalDistance != null && durSec != null && durSec > 0)
                (totalDistance!! / 1000.0) / (durSec / 3600.0) else null
            val vo2 = PolarSampleMapper.estimateVo2Max(totalDistance, durSec, computedAvgHr)
            val strideLengthCm = PolarSampleMapper.strideLengthCmFromCadenceAndDistance(
                cadenceAvg, totalDistance, durSec,
            )

            AmazfitWorkoutEntity(
                trackId = "polar-$exerciseId",
                dateKey = dateKey,
                startMs = startEpochMs,
                endMs = startEpochMs + (durSec ?: 0L) * 1000L,
                durationSeconds = durSec,
                sportType = PolarSampleMapper.mapSportToHealthConnectType(sport, null),
                sportName = PolarSampleMapper.mapSportToGerman(sport, null),
                distanceMeters = totalDistance,
                avgPaceSecPerKm = avgPace,
                maxPaceSecPerKm = maxPace,
                avgSpeedKmh = avgSpd,
                maxSpeedKmh = maxSpeed,
                calories = calories?.toDouble(),
                avgHeartRate = computedAvgHr,
                maxHeartRate = computedMaxHr,
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
                paceStreamJson = paceStreamJson,
                createdAt = System.currentTimeMillis(),
            )
        }.onFailure { ex ->
            Log.w(TAG, "PolarFlowWeb: TCX-Parse-Fehler — ${ex.message}", ex)
        }.getOrNull()
    }

    private fun splitsFromDistanceRaw(pairs: List<Pair<Long, Double>>): String? {
        if (pairs.size < 2) return null
        val kmTimes = mutableListOf<Double>()
        var nextKm = 1
        var prevT = pairs[0].first.toDouble()
        var prevD = pairs[0].second
        for (i in 1 until pairs.size) {
            val (t, d) = pairs[i]
            if (d < prevD) { prevD = d; prevT = t.toDouble(); continue }
            while (d >= nextKm * 1000.0) {
                val span = d - prevD
                val frac = if (span > 0) (nextKm * 1000.0 - prevD) / span else 0.0
                val splitT = prevT + frac * (t.toDouble() - prevT)
                kmTimes += splitT / 1000.0
                nextKm++
            }
            prevT = t.toDouble()
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

    private fun parseGpxOnly(exerciseId: Long, xml: String): AmazfitWorkoutEntity? {
        // GPX hat nur GPS — wir bauen minimale Entity mit GPS-Track.
        val gpsJson = PolarSampleMapper.parseGpxToTrackJson(xml) ?: return null
        val now = System.currentTimeMillis()
        return AmazfitWorkoutEntity(
            trackId = "polar-$exerciseId",
            dateKey = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
            startMs = now,
            endMs = now,
            durationSeconds = null,
            sportType = null,
            sportName = null,
            distanceMeters = null,
            avgPaceSecPerKm = null,
            maxPaceSecPerKm = null,
            avgSpeedKmh = null,
            maxSpeedKmh = null,
            calories = null,
            avgHeartRate = null,
            maxHeartRate = null,
            gpsTrackJson = gpsJson,
            heartRateSeriesJson = null,
            paceSeriesJson = null,
            splitsJson = null,
            altitudeGainMeters = null,
            altitudeLossMeters = null,
            trainingEffectAerobic = null,
            trainingEffectAnaerobic = null,
            vo2Max = null,
            cadence = null,
            strideLengthCm = null,
            recoveryTimeHours = null,
            skinTempCelsius = null,
            swolf = null,
            poolLaps = null,
            poolLengthMeters = null,
            source = "polar-flow-web",
            city = null,
            paceStreamJson = null,
            createdAt = System.currentTimeMillis(),
        )
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
            // Polar Flow Web hat KEINEN offiziell dokumentierten JSON-Endpoint.
            // Wir probieren mehrere bekannte Pfade aus OSS-Reverse-Engineering-
            // Projekten, plus moderne Varianten die in Polar Flow seit 2024
            // gesichtet wurden. Der erste der echtes JSON liefert gewinnt.
            val candidates = listOf(
                // Export-Endpoints (bestaetigt aus Polar's OSS-Reverse-Engineering)
                "https://flow.polar.com/api/export/training/tcx/$exerciseId",
                "https://flow.polar.com/api/export/training/gpx/$exerciseId",
                "https://flow.polar.com/api/export/training/csv/$exerciseId",
                "https://flow.polar.com/api/export/training/fit/$exerciseId",
                "https://flow.polar.com/api/training-sessions/$exerciseId",
                "https://flow.polar.com/api/training/$exerciseId",
                "https://flow.polar.com/training/$exerciseId/exportTcx",
                "https://flow.polar.com/api/exercise/$exerciseId",
            )
            val rawCookies = rawCookieHeader()
            val xsrfToken = extractXsrfToken(rawCookies)
            Log.i(TAG, "PolarFlowWeb: nutze rawCookies (len=${rawCookies?.length ?: 0}) xsrfToken=${xsrfToken?.take(20)}")
            for (url in candidates) {
                try {
                    val builder = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .header("Accept", if (url.contains("/tcx") || url.contains("/gpx")) "application/xml, text/xml" else "application/json, text/xml, */*; q=0.01")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Referer", "https://flow.polar.com/training/analysis/$exerciseId")
                    if (!rawCookies.isNullOrBlank()) builder.header("Cookie", rawCookies)
                    if (!xsrfToken.isNullOrBlank()) {
                        // Play-Framework akzeptiert mehrere Varianten — wir
                        // setzen sie alle damit wir die richtige treffen.
                        builder.header("Csrf-Token", xsrfToken)
                        builder.header("X-XSRF-TOKEN", xsrfToken)
                        builder.header("X-CSRF-TOKEN", xsrfToken)
                    }
                    val req = builder.build()
                    val resp = client.newCall(req).execute()
                    val body = resp.body?.string().orEmpty()
                    resp.close()
                    val trimmed = body.trimStart()
                    val isJson = trimmed.startsWith("{") || trimmed.startsWith("[")
                    val isXml = trimmed.startsWith("<?xml") || trimmed.startsWith("<gpx") || trimmed.startsWith("<TrainingCenterDatabase")
                    Log.i(TAG, "PolarFlowWeb: probe $url HTTP ${resp.code} isJson=$isJson isXml=$isXml bytes=${body.length} preview=${body.take(200)}")
                    if (resp.isSuccessful && body.length > 100) {
                        if (isJson) {
                            val parsed = parseWorkoutJson(exerciseId, body)
                            if (parsed != null) {
                                Log.i(TAG, "PolarFlowWeb: JSON erfolgreich geparst aus $url")
                                return@runCatching parsed
                            }
                        }
                        if (isXml) {
                            val parsed = if (url.contains("/tcx") || trimmed.contains("TrainingCenterDatabase")) {
                                parseTcx(exerciseId, body)
                            } else if (url.contains("/gpx") || trimmed.startsWith("<gpx")) {
                                parseGpxOnly(exerciseId, body)
                            } else null
                            if (parsed != null) {
                                Log.i(TAG, "PolarFlowWeb: XML erfolgreich geparst aus $url")
                                return@runCatching parsed
                            }
                        }
                    }
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    Log.w(TAG, "PolarFlowWeb: probe $url Exception: ${t.message}")
                }
            }
            // Fallback: HTML-Seite scrapen — Polar Flow embeds oft ein
            // <script> mit allen Daten als JSON-Variable.
            try {
                val htmlUrl = "https://flow.polar.com/training/analysis/$exerciseId"
                val htmlBuilder = Request.Builder()
                    .url(htmlUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml")
                if (!rawCookies.isNullOrBlank()) htmlBuilder.header("Cookie", rawCookies)
                val req = htmlBuilder.build()
                val resp = client.newCall(req).execute()
                val html = resp.body?.string().orEmpty()
                resp.close()
                Log.i(TAG, "PolarFlowWeb: HTML-Scrape $htmlUrl HTTP ${resp.code} bytes=${html.length}")
                if (resp.isSuccessful && html.isNotBlank()) {
                    val extracted = extractInlineJsonFromHtml(html)
                    if (extracted != null) {
                        Log.i(TAG, "PolarFlowWeb: inline-JSON extrahiert (${extracted.length} bytes)")
                        return@runCatching parseWorkoutJson(exerciseId, extracted)
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "PolarFlowWeb: HTML-Scrape Exception: ${t.message}")
            }
            Log.w(TAG, "PolarFlowWeb: KEIN Endpoint lieferte verarbeitbares JSON fuer $exerciseId")
            null
        }.onFailure { ex ->
            Log.w(TAG, "PolarFlowWeb: fetchWorkout($exerciseId) fehlgeschlagen — ${ex.message}", ex)
        }
    }

    /**
     * Sucht im HTML nach inline-JSON. Polar's Detail-Seiten haben oft
     * `<script>var __INITIAL_STATE__ = { ... };</script>` oder aehnliches.
     */
    private fun extractInlineJsonFromHtml(html: String): String? {
        val patterns = listOf(
            "var\\s+__INITIAL_STATE__\\s*=\\s*(\\{.*?\\});".toRegex(RegexOption.DOT_MATCHES_ALL),
            "window\\.__INITIAL_STATE__\\s*=\\s*(\\{.*?\\});".toRegex(RegexOption.DOT_MATCHES_ALL),
            "var\\s+trainingSession\\s*=\\s*(\\{.*?\\});".toRegex(RegexOption.DOT_MATCHES_ALL),
            "var\\s+trainingData\\s*=\\s*(\\{.*?\\});".toRegex(RegexOption.DOT_MATCHES_ALL),
            "data-session\\s*=\\s*'(\\{.*?\\})'".toRegex(RegexOption.DOT_MATCHES_ALL),
            "data-session\\s*=\\s*\"(\\{.*?\\})\"".toRegex(RegexOption.DOT_MATCHES_ALL),
        )
        for (re in patterns) {
            val m = re.find(html)
            if (m != null) {
                val raw = m.groupValues[1]
                Log.d(TAG, "PolarFlowWeb: inline-JSON gefunden via pattern, len=${raw.length}, preview=${raw.take(200)}")
                return raw
            }
        }
        // Auch nach allgemeinen JSON-Bloecken mit Trainings-Felder suchen
        val anyJson = "\\{\\s*\"(?:exerciseId|sessionId|trainingId|sport|startTime|duration|distance)[^\\{]*?(.*?)\\}".toRegex(RegexOption.DOT_MATCHES_ALL).find(html)
        if (anyJson != null) {
            Log.d(TAG, "PolarFlowWeb: fallback-JSON gefunden, preview=${anyJson.value.take(200)}")
            return anyJson.value
        }
        return null
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
