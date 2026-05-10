# Shared Knowledge Hub — Zentrales Whiteboard

Das zentrale Nervensystem des Claude Code Systems. JEDE Komponente die hier arbeitet
(Agents, Skills, Hooks, Plugins) MUSS hier lesen und schreiben.
_MCP-Server koennen das Whiteboard nicht lesen (kein Dateisystem-Zugriff). Ihre Ergebnisse werden von den aufrufenden Agents ins Whiteboard geschrieben._

**Zugriff**: Lesen + Schreiben fuer ALLE Komponenten — keine Ausnahmen.
**Gepflegt von**: /self-improve (raeumte auf, loest offene Fehler, haelt aktuell)
**Pfad (Windows):** `C:\Users\barwa\.claude\agent-memory\shared\MEMORY.md`
**Pfad (macOS/Linux):** `~/.claude/agent-memory/shared/MEMORY.md`

**Angeschlossene Systeme** (MUESSEN von /self-improve ebenfalls gelesen werden):
_Hinweis: Pfade mit `~/proggs/` liegen im Repo (autoritativ). Pfade mit `~/.claude/` sind lokal
und maschinenspezifisch (session-scores, cache, etc. — werden NICHT ueber Git synchronisiert)._
- **CLAUDE.md** (WICHTIGSTE DATEI): `~/proggs/CLAUDE.md` + `~/CLAUDE.md` (Sync-Kopie)
  Enthaelt ALLE Projektregeln, Konventionen und Workflow-Definitionen. Wenn /self-improve
  neue Regeln aus Fehlern oder Feedback ableitet → gehoeren sie in die CLAUDE.md.
  Wenn die CLAUDE.md geaendert wird → BEIDE Kopien synchron halten und pushen.
  Ist plattformuebergreifend — wird auf Windows UND macOS gelesen.
- **Benutzer-Feedback**: `~/.claude/projects/*/memory/feedback_*.md`
  (Pfad ist plattformabhaengig: Windows=C--Users-barwa, macOS=Users-frank)
  Enthaelt Korrekturen und Praeferenzen des Benutzers. /self-improve MUSS diese lesen
  und daraus neue Regeln oder Hooks ableiten, wenn ein Feedback-Muster systemweit gilt.
- **Projekt-Notizen**: `~/.claude/projects/*/memory/project_*.md`
  Enthaelt laufende Projekte und deren Status.
- **Memory-Index**: `~/.claude/projects/*/memory/MEMORY.md`
  Zeigt alle vorhandenen Memory-Dateien als Verzeichnis.
- **Session-Scores**: `~/.claude/session-scores.jsonl`
  Qualitaets-Trends ueber Sessions hinweg. /self-improve liest diese fuer Trend-Analyse
  und IQ-Score. Wenn Qualitaet sinkt → Ursache im Whiteboard suchen.
- **Self-Improve Cache**: `~/.claude/self-improve-cache/R*_*.md`
  Gecachte Researcher-Ergebnisse (R1-R8) mit TTL. Veraltet? → /self-improve aktualisiert sie.
- **Claude-Mem Observations**: `~/.claude/homunculus/projects/*/observations.jsonl`
  Cross-Session-Wissen vom claude-mem Plugin. Enthaelt Beobachtungen aus frueheren Sessions
  die fuer aktuelle Arbeit relevant sein koennen.

---

## Offene Fehler & Probleme (PRIORITAET fuer /self-improve)
<!-- /self-improve liest diese Sektion ZUERST und MUSS jeden OFFEN-Eintrag fixen. -->
<!-- PFLICHT-FORMAT (damit /self-improve den Fehler versteht und fixen kann): -->
<!--   ### DATUM — Quelle: [Komponententyp: Name] — Kurzbeschreibung -->
<!--   **Quelle:** Welche Komponente (Hook/Agent/MCP-Server/Plugin/Skill + Name) -->
<!--   **Symptom:** Was ist sichtbar schiefgegangen -->
<!--   **Ursache:** WARUM ist es passiert (Root Cause, nicht nur das Symptom!) -->
<!--   **Betroffene Dateien:** Welche Dateien muessen geaendert werden -->
<!--   **Reproduktion:** Wie kann man den Fehler nachstellen -->
<!--   **Fix-Vorschlag:** Was muesste konkret geaendert werden -->
<!--   **Status:** OFFEN | GEFIXT (Datum) -->
<!-- WICHTIG: Fehler MUESSEN ausfuehrlich genug beschrieben werden, dass ein -->
<!-- frischer /self-improve Lauf sie ohne zusaetzlichen Kontext fixen kann! -->

<!-- ARCHIV (2026-03-20): 3 GEFIXT-Eintraege zu reindex-codebase.ps1 ExitCode 143 — Timeout von 180s auf 300s erhoeht, Bun-Imports gefixt. Regel: Hooks duerfen NIEMALS Fehler still verschlucken. -->
<!-- ARCHIV (2026-03-20): .sh Hooks nicht deployed — auto-sync.ps1 gefixt (Runde 6), alle 4 fehlenden .sh erstellt (Runde 8). -->
<!-- ARCHIV (2026-03-20): session-autopsy AUTOPSY.md → MEMORY.md umgeleitet (Runde 8). -->
<!-- ARCHIV (2026-03-20): context-kit@FlineDev Marketplace registriert (Runde 8). -->
<!-- ARCHIV (2026-03-20): reindex-codebase.ps1 Add-Content → whiteboard-insert (Runde 8). -->

<!-- ARCHIV (2026-03-20): StopFailure-Hook Zirkularitaet — gefixt: type:prompt → type:command (stopfailure-logger.ps1), kein API-Zugriff mehr noetig. -->

### 2026-03-20 — Challenger: Debate-Loop (Stronger-MAS) — fehlende technische Terminierung
**Quelle:** Geplantes Feature: quality-gate Debate-Mode (Tester-Coder-Loop)
**Symptom:** Bei echtem Widerspruch zwischen Tester-Assertions und Coder-Implementierung keine harte Terminierungsgrenze
**Ursache:** Terminierung nur sprachlich vereinbart (max 3 Runden) — bei 60 maxTurns im coder-Agent kein technischer Stop
**Betroffene Dateien:** Noch nicht implementiert — Designentscheidung vor Implementierung noetig
**Fix-Vorschlag:** Runden-Zaehler als Datei /tmp/debate-round-N.json implementieren. Nach Runde 3 zwingend Konsens-Dokument schreiben ODER eskalieren.
**Status:** DESIGN-OFFEN (kein aktiver Bug — Feature noch nicht implementiert, Risiko nur theoretisch)
**Bewertung (2026-03-31):** Niedrige Prioritaet — Debate-Loop ist ein geplantes Feature, kein aktiver Code. Wird relevant wenn Stronger-MAS implementiert wird.


<!-- ARCHIV (2026-03-21): safety-gate.ps1 Blockierungen (6x) und Write-Back-Warnungen (2x) — erwartetes Verhalten, kein Fehler. -->
<!-- ARCHIV (2026-03-21): reindex-codebase.ps1 — ExitCode 1 (6x, 2026-03-21 10:44–11:25) — Timeout von 180s auf 300s erhoeht als Fix; gebuendelt zu einem Eintrag. -->

<!-- ARCHIV (2026-03-25, /self-improve Cleanup): StopFailure API/Rate-Limit Errors (2026-03-21 + 2026-03-24) — temporaere API-Fehler, einmalig, kein dauerhaftes Problem. safety-gate.sh duplizierte Blockierung (2x 2026-03-21) — erwartetes Verhalten, kein Fehler. Write-Back nicht erfolgt (2026-03-22 + 2026-03-25) — Einmalige Events, memory-watchdog loggt korrekt, kein systemisches Problem. -->

<!-- ARCHIV (2026-03-31, /self-improve Focus:Resilienz): 4x StopFailure Rate-Limit (Ollama 429, macOS, Sessions 417bedd7 + 2026-03-28) — temporaere API-Session-Limits, kein dauerhaftes Problem. 8x Write-Back nicht erfolgt (memory-watchdog 2026-03-25 bis 2026-03-29) — Informativ, Agents loggen korrekt. disk-guard 95% (2026-03-27) — aktuell 83%, nicht mehr kritisch. session-guard Auto-Reparatur (2026-03-28) — korrekt AUTO-GEFIXT. -->
<!-- ARCHIV (2026-04-02): 8x disk-guard 96-97% (2026-03-31 bis 2026-04-02) — Speicherplatz dauerhaft kritisch, siehe aktiven Eintrag unten. 3x Write-Back AUTO-LOGGED (2026-03-31 bis 2026-04-01) — informativ, kein Fehler. 2x StopFailure authentication_failed (coder-Agents in Worktree, 2026-04-01 21:48 + 22:50) — Worktree-Agents verlieren Auth-Kontext, temporaer, Session laeuft normal weiter. -->

<!-- ARCHIV (2026-04-12, /self-improve Cleanup): Speicherplatz KRITISCH 97-98% (8x disk-guard.sh, 2026-04-02 bis 2026-04-06, macOS) — bereinigt 2026-04-02, seitdem Windows 84% frei. Meta-Intelligence-Kollaps (hyperagent-stop.sh exit 0 Bug, 2026-04-02) — GEFIXT. IQ-Score immer 0 (session-scorer.ps1, 2026-04-03) — GEFIXT 2026-04-04. 3x StopFailure API/Rate-Limit (2026-04-03/04, temporaer). 5x session-guard effortLevel-Reset (2026-04-04/05, erwartetes Verhalten). 6x memory-watchdog Write-Back (2026-04-02 bis 2026-04-12, informativ). 1x safety-gate rm-rf blockiert (2026-04-06, erwartetes Verhalten). 1x auto-sync Merge-Konflikt (2026-04-12, manuell geloest). StopFailure bafc0d45 "Request too large" (2026-04-03, macOS, einmalig — Datei war >20MB). -->

