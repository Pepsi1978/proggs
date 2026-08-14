package de.frank.stacklabor.werftstudio.service.tts

import android.content.Context
import java.io.File
import java.security.MessageDigest

class TtsAudioCache(context: Context, private val maxBytes: Long = 64L * 1024L * 1024L) {
    private val directory = File(context.applicationContext.cacheDir, "tts_audio").apply { mkdirs() }

    @Synchronized
    fun find(key: String, extension: String): File? = file(key, extension).takeIf { it.isFile && it.length() > 0L }
        ?.also { it.setLastModified(System.currentTimeMillis()) }

    @Synchronized
    fun put(key: String, extension: String, bytes: ByteArray): File {
        require(bytes.isNotEmpty())
        val target = file(key, extension)
        val partial = File(directory, target.name + ".part")
        partial.outputStream().use { it.write(bytes) }
        if (target.exists() && !target.delete()) partial.delete()
        check(partial.renameTo(target)) { "TTS-Audiodatei konnte nicht atomar übernommen werden." }
        evictIfNeeded()
        return target
    }

    fun key(provider: TtsProviderId, model: String, request: TtsSynthesisRequest): String {
        val input = listOf(CACHE_VERSION, provider.name, model, request.voiceId, request.speechRate, request.text)
            .joinToString("\u0000")
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun file(key: String, extension: String): File = File(directory, "$key.$extension")

    private fun evictIfNeeded() {
        val files = directory.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }.orEmpty()
        var size = files.sumOf(File::length)
        for (candidate in files.sortedBy(File::lastModified)) {
            if (size <= maxBytes) break
            val length = candidate.length()
            if (candidate.delete()) size -= length
        }
        directory.listFiles()?.filter { it.name.endsWith(".part") }?.forEach(File::delete)
    }

    private companion object {
        const val CACHE_VERSION = "v1"
    }
}
