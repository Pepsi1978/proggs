# Markt UK / USA / Kanada / Australien / Neuseeland — Rechtsraum-Reference

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Wenn die App in einem englischsprachigen Zielmarkt
> ausgespielt wird oder die en-Locale aktiv ist.

## Quellen

- UK ICO `ico.org.uk`, DPA 2018 `legislation.gov.uk`, Ofcom `ofcom.org.uk` (Online Safety Act)
- US FTC `ftc.gov`, CPPA `cppa.ca.gov`, HHS `hhs.gov`, FDA `fda.gov`
- Kanada OPC `priv.gc.ca`, Quebec CAI `cai.gouv.qc.ca`
- Australien OAIC `oaic.gov.au`
- Neuseeland OPC `privacy.org.nz`, Legislation `legislation.govt.nz`

## Vereinigtes Koenigreich (UK)

### Pflichten
- UK-GDPR (1:1-Annaeherung an EU-DSGVO, plus DPA 2018)
- PECR: Tracking-/Cookie-Einwilligung
- Online Safety Act: User-Generated-Content-Pflichten, Reporting, Risk Assessment
- **UK-GDPR Art. 27: UK-Vertreter-Pflicht** bei Datenverarbeitung von UK-Buergern ohne UK-Sitz

### UK-Vertreter-Spezialfall
Siehe `uk-vertreter-pflicht.md` — Standard-Empfehlung dieses Skills ist **Option B (UK ausschliessen)**.

### Sprachanforderung
Englisch ausreichend. UK-Englisch (en-GB) im Store-Listing empfohlen.

### Release-Blocker
- PECR-Consent fehlt
- OSA-Pflichten bei UGC fehlen
- Privacy Policy ohne UK-Bezug
- Kein UK-Vertreter benannt trotz Datenverarbeitung

## USA

### Pflichten
- CCPA / CPRA (California Consumer Privacy Act/Rights Act) — "Do Not Sell"-Pflicht
- Weitere State Privacy Laws: VA, CO, CT, UT, TX, OR, MT, DE, IA, IN, NJ, NH, NE, KY, RI, MN, MD, TN
- COPPA (Kinderdatenschutz, FTC)
- FTC Act §5: Unfair or Deceptive Practices
- Health Breach Notification Rule (HBNR)
- HIPAA (Health Insurance Portability and Accountability Act) — nur bei Covered Entities
- FDA (Medical Device Software)

### Sprachanforderung
Englisch ausreichend.

### Release-Blocker
- "Do Not Sell"-Link fehlt bei CCPA-Anwendbarkeit
- COPPA-Verstoesse bei Kinderzielgruppe
- Health-Claims ohne FDA-Notice
- Deceptive Practices (FTC Act §5)

## Kanada

### Pflichten
- PIPEDA (Personal Information Protection and Electronic Documents Act) — Bundes-Datenschutz
- Quebec Law 25 (Bill 64) — strikteres Provinz-Recht
- DPO/DPA-Pflicht in Quebec
- Cross-Border-Transfer-Disclosure

### Sprachanforderung
- Englisch + **Franzoesisch zwingend fuer Quebec** (Loi 14 / Charta der franzoesischen Sprache)
- Apps die Quebec ansprechen brauchen vollstaendige FR-CA-Lokalisierung

### Release-Blocker
- Quebec-Sprachpflicht (FR) fehlt
- Privacy Officer fehlt (Quebec Law 25)
- Cross-Border-Disclosure fehlt

### Kanada beim Initial-Release (siehe MEMORY)
Frank schliesst Kanada beim Initial-Release oft AUS, weil die Quebec-Pflichten (FR + CAI-Compliance + DPO)
zusaetzliche Arbeit bedeuten. Option A = Initial-Release ohne Kanada.

## Australien

### Pflichten
- Privacy Act 1988 + Australian Privacy Principles (APPs)
- Notifiable Data Breaches scheme
- Cross-Border-Disclosure (APP 8)

### Sprachanforderung
Englisch ausreichend.

### Release-Blocker
- Privacy Policy ohne AU-Bezug
- APP-8-Cross-Border-Hinweis fehlt
- Notifiable Data Breach Plan fehlt

## Neuseeland — Privacy Act 2020

### Pflichten
- 13 Information Privacy Principles (IPPs)
- IPP 1: Zweck der Erhebung
- IPP 3: Informationspflicht bei Erhebung
- IPP 4: Keine Erhebung auf unzulaessige Weise
- IPP 11: Weitergabebeschraenkung
- **IPP 12: Cross-Border-Transfer** — nur erlaubt wenn:
  - (a) Empfaenger NZ Privacy Act unterliegt,
  - (b) vergleichbarer Schutz vorhanden,
  - (c) vertragliche Bindung (OPC Model Contract Clauses), oder
  - (d) ausdrueckliche informierte Einwilligung
- EU gilt als ausreichend schuetzend
- **72h-Breach-Meldepflicht** (OPC-Guidance Mai 2024)
- **Extraterritorialer Anwendungsbereich:** NZ-Bürger als Nutzer reicht — auch ohne NZ-Sitz

### Sprachanforderung
Englisch ausreichend. Keine Pflicht fuer Maori oder andere Sprachen.

### Sanktionen
- Bussgelder: bis NZD 10.000 (Einzelpersonen), bis NZD 50.000 (Organisationen) fuer spezifische Verstoesse
- OPC kann Compliance Notices ausstellen (Anordnungen)
- "Name and shame" durch Privacy Commissioner — tatsaechlich genutzt

### Aktuelle Aenderungen 2024–2026
- Mai 2024: OPC-Leitfaden zur 72h-Breach-Meldefrist praezisiert — Wissen von Mitarbeitern = Wissen des Unternehmens
- Verstaerkte Enforcement-Praxis 2024-2025

### Release-Blocker
- Fehlende Privacy Policy mit NZ-Bezug
- Fehlende 72h-Breach-Meldung-Vorbereitung
- IPP-12-Cross-Border-Grundlage fehlt

### Cross-Border (EU/DE-Anbieter)
EU gilt als ausreichend schuetzend (vergleichbar mit Privacy Act). EU-Anbieter: GDPR-SCCs als
Vertragsgrundlage ausreichend.

### App-spezifische Pflichten
- Breach Notification: Meldung an OPC "so soon as practicable", Ziel 72h, bei ernsten Risiken auch Nutzer informieren
- Keine Pflichtregistrierung
- Extraterritorialitaet: NZ-Bürger als Nutzer genuegt
