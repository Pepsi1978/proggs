# Markt-Uebersicht (Master-Index)

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Wenn du am Anfang eines Audits den Ueberblick brauchst,
> welche Maerkte abgedeckt sind und in welche Reference-Datei du gehen musst.

## Reference-Karte

| Reference | Maerkte | Wann reinschauen |
|--|--|--|
| `markt-de-eu.md` | DE, EU-Mitgliedstaaten (inkl. PT, IT, NL, PL) | App in DE/EU verfuegbar; PT/IT/NL/PL/AT/FR Sprachen |
| `markt-uk-us-ca-au-nz.md` | UK, USA, Kanada, Australien, Neuseeland | Englischsprachige Maerkte, EN-Locales |
| `markt-tuerkei-osteuropa.md` | Tuerkei, Ukraine | TR- oder UK-(Ukrainisch)-Locales |
| `markt-asien.md` | IN, JP, KR, TW, HK, SG, TH, ID, VN, LK | Asiatische Locales (hi/ja/ko/zh-Hant/th/id/bn/ta/te/mr/ur/gu/kn/ml) |
| `markt-sa-pakistan-bangladesch.md` | Pakistan, Bangladesch | UR-, BN-Locales |
| `markt-latam.md` | Brasilien, Mexiko, Argentinien | PT-BR oder ES-Locale fuer LatAm |
| `markt-mena-afrika.md` | Saudi-Arabien, UAE, Suedafrika | AR-Locale, EN fuer ZA |
| `uk-vertreter-pflicht.md` | UK-GDPR Art. 27 Spezialfall | App in UK + Anbieter nicht in UK |
| `ai-act-art-50.md` | EU AI Act Art. 50 ab 02.08.2026 | App hat KI/GenAI/Chatbot/Deepfake-Features |
| `play-policies.md` | Google Play 2025/2026 Updates | Vor jedem Play-Console-Release |
| `pflichtdokumente.md` | DSE/AGB/Impressum/Widerruf + interne Artefakte | Im Schritt 5 + Pflichtdokumente-Checks |
| `roentgen-integration.md` | Schritt 1.5 Detail | Wenn Roentgen-Output vorliegt oder fehlt |
| `enforcement-trends.md` | Aktuelle Abmahn-Hotspots DE/EU | Bei Recherche und Risiko-Einschaetzung |
| `wissensbasis-template.md` | Schritt 7 Wissensbasis-Pflege | Am Ende des Audits |

## Schliessliste — In dieser Programmierumgebung ausgeschlossene Maerkte

- **Russland (ru-Locale):** Frank liefert dort keine Apps aus — nicht geprueft, keine Researcher-Recherche.
- **China-Mainland (zh-Hans-Locale, PIPL):** Ebenfalls bewusst ausgeschlossen.

Wenn diese Maerkte irgendwann doch relevant werden: Eigene Reference-Datei erstellen, Researcher mit
PIPL/Roskomnadzor-Fokus laufen lassen.

## Skill-Stand und Recherche-Aktualitaet

- **Skill-Stand:** 2026-05-17
- **Naechste Pflicht-Aktualisierung:** spaetestens +30 Tage bei Play-Policies, +90 Tage sonst
- **Ausloeser fuer Sofort-Recherche:**
  - Letzte Recherche aelter als 30 Tage
  - Google Play Policies betroffen
  - Health, Kinder, Standort, Kontakte, Medien, SMS/Call Logs, Finanzdaten, KI/GenAI,
    Ads, Analytics, User Generated Content oder Accounts vorkommen
  - App wird in neue Laender/Sprachen/SDKs/Monetarisierungsmodelle ausgerollt
  - Benutzer sagt "aktuell", "neueste" oder "Release"

## Quellenprioritaet (verbindliche Reihenfolge)

1. **Primaerquellen** — Gesetzestexte, offizielle Regulierer, Google/Android-Policy
2. **Sekundaerquellen** — Fachanwaelte, Fachverbaende, Behoerden-Erklaerungen
3. **News/Blogs** — nur als Hinweis, NIE alleinige Grundlage

## Markt- und Rechtsraum-Prioritaet

