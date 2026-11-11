package de.frank.jarvis.bio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import de.frank.jarvis.bio.data.local.AppDatabase
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Übernimmt den Biomarker-Bereich einmalig und verlustfrei aus Entropie Reductor (Franks Vorgabe vom 11.11.2026). Nur auf dem
 * Handy; das Tablet bekommt die Daten danach über den Spiegel.
 *
 * Ablauf, damit der Whoop-Zugang nie in zwei Apps zugleich lebt (Whoop tauscht bei jedem Refresh den Refresh-Token):
 *  1. Entropie Reductor friert ein (`uebergabe_vorbereiten`): kein Token-Tausch, kein Abruf mehr dort; ein laufender Abgleich und
 *     ein laufender Refresh werden abgewartet; die Datenbank wird auf die Platte geschrieben; Zugangsdaten und Abrufstände kommen zurück.
 *  2. Jarvis kopiert die Biomarker-Tabellen spaltengenau (INSERT OR REPLACE, wiederholbar) und zählt die Zeilen nach.
 *  3. Jarvis rechnet Katalog und Tageswerte selbst und vergleicht sie mit der alten Brücke.
 *  4. Erst wenn alles stimmt, gilt die Übernahme ([Einstellungen.bioUebernommenMs]); Entropie Reductor gibt die
 *     Whoop-Rückmeldeadresse ab (`uebergabe_bestaetigen`). Scheitert etwas vorher, führt Entropie Reductor weiter wie bisher
 *     (`uebergabe_zurueck`), und Jarvis versucht es beim nächsten Start erneut.
 * Vor Schritt 4 nutzt Jarvis die übernommenen Zugangsdaten nie ([BioAbruf.bereit]).
 */
object BioUebernahme {
    private val ADRESSEN = listOf(Uri.parse("content://de.frank.entropyreducer.jarvis"), Uri.parse("content://de.frank.entropyreducer.debug.jarvis"))
    private val sperre = Mutex()
    private val ER_PAKETE = listOf("de.frank.entropyreducer", "de.frank.entropyreducer.debug")

    fun noetig(context: Context): Boolean = !Geraet.istTablet && Einstellungen.get(context).bioUebernommenMs == 0L

