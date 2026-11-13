# 05 – KI-Funktionen und KI-Verordnung (AI Act)

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. KI-Anbieter, Endpunkte, Modelle [T]

| Dienst | Endpunkt/Backend | Modelle | Anbindung |
|---|---|---|---|
| Google Gemini | Firebase AI Logic SDK 17.10.1, `GenerativeBackend.googleAI()` (Gemini Developer API) | `gemini-2.5-flash-lite` (Standard/Free/Lite-Stufe), `gemini-3.1-flash-lite` (Premium-Stufe, Rückblicke, Textverbesserung), Fallback `gemini-flash-lite-latest` | Firebase-Projekt `bestjurnal-a15b9`, App Check Play Integrity; Temperatur 0,3–0,7; maxOutputTokens bis 8192 |
| Groq | `https://api.groq.com/openai/v1/audio/transcriptions` | `whisper-large-v3-turbo`, `verbose_json`, temperature 0 | Bearer-Key aus Remote Config (SEC-001) |
| Lokal | sherpa-onnx 1.12.34 | Whisper base int8 | Offline, Assets |
| Microsoft | `wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1` | Neural-Stimmen je Locale (`TtsVoiceRegistry`) | inoffiziell (DS-003) |

## 2. Was übertragen wird [T]
- **Dashboard** (4 feste Profile + eigene Profile): System-Prompt (aus `strings.xml`) + **alle** Einträge („=== EINTRAG i von N (Datum) === Text“) + bei Aktualisierung bisherige Analyse (Kategorien, Belastungswerte, Zusammenfassung) + bei eigenen Profilen den Nutzer-Prompt; zweiter Aufruf „Re-Ranking“ mit bis zu 6.000 Zeichen Einträgen.
- **Rückblicke**: alle Einträge der Woche (Text + Datum) → Monatsrückblick aus Wochenrückblicken + Eintragsauszügen (150 Zeichen) → Jahresrückblick aus Monatsrückblicken; jeweils zweiter Aufruf für Titel (500 Zeichen).
- **Zusammenfassung/Titel**: ganzer Eintrag inkl. Nachträgen (automatisch beim Speichern).
- **Textverbesserung**: Rohtext (Chunks ≤ 3.500 Zeichen).
- **Metadaten**: Zeitstempel der Einträge, Antwortsprache (Gerätesprache); keine Nutzer-/Installationskennung im Prompt; Firebase überträgt App-Check-Token und Installations-ID.
- Keine Pseudonymisierung/Anonymisierung auf dem Gerät. Ausgaben werden lokal gespeichert (dashboard_db, retrospective_db, Einträge) bis zur Löschung.

## 3. Trainingsnutzung, Region, Speicherdauer [D]/[U]
- Gemini: Für API-Nutzer im EWR gelten die Datenregeln der bezahlten Dienste für alle Dienste – keine Produktverbesserung, keine menschliche Durchsicht zur Verbesserung; Protokollierung zur Missbrauchserkennung zeitlich begrenzt; Speicherung „in jedem Land“; DPA gilt, wenn Google Auftragsverarbeiter ist [D, Terms Stand 28.04.2026]. Ob das Firebase-Projekt ein aktives Cloud-Billing hat (bezahltes Kontingent) und die DPA akzeptiert wurde: [U]. Eine EU-Region ist über Firebase AI Logic mit Gemini-Developer-API-Backend nicht wählbar; nur über das Vertex-AI-Backend (Region z. B. europe-west1) möglich → Empfehlung prüfen (Aufwand ~1 h, andere Preisstruktur) [A].
- Groq: keine Trainingsnutzung; bis zu 30 Tage Speicherung, ZDR aktivierbar; Daten in US-Google-Cloud; SCC verfügbar [D]. Kontoeinstellungen [U].
- Microsoft (Edge-Endpunkt): keine Dokumentation [U].

