package de.frank.jarvis.atem

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.speech.Vorleser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AtemPhase(val anzeige: String) {
    INTRO("Ankommen"), EIN("Einatmen"), HALT_OBEN("Halten"), AUS("Ausatmen"), HALT_UNTEN("Halten"), OUTRO("Nachspüren"), ENDE("Fertig")
}

data class Abschnitt(val phase: AtemPhase, val start: Long, val dauer: Long, val zyklus: Int) {
    val ende: Long get() = start + dauer
}

/**
 * Eine laufende Atemübung. Eine einzige Uhr ([zeit], pausierbar) bestimmt Animation, Ansagen und Geräusche:
 * Alle Einsätze stehen als feste Zeitpunkte im Plan, so driftet auch eine 25-Minuten-Übung nicht.
 * Läuft im App-weiten Bereich weiter, auch bei ausgeschaltetem Bildschirm (Wachhaltung für die Dauer der Übung).
 */
object AtemSitzung {
    private const val TAG = "AtemSitzung"
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    var offen by mutableStateOf(false); private set
    var profil by mutableStateOf<AtemProfil?>(null); private set
    /** Fortschritt beim Vorbereiten der Stimme (fertig, gesamt); null = bereit oder läuft. */
    var vorbereitung by mutableStateOf<Pair<Int, Int>?>(null); private set
    var gestartet by mutableStateOf(false); private set
    var pausiert by mutableStateOf(false); private set
    var vorlesen by mutableStateOf(true); private set
    var geraeusch by mutableStateOf(true); private set
    /** Satz, der gerade gesprochen wird (als Untertitel). */
    var satz by mutableStateOf<String?>(null); private set
    var hinweis by mutableStateOf<String?>(null); private set

    var plan: List<Abschnitt> = emptyList(); private set
    var uebungStart = 0L; private set
    var uebungEnde = 0L; private set
    val gesamt: Long get() = plan.lastOrNull()?.ende ?: 0L

    private var startUhr = 0L
    private var pausenSumme = 0L
    private var pauseSeit = 0L

    private val spieler = HashMap<String, MediaPlayer>()
    private val dauern = HashMap<String, Long>()
    private val angehalten = ArrayList<MediaPlayer>()
    private var spur: AudioTrack? = null
    private var wach: PowerManager.WakeLock? = null
    private var fokus: AudioFocusRequest? = null

    /** Verstrichene Übungszeit in ms, steht in der Pause. */
    fun zeit(): Long = if (!gestartet) 0L else ((if (pausiert) pauseSeit else SystemClock.elapsedRealtime()) - startUhr - pausenSumme).coerceAtLeast(0L)

    fun abschnitt(t: Long): Abschnitt? {
        val p = plan
        if (p.isEmpty()) return null
        var lo = 0; var hi = p.size - 1
        while (lo < hi) { val m = (lo + hi + 1) / 2; if (p[m].start <= t) lo = m else hi = m - 1 }
        return p[lo].takeIf { t < it.ende }
    }

    fun starten(context: Context, p: AtemProfil) {
        beenden()
        val app = context.applicationContext
        profil = p; offen = true; gestartet = false; pausiert = false; satz = null; hinweis = null
        vorlesen = p.vorlesen; geraeusch = p.geraeusch
        runCatching { Vorleser.hole(app, SecureSettings(app)).stopp() }
        job = bereich.launch {
            vorbereitung = 0 to 1
            val clips = AtemStimme.vorbereiten(app, p) { a, b -> vorbereitung = a to b }
            if (clips.size < AtemStimme.texte(p).size && (p.vorlesen || p.saetzeAn)) hinweis = "Die Stimme ist gerade nicht vollständig erreichbar. Die Übung läuft ohne die fehlenden Ansagen."
            withContext(Dispatchers.Default) { AtemKlang.ton(true, p.ein * 100L); if (p.aus > 0) AtemKlang.ton(false, p.aus * 100L) }
            withContext(Dispatchers.IO) {
                clips.forEach { (text, clip) ->
                    runCatching {
                        MediaPlayer().apply {
                            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                            setDataSource(clip.datei.path); prepare()
                        }
                    }.onSuccess { spieler[text] = it; dauern[text] = clip.ms }.onFailure { Log.w(TAG, "Clip nicht ladbar", it) }
                }
            }
            plan = baue(p)
            uebungStart = plan.firstOrNull { it.phase == AtemPhase.EIN }?.start ?: 0L
            uebungEnde = plan.lastOrNull { it.zyklus > 0 }?.ende ?: 0L
            vorbereitung = null
            fokusHolen(app); wachHalten(app, gesamt)
            startUhr = SystemClock.elapsedRealtime(); pausenSumme = 0L
            gestartet = true
            ablauf(p)
        }
    }

    private fun baue(p: AtemProfil): List<Abschnitt> {
        val l = ArrayList<Abschnitt>()
        var t = 0L
        if (p.intro) { val d = (dauern[AtemStimme.INTRO] ?: 5_000L) + 2_400L; l += Abschnitt(AtemPhase.INTRO, t, d, 0); t += d }
        for (c in 1..p.zyklen) {
            listOf(AtemPhase.EIN to p.ein, AtemPhase.HALT_OBEN to p.halt1, AtemPhase.AUS to p.aus, AtemPhase.HALT_UNTEN to p.halt2).forEach { (ph, z) ->
                if (z > 0) { l += Abschnitt(ph, t, z * 100L, c); t += z * 100L }
            }
        }
        if (p.outro) { val d = (dauern[AtemStimme.OUTRO] ?: 4_000L) + 3_500L; l += Abschnitt(AtemPhase.OUTRO, t, d, 0); t += d }
        return l
    }

