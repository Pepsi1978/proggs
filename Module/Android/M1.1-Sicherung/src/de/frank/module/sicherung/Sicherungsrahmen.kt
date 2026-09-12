// ──────────────────────────────────────────────────────────────────────
// Modul M1.1 — Sicherung · Stand v1
// Quelle: Module/Android/M1.1-Sicherung/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.1 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sicherung

import android.util.JsonReader
import android.util.JsonWriter
import java.io.Reader
import java.io.Writer
import java.security.MessageDigest

class SicherungsFehler(meldung: String, ursache: Throwable? = null) : Exception(meldung, ursache)

/** Was in einer Sicherungsdatei steht — und was der Benutzer vor dem Einspielen sieht. */
data class SicherungsVorschau(
    val schema: Int,
    val erstelltAm: String,
    val datenmodellVersion: Int,
    val zahlen: Nutzlastzahlen = Nutzlastzahlen(),
    /** Was beim Sichern angehakt war. Bei alten Dateien leer. */
    val umfang: Set<String> = emptySet(),
)

/**
 * Eine Prüfsumme über die Inhalte, nicht über die Datei.
 *
 * Gerechnet wird über die Werte in der Reihenfolge, in der sie geschrieben werden — nicht über
 * die Bytes der Datei. Damit ist sie unabhängig von Einrückung, Zeilenenden und davon, ob ein
 * Schreiber Felder anders anordnet: Geprüft wird, was ankommt, nicht wie es formatiert ist.
 *
 * Zwischen zwei Werten steht ein Nullzeichen als Trenner. Ohne ihn ergäben „ab"+„c" und
 * „a"+„bc" dieselbe Summe, und ein verrutschtes Feld fiele nicht auf.
 */
class Inhaltspruefsumme {
    private val digest = MessageDigest.getInstance("SHA-256")

    fun nimm(vararg werte: Any?) {
        werte.forEach { wert ->
            digest.update(wert.toString().toByteArray(Charsets.UTF_8))
            digest.update(0)
        }
    }

    fun fertig(): String = digest.digest().joinToString("") { "%02x".format(it) }
}

/**
 * Der Umschlag der Sicherungsdatei — Kopf, Fußzeile, Prüfsumme, Vollständigkeit.
 *
 * Satzweise heißt: Es steht nie der gesamte Bestand gleichzeitig im Speicher. Beim Schreiben
 * wandert jeder Satz direkt in den Ausgabestrom, beim Lesen kommt jeder Satz einzeln heraus.
 * Lägen stattdessen alle Sätze UND die fertige JSON-Zeichenkette zusammen im Arbeitsspeicher,
 * wäre das die Stelle, die bei großen Beständen als erste kippt.
 *
 * Am Ende der Datei stehen eine Prüfsumme über die Inhalte und die Anzahl je Art. Eine Datei,
 * deren Übertragung abbrach, fällt damit **vor** dem Einspielen auf und nicht als rätselhafter
 * Fehler mittendrin.
 *
 * **Was hier steht, ist app-unabhängig.** Die benannten Nutzlast-Felder dazwischen schreibt und
 * liest [SicherungsInhalt] — deshalb bleibt eine bestehende Datei Byte für Byte wie bisher.
 */
