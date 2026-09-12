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
import androidx.room.withTransaction
import de.frank.genialeideen.data.local.GenialeIdeenDatabase
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.data.local.Kategorieart
import de.frank.genialeideen.data.local.NachrichtEntity
import de.frank.genialeideen.data.local.inhaltsSchluessel
import de.frank.genialeideen.data.local.weitereKategorieIds
import de.frank.genialeideen.data.local.weitereKategorienText
import de.frank.genialeideen.observability.IdeenLog

/**
 * Wie die Sicherungsdateien von Geniale Ideen heißen.
 *
 * **Die beiden Namen unten sind unverändert aus der bisherigen Fassung übernommen.** Dort steht
 * der gewählte Sicherungsordner und der Zeitpunkt der letzten Sicherung; ein anderer Name hieße,
 * dass der Benutzer beides nach dem Update verloren hätte, ohne dass ihm etwas angezeigt wird.
 * `pm_backup_status` sieht falsch aus, ist aber der Name, unter dem die Werte tatsächlich liegen.
 */
val IDEEN_SICHERUNGSNAMEN = SicherungsNamen(
    dateiPraefix = "geniale-ideen",
    einstellungenDatei = "pm_backup_status",
    ordnerSchluessel = "sicherungs_ordner",
)

/** Was gesichert werden kann. Die `id` steht so in der Datei und darf sich nie ändern. */
enum class IdeenTeil(
    override val id: String,
    override val titel: String,
    override val erklaerung: String,
) : SicherungsTeil {
    IDEEN("ideen", "Ideen", "Alle Ideen samt Entwürfen, Reihenfolge und Kategoriezuordnung."),
    KATEGORIEN("kategorien", "Kategorien", "Die selbst angelegten Kategorien mit ihrer Reihenfolge."),
    GESPRAECHE(
        "nachrichten",
        "Gespräche",
        "Die Unterhaltungen zu den Ideen. Sie kommen nur mit den Ideen zurück, zu denen sie gehören.",
    ),
}

/**
 * Die angehakten Teile aus den gemerkten Kennungen.
 *
 * Nichts gemerkt heißt „alles" — so bekommt niemand durch ein Update stillschweigend eine
 * kleinere Sicherung, als er vorher hatte. Dasselbe gilt, wenn nur unbekannte Kennungen
 * dastehen: Eine leere Sicherung sähe aus wie eine Sicherung und wäre keine.
 */
fun gewaehlteTeile(gemerkt: Set<String>): Set<SicherungsTeil> =
    IdeenTeil.entries.filter { it.id in gemerkt }.toSet().ifEmpty { IdeenTeil.entries.toSet() }

/** Das Protokoll der App, dem Modul hereingereicht. */
object IdeenProtokoll : SicherungsProtokoll {
    override fun info(stelle: String, was: String, meldung: String, felder: Map<String, Any?>) =
        IdeenLog.info(stelle, was, meldung, felder)

    override fun warn(stelle: String, was: String, meldung: String, felder: Map<String, Any?>) =
        IdeenLog.warn(stelle, was, meldung, felder)
}

/** Was ein Einspielvorgang bewirkt hat — für die Meldung an den Benutzer. */
data class Einspielbericht(val neu: Int, val schonDa: Int)

/**
 * Sammelt die gelesenen Sätze und schreibt sie am Ende in **einer** Transaktion.
 *
 * Warum nicht satzweise direkt in die Datenbank: Die drei Teile hängen voneinander ab. Eine Idee
 * braucht die Zuordnung ihrer Kategorien, ein Gespräch die Kennung der Idee, zu der es gehört —
 * und in einer vor dem Modul geschriebenen Datei stehen die Ideen **vor** den Kategorien. Wer
 * satzweise schriebe, verlöre bei jeder solchen Datei die Kategorien. Die Sätze sind Text; sie
 * kurz im Speicher zu halten kostet nichts gegen einen halb eingespielten Bestand.
 */
