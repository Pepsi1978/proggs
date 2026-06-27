package de.frank.cortex.network

import de.frank.cortex.data.model.*
import retrofit2.http.*

interface BrainApi {

    @GET("/category-counts")
    suspend fun categoryCounts(): CategoryCountsResponse

    @GET("/health")
    suspend fun health(): HealthResponse

    @POST("/search")
    suspend fun search(@Body request: SearchRequest): SearchResponse

    @GET("/by-category")
    suspend fun byCategory(
        @Query("category") category: String,
        @Query("user_id") userId: String = "frank"
    ): EntriesResponse

    @GET("/by-parent")
    suspend fun byParent(
        @Query("parent") parent: String,
        @Query("user_id") userId: String = "frank"
    ): EntriesResponse

    @GET("/by-title")
    suspend fun byTitle(
        @Query("title") title: String,
        @Query("user_id") userId: String = "frank"
    ): BrainEntry

    @PUT("/entry")
    suspend fun updateEntry(@Body request: UpdateEntryRequest): SimpleResponse

    @POST("/entry/category")
    suspend fun changeCategory(@Body request: ChangeCategoryRequest): SimpleResponse

    @DELETE("/entry")
    suspend fun deleteEntry(
        @Query("doc_id") docId: String,
        @Query("user_id") userId: String = "frank"
    ): SimpleResponse
}