    private suspend fun warteBis(t: Long) {
        while (zeit() < t) delay((t - zeit()).coerceIn(4L, 120L))
    }

    private suspend fun ablauf(p: AtemProfil) {
        for (a in plan) {
            warteBis(a.start)
            when (a.phase) {
                AtemPhase.INTRO -> bereich.launch { warteBis(a.start + 700); if (vorlesen) spiele(AtemStimme.INTRO) }
                AtemPhase.OUTRO -> { spurStopp(); bereich.launch { warteBis(a.start + 900); if (vorlesen) spiele(AtemStimme.OUTRO) } }
                AtemPhase.EIN, AtemPhase.AUS -> {
                    val ein = a.phase == AtemPhase.EIN
                    if (geraeusch) geraeusch(ein, a.dauer)
                    val stichwort = if (ein) AtemStimme.EINATMEN else AtemStimme.AUSATMEN
                    val cue = stichwortDauer(stichwort, a.dauer)
                    if (cue > 0) spiele(stichwort)
                    if (p.saetzeAn) {
                        val liste = if (ein) p.saetzeEin else p.saetzeAus
                        if (liste.isNotEmpty()) {
                            val text = liste[(a.zyklus - 1) % liste.size]
                            val s = dauern[text]
                            // Gleich viel Ruhe vor und nach dem Satz: zwischen Ende des Stichworts und Ende der Phase mittig gesetzt.
                            val frei = if (s != null) a.dauer - cue - s else -1L
                            if (s != null && frei >= 300) bereich.launch {
                                warteBis(a.start + cue + frei / 2)
                                if (zeit() < a.ende) { satz = text; if (vorlesen) spiele(text) }
                                warteBis(a.start + cue + frei / 2 + s + 400)
                                if (satz == text) satz = null
                            }
                        }
                    }
                }
                AtemPhase.HALT_OBEN, AtemPhase.HALT_UNTEN -> {
                    if (stichwortDauer(AtemStimme.HALTEN, a.dauer) > 0) spiele(AtemStimme.HALTEN)
                }
                AtemPhase.ENDE -> Unit
            }
        }
        warteBis(gesamt)
        spurStopp(); fokusAbgeben(); wachLos()
    }

    /** Länge des Stichworts, wenn es gesprochen wird und in die Phase passt, sonst 0. */
    private fun stichwortDauer(text: String, phase: Long): Long {
        if (!vorlesen) return 0L
        val d = dauern[text] ?: return 0L
        return if (d + 150 <= phase) d else 0L
    }

    private fun spiele(text: String) {
        val mp = spieler[text] ?: return
        runCatching { if (mp.isPlaying) mp.pause(); mp.seekTo(0); mp.start() }
    }

    private fun geraeusch(ein: Boolean, ms: Long) {
        spurStopp()
        spur = AtemKlang.spur(AtemKlang.ton(ein, ms), 0.75f)?.also { runCatching { it.play() } }
    }

    private fun spurStopp() {
        spur?.let { runCatching { it.stop() }; runCatching { it.release() } }
        spur = null
    }

    fun pause() {
        if (!gestartet || pausiert) return
        pauseSeit = SystemClock.elapsedRealtime(); pausiert = true
        spur?.let { runCatching { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause() } }
        angehalten.clear()
        spieler.values.forEach { runCatching { if (it.isPlaying) { it.pause(); angehalten += it } } }
    }

    fun weiter() {
        if (!pausiert) return
        pausenSumme += SystemClock.elapsedRealtime() - pauseSeit; pausiert = false
        spur?.let { runCatching { if (it.playState == AudioTrack.PLAYSTATE_PAUSED) it.play() } }
        angehalten.forEach { runCatching { it.start() } }; angehalten.clear()
    }

    fun vorlesenUmschalten() {
        vorlesen = !vorlesen
        if (!vorlesen) { spieler.values.forEach { runCatching { if (it.isPlaying) { it.pause(); it.seekTo(0) } } }; angehalten.clear() }
    }

    fun geraeuschUmschalten() {
        geraeusch = !geraeusch
        if (!geraeusch) spurStopp()
    }

    fun beenden() {
        job?.cancel(); job = null
        spurStopp()
        spieler.values.forEach { runCatching { it.stop() }; runCatching { it.release() } }
        spieler.clear(); dauern.clear(); angehalten.clear()
        fokusAbgeben(); wachLos()
        offen = false; gestartet = false; pausiert = false; vorbereitung = null; satz = null; hinweis = null; plan = emptyList()
        // Neue Fassung erzeugter Geräusche beim nächsten Profil: Der Speicher hält nur die zuletzt genutzten.
    }

    private fun fokusHolen(context: Context) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        val r = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { }
            .build()
        runCatching { am.requestAudioFocus(r) }
        fokus = r
        fokusManager = am
    }

    private var fokusManager: AudioManager? = null
    private fun fokusAbgeben() {
        fokus?.let { f -> runCatching { fokusManager?.abandonAudioFocusRequest(f) } }
        fokus = null
    }

    private fun wachHalten(context: Context, ms: Long) {
        wachLos()
        wach = context.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:atemuebung").apply {
            setReferenceCounted(false); runCatching { acquire(ms + 10 * 60_000L) }
        }
    }

    private fun wachLos() {
        wach?.let { runCatching { if (it.isHeld) it.release() } }
        wach = null
    }
}
