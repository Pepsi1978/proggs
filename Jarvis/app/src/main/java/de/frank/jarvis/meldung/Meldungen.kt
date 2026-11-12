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
import kotlinx.coroutines.sync.withLock
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
    val ton: Boolean = !pflicht,
    val vorlesen: Boolean = !pflicht,
    val wecker: Boolean = !pflicht,
    val minimierbar: Boolean = false,
    /** Hält sich an die Ruhezeit (still, ohne Vorlesen und Wecker). */
    val ruhezeit: Boolean = false,
    /** An heißt hier: Jarvis spricht (Update-Ansagen); dann gehören Stimme und Lautstärke zum Schalter selbst. */
    val sprichtImmer: Boolean = false,
    /** Zeitkritisch (Losfahren, Timer): auch im Live-Gespräch mit Ton und Wecker; Vorlesen wartet höchstens 90 Sekunden, sonst wäre es veraltet. */
    val dringend: Boolean = false,
    val vorlesenStandard: Boolean = false,
    /** Der vorgelesene Satz kommt nicht von woanders (Bericht, Route, Update), sondern ist fest: dann lässt er sich selbst schreiben. Leer = [eigenerTextStandard]. */
    val eigenerTextStandard: String? = null,
    /** 0 = so laut, wie das Handy steht: Ohne eigene Wahl stellt Jarvis die Lautstärke nicht um. */
    val lautStandard: Int = 0,
    /** Erinnerungen behalten ihre beim Anlegen gewählte Form. */
    val formFest: Boolean = false,
    val weckerStandard: Boolean = false,
    val tonStandard: Boolean = true,
    val alt: Map<String, String> = emptyMap(),
) {
    fun schluessel(feld: String) = alt[feld] ?: "meldung_${id}_$feld"
}

/** Die Einstellungen einer [Meldungsart], gelesen und geschrieben über [Einstellungen]. */
class MeldungsProfil(private val e: Einstellungen, val art: Meldungsart) {
    var an: Boolean get() = e.wahr(art.schluessel("an"), true); set(v) = e.setzeWahr(art.schluessel("an"), v)
    var ton: Boolean get() = e.wahr(art.schluessel("ton"), art.tonStandard); set(v) = e.setzeWahr(art.schluessel("ton"), v)
    var vorlesen: Boolean get() = e.wahr(art.schluessel("vorlesen"), art.vorlesenStandard); set(v) = e.setzeWahr(art.schluessel("vorlesen"), v)
    /** Stimm-Kennung aus dem TtsCatalog; leer = wie Jarvis. */
    var stimme: String get() = e.wort(art.schluessel("stimme"), ""); set(v) = e.setzeWort(art.schluessel("stimme"), v)
    /** 0 = wie Jarvis. */
    var tempo: Float get() = e.komma(art.schluessel("tempo"), 0f); set(v) = e.setzeKomma(art.schluessel("tempo"), v)
    /** Prozent der höchsten Medienlautstärke; 0 = so laut, wie das Handy gerade steht. */
    var lautstaerke: Int get() = e.ganz(art.schluessel("lautstaerke"), art.lautStandard).coerceIn(0, 100); set(v) = e.setzeGanz(art.schluessel("lautstaerke"), v.coerceIn(0, 100))
    var wecker: Boolean get() = if (art.formFest) art.weckerStandard else e.wahr(art.schluessel("wecker"), art.weckerStandard); set(v) { if (!art.formFest) e.setzeWahr(art.schluessel("wecker"), v) }
    var minimiert: Boolean get() = e.wahr(art.schluessel("minimiert"), false); set(v) = e.setzeWahr(art.schluessel("minimiert"), v)

    /** Selbst geschriebener Vorlesetext; „{name}“ steht für den Namen (etwa des Timers). Leer = der Standardsatz der Art. */
    var vorlesetext: String get() = e.wort(art.schluessel("vorlesetext"), ""); set(v) = e.setzeWort(art.schluessel("vorlesetext"), v)

    /** Der Satz, den Jarvis vorliest, mit [name] für „{name}“. */
    fun sprechtext(name: String) = vorlesetext.ifBlank { art.eigenerTextStandard.orEmpty() }.replace("{name}", name, ignoreCase = true)

