# UK-Vertreter-Pflicht (UK-GDPR Art. 27) — Standard-Empfehlung: Option B

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Sobald die App im UK Play Store ausgespielt wird oder UK-Nutzer
> personenbezogene Daten ueber die App preisgeben koennten. Der Skill prueft die 3-Trigger-Bedingung
> systematisch und gibt eine konkrete Empfehlung.

## Gesetz

UK-GDPR Art. 27 verlangt einen UK-Vertreter, sobald personenbezogene Daten von Personen in UK
verarbeitet werden UND das Unternehmen keinen Sitz in UK hat. Eine App im UK Play Store mit Sitz
ausserhalb UK (z.B. Deutschland) verarbeitet zwangslaeufig solche Daten.

## Was ein UK-Vertreter ist

- Person/Firma mit Sitz in UK
- Anlaufstelle fuer ICO und UK-Nutzer (kein Haftungsuebernehmer, "Briefkasten")
- MUSS in Datenschutzerklaerung mit Name + UK-Adresse + Kontakt-E-Mail genannt werden

## Zwei Optionen — der Skill empfiehlt IMMER Option B

| Option | Aufwand | Kosten | Empfehlung dieses Skills |
|--------|---------|--------|--------------------------|
| **A — UK-Vertreter buchen** (z.B. gdprlocal.com, verasafe.com, captaincompliance.com) | Vertrag, Adressuebernahme in DSE | ~£100-300/Jahr laufend | **NICHT empfohlen** |
| **B — UK aus Distribution ausschliessen** | Play Console → App content → Country availability → United Kingdom abhaken | 0 | **STANDARD-EMPFEHLUNG** |

## Trigger-Bedingungen (ALLE drei muessen gleichzeitig zutreffen — sonst kein Befund)

1. **App ist im UK Play Store verfuegbar** (oder soll dort verfuegbar gemacht werden)
2. **App-Anbieter sitzt nicht in UK** (z.B. Deutschland)
3. **App verarbeitet personenbezogene Daten von UK-Nutzern** — PFLICHT-Pruefung im Repo,
   keine Annahme. Was als Datenverarbeitung zaehlt:
   - Crash-Logs / Crashlytics / Sentry (IP-Adresse + Device-IDs)
   - Analytics (Firebase Analytics, Amplitude, Mixpanel, etc.)
   - Push-Notifications (FCM Token = personenbezogen)
   - Account-/Login-System (E-Mail, Username, Auth-Token)
   - Cloud-Sync, Backup-Server, Server-API-Calls
   - Werbe-IDs (Advertising ID), Ads SDKs
   - WebView mit Login/Cookie-Tracking
   - Datenbank-Backups in Cloud-Speicher
   - Server-Logs auf Backend (auch IP)

## WICHTIG — Wenn keine Datenverarbeitung nachgewiesen werden kann

Wenn der Skill nach gruendlicher Repo-Pruefung KEINE der oben genannten Datenverarbeitungen
findet (echte 100%-Offline-App, keine Permissions ausser ggf. STORAGE, kein Backend, keine
SDKs ausser Build-Tools), dann darf die App in UK bleiben — **es gibt keinen Befund und
keine Empfehlung zum Ausschluss**. Im Bericht wird notiert:

> "UK-Vertreter-Pflicht (Art. 27) greift hier nicht, weil im Repo keine personenbezogene
> Datenverarbeitung von UK-Nutzern nachweisbar ist. UK darf in Country Availability bleiben."

## Pflicht-Pruefung im Repo (vor jeder UK-Empfehlung)

Skript: `scripts/check-uk-data-processing.sh [APP_DIR]`

Inline-Pruefung (falls Skript nicht verfuegbar):
```sh
# Datenverarbeitung indirekt nachweisen
rg -n -i "firebase|crashlytics|analytics|sentry|amplitude|mixpanel|fcm|admob|adsense|advertising[-_ ]id|ad_id|ads-sdk" [APP_DIR]
# Netzwerk-/Backend-Aktivitaet
rg -n -i "INTERNET|http://|https://|retrofit|okhttp|ktor|api-key|bearer" [APP_DIR]
# Account-/Login-Indikatoren
rg -n -i "login|signup|signin|account|firebase[-_ ]auth|google[-_ ]signin" [APP_DIR]
# Cloud-Sync / Backup
rg -n -i "drive|dropbox|s3|gcs|cloud[-_ ]sync|backup[-_ ]server|sync[-_ ]server" [APP_DIR]
```

Wenn EINE dieser Suchen Treffer in produktivem Code ergibt → Datenverarbeitung gegeben →
Trigger 3 erfuellt → Option B empfehlen.

## Pflicht-Handlung des Skills (NUR wenn alle 3 Trigger erfuellt)

Wenn alle drei Trigger zutreffen, MUSS der Skill in den Bericht aufnehmen:

- Befund-Schweregrad: **🟠 HOCH** (mindestens) bzw. **🔴 BLOCKER** falls die
  App bereits live in UK ist und kein Vertreter benannt ist
- Empfehlung: **Option B — UK aus Country Availability entfernen**
- Konkrete Schritte fuer Play Console (UK abhaken, Release-Notes pruefen)
- Hinweis fuer Datenschutzerklaerung: Wenn UK ausgeschlossen wird, MUSS sichergestellt
  sein dass die DSE keine UK-Vertreter-Sektion mehr enthaelt (sonst Widersprueche)
- Hinweis fuer Store-Listing-Sprachen: UK-Englisch (en-GB) als Pflichtsprache faellt weg
- Hinweis: Auch andere Distribution-Kanaele pruefen (GitHub Releases, F-Droid,
  Amazon Appstore, Galaxy Store, Sideload-Anleitungen) — UK MUSS dort ebenfalls
  ausgeschlossen werden, sonst greift Art. 27 trotzdem
- Wenn der Benutzer ausdruecklich Option A waehlt: Vermerken aber NICHT eigenstaendig
  Vertreter-Daten in die DSE eintragen (juristische Pruefung noetig)

## Pruefung im Repo

```sh
# Privacy Policy auf "UK Representative" / "UK-Vertreter" pruefen
rg -n -i "uk[ -]representative|uk[ -]vertreter|article[ ]27|art\\.[ ]27|gdprlocal|verasafe|captaincompliance" [APP_DIR]
# Country availability / Distribution-Listen
rg -n -i "country[ -]availability|distribution|target[ -]markets|countries" [APP_DIR] -g "*.md" -g "*.txt" -g "*.yaml" -g "*.yml" -g "fastlane/**"
```

## Was NIEMALS passieren darf

- App in UK ausliefern, ohne Vertreter UND ohne Ausschluss
- Vertreter-Daten erfinden oder Platzhalter einsetzen
- Pseudo-Adresse (z.B. die eines Kollegen ohne Vertretungsvereinbarung)
- Annahme "App sammelt keine Daten" ohne harte Pruefung — Crash-Logs/IP/Token zaehlen schon

## Empfehlung in der Berichts-Box

> UK ist nicht im Country-Availability-Set freigegeben. Wenn UK ausgerollt werden
> soll, ist Option A (UK-Vertreter buchen) noetig — bis dahin gilt Option B
> (UK ausgeschlossen) als verbindliche Standard-Loesung dieses Skills.
