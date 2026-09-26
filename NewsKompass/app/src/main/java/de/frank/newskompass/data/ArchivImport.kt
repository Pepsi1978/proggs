package de.frank.newskompass.data

import android.content.ContentResolver
import android.net.Uri
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.observability.KompassLog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject

/** Was ein Import bewirkt hat; [fehler] gesetzt heißt: nichts oder nur Vollständiges wurde übernommen. */
data class ImportErgebnis(
    val fehler: String? = null,
    val neu: Int = 0,
    val doppelt: Int = 0,
    val konflikte: Int = 0,
    val konflikteLokalBeschaedigt: Int = 0,
    val beschaedigtGesichert: Int = 0,
    val bilderNeu: Int = 0,
    val bilderVorhanden: Int = 0,
    val bilderUmbenannt: Int = 0,
    /** Bilder, die für neu übernommene Ausgaben weder in der Sicherung noch lokal liegen. */
    val fehlendeBilder: Int = 0,
    /** Bilder, die schon beim Sichern fehlten — die gewählte Datei ist unvollständig, egal was lokal liegt. */
    val sicherungFehlendeBilder: Int = 0,
    val quarantaene: String? = null,
)

/**
 * Importiert eine Archivsicherung, ohne irgendetwas Lokales zu überschreiben oder zu löschen.
 *
 * Ablauf:
 * 1. Die ZIP-Datei wird mit derselben Logik wie „Sicherung prüfen“ vollständig validiert.
 * 2. Ein zweiter Durchgang legt jeden Eintrag in einem eigenen Bereitstellungsordner ab und
 *    vergleicht ihn mit der eben geprüften Prüfsumme — erst danach ändert sich lokal etwas.
 * 3. Planen: neue Ausgaben übernehmen, gleiche überspringen, abweichende beiseitelegen; Bilder mit
 *    gleichem Namen und anderem Inhalt bekommen einen neuen, aus dem Inhalt abgeleiteten Namen.
 * 4. Erst alle Bilder atomar, dann jede Ausgabe atomar, zuletzt den Index neu aufbauen.
 *
 * Ein abgebrochener Import lässt sich wiederholen: Jede Ausgabe ist für sich vollständig, und als
 * schon übernommen gilt nur, was lokal byte-gleich mit der Quelle oder mit genau der Fassung ist, die
 * dieser Import schreiben würde — umbenannte Bilder heißen beim zweiten Mal genauso. Das
 * Herkunftsbuch ist nur ein Protokoll und entscheidet nichts.
 */
object ArchivImport {

