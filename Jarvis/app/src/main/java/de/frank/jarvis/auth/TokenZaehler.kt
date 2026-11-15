package de.frank.jarvis.auth

import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Zählt die Tokens aller Modellanfragen einer Runde (Eingabe samt Zwischenspeicher und Ausgabe samt Denken). Läuft im
 * Koroutinen-Kontext der Runde mit; jede Anfrage an OpenAI oder Anthropic darin trägt ihre `usage` hier ein.
 * Dazu, was davon auf das Live-Gedächtnis entfiel (geschätzt nach seinem Anteil an der Eingabe) und aus welchen Bereichen.
 */
class TokenZaehler : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<TokenZaehler>

    private val summe = AtomicLong()
    private val ausgabeSumme = AtomicLong()
    private val gedaechtnisSumme = AtomicLong()
    private val genutzteBereiche = java.util.concurrent.ConcurrentLinkedQueue<String>()
    val tokens: Long get() = summe.get()
    /** Eingabe aller Anfragen (Anweisung, Gedächtnis, Verlauf, Werkzeugergebnisse). */
    val eingabe: Long get() = summe.get() - ausgabeSumme.get()
    /** Was das Modell selbst erzeugt hat: Denken und Antwort. */
    val ausgabe: Long get() = ausgabeSumme.get()
    /** Geschätzter Anteil des Live-Gedächtnisses an der Eingabe. */
    val gedaechtnis: Long get() = gedaechtnisSumme.get()
    /** Die Titel der Gedächtnis-Bereiche dieser Runde, in der Reihenfolge ihres ersten Auftretens. */
    val bereiche: List<String> get() = genutzteBereiche.toList().distinct()

    fun dazu(anzahl: Long, ausgabe: Long = 0L) {
        if (anzahl > 0) summe.addAndGet(anzahl)
        if (ausgabe > 0) ausgabeSumme.addAndGet(ausgabe)
    }

    fun gedaechtnisDazu(anzahl: Long, bereiche: Collection<String>) {
        if (anzahl > 0) gedaechtnisSumme.addAndGet(anzahl)
        bereiche.forEach { if (it !in genutzteBereiche) genutzteBereiche += it }
    }
}
