package de.frank.jarvis.tunnel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.data.Einstellungen
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Erneuert die Berechtigung, mit der der eigene Server das Tagebuch aus Google Drive liest.
 *
 * Ablauf: Jarvis öffnet die Zustimmungsseite von Google im Browser (nur Lesen von Drive). Nach dem Tipp auf
 * „Zulassen“ leitet Google auf eine Adresse dieses Handys zurück (127.0.0.1), an der Jarvis für diesen einen
 * Moment lauscht. Jarvis tauscht die Antwort gegen den Zugang und gibt ihn an den Server weiter.
 * Es ist derselbe Nur-Lese-Zugang, den auch `rclone` am PC einrichtet.
 */
object DriveFreigabe {
    private const val UMFANG = "https://www.googleapis.com/auth/drive.readonly"
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    val eingerichtet: Boolean get() = BuildConfig.DRIVE_CLIENT_ID.isNotBlank() && BuildConfig.DRIVE_CLIENT_SECRET.isNotBlank()

    /** Führt die Freigabe durch. Rückgabe: Meldung für den Benutzer; [erfolg] sagt, ob es geklappt hat. */
    suspend fun erneuere(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!eingerichtet) return@withContext false to "Diese Fassung von Jarvis kennt die Google-Zugangsdaten nicht (Bau ohne ~/SK)."
        val e = Einstellungen.get(context)
        runCatching {
            ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { lauscher ->
                lauscher.soTimeout = 5 * 60_000
                val rueckweg = "http://127.0.0.1:${lauscher.localPort}"
                val zufall = SecureRandom()
                val kennung = ByteArray(16).also(zufall::nextBytes).joinToString("") { "%02x".format(it) }
                val pruefwort = Base64.encodeToString(ByteArray(48).also(zufall::nextBytes), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val pruefsumme = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(pruefwort.toByteArray(Charsets.US_ASCII)), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val adresse = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&access_type=offline&prompt=consent" +
                    "&client_id=" + kodiert(BuildConfig.DRIVE_CLIENT_ID) + "&redirect_uri=" + kodiert(rueckweg) + "&scope=" + kodiert(UMFANG) +
                    "&state=" + kennung + "&code_challenge=" + pruefsumme + "&code_challenge_method=S256"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

                // Auf die Rückleitung warten. Der Browser fragt nebenbei auch nach dem Seitensymbol: solche Anfragen überspringen.
                var code: String? = null
                var abgelehnt: String? = null
                while (code == null && abgelehnt == null) {
                    lauscher.accept().use { verbindung ->
                        verbindung.soTimeout = 10_000
                        val zeile = verbindung.getInputStream().bufferedReader().readLine().orEmpty()
                        val werte = zeile.substringAfter("?", "").substringBefore(" ").split("&").mapNotNull {
                            val teile = it.split("=", limit = 2); if (teile.size == 2) teile[0] to URLDecoder.decode(teile[1], "UTF-8") else null
                        }.toMap()
                        if (werte["state"] == kennung) { code = werte["code"]; abgelehnt = werte["error"] ?: if (code == null) "keine Antwort" else null }
                        val text = when {
                            code != null -> "Fertig. Du kannst zu Jarvis zurückkehren."
                            abgelehnt != null -> "Die Freigabe wurde nicht erteilt. Du kannst zu Jarvis zurückkehren."
                            else -> ""
                        }
                        val seite = "<html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"></head><body style=\"font-family:sans-serif;padding:32px\"><h2>Jarvis</h2><p>$text</p></body></html>"
                        val bytes = seite.toByteArray(Charsets.UTF_8)
                        verbindung.getOutputStream().apply {
                            write(("HTTP/1.1 " + (if (text.isEmpty()) "404 Not Found" else "200 OK") + "\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray())
                            write(bytes); flush()
                        }
                    }
                }
                if (code == null) return@use false to "Google hat die Freigabe nicht erteilt ($abgelehnt)."

                val tausch = client.newCall(
                    Request.Builder().url("https://oauth2.googleapis.com/token").post(
                        FormBody.Builder().add("grant_type", "authorization_code").add("code", code!!).add("redirect_uri", rueckweg)
                            .add("client_id", BuildConfig.DRIVE_CLIENT_ID).add("client_secret", BuildConfig.DRIVE_CLIENT_SECRET).add("code_verifier", pruefwort).build(),
                    ).build(),
                ).execute().use { antwort -> JSONObject(antwort.body?.string().orEmpty()).also { check(antwort.isSuccessful) { it.optString("error_description", "Google antwortet mit ${antwort.code}") } } }
                val dauerhaft = tausch.optString("refresh_token").takeIf { it.isNotBlank() } ?: return@use false to "Google hat keinen dauerhaften Zugang geliefert. Bitte noch einmal versuchen."

                // Im selben Aufbau, den rclone in seiner Konfiguration erwartet.
                val zugang = JSONObject().put("access_token", tausch.optString("access_token")).put("token_type", "Bearer").put("refresh_token", dauerhaft)
                    .put("expiry", Instant.now().plusSeconds(tausch.optLong("expires_in", 3600)).toString())
                client.newCall(
                    Request.Builder().url("https://${e.serverHost}/geraet/drive-zugang").header("X-Jarvis-Token", e.serverToken)
                        .post(JSONObject().put("token", zugang).toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build(),
                ).execute().use { antwort -> check(antwort.isSuccessful) { "Der Server hat den Zugang nicht angenommen (${antwort.code})." } }
                true to "Berechtigung erneuert. Das Tagebuch wird wieder gelesen."
            }
        }.getOrElse { fehler ->
            false to (if (fehler is java.net.SocketTimeoutException) "Die Freigabe wurde nicht abgeschlossen (Zeit abgelaufen)." else "Erneuern fehlgeschlagen: ${fehler.message ?: fehler.javaClass.simpleName}")
        }
    }

    private fun kodiert(text: String): String = URLEncoder.encode(text, "UTF-8")
}
