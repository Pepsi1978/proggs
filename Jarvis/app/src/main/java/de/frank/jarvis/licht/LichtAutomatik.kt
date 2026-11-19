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

    private val dienstCache = java.util.concurrent.ConcurrentHashMap<LocalDate, Pair<Long, String>>()

    /**
     * Dienstart des Tages: „tag“, „nacht“, „frei“ oder null, wenn der Kalender nicht lesbar ist (je Tag 30 Minuten gepuffert).
     * Hinweis: Der Kalender ordnet Termine nach der Zeitzone des Geräts zu; Frank ist in Berlin, das Gerät auch.
     */
    private fun dienst(c: Context, tag: LocalDate): String? {
        dienstCache[tag]?.takeIf { System.currentTimeMillis() - it.first < 30 * 60_000L }?.let { return it.second }
        val wert = runCatching {
            de.frank.jarvis.faehigkeit.Register.alle(c).filterIsInstance<de.frank.jarvis.faehigkeit.KalenderFaehigkeit>().firstOrNull()?.schichten(tag, 1)?.let { it[tag]?.first ?: "frei" }
        }.getOrNull() ?: return null
        dienstCache.keys.removeIf { it.isBefore(tag.minusDays(2)) }
        dienstCache[tag] = System.currentTimeMillis() to wert
        return wert
    }

    fun fenster(c: Context, tag: LocalDate = LocalDate.now(BERLIN)): Fenster? {
        val e = Einstellungen.get(c)
        if (!e.lichtAutomatik) return null
        // null = Kalender nicht lesbar (kein Recht, Fehler): dann lieber nicht schalten als an einem Nachtdienst-Tag.
        val (art, ausStunde) = when (dienst(c, tag) ?: return null) {
            "nacht" -> return null
            "tag" -> if (e.lichtAutoTag) "Tagdienst" to e.lichtAusTag else return null
            else -> if (e.lichtAutoFrei) "frei" to e.lichtAusFrei else return null
        }
        val hoehe = if (e.lichtAnBei == "daemmerung") Sonnenstand.SONNENUNTERGANG else Sonnenstand.DUNKEL
        val an = java.time.Instant.ofEpochMilli(Sonnenstand.abend(tag, hoehe)).atZone(BERLIN).toLocalDateTime()
        val aus = tag.atTime(ausStunde, 0)
        return if (an.isBefore(aus)) Fenster(an, aus, art) else null
    }

    /** Warum die Automatik heute nicht schaltet, in einem Satz für Frank. */
    fun grundOhneFenster(c: Context, tag: LocalDate = LocalDate.now(BERLIN)): String {
        val e = Einstellungen.get(c)
        return when {
            !e.lichtAutomatik -> "Die Licht-Automatik ist ausgeschaltet."
            else -> when (dienst(c, tag)) {
                null -> "Jarvis kann den Dienstplan im Kalender gerade nicht lesen, deshalb schaltet die Automatik heute sicherheitshalber nicht."
                "nacht" -> "Heute ist Nachtdienst, da schaltet die Automatik nicht."
                "tag" -> if (!e.lichtAutoTag) "Die Automatik bei Tagdienst ist ausgeschaltet." else "Heute ${spaet(e.lichtAnBei)} erst nach ${e.lichtAusTag} Uhr, deshalb schaltet die Automatik nicht."
                else -> if (!e.lichtAutoFrei) "Die Automatik an freien Tagen ist ausgeschaltet." else "Heute ${spaet(e.lichtAnBei)} erst nach ${e.lichtAusFrei} Uhr, deshalb schaltet die Automatik nicht."
            }
        }
    }

    private fun spaet(anBei: String) = if (anBei == "daemmerung") "geht die Sonne" else "wird es"

    private fun p(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Ist Frank laut Umkreis zu Hause? Ohne Umkreis-Schalter oder ohne eingerichteten Umkreis immer ja (ein altes „weg“ blockiert sonst für immer). */
    fun zuHause(c: Context): Boolean = !Einstellungen.get(c).lichtNurZuhause || eingerichtet.isEmpty() || !p(c).getBoolean("weg", false)

    /** Aus der Minutenrunde; kostet nichts, solange nichts fällig ist. */
    suspend fun pruefen(context: Context) {
        if (Geraet.istTablet) return
        val c = context.applicationContext
        runCatching { umkreisAbgleichen(c) }
        if (!laeuft.compareAndSet(false, true)) return
        try {
            val jetzt = LocalDateTime.now(BERLIN)
            val pr = p(c)
            val faellig = pr.getLong("wegAusFaellig", 0L)
            if (faellig > 0L && System.currentTimeMillis() >= faellig) {
                pr.edit().putLong("wegAusFaellig", 0L).apply()
                if (pr.getBoolean("weg", false)) {
                    val b = LedStreifen.aus(c)
                    Logbuch.system("Licht Umkreis", "Frank ist weg: ${b.ok} von ${b.gesamt} aus")
                }
            }
            // Ausschalten auch nach Mitternacht nachholen (Fenster von gestern, höchstens 3 Stunden nach der Ausschaltzeit).
            for (tag in listOf(jetzt.toLocalDate().minusDays(1), jetzt.toLocalDate())) {
                val f = fenster(c, tag) ?: continue
                if (jetzt >= f.aus && jetzt < f.aus.plusHours(3) && pr.getString("ausTag", "") != tag.toString()) {
                    pr.edit().putString("ausTag", tag.toString()).apply()
                    val b = LedStreifen.aus(c)
                    Logbuch.system("Licht-Automatik", "${f.art}: Ausschaltzeit ${f.aus.hour} Uhr, ${b.ok} von ${b.gesamt} aus")
                }
            }
            val heute = jetzt.toLocalDate()
            val f = fenster(c, heute) ?: return
            // Einschalten: erledigt erst nach Erfolg oder bewusstem Auslassen; sonst alle 5 Minuten neu versuchen.
            if (jetzt >= f.an && jetzt < f.aus && pr.getString("anTag", "") != heute.toString() &&
                System.currentTimeMillis() - pr.getLong("anVersuch", 0L) >= 5 * 60_000L) {
                pr.edit().putLong("anVersuch", System.currentTimeMillis()).apply()
                if (einschaltenWennAus(c, "Automatik ${f.art}: ${if (Einstellungen.get(c).lichtAnBei == "daemmerung") "Dämmerung" else "dunkel"}"))
                    pr.edit().putString("anTag", heute.toString()).apply()
            }
        } catch (x: Exception) {
            Logbuch.fehler("Licht-Automatik", x)
        } finally { laeuft.set(false) }
    }

    /** true = erledigt (eingeschaltet, schon an oder Frank ist weg); false = gescheitert, später erneut. */
    private suspend fun einschaltenWennAus(c: Context, grund: String): Boolean {
        if (!zuHause(c)) { Logbuch.system("Licht-Automatik", "$grund: Frank ist nicht zu Hause, Licht bleibt aus"); return true }
        // Nur wenn alle eigenen Streifen gelesen und an sind, nichts tun; nicht lesbar (Handy außer Reichweite): schalten, der Mac hilft.
        if (LedStreifen.istAn(c) == true) { Logbuch.system("Licht-Automatik", "$grund: alle Streifen sind schon an, nichts zu tun"); return true }
        val b = LedStreifen.an(c)
        Logbuch.system("Licht-Automatik", "$grund: ${b.ok} von ${b.gesamt} an")
        return b.ok > 0
    }

    // ------------------------------------------------------------------ Umkreis

    private fun absicht(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 4712, Intent(c, LichtUmkreisEmpfaenger::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    @Volatile private var eingerichtet = ""
    @Volatile private var entferntInDiesemLauf = false
    @Volatile private var zuhauseFehlerBis = 0L

    /** Richtet den Umkreis ein oder neu (Zuhause, Radius oder Schalter geändert, App neu gestartet). */
    @SuppressLint("MissingPermission")
    private suspend fun umkreisAbgleichen(c: Context) {
        val e = Einstellungen.get(c)
        val gewuenscht = e.lichtAutomatik && e.lichtNurZuhause && de.frank.jarvis.fahrt.Standort.immerErlaubt(c)
        if (!gewuenscht) {
            // Auch nach einem Neustart entfernen: Geofences überleben den Prozess, „eingerichtet“ nicht.
            if (eingerichtet.isNotEmpty() || !entferntInDiesemLauf) {
                runCatching { LocationServices.getGeofencingClient(c).removeGeofences(absicht(c)) }
                eingerichtet = ""; entferntInDiesemLauf = true
            }
            return
        }
        entferntInDiesemLauf = false
        if (System.currentTimeMillis() < zuhauseFehlerBis) return
        // Adresse nicht auflösbar: höchstens stündlich neu versuchen (Geocoder geht übers Netz).
        val heim = de.frank.jarvis.kaffee.Kaffeemaschine.zuhause(c) ?: run { zuhauseFehlerBis = System.currentTimeMillis() + 3_600_000L; return }
        val kennung = "${heim.first},${heim.second},${e.lichtRadius}"
        if (eingerichtet == kennung) return
        eingerichtet = kennung
        val anfrage = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(
                Geofence.Builder().setRequestId(ID).setCircularRegion(heim.first, heim.second, e.lichtRadius.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(60_000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT).build(),
            ).build()
        runCatching {
            LocationServices.getGeofencingClient(c).addGeofences(anfrage, absicht(c))
                .addOnSuccessListener { Logbuch.system("Licht Umkreis", "eingerichtet (${e.lichtRadius} m)") }
                .addOnFailureListener { f -> eingerichtet = ""; Logbuch.system("Licht Umkreis", "nicht eingerichtet: ${f.message}") }
        }.onFailure { eingerichtet = "" }
    }

    /**
     * Frank hat das Haus verlassen: nach 3 Minuten Bestätigung Licht aus (GPS-Sprünge bei kleinem Radius melden sonst fälschlich „weg“).
     * Vom Handy aus erreicht das meist nur noch der Mac.
     */
    suspend fun verlassen(c: Context) {
        if (p(c).getBoolean("weg", false)) return // Aus ist schon vorgemerkt bzw. erledigt (die Minutenrunde holt es nach)
        val e = Einstellungen.get(c)
        val faellig = if (e.lichtAutomatik && e.lichtNurZuhause) System.currentTimeMillis() + 3 * 60_000L else 0L
        p(c).edit().putBoolean("weg", true).putLong("wegSeit", System.currentTimeMillis()).putLong("wegAusFaellig", faellig).apply()
        if (faellig > 0L) Logbuch.system("Licht Umkreis", "Frank verlässt das Haus: Licht geht in 3 Minuten aus, falls er nicht zurückkommt")
    }

    /** Frank ist zurück: im heutigen Fenster an, wenn es aus ist (nur mit eingeschaltetem Umkreis-Schalter). */
    suspend fun betreten(c: Context) {
        if (!p(c).getBoolean("weg", false)) return
        p(c).edit().putBoolean("weg", false).putLong("wegAusFaellig", 0L).apply()
        val e = Einstellungen.get(c)
        if (!e.lichtAutomatik || !e.lichtNurZuhause) return
        // Nur ein echtes Heimkommen (mindestens 10 Minuten weg) schaltet ein, kein GPS-Sprung.
        if (System.currentTimeMillis() - p(c).getLong("wegSeit", 0L) < 10 * 60_000L) return
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
