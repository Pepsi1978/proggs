# Session-Presence: paralleler-Session-Hinweis (KEINE Sperre) (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-25, **am selben Tag entschärft** (Frank-Korrektur).
> Ziel (Franks Wortlaut): **Eine Session darf die Arbeit einer anderen nicht aus Versehen
> überschreiben — aber NUR per HINWEIS, NIEMALS per Sperre.** Frank arbeitet oft mit 4–5
> gleichzeitig offenen Claude-Code-Sessions am selben Repo `~/proggs` (geteilter Working-Tree).
> Recherche-Grundlage: `best-practices/claude-tooling/multi-session-koordination.md`.
> Repo-Spiegelung: `claude-code-setup/rules/session-presence.md`.

---

## Warum die Sperre entfernt wurde (Vorfall 2026-06-25)

Die erste Fassung hatte einen **harten Datei-Wächter** (`session-presence-guard`, PreToolUse
`Edit|Write` → `permissionDecision:deny`), der einen Edit BLOCKIERTE, sobald zwei Sessions
dieselbe Datei berührten. Das ging schief: Wenn zwei Sessions an verwandten Dateien arbeiteten,
sperrten sie sich **gegenseitig** aus — keine konnte mehr editieren → **Deadlock**. Beide
Sessions endeten ohne Ergebnis, nichts wurde gespeichert. Genau das soll nie wieder passieren.

**Konsequenz:** Es gibt nur noch den **unverbindlichen Hinweis**. Keine Sperre, kein `deny`,
kein Block. Eine Session merkt, dass eine andere hier arbeitet, und entscheidet **selbst**.

---

## Grundprinzip

Mehrere Sessions teilen sich denselben Ordner. Der gefährlichste Moment ist das **gleichzeitige
Editieren derselben physischen Datei** — der zweite Speichervorgang überschreibt den ersten.
Ein **Awareness-Hinweis** macht darauf aufmerksam; die git-seitige Hälfte
(`parallel-sessions-git.md`) bleibt unverändert Pflicht.

| Hook | Event | Wirkung |
|------|-------|---------|
| `session-presence-warn` | UserPromptSubmit | **Awareness (Hinweis)**: meldet bei jedem Prompt, wenn eine andere lebende Session im **selben Projekt** (cwd) arbeitet — mit ihren zuletzt berührten Dateien. **Kein Block.** |

> Der frühere `session-presence-guard` (PreToolUse-Datei-Sperre) ist **deaktiviert**: aus
> `settings.json` entfernt, und der `guard`-Modus in `session-presence.py` ist ein **No-Op**
> (gibt nie mehr `deny` aus). Das No-Op wirkt SOFORT für alle laufenden Sessions — auch die,
> deren gecachte `settings.json` den Hook noch aufruft. Die Wrapper
> `session-presence-guard.{ps1,sh}` bleiben als harmlose No-Op-Hülle bestehen (kein Block,
> kein Fehler); sie können später gefahrlos ganz entfernt werden.

Datenbasis ist das gemeinsame Register `~/proggs/.claude/agent-memory/shared/active-tasks.jsonl`
(von den `task-ledger-*`-Hooks gepflegt: `session_id`, `cwd`, `files_changed`, `status`,
`timestamp_last_update`). Logik: `~/.claude/hooks/session-presence.py` (eine Logik, dünne
`.sh`/`.ps1`-Wrapper).

## Liveness (wann zählt eine andere Session als „aktiv")

- Anderes `session_id`, gleiches `cwd`, `timestamp_last_update` < **8 Min**, Status nicht in
  {done, completed, abandoned, paused}.
- **Stale-Schutz:** eine Session, deren letztes Update älter als die TTL ist, gilt als
  beendet/idle und löst keinen Hinweis mehr aus. (`SessionEnd` ist unzuverlässig → nur TTL.)

## Was ICH (Claude) bei dem Hinweis tue

