# 6-Phasen-Fahrplan: ALLE Projekte + Android-Spezifika (KRITISCH)

> **Diese Regel gilt fuer JEDES Projekt das in diesem System entwickelt wird.**
> **Claude MUSS immer wissen und kommunizieren in welcher Phase sich das Projekt befindet.**
> **Phasen duerfen NICHT uebersprungen werden. Jede Phase hat klare Eingangs- und Ausgangskriterien.**
> **Fuer Android-Projekte enthaelt dieses Dokument zusaetzliche Details (R8, ProGuard, Billing, etc.).**
> **Fuer andere Projekttypen gelten die generischen Abschnitte.**

## Plattform-spezifische Haertung (Phase 3 — andere Projekttypen als Android)

| Projekttyp | Phase-3-Besonderheiten |
|------------|----------------------|
| **C#/WPF** | `dotnet publish -c Release`, Trimming pruefen (aehnlich R8!), Code-Signing, Installer (WiX/InnoSetup/MSIX) |
| **Swift/macOS** | `xcodebuild -configuration Release`, Notarization, Hardened Runtime, DMG/ZIP |
| **Web/TypeScript** | `npm run build`, Bundle-Groesse, Environment Vars, Security Headers, SSL |
| **Rust** | `cargo build --release`, Clippy streng, Binary-Groesse, Cross-Compilation |
| **Go** | `go build -ldflags="-s -w"`, go vet + staticcheck, Cross-Compilation |
| **Tampermonkey** | Version-Bump, console.log entfernen, @match pruefen, @grant minimieren |

## Phase 4: Vermarktung (andere Projekttypen)

| Projekttyp | Recherche-Fokus |
|------------|----------------|
| **C#/WPF** | Windows-Distribution (Microsoft Store vs. Website vs. Winget), Installer-Practices |
| **Swift/macOS** | Mac App Store ASO, Notarization-Workflow, Indie-Mac-Communities |
| **Web/TypeScript** | SEO, Product Hunt, Indie Hackers, Landing Page Best Practices |
| **Tampermonkey** | GreasyFork/OpenUserJS Listing, Reddit-Communities |
| **CLI-Tool** | Package Manager (npm/cargo/brew), GitHub Releases, awesome-lists |
| **Open Source** | GitHub-Sichtbarkeit, README-Design, Contributing Guide |

## Phasen-Uebersicht

| Phase | Name | Kernaufgabe | Dauer (typisch) |
|-------|------|-------------|-----------------|
| **1** | Projekt-Setup | Infrastruktur aufbauen | 1 Session |
| **2** | Debug & Entwicklung | Alle Features bauen und testen | Wochen bis Monate |
| **3** | R8-Haertung & Release-Build | App release-faehig machen | 1-3 Sessions |
| **4** | Vermarktungsstrategie | App "an den Mann bringen" planen | 1-2 Sessions |
| **5** | Store-Veroeffentlichung | Upload und Go-Live | 1 Session |
| **6** | Updates & Wartung | Bugfixes, neue Features | Laufend |

---

## Phasen-Anzeige (PFLICHT bei jeder Aufgabe)

Bei JEDER Aufgabe die ein Android-Projekt betrifft MUSS Claude am Anfang der Antwort
den aktuellen Phasen-Status anzeigen:

```
📱 Phase 2 (Debug) — BestJournalAndroid v1.0.3 (Build 4)
   Feature: Suchfunktion | Geraet: Samsung S24
```

Format:
```
📱 Phase [N] ([Name]) — [Projektname] v[Version] (Build [N])
   [Aktuelle Aufgabe] | [Relevanter Kontext]
```

Wenn das Projekt KEINE Phase hat (z.B. erstes Mal erwaehnt), MUSS Claude fragen:
"In welcher Phase ist dieses Projekt? Soll ich den Status in SESSION-RULES.md eintragen?"

---

## Phase 1: Projekt-Setup

### Was passiert
- Ordnerstruktur in `~/proggs/[Projektname]/` anlegen
- Gradle mit Version Catalog (`libs.versions.toml`) einrichten
- Firebase-Projekt in der Google Cloud Console erstellen
- **Keystore SOFORT erstellen** — nicht erst vor dem Release!
- `google-services.json` herunterladen und in `app/` ablegen
- SHA-1 und SHA-256 Fingerprints dokumentieren (Debug UND Release)
- `.gitignore` einrichten (Keystore, google-services.json, local.properties)
- `SESSION-RULES.md` anlegen mit allen Projekt-Parametern
- Erster Commit: leere App die kompiliert

### Eingangs-Kriterium
- Benutzer sagt "neue Android App" oder "neues Projekt"

### Ausgangs-Kriterium (ALLE muessen erfuellt sein)
- [ ] App kompiliert fehlerfrei (`./gradlew assembleDebug`)
- [ ] Keystore existiert und ist NICHT im Repo
- [ ] SHA-Fingerprints in SESSION-RULES.md dokumentiert
- [ ] Firebase-Projekt erstellt (wenn Firebase genutzt wird)
- [ ] google-services.json vorhanden und gitignored
- [ ] local.properties mit Signing-Daten befuellt
- [ ] Erster Commit gepusht

### Was Claude NICHT tun darf
- Kein Feature-Code schreiben
- Keine UI bauen
- Keine Libraries hinzufuegen die nicht zur Grundstruktur gehoeren

### Keystore-Erstellung (Vorlage)
```bash
keytool -genkey -v \
  -keystore [projektname]-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias [projektname]-key \
  -storepass [SICHERES_PASSWORT] \
  -keypass [SICHERES_PASSWORT] \
  -dname "CN=[Name], O=[Organisation], L=[Stadt], ST=[Bundesland], C=DE"
```

