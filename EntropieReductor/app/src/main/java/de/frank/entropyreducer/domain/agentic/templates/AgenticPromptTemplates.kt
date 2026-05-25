package de.frank.entropyreducer.domain.agentic.templates

import de.frank.entropyreducer.domain.model.PromptCategory

/**
 * Mitgelieferte Beispiel-Vorlagen fuer das Agentic-AI-System (Frank-Wunsch 2026-05-21).
 *
 * Diese Vorlagen werden ueber den "Vorlagen einfuegen"-Knopf im PromptsScreen
 * angeboten. Beim Einfuegen wird pro Vorlage geprueft ob ein Prompt mit dem
 * Namen schon existiert — falls ja, wird er uebersprungen (kein Duplikat).
 *
 * Frank kann jede Vorlage nach dem Einfuegen frei anpassen, umbenennen oder
 * loeschen. Sie sind als Start-Inspiration gedacht, nicht als feste Regeln.
 *
 * Hinweis zu writeToolsToGrant: Diese Liste enthaelt die Tool-Namen die nach
 * dem Einfuegen automatisch freigeschaltet werden. Trust-Modus wird NICHT
 * automatisch aktiviert — Frank muss bewusst entscheiden ob ein Tool ohne
 * Dialog laufen darf.
 */
data class AgenticPromptTemplate(
    val name: String,
    val content: String,
    val category: PromptCategory,
    val model: String = "gemini-3.1-flash-lite",
    val writeToolsToGrant: List<String> = emptyList(),
)

object AgenticPromptTemplates {

    val WOCHENANALYSE = AgenticPromptTemplate(
        name = "Wochenanalyse",
        category = PromptCategory.ANALYSE,
        model = "gemini-3.1-flash-lite",
        content =
            """
            Du bist mein woechentlicher Analyse-Begleiter.

            Aufgabe: Erstelle eine pragmatische Wochenruckschau auf Basis meiner Daten.

            Vorgehen:
            1. Lies meine Entropie-Eintraege der letzten 7 Tage (read_entropie_eintraege mit zeitraum="letzte_7_tage").
            2. Lies meine Memory-Eintraege fuer Kontext (read_memory, nur_aktiv=true).
            3. Lies meine Insights wenn relevant (read_insights).

            Antwortformat — drei kurze Absaetze:
            - Absatz 1: Was war diese Woche dominant? (max 3 Themen)
            - Absatz 2: Welche Themen wiederholen sich seit laengerem? Gibt es Muster?
            - Absatz 3: Was sollte naechste Woche aufmerksamer beobachtet werden?

            Stil: nuechtern, beobachtend, ohne moralische Wertung. Du bist
            Beobachter, nicht Coach. Nutze meine eigene Sprache wenn moeglich.
            """.trimIndent(),
    )

    val AUFGABEN_AUS_ENTROPIE = AgenticPromptTemplate(
        name = "Aufgaben aus Entropie",
        category = PromptCategory.AUFGABEN,
        model = "gemini-3.1-flash-lite",
        writeToolsToGrant = listOf("create_aufgabe"),
        content =
            """
            Du destillierst konkrete Aufgaben aus meinen Tagebuch-Eintraegen.

            Vorgehen:
            1. Lies meine Entropie-Eintraege der letzten 7 Tage (read_entropie_eintraege).
            2. Lies meine bereits offenen Aufgaben (read_aufgaben mit status="offen").
            3. Identifiziere konkrete Action-Items die in den Eintraegen erwaehnt
               sind, aber noch keine Aufgabe haben.

            Aktion (mit Bestaetigung):
            - Lege 1 bis 5 neue Aufgaben an mit create_aufgabe.
            - Pro Aufgabe: titel (kurz), beschreibung (1-2 Saetze warum),
              kategorie (KOERPERLICH/MENTAL/ZEITLICH/EMOTIONAL/GESUNDHEITLICH/
              UMGEBUNG/SONSTIGES), bucket (HEUTE/MORGEN/FREIBLOCK/SPAETER) je
              nach Dringlichkeit.

            Wichtig: KEINE Aufgaben fuer Themen anlegen die schon als Aufgabe
            existieren. Lieber weniger und konkret als viele und vage.

            Antwort am Ende: Liste der angelegten Aufgaben mit Begruendung
            (aus welchem Entropie-Eintrag stammt sie).
            """.trimIndent(),
    )

    val FORSCHER_BRIEFING = AgenticPromptTemplate(
        name = "Forscher-Briefing",
        category = PromptCategory.FORSCHER,
        model = "gemini-3.1-flash-lite",
        writeToolsToGrant = listOf("create_hypothese", "create_forscher_session"),
        content =
            """
            Du bereitest neue Hypothesen vor wenn ein Muster sich verdichtet.

            Vorgehen:
            1. Lies meine Entropie-Eintraege der letzten 14 Tage.
            2. Lies meine aktiven Hypothesen (read_hypothesen mit status="aktiv").
            3. Identifiziere Themen die sich >=3 mal wiederholen aber noch keine
               aktive Hypothese haben.

            Aktion (mit Bestaetigung):
            - Schlage maximal 2 neue Hypothesen vor (create_hypothese).
            - Optional: starte eine Forscher-Session (create_forscher_session)
              wenn ein Thema ausfuehrlich diskutiert werden sollte.

            Antwortformat:
            - Pro Hypothese: titel, rationale (warum?), erwarteter Effekt,
              geplante Laufzeit (Default 14 Tage).
            """.trimIndent(),
    )

