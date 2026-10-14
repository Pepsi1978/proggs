package de.frank.jarvis.weckwort

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Absprache zwischen dem Weckwort-Dienst und dem Rest von Jarvis (alles läuft im selben Prozess).
 *
 * Das Mikrofon hat immer nur einer: Will Jarvis selbst aufnehmen (Mikrofon-Knopf oder nach „Hey Jarvis“),
 * meldet er es mit [mikrofonHolen] an, der Dienst gibt es frei, und erst dann startet die Aufnahme.
 */
object Weckwort {
    private val _belegt = MutableStateFlow(false)
    /** Jarvis nimmt gerade selbst auf; der Dienst lauscht so lange nicht. */
    val belegt: StateFlow<Boolean> = _belegt.asStateFlow()

    private val _hoertZu = MutableStateFlow(false)
    /** Der Dienst hält gerade das Mikrofon und wartet auf „Hey Jarvis“. */
    val hoertZu: StateFlow<Boolean> = _hoertZu.asStateFlow()

    /** Zuletzt erkannt: Zeitpunkt (Millisekunden) und Sicherheit (0 bis 1), für die Anzeige in den Einstellungen. */
    data class Treffer(val zeitpunkt: Long, val sicherheit: Float)
    private val _letzter = MutableStateFlow<Treffer?>(null)
    val letzter: StateFlow<Treffer?> = _letzter.asStateFlow()

    private val _aktivesWort = MutableStateFlow<String?>(null)
    /** Worauf der Dienst gerade wirklich hört („Hey Jarvis“ oder die eigene Phrase); null = er hört nicht. */
    val aktivesWort: StateFlow<String?> = _aktivesWort.asStateFlow()
    internal fun aktivesWort(wort: String?) { _aktivesWort.value = wort }

    /** Ab diesem Wert gilt „Hey Jarvis“ als erkannt (aus den Einstellungen, gilt sofort beim nächsten Block). */
    @Volatile var schwelle = 0.5f

    /** Der Dienst hat die App nach „Hey Jarvis“ gerufen, die App hat noch nicht übernommen. */
    @Volatile var gerufenUm = 0L
        internal set

    /** Zuletzt vom Stimmprofil abgewiesen (klang nicht nach Frank), für die Anzeige in den Einstellungen. */
    private val _abgewiesen = MutableStateFlow<Treffer?>(null)
    val abgewiesen: StateFlow<Treffer?> = _abgewiesen.asStateFlow()

    /** Die Merkmale des letzten Treffers: Kommt danach nichts, lernt das [Stimmprofil] daraus einen Fehlalarm. */
    @Volatile var trefferMerkmale: FloatArray? = null
        internal set

    /** Wann der Dienst das Mikrofon zuletzt losgelassen hat (elapsedRealtime). */
    @Volatile private var freiSeit = 0L
    /** Nach „Hey Jarvis“: Vor der eigenen Aufnahme die Schutzpause fürs Audiosystem abwarten. */
    @Volatile private var nachRuf = false

    internal fun hoertZu(an: Boolean) {
        if (!an && _hoertZu.value) freiSeit = SystemClock.elapsedRealtime()
        _hoertZu.value = an
    }
    internal fun erkannt(sicherheit: Float) { _letzter.value = Treffer(System.currentTimeMillis(), sicherheit) }
    internal fun abgewiesen(sicherheit: Float) { _abgewiesen.value = Treffer(System.currentTimeMillis(), sicherheit) }
    internal fun rufen() { gerufenUm = System.currentTimeMillis(); nachRuf = true; _belegt.value = true }

    /** Entfernt die „Tippen“-Meldung des Wächters, falls eine steht; null = keine. */
    @Volatile private var rufMeldungEntfernen: (() -> Unit)? = null

    /** Der Dienst hat den Ruf als Meldung gezeigt; [entfernen] räumt sie wieder ab. */
    internal fun rufMeldungGezeigt(entfernen: () -> Unit) { rufMeldungEntfernen = entfernen }

    /**
     * Die App übernimmt nach „Hey Jarvis“ (das Mikrofon bleibt dabei für sie belegt). Eine inzwischen gezeigte
     * Ruf-Meldung ist dann überholt und verschwindet.
     */
    fun uebernommen() {
        gerufenUm = 0L
        rufMeldungEntfernen?.let { entfernen ->
            rufMeldungEntfernen = null
            entfernen()
        }
    }

    /** Meldet eine eigene Aufnahme an und wartet, bis der Dienst das Mikrofon wirklich losgelassen hat. */
    suspend fun mikrofonHolen() {
        _belegt.value = true
        withTimeoutOrNull(1_500) { _hoertZu.first { !it } }
        // Nach „Hey Jarvis“ gibt das Audiosystem das Mikrofon verzögert frei: den Rest der Pause abwarten. Sie zählt
        // ab dem Loslassen und überlappt so mit dem Start der App, statt ihn zu verzögern.
        if (nachRuf) {
            nachRuf = false
            val rest = SCHUTZPAUSE_MS - (SystemClock.elapsedRealtime() - freiSeit)
            if (rest > 0) delay(minOf(rest, SCHUTZPAUSE_MS))
        }
    }

    fun mikrofonFreigeben() {
        gerufenUm = 0L
        nachRuf = false
        _belegt.value = false
    }

    private const val SCHUTZPAUSE_MS = 300L
}