    val stimmWahl get() = StimmWahl(stimme.ifBlank { null }, tempo.takeIf { it > 0f })

    /** Weckton als Adresse aus dem Android-Tonwähler; leer = Standard-Weckton des Handys. */
    var weckton: String get() = e.wort(art.schluessel("weckton"), ""); set(v) = e.setzeWort(art.schluessel("weckton"), v)
    /** So viele Sekunden klingelt der Wecker, dann verstummt er und Jarvis liest vor; 0 = bis zum Antippen oder Wegwischen. */
    var wecktonSekunden: Int get() = e.ganz(art.schluessel("weckton_sekunden"), if (art.vorlesen) 10 else 0).coerceIn(0, 300); set(v) = e.setzeGanz(art.schluessel("weckton_sekunden"), v.coerceIn(0, 300))

    /** Klingelt sie wirklich wie ein Wecker? „Lautlos“ schaltet den Wecker mit ab, auch wenn er noch gewählt ist. */
    val weckerWirksam get() = art.wecker && wecker && !(art.ton && !ton)
}

object Meldungsarten {
    const val ERGEBNISSE = "Ergebnisse und Rückmeldungen"
    const val ALLTAG = "Alltag"
    const val ERINNERUNGEN = "Erinnerungen"
    const val WECKWORT = "Weckwort"
    const val DAUER = "Daueranzeigen"

