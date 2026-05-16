package de.frank.entropyreducer.data.remote.polar

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray

/**
 * Wandelt Polar-Sample-Streams ins JSON-Format um, das AmazfitWorkoutEntity
 * erwartet — damit die bestehenden Detail-Screens (Pulsverlauf-Chart, GPS-Karte,
 * Pace-Verlauf) ohne UI-Aenderung weiterarbeiten koennen.
 *
 * Ziel-Formate (definiert von AmazfitWorkoutEntity / AmazfitTrainingDetailScreen):
 *  - `heartRateSeriesJson`: `[[ts, hr], ...]`  Pulsverlauf mit Unix-Millis-ts
 *  - `paceStreamJson`:      `[[ts, paceSecPerKm], ...]`  hochaufgeloester Tempo-Verlauf
 *  - `gpsTrackJson`:        `[[lat, lon, alt, ts], ...]`  GPS-Track aus GPX zusammengefuegt
 *
 * Eingang: Polar-Streams (kommagetrennte Strings + recording-rate in Sekunden).
 * Erster Sample-Wert ist oft 0 = "nicht verfuegbar" — wir filtern Nullen aus
 * dem HR-Stream weil 0 bpm physikalisch sinnlos sind. Bei Speed ist 0 jedoch
 * gueltig (Stillstand an der Ampel) → keine Filterung.
 */
object PolarSampleMapper {

    private val JSON = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * Wandelt einen Polar-Sample-Stream in `List<Double?>`. Leere Strings oder
     * nicht-numerische Werte werden zu null. Erster Wert (oft 0 = Marker) bleibt
     * — die Filterung passiert spaeter pro Stream-Typ.
     */
    fun parseValues(sample: PolarSample): List<Double?> {
        if (sample.data.isBlank()) return emptyList()
        return sample.data.split(",").map { raw ->
            raw.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        }
    }

    /**
     * Heart-Rate-Stream zu JSON `[[ts, hr], ...]`.
     * Werte = 0 oder null werden uebersprungen (kein gueltiger Puls).
     */
    fun heartRateToJson(sample: PolarSample, startEpochMs: Long): String {
        val rate = sample.recordingRate.coerceAtLeast(1)
        val values = parseValues(sample)
        val arr = buildJsonArray {
            values.forEachIndexed { idx, v ->
                val bpm = v?.toInt() ?: return@forEachIndexed
                if (bpm <= 0) return@forEachIndexed
                val ts = startEpochMs + idx.toLong() * rate.toLong() * 1000L
                addJsonArray {
                    add(JsonPrimitive(ts))
                    add(JsonPrimitive(bpm))
                }
            }
        }
        return JSON.encodeToString(JsonArray.serializer(), arr)
    }

    /**
     * Speed-Stream (km/h) zu Pace-Stream-JSON `[[ts, paceSecPerKm], ...]`.
     * Pace = 3600 / km_h Sekunden pro Kilometer.
     * Speed = 0 oder negativ → null-Eintrag (keine Pace bei Stillstand).
     */
    fun speedToPaceJson(sample: PolarSample, startEpochMs: Long): String {
        val rate = sample.recordingRate.coerceAtLeast(1)
        val values = parseValues(sample)
        val arr = buildJsonArray {
            values.forEachIndexed { idx, kmh ->
                val ts = startEpochMs + idx.toLong() * rate.toLong() * 1000L
                addJsonArray {
                    add(JsonPrimitive(ts))
                    if (kmh != null && kmh > 0.0) {
                        val paceSecPerKm = 3600.0 / kmh
                        add(JsonPrimitive(paceSecPerKm))
                    } else {
                        add(JsonPrimitive(null as String?))
                    }
                }
            }
        }
        return JSON.encodeToString(JsonArray.serializer(), arr)
    }

    /**
     * Berechnet Avg-Pace (Sekunden pro Kilometer) aus Distanz und Dauer.
     * Null wenn einer der Werte fehlt oder Distanz = 0.
     */
    fun computeAvgPaceSecPerKm(distanceMeters: Double?, durationSeconds: Long?): Double? {
        if (distanceMeters == null || distanceMeters <= 0.0 || durationSeconds == null || durationSeconds <= 0L) {
            return null
        }
        val km = distanceMeters / 1000.0
        return durationSeconds / km
    }

