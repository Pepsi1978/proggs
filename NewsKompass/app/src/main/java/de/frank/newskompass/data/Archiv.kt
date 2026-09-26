package de.frank.newskompass.data

import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.Block
import de.frank.newskompass.data.model.Meldung
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ein Kalendertag mit mindestens einer gespeicherten Ausgabe. */
data class ArchivTag(
    val datum: LocalDate,
    /** Ausgaben dieses Tages, neueste zuerst. */
    val ausgaben: List<AusgabenEintrag>,
) {
    val meldungen: Int get() = ausgaben.sumOf { it.meldungen }
    val fragen: Int get() = ausgaben.sumOf { it.fragen }
}

/** Ein Monat, in dem tatsächlich etwas aufgezeichnet wurde. */
data class ArchivMonat(
    val monat: YearMonth,
    val ersterTag: LocalDate,
    val letzterTag: LocalDate,
    val laufend: Boolean,
    val ausgaben: List<AusgabenEintrag>,
)

/**
 * Das Archiv entsteht allein aus gespeicherten Ausgaben — keine rückwirkende Recherche, keine
 * erfundenen Tage. Aufgezeichnet wird seit dem [BEGINN].
 */
object Archiv {

    val BEGINN: LocalDate = LocalDate.of(2026, 9, 25)

    /** So viele Kalendertage (heute eingeschlossen) stehen einzeln in der Seitenleiste. */
    const val TAGE = 30

    /** Je Thema kommen so viele Geschichten in den Monatsrückblick. */
    const val TOP_JE_THEMA = 5

    const val RUECKBLICK_PRAEFIX = "rueckblick-"

    /** Tage der letzten [TAGE] Kalendertage, an denen wirklich etwas gespeichert ist, neueste zuerst. */
    fun tage(index: List<AusgabenEintrag>, heute: LocalDate = LocalDate.now()): List<ArchivTag> {
        val grenze = maxOf(heute.minusDays(TAGE - 1L), BEGINN)
        return index.groupBy { it.tag }
            .filterKeys { !it.isBefore(grenze) && !it.isAfter(heute) }
            .map { (tag, liste) -> ArchivTag(tag, liste.sortedByDescending { it.erstelltUm }) }
            .sortedByDescending { it.datum }
    }

    /**
     * Alle Monate mit gespeicherten regulären Meldungen seit dem [BEGINN], neueste zuerst; der
     * laufende heißt „bisher“. Ein Monat nur mit gesprochenen Fragen ergibt keinen Rückblick.
     */
    fun monate(index: List<AusgabenEintrag>, heute: LocalDate = LocalDate.now()): List<ArchivMonat> =
        index.filter { !it.tag.isBefore(BEGINN) && it.regulaereMeldungen > 0 }
            .groupBy { YearMonth.from(it.tag) }
            .map { (monat, liste) ->
                ArchivMonat(
                    monat = monat,
                    ersterTag = liste.minOf { it.tag },
                    letzterTag = liste.maxOf { it.tag },
                    laufend = monat == YearMonth.from(heute),
                    ausgaben = liste.sortedByDescending { it.erstelltUm },
                )
            }
            .sortedByDescending { it.monat }

