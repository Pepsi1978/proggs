package de.frank.jarvis.weckwort

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

    /** Ab diesem Wert gilt „Hey Jarvis“ als erkannt (aus den Einstellungen, gilt sofort beim nächsten Block). */
    @Volatile var schwelle = 0.5f

    /** Der Dienst hat die App nach „Hey Jarvis“ gerufen, die App hat noch nicht übernommen. */
    @Volatile var gerufenUm = 0L
        internal set

    internal fun hoertZu(an: Boolean) { _hoertZu.value = an }
    internal fun erkannt(sicherheit: Float) { _letzter.value = Treffer(System.currentTimeMillis(), sicherheit) }
    internal fun rufen() { gerufenUm = System.currentTimeMillis(); _belegt.value = true }

    /** Die App übernimmt nach „Hey Jarvis“ (das Mikrofon bleibt dabei für sie belegt). */
    fun uebernommen() { gerufenUm = 0L }

    /** Meldet eine eigene Aufnahme an und wartet, bis der Dienst das Mikrofon wirklich losgelassen hat. */
    suspend fun mikrofonHolen() {
        _belegt.value = true
        withTimeoutOrNull(1_500) { _hoertZu.first { !it } }
    }

    fun mikrofonFreigeben() {
        gerufenUm = 0L
        _belegt.value = false
    }
}
