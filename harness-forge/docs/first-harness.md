# Der erste dogfoodete Harness

> Dokumentation der ersten End-to-End-Generierung durch Harness Forge.
> Gepruft via `EndToEndDogfoodingTests.swift` — alle 5 Szenarien laufen
> deterministisch durch die komplette Pipeline.

---

## Testaufgabe

**Aufgabe in Alltagssprache** (direkt aus dem urspruenglichen Prompt-Design):

> "Baue mir einen Begleiter fuer Packrafting-Touren in Schweden mit GPS und
> Tagesplan"

---

## Pipeline-Durchlauf

### Stufe 1 — Zerlegung (Task-Analyzer)

Der `TaskAnalyzer` zerlegt die Aufgabe entlang der 6 Achsen. Das Ergebnis:

| Achse | Wert |
|-------|------|
| Zielumgebung | `mobile` |
| Interaktivitaet | `ongoing` |
| Verifizierbarkeit | `softCriteria` |
| Datenquellen | `sensors` |
| Zielnutzer | `endUsers` |
| Offline-Faehigkeit | `mustOffline` |
| Begruendung | "Unterwegs-Tool mit GPS und Offline-Bedarf." |

### Stufe 2 — Web-Recherche

In diesem Durchlauf deaktiviert (kein `enableWebSearch: true`). Default
ist `NoWebSearchTool`, liefert `[]`. Fuer echten Einsatz waere hier z.B.
Tavily oder Brave Search anzubinden.

### Stufe 3 — Entscheidungs-Matrix

Die Matrix rechnet 5 Harness-Typen × 6 Achsen = 30 Zellen. Die hoechste
Gesamt-Punktzahl gewinnt.

```
Android (Kotlin + Compose)         29/30  ← Empfehlung
Tauri Desktop                      18/30
Claude Subagent                    14/30
Python CLI                         12/30
Pure Prompt                         9/30
```

Die Android-Empfehlung ist eindeutig — auf 5 von 6 Achsen sammelt sie die
maximale oder fast-maximale Punktzahl.

### Stufe 4 — Slug + Begruendung

- **Slug**: `baue-mir-einen-begleiter-fuer-packrafting-t` (auf 40 Zeichen
  gekuerzt, ohne Trailing-Hyphen)
- **Begruendung** (deterministisch aus Matrix + Zerlegung zusammengesetzt):

  > "Empfohlen: Android (Kotlin + Compose). Score: 29/30.
  > Staerken: mobile (5), ongoing (5), sensors (5), endUsers (5), mustOffline (5).
  > Alternative: Tauri Desktop (Score 18/30, 11 Punkte weniger)."

---

## Erzeugte Dateistruktur

Der Builder legt folgendes Projekt an:

```
~/proggs/baue-mir-einen-begleiter-fuer-packrafting-t/
├── build.gradle.kts            (root)
├── settings.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/harnessforge/baue_mir_einen_begleiter_fuer_packrafting_t/
│       │   ├── MainActivity.kt
│       │   ├── MainScreen.kt
│       │   └── LLMClient.kt
│       └── res/values/
│           ├── strings.xml
│           └── themes.xml
├── SYSTEM_PROMPT.md            (Harness-Forge-Marker!)
├── README.md
└── .gitignore
```

---

## Verifizierte Eigenschaften

Aus `EndToEndDogfoodingTests.swift`:

| Pruefung | Ergebnis |
|----------|---------|
| Erwartete Anzahl Dateien (≥13) | ✓ |
| `SYSTEM_PROMPT.md` enthaelt Original-Aufgabe | ✓ |
| `app/build.gradle.kts` vorhanden | ✓ |
| Kein eigenes `.git/` (gehoert ins Mono-Repo) | ✓ |
| Eigene `.gitignore` fuer Build-Artefakte | ✓ |

---

## Nachbau mit echten API-Keys

Sobald ein API-Key gesetzt ist:

```bash
cd ~/proggs/harness-forge
export ANTHROPIC_API_KEY="sk-ant-..."
swift run forge new "Baue mir einen Begleiter fuer Packrafting-Touren in Schweden mit GPS und Tagesplan"
```

Erwarteter Output:

```
Backend: Anthropic Claude
Analysiere Aufgabe ...

Slug: baue-mir-einen-begleiter-fuer-packrafting-t
Empfehlung: Android (Kotlin + Compose)
Kosten der Analyse: $0.0042

| Harness | Umgebung | Inter. | Verif. | Daten | Nutzer | Offline | Total |
|---------|:--------:|:------:|:------:|:-----:|:------:|:-------:|:-----:|
| Android (Kotlin + Compose) | 5 | 5 | 4 | 5 | 5 | 5 | 29 |
| ...

Baue Android (Kotlin + Compose) unter /Users/frank/proggs/... ...

Fertig.
Android-Kotlin-Harness 'baue-mir-einen-...' erzeugt.
- Package: com.harnessforge.baue_mir_einen_...
- 13 Dateien, Compose + Material 3
- Oeffne in Android Studio → Gradle-Sync → Run

Projektpfad: /Users/frank/proggs/baue-mir-einen-...
```

---

## Was dieser Test beweist

1. Die gesamte Pipeline (Analyzer → Matrix → Builder) ist verdrahtet.
2. Alle 5 Builder-Varianten lassen sich aufrufen (zusaetzlicher Test
   `e2eAllBuildersWork` iteriert ueber alle `HarnessType.allCases`).
3. Das Routing (welcher Builder fuer welche Zerlegung) funktioniert
   deterministisch — gleiche Eingabe, gleiche Ausgabe.
4. Erzeugte Projekte sind strukturell korrekt und self-contained.
5. Harness Forge ist **model-agnostisch**: Mit `ScriptedBackend` (fuer Tests)
   genauso nutzbar wie mit Claude/GPT/Gemini im Produktionseinsatz.

---

*Teil der Harness-Forge-Dokumentation. Letztes Update: Step 14 Abschluss.*
