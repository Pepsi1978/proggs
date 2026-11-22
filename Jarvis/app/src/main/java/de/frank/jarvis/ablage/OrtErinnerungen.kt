package de.frank.jarvis.ablage

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import de.frank.jarvis.R
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.fahrt.Orte
import de.frank.jarvis.fahrt.Standort
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Standort-Erinnerungen: „Erinnere mich, wenn ich bei Papa bin, an …“.
 *
 * Der Geofence-Dienst von Android weckt Jarvis nur (Betreten, Verlassen). Ob ausgelöst wird, entscheidet allein ein frischer, genauer Standort
 * die je Erinnerung gewählte Verzögerung (0 bis 20 Minuten) nach der erkannten Ankunft: drin = auslösen, sicher draußen = nichts, unsicher = begrenzt jede Minute erneut prüfen.
 * Nur am Handy (Franks mitgeführtes Gerät); das Tablet richtet keinen Umkreis ein und löst nie aus, so gibt es keine doppelte Auslösung.
 * Kein Bewegungsverlauf: je Ort nur „drin seit“, „draußen seit“ und ein Versuchszähler, jeweils überschrieben; keine Koordinaten im Logbuch.
 */
object OrtErinnerungen {
    const val RADIUS_M = 150
    /** Minuten nach der Ankunft: Vorgabe beim Anlegen (Sprache und Maske) und erlaubter Bereich des Reglers. */
    const val VERZOEGERUNG_STANDARD = 5
    const val VERZOEGERUNG_MAX = 20
    fun verzoegerungGueltig(min: Int) = min.coerceIn(0, VERZOEGERUNG_MAX)
    private fun verzoegerungMs(o: OrtBedingung) = verzoegerungGueltig(o.verzoegerung) * 60_000L
    /** Verlassen und binnen dieser Zeit wieder da: derselbe Aufenthalt, wenn er schon ausgelöst hat (Geofences flattern in Gebäuden). */
    private const val FLATTERN_MS = 10 * 60_000L
    private const val MAX_VERSUCHE = 10
    private const val ANKUNFT_RAND_MS = 3 * 60_000L
    private const val FIX_ALTER_MS = 90_000L
    private const val FIX_GENAUIGKEIT_M = 100f
    private const val NEU_REGISTRIERT_MS = 4 * 60_000L
    private const val NEU_EINRICHTEN_MS = 60 * 60_000L
    private const val PREFS = "ort_erinnerungen"
    const val AKTION_PRUEFEN = "de.frank.jarvis.ORT_ERINNERUNG_PRUEFEN"
    const val AKTION_TEST = "de.frank.jarvis.ORT_ERINNERUNG_TEST"
    private const val CODE_UMKREIS = 4721
    private const val CODE_WECKER = 4722
    private const val HINWEIS_NUMMER = 4723

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sperre = Mutex()
    @Volatile private var eingerichtet = ""
    @Volatile private var eingerichtetUm = 0L
    @Volatile private var fehlschlag = ""
    @Volatile private var fehlschlagUm = 0L

    // ------------------------------------------------------------------ Ort auflösen (beim Anlegen)

    sealed class Aufloesung {
        class Ok(val ort: OrtBedingung) : Aufloesung()
        class Mehrdeutig(val vorschlaege: List<String>) : Aufloesung()
        class Fehlt(val grund: String) : Aufloesung()
    }

