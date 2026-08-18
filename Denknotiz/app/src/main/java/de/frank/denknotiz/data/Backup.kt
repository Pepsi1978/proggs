package de.frank.denknotiz.data

import android.content.Context
import android.net.Uri
import de.frank.denknotiz.data.local.ContextBoundaryEntity
import de.frank.denknotiz.data.local.EntryEntity
import de.frank.denknotiz.data.local.EntryType
import de.frank.denknotiz.data.local.EvaluationSnapshotEntity
import de.frank.denknotiz.data.local.SessionEntity
import de.frank.denknotiz.data.local.SnapshotStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val sessions: List<SessionEntity>,
    val entries: List<EntryEntity>,
    val snapshots: List<EvaluationSnapshotEntity>,
    val boundaries: List<ContextBoundaryEntity>,
    val profileNames: Map<String, String> = emptyMap(),
    val profileInstructions: Map<String, String> = emptyMap(),
) {
    fun toJson(): String = JSONObject().apply {
        put("formatVersion", FORMAT_VERSION)
        put("createdAt", System.currentTimeMillis())
        put("sessions", JSONArray(sessions.map { it.json() }))
        put("entries", JSONArray(entries.map { it.json() }))
        put("snapshots", JSONArray(snapshots.map { it.json() }))
        put("boundaries", JSONArray(boundaries.map { it.json() }))
        put("profileNames", JSONObject(profileNames.toMap<String, Any>()))
        put("profileInstructions", JSONObject(profileInstructions.toMap<String, Any>()))
    }.toString(2)

    companion object {
        const val FORMAT_VERSION = 2
        const val FILE_NAME = "denknotiz-sicherung.json"

        fun fromJson(raw: String): BackupPayload {
            val root = JSONObject(raw)
            val version = root.optInt("formatVersion", 0)
            require(version in 1..FORMAT_VERSION) { "Dieses Sicherungsformat wird nicht unterstützt." }
            return BackupPayload(
                sessions = root.objects("sessions").map {
                    SessionEntity(it.string("id"), it.string("title"), it.long("createdAt"), it.long("updatedAt"),
                        it.optBoolean("pinned"), it.optBoolean("archived"), it.optBoolean("titleManual"), it.optBoolean("titleGenerated"))
                },
                entries = root.objects("entries").map {
                    EntryEntity(it.string("id"), it.string("sessionId"), it.long("ordinal"), EntryType.valueOf(it.string("type")),
                        it.optString("title").ifBlank { if (it.string("type") == EntryType.AI_RESPONSE.name) "KI-Auswertung" else "Notiz" },
                        it.string("text"), it.long("createdAt"), it.long("updatedAt"), it.optString("snapshotId").takeIf(String::isNotBlank),
                        it.optBoolean("historical"), it.optString("citationsJson", "[]"), it.optBoolean("titleManual"),
                        it.optBoolean("titleGenerated"), it.optString("originalText").takeIf(String::isNotBlank))
                },
                snapshots = root.objects("snapshots").map {
                    EvaluationSnapshotEntity(it.string("id"), it.string("sessionId"), it.long("lowerOrdinalExclusive"),
                        it.long("upperOrdinalInclusive"), it.optString("sourceNoteIdsJson", "[]"), it.string("focusQuestion"),
                        it.string("profileId"), it.optBoolean("webEnabled"), it.string("model"), it.string("reasoning"),
                        it.optInt("chunkCount", 1), SnapshotStatus.valueOf(it.string("status")), it.optString("error"),
                        it.long("createdAt"), it.optLong("completedAt").takeIf { value -> value > 0 },
                        it.optString("sourceNotesJson", "[]"), it.optString("profileInstruction"))
                },
                boundaries = root.objects("boundaries").map {
                    ContextBoundaryEntity(it.string("sessionId"), it.long("lastIncludedOrdinal"),
                        it.optString("lastResponseId").takeIf(String::isNotBlank), it.long("updatedAt"))
                },
                profileNames = root.stringMap("profileNames"),
                profileInstructions = root.stringMap("profileInstructions", keepBlank = true),
            )
        }
    }
}

class SafBackup(private val context: Context) {
    suspend fun write(uri: Uri, payload: BackupPayload) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(payload.toJson().encodeToByteArray()) }
            ?: error("Die Sicherungsdatei ist nicht erreichbar.")
    }

    suspend fun read(uri: Uri): BackupPayload = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Die Sicherungsdatei konnte nicht gelesen werden.")
        BackupPayload.fromJson(raw)
    }
}

private fun SessionEntity.json() = JSONObject().put("id", id).put("title", title).put("createdAt", createdAt)
    .put("updatedAt", updatedAt).put("pinned", pinned).put("archived", archived).put("titleManual", titleManual)
    .put("titleGenerated", titleGenerated)
private fun EntryEntity.json() = JSONObject().put("id", id).put("sessionId", sessionId).put("ordinal", ordinal)
    .put("type", type.name).put("title", title).put("text", text).put("createdAt", createdAt).put("updatedAt", updatedAt)
    .put("snapshotId", snapshotId ?: "").put("historical", historical).put("citationsJson", citationsJson)
    .put("titleManual", titleManual).put("titleGenerated", titleGenerated).put("originalText", originalText ?: "")
private fun EvaluationSnapshotEntity.json() = JSONObject().put("id", id).put("sessionId", sessionId)
    .put("lowerOrdinalExclusive", lowerOrdinalExclusive).put("upperOrdinalInclusive", upperOrdinalInclusive)
    .put("sourceNoteIdsJson", sourceNoteIdsJson).put("focusQuestion", focusQuestion).put("profileId", profileId)
    .put("webEnabled", webEnabled).put("model", model).put("reasoning", reasoning).put("chunkCount", chunkCount)
    .put("status", status.name).put("error", error).put("createdAt", createdAt).put("completedAt", completedAt ?: 0)
    .put("sourceNotesJson", sourceNotesJson).put("profileInstruction", profileInstruction)
private fun ContextBoundaryEntity.json() = JSONObject().put("sessionId", sessionId)
    .put("lastIncludedOrdinal", lastIncludedOrdinal).put("lastResponseId", lastResponseId ?: "").put("updatedAt", updatedAt)
private fun JSONObject.objects(name: String): List<JSONObject> = optJSONArray(name)?.let { array ->
    (0 until array.length()).mapNotNull(array::optJSONObject)
}.orEmpty()
private fun JSONObject.string(name: String) = getString(name)
private fun JSONObject.long(name: String) = getLong(name)
private fun JSONObject.stringMap(name: String, keepBlank: Boolean = false): Map<String, String> = optJSONObject(name)?.let { json ->
    json.keys().asSequence().associateWith(json::optString).let { values -> if (keepBlank) values else values.filterValues(String::isNotBlank) }
}.orEmpty()
