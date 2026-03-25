# Mirror Ledger — Universal Cross-Platform & Cross-CLI Sync
<!-- VERSION: 1 -->
<!-- FORMAT: universal-mirror-bridge-v1 -->
<!-- CREATED: 2026-03-25 -->

## Was ist diese Datei?

Diese Datei ist das **zentrale Austausch-Ledger** fuer ALLE Programmierumgebungen.
Jedes CLI-Tool (Claude Code, Codex CLI, Gemini CLI) und jede Plattform (macOS, Windows)
schreibt hier rein was geaendert wurde und liest was die anderen geaendert haben.

**Workflow:**
1. `export` Agent schreibt neue Eintraege am Ende einer Session
2. `import` Agent liest ausstehende Eintraege beim Start auf einer anderen Plattform/CLI
3. Jeder Eintrag ist EXTREM ausfuehrlich und selbsterklaerend — keine Vorkenntnisse noetig

**Regeln:**
- Jeder Eintrag hat eine eindeutige ID: `MIRROR-YYYY-MM-DD-{PLATTFORM}-{NNN}`
- Plattform-Kuerzel: MAC (macOS), WIN (Windows), CDX (Codex), GEM (Gemini)
- Eintraege werden NUR angehaengt, nie geloescht
- APPLIED-Kommentare werden von `import` Agents aktualisiert (PENDING → Timestamp)
- Eintraege aelter als 90 Tage UND auf allen Plattformen applied duerfen archiviert werden

**Entry-Typen:** `hook`, `rule`, `agent`, `skill`, `settings`, `mcp`, `software`, `env-fix`, `directive`, `plugin`, `whiteboard`

---

<!-- EINTRAEGE BEGINNEN HIER — neue Eintraege werden nach dem letzten --- angehaengt -->
