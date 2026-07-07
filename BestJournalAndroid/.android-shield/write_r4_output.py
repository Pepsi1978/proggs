# -*- coding: utf-8 -*-
"""Atomic writer for r4-hwg-ai worker output (FIN-025 schema)."""
import json, os, tempfile

OUT = os.path.expanduser(
    "~/proggs/BestJournalAndroid/.android-shield/worker-outputs/phase1b-recht-hwg-ai.json"
)
STRINGS = "app/src/main/res/values/strings.xml"

findings = [
    # ---------- HOCH ----------
    {
        "findingId": "A1",
        "riskLevel": "🟥",
        "category": "HWG",
        "language": "de",
        "jurisdiction": "DE/EU",
        "file": STRINGS,
        "line": 808,
        "stringKey": "ai_prompt_no_dates_rule",
        "currentText": "RICHTIG: \"Deine Borreliose haengt direkt mit dem Bewegungsmangel zusammen.\" ... RICHTIG: \"Laufen ist der Schluessel zur mentalen Klarheit, die du beim Intervalltraining spuerst.\"",
        "context": "System-Prompt-Regel (TTS-Datumsverbot), an JEDE Dashboard-Analyse angehaengt. Die 'RICHTIG'-Beispiele sind das Stil-Vorbild, das die KI nachahmt -> Gemini wird auf krankheitsbezogene Kausalaussagen trainiert.",
        "rationale": "HWG Sec.3 (irrefuehrende Heilaussagen) + Produkthaftung: Die App liefert per Prompt-Beispiel eine medizinische Kausalbehauptung ('Borreliose haengt direkt mit Bewegungsmangel zusammen') als Vorbild. Da die App den Prompt verantwortet, verantwortet sie auch krankheitsbezogene Outputs, die daraus entstehen. 'Schluessel zur mentalen Klarheit' = Wirkversprechen. Funktionserhaltung: Nur die BEISPIELE austauschen (Ordnung/Projekte/Schlafrhythmus statt Borreliose/mentale Klarheit) — die TTS-Datumsregel-Funktion bleibt voll erhalten.",
        "invasivityLevel": "text-only",
        "suggestedFixes": [
            {"fix": "Krankheits-Beispiele durch neutrale Alltags-Beispiele ersetzen: FALSCH \"Eintraege vom 3.4. zeigen dein volles Postfach\" / RICHTIG \"Dein Postfach laeuft seit Tagen ueber und raubt dir Zeit.\"", "lengthDeltaPct": 0},
            {"fix": "Zweites Beispiel neutral: FALSCH \"Laufen am 24.04. brachte dir Klarheit\" / RICHTIG \"Bewegung schafft dir spuerbar mehr Ueberblick im Alltag.\" (keine medizinische Kausalitaet)", "lengthDeltaPct": 0},
            {"fix": "Zusatz-Satz in die Regel: \"Stelle KEINE medizinischen Kausalzusammenhaenge her (z.B. Krankheit X kommt von Verhalten Y) — bleibe bei beobachtbaren Alltagsmustern.\"", "lengthDeltaPct": 8},
        ],
        "impactWithoutFix": "KI gibt mit hoher Wahrscheinlichkeit krankheitsbezogene Kausalaussagen aus (Borreliose-Stil). Abmahnrisiko HWG/UWG, Produkthaftung wenn Nutzer medizinische Schluesse zieht.",
    },
    {
        "findingId": "A2",
        "riskLevel": "🟥",
        "category": "HWG",
        "language": "de",
        "jurisdiction": "DE/EU",
        "file": STRINGS,
        "line": 914,
        "stringKey": "ai_prompt_entropy_intro",
        "currentText": "Du bist ein empathischer, hochintelligenter Lebensberater und Muster-Analyst. ... Finde wiederkehrende Quellen von Stress, Unordnung und Belastung. ... Alles, was Unordnung, Stress, Energieverlust, Schmerz, Schlafprobleme, emotionale Belastung oder Kontrollverlust im Leben des Nutzers erzeugt.",
        "context": "System-Prompt des Profils 'Raeume dein Leben auf'. Erzeugt ein 'Ratschlags-Dashboard' mit Prioritaet 'hoch=sofort handeln'. Die KI gibt aktiv Handlungsratschlaege bei 'Schmerz' und 'Schlafproblemen'.",
        "rationale": "HWG-Grenzbereich + Produkthaftung: 'Lebensberater', der gezielt 'Schmerz, Schlafprobleme, emotionale Belastung' analysiert und konkrete Massnahmen ('sofort handeln') gibt, naehert sich Gesundheits-/Psychoberatung. 'Schmerz' + 'Schlafprobleme' sind explizit koerper-/krankheitsnah. Funktionserhaltung: Wort 'Schmerz' und 'Schlafprobleme' aus der Sucheliste nehmen ODER um Guardrail ergaenzen — die Aufraeum-/Belastungs-Funktion bleibt voll.",
        "invasivityLevel": "text-only",
        "suggestedFixes": [
            {"fix": "Suchliste entschaerfen: '...Unordnung, Stress, Energieverlust, Ueberforderung, Zeitdruck oder Kontrollverlust...' (Schmerz/Schlafprobleme entfernen, bleibt Alltags-Belastung)", "lengthDeltaPct": -3},
            {"fix": "Guardrail-Satz anfuegen: 'Du gibst KEINE medizinischen, therapeutischen oder diagnostischen Ratschlaege. Bei gesundheitlichen Themen verweist du wertneutral auf Fachpersonen.'", "lengthDeltaPct": 14},
            {"fix": "Rolle umbenennen: 'einfuehlsamer Ordnungs- und Muster-Analyst' statt 'Lebensberater' (vermeidet Beratungs-Berufsbild)", "lengthDeltaPct": -1},
        ],
        "impactWithoutFix": "KI produziert handlungsleitende Ratschlaege zu Schmerz/Schlaf (Gesundheitsberatung-Anschein). Ohne sichtbaren Disclaimer am Output: HWG-/Haftungsrisiko.",
    },
    {
        "findingId": "A3",
        "riskLevel": "🟥",
        "category": "Produkthaftung",
        "language": "de",
        "jurisdiction": "DE/EU",
        "file": STRINGS,
        "line": 933,
        "stringKey": "ai_prompt_custom_intro",
        "currentText": "SCHRITT 2 — AUFGABE AUSFUEHREN: %1$s ... Wenn der Auftrag Recherche, Ideen, Alternativen, Vorschlaege, Empfehlungen oder neue Informationen verlangt, dann liefere diese AKTIV, auch wenn sie in den Eintraegen nicht vorkommen.",
        "context": "Custom-Profil: %1$s ist ein FREI vom Nutzer waehlbarer Auftrag. Der Prompt enthaelt KEINE Verbots-/Schutzklausel. Nutzer kann 'gib mir medizinische Ratschlaege zu meiner Krankheit' eintragen -> KI liefert aktiv ueber die Tagebuchdaten hinaus.",
        "rationale": "Produkthaftung + HWG/RDG-Risiko: Unbegrenzter, frei steuerbarer KI-Output ohne fachlichen Guardrail. Der Nutzer kann die KI in medizinische/rechtliche/finanzielle Beratung lenken; der Prompt 'liefere AKTIV neue Informationen' verstaerkt das. Auch der Re-Ranker (ai_prompt_rerank_system, Z.940) hat KEINEN Health-Guardrail. Funktionserhaltung: Custom-Funktion (eigener Fokus) bleibt voll — nur ein Verbots-Satz fuer Fachberatung wird ergaenzt.",
        "invasivityLevel": "text-only",
        "suggestedFixes": [
            {"fix": "Guardrail in ai_prompt_custom_intro anfuegen: 'UNABHAENGIG vom Auftrag gibst du KEINE medizinischen Diagnosen, Therapieempfehlungen, Rechts- oder Finanzberatung. Verweise bei solchen Themen wertneutral auf qualifizierte Fachpersonen.'", "lengthDeltaPct": 9},
            {"fix": "Gleichen Guardrail-Satz in ai_prompt_rerank_system (Z.940) ergaenzen, da der Re-Ranker neue Massnahmen frei generiert", "lengthDeltaPct": 6},
            {"fix": "In ai_prompt_custom_rules (Z.935) als zusaetzliche AUFTRAGS-REGEL spiegeln, damit der Guardrail auch im zweiten Prompt-Block steht", "lengthDeltaPct": 5},
        ],
        "impactWithoutFix": "Nutzer kann KI zu Heilaussagen/Rechtsrat verleiten; App haftet fuer den generierten Inhalt (Produkthaftung, HWG, ggf. RDG). Kein technischer Schutz vorhanden.",
    },
    {
        "findingId": "A4",
        "riskLevel": "🟥",
        "category": "HWG",
        "language": "de",
        "jurisdiction": "DE/EU",
        "file": STRINGS,
        "line": 390,
        "stringKey": "profile_entropy_long",
        "currentText": "Die KI sucht gezielt nach Stress, Belastung und Unordnung in deinen Eintraegen. Du bekommst: eine Analyse deiner groessten Belastungsquellen, 5 konkrete Massnahmen zum Aufraeumen, Tipps die dir im Alltag helfen koennen. Ideal wenn du das Gefuehl hast, dass gerade alles zu viel wird.",
        "context": "Profil-Beschreibung (Auswahl-Screen), bewirbt das Entropy-Profil. 'wenn alles zu viel wird' + '5 konkrete Massnahmen' + 'Tipps' = Wirkversprechen im psychischen Belastungs-Kontext.",
        "rationale": "HWG-Grenzbereich (Werbeaussage): Die Kombination 'wenn alles zu viel wird' (psychische Ueberlastung) + Versprechen konkreter Massnahmen/Tipps positioniert das Feature als Stress-/Belastungs-Hilfe. Grenzt an gesundheitsbezogenes Wirkversprechen. Funktionserhaltung: Reine Werbe-/Beschreibungstext, frei umformulierbar ohne Funktionsverlust.",
        "invasivityLevel": "text-only",
        "suggestedFixes": [
            {"fix": "Verkaufsstark + sicher: 'Die KI findet, was dich im Alltag belastet, und sortiert es. Du bekommst eine Uebersicht deiner groessten Belastungsquellen und 5 konkrete Aufraeum-Schritte. Ideal, wenn du den Ueberblick zurueckgewinnen willst.' (kein Heilbezug)", "lengthDeltaPct": -8},
            {"fix": "Disclaimer-Halbsatz anfuegen: '... helfen koennen. (Anregungen zur Selbstreflexion, keine Therapie.)'", "lengthDeltaPct": 10},
            {"fix": "'alles zu viel wird' -> 'der Alltag unuebersichtlich wird' (entfernt den Ueberlastungs-/Gesundheits-Beiklang, bleibt einladend)", "lengthDeltaPct": -2},
        ],
        "impactWithoutFix": "Wird als gesundheitsbezogenes Wirkversprechen lesbar (HWG/UWG Sec.5). Im Verbund mit Paywall (paywall_headline_stress, onboarding_goal_stress) verstaerkt sich die Wirkungsaussage.",
    },
    # ---------- MITTEL ----------
    {
        "findingId": "A5",
        "riskLevel": "🟧",
        "category": "HWG",
        "language": "de",
        "jurisdiction": "DE/EU",
        "file": STRINGS,
        "line": 897,
        "stringKey": "ai_prompt_goals_definition",
        "currentText": "Direkt: \"Ich will abnehmen\" ... Klagen: \"Mein Schlaf ist so schlecht\" = implizites Ziel Schlafverbesserung",
        "context": "Goals-Profil-Prompt: leitet die KI an, Gesundheits-/Koerperziele ('abnehmen', 'Schlafverbesserung') zu erkennen und Fortschritt zu verfolgen + 'naechste Schritte' (top_massnahmen) zu liefern.",
        "rationale": "HWG-Grenzbereich: KI verfolgt aktiv Abnehm-/Schlafziele und gibt 'naechste Schritte'. Bewegt sich in Wellness-/Gesundheitsberatung. Keine Diagnose-Anweisung (mildernd). Funktionserhaltung: Beispiele bleiben moeglich, aber Guardrail noetig, dass keine konkreten medizinischen Empfehlungen (Diaeten, Medikamente) gegeben werden.",
        "invasivityLevel": "text-only",
        "suggestedFixes": [
            {"fix": "Guardrail in ai_prompt_goals_intro (Z.896) oder _rules (Z.899): 'Bei Gesundheits- oder Koerperzielen begleitest du nur motivierend; gib KEINE Diaet-, Medikamenten- oder Therapieempfehlungen.'", "lengthDeltaPct": 7},
            {"fix": "Beispiel 'abnehmen' durch neutrales Ziel ersetzen: 'Ich will mehr sparen' / 'Ich will das Projekt fertig machen'", "lengthDeltaPct": 0},
            {"fix": "Beibehalten, aber Health-Disclaimer (ai_output_health_disclaimer) auf dem Goals-Dashboard sicherstellen (ist via AiOutputDisclaimer am Dashboard-Listenende gegeben)", "lengthDeltaPct": 0},
        ],
        "impactWithoutFix": "KI koennte konkrete Abnehm-/Schlaf-Empfehlungen geben (Gesundheitsberatung). Mittel, da motivierender Rahmen und Disclaimer am Dashboard vorhanden.",
    },
    {
        "findingId": "A6",
        "riskLevel": "🟧",
        "category": "HWG",
        "language": "de",
        "jurisdiction": "DE/EU",
        "file": STRINGS,
        "line": 904,
        "stringKey": "ai_prompt_insight_intro",
        "currentText": "Du bist ein einfuehlsamer, tiefgruendiger Muster-Analyst fuer persoenliche Entwicklung. ... Finde darin verborgene Muster, ... unbewusste Ueberzeugungen, emotionale Reaktionsmuster ... (Begleittext Z.905/910: 'wohlwollender Spiegel', 'Vermeidungsmuster', Bereiche 'Aengste', 'Resilienz', 'Grenzen')",
        "context": "Selbsterkenntnis-Profil-Prompt. Erzeugt psychologisch anmutende Profile mit Bereichen 'Aengste', 'Resilienz', 'Denkmuster'. Naehe zu Psychoanalyse/Psychotherapie.",
        "rationale": "HWG-Grenzbereich (psychische Gesundheit): Begriffe 'unbewusste Ueberzeugungen', 'emotionale Reaktionsmuster', 'Vermeidungsmuster', 'Aengste', 'Resilienz' sind psychotherapeutisches Vokabular. Risiko, dass Ausgaben wie psychologische Befunde wirken. Keine Diagnose-Anweisung (mildernd), Disclaimer am Dashboard vorhanden. Funktionserhaltung: Selbsterkenntnis-Funktion bleibt; ein Anti-Pathologisierungs-Satz schuetzt.",
        "invasivityLevel": "text-only",
        "suggestedFixes": [
            {"fix": "Anti-Pathologisierungs-Guardrail in ai_prompt_insight_attitude (Z.905): 'Du stellst KEINE psychologischen Diagnosen und benennst keine Stoerungen oder Krankheiten. Du beschreibst nur beobachtbare Muster aus den eigenen Worten des Nutzers.'", "lengthDeltaPct": 6},
            {"fix": "Bereich 'Aengste' (Z.910) zu 'Sorgen' umbenennen (alltagssprachlich, weniger klinisch)", "lengthDeltaPct": 0},
            {"fix": "Beibehalten + sicherstellen dass AiOutputDisclaimer auf dem Insight-Dashboard erscheint (gegeben via Dashboard-Footer Z.1455)", "lengthDeltaPct": 0},
        ],
        "impactWithoutFix": "Ausgaben koennen wie psychologische Befunde/Diagnosen wirken (z.B. 'Du zeigst ein Vermeidungsmuster/Angststoerung'). HWG-/Haftungsnaehe bei pathologisierenden Formulierungen.",
    },
    {
        "findingId": "A7",
        "riskLevel": "🟧",
        "category": "HWG",
        "language": "de",
        "jurisdiction": "DE/EU",
        "file": "app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt",
        "line": 2221,
        "stringKey": "urgency_high / legend_high_burden",
        "currentText": "Dringend — Sofort handeln  (Advice-Detail-Dialog zeigt einzelnen Ratschlag mit Prioritaet 'hoch', OHNE Disclaimer)",
        "context": "AdviceDerivationDialog (DashboardScreen.kt:2221) ist die Detailansicht EINES Ratschlags. Hier kann ein als 'hoch / Dringend — Sofort handeln' priorisierter KI-Ratschlag ohne sichtbaren Health-Disclaimer stehen. Der AiOutputDisclaimer haengt nur am Ende der Listen-Ansicht (Z.1455), nicht im Detail-Dialog.",
        "rationale": "HWG-/Haftungs-Coverage-Luecke: Genau im Moment des dringlichsten Handlungsratschlags ('Sofort handeln') fehlt die Abgrenzung 'keine Therapie'. Direktive: Disclaimer soll an JEDER Stelle sichtbar sein, wo KI-Gesundheitsratschlaege erscheinen koennen. invasivityLevel ehrlich: layout-required (eine Compose-Zeile in den Dialog einfuegen).",
        "invasivityLevel": "layout-required",
        "suggestedFixes": [
            {"fix": "In AdviceDerivationDialog vor dem confirmButton eine kompakte Disclaimer-Zeile (ai_output_health_disclaimer) ergaenzen — eine Zeile Compose: 'AiOutputDisclaimer()' am Ende der text-Column", "lengthDeltaPct": 0},
            {"fix": "Alternativ kleineren Text 'Anregung, keine Therapie' unter advice.description einblenden", "lengthDeltaPct": 0},
            {"fix": "'Sofort handeln' zu 'Bald angehen' / 'Zuerst kuemmern' abmildern (entfernt Notfall-/Gesundheits-Imperativ aus dem Wohlbefindens-Kontext)", "lengthDeltaPct": 0},
        ],
        "impactWithoutFix": "Dringlichkeits-Ratschlag der KI ohne Therapie-Abgrenzung im Detail-Dialog. Im psychischen Belastungs-Kontext als Gesundheitsanweisung lesbar.",
    },
    {
        "findingId": "Z1",
        "riskLevel": "🟧",
        "category": "AI-Act",
        "language": "de",
        "jurisdiction": "DE/EU",
        "file": "app/src/main/java/com/bestjournal/app/ui/components/CrisisHelpDialog.kt",
        "line": 49,
        "stringKey": "settings_crisis_dialog_body",
        "currentText": "CrisisHelpDialog vorhanden (Telefonseelsorge, 112, findahelpline.com), ABER nur manuell ueber Settings-Button (SettingsScreen.kt:3587) aufrufbar. KEINE automatische Krisen-/Suizid-Erkennung im Code.",
        "context": "grep nach suizid/crisis/harm/selbstverletz/krise in data/ai/repository/usecase = NONE. Die KI analysiert 'Schmerz, emotionale Belastung' (entropy/insight), aber wenn ein Eintrag Selbstgefaehrdungs-Signale enthaelt, triggert NICHTS automatisch den Hilfe-Verweis.",
        "rationale": "Ethik- + Produkthaftungs-Thema (kein hartes Gesetz in DE, aber Branchenstandard fuer Mental-Health-nahe Apps, vgl. SB-243-Logik): Eine App, die emotionale Belastung analysiert und Ratschlaege gibt, sollte bei erkennbaren Krisen-Signalen proaktiv auf Hilfe verweisen. Der Dialog existiert, ist aber nur tief in den Einstellungen versteckt. Funktionserhaltung: rein additiv — keine bestehende Funktion betroffen.",
        "invasivityLevel": "layout-required",
        "suggestedFixes": [
            {"fix": "Leichtgewichtige Keyword-Heuristik (lokal, keine Cloud) auf den Eintragstext: bei Treffer (Suizid/Selbstverletzung/etc.) nicht-blockierende Karte mit Verweis auf CrisisHelpDialog einblenden", "lengthDeltaPct": 0},
            {"fix": "CrisisHelpDialog-Zugang prominenter machen: dauerhafter kleiner 'Hilfe in Krisen'-Link im Footer der KI-Analyse-Screens (nicht nur Settings)", "lengthDeltaPct": 0},
            {"fix": "Mindestens: im Health-Disclaimer-Langtext (ai_output_health_disclaimer_long, vorhanden Z.1488) bleibt der Verweis 'Krisenhilfe in deinen Einstellungen' — verifiziert: CrisisHelpDialog existiert in Settings, Verweis ist also NICHT irrefuehrend (positiv)", "lengthDeltaPct": 0},
        ],
        "impactWithoutFix": "Bei einem Krisen-Eintrag verweist die App nicht aktiv auf Hilfe. Reputations-/Haftungsrisiko; kein technisches Schutznetz fuer den gefaehrlichsten Anwendungsfall.",
    },
    {
        "findingId": "Z2",
        "riskLevel": "🟧",
        "category": "AI-Act",
        "language": "de",
        "jurisdiction": "EU",
        "file": STRINGS,
        "line": 904,
        "stringKey": "ai_prompt_insight_intro / profile_insight_*",
        "currentText": "KI leitet aus Tagebuchtext 'wiederkehrende Gefuehle', 'emotionale Reaktionsmuster' ab (Insight-Profil). = Emotions-/Stimmungsanalyse aus Text.",
        "context": "EU AI Act Art. 50 Abs. 3 (Emotionserkennungs-Offenlegung). Bewertung der Einordnung.",
        "rationale": "Art. 50(3) (Emotionserkennung-Transparenzpflicht) zielt laut Definition (Art. 3 Nr. 39 i.V.m. Annex) primaer auf BIOMETRIE-basierte Emotionserkennung (Gesicht, Stimme, physiologische Signale). Text-Sentiment aus selbst geschriebenem Tagebuch faellt NACH ueberwiegender Auslegung NICHT unter das biometrische Emotionserkennungssystem -> Art. 50(3)-Pflicht greift hier wahrscheinlich NICHT. Trotzdem dokumentieren, da Graubereich. Die generelle KI-Kennzeichnung (Art. 50 Abs. 1/4) ist abgedeckt (Badge+Tooltip+Banner).",
        "invasivityLevel": "text-only",
        "suggestedFixes": [
            {"fix": "Interne AI-Risikoklassifizierung dokumentieren: 'Stimmungsanalyse aus Nutzer-Text = NICHT biometrisch, begrenztes Risiko Art.50(1)' (Pflicht-internes Artefakt, kein UI-Text)", "lengthDeltaPct": 0},
            {"fix": "Vorsorglich im AiInfoBanner/Tooltip ergaenzen: '... erkennt Themen und Stimmungen in deinen Texten.' (Transparenz ueber die Emotionsanalyse)", "lengthDeltaPct": 0},
            {"fix": "Keine UI-Pflicht-Aenderung noetig; Fachanwalt vor Release zur AI-Act-Einordnung bestaetigen lassen", "lengthDeltaPct": 0},
        ],
        "impactWithoutFix": "Geringes Restrisiko: falls eine Aufsicht Text-Stimmungsanalyse doch als Emotionserkennung wertet, fehlt die explizite Offenlegung. Mit vorhandener KI-Kennzeichnung gut abgesichert.",
    },
    # ---------- NIEDRIG (Konsistenz/Doku) ----------
    {
        "findingId": "Z3",
        "riskLevel": "🟨",
        "category": "AI-Act",
        "language": "de",
        "jurisdiction": "EU",
        "file": STRINGS,
        "line": 805,
        "stringKey": "ai_prompt_response_language",
        "currentText": "ANTWORTSPRACHE: Antworte vollstaendig auf Deutsch. Alle Texte und Ueberschriften muessen auf Deutsch sein, keine andere Sprache.",
        "context": "Wird an JEDEN Prompt angehaengt. translatable. Bei Lokalisierung in 26 Sprachen muss 'Deutsch' durch die Zielsprache ersetzt werden.",
        "rationale": "Funktions-/Lokalisierungs-Hinweis (kein Rechtsverstoss): Wenn der Uebersetzer-Skill 'Deutsch' nicht pro Locale ersetzt, antwortet die KI in falscher Sprache. Hier nur dokumentiert, da R4-Scope (gehoert zum Uebersetzungs-Worker).",
        "invasivityLevel": "text-only",
        "suggestedFixes": [
            {"fix": "Sicherstellen, dass values-xx/strings.xml jeweils die Zielsprache nennt (Aufgabe des uebersetzung-Skills)", "lengthDeltaPct": 0},
            {"fix": "Keine Aenderung in values/ (de) noetig", "lengthDeltaPct": 0},
            {"fix": "Pre-Commit-Check: ai_prompt_response_language darf pro Locale nicht woertlich 'Deutsch' enthalten (ausser de)", "lengthDeltaPct": 0},
        ],
        "impactWithoutFix": "Kein Rechtsrisiko; nur Funktions-/Qualitaetsrisiko bei Lokalisierung.",
    },
]

