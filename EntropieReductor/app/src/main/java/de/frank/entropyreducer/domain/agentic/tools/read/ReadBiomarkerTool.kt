package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.WhoopRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Read-Tool: liest Biomarker-Daten (Whoop-Wearable-Snapshots) aus dem lokalen Cache.
 *
 * Parameter:
 *  - tage (integer, optional, default=7, max=90):
 *    Wie viele Tage zurück sollen Snapshots gelesen werden.
 *    tage=1 → nur der jüngste Snapshot (observeLatest).
 *    tage>1 → alle Snapshots im Zeitfenster [jetzt - tage, jetzt].
 *
 * Liefert ein JSON-Objekt mit `count`, `days` und `snapshots` (Array).
 * Jeder Snapshot enthält alle verfügbaren Biomarker-Felder (Recovery, HRV,
 * Herzfrequenz, Schlaf, Strain etc.). Fehlende Felder werden als null serialisiert.
 *
 * Wenn keine Daten vorhanden sind (Whoop nicht verbunden oder Sync noch nicht
 * durchgelaufen), wird ein Success mit count=0 und einem leeren Array zurückgegeben
 * — kein Fehler, da der Nutzter über den Leer-Zustand informiert werden soll.
 */
@Singleton
class ReadBiomarkerTool
@Inject
constructor(private val whoopRepository: WhoopRepository) : AgenticTool {

    override val name: String = "read_biomarker"

    override val category: ToolCategory = ToolCategory.READ_BIOMARKER

    override val description: String =
        "Liest die Biomarker-Werte des Nutzers aus dem Whoop-Wearable für einen gewählten " +
            "Zeitraum. Nutze dieses Tool wenn du Recovery-Score, HRV, Herzfrequenz, " +
            "Schlafdaten oder Tagesbelastung (Strain) brauchst um Gesundheitsmuster zu " +
            "analysieren oder Hypothesen über körperliche Erholung zu bilden."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter-Parameter für die Biomarker-Abfrage.",
            properties =
                mapOf(
                    "tage" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Anzahl Tage zurück die abgefragt werden sollen. " +
                                    "Default: 7, Max: 90. Bei tage=1 wird nur der " +
                                    "jüngste Snapshot zurückgegeben.",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val tage =
                (obj["tage"]?.jsonPrimitive?.content?.toIntOrNull() ?: DEFAULT_DAYS)
                    .coerceIn(1, MAX_DAYS)

            val resultJson =
                if (tage == 1) {
                    // Nur den jüngsten Snapshot holen
                    val latest = whoopRepository.observeLatest().first()
                    if (latest == null) {
                        buildJsonObject {
                            put("count", 0)
                            put("days", 1)
                            put("hinweis", "Keine Biomarker-Daten verfügbar. " +
                                "Bitte Whoop verbinden und synchronisieren.")
                            put("snapshots", buildJsonArray { })
                        }
                    } else {
                        buildJsonObject {
                            put("count", 1)
                            put("days", 1)
                            put("snapshots", buildJsonArray {
                                add(snapshotToJson(latest))
                            })
                        }
                    }
                } else {
                    // Zeitfenster: von (jetzt - tage * ms_pro_tag) bis jetzt
                    val from = ctx.startedAt - tage.toLong() * MILLIS_PER_DAY
                    val snapshots =
                        whoopRepository.observeRange(from, ctx.startedAt).first()
                            .sortedByDescending { it.capturedAt }

                    if (snapshots.isEmpty()) {
                        buildJsonObject {
                            put("count", 0)
                            put("days", tage)
                            put("hinweis", "Keine Biomarker-Daten für die letzten " +
                                "$tage Tage verfügbar. Bitte Whoop synchronisieren.")
                            put("snapshots", buildJsonArray { })
                        }
                    } else {
                        buildJsonObject {
                            put("count", snapshots.size)
                            put("days", tage)
                            put("snapshots", buildJsonArray {
                                snapshots.forEach { snap -> add(snapshotToJson(snap)) }
                            })
                        }
                    }
                }

            ToolResult.Success(data = resultJson)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Lesen der Biomarker-Daten: ${t.message ?: t::class.simpleName}"
            )
        }
    }

    /**
     * Serialisiert einen BiomarkerSnapshotEntity in ein kompaktes JSON-Objekt.
     * Alle optionalen Felder werden als null ausgegeben wenn kein Wert vorhanden ist —
     * Gemini kann damit umgehen und ignoriert null-Felder beim Reasoning.
     */
    private fun snapshotToJson(
        snap: de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
    ) = buildJsonObject {
        put("id", snap.id)
        put("capturedAt", snap.capturedAt)

        // Recovery & HRV
        snap.recoveryScore?.let { put("recoveryScore", it) } ?: put("recoveryScore", null as String?)
        snap.hrvMs?.let { put("hrvMs", it) } ?: put("hrvMs", null as String?)
        snap.restingHeartRate?.let { put("restingHeartRate", it) } ?: put("restingHeartRate", null as String?)
        snap.spo2Percent?.let { put("spo2Percent", it) } ?: put("spo2Percent", null as String?)
        snap.skinTempCelsius?.let { put("skinTempCelsius", it) } ?: put("skinTempCelsius", null as String?)

        // Schlaf
        snap.sleepPerformance?.let { put("sleepPerformance", it) } ?: put("sleepPerformance", null as String?)
        snap.sleepTotalMinutes?.let { put("sleepTotalMinutes", it) } ?: put("sleepTotalMinutes", null as String?)
        snap.sleepRemMinutes?.let { put("sleepRemMinutes", it) } ?: put("sleepRemMinutes", null as String?)
        snap.sleepDeepMinutes?.let { put("sleepDeepMinutes", it) } ?: put("sleepDeepMinutes", null as String?)
        snap.sleepLightMinutes?.let { put("sleepLightMinutes", it) } ?: put("sleepLightMinutes", null as String?)
        snap.sleepAwakeMinutes?.let { put("sleepAwakeMinutes", it) } ?: put("sleepAwakeMinutes", null as String?)
        snap.sleepDisturbances?.let { put("sleepDisturbances", it) } ?: put("sleepDisturbances", null as String?)
        snap.sleepConsistencyPercent?.let { put("sleepConsistencyPercent", it) } ?: put("sleepConsistencyPercent", null as String?)
        snap.sleepEfficiencyPercent?.let { put("sleepEfficiencyPercent", it) } ?: put("sleepEfficiencyPercent", null as String?)
        snap.sleepNeedMinutes?.let { put("sleepNeedMinutes", it) } ?: put("sleepNeedMinutes", null as String?)
        snap.sleepDebtMinutes?.let { put("sleepDebtMinutes", it) } ?: put("sleepDebtMinutes", null as String?)
        snap.sleepCycleCount?.let { put("sleepCycleCount", it) } ?: put("sleepCycleCount", null as String?)
        snap.respiratoryRate?.let { put("respiratoryRate", it) } ?: put("respiratoryRate", null as String?)

        // Tagesbelastung (Strain)
        snap.dayStrain?.let { put("dayStrain", it) } ?: put("dayStrain", null as String?)
        snap.dayKilojoules?.let { put("dayKilojoules", it) } ?: put("dayKilojoules", null as String?)
        snap.averageHeartRate?.let { put("averageHeartRate", it) } ?: put("averageHeartRate", null as String?)
        snap.maxHeartRate?.let { put("maxHeartRate", it) } ?: put("maxHeartRate", null as String?)
    }

    companion object {
        private const val DEFAULT_DAYS = 7
        private const val MAX_DAYS = 90
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