### SHA-Fingerprint-Dokumentation (PFLICHT)
In SESSION-RULES.md muessen stehen:
```
## Signing & Fingerprints
- Debug SHA-1: [aus ~/.android/debug.keystore]
- Debug SHA-256: [aus ~/.android/debug.keystore]
- Release SHA-1: [aus dem erstellten Release-Keystore]
- Release SHA-256: [aus dem erstellten Release-Keystore]
- Keystore-Speicherort: [NICHT im Repo — externer Pfad]
- Key-Alias: [projektname]-key
```

Befehle:
```bash
# Debug-Fingerprints:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android

# Release-Fingerprints:
keytool -list -v -keystore [PFAD_ZUM_KEYSTORE] -alias [ALIAS]
```

Diese Fingerprints muessen dann in Firebase Console UND Google Cloud Console
als OAuth-Client eingetragen werden.

---

## Phase 2: Debug & Entwicklung

### Was passiert
- ALLE Features implementieren: UI, Logik, Datenbank, API-Anbindung
- Immer `assembleDebug` — NIEMALS `assembleRelease`
- R8/ProGuard ist AUS (`isMinifyEnabled = false`)
- Testen auf echten Geraeten ueber ADB
- Jedes Feature einzeln committen und pushen
- Version hochzaehlen bei jedem sinnvollen Meilenstein

### Regeln fuer Phase 2
1. **Kein R8**: `isMinifyEnabled = false` in ALLEN Build-Types
2. **Kein Release-Build**: Nur `assembleDebug` oder `installDebug`
3. **Kein Billing-Test**: In-App-Kaeufe und Abos werden in Phase 2 NICHT getestet
   (Billing braucht Release-Signing oder Test-Tracks, das gehoert in Phase 3)
4. **Debug-Suffix aktiv**: `applicationIdSuffix = ".debug"` damit Debug und Release
   parallel auf dem Geraet installiert sein koennen
5. **Feature-Branches optional**: Bei kleinen Features direkt auf main,
   bei grossen Features eigener Branch empfohlen

### Neue Features waehrend Phase 2 (NORMAL und ERWUENSCHT)
Phase 2 ist OFFEN — der Benutzer wird waehrend der Arbeit neue Feature-Ideen haben.
Das ist kein Problem, das ist der kreative Prozess. Claude:
- Nimmt neue Feature-Wuensche jederzeit entgegen
- Baut sie ein ohne den "Plan" als starr zu behandeln
- Zaehlt den Feature-Stand entsprechend hoch
- Fragt NICHT "aber das war nicht geplant" — es ist Phase 2, alles darf

### Wann ist Phase 2 fertig?
**NUR der Benutzer entscheidet wann Phase 2 Richtung R8 geht.**
Claude darf NICHT sagen "ich glaube wir sind fertig" oder "sollen wir zu Phase 3?".
Der Benutzer wird klar kommunizieren: "Phase 2 ist fertig" oder "wir koennen zu R8".

**WICHTIG: Phase 2 hat einen EXPLIZITEN letzten Schritt — die R8-Vorbereitung.**
Wenn der Benutzer sagt "R8", "Store", "Release" oder aehnliches:
1. Claude fragt: "Soll Phase 2 damit abgeschlossen sein?"
2. Wenn der Benutzer JA sagt, beginnt der **R8-Vorbereitungs-Scan** (siehe unten)
3. Phase 2 ist ERST abgeschlossen wenn die App R8-resistent vorbereitet ist
4. Erst DANACH wechseln wir zu Phase 3 (wo R8 tatsaechlich eingeschaltet wird)

### Letzter Schritt Phase 2: R8-Vorbereitungs-Scan (PFLICHT vor Phase 3)

> **Dieser Scan passiert NOCH IN Phase 2 — er ist der Abschluss, nicht der Anfang von Phase 3.**
> **Ziel: Die App wird R8-resistent VORBEREITET, bevor R8 ueberhaupt eingeschaltet wird.**
> **Bei BestJournal hat genau dieser Schritt gefehlt — R8 wurde eingeschaltet und alles crashte.**

Wenn der Benutzer bestaetigt dass Features komplett sind, sagt Claude:

```
📱 Phase 2 (Debug) — Letzter Schritt: R8-Vorbereitung

Bevor wir R8 einschalten, mache ich jetzt einen vollstaendigen Scan der App.
Ich pruefe JEDE Stelle die R8 kaputt machen koennte und bereite die App vor,
damit der Release-Build in Phase 3 beim ersten Mal funktioniert.

Das umfasst:
1. JNI/Native Libraries → Keep-Regeln schreiben
2. Reflection/Serialisierung → Keep-Regeln schreiben
3. Third-Party-Libraries → bekannte R8-Probleme pruefen
4. Billing/Firebase → spezielle Keep-Regeln
5. Alle ProGuard-Regeln in proguard-rules.pro eintragen

R8 wird dabei NICHT eingeschaltet — nur die Vorbereitung.
Wenn alles vorbereitet ist, ist Phase 2 abgeschlossen und Phase 3 beginnt.
```

#### Scan-Ablauf (alles noch mit isMinifyEnabled = false!)

**1. JNI/Native Libraries scannen**
```
Grep nach: System.loadLibrary, extern, native, .so-Dateien
→ Fuer jede gefundene: -keep class mit allen Feldern und Methoden
```

**2. Reflection-Nutzung scannen**
```
Grep nach: Class.forName, .getDeclaredField, .getDeclaredMethod,
           @SerializedName, @Json, @Keep, @Entity, @Dao
→ Fuer jede gefundene: -keep-Regel oder @Keep-Annotation
```

**3. Serialisierung scannen**
```
Grep nach: Moshi, Gson, Kotlinx.Serialization, Retrofit-Interfaces
→ Fuer jede Model-Klasse: -keep mit allen Feldern
→ Fuer Retrofit-Interfaces: -keep interface
```

