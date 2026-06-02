# Offene Almanache — fertige Recherche-Prompts (Copy-Paste)

> Stand: 2026-06-02. Fuer jeden Bereich OHNE Almanach stehen hier zwei fertige Prompts:
> einer startet den `bug-almanach-recherche`-Skill (bekannte Bugs + Loesungen), einer den
> `best-practices`-Skill (wie man es von vornherein richtig macht). Beide gehoeren zusammen
> (zwei Seiten einer Medaille, siehe `SYSTEM.md` §9). Landkarte aller Bereiche: `README.md`.

---

## So benutzt du diese Prompts

1. Oeffne eine neue Claude-Code-Session im Repo (`~/proggs`).
2. Kopiere EINEN Prompt-Block von unten und fuege ihn als Nachricht ein.
3. Der genannte Skill startet automatisch, ermittelt die Version live, recherchiert mit
   einem Researcher-Schwarm, legt die Datei an, traegt sie in `README.md` ein, ergaenzt das
   Hook-Pfad-Mapping und committet + pusht selbst.
4. Pro Bereich gibt es zwei Prompts: **zuerst** den Almanach-Prompt, **danach** den
   Best-Practices-Prompt.

## Wichtig: nicht zu viele gleichzeitig (RPM-Schutz)

- Jeder dieser Prompts startet selbst einen Researcher-Schwarm (5–7 parallele Researcher).
- Mehrere solcher Sessions GLEICHZEITIG addieren die Researcher → 429/RPM-Absturz-Risiko.
- **Empfehlung: hoechstens 2–3 dieser Recherche-Sessions parallel laufen lassen.**
- Fuer DENSELBEN Bereich: erst den Almanach-Prompt fertig laufen lassen, DANN den
  Best-Practices-Prompt — sie koppeln sich gegenseitig (`SYSTEM.md` §9), nicht gleichzeitig
  fuer denselben Bereich starten.

## Reihenfolge-Empfehlung (nach Hebel)

Gradle ⭐ → Python ⭐ → Jetpack Compose ⭐ → Android-Framework → Firebase/Billing →
Swift/AppKit → Tampermonkey → TypeScript → Stream-Deck → MCP-Server.

---

