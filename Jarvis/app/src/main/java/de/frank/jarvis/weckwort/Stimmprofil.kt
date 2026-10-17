package de.frank.jarvis.weckwort

import android.content.Context
import android.util.Log
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Stimmprofil für „Hey Jarvis“: Ein zweiter Prüfschritt nach dem Weckwort-Modell, wie die „custom verifier“
 * von openWakeWord, nur ohne Training auf dem PC.
 *
 * Beim Anlernen sagt Frank fünfzehnmal „Hey Jarvis“ (nah bis fünf Meter, schnell, leise, laut, aus allen Richtungen); gespeichert werden jeweils die 16 Einbettungen (1536 Werte), über
 * die das Modell geurteilt hat. Später zählt ein Treffer nur, wenn er einer dieser Proben ähnlich genug ist (Kosinus,
 * vorher um den Hintergrund bereinigt, damit nicht alles gleich ähnlich wirkt) und keinem gelernten Fehlalarm näher
 * liegt. Die Schwelle ergibt sich aus den Proben selbst: so ähnlich, wie sich Franks eigene Proben untereinander sind,
 * mit etwas Spielraum.
 *
 * Fehlalarme lernt Jarvis von allein: Kommt nach dem Piepton nichts, hat ihn wohl niemand gemeint.
 */
object Stimmprofil {
    /** So viele Proben braucht das Anlernen: 1–5 nah, 6–10 aus etwa zwei Metern, 11–15 aus etwa fünf Metern. */
    const val ANLERN_ZIEL = 15
    private const val MIN_PROBEN = 3
    private const val MIN_STIMMEN = 5
    private const val MAX_FEHLALARME = 40
    /** So viel ähnlicher als Franks Proben darf ein gelernter Fehlalarm sein, ohne Frank selbst auszusperren. */
    private const val FEHLALARM_SPIEL = 0.03f
    private const val TAG = "Stimmprofil"

    data class Stand(
        val proben: Int, val fehlalarme: Int, val anlernen: Int?, val letzteAehnlichkeit: Float?, val letzteSchwelle: Float?,
        /** Angelernte Stimm-Fingerabdrücke (echte Stimmerkennung); ab fünf entscheidet sie allein. */
        val stimmen: Int = 0,
        val letzteStimme: Float? = null,
        val stimmSchwelle: Float? = null,
    )

    private val _stand = MutableStateFlow(Stand(0, 0, null, null, null))
    val stand: StateFlow<Stand> = _stand.asStateFlow()

    /** Stimm-Fingerabdrücke ([SprecherErkenner]); gelten unabhängig von der Phrase, denn sie erkennen die Person. */
    @Volatile private var stimmen: List<FloatArray> = emptyList()
    @Volatile private var neueStimmen: MutableList<FloatArray>? = null

    /** Echte Stimmerkennung angelernt: Sie entscheidet, ob Frank spricht. */
    val stimmeAktiv: Boolean get() = stimmen.size >= MIN_STIMMEN

    /** Ergebnis der Stimmerkennung, auch fürs Protokoll. [dauer]: Sekunden Sprache im Fingerabdruck. */
    data class StimmPruefung(val ok: Boolean, val aehnlich: Float, val schwelle: Float, val dauer: Float? = null)

    /**
     * So viel tiefer liegt die Stimm-Schwelle je Empfindlichkeit (aus den Einstellungen). Bis 17.10.2026 wirkte die
     * Empfindlichkeit nur aufs Weckwort-Modell, Franks schnelle Rufe scheiterten aber an der Stimmerkennung.
     */
    @Volatile var schwellenAbzug = 0f

    /** Ist das Franks Stimme? null = keine Stimmerkennung angelernt oder zu wenig Sprache in der Aufnahme. */
    fun pruefeStimme(fingerabdruck: FloatArray?, dauer: Float? = null): StimmPruefung? {
        val s = stimmen
        if (s.size < MIN_STIMMEN || fingerabdruck == null) return null
        val aehnlich = stimmWert(fingerabdruck, s)
        // Kurze Rufe (schnell gesprochen, unter 1,2 s Sprache) geben CAM++ wenig Stimme: bis zu 0,04 Spielraum.
        val kurz = dauer?.let { (1.2f - it).coerceIn(0f, 0.4f) * 0.1f } ?: 0f
        val schwelle = maxOf(stimmSchwelle(s) - schwellenAbzug - kurz, STIMM_BODEN)
        val ok = aehnlich >= schwelle
        _stand.value = _stand.value.copy(letzteStimme = aehnlich, stimmSchwelle = schwelle)
        Log.i(TAG, "Stimme ${"%.3f".format(aehnlich)}, Schwelle ${"%.3f".format(schwelle)}, Sprache ${dauer?.let { "%.2f s".format(it) }} → ${if (ok) "Frank" else "fremde Stimme"}")
        return StimmPruefung(ok, aehnlich, schwelle, dauer)
    }

