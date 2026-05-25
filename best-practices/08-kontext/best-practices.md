# Best Practices: Kontext-Management

> Recherchiert: 2026-05-25 | Claude Code v2.1.150 | Quellen: code.claude.com/docs

---

## CLAUDE_AUTOCOMPACT_PCT_OVERRIDE — Kritische Falle

**Was:** Umgebungsvariable zum Anpassen des Compaction-Schwellwerts. Funktioniert durch einen Math.min()-Clamp.

**Best Practice:**
- Der Wert wird via `Math.min(userOverride, defaultThreshold)` verarbeitet
- Default-Schwellwert liegt bei ca. **83%** des Kontextfensters
- `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=100` bewirkt NICHTS über 83% hinaus (Math.min clamps)
- Der Wert kann nur SENKEN, nie erhöhen
- Empfohlener Wert: 100 (effektiv = Standard-Schwellwert beibehalten)
- **Niemals unter 85 setzen** (config-guard sollte das blockieren) — zu frühe Komprimierung

```bash
# In settings.json env-Sektion:
"CLAUDE_AUTOCOMPACT_PCT_OVERRIDE": "100"
# Bewirkt: Komprimierung bei ~83% (Standard), nicht früher
```

**Kontext-Buffer:** ~13.000 Tokens sind für Antwort-Generierung reserviert und werden NICHT genutzt. Die eigentliche Komprimierungsgrenze liegt daher etwas unter dem nominalen Schwellwert.

**Quelle:** code.claude.com/docs/en/how-claude-code-works, github.com/anthropics/claude-code/issues/31806, turboai.dev  
**Stand:** v2.1.150

---

## Komprimierungs-Mechanismus: Was passiert bei /compact

**Was:** `/compact` oder automatische Komprimierung verdichtet den Konversations-Kontext.

**Best Practice:**

Was **überlebt** die Komprimierung:
- Wichtige Entscheidungen und Erkenntnisse (aus der Zusammenfassung)
- CLAUDE.md wird neu geladen
- MEMORY.md wird neu geladen (erste 200 Zeilen / max 25KB)
- System-Prompt bleibt unverändert
- Aktuelle Aufgaben-Informationen (wenn Claude sie im Summary festhält)

Was **NICHT** überlebt:
- Detaillierte Tool-Ausgaben
- Konversations-Historie
- Zwischen-Ergebnisse von Tool-Aufrufen
- Lokale Variablen und Zwischenzustände

**Kompakt-Instruktionen in CLAUDE.md:** Man kann angeben was in der Zusammenfassung betont werden soll:
```markdown
## Compact Instructions
Bei der Komprimierung: Behalte immer den aktuellen Branch-Namen, 
offene Tasks und die letzte Commit-Nummer im Summary.
```

**Quelle:** code.claude.com/docs/en/how-claude-code-works, code.claude.com/docs/en/memory  
**Stand:** v2.1.150

---

## Auto Memory System (v2.1.59+)

**Was:** Automatisches Memory-System das MEMORY.md und zugehörige Topic-Dateien verwaltet.

**Best Practice:**
```json
{
  "autoMemoryEnabled": true,
  "autoMemoryDirectory": ".claude/agent-memory/shared"
}
```

**Limits die bekannt sein müssen:**
- MEMORY.md: Nur **erste 200 Zeilen** werden geladen (nicht die ganze Datei!)
- Maximum: **25KB** pro MEMORY.md
- Topic-Dateien: Werden on-demand geladen wenn relevant
- Index-Einträge sollten max. ~200 Zeichen sein

**Empfohlene Struktur:**
```
.claude/agent-memory/shared/
├── MEMORY.md          # Index (max 200 Zeilen / 25KB)
├── topic-android.md   # Detail-Datei
├── topic-hooks.md     # Detail-Datei
└── bug-cases.jsonl    # Bug-Datenbank
```

**Quelle:** code.claude.com/docs/en/memory  
**Stand:** v2.1.59+, v2.1.150

---

## Kontext-Fenster-Anatomie

**Was:** Wie das Kontextfenster aufgeteilt wird — wichtig für Token-Budgetierung.

**Best Practice — typische Aufteilung:**

| Komponente | ~Tokens | Anmerkung |
|-----------|---------|-----------|
| System Prompt | ~4.200 | Fest, unveränderlich |
| MEMORY.md | ~680 | Erste 200 Zeilen |
| Umgebungs-Info | ~280 | Plattform, Verzeichnis, etc. |
| MCP-Tool-Definitionen | ~120 (deferred) | Werden verzögert geladen |
| CLAUDE.md | variabel | Alle geladenen Regeln |
| Konversations-Historie | variabel | Wächst mit der Session |
| Antwort-Buffer | ~13.000 | Reserviert, nicht nutzbar |

