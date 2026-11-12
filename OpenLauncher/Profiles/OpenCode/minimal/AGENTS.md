# Open-Code-Profil: Minimal

Diese Regeln gelten für jede Programmieraufgabe!

1. Zugangsdaten, Secrets und Deploy-Wege liegen in: C:\Users\barwa\SK 
— Lies dort nach, bevor du deployst oder Zugangsdaten brauchst. Rate nie.

2. Best Practices und Bug Almanache liegen in den Ordnern C:\Users\barwa\proggs\best-practices und C:\Users\barwa\proggs\bugs
- Lies den passenden Almanach oder die passenden Best Practices, wenn ein Fehler auftritt oder du in einem Bereich arbeitest, den du in dieser Sitzung noch nicht angefasst hast.

3. Skills liegen ausschließlich im Repo-Ordner C:\Users\barwa\proggs\OpenLauncher\Profiles\ClaudeCode\standard\skills – für Claude Code, Codex und OpenCode, in jedem Profil und Modus. ~/.claude/skills, ~/.agents/skills und die Profil-Ordner "skills" sind nur Verknüpfungen dorthin. Skills nur dort bearbeiten, danach committen und pushen.

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
- Wenn ich sage: "fixe nach Direktive 3": lies zuerst vollständig `C:\Users\barwa\proggs\claude-code-setup\docs\rules\resilient-bugfixing.md` und fang erst danach mit dem Fix an.

9. Abschluss bei Code-Änderungen (Pflicht)
Sobald eine ` ; `-Teilaufgabe oder die Gesamtaufgabe Code geändert hat: → Version bumpen (echte Systemzeit) → bauen → committen → pushen → auf Gerät installieren bzw. deployen. Geht ein Schritt nicht (kein Gerät, roter Build, Push abgelehnt): melde das ausdrücklich, überspringe es nie stillschweigend.

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

12. Wenn der Emulator angefodert wird, Starte den Emulator, immer mit dem Fold 8 Profil über C:\Users\barwa\proggs\Werkzeuge\fold8-emulator

13. OpenLauncher-Updates immer nur über das Updatescript: Windows C:\Users\barwa\proggs\OpenLauncher\update-launcher.ps1, macOS /Users/frank/proggs/OpenLauncherMac/update-launcher.sh. Ablauf: Das Skript zeigt mir zuerst ein Ja/Nein-Fenster. Erst nach meinem Klick auf Ja schließt es den laufenden OpenLauncher, baut und startet die neue Version und endet danach sofort. Klicke ich Nein, passiert nichts. Nie mit -Force bzw. OPENLAUNCHER_UPDATE_FORCE=1 aufrufen, außer ich sage es ausdrücklich. Das Skript nicht selbst abbrechen, solange das Fenster auf meinen Klick wartet: den Befehl mit dem höchsten erlaubten Zeitlimit starten, nicht mit dem kurzen Standard. Rückmeldungen: started = neue Version installiert und gestartet; already-current = war schon installiert, fertig, kein Fenster; cancelled = ich habe Nein geklickt; no-answer = 4 Minuten kein Klick, nichts geändert. Bei cancelled oder no-answer nicht erneut starten, sondern mir melden.

14. Researches im Web erfolgen immer über den Researcher Skill. Der Ablauf den Resercher Skills wird immer eingehalten! Nach dem Research werden Best Parctices und Bug Almanache in den Ordnern C:\Users\barwa\proggs\best-practices und C:\Users\barwa\proggs\bugs gespeichert.

15. Android-Signierung: Alle Android-Apps signieren mit EINEM gemeinsamen Debug-Key `~/SK/Android/debug-shared.keystore` (SHA-256 F7:82:13:1C…, Alias androiddebugkey, Passwort android). OpenLauncher legt ihn beim Start nach `~/.android/debug.keystore`, deshalb nutzt jedes Projekt ohne eigene signingConfig ihn automatisch. Neue Projekte: nie einen eigenen Keystore erzeugen, keine Debug-signingConfig anlegen. Genau zwei Ausnahmen: (1) BestJournalAndroid-Release signiert nur seine Release-Builds weiter mit dem bisherigen Play-Keystore (release.keystore); daran ändert sich nichts. (2) GenialerWeckerAndroid (Verkaufs-App „Genialer Wecker“) signiert lokale Debug- UND Release-Builds mit einem eigenen Key unter `~/SK/GenialerWeckerAndroid`, nie mit dem gemeinsamen Debug-Key; Keystore und Passwörter liegen nur dort, nie im Git. Alle anderen Apps verwenden den gemeinsamen Debug-Key. Meldet adb INSTALL_FAILED_UPDATE_INCOMPATIBLE: NIE deinstallieren (Datenverlust), sondern per apksigner-Rotation umziehen, Anleitung in best-practices/android/debug-signing.md.

