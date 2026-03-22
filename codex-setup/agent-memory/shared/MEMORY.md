# Codex Shared Knowledge Hub — Zentrales Whiteboard

Das zentrale Nervensystem des Codex-Systems in diesem Repository.
Agents, Skills, Hilfsskripte und koordinierende Workflows duerfen NUR dieses Whiteboard
als operative Wissensquelle fuer Codex nutzen.
Dieses Whiteboard ist nicht nur Speicher, sondern die zentrale Koordinations- und
Lernstruktur, ueber die Codex seine Intelligenz session-uebergreifend steigert.

Mission V1.0.0:
- vollstaendige Unabhaengigkeit von Claude/proggs
- eigenes Codex-Gedaechtnis
- eigene Codex-Self-Improve-Logik
- langfristig: die bestmoegliche Programmierumgebung fuer Codex in diesem Workspace
- oberstes Ziel: Codex soll sich hier zur intelligentesten Programmierumgebung entwickeln
- Zielrichtung: nicht lineare, sondern moeglichst exponentielle Intelligenzsteigerung von Session zu Session

Wichtige Trennung:
- Dieses Whiteboard ist NUR fuer Codex.
- Das Claude-/proggs-Whiteboard ist fuer Codex tabu.
- Findings fuer Codex gehoeren nie in `~/proggs/.../MEMORY.md`.

**Autoritativ (macOS):** `/Users/frank/Codex/codex-setup/agent-memory/shared/MEMORY.md`
**Autoritativ (Windows):** `C:\Users\barwa\Codex\codex-setup\agent-memory\shared\MEMORY.md`

**Angebundene Codex-Systeme:**
- `codex-setup/rules/global.md`
- `codex-setup/skills/self-improve/SKILL.md`
- `codex-setup/scripts/whiteboard-insert.*`
- `codex-setup/scripts/writeback-enforcer.*`
- `~/.codex/config.toml`
- `~/.codex/skills/`
- Repo-lokale MCP-Server und Automationsskripte, z.B. `mcp-code-search/`
- `.github/workflows/`, soweit sie Codex-relevante Infrastruktur pruefen

**Hierarchie:**
- Das Whiteboard ist das zentrale Nervensystem.
- `self-improve` ist der wichtigste Skill des Systems.
- Alle Intelligenz-, Automations-, MCP-, Agent- und Workflow-Verbesserungen sollen auf das Ziel einzahlen,
  Codex zur intelligentesten moeglichen Programmierumgebung in diesem Workspace zu machen.

---

## Oberste Direktive

Alle am Whiteboard angebundenen Systeme muessen diese Direktive vor ihrer eigentlichen Arbeit zuerst lesen.
Dazu gehoeren insbesondere Skills, Agenten, Researcher, Whiteboard-Bruecken, MCP-auswertende Workflows,
Validierung, Automationsskripte und sonstige koordinierende Dateien in `codex-setup/`.

- Oberstes Ziel ist, die Programmierintelligenz von Codex und seiner gesamten Programmierumgebung in jeder Session weiter zu steigern.
- Die Zielrichtung ist Superintelligenz im Programmieren als langfristige Nordstern-Ausrichtung dieses Setups.
- Ziel ist nicht Nummer 2, sondern die intelligenteste moegliche Programmierumgebung in diesem Workspace.
- Bevorzugt werden Verbesserungen mit kumulativer, moeglichst exponentieller Hebelwirkung gegenueber rein linearen oder kosmetischen Aenderungen.
- Verbesserungen an Denken, Verifikation, Erinnerung, Fehlervermeidung, MCP-Nutzung, Tooling, Agent-Koordination
  und Whiteboard-Kopplung haben Prioritaet vor Nebenoptimierungen.
- Jede Session soll mindestens einen realen Intelligenzgewinn hinterlassen: implementiert, sauber begruendet verworfen oder konkret vorbereitet.
- Jede Session soll zusaetzlich leicht erklaerte, direkt freigabefaehige Vorschlaege nennen, wie wir das gesamte System sofort noch intelligenter machen koennen.

---