ai_prompt_audit = [
    {"key": "ai_prompt_entropy_intro", "riskPassage": "Du bist ein empathischer, hochintelligenter Lebensberater ... Schmerz, Schlafprobleme, emotionale Belastung ...", "risk": "HOCH — 'Lebensberater' analysiert Schmerz/Schlafprobleme und gibt Massnahmen 'sofort handeln'. Gesundheitsberatung-Anschein.", "fixDirection": "Rolle zu 'Ordnungs-/Muster-Analyst'; 'Schmerz/Schlafprobleme' aus Suchliste; Anti-Medizin-Guardrail. Funktion (Belastung sortieren) bleibt."},
    {"key": "ai_prompt_no_dates_rule", "riskPassage": "RICHTIG: 'Deine Borreliose haengt direkt mit dem Bewegungsmangel zusammen.' / 'Laufen ist der Schluessel zur mentalen Klarheit ...'", "risk": "HOCH — Stil-Vorbild lehrt krankheitsbezogene Kausalaussagen (HWG-Heilaussage).", "fixDirection": "Beispiele durch neutrale Alltags-Mustersaetze ersetzen (Postfach/Ueberblick); TTS-Datumsregel-Funktion unveraendert."},
    {"key": "ai_prompt_custom_intro", "riskPassage": "%1$s (frei) + 'liefere AKTIV ... auch wenn sie in den Eintraegen nicht vorkommen'", "risk": "HOCH — frei steuerbarer Output ohne Health/RDG-Guardrail; Nutzer kann KI in Fachberatung lenken.", "fixDirection": "Verbots-Satz 'keine Diagnosen/Therapie/Rechts-/Finanzberatung' anfuegen; Custom-Funktion bleibt voll."},
    {"key": "ai_prompt_rerank_system", "riskPassage": "Generiert frei 'profilstaerkere Massnahmen' aus Eintraegen, KEIN Health-Guardrail.", "risk": "HOCH (verbunden mit Custom) — zweiter Gemini-Call ohne Schutzklausel.", "fixDirection": "Gleichen Guardrail-Satz wie Custom ergaenzen."},
    {"key": "ai_prompt_insight_intro", "riskPassage": "unbewusste Ueberzeugungen, emotionale Reaktionsmuster, Vermeidungsmuster; Bereiche 'Aengste','Resilienz'", "risk": "MITTEL — psychotherapeutisches Vokabular; Outputs koennen wie Befunde wirken.", "fixDirection": "Anti-Pathologisierungs-Satz in ai_prompt_insight_attitude; 'Aengste'->'Sorgen'. Selbsterkenntnis-Funktion bleibt."},
    {"key": "ai_prompt_goals_definition", "riskPassage": "'Ich will abnehmen' ... 'Mein Schlaf ist so schlecht' = Ziel Schlafverbesserung", "risk": "MITTEL — KI verfolgt Abnehm-/Schlafziele + 'naechste Schritte' (Wellness-Beratung).", "fixDirection": "Guardrail 'keine Diaet-/Medikamenten-/Therapieempfehlung'; Beispiele optional neutralisieren."},
    {"key": "profile_style_advisor (Z.1414) / profile_style_insight (Z.1415) / profile_style_custom (Z.1417)", "riskPassage": "'Stil eines Lebensberaters'; 'Denkmuster ... aufdecken'; Custom-Stil mit freiem %1$s ohne Schutzklausel", "risk": "MITTEL — Retro-Profil-Stile erzeugen beratende/psychologisierende Rueckblicke; Custom-Stil ohne Guardrail.", "fixDirection": "'Lebensberater' -> 'einfuehlsamer Begleiter'; Health-Guardrail im Custom-Stil; durch ai_output_health_disclaimer abgesichert."},
    {"key": "ai_prompt_goals_intro / ai_prompt_summary_intro / profile_style_coach / profile_style_chronicler", "riskPassage": "'Ziel-Analyst', 'Tagebuch-Analyst', 'Ziel-Begleiter', 'Chronist' — neutral", "risk": "NIEDRIG — keine Gesundheits-/Diagnose-Anweisung; Summary verbietet sogar Coaching/Bewertung explizit (positiv).", "fixDirection": "Keine Aenderung noetig."},
]

