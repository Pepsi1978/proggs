package de.frank.jarvis.fahrt

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.speech.Vorleser
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.media.AudioManager
import android.os.PowerManager
import de.frank.jarvis.speech.VorleseZustand
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Pünktlich losfahren: An jedem Arbeitstag laut Dienstplan (Tag 1–4, Nacht 1–4, ohne X, F, U) fragt Jarvis die Fahrzeit
 * zur Arbeit mit Verkehr ab und meldet sich einige Minuten vor der nötigen Abfahrt, mit Benachrichtigung und Stimme.
 *
 * Ablauf je Dienst: Vorprüfung zweieinhalb Stunden vor der gewünschten Ankunft (Google rechnet mit dem erwarteten
 * Verkehr zur Abfahrtszeit), danach 30 und 8 Minuten vor der Meldung und zur Meldung selbst mit frischem Verkehr.
 * Zur Abfahrtszeit folgt eine letzte Prüfung; ist die Fahrt inzwischen deutlich länger, meldet sich Jarvis noch einmal.
 * Es ist immer genau ein exakter Wecker gestellt, wie bei der Tagesauswertung; der Schlaf wird hier bewusst nicht
 * ausgespart, denn vor dem Tagdienst fällt die Meldung in die Zeit um vier Uhr.
 */
object Abfahrt {
    private const val TAG = "JarvisAbfahrt"
    const val AKTION = "de.frank.jarvis.ABFAHRT"
    const val AKTION_STILL = "de.frank.jarvis.ABFAHRT_STILL"
    private const val KANAL = "jarvis_abfahrt"
    private const val HINWEIS_ID = 4200
    /** So lange vor der gewünschten Ankunft beginnt Jarvis zu rechnen (Minuten). */
    private const val VORPRUEFUNG = 150L
    private val UHR = DateTimeFormatter.ofPattern("HH:mm")
    private val laeuft = AtomicBoolean(false)

    /** Die nächste Fahrt zur Arbeit. */
    class Fahrt(val tag: LocalDate, val dienst: String, val ankunft: ZonedDateTime)

    /** Stand der Planung für einen Dienst, in den Einstellungen abgelegt, damit ein Neustart nichts doppelt meldet. */
    private class Zustand(val json: JSONObject) {
        var tag: String get() = json.optString("tag"); set(v) { json.put("tag", v) }
        /** Nötige Abfahrt in Millisekunden, 0 = noch nicht berechnet. */
        var abfahrt: Long get() = json.optLong("abfahrt"); set(v) { json.put("abfahrt", v) }
        var sekunden: Int get() = json.optInt("sekunden"); set(v) { json.put("sekunden", v) }
        var geprueft: Long get() = json.optLong("geprueft"); set(v) { json.put("geprueft", v) }
        var versuch: Long get() = json.optLong("versuch"); set(v) { json.put("versuch", v) }
        var pruefungen: Int get() = json.optInt("pruefungen"); set(v) { json.put("pruefungen", v) }
        var gemeldet: Boolean get() = json.optBoolean("gemeldet"); set(v) { json.put("gemeldet", v) }
        var gemeldetSekunden: Int get() = json.optInt("gemeldet_sekunden"); set(v) { json.put("gemeldet_sekunden", v) }
        var erledigt: Boolean get() = json.optBoolean("erledigt"); set(v) { json.put("erledigt", v) }
        var vomStandort: Boolean get() = json.optBoolean("vom_standort"); set(v) { json.put("vom_standort", v) }
        var fehler: String get() = json.optString("fehler"); set(v) { json.put("fehler", v) }
        fun abfahrtZeit(): ZonedDateTime? = abfahrt.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
    }

    private fun zustand(context: Context, tag: LocalDate): Zustand {
        val gespeichert = runCatching { JSONObject(Einstellungen.get(context).abfahrtZustand) }.getOrNull()?.let(::Zustand)
        return gespeichert?.takeIf { it.tag == tag.toString() } ?: Zustand(JSONObject()).also { it.tag = tag.toString() }
    }

    private fun sichere(context: Context, z: Zustand) { Einstellungen.get(context).abfahrtZustand = z.json.toString() }