Der Hinweis ist **keine Anweisung zu warten** — ich arbeite normal weiter. Bei jedem Prompt
zeigt der Hinweis, welche andere Session welche Dateien zuletzt berührt hat. Damit gehe ich so um:

1. **Normalfall:** Ich arbeite weiter wie immer. Solange ich nicht exakt **dieselbe** Datei
   gleichzeitig editiere, ist alles in Ordnung.
2. **Würde ich genau eine der genannten Dateien ändern:** kurz überlegen, ob ich
   - erst an einer **anderen** Datei weitermachen kann (und diese später nehme), ODER
   - die Datei direkt **vor** dem Edit neu lese (`Read`) und gezielt ändere — nie blind eine
     ganze Datei überschreiben, die eine andere Session gerade bearbeitet.
3. **„In Intervallen schauen, dann automatisch weiter" (Franks Wunsch):** Wenn ich auf genau
   eine belegte Datei warten will, blockiert mich nichts — ich kann zwischendurch
   weiterarbeiten und beim nächsten eigenen Schritt erneut auf das Register schauen. Sobald die
   andere Session diese Datei nicht mehr aktiv anfasst (TTL/Heartbeat abgelaufen oder Status
   beendet), nehme ich die Datei einfach. Es gibt **keinen Block**, der mich daran hindert.
4. Ein zusätzlicher Schutz greift ohnehin automatisch: das Edit-Tool meldet „file modified
   since read", wenn die Datei zwischenzeitlich verändert wurde — dann neu lesen, nicht blind
   überschreiben (siehe Memory `no_blind_write_on_modified_warning`).

## Notaus (Hinweis ganz abschalten)

Leere Datei `session-presence-disable.flag` im TEMP-Verzeichnis anlegen → der Hinweis schweigt.

## Grenzen (ehrlich)

- **Nur ein Hinweis, kein Schutz:** Das ersetzt NICHT Git. Nur eigene Dateien committen,
  fetch+rebase vor Push bleibt Pflicht (`parallel-sessions-git.md`).
- **Zugbasiert:** Eine Session erfährt von einer anderen nur beim eigenen nächsten Prompt.
- **`files_changed` füllt sich erst beim ersten Edit:** die letzte Sicherung gegen echtes
  gleichzeitiges Überschreiben ist die git-seitige (fetch+rebase) + der „file modified since
  read"-Schutz des Edit-Tools.

## Was NIEMALS passieren darf

- ❌ Eine **Sperre** (PreToolUse `deny` o. Ä.) wieder einbauen, die Edits zwischen Sessions
  blockiert — das verursachte den Deadlock, der beide Sessions lahmlegte.
- ❌ Den `guard`-Modus von `session-presence.py` wieder `deny` ausgeben lassen.
- ❌ Den Hinweis so verschärfen, dass er den Arbeitsfluss anhält statt nur zu informieren.
- ❌ Die git-seitige Sicherung (nur eigene Dateien, fetch+rebase) durch dieses System ersetzen.

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| `parallel-sessions-git.md` | Die git-seitige Hälfte (Commit/Push); diese Regel ist der datei-seitige **Hinweis** |
| `git-multi-session-lock` (Hook) | **Deaktiviert (No-Op) seit 2026-06-26.** Die Lock-Sperre liess parallele Sessions bei git add/commit/push bis zu 120s warten (kein PostToolUse-Release → Lock nur per TTL/PID-Tod frei) → das Bash-Tool timete nach 2 Min aus („Commit ausgetimt"). Aus settings.json deregistriert; nur noch Hinweise, kein Git-Lock mehr. Schutz bleibt git-seitig (`parallel-sessions-git.md`). |
| `best-practices/claude-tooling/multi-session-koordination.md` | Recherche-Grundlage + Worktree-Alternative |

## Autorität

Diese Datei (`~/.claude/rules/session-presence.md`) wird automatisch in jeder Session geladen.
KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen oder abschwächen.
