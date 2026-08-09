package de.frank.experimente.auth

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import okhttp3.Call

/**
 * Hält alle laufenden Codex-Aufrufe fest, damit sie gemeinsam abgebrochen werden können.
 *
 * Jeder Aufruf steht für sich: Die App fragt mehrere Dinge gleichzeitig an (Nachschub für die
 * laufende Sitzung, Verlaufstitel, verbesserte Fassung). Würde ein neuer Aufruf den laufenden
 * abbrechen, bliebe die Sitzung ohne neue Fragen stehen.
 */
internal class ActiveCallTracker {
    private val activeCalls: MutableSet<Call> = Collections.newSetFromMap(ConcurrentHashMap())

    fun track(call: Call) {
        activeCalls.add(call)
    }

    fun clear(call: Call) {
        activeCalls.remove(call)
    }

    fun cancel() {
        val running = activeCalls.toList()
        activeCalls.clear()
        running.forEach(Call::cancel)
    }
}
