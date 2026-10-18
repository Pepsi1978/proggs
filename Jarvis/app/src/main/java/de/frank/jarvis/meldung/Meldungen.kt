package de.frank.jarvis.meldung

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.speech.Ansage
import de.frank.jarvis.speech.GespraechsFokus
import de.frank.jarvis.speech.StimmWahl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalTime

/**
 * Eine Benachrichtigung von Jarvis, wie sie unter Einstellungen → Benachrichtigungen steht. Was eine Art kann
 * (abschalten, Ton, Vorlesen, Wecker, minimieren), legen die Schalter fest; die Oberfläche zeigt nur diese.
 * [alt] ordnet einem Feld den Schlüssel zu, unter dem es schon vor dem Bereich gespeichert war: So bleibt Franks Wahl erhalten.
 */
class Meldungsart(
    val id: String,
    val titel: String,
    val wozu: String,
    val wann: String,
    val gruppe: String,
    /** Android-Kanal für „In Android öffnen“; null = keiner. */
    val kanal: String?,
    /** Android verlangt sie, solange der zugehörige Dienst läuft: kein Aus-Schalter. */
    val pflicht: Boolean = false,
    val schaltbar: Boolean = true,
    val ton: Boolean = false,
    val vorlesen: Boolean = false,
    val wecker: Boolean = false,
    val minimierbar: Boolean = false,
    /** Hält sich an die Ruhezeit (still, ohne Vorlesen und Wecker). */
    val ruhezeit: Boolean = false,
    /** An heißt hier: Jarvis spricht (Update-Ansagen); dann gehören Stimme und Lautstärke zum Schalter selbst. */
    val sprichtImmer: Boolean = false,
    /** Zeitkritisch (Losfahren, Timer): auch im Live-Gespräch mit Ton und Wecker; Vorlesen wartet höchstens 90 Sekunden, sonst wäre es veraltet. */
    val dringend: Boolean = false,
    val vorlesenStandard: Boolean = false,
    /** 0 = so laut, wie das Handy steht: Ohne eigene Wahl stellt Jarvis die Lautstärke nicht um. */
    val lautStandard: Int = 0,
    val alt: Map<String, String> = emptyMap(),
) {
    fun schluessel(feld: String) = alt[feld] ?: "meldung_${id}_$feld"
}

/** Die Einstellungen einer [Meldungsart], gelesen und geschrieben über [Einstellungen]. */
class MeldungsProfil(private val e: Einstellungen, val art: Meldungsart) {
    var an: Boolean get() = e.wahr(art.schluessel("an"), true); set(v) = e.setzeWahr(art.schluessel("an"), v)
    var ton: Boolean get() = e.wahr(art.schluessel("ton"), true); set(v) = e.setzeWahr(art.schluessel("ton"), v)
    var vorlesen: Boolean get() = e.wahr(art.schluessel("vorlesen"), art.vorlesenStandard); set(v) = e.setzeWahr(art.schluessel("vorlesen"), v)
    /** Stimm-Kennung aus dem TtsCatalog; leer = wie Jarvis. */
    var stimme: String get() = e.wort(art.schluessel("stimme"), ""); set(v) = e.setzeWort(art.schluessel("stimme"), v)
    /** 0 = wie Jarvis. */
    var tempo: Float get() = e.komma(art.schluessel("tempo"), 0f); set(v) = e.setzeKomma(art.schluessel("tempo"), v)
    /** Prozent der höchsten Medienlautstärke; 0 = so laut, wie das Handy gerade steht. */
    var lautstaerke: Int get() = e.ganz(art.schluessel("lautstaerke"), art.lautStandard).coerceIn(0, 100); set(v) = e.setzeGanz(art.schluessel("lautstaerke"), v.coerceIn(0, 100))
    var wecker: Boolean get() = e.wahr(art.schluessel("wecker"), false); set(v) = e.setzeWahr(art.schluessel("wecker"), v)
    var minimiert: Boolean get() = e.wahr(art.schluessel("minimiert"), false); set(v) = e.setzeWahr(art.schluessel("minimiert"), v)

    val stimmWahl get() = StimmWahl(stimme.ifBlank { null }, tempo.takeIf { it > 0f })

