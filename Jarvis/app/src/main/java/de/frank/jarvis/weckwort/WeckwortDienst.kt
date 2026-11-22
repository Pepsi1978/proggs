package de.frank.jarvis.weckwort

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.meldung.Meldungen
import de.frank.jarvis.meldung.Meldungsarten
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.VorleseZustand
import de.frank.jarvis.speech.Vorleser
import de.frank.module.transkriptionsmodul.SileroVad
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lauscht dauerhaft auf „Hey Jarvis“, auch bei gesperrtem Handy (Vordergrunddienst vom Typ Mikrofon).
 *
 * Bei einem Treffer gibt der Dienst das Mikrofon frei und holt Jarvis nach vorn ([MainActivity] mit
 * [EXTRA_WECKWORT]); die App nimmt dann auf, piept und antwortet. Aus dem Hintergrund darf eine App das nur mit
 * der Erlaubnis „Über anderen Apps einblenden“; fehlt sie, kommt eine Benachrichtigung zum Antippen.
 *
 * Gelauscht wird nur, solange Jarvis nicht selbst aufnimmt ([Weckwort.belegt]) und nichts vorliest. So weckt sich
 * Jarvis nicht selbst, wenn in einer Antwort „Jarvis“ vorkommt.
 *
 * Android erlaubt den Start eines Mikrofon-Dienstes nur, während Jarvis sichtbar ist. Deshalb startet ihn
 * [abgleichen] aus der App; nach Neustart und Update übernimmt [nachNeustart] (kurz unsichtbar Jarvis öffnen oder
 * eine Benachrichtigung).
 */
