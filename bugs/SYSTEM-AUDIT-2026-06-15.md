# System-Audit: Bug-Almanach / Best-Practices / Aktivierung / Intelligenz

> **Stand: 2026-06-15.** Vollstaendiges read-only Audit des eigenen Wissens-/Lern-Systems mit
> 5 parallelen Agenten (Almanach-, Best-Practices-, Hook-Aktivierungs-, Intelligenz-Dimension +
> externe Web-Recherche). Ziel laut Frank: weniger Bugs im Alltag, das Potenzial der Software
> maximal in Richtung Best Practices lenken.
>
> Die generalisierbaren Erkenntnisse sind als Wissen persistiert:
> `bugs/claude-tooling/agent-knowledge-system.md` (Fallen) +
> `best-practices/claude-tooling/agent-knowledge-system.md` (richtige Bauweise).
> Dieses Dokument haelt den konkreten **Ist-Zustand** + **Maßnahmen-Backlog** fest.

---

## Gesamtbild

Das System hat einen **lebendigen Kern** (Bug-Almanach-Loop: proaktive Guard-Erzwingung +
reaktives `bug-cases.jsonl` + geschlossener Bug→Almanach-Kreislauf) und eine **tote Hülle**
(Lern-/Selbstbeobachtungs-Schleife: experience-store/session-scores/Near-Miss/Pheromon — definiert,
aber auf Platzhaltern, seit ~2 Monaten datentot). Dazu ein **kritischer Aktivierungs-Bug**, der das
ganze System für Subagenten unsichtbar macht. Kurz: stark gegen Bugs, schwach beim Schlauerwerden.

---

## Ist-Zustand je Dimension

### Bug-Almanach (STARK)
- 57 Almanache, alle mit Kurzcheck (Stufe A) + Stand-Datum; `bug-cases.jsonl`: 133 Eintraege, ALLE mit Root-Cause.
- Abdeckung praktisch vollstaendig (alle real bearbeiteten Stacks haben einen Almanach).
- Schwach: Versions-Anker uneinheitlich (~29 gelabelt, ~26 nur inline); Risiko-Asymmetrie — die meistgenutzten Kern-Almanache (claude-hooks 06-01, kotlin/compose/gradle/firebase-billing 06-02) sind die AELTESTEN.

### Best-Practices (STARK, mit Tool-Schwäche)
- 55 BP-Dateien, kategorie-deckungsgleich, alle mit Kurzcheck + Bezugstabelle; flache Namenskonvention → Guard-Erzwingung greift.
- Schwach: `check-coupling.py` meldet **18 Format-Drift-Fehlalarme** (Tabelle existiert, falsches Muster) → Tool unglaubwürdig. **Harness-BP (01–12) werden NICHT erzwungen** (nur projekt-code). Ungepaart: `multi-provider`, `toolchain-updates` (BP ohne Almanach).

### Aktivierung / Hooks (STARK, 1 kritischer Bug)
- Registrierung sauber (index/guard/auto-writer/subagent-context auf korrekten Events), Resilienz top (exit 0, Input-Guards, BOM-frei, pwsh), auto-writer-RAG funktioniert.
- 🔴 **KRITISCH:** aktiver `subagent-context.ps1`/`.sh` ist die ALTE Version (15. Mai), flaches JSON-Schema → **stille Nicht-Injektion**. Subagenten erben weder Bug-Almanach noch Such-Reflex noch Crash-Schutz. Korrekte Fassung liegt nur im Repo. **Verifiziert** (hash-Abweichung, 0 Almanach-Treffer in aktiv, nested+4 Treffer in repo).
- `bug-almanac-index.ps1`: nur EOL/BOM-Drift (kosmetisch). `bug-case-auto-writer.ps1`: `String.GetHashCode()` nicht prozessstabil → Duplikat-Check greift auf Windows nie (.sh nutzt korrekt md5sum).

### Intelligenz / Lernen (KERN lebt, HÜLLE tot)
- Lebendig: Bug→Almanach-Kreislauf real geschlossen (Stichprobe 6/8 Projekt-Bugs befördert), Direktiven sauber spezifiziert.
- 🟠 **Tot:** `experience-store.jsonl`/`trajectories.jsonl` = 65 Platzhalter-Einträge (`success_score:3`, leere tool_sequence), `near_miss:true` **0×**, utility eingefroren. `session-scores.jsonl` **eingefroren seit 2026-04-12** (Scorer feuert nicht). Pheromon-Tabelle verwaist (7 Zeilen, 15. Mai). MEMORY.md „Offene Fehler" mit Auto-Log-Spam verstopft. Harness-Bugs versickern ohne Almanach-Eintrag.

