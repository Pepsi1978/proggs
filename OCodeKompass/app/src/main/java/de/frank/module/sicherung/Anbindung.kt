// ──────────────────────────────────────────────────────────────────────
// Anbindung für Modul M1.1 — Sicherung
//
// Diese Datei gehört der App, nicht der Bibliothek. Hier kommt alles hin,
// was sich von App zu App unterscheidet: die Satz-Serialisierung, welche
// Teile es gibt, das Einspielen und die Namen der Dateien.
// Beim Nachziehen des Moduls wird sie NICHT überschrieben.
//
// Die Moduldateien daneben sind tabu — Anpassungen gehören hierher.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sicherung

import android.util.JsonReader
import android.util.JsonWriter
import de.frank.kompass.AppProfil
import de.frank.kompass.data.EinspielBericht
import de.frank.kompass.data.KompassRepository
import de.frank.kompass.data.Sicherung
import de.frank.kompass.data.SicherungsAnzahl
import de.frank.kompass.data.local.KompassDatabase
import de.frank.kompass.observability.KompassLog
import de.frank.kompass.data.Einspielspur as KompassSpur
import de.frank.kompass.data.model.SicherungsTeil as KompassTeil

/**
 * Wie die Sicherungsdateien von Kompass heißen.
 *
 * **Die beiden Namen unten sind unverändert übernommen.** Dort steht der gewählte
 * Sicherungsordner; ein anderer Name hieße, dass der Benutzer ihn nach dem Update verloren
 * hätte, ohne dass ihm etwas angezeigt wird.
 */
val KOMPASS_SICHERUNGSNAMEN = SicherungsNamen(
    dateiPraefix = AppProfil.DATEI_PRAEFIX,
    fruehere = AppProfil.FRUEHERE_DATEI_PRAEFIXE,
    einstellungenDatei = "kompass_backup_status",
    ordnerSchluessel = "sicherungs_ordner",
)

/** Das Protokoll der App, dem Modul hereingereicht. */
object KompassProtokoll : SicherungsProtokoll {
    override fun info(stelle: String, was: String, meldung: String, felder: Map<String, Any?>) =
        KompassLog.info(stelle, was, meldung, felder)

    override fun warn(stelle: String, was: String, meldung: String, felder: Map<String, Any?>) =
        KompassLog.warn(stelle, was, meldung, felder)
}

/** Das Zurücknehmen eines Einspielvorgangs — die Spur gehört dem Repository. */
class KompassRuecknahme(private val repository: KompassRepository) : SicherungsRuecknahme {
    /** Umhüllt die Spur des Repositories, damit das Modul sie nur durchreichen muss. */
    class Spur(val innen: KompassSpur) : Einspielspur

    private var offen: KompassRepository.EinspielSenke? = null

    override suspend fun beginne(): Einspielspur {
        val senke = repository.EinspielSenke()
        offen = senke
        return Spur(senke.spur)
    }

    override suspend fun schliesseAb(spur: Einspielspur) {
        repository.schliesseEinspielenAb((spur as Spur).innen)
    }

    override suspend fun nimmZurueck(spur: Einspielspur): Int =
        repository.nimmEinspielenZurueck((spur as Spur).innen)

    /** Die Senke des laufenden Einspielvorgangs — nur währenddessen gesetzt. */
    fun laufendeSenke(): KompassRepository.EinspielSenke? = offen

    fun bericht(): EinspielBericht? = offen?.bericht()
}

/**
 * Der Inhalt der Kompass-Sicherung.
 *
 * **Hier steht das Dateiformat.** Die Feldnamen und die Reihenfolge sind unverändert aus der
 * bisherigen Fassung übernommen, ebenso die Werte, die in die Prüfsumme eingehen — nur so
 * bleibt jede bereits geschriebene Sicherungsdatei einspielbar.
 */
