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
 * - `lesen` (`rubrik` optional, `tage` 1–31, Vorgabe 2, `aktuell`): Rubriken mit Zeitplan und Stand (letzter Lauf, fälliger Termin,
 *   Fehler) und die Meldungen der regulären Blöcke, neueste Ausgabe zuerst; dieselbe Geschichte aus mehreren Läufen nur einmal.
 *   `aktuell` = nur heutige Ausgaben und je Rubrik die neueste (bis `tage` zurück); gelesen wird nur, bis jede Rubrik ihre hat.
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
            // Zeitlimit (meist Kaltstart): als vorübergehend markiert, dann versucht Jarvis' AppBruecke es einmal neu.
            runBlocking(Dispatchers.IO) { withTimeoutOrNull(5_000L) { verarbeite(method, anfrage) } ?: fehler("News-Kompass antwortet nicht rechtzeitig.").put("_voruebergehend", true) }
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
        val nun = System.currentTimeMillis()
        val tage = a.optInt("tage", 2).coerceIn(1, 31)
        val aktuell = a.optBoolean("aktuell")
        val heute = jetzt.toLocalDate().atStartOfDay(jetzt.zone).toInstant().toEpochMilli()
        val stand = app.einstellungen.stand.value
        val themen = stand.themen.filter { it.text.isNotBlank() }
        // Neueste zuerst; der Stand je Rubrik braucht immer die letzten zwei Tage. Mit `aktuell` endet das Lesen, sobald jede Rubrik
        // ihre neueste gelaufene Ausgabe hat. Ohne Zwischenspeicher der Anzeige (merken = false), damit die App ihren behält.
        val fenster = nun - maxOf(tage, 2) * 86_400_000L
        val offen = themen.map { it.id }.toMutableSet()
        val neuesteJeThema = HashMap<String, String>()
        val ausgaben = mutableListOf<Ausgabe>()
        for (e in app.speicher.index.value.filter { it.erstelltUm >= fenster }.sortedByDescending { it.erstelltUm }) {
            if (aktuell && offen.isEmpty() && e.erstelltUm < minOf(heute, nun - 48 * 3_600_000L)) break
            val aus = app.speicher.ausgabe(e.id, merken = false) ?: continue
            if (!aus.istRegulaer) continue
            ausgaben += aus
            aus.bloecke.filter { NewsRecherche.istGelaufen(it) }.forEach { b -> if (offen.remove(b.themaId)) neuesteJeThema[b.themaId] = aus.id }
        }
        val faellig = Zeitplan.faelligZwischen(themen, jetzt.minusHours(48), jetzt)
        val rubrik = a.optString("rubrik").trim()

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

        val grenze = nun - tage * 86_400_000L
        val meldungen = JSONArray()
        val gesehen = mutableListOf<Meldung>()
        ausgaben.forEach { aus ->
            aus.bloecke.filter { it.frage == null }.forEach { block ->
                val dabei = if (aktuell) aus.erstelltUm >= heute || neuesteJeThema[block.themaId] == aus.id else aus.erstelltUm >= grenze
                if (!dabei) return@forEach
                val thema = themen.firstOrNull { it.id == block.themaId }
                val name = thema?.kopfzeile() ?: block.titel
                if (rubrik.isNotEmpty() && !passt(rubrik, name, block.titel, thema?.text.orEmpty())) return@forEach
                block.meldungen.forEach { m ->
                    // Neueste Fassung einer Geschichte gewinnt (die Ausgaben sind neueste zuerst sortiert).
                    if (gesehen.any { gleicheGeschichte(it, m) }) return@forEach
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
            .put("jetzt", nun)
    }

    /**
     * Wie [Geschichten.gleich], aber nur echte Artikeladressen (mindestens zwei Pfadteile oder ein Slug mit Ziffer/Bindestrich):
     * Rubrikseiten wie „bvb.de/news“ würden sonst verschiedene Meldungen zusammenwerfen.
     */
    private fun gleicheGeschichte(a: Meldung, b: Meldung): Boolean {
        val qa = a.quellen.firstOrNull()?.takeIf(::artikel)
        val qb = b.quellen.firstOrNull()?.takeIf(::artikel)
        return Geschichten.gleich(a.titel, qa, a.istUpdate, b.titel, qb, b.istUpdate)
    }

    private fun artikel(url: String): Boolean {
        val pfad = url.substringBefore('#').substringBefore('?').substringAfter("://", url).substringAfter('/', "").trimEnd('/')
        val teile = pfad.split('/').filter { it.isNotEmpty() }
        return teile.size >= 2 || teile.lastOrNull()?.let { it.length >= 8 && it.any { c -> c.isDigit() || c == '-' } } == true
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

    /**
     * „Fußball“ findet „Fußball kompakt“, „KI“ findet „KI Nachrichten“; verglichen werden auch die Wörter des Themas. Jedes Suchwort
     * muss als Wort vorkommen: kurze (unter vier Zeichen, etwa „KI“) ganz, längere als Wortanfang („Fußball“ in „Fußballnews“),
     * damit „KI“ nicht „Kicker“ oder „Kinder“ trifft.
     */
    private fun passt(suche: String, vararg felder: String): Boolean {
        val norm = { s: String -> s.lowercase().replace("ß", "ss").replace("-", " ") }
        val worte = norm(suche).split(Regex("\\s+")).filter { it.length >= 2 && it != "news" && it != "nachrichten" && it != "meldungen" }
            .ifEmpty { norm(suche).split(Regex("\\s+")).filter { it.length >= 2 } }
        if (worte.isEmpty()) return true
        return felder.any { f ->
            val feld = norm(f)
            worte.all { w -> Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(w) + if (w.length < 4) "(?![\\p{L}\\p{N}])" else "").containsMatchIn(feld) }
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
