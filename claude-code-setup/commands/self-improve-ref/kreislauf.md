# Der Kreislauf — was ein self-improve-Lauf konkret tut

> Manuell gestartet, voll sichtbar, kein Hintergrund. Jede Phase hat **konkrete Aktionen**
> und **Pflicht-Outputs**, damit der Lauf nie zu vager Philosophie verkommt.
> Maßstab für alles: [intelligenz-definition.md](intelligenz-definition.md).

**Die drei Pflicht-Outputs jedes Laufs (sonst ist der Lauf nicht fertig):**
1. Mindestens **1 umgesetzte und an der Wirklichkeit geprüfte** intelligentere Alternative.
2. Mindestens **1 erforschte** Alternative — auch wenn nichts kaputt schien (Achse 9).
3. Mindestens **1 Reflexion** über die Definition von Intelligenz selbst (Phase 6).

---

## Phase 0 — Maßstab laden + Pre-Checks

1. **[intelligenz-definition.md](intelligenz-definition.md) vollständig lesen.** Das ist die Linse für den ganzen Lauf.
2. **Merge-Konflikt-Pre-Check** (siehe [altlasten.md](altlasten.md) → A1). Bei Konflikt-Markern in MEMORY.md/CLAUDE.md/.mcp.json: SOFORT abbrechen, Frank warnen.
3. **Whiteboard schlank einlesen:** `.claude/agent-memory/shared/MEMORY.md` — aber nicht blind komplett in den Kopf laden. Gezielt: offene Fehler (Status OFFEN), letzter Systemzustand, "Forschung & Intelligence". Just-in-time statt alles (Achse 3 + RI-3).
4. **Thoroughness erkennen:** "/self-improve" allein → Standard (1 gründlicher Durchlauf jeder Phase). "sehr ausführlich" / "tief" → Thorough (mehr Subjects, mehr Researcher). "Fokus [X]" → ein Subjekt vertiefen.

## Phase 1 — Handlungsweisen sammeln (die Subjects)

Sammle **echte, konkrete** "Arten zu handeln", die hinterfragt werden — keine erfundene Beschäftigung.
Drei Quellen, in dieser Reihenfolge der Wichtigkeit:

**(a) Wie wurde zuletzt wirklich gehandelt — inkl. wie Claude selbst programmiert (erstklassig!):**
- Letzte 10–20 Commits (`git log --oneline -20`), letzte Trajektorien (`trajectories.jsonl` tail), letzte Sessions.
- Frage je Beispiel: "Wie wurde hier vorgegangen? Wäre ein intelligenterer Weg möglich gewesen?"
- Das ist Franks Kernpunkt: *die eigene Handlungsweise* ist das wichtigste Subjekt, nicht nur die Config.

**(b) Echte Reibung (das Gold):**
- Offene Fehler im Whiteboard (Status OFFEN), `bug-cases.jsonl` (neue Fälle), Feedback-Memories (`~/.claude/projects/*/memory/feedback_*.md`) — Frank-Korrekturen sind das stärkste Signal.
- Wiederkehrende Handarbeit: Wurde dasselbe mehrfach von Hand gemacht? → Kandidat für Automatisierung.

**(c) Die Mechanismen des Systems (sekundär, untergeordnet):**
- Regeln (`~/.claude/rules/`), Hooks, Agenten, Settings — und **der Skill selbst**.
- Systempflege als Subjekt: "Ist das System gerade die intelligenteste Version seiner selbst?" (Tool-/CVE-Hinweise, Cross-Platform-Lücken). Wichtig, aber NICHT die Identität des Laufs.

**Output Phase 1:** eine kurze Liste von 5–15 konkreten Subjects, je 1 Zeile.

## Phase 2 — Hinterfragen gegen die Definition

Für jedes Subjekt:
1. Beschreibe den **aktuellen Weg A** in einem Satz.
2. Frage: "Gibt es einen intelligenteren Weg B?" — geh dabei die 10 Achsen durch.
3. **Selbst-Skepsis (Achse 8) ist Pflicht bei den 2–3 wichtigsten Subjects:** lass die Idee von einem `challenger`-Agenten (oder einer bewussten Gegen-Perspektive) angreifen, BEVOR du sie für gut hältst. Das verhindert, dass das System sich selbst belügt (RI-1, Anti-Goodhart ohne Zahl).
4. Formuliere Kandidaten als: **"A → B, intelligenter weil überlegen auf [Achse(n)], ohne anderswo zu verlieren."**

**Output Phase 2:** eine Kandidatenliste (aktueller Weg → intelligenterer Weg → Begründung).

## Phase 3 — Forschen (Pflicht-Neugier)

