package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.Werkzeug
import org.json.JSONObject

/**
 * Die Bibliothek knapper Tätigkeitsbeschreibungen: was Jarvis gerade wirklich tut, für Chat, Sprachblase und Sperrbildschirm.
 * Nur belegbare Zustände: Jede Zeile gehört zu einem echten Werkzeugaufruf oder zum Warten auf das Modell, nie zu erfundenen
 * Zwischenschritten oder Gedankengängen. Die Werkzeug-Sätze stehen ohne Subjekt und klein („schaut in die Aufgaben“), damit sie
 * auch unter einem Agenten („Recherche arbeitet“) passen; [satz] setzt „Jarvis“ davor.
 */
object Taetigkeit {
    /** Die Eingabe ist angekommen, das Modell arbeitet (vor dem ersten Werkzeug und zwischen den Schritten). */
    const val DENKT = "Jarvis denkt"
    /** Aus dem Plus-Menü „Programmierer“ abgeschickt, das Modell bereitet die Übergabe vor. */
    const val PROGRAMMIERAUFTRAG = "Jarvis bereitet den Programmierauftrag vor"
    /** Das Tokenlimit-Fenster wartet auf Franks Entscheidung. */
    const val WARTET_AUF_DICH = "Jarvis wartet auf deine Entscheidung"

    /** „Jarvis“ + Werkzeug-Satz; leer = [DENKT]. */
    fun satz(schritt: String): String = if (schritt.isBlank()) DENKT else "Jarvis $schritt"

    /** Wie eine Runde endete. [FEHLER] ist der einzige rot angezeigte Ausgang. */
    enum class Ende { FERTIG, UEBERGEBEN, WARTET_AUF_WAHL, ABGEBROCHEN, FEHLER }

    /** Abschlusszeile einer Runde nach [sekunden]. */
    fun abschluss(ende: Ende, sekunden: Long): String = when (ende) {
        Ende.FEHLER -> "Fehlgeschlagen nach ${dauer(sekunden)}"
        Ende.WARTET_AUF_WAHL -> "Programmierauftrag wartet auf deine Auswahl"
        Ende.UEBERGEBEN -> "An den Programmierer übergeben nach ${dauer(sekunden)}"
        Ende.ABGEBROCHEN -> "Abgebrochen nach ${dauer(sekunden)}"
        Ende.FERTIG -> "Fertig nach ${dauer(sekunden)}"
    }

    /** Laufzeit wie im Terminal: „42 s“, „4 min 12 s“, „1 h 3 min“. */
    fun dauer(sekunden: Long): String {
        val s = sekunden.coerceAtLeast(0)
        return when {
            s < 60 -> "$s s"
            s < 3600 -> "${s / 60} min ${s % 60} s"
            else -> "${s / 3600} h ${(s % 3600) / 60} min"
        }
    }

    /** Titel der Werkzeuge, die Arbeit an den Programmierer weitergeben (so stehen sie in den Änderungen einer Runde). */
    val UEBERGABE_TITEL = listOf(de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.WERKZEUG_TITEL,
        "Nachtrag zu einem bestehenden Programmierauftrag", "Rückfrage einer Programmier-Sitzung beantworten")

    /**
     * Der Satz zu einem Werkzeugaufruf. Reihenfolge: eigener Satz des Werkzeugs ([Werkzeug.taetigkeit]), diese Bibliothek, sonst von
     * selbst aus App und Art (nur lesend: „schaut in Geniale Aufgaben“, löschend, sonst „arbeitet in …“). So zeigt jedes neue Werkzeug
     * und jede neue App ohne Nacharbeit, in welchem Bereich Jarvis gerade arbeitet.
     */
    fun werkzeug(context: Context, werkzeug: Werkzeug?, name: String, argumente: JSONObject = JSONObject()): String {
        werkzeug?.taetigkeit?.takeIf { it.isNotBlank() }?.let { return it }
        WERKZEUGE[name]?.let { return it }
        when (name) {
            "jarvis_merken" -> return when (argumente.optString("art").trim().lowercase()) {
                "regel", "programmierregel", "stil" -> "ändert seine eigenen Regeln"
                "ziel" -> "merkt sich ein Ziel"
                "entscheidung" -> "merkt sich deine Entscheidung"
                else -> "merkt sich etwas"
            }
            "jarvis_vergessen" -> return if (argumente.optString("art").trim().lowercase() in REGELARTEN) "streicht eine eigene Regel" else "vergisst etwas Gemerktes"
            "helfer_ausfuehren" -> return argumente.optString("kennung").takeIf { it.isNotBlank() }?.let { "benutzt den Helfer „$it“" } ?: "benutzt einen Helfer"
        }
        return automatisch(bereich(context, name), werkzeug, name)
    }

