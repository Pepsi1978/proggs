package de.frank.jarvis.faehigkeit

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.gedaechtnis.Lagebild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Brücke von der App „Ziele“ (de.frank.ziele) zu Jarvis, geschützt durch `de.frank.jarvis.permission.BRUECKE` (Signatur).
 * Adresse `content://de.frank.jarvis.ziele`, Aufruf mit `call` und Bundle "json" hin und zurück, wie bei den anderen Brücken.
 *
 * - `kontext`: der kurze Startkontext aus dem Live-Gedächtnis (Regeln, Ziele, Wissen über Frank, Dienst, Tageslage).
 *   Ziele schickt ihn bei jeder Auswertung mit, damit die Aufgabenvorschläge auf Frank zugeschnitten sind.
 * - `zugang`: Groq-, Anthropic- und Stimmen-Schlüssel, damit Frank sie in Ziele nicht noch einmal eintippen muss.
 *   Nur Apps mit Franks Signatur kommen durch; die Schlüssel verlassen das Handy dabei nicht.
 */
class ZieleBruecke : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val antwort = runCatching {
            val ctx = context ?: return@runCatching JSONObject().put("fehler", "App nicht bereit.")
            val anfrage = extras?.getString("json")?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            when (method) {
                "kontext" -> {
                    val warten = anfrage.optLong("warte_ms", 1_500L).coerceIn(0L, 8_000L)
                    JSONObject().put("kontext", runBlocking(Dispatchers.IO) { Lagebild.kurz(ctx, warten) })
                }
                "zugang" -> {
                    val s = SecureSettings.get(ctx)
                    JSONObject()
                        .put("groq", s.groqApiKey)
                        .put("anthropic", s.anthropicApiKey)
                        .put("google_tts", s.googleTtsApiKey)
                        .put("gemini_frei", s.geminiTtsKeyFrei)
                        .put("gemini_bezahlt", s.geminiTtsKeyBezahlt)
                        .put("azure", s.azureTtsKey)
                        .put("azure_region", s.azureTtsRegion)
                        .put("qwen", s.qwenTtsApiKey)
                        .put("qwen_stimme", s.qwenTtsVoiceId)
                }
                else -> JSONObject().put("fehler", "Unbekannte Methode: $method")
            }
        }.getOrElse { fehler -> JSONObject().put("fehler", fehler.message ?: fehler.javaClass.simpleName) }
        return Bundle().apply { putString("json", antwort.toString()) }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
