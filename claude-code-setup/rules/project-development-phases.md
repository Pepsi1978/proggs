# Projekt-Entwicklung: 6-Phasen-Fahrplan fuer ALLE Projekte (KRITISCH)

> **Diese Regel gilt fuer JEDES neue Projekt — egal welche Sprache, Plattform oder Groesse.**
> **Sie ist die GENERISCHE Version des Phasen-Systems. Fuer Android-spezifische Details
> siehe `android-development-phases.md` — diese Regel hier ist die Basis.**
>
> **Direktive #3 (Resilient Bugfixing):** Dieser Fahrplan existiert WEIL beim BestJournal-Projekt
> Versionschaos und R8-Crashes auftraten. Statt nur Android zu fixen, wird das Muster
> auf ALLE Projekte verallgemeinert. Ein Fehler → systemweites Upgrade.

## Wann gilt diese Regel?

Bei JEDEM neuen Projekt das mit "neues Projekt", "neue App", "wir bauen X" beginnt.
Claude MUSS sofort den Phasen-Fahrplan aktivieren und dem Benutzer den Weg zeigen.

---

## Phasen-Uebersicht (gilt fuer ALLE Projekttypen)

| Phase | Name | Kernaufgabe |
|-------|------|-------------|
| **1** | Projekt-Setup | Infrastruktur, Repo-Struktur, Build-System, Secrets |
| **2** | Entwicklung (Debug) | Alle Features bauen und testen — OFFEN, Benutzer entscheidet wann fertig |
| **3** | Haertung & Release-Build | Plattform-spezifische Optimierung, Signing, Testing |
| **4** | Vermarktung & Distribution | Strategie WIE das Projekt zum Nutzer kommt (Pflicht-Recherche!) |
| **5** | Veroeffentlichung | Go-Live auf der Zielplattform |
| **6** | Updates & Wartung | Bugfixes, neue Features → zurueck zu Phase 2 |

---

## Phasen-Anzeige (PFLICHT bei jeder Aufgabe)

```
📦 Phase [N] ([Name]) — [Projektname] v[Version]
   [Aktuelle Aufgabe] | [Projekttyp]
```

Beispiele:
```
📦 Phase 2 (Entwicklung) — BestJournalAndroid v1.0.3 (Build 4) | Android/Kotlin
📦 Phase 1 (Setup) — TerminalVoiceOverlay v0.1.0 | C#/WPF
📦 Phase 4 (Vermarktung) — QuizVerse v2.0.0 | Web/TypeScript
```

---

## Phase 1: Projekt-Setup (ALLE Projekttypen)

### Pflicht-Schritte (immer, egal welches Projekt)
1. Ordner in `~/proggs/[Projektname]/` anlegen
2. Build-System einrichten (Gradle, dotnet, npm, cargo, go mod — je nach Typ)
3. `.gitignore` mit Secrets-Schutz (API-Keys, Keystores, .env, Credentials)
4. `SESSION-RULES.md` anlegen mit Phasen-Block (siehe Template unten)
5. Erster Commit: leeres Projekt das kompiliert
6. **Signing/Secrets SOFORT einrichten** — nicht erst vor dem Release

### Plattform-spezifische Ergaenzungen Phase 1

| Projekttyp | Zusaetzliche Setup-Schritte |
|------------|---------------------------|
| **Android/Kotlin** | Keystore erstellen, Firebase-Projekt, SHA-Fingerprints, google-services.json → siehe `android-development-phases.md` |
| **C#/WPF** | .sln + .csproj erstellen, Code-Signing-Zertifikat (wenn Distribution geplant), NuGet-Pakete |
| **Swift/macOS** | Xcode-Projekt, Apple Developer Account (wenn Distribution), Signing Identity, Entitlements |
| **Web/TypeScript** | package.json, tsconfig, Linter (biome/eslint), ggf. Hosting-Setup (Vercel/Netlify) |
| **Rust** | Cargo.toml, clippy + rustfmt Konfiguration |
| **Go** | go.mod, golangci-lint Konfiguration |
| **Tampermonkey** | Userscript-Header mit @name, @version, @match, @grant |

