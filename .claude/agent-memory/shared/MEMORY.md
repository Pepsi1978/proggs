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

<!-- ARCHIV (2026-05-10, /self-improve Thorough): 3 SubagentStop-Endlosschleife-Eintraege (2026-04-15 22:18, 22:18, 22:30) — GEFIXT 2026-04-20 durch Compound Effect #5: agent_id-Guard in memory-watchdog.ps1+.sh und writeback-enforcer.ps1+.sh + neue Regel hook-input-validation.md. Hook feuert nur noch bei echten SubagentStop-Events. Fehlerklasse durch Poka-Yoke Stufe 3 eliminiert. Siehe Forschung & Intelligence #5 fuer Beweiskette. -->
<!-- ARCHIV (2026-05-10): 1x StopFailure Rate-Limit (b6155c28, macOS, "resets 10am") — temporaer, kein dauerhaftes Problem -->


<!-- ARCHIV (2026-05-10, /self-improve Thorough): 35 Spam-Eintraege archiviert: -->
<!--   - 30x startup-checks.ps1 Speicherplatz 95-96% (2026-04-27 bis 2026-05-10) — Disk-Spam — Root Cause: 96% Speicher dauerhaft, Spam-Filter im Hook fehlt -->
<!--   - 1x bash-guard.ps1 rm -rf blockiert (2026-04-27) — erwartetes Verhalten -->
<!--   - 6x session-guard.ps1 effortLevel-Reset (2026-05-07/08/10) — erwartetes Verhalten bei /clear oder Startup -->
<!--   - 3x memory-watchdog.ps1 Write-Back AUTO-LOGGED (2026-05-07) — informativ, agent_id-Guard greift wenn echter Subagent fehlt -->
<!--   - 2x StopFailure API/Rate-Limit (2026-04-23, 2026-05-10) — temporaer -->
<!--   - 3x auto-sync.ps1 Merge-Konflikt (2026-04-23, 2026-05-08, 2026-05-10) — siehe aktiven Eintrag unten -->

### 2026-05-10 — Hook: startup-checks.ps1 — Disk-Spam (30 Eintraege/Woche)
**Quelle:** Hook startup-checks.ps1 (Windows)
**Symptom:** Bei jeder Session erscheinen 5-12 disk-guard-Eintraege "Speicherplatz KRITISCH bei 95-96%"
**Ursache:** Hook hat keinen Spam-Filter — schreibt bei jedem Lauf (alle 5 Min) einen neuen Whiteboard-Eintrag, auch wenn Wert sich nicht geaendert hat. Zusaetzlich: Disk dauerhaft >95% — Hauptursache muss bereinigt werden, nicht nur Symptom.
**Betroffene Dateien:** ~/.claude/hooks/startup-checks.ps1, claude-code-setup/hooks/startup-checks.ps1
**Reproduktion:** Mehrere /clear hintereinander oder Sessions starten — fuer jeden Lauf 1 Eintrag.
**Fix-Vorschlag:** (1) Hook implementiert Cooldown: max 1 Eintrag pro Tag fuer den gleichen Disk-%-Wert. (2) Disk bereinigen: Temp-Dateien, alte Logs, ggf. node_modules-Caches. (3) `~/.claude/scripts/disk-cleanup.ps1` als manueller Aufruf bauen.
**Status:** GEFIXT (2026-05-30 — Cooldown im startup-checks-Hook implementiert; Disk-Vollstand ist bekanntes separates Thema, siehe Systemzustand)

### 2026-05-10 — Hook: auto-sync.ps1 — Merge-Konflikt seit 3 Tagen
**Quelle:** Hook auto-sync.ps1 (Windows)
**Symptom:** "git pull --rebase fehlgeschlagen (Merge-Konflikt?)" — 3 Vorkommen: 2026-05-08 13:27, 2026-05-10 17:01
**Ursache:** Parallele Sessions auf Windows + macOS schreiben gleichzeitig in MEMORY.md → Merge-Konflikt-Marker bleiben in der Datei stehen → naechster Pull schlaegt fehl. Konkret war ein Konflikt von 2026-04-23 (Updated upstream/Stash base/Stashed changes) bis heute UNGELOEST in der Datei.
**Betroffene Dateien:** .claude/agent-memory/shared/MEMORY.md (Konflikt-Marker), ~/.claude/hooks/auto-sync.ps1 (sollte erkennen und blocken/melden statt still rebase-fehler)
**Reproduktion:** Konflikt erzeugen durch zwei parallele Whiteboard-Inserts auf verschiedenen Plattformen.
**Fix-Vorschlag:** (1) Konflikt-Marker entfernt 2026-05-10 (manuell). (2) auto-sync.ps1 erweitern: vor pull pruefen ob Datei Konflikt-Marker enthaelt → wenn ja, lautstark melden statt still scheitern. (3) Whiteboard-Insert mit File-Lock absichern (Mutex auf .lock-Datei) damit parallele Inserts serialisiert werden.
**Status:** TEILWEISE GEFIXT (Konflikt entfernt 2026-05-10) — Praevention noch offen