class KompassSicherungsInhalt(
    private val quelle: Sicherung.Quelle,
    private val zaehler: suspend (bereiche: List<String>, mitFragen: Boolean, mitGespraechen: Boolean) -> SicherungsAnzahl,
    private val senkeGeber: () -> Sicherung.Senke?,
) : SicherungsInhalt {

    /**
     * Der übliche Weg: alles aus dem Repository.
     *
     * Die Bestandteile einzeln hereinzureichen ist kein Selbstzweck — der Formattest im
     * androidTest baut damit eine Sicherung aus erfundenen Sätzen, ohne eine Datenbank
     * anzulegen. Genau dieser Test bewacht, dass bereits geschriebene Dateien lesbar bleiben.
     */
    constructor(repository: KompassRepository, ruecknahme: KompassRuecknahme?) : this(
        quelle = repository.sicherungsQuelle,
        zaehler = { bereiche, mitFragen, mitGespraechen ->
            repository.sicherungsUmfangZaehlen(bereiche, mitFragen, mitGespraechen)
        },
        senkeGeber = { ruecknahme?.laufendeSenke() },
    )

    override val teile: List<SicherungsTeil> get() = KompassTeil.entries

    override val produkt: String get() = AppProfil.PRODUKT

    override val fruehereNamen: List<String> get() = AppProfil.FRUEHERE_NAMEN.toList()

    override val datenmodellVersion: Int get() = KompassDatabase.VERSION

    // ---- Schreiben ---------------------------------------------------------------------

    override suspend fun schreibeNutzlast(
        schreiber: JsonWriter,
        umfang: Set<SicherungsTeil>,
        pruefsumme: Inhaltspruefsumme,
    ): Nutzlastzahlen {
        val gewaehlt = umfang.filterIsInstance<KompassTeil>().toSet()
        val bereiche = gewaehlt.mapNotNull { it.bereich?.id }
        var eintraege = 0
        var fragen = 0
        var sitzungen = 0
        var nachrichten = 0
        val jeBereich = mutableMapOf<String, Int>()

        schreiber.name("eintraege").beginArray()
        if (bereiche.isNotEmpty()) {
            var nachId = ""
            while (true) {
                val seite = quelle.eintraegeSeite(bereiche, nachId)
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
                nachId = seite.last().id
            }
        }
        schreiber.endArray()

        schreiber.name("fragen").beginArray()
        if (KompassTeil.FRAGEN in gewaehlt) {
            var nachId = 0L
            while (true) {
                val seite = quelle.fragenSeite(nachId)
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
                nachId = seite.last().id
            }
        }
        schreiber.endArray()

        schreiber.name("sitzungen").beginArray()
        if (KompassTeil.GESPRAECHE in gewaehlt) {
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

        return Nutzlastzahlen(
            anzahl = mapOf(
                "eintraege" to eintraege,
                "fragen" to fragen,
                "sitzungen" to sitzungen,
                "nachrichten" to nachrichten,
            ),
            jeBereich = jeBereich,
        )
    }

    // ---- Lesen -------------------------------------------------------------------------

    override suspend fun liesNutzlast(
        feld: String,
        leser: JsonReader,
        pruefsumme: Inhaltspruefsumme,
        einspielen: Boolean,
    ): Nutzlastzahlen? {
        val senke = if (einspielen) senkeGeber() else null
        return when (feld) {
            "eintraege" -> {
                var anzahl = 0
                val jeBereich = mutableMapOf<String, Int>()
                leser.beginArray()
                while (leser.hasNext()) {
                    val werte = Sicherungsrahmen.liesFelder(leser)
                    pruefsumme.nimm(
                        werte["id"].orEmpty(),
                        werte["name"].orEmpty(),
                        werte["erklaerung"].orEmpty(),
                        werte["stufe"] ?: "0",
                    )
                    werte["bereich"]?.let { jeBereich[it] = (jeBereich[it] ?: 0) + 1 }
                    anzahl += 1
                    senke?.eintrag(werte)
                }
                leser.endArray()
                Nutzlastzahlen(mapOf("eintraege" to anzahl), jeBereich)
            }

            "fragen" -> {
                var anzahl = 0
                leser.beginArray()
                while (leser.hasNext()) {
                    val werte = Sicherungsrahmen.liesFelder(leser)
                    val eintragId = werte["eintragId"].orEmpty()
                    val text = werte["frage"].orEmpty()
                    val antwort = werte["antwort"].orEmpty()
                    pruefsumme.nimm(eintragId, text, antwort)
                    anzahl += 1
                    senke?.frage(eintragId, text, antwort, werte["erstelltAm"]?.toLongOrNull() ?: 0L)
                }
                leser.endArray()
                Nutzlastzahlen(mapOf("fragen" to anzahl))
            }

            "sitzungen" -> {
                var sitzungen = 0
                var nachrichten = 0
                leser.beginArray()
                while (leser.hasNext()) {
                    var titel = ""
                    var wann = 0L
                    val inhalt = mutableListOf<Triple<String, String, Long>>()
                    leser.beginObject()
                    while (leser.hasNext()) {
                        when (leser.nextName()) {
                            "titel" -> {
                                titel = leser.nextString()
                                // Genau hier, nicht später: Der Schreiber nimmt den Titel VOR
                                // den Nachrichten auf, und die Reihenfolge geht in die Summe ein.
                                pruefsumme.nimm(titel)
                            }
                            "erstelltAm" -> wann = leser.nextLong()
                            "nachrichten" -> {
                                leser.beginArray()
                                while (leser.hasNext()) {
                                    val werte = Sicherungsrahmen.liesFelder(leser)
                                    val rolle = werte["rolle"].orEmpty()
                                    val text = werte["text"].orEmpty()
                                    pruefsumme.nimm(rolle, text)
                                    nachrichten += 1
                                    inhalt += Triple(rolle, text, werte["erstelltAm"]?.toLongOrNull() ?: 0L)
                                }
                                leser.endArray()
                            }
                            else -> leser.skipValue()
                        }
                    }
                    leser.endObject()
                    sitzungen += 1
                    senke?.sitzung(titel, wann, inhalt)
                }
                leser.endArray()
                Nutzlastzahlen(mapOf("sitzungen" to sitzungen, "nachrichten" to nachrichten))
            }

            // Kein Feld von uns — das Modul überspringt es.
            else -> null
        }
    }

    // ---- Anzeige -----------------------------------------------------------------------

    override suspend fun zaehle(umfang: Set<SicherungsTeil>): Nutzlastzahlen {
        val gewaehlt = umfang.filterIsInstance<KompassTeil>().toSet()
        val anzahl = zaehler(
            gewaehlt.mapNotNull { it.bereich?.id },
            KompassTeil.FRAGEN in gewaehlt,
            KompassTeil.GESPRAECHE in gewaehlt,
        )
        return Nutzlastzahlen(
            mapOf(
                "eintraege" to anzahl.eintraege,
                "fragen" to anzahl.fragen,
                "sitzungen" to anzahl.sitzungen,
                "nachrichten" to anzahl.nachrichten,
            ),
        )
    }

    /** Erfahrungswerte je Satzart — die Größenordnung zählt, nicht das Byte. */
    override fun schaetzeGroesse(zahlen: Nutzlastzahlen): Long =
        (zahlen.anzahl["eintraege"] ?: 0) * 600L +
            (zahlen.anzahl["fragen"] ?: 0) * 900L +
            (zahlen.anzahl["nachrichten"] ?: 0) * 700L

    override fun fasseZusammen(vorschau: SicherungsVorschau): String {
        val teile = buildList {
            KompassTeil.entries.forEach { teil ->
                val bereich = teil.bereich?.id ?: return@forEach
                vorschau.zahlen.jeBereich[bereich]?.takeIf { it > 0 }?.let { add("$it ${teil.titel}") }
            }
            vorschau.zahlen.anzahl["fragen"]?.takeIf { it > 0 }?.let { add("$it Fragen") }
            vorschau.zahlen.anzahl["sitzungen"]?.takeIf { it > 0 }?.let { add("$it Gespräche") }
        }
        val kern = if (teile.isEmpty()) "Die Datei enthält keine Einträge." else teile.joinToString(", ")
        return "Sicherung vom ${vorschau.erstelltAm}: $kern. " +
            "Ergänzt wird nur, was hier fehlt — Vorhandenes bleibt unverändert."
    }
}
