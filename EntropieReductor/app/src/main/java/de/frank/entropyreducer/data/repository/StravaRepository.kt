package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.remote.strava.StravaActivityDetail
import de.frank.entropyreducer.data.remote.strava.StravaActivitySummary
import de.frank.entropyreducer.data.remote.strava.StravaApi
import de.frank.entropyreducer.data.remote.strava.StravaStream
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.util.runCatchingCancellable
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Strava-Workout-Sync (Frank-Wunsch 2026-05-16).
 *
 * Holt Workouts der letzten N Tage und konvertiert sie zu `AmazfitWorkoutEntity`,
 * damit sie ohne UI-Aenderungen in die existierende Workout-Pipeline einsteigen.
 * Die `source`-Spalte unterscheidet "strava" von "zepp-cloud" und "health_connect".
 *
 * Detail-Ladestrategie:
 *  - Liste aller Activities holen (1 Call)
 *  - Pro Activity Streams (GPS, HR, Pace, Cadence, Altitude) holen (1 Call)
 *  - Pro Activity Laps holen (1 Call)
 *  - Pro Activity Detail-Endpoint (1 Call) — fuer splits_metric
 *  → 4 Calls pro Activity. Bei 30 Tagen ~10 Workouts = 40 Calls. Strava-Limits
 *    100/15min und 1000/Tag werden nicht ueberschritten.
 *
 * Rate-Limit-Schutz: 200ms Delay zwischen Activity-Calls (= max 75 Activities/15min).
 *
 * Ergebnis: Liste von AmazfitWorkoutEntity mit source="strava". Der Aufrufer
 * (AmazfitRepository.mergeFromStrava) entscheidet ueber Insert/Update gegen
 * existierende Eintraege.
 */
