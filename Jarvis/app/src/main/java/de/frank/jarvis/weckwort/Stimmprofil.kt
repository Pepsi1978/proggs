package de.frank.jarvis.weckwort

import android.content.Context
import android.util.Log
import java.io.File
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
 * Beim Anlernen sagt Frank sechsmal „Hey Jarvis“; gespeichert werden jeweils die 16 Einbettungen (1536 Werte), über
 * die das Modell geurteilt hat. Später zählt ein Treffer nur, wenn er einer dieser Proben ähnlich genug ist (Kosinus,
 * vorher um den Hintergrund bereinigt, damit nicht alles gleich ähnlich wirkt) und keinem gelernten Fehlalarm näher
 * liegt. Die Schwelle ergibt sich aus den Proben selbst: so ähnlich, wie sich Franks eigene Proben untereinander sind,
 * mit etwas Spielraum.
 *
 * Fehlalarme lernt Jarvis von allein: Kommt nach dem Piepton nichts, hat ihn wohl niemand gemeint.
 */
object Stimmprofil {
    /** So viele Proben braucht das Anlernen. */
    const val ANLERN_ZIEL = 6
    private const val MIN_PROBEN = 3
    private const val MAX_FEHLALARME = 40
    private const val TAG = "Stimmprofil"

    data class Stand(val proben: Int, val fehlalarme: Int, val anlernen: Int?, val letzteAehnlichkeit: Float?, val letzteSchwelle: Float?)

    private val _stand = MutableStateFlow(Stand(0, 0, null, null, null))
    val stand: StateFlow<Stand> = _stand.asStateFlow()

    @Volatile private var proben: List<FloatArray> = emptyList()
    @Volatile private var fehl: List<FloatArray> = emptyList()
    /** Langsam gleitender Mittelwert der Einbettungen ohne Weckwort: der „Hintergrund“. */
    @Volatile private var hintergrund: FloatArray? = null
    private var hintergrundSeitSpeichern = 0
    /** Beim Anlernen gesammelte Proben; null = es wird nicht angelernt. */
    @Volatile private var neu: MutableList<FloatArray>? = null
    private var datei: File? = null

    /** Für welches Weckwort das Profil gilt („jarvis“ oder „phrase:OKAY COMPUTER JARVIS“): Eine neue Phrase braucht neue Proben. */
    @Volatile private var gespeicherterSchluessel = "jarvis"
    @Volatile var schluessel = "jarvis"
        private set

    val aktiv: Boolean get() = proben.size >= MIN_PROBEN && gespeicherterSchluessel == schluessel
    val lerntAn: Boolean get() = neu != null

    @Synchronized
    fun laden(context: Context) {
        if (datei != null) return
        val d = File(context.applicationContext.filesDir, "weckwort_stimme.json")
        datei = d
        runCatching {
            if (!d.exists()) return@runCatching
            val json = JSONObject(d.readText())
            proben = liste(json.optJSONArray("proben"))
            fehl = liste(json.optJSONArray("fehlalarme"))
            hintergrund = json.optJSONArray("hintergrund")?.let { feld(it) }
            gespeicherterSchluessel = json.optString("schluessel", "jarvis")
        }.onFailure { Log.w(TAG, "Stimmprofil nicht lesbar", it) }
        melden()
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
        val ok = aehnlich >= schwelle && aehnlich >= fremd
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
        if (++hintergrundSeitSpeichern >= 7_500) { hintergrundSeitSpeichern = 0; speichern() }
    }

    // ---- Anlernen ----

    fun anlernenStarten() {
        neu = mutableListOf()
        melden()
    }

    fun anlernenAbbrechen() {
        neu = null
        melden()
    }

    /** Eine Probe beim Anlernen. true = genug Proben, das Profil ist gespeichert. */
    fun probe(merkmale: FloatArray): Boolean {
        val liste = neu ?: return false
        liste += merkmale.copyOf()
        if (liste.size < ANLERN_ZIEL) { melden(); return false }
        proben = liste.toList()
        gespeicherterSchluessel = schluessel
        // Ein neues Profil fängt auch bei den Fehlalarmen von vorn an.
        fehl = emptyList()
        neu = null
        speichern()
        melden()
        return true
    }

    /** Nach „Hey Jarvis“ kam nichts: Dieser Klang war wohl nicht für Jarvis gemeint. */
    fun fehlalarm(merkmale: FloatArray?) {
        if (merkmale == null || !aktiv) return
        fehl = (fehl + merkmale.copyOf()).takeLast(MAX_FEHLALARME)
        speichern()
        melden()
    }

    fun loeschen() {
        proben = emptyList()
        fehl = emptyList()
        neu = null
        speichern()
        _stand.value = Stand(0, 0, null, null, null)
    }

    // ---- Hilfen ----

    private fun melden() {
        val passend = gespeicherterSchluessel == schluessel
        _stand.value = _stand.value.copy(proben = if (passend) proben.size else 0, fehlalarme = if (passend) fehl.size else 0, anlernen = neu?.size)
    }

    @Synchronized
    private fun speichern() {
        val d = datei ?: return
        runCatching {
            val json = JSONObject()
                .put("proben", JSONArray().apply { proben.forEach { put(alsJson(it)) } })
                .put("fehlalarme", JSONArray().apply { fehl.forEach { put(alsJson(it)) } })
                .put("schluessel", gespeicherterSchluessel)
            hintergrund?.let { json.put("hintergrund", alsJson(it)) }
            val tmp = File(d.parentFile, d.name + ".tmp")
            tmp.writeText(json.toString())
            tmp.renameTo(d)
        }.onFailure { Log.w(TAG, "Stimmprofil nicht gespeichert", it) }
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
