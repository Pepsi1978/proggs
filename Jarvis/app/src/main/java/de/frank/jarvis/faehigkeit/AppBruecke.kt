package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import android.os.Bundle
import de.frank.jarvis.observability.Logbuch
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.json.JSONObject

/**
 * Gemeinsamer Aufruf der Jarvis-Brücken in Franks Apps (`ContentResolver.call` mit Bundle "json" hin und zurück).
 *
 * - Unstabiler Client: Stürzt die Partner-App mitten im Aufruf ab, beendet Android nicht auch Jarvis.
 * - Ein zweiter Versuch fängt den Moment ab, in dem Android die Partner-App gerade beendet oder kalt startet.
 *   Schreibende Methoden nur, wenn die App sicher noch nichts getan hat („App nicht bereit.“) — sonst entstünde ein Doppel.
 * - Mehrere Adressen: Ist eine Variante nicht installiert, kommt die nächste dran.
 * - Zeitlimit: Der Aufruf läuft auf einem eigenen Faden (lesend 6 s, schreibend 25 s, bei `warte_ms` entsprechend länger).
 *   Hängt die Partner-App, kommt der Aufrufer trotzdem frei; der hängende Binder-Aufruf läuft im Hintergrund aus.
 *   Lesende Aufrufe werden danach einmal wiederholt (nicht mit `warte_ms`); schreibende tragen [ZEITLIMIT], weil sie womöglich doch ankamen.
 */
object AppBruecke {
    private const val NICHT_BEREIT = "App nicht bereit."
    private const val LESEND_MS = 6_000L
    private const val SCHREIBEND_MS = 25_000L

    /** Eigene Fäden je Aufruf: Ein hängender Binder-Aufruf darf den nächsten nicht blockieren. */
    private val ausfuehrer = Executors.newCachedThreadPool { r -> Thread(r, "jarvis-bruecke").apply { isDaemon = true } }

    /** Unter dieser Adresse ist keine Brücke installiert. */
    private class KeineBruecke : Exception()

    fun rufe(context: Context, app: String, adressen: List<Uri>, methode: String, anfrage: JSONObject, lesend: Boolean): JSONObject {
        // Wartet die Partner-App selbst (warte_ms, etwa beim Biodaten-Abgleich), bekommt sie diese Zeit plus Reserve.
        val grenze = maxOf(if (lesend) LESEND_MS else SCHREIBEND_MS, anfrage.optLong("warte_ms", 0L) + 5_000L)
        val beginn = System.currentTimeMillis()
        val erste = rufeEinmal(context, app, adressen, methode, anfrage, grenze, lesend)
        val nochmal = erste.optString("fehler") == NICHT_BEREIT || (lesend && erste.optBoolean(VORUEBERGEHEND))
        if (!nochmal) return erste.apply { remove(VORUEBERGEHEND) }.also { gemessen(app, methode, beginn) }
        Thread.sleep(1_500)
        return rufeEinmal(context, app, adressen, methode, anfrage, grenze, lesend).apply { remove(VORUEBERGEHEND) }.also { gemessen(app, methode, beginn) }
    }

    fun rufe(context: Context, app: String, adresse: Uri, methode: String, anfrage: JSONObject, lesend: Boolean): JSONObject =
        rufe(context, app, listOf(adresse), methode, anfrage, lesend)

    /** Langsame Aufrufe ins Logbuch, damit sich hängende Partner-Apps später finden lassen. */
    private fun gemessen(app: String, methode: String, beginn: Long) {
        val ms = System.currentTimeMillis() - beginn
        if (ms >= 2_000) Logbuch.dauer("Brücke $app.$methode", ms)
    }

    private fun rufeEinmal(context: Context, app: String, adressen: List<Uri>, methode: String, anfrage: JSONObject, grenze: Long, lesend: Boolean): JSONObject {
        var letzter = fehler("$app ist nicht installiert oder zu alt (die Jarvis-Brücke fehlt).")
        for (adresse in adressen) {
            try {
                // Auch das Holen des Clients gehört in den Faden: Beim Kaltstart der Partner-App blockiert schon das.
                val auftrag = ausfuehrer.submit(Callable<Bundle?> {
                    val client = context.contentResolver.acquireUnstableContentProviderClient(adresse) ?: throw KeineBruecke()
                    try {
                        client.call(methode, null, Bundle().apply { putString("json", anfrage.toString()) })
                    } finally {
                        client.close()
                    }
                })
                val antwort = try {
                    auftrag.get(grenze, TimeUnit.MILLISECONDS)
                } catch (_: TimeoutException) {
                    Logbuch.fehler("Brücke Zeitlimit", null, "$app.$methode nach $grenze ms")
                    // Lesend: einmal wiederholen (meist ein Kaltstart, der zweite Aufruf trifft den warmen Prozess).
                    // Nicht bei warte_ms: Die Partner-App hat schon lange gearbeitet (ein zweiter Durchgang überschritte die Relay-Grenze
                    // von 75 s und stieße womöglich eine zweite, bezahlte Recherche an).
                    if (lesend && anfrage.optLong("warte_ms", 0L) > 0L) return fehler("$app antwortet nicht (Zeitlimit).")
                    if (lesend) return fehler("$app antwortet nicht (Zeitlimit).").put(VORUEBERGEHEND, true)
                    // Schreibend kein zweiter Versuch: Der Binder-Aufruf läuft weiter und kommt womöglich doch an.
                    // [ZEITLIMIT] bleibt in der Antwort, damit der Doppel-Schutz der Aufrufer greift.
                    return fehler("$app antwortet nicht rechtzeitig (Zeitlimit). Der Auftrag wurde womöglich trotzdem ausgeführt; erst nachlesen, nicht neu anlegen.").put(ZEITLIMIT, true)
                } catch (e: ExecutionException) {
                    throw e.cause ?: e
                }
                return antwort?.getString("json")?.let(::JSONObject) ?: fehler("$app hat nicht geantwortet.").put(VORUEBERGEHEND, true)
            } catch (_: KeineBruecke) {
                // null: Unter dieser Adresse ist keine Brücke installiert.
            } catch (_: SecurityException) {
                return fehler("$app verweigert den Zugriff. Beide Apps müssen mit demselben Schlüssel signiert sein; danach Jarvis neu installieren.")
            } catch (_: IllegalArgumentException) {
                // Diese Variante kennt die Brücke nicht: nächste Adresse versuchen.
            } catch (_: InterruptedException) {
                // Der Aufrufer wurde abgebrochen: nicht weiter warten.
                Thread.currentThread().interrupt()
                return fehler("$app: Aufruf abgebrochen.")
            } catch (e: Exception) {
                // Meist DeadObjectException: Die Partner-App wurde mitten im Aufruf beendet.
                Logbuch.fehler("Brücke $app.$methode", e)
                letzter = fehler("$app ist nicht erreichbar: ${e.message ?: e.javaClass.simpleName}").put(VORUEBERGEHEND, true)
            }
        }
        return letzter
    }

    private const val VORUEBERGEHEND = "_voruebergehend"

    /** Schreibender Aufruf lief ins Zeitlimit: Er kam womöglich trotzdem an (bleibt in der Antwort stehen). */
    const val ZEITLIMIT = "_zeitlimit"

    private fun fehler(text: String) = JSONObject().put("fehler", text)
}
