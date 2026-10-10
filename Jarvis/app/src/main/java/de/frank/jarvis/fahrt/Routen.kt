package de.frank.jarvis.fahrt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import de.frank.jarvis.data.Einstellungen
import java.time.Duration
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Start oder Ziel einer Fahrt: eine Adresse, die Google selbst auflöst, oder ein Punkt (der Standort des Handys). */
sealed class Ort {
    class Adresse(val text: String) : Ort()
    class Punkt(val breite: Double, val laenge: Double) : Ort()
}

/** Eine berechnete Autofahrt. [sekunden] mit Verkehr, [ohneVerkehr] die Fahrzeit bei freier Strecke. */
class Route(val sekunden: Int, val ohneVerkehr: Int, val meter: Int, val ueber: String) {
    /** Aufgerundet, damit Jarvis nie zu knapp rechnet. */
    val minuten: Int get() = (sekunden + 59) / 60
    val minutenOhneVerkehr: Int get() = (ohneVerkehr + 59) / 60
    val km: Int get() = (meter + 500) / 1000
}

/** Ein Grund in einem Satz, den Jarvis Frank so sagen kann. */
class RoutenFehler(grund: String) : Exception(grund)

/**
 * Fahrzeiten mit Verkehr von der Google Routes API (computeRoutes, Auto, TRAFFIC_AWARE). Diese Stufe hat bei Google
 * 5000 kostenlose Abfragen im Monat; Jarvis zählt mit und hört vorher auf, damit nie Kosten entstehen.
 */
object Routen {
    const val FREI_IM_MONAT = 5000
    /** Ab hier fragt Jarvis im laufenden Monat nicht mehr. */
    private const val GRENZE = 4800
    private val CLIENT = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /** Abfragen im laufenden Monat. */
    fun verbraucht(context: Context): Int = Einstellungen.get(context).let { if (it.routenMonat == YearMonth.now().toString()) it.routenZaehler else 0 }

    @Synchronized
    private fun zaehle(context: Context) {
        val e = Einstellungen.get(context)
        val monat = YearMonth.now().toString()
        if (e.routenMonat != monat) { e.routenMonat = monat; e.routenZaehler = 0 }
        if (e.routenZaehler >= GRENZE) throw RoutenFehler("Das kostenlose Monatskontingent von Google Maps ist fast aufgebraucht (${e.routenZaehler} von $FREI_IM_MONAT Abfragen). Jarvis fragt erst im nächsten Monat wieder.")
        e.routenZaehler += 1
    }

    private fun ort(o: Ort): JSONObject = when (o) {
        is Ort.Adresse -> JSONObject().put("address", o.text)
        is Ort.Punkt -> JSONObject().put("location", JSONObject().put("latLng", JSONObject().put("latitude", o.breite).put("longitude", o.laenge)))
    }

    /**
     * Fahrzeit mit dem Auto. Mit [abfahrt] in der Zukunft rechnet Google mit dem dann erwarteten Verkehr,
     * ohne mit dem Verkehr von jetzt.
     */
    suspend fun berechne(context: Context, von: Ort, nach: Ort, abfahrt: ZonedDateTime? = null): Route = withContext(Dispatchers.IO) {
        val schluessel = Einstellungen.get(context).mapsSchluessel.ifBlank { throw RoutenFehler("Der Google-Maps-Schlüssel fehlt. In Jarvis unter Einstellungen → Pünktlich losfahren eintragen.") }
        zaehle(context)
        val rumpf = JSONObject().put("origin", ort(von)).put("destination", ort(nach)).put("travelMode", "DRIVE")
            .put("routingPreference", "TRAFFIC_AWARE").put("languageCode", "de").put("units", "METRIC")
        abfahrt?.takeIf { it.isAfter(ZonedDateTime.now().plusMinutes(2)) }?.let { rumpf.put("departureTime", DateTimeFormatter.ISO_INSTANT.format(it.toInstant().truncatedTo(ChronoUnit.SECONDS))) }
        val anfrage = Request.Builder().url("https://routes.googleapis.com/directions/v2:computeRoutes")
            .header("X-Goog-Api-Key", schluessel)
            .header("X-Goog-FieldMask", "routes.duration,routes.staticDuration,routes.distanceMeters,routes.description")
            .post(rumpf.toString().toRequestBody(JSON)).build()
        val antwort = try {
            CLIENT.newCall(anfrage).execute().use { a -> a.code to JSONObject(a.body?.string().orEmpty().ifBlank { "{}" }) }
        } catch (f: Exception) {
            throw RoutenFehler("Google Maps ist nicht erreichbar (kein Internet?).")
        }
        val (code, json) = antwort
        json.optJSONObject("error")?.let { fehler ->
            val text = fehler.optString("message")
            throw RoutenFehler(
                when {
                    "has not been used" in text || "is disabled" in text -> "Die Routes API ist im Google-Projekt noch nicht aktiviert."
                    "API key not valid" in text || code == 401 -> "Der Google-Maps-Schlüssel ist ungültig."
                    "blocked" in text || "not authorized" in text -> "Der Google-Maps-Schlüssel ist für die Routes API nicht freigegeben."
                    code == 429 -> "Google Maps meldet: Kontingent erreicht."
                    else -> "Google Maps meldet einen Fehler ($code): ${text.take(160)}"
                },
            )
        }
        val r = json.optJSONArray("routes")?.optJSONObject(0) ?: throw RoutenFehler("Google Maps findet keine Route. Bitte die Adresse prüfen.")
        fun sek(feld: String) = r.optString(feld).removeSuffix("s").toDoubleOrNull()?.toInt() ?: 0
        Route(sek("duration"), sek("staticDuration").takeIf { it > 0 } ?: sek("duration"), r.optInt("distanceMeters"), r.optString("description"))
    }

