---
name: apk-update-cloud
description: >
  Veröffentlicht in einer Claude-Code-CLOUD-Sitzung eine geänderte Android-App vollautomatisch aufs
  Handy: Versionslog-Eintrag anhängen, committen, Pull Request öffnen, Codex-Review abwarten und
  Befunde fixen, selbst mergen, GitHub Actions
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

## Wann

- Automatisch am Ende jeder Aufgabe, die Dateien unter `<Projekt>/app/**` oder die Gradle-Dateien eines
  Android-Projekts (Ordner mit `app/build.gradle.kts`) geändert hat.
- Nicht bei: `BestJournalAndroid` (Play-Store-Key, geht nur am PC), halbfertigen Änderungen, reinen
  Doku-Änderungen, oder wenn Frank ausdrücklich „nicht veröffentlichen“ sagt.
- Nie am PC ausführen (`CLAUDE_CODE_REMOTE` ist dort nicht `true`).

## Ablauf

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
4. **Pull Request öffnen, nicht als Entwurf** (GitHub-Werkzeug `create_pull_request`, `draft: false`).
   Das startet automatisch das Codex-Review (Bot `chatgpt-codex-connector`). Nie erst als Entwurf
   öffnen und dann auf „bereit“ setzen: Das löst ein zweites, überflüssiges Review aus.
5. **Codex-Review abwarten (höchstens etwa 8 Minuten):** Nicht mit `sleep` warten, sondern mit
   `send_later` nach etwa 2 Minuten nachsehen, bei Bedarf noch zweimal im Abstand von 3 Minuten. Lesen
   mit `pull_request_read` (`get_comments`, `get_reviews`, `get_review_comments`). Codex ist fertig, wenn
   sein Sammelkommentar „Codex Review Summary“ `Completed` zeigt oder er ein 👍 gesetzt hat.
   - **Keine Befunde** (👍, keine Zeilenkommentare) → weiter mit Schritt 6.
   - **Befunde** → Frank in der Sitzung **sofort** kurz melden, damit er weiß, warum es länger dauert:
     „Codex hat <n> Punkte gefunden (<je ein Halbsatz>), ich fixe sie jetzt, dann wird veröffentlicht.“
     P1 immer beheben, P2 beheben, wenn der Befund zutrifft; offensichtlich falsche Befunde begründet
     überspringen. Fix auf denselben Branch committen und pushen, **kein** neuer Versionslog-Eintrag
     (die Version ist noch nicht veröffentlicht). **Kein** zweites Review anstoßen (kein
     „@codex review“): genau eine Runde, damit Zeit und Codex-Kontingent im Rahmen bleiben.
   - **Nach 8 Minuten kein Ergebnis** → ohne Review weiter mit Schritt 6 und das in der Abschlussmeldung
     erwähnen.
6. **Selbst mergen** (`merge_pull_request`, `merge_method: merge`). Scheitert der Merge an einem
   Konflikt: `main` in den Branch mergen, Konflikt lösen, pushen, erneut mergen. Der Merge startet den
   Ablauf `.github/workflows/android-cloud-build.yml` (baut nur Merges von Pull Requests).
7. **Bau abwarten:** Nicht mit `sleep` warten. Mit `send_later` eine Nachkontrolle in etwa
   6 Minuten planen. Dann mit `actions_list` (`list_workflow_runs`, `android-cloud-build.yml`) den Lauf
   zum Merge-Commit suchen:
   - grün → Frank kurz melden: „<App> <Version> liegt in Google Drive, UpdateStation zeigt es bei der
     nächsten Prüfung (oder ‚Jetzt prüfen‘).“
   - läuft noch → noch einmal ein paar Minuten später prüfen.
   - rot → Protokoll lesen (`get_job_logs`, `failed_only`), Ursache beheben und denselben Ablauf ab
     Schritt 1 wiederholen (neuer Versionslog-Eintrag nur, wenn der vorige schon veröffentlicht war;
     bei „Versionslog-Eintrag fehlt“ genau diesen nachtragen). Erst aufgeben und Frank fragen, wenn
     die Ursache außerhalb der App liegt (z. B. Secret abgelaufen, Google-Drive-Zugang widerrufen).
8. **Abschlussmeldung** an Frank: App, alte → neue Version, was neu ist, was Codex gefunden und was davon
   behoben wurde, ob der Bau grün war.

## Sicherheitsregeln (unverändert gültig)

- Keine Änderungen an `.github/workflows/**`, an Secrets oder am Environment `android-signing` im Zuge
  eines App-Updates. Muss der Bau-Ablauf selbst geändert werden, Frank vorher fragen und diesen
  Pull Request **nicht** selbst mergen.
- Keine API-Schlüssel aus SK in die APK einbauen. Schlüssel trägt Frank in der App ein.
- Hintergrund: `docs/cloud-android-build/EINRICHTUNG-FUER-KI.md`, `ANDROID-APP-REFERENZ.md` Kapitel 8.1.
