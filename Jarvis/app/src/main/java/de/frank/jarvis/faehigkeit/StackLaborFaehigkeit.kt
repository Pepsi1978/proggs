package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONObject

/**
 * Anbindung an „Stack-Labor“ (de.frank.stacklabor.werftstudio) über dessen Jarvis-Brücke (Signatur-Erlaubnis): Franks
 * Nahrungsergänzungs-Stacks lesen und bearbeiten. Die Fachlogik (Namen auflösen, Dosis prüfen, Doppel-Schutz, Sperre,
 * Bestätigung beim Löschen, Nachlesen nach jeder Änderung) liegt in der App (`bruecke/JarvisStackDienst.kt`); die Brücke
 * liefert fertige Texte. Lesende und schreibende Werkzeuge sind getrennt, Löschen hat ein eigenes Werkzeug.
 */
class StackLaborFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "stacklabor"
    override val name = "Stack-Labor"
    override val beschreibung = "Stacks mit Nahrungsergänzungsmitteln lesen und vorlesen, Mittel und Stacks anlegen und ändern, Mittel aktivieren oder deaktivieren."
    override val hinweise =
        "Stack-Labor ist Franks App für seine Nahrungsergänzungs-Stacks. Ein Stack ist eine geplante Einnahme zu einem Zeitpunkt (etwa „Nach Aufstehen 1“, „Sport 2“, " +
            "„Vor dem Schlafen 1“) mit Mitteln in fester Reihenfolge, Dosis (Menge, Einheit, Kapselanzahl) und Haken (aktiv oder inaktiv). Regeln: " +
            "Franks tatsächliche Stacks und Einnahmeplanung stehen in Stack-Labor; die App Epigenetik hat einen eigenen Recherche-Stack. Bei „meine Stacks“, " +
            "„Sport 2“ oder „was nehme ich nach dem Aufstehen“ gilt Stack-Labor. " +
            "(1) Nur aktive Mittel gehören zu Franks geplanter Einnahmeroutine; inaktive nennst du beim vollständigen Vorlesen mit, klar als ausgeschaltet. " +
            "(2) Ein geplanter oder aktiver Stack belegt KEINE Einnahme: Sag nie, Frank habe etwas genommen, wenn er es nicht ausdrücklich gesagt hat. " +
            "(3) Nach-Aufstehen-Stacks gehören nach dem Aufstehen, Sport-Stacks zum Sport, Vor-dem-Schlafen-Stacks vor das Schlafen; bei gleichem Namen mit Nummer " +
            "kommt 1 vor 2 vor 3. Zeitpunkt und Einnahmehinweis jedes Stacks sind verbindlich und gehen vor. " +
            "(4) Dosis und Kapselanzahl nur übernehmen, wenn Frank sie ausdrücklich nennt, nie medizinisch ableiten oder ergänzen; fehlt eine nötige Angabe, nachfragen. " +
            "Gramm und Milligramm genau so übergeben, wie Frank sie sagt. (5) Meldet das Werkzeug mehrere passende Stacks oder Mittel, nachfragen statt raten. " +
            "(6) Löschen nur nach Franks ausdrücklichem Ja (bestaetigt erst danach setzen). (7) Eine Änderung erst bestätigen, wenn das Werkzeug „Gespeichert und nachgelesen“ " +
            "meldet; Fehler oder eine nicht erreichbare App offen sagen. (8) Ein gesperrter Stack lässt sich nur in der App entsperren. Beim Vorlesen keine ids nennen."

    override fun stoerung(): String? = rufeDirekt("status", JSONObject()).optString("fehler").takeIf { it.isNotEmpty() }

    /** Alle Stacks vollständig, fürs Live-Gedächtnis (Zeilen mit `[id …]`, damit Änderungen am selben Eintrag erkannt werden). */
    suspend fun gedaechtnis(): String = rufe("stacks", JSONObject().put("voll", true)).let { a ->
        a.optString("fehler").takeIf { it.isNotEmpty() }?.let { "NICHT VERFÜGBAR: $it" } ?: a.optString("text")
    }

    /** Nur die lesenden Werkzeuge: für Agenten und die Tagesauswertung, die Franks Einnahmeplan nie ändern sollen. */
    fun nurLesend(): Faehigkeit = object : Faehigkeit by this@StackLaborFaehigkeit {
        override val id = "stacklabor-lesen"
        override val werkzeuge = this@StackLaborFaehigkeit.werkzeuge.filter { it.nurLesen }
        override val hinweise = this@StackLaborFaehigkeit.hinweise + " Du darfst Stack-Labor hier nur lesen."
    }

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = "stacks_lesen",
                titel = "Stacks lesen",
                beschreibung = "Jarvis: liest Franks Stacks aus Stack-Labor frisch aus der App. Ohne Angabe: alle Stacks im Überblick (Reihenfolge, Zeitpunkt, Einnahmehinweis, " +
                    "Zahl der aktiven Mittel). Mit stack: ein Stack vollständig mit Beschreibung, Zielen, allen Mitteln in Einnahme-Reihenfolge, Dosis, Einheit, Kapselanzahl " +
                    "(soweit hinterlegt), Frequenz, Notizen und aktiv/INAKTIV. Mit vollstaendig: alle Stacks mit allen Mitteln. Mit mittel: ein Mittel aus dem Katalog mit " +
                    "allen Stacks, in denen es steht; mit katalog: alle Mittel. Nutze es für „Was ist in Sport 2?“, „Lies mir meine Stacks vor“, „Wo nehme ich Magnesium?“. " +
                    "Stack-Namen darf Frank frei sprechen („Sportmodus 2“, „zweiter Sport-Stack“); die App löst sie auf oder meldet Mehrdeutigkeit.",
                schema = schema(
                    "stack" to text("Name des Stacks, so wie Frank ihn sagt. Dann nur dieser Stack, vollständig."),
                    "mittel" to text("Name eines Mittels: dessen Angaben und alle Stacks, in denen es steht."),
                    "vollstaendig" to schalter("Alle Stacks mit allen Mitteln vollständig."),
                    "katalog" to schalter("Alle Mittel des Katalogs mit ihren Stacks."),
                    "ziele" to schalter("Den Ziel-Katalog mit Rängen je Stack."),
                    "auswertung" to schalter("Nur mit stack: zusätzlich die letzte in Stack-Labor gespeicherte Auswertung."),
                ),
                nurLesen = true,
            ) { a -> leseAufruf(a).let { (methode, anfrage) -> alsErgebnis(rufe(methode, anfrage)) } },
            Werkzeug(
                name = "stack_mittel_setzen",
                titel = "Mittel in einem Stack setzen",
                beschreibung = "Jarvis: fügt in Stack-Labor ein Mittel zu einem Stack hinzu, ändert Dosis, Frequenz, Notiz oder Position, schaltet es aktiv oder inaktiv, " +
                    "oder kopiert/verschiebt es in einen anderen Stack. Beispiel „Füge Ashwagandha in Sport 2 hinzu, 2 Gramm, zwei Kapseln“: stack „Sport 2“, mittel „Ashwagandha“, " +
                    "aktion hinzufuegen, menge „2“, einheit „g“, stueckzahl „2“. „Füge Glycin in Sport 2 hinzu, 6 Gramm“: menge „6“, einheit „g“. menge ist die Gesamtmenge je Einnahme; " +
                    "die App teilt sie selbst auf die Kapseln auf. Nur ausdrücklich genannte Angaben übergeben, nichts ergänzen. Fehlt die Dosis, frag Frank. Steht ein Mittel noch " +
                    "nicht im Katalog, meldet die App das mit ähnlichen Namen: Frag Frank und setze neues_mittel erst nach seiner Antwort. Ein wiederholter Aufruf legt nichts doppelt an. " +
                    "Zum Entfernen gibt es stacklabor_loeschen.",
                schema = schema(
                    "stack" to text("Name des Stacks, wie Frank ihn sagt."),
                    "mittel" to text("Name des Mittels, wie Frank ihn sagt."),
                    "aktion" to text("hinzufuegen (Vorgabe), aendern, aktivieren, deaktivieren, kopieren oder verschieben.", listOf("hinzufuegen", "aendern", "aktivieren", "deaktivieren", "kopieren", "verschieben")),
                    "menge" to text("Gesamtmenge je Einnahme als Zahl, z. B. „2“ oder „1,5“. Nur wenn Frank sie nennt."),
                    "einheit" to text("Einheit zur Menge, genau wie gesagt: g für Gramm, mg für Milligramm, µg für Mikrogramm, ml, IE, Stück, Tasse.", listOf("g", "mg", "µg", "ml", "IE", "Stück", "Tasse")),
                    "stueckzahl" to text("Anzahl Kapseln, Tabletten oder Stück je Einnahme, nur wenn Frank sie nennt."),
                    "menge_je_stueck" to text("Nur wenn Frank ausdrücklich „je Kapsel“ oder „pro Tablette“ sagt: Menge je Stück (statt menge)."),
                    "menge_je_stueck_beibehalten" to schalter("Nur bei aendern mit neuer Stückzahl: die bisherige Menge je Stück bleibt (Frank hat das bestätigt)."),
                    "variante" to text("Dosis für freie Tage (frei, Vorgabe), für Diensttage (dienst) oder beide.", listOf("frei", "dienst", "beide")),
                    "frequenz" to text("Nur wenn Frank es sagt: „täglich“ oder „alle N Tage“."),
                    "notiz" to text("Notiz zum Eintrag, nur wenn Frank sie diktiert; „leer“ löscht sie."),
                    "position" to zahl("Position im Stack (1 = zuerst), nur wenn Frank die Reihenfolge nennt."),
                    "ziel_stack" to text("Nur bei kopieren oder verschieben: der andere Stack."),
                    "neues_mittel" to schalter("Das Mittel neu im Katalog anlegen; erst nachdem Frank das bestätigt hat."),
                    "trotzdem_neu" to schalter("Neu anlegen, obwohl ein sehr ähnlicher Name existiert; nur nach Franks Bestätigung."),
                    "darreichungsform" to text("Nur beim Neuanlegen und nur wenn bekannt: Kapsel, Tablette, Pulver, Tropfen, Löffel, Tasse oder eigene Angabe."),
                    "ohne_dosis" to schalter("Nur wenn Frank ausdrücklich ohne Dosis eintragen will."),
                    pflicht = listOf("stack", "mittel"),
                ),
                nurLesen = false,
            ) { a -> schreibe("eintrag", a) },
            Werkzeug(
                name = "stack_speichern",
                titel = "Stack anlegen oder ändern",
                beschreibung = "Jarvis: legt in Stack-Labor einen neuen Stack an (neu: true) oder ändert einen vorhandenen: Name, Zeitpunkt, Einnahmehinweis (die verbindliche " +
                    "Beschreibung), Position unter den Stacks, Reihenfolge der Mittel, ein Ziel oder eine eigene Frage hinzufügen. Nur genannte Felder übergeben; leere Felder " +
                    "ändern nichts. Ein gleichnamiger Stack wird nicht doppelt angelegt.",
                schema = schema(
                    "stack" to text("Name des Stacks (beim Anlegen der neue Name)."),
                    "neu" to schalter("Einen neuen Stack anlegen."),
                    "trotzdem_neu" to schalter("Neu anlegen, obwohl ein sehr ähnlicher Name existiert; nur nach Franks Bestätigung."),
                    "neuer_name" to text("Neuer Name für einen vorhandenen Stack."),
                    "zeitpunkt" to text("Wann der Stack genommen wird, z. B. „45 Minuten vor dem Sport“ (höchstens 120 Zeichen); „leer“ löscht."),
                    "einnahme_hinweis" to text("Wie er genommen wird, z. B. „mit Olivenöl und Wasser“ (höchstens 120 Zeichen); „leer“ löscht."),
                    "position" to zahl("Position unter allen Stacks (1 = erster)."),
                    "mittel_reihenfolge" to textListe("Neue Einnahme-Reihenfolge: Mittelnamen von zuerst bis zuletzt; nicht genannte bleiben dahinter in bisheriger Folge."),
                    "ziel_hinzufuegen" to text("Ein Ziel für diesen Stack (wird im Ziel-Katalog angelegt, wenn neu)."),
                    "ziel_rang" to zahl("Rang dieses Ziels (1 = wichtigstes)."),
                    "frage_hinzufuegen" to text("Eine eigene Frage für die Auswertung dieses Stacks."),
                    pflicht = listOf("stack"),
                ),
                nurLesen = false,
            ) { a -> schreibe("stack_speichern", a) },
            Werkzeug(
                name = "mittel_speichern",
                titel = "Mittel im Katalog anlegen oder ändern",
                beschreibung = "Jarvis: legt ein Nahrungsergänzungsmittel im Katalog von Stack-Labor an (neu: true) oder ändert dessen Stammdaten (Name, Darreichungsform, " +
                    "Löslichkeit, Hersteller, Beistoffe, Durchfallrisiko). Dosis gehört nicht hierher, sondern zu stack_mittel_setzen. Gleicher Name = kein Doppel; " +
                    "bei sehr ähnlichem Namen fragt die App nach. Ohne Löslichkeit bestimmt Stack-Labor sie mit seiner KI.",
                schema = schema(
                    "mittel" to text("Name des Mittels (beim Anlegen der neue Name)."),
                    "neu" to schalter("Neu im Katalog anlegen."),
                    "trotzdem_neu" to schalter("Neu anlegen, obwohl ein sehr ähnlicher Name existiert; nur nach Franks Bestätigung."),
                    "neuer_name" to text("Neuer Name für ein vorhandenes Mittel."),
                    "darreichungsform" to text("Kapsel, Tablette, Pulver, Tropfen, Löffel, Tasse oder eigene Angabe."),
                    "loeslichkeit" to text("wasser, fett oder beides.", listOf("wasser", "fett", "beides")),
                    "hersteller" to text("Hersteller; „leer“ löscht."),
                    "beistoffe" to text("Beistoffe; „leer“ löscht."),
                    "durchfallrisiko" to schalter("Durchfallrisiko ja oder nein, nur wenn Frank es sagt."),
                    pflicht = listOf("mittel"),
                ),
                nurLesen = false,
            ) { a -> schreibe("mittel_speichern", a) },
            Werkzeug(
                name = "stacklabor_loeschen",
                titel = "In Stack-Labor löschen",
                beschreibung = "Jarvis: entfernt in Stack-Labor ein Mittel aus einem Stack, löscht einen Stack, ein Mittel aus dem Katalog, ein Ziel aus einem Stack oder eine " +
                    "eigene Frage, oder führt zwei doppelte Mittel zusammen. Erst ohne bestaetigt aufrufen: Die App nennt, was verloren ginge. Lies das Frank vor und rufe erst " +
                    "nach seinem ausdrücklichen Ja mit bestaetigt: true auf. Ist unklar, was gelöscht werden soll, frag nach. Zum bloßen Ausschalten nimm stack_mittel_setzen mit deaktivieren.",
                schema = schema(
                    "was" to text("Was gelöscht wird.", listOf("mittel_aus_stack", "stack", "mittel_aus_katalog", "mittel_zusammenfuehren", "ziel_aus_stack", "frage")),
                    "stack" to text("Der Stack (bei mittel_aus_stack, stack, ziel_aus_stack, frage)."),
                    "mittel" to text("Das Mittel (bei mittel_aus_stack, mittel_aus_katalog; bei mittel_zusammenfuehren das Mittel, das verschwindet)."),
                    "behalten" to text("Nur bei mittel_zusammenfuehren: das Mittel, das bleibt."),
                    "ziel" to text("Nur bei ziel_aus_stack: das Ziel."),
                    "frage" to text("Nur bei frage: Wortlaut oder Teil der Frage."),
                    "bestaetigt" to schalter("Erst true, nachdem Frank das Löschen ausdrücklich bestätigt hat."),
                    pflicht = listOf("was"),
                ),
                nurLesen = false,
                loeschend = true,
            ) { a -> schreibe("loeschen", a) },
        )
    }

    private suspend fun schreibe(methode: String, a: JSONObject): Ergebnis {
        val antwort = rufe(methode, a)
        // Zeitlimit: Die Änderung kam womöglich trotzdem an. Die Brücke legt nichts doppelt an, trotzdem erst nachlesen.
        if (antwort.optBoolean(AppBruecke.ZEITLIMIT)) return Ergebnis(antwort.optString("fehler") + " Vor einem neuen Versuch mit stacks_lesen nachlesen.", fehler = true)
        return alsErgebnis(antwort)
    }

    /** Unterbrechbar: Ein Abbruch des Aufrufers beendet das Warten auf die Brücke sofort (siehe [AppBruecke]). */
    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject = runInterruptible(Dispatchers.IO) { rufeDirekt(methode, anfrage) }

    private fun rufeDirekt(methode: String, anfrage: JSONObject): JSONObject =
        AppBruecke.rufe(context, "Stack-Labor", BRUECKE, methode, anfrage, lesend = lesend(methode))

    companion object {
        private val BRUECKE: Uri by lazy { Uri.parse("content://de.frank.stacklabor.werftstudio.jarvis") }
        private val LESEND = setOf("status", "stacks", "stack", "mittel", "ziele")

        /** Welche lesende Methode der Brücke `stacks_lesen` aufruft, und mit welcher Anfrage. */
        internal fun leseAufruf(a: JSONObject): Pair<String, JSONObject> = when {
            a.gesetzt("mittel") -> "mittel" to JSONObject().put("mittel", a.optString("mittel"))
            a.gesetzt("stack") -> "stack" to JSONObject().put("stack", a.optString("stack")).put("auswertung", a.optBoolean("auswertung", false))
            a.optBoolean("katalog", false) -> "mittel" to JSONObject()
            a.optBoolean("ziele", false) -> "ziele" to JSONObject()
            else -> "stacks" to JSONObject().put("voll", a.optBoolean("vollstaendig", false))
        }

        /** Antwort der Brücke als Werkzeug-Ergebnis: ein Fehler oder eine Rückfrage zählt nie als ausgeführte Änderung. */
        internal fun alsErgebnis(antwort: JSONObject): Ergebnis =
            antwort.optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) }
                ?: Ergebnis(antwort.optString("text").ifEmpty { "Stack-Labor hat leer geantwortet." })

        /** Methoden der Brücke, die nur lesen (kurzes Zeitlimit, ein zweiter Versuch). */
        internal fun lesend(methode: String): Boolean = methode in LESEND
    }
}