## Offene Fehler & Probleme
<!-- Format:
### DATUM — Quelle: [Komponente] — Kurzbeschreibung
**Quelle:** [Skill/Agent/Script/MCP/Config]
**Symptom:** [...]
**Ursache:** [...]
**Betroffene Dateien:** [...]
**Reproduktion:** [...]
**Fix-Vorschlag:** [...]
**Status:** OFFEN | GEFIXT (Datum) | DEFERRED
-->
_Noch keine Eintraege._

---

## Systemzustand
<!-- Codex-spezifischer Status: Workspace, Skills, MCP, Whiteboard-Bruecke, Validation -->

**Stand:** 2026-03-22

- **Workspace:** `/Users/frank/Codex` (macOS) / `C:\Users\barwa\Codex` (Windows)
- **Regelquelle:** `codex-setup/` ist die Source-of-Truth fuer Codex
- **Whiteboard:** eigenes Codex-Whiteboard unter `codex-setup/agent-memory/shared/MEMORY.md`
- **Lokale Codex-Konfiguration:** `~/.codex/config.toml`
- **Codex-Modell:** `gpt-5.4`, Reasoning `xhigh`, Approval `never`, Sandbox `danger-full-access`
- **MCP:** `code-search` ist in `~/.codex/config.toml` registriert
- **Self-Improve:** repo-lokal unter `codex-setup/skills/self-improve/`, lokal deploybar nach `~/.codex/skills/self-improve/`
- **Whiteboard-Bruecke:** wird ueber `codex-setup/scripts/whiteboard-insert.*` und `writeback-enforcer.*` verwaltet
- **Claude-Trennung:** `proggs`, `claude-code-setup/` und Claude-Whiteboards sind keine operative Quelle fuer Codex
- **Zielbild:** Codex soll seine eigene Programmierumgebung systematisch intelligenter, robuster und autonomer machen
- **Primaeres Leitmotiv:** Superintelligenz im Programmieren ist die oberste Entwicklungsrichtung dieses Setups
- **Session-Prinzip:** Jede Session soll messbar zu mehr Programmierintelligenz, besserer Verifikation,
  besserem Gedaechtnis oder besseren Arbeitsablaeufen fuehren

- **[2026-03-22 11:20] self-improve**: Auf diesem macOS-Host ist PowerShell 7.6.0 jetzt lokal unter `/opt/homebrew/bin/pwsh` verfuegbar. Kuenftige Codex-Sessions koennen `.ps1`-Validatoren und PowerShell-Paritaetschecks direkt lokal ausfuehren.

- **[2026-03-22 11:54] self-improve**: Fuer schnelle QuizVerse-Android-Starts auf macOS gilt jetzt der feste Emulator-Pfad `/opt/homebrew/share/android-commandlinetools/emulator/emulator`, `adb` liegt unter `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`, das SDK unter `/Users/frank/Library/Android/sdk`, das Projekt unter `/Users/frank/Codex/QuizVerse`, und der bevorzugte AVD ist `Pixel7_API35`.

---

## Erkenntnisse aus Code Reviews
_Noch keine Eintraege._

## Erkenntnisse aus Tests
- **[2026-03-22 10:48] self-improve**: `bash codex-setup/scripts/validate-codex-setup.sh` bestaetigt die gehaertete Whiteboard-Aufloesung und die aktualisierte Workflow-/Skill-Struktur. Die PowerShell-Validierung konnte in dieser macOS-Runtime nicht gestartet werden, weil `pwsh` lokal nicht installiert ist.


- **[2026-03-22 11:13] self-improve**: `validate-codex-setup.sh` prueft jetzt zusaetzlich falsche `--memory`-Pfade, gueltige und ungueltige Directive-Tokens sowie eine echte Bridge-Mutation auf einer temporaeren Workspace-Kopie. Damit wird nicht nur Struktur, sondern auch das Direktiven-Handshake-Verhalten getestet.

- **[2026-03-22 11:20] self-improve**: `pwsh -NoProfile -File codex-setup/scripts/validate-codex-setup.ps1` und `pwsh -NoProfile -File codex-setup/scripts/check-openai-docs-mcp.ps1` laufen lokal auf macOS grün. Der Bash-Validator prueft damit ab jetzt auch die PowerShell-Seite automatisch mit.

