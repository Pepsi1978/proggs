package de.frank.jarvis.licht

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.ui.theme.Sonnenstand
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Licht-Automatik (nur am Handy, das Tablet plant nie, sonst schaltet es doppelt). Aus der Minutenrunde ([pruefen]):
 * - Freie Tage (frei, krank, Urlaub, Freischicht; Dienstplan über KalenderFaehigkeit.schichten) und Tagdienst: ab Dämmerung bzw. Dunkelheit (Berlin, [Sonnenstand])
 *   einmal an, nur wenn die Streifen aus sind und Frank zu Hause ist; zur Ausschaltzeit (frei 22 Uhr, Tagdienst 19 Uhr) einmal aus.
 *   Liegt die Einschaltzeit nach der Ausschaltzeit (Sommer), passiert an dem Tag nichts. Nachtdienst: weder an noch aus.
 * - Umkreis um Zuhause (Geofence wie bei der Kaffeemaschine, eigener Radius): beim Verlassen aus (das übernimmt dann der Mac), beim Heimkommen
 *   im Zeitfenster an. Je Tag höchstens ein automatisches Einschalten zur Dämmerung; Heimkommen schaltet erneut, wenn es aus ist.
 */
object LichtAutomatik {
    private const val PREFS = "licht_automatik"
    private const val ID = "licht_zuhause"
    private val laeuft = AtomicBoolean(false)
    /** Prozessweiter Bereich für Umkreis-Ereignisse. */
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val BERLIN = ZoneId.of("Europe/Berlin")

    /** Heutiges Fenster: Einschaltzeit, Ausschaltzeit und Tagesart; null = heute keine Automatik (Nachtdienst, Schalter aus, Sommer). */
    data class Fenster(val an: LocalDateTime, val aus: LocalDateTime, val art: String)

    @Volatile private var dienstCache = Triple<LocalDate?, Long, String?>(null, 0L, null)

    fun fenster(c: Context, tag: LocalDate = LocalDate.now(BERLIN)): Fenster? {
        val e = Einstellungen.get(c)
        if (!e.lichtAutomatik) return null
        val dienst = dienstCache.takeIf { it.first == tag && System.currentTimeMillis() - it.second < 30 * 60_000L }?.third ?: runCatching {
            // null = Kalender nicht lesbar (kein Recht, Fehler): dann lieber nicht schalten als an einem Nachtdienst-Tag.
            de.frank.jarvis.faehigkeit.Register.alle(c).filterIsInstance<de.frank.jarvis.faehigkeit.KalenderFaehigkeit>().firstOrNull()?.schichten(tag, 1)?.let { it[tag]?.first ?: "frei" }
        }.getOrNull().also { if (it != null) dienstCache = Triple(tag, System.currentTimeMillis(), it) } ?: return null
        val (art, ausStunde) = when (dienst) {
            "nacht" -> return null
            "tag" -> if (e.lichtAutoTag) "Tagdienst" to e.lichtAusTag else return null
            else -> if (e.lichtAutoFrei) "frei" to e.lichtAusFrei else return null
        }
        val hoehe = if (e.lichtAnBei == "daemmerung") Sonnenstand.SONNENUNTERGANG else Sonnenstand.DUNKEL
        val an = java.time.Instant.ofEpochMilli(Sonnenstand.abend(tag, hoehe)).atZone(BERLIN).toLocalDateTime()
        val aus = tag.atTime(ausStunde, 0)
        return if (an.isBefore(aus)) Fenster(an, aus, art) else null
    }

