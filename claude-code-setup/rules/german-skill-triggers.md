# Deutsche Trigger-Map fuer Skills und Plugins (KRITISCH)

Frank spricht Deutsch (oft via Whisper). Diese Regel uebersetzt deutsche Anfragen in die
richtigen (meist englischen) Skills/Agents. **PFLICHT:** Bei JEDER Anfrage diese Map mental
pruefen — schon bei 1 % Wahrscheinlichkeit den passenden Skill aufrufen. Die Trigger zeigen auf
den Skill der WIRKLICH das Gewuenschte tut, nicht auf den aehnlich klingenden.

---

## 1. Git, Commit & Undo

| Deutsche Phrase | Skill |
|-----------------|-------|
| "committe", "mach ein Commit" | `commit-commands:commit` (nur lokal) |
| "committe und pushe", "ab damit", "PR machen" | `commit-commands:commit-push-pr` |
| "rückgängig", "undo", "revert" | `undo-changes` (git revert, nie force) |
| "alte Branches löschen" | `commit-commands:clean_gone` |
| "Worktree", "isoliert arbeiten" | `git:worktrees` |

"pushe das" (ohne PR) → kein Skill, direkt `git push`.

## 2. Uebersetzung & Strings (Android — SEHR wichtig)

| Deutsche Phrase | Skill |
|-----------------|-------|
| "Strings finden", "hardcodierte Strings", "i18n Audit" | `string-extraktor` (VORSTUFE, erstellt dt. Strings) |
| "übersetze die Strings", "Strings übersetzen", "mehrsprachig", "Lokalisierung", "i18n", "internationalisieren" | `uebersetzung` (strings.xml in alle Sprachen) |
| "nur neue Strings übersetzen" / "alle übersetzen" / "auf [Sprache] übersetzen" | `uebersetzung` |

Whisper: "Übersetzung"/"Translation"/"Translate"/"i18n"/"Lokalisierung" → `uebersetzung`.
NICHT verwechseln: `string-extraktor` ERSTELLT dt. Originale, `uebersetzung` KONSUMIERT sie.

## 3. Debugging & Fehler

| Deutsche Phrase | Skill |
|-----------------|-------|
| "finde den Bug", "funktioniert nicht", "warum geht das nicht" | `superpowers:systematic-debugging` (bekannter Bug) |
| "Tiefen-Debugging", "debugge die App", "alle Bugs/Logikfehler suchen", "Performance-Debugging" | `tiefen-debugging` (ganze App/Modul) |
| "Root Cause", "5x Warum" | `kaizen:why` |
| "was ist kaputt", "Analyse" (kein bekannter Bug) | `tool-check` |

## 4. Recherche & Wissen

| Deutsche Phrase | Skill |
|-----------------|-------|
| "recherchiere", "such im Web", "finde heraus" | `research` (Protokoll: Empfehlung + Frage 1 A/B/C/D) |
| "Best-Practices", "was ist neu in [Kotlin/Swift/…]" | `best-practices` |
| "Bugs für X recherchieren", "Bug-Almanach anlegen" | `bug-almanach-recherche` |
| "was sagt die Doku" | context7 MCP (`resolve-library-id` → `query-docs`) |

## 5. Second Brain / Cortex

| Deutsche Phrase | Skill |
|-----------------|-------|
| "Cortex Update", "synchronisiere ins Gehirn", "Gehirn-Sync", "was ist neu fürs Gehirn" | `cortex-update` |

## 6. Code Review (richtig waehlen)

| Deutsche Phrase | Skill |
|-----------------|-------|
| "reviewe den PR", "PR Review" (GitHub-PR) | `code-review:code-review` |
| "prüfe meine Änderungen" (lokal, kein PR) | `pr-review-toolkit:review-pr` |
| "CodeRabbit" | `coderabbit:coderabbit-review` |
| "zweite Meinung", "was sagt Codex/Gemini" | `second-opinion` |

## 7. Selbstverbesserung & Umgebung

| Deutsche Phrase | Skill |
|-----------------|-------|
| "verbessere dich", "self-improve", "mach mich intelligenter" | `self-improve` (nur manuell) |
| "prüfe die Umgebung", "Environment Check" | `env-checker` Agent |
| "konfiguriere", "Settings ändern", "Hook einrichten" | `update-config` |

## 8. Skill- & Hook-Erstellung

| Deutsche Phrase | Skill |
|-----------------|-------|
| "erstelle einen Skill", "Skill verbessern" | `skill-creator:skill-creator` |
| "erstelle/fixe einen Hook", "Hook bauen" | `hook-forge` (ZUERST, vor jedem Hook-Edit) |
| "erstelle ein Plugin" | `plugin-dev:create-plugin` |

## 9. Session-Backup/Restore (nur auf Franks Ansage)

| Deutsche Phrase | Skill |
|-----------------|-------|
| "starte session backup", "sichere die Session" | `session` (Modus BACKUP) |
| "starte session restore", "mach weiter wo wir waren" | `session` (Modus RESTORE) |

## 10. Aufgaben-Bruecke & Ledger (Konto-Wechsel)

| Deutsche Phrase | Skill |
|-----------------|-------|
| "Konto gewechselt mache weiter", "letzte Aufgabe fortsetzen", "was war offen", "wir machen weiter" | `aufgaben-bruecke` |
| "zeig die offenen Aufgaben", "Ledger zeigen", "Aufgaben-Tabelle" | `aufgaben-visualizer` |

Bei "weiter"/"fortsetzen" IMMER `aufgaben-bruecke`, nicht visualizer.
Whisper: "Ledger"/"Letscher" → visualizer; "Aufgabenbruecke"/"Brückenagent" → bruecke.

## 11. Weitere Skills

| Deutsche Phrase | Skill |
|-----------------|-------|
| "Android App", "Kotlin/Compose" | `android-dev` |
| "finde einen Sound", "Sound-Effekt" | `sound-search` |
| "portiere X zu cowork", "cowork-tauglich machen" | `cowork-portierung` (Muss-Bedingung) |
| "baue eine Webseite", "Web-UI" | `frontend-design:frontend-design` |

---

## Whisper Speech-to-Text Korrekturen (wichtigste)

| Whisper hoert | Gemeint ist |
|---------------|-------------|
| "Cloud" | **Claude** |
| "Self improve" | `self-improve` |
| "Tool check" | `tool-check` |
| "Brainstorm" | `superpowers:brainstorming` |
| "Code Rabbit" | `coderabbit:coderabbit-review` |
| "Reflektion"/"Reflect" | `claude-reflect:reflect` (lernen) oder `reflexion:reflect` (bewerten) — nachfragen |

---

## Proaktive Agents (laufen automatisch — NICHT manuell triggern)

| Agent | Wann / Was |
|-------|-----------|
| `code-simplifier` | nach Edit/Write — vereinfacht geaenderten Code |
| `auto-verify-iterate` | nach Coding-Aufgabe — 5-Schritt-Verifikation |
| `auto-format` | nach Edit/Write — formatiert Dateien |
| `hyperagent-stop` | Stop-Event (>5 Turns) — metacognitive Selbstanalyse |
| `session-scorer` | SessionEnd — schreibt Session-Metriken in JSONL |
