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

    // Schicht 2: verbose_json liefert die Confidence-Felder (no_speech_prob, avg_logprob,
    // compression_ratio) fuers Anti-Halluzinations-Gate — bei Groq ohne Mehrlatenz/-kosten.
    fun createResponseFormatPart() =
        "verbose_json".toRequestBody("text/plain".toMediaType())

    // temperature=0 als Basis (verhindert Stille-Halluzination nicht allein, aber sinnvoll).
    fun createTemperaturePart() =
        "0".toRequestBody("text/plain".toMediaType())
}
