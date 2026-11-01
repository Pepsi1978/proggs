package de.frank.jarvis.ablage

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.ReasoningEffort
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.gedaechtnis.Bereich
import de.frank.jarvis.gedaechtnis.Lagebild
import de.frank.jarvis.meldung.Meldungen
import de.frank.jarvis.meldung.Meldungsarten
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.GespraechsFokus
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Allgemeine Erinnerungen: Das Modell entscheidet höchstens alle drei Stunden, ob jetzt ein Hinweis passt. Es sieht dabei Dienst, Schlaf, Termine und die
 * Tagesauswertung. Entscheidet es sich für eine, bekommt sie einen Hinweis-Zeitpunkt und läuft danach wie jede Auslösung: Gerät, das Frank gerade benutzt, Kurzfassung vorlesen, einmal bestätigen.
 * Fest gerechnete Grenzen gegen Aufdringlichkeit: höchstens ein Hinweis je acht Stunden, dieselbe Erinnerung höchstens einmal in 24 Stunden.
 * Nur das Handy entscheidet: So kommt keiner doppelt.
 */
object ErinnerungHinweis {
    private const val PREFS = "erinnerung_hinweis"
    private const val PRUEF_ABSTAND_MS = 3 * 3_600_000L
    private const val HINWEIS_ABSTAND_MS = 8 * 3_600_000L
    private const val JE_ERINNERUNG_MS = 24 * 3_600_000L

    private val ANWEISUNG = """
        Du bist Jarvis und entscheidest, ob Frank gerade ein unaufdringlicher Hinweis auf eine seiner allgemeinen Erinnerungen ohne festen Termin passt.
        Halte dich streng an den RAHMEN: nie während er laut Rahmen schläft oder im Dienst ist, nie kurz vor der Abfahrt zum Dienst, nicht wenn der Tag voll ist. Passend sind freie, wache Zeiten; bevorzuge die Erinnerung, die zur Lage und zu seiner Kraft passt.
        Im Zweifel kein Hinweis. Antworte nur mit JSON: {"id":"<id>"} oder {"id":null}.
    """.trimIndent()

    private fun netzDa(app: Context) = app.getSystemService(ConnectivityManager::class.java).let { c -> c.getNetworkCapabilities(c.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true }

    /** Aus der Minutenrunde des Wächters; kostet nichts, solange nichts ansteht. */
    suspend fun pruefen(context: Context) {
        val app = context.applicationContext
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val jetzt = System.currentTimeMillis()
        if (jetzt - p.getLong("geprueft", 0L) < PRUEF_ABSTAND_MS) return
        val offen = Erinnerungen.allgemeine(app).filter { it.erledigt == 0L }
        if (offen.isEmpty()) return
        val zuletzt = offen.maxOf { maxOf(it.hinweisUm, it.zuletztErwaehnt) }
        if (jetzt - zuletzt < HINWEIS_ABSTAND_MS) return
        val moeglich = offen.filter { jetzt - maxOf(it.hinweisUm, it.zuletztErwaehnt) >= JE_ERINNERUNG_MS }
        if (moeglich.isEmpty() || GespraechsFokus.stilleHinweise || GespraechsFokus.aufnahme || !netzDa(app) || Meldungen.inRuhe(app, Meldungsarten.AGENT)) return
        val agent = JarvisAgent(app)
        if (!agent.verbunden) return
        p.edit().putLong("geprueft", jetzt).apply()
        val a = Lagebild.stand.value.abschnitte
        val eingabe = buildString {
            append("JETZT: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))).append("\n\n")
            append("RAHMEN:\n").append(a[Bereich.RAHMEN]?.text.orEmpty().take(2_000)).append("\n\n")
            append("TERMINE:\n").append(a[Bereich.TERMINE]?.text.orEmpty().take(1_500)).append("\n\n")
            append("TAGESAUSWERTUNG:\n").append(a[Bereich.AUSWERTUNG]?.text.orEmpty().take(1_500)).append("\n\n")
            append("ALLGEMEINE ERINNERUNGEN:\n").append(moeglich.joinToString("\n") { "- id ${it.id}: ${it.kurz}" + (if (it.zuletztErwaehnt > 0) " (zuletzt erwähnt ${Erinnerungen.zeitText(it.zuletztErwaehnt)})" else "") })
        }
        val e = Einstellungen.get(app)
        val antwort = runCatching { agent.auth.streamChat(ANWEISUNG, listOf(ChatTurn("user", eingabe)), e.modell, ReasoningEffort.LOW).trim() }
            .onFailure { Logbuch.fehler("Erinnerungshinweis", it) }.getOrNull() ?: return
        val id = runCatching { JSONObject(antwort.substring(antwort.indexOf('{'), antwort.lastIndexOf('}') + 1)).let { j -> if (j.isNull("id")) null else j.optString("id") } }.getOrNull() ?: return
        // Das Modell darf nur wählen, was erlaubt ist.
        if (moeglich.none { it.id == id }) return
        val nun = System.currentTimeMillis()
        Erinnerungen.aendere(app, id) { it.copy(hinweisUm = nun, zuletztErwaehnt = nun) }
        Logbuch.system("Erinnerungshinweis geplant", id)
    }
}
