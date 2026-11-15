package de.frank.jarvis.faehigkeit

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Base64
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Franks SwitchBot-Sensoren (Balkon, Wohnzimmer, Schlafzimmer, Auto und der CO2-Monitor Meter Pro CO2 in der Wohnung), per Bluetooth am Hub Mini, der sie in die SwitchBot-Cloud meldet.
 * Nur Lesen über die offizielle Cloud-API v1.1 (GET /devices, GET /devices/{id}/status), signiert mit Token und Secret aus
 * Einstellungen → Konten und APIs (HMAC-SHA256 über Token + Zeit + Nonce, Base64, Großbuchstaben).
 * Messwerte bewusst ohne Zwischenspeicher: Ist der Hub oder ein Sensor offline (Status 161/171) oder liefert er keine Werte, meldet Jarvis das
 * statt alter Werte. Nur die Geräteliste (Namen und Kennungen) bleibt bis zu eine Stunde im Speicher; nach einem Sensorproblem wird sie verworfen,
 * und fehlt in ihr ein gesuchter oder erwarteter Name (umbenannt), holt der Abruf sie einmal frisch (höchstens einmal je Minute). Die Cloud nennt keine eigene Messzeit; als Zeit gilt der Abruf.
 */
class SwitchbotFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "switchbot"
    override val name = "Raumklima"
    override val beschreibung = "Temperatur, Luftfeuchtigkeit und CO2 der SwitchBot-Sensoren (Balkon, Wohnzimmer, Schlafzimmer, Auto, CO2-Monitor in der Wohnung)."
    override val hinweise =
        "Fragt Frank nach Temperatur, Luftfeuchtigkeit oder CO2 in seiner Wohnung, auf dem Balkon, im Wohn- oder Schlafzimmer oder im Auto („Wie warm ist es im Schlafzimmer?“, " +
            "„Wie ist der CO2-Wert in der Wohnung?“, „Wie warm ist es im Auto?“), nutze raumklima_lesen. Der Sensor „Auto“ liegt in Franks Auto, nicht in der Wohnung. " +
            "Der CO2-Wert kommt vom Sensor „CO2-Monitor“ in der Wohnung (co2_ppm): ordne ihn ein, unter 800 ppm gut, 800 bis 1000 in Ordnung, 1000 bis 1400 lüften, darüber dringend lüften. " +
            "Fragt Frank nur nach CO2 oder Luftqualität, rufe raumklima_lesen mit sensor CO2-Monitor auf und nenne nur den CO2-Wert mit Einordnung. Fragt er nach einem Raum, nur diesen. " +
            "Sonst nenne je Sensor Name, Temperatur, Luftfeuchtigkeit, falls vorhanden CO2 und die Abrufzeit; der Sensor im Auto ist oft außer Reichweite des Hubs, das ist normal; in Bluetooth-Nähe liest das Handy ihn direkt. Steht bei einem Sensor quelle, kamen die Werte über den Mac zu Hause statt aus der SwitchBot-Cloud; nenne dann die Messzeit. Meldet es einen Sensor oder den Hub als nicht erreichbar, " +
            "sag das genau so und nenne für ihn keine Werte. Steht ein Raum unter nicht_im_konto, gibt es im SwitchBot-Konto keinen Sensor mit diesem Namen: " +
            "sag das und rate keine Werte (ähnlich benannte Sensoren nennen, z. B. Tippfehler). Für das Wetter draußen bleibt wetter_lesen zuständig."

    private val s get() = SecureSettings.get(context)

    override fun stoerung(): String? =
        if (s.switchbotToken.isBlank() || s.switchbotSecret.isBlank()) "SwitchBot-Token oder -Secret fehlen oder sind gerade nicht lesbar (Einstellungen → Konten und APIs)." else null

    override val werkzeuge = listOf(
        Werkzeug(
            name = "raumklima_lesen",
            titel = "Raumklima lesen",
            beschreibung = "Liest jetzt frisch aus der SwitchBot-Cloud Temperatur (°C), Luftfeuchtigkeit (%) und beim CO2-Monitor den CO2-Wert (ppm) von Franks SwitchBot-Sensoren " +
                "(Balkon, Wohnzimmer, Schlafzimmer, Auto, CO2-Monitor in der Wohnung), je mit Name und Abrufzeit. Nicht erreichbare Sensoren oder ein Hub offline werden gemeldet, nie alte Werte. Nur Lesen.",
            schema = schema("sensor" to text("Optional: Name eines einzelnen Sensors, z. B. Balkon, Auto oder CO2-Monitor. Für alle Sensoren weglassen.")),
            nurLesen = true,
            taetigkeit = "liest die Raumsensoren",
        ) { eingabe -> abrufen(eingabe.optString("sensor").trim()) },
    )

    /** Ergebnis eines Sensors: Werte oder ein verständlicher Grund, warum es keine gibt. */
    private class Sensor(val name: String, val typ: String, val temperatur: Double? = null, val feuchte: Double? = null, val batterie: Int? = null, val co2: Int? = null, val problem: String? = null, val offline: Boolean = false, val quelle: String? = null)

    /** Geräteliste (nur Namen und Kennungen, keine Messwerte) mit Abrufzeit. */
    @Volatile private var liste: Pair<Long, List<JSONObject>>? = null

    /** Geräteliste aus dem Speicher (höchstens [LISTE_GILT_MS] alt) oder frisch; [frisch] erzwingt den Abruf. Zweiter Wert: kam aus dem Speicher. */
    private fun sensorListe(frisch: Boolean = false): Pair<List<JSONObject>, Boolean> {
        if (!frisch) liste?.takeIf { System.currentTimeMillis() - it.first < LISTE_GILT_MS }?.let { return it.second to true }
        val geraete = anfrage("/v1.1/devices").optJSONObject("body")?.optJSONArray("deviceList") ?: JSONArray()
        val sensoren = (0 until geraete.length()).mapNotNull { geraete.optJSONObject(it) }.filter { SENSORTYP.containsMatchIn(it.optString("deviceType")) }
        if (sensoren.isNotEmpty()) liste = System.currentTimeMillis() to sensoren
        return sensoren to false
    }

    private fun List<JSONObject>.ohne(namen: List<String>) = namen.filter { n -> none { it.optString("deviceName").contains(n, ignoreCase = true) } }

    /** Frisch lesen (Werkzeug und Startkarte „Raumklima“); [Ergebnis.struktur] trägt je Sensor Werte oder „problem“. */
    suspend fun abrufen(nur: String = ""): Ergebnis = withContext(Dispatchers.IO) {
        stoerung()?.let { return@withContext Ergebnis(it, fehler = true) }
        val alle = try {
            val (gespeichert, ausSpeicher) = sensorListe()
            // Umbenannt? Fehlt in einer gespeicherten Liste ein gesuchter oder erwarteter Name, im selben Abruf einmal frisch holen.
            // Nur wenn die Liste älter als eine Minute ist: Fehlt ein Name dauerhaft, bleibt der Speicher sonst wirkungslos.
            val zuAlt = System.currentTimeMillis() - (liste?.first ?: 0L) > NEU_HOLEN_AB_MS
            val vermisst = if (nur.isNotEmpty()) gespeichert.ohne(listOf(nur)) else gespeichert.ohne(ERWARTET)
            if (ausSpeicher && zuAlt && vermisst.isNotEmpty()) sensorListe(frisch = true).first else gespeichert
        } catch (e: Problem) {
            return@withContext Ergebnis(e.message ?: "SwitchBot antwortet nicht.", fehler = true)
        }
        val genutzt = liste
        if (alle.isEmpty()) return@withContext Ergebnis(
            "In deinem SwitchBot-Konto ist kein Thermo-Hygrometer zu sehen. Prüfe in der SwitchBot-App, ob die Sensoren mit dem Hub verbunden sind und bei jedem der Cloud-Dienst eingeschaltet ist.", fehler = true)
        // „alle“, „Wohnung“ o. Ä. passt auf keinen Namen: dann alle statt eines Fehlers.
        val treffer = if (nur.isEmpty()) emptyList() else alle.filter { it.optString("deviceName").contains(nur, ignoreCase = true) }
        val gewaehlt = treffer.ifEmpty { alle }
        val einzeln = treffer.isNotEmpty()

        // Parallel zur Cloud: Webhook-Anmeldung, Relay-Stand (Webhook-Werte und Mac zu Hause, jarvis_switchbot.py) und das Handy selbst per
        // Bluetooth (wie bei der Victron-Batterie): für das Auto zuerst, denn es steht meist außer Reichweite des Hubs; für alle anderen nur,
        // wenn weder Cloud noch Relay einen brauchbaren Wert haben (Frank, 15.11.2026).
        val nah = ConcurrentHashMap<String, JSONObject>()
        val stopp = kotlinx.coroutines.CompletableDeferred<Unit>()
        val scanEnde = System.currentTimeMillis() + NAH_MS
        var webhook: String? = null
        lateinit var fern: Fern
        val sensoren = coroutineScope {
            val scan = async { runCatching { nahLauschen(nah, stopp) }.onFailure { Logbuch.fehler("SwitchBot: Bluetooth-Scan", null, it.javaClass.simpleName) } }
            val hookArbeit = async { webhookSicherstellen(alle) }
            val fernArbeit = async { runCatching { relayStand() }.onFailure { Logbuch.netz("SwitchBot-Stand vom Relay", it.message ?: it.javaClass.simpleName) }.getOrDefault(Fern()) }
            val ausCloud = gewaehlt.map { g ->
                async { runCatching { lesen(g) }.getOrElse { Sensor(g.optString("deviceName"), g.optString("deviceType"), problem = "gerade nicht lesbar (${it.javaClass.simpleName}).") } }
            }.awaitAll()
            webhook = hookArbeit.await()
            fern = fernArbeit.await().also { it.webhookAktiv = webhookAktiv() }
            // Gebraucht: das Auto und jeder Sensor ohne Cloud-Werte, für den auch der Relay nichts Frisches hat. Warten, bis alle gehört sind,
            // höchstens bis [NAH_MS] nach Scanbeginn und nur, solange der Scan wirklich läuft (kein Recht, Bluetooth aus, Scan gescheitert).
            val gebraucht = gewaehlt.zip(ausCloud).filter { (g, z) ->
                val id = g.optString("deviceId").lowercase(Locale.ROOT)
                istMobil(g) || (z.problem != null && frisch(fern.hook[id]) == null && frisch(fern.mac[id]) == null)
            }.map { (g, _) -> g.optString("deviceId").lowercase(Locale.ROOT) }.toSet()
            if (gebraucht.isNotEmpty()) withTimeoutOrNull((scanEnde - System.currentTimeMillis()).coerceAtLeast(0)) {
                while (!scan.isCompleted && !gebraucht.all { nah.containsKey(it) }) delay(150)
            }
            stopp.complete(Unit)
            scan.await()
            gewaehlt.zip(ausCloud).map { (g, z) ->
                val id = g.optString("deviceId").lowercase(Locale.ROOT)
                val direkt = nah[id]?.takeIf { z.problem != null || istMobil(g) }?.let { ausPaket(z, it, "direkt per Bluetooth") }
                // Die Cloud nennt beim Abruf kein Messalter und liefert bei ausgestecktem Hub alte Werte ohne Fehler (15.11.2026). Ob sie lebt,
                // zeigt der Webhook: Kam in den letzten [CLOUD_LEBT_S] irgendeine Meldung (der CO2-Monitor misst alle fünf Minuten und ändert
                // sich fast immer), gilt die Cloud; sonst geht ein frischer Mac-Wert vor. Thermometer melden per Webhook nur bei Änderung.
                val hook = fern.hook[id]
                val mac = fern.mac[id]
                direkt ?: if (fern.cloudLebt) {
                    (if (z.problem == null) z else null) ?: alsSensor(z, hook, MAC_GILT_S) ?: alsSensor(z, mac, MAC_GILT_S) ?: z
                } else {
                    alsSensor(z, mac, FRISCH_S) ?: alsSensor(z, hook, MAC_GILT_S) ?: (if (z.problem == null) z else null) ?: alsSensor(z, mac, MAC_GILT_S) ?: z
                }
            }
        }
        // Ein Problem kann an einer veralteten Liste liegen (Gerät getauscht, Cloud-Dienst umgeschaltet): beim nächsten Mal frisch holen.
        // Nur die hier benutzte Liste verwerfen, nie eine, die ein paralleler Abruf gerade frisch abgelegt hat.
        // Ein offline gemeldeter Sensor (z. B. das Auto außer Reichweite) ist kein Zeichen einer veralteten Liste.
        if (sensoren.any { it.problem != null && !it.offline }) synchronized(this@SwitchbotFaehigkeit) { if (liste === genutzt) liste = null }
        // Fehlt ein erwarteter Raum, steht er nur für das Sprachmodell in der Struktur, nicht als eigene Zeile auf der Karte (Frank, 13.11.2026).
        val fehlend = if (einzeln) emptyList() else alle.ohne(ERWARTET)
        val uhr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY))
        val zeilen = sensoren.map { z ->
            if (z.problem != null) "- ${z.name}: ${z.problem}"
            else "- ${z.name}: ${zahl(z.temperatur!!)} °C, ${zahl(z.feuchte!!, 0)} % Luftfeuchtigkeit" + (z.co2?.let { ", ${co2Text(it)}" } ?: "") + (z.batterie?.let { ", Batterie $it %" } ?: "") + (z.quelle?.let { " ($it)" } ?: "")
        }
        val vorweg = if (nur.isNotEmpty() && !einzeln) "Einen Sensor „$nur“ gibt es nicht, hier alle.\n" else ""
        val kopf = if (sensoren.any { it.problem == null && it.quelle == null }) "Abgerufen um $uhr Uhr; Werte ohne Herkunftsangabe kommen aus der SwitchBot-Cloud (sie nennt keine eigene Messzeit):"
            else "Abgerufen um $uhr Uhr:"
        val text = vorweg + kopf + "\n" + zeilen.joinToString("\n")
        val struktur = JSONObject().put("abgerufen", uhr).put("sensoren", JSONArray(sensoren.map { z ->
            JSONObject().put("name", z.name).put("typ", z.typ).apply {
                if (z.problem != null) { put("problem", z.problem); if (z.offline) put("offline", true) }
                else { put("temperatur_c", z.temperatur); put("luftfeuchtigkeit_prozent", z.feuchte); z.co2?.let { put("co2_ppm", it) }; z.quelle?.let { put("quelle", it) }; z.batterie?.let { put("batterie_prozent", it) } }
            }
        })).apply { if (fehlend.isNotEmpty()) put("nicht_im_konto", JSONArray(fehlend)); webhook?.let { put("webhook", it) } }
        Ergebnis(text + (if (fehlend.isEmpty()) "" else "\nKein Sensor mit diesem Namen im SwitchBot-Konto: ${fehlend.joinToString()}."), fehler = sensoren.all { it.problem != null }, struktur = struktur)
    }

    private fun lesen(g: JSONObject): Sensor {
        val name = g.optString("deviceName").ifBlank { g.optString("deviceId") }
        val typ = g.optString("deviceType")
        // Der Schalter in der Geräteliste kann veraltet sein: trotzdem den Status fragen, die Anleitung nur, wenn wirklich keine Werte kommen.
        val cloudAus = g.has("enableCloudService") && !g.optBoolean("enableCloudService", true)
        val cloudHinweis = "keine Werte: In der SwitchBot-App beim Sensor Zahnrad → Cloud-Dienst einschalten und als Hub den Hub Mini wählen (Sensor dafür nah an den Hub legen)."
        val antwort = try {
            anfrage("/v1.1/devices/${g.optString("deviceId")}/status", status = true)
        } catch (e: Problem) {
            return Sensor(name, typ, problem = if (cloudAus) cloudHinweis else e.message, offline = e.code == 161 && !cloudAus)
        }
        val body = antwort.optJSONObject("body") ?: JSONObject()
        val temperatur = body.wert("temperature")
        val feuchte = body.wert("humidity")
        if (temperatur == null || feuchte == null) return Sensor(name, typ, problem = if (cloudAus) cloudHinweis else "nicht erreichbar, die Cloud liefert gerade keine Messwerte.")
        // Lauter Nullen (0 °C, 0 % Feuchte) sind Platzhalter der Cloud: Der Sensor meldet sich nicht über den Hub, das sind keine Messwerte.
        if (temperatur == 0.0 && feuchte == 0.0) return Sensor(name, typ, problem = "keine echten Werte, die SwitchBot-Cloud bekommt von diesem Sensor nichts. " +
            "In der SwitchBot-App beim Sensor Zahnrad → Cloud-Dienst einschalten und als Hub den Hub Mini wählen (Sensor dafür nah an den Hub legen).")
        // Meter Pro CO2 meldet zusätzlich „CO2“ in ppm.
        val co2 = body.wert("CO2")?.toInt()
        return Sensor(name, typ, temperatur, feuchte, body.wert("battery")?.toInt(), co2)
    }

    /** Relay-Stand je MAC (klein, ohne Doppelpunkte), getrennt nach Webhook der Cloud und Mac; [cloudLebt]: Webhook in den letzten [CLOUD_LEBT_S]. */
    private class Fern(val hook: Map<String, JSONObject> = emptyMap(), val mac: Map<String, JSONObject> = emptyMap()) {
        /** Ohne eingetragenen Webhook lässt sich nicht erkennen, ob die Cloud schweigt: dann gilt sie (wie vor dem Webhook). */
        var webhookAktiv = false
        val cloudLebt: Boolean get() = !webhookAktiv || hook.values.any { System.currentTimeMillis() / 1000 - it.optLong("zeit") <= CLOUD_LEBT_S }
    }

    /** Stand vom Relay (GET /geraet/switchbot): Werte mit Messzeit „zeit“; „quelle“ = webhook für die Cloud, sonst vom Mac. */
    private fun relayStand(): Fern {
        val e = Einstellungen.get(context)
        if (e.serverToken.isBlank()) return Fern()
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/switchbot").header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung).build()
        val j = CLIENT.newCall(anfrage).execute().use { a ->
            check(a.isSuccessful) { "Relay antwortet mit ${a.code}" }
            JSONObject(a.body?.string().orEmpty())
        }
        val liste = j.optJSONArray("geraete") ?: return Fern()
        val alle = (0 until liste.length()).mapNotNull { liste.optJSONObject(it) }.filter { it.optLong("zeit") > 0 && it.optString("mac").length == 12 }
        val (hook, mac) = alle.partition { it.optString("quelle") == "webhook" }
        fun jeMac(l: List<JSONObject>) = l.groupBy { it.optString("mac").lowercase(Locale.ROOT) }.mapValues { (_, w) -> w.maxBy { it.optLong("zeit") } }
        return Fern(jeMac(hook), jeMac(mac))
    }

    /** Werte vom Mac zu Hause, wenn er den Sensor in den letzten [MAC_GILT_S] gehört hat; sonst null (das Cloud-Problem bleibt stehen). */
    private fun alsSensor(z: Sensor, w: JSONObject?, hoechstens: Long): Sensor? {
        w ?: return null
        val zeit = minOf(w.optLong("zeit"), System.currentTimeMillis() / 1000)
        if (System.currentTimeMillis() / 1000 - zeit > hoechstens) return null
        val uhr = java.time.Instant.ofEpochSecond(zeit).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY))
        val weg = if (w.optString("quelle") == "webhook") "über die SwitchBot-Cloud (Webhook)" else "über den Mac zu Hause"
        return ausPaket(z, w, "$weg, gemessen $uhr Uhr")
    }

    private fun istMobil(g: JSONObject) = MOBIL.any { g.optString("deviceName").equals(it, ignoreCase = true) }

    /** Werte aus Relay oder Bluetooth als Sensor; CO2 nur beim CO2-Monitor (ein 16-Byte-Paket eines anderen Geräts kann Zufallswerte tragen). */
    private fun ausPaket(z: Sensor, w: JSONObject, weg: String): Sensor? {
        val t = w.wert("temperatur_c") ?: return null
        val f = w.wert("luftfeuchtigkeit_prozent") ?: return null
        val co2 = if (z.typ.contains("CO2", ignoreCase = true)) w.wert("co2_ppm")?.toInt() else null
        return Sensor(z.name, z.typ, t, f, null, co2, quelle = weg)
    }

    /** [w], wenn höchstens [FRISCH_S] alt (Messzeit in der Zukunft zählt als jetzt), sonst null. */
    private fun frisch(w: JSONObject?): JSONObject? = w?.takeIf { System.currentTimeMillis() / 1000 - it.optLong("zeit") <= FRISCH_S }

    /** Webhook bei SwitchBot eingetragen? Nur dann sagt das Ausbleiben von Meldungen etwas über die Cloud. */
    private fun webhookAktiv() = webhookStand == "angemeldet" || webhookStand == "neu angemeldet"

    /** Zeitpunkte der letzten Scan-Starts: Android drosselt mehr als fünf startScan je 30 s still (keine Ergebnisse, kein Fehler). */
    private val scanStarts = java.util.ArrayDeque<Long>()

    /**
     * Hört die Werbepakete der SwitchBot-Sensoren (Hersteller 0x0969) und legt je MAC (klein, ohne Doppelpunkte) die Werte in [ziel],
     * bis [stopp] erfüllt ist oder [NAH_MS] um sind. Ohne Bluetooth-Recht („Geräte in der Nähe“) oder bei ausgeschaltetem Bluetooth: nichts.
     */
    @SuppressLint("MissingPermission")
    private suspend fun nahLauschen(ziel: ConcurrentHashMap<String, JSONObject>, stopp: kotlinx.coroutines.CompletableDeferred<Unit>) {
        if (!VictronFaehigkeit.erlaubt(context)) return
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        if (!adapter.isEnabled) return
        val scanner = adapter.bluetoothLeScanner ?: return
        synchronized(scanStarts) {
            val jetzt = System.currentTimeMillis()
            while (scanStarts.isNotEmpty() && jetzt - scanStarts.first() > 30_000) scanStarts.removeFirst()
            // Victron scannt ebenfalls: mit Reserve unter Androids Grenze bleiben.
            if (scanStarts.size >= 3) return
            scanStarts.addLast(jetzt)
        }
        val rueckruf = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // Im Binder-Thread: nichts darf hier werfen.
                runCatching { result.scanRecord?.getManufacturerSpecificData(WOAN)?.let { blePaket(it) }?.let { ziel[it.getString("mac")] = it } }
            }

            override fun onScanFailed(errorCode: Int) { stopp.complete(Unit) }
        }
        scanner.startScan(listOf(ScanFilter.Builder().setManufacturerData(WOAN, byteArrayOf()).build()),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), rueckruf)
        try { withTimeoutOrNull(NAH_MS + 500) { stopp.await() } } finally { runCatching { scanner.stopScan(rueckruf) } }
    }

    @Volatile private var webhookStand: String? = null
    @Volatile private var webhookFehlerBis = 0L
    private val webhookSperre = Any()

    /**
     * Webhook der SwitchBot-Cloud auf den Relay (POST /switchbot/hook/<geheim>): Die Cloud schickt dann jede Wertänderung selbst,
     * auch wenn GET /status nur Nullen liefert (SwitchBotAPI #436). Nur wenn ein Meter Pro CO2 im Konto ist; einmal je App-Lauf geprüft,
     * nach einem Fehler erst nach [WEBHOOK_PAUSE_MS] erneut. Eine fremde, schon eingetragene Adresse wird nie überschrieben; lässt sich
     * nicht abfragen, was eingetragen ist, wird auch nichts eingetragen. Nur ein Abruf zur Zeit (sonst zweimal setupWebhook).
     */
    private fun webhookSicherstellen(alle: List<JSONObject>): String? {
        if (alle.none { it.optString("deviceType").contains("CO2", ignoreCase = true) }) return null
        webhookStand?.let { return it }
        if (System.currentTimeMillis() < webhookFehlerBis) return "Fehler beim letzten Versuch, neuer Versuch später"
        synchronized(webhookSperre) {
            webhookStand?.let { return it }
            val stand = runCatching {
                val e = Einstellungen.get(context)
                if (e.serverToken.isBlank()) return null
                val ziel = CLIENT.newCall(Request.Builder().url("https://${e.serverHost}/geraet/switchbot-hook").header("X-Jarvis-Token", e.serverToken)
                    .header(Geraet.KOPFZEILE, Geraet.kennung).build()).execute().use { a ->
                    check(a.isSuccessful) { "Relay antwortet mit ${a.code}" }
                    JSONObject(a.body?.string().orEmpty()).getString("url")
                }
                val antwort = anfrage("/v1.1/webhook/queryWebhook", rumpf = JSONObject().put("action", "queryUrl"))
                val vorhanden = antwort.optJSONObject("body")?.optJSONArray("urls")?.let { u -> (0 until u.length()).map { u.optString(it) } }.orEmpty()
                when {
                    ziel in vorhanden -> "angemeldet"
                    vorhanden.isNotEmpty() -> "fremde Adresse eingetragen, nicht überschrieben"
                    else -> {
                        anfrage("/v1.1/webhook/setupWebhook", rumpf = JSONObject().put("action", "setupWebhook").put("url", ziel).put("deviceList", "ALL"))
                        Logbuch.netz("SwitchBot-Webhook", "angemeldet")
                        "neu angemeldet"
                    }
                }
            }.getOrElse { "Fehler: ${it.message ?: it.javaClass.simpleName}".also { m -> Logbuch.netz("SwitchBot-Webhook", m) } }
            if (stand.startsWith("Fehler")) webhookFehlerBis = System.currentTimeMillis() + WEBHOOK_PAUSE_MS else webhookStand = stand
            return stand
        }
    }

    private class Problem(text: String, val code: Int = 0) : Exception(text)

    /**
     * Eine signierte GET-Anfrage. Wirft ausschließlich [Problem] mit einem Satz für Frank: bei der Geräteliste ([status] = false) als ganzer Satz,
     * beim Status eines Sensors als Satzteil nach „Name:“.
     */
    private fun anfrage(pfad: String, status: Boolean = false, rumpf: JSONObject? = null): JSONObject {
        fun problem(sensor: String, liste: String, log: String, code: Int = 0): Problem {
            Logbuch.netz("SwitchBot $pfad", log)
            return Problem(if (status) sensor else liste, code)
        }
        val anfrage = try {
            val token = s.switchbotToken
            val zeit = System.currentTimeMillis().toString()
            val nonce = UUID.randomUUID().toString()
            val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(s.switchbotSecret.toByteArray(), "HmacSHA256")) }
            val sign = Base64.encodeToString(mac.doFinal((token + zeit + nonce).toByteArray()), Base64.NO_WRAP).uppercase(Locale.ROOT)
            Request.Builder().url("https://api.switch-bot.com$pfad")
                .header("Authorization", token).header("sign", sign).header("t", zeit).header("nonce", nonce)
                .header("Content-Type", "application/json; charset=utf8")
                .apply { if (rumpf != null) post(rumpf.toString().toRequestBody("application/json; charset=utf8".toMediaType())) }.build()
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
            161 -> problem("nicht erreichbar, der Sensor ist offline.", "SwitchBot meldet ein Gerät als offline.", "Status 161", 161)
            171 -> problem("nicht erreichbar, der SwitchBot-Hub ist offline.", "Der SwitchBot-Hub ist offline, es gibt keine aktuellen Werte.", "Status 171")
            190 -> problem("nicht erreichbar, der Sensor ist gerade nicht mit der Cloud abgeglichen.", "Die SwitchBot-Cloud meldet einen Systemfehler.", "Status 190")
            else -> problem("nicht erreichbar (Code $code$nachricht).", "SwitchBot meldet einen Fehler (Code $code$nachricht).", "Status $code")
        }
    }

    companion object {
        /** Thermo-Hygrometer: Meter, MeterPlus, MeterPro, MeterPro(CO2), WoIOSensor (Außensensor). Hubs zählen nicht als Sensor. */
        private val SENSORTYP = Regex("meter|iosensor", RegexOption.IGNORE_CASE)
        /** Franks Sensoren laut ihm (Aufträge 13.11. und 15.11.2026); fehlt einer in der gespeicherten Liste, holt der Abruf sie frisch. */
        private val ERWARTET = listOf("Balkon", "Wohnzimmer", "Schlafzimmer", "Auto", "CO2-Monitor")
        private const val LISTE_GILT_MS = 60 * 60 * 1000L
        private const val NEU_HOLEN_AB_MS = 60 * 1000L
        /** Der Mac meldet alle fünf Minuten, der Webhook bei jeder Änderung; älter als 40 Minuten gilt nicht mehr als aktuell
         *  (mit Batterie misst der CO2-Monitor nur alle 30 Minuten). */
        private const val MAC_GILT_S = 40 * 60L
        /** So frisch gemessen geht ein Wert vom Mac oder Webhook der Cloud vor (Mac meldet alle fünf Minuten). */
        private const val FRISCH_S = 10 * 60L
        /** Die Cloud gilt als lebendig, solange in dieser Zeit irgendeine Webhook-Meldung kam (CO2-Monitor misst alle fünf, mit Batterie
         *  höchstens alle 30 Minuten; Thermometer melden nur bei Änderung). */
        private const val CLOUD_LEBT_S = 45 * 60L
        /** Nach einem Fehler bei der Webhook-Anmeldung so lange nicht erneut (jeder Versuch kostet bis zu drei Netzanfragen). */
        private const val WEBHOOK_PAUSE_MS = 10 * 60 * 1000L
        private val CLIENT = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build()

        private const val WOAN = 0x0969
        /** Sensoren, die nicht beim Hub stehen: immer zuerst direkt per Bluetooth (Name wie in der SwitchBot-App). */
        private val MOBIL = listOf("Auto")
        /** So lange lauscht das Handy höchstens; Thermometer senden alle paar Sekunden, der CO2-Monitor seltener. */
        private const val NAH_MS = 5_000L

        /**
         * Werbepaket (Nutzdaten nach der Company-ID 0x0969, pySwitchbot adv_parsers/meter.py): 0–5 MAC, 8 Bits 3–0 Zehntel °C,
         * 9 Bit 7 Vorzeichen (1 = positiv) und Bits 6–0 ganze °C, 10 Bits 6–0 Feuchte %; beim Meter Pro CO2 (16 Byte) 13–14 CO2 ppm (Big Endian).
         * Geprüft am 15.11.2026 mit Franks Geräten (Schlafzimmer 20,3 °C / 48 %, CO2-Monitor 22,1 °C / 43 % / 662 ppm).
         */
        fun blePaket(roh: ByteArray): JSONObject? {
            if (roh.size < 11) return null
            val b = { i: Int -> roh[i].toInt() and 0xFF }
            val temp = ((b(9) and 0x7F) + (b(8) and 0x0F) / 10.0) * (if (b(9) and 0x80 != 0) 1 else -1)
            val feuchte = b(10) and 0x7F
            if (feuchte == 0 || feuchte > 100 || temp < -40 || temp > 70) return null
            val mac = (0..5).joinToString("") { String.format(Locale.ROOT, "%02x", b(it)) }
            return JSONObject().put("mac", mac).put("temperatur_c", Math.round(temp * 10) / 10.0).put("luftfeuchtigkeit_prozent", feuchte).apply {
                // CO2 nur bei Paketen ab 15 Byte (pySwitchbot); ob das Gerät ein CO2-Monitor ist, prüft ausPaket über den Cloud-Typ.
                if (roh.size >= 15) ((b(13) shl 8) or b(14)).takeIf { it in 400..9999 }?.let { put("co2_ppm", it) }
            }
        }

        private fun JSONObject.wert(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null
        /** Einheitliche CO2-Angabe für Sprachtext und Startkarte. */
        fun co2Text(ppm: Int) = "CO2 $ppm ppm"
        private fun zahl(v: Double, stellen: Int = 1): String = String.format(Locale.GERMANY, "%.${stellen}f", v)
    }
}
