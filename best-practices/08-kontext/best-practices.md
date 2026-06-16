# Kontext-Management — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Quelle-Hierarchie: offiziell = code.claude.com/docs (Grundwahrheit), extern = andere Quellen (klar markiert).
> Erstellt von Researcher-Agent am 2026-05-25.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | CLAUDE.md-Groesse | Ziel < 200 Zeilen; Detail in Rules/Skills (Context-Rot ab ~50 % Fuellung) | CLAUDE.md |
| 2 | `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE` | kann die Schwelle nur SENKEN (Math.min), nie erhoehen | OVERRIDE |
| 3 | was Compaction ueberlebt | Root-CLAUDE.md via Disk-Reread; reine Chat-Instruktionen gehen verloren | Was Komprimierung ueberlebt |
| 4 | `MEMORY.md` | nur ~200 Zeilen / 25 KB werden geladen — Index kurz, Detail auslagern | Auto Memory |
| 5 | grosse Daten | Subagents / File-as-Memory statt Hot-Context (lossless) | Subagents fuer Kontext-Schutz |
| 6 | MCP/Skills | deferred / on-demand laden statt Session-Start-Aufblaehung | MCP Tool Definitions |
| 7 | Compact Instructions | in CLAUDE.md pflegen — steuert was die Zusammenfassung behaelt | Compact Instructions |

---

## Wie der Kontext-Buffer funktioniert

