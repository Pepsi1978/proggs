package de.frank.jarvis.atem

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.frank.jarvis.speech.Vorleser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

enum class AtemPhase(val anzeige: String) {
    INTRO("Ankommen"), EIN("Einatmen"), HALT_OBEN("Halten"), AUS("Ausatmen"), HALT_UNTEN("Halten"), OUTRO("Nachspüren"), ENDE("Fertig")
}

data class Abschnitt(val phase: AtemPhase, val start: Long, val dauer: Long, val zyklus: Int) {
    val ende: Long get() = start + dauer
}

/**
 * Eine laufende Atemübung. Eine einzige Uhr ([zeit], pausierbar) bestimmt Animation, Ansagen und Geräusche:
 * Alle Einsätze stehen als feste Zeitpunkte im Plan, so driftet auch eine 25-Minuten-Übung nicht.
 * Alle Teilabläufe sind Kinder eines Jobs: [beenden] stoppt sie vollständig, nichts spukt in die nächste Übung.
 * Läuft auch bei ausgeschaltetem Bildschirm weiter (Wachhaltung für die Restdauer, nach jeder Pause neu gestellt).
 */
object AtemSitzung {
    private const val TAG = "AtemSitzung"
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var app: Context? = null

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

    /** Vorab frisch erzeugter Satz für die nächste Ein- (true) bzw. Ausatemphase. */
    private class Vorrat(val text: String, val mp: MediaPlayer, val ms: Long, val datei: java.io.File)
    private val vorrat = HashMap<Boolean, Vorrat>()
    /** Frische Sätze, die gerade spielen: Pause und Vorlesen-Häkchen gelten auch für sie. */
    private val laufendeFrisch = HashSet<MediaPlayer>()
    private fun alleSpieler(): List<MediaPlayer> = spieler.values + laufendeFrisch

    private fun freigeben(v: Vorrat) {
        laufendeFrisch.remove(v.mp)
        runCatching { v.mp.stop() }; runCatching { v.mp.release() }; v.datei.delete()
    }