    suspend fun importiere(
        speicher: AusgabenSpeicher,
        resolver: ContentResolver,
        quelle: Uri,
        fortschritt: suspend (String, Float) -> Unit,
    ): ImportErgebnis {
        // 1. Vollständig prüfen, bevor irgendetwas angefasst wird.
        fortschritt("Prüfe die Sicherung …", 0.05f)
        val pruefung = ArchivSicherung.pruefe(resolver, quelle) { text -> fortschritt(text, 0.15f) }
        pruefung.fehler?.let { return ImportErgebnis(fehler = "Die Sicherung ist nicht in Ordnung: $it Es wurde nichts übernommen.") }
        val platz = speicher.importWurzel.apply { mkdirs() }.usableSpace
        if (pruefung.bytes * 2 + RESERVE > platz) {
            return ImportErgebnis(fehler = "Zu wenig freier Speicher für den Import. Es wurde nichts übernommen.")
        }

        val lauf = System.currentTimeMillis().toString()
        val bereitstellung = File(speicher.importWurzel, "bereitstellung").also { it.deleteRecursively() }
        try {
            // 2. Bereitstellen und jede Datei gegen die geprüfte Prüfsumme halten.
            fortschritt("Stelle die Sicherung bereit …", 0.25f)
            stelleBereit(resolver, quelle, bereitstellung, pruefung.eintraege)?.let { return ImportErgebnis(fehler = "$it Es wurde nichts übernommen.") }

            // 3. Planen — noch ohne lokale Änderung.
            fortschritt("Gleiche mit dem lokalen Archiv ab …", 0.5f)
            speicher.bereit()
            val lokalBeschaedigt = speicher.beschaedigt.value
            val quarantaeneOrdner = File(speicher.importWurzel, "konflikte/$lauf")
            val kandidaten = mutableListOf<Kandidat>()
            var doppelt = 0
            var konflikte = 0
            var konflikteLokalBeschaedigt = 0
            var beschaedigtGesichert = 0

            pruefung.eintraege.keys.sorted().forEach { pfad ->
                coroutineContext.ensureActive()
                val datei = File(bereitstellung, pfad)
                ArchivSicherung.AUSGABE_PFAD.matchEntire(pfad)?.let { treffer ->
                    val id = treffer.groupValues[1]
                    val text = datei.readText()
                    kandidaten += Kandidat(id, pfad, pruefung.eintraege.getValue(pfad), text, AusgabenSpeicher.ausJson(JSONObject(text)))
                }
                ArchivSicherung.BESCHAEDIGT_PFAD.matchEntire(pfad)?.let { treffer ->
                    val id = treffer.groupValues[1]
                    val lokal = speicher.ausgabeDatei(id)
                    val ziel = File(speicher.importWurzel, "beschaedigt/$id.json")
                    when {
                        lokal.exists() && sha256(lokal) == pruefung.eintraege.getValue(pfad) -> doppelt++
                        ziel.exists() && sha256(ziel) == pruefung.eintraege.getValue(pfad) -> doppelt++
                        // Beschädigtes landet nie im Archiv selbst, sondern getrennt — und ersetzt nichts.
                        !ziel.exists() -> {
                            legeBeiseite(datei, ziel)
                            beschaedigtGesichert++
                        }
                        else -> {
                            legeBeiseite(datei, File(quarantaeneOrdner, pfad))
                            konflikte++
                        }
                    }
                }
            }

            // Bildnamen für alle Kandidaten festlegen — nur lesend und deterministisch: Gleicher Name mit
            // anderem Inhalt bekommt einen aus dem Inhalt abgeleiteten Namen, beim Wiederholen denselben.
            val umbenennung = HashMap<String, String>()
            kandidaten.flatMap { k -> k.bilder() }.toSet().forEach { name ->
                val quelleBild = File(bereitstellung, "bilder/$name")
                val lokal = File(speicher.bilderOrdner, name)
                if (quelleBild.exists() && lokal.exists() && sha256(lokal) != sha256(quelleBild)) {
                    umbenennung[name] = inhaltsName(quelleBild, speicher.bilderOrdner)
                }
            }

            // Ausgaben: Als vorhanden gilt nur, was lokal byte-gleich ist — mit der Quelldatei oder mit genau
            // der Fassung, die dieser Import schreiben würde (nach einem abgebrochenen Lauf mit umbenannten Bildern).
            // Jede andere lokale Fassung bleibt, die importierte kommt samt Bildern in die Quarantäne.
            val neueAusgaben = mutableListOf<Pair<Kandidat, Pair<Ausgabe, String>>>()
            val konfliktBilder = HashSet<String>()
            kandidaten.forEach { k ->
                coroutineContext.ensureActive()
                val fassung = k.fassung(umbenennung)
                val lokal = speicher.ausgabeDatei(k.id)
                if (!lokal.exists()) {
                    neueAusgaben += k to fassung
                    return@forEach
                }
                val lokalSha = sha256(lokal)
                if (lokalSha == k.quellSha || lokalSha == SicheresSchreiben.sha256(fassung.second)) {
                    doppelt++
                } else {
                    legeBeiseite(File(bereitstellung, k.pfad), File(quarantaeneOrdner, k.pfad))
                    konfliktBilder += k.bilder()
                    if (k.id in lokalBeschaedigt) konflikteLokalBeschaedigt++ else konflikte++
                }
            }
            konfliktBilder.forEach { name ->
                val quelleBild = File(bereitstellung, "bilder/$name")
                if (quelleBild.exists()) legeBeiseite(quelleBild, File(quarantaeneOrdner, "bilder/$name"))
            }

            // Bilder: nur die, auf die neu übernommene Ausgaben zeigen.
            val zuKopieren = mutableListOf<Pair<File, String>>()
            var bilderVorhanden = 0
            var fehlendeBilder = 0
            neueAusgaben.flatMap { (k, _) -> k.bilder() }.toSet().forEach { name ->
                val quelleBild = File(bereitstellung, "bilder/$name")
                val zielName = umbenennung[name] ?: name
                when {
                    !quelleBild.exists() -> if (File(speicher.bilderOrdner, name).exists()) bilderVorhanden++ else fehlendeBilder++
                    File(speicher.bilderOrdner, zielName).exists() -> bilderVorhanden++
                    else -> zuKopieren += quelleBild to zielName
                }
            }

            // 4a. Bilder zuerst — jedes atomar, nie über ein vorhandenes.
            zuKopieren.forEachIndexed { nr, (quelleBild, name) ->
                coroutineContext.ensureActive()
                kopiereAtomar(quelleBild, File(speicher.bilderOrdner, name))
                if (nr % 5 == 0) fortschritt("Übernehme Bilder ${nr + 1} von ${zuKopieren.size} …", 0.55f + 0.2f * nr / zuKopieren.size.coerceAtLeast(1))
            }

            // 4b. Dann jede Ausgabe atomar. Die Herkunft wird erst nach erfolgreichem Schreiben vermerkt und
            // entscheidet nichts: Ein Wiederholungslauf erkennt Übernommenes allein an den Bytes.
            var neu = 0
            neueAusgaben.forEachIndexed { nr, (k, fassung) ->
                coroutineContext.ensureActive()
                if (speicher.importiereAusgabe(fassung.first, fassung.second)) {
                    neu++
                    runCatching { speicher.merkeHerkunft(k.id, k.quellSha) }
                } else {
                    doppelt++
                }
                if (nr % 5 == 0) fortschritt("Übernehme Ausgaben ${nr + 1} von ${neueAusgaben.size} …", 0.75f + 0.2f * nr / neueAusgaben.size.coerceAtLeast(1))
            }

            // 4c. Index aus den Dateien neu aufbauen.
            fortschritt("Baue das Archiv neu auf …", 0.97f)
            speicher.lade()

            val ergebnis = ImportErgebnis(
                neu = neu,
                doppelt = doppelt,
                konflikte = konflikte,
                konflikteLokalBeschaedigt = konflikteLokalBeschaedigt,
                beschaedigtGesichert = beschaedigtGesichert,
                bilderNeu = zuKopieren.size,
                bilderVorhanden = bilderVorhanden,
                bilderUmbenannt = zuKopieren.count { (quelleBild, name) -> name != quelleBild.name },
                fehlendeBilder = fehlendeBilder,
                sicherungFehlendeBilder = pruefung.fehlendeBilder,
                quarantaene = if (konflikte + konflikteLokalBeschaedigt > 0) quarantaeneOrdner.relativeTo(speicher.importWurzel.parentFile!!).path else null,
            )
            KompassLog.info(
                "ArchivImport",
                "importiere",
                "Import fertig",
                mapOf("neu" to neu, "doppelt" to doppelt, "konflikte" to konflikte + konflikteLokalBeschaedigt, "bilderNeu" to zuKopieren.size, "fehlendeBilder" to fehlendeBilder),
            )
            return ergebnis
        } finally {
            // Nur die eigene Bereitstellung verschwindet; übernommene Dateien und Quarantäne bleiben.
            bereitstellung.deleteRecursively()
        }
    }