### Ausgangs-Kriterium
- [ ] Projekt kompiliert/startet fehlerfrei
- [ ] Secrets geschuetzt (gitignored)
- [ ] SESSION-RULES.md mit Phasen-Block angelegt
- [ ] Erster Commit gepusht

---

## Phase 2: Entwicklung (Debug) — ALLE Projekttypen

### Kernregeln (immer gleich)
1. **Debug-Modus**: Keine Optimierungen, keine Minifizierung, keine Obfuskation
2. **Features offen**: Neue Ideen waehrend der Arbeit sind NORMAL und ERWUENSCHT
3. **Nur der Benutzer entscheidet** wann Phase 2 fertig ist
4. **Claude fragt NIE** "sollen wir Phase 2 beenden?" — der Benutzer sagt es

### Plattform-spezifische Debug-Regeln

| Projekttyp | Debug-Besonderheiten |
|------------|---------------------|
| **Android/Kotlin** | `isMinifyEnabled = false`, kein R8, kein Billing-Test, Debug-Suffix aktiv |
| **C#/WPF** | Debug-Konfiguration, keine Code-Signierung, ClickOnce nur fuer lokales Testen |
| **Swift/macOS** | Debug-Build, keine Notarization, kein Hardened Runtime erzwingen |
| **Web/TypeScript** | Dev-Server (vite/next dev), keine Minifizierung, Source Maps aktiv |
| **Rust** | `cargo build` (nicht `--release`), debug_assertions aktiv |
| **Go** | `go build` ohne `-ldflags`, Race Detector aktiv (`-race`) |
| **Tampermonkey** | Direkt im Browser testen, console.log aktiv |

### Ausgangs-Kriterium
- [ ] **Benutzer sagt explizit**: "Phase 2 ist fertig" / "wir koennen zum naechsten Schritt"
- [ ] Projekt laeuft stabil
- [ ] Keine bekannten Crashes oder schweren Bugs

---

## Phase 3: Haertung & Release-Build — ALLE Projekttypen

> **ACHTUNG: Dies ist die Phase wo Debug-Code in Produktions-Code wird.**
> **Claude MUSS den Benutzer AUSFUEHRLICH informieren bevor diese Phase beginnt.**

### Eintritts-Ankuendigung (PFLICHT — ALLE Projekttypen)

```
⚠️ Phase 2 (Entwicklung) ist abgeschlossen.

Jetzt kommt Phase 3: Haertung & Release-Build.

Das bedeutet konkret fuer [Projekttyp]:
[Plattform-spezifische Erklaerung — siehe unten]

Keine neuen Features mehr bis Phase 3 fertig ist.
```

### Plattform-spezifische Haertung

#### Android/Kotlin
→ **Vollstaendige Anleitung in `android-development-phases.md`**
- R8-Vorbereitungs-Scan (JNI, Reflection, Serialisierung, Third-Party)
- ProGuard-Regeln schreiben
- Release-Build testen
- Billing/Subscription testen

#### C#/WPF (.exe Distribution)
- **Release-Build**: `dotnet publish -c Release -r win-x64 --self-contained`
- **Single-File**: `/p:PublishSingleFile=true /p:IncludeNativeLibrariesForSelfExtract=true`
- **Trimming pruefen**: Wenn `PublishTrimmed=true` → aehnlich wie R8! Reflection-Nutzung scannen
- **Code-Signing**: Zertifikat fuer .exe (verhindert "Unbekannter Herausgeber"-Warnung)
- **Installer erstellen**: WiX, InnoSetup oder MSIX
- **Auf sauberem System testen**: Alle Abhaengigkeiten vorhanden?

#### Swift/macOS (.app Distribution)
- **Release-Build**: `xcodebuild -configuration Release`
- **Code-Signing**: Developer ID Application Zertifikat
- **Notarization**: `xcrun notarytool submit` (Apple-Pflicht seit Catalina)
- **Hardened Runtime**: Entitlements pruefen
- **DMG oder ZIP erstellen**: Fuer Download-Distribution
- **Sandbox pruefen**: Wenn Mac App Store geplant

