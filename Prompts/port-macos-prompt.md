Aufgabe: Portiere die App <App> (Ordner ~/proggs/<App>) funktionsgleich nach macOS. Ein gemeinsamer Code-Stand für beide Plattformen – die Windows-Version muss danach weiterhin exakt so funktionieren wie jetzt.

## 0. Rahmen (gilt für den ganzen Auftrag)
- Monorepo: Die App liegt im Repo https://github.com/Pepsi1978/proggs (öffentlich), Branch `main`, Ordner `<App>/`. Alle anderen Apps im Repo bleiben unberührt.
- Gearbeitet wird direkt auf `main` (Regel 10), kein Feature-Branch, kein Pull Request. Weil `main` der Live-Stand ist, darf kein Push die Windows-Version kaputt machen: Jeder Commit muss für sich lauffähig sein.
- Der Schnellmodus gilt für diesen Auftrag NICHT. Tests, lokale Prüfungen und CI-Läufe sind Pflicht, sie sind der Kern des Auftrags.
- Vor der ersten Dateiänderung: `git -C ~/proggs fetch --quiet` und `git -C ~/proggs status -sb`. Steht dort `behind`: `git -C ~/proggs pull --rebase --autostash`. Bei Konflikten stoppen und melden.
- Die globalen Regeln gelten weiter: Commit-Stil `<App>: <was geändert wurde>` auf Deutsch mit echten Umlauten, Versions-Bump genau einmal pro Commit mit echter Systemzeit (`Get-Date -Format "dd.MM.yyyy HH:mm"`), vor jedem Push rebasen.
- Lies vorher den Skill `cross-platform` (Plattform-Mapping Windows ↔ macOS) sowie die passenden Dateien in ~/proggs/best-practices und ~/proggs/bugs.

## 1. Analyse zuerst (nichts ändern, bevor das steht)
- Stelle fest: Sprache, Framework, Build-System, Paketmanager, Zielarchitektur.
- Liste alle Windows-spezifischen Stellen auf: Win32-/Windows-only-APIs, Registry, Pfade mit Backslash oder Laufwerksbuchstaben, %APPDATA% und andere Umgebungsvariablen, Autostart, Tray-Icon, globale Hotkeys, Benachrichtigungen, Dateidialoge, Schriftarten, DPI-Skalierung, Aufrufe von cmd/PowerShell, Zeilenenden, Groß-/Kleinschreibung von Dateinamen, native Abhängigkeiten (DLLs).
- Schreibe daraus eine kurze Portierungsanalyse in `<App>/PORTING.md`: Was wird wie abstrahiert, was hat auf macOS kein Gegenstück und wie wird es ersetzt (z. B. Ctrl → Cmd, %APPDATA% → ~/Library/Application Support/<App>, Autostart → Login Item, Tray-Icon → Menüleisten-Icon).
- Läuft das UI-Framework nicht auf macOS (z. B. WPF oder WinForms), ist ein gemeinsamer Code-Stand nur mit einem Framework-Wechsel möglich (z. B. Avalonia). Das ist meine Entscheidung: Nach der Analyse anhalten, PORTING.md committen und pushen, mir die Optionen mit Aufwand und deiner Empfehlung nennen und auf meine Antwort warten.

## 2. Umsetzung
- Kleine, klar benannte Commits direkt auf `main`, damit jeder Schritt einzeln rückgängig gemacht werden kann. Jeder Commit baut auf Windows grün.
- Plattformabhängiger Code kommt hinter eine gemeinsame Schnittstelle. Keine kopierte zweite App, keine Sonderversion.
- Keine Funktion still weglassen. Was auf macOS wirklich nicht machbar ist, wird im Bericht unter „Nicht portiert“ genannt – nicht auskommentiert.
- Zielarchitektur: Apple Silicon (arm64); Universal-Build nur, wenn das Build-System das ohne Mehraufwand hergibt.

## 3. GitHub Actions
- Workflows liegen im Monorepo immer unter `~/proggs/.github/workflows/`. Dort gibt es bereits `build-macos.yml` und `build-windows.yml`. Prüfe zuerst, ob diese <App> schon bauen. Keine doppelten Läufe, bestehende Workflows nicht umbauen.
- Lege einen eigenen Workflow `.github/workflows/<app>-build.yml` an (Name kleingeschrieben) mit Matrix `windows-latest` + `macos-latest` und Pfadfilter `paths: ['<App>/**', '.github/workflows/<app>-build.yml']`, damit er nur bei Änderungen an dieser App läuft. Beide Jobs müssen grün werden – der Windows-Job ist der Beweis, dass nichts kaputtgegangen ist.
- Der macOS-Job muss: Abhängigkeiten installieren, kompilieren, alle vorhandenen Tests ausführen, wenn möglich einen Headless-Starttest (App startet und beendet sich sauber) und ein installierbares Paket (.app als .zip oder .dmg) als Workflow-Artefakt hochladen.
- Code-Signierung und Notarisierung: Im Repo ist derzeit kein Apple-Secret hinterlegt (nur `CLAUDE_CODE_OAUTH_TOKEN`). Also unsigniert bauen und im Bericht erklären, wie ich die App trotzdem öffne.

