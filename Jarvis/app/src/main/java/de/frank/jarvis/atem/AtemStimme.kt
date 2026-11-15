package de.frank.jarvis.atem

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.speech.Synthese
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Stimme der Atemübungen: dieselbe Vorlesestimme wie in Jarvis, aber vorab erzeugt und dauerhaft in files/atem/stimme abgelegt.
 * So kommt jedes Stichwort pünktlich ohne Netz-Wartezeit, und die Satzlänge ist vor dem Start bekannt.
 * Der Schlüssel enthält Anbieter, Stimmen und Tempo: Ein Stimmwechsel in Jarvis erzeugt die Clips neu.
 */
object AtemStimme {
    data class Clip(val datei: File, val ms: Long)

    const val EINATMEN = "Einatmen."
    const val HALTEN = "Halten."
    const val AUSATMEN = "Ausatmen."
    const val INTRO = "Mach es dir bequem. Setz oder leg dich entspannt hin, lass die Schultern sinken und atme ganz natürlich. Gleich geht es los."
    const val OUTRO = "Sehr gut gemacht. Atme jetzt wieder ganz natürlich und spür noch einen Moment nach, wie ruhig dein Körper geworden ist."

    private const val TAG = "AtemStimme"
    private val sperre = Mutex()
    private val dauern = HashMap<String, Long>()

    private fun stimmKennung(s: SecureSettings): String {
        val a = s.ttsProvider
        return listOf(a, s.ttsTempo(a), s.edgeTtsVoice, s.googleTtsVoice, s.azureTtsVoice, s.geminiFlashTtsVoice, s.geminiLiteTtsVoice, s.qwenStandardVoice, s.qwenTtsVoiceId).joinToString("|")
    }

    private fun schluessel(text: String, kennung: String): String =
        MessageDigest.getInstance("SHA-1").digest("$kennung#$text".toByteArray()).joinToString("") { "%02x".format(it) }

    private fun ordner(context: Context) = File(context.filesDir, "atem/stimme").apply { mkdirs() }

    /** Schon erzeugter Clip, ohne Netz; null, wenn er noch fehlt. */
    fun vorhanden(context: Context, text: String): Clip? {
        val s = SecureSettings(context)
        val k = schluessel(text, stimmKennung(s))
        val d = ordner(context).listFiles()?.firstOrNull { it.name.startsWith("$k.") && it.length() > 0 } ?: return null
        return Clip(d, dauer(d))
    }

    /** Holt oder erzeugt den Clip. Scheitert die Stimme (offline, Kontingent), kommt null: Die Übung läuft dann ohne. */
    suspend fun hole(context: Context, text: String): Clip? {
        vorhanden(context, text)?.let { return it }
        return sperre.withLock {
            vorhanden(context, text) ?: runCatching {
                val app = context.applicationContext
                val s = SecureSettings(app)
                val roh = Synthese(app, s).synthetisiere(text)
                withContext(Dispatchers.IO) {
                    // Sofort wegkopieren: Der Vorleser räumt seinen Zwischenordner nach jeder Wiedergabe leer.
                    val ziel = File(ordner(app), schluessel(text, stimmKennung(s)) + "." + roh.extension.ifBlank { "mp3" })
                    roh.copyTo(ziel, overwrite = true); roh.delete()
                    Clip(ziel, dauer(ziel))
                }
            }.onFailure { Log.w(TAG, "Stimme für „$text“ nicht erzeugt", it) }.getOrNull()
        }
    }

    private fun dauer(d: File): Long = synchronized(dauern) { dauern[d.path] } ?: runCatching {
        MediaMetadataRetriever().run {
            try { setDataSource(d.path); extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L } finally { release() }
        }
    }.getOrDefault(0L).also { ms -> if (ms > 0) synchronized(dauern) { dauern[d.path] = ms } }

    /** Grobe Schätzung vor der Erzeugung: gut 14 Zeichen je Sekunde, im eingestellten Tempo. */
    fun schaetzeMs(context: Context, text: String): Long {
        val s = SecureSettings(context)
        val tempo = s.ttsTempo(s.ttsProvider).coerceIn(0.5f, 2f)
        return (text.length / 14.5f / tempo * 1000f + 350f).toLong()
    }

    /** Alle Texte eines Profils, die gesprochen werden können. */
    fun texte(p: AtemProfil): List<String> = buildList {
        add(EINATMEN); if (p.halt1 > 0 || p.halt2 > 0) add(HALTEN); add(AUSATMEN)
        if (p.intro) add(INTRO); if (p.outro) add(OUTRO)
        if (p.saetzeAn) { addAll(p.saetzeEin); addAll(p.saetzeAus) }
    }.distinct()

    /** Erzeugt alles vorab, z. B. gleich nach dem Speichern; meldet den Fortschritt (fertig, gesamt). */
    suspend fun vorbereiten(context: Context, p: AtemProfil, fortschritt: (Int, Int) -> Unit = { _, _ -> }): Map<String, Clip> {
        val t = texte(p)
        val ergebnis = HashMap<String, Clip>()
        t.forEachIndexed { i, text ->
            fortschritt(i, t.size)
            hole(context, text)?.let { ergebnis[text] = it }
        }
        fortschritt(t.size, t.size)
        return ergebnis
    }
}
