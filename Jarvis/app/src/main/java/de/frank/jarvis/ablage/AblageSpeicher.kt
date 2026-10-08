package de.frank.jarvis.ablage

import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Eine Datei in der Ablage. [speicher] ist der Pfad relativ zum privaten App-Speicher (filesDir). */
data class Anhang(
    val id: String,
    val titel: String,
    val originalName: String,
    val endung: String,
    val mime: String,
    val groesse: Long,
    val erstellt: Long,
    val herkunft: String,
    val speicher: String,
    val sha256: String,
    val beschreibung: String = "",
    val vorschau: String? = null,
    val kennung: String? = null,
) {
    val art: Art get() = Dateityp.art(endung, mime)
}

/**
 * Ein Ablage-Eintrag: optional ein lesbarer Text (Markdown-Datei unter `filesDir/ablage/`, wie bisher) und
 * beliebig viele Anhänge. Eine Recherche kann so aus Bericht, PDF und mehreren Bildern bestehen.
 */
data class Eintrag(
    val id: String,
    val titel: String,
    val textDatei: String?,
    val erstellt: Long,
    val geaendert: Long,
    val herkunft: String,
    val beschreibung: String = "",
    val anhaenge: List<Anhang> = emptyList(),
) {
    val hatText: Boolean get() = textDatei != null
    val hatDateien: Boolean get() = anhaenge.isNotEmpty()
    /** Kategorien für die Filter: Text-Einträge zählen als Texte, dazu die Kategorien aller Anhänge. */
    val kategorien: Set<Kategorie> get() = buildSet { if (hatText) add(Kategorie.TEXTE); anhaenge.forEach { add(it.art.kategorie) } }
}

/** Was eine Übernahme ergibt. [neu] = false, wenn genau diese Datei schon da war (wiederholte Zustellung). */
data class Uebernahme(val eintrag: Eintrag, val anhang: Anhang, val neu: Boolean)

class AblageFehler(meldung: String) : Exception(meldung)

/**
 * Der dauerhafte Speicher der Ablage im privaten App-Speicher. Reines Kotlin (ohne Android), damit Migration,
 * Übernahme und Doppelschutz in Unit-Tests laufen.
 *
 * Aufbau unter [wurzel] (= filesDir):
 * - `ablage/<Titel>.md`: die Texte, unverändert wie vor der Erweiterung. Mail, Vorlesen und Agenten lesen sie weiter so.
 * - `ablage-dateien/<Eintrag-ID>/<Anhang-ID>.<endung>`: die Anhänge. Der Speichername kommt aus der ID, gleichnamige
 *   Dateien überschreiben sich deshalb nie; der ursprüngliche Name steht im Verzeichnis.
 * - `ablage-dateien/.teil/`: laufende Übertragungen. Eine Datei kommt erst nach vollständigem Empfang und Prüfung
 *   von dort in die Ablage; unvollständige erscheinen nie als Eintrag.
 * - `ablage-index.json`: das Verzeichnis aller Einträge (atomar geschrieben, Vorgängerfassung als `.bak`).
 *
 * Migration: Beim ersten Laden wird jede vorhandene Textdatei zu einem Eintrag. Es wird nichts verschoben,
 * umbenannt oder gelöscht; ohne Verzeichnis entsteht es einfach neu aus den Dateien.
 */
class AblageSpeicher private constructor(private val wurzel: File) {
    val textOrdner: File get() = File(wurzel, "ablage").apply { mkdirs() }
    private val dateiOrdner: File get() = File(wurzel, "ablage-dateien").apply { mkdirs() }
    val teilOrdner: File get() = File(dateiOrdner, ".teil").apply { mkdirs() }
    private val index = File(wurzel, "ablage-index.json")
    private val sicherung = File(wurzel, "ablage-index.json.bak")
    private val sperre = Any()

    private val _stand = MutableStateFlow(0L)
    /** Ändert sich bei jeder Änderung der Ablage; die Oberfläche liest dann neu ein. */
    val stand: StateFlow<Long> = _stand.asStateFlow()

