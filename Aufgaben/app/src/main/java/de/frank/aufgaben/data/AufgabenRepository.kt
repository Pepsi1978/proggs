package de.frank.aufgaben.data

import android.content.Context
import android.content.Intent
import de.frank.aufgaben.erinnerung.ErinnerungsDienst
import de.frank.aufgaben.erinnerung.Planer
import de.frank.aufgaben.widget.HeuteWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Einziger Schreibweg. Nach jeder Änderung ziehen Erinnerungen, Widget und die Wecker-Brücke nach —
 * so kann keine Stelle der App das vergessen.
 */
class AufgabenRepository private constructor(context: Context) {
    private val app = context.applicationContext
    private val dao = AufgabenDatenbank.get(app).dao()
    private val nebenbei = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val alle: Flow<List<Aufgabe>> = dao.alle()

    suspend fun eine(id: Long): Aufgabe? = dao.eine(id)
    suspend fun alleEinmal(): List<Aufgabe> = dao.alleEinmal()
    suspend fun fuerTag(tag: Long, mitUeberfaelligen: Boolean): List<Aufgabe> =
        dao.fuerTag(tag, if (mitUeberfaelligen) 1 else 0).filter { !it.laeuftTaeglich } +
            dao.taeglichOffen().mapNotNull { it.amTag(tag, Tage.heute()) }

    suspend fun neu(aufgabe: Aufgabe): Long {
        val id = dao.neu(aufgabe)
        geaendert()
        return id
    }

    suspend fun speichere(aufgabe: Aufgabe) {
        dao.aendere(aufgabe.copy(geaendert = System.currentTimeMillis()))
        geaendert()
    }

    suspend fun loesche(aufgabe: Aufgabe) {
        Planer.storniere(app, aufgabe.id)
        ErinnerungsDienst.stoppe(app, aufgabe.id)
        dao.loesche(aufgabe)
        geaendert()
    }

    /** Erledigt oder wieder offen. Bei Wiederholungen entsteht beim Erledigen der nächste Termin. */
    suspend fun setzeErledigt(aufgabe: Aufgabe, erledigt: Boolean): Aufgabe? {
        val heute = Tage.heute()
        val taeglich = aufgabe.wdh == Wiederholung.TAEGLICH
        if (erledigt && aufgabe.zuFrueh(heute)) return null
        // Schon erledigt (z. B. noch einmal über die Benachrichtigung): keine zweite Folge, sonst stünde sie täglich doppelt da.
        if (erledigt && taeglich && dao.eine(aufgabe.id)?.erledigt == true) return null
        if (erledigt) ErinnerungsDienst.stoppe(app, aufgabe.id)
        // Täglich: Erledigt wird das Vorkommen von heute. Ohne Tag oder mit altem Tag zählt es deshalb auf heute.
        val tagJetzt = if (erledigt && taeglich) maxOf(aufgabe.tag ?: heute, heute) else aufgabe.tag
        dao.aendere(aufgabe.copy(tag = tagJetzt, erledigt = erledigt, erledigtAm = if (erledigt) System.currentTimeMillis() else null, geaendert = System.currentTimeMillis()))
        // Wieder offen: Die offene Folge dieser Kette muss weg, sonst stünde die Aufgabe ab morgen doppelt da.
        if (!erledigt && taeglich && aufgabe.erledigt) {
            val zeilen = dao.alleEinmal()
            var naechste = zeilen.firstOrNull { it.folgeVon == aufgabe.id }
            while (naechste != null) {
                val zeile = naechste
                if (!zeile.erledigt) loesche(zeile)
                naechste = zeilen.firstOrNull { it.folgeVon == zeile.id }
            }
        }
        var folge: Aufgabe? = null
        if (erledigt && aufgabe.wdh != Wiederholung.KEINE) {
            val basis = if (taeglich) tagJetzt ?: heute else maxOf(aufgabe.tag ?: heute, heute - 1)
            Tage.naechster(basis, aufgabe.wdh)?.let { naechster ->
                val neu = aufgabe.copy(
                    id = 0, tag = naechster, folgeVon = aufgabe.id, erledigt = false, erledigtAm = null,
                    erstellt = System.currentTimeMillis(), geaendert = System.currentTimeMillis(),
                    schritteJson = Aufgabe.schritteAlsJson(aufgabe.schritte.map { it.copy(erledigt = false) }),
                )
                folge = neu.copy(id = dao.neu(neu))
            }
        }
        geaendert()
        return folge
    }

