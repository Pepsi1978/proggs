# Almanach-Trigger-Sonde + Auswertung — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eine rein beobachtende Sonde im `bug-almanac-guard` zeichnet jede Almanach-/Best-Practices-Unterbrechung UND jede Freigabe als JSON-Zeile auf; ein neuer Skill wertet diese Datei aus und schlaegt unnoetige Auslöser zum Ausschluss vor.

**Architecture:** Additive Erweiterung beider Guard-Varianten (`.ps1` + `.sh`) um eine zentrale Schreibfunktion, die an den bestehenden Block-/Pass-Stellen aufgerufen wird (genau dort, wo heute schon `bug-almanac-blocks.log` geschrieben wird). Die Block-Entscheidung bleibt unveraendert (Funktionserhalt, FAIL-OPEN). Auswertung getrennt als deutscher Skill (via skill-creator).

**Tech Stack:** PowerShell 7 (`.ps1`), Bash + Python3 (`.sh`), JSON-Lines, Claude-Skill (Markdown).

---

## File Structure

| Datei | Rolle | Aktion |
|-------|-------|--------|
| `~/.claude/hooks/bug-almanac-guard.ps1` | Live-Guard Windows — Sonde additiv | Modify |
| `~/.claude/hooks/bug-almanac-guard.sh` | Live-Guard macOS/Linux — Sonde additiv | Modify |
| `~/proggs/claude-code-setup/hooks/bug-almanac-guard.ps1` | Repo-Spiegelung (Commit) | Modify (1:1 Kopie) |
| `~/proggs/claude-code-setup/hooks/bug-almanac-guard.sh` | Repo-Spiegelung (Commit) | Modify (1:1 Kopie) |
| `~/.claude/state/bug-almanac-triggers.jsonl` | Ausgabe-Datei der Sonde (NICHT im Repo, ausserhalb) | wird zur Laufzeit erzeugt |
| `~/.claude/skills/almanach-trigger-auswertung/SKILL.md` | Auswertungs-Skill (+ Repo-Spiegelung) | Create (via skill-creator) |
| `~/proggs/docs/superpowers/specs/2026-06-16-almanach-trigger-sonde-design.md` | Freigegebene Spec | bereits vorhanden |

**Wichtig (Git-Regel):** `git add` nur unter `~/proggs/`. Die Live-Hooks in `~/.claude/hooks/` werden editiert (damit sie wirken), aber **committet** wird die Repo-Spiegelung unter `claude-code-setup/hooks/`. Ebenso der Skill (Live unter `~/.claude/skills/`, Spiegelung im Repo).

---

## Task 0: Pflicht-Lektuere (Hochrisiko-Bereich `claudehooks`, Stufe C)

Der Guard wird jeden Edit an `~/.claude/hooks/*.ps1|*.sh` blockieren, bis der claude-hooks-Almanach im VOLLTEXT + die Best-Practices gelesen sind. Das ist Pflicht (`known-bugs-before-coding`) und muss VOR Task 1 erledigt sein.

- [ ] **Step 1: Almanach-Volltext lesen (Stufe C)**

Read (OHNE limit): `~/proggs/bugs/claude-tooling/claude-hooks.md`
Zweck: bekannte Hook-Fallen (exit-2-blockt-nicht, Dot-Source-exit, JSON-Tiefe, `jq`-Verbot in `.sh`, Matcher-Cache) vor dem Edit kennen.

- [ ] **Step 2: Best-Practices-Kurzcheck lesen (Stufe A)**

Read (limit=80): `~/proggs/best-practices/claude-tooling/claude-hooks.md`

- [ ] **Step 3: Kein Commit**

Reine Lektuere — nichts zu committen. Marker werden vom Guard automatisch gesetzt und geben die Hook-Edits frei.

---

## Task 1: Sonde in die PowerShell-Variante einbauen

**Files:**
- Modify: `~/.claude/hooks/bug-almanac-guard.ps1`

- [ ] **Step 1: Baseline festhalten (Regressions-Referenz)**

Run:
```bash
test -f "$HOME/.claude/state/bug-almanac-triggers.jsonl" && echo "EXISTIERT (unerwartet)" || echo "OK: noch keine triggers.jsonl"
```
Expected: `OK: noch keine triggers.jsonl`

