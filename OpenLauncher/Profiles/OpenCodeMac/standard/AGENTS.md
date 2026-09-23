# OpenCode-Profil: Standard

Diese Regeln gelten für jede Programmieraufgabe!

1. Zugangsdaten, Secrets und Deploy-Wege liegen in: /Users/frank/SK 
— Lies dort nach, bevor du deployst oder Zugangsdaten brauchst. Rate nie.

2. Best Practices und Bug Almanache liegen in den Ordnern /Users/frank/proggs/best-practices und /Users/frank/proggs/bugs
- Lies den passenden Almanach oder Best Practices, bevor du in einem neuen Technologiebereich anfängst.

3. Skills liegen ausschließlich im Repo-Ordner /Users/frank/proggs/OpenLauncher/Profiles/ClaudeCode/standard/skills – für Claude Code, Codex und OpenCode, in jedem Profil und Modus, auf macOS und Windows. ~/.claude/skills, ~/.agents/skills und die Profil-Ordner "skills" sind nur Verknüpfungen dorthin. Skills nur dort bearbeiten, danach committen und pushen.

4. Aufgabentrennung und Ablauf
` ; ` (Leerzeichen, Semikolon, Leerzeichen) trennt eigenständige Aufgaben.
- Ein leerer Teil am Ende zählt nicht mit. Semikola in Code, SQL oder URLs trennen nicht.
- Ordne die Aufgaben nach ihren Abhängigkeiten.
- Widersprechen sich zwei Aufgaben: frag nach, bevor du anfängst.
- Ab zwei Aufgaben: zeig vorab die nummerierte Liste in der Reihenfolge, in der du sie abarbeitest.

5. Sprache
- Alle Ausgaben an mich sind auf Deutsch, mit echten Umlauten: ä ö ü Ä Ö Ü ß. Keine Ersatzschreibung wie "ae" oder "ss".

6. Version und Zeitstempel
- Bump die Version genau einmal pro Commit, sichtbar. Der Bump gehört in denselben Commit wie die Änderung.
- Hol Datum und Uhrzeit vorher per Befehl: `Get-Date -Format "dd.MM.yyyy HH:mm"`
- Übernimm die Zeit nie aus dem Kontext, aus einer früheren Nachricht, aus dem Gedächtnis oder aus einer Anzeige in der App. Nie schätzen.
- Wo die Version steht, hängt von der Plattform ab:
  - Android: `app/src/main/assets/versionslog.json` — pro Commit einen Eintrag unten anhängen: `{ "versionCode": <letzter + 1>, "versionName": "1.0.28", "stand": "19.07.2026, 21:00 Uhr", "notiz": "<was geändert wurde>" }`.
  versionCode immer um 1 erhöhen, sonst erkennt UpdateStation das Update nicht. Uhrzeit mit Doppelpunkt, nicht mit Punkt. `build.gradle.kts` liest versionCode, versionName und VERSION_BUMPED_AT aus dem Log — dort nie von Hand eintragen. Neue Android-Apps bekommen den Versionslog von Anfang an (Vorlage: UpdateStation).
  - .NET/WPF: `<Version>` in der `.csproj`, Form `2.1.84`.
  - Node/TypeScript: `version` in der Wurzel-`package.json`. Bei WerftStudio mit Zeitstempel-Suffix, Form `0.32.2-20260809.1545`.
- Wird die Version in der App angezeigt (meist im Einstellungs-Bildschirm, sonst auf anderen Seiten suchen), zieh die Anzeige mit.
- Weicht ein Projekt von diesen Mustern ab, steht die Regel in dessen eigener AGENTS.md. Findest du dort nichts und passt kein Muster: frag nach, rate nicht.

7. Für alle research Aufgaben nutze den skill research und folge seinen Anweisungen.

8. Externe Regelwerke
"Direktive 3" bezeichnet eine externe Datei, nicht Abschnitt 3 dieses Profils.
- Wenn ich sage: "fixe nach Direktive 3": lies zuerst vollständig `/Users/frank/proggs/claude-code-setup/docs/rules/resilient-bugfixing.md` und fang erst danach mit dem Fix an.

