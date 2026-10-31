package de.frank.jarvis.helfer

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ein Zugang im zentralen Bereich „Konten und APIs“. Werte kommen nur über [lesen]; außerhalb von Oberfläche (nach Fingerabdruck) und
 * [HelferLauf] verlässt kein Wert diese Klasse. [beschreibung] sind genau zwei Sätze: was er macht und wofür er verwendet wird.
 */
class KontoEintrag(
    val id: String, val titel: String, val beschreibung: String, val dienst: String, val fest: Boolean,
    val verwendetVon: String, val lesen: () -> String, val schreiben: (String) -> Unit,
)

/**
 * Der zentrale Bereich für alle Schlüssel. Die bestehenden Schlüssel bleiben in ihren verschlüsselten Speichern ([Einstellungen], [SecureSettings]):
 * nichts wandert um, also kann bei der Übernahme nichts verloren gehen, und jede funktionierende Anbindung liest weiter wie bisher. Neue Schlüssel für
 * Helfer liegen im eigenen Speicher `jarvis_konten`. Grundlage ist überall EncryptedSharedPreferences mit einem Hauptschlüssel im Android-Keystore.
 *
 * Schutzgrenzen ehrlich: Das schützt vor Auslesen der Dateien, Sicherungen und vor Helfern (sie bekommen nur eine autorisierte Funktion, nie den Wert).
 * Es schützt nicht vor einem gerooteten Gerät oder Code, der in der entsperrten App selbst läuft.
 */
object KontenRegister {
    private const val SPEICHER = "jarvis_konten"
    val stand = MutableStateFlow(0)