    /** Eine Ausgabe aus der Sicherung, samt Quelltext und dessen Prüfsumme. */
    private class Kandidat(val id: String, val pfad: String, val quellSha: String, val text: String, val ausgabe: Ausgabe) {
        fun bilder(): Set<String> = ausgabe.bloecke.flatMap { b -> b.meldungen.mapNotNull { it.bildDatei } }.toSet()

        /** Die Fassung, die geschrieben würde: unverändert der Quelltext, sonst mit umbenannten Bildern. */
        fun fassung(umbenennung: Map<String, String>): Pair<Ausgabe, String> {
            val umgeschrieben = ausgabe.copy(
                bloecke = ausgabe.bloecke.map { b ->
                    b.copy(meldungen = b.meldungen.map { m -> m.bildDatei?.let(umbenennung::get)?.let { m.copy(bildDatei = it) } ?: m })
                },
            )
            return if (umgeschrieben == ausgabe) ausgabe to text else umgeschrieben to AusgabenSpeicher.zuJson(umgeschrieben).toString()
        }
    }

    /**
     * Zweiter Durchgang: legt jeden geprüften Eintrag ab und hält ihn gegen die Prüfsumme des
     * ersten Durchgangs. Liefert einen Fehlertext, wenn sich die Datei inzwischen verändert hat.
     */
    private suspend fun stelleBereit(resolver: ContentResolver, quelle: Uri, ordner: File, erwartet: Map<String, String>): String? {
        val abgelegt = HashSet<String>()
        val strom = resolver.openInputStream(quelle) ?: return "Die Datei ließ sich nicht erneut öffnen."
        ZipInputStream(strom.buffered(64 * 1024)).use { zip ->
            while (true) {
                coroutineContext.ensureActive()
                val eintrag = zip.nextEntry ?: break
                val pfad = eintrag.name
                if (pfad == ArchivSicherung.MANIFEST) continue
                val sha = erwartet[pfad] ?: return "Die Sicherung hat sich seit der Prüfung verändert ($pfad)."
                if (!abgelegt.add(pfad)) return "Doppelter Eintrag in der Sicherung: $pfad"
                val grenze = if (ArchivSicherung.BILD_PFAD.matches(pfad)) ArchivSicherung.MAX_BILD else ArchivSicherung.MAX_JSON
                val ziel = File(ordner, pfad)
                // Nur Pfade aus der geprüften Liste, trotzdem sicherheitshalber im Bereitstellungsordner halten.
                if (!ziel.canonicalPath.startsWith(ordner.canonicalPath + File.separator)) return "Unzulässiger Pfad in der Sicherung: $pfad"
                ziel.parentFile?.mkdirs()
                val digest = MessageDigest.getInstance("SHA-256")
                var groesse = 0L
                FileOutputStream(ziel).use { aus ->
                    val puffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = zip.read(puffer)
                        if (n < 0) break
                        groesse += n
                        if (groesse > grenze) return "Eintrag zu groß: $pfad"
                        digest.update(puffer, 0, n)
                        aus.write(puffer, 0, n)
                    }
                    aus.fd.sync()
                }
                if (hex(digest.digest()) != sha) return "Die Sicherung hat sich seit der Prüfung verändert ($pfad)."
            }
        }
        if (abgelegt.size != erwartet.size) return "Die Sicherung hat sich seit der Prüfung verändert (Einträge fehlen)."
        return null
    }

    /** Kopiert über eine Zwischendatei mit fsync und benennt erst dann um — nie über eine vorhandene Datei. */
    private fun kopiereAtomar(quelle: File, ziel: File) {
        if (ziel.exists()) throw IOException("Ziel existiert bereits: ${ziel.name}")
        ziel.parentFile?.mkdirs()
        val zwischen = File(ziel.parentFile, "${ziel.name}.${UUID.randomUUID()}.tmp")
        try {
            quelle.inputStream().use { ein -> FileOutputStream(zwischen).use { aus -> ein.copyTo(aus); aus.fd.sync() } }
            if (sha256(zwischen) != sha256(quelle)) throw IOException("Kopie weicht ab: ${ziel.name}")
            if (ziel.exists()) throw IOException("Ziel existiert bereits: ${ziel.name}")
            if (!zwischen.renameTo(ziel)) throw IOException("Umbenennen gescheitert: ${ziel.name}")
        } finally {
            zwischen.delete()
        }
    }

    /** Legt eine Datei aus der Bereitstellung beiseite (Quarantäne oder getrennte Rohablage), ohne etwas zu überschreiben. */
    private fun legeBeiseite(quelle: File, ziel: File) {
        if (ziel.exists()) return
        kopiereAtomar(quelle, ziel)
    }

    /** Neuer Bildname aus dem Inhalt — derselbe Inhalt bekommt beim Wiederholen denselben Namen. */
    private fun inhaltsName(bild: File, ordner: File): String {
        val abgeleitet = UUID.nameUUIDFromBytes("newskompass-bild:${sha256(bild)}".toByteArray()).toString() + ".jpg"
        val vorhanden = File(ordner, abgeleitet)
        return if (!vorhanden.exists() || sha256(vorhanden) == sha256(bild)) abgeleitet else "${UUID.randomUUID()}.jpg"
    }

    private fun sha256(datei: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        datei.inputStream().use { ein ->
            val puffer = ByteArray(64 * 1024)
            while (true) {
                val n = ein.read(puffer)
                if (n < 0) break
                digest.update(puffer, 0, n)
            }
        }
        return hex(digest.digest())
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private const val RESERVE = 100L * 1024 * 1024
}