16. Repo-Abgleich: Der OpenLauncher gleicht `~/proggs` vor jedem CLI-Start mit GitHub ab (`git pull --ff-only`). Trotzdem vor der ersten Dateiänderung einer Sitzung prüfen: `git -C ~/proggs fetch --quiet` und `git -C ~/proggs status -sb`. Steht dort `behind`, zuerst `git -C ~/proggs pull --rebase --autostash` ausführen. Bei Konflikten stoppen und melden, nie selbst auflösen. Nie mit einem veralteten Repo-Stand arbeiten.

17. TVO/CVO-Updates (TerminalVoiceOverlay, ClaudeVoiceOverlay) immer nur über das Updatescript: Windows C:\Users\barwa\proggs\rebuild-overlay.ps1 mit TVO, CVO oder Both. Ablauf: Das Skript zeigt mir zuerst ein Ja/Nein-Fenster. Erst nach meinem Klick auf Ja beendet es das Overlay, baut und startet die neue Version. Klicke ich Nein, passiert nichts. Es gibt keinen -Force-Schalter: das Update ist immer an meinen Klick auf Ja gebunden und nicht überbrückbar. Nie Overlay-Prozesse von Hand beenden oder publish.ps1 direkt aufrufen. Das Skript nicht selbst abbrechen, solange das Fenster auf meinen Klick wartet: den Befehl mit dem höchsten erlaubten Zeitlimit starten, nicht mit dem kurzen Standard. Rückmeldungen: "Fertig — alle Ziele … erfolgreich" = neue Version läuft; OVERLAY_UPDATE_STATUS=cancelled = ich habe Nein geklickt; OVERLAY_UPDATE_STATUS=no-answer = 4 Minuten kein Klick, nichts geändert. Bei cancelled oder no-answer nicht erneut starten, sondern mir melden.

18. Android-Installation über WLAN: Standard ist adb über WLAN (`IP:5555`), das Kabel nur der Notnagel. Vor jeder Installation `adb devices`; ist das WLAN-Gerät da, alles darüber mit `-s IP:5555` (auch Logcat, Screenshots, Eingaben und wenn zusätzlich USB hängt). Fehlt es: ohne Rückfrage `powershell -ExecutionPolicy Bypass -File C:\Users\barwa\proggs\Werkzeuge\adb-wlan\adb-wlan.ps1` ausführen; es verbindet selbst neu (auch per WireGuard `10.8.0.7:5555` oder PC-Hotspot). Scheitert das: per Kabel installieren und den Grund melden (WLAN/WireGuard am Handy aus, anderes Netz, Handy neu gestartet → einmal Kabel anstecken, Skript ausführen). `adb connect` meldet "No route to host" trotz Ping: `adb kill-server`, neu verbinden. Jarvis (`de.frank.jarvis`) nie per nacktem `adb install`, sondern mit höchstem Zeitlimit über `C:\Users\barwa\proggs\Werkzeuge\jarvis-install\jarvis-install.ps1 <apk>`; `JARVIS_INSTALL_STATUS`: installiert = fertig, fehler = Grund melden; `INSTALL_FAILED_MISSING_SPLIT` ist dieser Schutz, kein Fehler. Zwei Geräte: `handy` (Galaxy Fold 8) und `tablet` (Galaxy Tab S9, „Jarvis Tablet“) stehen mit fester Seriennummer in `Werkzeuge/adb-wlan/geraete.json` und können gleichzeitig verbunden sein; ohne Angabe ist immer das Handy gemeint. Fürs Tablet: `adb-wlan.ps1 -Geraet tablet` (Mac und Server: `adb-wlan.sh --geraet tablet`), Jarvis mit `jarvis-install.ps1 <apk> -Ziel tablet` (Mac und Server: `jarvis-install.sh <apk> --ziel tablet`), Tunnel `10.8.0.8:5555`. Sind beide verbunden, nie „das erste Gerät“ nehmen: die Adresse des gemeinten über `adb -s <adresse> shell getprop ro.serialno` gegen `geraete.json` prüfen. Jarvis ist auf beiden Geräten dieselbe APK: Jede neue Jarvis-Version auf beide installieren, zuerst auf das Gerät, von dem der Auftrag kam; ist das andere nicht erreichbar, das melden. Alle anderen Apps ebenfalls auf beide, auf demselben Standardweg wie am Handy (WLAN, sonst Tunnel, Kabel nur als Notnagel), nie nur über UpdateStation: `~/proggs/Werkzeuge/adb-wlan/auf-beide.sh <apk…>` (Windows: mit Git Bash `"C:\Program Files\Git\bin\bash.exe"`, nie WSL-bash) installiert auf Handy und Tablet gleichzeitig und verbindet ein fehlendes Gerät selbst neu; ist das Tablet trotzdem nicht erreichbar, das melden (dann bleibt dort UpdateStation der Rückweg), bei den abgeglichenen Apps (Aufgaben, Ziele, Geniale Ideen, Epigenetik, Stack-Labor) ausdrücklich, weil ihr Abgleich über Jarvis gleiche Fassungen braucht.

