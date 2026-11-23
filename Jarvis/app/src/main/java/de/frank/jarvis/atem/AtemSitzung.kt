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
 * Läuft auch bei ausgeschaltetem Bildschirm weiter (Wachhaltung für die Restdauer; in der Pause freigegeben).
 *
 * Sätze: Jeder Satz wird eine Runde vor seinem Einsatz frisch erzeugt (klingt jedes Mal etwas anders). Kommt die frische
 * Fassung zu spät oder passt sie nicht, springt die gespeicherte ein (einmal je Satz im Hintergrund erzeugt); ohne beides
 * steht der Satz wenigstens als Untertitel da. Ein Satz, der gar nicht in die Phase passt, wird nicht erst erzeugt.
 */
object AtemSitzung {
    private const val TAG = "AtemSitzung"
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val haupt = Handler(Looper.getMainLooper())
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
    /** Lautstärke des Atemgeräuschs als Reglerstellung (0–1), live per Regler; bleibt für die nächsten Übungen gespeichert. */
    var pegel by mutableStateOf(0.5f); private set

    var plan: List<Abschnitt> = emptyList(); private set
    var uebungStart = 0L; private set
    var uebungEnde = 0L; private set
    val gesamt: Long get() = plan.lastOrNull()?.ende ?: 0L

    private var startUhr = 0L
    private var pausenSumme = 0L
    private var pauseSeit = 0L
    /** Pause kam vom Audiofokus (Anruf, Jarvis spricht): Bei Rückkehr des Fokus geht es von selbst weiter. */
    private var pauseDurchFokus = false
    private var fokusVerloren = false
    /** Zählt Untertitel: Ein später Aufräumschritt löscht nur seinen eigenen, nie den nächsten gleichen Satz. */
    private var satzNummer = 0

    private val spieler = HashMap<String, MediaPlayer>()
    private val dauern = HashMap<String, Long>()
    private val angehalten = ArrayList<MediaPlayer>()

    /** Vorab frisch erzeugter Satz für eine Phase (Ein, Halten, Aus, Halten) eines bestimmten Zyklus. */
    private class Vorrat(val zyklus: Int, val text: String, val mp: MediaPlayer, val ms: Long, val datei: java.io.File)
    private val vorrat = HashMap<AtemPhase, Vorrat>()
    private val SATZ_PHASEN = listOf(AtemPhase.EIN, AtemPhase.HALT_OBEN, AtemPhase.AUS, AtemPhase.HALT_UNTEN)
    private fun stichwortFuer(phase: AtemPhase) = when (phase) { AtemPhase.EIN -> AtemStimme.EINATMEN; AtemPhase.AUS -> AtemStimme.AUSATMEN; else -> AtemStimme.HALTEN }
    /** Frische Sätze, die gerade spielen: Pause und Vorlesen-Häkchen gelten auch für sie. */
    private val laufendeFrisch = HashSet<MediaPlayer>()
    private fun alleSpieler(): List<MediaPlayer> = spieler.values + laufendeFrisch

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

