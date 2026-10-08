package de.frank.genialeideen.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import de.frank.genialeideen.GenialeIdeenApplication
import de.frank.genialeideen.auth.ChatTurn
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.data.local.Kategorieart
import de.frank.genialeideen.data.local.alleKategorieIds
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.text.UmlautKorrektur
import de.frank.genialeideen.ui.IdeenViewModel
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Brücke zur App „Jarvis“ (de.frank.jarvis): Ideen lesen UND schreiben. Geschützt durch die
 * Signatur-Erlaubnis `de.frank.jarvis.permission.BRUECKE` — nur Apps mit demselben Signierschlüssel.
 *
 * Aufruf: `ContentResolver.call(content://de.frank.genialeideen.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Fachliche Fehler kommen als `{"fehler": "..."}` zurück.
 *
 * Methoden: `lesen`, `idee`, `anlegen`, `aendern`, `loeschen`, `kategorien`.
 *
 * Eine neue Idee wird sofort mit dem gesprochenen Text gespeichert. Danach glättet die KI den Text und
 * findet einen Titel — mit denselben Anweisungen wie der Erfassen-Bildschirm. Das Original bleibt als
 * `originalText` erhalten. Ohne KI-Zugang bleibt der gesprochene Text mit einem Titel aus den ersten Worten.
 */
class JarvisBruecke : ContentProvider() {
    private val nebenbei = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val antwort = runCatching {
            val anfrage = extras?.getString("json")?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            runBlocking(Dispatchers.IO) { verarbeite(method, anfrage) }
        }.getOrElse { fehler -> JSONObject().put("fehler", fehler.message ?: fehler.javaClass.simpleName) }
        return Bundle().apply { putString("json", antwort.toString()) }
    }

    private suspend fun verarbeite(methode: String, a: JSONObject): JSONObject {
        val c = (context?.applicationContext as? GenialeIdeenApplication)?.container ?: return fehler("App nicht bereit.")
        val repo = c.ideenRepository
        val kategorien = repo.alleKategorien().first()
        return when (methode) {
            "kategorien" -> JSONObject().put("kategorien", JSONArray(kategorien.map { JSONObject().put("id", it.id).put("name", it.name).put("art", it.art.name.lowercase()) }))

            "lesen" -> {
                val status = a.optString("status", "offen").lowercase(Locale.GERMAN)
                val suche = a.optString("suche").trim()
                val kategorie = a.optString("kategorie").trim().lowercase(Locale.GERMAN)
                var liste = (if (suche.isNotEmpty()) repo.suche(suche) else repo.alleIdeen().first()).filter { it.status != IdeenStatus.ENTWURF.name }
                liste = when (status) {
                    "umgesetzt" -> liste.filter { it.status == IdeenStatus.UMGESETZT.name }
                    "alle" -> liste
                    else -> liste.filter { it.status == IdeenStatus.OFFEN.name }
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
                val id = repo.lege(titel = titelEingabe.ifEmpty { ersatzTitel(roh) }, text = roh, kategorieId = kategorieId)
                if (a.optBoolean("verbessern", true)) verbessere(c, id, roh, titelFehlt = titelEingabe.isEmpty())
                JSONObject().put("idee", alsJson(repo.lade(id) ?: return fehler("Speichern fehlgeschlagen."), repo.alleKategorien().first(), Int.MAX_VALUE))
                    .put("hinweis", if (a.optBoolean("verbessern", true) && c.codexAuthManager.isConnected) "Text und Titel werden gerade von der KI geglättet." else "Gespeichert wie gesprochen.")
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
                }
                JSONObject().put("idee", alsJson(repo.lade(idee.id) ?: idee, repo.alleKategorien().first(), Int.MAX_VALUE))
            }

            "loeschen" -> {
                val idee = repo.lade(a.optLong("id", -1)) ?: return fehler("Keine Idee mit dieser id.")
                repo.loesche(idee)
                JSONObject().put("geloescht", alsJson(idee, kategorien, 200))
            }

            else -> fehler("Unbekannte Methode: $methode")
        }
    }

    /** Glättet Text und Titel im Hintergrund, wie „Korrektur“ und der Titelvorschlag im Erfassen-Bildschirm. */
    private fun verbessere(c: AppContainer, id: Long, roh: String, titelFehlt: Boolean) {
        if (!c.codexAuthManager.isConnected) return
        nebenbei.launch {
            runCatching {
                val modell = CodexModel.fromLabel(c.settings.model)
                val stufe = ReasoningEffort.fromLabel(c.settings.reasoning)
                val sauber = UmlautKorrektur.korrigiere(
                    c.codexAuthManager.streamChat(IdeenViewModel.KORREKTUR, listOf(ChatTurn("user", roh)), modell, stufe).trim(),
                )
                var titel: String? = null
                if (titelFehlt) {
                    titel = dreiWoerter(UmlautKorrektur.korrigiere(
                        c.codexAuthManager.streamChat(IdeenViewModel.TITEL, listOf(ChatTurn("user", roh.take(4000))), modell, stufe),
                    )).takeIf { it.isNotBlank() }
                }
                val repo = c.ideenRepository
                val idee = repo.lade(id) ?: return@launch
                // Nur überschreiben, wenn der Text seit dem Anlegen unverändert ist — sonst gewinnt die Bearbeitung von Hand.
                if (idee.text == roh && sauber.isNotBlank()) {
                    c.database.ideenDao().aktualisieren(idee.copy(text = sauber, titel = titel ?: idee.titel, originalText = roh, geaendertAm = System.currentTimeMillis()))
                } else if (titel != null && idee.titel == ersatzTitel(roh)) {
                    repo.aendere(idee, titel, idee.text)
                }
            }
        }
    }

    /** Bestehende Kategorie nach Name; ein unbekannter Name legt eine neue (mentale) Kategorie an. Leer = keine. */
    private suspend fun kategorieFuer(name: String, vorhandene: List<KategorieEntity>, c: AppContainer): Long? {
        val gesucht = name.trim()
        if (gesucht.isEmpty() || gesucht.lowercase(Locale.GERMAN) in setOf("ohne", "keine")) return null
        vorhandene.firstOrNull { it.name.equals(gesucht, ignoreCase = true) }?.let { return it.id }
        vorhandene.firstOrNull { gesucht.lowercase(Locale.GERMAN) in it.name.lowercase(Locale.GERMAN) }?.let { return it.id }
        return c.ideenRepository.legeKategorieAn(gesucht, Kategorieart.MENTAL)
    }

    private fun dreiWoerter(roh: String): String = roh.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trim()
        .trim('"', '\'', '„', '“', '”', '«', '»').split(Regex("\\s+")).filter(String::isNotBlank).take(3).joinToString(" ")
        .trimEnd('.', ',', ';', ':', '!').trim()

    private fun ersatzTitel(text: String): String = dreiWoerter(text).ifBlank { "Neue Idee" }

    private fun alsJson(i: IdeeEntity, kategorien: List<KategorieEntity>, laenge: Int): JSONObject {
        fun tag(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val namen = i.alleKategorieIds().mapNotNull { id -> kategorien.firstOrNull { it.id == id }?.name }
        return JSONObject()
            .put("id", i.id)
            .put("titel", i.titel)
            .put("text", if (i.text.length > laenge) i.text.take(laenge) + " …" else i.text)
            .put("gekuerzt", i.text.length > laenge)
            .put("status", if (i.status == IdeenStatus.UMGESETZT.name) "umgesetzt" else "offen")
            .put("kategorien", JSONArray(namen))
            .put("angelegt", tag(i.angelegtAm))
            .put("geaendert", tag(i.geaendertAm))
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
