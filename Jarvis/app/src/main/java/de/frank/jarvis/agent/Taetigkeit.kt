package de.frank.jarvis.agent

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

    /** Abschluss einer Runde nach [sekunden]: Fehler, offene Auswahlkarte, Übergabe oder Erfolg. */
    fun abschluss(fehler: Boolean, uebergeben: Boolean, wartetAufWahl: Boolean, sekunden: Long): String = when {
        fehler -> "Fehlgeschlagen nach $sekunden s"
        wartetAufWahl -> "Programmierauftrag wartet auf deine Auswahl"
        uebergeben -> "An den Programmierer übergeben nach $sekunden s"
        else -> "Fertig nach $sekunden s"
    }

    /** Der Satz zu einem Werkzeugaufruf; unbekannte Werkzeuge bekommen eine Beschreibung nach ihrer Art, sonst ihren Titel. */
    fun werkzeug(name: String, titel: String, argumente: JSONObject = JSONObject()): String {
        WERKZEUGE[name]?.let { return it }
        return when (name) {
            "jarvis_merken" -> when (argumente.optString("art")) {
                "regel", "programmierregel", "stil" -> "ändert seine eigenen Regeln"
                "ziel" -> "merkt sich ein Ziel"
                "entscheidung" -> "merkt sich deine Entscheidung"
                else -> "merkt sich etwas"
            }
            "jarvis_vergessen" -> if (argumente.optString("art") in setOf("regel", "programmierregel", "stil")) "streicht eine eigene Regel" else "vergisst etwas Gemerktes"
            else -> when {
                name.endsWith("_lesen") || name.endsWith("_suchen") || name.endsWith("_info") -> "schaut nach: $titel"
                name.endsWith("_loeschen") -> "löscht: $titel"
                name.endsWith("_speichern") || name.endsWith("_anlegen") || name.endsWith("_aendern") || name.endsWith("_setzen") -> "trägt ein: $titel"
                else -> "nutzt: $titel"
            }
        }
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
        "helfer_ausfuehren" to "benutzt einen Helfer",
        "helfer_bauen" to "lässt einen Helfer bauen",
        "helfer_registrieren" to "registriert einen Helfer",
        "helfer_verwalten" to "verwaltet die Helfer",
        "helfer_auftraege" to "schaut nach den Werkzeugbauten",
    )
}
