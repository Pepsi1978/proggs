# Session-Presence: parallele Sessions überschreiben sich nicht (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-25. Gilt AUTOMATISCH in JEDER Session.
> Ziel (Franks Wortlaut): **Eine Session darf die Arbeit einer anderen Session nicht aus
> Versehen überschreiben.** Frank arbeitet oft mit 4–5 gleichzeitig offenen Claude-Code-Sessions
> am selben Repo `~/proggs` (geteilter Working-Tree). Recherche-Grundlage:
> `best-practices/claude-tooling/multi-session-koordination.md` (Firecrawl-Recherche 2026-06-25).
> Repo-Spiegelung: `claude-code-setup/rules/session-presence.md`.

---

## Grundprinzip

Mehrere Sessions teilen sich denselben Ordner. Der gefährlichste Moment ist **nicht** der Commit,
sondern das **gleichzeitige Editieren derselben physischen Datei** — der zweite Speichervorgang
überschreibt den ersten. Zwei automatische Hooks schützen davor (datei-seitig); die git-seitige
Hälfte (`parallel-sessions-git.md`) bleibt unverändert Pflicht.

| Hook | Event | Wirkung |
|------|-------|---------|
| `session-presence-warn` | UserPromptSubmit | **Awareness**: warnt bei jedem Prompt, wenn eine andere lebende Session im **selben Projekt** (cwd) arbeitet — mit ihren zuletzt berührten Dateien. Kein Block. |
| `session-presence-guard` | PreToolUse `Edit\|Write` | **Datei-Wächter**: lehnt den Edit ab (`permissionDecision:deny`), wenn die Zieldatei GERADE von einer anderen lebenden Session bearbeitet wird (Datei steht in deren `files_changed`). |

Datenbasis ist das gemeinsame Register `~/proggs/.claude/agent-memory/shared/active-tasks.jsonl`
(von den `task-ledger-*`-Hooks gepflegt: `session_id`, `cwd`, `files_changed`, `status`,
`timestamp_last_update`). Logik: `~/.claude/hooks/session-presence.py` (eine Logik, dünne
`.sh`/`.ps1`-Wrapper). Begründung „Edit/Write per `deny`-JSON, nicht `exit 2`": `bugs/claude-tooling/claude-hooks.md` §1.6.

## Liveness (wann zählt eine andere Session als „aktiv")

- **Awareness-Warnung:** anderes `session_id`, gleiches `cwd`, `timestamp_last_update` < **8 Min**,
  Status nicht in {done, completed, abandoned, paused}.
- **Datei-Wächter (deny):** zusätzlich strenger — `timestamp_last_update` < **6 Min** (nur bei
  FRISCH aktiver anderer Session blockieren) UND die Zieldatei exakt in deren `files_changed`.
- **Stale-Schutz:** eine Session, deren letztes Update älter als die TTL ist, gilt als beendet/idle
  und blockiert nichts mehr — der Block fällt also **automatisch** weg, sobald die andere Session
  fertig ist oder pausiert. (`SessionEnd` ist unzuverlässig → nur TTL, kein Cleanup-Trigger.)

## Was ICH (Claude) bei einem `deny` des Datei-Wächters tue

Der Hook **wartet nie** (das verursacht Hook-Timeouts/Deadlocks — bewusste Design-Entscheidung).
Das „Warten" übernehme ICH außerhalb des Hooks. Bei einem Überschreib-Schutz-`deny`:

1. **Bevorzugt:** an einer **anderen** Datei weiterarbeiten und diese Datei später erneut versuchen
   — der Block verschwindet von selbst, sobald die andere Session fertig/inaktiv ist (TTL).
2. **Wenn die Datei dringend ist:** kurz warten (ein paar Sekunden bis ~1–2 Min) und es erneut
   versuchen; die andere Session läuft via Heartbeat ab. Frank informieren, dass ich auf die andere
   Session warte.
3. **Wenn ich sicher bin, dass kein echter Konflikt besteht** (z. B. die andere Session hat die
   Datei nur gelesen): die Datei zuerst **neu lesen** (`Read`), dann gezielt ändern — nie blind
   über den Block hinweg dieselbe Stelle überschreiben.
4. **Niemals** den Block reflexhaft per Notaus-Flag umgehen, nur um weiterzukommen — das Flag ist
   für echte Fehlalarme.

## Notaus (bei Fehlalarm)

Leere Datei `session-presence-disable.flag` im TEMP-Verzeichnis anlegen → beide Hooks schalten
sofort ab. Bewusste Geste bei echtem Fehlalarm, nicht reflexhaft.

## Grenzen (ehrlich)

- **Zugbasiert:** Eine Session erfährt von einer anderen nur beim eigenen nächsten Schritt (Prompt
  bzw. Edit) — kein Live-Push während stillem Warten.
- **`files_changed` füllt sich erst beim ersten Edit:** Fangen zwei Sessions exakt gleichzeitig
  dieselbe, noch unberührte Datei an, greift erst die git-seitige Sicherung (fetch+rebase) bzw. der
  eingebaute „file modified since read"-Schutz des Edit-Tools (optimistic concurrency).
- **Ersetzt nicht Git:** Das ist eine Frühwarnung/ein Lock auf Datei-Ebene, kein Git-Ersatz. Nur
  eigene Dateien committen, fetch+rebase vor Push bleibt Pflicht.

## Was NIEMALS passieren darf

- ❌ Bei einem Überschreib-Schutz-`deny` die Datei trotzdem blind überschreiben
- ❌ Den Hook so umbauen, dass er im Hook WARTET (Deadlock/Timeout-Gefahr) — deny+reason bleibt
- ❌ Das Notaus-Flag reflexhaft setzen, nur um den Block loszuwerden
- ❌ Die git-seitige Sicherung (nur eigene Dateien, fetch+rebase) durch dieses System ersetzen

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| `parallel-sessions-git.md` | Die git-seitige Hälfte (Commit/Push); diese Regel ist die datei-seitige |
| `git-multi-session-lock` (Hook) | Sperrt den Git-Index bei parallelen Commits; ergänzt diese Regel |
| `best-practices/claude-tooling/multi-session-koordination.md` | Recherche-Grundlage + Worktree-Alternative |
| `bugs/claude-tooling/claude-hooks.md` §1.6 | Warum Edit/Write per `deny`-JSON, nicht `exit 2` |

## Autorität

Diese Datei (`~/.claude/rules/session-presence.md`) wird automatisch in jeder Session geladen.
KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen oder abschwächen.