    private fun uhrzeit(text: String, vorgabe: LocalTime): LocalTime =
        Regex("^\\s*([01]?\\d|2[0-3])[:.]([0-5]\\d)").find(text)?.let { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) } ?: vorgabe

    /** Der nächste Dienst, dessen gewünschte Ankunft noch bevorsteht und der noch nicht abgeschlossen ist. null = keiner in 14 Tagen oder Kalender nicht lesbar. */
    fun naechste(context: Context, jetzt: ZonedDateTime = ZonedDateTime.now()): Fahrt? {
        val e = Einstellungen.get(context)
        val schichten = Register.alle(context).filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.schichten(jetzt.toLocalDate(), 15) ?: return null
        val erledigt = runCatching { JSONObject(e.abfahrtZustand) }.getOrNull()?.takeIf { it.optBoolean("erledigt") }?.optString("tag")
        return schichten.entries.sortedBy { it.key }.map { (tag, dienst) ->
            val zeit = if (dienst.first == "nacht") uhrzeit(e.ankunftNacht, LocalTime.of(17, 0)) else uhrzeit(e.ankunftTag, LocalTime.of(5, 0))
            Fahrt(tag, dienst.second, tag.atTime(zeit).atZone(jetzt.zone))
        }.firstOrNull { it.ankunft.isAfter(jetzt) && it.tag.toString() != erledigt }
    }

    private fun naechstePruefung(context: Context, f: Fahrt, z: Zustand, jetzt: ZonedDateTime): ZonedDateTime {
        val abfahrt = z.abfahrtZeit()
        var wann = when {
            abfahrt == null -> f.ankunft.minusMinutes(VORPRUEFUNG)
            !z.gemeldet -> {
                val meldung = abfahrt.minusMinutes(Einstellungen.get(context).abfahrtVorlauf.toLong())
                val rest = Duration.between(jetzt, meldung).toMinutes()
                when {
                    rest > 50 -> meldung.minusMinutes(30)
                    rest > 14 -> meldung.minusMinutes(8)
                    else -> meldung
                }
            }
            // Nach der Meldung: kurz vor der Abfahrt noch einmal nachsehen, ob es länger dauert.
            else -> abfahrt.minusMinutes(1)
        }
        // Nach einem Fehlschlag (kein Netz) nicht im Sekundentakt wiederholen.
        if (z.fehler.isNotBlank()) Instant.ofEpochMilli(z.versuch).atZone(jetzt.zone).plusMinutes(3).let { if (it.isAfter(wann)) wann = it }
        return if (wann.isBefore(jetzt.plusSeconds(2))) jetzt.plusSeconds(2) else wann
    }

    /** Stellt den Wecker für die nächste Prüfung. Liest nur den Kalender, kein Netz. */
    fun stelle(context: Context) {
        val app = context.applicationContext
        val wecker = app.getSystemService(AlarmManager::class.java)
        val absicht = PendingIntent.getBroadcast(app, 8, Intent(app, AbfahrtEmpfaenger::class.java).setAction(AKTION), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        // Während einer Prüfung stellt sie den Wecker am Ende selbst.
        if (laeuft.get()) return
        wecker.cancel(absicht)
        val e = Einstellungen.get(app)
        if (!e.abfahrtAn || e.mapsSchluessel.isBlank()) return
        val jetzt = ZonedDateTime.now()
        val f = naechste(app, jetzt)
        // Ohne Dienst in Sicht (oder ohne Kalender) in ein paar Stunden wieder nachsehen.
        val wann = if (f == null) jetzt.plusHours(6) else naechstePruefung(app, f, zustand(app, f.tag), jetzt)
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || wecker.canScheduleExactAlarms()) wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wann.toInstant().toEpochMilli(), absicht)
            else wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wann.toInstant().toEpochMilli(), absicht)
            Log.i(TAG, "Nächste Abfahrtsprüfung: $wann")
        }.onFailure { Log.w(TAG, "Wecker ließ sich nicht stellen", it) }
    }

    /** Eine Prüfung: Fahrzeit holen, Abfahrt neu rechnen und melden, wenn es so weit ist. Läuft im Dienst. */
    suspend fun pruefe(context: Context) {
        val app = context.applicationContext
        if (!laeuft.compareAndSet(false, true)) return
        try {
            val e = Einstellungen.get(app)
            if (!e.abfahrtAn) return
            val jetzt = ZonedDateTime.now()
            val f = naechste(app, jetzt) ?: return
            if (jetzt.isBefore(f.ankunft.minusMinutes(VORPRUEFUNG + 1))) return
            val z = zustand(app, f.tag)
            val geplant = z.abfahrtZeit()
            if (z.gemeldet && geplant != null && jetzt.isAfter(geplant.plusMinutes(10))) {
                z.erledigt = true
                sichere(app, z)
                return
            }
            val standort = Standort.aktuell(app)
            val von = standort?.let { Ort.Punkt(it.latitude, it.longitude) } ?: Ort.Adresse(e.adresseZuhause)
            val nach = Ort.Adresse(e.adresseArbeit)
            z.versuch = System.currentTimeMillis()
            z.pruefungen += 1
            val ergebnis = runCatching {
                if (z.gemeldet) Routen.berechne(app, von, nach).let { it to f.ankunft.minusSeconds(it.sekunden.toLong()) }
                else Routen.fuerAnkunft(app, von, nach, f.ankunft, if (z.sekunden > 0) (z.sekunden + 59) / 60 else 55)
            }
            val vorlauf = e.abfahrtVorlauf.toLong()
            ergebnis.onFailure { fehler ->
                z.fehler = fehler.message ?: "Fahrzeit nicht abrufbar."
                Log.w(TAG, "Prüfung fehlgeschlagen: ${z.fehler}")
                when {
                    z.gemeldet -> z.erledigt = true
                    // Kein frischer Stand, aber ein früherer: lieber mit dem melden als gar nicht.
                    geplant != null && !jetzt.isBefore(geplant.minusMinutes(vorlauf).minusSeconds(90)) -> {
                        melde(app, f, Route(z.sekunden, z.sekunden, 0, ""), geplant, jetzt, veraltet = true)
                        z.gemeldet = true
                        z.gemeldetSekunden = z.sekunden
                        z.erledigt = true
                    }
                    geplant == null && jetzt.isAfter(f.ankunft.minusMinutes(80)) -> {
                        sende(app, "Fahrzeit nicht abrufbar", "Ich konnte die Fahrzeit zur Arbeit nicht abrufen: ${z.fehler} Plane die Abfahrt bitte selbst, Ankunft soll ${f.ankunft.format(UHR)} Uhr sein.",
                            "Frank, ich konnte die Fahrzeit zur Arbeit nicht abrufen. Plane die Abfahrt bitte selbst.")
                        z.erledigt = true
                    }
                }
            }
            ergebnis.onSuccess { (route, abfahrt) ->
                z.fehler = ""
                z.geprueft = System.currentTimeMillis()
                z.vomStandort = standort != null
                when {
                    // Schon an der Arbeit oder kurz davor: nichts mehr sagen.
                    standort != null && route.meter in 1 until 1500 -> z.erledigt = true
                    z.gemeldet -> {
                        if (route.sekunden - z.gemeldetSekunden > 5 * 60) {
                            val an = jetzt.plusSeconds(route.sekunden.toLong())
                            sende(app, "Fahrzeit länger geworden", "Jetzt ${route.minuten} Minuten (+${(route.sekunden - z.gemeldetSekunden) / 60}). Fahre sofort los, Ankunft etwa ${an.format(UHR)} Uhr.",
                                "Achtung Frank, die Fahrzeit ist auf ${route.minuten} Minuten gestiegen. Fahre jetzt los, dann bist du um ${gesprochen(an)} da.", navigation = true)
                        }
                        z.erledigt = true
                    }
                    else -> {
                        z.abfahrt = abfahrt.toInstant().toEpochMilli()
                        z.sekunden = route.sekunden
                        if (!jetzt.isBefore(abfahrt.minusMinutes(vorlauf).minusSeconds(90)) || z.pruefungen >= 10) {
                            melde(app, f, route, abfahrt, jetzt, veraltet = false)
                            z.gemeldet = true
                            z.gemeldetSekunden = route.sekunden
                            // Ist die Abfahrt schon jetzt fällig, gibt es danach nichts mehr nachzuprüfen.
                            if (!abfahrt.isAfter(jetzt.plusMinutes(2))) z.erledigt = true
                        }
                    }
                }
            }
            sichere(app, z)
        } finally {
            laeuft.set(false)
            stelle(app)
        }
    }

    /** „Heute still“ aus der Benachrichtigung: Vorlesen beenden, für diesen Dienst nichts mehr melden. */
    fun still(context: Context) {
        val app = context.applicationContext
        naechste(app)?.let { f -> zustand(app, f.tag).also { it.erledigt = true; sichere(app, it) } }
        app.getSystemService(NotificationManager::class.java).cancel(HINWEIS_ID)
        runCatching { Vorleser.aktuell()?.stopp() }
        stelle(app)
    }

    private fun melde(context: Context, f: Fahrt, route: Route, abfahrt: ZonedDateTime, jetzt: ZonedDateTime, veraltet: Boolean) {
        val rest = Duration.between(jetzt, abfahrt).plusSeconds(30).toMinutes()
        val stand = if (veraltet) " (letzter Stand, gerade nicht abrufbar)" else ""
        if (rest >= 1) {
            val wann = if (rest == 1L) "einer Minute" else "$rest Minuten"
            sende(
                context, "In $wann losfahren",
                "Fahre um ${abfahrt.format(UHR)} Uhr los, damit du um ${f.ankunft.format(UHR)} Uhr auf Arbeit bist. Fahrzeit ${route.minuten} Minuten$stand. Jetzt ist es ${jetzt.format(UHR)} Uhr.",
                "Frank, fahre in $wann los, um ${gesprochen(abfahrt)}, damit du um ${gesprochen(f.ankunft)} auf Arbeit bist. Die Fahrt dauert heute ${route.minuten} Minuten. Jetzt ist es ${gesprochen(jetzt)}.",
                navigation = true,
            )
        } else {
            val an = jetzt.plusSeconds(route.sekunden.toLong())
            val spaet = Duration.between(f.ankunft, an).toMinutes()
            sende(
                context, "Jetzt losfahren",
                "Die Fahrt dauert ${route.minuten} Minuten$stand. " + (if (spaet > 0) "Ankunft frühestens ${an.format(UHR)} Uhr, das sind $spaet Minuten nach ${f.ankunft.format(UHR)} Uhr." else "Ankunft etwa ${an.format(UHR)} Uhr.") + " Jetzt ist es ${jetzt.format(UHR)} Uhr.",
                "Frank, fahre jetzt sofort los. Die Fahrt dauert ${route.minuten} Minuten, " + (if (spaet > 0) "Ankunft frühestens ${gesprochen(an)}, das sind $spaet Minuten zu spät." else "du bist um ${gesprochen(an)} da."),
                navigation = true,
            )
        }
    }

    /** Eine Beispiel-Meldung zum Ausprobieren von Stimme und Lautstärke; an der Planung ändert sie nichts. */
    fun probe(context: Context) {
        val jetzt = ZonedDateTime.now()
        val abfahrt = jetzt.plusMinutes(10)
        sende(
            context.applicationContext, "Probe: In 10 Minuten losfahren",
            "Fahre um ${abfahrt.format(UHR)} Uhr los, damit du pünktlich auf Arbeit bist. So klingt die Losfahr-Meldung.",
            "Frank, das ist eine Probe. Fahre in 10 Minuten los, um ${gesprochen(abfahrt)}, damit du pünktlich auf Arbeit bist.",
        )
    }

    /** Uhrzeit so, wie eine Stimme sie natürlich spricht: „16 Uhr 10“, „17 Uhr“. */
    private fun gesprochen(t: ZonedDateTime): String = if (t.minute == 0) "${t.hour} Uhr" else "${t.hour} Uhr ${t.minute}"

    private fun sende(context: Context, titel: String, text: String, sprechtext: String, navigation: Boolean = false) {
        val e = Einstellungen.get(context)
        val meldungen = context.getSystemService(NotificationManager::class.java)
        // Wichtigkeit hoch von Anfang an: Sie lässt sich später nicht mehr anheben.
        meldungen.createNotificationChannel(NotificationChannel(KANAL, "Losfahren", NotificationManager.IMPORTANCE_HIGH).apply { description = "Meldet, wann du zur Arbeit losfahren musst." })
        val unveraenderlich = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val bauer = NotificationCompat.Builder(context, KANAL).setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle(titel).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), unveraenderlich))
        if (navigation) {
            val ziel = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(e.adresseArbeit) + "&mode=d")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            bauer.addAction(R.drawable.ic_stat_jarvis, "Navigation starten", PendingIntent.getActivity(context, 81, ziel, unveraenderlich))
            bauer.addAction(R.drawable.ic_stat_jarvis, "Heute still", PendingIntent.getBroadcast(context, 82, Intent(context, AbfahrtEmpfaenger::class.java).setAction(AKTION_STILL), unveraenderlich))
        }
        runCatching { meldungen.notify(HINWEIS_ID, bauer.build()) }.onFailure { Log.w(TAG, "Benachrichtigung nicht möglich", it) }
        if (e.abfahrtVorlesen) {
            // Eigene Quelle je Meldung: Dieselbe Quelle ein zweites Mal würde das Vorlesen anhalten statt starten.
            CoroutineScope(Dispatchers.Main).launch {
                // Bei gesperrtem Handy darf das Gerät nicht einschlafen, bevor die Stimme fertig ist (sie wird erst aus dem Netz geholt).
                val wach = context.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:abfahrt-stimme")
                runCatching { wach.acquire(120_000L) }
                val ton = context.getSystemService(AudioManager::class.java)
                val vorher = ton.getStreamVolume(AudioManager.STREAM_MUSIC)
                val hoechste = ton.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val laut = (hoechste * e.abfahrtLautstaerke / 100.0).roundToInt().coerceIn(1, hoechste)
                try {
                    // Die Meldung hat ihre eigene Lautstärke, unabhängig davon, wie leise das Handy gerade steht.
                    runCatching { ton.setStreamVolume(AudioManager.STREAM_MUSIC, laut, 0) }.onFailure { Log.w(TAG, "Lautstärke ließ sich nicht setzen", it) }
                    val quelle = "abfahrt:" + System.currentTimeMillis()
                    val vorleser = Vorleser.hole(context, SecureSettings(context))
                    vorleser.sprich(quelle, "Losfahren", sprechtext)
                    withTimeoutOrNull(110_000L) { vorleser.stand.first { it.quelle != quelle || it.zustand == VorleseZustand.AUS } }
                } catch (f: Exception) {
                    Log.w(TAG, "Vorlesen nicht möglich", f)
                } finally {
                    // Zurück auf den Stand von vorher, außer Frank hat inzwischen selbst nachgeregelt.
                    runCatching { if (ton.getStreamVolume(AudioManager.STREAM_MUSIC) == laut) ton.setStreamVolume(AudioManager.STREAM_MUSIC, vorher, 0) }
                    runCatching { if (wach.isHeld) wach.release() }
                }
            }
        }
    }

    /** Ein Satz zum Stand, für die Einstellungen und für die Werkzeuge. */
    fun status(context: Context): String {
        val e = Einstellungen.get(context)
        if (!e.abfahrtAn) return "Ausgeschaltet."
        if (e.mapsSchluessel.isBlank()) return "Der Google-Maps-Schlüssel fehlt."
        val jetzt = ZonedDateTime.now()
        val f = naechste(context, jetzt) ?: return "Kein Dienst in den nächsten 14 Tagen im Kalender (oder Kalender nicht lesbar)."
        val z = zustand(context, f.tag)
        val tag = tagName(f.tag, jetzt.toLocalDate())
        val abfahrt = z.abfahrtZeit()
        return buildString {
            if (abfahrt != null) {
                append("Abfahrt ").append(tag).append(" um ").append(abfahrt.format(UHR)).append(" Uhr für ").append(f.dienst).append(", Ankunft ").append(f.ankunft.format(UHR)).append(" Uhr. ")
                append(if (z.gemeldet) "Meldung ist raus. " else "Meldung um ${abfahrt.minusMinutes(e.abfahrtVorlauf.toLong()).format(UHR)} Uhr. ")
                append("Fahrzeit ").append((z.sekunden + 59) / 60).append(" Minuten ").append(if (z.vomStandort) "vom Standort" else "von zu Hause")
                append(" (Stand ").append(Instant.ofEpochMilli(z.geprueft).atZone(jetzt.zone).format(UHR)).append(" Uhr).")
            } else {
                append("Nächste Fahrt: ").append(tag).append(", ").append(f.dienst).append(", Ankunft ").append(f.ankunft.format(UHR)).append(" Uhr. ")
                append("Jarvis rechnet ab ").append(f.ankunft.minusMinutes(VORPRUEFUNG).format(UHR)).append(" Uhr.")
            }
            if (z.fehler.isNotBlank()) append(" Letzter Versuch: ").append(z.fehler)
        }
    }

    private fun tagName(tag: LocalDate, heute: LocalDate): String = when (java.time.temporal.ChronoUnit.DAYS.between(heute, tag)) {
        0L -> "heute"; 1L -> "morgen"
        else -> tag.format(DateTimeFormatter.ofPattern("EE d.M.", Locale.GERMAN))
    }
}

/** Der Wecker ist da: den Dienst mit der Prüfung beauftragen. Außerdem der Knopf „Heute still“ der Benachrichtigung. */
class AbfahrtEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Abfahrt.AKTION -> JarvisDienst.abfahrtPruefen(context)
            Abfahrt.AKTION_STILL -> Abfahrt.still(context)
        }
    }
}
