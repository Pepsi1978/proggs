package de.frank.cortex.data.model

import com.squareup.moshi.JsonClass

// --- Agent API ---

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val text: String,
    val session_id: String,
    val user_id: String = "frank",
    val category: String? = null,
    val title: String? = null,
    val store_timestamp: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val ok: Boolean,
    val reply: String,
    val action: String,
    val session_id: String?,
    val category: String?,
    val title: String?,
    val stored: Boolean = false,
    val replaced: Boolean = false,
    val recall_hits: Int? = null,
    val options: List<ChatOption>? = null
)

@JsonClass(generateAdapter = true)
data class ChatOption(
    val label: String,
    val send: String
)

@JsonClass(generateAdapter = true)
data class CategoriesResponse(
    val categories: List<CategoryInfo>
)

@JsonClass(generateAdapter = true)
data class CategoryInfo(
    val name: String,
    val count: Int,
    val empty: Boolean
)

@JsonClass(generateAdapter = true)
data class CreateCategoryRequest(
    val name: String
)

// --- Brain API ---

@JsonClass(generateAdapter = true)
data class CategoryCountsResponse(
    val ok: Boolean,
    val counts: Map<String, Int>,
    val total_distinct: Int
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val status: String?,
    val version: String?,
    val points: Int?,
    val embed_model: String?
)

@JsonClass(generateAdapter = true)
data class SearchRequest(
    val query: String,
    val user_id: String = "frank",
    val limit: Int = 20,
    val category: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val ok: Boolean,
    val count: Int,
    val items: List<BrainEntry>
)

@JsonClass(generateAdapter = true)
data class BrainEntry(
    val doc_id: String,
    val title: String?,
    val category: String?,
    val score: Double? = null,
    val match: String? = null,
    val text: String?,
    val created_at: String?,
    val updated_at: String?
)

@JsonClass(generateAdapter = true)
data class EntriesResponse(
    val ok: Boolean,
    val count: Int,
    val items: List<BrainEntry>
)

@JsonClass(generateAdapter = true)
data class UpdateEntryRequest(
    val doc_id: String,
    val text: String,
    val title: String? = null,
    val categories: List<String>? = null,
    val user_id: String = "frank"
)

@JsonClass(generateAdapter = true)
data class ChangeCategoryRequest(
    val doc_id: String,
    val category: String,
    val user_id: String = "frank"
)

@JsonClass(generateAdapter = true)
data class SimpleResponse(
    val ok: Boolean,
    val message: String? = null
)

// --- Dashboard API ---

@JsonClass(generateAdapter = true)
data class OverviewResponse(
    val total: Int,
    val brain: BrainOverview?,
    val agent: AgentOverview?,
    val server: ServerOverview?
)

@JsonClass(generateAdapter = true)
data class BrainOverview(
    val status: String?,
    val version: String?,
    val points: Int?
)

@JsonClass(generateAdapter = true)
data class AgentOverview(
    val status: String?,
    val model: String?,
    val sessions: Int?
)

@JsonClass(generateAdapter = true)
data class ServerOverview(
    val cpu_pct: Double?,
    val mem_used: Long?,
    val mem_total: Long?,
    val mem_pct: Double?,
    val disk_used: Long?,
    val disk_total: Long?,
    val disk_pct: Double?
)

// --- Groq STT ---

@JsonClass(generateAdapter = true)
data class GroqTranscriptionResponse(
    val text: String,
    val segments: List<GroqSegment>? = null
)

@JsonClass(generateAdapter = true)
data class GroqSegment(
    val start: Double?,
    val end: Double?,
    val text: String?,
    val no_speech_prob: Double?,
    val avg_logprob: Double?
)