    /** Weckton als Adresse aus dem Android-Tonwähler; leer = Standard-Weckton des Handys. */
    var weckton: String get() = e.wort(art.schluessel("weckton"), ""); set(v) = e.setzeWort(art.schluessel("weckton"), v)
    /** So viele Sekunden klingelt der Wecker, dann verstummt er und Jarvis liest vor; 0 = bis zum Antippen oder Wegwischen. */
    var wecktonSekunden: Int get() = e.ganz(art.schluessel("weckton_sekunden"), 10).coerceIn(0, 300); set(v) = e.setzeGanz(art.schluessel("weckton_sekunden"), v.coerceIn(0, 300))

    /** Klingelt sie wirklich wie ein Wecker? „Lautlos“ schaltet den Wecker mit ab, auch wenn er noch gewählt ist. */
    val weckerWirksam get() = art.wecker && wecker && !(art.ton && !ton)
}

object Meldungsarten {
    const val ERGEBNISSE = "Ergebnisse und Rückmeldungen"
    const val ALLTAG = "Alltag"
    const val WECKWORT = "Weckwort"
    const val DAUER = "Daueranzeigen"

    val AGENT = Meldungsart(
        "agent", "Agent fertig", "Meldet, wenn ein Agent seinen Bericht in die Ablage gelegt hat oder abgebrochen ist.",
        "Sobald ein Agent fertig ist.", ERGEBNISSE, "jarvis_ergebnisse",
        ton = true, vorlesen = true, wecker = true, ruhezeit = true, alt = mapOf("vorlesen" to "ergebnisse_vorlesen"),
    )
    val PROGRAMMIEREN = Meldungsart(
        "programmieren", "Programmieraufträge", "Meldet Rückfragen, fertige Aufträge und Probleme beim Programmieren.",
        "Bei Rückfrage, Fertig oder Problem eines Programmierauftrags.", ERGEBNISSE, "jarvis_ergebnisse",
        ton = true, vorlesen = true, wecker = true, ruhezeit = true,
    )
    val ABLAGE = Meldungsart(
        "ablage", "Ablage", "Meldet, wenn ein Bild, ein PDF oder ein Download in der Ablage fertig ist oder scheitert.",
        "Wenn eine Arbeit in der Ablage fertig wird.", ERGEBNISSE, "jarvis_ergebnisse",
        ton = true, vorlesen = true, wecker = true, ruhezeit = true,
    )
    val GEMINI = Meldungsart(
        "gemini", "Gemini-Schlüssel gewechselt", "Sagt Bescheid, wenn das kostenlose Gemini-Kontingent leer ist und Jarvis mit dem bezahlten Schlüssel weiterspricht.",
        "Nur im Live-Gespräch, deshalb immer lautlos.", ERGEBNISSE, "jarvis_ergebnisse",
    )
    val ABFAHRT = Meldungsart(
        "abfahrt", "Losfahren", "Sagt dir an Arbeitstagen, wann du zur Arbeit losfahren musst, mit Navigation per Tipp.",
        "Vor jedem Dienst, rechtzeitig vor der nötigen Abfahrt.", ALLTAG, "jarvis_abfahrt",
        ton = true, vorlesen = true, wecker = true, vorlesenStandard = true, lautStandard = 100, dringend = true,
        alt = mapOf("an" to "abfahrt_an", "vorlesen" to "abfahrt_vorlesen", "lautstaerke" to "abfahrt_lautstaerke"),
    )
    val TIMER = Meldungsart(
        "timer", "Timer abgelaufen", "Klingelt, wenn ein Timer abgelaufen ist.",
        // Der hörbare Timer-Kanal („jarvis-timer“), nicht der stumme des Klingel-Dienstes.
        "Wenn ein Timer abläuft.", ALLTAG, "jarvis-timer",
        schaltbar = false, vorlesen = true, dringend = true,
    )
    val STOPPUHR = Meldungsart(
        "stoppuhr", "Stoppuhr", "Zeigt die laufende Stoppuhr mit mitlaufender Zeit.",
        "Solange eine Stoppuhr läuft.", ALLTAG, "jarvis-stoppuhr",
    )
    val ANSAGE = Meldungsart(
        "ansage", "Update-Ansagen", "Jarvis sagt an, wenn in UpdateStation ein Update fertig geladen bereitliegt.",
        "Wenn UpdateStation ein neues Update meldet.", ALLTAG, null,
        sprichtImmer = true, ruhezeit = true, lautStandard = 100,
        alt = mapOf("an" to "ansage_vorlesen", "stimme" to "ansage_stimme", "tempo" to "ansage_tempo", "lautstaerke" to "ansage_lautstaerke"),
    )
    val WECKWORT_HOERT = Meldungsart(
        "weckwort", "Jarvis hört zu", "Zeigt, dass Jarvis auf dein Weckwort lauscht, auch bei gesperrtem Handy.",
        "Immer, solange das Weckwort an ist.", WECKWORT, "jarvis_weckwort",
        pflicht = true, schaltbar = false, minimierbar = true,
    )
    val RUF = Meldungsart(
        "ruf", "Weckwort-Ruf", "Zum Antippen nach dem Weckwort, wenn Jarvis nicht von selbst nach vorn kommen darf.",
        "Nach dem Weckwort, solange „Über anderen Apps einblenden“ fehlt.", WECKWORT, "jarvis_weckwort_ruf",
        ton = true,
    )
    val WECKWORT_AUS = Meldungsart(
        "weckwort_aus", "Weckwort ist aus", "Erinnert dich, das Weckwort nach einem Neustart mit einem Tipp wieder einzuschalten.",
        "Nach Neustart oder Update, wenn Android den Mikrofon-Dienst nicht starten ließ.", WECKWORT, "jarvis_weckwort",
    )
    val BEREITSCHAFT = Meldungsart(
        "bereitschaft", "Jarvis-Bereitschaft", "Zeigt, dass Jarvis im Hintergrund läuft und für ChatGPT erreichbar ist.",
        "Immer, solange die Bereitschaft an ist.", DAUER, "jarvis_dienst",
        pflicht = true, schaltbar = false, minimierbar = true,
    )
    val AUFNAHME = Meldungsart(
        "aufnahme", "Aufnahme läuft", "Hält deine Aufnahme am Leben, wenn du währenddessen in eine andere App wechselst.",
        "Nur während einer von Hand gestarteten Aufnahme.", DAUER, "jarvis_aufnahme",
        pflicht = true, schaltbar = false,
    )
    val VORLESEN = Meldungsart(
        "vorlesen", "Vorlesen", "Zeigt, was Jarvis gerade vorliest, mit Pause, Weiter und Stopp.",
        "Nur während Jarvis vorliest.", DAUER, "vorlesen",
        pflicht = true, schaltbar = false,
    )

