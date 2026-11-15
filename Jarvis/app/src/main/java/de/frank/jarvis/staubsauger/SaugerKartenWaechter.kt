package de.frank.jarvis.staubsauger

import android.content.Context
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Liest nach jeder fertigen Reinigung die Wohnungskarte im Hintergrund ein und speichert sie (ProscenicMehr.kartenSpeichern),
 * damit die Staubsauger-Karte beim Aufklappen sofort die aktuelle Karte zeigt. Läuft im Jarvis-Dienst: alle 10 Minuten ein
 * kleiner Blick in den Reinigungsverlauf der Cloud; nur wenn dort eine Reinigung nach der gespeicherten Karte endete, wird die Karte geholt.
 */
object SaugerKartenWaechter {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var schleife: Job? = null

    @Synchronized
    fun starte(context: Context) {
        if (schleife?.isActive == true) return
        // Nur auf dem Handy: Handy und Tablet teilen das Proscenic-Konto, und jede Anmeldung macht die andere ungültig (Almanach proscenic-cloud Nr. 2).
        if (de.frank.jarvis.data.Geraet.istTablet) return
        val app = context.applicationContext
        schleife = bereich.launch {
            delay(60_000)
            while (true) {
                runCatching { pruefen(app) }.onFailure { Logbuch.netz("Staubsauger-Karte: Prüfung gescheitert", it.toString().take(200)) }
                delay(10 * 60_000L)
            }
        }
    }

    /** Holt die Karte, wenn eine Reinigung nach dem Stand der gespeicherten Karte endete. Gibt true zurück, wenn neu geholt wurde. */
    suspend fun pruefen(context: Context): Boolean {
        if (!ProscenicCloud.konfiguriert(context)) return false
        val ende = ProscenicMehr.letzteReinigungEnde(context) ?: return false
        if (ende <= 0L || ende <= ProscenicMehr.kartenStand(context)) return false
        val neu = ProscenicMehr.live(context, mitKarte = true)?.karte != null
        if (neu) Logbuch.system("Staubsauger-Karte nach der Reinigung gespeichert")
        return neu
    }
}