- Für jedes Subjekt, bei dem **keine** Alternative offensichtlich war: **trotzdem mindestens eine erforschen.** Details + Researcher-Vorlagen: [forschung.md](forschung.md).
- **Harte Regel:** Pro Lauf wird **mindestens eine** Alternative *erforscht* (nicht nur überlegt), auch wenn alles gut aussah. "Es geht wohl nicht besser" ist der Auftrag zu forschen, nicht das Ergebnis (RI-2).
- Parallele Researcher (5–7, Continuous-Spawning), jeder mit Robustheits-Preamble. Sie liefern intelligentere Wege aus der Welt da draußen (neue Werkzeuge, Denkweisen, Muster, was andere besser machen).

**Output Phase 3:** mindestens 1 erforschte Alternative mit Quelle + Bewertung gegen die Achsen.

## Phase 4 — An der Wirklichkeit prüfen (der Goodhart-Schutz)

Für die gewählte(n) Verbesserung(en):
1. **Umsetzen** (sichere, kleine Verbesserungen direkt — neue Regel, neuer Schutz-Hook, Bloat entfernen, Wissen festigen).
2. **An einer echten Aufgabe verifizieren:** Funktioniert die Änderung wirklich? Feuert der Hook? Greift die Regel? Löst der neue Weg ein konkretes Beispiel besser? Das ist eine **Ja/Nein-Prüfung an der Realität**, keine Zahl.
3. Wenn **nein** → **zurücknehmen** und in Phase 6 reflektieren, warum.
4. Großes/Riskantes (die 3 Direktiven, der Skill selbst, Löschungen) wird **nicht** selbst umgesetzt, sondern als Vorschlag in den Bericht (Phase 7) gelegt.

**Funktions-Erhalt (Pflicht):** Keine Verbesserung darf bestehende Funktionalität entfernen, auskommentieren oder still schlucken (Direktive #3). Vorher/Nachher gedanklich abgleichen.

**Output Phase 4:** mindestens 1 umgesetzte + real verifizierte Verbesserung (oder ehrliche Notiz "umgesetzt, an Realität gescheitert, zurückgenommen, Grund: …").

## Phase 5 — Festigen (episodisch → semantisch)

- Verdichte das Erlebte zu **allgemeinem** Wissen: aus "heute ist X passiert" wird "wenn Y, dann Z".
- Schreibe es an den richtigen Ort (Details: [gedaechtnis.md](gedaechtnis.md)): bewährtes Muster → Skill-Library; neue Verhaltensregel → `~/.claude/rules/` oder Whiteboard; adoptierte/verworfene Alternative → Intelligenz-Journal.
- **Gedächtnis schlank halten:** kein Wissensmüll, alte erledigte Einträge archivieren, Near-Miss behalten (RI-3, Context Rot).

**Output Phase 5:** mindestens 1 destillierte Lektion, korrekt abgelegt.

## Phase 6 — Die Definition selbst verbessern (Meta)

- Reflektiere verbal (keine Zahl): "Was hat dieser Lauf gebracht? Was lief unintelligent? Was mache ich nächstes Mal anders?"
- Prüfe: Hat der Lauf eine **neue Regel der Intelligenz** enthüllt oder eine **Schwäche einer Achse**?
- Wenn ja UND die drei Bedingungen erfüllt sind (Beleg, Überprüfung, Verlustfreiheit — siehe [intelligenz-definition.md](intelligenz-definition.md)): trage sie ins Änderungsprotokoll / die Regeln-der-Intelligenz-Tabelle ein.
- Wenn nein: das ist auch ein Ergebnis — kurz festhalten "Definition hielt diesem Lauf stand".

**Output Phase 6:** 1 Reflexion + ggf. 1 Verbesserung der Definition.

## Phase 7 — Bericht + Entscheidung

- Verständlicher Bericht (für Nicht-Programmierer) + **Entscheidungsliste** (Was / Warum / Empfehlung / Ja-Nein). Format: [gedaechtnis.md](gedaechtnis.md).
- Sichere Verbesserungen sind bereits umgesetzt (Phase 4); Großes/Riskantes liegt als Vorschlag vor.
- **Cross-Platform-Sync + Commit/Push** für alle eigenen Änderungen (siehe [altlasten.md](altlasten.md) → A2, A6).
- Falls Shell/Terminal-Updates anstehen: **ganz zuletzt**, nur nach Bestätigung (siehe [altlasten.md](altlasten.md) → A3).

---

## Leitplanken für den ganzen Lauf

- **Sichtbar:** keine versteckten Subagenten, keine `run_in_background`-Stille. Frank liest alles mit.
- **Parallel wo unabhängig:** Researcher und Scans gleichzeitig starten, nicht nacheinander (Achse 10).
- **Subagenten absturzsicher:** große Dateien nie komplett ins LLM laden (Python/Grep/Ranges), enger Scope, Ergebnisse in Dateien auslagern. Bei Worker-Crash: Orchestrator-Resume (kleiner + diszipliniert neu spawnen), nie die Aufgabe aufgeben.
- **Hartnäckigkeit:** wenn nach 3 erfolglosen Versuchen Entropie steigt → STOPP, nachschlagen/forschen statt weiter raten (Entropie-Reduktion).