    /** „Papa“, „Zuhause“, „Arbeit“ aus den Einstellungen, sonst die genannte Adresse über den Geocoder. Netz nötig, also nur im Hintergrund aufrufen. */
    @Suppress("DEPRECATION")
    suspend fun aufloesen(context: Context, eingabe: String): Aufloesung = withContext(Dispatchers.IO) {
        val roh = eingabe.trim().trim('.', ',', '„', '“', '"').trim()
        if (roh.isEmpty()) return@withContext Aufloesung.Fehlt("Es fehlt der Ort. Frag Frank, wo genau.")
        if (Orte.unklar(roh)) return@withContext Aufloesung.Fehlt("Für „$roh“ ist keine Adresse hinterlegt. Frag Frank nach der genauen Adresse.")
        val benannt = Orte.benannt(context, roh)
        val name = if (benannt != null) roh.lowercase(Locale.GERMAN).let { n ->
            when {
                "papa" in n || "papi" in n || "vater" in n -> "Papa"
                "arbeit" in n || "dienst" in n || "museumsinsel" in n -> "Arbeit"
                else -> "Zuhause"
            }
        } else roh
        if (benannt != null && benannt.isBlank()) return@withContext Aufloesung.Fehlt("Für $name ist in Jarvis keine Adresse hinterlegt (Einstellungen, Pünktlich losfahren). Frag Frank nach der Adresse.")
        val adresse = benannt ?: roh
        val treffer = try { android.location.Geocoder(context, Locale.GERMANY).getFromLocationName(adresse, 5).orEmpty() }
            catch (e: Exception) { return@withContext Aufloesung.Fehlt("Die Adresse ließ sich gerade nicht auflösen (kein Netz?). Nichts gespeichert; bitte gleich noch einmal.") }
        if (treffer.isEmpty()) return@withContext Aufloesung.Fehlt("Zu „$adresse“ findet Jarvis keinen Ort. Frag Frank nach Straße, Hausnummer und Ort.")
        // Mehrere Treffer weiter als 1 km auseinander: nicht raten. Benannte Orte sind Franks feste Adressen, dort gilt der erste.
        val verschieden = treffer.fold(mutableListOf<android.location.Address>()) { acc, t -> if (acc.none { abstand(it.latitude, it.longitude, t.latitude, t.longitude) < 1000f }) acc += t; acc }
        if (benannt == null && verschieden.size > 1) return@withContext Aufloesung.Mehrdeutig(verschieden.take(4).map { zeile(it) })
        val t = treffer.first()
        Aufloesung.Ok(OrtBedingung(name, if (benannt != null) adresse else zeile(t), t.latitude, t.longitude))
    }

    private fun zeile(a: android.location.Address) = (0..a.maxAddressLineIndex).joinToString(", ") { a.getAddressLine(it) }.ifBlank { listOfNotNull(a.thoroughfare, a.subThoroughfare, a.postalCode, a.locality).joinToString(" ") }

    private fun abstand(b1: Double, l1: Double, b2: Double, l2: Double): Float { val d = FloatArray(1); Location.distanceBetween(b1, l1, b2, l2, d); return d[0] }

    /** Ein Umkreis je Ort, nicht je Erinnerung: mehrere Erinnerungen bei Papa teilen sich den Aufenthalt. */
    private fun schluessel(o: OrtBedingung) = String.format(Locale.US, "%.5f,%.5f,%d", o.breite, o.laenge, o.radius)

    // ------------------------------------------------------------------ Berechtigungen

    fun ortungAn(context: Context): Boolean = runCatching { context.getSystemService(LocationManager::class.java).isLocationEnabled }.getOrDefault(true)

    /** null = alles da; sonst ein verständlicher Satz, was fehlt. */
    fun fehlendeFreigabe(context: Context): String? = when {
        !Standort.erlaubt(context) -> "Jarvis darf den Standort nicht verwenden. Standort-Erinnerungen lösen erst aus, wenn du in den App-Infos von Jarvis unter Berechtigungen, Standort „Immer zulassen“ wählst."
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED ->
            "Jarvis kennt nur deinen ungefähren Standort. Damit Standort-Erinnerungen auslösen, schalte in den App-Infos von Jarvis unter Berechtigungen, Standort „Genauen Standort verwenden“ ein und wähle „Immer zulassen“."
        !Standort.immerErlaubt(context) -> "Jarvis darf den Standort nur bei geöffneter App verwenden. Damit Standort-Erinnerungen auch bei gesperrtem Handy auslösen, wähle in den App-Infos von Jarvis unter Berechtigungen, Standort „Immer zulassen“."
        !ortungAn(context) -> "Die Standortermittlung des Handys ist ausgeschaltet. Standort-Erinnerungen lösen erst wieder aus, wenn du den Standort in den Schnelleinstellungen einschaltest."
        else -> null
    }

