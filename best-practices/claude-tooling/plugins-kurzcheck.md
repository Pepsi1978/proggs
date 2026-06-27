# Plugins Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | externer Plugin-/MCP-Code | vor Installation komplett lesen + scannen (Prompt-Injection) | Sicherheit |
| 2 | plugin.json | vollstaendiges Manifest-Schema einhalten; validieren vor Veroeffentlichung | Manifest-Struktur |
| 3 | Versionen | SHA-Pinning fuer Reproduzierbarkeit; `claude plugin update` zieht ggf. stale Clone | Versions-Management |
| 4 | command-Hooks im Plugin | Pre/PostToolUse werden gedroppt → in user-`settings.json` definieren | Komponenten |
| 5 | Auslieferung | `defaultEnabled:false` fuer opt-in (v2.1.154) | defaultEnabled |
| 6 | `.claude/skills` | auto-geladen ohne Marketplace (v2.1.157) | Auto-Loading |
| 7 | .sh-Hooks im Plugin | verlieren `+x` bei Sync → `git update-index --chmod=+x` | Sicherheit |
