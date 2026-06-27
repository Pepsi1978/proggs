package de.frank.cortex.network

import de.frank.cortex.data.model.*
import retrofit2.http.*

interface AgentApi {

    @POST("/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("/categories/detail")
    suspend fun getCategories(): CategoriesResponse

    @POST("/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): SimpleResponse

    @GET("/health")
    suspend fun health(): SimpleResponse
}
