package de.frank.jarvis.kaffee

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Umkreis um Zuhause über den Geofence-Dienst von Android: Das System meldet Betreten und Verlassen, Jarvis muss den Standort nicht selbst
 * alle paar Minuten abfragen (spart Akku). Nur am Handy und nur, wenn „wach halten“ und „Aus, wenn ich das Haus verlasse“ an sind.
 * Klappt die Einrichtung nicht (keine Google-Dienste, keine Standortfreigabe), fragt [Kaffeemaschine] den Standort ersatzweise selbst ab.
 */
object KaffeeUmkreis {
    private const val ID = "kaffee_zuhause"
    private const val PREFS = "kaffee_wach"

    /** Läuft der System-Umkreis mit genau diesem Zuhause und Radius? */
    fun aktiv(context: Context): Boolean = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("umkreis", "").orEmpty().isNotEmpty()

    private fun absicht(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 4711, Intent(context, KaffeeUmkreisEmpfaenger::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    /**
     * Aus der Minutenrunde: richtet den Umkreis ein oder neu, wenn sich Zuhause, Radius oder die Schalter geändert haben, und nach jedem App-Start
     * (Android vergisst Geofences bei Neustart und Neuinstallation). Kostet nichts, solange alles gleich bleibt.
     */
    @Volatile private var eingerichtetInDiesemLauf = ""

    @SuppressLint("MissingPermission")
    suspend fun abgleichen(context: Context) {
        val app = context.applicationContext
        val e = de.frank.jarvis.data.Einstellungen.get(app)
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val gewuenscht = e.kaffeeWachHalten && e.kaffeeNurZuhause && !de.frank.jarvis.data.Geraet.istTablet && de.frank.jarvis.fahrt.Standort.immerErlaubt(app)
        if (!gewuenscht) {
            if (p.getString("umkreis", "").orEmpty().isNotEmpty() || eingerichtetInDiesemLauf.isNotEmpty()) {
                runCatching { LocationServices.getGeofencingClient(app).removeGeofences(absicht(app)) }
                p.edit().putString("umkreis", "").apply()
                eingerichtetInDiesemLauf = ""
            }
            return
        }
        val heim = Kaffeemaschine.zuhause(app) ?: return
        val kennung = "${heim.first},${heim.second},${e.kaffeeRadius}"
        if (eingerichtetInDiesemLauf == kennung) return
        val anfrage = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(
                Geofence.Builder().setRequestId(ID)
                    .setCircularRegion(heim.first, heim.second, e.kaffeeRadius.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build(),
            ).build()
        eingerichtetInDiesemLauf = kennung
        runCatching {
            LocationServices.getGeofencingClient(app).addGeofences(anfrage, absicht(app))
                .addOnSuccessListener {
                    p.edit().putString("umkreis", kennung).apply()
                    de.frank.jarvis.observability.Logbuch.system("Kaffeemaschine Umkreis", "eingerichtet (${e.kaffeeRadius} m)")
                }
                .addOnFailureListener { f ->
                    p.edit().putString("umkreis", "").apply()
                    eingerichtetInDiesemLauf = ""
                    de.frank.jarvis.observability.Logbuch.system("Kaffeemaschine Umkreis", "nicht eingerichtet: ${f.message}; Jarvis fragt den Standort selbst ab")
                }
        }.onFailure { p.edit().putString("umkreis", "").apply(); eingerichtetInDiesemLauf = "" }
    }
}

/** Empfängt Betreten und Verlassen des Umkreises von Android und gibt es an die Kaffeemaschine weiter. */
class KaffeeUmkreisEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ereignis = GeofencingEvent.fromIntent(intent) ?: return
        if (ereignis.hasError()) return
        val app = context.applicationContext
        val warten = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (ereignis.geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_EXIT -> Kaffeemaschine.umkreisVerlassen(app)
                    Geofence.GEOFENCE_TRANSITION_ENTER -> Kaffeemaschine.umkreisBetreten(app)
                }
            } catch (_: Exception) {
            } finally { warten.finish() }
        }
    }
}
