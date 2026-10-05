# Cloud-Regeln (nur Claude-Code-Cloud-Sitzungen)

Diese Regeln gelten nur in Cloud-Sitzungen (claude.ai/code, Claude-App am Handy, `CLAUDE_CODE_REMOTE=true`).
Frank programmiert von unterwegs am Handy. Er hat keine Zeit für Rückfragen: Ziel erkennen und auf dem schnellsten
Weg umsetzen. Bearbeitet werden diese Regeln in OpenLauncher über den Knopf „Cloud-Regeln“.

1. Sprache
- Alle Ausgaben an Frank auf Deutsch mit echten Umlauten (ä ö ü Ä Ö Ü ß), nie „ae“, „oe“, „ue“, „ss“.
- Kurz antworten: Frank liest am Handy.

2. Was die Cloud nicht hat
- Kein `C:\Users\barwa\SK`, keine OpenLauncher-Profile, kein adb, kein Handy, kein Emulator, kein Browser.
  Regeln aus der Repo-`AGENTS.md` zu diesen Dingen (SK-Ordner, adb/WLAN, Emulator, Update-Skripte für
  OpenLauncher/TVO/CVO) gelten hier nicht.
- Geht etwas nur am PC: es Frank kurz sagen, statt es zu versuchen.
- Genau ein Repo pro Sitzung (`proggs`). Bei mehreren Repos lädt die Cloud keine Hooks.

3. Secrets und Schlüssel
- Die Cloud-Sitzung bekommt keine Secrets und braucht keine. Nie danach fragen, nie welche ins Repo schreiben.
- Für den Android-Bau liegen der geteilte Signier-Keystore und die übrigen Bau-Secrets (Google-Drive-Zugang für
  rclone, Maps-Schlüssel für EntropieReductor) sicher bei GitHub im Environment **`android-signing`**. Nur der
  Ablauf `.github/workflows/android-cloud-build.yml` darf sie benutzen, und nur auf `main`. Deshalb baut und
  signiert GitHub nach dem Merge; die Cloud-Sitzung signiert nie selbst.
- API-Schlüssel kommen nie in eine APK. Frank trägt sie in der App in den Einstellungen ein.

4. Android-Apps veröffentlichen
- Nach jeder fertigen Änderung an einer Android-App ohne Rückfrage den Skill **`apk-update-cloud`** ausführen
  (Anleitung: `OpenLauncher/Profiles/ClaudeCode/standard/skills/apk-update-cloud/SKILL.md`): Versionslog-Eintrag,
  Commit, Push, PR (nicht als Entwurf), bei „Ja“ Codex-Review anstoßen und Befunde fixen, selbst mergen, Bau prüfen.
- Ausnahme: BestJournalAndroid (Play-Store-Key) geht nur am PC.
- Versionslog: `<App>/app/src/main/assets/versionslog.json`, genau ein Eintrag pro Veröffentlichung, `versionCode` =
  letzter + 1, `versionName` letzte Stelle + 1. `build.gradle.kts` nie von Hand ändern.
- Uhrzeit: Die Cloud-VM läuft in UTC. Zeit immer per Befehl in deutscher Zeit holen:
  `TZ=Europe/Berlin date "+%d.%m.%Y, %H:%M Uhr"`. Nie schätzen.

5. Version und Zeitstempel (alle Projekte)
- Bump die Version genau einmal pro Commit, sichtbar. Der Bump gehört in denselben Commit wie die Änderung.
- Datum und Uhrzeit vorher per Befehl holen (die Cloud-VM läuft in UTC, `Get-Date` gibt es hier nicht):
  `TZ=Europe/Berlin date "+%d.%m.%Y, %H:%M Uhr"`, für Zeitstempel-Suffixe `TZ=Europe/Berlin date "+%Y%m%d.%H%M"`.
- Die Zeit nie aus dem Kontext, aus einer früheren Nachricht, aus dem Gedächtnis oder aus einer Anzeige in der App
  übernehmen. Nie schätzen.
- Wo die Version steht, hängt von der Plattform ab:
  - Android: `app/src/main/assets/versionslog.json`, pro Commit einen Eintrag unten anhängen:
    `{ "versionCode": <letzter + 1>, "versionName": "1.0.28", "stand": "19.07.2026, 21:00 Uhr", "notiz": "<was geändert wurde>" }`.
    versionCode immer um 1 erhöhen, sonst erkennt UpdateStation das Update nicht. Uhrzeit mit Doppelpunkt, nicht
    mit Punkt. `build.gradle.kts` liest versionCode, versionName und VERSION_BUMPED_AT aus dem Log, dort nie von
    Hand eintragen. Neue Android-Apps bekommen den Versionslog von Anfang an (Vorlage: UpdateStation).
  - .NET/WPF: `<Version>` in der `.csproj`, Form `2.1.84`.
  - Node/TypeScript: `version` in der Wurzel-`package.json`. Bei WerftStudio mit Zeitstempel-Suffix, Form
    `0.32.2-20260809.1545`.
- Wird die Version in der App angezeigt (meist im Einstellungs-Bildschirm, sonst auf anderen Seiten suchen), zieh
  die Anzeige mit.