**4. Third-Party-Libraries pruefen**
```
Jede Library in libs.versions.toml einzeln pruefen:
- Hat die Library eigene ProGuard-Regeln? (consumer-rules.pro)
- Braucht sie zusaetzliche -keep-Regeln?
- Bekannte R8-Probleme? (GitHub Issues der Library pruefen)
```

**5. Android-Komponenten scannen**
```
Grep nach: BroadcastReceiver, ContentProvider, Service, Activity die
           nicht im Manifest stehen oder dynamisch registriert werden
```

**6. Billing/Payment scannen (wenn vorhanden)**
```
- Google Play Billing AIDL-Interfaces
- BillingClient Callback-Klassen
- Purchase/SkuDetails-Klassen
```

**7. Firebase scannen (wenn vorhanden)**
```
- Firebase SDK consumer-rules verifizieren
- AppCheck Provider-Klassen
- Crashlytics-Mapping
```

**8. ProGuard-Regeln schreiben**
Alle gefundenen Risiken als `-keep`-Regeln in `proguard-rules.pro` eintragen.
Jede Regel bekommt einen Kommentar WARUM sie da ist:
```
# ===== R8-Vorbereitungs-Scan vom [Datum] =====
# Gefundene Risiken: [Anzahl]
# JNI: [Liste]
# Reflection: [Liste]
# Serialisierung: [Liste]
# Third-Party: [Liste]
# ==============================================

# Sherpa-ONNX: JNI-Klassen — R8 wuerde native Felder strippen
-keep class com.k2fsa.sherpa.onnx.** { *; }
```

**9. Scan-Ergebnis dem Benutzer zeigen**
```
✅ R8-Vorbereitungs-Scan abgeschlossen:
   - [N] JNI-Risiken gefunden und abgesichert
   - [N] Reflection-Stellen abgesichert
   - [N] Third-Party-Libraries geprueft
   - [N] ProGuard-Regeln geschrieben
   
   Phase 2 ist damit abgeschlossen.
   ➡️ Phase 3: R8 wird jetzt eingeschaltet und der Release-Build getestet.
```

### Was Claude kommunizieren muss
- Bei jedem Feature: "Phase 2 (Debug) — arbeite an Feature X"
- Bei Version-Bumps: "Version hochgezaehlt auf v0.X.Y (Build N)"
- Wenn der Benutzer "R8", "Store" oder "Release" erwaehnt:
  Nachfragen: "Soll Phase 2 damit abgeschlossen sein?"
- Wenn Benutzer JA sagt: R8-Vorbereitungs-Scan starten (letzter Schritt Phase 2)

### Ausgangs-Kriterium
- [ ] **Benutzer sagt explizit**: "Phase 2 ist fertig" / "wir koennen zu R8" / "Features sind komplett"
- [ ] **R8-Vorbereitungs-Scan durchgefuehrt** (PFLICHT — Phase 2 ist NICHT fertig ohne diesen Scan)
- [ ] **ProGuard-Regeln geschrieben** fuer alle gefundenen Risiken
- [ ] App laeuft stabil auf mindestens einem echten Geraet (Debug)
- [ ] Keine bekannten Crashes oder schweren Bugs

### Was Claude NICHT tun darf
- Keinen Release-Build starten
- R8 nicht einschalten
- Billing/Subscriptions nicht testen
- Nicht zum Store hochladen
- Nicht an Vermarktung denken (das kommt in Phase 4)

---

## Phase 3: R8-Haertung & Release-Build

> **Phase 3 beginnt NACHDEM Phase 2 abgeschlossen ist — inklusive R8-Vorbereitungs-Scan.**
> **Der Scan und die ProGuard-Regeln wurden bereits in Phase 2 geschrieben.**
> **Phase 3 schaltet R8 EIN und testet ob die Vorbereitung funktioniert hat.**

### Eintritts-Ankuendigung (PFLICHT)

```
🔧 Phase 3: R8-Haertung & Release-Build

Die R8-Vorbereitung aus Phase 2 ist abgeschlossen. Alle ProGuard-Regeln stehen.
Jetzt schalten wir R8 ein und pruefen ob alles haelt.

Schritte:
1. R8 einschalten (isMinifyEnabled = true)
2. Release-Build erstellen
3. Auf echtem Geraet testen — JEDEN Screen
4. Bei Crash: fehlende Keep-Regel ergaenzen und erneut bauen
5. Billing/Abos im Release-Build testen

Wenn alles stabil laeuft, ist die App release-faehig.
```

### Schritt 3a: R8 einschalten und Release-Build

```bash
./gradlew assembleRelease
```

- Bei Erfolg: APK auf echtem Geraet installieren und JEDEN Screen testen
- Bei Crash: Stacktrace analysieren, fehlende Keep-Regel ergaenzen, erneut bauen
- **Maximal 3 Iterationen** — wenn danach noch Crashes: zurueck zu Schritt 3a

### Schritt 3d: Billing/Subscription testen (wenn vorhanden)

- Test-Accounts in Google Play Console einrichten
- APK ueber Internal Testing Track hochladen
- Kauf-Flow testen: Abschluss, Kuendigung, Wiederherstellung
- Alle Freemium-Grenzen testen (Gratis-Limit, Abo-Aktivierung)

### Ausgangs-Kriterium
- [ ] Release-APK kompiliert fehlerfrei mit R8
- [ ] App laeuft stabil auf echtem Geraet (Release-Build)
- [ ] Alle Features funktionieren im Release-Build
- [ ] Billing funktioniert (wenn vorhanden)
- [ ] ProGuard-Regeln vollstaendig dokumentiert
- [ ] APK-Groesse geprueft und akzeptabel

### Was Claude NICHT tun darf
- Keine neuen Features einbauen
- Nicht zum Store hochladen
- Nicht an Vermarktung denken (noch nicht)

---

## Phase 4: Vermarktungsstrategie