    /** Ohne eigenen Satz: App und Art des Werkzeugs; ganz ohne App der Titel. */
    internal fun automatisch(bereich: String?, werkzeug: Werkzeug?, name: String): String {
        val loescht = werkzeug?.loeschend == true || name.endsWith("_loeschen")
        val liest = werkzeug?.nurLesen == true || name.endsWith("_lesen") || name.endsWith("_suchen")
        if (bereich.isNullOrBlank()) {
            val titel = werkzeug?.titel ?: name.replace('_', ' ')
            return when { loescht -> "löscht: $titel"; liest -> "schaut nach: $titel"; else -> "nutzt: $titel" }
        }
        // „Mac bedienen“ → „bedient den Mac“, statt „arbeitet in Mac bedienen“.
        if (bereich.endsWith(" bedienen")) return "bedient " + bereich.removeSuffix(" bedienen")
        return when { loescht -> "löscht etwas in $bereich"; liest -> "schaut in $bereich"; else -> "arbeitet in $bereich" }
    }

    private val REGELARTEN = setOf("regel", "programmierregel", "stil")

    /** Werkzeugname → Name der App ([Faehigkeit.name]); neu aufgebaut, wenn ein Name fehlt (neue Helfer), höchstens einmal je Minute. */
    @Volatile private var bereiche: Map<String, String> = emptyMap()
    @Volatile private var bereicheStand = 0L

    private fun bereich(context: Context, name: String): String? {
        bereiche[name]?.let { return it }
        val jetzt = System.currentTimeMillis()
        if (jetzt - bereicheStand < 60_000) return null
        bereicheStand = jetzt
        bereiche = runCatching { Register.alle(context).flatMap { f -> f.werkzeuge.map { it.name to f.name } }.toMap() }.getOrDefault(bereiche)
        return bereiche[name]
    }