Damit ist klar: jede spaeter erzeugte Zeile stammt aus unserer neuen Sonde.

- [ ] **Step 2: Schreibfunktion `Add-AlmanacTrigger` definieren**

Direkt nach `. "$PSScriptRoot/hook-log.ps1"` und `$ErrorActionPreference = "Stop"` (also vor dem grossen `try {`) diese Funktion einfuegen. Sie liest `$data`/`$tool`/`$fp` aus dem Hook-Scope (in PowerShell lesend sichtbar), faengt JEDEN Fehler ab (FAIL-OPEN) und beeinflusst nie die Entscheidung:

```powershell
function Add-AlmanacTrigger {
    param(
        [string]$EventType,
        [string]$BlockType,
        [string]$Slug,
        [string]$Area,
        [bool]$HighRisk
    )
    try {
        $stateDir = Join-Path $env:USERPROFILE ".claude/state"
        if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force -ErrorAction SilentlyContinue | Out-Null }
        $jsonl = Join-Path $stateDir "bug-almanac-triggers.jsonl"
        # Rotation: bei >5 MB einmalig nach .1 wegrollen (genau eine Vorgaengerdatei).
        try { if ((Test-Path $jsonl) -and ((Get-Item $jsonl).Length -gt 5MB)) { Move-Item -Path $jsonl -Destination "$jsonl.1" -Force -ErrorAction SilentlyContinue } } catch {}
        # change_excerpt aus dem Tool-Input (content / new_string / edits[].new_string), ~300 Zeichen.
        $excerpt = ""
        try {
            $ti = $data.tool_input
            if ($ti.content)        { $excerpt = [string]$ti.content }
            elseif ($ti.new_string) { $excerpt = [string]$ti.new_string }
            elseif ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $excerpt += [string]$e.new_string + "`n" } } }
        } catch {}
        if ($excerpt.Length -gt 300) { $excerpt = $excerpt.Substring(0, 300) }
        # Secret-Maskierung (observability-first §8).
        $excerpt = $excerpt -replace 'gho_[A-Za-z0-9]{20,}','[REDACTED]' `
                            -replace 'ghp_[A-Za-z0-9]{20,}','[REDACTED]' `
                            -replace 'sk-[A-Za-z0-9]{20,}','[REDACTED]' `
                            -replace 'AIza[A-Za-z0-9_\-]{20,}','[REDACTED]'
        $excerpt = [regex]::Replace($excerpt, '(?i)(token|key|secret|password)(["'']?\s*[:=]\s*["''])[^"'']+', '$1$2[REDACTED]')
        $sid = ""
        try { $sid = [string]$data.session_id } catch {}
        $obj = [ordered]@{
            ts             = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")
            event          = $EventType
            block_type     = $BlockType
            slug           = $Slug
            area           = $Area
            tool           = $tool
            file           = $fp
            change_excerpt = $excerpt
            high_risk      = $HighRisk
            session        = $sid
        }
        Add-Content -Path $jsonl -Value ($obj | ConvertTo-Json -Compress -Depth 5) -Encoding UTF8 -ErrorAction SilentlyContinue
    } catch {}
}
```

- [ ] **Step 3: Sonden-Aufrufe an die 6 Entscheidungsstellen setzen**

Jeweils UNMITTELBAR nach der bestehenden `Add-Content ... bug-almanac-blocks.log`-Zeile bzw. innerhalb des `seenMarker`-Blocks. Die bestehende Block-/Pass-Logik bleibt Byte-fuer-Byte erhalten — wir fuegen nur je eine Zeile hinzu:

