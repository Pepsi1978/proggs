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
import org.json.JSONException
import org.json.JSONObject

/**
 * Franks SwitchBot-Thermo-Hygrometer (Balkon, Wohnzimmer, Schlafzimmer), per Bluetooth am Hub Mini, der sie in die SwitchBot-Cloud meldet.
 * Nur Lesen über die offizielle Cloud-API v1.1 (GET /devices, GET /devices/{id}/status), signiert mit Token und Secret aus
 * Einstellungen → Konten und APIs (HMAC-SHA256 über Token + Zeit + Nonce, Base64, Großbuchstaben).
 * Messwerte bewusst ohne Zwischenspeicher: Ist der Hub oder ein Sensor offline (Status 161/171) oder liefert er keine Werte, meldet Jarvis das
 * statt alter Werte. Nur die Geräteliste (Namen und Kennungen) bleibt eine Stunde im Speicher. Die Cloud nennt keine eigene Messzeit; als Zeit gilt der Abruf.
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
        if (s.switchbotToken.isBlank() || s.switchbotSecret.isBlank()) "SwitchBot-Token oder -Secret fehlen oder sind gerade nicht lesbar (Einstellungen → Konten und APIs)." else null

    override val werkzeuge = listOf(
        Werkzeug(
            name = "raumklima_lesen",
            titel = "Raumklima lesen",
            beschreibung = "Liest jetzt frisch aus der SwitchBot-Cloud Temperatur (°C) und Luftfeuchtigkeit (%) von Franks SwitchBot-Sensoren " +
                "(Balkon, Wohnzimmer, Schlafzimmer), je mit Name und Abrufzeit. Nicht erreichbare Sensoren oder ein Hub offline werden gemeldet, nie alte Werte. Nur Lesen.",
            schema = schema("sensor" to text("Optional: Name eines einzelnen Sensors, z. B. Balkon. Für alle Sensoren weglassen.")),
            nurLesen = true,
            taetigkeit = "liest die Raumsensoren",
        ) { eingabe -> abrufen(eingabe.optString("sensor").trim()) },
    )

    /** Ergebnis eines Sensors: Werte oder ein verständlicher Grund, warum es keine gibt. */
    private class Sensor(val name: String, val typ: String, val temperatur: Double? = null, val feuchte: Double? = null, val batterie: Int? = null, val problem: String? = null)

    /** Geräteliste (nur Namen und Kennungen, keine Messwerte) mit Abrufzeit. */
    @Volatile private var liste: Pair<Long, List<JSONObject>>? = null

    private fun sensorListe(): List<JSONObject> {
        liste?.takeIf { System.currentTimeMillis() - it.first < LISTE_GILT_MS }?.let { return it.second }
        val geraete = anfrage("/v1.1/devices").optJSONObject("body")?.optJSONArray("deviceList") ?: JSONArray()
        val sensoren = (0 until geraete.length()).mapNotNull { geraete.optJSONObject(it) }.filter { SENSORTYP.containsMatchIn(it.optString("deviceType")) }
        if (sensoren.isNotEmpty()) liste = System.currentTimeMillis() to sensoren
        return sensoren
    }

    /** Frisch lesen (Werkzeug und Startkarte „Raumklima“); [Ergebnis.struktur] trägt je Sensor Werte oder „problem“. */
    suspend fun abrufen(nur: String = ""): Ergebnis = withContext(Dispatchers.IO) {
        stoerung()?.let { return@withContext Ergebnis(it, fehler = true) }
        val alle = try {
            sensorListe()
        } catch (e: Problem) {
            return@withContext Ergebnis(e.message ?: "SwitchBot antwortet nicht.", fehler = true)
        }
        if (alle.isEmpty()) return@withContext Ergebnis(
            "In deinem SwitchBot-Konto ist kein Thermo-Hygrometer zu sehen. Prüfe in der SwitchBot-App, ob die Sensoren mit dem Hub verbunden sind und bei jedem der Cloud-Dienst eingeschaltet ist.", fehler = true)
        // „alle“, „Wohnung“ o. Ä. passt auf keinen Namen: dann alle statt eines Fehlers.
        val treffer = if (nur.isEmpty()) emptyList() else alle.filter { it.optString("deviceName").contains(nur, ignoreCase = true) }
        val gewaehlt = treffer.ifEmpty { alle }
        val einzeln = treffer.isNotEmpty()

        val sensoren = coroutineScope {
            gewaehlt.map { g ->
                async { runCatching { lesen(g) }.getOrElse { Sensor(g.optString("deviceName"), g.optString("deviceType"), problem = "gerade nicht lesbar (${it.javaClass.simpleName}).") } }
            }.awaitAll()
        }
        // Ein Problem kann an einer veralteten Liste liegen (umbenannt, Cloud-Dienst umgeschaltet): beim nächsten Mal frisch holen.
        if (sensoren.any { it.problem != null }) liste = null
        val uhr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY))
        val fehlend = if (einzeln) emptyList() else ERWARTET.filter { name -> sensoren.none { it.name.contains(name, ignoreCase = true) } }
        val zeilen = sensoren.map { z ->
            if (z.problem != null) "- ${z.name}: ${z.problem}"
            else "- ${z.name}: ${zahl(z.temperatur!!)} °C, ${zahl(z.feuchte!!, 0)} % Luftfeuchtigkeit" + (z.batterie?.let { ", Batterie $it %" } ?: "")
        } + fehlend.map { "- $it: nicht in deinem SwitchBot-Konto gefunden (Name in der SwitchBot-App prüfen)." }
        val vorweg = if (nur.isNotEmpty() && !einzeln) "Einen Sensor „$nur“ gibt es nicht, hier alle.\n" else ""
        val text = vorweg + "Abgerufen um $uhr Uhr aus der SwitchBot-Cloud (sie nennt keine eigene Messzeit):\n" + zeilen.joinToString("\n")
        val struktur = JSONObject().put("abgerufen", uhr).put("sensoren", JSONArray(sensoren.map { z ->
            JSONObject().put("name", z.name).put("typ", z.typ).apply {
                if (z.problem != null) put("problem", z.problem)
                else { put("temperatur_c", z.temperatur); put("luftfeuchtigkeit_prozent", z.feuchte); z.batterie?.let { put("batterie_prozent", it) } }
            }
        } + fehlend.map { JSONObject().put("name", it).put("problem", "nicht im SwitchBot-Konto gefunden") }))
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

    /**
     * Eine signierte GET-Anfrage. Wirft ausschließlich [Problem] mit einem Satz für Frank: bei der Geräteliste ([status] = false) als ganzer Satz,
     * beim Status eines Sensors als Satzteil nach „Name:“.
     */
    private fun anfrage(pfad: String, status: Boolean = false): JSONObject {
        fun problem(sensor: String, liste: String, log: String): Problem {
            Logbuch.netz("SwitchBot $pfad", log)
            return Problem(if (status) sensor else liste)
        }
        val anfrage = try {
            val token = s.switchbotToken
            val zeit = System.currentTimeMillis().toString()
            val nonce = UUID.randomUUID().toString()
            val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(s.switchbotSecret.toByteArray(), "HmacSHA256")) }
            val sign = Base64.encodeToString(mac.doFinal((token + zeit + nonce).toByteArray()), Base64.NO_WRAP).uppercase(Locale.ROOT)
            Request.Builder().url("https://api.switch-bot.com$pfad")
                .header("Authorization", token).header("sign", sign).header("t", zeit).header("nonce", nonce)
                .header("Content-Type", "application/json; charset=utf8").build()
        } catch (e: Exception) {
            throw problem("keine Abfrage möglich: Token oder Secret ist leer oder enthält ungültige Zeichen.",
                "SwitchBot-Token oder -Secret ist leer oder enthält ungültige Zeichen; bitte unter Konten und APIs neu eintragen.", e.javaClass.simpleName)
        }
        val j = try {
            CLIENT.newCall(anfrage).execute().use { a ->
                when {
                    a.code == 401 -> throw problem("keine Werte, SwitchBot lehnt den Zugang ab.",
                        "SwitchBot lehnt den Zugang ab: Token oder Secret stimmen nicht, oder das Tageslimit von 10.000 Anfragen ist erreicht.", "HTTP 401")
                    a.code == 429 -> throw problem("keine Werte, SwitchBot drosselt gerade.", "SwitchBot drosselt gerade die Anfragen; bitte gleich noch einmal.", "HTTP 429")
                    !a.isSuccessful -> throw problem("nicht erreichbar, die Cloud antwortet mit Fehler ${a.code}.", "Die SwitchBot-Cloud antwortet mit Fehler ${a.code}.", "HTTP ${a.code}")
                }
                try {
                    JSONObject(a.body?.string().orEmpty())
                } catch (e: JSONException) {
                    throw problem("nicht lesbar, die Cloud schickt keine gültige Antwort.", "Die SwitchBot-Cloud schickt gerade keine gültige Antwort (Wartung oder WLAN-Anmeldeseite?).", "keine JSON-Antwort")
                }
            }
        } catch (e: Problem) {
            throw e
        } catch (e: Exception) {
            throw problem("nicht erreichbar, die SwitchBot-Cloud antwortet nicht.", "Die SwitchBot-Cloud ist gerade nicht erreichbar (kein Netz?).", e.message ?: e.javaClass.simpleName)
        }
        val code = j.optInt("statusCode", -1)
        if (code == 100) return j
        val nachricht = j.optString("message").takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
        throw when (code) {
            161 -> problem("nicht erreichbar, der Sensor ist offline.", "SwitchBot meldet ein Gerät als offline.", "Status 161")
            171 -> problem("nicht erreichbar, der SwitchBot-Hub ist offline.", "Der SwitchBot-Hub ist offline, es gibt keine aktuellen Werte.", "Status 171")
            190 -> problem("nicht erreichbar, der Sensor ist gerade nicht mit der Cloud abgeglichen.", "Die SwitchBot-Cloud meldet einen Systemfehler.", "Status 190")
            else -> problem("nicht erreichbar (Code $code$nachricht).", "SwitchBot meldet einen Fehler (Code $code$nachricht).", "Status $code")
        }
    }

    companion object {
        /** Thermo-Hygrometer: Meter, MeterPlus, MeterPro, WoIOSensor (Außensensor). Hubs zählen nicht als Sensor. */
        private val SENSORTYP = Regex("meter|iosensor", RegexOption.IGNORE_CASE)
        /** Franks Sensoren laut ihm (Auftrag 13.11.2026); fehlt einer in der Cloud, sagt Jarvis das ausdrücklich. */
        private val ERWARTET = listOf("Balkon", "Wohnzimmer", "Schlafzimmer")
        private const val LISTE_GILT_MS = 60 * 60 * 1000L
        private val CLIENT = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build()

        private fun JSONObject.wert(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null
        private fun zahl(v: Double, stellen: Int = 1): String = String.format(Locale.GERMANY, "%.${stellen}f", v)
    }
}
