package de.frank.gedankenspeicher.data

import android.content.Context
import de.frank.gedankenspeicher.audio.GroqTranscriber
import de.frank.gedankenspeicher.auth.CodexAuthManager
import de.frank.gedankenspeicher.auth.CodexModel
import de.frank.gedankenspeicher.auth.ReasoningEffort
import de.frank.gedankenspeicher.data.settings.Einstellungen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * **Alles, was mit Daten geschieht, geht hier durch.**
 *
 * Die Oberfläche kennt weder Room noch Groq noch Codex — sie ruft hier an. Das ist nicht nur
 * Ordnung: die Regeln, die das Spec aufstellt (nur einmal verbessern, Überschrift von Hand
 * schlägt KI, Kontext endet bei der letzten Auswertung), stehen dadurch an **einer** Stelle
 * und können nicht in zwei Bildschirmen auseinanderlaufen.
 */
class Repository(
    private val ctx: Context,
    private val db: Datenbank,
    val einstellungen: Einstellungen,
    val codex: CodexAuthManager,
) {

    // --- Sitzungen (F-12, F-13) -----------------------------------------------------------

    val sitzungen: Flow<List<Sitzung>> = db.sitzungen().alle()

    val ordner: Flow<List<Ordner>> = db.ordner().alle()

    /**
     * Sorgt dafür, dass immer mindestens eine Sitzung da ist, und liefert die offene.
     *
     * Die App steht nie ohne Sitzung da (F-12, Fehlerfall) — deshalb legt dieser Aufruf
     * beim ersten Start eine an, statt einen leeren Zustand zuzulassen, den die Oberfläche
     * dann irgendwie darstellen müsste.
     */
    suspend fun offeneSitzung(): Sitzung {
        val gemerkt = einstellungen.offeneSitzung
        // Eine Sitzung im Papierkorb wird nicht wieder geöffnet — sonst stünde sie beim
        // nächsten Start wieder da, obwohl Frank sie weggeräumt hat.
        db.sitzungen().eine(gemerkt)?.takeIf { it.geloeschtAm == null }?.let { return it }
        db.sitzungen().zuletztGeoeffnete()?.let {
            einstellungen.offeneSitzung = it.id
            return it
        }
        return neueSitzung()
    }

    suspend fun neueSitzung(): Sitzung {
        val jetzt = System.currentTimeMillis()
        val id = db.sitzungen().einfuegen(
            Sitzung(erstelltAm = jetzt, zuletztGeoeffnet = jetzt, zuletztGeaendert = jetzt),
        )
        einstellungen.offeneSitzung = id
        return db.sitzungen().eine(id)!!
    }

    suspend fun oeffneSitzung(id: Long) {
        db.sitzungen().merkeOeffnung(id, System.currentTimeMillis())
        einstellungen.offeneSitzung = id
    }

    suspend fun benenneSitzungUm(id: Long, titel: String) {
        // Von Hand vergeben — ab jetzt fasst die KI den Titel nicht mehr an (F-12, Regeln).
        db.sitzungen().setzeTitel(id, titel.trim().take(80), vonHand = true)
        merkeAenderung(id)
    }

    /**
     * Hält fest, dass sich an dieser Sitzung wirklich etwas geändert hat — davon hängt ihr
     * Platz in der Seitenleiste ab. Blosses Öffnen ruft das bewusst nicht auf.
     */
    private suspend fun merkeAenderung(sitzungId: Long) =
        db.sitzungen().merkeAenderung(sitzungId, System.currentTimeMillis())

    /**
     * Löscht die Sitzung samt Notizen und Antworten. War es die letzte, entsteht sofort eine
     * neue — die App steht nie ohne Sitzung da.
     */
    suspend fun loescheSitzung(sitzung: Sitzung): Sitzung {
        db.sitzungen().loeschen(sitzung)
        if (db.sitzungen().anzahl() == 0) return neueSitzung()
        val naechste = db.sitzungen().zuletztGeoeffnete()!!
        einstellungen.offeneSitzung = naechste.id
        return naechste
    }

    // --- Favoriten, Schutz, Papierkorb und Ordner ------------------------------------------

    suspend fun favoritUmschalten(id: Long) = db.sitzungen().favoritUmschalten(id)

    suspend fun setzeSchutz(id: Long, geschuetzt: Boolean) = db.sitzungen().setzeSchutz(id, geschuetzt)

    /** Verschiebt eine Sitzung in den Papierkorb bzw. holt sie wieder heraus. */
    suspend fun setzePapierkorb(id: Long, drin: Boolean) =
        db.sitzungen().setzePapierkorb(id, if (drin) System.currentTimeMillis() else null)

    suspend fun leerePapierkorb() = db.sitzungen().leerePapierkorb()

    suspend fun verschiebeInOrdner(id: Long, ordnerId: Long?) = db.sitzungen().setzeOrdner(id, ordnerId)

    suspend fun legeOrdnerAn(name: String): Long =
        db.ordner().einfuegen(Ordner(name = name.trim().take(60).ifBlank { "Neuer Ordner" }, erstelltAm = System.currentTimeMillis()))

    suspend fun benenneOrdnerUm(ordner: Ordner, name: String) =
        db.ordner().aendern(ordner.copy(name = name.trim().take(60).ifBlank { ordner.name }))

    /** Löscht den Ordner; die Sitzungen darin bleiben erhalten und liegen danach ausserhalb. */
    suspend fun loescheOrdner(id: Long) {
        db.ordner().loeseSitzungen(id)
        db.ordner().loeschen(id)
    }

    /** Die nächste sichtbare Sitzung, wenn die offene weggeräumt oder geschützt wurde. */
    suspend fun naechsteSichtbare(): Sitzung {
        db.sitzungen().zuletztGeoeffnete()?.let {
            einstellungen.offeneSitzung = it.id
            return it
        }
        return neueSitzung()
    }

    fun notizzahl(sitzungId: Long): Flow<Int> = db.sitzungen().notizzahl(sitzungId)

    fun letzteNotizzeit(sitzungId: Long): Flow<Long?> = db.sitzungen().letzteNotizzeit(sitzungId)

    // --- Verlauf (B-01) --------------------------------------------------------------------

    /**
     * Notizen und KI-Antworten als **eine** Liste, nach Zeit sortiert.
     *
     * Die Trennung in zwei Tabellen darf man dem Verlauf nicht ansehen — deshalb werden sie
     * hier zusammengeführt und nicht erst in der Oberfläche, wo zwei Listen unweigerlich
     * irgendwann verschieden sortiert würden.
     */
    fun verlauf(sitzungId: Long): Flow<List<Verlaufseintrag>> =
        combine(
            db.notizen().ausSitzung(sitzungId),
            db.antworten().ausSitzung(sitzungId),
        ) { notizen, antworten ->
            (notizen.map(Verlaufseintrag::NotizEintrag) + antworten.map(Verlaufseintrag::AntwortEintrag))
                .sortedBy { it.zeit }
        }

    /** Der Verlauf als einmalige Momentaufnahme — für den Export (F-16). */
    suspend fun verlaufEinmal(sitzungId: Long): List<Verlaufseintrag> {
        val notizen = db.notizen().alleFertigen(sitzungId).map(Verlaufseintrag::NotizEintrag)
        val antworten = db.antworten().alleEinmal(sitzungId).map(Verlaufseintrag::AntwortEintrag)
        return (notizen + antworten).sortedBy { it.zeit }
    }

    // --- Notizen anlegen (F-01, F-02, F-04) ------------------------------------------------

    suspend fun legeGetippteNotizAn(
        sitzungId: Long,
        text: String,
        anhaenge: List<Anhang> = emptyList(),
    ): Long =
        db.notizen().einfuegen(
            Notiz(
                sitzungId = sitzungId,
                erstelltAm = System.currentTimeMillis(),
                text = text.trim(),
                quelle = Notizquelle.GETIPPT,
                zustand = Notizzustand.FERTIG,
                anhaengeJson = anhaenge.alsJson(),
            ),
        ).also { merkeAenderung(sitzungId) }

    /** Die Karte entsteht sofort nach dem Aufnahmeende — noch ohne Text (F-01, Schritt 4). */
    suspend fun legeGesprocheneNotizAn(sitzungId: Long, zustand: Notizzustand, audioPfad: String?): Long =
        db.notizen().einfuegen(
            Notiz(
                sitzungId = sitzungId,
                erstelltAm = System.currentTimeMillis(),
                quelle = Notizquelle.GESPROCHEN,
                zustand = zustand,
                audioPfad = audioPfad,
            ),
        ).also { merkeAenderung(sitzungId) }

    suspend fun notiz(id: Long): Notiz? = db.notizen().eine(id)

    suspend fun aendere(notiz: Notiz) {
        db.notizen().aendern(notiz)
        merkeAenderung(notiz.sitzungId)
    }

    suspend fun loescheNotiz(notiz: Notiz) {
        notiz.audioPfad?.let { runCatching { File(it).delete() } }
        // Mit der Notiz verschwinden auch ihre Anhangsdateien — sonst bleiben Bilder,
        // Aufnahmen und PDFs als Karteileichen im App-Speicher liegen.
        Anhangsspeicher(ctx).loesche(anhaengeAusJson(notiz.anhaengeJson))
        db.notizen().loeschen(notiz)
        merkeAenderung(notiz.sitzungId)
    }

    /** Verschieben lässt den Zeitstempel unangetastet (F-08) — die Notiz bleibt, wann sie war. */
    suspend fun verschiebeNotiz(notiz: Notiz, zielSitzung: Long) {
        db.notizen().aendern(notiz.copy(sitzungId = zielSitzung))
        // Beide Sitzungen haben sich geändert: der einen fehlt die Notiz, die andere hat
        // sie dazubekommen.
        merkeAenderung(notiz.sitzungId)
        merkeAenderung(zielSitzung)
    }

    suspend fun bearbeiteNotiz(notiz: Notiz, ueberschrift: String, text: String) {
        merkeAenderung(notiz.sitzungId)
        db.notizen().aendern(
            notiz.copy(
                text = text.trim(),
                ueberschrift = ueberschrift.trim().takeIf(String::isNotBlank),
                // Wer die Überschrift anfasst, hat sie ab jetzt selbst in der Hand.
                ueberschriftVonHand = ueberschrift.trim() != notiz.ueberschrift?.trim(),
            ),
        )
    }

    suspend fun angefangeneAufraeumen() {
        // Eine Notiz im Zustand AUFNEHMEND hat den App-Start nicht überlebt: die Aufnahme ist
        // weg, die Karte wäre eine leere Hülle. A-13 verlangt, dass keine solche stehenbleibt.
        db.notizen().angefangene().forEach { it.audioPfad?.let { pfad -> runCatching { File(pfad).delete() } } }
        db.notizen().raeumeAngefangeneWeg()
    }

    // --- Transkription (F-03, F-04) --------------------------------------------------------

    fun transkriber(): GroqTranscriber = GroqTranscriber(einstellungen.groqSchluessel)

    suspend fun wartendeNotizen(): List<Notiz> = db.notizen().wartende(HOECHSTVERSUCHE)

    suspend fun notizenOhneUeberschrift(): List<Notiz> = db.notizen().ohneUeberschrift()

    suspend fun notizenOhneSchluessel(): List<Notiz> = db.notizen().ohneSchluessel()

    // --- KI (F-05, F-07, F-09, F-12) --------------------------------------------------------

    private fun modell(): CodexModel = CodexModel.fromLabel(einstellungen.codexModell)

    private fun effort(): ReasoningEffort = ReasoningEffort.fromLabel(einstellungen.codexEffort)

    suspend fun holeUeberschrift(text: String): String =
        codex.ueberschriftFuer(text, modell(), effort())

    suspend fun holeSitzungstitel(ersteNotiz: String): String =
        codex.sitzungstitelFuer(ersteNotiz, modell(), effort())

    suspend fun verbessere(text: String): String =
        codex.verbessereText(text, modell(), effort())

    suspend fun holeRueckfrage(notizen: String): String =
        codex.stelleRueckfrage(notizen, modell(), effort())

    suspend fun holeAuswertung(
        notizen: String,
        rueckfrage: String,
        antwort: String,
        profilAnweisung: String,
        websuche: Boolean,
    ): String = codex.werteAus(notizen, rueckfrage, antwort, profilAnweisung, websuche, modell(), effort())

    /**
     * Setzt den Sitzungstitel aus der ersten Notiz — aber nur, wenn er noch der
     * Auslieferungstitel ist. Ein von Hand vergebener bleibt stehen (F-12).
     */
    suspend fun setzeTitelWennNochKeiner(sitzungId: Long, ersteNotiz: String) {
        val sitzung = db.sitzungen().eine(sitzungId) ?: return
        if (sitzung.titelVonHand || sitzung.titel != "Neue Sitzung") return
        val titel = holeSitzungstitel(ersteNotiz).takeIf(String::isNotBlank) ?: return
        db.sitzungen().setzeTitel(sitzungId, titel, vonHand = false)
    }

    // --- Der Auswertungs-Kontext (F-09, Schritt 1) ------------------------------------------

    /** Die vollständige Sitzung in ihrer wirklichen Reihenfolge. */
    suspend fun kontextEintraege(sitzungId: Long): List<Verlaufseintrag> = verlaufEinmal(sitzungId)

    /** Der vollständige Sitzungskontext als Text, so wie ihn Codex bekommt. */
    fun alsKontext(eintraege: List<Verlaufseintrag>): String = eintraege.joinToString("\n\n") { eintrag ->
        when (eintrag) {
            is Verlaufseintrag.NotizEintrag -> {
                val notiz = eintrag.notiz
                val kopf = notiz.ueberschrift?.takeIf(String::isNotBlank)
                    ?.let { "$it (${zeitpunkt(notiz.erstelltAm)})" }
                    ?: zeitpunkt(notiz.erstelltAm)
                "[Notiz: $kopf]\n${notiz.text}"
            }

            is Verlaufseintrag.AntwortEintrag -> {
                val antwort = eintrag.antwort
                buildString {
                    append("[KI-Auswertung: ").append(zeitpunkt(antwort.erstelltAm)).append("]\n")
                    append("Rückfrage: ").append(antwort.rueckfrage).append('\n')
                    append("Antwort des Nutzers: ").append(antwort.antwortDesNutzers).append('\n')
                    append("KI-Antwort: ").append(antwort.text)
                }
            }
        }
    }

    suspend fun speichereAntwort(antwort: KiAntwort): Long =
        db.antworten().einfuegen(antwort).also { merkeAenderung(antwort.sitzungId) }

    suspend fun loescheAntwort(antwort: KiAntwort) {
        db.antworten().loeschen(antwort)
        merkeAenderung(antwort.sitzungId)
    }

    // --- Profile (F-10) ----------------------------------------------------------------------

    val profile: Flow<List<Auswertungsprofil>> = db.profile().alle()

    val aktivesProfil: Flow<Auswertungsprofil?> = db.profile().aktivesLaufend()

    suspend fun holeAktivesProfil(): Auswertungsprofil? = db.profile().aktives()

    /**
     * Setzt das Häkchen. Ein Profil ohne Anweisungstext lässt sich nicht aktivieren —
     * sonst liefe die Auswertung ohne jede Vorgabe (F-10, Fehlerfall).
     *
     * @return true, wenn das Häkchen wirklich umgesprungen ist
     */
    suspend fun aktiviereProfil(profil: Auswertungsprofil): Boolean {
        if (profil.anweisung.isBlank()) return false
        db.profile().setzeAktiv(profil.nummer)
        return true
    }

    suspend fun speichereProfil(profil: Auswertungsprofil) = db.profile().aendern(profil)

    /**
     * Stellt den Auslieferungstext wieder her — behält aber, ob dieses Profil gerade aktiv ist.
     * Sonst stünde die App nach einem Zurücksetzen ohne aktives Profil da.
     */
    suspend fun setzeProfilZurueck(nummer: Int, warAktiv: Boolean) {
        val vorlage = Auslieferungsprofile.vorlage(nummer)
        db.profile().aendern(vorlage.copy(istAktiv = warAktiv && vorlage.anweisung.isNotBlank()))
        if (warAktiv && vorlage.anweisung.isBlank()) {
            // Ein leeres Profil kann nicht aktiv bleiben: das Häkchen wandert zurück auf „Normal".
            db.profile().setzeAktiv(2)
        }
    }

    suspend fun legeProfileAnWennNoetig() {
        if (db.profile().anzahl() >= Auslieferungsprofile.ANZAHL) return
        db.profile().einfuegenAlle(Auslieferungsprofile.texte)
    }

    // --- Suche (F-14) --------------------------------------------------------------------------

    suspend fun suche(begriff: String): List<Suchtreffer> {
        val gesucht = begriff.trim().lowercase(Locale.GERMAN)
        if (gesucht.length < 2) return emptyList()
        return (db.suche().inNotizen(gesucht) + db.suche().inAntworten(gesucht))
            .sortedByDescending { it.erstelltAm }
    }

    // --- Export (F-16) -------------------------------------------------------------------------

    /** Die ganze Sitzung als Markdown — Notizen und Antworten in ihrer wirklichen Reihenfolge. */
    suspend fun alsMarkdown(sitzung: Sitzung, eintraege: List<Verlaufseintrag>): String = buildString {
        append("# ").append(sitzung.titel).append("\n\n")
        append("_Gedankenspeicher · ").append(zeitpunkt(sitzung.erstelltAm)).append("_\n\n")
        eintraege.forEach { eintrag ->
            when (eintrag) {
                is Verlaufseintrag.NotizEintrag -> {
                    val n = eintrag.notiz
                    append("## ").append(n.ueberschrift ?: zeitpunkt(n.erstelltAm)).append("\n")
                    append("_").append(zeitpunkt(n.erstelltAm)).append("_\n\n")
                    append(n.text).append("\n\n")
                }
                is Verlaufseintrag.AntwortEintrag -> {
                    val a = eintrag.antwort
                    append("---\n\n### Auswertung · ").append(zeitpunkt(a.erstelltAm)).append("\n\n")
                    append("**Rückfrage:** ").append(a.rueckfrage).append("\n\n")
                    append("**Antwort darauf:** ").append(a.antwortDesNutzers).append("\n\n")
                    append(a.text).append("\n\n")
                    append("_Profil: ").append(a.profilName)
                    append(" · Modell: ").append(a.modell)
                    append(" · Effort: ").append(a.effort)
                    append(" · Websuche: ").append(if (a.websucheAn) "an" else "aus").append("_\n\n")
                }
            }
        }
    }

    fun exportdatei(sitzung: Sitzung, inhalt: String): File {
        val ordner = File(ctx.cacheDir, "export").apply { mkdirs() }
        val name = sitzung.titel.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifBlank { "Sitzung" }
        val datum = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Date())
        return File(ordner, "$name-$datum.md").apply { writeText(inhalt) }
    }

    // --- Sicherung (F-17) -----------------------------------------------------------------------

    /** Die Datei, die nach Drive geht. Room hält daneben WAL-Dateien; die kommen mit. */
    fun datenbankdatei(): File = ctx.getDatabasePath(Datenbank.DATEINAME)

    companion object {
        /** Nach drei Fehlversuchen wird nicht mehr von selbst nachgereicht (F-04, Fehlerfall). */
        const val HOECHSTVERSUCHE = 3

        private val zeitformat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMAN)

        fun zeitpunkt(ms: Long): String = zeitformat.format(Date(ms))

        private val uhrzeitformat = SimpleDateFormat("HH:mm", Locale.GERMAN)

        /** Der Platzhalter, der steht, bis die KI-Überschrift da ist (F-05, Schritt 1). */
        fun uhrzeit(ms: Long): String = uhrzeitformat.format(Date(ms))
    }
}
