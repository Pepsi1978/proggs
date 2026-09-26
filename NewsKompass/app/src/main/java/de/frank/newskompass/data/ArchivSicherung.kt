package de.frank.newskompass.data

import android.content.ContentResolver
import android.net.Uri
import de.frank.newskompass.AppProfil
import de.frank.newskompass.observability.KompassLog
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject

/** Ergebnis eines Exports oder einer Prüfung; [fehler] null heißt: vollständig und geprüft. */
data class SicherungsErgebnis(
    val fehler: String?,
    val ausgaben: Int = 0,
    val beschaedigt: Int = 0,
    val bilder: Int = 0,
    val fehlendeBilder: Int = 0,
    /** Beiseitegelegtes aus Importen (Format 2): Konfliktfassungen, deren Bilder, beschädigte Rohdateien. */
    val quarantaeneKonflikte: Int = 0,
    val quarantaeneBilder: Int = 0,
    val quarantaeneBeschaedigt: Int = 0,
    /** Eigene Zwischendateien (*.tmp) unter files/import, die bewusst nicht gesichert wurden. */
    val uebersprungeneTemp: Int = 0,
    val bytes: Long = 0,
    val erstelltUm: Long = 0,
    /** Pfad → SHA-256 aller geprüften Einträge (ohne Manifest). */
    val eintraege: Map<String, String> = emptyMap(),
)

/**
 * Manuelle Archivsicherung als ZIP über die Android-Dateiauswahl.
 *
 * Inhalt: alle lesbaren Ausgaben byte-genau (`ausgaben/`), unlesbare roh (`beschaedigt/`), alle
 * Bilder, auf die eine Ausgabe zeigt (`bilder/`), ab Format 2 alles, was Importe beiseitegelegt
 * haben (`quarantaene/`), und zuletzt `manifest.json` mit Format, Pfaden, Größen und SHA-256 der
 * tatsächlich geschriebenen Bytes. Einstellungen, Schlüssel und Anmeldung gehören nicht hinein.
 * Lokale Dateien werden nur gelesen. Sicherungen im Format 1 (Version 1.7/1.8) bleiben lesbar.
 */
object ArchivSicherung {

    /** Geschrieben wird Format 2; geprüft und importiert werden 1 und 2. */
    const val FORMAT = 2
    val LESBARE_FORMATE = setOf(1, 2)
    const val MANIFEST = "manifest.json"

    internal const val MAX_JSON = 5L * 1024 * 1024
    internal const val MAX_BILD = 20L * 1024 * 1024
    private const val MAX_EINTRAEGE = 500_000
    private const val MAX_GESAMT = 64L * 1024 * 1024 * 1024

    internal val AUSGABE_PFAD = Regex("ausgaben/(a\\d{1,20})\\.json")
    internal val BESCHAEDIGT_PFAD = Regex("beschaedigt/(a\\d{1,20})\\.json")
    internal val BILD_PFAD = Regex("bilder/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.jpg")

    // Format 2: beiseitegelegte Importdaten, in der ZIP unter quarantaene/, lokal unter files/import/.
    private const val UUID_MUSTER = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    internal const val QUARANTAENE = "quarantaene/"
    internal val Q_KONFLIKT_PFAD = Regex("quarantaene/konflikte/(\\d{1,20})/(ausgaben|beschaedigt)/(a\\d{1,20})\\.json")
    internal val Q_BILD_PFAD = Regex("quarantaene/konflikte/(\\d{1,20})/bilder/($UUID_MUSTER)\\.jpg")
    internal val Q_BESCHAEDIGT_PFAD = Regex("quarantaene/beschaedigt/(a\\d{1,20})\\.json")

    /** Eigene Dateien unter files/import, die bewusst nicht beiseitegelegte Daten sind. */
    private val IMPORT_AUSGENOMMEN = setOf("herkunft.json")
    private const val IMPORT_BEREITSTELLUNG = "bereitstellung/"

    /** Beiseitegelegtes aus Importen, für die Anzeige in den Einstellungen. */
    data class Beiseite(val konflikte: Int, val bilder: Int, val beschaedigt: Int) {
        val leer: Boolean get() = konflikte + bilder + beschaedigt == 0
    }

    fun zaehleBeiseite(importWurzel: File): Beiseite {
        var konflikte = 0
        var bilder = 0
        var beschaedigt = 0
        beiseiteDateien(importWurzel).forEach { (pfad, _) ->
            when {
                Q_KONFLIKT_PFAD.matches(pfad) -> konflikte++
                Q_BILD_PFAD.matches(pfad) -> bilder++
                Q_BESCHAEDIGT_PFAD.matches(pfad) -> beschaedigt++
            }
        }
        return Beiseite(konflikte, bilder, beschaedigt)
    }

