package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.ablage.AgentenKontext
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.merkKontext
import de.frank.jarvis.faehigkeit.Register
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Das eigene Denken von Jarvis: ein Modell aus der ChatGPT-Anmeldung, das die Werkzeuge aller angebundenen
 * Apps selbst bedient. Die Schleife läuft auf dem Handy: Modell entscheidet → Werkzeug läuft → Ergebnis
 * geht zurück ans Modell, bis eine Antwort steht.
 *
 * Werkzeugaufrufe laufen über ein festes JSON-Format im Antworttext. Das hält die Anbindung unabhängig
 * vom Modell: Jedes Modell, das Text liefert, kann Jarvis antreiben.
 */
class JarvisAgent(context: Context) {
    private val app = context.applicationContext
    val auth = CodexAuthManager(app)
    private val einstellungen = Einstellungen.get(app)

    /**
     * Beantwortet einen Auftrag. [verlauf] sind die bisherigen Wortwechsel des Gesprächs,
     * [zeitlimitMs] begrenzt die Gesamtdauer (wichtig für Aufrufe aus dem Plugin).
     */
    suspend fun frage(
        auftrag: String,
        verlauf: List<ChatTurn> = emptyList(),
        zeitlimitMs: Long = 120_000,
        maxSchritte: Int = 8,
        beiSchritt: (String) -> Unit = {},
    ): String {
        if (!auth.isConnected) return "Ich bin noch nicht mit ChatGPT verbunden. Bitte in Jarvis unter Einstellungen anmelden."
        return versuche(auftrag, verlauf, zeitlimitMs, maxSchritte, beiSchritt, lernen = true, abschluss = true)
            ?: ("Ich bin nicht fertig geworden" + (abbruchGrund?.let { " ($it)" } ?: "") + ". Für große Aufgaben wie Recherchen oder Berichte sag mir, " +
                "ich soll den Recherche-Agenten starten; der arbeitet im Hintergrund weiter. Was genau passiert ist, steht unter Aktivität.")
    }

    val verbunden: Boolean get() = auth.isConnected

    /** Warum der letzte Lauf ohne fertige Antwort endete (Schritt- oder Zeitlimit, zuletzt laufendes Werkzeug); null, wenn er regulär fertig wurde. */
    @Volatile var abbruchGrund: String? = null
        private set