- **[2026-03-22 12:00] self-improve**: `validate-codex-setup.sh` hat jetzt einen `grep`-Fallback, wenn `rg` fehlt. Damit bleibt der Unix-Validator lokal schnell, laeuft aber auch auf GitHub-Ubuntu-Runnern ohne `ripgrep` statt mit Exit 127 auszufallen.

## Architektur-Entscheidungen
- **[2026-03-22 10:48] self-improve**: Die Whiteboard-Bruecke arbeitet jetzt fail-closed. Whiteboard-Aufloesung ist nur noch fuer das autoritative Workspace-Ziel `<workspace>/codex-setup/agent-memory/shared/MEMORY.md` erlaubt; Wrapper uebergeben dafuer explizit `--workspace`, und der Validator deckt CWD-Regressionsfaelle ab.


- **[2026-03-22 11:13] self-improve**: Whiteboard-Schreibzugriffe sind jetzt an einen Directive-Token gebunden, der aus der aktuellen `## Oberste Direktive` berechnet wird. Dadurch koennen Wrapper nur schreiben, wenn sie die aktuelle Direktive unmittelbar vorher gelesen haben; stale oder fremde Write-Pfade werden abgewiesen.

## Debugging-Muster
_Noch keine Eintraege._

## Performance & Optimierung
_Noch keine Eintraege._

## UI/UX-Patterns
_Noch keine Eintraege._

## Forschung & Intelligence
- **[2026-03-22 10:48] self-improve**: Die offizielle OpenAI-Dokumentation beschreibt einen oeffentlichen Docs-MCP unter `https://developers.openai.com/mcp`; die lokale Codex-Runtime registriert jetzt zusaetzlich `openaiDeveloperDocs` neben `code-search`, um OpenAI-Fragen kuenftig direkt gegen die Herstellerdoku aufloesen zu koennen.


- **[2026-03-22 11:13] self-improve**: OpenAI dokumentiert offiziell, dass Codex `AGENTS.md` vor der Arbeit liest und dass fuer den Docs-MCP die Kombination aus `~/.codex/config.toml` plus AGENTS-Snippet der zuverlaessige Weg ist. Dieses Workspace-Root hat jetzt ein `AGENTS.md`, das sowohl die `Oberste Direktive` als auch die Nutzung von `openaiDeveloperDocs` fuer OpenAI-Fragen fest verankert.

## Regeln & Konventionen
- Codex arbeitet nur im `Codex`-Workspace.
- Codex nutzt niemals `proggs` als Arbeitsverzeichnis oder Whiteboard-Quelle.
- Das Codex-Whiteboard wird nur sektionsbasiert geschrieben, nie blind ans Dateiende.
- Repo-Quelle fuer Codex-Regeln und Codex-Skills ist `codex-setup/`.
- Lokale Deployments nach `~/.codex/` sind Runtime-Kopien, keine Git-Quelle.
- Das Whiteboard ist das zentrale Nervensystem; systemische Erkenntnisse gehoeren zuerst hierher.
- Die Sektion `## Oberste Direktive` wird von allen angebundenen Komponenten zuerst gelesen.
- `self-improve` ist der wichtigste Skill und muss Whiteboard, Agenten, MCP, Skills und Workflows auf das Ziel
  maximaler Programmierintelligenz ausrichten.
- Jede Session soll einen Intelligenzgewinn erzeugen. Bevorzugt werden Verbesserungen mit kumulativer
  oder exponentieller Hebelwirkung gegenueber rein kosmetischen Aenderungen.

- **[2026-03-22 11:31] self-improve**: Validierte `codex-setup/`-Aenderungen sollen kuenftig automatisch committed und nach `origin/main` gepusht werden. Wenn der Sync zusaetzlich `AGENTS.md` oder `.github/workflows/codex-setup-validate.yml` braucht, gehoeren sie in denselben plattformuebergreifenden Sync-Commit.

- **[2026-03-22 11:31] self-improve**: Die letzte Zeile der Abschlussantwort soll kuenftig den Git-Status eindeutig melden: `Committed.` wenn nur committed wurde, oder `Gepusht in <path>, plattformuebergreifend.` wenn der Codex-Setup-Sync auf GitHub liegt.
