package de.frank.gedankenspeicher.ui.verlauf

import android.util.LruCache

/**
 * **Die Zwischenspeicher des Verlaufs.**
 *
 * Eine `LazyColumn` wirft alles weg, was aus dem Bild scrollt, und baut es beim
 * Zurückscrollen von Grund auf neu. Für eine Karte heisst das: der Text wird wieder in
 * Absätze zerlegt, das Markdown wieder geparst, der Anhangs-JSON wieder gelesen, das Bild
 * wieder von der Platte dekodiert. Bei einer Sitzung mit Bildern, PDFs und langen
 * Auswertungen sind das je Karte zweistellige Millisekunden — und damit ausgelassene Bilder.
 *
 * Alle diese Schritte sind reine Funktionen über unveränderlichem Text. Sie einmal
 * auszurechnen und das Ergebnis zu behalten ändert nichts am Ergebnis, spart aber genau die
 * Arbeit, die beim Scrollen anfällt.
 *
 * Die Grössen sind bewusst klein: es geht nicht darum, eine ganze Sitzung vorzuhalten,
 * sondern das Fenster um die sichtbaren Karten herum.
 */
internal object Zwischenspeicher {

    /**
     * Ein Speicher für Ergebnisse, die aus einem Textschlüssel entstehen.
     *
     * `LruCache` wirft von selbst das Älteste weg, sobald die Zahl der Einträge überschritten
     * ist — es kann also nichts unbegrenzt anwachsen.
     */
    class Textspeicher<T : Any>(hoechstzahl: Int) {
        private val speicher = object : LruCache<String, T>(hoechstzahl) {}

        fun hole(schluessel: String, rechne: (String) -> T): T =
            speicher.get(schluessel) ?: rechne(schluessel).also { speicher.put(schluessel, it) }

        fun leere() = speicher.evictAll()
    }

    /** Absätze eines Notiztextes (`Absaetze.teile`). */
    val absaetze = Textspeicher<List<String>>(96)

    /** Abschnitte mit ihren Nachträgen (`Nachtraege.abschnitte`). */
    val abschnitte = Textspeicher<List<de.frank.gedankenspeicher.data.Textabschnitt>>(64)

    /** Die Anhänge einer Notiz aus ihrem JSON-Feld. */
    val anhaenge = Textspeicher<List<de.frank.gedankenspeicher.data.Anhang>>(64)

    /** Die Bausteine einer KI-Auswertung (`Reichtext.zerlege`). */
    val bausteine = Textspeicher<List<Baustein>>(48)

    /**
     * Von welcher Vorlese-Absatznummer an jeder Baustein spricht.
     *
     * Das auszurechnen heisst, jeden Baustein noch einmal in Vorlese-Absätze zu zerlegen —
     * bei einer langen Auswertung der teuerste Einzelschritt am Aufbau der Karte.
     */
    val grenzen = Textspeicher<List<IntRange>>(48)

    /** Formatierte Zeitstempel — dieselbe Millisekunde ergibt immer denselben Text. */
    val zeitstempel = object : LruCache<Long, String>(256) {}

    /** Wird beim Verlassen des Verlaufs geleert, damit nichts unnötig im Speicher bleibt. */
    fun leereTexte() {
        absaetze.leere()
        abschnitte.leere()
        anhaenge.leere()
        bausteine.leere()
        zeitstempel.evictAll()
    }
}
