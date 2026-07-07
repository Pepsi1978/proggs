# Markt Pakistan + Bangladesch (Suedasien-Teil B) — Rechtsraum-Reference

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Wenn die App in Pakistan (ur-Locale Urdu) oder Bangladesch
> (bn-Locale Bengalisch) ausgespielt wird.

## Pakistan — KEIN aktives Datenschutzgesetz

### Status (zum Skill-Stand dieser Datei)
- PDPB 2023 vom Kabinett genehmigt (04/2023) — parlamentarische Zustimmung ausgeblieben
- PDPB 2025 (ueberarbeiteter Entwurf) — weiterhin nicht verabschiedet
- 12/2025-Analyse: Pakistans Presse bezeichnet Zustand als **"legal vacuum"** fuer Datenschutz
- Kein aktives Enforcement-Regime
- Keine NCPDP (Datenschutzbehoerde) operationell

### Aktuell geltendes Recht (fragmentiert)
- **PECA 2016** (Prevention of Electronic Crimes Act) — primaeres Cyberkriminalitaetsgesetz mit rudimentaeren Datenschutzbestimmungen
- Constitution Art. 14 (Wuerde) — indirekt relevant
- PEMRA fuer Medienkontexte

### Pflichtangaben
**Da kein Datenschutzgesetz gibt es keine gesetzlichen Pflichtangaben** fuer Datenschutzerklaerungen.
Google Play Store eigene Privacy-Policy-Anforderungen dominieren.

### Sprachanforderung
Keine gesetzliche Pflicht. Urdu oder Englisch — Play Store Policy gilt.

### Sanktionen
PECA 2016: bis PKR 3 Mio. / 3 Jahre Haft bei unbefugtem Datenzugang — aber kein spezifisches
Datenschutz-Enforcement wie in GDPR-Laendern.

### Wenn PDPB verabschiedet wird (ungewisser Zeitpunkt)
- NCPDP als unabhaengige Behoerde
- Detaillierte Notifikationspflichten
- Bussgelder: geplant max. "einige Millionen Dollar" (Kritiker: signifikant unter intl. Standards)
- Cross-Border-Transfer: Whitelist-System geplant

### Cross-Border-Transfer
Aktuell keine gesetzliche Regelung. PDPB-Entwurf: Whitelist-System.

### Risiko fuer DE-Anbieter
**Sehr gering** (kein Enforcement-Regime). Hauptrisiko: PECA-Verstoesse bei aktiven
Datenschutzverletzungen oder Cyberangriffen.

### App-spezifische Pflichten
Keine. Google Play Store eigene Anforderungen dominant.

### Im Skill-Bericht IMMER explizit notieren
> "Pakistan hat aktuell KEIN aktives Datenschutzgesetz ('legal vacuum'). Nur Google
> Play Store Policy + PECA 2016 als Cyberkriminalitaetsgesetz. Bei zukuenftiger PDPB-
> Verabschiedung: Skill neu pruefen."

### Quellen
- ICLG Pakistan Data Protection 2025-2026
- ITIF Pakistan Cross-Border 05/2025
- The Friday Times 12/2025

## Bangladesch — PDPO 2025 (Ord. 61/2025)

### Status
**Personal Data Protection Ordinance 2025 (PDPO) — in Kraft.** Veroeffentlicht im Government
Gazette am **16.11.2025** (Ordinance No. 61/2025). **Uebergangszeit: 18 Monate** ab Gazetierung
fuer die meisten Kernpflichten — vollstaendige Wirksamkeit ~05/2027.

### Pflichtangaben Datenschutzerklaerung
- Identitaet + Kontaktdaten des Datenverantwortlichen ("Data Fiduciary")
- Zwecke der Verarbeitung
- Kategorien personenbezogener Daten
- Rechte der Betroffenen (Zugang, Portabilitaet, Berichtigung, Loeschung, Widerruf)
- Aufbewahrungsdauern
- Cross-Border-Transfer-Informationen
- Kontakt des **Chief Data Officer (CDO)** bei "Significant Data Fiduciaries"
- Informationen zu automatisierten Entscheidungen (falls eingesetzt)

### Sprachanforderung
Ordinanz primaer auf Bangla — englische Uebersetzung optional, bei Konflikt gilt Bangla-Text.
Keine explizite Pflicht zur Bangla-DSE, aber behoerdliche Kommunikation auf Bangla erwartet.
**Empfehlung: Bangla-Version fuer Nutzer-Kommunikation.**

### Sanktionen
- Allgemeine Verstoesse: **1–2% des Jahresumsatzes** in Bangladesch
- "Significant Data Fiduciaries": **2–5% des Jahresumsatzes**
- Strafrecht: bis 7 Jahre Haft + Geldstrafe bis BDT 20 Lakh (~17.000 USD)
- Regulierungsbehoerde: **National Data Management Authority (NDGA)** — Registrierung, Audits, Bussgelder, Aussetzung von Cross-Border-Transfers

### Aktuelle Aenderungen
- **Cyber Security Act 2023** durch **Cyber Security Ordinance 2025** ersetzt (Gazette 21.05.2025)
- **PDPO 2025** als erste umfassende Datenschutzgesetzgebung Bangladeschs
- Daten als Eigentum der Buerger (nicht des Staates) — konzeptioneller Paradigmenwechsel
- Grosse Tech-Unternehmen unterliegen bangladeschischen Gerichten (lokale Jurisdiktion)

### Cross-Border-Transfer
- **Restricted Personal Data und Critical Information Infrastructure-Daten:** mindestens eine
  synchronisierte Echtzeit-Kopie MUSS in Bangladesch vorgehalten werden (Datenlokalisierung!)
- Allgemeiner Transfer ins Ausland: nur wenn Empfaengerland/Organisation gleichwertigen Schutz garantiert
- Fuer DE-Anbieter: Adequacy noch nicht festgestellt — SCCs oder Einwilligung als Interim-Basis

### App-spezifische Pflichten
- **Kinder (unter 18):** Verifiable Parental Consent verpflichtend; Tracking, Profiling,
  Monitoring, gezielte Werbung an Minderjaehrige **verboten**
- **Gesundheitsdaten:** sensible Daten — explizite separate Einwilligung
- **KI/Automated Decisions:** in DSE adressieren
- **Account-Loeschung:** Loeschrecht implementieren (nicht-verzichtbares Recht)
- **CDO-Pflicht:** fuer "Significant Data Fiduciaries" (Schwellenwerte durch NDGA festzulegen)
- **Breach Notification:** unverzueglich an Behoerde und Betroffene
- **Consent:** explizit, informiert, widerrufbar — kein Pre-checked-Feld

### Release-Blocker
- Keine BD-Privacy-Policy in Bangla
- Cross-Border ohne adaequate Grundlage bei sensiblen Daten
- Account-Loeschung fehlt
- Bei Significant Data Fiduciary: kein CDO

### Quellen
- The Daily Star — PDPO 2025 Key Takeaways
- Jural Acuity — Bangladesh PDPO
- DPO India Mirror — Bangladesh PDPO Full Text

## Vergleichsmatrix SA-Pakistan-Bangladesch

| Land | Rechtsstand | Dringlichkeit | Sprache Pflicht? | Datenlokalisierung |
|------|-------------|---------------|-------------------|---------------------|
| Pakistan | **KEIN aktives DPG** | NIEDRIG | Keine | Nein |
| Bangladesch | PDPO 2025, 18M Uebergang | MITTEL-HOCH | Bangla empfohlen | **Ja** (Restricted + CII) |
