package de.frank.jarvis.auth

import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Zählt die Tokens aller Modellanfragen einer Runde (Eingabe samt Zwischenspeicher und Ausgabe samt Denken). Läuft im
 * Koroutinen-Kontext der Runde mit; jede Anfrage an OpenAI oder Anthropic darin trägt ihre `usage` hier ein.
 */
class TokenZaehler : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<TokenZaehler>

    private val summe = AtomicLong()
    val tokens: Long get() = summe.get()

    fun dazu(anzahl: Long) { if (anzahl > 0) summe.addAndGet(anzahl) }
}
