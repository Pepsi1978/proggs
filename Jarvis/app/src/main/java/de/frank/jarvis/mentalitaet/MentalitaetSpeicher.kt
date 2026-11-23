package de.frank.jarvis.mentalitaet

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom

import java.util.Locale

const val MIN_WIEDERHOLUNGEN = 1
const val MAX_WIEDERHOLUNGEN = 10

const val MIN_PAUSE_SEKUNDEN = 3
const val MAX_PAUSE_SEKUNDEN = 30
const val STANDARD_PAUSE_SEKUNDEN = 5

/**
 * Parst eine Sprach- oder Textangabe für die Pause zwischen Sätzen (3 bis 30 Sekunden).
 * Akzeptiert ganze Zahlen mit optionaler Einheit („10“, „10 s“, „10 Sekunden“), „standard“/„zurücksetzen“ sowie „minimal“/„maximal“.
 * Wirft [IllegalArgumentException] mit verständlichem Hinweis bei ungültigen Werten oder Bereichen.
 */
fun parseMentalitaetPauseSekunden(eingabe: String): Int {
    val normiert = eingabe.trim().lowercase(Locale.GERMAN)
    when (normiert) {
        "standard", "zuruecksetzen", "zurücksetzen", "default" -> return STANDARD_PAUSE_SEKUNDEN
        "minimal", "minimum", "min" -> return MIN_PAUSE_SEKUNDEN
        "maximal", "maximum", "max", "voll" -> return MAX_PAUSE_SEKUNDEN
    }
    val treffer = Regex("""^(-?\d+)\s*(?:s|sek\.?|sekunden?)?$""").matchEntire(normiert)
        ?: throw IllegalArgumentException(
            "„${eingabe.trim()}“ ist keine gültige Sekundenzahl. Erlaubt: ganze Sekunden von $MIN_PAUSE_SEKUNDEN bis $MAX_PAUSE_SEKUNDEN.",
        )
    val wert = treffer.groupValues[1].toIntOrNull()
        ?: throw IllegalArgumentException(
            "„${eingabe.trim()}“ ist keine gültige Zahl. Erlaubt: $MIN_PAUSE_SEKUNDEN bis $MAX_PAUSE_SEKUNDEN Sekunden.",
        )
    if (wert !in MIN_PAUSE_SEKUNDEN..MAX_PAUSE_SEKUNDEN) {
        throw IllegalArgumentException(
            "$wert liegt außerhalb von $MIN_PAUSE_SEKUNDEN bis $MAX_PAUSE_SEKUNDEN Sekunden.",
        )
    }
    return wert
}

/**
 * Ein persönlicher mentaler Satz mit seiner Wiederholungszahl (1 bis 10).
 * Die [id] ist ein stabiler positiver Long-Wert (direkt passend für Drag-and-Drop und Abgleich).
 */
data class MentalSatz(
    val id: Long,
    val text: String,
    val wiederholungen: Int = MIN_WIEDERHOLUNGEN,
) {
    fun bereinigt(): MentalSatz = copy(
        text = text.trim(),
        wiederholungen = wiederholungen.coerceIn(MIN_WIEDERHOLUNGEN, MAX_WIEDERHOLUNGEN),
    )

    fun json(): JSONObject = JSONObject()
        .put("id", id)
        .put("text", text)
        .put("wiederholungen", wiederholungen.coerceIn(MIN_WIEDERHOLUNGEN, MAX_WIEDERHOLUNGEN))

    companion object {
        fun aus(o: JSONObject): MentalSatz? {
            val id = o.optLong("id", 0L)
            val text = o.optString("text").trim()
            if (id <= 0L || text.isEmpty()) return null
            val wiederholungen = o.optInt("wiederholungen", MIN_WIEDERHOLUNGEN)
                .coerceIn(MIN_WIEDERHOLUNGEN, MAX_WIEDERHOLUNGEN)
            return MentalSatz(id = id, text = text, wiederholungen = wiederholungen)
        }
    }
}

/**
 * Dauerhafter Speicher für Franks mentale Sätze in `files/mentalitaet/saetze.json`.
 * Text, Reihenfolge und Wiederholungszahl überleben Schließen der Ansicht und Neustart der App
 * und werden über [de.frank.jarvis.abgleich.GedaechtnisAbgleich] zwischen Handy und Tablet abgeglichen.
 */
object MentalitaetSpeicher {
    const val DATEI = "mentalitaet/saetze.json"

    private val _stand = MutableStateFlow<List<MentalSatz>>(emptyList())
    val stand: StateFlow<List<MentalSatz>> = _stand

    private val _hinweis = MutableStateFlow<String?>(null)
    val hinweis: StateFlow<String?> = _hinweis

    @Volatile private var geladen = false
    private val sperre = Mutex()
    private val zufall = SecureRandom()

    private fun datei(context: Context) = File(context.filesDir, DATEI)