9. Abschluss bei Code-Änderungen (Pflicht)
Sobald eine ` ; `-Teilaufgabe oder die Gesamtaufgabe Code geändert hat: → bauen und Tests laufen lassen — nur grün geht weiter → Version bumpen (echte Systemzeit) → committen → pushen → auf Gerät installieren bzw. deployen. Geht ein Schritt nicht (kein Gerät, roter Build, Push abgelehnt): melde das ausdrücklich, überspringe es nie stillschweigend.

10. Commits
- Message-Stil: `<Projekt>: <was geändert wurde>`, klein, imperativ, eine Zeile. Das Projekt-Präfix nur, wenn der Commit ein einzelnes Projekt betrifft.
- Sprache der Commit-Messages: Deutsch mit echten Umlauten. Nie ASCII-Ersatzschreibung ("ae", "ue", "ss").
- Committe direkt auf `main`, kein Feature-Branch. Vor dem Push wird rebased.

11. Abschluss (Pflicht, ohne Ausnahme)
Bevor du eine Antwort als fertig ausgibst, prüf diese zwei Punkte und hol Fehlendes sofort nach:
- Ist jede ` ; `-Teilaufgabe wirklich erledigt?
- Falls Code geändert wurde: sind alle fünf Schritte aus Regel 9 erledigt?

Melde danach kurz:
- Was geändert wurde. 
- Mache maximal 3 Verbesserungsvorschläge zum Projekt, falls dir etwas Substantielles aufgefallen ist.
- Committet ja/nein, gepusht ja/nein, installiert auf welchem Gerät.

12. Wenn der Emulator angefodert wird, Starte den Emulator, immer mit dem Fold 8 Profil über /Users/frank/proggs/Werkzeuge/fold8-emulator

13. OpenLauncher-Updates immer nur über das Updatescript: macOS /Users/frank/proggs/OpenLauncherMac/update-launcher.sh, Windows C:\Users\barwa\proggs\OpenLauncher\update-launcher.ps1. Ablauf: Das Skript zeigt mir zuerst ein Ja/Nein-Fenster. Erst nach meinem Klick auf Ja schließt es den laufenden OpenLauncher, baut und startet die neue Version und endet danach sofort. Klicke ich Nein, passiert nichts. Nie mit -Force bzw. OPENLAUNCHER_UPDATE_FORCE=1 aufrufen, außer ich sage es ausdrücklich. Das Skript nicht selbst abbrechen, solange das Fenster auf meinen Klick wartet: den Befehl mit dem höchsten erlaubten Zeitlimit starten, nicht mit dem kurzen Standard. Rückmeldungen: started = neue Version installiert und gestartet; already-current = war schon installiert, fertig, kein Fenster; cancelled = ich habe Nein geklickt; no-answer = 4 Minuten kein Klick, nichts geändert. Bei cancelled oder no-answer nicht erneut starten, sondern mir melden.

14. Researches im Web erfolgen immer über den Researcher Skill. Der Ablauf den Resercher Skills wird immer eingehalten! Nach dem Research werden Best Parctices und Bug Almanache in den Ordnern /Users/frank/proggs/best-practices und /Users/frank/proggs/bugs gespeichert.

15. Android-Signierung: Alle Android-Apps signieren mit EINEM gemeinsamen Debug-Key `~/SK/Android/debug-shared.keystore` (SHA-256 F7:82:13:1C…, Alias androiddebugkey, Passwort android). OpenLauncher legt ihn beim Start nach `~/.android/debug.keystore`, deshalb nutzt jedes Projekt ohne eigene signingConfig ihn automatisch. Neue Projekte: nie einen eigenen Keystore erzeugen, keine Debug-signingConfig anlegen. Einzige Ausnahme: BestJournalAndroid-Release (release.keystore, Play Store). Meldet adb INSTALL_FAILED_UPDATE_INCOMPATIBLE: NIE deinstallieren (Datenverlust), sondern per apksigner-Rotation umziehen, Anleitung in best-practices/android/debug-signing.md.

16. Repo-Abgleich: Der OpenLauncher gleicht `~/proggs` vor jedem CLI-Start mit GitHub ab (`git pull --ff-only`). Trotzdem vor der ersten Dateiänderung einer Sitzung prüfen: `git -C ~/proggs fetch --quiet` und `git -C ~/proggs status -sb`. Steht dort `behind`, zuerst `git -C ~/proggs pull --rebase --autostash` ausführen. Bei Konflikten stoppen und melden, nie selbst auflösen. Nie mit einem veralteten Repo-Stand arbeiten.

