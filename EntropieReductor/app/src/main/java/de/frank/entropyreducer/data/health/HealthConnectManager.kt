package de.frank.entropyreducer.data.health

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import java.util.Locale
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
import de.frank.entropyreducer.util.runCatchingCancellable
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

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
    private val diagnostics: DiagnosticLogger,
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
    private val bodyWaterMassPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(BodyWaterMassRecord::class),
    )
    private val boneMassPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(BoneMassRecord::class),
    )
    // Frank-Wunsch 2026-05-10 (zweite Iteration): zusaetzliche Permissions damit
    // zukuenftige Erweiterungen ohne erneuten Permission-Dialog auskommen.
    private val heightPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(HeightRecord::class),
    )
    private val basalMetabolicRatePermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
    )

    /**
     * Frank-Wunsch 2026-05-10 (dritte Iteration): ALLE Health-Connect-READ-
     * Permissions prophylaktisch anfragen, damit zukuenftige Plugins/Karten in
     * der App ohne erneuten Permission-Dialog auskommen. Health Connect normalisiert
     * unbekannte Permissions weg — wenn ein Datentyp in einer alten HC-Version
     * nicht existiert, wird der Eintrag stillschweigend ignoriert.
     *
     * Plattform-Strings statt Library-Konstanten, weil manche (z.B. SkinTemperature,
     * IN_BACKGROUND) erst in spaeteren androidx.health.connect-Releases als
     * Konstanten verfuegbar sind. Die Strings sind aber seit Android 14 stabil.
     *
     * Liste deckt alles aus android.health.connect.HealthPermissions ab — Activity,
     * Body Composition, Vitals, Heart, Sleep, Nutrition, Cycle Tracking. Frank
     * will damit ein generisches "alle Health-Connect-Daten lesen"-Recht.
     */
    private val additionalReadPermissions: Set<String> = setOf(
        // Activity
        "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
        "android.permission.health.READ_DISTANCE",
        "android.permission.health.READ_ELEVATION_GAINED",
        "android.permission.health.READ_EXERCISE",
        "android.permission.health.READ_EXERCISE_ROUTES",
        "android.permission.health.READ_FLOORS_CLIMBED",
        "android.permission.health.READ_POWER",
        "android.permission.health.READ_SPEED",
        "android.permission.health.READ_STEPS",
        "android.permission.health.READ_TOTAL_CALORIES_BURNED",
        "android.permission.health.READ_VO2_MAX",
        "android.permission.health.READ_WHEELCHAIR_PUSHES",
        // Heart & Vitals
        "android.permission.health.READ_BLOOD_GLUCOSE",
        "android.permission.health.READ_BLOOD_PRESSURE",
        "android.permission.health.READ_BODY_TEMPERATURE",
        "android.permission.health.READ_BASAL_BODY_TEMPERATURE",
        "android.permission.health.READ_HEART_RATE",
        "android.permission.health.READ_HEART_RATE_VARIABILITY",
        "android.permission.health.READ_OXYGEN_SATURATION",
        "android.permission.health.READ_RESPIRATORY_RATE",
        "android.permission.health.READ_RESTING_HEART_RATE",
        "android.permission.health.READ_SKIN_TEMPERATURE",
        // Sleep
        "android.permission.health.READ_SLEEP",
        // Nutrition
        "android.permission.health.READ_HYDRATION",
        "android.permission.health.READ_NUTRITION",
        // Cycle Tracking
        "android.permission.health.READ_CERVICAL_MUCUS",
        "android.permission.health.READ_INTERMENSTRUAL_BLEEDING",
        "android.permission.health.READ_MENSTRUATION",
        "android.permission.health.READ_OVULATION_TEST",
        "android.permission.health.READ_SEXUAL_ACTIVITY",
        // Background Read — erlaubt Hintergrund-Lesen ohne offene App
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND",
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
        weightPermissions + bodyFatPermissions + leanBodyMassPermissions +
            bodyWaterMassPermissions + boneMassPermissions +
            heightPermissions + basalMetabolicRatePermissions +
            additionalReadPermissions + historyPermission

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

    suspend fun hasBodyWaterMassReadPermission(): Boolean {
        val c = client() ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(bodyWaterMassPermissions)
    }

    suspend fun hasBoneMassReadPermission(): Boolean {
        val c = client() ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return granted.containsAll(boneMassPermissions)
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

    /**
     * Frank-Wunsch 2026-05-10: Permissions im Nachhinein bearbeiten — einzelne
     * zurueckziehen. Der Plattform-Intent
     * "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS" oeffnet die
     * Health-Connect-spezifische Permission-UI fuer unsere App, in der jeder
     * Datentyp einzeln togglebar ist.
     *
     * Fallback bei nicht-aufgeloestem Intent (alte HC-Versionen): die Health-
     * Connect-Hauptseite (HEALTH_HOME_SETTINGS). Von dort kann Frank sich manuell
     * in 'Apps und Daten' navigieren und unsere App finden.
     *
     * Drittes Fallback: System-App-Settings als universellster Fallback.
     */
    fun openAppPermissionsInHealthConnect() {
        val pkg = context.packageName
        // Frank-Befund 2026-05-10 (zweite Iteration): MANAGE_HEALTH_PERMISSIONS
        // mit EXTRA_PACKAGE_NAME funktionierte nicht zuverlaessig — Frank landete
        // in einem 'X von Y erteilt'-Status-Screen ohne Edit-Moeglichkeit. Daher
        // jetzt HEALTH_HOME_SETTINGS zuerst (robust auf Android 14+ und Samsung
        // One UI), Frank navigiert von dort selbst zu 'App-Berechtigungen' →
        // Entropie Reduktor und kann jeden Toggle einzeln editieren.
        val candidates = listOf(
            Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                .putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", pkg, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        for (intent in candidates) {
            if (runCatching { context.startActivity(intent) }.isSuccess) {
                Diag.i(DiagnosticArea.HEALTH_CONNECT, TAG, "Permissions-Editor geoeffnet via ${intent.action}")
                return
            }
        }
        Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "Kein Permissions-Editor-Intent funktionierte — alle Fallbacks haben fehlgeschlagen")
    }

    /**
     * Liefert ALLE aktuell erteilten Permissions. Wird vom API-Settings-Screen
     * genutzt um "X von Y erteilt" anzuzeigen (Frank-Wunsch 2026-05-10).
     */
    suspend fun allGrantedPermissions(): Set<String> {
        val c = client() ?: return emptySet()
        return c.permissionController.getGrantedPermissions()
    }

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
    suspend fun readLatestWeightKg(): Double? = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable null
        if (!hasWeightReadPermission()) return@runCatchingCancellable null
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
        Diag.d(DiagnosticArea.HEALTH_CONNECT, TAG, "Weight read: ${response.records.size} records, latest=${response.records.firstOrNull()?.weight?.inKilograms}")
        response.records.firstOrNull()?.weight?.inKilograms
    }.onFailure {
        Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readLatestWeightKg failed", it)
        diagnostics.error(
            DiagnosticArea.HEALTH_CONNECT,
            "Gewicht-Lesen fehlgeschlagen: ${it.message ?: it::class.java.simpleName}",
            it,
        )
    }.getOrNull()

    /**
     * Gewichts-Verlauf der letzten N Tage als (timestampMs, kg)-Paare,
     * aufsteigend sortiert. Leer wenn Permission fehlt oder keine Daten.
     */
    suspend fun readWeightHistory(days: Int = 730): List<Pair<Long, Double>> = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable emptyList()
        if (!hasWeightReadPermission()) return@runCatchingCancellable emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        readAllRecords(c, WeightRecord::class, TimeRangeFilter.between(start, end))
            .map { it.time.toEpochMilli() to it.weight.inKilograms }
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readWeightHistory failed", it) }.getOrDefault(emptyList())

    /** Durchschnitt der letzten N Tage (oder null wenn keine Daten). */
    suspend fun averageWeightKg(days: Int = 730): Double? {
        val history = readWeightHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }

    // ---------- Koerperfett ----------

    /**
     * Letzter Koerperfett-Wert in Prozent (0-100). null bei fehlender Permission
     * oder fehlenden Daten. Smart-Scales in Verbindung mit Zepp liefern das
     * normalerweise mit jeder Wiegung.
     */
    suspend fun readLatestBodyFatPercent(): Double? = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable null
        if (!hasBodyFatReadPermission()) return@runCatchingCancellable null
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
        Diag.d(DiagnosticArea.HEALTH_CONNECT, TAG, "BodyFat read: ${response.records.size} records, latest=${response.records.firstOrNull()?.percentage?.value}")
        response.records.firstOrNull()?.percentage?.value
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readLatestBodyFatPercent failed", it) }.getOrNull()

    /** Koerperfett-Verlauf der letzten N Tage. */
    suspend fun readBodyFatHistory(days: Int = 730): List<Pair<Long, Double>> = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable emptyList()
        if (!hasBodyFatReadPermission()) return@runCatchingCancellable emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        readAllRecords(c, BodyFatRecord::class, TimeRangeFilter.between(start, end))
            .map { it.time.toEpochMilli() to it.percentage.value }
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readBodyFatHistory failed", it) }.getOrDefault(emptyList())

    suspend fun averageBodyFatPercent(days: Int = 730): Double? {
        val history = readBodyFatHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }

    // ---------- Magermasse ----------

    /**
     * Letzte Magermasse (Lean Body Mass) in kg. Bei vielen Smart-Scales sind das
     * die Muskelmasse + Wassergehalt + Knochen — also alles ausser Fett.
     */
    suspend fun readLatestLeanBodyMassKg(): Double? = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable null
        if (!hasLeanBodyMassReadPermission()) return@runCatchingCancellable null
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
        Diag.d(DiagnosticArea.HEALTH_CONNECT, TAG, "LeanBodyMass read: ${response.records.size} records, latest=${response.records.firstOrNull()?.mass?.inKilograms}")
        response.records.firstOrNull()?.mass?.inKilograms
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readLatestLeanBodyMassKg failed", it) }.getOrNull()

    suspend fun readLeanBodyMassHistory(days: Int = 730): List<Pair<Long, Double>> = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable emptyList()
        if (!hasLeanBodyMassReadPermission()) return@runCatchingCancellable emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        readAllRecords(c, LeanBodyMassRecord::class, TimeRangeFilter.between(start, end))
            .map { it.time.toEpochMilli() to it.mass.inKilograms }
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readLeanBodyMassHistory failed", it) }.getOrDefault(emptyList())

    suspend fun averageLeanBodyMassKg(days: Int = 730): Double? {
        val history = readLeanBodyMassHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }

    // ---------- Koerperwasser ----------

    /**
     * Letzte Koerperwasser-Masse in kg. Bei Smart-Scales typischerweise zusammen
     * mit Gewicht und Koerperfett geschrieben — Frank-Wunsch 2026-05-10.
     */
    suspend fun readLatestBodyWaterMassKg(): Double? = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable null
        if (!hasBodyWaterMassReadPermission()) return@runCatchingCancellable null
        val end = Instant.now()
        val start = end.minusSeconds(365L * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = BodyWaterMassRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        Diag.d(DiagnosticArea.HEALTH_CONNECT, TAG, "BodyWaterMass read: ${response.records.size} records, latest=${response.records.firstOrNull()?.mass?.inKilograms}")
        response.records.firstOrNull()?.mass?.inKilograms
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readLatestBodyWaterMassKg failed", it) }.getOrNull()

    suspend fun readBodyWaterMassHistory(days: Int = 730): List<Pair<Long, Double>> = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable emptyList()
        if (!hasBodyWaterMassReadPermission()) return@runCatchingCancellable emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        readAllRecords(c, BodyWaterMassRecord::class, TimeRangeFilter.between(start, end))
            .map { it.time.toEpochMilli() to it.mass.inKilograms }
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readBodyWaterMassHistory failed", it) }.getOrDefault(emptyList())

    suspend fun averageBodyWaterMassKg(days: Int = 730): Double? {
        val history = readBodyWaterMassHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }

    // ---------- Knochenmasse ----------

    /** Letzte Knochenmasse in kg. Bei vielen Smart-Scales als Body-Composition-Wert dabei. */
    suspend fun readLatestBoneMassKg(): Double? = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable null
        if (!hasBoneMassReadPermission()) return@runCatchingCancellable null
        val end = Instant.now()
        val start = end.minusSeconds(365L * 24 * 60 * 60)
        val response = c.readRecords(
            ReadRecordsRequest(
                recordType = BoneMassRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        Diag.d(DiagnosticArea.HEALTH_CONNECT, TAG, "BoneMass read: ${response.records.size} records, latest=${response.records.firstOrNull()?.mass?.inKilograms}")
        response.records.firstOrNull()?.mass?.inKilograms
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readLatestBoneMassKg failed", it) }.getOrNull()

    suspend fun readBoneMassHistory(days: Int = 730): List<Pair<Long, Double>> = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable emptyList()
        if (!hasBoneMassReadPermission()) return@runCatchingCancellable emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24 * 60 * 60)
        readAllRecords(c, BoneMassRecord::class, TimeRangeFilter.between(start, end))
            .map { it.time.toEpochMilli() to it.mass.inKilograms }
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readBoneMassHistory failed", it) }.getOrDefault(emptyList())

    suspend fun averageBoneMassKg(days: Int = 730): Double? {
        val history = readBoneMassHistory(days)
        return history.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    }

    // ---------- Trainings (ExerciseSessionRecord) ----------

    /**
     * Frank-Wunsch 2026-05-16: Workouts aus Health Connect lesen, damit Trainings
     * sichtbar werden auch wenn die T-Rex 3 noch nicht zur Zepp-Cloud hochgeladen
     * hat. Die Zepp-App schreibt Sessions per Bluetooth-Sync sofort in Health
     * Connect — der Cloud-Upload ist eine separate Stufe und kann verzoegert sein.
     *
     * Liefert die Sessions der letzten [days] Tage aufsteigend sortiert.
     * Distanz, Kalorien und Durchschnittspuls werden pro Session via Aggregate-API
     * geholt, damit die Hero-Card vollstaendige Werte zeigen kann.
     */
    suspend fun readExerciseSessions(days: Int = 30): List<HealthConnectExerciseSession> = runCatchingCancellable {
        val c = client() ?: return@runCatchingCancellable emptyList()
        val end = Instant.now()
        val start = end.minusSeconds(days.toLong() * 24L * 60L * 60L)
        val sessions = readAllRecords(
            c,
            ExerciseSessionRecord::class,
            TimeRangeFilter.between(start, end),
            ascendingOrder = false,
        )
        Diag.d(DiagnosticArea.HEALTH_CONNECT, TAG, "ExerciseSessions read: ${sessions.size} im Fenster ${start} .. ${end}")
        val mapped = sessions.map { session ->
            val sessionStart = session.startTime
            val sessionEnd = session.endTime
            val durationSeconds = (sessionEnd.epochSecond - sessionStart.epochSecond).coerceAtLeast(0L)
            val range = TimeRangeFilter.between(sessionStart, sessionEnd)

            // Distanz + Kalorien + avg/max-Puls via Aggregate (zuverlaessig).
            val aggregate = runCatchingCancellable {
                c.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            DistanceRecord.DISTANCE_TOTAL,
                            TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                            HeartRateRecord.BPM_AVG,
                            HeartRateRecord.BPM_MAX,
                        ),
                        timeRangeFilter = range,
                    ),
                )
            }.getOrNull()
            val distanceMeters = aggregate?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters
            val calorieKcal = aggregate?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories

            // Frank-Bugfix 2026-07-03: avg/max + Verlaeufe werden aus den ROH-SAMPLES berechnet,
            // NICHT aus einem kombinierten Speed/Elevation/Cadence-Aggregate. Letzteres lieferte fuer
            // die realen Zepp/Polar-Sessions null (eine fehlende Sub-Metrik reisst die ganze Gruppe
            // mit), wodurch maxPace/Cadence/Hoehe/avgSpeed fehlten. Die readRecords-Samples kommen
            // dagegen zuverlaessig (3600+ Punkte pro Stunde).

            // Puls: Samples -> Verlauf + avg/max (Aggregate bevorzugt, sonst aus Samples).
            val hrSamples = runCatchingCancellable {
                readAllRecords(c, HeartRateRecord::class, range).flatMap { it.samples }
            }.getOrDefault(emptyList())
            val hrBpm = hrSamples.map { it.beatsPerMinute }.filter { it in 30L..230L }
            val avgHeartRate = aggregate?.get(HeartRateRecord.BPM_AVG)?.toInt()
                ?: hrBpm.takeIf { it.isNotEmpty() }?.average()?.toInt()
            val maxHeartRate = aggregate?.get(HeartRateRecord.BPM_MAX)?.toInt()
                ?: hrBpm.maxOrNull()?.toInt()
            val heartRateSeriesJson = hrSamples
                .mapNotNull { smp ->
                    val bpm = smp.beatsPerMinute
                    if (bpm in 30L..230L) {
                        "[${(smp.time.epochSecond - sessionStart.epochSecond).coerceAtLeast(0L)},$bpm]"
                    } else {
                        null
                    }
                }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "[", postfix = "]", separator = ",")

            // Tempo/Speed: Samples -> Verlauf + avg/max Pace.
            val speedSamples = runCatchingCancellable {
                readAllRecords(c, SpeedRecord::class, range).flatMap { it.samples }
            }.getOrDefault(emptyList())
            val speedMps = speedSamples.map { it.speed.inMetersPerSecond }.filter { it > 0.1 }
            val avgSpeedMps = speedMps.takeIf { it.isNotEmpty() }?.average()
            val maxSpeedMps = speedMps.maxOrNull()
            val paceStreamJson = speedSamples
                .mapNotNull { smp ->
                    val mps = smp.speed.inMetersPerSecond
                    if (mps > 0.1) {
                        val secPerKm = 1000.0 / mps
                        if (secPerKm in 150.0..1500.0) {
                            "[${(smp.time.epochSecond - sessionStart.epochSecond).coerceAtLeast(0L)}," +
                                "${"%.1f".format(Locale.US, secPerKm)}]"
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "[", postfix = "]", separator = ",")

            // Schrittfrequenz: StepsCadenceRecord-Samples (Schritte/Min).
            val cadenceRates = runCatchingCancellable {
                readAllRecords(c, StepsCadenceRecord::class, range)
                    .flatMap { it.samples }.map { it.rate }.filter { it > 0.0 }
            }.getOrDefault(emptyList())
            val cadenceAvg = cadenceRates.takeIf { it.isNotEmpty() }?.average()

            // Hoehenmeter hoch: Summe aller ElevationGainedRecord im Fenster.
            val elevationGainMeters = runCatchingCancellable {
                readAllRecords(c, ElevationGainedRecord::class, range)
                    .sumOf { it.elevation.inMeters }
            }.getOrDefault(0.0).takeIf { it > 0.0 }

            // GPS-Route + Hoehenverlust aus der Route. Route ist in HC besonders geschuetzt — Status
            // wird geloggt (Data/ConsentRequired/NoData), damit GPS-Luecken diagnostizierbar sind.
            val routeResult = session.exerciseRouteResult
            val routeLocations = (routeResult as? ExerciseRouteResult.Data)?.exerciseRoute?.route.orEmpty()
            val routeStatus = when (routeResult) {
                is ExerciseRouteResult.Data -> "data(${routeLocations.size})"
                is ExerciseRouteResult.ConsentRequired -> "consentRequired"
                is ExerciseRouteResult.NoData -> "noData"
                else -> "unknown"
            }
            val gpsTrackJson = routeLocations
                .filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "[", postfix = "]", separator = ",") { loc ->
                    "[${"%.6f".format(Locale.US, loc.latitude)},${"%.6f".format(Locale.US, loc.longitude)}]"
                }
            var altGainFromRoute = 0.0
            var altLoss = 0.0
            var prevAlt: Double? = null
            for (loc in routeLocations) {
                val alt = loc.altitude?.inMeters ?: continue
                val p = prevAlt
                if (p != null) {
                    if (alt > p) altGainFromRoute += (alt - p) else if (alt < p) altLoss += (p - alt)
                }
                prevAlt = alt
            }
            val altitudeGainMeters = elevationGainMeters ?: altGainFromRoute.takeIf { it > 0.0 }
            val altitudeLossMeters = altLoss.takeIf { it > 0.0 }

            // Pace abgeleitet aus dem (jetzt sample-basierten) avgSpeed, sonst aus Distanz/Dauer.
            val avgPaceSecPerKm = avgSpeedMps?.takeIf { it > 0.1 }?.let { 1000.0 / it }
                ?: run {
                    val d = distanceMeters
                    if (d != null && d > 0.0 && durationSeconds > 0L) {
                        durationSeconds.toDouble() / (d / 1000.0)
                    } else {
                        null
                    }
                }
            val maxPaceSecPerKm = maxSpeedMps?.takeIf { it > 0.1 }?.let { 1000.0 / it }
            // Frank-Bugfix 2026-07-04: Health Connect liefert StepsCadenceRecord als Schritte/Min
            // BEIDE Beine (~158 beim Laufen); Polar/Strava zeigen die Kadenz pro Bein (~79). Fuer
            // Konsistenz mit Franks Polar-App halbieren. Die Schrittlaenge unten nutzt bewusst den
            // VOLLEN cadenceAvg (beide Beine) — sonst waere sie doppelt so lang.
            val cadence = cadenceAvg?.takeIf { it > 0.0 }?.let { (it / 2.0).toInt() }
            val strideLengthCm = run {
                val d = distanceMeters
                val cad = cadenceAvg
                if (d != null && cad != null && cad > 0.0 && durationSeconds > 0L) {
                    val totalSteps = cad * (durationSeconds / 60.0)
                    val strideM = if (totalSteps > 0.0) d / totalSteps else 0.0
                    if (strideM in 0.4..2.5) (strideM * 100.0).toInt() else null
                } else {
                    null
                }
            }

            // Live-Logik-Sonde: welche Detailfelder Health Connect je Training wirklich lieferte
            // (inkl. Route-Status, um GPS-Luecken zu diagnostizieren).
            Diag.d(
                DiagnosticArea.HEALTH_CONNECT,
                TAG,
                "CHECKPOINT hc_training src=${session.metadata.dataOrigin.packageName} " +
                    "title=${session.title ?: "-"} start=$sessionStart dur=${durationSeconds}s " +
                    "dist=${distanceMeters?.toInt()}m route=$routeStatus hrS=${hrSamples.size} " +
                    "spdS=${speedSamples.size} cadS=${cadenceRates.size} " +
                    "avgPace=${avgPaceSecPerKm?.let { "%.0f".format(Locale.US, it) }} " +
                    "maxPace=${maxPaceSecPerKm?.let { "%.0f".format(Locale.US, it) }} cad=$cadence " +
                    "gain=${altitudeGainMeters?.toInt()} loss=${altitudeLossMeters?.toInt()} " +
                    "cal=${calorieKcal?.toInt()} avgHr=$avgHeartRate maxHr=$maxHeartRate",
            )

            HealthConnectExerciseSession(
                startMs = sessionStart.toEpochMilli(),
                endMs = sessionEnd.toEpochMilli(),
                durationSeconds = durationSeconds,
                exerciseType = session.exerciseType,
                title = session.title?.takeIf { it.isNotBlank() },
                distanceMeters = distanceMeters,
                calories = calorieKcal,
                avgHeartRate = avgHeartRate,
                maxHeartRate = maxHeartRate,
                avgPaceSecPerKm = avgPaceSecPerKm,
                maxPaceSecPerKm = maxPaceSecPerKm,
                avgSpeedKmh = avgSpeedMps?.times(3.6),
                maxSpeedKmh = maxSpeedMps?.times(3.6),
                cadence = cadence,
                strideLengthCm = strideLengthCm,
                altitudeGainMeters = altitudeGainMeters,
                altitudeLossMeters = altitudeLossMeters,
                gpsTrackJson = gpsTrackJson,
                heartRateSeriesJson = heartRateSeriesJson,
                paceStreamJson = paceStreamJson,
                sourceApp = session.metadata.dataOrigin.packageName.takeIf { it.isNotBlank() },
            )
        }
        // Frank-Bugfix 2026-07-03: Health Connect enthaelt Trainings oft DOPPELT (z.B. Polar Flow +
        // Zepp schreiben beide) — eine Version mit vollem Puls/Tempo, eine fast leer. Pro ~5-Min-
        // Zeitfenster die DATENREICHSTE Version behalten, sonst zeigt die App zufaellig die leere.
        // Frank-Bugfix 2026-08-05: Die reichste Version zu behalten reichte nicht — die Quellen
        // haben UNTERSCHIEDLICHE Luecken. Franks Lauf vom 04.08. lag dreifach in HC: eine Version
        // mit 1351 Puls-Punkten und Distanz, eine mit 601 Puls-Punkten, und nur EINE mit
        // GPS-Route. Wer die reichste behielt, warf die GPS-Route oder die Distanz weg. Jetzt
        // bleibt die reichste die Basis und fehlende Felder werden aus den anderen Versionen
        // desselben Fensters aufgefuellt — nur wo die Basis nichts hat, nie ueberschreibend.
        mergeBestPerWindow(mapped)
    }.onFailure { Diag.w(DiagnosticArea.HEALTH_CONNECT, TAG, "readExerciseSessions failed", it) }.getOrDefault(emptyList())

    /**
     * Frank-Bugfix 2026-07-03: Dedupliziert Health-Connect-Sessions, die dasselbe Training abbilden
     * (Start < 5 Min auseinander). Behaelt pro Zeitfenster die datenreichste Version (meiste
     * Puls-/Tempo-/GPS-Daten) — Health Connect enthaelt Trainings oft doppelt (Polar + Zepp).
     */
    private fun mergeBestPerWindow(
        sessions: List<HealthConnectExerciseSession>,
    ): List<HealthConnectExerciseSession> {
        val toleranceMs = 5L * 60L * 1000L
        val kept = mutableListOf<HealthConnectExerciseSession>()
        // Frank-Entscheidung 2026-08-05: Polar ist die fuehrende Quelle. Schreibt Polar Flow ein
        // Training nach Health Connect, ist DIESE Version die Grundlage — auch wenn eine andere
        // App (Oura, Whoop, Zepp) mehr Messpunkte liefert. Die anderen Quellen fuellen nur noch
        // Luecken, die Polar offen laesst; erst danach entscheidet der Datenreichtum.
        val ordered =
            sessions.sortedWith(
                compareByDescending<HealthConnectExerciseSession> { it.isPreferredSource() }
                    .thenByDescending { it.richness() }
            )
        for (s in ordered) {
            val idx = kept.indexOfFirst { kotlin.math.abs(it.startMs - s.startMs) <= toleranceMs }
            if (idx < 0) kept.add(s) else kept[idx] = kept[idx].fillGapsFrom(s)
        }
        return kept.sortedByDescending { it.startMs }
    }

    /** True fuer Sessions aus der fuehrenden Quelle (Polar Flow). */
    private fun HealthConnectExerciseSession.isPreferredSource(): Boolean =
        sourceApp?.startsWith(PREFERRED_WORKOUT_SOURCE) == true

    /**
     * Ergaenzt fehlende Felder aus einer zweiten Version desselben Trainings. Vorhandene Werte
     * bleiben IMMER unangetastet — [other] ist per Konstruktion die datenaermere Version, sie
     * darf nur Luecken fuellen. Startzeit, Dauer und Herkunft bleiben die der Basis, damit
     * trackId und Diagnose stabil bleiben.
     */
    private fun HealthConnectExerciseSession.fillGapsFrom(
        other: HealthConnectExerciseSession,
    ): HealthConnectExerciseSession =
        copy(
            title = title ?: other.title,
            distanceMeters = distanceMeters ?: other.distanceMeters,
            calories = calories ?: other.calories,
            avgHeartRate = avgHeartRate ?: other.avgHeartRate,
            maxHeartRate = maxHeartRate ?: other.maxHeartRate,
            avgPaceSecPerKm = avgPaceSecPerKm ?: other.avgPaceSecPerKm,
            maxPaceSecPerKm = maxPaceSecPerKm ?: other.maxPaceSecPerKm,
            avgSpeedKmh = avgSpeedKmh ?: other.avgSpeedKmh,
            maxSpeedKmh = maxSpeedKmh ?: other.maxSpeedKmh,
            cadence = cadence ?: other.cadence,
            strideLengthCm = strideLengthCm ?: other.strideLengthCm,
            altitudeGainMeters = altitudeGainMeters ?: other.altitudeGainMeters,
            altitudeLossMeters = altitudeLossMeters ?: other.altitudeLossMeters,
            gpsTrackJson = gpsTrackJson ?: other.gpsTrackJson,
            heartRateSeriesJson = heartRateSeriesJson ?: other.heartRateSeriesJson,
            paceStreamJson = paceStreamJson ?: other.paceStreamJson,
        )

    /** Datenreichtums-Score: je mehr Detailfelder + Verlaufspunkte, desto hoeher. */
    private fun HealthConnectExerciseSession.richness(): Int {
        var score = 0
        if (avgHeartRate != null) score += 200
        if (maxHeartRate != null) score += 100
        if (cadence != null) score += 100
        if (altitudeGainMeters != null) score += 50
        score += heartRateSeriesJson?.count { it == ',' } ?: 0
        score += paceStreamJson?.count { it == ',' } ?: 0
        score += (gpsTrackJson?.count { it == ',' } ?: 0) * 2
        return score
    }

    /** Vollständiger, fehlerpropagierender Read für den persistenten Sync-Cursor. */
    suspend fun bodyHistoryPermissionSignature(): String =
        listOf(
            hasWeightReadPermission(),
            hasBodyFatReadPermission(),
            hasLeanBodyMassReadPermission(),
            hasBodyWaterMassReadPermission(),
            hasBoneMassReadPermission(),
        ).joinToString(separator = "") { if (it) "1" else "0" }

    suspend fun readBodyHistoriesStrict(days: Int): HealthConnectBodyHistories {
        val c = client() ?: return HealthConnectBodyHistories()
        val end = Instant.now()
        val range = TimeRangeFilter.between(
            end.minusSeconds(days.toLong() * 24L * 60L * 60L),
            end,
        )
        val weightGranted = hasWeightReadPermission()
        val bodyFatGranted = hasBodyFatReadPermission()
        val leanGranted = hasLeanBodyMassReadPermission()
        val waterGranted = hasBodyWaterMassReadPermission()
        val boneGranted = hasBoneMassReadPermission()
        return HealthConnectBodyHistories(
            weight = if (weightGranted) {
                readAllRecords(c, WeightRecord::class, range)
                    .map { it.time.toEpochMilli() to it.weight.inKilograms }
            } else emptyList(),
            bodyFat = if (bodyFatGranted) {
                readAllRecords(c, BodyFatRecord::class, range)
                    .map { it.time.toEpochMilli() to it.percentage.value }
            } else emptyList(),
            leanBodyMass = if (leanGranted) {
                readAllRecords(c, LeanBodyMassRecord::class, range)
                    .map { it.time.toEpochMilli() to it.mass.inKilograms }
            } else emptyList(),
            bodyWaterMass = if (waterGranted) {
                readAllRecords(c, BodyWaterMassRecord::class, range)
                    .map { it.time.toEpochMilli() to it.mass.inKilograms }
            } else emptyList(),
            boneMass = if (boneGranted) {
                readAllRecords(c, BoneMassRecord::class, range)
                    .map { it.time.toEpochMilli() to it.mass.inKilograms }
            } else emptyList(),
        )
    }

    private suspend fun <T : Record> readAllRecords(
        client: HealthConnectClient,
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        ascendingOrder: Boolean = true,
    ): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = recordType,
                    timeRangeFilter = timeRangeFilter,
                    ascendingOrder = ascendingOrder,
                    pageToken = pageToken,
                )
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    private companion object {
        const val TAG = "HealthConnectMgr"

        /**
         * Frank-Entscheidung 2026-08-05: Polar ist die fuehrende Trainings-Quelle — seine
         * Trainings sollen aus Polar kommen, andere Apps nur einspringen wo Polar nichts hat.
         * Praefix, weil Polar je nach Installation `fi.polar.polarflow` oder `fi.polar.beat`
         * verwendet.
         */
        const val PREFERRED_WORKOUT_SOURCE = "fi.polar"
    }
}