    val alle = listOf(AGENT, PROGRAMMIEREN, ABLAGE, GEMINI, ABFAHRT, TIMER, STOPPUHR, ANSAGE, WECKWORT_HOERT, RUF, WECKWORT_AUS, BEREITSCHAFT, AUFNAHME, VORLESEN)
    val gruppen = listOf(ERGEBNISSE, ALLTAG, WECKWORT, DAUER)
}

/** Zeigt Benachrichtigungen so, wie Frank sie unter Einstellungen → Benachrichtigungen eingestellt hat. */
object Meldungen {
    /** Eigener Kanal für „Wie ein Wecker“: Weckton, auch bei „Nicht stören“; die Wichtigkeit lässt sich später nicht mehr anheben. */
    const val KANAL_WECKER = "jarvis_meldung_wecker_ton"
    private val nebenher = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun profil(context: Context, art: Meldungsart) = MeldungsProfil(Einstellungen.get(context), art)

    /** Ist die Meldung eingeschaltet? Nicht schaltbare sind immer an. */
    fun an(context: Context, art: Meldungsart) = !art.schaltbar || profil(context, art).an

    /** Gilt gerade die Ruhezeit für diese Art? */
    fun inRuhe(context: Context, art: Meldungsart): Boolean {
        val e = Einstellungen.get(context)
        if (!art.ruhezeit || !e.meldungRuheAn) return false
        val von = zeit(e.meldungRuheVon) ?: return false
        val bis = zeit(e.meldungRuheBis) ?: return false
        val jetzt = LocalTime.now()
        return if (von <= bis) jetzt >= von && jetzt < bis else jetzt >= von || jetzt < bis
    }

    private fun zeit(t: String) = runCatching { LocalTime.parse(t) }.getOrNull()

