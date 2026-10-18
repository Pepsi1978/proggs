package de.frank.jarvis.ablage

import android.content.Context
import java.io.File
import org.json.JSONObject

/**
 * Vorläufige Ergebnisse: Berichte, Bilder und PDFs, die Jarvis für ein Gespräch oder einen Hintergrundauftrag erzeugt,
 * ohne dass Frank sie dauerhaft behalten wollte. Gleicher Aufbau wie die Ablage (eigener [AblageSpeicher] unter
 * `filesDir/vorlaeufig`), aber nicht im Ablage-Reiter, nicht im Gesamtbild und nicht in ablage_lesen als Ablage-Eintrag.
 *
 * Dauerhaft wird etwas erst auf Franks ausdrücklichen Wunsch: [dateiInAblage] (Knopf „In Ablage verschieben“ am Bild)
 * oder [eintragInAblage] (Werkzeug in_ablage_uebernehmen). Erfolg erst, wenn die Datei in der Ablage liegt; danach
 * verschwindet die vorläufige Kopie, und [aufloesen] führt alte Chat-Verweise auf die neue Datei.
 * Was nicht übernommen wird, räumt [aufraeumen] nach [AUFBEWAHRUNG_TAGE] Tagen ab.
 */
object Vorlaeufig {
    const val AUFBEWAHRUNG_TAGE = 30

    fun wurzel(context: Context): File = File(context.applicationContext.filesDir, "vorlaeufig")
    fun speicher(context: Context): AblageSpeicher = AblageSpeicher.fuer(wurzel(context))

    /** Liegt [pfad] im vorläufigen Speicher (und nicht in der Ablage)? */
    fun istVorlaeufig(context: Context, pfad: String): Boolean = pfad.startsWith(wurzel(context).absolutePath + File.separator)

    /** Liegt [pfad] in den Dateien der dauerhaften Ablage? */
    fun istDauerhaft(context: Context, pfad: String): Boolean =
        pfad.startsWith(File(context.applicationContext.filesDir, "ablage-dateien").absolutePath + File.separator)

    // ---- Umzüge: vorläufiger Pfad → Pfad in der Ablage ----

    private val sperre = Any()
    private fun umzugDatei(context: Context) = File(wurzel(context), "umzuege.json")

    private fun umzuege(context: Context): JSONObject = synchronized(sperre) {
        runCatching { JSONObject(umzugDatei(context).readText()) }.getOrElse { JSONObject() }
    }

    private fun merkeUmzug(context: Context, von: String, nach: String) = synchronized(sperre) {
        val j = umzuege(context).put(von, nach)
        umzugDatei(context).parentFile?.mkdirs()
        AblageSpeicher.schreibeAtomar(umzugDatei(context), j.toString().toByteArray())
    }

    /** Die Datei hinter einem Chat-Verweis: die vorläufige, oder nach dem Übernehmen die in der Ablage. null = nicht mehr da. */
    fun aufloesen(context: Context, pfad: String): File? {
        val f = File(pfad)
        if (f.isFile) return f
        return umzuege(context).optString(pfad).takeIf { it.isNotEmpty() }?.let(::File)?.takeIf { it.isFile }
    }

    /** Wurde [pfad] schon in die Ablage übernommen? Dann der Titel des Eintrags dort. */
    fun uebernommenNach(context: Context, pfad: String): String? {
        val ziel = if (istDauerhaft(context, pfad)) pfad else umzuege(context).optString(pfad).takeIf { it.isNotEmpty() } ?: return null
        val echt = AblageZentrale.speicher(context)
        return echt.eintraege().firstOrNull { e -> e.anhaenge.any { echt.datei(it).absolutePath == ziel } }?.titel
    }

    // ---- In die Ablage übernehmen ----

    /** Ergebnis von [dateiInAblage]: der Eintrag in der Ablage, der neue Pfad und ob die Datei schon dort lag. */
    data class Uebernommen(val titel: String, val pfad: String, val schonDa: Boolean, val meldung: String)

    /**
     * Übernimmt die vorläufige Datei [pfad] dauerhaft in die Ablage, in einen Eintrag mit dem Titel ihres vorläufigen Eintrags.
     * Kehrt erst zurück, wenn die Datei dort liegt (sonst wirft es [AblageFehler]); dann erst wird die vorläufige Kopie entfernt.
     * Liegt sie schon in der Ablage (gleiche Kennung oder gleicher Inhalt), entsteht keine zweite.
     */
    fun dateiInAblage(context: Context, pfad: String): Uebernommen {
        uebernommenNach(context, pfad)?.let { titel ->
            val ziel = if (istDauerhaft(context, pfad)) pfad else umzuege(context).optString(pfad)
            return Uebernommen(titel, ziel, schonDa = true, meldung = "Liegt schon in der Ablage im Eintrag „$titel“.")
        }
        val v = speicher(context)
        val (eintrag, anhang) = v.eintraege().firstNotNullOfOrNull { e -> e.anhaenge.firstOrNull { v.datei(it).absolutePath == pfad }?.let { e to it } }
            ?: throw AblageFehler("Die vorläufige Datei gibt es nicht mehr (vorläufige Dateien bleiben $AUFBEWAHRUNG_TAGE Tage). Es wurde nichts gespeichert.")
        val u = uebernimmAnhang(context, v, eintrag, anhang, eintrag.titel)
        val ziel = AblageZentrale.speicher(context).datei(u.anhang).absolutePath
        return Uebernommen(u.eintrag.titel, ziel, schonDa = !u.neu, meldung = AblageZentrale.meldung(u))
    }

