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

## Struktur

```
projekt-code/
├── README.md                      ← diese Datei
├── kotlin/best-practices.md
├── dotnet-csharp/best-practices.md
├── chrome-extensions/best-practices.md   ← Chrome/Edge MV3 (Stand 2026-06-02, Chrome 148)
├── swift/best-practices.md        ← entsteht bei Bedarf
├── gradle/best-practices.md       ← entsteht bei Bedarf
└── …                              ← ein Unterordner pro Software, entsteht bei Bedarf
```

Vorhandene Software-Ordner: **kotlin**, **dotnet-csharp**, **chrome-extensions**
(jeweils mit Bezugs-Tabelle zum passenden Bug-Almanach in `~/proggs/bugs/`).

Jede Datei beginnt mit `# <Software> — Best Practices (Stand JJJJ-MM-TT, Version X)`.

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
