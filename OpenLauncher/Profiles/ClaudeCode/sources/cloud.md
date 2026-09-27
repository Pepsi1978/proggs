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
  Commit, Push, PR (nicht als Entwurf), Codex-Review abwarten und Befunde fixen, selbst mergen, Bau prüfen.
- Ausnahme: BestJournalAndroid (Play-Store-Key) geht nur am PC.
- Versionslog: `<App>/app/src/main/assets/versionslog.json`, genau ein Eintrag pro Veröffentlichung, `versionCode` =
  letzter + 1, `versionName` letzte Stelle + 1. `build.gradle.kts` nie von Hand ändern.
- Uhrzeit: Die Cloud-VM läuft in UTC. Zeit immer per Befehl in deutscher Zeit holen:
  `TZ=Europe/Berlin date "+%d.%m.%Y, %H:%M Uhr"`. Nie schätzen.

5. Andere Projekte (keine Android-App)
- .NET/WPF: `<Version>` in der `.csproj` erhöhen. Node/TypeScript: `version` in der Wurzel-`package.json`.
- Bauen geht in der Cloud meist nicht (kein Windows, kein SDK). Dann: Code ändern, Version bumpen, PR öffnen, selbst
  mergen und Frank sagen, dass am PC gebaut/installiert werden muss.

6. Commits und Git
- Commit-Messages: `<Projekt>: <was geändert wurde>`, klein, imperativ, eine Zeile, Deutsch mit Umlauten.
- Push geht nur auf den eigenen Branch `claude/…`. Nach `main` kommt alles über einen Pull Request, den die
  Sitzung selbst mergt (Ausnahme: Änderungen an `.github/workflows/**` erst nach Franks OK).
- **Kein PR bleibt liegen.** Das gilt für JEDE Änderung, nicht nur für Apps: Skills, Hooks, Regeln, Doku,
  Einstellungen. PR nie als Entwurf öffnen, Codex-Review abwarten (Skript `warte-auf-codex.sh` aus
  `apk-update-cloud` Schritt 5: kehrt sofort zurück, sobald Codex fertig ist, höchstens 8 Minuten), dann selbst mergen (`gh pr merge <N> --merge`). Frank arbeitet nur mit `main`;
  ein offener Branch gilt für ihn als nicht umgesetzt.
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

8. Abschluss (Pflicht, jede Antwort mit Änderungen endet so)
Immer genau dieser kurze Block am Ende. Die erste Zeile zeigt Frank, dass diese Regeln geladen wurden.
Was nicht zutrifft, mit „entfällt“ füllen, was fehlschlug, mit „nein (Grund)“, nie weglassen:
```
☁️ Cloud-Sitzung · Cloud-Regeln (cloud.md) erkannt ✓
Geändert: <ein Satz>
Version: <App> <alt> → <neu> (versionCode <n>)
Codex-Review: <keine Befunde | n Befunde, behoben | kein Ergebnis nach 8 min>
Commit + Push: ja · PR #<n> gemergt: ja
GitHub-Bau: grün · Google Drive: hochgeladen · UpdateStation: verfügbar
```
Bei Aufgaben ohne App-Änderung: Version, Codex, Bau, Drive und UpdateStation mit „entfällt“.