- **Was:** Claude Code reserviert ~33.000 Token (≈16,5 % eines 200K-Fensters) als festen Arbeitsbereich für die Komprimierung. Die restlichen ~167.000 Token stehen für die eigentliche Arbeit zur Verfügung. Vor 2026 war der Buffer größer (~45K Token); er wurde in v2.1.21 auf 33K reduziert — das gibt ca. 12.000 zusätzliche nutzbare Token.
- **Best Practice:** Nicht versuchen, den Buffer zu umgehen. Er ist das Sicherheitsnetz des Systems. `/context` aufrufen, um zu sehen, was den Platz gerade verbraucht.
- **Quelle:** [code.claude.com/docs/en/how-claude-code-works](https://code.claude.com/docs/en/how-claude-code-works) (offiziell), [claudefa.st/blog/guide/mechanics/context-buffer-management](https://claudefa.st/blog/guide/mechanics/context-buffer-management) (extern)
- **Stand:** 2026-05-25

---

## Drei Komprimierungs-Ebenen: Microcompact, Auto-Compact, /compact

- **Was:** Claude Code hat drei verschiedene Komprimierungs-Mechanismen:
  1. **Microcompact** (automatisch, ~60–70 % Auslastung): Leichter Pass — schreibt große Tool-Ergebnisse (Datei-Reads, Grep-Output, Befehlsausgaben) auf die Festplatte und hält nur eine Referenz im Kontext. Kein API-Aufruf nötig. Gibt 10–30K Token frei und verzögert die volle Auto-Komprimierung.
  2. **Auto-Compact** (automatisch, ~83,5 % Auslastung): Fasst die gesamte Konversations-Historie zusammen — ein strukturierter Summary von max. 20.000 Token wird generiert. Oldest tool outputs werden zuerst entfernt, danach wird die Konversation zusammengefasst.
  3. **`/compact` (manuell, jederzeit)**: Benutzer-ausgelöste Komprimierung mit optionaler Fokus-Anweisung: `/compact Focus on the API changes`. Ist präziser als Auto-Compact, weil sie zu einem logischen Aufgaben-Endpunkt ausgelöst wird.
- **Best Practice:** Manuell `/compact` bevorzugen — am Ende jeder abgeschlossenen Aufgabe oder bei 60 % Auslastung auslösen, bevor die Qualität sinkt. Nicht bis zu 90 %+ warten. Wer wartet, bis Auto-Compact feuert, verliert Kontrolle über was zusammengefasst wird.
- **Quelle:** [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell), [morphllm.com/claude-code-compact](https://www.morphllm.com/claude-code-compact) (extern)
- **Stand:** 2026-05-25

---

## CLAUDE_AUTOCOMPACT_PCT_OVERRIDE — Verhalten und Einschränkungen

- **Was:** Umgebungsvariable, die den Schwellwert für die Auto-Komprimierung steuert (Werte 1–100). **Kritische Einschränkung:** Das System verwendet intern `Math.min()`, sodass der Wert den internen Default (~83 %) nicht überschreiten kann. Die Variable kann die Komprimierung nur *früher* auslösen, nie später. Wer `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=100` setzt (wie in diesem System konfiguriert), hat faktisch keinen Effekt auf die obere Grenze — die Komprimierung feuert trotzdem bei ~83 %.
- **Best Practice:** Die Variable nur nutzen, um frühere Komprimierung zu erzwingen (z.B. bei `50` für öfter aber kleinere Zusammenfassungen). Um späte Komprimierung zu erreichen: manuell `/compact` nutzen und `/clear` zwischen unabhängigen Aufgaben. Den Wert 100 zu setzen ist technisch wirkungslos für die obere Grenze.
- **Hinweis für dieses System:** In CLAUDE.md steht `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE ist IMMER 100` — das ist eine bewusste Entscheidung (Frank, 2026-05-24), die große sichtbare Komprimierung erst ganz spät zulässt, während Microcompact den Rest leistet.
- **Quelle:** [github.com/anthropics/claude-code/issues/31806](https://github.com/anthropics/claude-code/issues/31806) (extern, GitHub-Issue), [claudefa.st/blog/guide/mechanics/context-buffer-management](https://claudefa.st/blog/guide/mechanics/context-buffer-management) (extern)
- **Stand:** 2026-05-25

---

## Was Komprimierung überlebt — und was verloren geht

- **Was überlebt:**
  - **CLAUDE.md** wird nach jeder Komprimierung frisch von der Festplatte neu eingelesen und re-injiziert. Das ist der zuverlässigste Kanal für persistente Regeln.
  - **Auto Memory** (erste 200 Zeilen / 25 KB von MEMORY.md) wird am Sessionstart geladen.
  - **Datei-Änderungen auf der Festplatte** bleiben erhalten (Git-Commits, bearbeitete Dateien).
  - **Komprimierter Konversations-Summary** bleibt als komprimierter Text erhalten.

- **Was verloren geht:**
  - Exakte Datei-Pfade, Zeilennummern und Fehlermeldungen aus früheren Turns.
  - Debugging-Hypothesen und Architektur-Überlegungen.
  - Detaillierte Anweisungen aus frühen Konversations-Turns.
  - Alte Tool-Outputs (Grep-Ergebnisse, Datei-Reads, Befehlsausgaben).

- **Best Practice:** Wichtige persistente Regeln gehören in CLAUDE.md, nicht in die Konversation. Für Komprimierungs-spezifische Anweisungen eine `## Compact Instructions`-Sektion in CLAUDE.md anlegen. Claude Code liest diese Sektion sowohl bei manuellem `/compact` als auch bei Auto-Compact.

- **Quelle:** [code.claude.com/docs/en/how-claude-code-works](https://code.claude.com/docs/en/how-claude-code-works) (offiziell), [morphllm.com/claude-code-auto-compact](https://www.morphllm.com/claude-code-auto-compact) (extern)
- **Stand:** 2026-05-25

---

## Compact Instructions in CLAUDE.md — Steuern was erhalten bleibt

- **Was:** Eine dedizierte Sektion `## Compact Instructions` (oder ähnliche Überschrift) in CLAUDE.md gibt Claude Code Anweisungen, was bei der Komprimierung bevorzugt erhalten werden soll. Wird von der offiziellen Doku explizit empfohlen.
- **Best Practice:**
  ```markdown
  ## Compact Instructions
  - Vollständige Liste der geänderten Dateien immer erhalten
  - Aktuelle Test-Befehle und Fehlerausgaben priorisieren
  - Offene Bugs und deren Root-Cause immer zusammenfassen
  ```
  Alternativ bei manueller Ausführung: `/compact Focus on API changes and modified file list`.
- **Quelle:** [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell)
- **Stand:** 2026-05-25

---

## /clear vs. /compact — wann was verwenden

- **Was:** Zwei verschiedene Mechanismen mit verschiedenen Zwecken:
  - `/clear`: Setzt den gesamten Kontext zurück (tabula rasa). Kein Summary — komplett frische Session. Ideal zwischen unabhängigen Aufgaben.
  - `/compact`: Fasst zusammen und setzt fort. Konversations-Geschichte bleibt als Summary. Ideal wenn man die aktuelle Aufgabe weiterverfolgt.
- **Best Practice:** `/clear` bei komplett neuen, unabhängigen Aufgaben verwenden. Wenn Claude eine Korrektur mehr als zweimal nicht übernimmt: mit `/clear` neu starten mit präziserem Prompt. `/compact` bei langen Sessions innerhalb der gleichen Aufgabe.
- **Quelle:** [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell)
- **Stand:** 2026-05-25

---

## Subagents für Kontext-Schutz nutzen

- **Was:** Subagents laufen in einem eigenen, frischen Kontext-Fenster. Alle Tool-Calls eines Subagents (Datei-Reads, Greps, Befehle) belasten den Haupt-Kontext nicht. Am Ende gibt der Subagent nur einen Summary zurück.
- **Best Practice:** Rechercheaufgaben, Sicherheits-Reviews, oder große Codebase-Erkundungen immer als Subagent delegieren. Formulierung: `"Use a subagent to investigate how authentication handles token refresh."` — der Subagent exploriert, der Haupt-Kontext bleibt sauber für die Implementierung.
- **Quelle:** [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell), [code.claude.com/docs/en/how-claude-code-works](https://code.claude.com/docs/en/how-claude-code-works) (offiziell)
- **Stand:** 2026-05-25

---

## /btw — Schnell-Fragen ohne Kontext-Kosten

- **Was:** `/btw <Frage>` zeigt die Antwort in einem overlay-artigen Bereich und schreibt sie nie in die Konversations-History. Nützlich für schnelle Lookup-Fragen (z.B. API-Syntax nachschlagen) mitten in einer Session.
- **Best Practice:** Für Fragen nutzen, die man nicht als Konversations-Kontext braucht. Verhindert unnötige Kontext-Aufblähung durch Neben-Fragen.
- **Quelle:** [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell)
- **Stand:** 2026-05-25

---

## /rewind und Partial-Compaction (Summarize from here)

- **Was:** `Esc + Esc` oder `/rewind` öffnet das Rewind-Menü. Optionen:
  - **Restore**: Stellt Konversation und/oder Code auf einen früheren Checkpoint zurück.
  - **Summarize from here**: Komprimiert Nachrichten ab dem gewählten Punkt — frühere Nachrichten bleiben vollständig erhalten.
  - **Summarize up to here**: Komprimiert frühere Nachrichten, neueste bleiben vollständig erhalten.
- **Best Practice:** Für gezielte Teilkomprimierung nutzen statt immer die ganze Session zu komprimieren. Wenn man nur den "Schlick" aus frühen Turns entfernen will, aber die aktuellen Turns komplett braucht: `Summarize up to here` auf einem frühen Checkpoint.
- **Quelle:** [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell)
- **Stand:** 2026-05-25

---

## CLAUDE.md — was rein soll (und was nicht)

- **Was:** CLAUDE.md wird bei jeder Session geladen und nach jeder Komprimierung neu eingelesen. Das macht es zum einzigen garantiert persistenten Kanal für Regeln.
- **Best Practice — Was rein soll:**
  - Bash-Befehle die Claude nicht erraten kann
  - Code-Style-Regeln die vom Default abweichen
  - Test-Anweisungen und Befehle
  - Architektur-Entscheidungen spezifisch für das Projekt
  - Häufige Fallstricke und nicht-offensichtliches Verhalten
  - Compact Instructions-Sektion
- **Best Practice — Was nicht rein soll:**
  - Was Claude aus dem Code ableiten kann
  - Standard-Sprachkonventionen
  - Detaillierte API-Dokumentation (stattdessen verlinken)
  - Dinge die sich häufig ändern
  - Offensichtliche Praktiken wie "schreib sauberen Code"
- **Faustregel:** Pro Zeile fragen: "Würde Claude einen Fehler machen, wenn diese Zeile fehlt?" Wenn nein: raus. Zu langes CLAUDE.md führt dazu, dass Regeln ignoriert werden.
- **Quelle:** [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell)
- **Stand:** 2026-05-25

---

## Auto Memory (MEMORY.md) — Grenzen und Nutzung

- **Was:** Claude lädt automatisch die ersten 200 Zeilen oder 25 KB der MEMORY.md (je was kleiner ist) beim Session-Start. Alles darüber hinaus wird nicht geladen.
- **Best Practice:** Index-Einträge kurz halten (max. eine Zeile, ~200 Zeichen). Details in verlinkte Topic-Dateien auslagern. Bei >25 KB aktiv beschneiden — neue Sessions "sehen" dann einen abgeschnittenen Index ohne Warnung.
- **Hinweis:** Dieses System hat bereits einen MEMORY.md-Overflow (25,5 KB bei 24,4 KB Limit — laut MEMORY.md-Header aktiv).
- **Quelle:** [code.claude.com/docs/en/how-claude-code-works](https://code.claude.com/docs/en/how-claude-code-works) (offiziell)
- **Stand:** 2026-05-25

---

## Skills — On-Demand-Laden statt Session-Start-Aufblähung

- **Was:** Skills werden nicht vollständig beim Session-Start geladen. Claude sieht nur die Skill-Beschreibungen. Der vollständige Skill-Inhalt wird erst geladen, wenn der Skill aufgerufen wird. Mit `disable-model-invocation: true` kann man sogar die Beschreibungen aus dem Kontext halten, bis der Skill manuell ausgelöst wird.
- **Best Practice:** Domänen-Wissen das nur manchmal relevant ist: als Skill statt in CLAUDE.md. Das schont den Kontext für alle anderen Sessions.
- **Quelle:** [code.claude.com/docs/en/how-claude-code-works](https://code.claude.com/docs/en/how-claude-code-works) (offiziell), [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell)
- **Stand:** 2026-05-25

---

## MCP Tool Definitions — Deferred Loading

- **Was:** MCP Tool-Definitionen werden per Default auf Anfrage geladen (nicht beim Session-Start). Nur die Tool-Namen stehen im Kontext, bis Claude ein bestimmtes Tool tatsächlich nutzt. `/mcp` zeigt die Kosten pro Server.
- **Best Practice:** `/mcp` regelmäßig prüfen, wenn viele MCP-Server konfiguriert sind. Tool-Search (wenn verfügbar) nutzen, um nur relevante Tools zu laden.
- **Quelle:** [code.claude.com/docs/en/how-claude-code-works](https://code.claude.com/docs/en/how-claude-code-works) (offiziell)
- **Stand:** 2026-05-25

---

## Typische Kontext-Fallen (Anti-Patterns)

- **Was:** Bekannte Muster die den Kontext unnötig aufblähen:
  1. **Kitchen-Sink-Session:** Eine Aufgabe anfangen, dann unabhängige Fragen stellen, dann zurück zur ersten. Kontext füllt sich mit irrelevantem Material. **Fix:** `/clear` zwischen unabhängigen Aufgaben.
  2. **Korrekturen stapeln:** Claude macht etwas falsch, man korrigiert, es ist immer noch falsch, man korrigiert wieder. Kontext füllt sich mit fehlgeschlagenen Ansätzen. **Fix:** Nach zwei Fehlkorrekturen `/clear` und mit besserem Prompt neu starten.
  3. **Endlose Exploration:** "Untersuche X" ohne Scope-Einschränkung. Claude liest hunderte Dateien. **Fix:** Scope eng definieren oder Subagent nutzen.
  4. **Zu langes CLAUDE.md:** Wichtige Regeln gehen im Rauschen unter. **Fix:** Regelmäßig beschneiden. Was Claude ohne die Regel richtig macht: raus.
- **Quelle:** [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) (offiziell)
- **Stand:** 2026-05-25

---

## Bekannte Probleme und Regressions (Stand 2026-05)

- **v2.1.92 Regression:** Autocompact-Schwellwert scheinbar auf 400K Token begrenzt bei Opus 4.6 (1M Kontext-Fenster). GitHub Issue #43989 — unbestätigt ob inzwischen behoben.
- **CLAUDE_AUTOCOMPACT_PCT_OVERRIDE ignoriert:** In einigen Sessions wird die Variable scheinbar ignoriert; Kontext überschreitet den konfigurierten Schwellwert ohne Komprimierung (GitHub Issue #36381).
- **Quelle:** [github.com/anthropics/claude-code/issues/43989](https://github.com/anthropics/claude-code/issues/43989) (extern, GitHub), [github.com/anthropics/claude-code/issues/36381](https://github.com/anthropics/claude-code/issues/36381) (extern, GitHub)
- **Stand:** 2026-05-25

---

*Quellen gesamt: 8 (3 offiziell, 5 extern). Status: VOLLSTÄNDIG.*
