# Near-Miss Retention: Beinahe-Fehler behalten statt loeschen (KRITISCH)

> Quelle: arXiv 2601.03192 — MemRL: Self-Evolving Agents via Runtime Reinforcement Learning
> Direktive: #3 Resilient Bugfixing + #1 Superintelligenz

---

## Regel

Eintraege in `experience-store.jsonl` und `bug-cases.jsonl` mit `"near_miss": true`
duerfen beim Pruning NICHT als erste geloescht werden. Sie werden BEVORZUGT behalten —
auch wenn neuere Eintraege vorhanden sind. Ein Near-Miss-Eintrag hat MEHR Lernwert
als ein gewoehnlich erfolgreicher Eintrag.

---

## Warum (Piloten-Analogie)

Ein Pilot der fast abgestuerzt waere, schreibt den Vorfall genau auf — auch wenn er
noch gelandet ist. Warum? Weil der naechste Pilot aus diesem Bericht lernen kann
BEVOR er denselben Fehler macht. Haette der Bericht geheissen "alles gut, kein
Absturz" waere die Warnung verloren gegangen.

Genauso hier: Eine Session mit `success_score: 3` und `error_count: 2` zeigt genau
welche Kombination aus Aufgabentyp und Fehlermustern zu Problemen fuehrt. Diese
Information fehlt in den "alles super"-Sessions komplett. Das System lernt MEHR
aus dem Beinahe-Fehler als aus dem reibungslosen Erfolg.

**Der MemRL-Effekt:** Wenn das System alte Near-Miss-Eintraege beim naechsten
aehnlichen Aufgabentyp LIEST, kann es proaktiv den kritischen Schritt vermeiden
der letztes Mal beinahe gescheitert waere. Das ist Compound Intelligence in Aktion.

---

## Definition: Was ist ein Near-Miss?

Ein Eintrag ist ein Near-Miss wenn ALLE drei Bedingungen zutreffen:

| Bedingung | Wert | Bedeutung |
|-----------|------|-----------|
| `success_score` | >= 2 UND <= 3 | Teilweise erfolgreich — nicht komplett gescheitert |
| `error_count` | > 0 | Aber Fehler sind aufgetreten |
| Kombination | beide gleichzeitig | Beinahe gut gegangen, aber nicht ganz |

**Near-Miss = NICHT gescheitert, ABER erkennbar problematisch.**

Was KEIN Near-Miss ist:
- `success_score: 1` → Richtiger Fehler, nicht Near-Miss (wird als Bug-Case gespeichert)
- `success_score: 4-5` → Erfolg, kein Near-Miss (auch mit kleinen Fehlern)
- `error_count: 0` → Keine Fehler → kein Near-Miss-Signal

---

## Wo Near-Misses gespeichert werden

| Datei | Zweck | Near-Miss-Feld |
|-------|-------|---------------|
| `~/proggs/.claude/agent-memory/shared/experience-store.jsonl` | Automatisch nach jeder Session | `"near_miss": true/false` |
| `~/proggs/.claude/agent-memory/shared/trajectories.jsonl` | Tool-Sequenzen der Session | `"near_miss": true/false` |
| `~/proggs/.claude/agent-memory/shared/bug-cases.jsonl` | Manuell eingetragen | `"near_miss": true/false` |

---

## Format in bug-cases.jsonl

Near-Miss-Faelle koennen auch manuell in die Bug-Datenbank eingetragen werden,
wenn ein Fehler BEINAHE aufgetreten waere aber durch Zufall oder Eingriff verhindert wurde:

```json
{
  "date": "2026-04-06",
  "symptom": "Hook lief beinahe mit falschem Exit-Code",
  "root_cause": "exit 0 stand am falschen Ort — wurde vor der Dot-Source-Pruefung gesetzt",
  "fix": "exit 0 ans Ende verschoben, nach allen Fehlerbehandlungen",
  "files": ["~/.claude/hooks/experience-logger.sh"],
  "tags": ["hook", "exit-code", "near-miss"],
  "severity": "mittel",
  "near_miss": true,
  "near_miss_reason": "Fehler waere beim naechsten Session-Start aufgefallen — diesmal funktionierte es zufaellig"
}
```

---

## Format in experience-store.jsonl

Der Experience Logger setzt `near_miss` automatisch basierend auf Session-Score-Daten:

```json
{
  "date": "2026-04-06",
  "task_type": "hook_edit",
  "task_description": "Hook-Pruning-Logik erweitern",
  "strategy": "auto-captured",
  "tool_count": 12,
  "error_count": 3,
  "success_score": 3,
  "tags": ["auto-logged"],
  "tool_sequence": ["Read", "Edit", "Bash", "Edit", "Bash", "Read"],
  "utility_score": 0.475,
  "near_miss": true,
  "task_category": "config",
  "timestamp_decay": 1.0
}
```

---

## Wann einen Near-Miss eintragen

**Automatisch (kein manueller Eingriff noetig):**
- Der `experience-logger.sh` Hook setzt `near_miss` automatisch bei `success_score 2-3 + error_count > 0`