class EinspielSenke(private val datenbank: GenialeIdeenDatabase) {

    private val kategorien = mutableListOf<Map<String, String>>()
    private val ideen = mutableListOf<Map<String, String>>()
    private val nachrichten = mutableListOf<Map<String, String>>()

    /** Was dabei entstanden ist — damit es sich vollständig zurücknehmen lässt. */
    val spur = IdeenSpur()

    var bericht = Einspielbericht(neu = 0, schonDa = 0)
        private set

    fun kategorie(werte: Map<String, String>) { kategorien += werte }

    fun idee(werte: Map<String, String>) { ideen += werte }

    fun nachricht(werte: Map<String, String>) { nachrichten += werte }

    /** Legt an, was in der App fehlt. Vorhandenes bleibt in jedem Fall unverändert. */
    suspend fun schreibe() = datenbank.withTransaction {
        val ideenDao = datenbank.ideenDao()
        val kategorienDao = datenbank.kategorienDao()
        val nachrichtenDao = datenbank.nachrichtenDao()

        // Die Kategorien zuerst und über Name + Art zugeordnet — die Kennungen der Sicherung
        // passen auf einem anderen Gerät (oder nach dem Umbenennen) nicht zu den eigenen.
        val kategorieZuordnung = mutableMapOf<Long, Long>()
        kategorien.forEach { werte ->
            val name = werte["name"].orEmpty().trim()
            if (name.isBlank()) return@forEach
            val art = runCatching {
                Kategorieart.valueOf(werte["art"] ?: Kategorieart.MENTAL.name)
            }.getOrDefault(Kategorieart.MENTAL)
            val vorhanden = kategorienDao.nachNameUndArt(name, art)?.id
            val id = vorhanden
                ?: kategorienDao.einfuegen(
                    KategorieEntity(name = name, reihenfolge = kategorienDao.anzahl(art), art = art),
                ).takeIf { it > 0 }?.also { spur.kategorien += it }
                ?: kategorienDao.nachNameUndArt(name, art)?.id
                ?: return@forEach
            werte["id"]?.toLongOrNull()?.let { kategorieZuordnung[it] = id }
        }

        val abgleich = Abgleich(ideenDao.alleEinmal())
        val ideenZuordnung = mutableMapOf<Long, Long>()
        var uebersprungen = 0
        // In der Reihenfolge der Sicherung unten an die jeweilige Liste anhängen.
        val jetzt = System.currentTimeMillis()
        val gelesen = ideen.mapIndexed { stelle, werte ->
            werte["id"]?.toLongOrNull() to zuIdee(werte, jetzt + stelle)
        }.sortedBy { (_, idee) -> idee.reihenfolge }
        gelesen.forEach { (alteId, idee) ->
            if (!abgleich.istNeu(idee)) {
                uebersprungen++
                return@forEach
            }
            abgleich.merke(idee)
            val haupt = idee.kategorieId?.let(kategorieZuordnung::get)
            val weitere = idee.weitereKategorieIds().mapNotNull(kategorieZuordnung::get).filter { it != haupt }
            val neueId = ideenDao.einfuegen(
                idee.copy(
                    reihenfolge = ideenDao.naechsteReihenfolgeUnten(idee.status),
                    kategorieId = haupt,
                    weitereKategorien = weitereKategorienText(weitere),
                ),
            )
            ideenZuordnung[alteId ?: -1L] = neueId
            spur.ideen += neueId
        }

        // Gespräche nur für die Ideen, die gerade neu dazugekommen sind.
        val liste = nachrichten.mapNotNull { werte ->
            val ideeId = werte["ideeId"]?.toLongOrNull()?.let(ideenZuordnung::get) ?: return@mapNotNull null
            NachrichtEntity(
                ideeId = ideeId,
                rolle = werte["rolle"].orEmpty(),
                text = werte["text"].orEmpty(),
                zeitpunkt = werte["zeitpunkt"]?.toLongOrNull() ?: System.currentTimeMillis(),
                unvollstaendig = werte["unvollstaendig"] == "true",
            )
        }
        if (liste.isNotEmpty()) nachrichtenDao.einfuegenAlle(liste)

        bericht = Einspielbericht(neu = ideenZuordnung.size, schonDa = uebersprungen)
        IdeenLog.info(
            "Sicherung",
            "spieleEin",
            "Sicherung eingespielt",
            mapOf("neu" to bericht.neu, "schonDa" to bericht.schonDa),
        )
    }
}