class WeckwortDienst : Service() {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    // Die Modelle lädt und nutzt der Weckwort-Faden; geladen wird nur unter [modellSperre].
    @Volatile private var erkenner: WeckwortErkenner? = null
    /** Eigene Phrase (sherpa-onnx); null = „Hey Jarvis“. */
    @Volatile private var phrasenErkenner: PhrasenErkenner? = null
    /** Für welchen [phrasenStand] [phrasenErkenner] geladen wurde. */
    private var phrasenErkennerStand = -1
    /** Zählt jede Änderung der Einstellungen: Ein Phrasen-Modell gilt nur für den Stand, mit dem es geladen wurde. */
    @Volatile private var phrasenStand = 0
    /** Tokens der eigenen Phrase aus den Einstellungen; null = „Hey Jarvis“. */
    private var phrasenTokens: String? = null
    private var phrasenSchwelle = 0.25f
    private var phrasenVarianten: List<String> = emptyList()
    @Volatile private var sprecherErkenner: SprecherErkenner? = null
    /** Sprachaktivitätserkennung (Silero, wie im Sprachmodus): hört Stimmen aus Lärm heraus. null = nicht ladbar. */
    @Volatile private var vad: SileroVad? = null
    private val modellSperre = Any()
    private var vorleser: Vorleser? = null
    @Volatile private var lauf: AtomicBoolean? = null
    private var faden: Thread? = null
    /** Der zuletzt gestartete Durchgang: Der nächste wartet erst auf ihn, so lesen nie zwei Fäden zugleich. */
    private var letzterFaden: Thread? = null
    /** Mikrofon-Fehler in Folge, damit ein dauerhaft belegtes Mikrofon das Logbuch nicht flutet. */
    @Volatile private var lauschFehler = 0
    @Volatile private var wach: PowerManager.WakeLock? = null
    private var gestartet = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        kanaeleAnlegen(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (gestartet) {
            if (intent?.action == AKTION_NEU_LADEN) { hinweisErneuern(this, nurSichtbare = true); bereich.launch { neuLaden() } }
            // Nur einen neuen Durchgang beginnen (Mitschnitt): Modelle und Verstärkung bleiben, wie sie sind.
            if (intent?.action == AKTION_NEU_STARTEN) bereich.launch {
                anhalten()
                if (!Weckwort.belegt.value && vorleser?.stand?.value?.zustand == VorleseZustand.AUS) lauschen()
            }
            return START_STICKY
        }
        val e = Einstellungen.get(this)
        if (!e.weckwortAn || !mikrofonErlaubt(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val typ = if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ServiceCompat.startForeground(this, HINWEIS_ID, hinweis(), typ)
            laeuft = true
        } catch (fehler: Exception) {
            // Aus dem Hintergrund (etwa nach einem Neustart durch Android) verweigert: über die App neu starten.
            Log.w(TAG, "Mikrofon-Dienst aus dem Hintergrund nicht erlaubt", fehler)
            nachNeustart(this)
            stopSelf()
            return START_NOT_STICKY
        }
        gestartet = true
        Stimmprofil.laden(this)
        einstellungenUebernehmen()
        wach = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:weckwort")
            .apply { setReferenceCounted(false); runCatching { acquire() } }
        val vorleser = Vorleser.hole(this, SecureSettings(this)).also { this.vorleser = it }
        bereich.launch {
            combine(Weckwort.belegt, vorleser.stand) { belegt, stand ->
                // null = frei; sonst der Grund, warum das Weckwort gerade taub ist (aus genau den Werten, die es auslösten).
                if (belegt) "Jarvis nimmt auf oder führt ein Sprachgespräch" else if (stand.zustand != VorleseZustand.AUS) "Jarvis liest vor" else null
            }
                .distinctUntilChanged { a, b -> (a == null) == (b == null) }
                .collect { grund ->
                    if (grund == null) {
                        pauseMelden()
                        lauschen()
                    } else {
                        // Warum taub: Für die Auswertung „reagiert nicht“ (Leerlauf oder belegt) im Logbuch.
                        pauseSeit = SystemClock.elapsedRealtime()
                        pauseGrund = grund
                        anhalten()
                    }
                }
        }
        // Ruft der Dienst die App und sie kommt nicht (Android blockt den Start still), nicht taub bleiben: Der Ruf
        // kommt als Benachrichtigung zum Antippen, und das Mikrofon geht zurück ans Weckwort.
        bereich.launch {
            while (true) {
                delay(1_000)
                val gerufen = Weckwort.gerufenUm
                if (gerufen != 0L && System.currentTimeMillis() - gerufen > WAECHTER_MS) {
                    Log.w(TAG, "Jarvis kam nach „Hey Jarvis“ nicht nach vorn")
                    Logbuch.system("Weckwort: Jarvis kam nicht nach vorn", "Ruf als Benachrichtigung")
                    // Ohne Vollbild-Absicht: Startet die App nur langsam (Kaltstart unter Last), darf diese Meldung sie
                    // kein zweites Mal starten, sonst bricht der zweite Start das laufende Gespräch ab.
                    zumAntippen(rufAbsicht(), vollbild = false)
                }
            }
        }
        return START_STICKY
    }

    /** Seit wann der Dienst absichtlich nicht lauscht (0 = er lauscht) und warum. */
    private var pauseSeit = 0L
    private var pauseGrund = ""

    /** Eine absichtliche Pause ab einer Sekunde kommt ins Logbuch: So lässt sich ein verpasster Ruf einer Ursache zuordnen. */
    private fun pauseMelden() {
        val seit = pauseSeit
        pauseSeit = 0L
        if (seit == 0L) return
        val ms = SystemClock.elapsedRealtime() - seit
        if (ms >= 1_000) Logbuch.dauer("Weckwort-Pause", ms, pauseGrund)
    }

    /** Weckwort, Phrase und Empfindlichkeit aus den Einstellungen lesen. */
    private fun einstellungenUebernehmen() {
        val e = Einstellungen.get(this)
        Weckwort.schwelle = e.weckwortSchwelle
        phrasenTokens = if (e.weckwortArt == "phrase") PhrasenTokens.zerlege(this, e.weckwortPhrase).also {
            if (it == null) Log.w(TAG, "Phrase ${e.weckwortPhrase} nicht zerlegbar, nehme Hey Jarvis")
        } else null
        phrasenVarianten = if (phrasenTokens != null) PhrasenTokens.varianten(this, e.weckwortPhrase) else emptyList()
        phrasenSchwelle = PhrasenErkenner.schwelleFuer(e.weckwortEmpfindlichkeit)
        Stimmprofil.schwellenAbzug = e.weckwortStimmAbzug
        Stimmprofil.kurzerRufBoden = if (e.weckwortEmpfindlichkeit == "sehr_hoch") Stimmprofil.STIMM_BODEN_KURZ else Stimmprofil.STIMM_BODEN
        Stimmprofil.schluesselSetzen(if (phrasenTokens != null) "phrase:" + PhrasenTokens.normiert(e.weckwortPhrase) else "jarvis")
    }

    /** Einstellungen geändert: neu einlesen und mit dem neuen Weckwort weiterlauschen. */
    private suspend fun neuLaden() {
        // Neuer Stand: Der nächste Durchgang lädt das Phrasen-Modell neu und gibt das alte frei, sobald der alte
        // Faden sicher zu Ende ist (vorher würde er auf ein freigegebenes Modell zugreifen).
        phrasenStand++
        einstellungenUebernehmen()
        anhalten()
        if (!Weckwort.belegt.value && vorleser?.stand?.value?.zustand == VorleseZustand.AUS) lauschen()
    }

    /** Startet den Weckwort-Faden. Die Modelle lädt er selbst, das dauert bis über eine Sekunde und hielte sonst die App an. */
    private fun lauschen() {
        if (faden?.isAlive == true) return
        val tokens = phrasenTokens
        val schwelle = phrasenSchwelle
        val varianten = phrasenVarianten
        val stand = phrasenStand
        val phrase = Einstellungen.get(this).weckwortPhrase
        val vorher = letzterFaden
        val an = AtomicBoolean(true)
        lauf = an
        faden = Thread({
            // Wie Audio-Aufnahme: Unter Last (Mitschrift, Modelle, Oberfläche) darf das Lesen nie zurückfallen, sonst läuft der
            // Puffer über und ein Ruf geht verloren.
            runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO) }
            // Hängt ein alter Durchgang noch (Treffer, Mikrofon), erst ihn auslaufen lassen: nie zwei Leser zugleich.
            val vorgaengerFertig = vorher == null || runCatching { vorher.join(3_000); !vorher.isAlive }.getOrDefault(false)
            if (!an.get()) return@Thread
            val m = modelleHolen(tokens, schwelle, varianten, stand, vorgaengerFertig)
            val erk = m.erk
            if (erk == null) {
                // Ohne Modell bliebe der Dienst sonst für immer taub, mit Meldung und wachgehaltener CPU.
                if (an.get()) modelleSpaeterNeu(an)
                return@Thread
            }
            if (!an.get()) return@Thread
            ladeFehler = 0
            // Nach einem gescheiterten Laden freigegeben (siehe [modelleSpaeterNeu]): fürs Lauschen wieder wach halten.
            runCatching { wach?.takeIf { !it.isHeld }?.acquire() }
            Weckwort.aktivesWort(if (m.phr != null) PhrasenTokens.normiert(phrase) else "Hey Jarvis")
            schleife(erk, m.phr, m.spr, m.vad, an)
        }, "Weckwort").also { letzterFaden = it; it.start() }
    }

    /** Gescheiterte Ladeversuche der Weckwort-Modelle in Folge, für den wachsenden Abstand bis zum nächsten. */
    @Volatile private var ladeFehler = 0

    /**
     * Die Weckwort-Modelle ließen sich nicht laden: später erneut versuchen (1, 2, 4 … höchstens 30 Minuten) und die CPU
     * bis dahin schlafen lassen, denn ohne Modell wird nicht gelauscht.
     */
    private fun modelleSpaeterNeu(an: AtomicBoolean) {
        val warte = minOf(60_000L shl minOf(ladeFehler++, 5), 30 * 60_000L)
        Log.w(TAG, "Weckwort-Modelle fehlen, neuer Versuch in ${warte / 1000} s")
        runCatching { if (wach?.isHeld == true) wach?.release() }
        bereich.launch {
            delay(warte)
            if (lauf === an) { faden = null; lauschen() }
        }
    }

    private class Modelle(val erk: WeckwortErkenner?, val phr: PhrasenErkenner?, val spr: SprecherErkenner?, val vad: SileroVad?)

    /** Lädt fehlende Modelle (im Weckwort-Faden) und hält sie für den nächsten Durchgang bereit. */
    private fun modelleHolen(
        tokens: String?, schwelle: Float, varianten: List<String>, stand: Int, vorgaengerFertig: Boolean,
    ): Modelle = synchronized(modellSperre) {
        val beginn = SystemClock.elapsedRealtime()
        var geladen = false
        val erk = erkenner ?: runCatching { WeckwortErkenner(this) }
            .onFailure { Log.e(TAG, "Weckwort-Modelle nicht ladbar", it); Logbuch.fehler("Weckwort-Modelle nicht ladbar", it) }
            .getOrNull()?.also { erkenner = it; geladen = true }
        // Ein altes Phrasen-Modell (andere Phrase oder Empfindlichkeit) nur schließen, wenn kein Faden es mehr nutzt;
        // sonst gibt sherpa-onnx es beim Aufräumen selbst frei (finalize).
        val alt = phrasenErkenner
        if (alt != null && (tokens == null || phrasenErkennerStand != stand)) {
            phrasenErkenner = null
            if (vorgaengerFertig) runCatching { alt.close() }
        }
        // Eigene Phrase: sherpa-onnx dazu. Lässt sich das Modell nicht laden, bleibt „Hey Jarvis“ aktiv.
        val phr = if (tokens == null) null else phrasenErkenner ?: runCatching { PhrasenErkenner(this, tokens, schwelle, varianten) }
            .onFailure { Log.e(TAG, "Phrasen-Modell nicht ladbar, nehme Hey Jarvis", it); Logbuch.fehler("Phrasen-Modell nicht ladbar", it) }
            .getOrNull()?.also { phrasenErkenner = it; phrasenErkennerStand = stand; geladen = true }
        // Stimmerkennung (Stimm-Fingerabdruck). Fehlt sie, bleibt es beim Stimmprofil aus den Lautmerkmalen.
        val spr = sprecherErkenner ?: runCatching { SprecherErkenner(this) }
            .onFailure { Log.e(TAG, "Stimmerkennung nicht ladbar", it); Logbuch.fehler("Stimmerkennung nicht ladbar", it) }
            .getOrNull()?.also { sprecherErkenner = it; geladen = true }
        // Silero: Fehlt es, bleibt es bei Lautstärke-Filter und Zuschnitt nach Lautstärke.
        val v = vad ?: runCatching { SileroVad(this, WeckwortErkenner.KLEIDIAI_ABSCHALTEN) }
            .onFailure { Log.e(TAG, "Silero nicht ladbar", it); Logbuch.fehler("Weckwort: Silero nicht ladbar", it) }
            .getOrNull()?.also { vad = it; geladen = true }
        if (geladen) Logbuch.dauer("Weckwort-Modelle geladen", SystemClock.elapsedRealtime() - beginn, if (phr != null) "mit Phrase" else "Hey Jarvis")
        Modelle(erk, phr, spr, v)
    }

    /** Hält das Lauschen an und wartet abseits des Hauptfadens kurz, bis das Mikrofon wirklich frei ist. */
    private suspend fun anhalten() {
        val alt = anhaltenSofort() ?: return
        if (alt.isAlive) withContext(Dispatchers.IO) { runCatching { alt.join(500) } }
    }

    /** Nur das Zeichen zum Aufhören, ohne Warten; liefert den laufenden Faden. */
    private fun anhaltenSofort(): Thread? {
        lauf?.set(false)
        lauf = null
        val alt = faden
        faden = null
        return alt
    }

    /** Bis dahin nicht erneut über den Anker lauschen (Umschalten gescheitert oder Strecke abgerissen). */
    @Volatile private var ankerSperreBis = 0L
    /** Lauschte der letzte Durchgang über den Anker? Nur für knappe Logbuch-Einträge beim Wechsel. */
    @Volatile private var zuletztAnker = false

    /** Gescheiterte Versuche über den Anker in Folge: Der Abstand wächst (15 s, 60 s, 5 min), damit das Weckwort nicht ständig taub ist. */
    @Volatile private var ankerFehler = 0

    /** Telefonat oder VoIP-Gespräch: Dann gehört die Freisprech-Strecke der Telefon-App. */
    private fun telefoniert(): Boolean = getSystemService(android.media.AudioManager::class.java)?.mode?.let { it != android.media.AudioManager.MODE_NORMAL } == true

    /** Kann das Weckwort gerade über die Mikrofone des Anker gehört werden (Schalter, verbunden, keine fremde Musik, kein Telefonat)? */
    private fun ankerMoeglich(): Boolean {
        if (Build.VERSION.SDK_INT < 31 || !Einstellungen.get(this).ankerWeckwort || Stimmprofil.lerntAn || Weckwort.nurHandy) return false
        if (SystemClock.elapsedRealtime() < ankerSperreBis || telefoniert()) return false
        // Spielt eine andere App Musik, bliebe sie sonst in Telefonqualität: dann lauscht das Handy.
        if (getSystemService(android.media.AudioManager::class.java)?.isMusicActive == true) return false
        return de.frank.jarvis.audio.KonferenzMikrofon.verfuegbar(this)
    }

    @SuppressLint("MissingPermission") // geprüft in onStartCommand
    private fun schleife(erk: WeckwortErkenner, phr: PhrasenErkenner?, spr: SprecherErkenner?, vad: SileroVad?, an: AtomicBoolean) {
        var aufnahme: AudioRecord? = null
        var mitschnitt: WeckwortMitschnitt? = null
        // Über den Anker (Telefon-Profil): Strecke vor der Aufnahme öffnen, am Ende freigeben oder ans Gespräch übergeben.
        var ueberAnker = false
        var uebergabe = false
        var neuStart = false
        try {
            // Inzwischen angehalten (Gespräch holt das Mikrofon): weder Anker-Strecke noch Mikrofon öffnen.
            if (!an.get()) return
            if (ankerMoeglich()) {
                ueberAnker = kotlinx.coroutines.runBlocking { de.frank.jarvis.audio.KonferenzMikrofon.holenFuerWeckwort(this@WeckwortDienst) }
                // Kurz nach dem Verbinden steht das Telefon-Profil oft noch nicht: bald erneut versuchen, bei Wiederholung seltener.
                if (!ueberAnker) ankerSperreBis = SystemClock.elapsedRealtime() + when (ankerFehler++) { 0 -> 15_000L; 1 -> 60_000L; else -> 300_000L }
                else ankerFehler = 0
            }
            if (ueberAnker != zuletztAnker) {
                zuletztAnker = ueberAnker
                Logbuch.system("Weckwort", if (ueberAnker) "lauscht über die Mikrofone des Anker" else "lauscht über das Handy")
            }
            val minimum = AudioRecord.getMinBufferSize(WeckwortErkenner.ABTASTRATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            aufnahme = AudioRecord(
                if (ueberAnker) MediaRecorder.AudioSource.VOICE_COMMUNICATION else MediaRecorder.AudioSource.VOICE_RECOGNITION, WeckwortErkenner.ABTASTRATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 4, WeckwortErkenner.ABTASTRATE * 2 * 2), // 2 s (Bytes)
            )
            // Wie jeder andere Mikrofon-Fehler: nach einer Pause neu versuchen (catch unten).
            check(aufnahme.state == AudioRecord.STATE_INITIALIZED) { "Mikrofon nicht verfügbar" }
            if (ueberAnker) de.frank.jarvis.audio.KonferenzMikrofon.eingang?.let { runCatching { aufnahme.setPreferredDevice(it) } }
            // Das Umschalten auf den Anker kann bis 3 s dauern: Hat das Gespräch inzwischen das Mikrofon geholt, nicht mitschneiden.
            if (!an.get()) return
            aufnahme.startRecording()
            Weckwort.hoertZu(true)
            lauschFehler = 0
            erk.zuruecksetzen()
            phr?.zuruecksetzen()
            val art = if (phr != null) "phrase" else "jarvis"
            val block = ShortArray(WeckwortErkenner.BLOCK)
            // Ein einzelner Block über der Schwelle reicht nicht: Erst zwei in Folge (160 ms) zählen, wie „patience“
            // bei openWakeWord. Kurze Ausreißer bei ähnlich klingenden Wörtern fallen so weg.
            var inFolge = 0
            // Die letzten Sekunden Ton für den Stimm-Fingerabdruck: Das Weckwort ist schon gesagt, wenn es erkannt wird.
            val ring = ShortArray(RING_WERTE)
            var ringPos = 0
            // Verstärkung nur fürs Weckwort: über den Anker dessen Regler, am eingebauten Mikrofon der eigene (nicht beim Anlernen,
            // damit das Stimmprofil aus normalem Ton entsteht). Die Spracheingabe danach öffnet ein eigenes Mikrofon ohne Verstärkung.
            // Am Handy wirkt sie nur auf das, was die Modelle hören: Stille-Filter, Silero, Rauschboden und Stimm-Fingerabdruck
            // bekommen den unverstärkten Ton (sonst schliefe das Weckwort in ruhigen Räumen nie und die Stimmerkennung hörte Rauschen).
            var handyStufe = Einstellungen.get(this).handyVerstaerkung.toFloat()
            fun weckVerstaerkung(): Float = if (ueberAnker) Einstellungen.get(this).ankerVerstaerkung.toFloat() else if (Stimmprofil.lerntAn) 1f else handyStufe
            var verstaerkung = weckVerstaerkung()
            // Diagnose-Mitschnitt (nur per adb angestoßen): unverstärkter Ton und je Block, was der Filter und der Spotter taten.
            mitschnitt = WeckwortMitschnitt.oeffnen(this)
            // Rückstand gegenüber der Uhr: Wächst er, kommt der Faden mit dem Lesen nicht nach und der Puffer läuft über.
            var uhrStart = SystemClock.elapsedRealtime()
            var gelesenWerte = 0L
            // Der Ring hält den unverstärkten Ton. Über den Anker hebt [letzterTon] ihn nur linear an (in Gleitkomma, ohne Begrenzer):
            // Der weiche Begrenzer der Verstärkung verzerrte laute Rufe, und die Stimmerkennung wies Frank dann ab (Stimme 0,43 statt
            // um 0,6 am Handy).
            fun letzterTon(): FloatArray {
                val n = TON_WERTE
                val ton = FloatArray(n) { i -> ring[(ringPos - n + i + RING_WERTE) % RING_WERTE] / 32768f }
                if (ueberAnker && verstaerkung > 1f) {
                    var spitze = 0f
                    for (w in ton) spitze = maxOf(spitze, kotlin.math.abs(w))
                    val faktor = if (spitze > 0f) minOf(verstaerkung, TON_SPITZE / spitze) else 1f
                    if (faktor > 1f) for (i in ton.indices) ton[i] *= faktor
                }
                return ton
            }
            // Handy-Verstärkung für das „Hey Jarvis“-Modell: gehalten (fällt bei lauter Spitze, steigt langsam), die Phrase verstärkt selbst.
            val halten = GehalteneVerstaerkung()
            /** Ein früherer Block aus dem Ring (1 = der Block vor dem gerade gelesenen), verstärkt wie beim Lesen (am Handy roh). */
            fun frueherBlock(zurueck: Int): ShortArray {
                val start = ringPos - (zurueck + 1) * WeckwortErkenner.BLOCK
                val b = ShortArray(WeckwortErkenner.BLOCK) { i -> ring[((start + i) % RING_WERTE + RING_WERTE) % RING_WERTE] }
                if (ueberAnker && verstaerkung > 1f) verstaerken(b, verstaerkung)
                return b
            }
            // Lief das „Hey Jarvis“-Modell im letzten Block? Springt es wieder an, fängt es frisch an.
            var lautLief = true
            // Stille-Filter: Rauschboden (fällt sofort, steigt nur langsam) und Blöcke in Folge, die nicht darüber liegen.
            var boden = -1f
            var stilleBloecke = 0
            var uebersprungen = 0
            // Silero für den Stille-Filter: die zwei vollen Rahmen (je 512 Werte) eines Blocks. Lief Silero im Block davor
            // nicht, fängt es frisch an, damit kein Zustand aus älterem Ton nachwirkt.
            val vadRahmen = FloatArray(SprecherErkenner.VAD_RAHMEN)
            var vadLief = false
            /** Hört Silero in diesem Block eine Stimme? */
            fun stimmeImBlock(v: SileroVad): Boolean {
                if (!vadLief) v.zuruecksetzen()
                var stimme = false
                for (r in 0 until block.size / vadRahmen.size) {
                    for (i in vadRahmen.indices) vadRahmen[i] = block[r * vadRahmen.size + i] / 32768f
                    if (v.wahrscheinlichkeit(vadRahmen) >= VAD_STIMME) stimme = true
                }
                return stimme
            }
            /** Je 32-ms-Rahmen des letzten Tons: Stimme ja/nein, für den Stimm-Fingerabdruck. null ohne Silero. */
            fun stimmMaske(ton: FloatArray): BooleanArray? {
                val v = vad ?: return null
                return runCatching {
                    v.zuruecksetzen()
                    val r = SprecherErkenner.VAD_RAHMEN
                    BooleanArray(ton.size / r) { i -> System.arraycopy(ton, i * r, vadRahmen, 0, r); v.wahrscheinlichkeit(vadRahmen) >= VAD_MASKE }
                        .also { vadLief = false }
                }.getOrNull()
            }
            /** Fingerabdruck der letzten Sekunden: nur die Stimme darin, wenn Silero da ist (hilft bei Lärm). */
            fun fingerabdruck(): FloatArray? {
                val s = spr ?: return null
                val ton = letzterTon()
                return runCatching { s.fingerabdruck(ton, stimmMaske(ton)) }.getOrNull()
            }
            // Knapp an der Stimme gescheitert: Wann, damit ein gleich wiederholter Ruf als Nachdruck durchgeht.
            var knappUm = 0L
            // Blöcke seit dem letzten Urteil über einen Ruf (Treffer, Abweisung, Anlern-Probe): Der Vorlauf eines Frischstarts darf
            // diesen schon beurteilten Ruf nicht noch einmal durch den Spotter schicken.
            var seitUrteil = Int.MAX_VALUE / 2
            // Alle 50 Blöcke (4 s): Anker dazugekommen, Strecke abgerissen oder fremde Musik? Dann neu anfangen.
            // Am Handy gestartet (etwa gleich nach einem Gespräch, solange der Anker noch umschaltet): schon nach 1 s statt 4 s
            // erneut nach dem Anker sehen. Im Mitschnitt hörte das Weckwort nach jedem Gespräch 5 s nur übers Handy und
            // verpasste dort Rufe vom Sofa.
            var pruefZaehler = if (ueberAnker) 0 else 50 - ANKER_ERSTPRUEFUNG
            while (an.get()) {
                if (++pruefZaehler >= 50) {
                    pruefZaehler = 0
                    handyStufe = Einstellungen.get(this).handyVerstaerkung.toFloat()
                    if (ueberAnker) verstaerkung = weckVerstaerkung()
                    val wechsel = if (ueberAnker) {
                        val abgerissen = !de.frank.jarvis.audio.KonferenzMikrofon.kanalSteht(this)
                        if (abgerissen) ankerSperreBis = SystemClock.elapsedRealtime() + 30_000L
                        abgerissen || !Einstellungen.get(this).ankerWeckwort || Stimmprofil.lerntAn || Weckwort.nurHandy || telefoniert() || de.frank.jarvis.audio.AnkerVerbindung.fremdesAudioGeraet(this) != null || getSystemService(android.media.AudioManager::class.java)?.isMusicActive == true
                    } else ankerMoeglich()
                    if (wechsel) { neuStart = true; break }
                }
                var gelesen = 0
                while (gelesen < block.size && an.get()) {
                    val n = aufnahme.read(block, gelesen, block.size - gelesen)
                    if (n <= 0) throw IllegalStateException("Mikrofon liefert $n")
                    gelesen += n
                }
                if (!an.get()) break
                seitUrteil++
                gelesenWerte += block.size
                val rueckstand = SystemClock.elapsedRealtime() - uhrStart - gelesenWerte * 1000 / WeckwortErkenner.ABTASTRATE
                if (rueckstand > RUECKSTAND_MS) {
                    Logbuch.system("Weckwort kommt nicht nach", "${rueckstand} ms Ton verloren oder verspätet (Last auf dem Handy)")
                    uhrStart = SystemClock.elapsedRealtime(); gelesenWerte = 0L
                }
                for (w in block) { ring[ringPos] = w; ringPos = (ringPos + 1) % RING_WERTE }
                mitschnitt = mitschnitt?.takeIf { it.ton(block) }
                // Am Handy je Block: beim Anlernen unverstärkt, danach sofort wieder der Regler.
                if (!ueberAnker) verstaerkung = weckVerstaerkung()
                val handyFaktor = if (!ueberAnker && verstaerkung > 1f) verstaerkung else 1f
                // Über den Anker leise Rufe hörbar machen: verstärken, Spitzen weich begrenzen (kein hartes Übersteuern).
                if (ueberAnker && verstaerkung > 1f) verstaerken(block, verstaerkung)
                val anlernen = Stimmprofil.lerntAn
                // Mit angelernter Stimmerkennung springt das Weckwort leichter an (auch aus fünf Metern); Fremde
                // weist danach die Stimmerkennung ab. Beim Anlernen so leicht wie möglich.
                val mitStimme = Stimmprofil.stimmeAktiv && spr != null
                // „Hey Jarvis“-Modell: Seine Merkmale braucht das Stimmprofil auch bei eigener Phrase. Entscheidet bei
                // eigener Phrase allein die Stimmerkennung, ruht es (spart rund um die Uhr Akku).
                val brauchtLaut = phr == null || anlernen || !mitStimme
                if (brauchtLaut && !lautLief) erk.zuruecksetzen()
                lautLief = brauchtLaut
                // In längerer Stille (über 4 s nichts über dem Rauschboden) ruhen die Modelle. Großzügig eingestellt:
                // Auch leise Sprache aus fünf Metern liegt darüber. Der Ring läuft weiter.
                val rms = lautstaerke(block)
                boden = if (boden < 0f || rms < boden) rms else boden + (rms - boden) * BODEN_ANSTIEG
                // Bei gleichmäßigem Lärm (Auto, Lüfter, Straße) steigt der Rauschboden mit, eine Stimme aus der Ferne
                // liegt kaum darüber: Dann entscheidet Silero, ob jemand spricht, statt die Modelle schlafen zu legen.
                val laut = rms > maxOf(boden * STILLE_FAKTOR, STILLE_MIN / handyFaktor)
                val mitVad = !laut && boden > LAERM_BODEN && vad != null
                val stimmeImLaerm = mitVad && runCatching { stimmeImBlock(vad!!) }.getOrDefault(false)
                vadLief = mitVad
                val stilleVorher = stilleBloecke
                if (laut || stimmeImLaerm) stilleBloecke = 0 else stilleBloecke++
                if (!anlernen && stilleBloecke > STILLE_BLOECKE) {
                    uebersprungen++
                    inFolge = 0
                    phr?.ruhen(boden)
                    mitschnitt?.zeile(rms, boden, laut, stimmeImLaerm, true, phr?.verstaerkung, ueberAnker, false)
                    // Kein Hintergrund-Lernen in der Stille: Das Modell ruht, seine Merkmale stammen vom letzten Block davor und hätten
                    // über eine lange Stille (Nacht) den Hintergrund ganz auf diesen einen Vektor gezogen.
                    continue
                }
                // Mit Stimmerkennung auch die Aussprache-Varianten (schnell, verschliffen, mit Akzent): Fremde fallen
                // danach an der Stimme durch, Frank soll die Phrase sagen dürfen, wie sie ihm über die Lippen geht.
                // Vor dem Nachholen einstellen: Eine Änderung legt einen neuen Strom an und verwürfe sonst den nachgeholten Anfang.
                when {
                    phr == null -> Unit
                    // Beim Anlernen mit allen Aussprache-Varianten und so leicht wie möglich: Sonst kommen nur
                    // deutliche Proben ins Profil, und später scheitern gerade die schnellen, leisen Rufe daran.
                    anlernen -> phr.einstellen(PhrasenErkenner.SCHWELLE_ANLERNEN, breit = true)
                    mitStimme -> phr.einstellen((phrasenSchwelle - PhrasenErkenner.SPRECHER_BONUS).coerceAtLeast(PhrasenErkenner.SCHWELLE_MIN), breit = true)
                    else -> phr.einstellen(phrasenSchwelle, breit = false)
                }
                // Die Phrase bekommt den rohen Ton und hebt ihn in ihrer eigenen, gehaltenen Verstärkung um den Regler mit an.
                phr?.zusatz = handyFaktor
                var nachgeholt = false
                // Nach gut 1 s Stille fängt der Spotter ebenfalls frisch an (sherpa-onnx 1.13.8 setzt seinen Strom nach 1,5 s Stille
                // selbst zurück und verliert dabei den Anfang des nächsten Rufs, Almanach §16). Im Mitschnitt holte das einen
                // deutlich gerufenen, sonst verpassten Ruf zurück, ohne einen anderen zu verlieren.
                val frisch = uebersprungen > 0 || (phr != null && (laut || stimmeImLaerm) && stilleVorher >= FRISCH_NACH_STILLE)
                val nachgeholtBloecke = if (!frisch) 0 else if (phr != null) minOf(NACHHOLEN, seitUrteil - 1) else minOf(uebersprungen, NACHHOLEN)
                if (frisch) {
                    // Wortanfang nachholen: die zuletzt übersprungenen Blöcke vor diesem durch die Modelle schicken.
                    // Der Spotter fängt dafür frisch an: Sein Strom stammt von vor der Stille, und sherpa-onnx 1.13.8
                    // setzt ihn in Stille von selbst zurück und verliert dabei den Anfang eines Rufs (Almanach §16).
                    phr?.zuruecksetzen()
                    // Der frische Spotter bekommt immer die vollen 2 s Vorlauf aus dem Ring, auch wenn der Filter kürzer schlief:
                    // Mit nur gut 1 s Vorlauf verpasste er im Mitschnitt einen deutlichen Ruf, mit 2 s nicht. „Hey Jarvis“ bekommt
                    // nur, was es noch nicht gehört hat.
                    for (k in (if (phr != null) minOf(NACHHOLEN, seitUrteil - 1) else minOf(uebersprungen, NACHHOLEN)) downTo 1) {
                        val frueher = frueherBlock(k)
                        if (brauchtLaut && k <= uebersprungen) erk.bewerte(halten.anwenden(frueher, handyFaktor, nachfuehren = false))
                        // Ein Treffer schon im nachgeholten Ton zählt: Der Spotter hat danach zurückgesetzt und meldet ihn nicht noch einmal.
                        if (phr?.erkannt(frueher, boden) == true) nachgeholt = true
                    }
                    uebersprungen = 0
                }
                val wert = if (brauchtLaut) erk.bewerte(halten.anwenden(block, handyFaktor)) else 0f
                val merkmale = if (brauchtLaut) erk.letzteMerkmale else null
                if (phr != null) {
                    val spotterTreffer = nachgeholt || phr.erkannt(block, boden)
                    mitschnitt?.zeile(rms, boden, laut, stimmeImLaerm, false, phr.verstaerkung, ueberAnker, spotterTreffer, nachgeholtBloecke)
                    if (!spotterTreffer) {
                        if (merkmale != null && wert < 0.1f && !ueberAnker && handyFaktor <= 1f) Stimmprofil.hintergrundLernen(merkmale)
                        continue
                    }
                } else {
                    // Beim Anlernen etwas großzügiger, damit auch Franks deutsche Aussprache Proben liefert.
                    val schwelle = when {
                        anlernen -> minOf(Weckwort.schwelle, 0.3f)
                        mitStimme -> Weckwort.schwelle * 0.7f
                        else -> Weckwort.schwelle
                    }
                    if (wert < schwelle) {
                        inFolge = 0
                        if (merkmale != null && wert < 0.1f && !ueberAnker && handyFaktor <= 1f) Stimmprofil.hintergrundLernen(merkmale)
                        continue
                    }
                    // Schnell gesprochen ist der Gipfel oft nur einen Block (80 ms) breit: Beim Anlernen und mit
                    // Stimmerkennung genügt einer, sonst kämen schnelle Rufe nie an (und nie als Probe ins Profil).
                    val noetig = if (anlernen || mitStimme) 1 else BLOCKS_IN_FOLGE
                    if (++inFolge < noetig) continue
                    inFolge = 0
                }
                val modellwert = if (phr == null) wert else null
                seitUrteil = 0
                if (anlernen) {
                    if (merkmale != null) {
                        val fingerabdruck = fingerabdruck()
                        Log.i(TAG, "Probe fürs Stimmprofil (${"%.2f".format(wert)}), Stimm-Fingerabdruck: ${fingerabdruck != null}")
                        val fertig = Stimmprofil.probe(merkmale, fingerabdruck)
                        bereich.launch { piepKurz(); if (fertig) { delay(150); piepKurz() } }
                    }
                    // Neu anfangen: Die nächste Probe soll ein neues „Hey Jarvis“ sein, nicht der Rest von diesem.
                    // Bei eigener Phrase bleibt „Hey Jarvis“ stehen: Sein Fenster gleitet ohnehin, und ohne Anlauf
                    // fehlen der nächsten Probe nicht 1,3 s lang die Lautmerkmale.
                    if (phr == null) erk.zuruecksetzen()
                    phr?.zuruecksetzen()
                    continue
                }
                // Phrase erkannt, aber die Lautmerkmale fehlen noch (Anlauf): Mit Profil nicht ungeprüft durchlassen.
                if (phr != null && !mitStimme && Stimmprofil.aktiv && merkmale == null) {
                    phr.zuruecksetzen()
                    continue
                }
                val pruefung = Stimmprofil.pruefe(merkmale)
                // Mit Stimmerkennung entscheidet allein sie: Sie erkennt die Person, auch aus fünf Metern, wo die
                // Lautmerkmale schon stark abweichen. Zu wenig Sprache im Ton zählt als „nicht Frank“.
                val stimme = if (mitStimme) fingerabdruck().let { Stimmprofil.pruefeStimme(it, spr?.letzteDauer) } else null
                // Nachdruck: Zwei knappe Abweisungen kurz hintereinander sind Frank, der es noch einmal sagt (schnell,
                // heiser, im Lärm). Fremde und Fernseher sagen das Weckwort kaum zweimal in wenigen Sekunden.
                // Bis 1.8.902 war das Fenster leer (Schwelle = Boden 0,45 = Untergrenze des Nachdrucks): Kein Ruf zählte je als knapp.
                // Jetzt zählt ein kurzer Einzelruf ab [NACHDRUCK_UNTEN]; Fernseher und Fremde reden meist länger am Stück.
                val knapp = stimme != null && !stimme.ok && stimme.aehnlich >= NACHDRUCK_UNTEN && (stimme.dauer ?: Float.MAX_VALUE) <= Stimmprofil.KURZ_RUF_S
                val jetzt = SystemClock.elapsedRealtime()
                // Mindestabstand: Sonst urteilt das gleitende „Hey Jarvis“-Fenster nach dem Abklingen über denselben Ruf noch einmal.
                val nachdruck = knapp && jetzt - knappUm in NACHDRUCK_ABSTAND_MS..NACHDRUCK_MS
                val ok = if (mitStimme) stimme?.ok == true || nachdruck else pruefung?.ok != false
                if (!ok) {
                    if (knapp) knappUm = jetzt
                    Log.i(TAG, "Weckwort ($art) abgewiesen, klingt nicht nach Frank (${"%.2f".format(wert)})")
                    WeckwortProtokoll.treffer(this, art, modellwert, pruefung, if (knapp) "knapp abgewiesen" else "abgewiesen", stimme, mitStimme, phr?.letzterTreffer)
                    Weckwort.abgewiesen(modellwert ?: 1f)
                    // Neu anfangen: Ein zweites Urteil über denselben Ruf darf nicht als Nachdruck zählen.
                    if (phr == null) erk.zuruecksetzen()
                    phr?.zuruecksetzen()
                    continue
                }
                knappUm = 0L
                // Inzwischen angehalten (die Antwort kam während der Prüfung, Denkpause vorbei): nicht mehr rufen, sonst bräche der
                // neue Ruf die eben beginnende Antwort ab.
                if (!an.compareAndSet(true, false)) {
                    Logbuch.system("Weckwort verworfen", "Ruf kam, als das Lauschen schon endete")
                    break
                }
                Log.i(TAG, "Weckwort ($art) erkannt (${"%.2f".format(wert)})${if (nachdruck) ", Nachdruck" else ""}")
                WeckwortProtokoll.treffer(this, art, modellwert, pruefung, if (nachdruck) "gerufen (Nachdruck)" else "gerufen", stimme, mitStimme, phr?.letzterTreffer)
                Weckwort.erkannt(modellwert ?: 1f)
                // Hat die Stimmerkennung Frank bestätigt, war es kein Fehlalarm, auch wenn danach nichts kommt: Dann
                // gibt es keine Merkmale zum Lernen, sonst sperrt ein solcher Eintrag später Franks eigene Rufe aus.
                // Mit Handy-Verstärkung stammen die Lautmerkmale aus angehobenem Ton: nicht als möglichen Fehlalarm lernen.
                Weckwort.trefferMerkmale = if (stimme?.ok == true || handyFaktor > 1f) null else merkmale
                // Hört das Gespräch ebenfalls über den Anker zu, bleibt die Strecke stehen; sonst zurück ins Medien-Profil.
                uebergabe = ueberAnker && Einstellungen.get(this).konferenzMikrofonAn
                if (ueberAnker) Logbuch.system("Weckwort über den Anker erkannt", if (uebergabe) "Gespräch übernimmt die Anker-Mikrofone" else "zurück ins Medien-Profil")
                bereich.launch { rufen() }
                break
            }
        } catch (fehler: Exception) {
            Log.w(TAG, "Lauschen unterbrochen", fehler)
            if (lauschFehler++ % 100 == 0) Logbuch.fehler("Weckwort: Lauschen unterbrochen", fehler)
            // Scheiterte es über den Anker, eine Weile am Handy lauschen.
            if (ueberAnker) ankerSperreBis = SystemClock.elapsedRealtime() + 60_000L
            // Nach einem Fehler (Mikrofon belegt, Lesefehler) nach einer Pause neu versuchen.
            if (an.get()) bereich.launch {
                delay(3_000)
                if (lauf === an) { faden = null; lauschen() }
            }
        } finally {
            mitschnitt?.schliessen()
            runCatching { aufnahme?.stop() }
            runCatching { aufnahme?.release() }
            if (ueberAnker) {
                if (uebergabe) de.frank.jarvis.audio.KonferenzMikrofon.uebergeben(this)
                else de.frank.jarvis.audio.KonferenzMikrofon.freigeben(this, de.frank.jarvis.audio.KonferenzMikrofon.WECKWORT)
            }
            if (neuStart && an.get()) bereich.launch {
                delay(300)
                if (lauf === an) { faden = null; lauschen() }
            }
            // Läuft schon ein neuer Durchgang, gehört die Anzeige ihm.
            val aktuell = lauf
            if (aktuell == null || aktuell === an) Weckwort.hoertZu(false)
        }
    }

    /**
     * Verstärkt einen Block um [faktor]; über [KNIE] werden Spitzen weich zusammengedrückt, damit laute Rufe nicht hart
     * übersteuern (das verfälschte Weckwort-Modell und Stimmerkennung mehr als leiser Ton).
     */
    private fun verstaerken(block: ShortArray, faktor: Float) {
        for (i in block.indices) {
            val y = block[i] * faktor
            val betrag = kotlin.math.abs(y)
            val begrenzt = if (betrag <= KNIE) betrag else KNIE + (32_767f - KNIE) * (1f - kotlin.math.exp(-(betrag - KNIE) / (32_767f - KNIE)))
            block[i] = (if (y < 0) -begrenzt else begrenzt).toInt().coerceIn(-32_767, 32_767).toShort()
        }
    }

    /** Effektivwert eines Blocks in 16-Bit-Werten (für den Stille-Filter). */
    private fun lautstaerke(block: ShortArray): Float {
        var summe = 0.0
        for (w in block) summe += w.toDouble() * w
        return sqrt(summe / block.size).toFloat()
    }

    /** Bestätigt beim Anlernen jede Probe mit einem Ton, laut genug für fünf Meter; am Ende zweimal. */
    private suspend fun piepKurz() {
        val ton = runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100) }.getOrNull() ?: return
        try {
            ton.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 220)
            delay(300)
        } finally {
            ton.release()
        }
    }

    /**
     * Nach „Hey Jarvis“: Jarvis sofort holen. Die kurze Schutzpause fürs Audiosystem (es gibt das Mikrofon verzögert
     * frei) wartet [Weckwort.mikrofonHolen] ab, sie läuft so parallel zum Start der App.
     */
    private suspend fun rufen() {
        Weckwort.rufen()
        anhalten()
        // Jarvis nicht vorn und das Handy entsperrt: Gespräch im Hintergrund, oben nur die Sprachblase, die App bleibt zu.
        // Das Mikrofon darf der Prozess über diesen Dienst (Mikrofon-Typ, im Vordergrund) weiter nutzen.
        if (!MainActivity.sichtbar && de.frank.jarvis.ui.SprachBlase.darf(this) &&
            !getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked
        ) {
            val gestartet = runCatching { de.frank.jarvis.ui.SprachBlase.sprachmodusStarten(this) }
                .onFailure { Logbuch.fehler("Weckwort: Sprachblase ließ sich nicht starten", it) }
                .getOrDefault(false)
            if (gestartet) return
        }
        val absicht = rufAbsicht()
        val direkt = de.frank.jarvis.Hintergrundstart.ueberlagernErlaubt(this)
        if (direkt) {
            runCatching { startActivity(absicht) }.onFailure {
                Log.w(TAG, "Jarvis ließ sich nicht öffnen", it)
                Logbuch.fehler("Weckwort: Jarvis ließ sich nicht öffnen", it)
                zumAntippen(absicht)
            }
        } else {
            // Ohne die Erlaubnis bleibt der Ruf als Benachrichtigung; blockt Android still, übernimmt der Wächter.
            zumAntippen(absicht)
        }
    }

    private fun rufAbsicht(): Intent = Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(EXTRA_WECKWORT, true)
        .putExtra(Weckwort.EXTRA_RUF_CODE, Weckwort.rufCode)
        // Vor dem Öffnen festhalten, ob Jarvis schon vorn war: Danach ist die App ja immer sichtbar.
        .putExtra(EXTRA_WAR_SICHTBAR, MainActivity.sichtbar)

    /** @param vollbild false für den Wächter: Die Meldung soll die womöglich schon startende App nicht erneut öffnen. */
    private fun zumAntippen(absicht: Intent, vollbild: Boolean = true) {
        Weckwort.mikrofonFreigeben()
        // Unter Einstellungen → Benachrichtigungen abgeschaltet: kein Ruf zum Antippen.
        val profil = Meldungen.profil(this, Meldungsarten.RUF)
        if (!profil.an) return
        val tippen = PendingIntent.getActivity(this, 31, absicht, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val darfDirekt = de.frank.jarvis.Hintergrundstart.ueberlagernErlaubt(this)
        val text = if (darfDirekt) "Tippen, um mit Jarvis zu sprechen."
        else "Tippen, um mit Jarvis zu sprechen. Für die Sprachblase über anderen Apps bei Jarvis „Über anderen Apps einblenden“ erlauben."
        val meldung = NotificationCompat.Builder(this, KANAL_RUF)
            .setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle("Hey Jarvis")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(tippen)
            .apply { if (vollbild) setFullScreenIntent(tippen, true) }
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setSilent(!profil.ton)
        val nm = getSystemService(NotificationManager::class.java)
        Meldungen.zeige(this, Meldungsarten.RUF, RUF_ID, meldung, "Hey Jarvis", text)
        // Übernimmt die App danach doch noch, verschwindet die dann falsche „Tippen“-Meldung.
        Weckwort.rufMeldungGezeigt { runCatching { nm.cancel(RUF_ID) } }
    }

    private fun hinweis(): Notification = baueHinweis(this)

    override fun onDestroy() {
        laeuft = false
        anhaltenSofort()
        bereich.cancel()
        // Modelle erst freigeben, wenn der letzte Durchgang zu Ende ist; darauf nicht auf dem Hauptfaden warten.
        val alt = letzterFaden
        letzterFaden = null
        Thread({
            runCatching { alt?.join(3_000) }
            synchronized(modellSperre) {
                runCatching { erkenner?.close() }
                erkenner = null
                runCatching { phrasenErkenner?.close() }
                phrasenErkenner = null
                runCatching { sprecherErkenner?.close() }
                sprecherErkenner = null
                runCatching { vad?.close() }
                vad = null
            }
        }, "Weckwort-Ende").start()
        runCatching { if (wach?.isHeld == true) wach?.release() }
        Weckwort.hoertZu(false)
        Weckwort.aktivesWort(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_WECKWORT = "weckwort"
        const val EXTRA_WAR_SICHTBAR = "war_sichtbar"
        private const val TAG = "Weckwort"
        private const val KANAL = "jarvis_weckwort"
        private const val KANAL_RUF = "jarvis_weckwort_ruf"
        /** Läuft der Dienst im Vordergrund (für [hinweisErneuern]). */
        @Volatile private var laeuft = false

        /**
         * Der Dienst hält das Mikrofon im Vordergrund (Typ Mikrofon). Nur dann darf Jarvis aus dem Hintergrund oder bei gesperrtem
         * Handy wieder zuhören (Android gibt das Mikrofon sonst nur der sichtbaren App), etwa nach einer Sprachzustellung.
         */
        val imVordergrund: Boolean get() = laeuft

        /** Mit dem gewählten Weckwort im Text; wegwischbar (Einstellungen → Benachrichtigungen) nicht fest. */
        private fun baueHinweis(context: Context): Notification {
            val e = Einstellungen.get(context)
            val wort = if (e.weckwortArt == "phrase") e.weckwortPhrase else "Hey Jarvis"
            val minimiert = Meldungen.profil(context, Meldungsarten.WECKWORT_HOERT).minimiert
            val oeffnen = PendingIntent.getActivity(
                context, 30, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, KANAL)
                .setSmallIcon(R.drawable.ic_stat_jarvis)
                .setContentTitle("Jarvis hört auf „$wort“")
                .setContentText("Einstellen unter Einstellungen → Benachrichtigungen")
                .setOngoing(!minimiert)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(oeffnen)
                .build()
        }

        /**
         * Nach „Minimiert“ oder neuem Weckwort: die laufende Meldung sofort erneuern. [nurSichtbare] = eine weggewischte
         * (wegwischbare) Meldung nicht zurückholen, etwa beim bloßen Neuladen des Weckworts.
         */
        fun hinweisErneuern(context: Context, nurSichtbare: Boolean = false) {
            if (!laeuft) return
            val app = context.applicationContext
            val nm = app.getSystemService(NotificationManager::class.java)
            if (nurSichtbare && Meldungen.profil(app, Meldungsarten.WECKWORT_HOERT).minimiert &&
                runCatching { nm.activeNotifications.none { it.id == HINWEIS_ID } }.getOrDefault(true)) return
            kanaeleAnlegen(app)
            runCatching { nm.notify(HINWEIS_ID, baueHinweis(app)) }
        }
        private const val HINWEIS_ID = 4300
        private const val RUF_ID = 4301
        private const val BLOCKS_IN_FOLGE = 2
        /** Ab so viel Rückstand beim Lesen meldet das Logbuch, dass der Faden nicht nachkommt (der Puffer hält 2 s). */
        private const val RUECKSTAND_MS = 1_000L
        /** Spitzenpegel, auf den der Ton für den Stimm-Fingerabdruck über den Anker höchstens linear angehoben wird. */
        private const val TON_SPITZE = 0.9f

        /**
         * Empfindlichkeit geändert (App oder Jarvis per Sprache): Schwellen sofort übernehmen; die eigene Phrase bekommt
         * ihre Schwelle beim Anlegen, dafür liest der Dienst neu ein.
         */
        fun empfindlichkeitUebernehmen(context: Context) {
            val e = Einstellungen.get(context)
            Weckwort.schwelle = e.weckwortSchwelle
            Stimmprofil.schwellenAbzug = e.weckwortStimmAbzug
            Stimmprofil.kurzerRufBoden = if (e.weckwortEmpfindlichkeit == "sehr_hoch") Stimmprofil.STIMM_BODEN_KURZ else Stimmprofil.STIMM_BODEN
            if (e.weckwortArt == "phrase") neuLaden(context)
        }
        /** Silero-Werte: ab hier zählt ein Rahmen im Lärm als Stimme (Stille-Filter) bzw. für den Fingerabdruck. */
        private const val VAD_STIMME = 0.5f
        private const val VAD_MASKE = 0.3f
        /** Nachdruck: knapp heißt höchstens so weit unter der Stimm-Schwelle, und der zweite Ruf binnen dieser Zeit. */
        private const val NACHDRUCK_MS = 10_000L
        private const val NACHDRUCK_ABSTAND_MS = 1_500L
        /** Untergrenze für einen knappen Ruf: Franks wiederholte leise Rufe lagen ab 0,26, Fremde bis 0,24 (Protokoll bis 22.11.2026). */
        private const val NACHDRUCK_UNTEN = 0.25f
        /** Erst ab diesem Rauschboden (16 Bit, etwa -47 dBFS) ist es Lärm und Silero entscheidet; im ruhigen Raum schlafen die Modelle wie bisher. */
        private const val LAERM_BODEN = 150f
        /**
         * Stille-Filter: so viele Blöcke (je 80 ms, also 4 s) ohne Laut, dann ruhen die Modelle; beim Aufwachen so viele
         * nachholen (2 s: Die Phrase dauert bis gut 2 s, leise gesprochen wacht der Filter erst spät in ihr auf). Der Ring hält 2,5 s; so fehlt den Modellen auch ein leiser Anlaut („Hey“) nicht, der
         * deutlich vor der ersten lauten Silbe liegt.
         */
        private const val STILLE_BLOECKE = 50
        private const val NACHHOLEN = 25
        /** Blöcke (je 80 ms) bis zur ersten Anker-Prüfung nach einem Start am Handy. */
        private const val ANKER_ERSTPRUEFUNG = 12
        /** Blöcke (je 80 ms) Stille, nach denen der Spotter beim nächsten Laut frisch mit 2 s Vorlauf anfängt. */
        private const val FRISCH_NACH_STILLE = 12
        /**
         * Laut ist ein Block etwa 2 dB über dem Rauschboden, mindestens aber über einem sehr niedrigen Pegel (16 Bit,
         * etwa -65 dBFS): Ohne Verstärkung liegt leise Sprache aus mehreren Metern nur knapp darüber.
         */
        private const val STILLE_FAKTOR = 1.25f
        private const val STILLE_MIN = 18f
        /** Ab hier begrenzt die Anker-Verstärkung weich. */
        private const val KNIE = 20_000f
        /**
         * So lange darf die App nach „Hey Jarvis“ zum Übernehmen brauchen, bevor der Wächter den Ruf als Meldung zeigt.
         * Großzügig, weil ein Kaltstart bei gesperrtem Handy unter Last mehrere Sekunden dauern kann.
         */
        private const val WAECHTER_MS = 8_000L
        private const val BODEN_ANSTIEG = 0.001f
        /** Ringpuffer 2,5 s; davon gehen die letzten 2,2 s (das Weckwort samt Rand) in den Stimm-Fingerabdruck. */
        private const val RING_WERTE = WeckwortErkenner.ABTASTRATE * 5 / 2
        private const val TON_WERTE = WeckwortErkenner.ABTASTRATE * 22 / 10
        private const val AKTION_NEU_LADEN = "de.frank.jarvis.weckwort.NEU_LADEN"
        private const val AKTION_NEU_STARTEN = "de.frank.jarvis.weckwort.NEU_STARTEN"

        /** Neuer Durchgang ohne neu geladene Modelle (für den Diagnose-Mitschnitt). */
        fun neuStarten(context: Context) {
            val app = context.applicationContext
            if (!laeuft || !Einstellungen.get(app).weckwortAn || !mikrofonErlaubt(app)) return
            runCatching { app.startService(Intent(app, WeckwortDienst::class.java).setAction(AKTION_NEU_STARTEN)) }
                .onFailure { Log.w(TAG, "Neu starten nicht möglich", it) }
        }

        /** Weckwort, Phrase oder Empfindlichkeit geändert: Der laufende Dienst liest sie neu ein. Nur aus der sichtbaren App. */
        fun neuLaden(context: Context) {
            val app = context.applicationContext
            if (!Einstellungen.get(app).weckwortAn || !mikrofonErlaubt(app)) return
            runCatching { ContextCompat.startForegroundService(app, Intent(app, WeckwortDienst::class.java).setAction(AKTION_NEU_LADEN)) }
                .onFailure { Log.w(TAG, "Neu laden nicht möglich", it) }
        }

        /** Zuletzt unsichtbar gestartet (gegen eine Schleife, falls Android den Dienst immer wieder ablehnt). */
        @Volatile private var zuletztUnsichtbar = 0L

        fun mikrofonErlaubt(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        fun kanaeleAnlegen(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(KANAL, "Hey Jarvis", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Zeigt, dass Jarvis auf „Hey Jarvis“ hört."
                    setShowBadge(false)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(KANAL_RUF, "Hey Jarvis: Ruf", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Meldet „Hey Jarvis“, wenn Jarvis nicht von selbst nach vorn kommen darf."
                },
            )
            // Der frühere Kanal „minimiert“ (1.8.151) wirkte bei Vordergrund-Diensten nicht.
            runCatching { nm.deleteNotificationChannel("jarvis_weckwort_min") }
        }

        /** Nur aufrufen, während Jarvis sichtbar ist: Dann darf Android den Mikrofon-Dienst starten. */
        fun abgleichen(context: Context) {
            val app = context.applicationContext
            val absicht = Intent(app, WeckwortDienst::class.java)
            if (Einstellungen.get(app).weckwortAn && mikrofonErlaubt(app)) {
                runCatching { ContextCompat.startForegroundService(app, absicht) }.onFailure { Log.w(TAG, "Start nicht möglich", it) }
            } else {
                app.stopService(absicht)
            }
        }

        /**
         * Nach Neustart oder Update: Den Mikrofon-Dienst darf nur eine sichtbare App starten. Mit „Über anderen Apps
         * einblenden“ öffnet sich Jarvis dafür einen Augenblick unsichtbar ([Starter]); sonst eine Benachrichtigung.
         */
        fun nachNeustart(context: Context) {
            val app = context.applicationContext
            if (!Einstellungen.get(app).weckwortAn || !mikrofonErlaubt(app)) return
            val jetzt = SystemClock.elapsedRealtime()
            val unsichtbar = Build.VERSION.SDK_INT >= 29 && Settings.canDrawOverlays(app) && jetzt - zuletztUnsichtbar > 60_000
            if (unsichtbar) {
                zuletztUnsichtbar = jetzt
                val ok = runCatching { app.startActivity(Intent(app, Starter::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)) }.isSuccess
                if (ok) return
            }
            if (!Meldungen.an(app, Meldungsarten.WECKWORT_AUS)) return
            kanaeleAnlegen(app)
            val tippen = PendingIntent.getActivity(
                app, 32, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE,
            )
            val meldung = NotificationCompat.Builder(app, KANAL)
                .setSmallIcon(R.drawable.ic_stat_jarvis)
                .setContentTitle("„Hey Jarvis“ ist aus")
                .setContentText("Tippen, um es wieder einzuschalten.")
                .setContentIntent(tippen)
                .setAutoCancel(true)
            Meldungen.zeige(app, Meldungsarten.WECKWORT_AUS, RUF_ID, meldung, "Hey Jarvis ist aus", "Tippen, um das Weckwort wieder einzuschalten.")
        }
    }

    /** Unsichtbare Durchgangs-Seite: Solange sie sichtbar ist, darf der Mikrofon-Dienst starten. */
    class Starter : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            overridePendingTransition(0, 0)
        }

        override fun onResume() {
            super.onResume()
            abgleichen(this)
            finish()
            overridePendingTransition(0, 0)
        }
    }
}

