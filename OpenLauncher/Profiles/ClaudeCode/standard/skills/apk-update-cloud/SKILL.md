---
name: apk-update-cloud
description: >
  Veröffentlicht in einer Claude-Code-CLOUD-Sitzung eine geänderte Android-App vollautomatisch aufs
  Handy: Frank fragen, ob ein Codex-Review laufen soll (Ja/Nein), Versionslog-Eintrag anhängen,
  committen, Pull Request öffnen, bei Ja Codex-Review anstoßen, abwarten und Befunde fixen, selbst
  mergen, GitHub Actions
  baut und signiert mit dem geteilten Key und legt APK + update.json nach Google Drive
  "Dokumente/Updates/<Projekt>/", UpdateStation zeigt das Update. Nutze diesen Skill IMMER in einer
  Cloud-Sitzung (Umgebungsvariable CLAUDE_CODE_REMOTE=true, kein C:\Users\barwa\SK), sobald eine
  Änderung an einer Android-App fertig ist, auch ohne dass Frank etwas sagt, und wenn er sagt:
  "apk update", "APK-Update", "bau die App", "aufs Handy", "Update bereitstellen". NICHT am Windows-PC,
  dort gilt der Skill apk-update mit dem Skript.
---

# APK-Update aus der Cloud (vollautomatisch)

Frank hat am 26.09.2026 dauerhaft freigegeben: In Cloud-Sitzungen wird jede fertige Änderung an einer
Android-App **ohne Rückfrage** veröffentlicht, einschließlich des Merges auf `main`. Er soll nichts
tippen müssen außer „Installieren“ in UpdateStation.

**Einzige Frage (seit 05.10.2026):** ob ein Codex-Review laufen soll. Codex prüft Pull Requests nicht
mehr automatisch (Frank hat die automatischen Reviews in den Codex-Einstellungen abgeschaltet), sondern
nur noch, wenn die Sitzung `@codex review` in den PR schreibt. Große Aufgaben bekommen so ein Review,
kleine Fixes gehen ohne Wartezeit raus.

## Wann

- Automatisch am Ende jeder Aufgabe, die Dateien unter `<Projekt>/app/**` oder die Gradle-Dateien eines
  Android-Projekts (Ordner mit `app/build.gradle.kts`) geändert hat.
- Nicht bei: `BestJournalAndroid` (Play-Store-Key, geht nur am PC), halbfertigen Änderungen, reinen
  Doku-Änderungen, oder wenn Frank ausdrücklich „nicht veröffentlichen“ sagt.
- Nie am PC ausführen (`CLAUDE_CODE_REMOTE` ist dort nicht `true`).

## Ablauf

0. **Codex-Frage, sobald die Programmierung fertig ist** (vor Schritt 1, damit der Rest ohne Unterbrechung
   durchläuft): Werkzeug `AskUserQuestion` mit genau einer Frage, Kopf „Codex“, zwei Antworten:
   - „Ja“ – Codex prüft den PR, ich warte und behebe Befunde, dann wird gebaut.
   - „Nein“ – sofort mergen und bauen, ohne Codex.
   Keine Empfehlung markieren. Nicht fragen, wenn Frank es schon im Auftrag gesagt hat („mit Codex“ =
   Ja; „ohne Codex“, „kein Codex“ = Nein). Mehrere Runden in einer Sitzung: pro PR neu fragen.
1. **Versionslog:** Unten in `<Projekt>/app/src/main/assets/versionslog.json` genau **einen** Eintrag
   anhängen: `versionCode` = letzter + 1, `versionName` letzte Stelle + 1, `stand` = jetzt in
   deutscher Zeit (`"26.09.2026, 18:05 Uhr"`), `notiz` = kurz auf Deutsch, was neu ist. Pro
   Veröffentlichung nur ein Eintrag. Fehlt die Datei: siehe Abschnitt Versionslog im Skill
   `OpenLauncher/Profiles/ClaudeCode/standard/skills/apk-update/SKILL.md`.
2. **Prüfen, soweit es in der Cloud geht:** JSON gültig (`python3 -m json.tool`), Diff noch einmal
   kritisch lesen. Ein Gradle-Bau braucht hier ein Android SDK, das meist fehlt. Die echte Bauprüfung
   macht GitHub in Schritt 5. **Nie** selbst signieren, **nie** `apk-update.ps1` starten, **nie** nach
   dem Keystore fragen, **nie** eine APK selbst hochladen.
3. **Committen und pushen** auf den Arbeits-Branch der Sitzung (nur die eigenen Pfade stagen).
   Push geht nur auf den eigenen `claude/…`-Branch, nie direkt auf `main`. Scheitert er mit 403
   („authorized repository set“ leer, Bug B3): nicht herumprobieren, Frank melden, dass die Sitzung mit
   dem Repo `proggs` als Quelle neu gestartet werden muss.