    /** Für den Abgleich: Schreiben von außen unter derselben Sperre wie eigene Änderungen. */
    fun vonAussen(block: () -> Unit) = runBlocking {
        sperre.withLock {
            try {
                block()
            } finally {
                geladen = false
            }
        }
    }

    /** Nach dem Abgleich: neu einlesen, damit die Ansicht sofort den aktuellen Stand zeigt. */
    fun neuLaden(context: Context) = runBlocking {
        sperre.withLock {
            geladen = false
            laden(context.applicationContext)
        }
    }

    /**
     * Drei-Wege-Zusammenführung für den Abgleich bei gleichzeitiger Änderung auf Handy und Tablet.
     * Je Satz (nach [MentalSatz.id]): nur eine Seite geändert → deren Fassung; beide geändert → jüngere;
     * auf einer Seite gelöscht und auf der anderen unverändert → weg; neu auf einer Seite → bleibt.
     * Reihenfolge: hat nur eine Seite umsortiert, gilt deren Reihenfolge, sonst die der jüngeren.
     */
    fun fuehreZusammen(basisText: String, lokalText: String, fernText: String, fernGewinnt: Boolean): String {
        fun liste(t: String): List<JSONObject> = JSONArray(t).let { a ->
            (0 until a.length()).mapNotNull { a.optJSONObject(it) }
                .filter { it.optLong("id", 0L) > 0L && it.optString("text").isNotBlank() }
        }
        val b = liste(basisText).associateBy { it.optLong("id") }
        val l = liste(lokalText)
        val f = liste(fernText)
        val lm = l.associateBy { it.optLong("id") }
        val fm = f.associateBy { it.optLong("id") }
        fun gleich(x: JSONObject?, y: JSONObject?) =
            x?.optString("text") == y?.optString("text") && x?.optInt("wiederholungen", 1) == y?.optInt("wiederholungen", 1)

        val ergebnis = LinkedHashMap<Long, JSONObject>()
        for (id in (lm.keys + fm.keys)) {
            val le = lm[id]
            val fe = fm[id]
            val be = b[id]
            val wahl = if (be == null) {
                if (fernGewinnt) fe ?: le else le ?: fe
            } else when {
                le == null -> fe?.takeIf { !gleich(it, be) }
                fe == null -> le.takeIf { !gleich(it, be) }
                gleich(le, be) -> fe
                gleich(fe, be) -> le
                else -> if (fernGewinnt) fe else le
            }
            if (wahl != null) ergebnis[id] = wahl
        }

        fun folge(x: List<JSONObject>) = x.map { it.optLong("id") }.filter { it in ergebnis }
        val basisFolge = b.keys.filter { it in ergebnis }
        fun gemeinsam(x: List<Long>, y: List<Long>) = x.filter { it in y } == y.filter { it in x }
        val (erst, zweit) = when {
            gemeinsam(folge(l), basisFolge) -> folge(f) to folge(l)
            gemeinsam(folge(f), basisFolge) -> folge(l) to folge(f)
            fernGewinnt -> folge(f) to folge(l)
            else -> folge(l) to folge(f)
        }
        val reihe = erst.toMutableList()
        zweit.forEachIndexed { i, id ->
            if (id in reihe) return@forEachIndexed
            val vor = zweit.take(i).lastOrNull { it in reihe }
            reihe.add(if (vor == null) 0 else reihe.indexOf(vor) + 1, id)
        }
        return JSONArray().apply { reihe.forEach { put(ergebnis.getValue(it)) } }.toString(2)
    }

    suspend fun sicher(context: Context) {
        if (geladen) return
        sperre.withLock { laden(context.applicationContext) }
    }

    private suspend fun laden(context: Context) {
        if (geladen) return
        withContext(Dispatchers.IO) {
            val d = datei(context)
            val liste = if (d.exists()) runCatching {
                parseListe(d.readText())
            }.getOrElse {
                val sicherung = File(d.parentFile, "saetze.defekt-${System.currentTimeMillis()}.json")
                d.renameTo(sicherung)
                _hinweis.value = "Die Mentalität-Datei war beschädigt und liegt als ${sicherung.name} beiseite. Neue Sätze werden frisch gespeichert."
                emptyList()
            } else emptyList()
            _stand.value = liste
            geladen = true
        }
    }

    internal fun parseListe(jsonText: String): List<MentalSatz> {
        val a = JSONArray(jsonText)
        val gesehen = HashSet<Long>()
        val aus = ArrayList<MentalSatz>(a.length())
        for (i in 0 until a.length()) {
            val satz = a.optJSONObject(i)?.let(MentalSatz::aus) ?: continue
            if (gesehen.add(satz.id)) aus += satz
        }
        return aus
    }

    internal fun serialisiereListe(liste: List<MentalSatz>): String =
        JSONArray().apply { liste.forEach { put(it.bereinigt().json()) } }.toString(2)

