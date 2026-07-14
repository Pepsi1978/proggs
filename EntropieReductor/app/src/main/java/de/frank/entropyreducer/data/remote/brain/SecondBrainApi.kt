package de.frank.entropyreducer.data.remote.brain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SecondBrainApi {
    @GET("health")
    suspend fun health(@Header("Authorization") authorization: String): SecondBrainHealthResponse

    /** Authentifizierter, kompakter Read fuer den Verbindungstest. */
    @GET("list")
    suspend fun list(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 0,
    ): SecondBrainListResponse

    @POST("store")
    suspend fun store(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SecondBrainStoreRequest,
    ): SecondBrainStoreResponse

    /** Loescht einen Eintrag im Second Brain per Titel (DELETE /by-title). */
    @DELETE("by-title")
    suspend fun forget(
        @Header("Authorization") authorization: String,
        @Query("title") title: String,
    ): SecondBrainForgetResponse

    /** Alle Eintraege einer Kategorie (GET /by-category) — fuer den vollstaendigen Neu-Sync. */
    @GET("by-category")
    suspend fun byCategory(
        @Header("Authorization") authorization: String,
        @Query("category") category: String,
    ): SecondBrainCategoryResponse
}

@Serializable
data class SecondBrainStoreRequest(
    val text: String,
    val title: String,
    val category: String,
    @SerialName("user_id") val userId: String = "frank",
    val source: String = "entropyreductor",
)

@Serializable
data class SecondBrainStoreResponse(
    val ok: Boolean = false,
    @SerialName("doc_id") val docId: String? = null,
    val replaced: Boolean = false,
)

@Serializable
data class SecondBrainHealthResponse(
    val status: String = "unknown",
    val ready: Boolean = false,
    val version: String? = null,
)

@Serializable
data class SecondBrainListResponse(
    val ok: Boolean = false,
    val ready: Boolean = false,
    val count: Int = 0,
)

@Serializable
data class SecondBrainForgetResponse(
    val ok: Boolean = false,
    val deleted: Boolean = false,
    val title: String? = null,
)

@Serializable
data class SecondBrainCategoryResponse(
    val ok: Boolean = false,
    val ready: Boolean = false,
    val count: Int = 0,
    val items: List<SecondBrainCategoryItem> = emptyList(),
)

@Serializable
data class SecondBrainCategoryItem(
    @SerialName("doc_id") val docId: String? = null,
    val title: String? = null,
    val text: String = "",
    val source: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @kotlinx.serialization.Transient val appRowId: String? = null,
)
