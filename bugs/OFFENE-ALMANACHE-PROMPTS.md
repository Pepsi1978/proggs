# Offene Almanache — fertige Recherche-Prompts (Copy-Paste)

> Stand: 2026-06-02. Fuer jeden Bereich OHNE Almanach stehen hier zwei fertige Prompts:
> einer startet den `bug-almanach-recherche`-Skill, einer den `best-practices`-Skill.
> Landkarte aller Bereiche: `README.md`. Systemverhalten: `SYSTEM.md`.

---

## Die zwei Prompt-Sorten — klar getrennt

Jeder Bereich hat ZWEI Seiten, und jede Prompt-Sorte zielt nur auf EINE davon:

| Prompt-Sorte | Skill | ZIEL — worauf er rein abzielt |
|--------------|-------|-------------------------------|
| **🐛 Bug-Prompt** | `bug-almanach-recherche` | **Reine Bugsuche:** was in der Praxis SCHIEFGEHT — bekannte Bugs, Fehler, Abstuerze, Fallstricke, Versions-/Plattform-Fallen + die funktionserhaltende Loesung pro Bug. |
| **✅ Best-Practices-Prompt** | `best-practices` | **Wie man es RICHTIG macht:** die empfohlene, idiomatische Arbeitsweise — bewaehrte Patterns, offizielle Empfehlungen, Do's & Don'ts, „so arbeitet man am besten damit". |

Beide Prompts sagen ihr Ziel ausdruecklich und grenzen sich gegen die andere Sorte ab,
damit die Recherche nicht vermischt: Bugs landen im Almanach, Arbeitsweise in best-practices.

---

## So benutzt du diese Prompts

1. Neue Claude-Code-Session im Repo (`~/proggs`) oeffnen.
2. EINEN Prompt-Block kopieren und als Nachricht einfuegen.
3. Der Skill startet, ermittelt die Version live, recherchiert, legt die Datei an, traegt
   sie ein, ergaenzt das Hook-Mapping und committet + pusht selbst.
4. Pro Bereich: **zuerst** den 🐛 Bug-Prompt fertig laufen lassen, **danach** den ✅ Best-Practices-Prompt.

## Wichtig: Reihenfolge & RPM-Schutz

- **Gleicher Bereich nie gleichzeitig:** erst Bug-Almanach fertig + committet, DANN Best-Practices
  (sie schreiben dieselben Dateien und koppeln sich → sonst Datei-/Push-Konflikt).
- **Nicht zu viele parallel:** jeder Prompt startet selbst 5–7 Researcher → hoechstens
  **2–3 Recherche-Sessions gleichzeitig**, sonst 429/RPM-Absturz.

## Reihenfolge-Empfehlung (nach Hebel)

Python ⭐ → Jetpack Compose ⭐ → Android-Framework → Firebase/Billing → Swift/AppKit →
TypeScript → Stream-Deck → MCP-Server. (Gradle-Best-Practices siehe ganz unten.)

---

## 1. Python (Windows-Encoding & Cross-Platform-Scripting) ⭐

**🐛 Bug-Prompt:**
```
Starte den Skill bug-almanach-recherche fuer Python (Schwerpunkt Windows-Encoding & Cross-Platform-Scripting).

ZIEL = REINE BUGSUCHE: Finde gezielt die bekannten BUGS, Fehler und Fallstricke — was in der Praxis SCHIEFGEHT — und zu jedem die funktionserhaltende Loesung. NICHT um Best Practices/Stil (das laeuft getrennt), sondern um konkrete Fehlerquellen: cp1252-vs-UTF-8, BOM, UnicodeEncodeError beim Schreiben von JSON mit Emojis/Umlauten, fehlendes encoding='utf-8', /c/Users-vs-Windows-Pfade (FileNotFoundError), abgeschnittene Dateien bei Crash ohne atomares Schreiben, venv-/PATH-Fallen. Bei mir schon mehrfach passiert (BOM-Bug 2x).

Python-Version LIVE ermitteln (python --version). Scripts liegen u.a. in ~/proggs/scripts und ~/proggs/tools. Lege bugs/claude-tooling/python-windows.md an, trage ihn in bugs/README.md (von "ohne Almanach" nach "Vorhandene") ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.py). Am Ende committen und pushen.
```

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer Python auf Windows / Cross-Platform-Scripting (Projekt-Code-Seite, NICHT den Claude-Harness).

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN PYTHON-SCRIPTING AUF WINDOWS/CROSS-PLATFORM AM BESTEN UND RICHTIG MACHT — die empfohlene Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs laufen getrennt im Almanach), sondern der "so macht man es richtig"-Leitfaden: immer encoding='utf-8' + ensure_ascii=False, atomares Schreiben (temp + os.replace), plattformneutrale Pfade (pathlib/os.path statt Hardcoding), saubere venv-/Dependency-Verwaltung, robuste CLI-Struktur, Typ-Hints.

