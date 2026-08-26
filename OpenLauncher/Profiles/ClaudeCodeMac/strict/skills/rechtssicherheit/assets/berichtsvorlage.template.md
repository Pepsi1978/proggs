# Rechtssicherheits-Audit: [App]
Datum: YYYY-MM-DD
Skill-Stand: 2026-05-17

## Disclaimer
Technische Pruefhilfe, keine anwaltliche Beratung. Vor Release Fachanwalt fuer
IT-Recht konsultieren.

## Scope und Annahmen
- App/Package:
- Zielmaerkte:
- Monetarisierung:
- Accounts:
- SDKs:
- Sensible Daten:
- Feature-Gates (Kinder/Health/AI/UGC/Ads/Abo/Standort/Barrierefreiheit):

## Gesamtstatus
- Release-Empfehlung: [BLOCKIEREN | BEDINGT | TECHNISCH OK NACH ANWALTSPRUEFUNG]
- BLOCKER: N
- HOCH: N
- MITTEL: N
- Wichtigste Risiken:

## Befunde
### 🔴 BLOCKER
1. [Titel]
   - Nachweis: [Datei:Zeile oder Quelle]
   - Risiko: [konkret]
   - Fix: [konkret]
   - Quelle: [URL, Abrufdatum]

### 🟠 HOCH
...
### 🟡 MITTEL
...
### 🟢 NIEDRIG
...
### ℹ️ INFO
...

## Dokumentenmatrix
| Dokument | In App | Store/Web | Inhalt OK | Sprache OK | Befund |
|---|---:|---:|---:|---:|---|
| Datenschutzerklaerung | | | | | |
| Nutzungsbedingungen | | | | | |
| Impressum | | | | | |
| Widerruf | | | | | |
| Account-/Datenloeschung | | | | | |

## Interne Compliance-Artefakte (im Repo nachweisbar?)
| Dokument | Vorhanden | Pfad | Befund |
|---|---:|---|---|
| VVT / ROPA (DSGVO Art. 30) | | | |
| DSFA / DPIA (Art. 35) | | | |
| TIA (Art. 44 ff.) | | | |
| TOMs (Art. 32) | | | |
| AVV-Liste (Art. 28) | | | |
| SCCs / DPF / BCR | | | |
| Loeschkonzept | | | |
| Datenpannen-Meldeplan + Logbuch (Art. 33/34, 72h-Frist) | | | |
| AI-System-Risikoklassifizierung (AI Act) | | | |
| DSA-Beschwerde-/Kontaktstelle (Art. 11) | | | |

## Codestruktur-Vollscan
| Treffergruppe | Dateien/Beispiele | Rechtliche Relevanz | Abgleich | Status |
|---|---|---|---|---|
| SDKs/Dritte | | Datenschutz, Transfer, Data Safety | | |
| Permissions/Sensoren | | Prominent Disclosure, Consent | | |
| Account/Sync/Loeschung | | DSGVO, Google Account Deletion | | |
| Billing/Ads/Abo | | Widerruf, Terms, Ads Policy | | |
| Security/Backup/Logs | | Datenschutz, Sicherheitsversprechen | | |
| Health/Kinder/AI/UGC | | Spezial-Policies | | |
| Barrierefreiheit | | BFSG/EAA, WCAG | | |

## Code-vs-Text-vs-Play-Matrix
| Daten/Feature | Code/SDK/Permission | Privacy Policy | Data Safety | Consent/UI | Status |
|---|---|---|---|---|---|

## Werbeaussagen-vs-Feature-Matrix (UWG §5/§5a — nur mit Roentgen-Output)
| Werbeaussage | Quelle (Store/Paywall/...) | Feature im Code | Stimmt? | Befund |
|---|---|---|---:|---|

## Android-Sicherheitscheck
| Kontrolle | Ergebnis | Risiko | Fix |
|---|---|---|---|
| Permissions minimal | | | |
| Backup-Regeln | | | |
| TLS/Cleartext | | | |
| Sensitive Logs | | | |
| Secrets im Repo | | | |
| Exported Components | | | |
| WebView sicher | | | |

## Sprach- und Marktfreigabe
| Markt | App-Locale | Rechtstexte | Pflicht/Empfehlung | Freigabe |
|---|---|---|---|---|

## Jurisdiktions-Gates