    /** Löscht erledigte Aufgaben endgültig, die vor mehr als 30 Tagen abgehakt wurden. Offene bleiben unberührt. */
    suspend fun alteErledigteAufraeumen() {
        val jetzt = System.currentTimeMillis()
        val datiert = dao.erledigtOhneZeitDatieren(jetzt)
        val grenze = erledigtGrenze(jetzt)
        val alt = dao.erledigtVor(grenze)
        if (alt.isNotEmpty()) {
            // Die Tage der Serie bleiben erhalten, auch wenn die Aufgaben selbst verschwinden.
            serieTageMerken(alt.mapNotNull { it.erledigtAm })
            alt.forEach { Planer.storniere(app, it.id); ErinnerungsDienst.stoppe(app, it.id) }
            dao.loescheErledigtVor(grenze)
        }
        if (datiert > 0 || alt.isNotEmpty()) geaendert()
    }

    private val serieSpeicher by lazy { app.getSharedPreferences("aufgaben_serie", Context.MODE_PRIVATE) }

    /** Tage (Epoch-Day) mit erledigten Aufgaben, deren Zeilen schon gelöscht wurden; nur für die Serie. */
    fun archivierteSerieTage(): Set<Long> =
        serieSpeicher.getStringSet("tage", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    private fun serieTageMerken(zeitpunkte: List<Long>) {
        val zone = java.time.ZoneId.systemDefault()
        val neu = zeitpunkte.map { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toEpochDay().toString() }
        serieSpeicher.edit().putStringSet("tage", serieSpeicher.getStringSet("tage", emptySet()).orEmpty() + neu).apply()
    }

    /** Aufräumen mit Protokoll statt stillem Verschlucken; ein Abbruch der Coroutine wird weitergereicht. */
    suspend fun aufraeumenSicher() {
        try { alteErledigteAufraeumen() }
        catch (c: kotlinx.coroutines.CancellationException) { throw c }
        catch (e: Exception) { android.util.Log.w("Aufgaben", "Aufräumen alter erledigter Aufgaben fehlgeschlagen", e) }
    }

    // Einmal je Prozessstart, auch wenn nur Widget, Erinnerung oder die Jarvis-Brücke die App weckt.
    init { nebenbei.launch { aufraeumenSicher() } }

    /** Stellt eine gelöschte Aufgabe mit derselben ID wieder her (Rückgängig). */
    suspend fun wiederherstellen(aufgabe: Aufgabe) {
        dao.neu(aufgabe)
        geaendert()
    }

    private fun geaendert() {
        nebenbei.launch {
            runCatching { Planer.planeAlle(app) }
            runCatching { HeuteWidget.aktualisiere(app) }
            runCatching {
                // Die Wecker-App hört darauf und liest die Aufgaben neu (siehe README, Brücke).
                app.sendBroadcast(Intent(AKTION_GEAENDERT).setPackage("de.frank.genialerwecker"))
            }
        }
    }

    companion object {
        const val AKTION_GEAENDERT = "de.frank.aufgaben.AUFGABEN_GEAENDERT"
        @Volatile private var instanz: AufgabenRepository? = null
        fun get(context: Context): AufgabenRepository = instanz ?: synchronized(this) {
            instanz ?: AufgabenRepository(context).also { instanz = it }
        }
    }
}

/** So viele Tage bleiben erledigte Aufgaben sichtbar; danach werden sie endgültig gelöscht. */
const val ERLEDIGT_TAGE = 30

/** Gemeinsame Grenze für Anzeige und Aufräumen: Was davor erledigt wurde, ist älter als [ERLEDIGT_TAGE] Tage. */
fun erledigtGrenze(jetzt: Long = System.currentTimeMillis()): Long = jetzt - java.util.concurrent.TimeUnit.DAYS.toMillis(ERLEDIGT_TAGE.toLong())