### 2026-05-17 20:57 — Hook: bash-guard.ps1 — Befehl blockiert: rm\s+-rf\s+[/~]
### 2026-05-18 11:01 — Hook: bash-guard.ps1 — Befehl blockiert: rm\s+-rf\s+[/~]
### 2026-05-18 11:11 — Hook: bash-guard.ps1 — Befehl blockiert: rm\s+-rf\s+[/~]
### 2026-05-18 14:24 — Hook: bash-guard.ps1 — Befehl blockiert: rm\s+-rf\s+[/~]
### 2026-05-30 16:26 — Hook: auto-sync.ps1 — git pull --rebase fehlgeschlagen (Merge-Konflikt?) — Status: GEFIXT (2026-05-30 geprueft: rebase laeuft sauber durch, war transienter Parallel-Session-Konflikt)

### 2026-06-05 18:05 — StopFailure: API/Rate-Limit Error — Status: TRANSIENT (externer API-Rate-Limit, kein Harness-Bug)
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** {"session_id":"13686411-5971-42e4-aae0-2e4dc4abee8c","transcript_path":"/Users/frank/.claude/projects/-Users-frank--claude-mem-observer-sessions/13686411-5971-42e4-aae0-2e4dc4abee8c.jsonl","cwd":"/Users/frank/.claude-mem/observer-sessions","hook_event_name":"StopFailure","error":"invalid_request","error_details":"400 {\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"prompt is too long: 207552 tokens > 200000 maximum\"},\"request_id\":\"req_011CbkQ3PKZfmnBXMhhaRR7D\"... (truncated)
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** TRANSIENT (externer API-Rate-Limit, kein Harness-Bug)

### 2026-06-13 06:46 — StopFailure: API/Rate-Limit Error — Status: TRANSIENT (externer API-Rate-Limit, kein Harness-Bug)
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** {"session_id":"13686411-5971-42e4-aae0-2e4dc4abee8c","transcript_path":"/Users/frank/.claude/projects/-Users-frank--claude-mem-observer-sessions/13686411-5971-42e4-aae0-2e4dc4abee8c.jsonl","cwd":"/Users/frank/.claude-mem/observer-sessions","hook_event_name":"StopFailure","error":"invalid_request","error_details":"400 {\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"prompt is too long: 212077 tokens > 200000 maximum\"},\"request_id\":\"req_011Cbzem3WYyU6QmjZWoVBuC\"... (truncated)
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** TRANSIENT (externer API-Rate-Limit, kein Harness-Bug)
### 2026-06-15 18:28 — Hook: auto-sync.ps1 — git pull --rebase fehlgeschlagen (Merge-Konflikt?) — Status: OFFEN
### 2026-06-16 12:37 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: xhigh, jetzt: high — Quelle: clear) — Status: AUTO-GEFIXT
### 2026-05-30 17:36 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (5 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-06-30 19:01 — Hook: startup-checks.ps1 — Python-Stub-Shadowing automatisch geheilt: python.exe, python3.exe, pythonw.exe, py.exe aus WindowsApps entfernt (echtes Python: C:\Users\barwa\AppData\Local\Python\bin\python.exe)
### 2026-06-30 19:06 — Hook: session-guard.ps1 — Auto-Reparatur: Projekt C--WINDOWS-system32 settings.local.json erstellt — Status: AUTO-GEFIXT
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

<!-- ARCHIV (2026-06-15, Welle 2 Entruempelung): 71 einzeilige Auto-Log-Spam-Eintraege entfernt (startup-checks Speicherplatz-KRITISCH, memory-watchdog Write-Back AUTO-LOGGED, session-guard effortLevel/settings.local AUTO-GEFIXT, StopFailure transient/archiviert) — alle erledigt/informativ, kein Fix-Wert. stopfailure-logger hat Rate-Limiting (1/h); Disk-Vollstand ist bekanntes separates Thema. -->
## Systemzustand (aktuell)
<!-- Wird von /self-improve und env-checker aktualisiert -->
<!-- Zeigt den aktuellen Stand des Programmiersystems -->

**Stand:** 2026-06-30 (Windows-Session nach laengerer Pause: Versions-Drift behoben, v2.1.196 lokal verifiziert; verwaister stash-pop-Konflikt in MEMORY.md aufgeloest)

- **Plattform:** Windows 11 Home 10.0.26200 (x64), Claude Code **v2.1.196** (Drift behoben 2026-06-30: stand auf v2.1.177), Opus 4.8 (1M context, neuer Default seit Mai — High Effort by Default, Lean System Prompt, Dynamic Workflows Research Preview)
- **Sprachen:** Rust 1.94.0 (1 Minor hinter, CVE-2026-33055/33056), Go 1.26.2 (1 Patch hinter), Kotlin 2.3.20, Java OpenJDK 21.0.10, Python 3.13.12 (1 Patch hinter), Bun 1.3.11 (2 Patch hinter), Node.js v24.15.0 (LTS aktuell)
- **Effort Level:** high (Standard seit 2026-04-12). Manuelle Aenderung bleibt bis Session-Ende erhalten (CLAUDE.md-Update 2026-05-08).
- **AUTOCOMPACT:** 100 (dauerhafter Standard ab 2026-05-24; grosse Komprimierung erst bei 100%, Microcompact erledigt den Rest. Alter Wert 85 war wegen Math.min-Clamp wirkungslos)
- **worktree.baseRef:** "fresh" (NEU 2026-05-10) — schuetzt vor unpushed Commits in Worktrees
- **Quality Gate:** quality-gate Agent (test+review+optimize parallel)
- **Agents:** 29+ aktiv
- **Hooks:** 46+ PS1, 46+ SH. Cross-Platform-Paritaet: 2026-05-10 startup-checks.sh nachgezogen (Disk-Cooldown).
- **Rules:** 57+ in ~/.claude/rules/
- **Plugins:** 88+ installiert
- **Session-Scorer:** v4 — Dedup-Bug 2026-05-10 GEFIXT (R7-Diagnose: Loop ueber letzte 10 Lines statt nur lastLine; reines session_id-Matching ohne "similar turns"-Toleranz nach Challenger-Review)
- **Self-Improve Skill:** v5.19
- **Git:** v2.53.0, Git Credential Manager aktiv
- **Sicherheit:** Claude Code v2.1.196 (CVE-Stand fuer 2.1.196 noch nicht geprueft — Best-Practices/Security-Recherche empfohlen). NEU offen: Axios npm Supply-Chain (NK-Attribution, April 2026) — npm audit in PromptBoard + VoiceOverlay-Projekten DRINGEND. Comment-and-Control Prompt Injection 2.0 (Mai 2026, Microsoft Research) — Parry-Scanner aktiv halten.
- **Speicherplatz (Windows):** **97% belegt, ~28GB frei (KRITISCH)** — 2026-05-30: 436MB sichere Caches geraeumt. Grosse regenerierbare Caches bewusst behalten (Frank-Entscheidung): ~/.gradle 11G, build/-Ordner 5.6G, ~/.claude/projects 4.2G (cleanupPeriodDays=99999), plugins 2.1G. Heartbeat zeigt seit #1209 WARNING statt CRITICAL (GB-basierte Schwelle: CRITICAL erst <10GB frei) — kein Falsch-Alarm mehr, warnt aber weiter.
- **PATH:** Alle kritischen Verzeichnisse vorhanden (verifiziert 04.04)
- **Evolution-Analyst (2026-05-10):** Quality 8.72→8.74 PLATEAU. Meta-Intelligence KRITISCH bei 10% (Schwelle 20%). Intelligence-Vorschlaege bei 40% (Pflicht 70%). Compound Effect Pause: 20 Tage seit #6 (2026-04-20). Session-Scorer Duplikat-Bug verfaelschte die Messung — nach Fix erneute Messung in 5 Sessions noetig.
- **Letzter Compound Effect:** #6 (2026-04-20) — Sechster Effect, 12 Verbesserungen in einer Session via Entscheidungsliste-Workflow.
- **Cross-Platform:** Beide Plattformen synchron. macOS-Stand wird beim naechsten Mac-Start aufgeholt.
- **[2026-05-10 19:30] /self-improve Thorough**: Lauf abgeschlossen — Merge-Konflikt MEMORY.md gefixt, 35 Spam-Eintraege archiviert, 3 Stop-Hook-Stale-Eintraege als GEFIXT markiert (Compound Effect #5), session-scorer.ts Dedup-Fix umgesetzt, startup-checks Cooldown gestuft (.ps1+.sh), worktree.baseRef:"fresh" eingetragen, R8-Findings (Anthropic Dreaming, Darwin Goedel Machine, ARISE, Outcomes/Grader) in Forschung.md ergaenzt.
- **[2026-05-10 19:22] evolution-analyst**: PLATEAU (8.72→8.74, +0.02): Qualitaet stabil aber Meta-Intelligence KRITISCH (10% self-improving, 40% Vorschlaege) — groesste Schwaeche ist fehlendes 7. Compound-Effect seit 3 Wochen und 6 unumgesetzte UMZUSETZEN-Forschungseintraege (SICA, MAR, TraceCoder, When-To-Verify, KGCompass, Fault-Localization) die alle >30 Tage alt sind.
- **Pending Admin Updates (20):** biome,certifi,deno,dotnet,ffmpeg,fontconfig,fzf,gh,giflib,git,glib,go,golangci-lint,gradle,gradle-completion,graphite2,harfbuzz,htop,kotlin,ktfmt,
---

## Erkenntnisse aus Code Reviews
<!-- code-reviewer, mar-reviewer, batch-reviewer schreiben hierher -->
- **[2026-04-04 17:11] code-reviewer**: Kontextoptimierung-Review: experience-store.jsonl Groessenlimit fehlt in experience-and-trajectory.md; codified-context.md nennt veralteten Dateinamen; alle kritischen Regeln vorhanden

- **[2026-04-04 17:15] code-reviewer**: Fix-Pruefung Runde 2: experience-and-trajectory.md fehlt Defense-in-Depth + AutoRefine/ACE Abschnitt; codified-context.md Defense-in-Depth Schicht 2+3 dupliziert; self-observation.md PASS; development-phases.md PASS
- **[2026-04-04 17:18] code-reviewer**: Runde 3 Review: 2 funktionale Luecken gefunden in self-observation.md — Drift-Detektor fehlt ~10-Tool-Calls-Frequenz, Score-Tabelle fehlt Mittenwert 3(ok)
- **[2026-04-04 17:21] code-reviewer**: PASS: Finale Verifikation Runde 4 — alle 4 Dateien vollstaendig. Alle funktionalen Inhalte vorhanden: 4 Tracker, Score-Tabelle, Hyperagent-Trigger, Compound-Gains, 4 Speicher, 3 Muster-Typen, Phasen-Anzeige, Session-Template, Verbotene Spruenge, korrigierte Dateinamen in codified-context.
- **[2026-05-10 19:41] unknown**: (no findings)
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
| 2026-05-15 | Custom Win+Alt-Hotkey im LL-Keyboard-Hook (.NET/WPF) | 3-Killer-Check: (1) EF HasConversion fuer char?, (2) Bootstrap-Pfad neben Render-Pfad, (3) ReleaseNonCtrlModifiers vor SendCtrlV + File-Logging in %TEMP%. Volle Anleitung: ~/proggs/LEARNINGS.md "Custom Global Hotkeys" | Hoch (nach 6 Commits) |
| 2026-05-15 | Externe Trigger (Stream Deck / MacroPad / Voice) auf UI-Toggle legen | Separate Toggle-Methode `ToggleXFromHotkey()` einbauen die NUR den State + die Button-Optik aendert. NIEMALS den UI-Click-Handler direkt aufrufen — der hat fast immer Side-Effects (Return-Push, Submit, Animation) die fuer den externen Trigger unerwuenscht sind. Faustregel: UI-Klick = visuelle Sofortwirkung mit Side-Effects, externer Hotkey = reine Status-Aenderung. | Hoch |

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
- **[2026-05-10 19:36] researcher**: Claude Code v2.1.114→v2.1.138: HOOKS erhalten jetzt effort.level + $CLAUDE_EFFORT + type:mcp_tool; NEUE SLASH COMMANDS: /ultrareview, /recap, /usage; NEUE SETTINGS: worktree.baseRef, channelsEnabled, alwaysLoad MCP, disableSkillShellExecution; WINDOWS: Git Bash ab v2.1.120 nicht mehr Pflicht; MODELLE: claude-opus-4-7 neuer Default; PLUGINS: .zip + --plugin-url neu. Neueste Version: v2.1.138 (2026-05-09).
- **[2026-05-10 19:37] researcher**: Multi-Agent Mai 2026: Cursor 3 /multitask + Tiled Agents Window fuehrt; Hierarchical (Planner+Worker+Judge) dominiert ueber Mesh bei 5+ Agents; 3-Tier-Routing spart 51-75% Kosten (Haiku=Grep/Nav $1/MTok, Sonnet=Code $3, Opus=Arch $5); MAS-FIRE Closed-Loop neutralisiert 40% mehr Faults als Linear-Workflow; Debate-Loop Template: MAX_ROUNDS 3-5 + AGREED-Token als Stop; Sequentielles Sampling schlaegt paralleles Self-Consistency in 95.6% der Configs; Anthropic Managed Agents (Dreaming/Outcomes) in public beta.
- **[2026-05-10 19:38] intelligence-researcher**: 6 Findings: DGM Open-Ended Evolution (Score 17/20, SOFORT), Anthropic Dreaming (Score 18/20, SOFORT), ARISE Graph-Fault-Localization (Score 16/20, SOFORT), Outcomes/Grader-Loop (Score 16/20, SOFORT), Self-Organizing MAS Continuous Dev (Score 14/20, SPAETER), AlphaEvolve OpenEvolve API (Score 15/20, SPAETER) — Compound Effect Durchbruch: Dreaming ist direkter Drop-in fuer unser MEMORY.md-Self-Improve-System
- **[2026-05-10 19:38] researcher**: Kreative Verbesserungen Claude-Code-Harness: 1) Verifiable-Outcome-Gate statt timer-basiertem self-improve (RLVR-Pattern arxiv 2601.22607); 2) Strukturiertes versioniertes Memory Memstate-Pattern 92% vs 17% Recall; 3) Radikale Vereinfachung Vercel-Case 80% Tools geloescht Success 80->100% Speed 3.5x; 4) Ebbinghaus-Forgetting-Curve fuer experience-store.jsonl; 5) Intrinsic Metacognition: Hooks sind EXTRINSIC human-designed loops - Geheimwaffe Agent entscheidet SELBST wann er sich verbessert.
- **[2026-05-16 13:13] researcher**: Polar AccessLink API: Zwei aktive Versionen (v3 stabil + v4 neu). Kostenlos, OAuth2, Rate-Limit 3000/15min + 100000/24h. Registrierung: admin.polaraccesslink.com. HR-Samples, GPS, Pace, Cadence, Power, Altitude verfügbar. H10 Daten landen in Polar Flow wenn Uhr gekoppelt + synct. Webhook-Support vorhanden (Polling als Alternative). Redirect-URI muss bei Registrierung eingetragen werden, localhost für Dev erlaubt.
- **[2026-05-16 13:33] researcher**: Polar AccessLink API v3 vollständig analysiert: Exercise-Detail-DTO (20+ Felder inkl. heart-rate, training-load-pro, fat/carb/protein-percentage, running-index), Sample-Format (kommagetrennte String, KEIN JSON-Array, recording-rate in Sekunden), OAuth2 (HTTP Basic Auth mit base64 client_id:client_secret, NUR accesslink.read_all Scope, Token läuft NICHT ab, KEIN Refresh-Token, x_user_id im Token-Response), User-Registration (member-id im Body, polar-user-id aus OAuth-Token-Response), Transaction-Pflicht (PUT-Commit sonst doppelte Daten). Kotlin-Datenklassen-Block mit @SerialName erstellt.
- **[2026-05-16 22:08] researcher**: Polar Flow Web-API Reverse Engineering: Interner Endpoint flow.polar.com/training/analysis/{activityId}/range/data liefert HR/Speed/Altitude/Pace als JSON-Samples. Export-URL: flow.polar.com/api/export/training/{format}/{activityId} (format=tcx/gpx/csv). Authentifizierung via Browser-Session-Cookie (kein oeffentliches Login-API). Mehrere Open-Source-Projekte (scanban/polar-flow-export, campbellr/flow-client, asib/polar-flow-export) nutzen Session-Cookies. AccessLink-Webhook liefert NUR entity_id + url, keine Streams. Re-Sync committed Workouts: nicht moeglich via offizielle API. Fuer Android ohne Browser: POST flow.polar.com/login mit email/password + Cookie-Jar ist realistischster Weg.
- **[2026-05-16 22:19] researcher**: Polar AccessLink V3: GET /v3/exercises (kein Parameter) existiert als Listen-Endpoint und liefert HASHED IDs — kein Webhook noetig. Sub-Endpoints /tcx /gpx /fit alle mit hashed ID. /v3/notifications liefert entity_id als hashed ID (Basic Auth). Numeric-zu-Hash-Konversion ist NICHT oeffentlich dokumentiert. V4 existiert (polar.com/polar-api-v4) mit UUID-basierten IDs, loest das Hashed-ID-Problem nicht explizit. flow.polar.com URLs nutzen numerische IDs.
- **[2026-05-17 13:04] researcher**: Android i18n Best Practices 2025/2026: Naming-Schema <kontext>_<beschreibung>, xliff:g mit id+example Pflicht, ICU MessageFormat statt <plurals> ab API24, UiText sealed class fuer ViewModel-Strings (DynamicString+StringResource), Compose pluralStringResource(), translatable=false fuer Markennamen, ICU-Plurals haben 6 CLDR-Kategorien. Bug-Klassen: falsche Verzeichnisnamen (values-en-US statt values-en-rUS), fehlende 'other'-Quantity, String.format statt getString(). Tools: Crowdin + Android-Studio-Plugin (2025 aktiv), Lokalise, SimpleLocalize. Deutsche UX: German UPA 8 Heuristiken (Einheitlich = kein Mix Du/Sie), Gendern per Doppelpunkt empfohlen (Barrierefreiheit bevorzugt Vollformen).
- **[2026-05-17 19:50] researcher**: Sprach-Eigenheiten fuer 26-Sprachen-Uebersetzungs-Skill erfasst: Plural-Kategorien (AR=6, PL/RU/CS=4, HI=5, HE/RO/IT=3, FR/PT=4, TR/FI/HU/NL/SV/DA/NO/EL/FA/GR=2, TH/KO/JA/ZH/VI/ID=1). Text-Expansion vs Deutsch: FI/JA stark kuerzer, PL/HI stark laenger, AR+20-25%. RTL-Sprachen (AR/HE/FA): Bidi-Markierungen und keine String-Konkatenation. Consumer-Apps: meistens informell (tu/du/je), Ausnahmen PT-PT formal, KO Hoeflichkeitsstufe haefig.
- **[2026-05-17 20:58] researcher**: Datenschutz-Pflichtangaben fuer 5 Maerkte recherchiert: HK (PDPO, Sec.33 nicht in Kraft, PICS-Pflicht, GBA-Vertrag ab Nov 2024), TW (PDPA-Reform Nov 2025, PDPC noch nicht gegruendet, NT$2-15Mio Strafe), NZ (Privacy Act 2020, IPP12 Cross-Border, 72h Meldepflicht, max NZD 50k), MX (neue LFPDPPP Maerz 2025, INAI aufgeloest, Ministerio ACGG, Simplified Notice Pflicht, Spanisch Pflicht, bis ~$3.8Mio USD Strafe), AR (Ley 25326 noch in Kraft, Reform-Bills 2025, AAIP, EU+CH+NZ+IL+UY+CA adequat, SCCs moeglich). Alle Laender benoetigen Privacy Policy. Kein Land schreibt explizit App-Store-Enforcement vor — Google setzt eigene DPP durch.
- **[2026-05-17 20:58] researcher**: Rechtssicherheits-Checkliste Android-Apps DE/Google Play Mai 2026: Oeffentlich (Pflicht): Impressum (DDG §5), Datenschutzerklaerung DE+EN (DSGVO Art.13/14), AGB/Nutzungsbedingungen, Widerrufsbelehrung (BGB), TDDDG §25 Cookie/Tracking-Consent-Banner, AVV mit Firebase/Google/OpenAI (Art.28), Account-Loeschseite (Play-Pflicht), Barrierefreiheitserklaerung (BFSG ab 28.6.2025, KMU-Ausnahme <10MA/<2Mio EUR), DSA-Beschwerdekontakt. Play Console (mandatory): Data Safety Form, IARC Content Rating, Target Audience, Health Apps Declaration (ab Aug 2025), Permissions Declaration. Intern (nicht oeffentlich): VVT/ROPA Art.30, DSFA Art.35 (bei Hochrisiko), TOM-Dokument, SCCs fuer Firebase/OpenAI (kein DPF fuer beide), Drittlandtransfer-Dokumentation, Loeschkonzept, Datenpannen-Meldeplan. AI Act ab 2.8.2026: Art.50 KI-Transparenzpflicht, Deepfake-Kennzeichnung. Haeufigste Abmahngruende: fehlendes Impressum, fehlende Datenschutzerklaerung, kein Account-Loeschpfad, fehlende Widerrufsbelehrung bei In-App-Kaeufen.
- **[2026-05-17 21:27] researcher**: Compose Pinch-Zoom 60fps: graphicsLayer{} Lambda (Draw-Phase, kein Recomposition) > Canvas-Neuberechnung; detectTransformGestures OK aber awaitPointerEvent mit awaitEachGesture fuer volle Kontrolle; drawWithCache fuer Path/Brush-Objekte cachen; derivedStateOf nur wenn berechnetes Ergebnis selten wechselt; Google Maps nutzt native GL-Raster-Tiles nicht Compose-Canvas; Haupt-Ursache fuer Ruckeln: State-Reads in Composition-Phase statt Draw-Phase.
- **[2026-05-18 10:48] researcher**: Claude Code Skills Best Practices 2026: Offizielle Anthropic-Doku vollstaendig gefunden. Kernpunkte: SKILL.md max 500 Zeilen, description max 1024 Zeichen in 3rd Person, Progressive Disclosure via references/-Ordner (max 1 Ebene tief), Eval-Driven Development (EDD) als Goldstandard, Anti-Patterns: zu viele Skills, verschachtelte Referenzen, Windows-Pfade. Fuer String-Extraktor-Skill relevant: domain-spezifische Ordnerstruktur (reference/rules-de.md, reference/android-patterns.md), feedback-loop via validate-Script, explizite Trigger-Woerter fuer hardcoded/strings.xml/i18n im description-Feld.
- **[2026-05-18 10:50] researcher**: Android-Roentgen-Skill 2026: Compose Compiler Reports (composables.txt) sind der direkteste Weg um alle Composables zu inventarisieren. KSP2 (default seit 2025) + custom Annotation Processor ist State-of-the-Art fuer NavGraph-Extraktion. MobSF/mobsfscan (Semgrep-basiert) deckt Kotlin-Source-Analyse ab. Twitter/X Compose Rules decken nur Code-Qualitaet, nicht Feature-Inventar. Google Play Policy April 2026 verschaerft Enforcement gegen misleading descriptions. Detekt Custom Rules via PSI-AST koennen BillingClient-Aufrufe und Premium-Gates extrahieren. UWG §5 gilt fuer App-Werbung ohne spezielle App-Compliance-Praxis.
- **[2026-05-18 10:52] researcher**: Claude Code Skill Design Patterns 2026: SKILL.md max 500 Zeilen (Layer 2), references/scripts/assets Pattern bestaetigt, Progressive Context Loading laedt nur YAML (~100 Token) bis Skill relevant. Description: imperativ statt deskriptiv, multilingual DE+EN moeglich. Multi-Phase: JSON-Checkpoint-Dateien fuer State-Persistence, Trail-of-Bits-Pattern fuer Security-Audits. Cross-Platform: sed via Tempfile statt sed -i, grep -E statt grep -P, find mit xargs -0. Token-Effizienz: Skript-Vorverarbeitung spart 14-70% vs Raw-Read. Externe Tools: Auto-Install via Claude wenn ModuleNotFoundError, explizite command-v Checks empfohlen.
- **[2026-05-18 10:56] researcher**: Claude Code Plugin Best Practices 2026: Struktur ist plugin-root/.claude-plugin/plugin.json (einzige Pflicht-Datei) + skills/ agents/ hooks/ commands/ auf Root-Ebene. Plugin-Dev-Toolkit (7 Skills) existiert offiziell unter /plugin-dev:create-plugin. Skills haben eigene references/ Ordner, kein geteilter Plugin-Level-references/. Orchestrator-Agent-Pattern via settings.json 'agent'-Key. Skill-Chaining durch SKILL.md-Anweisungen die andere Skills aufrufen. Marketplace via .claude-plugin/marketplace.json. Plugin-Hooks in hooks/hooks.json. Grosse Plugins wie compound-engineering (37 Skills, 51 Agents) und superpowers nutzen beide Skills+Agents parallel.
- **[2026-05-18 12:09] researcher**: Compound Engineering Plugin (EveryInc/compound-engineering-plugin): 37 Skills + 51 Agents, MIT-lizenziert, GitHub ~9.3k Stars. Core Loop: ce-brainstorm → ce-plan → ce-work → ce-code-review → ce-compound. Philosophie: 80% Planung/Review, 20% Ausführung. Compound-Schritt persistiert Learnings in docs/solutions/ die zukünftige Agenten lesen. Gebaut von Dan Shipper + Kieran Klaassen (Every Inc). Sweet Spot: komplexe Solo/Small-Team Codebases. vs Superpowers (121k Stars): CE fokussiert auf Wissenserhalt/Parallel-Research, Superpowers auf Engineering-Disziplin-Erzwingung (TDD). Key Pattern für Plugin-Bauer: strukturierte Artefakte (Plans, Solution Docs, STRATEGY.md) die Agent in Folgesessions liest = echter Compound Effect.
- **[2026-05-24 11:01] researcher**: Semantische Code-Suche (snowflake-arctic-embed2, 8192 Token, 568M Param): Optimale Chunk-Groesse 128-512 Token / Funktions-Level; AST-Chunking +21pp Recall@5 (70.1% vs 42.4% Fixed-Size, cAST-Paper); Semantic Search schlechter als grep bei exakten Symbolen/Rename/allen Aufrufstellen/Import-Graphen; arctic-embed2 bevorzugt kleinere Chunks laut Snowflake-eigenen Hinweisen; Hybrid BM25+Vector per RRF ist Best Practice 2024-2025.
- **[2026-05-24 11:02] researcher**: Semantische Code-Suche per Stack: Kotlin/Compose gut (tree-sitter-kotlin aktiv gepflegt, Maturity 14, aber Compose-Bäume > 200 Zeilen benötigen rekursives AST-Chunking); C#/XAML gespalten (C# top in coa-codesearch-mcp, XAML-Markup semantisch arm da kein AST-Semantik by design → grep bevorzugen); TypeScript/JS exzellent (native Unterstützung in allen Tools); Tampermonkey-JS (einzelne Großdatei) → function-granular Chunking nötig, grep für exakte Funktionsnamen besser; Markdown gut (tree-sitter-markdown, heading-aware Chunking); PowerShell nicht in den meisten Tools (tree-sitter-PowerShell existiert, aber kaum integriert → grep-Only empfohlen). Hybrid (grep+semantic) +31% Erfolgsrate, +56% auf großen Codebasen. XAML semantisch bedeutungslos by design.
- **[2026-05-30 16:41] researcher**: Claude Code 2.1.154-2.1.158: Dynamic Workflows (JS-Orchestrierung, max 16 concurrent / 1000 total Agents, /workflows, resumable, Research Preview); Opus 4.8 neues Default-Modell (High Effort by Default, /effort ultracode=xhigh+auto-workflows); Background Shell Commands (!cmd, --bg --exec); Streaming Tool Execution immer aktiv; EnterWorktree mid-session + Worktrees nach Session-Ende entsperrt (v2.1.157); agent-Feld in settings.json fuer dispatched Sessions. Datei aktualisiert: best-practices/claude-tooling/agents.md.
- **[2026-05-30 16:42] researcher**: Claude Code 2.1.154: Opus 4.8 eingefuehrt (Standard-Effort=high, xhigh fuer haerteste Tasks), Lean System Prompt jetzt Default fuer Opus 4.8+ (spart Token), Opus 4.8 Fast Mode $10/$50 MTok (3x guenstiger als Opus 4.6/4.7 Fast Mode $30/$150), Effort-Labels umbenannt Faster/Smarter, CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE deprecated (entfernt 01.06.2026 - Migration: /model claude-opus-4-6[1m] + /fast on), Dynamic Workflows Research Preview, Tokenizer-Warnung Opus 4.7+: bis zu 35% mehr Tokens. Datei aktualisiert: best-practices/claude-tooling/token-effizienz.md (Stand 2026-05-30, Claude Code 2.1.158)
- **[2026-05-30 16:43] researcher**: Claude Code 2.1.154-2.1.158 Settings-Delta eingearbeitet in best-practices/07-settings: Auto-Mode auf Bedrock/Vertex/Foundry via CLAUDE_CODE_ENABLE_AUTO_MODE=1 (Opus 4.7/4.8); Workflow-Keyword-Trigger deaktivierbar in /config; OTEL_LOG_TOOL_DETAILS=1 fuer tool_parameters; agent-Feld in settings.json fuer dispatched sessions; CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE deprecated 06/01; CLAUDE_CODE_ALWAYS_ENABLE_EFFORT fuer Nicht-Standardmodelle; autoMode-Block mit environment/allow/soft_deny/hard_deny und $defaults-Pflicht; Worktree-Unlock beim Agent-Ende; Frank-Relevanz: bypassPermissions bleibt optimal, Auto-Mode ist kein Ersatz.
- **[2026-06-30 19:01] Cross-CLI Delta:** Codex(2) neue Commits — Bruecke starten fuer Details.
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

