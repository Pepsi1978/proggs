# ✅ INHALTSVERZEICHNIS BEST-PRACTICES

> **Die „richtige Seite der Medaille" zum Bug-Almanach (`~/proggs/bugs/`).**
> Für jeden Bereich, an dem gearbeitet wird, liegt hier eine `.md`-Datei mit den
> **bewährten, funktionserhaltenden Best-Practices** — *wie man es von vornherein
> richtig macht, damit der Bug gar nicht erst entsteht*. Der Almanach sagt *was
> schiefgeht und wie man es löst*, diese Seite sagt *wie man es richtig macht*.
> Beide Kurzchecks werden VOR der Arbeit gelesen (Digest-Modell, Stufe A — erst
> Almanach, dann Best Practices). Das vollständige Systemverhalten steht in
> [`SYSTEM.md`](SYSTEM.md).

---

## Struktur (seit 2026-06-16: flach, 1:1 wie `bugs/`)

Die Best-Practices liegen — exakt symmetrisch zum Bug-Almanach — in
**Kategorie-Unterordnern** mit selbst-identifizierendem Dateinamen:
`best-practices/<kategorie>/<bereich>.md` (z. B. `android/room.md`,
`claude-tooling/hooks.md`). Kein `best-practices-`-Präfix, keine
`projekt-code/`-Zwischenebene mehr — dieselben Kategorien und Dateinamen wie
`bugs/<kategorie>/<bereich>.md`, damit beide Seiten der Medaille deckungsgleich sind.

