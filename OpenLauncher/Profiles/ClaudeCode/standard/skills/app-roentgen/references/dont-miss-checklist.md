# Don't-Miss-Checkliste: 92 Punkte fuer 100% Coverage

## Zweck

Diese Checkliste ist die letzte Verteidigungslinie. Bevor der Audit-Bericht als "fertig" markiert wird, MUSS jeder Punkt geprueft sein. Jeder ungeprueft markierte Punkt ist eine Luecke im Audit.

Format pro Punkt: **Status [JA / NEIN / NICHT_VERIFIZIERT (Grund)]**.

## Block A — Manifest (8 Punkte)

```
[ ] A1. AndroidManifest.xml gelesen und alle Permissions katalogisiert?
[ ] A2. Alle Activities mit Intent-Filtern dokumentiert?
[ ] A3. Alle Services dokumentiert (besonders FCM, MediaSession, AccessibilityService)?
[ ] A4. Alle BroadcastReceiver dokumentiert (besonders BOOT_COMPLETED)?
[ ] A5. Alle ContentProvider dokumentiert (besonders FileProvider, custom)?
[ ] A6. Alle Deep-Links und URL-Schemes katalogisiert?
[ ] A7. Backup-Konfiguration geprueft (allowBackup, backup_rules.xml)?
[ ] A8. Mehrere Manifest-Varianten geprueft (debug, staging, release, library-modules)?
```

## Block B — Dependencies (6 Punkte)

```
[ ] B1. Alle Dependencies aus build.gradle.kts und libs.versions.toml extrahiert?
[ ] B2. Capability-Mapping pro Dependency durchgefuehrt?
[ ] B3. Tote Dependencies identifiziert (drin aber kein Import gefunden)?
[ ] B4. Build-Variants und Flavors geprueft?
[ ] B5. Plugin-Liste extrahiert (Firebase, Hilt, Crashlytics, etc.)?
[ ] B6. Werbung-SDKs gefunden? Wenn ja, Werbe-aussagen besonders streng pruefen.
```

## Block C — Architektur (7 Punkte)

```
[ ] C1. Alle ViewModels gelistet mit ihren Public-Funktionen?
[ ] C2. Alle Repositories und Datenquellen kartographiert?
[ ] C3. Alle UseCases / Interactors gelistet?
[ ] C4. Hilt-Module mit Provided-Klassen dokumentiert?
[ ] C5. Room-Datenmodell (Entities, DAOs) komplett?
[ ] C6. Alle Workers (WorkManager) gelistet mit Trigger und Constraints?
[ ] C7. Sealed-Class-States komplett extrahiert?
```

## Block D — Bildschirme und User-Flows (8 Punkte)

```
[ ] D1. Vollstaendiger Compose-Screen-Index (jeder @Composable Screen erfasst)?
[ ] D2. Komplette Navigation-Graph-Darstellung (Mermaid + Baum)?
[ ] D3. Pro Screen: alle Click-Handler dokumentiert?
[ ] D4. Pro Screen: alle Side-Effects (LaunchedEffect, etc.) dokumentiert?
[ ] D5. Alle Dialoge und Bottom-Sheets als Sub-Flows erfasst?
[ ] D6. Onboarding-Flow Schritt-fuer-Schritt mit Werbeaussagen pro Seite?
[ ] D7. BackHandler-Verhalten pro Screen dokumentiert?
[ ] D8. Externe Entry-Points (Deep-Links, Widget-Tap, Notification-Tap, Share-Receiver) erfasst?
```

## Block E — Paywall (10 Punkte — am wichtigsten!)

```
[ ] E1. Alle Paywall-Bildschirme inventarisiert (Haupt, Onboarding, Trial, Promo, Win-Back, Renewal, Soft, Hard)?
[ ] E2. Pro Paywall: alle Plaene mit Preisen und Trials dokumentiert?
[ ] E3. Pro Paywall: alle 10 Pflichtangaben geprueft (Preis, Intervall, Auto-Renewal, Cancel-Info, etc.)?
[ ] E4. Alle 7 Subscription-States geprueft ob im Code behandelt?
[ ] E5. Cancel-Flow vollstaendig (Klick-Anzahl, Survey, Bestaetigung, Win-Back)?
[ ] E6. Trial-Mechanik dokumentiert (Free-Trial vs Intro-Pricing, Eligibility)?
[ ] E7. Server-Side Subscription-Validation geprueft (Cloud Function, RTDN)?
[ ] E8. Edge-Cases geprueft (acknowledge, PENDING, ITEM_ALREADY_OWNED, BILLING_UNAVAILABLE)?
[ ] E9. Restore-Purchase-Button vorhanden und funktional?
[ ] E10. Cancel-Button-Visualitaet kein Dark Pattern (gleich gross wie Purchase)?
```

