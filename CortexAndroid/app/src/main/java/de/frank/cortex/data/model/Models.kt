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
    val store_timestamp: Boolean = true,
    val context_mode: String? = null,
    val context_prompt: String? = null,
    // S/M/XL-Profil explizit mitschicken — steuert serverseitig auch die Tavily-Suchtiefe
    // (xl = advanced-Tiefensuche mit mehr Treffern, Frank-Wunsch 2026-07-02).
    val response_size: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val ok: Boolean,
    // True genau EINMAL pro Session, wenn der Verlauf erstmals das Server-Kontextfenster
    // (AGENT_HISTORY_MAX, 40 Nachrichten) ueberschreitet -> sichtbare Chat-Meldung.
    val context_limit_reached: Boolean = false,
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

@JsonClass(generateAdapter = true)
data class ModelPrice(
    val input: Double? = null,
    val output: Double? = null
)

@JsonClass(generateAdapter = true)
data class AgentConfigResponse(
    val models: Map<String, String> = emptyMap(),
    val reasoning: Map<String, String> = emptyMap(),
    val reasoning_available: List<String> = emptyList(),
    // Router (Schritt 1 = Einordnung der Nachricht): eigenes Modell/Reasoning; leer = "wie Hauptagent".
    val router_model: String = "",
    val router_reasoning: String = "",
    val codex: CodexState? = null,
    val tavily_enabled: Boolean = true,
    val model: String? = null,
    val default: String? = null,
    val available: List<String> = emptyList(),
    val model_prices: Map<String, ModelPrice> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class CodexState(
    val connected: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AgentConfigRequest(
    val haupt_model: String,
    val speicher_model: String,
    val abfrage_model: String,
    val haupt_reasoning: String? = null,
    val speicher_reasoning: String? = null,
    val abfrage_reasoning: String? = null,
    // "auto" = wie Hauptagent (Server speichert dann leer); sonst explizite Router-Wahl.
    val router_model: String? = null,
    val router_reasoning: String? = null,
    val tavily_enabled: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class AgentConfigUpdateResponse(
    val status: String,
    val models: Map<String, String> = emptyMap(),
    val reasoning: Map<String, String> = emptyMap(),
    val tavily_enabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CodexAuthStartResponse(
    val ok: Boolean,
    val auth_id: String,
    val user_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int
)

@JsonClass(generateAdapter = true)
data class CodexAuthPollRequest(
    val auth_id: String
)

@JsonClass(generateAdapter = true)
data class CodexAuthPollResponse(
    val ok: Boolean,
    val status: String,
    val connected: Boolean = false,
    val models: List<String> = emptyList()
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