- Weicht ein Projekt von diesen Mustern ab, steht die Regel in dessen eigener CLAUDE.md. Findest du dort nichts und
  passt kein Muster: frag nach, rate nicht.
- Bauen geht in der Cloud bei Nicht-Android-Projekten meist nicht (kein Windows, kein SDK). Dann: Code ändern,
  Version bumpen, PR öffnen, selbst mergen und Frank sagen, dass am PC gebaut/installiert werden muss.

6. Commits und Git
- Commit-Messages: `<Projekt>: <was geändert wurde>`, klein, imperativ, eine Zeile, Deutsch mit Umlauten.
- Push geht nur auf den eigenen Branch `claude/…`. Nach `main` kommt alles über einen Pull Request, den die
  Sitzung selbst mergt (Ausnahme: Änderungen an `.github/workflows/**` erst nach Franks OK).
- **Kein PR bleibt liegen.** Das gilt für JEDE Änderung, nicht nur für Apps: Skills, Hooks, Regeln, Doku,
  Einstellungen. PR nie als Entwurf öffnen, dann selbst mergen (GitHub-MCP `merge_pull_request`, `gh` fehlt in der Cloud oft). Frank arbeitet nur mit `main`;
  ein offener Branch gilt für ihn als nicht umgesetzt.
- **Codex-Review nur auf Wunsch.** Codex prüft nicht mehr automatisch. Sobald die Programmierung fertig ist
  (vor Commit und PR), Frank mit `AskUserQuestion` fragen: „Codex-Review drüber laufen lassen?“ Ja / Nein.
  Nicht fragen, wenn der Auftrag „mit Codex“ oder „ohne Codex“ enthält. Bei **Ja**: nach dem PR
  `@codex review` in den PR kommentieren, mit `warte-auf-codex.sh <N> 480 <Kommentar-ID>` warten (aus
  `apk-update-cloud` Schritt 5, höchstens 8 Minuten), Befunde fixen, dann mergen. Bei **Nein**: kein
  Codex, sofort mergen, weiter wie ohne Review.
- Geht der Merge nicht (Workflow-Ausnahme, Auto-Modus blockiert, Konflikt nicht lösbar), steht im Abschlussblock
  statt „PR gemergt: ja“ gut sichtbar: `⚠️ PR #<n> NICHT gemergt: <Grund>`.
- Meldet der Start-Hinweis offene Cloud-PRs aus früheren Sitzungen, Frank zu Beginn darauf hinweisen und fragen,
  ob sie gemergt oder geschlossen werden sollen.
- Nur eigene Dateien stagen, nie `git add -A`.

7. Wissen im Repo nutzen
- Bekannte Cloud-Fallen: `bugs/claude-tooling/claude-code-cloud-kurzcheck.md`. Bei jedem Fehler in der Cloud den
  Volltext daneben lesen.
- Best Practices und Bug-Almanache liegen in `best-practices/` und `bugs/`. Vor Arbeit in einem Bereich den
  passenden Kurzcheck lesen.
- Skills liegen zentral in `OpenLauncher/Profiles/ClaudeCode/standard/skills/<name>/SKILL.md`. Die Cloud lädt sie
  nicht automatisch: Wird ein Skill gebraucht, die SKILL.md direkt lesen und befolgen.
- „Fixe nach Direktive 3“: zuerst `claude-code-setup/docs/rules/resilient-bugfixing.md` vollständig lesen.
- Web-Recherche: Die Recherche-Skripte brauchen Schlüssel aus SK und laufen hier nicht. Mit den eigenen
  Such-Werkzeugen recherchieren und die Erkenntnisse trotzdem in `best-practices/` und `bugs/` speichern.

8. Aufgabentrennung und Ablauf
` ; ` (Leerzeichen, Semikolon, Leerzeichen) trennt eigenständige Aufgaben.
- Ein leerer Teil am Ende zählt nicht mit. Semikola in Code, SQL oder URLs trennen nicht.
- Ordne die Aufgaben nach ihren Abhängigkeiten.
- Widersprechen sich zwei Aufgaben: frag nach, bevor du anfängst.
- Ab zwei Aufgaben: zeig vorab die nummerierte Liste in der Reihenfolge, in der du sie abarbeitest.

9. Abschluss (Pflicht, jede Antwort mit Änderungen endet so)
Immer genau dieser kurze Block am Ende. Die erste Zeile zeigt Frank, dass diese Regeln geladen wurden.
Was nicht zutrifft, mit „entfällt“ füllen, was fehlschlug, mit „nein (Grund)“, nie weglassen:
```
Cloud-Sitzung · Cloud-Regeln (cloud.md) erkannt ✓
Geändert: <ein Satz>
Version: <App> <alt> → <neu> (versionCode <n>)
Codex-Review: <übersprungen (Nein) | keine Befunde | n Befunde, behoben | kein Ergebnis nach 8 min>
Commit + Push: ja · PR #<n> gemergt: ja
GitHub-Bau: grün · Google Drive: hochgeladen · UpdateStation: verfügbar
```
Bei Aufgaben ohne App-Änderung: Version, Bau, Drive und UpdateStation mit „entfällt“ (Codex je nach Ja/Nein).
