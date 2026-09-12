package de.frank.kompass.data

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import de.frank.kompass.AppProfil
import de.frank.kompass.data.local.ChatNachrichtEntity
import de.frank.kompass.data.local.ChatSitzungEntity
import de.frank.kompass.data.local.EintragEntity
import de.frank.kompass.data.local.FrageEntity
import de.frank.kompass.data.local.KompassDatabase
import de.frank.kompass.data.model.SicherungsTeil
import de.frank.kompass.observability.KompassLog
import java.io.Reader
import java.io.Writer
import java.security.MessageDigest

class SicherungsFehler(meldung: String, ursache: Throwable? = null) : Exception(meldung, ursache)

/** Wie viele Sätze je Art in der Datei stehen — der Kopf verspricht es, der Inhalt hält es. */
data class SicherungsAnzahl(
    val eintraege: Int = 0,
    val fragen: Int = 0,
    val sitzungen: Int = 0,
    val nachrichten: Int = 0,
)

/** Was in einer Sicherungsdatei steht — und was der Benutzer vor dem Einspielen sieht. */
data class SicherungsVorschau(
    val schema: Int,
    val erstelltAm: String,
    val roomVersion: Int,
    val anzahl: SicherungsAnzahl,
    /** Wie viele Einträge je Wissensbereich, aus dem Kopf der Datei. */
    val jeBereich: Map<SicherungsTeil, Int> = emptyMap(),
    /** Was beim Sichern angehakt war. Bei alten Dateien abgeleitet. */
    val umfang: Set<SicherungsTeil> = emptySet(),
) {
    /** Ein Satz, der sagt, was in der Datei steckt — vor dem Einspielen. */
    fun alsText(): String {
        val teile = buildList {
            SicherungsTeil.entries.forEach { teil ->
                jeBereich[teil]?.takeIf { it > 0 }?.let { add("$it ${teil.titel}") }
            }
            if (anzahl.fragen > 0) add("${anzahl.fragen} Fragen")
            if (anzahl.sitzungen > 0) add("${anzahl.sitzungen} Gespräche")
        }
        val kern = if (teile.isEmpty()) "Die Datei enthält keine Einträge." else teile.joinToString(", ")
        return "Sicherung vom $erstelltAm: $kern. " +
            "Ergänzt wird nur, was hier fehlt — Vorhandenes bleibt unverändert."
    }
}

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
 * Das Dateiformat der Sicherung — Schreiben und Lesen, beides satzweise.
 *
 * Satzweise heisst: Es steht nie der gesamte Bestand gleichzeitig im Speicher. Beim Schreiben
 * wandert jeder Satz direkt in den Ausgabestrom, beim Lesen kommt jeder Satz einzeln heraus.
 * Vorher lagen alle Einträge, alle Nachrichten UND die fertige JSON-Zeichenkette zusammen im
 * Arbeitsspeicher — bei Claude Kompass gut über ein Megabyte allein an Text. Das trug bisher,
 * war aber die Stelle, die als erste kippt.
 *
 * Am Ende der Datei stehen eine Prüfsumme über die Inhalte und die Anzahl je Art. Eine Datei,
 * deren Übertragung abbrach, fällt damit VOR dem Einspielen auf und nicht als rätselhafter
 * Fehler mittendrin.
 */
object Sicherung {

    /**
     * 1: nur der selbst erarbeitete Anteil.
     * 2: wählbarer Umfang, Wissensbereiche vollständig.
     * 3: Prüfsumme, Anzahlen und die Fassung des Datenmodells im Kopf.
     *
     * Ältere Dateien bleiben lesbar; was sie nicht mitbringen, wird nicht geprüft.
     */
    const val SCHEMA_VERSION = 3

    /** So viele Sätze werden je Abfrage aus der Datenbank geholt. */
    const val SEITE = 200

    fun baueDateiname(zeitstempel: String): String = "${AppProfil.DATEI_PRAEFIX}-sicherung-$zeitstempel.json"

    // ---- Schreiben ---------------------------------------------------------------------

