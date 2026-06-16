# Session Handoff — 2026-06-16 ~12:00 (BP-Umbau pausiert nach Phase 1)

## Ziel (Gesamt)
Zwei Vorhaben dieser Session: (1) ERLEDIGT — alle 8 Audit-Befunde am Bug-Almanach-/Best-Practices-System
gefixt (#46822-#46832). (2) LAUFEND — den best-practices/-Ordner strukturell 1:1 wie bugs/ umbauen
(6-Phasen-Plan). Phase 0+1 fertig+committet, Phase 2-6 offen.

## Aktueller Status
- ERLEDIGT (Aufgabe 1, alle 8 Befunde, #46822-#46832): Coverage-Luecke cowork-scheduled-tasks (Allowlist),
  README-Index 59/59 + cowork-Drift, best-practices-room.md angelegt, claude-hooks an BP-Guard gekoppelt,
  Kurzcheck in alle 12 Harness-BP (01-12) nachgeruestet, 20 verwaiste bug-cases angereichert + 4 als noise,
  immune-check reaktiviert (additionalContext statt Write-Host), auto-writer MD5-Hash + Pruning-Skill +
  hint python3. health.py KOMPLETT GRUEN.
- ERLEDIGT (BP-Umbau Phase 0+1, #46833 = e869db837): Werkzeuge ABWAERTSKOMPATIBEL umgestellt:
  bug-almanac-guard.ps1+.sh + bugs/check-coupling.py erkennen JETZT BEIDE Strukturen:
  NEU best-practices/<kat>/<bereich>.md  UND  ALT best-practices/projekt-code/<kat>/best-practices-<bereich>.md.
  Live getestet (neu+alt+README-Ausschluss, ps1+sh). NICHTS gebrochen, alte Struktur voll funktionsfaehig.
  Guard-Spiegel in claude-code-setup/hooks/ gesynct (codex/gemini NICHT angefasst — eigene CLI-Pfade).
- OFFEN: Phase 2-6 (siehe Naechste Schritte).

## Frank's STRUKTUR-ENTSCHEIDUNGEN (verbindlich, per AskUserQuestion bestaetigt)
1. Harness-Wissen (01-hooks..12-neues) -> nach best-practices/claude-tooling/ (wie bugs/).
2. projekt-code/-Zwischenebene ENTFERNEN -> best-practices/<kategorie>/ direkt (1:1 wie bugs/).
3. Dateinamen-Schema: <bereich>.md (KEIN best-practices- Praefix mehr, 1:1 wie bugs/).

## Relevante Dateien
- ~/.claude/hooks/bug-almanac-guard.ps1 + .sh (Stufe-C) — schon abwaertskompatibel. BP-Erkennung jetzt
  PFAD-basiert (/best-practices/.../<name>.md) statt Praefix-basiert; bpRoot=best-practices (ganz),
  Filter beide Namensmuster; Transcript-Pattern best-practices[/-].
- ~/proggs/bugs/check-coupling.py — find_best_practices() durchsucht best-practices/ (ganz), strippt
  best-practices- Praefix, schliesst readme/system/_* aus. pc_dir->bp_root.
- ~/proggs/bugs/check-guard-coverage.py — prueft NUR bugs/, NICHT best-practices -> vom Umbau NICHT betroffen.
- ~/proggs/best-practices/projekt-code/<kat>/best-practices-<bereich>.md (~63 Dateien, zu migrieren).
- ~/proggs/best-practices/01-hooks..12-neues/best-practices.md (12 Harness, zu mergen).
- bugs/<kat>/<bereich>.md — Bezugstabellen verweisen auf best-practices/projekt-code/... (Link-Rewrite noetig!).

## Getroffene Entscheidungen
- Werkzeuge ZUERST abwaertskompatibel (Phase 1), DANN migrieren -> kein kaputter Zwischenzustand,
  Migration kann sogar kategorieweise erfolgen. Das ist der sichere Schnittpunkt, an dem pausiert wurde.
- Harness-Ueberschneidung (01-hooks vs schon vorhandenes claude-hooks.md; 05-mcp vs mcp-server.md;
  03-agents vs agents/-Kategorie): in Phase 3 transparent loesen (generische Harness-Themen als
  claude-tooling/<thema>.md, koexistieren mit den spezifischen Bug-Gegenstuecken).

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT — NICHT wiederholen)
- `git rebase --autostash` im GETEILTEN Working Tree: hat uncommittete Arbeit einer parallelen
  ClaudeVoiceOverlay-Session weggestasht (Stash-pop kollidierte). Verlustfrei gerettet nach
  %LOCALAPPDATA%/Temp/stash-rescue/. LEHRE: KEIN rebase --autostash mehr. Stattdessen:
  `git commit -- <pfade>` (atomar) -> `git fetch` -> `git log HEAD..origin/main` pruefen ->
  push NUR wenn behind=0 (fast-forward). pre-push-Hook synct zusaetzlich.
- `git commit -- <pfad>` auf eine NEUE/untracked Datei schlaegt fehl ("pathspec did not match") ->
  ERST `git add <pfad>`, DANN `git commit -- <pfad>`.

## Naechste Schritte (priorisiert) — Phase 2-6
1. Phase 2 (Migration projekt-code): Python-Batch + `git mv` aller ~63
   best-practices/projekt-code/<kat>/best-practices-<bereich>.md -> best-practices/<kat>/<bereich>.md.
   DANN Link-Rewrite ueber ALLE .md im Repo: (a) Pfad best-practices/projekt-code/<kat>/best-practices-<bereich>.md
   -> best-practices/<kat>/<bereich>.md (in bugs-Bezugstabellen, Rules ~/.claude/rules/known-bugs-before-coding.md
   + research-persistence.md, CLAUDE.md). (b) relative Tiefe IN den verschobenen BP-Dateien: eine Ebene
   weniger (z.B. ../../../bugs/ -> ../../bugs/). Empfehlung: kategorieweise (erst android/, testen, dann naechste).
2. Phase 3 (Harness-Merge): 12x best-practices/NN-thema/best-practices.md -> best-practices/claude-tooling/<thema>.md
   (git mv + umbenennen). Verweis in best-practices-claude-hooks.md (heute #46826 angelegt) auf 01-hooks aktualisieren.
3. Phase 4: best-practices/README.md (Kategorie-Index, analog bugs/README.md) + best-practices/SYSTEM.md anlegen.
   Alte 01-12-Ordner + projekt-code/ leeren/entfernen.
4. Phase 5: best-practices-Skill via skill-creator umbauen (vereintes Harness+Plattform-System, schreibt in
   die bugs-gleiche Struktur, Positiv-Medaille). Skill liegt unter ~/.claude/skills/ bzw. als plugin.
5. Phase 6: Gesamt-Test: `python bugs/health.py` (muss gruen), check-coupling 0 Drift, check-guard-coverage
   0 Luecken, Guard-Live-Test (Read neue BP-Datei -> bp-read-Marker; Edit triggert BP-Erzwingung), Tote-Link-Check
   (Markdown-Validator ueber bugs/+best-practices/), Regressionstest gegen Baseline (alle Kopplungs-Paare erhalten).
6. NACH erfolgreichem Umbau: Werkzeug-Abwaertskompatibilitaet wieder straffen (Alt-Pfad/Praefix-Code aus
   guard + check-coupling entfernen, da keine Alt-Dateien mehr).

## Offene Fragen (an Frank, NICHT blockierend)
- Migration kategorieweise (sicherer) oder Big-Bang? (Vorschlag: kategorieweise.)
- Sollen einzelne Harness-Themen (settings, commands) ein eigenes bugs/-Gegenstueck bekommen fuer echte 1:1-Medaille?

## Anker
- Branch: main
- Commit-Nummern parallel: andere Session pushte #46834-#46836 (TVO/CVO Overlay) + eine doppelte #46833.
  Naechste freie Nummer ab #46837.
- Letzte Commits:
  e869db837 #46833 - BP-Umbau Phase 1: guard + check-coupling backward-compatible
  05926b4ce #46832 - bug-almanac-hint.ps1: prefer python3
  cfc789ac0 #46831 - bugs: add prune-bug-cases.py (soft-cap + near-miss-schutz)
  9052867b9 #46830 - bug-case-auto-writer.ps1: stable MD5 error_hash + dead code weg
  23191678c #46829 - immune-check: additionalContext statt Write-Host (reaktiviert)
  e4416a974 #46828 - bug-cases: 20 verwaiste angereichert + 4 noise
