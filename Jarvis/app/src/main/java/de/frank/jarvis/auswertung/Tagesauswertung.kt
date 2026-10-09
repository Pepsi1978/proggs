package de.frank.jarvis.auswertung

import android.content.Context
import android.util.Log
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.BiomarkerFaehigkeit
import de.frank.jarvis.faehigkeit.IdeenFaehigkeit
import de.frank.jarvis.faehigkeit.TagebuchFaehigkeit
import de.frank.jarvis.faehigkeit.WeckerFaehigkeit
import de.frank.jarvis.faehigkeit.WetterFaehigkeit
import de.frank.jarvis.faehigkeit.WissenFaehigkeit
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import org.json.JSONObject

/** Eine gespeicherte Tagesauswertung. [mitKi] = vom Modell geschrieben; sonst der reine Datenbericht. */
data class Auswertung(val zeit: Long, val anlass: String, val text: String, val daten: String, val mitKi: Boolean, val modell: String) {
    val zeitpunkt: LocalDateTime get() = Instant.ofEpochMilli(zeit).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

data class AuswertungsStand(val laeuft: Boolean = false, val schritt: String = "", val neueste: Auswertung? = null)

/**
 * Die Tagesauswertung: Jarvis trägt zu festen Uhrzeiten zusammen, was für ein Tag heute ist, wie die Biodaten
 * im Vergleich stehen und was die nächsten Tage bringen, und legt das Ergebnis fertig ab. ChatGPT holt es mit
 * einem einzigen Aufruf, statt alle Apps neu abzufragen.
 *
 * Zwei Schritte: Erst sammeln feste Regeln die Daten (Dienst, Schlafzeiten, Termine, Kennzahlen). Dann schreibt
 * das eigene Modell von Jarvis daraus die Auswertung. Ohne Modell bleibt der Datenbericht — er ist auch allein
 * brauchbar. Die Aufgaben gehören nicht in die gespeicherte Fassung: Sie ändern sich laufend und werden beim
 * Abruf frisch gelesen.
 */
object Tagesauswertung {
    private const val TAG = "JarvisAuswertung"
    private const val BEHALTEN = 100  // bei stündlicher Synchronisation gut vier Tage
    private val sperre = Mutex()
    private val _stand = MutableStateFlow(AuswertungsStand())
    val stand: StateFlow<AuswertungsStand> = _stand.asStateFlow()

    private fun ordner(context: Context) = File(context.filesDir, "tagesauswertung").apply { mkdirs() }

    fun lade(context: Context) {
        if (_stand.value.neueste == null) _stand.value = _stand.value.copy(neueste = neueste(context))
    }

    fun neueste(context: Context): Auswertung? =
        ordner(context).listFiles { f -> f.name.endsWith(".json") }?.maxByOrNull { it.name }?.let(::lies)

    private fun lies(datei: File): Auswertung? = runCatching {
        JSONObject(datei.readText()).let { Auswertung(it.getLong("zeit"), it.optString("anlass"), it.optString("text"), it.optString("daten"), it.optBoolean("ki"), it.optString("modell")) }
    }.getOrNull()

    private fun speichere(context: Context, a: Auswertung) {
        val name = a.zeitpunkt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")) + ".json"
        File(ordner(context), name).writeText(
            JSONObject().put("zeit", a.zeit).put("anlass", a.anlass).put("text", a.text).put("daten", a.daten).put("ki", a.mitKi).put("modell", a.modell).toString(),
        )
        ordner(context).listFiles { f -> f.name.endsWith(".json") }?.sortedByDescending { it.name }?.drop(BEHALTEN)?.forEach { it.delete() }
    }

    /**
     * Sofort-Fassung ohne Abgleich und ohne Modell, in wenigen Sekunden fertig. Für den Fall, dass beim Abruf
     * nur eine veraltete Auswertung vorliegt: lieber frische Daten ohne Deutung als eine alte Deutung.
     */
    suspend fun datenbericht(context: Context, anlass: String): Auswertung {
        val app = context.applicationContext
        val fertig = Auswertung(System.currentTimeMillis(), anlass, "Diese Fassung ist der reine Datenbericht, ohne die Deutung von Jarvis. Die vollständige Auswertung wird gerade im Hintergrund erstellt.", sammle(app, false), false, "")
        speichere(app, fertig)
        _stand.value = _stand.value.copy(neueste = fertig)
        return fertig
    }