    /** Woher die Sätze kommen. Jede Seite wird geholt, geschrieben und wieder freigegeben. */
    interface Quelle {
        suspend fun eintraegeSeite(bereiche: List<String>, versatz: Int): List<EintragEntity>
        suspend fun fragenSeite(versatz: Int): List<FrageEntity>
        suspend fun sitzungen(): List<ChatSitzungEntity>
        suspend fun nachrichten(sitzungId: Long): List<ChatNachrichtEntity>
    }

    /**
     * Schreibt die Sicherung satzweise in [ziel].
     *
     * Zurück kommt, was tatsächlich geschrieben wurde — dieselben Zahlen stehen in der Datei.
     */
    suspend fun schreibe(
        ziel: Writer,
        quelle: Quelle,
        erstelltAm: String,
        umfang: Set<SicherungsTeil>,
        roomVersion: Int,
    ): SicherungsAnzahl {
        val bereiche = umfang.mapNotNull { it.bereich?.id }
        val pruefsumme = Inhaltspruefsumme()
        var eintraege = 0
        var fragen = 0
        var sitzungen = 0
        var nachrichten = 0
        val jeBereich = mutableMapOf<String, Int>()

        val schreiber = JsonWriter(ziel).apply { setIndent(" ") }
        schreiber.beginObject()
        schreiber.name("schema").value(SCHEMA_VERSION.toLong())
        schreiber.name("app").value(AppProfil.PRODUKT)
        schreiber.name("erstelltAm").value(erstelltAm)
        schreiber.name("roomVersion").value(roomVersion.toLong())
        schreiber.name("umfang").beginArray()
        umfang.forEach { schreiber.value(it.id) }
        schreiber.endArray()

        schreiber.name("eintraege").beginArray()
        if (bereiche.isNotEmpty()) {
            var versatz = 0
            while (true) {
                val seite = quelle.eintraegeSeite(bereiche, versatz)
                if (seite.isEmpty()) break
                seite.forEach { eintrag ->
                    // Vollständig, mit allen Angaben: Auf einem neuen Gerät muss der Eintrag
                    // allein aus der Datei entstehen können, ohne die Wissensbasis der App.
                    schreiber.beginObject()
                    schreiber.name("id").value(eintrag.id)
                    schreiber.name("bereich").value(eintrag.bereich)
                    schreiber.name("name").value(eintrag.name)
                    schreiber.name("kurz").value(eintrag.kurz)
                    schreiber.name("erklaerung").value(eintrag.erklaerung)
                    schreiber.name("stufe").value(eintrag.stufe.toLong())
                    schreiber.name("kategorie").value(eintrag.kategorie)
                    schreiber.name("art").value(eintrag.art)
                    schreiber.name("quelleEnglisch").value(eintrag.quelleEnglisch)
                    schreiber.name("seitVersion").value(eintrag.seitVersion)
                    schreiber.name("sortierName").value(eintrag.sortierName)
                    schreiber.name("entfernt").value(eintrag.entfernt)
                    schreiber.name("entferntInVersion").value(eintrag.entferntInVersion)
                    schreiber.name("ersatz").value(eintrag.ersatz)
                    schreiber.endObject()
                    pruefsumme.nimm(eintrag.id, eintrag.name, eintrag.erklaerung, eintrag.stufe)
                    jeBereich[eintrag.bereich] = (jeBereich[eintrag.bereich] ?: 0) + 1
                    eintraege += 1
                }
                versatz += seite.size
            }
        }
        schreiber.endArray()

        schreiber.name("fragen").beginArray()
        if (SicherungsTeil.FRAGEN in umfang) {
            var versatz = 0
            while (true) {
                val seite = quelle.fragenSeite(versatz)
                if (seite.isEmpty()) break
                seite.forEach { frage ->
                    schreiber.beginObject()
                    schreiber.name("eintragId").value(frage.eintragId)
                    schreiber.name("frage").value(frage.frage)
                    schreiber.name("antwort").value(frage.antwort)
                    schreiber.name("erstelltAm").value(frage.erstelltAm)
                    schreiber.endObject()
                    pruefsumme.nimm(frage.eintragId, frage.frage, frage.antwort)
                    fragen += 1
                }
                versatz += seite.size
            }
        }
        schreiber.endArray()

        schreiber.name("sitzungen").beginArray()
        if (SicherungsTeil.GESPRAECHE in umfang) {
            quelle.sitzungen().forEach { sitzung ->
                schreiber.beginObject()
                schreiber.name("titel").value(sitzung.titel)
                schreiber.name("erstelltAm").value(sitzung.erstelltAm)
                schreiber.name("nachrichten").beginArray()
                pruefsumme.nimm(sitzung.titel)
                // Die Nachrichten eines Gesprächs auf einmal, nicht alle Gespräche auf einmal.
                quelle.nachrichten(sitzung.id).forEach { nachricht ->
                    schreiber.beginObject()
                    schreiber.name("rolle").value(nachricht.rolle)
                    schreiber.name("text").value(nachricht.text)
                    schreiber.name("erstelltAm").value(nachricht.erstelltAm)
                    schreiber.endObject()
                    pruefsumme.nimm(nachricht.rolle, nachricht.text)
                    nachrichten += 1
                }
                schreiber.endArray()
                schreiber.endObject()
                sitzungen += 1
            }
        }
        schreiber.endArray()

        val anzahl = SicherungsAnzahl(eintraege, fragen, sitzungen, nachrichten)
        schreiber.name("anzahl").beginObject()
        schreiber.name("eintraege").value(anzahl.eintraege.toLong())
        schreiber.name("fragen").value(anzahl.fragen.toLong())
        schreiber.name("sitzungen").value(anzahl.sitzungen.toLong())
        schreiber.name("nachrichten").value(anzahl.nachrichten.toLong())
        schreiber.endObject()
        schreiber.name("jeBereich").beginObject()
        jeBereich.forEach { (bereich, wieViele) -> schreiber.name(bereich).value(wieViele.toLong()) }
        schreiber.endObject()
        schreiber.name("pruefsumme").value(pruefsumme.fertig())
        schreiber.endObject()
        schreiber.flush()

        KompassLog.info(
            "Sicherung",
            "schreibe",
            "Sicherung geschrieben",
            mapOf(
                "umfang" to umfang.joinToString(",") { it.id },
                "eintraege" to eintraege,
                "fragen" to fragen,
                "sitzungen" to sitzungen,
            ),
        )
        return anzahl
    }