---

## Maßnahmen-Backlog (3 Wellen)

### Welle 1 — sofort, kleiner Aufwand, großer Hebel
- [ ] **[D1/D3] `subagent-context` Repo→aktiv spiegeln** (1 Kopie .ps1+.sh) — behebt, dass Subagenten das System nicht erben. + Hash-Gleichheit verifizieren.
- [ ] **[D3] SessionStart-Drift-Detektor** (read-only sha256 aktiv↔repo aller Hook-Paare) — hätte den 1-Monats-Drift sofort gemeldet.
- [ ] **[D3] `check-coupling.py` Format-Drift beheben** (18 Fehlalarme → 0) + ungepaarte (`multi-provider`/`toolchain-updates`) whitelisten.

### Welle 2 — Kern-Reparatur (Compound-Effekt real machen)
- [ ] **[D1/D2] Auto-Log-Pipeline:** echte Signale schreiben ODER ehrlich abschalten (leer > irreführend). `session-scorer`-Trigger reparieren (warum seit 2026-04-12 still?).
- [ ] **[D3] Harness-Bugs in den Kreislauf zwingen** (claude-tooling/-Almanach-Eintrag wie bei Projekt-Bugs).
- [ ] **[D2] MEMORY.md „Offene Fehler" entrümpeln** + Auto-Log-Dedup/Rate-Limit; Pheromon-Rückschreibpflicht.
- [ ] **[D3] Self-Tests bündeln** in `bugs/health.py` (coupling + guard-coverage + Stand-Verfall + Anker-Konsistenz + Hook-Drift), im SessionStart/PreCommit.
- [ ] **[D1] Harness-BP (01–12) erzwingen** (huckepack bei Hook/Skill/Agent/Config-Edits).

### Welle 3 — Strategie + Pflege
- [ ] **[D2] Kern-Almanache re-recherchieren** (claude-hooks, kotlin, compose, gradle, firebase-billing — älteste + höchstes Volumen).
- [ ] **[D3] Versions-Anker vereinheitlichen** (Pflichtfeld `Anker:` unter `Stand:`) + Staleness-Skript (`version_anchor` ↔ Live-Version).
- [ ] **[D1] Semantischer „Agent-Requested"-Trigger** zusätzlich zum Datei-Trigger (code-search MCP existiert) — fängt Konzept-Arbeit/neue Dateien.
- [ ] **[D1/D2] Memory-Governance** (confidence/last_verified/version_anchor pro Eintrag); langfristig Graph-Memory.
- [ ] **[D1] Agent Skills** als nativen Träger des Digest-Modells evaluieren.

---

## Offene Befunde aus diesem Lauf (NEU)

- 🟡 **`windows-electron-text-injection` ohne Guard-Mapping** — eine parallele Session legte diesen
  `.cs`/`.csproj`-Almanach (ClaudeVoiceOverlay → Claude Desktop Text-Injection) an, ohne Inhalts-Probe
  im `.cs`-Block des `bug-almanac-guard` zu ergänzen → er wird von `dotnet-csharp` verdeckt (gleiche
  Lücken-Klasse wie die 13 in #46777 geschlossenen). **Vom `check-guard-coverage.py` sofort gefangen**
  (Beleg, dass der Poka-Yoke wirkt). Fix: Content-Probe für Text-Injection-Signale (`SendInput`,
  `keybd_event`, `Chrome_RenderWidgetHostHWND`, `contenteditable`, `AttachThreadInput`, `FlaUI`,
  `ValuePattern`, `UIPI`, …) im `.cs`-Block, VOR den bestehenden .cs-Signalen einordnen. Noch NICHT
  umgesetzt (parallele Session arbeitet am Bereich; bewusst nicht hineingefasst).

---

## Verweise
- Wissen (Fallen): `bugs/claude-tooling/agent-knowledge-system.md`
- Wissen (Bauweise): `best-practices/claude-tooling/agent-knowledge-system.md`
- Coverage-Self-Test: `bugs/check-guard-coverage.py` · Kopplungs-Check: `bugs/check-coupling.py`
- Systemverhalten: `bugs/SYSTEM.md` · Verhaltensregel: `~/.claude/rules/known-bugs-before-coding.md`