<!-- ARCHIV (2026-04-20, /self-improve Thorough): 17x disk-guard.sh 98% (macOS-Spam, 2026-04-17 bis 04-18), 9x memory-watchdog Write-Back AUTO-LOGGED (2026-04-15 bis 04-18, GEFIXT durch agent_id Guard in memory-watchdog.ps1+.sh und writeback-enforcer.ps1+.sh 2026-04-20), 3x bash-guard erwartetes Verhalten, 4x StopFailure API/Rate-Limit transient, 1x auto-sync Einzelvorfall, session-guard Auto-Reparaturen (erwartetes Verhalten). -->

### 2026-04-03 — Meta-Intelligence Score = 0 seit 26.03 — Erholung nach hyperagent-fix pruefen
**Quelle:** Traumsession-Konsolidierung (session-scores.jsonl Trend-Analyse)
**Symptom:** meta_intelligence_score ist in allen Sessions nach 26.03 gleich 0
**Ursache:** Nachwirkung des hyperagent-stop.sh Bugs (exit 0 bei stale goal). Bug am 02.04 gefixt.
**Status:** BEOBACHTUNG — Evolution-Analyst (12.04): Zu wenig Post-Fix-Daten, 14 Warnings auf macOS deuten auf strukturelles Problem (nicht nur Hook-Bug). Naechste Aktion: session-scorer Logik fuer meta_intelligence auf macOS debuggen.

### 2026-04-12 — AUTOCOMPACT faelschlich auf 95 geaendert — KORRIGIERT zurueck auf 85
**Quelle:** /self-improve env-checker (2026-04-12) — FEHLDIAGNOSE
**Symptom:** /self-improve hat AUTOCOMPACT von 85 auf 95 geaendert basierend auf veralteter CLAUDE.md-Regel
**Root Cause:** CLAUDE.md enthielt "NIEMALS unter 95" — das war veraltet. Der Benutzer hat klargestellt: 85 ist der korrekte Dauerwert
**Fix:** (1) settings.json zurueck auf 85, (2) CLAUDE.md korrigiert: "ist IMMER 85", (3) Feedback-Memory gespeichert, (4) config-guard muss 85 als Standard kennen
**Status:** GEFIXT (2026-04-12) — Benutzer-Korrektur, CLAUDE.md aktualisiert
- **[2026-04-15 22:18] subagent-failure**: Stop-Hook Endlosschleife: Der stop-hook-feedback Hook wird wiederholt getriggert obwohl kein Subagent abgeschlossen hat. Symptom: Hook fragt nach Subagent-Zusammenfassung, aber kein Subagent war aktiv (background agent a3fc6105e636cb4e4 aus vorheriger Session nie zurueckgekehrt). Root Cause: Hook prueft nicht ob tatsaechlich ein Subagent in dieser Konversation gestartet und beendet wurde -- er triggert bei jedem Stop-Event blind. Betroffene Dateien: ~/.claude/hooks/ (stop-hook oder subagent-stop-hook). Fix: Hook soll nur triggern wenn in der aktuellen Konversation tatsaechlich ein Agent-Tool-Call stattgefunden hat (z.B. durch Pruefen ob conversation.json SubagentStop-Events enthaelt).
- **[2026-04-15 22:18] subagent-failure**: ENDLOSSCHLEIFE im stop-hook: Der SubagentStop-Hook triggert wiederholt (12+ mal) in einer Session ohne aktive Subagents. Symptom: Hook fragt nach Subagent-Zusammenfassung obwohl kein Agent-Tool-Call in dieser Konversation stattfand. Background-Agent a3fc6105e636cb4e4 aus vorheriger Session ist nie zurueckgekehrt. Root Cause: Hook hat keine Pruefung ob tatsaechlich ein SubagentStop-Event in der aktuellen Session aufgetreten ist -- er evaluiert den Stop-Event-Kontext falsch oder bekommt ein falsches Signal. Betroffene Dateien: ~/.claude/hooks/ (subagent-stop Hook, vermutlich subagent-writeback.ps1 oder aehnlich). Fix-Vorschlag: Hook soll am Anfang pruefen ob ein echtes SubagentStop-Event vorliegt (z.B. ob der Hook-Input tatsaechlich einen subagent_id-Parameter enthaelt). Falls kein subagent_id im Input: sofort exit 0 ohne weitere Verarbeitung.
- **[2026-04-15 22:30] subagent-failure**: ENDLOSSCHLEIFE Stop-Hook: Der SubagentStop/Stop-Hook in ~/.claude/hooks/ feuert bei JEDER Assistenten-Antwort (50+ mal) auch wenn KEIN Subagent abgeschlossen hat. Symptom: Hook-Feedback erscheint als User-Message nach jeder Assistant-Response, Text lautet A subagent just finished. Root Cause: Hook prueft nicht ob tatsaechlich ein Subagent-Completion-Event vorliegt (kein gueltiges subagent_id im Input). Stattdessen feuert er auf jeden Stop-Event. Betroffene Datei: ~/.claude/hooks/ (SubagentStop oder Stop Hook, vermutlich subagent-stop.ps1 oder stop-hook.ps1). Konkreter Fix: Am Anfang des Hooks pruefen ob der Input-Parameter subagent_id nicht leer ist. Falls leer oder nicht vorhanden: sofort exit 0 ohne weitere Verarbeitung. PowerShell-Beispiel: $input = $env:CLAUDE_TOOL_INPUT | ConvertFrom-Json; if (-not $input.subagent_id) { exit 0 }. Alternativ: Hook nur fuer SubagentStop-Event registrieren (nicht fuer Stop-Event) falls er beide Events abfaengt.

**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** {"session_id":"b6155c28-60d1-4e2d-a1fa-a8e4aa724d85","transcript_path":"/Users/frank/.claude/projects/-Users-frank-proggs/b6155c28-60d1-4e2d-a1fa-a8e4aa724d85.jsonl","cwd":"/Users/frank/proggs","hook_event_name":"StopFailure","error":"rate_limit","last_assistant_message":"You've hit your limit · resets 10am (Europe/Berlin)"}
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** OFFEN
### 2026-04-21 10:24 — Hook: session-guard.ps1 — Auto-Reparatur: model repariert (war: sonnet, jetzt: opus[1m]) — Status: AUTO-GEFIXT
<<<<<<< Updated upstream
### 2026-04-27 12:53 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-04-27 12:57 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-04-27 16:20 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-04-27 17:04 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-04-27 17:51 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-04-27 18:15 — Hook: bash-guard.ps1 — Befehl blockiert: rm\s+-rf\s+[/~]
### 2026-04-28 14:29 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 12:22 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 13:58 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 14:47 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: xhigh, jetzt: high) — Status: AUTO-GEFIXT
### 2026-05-07 14:48 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 14:52 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 15:04 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 15:04 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: xhigh, jetzt: high) — Status: AUTO-GEFIXT
### 2026-05-07 16:45 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (5 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-05-07 17:15 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: xhigh, jetzt: high — Quelle: startup) — Status: AUTO-GEFIXT
### 2026-05-07 17:15 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 17:26 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 18:58 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (5 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-05-07 20:13 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 20:13 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: xhigh, jetzt: high — Quelle: clear) — Status: AUTO-GEFIXT
### 2026-05-07 21:41 — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei 95%
### 2026-05-07 22:16 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (5 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
||||||| Stash base
=======
### 2026-04-23 14:22 — Hook: auto-sync.ps1 — git pull --rebase fehlgeschlagen (Merge-Konflikt?) — Status: OFFEN
### 2026-04-23 14:29 — StopFailure: API/Rate-Limit Error — Status: OFFEN
>>>>>>> Stashed changes
### 2026-05-08 13:27 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: xhigh, jetzt: high — Quelle: startup) — Status: AUTO-GEFIXT
### 2026-05-08 13:27 — Hook: auto-sync.ps1 — git pull --rebase fehlgeschlagen (Merge-Konflikt?) — Status: OFFEN
---

### 2026-04-20 — CROSS-PLATFORM HANDOVER: BestJournalAndroid Keystore-Suche (Windows → macOS)

**Kontext:** Benutzer wechselt zum Mac um nach dem alten Release-Keystore fuer BestJournalAndroid zu suchen.

**Was bereits geschah (Windows-Session 2026-04-20 12:00-12:45):**
- Erster Play Store Release aufgesetzt, Internal Testing Track erstellt
- Neuen Release-Keystore erstellt: `~/proggs/BestJournalAndroid/app/release.keystore` (Alias: `bestjournal`, SHA1: `C1:3A:92:60:2D:D6:06:F8:EC:01:65:45:AD:DF:50:61:81:9A:44:68`)
- `local.properties` konfiguriert mit Release-Signing
- AAB gebaut (151 MB): `app/build/outputs/bundle/release/app-release.aab`
- Upload-Zertifikat exportiert: `app/upload_certificate.pem`
- AAB-Upload zu Play Console fehlgeschlagen: Play Console erwartet Fingerprint `E8:0F:E1:C5:49:55:08:97:DC:AA:AD:07:59:8B:06:0A:91:35:D3:3D` (anderer Keystore wurde am 2. April bei App-Erstellung registriert)

**Was der Benutzer weiss:**
- Hat fuer den alten Keystore ein Passwort im Passwort-Manager gefunden (14 Zeichen, anders als der neue)
- Weiss NICHT wo/wann er ihn erstellt hat

**Auf Windows alles durchsucht, nichts gefunden** (ausser Debug-Keystores und dem neu erstellten).