verified_ok = [
    "ai_generated_tooltip (Z.785): KI-Kennzeichnung 'Google Gemini' + Fehlerhinweis (AI Act Art.50) — vorbildlich.",
    "ai_generated_badge 'KI-generiert' (Z.784): Badge-Komponente AiGeneratedBadge/Inline gerendert auf Dashboard(270), EntryDetail(343,436), Retrospective(322,1080), FollowUp(213).",
    "AiInfoBanner: dismissbarer KI-Transparenz-Banner auf Dashboard (DashboardScreen:372, gesteuert via shouldShowAiInfoBanner) — Art.50 Offenlegung KI-Einsatz.",
    "label_improved '✨ KI verbessert' (Z.51/1005): Textverbesserungs-Output in JournalScreen als KI gekennzeichnet (Art.50).",
    "ai_output_health_disclaimer (Z.1487) + _long (Z.1488): HWG-Therapie-Disclaimer-Strings vorhanden; AiOutputDisclaimer gerendert auf Dashboard-Listenende(1455), EntryDetail(392,636), Retrospective(675), FollowUp(290).",
    "CrisisHelpDialog (Komponente vorhanden): 112 + Telefonseelsorge 0800-1110111/-222 + findahelpline.com, korrekte tel:/VIEW-Intents mit Fallback-Toast.",
    "Verweis 'Krisenhilfe in deinen Einstellungen' (in ai_output_health_disclaimer_long) ist NICHT irrefuehrend — CrisisHelpDialog existiert tatsaechlich in den Einstellungen (SettingsScreen:3587/3816).",
    "ai_banner_body (Z.1203): 'Lebensratschlaege ... keine professionelle Beratung' — Disclaimer direkt am Banner (positiv).",
    "ai_prompt_summary_intro (Z.923): verbietet der KI ausdruecklich Coaching/Problemsuche/Bewertung ('DU BEWERTEST NICHT') — gute Selbstbegrenzung des neutralen Profils.",
]

