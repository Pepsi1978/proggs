# 🛡️ DRITTE DIREKTIVE: Resilient Bugfixing & Proaktive Intelligenz-Steigerung (KRITISCH)

> **Diese Direktive ist die DRITTWICHTIGSTE im gesamten System — direkt nach Superintelligenz (#1)
> und Selbstbeobachtung (#2). Sie gilt fuer JEDEN Bugfix in der Programmierumgebung.**

## Prioritaets-Hierarchie des Gesamtsystems

| Rang | Direktive | Datei |
|------|-----------|-------|
| **#1** | Superintelligenz | `~/.claude/rules/superintelligence.md` |
| **#2** | Selbstbeobachtung | `~/.claude/rules/self-observation.md` |
| **#3** | **Resilient Bugfixing** | `~/.claude/rules/resilient-bugfixing.md` (diese Datei) |
| #4+ | Alle anderen Regeln | `~/.claude/rules/*` |

## Regel: Jeder Bug wird zum permanenten System-Upgrade

Wenn ein Fehler gefunden und gefixt wird, ist der Fix NICHT fertig, bis er **zukunftssicher** ist.
Ein Fehler darf in dieser Programmierumgebung **niemals zweimal auftreten** — egal in welcher
Session, auf welcher Plattform, nach welchem Update.

## Proaktive Intelligenz-Vorschlaege (PFLICHT in jeder Session)

Claude muss **waehrend der Arbeit** kontinuierlich nach Verbesserungen suchen und dem Benutzer
Vorschlaege machen. Nicht nur wenn Fehler auftreten, sondern IMMER:

### Wann und wo Vorschlaege machen?
- **Nur AM ENDE der Aufgabe** — NIEMALS mittendrin waehrend der Arbeit.
- Der Vorschlag kommt NACH der Status-Meldung ("Committed, gepusht und plattformuebergreifend.").
- **Nur wenn es einen ECHTEN Mehrwert gibt** — keinen Vorschlag erzwingen wenn nichts auffaellt.
- Kein Vorschlag ist besser als ein banaler Vorschlag.

### Wann ist ein Vorschlag sinnvoll?
- Zwischenfehler in der Session die spaeter gefixt wurden → Praevention vorschlagen
- Wiederkehrende manuelle Schritte → Automatisierung vorschlagen
- Fehlende Absicherung (keine Tests, kein Retry, kein Fallback) → Haertung vorschlagen
- Ineffizienzen (zu viele Versuche, zu lange Wartezeit) → Optimierung vorschlagen
- Neue Muster erkannt die das System schlauer machen koennten

### Format
Kurz, klar, am Ende der Antwort, maximal 2-3 Saetze:
```
💡 **Intelligenz-Vorschlag**: [Was verbessert werden kann]
   → [Konkreter Vorschlag] — Soll ich das umsetzen?
```

## Pflicht-Ablauf bei JEDEM Bugfix

### 1. Root Cause finden (nicht nur Symptom fixen)
- Den eigentlichen Ausloeser identifizieren, nicht nur das sichtbare Problem
- Frage: "Was ist die TIEFSTE Ursache?" — mindestens 3x "Warum?" fragen
- Beispiel: Hook-Fehler → Symptom ist der Fehler, Root Cause ist die Race Condition

### 2. Verwandte Fehlerquellen suchen (PFLICHT)
- **Gleiche Klasse**: Kann der gleiche Fehlertyp an anderer Stelle auftreten?
  (z.B. Race Condition bei claude-mem → gibt es andere Plugins mit dem gleichen Muster?)
- **Gleiche Komponente**: Kann die betroffene Komponente auf andere Weise versagen?
  (z.B. Port-Konflikt → was passiert bei Timeout, bei Crash, bei Update?)
- **Gleiche Abhaengigkeit**: Welche anderen Teile haengen von der gleichen Sache ab?
  (z.B. Worker-Service → wer braucht den noch? Was wenn er spaeter crasht?)

### 3. Zukunftssicheren Fix implementieren
Der Fix muss diese Eigenschaften haben:

| Eigenschaft | Bedeutung |
|-------------|-----------|
| **Self-Healing** | Repariert sich selbst nach Updates, Neustarts, Plattformwechsel |
| **Defensiv** | Faengt nicht nur den bekannten Fehler ab, sondern die ganze Fehlerklasse |
| **Ueberlebbar** | Uebersteht Plugin-Updates, Config-Aenderungen, System-Upgrades |
| **Erweiterbar** | Kann fuer zukuenftige aehnliche Faelle erweitert werden (z.B. Service Registry) |
| **Dokumentiert** | Memory-Eintrag erklaert Ursache, Fix und wie man neue Faelle hinzufuegt |
| **Schadensfrei** | Der Fix selbst darf KEINE neuen Fehler einfuehren (siehe Schritt 3b) |

### 3b. Fix-Induced-Failure-Pruefung (PFLICHT — VOR dem Commit)
**Ein Fix der neue Probleme verursacht ist SCHLIMMER als kein Fix.**
Vor JEDEM Commit eines Bugfixes MUSS geprueft werden:

| Pruefung | Frage | Beispiel |
|----------|-------|----------|
| **Abhaengigkeiten** | Was haengt vom geaenderten Code ab? | Launcher-Script aendern → launchd-plist testen |
| **Fehlszenarien** | Was passiert wenn der Fix-Code selbst fehlschlaegt? | Health-Check crasht → blockiert er die Session? (Muss exit 0 sein!) |
| **Zustandsaenderungen** | Aendert der Fix einen Systemzustand dauerhaft? | launchd-Agent → was wenn Plugin deinstalliert wird? (Crash-Loop!) |
| **Race Conditions** | Kann der Fix mit anderem Code kollidieren? | Guard + Plugin-Hook starten Worker → doppelter Start? |
| **Rueckwaertskompatibilitaet** | Bricht der Fix etwas das vorher funktionierte? | Neue Imports in index.ts → existieren die Module? |
| **Plattform-Effekte** | Funktioniert der Fix auf macOS UND Windows? | .sh-Hook → braucht es ein .ps1-Gegenstueck? |
| **Update-Resistenz** | Ueberlebt der Fix das naechste Plugin/CLI/OS-Update? | Hardcoded Pfade → dynamisch aufloesen |
| **Graceful Degradation** | Was wenn eine Voraussetzung fehlt? | Bun nicht installiert → auf Node.js zurueckfallen, nicht crashen |

**Faustregel**: Der Fix muss die Frage bestehen: "Was ist das Schlimmste das passieren kann
wenn ich diesen Fix deploye und dann 6 Monate lang nicht hinschaue?"

### 4. Mehrere Absicherungsschichten (Defense in Depth)
Nie nur EINE Absicherung. Immer mindestens 2-3 Schichten:
- Schicht 1: Praeventiv (Problem verhindern bevor es auftritt)
- Schicht 2: Reaktiv (Problem abfangen wenn es doch auftritt)
- Schicht 3: Selbstheilend (Fix automatisch wiederherstellen nach Updates)
- Schicht 4: Upstream (Bug beim Verursacher melden fuer permanenten Fix)

### 5. Poka-Yoke: Fehler durch Design unmoeglich machen

> **Poka-Yoke** (jap. ポカヨケ, "Fehlervermeidung") ist ein Prinzip aus dem Toyota-Produktionssystem:
> Gestalte den Prozess so, dass Fehler gar nicht erst entstehen koennen — statt sie nachtraeglich
> zu finden und zu reparieren. In dieser Programmierumgebung bedeutet das:

**Kernprinzip**: Wenn ein Fehler passiert ist, frage nicht nur "Wie fixe ich das?" sondern:
**"Wie gestalte ich den Prozess so um, dass dieser Fehler UNMOEGLICH wird?"**

#### Die 3 Poka-Yoke-Stufen (von schwach zu stark)

| Stufe | Name | Beschreibung | Beispiel |
|-------|------|-------------|----------|
| **1** | **Warnung** | System warnt wenn ein Fehler droht, laesst ihn aber zu | `hook-exit0-guard` warnt bei fehlendem `exit 0` im Commit |
| **2** | **Erzwingung** | System verhindert den Fehler aktiv | Pre-Push-Hook rejected Push ohne vorheriges `fetch+rebase` |
| **3** | **Eliminierung** | Fehler kann konzeptionell nicht mehr auftreten | Hook-Template hat `exit 0` eingebaut — man muesste es aktiv ENTFERNEN um den Fehler zu machen |

**Ziel**: So viele Fehlerquellen wie moeglich auf **Stufe 3 (Eliminierung)** bringen.
Stufe 1 (Warnung) ist besser als nichts, aber Stufe 3 ist das Ideal.

#### Wann Poka-Yoke anwenden (PFLICHT)

Bei JEDEM Bugfix pruefen:
1. **Kann ich den Fehler durch ein Template eliminieren?** (Stufe 3)
   - Beispiel: Hook-Template mit eingebautem `exit 0` → Fehler kann nicht mehr passieren
   - Beispiel: JSON-Validierung direkt im Write-Workflow → kaputtes JSON unmoeglich
2. **Kann ich den Fehler durch einen Guard erzwingen?** (Stufe 2)
   - Beispiel: Pre-Push-Hook erzwingt `fetch+rebase` → Push-Rejection unmoeglich
   - Beispiel: Config-Guard blockiert Aenderungen an `defaultMode` → Permission-Reset unmoeglich
3. **Kann ich zumindest warnen?** (Stufe 1)
   - Beispiel: Disk-Guard warnt bei <5GB Speicher → Speicherueberlauf frueh erkannt
   - Beispiel: Session-Guard warnt bei falscher Permission → schnelle Korrektur

#### Bestehende Poka-Yoke-Mechanismen im System

| Mechanismus | Stufe | Was er verhindert |
|-------------|-------|------------------|
| `hook-forge` Skill (Templates) | 3 — Eliminierung | Hooks ohne exit 0, ohne try/catch, ohne Logging |
| `hook-exit0-guard` (Pre-Commit) | 1 — Warnung | Vergessenes exit 0 bei Hook-Commits |
| `poka-yoke-git-push` (Pre-Push) | 2 — Erzwingung | Push ohne fetch+rebase |
| `config-guard` (PostToolUse) | 2 — Erzwingung | Aenderungen an bypassPermissions |
| `session-guard` (SessionStart) | 3 — Eliminierung | Falsche Permission-Einstellungen (repariert automatisch) |
| `safety-gate` (PreToolUse) | 2 — Erzwingung | Destruktive Befehle (rm -rf, DROP TABLE) |
| `redact-secrets` (PreToolUse) | 2 — Erzwingung | Secrets in Tool-Ausgaben |
| Hook-Templates (ps1/sh) | 3 — Eliminierung | Fehlerhafte Hook-Grundstruktur |
| Python-Batch statt Agents (Regel) | 3 — Eliminierung | Inkonsistente Multi-Datei-Edits |
| 3-Dateien-Regel (Settings) | 1 — Warnung | Vergessene Settings-Synchronisation |

#### Poka-Yoke bei neuen Features (nicht nur bei Bugfixes)

Auch bei NEUEN Features pruefen: "Welche Fehler koennte ein Benutzer oder Agent machen?"
und praventive Mechanismen einbauen BEVOR der erste Fehler passiert. Das ist der Unterschied
zwischen reaktivem Bugfixing (Fehler passiert → Fix) und proaktivem Poka-Yoke (Fehler kann
gar nicht erst passieren).

### 6. Memory speichern
- Feedback-Memory mit: Was war der Fehler, was war die Root Cause, was ist der Fix
- Muster-Erkennung: "Wenn ich in Zukunft [Muster X] sehe, muss ich [Check Y] machen"
- **Poka-Yoke-Check**: Wurde der Prozess so umgestaltet dass der Fehler UNMOEGLICH wird? (Stufe 3 anstreben)

## Beispiel: claude-mem Hook-Fehler (2026-03-22)

| Schritt | Was gemacht wurde |
|---------|-------------------|
| Root Cause | Race Condition: MCP-Server und Hook starten Worker gleichzeitig |
| Verwandte Fehler | Alle Plugins mit Daemon/Worker/Server-Start geprueft |
| Self-Healing | Auto-Patcher repariert hooks.json nach jedem Plugin-Update |
| Defensiv | Service Registry fuer ALLE zukuenftigen Service-Plugins |
| Mehrere Schichten | Guard-Hook + Auto-Patcher + Plugin-Patch + Upstream-Issue |
| Memory | Feedback-Memory mit Muster-Erkennung gespeichert |

## Beispiel: Fix-Induced-Failure vermieden (2026-03-22)

| Pruefung | Problem erkannt | Massnahme |
|----------|----------------|-----------|
| Zustandsaenderung | launchd-Agent wuerde bei Plugin-Deinstallation Crash-Loop ausloesen | Launcher wartet geduldig statt exit 1, schlaeft 5min vor Retry |
| Graceful Degradation | Bun koennte fehlen oder Pfad sich aendern | Multi-Path-Suche + Fallback auf Node.js |
| Fehlszenario | Health-Check-Hook koennte crashen und Session blockieren | Immer exit 0, set +e, Fallback-Logger |
| Race Condition | Worker bereits laufend → doppelter Start | Pre-Flight-Check via Health-Endpoint |
| Rueckwaertskompatibilitaet | Neue Module (db-state.ts) muessen committed sein | Sofort ins Repo committed, nie nur lokal |

## Plattform-spezifische Bugfix-Regeln (Windows vs macOS)

### Windows: UTF-8 Encoding ist PFLICHT
Python auf Windows verwendet standardmaessig `cp1252` als Dateikodierung. Jede Datei
die Unicode-Zeichen enthalten koennte (Emojis, Umlaute, Sonderzeichen) MUSS mit
explizitem `encoding='utf-8'` geoeffnet werden:
```python
# FALSCH — crasht auf Windows bei Emojis/Sonderzeichen:
with open(path, 'w') as f:
    json.dump(data, f)

# RICHTIG — funktioniert auf Windows UND macOS:
with open(path, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False)
```
**Gilt fuer**: Alle Python-Skripte, Hooks, Build-Skripte und Einmal-Befehle die Dateien
schreiben oder lesen. Ohne `encoding='utf-8'` schlaegt das Schreiben von JSON mit
Emojis/Unicode fehl (UnicodeEncodeError: 'charmap' codec).

### Atomares Schreiben (Write-to-Temp-then-Rename)
Beim Schreiben von kritischen Konfigurationsdateien (settings.json, MEMORY.md, etc.)
MUSS das atomare Muster verwendet werden. Sonst bleibt bei einem Crash eine abgeschnittene
Datei zurueck die nicht mehr gelesen werden kann:
```python
import tempfile, os, json

def safe_json_write(path, data):
    """Atomic write: temp file → rename. Never corrupts the original."""
    dir_name = os.path.dirname(path)
    with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp',
                                      delete=False, encoding='utf-8') as tmp:
        json.dump(data, tmp, indent=2, ensure_ascii=False)
        tmp.write('\n')
        tmp_path = tmp.name
    # Atomic replace (on Windows: os.replace handles this)
    os.replace(tmp_path, path)
```
**Wann**: Bei JEDER Datei die bei Beschaedigung das System blockiert (settings.json,
MEMORY.md, session-scores.jsonl, etc.)
**Warum**: In dieser Session ist genau das passiert — Python-Crash beim Schreiben hat
settings.json abgeschnitten und JSON unlesbar gemacht.

### Windows vs macOS Unterschiede (Referenz)

| Aspekt | macOS | Windows |
|--------|-------|---------|
| Default File Encoding | UTF-8 | cp1252 (ANSI) — IMMER `encoding='utf-8'` |
| Shell | zsh/bash nativ | Git Bash (bash-Emulation), pwsh fuer Hooks |
| Hook-Ausfuehrung | `bash ~/.claude/hooks/*.sh` | `pwsh -File "$USERPROFILE/.claude/hooks/*.ps1"` |
| Temp-Verzeichnis | `/tmp/` | `$env:TEMP/` (= `%LOCALAPPDATA%\Temp`) |
| Home-Variable | `$HOME` oder `~` | `$USERPROFILE` oder `$HOME` (Git Bash) |
| Pfad-Trenner | `/` (Forward Slash) | `\` nativ, aber `/` funktioniert in Git Bash |
| Line Endings | LF (`\n`) | CRLF (`\r\n`) — Git autocrlf=true |
| Bun-Pfad | `/opt/homebrew/bin/bun` | `$USERPROFILE/.bun/bin/bun.exe` |
| npx-Pfad | `/opt/homebrew/bin/npx` | npx via npm im PATH |
| Executable-Endung | keine | `.exe` |
| Symlinks | Native Unterstuetzung | Braucht Developer Mode oder Admin-Rechte |
| `grep -P` (PCRE) | Funktioniert (UTF-8 Locale) | CRASHT! `grep -P supports only unibyte and UTF-8 locales` — immer `awk`/`sed` verwenden |
| Default Locale | `en_US.UTF-8` | `German_Germany.1252` (kein UTF-8!) |

## Was NIEMALS passieren darf
- ❌ Nur das Symptom fixen ohne Root Cause zu verstehen
- ❌ Fix der bei naechstem Plugin-Update oder Neustart kaputt geht
- ❌ Fix nur fuer den einen konkreten Fall, ohne aehnliche Faelle zu pruefen
- ❌ Fix ohne Memory-Eintrag (Wissen geht verloren zwischen Sessions)
- ❌ "Funktioniert jetzt" sagen ohne zu pruefen ob es in 2 Wochen noch funktioniert
- ❌ Zwischenfehler in der Session ignorieren nur weil sie "danach" gefixt wurden
- ❌ Session beenden ohne Rueckblick auf Verbesserungsmoeglichkeiten
- ❌ Stumm arbeiten ohne proaktive Vorschlaege zur Intelligenz-Steigerung
- ❌ Fix deployen der selbst neue Fehler einfuehrt (Fix-Induced Failure)
- ❌ Fix ohne die 8-Punkte-Pruefung aus Schritt 3b durchzugehen
- ❌ Einen Fehler ein ZWEITES Mal machen — jeder Fehler wird genau EINMAL gemacht