    /**
     * Ähnlichkeit zu Franks Proben: zur Mitte aller, oder zu den drei ähnlichsten einzelnen, was höher ist. So passt
     * ein schneller, leiser Ruf auch zu den schnellen, leisen Proben, statt nur zur von deutlichen Proben geprägten Mitte.
     * Derselbe Wert bestimmt die Schwelle ([stimmSchwelle]), sonst lägen beide auf verschiedenen Skalen.
     */
    private fun stimmWert(f: FloatArray, s: List<FloatArray>): Float {
        val mitte = SprecherErkenner.kosinus(f, SprecherErkenner.normiert(mittel(s)))
        val besteDrei = s.map { SprecherErkenner.kosinus(f, it) }.sortedDescending().take(3).average().toFloat()
        return maxOf(mitte, besteDrei)
    }

    /**
     * Schwelle der Stimmerkennung aus Franks eigenen Proben: Wie ähnlich ist jede Probe den übrigen? Die geringste
     * davon (meist eine aus fünf Metern), mit etwas Spielraum. Nie über 0,55: Schnell gesprochen lag Frank am 15. bis
     * 17.10.2026 oft bei 0,53 bis 0,60 und scheiterte an der alten Grenze 0,60. Nie unter [STIMM_BODEN].
     */
    private fun stimmSchwelle(s: List<FloatArray>): Float {
        val min = s.indices.minOf { i -> stimmWert(s[i], s.filterIndexed { j, _ -> j != i }) }
        return (min - 0.10f).coerceIn(STIMM_BODEN, 0.55f)
    }

    /** Darunter fängt CAM++ an, fremde Stimmen durchzulassen. */
    private const val STIMM_BODEN = 0.40f

    private fun mittel(s: List<FloatArray>): FloatArray = FloatArray(s.first().size) { k -> s.sumOf { it[k].toDouble() }.toFloat() / s.size }

