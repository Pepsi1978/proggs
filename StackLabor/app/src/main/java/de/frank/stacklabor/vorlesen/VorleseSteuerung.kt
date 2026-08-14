package de.frank.stacklabor.vorlesen

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.nio.ByteOrder
import java.util.UUID
import java.util.WeakHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/*
 * PLAN (F-16 "Auswertung vorlesen")
 *  1. Der Fliesstext wird an Leerzeilen in Absaetze zerlegt; zu lange Absaetze werden intern an
 *     Satzgrenzen in Stuecke geteilt, behalten aber ihre Absatz-Nummer (fuer die Hervorhebung M-15).
 *  2. Je Stueck holt der gewaehlte Sprecher die Audiodaten; das naechste Stueck wird waehrend der
 *     laufenden Wiedergabe schon vorab geholt, damit zwischen den Absaetzen keine Luecke entsteht.
 *  3. Aus den Audiodaten wird vorab eine Huellkurve berechnet — daraus speist sich der Pegel fuer
 *     die drei Balken am Knopf, ohne Mikrofon-Berechtigung.
 *  4. Abgespielt wird mit MediaPlayer, verstaerkt wie in der Vorlage (SpeechLoudness).
 *  5. Ein Vordergrunddienst haelt die Wiedergabe am Leben, wenn Frank den Bildschirm verlaesst.
 */

/** Einstellbare Werte des Vorlesens (Rubrik "Vorlesen" in B-10, F-18). */
data class VorleseEinstellungen(
    val anbieter: Anbieter = StimmenKatalog.VORGABE_ANBIETER,
    val stimmeId: String = StimmenKatalog.VORGABE_EDGE_STIMME,
    /** 1.0 ist normales Tempo; die Anbieter nehmen 0.7 bis 1.3 an. */
    val tempo: Float = 1f,
    /** Pause zwischen zwei Absaetzen in Millisekunden. */
    val absatzPauseMs: Long = 400L,
)

/** Zustand des Vorlesens fuer die Oberflaeche. */
sealed interface VorleseZustand {
    data object BEREIT : VorleseZustand
    data object SPRICHT : VorleseZustand
    data object PAUSIERT : VorleseZustand

    /** [meldung] ist Klartext und kann unveraendert angezeigt werden. */
    data class FEHLER(val meldung: String) : VorleseZustand
}

/**
 * Steuert das Vorlesen: zerlegt den Fliesstext, laesst ihn absatzweise sprechen und meldet
 * Zustand, laufenden Absatz und Pegel an die Oberflaeche.
 *
 * Alle Flows werden auf dem Hauptthread fortgeschrieben, Netz- und Rechenarbeit laeuft auf
 * [Dispatchers.IO].
 */
