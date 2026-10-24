package de.frank.ziele.bruecke

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import de.frank.ziele.auth.CodexModel
import de.frank.ziele.auth.ReasoningEffort
import de.frank.ziele.data.Einstellungen
import de.frank.ziele.data.Kategorie
import de.frank.ziele.data.Kontext
import de.frank.ziele.data.Rueckmeldung
import de.frank.ziele.data.Sortierung
import de.frank.ziele.data.Vorschlag
import de.frank.ziele.data.Ziel
import de.frank.ziele.data.ZielStatus
import de.frank.ziele.data.ZieleRepository
import de.frank.ziele.data.sortiert
import de.frank.ziele.ki.Abgleich
import de.frank.ziele.ki.Auswertung
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Brücke zu Jarvis (de.frank.jarvis): Ziele lesen und schreiben und alle Funktionen der App steuern.
 * Geschützt durch die Signatur-Erlaubnis `de.frank.ziele.permission.JARVIS`.
 *
 * Aufruf über `ContentResolver.call(content://de.frank.ziele.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Fachliche Fehler kommen als `{"fehler": "..."}` zurück, nie als Ausnahme.
 *
 * Methoden: info, lesen, anlegen, aendern, loeschen, auswerten, auswertung_stand, uebernehmen, verwerfen,
 * kontext_lesen, kontext_ergaenzen, kontext_ersetzen, einstellungen.
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
        val ctx = context ?: return fehler("App nicht bereit.")
        val repo = ZieleRepository.get(ctx)
        val e = Einstellungen.get(ctx)
        return when (methode) {
            "info" -> {
                val alle = repo.zieleEinmal()
                JSONObject()
                    .put("app", "Ziele")
                    .put("heute", LocalDate.now().toString())
                    .put("ziele", alle.size)
                    .put("aktiv", alle.count { it.zustand == ZielStatus.AKTIV })
                    .put("erreicht", alle.count { it.zustand == ZielStatus.ERREICHT })
                    .put("modell", e.modell.label)
                    .put("auswertung_laeuft", JSONArray(Auswertung.stand.value.keys.toList()))
            }

            "lesen" -> {
                // Erledigt-Stand aus Geniale Aufgaben kurz nachziehen; hängt die App, liest Jarvis trotzdem rechtzeitig.
                kotlinx.coroutines.withTimeoutOrNull(2_500) { runCatching { Abgleich.stand(ctx) } }
                val alleV = repo.alleVorschlaegeEinmal().groupBy { it.zielId }
                val id = a.optLong("id", -1)
                if (id >= 0) {
                    val z = repo.ziel(id) ?: return fehler("Kein Ziel mit dieser id.")
                    val zj = zielJson(z, alleV[z.id].orEmpty(), voll = true, sortierung = sortierungAus(a, e))
                        .put("abgelehnt", JSONArray(repo.abgelehnt(z.id).map { JSONObject().put("titel", it.titel) }))
                        .put("gespraech", JSONArray(repo.rueckmeldungen(z.id).takeLast(20).map { JSONObject().put("von", if (it.vonFrank) "frank" else "agent").put("text", it.text) }))
                    return JSONObject().put("ziel", zj)
                }
                val status = a.optString("status", "aktiv").lowercase(Locale.GERMAN)
                val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
                var liste = repo.zieleEinmal()
                liste = when (status) {
                    "alle" -> liste
                    "erreicht" -> liste.filter { it.zustand == ZielStatus.ERREICHT }
                    "pausiert" -> liste.filter { it.zustand == ZielStatus.PAUSIERT }
                    else -> liste.filter { it.zustand == ZielStatus.AKTIV }
                }
                if (suche.isNotEmpty()) liste = liste.filter { suche in it.titel.lowercase(Locale.GERMAN) || suche in it.beschreibung.lowercase(Locale.GERMAN) }
                val mitVorschlaegen = a.optBoolean("mit_vorschlaegen", false)
                JSONObject()
                    .put("heute", LocalDate.now().toString())
                    .put("anzahl", liste.size)
                    .put("ziele", JSONArray(liste.map { zielJson(it, alleV[it.id].orEmpty(), voll = mitVorschlaegen, sortierung = sortierungAus(a, e)) }))
            }

            "anlegen" -> {
                val titel = a.optString("titel").trim()
                if (titel.isEmpty()) return fehler("Das Ziel braucht einen Titel.")
                val frist = if (a.has("frist")) datumAus(a.optString("frist")) else null
                if (a.has("frist") && frist == null && !ohne(a.optString("frist"))) return fehler("Frist nicht verstanden: ${a.optString("frist")}. Format JJJJ-MM-TT oder ohne.")
                val neu = Ziel(
                    titel = titel.take(120),
                    beschreibung = a.optString("beschreibung").trim(),
                    warum = a.optString("warum").trim(),
                    frist = frist,
                    symbol = a.optString("symbol").trim().ifEmpty { "🎯" },
                )
                val id = repo.neu(neu)
                val auswerten = a.optBoolean("auswerten", false)
                if (auswerten) Auswertung.starte(ctx, id, a.optString("wunsch"))
                JSONObject().put("ziel", zielJson(neu.copy(id = id), emptyList(), voll = false)).put("auswertung_gestartet", auswerten)
            }

            "aendern" -> {
                val alt = repo.ziel(a.optLong("id", -1)) ?: return fehler("Kein Ziel mit dieser id.")
                var frist = alt.frist
                if (a.has("frist")) {
                    frist = datumAus(a.optString("frist"))
                    if (frist == null && !ohne(a.optString("frist"))) return fehler("Frist nicht verstanden: ${a.optString("frist")}.")
                }
                val status = if (a.has("status")) statusAus(a.optString("status")) ?: return fehler("Status: aktiv, pausiert oder erreicht.") else alt.zustand
                val neu = alt.copy(
                    titel = a.optString("titel").trim().ifEmpty { alt.titel },
                    beschreibung = if (a.has("beschreibung")) a.optString("beschreibung").trim() else alt.beschreibung,
                    warum = if (a.has("warum")) a.optString("warum").trim() else alt.warum,
                    frist = frist,
                    symbol = a.optString("symbol").trim().ifEmpty { alt.symbol },
                    status = status.name,
                    erreichtAm = if (status == ZielStatus.ERREICHT) (alt.erreichtAm ?: System.currentTimeMillis()) else null,
                )
                repo.speichere(neu)
                JSONObject().put("ziel", zielJson(neu, repo.vorschlaege(neu.id), voll = false))
            }

            "loeschen" -> {
                val alt = repo.ziel(a.optLong("id", -1)) ?: return fehler("Kein Ziel mit dieser id.")
                Auswertung.abbrechen(alt.id)
                repo.loesche(alt)
                JSONObject().put("geloescht", JSONObject().put("id", alt.id).put("titel", alt.titel))
            }

            "auswerten" -> {
                val z = repo.ziel(a.optLong("id", -1)) ?: return fehler("Kein Ziel mit dieser id.")
                val gestartet = Auswertung.starte(ctx, z.id, a.optString("wunsch"))
                JSONObject().put("gestartet", gestartet)
                    .put("hinweis", if (gestartet) "Die Auswertung läuft im Hintergrund (Recherche und Aufgabenplan, meist zwei bis acht Minuten). Danach mit lesen und id abrufen."
                    else "Für dieses Ziel läuft schon eine Auswertung.")
            }

            "auswertung_stand" -> {
                val id = a.optLong("id", -1)
                JSONObject()
                    .put("laeuft", Auswertung.stand.value[id] != null)
                    .put("schritt", Auswertung.stand.value[id] ?: JSONObject.NULL)
                    .put("letzter_fehler", Auswertung.fehler.value[id] ?: JSONObject.NULL)
                    .put("ausgewertet_am", repo.ziel(id)?.ausgewertetAm ?: JSONObject.NULL)
            }

            "uebernehmen" -> {
                val v = try { Abgleich.uebernehmen(ctx, a.optLong("vorschlag_id", -1)) } catch (f: GenialeAufgaben.Fehler) { return fehler(f.message.orEmpty()) }
                JSONObject().put("vorschlag", vorschlagJson(v)).put("aufgabe_id", v.aufgabeId)
            }

            "verwerfen" -> {
                val v = repo.vorschlag(a.optLong("vorschlag_id", -1)) ?: return fehler("Kein Vorschlag mit dieser id.")
                val neu = v.copy(verworfen = a.optBoolean("verworfen", true))
                repo.speichere(neu)
                JSONObject().put("vorschlag", vorschlagJson(neu))
            }

            "ablehnen" -> {
                val v = repo.vorschlag(a.optLong("vorschlag_id", -1)) ?: return fehler("Kein Vorschlag mit dieser id.")
                val abgelehnt = Abgleich.gesperrt { repo.vorschlag(v.id)?.takeIf { it.aufgabeId == null }?.also { repo.ablehnen(it) } }
                    ?: return fehler("Dieser Vorschlag steht schon in Geniale Aufgaben und kann nicht abgelehnt werden.")
                JSONObject().put("abgelehnt", JSONObject().put("id", v.id).put("titel", v.titel))
            }

            "neue_aufgaben" -> {
                val z = repo.ziel(a.optLong("id", -1)) ?: return fehler("Kein Ziel mit dieser id.")
                val gestartet = Auswertung.starte(ctx, z.id, a.optString("wunsch"), Auswertung.Modus.NACHFUELLEN)
                JSONObject().put("gestartet", gestartet)
                    .put("hinweis", if (gestartet) "Ergänzende Recherche und neue Vorschläge für die freien Plätze (je Ziel ${Auswertung.PLAETZE_JE_KATEGORIE} notwendige und ${Auswertung.PLAETZE_JE_KATEGORIE} unterstützende) laufen im Hintergrund, Rückmeldungen fließen ein. Danach mit lesen und id abrufen."
                    else "Für dieses Ziel läuft schon eine Auswertung.")
            }

            "rueckmeldung" -> {
                val z = repo.ziel(a.optLong("id", -1)) ?: return fehler("Kein Ziel mit dieser id.")
                val text = a.optString("text").trim()
                if (text.isEmpty()) return fehler("Es fehlt der Text der Rückmeldung.")
                repo.neueRueckmeldung(Rueckmeldung(zielId = z.id, vonFrank = true, text = text))
                val gestartet = Auswertung.starte(ctx, z.id, modus = Auswertung.Modus.RUECKMELDUNG)
                JSONObject().put("gestartet", gestartet)
                    .put("hinweis", if (gestartet) "Der Aufgaben-Agent antwortet im Hintergrund; die Antwort steht danach unter gespraech (lesen mit id)."
                    else "Gespeichert; gerade läuft schon eine Auswertung, die Rückmeldung fließt in die nächste ein.")
            }

            "kontext_lesen" -> JSONObject().put("kontext", Kontext.lies(ctx))
            "kontext_ergaenzen" -> {
                val text = a.optString("text").trim()
                if (text.isEmpty()) return fehler("Es fehlt der Text, der in die Kontextdatei soll.")
                JSONObject().put("kontext", Kontext.ergaenze(ctx, text))
            }
            "kontext_ersetzen" -> {
                if (!a.has("text")) return fehler("Es fehlt der neue Text der Kontextdatei.")
                Kontext.schreibe(ctx, a.optString("text"))
                JSONObject().put("kontext", Kontext.lies(ctx))
            }

            "einstellungen" -> {
                // Unbekannte Namen nie still auf ein Ersatzmodell legen: lieber mit der Liste der gültigen Werte zurückmelden.
                if (a.has("modell")) {
                    val m = modellAus(a.optString("modell")) ?: return fehler("Modell nicht erkannt: ${a.optString("modell")}. Möglich: ${CodexModel.entries.joinToString { it.label }}.")
                    e.modell = m
                    e.denkstufe = m.normalizeEffort(e.denkstufe)
                }
                if (a.has("denkstufe")) {
                    val wert = a.optString("denkstufe").trim()
                    val d = ReasoningEffort.entries.firstOrNull { it.label.equals(wert, true) || it.anthropicLabel.equals(wert, true) || it.apiValue.equals(wert, true) || it.name.equals(wert, true) }
                        ?.takeIf { it in e.modell.supportedEfforts }
                        ?: return fehler("Denkstufe nicht erkannt oder für ${e.modell.label} nicht möglich: $wert. Möglich: ${e.modell.supportedEfforts.joinToString { it.titel(e.modell) }}.")
                    e.denkstufe = d
                }
                if (a.has("recherche")) e.recherche = a.optBoolean("recherche")
                if (a.has("jarvis_kontext")) e.jarvisKontext = a.optBoolean("jarvis_kontext")
                if (a.has("kontext_nutzen")) e.kontextNutzen = a.optBoolean("kontext_nutzen")
                if (a.has("sortierung")) e.sortierung = sortierungAus(a, e)
                if (a.has("modus")) a.optString("modus").lowercase(Locale.GERMAN).let { m -> e.modus = when (m) { "hell" -> "hell"; "dunkel" -> "dunkel"; else -> "system" } }
                JSONObject()
                    .put("modell", e.modell.label)
                    .put("modelle", JSONArray(CodexModel.entries.map { it.label }))
                    .put("denkstufe", e.denkstufe.titel(e.modell))
                    .put("denkstufen", JSONArray(e.modell.supportedEfforts.map { it.titel(e.modell) }))
                    .put("recherche", e.recherche)
                    .put("jarvis_kontext", e.jarvisKontext)
                    .put("kontext_nutzen", e.kontextNutzen)
                    .put("sortierung", e.sortierung.name.lowercase())
                    .put("sortierungen", JSONArray(Sortierung.entries.map { it.name.lowercase() }))
                    .put("modus", e.modus)
                    .put("menge", "fest: je Ziel ${Auswertung.PLAETZE_JE_KATEGORIE} notwendige und ${Auswertung.PLAETZE_JE_KATEGORIE} unterstützende Vorschläge")
            }

            else -> fehler("Unbekannte Methode: $methode")
        }
    }

    private fun sortierungAus(a: JSONObject, e: Einstellungen): Sortierung =
        a.optString("sortierung").takeIf { it.isNotBlank() }?.let { s -> Sortierung.entries.firstOrNull { it.name.equals(s.trim(), true) || it.anzeige.equals(s.trim(), true) } } ?: e.sortierung

    private fun zielJson(z: Ziel, vorschlaege: List<Vorschlag>, voll: Boolean, sortierung: Sortierung = Sortierung.WICHTIGKEIT): JSONObject {
        val aktiv = vorschlaege.filter { !it.verworfen }
        val notwendig = aktiv.filter { it.gruppe == Kategorie.NOTWENDIG }
        val o = JSONObject()
            .put("id", z.id)
            .put("titel", z.titel)
            .put("symbol", z.symbol)
            .put("beschreibung", z.beschreibung)
            .put("warum", z.warum)
            .put("frist", z.frist?.let { LocalDate.ofEpochDay(it).toString() } ?: JSONObject.NULL)
            .put("tage_bis_frist", z.frist?.let { it - LocalDate.now().toEpochDay() } ?: JSONObject.NULL)
            .put("status", z.zustand.name.lowercase())
            .put("strategie", z.strategie)
            .put("ausgewertet_am", z.ausgewertetAm ?: JSONObject.NULL)
            .put("vorschlaege", aktiv.size)
            .put("uebernommen", aktiv.count { it.aufgabeId != null })
            .put("erledigt", aktiv.count { it.erledigt })
            .put("notwendig_erledigt", "${notwendig.count { it.erledigt }} von ${notwendig.size}")
            .put("auswertung_laeuft", Auswertung.stand.value[z.id] ?: JSONObject.NULL)
        if (voll) {
            // Belege getrennt vom Text: Jarvis liest den Bericht vor, Adressen gehören nicht hinein.
            val recherche = de.frank.ziele.text.Quellen.teilen(z.bericht)
            o.put("bericht", recherche.text)
            o.put("quellen", JSONArray(recherche.quellen.map { JSONObject().put("titel", it.titel).put("seite", it.seite).put("url", it.url) }))
            o.put("sortierung", sortierung.name.lowercase())
            Kategorie.entries.forEach { k ->
                o.put(k.name.lowercase(), JSONArray(aktiv.filter { it.gruppe == k }.sortiert(sortierung).map(::vorschlagJson)))
            }
            o.put("verworfen", JSONArray(vorschlaege.filter { it.verworfen }.map { JSONObject().put("id", it.id).put("titel", it.titel) }))
        }
        return o
    }

    private fun vorschlagJson(v: Vorschlag) = JSONObject()
        .put("id", v.id)
        .put("schluessel", v.schluessel)
        .put("titel", v.titel)
        .put("text", v.text)
        .put("begruendung", v.begruendung)
        .put("kategorie", v.gruppe.name.lowercase())
        .put("wichtigkeit", v.wichtigkeit)
        .put("aufwand", v.aufwandKlasse.anzeige)
        .put("minuten", v.minuten)
        .put("phase", v.phase)
        .put("voraussetzungen", JSONArray(v.voraussetzungen))
        .put("erfolg", v.erfolg)
        .put("wiederkehrend", v.wiederkehrend)
        .put("in_aufgaben", v.aufgabeId != null)
        .put("aufgabe_id", v.aufgabeId ?: JSONObject.NULL)
        .put("erledigt", v.erledigt)
        .put("verworfen", v.verworfen)
        .put("angenommen", v.angenommen)

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    /** Genauer Name, API-Kennung oder eindeutiger Teil („Opus“, „Sol“, „Claude Sonnet“); sonst null. */
    private fun modellAus(wert: String): CodexModel? {
        val w = wert.trim()
        CodexModel.entries.firstOrNull { it.label.equals(w, true) || it.apiId.equals(w, true) || it.name.equals(w, true) }?.let { return it }
        val woerter = w.lowercase(Locale.GERMAN).split(Regex("[\\s\\-_.]+")).filter { it.length >= 3 && it !in setOf("claude", "gpt") }
        return CodexModel.entries.filter { m -> woerter.isNotEmpty() && woerter.all { it in m.label.lowercase(Locale.GERMAN) || it in m.apiId } }.singleOrNull()
    }

    private fun ohne(wert: String): Boolean = wert.trim().lowercase(Locale.GERMAN) in setOf("", "ohne", "kein", "keine", "null", "none")

    private fun datumAus(wert: String): Long? = runCatching { LocalDate.parse(wert.trim().take(10)).toEpochDay() }.getOrNull()

    private fun statusAus(wert: String): ZielStatus? = when (wert.trim().lowercase(Locale.GERMAN)) {
        "aktiv", "offen" -> ZielStatus.AKTIV
        "pausiert", "pause" -> ZielStatus.PAUSIERT
        "erreicht", "fertig", "erledigt" -> ZielStatus.ERREICHT
        else -> null
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
