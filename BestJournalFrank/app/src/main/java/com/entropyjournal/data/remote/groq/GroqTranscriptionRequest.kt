package com.entropyjournal.data.remote.groq

import com.entropyjournal.util.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

object GroqTranscriptionRequest {

    fun createFilePart(audioFile: File): MultipartBody.Part {
        val requestBody = audioFile.asRequestBody("audio/wav".toMediaType())
        return MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
    }

    fun createModelPart() =
        Constants.GROQ_TRANSCRIPTION_MODEL.toRequestBody("text/plain".toMediaType())

    fun createLanguagePart() =
        Constants.GROQ_LANGUAGE.toRequestBody("text/plain".toMediaType())

    fun createResponseFormatPart() =
        "json".toRequestBody("text/plain".toMediaType())
}