| Stelle (bestehender Anker) | Neuer Aufruf |
|----------------------------|--------------|
| Block „Almanach ungelesen" (nach `... blocks.log` mit `$slug`) | `Add-AlmanacTrigger -EventType "block" -BlockType "almanach-ungelesen" -Slug $slug -Area $name -HighRisk $isHighRisk` |
| Block „Stufe-C-Volltext" (nach `... (stufe-c-volltext)`) | `Add-AlmanacTrigger -EventType "block" -BlockType "volltext-c" -Slug $slug -Area $name -HighRisk $true` |
| Block „Best-Practices" (nach `... (best-practices)`) | `Add-AlmanacTrigger -EventType "block" -BlockType "best-practices" -Slug $slug -Area $name -HighRisk $isHighRisk` |
| Pass „Almanach gelesen" (im `if (-not (Test-Path $seenMarker))`-Block bei `$almanachExists`) | `Add-AlmanacTrigger -EventType "pass" -BlockType ($(if ($disabled) {"disabled"} else {"already-read"})) -Slug $slug -Area $name -HighRisk $isHighRisk` |
| Pass „kein Almanach, aber frei (ack/disabled)" (im `if (-not (Test-Path $seenMarker))`-Block nach `$ackMarker`) | `Add-AlmanacTrigger -EventType "pass" -BlockType ($(if ($disabled) {"disabled"} else {"ack"})) -Slug $slug -Area $name -HighRisk $false` |
| Block „kein Almanach" (nach `... (kein-almanach)`) | `Add-AlmanacTrigger -EventType "block" -BlockType "kein-almanach" -Slug $slug -Area $name -HighRisk $false` |

Hinweis: `$isHighRisk` wird im Guard erst ab dem Almanach-Block-Bereich gesetzt. Bei „kein Almanach" gibt es keinen `$isHighRisk` im Scope → dort fest `$false` (wie in der Tabelle).

- [ ] **Step 4: Syntax-Pruefung**

Run:
```bash
pwsh -NoProfile -Command "\$null = [System.Management.Automation.Language.Parser]::ParseFile('$HOME/.claude/hooks/bug-almanac-guard.ps1', [ref]\$null, [ref]\$null); Write-Output 'PARSE-OK'"
```
Expected: `PARSE-OK` (kein Parser-Fehler).

- [ ] **Step 5: Smoke-Test BLOCK — Entscheidung unveraendert + JSONL-Zeile**

Vorbereiten: Marker fuer Bereich `gradle` entfernen (frischer Block-Zustand) und Ausgabe leeren:
```bash
rm -f "$TEMP/bug-almanac-read-gradle.flag" "$TEMP/bug-almanac-seen-gradle.flag" "$HOME/.claude/state/bug-almanac-triggers.jsonl" 2>/dev/null
TMP="${TEMP:-$LOCALAPPDATA/Temp}"; rm -f "$TMP"/bug-almanac-read-gradle.flag "$TMP"/bug-almanac-seen-gradle.flag 2>/dev/null
```
Hook fuettern (simulierter Edit an build.gradle.kts mit Version-Bump):
```bash
echo '{"tool_name":"Edit","tool_input":{"file_path":"C:/Users/barwa/proggs/app/build.gradle.kts","old_string":"versionName = \"0.11.0\"","new_string":"versionName = \"0.11.1\""},"session_id":"smoke1"}' | pwsh -NoProfile -File "$HOME/.claude/hooks/bug-almanac-guard.ps1"
```
Expected (stdout): JSON mit `"permissionDecision":"deny"` und „Bug-Almanach-Pflicht" — also **identisches Block-Verhalten wie vorher**.

JSONL pruefen:
```bash
cat "$HOME/.claude/state/bug-almanac-triggers.jsonl"
```
Expected: genau **eine** Zeile, gueltiges JSON, mit `"event":"block"`, `"block_type":"almanach-ungelesen"`, `"slug":"gradle"`, `"tool":"Edit"`, `"change_excerpt"` enthaelt `versionName = "0.11.1"`, `"session":"smoke1"`.

- [ ] **Step 6: Smoke-Test PASS — nur eine Zeile pro Bereich+Session**