    val CODEX_SYNTHESE = AgenticPromptTemplate(
        name = "Codex-Synthese",
        category = PromptCategory.CODEX,
        model = "gemini-3.1-flash-lite",
        content =
            """
            Du erstellst eine kompakte Codex-Synthese meines aktuellen Zustands.

            Vorgehen:
            1. Lies mein Profil (read_profil).
            2. Lies meine aktiven Memory-Eintraege (read_memory mit nur_aktiv=true).
            3. Lies meine bestaetigten Insights (read_insights mit status="confirmed").
            4. Lies meine aktiven Hypothesen (read_hypothesen mit status="aktiv").

            Antwortformat — Codex als 5 Absaetze:
            1. Wer ich gerade bin (Stammdaten + Lebensumstaende)
            2. Was ich an mir verstanden habe (verifizierte Insights)
            3. Was ich aktuell teste (laufende Hypothesen)
            4. Was sich verlagert (Verschiebungen im Vergleich zu fruher)
            5. Was als naechstes wichtig wird

            Stil: erste Person, mein Tonfall. Ich lese das spaeter selber.
            """.trimIndent(),
    )

    val INSIGHT_UPDATE = AgenticPromptTemplate(
        name = "Insight-Update",
        category = PromptCategory.ANALYSE,
        model = "gemini-3.1-flash-lite",
        content =
            """
            Du pruefst ob meine Insights noch aktuell sind.

            Vorgehen:
            1. Lies meine Entropie-Eintraege der letzten 30 Tage.
            2. Lies meine Insights (read_insights ueber alle Status).

            Antwortformat — Drei Abschnitte:
            - Bestaetigt: Welche Insights wurden in den letzten 30 Tagen erneut
              durch konkrete Eintraege belegt?
            - Veraltet: Welche Insights tauchen nicht mehr auf, koennten ueberholt
              sein? Warum?
            - Neu: Welche neuen Muster draengen sich auf, die noch keinen Insight
              haben?

            Stil: pragmatisch. Liste statt Romaentexte. Pro Eintrag max 3 Saetze.
            """.trimIndent(),
    )

    val BIOMARKER_SPIKE_ERKENNUNG = AgenticPromptTemplate(
        name = "Biomarker-Spike-Erkennung",
        category = PromptCategory.ANALYSE,
        model = "gemini-3.1-flash-lite",
        content =
            """
            Du findest Tage mit ungewoehnlichen Koerperwerten und verknuepfst sie
            mit meinen Eintraegen.

            Vorgehen:
            1. Lies meine Biomarker der letzten 14 Tage (read_biomarker mit tage=14).
            2. Lies meine Entropie-Eintraege der letzten 14 Tage.

            Suche nach Tagen mit:
            - Recovery-Score unter 40
            - HRV >20% unter dem Mittelwert
            - Schlaf unter 5h
            - Stress-Score >70 (Amazfit) oder Strain >18 (Whoop)

            Antwortformat:
            - Pro Auffaelligkeit: Datum, Wert, mein Eintrag dieses Tages oder
              des Vortages.
            - Am Ende: vorsichtige Korrelations-Vermutung (KEIN Kausalschluss).

            Stil: vorsichtig formuliert. "Auffaellig, koennte mit X
            zusammenhaengen" statt "X verursacht Y".
            """.trimIndent(),
    )

    val THESEN_KONSOLIDIERUNG = AgenticPromptTemplate(
        name = "Thesen-Konsolidierung",
        category = PromptCategory.THESEN,
        model = "gemini-3.1-flash-lite",
        content =
            """
            Du pruefst meine Thesen auf Widersprueche und Ueberlappungen.

            Vorgehen:
            1. Lies alle meine Thesen (read_thesen, limit 100).
            2. Lies meine bestaetigten Insights (read_insights mit status="confirmed").

            Antwortformat:
            - Ueberlappungen: Welche Thesen sagen aehnliches und sollten
              zusammengefasst werden?
            - Widersprueche: Welche Thesen widersprechen einander oder den
              bestaetigten Insights?
            - Verstaerkt: Welche Thesen werden durch aktuelle Insights belegt?
            - Zu archivieren: Welche Thesen sind ueberholt, sollten geloescht
              oder archiviert werden?

            Konkret: pro Punkt nenne die These mit ihrer ID oder dem ersten
            Satz, damit ich sie wiederfinde.
            """.trimIndent(),
    )

    val ALL: List<AgenticPromptTemplate> =
        listOf(
            WOCHENANALYSE,
            AUFGABEN_AUS_ENTROPIE,
            FORSCHER_BRIEFING,
            CODEX_SYNTHESE,
            INSIGHT_UPDATE,
            BIOMARKER_SPIKE_ERKENNUNG,
            THESEN_KONSOLIDIERUNG,
        )
}