    /** Führt die Übernahme aus, falls nötig. Gibt einen Satz für Logbuch und Anzeige zurück. */
    suspend fun starte(context: Context): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        if (!noetig(app)) return@withContext "Biomarker sind schon in Jarvis."
        if (!sperre.tryLock()) return@withContext "Die Übernahme läuft bereits."
        try {
            BioModul.einrichten(app)
            // Gar kein Entropie Reductor installiert (neues Handy, App entfernt): Jarvis beginnt mit leerem Bereich, statt nie abzurufen.
            if (ER_PAKETE.none { p -> runCatching { app.packageManager.getPackageInfo(p, 0) }.isSuccess }) {
                Einstellungen.get(app).bioUebernommenMs = System.currentTimeMillis()
                return@withContext melde("Kein Entropie Reductor installiert: Jarvis führt die Biomarker ab jetzt ohne alte Daten; Zugänge unter Biomarker → Einstellungen eintragen.")
            }
            val (adresse, stand) = ADRESSEN.firstNotNullOfOrNull { a -> rufe(app, a, "uebergabe_stand")?.let { a to it } }
                ?: return@withContext melde("Entropie Reductor antwortet gerade nicht; die Übernahme folgt beim nächsten Start.", fehler = true)
            if (!stand.has("abgegeben")) {
                return@withContext melde("Entropie Reductor ist zu alt für die Übergabe (mindestens 0.29.29 nötig).", fehler = true)
            }
            // Zwei Versuche: Schreibt Entropie Reductor zwischen Schnappschuss und Vergleich noch etwas (Aufräumen beim Start),
            // passt der Vergleich beim zweiten, frischen Schnappschuss.
            var letzterFehler: Exception? = null
            for (versuch in 1..2) {
                letzterFehler = versuche(app, adresse) ?: break
            }
            letzterFehler?.let { e ->
                Logbuch.fehler("Biomarker-Übernahme", e)
                return@withContext melde("Übernahme abgebrochen, Entropie Reductor führt die Biomarker weiter: ${e.message ?: e.javaClass.simpleName}", fehler = true)
            }
            Einstellungen.get(app).bioUebernommenMs = System.currentTimeMillis()
            bestaetigen(app)
            BioSpiegel.geaendert(app, "Übernahme aus Entropie Reductor")
            BioAbruf.neuGelesen(app)
            melde("Biomarker aus Entropie Reductor übernommen: alle Tabellen, Whoop- und Oura-Zugang, Abrufstände und Kartenreihenfolge. Ab jetzt ruft Jarvis selbst ab.")
        } finally {
            sperre.unlock()
        }
    }

    /**
     * Entropie Reductor gibt die Whoop-Rückmeldeadresse ab und löscht seinen Schnappschuss. Bis das bestätigt ist, wird es bei
     * jedem Start erneut versucht ([nachholen]).
     */
    private fun bestaetigen(app: Context) {
        val ok = ADRESSEN.any { a -> rufe(app, a, "uebergabe_bestaetigen")?.optBoolean("ok") == true }
        app.getSharedPreferences("bio_uebernahme", Context.MODE_PRIVATE).edit().putBoolean("bestaetigt", ok).apply()
    }

    /** Beim Start: eine früher nicht angekommene Bestätigung nachholen (nur Handy, nur nach der Übernahme). */
    fun nachholen(context: Context) {
        val app = context.applicationContext
        if (Geraet.istTablet || Einstellungen.get(app).bioUebernommenMs == 0L) return
        if (app.getSharedPreferences("bio_uebernahme", Context.MODE_PRIVATE).getBoolean("bestaetigt", true)) return
        bestaetigen(app)
    }

    /** Ein Versuch: einfrieren, kopieren, zählen, vergleichen, Zugänge übernehmen. Null = gelungen, sonst der Fehler (Entropie Reductor ist dann wieder aufgetaut). */
    private suspend fun versuche(app: Context, adresse: Uri): Exception? {
            val paket = rufe(app, adresse, "uebergabe_vorbereiten")
            if (paket == null || paket.optString("fehler").isNotEmpty()) {
                // Entropie Reductor könnte schon eingefroren sein: auftauen, damit nicht keine App mehr abruft.
                rufe(app, adresse, "uebergabe_zurueck")
                return IllegalStateException("Übergabe noch nicht möglich: " + (paket?.optString("fehler") ?: "keine Antwort von Entropie Reductor"))
            }
            try {
                val kopie = File(app.cacheDir, "er_uebernahme.db")
                try {
                    app.contentResolver.openInputStream(adresse.buildUpon().appendPath("datenbank").build())?.use { ein -> kopie.outputStream().use { ein.copyTo(it) } }
                        ?: error("Die Datenbank von Entropie Reductor ließ sich nicht öffnen.")
                    val gezaehlt = kopiere(kopie)
                    val erwartet = paket.optJSONObject("zeilen") ?: JSONObject()
                    val abweichend = AppDatabase.TABELLEN.filter { t -> erwartet.has(t) && erwartet.optLong(t) != gezaehlt[t] }
                    check(abweichend.isEmpty()) { "Zeilenzahl weicht ab: " + abweichend.joinToString { "$it ${gezaehlt[it]} statt ${erwartet.optLong(it)}" } }
                } finally {
                    listOf("", "-journal", "-wal", "-shm").forEach { File(kopie.path + it).delete() }
                }
                vergleiche(app, adresse)
                // Zugangsdaten erst nach bestandenem Vergleich, und solange die Übernahme nicht gilt, immer die frischen aus
                // Entropie Reductor: Ein früherer, gescheiterter Versuch darf keinen inzwischen verbrauchten Whoop-Token hinterlassen.
                paket.optJSONObject("geheim")?.let { BioModul.geheim.uebernimm(it) }
                paket.optJSONObject("einstellungen")?.let { BioModul.einstellungen.uebernimm(it) }
                paket.optJSONArray("karten_reihenfolge")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() } }
                    ?.takeIf { it.isNotEmpty() }?.let { BioModul.kartenReihenfolge.saveOrder(it) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Abgebrochen (Dienst beendet): Entropie Reductor wieder auftauen und den Abbruch weiterreichen.
                withContext(kotlinx.coroutines.NonCancellable) { rufe(app, adresse, "uebergabe_zurueck") }
                throw e
            } catch (e: Exception) {
                rufe(app, adresse, "uebergabe_zurueck")
                return e
            }
            return null
    }

    private fun kopiere(kopie: File): Map<String, Long> = BioTabellenKopie.kopiere(kopie)

    /** Rechnet Jarvis dasselbe wie Entropie Reductor? Katalog (Tage, von, bis je Messgröße) und der neueste Tag müssen gleich sein. */
    private suspend fun vergleiche(app: Context, adresse: Uri) {
        val eigen = BioAuswertung(app)
        val altK = rufe(app, adresse, "katalog") ?: error("Vergleich: Katalog aus Entropie Reductor fehlt")
        val neuK = eigen.rufe("katalog", JSONObject())
        neuK.optString("fehler").takeIf { it.isNotEmpty() }?.let { error("Jarvis' eigene Rechnung scheitert: $it") }
        fun karte(o: JSONObject): Map<String, String> = (o.optJSONArray("metriken") ?: JSONArray()).let { a ->
            (0 until a.length()).associate { i -> a.getJSONObject(i).let { m -> m.optString("id") to "${m.optInt("tage")} ${m.optString("von")} ${m.optString("bis")}" } }
        }
        val alt = karte(altK); val neu = karte(neuK)
        val unterschiede = alt.keys.filter { alt[it] != neu[it] }
        check(unterschiede.isEmpty()) { "Rechnung weicht ab bei " + unterschiede.take(5).joinToString { "$it (${alt[it]} gegen ${neu[it]})" } }
        val altT = rufe(app, adresse, "tag") ?: error("Vergleich: Tageswerte aus Entropie Reductor fehlen")
        val neuT = eigen.rufe("tag", JSONObject().put("datum", altT.optString("datum")))
        check(altT.optJSONArray("werte")?.toString() == neuT.optJSONArray("werte")?.toString()) { "Tageswerte vom ${altT.optString("datum")} weichen ab" }
        Logbuch.system("Biomarker-Übernahme geprüft", "${alt.size} Messgrößen und die Tageswerte vom ${altT.optString("datum")} stimmen überein")
    }

    private fun rufe(app: Context, adresse: Uri, methode: String, anfrage: JSONObject = JSONObject()): JSONObject? = runCatching {
        app.contentResolver.call(adresse, methode, null, Bundle().apply { putString("json", anfrage.toString()) })?.getString("json")?.let(::JSONObject)
    }.getOrNull()

    private fun melde(text: String, fehler: Boolean = false): String {
        if (fehler) Logbuch.fehler("Biomarker-Übernahme", null, text) else Logbuch.system("Biomarker-Übernahme", text)
        return text
    }
}