    /**
     * Wann muss Frank losfahren, um zu [ankunft] da zu sein? Die Fahrzeit hängt von der Abfahrtszeit ab; deshalb rechnet
     * Jarvis mit einer Schätzung und einmal nach, wenn sie deutlich danebenlag. Liegt die Abfahrt schon in der
     * Vergangenheit, gilt der Verkehr von jetzt.
     */
    suspend fun fuerAnkunft(context: Context, von: Ort, nach: Ort, ankunft: ZonedDateTime, schaetzungMinuten: Int = 55): Pair<Route, ZonedDateTime> {
        val erste = ankunft.minusMinutes(schaetzungMinuten.toLong())
        var route = berechne(context, von, nach, erste)
        var abfahrt = ankunft.minusSeconds(route.sekunden.toLong())
        if (abs(Duration.between(erste, abfahrt).toMinutes()) > 6 && abfahrt.isAfter(ZonedDateTime.now().plusMinutes(2))) {
            route = berechne(context, von, nach, abfahrt)
            abfahrt = ankunft.minusSeconds(route.sekunden.toLong())
        }
        return route to abfahrt.truncatedTo(ChronoUnit.MINUTES)
    }
}

/** Der Standort des Handys über den Ortungsdienst von Android (ohne Google-Bibliothek). */
object Standort {
    fun erlaubt(context: Context): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /** Auch bei gesperrtem Handy („Immer zulassen“); nur dann kennt Jarvis den Standort für Meldungen und für ChatGPT. */
    fun immerErlaubt(context: Context): Boolean = erlaubt(context) &&
        (Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)

    /** Der Standort von jetzt, höchstens [wartenMs] gewartet; ersatzweise der letzte bekannte, wenn er jünger als 30 Minuten ist. */
    @SuppressLint("MissingPermission")
    suspend fun aktuell(context: Context, wartenMs: Long = 8000): Location? {
        if (!erlaubt(context)) return null
        val dienst = context.getSystemService(LocationManager::class.java) ?: return null
        val anbieter = runCatching { dienst.getProviders(true) }.getOrDefault(emptyList())
        fun letzter(): Location? = anbieter.mapNotNull { runCatching { dienst.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        fun alter(l: Location) = System.currentTimeMillis() - l.time
        letzter()?.takeIf { alter(it) < 2 * 60_000L }?.let { return it }
        val quelle = when {
            Build.VERSION.SDK_INT >= 31 && LocationManager.FUSED_PROVIDER in anbieter -> LocationManager.FUSED_PROVIDER
            LocationManager.NETWORK_PROVIDER in anbieter -> LocationManager.NETWORK_PROVIDER
            LocationManager.GPS_PROVIDER in anbieter -> LocationManager.GPS_PROVIDER
            else -> null
        }
        val frisch = if (Build.VERSION.SDK_INT >= 30 && quelle != null) withTimeoutOrNull(wartenMs) {
            suspendCancellableCoroutine<Location?> { weiter ->
                val abbruch = CancellationSignal()
                weiter.invokeOnCancellation { abbruch.cancel() }
                runCatching { dienst.getCurrentLocation(quelle, abbruch, context.mainExecutor) { ort -> if (weiter.isActive) weiter.resume(ort) } }
                    .onFailure { if (weiter.isActive) weiter.resume(null) }
            }
        } else null
        return frisch ?: letzter()?.takeIf { alter(it) < 30 * 60_000L }
    }
}
