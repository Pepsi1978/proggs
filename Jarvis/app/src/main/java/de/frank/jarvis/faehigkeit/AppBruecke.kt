package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import android.os.Bundle
import org.json.JSONObject

/**
 * Gemeinsamer Aufruf der Jarvis-Brücken in Franks Apps (`ContentResolver.call` mit Bundle "json" hin und zurück).
 *
 * - Unstabiler Client: Stürzt die Partner-App mitten im Aufruf ab, beendet Android nicht auch Jarvis.
 * - Ein zweiter Versuch fängt den Moment ab, in dem Android die Partner-App gerade beendet oder kalt startet.
 *   Schreibende Methoden nur, wenn die App sicher noch nichts getan hat („App nicht bereit.“) — sonst entstünde ein Doppel.
 * - Mehrere Adressen: Ist eine Variante nicht installiert, kommt die nächste dran.
 */
object AppBruecke {
    private const val NICHT_BEREIT = "App nicht bereit."

    fun rufe(context: Context, app: String, adressen: List<Uri>, methode: String, anfrage: JSONObject, lesend: Boolean): JSONObject {
        val erste = rufeEinmal(context, app, adressen, methode, anfrage)
        val nochmal = erste.optString("fehler") == NICHT_BEREIT || (lesend && erste.optBoolean(VORUEBERGEHEND))
        if (!nochmal) return erste.apply { remove(VORUEBERGEHEND) }
        Thread.sleep(1_500)
        return rufeEinmal(context, app, adressen, methode, anfrage).apply { remove(VORUEBERGEHEND) }
    }

    fun rufe(context: Context, app: String, adresse: Uri, methode: String, anfrage: JSONObject, lesend: Boolean): JSONObject =
        rufe(context, app, listOf(adresse), methode, anfrage, lesend)

    private fun rufeEinmal(context: Context, app: String, adressen: List<Uri>, methode: String, anfrage: JSONObject): JSONObject {
        var letzter = fehler("$app ist nicht installiert oder zu alt (die Jarvis-Brücke fehlt).")
        for (adresse in adressen) {
            try {
                // null: Unter dieser Adresse ist keine Brücke installiert.
                val client = context.contentResolver.acquireUnstableContentProviderClient(adresse) ?: continue
                val antwort = try {
                    client.call(methode, null, Bundle().apply { putString("json", anfrage.toString()) })
                } finally {
                    client.close()
                }
                return antwort?.getString("json")?.let(::JSONObject) ?: fehler("$app hat nicht geantwortet.").put(VORUEBERGEHEND, true)
            } catch (_: SecurityException) {
                return fehler("$app verweigert den Zugriff. Beide Apps müssen mit demselben Schlüssel signiert sein; danach Jarvis neu installieren.")
            } catch (_: IllegalArgumentException) {
                // Diese Variante kennt die Brücke nicht: nächste Adresse versuchen.
            } catch (e: Exception) {
                // Meist DeadObjectException: Die Partner-App wurde mitten im Aufruf beendet.
                letzter = fehler("$app ist nicht erreichbar: ${e.message ?: e.javaClass.simpleName}").put(VORUEBERGEHEND, true)
            }
        }
        return letzter
    }

    private const val VORUEBERGEHEND = "_voruebergehend"

    private fun fehler(text: String) = JSONObject().put("fehler", text)
}