/**
 * Handy-Verstärkung fürs Weckwort-Modell: fällt bei einer lauten Spitze sofort auf das, was ohne Übersteuern geht, und steigt
 * danach langsam (gut 2 s) zum Regler zurück. Anders als eine Normierung je Block bleibt so der Lautstärkeverlauf eines Rufs
 * erhalten, und es entstehen keine Sprünge an den Blockgrenzen.
 */
private class GehalteneVerstaerkung {
    private var g = -1f
    private var aus = ShortArray(WeckwortErkenner.BLOCK)

    /** Liefert den verstärkten Block (eigener Puffer, gilt bis zum nächsten Aufruf) oder bei [ziel] ≤ 1 den Block selbst. */
    fun anwenden(block: ShortArray, ziel: Float, nachfuehren: Boolean = true): ShortArray {
        if (ziel <= 1f) { g = -1f; return block }
        if (g < 0f || g > ziel) g = ziel
        var spitze = 0
        for (w in block) spitze = maxOf(spitze, kotlin.math.abs(w.toInt()))
        var f = if (nachfuehren) g + (ziel - g) * ANSTIEG else g
        if (spitze > 0 && spitze * f > GRENZE) f = maxOf(1f, GRENZE / spitze)
        if (nachfuehren) g = f
        if (aus.size != block.size) aus = ShortArray(block.size)
        for (i in block.indices) aus[i] = (block[i] * f).toInt().coerceIn(-32_767, 32_767).toShort()
        return aus
    }

    private companion object {
        const val GRENZE = 0.9f * 32_767f
        const val ANSTIEG = 0.03f
    }
}