    // ---- Lesen -------------------------------------------------------------------------

    /** Wohin die gelesenen Sätze gehen. Jeder kommt einzeln und wird sofort verarbeitet. */
    interface Senke {
        suspend fun eintrag(werte: Map<String, String>)
        suspend fun frage(eintragId: String, frage: String, antwort: String, erstelltAm: Long)
        suspend fun sitzung(titel: String, erstelltAm: Long, nachrichten: List<Triple<String, String, Long>>)
    }

    /**
     * Liest nur den Kopf und die Zählwerke — für die Vorschau, ohne etwas einzuspielen.
     *
     * Die Datei wird dabei vollständig durchlaufen und die Prüfsumme nachgerechnet. Das ist
     * zugleich die Prüfung, ob die Datei heil angekommen ist: Eine abgebrochene Übertragung
     * fällt hier auf und nicht erst mitten im Einspielen.
     */
    suspend fun pruefe(quelle: Reader): SicherungsVorschau = lies(quelle, senke = null)

    /** Liest die Datei und gibt jeden Satz an [senke] weiter. Prüft dabei dasselbe wie [pruefe]. */
    suspend fun spieleEin(quelle: Reader, senke: Senke): SicherungsVorschau = lies(quelle, senke)

    private suspend fun lies(quelle: Reader, senke: Senke?): SicherungsVorschau {
        val leser = JsonReader(quelle)
        var schema = -1
        var app = ""
        var erstelltAm = ""
        var roomVersion = 0
        var umfang = emptySet<SicherungsTeil>()
        var umfangGelesen = false
        var erwartet: SicherungsAnzahl? = null
        var erwarteteSumme = ""
        val jeBereich = mutableMapOf<SicherungsTeil, Int>()
        var jeBereichGelesen = false
        val pruefsumme = Inhaltspruefsumme()
        var eintraege = 0
        var fragen = 0
        var sitzungen = 0
        var nachrichten = 0

        try {
            leser.beginObject()
            while (leser.hasNext()) {
                when (val feld = leser.nextName()) {
                    "schema" -> schema = leser.nextInt()
                    "app" -> app = leser.nextString()
                    "erstelltAm" -> erstelltAm = leser.nextString()
                    "roomVersion" -> roomVersion = leser.nextInt()
                    "pruefsumme" -> erwarteteSumme = leser.nextString()
                    "umfang" -> {
                        val gelesen = mutableSetOf<SicherungsTeil>()
                        leser.beginArray()
                        while (leser.hasNext()) SicherungsTeil.fromId(leser.nextString())?.let(gelesen::add)
                        leser.endArray()
                        umfang = gelesen
                        umfangGelesen = true
                    }
                    "anzahl" -> {
                        var e = 0
                        var f = 0
                        var s = 0
                        var n = 0
                        leser.beginObject()
                        while (leser.hasNext()) {
                            when (leser.nextName()) {
                                "eintraege" -> e = leser.nextInt()
                                "fragen" -> f = leser.nextInt()
                                "sitzungen" -> s = leser.nextInt()
                                "nachrichten" -> n = leser.nextInt()
                                else -> leser.skipValue()
                            }
                        }
                        leser.endObject()
                        erwartet = SicherungsAnzahl(e, f, s, n)
                    }
                    "jeBereich" -> {
                        leser.beginObject()
                        while (leser.hasNext()) {
                            val bereich = leser.nextName()
                            val wieViele = leser.nextInt()
                            SicherungsTeil.entries.firstOrNull { it.bereich?.id == bereich }
                                ?.let { jeBereich[it] = wieViele }
                        }
                        leser.endObject()
                        jeBereichGelesen = true
                    }
                    "eintraege" -> {
                        // Der Kopf steht vor den Daten: Ab hier ist bekannt, ob die Datei
                        // überhaupt zu dieser App gehört. Erst prüfen, dann einspielen.
                        pruefeKopf(schema, app, roomVersion)
                        leser.beginArray()
                        while (leser.hasNext()) {
                            val werte = liesFelder(leser)
                            pruefsumme.nimm(
                                werte["id"].orEmpty(),
                                werte["name"].orEmpty(),
                                werte["erklaerung"].orEmpty(),
                                werte["stufe"] ?: "0",
                            )
                            werte["bereich"]?.let { bereich ->
                                SicherungsTeil.entries.firstOrNull { it.bereich?.id == bereich }
                                    ?.let { if (!jeBereichGelesen) jeBereich[it] = (jeBereich[it] ?: 0) + 1 }
                            }
                            eintraege += 1
                            senke?.eintrag(werte)
                        }
                        leser.endArray()
                    }
                    "fragen" -> {
                        leser.beginArray()
                        while (leser.hasNext()) {
                            val werte = liesFelder(leser)
                            val eintragId = werte["eintragId"].orEmpty()
                            val text = werte["frage"].orEmpty()
                            val antwort = werte["antwort"].orEmpty()
                            pruefsumme.nimm(eintragId, text, antwort)
                            fragen += 1
                            senke?.frage(eintragId, text, antwort, werte["erstelltAm"]?.toLongOrNull() ?: 0L)
                        }
                        leser.endArray()
                    }
                    "sitzungen" -> {
                        leser.beginArray()
                        while (leser.hasNext()) {
                            var titel = ""
                            var wann = 0L
                            val inhalt = mutableListOf<Triple<String, String, Long>>()
                            leser.beginObject()
                            while (leser.hasNext()) {
                                when (leser.nextName()) {
                                    "titel" -> titel = leser.nextString()
                                    "erstelltAm" -> wann = leser.nextLong()
                                    "nachrichten" -> {
                                        leser.beginArray()
                                        while (leser.hasNext()) {
                                            val werte = liesFelder(leser)
                                            val rolle = werte["rolle"].orEmpty()
                                            val text = werte["text"].orEmpty()
                                            pruefsumme.nimm(rolle, text)
                                            nachrichten += 1
                                            inhalt += Triple(
                                                rolle,
                                                text,
                                                werte["erstelltAm"]?.toLongOrNull() ?: 0L,
                                            )
                                        }
                                        leser.endArray()
                                    }
                                    else -> leser.skipValue()
                                }
                            }
                            leser.endObject()
                            // Die Prüfsumme nimmt den Titel NACH den Nachrichten nicht auf —
                            // beim Schreiben stand er davor, und die Reihenfolge zählt.
                            sitzungen += 1
                            senke?.sitzung(titel, wann, inhalt)
                        }
                        leser.endArray()
                    }
                    else -> {
                        KompassLog.debug("Sicherung", "lies", "Unbekanntes Feld übergangen", mapOf("feld" to feld))
                        leser.skipValue()
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

        pruefeKopf(schema, app, roomVersion)
        val gezaehlt = SicherungsAnzahl(eintraege, fragen, sitzungen, nachrichten)
        pruefeVollstaendigkeit(erwartet, gezaehlt, erwarteteSumme, pruefsumme.fertig())

        return SicherungsVorschau(
            schema = schema,
            erstelltAm = erstelltAm,
            roomVersion = roomVersion,
            anzahl = gezaehlt,
            jeBereich = jeBereich,
            // Schema 1 kennt kein Feld „umfang". Dort stand immer dasselbe drin.
            umfang = if (umfangGelesen) umfang else setOf(SicherungsTeil.FRAGEN, SicherungsTeil.GESPRAECHE),
        )
    }

    /** Alle Felder eines flachen Objekts als Text — verschachtelte Werte werden übergangen. */
    private fun liesFelder(leser: JsonReader): Map<String, String> {
        val werte = mutableMapOf<String, String>()
        leser.beginObject()
        while (leser.hasNext()) {
            val feld = leser.nextName()
            when (leser.peek()) {
                JsonToken.STRING -> werte[feld] = leser.nextString()
                JsonToken.NUMBER -> werte[feld] = leser.nextString()
                JsonToken.BOOLEAN -> werte[feld] = leser.nextBoolean().toString()
                JsonToken.NULL -> { leser.nextNull(); werte[feld] = "" }
                else -> leser.skipValue()
            }
        }
        leser.endObject()
        return werte
    }

    private fun pruefeKopf(schema: Int, app: String, roomVersion: Int) {
        // Auch unter einem früheren Namen geschriebene Sicherungen gehören zu dieser App.
        // Ohne das hätte jede Umbenennung sämtliche bis dahin geschriebenen Dateien entwertet.
        if (app.isNotBlank() && app != AppProfil.PRODUKT && app !in AppProfil.FRUEHERE_NAMEN) {
            throw SicherungsFehler("Diese Sicherung gehört nicht zu ${AppProfil.PRODUKT}.")
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
        // Format kann gleich bleiben, während eine Spalte dazukommt. Eine Datei aus einer
        // neueren Datenbank kann Felder mitbringen, die hier noch niemand einordnen kann.
        if (roomVersion > KompassDatabase.VERSION) {
            throw SicherungsFehler(
                "Diese Sicherung stammt aus einer neueren Datenbank (Fassung $roomVersion, " +
                    "diese App hat ${KompassDatabase.VERSION}). Aktualisier zuerst die App.",
            )
        }
    }

    private fun pruefeVollstaendigkeit(
        erwartet: SicherungsAnzahl?,
        gezaehlt: SicherungsAnzahl,
        erwarteteSumme: String,
        gerechneteSumme: String,
    ) {
        // Dateien vor Schema 3 bringen weder Zählwerk noch Prüfsumme mit. Was nicht dasteht,
        // kann nicht geprüft werden — abzulehnen wäre hier falsch.
        if (erwartet != null && erwartet != gezaehlt) {
            throw SicherungsFehler(
                "Die Datei ist unvollständig: Sie kündigt ${erwartet.eintraege} Einträge, " +
                    "${erwartet.fragen} Fragen und ${erwartet.sitzungen} Gespräche an, " +
                    "enthält aber ${gezaehlt.eintraege}, ${gezaehlt.fragen} und " +
                    "${gezaehlt.sitzungen}. Nimm die Sicherung davor.",
            )
        }
        if (erwarteteSumme.isNotBlank() && erwarteteSumme != gerechneteSumme) {
            throw SicherungsFehler(
                "Der Inhalt der Datei stimmt nicht mit ihrer Prüfsumme überein. Sie ist bei der " +
                    "Übertragung beschädigt worden. Nimm die Sicherung davor.",
            )
        }
    }
}
