package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Register
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
        val werkzeuge = Register.werkzeuge(app)
        val zuege = (verlauf.takeLast(12) + ChatTurn("user", auftrag)).toMutableList()
        val ergebnis = withTimeoutOrNull(zeitlimitMs) {
            repeat(maxSchritte) {
                val roh = auth.streamChat(anweisung(), zuege, einstellungen.modell, einstellungen.denkstufe)
                val schritt = lies(roh)
                val name = schritt?.optString("werkzeug").orEmpty()
                if (schritt == null || name.isEmpty()) {
                    return@withTimeoutOrNull schritt?.optString("antwort")?.takeIf { it.isNotBlank() } ?: roh.trim()
                }
                val werkzeug = werkzeuge.firstOrNull { it.name == name }
                val argumente = schritt.optJSONObject("argumente") ?: JSONObject()
                beiSchritt(werkzeug?.titel ?: name)
                val antwort = if (werkzeug == null) {
                    "FEHLER: Das Werkzeug $name gibt es nicht."
                } else {
                    val r = runCatching { werkzeug.ausfuehren(argumente) }
                        .getOrElse { de.frank.jarvis.faehigkeit.Ergebnis("Werkzeug fehlgeschlagen: ${it.message}", fehler = true) }
                    Protokoll.melde(Quelle.JARVIS, werkzeug.titel, r.text, !r.fehler)
                    (if (r.fehler) "FEHLER: " else "") + r.text
                }
                zuege += ChatTurn("assistant", schritt.toString())
                zuege += ChatTurn("user", "WERKZEUG-ERGEBNIS ($name):\n$antwort")
            }
            "Ich habe mehrere Schritte versucht, bin aber nicht fertig geworden. Bitte formuliere den Auftrag genauer."
        }
        return ergebnis ?: "Das hat zu lange gedauert. Bitte versuche es noch einmal oder teile den Auftrag auf."
    }

    private fun anweisung(): String {
        val jetzt = LocalDateTime.now()
        val datum = jetzt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
        val faehigkeiten = Register.alle(app)
        return buildString {
            append("Du bist Jarvis, Franks persönlicher Assistent auf seinem Handy. Du sprichst Frank mit „du“ an, antwortest kurz, ")
            append("klar und freundlich in gutem Deutsch, so dass man es gut vorlesen kann (keine Listenzeichen, kein Markdown, keine ids).\n")
            append("Jetzt ist ").append(datum).append(" (ISO-Datum ").append(jetzt.toLocalDate()).append(").\n\n")
            append("Du arbeitest mit Franks Apps über Werkzeuge. Regeln:\n")
            append("- Handle selbstständig: Lies nach, bevor du fragst. Frage nur, wenn eine nötige Angabe wirklich fehlt oder mehrdeutig ist.\n")
            append("- Erfinde nie Daten. Was du über Aufgaben sagst, stammt aus einem Werkzeug-Ergebnis.\n")
            append("- Nach einer Änderung bestätigst du in einem Satz, was jetzt gilt.\n\n")
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