    // ---- Lesen ----

    /** Alle Einträge, neueste zuerst. Gleicht dabei mit den Textdateien ab (Migration und Fremdänderungen). */
    fun eintraege(): List<Eintrag> = synchronized(sperre) { abgeglichen() }.sortedByDescending { it.geaendert }

    fun eintrag(id: String): Eintrag? = eintraege().firstOrNull { it.id == id }

    /** Sucht einen Eintrag: genauer Titel (ohne Groß/klein) oder ein eindeutiger Teil davon. */
    fun finde(titel: String): Eintrag? {
        val gesucht = schluessel(titel.removeSuffix(".md"))
        if (gesucht.isEmpty()) return null
        val alle = eintraege()
        return alle.firstOrNull { schluessel(it.titel) == gesucht }
            ?: alle.filter { gesucht in schluessel(it.titel) }.singleOrNull()
    }

    /** Nur genauer Titel: für das Anhängen von Dateien, damit nie ein falscher Eintrag ergänzt wird. */
    fun findeGenau(titel: String): Eintrag? = schluessel(titel).let { g -> eintraege().firstOrNull { schluessel(it.titel) == g } }

    fun datei(anhang: Anhang): File = File(wurzel, anhang.speicher)
    fun textDatei(eintrag: Eintrag): File? = eintrag.textDatei?.let { File(textOrdner, it) }?.takeIf { it.isFile }
    fun vorschauDatei(anhang: Anhang): File = File(datei(anhang).parentFile, anhang.id + ".vorschau.jpg")

    fun text(eintrag: Eintrag, hoechstens: Int = Int.MAX_VALUE): String =
        textDatei(eintrag)?.let { runCatching { lesen(it, hoechstens) }.getOrNull() }.orEmpty()

    // ---- Schreiben ----

    /**
     * Legt den Text eines Eintrags an oder ersetzt ihn (mit [anhaengen] wird ergänzt). Verhalten wie die bisherige
     * Textablage: ein vorhandener Eintrag wird über den Titel gefunden, auch über einen eindeutigen Teil davon.
     */
    fun schreibeText(titel: String, inhalt: String, anhaengen: Boolean = false, herkunft: String = "Jarvis"): Pair<Eintrag, File> {
        synchronized(sperre) {
            val liste = abgeglichen().toMutableList()
            val vorhanden = finde(titel)
            val name = vorhanden?.textDatei ?: (vorhanden?.titel ?: titel).let { dateiname(it) + ".md" }
            val datei = File(textOrdner, name)
            if (anhaengen && datei.exists()) datei.appendText("\n\n" + inhalt.trim() + "\n") else schreibeAtomar(datei, (inhalt.trim() + "\n").toByteArray())
            val jetzt = System.currentTimeMillis()
            val eintrag = if (vorhanden != null) {
                vorhanden.copy(textDatei = name, geaendert = jetzt).also { neu -> liste.replaceAll { if (it.id == neu.id) neu else it } }
            } else {
                Eintrag(neueId(), dateiname(titel), name, jetzt, jetzt, herkunft).also { liste += it }
            }
            speichere(liste)
            return eintrag to datei
        }
    }

    fun neueTeilDatei(): File = File(teilOrdner, neueId() + ".teil")

