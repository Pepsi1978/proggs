Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird

Vor echter Arbeit an einem technischen Bereich IMMER zuerst im zweiten Gehirn (MCP `second-brain`)
nachschlagen, ob ein Bug-Almanach (bekannte Fehler+Fix) und Best Practices (wie man's richtig macht)
existieren — damit bekannte Fehler gar nicht erst passieren. Kategorie-Baum: siehe Tabelle unten.

## Digest-Modell — 3 Stufen (ueber second-brain-Tools, NIE eine ganze Kategorie als Block laden)
- **A (vor JEDER echten Arbeit):** `recall(query="<Bereich>", category="Programmierung/Almanache/Kurzchecks")`
  lesen, DANACH dieselbe Suche mit `category="Programmierung/Best Practices/Kurzchecks"`. Reihenfolge:
  erst Almanach-Kurzcheck, dann Best-Practices-Kurzcheck, dann erst coden.
- **B (ab dem ERSTEN Fehler im Bereich):** Volltext holen via `get_by_title` — GLEICHER Titel, aber ohne
  " Kurzcheck" und mit geaendertem Klammerzusatz: "X Kurzcheck (Almanach Kurzcheck)" -> "X (Almanach)",
  "X Kurzcheck (Best Practices Kurzcheck)" -> "X (Best Practices)". Kurzcheck reicht ab jetzt nicht mehr.
  Zuerst pruefen ob bekannter Bug -> dokumentierten Fix anwenden.
- **C (Hochrisiko: Claude-Hooks/-Config, R8, Firebase-Billing):** gleich vorab den Volltext lesen.

## Kein Eintrag im Gehirn fuer den Bereich
- Trivialer Kleinkram (String, Doku, Kommentar, Versions-Bump): normal weiterarbeiten.
- Echte Bereichs-Arbeit UND vom Modell als nicht-trivial/schwierig eingeschaetzt: IMMER kurz anzeigen
  `Risiko: gering/mittel/hoch — <1 Satz warum>`. Bei mittel/hoch ZUERST den `researcher`-Subagenten fuer
  eine kurze gezielte Recherche starten, bevor programmiert wird.

## Inhaltsverzeichnis: Programmierung/Almanache/* + Programmierung/Best Practices/* (Second Brain)
Gleiche Unterkategorie-Namen in beiden Zweigen (Ausnahme markiert); je Bereich meist Kurzcheck +
Volltext (s.o.). Daneben: `Programmierung/Rules` (Arbeitsregeln), `Programmierung/Projekte`
(-> `projekt-wissen-aus-gehirn.md`), `Programmierung/Direktiven`.

| Unterkategorie | Alm | BP | Stichworte |
|---|---|---|---|
| API | x | x | Anthropic/OpenAI/OpenRouter/Gemini/Mistral/Groq/DeepSeek/xAI, Firecrawl, OAuth Device-Code, CLI-Impersonation |
| Agenten | x | x | Orchestrator-/Boss-Agent, Loop Engineering, Multi-Agent-Patterns |
| Android | x | x | Kotlin, Compose, Room, Hilt/Dagger+KSP, Retrofit/OkHttp/Moshi, Media3/ExoPlayer, Coil3, Firebase/Billing, WorkManager, Widgets, Wake-Word |
| Android Build | x | x | Gradle/AGP, R8, Play-Store-Release & Policy |
| Assets | x | x | Icon-Building, 3D-visuelle-Qualitaet (PBR/Licht/PostFX) |
| Claude Tooling | x | x | Hooks, Skills, MCP-Server(-Bau), Settings, Slash-Commands, Agents, Cowork(+Tasks), Plugins, Kontext/Token-Effizienz |
| Desktop | x | x | Swift/AppKit (macOS), C#/.NET/WPF (Win), Wake-Word, Whisper/Groq-Transkription, Voice-Pipeline, Text-Injection, 3D (Godot/Rust-wgpu/Metal/DirectX) |
| Opencode | x | x | OpenCode-CLI: Grundlagen, Agents/Modes, AGENTS.md/Memory, Konfig, Modellauswahl/OpenRouter-Go, Recherche-Pipeline |
| Peripherie | x | x | Stream Deck Plugins |
| Second Brain | – | x | Cortex/Qdrant, mem0, Datenmodell, Ingestion, RAG-Retrieval, Multi-Client (MCP+REST), Hostinger |
| Server | x | x | Docker, FastAPI, Qdrant, WireGuard, Samba/SMB, Caddy/TLS, VPS-Hosting, autonome KI-Agenten-Server |
| Web | x | x | Chrome-Erweiterungen (MV3), TypeScript/Node.js, Webdesign, Three.js/3D-Web, Lottie/Rive/SVG |

## Gilt AUCH fuer OpenCode selbst (Harness-Arbeit)
AGENTS.md/Regel/Konfig -> Kurzcheck+Volltext unter Claude Tooling ODER Opencode; Python-Skript -> Claude Tooling.

## Nach der Aufgabe
Jeden NEU erlebten Bug/jede neue Erkenntnis melden, damit sie ins Gehirn nachgezogen wird.

## NIEMALS
- An einem Bereich mit vorhandenem Kurzcheck arbeiten, ohne ihn gelesen zu haben (Stufe A).
- Nach einem Fehler im Bereich weiterarbeiten ohne den Volltext (Stufe B).
- In einem Hochrisiko-Bereich nur den Kurzcheck lesen (Stufe C verlangt Volltext).
- Eine ganze Gehirn-Kategorie als EINEN Block laden (immer einzeln/gezielt: recall/get_by_title).
- Bei unbekanntem UND schwierigem Terrain ohne Risiko-Hinweis + Research einfach drauflos programmieren.
- Einen Bug "loesen", indem Funktionalitaet entfernt wird (Loesungen sind funktionserhaltend).
