package de.frank.jarvis.auswertung

import android.content.Context
import android.util.Log
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.BiomarkerFaehigkeit
import de.frank.jarvis.faehigkeit.IdeenFaehigkeit
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
    private const val BEHALTEN = 30
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
        if (neu.toLocalDate() != LocalDate.now()) return true
        val faellig = Zeitplan.letzterFaelliger(context) ?: return false
        // Fünf Minuten Luft: Ein Lauf, der gerade erst gestartet ist, zählt noch nicht als verpasst.
        return neu.isBefore(faellig) && faellig.isBefore(LocalDateTime.now().minusMinutes(5))
    }

    /** Erstellt eine neue Auswertung. Läuft schon eine, kehrt der Aufruf sofort mit null zurück. */
    suspend fun erstelle(context: Context, anlass: String): Auswertung? {
        val app = context.applicationContext
        if (!sperre.tryLock()) return null
        try {
            fun schritt(text: String) { _stand.value = _stand.value.copy(laeuft = true, schritt = text) }
            schritt("Frische Biodaten holen")
            val biomarker = Register.alle(app).filterIsInstance<BiomarkerFaehigkeit>().firstOrNull()
            val abgeglichen = runCatching { biomarker?.abgleich() }.getOrNull() == true

            schritt("Wissens-Datenbank holen")
            runCatching { Register.alle(app).filterIsInstance<WissenFaehigkeit>().firstOrNull()?.synchronisiere() }

            schritt("Daten zusammentragen")
            val daten = sammle(app, abgeglichen)

            // Fehlt der Schlafwert von heute noch (Band hat noch nicht übertragen), einmal in 90 Minuten nachbessern.
            val einstellungenVorab = Einstellungen.get(app)
            val heuteTeil = daten.substringAfter("== BIODATEN: WERTE DES AKTUELLEN TAGES ==").substringBefore("== BIODATEN: AKTUELLER STAND")
            val schlafFehlt = "Schlafzeit:" !in heuteTeil
            einstellungenVorab.nachbesserungUm =
                if (schlafFehlt && !anlass.startsWith("nachgebessert") && LocalDateTime.now().hour in 3..18) System.currentTimeMillis() + 90 * 60_000L else 0L

            schritt("Auswertung schreiben")
            val agent = JarvisAgent(app)
            val einstellungen = Einstellungen.get(app)
            val text = runCatching {
                agent.versuche(auftrag(daten), zeitlimitMs = 5 * 60_000L, maxSchritte = 3, beiSchritt = { schritt("Jarvis prüft nach: $it") })
            }.onFailure { Log.w(TAG, "Modell fehlgeschlagen", it) }.getOrNull()?.trim()?.takeIf { it.length > 200 }

            val fertig = Auswertung(
                zeit = System.currentTimeMillis(),
                anlass = anlass,
                text = text ?: "Diese Fassung ist der reine Datenbericht: Das Modell von Jarvis war nicht erreichbar oder nicht verbunden.",
                daten = daten,
                mitKi = text != null,
                modell = if (text != null) einstellungen.modell.label else "",
            )
            speichere(app, fertig)
            _stand.value = AuswertungsStand(laeuft = false, neueste = fertig)
            Protokoll.melde(Quelle.JARVIS, "Tagesauswertung", "$anlass: " + (if (fertig.mitKi) "erstellt mit ${fertig.modell}" else "nur Datenbericht, Modell nicht erreichbar"), fertig.mitKi)
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

            append("== BIODATEN: WERTE DES AKTUELLEN TAGES ==\n")
            append(rufe("biomarker_tag", JSONObject().put("datum", heute.toString()))).append("\n\n")

            append("== BIODATEN: AKTUELLER STAND GEGEN DEN LETZTEN MONAT, 7 TAGE UND ALLE BISHERIGEN TAGE ==\n")
            append(rufe("biomarker_auswertung", JSONObject().put("tage", 30).put("metriken", JSONArray(biomarker?.alleMetriken ?: emptyList<String>())))).append("\n\n")

            append("== TRAININGS DER LETZTEN 14 TAGE ==\n")
            append(rufe("trainings_lesen", JSONObject().put("von", heute.minusDays(14).toString()).put("limit", 20))).append("\n\n")

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
Erstelle Franks Tagesauswertung. Sie wird gespeichert und später von einem Sprachassistenten vorgelesen und besprochen. Schreibe deshalb in ganzen, gut sprechbaren Sätzen, ohne Tabellen und ohne Markdown-Zeichen, mit du-Anrede.

Unten stehen alle Daten, bereits fertig gerechnet. Regeln:
- Verwende ausschließlich Zahlen, die unten stehen. Erfinde und schätze keine Werte. Fehlt etwas (NICHT VERFÜGBAR oder eine alte Messung), sage das in einem Satz und deute es nicht.
- Der Abschnitt RAHMEN ist verbindlich: Arbeitstag oder freier Tag, Schlafzeiten und freie Zeitfenster stehen dort fest. Rechne sie nicht neu.
- Beginne mit genau einem Satz Fazit, der den Tag zusammenfasst, noch vor dem ersten Abschnitt.
- Nenne insgesamt höchstens etwa zwölf Zahlen, gerundet, jede mit einem Richtungswort (zum Beispiel „HRV 49, über deinem Monatsschnitt“). Keine Kalenderdaten in Zahlenform, sage Wochentage.
- Schlaf nach einem Nachtdienst ist Tagschlaf und fällt oft kürzer aus; am Tag nach dem letzten Nachtdienst ist eine kurze Schlafdauer erwartbar und kein schlechtes Zeichen. Vergleiche das vorsichtig mit den Durchschnitten, die überwiegend Nachtschlaf enthalten.
- Schichtlogik: Nach einem Nachtdienst schläft Frank tagsüber etwa von 6 bis 15 Uhr. Schlafwerte gehören zu dem Tag, an dem der Schlaf endet. Steht für heute noch kein Schlafwert da, obwohl er laut Rahmen noch schläft oder gerade erst aufgestanden ist, ist das normal und kein schlechter Wert. Tagesbelastung, Energieumsatz und Schritte des laufenden Tages sind Zwischenstände.
- Stelle den heutigen Tag in den Vordergrund und ordne ihn gegen 7 Tage, den letzten Monat und alle bisherigen Tage ein. Nenne nur die Zahlen, die etwas aussagen: zuerst, was auffällig besser oder schlechter ist, dann kurz das Unauffällige in einem Satz. Keine Aufzählung aller Messgrößen.
- Der Abschnitt WISSENS-DATENBANK ist nur ein Verzeichnis und für die Auswertung ohne Belang.
- Die Liste GENIALE IDEEN ist nur Hintergrundwissen. Zähle die Ideen nicht auf; greife höchstens eine auf, wenn sie heute wirklich passt (freier Tag, gute Erholung).
- Keine medizinischen Diagnosen. Empfehlungen konkret und alltagsnah (Belastung, Schlaf, Erholung, Training).
- Du darfst höchstens zwei Werkzeuge zusätzlich aufrufen, und nur wenn ein auffälliger Wert einen Blick in den Verlauf braucht. Meist ist das nicht nötig.

Gliedere genau in diese sechs Abschnitte, jeder beginnt mit seiner Überschrift in Großbuchstaben auf eigener Zeile:
HEUTE: Was für ein Tag ist heute (Arbeitstag oder frei, welcher Dienst, Abfahrt, Schlaf- und freie Zeitfenster) und welche Termine stehen an. Zwei bis vier Sätze.
ERHOLUNG UND SCHLAF: Der heutige Stand im Vergleich. Vier bis sieben Sätze.
KÖRPER UND TRAINING: Körperwerte nur, wenn es neue oder auffällige gibt. Trainings der letzten Tage, Belastung im Verhältnis zur Erholung, VO2max. Zwei bis fünf Sätze.
EINSCHÄTZUNG: Ein klares Gesamtbild des Tages in zwei bis drei Sätzen: Wie belastbar ist Frank heute.
EMPFEHLUNG FÜR HEUTE: Zwei bis vier konkrete Punkte, passend zu Tagesart, Erholung und freien Zeitfenstern. Du darfst offene Aufgaben einbeziehen, aber nur als Hinweis; die aktuelle Aufgabenliste wird beim Abruf gesondert frisch angehängt.
AUSBLICK: Die nächsten Tage in drei bis fünf Sätzen: kommende Dienste und freie Tage, woran Frank rechtzeitig denken sollte (zum Beispiel Vorbereitung auf einen Nachtdienst-Block, früh schlafen vor einem Tagdienst, Termine, Abholung der Tonnen, Spiele), und wann abends keine Aufgaben mehr passen.

DATEN:
$daten
""".trim()

    /** Die Bereiche der Tagesdatenbank: Kurzname → Anfang der Abschnittsüberschrift. */
    val BEREICHE = linkedMapOf(
        "rahmen" to "RAHMEN", "termine" to "TERMINE", "biodaten_heute" to "BIODATEN: WERTE", "biodaten_vergleich" to "BIODATEN: AKTUELLER",
        "trainings" to "TRAININGS", "ideen" to "GENIALE IDEEN", "wissen" to "WISSENS-DATENBANK",
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