    /**
     * Übernimmt eine vollständig empfangene Datei [quelle] (liegt in [teilOrdner]) in den Eintrag [eintragTitel].
     * Doppelschutz: gleiche [kennung] oder gleicher Inhalt im selben Eintrag ergibt keinen zweiten Anhang.
     * Erst wenn die Datei an ihrem Platz liegt, wird das Verzeichnis geschrieben.
     */
    fun uebernimm(
        eintragTitel: String,
        quelle: File,
        name: String,
        mime: String? = null,
        herkunft: String = "Jarvis",
        beschreibung: String = "",
        kennung: String? = null,
        titel: String? = null,
        eintragBeschreibung: String = "",
    ): Uebernahme {
        if (!quelle.isFile) throw AblageFehler("Die empfangene Datei fehlt.")
        val groesse = quelle.length()
        if (groesse <= 0) { quelle.delete(); throw AblageFehler("Die Datei ist leer und wurde nicht gespeichert.") }
        val sha = sha256(quelle)
        val kopf = quelle.inputStream().use { s -> ByteArray(4096).let { b -> val n = s.read(b); if (n <= 0) ByteArray(0) else b.copyOf(n) } }
        val (endung, mimeTyp) = Dateityp.bestimme(name, mime, kopf)
        val anzeigeName = sichererName(name, endung)
        synchronized(sperre) {
            val liste = abgeglichen().toMutableList()
            if (!kennung.isNullOrBlank()) {
                liste.forEach { e -> e.anhaenge.firstOrNull { it.kennung == kennung }?.let { quelle.delete(); return Uebernahme(e, it, neu = false) } }
            }
            val vorhanden = liste.firstOrNull { schluessel(it.titel) == schluessel(eintragTitel) }
            vorhanden?.anhaenge?.firstOrNull { it.sha256 == sha }?.let { quelle.delete(); return Uebernahme(vorhanden, it, neu = false) }
            val jetzt = System.currentTimeMillis()
            val eintrag = vorhanden ?: Eintrag(neueId(), eintragTitel.trim().ifEmpty { anzeigeName }.take(120), null, jetzt, jetzt, herkunft, eintragBeschreibung)
            val anhangId = neueId()
            val ordner = File(dateiOrdner, eintrag.id).apply { mkdirs() }
            val ziel = File(ordner, "$anhangId.$endung")
            verschiebe(quelle, ziel)
            if (ziel.length() != groesse) { ziel.delete(); throw AblageFehler("Die Datei wurde beim Ablegen unvollständig kopiert.") }
            val anhang = Anhang(
                id = anhangId,
                titel = (titel?.trim()?.takeIf { it.isNotEmpty() } ?: anzeigeName.substringBeforeLast('.')).take(120),
                originalName = anzeigeName,
                endung = endung,
                mime = mimeTyp,
                groesse = groesse,
                erstellt = jetzt,
                herkunft = herkunft,
                speicher = ziel.relativeTo(wurzel).invariantSeparatorsPath,
                sha256 = sha,
                beschreibung = beschreibung.trim(),
                kennung = kennung?.takeIf { it.isNotBlank() },
            )
            val neu = eintrag.copy(anhaenge = eintrag.anhaenge + anhang, geaendert = jetzt)
            if (vorhanden == null) liste += neu else liste.replaceAll { if (it.id == neu.id) neu else it }
            try {
                speichere(liste)
            } catch (e: Exception) {
                ziel.delete()
                throw AblageFehler("Das Ablage-Verzeichnis ließ sich nicht schreiben: ${e.message}")
            }
            return Uebernahme(neu, anhang, neu = true)
        }
    }

    /** Bequemer Weg für Inhalte, die schon im Speicher liegen (Text, kleine Base64-Dateien, erzeugte Bilder). */
    fun uebernimmBytes(eintragTitel: String, bytes: ByteArray, name: String, mime: String? = null, herkunft: String = "Jarvis", beschreibung: String = "", kennung: String? = null, titel: String? = null): Uebernahme {
        val teil = neueTeilDatei()
        FileOutputStream(teil).use { it.write(bytes); it.fd.sync() }
        return try { uebernimm(eintragTitel, teil, name, mime, herkunft, beschreibung, kennung, titel) } finally { if (teil.exists()) teil.delete() }
    }