    /**
     * Erzeugt den Satz für Zyklus [zyklus] der Ein- bzw. Ausatemphase schon jetzt neu, eine ganze Runde vor seinem Einsatz:
     * Er klingt jedes Mal etwas anders und ist trotzdem pünktlich, auch bei schwachem Netz. Ist er nicht rechtzeitig fertig,
     * springt der gespeicherte Clip ein.
     */
    private fun kotlinx.coroutines.CoroutineScope.frischVorbereiten(ein: Boolean, zyklus: Int, p: AtemProfil) {
        val a = app ?: return
        val liste = if (ein) p.saetzeEin else p.saetzeAus
        if (!p.saetzeAn || !vorlesen || liste.isEmpty() || zyklus > p.zyklen) return
        val text = liste[(zyklus - 1) % liste.size]
        launch {
            val clip = AtemStimme.frisch(a, text) ?: return@launch
            var mp: MediaPlayer? = null
            try {
                mp = withContext(Dispatchers.IO) {
                    MediaPlayer().apply {
                        setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        setDataSource(clip.datei.path); prepare()
                    }
                }
                val neu = Vorrat(text, mp, mp.duration.toLong().takeIf { it > 0 } ?: clip.ms, clip.datei)
                vorrat.put(ein, neu)?.let { freigeben(it) }
                mp = null
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (f: Exception) {
                Log.w(TAG, "Frischer Satz nicht ladbar", f)
            } finally {
                mp?.let { runCatching { it.release() }; clip.datei.delete() }
            }
        }
    }
    /** Zwei Geräuschspuren, je Übung einmal gebaut und für jede Phase neu abgespielt. */
    private var spurEin: AudioTrack? = null
    private var spurAus: AudioTrack? = null
    private var spur: AudioTrack? = null
    private var wach: PowerManager.WakeLock? = null
    private var fokus: AudioFocusRequest? = null
    private var fokusManager: AudioManager? = null

    /** Verstrichene Übungszeit in ms, steht in der Pause. */
    fun zeit(): Long = if (!gestartet) 0L else ((if (pausiert) pauseSeit else SystemClock.elapsedRealtime()) - startUhr - pausenSumme).coerceAtLeast(0L)

    /** Index des Abschnitts zur Zeit [t] per Halbierungssuche, -1 außerhalb des Plans. */
    fun abschnittIndex(t: Long): Int {
        val p = plan
        if (p.isEmpty()) return -1
        var lo = 0; var hi = p.size - 1
        while (lo < hi) { val m = (lo + hi + 1) / 2; if (p[m].start <= t) lo = m else hi = m - 1 }
        return if (t < p[lo].ende) lo else -1
    }

    fun abschnitt(t: Long): Abschnitt? = abschnittIndex(t).takeIf { it >= 0 }?.let { plan[it] }

    fun starten(context: Context, p: AtemProfil) {
        beenden()
        val a = context.applicationContext
        app = a
        profil = p; offen = true; gestartet = false; pausiert = false; satz = null; hinweis = null
        vorlesen = p.vorlesen; geraeusch = p.geraeusch
        runCatching { Vorleser.aktuell()?.stopp() }
        job = bereich.launch {
            vorbereitung = 0 to 1
            // Die ersten Sätze entstehen schon parallel zur Vorbereitung, alle weiteren erst während der Übung.
            frischVorbereiten(true, 1, p); frischVorbereiten(false, 1, p)
            val clips = AtemStimme.vorbereiten(a, p) { x, y -> vorbereitung = x to y }
            if (AtemStimme.noetig(p).any { it !in clips }) hinweis = "Die Stimme ist gerade nicht vollständig erreichbar. Die Übung läuft ohne die fehlenden Ansagen."
            // Spieler und Spuren erst lokal bauen; nur eine noch laufende Übung übernimmt sie, sonst werden sie gleich freigegeben.
            val lokal = HashMap<String, Pair<MediaPlayer, Long>>()
            var ein: AudioTrack? = null; var aus: AudioTrack? = null
            try {
                withContext(Dispatchers.IO) {
                    clips.forEach { (text, clip) ->
                        ensureActive()
                        runCatching {
                            MediaPlayer().apply {
                                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                                setDataSource(clip.datei.path); prepare()
                            }
                        }.onSuccess { mp ->
                            // Die Länge des geladenen Spielers ist verlässlicher als die Metadaten der Datei.
                            lokal[text] = mp to (mp.duration.toLong().takeIf { it > 0 } ?: clip.ms)
                        }.onFailure { Log.w(TAG, "Clip nicht ladbar", it) }
                    }
                    // Nur ein Nebengeräusch: deutlich leiser als die Stimme.
                    ein = AtemKlang.spur(AtemKlang.ton(true, p.ein * 100L), AtemKlang.LAUTSTAERKE)
                    if (p.aus > 0) aus = AtemKlang.spur(AtemKlang.ton(false, p.aus * 100L), AtemKlang.LAUTSTAERKE)
                }
            } catch (abbruch: CancellationException) {
                lokal.values.forEach { runCatching { it.first.release() } }
                ein?.let { runCatching { it.release() } }; aus?.let { runCatching { it.release() } }
                throw abbruch
            }
            lokal.forEach { (text, paar) -> spieler[text] = paar.first; dauern[text] = paar.second }
            spurEin = ein; spurAus = aus
            // Ohne Begrüßung kommt der erste Satz gleich: kurz auf ihn warten, höchstens zehn Sekunden.
            if (!p.intro && p.vorlesen && p.saetzeAn && p.saetzeEin.isNotEmpty()) {
                val bis = SystemClock.elapsedRealtime() + 10_000L
                while (!vorrat.containsKey(true) && SystemClock.elapsedRealtime() < bis) delay(100)
            }
            // Der Abschluss liegt meist schon im Speicher; sonst entsteht er im Hintergrund, bis dahin gilt eine Schätzung.
            if (p.outro) {
                withContext(Dispatchers.IO) { AtemStimme.vorhanden(a, AtemStimme.OUTRO) }?.let { lade(AtemStimme.OUTRO, it) }
                if (AtemStimme.OUTRO !in spieler) launch { AtemStimme.hole(a, AtemStimme.OUTRO)?.let { lade(AtemStimme.OUTRO, it) } }
            }
            plan = baue(p)
            uebungStart = plan.firstOrNull { it.phase == AtemPhase.EIN }?.start ?: 0L
            uebungEnde = plan.lastOrNull { it.zyklus > 0 }?.ende ?: 0L
            vorbereitung = null
            fokusHolen(a); wachHalten(gesamt)
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
        if (p.outro) { val d = (dauern[AtemStimme.OUTRO] ?: app?.let { AtemStimme.schaetzeMs(it, AtemStimme.OUTRO) } ?: 8_000L) + 3_500L; l += Abschnitt(AtemPhase.OUTRO, t, d, 0); t += d }
        return l
    }

    private suspend fun warteBis(t: Long) {
        while (zeit() < t) { coroutineContext.ensureActive(); delay((t - zeit()).coerceIn(4L, 120L)) }
    }

    /** Lädt einen fertigen Clip als Spieler nach; nur eine noch laufende Übung übernimmt ihn. */
    private suspend fun lade(text: String, clip: AtemStimme.Clip) {
        val mp = runCatching {
            withContext(Dispatchers.IO) {
                MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    setDataSource(clip.datei.path); prepare()
                }
            }
        }.onFailure { if (it is CancellationException) throw it; Log.w(TAG, "Clip nicht ladbar", it) }.getOrNull() ?: return
        if (!coroutineContext.isActive || text in spieler) { runCatching { mp.release() }; return }
        spieler[text] = mp; dauern[text] = mp.duration.toLong().takeIf { it > 0 } ?: clip.ms
    }

    private suspend fun ablauf(p: AtemProfil) = coroutineScope {
        for (a in plan) {
            warteBis(a.start)
            when (a.phase) {
                AtemPhase.INTRO -> launch { warteBis(a.start + 700); if (vorlesen) spiele(AtemStimme.INTRO) }
                AtemPhase.OUTRO -> { spurStopp(); launch { warteBis(a.start + 900); if (vorlesen) spiele(AtemStimme.OUTRO) } }
                AtemPhase.EIN, AtemPhase.AUS -> {
                    val ein = a.phase == AtemPhase.EIN
                    if (geraeusch) geraeusch(ein)
                    val stichwort = if (ein) AtemStimme.EINATMEN else AtemStimme.AUSATMEN
                    val cue = stichwortDauer(stichwort, a.dauer)
                    if (cue > 0) spiele(stichwort)
                    if (p.saetzeAn) {
                        val liste = if (ein) p.saetzeEin else p.saetzeAus
                        if (liste.isNotEmpty()) {
                            val text = liste[(a.zyklus - 1) % liste.size]
                            // Frische Fassung nehmen, wenn sie fertig ist, und gleich die für die nächste Runde in Auftrag geben.
                            val frisch = vorrat.remove(ein)?.let { v -> if (v.text == text) v else { freigeben(v); null } }
                            frischVorbereiten(ein, a.zyklus + 1, p)
                            val s = frisch?.ms ?: dauern[text]
                            // Gleich viel Ruhe vor und nach dem Satz: zwischen Ende des Stichworts und Ende der Phase mittig gesetzt.
                            val frei = if (s != null) a.dauer - cue - s else -1L
                            if (s != null && frei >= 300) launch {
                                try {
                                    warteBis(a.start + cue + frei / 2)
                                    if (zeit() < a.ende) {
                                        satz = text
                                        if (vorlesen) {
                                            if (frisch != null) { laufendeFrisch += frisch.mp; runCatching { frisch.mp.start() } } else spiele(text)
                                        }
                                    }
                                    warteBis(a.start + cue + frei / 2 + s + 400)
                                    if (satz == text) satz = null
                                } finally {
                                    frisch?.let { freigeben(it) }
                                }
                            } else frisch?.let { freigeben(it) }
                        }
                    }
                }
                AtemPhase.HALT_OBEN, AtemPhase.HALT_UNTEN -> {
                    spurStopp()
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
        return if (d in 1..(phase - 150)) d else 0L
    }

    private fun spiele(text: String) {
        val mp = spieler[text] ?: return
        runCatching { if (mp.isPlaying) mp.pause(); mp.seekTo(0); mp.start() }
    }

    private fun geraeusch(ein: Boolean) {
        spurStopp()
        val s = (if (ein) spurEin else spurAus) ?: return
        runCatching { s.reloadStaticData(); s.play() }
        spur = s
    }

    private fun spurStopp() {
        spur?.let { runCatching { it.stop() } }
        spur = null
    }

    fun pause() {
        if (!gestartet || pausiert) return
        pauseSeit = SystemClock.elapsedRealtime(); pausiert = true
        spur?.let { runCatching { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause() } }
        angehalten.clear()
        alleSpieler().forEach { runCatching { if (it.isPlaying) { it.pause(); angehalten += it } } }
    }

    fun weiter() {
        if (!pausiert) return
        pausenSumme += SystemClock.elapsedRealtime() - pauseSeit; pausiert = false
        spur?.let { runCatching { if (it.playState == AudioTrack.PLAYSTATE_PAUSED) it.play() } }
        angehalten.forEach { runCatching { it.start() } }; angehalten.clear()
        // Die Wachhaltung zählt auch in der Pause: für die Restdauer neu stellen.
        if (gestartet) wachHalten((gesamt - zeit()).coerceAtLeast(0L))
    }

    fun vorlesenUmschalten() {
        vorlesen = !vorlesen
        if (!vorlesen) { alleSpieler().forEach { runCatching { if (it.isPlaying) { it.pause(); it.seekTo(0) } } }; angehalten.clear() }
    }

    fun geraeuschUmschalten() {
        geraeusch = !geraeusch
        if (!geraeusch) spurStopp()
    }

    fun beenden() {
        job?.cancel(); job = null
        spurStopp()
        listOfNotNull(spurEin, spurAus).forEach { runCatching { it.release() } }
        spurEin = null; spurAus = null
        spieler.values.forEach { runCatching { it.stop() }; runCatching { it.release() } }
        spieler.clear(); dauern.clear(); angehalten.clear()
        vorrat.values.toList().forEach { freigeben(it) }; vorrat.clear()
        laufendeFrisch.forEach { runCatching { it.stop() }; runCatching { it.release() } }; laufendeFrisch.clear()
        app?.let { AtemStimme.frischAufraeumen(it) }
        fokusAbgeben(); wachLos()
        offen = false; gestartet = false; pausiert = false; vorbereitung = null; satz = null; hinweis = null; plan = emptyList()
    }

    /** Kurzzeitiger Fokus: Musik setzt danach wieder ein; Anruf oder anderer Ton pausiert die Übung. */
    private fun fokusHolen(context: Context) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        val r = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener({ wechsel ->
                if (wechsel == AudioManager.AUDIOFOCUS_LOSS || wechsel == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause()
            }, Handler(Looper.getMainLooper()))
            .build()
        runCatching { am.requestAudioFocus(r) }
        fokus = r; fokusManager = am
    }

    private fun fokusAbgeben() {
        fokus?.let { f -> runCatching { fokusManager?.abandonAudioFocusRequest(f) } }
        fokus = null
    }

    private fun wachHalten(ms: Long) {
        val a = app ?: return
        wachLos()
        wach = a.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:atemuebung").apply {
            setReferenceCounted(false); runCatching { acquire(ms + 10 * 60_000L) }
        }
    }

    private fun wachLos() {
        wach?.let { runCatching { if (it.isHeld) it.release() } }
        wach = null
    }
}