**Manuell in bug-cases.jsonl eintragen wenn:**
- Eine Session FAST in einem kritischen Fehler geendet waere, aber nicht
- Ein Fehler durch Zufall verhindert wurde (nicht durch bewusste Massnahme)
- Eine Situation "komisch war" aber kein messbarer Fehler aufgetreten ist
- Die gleiche Situation beim naechsten Mal WIRKLICH schiefgehen koennte

---

## Wann einen Near-Miss LESEN

**Vor aehnlichen Aufgaben (PFLICHT):**

Wenn eine neue Aufgabe dem gleichen `task_category` wie ein Near-Miss-Eintrag entspricht,
MUSS der Near-Miss gelesen werden. Konkret: Vor einer `config`-Aufgabe alle Near-Miss-Eintraege
mit `task_category: "config"` durchgehen.

**Grep-Befehl fuer schnelle Suche:**
```bash
grep '"near_miss": true' ~/proggs/.claude/agent-memory/shared/experience-store.jsonl | \
  python3 -c "import sys,json; [print(json.loads(l).get('task_description','?'), '|', json.loads(l).get('task_category','?')) for l in sys.stdin]"
```

**Wann das besonders wichtig ist:**
- Vor dem Bearbeiten von Hooks (haufige Near-Miss-Quelle: exit-Code-Probleme)
- Vor Release-Builds (haufige Near-Miss-Quelle: R8-Kompatibilitaet)
- Vor Cross-Platform-Aenderungen (haefige Near-Miss-Quelle: Pfade, Encoding)
- Vor grossen Refactorings (haefige Near-Miss-Quelle: Funktions-Verlust)

---

## Pruning-Regeln fuer Near-Miss-Eintraege

Der Experience Logger implementiert folgende Pruning-Prioritaet:

| Prioritaet | Welche Eintraege werden zuerst geloescht |
|------------|----------------------------------------|
| 1 (zuerst) | Normale Eintraege (`near_miss: false`), aelteste zuerst |
| 2 (danach) | Near-Miss-Eintraege, nur wenn normale Eintraege erschoepft und Limit noch ueberschritten |

**Limits:** 200 Eintraege in experience-store.jsonl, 100 in trajectories.jsonl.
Near-Misses koennen also bis zu 100% des Limits belegen wenn alle anderen geloescht wurden.
Das ist gewollt — Near-Misses haben dauerhaft Lernwert.

---

## Was NIEMALS passieren darf

- ❌ Near-Miss-Eintraege loeschen nur weil sie "alt" sind
- ❌ Near-Miss ignorieren und normal mit der Aufgabe beginnen (LESEN ist PFLICHT)
- ❌ `near_miss: true` auf einen Score-5-Eintrag setzen (falsche Klassifikation)
- ❌ Near-Miss-Eintraege in bug-cases.jsonl NICHT eintragen wenn ein Fehler beinahe aufgetreten waere
- ❌ Den Experience Logger so aendern dass er Near-Miss-Erkennung deaktiviert
- ❌ Pruning-Logik aendern ohne Near-Miss-Schutz beizubehalten
- ❌ Near-Miss-Signal ignorieren weil "es ja gut gegangen ist" — gerade DANN ist der Lernwert hoch

---

## Zusammenspiel mit anderen Systemen

| System | Zusammenspiel |
|--------|--------------|
| **Direktive #3 (Resilient Bugfixing)** | Near-Miss IST ein Bugfix-Signal — Root Cause analysieren bevor der Beinahe-Fehler zum echten Fehler wird |
| **CBR (bug-cases.jsonl)** | Near-Miss-Faelle in CBR eintragen ermoeglichen "Retrieve" in Zukunft |
| **Selbstbeobachtung (Direktive #2)** | Near-Miss-Erkennung IST Selbstbeobachtung — das System erkennt eigene Schwachstellen |
| **Pheromon-Tabelle (MEMORY.md)** | Near-Miss-Muster koennen als "zu vermeidende Ansaetze" in die Tabelle eingetragen werden |
| **Experience Logger Hook** | Automatische Erkennung und Speicherung via `experience-logger.sh` |

---

## Compound Intelligence Effect

Near-Miss-Retention verstaerkt den Compound Intelligence Effect direkt:

```
Session N:    Near-Miss tritt auf (score: 3, errors: 2) → gespeichert mit near_miss: true
Session N+1:  Gleicher task_category → Near-Miss wird gelesen → kritischer Schritt beachtet
Session N+1:  Kein Fehler mehr (score: 5, errors: 0) → Near-Miss hat gewirkt
Session N+5:  Pattern etabliert → ganzer task_category laeuft zuverlaessig

Ohne Near-Miss-Retention:
Session N:    Near-Miss wird nach 3 Wochen geloescht (Limit erreicht)
Session N+20: Gleicher Fehler tritt wieder auf — System hat nicht gelernt
```

Das ist genau das Gegenteil von Superintelligenz. Near-Miss-Retention verhindert das.