/** Die Kennung eines Einspielvorgangs: was dabei angelegt wurde. */
class IdeenSpur : Einspielspur {
    val ideen = mutableListOf<Long>()
    val kategorien = mutableListOf<Long>()
}

/**
 * Das Zurücknehmen eines Einspielvorgangs.
 *
 * Zurückgenommen werden die eingespielten Ideen; ihre Gespräche gehen über den Fremdschlüssel
 * mit. Neu angelegte Kategorien bleiben stehen: Sie sind nach dem Einspielen womöglich schon
 * anderen Ideen zugewiesen worden, und eine leere Kategorie ist harmloser als eine Idee, die
 * ihre Zuordnung verliert.
 */
class IdeenRuecknahme(private val datenbank: GenialeIdeenDatabase) : SicherungsRuecknahme {

    /** Die Senke des laufenden Einspielvorgangs — nur währenddessen gesetzt. */
    private var offen: EinspielSenke? = null

    override suspend fun beginne(): Einspielspur {
        val senke = EinspielSenke(datenbank)
        offen = senke
        return senke.spur
    }

    /**
     * Hier wird tatsächlich eingespielt.
     *
     * Das Modul ruft diesen Punkt auf, nachdem die Datei vollständig gelesen ist — und genau
     * dann darf geschrieben werden, keinen Satz früher: Vorher steht nicht fest, welche
     * Kategorien es gibt und zu welcher Idee ein Gespräch gehört.
     */
    override suspend fun schliesseAb(spur: Einspielspur) {
        offen?.schreibe()
    }

    override suspend fun nimmZurueck(spur: Einspielspur): Int {
        val eigene = spur as? IdeenSpur ?: return 0
        if (eigene.ideen.isEmpty()) return 0
        val entfernt = datenbank.ideenDao().loescheMehrere(eigene.ideen)
        IdeenLog.info("Sicherung", "nimmZurueck", "Einspielen zurückgenommen", mapOf("anzahl" to entfernt))
        eigene.ideen.clear()
        return entfernt
    }

    fun laufendeSenke(): EinspielSenke? = offen

    fun bericht(): Einspielbericht? = offen?.bericht
}

/**
 * Der Inhalt der Sicherung von Geniale Ideen.
 *
 * **Hier steht das Dateiformat.** Die Feldnamen sind unverändert aus der bisherigen Fassung
 * übernommen — nur so bleibt jede bereits geschriebene Sicherungsdatei einspielbar.
 */