    val AGENT = Meldungsart(
        "agent", "Agent fertig", "Meldet, wenn ein Agent seinen Bericht in die Ablage gelegt hat oder abgebrochen ist.",
        "Sobald ein Agent fertig ist.", ERGEBNISSE, "jarvis_ergebnisse",
        ton = true, vorlesen = true, wecker = true, ruhezeit = true, alt = mapOf("vorlesen" to "ergebnisse_vorlesen"),
    )
    val PROGRAMMIEREN = Meldungsart(
        "programmieren", "Programmieraufträge", "Meldet fertige Aufträge und Probleme beim Programmieren. Rückfragen haben ihre eigene Meldung.",
        "Bei Fertig oder Problem eines Programmierauftrags.", ERGEBNISSE, "jarvis_ergebnisse",
        ton = true, vorlesen = true, wecker = true, ruhezeit = true,
    )
    /**
     * Franks Wunsch vom 23.10.2026: Rückfragen getrennt von den Programmieraufträgen an- und abschalten. An = kurzer Ton,
     * dann liest Jarvis vor, dass eine Frage beantwortet werden muss, auch bei gesperrtem Handy. Das Vollbild der Rückfrage
     * in der App kommt immer, unabhängig von diesem Schalter.
     */
    val RUECKFRAGE = Meldungsart(
        "rueckfrage", "Rückfragen vom Programmierer", "Meldet jede Rückfrage einer Programmier-Sitzung, auch die Code-Review-Frage, mit kurzem Ton und liest vor, dass eine Frage beantwortet werden muss.",
        "Sobald eine Programmier-Sitzung dich etwas fragt, auch bei gesperrtem Handy.", ERGEBNISSE, Meldungen.KANAL_RUECKFRAGE,
        ton = true, vorlesen = true, vorlesenStandard = true,
    )
    val ABLAGE = Meldungsart(
        "ablage", "Ablage", "Meldet, wenn ein Bild, ein PDF oder ein Download in der Ablage fertig ist oder scheitert.",
        "Wenn eine Arbeit in der Ablage fertig wird.", ERGEBNISSE, "jarvis_ergebnisse",
        ton = true, vorlesen = true, wecker = true, ruhezeit = true,
    )
    val GEMINI = Meldungsart(
        "gemini", "Gemini-Schlüssel gewechselt", "Sagt Bescheid, wenn das kostenlose Gemini-Kontingent leer ist und Jarvis mit dem bezahlten Schlüssel weiterspricht.",
        "Beim Schlüsselwechsel; im Live-Gespräch lautlos, Vorlesen wartet das Gespräch ab.", ERGEBNISSE, "jarvis_ergebnisse", tonStandard = false,
    )
    val KONTINGENT = Meldungsart(
        "kontingent", "Kontingent zu schnell", "Warnt, wenn ein Limit bei Claude, ChatGPT/Codex, Kimi oder Gemini beim jetzigen Tempo vor dem Zurücksetzen aufgebraucht wäre.",
        "Einmal je Zeitfenster, sobald mindestens 30 Prozent genutzt sind und das Tempo zu hoch ist.", ERGEBNISSE, "jarvis_ergebnisse",
        ton = true, vorlesen = true, ruhezeit = true,
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
        schaltbar = false, vorlesen = true, dringend = true, eigenerTextStandard = "{name} ist abgelaufen.",
        weckerStandard = true, alt = mapOf("wecker" to "timer_wecker"),
    )
    /** Zwei unabhängige Profile; die Form gehört weiterhin zur einzelnen Erinnerung. */
    val ERINNERUNG = Meldungsart(
        "erinnerung", "Als Benachrichtigung", "Einstellungen für Erinnerungen in der Form Benachrichtigung.",
        "Zu den eingestellten Zeitpunkten, auf dem Gerät, das du gerade benutzt.", ERINNERUNGEN, "jarvis_erinnerung",
        schaltbar = false, vorlesenStandard = true, formFest = true,
    )
    val ERINNERUNG_WECKER = Meldungsart(
        "erinnerung_wecker", "Als Wecker", "Einstellungen für Erinnerungen in der Form Wecker; unabhängig von normalen Erinnerungsbenachrichtigungen.",
        "Zu den eingestellten Zeitpunkten, bis du die Auslösung bestätigst.", ERINNERUNGEN, "jarvis-timer-wecker",
        schaltbar = false, vorlesenStandard = true, formFest = true, weckerStandard = true,
    )
    val STOPPUHR = Meldungsart(
        "stoppuhr", "Stoppuhr", "Zeigt die laufende Stoppuhr mit mitlaufender Zeit.",
        "Beim Starten oder Fortsetzen; danach läuft die Zeitanzeige still weiter.", ALLTAG, "jarvis-stoppuhr", tonStandard = false,
    )
    val ANSAGE = Meldungsart(
        "ansage", "Update-Ansagen", "Jarvis sagt an, wenn in UpdateStation ein Update fertig geladen bereitliegt.",
        "Wenn UpdateStation ein neues Update meldet.", ALLTAG, null,
        vorlesenStandard = true, ruhezeit = true, lautStandard = 100, tonStandard = false,
        alt = mapOf("an" to "ansage_vorlesen", "stimme" to "ansage_stimme", "tempo" to "ansage_tempo", "lautstaerke" to "ansage_lautstaerke"),
    )
    val INSTALLFRAGE = Meldungsart(
        "installfrage", "Installationsfrage ansagen", "Jarvis sagt an, wenn eine neue Jarvis-Version installiert werden kann und das Fenster auf deine Antwort wartet.",
        "Wenn die Frage zur Installation erscheint, außer während einer Aufnahme oder eines Gesprächs.", ALLTAG, null,
        vorlesenStandard = true, ruhezeit = true, lautStandard = 100, tonStandard = false,
        eigenerTextStandard = "Eine neue Jarvis-Version kann installiert werden. Bitte wähle auf dem Handy im Jarvis-Fenster, wann die Installation starten soll. Ohne Antwort startet sie gleich von selbst.",
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
        "Nach Neustart oder Update, wenn Android den Mikrofon-Dienst nicht starten ließ.", WECKWORT, "jarvis_weckwort", tonStandard = false,
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

    val alle = listOf(AGENT, PROGRAMMIEREN, RUECKFRAGE, ABLAGE, GEMINI, KONTINGENT, ABFAHRT, TIMER, ERINNERUNG, ERINNERUNG_WECKER, STOPPUHR, ANSAGE, INSTALLFRAGE, WECKWORT_HOERT, RUF, WECKWORT_AUS, BEREITSCHAFT, AUFNAHME, VORLESEN)
    val gruppen = listOf(ERGEBNISSE, ALLTAG, ERINNERUNGEN, WECKWORT, DAUER)
}

/** Zeigt Benachrichtigungen so, wie Frank sie unter Einstellungen → Benachrichtigungen eingestellt hat. */
object Meldungen {
    /** Eigener Kanal für „Wie ein Wecker“: Weckton, auch bei „Nicht stören“; die Wichtigkeit lässt sich später nicht mehr anheben. */
    const val KANAL_WECKER = "jarvis_meldung_wecker_ton"
    /** Rückfragen: hohe Wichtigkeit (kurzer Ton, Vollbild bei gesperrtem Handy; jarvis_ergebnisse ist nur DEFAULT). */
    const val KANAL_RUECKFRAGE = "jarvis_rueckfrage"