out = {
    "worker": "r4-hwg-ai",
    "findings": findings,
    "aiPromptAudit": ai_prompt_audit,
    "disclaimerCoverage": {
        "locations": [
            "DashboardScreen.kt:1455 (AiOutputDisclaimer am Ende der Ratschlaege-Liste)",
            "EntryDetailScreen.kt:392 + 636 (AiOutputDisclaimer)",
            "RetrospectiveScreen.kt:675 (AiOutputDisclaimer)",
            "FollowUpComponents.kt:290 (AiOutputDisclaimer)",
            "JournalScreen.kt:1005 (label_improved '✨ KI verbessert' am verbesserten Text)",
        ],
        "gaps": [
            "AdviceDerivationDialog (DashboardScreen.kt:2221): Detailansicht EINES Ratschlags (kann 'hoch / Sofort handeln') OHNE AiOutputDisclaimer — siehe A7 (layout-required, 1 Zeile).",
            "Profil-Beschreibungen (profile_entropy_long Z.390) bewerben Stress-/Belastungshilfe ohne unmittelbaren Disclaimer — siehe A4.",
        ],
    },
    "aiActCompliance": {
        "badgeCoverage": "GUT: AiGeneratedBadge/Inline auf allen KI-Output-Screens (Dashboard, Retrospective, EntryDetail, FollowUp) + dismissbarer AiInfoBanner + Tooltip mit 'Google Gemini'. Textverbesserung via label_improved gekennzeichnet. Art. 50 Abs. 1/4 (KI-Transparenz) erfuellt. Hinweis: Titel-Generierung (falls vorhanden) nicht separat verifiziert — pruefen ob Auto-Titel ebenfalls als KI markiert ist.",
        "emotionRecognitionAssessment": "Stimmungs-/Gefuehlsanalyse erfolgt aus selbst geschriebenem Tagebuchtext (Insight-Profil: 'emotionale Reaktionsmuster'). Das ist TEXT-Sentiment, NICHT biometrische Emotionserkennung. Art. 50 Abs. 3 (Emotionserkennung) zielt auf biometrische Systeme (Gesicht/Stimme/Physiologie) -> greift hier nach ueberwiegender Auslegung NICHT. Graubereich, intern als 'begrenztes Risiko Art.50(1)' dokumentieren; Fachanwalt bestaetigen lassen. Siehe Z2.",
    },
    "verifiedOk": verified_ok,
    "plugin_bugs_observed": [],
}

tmp_dir = os.path.dirname(OUT)
os.makedirs(tmp_dir, exist_ok=True)
fd, tmp = tempfile.mkstemp(dir=tmp_dir, suffix=".tmp")
with os.fdopen(fd, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
os.replace(tmp, OUT)
# validate
with open(OUT, "r", encoding="utf-8") as f:
    json.load(f)
print("WROTE", OUT)
print("findings:", len(findings), "| promptAudit:", len(ai_prompt_audit), "| verifiedOk:", len(verified_ok))