> **Sync-Hinweis:** Diese Tabelle ist eine 1:1-Spiegelung der Master-Tabelle in
> `references/markt-uebersicht.md`. Wenn ein Markt geaendert wird (neuer Markt aufgenommen,
> Bewertung geaendert, Markt entfernt), MUESSEN beide Tabellen aktualisiert werden. Der Stil
> (Markt-Namen in `**Fett**`) muss identisch bleiben damit visuelle Konsistenz gewahrt ist.

| Rechtsraum | Pflichtpruefung | Bewertung | Release-Blocker? |
|---|---|---|---|
| **DE/EU** | DSGVO, DDG §5, TDDDG + PIMS, BGB §312k/§356, BFSG, DSA, EU Data Act, AI Act Art. 50 (02.08.2026), DPF | | |
| **UK** | UK-GDPR, PECR, Online Safety Act, Art. 27 | | |
| **USA** | CCPA/CPRA, COPPA, FTC, Health Breach/HIPAA/FDA | | |
| **Kanada** | PIPEDA, Quebec Law 25 | | |
| **Australien** | Privacy Act / APPs | | |
| **Neuseeland** | Privacy Act 2020 (IPP 1-13, IPP 12, 72h-Breach) | | |
| **Tuerkei** | KVKK, VERBIS, lokaler Vertreter, SCCs auf TR, Cross-Border-Reform 06/2024 | | |
| **Ukraine** | Law 2297-VI (aktuell) / Bill 8153 (Annaeherung) | | |
| **Pakistan** | KEIN aktives Datenschutzgesetz — nur Play-Anforderungen + PECA 2016 | | |
| **Bangladesch** | PDPO 2025 (Uebergangszeit), CDO, Datenlokalisierung Restricted/CII | | |
| **China** | PIPL (in dieser Programmierumgebung ausgeschlossen) | | |
| **Indien** | DPDP + Rules 2025 | | |
| **Japan** | APPI | | |
| **Korea** | PIPA | | |
| **Taiwan** | PDPA + Novelle 11/2025 | | |
| **Hongkong** | PDPO + Doxxing-Reform | | |
| **Thailand** | PDPA 2019 + Cross-Border-Verordnung 03/2024 + ASEAN-SCCs | | |
| **Indonesien** | UU PDP 2022, Bahasa-Pflicht | | |
| **Vietnam** | PDPL 2026 (Law 91/2025/QH15), DPIA 60-Tage-Pflicht | | |
| **Sri Lanka** | PDPA No. 9/2022 (substantive Provisions ausgesetzt 10/2025) | | |
| **Singapore** | PDPA | | |
| **Brasilien** | LGPD | | |
| **Mexiko** | LFPDPPP-Reform 21.03.2025, ACGG, Simplified Aviso, Spanisch | | |
| **Argentinien** | Ley 25.326 + SCCs 2023 + Sanktionsreform 06/2024 | | |
| **Suedafrika** | POPIA | | |
| **Saudi/UAE** | PDPL | | |

## Formulierungs-Check
| Textstelle | Datei:Zeile | Risikoart | Empfehlung |
|---|---|---|---|
| (z.B. "100% sicher") | | Garantie | Entfernen, Plain-Language-Alternative |

## Play-Console-Checkliste
- [ ] Data Safety passt zu Code und SDKs
- [ ] Privacy Policy URL erreichbar
- [ ] Account deletion beantwortet und verlinkt (in-app + web-url)
- [ ] App Access korrekt
- [ ] Content Rating korrekt
- [ ] Target Audience/Families korrekt
- [ ] Ads/Health/AI/Finance/UGC/Permissions Declarations korrekt
- [ ] Subscription Cancel-Button in App
- [ ] News-App Self-Declaration falls relevant
- [ ] AI-Generated Content Declaration + In-App-Flagging falls KI-Features

## Fix-Reihenfolge
1. BLOCKER zuerst.
2. HOCH vor Release.
3. MITTEL vor Rollout in weitere Laender.
4. NIEDRIG bei naechster Pflege.

## Quellen
- [URL] - [Thema] - [Quellenklasse: Primaer/Sekundaer/News] - abgerufen am YYYY-MM-DD

## Abschluss-Disclaimer
Technische Pruefhilfe, keine anwaltliche Beratung. Vor Release Fachanwalt fuer
IT-Recht konsultieren.
