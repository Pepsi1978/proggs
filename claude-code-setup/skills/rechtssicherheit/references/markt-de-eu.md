# Markt DE/EU — Rechtsraum-Reference

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Wenn die App in Deutschland oder einem EU-Mitgliedstaat
> ausgespielt wird (das ist fast immer der Fall). Diese Datei deckt auch die EU-weiten
> Verordnungen ab, die in allen Mitgliedstaaten direkt gelten.

## Stand: 2026-05-17 — Was hat sich seit dem letzten Skill-Stand (2026-04-28) geaendert

| Aenderung | Inkrafttreten | Wirkung |
|--|--|--|
| **EU AI Act Art. 50 Transparenzpflichten** | 02.08.2026 (verbindlich) | Chatbot/Deepfake-Kennzeichnung + maschinenlesbare Markierung von GenAI-Output. Details: `ai-act-art-50.md` |
| **EU Data Act** | 12.09.2025 (anwendbar) | Datenzugang/Portabilitaet, Bussgelder bis 7% Jahresumsatz |
| **BFSG/EAA KMU-Schwelle praezisiert** | 28.06.2025 | KMU-Ausnahme: <10 MA UND <2 Mio. EUR Jahresumsatz |
| **TDDDG PIMS-Verordnung** | 01.04.2025 | Anerkannte Consent-Management-Dienste |
| **DDG ersetzt TMG** | 14.05.2024 | §5 DDG statt §5 TMG (Apps die noch TMG zitieren = veraltet) |
| **EU-US DPF EuG-Urteil** | 03.09.2025 | Erste Klage abgewiesen, EuGH-Berufung anhaengig; Schrems-III-Risiko |

## Pflicht-Quellen

| Quelle | Wozu |
|--|--|
| `dsgvo-gesetz.de` | DSGVO-Volltext |
| `gesetze-im-internet.de` | DDG, BGB, TDDDG, BFSG, EGBGB |
| `edpb.europa.eu` | EDPB-Guidelines + Decisions |
| `ec.europa.eu` | EU-Kommission, DSA, AI Act, Data Act |
| `artificialintelligenceact.eu` | AI-Act-Artikel inkl. Art. 50 |
| `datenschutzkonferenz-online.de` | DSK-Beschluesse |
| `bundesfachstelle-barrierefreiheit.de` | BFSG-Praxisleitfaden |

## Pflicht-Gesetze und ihre Pflichten

### DSGVO (EU-Verordnung 2016/679)

- Art. 5: Grundsaetze (Rechtmaessigkeit, Zweckbindung, Datenminimierung, Speicherbegrenzung)
- Art. 6/9: Rechtsgrundlagen + sensible Daten
- Art. 7: Einwilligung — Widerruf so einfach wie Zustimmung
- Art. 13/14: Informationspflichten bei Erhebung
- Art. 17: Recht auf Loeschung
- Art. 28: Auftragsverarbeitung (AVV-Pflicht)
- Art. 30: VVT/ROPA
- Art. 32: TOMs
- Art. 33/34: Datenpannen-Meldung (72h)
- Art. 35: DSFA bei Hochrisiko
- Art. 44 ff.: Drittlandtransfer (SCCs, DPF, BCR)
- Bussgelder: bis 20 Mio. EUR oder 4% Jahresumsatz

### DDG (Digitale-Dienste-Gesetz, ersetzt TMG seit 14.05.2024)

- **§5 DDG** Anbieterkennzeichnung — ersetzt §5 TMG
- Apps oder Texte die noch "§5 TMG" zitieren sind **veraltet** und MUESSEN aktualisiert werden
- Inhalte unveraendert: Name, Adresse, Kontakt, Register, USt-ID

### TDDDG + PIMS-Verordnung

- §25 TDDDG: Zugriff auf Endgeraete-Speicher (Cookies, Local Storage, Tracking) nur mit Einwilligung
- PIMS-Verordnung seit 01.04.2025: Anerkannte Consent-Management-Dienste
- Gilt explizit auch fuer mobile Apps
- Bussgelder bis 300.000 EUR

### BGB / EGBGB Widerruf (digitale Produkte)

- BGB §355 ff.: Widerrufsrecht 14 Tage
- BGB §356 Abs. 5: Erloeschen bei digitalen Inhalten nur mit ausdruecklicher Zustimmung + Kenntnis
- BGB §312k: Kuendigungsbutton-Pflicht (so einfach wie der Vertragsabschluss)
- EGBGB Art. 246a: Pflichtinformationen vor Vertragsschluss
- Muster-Widerrufsformular bei Pflichtangaben

### BFSG / EAA (Barrierefreiheit, seit 28.06.2025)

- Umsetzung EU-Richtlinie 2019/882
- Betrifft B2C-Apps (nicht B2B)
- Pflichtstandard: WCAG 2.1 Level AA via EN 301 549
- Bussgelder bis 100.000 EUR + Abmahn-/Verbandsklage-Risiko + im Extremfall Verkaufsverbot
- **KMU-Ausnahme:** <10 MA UND <2 Mio. EUR Jahresumsatz
- Erste Enforcement-Massnahmen ab Herbst 2025

### DSA (Digital Services Act)

- Hosting-/UGC-/Marketplace-Pflichten
- Art. 11: Single Point of Contact (DSA-Beschwerde-/Kontaktstelle) — auch ohne UGC fuer Vermittlungsdienste
- Art. 16: Reporting-Mechanismus bei UGC
- Harmonisierte Berichte ab 01.07.2025
- Enforcement: X (Twitter) 120 Mio. EUR Bussgeld als erstes DSA-Bussgeld

### EU Data Act (anwendbar seit 12.09.2025)

- Datenzugang/Portabilitaet bei Connected Devices und Diensten
- Mechanismen zur Datenweitergabe an Dritte auf Nutzeranfrage
- Export in standardisierten Formaten
- Bussgelder bis 7% globaler Jahresumsatz
- Fuer Standard-Apps mit lokalen Daten begrenzt relevant, aber Datenzugang-UX pruefen

### AI Act

- GPAI-Pflichten seit 02.08.2025
- **Art. 50 Transparenzpflichten ab 02.08.2026** — siehe `ai-act-art-50.md`
- Hochrisiko-KI-Pflichten ab 2026/2027 (App-Chatbots koennen darunter fallen)

### EU-US DPF (Data Privacy Framework)

- EuG 03.09.2025: Erste Klage abgewiesen → DPF derzeit gueltig
- EuGH-Berufung anhaengig (eingereicht 10/2025) — "Schrems-III"-Risiko
- Fuer App-Entwickler: Firebase/OpenAI/Anthropic stuetzen sich auf DPF
- **Empfehlung:** SCCs als Backup-Mechanismus vorhalten

## Release-Blocker fuer DE/EU

- Fehlendes Impressum trotz geschaeftsmaessigem Angebot
- Privacy Policy sagt "keine Daten", aber App nutzt Analytics/Ads/Crash/Cloud
- Fehlende Cookie-/Tracking-Einwilligung
- Kein Widerruf bei IAP/Abos
- BFSG-Verstoesse (sofern nicht KMU-Ausnahme)
- DSA-Kontaktstelle fehlt
- AI-Act-Art.-50-Hinweis fehlt (ab 02.08.2026)

## Enforcement-Trends DE/EU

Siehe `enforcement-trends.md` fuer aktuelle Abmahn-Hotspots, Cookie-Consent-Urteile,
Data-Safety-Falschangaben, KI-Risiken, BFSG-Klagen.