Speichere unter best-practices/claude-tooling/python-windows.md und koppele wechselseitig mit dem Bug-Almanach bugs/claude-tooling/python-windows.md (Bezugs-Tabelle in beiden Dateien). Am Ende committen und pushen.
```

---

## 2. Jetpack Compose (Android-UI) ⭐

**🐛 Bug-Prompt:**
```
Starte den Skill bug-almanach-recherche fuer Jetpack Compose (Android-UI).

ZIEL = REINE BUGSUCHE: Finde gezielt die bekannten BUGS, Fehler und Fallstricke — was in der Praxis SCHIEFGEHT — und zu jedem die funktionserhaltende Loesung. NICHT um Best Practices/Stil (laeuft getrennt), sondern konkrete Fehlerquellen: Recomposition-Schleifen/zu-viele-Recompositions, falsches remember/rememberSaveable (State-Verlust bei Rotation/Navigation), Modifier-Reihenfolge-Bugs, LazyColumn/LazyRow ohne stabile Keys (Scroll-Sprung/Flackern), Side-Effect-Fallen (LaunchedEffect-Key falsch), Instabilitaet/Skippability-Probleme, Crashes.

Compose-Version LIVE ermitteln aus ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor (Compose-BOM / Compiler in gradle/libs.versions.toml). Lege bugs/android/jetpack-compose.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.kt mit @Composable/setContent). Grenze klar gegen kotlin.md (Sprache/Coroutines), android-platform.md (Framework) und gradle.md (Build) ab — hier nur Compose-UI. Am Ende committen und pushen.
```

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer Jetpack Compose (Projekt-Code-Seite, NICHT Harness).

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN MIT JETPACK COMPOSE AM BESTEN UND RICHTIG ARBEITET — empfohlene, idiomatische Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs getrennt im Almanach), sondern der "so macht man es richtig"-Leitfaden: unidirektionaler Datenfluss + State-Hoisting, stabile/immutable State-Typen fuer Skippability, korrekte remember-/derivedStateOf-Nutzung, LazyList mit stabilen Keys, richtige Side-Effect-API je Fall, Material3-Theming, Performance-Empfehlungen.

Compose-Version aus ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor abgleichen. Speichere unter best-practices/android/jetpack-compose.md und koppele wechselseitig mit bugs/android/jetpack-compose.md. Am Ende committen und pushen.
```

---

## 3. Android-Framework / Platform-SDK

**🐛 Bug-Prompt:**
```
Starte den Skill bug-almanach-recherche fuer das Android-Framework/Platform-SDK.

ZIEL = REINE BUGSUCHE: Finde gezielt die bekannten BUGS, Fehler und Fallstricke — was in der Praxis SCHIEFGEHT — plus funktionserhaltende Loesung. NICHT Best Practices (getrennt), sondern konkrete Fehlerquellen: Lifecycle-Crashes (Leaks, Arbeit nach onDestroy), Permission-Fallen bei neuen API-Leveln, Foreground-Service-Restriktionen/ANRs, WorkManager-Eigenheiten, Room-Migrations-Crashes + WAL-Checkpoint, PendingIntent-Mutability-Flags, Scoped-Storage-Bruch, Background-/Doze-Drosselung.

compileSdk/targetSdk/minSdk LIVE ermitteln aus ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor (build.gradle.kts). Lege bugs/android/android-platform.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (AndroidManifest.xml + android-spezifische Verzeichnisse). Grenze gegen kotlin.md, jetpack-compose.md und gradle.md ab — hier nur Framework/Runtime. Am Ende committen und pushen.
```

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer das Android-Framework/Platform-SDK (Projekt-Code-Seite, NICHT Harness).

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN MIT DEM ANDROID-FRAMEWORK AM BESTEN UND RICHTIG ARBEITET — empfohlene Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs getrennt), sondern "so macht man es richtig": lifecycle-sicheres Arbeiten (lifecycleScope, repeatOnLifecycle), saubere Room-Migrationsstrategie, korrekter WorkManager-Einsatz, moderner Runtime-Permission-Flow, sauberer Foreground-Service-/Notification-/Scoped-Storage-Umgang, empfohlene App-Architektur.