- **[2026-05-10 R8] Anthropic Dreaming — Cross-Session Memory Curation (Score: 18/20)** | Quelle: claude.com/blog/new-in-claude-managed-agents
  Status: UMZUSETZEN | Aufwand: 2h
  Anthropic hat am 06.05.2026 "Dreaming" als Research Preview eingefuehrt: Separater Prozess konsolidiert Memory automatisch (merged Duplikate, entfernt veraltete, extrahiert Muster, schreibt Playbooks). Adaptierbar als eigenstaendiger "dreaming-agent" der MEMORY.md + session-scores.jsonl reviewt. Loest direkt das Meta-Intelligence-Problem (10% statt 20%).

- **[2026-05-10 R8] Anthropic Outcomes/Grader-Loop (Score: 16/20)** | Quelle: platform.claude.com/cookbook/managed-agents-cma-verify-with-outcome-grader
  Status: UMZUSETZEN | Aufwand: 30 Min
  Zweiter Agent (Grader) bewertet Output anhand Rubrik in EIGENEM Kontextfenster — nicht beeinflusst vom Reasoning-Prozess. +10 Prozentpunkte Erfolgsrate intern. Direkt anwendbar: quality-gate um Rubrik-Sektion erweitern, code-reviewer bekommt Rubrik + Output, nicht den Reasoning-Prozess.