Audits **immer in dieser Reihenfolge** bewerten:

### 1. Deutschland / EU (hoechste Prioritaet)

DSGVO, DDG, TDDDG, BGB/EGBGB Widerruf/digitale Produkte, BFSG/EAA Barrierefreiheit,
DSA bei UGC/Hosting/Marketplace, AI Act (Art. 50 ab 02.08.2026) bei KI-Features,
EU Data Act seit 12.09.2025.

### 2. Englischsprachige Zielmaerkte

UK, USA, Kanada, Australien, Neuseeland. Details in `markt-uk-us-ca-au-nz.md`.

### 3. Internationale Zielmaerkte

Tuerkei, Asien, LatAm, MENA, Afrika. Details in den jeweiligen `markt-*.md`-Dateien.

## Pflichtfragen pro Zielmarkt

Fuer jeden Zielmarkt MUSS geprueft werden:

1. Ist die App dort verfuegbar (Store-Listing, Geo-Filter, Backend-Erreichbarkeit)?
2. Gibt es App-Locale + Store-Listing-Locale + verstaendliche Rechtstexte in der
   Landessprache?
3. Welche lokalen Pflichtangaben, Consent-, Kinder-, Health-, AI-, UGC-, Abo-,
   Zahlungs- oder Transferregeln greifen?
4. Was ist technisch nachweisbar (Code, Manifest, SDK, Logs)?
5. Was muss juristisch geklaert werden?

**Wenn ein Markt nicht bewertet werden kann: BLOCKER fuer Rollout in diesem Markt.**

## Master-Tabelle: Jurisdiktions-Gates

> **Sync-Pflicht:** Diese Tabelle wird an drei Stellen synchron gehalten:
> 1. Hier (`references/markt-uebersicht.md`) — Master mit Reference-Spalte
> 2. `assets/berichtsvorlage.template.md` — Bericht-Variante mit Befund-Spalte
> 3. `references/wissensbasis-template.md` — Bullet-Liste im Wissensbasis-Template
>
> Bei jedem Markt-Update (neu, geaendert, entfernt) MUESSEN alle drei Stellen aktualisiert
> werden. Stil-Konvention: Markt-Namen IMMER in `**Fett**`.

Diese Master-Tabelle ist die zentrale Pflichtpruefungs-Liste — Details siehe die jeweiligen
`markt-*.md`-Dateien.

