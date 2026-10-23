package de.frank.ziele.bruecke

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONObject

/**
 * Aufruf der Brücken in Franks anderen Apps (`ContentResolver.call` mit Bundle "json" hin und zurück), wie in Jarvis:
 * unstabiler Client (ein Absturz der Partner-App reißt Ziele nicht mit), eigener Faden mit Zeitlimit,
 * lesende Aufrufe einmal wiederholt (Kaltstart der Partner-App). Schreibende nie doppelt.
 */
object AppBruecke {
    private const val LESEND_MS = 6_000L
    private const val SCHREIBEND_MS = 20_000L
    private val ausfuehrer = Executors.newCachedThreadPool { r -> Thread(r, "ziele-bruecke").apply { isDaemon = true } }

    private class KeineBruecke : Exception()

    /** Aus einer Coroutine: auf dem IO-Dispatcher und unterbrechbar. */
    suspend fun rufe(context: Context, app: String, adresse: Uri, methode: String, anfrage: JSONObject, lesend: Boolean): JSONObject =
        runInterruptible(Dispatchers.IO) {
            val grenze = maxOf(if (lesend) LESEND_MS else SCHREIBEND_MS, anfrage.optLong("warte_ms", 0L) + 5_000L)
            val erste = einmal(context, app, adresse, methode, anfrage, grenze, lesend)
            if (lesend && erste.optBoolean(VORUEBERGEHEND)) {
                Thread.sleep(1_200)
                einmal(context, app, adresse, methode, anfrage, grenze, lesend)
            } else erste
        }.apply { remove(VORUEBERGEHEND) }

    private fun einmal(context: Context, app: String, adresse: Uri, methode: String, anfrage: JSONObject, grenze: Long, lesend: Boolean): JSONObject {
        return try {
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
                return if (lesend) fehler("$app antwortet nicht (Zeitlimit).").put(VORUEBERGEHEND, true)
                else fehler("$app antwortet nicht rechtzeitig. Der Auftrag kam womöglich trotzdem an.").put(ZEITLIMIT, true)
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
            antwort?.getString("json")?.let(::JSONObject) ?: fehler("$app hat nicht geantwortet.").put(VORUEBERGEHEND, true)
        } catch (_: KeineBruecke) {
            fehler("$app ist nicht installiert oder zu alt (die Brücke fehlt).").put(NICHT_INSTALLIERT, true)
        } catch (_: SecurityException) {
            fehler("$app verweigert den Zugriff. Beide Apps müssen mit demselben Schlüssel signiert sein.")
        } catch (_: IllegalArgumentException) {
            fehler("$app ist nicht installiert oder zu alt (die Brücke fehlt).").put(NICHT_INSTALLIERT, true)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            fehler("$app: Aufruf abgebrochen.")
        } catch (e: Exception) {
            Log.w("ZieleBruecke", "$app.$methode", e)
            fehler("$app ist nicht erreichbar: ${e.message ?: e.javaClass.simpleName}").put(VORUEBERGEHEND, true)
        }
    }

    private const val VORUEBERGEHEND = "_voruebergehend"
    const val ZEITLIMIT = "_zeitlimit"
    const val NICHT_INSTALLIERT = "_nicht_installiert"

    private fun fehler(text: String) = JSONObject().put("fehler", text)
}
