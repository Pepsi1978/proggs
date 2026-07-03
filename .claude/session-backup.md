# Session Handoff — 2026-07-03, ca. 22:55 Uhr

## Ziel (1-3 Saetze)
Grosse Wartungs-Session an EntropieReductor: Pager-Swipe-Fix, Tiefen-Debugging (Bug- + Performance-Brille), Room-Migrations-Haertung, LWW-Sync-Sonden, Pre-Commit-Guard — und zuletzt Diagnose + Recherche zum Strava-Sync-Ausfall (403). Alles ist committed und gepusht; es laeuft KEINE Aufgabe mehr.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Das Backup kam direkt nach der finalen Antwort auf Franks Strava-Frage.

## Aktueller Status
- Erledigt (alles committed + gepusht, #47440–#47453):
  - #47440/#47441: Sub-Tab-Pager-Swipes fluessig (Root Cause: staticCompositionLocalOf + un-remembered MicActionsState invalidierte alle 4 Seiten pro Swipe; SubTabRow offset-Lambda). Version 0.17.43. Compose-Almanach 1.7 + Kurzcheck #19 dokumentiert.
  - #47442/#47443: Tiefen-Debugging Bug-Brille ueber die GANZE App: @Upsert statt @Insert(REPLACE) auf 4 CASCADE-Eltern-DAOs (stiller Followup-/Permission-Verlust bei jedem Update+Drive-Sync!), 45x CancellationException-Rethrow in 38 Dateien, TtsPlayer AC1/AC4/AC9 (atomarer Cache, Listener-Nulling, Instanz-Guards), Polar-Stream-Leak. Version 0.17.44, installiert, crash-frei. Near-Miss: doppeltes @Synchronized (Regel ergaenzt: grep -B5 vor Annotation-Einfuegungen, #47445).
  - #47446: Pre-Commit-Guard .githooks/android-bug-guard.py blockt @Insert(REPLACE) auf CASCADE-Eltern (Override: COMMIT_ALLOW_REPLACE_CASCADE=1). Fand sofort echten Verstoss in BestJournal JournalEntryDao → gefixt (@Upsert, v0.21.15, installiert). CancellationException-Guard existierte schon (tools/check_cancellation_rethrow.py, andere Session 2026-07-02).
  - #47447: Room-Datenverlust-Bombe entschaerft: AppDatabase fallbackToDestructiveMigrationFrom(true, 1..9) statt pauschal destruktiv; Schema-JSONs 1-35 + Scientist 1-2 aus EntropieReductor/.gitignore befreit und eingecheckt. DiagnosticLog + JournalMirror behalten Fallback bewusst (Wegwerf-/Spiegeldaten).
  - #47448: LWW-Merge-Checkpoint-Sonden in SyncEntriesUseCase (CHECKPOINT sync=lww_merge + followup_bestand-Invariante als Multi-Device-Verlust-Detektor). Version 0.17.45. Live verifiziert (ok=true).
  - #47449/#47450: Performance-Brille: Heavy/Finalize-Split in Hrv/Recovery/DeepSleepGraphCard (remember-Key enthielt Scrub-State → groupBy-Pipeline lief pro Frame; Compose-Almanach 10.9 + Kurzcheck #20), Whoop-Snapshot-Batch-Restore (1 Transaktion statt ~300/Start). Version 0.17.46, installiert, crash-frei.
  - #47451/#47452/#47453: Strava-403-Diagnose: Sonde loggt jetzt errorBody (v0.17.47 installiert). Root Cause BEWIESEN: {"resource":"Application","field":"Status","code":"Inactive"} = Strava-Abo-Pflicht fuer API-Apps seit 30.06.2026 (Franks Konto = "Kostenloses Kundenkonto"). Firecrawl-Recherche (3 Researcher) bestaetigt: regulaeres Abo reicht (keine Extra-Gebuehr), Reaktivierung MANUELL auf strava.com/settings/api, 3-Monats-Gratis-Code d0a2074c43 fuer Bestands-Entwickler. Neuer Almanach bugs/apis/strava-api.md + Best Practices + README-Index + hint-Hook-Registrierung (getestet, in beide Spiegel gespiegelt).
- In Arbeit: nichts.
- Blockiert (wartet auf Frank):
  1. Strava: Frank entscheidet Abo (mit Gratis-Code) + manuelle Reaktivierung ODER Polar-AccessLink-Direktanbindung als Umbau. Seite strava.com/settings/api war im Browser offen.
  2. Handy-Tests unbestaetigt: fluessiges Sub-Tab-Wischen (0.17.43+) und Gesamtverhalten 0.17.47 — bei Franks JA die bestaetigten Bugfixes gesammelt ins zweite Gehirn (bugfix-to-second-brain, Format "Bugfix <App> <Bereich> <Datum HH:MM>").

## Relevante Dateien
- `EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/repository/StravaRepository.kt` — 403-Sonde in handleSyncFailure
- `EntropieReductor/app/src/main/java/de/frank/entropyreducer/di/DatabaseModule.kt` — neue Migrations-Politik
- `EntropieReductor/app/src/main/java/de/frank/entropyreducer/domain/usecase/SyncEntriesUseCase.kt` — LWW-Checkpoints
- `.githooks/android-bug-guard.py` + `.githooks/pre-commit` — neuer CASCADE-Guard
- `bugs/apis/strava-api.md` (+ -kurzcheck, + best-practices/apis/strava-api*) — neuer Almanach
- `~/.claude/hooks/bug-almanac-hint.py` — Strava-AREAS-Eintrag (gespiegelt in claude-code-setup/hooks + Umgebung/Hooks)

## Getroffene Entscheidungen
- @Upsert nur auf den 4 CASCADE-Eltern-DAOs (minimal-invasiv); andere REPLACE-Stellen ohne CASCADE-Kinder bewusst unveraendert.
- fallbackToDestructiveMigration NUR fuer Uralt-Versionen <10 erlaubt (fuer die keine Migrationen existieren); ab v10 lauter Crash statt stiller Datenverlust.
- Strava-Guard-Registrierung nur im hint-Hook (apis = Konzept-Bereich, kein Datei-Guard — konsistent mit anderen APIs).
- Start-Restore re-upserted weiterhin ~1300 unveraenderte Zeilen pro App-Start — Diff-Filter bewusst NICHT gemacht (Liste b: createdAt-Semantik + 8 Tabellen, eigene Aufgabe).

## Fehlgeschlagene Ansaetze
- Erste Strava-Hypothese "Consent-Haekchen/Scope fehlt nach Fresh-Install" war FALSCH — der 403-Body bewies "Application Inactive" (Abo-Pflicht). Nicht wieder auf Scope-Verdacht einsteigen.
- Annotation-Einfuegung per Batch ohne Blick auf Zeilen UEBER der Signatur → doppeltes @Synchronized (Build-Fehler). Regel ergaenzt (#47445), nicht wiederholen.
- Lange Python-Heredocs in Bash brechen am Quoting → Skript als Datei ins Scratchpad schreiben und ausfuehren.

## Wichtige Recherche-Ergebnisse
- Strava-API-Abo-Pflicht seit 30.06.2026 (Standard-Tier): regulaeres Strava-Abo reicht, keine API-Extra-Gebuehr; Reaktivierung manuell im API-Dashboard; Gratis-Code d0a2074c43 (3 Monate) fuer aktive Bestands-Entwickler; Limits/Scopes unveraendert (200/15min, 2000/Tag). Quellen: communityhub.strava.com (offizielle Antwort Emily_A), developers.strava.com, forum.intervals.icu. Alles in bugs/apis/strava-api.md persistiert.

## Naechste Schritte (priorisiert)
1. Franks Rueckmeldung zu Strava abwarten: Abo+Reaktivierung (dann Biomarker-Refresh + Log pruefen: adb logcat | grep "Diag/STRAVA") ODER Polar-AccessLink-Umbau starten.
2. Nach Franks Test-JA: bestaetigte Bugfixes (Pager-Swipe, Tiefen-Debugging-Paket, Strava-Diagnose) ins zweite Gehirn (second-brain remember, Kategorie bugfixes/entropie-reductor).
3. Offene Empfehlungen aus den Berichten: Diff-Filter fuer Start-Restore (~1300 redundante Writes/Start), 403-Body-Sonde auch fuer Whoop/Oura/TTS, Strava-Inactive-Banner im Biomarker-Reiter, Release-Build-Test beider Apps.

## Offene Fragen
- Macht Frank das Strava-Abo (ggf. mit Gratis-Code) oder soll die Polar-AccessLink-Direktanbindung gebaut werden?
- Handy-Tests: Wischen fluessig? App laeuft normal? (fuer Second-Brain-Doku)

## Anker
- Branch: main
- Letzte Commits:
15acb1890 #47453 - Strava API almanac (subscription requirement since 2026-06-30, Application Inactive, reactivation flow, scope/rate-limit traps) + best practices + README index + hint-hook registration (mirrored)
e11365b15 #47452 - Bug-case: Strava 403 root cause = Application Status Inactive (Strava subscription now required for API apps); probe proved it in minutes
074abce00 #47451 - Strava 403 diagnostic probe: log error body (was discarded) + plain-text scope hint (activity:read_permission missing after fresh-install re-auth); version 0.17.47
caedb2f3c #47450 - RETAIN performance findings: compose almanac 10.9 + quickcheck #20 (too-broad remember key defeats memoization on scrub frames) + 2 bug-cases
9ff4138f9 #47449 - Performance sweep EntropieReductor: split history-heavy groupBy pipelines out of scrub-frame remember keys in Hrv/Recovery/DeepSleep graph cards (was recomputing full history per finger move), pre-reverse row lists, batch Whoop snapshot restore (1 transaction instead of ~300 per app start); version 0.17.46