## 4. System-Prompts – Bewertung [T]
Alle Prompts liegen lokalisiert in `strings.xml` (`ai_prompt_*`, `ai_retro_*`); die deutsche Fassung wurde vollständig gelesen, Übersetzungen enthalten dieselben Schlüssel (Bedeutungsgleichheit stichprobenartig, nicht vollständig geprüft).
- Diagnose-/Therapieverbote: Belastungsanalyse („KEINE medizinischen, therapeutischen oder diagnostischen Ratschläge … verweist auf Fachpersonen“), Selbsterkenntnis („KEINE psychologischen Diagnosen … keine Störungen oder Krankheiten“), Ziele („KEINE Diät-, Medikamenten- oder Therapieempfehlungen“), eigene Profile („KEINE medizinischen, rechtlichen oder finanziellen Diagnosen … verweise auf Fachpersonen“; „FACHBERATUNGS-REGEL – KEINE DIAGNOSEN“). Rückblicke und Zusammenfassung: keine Verbote (erzählend/neutral) – Empfehlung: denselben Satz ergänzen (NIEDRIG).
- Selbstidentifikation als KI: nicht im Prompt, aber durch UI-Kennzeichnung („KI-generiert“-Badge mit Tooltip „von einem KI-System (Google Gemini) erstellt“, „✨ KI-verbessert“, Disclaimer „Keine Therapie – ersetzt keine professionelle Beratung“) erfüllt.
- Manipulation/Abhängigkeit: keine Anweisungen zu Bindung oder emotionaler Beziehung; Tonalität „wohlwollender Spiegel“, „motivierend“. Eigene Profile erlauben freie Aufträge („Führe den Auftrag wörtlich aus … recherchiere und empfehle aktiv“) – Prompt-Injection durch den Nutzer selbst ist möglich (er schadet nur sich selbst); Diagnoseverbot bleibt als Systemregel bestehen. Anbieterseitige Sicherheitsfilter (Gemini Safety Settings) werden mit Standardwerten genutzt (keine Konfiguration im Code).
- Keine Stimm-/Tonfallanalyse; ausschließlich Text.

## 5. Krisen- und Schutzmechanismen [T]/[D]
- `CrisisHelpDialog` (Einstellungen → Krisenhilfe): Notruf 112, TelefonSeelsorge 0800 111 0 111 / 0800 111 0 222 (am Prüftag auf telefonseelsorge.de bestätigt [D]), findahelpline.com; Text weist auf Suizid-/Selbstverletzungsgedanken hin. Nummern in allen 27 Sprachen identisch (für DE korrekt). Nicht enthalten: 116 123 (TelefonSeelsorge) und Nummer gegen Kummer 116 111 (Kinder/Jugendliche) – Empfehlung ergänzen (NIEDRIG, relevant bei 16–17-Jährigen).
- Keine automatische Erkennung von Kriseninhalten in Einträgen; die App reagiert nicht auf Suizidgedanken im Text. Für ein Tagebuch vertretbar; Hinweis in DSE 12b vorhanden. Optional (NIEDRIG): einfacher lokaler Schlüsselwort-Hinweis, der den Krisen-Dialog anbietet – ohne Übermittlung.
- Meldefunktion: siehe BJA-KI-001 (nur in Einstellungen, Google-Login nötig / mailto).

## 6. Medizinprodukt, HWG, Gesundheitsaussagen [T]/[R]
- Store-/App-Texte: keine Aussagen über Erkennung, Linderung, Behandlung oder Vorbeugung von Krankheiten; Begriffe „Stress“, „Belastung“, „Klarheit“, „Muster“, „Selbstreflexion“. Health-Deklarationsvorlage nennt „Stress Management, Mental Wellness“.
- Zweckbestimmung (Art. 2 Nr. 1 MDR): keine medizinische; Prompts verbieten Diagnosen; Disclaimer „kein medizinisches Produkt“ (DSE 12b, AGB 4.1, `ai_output_health_disclaimer`). MDCG 2019-11 (Rev. 1) Entscheidungsbaum: keine Handlung „zum Zweck“ der Diagnose/Therapie → **kein Medizinprodukt** [R].
- HWG: nicht anwendbar (kein Krankheitsbezug). UWG § 5: Store-Beschreibung liegt nicht vor (12); Superlative vermeiden.
- Unbedenklich: „Dein persönliches KI-Tagebuch für mehr Klarheit im Alltag“, „Muster sichtbar machen“. Zu vermeiden: „hilft gegen Stress/Angst/Depression“, „therapeutisch“, „Burn-out vorbeugen“.

