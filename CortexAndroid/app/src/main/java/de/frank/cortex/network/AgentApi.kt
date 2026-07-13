package de.frank.cortex.network

import de.frank.cortex.data.model.*
import retrofit2.http.*

interface AgentApi {

    @POST("/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @POST("/end")
    suspend fun endSession(@Body request: EndSessionRequest): SimpleResponse

    @GET("/categories/detail")
    suspend fun getCategories(): CategoriesResponse

    @POST("/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): CreateCategoryResponse

    // Eigenes Modell: Agent-/health sendet KEIN "ok"-Feld — SimpleResponse haette beim ersten
    // echten Aufruf mit JsonDataException (Pflichtfeld fehlt) versagt.
    @GET("/health")
    suspend fun health(): AgentHealthResponse

    @GET("/config")
    suspend fun getConfig(): AgentConfigResponse

    @PUT("/config")
    suspend fun updateConfig(@Body request: AgentConfigRequest): AgentConfigUpdateResponse

    @GET("/websearch-toggle-selftest")
    suspend fun websearchToggleSelftest(): WebsearchToggleSelftestResponse

    @POST("/codex/auth/start")
    suspend fun startCodexAuth(): CodexAuthStartResponse

    @POST("/codex/auth/poll")
    suspend fun pollCodexAuth(@Body request: CodexAuthPollRequest): CodexAuthPollResponse

    @POST("/codex/auth/disconnect")
    suspend fun disconnectCodex(): SimpleResponse
}
