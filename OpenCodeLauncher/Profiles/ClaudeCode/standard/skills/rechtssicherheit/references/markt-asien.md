# Markt Asien — Rechtsraum-Reference

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.

> **Wann diese Datei lesen:** Wenn die App in einem asiatischen Markt ausgespielt wird
> (Indien, Japan, Korea, Taiwan, Hongkong, Singapore, Thailand, Indonesien, Vietnam, Sri Lanka)
> oder eine entsprechende Locale aktiv ist (hi, ja, ko, zh-Hant, th, id, bn, te, mr, ta, ur, gu, kn, ml).

> **Hinweis zu China:** China (PIPL, zh-Hans) ist in dieser Programmierumgebung BEWUSST AUSGESCHLOSSEN.
> Die Sektion bleibt nur als Referenz fuer eventuell spaetere Faelle.

## Inhalt

1. [Indien — DPDP Act + Rules 2025](#indien-dpdp-act-rules-2025)
2. [Japan — APPI](#japan-appi)
3. [Korea (Suedkorea) — PIPA](#korea-suedkorea-pipa)
4. [Taiwan — PDPA + Novelle 11/2025](#taiwan-pdpa-novelle-112025)
5. [Hongkong — PDPO](#hongkong-pdpo-inkl-doxxing-reform)
6. [Singapore — PDPA](#singapore-pdpa)
7. [Thailand — PDPA 2019](#thailand-pdpa-2019)
8. [Indonesien — UU PDP 2022](#indonesien-uu-pdp-law-27-of-2022)
9. [Vietnam — PDPL 2026](#vietnam-pdpl-law-912025qh15)
10. [Sri Lanka — PDPA 2022](#sri-lanka-pdpa-no-9-of-2022)
11. [Vergleichsmatrix](#vergleichsmatrix-asien)

## Indien — DPDP Act + Rules 2025

### Pflichten
- Notice-Pflichten bei Erhebung
- Consent-Manager-Pflicht (registriert)
- Data Fiduciary / Significant Data Fiduciary
- DPB (Data Protection Board of India)
- Children: explizite Eltern-Einwilligung

### Sprachanforderung
Englisch + lokale Sprache empfohlen (Hindi, Tamil, Telugu, etc.)

### Quelle
`meity.gov.in`

## Japan — APPI

### Pflichten
- Privacy Policy Pflicht
- Cross-Border-Transfer-Disclosure (Sec. 24)
- Sensitive-Daten: Consent zwingend
- PPC (Personal Information Protection Commission)

### Sprachanforderung
Japanisch empfohlen

### Quelle
`ppc.go.jp`

## Korea (Suedkorea) — PIPA

### Pflichten
- Strikte Consent-Pflichten (granular, separat pro Zweck)
- Notification-Pflichten (Breach 24h)
- DPO (Data Protection Officer) ab bestimmter Groesse Pflicht
- PIPC (Personal Information Protection Commission)

### Sprachanforderung
Koreanisch empfohlen

### Quelle
`pipc.go.kr`

## Taiwan — PDPA + Novelle 11/2025

### Status
Aktiv. Novelle November 2025 offiziell verkuendet — neue Pflichten:
- Breach Notification (neu)
- DPO fuer Behoerden (neu)
- Erweiterte Inspektionsrechte der PDPC
- **Personal Data Protection Commission (PDPC):** Vorbereitungsbuero aktiv, noch nicht offiziell gegruendet
  (zum Skill-Stand dieser Datei — urspruenglich 08/2025 geplant, verzoegert)
- Bis PDPC operativ: Branchenbehoerden zustaendig (NCC fuer Apps/Telekom)

### Pflichtangaben
- Vor Erhebung informieren ueber: Zweck, Datenkategorien, Nutzungszeitraum + -gebiet, Empfaenger, Rechte
- Rechtsgrundlage nennen (8 zulaessige Rechtsgrundlagen)
- Einwilligung informiert + ausdruecklich; sensible Daten schriftlich

### Sprachanforderung
Keine gesetzliche Chinesisch-Pflicht, aber Verstaendlichkeit. De facto Chinesisch (Traditionell) erwartet.

### Sanktionen
- Datensicherheitsverstoesse: NT$200.000–15.000.000 (~6.000–450.000 EUR)
- Direktmarketing: bis NT$200.000
- Strafrechtlich: bis 2 Jahre Haft

### Cross-Border (EU/DE-Anbieter)
- Restriktionen nur bei: nationale Interessen, Laender ohne ausreichenden Schutz, Umgehungsversuche
- Kein Prior-Approval-System, keine Whitelist
- EU-SCCs als Vertragsgrundlage praktisch ausreichend

### Quellen
- `pcpd.org.hk` (HK)
- Jones Day Taiwan PDPA Amendments 12/2025
- K&L Gates Taiwan PDPA New Developments 01/2026

## Hongkong — PDPO (inkl. Doxxing-Reform)

### Pflichten
- Personal Information Collection Statement (PICS) **vor oder bei Datenerhebung**
- PICS-Inhalt: Zweck, Empfaengerklassen, Auskunfts-/Korrekturrecht, Kontaktdaten
- 6 Data Protection Principles (DPP)
- **Doxxing-Reform 2021 (Sec. 26K-26N)** — bis HKD 1.000.000 + 5 Jahre Haft bei UGC-Doxxing

### Sprachanforderung
Keine gesetzliche Pflicht fuer Chinesisch. Englisch und/oder Chinesisch ueblich.

### Sanktionen
- Verstoss gegen Enforcement Notice: HKD 50.000–100.000 + 2 Jahre Haft
- Kleinere Verstoesse: bis HKD 10.000
- **Doxxing-Verstoss: bis HKD 1.000.000 + 5 Jahre Haft**

### Aenderungen 2024–2026
- 11/2024: GBA Standard Contract (Greater Bay Area) auf alle Sektoren ausgeweitet — nur relevant bei Mainland-China-Transfer
- Geplante Reform (Breach Notification, Data Processor-Haftung) fuer 2026 — noch nicht in Kraft

### Cross-Border (EU/DE-Anbieter)
- **Sec. 33 PDPO ist seit 1995 nie in Kraft getreten** — kein formales Transferverbot
- PCPD empfiehlt Model Contractual Clauses (MCC, 2022)
- EU-GDPR-SCCs decken HK ab (kein Zusatzaufwand)

### App-spezifische Pflichten
- PICS bei App-Installation oder erster Erhebung
- Opt-out fuer Marketing
- Keine Pflichtregistrierung

### Quelle
`pcpd.org.hk`

## Singapore — PDPA

### Pflichten
- Data Protection Officer (DPO) Pflicht
- DNC-Register (Do Not Call)
- Consent-Pflichten (Opt-In)
- PDPC (Personal Data Protection Commission)

### Sprachanforderung
Englisch ausreichend

### Quelle
`pdpc.gov.sg`

## Thailand — PDPA 2019

### Status
Voll aktiv seit 01.06.2022. Erste Grossbussgelder 08/2025 (THB 21,5 Mio. gesamt).

### Pflichtangaben
- Zwecke der Erhebung (vor oder bei Erhebung)
- Empfaenger-Kategorien
- Kontaktdaten Verantwortlicher
- Rechte der Betroffenen
- Rechtsgrundlage pro Zweck
- Bei sensiblen Daten (Health, biometrisch): explizite Einwilligung SEPARAT

### Consent-Vorgaben
- Schriftlich oder elektronisch, **eindeutige aktive Handlung**
- **Separates Consent pro Zweck** — kein Buendel-Consent
- Nicht an Vertrag koppeln
- Widerruf jederzeit, genauso einfach wie Einwilligung
- **Sensitive Data (Sec. 26):** Explicit Consent zwingend, keine Alternativen

### Sprachanforderung
Keine formelle Pflicht, "klar und leicht verstaendlich" — de facto Thai empfohlen

### Sanktionen
- Bis **THB 5 Mio.** (~130.000 EUR) administrativ
- Strafrecht: Freiheitsstrafe + Bussgeld fuer Geschaeftsfuehrer persoenlich
- Schadensersatz bis zum **Doppelten** des tatsaechlichen Schadens
- **08/2025:** 8 Bussgelder gesamt THB 21,5 Mio., hoechste THB 7 Mio.

### Aenderungen 2024–2026
- 03/2024: Cross-Border-Verordnungen (Whitelist-Notification + BCR/Safeguards)
- 08/2025: Erste Grossbussgeld-Welle (Ende Grace-Period)
- Noch keine Laender auf Whitelist

### Cross-Border (EU/DE-Anbieter)
4 Wege: (1) Adequacy-Entscheidung PDPC, (2) BCR (PDPC-Genehmigung), (3) SCCs (ASEAN oder PDPC), (4) Einwilligung
- EU/DE: ASEAN Model Contractual Clauses oder PDPC-SCCs verwenden

### App-spezifische Pflichten
- Loesch-/Vernichtungs-/Anonymisierungsanfragen nach 2024er Sub-Regulations
- Sec. 26 Sensitive Data: separate Consent-Box
- Kein dedizierter Kinder-Schutz im PDPA selbst

### Quelle
DLA Piper Thailand, Tilleke & Gibbins

## Indonesien — UU PDP / Law 27 of 2022

### Status
Voll seit **17.10.2024** (Uebergang abgelaufen). Implementing Regulations (RPP PDP) noch ausstehend
(Justizministerium-Harmonisierung). BP3DP (eigene Behoerde) noch nicht etabliert — derzeit MOCD aufsicht.

### Pflichtangaben
- Identitaet Verantwortlicher
- Rechtsgrundlage
- Verarbeitungszwecke
- Datenkategorien
- DPO-Kontaktdaten (falls ernannt)
- "Klar, zugaenglich, leicht verstaendlich"

### Sprachanforderung
**Bahasa Indonesia faktisch Pflicht** (formell fuer Finanzsektor-Marketing, praktisch fuer alle Apps Standard)

### Consent
7 Rechtsgrundlagen analog DSGVO. Kinder: Eltern-Einwilligung + Verifikation.

### Sanktionen
- Administrativ: Warnung, Aussetzung, Datenloeschung, Bussgeld **bis 2% Jahresumsatz**
- Strafrecht: bis 5 Jahre Haft + IDR 5 Mrd. (~280.000 EUR)
- EIT Law: bis 9 Jahre Haft + IDR 3 Mrd.

### Cross-Border (EU/DE-Anbieter)
- 3 Wege: Adequacy (noch nicht etabliert), SCCs (noch nicht ausgegeben), BCR (noch nicht moeglich)
- **De facto:** Einwilligung als einzige praktikable Grundlage aktuell
- Health-Daten: zusaetzlich Zentralregierungs-Genehmigung

### App-spezifische Pflichten
- Kinder: Eltern-Einwilligung + Verifikation
- Health Data: erhoehte Schutzpflichten (Details ausstehend)
- Account-Loeschung: Loeschrecht implementieren

### Quelle
DLA Piper Indonesia, ICLG Indonesia, Makarim

## Vietnam — PDPL Law 91/2025/QH15

### Status
**In Kraft seit 01.01.2026** — ersetzt Decree 13/2023. Begleitverordnung Decree 356/2025 seit 31.12.2025.

### Pflichten
- Zwecke der Verarbeitung
- Arten der Daten
- Verarbeitungsaktivitaeten (Decree 356)
- **DPIA-Pflicht:** binnen 60 Tagen nach Verarbeitungsbeginn bei Datenschutzbehoerde einreichen
- Social-Network- und Online-Kommunikationsdienste: Besondere Schutzmassnahmen

### Consent
- Freiwillig, spezifisch, vollstaendig informiert, **pro Zweck separat**
- Keine erzwungene Kopplung
- Kinder unter 7: Elterneinwilligung; Kinder 7+: Doppelte Einwilligung (Kind + Eltern)

### Sprachanforderung
Keine explizite Pflicht, Vietnamesisch de facto Standard fuer VN-Nutzer

### Sanktionen
- **5% des Jahresumsatzes** fuer Cross-Border-Verstoesse (Mindeststrafe VND 3 Mrd. ~114.500 EUR)
- Bis **10-facher Erloes** fuer illegalen Datenhandel
- Bis VND 3 Mrd. fuer sonstige Verstoesse
- 5 Jahre Gnadenfrist fuer Start-ups bei einigen Pflichten (DPIA)

### Aenderungen
- 06/2025: PDPL verabschiedet
- 31.12.2025: Decree 356 veroeffentlicht (ersetzt Decree 13)
- 01.01.2026: PDPL in Kraft — kompletter Rechtsrahmen-Wechsel
- Systeme mit KI, Big Data, Blockchain, Cloud: Sicherheitsmassnahmen + Authentifizierung

### Cross-Border (EU/DE-Anbieter)
- PDPL enthaelt Cross-Border-Regeln (Decree 356)
- Cybersecurity Law (Decree 53/2022) fordert Datenlokalisierung fuer bestimmte Kategorien
- Fuer Standard-Apps: keine strikte Lokalisierungspflicht
- Health/strategische Daten: pruefen
- Max-Sanktion: 5% Jahresumsatz

### App-spezifische Pflichten
- **DPIA Pflicht** binnen 60 Tagen (Ausnahme: Start-ups, 5 Jahre Frist)
- KI/Cloud: explizite Sicherheitsanforderungen
- Kinder: Dual-Consent ab 7 Jahren
- Health: spezielle Schutzmassnahmen
- Verbot: Kauf/Verkauf von Personendaten (ausser gesetzlich erlaubt)

### Quelle
Tilleke & Gibbins Vietnam PDPL, DLA Piper Vietnam, Rouse

## Sri Lanka — PDPA No. 9 of 2022

### Status: Substantive Provisions NOCH NICHT in Kraft (zum Skill-Stand dieser Datei)

### Chronologie
- 03/2022: Gesetz verabschiedet
- 2023: Nur DPA-Gruendungsprovisions + Definitionssektion in Kraft
- 03/2025: Geplanter Start (18.03.2025) — **ABGESAGT** per Gazette 14.03.2025
- 10/2025: Amendment Act No. 22 of 2025 — Minister erhaelt Ermessen ueber Inkraftsetzung
- **Zum Skill-Stand dieser Datei:** Substantive Provisions noch nicht in Kraft

### Was bereits gilt
- DPA (Data Protection Authority) operativ: `dpa.gov.lk`, `info@dpa.gov.lk`
- Draft-Regulations 10/2024 zur Konsultation (DPIA, Breach Notification, DPO, Betroffenenrechte, Transfers)
- Circular 01/2024 fuer oeffentlichen Sektor

### Pflichtangaben (wenn/sobald in Kraft)
Auf Basis Draft Regulations: Zweck, Datenkategorien, Empfaengerkategorien, Betroffenenrechte,
DPO/Controller-Kontakt

### Sprachanforderung
Keine explizite Pflicht; Singhalesisch/Tamil-Lokalisierung empfohlen aber nicht erzwungen

### Sanktionen (wenn in Kraft)
- Bis **LKR 10 Mio.** (~30.000 EUR) pro Instanz
- Amendment 2025 koennte Sanktionsrahmen angepasst haben

### Empfehlung fuer DE-Anbieter
Jetzt vorbereiten (Privacy Policy ausrichten), nicht abwarten. Vollstaendige Enforcement erwartet 2026.

### Quelle
`dpa.gov.lk`, RecordingLaw Sri Lanka

## Vergleichsmatrix Asien

| Land | Rechtsstand | Dringlichkeit | Sprache Pflicht? | Cross-Border-Basis DE |
|------|-------------|---------------|-------------------|-----------------------|
| Indien | DPDP Act + Rules 2025 | HOCH | EN + lokal | Consent + SCCs |
| Japan | APPI | MITTEL | JA empfohlen | Adequacy fuer EU/Japan beidseitig |
| Korea | PIPA | HOCH | KO empfohlen | SCCs |
| Taiwan | Aktiv + Novelle 11/2025 | HOCH | ZH-Hant erwartet | EU-SCCs praktisch ausreichend |
| HK | Aktiv | NIEDRIG | EN/ZH | GDPR-SCCs reichen |
| SG | Aktiv | MITTEL | EN | SCCs |
| Thailand | Voll aktiv, Enforcement seit 2025 | **HOCH** | Thai empfohlen | SCCs (ASEAN-Muster) |
| Indonesien | Aktiv, Behoerde fehlt | MITTEL-HOCH | Bahasa Indonesia empfohlen | Einwilligung |
| Vietnam | Neues Gesetz ab 01/2026 | **HOCH** | VN empfohlen | Decree 356, DPIA Pflicht |
| Sri Lanka | Substantiv noch nicht in Kraft | NIEDRIG (vorbereiten) | Keine | Draft-Phase |