compileSdk/targetSdk aus ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor abgleichen. Speichere unter best-practices/android/android-platform.md und koppele wechselseitig mit bugs/android/android-platform.md. Am Ende committen und pushen.
```

---

## 4. Firebase / Crashlytics / Google Play Billing

**🐛 Bug-Prompt:**
```
Starte den Skill bug-almanach-recherche fuer die Google-Backend-Dienste meiner Android-App (Firebase + Google Play Billing).

ZIEL = REINE BUGSUCHE: Finde gezielt die bekannten BUGS, Fehler und Fallstricke — was in der Praxis SCHIEFGEHT — plus funktionserhaltende Loesung. NICHT Best Practices (getrennt), sondern konkrete Fehlerquellen: google-services.json nicht neu geladen nach Konsolen-Aenderung, Crashlytics-Mapping fehlt/Symbole unleserlich, FCM-Zustellprobleme, BillingClient-Verbindungsabbrueche, vergessenes acknowledgePurchase (Auto-Refund), Pending Purchases, Proration-Fehler, falsche Abo-Status-Synchronisation.

SDK-Versionen LIVE aus ~/proggs/BestJournalAndroid (gradle/libs.versions.toml). Lege bugs/android/firebase-billing.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (google-services.json, BillingClient/Billing-/Subscription-Klassen). Am Ende committen und pushen.
```

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer Firebase + Google Play Billing (Projekt-Code-Seite, NICHT Harness).

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN FIREBASE UND PLAY BILLING AM BESTEN UND RICHTIG EINSETZT — empfohlene Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs getrennt), sondern "so macht man es richtig": sichere Billing-Flows (Kauf verifizieren + acknowledgen), Abo-Status serverseitig (Cloud Function / Play Developer API) validieren statt nur lokal, korrekter Crashlytics-Mapping-Upload, sauberer FCM-Umgang, sinnvolle Firestore-Security-Rules.

Hauptprojekt ~/proggs/BestJournalAndroid. Speichere unter best-practices/android/firebase-billing.md und koppele wechselseitig mit bugs/android/firebase-billing.md. Am Ende committen und pushen.
```

---

## 5. Swift / AppKit (macOS-Desktop)

**🐛 Bug-Prompt:**
```
Starte den Skill bug-almanach-recherche fuer macOS-Desktop-Entwicklung mit Swift und AppKit.

ZIEL = REINE BUGSUCHE: Finde gezielt die bekannten BUGS, Fehler und Fallstricke — was in der Praxis SCHIEFGEHT — plus funktionserhaltende Loesung. NICHT Best Practices (getrennt), sondern konkrete Fehlerquellen: Overlay-Fenster (NSWindow/NSPanel) bekommt keinen Fokus / falsches Level / verschwindet, Accessibility-API-Berechtigung wird nicht erkannt, globale Hotkeys feuern nicht, Mikrofon/AVFoundation-Permission-Fallen, App-Sandbox blockiert Zugriffe, ruckelnde Fenster-Animation.

Swift-Version LIVE ermitteln (swift --version); falls Session auf Windows laeuft: fuer die aktuelle stabile Swift-/Xcode-Version recherchieren. Projekte ~/proggs/ClaudeCodexVoiceOverlay-macOS und ~/proggs/TerminalVoiceOverlay-macOS. Lege bugs/desktop/swift-appkit.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.swift, *.xcodeproj, Info.plist). Am Ende committen und pushen.
```

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer Swift + AppKit (macOS-Desktop, Projekt-Code-Seite, NICHT Harness).

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN MIT SWIFT/APPKIT AM BESTEN UND RICHTIG ARBEITET — empfohlene Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs getrennt), sondern "so macht man es richtig": idiomatische Overlay-Fenster-Konfiguration (Level/Collection-Behavior), moderne Swift-Concurrency (async/await, Actors, @MainActor), saubere Accessibility-Integration, korrektes Permission-Handling, empfohlene App-Architektur (MVVM).