    fun rueckfrageKanalAnlegen(context: Context) {
        context.applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(KANAL_RUECKFRAGE, "Rückfragen beim Programmieren", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Eine Programmier-Sitzung wartet auf deine Antwort: erscheint sofort im Vollbild." })
    }
    private val nebenher = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun profil(context: Context, art: Meldungsart) = MeldungsProfil(Einstellungen.get(context), art)

    fun erinnerungsArt(alsWecker: Boolean) = if (alsWecker) Meldungsarten.ERINNERUNG_WECKER else Meldungsarten.ERINNERUNG

    /** Eigener hörbarer Ereigniskanal, auch wenn der frühere Statuskanal nur LOW war. */
    fun normalerKanal(context: Context, art: Meldungsart): String {
        val id = "jarvis_meldung_${art.id}"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(id, art.titel, NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            })
        return id
    }

    fun ereignis(context: Context, art: Meldungsart, nummer: Int, titel: String, text: String, vorlesen: Boolean = true) {
        val app = context.applicationContext
        val b = NotificationCompat.Builder(app, normalerKanal(app, art)).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle(titel).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(app, nummer, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        zeige(app, art, nummer, b, titel, if (art.formFest || !vorlesen) null else text)
    }

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
        // Während einer Aufnahme kein Hinweiston (er landete in der Mitschrift); ein Wecker klingelt trotzdem.
        val leise = gespraech || (GespraechsFokus.aufnahme && !art.dringend)
        // Lautlos gewählt heißt auch: kein Wecker, selbst wenn er noch eingeschaltet ist.
        val tonAus = art.ton && !p.ton
        val klingeln = laut && p.weckerWirksam && !ruhe && !gespraech && (!GespraechsFokus.aufnahme || art.dringend)
        val still = !laut || ruhe || (leise && imGespraechStill) || tonAus
        if (!art.pflicht) bauer.setChannelId(normalerKanal(app, art))
        if (p.wecker) {
            weckerKanalAnlegen(app)
            bauer.setChannelId(KANAL_WECKER).setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
        }
        bauer.setSilent(still && !klingeln)
        // SECRET: auf dem Sperrbildschirm gar nicht, erst nach dem Entsperren (PRIVATE wäre der Standard und hülfe nicht).
        // Ein klingelnder Wecker muss sich aber gesperrt abstellen lassen: dann PRIVATE (sichtbar, Inhalt verborgen).
        if (e.meldungInhaltVerbergen) bauer.setVisibility(if (p.wecker) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_SECRET)
        val meldung = bauer.build()
        // Wegwischen beendet den Weckton sofort (ohne eigenen Lösch-Auftrag der Aufrufstelle).
        if (meldung.deleteIntent == null) meldung.deleteIntent = PendingIntent.getBroadcast(app, nummer,
            Intent(app, WeckerTonEmpfaenger::class.java).putExtra(WeckerTonEmpfaenger.EXTRA_NUMMER, nummer), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        // Eine neue Fassung derselben Meldung ohne Wecker (still aktualisiert, Stand „läuft“) beendet einen noch laufenden Weckton.
        if (!klingeln) WeckerTon.stopp(nummer)
        val gezeigt = runCatching { app.getSystemService(NotificationManager::class.java).notify(nummer, meldung) }
            .onFailure { de.frank.jarvis.observability.Logbuch.fehler("Benachrichtigung: ${art.titel}", it) }.isSuccess
        // Den Weckton spielt Jarvis selbst (eigener Ton und eigene Dauer je Meldung, der Kanal ist stumm): Er läuft die gewählten
        // Sekunden oder bis Frank die Meldung antippt oder wegwischt, dann verstummt er, die Meldung bleibt stehen, und Jarvis liest vor.
        if (klingeln && gezeigt) klingle(app, nummer, art, p)
        if (laut && sprechtext != null && art.vorlesen && p.vorlesen && !ruhe)
            vorlesenSpaeter(app, art, titel, sprechtext, nummer.toString(), solange = if (klingeln) ({ WeckerTon.klingelt(nummer) }) else null)
        return gezeigt
    }

    /** Ohne Dauer („bis zum Antippen“) klingelt der Wecker höchstens so lange. */
    private const val WECKTON_HOECHSTENS_MS = 10 * 60_000L

    private fun klingle(app: Context, nummer: Int, art: Meldungsart, p: MeldungsProfil) {
        // Die Kennung gehört genau zu diesem Klingeln: Eine neuere Meldung unter derselben Nummer beendet nicht die alte Schleife.
        val kennung = WeckerTon.starte(app, nummer, p.weckton, art.id)
        val sekunden = if (art == Meldungsarten.ERINNERUNG_WECKER) p.wecktonSekunden.takeIf { it > 0 } ?: 10 else p.wecktonSekunden
        nebenher.launch {
            val anfang = android.os.SystemClock.elapsedRealtime()
            val bis = anfang + if (sekunden > 0) sekunden * 1_000L else WECKTON_HOECHSTENS_MS
            // Während der kurzen Sichtbarkeitsfrist gelten Lautlos, Aufnahme und Abschalten schon sofort.
            // Ein Live-Gespräch hat Vorrang: Beginnt es, verstummt der Weckton (außer bei Losfahren und Timer).
            while (WeckerTon.klingelt(nummer, kennung) && (android.os.SystemClock.elapsedRealtime() - anfang < 1_500L || sichtbar(app, nummer)) && android.os.SystemClock.elapsedRealtime() < bis &&
                p.weckerWirksam && an(app, art) && !inRuhe(app, art) &&
                !((GespraechsFokus.stilleHinweise || GespraechsFokus.aufnahme) && !art.dringend)) delay(200L)
            WeckerTon.stopp(nummer, kennung)
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
        // Franks Vorgabe vom 24.10.2026: Jede Rückfrage wird einzeln angesagt, auch bei gleichem Wortlaut aus einer anderen
        // Session. Die nächste wartet, bis die vorige gesprochen ist; ihr Start schnitt sonst die laufende mitten im Satz ab.
        if (art === Meldungsarten.RUECKFRAGE) rueckfrageReihe.withLock { sprichEinzeln(context, art, titel, text, solange, ausdruecklich, warten = true) }
        else sprichEinzeln(context, art, titel, text, solange, ausdruecklich, warten = false)
    }

    private val rueckfrageReihe = kotlinx.coroutines.sync.Mutex()

    private suspend fun sprichEinzeln(context: Context, art: Meldungsart, titel: String, text: String, solange: (() -> Boolean)?, ausdruecklich: Boolean, warten: Boolean) {
        // Erst den Weckton zu Ende klingeln lassen (er endet selbst, spätestens nach seiner Höchstdauer), dann das Gespräch abwarten.
        if (solange != null) withTimeoutOrNull(WECKTON_HOECHSTENS_MS + 5_000L) { while (solange()) delay(500L) }
        // Während einer Sprachaufnahme nichts vorlesen, sonst landet die Ansage in Franks Mitschrift. Ohne Zeitgrenze: Ein Diktat
        // darf lange laufen; eine inzwischen erledigte Rückfrage bricht ihre Ansage selbst ab.
        while (GespraechsFokus.aufnahme) delay(1_000L)
        val frei = withTimeoutOrNull(if (art.dringend) 90_000L else 15 * 60_000L) {
            // Steht das Vorlesen wegen einer Rückfrage (auch eine Antwort im Gesprächsmodus), kommt ihre Ansage sofort. Im Live-Gespräch steht es nie.
            val rueckfrage = art === Meldungsarten.RUECKFRAGE
            while (GespraechsFokus.stilleHinweise && !(rueckfrage && de.frank.jarvis.speech.Vorleser.aktuell()?.unterbrochen == true))
                delay(if (rueckfrage) 250L else 2_000L)
            true
        }
        if (frei != true) return
        val p = profil(context, art)
        if (!ausdruecklich && (!an(context, art) || inRuhe(context, art) || (art.vorlesen && !p.vorlesen))) return
        if (warten) Ansage.sprichUndWarte(context, art.id, titel, text, p.lautstaerke, p.stimmWahl)
        else Ansage.sprich(context, art.id, titel, text, p.lautstaerke, p.stimmWahl)
    }

    /** Probe aus den Einstellungen: zeigt (und liest) eine Beispielmeldung mit den aktuellen Einstellungen. */
    fun probe(context: Context, art: Meldungsart) {
        val app = context.applicationContext
        when (art) {
            Meldungsarten.ABFAHRT -> de.frank.jarvis.fahrt.Abfahrt.probe(app)
            Meldungsarten.TIMER -> de.frank.jarvis.timer.Timer.starten(app, 5_000L, "Probe-Timer")
            Meldungsarten.ANSAGE -> Ansage.ansage(app, "UpdateStation", "Ein Update von Jarvis ist bereit in UpdateStation.")
            Meldungsarten.ERINNERUNG, Meldungsarten.ERINNERUNG_WECKER -> nebenher.launch { de.frank.jarvis.ablage.ErinnerungStimme.probe(app, art == Meldungsarten.ERINNERUNG_WECKER) }
            else -> {
                de.frank.jarvis.dienst.JarvisDienst.kanalAnlegen(app)
                de.frank.jarvis.weckwort.WeckwortDienst.kanaeleAnlegen(app)
                if (art == Meldungsarten.RUECKFRAGE) rueckfrageKanalAnlegen(app)
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
        if (art.schaltbar && !profil(app, art).an) { vorlesenAbbrechen(art); WeckerTon.stoppArt(art.id) }
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
    private const val TAG = "JarvisWeckerTon"
    private const val PROBE = -4_711

    /** Ein Klingeln: [kennung] unterscheidet es von einem späteren unter derselben Nummer; der Spieler kommt nach dem Laden dazu. */
    private class Klang(val kennung: Long, val art: String) { var spieler: android.media.MediaPlayer? = null }

    private val laufend = java.util.concurrent.ConcurrentHashMap<Int, Klang>()
    private val sperre = Any()
    private val zaehler = java.util.concurrent.atomic.AtomicLong()
    private val laden = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val probeEnde = Runnable { stopp(PROBE) }
    private val haupt = android.os.Handler(android.os.Looper.getMainLooper())
    @Volatile private var vibrator: android.os.Vibrator? = null
    private val attribute = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    /** Klingelt unter [nummer] gerade etwas (mit [kennung]: genau dieses Klingeln)? */
    fun klingelt(nummer: Int, kennung: Long? = null): Boolean = laufend[nummer]?.let { kennung == null || it.kennung == kennung } == true

    /**
     * Startet den Ton und kehrt sofort zurück (geladen wird abseits, nicht auf dem Hauptfaden). [ton] leer = Standard-Weckton;
     * ist der gewählte nicht abspielbar, der Standard. Mit Vibration in Schleife wie ein Wecker, außer bei der Probe.
     */
    fun starte(context: Context, nummer: Int, ton: String, art: String): Long {
        val app = context.applicationContext
        val klang = synchronized(sperre) {
            stopp(nummer)
            Klang(zaehler.incrementAndGet(), art).also { laufend[nummer] = it }
        }
        laden.launch {
            val p = vorbereiten(app, ton)
            val behalten = synchronized(sperre) {
                if (p != null && laufend[nummer] === klang) {
                    runCatching {
                        p.start()
                        klang.spieler = p
                        if (nummer != PROBE) vibrieren(app)
                    }.onFailure { android.util.Log.w(TAG, "Weckton nicht gestartet", it) }.isSuccess
                } else false
            }
            if (!behalten) { p?.let { runCatching { it.release() } }; laufend.remove(nummer, klang); return@launch }
        }
        return klang.kennung
    }

    /** Spielbereit gemachter Spieler mit dem ersten Ton, der sich öffnen lässt; null = keiner. */
    internal fun vorbereiten(app: Context, ton: String): android.media.MediaPlayer? {
        for (uri in toene(app, ton)) {
            val p = android.media.MediaPlayer()
            val ok = runCatching {
                p.setAudioAttributes(attribute)
                p.setDataSource(app, uri)
                p.isLooping = true
                p.setWakeMode(app, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                p.prepare()
            }.onFailure { android.util.Log.w(TAG, "Weckton $uri nicht abspielbar", it) }.isSuccess
            if (ok) return p
            runCatching { p.release() }
        }
        return null
    }

    private fun toene(app: Context, ton: String) = listOfNotNull(
        ton.takeIf { it.isNotBlank() }?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() },
        RingtoneManager.getActualDefaultRingtoneUri(app, RingtoneManager.TYPE_ALARM),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
    )

    /** Lässt sich der Ton öffnen? Eigene Musik aus dem Speicher darf Jarvis ohne Medien-Erlaubnis oft nicht lesen. */
    fun abspielbar(context: Context, ton: String): Boolean = ton.isBlank() ||
        runCatching { context.contentResolver.openAssetFileDescriptor(android.net.Uri.parse(ton), "r")?.use { true } ?: false }.getOrDefault(false)

    /** Beendet das Klingeln unter [nummer]; mit [kennung] nur, wenn es noch genau dieses ist. */
    fun stopp(nummer: Int, kennung: Long? = null) {
        synchronized(sperre) {
            val klang = laufend[nummer] ?: return
            if (kennung != null && klang.kennung != kennung) return
            if (!laufend.remove(nummer, klang)) return
            klang.spieler?.let { runCatching { it.stop() }; runCatching { it.release() } }
            klang.spieler = null
            if (laufend.keys.none { it != PROBE }) vibrator?.let { runCatching { it.cancel() }; vibrator = null }
        }
    }

    /** Alle Wecktöne einer Meldungsart beenden (etwa nachdem sie ausgeschaltet wurde). */
    fun stoppArt(art: String) = laufend.filterValues { it.art == art }.forEach { (nummer, klang) -> stopp(nummer, klang.kennung) }

    /** Alle Wecktöne beenden (Frank hat Jarvis geöffnet: Er hat die Meldung gesehen). */
    fun alleStopp() = laufend.keys.toList().forEach { stopp(it) }

    private fun vibrieren(app: Context) {
        if (vibrator != null) return
        runCatching {
            val v = if (android.os.Build.VERSION.SDK_INT >= 31) app.getSystemService(android.os.VibratorManager::class.java).defaultVibrator
                else @Suppress("DEPRECATION") (app.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator)
            v.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attribute)
            vibrator = v
        }
    }

    /** Zum Anhören in den Einstellungen: vier Sekunden; eine neue Probe ersetzt die alte samt ihrem Ende. */
    fun probe(context: Context, ton: String) {
        haupt.removeCallbacks(probeEnde)
        starte(context, PROBE, ton, "probe")
        haupt.postDelayed(probeEnde, 4_000L)
    }

    fun probeStopp() { haupt.removeCallbacks(probeEnde); stopp(PROBE) }

    fun name(context: Context, ton: String): String =
        if (ton.isBlank()) "Standard-Weckton"
        else runCatching { RingtoneManager.getRingtone(context, android.net.Uri.parse(ton))?.getTitle(context) }.getOrNull() ?: "Eigener Ton"

    /** Die Wecktöne des Handys (Adresse, Name), für die Auswahl per Sprache. Den Cursor verwaltet der RingtoneManager selbst. */
    fun toene(context: Context): List<Pair<String, String>> = runCatching {
        val m = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val c = m.cursor
        try {
            buildList { while (c.moveToNext()) add(m.getRingtoneUri(c.position).toString() to c.getString(RingtoneManager.TITLE_COLUMN_INDEX)) }
        } finally {
            @Suppress("DEPRECATION") c.deactivate()
        }
    }.getOrDefault(emptyList())
}

/** Wegwischen einer Meldung: ihren Weckton beenden. */
class WeckerTonEmpfaenger : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nummer = intent.getIntExtra(EXTRA_NUMMER, Int.MIN_VALUE)
        if (nummer != Int.MIN_VALUE) WeckerTon.stopp(nummer)
    }

    companion object { const val EXTRA_NUMMER = "nummer" }
}
