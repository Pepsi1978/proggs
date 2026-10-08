package de.frank.aufgaben.bruecke

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import de.frank.aufgaben.auth.CodexAuthManager
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Einstellungen
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Schritt
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.data.Wiederholung
import de.frank.aufgaben.data.kurzerTitel
import de.frank.aufgaben.ki.AufgabenKi
import de.frank.aufgaben.ki.notTitel
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Brücke zur App „Jarvis“ (de.frank.jarvis): lesen UND schreiben. Geschützt durch die Signatur-Erlaubnis
 * `de.frank.aufgaben.permission.JARVIS` — nur Apps mit demselben Signierschlüssel kommen durch.
 *
 * Aufruf über `ContentResolver.call(content://de.frank.aufgaben.jarvis, methode, null, Bundle("json" → Anfrage))`,
 * Antwort im Bundle unter "json". Methoden: info, lesen, anlegen, aendern, erledigen, loeschen.
 * Fachliche Fehler kommen als `{"fehler": "..."}` zurück, nie als Ausnahme.
 *
 * Alle Änderungen laufen über das [AufgabenRepository] — so ziehen Erinnerungen, Widget und Wecker nach.
 * Die Regeln (Tag ohne Priorität → Mittel, Uhrzeit nur mit Tag, Erinnerung nur mit Uhrzeit) sind dieselben wie im Editor.
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
        val ctx = context ?: return fehler("App nicht bereit.")
        val repo = AufgabenRepository.get(ctx)
        val einstellungen = Einstellungen.get(ctx)
        return when (methode) {
            "info" -> JSONObject()
                .put("app", "Geniale Aufgaben")
                .put("heute", LocalDate.now().toString())
                .put("vorlauf_standard", einstellungen.vorlaufStandard)
                .put("offen", repo.alleEinmal().count { !it.erledigt })

            "lesen" -> {
                val heute = Tage.heute()
                val bereich = a.optString("bereich", "offen").lowercase(Locale.GERMAN)
                val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
                val alle = repo.alleEinmal()
                var liste = when (bereich) {
                    "heute" -> alle.filter { !it.erledigt && it.tag != null && it.tag <= heute }
                    "morgen" -> alle.filter { !it.erledigt && it.tag == heute + 1 }
                    "tag" -> {
                        val tag = tagAus(a.optString("datum")) ?: return fehler("Für bereich=tag fehlt ein gültiges datum (JJJJ-MM-TT).")
                        alle.filter { it.tag == tag }
                    }
                    "demnaechst" -> alle.filter { !it.erledigt && it.tag != null && it.tag > heute + 1 }
                    "ohne_tag" -> alle.filter { !it.erledigt && it.tag == null }
                    "erledigt" -> alle.filter { it.erledigt }.sortedByDescending { it.erledigtAm ?: 0 }
                    "alle" -> alle
                    else -> alle.filter { !it.erledigt }
                }
                if (suche.isNotEmpty()) liste = liste.filter { suche in it.titel.lowercase(Locale.GERMAN) || suche in it.text.lowercase(Locale.GERMAN) }
                if (bereich != "erledigt") {
                    liste = liste.sortedWith(compareBy<Aufgabe>({ it.tag ?: Long.MAX_VALUE }, { it.minuten == null }, { it.minuten ?: 0 }, { it.prio.rang }))
                }
                val grenze = a.optInt("limit", 60).coerceIn(1, 200)
                JSONObject()
                    .put("heute", LocalDate.now().toString())
                    .put("anzahl", liste.size)
                    .put("aufgaben", JSONArray(liste.take(grenze).map { alsJson(it, heute) }))
            }

            "anlegen" -> {
                val text = a.optString("text").trim()
                val titelEingabe = a.optString("titel").trim()
                val schritte = schritteAus(a.optJSONArray("schritte"))
                if (text.isEmpty() && titelEingabe.isEmpty() && schritte.isEmpty()) return fehler("Die Aufgabe braucht einen Text.")
                val tag = if (a.has("datum")) tagAus(a.optString("datum")) else null
                if (a.has("datum") && tag == null && !ohne(a.optString("datum"))) return fehler("Datum nicht verstanden: ${a.optString("datum")}. Erlaubt: heute, morgen, uebermorgen, JJJJ-MM-TT, ohne.")
                val minuten = if (a.has("uhrzeit") && !ohne(a.optString("uhrzeit"))) {
                    minutenAus(a.optString("uhrzeit")) ?: return fehler("Uhrzeit nicht verstanden: ${a.optString("uhrzeit")}. Format HH:MM.")
                } else null
                if (minuten != null && tag == null) return fehler("Eine Uhrzeit braucht einen Tag. Frage den Nutzer, an welchem Tag.")
                val prio = a.optString("prioritaet").takeIf { it.isNotBlank() }?.let(::prioAus) ?: Prioritaet.SPAETER
                val kiTitel = titelEingabe.isEmpty() && text.isNotEmpty()
                val titel = titelEingabe.ifEmpty { if (text.isNotEmpty()) notTitel(text) else kurzerTitel(schritte.first().text) }
                val neu = Aufgabe(
                    titel = kurzerTitel(titel),
                    text = text,
                    prioritaet = (if (tag != null && prio == Prioritaet.SPAETER) Prioritaet.MITTEL else prio).name,
                    tag = tag,
                    minuten = minuten,
                    dauer = a.optInt("dauer", 30).coerceIn(5, 24 * 60),
                    erinnerung = a.optBoolean("erinnerung", true) && minuten != null,
                    vorlauf = a.optInt("vorlauf", einstellungen.vorlaufStandard).coerceIn(0, 24 * 60),
                    wiederholung = wdhAus(a.optString("wiederholung")).name,
                    vorlesen = a.optBoolean("vorlesen", true),
                    alsWecker = a.optBoolean("als_wecker", false),
                    schritteJson = Aufgabe.schritteAlsJson(schritte),
                )
                val id = repo.neu(neu)
                if (kiTitel && einstellungen.kiTitel) titelErzeugen(id, text)
                JSONObject().put("aufgabe", alsJson(neu.copy(id = id), Tage.heute()))
            }

            "aendern" -> {
                val alt = repo.eine(a.optLong("id", -1)) ?: return fehler("Keine Aufgabe mit dieser id.")
                var tag = alt.tag
                if (a.has("datum")) {
                    tag = tagAus(a.optString("datum"))
                    if (tag == null && !ohne(a.optString("datum"))) return fehler("Datum nicht verstanden: ${a.optString("datum")}.")
                }
                var minuten = alt.minuten
                if (a.has("uhrzeit")) {
                    minuten = if (ohne(a.optString("uhrzeit"))) null
                    else minutenAus(a.optString("uhrzeit")) ?: return fehler("Uhrzeit nicht verstanden: ${a.optString("uhrzeit")}.")
                }
                if (tag == null) minuten = null
                var prio = if (a.has("prioritaet")) prioAus(a.optString("prioritaet")) else alt.prio
                if (tag != null && prio == Prioritaet.SPAETER) prio = Prioritaet.MITTEL
                // Wird aus einer Aufgabe ohne Uhrzeit ein Termin, gelten dieselben Vorgaben wie beim Ziehen auf die Zeitleiste.
                val neuerTermin = minuten != null && alt.minuten == null
                val erinnerung = when {
                    a.has("erinnerung") -> a.optBoolean("erinnerung")
                    neuerTermin -> true
                    else -> alt.erinnerung
                }
                val text = if (a.has("text")) a.optString("text").trim() else alt.text
                val titelNeu = a.optString("titel").trim()
                val neu = alt.copy(
                    titel = if (titelNeu.isNotEmpty()) kurzerTitel(titelNeu) else alt.titel,
                    titelVonKi = if (titelNeu.isNotEmpty()) false else alt.titelVonKi,
                    text = text,
                    prioritaet = prio.name,
                    tag = tag,
                    minuten = minuten,
                    dauer = if (a.has("dauer")) a.optInt("dauer", alt.dauer).coerceIn(5, 24 * 60) else alt.dauer,
                    erinnerung = erinnerung && minuten != null,
                    vorlauf = when {
                        a.has("vorlauf") -> a.optInt("vorlauf", alt.vorlauf).coerceIn(0, 24 * 60)
                        neuerTermin -> einstellungen.vorlaufStandard
                        else -> alt.vorlauf
                    },
                    vorlesen = when {
                        a.has("vorlesen") -> a.optBoolean("vorlesen")
                        neuerTermin -> true
                        else -> alt.vorlesen
                    },
                    alsWecker = if (a.has("als_wecker")) a.optBoolean("als_wecker") else alt.alsWecker,
                    wiederholung = if (a.has("wiederholung")) wdhAus(a.optString("wiederholung")).name else alt.wiederholung,
                    schritteJson = if (a.has("schritte")) Aufgabe.schritteAlsJson(schritteAus(a.optJSONArray("schritte"))) else alt.schritteJson,
                )
                repo.speichere(neu)
                // Neuer Text, aber der Titel stammte von der KI: Titel passend zum neuen Text nachziehen.
                if (a.has("text") && titelNeu.isEmpty() && text != alt.text && text.isNotEmpty() && alt.titelVonKi && einstellungen.kiTitel) titelErzeugen(neu.id, text)
                JSONObject().put("aufgabe", alsJson(neu, Tage.heute()))
            }

            "erledigen" -> {
                val alt = repo.eine(a.optLong("id", -1)) ?: return fehler("Keine Aufgabe mit dieser id.")
                val erledigt = a.optBoolean("erledigt", true)
                val folge = repo.setzeErledigt(alt, erledigt)
                JSONObject()
                    .put("aufgabe", alsJson(alt.copy(erledigt = erledigt), Tage.heute()))
                    .apply { folge?.let { put("naechste_wiederholung", alsJson(it, Tage.heute())) } }
            }

            "loeschen" -> {
                val alt = repo.eine(a.optLong("id", -1)) ?: return fehler("Keine Aufgabe mit dieser id.")
                repo.loesche(alt)
                JSONObject().put("geloescht", alsJson(alt, Tage.heute()))
            }

            else -> fehler("Unbekannte Methode: $methode")
        }
    }

    /** Wie im Editor: erst der Not-Titel, danach ersetzt ihn die KI im Hintergrund — falls ChatGPT in dieser App verbunden ist. */
    private fun titelErzeugen(id: Long, text: String) {
        val ctx = context?.applicationContext ?: return
        nebenbei.launch {
            runCatching {
                val auth = CodexAuthManager(ctx)
                if (!auth.isConnected) return@launch
                val titel = AufgabenKi(auth, Einstellungen.get(ctx)).titel(text)
                val repo = AufgabenRepository.get(ctx)
                if (titel.isNotBlank()) repo.eine(id)?.let { repo.speichere(it.copy(titel = kurzerTitel(titel), titelVonKi = true)) }
            }
        }
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    private fun alsJson(a: Aufgabe, heute: Long): JSONObject = JSONObject()
        .put("id", a.id)
        .put("titel", a.titel)
        .put("text", a.text)
        .put("datum", a.tag?.let { LocalDate.ofEpochDay(it).toString() } ?: JSONObject.NULL)
        .put("tag_anzeige", a.tag?.let { Tage.datum(it) } ?: "ohne Tag")
        .put("uhrzeit", a.minuten?.let { Tage.zeit(it) } ?: JSONObject.NULL)
        .put("dauer", a.dauer)
        .put("prioritaet", a.prio.name)
        .put("erinnerung", a.erinnerung)
        .put("vorlauf", a.vorlauf)
        .put("vorlesen", a.vorlesen)
        .put("als_wecker", a.alsWecker)
        .put("wiederholung", a.wdh.name)
        .put("erledigt", a.erledigt)
        .put("ueberfaellig", !a.erledigt && a.tag != null && a.tag < heute)
        .put("schritte", JSONArray(a.schritte.map { JSONObject().put("text", it.text).put("erledigt", it.erledigt) }))

    private fun ohne(wert: String): Boolean = wert.trim().lowercase(Locale.GERMAN) in setOf("", "ohne", "kein", "keine", "null", "none")

    private fun tagAus(wert: String): Long? {
        val w = wert.trim().lowercase(Locale.GERMAN)
        val heute = Tage.heute()
        return when (w) {
            "heute" -> heute
            "morgen" -> heute + 1
            "uebermorgen", "übermorgen" -> heute + 2
            else -> runCatching { LocalDate.parse(w.take(10)).toEpochDay() }.getOrNull()
        }
    }

    private fun minutenAus(wert: String): Int? {
        val treffer = Regex("^\\s*([01]?\\d|2[0-3])(?:[:.]([0-5]\\d))?").find(wert) ?: return null
        return treffer.groupValues[1].toInt() * 60 + (treffer.groupValues[2].toIntOrNull() ?: 0)
    }

    private fun prioAus(wert: String): Prioritaet = when (wert.trim().lowercase(Locale.GERMAN)) {
        "hoch", "dringend" -> Prioritaet.HOCH
        "mittel" -> Prioritaet.MITTEL
        "gering", "niedrig" -> Prioritaet.GERING
        else -> Prioritaet.SPAETER
    }

    private fun wdhAus(wert: String): Wiederholung = when (wert.trim().lowercase(Locale.GERMAN)) {
        "taeglich", "täglich" -> Wiederholung.TAEGLICH
        "werktags" -> Wiederholung.WERKTAGS
        "woechentlich", "wöchentlich" -> Wiederholung.WOECHENTLICH
        "monatlich" -> Wiederholung.MONATLICH
        else -> Wiederholung.KEINE
    }

    /** Schritte als Liste von Texten oder von Objekten {"text", "erledigt"}. */
    private fun schritteAus(array: JSONArray?): List<Schritt> {
        array ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            when (val eintrag = array.opt(i)) {
                is JSONObject -> Schritt(eintrag.optString("text").trim(), eintrag.optBoolean("erledigt"))
                null -> null
                else -> Schritt(eintrag.toString().trim(), false)
            }
        }.filter { it.text.isNotBlank() }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