    /** Soll Jarvis diese Art jetzt von selbst vorlesen? */
    fun vorlesenAn(context: Context, art: Meldungsart): Boolean {
        val p = profil(context, art)
        return art.vorlesen && an(context, art) && p.vorlesen && !inRuhe(context, art)
    }

    /**
     * Zeigt die Meldung, wenn sie eingeschaltet ist: lautlos nach Wahl, in der Ruhezeit und im Gespräch; wie ein Wecker
     * auf eigenem Kanal mit Dauerton; danach auf Wunsch vorgelesen. [laut] = false: nur still aktualisieren.
     * [sprechtext] null = nicht vorlesen (die Aufrufstelle liest selbst vor). false = abgeschaltet, nichts gezeigt.
     */
    fun zeige(
        context: Context, art: Meldungsart, nummer: Int, bauer: NotificationCompat.Builder,
        titel: String, sprechtext: String?, laut: Boolean = true, probe: Boolean = false,
        /** false = auch im Gespräch mit Ton (Rückfragen beim Programmieren sollen nicht liegen bleiben). */
        imGespraechStill: Boolean = true,
    ): Boolean {
        val app = context.applicationContext
        val e = Einstellungen.get(app)
        val p = profil(app, art)
        if (!probe && !an(app, art)) return false
        val ruhe = !probe && inRuhe(app, art)
        val gespraech = GespraechsFokus.stilleHinweise && !art.dringend
        // Lautlos gewählt heißt auch: kein Wecker, selbst wenn er noch eingeschaltet ist.
        val tonAus = art.ton && !p.ton
        val klingeln = laut && p.weckerWirksam && !ruhe && !gespraech
        val still = !laut || ruhe || (gespraech && imGespraechStill) || tonAus
        if (klingeln) {
            weckerKanalAnlegen(app)
            bauer.setChannelId(KANAL_WECKER).setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
        }
        bauer.setSilent(still && !klingeln)
        // SECRET: auf dem Sperrbildschirm gar nicht, erst nach dem Entsperren (PRIVATE wäre der Standard und hülfe nicht).
        // Ein klingelnder Wecker muss sich aber gesperrt abstellen lassen: dann PRIVATE (sichtbar, Inhalt verborgen).
        if (e.meldungInhaltVerbergen) bauer.setVisibility(if (klingeln) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_SECRET)
        runCatching { app.getSystemService(NotificationManager::class.java).notify(nummer, bauer.build()) }
        // Den Weckton spielt Jarvis selbst (eigener Ton und eigene Dauer je Meldung, der Kanal ist stumm): Er läuft die gewählten
        // Sekunden oder bis Frank die Meldung antippt oder wegwischt, dann verstummt er, die Meldung bleibt stehen, und Jarvis liest vor.
        if (klingeln) klingle(app, nummer, p)
        if (laut && sprechtext != null && art.vorlesen && p.vorlesen && !ruhe)
            vorlesenSpaeter(app, art, titel, sprechtext, nummer.toString(), solange = if (klingeln) ({ WeckerTon.klingelt(nummer) }) else null)
        return true
    }

    /** Ohne Dauer („bis zum Antippen“) klingelt der Wecker höchstens so lange. */
    private const val WECKTON_HOECHSTENS_MS = 10 * 60_000L

    private fun klingle(app: Context, nummer: Int, p: MeldungsProfil) {
        if (!WeckerTon.starte(app, nummer, p.weckton)) return
        val sekunden = p.wecktonSekunden
        nebenher.launch {
            val bis = System.currentTimeMillis() + if (sekunden > 0) sekunden * 1_000L else WECKTON_HOECHSTENS_MS
            // Die Meldung braucht einen Moment, bis Android sie als sichtbar führt.
            delay(1_500L)
            while (WeckerTon.klingelt(nummer) && sichtbar(app, nummer) && System.currentTimeMillis() < bis) delay(500L)
            WeckerTon.stopp(nummer)
        }
    }