> **KEINE Veroeffentlichung ohne Vermarktungsstrategie.**
> **Ziel: Die App soll GEFUNDEN werden, HERUNTERGELADEN werden, und GELD verdienen.**
> **Diese Phase ist fuer Solo-Entwickler (1-Mann-Unternehmer) ohne grosses Budget optimiert.**
> **Claude leitet den Benutzer AKTIV durch jeden Schritt — nicht passiv abwarten.**

### Eintritts-Ankuendigung (PFLICHT)

Wenn Phase 3 abgeschlossen ist, MUSS Claude folgendes sagen:

```
🎯 Phase 3 (R8) ist abgeschlossen. Die App laeuft stabil im Release-Build.

Jetzt kommt Phase 4: Vermarktungsstrategie.

Bevor wir irgendwas in den Store hochladen, machen wir einen Plan wie die App
GEFUNDEN und HERUNTERGELADEN wird. Eine App ohne Vermarktung ist wie ein Laden
ohne Schild — niemand weiss dass es ihn gibt.

Ich starte jetzt eine gruendliche Recherche zu:
- Wie Solo-Entwickler 2025/2026 erfolgreich Apps vermarkten
- Welche kostenlosen/guenstigen Strategien wirklich funktionieren
- Was die Top-Apps in deiner Kategorie anders machen
- Wie du mit null Budget trotzdem Sichtbarkeit bekommst

Das dauert etwa 5-10 Minuten. Danach gehen wir Schritt fuer Schritt durch.
```

### Schritt 4a: Pflicht-Recherche (MUSS vor allem anderen passieren)

> **Claude MUSS bei Eintritt in Phase 4 eine gruendliche Internetrecherche durchfuehren.**
> **Keine Vermarktungsentscheidung ohne aktuelle Daten. Kein Bauchgefuehl, sondern Fakten.**

**WICHTIG: Die Recherche muss STATE OF THE ART sein.**
Die Recherche wird NICHT vorab gemacht und gespeichert. Sie wird genau DANN durchgefuehrt
wenn Phase 4 beginnt — damit die Ergebnisse zum Zeitpunkt der Vermarktung aktuell sind.
Marketing-Strategien aendern sich schnell (Algorithmus-Updates, neue Plattformen, neue Tools).
Was heute funktioniert kann in 3 Monaten veraltet sein. Deshalb: IMMER frisch recherchieren.

Die Researcher suchen EXPLIZIT nach den **aktuellsten Entwicklungen** (letztes halbes Jahr):
- Neue Play Store Algorithmus-Aenderungen
- Neue Marketing-Kanaele die gerade aufkommen
- Aktuelle Conversion-Benchmarks und Preisdaten
- Neue Tools fuer Solo-Entwickler
- Aktuelle Erfolgsgeschichten (nicht 3 Jahre alte Blogposts)

Claude startet 3-5 parallele Researcher-Agents (Sonnet, je max 15 Web-Fetches) zu diesen Themen:

#### Researcher 1: Play Store Algorithmus & ASO (App Store Optimization)
- Wie funktioniert der Play Store Ranking-Algorithmus aktuell (2025/2026)?
- Welche Faktoren beeinflussen die Sichtbarkeit am meisten?
- Welche kostenlosen ASO-Tools gibt es fuer Keyword-Recherche?
- Best Practices fuer Titel (30 Zeichen), Kurzbeschreibung (80 Zeichen), Langbeschreibung
- Wie funktionieren Custom Store Listings und A/B-Tests im Play Store?
- Welche Kategorien haben wenig Konkurrenz aber hohe Nachfrage?

#### Researcher 2: Solo-Entwickler Erfolgsstrategien
- Wie haben erfolgreiche Solo-Entwickler ihre erste App vermarktet?
- Welche kostenlosen Marketing-Kanaele funktionieren am besten? (Reddit, Product Hunt,
  Indie Hackers, Twitter/X, TikTok, YouTube, Blogs, Foren)
- Wie bekommt man die ersten 100 Downloads ohne Budget?
- Wie bekommt man die ersten 10 echten Reviews?
- Soft Launch Strategien: In welchen Laendern zuerst launchen?
- Fehler die Solo-Entwickler am haeufigsten machen

#### Researcher 3: Monetarisierung fuer 1-Mann-Unternehmen
- Freemium vs. Paid vs. Ads — was funktioniert 2025/2026 am besten fuer Solo-Devs?
- Optimale Preispunkte fuer Abo-Apps in DACH (Deutschland, Oesterreich, Schweiz)
- Wann und wie den Nutzer zum Kauf auffordern ohne ihn zu nerven?
- Conversion-Rate-Benchmarks: Was ist realistisch fuer eine neue App?
- Google Play Gebuehren: 15% fuer die erste Million — wie das nutzen?
- Steuerliche Grundlagen fuer App-Einnahmen als Einzelunternehmer in Deutschland

#### Researcher 4: Konkurrenz-Analyse (APP-SPEZIFISCH)
- Top 10 Apps in der gleichen Kategorie analysieren:
  - Wie sehen ihre Store-Listings aus?
  - Welche Keywords nutzen sie?
  - Wie sind ihre Screenshots gestaltet?
  - Was sind ihre Preise?
  - Was sagen die 1-Stern-Reviews? (= unerfuellte Beduerfnisse = deine Chance)
- Luecken im Markt finden: Was bieten die Konkurrenten NICHT?

#### Researcher 5: Visuelle Vermarktung & Conversion
- Screenshot-Design das konvertiert: Welche Layouts funktionieren am besten?
- Icon-Design-Trends 2025/2026 die im Store auffallen
- Feature-Grafik (1024x500): Best Practices
- Video-Vorschau: Lohnt sich der Aufwand fuer einen Solo-Dev?
- Lokalisierung: Welche Sprachen bringen die meisten Downloads pro Aufwand?

### Schritt 4b: Recherche-Ergebnis aufbereiten

Claude fasst die Recherche in einem **Vermarktungs-Dokument** zusammen:
`docs/marketing/[app-name]-marketing-strategy.md`

