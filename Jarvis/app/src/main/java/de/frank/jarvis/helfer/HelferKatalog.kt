package de.frank.jarvis.helfer

import android.content.Context
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Zustände eines Helfers, gleichlautend am Mac. „Mac gerade aus“ ist kein Zustand, sondern die getrennte Erreichbarkeit. */
object HelferZustand {
    const val IN_PRUEFUNG = "in_pruefung"
    const val BEREIT = "bereit"
    const val WARTET_AUF_FREIGABE = "wartet_auf_freigabe"
    const val FEHLERHAFT = "fehlerhaft"
    const val PAPIERKORB = "papierkorb"

    fun text(z: String, aktiv: Boolean) = when {
        z == PAPIERKORB -> "Im Papierkorb"
        z == IN_PRUEFUNG -> "In Prüfung"
        z == WARTET_AUF_FREIGABE -> "Wartet auf deine Freigabe"
        z == FEHLERHAFT -> "Fehlerhaft"
        !aktiv -> "Deaktiviert"
        else -> "Einsatzbereit"
    }
}

/** Was ein Helfer darf; Vorlage ist der Steckbrief, Erklärung in JarvisHelfer/README.md. */
data class HelferRechte(val netz: List<String>, val konten: List<HelferKonto>, val werkzeuge: List<String>, val extern: Boolean) {
    fun alsText(): String = buildList {
        if (netz.isNotEmpty()) add("Internet: " + netz.joinToString(", "))
        if (konten.isNotEmpty()) add("Konten: " + konten.joinToString(", ") { "${it.id} (nur an ${it.hosts.joinToString(", ")})" })
        if (werkzeuge.isNotEmpty()) add("Jarvis-Werkzeuge: " + werkzeuge.joinToString(", "))
        if (extern) add("wirkt nach außen")
    }.joinToString(" · ").ifEmpty { "Keine besonderen Rechte" }
}

data class HelferKonto(val id: String, val hosts: List<String>, val art: String)

/** Ein Katalogeintrag. [roh] ist das JSON, wie es gespeichert und an den Mac-Kanal und die Oberfläche geht. */
class Helfer(val roh: JSONObject) {
    val kennung: String get() = roh.optString("kennung")
    val name: String get() = roh.optString("name").ifEmpty { kennung }
    val zweck: String get() = roh.optString("zweck")
    val version: String get() = roh.optString("version")
    val ort: String get() = roh.optString("ort", "handy")
    val art: String get() = roh.optString("art", "skript")
    val datei: String get() = roh.optString("datei")
    val rolle: String get() = roh.optString("rolle")
    val zustand: String get() = roh.optString("zustand", HelferZustand.IN_PRUEFUNG)
    val aktiv: Boolean get() = roh.optBoolean("aktiv", true)
    val position: Int get() = roh.optInt("position")
    val faehigkeiten: List<String> get() = liste("faehigkeiten")
    val schlagworte: List<String> get() = liste("schlagworte")
    val voraussetzungen: List<String> get() = liste("voraussetzungen")
    val grenzen: List<String> get() = liste("grenzen")
    val ausgabe: String get() = roh.optString("ausgabe")
    val eingabe: JSONObject get() = roh.optJSONObject("eingabe") ?: JSONObject()
    val brauchtAppUpdate: Boolean get() = roh.optBoolean("braucht_app_update")
    val pruefung: JSONObject get() = roh.optJSONObject("pruefung") ?: JSONObject()
    val review: JSONObject get() = roh.optJSONObject("review") ?: JSONObject()
    val freigegebeneRechte: String get() = roh.optString("freigabe")
    val rechte: HelferRechte get() {
        val r = roh.optJSONObject("rechte") ?: JSONObject()
        val k = r.optJSONArray("konten") ?: JSONArray()
        return HelferRechte(
            liste(r, "netz"),
            (0 until k.length()).mapNotNull { k.optJSONObject(it) }.map { HelferKonto(it.optString("id"), liste(it, "hosts"), it.optString("art", "bearer")) },
            liste(r, "werkzeuge"), r.optBoolean("extern"),
        )
    }
    val einsatzbereit: Boolean get() = zustand == HelferZustand.BEREIT && aktiv