    private fun sichtbar(context: Context, nummer: Int) =
        runCatching { context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == nummer } }.getOrDefault(false)

    /** Wartendes Vorlesen je Art und Meldung („art:schlüssel“), damit „Heute still“ oder ein Abschalten es noch verhindern kann. */
    private val wartend = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    /**
     * Liest im Hintergrund vor, mit Stimme, Tempo und Lautstärke dieser Art; wartet ein laufendes Gespräch ab. Je [schluessel]
     * gilt nur das neueste Vorlesen (eine aktualisierte Losfahr-Meldung ersetzt die alte), verschiedene Meldungen warten nebeneinander.
     */
    fun vorlesenSpaeter(context: Context, art: Meldungsart, titel: String, text: String, schluessel: String = "", solange: (() -> Boolean)? = null, ausdruecklich: Boolean = false) {
        val app = context.applicationContext
        nebenher.launch { vorlesenWartend(app, art, titel, text, schluessel, solange, ausdruecklich) }
    }

    /** Wie [vorlesenSpaeter], aber kehrt erst nach dem Start der Stimme (oder dem Verwerfen) zurück; bleibt dabei abbrechbar. */
    suspend fun vorlesenWartend(context: Context, art: Meldungsart, titel: String, text: String, schluessel: String = "", solange: (() -> Boolean)? = null, ausdruecklich: Boolean = false) = coroutineScope {
        val name = "${art.id}:$schluessel"
        val job = launch(start = CoroutineStart.LAZY) { sprich(context, art, titel, text, solange, ausdruecklich) }
        wartend.put(name, job)?.cancel()
        job.invokeOnCompletion { wartend.remove(name, job) }
        job.start()
        job.join()
    }

    /** Noch nicht gesprochenes Vorlesen dieser Art verwerfen (etwa nach „Heute still“); mit [schluessel] nur das einer Meldung. */
    fun vorlesenAbbrechen(art: Meldungsart, schluessel: String? = null) {
        wartend.keys.filter { if (schluessel == null) it.startsWith("${art.id}:") else it == "${art.id}:$schluessel" }.forEach { wartend.remove(it)?.cancel() }
    }

    /**
     * Wartet ein Gespräch (und [solange], etwa einen klingelnden Wecker) ab und spricht dann. Der Live-Sprachmodus hat Vorrang:
     * höchstens 15 Minuten warten (dringend 90 Sekunden), dann verwerfen. Danach gilt der Stand von dann: ausgeschaltet,
     * Vorlesen abgewählt oder inzwischen Ruhezeit heißt still. [ausdruecklich] = Frank hat das Vorlesen eigens verlangt.
     */
    private suspend fun sprich(context: Context, art: Meldungsart, titel: String, text: String, solange: (() -> Boolean)?, ausdruecklich: Boolean) {
        // Erst den Weckton zu Ende klingeln lassen (er endet selbst, spätestens nach seiner Höchstdauer), dann das Gespräch abwarten.
        if (solange != null) withTimeoutOrNull(WECKTON_HOECHSTENS_MS + 5_000L) { while (solange()) delay(500L) }
        val frei = withTimeoutOrNull(if (art.dringend) 90_000L else 15 * 60_000L) {
            while (GespraechsFokus.stilleHinweise) delay(2_000L)
            true
        }
        if (frei != true) return
        val p = profil(context, art)
        if (!ausdruecklich && (!an(context, art) || inRuhe(context, art) || (art.vorlesen && !p.vorlesen))) return
        Ansage.sprich(context, art.id, titel, text, p.lautstaerke, p.stimmWahl)
    }

    /** Probe aus den Einstellungen: zeigt (und liest) eine Beispielmeldung mit den aktuellen Einstellungen. */
    fun probe(context: Context, art: Meldungsart) {
        val app = context.applicationContext
        when (art) {
            Meldungsarten.ABFAHRT -> de.frank.jarvis.fahrt.Abfahrt.probe(app)
            Meldungsarten.TIMER -> de.frank.jarvis.timer.Timer.starten(app, 5_000L, "Probe-Timer")
            Meldungsarten.ANSAGE -> Ansage.ansage(app, "UpdateStation", "Ein Update von Jarvis ist bereit in UpdateStation.")
            else -> {
                de.frank.jarvis.dienst.JarvisDienst.kanalAnlegen(app)
                de.frank.jarvis.weckwort.WeckwortDienst.kanaeleAnlegen(app)
                val text = "So sieht die Meldung „${art.titel}“ aus. ${art.wozu}"
                val bauer = NotificationCompat.Builder(app, art.kanal ?: "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis)
                    .setContentTitle("Probe: ${art.titel}").setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                zeige(app, art, 4_400 + Meldungsarten.alle.indexOf(art), bauer, "Probe: ${art.titel}", "Probe. ${art.wozu}", probe = true)
            }
        }
    }

    /** Folgeaktionen nach einer Änderung, für den Bildschirm und für Jarvis' Werkzeug gleich. */
    fun folgen(context: Context, art: Meldungsart) {
        val app = context.applicationContext
        when (art) {
            Meldungsarten.STOPPUHR -> de.frank.jarvis.timer.Stoppuhr.meldungErneuern(app)
            Meldungsarten.BEREITSCHAFT -> de.frank.jarvis.dienst.JarvisDienst.hinweisErneuern(app)
            Meldungsarten.WECKWORT_HOERT -> de.frank.jarvis.weckwort.WeckwortDienst.hinweisErneuern(app)
            Meldungsarten.ABFAHRT -> { Einstellungen.get(app).abfahrtZustand = ""; de.frank.jarvis.fahrt.Abfahrt.stelle(app) }
            else -> {}
        }
        if (art.schaltbar && !profil(app, art).an) vorlesenAbbrechen(art)
    }

    fun weckerKanalAnlegen(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        // Der frühere Kanal spielte den Ton selbst (nicht wählbar, ein Kanalton lässt sich nachträglich nicht ändern).
        runCatching { nm.deleteNotificationChannel(KANAL_WECKER_ALT) }
        nm.createNotificationChannel(
            NotificationChannel(KANAL_WECKER, "Wie ein Wecker", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Für Meldungen, die wie ein Wecker klingeln. Den Weckton und wie lange er klingelt stellst du in Jarvis je Meldung ein."
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 700, 500, 700)
                setBypassDnd(true)
            },
        )
    }
    private const val KANAL_WECKER_ALT = "jarvis_meldung_wecker"
}