class IdeenSicherungsInhalt(
    private val datenbank: GenialeIdeenDatabase,
    private val senkeGeber: () -> EinspielSenke?,
) : SicherungsInhalt {

    constructor(datenbank: GenialeIdeenDatabase, ruecknahme: IdeenRuecknahme?) :
        this(datenbank, { ruecknahme?.laufendeSenke() })

    override val teile: List<SicherungsTeil> get() = IdeenTeil.entries

    override val produkt: String get() = "Geniale Ideen"

    override val datenmodellVersion: Int get() = GenialeIdeenDatabase.VERSION

    /**
     * Vor dem Modul hieß das Schema-Feld `schemaVersion`.
     *
     * Ohne diese Zuordnung liefe es in die Nutzlast, der Kopf bliebe ohne Schema-Angabe und jede
     * bis dahin geschriebene Sicherung würde als unbrauchbar abgelehnt.
     */
    override val kopfAliase: Map<String, String> get() = mapOf("schemaVersion" to "schema")

    // ---- Schreiben ---------------------------------------------------------------------

    override suspend fun schreibeNutzlast(
        schreiber: JsonWriter,
        umfang: Set<SicherungsTeil>,
        pruefsumme: Inhaltspruefsumme,
    ): Nutzlastzahlen {
        val gewaehlt = umfang.filterIsInstance<IdeenTeil>().toSet()
        var ideen = 0
        var kategorien = 0
        var nachrichten = 0

        schreiber.name("ideen").beginArray()
        if (IdeenTeil.IDEEN in gewaehlt) {
            datenbank.ideenDao().alleEinmal().forEach { idee ->
                schreiber.beginObject()
                schreiber.name("id").value(idee.id)
                schreiber.name("titel").value(idee.titel)
                schreiber.name("text").value(idee.text)
                schreiber.name("status").value(idee.status)
                schreiber.name("reihenfolge").value(idee.reihenfolge.toLong())
                schreiber.name("angelegtAm").value(idee.angelegtAm)
                schreiber.name("geaendertAm").value(idee.geaendertAm)
                schreiber.name("umgesetztAm").value(idee.umgesetztAm)
                schreiber.name("originalText").value(idee.originalText)
                schreiber.name("kategorieId").value(idee.kategorieId)
                schreiber.name("weitereKategorien").value(idee.weitereKategorien)
                schreiber.endObject()
                pruefsumme.nimm(idee.id, idee.titel, idee.text, idee.status)
                ideen += 1
            }
        }
        schreiber.endArray()

        schreiber.name("kategorien").beginArray()
        if (IdeenTeil.KATEGORIEN in gewaehlt) {
            datenbank.kategorienDao().alleEinmal().forEach { kategorie ->
                schreiber.beginObject()
                schreiber.name("id").value(kategorie.id)
                schreiber.name("name").value(kategorie.name)
                schreiber.name("reihenfolge").value(kategorie.reihenfolge.toLong())
                schreiber.name("art").value(kategorie.art.name)
                schreiber.endObject()
                pruefsumme.nimm(kategorie.id, kategorie.name, kategorie.art.name)
                kategorien += 1
            }
        }
        schreiber.endArray()

        schreiber.name("nachrichten").beginArray()
        if (IdeenTeil.GESPRAECHE in gewaehlt) {
            datenbank.nachrichtenDao().alle().forEach { nachricht ->
                schreiber.beginObject()
                schreiber.name("id").value(nachricht.id)
                schreiber.name("ideeId").value(nachricht.ideeId)
                schreiber.name("rolle").value(nachricht.rolle)
                schreiber.name("text").value(nachricht.text)
                schreiber.name("zeitpunkt").value(nachricht.zeitpunkt)
                schreiber.name("unvollstaendig").value(nachricht.unvollstaendig)
                schreiber.endObject()
                pruefsumme.nimm(nachricht.ideeId, nachricht.rolle, nachricht.text)
                nachrichten += 1
            }
        }
        schreiber.endArray()

        return Nutzlastzahlen(
            mapOf("ideen" to ideen, "kategorien" to kategorien, "nachrichten" to nachrichten),
        )
    }

    // ---- Lesen -------------------------------------------------------------------------

    /**
     * Die Fassung ohne Zweck — sie liest wie eine Vorschau.
     *
     * Das Modul ruft sie nicht mehr; sie steht nur noch da, weil der Vertrag sie verlangt.
     */
    override suspend fun liesNutzlast(
        feld: String,
        leser: JsonReader,
        pruefsumme: Inhaltspruefsumme,
        einspielen: Boolean,
    ): Nutzlastzahlen? = liesNutzlast(
        feld,
        leser,
        pruefsumme,
        if (einspielen) Lesezweck.EINSPIELEN else Lesezweck.VORSCHAU,
    )

    /**
     * Der Bestand daneben wird nur für die **Vorschau** geladen.
     *
     * Er beantwortet die einzige Frage, die vor dem Einspielen wirklich zählt: Wie viel davon
     * fehlt in der App überhaupt? Ohne sie stünde da „42 Ideen", und niemand wüsste, ob danach
     * 42 dazukommen oder null.
     *
     * Beim blossen Nachrechnen einer frisch geschriebenen Datei fragt das niemand — und genau
     * das lief vorher bei **jeder selbsttätigen Sicherung** mit: alle Ideen ein zweites Mal aus
     * der Datenbank, dazu zwei Mengen darüber, deren Ergebnis niemand ansah.
     */
    override suspend fun liesNutzlast(
        feld: String,
        leser: JsonReader,
        pruefsumme: Inhaltspruefsumme,
        zweck: Lesezweck,
    ): Nutzlastzahlen? {
        val senke = if (zweck == Lesezweck.EINSPIELEN) senkeGeber() else null
        return when (feld) {
            "ideen" -> {
                var anzahl = 0
                var neu = 0
                val abgleich = if (zweck == Lesezweck.VORSCHAU) vorschauAbgleichHolen() else null
                leser.beginArray()
                while (leser.hasNext()) {
                    val werte = Sicherungsrahmen.liesFelder(leser)
                    pruefsumme.nimm(
                        werte["id"] ?: "0",
                        werte["titel"].orEmpty(),
                        werte["text"].orEmpty(),
                        werte["status"] ?: IdeenStatus.OFFEN.name,
                    )
                    anzahl += 1
                    if (abgleich != null) {
                        val idee = zuIdee(werte, System.currentTimeMillis() + anzahl)
                        if (abgleich.istNeu(idee)) {
                            abgleich.merke(idee)
                            neu += 1
                        }
                    }
                    senke?.idee(werte)
                }
                leser.endArray()
                Nutzlastzahlen(mapOf("ideen" to anzahl, "neu" to neu))
            }

            "kategorien" -> {
                var anzahl = 0
                leser.beginArray()
                while (leser.hasNext()) {
                    val werte = Sicherungsrahmen.liesFelder(leser)
                    pruefsumme.nimm(
                        werte["id"] ?: "0",
                        werte["name"].orEmpty(),
                        werte["art"] ?: Kategorieart.MENTAL.name,
                    )
                    anzahl += 1
                    senke?.kategorie(werte)
                }
                leser.endArray()
                Nutzlastzahlen(mapOf("kategorien" to anzahl))
            }

            "nachrichten" -> {
                var anzahl = 0
                leser.beginArray()
                while (leser.hasNext()) {
                    val werte = Sicherungsrahmen.liesFelder(leser)
                    pruefsumme.nimm(
                        werte["ideeId"] ?: "0",
                        werte["rolle"].orEmpty(),
                        werte["text"].orEmpty(),
                    )
                    anzahl += 1
                    senke?.nachricht(werte)
                }
                leser.endArray()
                Nutzlastzahlen(mapOf("nachrichten" to anzahl))
            }

            // Kein Feld von uns — das Modul überspringt es.
            else -> null
        }
    }

    private suspend fun vorschauAbgleichHolen(): Abgleich = Abgleich(datenbank.ideenDao().alleEinmal())

    // ---- Anzeige -----------------------------------------------------------------------

    override suspend fun zaehle(umfang: Set<SicherungsTeil>): Nutzlastzahlen {
        val gewaehlt = umfang.filterIsInstance<IdeenTeil>().toSet()
        return Nutzlastzahlen(
            mapOf(
                "ideen" to if (IdeenTeil.IDEEN in gewaehlt) datenbank.ideenDao().anzahl() else 0,
                "kategorien" to
                    if (IdeenTeil.KATEGORIEN in gewaehlt) datenbank.kategorienDao().anzahlGesamt() else 0,
                "nachrichten" to
                    if (IdeenTeil.GESPRAECHE in gewaehlt) datenbank.nachrichtenDao().anzahl() else 0,
            ),
        )
    }

    /**
     * Erfahrungswerte, keine Rechnung: Eine Idee wiegt etwa ein halbes Kilobyte, ein Satz aus
     * einem Gespräch etwas mehr, eine Kategorie fast nichts.
     */
    override fun schaetzeGroesse(zahlen: Nutzlastzahlen): Long =
        (zahlen.anzahl["ideen"] ?: 0) * 500L +
            (zahlen.anzahl["nachrichten"] ?: 0) * 700L +
            (zahlen.anzahl["kategorien"] ?: 0) * 80L

    override fun fasseZusammen(vorschau: SicherungsVorschau): String {
        val ideen = vorschau.zahlen.anzahl["ideen"] ?: 0
        val kategorien = vorschau.zahlen.anzahl["kategorien"] ?: 0
        val nachrichten = vorschau.zahlen.anzahl["nachrichten"] ?: 0
        val teile = buildList {
            add(zaehlwort(ideen, "Idee", "Ideen"))
            if (kategorien > 0) add(zaehlwort(kategorien, "Kategorie", "Kategorien"))
            if (nachrichten > 0) add(zaehlwort(nachrichten, "Gesprächsbeitrag", "Gesprächsbeiträge"))
        }
        return "Sicherung vom ${vorschau.erstelltAm}: ${teile.joinToString(", ")}"
    }
}