## 7. Einordnung nach der KI-Verordnung [R]
1. **Rolle**: Frank Barwandt bringt „Best Journal“ als KI-System (Art. 3 Nr. 1) unter eigenem Namen in Verkehr → **Anbieter** (Art. 3 Nr. 3) des Systems; zugleich **Betreiber** (Art. 3 Nr. 4) der eingebundenen GPAI-Modelle (Gemini, Whisper). Google/Groq sind Anbieter der Modelle.
2. **Art. 50 Abs. 1** (Interaktion mit natürlichen Personen): Die App interagiert nicht als Chatbot; die Ausgaben sind aber an den Nutzer gerichtete, personalisierte Texte. Die Information „KI“ ist offensichtlich und wird zusätzlich vor der ersten Nutzung (Consent-Screen, Gate) und am Output (Badge) erteilt → **erfüllt** [T].
3. **Art. 50 Abs. 2** (synthetischer Text, maschinenlesbar): Dashboard/Rückblicke sind synthetischer Text; Textverbesserung fällt unter die Ausnahme „unterstützende Funktion für Standardbearbeitung“. In-App-Kennzeichnung vorhanden; **beim Teilen/PDF-Export keine maschinenlesbare Markierung** → BJA-KI-002. Übergangsfrist bis 02.12.2026 gilt nur für vor dem 02.08.2026 in Verkehr gebrachte Systeme [D] – die App ist noch nicht auf dem Markt, daher keine Übergangsfrist. Leitlinien und freiwilliger Code of Practice der Kommission existieren [D].
4. **Sichtbarkeit/Zeitpunkt**: Badge bei jedem KI-Output, Tooltip, Disclaimer; Consent vor erster Übermittlung → **erfüllt**; Rückblicke zeigen Badge inline (`AiGeneratedBadgeInline`).
5. **Art. 5 verbotene Praktiken**: keine unterschwelligen/manipulativen Techniken durch die KI; Marketing-Elemente (atmende Buttons, Exit-Intent-Angebot) sind keine KI-Praktiken; keine Ausnutzung von Schwächen → **ausgeschlossen** [R].
6. **Emotionserkennung (Art. 3 Nr. 39)**: Ableitung von Emotionen aus **biometrischen** Daten – findet nicht statt (nur Text; Audio nur Lautstärke-Messung) → Anhang III Nr. 1 lit. c **nicht anwendbar**; Textbasierte Gefühlsauswertung ist keine Emotionserkennung im Sinne der Definition [R].
7. **Hochrisiko**: keine Anhang-III-Kategorie (kein Beschäftigungs-, Kredit-, Bildungs-, Strafverfolgungskontext) → **kein Hochrisiko-System** [R]. Omnibus-Fristen (02.12.2027/02.08.2028) daher nicht relevant.
8. **Art. 4 KI-Kompetenz**: Pflicht (nach Omnibus „vereinfacht“, Wortlaut [U]) – Empfehlung: kurze Dokumentation „KI-Kompetenz des Anbieters“ (Schulungsnachweis, Kenntnis der Modellgrenzen, Prompt-Regeln) in `docs/` (NIEDRIG).
9. **Weitere Pflichten**: keine (kein GPAI-Anbieter, keine Konformitätsbewertung).

## 8. Google Play – KI-Richtlinien
Anwendungsbereich der AI-Generated Content Policy: erfasst Apps, die KI-Inhalte erzeugen; Ausnahmen für reine Zusammenfassung als einziges Feature und Produktivitäts-Apps mit KI-Verbesserung bestehender Funktion. Best Journal erzeugt frei formulierte Analysen/Rückblicke als Kernmerkmal → **erfasst**. Meldefunktion: vorhanden, aber nicht richtlinienkonform platziert/zugänglich (KI-001). Schutz vor unzulässigen Inhalten: Gemini-Standardfilter + Diagnoseverbote; keine eigene Nachfilterung (für ein Tagebuch ohne Fremdinhalte vertretbar). KI-Deklaration in der Play Console: „Ja, erzeugt KI-Inhalte“.
