# Best Practices: Arbeitsweise & Verhalten (Kategorie 10)

> Stand: 2026-05-25 | Claude Code v2.1.150 | Quelle: offizielle Anthropic-Dokumentation

---

## 1. Der 4-Phasen-Workflow (offiziell empfohlen)

Für nicht-triviale Aufgaben diesen Workflow konsequent anwenden:

```
Phase 1 — EXPLORE:    Codebase lesen, Fragen stellen, Kontext aufbauen
Phase 2 — PLAN:       Ansatz formulieren, mit Benutzer diskutieren
Phase 3 — IMPLEMENT:  Code schreiben, Tests grün machen
Phase 4 — COMMIT:     Aufräumen, committen, Status-Meldung
```

**Wichtig:** NIEMALS Phase 1 und 3 zusammenführen. Wer direkt implementiert ohne zu erkunden, trifft falsche Annahmen.

### Plan Mode (Shift+Tab)

- Aktiviert **ausschließlich Lesezugriff** — kein Schreiben, kein Ausführen
- Ideal für Phase 1+2: Codebase erkunden ohne Seiteneffekte
- Deaktiviert sich nach dem Plan automatisch (Shift+Tab wieder drücken für Auto Mode)

### Auto Mode (2x Shift+Tab)

- Claude führt autonom aus bis die Aufgabe erledigt ist
- Nutzt internen Classifier (auf Sonnet 4.6) um Read-only vs. Schreib-Ops zu entscheiden
- Für lange unbeaufsichtigte Operationen: `--dangerously-skip-permissions` als Flag

---

## 2. Kontextmanagement während der Session

### /clear — der wichtigste Befehl

```
Nach jeder abgeschlossenen Aufgabe:  /clear  → Kontext zurücksetzen
Nach 2 fehlgeschlagenen Versuchen:   /clear  → Neustart mit frischer Perspektive
```

**Faustregel:** Wenn du merkst dass Claude im Kreis dreht → sofort `/clear`. Nicht 5. Versuch abwarten.

### /compact — Kontext komprimieren ohne zu löschen

- Fasst Konversations-Historie zusammen
- Nützlich bei langen Sessions bevor Kontext-Fenster voll wird
- **Invalidiert den Prompt-Cache** → danach kurzfristig höhere Kosten

### /rewind — Teilschritt rückgängig machen

- Rollt zur letzten sicheren Stelle zurück
- Besser als manuelles `git revert` in den meisten Fällen

### /btw — Side-Questions ohne Kontext-Auswirkung

```
/btw was ist der Unterschied zwischen X und Y?
```
- Stellt eine Frage ohne sie in den Haupt-Konversations-Thread einzubauen
- Kontext bleibt sauber, keine "Ablenkung" für den nächsten Turn

---

## 3. TDD: Das stärkste agentische Arbeitspattern

Aus offizieller Anthropic-Empfehlung: **TDD ist das effektivste Muster für agentische Codierung.**

### Warum TDD für Agents?

- Tests sind **verifiable checkpoints** — Claude weiß wann fertig
- Kein Raten ob Implementierung korrekt ist
- Reduziert Halluzinations-Risiko bei der Implementierung
- Ermöglicht autonomes Arbeiten ohne menschliche Verifikation

### TDD-Ablauf für Claude Code

```
1. Spec schreiben (Anforderungen als Kommentar oder Datei)
2. Tests schreiben (failing tests als Checkpoint)
3. Claude implementiert bis Tests grün
4. Refactor unter grünen Tests
5. Commit
```

**Konkret:** "Schreibe zuerst Tests für Funktion X, dann implementiere sie bis alle Tests grün sind."

---

## 4. Writer/Reviewer Pattern

Für kritischen Code: **Zwei separate Sessions** statt einer.

```
Session 1 (Writer):   Implementierung in Isolation, ohne Review-Druck
Session 2 (Reviewer): Frischer Kontext, keine "Investition" in die Lösung
```

**Warum:** Claude in derselben Session reviewt den eigenen Code mit Bestätigungsbias. Neue Session = objektiver.

