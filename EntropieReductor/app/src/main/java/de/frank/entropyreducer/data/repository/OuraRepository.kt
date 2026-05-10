package de.frank.entropyreducer.data.repository

import android.util.Log
import de.frank.entropyreducer.data.local.dao.OuraActivityDao
import de.frank.entropyreducer.data.local.dao.OuraDailySleepDao
import de.frank.entropyreducer.data.local.dao.OuraPersonalInfoDao
import de.frank.entropyreducer.data.local.dao.OuraReadinessDao
import de.frank.entropyreducer.data.local.dao.OuraResilienceDao
import de.frank.entropyreducer.data.local.dao.OuraSleepDetailDao
import de.frank.entropyreducer.data.local.entities.OuraActivityEntity
import de.frank.entropyreducer.data.local.entities.OuraDailySleepEntity
import de.frank.entropyreducer.data.local.entities.OuraPersonalInfoEntity
import de.frank.entropyreducer.data.local.entities.OuraReadinessEntity
import de.frank.entropyreducer.data.local.entities.OuraResilienceEntity
import de.frank.entropyreducer.data.local.entities.OuraSleepDetailEntity
import de.frank.entropyreducer.data.remote.oura.OuraApi
import de.frank.entropyreducer.data.remote.oura.OuraDailyActivity
import de.frank.entropyreducer.data.remote.oura.OuraDailyReadiness
import de.frank.entropyreducer.data.remote.oura.OuraDailyResilience
import de.frank.entropyreducer.data.remote.oura.OuraDailySleep
import de.frank.entropyreducer.data.remote.oura.OuraSleep
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Oura Cloud API v2 Anbindung (Frank-Wunsch 2026-05-10). Personal-Access-Token
 * wird verschluesselt im EncryptedSecretsStore gespeichert. Sync-Lauf zieht
 * ueber die Pagination-Schleife alle Tage des angefragten Bereichs nach Hause
 * und schreibt sie in 5+1 Tabellen.
 *
 * Endpunkt-Auswahl (Frank): readiness, sleep-score, activity, resilience,
 * sleep-detail, personal_info — die anderen ~14 Endpunkte werden bewusst
 * NICHT abgerufen um Daten-Wildwuchs zu vermeiden.
 *
 * Rate-Limit ist sehr grosszuegig (5000 req / 5 min), daher kein Backoff noetig.
 * Bei 401 (Token ungueltig) und 429 (Rate-Limit) werfen wir kontrollierte
 * Exceptions die das ViewModel als benutzerfreundliche Banner-Meldung mappt.
 */
