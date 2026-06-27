# 🐛 Claude Code — Desktop-App (Code-Tab) vs. CLI Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Code-Tab startet auf Windows nicht ("Git is required") | Git for Windows installieren → App neu starten | §A1 |
| 2 | Push scheitert: "Git LFS is required" / pre-push-Hook | git-lfs installieren + `git lfs install`; nur Markdown-Commit notfalls `--no-verify` | §A2 |
| 3 | Tool "nicht gefunden" obwohl installiert | Neues Terminal / App komplett neu starten (PATH-Snapshot veraltet) | §B1 |
| 4 | Build/Server scheitert an fehlender Env-Variable | Variable in den **lokalen Environment-Editor** (Local → Zahnrad), NICHT ins Shell-Profil | §B2 |
| 5 | macOS: npm/node/brew nicht gefunden beim Dock-Start | PATH (inkl. `/opt/homebrew/bin`) im Environment-Editor setzen oder Tools mit absolutem Pfad | §B3 |
| 6 | Hook (PostToolUse/SessionStart) feuert in der App nicht | Für hook-kritische Arbeit in die **CLI**; Hook-Befehl mit absoluten Pfaden | §C1 |
| 7 | Jeder Tool-Call fragt trotz bypassPermissions | Desktop liest `defaultMode` nicht zuverlässig → CLI mit `--dangerously-skip-permissions` | §D1 |
| 8 | MCP-Server verbindet nicht / Hammer-Icon fehlt | Absolute Pfade in der Config, App neu starten; im Zweifel MCP über die CLU | §E1 |
| 9 | "Wo sind meine Änderungen?" / `.claude/worktrees/` wuchert | Jede Session hat eigenen Worktree; `.claude/worktrees/` in `.gitignore`, committen+pushen | §F1 |
| 10 | Computer Use tut nichts trotz Toggle (macOS) | Accessibility **und** Screen Recording erteilen, dann App neu starten; nach Update neu erteilen | §G1 |
| 11 | Preview blockt `http://localhost:PORT` | Build-Regression — Link in Safari öffnen / auf Patch warten; Server-Config NICHT ändern | §H1 |
| 12 | Cloud-Session ignoriert CLAUDE.md/Skills/MCP, kein Terminal | Kontext-/Skill-/Terminal-Arbeit nur in **lokaler** (oder SSH-)Session | §I1 |
| 13 | `/agents` `/doctor` `/config` `/permissions` → "isn't available" | Settings-Datei direkt editieren oder Befehl in der eigenständigen CLI | §J3 |
| 14 | Skript/CI/Pipe gewünscht (`-p`, `--output-format`) | Geht im Desktop NICHT — bewusst in die CLI/Agent SDK wechseln | §J1 |
| 15 | macOS: "Claude is damaged" / schwarzer Hauptbereich | Notarisierungs-/Asar-Problem; App updaten, sonst CLI; `xattr` hilft oft nicht | §A8 |
