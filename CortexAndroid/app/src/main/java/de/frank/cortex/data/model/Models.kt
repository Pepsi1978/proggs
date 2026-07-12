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
    val context_mode_revision: Int = 0,
    val context_prompt: String? = null,
    // S/M/XL-Profil explizit mitschicken — steuert serverseitig auch die Tavily-Suchtiefe
    // (xl = advanced-Tiefensuche mit mehr Treffern, Frank-Wunsch 2026-07-02).
    val response_size: String? = null,
    // Idempotency-Key (UUID pro Nutzer-Intent): Stream-Versuch und /chat-Fallback tragen
    // DENSELBEN Key — der Server (agent 0.48.0) verarbeitet Duplikate nur EINMAL
    // (verhindert Doppel-Speicherung nach Tunnel-Abriss, Frank-Wunsch 2026-07-02).
    val request_id: String? = null,
    val memory_edit: Boolean = false,
    val memory_doc_id: String? = null,
    val memory_categories: List<String>? = null
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
    val categories: List<String>? = null,
    val doc_id: String? = null,
    val memory_text: String? = null,
    val stored_text: String? = null,
    val memory_editable: Boolean = false,
    val stored: Boolean = false,
    val replaced: Boolean = false,
    val recall_hits: Int? = null,
    val options: List<ChatOption>? = null
)

@JsonClass(generateAdapter = true)
data class EndSessionRequest(
    val session_id: String,
    val user_id: String = "frank"
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

// Antwort des Agenten auf POST /categories: der KANONISCHE key (evtl. anders normalisiert als der
// getippte Name) + die vollstaendige Kategorienliste (inkl. leerer). key nutzen, damit der Eintrag
// exakt denselben Kategorie-String bekommt, den by-category filtert.
@JsonClass(generateAdapter = true)
data class CreateCategoryResponse(
    val ok: Boolean,
    val key: String? = null,
    val categories: List<String>? = null,
    val detail: String? = null
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
    val categories: List<String>? = null,
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

// Multi-Category: die VOLLSTAENDIGE Kategorie-Liste eines Eintrags setzen (brain re-embedded -> in
// jeder Kategorie auffindbar). Erste = primaer. Hinter dem "+ Kategorie" in der Detailansicht.
@JsonClass(generateAdapter = true)
data class SetCategoriesRequest(
    val doc_id: String,
    val categories: List<String>,
    val user_id: String = "frank"
)

@JsonClass(generateAdapter = true)
data class SetCategoriesResponse(
    val ok: Boolean,
    val doc_id: String? = null,
    val categories: List<String>? = null,
    val parents: List<String>? = null,
    val detail: String? = null
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
data class AgentUserContextPrompt(
    val id: String,
    val text: String,
    val enabled: Boolean = true,
    val original_text: String? = null
)

@JsonClass(generateAdapter = true)
data class AgentConfigResponse(
    val models: Map<String, String> = emptyMap(),
    val reasoning: Map<String, String> = emptyMap(),
    val reasoning_available: List<String> = emptyList(),
    // Router (Schritt 1 = Einordnung der Nachricht): eigenes Modell/Reasoning; leer = "wie Hauptagent".
    val router_model: String = "",
    val router_reasoning: String = "",
    // Zentrale A/S/M/XL- und Modus-Prompts (Quelle der Wahrheit auf dem Server; custom=false -> App darf seeden).
    val size_prompts: Map<String, String> = emptyMap(),
    val size_prompts_custom: Boolean = false,
    val context_prompts: Map<String, String> = emptyMap(),
    val context_prompts_custom: Boolean = false,
    val user_context_prompts: List<AgentUserContextPrompt> = emptyList(),
    val user_context_prompts_custom: Boolean = false,
    val limits: RuntimeLimits = RuntimeLimits(),
    val codex: CodexState? = null,
    val tavily_enabled: Boolean = true,
    val memory_web_influence: String = "normal",
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
    // Alle Felder optional — der Server aendert nur, was gesetzt ist (z.B. reiner Prompt-Sync).
    val haupt_model: String? = null,
    val speicher_model: String? = null,
    val abfrage_model: String? = null,
    val haupt_reasoning: String? = null,
    val speicher_reasoning: String? = null,
    val abfrage_reasoning: String? = null,
    // "auto" = wie Hauptagent (Server speichert dann leer); sonst explizite Router-Wahl.
    val router_model: String? = null,
    val router_reasoning: String? = null,
    // Zentrale A/S/M/XL- und Modus-Prompts zum Server syncen (Seed + jede Aenderung in den Einstellungen).
    val size_prompt_auto: String? = null,
    val size_prompt_s: String? = null,
    val size_prompt_m: String? = null,
    val size_prompt_xl: String? = null,
    val context_prompt_save: String? = null,
    val context_prompt_search: String? = null,
    val user_context_prompts: List<AgentUserContextPrompt>? = null,
    val tavily_enabled: Boolean? = null,
    val memory_web_influence: String? = null,
    val limits: RuntimeLimits? = null
)

@JsonClass(generateAdapter = true)
data class AgentConfigUpdateResponse(
    val status: String,
    val models: Map<String, String> = emptyMap(),
    val reasoning: Map<String, String> = emptyMap(),
    val tavily_enabled: Boolean = true,
    val memory_web_influence: String = "normal",
    val limits: RuntimeLimits = RuntimeLimits()
)

@JsonClass(generateAdapter = true)
data class WebsearchToggleSelftestResponse(
    val ok: Boolean,
    val passed: Int,
    val total: Int,
    val current: WebsearchToggleCurrent? = null
)

@JsonClass(generateAdapter = true)
data class WebsearchToggleCurrent(
    val tavily_enabled: Boolean = true,
    val model: String? = null,
    val native_web_supported: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RuntimeLimits(
    val agent: AgentRuntimeLimits = AgentRuntimeLimits(),
    val brain: BrainRuntimeLimits = BrainRuntimeLimits()
)

@JsonClass(generateAdapter = true)
data class AgentRuntimeLimits(
    val dedup_candidates: Int = 3,
    val history_max: Int = 40,
    val history_compress_at: Int = 0,
    val recall_full_limit: Int = 0,
    val multi_query_variants: Int = 2,
    val entity_extract_chars: Int = 2500,
    val entity_docs_limit: Int = 0,
    val lese_snippet_chars: Int = 1200,
    val answer_snippet_limit: Int = 12,
    val source_chip_limit: Int = 8,
    val answer_hit_chars: Int = 8000,
    val answer_total_chars: Int = 24000,
    val answer_max_tokens: Int = 4096,
    val recall_working_cache_threshold: Int = 20,
    val recall_normal_max: Int = 50,
    val full_category_batch_chars: Int = 28000,
    val context_prompt_max_chars: Int = 4000,
    val chat_text_max_chars: Int = 500000
)

@JsonClass(generateAdapter = true)
data class BrainRuntimeLimits(
    val chunk_chars: Int = 4000,
    val chunk_overlap: Int = 200,
    val search_overfetch_factor: Int = 4,
    val search_date_overfetch_factor: Int = 20,
    val bm25_candidate_factor: Int = 4,
    val bm25_min_candidates: Int = 20
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
data class VitalsResponse(
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
    val avg_logprob: Double?,
    val compression_ratio: Double? = null
)
