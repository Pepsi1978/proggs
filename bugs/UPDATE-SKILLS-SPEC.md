# Spec: Zwei Update-Skills (Almanach + Best-Practices) — für regelmäßige Wellen in Claude Cowork

> Festgehalten 2026-06-15 (Frank-Anweisung). Status: **GEBAUT 2026-06-16** (Frank-OK — der
> best-practices-Skill ist umgebaut, Vorbedingung erfuellt). Skills: `almanach-update` (Skill 1) +
> `best-practices-update` (Skill 2), je echt unter `~/.claude/skills/` + Spiegel in
> `claude-code-setup/skills/` und `Cowork/skills-src/`. Diese Spec bleibt als Anforderungs-Referenz.

## Warum (Frank, 2026-06-15)
Das Re-Recherchieren der Almanache/Best-Practices (z.B. "Welle 3" = älteste Kern-Almanache auf
aktuelle Software-Version heben) soll künftig REGELMÄSSIG laufen — aber in **Claude Cowork**, nicht
im CLI. Grund: Cowork hat momentan bessere Limits, verbraucht Franks CLI-Token nicht so schnell.
Deshalb: die Welle-Logik als wiederverwendbaren Skill kapseln, Cowork-tauglich machen, rüberschicken.

## Skill 1 — Almanach-Update-Skill (PRIORITÄT)
**Zweck:** Geht SÄMTLICHE bestehenden Almanache unter `~/proggs/bugs/<kategorie>/<bereich>.md` durch
und hebt sie auf die jeweils aktuelle Software-Version (Re-Recherche-Welle). Kein neuer Almanach —
Aktualisierung bestehender. Basiert auf dem bestehenden Skill `bug-almanach-recherche` (7 Schritte),
aber als BATCH/WELLE über viele Almanache, mit Auswahl-Logik.

**Auswahl, welche Almanache dran sind (konfigurierbar):**
- Default: die ÄLTESTEN nach Stand-Datum zuerst (Stand-Header `> **Stand:** … DATUM`), bzw. alle mit
  Stand-Verfall (Richtwert > ~14 Tage oder Hochrisiko-Bereiche bei jedem Software-Versionssprung).
- Optional: nur eine Kategorie ("nur android"), nur eine Liste, oder "alle".
- Welle-3-Beispiel war: claude-hooks, kotlin, jetpack-compose, gradle, firebase-billing.

**Pro Almanach der erprobte 7-Schritte-Ablauf (aus bug-almanach-recherche):**
1. Version(en) der JEWEILIGEN Software LIVE ermitteln (nicht raten): claude --version / kotlinc -version
   bzw. Plugin-Version aus build.gradle.kts/libs.versions.toml / ./gradlew --version / AGP+compileSdk /
   Compose-BOM / Billing-Dep-Version. Mehrere Versionen pro Android-Projekt sind normal → alle in den Stand-Header.
2. 7 Researcher GLEICHZEITIG (Continuous-Spawning, NIE Workflow, max 7, ab ~12 RPM-Absturz). Teilbereiche:
   offizielle Doku/Changelog, Issue-Tracker, Community/Praxis, Plattform-Fallen (Win+macOS), Mechanik,
   Fix-Status-Changelog, neue Features. Jeder Researcher: max 15 Fetches, max 10 Min, KEIN Eintrags-Cap.
   WICHTIG: Researchern den bestehenden Almanach-Stand mitgeben → sie suchen gezielt NUR NEUES seit Stand X.
3. Fix-Status HART per gh prüfen (HAUPTAGENT, Researcher haben kein Bash):
   gh issue view <nr> --repo <org>/<repo> --json number,state,title,closedAt,stateReason
   → NOT_PLANNED = won't fix (Workaround bleibt DAUERHAFT), COMPLETED = echt gefixt, DUPLICATE = gebündelt.
   Mehrere Researcher meldeten falsche/unsichere Versionen — gh ist die Grundwahrheit. Issue-Nummern von
   den Researchern sammeln lassen, dann selbst verifizieren.
4. Best-Practices-Abgleich BEIDE Richtungen: lesen (grep best-practices/ → bekannte Lösung in FIX
   aufnehmen) UND schreiben (allgemeingültige Prävention nach best-practices/ zurückspeisen). Bezugs-
   Tabellen (🔗) synchron halten, wenn beide Dateien existieren.
5. Kuratieren: deduplizieren, thematisch gruppieren, Format pro Bug (Symptom/Ursache/Versionen/FIX/Quelle),
   Kurzcheck-Tabelle UND Volltext pflegen, Fix-Status-Sektion mit gh-Status, Methodik-Hinweis.
6. Ins System einhängen: bugs/README.md (Stand-Datum + Bug-Anzahl), Hook-Mapping nur bei neuem Dateimuster.
7. Committen + pushen pro Almanach (eigene Pfade namentlich).

