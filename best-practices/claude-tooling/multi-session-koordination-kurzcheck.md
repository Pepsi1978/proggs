# Multi-Session-Koordination (parallele CLI-Agenten am selben Repo) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