## Block F — Hidden Features (10 Punkte)

```
[ ] F1. Alle WorkManager-Worker dokumentiert?
[ ] F2. Alle Widgets (App-Widget) erfasst?
[ ] F3. Alle Quick-Settings-Tile-Services erfasst?
[ ] F4. Alle App-Shortcuts gelistet?
[ ] F5. Alle Notification-Channels dokumentiert?
[ ] F6. Accessibility-Service vorhanden? Falls ja, kritisch markiert?
[ ] F7. Boot-Auto-Start (RECEIVE_BOOT_COMPLETED Receiver)?
[ ] F8. Feature-Flags / Remote Config Keys mit Defaults dokumentiert?
[ ] F9. Debug-Menus / Long-Click-Trigger gefunden?
[ ] F10. A/B-Tests / Cohort-Variants identifiziert?
```

## Block G — Compliance und Privacy (6 Punkte)

```
[ ] G1. Account-Deletion in der App vorhanden? (DSGVO Art. 17, Google Play 2024)
[ ] G2. Account-Deletion Web-URL vorhanden? (Pflicht seit 2024)
[ ] G3. Datenschutzerklaerung-URL gesetzt und erreichbar?
[ ] G4. DSGVO-relevante Permissions in DS-Erklaerung erwaehnt? (Standort, Kontakte, Health, AD_ID, etc.)
[ ] G5. Cookie/Consent-Banner bei EU-Nutzern (falls Web-Komponente)?
[ ] G6. Cancel-Survey-Reasons werden DSGVO-konform behandelt (Erwaehnung in DS-Erklaerung)?
```

## Block H — Werbeaussagen-Audit (5 Punkte)

```
[ ] H1. Alle Risiko-Keywords in strings.xml geprueft?
[ ] H2. Multi-Sprach-Konsistenz fuer kritische Aussagen geprueft (alle uebersetzten Sprachen)?
[ ] H3. Store-Listing manuell geprueft (Frank beauftragt)?
[ ] H4. 6-Felder-Matrix komplett (jede Aussage mit Code-Beleg)?
[ ] H5. Risiko-Klassifizierung pro Aussage durchgefuehrt?
```

## Block I — Wortlaut-Erfassung 1:1 (8 Punkte — Grundlage fuer Rechtssicherheit)

```
[ ] I1. Fuer JEDEN Screen eine Wortlaut-Tabelle mit allen UI-Elementen erstellt (TopBar-Title, Headlines, Bodies, Buttons, ContentDescriptions)?
[ ] I2. Fuer JEDEN Dialog ALLE Slots zitiert (Title, Body, Confirm-Button, Dismiss-Button, Neutral-Button)?
[ ] I3. Fuer JEDES Bottom-Sheet alle Items mit Label + Beschreibung zitiert?
[ ] I4. Menue-Hierarchien REKURSIV ausgerollt — JEDE Untermenue-Ebene als eigene Zeile mit Breadcrumb-Pfad, egal wie tief (keine "und weitere"-Abkuerzungen)?
[ ] I5. Settings-Items einzeln dokumentiert mit Label + Beschreibung + Switch-/Dropdown-/Slider-Werten?
[ ] I6. Alle Snackbars, Toasts, Error-/Empty-/Loading-States mit 1:1-Wortlaut + Action-Button-Wortlaut zitiert?
[ ] I7. Alle Push-Notifications (Channel-Name, Title, Body, Action-Buttons) zitiert?
[ ] I8. Plurals, Array-Resources, Format-Strings (%s, %d) und hardcoded Strings erfasst — Differenz "Keys im Code" vs "Keys in strings.xml" dokumentiert?
```

## Block J — Translation-Context (8 Punkte — Grundlage fuer Uebersetzungs-Skill)