#### Web/TypeScript
- **Production Build**: `npm run build` / `next build` / `vite build`
- **Bundle-Groesse pruefen**: Lighthouse-Score, Tree-Shaking funktioniert?
- **Environment Variables**: Alle Prod-Werte gesetzt? Keine Dev-Keys im Build?
- **Security Headers**: CSP, HSTS, X-Frame-Options
- **SSL/HTTPS**: Zertifikat vorhanden und gueltig?
- **SEO**: Meta-Tags, Open Graph, Sitemap

#### Rust
- **Release-Build**: `cargo build --release`
- **Clippy streng**: `cargo clippy -- -D warnings`
- **Binary-Groesse**: `strip` + `opt-level = "z"` wenn noetig
- **Cross-Compilation**: Fuer Zielplattformen (x86_64, aarch64)

#### Go
- **Release-Build**: `go build -ldflags="-s -w"` (Strip Debug-Info)
- **go vet + staticcheck**: Alle Warnungen beheben
- **Cross-Compilation**: `GOOS=windows/darwin/linux GOARCH=amd64/arm64`

#### Tampermonkey
- **Version-Bump**: @version erhoehen
- **Alle console.log entfernen** oder hinter Debug-Flag
- **@match pruefen**: Stimmen die URL-Patterns noch?
- **Permissions minimieren**: Nur die @grant die wirklich gebraucht werden

### Ausgangs-Kriterium (ALLE Projekttypen)
- [ ] Release-Build kompiliert/baut fehlerfrei
- [ ] Auf Zielplattform getestet und stabil
- [ ] Alle Optimierungen aktiv (Minifizierung, Signing, etc.)
- [ ] Keine Debug-Artefakte im Release (console.log, Test-Keys, etc.)

---

## Phase 4: Vermarktung & Distribution — ALLE Projekttypen

> **KEIN Release ohne Vermarktungsstrategie.**
> **Gilt fuer JEDES Projekt das Nutzer erreichen soll — nicht nur Apps.**

### Pflicht-Recherche (wird FRISCH durchgefuehrt, nicht aus dem Cache)

**WICHTIG: Die Recherche muss STATE OF THE ART sein.**
Sie wird genau DANN durchgefuehrt wenn Phase 4 beginnt — damit die Ergebnisse aktuell sind.

Claude startet 3-5 parallele Researcher-Agents zu diesen Themen
(angepasst an den Projekttyp):

| Projekttyp | Recherche-Fokus |
|------------|----------------|
| **Android** | Play Store ASO, Solo-Dev-Strategien, Monetarisierung, Konkurrenz → siehe `android-development-phases.md` |
| **C#/WPF** | Windows-Distribution (Microsoft Store vs. Website vs. Winget), Installer-Best-Practices, Software-Verzeichnisse |
| **Swift/macOS** | Mac App Store ASO, Notarization-Workflow, macOS-App-Review-Blogs, Indie-Mac-Developer-Communities |
| **Web/TypeScript** | SEO, Product Hunt Launch, Indie Hackers, Social Media Marketing, Landing Page Best Practices |
| **Tampermonkey** | GreasyFork/OpenUserJS Listing, Reddit-Communities, UserScript-Verzeichnisse |
| **CLI-Tool** | Package Manager (npm, cargo, brew), GitHub Releases, README als Marketing, awesome-lists |
| **Open Source** | GitHub-Sichtbarkeit, README-Design, Contributing Guide, Community-Building |

### Vermarktungs-Dokument erstellen

Ergebnis wird gespeichert als: `docs/marketing/[projektname]-marketing-strategy.md`

Mindest-Inhalt:
1. **Zielgruppe**: Wer ist der Nutzer? Warum braucht er das?
2. **Konkurrenz**: Was gibt es schon? Was machen wir besser?
3. **Distribution**: WIE kommt das Projekt zum Nutzer?
4. **Sichtbarkeit**: WO findet der Nutzer es?
5. **Monetarisierung** (wenn zutreffend): Preismodell, Freemium-Grenzen
6. **Launch-Plan**: Zeitplan, erste Schritte, Erfolgskriterien

