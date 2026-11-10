package de.frank.jarvis.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Einstellungs-Umzug zwischen Franks eigenen Geräten (Handy → Tablet): alle Schalter, Stimmen, Modelle und Zugangsschlüssel
 * auf einmal, damit auf dem zweiten Gerät nichts abgetippt werden muss. Die verschlüsselten Speicher selbst lassen sich nicht
 * kopieren (ihr Schlüssel liegt im Keystore des Geräts), deshalb Wert für Wert:
 *
 * 1. Quelle: `adb shell am broadcast -a de.frank.jarvis.EINSTELLUNGEN_UMZUG -p de.frank.jarvis` (nur Shell und System, Recht DUMP)
 *    schreibt `files/umzug-export.json` in den privaten App-Speicher.
 * 2. Das Skript `werkzeuge/tablet-einrichten.sh` reicht die Datei per run-as direkt ans Ziel (`files/umzug.json`) und löscht sie
 *    an der Quelle. Sie liegt nie auf dem Rechner. Bleibt sie liegen, entfernt die App sie beim nächsten Start.
 * 3. Ziel: Beim App-Start übernimmt [uebernimm] die Werte in die verschlüsselten Speicher und löscht die Datei.
 *
 * Nicht mit umziehen: das Plugin-Geheimnis (jedes Gerät hat seine eigene Adresse), die ChatGPT-Anmeldung und die
 * Anthropic-Sitzungen (eine geteilte Sitzung kann das andere Gerät abmelden) und Zustände, die nur für ein Gerät stimmen.
 * Protokolliert wird nie ein Wert, nur die Anzahl.
 */
object Umzug {
    private const val TAG = "JarvisUmzug"
    private const val EXPORT = "umzug-export.json"
    private const val IMPORT = "umzug.json"
    private val SPEICHER = listOf(Einstellungen.NAME, SecureSettings.STORE_NAME, "jarvis_konten")
    private val NIE = setOf("geheimnis", "abfahrt_zustand", "chat_gpt_connected_at", "claude_ai_sitzung", "anthropic_console_sitzung", "letzte_sicherung")
    private val NIE_VORSILBE = listOf("routen_zaehler", "liveopt_")

    /**
     * Auf dem Tablet aus, auch wenn sie am Handy an sind: Die stündliche Tagesauswertung und das Lernen schreiben Franks
     * Gedächtnis (das Handy bleibt dafür die eine Quelle, das Tablet bekommt es über den Abgleich), die Losfahr-Meldung
     * verbraucht Routen-Kontingent, und auf „Hey Jarvis“ antworteten sonst beide Geräte zugleich. Frank kann jedes wieder einschalten.
     */
    val TABLET_AUS = listOf("auswertung_an", "auswertung_lernen", "abfahrt_an", "weckwort_an")

    private fun speicher(c: Context, name: String): SharedPreferences = EncryptedSharedPreferences.create(
        name, MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC), c.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun zieht(schluessel: String) = schluessel !in NIE && NIE_VORSILBE.none { schluessel.startsWith(it) }

    /** Schreibt alle umziehenden Werte nach `files/umzug-export.json`; liefert ihre Anzahl. */
    fun exportiere(c: Context): Int {
        val alles = JSONObject()
        var anzahl = 0
        for (name in SPEICHER) {
            val werte = JSONObject()
            for ((k, v) in speicher(c, name).all) {
                if (!zieht(k) || v == null) continue
                val eintrag = when (v) {
                    is String -> JSONObject().put("t", "s").put("v", v)
                    is Boolean -> JSONObject().put("t", "b").put("v", v)
                    is Int -> JSONObject().put("t", "i").put("v", v)
                    is Long -> JSONObject().put("t", "l").put("v", v)
                    is Float -> JSONObject().put("t", "f").put("v", v.toString())
                    is Set<*> -> JSONObject().put("t", "m").put("v", JSONArray(v.map { it.toString() }))
                    else -> null
                } ?: continue
                werte.put(k, eintrag)
                anzahl++
            }
            alles.put(name, werte)
        }
        val ziel = File(c.filesDir, EXPORT)
        val teil = File(c.filesDir, "$EXPORT.teil")
        teil.writeText(alles.toString())
        if (!teil.renameTo(ziel)) { teil.delete(); error("umzug-export.json ließ sich nicht schreiben") }
        return anzahl
    }

    /**
     * Beim App-Start, vor jedem anderen Zugriff auf die Einstellungen: übernimmt eine bereitliegende `files/umzug.json` und
     * entfernt einen liegen gebliebenen Export. True, wenn etwas übernommen wurde.
     */
    fun uebernimm(c: Context): Boolean {
        val export = File(c.filesDir, EXPORT)
        if (export.exists() && System.currentTimeMillis() - export.lastModified() > 10 * 60_000L) export.delete()
        val datei = File(c.filesDir, IMPORT)
        if (!datei.exists()) return false
        return try {
            val alles = JSONObject(datei.readText())
            var anzahl = 0
            for (name in SPEICHER) {
                val werte = alles.optJSONObject(name) ?: continue
                val e = speicher(c, name).edit()
                for (k in werte.keys()) {
                    if (!zieht(k)) continue
                    val eintrag = werte.optJSONObject(k) ?: continue
                    when (eintrag.optString("t")) {
                        "s" -> e.putString(k, eintrag.optString("v"))
                        "b" -> e.putBoolean(k, eintrag.optBoolean("v"))
                        "i" -> e.putInt(k, eintrag.optInt("v"))
                        "l" -> e.putLong(k, eintrag.optLong("v"))
                        "f" -> e.putFloat(k, eintrag.optString("v").toFloatOrNull() ?: continue)
                        "m" -> e.putStringSet(k, eintrag.optJSONArray("v")?.let { a -> (0 until a.length()).map { a.optString(it) }.toSet() } ?: continue)
                        else -> continue
                    }
                    anzahl++
                }
                if (name == Einstellungen.NAME && Geraet.istTablet) TABLET_AUS.forEach { e.putBoolean(it, false) }
                // commit statt apply: Die Datei wird gleich gelöscht, die Werte müssen vorher sicher auf der Platte stehen.
                check(e.commit()) { "Speicher $name ließ sich nicht schreiben" }
            }
            datei.delete()
            Log.i(TAG, "Einstellungs-Umzug übernommen: $anzahl Werte")
            Logbuch.system("Einstellungs-Umzug", "$anzahl Werte vom anderen Gerät übernommen")
            true
        } catch (e: Exception) {
            // Nur die Art des Fehlers: Die Meldung einer JSONException enthielte Teile der Datei, also Schlüssel.
            Log.w(TAG, "umzug.json nicht übernommen (${e.javaClass.simpleName})")
            Logbuch.fehler("Einstellungs-Umzug", null, e.javaClass.simpleName)
            datei.delete()
            false
        }
    }
}

/** Auslöser des Exports, nur für Shell und System (Manifest: Recht DUMP), siehe [Umzug]. */
class UmzugEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val wartend = goAsync()
        Thread {
            try {
                val anzahl = Umzug.exportiere(app)
                Logbuch.system("Einstellungs-Umzug", "$anzahl Werte für das andere Gerät bereitgelegt")
            } catch (e: Exception) {
                Logbuch.fehler("Einstellungs-Umzug: Export", null, e.javaClass.simpleName)
            } finally {
                wartend.finish()
            }
        }.start()
    }
}