Projekte ~/proggs/ClaudeCodexVoiceOverlay-macOS und ~/proggs/TerminalVoiceOverlay-macOS. Speichere unter best-practices/desktop/swift-appkit.md und koppele wechselseitig mit bugs/desktop/swift-appkit.md. Am Ende committen und pushen.
```

---

## 6. TypeScript / Node

**🐛 Bug-Prompt:**
```
Starte den Skill bug-almanach-recherche fuer TypeScript und Node.js.

ZIEL = REINE BUGSUCHE: Finde gezielt die bekannten BUGS, Fehler und Fallstricke — was in der Praxis SCHIEFGEHT — plus funktionserhaltende Loesung. NICHT Best Practices (getrennt), sondern konkrete Fehlerquellen: ESM-vs-CommonJS-Bruch, unhandled Promise rejections, falsche tsconfig (kein strict), Typ-Fallen (any-Leaks, falsche Generics), npm/peer-dependency-Konflikte, ESM-only-Pakete in CJS, Bun-Kompatibilitaetsprobleme.

Node-/TypeScript-Version LIVE ermitteln (node --version, package.json, tsconfig.json). Beispielprojekt ~/proggs/mcp-code-search. Lege bugs/web/typescript.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.ts, *.tsx, tsconfig.json). Am Ende committen und pushen.
```

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer TypeScript + Node.js (Projekt-Code-Seite, NICHT Harness).

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN MIT TYPESCRIPT/NODE AM BESTEN UND RICHTIG ARBEITET — empfohlene Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs getrennt), sondern "so macht man es richtig": strikte tsconfig, sauberes ESM-Setup, typsichere Patterns (kein any, korrekte Generics/Utility-Types), robuste async-Fehlerbehandlung, Dependency-Hygiene.

Speichere unter best-practices/web/typescript.md und koppele wechselseitig mit bugs/web/typescript.md. Am Ende committen und pushen.
```

---

## 8. Stream-Deck-Plugin (Elgato)

**🐛 Bug-Prompt:**
```
Starte den Skill bug-almanach-recherche fuer Elgato-Stream-Deck-Plugin-Entwicklung.

ZIEL = REINE BUGSUCHE: Finde gezielt die bekannten BUGS, Fehler und Fallstricke — was in der Praxis SCHIEFGEHT — plus funktionserhaltende Loesung. NICHT Best Practices (getrennt), sondern konkrete Fehlerquellen: manifest.json-Fehler (Actions/States), WebSocket-Verbindung zum Host bricht ab, Property-Inspector zeigt/sendet keine Settings, Action-Lifecycle-Events (willAppear/keyDown) feuern nicht/doppelt, Settings-Persistenz verloren, Node-Backend-Crash.

Projekt ~/proggs/TVO-StreamDeck-Plugin. SDK-/Software-Version aus manifest.json ermitteln. Lege bugs/peripherie/stream-deck.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.sdPlugin/*, Stream-Deck-manifest.json, propertyInspector). Am Ende committen und pushen.
```

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer Elgato-Stream-Deck-Plugins (Projekt-Code-Seite, NICHT Harness).

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN STREAM-DECK-PLUGINS AM BESTEN UND RICHTIG BAUT — empfohlene Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs getrennt), sondern "so macht man es richtig": sauberer Action-Lifecycle, korrekte bidirektionale Kommunikation Plugin<->Property-Inspector, robuste Settings-Persistenz, empfohlene Projekt-/Manifest-Struktur, offizielle SDK-Konventionen.

Projekt ~/proggs/TVO-StreamDeck-Plugin. Speichere unter best-practices/peripherie/stream-deck.md und koppele wechselseitig mit bugs/peripherie/stream-deck.md. Am Ende committen und pushen.
```

---

## 9. MCP-Server-Bau

**🐛 Bug-Prompt:**
```
Starte den Skill bug-almanach-recherche fuer den Bau von MCP-Servern (Model Context Protocol).

