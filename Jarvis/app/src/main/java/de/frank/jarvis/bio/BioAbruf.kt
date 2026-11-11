package de.frank.jarvis.bio

import android.content.Context
import de.frank.jarvis.bio.domain.usecase.ApiSyncBericht
import de.frank.jarvis.bio.domain.usecase.WhoopAbrufPlan
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

/** Was die Oberfläche über den Abruf wissen muss. */
data class BioAbrufStand(val laeuft: Boolean = false, val anlass: String = "", val bericht: ApiSyncBericht? = null)

/**
 * Die EINZIGEN Auslöser für frische Biomarker (Franks Vorgabe vom 11.11.2026). Es gibt keinen Hintergrund-Abgleich, keinen
 * Abruf durch bloßes Lesen, durch das Live-Gedächtnis oder durch den Wächter:
 *  1. (entfallen am 11.11.2026: beim Öffnen des Bereichs wird nichts mehr abgerufen);
 *  2. [manuell]: auf ausdrücklichen Wunsch (Knopf im Bereich, „Aktualisiere meine Biodaten“ per Sprache oder Text);
 *  3. [vorTagesauswertung]: unmittelbar vor jeder Tagesauswertung, mit begrenzter Wartezeit.
 * Alle drei laufen durch [BioModul.sync]; wer dazukommt, wartet auf den laufenden Abgleich statt einen zweiten zu starten.
 *
 * Auf dem Tablet holt kein Auslöser selbst bei Whoop, Oura, Zepp oder Health Connect: Der Whoop-Zugang verträgt nur einen
 * Abrufer (rotierender Refresh-Token), Zepp und Health Connect gibt es nur auf dem Handy. Dort holen die Auslöser den
 * Spiegel des Handys ([BioSpiegel]) und nennen dessen Stand ehrlich.
 */
object BioAbruf {
    private val _stand = MutableStateFlow(BioAbrufStand())
    val stand: StateFlow<BioAbrufStand> = _stand.asStateFlow()

    /** Ein Abruf darf erst laufen, wenn die Daten in Jarvis liegen (sonst bliebe der Whoop-Zugang in Entropie Reductor). */
    fun bereit(context: Context): Boolean = Geraet.istTablet || Einstellungen.get(context).bioUebernommenMs > 0L

    /** Auslöser 2: ausdrücklicher Wunsch (Knopf, Sprache). Whoop wird gefragt, sofern der letzte Versuch nicht ganz frisch ist. */
    fun manuell(context: Context, wer: String): Deferred<ApiSyncBericht?> =
        starte(context.applicationContext, wer, WhoopAbrufPlan.Anlass.AUSDRUECKLICH)

    private fun starte(app: Context, anlass: String, art: WhoopAbrufPlan.Anlass): Deferred<ApiSyncBericht?> = BioModul.bereich.async {
        BioModul.einrichten(app)
        if (!bereit(app)) return@async null
        _stand.update { it.copy(laeuft = true, anlass = anlass) }
        try {
            val bericht = if (Geraet.istTablet) { BioSpiegel.holen(app); null } else BioModul.sync.syncApisNow(anlass, art)
            _stand.update { BioAbrufStand(false, anlass, bericht ?: it.bericht) }
            if (bericht != null) Logbuch.system("Biomarker abgerufen", "$anlass: ${bericht.kurzfassung()}")
            // Das Live-Gedächtnis liest die neuen Werte (nur lesen, kein weiterer Abruf). Auf dem Tablet macht das der Spiegel selbst, wenn etwas Neues kam.
            if (bericht != null) neuGelesen(app)
            // Neuer Stand: auch fürs Tablet spiegeln (nur Handy, nur wenn sich die Datenbank wirklich geändert hat).
            if (bericht != null) BioSpiegel.geaendert(app, anlass)
            bericht
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logbuch.fehler("Biomarker-Abruf", e, anlass)
            _stand.update { it.copy(laeuft = false) }
            null
        }
    }

