# 06 – Altersfreigabe und Minderjährige

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. Bestandsaufnahme [T]
| Ort | Angabe |
|---|---|
| DSE Abschnitt 10 | „ab 13 Jahren“; unter 16 Elternzustimmung für Anmeldung/Cloud (Groq, Drive, Gemini); „wissentlich keine Daten von Kindern unter 13“ |
| AGB Abschnitt 3 | „ab 13 Jahren“; 13–16 Elternzustimmung bei Anmeldung/Cloud; „Mit der Nutzung versicherst du das Mindestalter“ |
| App (Onboarding, Consent) | **keine** Altersabfrage, keine Bestätigung, keine Elternzustimmung (`OnboardingScreen.kt` ohne Alters-Strings; `strings.xml` ohne Alters-Schlüssel) |
| Play-Vorlagen | `data-safety-checklist.md`: „Zielgruppe 13+ (COPPA)“, „PEGI 12/ESRB Teen“ |
| Konsistenz | Texte untereinander konsistent (13/16), App setzt nichts davon um |

## 2. Getrennte Bewertung
1. **Datenschutzrecht (Art. 8 DSGVO)**: Die Cloud-Funktionen sind Dienste der Informationsgesellschaft auf Einwilligungsbasis; in Deutschland gilt 16 Jahre (keine nationale Absenkung). Unter 16 nur mit Elternzustimmung und „angemessenen Anstrengungen“ zur Prüfung (Abs. 2). Ohne Mechanik ist die Einwilligung 13–15-Jähriger unwirksam. Da die App als Kernnutzen KI-Analysen von Art.-9-nahen Inhalten anbietet, ist eine rein lokale Nutzung durch Jüngere zwar möglich, aber praktisch nicht das Produkt.
2. **Vertragsrecht**: Abo/Lifetime über Google Play; Minderjährige brauchen Zustimmung der Eltern (§§ 107, 108 BGB); Google Play prüft dies über Familienkonten. Der kostenlose Nutzungsvertrag (§ 327 Abs. 3 bei Opt-in-Analytics) ist für Minderjährige rechtlich neutral („lediglich rechtlich vorteilhaft“ ist umstritten, wenn Daten Gegenleistung sind) [R].
3. **Jugendschutz**: Inhalte sind selbst verfasst; KI-Ausgaben sind reflektierend, nicht jugendgefährdend; kein UGC-Austausch, keine Kontaktaufnahme → JuSchG/JMStV ohne besondere Pflichten; IARC-Fragebogen ehrlich beantworten (voraussichtlich USK 12 oder 16 wegen „reifer Themen möglich“).
4. **Google-Play-Richtlinien**: Zielgruppe wählbar; bei Einschluss von 13–15 wird die App „für Kinder und Erwachsene“ und die Familienrichtlinie greift teilweise (Daten, Werbung, Analytics-Anforderungen). Bei „16–17“ + „18+“ keine Familienrichtlinie. Altersprüfungssignale/Age-Assurance-Pflichten für Deutschland: am Prüftag keine spezifische Play-Anforderung verifiziert [U].
5. **Inhaltliche Eignung**: KI-Selbsterkenntnis-Analysen („verborgene Muster, unbewusste Überzeugungen“) und Belastungsanalysen sind für 13–15-Jährige ohne Begleitung ungeeignet; Krisenhilfe ist vorhanden, aber nur mit Erwachsenen-Nummern (Nummer gegen Kummer fehlt).
6. **Technische Schutzmaßnahmen**: keine; Biometrie-Sperre optional.

## 3. Ergebnis
- **Empfohlenes Mindestalter: 16 Jahre.** Begründung: Einwilligungsbasierte Art.-9-nahe KI-Verarbeitung im Drittland; keine verhältnismäßige Elternzustimmungs-Mechanik verfügbar; Play-Familienrichtlinie vermieden; inhaltliche Eignung.
- **Vertretbare Alternative**: 13+ nur mit (a) verifizierbarer Elternzustimmung (z. B. E-Mail-Bestätigung eines Elternteils + Sperre der Cloud-Funktionen bis dahin), (b) Familienrichtlinien-Konformität in der Play Console, (c) Nummer gegen Kummer im Krisen-Dialog. Aufwand hoch, Nutzen gering → nicht empfohlen.
- **18+** ist nicht erforderlich; die pauschale Anhebung würde 16–17-Jährige ohne Grund ausschließen.
- **Bedingungen für 16+**: Onboarding-Bestätigung „Ich bin mindestens 16 Jahre alt“ (Checkbox, nicht speichernde Selbstauskunft; verhältnismäßig, keine Ausweisprüfung); DSE 10 und AGB 3 auf 16 anpassen; Krisen-Dialog um 116 111 (Nummer gegen Kummer, Mo–Sa 14–20 Uhr) und 116 123 ergänzen; Play Console: Zielgruppe „16–17“ und „18 und älter“, „nicht an Kinder gerichtet“; IARC ehrlich.
- **Formulierungsvorschläge**: siehe 13_REQUIRED_CHANGES.md (ALT-001).
- **Verbleibende Unsicherheit**: Selbstauskunft ist nicht fälschungssicher (allgemein akzeptiert bei 16+ ohne besondere Risikoinhalte); Play-Prüfer könnten wegen „Stimmung/Mental Wellness“ Rückfragen stellen (Health-Deklaration ehrlich ausfüllen).
- Diese Altersbewertung ist in allen Berichten identisch: **16+**.