// ---- Gemeinsames --------------------------------------------------------------------------

private fun zaehlwort(anzahl: Int, einzahl: String, mehrzahl: String) =
    "$anzahl ${if (anzahl == 1) einzahl else mehrzahl}"

/**
 * Eine Idee aus den gelesenen Feldern — ohne Kennung, die vergibt die Datenbank neu.
 *
 * [ersatzZeit] springt ein, wenn die Datei keinen Anlagezeitpunkt mitbringt. Sie muss je Satz
 * verschieden sein: Bekämen alle dasselbe „jetzt", hielte der Abgleich alle bis auf die erste
 * für schon vorhanden.
 */
private fun zuIdee(werte: Map<String, String>, ersatzZeit: Long): IdeeEntity {
    val gueltig = IdeenStatus.entries.map { it.name }.toSet()
    return IdeeEntity(
        titel = werte["titel"].orEmpty(),
        text = werte["text"].orEmpty(),
        status = werte["status"]?.takeIf { it in gueltig } ?: IdeenStatus.OFFEN.name,
        reihenfolge = werte["reihenfolge"]?.toIntOrNull() ?: 0,
        angelegtAm = werte["angelegtAm"]?.toLongOrNull()?.takeIf { it > 0 } ?: ersatzZeit,
        geaendertAm = werte["geaendertAm"]?.toLongOrNull()?.takeIf { it > 0 } ?: ersatzZeit,
        umgesetztAm = werte["umgesetztAm"]?.toLongOrNull(),
        originalText = werte["originalText"]?.takeIf(String::isNotBlank),
        kategorieId = werte["kategorieId"]?.toLongOrNull()?.takeIf { it > 0L },
        weitereKategorien = werte["weitereKategorien"].orEmpty(),
    )
}

/**
 * Wer schon da ist, kommt nicht noch einmal dazu: gleicher Anlagezeitpunkt (dieselbe Idee, auch
 * wenn sie seither bearbeitet wurde) oder gleicher Titel und Text.
 */
private class Abgleich(bestehende: List<IdeeEntity>) {
    private val zeitpunkte = bestehende.map { it.angelegtAm }.toMutableSet()
    private val inhalte = bestehende.map { it.inhaltsSchluessel() }.toMutableSet()

    fun istNeu(idee: IdeeEntity): Boolean =
        idee.angelegtAm !in zeitpunkte && idee.inhaltsSchluessel() !in inhalte

    fun merke(idee: IdeeEntity) {
        zeitpunkte += idee.angelegtAm
        inhalte += idee.inhaltsSchluessel()
    }
}