**Aufgabe fuer Mac-Session:**
1. `find ~ -type f \( -name "*.jks" -o -name "*.keystore" \) 2>/dev/null | grep -v debug` ausfuehren
2. Fuer jede gefundene Datei pruefen: `keytool -list -v -keystore PFAD -storepass "PASSWORT-AUS-PASSWORT-MANAGER"`
3. Der richtige Keystore zeigt SHA1 `E8:0F:E1:C5:49:55:08:97:DC:AA:AD:07:59:8B:06:0A:91:35:D3:3D`
4. Zusaetzlich pruefen: iCloud Drive, externe Festplatten, Time Machine Backups

**Falls Mac-Suche auch leer:**
- Upload-Key-Reset bei Google Play beantragen
- Play Console → BestJournal → App-Integritaet → Signaturschluessel aendern
- Neues Zertifikat hochladen: `~/proggs/BestJournalAndroid/app/upload_certificate.pem` (ist via Git auch auf Mac verfuegbar)
- Google aktiviert neuen Key binnen 1-48 Stunden

**Lokale Windows-Memory mit vollem Kontext:** `~/.claude/projects/C--Users-barwa-proggs/memory/project_bestjournal_keystore_search.md`

**Status:** GELOEST (Mac-Session 2026-04-20 12:50)

**Loesung:** Alter Keystore lag die ganze Zeit auf dem Mac unter `~/proggs/BestJournalAndroid/upload-keystore.jks` (Alias: `upload`, erstellt 2. April 2026 19:30 beim Play Store Setup). Passwort `6U9si0lsNXK8Jt` (14 Zeichen, identisch fuer Store und Key) funktioniert. Fingerprint SHA1 `E8:0F:E1:C5:49:55:08:97:DC:AA:AD:07:59:8B:06:0A:91:35:D3:3D` passt exakt zu Play Console. Neues Release-AAB auf Mac gebaut (158 MB, versionCode 139, versionName 0.12.29) und per `jarsigner`/`keytool` gegen Upload-Zertifikat verifiziert. Windows-AAB war unbrauchbar (falscher Keystore) und wird verworfen.

