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
            KiLog.kopie("diskussion.json", a.toString())
        }
    }

    internal var auftrag: (suspend (Fortschritt) -> Unit)? = null
    internal var abbruch: (() -> Unit)? = null

    fun starte(
        context: Context,
        art: Art,
        titel: String,
        bezugId: Long? = null,
        /** Ein fortgesetzter Lauf behält die bisherige Diskussion. */
        protokollBehalten: Boolean = false,
        auftrag: suspend (Fortschritt) -> Unit,
    ): Boolean {
        if (laeuft) return false
        this.art = art
        this.titel = titel
        this.bezugId = bezugId
        prozent = 0f
        schritt = "Starte …"
        // Nur ein neuer großer Lauf beginnt eine neue Diskussion; Mitreden baut auf ihr auf.
        if (art == Art.AKTUALISIEREN && !protokollBehalten) { protokoll.clear(); protokollSichern() }
        KiLog.info("KI-Arbeit gestartet: ${art.anzeige} „$titel“" + if (protokollBehalten) " (Fortsetzung, ${protokoll.size} Beiträge behalten)" else "")
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
 * Schätzt den Fortschritt. Einzelaufrufe ([band]): Innerhalb des Prozentbands zählt zuerst die Denkzeit, dann
 * die gestreamten Zeichen. Phasen des großen Laufs ([phase]): Der Balken zählt fertige Aufrufe
 * („3 von 5 Blöcken“) und kriecht zwischen zwei fertigen Aufrufen nach der Zeit weiter – er steht also nie still,
 * nur weil ein Aufruf gerade stumm denkt oder im Internet sucht. Der Balken läuft nie rückwärts.
 */
class Fortschritt internal constructor() {
    private var von = 0f
    private var bis = 1f
    private var erwZeichen = 1
    private var erwSekunden = 30f
    private var bandStart = System.currentTimeMillis()
    private var zeichen = 0
    private var phasenModus = false
    private var gesamt = 1
    private var erledigt = 0

    /** Eine Phase mit [gesamt] gleich großen Aufrufen; [sekundenJeSchritt] = erwartete Zeit bis zum nächsten fertigen Aufruf. */
    fun phase(von: Float, bis: Float, gesamt: Int, sekundenJeSchritt: Float, schritt: String) {
        synchronized(sperre) {
            this.von = von
            this.bis = bis
            this.gesamt = gesamt.coerceAtLeast(1)
            erledigt = 0
            erwSekunden = sekundenJeSchritt.coerceAtLeast(5f)
            bandStart = System.currentTimeMillis()
            zeichen = 0
            phasenModus = true
        }
        KiArbeit.schritt = schritt
        setze(von)
    }

    /** Ein Aufruf der Phase ist fertig (auch wenn er gescheitert ist). */
    fun schrittFertig(schritt: String? = null) {
        synchronized(sperre) {
            erledigt = min(gesamt, erledigt + 1)
            bandStart = System.currentTimeMillis()
        }
        if (schritt != null) KiArbeit.schritt = schritt
        tick()
    }

    fun band(von: Float, bis: Float, erwarteteZeichen: Int, erwarteteSekunden: Float, schritt: String) {
        this.von = von
        this.bis = bis
        erwZeichen = erwarteteZeichen.coerceAtLeast(1)
        erwSekunden = erwarteteSekunden.coerceAtLeast(5f)
        bandStart = System.currentTimeMillis()
        zeichen = 0
        phasenModus = false
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
        if (phasenModus) {
            val anteil = (bis - von) / gesamt
            val weiter = if (erledigt >= gesamt) 0f else anteil * 0.9f * (1f - exp(-sekunden / erwSekunden))
            setze(von + anteil * erledigt + weiter)
            return
        }
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

    /** Vor einer Wiederholung: den halb geschriebenen Live-Text verwerfen. */
    fun liveLeeren() { KiArbeit.live?.let { KiArbeit.live = it.copy(text = "") } }

    fun schritt(text: String) { KiArbeit.schritt = text }

    private val sperre = Any()

    fun fertig() = setze(1f)

    private fun setze(p: Float) = synchronized(sperre) { if (p > KiArbeit.prozent) KiArbeit.prozent = p.coerceIn(0f, 1f) }
}