    @Volatile private var offen: SharedPreferences? = null
    private fun speicher(c: Context): SharedPreferences = offen ?: synchronized(this) {
        offen ?: EncryptedSharedPreferences.create(
            SPEICHER, MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC), c.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).also { offen = it }
    }

    private fun feste(c: Context): List<KontoEintrag> {
        val e = Einstellungen.get(c)
        val s = SecureSettings.get(c)
        fun k(id: String, titel: String, was: String, wozu: String, dienst: String, lies: () -> String, schreib: (String) -> Unit) =
            KontoEintrag(id, titel, "$was $wozu", dienst, true, wozu.removeSuffix("."), lies, schreib)
        return listOf(
            k("anthropic", "Anthropic", "Meldet Jarvis bei den Claude-Modellen an.", "Wird im Chat verwendet, wenn du Claude als Kopf wählst.", "platform.claude.com", { s.anthropicApiKey }) { s.anthropicApiKey = it },
            k("openai", "OpenAI", "Öffnet die OpenAI-Plattform mit eigener Abrechnung.", "Wird für das Live-Gespräch verwendet.", "platform.openai.com", { s.openAiApiKey }) { s.openAiApiKey = it },
            k("gemini_frei", "Gemini kostenlos", "Öffnet Google AI Studio ohne Abrechnung.", "Wird für Gemini Live verwendet.", "aistudio.google.com", { s.geminiApiKey }) { s.geminiApiKey = it },
            k("gemini_bezahlt", "Gemini bezahlt", "Öffnet Google AI Studio mit Abrechnung.", "Wird für Gemini Live verwendet, wenn das kostenlose Kontingent aufgebraucht ist.", "aistudio.google.com", { s.geminiApiKeyBezahlt }) { s.geminiApiKeyBezahlt = it },
            k("gemini_tts_frei", "Gemini-Stimme kostenlos", "Erzeugt Sprache mit den Gemini-Stimmen ohne Abrechnung.", "Wird zum Vorlesen verwendet.", "aistudio.google.com", { s.geminiTtsKeyFrei }) { s.geminiTtsKeyFrei = it },
            k("gemini_tts_bezahlt", "Gemini-Stimme bezahlt", "Erzeugt Sprache mit den Gemini-Stimmen mit Abrechnung.", "Wird zum Vorlesen verwendet, je nach deiner Schlüsselwahl.", "aistudio.google.com", { s.geminiTtsKeyBezahlt }) { s.geminiTtsKeyBezahlt = it },
            k("google_tts", "Google Cloud Sprache", "Erzeugt Sprache mit den Chirp-Stimmen von Google.", "Wird zum Vorlesen und als Rückfallstimme verwendet.", "console.cloud.google.com", { s.googleTtsApiKey }) { s.googleTtsApiKey = it },
            k("azure", "Azure Speech", "Erzeugt Sprache mit den Azure-Stimmen von Microsoft.", "Wird zum Vorlesen verwendet.", "portal.azure.com", { s.azureTtsKey }) { s.azureTtsKey = it },
            k("qwen", "Alibaba Qwen", "Erzeugt Sprache mit den Qwen-Stimmen und deiner geklonten Stimme.", "Wird zum Vorlesen verwendet.", "alibabacloud.com", { s.qwenTtsApiKey }) { s.qwenTtsApiKey = it },
            k("groq", "Groq", "Wandelt Sprache mit Whisper in Text um.", "Wird für die Spracheingabe online verwendet.", "console.groq.com", { s.groqApiKey }) { s.groqApiKey = it },
            k("tavily", "Tavily", "Durchsucht das Internet für Jarvis.", "Wird für die Websuche und die Recherche-Agenten verwendet.", "tavily.com", { e.suchSchluessel }) { e.suchSchluessel = it },
            k("maps", "Google Maps", "Berechnet Fahrzeiten mit aktuellem Verkehr.", "Wird für die Fahrzeit und die Losfahr-Meldung verwendet.", "console.cloud.google.com", { e.mapsSchluessel }) { e.mapsSchluessel = it },
            k("github", "GitHub", "Erlaubt das Lesen deines Repos mit Code-Suche.", "Wird von repo_lesen verwendet und schreibt nie.", "github.com", { e.githubToken }) { e.githubToken = it },
            k("mail", "Gmail App-Passwort", "Meldet Jarvis bei deinem Gmail-Konto an.", "Wird zum Lesen und Senden von E-Mails verwendet.", "myaccount.google.com", { e.mailPasswort }) { e.mailPasswort = it },
            k("drive", "Google Drive", "Meldet Jarvis bei deinem Drive an.", "Wird für das Tagebuch verwendet.", "console.cloud.google.com", { e.driveClientSecret }) { e.driveClientSecret = it },
            k("routine", "Claude-Routine", "Löst die Programmier-Routine in der Cloud aus.", "Wird für Programmieraufträge ohne erreichbaren Rechner verwendet.", "claude.ai", { e.routineToken }) { e.routineToken = it },
            k("server", "Jarvis-Server", "Weist dieses Handy am eigenen Server aus.", "Wird für Plugin, Programmieraufträge, Ablage und Mac-Kanal verwendet.", "eigener Server", { e.serverToken }) { e.serverToken = it },
        )
    }

    private fun weitereMeta(c: Context): JSONArray = runCatching { JSONArray(speicher(c).getString("meta", "[]")) }.getOrDefault(JSONArray())

    private fun weitere(c: Context): List<KontoEintrag> {
        val m = weitereMeta(c)
        return (0 until m.length()).mapNotNull { m.optJSONObject(it) }.map { j ->
            val id = j.optString("id")
            KontoEintrag(id, j.optString("titel"), j.optString("beschreibung"), j.optString("dienst"), false, "",
                { speicher(c).getString("wert_$id", "").orEmpty() },
                { v -> speicher(c).edit().putString("wert_$id", v.trim()).apply(); stand.value++ })
        }
    }

    fun alle(c: Context): List<KontoEintrag> = feste(c) + runCatching { weitere(c) }.getOrDefault(emptyList())

    fun finde(c: Context, id: String): KontoEintrag? = alle(c).firstOrNull { it.id == id }

    fun neu(c: Context, titel: String, beschreibung: String, dienst: String, wert: String): String? {
        val id = titel.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(30)
        if (id.isEmpty() || titel.isBlank() || beschreibung.isBlank() || wert.isBlank()) return "Name, Beschreibung und Schlüssel sind nötig."
        if (finde(c, id) != null) return "Einen Zugang mit diesem Namen gibt es schon."
        synchronized(this) {
            val m = weitereMeta(c).put(JSONObject().put("id", id).put("titel", titel.trim()).put("beschreibung", beschreibung.trim()).put("dienst", dienst.trim()))
            speicher(c).edit().putString("meta", m.toString()).putString("wert_$id", wert.trim()).apply()
        }
        stand.value++
        return null
    }

    /** Entfernt einen selbst angelegten Zugang. Liefert einen Grund, wenn das nicht geht. */
    fun entfernen(c: Context, id: String): String? {
        val nutzer = verwendetVonHelfern(c, id)
        if (nutzer.isNotEmpty()) return "Noch in Gebrauch: " + nutzer.joinToString(", ") + ". Erst dort entfernen."
        if (finde(c, id)?.fest != false) return "Eingebaute Zugänge bleiben bestehen; leere dort den Wert."
        synchronized(this) {
            val m = weitereMeta(c)
            val rest = JSONArray((0 until m.length()).mapNotNull { m.optJSONObject(it) }.filter { it.optString("id") != id })
            speicher(c).edit().putString("meta", rest.toString()).remove("wert_$id").remove("gesperrt_$id").apply()
        }
        stand.value++
        return null
    }

    /** Sperrt einen Zugang für alle Helfer sofort (etwa bei Verdacht auf Missbrauch); Jarvis' feste Anbindungen betrifft das nicht. */
    fun sperren(c: Context, id: String, an: Boolean) { speicher(c).edit().putBoolean("gesperrt_$id", an).apply(); stand.value++ }
    fun gesperrt(c: Context, id: String): Boolean = runCatching { speicher(c).getBoolean("gesperrt_$id", false) }.getOrDefault(false)

    fun verwendetVonHelfern(c: Context, id: String): List<String> = (HelferKatalog.alle(c, true)).filter { h -> h.rechte.konten.any { it.id == id } }.map { it.name }

    fun nutzungMerken(c: Context, id: String, helfer: String) {
        runCatching { speicher(c).edit().putString("nutzung_$id", "$helfer|${System.currentTimeMillis()}").apply() }
    }
    fun letzteNutzung(c: Context, id: String): Pair<String, Long>? = runCatching {
        speicher(c).getString("nutzung_$id", null)?.split('|')?.let { it[0] to it[1].toLong() }
    }.getOrNull()

    /** Für die Schwärzung in Helfer-Ergebnissen: alle Werte ab acht Zeichen. Wird nie gespeichert oder ausgegeben. */
    fun werte(c: Context): List<String> = alle(c).map { runCatching { it.lesen() }.getOrDefault("") }.filter { it.length >= 8 }

    fun schwaerzen(c: Context, text: String): String = werte(c).fold(text) { t, w -> t.replace(w, "***") }

    /** Nur Namen und Stand, nie Werte: für Modell und Plugin. */
    fun uebersicht(c: Context): String = alle(c).joinToString("\n") { k ->
        val h = verwendetVonHelfern(c, k.id)
        "- ${k.id}: ${k.titel} (${if (k.lesen().isBlank()) "fehlt" else "eingetragen"}${if (gesperrt(c, k.id)) ", für Helfer gesperrt" else ""})" +
            (if (h.isEmpty()) "" else ", Helfer: " + h.joinToString(", "))
    }
}