    fun setzeVorschau(eintragId: String, anhangId: String, vorschau: File) = synchronized(sperre) {
        val liste = abgeglichen().toMutableList()
        val rel = vorschau.relativeTo(wurzel).invariantSeparatorsPath
        var geaendert = false
        liste.replaceAll { e ->
            if (e.id != eintragId) e else e.copy(anhaenge = e.anhaenge.map { a -> if (a.id == anhangId && a.vorschau != rel) { geaendert = true; a.copy(vorschau = rel) } else a })
        }
        if (geaendert) speichere(liste, melden = false)
    }

    fun loescheEintrag(id: String): Boolean = synchronized(sperre) {
        val liste = abgeglichen().toMutableList()
        val e = liste.firstOrNull { it.id == id } ?: return false
        liste.removeAll { it.id == id }
        speichere(liste)
        e.textDatei?.let { File(textOrdner, it).delete() }
        File(dateiOrdner, e.id).deleteRecursively()
        true
    }

    fun loescheAnhang(eintragId: String, anhangId: String): Boolean = synchronized(sperre) {
        val liste = abgeglichen().toMutableList()
        val e = liste.firstOrNull { it.id == eintragId } ?: return false
        val a = e.anhaenge.firstOrNull { it.id == anhangId } ?: return false
        val rest = e.copy(anhaenge = e.anhaenge - a, geaendert = System.currentTimeMillis())
        if (rest.anhaenge.isEmpty() && rest.textDatei == null) liste.removeAll { it.id == eintragId } else liste.replaceAll { if (it.id == eintragId) rest else it }
        speichere(liste)
        datei(a).delete()
        vorschauDatei(a).delete()
        if (rest.anhaenge.isEmpty()) File(dateiOrdner, e.id).deleteRecursively()
        true
    }

    /**
     * Räumt Reste auf: Teil-Dateien abgebrochener Übertragungen (älter als [teilAlterMs], außer [behalten]) und
     * Dateien, die nach einem Absturz zwischen Ablegen und Verzeichnis-Schreiben ohne Eintrag liegen blieben.
     */
    fun aufraeumen(behalten: Set<String> = emptySet(), teilAlterMs: Long = 48 * 3_600_000L) = synchronized(sperre) {
        val jetzt = System.currentTimeMillis()
        teilOrdner.listFiles()?.forEach { if (it.name !in behalten && jetzt - it.lastModified() > teilAlterMs) it.delete() }
        val bekannt = abgeglichen().flatMap { e -> e.anhaenge.flatMap { a -> listOfNotNull(a.speicher, a.vorschau, vorschauDatei(a).relativeTo(wurzel).invariantSeparatorsPath) } }.toSet()
        val eintragIds = abgeglichen().map { it.id }.toSet()
        dateiOrdner.listFiles()?.filter { it.isDirectory && it.name != ".teil" }?.forEach { ordner ->
            ordner.listFiles()?.forEach { f ->
                if (f.relativeTo(wurzel).invariantSeparatorsPath !in bekannt && jetzt - f.lastModified() > 3_600_000L) f.delete()
            }
            if (ordner.name !in eintragIds && (ordner.listFiles()?.isEmpty() != false)) ordner.delete()
        }
    }

    // ---- Verzeichnis ----

