package de.frank.longevity.ki

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

enum class Art(val anzeige: String) { AUSWERTEN("Auswertung"), VERTIEFEN("Vertiefung"), AKTUALISIEREN("Aktualisierung"), EINWAND("Mitdiskutieren") }

/** Arbeiten, die im Diskussions-Protokoll mitlaufen. */
val Art?.diskutiert: Boolean get() = this == Art.AKTUALISIEREN || this == Art.EINWAND

/** Ein Diskussionsbeitrag eines Agenten. */
data class Beitrag(val name: String, val text: String)

/**
 * Der Zustand der gerade laufenden KI-Arbeit – für die ganze App sichtbar (Fortschrittsbalken,
 * Diskussionsprotokoll), unabhängig davon, welcher Bildschirm offen ist. Ausgeführt wird die Arbeit
 * im [KiDienst], damit sie auch bei ausgeschaltetem Bildschirm weiterläuft.
 */
object KiArbeit {
    var laeuft by mutableStateOf(false); private set
    var art by mutableStateOf<Art?>(null); private set
    var titel by mutableStateOf(""); private set
    var prozent by mutableFloatStateOf(0f); internal set
    var schritt by mutableStateOf(""); internal set
    val protokoll = mutableStateListOf<Beitrag>()
    var live by mutableStateOf<Beitrag?>(null); internal set
    var fehler by mutableStateOf<String?>(null); internal set
    var ergebnis by mutableStateOf<String?>(null); internal set
    /** Faktor, auf den sich die Arbeit bezieht (vertieft bzw. neu eingeordnet). */
    var bezugId by mutableStateOf<Long?>(null); internal set
    var startZeit by mutableStateOf(0L); private set

    /** Die letzte Diskussion liegt als Datei vor, damit man auch nach einem Neustart noch mitreden kann. */
    private var datei: File? = null

    fun protokollLaden(ordner: File) {
        if (datei != null) return
        datei = File(ordner, "diskussion.json")
        if (protokoll.isNotEmpty()) return
        runCatching {
            val a = JSONArray(datei!!.readText())
            protokoll.addAll(List(a.length()) { i -> a.getJSONObject(i).let { Beitrag(it.optString("name"), it.optString("text")) } })
        }
    }

    internal fun protokollSichern() {
        val d = datei ?: return
        runCatching {
            val a = JSONArray()
            protokoll.forEach { a.put(JSONObject().put("name", it.name).put("text", it.text)) }
            d.writeText(a.toString())
        }
    }

    internal var auftrag: (suspend (Fortschritt) -> Unit)? = null
    internal var abbruch: (() -> Unit)? = null

    fun starte(context: Context, art: Art, titel: String, bezugId: Long? = null, auftrag: suspend (Fortschritt) -> Unit): Boolean {
        if (laeuft) return false
        this.art = art
        this.titel = titel
        this.bezugId = bezugId
        prozent = 0f
        schritt = "Starte …"
        // Nur ein neuer großer Lauf beginnt eine neue Diskussion; Mitreden baut auf ihr auf.
        if (art == Art.AKTUALISIEREN) { protokoll.clear(); protokollSichern() }
        live = null
        fehler = null
        ergebnis = null
        startZeit = System.currentTimeMillis()
        this.auftrag = auftrag
        laeuft = true
        ContextCompat.startForegroundService(context, Intent(context, KiDienst::class.java))
        return true
    }

    fun abbrechen() { abbruch?.invoke() }

    internal fun beendet() { laeuft = false; auftrag = null; live = null }

    /** Fertig-Anzeige wegklicken. */
    fun quittieren() { if (!laeuft) { art = null; ergebnis = null; fehler = null } }
}

/**
 * Schätzt den Fortschritt: Jeder Schritt hat ein festes Prozentband. Innerhalb des Bands zählt zuerst
 * die Denkzeit (das Modell denkt, bevor es schreibt), dann die gestreamten Zeichen im Verhältnis zur
 * erwarteten Länge. Der Balken läuft nie rückwärts und erreicht 100 % erst am Ende.
 */
class Fortschritt internal constructor() {
    private var von = 0f
    private var bis = 1f
    private var erwZeichen = 1
    private var erwSekunden = 30f
    private var bandStart = System.currentTimeMillis()
    private var zeichen = 0

    fun band(von: Float, bis: Float, erwarteteZeichen: Int, erwarteteSekunden: Float, schritt: String) {
        this.von = von
        this.bis = bis
        erwZeichen = erwarteteZeichen.coerceAtLeast(1)
        erwSekunden = erwarteteSekunden.coerceAtLeast(5f)
        bandStart = System.currentTimeMillis()
        zeichen = 0
        KiArbeit.schritt = schritt
        setze(von)
    }

    fun zeichen(stueck: String) {
        zeichen += stueck.length
        KiArbeit.live?.let { KiArbeit.live = it.copy(text = it.text + stueck) }
        tick()
    }

    fun tick() {
        val sekunden = (System.currentTimeMillis() - bandStart) / 1000f
        val denken = 0.35f * (1f - exp(-sekunden / erwSekunden))
        val schreiben = if (zeichen > 0) 0.3f + 0.7f * min(1f, zeichen.toFloat() / erwZeichen) else 0f
        setze(von + (bis - von) * min(0.97f, max(denken, schreiben)))
    }

    fun beitragBeginnt(name: String) { KiArbeit.live = Beitrag(name, "") }

    fun beitragFertig(name: String, text: String, liveLeeren: Boolean = true) = synchronized(sperre) {
        KiArbeit.protokoll.add(Beitrag(name, text))
        if (liveLeeren) KiArbeit.live = null
        KiArbeit.protokollSichern()
    }

    /** Für parallele Aufrufe: zählt die Zeichen für den Balken, ohne sie in den Live-Beitrag zu mischen. */
    fun zeichenStill(stueck: String) {
        synchronized(sperre) { zeichen += stueck.length }
        tick()
    }

    /** Statuszeile im Live-Bereich, während mehrere Agenten gleichzeitig arbeiten. */
    fun status(name: String, text: String) { KiArbeit.live = Beitrag(name, text) }

    fun schritt(text: String) { KiArbeit.schritt = text }

    private val sperre = Any()

    fun fertig() = setze(1f)

    private fun setze(p: Float) = synchronized(sperre) { if (p > KiArbeit.prozent) KiArbeit.prozent = p.coerceIn(0f, 1f) }
}