Struktur:
```markdown
# Vermarktungsstrategie: [App-Name]
## Erstellt am: [Datum]
## Basierend auf: [Anzahl] Quellen, recherchiert am [Datum]

### 1. Marktanalyse
- Kategorie und Wettbewerb
- Marktluecken und Chancen
- Zielgruppe (wer, warum, wann)

### 2. ASO-Plan
- Titel (mit Begruendung)
- Kurzbeschreibung (mit Begruendung)
- Langbeschreibung (vollstaendig ausgeschrieben)
- Keywords (primaer + sekundaer)
- Kategorie-Empfehlung

### 3. Visueller Auftritt
- Icon-Konzept
- Screenshot-Plan (welcher Screen, welcher Text, welche Reihenfolge)
- Feature-Grafik-Konzept

### 4. Preisstrategie
- Preismodell (Freemium/Paid/Hybrid)
- Konkrete Preise mit Begruendung
- Einfuehrungsangebot
- Upsell-Zeitpunkte (wann wird der Nutzer auf Abo hingewiesen)

### 5. Launch-Plan
- Soft Launch: Land, Dauer, Erfolgskriterien
- Full Launch: Zeitpunkt, Kanaele
- Erste 100 Downloads: Konkrete Schritte
- Erste 10 Reviews: Konkrete Strategie

### 6. Kostenlose Marketing-Kanaele
- Welche Plattformen (mit konkreten Subreddits, Foren, Communities)
- Content-Plan (was posten, wann, wie oft)
- Cross-Promotion-Moeglichkeiten

### 7. Langfristige Wachstumsstrategie
- Was nach dem Launch passieren muss
- Metriken die beobachtet werden (Downloads, Retention, Conversion)
- Wann und wie auf Feedback reagieren
```

### Schritt 4c: Store-Listing erstellen

Basierend auf dem Vermarktungs-Dokument erstellt Claude:

#### Titel (max 30 Zeichen)
- 3 Vorschlaege mit Begruendung warum jeder funktionieren koennte
- Benutzer waehlt aus oder kombiniert

#### Kurzbeschreibung (max 80 Zeichen)
- 3 Vorschlaege mit den wichtigsten Keywords
- Muss den Hauptnutzen in einem Satz kommunizieren

#### Langbeschreibung (max 4000 Zeichen)
- Vollstaendig ausgeschrieben, auf Deutsch UND Englisch
- Keywords natuerlich eingebaut (nicht gestopft)
- Struktur: Hook → Features → Alleinstellung → Social Proof → Call-to-Action
- Emojis sparsam aber strategisch einsetzen

#### Screenshots (mindestens 6, besser 8)
- Fuer JEDEN Screenshot: Welcher App-Screen, welcher Overlay-Text, welche Reihenfolge
- Erster Screenshot = wichtigster — muss den Hauptnutzen zeigen
- Kein reiner Screenshot — immer mit erklaerenden Text-Overlays
- Claude erstellt die Text-Overlays und Layouts als HTML-Vorschau wenn moeglich

#### Feature-Grafik (1024x500)
- Konzept mit Farbschema, Text, Motiv
- Muss auch als kleines Thumbnail erkennbar sein

#### Icon
- Falls noch kein professionelles Icon: Konzept-Vorschlag
- Kontrastreiche Farben, einfaches Motiv, kein Text im Icon

### Schritt 4d: Preisstrategie finalisieren

Claude geht mit dem Benutzer folgende Fragen durch:

1. **Freemium-Grenzen**: Sind sie so gesetzt dass der Nutzer den Wert der App
   erkennt BEVOR er bezahlen muss? (Zu frueh = Nutzer geht, zu spaet = zahlt nie)
2. **Preispunkte**: Verglichen mit Konkurrenz — sind wir im richtigen Bereich?
3. **Einfuehrungsangebot**: Erster Monat kostenlos? 50% Rabatt? Oder voller Preis?
4. **Upsell-Momente**: An welchen Stellen in der App wird dezent auf das Abo hingewiesen?
   (z.B. wenn Gratis-Limit erreicht, wenn Premium-Feature angetippt wird)
5. **Jahresabo-Rabatt**: Wie viel guenstiger als Monatsabo? (Standard: 30-40% Ersparnis)
6. **Google Play Gebuehren**: 15% auf die erste $1M/Jahr — das ist ein Vorteil gegenueber
   Apple (30%). In der Beschreibung/Pricing beruecksichtigen.

### Schritt 4e: Launch-Plan festlegen

1. **Soft Launch Land waehlen**: Deutschland (Heimatmarkt, einfach zu supporten)
2. **Soft Launch Dauer**: 2-4 Wochen mit echten Nutzern
3. **Erfolgs-Metriken definieren**:
   - Crash-Rate < 1%
   - Mindestens 4.0 Sterne Bewertung
   - Retention Day 7 > 20%
   - Mindestens 5 organische Reviews
4. **Full Launch Zeitpunkt**: Erst wenn Soft-Launch-Metriken erfuellt sind
5. **Marketing-Aktionen zum Launch**:
   - Reddit-Posts in relevanten Subreddits (r/androidapps, r/apps, nischen-spezifisch)
   - Product Hunt Launch (kostenlos, hohe Sichtbarkeit bei Tech-affinen Nutzern)
   - Indie Hackers Community Post
   - Eigene Social-Media-Kanaele (wenn vorhanden)
   - App-Review-Blogs anschreiben (kostenlose Reviews)
   - YouTube: Kurzes Demo-Video (kann mit Handy aufgenommen werden)

### Claude's Rolle in Phase 4 (AKTIVE ANLEITUNG)

