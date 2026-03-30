package com.entropyjournal.data.remote.groq

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface GroqApi {

    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribe(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("language") language: RequestBody,
        @Part("response_format") responseFormat: RequestBody
    ): GroqTranscriptionResponse
}
