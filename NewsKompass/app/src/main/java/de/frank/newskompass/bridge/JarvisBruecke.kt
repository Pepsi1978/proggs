package de.frank.newskompass.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.data.Geschichten
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.news.NewsRecherche
import de.frank.newskompass.news.Zeitplan
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Brücke zur App „Jarvis“ (de.frank.jarvis): Jarvis liest den Bestand, nur lesend. Nur Apps mit demselben
 * Signierschlüssel kommen durch; die Prüfung steht im Code ([nurEigeneSignatur]), nicht als Erlaubnis im Manifest.
 *
 * Aufruf: `ContentResolver.call(content://de.frank.newskompass.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Fachliche Fehler kommen als `{"fehler": "..."}` zurück.
 *
 * - `lesen` (`rubrik` optional, `tage` 1–10, Vorgabe 2): Rubriken mit Zeitplan und Stand (letzter Lauf, fälliger Termin,
 *   Fehler) und die Meldungen der regulären Blöcke, neueste Ausgabe zuerst; dieselbe Geschichte aus mehreren Läufen nur einmal.
 * - `meldung` (`id` = „Ausgabe/Meldung“): eine Meldung vollständig mit allen Absätzen und Quellen.
 *
 * Die Brücke stößt nie einen Lauf an und holt nichts nach: Das Sammeln um 5 und 17 Uhr bleibt allein Sache des Zeitplans.
 */
class JarvisBruecke : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        nurEigeneSignatur()
        val antwort = runCatching {
            val anfrage = extras?.getString("json")?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            runBlocking(Dispatchers.IO) { withTimeoutOrNull(5_000L) { verarbeite(method, anfrage) } ?: fehler("News-Kompass antwortet nicht rechtzeitig.") }
        }.getOrElse { f -> fehler(f.message ?: f.javaClass.simpleName) }
        return Bundle().apply { putString("json", antwort.toString()) }
    }

    private suspend fun verarbeite(methode: String, a: JSONObject): JSONObject {
        val app = context?.applicationContext as? NewsApplication ?: return fehler("App nicht bereit.")
        app.speicher.bereit()
        return when (methode) {
            "lesen" -> lesen(app, a)
            "meldung" -> meldung(app, a.optString("id"))
            else -> fehler("Unbekannte Methode „$methode“.")
        }
    }

    private suspend fun lesen(app: NewsApplication, a: JSONObject): JSONObject {
        val jetzt = ZonedDateTime.now()
        val tage = a.optInt("tage", 2).coerceIn(1, 10)
        val stand = app.einstellungen.stand.value
        val themen = stand.themen.filter { it.text.isNotBlank() }
        // Für den Stand je Rubrik mindestens zwei Tage zurück, auch wenn weniger Meldungen gewünscht sind.
        val ausgaben = app.speicher.ausgabenSeit(System.currentTimeMillis() - maxOf(tage, 2) * 86_400_000L)
            .filter { it.istRegulaer }.sortedByDescending { it.erstelltUm }
        val faellig = Zeitplan.faelligZwischen(themen, jetzt.minusHours(48), jetzt)
        val rubrik = a.optString("rubrik").trim().lowercase()

        val rubriken = JSONArray()
        themen.forEach { thema ->
            val name = thema.kopfzeile()
            val bloecke = ausgaben.mapNotNull { aus -> aus.bloecke.firstOrNull { it.themaId == thema.id && it.frage == null }?.let { aus to it } }
            val gelaufen = bloecke.firstOrNull { NewsRecherche.istGelaufen(it.second) }
            val letzterFehler = bloecke.firstOrNull()?.takeIf { !NewsRecherche.istGelaufen(it.second) }?.second?.fehler
            rubriken.put(
                JSONObject()
                    .put("id", thema.id)
                    .put("name", name)
                    .put("titel", bloecke.firstOrNull()?.second?.titel ?: name)
                    .put("thema", thema.text.take(300))
                    .put("uhrzeiten", JSONArray(thema.uhrzeiten.map { "%02d:%02d".format(it / 60, it % 60) }))
                    .put("letzter_lauf", gelaufen?.first?.erstelltUm ?: 0L)
                    .put("faellig_seit", faellig[thema.id] ?: 0L)
                    .put("naechster", Zeitplan.naechsterTermin(thema, jetzt)?.toInstant()?.toEpochMilli() ?: 0L)
                    .put("nichts_neues", gelaufen?.second?.let { it.meldungen.isEmpty() } ?: false)
                    .apply { letzterFehler?.let { put("fehler", it.take(300)) } },
            )
        }

        val grenze = System.currentTimeMillis() - tage * 86_400_000L
        val meldungen = JSONArray()
        val gesehen = mutableListOf<Meldung>()
        ausgaben.filter { it.erstelltUm >= grenze }.forEach { aus ->
            aus.bloecke.filter { it.frage == null }.forEach { block ->
                val thema = themen.firstOrNull { it.id == block.themaId }
                val name = thema?.kopfzeile() ?: block.titel
                if (rubrik.isNotEmpty() && !passt(rubrik, name, block.titel, thema?.text.orEmpty())) return@forEach
                block.meldungen.forEach { m ->
                    // Neueste Fassung einer Geschichte gewinnt (die Ausgaben sind neueste zuerst sortiert).
                    if (gesehen.any { Geschichten.gleich(it.titel, it.quellen.firstOrNull(), it.istUpdate, m.titel, m.quellen.firstOrNull(), m.istUpdate) }) return@forEach
                    gesehen += m
                    meldungen.put(
                        JSONObject()
                            .put("id", "${aus.id}/${m.id}")
                            .put("rubrik_id", block.themaId)
                            .put("rubrik", name)
                            .put("titel", m.titel)
                            .put("kurz", m.absaetze.firstOrNull().orEmpty().take(400))
                            .put("wann", m.wann)
                            .put("update", m.istUpdate)
                            .put("quelle", m.quellen.firstOrNull().orEmpty())
                            .put("ausgabe_um", aus.erstelltUm),
                    )
                }
            }
        }
        return JSONObject()
            .put("rubriken", rubriken)
            .put("meldungen", meldungen)
            .put("zeitplan_aktiv", stand.zeitplanAktiv)
            .put("lauf_wartet", app.einstellungen.hatOffeneLaeufe)
            .put("jetzt", System.currentTimeMillis())
    }

    private suspend fun meldung(app: NewsApplication, id: String): JSONObject {
        val ausgabeId = id.substringBefore('/', "")
        val meldungId = id.substringAfter('/', "")
        if (ausgabeId.isEmpty() || meldungId.isEmpty()) return fehler("Ungültige Meldungs-id „$id“.")
        val aus: Ausgabe = app.speicher.ausgabe(ausgabeId, merken = false) ?: return fehler("Ausgabe nicht gefunden oder nicht lesbar.")
        val themen = app.einstellungen.stand.value.themen
        aus.bloecke.forEach { block ->
            block.meldungen.firstOrNull { it.id == meldungId }?.let { m ->
                return JSONObject().put(
                    "meldung",
                    JSONObject()
                        .put("id", id)
                        .put("rubrik", themen.firstOrNull { it.id == block.themaId }?.kopfzeile() ?: block.titel)
                        .put("titel", m.titel)
                        .put("absaetze", JSONArray(m.absaetze))
                        .put("quellen", JSONArray(m.quellen))
                        .put("wann", m.wann)
                        .put("update", m.istUpdate)
                        .put("ausgabe_um", aus.erstelltUm),
                )
            }
        }
        return fehler("Meldung nicht gefunden.")
    }

    /** „Fußball“ findet „Fußball kompakt“, „KI“ findet „KI Nachrichten“; verglichen werden auch die Wörter des Themas. */
    private fun passt(suche: String, vararg felder: String): Boolean {
        val norm = { s: String -> s.lowercase().replace("ß", "ss").replace("-", " ") }
        val s = norm(suche).removeSuffix("news").removeSuffix("nachrichten").trim().ifEmpty { norm(suche) }
        return felder.any { f ->
            val feld = norm(f)
            feld.contains(s) || s.split(' ').filter { it.length >= 2 }.let { w -> w.isNotEmpty() && w.all { Regex("\\b${Regex.escape(it)}").containsMatchIn(feld) } }
        }
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}

private fun ContentProvider.nurEigeneSignatur() {
    val pm = context?.packageManager ?: throw SecurityException("App nicht bereit.")
    val uid = Binder.getCallingUid()
    if (uid != Process.myUid() && pm.checkSignatures(uid, Process.myUid()) != PackageManager.SIGNATURE_MATCH)
        throw SecurityException("Nur Apps mit demselben Signierschlüssel.")
}