    /** Ist die neueste Auswertung älter als der letzte Lauf, der laut Plan schon hätte stattfinden sollen? */
    fun veraltet(context: Context): Boolean {
        val neu = neueste(context)?.zeitpunkt ?: return true
        // Ohne Synchronisation zählt nur der Tag. Mit ihr zählt der letzte fällige Lauf: Über die Schlafpause hinweg
        // (auch über Mitternacht) bleibt die Fassung von davor die richtige.
        val faellig = Zeitplan.letzterFaelliger(context) ?: return neu.toLocalDate() != LocalDate.now()
        // Fünf Minuten Luft: Ein Lauf, der gerade erst gestartet ist, zählt noch nicht als verpasst.
        return neu.isBefore(faellig) && faellig.isBefore(LocalDateTime.now().minusMinutes(5))
    }

    /** Erstellt eine neue Auswertung. Läuft schon eine, kehrt der Aufruf sofort mit null zurück. */
    suspend fun erstelle(context: Context, anlass: String): Auswertung? {
        val app = context.applicationContext
        if (!sperre.tryLock()) return null
        try {
            fun schritt(text: String) { _stand.value = _stand.value.copy(laeuft = true, schritt = text) }
            val einstellungen = Einstellungen.get(app)
            schritt("Frische Biodaten holen")
            val biomarker = Register.alle(app).filterIsInstance<BiomarkerFaehigkeit>().firstOrNull()
            val abgeglichen = einstellungen.auswertungAbgleich && runCatching { biomarker?.abgleich() }.getOrNull() == true

            schritt("Wetter holen")
            runCatching { Register.alle(app).filterIsInstance<WetterFaehigkeit>().firstOrNull()?.synchronisiere() }

            schritt("Tagebuch holen")
            runCatching { Register.alle(app).filterIsInstance<TagebuchFaehigkeit>().firstOrNull()?.synchronisiere() }

            schritt("Wissens-Datenbank holen")
            runCatching { Register.alle(app).filterIsInstance<WissenFaehigkeit>().firstOrNull()?.synchronisiere() }

            schritt("Daten zusammentragen")
            val daten = sammle(app, abgeglichen)

            val deuten = einstellungen.auswertungDeutung
            if (deuten) schritt("Auswertung schreiben")
            val text = if (!deuten) null else runCatching {
                JarvisAgent(app).versuche(auftrag(daten), zeitlimitMs = 5 * 60_000L, maxSchritte = 3, beiSchritt = { schritt("Jarvis prüft nach: $it") })
            }.onFailure { Log.w(TAG, "Modell fehlgeschlagen", it) }.getOrNull()?.trim()?.takeIf { it.length > 200 }

            val fertig = Auswertung(
                zeit = System.currentTimeMillis(),
                anlass = anlass,
                text = text ?: if (deuten) "Diese Fassung ist der reine Datenbericht: Das Modell von Jarvis war nicht erreichbar oder nicht verbunden."
                else "Diese Fassung ist der reine Datenbericht: Die Deutung durch das Modell ist in Jarvis unter Einstellungen ausgeschaltet.",
                daten = daten,
                mitKi = text != null,
                modell = if (text != null) einstellungen.modell.label else "",
            )
            speichere(app, fertig)
            _stand.value = AuswertungsStand(laeuft = false, neueste = fertig)
            Protokoll.melde(Quelle.JARVIS, "Tagesauswertung", "$anlass: " + (if (fertig.mitKi) "erstellt mit ${fertig.modell}" else if (deuten) "nur Datenbericht, Modell nicht erreichbar" else "nur Datenbericht, Deutung ausgeschaltet"), fertig.mitKi || !deuten)
            return fertig
        } catch (e: Exception) {
            Log.e(TAG, "Auswertung fehlgeschlagen", e)
            Protokoll.melde(Quelle.JARVIS, "Tagesauswertung", "Fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}", ok = false)
            _stand.value = _stand.value.copy(laeuft = false, schritt = "")
            return null
        } finally {
            sperre.unlock()
        }
    }

