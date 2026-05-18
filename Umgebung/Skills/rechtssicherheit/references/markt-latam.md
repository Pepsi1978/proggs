# Markt LATAM — Rechtsraum-Reference

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Wenn die App in Brasilien (pt-BR), Mexiko oder Argentinien
> (beide es) ausgespielt wird.

## Brasilien — LGPD / ANPD

### Pflichten
- Rechtsgrundlagen-Disclosure
- ANPD-Registrierung
- Cross-Border-Transfer-Disclosure
- Data Protection Officer (DPO) ab gewisser Groesse
- Sensible Daten: erhoehte Anforderungen

### Sprachanforderung
Portugiesisch (pt-BR) zwingend

### Sanktionen
- Bis 2% Jahresumsatz (max BRL 50 Mio. pro Verstoss)
- ANPD aktiv im Enforcement

### Cross-Border (EU/DE)
SCCs oder ANPD-Whitelist

### Quelle
`gov.br/anpd`

## Mexiko — LFPDPPP-Reform 21.03.2025

### Status — GROSSE REFORM
**Komplett neue Fassung in Kraft seit 21.03.2025** — groesste Reform seit 2010.
- INAI **aufgeloest**
- **ACGG** (Ministerio de Anticorrupcion y Buena Gobernanza) uebernimmt
- Data Processor (App-Entwickler) jetzt **direkt haftbar** (vorher nur Verantwortliche)
- KI/Automated Decision Making: explizite Offenlegungspflicht (NEU)
- Consent-Anforderungen verschaerft: granulare Zweck-Trennung

### Pflichtangaben — Aviso de Privacidad (3 Formate gesetzlich)

| Format | Wann | Inhalt |
|--|--|--|
| **Vollstaendiger Aviso** | Default | Alle Pflichtangaben: Identitaet, Kontaktdaten, Datenkategorien, Zwecke (konsentpflichtig getrennt von nicht-konsentpflichtigen), sensible Daten ausgewiesen, ARCO-Rechte, Cross-Border-Hinweis, Aenderungsverfahren |
| **Vereinfachter Aviso (Pflicht fuer Apps)** | Bei elektronischer Datenerhebung (App-Screens) | Name des Verantwortlichen, Zwecke, Hinweis auf Vollversion, Opt-out-Mechanismus |
| **Kurz-Aviso** | Sehr platzbeschraenkte Faelle | Minimal |

### Neu 2025
Automatisierte Entscheidungen (KI) MUESSEN im Aviso ausgewiesen werden.

### Sprachanforderung
**Spanisch zwingend.** Klare, einfache Sprache ohne Fachjargon. Mehrsprachige Versionen zusaetzlich erlaubt.

### Sanktionen
- 100–320.000 UMA (~USD 1.200–3.857.000)
- Verdopplung bei sensiblen Daten
- Strafrecht: 3 Monate–3 Jahre Haft fuer vorsaetzliche Sicherheitsverstoesse
- Enforcement: **ACGG** seit 03/2025 (loest INAI ab)

### Cross-Border (EU/DE-Anbieter)
- Einwilligung erforderlich oder legitimer Ausnahmegrund
- Kein formales Adequacy-System
- EU-Anbieter: GDPR-Compliance + dokumentierte Einwilligung im Aviso ausreichend
- Keine Vorab-Genehmigung beim ACGG

### App-spezifische Pflichten
- **Vereinfachter Aviso bei App-Installation Pflicht**
- Nutzer muessen Aviso bestaetigen koennen
- Sensible Daten (Gesundheit, Biometrie, Finanzen): explizite Einwilligung zwingend
- Keine Pflichtregistrierung

### Release-Blocker
- Kein Spanisch-Aviso
- Kein Simplified Aviso in App
- ACGG-Aenderungen nicht beruecksichtigt
- KI nicht im Aviso ausgewiesen

### Quellen
- White & Case Mexico New Data Protection 2025
- Greenberg Traurig Mexico LFPDPPP Reform
- IAPP Mexico New Authority
- ICLG Mexico Data Protection 2025-2026

## Argentinien — Ley 25.326

### Status
Aktuell **Ley 25.326** mit Reformprozess 2024/2025. Reform-Bills 644-S-2025 + 1948-D-2025 im Kongress.

### Pflichtangaben Privacy Policy
- Identitaet + Adresse Verantwortlicher (Argentinisches Recht fordert physische Praesenz oder Vertreter)
- Zweck der Datenerhebung + -verarbeitung
- ARCO-Rechte (Zugang, Berichtigung, Loeschung, Widerspruch)
- Hinweis auf Weitergabe an Dritte
- Cross-Border-Hinweis
- Einwilligungstext fuer sensible Daten

### Sprachanforderung
Spanisch de facto Pflicht (Vollstreckbarkeit setzt Spanisch voraus)

### Sanktionen
- Ley 25.326: bis ARS 3 Mio. (~USD 3.000 — inflationsbedingt de facto gering)
- **06/2024:** AAIP-Resolution modernisiert Sanktionsrahmen, Strafen gestaffelt
- Reform-Bills 2025: GDPR-Niveau geplant — bis 2% Umsatz oder ARS 50 Mio.

### Aenderungen 2024–2026
- **06/2024:** Neue AAIP-Sanktionsverordnung
- 2025: Drei Reform-Bills im Kongress — Biometrie, Automated Decision Making, DPIA-Pflicht, DPO optional
- **Kein neues Gesetz verabschiedet** (zum Skill-Stand dieser Datei) — Ley 25.326 weiter in Kraft
- **AAIP 2023:** Neue SCCs (Standard Contractual Clauses) fuer Drittstaaten-Transfers eingefuehrt

### Cross-Border (EU/DE-Anbieter) — WICHTIG
**Adequacy-Liste (AAIP Resolution 60-E/2016):**
- **EU/EWR** ist anerkannt — **Transfer ohne Zusatzmassnahmen moeglich**
- Schweiz, Kanada (privat), Neuseeland, Israel, Uruguay, Andorra, Guernsey, Jersey, Isle of Man, Faeroeer

Fuer Nicht-Listen-Laender: AAIP-SCCs (2023 aktualisiert) oder BCRs.

**EU-Anbieter:** Argentinien hat GDPR-Adequacy-Status — EU↔AR-Transfers ohne SCCs moeglich.
- Keine Vorab-Genehmigung fuer SCCs mehr noetig (seit 2023)

### App-spezifische Pflichten
- Datenbank-Registrierungspflicht (aktuell, soll mit Reform abgeschafft werden)
- Einwilligung dokumentiert + widerruflich
- Sensible Daten: schriftliche Einwilligung

### Quellen
- AAIP Argentina Cross-Border Transfer
- IAPP Argentina AAIP SCCs 2023
- DLA Piper Argentina
- ITIF Argentina Cross-Border 02/2025

## Vergleichsmatrix LATAM

| Land | Rechtsstand | Dringlichkeit | Sprache Pflicht? | Cross-Border-Basis DE |
|------|-------------|---------------|-------------------|-----------------------|
| Brasilien | LGPD aktiv | HOCH | PT-BR | SCCs / ANPD |
| Mexiko | LFPDPPP-Reform 03/2025 | **HOCH** | ES Pflicht | Einwilligung + GDPR-Compliance |
| Argentinien | Ley 25.326 + Reform | MITTEL | ES de facto | **EU adaequat (kein SCCs noetig)** |
