# Claude Code Konfiguration & Regeln Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): **Hochrisiko-Bereich (Stufe C)** — vor echter
> Arbeit hier ist der VOLLTEXT Pflicht (`Read` ohne `limit`); dieser Kurzcheck dient nur der
> Schnell-Orientierung. Bei JEDEM Fehler im Bereich gilt ebenfalls Volltext-Pflicht (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Verhalten MUSS immer laufen | Hook statt CLAUDE.md (CLAUDE.md ist advisory) | §1.1 |
| 2 | CLAUDE.md waechst | Ziel < 200 Zeilen, Detail in Rules/Skills (Context-Rot) | §1.2 |
| 3 | Token sparen via `@import` | Spart KEINE Tokens — laedt voll; nur path-scoped/Skill spart | §1.3 |
| 4 | settings.json schreiben | Ein JSON-Fehler ODER BOM killt ALLES still — validieren | §3.1 |
| 5 | Windows: settings/.mcp.json | UTF-8-BOM bricht Parse — BOM-frei speichern | §3.2 |
| 6 | `MEMORY.md` pflegen | Nur erste ~200 Zeilen / 25 KB laden — Index kurz | §6.1 |
| 7 | User-Rule mit `paths:` | NIE `paths:` user-level (ignoriert) | §2.1 |
| 8 | path-scoped Rule | Triggert nur bei Read, nicht Write; Globs quoten | §2.2 |
| 9 | Skill mit `paths:` | ENTFERNEN — macht Skill undiscoverable | §4.1 |
| 10 | Skill `description` | Einzeilig in Quotes, Trigger front-loaden | §4.2 |
| 11 | Subagent braucht Kontext | Erbt CLAUDE.md/Rules nicht — per Hook injizieren | §2.4 |
| 12 | Custom-Agent starten | `general-purpose` + Prompt, nie Custom-`subagent_type` | §5.1 |
| 13 | `allow`-Liste setzen | Keine Whitelist — Sperren nur via `deny` | §3.6 |
| 14 | Config-/Skill-Datei (Win) | LF halten (CRLF bricht Edit-Tool) | §8.1 |
| 15 | Subagent crasht (0 Token) | `ENABLE_TOOL_SEARCH` + `tools:`-Whitelist | §5.4 |
| 16 | Login wird staendig neu verlangt | Pro `CLAUDE_CONFIG_DIR` eigener Schluesselbund-Eintrag — `claude-login-sync` spiegelt | §3.9 |