## 1. Gradle / AGP / R8 / KSP (Android-Build)

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer das Android-Build-System: Gradle, Android Gradle Plugin (AGP), R8/ProGuard und KSP. Ermittle die aktuell benutzten Versionen LIVE aus meinen Android-Projekten in ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor (z.B. ./gradlew --version sowie die Versionen in gradle/libs.versions.toml und build.gradle.kts). Lege einen eigenen Almanach bugs/gradle.md an, trage ihn in bugs/README.md (von "ohne Almanach" nach "Vorhandene") ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (build.gradle*, settings.gradle*, gradle.properties, gradle/*). Grenze klar gegen kotlin.md (Sprache) ab — hier nur Build-, Dependency-, R8- und KSP-Themen. Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer das Android-Build-System (Gradle, AGP, R8, KSP) — die Projekt-Code-Seite, NICHT den Claude-Harness. Gleiche gegen die aktuell benutzten Versionen in ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor ab. Speichere unter best-practices/projekt-code/gradle/best-practices.md und koppele die Funde wechselseitig mit dem Bug-Almanach bugs/gradle.md (Bezugs-Tabelle in beiden Dateien). Am Ende committen und pushen.
```

---

## 2. Python (Windows-Encoding & Cross-Platform-Scripting)

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer Python mit Schwerpunkt Windows-Encoding und Cross-Platform-Scripting: cp1252-vs-UTF-8, BOM, UnicodeEncodeError beim Schreiben von JSON mit Emojis/Umlauten, open(...encoding='utf-8'), Pfad-Probleme (/c/Users vs native Windows-Pfade), atomares Schreiben (temp+rename). Diese Encoding-Fehler sind bei mir schon mehrfach aufgetreten (BOM-Bug 2x). Benutzte Python-Version LIVE ermitteln (python --version). Scripts liegen u.a. in ~/proggs/scripts und ~/proggs/tools. Lege bugs/python-windows.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.py). Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer Python auf Windows / Cross-Platform-Scripting (Projekt-Code, nicht Harness): Encoding (immer encoding='utf-8'), atomares Schreiben, Pfad-Handling, venv/Abhaengigkeiten. Speichere unter best-practices/projekt-code/python-windows/best-practices.md und koppele wechselseitig mit bugs/python-windows.md. Am Ende committen und pushen.
```

---

## 3. Jetpack Compose (Android-UI)

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer Jetpack Compose (Android-UI): Recomposition-Fallen, State-Hoisting, remember/rememberSaveable/derivedStateOf, Modifier-Reihenfolge, LazyColumn/LazyRow (Keys, Scroll-State), Navigation-Compose, Stability/Performance (@Stable, Skippability), Side-Effects (LaunchedEffect/DisposableEffect/SideEffect). Ermittle die benutzte Compose-Version LIVE aus ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor (Compose-BOM / Compose-Compiler in gradle/libs.versions.toml). Lege bugs/jetpack-compose.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.kt mit @Composable / setContent). Grenze klar gegen kotlin.md (reine Sprache/Coroutines) und android-platform.md (Framework) ab — hier nur Compose-UI. Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer Jetpack Compose (Projekt-Code, nicht Harness): State-Management, Recomposition-Vermeidung, Stability, LazyList-Keys, Theming. Gleiche gegen die benutzte Compose-Version in ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor ab. Speichere unter best-practices/projekt-code/jetpack-compose/best-practices.md und koppele wechselseitig mit bugs/jetpack-compose.md. Am Ende committen und pushen.
```

---

## 4. Android-Framework / Platform-SDK

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer das Android-Framework/Platform-SDK: Activity/Fragment-Lifecycle, Permissions (Runtime + neue API-Level-Restriktionen), Foreground-Services, WorkManager, Room (SQLite, Migrationen, WAL-Checkpoint), Notifications, Intents/PendingIntent (Mutability-Flags), Scoped Storage, Background-Restrictions/Doze. Ermittle compileSdk/targetSdk/minSdk LIVE aus ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor (build.gradle.kts). Lege bugs/android-platform.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (AndroidManifest.xml und android-spezifische Verzeichnisse). Grenze gegen kotlin.md (Sprache), jetpack-compose.md (UI) und gradle.md (Build) ab — hier nur Framework-/Runtime-Themen. Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer das Android-Framework/Platform-SDK (Projekt-Code, nicht Harness): Lifecycle-sicheres Arbeiten, Room-Migrationen, WorkManager, Permissions, Foreground-Services, Storage. Gleiche gegen compileSdk/targetSdk in ~/proggs/BestJournalAndroid und ~/proggs/EntropieReductor ab. Speichere unter best-practices/projekt-code/android-platform/best-practices.md und koppele wechselseitig mit bugs/android-platform.md. Am Ende committen und pushen.
```

---

## 5. Firebase / Crashlytics / Google Play Billing

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer die Google-Backend-Dienste meiner Android-App: Firebase (Auth, Firestore, Cloud Functions, Crashlytics inkl. Mapping-Upload, FCM, Remote Config) und Google Play Billing (BillingClient, Abos, acknowledgePurchase, Pending Purchases, Proration, Subscription-Status-Validierung). Hauptprojekt: ~/proggs/BestJournalAndroid (nutzt google-services.json + BillingClient). Ermittle die benutzten SDK-Versionen LIVE aus gradle/libs.versions.toml. Lege bugs/firebase-billing.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (google-services.json, BillingClient/Billing-/Subscription-Klassen). Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer Firebase + Google Play Billing (Projekt-Code, nicht Harness): Crashlytics-Mapping, sichere Billing-Flows, Abo-Status serverseitig validieren, FCM. Hauptprojekt ~/proggs/BestJournalAndroid. Speichere unter best-practices/projekt-code/firebase-billing/best-practices.md und koppele wechselseitig mit bugs/firebase-billing.md. Am Ende committen und pushen.
```

---

## 6. Swift / AppKit (macOS-Desktop)

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer macOS-Desktop-Entwicklung mit Swift und AppKit: Overlay-Fenster (NSWindow/NSPanel, Level/Collection-Behavior), Accessibility-API, globale Hotkeys, Mikrofon/AVFoundation-Permissions, App-Sandbox, ruckelfreie Fenster-Animation. Projekte: ~/proggs/ClaudeCodexVoiceOverlay-macOS und ~/proggs/TerminalVoiceOverlay-macOS. Swift-Version LIVE ermitteln (swift --version); falls die Session auf Windows laeuft (kein Swift installiert): fuer die aktuelle stabile Swift-/Xcode-Version recherchieren. Lege bugs/swift-appkit.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.swift, *.xcodeproj, Info.plist). Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer Swift + AppKit (macOS-Desktop, Projekt-Code, nicht Harness): Overlay-Fenster, Concurrency (async/await, Actor), Accessibility, Permissions. Projekte ~/proggs/ClaudeCodexVoiceOverlay-macOS und ~/proggs/TerminalVoiceOverlay-macOS. Speichere unter best-practices/projekt-code/swift-appkit/best-practices.md und koppele wechselseitig mit bugs/swift-appkit.md. Am Ende committen und pushen.
```

---

## 7. TypeScript / Node

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer TypeScript und Node.js: Modul-System (ESM vs CommonJS), async/Promises (unhandled rejections), tsconfig strictness, haeufige Typ-Fallen, npm-/Dependency-Probleme (peer deps, ESM-only Pakete), Bun-Kompatibilitaet. Projekt-Beispiel: ~/proggs/mcp-code-search. Node-/TypeScript-Version LIVE ermitteln (node --version, package.json, tsconfig.json). Lege bugs/typescript.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.ts, *.tsx, tsconfig.json). Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer TypeScript + Node.js (Projekt-Code, nicht Harness): strict-Konfiguration, Modul-Setup, Fehlerbehandlung, Typsicherheit. Speichere unter best-practices/projekt-code/typescript/best-practices.md und koppele wechselseitig mit bugs/typescript.md. Am Ende committen und pushen.
```

---

## 8. Tampermonkey / Userscripts

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer Tampermonkey/Userscripts: GM_-APIs (@grant), @match/@include-Fallen, unsafeWindow, SPA-DOM-Timing (Elemente noch nicht da), MutationObserver, CSP-Probleme, Background-Throttling (setInterval gedrosselt). Scripts liegen in ~/proggs/Tampermonkey (*.user.js). Beruecksichtige die Tampermonkey-Engine- und Browser-Version. Lege bugs/tampermonkey.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.user.js). Grenze klar gegen chrome-extensions.md (MV3-Erweiterungen) ab — hier nur Userscripts. Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer Tampermonkey/Userscripts (Projekt-Code, nicht Harness): robustes DOM-Warten, GM_-API-Nutzung, Persistenz, Update-Mechanik. Scripts in ~/proggs/Tampermonkey. Speichere unter best-practices/projekt-code/tampermonkey/best-practices.md und koppele wechselseitig mit bugs/tampermonkey.md. Am Ende committen und pushen.
```

---

## 9. Stream-Deck-Plugin (Elgato)

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer Elgato-Stream-Deck-Plugin-Entwicklung: Stream Deck SDK, manifest.json (Actions, States), Property Inspector (HTML/JS), WebSocket-Verbindung zum Stream-Deck-Host, Action-Lifecycle (willAppear/keyDown), Node-Backend, Settings-Persistenz. Projekt: ~/proggs/TVO-StreamDeck-Plugin. SDK-/Software-Version aus dem manifest.json ermitteln. Lege bugs/stream-deck.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (*.sdPlugin/*, Stream-Deck-manifest.json, propertyInspector). Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer Elgato-Stream-Deck-Plugins (Projekt-Code, nicht Harness): Action-Lifecycle, bidirektionale Kommunikation Plugin<->Property-Inspector, State-Handling. Projekt ~/proggs/TVO-StreamDeck-Plugin. Speichere unter best-practices/projekt-code/stream-deck/best-practices.md und koppele wechselseitig mit bugs/stream-deck.md. Am Ende committen und pushen.
```

---

## 10. MCP-Server-Bau

**Bug-Almanach:**
```
Starte den Skill bug-almanach-recherche fuer den Bau von MCP-Servern (Model Context Protocol): stdio- vs SSE-Transport, Tool-Schema-Validierung (JSON-Schema), Timeouts, Fehler-Propagation, .mcp.json-Konfiguration (ABSOLUTE Pfade statt nackter Befehlsnamen!), Cross-Platform-Start (Windows vs macOS), Server-Crash-Recovery. Projekt: ~/proggs/mcp-code-search. MCP-SDK-Version aus package.json bzw. Cargo.toml ermitteln. Lege bugs/mcp-server.md an, trage ihn in bugs/README.md ein und ergaenze das Pfad-Mapping im bug-almanac-guard-Hook (.mcp.json, MCP-Server-Quellen). Grenze gegen claude-hooks.md ab. Am Ende committen und pushen.
```

**Best-Practices:**
```
Starte den Best-Practices-Skill fuer den Bau von MCP-Servern (Projekt-Code, nicht Harness): Transport-Wahl, Tool-Schema-Design, Fehlerbehandlung, .mcp.json mit absoluten Pfaden, Cross-Platform. Projekt ~/proggs/mcp-code-search. Speichere unter best-practices/projekt-code/mcp-server/best-practices.md und koppele wechselseitig mit bugs/mcp-server.md. Am Ende committen und pushen.
```

---

## Optionale Vertiefung (kein eigener Almanach noetig)

- **PowerShell-Scripting allgemein** (ausserhalb von Hooks, z.B. Overlay-Start-/Stop-Scripts):
  waechst zunaechst als Abschnitt in `claude-hooks.md`. Bei genug Eigenleben spaeter als
  `powershell.md` ausgliedern.

## Fortschritt abhaken

| Bereich | Almanach erledigt | Best-Practices erledigt |
|---------|:-----------------:|:-----------------------:|
| 1. Gradle | ⬜ | ⬜ |
| 2. Python-Windows | ⬜ | ⬜ |
| 3. Jetpack Compose | ⬜ | ⬜ |
| 4. Android-Platform | ⬜ | ⬜ |
| 5. Firebase/Billing | ⬜ | ⬜ |
| 6. Swift/AppKit | ⬜ | ⬜ |
| 7. TypeScript/Node | ⬜ | ⬜ |
| 8. Tampermonkey | ⬜ | ⬜ |
| 9. Stream-Deck | ⬜ | ⬜ |
| 10. MCP-Server | ⬜ | ⬜ |