    private fun liste(n: String) = liste(roh, n)
    private fun liste(o: JSONObject, n: String): List<String> = o.optJSONArray(n)?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } } ?: emptyList()
}

/**
 * Der dauerhafte Katalog des Handys, die „vorbereitete Garage“: Helfer lassen sich ohne App-Update ergänzen und entfernen. Je Helfer ein Ordner
 * `files/helfer/<kennung>/` mit `aktuell/` (Skript und Steckbrief), `vorher/` (letzte geprüfte Fassung) und `speicher.json` (eigener Speicher).
 * Mac-Helfer stehen als Abbild daneben ([macHelfer]), damit nichts doppelt gepflegt wird: Maßgeblich bleibt der Katalog am Mac.
 */
object HelferKatalog {
    private val sperre = Any()
    private val _stand = MutableStateFlow(0)
    /** Zählt jede Änderung hoch; die Oberfläche liest den Katalog daraufhin neu. */
    val stand: StateFlow<Int> = _stand.asStateFlow()

    private fun wurzel(c: Context) = File(c.applicationContext.filesDir, "helfer").apply { mkdirs() }
    private fun katalogDatei(c: Context) = File(wurzel(c), "katalog.json")
    private fun macDatei(c: Context) = File(wurzel(c), "mac-katalog.json")
    fun ordner(c: Context, kennung: String) = File(wurzel(c), kennung)
    fun aktuell(c: Context, kennung: String) = File(ordner(c, kennung), "aktuell")
    fun ausgabe(c: Context, kennung: String) = File(ordner(c, kennung), "ausgabe").apply { mkdirs() }
    fun speicherDatei(c: Context, kennung: String) = File(ordner(c, kennung), "speicher.json")

    val KENNUNG = Regex("^[a-z0-9][a-z0-9-]{0,39}$")

    /** Für Helfer gesperrt: Sie sollen sich keine Rechte erschließen und keine Schutzregeln umgehen können. */
    private val GESPERRTE_WERKZEUGE = Regex("^(helfer_.*|programm.*|agent_starten|agenten|vorgang_.*|mail_senden|einstellungen|jarvis_merken|jarvis_vergessen|jarvis_langzeitkontext_ersetzen|mac_.*|jarvis_mac_.*|.*_loeschen)$")
    fun werkzeugGesperrt(name: String) = GESPERRTE_WERKZEUGE.matches(name)

    private fun lies(d: File): JSONArray = runCatching { JSONArray(d.readText()) }.getOrDefault(JSONArray())

    private fun schreib(c: Context, a: JSONArray, d: File = katalogDatei(c)) {
        val neu = File(d.parentFile, d.name + ".neu")
        neu.writeText(a.toString())
        check(neu.renameTo(d)) { "Katalog nicht gespeichert" }
        _stand.value++
    }