ZIEL = REINE BUGSUCHE: Finde gezielt die bekannten BUGS, Fehler und Fallstricke — was in der Praxis SCHIEFGEHT — plus funktionserhaltende Loesung. NICHT Best Practices (getrennt), sondern konkrete Fehlerquellen: stdio-vs-SSE-Transport-Fehler, ungueltiges Tool-Schema (Client lehnt ab), Timeouts, Fehler werden verschluckt statt propagiert, .mcp.json mit nackten Befehlsnamen statt absoluten Pfaden (startet nicht), Cross-Platform-Start-Fehler (Windows vs macOS), Server-Crash ohne Recovery.

Projekt ~/proggs/mcp-code-search. MCP-SDK-Version aus package.json/Cargo.toml ermitteln. Lege bugs/claude-tooling/mcp-server.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (.mcp.json, MCP-Server-Quellen). Grenze gegen claude-hooks.md ab. Am Ende committen und pushen.
```

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer den Bau von MCP-Servern (Projekt-Code-Seite, NICHT Harness).

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN MCP-SERVER AM BESTEN UND RICHTIG BAUT — empfohlene Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs getrennt), sondern "so macht man es richtig": passende Transport-Wahl (stdio vs SSE), sauberes Tool-Schema-Design (klare Beschreibungen, enge Typen), korrekte Fehler-Propagation, .mcp.json mit absoluten Pfaden, Cross-Platform-tauglicher Start, sinnvolle Tool-Granularitaet.

Projekt ~/proggs/mcp-code-search. Speichere unter best-practices/claude-tooling/mcp-server.md und koppele wechselseitig mit bugs/claude-tooling/mcp-server.md. Am Ende committen und pushen.
```

---

## Gradle — nur noch Best-Practices offen (Almanach ✅ fertig)

Der Bug-Almanach `bugs/android-build/gradle.md` existiert bereits (Stand 2026-06-02, ~67 Bugs). Es fehlt
nur noch die Best-Practices-Seite:

**✅ Best-Practices-Prompt:**
```
Starte den Best-Practices-Skill fuer das Android-Build-System (Gradle, AGP, R8, KSP) — Projekt-Code-Seite, NICHT Harness.

ZIEL = BEST PRACTICES: Recherchiere, WIE MAN GRADLE/AGP AM BESTEN UND RICHTIG EINSETZT — empfohlene Arbeitsweise und Do's & Don'ts. NICHT was schiefgeht (Bugs stehen schon in bugs/android-build/gradle.md), sondern "so macht man es richtig": Version-Catalog (libs.versions.toml) konsequent nutzen, Configuration-Cache + Build-Cache aktivieren, saubere Dependency-Deklaration (api vs implementation), korrekte R8/ProGuard-Regel-Pflege, modulare Projektstruktur, schnelle Builds.

Versionen aus ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor abgleichen. Speichere unter best-practices/android-build/gradle.md und koppele wechselseitig mit dem bestehenden Bug-Almanach bugs/android-build/gradle.md (Bezugs-Tabelle in beiden Dateien). Am Ende committen und pushen.
```

---

## Fortschritt abhaken

| Bereich | 🐛 Bug-Almanach | ✅ Best-Practices |
|---------|:---------------:|:-----------------:|
| Gradle | ✅ fertig | ⬜ |
| Python-Windows | ⬜ | ⬜ |
| Jetpack Compose | ⬜ | ⬜ |
| Android-Platform | ⬜ | ⬜ |
| Firebase/Billing | ⬜ | ⬜ |
| Swift/AppKit | ⬜ | ⬜ |
| TypeScript/Node | ⬜ | ⬜ |
| Stream-Deck | ⬜ | ⬜ |
| MCP-Server | ⬜ | ⬜ |

## Optionale Vertiefung (kein eigener Almanach noetig)

- **PowerShell-Scripting allgemein** (ausserhalb von Hooks): waechst zunaechst als Abschnitt
  in `claude-hooks.md`. Bei genug Eigenleben spaeter als `powershell.md` ausgliedern.
