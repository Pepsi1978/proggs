package de.frank.wecker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId

/**
 * Wo der Sonnenwecker rechnet: am zuletzt bekannten ungefähren Standort des Handys (grobe Ortung, kein GPS-Dauerbetrieb).
 * Die Zeitzone kommt immer automatisch vom Handy (ZoneId.systemDefault()). Ohne Standort wird aus der Zeitzone geschätzt,
 * in Mitteleuropa auf Neuenhagen bei Berlin.
 */
object SonnenOrt {
    private const val TAG = "WeckerSonnenOrt"
    private const val KEY_BREITE = "sun_location_lat"
    private const val KEY_LAENGE = "sun_location_lon"
    private const val KEY_ZEIT = "sun_location_time"
    const val NEUENHAGEN_BREITE = 52.529
    const val NEUENHAGEN_LAENGE = 13.689

    data class Ort(val breite: Double, val laenge: Double, val quelle: String)

    /** Gesetzt von [aktualisiere]; ohne Kontext (reine Rechnung, Tests) gilt die Schätzung aus der Zeitzone. */
    @Volatile private var gemerkt: Ort? = null

    /**
     * Der gemerkte Standort, solange er zur aktuellen Zeitzone passt. Nach einer Reise (Zeitzone springt, die neue Ortung
     * fehlt noch) liegt er mehr als etwa 40° neben dem Längengrad der Zeitzone; dann gilt bis zur neuen Ortung die Schätzung.
     */
    fun aktuell(zone: ZoneId = ZoneId.systemDefault()): Ort {
        val g = gemerkt ?: return ausZeitzone(zone)
        val zonenLaenge = zone.rules.getStandardOffset(Instant.now()).totalSeconds / 240.0
        return if (kotlin.math.abs(g.laenge - zonenLaenge) > 40) ausZeitzone(zone) else g
    }

    fun erlaubt(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Gespeicherten Ort laden und, falls erlaubt, mit der letzten bekannten Ortung des Handys auffrischen. Wirft nie. */
    fun aktualisiere(context: Context): Ort {
        val prefs = AlarmStore.get(context).prefs
        try {
            if (prefs.contains(KEY_BREITE)) gemerkt = Ort(prefs.getFloat(KEY_BREITE, 0f).toDouble(), prefs.getFloat(KEY_LAENGE, 0f).toDouble(), "Standort")
            if (erlaubt(context)) {
                val manager = context.getSystemService(LocationManager::class.java)
                val neueste = manager.allProviders.mapNotNull { p -> runCatching { manager.getLastKnownLocation(p) }.getOrNull() }.maxByOrNull { it.time }
                if (neueste != null) merke(context, neueste)
                // Frische Ortung im Hintergrund anstoßen; das Ergebnis gilt ab der nächsten Planung.
                if (Build.VERSION.SDK_INT >= 30 && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) runCatching {
                    manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, ContextCompat.getMainExecutor(context)) { loc ->
                        if (loc != null) {
                            val vorher = gemerkt
                            merke(context, loc)
                            // Deutlich anderer Ort (mehr als etwa 0,5°): Weckzeiten neu berechnen.
                            if (vorher == null || kotlin.math.abs(vorher.breite - loc.latitude) > .5 || kotlin.math.abs(vorher.laenge - loc.longitude) > .5)
                                neuPlanen(context)
                        }
                    }
                }
            }
        } catch (e: Exception) { Log.w(TAG, "Standort nicht lesbar", e) }
        return aktuell()
    }

    /** Nur die Sonnenwecker neu berechnen und planen, im Hintergrund. */
    fun neuPlanen(context: Context) {
        val app = context.applicationContext
        Thread {
            runCatching {
                val ids = AlarmStore.get(app).all().filter { it.sunMode.isNotBlank() }.map { it.id }.toSet()
                if (ids.isNotEmpty()) AlarmScheduler(app).restore(clockChanged = true, onlyIds = ids)
            }.onFailure { Log.w(TAG, "Sonnenwecker nicht neu geplant", it) }
        }.start()
    }

    private fun merke(context: Context, loc: Location) {
        gemerkt = Ort(loc.latitude, loc.longitude, "Standort")
        runCatching { AlarmStore.get(context).prefs.edit().putFloat(KEY_BREITE, loc.latitude.toFloat())
            .putFloat(KEY_LAENGE, loc.longitude.toFloat()).putLong(KEY_ZEIT, loc.time).apply() }
    }

    /** Schätzung ohne Ortung: Mitteleuropa = Neuenhagen, sonst Längengrad aus dem Zeitzonenversatz, mittlere Breite. */
    fun ausZeitzone(zone: ZoneId): Ort {
        if (zone.id in setOf("Europe/Berlin", "Europe/Vienna", "Europe/Zurich", "Europe/Prague", "Europe/Copenhagen", "Europe/Warsaw"))
            return Ort(NEUENHAGEN_BREITE, NEUENHAGEN_LAENGE, "Neuenhagen (geschätzt)")
        val versatzStunden = zone.rules.getStandardOffset(Instant.now()).totalSeconds / 3600.0
        return Ort(45.0, versatzStunden * 15.0, "aus Zeitzone ${zone.id} geschätzt")
    }

    fun beschreibung(context: Context): String {
        val ort = aktuell()
        val zone = ZoneId.systemDefault().id
        return if (ort.quelle == "Standort") "Berechnet für deinen Standort (%.2f°, %.2f°), Zeitzone %s automatisch vom Handy.".format(ort.breite, ort.laenge, zone)
        else "Berechnet ${ort.quelle}, Zeitzone $zone automatisch vom Handy." + if (!erlaubt(context)) " Für genaue Zeiten den ungefähren Standort erlauben." else ""
    }
}