| Rechtsraum | Pflichtpruefung | Typische Release-Blocker | Reference |
|---|---|---|---|
| **DE/EU** | DSGVO, DDG §5, TDDDG + PIMS, BGB §312k/§356, BFSG (KMU-Schwelle <10 MA & <2 Mio. EUR), DSA, EU Data Act, AI Act Art. 50 (02.08.2026), DPF-Status | Fehlendes Impressum, fehlende DSE, Cookie-Consent fehlt, kein Widerruf bei IAP/Abos, BFSG-Verstoesse | `markt-de-eu.md` |
| **UK** | UK-GDPR, DPA 2018, PECR, Online Safety Act, **Art. 27 (UK-Vertreter)** | PECR-Consent fehlt, OSA-Pflichten bei UGC, kein UK-Vertreter trotz Datenverarbeitung — Standard-Empfehlung: **Option B (UK ausschliessen)** | `markt-uk-us-ca-au-nz.md` + `uk-vertreter-pflicht.md` |
| **USA** | CCPA/CPRA, weitere State Privacy Laws, COPPA, FTC Act, Health Breach/HIPAA/FDA | "Do Not Sell"-Pflichten, COPPA bei Kindern, Health-Claims ohne FDA | `markt-uk-us-ca-au-nz.md` |
| **Kanada** | PIPEDA, Quebec Law 25 | Quebec-Sprachpflicht, Privacy-Officer, Data-Transfer-Disclosure | `markt-uk-us-ca-au-nz.md` |
| **Australien** | Privacy Act / APPs | Fehlende Privacy Policy mit AU-Bezug, Cross-Border-Disclosure | `markt-uk-us-ca-au-nz.md` |
| **Neuseeland** | Privacy Act 2020 (IPP 1-13) | Fehlende NZ-Privacy-Policy, fehlende 72h-Breach-Meldung, IPP-12-Cross-Border | `markt-uk-us-ca-au-nz.md` |
| **Tuerkei** | KVKK (Law 6698) | VERBIS fehlt, lokaler Vertreter fehlt, SCCs nicht im KVKK-Muster (TR), Sprachpflicht TR | `markt-tuerkei-osteuropa.md` |
| **Ukraine** | Law 2297-VI (aktuell) / Bill 8153 (in Bearbeitung) | Aktuelles Regime minimaler Aufwand; bei Bill 8153 GDPR-Anpassung | `markt-tuerkei-osteuropa.md` |
| **Pakistan** | KEIN aktives Datenschutzgesetz ("legal vacuum") | Nur Play-Store-Anforderungen + PECA 2016 | `markt-sa-pakistan-bangladesch.md` |
| **Bangladesch** | PDPO 2025 (seit 16.11.2025) | 18M Uebergang; CDO-Pflicht; **Datenlokalisierung Restricted/CII** | `markt-sa-pakistan-bangladesch.md` |
| **China** | PIPL (in dieser Umgebung ausgeschlossen) | Skill enthaelt China-Sektion nur als Referenz; standardmaessig BLOCKER fuer Rollout | (keine eigene Datei) |
| **Indien** | DPDP Act / Rules 2025 | Consent-Manager-Pflicht, Notice-Pflichten, Data-Fiduciary | `markt-asien.md` |
| **Japan** | APPI | Cross-Border-Transfer-Disclosure, Sensitive-Daten-Consent | `markt-asien.md` |
| **Korea** | PIPA | Strikte Consent-Pflichten, Notification, DPO | `markt-asien.md` |
| **Taiwan** | PDPA + Novelle 11/2025 | Breach Notification + DPO Pflicht neu | `markt-asien.md` |
| **Hongkong** | PDPO (inkl. Doxxing-Sec. 26K-26N) | PICS vor Erhebung, Doxxing bei UGC | `markt-asien.md` |
| **Thailand** | PDPA 2019 (voll aktiv seit 01.06.2022) | Separate Consent pro Zweck, Cross-Border via ASEAN/PDPC-SCCs | `markt-asien.md` |
| **Indonesien** | UU PDP / Law 27 of 2022 | Bahasa faktisch Pflicht; Eltern-Consent fuer Kinder | `markt-asien.md` |
| **Vietnam** | PDPL Law 91/2025/QH15 (ab 01.01.2026) | DPIA binnen 60 Tagen; Bussgelder bis 5% Umsatz fuer Cross-Border | `markt-asien.md` |
| **Sri Lanka** | PDPA No. 9/2022 (substantive Provisions ausgesetzt) | Aktuell nur Vorbereitung | `markt-asien.md` |
| **Singapore** | PDPA | DNC-Register, DPO, Consent-Pflichten | `markt-asien.md` |
| **Brasilien** | LGPD / ANPD | Rechtsgrundlagen-Disclosure, Cross-Border | `markt-latam.md` |
| **Mexiko** | LFPDPPP-Reform 21.03.2025 | **INAI aufgeloest, ACGG zustaendig**, Simplified Aviso de Privacidad, Spanisch Pflicht | `markt-latam.md` |
| **Argentinien** | Ley 25.326 + SCCs 2023 + Sanktionsreform 06/2024 | **EU/EWR adaequat anerkannt**; neue SCCs ohne Vorabgenehmigung | `markt-latam.md` |
| **Suedafrika** | POPIA | Information-Officer-Pflicht, Cross-Border | `markt-mena-afrika.md` |
| **Saudi/UAE** | PDPL | Cross-Border-Transfer-Genehmigung, Lokalisierung | `markt-mena-afrika.md` |

## Was NIEMALS passieren darf

- Markt unbewertet freigeben — wenn der Skill den Rechtsraum nicht aktuell kennt: BLOCKER setzen
- Maerkte ueberspringen, weil "klingt aehnlich wie EU" — jede Jurisdiktion separat pruefen
- Sprache als technisches Detail abtun — Sprachpflicht ist juristische Pflicht in vielen Maerkten