    private fun abgeglichen(): List<Eintrag> {
        val geladen = lade()
        val liste = geladen.toMutableList()
        var geaendert = false
        val mds = textOrdner.listFiles { f -> f.isFile && f.name.endsWith(".md") }.orEmpty().associateBy { it.name }
        // Text verschwunden (außerhalb gelöscht): Eintrag ohne Anhänge entfällt, sonst bleibt er ohne Text.
        liste.replaceAll { e -> if (e.textDatei != null && e.textDatei !in mds) { geaendert = true; e.copy(textDatei = null) } else e }
        val vergeben = liste.mapNotNull { it.textDatei }.toMutableSet()
        mds.values.filter { it.name !in vergeben }.sortedBy { it.lastModified() }.forEach { md ->
            val passend = liste.indexOfFirst { it.textDatei == null && dateiname(it.titel) + ".md" == md.name }
            if (passend >= 0) {
                liste[passend] = liste[passend].copy(textDatei = md.name, geaendert = maxOf(liste[passend].geaendert, md.lastModified()))
            } else {
                // Migration: jede bisherige Textdatei wird ein Eintrag.
                liste += Eintrag(neueId(), md.nameWithoutExtension, md.name, md.lastModified(), md.lastModified(), "Textablage")
            }
            vergeben += md.name
            geaendert = true
        }
        // Außerhalb geänderte Texte: Änderungszeit nachziehen (ohne neu zu speichern).
        val ergebnis = liste.filter { it.textDatei != null || it.anhaenge.isNotEmpty() }.map { e ->
            e.textDatei?.let { mds[it] }?.lastModified()?.takeIf { it > e.geaendert }?.let { e.copy(geaendert = it) } ?: e
        }
        if (ergebnis.size != liste.size) geaendert = true
        if (geaendert || !index.exists()) speichere(ergebnis, melden = geaendert && geladen.isNotEmpty())
        return ergebnis
    }

    private fun lade(): List<Eintrag> {
        for (quelle in listOf(index, sicherung)) {
            if (!quelle.isFile) continue
            val liste = runCatching { ausJson(JSONObject(quelle.readText())) }.getOrNull()
            if (liste != null) return liste
            // Beschädigt: zur Untersuchung beiseitelegen, dann die Vorgängerfassung versuchen.
            quelle.copyTo(File(wurzel, quelle.name + ".defekt-" + System.currentTimeMillis()), overwrite = true)
        }
        return emptyList()
    }

    private fun speichere(liste: List<Eintrag>, melden: Boolean = true) {
        val json = JSONObject().put("format", 1).put("eintraege", JSONArray(liste.map(::alsJson))).toString(1)
        if (index.isFile) index.copyTo(sicherung, overwrite = true)
        schreibeAtomar(index, json.toByteArray())
        if (melden) _stand.value = _stand.value + 1
    }

    private fun alsJson(e: Eintrag) = JSONObject()
        .put("id", e.id).put("titel", e.titel).put("text", e.textDatei ?: JSONObject.NULL)
        .put("erstellt", e.erstellt).put("geaendert", e.geaendert).put("herkunft", e.herkunft).put("beschreibung", e.beschreibung)
        .put("anhaenge", JSONArray(e.anhaenge.map { a ->
            JSONObject().put("id", a.id).put("titel", a.titel).put("name", a.originalName).put("endung", a.endung).put("mime", a.mime)
                .put("groesse", a.groesse).put("erstellt", a.erstellt).put("herkunft", a.herkunft).put("speicher", a.speicher)
                .put("sha256", a.sha256).put("beschreibung", a.beschreibung).put("vorschau", a.vorschau ?: JSONObject.NULL)
                .put("kennung", a.kennung ?: JSONObject.NULL)
        }))

    private fun ausJson(json: JSONObject): List<Eintrag> {
        val liste = json.getJSONArray("eintraege")
        return (0 until liste.length()).map { i ->
            val e = liste.getJSONObject(i)
            val anhaenge = e.optJSONArray("anhaenge") ?: JSONArray()
            Eintrag(
                id = e.getString("id"),
                titel = e.getString("titel"),
                textDatei = e.optString("text").takeIf { !e.isNull("text") && it.isNotEmpty() },
                erstellt = e.optLong("erstellt"),
                geaendert = e.optLong("geaendert"),
                herkunft = e.optString("herkunft"),
                beschreibung = e.optString("beschreibung"),
                anhaenge = (0 until anhaenge.length()).map { j ->
                    val a = anhaenge.getJSONObject(j)
                    Anhang(
                        id = a.getString("id"), titel = a.optString("titel"), originalName = a.optString("name"), endung = a.optString("endung"),
                        mime = a.optString("mime"), groesse = a.optLong("groesse"), erstellt = a.optLong("erstellt"), herkunft = a.optString("herkunft"),
                        speicher = a.getString("speicher"), sha256 = a.optString("sha256"), beschreibung = a.optString("beschreibung"),
                        vorschau = a.optString("vorschau").takeIf { !a.isNull("vorschau") && it.isNotEmpty() },
                        kennung = a.optString("kennung").takeIf { !a.isNull("kennung") && it.isNotEmpty() },
                    )
                }.filter { File(wurzel, it.speicher).isFile },
            )
        }
    }

