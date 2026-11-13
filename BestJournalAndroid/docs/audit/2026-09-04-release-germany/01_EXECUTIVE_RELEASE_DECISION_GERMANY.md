# 01 – Abschlussentscheidung für die Veröffentlichung in Deutschland

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), Version 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, Branch `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## Entscheidung: **C. NOT READY FOR RELEASE**

Vier Release-Blocker sind offen. Jeder ist mit überschaubarem Aufwand (insgesamt ca. 2–3 Arbeitstage plus Hosting-Umzug) zu schließen. Die Grundarchitektur (lokale Speicherung, granularer Consent-Screen, Privacy-Gates, KI-Kennzeichnung, Krisenhilfe, Kontolöschung) ist tragfähig; die Blocker sind konkrete Lücken in der Durchsetzung der Einwilligung und in der Richtigkeit der Texte.

### Release-Blocker (kleinste Maßnahme, Aufwand)

| Nr. | Befund | Kleinste Maßnahme | Aufwand |
|---|---|---|---|
| BJA-DS-001 | Sprachaufnahmen gehen in Testphase/Abo auch ohne Einwilligung an Groq; keine lokale Option (`TranscriptionRepository.kt:65-119`, `JournalScreen.kt:236-266`) | Consent-Prüfung vor dem Groq-Zweig, „Ablehnen“ persistieren, Schalter „Lokal/Cloud“ in den Einstellungen | 3–5 h |
| BJA-DS-002 | Tagebuchtexte gehen bei automatischen KI-Läufen (Zusammenfassung beim Speichern, Auto-Dashboard, Rückblick-Init) ohne Gemini-Einwilligung an Google | Zentrale Consent-Sperre in `FirebaseAiService.generateContent()` + stille Fehlerbehandlung | 4–8 h |
| BJA-WEB-001 | Datenschutz-, Impressum-, AGB- und Kontolösch-URLs liefern 404 (Play-Pflicht) | Statische Seiten auf Hostinger veröffentlichen, URLs in `strings.xml:1475-1486` und Play Console eintragen | 3–6 h |
| BJA-RT-001 | Falsche Tatsachen in Einwilligungs- und Rechtstexten (nicht existente lokale Vorlese-Stimme, Groq „sofort gelöscht“, DPAs mit Microsoft/Groq, Firebase Authentication, „Standard lokal“) | Texte korrigieren (Vorlagen in `13_REQUIRED_CHANGES.md`), Policy-Version erhöhen | 3–5 h |

### Getrennte Bewertung der neun Dimensionen

| Dimension | Ergebnis | Kernbegründung |
|---|---|---|
| 1. Technische Veröffentlichungsfähigkeit | **BESTANDEN mit Auflagen** | Release-AAB baut signiert (151 MB), R8 aktiv, Mapping vorhanden, targetSdk 36, alle 24 nativen Bibliotheken 16-KB-ausgerichtet, kein `debuggable`, kein Klartextverkehr, 34 Unit-Tests grün. Auflagen: AD_ID-Berechtigung (BJA-GP-001), Logging (BJA-SEC-002). |
| 2. Datenschutz-Compliance | **NICHT BESTANDEN** | Einwilligung wird nicht durchgesetzt (DS-001/002), Texte unzutreffend (RT-001), DSFA/DSB fehlen (DS-006), Re-Consent fehlt (DS-004). |
| 3. Alters- und Minderjährigenkonzept | **NICHT BESTANDEN** | „ab 13“ ohne jede Mechanik; Empfehlung Mindestalter 16 mit Bestätigung (ALT-001). |
| 4. Google-Play-Konformität | **NICHT BESTANDEN** | Privacy-URL 404 (WEB-001), Werbe-ID-Deklaration (GP-001), Meldefunktion nicht am Output (KI-001); Kontotyp/Testpflicht/Händlerstatus ungeklärt (GP-005). |
| 5. KI-Transparenz und KI-Sicherheit | **BESTANDEN mit Auflagen** | Badges, Disclaimer, Krisenhilfe, Diagnoseverbote in Prompts vorhanden; offen: Meldefunktion (KI-001), maschinenlesbare Kennzeichnung beim Export (KI-002). Keine Emotionserkennung, kein Hochrisiko-System. |
| 6. Internetseite und öffentliche Rechtstexte | **NICHT BESTANDEN** | Seite tot (404); Website-Datenschutzerklärung für den neuen Hoster fehlt. |
| 7. Internationale Datenübermittlungen | **NICHT BESTANDEN** | Edge-TTS ohne Vertrag (DS-003); Groq-Vertragslage/ZDR ungeklärt (DS-011); Gemini/Drive/Analytics über DPF + SCC plausibel [U]. |
| 8. Medizinprodukt / Gesundheitsversprechen | **BESTANDEN** | Keine Krankheitsbezüge in Store-/App-Texten, Prompts verbieten Diagnosen/Therapie, Disclaimer „kein Medizinprodukt“ vorhanden (DSE 12b). |
| 9. Verbleibendes rechtliches Restrisiko | **MITTEL** | Nach Behebung: c/o-Impressum, Art.-49-Konstruktion (falls TTS bleibt), DSFA-Einordnung, Titel „Best Journal“. Eine Abmahnung ist nie ausgeschlossen. |

### Empfohlenes Mindestalter
**16 Jahre** (Begründung in `06_AGE_AND_MINOR_AUDIT.md`). Play Console: Zielgruppe „16–17“ und „18 und älter“, keine Familienrichtlinie.

### Empfohlene Google-Play-Einstellungen (Kurzfassung, Details in `07_GOOGLE_PLAY_COMPLIANCE.md`)
Produktion nur **Deutschland**; Kategorie „Lifestyle“ (alternativ „Produktivität“); Preis kostenlos mit In-App-Käufen; keine Anzeigen; Werbe-ID: nach Entfernen „Nein“; Health-Deklaration: „Health & wellness – Stress management / mental wellness“, kein Medizinprodukt; KI-Deklaration: erzeugt KI-Inhalte, Meldefunktion vorhanden (nach KI-001); Data Safety gemäß Tabelle in 07; App-Zugriff: Testkonto nicht nötig (kein Login-Zwang), aber Hinweis auf Premium-Testphase; Inhaltsbewertung über IARC-Fragebogen wahrheitsgemäß (keine nutzergenerierten öffentlichen Inhalte, KI-Inhalte ja).

### Priorisierte Änderungen
1. Blocker: DS-001, DS-002, RT-001, WEB-001.
2. HOCH: SEC-001 (Groq-Proxy), DS-003 (TTS), DS-004 (Re-Consent), DS-005 (Umfang), DS-006 (DSFA/DSB), GP-001 (AD_ID), ALT-001 (Alter 16), KI-001 (Melden am Output).
3. MITTEL: VR-001, RT-002, RT-003, SEC-002, SEC-003, GP-002, DS-008, DS-009, KI-002, OSS-001, GP-003, CRA-001.
4. NIEDRIG: TECH-001, SEC-004, DS-010, GP-004, DOC-001, RT-004, SEC-005, A11Y-001.

### Nicht verifizierbare externe Tatsachen (Prüfanweisungen in `12_EXTERNAL_EVIDENCE_REQUIRED.md`)
Kontotyp/Erstellungsdatum/Testpflicht/Händlerangaben des Play-Kontos · Groq DPA/SCC/ZDR/DPF · Google Cloud DPA, Blaze-Plan (bezahltes Gemini-Kontingent) · OAuth-Verifizierung gmail.send · Play-Console-Deklarationen · Store-Assets · Impressumservice-Vertrag (Zustellung) · Firebase-API-Key-Einschränkung · Verfahrensstand EuGH C-703/25 P (Data Privacy Framework) · Stand des Produkthaftungs-Umsetzungsgesetzes.

**Audit-Verzeichnis:** `C:\Users\barwa\proggs\BestJournalAndroid\docs\audit\2026-09-04-release-germany\` – Lesereihenfolge siehe `README.md`.
