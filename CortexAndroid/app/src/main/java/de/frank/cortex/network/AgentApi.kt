package de.frank.cortex.network

import de.frank.cortex.data.model.*
import retrofit2.http.*

interface AgentApi {

    @POST("/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("/categories/detail")
    suspend fun getCategories(): CategoriesResponse

    @POST("/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): CreateCategoryResponse

    @GET("/health")
    suspend fun health(): SimpleResponse

    @GET("/config")
    suspend fun getConfig(): AgentConfigResponse

    @PUT("/config")
    suspend fun updateConfig(@Body request: AgentConfigRequest): AgentConfigUpdateResponse

    @POST("/codex/auth/start")
    suspend fun startCodexAuth(): CodexAuthStartResponse

    @POST("/codex/auth/poll")
    suspend fun pollCodexAuth(@Body request: CodexAuthPollRequest): CodexAuthPollResponse

    @POST("/codex/auth/disconnect")
    suspend fun disconnectCodex(): SimpleResponse
}