    private fun offene(app: Context) = Erinnerungen.alle(app).filter { it.ort != null && it.erledigt == 0L }

    // ------------------------------------------------------------------ Umkreise einrichten

    private fun umkreisAbsicht(app: Context): PendingIntent = PendingIntent.getBroadcast(app, CODE_UMKREIS,
        Intent(app, OrtErinnerungEmpfaenger::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

    private fun weckerAbsicht(app: Context): PendingIntent = PendingIntent.getBroadcast(app, CODE_WECKER,
        Intent(app, OrtErinnerungEmpfaenger::class.java).setAction(AKTION_PRUEFEN), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun prefs(app: Context) = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Nach jeder Änderung an Erinnerungen und einmal je Minute: richtet die Umkreise ein, wenn sich die offenen Orte geändert haben,
     * nach jedem App-Start und spätestens stündlich neu (Android vergisst Geofences bei Neustart, Ortung aus oder Zurücksetzen der Google-Dienste).
     */
    @SuppressLint("MissingPermission")
    fun abgleichen(context: Context) {
        val app = context.applicationContext
        if (Geraet.istTablet) return
        val orte = offene(app).mapNotNull { it.ort }.associateBy { schluessel(it) }
        val fehlt = fehlendeFreigabe(app)
        val p = prefs(app)
        // Ein Einrichtungsfehler bleibt sichtbar, bis eine Einrichtung gelingt; nicht jede Minute neu melden.
        hinweisPflegen(app, if (orte.isEmpty()) null else fehlt ?: p.getString("fehler", "").orEmpty().ifEmpty { null })
        if (orte.isEmpty() || fehlt != null) {
            if (eingerichtet.isNotEmpty() || p.getString("eingerichtet", "").orEmpty().isNotEmpty()) {
                runCatching { LocationServices.getGeofencingClient(app).removeGeofences(umkreisAbsicht(app)) }
                p.edit().putString("eingerichtet", "").apply()
                eingerichtet = ""
            }
            // Ohne Umkreis kommt kein Verlassen an: alter Aufenthaltsstand gilt nicht mehr, die nächste Ankunft ist neu.
            aufraeumen(app, emptySet())
            if (orte.isEmpty()) runCatching { app.getSystemService(AlarmManager::class.java).cancel(weckerAbsicht(app)) }
            return
        }
        aufraeumen(app, orte.keys)
        val kennung = orte.keys.sorted().joinToString("|")
        val jetzt = System.currentTimeMillis()
        // Geänderte Minuten nach Ankunft gelten sofort, auch mitten im Aufenthalt.
        weckerPlanen(app)
        if (eingerichtet == kennung && jetzt - eingerichtetUm < NEU_EINRICHTEN_MS) return
        // Nach einem Fehlschlag nicht jede Minute, sondern höchstens alle 10 Minuten erneut versuchen.
        if (fehlschlag == kennung && jetzt - fehlschlagUm < 10 * 60_000L) return
        // Erste Einrichtung eines Orts merken: Ein „Betreten“ kurz danach bei frisch angelegter Erinnerung heißt „war schon da“.
        val e = p.edit()
        orte.keys.filter { !p.contains("reg_$it") }.forEach { e.putLong("reg_$it", jetzt) }
        e.apply()
        // Auch „draußen“ beim Einrichten melden: Nach Neustart oder Ortung aus kam kein Verlassen an, der alte Aufenthalt endet so.
        val anfrage = GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT).apply {
            orte.forEach { (k, o) ->
                addGeofence(Geofence.Builder().setRequestId(k).setCircularRegion(o.breite, o.laenge, o.radius.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE).setNotificationResponsiveness(30_000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT).build())
            }
        }.build()
        eingerichtet = kennung
        eingerichtetUm = jetzt
        runCatching {
            LocationServices.getGeofencingClient(app).addGeofences(anfrage, umkreisAbsicht(app))
                .addOnSuccessListener { fehlschlag = ""; p.edit().putString("eingerichtet", kennung).remove("fehler").apply(); hinweisPflegen(app, fehlendeFreigabe(app)) }
                .addOnFailureListener { f ->
                    eingerichtet = ""
                    fehlschlag = kennung
                    fehlschlagUm = System.currentTimeMillis()
                    val code = (f as? com.google.android.gms.common.api.ApiException)?.statusCode
                    Logbuch.system("Standort-Erinnerungen", "Umkreis nicht eingerichtet: ${f.message}")
                    val grund = if (code == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) "Android stellt gerade keine Ortsüberwachung bereit. Bitte den Standort einschalten und in den Standort-Einstellungen die Google-Standortgenauigkeit aktivieren."
                        else "Die Ortsüberwachung für Standort-Erinnerungen ließ sich nicht einrichten (${f.message ?: "unbekannter Fehler"}). Jarvis versucht es alle 10 Minuten erneut."
                    p.edit().putString("fehler", grund).apply()
                    hinweisPflegen(app, grund)
                }
        }.onFailure { eingerichtet = ""; Logbuch.fehler("Standort-Erinnerungen einrichten", it) }
    }

    /** Zustand von Orten ohne offene Erinnerung vergessen: nichts bleibt liegen. */
    private fun aufraeumen(app: Context, behalten: Set<String>) {
        val p = prefs(app)
        val weg = p.all.keys.filter { k -> listOf("drin_", "draussen_", "versuche_", "geprueft_", "betreten_", "ausgeloest_", "reg_").any { k.startsWith(it) } && k.substringAfter('_') !in behalten }
        if (weg.isNotEmpty()) p.edit().apply { weg.forEach { remove(it) } }.apply()
    }

    // ------------------------------------------------------------------ Betreten und Verlassen

    fun empfangen(context: Context, intent: Intent, fertig: () -> Unit) {
        val app = context.applicationContext
        bereich.launch {
            try {
                when (intent.action) {
                    AKTION_PRUEFEN -> pruefen(app)
                    AKTION_TEST -> test(app, intent.getStringExtra("id").orEmpty())
                    else -> {
                        val ereignis = GeofencingEvent.fromIntent(intent)
                        if (ereignis == null || ereignis.hasError()) { ereignis?.let { Logbuch.system("Standort-Erinnerungen", "Umkreis-Fehler ${it.errorCode}") }; return@launch }
                        val orte = ereignis.triggeringGeofences.orEmpty().map { it.requestId }
                        when (ereignis.geofenceTransition) {
                            Geofence.GEOFENCE_TRANSITION_ENTER -> orte.forEach { betreten(app, it) }
                            Geofence.GEOFENCE_TRANSITION_EXIT -> orte.forEach { verlassen(app, it) }
                        }
                        sperre.withLock { weckerStellen(app) }
                    }
                }
            } catch (e: Exception) { Logbuch.fehler("Standort-Erinnerungen", e) } finally { fertig() }
        }
    }

    private suspend fun betreten(app: Context, k: String) = sperre.withLock {
        val p = prefs(app)
        val jetzt = System.currentTimeMillis()
        val drin = p.getLong("drin_$k", 0L)
        val draussen = p.getLong("draussen_$k", 0L)
        // Ort erst seit Kurzem überwacht und jede offene Erinnerung dort gerade erst scharf: Frank war beim Anlegen schon da.
        val frisch = jetzt - p.getLong("reg_$k", 0L) < NEU_REGISTRIERT_MS &&
            offene(app).mapNotNull { it.ort }.filter { schluessel(it) == k }.let { l -> l.isNotEmpty() && l.all { jetzt - it.scharfSeit < NEU_REGISTRIERT_MS + 60_000L } }
        // Schon in diesem Aufenthalt ausgelöst (alle oder nur die mit kürzerer Verzögerung)?
        val schonAusgeloest = p.getBoolean("ausgeloest_$k", false) || (drin > 0L && offene(app).any { it.ort != null && schluessel(it.ort) == k && it.ortAusgeloest >= drin })
        val e = p.edit()
        when {
            // Neu eingerichtet und schon da (Anlegen vor Ort): keine Ankunft, erst die nächste zählt.
            (drin == 0L || draussen > 0L) && frisch -> e.putLong("drin_$k", jetzt).putLong("betreten_$k", jetzt).putBoolean("ausgeloest_$k", true)
            // Erneutes Einrichten: derselbe Aufenthalt.
            drin > 0L && draussen == 0L -> Unit
            // Binnen 10 Minuten zurück nach einer Auslösung (Geofences flattern in Gebäuden): derselbe Aufenthalt, nichts löst doppelt aus;
            // die noch wartenden prüfen ab jetzt neu (Randschutz und Wiederholungen ab dieser Rückkehr).
            drin > 0L && schonAusgeloest && jetzt - draussen < FLATTERN_MS -> e.putLong("betreten_$k", jetzt).putLong("geprueft_$k", 0L)
            // Sonst eine neue Ankunft: Vorbeifahren und kurz darauf Wiederkommen zählt ab dem Wiederkommen.
            else -> e.putLong("drin_$k", jetzt).putLong("betreten_$k", jetzt).putBoolean("ausgeloest_$k", false).putLong("geprueft_$k", 0L)
        }
        e.putLong("draussen_$k", 0L).apply()
    }

    private suspend fun verlassen(app: Context, k: String) = sperre.withLock {
        val p = prefs(app)
        // Nur das erste Verlassen zählt: Ein wiederholtes „draußen“ beim stündlichen Einrichten verlängert die Flatter-Frist nicht.
        if (p.getLong("drin_$k", 0L) > 0L && p.getLong("draussen_$k", 0L) == 0L) p.edit().putLong("draussen_$k", System.currentTimeMillis()).apply()
    }

    /** Shell-Anstoß für den Gerätetest: Ankunft jetzt am Ort dieser Erinnerung; Wecker, echter Standort und Auslösung laufen danach unverändert. */
    private suspend fun test(app: Context, id: String) {
        val o = Erinnerungen.eine(app, id)?.ort ?: run { Logbuch.system("Standort-Erinnerungen", "Test: keine Standort-Erinnerung mit dieser Kennung"); return }
        val k = schluessel(o)
        sperre.withLock {
            val jetzt = System.currentTimeMillis()
            prefs(app).edit().putLong("drin_$k", jetzt).putLong("betreten_$k", jetzt).putLong("draussen_$k", 0L).putBoolean("ausgeloest_$k", false).putLong("geprueft_$k", 0L).apply()
            weckerStellen(app)
        }
        Logbuch.system("Standort-Erinnerungen", "Test: Ankunft gesetzt, Prüfung nach ${verzoegerungGueltig(o.verzoegerung)} Minuten")
    }

    // ------------------------------------------------------------------ Nach der gewählten Verzögerung prüfen

    /**
     * Wartende Orte mit dem Zeitpunkt der nächsten Prüfung. Jede noch nicht ausgelöste Erinnerung hat ihr eigenes Fenster: ab Ankunft plus ihrer Verzögerung
     * (frühestens ab der letzten Rückkehr) bis zu [MAX_VERSUCHE] Prüfungen im Minutentakt. Eine kurze Verzögerung verbraucht so keine Versuche der längeren.
     */
    private fun wartende(app: Context): Map<String, Long> {
        val p = prefs(app)
        val orte = offene(app).mapNotNull { it.ort }.groupBy { schluessel(it) }
        return orte.mapNotNull { (k, liste) ->
            val drin = p.getLong("drin_$k", 0L)
            if (drin == 0L || p.getLong("draussen_$k", 0L) > 0L || p.getBoolean("ausgeloest_$k", false)) return@mapNotNull null
            val offen = liste.filter { it.scharfSeit < drin }
            if (offen.isEmpty()) return@mapNotNull null
            val basis = maxOf(drin, p.getLong("betreten_$k", 0L))
            val letzte = p.getLong("geprueft_$k", 0L)
            offen.mapNotNull { o ->
                val start = drin + verzoegerungMs(o)
                val ende = maxOf(start, basis) + (MAX_VERSUCHE - 1) * 60_000L
                when { letzte < start -> start; letzte + 60_000L <= ende -> letzte + 60_000L; else -> null }
            }.minOrNull()?.let { k to it }
        }.toMap()
    }

    /** Aus dem minütlichen Abgleich (auch Hauptthread): im Hintergrund und gesperrt, damit kein Betreten oder Prüfen dazwischenfunkt. */
    private fun weckerPlanen(app: Context) { bereich.launch { runCatching { sperre.withLock { weckerStellen(app) } }.onFailure { Logbuch.fehler("Standort-Erinnerung Wecker", it) } } }

    private fun weckerStellen(app: Context) {
        val wecker = app.getSystemService(AlarmManager::class.java)
        val zeit = wartende(app).values.minOrNull()
        val pi = weckerAbsicht(app)
        if (zeit == null) { runCatching { wecker.cancel(pi) }; return }
        val wann = maxOf(zeit, System.currentTimeMillis() + 1_000L)
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || wecker.canScheduleExactAlarms()) wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wann, pi)
            else wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wann, pi)
        }.onFailure { Logbuch.fehler("Standort-Erinnerung Wecker", it) }
    }

    enum class Lage { DRIN, DRAUSSEN, UNSICHER }

    /** Reine Entscheidung aus einem Standort: nur ein frischer, genauer Fix im Radius gilt als „noch dort“. */
    fun lage(abstandM: Float, genauigkeitM: Float, alterMs: Long, radiusM: Int): Lage = when {
        alterMs <= FIX_ALTER_MS && genauigkeitM <= FIX_GENAUIGKEIT_M && abstandM <= radiusM -> Lage.DRIN
        alterMs <= FIX_ALTER_MS && abstandM - genauigkeitM > radiusM -> Lage.DRAUSSEN
        else -> Lage.UNSICHER
    }

    private suspend fun pruefen(app: Context) {
        val jetzt = System.currentTimeMillis()
        val faellig = wartende(app).filterValues { it <= jetzt + 5_000L }.keys
        if (faellig.isEmpty()) { sperre.withLock { weckerStellen(app) }; return }
        if (Geraet.istTablet || fehlendeFreigabe(app) != null) { Logbuch.system("Standort-Erinnerungen", "nicht geprüft: Standortfreigabe fehlt"); hinweisPflegen(app, fehlendeFreigabe(app)); return }
        val wach = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:ort-erinnerung").apply { setReferenceCounted(false) }
        runCatching { wach.acquire(60_000L) }
        try {
            val fix = frischerStandort(app)
            sperre.withLock {
                val p = prefs(app)
                for (k in faellig) {
                    val liste = offene(app).filter { it.ort != null && schluessel(it.ort) == k }
                    val o = liste.firstOrNull()?.ort ?: continue
                    val drin = p.getLong("drin_$k", 0L)
                    val l0 = fix?.let { lage(abstand(o.breite, o.laenge, it.latitude, it.longitude), if (it.hasAccuracy()) it.accuracy else 9_999f, System.currentTimeMillis() - it.time, o.radius) } ?: Lage.UNSICHER
                    // Kurz nach dem Betreten (0 Minuten: Prüfung am Rand des Umkreises) zählt „knapp draußen“ noch nicht als weg, sondern als erneut prüfen.
                    val l = if (l0 == Lage.DRAUSSEN && System.currentTimeMillis() - maxOf(drin, p.getLong("betreten_$k", 0L)) < ANKUNFT_RAND_MS) Lage.UNSICHER else l0
                    when (l) {
                        Lage.DRIN -> {
                            val ausgeloest = System.currentTimeMillis()
                            // Nur die, deren Verzögerung um ist; die mit längerer warten im selben Aufenthalt weiter.
                            val wartend = liste.filter { it.ort!!.scharfSeit < drin }
                            val jetztDran = wartend.filter { drin + verzoegerungMs(it.ort!!) <= ausgeloest + 5_000L }
                            p.edit().putBoolean("ausgeloest_$k", jetztDran.size == wartend.size).putLong("geprueft_$k", ausgeloest).apply()
                            jetztDran.forEach { e ->
                                Erinnerungen.aendere(app, e.id) { alt -> if (alt.erledigt != 0L || alt.ort == null) alt else alt.copy(ortAusgeloest = ausgeloest, ort = alt.ort.copy(scharfSeit = ausgeloest)) }
                                Logbuch.system("Standort-Erinnerung ausgelöst", "${e.zeile} (bei ${o.name})")
                            }
                        }
                        // Sicher nicht (mehr) dort: dieser Aufenthalt zählt nicht; die Erinnerung bleibt für die nächste Ankunft offen.
                        Lage.DRAUSSEN -> { p.edit().putLong("drin_$k", 0L).putLong("draussen_$k", 0L).putLong("betreten_$k", 0L).putLong("geprueft_$k", 0L).apply(); Logbuch.system("Standort-Erinnerungen", "nicht ausgelöst: nicht mehr bei ${o.name}") }
                        Lage.UNSICHER -> {
                            p.edit().putLong("geprueft_$k", System.currentTimeMillis()).commit()
                            Logbuch.system("Standort-Erinnerungen", if (k !in wartende(app)) "nicht ausgelöst: kein verlässlicher Standort bei ${o.name}" else "Standort noch unsicher, neuer Versuch")
                        }
                    }
                }
            }
        } finally { runCatching { if (wach.isHeld) wach.release() } }
        sperre.withLock { weckerStellen(app) }
    }

    /** Frischer Standort über die Google-Ortung (hohe Genauigkeit), ersatzweise über Android selbst. Wird nicht gespeichert. */
    @SuppressLint("MissingPermission")
    private suspend fun frischerStandort(app: Context): Location? {
        val google = withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine<Location?> { w ->
                val abbruch = CancellationTokenSource()
                w.invokeOnCancellation { abbruch.cancel() }
                runCatching {
                    LocationServices.getFusedLocationProviderClient(app).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, abbruch.token)
                        .addOnSuccessListener { if (w.isActive) w.resume(it) }
                        .addOnFailureListener { if (w.isActive) w.resume(null) }
                }.onFailure { if (w.isActive) w.resume(null) }
            }
        }
        if (google != null) return google
        return Standort.aktuell(app, 8_000L)
    }

    // ------------------------------------------------------------------ Fehlende Freigabe sichtbar machen

    /** Eine Meldung, solange offene Standort-Erinnerungen nicht auslösen können; verschwindet von selbst, wenn alles wieder geht. */
    private fun hinweisPflegen(app: Context, grund: String?) {
        val p = prefs(app)
        val nm = app.getSystemService(NotificationManager::class.java)
        if (grund == null) {
            if (p.getString("hinweis", "").orEmpty().isNotEmpty()) { nm.cancel(HINWEIS_NUMMER); p.edit().putString("hinweis", "").apply() }
            return
        }
        if (p.getString("hinweis", "") == grund) return
        p.edit().putString("hinweis", grund).apply()
        Logbuch.system("Standort-Erinnerungen", grund)
        val oeffnen = PendingIntent.getActivity(app, HINWEIS_NUMMER, Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", app.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        runCatching {
            nm.notify(HINWEIS_NUMMER, NotificationCompat.Builder(app, ErinnerungPlaner.KANAL).setSmallIcon(R.drawable.ic_stat_jarvis)
                .setContentTitle("Standort-Erinnerungen pausieren").setContentText(grund).setStyle(NotificationCompat.BigTextStyle().bigText(grund))
                .setContentIntent(oeffnen).setAutoCancel(true).setOnlyAlertOnce(true).build())
        }
    }
}

/** Betreten, Verlassen, die Prüfung nach der gewählten Verzögerung und der Shell-Test (`adb shell am broadcast -a de.frank.jarvis.ORT_ERINNERUNG_TEST -p de.frank.jarvis --es id <Kennung>`). */
class OrtErinnerungEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rest = goAsync()
        OrtErinnerungen.empfangen(context, intent) { rest.finish() }
    }
}

/** Nur Shell und System (Manifest: Recht DUMP): stößt den Gerätetest an, ohne dass jemand anderes Ankünfte vortäuschen kann. */
class OrtErinnerungTestEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rest = goAsync()
        OrtErinnerungen.empfangen(context, Intent(intent).setAction(OrtErinnerungen.AKTION_TEST)) { rest.finish() }
    }
}