    private fun p(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Ist Frank laut Umkreis zu Hause? Ohne Umkreis-Schalter immer ja; ohne Meldung des Systems ebenfalls ja (lieber an als im Dunkeln). */
    fun zuHause(c: Context): Boolean = !Einstellungen.get(c).lichtNurZuhause || !p(c).getBoolean("weg", false)

    /** Aus der Minutenrunde; kostet nichts, solange nichts fällig ist. */
    suspend fun pruefen(context: Context) {
        if (Geraet.istTablet) return
        val c = context.applicationContext
        runCatching { umkreisAbgleichen(c) }
        if (!laeuft.compareAndSet(false, true)) return
        try {
            val f = fenster(c) ?: return
            val jetzt = LocalDateTime.now(BERLIN)
            val heute = jetzt.toLocalDate().toString()
            val pr = p(c)
            when {
                jetzt >= f.an && jetzt < f.aus && pr.getString("anTag", "") != heute -> {
                    pr.edit().putString("anTag", heute).apply()
                    einschaltenWennAus(c, "Automatik ${f.art}: ${if (Einstellungen.get(c).lichtAnBei == "daemmerung") "Dämmerung" else "dunkel"}")
                }
                jetzt >= f.aus && jetzt < f.aus.plusHours(3) && pr.getString("ausTag", "") != heute -> {
                    pr.edit().putString("ausTag", heute).apply()
                    val b = LedStreifen.aus(c)
                    Logbuch.system("Licht-Automatik", "${f.art}: Ausschaltzeit ${f.aus.hour} Uhr, ${b.ok} von ${b.gesamt} aus")
                }
            }
        } catch (x: Exception) {
            Logbuch.fehler("Licht-Automatik", x)
        } finally { laeuft.set(false) }
    }

    private suspend fun einschaltenWennAus(c: Context, grund: String) {
        if (!zuHause(c)) { Logbuch.system("Licht-Automatik", "$grund: Frank ist nicht zu Hause, Licht bleibt aus"); return }
        // Nicht lesbar (Handy außer Reichweite): trotzdem schalten, dann erreicht der Mac die Streifen.
        if (LedStreifen.istAn(c) == true) { Logbuch.system("Licht-Automatik", "$grund: alle lesbaren Streifen sind schon an, nichts zu tun"); return }
        val b = LedStreifen.an(c)
        Logbuch.system("Licht-Automatik", "$grund: ${b.ok} von ${b.gesamt} an")
    }

    // ------------------------------------------------------------------ Umkreis

    private fun absicht(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 4712, Intent(c, LichtUmkreisEmpfaenger::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    @Volatile private var eingerichtet = ""

    /** Richtet den Umkreis ein oder neu (Zuhause, Radius oder Schalter geändert, App neu gestartet). */
    @SuppressLint("MissingPermission")
    private suspend fun umkreisAbgleichen(c: Context) {
        val e = Einstellungen.get(c)
        val gewuenscht = e.lichtAutomatik && e.lichtNurZuhause && de.frank.jarvis.fahrt.Standort.immerErlaubt(c)
        if (!gewuenscht) {
            if (eingerichtet.isNotEmpty()) { runCatching { LocationServices.getGeofencingClient(c).removeGeofences(absicht(c)) }; eingerichtet = "" }
            return
        }
        val heim = de.frank.jarvis.kaffee.Kaffeemaschine.zuhause(c) ?: return
        val kennung = "${heim.first},${heim.second},${e.lichtRadius}"
        if (eingerichtet == kennung) return
        eingerichtet = kennung
        val anfrage = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(
                Geofence.Builder().setRequestId(ID).setCircularRegion(heim.first, heim.second, e.lichtRadius.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT).build(),
            ).build()
        runCatching {
            LocationServices.getGeofencingClient(c).addGeofences(anfrage, absicht(c))
                .addOnSuccessListener { Logbuch.system("Licht Umkreis", "eingerichtet (${e.lichtRadius} m)") }
                .addOnFailureListener { f -> eingerichtet = ""; Logbuch.system("Licht Umkreis", "nicht eingerichtet: ${f.message}") }
        }.onFailure { eingerichtet = "" }
    }

    /** Frank hat das Haus verlassen: Licht aus (vom Handy aus erreicht das meist nur noch der Mac). */
    suspend fun verlassen(c: Context) {
        if (p(c).getBoolean("weg", false)) return
        p(c).edit().putBoolean("weg", true).apply()
        val e = Einstellungen.get(c)
        if (!e.lichtAutomatik || !e.lichtNurZuhause) return
        val b = LedStreifen.aus(c)
        Logbuch.system("Licht Umkreis", "Frank ist weg: ${b.ok} von ${b.gesamt} aus")
    }

    /** Frank ist zurück: im heutigen Fenster an, wenn es aus ist. */
    suspend fun betreten(c: Context) {
        if (!p(c).getBoolean("weg", false)) return
        p(c).edit().putBoolean("weg", false).apply()
        val f = fenster(c) ?: return
        val jetzt = LocalDateTime.now(BERLIN)
        if (jetzt >= f.an && jetzt < f.aus) einschaltenWennAus(c, "Heimkommen (${f.art})")
    }
}

/** Empfängt Betreten und Verlassen des Licht-Umkreises von Android. */
class LichtUmkreisEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ereignis = GeofencingEvent.fromIntent(intent) ?: return
        if (ereignis.hasError()) return
        val app = context.applicationContext
        val uebergang = ereignis.geofenceTransition
        // Schalten kann bis zu einer Minute dauern (Handy, Mac, Sperre): nicht im Broadcast-Zeitlimit warten.
        LichtAutomatik.bereich.launch {
            runCatching {
                when (uebergang) {
                    Geofence.GEOFENCE_TRANSITION_EXIT -> LichtAutomatik.verlassen(app)
                    Geofence.GEOFENCE_TRANSITION_ENTER -> LichtAutomatik.betreten(app)
                }
            }.onFailure { Logbuch.fehler("Licht Umkreis", it) }
        }
    }
}