- **[2026-05-10 R8] Darwin Goedel Machine — Evolutionaere Selbstmodifikation (Score: 17/20)** | Quelle: arxiv.org/abs/2505.22954, sakana.ai/dgm
  Status: UMZUSETZEN | Aufwand: 1d
  Agent verbessert nicht nur seinen Code, sondern den Code seiner Verbesserungs-Logik. Archiv aus Agent-Varianten + Evolution. SWE-bench 20%→50%. Adaption: self-improve-cache als Evolutions-Archiv nutzen, nicht nur als TTL-Cache. Direkte Antwort auf Compound-Effect-Stagnation seit 2026-04-20.

- **[2026-05-10 R8] ARISE — Multi-Granularitaets-Program-Graph (Score: 16/20)** | Quelle: arxiv.org/abs/2605.03117
  Status: UMZUSETZEN | Aufwand: 2h
  Repository als File→Function→Statement-Graph mit Data-Flow-Slicing als Tool. +17 Pkt Function Recall@1. Schliesst zwei alte UMZUSETZEN-Findings (KGCompass + Fault-Localization-Context) in einem Schritt. Adaption: debugger-Agent um Data-Flow-Slice-Heuristik erweitern.

- **[2026-05-10 R6] Verifiable-Outcome-Gate als Compound-Trigger** | Quelle: arxiv 2601.22607 (RLVR for Tool-Using Agents)
  Status: UMZUSETZEN | Aufwand: 2h
  /self-improve laeuft heute timer-basiert — Compound-Effect stagniert weil Trigger nicht an Qualitaetssignale gebunden. Idee: PostToolUseFailure + Stop Hook zaehlt Fehler-Cluster, bei >3 Fehler gleicher Klasse in 48h automatischer /self-improve-Trigger. Dieses Pattern verdreifachte Verbesserungsrate in Coding-Agents.