/**
 * Frank-Wunsch 2026-05-16: vereinfachte Sicht auf eine Health-Connect-Trainings-
 * Session inkl. aggregierter Distanz/Kalorien/Puls. Verbraucher (AmazfitRepository)
 * mappen das auf `AmazfitWorkoutEntity` und schreiben mit `source = "health_connect"`.
 */
data class HealthConnectBodyHistories(
    val weight: List<Pair<Long, Double>> = emptyList(),
    val bodyFat: List<Pair<Long, Double>> = emptyList(),
    val leanBodyMass: List<Pair<Long, Double>> = emptyList(),
    val bodyWaterMass: List<Pair<Long, Double>> = emptyList(),
    val boneMass: List<Pair<Long, Double>> = emptyList(),
)

data class HealthConnectExerciseSession(
    val startMs: Long,
    val endMs: Long,
    val durationSeconds: Long,
    val exerciseType: Int,
    val title: String?,
    val distanceMeters: Double?,
    val calories: Double?,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    // Frank-Wunsch 2026-07-03: dieselben Detail-Felder wie ein voller Streams-Import, damit
    // Health Connect die alleinige Workout-Quelle sein kann. JSON-Strings im exakten UI-Parser-Format
    // (AmazfitTrainingDetailScreen): gpsTrackJson [[lat,lon],...], heartRateSeriesJson [[ts,bpm],...],
    // paceStreamJson [[ts,secProKm],...].
    val avgPaceSecPerKm: Double? = null,
    val maxPaceSecPerKm: Double? = null,
    val avgSpeedKmh: Double? = null,
    val maxSpeedKmh: Double? = null,
    val cadence: Int? = null,
    val strideLengthCm: Int? = null,
    val altitudeGainMeters: Double? = null,
    val altitudeLossMeters: Double? = null,
    val gpsTrackJson: String? = null,
    val heartRateSeriesJson: String? = null,
    val paceStreamJson: String? = null,
    /**
     * Frank-Diagnose 2026-08-05: Paketname der App, die diese Session in Health Connect
     * geschrieben hat (z.B. `fi.polar.beat`, `com.sec.android.app.shealth`). Nur fuer die
     * Diagnose-Sonde — so ist ohne Raten sichtbar, welche Quelle ein Training geliefert hat,
     * wenn Werte fehlen oder der Name nicht stimmt.
     */
    val sourceApp: String? = null,
)