    private val WERKZEUGE = mapOf(
        // Jarvis selbst
        "jarvis_kontext" to "holt sich das Gesamtbild",
        "jarvis_status" to "prüft seinen eigenen Zustand",
        "jarvis_gemerktes_lesen" to "schaut in seine Regeln und Notizen",
        "jarvis_langzeitkontext_lesen" to "schaut in den Langzeitkontext",
        "jarvis_langzeitkontext_ersetzen" to "schreibt den Langzeitkontext neu",
        "funktionen_lesen" to "schaut in die Funktionsdatei",
        "einstellungen" to "schaut in seine Einstellungen",
        "gedaechtnis_suchen" to "sucht in seinem Gedächtnis",
        "logbuch_lesen" to "schaut ins Logbuch",
        "protokoll_lesen" to "schaut ins Protokoll",
        "tagesauswertung_lesen" to "schaut in die Tagesauswertung",
        "tagesdaten_lesen" to "schaut in die Tagesdaten",
        "sprachsession_beenden" to "beendet das Sprachgespräch",
        // Programmieren
        "programmieren" to "übergibt den Auftrag an den Programmierer",
        "programmier_nachtrag" to "gibt den Nachtrag an den Programmierer",
        "programmier_antwort" to "beantwortet die Rückfrage des Programmierers",
        "programmierer_sprechen" to "spricht mit dem Programmierer",
        "programmierauftraege" to "schaut in die Programmieraufträge",
        "programmier_fortschritt" to "meldet den Fortschritt",
        "pc_status" to "prüft PC und Mac",
        "repo_lesen" to "schaut im Repo nach",
        // Apps
        "aufgaben_lesen" to "schaut in die Aufgaben",
        "aufgabe_anlegen" to "legt eine Aufgabe an",
        "aufgabe_aendern" to "ändert eine Aufgabe",
        "aufgabe_erledigen" to "hakt eine Aufgabe ab",
        "aufgabe_loeschen" to "löscht eine Aufgabe",
        "erinnerungen_lesen" to "schaut in die Erinnerungen",
        "erinnerung_speichern" to "speichert eine Erinnerung",
        "tagebuch_lesen" to "schaut ins Tagebuch",
        "kalender_lesen" to "schaut in den Kalender",
        "dienstplan_lesen" to "schaut in den Dienstplan",
        "kalender_eintragen" to "trägt einen Termin ein",
        "kalender_loeschen" to "löscht einen Termin",
        "ideen_lesen" to "schaut in die Ideen",
        "idee_lesen" to "liest eine Idee",
        "idee_speichern" to "speichert eine Idee",
        "idee_anlegen" to "speichert eine Idee",
        "idee_aendern" to "ändert eine Idee",
        "idee_loeschen" to "löscht eine Idee",
        "ziele_lesen" to "schaut in die Ziele",
        "ziel_speichern" to "trägt ein Ziel ein",
        "ziel_auswerten" to "wertet ein Ziel aus",
        "ziel_loeschen" to "löscht ein Ziel",
        "ziele_kontextdatei" to "schaut in die Kontextdatei der Ziele",
        "ziele_einstellungen" to "schaut in die Einstellungen der Ziele-App",
        "biomarker_auswertung" to "wertet deine Biodaten aus",
        "biomarker_tag" to "schaut in die Biodaten des Tages",
        "biomarker_verlauf" to "schaut in den Verlauf der Biodaten",
        "biomarker_aktualisieren" to "holt frische Biodaten",
        "biomarker_katalog" to "schaut in den Biomarker-Katalog",
        "trainings_lesen" to "schaut in die Trainings",
        "training_details" to "schaut sich ein Training an",
        "epigenetik_lesen" to "schaut in die Epigenetik",
        "epigenetik_uebersicht" to "schaut in die Epigenetik",
        "epigenetik_schalter" to "schaut in die Epigenetik",
        "epigenetik_recherchieren" to "recherchiert die Epigenetik neu",
        "stacks_lesen" to "schaut ins Stack-Labor",
        "wetter_lesen" to "schaut aufs Wetter",
        "fahrzeit_lesen" to "prüft die Fahrzeit",
        "abfahrt_einstellen" to "stellt die Losfahr-Meldung",
        "wecker_lesen" to "schaut in die Wecker",
        "wecker_stellen" to "stellt einen Wecker",
        "wecker_loeschen" to "löscht einen Wecker",
        "timer" to "kümmert sich um den Timer",
        "stoppuhr" to "kümmert sich um die Stoppuhr",
        "mail_lesen" to "schaut in die E-Mails",
        "mail_details" to "liest eine E-Mail",
        "mail_senden" to "schreibt eine E-Mail",
        "prompts_lesen" to "schaut in die Prompts",
        "prompt_speichern" to "speichert einen Prompt",
        "kontingente_lesen" to "schaut in die Kontingente",
        "anthropic_guthaben" to "prüft das Anthropic-Guthaben",
        "google_guthaben" to "prüft das Google-Guthaben",
        "konten_lesen" to "schaut in die Konten",
        // Wissen, Ablage, Internet
        "wissen_lesen" to "schaut in die Wissens-Datenbank",
        "wissen_inhalt" to "schaut in die Wissens-Datenbank",
        "wissen_suchen" to "sucht in der Wissens-Datenbank",
        "ablage_liste" to "schaut in die Ablage",
        "ablage_struktur" to "schaut in die Ablage",
        "ablage_lesen" to "liest in der Ablage",
        "ablage_datei" to "holt eine Datei aus der Ablage",
        "ablage_zeigen" to "zeigt eine Datei aus der Ablage",
        "ablage_schreiben" to "schreibt in die Ablage",
        "ablage_datei_speichern" to "legt eine Datei in die Ablage",
        "in_ablage_uebernehmen" to "legt das Ergebnis in die Ablage",
        "ablage_loeschen" to "löscht aus der Ablage",
        "pdf_entwurf_pruefen" to "prüft den PDF-Entwurf",
        "bild_erzeugen" to "erzeugt ein Bild",
        "web_suche" to "sucht im Internet",
        "web_seite_lesen" to "liest eine Webseite",
        // Agenten und Helfer
        "agent_starten" to "beauftragt einen Agenten",
        "agenten" to "schaut nach den Agenten",
        "vorgaenge" to "schaut nach den Hintergrundaufträgen",
        "vorgang_abbrechen" to "bricht einen Hintergrundauftrag ab",
        "vorgang_fortsetzen" to "setzt einen Hintergrundauftrag fort",
        "helfer_suchen" to "sucht einen passenden Helfer",
        "helfer_info" to "schaut sich einen Helfer an",
        "helfer_bauen" to "lässt einen Helfer bauen",
        "helfer_registrieren" to "registriert einen Helfer",
        "helfer_verwalten" to "verwaltet die Helfer",
        "helfer_auftraege" to "schaut nach den Werkzeugbauten",
    )
}
