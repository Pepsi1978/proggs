# Multi-Session-Koordination (parallele CLI-Agenten am selben Repo) — Best Practices

> **Zweck:** Wie verhindert man, dass mehrere gleichzeitig laufende Claude-Code-Sessions (oder
> andere CLI-Coding-Agenten) im SELBEN Git-Repo/Working-Tree sich gegenseitig die Arbeit
> **versehentlich ueberschreiben**? Recherchiert 2026-06-25 (Firecrawl+MiniMax-Schwarm, 5 Researcher).
> Quellen-Label `offiziell` (Anthropic-Empfehlung) / `extern` (OSS-Tools, Blogs).
>
> **Stand:** 2026-06-25, Claude Code v2.1.191. Bezug auf bestehendes lokales System:
> `active-tasks.jsonl` + `task-ledger-*`-Hooks + `git-multi-session-lock` + Regel
> `parallel-sessions-git.md`. Gegenstueck (Fallen): siehe Abschnitt 6.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) |
|---|-----------|--------------------------|
| 1 | ⭐ Sauberste Loesung | **Git-Worktrees** — jede Session eigenes Arbeitsverzeichnis, gemeinsamer Git-Store, KEINE Datei-Kollision (Anthropic-empfohlen). Aendert aber den Workflow (separate Ordner). |
| 2 | ⭐ Gleicher Working-Tree (Franks Stil) | **Dateibasiertes Session-Board**: jede Session registriert sich, Lock pro Datei, Awareness beim Prompt. Vorbild: OSS `claude-sessions-board`. |
| 3 | ⭐ Lock-Hook | **NIE warten im Hook** → `permissionDecision:deny` + Begruendung (nennt Lock-Halter). Warten = Hook-Timeout + Deadlock-Gefahr. |
| 4 | ⭐ Stale-Erkennung | **3 Bedingungen** statt nur Zeit: `pid_alive` + Prozess-Startzeit (`ps -o lstart=`) + `expires_at` (TTL). Verhindert PID-Reuse-Fehler. |
| 5 | Cleanup | `SessionEnd` NICHT vertrauen (greift nicht bei `kill -9`) → nur **TTL + Lazy GC** (z.B. Sessions 30 min, Locks 10 min). |
| 6 | Atomares Schreiben | Registry/Lock-Dateien immer **tmp-Datei → rename** (POSIX-atomar), nie in-place. |
| 7 | OS-File-Locks (`flock`/`fcntl`) | Fuer Agenten **nutzlos** — sie sind *advisory* (Claudes Edit-Tool prueft sie nicht) + `st_ino`-Probleme auf Windows. NICHT darauf bauen. |
| 8 | Schon vorhandener Schutz | Claudes Edit-Tool macht **optimistic concurrency**: weigert sich zu schreiben, wenn die Datei seit dem letzten Read geaendert wurde („file modified since read"). Das faengt direktes Ueberschreiben teilweise schon ab. |
| 9 | Git-Ebene | Bleibt Pflicht: nur eigene Dateien namentlich stagen, `fetch+rebase` vor Push, nie force-push (`parallel-sessions-git.md`). |

---

## 1. Das Kernproblem

Mehrere Claude-Code-Sessions im selben Working-Tree „edit the same files without realizing the
other's existence, causing one side's work to be completely erased" `extern`. Der gefaehrlichste
Punkt ist **nicht** der Commit, sondern das gleichzeitige **Editieren derselben physischen Datei** —
der zweite Speichervorgang ueberschreibt den ersten, noch bevor Git involviert ist. Weitere
Symptome: korrupte Lockfiles bei parallelem `npm install`, Dev-Server-/Compiler-Crashes durch
Dateien, die mitten im Read verschwinden, Context-Noise.

## 2. Loesungsebene A — Git-Worktrees (physische Isolation, Anthropic-empfohlen)

Anthropic empfiehlt fuer parallele Arbeit **separate checkouts oder `git worktree`** `offiziell`/`extern`.
Jeder Worktree = eigenes Arbeitsverzeichnis aus demselben Repo, eigener Branch, **keine Merge-Konflikte
zwischen Worktrees**, unabhaengige Dependencies. Muster:
```bash
git worktree add ../<repo>-<feature> <branch>
git worktree list
git worktree remove <path>   # Cleanup
```
- Naming `../projekt-typ-beschreibung` (nicht `../temp`).
- **Grenzen:** Gemeinsame Dateien (`package.json`, CI-Config, `.env.example`) existieren nur einmal →
  kollidieren beim Merge. Zusaetzlich oft **DB-Branching + Port-Isolation** noetig, sonst kollidieren
  Agenten auf DB-/Port-Ebene trotz Datei-Isolation.
- **Wann NICHT:** wenn man bewusst im selben Ordner arbeitet (Frank) — dann Ebene B.

## 3. Loesungsebene B — Dateibasiertes Session-Board (gleicher Working-Tree)

Vorbild: das OSS-Tool **`claude-sessions-board`** (`extern`, github.com/grinzing/claude-sessions-board) —
loest exakt das Problem ohne Worktree-Wechsel. Mechanik:
- **Auto-Registrierung** jeder Session beim Start.
- **SessionStart-Uebersicht**: zeigt andere aktive Sessions + ungelesene Nachrichten.
- **`lock <datei>`** → blockt andere Sessions per **PreToolUse-Hook** mit Begruendung (nennt den Lock-Halter).
- **`send <session_id> "<text>"`** → asynchrone Nachrichten; Empfaenger sieht sie beim naechsten Prompt
  (UserPromptSubmit-Hook).

Bewusste, uebernehmenswerte Design-Entscheidungen `extern`:

| Entscheidung | Begruendung |
|--------------|-------------|
| Komplett dateibasiert (kein DB/Daemon) | Einfache Installation, kein resident Prozess |
| Lazy GC mit TTL (Sessions 30 min, Locks 10 min) | Kein Cron/Daemon noetig |
| **3-Bedingungs-Stale-Detection** (`pid_alive` + `ps -o lstart=` + `expires_at`) | Verhindert PID-Reuse-Fehler |
| **Atomic write** (tmp → rename im selben Verzeichnis) | POSIX-Atomizitaet |
| Inbox-„read"-Status via Rename statt Flag | Vermeidet Read/Write-Contention |
| **Locks WARTEN NICHT, sondern `deny + reason`** | Vermeidet Hook-Timeouts und Deadlocks |
| `SessionEnd` NICHT vertrauen (kein Aufruf bei `kill -9`) | Cleanup nur via TTL + Lazy GC |

**Alternativen:** GitButler (Hook-basiert, auto-Branch + Commit pro Session via Session-ID — kein
Worktree-Bootstrap noetig) `extern`; offizielle **Agent Teams** (Mailbox `~/.claude/teams/{team}/inboxes/`,
Task-Claiming via Lock-Files) — aber „heavy" fuer beilaeufige Alltags-Sessions.

## 4. Loesungsebene C — Optimistic Concurrency (teils schon im Harness)

„Optimistic": lesen → Version notieren (Versionsnummer/Timestamp/**Hash**) → vor dem Schreiben pruefen,
ob sich die Version geaendert hat → bei Konflikt abbrechen + neu lesen `extern`. Faustregel: <10 %
Konfliktrate → optimistic gewinnt; >50 % → pessimistic (sperren) `extern`. Anti-Pattern: pessimistischen
Lock ueber eine ganze UI-/Agenten-Session halten (blockt alle anderen).

**Wichtig:** Claudes **Edit-Tool macht das bereits** — es lehnt einen Schreibvorgang ab, wenn die Datei
seit dem letzten Read veraendert wurde („file modified since read", lokal als
`feedback_no_blind_write_on_modified_warning`). Das ist optimistic concurrency auf Datei-Ebene und faengt
das *direkte* Ueberschreiben im selben Tree teilweise schon ab. Ein Session-Board ergaenzt die **proaktive
Awareness** (wissen, BEVOR man anfaengt, statt erst beim Schreib-Konflikt).

## 5. Empfohlene Architektur fuer Franks Setup (Synthese)

Da Frank bewusst mehrere Sessions im selben `~/proggs` faehrt: **dateibasiertes Board auf der schon
vorhandenen `active-tasks.jsonl`** (kein fremder Code noetig — Datenbasis existiert: `session_id`, `cwd`,
`files_changed`, `status`, `timestamp_last_update`, `pushed`):
1. **Awareness-Hook** (UserPromptSubmit): warnt, wenn eine andere lebende Session im selben Projekt arbeitet.
2. **Datei-Waechter** (PreToolUse `Edit|Write`): wenn die Zieldatei in `files_changed` einer anderen
   lebenden Session steht → **`permissionDecision:deny` + Begruendung** (kein Warten).
3. **Liveness** via 3-Bedingungs-Stale-Check (mind. `timestamp_last_update`-TTL; PID/Startzeit wo verfuegbar).
4. **Git-Ebene** unveraendert (nur eigene Dateien, fetch+rebase) — das Board ist die *datei-seitige* Haelfte.

## 6. Was NICHT funktioniert / Fallen

- **OS-Advisory-Locks (`flock`/`fcntl`/`lockf`)**: Claudes Edit-Tool respektiert sie nicht → nutzlos gegen
  Agenten-Ueberschreiben. Zusaetzlich nicht zuverlaessig ueber NFS/SMB; `st_ino` auf Windows = 0
  (Inode-Vergleich gegen Stale-Race bricht). `extern`
- **Warten im Hook** („blockiere, bis die andere Session fertig ist"): fuehrt zu Hook-Timeouts und
  **Deadlocks** (beide warten). Stattdessen `deny + reason`; das Warten — falls ueberhaupt — macht der
  Agent AUSSERHALB des Hooks. `extern`
- **`exit 2` zum Blocken von Edit/Write**: greift dort laut Hooks-Almanach nicht zuverlaessig → fuer
  Edit/Write IMMER `permissionDecision:deny` (JSON, `exit 0`). Siehe `bugs/claude-tooling/claude-hooks.md` §1.6.
- **`SessionEnd` als Cleanup-Trigger**: unzuverlaessig (kein Aufruf bei Crash/`kill -9`) → nur TTL.
- **Stale-Lock durch reinen Timestamp-TTL**: PID-Reuse kann eine tote Session „lebendig" erscheinen
  lassen → 3-Bedingungs-Check.

---

## Quellen
- Anthropic-Empfehlung „separate checkouts / git worktree" fuer parallele Sessions `offiziell`/`extern` (Blog/Community, recherchiert 2026-06-25)
- `claude-sessions-board` (github.com/grinzing/claude-sessions-board) — dateibasiertes Koordinations-Board `extern`
- GitButler (Scott Chacon) — Hook-basiertes Auto-Branching pro Session `extern`
- apenwarr.ca „Everything you never wanted to know about file locking"; unix.stackexchange (stale flock) `extern`
- StackOverflow „Optimistic vs. Pessimistic locking"; crackingwalnuts.com (concurrency control) `extern`
- dev.to (OT vs CRDT); josephg.com (CRDTs) — fuer Echtzeit-Kollaboration, fuer uns overkill `extern`
- Bezug: `bugs/claude-tooling/claude-hooks.md` (§1.6 deny statt exit 2), `parallel-sessions-git.md`, `active-tasks.jsonl`