## Skill 2 — Best-Practices-Update-Skill
**Zweck:** Analog für `~/proggs/best-practices/` (Harness-Themen `claude-tooling/<thema>.md` UND
best-practices/<kategorie>/<software>.md). Basiert auf bestehendem Skill `best-practices`.
Gleicht gegen die jeweils passenden offiziellen Changelogs/Docs ab (Claude-Code-Changelog für Harness,
Software-Changelog für Projekt-Code), hebt Stand-Header, koppelt gefundene Bugs in die Almanache zurück.
Selbe Researcher-/gh-/Persistenz-Disziplin wie Skill 1. Bezugs-Tabellen synchron halten.

## Cowork-Umwandlung (PFLICHT vor dem Rüberschicken — beide Skills)
Siehe `~/.claude/rules/cowork-git-push.md`. Kernpunkte für Cowork-Logik:
- Git NIEMALS nackt: IMMER `bash ~/proggs/cowork-git.sh push-files "#NNN - Text" <datei...>` (gezielt) bzw.
  `setup` zuerst ("Push-Zugang OK" abwarten). Mount-Fallen (Lock/BOM/LFS/Symlink/Build-Berge) fängt das Skript ab.
- Ein Cowork-Shell-Aufruf läuft max ~45s, Hintergrundprozesse überleben den Wechsel NICHT → Researcher-
  Schwarm + Einarbeitung muss in tragfähige Häppchen pro Aufruf. (Researcher laufen aber als Agenten, nicht als Shell-Hintergrund.)
- Datei-Schreiben über die Mount-Brücke kann abgeschnitten sein → nach dem Schreiben Dateiende prüfen
  (tail -1, wc -l) ODER git-intern bauen. Datenverlust-Wächter im cowork-git.sh nutzen.
- Token-Persistenz in ~/proggs/.git/credentials. Nie roh ins Log.

## Erprobtes Muster aus dem claude-hooks-Lauf (2026-06-15, als Referenz)
- 7 Researcher (Doku/Issues/Community/Plattform/Mechanik/Fix-Status/Features) liefen einwandfrei, keiner abgestürzt.
- gh-Prüfung war ENTSCHEIDEND: korrigierte mehrere Researcher-Statusangaben (z.B. #24327/#21988/#15664/#40280/
  #16047/#13650 = COMPLETED; viele andere = NOT_PLANNED = won't fix; #17088/#54743/#59939 = OPEN).
- Ergebnis: Almanach §16 (neue Bugs) + §15 (Fix-Status gh-hart) + Kurzcheck +4 Zeilen; BP 01-hooks +5 Einträge
  + Feldkorrektur modifiedInput→updatedInput; README-Stand. Commit #46802.
- Lehre: Researcher-Web-Snippets sind unzuverlässig bei Versions-/Status-Angaben → gh ist Pflicht-Gegenprobe.
- Lehre: bestehende Almanach-/BP-Datei ist oft schon umfangreich → Researchern Stand mitgeben, nur Delta holen.

## Offene 4 Almanache aus Welle 3 (vom Skill nachzuholen, NICHT manuell)
kotlin (Stand 2026-06-02), jetpack-compose (2026-06-02), gradle (2026-06-02), firebase-billing (2026-06-02).

## Neues Logik-System (W3, 2026-06-15) — die Update-Skills MUESSEN es befolgen
Die bestehenden Skills `bug-almanach-recherche` (Schritt 6, Punkte 3-5) und `best-practices` (Schritt 5)
wurden 2026-06-15 an diese Mechanismen angepasst — die beiden NEUEN Update-Skills erben/uebernehmen sie:
- **Versions-Anker (W3-1):** Jeder software-gebundene Almanach traegt `> **Anker:** <label>=<version>` unter
  dem Stand-Header (SYSTEM.md §7). Bei Live-abgleichbarer Software (installiert==relevant): Eintrag in
  `bugs/check-version-anchor.py` → `ANCHORS` mit `live`-Tupel; projekt-gebunden → `live: None`.
- **Semantischer Prompt-Trigger (W3-2):** Neue/aktualisierte Bereiche mit eindeutigen Stichwoertern in
  `~/.claude/hooks/bug-almanac-hint.py` → `AREAS` pflegen (+ Repo-Spiegelung).
- **Self-Test:** Nach jeder Welle `python bugs/health.py` — alle VIER Checks gruen (coupling, guard-coverage,
  version-anchor, Stand-Verfall), bevor committet wird.
- **gh-Status-Pflicht (aus der claude-hooks-Re-Recherche 2026-06-15):** Researcher-Web-Snippets sind bei
  Versions-/Status-Angaben unzuverlaessig; der Hauptagent verifiziert jeden Issue-Status HART per
  `gh issue view <nr> --repo <org>/<repo> --json state,stateReason,closedAt` (NOT_PLANNED=won't fix,
  COMPLETED=gefixt). Das war im claude-hooks-Lauf entscheidend (korrigierte mehrere Researcher-Angaben).
- **Memory-Governance (W3-3):** Nicht Teil der Almanach/BP-Skills, aber verwandt: `claude-code-setup/tools/memory-staleness.py`.
