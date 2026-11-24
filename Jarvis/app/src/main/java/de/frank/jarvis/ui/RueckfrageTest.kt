package de.frank.jarvis.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.frank.jarvis.data.Gespraeche
import de.frank.jarvis.faehigkeit.ProgrammierChat
import de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gerätetest über adb, nur für Shell und System (Manifest: Recht DUMP): blendet eine oder mehrere Rückfragen wie die Review-Frage
 * am Ende eines Auftrags ein, ohne Programmier-Sitzung. Sie gehören zum Gespräch „Rückfrage-Test“ und werden lokal beantwortet
 * (Kennung „HF…“ wie im Werkzeugbau), nichts geht an den Server. Damit lässt sich prüfen, dass Rückfragen ein laufendes Diktat nicht beenden.
 *
 * `adb shell am broadcast -a de.frank.jarvis.RUECKFRAGE_TEST -p de.frank.jarvis --ei anzahl 3 --el abstand_ms 400`
 */
class RueckfrageTest : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val anzahl = intent.getIntExtra("anzahl", 1).coerceIn(1, 5)
        val abstand = intent.getLongExtra("abstand_ms", 400L).coerceIn(0L, 10_000L)
        val ende = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                val prog = Register.alle(app).filterIsInstance<ProgrammierFaehigkeit>().firstOrNull() ?: return@launch
                val gespraech = testGespraech(app)
                if (gespraech.isEmpty()) { Logbuch.system("Rückfrage-Test", "kein Testgespräch"); return@launch }
                Logbuch.system("Rückfrage-Test", "$anzahl Rückfrage(n), Abstand $abstand ms")
                repeat(anzahl) { i ->
                    if (i > 0) delay(abstand)
                    val frageId = "HFTEST" + System.currentTimeMillis().toString(36) + i
                    val fragen = JSONArray()
                        .put(frage("Soll Jarvis auch auf dem Tablet installiert werden? (Test ${i + 1})", "Tablet", "Ja", "Nein"))
                        .put(frage("Zusätzliches Code-Review? (Test ${i + 1})", "Review", "Nein", "Ja, Standard", "Ja, sehr ausführlich (bis 100 Befunde)"))
                    val frage = JSONObject().put("frage_id", frageId).put("fragen", fragen).put("bis", 0).put("lokal", true)
                    val bau = JSONObject().put("id", "TEST").put("name", "Rückfrage-Test").put("gespraech", gespraech).put("aufgabe", "Gerätetest: Rückfragen während eines Diktats")
                    prog.helferFrageAnzeigen(frageId, bau, frage, JSONObject())
                }
            } finally {
                ende.finish()
            }
        }
    }

    private fun frage(text: String, kopf: String, vararg optionen: String) = JSONObject().put("question", text).put("header", kopf)
        .put("options", JSONArray().apply { optionen.forEach { put(JSONObject().put("label", it).put("description", "")) } })

    /** Ein Gespräch für alle Testläufe, damit die Seitenleiste nicht volläuft. */
    private fun testGespraech(c: Context): String {
        val prefs = c.getSharedPreferences("rueckfrage_test", Context.MODE_PRIVATE)
        prefs.getString("gespraech", null)?.takeIf { Gespraeche.laden(c, it) != null }?.let { return it }
        val neu = ProgrammierChat.neuesGespraech(c, "Rückfrage-Test", "Gerätetest: Rückfragen während eines Diktats (adb).", ueberPlugin = false)
        if (neu.isNotEmpty()) prefs.edit().putString("gespraech", neu).apply()
        return neu
    }
}