/**
 * Spielt den Weckton einer Meldung selbst, als Wecker-Ton (USAGE_ALARM) in Schleife: so lässt sich der Ton je Meldung
 * wählen und nach der eingestellten Dauer beenden, ohne die Meldung zurückzuziehen. Je Meldungsnummer höchstens ein Ton.
 */
object WeckerTon {
    private const val PROBE = -4_711
    private val spieler = java.util.concurrent.ConcurrentHashMap<Int, android.media.MediaPlayer>()
    private val attribute = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    fun klingelt(nummer: Int) = spieler.containsKey(nummer)

    /** [ton] leer = Standard-Weckton; ist der gewählte Ton nicht mehr da, der Standard. false = gar kein Ton abspielbar. */
    fun starte(context: Context, nummer: Int, ton: String): Boolean {
        stopp(nummer)
        val app = context.applicationContext
        val toene = listOfNotNull(
            ton.takeIf { it.isNotBlank() }?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() },
            RingtoneManager.getActualDefaultRingtoneUri(app, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        for (uri in toene) {
            val p = android.media.MediaPlayer()
            val ok = runCatching {
                p.setAudioAttributes(attribute)
                p.setDataSource(app, uri)
                p.isLooping = true
                p.setWakeMode(app, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                p.prepare()
                p.start()
            }.isSuccess
            if (ok) { spieler.put(nummer, p)?.let { alt -> runCatching { alt.release() } }; return true }
            runCatching { p.release() }
        }
        return false
    }

    fun stopp(nummer: Int) {
        spieler.remove(nummer)?.let { runCatching { it.stop() }; runCatching { it.release() } }
    }

    /** Zum Anhören in den Einstellungen: vier Sekunden. */
    fun probe(context: Context, ton: String) {
        if (starte(context, PROBE, ton)) android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ stopp(PROBE) }, 4_000L)
    }

    fun name(context: Context, ton: String): String =
        if (ton.isBlank()) "Standard-Weckton"
        else runCatching { RingtoneManager.getRingtone(context, android.net.Uri.parse(ton))?.getTitle(context) }.getOrNull() ?: "Eigener Ton"

    /** Die Wecktöne des Handys (Adresse, Name), für die Auswahl per Sprache. */
    fun toene(context: Context): List<Pair<String, String>> = runCatching {
        val m = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val c = m.cursor
        buildList { while (c.moveToNext()) add(m.getRingtoneUri(c.position).toString() to c.getString(RingtoneManager.TITLE_COLUMN_INDEX)) }
    }.getOrDefault(emptyList())
}
