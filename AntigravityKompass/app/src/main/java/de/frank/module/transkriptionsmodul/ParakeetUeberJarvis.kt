package de.frank.module.transkriptionsmodul

import android.content.Context
import android.os.Bundle
import android.os.ParcelFileDescriptor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline-Transkription über das Parakeet-Modell in Jarvis: Diese App lädt kein eigenes Modell, sondern schickt ihre Aufnahme samt
 * eigener Wortliste an Jarvis (`ParakeetBruecke`) und bekommt den Text zurück. Fehlt Jarvis oder das Modell, gibt es einen Fehler,
 * nie einen stillen Rückfall auf einen Online-Dienst.
 *
 * Braucht im Manifest die Signatur-Erlaubnis `de.frank.jarvis.permission.BRUECKE` und `<queries>` für `de.frank.jarvis.parakeet`
 * (Modul M1.4, MODUL.md). Die Einstellungen gehören zur App und liegen in eigenen Prefs.
 */
object ParakeetUeberJarvis {
    private const val ADRESSE = "de.frank.jarvis.parakeet"
    private const val PREFS = "parakeet_ueber_jarvis"

    /** Startliste: die Begriffe aus Jarvis. Danach pflegt jede App ihre eigene Liste. */
    const val STANDARD_WOERTERBUCH =
        "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic"

    private const val MAX_BEGRIFFE = 100

    enum class Verbindung(val bereit: Boolean, val text: String) {
        VERBUNDEN(true, "Verknüpft mit Jarvis: Modell bereit, kein Download nötig."),
        MODELL_FEHLT(false, "Jarvis ist da, aber das Modell fehlt noch. Lade es in Jarvis unter Einstellungen → Sprache → Spracheingabe herunter."),
        JARVIS_FEHLT(false, "Jarvis ist nicht installiert oder zu alt."),
        KEINE_ERLAUBNIS(false, "Jarvis lehnt den Zugriff ab. Installiere diese App nach Jarvis noch einmal."),
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun gewaehlt(context: Context): Boolean = prefs(context).getBoolean("gewaehlt", false)
    fun gewaehlt(context: Context, an: Boolean) = prefs(context).edit().putBoolean("gewaehlt", an).apply()

    fun woerterbuchAn(context: Context): Boolean = prefs(context).getBoolean("woerterbuch_an", true)
    fun woerterbuchAn(context: Context, an: Boolean) = prefs(context).edit().putBoolean("woerterbuch_an", an).apply()

    fun begriffe(context: Context): String = prefs(context).getString("begriffe", STANDARD_WOERTERBUCH) ?: STANDARD_WOERTERBUCH
    fun begriffe(context: Context, text: String) = prefs(context).edit().putString("begriffe", text).apply()

    /** Begriffe im Format von sherpa-onnx: durch „/“ getrennt; „/“, „:“ und „#“ haben dort eine eigene Bedeutung und fallen weg. */
    private fun hotwords(context: Context): String {
        if (!woerterbuchAn(context)) return ""
        return begriffe(context).split('\n', '\r', ',', ';')
            .map { it.replace(Regex("[/:#]"), " ").trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(MAX_BEGRIFFE)
            .joinToString("/")
    }

    suspend fun pruefeVerbindung(context: Context): Verbindung = withContext(Dispatchers.IO) {
        try {
            val antwort = rufe(context, "status", Bundle()) ?: return@withContext Verbindung.JARVIS_FEHLT
            if (antwort.getBoolean("modell_bereit")) Verbindung.VERBUNDEN else Verbindung.MODELL_FEHLT
        } catch (_: SecurityException) {
            Verbindung.KEINE_ERLAUBNIS
        } catch (_: Exception) {
            Verbindung.JARVIS_FEHLT
        }
    }

    /** Lädt das Modell in Jarvis vor (beim Aufnahmestart), damit das Diktat nicht wartet. Fehler zählen hier nicht. */
    suspend fun vorwaermen(context: Context) = withContext(Dispatchers.IO) {
        runCatching { rufe(context, "vorwaermen", Bundle()) }
        Unit
    }

    /** Mitschrift einer Aufnahme (WAV in beliebiger Abtastrate oder eine andere Audiodatei, etwa MP4). */
    suspend fun transkribiere(context: Context, wav: ByteArray): String = withContext(Dispatchers.IO) {
        val datei = File.createTempFile("diktat", ".audio", context.cacheDir)
        try {
            datei.writeBytes(wav)
            transkribiereDatei(context, datei)
        } finally {
            datei.delete()
        }
    }

    suspend fun transkribiere(context: Context, audio: File): String = withContext(Dispatchers.IO) {
        transkribiereDatei(context, audio)
    }

    private fun transkribiereDatei(context: Context, audio: File): String {
        val extras = Bundle().apply {
            putParcelable("audio", ParcelFileDescriptor.open(audio, ParcelFileDescriptor.MODE_READ_ONLY))
            putString("hotwords", hotwords(context))
        }
        val antwort = rufe(context, "transkribiere", extras)
            ?: throw ParakeetUeberJarvisFehler("Jarvis ist nicht erreichbar. Ist die App installiert?")
        antwort.getString("fehler")?.let { throw ParakeetUeberJarvisFehler(it) }
        return antwort.getString("text").orEmpty().trim()
    }

    /** Unstabiler Client: Stürzt Jarvis mitten im Aufruf ab, beendet Android nicht auch diese App. Null = kein Jarvis gefunden. */
    private fun rufe(context: Context, methode: String, extras: Bundle): Bundle? {
        val client = context.applicationContext.contentResolver.acquireUnstableContentProviderClient(ADRESSE) ?: return null
        return try {
            client.call(methode, null, extras)
        } finally {
            client.close()
        }
    }
}

class ParakeetUeberJarvisFehler(message: String) : Exception(message)