4. **Pull Request öffnen, nicht als Entwurf** (GitHub-MCP `create_pull_request` mit `draft: false`,
   oder `gh pr create --base main --title … --body …`, falls `gh` da ist). Titel und Text selbst setzen (sonst nimmt GitHub den ältesten Commit).
   Nie erst als Entwurf öffnen und dann auf „bereit“ setzen (falls Codex doch noch automatisch prüft,
   löst das ein zweites Review aus).
5. **Codex-Review, nur bei „Ja“ in Schritt 0.**
   - **Nein** → Schritt 5 ganz überspringen, direkt Schritt 6. Kommentiert Codex trotzdem (automatische
     Reviews noch an), Befunde ignorieren und im Abschlussblock „übersprungen (Nein)“ schreiben; Frank
     einmal darauf hinweisen, dass Codex noch automatisch prüft.
   - **Ja** → Review anstoßen: Kommentar `@codex review` in den PR schreiben (GitHub-MCP
     `add_issue_comment`, `issue_number` = PR-Nummer). Die `id` des Kommentars aus der Antwort merken.
     Prüft Codex schon automatisch (Kommentar oder 👀 von `chatgpt-codex-connector[bot]` vor deinem
     Kommentar), keinen Kommentar schreiben, sondern nur warten.

   **Abwarten (höchstens 8 Minuten, aber keine Sekunde länger als nötig):** Direkt nach dem Kommentar
   das Warte-Skript starten, als Bash-Befehl mit dem **höchsten Zeitlimit (600000 ms)**:
   `bash OpenLauncher/Profiles/ClaudeCode/standard/skills/apk-update-cloud/warte-auf-codex.sh <N> 480 <Kommentar-ID>`
   (die Kommentar-ID braucht das Skript, weil Codex sein 👍 bei Auftrag per Kommentar an den Kommentar
   hängt, nicht an den PR).
   Es fragt alle 15 Sekunden per REST nach und kehrt **sofort** zurück, sobald Codex fertig ist
   (`CODEX=ok`, `CODEX=befunde (…)` oder nach 8 min `CODEX=timeout`). Keine festen Wartezeiten mit
   `send_later`, kein nacktes `sleep`: sonst wartet die Sitzung Minuten, obwohl Codex längst fertig ist.
   **`gh` ist in Cloud-Sitzungen oft nicht installiert (Bug B15).** Die Skripte brauchen es nicht: Über
   `github-api.sh` nehmen sie `gh`, wenn da, sonst `curl` + `jq` mit `GH_TOKEN`. Meldet ein Skript
   `ZUGANG=fehlt (…)`, gibt es keinen API-Weg aus der Shell; dann die GitHub-MCP-Werkzeuge nehmen (unten).
   Befunde danach lesen mit dem GitHub-MCP-Werkzeug `pull_request_read`: `get_review_comments`
   (Zeilen-Befunde P1/P2) und `get_comments` (Codex-Sammelkommentar). Ohne MCP geht dasselbe per
   `source …/github-api.sh; api repos/Pepsi1978/proggs/pulls/<N>/comments | jq …`
   (ebenso `issues/<N>/comments` und `issues/<N>/reactions` für das 👍 von `chatgpt-codex-connector[bot]`). **Nicht auf ein Ereignis warten:** Bot-Kommentare
   werden Cloud-Sitzungen nicht zugestellt (Bug #62977), sie müssen aktiv abgefragt werden. Codex ist fertig, wenn
   sein Sammelkommentar „Codex Review Summary“ `Completed` bzw. „Didn't find any major issues“ zeigt oder
   er ein 👍 gesetzt hat.
   - **Keine Befunde** (👍, keine Zeilenkommentare) → weiter mit Schritt 6.
   - **Befunde** → Frank in der Sitzung **sofort** kurz melden, damit er weiß, warum es länger dauert:
     „Codex hat <n> Punkte gefunden (<je ein Halbsatz>), ich fixe sie jetzt, dann wird veröffentlicht.“
     P1 immer beheben, P2 beheben, wenn der Befund zutrifft; offensichtlich falsche Befunde begründet
     überspringen. Fix auf denselben Branch committen und pushen, **kein** neuer Versionslog-Eintrag
     (die Version ist noch nicht veröffentlicht). **Kein** zweites Review anstoßen (kein weiteres
     „@codex review“): genau eine Runde, damit Zeit und Codex-Kontingent im Rahmen bleiben.
   - **Nach 8 Minuten kein Ergebnis** → ohne Review weiter mit Schritt 6 und das in der Abschlussmeldung
     erwähnen.
6. **Selbst mergen** (GitHub-MCP `merge_pull_request` mit `merge_method: merge`, oder `gh pr merge <N> --merge`).
   Blockiert der Auto-Modus den Merge oder danach das Bau-Warten (`[Merge Without Review]`, Bug B5 / #96257),
   nicht umgehen: Frank melden und auf den Berechtigungsmodus „Änderungen akzeptieren“ hinweisen. Scheitert der Merge an einem
   Konflikt: `main` in den Branch mergen, Konflikt lösen, pushen, erneut mergen. Der Merge startet den
   Ablauf `.github/workflows/android-cloud-build.yml` (baut nur Merges von Pull Requests).
7. **Bau abwarten, fertig sofort erkennen:** Direkt nach dem Merge, wieder mit Zeitlimit 600000 ms:
   `bash OpenLauncher/Profiles/ClaudeCode/standard/skills/apk-update-cloud/warte-auf-bau.sh <N>`
   Das Skript sucht den Lauf von `android-cloud-build.yml` zum Merge-Commit, fragt alle 15 Sekunden
   nach und kehrt **in dem Moment** zurück, in dem der Bau fertig ist. Keine feste Nachkontrolle nach
   6 Minuten planen. Meldet es `ZUGANG=fehlt`: GitHub-MCP `actions_list` (`list_workflow_runs`,
   `resource_id: android-cloud-build.yml`) und `actions_get` (`get_workflow_run`) nehmen. Ein nacktes
   `BAU=kein-lauf (Commit )` ohne Commit-Nummer heißt: der Zugang fehlte, nicht der Bau.
   - `BAU=gruen` → Frank kurz melden: „<App> <Version> liegt in Google Drive, UpdateStation zeigt es bei der
     nächsten Prüfung (oder ‚Jetzt prüfen‘).“
   - `BAU=laeuft` (nach 9,5 min noch nicht fertig) → Skript sofort noch einmal starten.
   - `BAU=kein-lauf` → der Merge hat keine App-Dateien berührt oder der Auslöser greift nicht: prüfen,
     ob die Änderung unter `<Projekt>/app/**` liegt, sonst Frank melden.
   - `BAU=rot` → Protokoll lesen (`get_job_logs`, `failed_only`), Ursache beheben und denselben Ablauf ab
     Schritt 1 wiederholen (neuer Versionslog-Eintrag nur, wenn der vorige schon veröffentlicht war;
     bei „Versionslog-Eintrag fehlt“ genau diesen nachtragen). Erst aufgeben und Frank fragen, wenn
     die Ursache außerhalb der App liegt (z. B. Secret abgelaufen, Google-Drive-Zugang widerrufen).
8. **Abschlussmeldung** an Frank, immer genau dieser Block (Details in `OpenLauncher/Profiles/ClaudeCode/sources/cloud.md` §8):
   ```
   ☁️ Cloud-Sitzung · Cloud-Regeln (cloud.md) erkannt ✓
   Geändert: <ein Satz>
   Version: <App> <alt> → <neu> (versionCode <n>)
   Codex-Review: <übersprungen (Nein) | keine Befunde | n Befunde, behoben | kein Ergebnis nach 8 min>
   Commit + Push: ja · PR #<n> gemergt: ja
   GitHub-Bau: grün · Google Drive: hochgeladen · UpdateStation: verfügbar
   ```

## Bekannte Cloud-Fallen (Stand 27.09.2026)

- **Kein `gh` in der Cloud-VM** (Bug B15): Die alten Skripte meldeten dann still `CODEX=timeout` und
  `BAU=kein-lauf`. Seit 27.09.2026 laufen sie über `github-api.sh` mit curl + jq und prüfen den Zugang vorab.

- **Genau ein Repo pro Sitzung** (`proggs`). Bei zwei Repos lädt die Cloud weder Hooks noch Permissions.
- Meldet ein Stop-Hook nach dem Merge noch „unpushed commits“: `git fetch --prune`, notfalls
  `git checkout -B <branch> origin/main`. Nie veröffentlichte History umschreiben (Bug B6).
- Kein Android-Bau in der Cloud-VM nötig und meist nicht möglich (`dl.google.com` gesperrt, kein SDK):
  bauen macht GitHub Actions.
- Details: `bugs/claude-tooling/claude-code-cloud.md`.

## Sicherheitsregeln (unverändert gültig)

- Keine Änderungen an `.github/workflows/**`, an Secrets oder am Environment `android-signing` im Zuge
  eines App-Updates. Muss der Bau-Ablauf selbst geändert werden, Frank vorher fragen und diesen
  Pull Request **nicht** selbst mergen.
- Keine API-Schlüssel aus SK in die APK einbauen. Schlüssel trägt Frank in der App ein.
- Hintergrund: `docs/cloud-android-build/EINRICHTUNG-FUER-KI.md`, `ANDROID-APP-REFERENZ.md` Kapitel 8.1.
- Bekannte Cloud-Fallen: `bugs/claude-tooling/claude-code-cloud.md`, Best Practices:
  `best-practices/claude-tooling/claude-code-cloud.md`.
