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
 * Dazu ein stiller Statusblick (um Zeitpläne und in Ladepausen alle 2, sonst alle 10 Minuten): Arbeitet der Roboter, startet die
 * Hintergrund-Aufzeichnung der Live-Karte ([SaugerLive]).
 */
object SaugerKartenWaechter {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var schleife: Job? = null

    @Synchronized
    fun stoppe() { schleife?.cancel(); schleife = null }

    @Synchronized
    fun starte(context: Context) {
        if (schleife?.isActive == true) return
        // Nur auf dem Handy: Handy und Tablet teilen das Proscenic-Konto, und jede Anmeldung macht die andere ungültig (Almanach proscenic-cloud Nr. 2).
        if (de.frank.jarvis.data.Geraet.istTablet) return
        val app = context.applicationContext
        schleife = bereich.launch {
            delay(60_000)
            var runde = 0
            var plaene = emptyList<ProscenicMehr.Zeitplan>(); var plaeneUm = 0L
            while (true) {
                val zehn = runde++ % 5 == 0
                // Zeitpläne aus der gespeicherten Karte, höchstens alle 30 Minuten neu gelesen (die Datei trägt das Kartenbild).
                if (System.currentTimeMillis() - plaeneUm > 30 * 60_000L) {
                    plaeneUm = System.currentTimeMillis()
                    runCatching { ProscenicMehr.ausSpeicher(app)?.zeitplaene }.getOrNull()?.let { plaene = it }
                }
                // Still nachsehen, ob der Roboter arbeitet (Zeitplan, Proscenic-App); dann startet die Aufzeichnung der Live-Karte im
                // Hintergrund (ProscenicMehr.zustandStill, ohne Neu-Anmeldung). Jede Abfrage belegt kurz das Gateway (Almanach Nr. 3), darum
                // nur um einen Zeitplan und in einer Ladepause alle 2 Minuten, sonst alle 10.
                val dicht = SaugerLive.wartetAufFortsetzung(app) || ProscenicMehr.zeitplanNahe(plaene)
                var arbeitet = false
                if (!SaugerLive.aktiv && (dicht || zehn))
                    arbeitet = ProscenicMehr.aktiv(runCatching { ProscenicMehr.zustandStill(app) }.getOrNull()?.modus)
                // Alle 10 Minuten die Karte nach einer fertigen Reinigung (nicht, während er gerade wieder arbeitet).
                if (zehn && !arbeitet) runCatching { pruefen(app) }.onFailure { Logbuch.netz("Staubsauger-Karte: Prüfung gescheitert", it.toString().take(200)) }
                delay(2 * 60_000L)
            }
        }
    }

    /** Holt die Karte, wenn eine Reinigung nach dem Stand der gespeicherten Karte endete. Gibt true zurück, wenn neu geholt wurde. */
    suspend fun pruefen(context: Context): Boolean {
        if (!ProscenicCloud.konfiguriert(context)) return false
        // Während die Live-Karte offen ist, hält sie selbst die Gateway-Verbindung; eine zweite würde sie verdrängen.
        if (SaugerLive.aktiv) return false
        val ende = ProscenicMehr.letzteReinigungEnde(context) ?: return false
        if (ende <= 0L || ende <= ProscenicMehr.kartenStand(context)) return false
        // Arbeitet er schon wieder (nächste Reinigung), wartet die Karte bis zu deren Ende.
        if (ProscenicMehr.aktiv(ProscenicMehr.live(context, mitKarte = false)?.modus)) return false
        val neu = ProscenicMehr.live(context, mitKarte = true)?.karte != null
        if (neu) Logbuch.system("Staubsauger-Karte nach der Reinigung gespeichert")
        return neu
    }
}