    private fun neueId(bestehend: Set<Long>): Long {
        while (true) {
            val kandidat = (zufall.nextLong() and Long.MAX_VALUE).coerceAtLeast(1L)
            if (kandidat !in bestehend) return kandidat
        }
    }

    private fun schreibe(context: Context, liste: List<MentalSatz>) {
        val d = datei(context).apply { parentFile?.mkdirs() }
        val tmp = File(d.parentFile, "saetze.json.tmp")
        tmp.writeText(serialisiereListe(liste))
        if (!tmp.renameTo(d)) {
            d.delete()
            check(tmp.renameTo(d)) { "Mentalität-Datei ließ sich nicht ersetzen" }
        }
        _stand.value = liste
        _hinweis.value = null
        runCatching { de.frank.jarvis.abgleich.GedaechtnisAbgleich.anstossen() }
    }

    suspend fun hinzufuegen(context: Context, text: String, wiederholungen: Int = MIN_WIEDERHOLUNGEN): MentalSatz? {
        val sauber = text.trim()
        if (sauber.isEmpty()) return null
        val app = context.applicationContext
        return sperre.withLock {
            laden(app)
            withContext(Dispatchers.IO + NonCancellable) {
                val alt = _stand.value
                val neuSatz = MentalSatz(
                    id = neueId(alt.map { it.id }.toSet()),
                    text = sauber,
                    wiederholungen = wiederholungen.coerceIn(MIN_WIEDERHOLUNGEN, MAX_WIEDERHOLUNGEN),
                )
                runCatching {
                    schreibe(app, alt + neuSatz)
                    neuSatz
                }.onFailure {
                    _hinweis.value = "Der Satz ließ sich nicht speichern (Speicher voll?)."
                }.getOrNull()
            }
        }
    }

    suspend fun wiederholungenSetzen(context: Context, id: Long, wert: Int) {
        val clamped = wert.coerceIn(MIN_WIEDERHOLUNGEN, MAX_WIEDERHOLUNGEN)
        val app = context.applicationContext
        sperre.withLock {
            laden(app)
            withContext(Dispatchers.IO + NonCancellable) {
                val alt = _stand.value
                val vorhanden = alt.firstOrNull { it.id == id } ?: return@withContext
                if (vorhanden.wiederholungen == clamped) return@withContext
                val neu = alt.map { if (it.id == id) it.copy(wiederholungen = clamped) else it }
                runCatching { schreibe(app, neu) }
                    .onFailure { _hinweis.value = "Die Wiederholungszahl ließ sich nicht speichern (Speicher voll?)." }
            }
        }
    }

    suspend fun textAendern(context: Context, id: Long, neuerText: String) {
        val sauber = neuerText.trim()
        if (sauber.isEmpty()) return
        val app = context.applicationContext
        sperre.withLock {
            laden(app)
            withContext(Dispatchers.IO + NonCancellable) {
                val alt = _stand.value
                val vorhanden = alt.firstOrNull { it.id == id } ?: return@withContext
                if (vorhanden.text == sauber) return@withContext
                val neu = alt.map { if (it.id == id) it.copy(text = sauber) else it }
                runCatching { schreibe(app, neu) }
                    .onFailure { _hinweis.value = "Die Änderung ließ sich nicht speichern (Speicher voll?)." }
            }
        }
    }

    suspend fun loeschen(context: Context, id: Long) {
        val app = context.applicationContext
        sperre.withLock {
            laden(app)
            withContext(Dispatchers.IO + NonCancellable) {
                val alt = _stand.value
                if (alt.none { it.id == id }) return@withContext
                val neu = alt.filterNot { it.id == id }
                runCatching { schreibe(app, neu) }
                    .onFailure { _hinweis.value = "Der Satz ließ sich nicht löschen (Speicher voll?)." }
            }
        }
    }

    /** Neue Reihenfolge nach Drag-and-Drop; fehlende Kennungen bleiben hinten in alter Reihenfolge. */
    suspend fun reihenfolge(context: Context, ids: List<Long>) {
        val app = context.applicationContext
        sperre.withLock {
            laden(app)
            withContext(Dispatchers.IO + NonCancellable) {
                val alt = _stand.value
                val neu = ordneNachIds(alt, ids)
                if (neu.map { it.id } != alt.map { it.id }) {
                    runCatching { schreibe(app, neu) }
                        .onFailure { _hinweis.value = "Die neue Reihenfolge ließ sich nicht speichern (Speicher voll?)." }
                }
            }
        }
    }
}

/** Ordnet [liste] nach der Reihenfolge in [ids]; nicht genannte Sätze bleiben hinten in alter Folge. */
fun ordneNachIds(liste: List<MentalSatz>, ids: List<Long>): List<MentalSatz> {
    val rang = ids.withIndex().associate { (i, id) -> id to i }
    return liste.withIndex().sortedBy { (i, s) -> rang[s.id] ?: (ids.size + i) }.map { it.value }
}