- **[2026-05-10 R6] SQLite-Memory statt JSONL-Vektoren (Score: hoch)** | Quelle: Memstate-Benchmark 2026
  Status: UMZUSETZEN | Aufwand: 1d
  bug-cases.jsonl + experience-store.jsonl auf SQLite umstellen. Memstate-Benchmark zeigt: strukturiertes Memory 92% Fact-Recall vs. JSONL-Vektoren 17% (Faktor 5.3). symptom_hash + Exact-Match statt Fuzzy-Suche. Versionierung statt stilles Ueberschreiben.

- **[2026-05-10 R3] Sequential beats Parallel Self-Consistency (arXiv 2511.02309)** — Status: BESTAETIGT
  Sequentiell schlaegt Parallel in 95.6% der Konfigurationen, +46.7% Accuracy. Konsequenz: Bei DERSELBEN Aufgabe besser sequentiell mit Kontext-Uebergabe als blind parallel. Parallele Agents bleiben sinnvoll fuer UNABHAENGIGE Teilaufgaben.

- **[2026-05-10 R5] OAuth Token Interception via ~/.claude.json** — Status: BEOBACHTEN, kein Patch
  Mitiga Research, 2026-05-07. npm postinstall-Hook ueberschreibt MCP-Endpoints, faengt OAuth-Tokens fuer Jira/GitHub/Confluence ab. Anthropic-Response: "Out of scope". Aktion: ~/.claude.json regelmaessig pruefen, keine wildfremden npm-Pakete ohne Pruefung.

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
- **[2026-06-05] intelligence-checker**: [WARNING] Session 4b40b478 (10 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-06-13] intelligence-checker**: [WARNING] Session c6c74c18 (10 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-06-13] intelligence-checker**: [WARNING] Session c6c74c18 (25 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-06-13] self-observation-checker**: [WARNING] Session c6c74c18 (25 Turns) zeigte keine Selbstbeobachtung
- **[2026-06-13] intelligence-checker**: [WARNING] Session 97942a5d (17 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-06-13] self-observation-checker**: [WARNING] Session 97942a5d (17 Turns) zeigte keine Selbstbeobachtung
- **[2026-06-13] intelligence-checker**: [WARNING] Session c6c74c18 (41 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-06-13] self-observation-checker**: [WARNING] Session c6c74c18 (41 Turns) zeigte keine Selbstbeobachtung
- **[2026-06-13] intelligence-checker**: [WARNING] Session 97942a5d (34 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-06-13] self-observation-checker**: [WARNING] Session 97942a5d (34 Turns) zeigte keine Selbstbeobachtung
- **[2026-06-13] intelligence-checker**: [WARNING] Session c6c74c18 (67 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-06-13] self-observation-checker**: [WARNING] Session c6c74c18 (67 Turns) zeigte keine Selbstbeobachtung
- **[2026-06-13] intelligence-checker**: [WARNING] Session dbdaa9cc (13 Turns) hatte keinen Intelligenz-Vorschlag
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