Als Subagents:
```
Agent 1 (coder):         Implementiert Feature A
Agent 2 (code-reviewer): Reviewt Feature A mit frischem Kontext
```

---

## 5. Fan-out Pattern: Parallele Batch-Operationen

Für gleichartige Operationen auf vielen Dateien:

```bash
# Fan-out via claude -p in Bash-Loop
for file in src/**/*.ts; do
  claude -p "Add JSDoc to all exported functions in $file" --output-format stream-json &
done
wait
```

**Oder als parallele Subagents:**
```
Agent 1: Datei A refactoren
Agent 2: Datei B refactoren  
Agent 3: Datei C refactoren
→ Alle gleichzeitig, dann Merge
```

**Datei-Ownership-Regel:** Zwei Agents NIEMALS dieselbe Datei gleichzeitig bearbeiten lassen.

---

## 6. Kontext für Subagents: Großzügig geben

Subagents erben NICHT die Konversations-Historie. Im Prompt mitgeben:

- Welches Projekt (Name, Zweck)
- Welche Dateien (Ownership)
- Welche Konventionen (Sprache, Stil)
- Was das Ziel ist (konkret)
- Was NICHT gemacht werden soll

**Template:**
```
Du arbeitest an [Projekt]. Deine Aufgabe: [Aufgabe].
Dateien die du bearbeiten darfst: [Liste].
Konventionen: [Kotlin, ktfmt, 4 Spaces, ...].
Commit nach jeder Änderung.
```

---

## 7. CLAUDE.md: Ruthless Pruning

- **Unter 200 Zeilen** — alles was länger selten genutzt wird, gehört in einen Skill
- Regeln die nur für einen Kontext gelten → Skill (wird on-demand geladen)
- Regeln die IMMER gelten → CLAUDE.md
- **Keine Redundanz** zwischen CLAUDE.md und `~/.claude/rules/*.md`
- Regelmäßig prunen: "Welche Regel habe ich letzte Woche wirklich gebraucht?"

---

## 8. Let Claude Interview You

Für große, unklare Features: **Claude stellt Fragen, du antwortest.**

```
"Ich will Feature X bauen. Stelle mir alle Fragen die du brauchst um anzufangen."
```

- Nutzt AskUserQuestion intern (bei manuellen Eingaben)
- Klärt Anforderungen bevor falscher Code entsteht
- Verhindert Scope-Creep ("oh, das war gemeint")

---

## 9. Kurskorrektur: Früh eingreifen

**Nicht bis zum Ende warten.** Wenn Claude in die falsche Richtung geht:

- **Nach 2 Fehlversuchen** → sofort `/clear` und neuen Ansatz beschreiben
- **Falscher Ansatz erkennbar** → direkt unterbrechen (Ctrl+C), nicht "sehen wo es hinführt"
- **Falsches Modell** → `/model sonnet` oder `/model opus` und neu starten

---

## 10. Checkpoints: Commits als Rettungspunkte

Aus Anthropic Best Practice: **Commit früh und oft.**

- Vor jedem riskanten Schritt committen
- Lieber 5 kleine Commits als 1 großer
- Claude Code kann nach Commit sicher `/rewind` oder `git reset` verwenden
- **Git-Stash** für Work-in-Progress die noch nicht commit-reif sind

---

## 11. Zusammenfassung: Die 10 wichtigsten Verhaltensregeln

1. **4-Phasen-Workflow** (Explore → Plan → Implement → Commit) — nie überspringen
2. **Plan Mode** für Erkundung, Auto Mode für Ausführung
3. **TDD** als Standard-Implementierungsmuster
4. **Writer/Reviewer** in separaten Sessions für kritischen Code
5. **Fan-out** für gleichartige Batch-Operationen
6. **Subagents**: Kontext explizit mitgeben, Datei-Ownership klar trennen
7. **CLAUDE.md unter 200 Zeilen** — selten genutzte Regeln in Skills
8. **Früh korrigieren** — nach 2 Fehlern `/clear`, nicht weitermachen
9. **Commits als Checkpoints** — früh und oft, vor riskanten Schritten
10. **/btw für Side-Questions** — Haupt-Kontext nicht mit Nebenthemen verschmutzen