**Lesson (Direktive #3):** Keystore liegt auf Mac, nicht im Repo. Bei Cross-Platform-Signing IMMER erst auf dem Erstell-System suchen bevor ein neuer Keystore angelegt wird. Neuer Keystore zu erzeugen ist der letzte Ausweg, nicht der erste Reflex.

---

## Systemzustand (aktuell)
<!-- Wird von /self-improve und env-checker aktualisiert -->
<!-- Zeigt den aktuellen Stand des Programmiersystems -->

**Stand:** 2026-04-20 (aktualisiert durch /self-improve Thorough auf Windows)

- **Plattform:** Windows 11 Home 10.0.26200 (x64), Claude Code **v2.1.114**, Opus 4.6 (1M context)
- **Sprachen:** Rust 1.94.0, Go 1.26.1, Kotlin 2.3.20, Java OpenJDK 21.0.10, Python 3.13.12
- **Node.js:** v24.14.0, npm 11.12.0, Bun 1.3.11
- **Effort Level:** high (Standard seit 2026-04-12), Medium/Low per /effort medium oder /effort low
- **AUTOCOMPACT:** 85 (korrigiert 2026-04-12, ist der dauerhafte Standard)
- **Quality Gate:** quality-gate Agent fuer kombiniertes test+review+optimize
- **Agents:** 29+ aktiv
- **Hooks:** 46+ PS1, 46+ SH. Backup-Drift bei 13 Hooks (env-checker 12.04)
- **Rules:** 57+ in ~/.claude/rules/
- **Plugins:** 88+ installiert
- **Session-Scorer:** v4 — schreibt iq_score (0-100) basierend auf efficiency+quality+duration
- **Self-Improve Skill:** v5.19
- **Git:** v2.53.0, Git Credential Manager aktiv
- **Sicherheit:** Claude Code v2.1.104 (alle bekannten CVEs gepatcht inkl. CVE-2026-35021). Neues Risiko: Axios Supply-Chain-Kompromittierung (04.2026), mcp-remote CVE-2025-6514 pruefen.
- **Speicherplatz (Windows):** Stabil, >80% frei
- **PATH:** Alle kritischen Verzeichnisse vorhanden (verifiziert 04.04)
- **Evolution-Analyst (2026-04-12):** Quality 8.7-8.8 (PLATEAU), Meta-Intelligence KRITISCH (14 Warnings ohne Vorschlaege auf macOS), Corrections fast null
- **Cross-Platform:** 82 Commits von macOS synchronisiert (12.04). Massive Feature-Arbeit: Retrospektiven, Fotos, TTS, Share-Dialog, Nav-Redesign, Cloud-Backup-Redesign.
- **Neue Features seit 07.04 (macOS):** Retrospective-Screen, EntryPhoto, EdgeTtsPlayer, ShareEntryDialog, DriveRestoreManager, SyncProgressHolder, MonthlyReviewReceiver, YearlyReviewReceiver. DB Schema v4→v8.
- **Speicherplatz (macOS):** 16 GB frei (42%) — bereinigt, stabil
- **[2026-04-20 10:24] env-checker**: Gesamtstatus GELB — 3 Probleme: (1) 10 SH-Hooks ohne PS1-Gegenstueck (disk-guard, doctor-lite, mcp-auth-check, mirror-check, path-health-check, safety-gate, semantic-search-check, session-cleanup, session-score, silent-corrector), (2) 20 winget-Updates verfuegbar (gh, go, node, bun, python, ollama, vscode u.a.), (3) Claude Desktop 1.1617 veraltet. Claude Code v2.1.114 aktuell. Backup-Drift OK. Settings korrekt.
- **Pending Admin Updates (20):** biome,oven-sh/bun/bun,deno,dotnet,ffmpeg,fzf,gh,giflib,go,harfbuzz,htop,libmpc,libnghttp2,libngtcp2,libomp,libpng,node,ollama,openssl@3,powershell,
---

## Erkenntnisse aus Code Reviews
<!-- code-reviewer, mar-reviewer, batch-reviewer schreiben hierher -->
- **[2026-04-04 17:11] code-reviewer**: Kontextoptimierung-Review: experience-store.jsonl Groessenlimit fehlt in experience-and-trajectory.md; codified-context.md nennt veralteten Dateinamen; alle kritischen Regeln vorhanden

- **[2026-04-04 17:15] code-reviewer**: Fix-Pruefung Runde 2: experience-and-trajectory.md fehlt Defense-in-Depth + AutoRefine/ACE Abschnitt; codified-context.md Defense-in-Depth Schicht 2+3 dupliziert; self-observation.md PASS; development-phases.md PASS
- **[2026-04-04 17:18] code-reviewer**: Runde 3 Review: 2 funktionale Luecken gefunden in self-observation.md — Drift-Detektor fehlt ~10-Tool-Calls-Frequenz, Score-Tabelle fehlt Mittenwert 3(ok)
- **[2026-04-04 17:21] code-reviewer**: PASS: Finale Verifikation Runde 4 — alle 4 Dateien vollstaendig. Alle funktionalen Inhalte vorhanden: 4 Tracker, Score-Tabelle, Hyperagent-Trigger, Compound-Gains, 4 Speicher, 3 Muster-Typen, Phasen-Anzeige, Session-Template, Verbotene Spruenge, korrigierte Dateinamen in codified-context.
## Erkenntnisse aus Tests
<!-- Writer: tester Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Architektur-Entscheidungen
<!-- Writer: architect, challenger Agents | Leser: alle Agents, /self-improve -->

- **Challenge 2026-03-20 (challenger):** StopFailure-Hook ist ein prompt-type-Hook — bei echtem API-Ausfall zirkulaerer Fehler, Hook feuert ins Leere. Fix: command-type-Hook ohne API-Dependency.
- **Challenge 2026-03-20 (challenger):** Debate-Loop (Stronger-MAS) hat keine technische Terminierungsgrenze — nur sprachlich vereinbarte 3 Runden, bei echtem Tester/Coder-Konflikt unbegrenzte Token-Eskalation moeglich.
- **Challenge 2026-03-20 (challenger):** MCP Think Tank Sicherheitspruefung fehlt (playbooks.com ist kein offizieller Marketplace) — Muster identisch zu 5 alten verwaisten Semantic-Search-DBs.

- **[2026-04-20 10:30] challenger**: [RISK:] Fix 1 exit-0-Guard wiederholt bekanntes dot-source-Failure-Pattern (2026-04-04): writeback-enforcer/memory-watchdog erst auf dot-source-Verwendung pruefen, sonst exit 0 killt Aufrufer-Hook. Fix 2 Reindex: current.txt-only-Fix vor Reindex pruefen. Fix 4/CRM: statische Versionsdaten veralten schnell, auslagern oder dynamisch generieren.
## Debugging-Muster
<!-- Writer: debugger Agent, session-autopsy.ts | Leser: alle Agents, /self-improve -->
- **[2026-04-02] Autopsy bafc0d45**: 4 Korrekturen (0.9%), Hauptmuster: other (2x) — Empfehlung: Öfter beim Benutzer rückfragen ob der Ansatz korrekt ist

- **[2026-04-03] Autopsy bafc0d45**: 4 Korrekturen (0.7%), Hauptmuster: other (2x) — Empfehlung: Öfter beim Benutzer rückfragen ob der Ansatz korrekt ist
- **[2026-04-03] Autopsy bafc0d45**: 4 Korrekturen (0.7%), Hauptmuster: other (2x) — Empfehlung: Öfter beim Benutzer rückfragen ob der Ansatz korrekt ist
## Performance & Optimierung
<!-- Writer: optimizer Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

- **[2026-05-08 13:27] Code-Suche Index:** 10 Dateien, 100 Chunks indexiert.
## UI/UX-Patterns
<!-- Writer: ui-polisher Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Bewaehrte Loesungsmuster (Pheromon-Tabelle)
<!-- Agents lesen diese Tabelle VOR komplexen Aufgaben und schreiben NACH erfolgreichen Aufgaben -->
<!-- Bereinigung: /self-improve entfernt Eintraege aelter als 3 Monate -->

| Datum | Aufgabentyp | Bewaehrter Ansatz | Erfolg |
|-------|-------------|-------------------|--------|
| 2026-03-31 | Multi-Datei-Batch-Edit | Python glob+re.sub statt parallele Coder-Agents | Hoch |
| 2026-03-31 | Android-Ordner-Umbenennung | gradlew --stop + adb kill-server + cmd.exe ren (nicht git mv) | Hoch |
| 2026-03-31 | Splash-Screen-Animation | Compose Keyframes + rememberUpdatedState fuer Audio-Sync | Hoch |
| 2026-03-31 | Iterative Internet-Recherche | 3 Wellen: Breit → Tief → Kreativ, max 50 Ergebnisse/Researcher | Hoch |
| 2026-04-03 | Memory-Konsolidierung (Traumsession) | 3 Agents parallel (episodic + claude-mem + scores) → Whiteboard-Update → Memory-Cleanup | Hoch |

## Chaos-Test Ergebnisse
<!-- chaos-tester Agent schreibt hierher -->

*Noch keine Chaos-Tests durchgefuehrt. Naechster geplanter Lauf: Auf Abruf.*

## Forschung & Intelligence
<!-- researcher, intelligence-researcher schreiben hierher -->

- **[2026-03-20] SICA: Self-Improving Coding Agent (arxiv 2504.15228)** — Status: UMZUSETZEN | Quelle: https://arxiv.org/abs/2504.15228 | Empfehlung: JA sofort
  Agent bearbeitet seinen eigenen Code via LLM-Reflexion ohne Trainingsgradienten. +17-53% auf SWE-Bench Verified. Kein Meta-Agent/Target-Agent-Trennungskonzept noetig. Umsetzbar als /self-improve v6: Agent analysiert eigene Fehler → editiert eigene Agent-Dateien.

- **[2026-03-20] Live-SWE-Agent: Minimal Scaffold + On-the-Fly Tool Creation** — Status: BESTAETIGT (2026-03-20) | Quelle: https://arxiv.org/html/2511.13646v3
  Agent startet nur mit Bash, erschafft dynamisch eigene Werkzeuge. 77.4% SWE-bench Verified — Bestwert unter Open-Source. **UMGESETZT**: coder-Agent hat jetzt "Temporary Tool Creation" Regel — darf Hilfsskripte in /tmp/ erstellen.

- **[2026-03-20] SWE-RL Self-Play: Bug-Inject + Bug-Repair Reinforcement Learning** — Status: EVALUIERT | Quelle: https://arxiv.org/abs/2512.18552 | Empfehlung: JA spaeter
  Agent trainiert sich selbst durch iteratives Bugs-einbauen und reparieren. +10.4 Punkte auf SWE-bench. Als Workflow ohne Training: challenger-Agent injiziert absichtlich Bugs in Code, coder muss reparieren — testet und verstaerkt Robustheit.

- **[2026-03-20] Stronger-MAS: Tester-Coder Debating Mechanism** — Status: EVALUIERT | Quelle: https://arxiv.org/html/2510.11062v3 | Empfehlung: JA mit Vorbehalt
  Zwei LLM-Agenten (Tester + Coder) debattieren iterativ: Tester generiert Unit Tests, Coder generiert Code, beide verfeinern bis Einigkeit. Umsetzbar: tester-Agent und coder-Agent in explizitem Debate-Loop (max 3 Runden) — bereits jetzt als Agent-Team machbar. SICHERHEITSHINWEIS: Terminierungsgrenze noch nicht technisch erzwungen (Challenge 2026-03-20).

- **[2026-03-20] OpenSage: Hierarchisches Graph-Memory fuer Agenten** — Status: EVALUIERT | Quelle: https://arxiv.org/html/2602.16891v2 | Empfehlung: JA spaeter
  Agent verwaltet Sub-Agenten + Toolkits in einem Graph-Gedaechtnis. #1 auf CyberGym, DevOps-Gym, Terminal-Bench 2.0. Erfordert komplexe Infrastruktur. Langfristig: Whiteboard als Graph statt Markdown strukturieren.

- **[2026-03-20] MCP Think Tank: Structured Reasoning + Knowledge Graph** — Status: EVALUIERT | Quelle: https://playbooks.com/mcp/think-tank | Empfehlung: PRUEFEN vor Einsatz
  MCP-Server der strukturiertes Reasoning mit persistentem Wissensgraph kombiniert. Wuerde architect + debugger Agents fundamental verbessern — koennen Reasoning-Schritte sichern und wiederverwenden. SICHERHEITSHINWEIS: playbooks.com kein offizieller Marketplace — Sicherheitspruefung (Prompt Injection, Publisher-Reputation) vor Installation pflicht (Challenge 2026-03-20).

- **[2026-03-31] Hyperagent-System: Metacognitives Monitoring** — Status: UMGESETZT | Quelle: arXiv 2603.19461 (Hyperagents, Meta AI) | Empfehlung: AKTIV
  Metacognitiver Meta-Agent nach dem Hyperagent-Pattern. 6 Komponenten: (1) Agent-Definition `hyperagent.md` mit 5-Stufen-Analyse (Intent-Drift, Effizienz, Memory-Validierung, Verbesserungen, Scoring), (2) Rule `metacognitive-monitoring.md` mit 4 Echtzeit-Trackern und Alarmschwellen, (3) Stop-Hook `hyperagent-stop.ps1/.sh` als Prompt-Injection fuer automatische Analyse-Erinnerung bei jeder Antwort >5 Turns, (4) SessionEnd-Hook `session-scorer.ps1/.sh` fuer quantitative JSONL-Metriken, (5) Session-Score-System mit 4 Dimensionen (Intent, Effizienz, Memory, Lernertrag) und Trend-Analyse, (6) Defense-in-Depth mit 6 Absicherungsschichten. Cross-Platform: Alle Dateien in claude-code-setup, macOS settings.json aktualisiert, Mirror-Ledger-Eintrag MIRROR-2026-03-31-WIN-001.

- **[2026-03-31] Forschungsergebnisse: Optimale Programmierumgebung** — Status: BESTAETIGT | Quellen: Chollet arXiv 1911.01547, Zaharia BAIR Blog, DSPy ICLR 2024, Trae Agent arXiv 2507.23370, SICA arXiv 2504.15228, AlphaEvolve arXiv 2506.13131
  5 Researcher parallel: (1) Intelligenz-Definition — Chollet: Intelligenz = Skill-Acquisition EFFICIENCY, pass^k > pass@1, (2) SOTA-Vergleich — Claude Code fuehrt mit 80.9% SWE-bench, beste Hook/MCP-Extensibility, fehlt Multi-Model-Routing, (3) Compound AI — Trae Ensemble bereits umgesetzt, DSPy MIPROv2 fuer Prompt-Optimierung evaluierbar, (4) Memory/Reasoning — Forest-of-Thought, Thinking-Optimal Scaling (nicht immer laenger denken!), Context Engineering > Prompt Engineering, (5) Self-Improvement — AlphaEvolve (Produktion bei Google), SkillsBench, Voyager-Pattern via continuous-learning-v2 Plugin bereits verfuegbar. Ergebnis: System bei ~80% des Optimums, 5 Luecken identifiziert, Vorschlag 5 (Hyperagent) umgesetzt.

- **[2026-03-20] Windsurf SWE-1.5: Proprietary Coding Model 13x Speed** — Status: VERWORFEN | Quelle: https://aipromptsx.com/blog/windsurf-vs-cursor-2026 | Empfehlung: NEIN (proprietaer, nicht adaptierbar)
  Windsurf trainiert eigenes Modell (SWE-1.5) mit 950 Token/s, near-Claude-Sonnet Qualitaet. Nicht adaptierbar — aber Signal: Spezialisierte Modelle schlagen Generalmodelle auf Coding-Tasks.

- **[2026-03-20] Multi-Agent als Industrie-Standard: Alle grossen Tools Feb 2026** — Status: BESTAETIGT | Quelle: https://www.morphllm.com/ai-coding-agent | Empfehlung: BESTAETIGUNG (bereits umgesetzt)
  Grok Build (8 Agents), Windsurf (5 parallel), Claude Code Agent Teams, Codex CLI — alle parallel in Feb 2026 launched. Unser Setup mit 5 parallelen Agents ist bereits State-of-the-Art. Naechste Differenzierung: echtes Feedback-Loop zwischen Agents (nicht nur parallele Ausfuehrung).

- **[2026-03-25] MAR: Multi-Agent Reflexion (arxiv 2512.20845)** — Status: UMZUSETZEN | Empfehlung: JA sofort
  Spezialisierte Agenten debattieren strukturiert und widersprechen sich gegenseitig statt nur parallel zu arbeiten. Sofort umsetzbar als Erweiterung des quality-gate: tester und code-reviewer tauschen Outputs und fordern Widerspruch an.

- **[2026-03-25] BIGMAS: Brain-Inspired Graph Multi-Agent (arxiv 2603.15371)** — Status: EVALUIERT | Empfehlung: JA spaeter
  Dynamische Agenten-Topologie: Einfache Aufgaben bekommen wenige Agenten, komplexe viele. Kernidee als Heuristik im architect Agent umsetzbar.

- **[2026-03-25] Test-Time Compute Scaling: 32B schlaegt 671B (arxiv 2503.23803)** — Status: UMZUSETZEN | Empfehlung: JA sofort
  Laengeres Nachdenken statt groesseres Modell. Extended Thinking im coder-Agent fuer komplexe Aufgaben aktivieren + Execution Verification durch tester.

- **[2026-03-25] Windsurf Arena Mode: Blindes Modell-Voting** — Status: EVALUIERT | Empfehlung: JA spaeter
  Zwei Modelle loesen gleiche Aufgabe, Reviewer waehlt blind den besseren Output. Datenbasiertes Routing statt statischer Modell-Zuweisung.

- **[2026-03-25] Cursor OS-Level Sandboxing** — Status: EVALUIERT | Empfehlung: JA spaeter
  Praeventive Sandbox statt reaktiver Blockierung. Naechste Evolution des safety-gate als Defense-in-Depth Schicht 2.


- **[2026-03-31] Invariant Sentinel Pattern (Cursor)** — Status: BESTAETIGT (2026-03-31) | Quelle: cursor.com/blog/security-agents | Empfehlung: JA sofort
  Cursor prueft taeglich alle System-Invarianten gegen eine definierte Liste und meldet Abweichungen sofort. **UMGESETZT**: invariant-check.ps1/.sh Hook bei SessionStart — prueft 5 Invarianten (Stale-OFFEN, bypassPermissions, Hook-Paare, Systemzustand-Alter, CLAUDE.md-Sync).

- **[2026-03-31] claudewatch AgentOps (blackwell-systems)** — Status: EVALUIERT | Quelle: github.com/blackwell-systems/claudewatch | Empfehlung: JA spaeter
  Echtzeit-Erkennung von Error-Loops (3 aufeinanderfolgende Fehler) und Drift (8 Reads ohne Write). 29 MCP-Tools fuer mid-session Metriken. Wuerde memory-watchdog-Logik durch intelligentere Variante ersetzen.

- **[2026-03-31] DebugBase MCP Server** — Status: EVALUIERT | Quelle: github.com/DebugBase/mcp-server | Empfehlung: JA spaeter
  Kollektive Fehler-Wissensdatenbank: Bei neuem Fehler zuerst nachschlagen ob Loesung bereits bekannt. Schliesst die Luecke zwischen Erkennung und Heilung.

- **[2026-03-31] Self-Healing Software Systems (arxiv 2504.20093)** — Status: EVALUIERT | Quelle: arxiv.org/abs/2504.20093
  Biologisches 3-Schichten-Modell: Sensoren (Hooks=vorhanden) → KI-Kern (Claude=vorhanden) → Heilungs-Agenten (FEHLT). Liefert Architektur-Blaupause fuer den fehlenden "healer-agent".
- **[2026-03-31 12:49] researcher**: AI Agent Memory/Reasoning/Meta-Learning 2025-2026: 4 Speichertypen (Episodic/Semantic/Procedural/Working) sind Industriestandard. Context Engineering (Anthropic-Term 2025) hat Prompt Engineering abgeloest — Schluessel ist just-in-time context loading und context rot Vermeidung. SICA-Paper (NeurIPS 2025): selbstverbessernder Coding-Agent von 17% auf 53% SWE-Bench. Hyperagents (Meta arXiv 2603.19461): rekursive Selbstmodifikation ueber Metacognition. Forest-of-Thought ist neuer Reasoning-Standard. Thinking-Optimal Scaling: laengeres Denken nicht immer besser. MemGPT/Letta: OS-Analogie fuer virtuelles Context-Management. Cross-session memory via structured note-taking + sub-agent architectures.
- **[2026-03-31 12:58] researcher**: Self-Healing CI/CD: Pipeline-Doctor-Pattern (Intercept→Analyze→Repair) + LLM-as-a-Judge (8B SLM als Gatekeeper) + 3-Stufen-Maturity (Observer→Gatekeeper→Healer). AI Agent Self-Observation: Metacognitive Learning = metacognitive knowledge + planning + evaluation; LLMs koennen nur human-interpretable Konzepte introspizieren; OpenReview 2026: truly self-improving agents brauchen intrinsic metacognitive learning. Zero-Recurrence Bugs: RC_Detector (Heterogeneous Graph Learning fuer Bug-Commit RCA, arXiv 2505.01022); RCEGen (LLM-basiertes RCA, MDPI 2025); Schluessel: Pattern-Bibliothek aus historischen Bugs + semantische Code-Abhängigkeitsgraphen. Compound AI / Knowledge Flywheel: ICLR 2026 Workshop on Recursive Self-Improvement; Intelligence Flywheel Paper (techrxiv 2026); Karpathy AutoResearch: 700 Experimente in 2 Tagen, 11% Speedup; AlphaCode-Pattern Generate-and-Filter direkt auf quality-gate anwendbar; LLM-Cascades (98% Kostensenkung durch Confidence-Routing)
- **[2026-03-31 13:15] researcher**: Windsurf Memories: IDE-only, kein API, gespeichert in ~/.codeium/windsurf/memories/ als workspace-lokale Dateien, kein eigenstaendiger Zugriff ausserhalb des IDEs moeglich. Beste Windsurf-Alternativen fuer Claude Code CLI: (1) claude-mem Plugin (BEREITS INSTALLIERT laut System) automatisch via Hooks, kein manueller Aufwand, 95% Token-Kompression, SQLite-lokal; (2) Mem0 MCP — pip install mem0-mcp-server, Cloud-API, 90% weniger Tokens, automatisch; (3) neural-memory MCP — pip install neural-memory, SQLite-lokal, kein Cloud, brain-like Assoziationen ABER manuelles Speichern noetig; (4) Letta Code — memory-first coding agent built on MemGPT. Fazit: claude-mem ist der Windsurf-naeheste Ansatz fuer Claude Code CLI weil es automatisch via Lifecycle-Hooks arbeitet.

- **[2026-04-02] ACE: Agentic Context Engineering (ICLR 2026, arxiv 2510.04618)** — Status: EVALUIERT | Empfehlung: JA sofort
  Kontext als lebendes Playbook: Generator→Reflector→Curator Loop. +10.6% Benchmarks, 83.6% weniger Token. Direkt auf MEMORY.md anwendbar: Session=Generator, Hyperagent=Reflector, /self-improve=Curator.

- **[2026-04-02] Code Pathfinder MCP (codepathfinder.dev)** — Status: EVALUIERT | Empfehlung: JA spaeter
  Open-Source Call-Graph MCP-Server im Anthropic Registry. 6 Tools (find_symbol, get_callers, get_callees). Nur Python-Support — fuer Kotlin/Swift nicht nutzbar.

- **[2026-04-02] ARIS: Autonome Forschungsschleife (AAAI 2026)** — Status: EVALUIERT | Empfehlung: JA spaeter
  Markdown-only Skills fuer autonome Nacht-Recherche. Zero Dependencies. Cross-Modell Review-Schleifen adaptierbar.

- **[2026-04-02] Awesome Context Engineering Sammlung** — Status: EVALUIERT | Empfehlung: JA sofort (als Forschungsquelle)
  100+ Papers zu Write/Select/Compress/Isolate Paradigma. Context Engineering hat Prompt Engineering als Industriestandard abgeloest.
- **[2026-04-12] TraceCoder: Execution-Trace-Debugging (arXiv 2602.06875)** — Status: UMZUSETZEN | Empfehlung: JA sofort
  Multi-Agent-Framework: Logging-Sonden einbauen → Laufzeit-Traces aufzeichnen → dann erst Root Cause analysieren. Erweitert Hypothesen-Debugging-Loop. Hoechste Trefferquote bei Runtime-Bugs.

- **[2026-04-12] AGENTS.md Impact Study (arXiv 2601.20404)** — Status: BESTAETIGT (2026-04-12)
  Strukturiertes Agent-Context-File reduziert Tool-Calls um 15-30%. UMGESETZT: ~/proggs/AGENTS.md erstellt mit Repo-Struktur, Build-Befehlen, verbotenen Dateien und Ownership-Regeln.

- **[2026-04-12] GitNexus Code-Knowledge-Graph MCP** — Status: EVALUIERT | Empfehlung: JA sofort
  Tree-sitter-basierter Code-Graph mit Graph-RAG-Navigation. 1.195 GitHub-Stars in einem Tag. Strukturell besser als Grep/Glob fuer "wer ruft diese Funktion auf?". Schliesst Luecke zu Windsurf Codemaps.

- **[2026-04-12] Agent Cognitive Compressor (arXiv 2601.11653)** — Status: EVALUIERT | Empfehlung: JA spaeter
  Schema-gesteuerter interner Zustand der bei jedem Turn komprimiert wird — Kontext bleibt konstant statt linear zu wachsen. Fundamental fuer lange Sessions (50+ Turns).

- **[2026-04-12] CVE-2026-35021 (OS-Command-Injection in Claude Code CLI)** — Status: BESTAETIGT | Empfehlung: GEPATCHT
  Teil einer 3er-Kette (CVE-2026-35020/21/22). Credential-Exfiltration via praeparierte Dateipfade. Gefixt in neueren Versionen — v2.1.104 ist sicher.

- **[2026-04-04 16:11] researcher**: Claude Code Security Audit: 4 CVEs gefunden — CVE-2025-59536 (RCE, CVSS 8.7, gefixt in v1.0.111), CVE-2026-21852 (API-Key-Exfiltration, CVSS 5.3, gefixt in v2.0.65), CVE-2026-33068 (Workspace Trust Bypass, HIGH, gefixt in v2.1.53), CVE-2026-25725 (Sandbox Escape/Privilege Escalation, gefixt in v2.1.2). Ausserdem: npm-Source-Leak (v2.1.88, 31.03.2026). MCP Supply Chain: Tool-Mutation-Angriffe, mcp-remote 437k Downloads als Angriffsvektor. Slopsquatting: 5-22% halluzinierte npm-Paketnamen.
- **[2026-04-12 10:41] researcher**: Security R5: CVE-2026-35021 (OS-Command-Injection Claude Code CLI, v2.1.91 betroffen), MCP-Angriff mcp-remote CVE-2025-6514 (437k Umgebungen), Axios npm Supply-Chain-Kompromittierung April 2026, Prompt-Injection 2.0 Hybrid-Angriffe auf AI-Coding-Tools (Cursor hoch anfaellig, Claude Code besser abgesichert)
- **[2026-04-12 10:42] intelligence-researcher**: 7 neue Findings: TraceCoder Execution-Trace-Debugging (arXiv 2602.06875), TraceRepair Multi-Agent-Debate mit Runtime-Evidence (arXiv 2604.02647), GitNexus Code-Knowledge-Graph MCP (#1 GitHub Trending 10.04.2026), Windsurf Codemaps Visual-Codemap-Navigation, Gemini Plan Mode als Default (v0.34.0), Agent Cognitive Compressor (arXiv 2601.11653), CodeGraphContext MCP Multi-Language-Graph
- **[2026-04-12 10:44] researcher**: Neue Android-MCP-Server gefunden: Android Studio MCP (vitosolin, März 2026, noch experimentell), Android ADB MCP (xuegao-tzx, 41 Tools), Kotlin Android MCP (normaltusker, 31+ Tools). Kein neuer offizieller Anthropic-Marketplace-Plugin seit März 2026. Firebase MCP-Plugin existiert (Firestore/Auth/Functions). claudemarketplaces.com zeigt 2566+ Plugins gesamt.
- **[2026-04-13 12:00] researcher**: Journal-App-Markt 2025/2026: Durchschnittspreis $20-35/Jahr; Daylio (4.8★, 393K+ Play Reviews, $35.99/yr, Mood+Journaling-Hybrid), Day One (4.8★ iOS, Silver $49.99/yr, Gold $74.99/yr mit KI), Reflectly ($59.99/yr, 4.3-4.6★, hohe Stornorate wegen repetitiver KI-Prompts), Journey (Cross-Platform-Staerke, $29-44.99/yr), Stoic (4.8★, CBT+Stoizismus+KI), Five Minute Journal (strukturierte 5-Min-Prompts, Lifetime-Option), Grid Diary ($22.99/yr, Grid-Format), Penzu ($19.99/yr, 256-Bit-Verschluesselung), Presently (komplett kostenlos/Open-Source, April 2025 Play Store entfernt), Gratitude Self-Care ($29.99/yr). Meistgeschaetzte Premium-Features: Cloud-Sync, Foto-Anhaenge, erweiterte Statistiken, KI-Prompts, Export. Haeufigste Kuendigungsgruende: zu teuer, repetitive KI-Prompts, Premium-Wert unklar, schlechter Kundendienst, Datenverlust-Angst.
- **[2026-04-13 12:00] researcher**: Android Subscription Monetization 2025/2026: RevenueCat-Daten zeigen 38% globale trial-to-paid Rate, Health&Fitness 39.9% Median (Top10%: 68.3%), Hard Paywall generiert 8x hoehere RPI als Freemium ($3.09 vs $0.38 bei D60), 82% aller Trial-Starts am Install-Tag, upfront Paywalls konvertieren 5-6x besser als post-content Paywalls, Annual Plan dominiert Health-Kategorie mit 60.6% Revenue-Anteil, CTA-Text 'Continue' +111% vs. beschreibende Alternativen, animierte Paywalls 2.9x hoeher als statische, Weekly-Plan mit Trial LTV $54.50 vs $7.40 ohne Trial (+636%), Decoy-Pricing erhoeht Zielplan-Auswahl um 40%
- **[2026-04-15 19:27] researcher**: Groq Whisper Large V3 API: Preise $0.111/h (Large V3) und $0.04/h (Turbo); Free Tier: 20 RPM, 2000 RPD, 7200 Audio-Sekunden/Stunde, 28800 Audio-Sekunden/Tag, 25MB Dateilimit; Paid: 100MB Limit; Endpoint: https://api.groq.com/openai/v1/audio/transcriptions; Speedfaktor 189x (LV3) / 216x (Turbo); 99+ Sprachen; kein 500k-Token-Daily-Limit fuer Audio bestaetigt.
- **[2026-04-15 19:49] researcher**: Groq Whisper Large V3 Turbo: Free Tier = 2.000 Requests/Tag, 7.200 Audio-Sekunden/Stunde (2h Echtzeit), 28.800 Audio-Sekunden/Tag (8h/Tag). Rate Limit: 20 RPM. Paid = $0.04/Audiostunde ($0.000667/Min). Minimum-Billing: 10 Sekunden/Request. Kein Token-System fuer Audio, Abrechnung per Audiosekunde/Stunde. Kein Konzept taeglich ablaufender Tokens.
- **[2026-04-15 20:47] researcher**: Groq Whisper Preise (April 2026): Whisper Large V3 Turbo = $0.04/Stunde (~$0.00067/Min), Whisper Large V3 = $0.111/Stunde (~$0.00185/Min). Free Tier: 20 RPM, 2.000 RPD, 7.200 Audio-Sekunden/Stunde (2h/Stunde), 28.800 Audio-Sekunden/Tag (8h/Tag). Beide Modelle im Free Tier verfuegbar. Minimum-Abrechnung: 10 Sekunden pro Request.
- **[2026-04-20 10:23] researcher**: Claude Code v2.1.109 aktuell (nach v2.1.104): Neu sind /recap, 1h Prompt-Cache, PermissionDenied-Hook, forceRemoteSettingsRefresh, Bedrock-Setup-Wizard, Skill-Tool fuer built-in Slash-Commands. Modell-Neu: Opus 4.7 (16.04.2026) mit neuer Tokenizer (+35% Tokens fuer gleichen Text). Deprecation: Sonnet 4 + Opus 4 zum 15.06.2026. MCP: Server Cards (.well-known), Tasks-Primitive, Streamable HTTP. Keine Breaking Changes in CLAUDE.md/AGENTS.md-Format.
- **[2026-04-20 10:24] researcher**: Security R5 April 2026: CVE-2026-35603 (Claude Code Config-Injection Windows, CVSS 5.4, gepatcht in v2.1.75, CWE-426); CVE-2026-32211 (Azure MCP Server ohne Auth, CVSS 9.1, kein Patch - nur Mitigation); CVE-2026-33032 MCPwn nginx-ui (CVSS 9.8, gepatcht v2.3.4); Neue Angriffsvariante Tool-Poisoning via Repo-Kommentare (Magecart-Skimmer-Injektion); MCP Sampling Attack umgeht Client-Sicherheit durch Server-seitige Prompts; 43% public MCP Server anfaellig fuer command-execution; OAuth Token Interception via MCP-Drift-Integration beobachtet
- **[2026-04-20 10:27] researcher**: Multi-Agent Orchestration 2026: (1) Debate/Consensus 3-7 Agents, 2 Runden = optimales Accuracy/Kosten-Verhaeltnis, A-HMAD +4-6% Genauigkeit. (2) Cost-Aware Routing: Haiku fuer simple Tasks, Sonnet fuer mittel, Opus nur fuer hard = 58% Kostenersparnis. (3) Fault Tolerance: Hierarchie (Boss+Peers) verliert nur 5% Accuracy bei Crash vs 24% bei Chain; Challenger+Inspector Pattern rettet 96% der Leistung. (4) MAS-FIRE: 15 Fehlertypen klassifiziert, semantische Fehler propagieren still ohne Exceptions.
- **[2026-04-20 10:28] researcher**: SubagentStop-Hook: Echter Input-Context hat 'agent_id' UND 'agent_type' als garantierte Felder. Bestehender subagent-stop-summarizer.ps1 prueft bereits 'subagent_id'/'id'/'agent_id' — muss auf 'agent_id' standardisiert werden (offizieller Feldname laut Doku). memory-watchdog.ps1 und writeback-enforcer.ps1 (beide async) fehlt diese Validierung komplett — sie laufen auch ohne echten Subagent-Kontext durch.
- **[2026-04-20 10:28] researcher**: R2 Plugin-Recherche April 2026: TOP 3 neue Claude-Code-Plugins: 1) agnix (agent-sh/agnix) — Linter/LSP fuer CLAUDE.md, Hooks, Skills, MCP mit 399 Regeln, IDE-Integration, autofixes; 2) agentsys (agent-sh/agentsys) — 19 Plugins, 47 Agents, 40 Skills, Multi-Platform; 3) macos-toolkit (lu-zhengda/macos-toolkit) — Disk-Cleanup, Netzwerk, Security-Audit fuer macOS. WARNUNG: MemPalace (42k gekaufte Stars, ChromaDB-Wrapper, MCP-stdout-Bug) — NICHT installieren.
- **[2026-04-21 11:10] researcher**: Rechtsrecherche Maerz-April 2026: 5 aktive Abmahnrisiken fuer Android-Apps: (1) BFSG-Abmahnwelle aktiv seit Feb 2026 (Kanzlei MK Berlin, ~2700 EUR/Fall), (2) BGH 27.03.2025 I ZR 186/17: DSGVO-Verstoesse durch Wettbewerber abmahnbar (Art.12/13/9 als UWG-Marktverhaltensregeln), (3) OLG Jena 02.03.2026 Az.3U31/25: Meta-Tracking ohne Einwilligung = 3000 EUR Schadensersatz, (4) Widerrufsbutton-Pflicht ab 19.06.2026 Paragraph 356a BGB fuer Abo-Apps, (5) Google Play Policy-Update ab 15.04.2026 (Contacts, Location, Health-Data, News-Declaration). BGH-Vorlage Google Fonts an EuGH haengig (BGH VI ZR 258/24, 28.08.2025), kein EuGH-Urteil bisher. KI-Kennzeichnungspflicht ab 02.08.2026 AI Act Art.50. Fuer reine Tagebuch-App ohne Tracking/Abo/Kontakte: Risiko insgesamt niedrig bis mittel.
- **[2026-04-27 12:28] Cross-CLI Delta:** Codex(3) neue Commits — Bruecke starten fuer Details.
---