    /**
     * Auslöser 3: unmittelbar vor einer Tagesauswertung. Holt alle Quellen und wartet höchstens die eingestellte Zeit. Ist der
     * neueste Whoop-Schlaf danach noch nicht fertig berechnet, wird innerhalb dieser Zeit noch einmal nachgefragt. Eine
     * hängende oder ausgefallene Quelle hält die Auswertung nie länger auf; ihr Zustand steht danach in jeder Antwort.
     * Gibt eine Zeile für den Datenbericht zurück (Abrufzeit, was neu ist, was fehlte).
     */
    suspend fun vorTagesauswertung(context: Context): String {
        val app = context.applicationContext
        val e = Einstellungen.get(app)
        if (!e.bioAbrufVorAuswertung) return "Biodaten: Abruf vor der Auswertung ist in den Einstellungen ausgeschaltet, es gilt der gespeicherte Stand."
        if (!bereit(app)) return "Biodaten: noch aus Entropie Reductor gelesen (die Übernahme nach Jarvis ist nicht abgeschlossen)."
        val beginn = System.currentTimeMillis()
        val frist = beginn + e.bioWartezeit * 1000L
        val erster = manuell(app, "vor der Tagesauswertung")
        var bericht = withTimeoutOrNull(frist - System.currentTimeMillis()) { erster.await() }
        if (bericht == null && !erster.isCompleted) {
            Logbuch.system("Tagesauswertung: Biomarker-Abruf dauert zu lange", "beginnt nach ${e.bioWartezeit} s mit dem bisherigen Stand")
            return "Biodaten: Der Abruf war nach ${e.bioWartezeit} Sekunden nicht fertig; die Werte unten sind der zuvor gespeicherte Stand (Datum je Wert beachten)."
        }
        if (Geraet.istTablet) return "Biodaten: Stand vom Handy (" + BioSpiegel.standText(app) + ")."
        if (bericht == null) {
            return "Biodaten: Der Abruf vor der Auswertung ist gescheitert (siehe Logbuch); die Werte unten sind der zuvor gespeicherte Stand (Datum je Wert beachten)."
        }
        // Frisch berechnete Werte abwarten: Whoop rechnet Schlaf und Erholung oft noch Minuten nach dem Aufwachen.
        // Die Nachfrage geht nur an Whoop und kurz nach dem ersten Abruf: Der Zugangs-Token ist dann noch gültig, es wird also
        // kein Refresh-Token getauscht (der Grund für die Mindestabstände der Whoop-Steuerung).
        var nachgefragt = 0
        if (e.bioSchlafAbwarten && bericht != null && bericht.quellen.any { it.id == "whoop" && it.gefragt && it.ok }) {
            while (nachgefragt < 2 && !schlafFertig() && System.currentTimeMillis() + NACHFRAGE_ABSTAND_MS + 20_000L < frist) {
                nachgefragt++
                delay(NACHFRAGE_ABSTAND_MS)
                // syncLastDays liefert ein Result und wirft nicht; null heißt hier allein: Wartezeit vorbei.
                withTimeoutOrNull(frist - System.currentTimeMillis()) { BioModul.whoop.syncLastDays(3) } ?: break
            }
            if (nachgefragt > 0) BioModul.abgleich.requestSync("Whoop: Schlaf nachgefragt")
        }
        val dauer = (System.currentTimeMillis() - beginn) / 1000
        return "Biodaten: vor dieser Auswertung abgerufen (${dauer} s): " + (bericht?.kurzfassung() ?: "kein Ergebnis") +
            (if (nachgefragt > 0) ". Whoop wurde $nachgefragt× nachgefragt, weil der Schlaf noch berechnet wurde" else "") +
            (if (e.bioSchlafAbwarten && !schlafFertig()) ". Der neueste Whoop-Schlaf ist noch nicht fertig berechnet; Erholung und Schlafwerte können sich noch ändern." else "")
    }

    /** Ist der neueste Whoop-Schlaf fertig berechnet (oder Whoop nicht verbunden)? */
    private suspend fun schlafFertig(): Boolean = runCatching {
        if (!BioModul.oauth.loadWhoopAuthState().isAuthorized) return true
        val s = BioModul.einstellungen.whoopAbrufStandFlow.first()
        s.schlafEndeMs == 0L || s.schlafVollstaendig || System.currentTimeMillis() - s.schlafEndeMs > 6 * 3_600_000L
    }.getOrDefault(true)

    /** Live-Gedächtnis auf die gespeicherten Werte auffrischen (liest nur). */
    fun neuGelesen(app: Context) {
        runCatching { de.frank.jarvis.gedaechtnis.Lagebild.anstossen(app, listOf(de.frank.jarvis.gedaechtnis.Bereich.BIODATEN, de.frank.jarvis.gedaechtnis.Bereich.TRAININGS), "neue Biodaten") }
    }

    private const val NACHFRAGE_ABSTAND_MS = 45_000L
}
