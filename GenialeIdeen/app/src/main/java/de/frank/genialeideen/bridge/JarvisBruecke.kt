package de.frank.genialeideen.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import de.frank.genialeideen.GenialeIdeenApplication
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.data.local.Kategorieart
import de.frank.genialeideen.data.local.alleKategorieIds
import de.frank.genialeideen.di.AppContainer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import de.frank.genialeideen.auth.ChatTurn
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.EigeneAnfrage
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.text.Kurzfassung
import de.frank.genialeideen.text.UmlautKorrektur
import de.frank.genialeideen.ui.IdeenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Brücke zur App „Jarvis“ (de.frank.jarvis): Ideen lesen UND schreiben. Geschützt durch die
 * Signatur-Erlaubnis `de.frank.jarvis.permission.BRUECKE` — nur Apps mit demselben Signierschlüssel.
 *
 * Aufruf: `ContentResolver.call(content://de.frank.genialeideen.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Fachliche Fehler kommen als `{"fehler": "..."}` zurück.
 *
 * Methoden: `lesen`, `idee`, `anlegen`, `aendern`, `loeschen`, `kategorien`, `uebersicht` (Reiter und Seitenleiste),
 * `verbessern` (wie der Knopf „Mit KI verbessern“) und `rueckgaengig` (holt die Fassung davor zurück).
 *
 * Eine neue Idee wird sofort mit dem gesprochenen Text gespeichert. Danach glättet die KI den Text und
 * findet einen Titel — mit denselben Anweisungen wie der Erfassen-Bildschirm. Das Original bleibt als
 * `originalText` erhalten. Ohne KI-Zugang bleibt der gesprochene Text mit einem Titel aus den ersten Worten.
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
        val c = (context?.applicationContext as? GenialeIdeenApplication)?.warteAufContainer() ?: return fehler("App nicht bereit.")
        val repo = c.ideenRepository
        val kategorien = repo.alleKategorien().first()
        return when (methode) {
            "kategorien" -> JSONObject().put("kategorien", JSONArray(kategorien.map { JSONObject().put("id", it.id).put("name", it.name).put("art", it.art.name.lowercase()) }))

            "lesen" -> {
                val status = a.optString("status", "offen").lowercase(Locale.GERMAN)
                val suche = a.optString("suche").trim()
                val kategorie = a.optString("kategorie").trim().lowercase(Locale.GERMAN)
                val bestand = if (suche.isNotEmpty()) repo.suche(suche) else repo.alleIdeen().first()
                // Entwürfe nur auf ausdrücklichen Wunsch: Ohne neue Angaben bleibt alles wie bisher.
                var liste = if (status == "entwurf" || a.optBoolean("entwuerfe")) bestand else bestand.filter { it.status != IdeenStatus.ENTWURF.name }
                liste = when (status) {
                    "entwurf" -> liste.filter { it.status == IdeenStatus.ENTWURF.name }
                    // Wie der Reiter „Offen“ der App: ohne die angenommenen.
                    "nur_offen" -> liste.filter { it.status == IdeenStatus.OFFEN.name }
                    "umgesetzt" -> liste.filter { it.status == IdeenStatus.UMGESETZT.name }
                    "angenommen" -> liste.filter { it.status == IdeenStatus.ANGENOMMEN.name }
                    "alle" -> liste
                    // Angenommene gehören zu den noch nicht umgesetzten — Jarvis soll sie weiter sehen.
                    else -> liste.filter { it.status == IdeenStatus.OFFEN.name || it.status == IdeenStatus.ANGENOMMEN.name }
                }
                if (kategorie.isNotEmpty()) {
                    val ids = kategorien.filter { kategorie in it.name.lowercase(Locale.GERMAN) }.map { it.id }.toSet()
                    liste = liste.filter { idee -> idee.alleKategorieIds().any { it in ids } }
                }
                liste = liste.sortedWith(compareBy({ it.status }, { it.reihenfolge }))
                val grenze = a.optInt("limit", 50).coerceIn(1, 300)
                val voll = a.optBoolean("volltext", false)
                JSONObject().put("anzahl", liste.size).put("ideen", JSONArray(liste.take(grenze).map { alsJson(it, kategorien, if (voll) Int.MAX_VALUE else 400) }))
            }

            "idee" -> {
                val idee = repo.lade(a.optLong("id", -1)) ?: return fehler("Keine Idee mit dieser id.")
                val gespraech = repo.nachrichtenEinmal(idee.id)
                JSONObject().put("idee", alsJson(idee, kategorien, Int.MAX_VALUE))
                    .put("gespraech", JSONArray(gespraech.takeLast(20).map { JSONObject().put("rolle", it.rolle).put("text", it.text.take(4000)) }))
            }

            "anlegen" -> {
                val roh = a.optString("text").trim()
                if (roh.isEmpty()) return fehler("Die Idee braucht einen Text.")
                val titelEingabe = a.optString("titel").trim()
                val kategorieId = kategorieFuer(a.optString("kategorie"), kategorien, c)
                val id = repo.lege(titel = titelEingabe.ifEmpty { JarvisGlaettung.ersatzTitel(roh) }, text = roh, kategorieId = kategorieId)
                if (a.optBoolean("verbessern", true)) JarvisGlaettung.starte(c, requireNotNull(context), id, roh, titelFehlt = titelEingabe.isEmpty())
                JSONObject().put("idee", alsJson(repo.lade(id) ?: return fehler("Speichern fehlgeschlagen."), repo.alleKategorien().first(), Int.MAX_VALUE))
                    .put("hinweis", if (a.optBoolean("verbessern", true) && c.codexAuthManager.kannDenken(de.frank.genialeideen.auth.CodexModel.fromLabel(c.settings.model))) "Text und Titel werden gerade von der KI geglättet." else "Gespeichert wie gesprochen.")
            }

            "aendern" -> {
                var idee = repo.lade(a.optLong("id", -1)) ?: return fehler("Keine Idee mit dieser id.")
                if (a.has("text") || a.has("titel")) {
                    val titel = a.optString("titel").trim().ifEmpty { idee.titel }
                    val text = if (a.has("text")) a.optString("text").trim() else idee.text
                    if (text.isEmpty()) return fehler("Der Text darf nicht leer sein.")
                    repo.aendere(idee, titel, text)
                    idee = repo.lade(idee.id) ?: idee
                }
                if (a.has("kategorie")) {
                    repo.setzeKategorie(idee.id, kategorieFuer(a.optString("kategorie"), kategorien, c))
                    idee = repo.lade(idee.id) ?: idee
                }
                when (a.optString("status").lowercase(Locale.GERMAN)) {
                    "umgesetzt" -> repo.setzeStatus(idee, IdeenStatus.UMGESETZT)
                    "offen" -> repo.setzeStatus(idee, IdeenStatus.OFFEN)
                    "angenommen" -> repo.setzeStatus(idee, IdeenStatus.ANGENOMMEN)
                }
                JSONObject().put("idee", alsJson(repo.lade(idee.id) ?: idee, repo.alleKategorien().first(), Int.MAX_VALUE))
            }

            "uebersicht" -> {
                // Dieselben Listen wie Reiter und Seitenleiste der App, mit denselben Zählungen.
                val alle = repo.alleIdeen().first()
                fun zahl(status: IdeenStatus) = alle.count { it.status == status.name }
                JSONObject()
                    .put("angenommen", zahl(IdeenStatus.ANGENOMMEN))
                    .put("offen", zahl(IdeenStatus.OFFEN))
                    .put("umgesetzt", zahl(IdeenStatus.UMGESETZT))
                    .put("entwuerfe", zahl(IdeenStatus.ENTWURF))
                    .put("alle", alle.size)
            }

            "verbessern" -> {
                val idee = repo.lade(a.optLong("id", -1)) ?: return fehler("Keine Idee mit dieser id.")
                if (idee.text.isBlank()) return fehler("Die Idee hat keinen Text zum Verbessern.")
                if (!c.codexAuthManager.kannDenken(CodexModel.fromLabel(c.settings.model))) return fehler("Geniale Ideen ist nicht bei der KI angemeldet; Verbessern geht gerade nicht.")
                val nurKurz = a.optBoolean("nur_kurzfassung")
                // Läuft im Hintergrund zu Ende, auch wenn Jarvis nicht so lange wartet; ein zweiter Aufruf hängt sich an.
                val auftrag = synchronized(verbesserungen) {
                    verbesserungen[idee.id]?.takeIf { it.isActive } ?: nebenbei.async { verbessere(c, idee, nurKurz) }.also { verbesserungen[idee.id] = it }
                }
                val fertig = withTimeoutOrNull(a.optLong("warte_ms", 50_000L).coerceIn(1_000L, 60_000L)) { auftrag.await() }
                    ?: return JSONObject().put("laeuft", true).put("hinweis", "Die KI verbessert noch; das Ergebnis steht gleich in der App. Mit der id nachlesen, nicht erneut verbessern.")
                fertig.optString("fehler").takeIf { it.isNotEmpty() }?.let { return fehler(it) }
                JSONObject().put("idee", alsJson(repo.lade(idee.id) ?: idee, kategorien, Int.MAX_VALUE))
                    .put("hinweis", if (nurKurz) "Die Kurzfassung ist neu erzeugt." else "Beide Fassungen sind verbessert.")
            }

            "rueckgaengig" -> {
                val idee = repo.lade(a.optLong("id", -1)) ?: return fehler("Keine Idee mit dieser id.")
                if (!repo.nimmVerbesserungZurueck(idee.id)) return fehler("Bei dieser Idee gibt es keine vorige Fassung zum Zurückholen.")
                JSONObject().put("idee", alsJson(repo.lade(idee.id) ?: idee, kategorien, Int.MAX_VALUE)).put("hinweis", "Die vorige Fassung ist wieder da.")
            }

            "loeschen" -> {
                val idee = repo.lade(a.optLong("id", -1)) ?: return fehler("Keine Idee mit dieser id.")
                repo.loesche(idee)
                JSONObject().put("geloescht", alsJson(idee, kategorien, 200))
            }

            else -> fehler("Unbekannte Methode: $methode")
        }
    }

    /** Wie „Mit KI verbessern“ in der Einzelansicht; mit [nurKurz] bleibt der Text und nur die Kurzfassung wird neu. */
    private suspend fun verbessere(c: AppContainer, idee: IdeeEntity, nurKurz: Boolean): JSONObject = try {
        val modell = CodexModel.fromLabel(c.settings.model)
        val stufe = ReasoningEffort.fromLabel(c.settings.reasoning)
        val text = if (nurKurz) idee.text else UmlautKorrektur.korrigiere(
            withContext(EigeneAnfrage) {
                c.codexAuthManager.streamChat(IdeenViewModel.VERBESSERUNG, listOf(ChatTurn("user", idee.text)), modell, stufe)
            }.trim(),
        )
        if (text.isBlank()) {
            fehler("Es kam keine verbesserte Fassung zurück.")
        } else {
            val kurz = runCatching { Kurzfassung.erzeuge(c.codexAuthManager, modell, stufe, text) }.getOrDefault("")
            when {
                nurKurz && kurz.isBlank() -> fehler("Es kam keine Kurzfassung zurück.")
                c.ideenRepository.setzeVerbessert(idee.id, idee.text, text, kurz) -> JSONObject()
                else -> fehler("Die Idee wurde inzwischen geändert — bitte noch einmal verbessern.")
            }
        }
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        fehler(e.message ?: e.javaClass.simpleName)
    }

    /** Bestehende Kategorie nach Name; ein unbekannter Name legt eine neue (mentale) Kategorie an. Leer = keine. */
    private suspend fun kategorieFuer(name: String, vorhandene: List<KategorieEntity>, c: AppContainer): Long? {
        val gesucht = name.trim()
        if (gesucht.isEmpty() || gesucht.lowercase(Locale.GERMAN) in setOf("ohne", "keine")) return null
        vorhandene.firstOrNull { it.name.equals(gesucht, ignoreCase = true) }?.let { return it.id }
        vorhandene.firstOrNull { gesucht.lowercase(Locale.GERMAN) in it.name.lowercase(Locale.GERMAN) }?.let { return it.id }
        return c.ideenRepository.legeKategorieAn(gesucht, Kategorieart.MENTAL)
    }

    private fun alsJson(i: IdeeEntity, kategorien: List<KategorieEntity>, laenge: Int): JSONObject {
        fun tag(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val namen = i.alleKategorieIds().mapNotNull { id -> kategorien.firstOrNull { it.id == id }?.name }
        return JSONObject()
            .put("id", i.id)
            .put("titel", i.titel)
            .put("text", if (i.text.length > laenge) i.text.take(laenge) + " …" else i.text)
            .put("gekuerzt", i.text.length > laenge)
            .put("kurzfassung", i.kurzfassung)
            .put("rueckgaengig_moeglich", i.vorigerText != null)
            .put("status", when (i.status) {
                IdeenStatus.UMGESETZT.name -> "umgesetzt"
                IdeenStatus.ANGENOMMEN.name -> "angenommen"
                IdeenStatus.ENTWURF.name -> "entwurf"
                else -> "offen"
            })
            .put("kategorien", JSONArray(namen))
            .put("angelegt", tag(i.angelegtAm))
            .put("geaendert", tag(i.geaendertAm))
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    private companion object {
        /** Überlebt den einzelnen Aufruf: Die Verbesserung läuft zu Ende, auch wenn Jarvis nicht mehr wartet. */
        val nebenbei = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val verbesserungen = mutableMapOf<Long, Deferred<JSONObject>>()
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