@Singleton
class StravaRepository @Inject constructor(
    private val api: StravaApi,
    private val oauth: OAuthService,
    private val secrets: EncryptedSecretsStore,
    private val diagnostics: DiagnosticLogger,
) {

    /**
     * Frank-Bugfix 2026-05-23: Nur EIN Strava-Sync gleichzeitig. Frueher feuerten bei jedem
     * App-Foreground gleich mehrere Syncs (ON_START + BiomarkerViewModel) parallel — jeder
     * machte eigene API-Calls und sprengte zusammen das Strava-Rate-Limit (HTTP 429). Mit
     * tryLock() ueberspringt ein paralleler Aufruf einfach, statt zu warten und dann auch zu syncen.
     */
    private val syncMutex = Mutex()

    /** Sind wir bei Strava authentifiziert? */
    fun isAuthenticated(): Boolean = oauth.loadStravaAuthState().isAuthorized

    /**
     * Holt Strava-Activities der letzten [days] Tage + Streams + Laps und mapped
     * jede auf AmazfitWorkoutEntity. Liefert die Liste — der Aufrufer schreibt sie
     * in die DB.
     *
     * Bei fehlender Auth liefert es eine leere Liste (kein Fehler — der User hat
     * Strava einfach noch nicht verbunden).
     */
    suspend fun fetchWorkoutsAsEntities(
        days: Int = 30,
        knownTrackIds: Set<String> = emptySet(),
    ): Result<List<AmazfitWorkoutEntity>> {
        if (!isAuthenticated()) {
            Diag.d(DiagnosticArea.STRAVA, TAG, "Strava: nicht authentifiziert — kein Workout-Sync")
            return Result.success(emptyList())
        }
        // Frank-Bugfix 2026-05-23: 429-Backoff — laeuft ein Cooldown, gar nicht erst syncen.
        val nowMs = System.currentTimeMillis()
        val cooldownUntil = secrets.stravaRateLimitedUntilMs
        if (cooldownUntil > nowMs) {
            val minsLeft = ((cooldownUntil - nowMs) / 60_000L) + 1
            Diag.d(DiagnosticArea.STRAVA, TAG, "Strava-Sync uebersprungen — Rate-Limit-Cooldown noch ~$minsLeft Min")
            diagnostics.warn(
                DiagnosticArea.STRAVA,
                "Sync uebersprungen — Rate-Limit-Cooldown laeuft noch ~$minsLeft Min",
            )
            return Result.success(emptyList())
        }
        // Frank-Bugfix 2026-05-23: Dedup — laeuft schon ein Sync, diesen ueberspringen.
        if (!syncMutex.tryLock()) {
            Diag.d(DiagnosticArea.STRAVA, TAG, "Strava-Sync uebersprungen — ein anderer Sync laeuft bereits")
            diagnostics.info(DiagnosticArea.STRAVA, "Sync uebersprungen — laeuft bereits")
            return Result.success(emptyList())
        }
        val result =
            try {
                runCatchingCancellable { doFetchWorkouts(days, knownTrackIds) }
            } finally {
                syncMutex.unlock()
            }
        return result.onFailure { handleSyncFailure(it) }
    }

    /**
     * Frank-Wunsch 2026-05-24: Liefert trackId ("strava_<id>") -> verstrichene Zeit
     * (elapsed_time in Sekunden) fuer alle Strava-Activities der letzten [days] Tage.
     * Macht NUR den (billigen) Listen-Call — KEINE Detail/Streams/Laps-Calls. Dient dem
     * einmaligen Dauer-Backfill bestehender Trainings ohne Rate-Limit-Gefahr.
     *
     * Bei fehlender Auth oder laufendem Rate-Limit-Cooldown -> Result.failure, damit der
     * Aufrufer das Einmal-Flag NICHT setzt und beim naechsten Sync erneut versucht.
     */
    suspend fun fetchElapsedDurations(days: Int = 30): Result<Map<String, Long>> {
        if (!isAuthenticated()) {
            return Result.failure(IllegalStateException("Strava nicht authentifiziert"))
        }
        if (secrets.stravaRateLimitedUntilMs > System.currentTimeMillis()) {
            return Result.failure(IllegalStateException("Strava Rate-Limit-Cooldown aktiv"))
        }
        return runCatchingCancellable {
            val accessToken = oauth.freshStravaAccessToken()
                ?: throw IllegalStateException("Strava-Access-Token konnte nicht erfrischt werden")
            val bearer = "Bearer $accessToken"
            val afterEpoch =
                (System.currentTimeMillis() / 1000L) - (days.toLong() * 24L * 60L * 60L)
            val out = HashMap<String, Long>()
            var page = 1
            while (true) {
                val pageItems = api.listActivities(
                    bearer = bearer,
                    after = afterEpoch,
                    perPage = 100,
                    page = page,
                )
                for (s in pageItems) {
                    val elapsed = s.elapsedTime ?: continue
                    out["strava_${s.id}"] = elapsed
                }
                if (pageItems.size < 100) break
                page += 1
                delay(200L) // Rate-Limit-Schutz
            }
            Diag.i(DiagnosticArea.STRAVA, TAG, "Strava-Dauer-Backfill: ${out.size} Activities gelistet (${days}d-Fenster)")
            out
        }.onFailure { handleSyncFailure(it) }
    }

    private suspend fun doFetchWorkouts(
        days: Int,
        knownTrackIds: Set<String>,
    ): List<AmazfitWorkoutEntity> {
        val accessToken = oauth.freshStravaAccessToken()
            ?: throw IllegalStateException("Strava-Access-Token konnte nicht erfrischt werden")
        val bearer = "Bearer $accessToken"

        // Frank-Wunsch 2026-05-23 (Schritt 4): inkrementell. Wurde schon einmal erfolgreich
        // synchronisiert, nur Activities seit dem letzten Sync holen — minus 7 Tage Ueberlapp.
        // Beim ersten Sync das volle [days]-Fenster.
        val lastSyncMs = secrets.stravaLastSyncEpochMs
        val afterEpoch =
            if (lastSyncMs > 0L) {
                (lastSyncMs - INCREMENTAL_OVERLAP_MS) / 1000L
            } else {
                (System.currentTimeMillis() / 1000L) - (days.toLong() * 24L * 60L * 60L)
            }
        Diag.i(DiagnosticArea.STRAVA, TAG, "Strava: hole Activities ab Unix-Zeit $afterEpoch (inkrementell=${lastSyncMs > 0L})")

        val summaries = mutableListOf<StravaActivitySummary>()
        var page = 1
        while (true) {
            val pageItems = api.listActivities(
                bearer = bearer,
                after = afterEpoch,
                perPage = 100,
                page = page,
            )
            summaries += pageItems
            if (pageItems.size < 100) break // Letzte Seite
            page += 1
            delay(200L) // Rate-Limit-Schutz
        }
        Diag.i(DiagnosticArea.STRAVA, TAG, "Strava: ${summaries.size} Activities im ${days}-Tage-Fenster gefunden")

        val results = mutableListOf<AmazfitWorkoutEntity>()
        var skipped = 0
        for (summary in summaries) {
            // Frank-Wunsch 2026-05-23 (Schritt 4): Die teuren Detail-Calls (Detail + Streams +
            // Laps = 3 Calls pro Workout) NUR fuer NEUE Workouts. Bereits in der DB vorhandene
            // ueberspringen — ein abgeschlossenes Strava-Workout aendert sich nicht nachtraeglich.
            // Das spart die meisten API-Calls und schuetzt vor dem Tages-Lese-Limit.
            if ("strava_${summary.id}" in knownTrackIds) {
                skipped++
                continue
            }
            // Rate-Limit-Schutz: 200ms zwischen echten Activity-Calls, plus 5s-Pause nach je 25.
            if (results.isNotEmpty()) delay(200L)
            if (results.isNotEmpty() && results.size % 25 == 0) {
                Diag.d(DiagnosticArea.STRAVA, TAG, "Strava: zusaetzliche 5s-Pause nach ${results.size} neuen Activities")
                delay(5_000L)
            }

            val activityId = summary.id
            val detail = runCatching { api.getActivity(bearer, activityId) }
                .getOrElse {
                    Diag.w(DiagnosticArea.STRAVA, TAG, "Strava: getActivity($activityId) fehlgeschlagen — ${it.message}")
                    null
                }
            val streams = runCatching { api.getStreams(bearer, activityId) }
                .getOrElse {
                    Diag.w(DiagnosticArea.STRAVA, TAG, "Strava: getStreams($activityId) fehlgeschlagen — ${it.message}")
                    emptyMap()
                }
            val laps = runCatching { api.getLaps(bearer, activityId) }
                .getOrElse {
                    Diag.w(DiagnosticArea.STRAVA, TAG, "Strava: getLaps($activityId) fehlgeschlagen — ${it.message}")
                    emptyList()
                }

            results += summaryToEntity(summary, detail, streams, laps)
        }

        secrets.stravaLastSyncEpochMs = System.currentTimeMillis()
        secrets.stravaRateLimitedUntilMs = 0L // Erfolg -> evtl. bestehenden Cooldown aufheben
        Diag.i(DiagnosticArea.STRAVA, TAG, "Strava-Sync abgeschlossen: ${results.size} neu, $skipped uebersprungen")
        diagnostics.success(
            DiagnosticArea.STRAVA,
            "Sync OK — ${summaries.size} Activities geprueft, ${results.size} neu geladen, " +
                "$skipped uebersprungen (schon vorhanden)",
        )
        return results
    }

    /**
     * Frank-Bugfix 2026-05-23: Zentrale Fehlerbehandlung fuer den Strava-Sync. Bei HTTP 429
     * (Rate-Limit) wird ein 16-Minuten-Cooldown gesetzt (ueberbrueckt Stravas 15-Min-Fenster),
     * damit die App nicht weiter gegen das erschoepfte Limit haemmert. Die Rate-Limit-Header
     * (X-RateLimit-Usage/-Limit) werden mitgeloggt, damit im Diagnose-Screen sichtbar ist ob es
     * das 15-Min- oder das Tageslimit war.
     */
    private fun handleSyncFailure(t: Throwable) {
        if (t is kotlinx.coroutines.CancellationException) return
        val http = t as? retrofit2.HttpException
        if (http?.code() == 429) {
            val headers = http.response()?.headers()
            // Frank-Bugfix 2026-05-23 (Iteration 2): Strava hat ZWEI getrennte Limits mit
            // eigenen Headern:
            //  - X-RateLimit-*      = Gesamt-Limit (ALLE Requests), Format "15min,Tag"
            //  - X-ReadRateLimit-*  = separates LESE-Limit (Daten-Abfragen), oft NIEDRIGER
            // Unsere Sync-Calls sind reine Reads. Frueher lasen wir nur das Gesamt-Limit —
            // das sah mit z.B. "1035/2000" harmlos aus, waehrend in Wahrheit das Lese-
            // TAGES-Limit erschoepft war. Ein 16-Min-Cooldown half da nicht: das Tages-Limit
            // setzt sich erst um Mitternacht UTC zurueck. Darum: bei erschoepftem Tages-Limit
            // (Lese ODER Gesamt) pausieren wir bis Mitternacht UTC, sonst 16 Min (15-Min-Fenster).
            val readUsage = headers?.get("X-ReadRateLimit-Usage")
            val readLimit = headers?.get("X-ReadRateLimit-Limit")
            val overallUsage = headers?.get("X-RateLimit-Usage")
            val overallLimit = headers?.get("X-RateLimit-Limit")

            val dailyExhausted =
                isDailyExhausted(readUsage, readLimit) || isDailyExhausted(overallUsage, overallLimit)
            val cooldownMs = if (dailyExhausted) msUntilMidnightUtc() else RATE_LIMIT_COOLDOWN_MS
            secrets.stravaRateLimitedUntilMs = System.currentTimeMillis() + cooldownMs

            val pauseText =
                if (dailyExhausted) "Tageslimit erschoepft — pausiert bis Mitternacht UTC (~${cooldownMs / 60_000L} Min)"
                else "15-Min-Limit erreicht — pausiert 16 Min"
            Diag.e(DiagnosticArea.STRAVA, 
                TAG,
                "Strava 429 — Lese=$readUsage/$readLimit Gesamt=$overallUsage/$overallLimit; $pauseText",
            )
            diagnostics.error(
                DiagnosticArea.STRAVA,
                "Rate-Limit erreicht (HTTP 429). $pauseText. " +
                    "Lese-Limit ${readUsage ?: "?"} von ${readLimit ?: "?"}, " +
                    "Gesamt ${overallUsage ?: "?"} von ${overallLimit ?: "?"} (je 15-Min,Tag).",
                t,
            )
        } else if (http?.code() == 403) {
            // Diagnose-Sonde 2026-07-03 (#47451, Vorfall "Trainings kommen nicht mehr an"):
            // Der 403-Fehler-Body wurde bisher weggeworfen — dabei steht dort die Ursache.
            // Strava antwortet bei fehlender Token-Berechtigung mit {"message":"Authorization
            // Error","errors":[{"resource":"AccessToken","field":"activity:read_permission",
            // "code":"missing"}]} — passiert, wenn beim Strava-Login die (abwaehlbare!)
            // Consent-Checkbox "Aktivitaeten ansehen" nicht gesetzt war. Body genau EINMAL
            // lesen (Almanach retrofit S5).
            val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
            val scopeMissing =
                body?.contains("activity:read_permission") == true ||
                    body?.contains("Authorization Error") == true
            val hint =
                if (scopeMissing) {
                    " — dem Token fehlt die Aktivitaets-Berechtigung! Fix: In den App-" +
                        "Einstellungen Strava trennen und NEU verbinden; beim Strava-Consent " +
                        "ALLE Haekchen (\"Aktivitaeten ansehen\") gesetzt lassen."
                } else {
                    ""
                }
            Diag.e(DiagnosticArea.STRAVA, TAG, "Strava 403 Forbidden — Body: ${body ?: "(leer)"}$hint", t)
            diagnostics.error(
                DiagnosticArea.STRAVA,
                "Strava-Zugriff verweigert (HTTP 403): ${body ?: "(kein Fehler-Body)"}$hint",
                t,
            )
        } else {
            Diag.e(DiagnosticArea.STRAVA, TAG, "Strava-Sync fehlgeschlagen", t)
            diagnostics.error(
                DiagnosticArea.STRAVA,
                "Sync fehlgeschlagen: ${t.message ?: t::class.java.simpleName}",
                t,
            )
        }
    }

    /**
     * Prueft ob der TAGES-Anteil eines Strava-Limit-Headers erschoepft ist. Strava-Header haben
     * das Format "15min,Tag" (z.B. Usage "12,1850", Limit "200,2000"). Wir vergleichen den
     * zweiten Wert (Tag). null/unparsebar → false (nicht als erschoepft werten).
     */
    private fun isDailyExhausted(usage: String?, limit: String?): Boolean {
        val u = usage?.split(",")?.getOrNull(1)?.trim()?.toIntOrNull() ?: return false
        val l = limit?.split(",")?.getOrNull(1)?.trim()?.toIntOrNull() ?: return false
        return l > 0 && u >= l
    }

    /** Millisekunden bis zum naechsten Mitternacht UTC — dann setzt Strava seine Tages-Limits zurueck. */
    private fun msUntilMidnightUtc(): Long {
        val now = java.time.Instant.now()
        val nextMidnight =
            now.atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
        return (nextMidnight.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(RATE_LIMIT_COOLDOWN_MS)
    }

    /**
     * Map ein Strava-Workout auf AmazfitWorkoutEntity. Strava liefert m/s — wir
     * konvertieren auf sec/km fuer Pace.
     */
    private fun summaryToEntity(
        summary: StravaActivitySummary,
        detail: StravaActivityDetail?,
        streams: Map<String, StravaStream>,
        laps: List<de.frank.entropyreducer.data.remote.strava.StravaLap>,
    ): AmazfitWorkoutEntity {
        val startMs = parseStartMs(summary.startDateUtc, summary.startDateLocal)
        // Frank-Wunsch 2026-05-24: "Dauer" = verstrichene Zeit (elapsed_time), NICHT Bewegungszeit
        // (moving_time). Stravas moving_time stoppt bei Mini-Pausen (z.B. 2s Gehen nach einem Sprint);
        // Frank will die Gesamtzeit Start→Stopp wie auf der Polar-Uhr. durationSeconds + endMs nutzen
        // daher elapsedTime.
        val duration = summary.elapsedTime ?: summary.movingTime ?: 0L
        // Bewegungszeit bleibt fuer die Schrittlaengen-Schaetzung erhalten: die Kadenz (rpm) gilt nur
        // waehrend der Bewegung — mit verstrichener Zeit (inkl. Pausen) wuerde die Schrittzahl
        // ueberschaetzt und die Schrittlaenge zu kurz berechnet.
        val movingDuration = summary.movingTime ?: summary.elapsedTime ?: 0L
        val endMs = startMs + duration * 1000L
        val zone = ZoneId.systemDefault()
        val dateKey = Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate().toString()

        // m/s → sec/km: 1000 / speed
        val avgPaceSecPerKm = summary.averageSpeed?.takeIf { it > 0 }?.let { 1000.0 / it }
        val maxPaceSecPerKm = summary.maxSpeed?.takeIf { it > 0 }?.let { 1000.0 / it }

        // Frank-Wunsch 2026-05-17: Streams im Polar-Bulk-Tupel-Format
        // `[[ts, value], ...]` schreiben — das Format das der UI-Parser
        // (AmazfitTrainingDetailScreen.parseJsonTimestampValueAsIntList /
        // parseJsonTimestampValueAsDoubleList) erwartet. Frueher wurden
        // flache Arrays `[100, 105, 102]` geschrieben → UI zeigte "Format-
        // Fehler" weil der Parser bei valueIndex=1 ins Leere griff.
        val timeStream = streams["time"]?.data
        // GPS-Track als [[lat, lng], ...] — bleibt unveraendert, UI-Parser
        // versteht das (parseGpsPoints).
        val gpsJson = streams["latlng"]?.data?.let { latlngArr ->
            buildJsonArray {
                latlngArr.forEach { pair -> add(pair) }
            }.toString()
        }
        // Pulsverlauf: [[time_seconds, hr_bpm], ...]
        val hrJson = streams["heartrate"]?.data?.let { hrArr ->
            buildPairedTimestampArray(timeStream, hrArr)
        }
        // Tempo-Verlauf: velocity_smooth (m/s) -> sec/km, gepaart mit time.
        // Format: [[time_seconds, sec_per_km], ...]
        val paceStreamJson = streams["velocity_smooth"]?.data?.let { speedArr ->
            buildPairedTimestampArray(timeStream, speedArr) { speedElem ->
                val v = (speedElem as? JsonPrimitive)?.content?.toDoubleOrNull()
                if (v != null && v > 0.1) JsonPrimitive(1000.0 / v) else null
            }
        }
        // Pace pro Kilometer (km-Splits): UI nutzt parsePipeDoubleList das ein
        // ";"-getrenntes Zepp-Format erwartet, KEIN JSON. Format z.B. "600.5;595.2;610.3"
        // mit Pace in sec/km pro Kilometer.
        val splitsZeppFormat = detail?.splitsMetric?.let { splits ->
            splits.mapNotNull { split ->
                val avgSpeed = split.averageSpeed ?: return@mapNotNull null
                if (avgSpeed <= 0.1) return@mapNotNull null
                val secPerKm = 1000.0 / avgSpeed
                "%.1f".format(java.util.Locale.US, secPerKm)
            }.joinToString(";")
        }?.takeIf { it.isNotBlank() }
        // splitsJson behalten wir zusaetzlich als Rohdaten falls spaeter
        // gebraucht (Hoehenmeter pro Split, HR pro Split). Nutzt nichts
        // im aktuellen UI, schadet aber auch nichts.
        val splitsJson = detail?.splitsMetric?.let { splits ->
            buildJsonArray {
                splits.forEach { split ->
                    add(buildJsonArray {
                        add(JsonPrimitive(split.split ?: 0))
                        add(JsonPrimitive(split.distance ?: 0.0))
                        add(JsonPrimitive(split.movingTime ?: 0L))
                        add(JsonPrimitive(split.averageHeartrate ?: 0.0))
                        add(JsonPrimitive(split.elevationDifference ?: 0.0))
                    })
                }
            }.toString()
        }

        // Frank-Wunsch 2026-05-17: Hoehenverlust aus altitude-Stream
        // berechnen (Strava Summary hat nur total_elevation_gain). Wir
        // summieren alle negativen Hoehen-Deltas.
        val altitudeLossMeters: Double? = streams["altitude"]?.data?.let { altArr ->
            var loss = 0.0
            var prev: Double? = null
            for (el in altArr) {
                val curr = (el as? JsonPrimitive)?.content?.toDoubleOrNull() ?: continue
                val p = prev
                if (p != null && curr < p) loss += (p - curr)
                prev = curr
            }
            if (loss > 0.0) loss else null
        }

        // Frank-Wunsch 2026-05-17: Schrittlaenge aus Cadence + Distance + Dauer.
        // Strava cadence ist rpm pro Bein (typ. 80-90 fuer Laeufer). Schritte
        // gesamt = cadence * 2 * duration_minutes. Stride = distance / Schritte.
        val strideLengthCm: Int? = run {
            val dist = summary.distance ?: return@run null
            val cad = summary.averageCadence ?: return@run null
            if (cad <= 0.0 || dist <= 0.0 || movingDuration <= 0) return@run null
            val durationMin = movingDuration / 60.0
            val totalSteps = cad * 2.0 * durationMin
            if (totalSteps <= 0.0) return@run null
            val strideM = dist / totalSteps
            if (strideM in 0.4..2.5) (strideM * 100.0).toInt() else null
        }

        return AmazfitWorkoutEntity(
            trackId = "strava_${summary.id}",
            dateKey = dateKey,
            startMs = startMs,
            endMs = endMs,
            durationSeconds = duration,
            // Strava sport_type ist ein String wie "TrailRun", "Run". Wir mappen
            // die wichtigsten auf deutsche Namen damit die UI konsistent ist mit
            // Zepp-Workouts. Bei unbekanntem Wert nehmen wir den Strava-String roh.
            sportType = stravaSportTypeNumeric(summary.sportType),
            sportName = stravaSportTypeGerman(summary.sportType ?: summary.activityType, summary.name),
            distanceMeters = summary.distance,
            avgPaceSecPerKm = avgPaceSecPerKm,
            maxPaceSecPerKm = maxPaceSecPerKm,
            avgSpeedKmh = summary.averageSpeed?.times(3.6),
            maxSpeedKmh = summary.maxSpeed?.times(3.6),
            calories = detail?.calories ?: summary.calories,
            avgHeartRate = summary.averageHeartrate?.toInt(),
            maxHeartRate = summary.maxHeartrate?.toInt(),
            gpsTrackJson = gpsJson,
            heartRateSeriesJson = hrJson,
            // paceSeriesJson = Pace pro Kilometer (Zepp-Format ";"-getrennt
            // sec/km-Werte) — wird von parsePipeDoubleList gelesen.
            paceSeriesJson = splitsZeppFormat,
            splitsJson = splitsJson,
            altitudeGainMeters = summary.totalElevationGain,
            altitudeLossMeters = altitudeLossMeters,
            trainingEffectAerobic = null,
            trainingEffectAnaerobic = null,
            cadence = summary.averageCadence?.toInt(),
            strideLengthCm = strideLengthCm,
            swolf = null,
            poolLengthMeters = null,
            source = "strava",
            city = null,
            paceStreamJson = paceStreamJson,
            createdAt = System.currentTimeMillis(),
        )
    }

    /**
     * Frank-Wunsch 2026-05-17: Helfer fuer das Polar-Bulk-Tupel-Format
     * `[[time_seconds, value], ...]`. Paart die [time]-Stream-Eintraege
     * mit den [value]-Stream-Eintraegen (gleiche Index-Position) und liefert
     * ein JSON-Array-String das der UI-Parser
     * (parseJsonTimestampValueAsIntList / parseJsonTimestampValueAsDoubleList)
     * direkt verarbeiten kann.
     *
     * Wenn [time] null ist, wird die Index-Position selbst als Pseudo-Zeit
     * verwendet — der Parser braucht im Polar-Bulk-Pfad nur valueIndex=1, die
     * Time-Position wird ignoriert. So funktioniert es auch bei Strava-Activities
     * ohne separaten time-Stream (selten, aber moeglich).
     *
     * @param valueTransform Optionale Transformation pro Wert (z.B. m/s → sec/km).
     *   Liefert null → Sample wird uebersprungen.
     */
    private fun buildPairedTimestampArray(
        time: JsonArray?,
        values: JsonArray,
        valueTransform: (kotlinx.serialization.json.JsonElement) -> JsonPrimitive? = { el ->
            (el as? JsonPrimitive)
        },
    ): String {
        return buildJsonArray {
            values.forEachIndexed { idx, valueEl ->
                val transformed = valueTransform(valueEl) ?: return@forEachIndexed
                val ts = (time?.getOrNull(idx) as? JsonPrimitive) ?: JsonPrimitive(idx)
                add(
                    buildJsonArray {
                        add(ts)
                        add(transformed)
                    },
                )
            }
        }.toString()
    }

    private fun parseStartMs(utcIso: String?, localIso: String?): Long {
        // start_date ist UTC ISO-8601: "2026-05-15T15:32:11Z"
        if (!utcIso.isNullOrBlank()) {
            return runCatching { Instant.parse(utcIso).toEpochMilli() }.getOrElse { 0L }
        }
        // Fallback: start_date_local ohne Zone — als systemDefault interpretieren
        if (!localIso.isNullOrBlank()) {
            return runCatching {
                ZonedDateTime.parse(localIso + "Z").toInstant().toEpochMilli()
            }.getOrElse { 0L }
        }
        return 0L
    }

    /**
     * Strava sport_type ist ein String. Wir versuchen ihm eine numerische ID zu
     * geben damit die existierende Mini-Card-Logik (die nach sportType filtert)
     * funktioniert. Wenn unbekannt → null (UI faellt auf sportName zurueck).
     */
    private fun stravaSportTypeNumeric(sportType: String?): Int? = when (sportType?.lowercase()) {
        "run", "trailrun" -> 1            // Frank's T-Rex 3 nutzt 7 fuer Trail, 1 fuer Outdoor-Run
        "ride", "mountainbikeride" -> 6
        "swim" -> 14
        "walk", "hike" -> 9
        "weighttraining" -> 52            // konsistent mit Zepp's Krafttraining
        "workout" -> 0
        else -> null
    }

    /**
     * Deutsche Bezeichnung — Strava hat keinen "Trailrunning"-String, nur "TrailRun".
     * Wenn der Activity-Name aussagekraeftig ist (z.B. "Morgenrunde Wald"), bevorzugen
     * wir den nicht — der ist zu speziell. sportType ist robuster.
     */
    private fun stravaSportTypeGerman(sportType: String?, name: String?): String = when (sportType?.lowercase()) {
        "run" -> "Laufen"
        "trailrun" -> "Trailrunning"
        "treadmillrun" -> "Laufband"
        "virtualrun" -> "Laufen (virtuell)"
        "ride" -> "Radfahren"
        "mountainbikeride" -> "Mountainbike"
        "gravelride" -> "Gravel"
        "virtualride" -> "Radfahren (virtuell)"
        "ebikeride" -> "E-Bike"
        "swim" -> "Schwimmen"
        "walk" -> "Walking"
        "hike" -> "Wandern"
        "weighttraining" -> "Krafttraining"
        "workout" -> "Training"
        "yoga" -> "Yoga"
        "rowing" -> "Rudern"
        "alpineski" -> "Ski"
        "snowboard" -> "Snowboard"
        "elliptical" -> "Crosstrainer"
        "stairstepper" -> "Stepper"
        "rockclimbing" -> "Klettern"
        else -> sportType?.takeIf { it.isNotBlank() } ?: (name ?: "Training")
    }

    companion object {
        private const val TAG = "StravaRepository"

        /** 16 Min — etwas mehr als Stravas 15-Min-Rate-Limit-Fenster, damit der naechste Versuch
         * garantiert in einem frischen Fenster landet. */
        private const val RATE_LIMIT_COOLDOWN_MS = 16L * 60L * 1000L

        /** Sicherheits-Ueberlapp fuer inkrementelle Syncs: 7 Tage vor dem letzten Sync. */
        private const val INCREMENTAL_OVERLAP_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