19. Versionslog bei Android-Apps: Einzige Versionsquelle ist `app/src/main/assets/versionslog.json` (`"format": 1`, `"app"`, `"eintraege"` mit `versionCode`, `versionName`, `stand`, `notiz`; ein Eintrag pro Zeile, neuester unten; gültiges JSON, UTF-8 ohne BOM). `build.gradle.kts` liest die Version über den Block `versionslogAktuell`, dort nie von Hand eintragen. Pro Commit an einer App genau einen Eintrag anhängen: `versionCode` = letzter + 1 (sonst erkennt UpdateStation kein Update), `versionName` letzte Stelle + 1, `stand` = echte Systemzeit per Befehl als `23.09.2026, 15:10 Uhr`, `notiz` = ein deutscher Satz, was sich für den Nutzer ändert. Alte Einträge nie ändern oder löschen. Neue Apps bekommen Log und Gradle-Block von Anfang an (Vorlage: `UpdateStation/app/build.gradle.kts` und `UpdateStation/app/src/main/assets/versionslog.json`).

20. APK-Update nach jeder Android-Änderung: Jede neue oder geänderte Android-App kommt nach Bump, Bauen, Commit, Push und Installation ohne Rückfrage per Skill `apk-update` nach `Meine Ablage/Dokumente/Updates/<Projekt>`. Der Upload läuft im Hintergrund weiter: nie darauf warten, weder für die Abschlussmeldung noch für die Review-Frage. Meldet der Skill `vorbereitet`: genannte Dateien committen, pushen, Skill erneut starten; bei `fehler`: Ursache beheben, neu starten. Nie eine APK von Hand in den Ordner kopieren (ohne `update.json` sieht UpdateStation sie nicht). Abschlussmeldung: APK-Update ja/nein mit versionCode („gestartet“ = ja, Zusatz „Upload läuft im Hintergrund“).

21. Jarvis-Tagesauswertung beim Start: Rufe zu Beginn jeder neuen Sitzung, vor der ersten Aufgabe, einmal die letzte Tagesauswertung von Jarvis über den MCP-Server `jarvis` ab (Werkzeug `tagesauswertung_lesen`, ohne `neu_erstellen`; ist das Werkzeug zurückgestellt, lade es zuerst über die Werkzeugsuche). Sie ist dein Hintergrund zu meinem Tag: Dienst, Schlaf und Erholung, Termine, Aufgaben. Gib sie nicht ungefragt wieder und erwähne sie nur, wenn sie für die Aufgabe zählt. Steht im Ergebnis der Hinweis „GESAMTBILD FEHLT“, übergeh ihn: Für die Programmierarbeit genügt die Tagesauswertung. Ein Abruf je Sitzung genügt; nach einer Zusammenfassung des Kontexts nicht erneut abrufen. Ist der Server nicht angebunden oder antwortet er nicht (Handy aus, kein Netz), melde das in einem Satz und arbeite normal weiter. Nie deswegen warten, nachfragen oder die Aufgabe abbrechen.

22. Code-Review am Ende: Ist eine Programmieraufgabe samt Pflicht-Abschluss fertig und das APK-Update angestoßen, frag sofort, ohne auf den Upload zu warten, „Zusätzliches Code-Review?“ mit „Nein“, „Ja, Standard“, „Ja, sehr ausführlich (bis 100 Befunde)“ und Freitext („Other“; befolgen, z. B. nur bestimmte Dateien). Keine Antwort nach 5 Minuten: selbst entscheiden und weiterarbeiten; kennt das Frage-Werkzeug keine Zeitgrenze, bleibt die Frage offen. Standard: Claude Code `/code-review`, andere CLIs gründliche Prüfung des ganzen Diffs. Sehr ausführlich: nur die in dieser Sitzung geänderten Stellen, bis 100 Befunde (Claude Code `/code-review max --max-findings 100`, andere CLIs eigene sehr gründliche Prüfung). Bei Ja: Befunde beheben und den Pflicht-Abschluss inkl. `apk-update` wiederholen. Die erste Antwort zählt, egal wo (CLI oder Jarvis); danach nie erneut fragen. Verlangt der Auftrag das Review schon (z. B. Jarvis „REVIEW: ja“): nicht fragen, sondern vor dem Abschluss ausführen (Standard, außer sehr ausführlich verlangt).

23. Sitzungs-Auswertung zuletzt: Ist eine Programmieraufgabe komplett abgeschlossen (Abschlussmeldung gegeben, Review-Frage erledigt), führe als allerletzten Schritt ohne Rückfrage den Skill `session-flaschenhals` aus. Nicht bei reinen Fragen ohne Codeänderung.