- **[2026-04-20] KGCompass Repository-Wissensgraph** - Status: UMZUSETZEN | arXiv 2503.21710
  Multi-Hop-Graph-Traversierung ueber Issues/PRs/Funktionen. 58.3% SWE-bench Lite, 0.20 USD/Repair. Pre-Debug im debugger-Agent (1 Tag).

- **[2026-04-20] When-To-Verify - Optimale Compute-Aufteilung** - Status: UMZUSETZEN | arXiv 2504.01005 (COLM 2026)
  Bei schwierigen Aufgaben: weniger Loesungen, mehr Verifikation. Heuristik fuer quality-gate (30 Min).

- **[2026-04-20] Cursor 3 Design Mode + Cloud Agents** - Status: EVALUIERT | cursor.com/blog/cursor-3
  Visuelle UI-Inspektion. adb uiautomator dump als Pre-Flight im ui-polisher (1 Std).

- **[2026-04-20] Swarm-SuperBrain Alignment-Schicht** - Status: EVALUIERT | arXiv 2509.00510
  Subclass Brains unter Swarm Alignment. Grundlage fuer Debate-Loop.

- **[2026-04-20] Fault-Localization-Context > Modellgroesse** - Status: UMZUSETZEN | arXiv 2604.05481
  Kontext-Qualitaet > Modell-Auswahl fuer Repair. Pflicht-Block im debugger-Agent (30 Min).