class Sicherungsrahmen(
    private val inhalt: SicherungsInhalt,
    private val protokoll: SicherungsProtokoll = StillesProtokoll,
) {

    /** Schreibt die Sicherung satzweise nach [ziel]. Zurück kommt, was tatsächlich drinsteht. */
    suspend fun schreibe(
        ziel: Writer,
        erstelltAm: String,
        umfang: Set<SicherungsTeil>,
    ): Nutzlastzahlen {
        val pruefsumme = Inhaltspruefsumme()
        val schreiber = JsonWriter(ziel).apply { setIndent(" ") }

        schreiber.beginObject()
        schreiber.name("schema").value(SCHEMA_VERSION.toLong())
        schreiber.name("app").value(inhalt.produkt)
        schreiber.name("erstelltAm").value(erstelltAm)
        schreiber.name("roomVersion").value(inhalt.datenmodellVersion.toLong())
        schreiber.name("umfang").beginArray()
        umfang.forEach { schreiber.value(it.id) }
        schreiber.endArray()

        val zahlen = inhalt.schreibeNutzlast(schreiber, umfang, pruefsumme)

        schreiber.name("anzahl").beginObject()
        zahlen.anzahl.forEach { (art, wieViele) -> schreiber.name(art).value(wieViele.toLong()) }
        schreiber.endObject()
        schreiber.name("jeBereich").beginObject()
        zahlen.jeBereich.forEach { (bereich, wieViele) -> schreiber.name(bereich).value(wieViele.toLong()) }
        schreiber.endObject()
        schreiber.name("pruefsumme").value(pruefsumme.fertig())
        schreiber.endObject()
        schreiber.flush()

        protokoll.info("Sicherung", "schreibe", "Sicherung geschrieben", buildMap {
            put("umfang", umfang.joinToString(",") { it.id })
            zahlen.anzahl.forEach { (art, wieViele) -> put(art, wieViele) }
        })
        return zahlen
    }

    /**
     * Liest nur Kopf und Zählwerke — für die Vorschau, ohne etwas einzuspielen.
     *
     * Die Datei wird dabei vollständig durchlaufen und die Prüfsumme nachgerechnet. Das ist
     * zugleich die Prüfung, ob die Datei heil angekommen ist: Eine abgebrochene Übertragung
     * fällt hier auf und nicht erst mitten im Einspielen.
     */
    suspend fun pruefe(quelle: Reader): SicherungsVorschau = lies(quelle, einspielen = false)

    /** Liest die Datei und legt die Sätze über [SicherungsInhalt] an. Prüft dasselbe wie [pruefe]. */
    suspend fun spieleEin(quelle: Reader): SicherungsVorschau = lies(quelle, einspielen = true)

    private suspend fun lies(quelle: Reader, einspielen: Boolean): SicherungsVorschau {
        val leser = JsonReader(quelle)
        var schema = -1
        var app = ""
        var erstelltAm = ""
        var datenmodellVersion = 0
        var umfang = emptySet<String>()
        var erwartet: Map<String, Int>? = null
        var erwarteteSumme = ""
        var jeBereichKopf = emptyMap<String, Int>()
        val pruefsumme = Inhaltspruefsumme()
        var gezaehlt = Nutzlastzahlen()

        try {
            leser.beginObject()
            while (leser.hasNext()) {
                when (val feld = leser.nextName()) {
                    "schema" -> schema = leser.nextInt()
                    "app" -> app = leser.nextString()
                    "erstelltAm" -> erstelltAm = leser.nextString()
                    "roomVersion" -> datenmodellVersion = leser.nextInt()
                    "pruefsumme" -> erwarteteSumme = leser.nextString()
                    "umfang" -> {
                        val gelesen = mutableSetOf<String>()
                        leser.beginArray()
                        while (leser.hasNext()) gelesen.add(leser.nextString())
                        leser.endArray()
                        umfang = gelesen
                    }
                    "anzahl" -> erwartet = liesZahlen(leser)
                    "jeBereich" -> jeBereichKopf = liesZahlen(leser)
                    // Alles Übrige ist Nutzlast und gehört der App. Kennt sie das Feld nicht,
                    // wird es übergangen — eine Datei aus einer neueren Fassung bleibt so lesbar.
                    else -> {
                        val daraus = inhalt.liesNutzlast(feld, leser, pruefsumme, einspielen)
                        if (daraus == null) {
                            protokoll.info("Sicherung", "lies", "Unbekanntes Feld übergangen", mapOf("feld" to feld))
                            leser.skipValue()
                        } else {
                            gezaehlt += daraus
                        }
                    }
                }
            }
            leser.endObject()
        } catch (fehler: SicherungsFehler) {
            throw fehler
        } catch (fehler: Exception) {
            throw SicherungsFehler(
                "Die Datei ist unvollständig oder beschädigt und wurde nicht eingespielt.",
                fehler,
            )
        }

        pruefeKopf(schema, app, datenmodellVersion)
        pruefeVollstaendigkeit(erwartet, gezaehlt.anzahl, erwarteteSumme, pruefsumme.fertig())

        return SicherungsVorschau(
            schema = schema,
            erstelltAm = erstelltAm,
            datenmodellVersion = datenmodellVersion,
            zahlen = Nutzlastzahlen(
                anzahl = gezaehlt.anzahl,
                // Der Kopf ist genauer als das Gezählte: Bei der Vorschau ohne Einspielen
                // liefert die App womöglich keine Bereichsaufteilung mit.
                jeBereich = jeBereichKopf.ifEmpty { gezaehlt.jeBereich },
            ),
            umfang = umfang,
        )
    }

    private fun liesZahlen(leser: JsonReader): Map<String, Int> {
        val werte = mutableMapOf<String, Int>()
        leser.beginObject()
        while (leser.hasNext()) werte[leser.nextName()] = leser.nextInt()
        leser.endObject()
        return werte
    }

    private fun pruefeKopf(schema: Int, app: String, datenmodellVersion: Int) {
        // Auch unter einem früheren Namen geschriebene Sicherungen gehören zu dieser App.
        // Ohne das hätte jede Umbenennung sämtliche bis dahin geschriebenen Dateien entwertet.
        if (app.isNotBlank() && app != inhalt.produkt && app !in inhalt.fruehereNamen) {
            throw SicherungsFehler("Diese Sicherung gehört nicht zu ${inhalt.produkt}.")
        }
        if (schema < 0) {
            throw SicherungsFehler("In der Datei fehlt die Angabe, nach welchem Schema sie erstellt wurde.")
        }
        if (schema > SCHEMA_VERSION) {
            throw SicherungsFehler(
                "Diese Sicherung stammt aus einer neueren Fassung der App (Schema $schema, " +
                    "diese App kennt $SCHEMA_VERSION). Aktualisier zuerst die App — " +
                    "eine halb eingelesene Sicherung wäre schlimmer als keine.",
            )
        }
        // Die Fassung des Datenmodells ist eine andere Frage als die des Dateiformats: Das
        // Format kann gleich bleiben, während eine Spalte dazukommt. Eine Datei aus einem
        // neueren Datenmodell kann Felder mitbringen, die hier noch niemand einordnen kann.
        if (datenmodellVersion > inhalt.datenmodellVersion) {
            throw SicherungsFehler(
                "Diese Sicherung stammt aus einer neueren Datenbank (Fassung $datenmodellVersion, " +
                    "diese App hat ${inhalt.datenmodellVersion}). Aktualisier zuerst die App.",
            )
        }
    }

    private fun pruefeVollstaendigkeit(
        erwartet: Map<String, Int>?,
        gezaehlt: Map<String, Int>,
        erwarteteSumme: String,
        gerechneteSumme: String,
    ) {
        // Dateien vor Schema 3 bringen weder Zählwerk noch Prüfsumme mit. Was nicht dasteht,
        // kann nicht geprüft werden — abzulehnen wäre hier falsch.
        if (erwartet != null && erwartet.any { (art, wieViele) -> (gezaehlt[art] ?: 0) != wieViele }) {
            val angekuendigt = erwartet.entries.joinToString(", ") { "${it.value} ${it.key}" }
            val vorhanden = erwartet.keys.joinToString(", ") { "${gezaehlt[it] ?: 0} $it" }
            throw SicherungsFehler(
                "Die Datei ist unvollständig: Sie kündigt $angekuendigt an, " +
                    "enthält aber $vorhanden. Nimm die Sicherung davor.",
            )
        }
        if (erwarteteSumme.isNotBlank() && erwarteteSumme != gerechneteSumme) {
            throw SicherungsFehler(
                "Der Inhalt der Datei stimmt nicht mit ihrer Prüfsumme überein. Sie ist bei der " +
                    "Übertragung beschädigt worden. Nimm die Sicherung davor.",
            )
        }
    }

    companion object {
        /**
         * 1: nur der selbst erarbeitete Anteil.
         * 2: wählbarer Umfang, Wissensbereiche vollständig.
         * 3: Prüfsumme, Anzahlen und die Fassung des Datenmodells im Kopf.
         *
         * Ältere Dateien bleiben lesbar; was sie nicht mitbringen, wird nicht geprüft.
         */
        const val SCHEMA_VERSION = 3

        /** So viele Sätze holt die App je Abfrage aus ihrer Datenbank. */
        const val SEITE = 200

        fun baueDateiname(praefix: String, zeitstempel: String): String =
            "$praefix-sicherung-$zeitstempel.json"

        /** Alle Felder eines flachen Objekts als Text — verschachtelte Werte werden übergangen. */
        fun liesFelder(leser: JsonReader): Map<String, String> {
            val werte = mutableMapOf<String, String>()
            leser.beginObject()
            while (leser.hasNext()) {
                val feld = leser.nextName()
                when (leser.peek()) {
                    android.util.JsonToken.STRING -> werte[feld] = leser.nextString()
                    android.util.JsonToken.NUMBER -> werte[feld] = leser.nextString()
                    android.util.JsonToken.BOOLEAN -> werte[feld] = leser.nextBoolean().toString()
                    android.util.JsonToken.NULL -> { leser.nextNull(); werte[feld] = "" }
                    else -> leser.skipValue()
                }
            }
            leser.endObject()
            return werte
        }
    }
}