    companion object {
        private val instanzen = HashMap<String, AblageSpeicher>()

        /** Eine Instanz je Speicherort, damit alle Zugriffe dieselbe Sperre teilen. */
        fun fuer(wurzel: File): AblageSpeicher = synchronized(instanzen) { instanzen.getOrPut(wurzel.absolutePath) { AblageSpeicher(wurzel) } }

        /** Nur für Tests: vergisst die Instanz, damit ein „Neustart“ alles frisch von der Platte liest. */
        internal fun vergiss(wurzel: File) = synchronized(instanzen) { instanzen.remove(wurzel.absolutePath) }

        /** Macht aus einem Titel einen sicheren Dateinamen, ohne Pfadzeichen (wie bisher in der Textablage). */
        fun dateiname(titel: String): String =
            titel.trim().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").replace(Regex("\\s+"), " ").take(80).trim().ifEmpty { "Notiz" }

        private fun schluessel(titel: String) = dateiname(titel).lowercase(Locale.GERMAN)

        /** Dateiname zum Anzeigen und Exportieren: ohne Pfad- und Steuerzeichen, mit passender Endung. */
        fun sichererName(name: String, endung: String): String {
            val basis = name.substringAfterLast('/').substringAfterLast('\\').replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trim('.').take(120)
            val ohne = if (Dateityp.endung(basis).isNotEmpty()) basis.substringBeforeLast('.') else basis
            val vorhanden = Dateityp.endung(basis)
            val passend = vorhanden.isNotEmpty() && (vorhanden == endung || Dateityp.mimeFuer(vorhanden) == Dateityp.mimeFuer(endung) && Dateityp.mimeFuer(endung) != "application/octet-stream")
            return (ohne.ifEmpty { "Datei" }) + "." + (if (passend) vorhanden else endung)
        }

        fun neueId(): String = UUID.randomUUID().toString().replace("-", "").take(20)

        fun sha256(datei: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            datei.inputStream().use { s -> val puffer = ByteArray(64 * 1024); while (true) { val n = s.read(puffer); if (n < 0) break; md.update(puffer, 0, n) } }
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        private fun lesen(datei: File, hoechstens: Int): String =
            if (hoechstens == Int.MAX_VALUE || datei.length() <= hoechstens) datei.readText()
            else datei.inputStream().use { s -> String(s.readNBytesKompatibel(hoechstens), Charsets.UTF_8) }

        private fun java.io.InputStream.readNBytesKompatibel(n: Int): ByteArray {
            val puffer = ByteArray(n)
            var gelesen = 0
            while (gelesen < n) { val r = read(puffer, gelesen, n - gelesen); if (r < 0) break; gelesen += r }
            return puffer.copyOf(gelesen)
        }

        /** Schreibt über eine Zwischendatei und benennt dann um: Ein Abbruch hinterlässt nie eine halbe Datei. */
        fun schreibeAtomar(ziel: File, daten: ByteArray) {
            ziel.parentFile?.mkdirs()
            val tmp = File(ziel.parentFile, ziel.name + ".tmp")
            FileOutputStream(tmp).use { it.write(daten); it.fd.sync() }
            if (!tmp.renameTo(ziel)) { ziel.delete(); if (!tmp.renameTo(ziel)) { tmp.copyTo(ziel, overwrite = true); tmp.delete() } }
        }

        private fun verschiebe(quelle: File, ziel: File) {
            if (quelle.renameTo(ziel)) return
            quelle.copyTo(ziel, overwrite = false)
            quelle.delete()
        }
    }
}