Claude wartet NICHT bis der Benutzer fragt. Claude:
- **Startet die Recherche automatisch** bei Eintritt in Phase 4
- **Praesentiert Ergebnisse verstaendlich** (kein Marketing-Jargon)
- **Macht konkrete Vorschlaege** ("Ich schlage vor: Titel X, weil...")
- **Erklaert WARUM** jeder Vorschlag so ist wie er ist
- **Zeigt Beispiele** von erfolgreichen Solo-Dev-Apps
- **Warnt vor Fehlern** die Solo-Entwickler typischerweise machen
- **Erstellt die Texte** (nicht nur Tipps geben, sondern fertige Texte schreiben)
- **Rechnet Monetarisierung durch**: "Bei X Downloads und Y% Conversion verdienst du Z€/Monat"
- **Leitet durch jeden Unter-Schritt**: "Jetzt machen wir die Kurzbeschreibung. Hier sind 3 Vorschlaege..."

### Ausgangs-Kriterium (ALLE muessen erfuellt sein)
- [ ] Gruendliche Internetrecherche durchgefuehrt (5 Researcher-Bereiche)
- [ ] Vermarktungs-Dokument erstellt und vom Benutzer abgesegnet
- [ ] Store-Listing komplett (Titel, Kurz- und Langbeschreibung, DE + EN)
- [ ] Screenshots geplant und erstellt (mindestens 6)
- [ ] Feature-Grafik erstellt
- [ ] Icon finalisiert
- [ ] Preisstrategie festgelegt und durchgerechnet
- [ ] Launch-Plan definiert (Soft Launch Land, Dauer, Metriken, Marketing-Kanaele)
- [ ] Datenschutzerklaerung geschrieben (PFLICHT)
- [ ] Benutzer hat den GESAMTEN Plan abgesegnet

### Was Claude NICHT tun darf
- ❌ Veroeffentlichen ohne dass der Benutzer den Plan abgesegnet hat
- ❌ "Standard"-Texte verwenden — alles muss auf die spezifische App zugeschnitten sein
- ❌ Die Monetarisierung ueberspringen ("kostenlos reicht erstmal" — NEIN)
- ❌ Recherche ueberspringen ("ich weiss schon wie Marketing geht" — NEIN, aktuelle Daten holen)
- ❌ Marketing-Jargon benutzen ohne es zu erklaeren
- ❌ Den Benutzer allein lassen ("hier sind Tipps, mach mal" — NEIN, gemeinsam durcharbeiten)
- ❌ Unrealistische Versprechen machen ("10.000 Downloads im ersten Monat")

---

## Phase 5: Store-Veroeffentlichung

### Was passiert
- Google Play Console: App-Eintrag erstellen
- Store-Listing eintragen (aus Phase 4)
- Content Rating Fragebogen ausfuellen
- Datenschutzerklaerung verlinken (PFLICHT)
- App-Bundle (AAB) hochladen — NICHT APK
- Internal Testing → Closed Testing → Open Testing → Production
- Review durch Google abwarten (typisch 1-7 Tage)

### Schritt-fuer-Schritt
1. **AAB erstellen**: `./gradlew bundleRelease`
2. **Play Console**: Neuer App-Eintrag
3. **Store-Listing**: Texte und Grafiken aus Phase 4 eintragen
4. **Content Rating**: Fragebogen wahrheitsgetreu beantworten
5. **Datenschutz**: URL zur Datenschutzerklaerung (PFLICHT fuer Apps mit Internet/Konto)
6. **Preis**: Freemium → als "Kostenlos" listen, Abo-Preise ueber Billing
7. **Testing Track**: Erst Internal, dann Production
8. **Submit**: Zur Pruefung einreichen
9. **Warten**: Google Review

### Ausgangs-Kriterium
- [ ] App ist im Play Store live
- [ ] Store-Listing stimmt mit Phase-4-Plan ueberein
- [ ] Preis/Abo korrekt konfiguriert
- [ ] Datenschutzerklaerung vorhanden
- [ ] Erster Download funktioniert

---

## Phase 6: Updates & Wartung

### Was passiert
- Benutzer-Feedback auswerten
- Bugs fixen → Mini-Zyklus: Fix → R8-Build → Test → Upload
- Neue Features → Zurueck zu Phase 2 fuer das neue Feature
- Version hochzaehlen bei jedem Update

### Regeln
- **Bugfixes**: Direkt in Phase 6 fixen, R8-Build testen, hochladen
- **Kleine Features** (1-2 Sessions): In Phase 6 implementieren, testen, hochladen
- **Grosse Features** (3+ Sessions): Zurueck zu Phase 2, dann Phase 3 erneut durchlaufen
- **R8-Regression**: Bei JEDEM Update pruefen ob R8 noch funktioniert
- **Version**: Patch fuer Bugfixes (1.0.1 → 1.0.2), Minor fuer Features (1.0 → 1.1),
  Major fuer Breaking Changes (1.0 → 2.0)

---

## Phasen-Tracking in SESSION-RULES.md (PFLICHT — IMMER AKTUELL)

Jedes Android-Projekt MUSS in seiner SESSION-RULES.md einen Phasen-Block haben.
Dieser Block ist die EINZIGE Wahrheitsquelle fuer den aktuellen Projektstand.
Er wird bei JEDEM Session-Ende und bei JEDEM Phasenwechsel aktualisiert.

```markdown
## Entwicklungsphase
- **Aktuelle Phase**: 2 (Debug & Entwicklung)
- **Phase gestartet am**: 2026-03-31
- **Version**: v1.0.2 (Build 3)
- **Letzter Arbeitsstand**: [Was wurde in der letzten Session gemacht?]
- **Naechster Schritt**: [Was kommt ALS NAECHSTES? Konkret, nicht vage]
- **Naechster Meilenstein**: [Wann ist die aktuelle Phase fertig?]
- **Offene Features**: [Nummerierte Liste mit Status: ✅ fertig / 🔧 in Arbeit / ⬜ offen]
- **Bekannte Bugs**: [Liste, leer wenn keine]
- **Letzte Session**: [Datum]
```

---

