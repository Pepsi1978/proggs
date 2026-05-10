package de.frank.entropyreducer.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health-Connect-Bruecke fuer Gewichts-Daten (Frank-Wunsch 2026-05-10).
 *
 * Die Zepp-App synct seit Januar 2025 ihre Gewichtsdaten ueber Health Connect
 * weiter an andere Apps. Wenn Frank in der Zepp-App den Health-Connect-Sync
 * eingeschaltet hat, koennen wir hier nur-lesend auf den Wert zugreifen — ohne
 * eigenen Cloud-Auth, ohne Reverse-Engineering von Endpoints.
 *
 * Was diese Klasse NICHT macht:
 *  - Schreibt nichts nach Health Connect (nur READ_WEIGHT-Permission noetig)
 *  - Speichert nichts in der App-DB (das macht der Aufrufer wenn gewollt)
 *  - Triggert keinen Sync auf der Zepp-Seite (das macht die Zepp-App selbst)
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
    )

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
        return granted.containsAll(readPermissions)
    }

    /**
     * Liefert den ActivityResultContract zum Anfordern der Permission. Aufrufer
     * (z.B. Activity oder Composable mit rememberLauncherForActivityResult)
     * uebergibt das Set [readPermissions] und bekommt zurueck welche
     * Permissions tatsaechlich erteilt wurden.
     */
    fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()

    fun requiredReadPermissions(): Set<String> = readPermissions

    /**
     * Letzter Gewichts-Wert in kg (oder null wenn noch nichts vorhanden bzw.
     * Permission fehlt). Liest die letzten 365 Tage und nimmt den juengsten
     * Eintrag.
     */
    suspend fun readLatestWeightKg(): Double? {
        val c = client() ?: return null
        if (!hasWeightReadPermission()) return null
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
        return response.records.firstOrNull()?.weight?.inKilograms
    }

    /**
     * Gewichts-Verlauf der letzten N Tage als (timestampMs, kg)-Paare,
     * aufsteigend sortiert. Leer wenn Permission fehlt oder keine Daten.
     */
    suspend fun readWeightHistory(days: Int = 30): List<Pair<Long, Double>> {
        val c = client() ?: return emptyList()
        if (!hasWeightReadPermission()) return emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = true,
            ),
        )
        return response.records.map { it.time.toEpochMilli() to it.weight.inKilograms }
    }

    /** Durchschnitt der letzten N Tage (oder null wenn keine Daten). */
    suspend fun averageWeightKg(days: Int = 30): Double? {
        val history = readWeightHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }
}
