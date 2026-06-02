# 🐛 INHALTSVERZEICHNIS BUGS

> **Der zentrale Bug-Almanach pro Technologie-Bereich.**
> Hier liegt fuer jeden Bereich, an dem gearbeitet wird, eine eigene `.md`-Datei mit
> den oeffentlich bekannten Bugs/Fallen **und ihren bewaehrten, funktionserhaltenden
> Loesungen**. Ziel: Bekannte Fehler werden VOR der Arbeit nachgeschlagen, statt
> hinterher teuer debuggt (Poka-Yoke Stufe 3). Das vollstaendige Systemverhalten
> steht in [`SYSTEM.md`](SYSTEM.md).

---

## So funktioniert es (Kurzfassung)

1. **Vor** echter Arbeit an einem technischen Bereich: pruefen, ob es hier einen
   Almanach fuer den Bereich gibt (diese Liste).
2. **Almanach vorhanden** → komplett lesen, Versionen abgleichen, DANN arbeiten.
3. **Kein Almanach** → Frank Bescheid geben, auf sein **OK** warten, dann die
   bekannten Bugs des Bereichs recherchieren und hier einen neuen Almanach anlegen.
4. **Neuen Bug erlebt** → in den passenden Almanach eintragen (Bug + Loesung + Version).

Drei Automatik-Schichten sorgen dafuer, dass das in **jeder** Session laeuft:
Session-Hook (`bug-almanac-index`) blendet diese Liste beim Start ein · Datei-Hook
(`bug-almanac-guard`) erinnert beim Anfassen bereichstypischer Dateien an den Almanach ·
Regel `known-bugs-before-coding.md` als Verhaltensschicht.

---

## ✅ Vorhandene Almanache

| Bereich | Datei | Stand | Bugs | Erkennungs-Trigger (Dateien / Stichworte) |
|---------|-------|-------|------|-------------------------------------------|
| **Browser-Erweiterungen** (Chrome/Edge, MV3) | [`chrome-extensions.md`](chrome-extensions.md) | 2026-06-01 | 10 | `manifest.json` (mit `manifest_version`), `background.js`, `service-worker.js`, `*/overlays/*`, `chrome.*`-APIs, `getUserMedia`/Mikrofon · „Erweiterung", „Extension", „Overlay", „Mikrofon" |
| **Claude-Harness — Hooks** (PowerShell/Bash) | [`claude-hooks.md`](claude-hooks.md) | 2026-06-01 | ~45 | `~/.claude/hooks/*.ps1`, `*.sh`, `settings.json` hooks-Sektion · „Hook", „SessionStart", „PreToolUse", „PostToolUse", „SubagentStop" |
| **Kotlin** (Sprache/K2 + Coroutines + Compose-Kontext) | [`kotlin.md`](kotlin.md) | 2026-06-02 | ~46 | `*.kt`, `*.kts` (ausser `build/settings.gradle.kts` → Gradle), `AndroidManifest.xml` · „Kotlin", „K2", „Coroutines", „Flow", „Compose", „Android" |
| **C# / .NET 8** (WPF, WinUI 3, Konsole, Backend) | [`dotnet-csharp.md`](dotnet-csharp.md) | 2026-06-02 | ~130 | `*.cs`, `*.csproj`, `*.xaml` · „WPF", „WinUI", „.NET", „C#", „Overlay" |

---

## ⬜ Bereiche ohne Almanach (bei erster echter Arbeit: recherchieren — erst Franks OK)

> Diese Liste ist die Landkarte der erwarteten Bereiche. Sobald an einem davon
> echte Arbeit beginnt und noch kein Almanach existiert, wird er angelegt.

> Priorisiert nach Franks tatsaechlicher Nutzung (⭐ = hoher Hebel, oft angefasst).
> Android ist bewusst granular geschnitten (Sprache/UI/Framework/Build getrennt), weil
> jeder dieser Sektoren eigene Fehlerklassen hat.

| Prio | Bereich | (geplante Datei) | Erkennungs-Trigger (Dateien / Stichworte) |
|------|---------|------------------|-------------------------------------------|
| ⭐ | **Build — Gradle / AGP / R8 / KSP** | `gradle.md` | `build.gradle*`, `settings.gradle*`, `gradle/*`, `gradle.properties` · „Gradle", „AGP", „R8", „KSP" |
| ⭐ | **Python (v.a. Windows-Encoding)** | `python-windows.md` | `*.py` · „Python", „Encoding", „BOM", „cp1252", „UnicodeEncodeError" |
| ⭐ | **Jetpack Compose** (Android-UI) | `jetpack-compose.md` | `*.kt` mit `@Composable`/`setContent` · „Compose", „Recomposition", „remember", „LazyColumn", „Modifier" |
| · | **Android-Framework / Platform** | `android-platform.md` | `AndroidManifest.xml`, Lifecycle/Room/WorkManager · „Lifecycle", „Room", „WorkManager", „Permission", „Service" |
| · | **Firebase / Crashlytics / Play Billing** | `firebase-billing.md` | `google-services.json`, `BillingClient`, Cloud Functions · „Firebase", „Crashlytics", „FCM", „Billing", „Paywall" |
| · | **macOS-Desktop — Swift / AppKit** | `swift-appkit.md` | `*.swift`, `*.xcodeproj`, `Info.plist` · „Swift", „AppKit", „SwiftUI" |
| · | **Tampermonkey / Userscripts** | `tampermonkey.md` | `*.user.js` · „Tampermonkey", „Userscript", „Greasemonkey" |
| · | **Web — TypeScript / Node** | `typescript.md` | `*.ts`, `*.tsx`, `tsconfig.json` · „TypeScript", „Node", „npm" |
| · | **Stream-Deck-Plugin (Node-SDK)** | `stream-deck.md` | `*.sdPlugin/*`, Stream-Deck-`manifest.json`, `propertyInspector` · „Stream Deck", „Elgato" |
| · | **MCP-Server-Bau** | `mcp-server.md` | `.mcp.json`, MCP-SDK · „MCP", „Model Context Protocol", „stdio", „tool schema" |

> **Fertige Recherche-Prompts** fuer alle 10 offenen Bereiche (Almanach + Best-Practices,
> Copy-Paste fuer parallele Sessions): siehe [`OFFENE-ALMANACHE-PROMPTS.md`](OFFENE-ALMANACHE-PROMPTS.md).
>
> **Moegliche Vertiefung** als Abschnitt statt eigener Datei: PowerShell-Scripting allgemein
> → `claude-hooks.md` (bei genug Eigenleben spaeter ausgliedern).

(Liste waechst mit. Neue Bereiche hier ergaenzen, sobald sie auftauchen — und das
Pfad-Mapping im `bug-almanac-guard`-Hook entsprechend nachziehen.)

---

## Aufbau jeder Almanach-Datei (Format-Vorlage)

```
# Bekannte Bugs: <Thema>
> PFLICHT-LESEN vor Arbeit an <Thema>.
> Stand: zuletzt recherchiert am <Datum> fuer Version <X>.

## N. <Bug-Titel>   [⭐ HAEUFIG falls oft]
Symptom:    Was man sieht
Ursache:    Der wahre Grund
Versionen:  betrifft V1-V3, gefixt ab V4   (oder „per Design / unabhaengig")
FIX:        Beste funktionserhaltende Loesung (NIE „Feature weg")
Quelle:     Link / eigener Vorfall
```