    /**
     * Berechnet Avg-Speed (km/h) aus Distanz und Dauer.
     */
    fun computeAvgSpeedKmh(distanceMeters: Double?, durationSeconds: Long?): Double? {
        if (distanceMeters == null || durationSeconds == null || durationSeconds <= 0L) return null
        return (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
    }

    /**
     * Findet den schnellsten Sekundenabschnitt im Speed-Stream und liefert die
     * dortige Pace als Sekunden-pro-Kilometer.
     */
    fun maxPaceFromSpeedStream(sample: PolarSample): Double? {
        val values = parseValues(sample).mapNotNull { it?.takeIf { v -> v > 0.0 } }
        if (values.isEmpty()) return null
        val maxKmh = values.max()
        if (maxKmh <= 0.0) return null
        return 3600.0 / maxKmh
    }

    /** Maximale Geschwindigkeit aus Speed-Stream in km/h. */
    fun maxSpeedKmhFromSpeedStream(sample: PolarSample): Double? {
        val values = parseValues(sample).mapNotNull { it?.takeIf { v -> v > 0.0 } }
        return values.maxOrNull()
    }

    /** Durchschnittswert eines Sample-Streams (z.B. RUN_CADENCE). Null wenn leer. */
    fun avgFromSample(sample: PolarSample): Double? {
        val values = parseValues(sample).mapNotNull { it?.takeIf { v -> v > 0.0 } }
        if (values.isEmpty()) return null
        return values.average()
    }

    /**
     * Hoehengewinn — alle positiven Differenzen aufaddieren. Polar liefert
     * in der Exercise-Summary keinen vorberechneten "ascent"-Wert.
     */
    fun altitudeGainFromSample(sample: PolarSample): Double? {
        val values = parseValues(sample).filterNotNull()
        if (values.size < 2) return null
        var gain = 0.0
        for (i in 1 until values.size) {
            val delta = values[i] - values[i - 1]
            if (delta > 0.0) gain += delta
        }
        return if (gain > 0.5) gain else null
    }

    /**
     * Hoehenverlust analog zum Hoehengewinn — alle negativen Differenzen
     * aufaddieren. Polar liefert in der Exercise-Summary keinen "descent"-Wert.
     */
    fun altitudeLossFromAltitudeStream(sample: PolarSample): Double? {
        val values = parseValues(sample).filterNotNull()
        if (values.size < 2) return null
        var loss = 0.0
        for (i in 1 until values.size) {
            val delta = values[i] - values[i - 1]
            if (delta < 0.0) loss += -delta
        }
        return if (loss > 0.5) loss else null
    }

    /**
     * Schrittlaenge in cm aus Distanz und Run-Cadence.
     * Schritte_total = run_cadence_avg [spm] * duration_min
     * Schrittlaenge_m = distance_m / Schritte_total
     */
    fun strideLengthCmFromCadenceAndDistance(
        runCadenceAvgSpm: Double?,
        distanceMeters: Double?,
        durationSeconds: Long?,
    ): Int? {
        if (runCadenceAvgSpm == null || runCadenceAvgSpm <= 0.0) return null
        if (distanceMeters == null || distanceMeters <= 0.0) return null
        if (durationSeconds == null || durationSeconds <= 0L) return null
        val totalSteps = runCadenceAvgSpm * (durationSeconds / 60.0)
        if (totalSteps <= 0.0) return null
        val strideM = distanceMeters / totalSteps
        // Plausibilitaet: Schrittlaenge Laufen 50-200 cm
        val strideCm = (strideM * 100.0).toInt()
        return if (strideCm in 30..250) strideCm else null
    }

    /**
     * Km-Splits aus kumuliertem Distance-Stream — fuer jeden vollen Kilometer
     * wird die Zeit dorthin festgehalten und die Pace berechnet (sec/km).
     * Ergebnis als Pipe-Format `"308.5|315.2|..."` (passend zu parsePipeDoubleList
     * im Detail-Screen).
     *
     * @param distSample Distance-Stream (Type "10"), Werte in Metern kumulativ
     * @return Pipe-getrennte Km-Pace-Liste oder null wenn nicht genug Daten
     */
    fun splitsFromDistanceStream(distSample: PolarSample): String? {
        val rate = distSample.recordingRate.coerceAtLeast(1)
        val values = parseValues(distSample)
        if (values.size < 2) return null
        // Distance ist kumulativ — wann wurde jeder Kilometer erreicht?
        val kmTimesSeconds = mutableListOf<Double>() // Zeitpunkt fuer 1km, 2km, ...
        var nextKm = 1
        var lastValue = 0.0
        var lastIdx = 0
        for ((idx, v) in values.withIndex()) {
            val d = v ?: continue
            if (d < lastValue) { lastValue = d; continue } // Reset/Glitch
            while (d >= nextKm * 1000.0) {
                // Interpolation zwischen idx-1 (lastValue) und idx (d):
                val prevDist = if (idx == 0) 0.0 else (values[lastIdx] ?: lastValue)
                val target = nextKm * 1000.0
                val span = d - prevDist
                val frac = if (span > 0.0) (target - prevDist) / span else 0.0
                val t = (lastIdx + frac * (idx - lastIdx)) * rate.toDouble()
                kmTimesSeconds += t
                nextKm++
            }
            lastValue = d
            lastIdx = idx
        }
        if (kmTimesSeconds.isEmpty()) return null
        // Splits = Zeit pro Km = Differenz zur Vor-Km-Zeit (in Sekunden pro Km)
        val splits = buildList {
            var prev = 0.0
            for (t in kmTimesSeconds) {
                add(t - prev)
                prev = t
            }
        }
        // Plausibilitaetsfilter: 2:00..30:00 pro Km
        val cleaned = splits.filter { it in 120.0..1800.0 }
        if (cleaned.isEmpty()) return null
        return cleaned.joinToString("|") { "%.1f".format(it).replace(",", ".") }
    }

    /**
     * Wandelt Polar's GPX-XML in das App-interne GPS-Track-JSON-Format
     * `[[lat, lon, alt, ts], ...]`.
     *
     * GPX-Struktur (vereinfacht):
     *   <gpx>
     *     <trk>
     *       <trkseg>
     *         <trkpt lat="60.1234" lon="24.5678">
     *           <ele>15.3</ele>
     *           <time>2026-05-16T13:42:01Z</time>
     *         </trkpt>
     *         ...
     *
     * Wir nutzen XmlPullParser (Android-built-in, keine Dependency).
     * Bei Parse-Fehler liefern wir null statt einer Exception, damit der Sync
     * weiterlaeuft.
     */
    fun parseGpxToTrackJson(gpxXml: String): String? {
        if (gpxXml.isBlank()) return null
        return runCatching {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(gpxXml))
            val points = mutableListOf<Triple<Double, Double, Pair<Double?, Long?>>>()
            var lat: Double? = null
            var lon: Double? = null
            var ele: Double? = null
            var ts: Long? = null
            var current = ""
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (event) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        current = parser.name
                        if (current.equals("trkpt", ignoreCase = true)) {
                            lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            ele = null
                            ts = null
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            when (current.lowercase()) {
                                "ele" -> ele = text.toDoubleOrNull()
                                "time" -> ts = runCatching {
                                    java.time.Instant.parse(text).toEpochMilli()
                                }.getOrNull()
                            }
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name.equals("trkpt", ignoreCase = true)) {
                            val la = lat; val lo = lon
                            if (la != null && lo != null) {
                                points += Triple(la, lo, ele to ts)
                            }
                            lat = null; lon = null; ele = null; ts = null
                        }
                        current = ""
                    }
                }
                event = parser.next()
            }
            if (points.isEmpty()) return@runCatching null
            val arr = buildJsonArray {
                for ((la, lo, alt_ts) in points) {
                    addJsonArray {
                        add(JsonPrimitive(la))
                        add(JsonPrimitive(lo))
                        val a = alt_ts.first
                        if (a != null) add(JsonPrimitive(a)) else add(JsonPrimitive(0.0))
                        val t = alt_ts.second
                        if (t != null) add(JsonPrimitive(t)) else add(JsonPrimitive(0L))
                    }
                }
            }
            JSON.encodeToString(JsonArray.serializer(), arr)
        }.getOrNull()
    }

    /**
     * Schaetzt VO2Max nach ACSM-Lauf-Formel + Karvonen-HR-Reserve + Frank-Offset.
     * Identisch zur estimateVo2Max() im AmazfitTrainingDetailScreen, aber als
     * Repository-Funktion damit wir die Berechnung beim Sync persistieren koennen.
     * Frank-Wunsch 2026-05-16: gleiche Formel wie bei Zepp, auch fuer API-Lauf.
     *
     * Schritt 1: VO2 bei diesem Workout = 0.2 * Speed (m/min) + 3.5
     * Schritt 2: HR-Reserve-Anteil = (avgHr - restHr) / (maxHr - restHr)
     * Schritt 3: VO2Max = VO2_workout / HR_reserve + 2.0 (Frank-Empirie)
     *
     * Frank's Konstanten: maxHr=180, restHr=65.
     */
    fun estimateVo2Max(
        distanceMeters: Double?,
        durationSeconds: Long?,
        avgHr: Int?,
    ): Double? {
        if (distanceMeters == null || distanceMeters < 200.0) return null
        if (durationSeconds == null || durationSeconds <= 60L) return null
        if (avgHr == null || avgHr < 60) return null
        val maxHr = 180
        val restHr = 65
        val durationMin = durationSeconds / 60.0
        val speedMperMin = distanceMeters / durationMin
        val vo2Workout = 0.2 * speedMperMin + 3.5
        val hrReserveFraction = (avgHr - restHr).toDouble() / (maxHr - restHr).toDouble()
        if (hrReserveFraction <= 0.1) return null
        val vo2 = vo2Workout / hrReserveFraction + 2.0
        return vo2.takeIf { it in 20.0..80.0 }
    }

    /**
     * Parst ISO-8601-Duration ("PT1H30M45S") in Sekunden. Polar-spezifisch:
     * Bruchteile (z.B. "PT45.5S") werden aufgerundet.
     */
    fun parseIsoDurationToSeconds(duration: String?): Long? {
        if (duration.isNullOrBlank()) return null
        return runCatching {
            java.time.Duration.parse(duration).seconds
        }.getOrNull()
    }

    /**
     * Parst Polar's "start-time" + "start-time-utc-offset" (Minuten) in Unix-Millis.
     * start-time hat keine Zeitzone — wir kombinieren sie mit dem Offset zu einem
     * OffsetDateTime.
     */
    fun parseStartTimeToEpochMs(startTime: String, utcOffsetMinutes: Int): Long? {
        return runCatching {
            val offset = java.time.ZoneOffset.ofTotalSeconds(utcOffsetMinutes * 60)
            // start-time kann mit oder ohne Sekundenbruchteile kommen.
            val local = java.time.LocalDateTime.parse(startTime)
            local.atOffset(offset).toInstant().toEpochMilli()
        }.getOrNull()
    }

    /**
     * Mappt Polar's Sport-Enum auf einen deutschen Namen.
     * Polar liefert ALL_CAPS-Strings wie "RUNNING", "CYCLING".
     * Bei detailedSportInfo zaehlt dieser Vorrang — der ist genauer (z.B. "WATERSPORTS_WATERSKI").
     */
    fun mapSportToGerman(sport: String?, detailedSportInfo: String? = null): String {
        val key = (detailedSportInfo ?: sport)?.uppercase() ?: return "Training"
        return SPORT_MAP[key] ?: when {
            key.startsWith("RUNNING") || key.endsWith("_RUNNING") -> "Laufen"
            key.contains("TRAIL") -> "Trailrunning"
            key.startsWith("CYCLING") || key.contains("BIKE") -> "Radfahren"
            key.contains("SWIM") -> "Schwimmen"
            key.contains("WALK") -> "Walking"
            key.contains("HIKE") || key.contains("HIKING") -> "Wandern"
            key.contains("STRENGTH") || key.contains("WEIGHT") -> "Krafttraining"
            key.contains("YOGA") -> "Yoga"
            key.contains("ELLIPTICAL") -> "Crosstrainer"
            key.contains("ROWING") -> "Rudern"
            key.contains("CLIMB") -> "Klettern"
            key.contains("SKI") -> "Skifahren"
            else -> "Training"
        }
    }

    /**
     * Mappt Polar's Sport-Enum auf einen numerischen Code analog zu Zepp/HC.
     * Wir nutzen die Health-Connect-ExerciseType-Codes, weil AmazfitRepository
     * dort schon eine vollstaendige Mapping-Tabelle hat.
     */
    fun mapSportToHealthConnectType(sport: String?, detailedSportInfo: String? = null): Int? {
        val key = (detailedSportInfo ?: sport)?.uppercase() ?: return null
        return SPORT_TO_HC_CODE[key] ?: when {
            key.startsWith("RUNNING") || key.endsWith("_RUNNING") -> 56  // RUNNING
            key.contains("TRAIL") -> 56                                  // ebenfalls als RUNNING
            key.startsWith("CYCLING") || key.contains("BIKE") -> 8
            key.contains("SWIM") -> 74
            key.contains("WALK") -> 79
            key.contains("HIKE") -> 37
            key.contains("STRENGTH") || key.contains("WEIGHT") -> 70
            key.contains("YOGA") -> 83
            key.contains("ELLIPTICAL") -> 25
            key.contains("ROWING") -> 54
            key.contains("CLIMB") -> 51
            else -> null
        }
    }

    /**
     * Bekannte Polar-Sport-Strings aus der AccessLink-Doku.
     * Liste wird bei Bedarf erweitert sobald reale Trainings reinkommen.
     */
    private val SPORT_MAP: Map<String, String> = mapOf(
        "RUNNING" to "Laufen",
        "JOGGING" to "Joggen",
        "ROAD_RUNNING" to "Laufen (Strasse)",
        "TRAIL_RUNNING" to "Trailrunning",
        "TREADMILL_RUNNING" to "Laufband",
        "CYCLING" to "Radfahren",
        "ROAD_BIKING" to "Rennrad",
        "MOUNTAIN_BIKING" to "Mountainbike",
        "INDOOR_CYCLING" to "Indoor-Radfahren",
        "WALKING" to "Walking",
        "NORDIC_WALKING" to "Nordic Walking",
        "HIKING" to "Wandern",
        "POOL_SWIMMING" to "Schwimmen (Pool)",
        "OPEN_WATER_SWIMMING" to "Freiwasser-Schwimmen",
        "SWIMMING" to "Schwimmen",
        "STRENGTH_TRAINING" to "Krafttraining",
        "FUNCTIONAL_TRAINING" to "Funktionelles Training",
        "CIRCUIT_TRAINING" to "Zirkeltraining",
        "YOGA" to "Yoga",
        "PILATES" to "Pilates",
        "STRETCHING" to "Stretching",
        "ELLIPTICAL" to "Crosstrainer",
        "ROWING" to "Rudern",
        "INDOOR_ROWING" to "Rudergeraet",
        "CLIMBING" to "Klettern",
        "INDOOR_CLIMBING" to "Bouldern",
        "BOULDERING" to "Bouldern",
        "CROSS_COUNTRY_SKIING" to "Skilanglauf",
        "DOWNHILL_SKIING" to "Skifahren",
        "SNOWBOARDING" to "Snowboard",
        "ICE_SKATING" to "Eislaufen",
        "TENNIS" to "Tennis",
        "BADMINTON" to "Badminton",
        "SQUASH" to "Squash",
        "TABLE_TENNIS" to "Tischtennis",
        "GOLF" to "Golf",
        "SOCCER" to "Fussball",
        "BASKETBALL" to "Basketball",
        "VOLLEYBALL" to "Volleyball",
        "BOXING" to "Boxen",
        "MARTIAL_ARTS" to "Kampfsport",
        "DANCING" to "Tanzen",
        "AEROBICS" to "Aerobic",
        "OTHER" to "Training",
        "OTHER_INDOOR" to "Indoor-Training",
        "OTHER_OUTDOOR" to "Outdoor-Training",
    )

    private val SPORT_TO_HC_CODE: Map<String, Int> = mapOf(
        "RUNNING" to 56,
        "JOGGING" to 56,
        "ROAD_RUNNING" to 56,
        "TRAIL_RUNNING" to 56,
        "TREADMILL_RUNNING" to 57,
        "CYCLING" to 8,
        "ROAD_BIKING" to 8,
        "MOUNTAIN_BIKING" to 8,
        "INDOOR_CYCLING" to 9,
        "WALKING" to 79,
        "HIKING" to 37,
        "POOL_SWIMMING" to 74,
        "OPEN_WATER_SWIMMING" to 73,
        "SWIMMING" to 74,
        "STRENGTH_TRAINING" to 70,
        "YOGA" to 83,
        "PILATES" to 48,
        "STRETCHING" to 71,
        "ELLIPTICAL" to 25,
        "ROWING" to 53,
        "INDOOR_ROWING" to 54,
        "CLIMBING" to 51,
        "TENNIS" to 76,
        "BADMINTON" to 2,
        "GOLF" to 32,
        "SOCCER" to 64,
        "BASKETBALL" to 5,
        "VOLLEYBALL" to 78,
        "BOXING" to 11,
    )
}
