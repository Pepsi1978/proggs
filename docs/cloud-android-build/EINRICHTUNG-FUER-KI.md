# Auftrag: Android-Apps in der Cloud bauen und signieren (GitHub Actions → Google Drive → UpdateStation)

Stand: 2026-09-26 · Verfasst von Claude Code (Cloud-Sitzung) für die KI im CLI auf Franks Windows-PC.

Du bist die KI auf Franks PC. Richte den hier beschriebenen Ablauf **vollständig** ein. Du hast Zugriff auf den
SK-Ordner (`C:\Users\barwa\SK\`), auf GitHub (`gh` angemeldet) und auf den Skill `apk-update`.
Frank sitzt daneben und erledigt die Schritte im Browser, die nur er machen kann. Sag ihm genau, was er klicken
muss, und warte auf seine Bestätigung.

## 1. Ziel

Frank programmiert unterwegs nur mit dem Handy in einer Claude-Code-Cloud-Sitzung. Danach soll die fertige App
ohne Rechner auf seinem Handy landen:

```
Cloud-Sitzung (Claude) ─► Branch claude/… + Pull Request
Frank tippt in der GitHub-App auf „Merge“ (= Freigabe zum Signieren)
GitHub Actions auf main ─► baut + signiert mit dem geteilten Keystore
                        ─► lädt APK + update.json nach Google Drive „Dokumente/Updates/<Projekt>/“
UpdateStation auf dem Handy ─► zeigt das Update ─► Frank installiert
```

Die Cloud-Sitzung bekommt den Keystore **nie** zu sehen. Er liegt ausschließlich als verschlüsseltes
GitHub-Secret vor und steht nur Bauvorgängen auf `main` zur Verfügung.

## 2. Was schon feststeht (bereits im Code geprüft)

- **Ein Keystore für alle Apps:** `~/SK/Android/debug-shared.keystore`, Alias `androiddebugkey`, Passwörter
  `android` (das ist der öffentliche Android-Standard und steht schon im Code, **geheim ist nur die Datei**).
- Die Gradle-Dateien lesen ihn schon über `System.getProperty("user.home") + "/SK/Android/debug-shared.keystore"`
  (z. B. `CortexAndroid`, `VoiceKey`, `Gedankenspeicher`, `EntropieReductor`, `BestJournal*`).
  `ClaudeKompass` und `CodexKompass` verlassen sich dagegen auf `~/.android/debug.keystore`, das OpenLauncher
  aus SK kopiert.
  **Folge: Du musst keine `build.gradle.kts` ändern.** Der Bau-Ablauf legt die Datei auf dem GitHub-Rechner
  an **beide** Stellen: `$HOME/SK/Android/debug-shared.keystore` und `$HOME/.android/debug.keystore`.
  Lokal am PC bleibt alles wie bisher, und der SK-Ordner bleibt das Original.
- Apps mit `syncSecretsFromSk` (`BestJournalAndroid`, `BestJournalFrank`, `EntropieReductor`) brauchen
  zusätzlich `google-services-debug.json` aus SK. Ohne die Datei brechen sie mit einer GradleException ab.
  → siehe Schritt 6.
- **UpdateStation** (`UpdateStation/app/src/main/java/de/frank/updatestation/Quellen.kt`, `Daten.kt`) liest aus
  Google Drive den Pfad `Dokumente/Updates` (Standard). Jeder Unterordner ist ein Projekt, darin liegen
  `update.json` (Format 1, Klasse `UpdateManifest`) und die APK, deren Name in `update.json` → `apk` steht.
  Namensschema: `<Projekt>-<Version>-vc<Nummer>.apk`.
  Felder: `projekt, paket, versionCode, versionName, versionStand, apk, groesse, sha256, signaturSha256,
  erstelltAm, versionslog`.
- Der Skill **`apk-update`** auf deinem PC erzeugt genau diese Dateien. **Lies ihn zuerst vollständig** und
  bilde im Bau-Ablauf dieselbe Logik nach (welcher Gradle-Task, Versionsermittlung, `signaturSha256`,
  `versionslog`, Upload-Reihenfolge: **erst APK, dann update.json**, damit UpdateStation nie eine update.json
  ohne APK sieht). Weicht etwas von dieser Anleitung ab, gilt der Skill.

## 3. Voraussetzungen prüfen und Frank berichten

1. `gh auth status` und `gh api user --jq .plan.name` → Hat Frank **GitHub Pro**?
2. `gh repo view pepsi1978/proggs --json visibility` → privat oder öffentlich?
3. `gh api repos/pepsi1978/proggs/collaborators --jq '.[].login'` → Wer hat Schreibrecht? Zeig Frank die Liste.
4. Ist die **Zwei-Faktor-Anmeldung** bei GitHub aktiv? (`gh api user --jq .two_factor_authentication`,
   sonst Frank bitten, unter github.com → Settings → Password and authentication nachzusehen.) Ist sie aus,
   zuerst einschalten lassen.

**Entscheidungsregel:**
- Environments sind nutzbar, wenn das Projekt öffentlich ist oder Frank Pro hat.
  → Secrets in ein **Environment `android-signing`** legen, mit der Regel „Deployment branches: nur `main`“.
  So kommt ein `claude/…`-Branch, also auch eine Cloud-Sitzung, nie an den Schlüssel.
- Ist das Projekt privat **und** Frank hat kein Pro: Erkläre ihm das Restrisiko in einfachen Worten.
  Jeder mit Schreibrecht (auch die Cloud-KI) könnte einen Ablauf schreiben, der ein Repository-Secret
  ausliest. Lass ihn entscheiden: Pro buchen (rund 4 US-Dollar im Monat) oder bewusst mit Repository-Secrets
  arbeiten. Nicht selbst entscheiden.

## 4. Keystore als Secret hinterlegen

Das macht die KI am PC. Der Schlüssel darf dabei **nie** in Chat, Protokoll oder Datei im Projekt landen:

```powershell
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\SK\Android\debug-shared.keystore"))
$b64 | gh secret set ANDROID_SHARED_KEYSTORE_B64 --repo pepsi1978/proggs --env android-signing
Remove-Variable b64
```

(Ohne Environment: `--env android-signing` weglassen.) Gib den Base64-Text **nicht** aus. Danach mit
`gh secret list --env android-signing` nur prüfen, ob der **Name** da ist.

Zusätzlich den SHA-256-Fingerabdruck des Zertifikats **ohne** Geheimnis als Variable ablegen. Damit kann der
Bau-Ablauf prüfen, ob er mit dem richtigen Schlüssel signiert hat:

```powershell
keytool -list -v -keystore "$HOME\SK\Android\debug-shared.keystore" -storepass android -alias androiddebugkey
# Zeile "SHA256:" nehmen, Doppelpunkte entfernen, klein schreiben
gh variable set ANDROID_SIGNATUR_SHA256 --repo pepsi1978/proggs --body "<fingerabdruck>"
```

## 5. Google-Drive-Zugang für den Upload

- Nimm **rclone** mit einem Google-Drive-Remote namens `gdrive`.
- **Kein Dienstkonto** verwenden. Dienstkonten haben keinen eigenen Speicherplatz, deshalb scheitern Uploads in
  ein privates „Meine Ablage“ mit `storageQuotaExceeded`.
- Stattdessen Franks **eigenes Google-Konto per OAuth**: `rclone config` am PC, Frank meldet sich im Browser an.
- Umfang (Scope): `drive.file` reicht **nicht**. Damit sieht rclone nur selbst angelegte Dateien und kann nicht
  in den vorhandenen Ordner `Dokumente/Updates` schreiben. Also Scope `drive` verwenden.
  **Sag Frank offen:** Dieses Token erlaubt Zugriff auf sein ganzes Google Drive. Deshalb gehört es genauso ins
  geschützte Environment wie der Keystore. Wenn möglich, einen eigenen OAuth-Client (Client-ID/Secret aus der
  Google Cloud Console) statt des rclone-Standard-Clients anlegen, damit Frank den Zugang dort jederzeit
  widerrufen kann.
- Die rclone-Konfiguration als Secret ablegen, ohne sie anzuzeigen:
  `Get-Content (rclone config file | Select-Object -Last 1) -Raw | gh secret set RCLONE_CONFIG --repo pepsi1978/proggs --env android-signing`
  Liegen in der rclone-Konfiguration noch andere Remotes, vorher eine eigene Konfiguration nur mit `gdrive` erzeugen.
- Einmal lokal testen: `rclone lsd gdrive:Dokumente/Updates` muss die Projektordner zeigen.

## 6. Apps mit weiteren SK-Dateien

`google-services-debug.json` für `BestJournalAndroid`, `BestJournalFrank` und `EntropieReductor`:
**Frank fragen**, ob diese Dateien ebenfalls als Secrets ins Environment sollen (Name z. B.
`SK_<PROJEKT>_GOOGLE_SERVICES_DEBUG_JSON`, Base64). Wenn nein, schließt der Bau-Ablauf diese Apps aus und
meldet das deutlich im Ergebnis. Prüfe mit einer Suche nach `syncSecretsFromSk` und `SK/`, ob weitere Apps
Dateien aus SK brauchen, und behandle sie genauso.

## 7. Bau-Ablauf schreiben: `.github/workflows/android-cloud-build.yml`

Anforderungen:

- **Auslöser:** `push` auf `main` mit Pfadfilter auf die Android-Projektordner, dazu `workflow_dispatch` mit
  der Eingabe `projekt` (Ordnername), damit Frank einen Bau auch per Knopf in der GitHub-App starten kann.
  **Kein** `pull_request`- und **kein** `pull_request_target`-Auslöser für diesen Ablauf.
- **Welche Apps:** Ermittle geänderte oberste Ordner (`git diff --name-only ${{ github.event.before }} ${{ github.sha }}`),
  die eine `app/build.gradle.kts` enthalten. Baue jede davon in einer eigenen Matrix-Zeile.
  Ausnahmen: `Designs/**` und die Apps ohne Secrets aus Schritt 6.
- **Job-Einstellungen:** `environment: android-signing`, `permissions: contents: read`,
  `concurrency: android-cloud-build-${{ matrix.projekt }}`.
- **Bausteine nur von GitHub und Gradle**, jeweils auf den **Commit-SHA eingefroren** (aktuellen SHA
  nachschlagen, Versionsnummer als Kommentar dahinter): `actions/checkout`, `actions/setup-java` (JDK wie im
  Projekt verlangt, sonst 21 bzw. 17), `gradle/actions/setup-gradle`. **Keine** fremden Actions für Signieren,
  Drive oder „changed files“. Das erledigen eigene Shell-Schritte.
  (Hintergrund: 2025 wurde `tj-actions/changed-files` manipuliert und hat Secrets in Protokolle geschrieben.)
- **Keystore-Schritt** (Secret nur hier als `env`, nie in `run:`-Text einsetzen):
  ```bash
  mkdir -p "$HOME/SK/Android" "$HOME/.android"
  printf '%s' "$KEYSTORE_B64" | base64 -d > "$HOME/SK/Android/debug-shared.keystore"
  cp "$HOME/SK/Android/debug-shared.keystore" "$HOME/.android/debug.keystore"
  chmod 600 "$HOME/SK/Android/debug-shared.keystore" "$HOME/.android/debug.keystore"
  ```
- **Bauen:** im Projektordner `./gradlew <Task wie im apk-update-Skill> --no-daemon`. Das Android SDK ist auf
  `ubuntu-latest` vorinstalliert. Fehlende Plattformen installiert Gradle selbst, sonst `sdkmanager`.
- **Signatur prüfen:** `apksigner verify --print-certs` aus `$ANDROID_HOME/build-tools/<neueste>/`. Der
  SHA-256 muss `vars.ANDROID_SIGNATUR_SHA256` entsprechen, sonst abbrechen und **nichts** hochladen.
- **update.json erzeugen** wie im Skill `apk-update` (Format 1, alle Felder aus Schritt 2, `versionslog` aus
  `app/src/main/assets/versionslog.json`, falls vorhanden). Werte mit `aapt2 dump badging` oder aus Gradle lesen.
- **Upload** mit rclone (aus dem offiziellen Ubuntu-Paket oder offiziellem Release mit Prüfsummencheck):
  Konfiguration aus `RCLONE_CONFIG` in eine Temp-Datei, dann
  `rclone copyto <apk> "gdrive:Dokumente/Updates/<Projekt>/<Dateiname>"`, **danach** `update.json`.
  Alte APKs im Ordner so behandeln, wie es der Skill tut.
- **Aufräumen:** letzter Schritt mit `if: always()` löscht Keystore- und rclone-Dateien. Der Rechner ist zwar
  ohnehin ein Wegwerf-Rechner, aber sicher ist sicher.
- **Ergebnis:** In `$GITHUB_STEP_SUMMARY` auf Deutsch schreiben, welche App mit welcher Version hochgeladen
  wurde. Dann sieht Frank das in der GitHub-App.
- Die APK zusätzlich als Actions-Artefakt (7 Tage) anhängen. So kann Frank sie notfalls direkt aus GitHub laden.

## 8. Regel für künftige Cloud-Sitzungen festhalten

Ergänze in `ANDROID-APP-REFERENZ.md` (und, falls vorhanden, im Skill `apk-update`) einen kurzen Abschnitt
„Bauen aus der Cloud“:

- In einer Cloud-Sitzung gibt es keinen SK-Ordner. **Nicht versuchen, selbst zu signieren**, und **nie** nach
  dem Keystore fragen.
- Stattdessen: `versionCode`/`versionName` wie gewohnt erhöhen, `versionslog.json` pflegen, testen, soweit es
  ohne Signatur geht (Unit-Tests, `assembleDebug` mit Wegwerf-Schlüssel nur zur Kompilierprüfung, das
  Ergebnis **nie** hochladen), Pull Request öffnen und Frank sagen: „Zum Installieren den Pull Request mergen,
  danach baut GitHub und legt die APK in Google Drive.“

## 9. Probelauf und Abnahme

1. Mit einer kleinen App (z. B. `NovaDrehen` oder `Experimente`) einen Test-Branch mit erhöhtem `versionCode`
   anlegen, Pull Request öffnen, Frank merged.
2. Prüfen: Ablauf grün, Signatur-Prüfung bestanden, APK und update.json in `Dokumente/Updates/<Projekt>/`.
3. Frank prüft in UpdateStation: Update erscheint, Installation **über** die vorhandene App klappt ohne
   Deinstallation. Das ist der Beweis, dass es derselbe Schlüssel ist.
4. Gegenprobe Sicherheit: Auf einem `claude/…`-Branch einen Testlauf von `workflow_dispatch` starten.
   Er muss wegen der Environment-Regel **blockiert** werden (nur falls Environment genutzt wird).
5. Alles committen (Workflow und Doku, **keine** Secrets), pushen und Pull Request öffnen.

## 10. Was niemals passieren darf

- Keystore, Base64-Text, rclone-Token oder google-services-Dateien im Projekt, im Chat, in Protokollen oder in
  Commits.
- `pull_request_target` oder Bauten von Forks mit Zugriff auf die Secrets.
- Einen **anderen** Keystore für die Cloud erzeugen. Dann lehnt Android die Updates ab.
- Den SK-Ordner löschen oder verändern. Er bleibt Original und Sicherung. Das GitHub-Secret ist nur eine Kopie,
  die man nicht zurückholen kann.

## 11. Abschlussbericht an Frank (auf Deutsch, einfach)

- Was eingerichtet wurde, welche Secrets (nur Namen) wo liegen, ob Environment-Schutz aktiv ist.
- Welche Apps aus der Cloud baubar sind und welche nicht (und warum).
- Wie er künftig vorgeht: „Am Handy programmieren lassen → Pull Request in der GitHub-App mergen → etwa
  5 Minuten warten → UpdateStation öffnen.“
- Wie er im Notfall alles abschaltet: Secret löschen oder das Environment löschen (GitHub → Projekt →
  Settings → Environments). Drive-Zugang widerrufen: myaccount.google.com → Sicherheit → Drittanbieter-Apps.