```
[ ] J1. Slot-Laengen-Audit durchgefuehrt — alle Wortlaute gegen UI-Slot-Maxlaenge geprueft, Ueberlaenge markiert?
[ ] J2. translatable="false" Strings erfasst und auf Plausibilitaet geprueft (Markennamen, URLs, Versionen)?
[ ] J3. xliff:g-Tags erfasst — xmlns:xliff korrekt deklariert? Format-Strings ohne xliff:g als Kandidaten markiert?
[ ] J4. XML-Kommentare (<!-- ... -->) als Uebersetzer-Notizen erfasst, fehlende Notes bei Format-Strings flagged?
[ ] J5. CLDR-Plural-Vollstaendigkeit pro Sprache geprueft — fehlende Quantitaeten (one/few/many/other/zero/two) pro Sprache aufgelistet?
[ ] J6. HTML/CDATA/Entity-Inhalte erfasst und Uebersetzer-Hinweis aufgenommen?
[ ] J7. Format-Argumente semantisch dokumentiert (%1$s = was, %2$d = was), Argument-Anzahl pro String geprueft?
[ ] J8. Glossar-Begriffe automatisch erkannt (Top-30 Substantive), Konsistenz-Inkonsistenzen aufgelistet, Region-Differenzen + Du/Sie-Konsistenz geprueft?
```

## Block K — Legal-Text-Inventar (10 Punkte — Grundlage fuer Rechtssicherheits-Skill)

```
[ ] K1. Permission-Rationale-Dialoge pro Runtime-Permission erfasst (Title/Body/Allow/Deny/Settings-Verweis)?
[ ] K2. Consent-Banner-Wortlaut erfasst — "Akzeptieren" und "Ablehnen" gleich prominent?
[ ] K3. AGB-, Datenschutz-, Impressums-, Widerruf-Links erfasst und auf Erreichbarkeit geprueft (HTTP 200)?
[ ] K4. Health-Disclaimer-Texte erfasst (falls Health-/Fitness-/Mental-Health-App)?
[ ] K5. AI-Disclaimer-Texte erfasst (falls KI-SDK-Aufrufe vorhanden) — pro Antwort + global?
[ ] K6. Werbe-Markierungen ("Werbung"/"Anzeige"/"Gesponsert") erfasst (falls Ad-SDK)?
[ ] K7. Account-Deletion-Flow vollstaendig zitiert (Settings-Item, Confirm-Dialog mit Wort "unwiderruflich", Erfolgsmeldung)?
[ ] K8. Newsletter-Opt-In-Texte erfasst (falls Newsletter) — Double-Opt-In-Hinweis enthalten?
[ ] K9. In-App-Kauf-Confirmation + Widerrufsbelehrung-Texte erfasst (Pflichthinweise vor Kauf)?
[ ] K10. Standort-Begruendung + Altersfreigabe-Anzeige erfasst (falls relevant)?
```

## Block L — Externe Inhalte (6 Punkte — Audit ueber die App hinaus)

```
[ ] L1. Google Play Store Listing (Title, Short/Long Description, Screenshot-Texte) — pro Sprache erfasst?
[ ] L2. Firebase Remote Config Defaults + Live-Werte (Frank-Aufgabe) dokumentiert?
[ ] L3. Cloud Functions Notification-Templates erfasst (falls Functions-Code im Repo)?
[ ] L4. Email-Templates (Firebase Auth, Stripe) — Frank-Aufgabe dokumentiert?
[ ] L5. WebView-Inhalte (HTML in assets/, externe URLs, PDF-Vorlagen) erfasst?
[ ] L6. Marketing-Materialien (Webseite, Promo-Videos, Social-Media, Newsletter-Archiv) als Frank-Aufgabe gelistet?
```

## Markier-Beispiele

```markdown
[X] A1. AndroidManifest.xml gelesen und alle Permissions katalogisiert?
       → 18 Permissions gefunden, siehe Schicht 1.1

[ ] A8. Mehrere Manifest-Varianten geprueft?
       → NICHT_VERIFIZIERT — kein staging-Manifest gefunden, nur main + debug. OK.

[X] E1. Alle Paywall-Bildschirme inventarisiert?
       → 7 Paywall-Bildschirme gefunden:
         - PaywallScreen (Haupt)
         - OnboardingPaywall
         - ChurnFlowDialog (Cancel-Survey)
         - WinBackBanner
         - GracePeriodBanner
         - AccountHoldScreen
         - PausedSubscriptionInfo

[ ] G2. Account-Deletion Web-URL vorhanden?
       → KRITISCH FEHLT — Frank muss Datenschutzerklaerung um Web-URL fuer Konto-Loeschung ergaenzen
```

## Selbst-Pruefung am Ende

Wenn die Checkliste durch ist:
- 92 Punkte gesamt
- Wie viele [X]: ___
- Wie viele [ ] mit "NICHT_VERIFIZIERT (Grund)": ___ — diese sind OK wenn der Grund nachvollziehbar ist
- Wie viele [ ] ohne Begruendung: ___ — sollte 0 sein

Nur wenn alle 92 Punkte entweder geprueft oder mit Begruendung als "nicht zutreffend" markiert sind, gilt der Audit als vollstaendig.
