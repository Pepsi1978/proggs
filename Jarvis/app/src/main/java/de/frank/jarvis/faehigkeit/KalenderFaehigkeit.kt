package de.frank.jarvis.faehigkeit

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Franks Kalender, gelesen über den Kalenderspeicher von Android (alle Kalender, die das Handy
 * synchronisiert, also auch der Google-Kalender). Nur lesend, keine eigene Google-Anmeldung nötig.
 *
 * Dazu der Dienstplan, der in Franks Kalender als Ganztagstermine steht:
 *  - „Nacht 1“ bis „Nacht 4“ = Nachtdienst, Abfahrt etwa 16:00 Uhr
 *  - „Tag 1“ bis „Tag 4“ = Tagdienst, Abfahrt etwa 4:30 Uhr
 *  - „X“ oder „F“ am selben Tag = freier Tag, der Dienst entfällt (der Diensteintrag bleibt stehen)
 *  - „U“ am selben Tag = Urlaub, der Dienst entfällt
 *  - kein Diensteintrag = frei
 */
class KalenderFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "kalender"
    override val name = "Kalender"
    override val beschreibung = "Termine, Geburtstage, Müllabfuhr, Spiele und den Dienstplan aus dem Google-Kalender lesen."
    override val hinweise =
        "Franks Kalender enthält seine Termine und seinen Dienstplan als Ganztagstermine. Die Regeln: „Nacht 1“ bis „Nacht 4“ sind Nachtdienste " +
            "(Nacht 1 ist die erste, Nacht 4 die letzte eines Blocks), Abfahrt etwa 16:00 Uhr, er kommt am nächsten Morgen zurück. „Tag 1“ bis „Tag 4“ sind Tagdienste, " +
            "Abfahrt etwa 4:30 Uhr. Steht am selben Tag zusätzlich „X“ oder „F“, hat er frei, bei „U“ Urlaub: Der Dienst entfällt dann, auch wenn er noch im Kalender steht. " +
            "Tage ohne Diensteintrag sind frei. Die Werkzeuge werten das bereits aus; verlasse dich auf die Zeile „Dienst“. " +
            "Weitere Einträge: „Hausmüll“ und „Gelbe Tonne“ (Abholung), „Geb. <Name>“ (Geburtstag), Spiele von Union und Dortmund. " +
            "Der Kalender ist nur lesbar. Eigene Erinnerungen legst du in Geniale Aufgaben an, nicht im Kalender."

    private val erlaubt: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    override fun stoerung(): String? = if (erlaubt) null else "Der Zugriff auf den Kalender ist noch nicht erlaubt. In Jarvis unter Einrichtung auf „Erlauben“ tippen."

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "kalender_lesen",
            titel = "Kalender lesen",
            beschreibung = "Jarvis: liest Franks Kalender (Google-Kalender) Tag für Tag mit allen Terminen und der ausgewerteten Dienst-Zeile. " +
                "Nutze es für „Was steht heute, morgen oder am Wochenende im Kalender?“, „Wann wird die gelbe Tonne abgeholt?“, „Wann spielt Union?“, „Wann hat Mizi Geburtstag?“. " +
                "Ohne Angaben kommen heute und die nächsten 7 Tage. Mit suche werden nur passende Termine gezeigt, dann reicht der Blick ohne bis ein Jahr voraus.",
            schema = schema(
                "von" to text("Erster Tag: heute, morgen oder JJJJ-MM-TT (Vorgabe: heute)."),
                "bis" to text("Letzter Tag JJJJ-MM-TT (Vorgabe: 7 Tage nach von; mit suche 365 Tage)."),
                "suche" to text("Wort aus dem Titel, z. B. Union, Dortmund, Tonne, Hausmüll, Geb, Mizi."),
            ),
            nurLesen = true,
        ) { a -> mitKalender { kalenderText(a) } },
        Werkzeug(
            name = "dienstplan_lesen",
            titel = "Dienstplan lesen",
            beschreibung = "Jarvis: wertet Franks Dienstplan aus dem Kalender aus: je Tag Nachtdienst, Tagdienst, frei oder Urlaub mit der Abfahrtszeit, dazu der nächste Dienst " +
                "und die nächsten freien Tage. Nutze es für „Muss ich morgen arbeiten?“, „Wann habe ich wieder frei?“, „Wann muss ich heute los?“, „Wie sieht meine Woche aus?“. " +
                "Ohne Angaben kommen heute und die nächsten 14 Tage.",
            schema = schema(
                "von" to text("Erster Tag: heute, morgen oder JJJJ-MM-TT (Vorgabe: heute)."),
                "bis" to text("Letzter Tag JJJJ-MM-TT (Vorgabe: 14 Tage nach von)."),
            ),
            nurLesen = true,
        ) { a -> mitKalender { dienstplanText(a) } },
    )

    private suspend fun mitKalender(block: () -> String): Ergebnis {
        stoerung()?.let { return Ergebnis(it, fehler = true) }
        return withContext(Dispatchers.IO) {
            runCatching { Ergebnis(block()) }.getOrElse { Ergebnis("Der Kalender ließ sich nicht lesen: ${it.message ?: it.javaClass.simpleName}", fehler = true) }
        }
    }

    // ---------------------------------------------------------------- Daten

    private class Termin(val titel: String, val ganztags: Boolean, val von: LocalTime?, val bis: LocalTime?, val ort: String, val kalender: String)

    /** Der Dienst eines Tages, schon mit X, F und U verrechnet. */
    private class Dienst(val art: Art, val nummer: Int, val frei: String?) {
        enum class Art { NACHT, TAG, KEINER }
        val arbeitet: Boolean get() = art != Art.KEINER && frei == null
    }

    /** Heutiger Tagesstand für andere Stellen (z. B. den Jarvis-Status). Leer, wenn der Zugriff fehlt. */
    fun heuteKurz(): String = if (!erlaubt) "" else runCatching {
        val heute = LocalDate.now()
        val tage = lies(heute.minusDays(1), heute)
        dienstSatz(dienst(tage[heute].orEmpty()), dienst(tage[heute.minusDays(1)].orEmpty()))
    }.getOrDefault("")

    /**
     * Der Rahmen der nächsten Tage nach festen Regeln: Dienst, Schlafzeiten und die Stunden, in denen Frank
     * etwas erledigen kann. Bewusst gerechnet und nicht vom Modell gedeutet, damit Zeitkonflikte stimmen.
     * Leer, wenn der Kalender-Zugriff fehlt.
     */
    fun rahmen(tage: Int = 6): String = if (!erlaubt) "" else runCatching {
        val heute = LocalDate.now()
        val termine = lies(heute.minusDays(1), heute.plusDays(tage.toLong()))
        fun d(tag: LocalDate) = dienst(termine[tag].orEmpty())
        buildString {
            for (i in 0 until tage) {
                val tag = heute.plusDays(i.toLong())
                val heuteD = d(tag)
                val gestern = d(tag.minusDays(1))
                val morgen = d(tag.plusDays(1))
                val nachNacht = gestern.arbeitet && gestern.art == Dienst.Art.NACHT
                append("- ").append(kurz(tag, heute)).append(": ")
                append(if (heuteD.arbeitet) "ARBEITSTAG, " + name(heuteD) else "FREIER TAG" + (heuteD.frei?.let { " ($it)" } ?: ""))
                val fakten = mutableListOf<String>()
                if (nachNacht) fakten += "am Morgen Rückkehr aus dem Nachtdienst, Schlaf etwa 6 bis 15 Uhr, davor und währenddessen keine Aufgaben"
                when {
                    heuteD.arbeitet && heuteD.art == Dienst.Art.TAG -> {
                        fakten += "Aufstehen etwa 4:00 Uhr, Abfahrt etwa $ABFAHRT_TAG Uhr, tagsüber im Dienst"
                        fakten += if (morgen.arbeitet && morgen.art == Dienst.Art.TAG) "abends nur kurz Zeit: Schlafengehen gegen 20 Uhr, weil morgen wieder Tagdienst ist" else "nach dem Dienst abends frei"
                    }
                    heuteD.arbeitet && heuteD.art == Dienst.Art.NACHT -> {
                        fakten += if (nachNacht) "freie Zeit nur etwa 15 bis 16 Uhr" else "erster Nachtdienst des Blocks: vormittags und mittags frei, Vorschlafen am Nachmittag sinnvoll"
                        fakten += "Abfahrt etwa $ABFAHRT_NACHT Uhr, Dienst über Nacht"
                    }
                    else -> {
                        if (!nachNacht) fakten += "tagsüber frei verfügbar" else fakten += "frei verfügbar ab etwa 15 Uhr"
                        if (morgen.arbeitet && morgen.art == Dienst.Art.TAG) fakten += "Schlafengehen gegen 20 Uhr, weil morgen Tagdienst ist (Aufstehen 4:00 Uhr); nach 20 Uhr nichts mehr einplanen"
                        if (morgen.arbeitet && morgen.art == Dienst.Art.NACHT) fakten += "morgen beginnt ein Nachtdienst-Block: Vorbereitungen dafür heute erledigen"
                    }
                }
                append(" – ").append(fakten.joinToString("; ")).append('\n')
            }
        }.trim()
    }.getOrDefault("")

    /** Ist heute ein Arbeitstag? null, wenn der Kalender nicht lesbar ist. */
    fun arbeitetHeute(): Boolean? = if (!erlaubt) null else runCatching {
        val heute = LocalDate.now()
        dienst(lies(heute, heute)[heute].orEmpty()).arbeitet
    }.getOrNull()

    /** Alle Termine je Tag. Mehrtägige Ganztagstermine stehen an jedem ihrer Tage. */
    private fun lies(von: LocalDate, bis: LocalDate): Map<LocalDate, List<Termin>> {
        val zone = ZoneId.systemDefault()
        // Ganztagstermine liegen im Speicher auf UTC-Mitternacht: einen Tag Rand lassen und danach nach Datum filtern.
        val start = von.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val ende = bis.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val adresse = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start)
            ContentUris.appendId(it, ende)
        }.build()
        val spalten = arrayOf(
            CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION, CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )
        val tage = HashMap<LocalDate, MutableList<Termin>>()
        val gesehen = HashSet<String>()
        context.contentResolver.query(adresse, spalten, "${CalendarContract.Instances.VISIBLE} = 1", null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                val titel = c.getString(0)?.trim().orEmpty()
                if (titel.isEmpty()) continue
                val beginn = c.getLong(1)
                val schluss = c.getLong(2)
                val ganztags = c.getInt(3) == 1
                // Derselbe Termin kann in mehreren Kalendern liegen: nur einmal zeigen.
                if (!gesehen.add("$titel|$beginn|$schluss")) continue
                val ort = c.getString(4)?.trim().orEmpty()
                val kalender = c.getString(5)?.trim().orEmpty()
                if (ganztags) {
                    val erster = Instant.ofEpochMilli(beginn).atZone(ZoneOffset.UTC).toLocalDate()
                    val nachLetztem = Instant.ofEpochMilli(schluss).atZone(ZoneOffset.UTC).toLocalDate()
                    var tag = erster
                    do {
                        if (!tag.isBefore(von) && !tag.isAfter(bis)) tage.getOrPut(tag) { mutableListOf() } += Termin(titel, true, null, null, ort, kalender)
                        tag = tag.plusDays(1)
                    } while (tag.isBefore(nachLetztem))
                } else {
                    val anfang = Instant.ofEpochMilli(beginn).atZone(zone)
                    val tag = anfang.toLocalDate()
                    if (tag.isBefore(von) || tag.isAfter(bis)) continue
                    tage.getOrPut(tag) { mutableListOf() } += Termin(titel, false, anfang.toLocalTime(), Instant.ofEpochMilli(schluss).atZone(zone).toLocalTime(), ort, kalender)
                }
            }
        }
        return tage
    }

    private fun dienst(termine: List<Termin>): Dienst {
        var art = Dienst.Art.KEINER
        var nummer = 0
        var frei: String? = null
        termine.filter { it.ganztags }.forEach { t ->
            val titel = t.titel.trim().lowercase(Locale.GERMAN)
            DIENST.matchEntire(titel)?.let { treffer ->
                art = if (treffer.groupValues[1] == "nacht") Dienst.Art.NACHT else Dienst.Art.TAG
                nummer = treffer.groupValues[2].toInt()
            }
            when (titel) {
                "x", "f" -> if (frei == null) frei = "frei (${titel.uppercase()})"
                "u" -> frei = "Urlaub (U)"
            }
        }
        return Dienst(art, nummer, frei)
    }

    private fun istDienstzeichen(t: Termin): Boolean =
        t.ganztags && t.titel.trim().lowercase(Locale.GERMAN).let { DIENST.matches(it) || it in setOf("x", "f", "u") }

    /** [vortag] = Dienst des Tages davor: Nach einem Nachtdienst kommt Frank erst am Morgen nach Hause. */
    private fun dienstSatz(d: Dienst, vortag: Dienst? = null): String {
        val satz = when {
            d.art == Dienst.Art.KEINER && d.frei != null -> d.frei
            d.art == Dienst.Art.KEINER -> "frei (kein Dienst eingetragen)"
            d.frei != null -> "${d.frei} – der eingetragene ${name(d)} entfällt"
            d.art == Dienst.Art.NACHT -> "${name(d)}, Abfahrt etwa $ABFAHRT_NACHT Uhr, Rückkehr am nächsten Morgen"
            else -> "${name(d)}, Abfahrt etwa $ABFAHRT_TAG Uhr"
        }
        val nachNacht = vortag != null && vortag.arbeitet && vortag.art == Dienst.Art.NACHT
        return if (nachNacht) "$satz; am Morgen Rückkehr aus dem Nachtdienst (Nacht ${vortag!!.nummer})" else satz
    }

    private fun name(d: Dienst): String = (if (d.art == Dienst.Art.NACHT) "Nachtdienst (Nacht " else "Tagdienst (Tag ") + d.nummer + ")"

    // ---------------------------------------------------------------- Texte

    private fun zeitraum(a: JSONObject, tageVorgabe: Long): Pair<LocalDate, LocalDate> {
        val von = datum(a.optString("von")) ?: LocalDate.now()
        val bis = datum(a.optString("bis")) ?: von.plusDays(tageVorgabe)
        // Höchstens zwei Jahre, damit eine falsche Angabe nicht den ganzen Speicher durchläuft.
        return von to (if (bis.isBefore(von)) von else minOf(bis, von.plusDays(731)))
    }

    private fun kalenderText(a: JSONObject): String {
        val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
        val (von, bis) = zeitraum(a, if (suche.isEmpty()) 7 else 365)
        val tage = lies(von.minusDays(1), bis)
        val heute = LocalDate.now()
        return buildString {
            append("Heute ist ").append(lang(heute)).append(". Kalender von ").append(kurz(von, heute)).append(" bis ").append(kurz(bis, heute))
            if (suche.isNotEmpty()) append(", Suche „").append(suche).append("“")
            append(":\n")
            var treffer = 0
            var tag = von
            while (!tag.isAfter(bis)) {
                val alle = tage[tag].orEmpty()
                val gezeigt = alle.filter { !istDienstzeichen(it) && (suche.isEmpty() || suche in it.titel.lowercase(Locale.GERMAN)) }
                    .sortedWith(compareBy({ !it.ganztags }, { it.von }))
                if (suche.isEmpty()) {
                    append(kurz(tag, heute)).append(" – Dienst: ").append(dienstSatz(dienst(alle), dienst(tage[tag.minusDays(1)].orEmpty())))
                    if (gezeigt.isEmpty()) append("; keine weiteren Termine")
                    append('\n')
                } else if (gezeigt.isNotEmpty()) {
                    val abstand = ChronoUnit.DAYS.between(heute, tag)
                    append(kurz(tag, heute))
                    if (abstand > 1) append(" (in ").append(abstand).append(" Tagen)")
                    append(":\n")
                }
                gezeigt.take(40).forEach { t ->
                    treffer++
                    append("  - ")
                    if (t.ganztags) append("ganztägig: ") else append(t.von.toString().take(5)).append("–").append(t.bis.toString().take(5)).append(" Uhr: ")
                    append(t.titel)
                    if (t.ort.isNotEmpty()) append(" (").append(t.ort.take(80)).append(")")
                    append('\n')
                }
                tag = tag.plusDays(1)
                if (treffer >= 200) { append("… weitere Termine ausgelassen, bitte Zeitraum oder Suche eingrenzen.\n"); break }
            }
            if (suche.isNotEmpty() && treffer == 0) append("Kein Termin passt.")
        }.trim()
    }

    private fun dienstplanText(a: JSONObject): String {
        val (von, bis) = zeitraum(a, 14)
        val tage = lies(von.minusDays(1), bis)
        val heute = LocalDate.now()
        val dienste = generateSequence(von) { it.plusDays(1) }.takeWhile { !it.isAfter(bis) }.associateWith { dienst(tage[it].orEmpty()) }
        return buildString {
            append("Heute ist ").append(lang(heute)).append(", ").append(LocalTime.now().toString().take(5)).append(" Uhr. Dienstplan von ").append(kurz(von, heute)).append(" bis ").append(kurz(bis, heute)).append(":\n")
            dienste.forEach { (tag, d) ->
                append("- ").append(kurz(tag, heute)).append(": ").append(dienstSatz(d, dienste[tag.minusDays(1)] ?: dienst(tage[tag.minusDays(1)].orEmpty()))).append('\n')
            }
            val naechsterDienst = dienste.entries.firstOrNull { it.value.arbeitet && !it.key.isBefore(heute) }
            // Frei „wieder“ heißt: der erste freie Tag NACH dem nächsten Dienst. Arbeitet Frank im Zeitraum gar nicht, der erste freie Tag.
            val naechsterFrei = dienste.entries.firstOrNull { !it.value.arbeitet && !it.key.isBefore(naechsterDienst?.key ?: heute) }
            dienste[heute]?.let { append("Heute: ").append(if (it.arbeitet) name(it) else "frei").append(".\n") }
            append("Nächster Dienst: ").append(naechsterDienst?.let { kurz(it.key, heute) + ", " + name(it.value) } ?: "keiner im Zeitraum").append(".\n")
            if (naechsterFrei != null) {
                // Länge des freien Blocks ab dem ersten freien Tag.
                val laenge = dienste.entries.dropWhile { it.key != naechsterFrei.key }.takeWhile { !it.value.arbeitet }.size
                append(if (naechsterDienst != null) "Danach wieder frei ab: " else "Nächster freier Tag: ").append(kurz(naechsterFrei.key, heute)).append(", ").append(laenge).append(if (laenge == 1) " Tag" else " Tage").append(" am Stück")
                if (!dienste.entries.any { it.key.isAfter(naechsterFrei.key) && it.value.arbeitet }) append(" (mindestens, bis zum Ende des Zeitraums)")
                append(".\n")
            }
            append("Arbeitstage im Zeitraum: ").append(dienste.values.count { it.arbeitet }).append(", freie Tage: ").append(dienste.values.count { !it.arbeitet }).append(".")
        }.trim()
    }

    private fun datum(text: String): LocalDate? = when (text.trim().lowercase(Locale.GERMAN)) {
        "" -> null
        "heute" -> LocalDate.now()
        "morgen" -> LocalDate.now().plusDays(1)
        "uebermorgen", "übermorgen" -> LocalDate.now().plusDays(2)
        "gestern" -> LocalDate.now().minusDays(1)
        else -> runCatching { LocalDate.parse(text.trim().take(10)) }.getOrNull()
    }

    private fun lang(tag: LocalDate): String = tag.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))

    /** „Sa 10.10.“, bei heute und morgen mit Zusatz; mit Jahr, wenn es nicht das laufende ist. */
    private fun kurz(tag: LocalDate, heute: LocalDate): String {
        val muster = if (tag.year == heute.year) "EE d.M." else "EE d.M.yyyy"
        val zusatz = when (ChronoUnit.DAYS.between(heute, tag)) { 0L -> " (heute)"; 1L -> " (morgen)"; else -> "" }
        return tag.format(DateTimeFormatter.ofPattern(muster, Locale.GERMAN)) + zusatz
    }

    companion object {
        private val DIENST = Regex("(nacht|tag)\\s*([1-4])")
        private const val ABFAHRT_NACHT = "16:00"
        private const val ABFAHRT_TAG = "4:30"
    }
}