    private fun rohListe(c: Context): List<JSONObject> = lies(katalogDatei(c)).let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }

    /** Alle Helfer des Handys in Franks Reihenfolge; Papierkorb nur auf Wunsch. */
    fun alle(c: Context, mitPapierkorb: Boolean = false): List<Helfer> = synchronized(sperre) { rohListe(c) }.map { Helfer(it) }
        .filter { mitPapierkorb || it.zustand != HelferZustand.PAPIERKORB }.sortedBy { it.position }

    fun macHelfer(c: Context): List<Helfer> = synchronized(sperre) { lies(macDatei(c)) }.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.map { Helfer(it) }
        .filter { it.zustand != HelferZustand.PAPIERKORB }.sortedBy { it.position }

    fun finde(c: Context, kennung: String): Helfer? = alle(c, true).firstOrNull { it.kennung == kennung } ?: macHelfer(c).firstOrNull { it.kennung == kennung }

    fun macKatalogUebernehmen(c: Context, helfer: JSONArray) {
        synchronized(sperre) {
            schreib(c, helfer, macDatei(c))
            c.applicationContext.getSharedPreferences("helfer_mac", Context.MODE_PRIVATE).edit().putLong("gesehen", System.currentTimeMillis()).apply()
        }
        // Am Mac freigegeben: Eine Aufgabe, die darauf wartet, geht jetzt weiter.
        macHelfer(c).filter { it.einsatzbereit }.forEach { HelferAblauf.nachFreigabe(c, it.kennung) }
    }

    fun macZuletztGesehen(c: Context): Long = c.applicationContext.getSharedPreferences("helfer_mac", Context.MODE_PRIVATE).getLong("gesehen", 0L)

    /** Schreibt die Änderung genau eines Eintrags unter der Sperre zurück. */
    private fun aendere(c: Context, kennung: String, aenderung: (JSONObject) -> Unit): Helfer? = synchronized(sperre) {
        val l = rohListe(c)
        val e = l.firstOrNull { it.optString("kennung") == kennung } ?: return null
        aenderung(e)
        schreib(c, JSONArray(l))
        Helfer(e)
    }

    /** Liefert einen Grund, wenn der Steckbrief nicht passt; null = in Ordnung. */
    fun steckbriefFehler(m: JSONObject): String? = when {
        !KENNUNG.matches(m.optString("kennung")) -> "kennung muss klein aus a-z, 0-9 und - bestehen, höchstens 40 Zeichen."
        m.optString("name").isBlank() || m.optString("zweck").isBlank() -> "name und zweck fehlen."
        m.optString("ort") !in setOf("handy", "mac") -> "ort muss handy oder mac sein."
        m.optString("art") !in setOf("skript", "agent") -> "art muss skript oder agent sein."
        m.optString("art") == "agent" && m.optString("rolle").length < 40 -> "Ein Agent-Helfer braucht eine ausführliche rolle."
        m.optString("art") == "skript" && m.optString("datei").let { it.isBlank() || it.contains('/') || it.contains("..") } -> "datei fehlt oder ist kein einfacher Dateiname."
        m.optString("art") == "skript" && !testsVollstaendig(m) -> "Pflicht sind mindestens ein Normalfall (erwartet ok=true) und ein Fehlerfall (ok=false), jeder Test als Objekt."
        Helfer(m).rechte.werkzeuge.firstOrNull { werkzeugGesperrt(it) } != null -> "Dieses Werkzeug ist für Helfer gesperrt: " + Helfer(m).rechte.werkzeuge.first { werkzeugGesperrt(it) }
        else -> null
    }

    private fun testsVollstaendig(m: JSONObject): Boolean {
        val t = m.optJSONArray("tests") ?: return false
        val alle = (0 until t.length()).map { t.opt(it) }
        if (alle.isEmpty() || alle.any { it !is JSONObject }) return false
        val erwartet = alle.map { (it as JSONObject).optJSONObject("erwartet")?.optBoolean("ok", true) ?: true }
        return erwartet.any { it } && erwartet.any { !it }
    }

    /** Fingerabdruck der Rechte, die Frank freigeben muss. Leer = keine Freigabe nötig. Neue Rechte in neuer Fassung fragen erneut. */
    fun freigabePflicht(c: Context, h: Helfer): String {
        val r = h.rechte
        val schreibend = r.werkzeuge.filter { n -> de.frank.jarvis.faehigkeit.Register.werkzeuge(c).firstOrNull { it.name == n }?.nurLesen != true }
        // Lesen plus Netz könnte private Daten hinaustragen: Das braucht wie schreibende Werkzeuge Franks Ja.
        val abfluss = r.netz.isNotEmpty() && r.werkzeuge.isNotEmpty()
        if (r.konten.isEmpty() && schreibend.isEmpty() && !r.extern && !abfluss) return ""
        return (r.konten.map { "konto:${it.id}:${it.art}:${it.hosts.sorted()}" } + schreibend.map { "werkzeug:$it" } + (if (r.extern) listOf("extern") else emptyList()) +
            (if (abfluss) listOf("netz:${r.netz.sorted()}", "lesen:${r.werkzeuge.sorted()}") else emptyList())).sorted().joinToString("|")
    }

    /**
     * Installiert eine geprüfte neue Fassung: Die bisherige wird `vorher/`, der eigene Speicher bleibt. Wer Tests nicht besteht, kommt nie hierher.
     * Liefert den neuen Eintrag.
     */
    fun installieren(c: Context, manifest: JSONObject, probeOrdner: File, pruefung: JSONObject, review: JSONObject): Helfer = synchronized(sperre) {
        val k = manifest.optString("kennung")
        val o = ordner(c, k).apply { mkdirs() }
        val akt = File(o, "aktuell")
        val alt = File(o, "vorher")
        val zwischen = File(o, "vorher.neu").apply { deleteRecursively() }
        val hatteAlt = akt.exists()
        if (hatteAlt) check(akt.renameTo(zwischen)) { "Vorige Fassung nicht gesichert" }
        if (!probeOrdner.renameTo(akt)) {
            // Lieber die alte Fassung behalten, als einen Eintrag ohne Programm zu hinterlassen.
            if (hatteAlt) zwischen.renameTo(akt)
            error("Neue Fassung nicht übernommen")
        }
        if (hatteAlt) {
            rohListe(c).firstOrNull { it.optString("kennung") == k }?.let { File(zwischen, "eintrag.json").writeText(it.toString()) }
            alt.deleteRecursively()
            zwischen.renameTo(alt)
        }
        val l = rohListe(c).toMutableList()
        val bisher = l.firstOrNull { it.optString("kennung") == k }
        val e = JSONObject(manifest.toString())
        e.remove("tests"); e.remove("zustand_vorher")
        e.put("pruefung", pruefung).put("review", review)
        e.put("position", bisher?.optInt("position") ?: ((l.maxOfOrNull { it.optInt("position") } ?: -1) + 1))
        e.put("aktiv", bisher?.optBoolean("aktiv", true) ?: true)
        val pflicht = freigabePflicht(c, Helfer(e))
        val frei = bisher?.optString("freigabe").orEmpty()
        e.put("freigabe", if (pflicht == frei) frei else "")
        val neu = if (pflicht.isEmpty() || pflicht == frei) HelferZustand.BEREIT else HelferZustand.WARTET_AUF_FREIGABE
        // Im Papierkorb bleibt er im Papierkorb: Eine neue Fassung holt ihn nicht ungefragt zurück.
        if (bisher?.optString("zustand") == HelferZustand.PAPIERKORB) e.put("zustand", HelferZustand.PAPIERKORB).put("zustand_vorher", neu) else e.put("zustand", neu)
        l.removeAll { it.optString("kennung") == k }
        l += e
        schreib(c, JSONArray(l))
        Helfer(e)
    }

    /** Nur aus der Oberfläche: Frank gibt die Rechte der aktuellen Fassung frei. Kein Werkzeug führt hierher. */
    fun freigeben(c: Context, kennung: String) = aendere(c, kennung) { e ->
        e.put("freigabe", freigabePflicht(c, Helfer(e))).put("zustand", HelferZustand.BEREIT)
    }

    fun aktivieren(c: Context, kennung: String, an: Boolean) = aendere(c, kennung) { it.put("aktiv", an) }
    fun inPapierkorb(c: Context, kennung: String) = aendere(c, kennung) { if (it.optString("zustand") != HelferZustand.PAPIERKORB) it.put("zustand_vorher", it.optString("zustand")).put("zustand", HelferZustand.PAPIERKORB) }
    fun wiederherstellen(c: Context, kennung: String) = aendere(c, kennung) { if (it.optString("zustand") == HelferZustand.PAPIERKORB) it.put("zustand", it.optString("zustand_vorher").ifEmpty { HelferZustand.FEHLERHAFT }) }

    /** Setzt auf die letzte geprüfte Fassung zurück. null = es gibt keine. */
    fun rueckrollen(c: Context, kennung: String): Helfer? = synchronized(sperre) {
        val o = ordner(c, kennung)
        val alt = File(o, "vorher")
        val eintrag = runCatching { JSONObject(File(alt, "eintrag.json").readText()) }.getOrNull() ?: return null
        val akt = File(o, "aktuell")
        akt.deleteRecursively()
        check(alt.renameTo(akt)) { "Vorige Fassung nicht wiederhergestellt" }
        val l = rohListe(c).toMutableList()
        val jetzt = l.firstOrNull { it.optString("kennung") == kennung }
        eintrag.put("position", jetzt?.optInt("position") ?: eintrag.optInt("position"))
        // Franks Schalter gilt weiter: Ein deaktivierter Helfer wird durch das Rücksetzen nicht wieder aktiv.
        eintrag.put("aktiv", jetzt?.optBoolean("aktiv", true) ?: true)
        l.removeAll { it.optString("kennung") == kennung }
        l += eintrag
        schreib(c, JSONArray(l))
        Helfer(eintrag)
    }

    fun hatVorherigeFassung(c: Context, kennung: String) = File(ordner(c, kennung), "vorher/eintrag.json").exists()

    /** Endgültig löschen: Ordner samt eigenem Speicher. Konten bleiben immer erhalten, sie gehören dem zentralen Bereich. */
    fun endgueltigLoeschen(c: Context, kennung: String) = synchronized(sperre) {
        val l = rohListe(c).filter { it.optString("kennung") != kennung }
        schreib(c, JSONArray(l))
        ordner(c, kennung).deleteRecursively()
        Unit
    }

    /** Verschiebt in der Reihenfolge nach oben (-1) oder unten (+1). */
    fun verschieben(c: Context, kennung: String, richtung: Int) = synchronized(sperre) {
        val alle = rohListe(c).sortedBy { it.optInt("position") }.toMutableList()
        val i = alle.indexOfFirst { it.optString("kennung") == kennung }
        val j = i + richtung
        if (i >= 0 && j in alle.indices) {
            java.util.Collections.swap(alle, i, j)
            alle.forEachIndexed { n, e -> e.put("position", n) }
            schreib(c, JSONArray(alle))
        }
    }

    /** Wer hängt an diesem Helfer? Andere Helfer dürfen ihn nur über Werkzeuge nutzen, das ist gesperrt; bleiben geteilte Konten. */
    fun geteilteKonten(c: Context, h: Helfer): List<String> {
        val andere = alle(c, true).filter { it.kennung != h.kennung }
        return h.rechte.konten.map { it.id }.filter { id -> andere.any { o -> o.rechte.konten.any { it.id == id } } }
    }

    /** Schnelle Suche nach Wortteilen in Name, Zweck, Fähigkeiten, Schlagworten; ohne Treffer leere Liste. Kein Modell, kein Netz. */
    fun suche(c: Context, anfrage: String): List<Helfer> {
        val worte = anfrage.lowercase(Locale.GERMAN).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 2 }
        val alle = alle(c) + macHelfer(c)
        if (worte.isEmpty()) return alle
        return alle.map { h ->
            val text = (listOf(h.kennung, h.name, h.zweck) + h.faehigkeiten + h.schlagworte).joinToString(" ").lowercase(Locale.GERMAN)
            h to worte.count { text.contains(it) }
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
    }
}
