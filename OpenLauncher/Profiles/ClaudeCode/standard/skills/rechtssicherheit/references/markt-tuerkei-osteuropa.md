# Markt Tuerkei + Osteuropa — Rechtsraum-Reference

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Wenn die App in der Tuerkei (tr-Locale) oder Ukraine
> (uk-Locale) ausgespielt wird.

## Tuerkei — KVKK (Law No. 6698)

### Status
Aktiv und vollstaendig enforced. Tiefgreifende Reformen in Kraft seit 06/2024 und 09/2024.

### Pflichtangaben Datenschutzerklaerung
- Identitaet + Kontaktdaten des Datenverantwortlichen
- Verarbeitungszwecke (pro Zweck separat)
- Kategorien verarbeiteter Daten + Empfaenger
- Rechte der Betroffenen (inkl. Widerspruchsrecht gegen automatisierte Entscheidungen — neu seit 2025)
- Grundlage der Verarbeitung
- Cross-Border-Transfer-Informationen + Grundlage
- Aufbewahrungsdauer

### Sprachanforderung
**Tuerkisch ist faktisch Pflicht.** SCCs MUESSEN in tuerkischer Sprache vorliegen, tuerkische
Version vorranging. KVKK Board veroeffentlicht alle Muster auf Tuerkisch. Englisch-only-DSE ist
kein sicherer Compliance-Stand.

### Sanktionen 2025/2026
- Verletzung der Informationspflicht: TRY 85.437–1.709.200 (2026 +25,49% gegenueber 2025)
- Maximales Bussgeld bei schweren Sicherheitsverstoessen: **TRY 13,6 Mio.** (~350.000 EUR)
- 08/2024: Enforcement gegen 16.350 Organisationen, gesamt ~14 Mio. EUR
- Strafrechtliche Sanktionen moeglich
- Sanktionsuntergrenzen +25,49% in 2026

### Cross-Border-Reform 06/2024 (vollstaendig ab 09/2024)
- Tiered System analog GDPR: Adequacy Decision (KVKK Board) > SCCs (TR-Muster, unveraenderbar) > BCR
- **Explicit Consent als Transfer-Basis ABGESCHAFFT**
- SCCs MUESSEN UNVERAENDERT verwendet werden (keine eigenen Klauseln)
- Notifikation bei KVKK innerhalb 5 Werktagen nach SCC-Unterzeichnung — Fristversaeumnis bussgeldbewehrt (TRY 72.000–1,4 Mio.)
- SCCs apostilliert und tuerkisch uebersetzt vorliegen
- Fuer DE-Anbieter ohne Adequacy: KVKK-Standard-SCCs zwingend

### VERBIS-Registrierungspflicht
- Ausl. Datenverantwortliche die TR-Daten verarbeiten: **VERBIS-Registrierung Pflicht**
- Schwellenwert: >50 MA ODER Sensible Daten (unabh. von Groesse)
- **Lokaler Vertreter (Repraesentant) verpflichtend** — notariell beglaubigt und apostilliert
- Enforcement: KVKK Board prueft aktiv ausl. Unternehmen

### Neuerungen seit 2025
- 2025: Neue Rechte: Datenportabilitaet + Widerspruch gegen automatisierte Entscheidungen
- 2025: Einwilligungsmanagement aktualisiert — digitale Opt-in + einfacher Widerruf
- Breach-Notification: 72h-Frist (2024/2025)

### App-spezifische Pflichten
- Cookie-Consent mit opt-in (nicht opt-out) fuer nicht-essentielle Cookies
- Kinder-Datenschutz: Elterneinwilligung fuer unter 18
- KI/Automated Decisions: neues Widerspruchsrecht 2025
- Account-Loeschung: Datenloeschungsrecht implementieren und kommunizieren

### Release-Blocker fuer Tuerkei
- Keine TR-Datenschutzerklaerung
- Kein lokaler Vertreter (bei VERBIS-Pflicht)
- VERBIS nicht registriert
- SCCs nicht im KVKK-Muster, nicht apostilliert, nicht TR-uebersetzt
- Cross-Border-Notifikation an KVKK ausstehend

---

## Ukraine

### Status
Aktuell geltendes Recht: **Gesetz Nr. 2297-VI "Ueber den Schutz personenbezogener Daten"** (basiert
auf Europaratskonvention 108, nicht GDPR). Bill 8153 (GDPR-Annaeherung) hat erste Lesung 20.11.2024
bestanden — noch kein Gesetz.

### Pflichtangaben nach aktuellem Recht
- Identitaet + Kontaktdaten Datenverantwortlicher
- Zwecke der Verarbeitung
- Kategorien der Daten
- Empfaenger oder Empfaengerkategorien
- Rechte der Betroffenen (Zugang, Berichtigung, Loeschung, Widerspruch)
- Cross-Border-Transfer-Informationen

### Sprachanforderung
Aktuelles Gesetz schreibt keine spezifische Sprache vor. **Ukrainisch wird empfohlen** (Behoerdensprache).
Englisch allein ist rechtlich riskant aber nicht explizit verboten.

### Sanktionen (aktuelles Recht — sehr niedrig)
- Natuerliche Personen: UAH 1.700–3.400 (~USD 40–80)
- Wiederholt: UAH 5.100–8.500 (~USD 120–200)
- Juristische Personen/Geschaeftsfuehrer: UAH 3.400–6.800 (~USD 80–160)
- Ombudsperson fuer Menschenrechte ist Aufsicht — Enforcement sehr schwach

### Wenn Bill 8153 verabschiedet wird (voraussichtlich 2026)
- Bussgelder bis UAH 150 Mio. oder 8% Jahresumsatz
- Unabhaengige Datenschutzbehoerde (Nationale Kommission)
- DPO-Pflicht
- Datenportabilitaet + Widerspruch gegen automatisierte Entscheidungen
- Explizite Einwilligung (kein Pre-checked-Feld)

### Kriegszustand
Datenschutzbehoerde hat festgestellt, dass Einschraenkungen im Kriegsrecht fuer nationale
Sicherheit gerechtfertigt sein koennen. Aktueller Enforcement-Fokus auf Cybersicherheit.

### Cross-Border-Transfer
- Transfer erlaubt wenn Empfaengerland "angemessenes Schutzniveau" bietet
- EWR-Mitgliedstaaten + Unterzeichner der Europaratskonvention 108 automatisch adequat
- **Deutschland: kein Problem (EWR)**
- Mit individueller Einwilligung auch andere Staaten

### App-spezifische Pflichten (aktuelles Recht)
- Keine spezifischen App-Anforderungen
- Allgemeine Datenschutzprinzipien gelten

### Release-Bewertung Ukraine
Aktuelles Regime: minimaler Aufwand, kaum Risiko. Bei Bill 8153 (wenn verabschiedet): GDPR-Anpassung
und 8%-Bussgelder — dann gleicher Aufwand wie DE/EU.