### Wann Phase 4 NICHT noetig ist

| Projekttyp | Phase 4 ueberspringen? |
|------------|----------------------|
| Persoenliches Tool (nur fuer sich selbst) | Ja — direkt zu Phase 5 |
| Internes Firmen-Tool | Ja — direkt zu Phase 5 |
| Open-Source-Library (kein Endnutzer-Produkt) | Verkuerzt: nur README + GitHub-Sichtbarkeit |
| Alles was Nutzer/Kunden erreichen soll | **NEIN — Phase 4 ist PFLICHT** |

### Ausgangs-Kriterium
- [ ] Vermarktungs-Dokument erstellt und vom Benutzer abgesegnet
- [ ] Distribution-Kanal festgelegt
- [ ] Listing/Landing Page vorbereitet (wenn zutreffend)
- [ ] Launch-Plan definiert

---

## Phase 5: Veroeffentlichung — ALLE Projekttypen

### Plattform-spezifische Veroeffentlichung

| Projekttyp | Veroeffentlichungs-Schritte |
|------------|---------------------------|
| **Android** | Play Console → AAB Upload → Testing Tracks → Production → siehe `android-development-phases.md` |
| **C#/WPF** | GitHub Release + Installer-Download ODER Microsoft Store ODER Winget-Manifest |
| **Swift/macOS** | Notarization → DMG/ZIP auf Website ODER Mac App Store Review |
| **Web** | Deploy auf Hosting (Vercel/Netlify/eigener Server), DNS konfigurieren, SSL pruefen |
| **Tampermonkey** | GreasyFork Upload + OpenUserJS, Beschreibung und Screenshots |
| **CLI-Tool** | `npm publish` / `cargo publish` / `brew tap` + GitHub Release mit Binaries |
| **Open Source** | GitHub Release mit Changelog, Tag erstellen, README finalisieren |

### Ausgangs-Kriterium
- [ ] Projekt ist fuer Nutzer erreichbar/installierbar
- [ ] Listing/Website stimmt mit Vermarktungsplan ueberein
- [ ] Erster erfolgreicher Download/Install durch echten Nutzer

---

## Phase 6: Updates & Wartung — ALLE Projekttypen

### Regeln (immer gleich)
- **Bugfixes**: Direkt in Phase 6, Release-Build testen, veroeffentlichen
- **Kleine Features** (1-2 Sessions): In Phase 6 implementieren
- **Grosse Features** (3+ Sessions): Zurueck zu Phase 2, dann Phase 3 erneut
- **Version**: Patch fuer Bugfixes, Minor fuer Features, Major fuer Breaking Changes

---

## Session-Uebergabe-Protokoll (gilt fuer ALLE Projekte)

> **Der Faden darf NIE abreissen.**

### Am ENDE jeder Session (PFLICHT)

1. **SESSION-RULES.md aktualisieren** mit exaktem Stand
2. **Phasen-Status dem Benutzer zeigen**:
   ```
   📦 Session-Stand gespeichert:
      Phase [N] ([Name]) — [Projekt] v[Version]
      ✅ Heute erledigt: [Was wurde gemacht]
      ➡️ Naechste Session: [Was kommt als naechstes — KONKRET]
      🏁 Horizont: [Wie weit bis zum naechsten Phasenwechsel]
   ```
3. **Committen und Pushen** — SESSION-RULES.md muss im Repo sein

### Am ANFANG jeder Session (wenn "weiter mit Projekt X")

1. **SESSION-RULES.md lesen**
2. **Dem Benutzer den Stand zeigen**:
   ```
   📦 Willkommen zurueck! Stand von [Projekt]:
      Phase [N] ([Name]) — v[Version]
      Letzte Session ([Datum]): [Was wurde gemacht]
      ➡️ Heute dran: [Naechster Schritt]
   
   Sollen wir damit anfangen?
   ```