    /** Schritt 1: alle Daten nach festen Regeln, als ein Text mit klaren Abschnitten. */
    private suspend fun sammle(app: Context, abgeglichen: Boolean): String {
        val alle = Register.alle(app)
        val kalender = alle.filterIsInstance<KalenderFaehigkeit>().firstOrNull()
        val biomarker = alle.filterIsInstance<BiomarkerFaehigkeit>().firstOrNull()
        val werkzeuge = alle.flatMap { it.werkzeuge }.associateBy { it.name }
        suspend fun rufe(name: String, argumente: JSONObject = JSONObject()): String =
            runCatching { werkzeuge[name]?.ausfuehren(argumente)?.let { (if (it.fehler) "NICHT VERFÜGBAR: " else "") + it.text } }.getOrNull() ?: "NICHT VERFÜGBAR"

        val heute = LocalDate.now()
        val jetzt = LocalDateTime.now()
        return buildString {
            append("STAND: ").append(jetzt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))).append('\n')
            append("Biodaten vor der Auswertung frisch abgeglichen: ").append(if (abgeglichen) "ja" else "nein (Stand des letzten Abgleichs in Entropie Reductor)").append("\n\n")

            append("== RAHMEN DER NÄCHSTEN TAGE (fest gerechnet, verbindlich) ==\n")
            append(kalender?.rahmen(6)?.ifEmpty { null } ?: "NICHT VERFÜGBAR: Kalender nicht lesbar").append("\n\n")

            append("== TERMINE HEUTE UND DIE NÄCHSTEN 4 TAGE ==\n")
            append(rufe("kalender_lesen", JSONObject().put("von", heute.toString()).put("bis", heute.plusDays(4).toString()))).append("\n\n")

            append("== WETTER HEUTE, MORGEN, ÜBERMORGEN ==\n")
            append(runCatching { alle.filterIsInstance<WetterFaehigkeit>().firstOrNull()?.vorschau(3) }.getOrNull() ?: "NICHT VERFÜGBAR").append("\n\n")

            append("== GESTELLTE WECKER ==\n")
            append(runCatching { alle.filterIsInstance<WeckerFaehigkeit>().firstOrNull()?.ueberblick() }.getOrNull() ?: "NICHT VERFÜGBAR").append("\n\n")

            append("== BIODATEN: WERTE DES AKTUELLEN TAGES ==\n")
            append(rufe("biomarker_tag", JSONObject().put("datum", heute.toString()))).append("\n\n")

            append("== BIODATEN: AKTUELLER STAND GEGEN DEN LETZTEN MONAT, 7 TAGE UND ALLE BISHERIGEN TAGE ==\n")
            append(rufe("biomarker_auswertung", JSONObject().put("tage", 30).put("metriken", JSONArray(biomarker?.alleMetriken ?: emptyList<String>())))).append("\n\n")

            append("== BIODATEN: VERLAUF DER LETZTEN 7 TAGE (Schlaf und Erholung, zum Abgleich mit den Diensten) ==\n")
            append(rufe("biomarker_verlauf", JSONObject().put("von", heute.minusDays(7).toString()).put("aufloesung", "tag").put("metriken", JSONArray(listOf("schlafdauer", "wach_min", "erholung", "hrv", "ruhepuls"))))).append("\n\n")

            append("== DIENSTE UND TERMINE DER LETZTEN 4 TAGE ==\n")
            append(rufe("kalender_lesen", JSONObject().put("von", heute.minusDays(4).toString()).put("bis", heute.minusDays(1).toString()))).append("\n\n")

            append("== TRAININGS DER LETZTEN 14 TAGE ==\n")
            append(rufe("trainings_lesen", JSONObject().put("von", heute.minusDays(14).toString()).put("limit", 20))).append("\n\n")

            append("== TAGEBUCH DER LETZTEN 7 TAGE ==\n")
            append(runCatching { alle.filterIsInstance<TagebuchFaehigkeit>().firstOrNull()?.rueckblick(7) }.getOrNull() ?: "NICHT VERFÜGBAR").append("\n\n")

            append("== GENIALE IDEEN (offen, gekürzt) ==\n")
            append(runCatching { alle.filterIsInstance<IdeenFaehigkeit>().firstOrNull()?.ueberblick() }.getOrNull() ?: "NICHT VERFÜGBAR").append("\n\n")

