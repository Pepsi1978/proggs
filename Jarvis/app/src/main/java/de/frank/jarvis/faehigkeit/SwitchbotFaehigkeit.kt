package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Base64
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks SwitchBot-Thermo-Hygrometer (Balkon, Wohnzimmer, Schlafzimmer), per Bluetooth am Hub Mini, der sie in die SwitchBot-Cloud meldet.
 * Nur Lesen über die offizielle Cloud-API v1.1 (GET /devices, GET /devices/{id}/status), signiert mit Token und Secret aus
 * Einstellungen → Konten und APIs (HMAC-SHA256 über Token + Zeit + Nonce, Base64, Großbuchstaben).
 * Bewusst ohne Zwischenspeicher: Ist der Hub oder ein Sensor offline (Status 161/171) oder liefert er keine Werte, meldet Jarvis das statt alter Werte.
 * Die Cloud nennt keine eigene Messzeit; als Zeit gilt der Abruf.
 */
class SwitchbotFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "switchbot"
    override val name = "Raumklima"
    override val beschreibung = "Temperatur und Luftfeuchtigkeit der SwitchBot-Sensoren in Franks Wohnung (Balkon, Wohnzimmer, Schlafzimmer)."
    override val hinweise =
        "Fragt Frank nach Temperatur oder Luftfeuchtigkeit in seiner Wohnung, auf dem Balkon, im Wohn- oder Schlafzimmer („Wie warm ist es im Schlafzimmer?“), " +
            "nutze raumklima_lesen. Nenne je Sensor Name, Temperatur, Luftfeuchtigkeit und die Abrufzeit. Meldet es einen Sensor oder den Hub als nicht erreichbar, " +
            "sag das genau so und nenne für ihn keine Werte. Für das Wetter draußen bleibt wetter_lesen zuständig."

    private val s get() = SecureSettings.get(context)

    override fun stoerung(): String? =
        if (s.switchbotToken.isBlank() || s.switchbotSecret.isBlank()) "SwitchBot-Token und -Secret fehlen (Einstellungen → Konten und APIs)." else null

    override val werkzeuge = listOf(
        Werkzeug(
            name = "raumklima_lesen",
            titel = "Raumklima lesen",
            beschreibung = "Liest jetzt frisch aus der SwitchBot-Cloud Temperatur (°C) und Luftfeuchtigkeit (%) von Franks SwitchBot-Sensoren " +
                "(Balkon, Wohnzimmer, Schlafzimmer), je mit Name und Abrufzeit. Nicht erreichbare Sensoren oder ein Hub offline werden gemeldet, nie alte Werte. Nur Lesen.",
            schema = schema("sensor" to text("Optional: nur dieser Sensor, z. B. Balkon. Weglassen = alle.")),
            nurLesen = true,
            taetigkeit = "liest die Raumsensoren",
        ) { eingabe -> abrufen(eingabe.optString("sensor").trim()) },
    )

    /** Ergebnis eines Sensors: Werte oder ein verständlicher Grund, warum es keine gibt. */
    private class Sensor(val name: String, val typ: String, val temperatur: Double? = null, val feuchte: Double? = null, val batterie: Int? = null, val problem: String? = null)

    private suspend fun abrufen(nur: String): Ergebnis = withContext(Dispatchers.IO) {
        stoerung()?.let { return@withContext Ergebnis(it, fehler = true) }
        val geraete = try {
            anfrage("/v1.1/devices").optJSONObject("body")?.optJSONArray("deviceList") ?: JSONArray()
        } catch (e: Problem) {
            return@withContext Ergebnis(e.message ?: "SwitchBot antwortet nicht.", fehler = true)
        }
        val alle = (0 until geraete.length()).mapNotNull { geraete.optJSONObject(it) }.filter { SENSORTYP.containsMatchIn(it.optString("deviceType")) }
        if (alle.isEmpty()) return@withContext Ergebnis(
            "In deinem SwitchBot-Konto ist kein Thermo-Hygrometer zu sehen. Prüfe in der SwitchBot-App, ob die Sensoren mit dem Hub verbunden sind und bei jedem der Cloud-Dienst eingeschaltet ist.", fehler = true)
        val gewaehlt = if (nur.isEmpty()) alle else alle.filter { it.optString("deviceName").contains(nur, ignoreCase = true) }
        if (gewaehlt.isEmpty()) return@withContext Ergebnis(
            "Einen Sensor „$nur“ gibt es nicht. Vorhanden: " + alle.joinToString(", ") { it.optString("deviceName") } + ".", fehler = true)

        val sensoren = coroutineScope { gewaehlt.map { g -> async { lesen(g) } }.awaitAll() }
        val uhr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY))
        val fehlend = if (nur.isNotEmpty()) emptyList() else ERWARTET.filter { name -> sensoren.none { it.name.contains(name, ignoreCase = true) } }
        val zeilen = sensoren.map { z ->
            if (z.problem != null) "- ${z.name}: ${z.problem}"
            else "- ${z.name}: ${zahl(z.temperatur!!)} °C, ${zahl(z.feuchte!!, 0)} % Luftfeuchtigkeit" + (z.batterie?.let { ", Batterie $it %" } ?: "")
        } + fehlend.map { "- $it: nicht in deinem SwitchBot-Konto gefunden (Name in der SwitchBot-App prüfen)." }
        val text = "Abgerufen um $uhr Uhr aus der SwitchBot-Cloud (sie nennt keine eigene Messzeit):\n" + zeilen.joinToString("\n")
        val struktur = JSONObject().put("abgerufen", uhr).put("sensoren", JSONArray(sensoren.map { z ->
            JSONObject().put("name", z.name).put("typ", z.typ).apply {
                if (z.problem != null) put("problem", z.problem)
                else { put("temperatur_c", z.temperatur); put("luftfeuchtigkeit_prozent", z.feuchte); z.batterie?.let { put("batterie_prozent", it) } }
            }
        }))
        Ergebnis(text, fehler = sensoren.all { it.problem != null }, struktur = struktur)
    }

    private fun lesen(g: JSONObject): Sensor {
        val name = g.optString("deviceName").ifBlank { g.optString("deviceId") }
        val typ = g.optString("deviceType")
        if (g.has("enableCloudService") && !g.optBoolean("enableCloudService", true))
            return Sensor(name, typ, problem = "keine Werte, weil der Cloud-Dienst für diesen Sensor in der SwitchBot-App ausgeschaltet ist.")
        val antwort = try {
            anfrage("/v1.1/devices/${g.optString("deviceId")}/status", status = true)
        } catch (e: Problem) {
            return Sensor(name, typ, problem = e.message)
        }
        val body = antwort.optJSONObject("body") ?: JSONObject()
        val temperatur = body.wert("temperature")
        val feuchte = body.wert("humidity")
        if (temperatur == null || feuchte == null) return Sensor(name, typ, problem = "nicht erreichbar, die Cloud liefert gerade keine Messwerte.")
        return Sensor(name, typ, temperatur, feuchte, body.wert("battery")?.toInt())
    }

    private class Problem(text: String) : Exception(text)

    /** Eine signierte GET-Anfrage. Wirft [Problem] mit einem Satz für Frank. */
    private fun anfrage(pfad: String, status: Boolean = false): JSONObject {
        val token = s.switchbotToken
        val zeit = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(s.switchbotSecret.toByteArray(), "HmacSHA256")) }
        val sign = Base64.encodeToString(mac.doFinal((token + zeit + nonce).toByteArray()), Base64.NO_WRAP).uppercase(Locale.ROOT)
        val anfrage = Request.Builder().url("https://api.switch-bot.com$pfad")
            .header("Authorization", token).header("sign", sign).header("t", zeit).header("nonce", nonce)
            .header("Content-Type", "application/json; charset=utf8").build()
        val j = try {
            CLIENT.newCall(anfrage).execute().use { a ->
                when {
                    a.code == 401 -> throw Problem("SwitchBot lehnt den Zugang ab: Token oder Secret stimmen nicht, oder das Tageslimit von 10.000 Anfragen ist erreicht.")
                    a.code == 429 -> throw Problem("SwitchBot drosselt gerade die Anfragen; bitte gleich noch einmal.")
                    !a.isSuccessful -> throw Problem("Die SwitchBot-Cloud antwortet mit Fehler ${a.code}.")
                }
                JSONObject(a.body?.string().orEmpty())
            }
        } catch (e: Problem) {
            throw e
        } catch (e: Exception) {
            Logbuch.netz("SwitchBot $pfad", e.message ?: e.javaClass.simpleName)
            throw Problem("Die SwitchBot-Cloud ist gerade nicht erreichbar (kein Netz?).")
        }
        when (val code = j.optInt("statusCode", -1)) {
            100 -> return j
            161 -> throw Problem("nicht erreichbar, der Sensor ist offline.")
            171 -> throw Problem("nicht erreichbar, der SwitchBot-Hub ist offline.")
            190 -> throw Problem(if (status) "nicht erreichbar, der Sensor ist gerade nicht mit der Cloud abgeglichen." else "Die SwitchBot-Cloud meldet einen Systemfehler.")
            else -> throw Problem((if (status) "nicht erreichbar" else "SwitchBot meldet einen Fehler") + " (Code $code${j.optString("message").takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}).")
        }
    }

    companion object {
        /** Geräte mit Temperatur und Luftfeuchtigkeit: Meter, MeterPlus, WoIOSensor (Außensensor), MeterPro, Hub 2/3. */
        private val SENSORTYP = Regex("meter|iosensor|hub ?[23]", RegexOption.IGNORE_CASE)
        /** Franks Sensoren laut ihm; fehlt einer in der Cloud, sagt Jarvis das ausdrücklich. */
        private val ERWARTET = listOf("Balkon", "Wohnzimmer", "Schlafzimmer")
        private val CLIENT = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()

        private fun JSONObject.wert(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null
        private fun zahl(v: Double, stellen: Int = 1): String = String.format(Locale.GERMANY, "%.${stellen}f", v)
    }
}