3. **Nicht blind loslegen** — erst Stand bestaetigen lassen

### Aktive Anleitung (Navigator-Prinzip)

Claude ist NAVIGATOR — nicht nur Programmierer:
- **Vor einer Aufgabe**: "Wir machen jetzt X. Das gehoert zu Phase [N]."
- **Nach einer Aufgabe**: "X ist fertig. [Fortschritts-Update]"
- **Bei Problemen**: "Das ist ein Umweg, aber der Plan aendert sich nicht."
- **Bei Phasenwechsel**: Ausfuehrliche Erklaerung was die neue Phase bedeutet

---

## SESSION-RULES.md Template (fuer neue Projekte)

Beim Anlegen eines neuen Projekts wird dieser Block in SESSION-RULES.md eingefuegt:

```markdown
# [Projektname] — Session-Regeln

## Entwicklungsphase (IMMER AKTUELL HALTEN)
- **Aktuelle Phase**: 1 (Projekt-Setup)
- **Phase gestartet am**: [Datum]
- **Projekttyp**: [Android/Kotlin | C#/WPF | Swift/macOS | Web/TS | Rust | Go | Tampermonkey]
- **Version**: v0.1.0
- **Letzter Arbeitsstand**: Projekt gerade erstellt
- **Naechster Schritt**: [Erster konkreter Schritt]
- **Bekannte Bugs**: keine
- **Letzte Session**: [Datum]

## Projekt-Parameter
- **Sprache**: [Kotlin/C#/Swift/TypeScript/Rust/Go/JavaScript]
- **Zielplattform**: [Android/Windows/macOS/Web/CLI/Browser-Extension]
- **Distribution**: [Play Store/GitHub Release/Website/npm/etc.]
- **Monetarisierung**: [Freemium/Paid/Kostenlos/Open Source]
```

---

## Verbotene Phasen-Spruenge (gilt fuer ALLE Projekttypen)

| Von | Nach | Erlaubt? | Warum |
|-----|------|----------|-------|
| 1 | 2 | Ja | Normaler Ablauf |
| 2 | 3 | Ja | Normaler Ablauf (nur wenn Benutzer sagt "Phase 2 fertig") |
| 2 | 4 | **NEIN** | Keine Vermarktung ohne Haertung/Testing |
| 2 | 5 | **NEIN** | Kein Release ohne Haertung UND Vermarktung |
| 3 | 5 | **NEIN** | Kein Release ohne Vermarktungsstrategie (ausser persoenliche Tools) |
| 3 | 4 | Ja | Normaler Ablauf |
| 4 | 5 | Ja | Normaler Ablauf |
| 6 | 2 | Ja | Grosse Features zurueck in Entwicklung |

---

## Zusammenspiel mit android-development-phases.md

Diese Regel hier ist die **generische Basis**. Fuer Android-Projekte gilt ZUSAETZLICH
die detaillierte Android-Regel (`android-development-phases.md`) mit:
- R8-Scan und ProGuard-Checkliste (Phase 3)
- Firebase/Keystore/SHA-Fingerprint-Details (Phase 1)
- Play Store ASO und Solo-Dev-Marketing (Phase 4)
- Billing-Test-Protokoll (Phase 3)

Bei einem Android-Projekt werden BEIDE Regeln gleichzeitig angewendet:
diese generische + die Android-spezifische.

---

## Warum diese Regel existiert

Am 2026-03-31 bis 2026-04-03 entstand beim BestJournal-Projekt Versionschaos:
- Mehrere Versionen (Frank, Android, Debug, Test) ohne klare Zuordnung
- R8-Crashes im Release-Build weil nie systematisch vorbereitet
- Fehlende Vermarktungsstrategie
- Kein Session-Uebergabe-Protokoll → Wissen ging zwischen Sessions verloren

Statt nur Android zu fixen (Direktive #3: "verwandte Fehlerquellen suchen"),
wird das Muster auf ALLE Projekttypen verallgemeinert. Kein Projekt startet
mehr ohne Phasen-System, Navigation und Session-Uebergabe.
