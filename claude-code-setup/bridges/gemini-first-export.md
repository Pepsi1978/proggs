# Gemini CLI — Erster Export (BASELINE generieren)

> **Diesen Prompt beim ERSTEN Mal in Gemini CLI einfuegen wenn du exportieren willst.**
> Er scannt die gesamte Gemini-Umgebung und schreibt eine BASELINE ins mirror-ledger.md,
> damit Claude Code und andere CLIs die Gemini-Umgebung vollstaendig importieren koennen.

---

## Prompt (komplett kopieren und in Gemini CLI einfuegen)

```
Du bist der Export-Agent fuer Gemini CLI. Deine Aufgabe: Scanne die GESAMTE
Gemini-Umgebung und schreibe einen vollstaendigen BASELINE-Eintrag ins mirror-ledger.md.
Danach koennen andere CLIs (Claude Code, Codex) die Gemini-Umgebung importieren.

## Schritt 1: Repository pruefen und aktualisieren

cd ~/proggs && git pull --rebase

## Schritt 2: BASELINE-Check

Pruefe ob bereits ein Gemini-BASELINE im Ledger existiert:

grep "BASELINE-.*-GEM" ~/proggs/claude-code-setup/mirror-ledger.md

Wenn JA: Ueberspringe die BASELINE-Erstellung, gehe direkt zu Schritt 5 (inkrementeller Export).
Wenn NEIN: Weiter mit Schritt 3.

## Schritt 3: Gemini-Umgebung scannen

Erfasse ALLES was in dieser Gemini CLI Umgebung konfiguriert ist:

### 3a. Konfigurationsdateien
- Lies GEMINI.md (falls vorhanden): ~/proggs/GEMINI.md oder ~/.gemini/GEMINI.md
- Lies AGENTS.md (falls vorhanden): ~/proggs/AGENTS.md
- Lies alle .gemini/ Konfigurationen
- Lies ~/.gemini/settings.json (oder aequivalent)

### 3b. Installierte Tools und Extensions
- Welche MCP-Server sind konfiguriert? (Lies .gemini/settings.json oder .mcp.json)
- Welche Extensions/Plugins sind aktiv?
- Welche Sprach-Modelle sind konfiguriert?

### 3c. Eigene Regeln und Anpassungen
- Welche Regeln stehen in GEMINI.md?
- Welche Workflow-Anpassungen wurden gemacht?
- Welche Permissions sind konfiguriert?

### 3d. Dev-Tools
- Laufe diese Befehle und notiere die Ergebnisse:

node --version 2>/dev/null
bun --version 2>/dev/null
deno --version 2>/dev/null
python3 --version 2>/dev/null
go version 2>/dev/null
rustc --version 2>/dev/null
swift --version 2>/dev/null
dotnet --version 2>/dev/null
java -version 2>/dev/null
kotlin -version 2>/dev/null
git --version
gh --version 2>/dev/null

### 3e. Bereits importierte Claude-Code-Komponenten
- Welche Eintraege aus dem mirror-ledger sind fuer gemini als APPLIED markiert?
- Welche Agents/Rules/Hooks wurden bereits uebernommen?

## Schritt 4: BASELINE-Eintrag schreiben

Oeffne ~/proggs/claude-code-setup/mirror-ledger.md und haenge am Ende folgenden Eintrag an:

---

## [BASELINE-{HEUTE}-GEM] Vollstaendiges Gemini CLI Umgebungs-Inventar
<!-- SOURCE: gemini | PLATFORM: {macos oder windows} | TIMESTAMP: {ISO8601 jetzt} -->
<!-- TARGETS: macos/claude-code,windows/claude-code,codex -->
<!-- TYPE: baseline -->
<!-- AFFECTS: GESAMTE Gemini-Programmierumgebung -->

> Dies ist der BASELINE-Eintrag fuer Gemini CLI. Er beschreibt die komplette
> Gemini-Umgebung so detailliert, dass andere CLIs die Faehigkeiten importieren koennen.

### 1. Gemini CLI Version und Konfiguration
[Version, Modell, Settings — alles was du in Schritt 3 gefunden hast]

### 2. GEMINI.md / AGENTS.md Inhalt
[Vollstaendiger Inhalt aller Konfigurationsdateien]

### 3. MCP-Server und Extensions
[Alle konfigurierten MCP-Server mit Befehlen und Parametern]

### 4. Eigene Regeln und Anpassungen
[Alle Regeln die in GEMINI.md oder anderswo definiert sind]

### 5. Bereits importierte Claude-Code-Komponenten
[Liste aller APPLIED-Eintraege — damit andere CLIs wissen was Gemini schon hat]

### 6. Installierte Dev-Tools
[Ausgabe aller Versions-Checks aus Schritt 3d]

### 7. Gemini-spezifische Faehigkeiten
[Was kann Gemini was andere CLIs nicht koennen? Z.B. Google-Integration, Grounding, etc.]

### 8. Benutzer-Profil (falls abweichend von Claude-Code-BASELINE)
[Nur Abweichungen notieren — das Claude-Code-BASELINE hat das Hauptprofil]

### Portierung auf Claude Code
1. Lies diesen BASELINE um zu verstehen was Gemini kann
2. Gemini-spezifische MCP-Server/Extensions: pruefen ob Claude-Code-Aequivalente existieren
3. GEMINI.md Regeln: in CLAUDE.md oder rules/ uebernehmen wenn sinnvoll
4. Dev-Tools: pruefen ob Versionen kompatibel sind

### Portierung auf Codex
[Analog]

<!-- APPLIED: gemini={ISO8601 jetzt} -->
<!-- APPLIED: macos/claude-code=PENDING -->
<!-- APPLIED: windows/claude-code=PENDING -->
<!-- APPLIED: codex=PENDING -->

---

Danach:

cd ~/proggs
git add claude-code-setup/mirror-ledger.md
# Letzte Commit-Nummer ermitteln
LAST_NUM=$(git log --oneline -5 | head -1 | sed 's/.*#\([0-9]*\).*/\1/')
NEXT_NUM=$((LAST_NUM + 1))
git commit -m "#${NEXT_NUM} - gemini export: add Gemini CLI BASELINE to mirror-ledger"
git push

## Schritt 5: Inkrementeller Export (fuer JEDE weitere Session)

Wenn der BASELINE bereits existiert, schreibe NUR inkrementelle Eintraege
fuer Aenderungen die in DIESER Session gemacht wurden:

- Neue Regeln in GEMINI.md
- Neue MCP-Server oder Extensions
- Neue Workflow-Anpassungen
- Bugfixes oder Verbesserungen

Format: MIRROR-{DATUM}-GEM-{NNN}
Gleiche Struktur wie die Claude-Code-Eintraege im Ledger.

## Schritt 6: Bestaetigung

Zeige dem Benutzer:
- "Gemini BASELINE ins Ledger geschrieben"
- Anzahl der dokumentierten Komponenten
- Git-Commit-Nummer
- "Andere CLIs koennen jetzt die Gemini-Umgebung importieren"

## WICHTIG

- Schreibe NUR in ~/proggs/claude-code-setup/mirror-ledger.md
- Aendere NIEMALS Dateien in ~/.claude/ (das ist Claude Code's Workspace)
- Das Ledger ist die EINZIGE Austausch-Datei zwischen allen CLIs
- ID-Format fuer Gemini: BASELINE-YYYY-MM-DD-GEM oder MIRROR-YYYY-MM-DD-GEM-NNN
- APPLIED-Status: eigene Plattform = Timestamp, alle anderen = PENDING
- Nach dem Schreiben IMMER committen und pushen nach Pepsi1978/proggs
```
