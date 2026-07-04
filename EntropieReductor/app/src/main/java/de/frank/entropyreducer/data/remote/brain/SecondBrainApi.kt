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
}

@Serializable
data class SecondBrainStoreRequest(
    val text: String,
    val title: String,
    val category: String,
    @SerialName("user_id") val userId: String = "frank",
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
data class SecondBrainForgetResponse(
    val ok: Boolean = false,
    val deleted: Boolean = false,
    val title: String? = null,
)