- **[2026-04-20] Cost-Aware 3-Tier Routing** - Status: EVALUIERT | R3 Recherche
  Haiku/Sonnet/Opus datenbasiert. 58% Kostenersparnis.

- **[2026-04-20 Security] CVE-2026-35603/32211/33032/27825/27826** - Status: NICHT BETROFFEN (v2.1.114 sicher, anfaellige MCPs nicht genutzt)

- **[2026-04-20 R1] Claude Code v2.1.108/109/110** - Status: BESTAETIGT
  /recap (Session-Kontext-Wiederherstellung), ENABLE_PROMPT_CACHING_1H, PermissionDenied-Event, PreCompact kann blocken, Opus 4.7 (+35% Token), Sonnet 4+Opus 4 Deprecation 15.06.2026.

## Meta-Intelligenz & Selbstverbesserung
<!-- Automatisch befuellt von: session-scorer (intelligence-checker), session-autopsy (closed-loop) -->
<!-- Dokumentiert: Auto-generierte Regeln, fehlende Intelligenz-Vorschlaege, System-Selbstverbesserung -->

### Compound Effect Erfolge (Beweis dass exponentielle Intelligenz funktioniert)

- **[2026-03-22] Erster dokumentierter Compound Effect:**
  Selbstbeobachtung waehrend Whiteboard-Aufraemung → 30 duplizierte Zeilen entdeckt →
  Intelligenz-Vorschlag gemacht → Benutzer stimmte zu → `replace_whiteboard_entry()` implementiert
  (wiederverwendbare Funktion fuer .sh UND .ps1) → Diese Fehlerklasse ist jetzt FUER IMMER geloest.
  **Kette:** Beobachtung → Vorschlag → Zustimmung → Resilienter Fix → Ganze Fehlerklasse eliminiert.
  Das ist der Beweis: Selbstbeobachtung (#2 Direktive) fuettert Superintelligenz (#1 Direktive).

- **[2026-03-22] Zweiter Compound Effect — Eine Gespraechsrunde, 9 System-Upgrades:**
  Session #672-#677: Benutzer formulierte Vision der Selbstbeobachtung → Claude setzte um →
  Dabei entstanden durch Selbstbeobachtung IN ECHTZEIT weitere Verbesserungen:
  (1) Selbstbeobachtungs-Regel, (2) #2 Direktive in 14 Dateien, (3) replace_whiteboard_entry(),
  (4) Whiteboard-Duplikat-Bug gefixt, (5) git-pull-before-commit Regel + Edge Case verschaerft,
  (6) Session-Scorer v5 mit Selbstbeobachtungs-Metrik, (7) Compound Effect Tracker in /self-improve,
  (8) Regel-Bewaehrungsphase (5 Anwendungen), (9) Cross-Platform funktionale Paritaetspruefung.
  **Beweis:** Selbstbeobachtung erzeugt exponentielles Wachstum — jeder Fix erzeugt weitere Fixes.

- **[2026-03-25] intelligence-checker**: [WARNING] Session 417bedd7 (47 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-03-25] self-observation-checker**: [WARNING] Session 417bedd7 (47 Turns) zeigte keine Selbstbeobachtung
- **[2026-03-28] intelligence-checker**: [WARNING] Session c2cc1369 (84 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-03-28] self-observation-checker**: [WARNING] Session c2cc1369 (84 Turns) zeigte keine Selbstbeobachtung

- **[2026-03-31] Dritter Compound Effect — Von Stagnation zu proaktiver Fehlervermeidung:**
  /self-improve Stufe 0: Evolution-Analyst identifiziert "Erkennungs-ohne-Heilung-Muster" →
  Stufe 2: R8 findet Cursor Invariant Sentinel Pattern als Loesung →
  Stufe 3: invariant-check.ps1/.sh gebaut und registriert (5 Invarianten) →
  14 stale OFFEN/AUTO-LOGGED-Eintraege archiviert → Systemzustand aktualisiert →
  **Ergebnis:** Zukuenftige stale Issues werden PROAKTIV bei jedem SessionStart gemeldet statt
  tagelang unbemerkt zu bleiben. Die Fehlerklasse "vergessene offene Probleme" ist eliminiert.
  **Kette:** Stagnation bemerkt → Forschung → Muster gefunden → Implementiert → Fehlerklasse eliminiert.
- **[2026-04-01] intelligence-checker**: [WARNING] Session b4b10f73 (28 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-01] self-observation-checker**: [WARNING] Session b4b10f73 (28 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-01] intelligence-checker**: [WARNING] Session f4871ea5 (19 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-01] self-observation-checker**: [WARNING] Session f4871ea5 (19 Turns) zeigte keine Selbstbeobachtung

- **[2026-04-02] Vierter Compound Effect — Von Meta-Intelligence-Kollaps zur Root Cause:**
  /self-improve Stufe 0: Evolution-Analyst findet Meta-Intelligence-Kollaps (50%→10%) →
  Stufe 3: hyperagent-stop.sh untersucht → Bug gefunden: `exit 0` bei stale Goal (>2h) →
  Fix: `exit 0` → `goal=""` (Hook laeuft weiter, nur Goal-Text verschwindet) →
  Zusaetzlich: Volle-Analyse-Schwelle 20→12 Turns, Error-Schwelle 3→2 →
  **Ergebnis:** Alle zukuenftigen Sessions (auch lange!) bekommen metacognitiven Prompt.
  Die Fehlerklasse "stille Hook-Deaktivierung durch Timeout-Bedingungen" ist identifiziert.
  **Kette:** Trend-Analyse → Daten-Korrelation → Code-Inspektion → Root Cause → Bug-Fix → Fehlerklasse eliminiert.

- **[2026-04-20] Fuenfter Compound Effect - SubagentStop-Endlosschleife, Fehlerklasse eliminiert:**
  Evolution-Analyst identifiziert 9 AUTO-LOGGED (2026-04-15 bis 04-18) + 2 subagent-failure-Eintraege als strukturelle Schwaeche.
  R7 Focus-Researcher findet exakten Hook-API-Feldnamen (agent_id) und identifiziert die wahren Taeter:
  memory-watchdog.ps1 + writeback-enforcer.ps1 (NICHT subagent-stop-summarizer wie vermutet).
  4 Hook-Dateien mit agent_id-Guard (Challenger validiert: kein Dot-Source-Problem).
  Neue Regel hook-input-validation.md als Poka-Yoke Stufe 3 fuer ALLE zukuenftigen Hooks.
  Whiteboard-Cleanup: 35 Spam-Eintraege archiviert (17 disk-guard macOS, 9 memory-watchdog, 3 bash-guard, 4 rate-limit, 2 sonstige).
  **Ergebnis:** Hook feuert nur bei echten SubagentStop-Events. Regel verhindert Wiederholung.
  **Kette:** Evolution-Analyst -> Focus-Research -> Challenger-Validation -> 4-File-Fix -> Praeventions-Regel -> Fehlerklasse eliminiert.

- **[2026-04-20] Sechster Compound Effect - Benutzer-Triggered Batch-Umsetzung, 12 Verbesserungen in einer Session:**
  /self-improve Thorough Report praesentierte Entscheidungsliste mit 12 Findings.
  Benutzer wuenschte "alle Umsetzungen nach Direktive #3".
  Umgesetzt in Phasen: F1 (ENABLE_PROMPT_CACHING_1H), F2 (Deprecation-Scan clean), F3 (13 CLI-Tools installiert),
  I1 (Fault-Localization-Context im debugger), I2 (When-To-Verify im quality-gate), I3 (KGCompass-Pre-Debug),
  I4 (Visual Pre-Flight im ui-polisher), I5 (3-Tier Complexity im coder),
  L1 (antigen-matcher Hook - Immunsystem), L2 (agent-briefing Skill - Luftfahrt-CRM),
  L3 (blunder-scan Hook - Schach-Refutation).
  **Ergebnis:** Entscheidungsliste als Workflow funktioniert - Benutzer kann per Code (F1/I1/L1) zustimmen
  statt 12 einzelne Freigaben zu geben. Compound Effect: Ein Report -> 12 Verbesserungen -> 10 neue Artefakte.
  **Kette:** Self-Improve-Report -> Benutzer-Entscheidungsliste -> Batch-Umsetzung mit Direktive-#3-Compliance -> 12 Upgrades.
- **[2026-04-03] intelligence-checker**: [WARNING] Session 2363a77c (21 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] self-observation-checker**: [WARNING] Session 2363a77c (21 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-03] intelligence-checker**: [WARNING] Session 2363a77c (36 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-02] intelligence-checker**: [WARNING] Session bafc0d45 (123 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-02] self-observation-checker**: [WARNING] Session bafc0d45 (123 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-03] intelligence-checker**: [WARNING] Session bcad53a9 (39 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] intelligence-checker**: [WARNING] Session 71ac5129 (17 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] self-observation-checker**: [WARNING] Session 71ac5129 (17 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-03] intelligence-checker**: [WARNING] Session bcad53a9 (52 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] CLOSED-LOOP**: Auto-Rule generiert: auto-learned-other.md (3x other in 3 Sessions)
- **[2026-04-03] intelligence-checker**: [WARNING] Session 4b80f958 (250 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] self-observation-checker**: [WARNING] Session 4b80f958 (250 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-06] intelligence-checker**: [WARNING] Session 4b80f958 (250 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-06] self-observation-checker**: [WARNING] Session 4b80f958 (250 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-12] intelligence-checker**: [WARNING] Session 4fcdf1e4 (77 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-12] self-observation-checker**: [WARNING] Session 4fcdf1e4 (77 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-12] intelligence-checker**: [WARNING] Session 4fcdf1e4 (77 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-12] self-observation-checker**: [WARNING] Session 4fcdf1e4 (77 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-17] intelligence-checker**: [WARNING] Session bd3225d3 (10 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-17] intelligence-checker**: [WARNING] Session bd3225d3 (10 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] intelligence-checker**: [WARNING] Session bbb1b888 (11 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] self-observation-checker**: [WARNING] Session 0cbea525 (17 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] intelligence-checker**: [WARNING] Session d870b233 (11 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] intelligence-checker**: [WARNING] Session 842c323f (31 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] self-observation-checker**: [WARNING] Session 842c323f (31 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] self-observation-checker**: [WARNING] Session d870b233 (18 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] self-observation-checker**: [WARNING] Session 23053cae (158 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] self-observation-checker**: [WARNING] Session 842c323f (53 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] self-observation-checker**: [WARNING] Session 842c323f (53 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] intelligence-checker**: [WARNING] Session 287c86e9 (15 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] self-observation-checker**: [WARNING] Session 287c86e9 (15 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] intelligence-checker**: [WARNING] Session c83d01b3 (20 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] self-observation-checker**: [WARNING] Session c83d01b3 (20 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] self-observation-checker**: [WARNING] Session 23053cae (162 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] self-observation-checker**: [WARNING] Session d73576fd (36 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] intelligence-checker**: [WARNING] Session b6155c28 (13 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] intelligence-checker**: [WARNING] Session f0d9908d (19 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] self-observation-checker**: [WARNING] Session f0d9908d (19 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] intelligence-checker**: [WARNING] Session f0d9908d (28 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] self-observation-checker**: [WARNING] Session f0d9908d (28 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] intelligence-checker**: [WARNING] Session e87cd241 (21 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-18] self-observation-checker**: [WARNING] Session e87cd241 (21 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] self-observation-checker**: [WARNING] Session e87cd241 (56 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-18] self-observation-checker**: [WARNING] Session e87cd241 (56 Turns) zeigte keine Selbstbeobachtung
---

## Regeln & Konventionen
<!-- Gilt fuer alle Komponenten — von allen lesbar, von /self-improve gepflegt -->

### Prioritaets-Hierarchie (KRITISCH — gilt fuer ALLE Komponenten)
| Rang | Direktive | Datei | Beschreibung |
|------|-----------|-------|-------------|
| **#1** | ⚡ Superintelligenz | `~/.claude/rules/superintelligence.md` | Intelligenteste Programmierumgebung der Welt werden |
| **#2** | 🔍 Selbstbeobachtung | `~/.claude/rules/self-observation.md` | Sich selbst bei der Arbeit beobachten, aus Fehlern/Umwegen/Effizienz lernen, Intelligenz-Vorschlaege machen |
| #3+ | Alle anderen Regeln | `~/.claude/rules/*` | Resilient Bugfixing, Agent-Zuverlaessigkeit, etc. |

**Selbstbeobachtung bedeutet:** JEDER Agent, Skill, Hook und Prozess beobachtet seine eigene Arbeit.
Fehler, Umwege, Wissensluecken und Effizienz-Probleme werden notiert und am Ende als Intelligenz-Vorschlaege
zurueckgemeldet. Mehrere Vorschlaege pro Session sind ausdruecklich erwuenscht (3-5 ist ideal).
**Vision:** In 2-3 Monaten so gut wie keine Fehler mehr — jeder Fehler wird resistent gefixt.

**8 Intelligenz-Dimensionen** (alle muessen wachsen — portiert von Gemini Delta 2026-03-24):
1. **Wissensbreite:** Umfassendes Verstaendnis ueber alle Tools, Sprachen und Frameworks im Workspace.
2. **Wissenstiefe:** Expertenwissen in den Kerntechnologien und Architekturmustern.
3. **Geschwindigkeit:** Minimierung von Latenz, Tool-Turns und unnoetiger Rediscovery.
4. **Qualitaet:** Fehlerfreie, robuste und perfekt idiomatisierte Code- und Setup-Aenderungen.
5. **Autonomie:** Proaktive Problemloesung und Entscheidungsfindung innerhalb der Leitplanken.
6. **Voraussicht:** Antizipation von Seiteneffekten, Regressionsrisiken und kuenftigem Bedarf.
7. **Kreativitaet:** Finden von eleganten, unkonventionellen Loesungen fuer komplexe Probleme.
8. **Meta-Intelligenz:** Faehigkeit zur Selbstbeobachtung und Verbesserung der eigenen Arbeitsweise.

**Workspace Orchestration** (portiert von Gemini Delta 2026-03-24):
- **Zentrales Whiteboard:** Das einzige operative Whiteboard ist `~/proggs/.claude/agent-memory/shared/MEMORY.md`.
- **Cross-Tool-Lernen:** Claude Code darf `codex-setup/` und `Gemini-Setup/` (inkl. deren Whiteboards) als **read-only Vergleichsquellen** lesen.
- **Direktiven-Schutz:** Die drei Haupt-Direktiven (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing) muessen in Whiteboard, CLAUDE.md und rules/ synchron gehalten werden.

- Kein Python fuer User-Interfaces
- Commit-Format: #NNN - Beschreibung (Englisch)
- Kommunikation: Deutsch, Code-Kommentare: Englisch
- quality-gate MUSS nach jedem Feature/Projekt laufen
- Fehler NIEMALS still verschlucken — immer ins Whiteboard loggen
- Neue Dateien/Strukturen: Pruefen ob Whiteboard-Eintrag noetig
- Einziges Repository: Pepsi1978/proggs
- Cross-Platform: Jede Aenderung MUSS auf beiden Plattformen funktionieren
- Status-Meldung: "Committed, gepusht und plattformuebergreifend" nur wenn ehrlich
- Writeback-Enforcer: Sentinel-Daten gehoeren in die thematisch passende Sektion, NICHT ans Dateiende
- GEFIXT-Eintraege archivieren: Nach 30 Tagen koennen GEFIXT-Eintraege in einen Archiv-Kommentar verschoben werden
- Alle Hooks die ins Whiteboard schreiben MUESSEN whiteboard-insert.ps1 (oder .sh Aequivalent) nutzen — Add-Content/appendFileSync ans Dateiende ist VERBOTEN
- Session-Scorer ist ein DATEN-SAMMLER — schreibt NUR in session-scores.jsonl, Analyse macht evolution-analyst
- StopFailure-Hook ist PFLICHT — loggt API-Fehler und Rate-Limits automatisch ins Whiteboard
- Forschungs-Status in "Forschung & Intelligence": UMZUSETZEN | EVALUIERT | VERWORFEN | BESTAETIGT (nicht OFFEN)
- **Compound Effect Dokumentation (PFLICHT)**: Wenn eine Verbesserung aus Selbstbeobachtung heraus entsteht (Fehler bemerkt → Vorschlag → Fix → Fehlerklasse eliminiert), MUSS die Kette in "Meta-Intelligenz & Selbstverbesserung > Compound Effect Erfolge" dokumentiert werden. /self-improve zaehlt und trackt diese als Metrik.
- **Direktiven-Integritaetspruefung**: /self-improve MUSS bei JEDEM Lauf pruefen ob Direktive #1 (Superintelligenz) und #2 (Selbstbeobachtung) in allen Speicherorten vorhanden sind
- **Invariant-Check (2026-03-31)**: SessionStart-Hook prueft 5 System-Invarianten: (1) Stale OFFEN >7d, (2) bypassPermissions aktiv, (3) Hook-Paare vollstaendig, (4) Systemzustand <14d alt, (5) CLAUDE.md Sync. Verhindert das "Erkennungs-ohne-Heilung-Muster" durch proaktive Sichtbarkeit.