## 4. Fehler beheben, bis beide Jobs grün sind
- `gh` ist installiert und angemeldet (Konto Pepsi1978, Rechte `repo` + `workflow`).
- Ablauf: pushen → Lauf zum eigenen Commit finden mit `gh run list --workflow <app>-build.yml --commit <SHA> --json databaseId` → `gh run watch <ID> --exit-status` → bei Fehler `gh run view <ID> --log-failed` → Ursache beheben → erneut pushen. Wiederholen, bis beide Jobs grün sind.
- Vor jedem Push alles prüfen, was lokal prüfbar ist (Kompilieren, Tests, Linter), damit keine CI-Runden verschwendet werden. Einen roten Windows-Stand auf `main` sofort reparieren, bevor irgendetwas anderes weitergeht.
- Verboten, um „grün“ zu erreichen: Tests löschen, überspringen oder abschwächen; `continue-on-error`; Jobs oder Schritte entfernen; Funktionen auskommentieren; Warnungen oder Fehler global unterdrücken. Grün muss echte Funktionsfähigkeit bedeuten.
- Nur anhalten, wenn das Ziel selbst nicht erreichbar ist (z. B. ein Secret, ein Apple-Developer-Konto oder eine Entscheidung von mir ist nötig). Dann: aktuellen Stand committen und pushen, genau benennen, was fehlt und was ich tun muss.

## 5. Windows-Installation und Abschlussbericht
- Nach grüner CI die Windows-Version lokal bauen und auf diesem PC installieren bzw. deployen, so wie es das Projekt vorsieht (eigene CLAUDE.md, Update-Skript). Die Mac-Installation erfolgt nicht von hier aus, sondern über den Mac-Prompt aus Abschnitt 6. Melde sie als „ausstehend, über MAC-PROMPT.md“.
- Bericht in `<App>/PORTING.md` und als Zusammenfassung im Chat, strikt in drei Teile getrennt:
A) Automatisch geprüft – was die CI auf macOS tatsächlich bewiesen hat (Build, Tests, Starttest, Paket), mit Link zum grünen Workflow-Lauf und zum Artefakt.
B) Auf meinem Mac manuell zu testen – nummerierte Checkliste; pro Punkt: genaue Schritte, erwartetes Ergebnis, und warum das nicht automatisch prüfbar war (z. B. Systemberechtigungen, Menüleiste, Dateidialoge, Benachrichtigungen, Hotkeys, Retina-Darstellung, Verhalten nach Neustart). Diese Liste ist die Grundlage für den Mac-Prompt in Abschnitt 6.
C) Bekannte Unterschiede / nicht portiert – jeweils mit Begründung und Vorschlag.
- Dazu eine Installationsanleitung für das CI-Artefakt auf dem Mac (Artefakt herunterladen, entpacken, Gatekeeper: Rechtsklick → Öffnen oder `xattr -d com.apple.quarantine`).

## 6. Übergabe-Prompt für den Mac erzeugen
Ganz zum Schluss erzeugst du einen zweiten, eigenständigen Prompt, den ich auf meinem Mac in Claude Code einfüge. Speichere ihn als `<App>/MAC-PROMPT.md` (committen und pushen) und gib ihn zusätzlich als allerletzte Ausgabe im Chat in EINEM einzigen Codeblock aus, den ich komplett kopieren kann.
Regeln für diesen Prompt:
- Die Mac-Sitzung kennt nichts aus diesem Gespräch. Der Prompt muss allein ausreichen: Repository-URL https://github.com/Pepsi1978/proggs, Branch `main`, Repo-Ordner auf dem Mac `/Users/frank/proggs`, App-Ordner `/Users/frank/proggs/<App>`, Sprache/Framework, benötigte Werkzeuge mit Versionen, die exakten Befehle. Keine Platzhalter – alles konkret ausgefüllt.
- Die komplette Checkliste aus Teil B ist wörtlich enthalten.
- Auf Deutsch, nummerierte Abschnitte, gleicher Stil wie dieser Prompt.
Der Mac-Prompt muss Claude Code auf dem Mac anweisen:
1. Umgebung prüfen und Fehlendes installieren: Xcode Command Line Tools, Homebrew, die benötigte Laufzeit bzw. Toolchain in der richtigen Version (genau benennen), git-Zugriff auf GitHub, `gh` mit `gh auth status`.
2. Projekt holen: Existiert `/Users/frank/proggs` nicht, klonen; sonst `git -C /Users/frank/proggs fetch --quiet`, `git -C /Users/frank/proggs status -sb` und bei `behind` `git -C /Users/frank/proggs pull --rebase --autostash`. Bei Konflikten stoppen und melden. Gearbeitet wird auf `main`.
3. Lokal aus dem Quellcode bauen (nicht das CI-Artefakt verwenden), alle Tests ausführen, das .app-Paket erzeugen, nach /Applications installieren und die App starten.
4. Build- oder Testfehler, die nur auf dem echten Mac auftreten, selbst beheben, committen und auf `main` pushen (vorher rebasen, Commit-Stil und Versions-Bump wie oben). Jeder Push muss den Windows-Job des Workflows `<app>-build.yml` grün lassen; das mit `gh run watch` prüfen. Nicht anhalten und melden, anhalten nur, wenn das Ziel selbst unerreichbar ist.
5. Danach die Checkliste aus Teil B Punkt für Punkt mit mir durchgehen: Claude führt selbst aus, was es kann (App starten, Testdateien anlegen, Ausgaben und Logs prüfen), fragt mich nur bei Punkten, die einen Menschen brauchen (Menüleiste, Dialoge, Berechtigungs-Popups, Optik), und hält für jeden Punkt ✅ oder ❌ mit kurzer Beobachtung fest.
6. Ergebnis als Abschnitt „Ergebnis Mac-Test <Datum>“ in `<App>/PORTING.md` eintragen (Datum per `date "+%d.%m.%Y %H:%M"`), committen, pushen und mir sagen, ob die Portierung abgeschlossen ist oder was noch offen ist.