> **Historie:** Bis 2026-06-16 lag Projekt-Wissen unter
> `best-practices/projekt-code/<kat>/best-practices-<bereich>.md` und das generische
> Harness-Wissen in nummerierten Ordnern `01-hooks` … `12-neues`. Beides wurde auf die
> flache, präfixlose 1:1-Struktur migriert (Commits #46839–#46848 ff.). `check-coupling.py`
> paart weiterhin über den Bereichs-Namen (rekursiv gesucht), ein Kategorie-Wechsel ist
> also unkritisch.

```
best-practices/
├── README.md · SYSTEM.md · _state.json · _changelog-archiv.md   (oben, kategorielos)
├── android/          Kotlin, Jetpack Compose, Android-Platform/SDK, Firebase/Billing, Drive-Backup, WorkManager, Hilt, Retrofit, Media3, Coil3, 3D (Filament), Voice-Trigger
├── android-build/    Gradle/AGP, R8, Play-Store-Release
├── desktop/          .NET/WPF (Windows), Swift/AppKit (macOS), Overlays, Whisper-STT, Text-Injection, 3D (Metal/.NET/Rust/Godot), Windows-Tastenkombinationen
├── web/              Chrome-Erweiterungen, TypeScript/Node, 3D (Three.js/WebGPU), Webseitenbau/Webdesign/Effekte, Lottie/Rive/SVG-Animationen
├── apis/             LLM-/HTTP-API-Integration + OAuth/Auth (OpenAI, Anthropic, Gemini, Groq, OpenRouter, xAI, Mistral, DeepSeek, lokal, OAuth, TTS, …)
├── peripherie/       Elgato Stream-Deck-Plugin
├── assets/           App-Icon-Building, 3D-Visuelle-Qualität (PBR/Licht/PostFX)
├── agents/           Boss-/Orchestrator-Agent im Multi-Agenten-System + Loop Engineering (autonome Agenten-Schleifen) + Anti-Halluzinations-Regeln (Modelle/Agenten zum Nicht-Erfinden bewegen)
├── second-brain/     Selbstgehostetes persönliches Memory-System („zweites Gehirn"/PKM): VPS, Memory-Backends, Datenmodell, Orchestrator+Suche, Schreibpfad, Multi-Client, Sicherheit, Qualität, Referenz-Architekturen
└── claude-tooling/   Claude-Code-Werkzeuge (Harness) — siehe Doppelnatur unten
```

---

## Kategorie-Übersicht

| Kategorie | Dateien | Inhalt | Bug-Gegenstück |
|-----------|--------:|--------|----------------|
| `android/` | 13 | Kotlin/Compose/Android-Stack + Libraries | `bugs/android/` |
| `android-build/` | 3 | Gradle/AGP, R8, Play-Store-Release | `bugs/android-build/` |
| `desktop/` | 14 | .NET/WPF, Swift/AppKit, Overlays, STT, 3D nativ, Windows-Tastenkombinationen | `bugs/desktop/` |
| `web/` | 5 | Chrome-Erweiterungen, TypeScript, 3D Web, Webseitenbau/Webdesign/Effekte, Lottie/Rive/SVG-Animationen | `bugs/web/` |
| `apis/` | 15 | LLM-/HTTP-APIs + OAuth (14 gepaart + `multi-provider` ungepaart) | `bugs/apis/` |
| `peripherie/` | 1 | Elgato Stream-Deck | `bugs/peripherie/` |
| `assets/` | 2 | Icon-Building, 3D-Optik | `bugs/assets/` |
| `agents/` | 3 | Orchestrator-Agent, Loop Engineering, Anti-Halluzinations-Regeln (konzeptionell, ungepaart) | `bugs/agents/` |
| `second-brain/` | 9 | Selbstgehostetes persönliches Memory („zweites Gehirn") — Architektur/Best-Practices (konzeptionell) | — (ungepaart) |
| `claude-tooling/` | 22 | Harness — Bug-gepaart **und** generisches Harness-Wissen | `bugs/claude-tooling/` (teilweise) |
| `design/` | 1 | Design-zu-Code-Treue: Handoff-Formate, Responsivität, Verifikation | `bugs/design/` |

---

## Doppelnatur von `claude-tooling/`

`claude-tooling/` enthält zwei Sorten Dateien, die bewusst koexistieren:

1. **Bug-gepaarte Digests** (Gegenstück zu `bugs/claude-tooling/<x>.md`):
   `claude-hooks.md`, `mcp-server.md`, `claude-config.md`, `cowork.md`,
   `cowork-git-push.md`, `cowork-scheduled-tasks.md`, `python-windows.md`,
   `claude-code-desktop-vs-cli.md`, `agent-knowledge-system.md`. Diese tragen die
   Bezugs-Tabelle zum Almanach und werden von `check-coupling.py` als Paar geführt.

2. **Generisches Harness-Wissen** (früher die nummerierten Ordner `01-hooks`…`12-neues`):
   `hooks.md`, `skills.md`, `agents.md`, `plugins.md`, `mcp.md`, `commands.md`,
   `settings.md`, `kontext.md`, `token-effizienz.md`, `arbeitsweise.md`,
   `researcher.md`, `neues.md`. Das sind die ausführlichen Best-Practices **wie man die
   Claude-Code-Werkzeuge am besten benutzt**. Sie haben (noch) kein eigenes Bug-Pendant
   und erscheinen in `check-coupling.py` daher als `[INFO]` (ungepaart). Wo ein
   spezifischer Digest auf den Volltext zeigt (z. B. `claude-hooks.md` → `hooks.md`),
   ist die ausführliche Datei die Tiefe.

> `neues.md` ist die Auffangzone für Themen ohne eigene Kategorie und bleibt das
> generische Sammelbecken des Horizont-Scans.

---

## Quellen & Pflege

- **Quellen-Rangordnung:** Offiziell (Hersteller-Doku/Changelog) = Grundwahrheit,
  extern = klar gelabelte Alternative. Jeder Eintrag trägt Quelle (URL) + Datum + Version
  + `offiziell`/`extern`-Flag.
- **Versions-Anker:** die live ermittelte installierte Version der jeweiligen Software.
- **Stand des Harness-Wissens:** siehe [`_state.json`](_state.json) (`last_version` / `last_checked`).
- **Claude-Code-Changelog-Archiv (Recherche-Quelle):** [`_changelog-archiv.md`](_changelog-archiv.md) — das
  **wortwörtliche** Claude-Code-Changelog von GitHub, vom `best-practices`-Skill bei jedem Lauf neu geholt
  (NICHT von Hand bearbeiten — wird überschrieben).
- **Struktur-/System-Historie dieses Ordners:** [`SYSTEM.md`](SYSTEM.md) §8 (persistent).
- **Wer schreibt hier rein:**
  - `bug-almanach-recherche`-Skill — trägt bei jeder Bug-Recherche die Prävention/Best-Practice ein.
  - `best-practices`-Skill — rollt eine Software/ein Harness-Thema gezielt auf und pflegt die neuesten Empfehlungen.
- **Kopplung:** `python bugs/check-coupling.py` prüft, dass jede gepaarte Datei wechselseitig
  mit ihrem Almanach verlinkt ist; `python bugs/health.py` ist der Gesamt-Selbsttest.
