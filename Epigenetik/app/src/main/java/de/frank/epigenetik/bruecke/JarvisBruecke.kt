package de.frank.epigenetik.bruecke

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import de.frank.epigenetik.BuildConfig
import de.frank.epigenetik.auth.CodexAuthManager
import de.frank.epigenetik.data.Bericht
import de.frank.epigenetik.data.Kategorie
import de.frank.epigenetik.data.Recherche
import de.frank.epigenetik.data.Repository
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.data.Status
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.data.Zielkatalog
import de.frank.epigenetik.data.normalisiere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Brücke zur App „Jarvis“ (de.frank.jarvis). Geschützt durch die Signatur-Erlaubnis
 * `de.frank.jarvis.permission.BRUECKE` — nur Apps mit demselben Signierschlüssel.
 *
 * Aufruf: `ContentResolver.call(content://de.frank.epigenetik.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Fachliche Fehler kommen als `{"fehler": "..."}` zurück.
 *
 * Methoden:
 * - `info`: Umfang der Bibliothek, KI-Verbindung.
 * - `lesen` {name, typ?, kurz?, recherchieren_wenn_fehlt?, warte_ms?}: vorhandene Auswertung als vollständiger Bericht.
 *   Fehlt sie und ist `recherchieren_wenn_fehlt` gesetzt, wird sie angestoßen.
 * - `recherchieren` {name, typ: mittel|wirkung|ziel|frage, kategorie?: supplement|nahrung|verhalten, neu?, warte_ms?}
 * - `status` {id}
 * - `liste` {typ?, nur_stack?, nur_favoriten?}
 * - `schalter` {schalter}: welche recherchierten Mittel auf einen Schalter wirken (ohne neue Recherche).
 * - `stack`: der Stack des Nutzers mit der letzten Stack-Analyse.
 * - `stack_setzen` {name, an}: ein Mittel in den Stack aufnehmen oder entfernen.
 *
 * Recherchen dauern 1–4 Minuten. Mit `warte_ms` (höchstens 60 000) wartet die Brücke auf das Ergebnis;
 * sonst kommt sofort `{"status": "laeuft", "id": …}` zurück und Jarvis liest später mit `status` oder `lesen`.
 */
class JarvisBruecke : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val antwort = runCatching {
            val anfrage = extras?.getString("json")?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            runBlocking(Dispatchers.IO) { verarbeite(method, anfrage) }
        }.getOrElse { fehler -> JSONObject().put("fehler", fehler.message ?: fehler.javaClass.simpleName) }
        return Bundle().apply { putString("json", antwort.toString()) }
    }

    private suspend fun verarbeite(methode: String, a: JSONObject): JSONObject {
        val ctx = context?.applicationContext ?: return fehler("App nicht bereit.")
        val repo = Repository.get(ctx)
        return when (methode) {
            "info" -> {
                val alle = repo.dao.alleJetzt()
                JSONObject()
                    .put("app", "Epigenetik ${BuildConfig.VERSION_NAME}")
                    .put("ki_verbunden", CodexAuthManager(ctx).isConnected)
                    .put("mittel", alle.count { it.typEnum == Typ.MITTEL && it.fertig })
                    .put("wirkungen", alle.count { it.typEnum == Typ.WIRKUNG && it.fertig })
                    .put("ziele", alle.count { it.typEnum == Typ.ZIEL && it.fertig })
                    .put("laufend", alle.count { it.offen })
                    .put("im_stack", alle.count { it.imStack })
                    .put("schalter_vokabular", JSONArray(Schalterkatalog.alle.map { "${it.id}: ${it.name}" }))
            }

            "lesen" -> {
                val name = a.optString("name").trim().ifBlank { return fehler("Name fehlt.") }
                val typ = a.optString("typ").takeIf { it.isNotBlank() }?.let { Typ.von(it) }
                val r = repo.findeIrgendwas(name, typ)
                when {
                    r != null && r.offen -> warte(repo, r, a)
                    r != null -> ergebnis(repo, r, a.optBoolean("kurz"))
                    a.optBoolean("recherchieren_wenn_fehlt") -> starte(repo, a.put("typ", (typ ?: rateTyp(name)).name))
                    else -> JSONObject().put("gefunden", false).put("name", name)
                        .put("vorhanden", JSONArray(repo.dao.alleJetzt().filter { it.fertig && it.typEnum != Typ.STACK }.map { it.name }.take(200)))
                        .put("hinweis", "Noch nicht recherchiert. Mit recherchieren oder recherchieren_wenn_fehlt starten.")
                }
            }

            "recherchieren" -> starte(repo, a)

            "status" -> {
                val r = repo.dao.hole(a.optLong("id", -1)) ?: return fehler("Keine Recherche mit dieser id.")
                if (r.offen) warte(repo, r, a) else ergebnis(repo, r, a.optBoolean("kurz"))
            }

            "liste" -> {
                val typ = a.optString("typ").takeIf { it.isNotBlank() }?.let { Typ.von(it) }
                val liste = repo.dao.alleJetzt()
                    .filter { typ == null || it.typEnum == typ }
                    .filter { !a.optBoolean("nur_stack") || it.imStack }
                    .filter { !a.optBoolean("nur_favoriten") || it.favorit }
                    .sortedWith(compareByDescending<Recherche> { it.potenzial }.thenBy { it.name })
                JSONObject().put("anzahl", liste.size).put("eintraege", JSONArray(liste.map(::kurzeintrag)))
            }

            "schalter" -> {
                val s = Schalterkatalog.zu(a.optString("schalter")) ?: Schalterkatalog.alle.firstOrNull {
                    normalisiere(it.name).contains(normalisiere(a.optString("schalter")))
                } ?: return fehler("Unbekannter Schalter. Bekannt: ${Schalterkatalog.alle.joinToString { it.id }}")
                val treffer = repo.dao.mittelFuer(s.id)
                JSONObject().put("schalter", s.id).put("name", s.name).put("guenstig", s.guenstig.anzeige).put("info", s.info)
                    .put("mittel", JSONArray(treffer.map {
                        JSONObject().put("name", it.name).put("kategorie", Kategorie.von(it.kategorie).anzeige).put("richtung", it.richtung)
                            .put("staerke", it.staerke).put("evidenz", it.evidenz).put("mechanismus", it.mechanismus).put("im_stack", it.imStack)
                    }))
                    .put("hinweis", if (treffer.isEmpty()) "Noch kein recherchiertes Mittel wirkt auf diesen Schalter. Eine Wirkungs-Recherche findet passende." else "")
            }

            "stack" -> {
                val alle = repo.dao.alleJetzt()
                val analyse = alle.firstOrNull { it.typEnum == Typ.STACK }
                JSONObject()
                    .put("mittel", JSONArray(alle.filter { it.imStack }.map(::kurzeintrag)))
                    .put("analyse", analyse?.takeIf { it.fertig }?.let { Bericht.text(it) } ?: "Noch keine Stack-Analyse.")
            }

            "stack_setzen" -> {
                val r = repo.findeMittel(a.optString("name")) ?: return fehler("Mittel nicht gefunden – erst recherchieren.")
                repo.setzeStack(r, a.optBoolean("an", true))
                JSONObject().put("ok", true).put("name", r.name).put("im_stack", a.optBoolean("an", true))
            }

            else -> fehler("Unbekannte Methode $methode.")
        }
    }

    private suspend fun starte(repo: Repository, a: JSONObject): JSONObject {
        val name = a.optString("name").trim().ifBlank { return fehler("Name fehlt.") }
        val typ = a.optString("typ").takeIf { it.isNotBlank() }?.let { Typ.von(it) } ?: rateTyp(name)
        val kategorie = a.optString("kategorie").takeIf { it.isNotBlank() }?.let { Kategorie.von(it) }
        val r = repo.anlegen(typ, name, kategorie, quelle = "jarvis", neu = a.optBoolean("neu"))
        return if (r.fertig && !a.optBoolean("neu")) ergebnis(repo, r, a.optBoolean("kurz")).put("schon_vorhanden", true)
        else warte(repo, r, a)
    }

    /** Wartet bis zu warte_ms (höchstens 60 s) auf das Ergebnis, sonst meldet sie „läuft“. */
    private suspend fun warte(repo: Repository, r: Recherche, a: JSONObject): JSONObject {
        val bis = System.currentTimeMillis() + a.optLong("warte_ms", 0).coerceIn(0, 60_000)
        var stand = r
        while (stand.offen && System.currentTimeMillis() < bis) {
            delay(1_000)
            stand = repo.dao.hole(r.id) ?: return fehler("Die Recherche wurde gelöscht.")
        }
        return if (stand.offen) JSONObject().put("id", stand.id).put("name", stand.name).put("typ", stand.typEnum.anzeige)
            .put("status", "laeuft").put("hinweis", "Die Recherche läuft (meist 1–4 Minuten). Später mit status {id: ${stand.id}} oder lesen abrufen.")
        else ergebnis(repo, stand, a.optBoolean("kurz"))
    }

    private suspend fun ergebnis(repo: Repository, r: Recherche, kurz: Boolean): JSONObject {
        val o = JSONObject().put("gefunden", true).put("id", r.id).put("name", r.name).put("typ", r.typEnum.anzeige)
            .put("status", r.statusEnum.name.lowercase())
        if (r.statusEnum == Status.FEHLER) return o.put("fehler", r.fehler)
        if (r.typEnum == Typ.MITTEL) o.put("kategorie", r.kategorieEnum.anzeige).put("potenzial", r.potenzial).put("evidenz", r.evidenz).put("im_stack", r.imStack)
        return o.put("stand", java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.GERMANY).format(java.util.Date(r.aktualisiert)))
            .put("bericht", Bericht.text(r, repo.dao.wirkungenVon(r.id), kurzfassung = kurz))
    }

    private fun kurzeintrag(r: Recherche): JSONObject = JSONObject().put("id", r.id).put("name", r.name).put("typ", r.typEnum.anzeige)
        .put("status", r.statusEnum.name.lowercase()).apply {
            if (r.typEnum == Typ.MITTEL) put("kategorie", r.kategorieEnum.anzeige).put("potenzial", r.potenzial)
            if (r.kurz.isNotBlank()) put("kurz", r.kurz)
            if (r.imStack) put("im_stack", true)
        }

    /** Ohne Angabe: Ziel-Vorlagen und Wirkungs-Wörter erkennen, sonst ein Mittel. */
    private fun rateTyp(name: String): Typ = Eingabe.rateTyp(name)

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}

/** Erkennt aus dem Text, was gemeint ist – die Oberfläche zeigt den Vorschlag, der Nutzer kann umschalten. */
object Eingabe {
    private val wirkungsWoerter = listOf(
        "aktivier", "hemm", "senk", "steiger", "erhoeh", "anschalt", "abschalt", "einschalt", "ausschalt", "foerder", "unterdrueck",
        "methylierung", "acetylierung", "sirtuin", "autophag", "mitophag", "telomer", "entzuendung", "signalweg", " gen ", " gene ",
        "mtor", "ampk", "nrf2", "nf kb", "foxo", "p53", "klotho", "bdnf", "seneszen", "epigenetische uhr", "nad+",
    )
    private val zielWoerter = listOf("ziel", "laenger leben", "langlebig", "praevention", "vorbeug", "gesund bleiben", "schutz vor", "fitter", "besser schlafen")

    fun rateTyp(text: String): Typ {
        val k = " " + normalisiere(text) + " "
        if (text.trim().endsWith("?") || k.startsWith(" was ") || k.startsWith(" wie ") || k.startsWith(" welche ") || k.startsWith(" warum ") || k.startsWith(" ist ") || k.startsWith(" kann ")) return Typ.FRAGE
        if (Zielkatalog.alle.any { normalisiere(it.name) == k.trim() || normalisiere(it.id) == k.trim() }) return Typ.ZIEL
        if (zielWoerter.any { k.contains(it) }) return Typ.ZIEL
        if (wirkungsWoerter.any { k.contains(it) }) return Typ.WIRKUNG
        if (Schalterkatalog.zu(text.trim()) != null) return Typ.WIRKUNG
        return Typ.MITTEL
    }

    private val verhaltensWoerter = listOf("training", "fasten", "sauna", "schlaf", "meditation", "sport", "laufen", "kaelte", "eisbad", "atem", "yoga", "bewegung", "sonne", "intervall", "hiit", "krafttraining", "spazier", "wandern")
    private val nahrungsWoerter = listOf("brokkoli", "gruener tee", "kurkuma", "beeren", "nuesse", "olivenoel", "knoblauch", "zwiebel", "spinat", "kaffee", "kakao", "lachs", "sprossen", "pilze", "aepfel", "trauben", "soja", "leinsamen", "tomate", "granatapfel", "rotwein", "ingwer", "kohl", "fisch", "gemuese", "obst")

    fun rateKategorie(text: String): Kategorie {
        val k = normalisiere(text)
        return when {
            verhaltensWoerter.any { k.contains(it) } -> Kategorie.VERHALTEN
            nahrungsWoerter.any { k.contains(it) } && !k.contains("extrakt") && !k.contains("kapsel") -> Kategorie.NAHRUNG
            else -> Kategorie.SUPPLEMENT
        }
    }
}