    private fun spielerFuer(datei: java.io.File): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        setDataSource(datei.path); prepare()
    }

    private fun freigeben(v: Vorrat) {
        laufendeFrisch.remove(v.mp)
        runCatching { v.mp.stop() }; runCatching { v.mp.release() }; v.datei.delete()
    }

    private fun satzText(p: AtemProfil, phase: AtemPhase, zyklus: Int): String? {
        val liste = p.saetze(phase)
        return if (!p.saetzeAn || liste.isEmpty()) null else liste[(zyklus - 1) % liste.size]
    }

    /** Länge des Stichworts, wenn es gesprochen wird und in die Phase passt, sonst 0. */
    private fun stichwortDauer(text: String, phase: Long): Long {
        if (!vorlesen) return 0L
        val d = dauern[text] ?: return 0L
        return if (AtemStimme.stichwortPasst(d, phase)) d else 0L
    }

    /**
     * Erzeugt den Satz für Zyklus [zyklus] der Ein- bzw. Ausatemphase schon jetzt neu, eine Runde vor seinem Einsatz.
     * Nur, wenn vorgelesen wird und der Satz (nach bekannter oder geschätzter Länge) überhaupt in die Phase passt.
     */
    private fun CoroutineScope.frischVorbereiten(art: AtemPhase, zyklus: Int, p: AtemProfil) {
        val a = app ?: return
        if (!vorlesen || zyklus > p.zyklen) return
        val text = satzText(p, art, zyklus) ?: return
        val phase = p.zehntel(art) * 100L
        val cue = stichwortDauer(stichwortFuer(art), phase)
        if (AtemStimme.satzRuhe(phase, cue, dauern[text] ?: AtemStimme.schaetzeMs(a, text)) == null) return
        launch {
            val clip = AtemStimme.frisch(a, text) ?: return@launch
            var uebernommen = false
            try {
                val mp = withContext(Dispatchers.IO) { spielerFuer(clip.datei) }
                ensureActive()
                val neu = Vorrat(zyklus, text, mp, mp.duration.toLong().takeIf { it > 0 } ?: clip.ms, clip.datei)
                val alt = vorrat[art]
                // Kommt eine ältere Runde verspätet nach einer neueren an, bleibt die neuere.
                if (alt != null && alt.zyklus > zyklus) { runCatching { mp.release() } } else { vorrat[art] = neu; alt?.let { freigeben(it) }; uebernommen = true }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (f: Exception) {
                Log.w(TAG, "Frischer Satz nicht ladbar", f)
            } finally {
                if (!uebernommen) clip.datei.delete()
            }
        }
    }

    fun starten(context: Context, p: AtemProfil) {
        beende(leeren = false)
        val a = context.applicationContext
        app = a
        profil = p; offen = true; gestartet = false; pausiert = false; satz = null; hinweis = null
        pauseDurchFokus = false; fokusVerloren = false
        vorlesen = p.vorlesen; geraeusch = p.geraeusch
        pegel = de.frank.jarvis.data.Einstellungen.get(a).atemGeraeuschLautstaerke / 100f
        runCatching { Vorleser.aktuell()?.stopp() }
        runCatching { de.frank.jarvis.mentalitaet.MentalitaetSpieler.stopp() }
        job = bereich.launch {
            withContext(Dispatchers.IO) { AtemStimme.frischAufraeumen(a); AtemStimme.aufraeumen(a) }
            vorbereitung = 0 to 1
            // Die ersten Sätze entstehen schon parallel zur Vorbereitung, alle weiteren erst während der Übung.
            SATZ_PHASEN.forEach { frischVorbereiten(it, 1, p) }
            val clips = AtemStimme.vorbereiten(a, p) { x, y -> vorbereitung = x to (y + 1) }
            // Spieler und Spuren erst lokal bauen; nur eine noch laufende Übung übernimmt sie, sonst werden sie gleich freigegeben.
            val lokal = HashMap<String, Pair<MediaPlayer, Long>>()
            var ein: AudioTrack? = null; var aus: AudioTrack? = null
            try {
                withContext(Dispatchers.IO) {
                    clips.forEach { (text, clip) ->
                        ensureActive()
                        runCatching { spielerFuer(clip.datei) }
                            .onSuccess { mp -> lokal[text] = mp to (mp.duration.toLong().takeIf { it > 0 } ?: clip.ms) }
                            // Beschädigter Clip: löschen, damit er beim nächsten Start neu entsteht.
                            .onFailure { Log.w(TAG, "Clip nicht ladbar", it); AtemStimme.verwerfen(clip) }
                    }
                    ein = AtemKlang.spur(AtemKlang.ton(true, p.ein * 100L), lautstaerke(pegel))
                    if (p.aus > 0) aus = AtemKlang.spur(AtemKlang.ton(false, p.aus * 100L), lautstaerke(pegel))
                }
            } catch (abbruch: CancellationException) {
                lokal.values.forEach { runCatching { it.first.release() } }
                ein?.let { runCatching { it.release() } }; aus?.let { runCatching { it.release() } }
                throw abbruch
            }
            lokal.forEach { (text, paar) -> spieler[text] = paar.first; dauern[text] = paar.second }
            spurEin = ein; spurAus = aus
            val hinweise = buildList {
                if (AtemStimme.noetig(p).any { it !in lokal }) add("Die Stimme ist gerade nicht vollständig erreichbar. Die Übung läuft ohne die fehlenden Ansagen.")
                if (p.geraeusch && ein == null) add("Das Atemgeräusch ist auf diesem Gerät gerade nicht verfügbar.")
            }
            hinweis = hinweise.joinToString(" ").ifBlank { null }
            // Gespeicherte Fassungen der Sätze als Rückfall laden (ohne Netz); fehlende entstehen einmal im Hintergrund.
            if (p.saetzeAn) p.alleSaetze.forEach { text ->
                val vorhanden = withContext(Dispatchers.IO) { AtemStimme.vorhanden(a, text) }
                if (vorhanden != null) lade(text, vorhanden)
                else if (p.vorlesen) launch { AtemStimme.hole(a, text)?.let { lade(text, it) } }
            }
            // Ohne Begrüßung kommt der erste Satz gleich: kurz auf ihn warten, höchstens zehn Sekunden.
            if (!p.intro && p.vorlesen && p.saetzeEin.isNotEmpty() && p.saetzeEin.first() !in spieler && p.saetzeAn) {
                vorbereitung = (vorbereitung?.second ?: 1).let { (it - 1) to it }
                val bis = SystemClock.elapsedRealtime() + 10_000L
                while (!vorrat.containsKey(AtemPhase.EIN) && SystemClock.elapsedRealtime() < bis) delay(100)
            }
            // Der Abschluss liegt meist schon im Speicher; sonst entsteht er im Hintergrund, bis dahin gilt eine Schätzung.
            if (p.outro && p.vorlesen) {
                withContext(Dispatchers.IO) { AtemStimme.vorhanden(a, AtemStimme.OUTRO) }?.let { lade(AtemStimme.OUTRO, it) }
                if (AtemStimme.OUTRO !in spieler) launch { AtemStimme.hole(a, AtemStimme.OUTRO)?.let { lade(AtemStimme.OUTRO, it) } }
            }
            plan = baue(p)
            uebungStart = plan.firstOrNull { it.phase == AtemPhase.EIN }?.start ?: 0L
            uebungEnde = plan.lastOrNull { it.zyklus > 0 }?.ende ?: 0L
            vorbereitung = null
            val fokusDa = fokusHolen(a); wachHalten(gesamt)
            startUhr = SystemClock.elapsedRealtime(); pausenSumme = 0L
            gestartet = true
            // Kein Fokus (z. B. während eines Anrufs): nicht über das Gespräch sprechen, sondern pausiert warten.
            if (!fokusDa) { pause(); pauseDurchFokus = true; hinweis = listOfNotNull(hinweis, "Gerade läuft ein anderer Ton. Die Übung wartet, bis er endet, oder tippe auf Weiter.").joinToString(" ") }
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
        if (p.outro) {
            val sprechen = if (p.vorlesen) dauern[AtemStimme.OUTRO] ?: app?.let { AtemStimme.schaetzeMs(it, AtemStimme.OUTRO) } ?: 8_000L else 2_500L
            val d = sprechen + 3_500L; l += Abschnitt(AtemPhase.OUTRO, t, d, 0); t += d
        }
        return l
    }

    private suspend fun warteBis(t: Long) {
        while (zeit() < t) {
            coroutineContext.ensureActive()
            // In der Pause steht die Uhr: selten nachsehen, die CPU darf schlafen.
            delay(if (pausiert) 500L else (t - zeit()).coerceIn(4L, 120L))
        }
    }

    /** Lädt einen fertigen Clip als Spieler nach; nur eine noch laufende Übung übernimmt ihn. */
    private suspend fun lade(text: String, clip: AtemStimme.Clip) {
        val mp = runCatching { withContext(Dispatchers.IO) { spielerFuer(clip.datei) } }
            .onFailure { if (it is CancellationException) throw it; Log.w(TAG, "Clip nicht ladbar", it); AtemStimme.verwerfen(clip) }.getOrNull() ?: return
        if (!coroutineContext.isActive || text in spieler) { runCatching { mp.release() }; return }
        spieler[text] = mp; dauern[text] = mp.duration.toLong().takeIf { it > 0 } ?: clip.ms
    }

    private suspend fun ablauf(p: AtemProfil) = coroutineScope {
        for (a in plan) {
            warteBis(a.start)
            when (a.phase) {
                AtemPhase.INTRO -> launch { warteBis(a.start + 700); if (vorlesen) spiele(AtemStimme.INTRO) }
                AtemPhase.OUTRO -> {
                    spurStopp()
                    launch {
                        warteBis(a.start + 900)
                        // Entsteht der Abschluss noch, ein paar Sekunden auf ihn warten.
                        var warten = 0
                        while (vorlesen && AtemStimme.OUTRO !in spieler && warten < 40) { delay(100); warten++ }
                        if (vorlesen) spiele(AtemStimme.OUTRO)
                    }
                }
                AtemPhase.EIN, AtemPhase.AUS -> {
                    val ein = a.phase == AtemPhase.EIN
                    if (geraeusch) geraeusch(ein)
                    val cue = stichwortDauer(if (ein) AtemStimme.EINATMEN else AtemStimme.AUSATMEN, a.dauer)
                    if (cue > 0) spiele(if (ein) AtemStimme.EINATMEN else AtemStimme.AUSATMEN)
                    satzEinplanen(p, a, cue)
                }
                AtemPhase.HALT_OBEN, AtemPhase.HALT_UNTEN -> {
                    spurStopp()
                    val cue = stichwortDauer(AtemStimme.HALTEN, a.dauer)
                    if (cue > 0) spiele(AtemStimme.HALTEN)
                    satzEinplanen(p, a, cue)
                }
                AtemPhase.ENDE -> Unit
            }
        }
        warteBis(gesamt)
        // Spricht der Abschluss noch, ihn ausreden lassen, bevor der Fokus zurückgeht.
        var warten = 0
        while (alleSpieler().any { runCatching { it.isPlaying }.getOrDefault(false) } && warten < 150) { delay(100); warten++ }
        ressourcenFrei()
        // Liegt der Endbildschirm lange unbeachtet, schließt er sich selbst.
        delay(10 * 60_000L)
        beende(leeren = true)
    }

    /** Wählt für den Satz dieser Phase die frische Fassung, sonst die gespeicherte, sonst nur den Untertitel, und setzt ihn mittig. */
    private fun CoroutineScope.satzEinplanen(p: AtemProfil, a: Abschnitt, cue: Long) {
        val text = satzText(p, a.phase, a.zyklus) ?: return
        val frisch = vorrat.remove(a.phase)?.let { v -> if (v.zyklus == a.zyklus && v.text == text) v else { freigeben(v); null } }
        frischVorbereiten(a.phase, a.zyklus + 1, p)
        val gespeichert = dauern[text]
        val schaetzung = app?.let { AtemStimme.schaetzeMs(it, text) } ?: 3_000L
        val (nimmFrisch, s) = when {
            frisch != null && AtemStimme.satzRuhe(a.dauer, cue, frisch.ms) != null -> true to frisch.ms
            gespeichert != null && AtemStimme.satzRuhe(a.dauer, cue, gespeichert) != null -> false to gespeichert
            else -> false to schaetzung
        }
        if (!nimmFrisch) frisch?.let { freigeben(it) }
        val ruhe = AtemStimme.satzRuhe(a.dauer, cue, s) ?: return
        val hoerbar = nimmFrisch || gespeichert == s
        launch {
            try {
                warteBis(a.start + cue + ruhe)
                if (zeit() >= a.ende) return@launch
                val nummer = ++satzNummer
                satz = text
                if (vorlesen && hoerbar) {
                    if (nimmFrisch && frisch != null) { laufendeFrisch += frisch.mp; runCatching { frisch.mp.start() } } else spiele(text)
                }
                warteBis(a.start + cue + ruhe + s + 400)
                if (satzNummer == nummer) satz = null
            } finally {
                if (nimmFrisch) frisch?.let { freigeben(it) }
            }
        }
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
        pauseSeit = SystemClock.elapsedRealtime(); pausiert = true; pauseDurchFokus = false
        spur?.let { runCatching { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause() } }
        angehalten.clear()
        alleSpieler().forEach { runCatching { if (it.isPlaying) { it.pause(); angehalten += it } } }
        // In der Pause braucht es keine Wachhaltung; Weiter stellt sie für die Restdauer neu.
        wachLos()
    }

    fun weiter() {
        if (!pausiert) return
        // Nach einem Fokusverlust erst wieder um den Ton bitten, sonst spielte die Übung über die andere App.
        if (fokusVerloren || fokus == null) app?.let { fokusHolen(it) }
        pausenSumme += SystemClock.elapsedRealtime() - pauseSeit; pausiert = false; pauseDurchFokus = false
        spur?.let { runCatching { if (it.playState == AudioTrack.PLAYSTATE_PAUSED) it.play() } }
        angehalten.forEach { runCatching { it.start() } }; angehalten.clear()
        if (gestartet) wachHalten((gesamt - zeit()).coerceAtLeast(0L))
    }

    fun vorlesenUmschalten() {
        vorlesen = !vorlesen
        if (!vorlesen) { alleSpieler().forEach { runCatching { if (it.isPlaying) { it.pause(); it.seekTo(0) } } }; angehalten.clear() }
    }

    /**
     * Reglerstellung → Lautstärke, gehörrichtig gekrümmt: Mitte = 18 % der Stimme (das leise Nebengeräusch),
     * ganz links still, ganz rechts so laut wie die Stimme. 0,5^2,47 ≈ 0,18.
     */
    private fun lautstaerke(regler: Float): Float = Math.pow(regler.coerceIn(0f, 1f).toDouble(), 2.47).toFloat()

    /** Auch von anderen Fäden (Spracheinstellung) aufrufbar: Zustand und Spuren werden nur auf dem Hauptfaden angefasst. */
    fun pegelSetzen(wert: Float, speichern: Boolean = true) {
        if (Looper.myLooper() != Looper.getMainLooper()) { haupt.post { pegelSetzen(wert, speichern) }; return }
        pegel = wert.coerceIn(0f, 1f)
        listOfNotNull(spurEin, spurAus).forEach { runCatching { it.setVolume(lautstaerke(pegel)) } }
        if (speichern) app?.let { de.frank.jarvis.data.Einstellungen.get(it).atemGeraeuschLautstaerke = Math.round(pegel * 100) }
    }

    fun geraeuschUmschalten() {
        geraeusch = !geraeusch
        if (!geraeusch) spurStopp()
    }

    /** Gibt Spieler, Spuren, Fokus und Wachhaltung frei; der Endbildschirm bleibt stehen. */
    private fun ressourcenFrei() {
        spurStopp()
        listOfNotNull(spurEin, spurAus).forEach { runCatching { it.release() } }
        spurEin = null; spurAus = null
        spieler.values.forEach { runCatching { it.stop() }; runCatching { it.release() } }
        spieler.clear(); angehalten.clear()
        vorrat.values.toList().forEach { freigeben(it) }; vorrat.clear()
        laufendeFrisch.toList().forEach { runCatching { it.stop() }; runCatching { it.release() } }; laufendeFrisch.clear()
        app?.let { a -> bereich.launch(Dispatchers.IO) { AtemStimme.frischAufraeumen(a) } }
        fokusAbgeben(); wachLos()
    }

    fun beenden() = beende(leeren = true)

    private fun beende(leeren: Boolean) {
        job?.cancel(); job = null
        ressourcenFrei()
        dauern.clear()
        if (leeren) AtemKlang.leeren()
        offen = false; gestartet = false; pausiert = false; vorbereitung = null; satz = null; hinweis = null; plan = emptyList()
    }

    /** Kurzzeitiger Fokus: Musik setzt danach wieder ein; Anruf oder anderer Ton pausiert, seine Rückkehr setzt fort. */
    private fun fokusHolen(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return true
        fokusAbgeben()
        val r = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener({ wechsel ->
                when (wechsel) {
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        fokusVerloren = wechsel == AudioManager.AUDIOFOCUS_LOSS
                        if (gestartet && !pausiert) { pause(); pauseDurchFokus = true }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        fokusVerloren = false
                        if (pauseDurchFokus) weiter()
                    }
                }
            }, haupt)
            .build()
        val ergebnis = runCatching { am.requestAudioFocus(r) }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        fokus = r; fokusManager = am
        fokusVerloren = ergebnis != AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return !fokusVerloren
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
