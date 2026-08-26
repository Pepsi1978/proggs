# Enforcement-/Abmahn-Trends 2025/2026

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren — diese Datei wird
> haeufiger aktualisiert als andere References.

> **Wann diese Datei lesen:** Bei jeder Risiko-Einschaetzung und Recherche.

## Top-Risiken nach Wirkung (sortiert)

| Bereich | Aktueller Trend 2025/2026 | Schweregrad |
|--|--|--|
| DSA | X (Twitter): 120 Mio. EUR Bussgeld (erstes DSA-Bussgeld). Shein: Verfahren 02/2026 eroeffnet. Harmonisierte Berichte ab 01.07.2025 Pflicht | 🔴 BLOCKER |
| BFSG | Aufsichtsbehoerden im Aufbau, erste Bescheide ab Herbst 2025. **Abmahnrisiko durch Konkurrenten besteht bereits** | 🟠 HOCH |
| TDDDG/Cookie | Striktere DE-Durchsetzung 2025/2026. Nudging (Accept-Button optisch groesser) jetzt klar unzulaessig. Abmahnungen durch Verbaende aktiv | 🟠 HOCH |
| AI Act | Vollstaendige Durchsetzung erst ab 02.08.2026 — aber Dokumentationspflichten (GPAI) bereits aktiv | 🟡 MITTEL (steigend) |
| Play Store | App-Entfernungen bei AI-Policy-Verstoss dokumentiert. Health-Data-Verstoesse und fehlende Account-Deletion zunehmend Ablehnungsgrund | 🟠 HOCH |
| DPF | Juristisch derzeit stabil, politisch wackelig durch Trump-Regierung (Angriff auf unabhaengige US-Behoerden die DPF ueberwachen) | 🟡 MITTEL (volatil) |
| Tuerkei KVKK | 08/2024 Enforcement gegen 16.350 Organisationen, ~14 Mio. EUR. Bussgeldgrenzen 2026 +25,49% | 🟠 HOCH |
| Thailand PDPA | Erste Grossbussgeld-Welle 08/2025 (THB 21,5 Mio. gesamt, hoechste THB 7 Mio.) | 🟠 HOCH |
| Indonesien UU PDP | Vollcompliance formal seit 10/2024, aber Behoerde (BP3DP) noch nicht etabliert — derzeit MOCD | 🟡 MITTEL |
| Vietnam PDPL 2026 | Komplett neues Gesetz seit 01.01.2026, DPIA Pflicht binnen 60 Tagen | 🟠 HOCH |
| Mexiko LFPDPPP | Komplett neue Fassung seit 21.03.2025: INAI aufgeloest, ACGG uebernimmt, Processor direkt haftbar | 🟠 HOCH |
| Bangladesch PDPO 2025 | Seit 16.11.2025, 18 Monate Uebergang, Datenlokalisierung fuer Restricted/CII | 🟠 HOCH |

## Top-Abmahn-Hotspots Deutschland 2025/2026

1. **Fehlendes/unvollstaendiges Impressum** — aktivste Abmahnwelle DE. §5 DDG verlangt: Name, Adresse, E-Mail, Handelsregister, USt-ID. Im Play Store unter "Kontaktdaten Entwickler" UND in der App selbst erreichbar.
2. **Datenschutzerklaerung fehlt oder veraltet** — haeufigster Play-Reject-Grund. Muss im Store-Listing verlinkt sein (oeffentliche URL, kein Login).
3. **Account-Loeschpfad fehlt** — Google enforced seit 2023. Braucht oeffentliche Web-URL + In-App-Flow.
4. **Data Safety Form unvollstaendig** — Android ID seit 2025 explizit required. SDKs (Firebase Analytics, Adjust, Meta SDK) werden automatisch gescannt.
5. **Widerrufsbelehrung fehlt bei In-App-Kaeufen** — B2C-Pflicht, haeufig uebersehen.
6. **Kein AVV mit Firebase/Google** — DSGVO-Verstoss, max. 20 Mio. EUR Bussgeld.
7. **Health Declaration fehlt** — Seit 08/2025 mandatory; Apps ohne Erklaerung werden rejected.
8. **KI-Hinweis fehlt** (ab 02.08.2026) — Bussgeld bis 15 Mio. EUR oder 3 % Jahresumsatz.
9. **IARC Rating fehlt** — App erscheint in DE nicht.
10. **TDDDG-Consent fehlt** — Tracking-SDKs ohne Einwilligung = §28 TDDDG, bis 300.000 EUR Bussgeld.

## Aktuelle Urteile 2024-2026

- **DSA Enforcement** 02/2026: Shein-Verfahren eroeffnet
- **DSA Erstes Bussgeld** 2025: X (Twitter) 120 Mio. EUR
- **DPF Klage** 03.09.2025 — EuG abgewiesen, EuGH-Berufung anhaengig
- **EuGH Cookie-Banner** mehrere Urteile 2025 zu Dark Patterns und Nudging
- **BFSG** erste Enforcement-Erkenntnisse Herbst 2025

## Was haeufig in Apps schiefgeht

| Kategorie | Typischer Fehler |
|--|--|
| Privacy Policy | Generator-Text ohne App-/SDK-Bezug |
| Impressum | Verweis auf "§5 TMG" statt "§5 DDG" |
| Data Safety | Android ID nicht deklariert |
| Account-Loeschung | Nur In-App, kein Weblink |
| Cookie/Consent | Pre-checked Boxes |
| Widerruf | Bei sofortiger digitaler Leistung kein Verzicht erklaert |
| Health | Health Apps Declaration nicht ausgefuellt |
| KI | Keine Kennzeichnung "von KI generiert" |
| BFSG | Keine Erklaerung zur Barrierefreiheit |
| DSA | Keine Beschwerde-Kontaktstelle |
| UK | Vertreter nicht benannt UND UK nicht ausgeschlossen |
| Tuerkei | Kein lokaler Vertreter trotz VERBIS-Pflicht |
| Mexiko | Kein Spanisch-Aviso de Privacidad in App |

## Aktualisierungs-Plan

Diese Datei MUSS bei jedem Audit neu geprueft werden, wenn:
- Letzte Aktualisierung > 30 Tage
- Neues grosses Urteil bekannt (BGH, EuGH, BfDI, ICO)
- Neue Play Store Policy
- Neuer Markt im Skill aufgenommen

## Quellen
- `it-recht-kanzlei.de`
- `dr-bahr.com`
- `wbs.legal`
- `juris.de`
- `haendlerbund.de`
- `edpb.europa.eu` (Decisions)
