package de.frank.jarvis.speech

import android.content.Context
import de.frank.jarvis.observability.Logbuch
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** Der Sprachweg, in dem Frank eine Anfrage begonnen hat. Die [kennung] steht dauerhaft im Vorgangsbuch. */
enum class SprachModus(val kennung: String, val anzeige: String) {
    WECKWORT("weckwort", "Hey Jarvis"),
    LIVE("live", "Live-Gespräch");

    companion object {
        fun aus(kennung: String?): SprachModus? = entries.firstOrNull { it.kennung == kennung }
    }
}

/**
 * Läuft im Koroutinen-Kontext einer Runde mit, die Frank gesprochen begonnen hat (Gesprächsmodus mit eingeschaltetem Vorlesen
 * oder Live-Gespräch). Startet Jarvis darin einen Agenten, erbt der Vorgang den Sprachursprung von selbst; das hängt nicht am
 * Modellparameter `vorlesen` von agent_starten. [seit] = Beginn der Anfrage, für den Abgleich mit einem bewussten Ende.
 */
class SprachUrsprung(val modus: SprachModus, val gespraech: String, val seit: Long = System.currentTimeMillis()) :
    AbstractCoroutineContextElement(SprachUrsprung) {
    companion object Key : CoroutineContext.Key<SprachUrsprung>
}

/**
 * Eine Antwort, die Frank im Sprachgespräch angestoßen hat und die erst nach dessen Ende (Stille, App-Wechsel, Bildschirm aus)
 * oder neben einem laufenden Gespräch ankommt. Sie steht schon im Chat ([gespraech]); hier geht es nur noch ums Hören.
 * [schluessel] ist je Ergebnis eindeutig (Vorgang, Live-Aufruf, Runde), damit nichts doppelt vorgelesen wird.
 * [ansage] kommt zuerst („Frank, die Ergebnisse sind jetzt da.“), dann [text] vollständig.
 */
data class Zustellung(
    val schluessel: String,
    val modus: SprachModus,
    val gespraech: String,
    val begonnen: Long,
    val ansage: String,
    val text: String,
)

/**
 * Buchführung der Sprachzustellung: bewusstes Ende, doppelte Zustellung, Entscheidung, ob gesprochen werden darf.
 * Gesprochen und wieder aufgenommen wird im [de.frank.jarvis.ui.AppViewModel] (dort laufen Vorleser, Gesprächsmodus und Live).
 *
 * Ein Stille-Ende (Gesprächsmodus hört nichts mehr, Live endet nach der eingestellten Stille) verwirft nichts. Ein bewusstes
 * Ende durch Frank („Sprachsession beenden“, Sprachblase lang drücken, Live-Knopf, „Gespräch beenden“) gilt dagegen für alle
 * Anfragen, die davor begonnen haben: Deren Ergebnis steht dann nur im Chat (mit der üblichen Benachrichtigung).
 */
object SprachZustellung {
    private const val PREFS = "jarvis_sprachzustellung"
    private const val BEENDET_UM = "beendet_um"
    private const val GEMERKT = 200

    /** Warum eine Zustellung (nicht) gesprochen wird. */
    enum class Ausgang(val text: String) {
        SPRECHEN("wird vorgelesen"),
        BEWUSST_BEENDET("nicht vorgelesen: Frank hat das Sprachgespräch danach bewusst beendet"),
        VORLESEN_AUS("nicht vorgelesen: Antworten vorlesen ist ausgeschaltet"),
        RUHEZEIT("nicht vorgelesen: Ruhezeit"),
    }

    /** Reine Entscheidung (ohne Android, prüfbar): Ein bewusstes Ende nach dem Beginn der Anfrage geht allem vor. */
    fun entscheide(begonnen: Long, beendetUm: Long, vorlesenAn: Boolean, ruhe: Boolean): Ausgang = when {
        beendetUm >= begonnen -> Ausgang.BEWUSST_BEENDET
        !vorlesenAn -> Ausgang.VORLESEN_AUS
        ruhe -> Ausgang.RUHEZEIT
        else -> Ausgang.SPRECHEN
    }

    /**
     * Die Entscheidung mit den Einstellungen von jetzt: Gesprächsmodus nur mit „Antworten vorlesen“ (Live spricht immer).
     * Die Ruhezeit (Meldung „Agent fertig“) gilt nur für Agentenergebnisse ([agent]) außerhalb eines laufenden Live-Gesprächs:
     * Eine Antwort, die Frank im Gespräch erwartet, ist keine unaufgeforderte Meldung.
     */
    fun ausgang(context: Context, modus: SprachModus, begonnen: Long, agent: Boolean): Ausgang {
        val app = context.applicationContext
        return entscheide(begonnen, beendetUm(app),
            vorlesenAn = modus == SprachModus.LIVE || de.frank.jarvis.data.Einstellungen.get(app).antwortenVorlesen,
            ruhe = agent && !de.frank.jarvis.live.LiveGespraech.aktiv && de.frank.jarvis.meldung.Meldungen.inRuhe(app, de.frank.jarvis.meldung.Meldungsarten.AGENT))
    }

    /** Frank hat das Sprachgespräch bewusst beendet: Was bis jetzt begonnen hat, wird später nicht mehr vorgelesen. */
    fun bewusstBeendet(context: Context, anlass: String) {
        val jetzt = System.currentTimeMillis()
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(BEENDET_UM, jetzt).apply()
        Logbuch.sprache("Sprachzustellung: bewusst beendet", anlass)
    }

    fun beendetUm(context: Context): Long =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(BEENDET_UM, 0L)

    private val zugestellt = LinkedHashSet<String>()

    /** true genau beim ersten Mal je [schluessel]: Ein doppeltes Fertig-Ereignis liest nichts zweimal vor. */
    @Synchronized
    fun einmal(schluessel: String): Boolean {
        if (!zugestellt.add(schluessel)) return false
        while (zugestellt.size > GEMERKT) zugestellt.remove(zugestellt.first())
        return true
    }
}