**Konsequenzen:**
- Lange CLAUDE.md-Dateien fressen Kontext
- Viele MCP-Tools erhöhen den Basis-Kontext-Overhead
- `.claude/rules/` mit Pfad-Scoping reduziert Overhead
- MEMORY.md-Index kurz halten (Details in Topic-Dateien)

**Quelle:** code.claude.com/docs/en/how-claude-code-works, code.claude.com/docs/en/memory  
**Stand:** v2.1.150

---

## Microcompact vs. /compact

**Was:** Zwei verschiedene Komprimierungsmechanismen mit unterschiedlichem Verhalten.

**Best Practice:**

**Microcompact (automatisch, leise):**
- Läuft kontinuierlich im Hintergrund
- Lagert große Tool-Ergebnisse auf Disk aus
- Inkrementelles Aufräumen ohne sichtbare Unterbrechung
- Kein Kontext-Verlust

**Auto-Compaction (bei ~83%):**
- Vollständige Zusammenfassung des Konversationsverlaufs
- Sichtbare Unterbrechung der Session
- Kontext-Verlust (nur Summary bleibt)
- Wird durch `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE` beeinflusst

**/compact (manuell):**
- Sofortige Komprimierung auf Befehl
- Nützlich bevor wichtige, token-intensive Aufgaben
- `compact` in CLAUDE.md definiert was betont werden soll

**Empfehlung:** Vor großen Aufgaben manuell `/compact` ausführen wenn Kontext bereits voll ist.

**Quelle:** code.claude.com/docs/en/how-claude-code-works  
**Stand:** v2.1.150

---

## Subagents für Kontext-Isolation

**Was:** Subagents erhalten einen frischen Kontext — ideal für token-intensive Teilaufgaben.

**Best Practice:**
- Subagents erben NICHT die Konversations-Historie
- Jeder Subagent startet mit dem System-Prompt + CLAUDE.md + MEMORY.md
- Für große, isolierbare Aufgaben: Subagent spawnen statt im Hauptchat arbeiten
- Subagent-Ergebnisse zurückgeben lassen ohne den gesamten Verlauf mitzunehmen

**Kontext-Kosten von Subagents:**
- Parallele Subagents teilen sich NICHT das Kontextfenster
- Jeder bekommt sein eigenes Fenster (separate API-Aufrufe)
- Kosteneffizienz: 3-5 Sonnet-Subagents für Implementation, 1 Opus für Review

**Quelle:** code.claude.com/docs/en/how-claude-code-works  
**Stand:** v2.1.150

---

## Skills mit disable-model-invocation

**Was:** Skills können mit `disable-model-invocation: true` konfiguriert werden um Kontext zu sparen.

**Best Practice:**
- Skill-Definitionen tragen zum Kontext-Overhead bei
- Selten genutzte Skills: `skillOverrides` in settings.json nutzen um sie zu deaktivieren
- `maxSkillDescriptionChars` — Skill-Beschreibungen kurz halten
- `skillListingBudgetFraction` — Anteil des Kontextfensters für Skill-Listings begrenzen

```json
{
  "skillOverrides": {
    "selten-genutzter-skill": {
      "disabled": true
    }
  },
  "maxSkillDescriptionChars": 200,
  "skillListingBudgetFraction": 0.1
}
```

**Quelle:** code.claude.com/docs/en/settings  
**Stand:** v2.1.129+ (skillOverrides), v2.1.150

---

## CLAUDE.md — Token-effizient halten

**Was:** CLAUDE.md wird bei jeder Session und nach Komprimierung geladen — Token-Kosten fallen immer an.

**Best Practice:**
- Hauptdatei kurz halten, Details in `.claude/rules/` auslagern
- HTML-Kommentare werden aus dem Kontext herausgefiltert (für unsichtbare Metadaten)
- `@path`-Imports für dynamisch geladene Teile nutzen
- `claudeMdExcludes` in settings.json: Bestimmte CLAUDE.md-Dateien ausschließen
- Einzeiler-Verweise auf Rules-Dateien statt vollständigen Regeltext in CLAUDE.md

```markdown
<!-- Dieser Kommentar wird nicht als Kontext geladen -->
Vollständige Regel: siehe `~/.claude/rules/meine-regel.md`
```

**Quelle:** code.claude.com/docs/en/memory  
**Stand:** v2.1.150

---

## Checkpoint-System

**Was:** Claude Code erstellt automatisch Checkpoints bevor risikoreiche Aktionen ausgeführt werden.

**Best Practice:**
- Checkpoints erlauben das Zurücksetzen auf vorherigen Zustand
- Werden automatisch erstellt — kein manuelles Eingreifen nötig
- Bei kritischen Operationen: Checkpoint-Erstellung abwarten
- Checkpoints nicht mit dem Kontext-System verwechseln — sie sind Git-ähnliche Snapshots des Arbeitsverzeichnisses

**Quelle:** code.claude.com/docs/en/how-claude-code-works  
**Stand:** v2.1.150