## Session-Uebergabe-Protokoll (KRITISCH)

> **Der Faden darf NIE abreissen. Wenn der Benutzer in einer neuen Session sagt
> "wir machen mit Projekt X weiter", muss Claude SOFORT wissen wo wir stehen
> und was als naechstes kommt. Der Benutzer muss das ebenfalls wissen.**

### Am ENDE jeder Session (PFLICHT bei Android-Projekten)

Bevor die Session endet (letzter Commit/Push), MUSS Claude:

1. **SESSION-RULES.md aktualisieren** mit dem exakten Stand:
   - Was wurde HEUTE gemacht? (konkreter 1-2-Satz-Stand)
   - Was ist der NAECHSTE Schritt? (so konkret dass Claude in der naechsten Session
     sofort weitermachen kann, z.B. "SearchBar-Feature implementieren" nicht "weiterarbeiten")
   - Welche Features sind fertig, welche offen?
   - Version und Build-Nummer aktuell?

2. **Phasen-Status dem Benutzer zeigen** als letzten Block vor "Committed und gepusht":
   ```
   📱 Session-Stand gespeichert:
      Phase 2 (Debug) — BestJournalAndroid v1.0.3 (Build 4)
      ✅ Heute erledigt: Suchfunktion implementiert, Dashboard-Shimmer
      ➡️ Naechste Session: Export-Feature (PDF/Text), dann Backup-System
      📊 Feature-Status: 8/12 Features fertig
      🏁 Phase 2 Ende: Noch ~4 Features, dann R8-Phase
   ```

3. **Committen und Pushen** — die aktualisierte SESSION-RULES.md MUSS im Repo sein,
   damit sie in der naechsten Session (auch auf anderem Geraet) verfuegbar ist.

### Am ANFANG jeder Session (wenn "weiter mit Projekt X")

Wenn der Benutzer sagt er will an einem Android-Projekt weiterarbeiten, MUSS Claude:

1. **SESSION-RULES.md lesen** — den Phasen-Block und den letzten Arbeitsstand
2. **Dem Benutzer den Stand zeigen**:
   ```
   📱 Willkommen zurueck! Hier ist der aktuelle Stand:
      Phase 2 (Debug) — BestJournalAndroid v1.0.3 (Build 4)
      Letzte Session (03.04.2026): Suchfunktion und Dashboard-Shimmer implementiert
      ➡️ Heute dran: Export-Feature (PDF/Text)
      📊 Feature-Status: 8/12 Features fertig
      🏁 Bis Phase 3 (R8): Noch 4 Features
   
   Sollen wir mit dem Export-Feature anfangen?
   ```
3. **Nicht blind loslegen** — erst den Stand bestaetigen lassen, dann arbeiten

### In-Session Orientierung (PFLICHT bei laengeren Sessions)

Waehrend der Arbeit an einem Android-Projekt:

- **Nach jedem abgeschlossenen Feature/Fix**: Kurzer Phasen-Status als Orientierung
  ```
  ✅ Suchfunktion fertig. Phase 2: 9/12 Features. Naechstes: Export-Feature.
  ```
- **Wenn der Benutzer fragt "wo stehen wir?"**: Ausfuehrlichen Status zeigen
- **Wenn die Session lang wird (>5 Features)**: Proaktiv Status zeigen
- **Bei jedem Phasenwechsel**: Die ausfuehrliche Phasenwechsel-Ankuendigung (siehe unten)

### Was im Phasen-Block NICHT stehen darf

- ❌ Vage Formulierungen: "weiterarbeiten", "diverse Bugs", "verschiedene Features"
- ❌ Veraltete Informationen (von vor 3 Sessions)
- ❌ Technischer Jargon den der Benutzer nicht versteht
- ❌ Leere Felder — jedes Feld muss ausgefuellt sein

### Was im Phasen-Block IMMER stehen muss

- ✅ Konkreter naechster Schritt: "SearchBar mit Live-Filter implementieren"
- ✅ Zaehlbarer Feature-Status: "8/12 Features fertig"
- ✅ Klarer Horizont: "Noch ~4 Features bis Phase 3 (R8)"
- ✅ Datum der letzten Session
- ✅ Aktuelle Version und Build-Nummer

---

## Aktive Anleitung durch JEDE Phase (PFLICHT)

> **Claude ist nicht nur Programmierer, sondern NAVIGATOR.**
> **Der Benutzer muss JEDERZEIT wissen: Wo sind wir? Was kommt als naechstes?
> Wie weit ist es noch? Du kennst den Weg, ich kenn den Weg, alles laeuft nach Plan.**

### Das Navigator-Prinzip

Claude fuehrt den Benutzer durch jede Phase wie ein Reiseführer:
- **Vor einer Aufgabe**: "Wir machen jetzt X. Das gehoert zu Phase 2, Schritt Y."
- **Nach einer Aufgabe**: "X ist fertig. Noch Z Schritte bis zum naechsten Meilenstein."
- **Bei Problemen**: "Das ist ein Umweg, aber der Plan aendert sich nicht. Wir sind immer noch bei Phase 2."
- **Bei Phasenwechsel**: Ausfuehrliche Erklaerung was die neue Phase bedeutet (siehe unten)

### Was Claude bei JEDER Aufgabe sagt

Nicht nur die Aufgabe erledigen, sondern einordnen:

```
📱 Phase 2 (Debug) — BestJournalAndroid v1.0.3 (Build 4)
   Aufgabe: Export-Feature (PDF/Text) — Feature 9 von 12
```

[...Arbeit...]

```
✅ Export-Feature fertig. Phase 2: 9/12 Features.
   ➡️ Naechstes: Backup-System (Feature 10/12)
   🏁 Noch 3 Features bis Phase 3 (R8-Haertung)
```

### Was Claude NICHT tun darf

