package de.frank.jarvis.fernsteuerung

import android.content.Context
import de.frank.jarvis.data.Einstellungen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Fragt den Jarvis-Server, ob gerade ein Rechner (Mac oder Windows-PC) für Programmieraufträge verbunden ist. */
object RechnerStatus {
    private val HTTP = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()

    /** true = ein Rechner meldet sich, false = keiner oder Server nicht erreichbar. */
    suspend fun verbunden(context: Context): Boolean = withContext(Dispatchers.IO) {
        val e = Einstellungen.get(context.applicationContext)
        if (e.serverToken.isBlank()) return@withContext false
        runCatching {
            val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/pc").header("X-Jarvis-Token", e.serverToken).build()
            HTTP.newCall(anfrage).execute().use { a -> a.isSuccessful && JSONObject(a.body?.string().orEmpty()).optBoolean("pc") }
        }.getOrDefault(false)
    }
}