    @Volatile private var proben: List<FloatArray> = emptyList()
    @Volatile private var fehl: List<FloatArray> = emptyList()
    /** Langsam gleitender Mittelwert der Einbettungen ohne Weckwort: der „Hintergrund“. */
    @Volatile private var hintergrund: FloatArray? = null
    private var hintergrundSeitSpeichern = 0
    /** Beim Anlernen gesammelte Proben; null = es wird nicht angelernt. */
    @Volatile private var neu: MutableList<FloatArray>? = null
    /** Erst gesetzt, wenn das Laden fertig ist: Vorher würde [speichern] ein leeres Profil über die Datei schreiben. */
    @Volatile private var datei: File? = null
    @Volatile private var ladeStart = false
    /** Vor dem Ende des Ladens geändert (Anlernen, Löschen): Dann gilt die Änderung, nicht die alte Datei. */
    @Volatile private var geaendert = false
    /** Lesen und Schreiben der Profildatei laufen hier, nie auf dem Hauptfaden oder im Audiofaden. */
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "jarvis-stimmprofil").apply { isDaemon = true } }

    /** Für welches Weckwort das Profil gilt („jarvis“ oder „phrase:OKAY COMPUTER JARVIS“): Eine neue Phrase braucht neue Proben. */
    @Volatile private var gespeicherterSchluessel = "jarvis"
    @Volatile var schluessel = "jarvis"
        private set

    val aktiv: Boolean get() = proben.size >= MIN_PROBEN && gespeicherterSchluessel == schluessel
    val lerntAn: Boolean get() = neu != null

    /** Lädt das Profil im Hintergrund (einige hundert Millisekunden bei großem Profil). Bis dahin gilt „kein Profil“. */
    fun laden(context: Context) {
        synchronized(this) { if (ladeStart) return; ladeStart = true }
        val d = File(context.applicationContext.filesDir, "weckwort_stimme.json")
        io.execute {
            val beginn = System.currentTimeMillis()
            synchronized(this) {
                runCatching {
                    if (!d.exists() || geaendert) return@runCatching
                    val json = JSONObject(d.readText())
                    proben = liste(json.optJSONArray("proben"))
                    fehl = liste(json.optJSONArray("fehlalarme"))
                    hintergrund = json.optJSONArray("hintergrund")?.let { feld(it) }
                    gespeicherterSchluessel = json.optString("schluessel", "jarvis")
                    stimmen = liste(json.optJSONArray("stimmen"))
                }.onFailure { Log.w(TAG, "Stimmprofil nicht lesbar", it); Logbuch.fehler("Stimmprofil nicht lesbar", it) }
                datei = d
            }
            // Was vor dem Ende des Ladens geändert wurde, jetzt sichern.
            if (geaendert) speichern()
            Logbuch.dauer("Stimmprofil geladen", System.currentTimeMillis() - beginn, "${proben.size} Proben, ${stimmen.size} Stimmen")
            melden()
        }
    }

    /** Das gerade eingestellte Weckwort; passt das Profil nicht dazu, ruht es, bis neu angelernt ist. */
    fun schluesselSetzen(neuerSchluessel: String) {
        if (schluessel == neuerSchluessel) return
        schluessel = neuerSchluessel
        neu = null
        melden()
    }

    /** Ergebnis einer Prüfung, auch fürs Protokoll. */
    data class Pruefung(val ok: Boolean, val aehnlich: Float, val schwelle: Float, val fremd: Float)

    /** Urteil über einen Treffer des Weckwort-Modells. Ohne Profil zählt jeder Treffer. */
    fun passt(merkmale: FloatArray?): Boolean = pruefe(merkmale)?.ok ?: true

    /** Wie [passt], mit allen Werten; null = kein Profil für dieses Weckwort, der Treffer zählt. */
    fun pruefe(merkmale: FloatArray?): Pruefung? {
        val p = proben
        if (!aktiv || merkmale == null) return null
        val bg = hintergrund
        val v = bereinigt(merkmale, bg)
        val bereinigteProben = p.map { bereinigt(it, bg) }
        val aehnlich = bereinigteProben.maxOf { aehnlichkeit(v, it) }
        val fremd = fehl.maxOfOrNull { aehnlichkeit(v, bereinigt(it, bg)) } ?: -1f
        val schwelle = schwelle(bereinigteProben)
        _stand.value = _stand.value.copy(letzteAehnlichkeit = aehnlich, letzteSchwelle = schwelle)
        // Etwas Spielraum zum Fehlalarm: Ein einzelner falsch gelernter Eintrag soll Frank nicht aussperren.
        val ok = aehnlich >= schwelle && aehnlich >= fremd - FEHLALARM_SPIEL
        Log.i(TAG, "Ähnlichkeit ${"%.3f".format(aehnlich)}, Schwelle ${"%.3f".format(schwelle)}, Fehlalarm ${"%.3f".format(fremd)} → ${if (ok) "Frank" else "abgewiesen"}")
        return Pruefung(ok, aehnlich, schwelle, fremd)
    }

    /**
     * Schwelle aus den Proben selbst: Für jede Probe die Ähnlichkeit zur nächsten anderen; die geringste davon,
     * mit Spielraum nach unten, damit Frank auch heiser, müde oder im Auto zuverlässig durchkommt.
     */
    private fun schwelle(p: List<FloatArray>): Float {
        val naechste = p.indices.map { i -> p.indices.filter { it != i }.maxOf { j -> aehnlichkeit(p[i], p[j]) } }
        val min = naechste.min()
        return min - 0.5f * (1f - min)
    }

    /** Läuft bei jedem Block ohne Weckwort: hält den Hintergrund aktuell. */
    fun hintergrundLernen(merkmale: FloatArray) {
        val bg = hintergrund
        hintergrund = if (bg == null || bg.size != merkmale.size) merkmale.copyOf()
        else FloatArray(bg.size) { bg[it] + (merkmale[it] - bg[it]) * 0.002f }
        // Etwa alle zehn Minuten sichern, damit der Hintergrund einen Neustart übersteht.
        if (++hintergrundSeitSpeichern >= 7_500) { hintergrundSeitSpeichern = 0; spaeterSpeichern() }
    }

    // ---- Anlernen ----

    fun anlernenStarten() {
        neu = mutableListOf()
        neueStimmen = mutableListOf()
        melden()
    }

    fun anlernenAbbrechen() {
        neu = null
        neueStimmen = null
        melden()
    }

    /**
     * Eine Probe beim Anlernen: Lautmerkmale des Weckworts und, wenn genug Sprache darin war, der Stimm-Fingerabdruck.
     * true = genug Proben, das Profil ist gespeichert.
     */
    fun probe(merkmale: FloatArray, fingerabdruck: FloatArray?): Boolean {
        val liste = neu ?: return false
        liste += merkmale.copyOf()
        fingerabdruck?.let { neueStimmen?.add(it.copyOf()) }
        if (liste.size < ANLERN_ZIEL) { melden(); return false }
        proben = liste.toList()
        gespeicherterSchluessel = schluessel
        // Ein neues Profil fängt auch bei den Fehlalarmen von vorn an.
        fehl = emptyList()
        neueStimmen?.takeIf { it.size >= MIN_STIMMEN }?.let { stimmen = it.toList() }
        Log.i(TAG, "Angelernt: ${liste.size} Proben, ${neueStimmen?.size ?: 0} Stimm-Fingerabdrücke")
        neu = null
        neueStimmen = null
        geaendert = true
        spaeterSpeichern()
        melden()
        return true
    }

    /** Nach „Hey Jarvis“ kam nichts: Dieser Klang war wohl nicht für Jarvis gemeint. */
    fun fehlalarm(merkmale: FloatArray?) {
        if (merkmale == null || !aktiv) return
        fehl = (fehl + merkmale.copyOf()).takeLast(MAX_FEHLALARME)
        spaeterSpeichern()
        melden()
    }

    fun loeschen() {
        proben = emptyList()
        fehl = emptyList()
        stimmen = emptyList()
        neu = null
        neueStimmen = null
        geaendert = true
        spaeterSpeichern()
        _stand.value = Stand(0, 0, null, null, null)
    }

    // ---- Hilfen ----

    private fun melden() {
        val passend = gespeicherterSchluessel == schluessel
        _stand.value = _stand.value.copy(proben = if (passend) proben.size else 0, fehlalarme = if (passend) fehl.size else 0, anlernen = neu?.size, stimmen = stimmen.size)
    }

    @Synchronized
    private fun speichern() {
        val d = datei ?: return
        runCatching {
            val json = JSONObject()
                .put("proben", JSONArray().apply { proben.forEach { put(alsJson(it)) } })
                .put("fehlalarme", JSONArray().apply { fehl.forEach { put(alsJson(it)) } })
                .put("schluessel", gespeicherterSchluessel)
                .put("stimmen", JSONArray().apply { stimmen.forEach { put(alsJson(it)) } })
            hintergrund?.let { json.put("hintergrund", alsJson(it)) }
            val tmp = File(d.parentFile, d.name + ".tmp")
            tmp.writeText(json.toString())
            tmp.renameTo(d)
        }.onFailure { Log.w(TAG, "Stimmprofil nicht gespeichert", it); Logbuch.fehler("Stimmprofil nicht gespeichert", it) }
    }

    /** Speichert auf dem eigenen Faden: Das ganze Profil ist groß, Hauptfaden und Mikrofon sollen nicht warten. */
    private fun spaeterSpeichern() {
        runCatching { io.execute { speichern() } }
    }

    private fun alsJson(v: FloatArray) = JSONArray().apply { v.forEach { put(it.toDouble()) } }
    private fun feld(a: JSONArray) = FloatArray(a.length()) { a.optDouble(it).toFloat() }
    private fun liste(a: JSONArray?): List<FloatArray> = if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONArray(it)?.let(::feld) }

    private fun bereinigt(v: FloatArray, bg: FloatArray?): FloatArray =
        if (bg == null || bg.size != v.size) v else FloatArray(v.size) { v[it] - bg[it] }

    private fun aehnlichkeit(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return -1f
        var ab = 0.0; var aa = 0.0; var bb = 0.0
        for (i in a.indices) { ab += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i] }
        return if (aa == 0.0 || bb == 0.0) 0f else (ab / sqrt(aa * bb)).toFloat()
    }
}