            append("== WISSENS-DATENBANK (Inhaltsverzeichnis; Volltexte über wissen_lesen) ==\n")
            append(runCatching { alle.filterIsInstance<WissenFaehigkeit>().firstOrNull()?.ueberblick() }.getOrNull() ?: "NICHT VERFÜGBAR").append("\n\n")

            append("== OFFENE AUFGABEN (nur Stand jetzt, ändert sich laufend) ==\n")
            append(rufe("aufgaben_lesen", JSONObject().put("bereich", "heute"))).append('\n')
            append(rufe("aufgaben_lesen", JSONObject().put("bereich", "morgen")))
        }
    }

    /** Schritt 2: der Auftrag an das Modell. */
    private fun auftrag(daten: String): String = """
Schreibe Franks Tagesauswertung. Sie wird gespeichert und ihm später vorgelesen: ganze, gut sprechbare Sätze, du-Anrede, keine Tabellen, kein Markdown.

Dein Ziel: Frank weiß danach, wie es ihm geht, was für ein Tag heute ist und was jetzt sinnvoll ist. Schreibe ausführlich und verbinde die Daten miteinander, statt sie nacheinander aufzuzählen.

So gehst du vor:
1. Lies zuerst den RAHMEN. Er legt verbindlich fest, ob ein Tag Arbeitstag oder frei ist, welcher Dienst ansteht, wann Frank schläft und wann er Zeit hat. Rechne das nicht neu.
2. Deute die Biodaten immer vor diesem Hintergrund. Nach einem Nachtdienst schläft Frank am Tag, etwa von 6 bis 15 Uhr. Tagschlaf ist oft kürzer und unruhiger als Nachtschlaf. Sind dann Schlafdauer, Tiefschlaf oder REM niedriger oder die Wachzeit höher als sonst, nenne den Tagschlaf als wahrscheinlichen Grund und rate zu Erholung und Entspannung statt zu Belastung. Ein Schlafwert gehört zu dem Tag, an dem der Schlaf endet. Fehlt der Schlafwert von heute, weil Frank laut Rahmen noch schläft oder gerade aufgestanden ist, ist das normal.
3. Verbinde alles miteinander: Erholung mit Dienst und Training, Wetter mit den freien Zeitfenstern, Aufgaben und Ideen mit der Zeit und der Kraft, die Frank heute wirklich hat.

Feste Regeln:
- Verwende nur Zahlen, die unten stehen, und schätze nichts. Steht bei etwas NICHT VERFÜGBAR, sage das in einem Satz und deute es nicht.
- Nenne die Zahlen, die etwas aussagen, gerundet und mit Einordnung (zum Beispiel „HRV 49, über deinem Monatsschnitt“). Sage Wochentage statt Kalenderdaten.
- Tagesbelastung, Energieumsatz und Schritte des laufenden Tages sind Zwischenstände.
- Stehen in deiner Anweisung FRANKS REGELN, halte sie ein. Sie gehen diesen Vorgaben vor. Steht dort WAS JARVIS ÜBER FRANK WEISS, beziehe es in Deutung und Empfehlungen ein (zum Beispiel was ihm wichtig ist oder was er bei Regen nicht mag). Stehen dort Ziele, stelle sie den aktuellen Werten gegenüber und nenne, was heute darauf einzahlt.
- Keine medizinischen Diagnosen.
- Meist reichen die Daten unten. Du darfst höchstens zwei Werkzeuge zusätzlich aufrufen, wenn ein auffälliger Wert einen Blick in den Verlauf braucht.

Aufbau: Beginne mit einem Satz Fazit. Danach folgen genau diese drei Teile, jeder mit seiner Überschrift in Großbuchstaben auf eigener Zeile.

RÜCKBLICK: Die letzten Tage. Welche Dienste und freien Tage hinter Frank liegen, wie sich Schlaf und Erholung dazu entwickelt haben (Tagschlaf nach Nachtdiensten gesondert betrachten), welche Trainings es gab und was er laut Tagebuch gemacht und erlebt hat.

AKTUELL: Der heutige Tag. Was für ein Tag es ist (Dienst oder frei, Abfahrt, Schlaf- und freie Zeitfenster) und welche Termine anstehen. Wie es Frank heute geht: der letzte Schlaf mit Dauer, Tiefschlaf, REM und Wachzeit, dazu Erholung, HRV und Ruhepuls, eingeordnet gegen 7 Tage, den letzten Monat und alle bisherigen Tage und bezogen auf die Art des Tages. Körperwerte nur, wenn sie neu oder auffällig sind. Das Wetter jetzt und über den Tag. Welche Aufgaben heute anliegen oder überfällig sind und welche in die freien Zeitfenster passen. Dann eine klare Einschätzung, wie belastbar Frank heute ist, und zwei bis vier konkrete Empfehlungen, was als Nächstes sinnvoll ist. Passt eine der genialen Ideen zu Zeit und Kraft von heute, nenne sie; zähle die Ideen nicht auf.

AUSBLICK: Die nächsten Tage. Kommende Dienste und freie Tage, Termine, die Aufgaben von morgen, das Wetter für morgen und übermorgen und das beste Zeitfenster für einen Lauf oder ein Vorhaben draußen. Woran Frank rechtzeitig denken sollte (zum Beispiel Vorschlafen vor einem Nachtdienst-Block, früh schlafen vor einem Tagdienst, ein Wecker, der zum Dienst fehlt, Abholung der Tonnen). Und welche Idee oder größere Aufgabe sich für den nächsten freien Tag anbietet.

DATEN:
$daten
""".trim()

    /** Die Bereiche der Tagesdatenbank: Kurzname → Anfang der Abschnittsüberschrift. */
    val BEREICHE = linkedMapOf(
        "rahmen" to "RAHMEN", "termine" to "TERMINE", "wetter" to "WETTER", "wecker" to "GESTELLTE WECKER", "biodaten_heute" to "BIODATEN: WERTE", "biodaten_vergleich" to "BIODATEN: AKTUELLER",
        "biodaten_verlauf" to "BIODATEN: VERLAUF", "rueckblick_termine" to "DIENSTE UND TERMINE DER LETZTEN", "trainings" to "TRAININGS", "tagebuch" to "TAGEBUCH", "ideen" to "GENIALE IDEEN", "wissen" to "WISSENS-DATENBANK",
    )

    /** Ein oder alle Bereiche der zuletzt gespeicherten Tagesdaten. Aufgaben fehlen bewusst: Sie werden immer frisch gelesen. */
    fun tagesdaten(a: Auswertung, bereich: String): String {
        val abschnitte = a.daten.substringBefore("== OFFENE AUFGABEN").split(Regex("(?m)^== ")).drop(1).map { "== $it".trim() }
        val gesucht = BEREICHE[bereich.trim().lowercase(Locale.GERMAN)]
        val teile = if (gesucht == null) abschnitte else abschnitte.filter { it.startsWith("== $gesucht") }
        val minuten = java.time.Duration.between(a.zeitpunkt, LocalDateTime.now()).toMinutes()
        return "TAGESDATENBANK, Stand " + a.zeitpunkt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM, HH:mm 'Uhr'", Locale.GERMAN)) +
            " (vor " + (if (minuten < 90) "$minuten Minuten" else "${minuten / 60} Stunden") + ").\n\n" + teile.joinToString("\n\n").ifEmpty { "Dieser Bereich ist nicht vorhanden." }
    }

    /** Der Text, den ein Abruf bekommt: Auswertung, Datenanhang und Alter. Aufgaben hängt der Aufrufer frisch an. */
    fun alsText(a: Auswertung, mitDaten: Boolean): String = buildString {
        val jetzt = LocalDateTime.now()
        val minuten = java.time.Duration.between(a.zeitpunkt, jetzt).toMinutes()
        append("TAGESAUSWERTUNG, erstellt ").append(a.zeitpunkt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN)))
        append(" (").append(if (minuten < 90) "vor $minuten Minuten" else "vor ${minuten / 60} Stunden").append(", ").append(a.anlass).append(")")
        if (a.mitKi) append(", geschrieben von Jarvis mit ").append(a.modell)
        append(".\n")
        if (a.zeitpunkt.toLocalDate() != jetzt.toLocalDate()) append("ACHTUNG: Diese Auswertung ist nicht von heute. Für heute liegt noch keine vor.\n")
        append('\n').append(a.text).append('\n')
        if (mitDaten || !a.mitKi) append("\n--- DATENANHANG (Stand der Erstellung, zum Nachschlagen einzelner Werte) ---\n").append(a.daten).append('\n')
    }
}