@Singleton
class OuraRepository @Inject constructor(
    private val readinessDao: OuraReadinessDao,
    private val sleepDayDao: OuraDailySleepDao,
    private val activityDao: OuraActivityDao,
    private val resilienceDao: OuraResilienceDao,
    private val sleepDetailDao: OuraSleepDetailDao,
    private val personalInfoDao: OuraPersonalInfoDao,
    private val api: OuraApi,
    private val secrets: EncryptedSecretsStore,
) {

    fun observeReadiness(): Flow<List<OuraReadinessEntity>> = readinessDao.getAll()
    fun observeLatestReadiness(): Flow<OuraReadinessEntity?> = readinessDao.getLatest()

    fun observeDailySleep(): Flow<List<OuraDailySleepEntity>> = sleepDayDao.getAll()
    fun observeLatestDailySleep(): Flow<OuraDailySleepEntity?> = sleepDayDao.getLatest()

    fun observeActivity(): Flow<List<OuraActivityEntity>> = activityDao.getAll()
    fun observeLatestActivity(): Flow<OuraActivityEntity?> = activityDao.getLatest()

    fun observeResilience(): Flow<List<OuraResilienceEntity>> = resilienceDao.getAll()
    fun observeLatestResilience(): Flow<OuraResilienceEntity?> = resilienceDao.getLatest()

    fun observeSleepDetails(): Flow<List<OuraSleepDetailEntity>> = sleepDetailDao.getAll()
    fun observeLatestSleepDetail(): Flow<OuraSleepDetailEntity?> = sleepDetailDao.getLatest()
    fun observeSleepDetailsForDay(day: String): Flow<List<OuraSleepDetailEntity>> =
        sleepDetailDao.getByDay(day)

    fun observePersonalInfo(): Flow<OuraPersonalInfoEntity?> = personalInfoDao.observe()

    /** True wenn ein Personal Access Token hinterlegt ist. */
    fun isTokenConfigured(): Boolean = !secrets.ouraPersonalAccessToken.isNullOrBlank()

    /**
     * Loescht ALLE lokalen Oura-Daten plus Token. Wird beim Trennen-Button im
     * Settings-Screen aufgerufen.
     */
    suspend fun clearAll() {
        readinessDao.deleteAll()
        sleepDayDao.deleteAll()
        activityDao.deleteAll()
        resilienceDao.deleteAll()
        sleepDetailDao.deleteAll()
        personalInfoDao.deleteAll()
        secrets.ouraPersonalAccessToken = null
        secrets.ouraLastSyncEpochMs = 0L
    }

    /**
     * Testet das aktuelle Token mit einem GET /personal_info. Wirft
     * IllegalStateException bei fehlendem Token, HttpException bei 401/403/etc.
     * Liefert den OuraUserId-String zurueck wenn alles OK ist.
     */
    suspend fun testToken(): String {
        val token = secrets.ouraPersonalAccessToken
            ?: throw IllegalStateException("Kein Oura-Token gespeichert.")
        val info = api.getPersonalInfo("Bearer $token")
        // Sofort als Stammdaten speichern damit Settings-UI sieht: hat geklappt.
        val now = System.currentTimeMillis()
        personalInfoDao.upsert(
            OuraPersonalInfoEntity(
                id = 1L,
                ouraUserId = info.id,
                age = info.age,
                weightKg = info.weight,
                heightMeters = info.height,
                biologicalSex = info.biologicalSex,
                email = info.email,
                capturedAt = now,
            ),
        )
        return info.id ?: "unbekannt"
    }

    /**
     * Synchronisiert die letzten [days] Tage. Liefert die Anzahl der pro
     * Endpunkt gespeicherten Eintraege als Map zurueck. Wirft IllegalStateException
     * bei fehlendem Token.
     *
     * Default 365 Tage analog zu WhoopRepository fuer einen vollstaendigen
     * Initial-Sync. Folgesyncs ueberschreiben bestehende Tage idempotent
     * (REPLACE-onConflict).
     */
    suspend fun syncLastDays(days: Int = 365): Result<Map<String, Int>> = runCatching {
        val token = secrets.ouraPersonalAccessToken
            ?: throw IllegalStateException("Kein Oura-Token. Bitte unter Einstellungen → API einen Personal Access Token eintragen.")
        val auth = "Bearer $token"

        val today = LocalDate.now()
        val endDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val startDate = today.minusDays(days.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val now = System.currentTimeMillis()

        Log.i(TAG, "Oura Sync gestartet: $startDate bis $endDate ($days Tage)")

        // Personal Info immer am Anfang aktualisieren — minimal teuer (1 Request),
        // aber nuetzlich falls Frank Geburtstag hatte oder Gewicht aktualisiert hat.
        val info = api.getPersonalInfo(auth)
        personalInfoDao.upsert(
            OuraPersonalInfoEntity(
                id = 1L,
                ouraUserId = info.id,
                age = info.age,
                weightKg = info.weight,
                heightMeters = info.height,
                biologicalSex = info.biologicalSex,
                email = info.email,
                capturedAt = now,
            ),
        )

        val readinessCount = pullReadiness(auth, startDate, endDate, now)
        val sleepDayCount = pullDailySleep(auth, startDate, endDate, now)
        val activityCount = pullActivity(auth, startDate, endDate, now)
        val resilienceCount = pullResilience(auth, startDate, endDate, now)
        val sleepDetailCount = pullSleepDetail(auth, startDate, endDate, now)

        secrets.ouraLastSyncEpochMs = now
        Log.i(
            TAG,
            "Oura Sync fertig: readiness=$readinessCount, sleepDay=$sleepDayCount, " +
                "activity=$activityCount, resilience=$resilienceCount, " +
                "sleepDetail=$sleepDetailCount",
        )
        mapOf(
            "readiness" to readinessCount,
            "daily_sleep" to sleepDayCount,
            "activity" to activityCount,
            "resilience" to resilienceCount,
            "sleep_detail" to sleepDetailCount,
        )
    }

    private suspend fun pullReadiness(auth: String, start: String, end: String, now: Long): Int {
        val all = mutableListOf<OuraDailyReadiness>()
        var nextToken: String? = null
        do {
            val page = api.listDailyReadiness(auth, start, end, nextToken)
            all += page.data
            nextToken = page.nextToken
        } while (nextToken != null)
        val entities = all.map { it.toEntity(now) }
        readinessDao.upsertAll(entities)
        return entities.size
    }

    private suspend fun pullDailySleep(auth: String, start: String, end: String, now: Long): Int {
        val all = mutableListOf<OuraDailySleep>()
        var nextToken: String? = null
        do {
            val page = api.listDailySleep(auth, start, end, nextToken)
            all += page.data
            nextToken = page.nextToken
        } while (nextToken != null)
        val entities = all.map { it.toEntity(now) }
        sleepDayDao.upsertAll(entities)
        return entities.size
    }

    private suspend fun pullActivity(auth: String, start: String, end: String, now: Long): Int {
        val all = mutableListOf<OuraDailyActivity>()
        var nextToken: String? = null
        do {
            val page = api.listDailyActivity(auth, start, end, nextToken)
            all += page.data
            nextToken = page.nextToken
        } while (nextToken != null)
        val entities = all.map { it.toEntity(now) }
        activityDao.upsertAll(entities)
        return entities.size
    }

    private suspend fun pullResilience(auth: String, start: String, end: String, now: Long): Int {
        // Resilience ist relativ neu (2024) — nicht jeder Account hat es. Bei 404
        // liefern wir 0 statt zu crashen.
        val all = mutableListOf<OuraDailyResilience>()
        var nextToken: String? = null
        try {
            do {
                val page = api.listDailyResilience(auth, start, end, nextToken)
                all += page.data
                nextToken = page.nextToken
            } while (nextToken != null)
        } catch (e: HttpException) {
            if (e.code() == 404 || e.code() == 422) {
                Log.w(TAG, "Resilience-Endpunkt nicht verfuegbar (HTTP ${e.code()}) — ueberspringe.")
                return 0
            }
            throw e
        }
        val entities = all.map { it.toEntity(now) }
        resilienceDao.upsertAll(entities)
        return entities.size
    }

    private suspend fun pullSleepDetail(auth: String, start: String, end: String, now: Long): Int {
        val all = mutableListOf<OuraSleep>()
        var nextToken: String? = null
        do {
            val page = api.listSleep(auth, start, end, nextToken)
            all += page.data
            nextToken = page.nextToken
        } while (nextToken != null)
        val entities = all.map { it.toEntity(now) }
        sleepDetailDao.upsertAll(entities)
        return entities.size
    }

    companion object {
        private const val TAG = "OuraRepository"
    }
}

/* ----------------------- DTO -> Entity Mapper ----------------------- */

private fun OuraDailyReadiness.toEntity(now: Long): OuraReadinessEntity =
    OuraReadinessEntity(
        day = day,
        capturedAt = now,
        score = score,
        temperatureDeviation = temperatureDeviation,
        temperatureTrendDeviation = temperatureTrendDeviation,
        activityBalance = contributors?.activityBalance,
        bodyTemperature = contributors?.bodyTemperature,
        hrvBalance = contributors?.hrvBalance,
        previousDayActivity = contributors?.previousDayActivity,
        previousNight = contributors?.previousNight,
        recoveryIndex = contributors?.recoveryIndex,
        restingHeartRate = contributors?.restingHeartRate,
        sleepBalance = contributors?.sleepBalance,
        createdAt = now,
    )

private fun OuraDailySleep.toEntity(now: Long): OuraDailySleepEntity =
    OuraDailySleepEntity(
        day = day,
        capturedAt = now,
        score = score,
        deepSleepScore = contributors?.deepSleep,
        efficiencyScore = contributors?.efficiency,
        latencyScore = contributors?.latency,
        remSleepScore = contributors?.remSleep,
        restfulnessScore = contributors?.restfulness,
        timingScore = contributors?.timing,
        totalSleepScore = contributors?.totalSleep,
        createdAt = now,
    )

private fun OuraDailyActivity.toEntity(now: Long): OuraActivityEntity =
    OuraActivityEntity(
        day = day,
        capturedAt = now,
        score = score,
        activeCalories = activeCalories,
        totalCalories = totalCalories,
        targetCalories = targetCalories,
        steps = steps,
        walkingDistanceMeters = equivalentWalkingDistance,
        highActivitySeconds = highActivityTime,
        mediumActivitySeconds = mediumActivityTime,
        lowActivitySeconds = lowActivityTime,
        nonWearSeconds = nonWearTime,
        restingSeconds = restingTime,
        sedentarySeconds = sedentaryTime,
        inactivityAlerts = inactivityAlerts,
        createdAt = now,
    )

private fun OuraDailyResilience.toEntity(now: Long): OuraResilienceEntity =
    OuraResilienceEntity(
        day = day,
        capturedAt = now,
        level = level,
        sleepRecovery = contributors?.sleepRecovery,
        daytimeRecovery = contributors?.daytimeRecovery,
        stress = contributors?.stress,
        createdAt = now,
    )

private fun OuraSleep.toEntity(now: Long): OuraSleepDetailEntity =
    OuraSleepDetailEntity(
        id = id,
        day = day,
        capturedAt = now,
        bedtimeStart = bedtimeStart,
        bedtimeEnd = bedtimeEnd,
        type = type,
        totalSleepSeconds = totalSleepDuration,
        timeInBedSeconds = timeInBed,
        awakeSeconds = awakeTime,
        lightSeconds = lightSleepDuration,
        deepSeconds = deepSleepDuration,
        remSeconds = remSleepDuration,
        efficiency = efficiency,
        latencySeconds = latency,
        restlessPeriods = restlessPeriods,
        averageBreath = averageBreath,
        averageHeartRate = averageHeartRate,
        averageHrv = averageHrv,
        lowestHeartRate = lowestHeartRate,
        sleepPhase5Min = sleepPhase5Min,
        createdAt = now,
    )