    /**
     * Wie [frage], liefert aber null statt eines Entschuldigungssatzes, wenn keine Antwort zustande kommt
     * (nicht verbunden, Zeitlimit, zu viele Schritte). Für Abläufe im Hintergrund, die dann selbst entscheiden.
     */
    suspend fun versuche(
        auftrag: String,
        verlauf: List<ChatTurn> = emptyList(),
        zeitlimitMs: Long = 120_000,
        maxSchritte: Int = 8,
        beiSchritt: (String) -> Unit = {},
        rolle: String? = null,
        mitInternet: Boolean = false,
        ablageTitel: String? = null,
        lernen: Boolean = false,
        /** true = reichen Schritte oder Zeit nicht, schreibt das Modell zum Schluss eine Antwort aus dem bisher Gesammelten, statt alles zu verwerfen. */
        abschluss: Boolean = false,
    ): String? {
        abbruchGrund = null
        if (!auth.isConnected) { abbruchGrund = "Jarvis ist nicht mit ChatGPT angemeldet"; return null }
        // Regeln und Notizen über Frank schreibt Jarvis nur im Gespräch mit Frank. Agenten und die Tagesauswertung lesen fremde Texte (Internet, Mails) und haben beides nur vor Augen.
        // Agenten beauftragen und das Protokoll lesen nur im Gespräch mit Frank: Agenten starten keine Agenten.
        val faehigkeiten = Register.alle(app).filter { (mitInternet || it.id != "web") && (lernen || (it.id != "regeln" && it.id != "agenten")) }
        val werkzeuge = faehigkeiten.flatMap { it.werkzeuge }
        val zuege = (verlauf.takeLast(12) + ChatTurn("user", auftrag)).toMutableList()
        // Im Gespräch und bei Agenten denkt Jarvis mit dem ganzen Gesamtbild. Beim Schreiben der Tagesauswertung selbst
        // genügen Regeln und Wissen über Frank: Die Daten stehen dort im Auftrag, und die alte Auswertung soll die neue nicht färben.
        val gesamtbild = if (lernen || rolle != null) runCatching { Gehirn.kontext(app) }.getOrElse { merkKontext(app) } else merkKontext(app)
        // Für den Abschluss bleibt ein Teil der Zeit übrig: Ein langes Werkzeug (Bild, Download) darf nicht das ganze Ergebnis verschlucken.
        val reserve = if (abschluss) (zeitlimitMs / 6).coerceIn(25_000L, 180_000L) else 0L
        var schritte = 0
        var alleSchritte = false
        var letztesWerkzeug = ""
        val ergebnis = withTimeoutOrNull<String?>(zeitlimitMs - reserve) {
            repeat(maxSchritte) {
                schritte++
                val roh = auth.streamChat(anweisung(faehigkeiten, rolle, gesamtbild), zuege, einstellungen.modell, einstellungen.denkstufe)
                val schritt = lies(roh)
                val name = schritt?.optString("werkzeug").orEmpty()
                if (schritt == null || name.isEmpty()) {
                    return@withTimeoutOrNull schritt?.optString("antwort")?.takeIf { it.isNotBlank() } ?: roh.trim()
                }
                val werkzeug = werkzeuge.firstOrNull { it.name == name }
                val argumente = schritt.optJSONObject("argumente") ?: JSONObject()
                letztesWerkzeug = werkzeug?.titel ?: name
                beiSchritt(werkzeug?.titel ?: name)
                val antwort = if (werkzeug == null) {
                    "FEHLER: Das Werkzeug $name gibt es nicht."
                } else {
                    // Werkzeuge dürfen hier auf lange Arbeit warten (Bild, Download); Dateien ohne Titel gehören zum Agenten-Eintrag.
                    val r = runCatching { withContext(AgentenKontext(ablageTitel, langeWarten = ablageTitel != null)) { werkzeug.ausfuehren(argumente) } }
                        .getOrElse { de.frank.jarvis.faehigkeit.Ergebnis("Werkzeug fehlgeschlagen: ${it.message}", fehler = true) }
                    Protokoll.melde(Quelle.JARVIS, werkzeug.titel, r.text, !r.fehler)
                    (if (r.fehler) "FEHLER: " else "") + r.text
                }
                zuege += ChatTurn("assistant", schritt.toString())
                zuege += ChatTurn("user", "WERKZEUG-ERGEBNIS ($name):\n$antwort")
            }
            alleSchritte = true
            null
        }
        if (ergebnis != null) return ergebnis
        val grund = (if (alleSchritte) "Schrittlimit von $maxSchritte Schritten erreicht" else "Zeitlimit von ${dauer(zeitlimitMs - reserve)} erreicht") +
            (if (letztesWerkzeug.isEmpty()) "" else ", zuletzt: $letztesWerkzeug")
        if (!abschluss || schritte == 0) {
            abbruchGrund = grund
            Protokoll.melde(Quelle.JARVIS, "Abbruch", grund, ok = false)
            return null
        }
        zuege += ChatTurn("user", "ABSCHLUSS: $grund. Rufe KEIN Werkzeug mehr auf. Antworte jetzt sofort mit {\"antwort\": \"…\"} und schreibe das Ergebnis aus allem, " +
            "was du bisher hast. Sag am Anfang in einem Satz, dass es ein Zwischenstand ist und was noch fehlt. Nenne nur Dateien, deren Speicherung ein Werkzeug-Ergebnis bestätigt hat.")
        val ende = withTimeoutOrNull(reserve) {
            runCatching { auth.streamChat(anweisung(faehigkeiten, rolle, gesamtbild), zuege, einstellungen.modell, einstellungen.denkstufe) }.getOrNull()
        }
        val antwort = ende?.let { roh -> lies(roh)?.optString("antwort")?.takeIf { it.isNotBlank() } ?: roh.trim().takeIf { it.isNotEmpty() && !it.trimStart().startsWith("{") } }
        abbruchGrund = if (antwort != null) "$grund; Zwischenstand geschrieben" else "$grund; auch der Zwischenstand kam nicht zustande"
        Protokoll.melde(Quelle.JARVIS, "Abbruch", abbruchGrund.orEmpty(), ok = false)
        return antwort
    }

