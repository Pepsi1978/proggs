# Wissensbasis-Template (Schritt 7)

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** In Schritt 7 des Skill-Ablaufs, wenn die Wissensbasis
> `<WORKSPACE_ROOT>/tools/rechtssicherheit.md` aktualisiert oder neu angelegt wird.

## Struktur

```markdown
# rechtssicherheit.md - Wissensbasis
Letzte Recherche: YYYY-MM-DD
Naechste Pflicht-Pruefung: YYYY-MM-DD (+30 Tage bei Play Policies, +90 Tage sonst)

## Quellenregister
| Datum | Quelle | Thema | Quellenklasse | Relevanz |
|---|---|---|---|---|

## Pflichtangaben-Matrix

### EU/DE - Datenschutz (DSGVO)
### EU/DE - Impressum (DDG §5)
### EU/DE - Widerruf (BGB/EGBGB, digitale Produkte)
### EU/DE - TDDDG / ePrivacy (Tracking/Cookies/Storage)
### EU/DE - DSA (Hosting/UGC/Marketplace)
### EU/DE - AI Act (KI-Features je Risikoklasse, Art. 50 ab 02.08.2026)
### EU/DE - BFSG / EAA (Barrierefreiheit)
### EU/DE - EU Data Act (12.09.2025)
### EU/DE - EU-US DPF (EuG 03.09.2025 / EuGH-Berufung)

### UK - UK-GDPR / DPA / PECR / Online Safety Act / Art. 27
### USA - CCPA/CPRA, State Privacy Laws, COPPA, FTC, Health Breach/HIPAA/FDA
### Kanada - PIPEDA, Quebec Law 25
### Australien - Privacy Act / APPs
### Neuseeland - Privacy Act 2020 (IPP 1-13, IPP 12, 72h-Breach)

### Tuerkei & Osteuropa
- Tuerkei KVKK + VERBIS + lokaler Vertreter + Cross-Border-Reform 06/2024
- Ukraine Law 2297-VI / Bill 8153

### International - Asien
- China PIPL (in dieser Programmierumgebung ausgeschlossen — nur Referenz)
- Indien DPDP + Rules 2025
- Japan APPI
- Korea PIPA
- Taiwan PDPA + Novelle 11/2025
- Hongkong PDPO + Doxxing-Reform Sec. 26K-26N
- Singapore PDPA
- Thailand PDPA 2019 + Cross-Border-Verordnung 03/2024
- Indonesien UU PDP 2022 + RPP PDP (ausstehend)
- Vietnam PDPL 2026 (Law 91/2025/QH15) + Decree 356/2025
- Pakistan (KEIN aktives Datenschutzgesetz — "legal vacuum")
- Bangladesch PDPO 2025 (Ord. 61/2025)
- Sri Lanka PDPA No. 9/2022 (substantive Provisions ausgesetzt)

### International - LATAM
- Brasilien LGPD
- Mexiko LFPDPPP-Reform 21.03.2025
- Argentinien Ley 25.326 + SCCs 2023 + Sanktionsreform 06/2024

### International - MENA / Afrika
- Suedafrika POPIA
- Saudi PDPL
- UAE PDPL

### Google Play
- Data Safety / User Data
- Permissions / Sensitive Permissions (April-2026-Update)
- Account Deletion
- Families / Kinder
- Health Apps Declaration
- AI-generated Content Declaration
- UGC / Deceptive Behavior
- Ads
- Payments / Subscriptions (Cancel-Button in App)

### Android Security/Privacy Controls
- Photo Picker, Storage Scoped, Permissions
- Backup-Regeln, dataExtractionRules
- Network Security Config, TLS, Cleartext
- WebView, JS Bridge

### Spezialfaelle
- Kinder
- Health
- AI/GenAI/Deepfake
- Ads / Advertising ID
- UGC / Moderation / Beschwerdeweg
- Finance

## Sprach-Anforderungen pro Markt
| Markt | Sprache | Pflicht? | Quelle |
|---|---|---|---|

## Aktuelle Abmahn-Hotspots (Stand YYYY-MM)
| Thema | Quelle | Empfehlung |
|---|---|---|

## App-Audit-Log
| Datum | App | Version | Status | Blocker | Hoch | Commit/Notiz |
|---|---|---|---|---:|---:|---|

## Wiederverwendbare Befundmuster
| Muster | App-Klasse | Empfohlener Fix |
|---|---|---|

## Muster-Klauseln (mit Quelle)
| Klausel | Bereich | Quelle | Stand |
|---|---|---|---|
```

## Diff-Logik

- Neue Erkenntnisse gegenueber dem gespeicherten Stand hervorheben ("**Aenderungen seit letzter Recherche**")
- Veraltete Eintraege (>90 Tage) als "zu verifizieren" markieren
- Jede Pflichtangabe mit Quell-URL + Abrufdatum + Quellenklasse

## Was NICHT in diese Datei schreiben

- Keine Secrets
- Keine echten Kundendaten
- Keine privaten Adressen
- Keine Token

Ausser der Benutzer verlangt explizit genau diese Ablage.