    /**
     * Übernimmt den ganzen vorläufigen Eintrag [titel] (Text und Dateien) dauerhaft in die Ablage, unter [zielTitel] oder
     * seinem eigenen Titel. Ein vorhandener Text gleichen Titels wird ergänzt, nie überschrieben. Rückgabe: Meldung je Teil.
     */
    fun eintragInAblage(context: Context, titel: String, zielTitel: String? = null): Pair<String, List<String>> {
        val v = speicher(context)
        val e = v.findeGenau(titel) ?: v.finde(titel)
            ?: throw AblageFehler("Einen vorläufigen Eintrag „$titel“ gibt es nicht (mehr). Es wurde nichts gespeichert.")
        val ziel = zielTitel?.trim()?.takeIf { it.isNotEmpty() } ?: e.titel
        val echt = AblageZentrale.speicher(context)
        val zeilen = mutableListOf<String>()
        v.textDatei(e)?.readText()?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
            val vorhanden = echt.findeGenau(ziel)
            if (vorhanden?.hatText == true && echt.text(vorhanden).contains(text)) {
                zeilen += "Text war schon gespeichert."
            } else {
                val (eintrag, datei) = echt.schreibeText(vorhanden?.titel ?: ziel, text, anhaengen = vorhanden?.hatText == true, herkunft = e.herkunft)
                if (!datei.isFile || datei.length() == 0L) throw AblageFehler("Der Text ließ sich nicht in die Ablage schreiben; es wurde nichts verschoben.")
                zeilen += "Text gespeichert im Eintrag „${eintrag.titel}“."
            }
        }
        var zielEintrag = echt.findeGenau(ziel)?.titel ?: ziel
        e.anhaenge.forEach { a ->
            val u = uebernimmAnhang(context, v, e, a, zielEintrag)
            zielEintrag = u.eintrag.titel
            zeilen += AblageZentrale.meldung(u)
        }
        if (zeilen.isEmpty()) throw AblageFehler("„${e.titel}“ ist leer; es wurde nichts gespeichert.")
        // Alles liegt in der Ablage: erst jetzt den vorläufigen Eintrag (mit Text) entfernen.
        v.loescheEintrag(e.id)
        return zielEintrag to zeilen
    }

    private fun uebernimmAnhang(context: Context, v: AblageSpeicher, eintrag: Eintrag, anhang: Anhang, zielTitel: String): Uebernahme {
        val echt = AblageZentrale.speicher(context)
        val quelle = v.datei(anhang)
        if (!quelle.isFile) throw AblageFehler("„${anhang.originalName}“ fehlt im vorläufigen Speicher; es wurde nichts gespeichert.")
        val teil = echt.neueTeilDatei()
        try {
            quelle.copyTo(teil, overwrite = true)
            val u = echt.uebernimm(zielTitel, teil, anhang.originalName, anhang.mime, anhang.herkunft, anhang.beschreibung,
                anhang.kennung ?: "vorlaeufig-${anhang.id}", anhang.titel)
            val ziel = echt.datei(u.anhang)
            if (!ziel.isFile || ziel.length() == 0L) throw AblageFehler("„${anhang.originalName}“ kam nicht vollständig in der Ablage an; die vorläufige Datei bleibt erhalten.")
            AblageZentrale.nachDemSpeichern(context, u)
            merkeUmzug(context, quelle.absolutePath, ziel.absolutePath)
            v.loescheAnhang(eintrag.id, anhang.id)
            return u
        } finally {
            teil.delete()
        }
    }

    /** Beim Start: vorläufige Einträge, die älter als [AUFBEWAHRUNG_TAGE] Tage sind, entfernen; Reste wie in der Ablage aufräumen. */
    fun aufraeumen(context: Context) {
        val v = speicher(context)
        val grenze = System.currentTimeMillis() - AUFBEWAHRUNG_TAGE * 24 * 3_600_000L
        v.eintraege().filter { it.geaendert < grenze }.forEach { runCatching { v.loescheEintrag(it.id) } }
        runCatching { v.aufraeumen() }
    }
}
