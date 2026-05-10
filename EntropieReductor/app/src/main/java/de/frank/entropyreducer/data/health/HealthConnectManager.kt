package de.frank.entropyreducer.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health-Connect-Bruecke fuer Body-Daten aus Zepp (Frank-Wunsch 2026-05-10).
 *
 * Die Zepp-App synct seit Januar 2025 Gewicht/Koerperfett/Magermasse ueber
 * Health Connect weiter an andere Apps. Wenn Frank in der Zepp-App den
 * Health-Connect-Sync eingeschaltet hat, koennen wir hier nur-lesend auf die
 * Werte zugreifen — ohne eigenen Cloud-Auth, ohne Reverse-Engineering von
 * Endpoints.
 *
 * Was diese Klasse NICHT macht:
 *  - Schreibt nichts nach Health Connect (nur READ-Permissions noetig)
 *  - Speichert nichts in der App-DB (das macht der Aufrufer wenn gewollt)
 *  - Triggert keinen Sync auf der Zepp-Seite (das macht die Zepp-App selbst)
 *
 * Frank-Iteration 2026-05-10: BodyFat + LeanBodyMass dazu, weil die Smart-Scale
 * der T-Rex 3 / der Zepp-Gewichtsmessgeraete diese Werte ebenfalls liefert.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val weightPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
    )
    private val bodyFatPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(BodyFatRecord::class),
    )
    private val leanBodyMassPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
    )

    /**
     * Frank-Befund 2026-05-10: Ohne PERMISSION_READ_HEALTH_DATA_HISTORY ist die
     * Lese-Reichweite auf die letzten 30 Tage begrenzt — was bei seltenen
     * Wiegungen (Frank's letzte Messung vom 14. Januar) bedeutet, dass alle
     * Read-Methoden 0 records zurueckgeben. Diese Permission hebt das 30-Tage-
     * Limit auf.
     */
    private val historyPermission: Set<String> = setOf(
        // Frank-Befund 2026-05-10: PERMISSION_READ_HEALTH_DATA_HISTORY-Konstante
        // existiert in der aktuellen Health-Connect-Client-Version (1.1.0-alpha07)
        // noch nicht. Wir nutzen direkt den Permission-String aus der Android-
        // Plattform — der ist seit Android 14 stabil.
        "android.permission.health.READ_HEALTH_DATA_HISTORY",
    )

    /**
     * Komplette Set aller noetigen READ-Permissions — wird beim einmaligen
     * Permission-Request an den ActivityResultContract uebergeben, damit Frank
     * mit einem Klick alle Berechtigungen erteilen kann (drei Datentypen +
     * History-Zugriff).
     */
    private val allReadPermissions: Set<String> =
        weightPermissions + bodyFatPermissions + leanBodyMassPermissions + historyPermission

    /** Health Connect ist auf dem Geraet verfuegbar (App installiert oder Modul aktiv). */
    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    private fun client(): HealthConnectClient? {
        return if (isAvailable()) HealthConnectClient.getOrCreate(context) else null
    }

    /** Pruefen ob die READ_WEIGHT-Permission schon erteilt ist. */
    suspend fun hasWeightReadPermission(): Boolean {
        val c = client() ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(weightPermissions)
    }

    /** Pruefen ob die READ_BODY_FAT-Permission schon erteilt ist. */
    suspend fun hasBodyFatReadPermission(): Boolean {
        val c = client() ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(bodyFatPermissions)
    }

    /** Pruefen ob die READ_LEAN_BODY_MASS-Permission schon erteilt ist. */
    suspend fun hasLeanBodyMassReadPermission(): Boolean {
        val c = client() ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(leanBodyMassPermissions)
    }

    /** Pruefen ob READ_HEALTH_DATA_HISTORY erteilt ist (Frank-Befund 2026-05-10). */
    suspend fun hasHistoryReadPermission(): Boolean {
        val c = client() ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(historyPermission)
    }

    /**
     * Pruefen ob ALLE noetigen Permissions erteilt sind: Weight + BodyFat +
     * LeanBodyMass + History. Wenn auch nur eine fehlt, liefert die Lese-
     * Schicht entweder 0 records (Datentyp-Permission fehlt) oder ein limitiertes
     * 30-Tage-Fenster (History-Permission fehlt).
     */
    suspend fun hasAllReadPermissions(): Boolean {
        val c = client() ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(allReadPermissions)
    }

    /**
     * Liefert den ActivityResultContract zum Anfordern der Permissions. Aufrufer
     * (z.B. Activity oder Composable mit rememberLauncherForActivityResult)
     * uebergibt das Set [allReadPermissions] und bekommt zurueck welche
     * Permissions tatsaechlich erteilt wurden.
     */
    fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()

    /**
     * ALLE Permissions die wir lesen koennen (Weight + BodyFat + LeanBodyMass).
     * Wird beim einmaligen Permission-Request uebergeben damit Frank mit einem
     * Klick alle drei Berechtigungen erteilen kann.
     */
    fun requiredReadPermissions(): Set<String> = allReadPermissions

    // ---------- Gewicht ----------

    /**
     * Letzter Gewichts-Wert in kg. null wenn:
     *  - Health Connect nicht verfuegbar
     *  - READ_WEIGHT-Permission nicht erteilt
     *  - Keine Records in den letzten 365 Tagen
     *  - Lese-Fehler (z.B. wenn Health Connect kurz nicht erreichbar war)
     *
     * Bei Lese-Fehlern wird gelogged statt zu werfen — die UI kriegt einfach
     * null zurueck und zeigt den Strich.
     */
    suspend fun readLatestWeightKg(): Double? = runCatching {
        val c = client() ?: return@runCatching null
        if (!hasWeightReadPermission()) return@runCatching null
        val end = Instant.now()
        val start = end.minusSeconds(365L * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        Log.d(TAG, "Weight read: ${response.records.size} records, latest=${response.records.firstOrNull()?.weight?.inKilograms}")
        response.records.firstOrNull()?.weight?.inKilograms
    }.onFailure { Log.w(TAG, "readLatestWeightKg failed", it) }.getOrNull()

    /**
     * Gewichts-Verlauf der letzten N Tage als (timestampMs, kg)-Paare,
     * aufsteigend sortiert. Leer wenn Permission fehlt oder keine Daten.
     */
    suspend fun readWeightHistory(days: Int = 30): List<Pair<Long, Double>> = runCatching {
        val c = client() ?: return@runCatching emptyList()
        if (!hasWeightReadPermission()) return@runCatching emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ),
        )
        response.records.map { it.time.toEpochMilli() to it.weight.inKilograms }
    }.onFailure { Log.w(TAG, "readWeightHistory failed", it) }.getOrDefault(emptyList())

    /** Durchschnitt der letzten N Tage (oder null wenn keine Daten). */
    suspend fun averageWeightKg(days: Int = 30): Double? {
        val history = readWeightHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }

    // ---------- Koerperfett ----------

    /**
     * Letzter Koerperfett-Wert in Prozent (0-100). null bei fehlender Permission
     * oder fehlenden Daten. Smart-Scales in Verbindung mit Zepp liefern das
     * normalerweise mit jeder Wiegung.
     */
    suspend fun readLatestBodyFatPercent(): Double? = runCatching {
        val c = client() ?: return@runCatching null
        if (!hasBodyFatReadPermission()) return@runCatching null
        val end = Instant.now()
        val start = end.minusSeconds(365L * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = BodyFatRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        Log.d(TAG, "BodyFat read: ${response.records.size} records, latest=${response.records.firstOrNull()?.percentage?.value}")
        response.records.firstOrNull()?.percentage?.value
    }.onFailure { Log.w(TAG, "readLatestBodyFatPercent failed", it) }.getOrNull()

    /** Koerperfett-Verlauf der letzten N Tage. */
    suspend fun readBodyFatHistory(days: Int = 30): List<Pair<Long, Double>> = runCatching {
        val c = client() ?: return@runCatching emptyList()
        if (!hasBodyFatReadPermission()) return@runCatching emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = BodyFatRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ),
        )
        response.records.map { it.time.toEpochMilli() to it.percentage.value }
    }.onFailure { Log.w(TAG, "readBodyFatHistory failed", it) }.getOrDefault(emptyList())

    suspend fun averageBodyFatPercent(days: Int = 30): Double? {
        val history = readBodyFatHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }

    // ---------- Magermasse ----------

    /**
     * Letzte Magermasse (Lean Body Mass) in kg. Bei vielen Smart-Scales sind das
     * die Muskelmasse + Wassergehalt + Knochen — also alles ausser Fett.
     */
    suspend fun readLatestLeanBodyMassKg(): Double? = runCatching {
        val c = client() ?: return@runCatching null
        if (!hasLeanBodyMassReadPermission()) return@runCatching null
        val end = Instant.now()
        val start = end.minusSeconds(365L * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = LeanBodyMassRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        Log.d(TAG, "LeanBodyMass read: ${response.records.size} records, latest=${response.records.firstOrNull()?.mass?.inKilograms}")
        response.records.firstOrNull()?.mass?.inKilograms
    }.onFailure { Log.w(TAG, "readLatestLeanBodyMassKg failed", it) }.getOrNull()

    suspend fun readLeanBodyMassHistory(days: Int = 30): List<Pair<Long, Double>> = runCatching {
        val c = client() ?: return@runCatching emptyList()
        if (!hasLeanBodyMassReadPermission()) return@runCatching emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = LeanBodyMassRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ),
        )
        response.records.map { it.time.toEpochMilli() to it.mass.inKilograms }
    }.onFailure { Log.w(TAG, "readLeanBodyMassHistory failed", it) }.getOrDefault(emptyList())

    suspend fun averageLeanBodyMassKg(days: Int = 30): Double? {
        val history = readLeanBodyMassHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }

    private companion object {
        const val TAG = "HealthConnectMgr"
    }
}