- ❌ Einfach drauflos programmieren ohne den Benutzer zu orientieren
- ❌ Phasen-Status weglassen weil "der Benutzer es schon weiss"
- ❌ Vage bleiben: "dann machen wir irgendwann den Release" — NEIN: konkrete Schritte
- ❌ Den Benutzer im Unklaren lassen wo er auf der Reise steht
- ❌ Eine Session beenden ohne den Stand zu speichern

### Was Claude IMMER tun muss

- ✅ Bei Session-Start: Stand zeigen und naechsten Schritt vorschlagen
- ✅ Bei jeder Aufgabe: Phase und Fortschritt anzeigen
- ✅ Bei Problemen: Einordnen ("Das aendert den Plan nicht" oder "Wir muessen den Plan anpassen")
- ✅ Bei Session-Ende: Stand speichern (SESSION-RULES.md), committen, pushen
- ✅ Bei Phasenwechsel: Ausfuehrlich erklaeren was kommt

---

## Phasenwechsel-Protokoll

### Von Phase 1 → Phase 2
Claude sagt: "Phase 1 (Setup) abgeschlossen. App kompiliert, Keystore erstellt,
Firebase konfiguriert. Wir sind jetzt in Phase 2 (Debug). Was ist das erste Feature?"

### Von Phase 2 → Phase 3 (BESONDERS WICHTIG)
Claude sagt AUSFUEHRLICH:
```
⚠️ Phase 2 (Debug) ist abgeschlossen. Alle Features sind implementiert und getestet.

Jetzt kommt Phase 3: R8-Haertung & Release-Build.

Das bedeutet konkret:
1. Ich scanne jetzt die GESAMTE App nach R8-Risiken
   (JNI, Reflection, Serialisierung, Third-Party-Libraries)
2. Ich schreibe vorab alle noetige ProGuard-Regeln
3. Dann machen wir den ersten Release-Build
4. Wenn etwas crasht, debuggen wir es sofort
5. Danach testen wir Billing/Abos im Release-Build

Das kann 1-3 Sessions dauern. Keine neuen Features mehr bis Phase 3 fertig ist.
Soll ich mit dem R8-Scan beginnen?
```

### Von Phase 3 → Phase 4
Claude sagt: "Release-Build laeuft stabil. Jetzt planen wir die Vermarktung.
Ich leite dich durch: Keywords, Store-Listing, Screenshots, Preisstrategie."

### Von Phase 4 → Phase 5
Claude sagt: "Vermarktungsstrategie steht. Bereit fuer den Upload.
Ich fuehre dich Schritt fuer Schritt durch die Play Console."

### Von Phase 5 → Phase 6
Claude sagt: "App ist live im Store! Ab jetzt: Updates und Wartung.
Neue Features → zurueck zu Phase 2. Bugfixes → direkt hier."

---

## Verbotene Phasen-Spruenge

| Von | Nach | Erlaubt? | Warum |
|-----|------|----------|-------|
| 1 | 2 | Ja | Normaler Ablauf |
| 2 | 3 | Ja | Normaler Ablauf |
| 2 | 4 | **NEIN** | Kein Release ohne R8-Haertung |
| 2 | 5 | **NEIN** | Kein Store ohne R8 und ohne Vermarktung |
| 3 | 5 | **NEIN** | Kein Store ohne Vermarktungsstrategie |
| 3 | 4 | Ja | Normaler Ablauf |
| 4 | 5 | Ja | Normaler Ablauf |
| 6 | 2 | Ja | Grosse Features zurueck in Debug |
| 6 | 3 | Ja | Update mit R8-Test |

---

## R8-Checkliste (Phase 3 — zum Abhaken)

Diese Checkliste wird bei JEDEM Eintritt in Phase 3 durchgegangen:

### Scan
- [ ] Alle JNI/native Libraries identifiziert
- [ ] Alle Reflection-Nutzungen identifiziert
- [ ] Alle Serialisierungs-Frameworks identifiziert (Moshi, Gson, etc.)
- [ ] Alle Third-Party-Libraries auf bekannte R8-Probleme geprueft
- [ ] Alle dynamischen Android-Komponenten identifiziert
- [ ] Billing-AIDL-Interfaces identifiziert (wenn Billing vorhanden)
- [ ] Firebase-Klassen identifiziert (wenn Firebase vorhanden)

### ProGuard-Regeln
- [ ] JNI-Keep-Regeln geschrieben
- [ ] Reflection-Keep-Regeln geschrieben
- [ ] Serialisierungs-Keep-Regeln geschrieben
- [ ] Third-Party-Keep-Regeln geschrieben (oder consumer-rules verifiziert)
- [ ] Billing-Keep-Regeln geschrieben
- [ ] Firebase-Keep-Regeln verifiziert
- [ ] JEDE Regel hat einen Kommentar WARUM sie da ist

### Release-Build
- [ ] `./gradlew assembleRelease` kompiliert fehlerfrei
- [ ] APK auf echtem Geraet installiert
- [ ] App startet ohne Crash
- [ ] JEDER Screen einmal geoeffnet
- [ ] Kernfunktionen getestet (Aufnahme, KI, Suche, etc.)
- [ ] Billing-Flow getestet (wenn vorhanden)
- [ ] APK-Groesse akzeptabel

---

## Warum diese Regel existiert

Am 2026-03-31 bis 2026-04-03 wurde BestJournalAndroid entwickelt. Dabei gab es:
- Versionsverwirrung zwischen BestJournalFrank und BestJournalAndroid
- R8-Crash im Release-Build (Sherpa-ONNX JNI-Felder gestripped)
- Falscher Paketname in ProGuard-Regeln (com.entropyjournal statt com.bestjournal.app)
- Fehlende Keep-Regeln fuer 6+ Libraries
- Kein Release-Keystore erstellt
- SHA-Fingerprints nirgendwo dokumentiert
- Billing-Tests ohne Release-Build versucht

Dieser Fahrplan verhindert all das fuer JEDE zukuenftige Android-App.