    /**
     * Alle Dateien unter files/import als ZIP-Pfad (quarantaene/…) — ohne Bereitstellung und
     * Herkunftsbuch. Eigene Zwischendateien (*.tmp) bekommen den Pfad null.
     */
    private fun beiseiteDateien(importWurzel: File): List<Pair<String, File>> {
        if (!importWurzel.isDirectory) return emptyList()
        val basis = importWurzel.canonicalFile
        return basis.walkTopDown().filter { it.isFile }.mapNotNull { datei ->
            val relativ = datei.relativeTo(basis).invariantSeparatorsPath
            when {
                relativ.startsWith(IMPORT_BEREITSTELLUNG) || relativ in IMPORT_AUSGENOMMEN -> null
                relativ.endsWith(".tmp") -> "" to datei
                else -> (QUARANTAENE + relativ) to datei
            }
        }.sortedBy { it.first }.toList()
    }

    // --- Export ------------------------------------------------------------------------------------

    /**
     * Schreibt die Sicherung gestreamt in [ziel] und liefert, was geschrieben wurde. Wirft bei
     * jedem Fehler oder Abbruch — aufräumen muss der Aufrufer, denn nur er weiß, dass die Datei neu ist.
     */
    suspend fun exportiere(
        speicher: AusgabenSpeicher,
        resolver: ContentResolver,
        ziel: Uri,
        fortschritt: suspend (String, Float) -> Unit,
    ): SicherungsErgebnis {
        speicher.bereit()
        val eintraege = speicher.index.value
        val beschaedigt = speicher.beschaedigt.value.sorted()
        val jetzt = System.currentTimeMillis()
        val geschrieben = LinkedHashMap<String, Pair<Long, String>>()
        val arten = HashMap<String, String>()
        val bilder = LinkedHashSet<String>()
        val fehlendeBilder = mutableListOf<String>()
        var rohBeschaedigt = 0
        var uebersprungeneTemp = 0
        var quarantaene = Triple(0, 0, 0)

        val strom = resolver.openOutputStream(ziel, "w") ?: throw IllegalStateException("Die Zieldatei ließ sich nicht öffnen.")
        ZipOutputStream(BufferedOutputStream(strom, 64 * 1024)).use { zip ->
            fun schreibe(pfad: String, bytes: ByteArray, art: String, roh: Boolean) {
                val eintrag = ZipEntry(pfad)
                if (roh) {
                    // Bilder unkomprimiert: JPEG lässt sich kaum verkleinern, das spart nur Rechenzeit.
                    val crc = CRC32().apply { update(bytes) }
                    eintrag.method = ZipEntry.STORED
                    eintrag.size = bytes.size.toLong()
                    eintrag.compressedSize = bytes.size.toLong()
                    eintrag.crc = crc.value
                }
                zip.putNextEntry(eintrag)
                zip.write(bytes)
                zip.closeEntry()
                geschrieben[pfad] = bytes.size.toLong() to sha256(bytes)
                arten[pfad] = art
            }

            val gesamt = (eintraege.size + beschaedigt.size).coerceAtLeast(1)
            eintraege.forEachIndexed { nr, e ->
                coroutineContext.ensureActive()
                val datei = speicher.ausgabeDatei(e.id)
                // Eine im Archiv geführte Ausgabe fehlt: Die Sicherung wäre lückenhaft — lieber scheitern.
                if (!datei.exists()) throw IllegalStateException("Die Ausgabe ${e.id} fehlt lokal; die Sicherung wäre unvollständig.")
                val bytes = datei.readBytes()
                // Was geschrieben wird, bestimmt auch die Bilder — nicht der womöglich ältere Index.
                val ausgabe = runCatching { AusgabenSpeicher.ausJson(JSONObject(String(bytes, Charsets.UTF_8))) }.getOrNull()
                if (ausgabe == null) {
                    schreibe("beschaedigt/${e.id}.json", bytes, "beschaedigt", roh = false)
                    rohBeschaedigt++
                } else {
                    schreibe("ausgaben/${e.id}.json", bytes, "ausgabe", roh = false)
                    ausgabe.bloecke.forEach { b -> b.meldungen.forEach { m -> m.bildDatei?.let(bilder::add) } }
                }
                if (nr % 5 == 0) fortschritt("Sichere Ausgaben …", 0.1f * nr / gesamt)
            }
            beschaedigt.forEach { id ->
                coroutineContext.ensureActive()
                if (geschrieben.containsKey("beschaedigt/$id.json")) return@forEach
                val datei = speicher.ausgabeDatei(id)
                // Auch eine beschädigte Originaldatei gehört vollständig in die Sicherung — fehlt sie, lieber scheitern.
                if (!datei.exists()) throw IllegalStateException("Die beschädigte Ausgabe $id fehlt lokal; die Sicherung wäre unvollständig.")
                schreibe("beschaedigt/$id.json", datei.readBytes(), "beschaedigt", roh = false)
                rohBeschaedigt++
            }

            val bildListe = bilder.toList()
            bildListe.forEachIndexed { nr, name ->
                coroutineContext.ensureActive()
                val datei = speicher.bildDatei(name)
                if (datei == null || !BILD_PFAD.matches("bilder/$name")) {
                    fehlendeBilder += name
                    return@forEachIndexed
                }
                schreibe("bilder/$name", datei.readBytes(), "bild", roh = true)
                if (nr % 5 == 0) fortschritt("Sichere Bilder ${nr + 1} von ${bildListe.size} …", 0.1f + 0.5f * nr / bildListe.size.coerceAtLeast(1))
            }

            // Format 2: alles, was Importe beiseitegelegt haben — nichts davon fällt still weg.
            var qKonflikte = 0
            var qBilder = 0
            var qBeschaedigt = 0
            beiseiteDateien(speicher.importWurzel).forEach { (pfad, datei) ->
                coroutineContext.ensureActive()
                when {
                    pfad.isEmpty() -> uebersprungeneTemp++
                    Q_KONFLIKT_PFAD.matches(pfad) -> {
                        schreibe(pfad, datei.readBytes(), "quarantaene", roh = false)
                        qKonflikte++
                    }
                    Q_BILD_PFAD.matches(pfad) -> {
                        schreibe(pfad, datei.readBytes(), "quarantaene", roh = true)
                        qBilder++
                    }
                    Q_BESCHAEDIGT_PFAD.matches(pfad) -> {
                        schreibe(pfad, datei.readBytes(), "quarantaene", roh = false)
                        qBeschaedigt++
                    }
                    else -> throw IllegalStateException(
                        "Unbekannte Datei im Import-Bereich (${pfad.removePrefix(QUARANTAENE)}); die Sicherung wird abgebrochen, damit nichts still fehlt.",
                    )
                }
            }
            quarantaene = Triple(qKonflikte, qBilder, qBeschaedigt)
            fortschritt("Schließe die Sicherung ab …", 0.62f)

            val manifest = JSONObject()
                .put("format", FORMAT)
                .put("app", "NewsKompass")
                .put("appVersion", AppProfil.VERSION_NAME)
                .put("erstelltUm", jetzt)
                .put("anzahl", JSONObject().put("ausgaben", geschrieben.count { arten[it.key] == "ausgabe" })
                    .put("beschaedigt", rohBeschaedigt).put("bilder", geschrieben.count { arten[it.key] == "bild" })
                    .put("quarantaeneKonflikte", qKonflikte).put("quarantaeneBilder", qBilder).put("quarantaeneBeschaedigt", qBeschaedigt))
                .put("fehlendeBilder", JSONArray(fehlendeBilder))
                .put("uebersprungeneTemp", uebersprungeneTemp)
                .put("eintraege", JSONArray().apply {
                    geschrieben.forEach { (pfad, info) ->
                        put(JSONObject().put("pfad", pfad).put("art", arten[pfad]).put("groesse", info.first).put("sha256", info.second))
                    }
                })
            val manifestBytes = manifest.toString(2).toByteArray(Charsets.UTF_8)
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifestBytes)
            zip.closeEntry()
        }
        KompassLog.info(
            "ArchivSicherung",
            "exportiere",
            "Sicherung geschrieben",
            mapOf("eintraege" to geschrieben.size, "fehlendeBilder" to fehlendeBilder.size),
        )
        return SicherungsErgebnis(
            fehler = null,
            ausgaben = geschrieben.count { arten[it.key] == "ausgabe" },
            beschaedigt = rohBeschaedigt,
            bilder = geschrieben.count { arten[it.key] == "bild" },
            fehlendeBilder = fehlendeBilder.size,
            quarantaeneKonflikte = quarantaene.first,
            quarantaeneBilder = quarantaene.second,
            quarantaeneBeschaedigt = quarantaene.third,
            uebersprungeneTemp = uebersprungeneTemp,
            bytes = geschrieben.values.sumOf { it.first },
            erstelltUm = jetzt,
            eintraege = geschrieben.mapValues { it.value.second },
        )
    }

    // --- Prüfen ------------------------------------------------------------------------------------

    /**
     * Liest eine Sicherung vollständig und prüft sie, ohne irgendetwas lokal zu verändern:
     * erlaubte Pfade, keine Duplikate, Größengrenzen nach tatsächlich gelesenen Bytes, Manifest
     * vollständig und passend, jede lesbare Ausgabe parsebar, beschädigte Rohdateien nur byteweise.
     */
    suspend fun pruefe(resolver: ContentResolver, quelle: Uri, fortschritt: suspend (String) -> Unit): SicherungsErgebnis {
        val gelesen = HashMap<String, Pair<Long, String>>()
        val verweise = HashSet<String>()
        var manifestText: String? = null
        var gesamt = 0L
        var ausgaben = 0
        var beschaedigt = 0
        var bilder = 0
        var qKonflikte = 0
        var qBilder = 0
        var qBeschaedigt = 0

        val strom = resolver.openInputStream(quelle) ?: return SicherungsErgebnis("Die Datei ließ sich nicht öffnen.")
        try {
            ZipInputStream(strom.buffered(64 * 1024)).use { zip ->
                while (true) {
                    coroutineContext.ensureActive()
                    val eintrag = zip.nextEntry ?: break
                    val pfad = eintrag.name
                    if (pfad.startsWith("/") || pfad.contains("\\") || pfad.split('/').any { it == ".." || it == "." } || eintrag.isDirectory) {
                        return SicherungsErgebnis("Unzulässiger Pfad in der Sicherung: $pfad")
                    }
                    if (gelesen.containsKey(pfad) || (pfad == MANIFEST && manifestText != null)) {
                        return SicherungsErgebnis("Doppelter Eintrag in der Sicherung: $pfad")
                    }
                    if (gelesen.size >= MAX_EINTRAEGE) return SicherungsErgebnis("Die Sicherung hat unplausibel viele Einträge.")
                    val grenze = when {
                        pfad == MANIFEST -> MAX_JSON
                        AUSGABE_PFAD.matches(pfad) || BESCHAEDIGT_PFAD.matches(pfad) -> MAX_JSON
                        BILD_PFAD.matches(pfad) -> MAX_BILD
                        Q_KONFLIKT_PFAD.matches(pfad) || Q_BESCHAEDIGT_PFAD.matches(pfad) -> MAX_JSON
                        Q_BILD_PFAD.matches(pfad) -> MAX_BILD
                        else -> return SicherungsErgebnis("Unerwarteter Eintrag in der Sicherung: $pfad")
                    }
                    val behalten = pfad == MANIFEST || AUSGABE_PFAD.matches(pfad)
                    val (groesse, pruefsumme, inhalt) = lies(zip, grenze, behalten)
                        ?: return SicherungsErgebnis("Eintrag zu groß: $pfad")
                    gesamt += groesse
                    if (gesamt > MAX_GESAMT) return SicherungsErgebnis("Die Sicherung ist unplausibel groß.")
                    when {
                        pfad == MANIFEST -> manifestText = String(inhalt!!, Charsets.UTF_8)
                        AUSGABE_PFAD.matches(pfad) -> {
                            val id = AUSGABE_PFAD.matchEntire(pfad)!!.groupValues[1]
                            val ausgabe = runCatching { AusgabenSpeicher.ausJson(JSONObject(String(inhalt!!, Charsets.UTF_8))) }.getOrNull()
                                ?: return SicherungsErgebnis("Ausgabe $id in der Sicherung ist unlesbar.")
                            if (ausgabe.id != id) return SicherungsErgebnis("Ausgabe $id trägt eine andere Nummer.")
                            ausgabe.bloecke.forEach { b -> b.meldungen.forEach { m -> m.bildDatei?.let(verweise::add) } }
                            ausgaben++
                        }
                        BESCHAEDIGT_PFAD.matches(pfad) -> beschaedigt++
                        BILD_PFAD.matches(pfad) -> bilder++
                        // Beiseitegelegtes wird nur byteweise geprüft, nie geparst oder übernommen.
                        Q_KONFLIKT_PFAD.matches(pfad) -> qKonflikte++
                        Q_BILD_PFAD.matches(pfad) -> qBilder++
                        else -> qBeschaedigt++
                    }
                    if (pfad != MANIFEST) gelesen[pfad] = groesse to pruefsumme
                    if ((ausgaben + bilder) % 10 == 0) fortschritt("Prüfe ${gelesen.size} Einträge …")
                }
            }
        } catch (abbruch: kotlinx.coroutines.CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            return SicherungsErgebnis("Die Datei ist keine lesbare ZIP-Sicherung (${fehler.javaClass.simpleName}).")
        }

        val manifest = manifestText?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return SicherungsErgebnis("Das Manifest fehlt oder ist unlesbar — die Sicherung ist unvollständig.")
        val format = manifest.optInt("format", -1)
        if (format !in LESBARE_FORMATE) return SicherungsErgebnis("Unbekanntes Sicherungsformat $format; bitte die App aktualisieren.")
        if (format == 1 && qKonflikte + qBilder + qBeschaedigt > 0) {
            return SicherungsErgebnis("Eine Sicherung im Format 1 darf keine beiseitegelegten Importdaten enthalten.")
        }
        val liste = manifest.optJSONArray("eintraege") ?: return SicherungsErgebnis("Das Manifest nennt keine Einträge.")
        val erwartet = HashMap<String, Pair<Long, String>>()
        for (i in 0 until liste.length()) {
            val e = liste.getJSONObject(i)
            val pfad = e.getString("pfad")
            if (erwartet.put(pfad, e.getLong("groesse") to e.getString("sha256")) != null) {
                return SicherungsErgebnis("Das Manifest nennt $pfad doppelt.")
            }
        }
        erwartet.forEach { (pfad, info) ->
            val ist = gelesen[pfad] ?: return SicherungsErgebnis("Eintrag fehlt in der Sicherung: $pfad")
            if (ist.first != info.first) return SicherungsErgebnis("Größe weicht ab: $pfad")
            if (ist.second != info.second) return SicherungsErgebnis("Prüfsumme weicht ab: $pfad")
        }
        gelesen.keys.firstOrNull { it !in erwartet }?.let { return SicherungsErgebnis("Eintrag steht nicht im Manifest: $it") }
        val anzahl = manifest.optJSONObject("anzahl") ?: return SicherungsErgebnis("Das Manifest nennt keine Anzahlen.")
        val qPasst = format == 1 || (
            anzahl.optInt("quarantaeneKonflikte", -1) == qKonflikte &&
                anzahl.optInt("quarantaeneBilder", -1) == qBilder &&
                anzahl.optInt("quarantaeneBeschaedigt", -1) == qBeschaedigt
            )
        if (anzahl.optInt("ausgaben", -1) != ausgaben || anzahl.optInt("beschaedigt", -1) != beschaedigt || anzahl.optInt("bilder", -1) != bilder || !qPasst) {
            return SicherungsErgebnis("Die Anzahlen im Manifest passen nicht zu den gelesenen Einträgen.")
        }
        val fehlend = manifest.optJSONArray("fehlendeBilder")?.let { a -> (0 until a.length()).map(a::getString).toSet() }.orEmpty()
        verweise.firstOrNull { "bilder/$it" !in gelesen && it !in fehlend }?.let {
            return SicherungsErgebnis("Ein Bild, auf das eine Ausgabe zeigt, fehlt ohne Vermerk: $it")
        }
        return SicherungsErgebnis(
            fehler = null,
            ausgaben = ausgaben,
            beschaedigt = beschaedigt,
            bilder = bilder,
            fehlendeBilder = fehlend.size,
            quarantaeneKonflikte = qKonflikte,
            quarantaeneBilder = qBilder,
            quarantaeneBeschaedigt = qBeschaedigt,
            uebersprungeneTemp = manifest.optInt("uebersprungeneTemp", 0),
            bytes = gesamt,
            erstelltUm = manifest.optLong("erstelltUm"),
            eintraege = gelesen.mapValues { it.value.second },
        )
    }

    /** Liest einen Eintrag mit harter Grenze über die tatsächlich gelesenen Bytes; null = zu groß. */
    private fun lies(strom: InputStream, grenze: Long, behalten: Boolean): Triple<Long, String, ByteArray?>? {
        val digest = MessageDigest.getInstance("SHA-256")
        val puffer = ByteArray(64 * 1024)
        val sammler = if (behalten) ByteArrayOutputStream() else null
        var groesse = 0L
        while (true) {
            val n = strom.read(puffer)
            if (n < 0) break
            groesse += n
            if (groesse > grenze) return null
            digest.update(puffer, 0, n)
            sammler?.write(puffer, 0, n)
        }
        return Triple(groesse, digest.digest().joinToString("") { "%02x".format(it) }, sammler?.toByteArray())
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