    fun monatsName(monat: YearMonth): String =
        monat.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY))

    /** „bisher“ für den laufenden Monat, sonst der tatsächlich aufgezeichnete Zeitraum, falls kürzer. */
    fun zeitraum(m: ArchivMonat): String {
        val tage = DateTimeFormatter.ofPattern("d.M.", Locale.GERMANY)
        val voll = m.ersterTag.dayOfMonth == 1 && m.letzterTag == m.monat.atEndOfMonth()
        return when {
            m.laufend -> "bisher, ab ${m.ersterTag.format(tage)}"
            voll -> "ganzer Monat"
            else -> "${m.ersterTag.format(tage)} bis ${m.letzterTag.format(tage)}"
        }
    }

    // --- Monatsrückblick -------------------------------------------------------------------------

    private val merker = HashMap<String, Ausgabe>()

    /**
     * Baut den Rückblick eines Monats aus den gespeicherten Ausgaben: je Thema die [TOP_JE_THEMA]
     * wichtigsten Geschichten. Gleiche Geschichten aus mehreren Läufen werden zusammengefasst.
     *
     * Wichtigkeit (heuristisch, nur aus gespeicherten Fakten): an wie vielen Tagen berichtet (×3),
     * wie weit oben im Block im Mittel (×2, Codex sortiert jeden Block nach Relevanz) und aus wie
     * vielen verschiedenen Quellen (×1, höchstens 3). Gesprochene Fragen zählen nicht mit.
     */
    suspend fun rueckblick(speicher: AusgabenSpeicher, m: ArchivMonat): Ausgabe = withContext(Dispatchers.Default) {
        val schluessel = m.monat.toString() + m.ausgaben.joinToString { "${it.id}:${it.stand}" }
        synchronized(merker) { merker[schluessel] }?.let { return@withContext it }

        val funde = mutableListOf<Fund>()
        m.ausgaben.sortedBy { it.erstelltUm }.forEach { eintrag ->
            val ausgabe = speicher.ausgabe(eintrag.id, merken = false) ?: return@forEach
            ausgabe.bloecke.filter { it.frage == null }.forEach { block ->
                block.meldungen.forEachIndexed { rang, meldung ->
                    funde += Fund(block.themaId, block.titel, meldung, rang, block.meldungen.size, eintrag.tag, eintrag.erstelltUm)
                }
            }
        }

        val bloecke = funde.groupBy { it.themaId }.map { (themaId, liste) ->
            val geschichten = mutableListOf<MutableList<Fund>>()
            liste.forEach { fund ->
                val passend = geschichten.firstOrNull { g -> g.any { gleicheGeschichte(it, fund) } }
                if (passend != null) passend += fund else geschichten += mutableListOf(fund)
            }
            val besten = geschichten.map { it to punkte(it) }
                .sortedWith(compareByDescending<Pair<List<Fund>, Double>> { it.second }.thenByDescending { it.first.maxOf { f -> f.zeit } })
                .take(TOP_JE_THEMA)
                .map { vertreter(it.first) }
            Block(themaId, liste.maxBy { it.zeit }.blockTitel, besten, null)
        }

        val name = "Rückblick ${monatsName(m.monat)}"
        val ausgabe = Ausgabe(
            id = RUECKBLICK_PRAEFIX + m.monat,
            erstelltUm = m.ausgaben.maxOfOrNull { it.erstelltUm } ?: 0L,
            slot = "$name · ${zeitraum(m)}",
            bloecke = bloecke,
        )
        synchronized(merker) {
            merker.keys.removeAll { it.startsWith(m.monat.toString()) }
            merker[schluessel] = ausgabe
        }
        ausgabe
    }

    private class Fund(
        val themaId: String,
        val blockTitel: String,
        val meldung: Meldung,
        val rang: Int,
        val blockGroesse: Int,
        val tag: LocalDate,
        val zeit: Long,
    ) {
    }

    private fun gleicheGeschichte(a: Fund, b: Fund): Boolean = Geschichten.gleich(
        a.meldung.titel, a.meldung.quellen.firstOrNull(), a.meldung.istUpdate,
        b.meldung.titel, b.meldung.quellen.firstOrNull(), b.meldung.istUpdate,
    )

    private fun punkte(g: List<Fund>): Double {
        val tage = g.map { it.tag }.distinct().size
        val rang = g.map { 1.0 - it.rang.toDouble() / maxOf(it.blockGroesse, 1) }.average()
        val quellen = g.flatMap { it.meldung.quellen }.distinct().size.coerceAtMost(3)
        return 3.0 * tage + 2.0 * rang + quellen
    }

    /** Der neueste Stand der Geschichte, mit dem neuesten vorhandenen Bild und einer sichtbaren Begründung. */
    private fun vertreter(g: List<Fund>): Meldung {
        val neuester = g.maxBy { it.zeit }
        val bild = g.sortedByDescending { it.zeit }.firstOrNull { it.meldung.bildDatei != null }?.meldung
        val tage = g.map { it.tag }.distinct().size
        val erstmals = g.minOf { it.tag }.format(DateTimeFormatter.ofPattern("d.M.", Locale.GERMANY))
        val begruendung = (if (tage == 1) "an 1 Tag berichtet" else "an $tage Tagen berichtet") + " · erstmals $erstmals"
        return neuester.meldung.copy(
            id = "rb-${neuester.meldung.id}",
            bildDatei = bild?.bildDatei,
            bildIstKi = bild?.bildIstKi ?: false,
            quellen = g.sortedByDescending { it.zeit }.flatMap { it.meldung.quellen }.distinct().take(6),
            wann = begruendung,
            istUpdate = false,
        )
    }
}