class VorleseSteuerung(
    context: Context,
    private val einstellungen: StateFlow<VorleseEinstellungen> =
        MutableStateFlow(VorleseEinstellungen()),
    private val zaehler: VerbrauchsZaehler = VerbrauchsZaehler(context),
    private val schluessel: SchluesselSpeicher = SchluesselSpeicher(context),
    private val bereich: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {

    private val anwendung: Context = context.applicationContext

    private val _zustand = MutableStateFlow<VorleseZustand>(VorleseZustand.BEREIT)

    /** BEREIT | SPRICHT | PAUSIERT | FEHLER(meldung). */
    val zustand: StateFlow<VorleseZustand> = _zustand.asStateFlow()

    private val _aktuellerAbsatz = MutableStateFlow(-1)

    /** Nummer des gerade gesprochenen Absatzes, `-1` wenn nichts laeuft (Hervorhebung M-15). */
    val aktuellerAbsatz: StateFlow<Int> = _aktuellerAbsatz.asStateFlow()

    private val _pegel = MutableStateFlow(0f)

    /** Lautstaerke 0..1 fuer die drei Pegelbalken am Knopf. */
    val pegel: StateFlow<Float> = _pegel.asStateFlow()

    private val _absaetze = MutableStateFlow<List<String>>(emptyList())

    /** Die zerlegten Absaetze in gesprochener Reihenfolge — Grundlage der Hervorhebung. */
    val absaetze: StateFlow<List<String>> = _absaetze.asStateFlow()

    /** Solange `false`, wartet die Absatzfolge (Pause). */
    private val fortsetzen = MutableStateFlow(true)

    private var auftrag: Job? = null

    @Volatile
    private var spieler: MediaPlayer? = null

    private val edgeSprecher: Sprecher by lazy { EdgeStimme() }
    private val googleSprecher: Sprecher by lazy {
        GoogleStimme(schluessel = { schluessel.googleSchluessel() })
    }
    private val qwenSprecher: Sprecher by lazy {
        QwenStimme(
            schluessel = { schluessel.qwenSchluessel() },
            stimmKennung = { schluessel.qwenStimmKennung() },
        )
    }

    /**
     * Liest [text] absatzweise vor. Ein laufendes Vorlesen wird vorher beendet.
     * Vorgelesen wird genau das, was hereingereicht wird — die Aufbereitung (nur Fliesstext,
     * keine Tabelle, keine Zahlen) passiert vor dem Aufruf.
     */
    fun lies(text: String) {
        stopp()
        val stuecke = zerlege(text)
        if (stuecke.isEmpty()) {
            _zustand.value = VorleseZustand.FEHLER("Es gibt keinen Fliesstext zum Vorlesen.")
            return
        }
        if (!netzVorhanden()) {
            _zustand.value = VorleseZustand.FEHLER(
                "Ohne Internet kann nicht vorgelesen werden. Alle drei Stimmen kommen aus dem Netz.",
            )
            return
        }

        // distinctBy statt distinct: Zwei gleichlautende Absaetze bleiben zwei Absaetze, sonst
        // zeigte die Hervorhebung auf die falsche Stelle.
        _absaetze.value = stuecke.distinctBy { it.absatzNummer }.map { it.absatzText }
        fortsetzen.value = true
        _zustand.value = VorleseZustand.SPRICHT
        laufende = this
        VorleseDienst.starte(anwendung)

        auftrag = bereich.launch {
            try {
                spreche(stuecke)
                _zustand.value = VorleseZustand.BEREIT
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: VorleseFehler) {
                _zustand.value = VorleseZustand.FEHLER(fehler.message.orEmpty())
            } catch (fehler: Exception) {
                Log.w(MARKE, "Vorlesen gescheitert", fehler)
                _zustand.value = VorleseZustand.FEHLER(
                    "Das Vorlesen ist gescheitert: ${fehler.message ?: fehler.javaClass.simpleName}",
                )
            } finally {
                _aktuellerAbsatz.value = -1
                _pegel.value = 0f
                VorleseDienst.stoppe(anwendung)
                if (laufende === this@VorleseSteuerung) laufende = null
            }
        }
    }

    /** Haelt die Wiedergabe an. Ohne laufendes Vorlesen passiert nichts. */
    fun pause() {
        if (_zustand.value != VorleseZustand.SPRICHT) return
        fortsetzen.value = false
        try {
            spieler?.takeIf { it.isPlaying }?.pause()
        } catch (fehler: IllegalStateException) {
            Log.w(MARKE, "Pause nicht moeglich: ${fehler.message}")
        }
        _pegel.value = 0f
        _zustand.value = VorleseZustand.PAUSIERT
    }

    /** Setzt eine Pause fort. */
    fun weiter() {
        if (_zustand.value != VorleseZustand.PAUSIERT) return
        _zustand.value = VorleseZustand.SPRICHT
        try {
            spieler?.start()
        } catch (fehler: IllegalStateException) {
            Log.w(MARKE, "Fortsetzen nicht moeglich: ${fehler.message}")
        }
        fortsetzen.value = true
    }

    /** Beendet das Vorlesen vollstaendig und raeumt auf. */
    fun stopp() {
        auftrag?.cancel()
        auftrag = null
        fortsetzen.value = true
        gibSpielerFrei()
        _aktuellerAbsatz.value = -1
        _pegel.value = 0f
        if (_zustand.value !is VorleseZustand.FEHLER) _zustand.value = VorleseZustand.BEREIT
        VorleseDienst.stoppe(anwendung)
        if (laufende === this) laufende = null
    }

    /** Gibt Netz- und Abspiel-Ressourcen frei (beim Beenden der App oder des Bildschirms). */
    fun freigeben() {
        stopp()
        bereich.cancel()
        edgeSprecher.schliesse()
        googleSprecher.schliesse()
        qwenSprecher.schliesse()
    }

    private suspend fun spreche(stuecke: List<Sprechstueck>) = supervisorScope {
        val gewaehlt = einstellungen.value
        val sprecher = sprecherFuer(gewaehlt.anbieter)
        val stimme = StimmenKatalog.gueltigeStimme(gewaehlt.anbieter, gewaehlt.stimmeId)
        var vorab: Deferred<ByteArray>? = null

        stuecke.forEachIndexed { nummer, stueck ->
            // Wartet, solange pausiert ist.
            fortsetzen.first { es -> es }

            val holen = vorab ?: async(Dispatchers.IO) {
                sprecher.sprich(stueck.text, stimme, gewaehlt.tempo)
            }
            vorab = null
            val daten = holen.await()
            zaehler.buche(gewaehlt.anbieter, stueck.text.length)
            _aktuellerAbsatz.value = stueck.absatzNummer

            // DECISION: Das naechste Stueck wird waehrend der laufenden Wiedergabe geholt, sonst
            // entsteht zwischen zwei Absaetzen eine hoerbare Wartezeit.
            val naechstes = stuecke.getOrNull(nummer + 1)
            if (naechstes != null) {
                vorab = async(Dispatchers.IO) {
                    sprecher.sprich(naechstes.text, stimme, gewaehlt.tempo)
                }
            }

            if (daten.isEmpty()) return@forEachIndexed
            val datei = withContext(Dispatchers.IO) { schreibeZwischendatei(daten) }
            val huelle = withContext(Dispatchers.IO) { Huellkurve.berechne(datei) }
            spieleAb(datei, huelle)

            val absatzWechsel = naechstes != null && naechstes.absatzNummer != stueck.absatzNummer
            if (absatzWechsel && gewaehlt.absatzPauseMs > 0) delay(gewaehlt.absatzPauseMs)
        }
    }

    private fun sprecherFuer(anbieter: Anbieter): Sprecher = when (anbieter) {
        Anbieter.EDGE -> edgeSprecher
        Anbieter.GOOGLE_CLOUD -> googleSprecher
        Anbieter.QWEN_KLON -> qwenSprecher
    }

    private suspend fun spieleAb(datei: File, huelle: FloatArray?) {
        val abspieler = MediaPlayer()
        try {
            abspieler.setAudioAttributes(Lautheit.eigenschaften)
            abspieler.setDataSource(datei.absolutePath)
            abspieler.prepare()
            Lautheit.verstaerke(abspieler)
            spieler = abspieler
            abspieler.start()
            supervisorScope {
                val takt = launch { haltePegelNach(abspieler, huelle) }
                try {
                    warteAufEnde(abspieler)
                } finally {
                    takt.cancel()
                    _pegel.value = 0f
                }
            }
        } catch (fehler: IllegalStateException) {
            throw VorleseFehler("Die Aufnahme konnte nicht abgespielt werden.", fehler)
        } catch (fehler: java.io.IOException) {
            throw VorleseFehler("Die Aufnahme konnte nicht gelesen werden.", fehler)
        } finally {
            if (spieler === abspieler) spieler = null
            Lautheit.gibFrei(abspieler)
            try {
                abspieler.reset()
            } catch (fehler: IllegalStateException) {
                Log.w(MARKE, "Abspieler liess sich nicht zuruecksetzen: ${fehler.message}")
            }
            abspieler.release()
            datei.delete()
        }
    }

    private suspend fun warteAufEnde(abspieler: MediaPlayer) =
        suspendCancellableCoroutine { fortsetzung ->
            abspieler.setOnCompletionListener {
                if (fortsetzung.isActive) fortsetzung.resume(Unit)
            }
            abspieler.setOnErrorListener { _, was, extra ->
                if (fortsetzung.isActive) {
                    fortsetzung.resumeWith(
                        Result.failure(VorleseFehler("Die Wiedergabe brach ab ($was/$extra).")),
                    )
                }
                true
            }
            fortsetzung.invokeOnCancellation {
                try {
                    abspieler.stop()
                } catch (fehler: IllegalStateException) {
                    Log.w(MARKE, "Abspieler liess sich nicht stoppen: ${fehler.message}")
                }
            }
        }

    /**
     * Schreibt den Pegel fort: Wert aus der Huellkurve an der aktuellen Abspielstelle, geglaettet,
     * damit die drei Balken nicht zappeln. Ohne Huellkurve zeigt der Balken einen ruhigen
     * Ersatzwert — sichtbar bleibt, dass gesprochen wird.
     */
    private suspend fun haltePegelNach(abspieler: MediaPlayer, huelle: FloatArray?) {
        var geglaettet = 0f
        while (currentCoroutineContext().isActive) {
            val roh = try {
                when {
                    !abspieler.isPlaying -> 0f
                    huelle == null -> ERSATZ_PEGEL
                    else -> huelle.getOrElse(abspieler.currentPosition / Huellkurve.FENSTER_MS) { 0f }
                }
            } catch (fehler: IllegalStateException) {
                0f
            }
            geglaettet = if (roh > geglaettet) roh else geglaettet * 0.55f + roh * 0.45f
            _pegel.value = geglaettet.coerceIn(0f, 1f)
            delay(PEGEL_TAKT_MS)
        }
    }

    private fun schreibeZwischendatei(daten: ByteArray): File {
        val datei = File(anwendung.cacheDir, "vorlesen_${UUID.randomUUID()}.audio")
        datei.writeBytes(daten)
        return datei
    }

    private fun gibSpielerFrei() {
        val abspieler = spieler ?: return
        spieler = null
        try {
            abspieler.stop()
        } catch (fehler: IllegalStateException) {
            Log.w(MARKE, "Abspieler war schon beendet: ${fehler.message}")
        }
        Lautheit.gibFrei(abspieler)
        abspieler.release()
    }

    private fun netzVorhanden(): Boolean {
        val verwaltung = anwendung.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val netz = verwaltung.activeNetwork ?: return false
        val faehigkeiten = verwaltung.getNetworkCapabilities(netz) ?: return false
        return faehigkeiten.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Ein Stueck Sprechtext samt der Absatz-Nummer, zu der es gehoert. */
    private data class Sprechstueck(
        val absatzNummer: Int,
        val absatzText: String,
        val text: String,
    )

    private fun zerlege(text: String): List<Sprechstueck> {
        val absaetze = text.split(ABSATZ_TRENNER)
            .map { absatz -> absatz.trim().replace(LEERRAUM, " ") }
            .filter { absatz -> absatz.isNotBlank() }
        val stuecke = ArrayList<Sprechstueck>()
        absaetze.forEachIndexed { nummer, absatz ->
            teileLangenAbsatz(absatz).forEach { teil ->
                stuecke += Sprechstueck(absatzNummer = nummer, absatzText = absatz, text = teil)
            }
        }
        return stuecke
    }

    /**
     * Sehr lange Absaetze werden an Satzgrenzen geteilt: Google nimmt hoechstens 5000 Byte je
     * Anfrage an, und kurze Stuecke fangen frueher an zu klingen. Die Absatz-Nummer bleibt gleich,
     * damit die Hervorhebung nicht springt.
     */
    private fun teileLangenAbsatz(absatz: String): List<String> {
        if (absatz.length <= HOECHSTLAENGE) return listOf(absatz)
        val teile = ArrayList<String>()
        val bau = StringBuilder()
        SATZ_ENDE.split(absatz).forEach { satz ->
            val kurz = satz.trim()
            if (kurz.isEmpty()) return@forEach
            if (bau.isNotEmpty() && bau.length + kurz.length + 1 > HOECHSTLAENGE) {
                teile += bau.toString()
                bau.setLength(0)
            }
            if (bau.isNotEmpty()) bau.append(' ')
            bau.append(kurz)
        }
        if (bau.isNotEmpty()) teile += bau.toString()
        return if (teile.isEmpty()) listOf(absatz) else teile
    }

    companion object {
        private const val MARKE = "Vorlesen"
        private const val PEGEL_TAKT_MS = 50L
        private const val ERSATZ_PEGEL = 0.45f
        private const val HOECHSTLAENGE = 1200

        private val ABSATZ_TRENNER = Regex("\\r?\\n\\s*\\r?\\n")
        private val LEERRAUM = Regex("\\s+")
        private val SATZ_ENDE = Regex("(?<=[.!?])\\s+")

        /**
         * Die gerade vorlesende Steuerung. Der Vordergrunddienst braucht sie fuer den
         * Stopp-Knopf in der Benachrichtigung.
         */
        @Volatile
        private var laufende: VorleseSteuerung? = null

        /** Stoppt ein laufendes Vorlesen — aus der Benachrichtigung heraus aufgerufen. */
        fun stoppeLaufendes() {
            laufende?.stopp()
        }
    }
}

/**
 * Holt die Zugangsschluessel — niemals aus dem Quelltext.
 *
 * Zwei Quellen, in dieser Reihenfolge:
 *  1. Die verschluesselte Einstellung, die Frank in B-10 hinterlegt.
 *  2. Eine Schluesseldatei unter `<Dateien der App>/sk/`. Dorthin wird der Inhalt aus
 *     `$HOME/SK/StackLabor/` gelegt (`google-tts.key`, `dashscope.key`, `qwen-voice-id.txt`) —
 *     dieselbe Ablage wie im Vorbildprojekt PerfectMoment.
 */
class SchluesselSpeicher(context: Context) {

    private val anwendung: Context = context.applicationContext

    private val ablage: SharedPreferences? by lazy {
        try {
            val hauptschluessel = MasterKey.Builder(anwendung)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                anwendung,
                DATEI,
                hauptschluessel,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (fehler: Exception) {
            Log.w(MARKE, "Verschluesselte Ablage nicht verfuegbar: ${fehler.message}")
            null
        }
    }

    suspend fun googleSchluessel(): String = lies(NAME_GOOGLE, "google-tts.key")

    suspend fun qwenSchluessel(): String = lies(NAME_QWEN, "dashscope.key")

    suspend fun qwenStimmKennung(): String = lies(NAME_QWEN_STIMME, "qwen-voice-id.txt")

    /** Hinterlegt einen Schluessel aus den Einstellungen. Leerer Wert loescht ihn. */
    suspend fun hinterlege(name: String, wert: String) = withContext(Dispatchers.IO) {
        val speicher = ablage ?: return@withContext
        val sauber = wert.trim()
        val bearbeiter = speicher.edit()
        if (sauber.isBlank()) bearbeiter.remove(name) else bearbeiter.putString(name, sauber)
        bearbeiter.apply()
    }

    private suspend fun lies(name: String, dateiname: String): String = withContext(Dispatchers.IO) {
        val ausEinstellung = ablage?.getString(name, null)?.trim().orEmpty()
        if (ausEinstellung.isNotBlank()) return@withContext ausEinstellung
        val datei = File(File(anwendung.filesDir, "sk"), dateiname)
        if (!datei.isFile) return@withContext ""
        try {
            datei.readText().trim()
        } catch (fehler: Exception) {
            Log.w(MARKE, "Schluesseldatei $dateiname nicht lesbar: ${fehler.message}")
            ""
        }
    }

    companion object {
        const val NAME_GOOGLE = "google_tts_key"
        const val NAME_QWEN = "qwen_key"
        const val NAME_QWEN_STIMME = "qwen_voice_id"

        private const val DATEI = "vorlese_schluessel"
        private const val MARKE = "VorleseSchluessel"
    }
}

/**
 * Sorgt dafuer, dass jede gesprochene Zeile mit voller Kraft herauskommt — uebernommen aus
 * `SpeechLoudness.kt` (PerfectMoment).
 *
 * Drei Dinge fehlten dort urspruenglich und kosteten jeweils Lautstaerke: keine
 * [AudioAttributes] (die Wiedergabe meldete sich nicht als Sprache auf dem Medien-Kanal, den die
 * Lautstaerketasten steuern), keine ausdrueckliche Kanal-Lautstaerke und gar keine Verstaerkung.
 */
internal object Lautheit {

    /** +12 dB. Der Verstaerker komprimiert vor dem Anheben, deshalb knackt nichts. */
    private const val ZIEL_VERSTAERKUNG_MILLIBEL = 1200
    private const val MARKE = "Lautheit"

    private val verstaerker = WeakHashMap<MediaPlayer, LoudnessEnhancer>()

    /** Sprache auf dem Medien-Kanal: Lautstaerketasten wirken, Abduecken verhaelt sich richtig. */
    val eigenschaften: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /** Direkt nach `prepare()` und vor `start()` aufrufen. */
    fun verstaerke(abspieler: MediaPlayer) {
        try {
            abspieler.setVolume(1f, 1f)
        } catch (fehler: Exception) {
            Log.w(MARKE, "Lautstaerke liess sich nicht setzen: ${fehler.message}")
        }
        try {
            val geraet = LoudnessEnhancer(abspieler.audioSessionId)
            geraet.setTargetGain(ZIEL_VERSTAERKUNG_MILLIBEL)
            geraet.enabled = true
            synchronized(verstaerker) { verstaerker[abspieler] = geraet }
        } catch (fehler: Exception) {
            // Manche Geraete kennen den Effekt nicht. Leiser ist schlecht, stumm waere schlimmer.
            Log.w(MARKE, "Verstaerker nicht verfuegbar, es wird unverstaerkt gespielt: ${fehler.message}")
        }
    }

    /** Vor `release()` aufrufen — der Effekt ueberlebt den Abspieler sonst. */
    fun gibFrei(abspieler: MediaPlayer?) {
        val geraet = synchronized(verstaerker) { verstaerker.remove(abspieler) } ?: return
        try {
            geraet.enabled = false
            geraet.release()
        } catch (fehler: Exception) {
            Log.w(MARKE, "Verstaerker liess sich nicht freigeben: ${fehler.message}")
        }
    }
}

/**
 * Berechnet die Lautstaerke-Huellkurve einer Aufnahme: Die Datei wird einmal dekodiert und in
 * Fenstern von [FENSTER_MS] Millisekunden der Effektivwert gebildet.
 *
 * DECISION: Der naheliegende Weg (`Visualizer`) braucht die Mikrofon-Berechtigung — fuer drei
 * Pegelbalken ist das unverhaeltnismaessig. Die Huellkurve liefert denselben Eindruck aus den
 * Audiodaten selbst, ohne zusaetzliche Berechtigung.
 */
internal object Huellkurve {

    const val FENSTER_MS = 50

    private const val MARKE = "Huellkurve"
    private const val WARTE_US = 10_000L

    /** Gibt je Fenster einen Wert 0..1 zurueck, oder `null` wenn die Datei nicht lesbar war. */
    fun berechne(datei: File): FloatArray? {
        var zieher: MediaExtractor? = null
        var dekoder: MediaCodec? = null
        return try {
            val entnahme = MediaExtractor()
            zieher = entnahme
            entnahme.setDataSource(datei.absolutePath)
            val spur = (0 until entnahme.trackCount).firstOrNull { nummer ->
                entnahme.getTrackFormat(nummer).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return null
            entnahme.selectTrack(spur)
            val format = entnahme.getTrackFormat(spur)
            val art = format.getString(MediaFormat.KEY_MIME) ?: return null
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var kanaele = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val werk = MediaCodec.createDecoderByType(art)
            dekoder = werk
            werk.configure(format, null, null, 0)
            werk.start()

            val werte = ArrayList<Float>()
            var summe = 0.0
            var anzahl = 0
            var proFenster = fensterGroesse(rate, kanaele)
            val info = MediaCodec.BufferInfo()
            var eingabeFertig = false
            var ausgabeFertig = false

            while (!ausgabeFertig) {
                if (!eingabeFertig) {
                    val platz = werk.dequeueInputBuffer(WARTE_US)
                    if (platz >= 0) {
                        val puffer = werk.getInputBuffer(platz)
                        val gelesen = if (puffer == null) -1 else entnahme.readSampleData(puffer, 0)
                        if (gelesen < 0) {
                            werk.queueInputBuffer(
                                platz, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            eingabeFertig = true
                        } else {
                            werk.queueInputBuffer(platz, 0, gelesen, entnahme.sampleTime, 0)
                            entnahme.advance()
                        }
                    }
                }

                val platz = werk.dequeueOutputBuffer(info, WARTE_US)
                when {
                    platz == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val neu = werk.outputFormat
                        rate = neu.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        kanaele = neu.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        proFenster = fensterGroesse(rate, kanaele)
                    }

                    platz >= 0 -> {
                        val puffer = werk.getOutputBuffer(platz)
                        if (puffer != null && info.size > 0) {
                            puffer.position(info.offset)
                            puffer.limit(info.offset + info.size)
                            val proben = puffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                            while (proben.hasRemaining()) {
                                val wert = proben.get() / 32768f
                                summe += (wert * wert).toDouble()
                                anzahl++
                                if (anzahl >= proFenster) {
                                    werte += sqrt(summe / anzahl).toFloat()
                                    summe = 0.0
                                    anzahl = 0
                                }
                            }
                        }
                        werk.releaseOutputBuffer(platz, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            ausgabeFertig = true
                        }
                    }

                    else -> Unit
                }
            }
            if (anzahl > 0) werte += sqrt(summe / anzahl).toFloat()
            normiere(werte)
        } catch (fehler: Exception) {
            Log.w(MARKE, "Pegel liess sich nicht berechnen: ${fehler.message}")
            null
        } finally {
            try {
                dekoder?.stop()
            } catch (fehler: Exception) {
                Log.w(MARKE, "Dekoder liess sich nicht stoppen: ${fehler.message}")
            }
            dekoder?.release()
            zieher?.release()
        }
    }

    private fun fensterGroesse(rate: Int, kanaele: Int): Int =
        maxOf(1, rate * maxOf(1, kanaele) * FENSTER_MS / 1000)

    /** Hebt die Kurve auf 0..1 an, damit die Balken unabhaengig vom Anbieter gleich aussehen. */
    private fun normiere(werte: List<Float>): FloatArray? {
        if (werte.isEmpty()) return null
        val hoechster = werte.max()
        if (hoechster <= 0f) return null
        return FloatArray(werte.size) { stelle -> (werte[stelle] / hoechster).coerceIn(0f, 1f) }
    }
}