Read-Marker setzen (Almanach „gelesen") + BP-Marker (damit kein BP-Block) und seen-Marker entfernen:
```bash
TMP="${TEMP:-$LOCALAPPDATA/Temp}"; touch "$TMP/bug-almanac-read-gradle.flag" "$TMP/bug-almanac-bp-read-gradle.flag"; rm -f "$TMP/bug-almanac-seen-gradle.flag"
```
Zwei Edits hintereinander:
```bash
for i in 1 2; do echo '{"tool_name":"Edit","tool_input":{"file_path":"C:/Users/barwa/proggs/app/build.gradle.kts","new_string":"x"},"session_id":"smoke1"}' | pwsh -NoProfile -File "$HOME/.claude/hooks/bug-almanac-guard.ps1" >/dev/null; done
grep -c '"event":"pass"' "$HOME/.claude/state/bug-almanac-triggers.jsonl"
```
Expected: `1` — genau eine `pass`-Zeile (zweiter Edit erzeugt keine, weil seenMarker schon gesetzt).

- [ ] **Step 7: Smoke-Test Secret-Maskierung**

```bash
TMP="${TEMP:-$LOCALAPPDATA/Temp}"; rm -f "$TMP/bug-almanac-read-gradle.flag" "$TMP/bug-almanac-seen-gradle.flag" "$HOME/.claude/state/bug-almanac-triggers.jsonl"
echo '{"tool_name":"Write","tool_input":{"file_path":"C:/Users/barwa/proggs/app/build.gradle.kts","content":"val k = \"ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZ012345\""},"session_id":"smoke2"}' | pwsh -NoProfile -File "$HOME/.claude/hooks/bug-almanac-guard.ps1" >/dev/null
grep -q 'REDACTED' "$HOME/.claude/state/bug-almanac-triggers.jsonl" && grep -q 'ghp_ABCDEF' "$HOME/.claude/state/bug-almanac-triggers.jsonl" && echo "LEAK!" || echo "OK: maskiert"
```
Expected: `OK: maskiert`

- [ ] **Step 8: Aufraeumen (Testdaten verwerfen)**

```bash
rm -f "$HOME/.claude/state/bug-almanac-triggers.jsonl"; TMP="${TEMP:-$LOCALAPPDATA/Temp}"; rm -f "$TMP/bug-almanac-seen-gradle.flag" "$TMP/bug-almanac-read-gradle.flag" "$TMP/bug-almanac-bp-read-gradle.flag"
echo "bereinigt"
```

- [ ] **Step 9: Spiegeln + Commit**

```bash
cp "$HOME/.claude/hooks/bug-almanac-guard.ps1" "$HOME/proggs/claude-code-setup/hooks/bug-almanac-guard.ps1"
cd "$HOME/proggs"
git add claude-code-setup/hooks/bug-almanac-guard.ps1
git commit -m "#NNN - Almanach-Trigger-Sonde: JSON-Lines-Logging in guard.ps1 (Direktive 2)" -- claude-code-setup/hooks/bug-almanac-guard.ps1
git fetch origin && git rebase origin/main && git push
```
(NNN = naechste fortlaufende Nummer.)

---

## Task 2: Sonde in die Bash-Variante spiegeln (schema-gleich)

**Files:**
- Modify: `~/.claude/hooks/bug-almanac-guard.sh`

- [ ] **Step 1: Schreibfunktion `add_almanac_trigger` definieren**

Direkt nach `. "$SCRIPT_DIR/hook-log.sh"` und dem `trap`-Setup einfuegen. JSON-Schreiben per Python3 (kein `jq` — claude-hooks.md). `$input` wird ueber eine Umgebungsvariable an Python gegeben (Heredoc belegt stdin), die kleinen Parameter ueber argv. `|| true` haelt FAIL-OPEN trotz `set -e`:

```bash
add_almanac_trigger() {
    # $1=event $2=block_type $3=slug $4=area $5=high_risk(0/1)
    stateDir="$HOME/.claude/state"
    mkdir -p "$stateDir" 2>/dev/null || true
    jsonl="$stateDir/bug-almanac-triggers.jsonl"
    if [ -f "$jsonl" ]; then
        sz=$(wc -c < "$jsonl" 2>/dev/null || echo 0)
        [ "${sz:-0}" -gt 5242880 ] && mv -f "$jsonl" "$jsonl.1" 2>/dev/null || true
    fi
    ALM_INPUT="$input" ALM_TOOL="$tool" ALM_FP="$fp" ALM_JSONL="$jsonl" \
    python3 - "$1" "$2" "$3" "$4" "$5" <<'PYEOF' 2>/dev/null || true
import json, sys, re, datetime, os
ev, bt, slug, area, hr = sys.argv[1:6]
try:
    d = json.loads(os.environ.get('ALM_INPUT', '') or '{}')
except Exception:
    d = {}
ti = d.get('tool_input') or {}
ex = ti.get('content') or ti.get('new_string') or ''
if not ex and ti.get('edits'):
    ex = '\n'.join((e.get('new_string') or '') for e in ti['edits'])
ex = ex[:300]
for p in [r'gho_[A-Za-z0-9]{20,}', r'ghp_[A-Za-z0-9]{20,}', r'sk-[A-Za-z0-9]{20,}', r'AIza[A-Za-z0-9_\-]{20,}']:
    ex = re.sub(p, '[REDACTED]', ex)
ex = re.sub(r'''(?i)(token|key|secret|password)(["']?\s*[:=]\s*["'])[^"']+''', r'\1\2[REDACTED]', ex)
o = {
    'ts': datetime.datetime.now().strftime('%Y-%m-%dT%H:%M:%S'),
    'event': ev, 'block_type': bt, 'slug': slug, 'area': area,
    'tool': os.environ.get('ALM_TOOL', ''), 'file': os.environ.get('ALM_FP', ''),
    'change_excerpt': ex, 'high_risk': (hr == '1'),
    'session': d.get('session_id', '') or '',
}
try:
    with open(os.environ['ALM_JSONL'], 'a', encoding='utf-8') as f:
        f.write(json.dumps(o, ensure_ascii=False) + '\n')
except Exception:
    pass
PYEOF
}
```

- [ ] **Step 2: Sonden-Aufrufe an dieselben 6 Stellen setzen**

Jeweils unmittelbar nach der bestehenden `echo "... >> "$stateDir/bug-almanac-blocks.log"`-Zeile bzw. im `seenMarker`-Block. `$isHighRisk` ist im `.sh` `0/1`:

| Stelle | Neuer Aufruf |
|--------|--------------|
| Block „Almanach ungelesen" | `add_almanac_trigger block almanach-ungelesen "$slug" "$name" "$isHighRisk"` |
| Block „stufe-c-volltext" | `add_almanac_trigger block volltext-c "$slug" "$name" 1` |
| Block „best-practices" | `add_almanac_trigger block best-practices "$slug" "$name" "$isHighRisk"` |
| Pass „Almanach gelesen" (im `if [ ! -f "$seenMarker" ]` bei `$almanachPath`) | `bt="already-read"; [ "$disabled" -eq 1 ] && bt="disabled"; add_almanac_trigger pass "$bt" "$slug" "$name" "$isHighRisk"` |
| Pass „kein Almanach, frei (ack/disabled)" (im `if [ ! -f "$seenMarker" ]` nach `$ackMarker`) | `bt="ack"; [ "$disabled" -eq 1 ] && bt="disabled"; add_almanac_trigger pass "$bt" "$slug" "$name" 0` |
| Block „kein-almanach" | `add_almanac_trigger block kein-almanach "$slug" "$name" 0` |

- [ ] **Step 3: Syntax-Pruefung + shellcheck**

Run:
```bash
bash -n "$HOME/.claude/hooks/bug-almanac-guard.sh" && echo "SYNTAX-OK"
shellcheck -S error "$HOME/.claude/hooks/bug-almanac-guard.sh" && echo "SHELLCHECK-OK"
```
Expected: `SYNTAX-OK` und `SHELLCHECK-OK` (keine Errors; Style-Warnungen tolerierbar).

- [ ] **Step 4: Smoke-Test (Git Bash)**

```bash
TMP="${TMPDIR:-/tmp}"; rm -f "$TMP"/bug-almanac-read-gradle.flag "$TMP"/bug-almanac-seen-gradle.flag "$HOME/.claude/state/bug-almanac-triggers.jsonl"
echo '{"tool_name":"Edit","tool_input":{"file_path":"/c/Users/barwa/proggs/app/build.gradle.kts","new_string":"versionCode = 42"},"session_id":"shsmoke"}' | bash "$HOME/.claude/hooks/bug-almanac-guard.sh"
echo "--- jsonl ---"; cat "$HOME/.claude/state/bug-almanac-triggers.jsonl"
```
Expected: stdout = `deny`-JSON (Block unveraendert); jsonl = eine Zeile `"event":"block"`, `"block_type":"almanach-ungelesen"`, `"slug":"gradle"`, `change_excerpt` enthaelt `versionCode = 42`.

- [ ] **Step 5: Schema-Gleichheit .ps1 vs .sh pruefen**

Run:
```bash
python3 - <<'PY'
import json
p="C:/Users/barwa/.claude/state/bug-almanac-triggers.jsonl".replace("C:/","/c/")
keys=set()
for line in open(p,encoding="utf-8"):
    line=line.strip()
    if line: keys.add(tuple(sorted(json.loads(line).keys())))
print("SCHEMATA:", keys)
assert len(keys)==1, "Schema weicht ab!"
print("OK: identisches Schema")
PY
```
Expected: ein einziges Schema-Tupel, `OK: identisches Schema`. (Datei enthaelt jetzt sowohl die `.sh`- als auch ggf. `.ps1`-Zeile aus den Tests.)

- [ ] **Step 6: Aufraeumen + Spiegeln + Commit**

```bash
rm -f "$HOME/.claude/state/bug-almanac-triggers.jsonl"; TMP="${TMPDIR:-/tmp}"; rm -f "$TMP"/bug-almanac-seen-gradle.flag "$TMP"/bug-almanac-read-gradle.flag
cp "$HOME/.claude/hooks/bug-almanac-guard.sh" "$HOME/proggs/claude-code-setup/hooks/bug-almanac-guard.sh"
cd "$HOME/proggs"
git add claude-code-setup/hooks/bug-almanac-guard.sh
git commit -m "#NNN - Almanach-Trigger-Sonde: JSON-Lines-Logging in guard.sh (Cross-Platform)" -- claude-code-setup/hooks/bug-almanac-guard.sh
git fetch origin && git rebase origin/main && git push
```

---

## Task 3: Regressionscheck (Funktionserhalt, Direktive #3)

**Files:** keine — reine Verifikation.

- [ ] **Step 1: Alle 4 Block-Faelle + 2 Pass-Faelle verhalten sich wie vorher**

Fuer jeden Fall: Marker passend setzen, Hook fuettern, pruefen dass die ERWARTETE Entscheidung kommt (deny bei Blocks, additionalContext/leer bei Pass). Tabelle der erwarteten stdout-Inhalte:

| Fall | Marker-Setup | Erwartete Entscheidung |
|------|--------------|------------------------|
| almanach-ungelesen | read-Marker weg | `deny` + „Bug-Almanach-Pflicht" |
| volltext-c (z.B. `r8`) | read-Marker da, full-Marker weg | `deny` + „Volltext-Pflicht" |
| best-practices | read-Marker da, bp-Marker weg | `deny` + „Best-Practices-Pflicht" |
| kein-almanach (z.B. `.go`-Datei) | ack-Marker weg | `deny` + „es gibt noch KEINEN Almanach" |
| pass already-read | read+bp-Marker da, seen weg | leer ODER `additionalContext` „freigegeben" |
| pass disabled | `bug-almanac-disable.flag` da | `additionalContext` „Notaus aktiv" |

- [ ] **Step 2: FAIL-OPEN bei kaputtem State**

State-Verzeichnis kurzzeitig unschreibbar simulieren ist heikel; stattdessen pruefen: Hook mit leerem/kaputtem stdin → exit 0, kein Crash:
```bash
echo '' | bash "$HOME/.claude/hooks/bug-almanac-guard.sh"; echo "exit=$?"
echo 'kaputt{' | bash "$HOME/.claude/hooks/bug-almanac-guard.sh"; echo "exit=$?"
```
Expected: beide `exit=0`, keine Fehlerausgabe auf stderr.

- [ ] **Step 3: Aufraeumen**

```bash
rm -f "$HOME/.claude/state/bug-almanac-triggers.jsonl"
TMP="${TMPDIR:-/tmp}"; rm -f "$TMP"/bug-almanac-disable.flag "$TMP"/bug-almanac-seen-*.flag
echo "Regression bestanden, bereinigt"
```

Kein Commit (reine Verifikation).

---

## Task 4: Auswertungs-Skill `almanach-trigger-auswertung` (via skill-creator)

**Files:**
- Create: `~/.claude/skills/almanach-trigger-auswertung/SKILL.md` (+ Repo-Spiegelung)

- [ ] **Step 1: skill-creator starten**

Den Skill `skill-creator` aufrufen (CLAUDE.md-Pflicht — NICHT von Hand anlegen). Vorgabe an skill-creator:

> Deutscher Skill `almanach-trigger-auswertung`. Zweck: wertet `~/.claude/state/bug-almanac-triggers.jsonl` aus und schlaegt unnoetige Almanach-Auslöser zum Ausschluss vor (Direktive #2 Selbstbeobachtung). Trigger-Phrasen: „werte die Almanach-Trigger aus", „Almanach-Auswertung", „welche Almanach-Unterbrechungen waren unnoetig", „Almanach-Trigger-Statistik", „analysiere die Almanach-Sonde". Aendert nichts — liest, aggregiert, schlaegt vor.

- [ ] **Step 2: Aggregations-Logik des Skills (Python, lossless)**

Der Skill fuehrt zuerst diese Aggregation aus (laedt NICHT die ganze Datei in den Kontext — nur die kompakte Auswertung; Details bleiben per Pfad erreichbar):

```python
import json, os, collections
p = os.path.expanduser("~/.claude/state/bug-almanac-triggers.jsonl")
rows = []
for fn in (p, p + ".1"):
    if os.path.exists(fn):
        for line in open(fn, encoding="utf-8"):
            line = line.strip()
            if line:
                try: rows.append(json.loads(line))
                except Exception: pass
blocks = [r for r in rows if r.get("event") == "block"]
passes = [r for r in rows if r.get("event") == "pass"]
print(f"Gesamt: {len(rows)}  |  Blocks: {len(blocks)}  |  Passes: {len(passes)}")
by_area = collections.Counter((r.get("slug"), r.get("block_type")) for r in blocks)
print("\nBlocks nach Bereich + Typ:")
for (slug, bt), n in by_area.most_common():
    print(f"  {n:4d}  {slug:18s} {bt}")
# Block-Auszuege je Bereich fuer die KI-Verdachtspruefung (gruppiert, kompakt):
print("\n--- Block-change_excerpts je Bereich (fuer Verdachtspruefung) ---")
ex_by_slug = collections.defaultdict(list)
for r in blocks:
    ex_by_slug[r.get("slug")].append((r.get("file",""), (r.get("change_excerpt","") or "").replace("\n"," ")[:120]))
for slug, items in ex_by_slug.items():
    print(f"\n[{slug}] ({len(items)} Blocks)")
    for f, ex in items[:25]:
        print(f"  {os.path.basename(f)} :: {ex}")
```

- [ ] **Step 3: KI-Verdachtsanalyse + Bericht**

Aus der Aggregation erstellt der Skill (KI-Schritt):
1. **Verhaeltnis-Zeile** Blocks vs. Passes (gesamt + Top-Bereiche).
2. **Verdachtsliste:** Bereiche/Faelle, deren `change_excerpt` nach trivialer Aenderung aussieht — Heuristik-Beispiele, die der Skill explizit pruefen soll:
   - nur `versionName`/`versionCode`/`version =`/`"version":` geaendert → Version-Bump
   - nur String-Ressource (`<string …>` / `getString`) → reine Lokalisierung
   - nur Kommentar/Whitespace/Doku
3. **Konkreter Ausschluss-Vorschlag** pro Verdachtsgruppe im Format:
   `„Bereich <slug>: <n> Blocks, davon <m> Verdacht <Grund> → Vorschlag: <Grund> im Guard von der Pflicht ausnehmen."`
4. Hinweis, dass die Umsetzung (Guard-Ausschluss) ein **separater** Schritt ist (Baustein 3 der Spec).

- [ ] **Step 4: Test des Skills auf Beispiel-Daten**

Beispiel-`.jsonl` mit gemischten Eintraegen erzeugen und den Aggregations-Block laufen lassen:
```bash
mkdir -p "$HOME/.claude/state"
cat > "$HOME/.claude/state/bug-almanac-triggers.jsonl" <<'EOF'
{"ts":"2026-06-16T10:00:00","event":"block","block_type":"almanach-ungelesen","slug":"gradle","area":"Build - Gradle","tool":"Edit","file":"/x/build.gradle.kts","change_excerpt":"versionName = \"0.11.1\"","high_risk":false,"session":"a"}
{"ts":"2026-06-16T10:01:00","event":"block","block_type":"almanach-ungelesen","slug":"gradle","area":"Build - Gradle","tool":"Edit","file":"/x/build.gradle.kts","change_excerpt":"versionCode = 43","high_risk":false,"session":"a"}
{"ts":"2026-06-16T10:02:00","event":"block","block_type":"best-practices","slug":"kotlin","area":"Kotlin","tool":"Edit","file":"/x/Main.kt","change_excerpt":"fun foo() { doRealLogic() }","high_risk":false,"session":"a"}
{"ts":"2026-06-16T10:03:00","event":"pass","block_type":"already-read","slug":"kotlin","area":"Kotlin","tool":"Edit","file":"/x/Main.kt","change_excerpt":"","high_risk":false,"session":"a"}
EOF
```
Den Aggregations-Block (Step 2) ausfuehren.
Expected: „Gesamt: 4 | Blocks: 3 | Passes: 1"; `gradle` mit 2 Blocks, beide Auszuege Version-Bump; `kotlin` mit 1 Block (echte Logik, KEIN Verdacht). Der KI-Bericht markiert `gradle` als Verdacht „Version-Bump", `kotlin` nicht.

- [ ] **Step 5: Aufraeumen + Commit**

```bash
rm -f "$HOME/.claude/state/bug-almanac-triggers.jsonl"
# Skill ins Repo spiegeln (Pfad gemaess skill-creator-Ausgabe; i.d.R. claude-code-setup/skills/ + ggf. Umgebung/Skills/)
cd "$HOME/proggs"
git add claude-code-setup/skills/almanach-trigger-auswertung
git commit -m "#NNN - Neuer Skill almanach-trigger-auswertung (Auswertung der Almanach-Sonde)" -- claude-code-setup/skills/almanach-trigger-auswertung
git fetch origin && git rebase origin/main && git push
```

---

## Task 5: Doku + Memory + Abschluss

**Files:**
- Modify: `~/proggs/.claude/agent-memory/shared/MEMORY.md` (Whiteboard, Pheromon/Hinweis-Eintrag)
- Create/Modify: Memory-Datei im Auto-Memory (Projekt-Eintrag, falls fortzusetzen)

- [ ] **Step 1: Whiteboard-Eintrag**

Im Whiteboard (`~/proggs/.claude/agent-memory/shared/MEMORY.md`) kurz festhalten: Sonde aktiv, Ausgabe `~/.claude/state/bug-almanac-triggers.jsonl`, Auswertung per Skill `almanach-trigger-auswertung`, naechster (separater) Schritt = datengetriebener Guard-Ausschluss.

- [ ] **Step 2: Auto-Memory-Eintrag**

Projekt-Memory anlegen (`memory/`-Ordner) als Trigger fuer die spaetere Ausschluss-Phase: „weiter bei Almanach-Trigger-Ausschluss" → Daten in der `.jsonl` auswerten und unnoetige Auslöser im Guard ausnehmen. MEMORY.md-Indexzeile ergaenzen.

- [ ] **Step 3: Commit**

```bash
cd "$HOME/proggs"
git add .claude/agent-memory/shared/MEMORY.md
git commit -m "#NNN - Whiteboard: Almanach-Trigger-Sonde aktiv + Auswertungs-Skill" -- .claude/agent-memory/shared/MEMORY.md
git fetch origin && git rebase origin/main && git push
```

---

## Self-Review (gegen die Spec)

- **Baustein 1 (Sonde):** Task 1 (.ps1) + Task 2 (.sh) — alle 7 `block_type`/`event`-Kombinationen abgedeckt, JSON-Lines, Felder vollstaendig, Secret-Maskierung, Rotation, FAIL-OPEN. ✓
- **Baustein 2 (Auswertungs-Skill):** Task 4, via skill-creator, lossless-Aggregation + KI-Verdachtsliste. ✓
- **Baustein 3 (spaeterer Ausschluss):** bewusst NICHT umgesetzt; als Memory-Trigger in Task 5 verankert. ✓
- **Cross-Platform:** .ps1 + .sh schema-gleich (Task 2 Step 5), beide Repo-gespiegelt. ✓
- **Pflicht-Auflagen:** Task 0 (claude-hooks Volltext + BP), Task 4 (skill-creator). ✓
- **Funktionserhalt:** Task 3 Regressionscheck aller Block-/Pass-Faelle + FAIL-OPEN. ✓
- **Platzhalter:** keine — alle Schritte mit konkretem Code/Befehl. ✓
- **Konsistenz:** Funktionsnamen `Add-AlmanacTrigger` / `add_almanac_trigger`, Feldnamen identisch zur Spec-Tabelle, `block_type`-Werte konsistent (`almanach-ungelesen`/`volltext-c`/`best-practices`/`kein-almanach`/`already-read`/`disabled`/`ack`). ✓