17. TVO/CVO-Updates (TerminalVoiceOverlay, ClaudeVoiceOverlay) immer nur über das Updatescript: macOS /Users/frank/proggs/rebuild-overlay.sh mit TVO, CVO oder Both, Windows C:\Users\barwa\proggs\rebuild-overlay.ps1 mit TVO, CVO oder Both. Ablauf: Das Skript zeigt mir zuerst ein Ja/Nein-Fenster. Erst nach meinem Klick auf Ja beendet es das Overlay, baut und startet die neue Version. Klicke ich Nein, passiert nichts. Es gibt keinen -Force-Schalter: das Update ist immer an meinen Klick auf Ja gebunden und nicht überbrückbar. Nie Overlay-Prozesse von Hand beenden oder publish.ps1 bzw. build.sh direkt aufrufen. Das Skript nicht selbst abbrechen, solange das Fenster auf meinen Klick wartet: den Befehl mit dem höchsten erlaubten Zeitlimit starten, nicht mit dem kurzen Standard. Rückmeldungen: "Fertig — alle Ziele … erfolgreich" = neue Version läuft; OVERLAY_UPDATE_STATUS=cancelled = ich habe Nein geklickt; OVERLAY_UPDATE_STATUS=no-answer = 4 Minuten kein Klick, nichts geändert. Bei cancelled oder no-answer nicht erneut starten, sondern mir melden.

18. Android-Installation über WLAN: Apps kommen standardmäßig per WLAN (adb über TCP/IP) aufs Handy, das Kabel ist nur der Notnagel. Vor jeder Installation `adb devices` prüfen. Ist das Handy per WLAN (`IP:5555`) verbunden, darüber installieren (`adb -s IP:5555 …`, auch Logcat, Screenshots, Eingaben). Ist es nicht verbunden: ohne Rückfrage `/Users/frank/proggs/Werkzeuge/adb-wlan/adb-wlan.sh` ausführen. Das Skript verbindet per WLAN neu (mit Kabel: schaltet es WLAN und tcpip ein, ohne Kabel: nimmt es die letzte bekannte IP). Hängt das Handy per USB und WLAN gleichzeitig: immer das WLAN-Gerät mit `-s` wählen. Nur wenn WLAN trotz Skript scheitert, per Kabel installieren und mir den Grund melden (WLAN am Handy aus, anderes Netz, Handy neu gestartet → einmal Kabel anstecken und das Skript ausführen). Meldet `adb connect` "No route to host", obwohl Ping klappt: `adb kill-server` und neu verbinden.

19. Versionslog bei Android-Apps: Jede Android-App führt `app/src/main/assets/versionslog.json` (`"format": 1`, `"app"`, `"eintraege"`; je Eintrag `versionCode`, `versionName`, `stand`, `notiz`; neuester Eintrag unten, ein Eintrag pro Zeile). Er ist die einzige Quelle der Version: `build.gradle.kts` liest versionCode, versionName und VERSION_BUMPED_AT über den Block `versionslogAktuell` daraus — dort nie Versionen von Hand eintragen. Pro Commit an einer App genau einen Eintrag unten anhängen: `versionCode` = letzter + 1 (nie gleich, nie kleiner, sonst erkennt UpdateStation das Update nicht), `versionName` in der letzten Stelle erhöhen, `stand` = echte Systemzeit per Befehl im Format `23.09.2026, 15:10 Uhr` (nie schätzen), `notiz` = ein Satz auf Deutsch, was sich für den Nutzer ändert. Alte Einträge nie ändern oder löschen. Die Datei bleibt gültiges JSON in UTF-8 ohne BOM. Die Datei landet als Asset in der APK; UpdateStation zeigt daraus Versionsverlauf und „Neu in dieser Version“. Neue Android-Apps bekommen Versionslog und Gradle-Block von Anfang an (Vorlage: `UpdateStation/app/build.gradle.kts` und `UpdateStation/app/src/main/assets/versionslog.json`). Update fürs Handy bereitstellen: Skill `apk-update` — er legt die APK in den Drive-Ordner `Meine Ablage/Dokumente/Updates/<Projekt>` und ergänzt den Versionslog selbst, falls der versionCode nicht höher ist.