    private fun dauer(ms: Long): String = if (ms < 120_000L) "${ms / 1000} Sekunden" else "${ms / 60_000} Minuten"

    private fun anweisung(faehigkeiten: List<de.frank.jarvis.faehigkeit.Faehigkeit>, rolle: String?, gesamtbild: String): String {
        val jetzt = LocalDateTime.now()
        val datum = jetzt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
        return buildString {
            if (rolle != null) append(rolle).append("\n\n")
            append("Du bist Jarvis, Franks persönlicher Assistent auf seinem Handy. Du sprichst Frank mit „du“ an, antwortest so kurz, wie es die Frage erlaubt, ")
            append("klar und freundlich in gutem Deutsch, so dass man es gut vorlesen kann (keine Listenzeichen, kein Markdown, keine ids).\n")
            append("Jetzt ist ").append(datum).append(" (ISO-Datum ").append(jetzt.toLocalDate()).append(").\n\n")
            append("Du arbeitest mit Franks Apps über Werkzeuge. Grundsätze:\n")
            append("- Handle selbstständig: Lies nach, bevor du fragst. Frage nur, wenn eine nötige Angabe wirklich fehlt oder mehrdeutig ist.\n")
            append("- Erfinde nie Daten. Was du über Aufgaben sagst, stammt aus einem Werkzeug-Ergebnis.\n")
            append("- Nach einer Änderung bestätigst du in einem Satz, was jetzt gilt.\n")
            append("- Behaupte nie, eine Datei, ein Bild oder ein Dokument erzeugt oder gespeichert zu haben, wenn ein Werkzeug-Ergebnis das nicht mit „Gespeichert“ bestätigt. ")
            append("Meldet ein Werkzeug „läuft noch“ oder einen Fehler, sag genau das.\n\n")
            if (gesamtbild.isNotEmpty()) append(gesamtbild).append('\n')
            faehigkeiten.forEach { append("App ").append(it.name).append(": ").append(it.hinweise).append("\n\n") }
            append("WERKZEUGE:\n")
            faehigkeiten.flatMap { it.werkzeuge }.forEach { w ->
                append("- ").append(w.name).append(": ").append(w.beschreibung).append("\n  Eingabe (JSON-Schema): ").append(w.schema.toString()).append('\n')
            }
            append("\nANTWORTFORMAT — antworte IMMER mit genau einem JSON-Objekt und sonst nichts:\n")
            append("Werkzeug aufrufen: {\"werkzeug\": \"<name>\", \"argumente\": { … }}\n")
            append("Frank antworten oder nachfragen: {\"antwort\": \"<Text>\"}\n")
            append("Pro Antwort genau ein Werkzeug. Das Ergebnis bekommst du als nächste Nachricht, die mit WERKZEUG-ERGEBNIS beginnt.")
        }
    }

    /** Holt das JSON-Objekt aus der Modellantwort, auch wenn es in Text oder Codezäune eingepackt ist. */
    private fun lies(roh: String): JSONObject? {
        val start = roh.indexOf('{')
        val ende = roh.lastIndexOf('}')
        if (start < 0 || ende <= start) return null
        return runCatching { JSONObject(roh.substring(start, ende + 1)) }.getOrNull()
            ?.takeIf { it.has("werkzeug") || it.has("antwort") }
    }
}
