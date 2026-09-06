package de.frank.entropyreducer.data.repository

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
import de.frank.entropyreducer.data.local.dao.HealthConnectValueDao
import de.frank.entropyreducer.data.local.entities.HealthConnectValueEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.settings.AppSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.round
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

data class ZeppBodySyncStatus(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastReadAtMs: Long = 0L,
)

/** Local, read-only Zepp export. Health Connect remains independent for workouts. */
@Singleton
class ZeppBodyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: HealthConnectValueDao,
    private val settings: AppSettings,
    private val diagnostics: DiagnosticLogger,
    private val syncCoordinator: Lazy<SyncCoordinator>,
) {
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow(ZeppBodySyncStatus())
    val status: StateFlow<ZeppBodySyncStatus> = mutableStatus.asStateFlow()

    fun isAvailable(): Boolean = try {
        val receiver = context.packageManager.getReceiverInfo(ComponentName(PACKAGE, RECEIVER), 0)
        receiver.enabled && receiver.applicationInfo.enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    suspend fun syncToCache(): Int = mutex.withLock {
        mutableStatus.value = mutableStatus.value.copy(isLoading = true, error = null)
        try {
            check(isAvailable()) { "Zepp ist nicht installiert oder die direkte Schnittstelle fehlt." }
            val requestId = UUID.randomUUID().toString()
            val request = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", requestId)
                .put("method", "tools/call")
                .put("params", JSONObject().put("name", "getProfile").put("arguments", JSONObject()))
            val reply = withTimeoutOrNull(20_000L) {
                suspendCancellableCoroutine<String?> { continuation ->
                    val resultReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            if (continuation.isActive) continuation.resume(resultData)
                        }
                    }
                    context.sendOrderedBroadcast(
                        Intent(ACTION).setComponent(ComponentName(PACKAGE, RECEIVER))
                            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                            .putExtra("mcpRequest", request.toString()),
                        null,
                        resultReceiver,
                        Handler(Looper.getMainLooper()),
                        0,
                        null,
                        null,
                    )
                }
            }
            check(!reply.isNullOrBlank()) { "Zepp antwortet nicht. Zepp öffnen und erneut aktualisieren." }
            val now = System.currentTimeMillis()
            val rows = withContext(Dispatchers.Default) { parseMeasurement(reply, requestId, now) }
            val existing = dao.getAll().associateBy { it.metric to it.timestampMs }
            val changed = rows.filter { existing[it.metric to it.timestampMs]?.value != it.value }
            if (changed.isNotEmpty()) dao.upsertAll(changed)
            settings.setLastZeppBodySync(now)
            mutableStatus.value = ZeppBodySyncStatus(lastReadAtMs = now)
            if (changed.isNotEmpty()) {
                syncCoordinator.get().requestSync(
                    "Biomarker: direkte Zepp-Körperwerte aktualisiert",
                    SyncCoordinator.BIOMARKER_DEBOUNCE_MS,
                )
            }
            // Never log the profile response: it also contains unrelated personal health data.
            diagnostics.success(DiagnosticArea.BIOMARKER, "Zepp direkt: ${rows.size} Körperwerte gelesen, ${changed.size} aktualisiert")
            rows.size
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val message = when (error) {
                is IllegalStateException -> error.message.orEmpty()
                else -> "Zepp-Körperdaten konnten nicht gelesen werden. Zepp öffnen und erneut aktualisieren."
            }
            mutableStatus.value = mutableStatus.value.copy(error = message)
            diagnostics.warn(DiagnosticArea.BIOMARKER, "Zepp direkt: $message")
            throw IllegalStateException(message)
        } finally {
            mutableStatus.value = mutableStatus.value.copy(isLoading = false)
        }
    }

    /** Keep the shipped cache/backup format; Zepp rows have their own source-prefixed keys. */
    fun observeHistories(): Flow<Map<String, List<Pair<Long, Double>>>> = dao.observeAll()
        .map { rows ->
            val zone = ZoneId.systemDefault()
            val direct = rows.filter { it.metric.startsWith(SOURCE_PREFIX) }.groupBy { it.metric.removePrefix(SOURCE_PREFIX) }
            val legacy = rows.filterNot { it.metric.startsWith(SOURCE_PREFIX) }.groupBy { it.metric }
            (legacy.keys + direct.keys).associateWith { metric ->
                val zeppDays = direct[metric].orEmpty().associate {
                    Instant.ofEpochMilli(it.timestampMs).atZone(ZoneOffset.UTC).toLocalDate() to it.value
                }
                val oldPoints = legacy[metric].orEmpty().filterNot {
                    Instant.ofEpochMilli(it.timestampMs).atZone(zone).toLocalDate() in zeppDays
                }.map { it.timestampMs to it.value }
                // Zepp provides a DATE, not a measurement time. UTC day keys are portable;
                // local day boundaries are used only to place points on the existing charts.
                (oldPoints + zeppDays.map { (date, value) ->
                    date.atStartOfDay(zone).toInstant().toEpochMilli() to value
                }).sortedBy { it.first }
            }
        }.distinctUntilChanged().flowOn(Dispatchers.Default)

    private fun parseMeasurement(reply: String, requestId: String, now: Long): List<HealthConnectValueEntity> {
        val response = try { JSONObject(reply) } catch (_: Exception) {
            error("Zepp ist noch nicht bereit. Zepp öffnen und erneut aktualisieren.")
        }
        check(response.optString("id") == requestId && response.optString("jsonrpc") == "2.0") {
            "Zepp hat keine passende Exportantwort geliefert. Zepp öffnen und erneut aktualisieren."
        }
        check(response.optJSONObject("error") == null) {
            "Die Zepp-Schnittstelle ist nicht bereit. Zepp öffnen und erneut aktualisieren."
        }
        val result = response.optJSONObject("result")
        check(result != null && !result.optBoolean("isError")) { "Zepp hat den Körperdaten-Export abgelehnt." }
        val text = result.optJSONArray("content")?.optJSONObject(0)?.optString("text")
        check(!text.isNullOrBlank()) { "Zepp hat keine Körperdaten geliefert." }
        val body = JSONObject(text).optJSONObject("lastBodyMeasurement")
        check(body != null) { "In Zepp ist keine letzte Körpermessung verfügbar." }
        val date = try { LocalDate.parse(body.optString("date")) } catch (_: Exception) {
            error("Der Zepp-Messung fehlt ein gültiges Datum.")
        }
        check(!date.isAfter(LocalDate.now().plusDays(1))) { "Das Zepp-Messdatum liegt in der Zukunft." }
        val timestamp = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val values = linkedMapOf<String, Double>()
        fun read(field: String, key: String, range: ClosedFloatingPointRange<Double>, mass: Boolean = false): Double? {
            val value = number(body.opt(field), mass)?.takeIf { it.isFinite() && it in range }
            if (value != null) values[key] = value
            return value
        }
        val weight = read("weight", METRIC_WEIGHT, 1.0..700.0, mass = true)
        check(weight != null) { "Zepp hat kein gültiges Messgewicht geliefert." }
        val lean = read("leanBodyMass", METRIC_LEAN, 0.1..weight, mass = true)
        if (lean != null) {
            values[METRIC_BODY_FAT] = round((1.0 - lean / weight) * 1000.0) / 10.0
        }
        read("muscle", METRIC_MUSCLE, 0.1..weight, mass = true)
        read("skeletalMuscle", METRIC_SKELETAL_MUSCLE, 0.1..weight, mass = true)
        read("boneMass", METRIC_BONE, 0.1..weight, mass = true)
        read("visceralFat", METRIC_VISCERAL_FAT, 0.0..100.0)
        read("bmi", METRIC_BMI, 1.0..200.0)
        read("proteinRate", METRIC_PROTEIN, 0.0..100.0)
        read("moisture", METRIC_WATER_PERCENT, 0.0..100.0)?.let {
            values[METRIC_WATER] = round(weight * it) / 100.0
        }
        return values.map { (metric, value) ->
            HealthConnectValueEntity(SOURCE_PREFIX + metric, timestamp, value, now)
        }
    }

    private fun number(raw: Any?, mass: Boolean): Double? {
        if (raw is Number) return raw.toDouble()
        if (raw !is String) return null
        val normalized = buildString {
            raw.trim().lowercase(java.util.Locale.ROOT).forEach { ch ->
                when {
                    Character.digit(ch, 10) >= 0 -> append(Character.digit(ch, 10))
                    ch == ',' || ch == '\u066b' -> append('.')
                    ch == '\u066a' -> append('%')
                    else -> append(ch)
                }
            }
        }
        val match = Regex("^([+-]?[0-9]+(?:\\.[0-9]+)?)\\s*(kg|lbs?|%)?$").matchEntire(normalized) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        return when (match.groupValues[2]) {
            "" -> value // Zepp 10.8.1 serializes Mass in kilograms without a unit suffix.
            "kg" -> if (mass) value else null
            "lb", "lbs" -> if (mass) value * 0.45359237 else null
            "%" -> if (!mass) value else null
            else -> null
        }
    }

    companion object {
        const val PACKAGE = "com.huami.watch.hmwatchmanager"
        private const val RECEIVER = "com.huami.health.matrix.context.broadcast.McpBroadcastReceiver"
        private const val ACTION = "com.huami.health.mcp.EXPORT_DATA"
        const val SOURCE_PREFIX = "zepp_"
        const val METRIC_WEIGHT = "weight"
        const val METRIC_BODY_FAT = "body_fat"
        const val METRIC_LEAN = "lean_body_mass"
        const val METRIC_WATER = "body_water"
        const val METRIC_BONE = "bone_mass"
        const val METRIC_MUSCLE = "muscle_mass"
        const val METRIC_SKELETAL_MUSCLE = "skeletal_muscle_mass"
        const val METRIC_VISCERAL_FAT = "visceral_fat"
        const val METRIC_BMI = "bmi"
        const val METRIC_PROTEIN = "protein_percent"
        const val METRIC_WATER_PERCENT = "body_water_percent"
    }
}
