# Best-Practices — Projekt-Code

Diese Sektion ergaenzt die Harness-Kategorien (`01-hooks` … `12-neues`) um Best-Practices
fuer die **Software/Sprachen, die in den Projekten benutzt werden** — Kotlin, Swift,
Gradle, .NET/WPF, TypeScript, Rust usw. Sie ist die zweite Seite der Medaille zum
Bug-Almanach (`~/proggs/bugs/`): der Almanach sammelt *was schiefgeht und wie man es
umgeht*, diese Sektion sammelt *wie man es von vornherein richtig macht, damit der Bug
gar nicht erst entsteht*.

## Wer schreibt hier rein

- **`bug-almanach-recherche`-Skill:** traegt bei jeder Bug-Recherche die allgemeingueltige
  Praevention/Best-Practice einer gefundenen Loesung hier ein (gezielt, pro Bug).
- **`best-practices`-Skill:** kann eine ganze Software gezielt aufrollen
  ("recherchiere Best-Practices nur fuer Kotlin") und die neuesten Empfehlungen pflegen.

## Struktur (seit 2026-06-03: nach Kategorie gruppiert, selbst-identifizierende Dateinamen)

Jede Best-Practice-Datei liegt **direkt im Kategorie-Ordner** und traegt den Software-Namen
**im Dateinamen** (`best-practices-<software>.md`) — kein generischer `best-practices.md`
mehr und kein Software-Unterordner. So ist sofort am Dateinamen erkennbar, worum es geht,
auch wenn eine Datei mal woandershin geraet. Dieselben Kategorien wie der Bug-Almanach
(`bugs/<kategorie>/`), damit beide Seiten der Medaille deckungsgleich sind. Ein blosser
Kategorie-Wechsel ist unkritisch: `check-coupling.py` paart ueber den Software-Namen
(aus dem Dateinamen, rekursiv gesucht), nicht ueber die Kategorie.

```
projekt-code/
├── README.md                                  ← diese Datei
├── android/
│   ├── best-practices-kotlin.md
│   ├── best-practices-jetpack-compose.md      ← Android-UI (Stand 2026-06-02, BOM 2025.01.01 / 2026.03.00)
│   ├── best-practices-android-platform.md     ← Framework/Platform-SDK (Stand 2026-06-02, targetSdk 36, Room 2.7.0, WorkManager 2.10.0)
│   └── best-practices-firebase-billing.md     ← Firebase + Play Billing (Stand 2026-06-02, Billing 7.1.1, Firebase BOM 34.11.0)
├── android-build/
│   ├── best-practices-gradle.md               ← Gradle/AGP/R8/KSP (Stand 2026-06-02)
│   └── best-practices-r8.md                    ← R8 Shrinker/Optimizer — Play-Store-Release-Fokus, A–J (Stand 2026-06-03, AGP 8.7.3 / 8.10.0)
├── desktop/
│   ├── best-practices-dotnet-csharp.md
│   └── best-practices-swift-appkit.md         ← macOS Swift/AppKit Overlay-Apps (Stand 2026-06-02, Swift 6.3.2 / Xcode 26.5, macOS 13+)
├── web/
│   ├── best-practices-chrome-extensions.md    ← Chrome/Edge MV3 (Stand 2026-06-02, Chrome 148)
│   └── best-practices-typescript.md           ← TypeScript/Node.js (Stand 2026-06-03, Node 24.15.0 / TS 6.0.2 / npm 11.12.0 / Bun 1.3.11)
├── peripherie/
│   └── best-practices-stream-deck.md          ← Elgato Stream Deck Plugins (Stand 2026-06-03, Stream Deck 7.4.2 / @elgato/streamdeck 2.1.0 / SDKVersion 2+3)
└── claude-tooling/
    ├── best-practices-mcp-server.md           ← MCP-Server-Bau (Stand 2026-06-03, MCP TS-SDK 1.27.1/1.29.0, zod v4, Spec 2025-11-25) — Gegenstueck zu bugs/claude-tooling/mcp-server.md
    └── best-practices-python-windows.md       ← Python auf Windows / Cross-Platform-Scripting (Stand 2026-06-02, CPython 3.13.13)
```

Vorhandene Kategorien & Dateien:
**android** (best-practices-kotlin, -jetpack-compose, -android-platform, -firebase-billing) ·
**android-build** (-gradle, -r8) ·
**desktop** (-dotnet-csharp, -swift-appkit) ·
**web** (-chrome-extensions, -typescript) ·
**peripherie** (-stream-deck) ·
**claude-tooling** (-mcp-server, -python-windows)
(jede Datei mit Bezugs-Tabelle zum passenden Bug-Almanach in `~/proggs/bugs/<kategorie>/`).

Dateiname-Konvention: `best-practices-<software>.md`. Inhalt beginnt mit
`# <Software> — Best Practices (Stand JJJJ-MM-TT, Version X)`.

## Unterschied zu den Harness-Kategorien (wichtig)

| | Harness-Kategorien (01–12) | Projekt-Code (diese Sektion) |
|---|----------------------------|------------------------------|
| Thema | Claude-Code-Werkzeuge (Hooks, Skills, MCP, Settings …) | Software in den Projekten (Kotlin, Swift, Gradle …) |
| Changelog-Quelle | offizieller **Claude-Code**-Changelog (`update-changelog.ps1`) | der **eigene** Changelog der Software (Kotlin-Releases, Swift-Releases …) — KEIN Claude-Script |
| Versions-Anker | installierte Claude-Code-Version | live ermittelte Version der jeweiligen Software |

## Quellen-Rangordnung (wie im Rest des Ordners)

Offizielle Hersteller-Quelle (JetBrains/Kotlin, Apple/Swift, Gradle, Microsoft/.NET …) =
Grundwahrheit. Community/Blogs = gelabelte `extern`-Alternative, ueberstimmt nie das
Offizielle. Jeder Eintrag traegt Quelle + Datum + `offiziell`/`extern`-Flag.
