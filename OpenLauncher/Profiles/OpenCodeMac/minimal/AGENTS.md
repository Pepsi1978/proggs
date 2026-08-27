# Open-Code-Profil: Minimal

Diese Regeln gelten für jede Programmieraufgabe!

1. Zugangsdaten, Secrets und Deploy-Wege liegen in: /Users/frank/SK 
— Lies dort nach, bevor du deployst oder Zugangsdaten brauchst. Rate nie.

2. Best Practices und Bug Almanache liegen in den Ordnern /Users/frank/proggs/best-practices und /Users/frank/proggs/bugs
- Lies den passenden Almanach oder die passenden Best Practices, wenn ein Fehler auftritt oder du in einem Bereich arbeitest, den du in dieser Sitzung noch nicht angefasst hast.

3. Skills liegen im Ordner /Users/frank/proggs/OpenLauncher/Profiles/ClaudeCodeMac/minimal/skills

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
  - Android: `app/build.gradle.kts` — `versionName = "1.0.27"` UND `buildConfigField("String", "VERSION_BUMPED_AT", "\"19.07.2026, 21:00 Uhr\"")`. 
  Immer beide zusammen, nie nur eines. Uhrzeit mit Doppelpunkt, nicht mit Punkt. 
  - .NET/WPF: `<Version>` in der `.csproj`, Form `2.1.84`. 
  - Node/TypeScript: `version` in der Wurzel-`package.json`. Bei WerftStudio mit Zeitstempel-Suffix, Form `0.32.2-20260809.1545`.
- Wird die Version in der App angezeigt (meist im Einstellungs-Bildschirm, sonst auf anderen Seiten suchen), zieh die Anzeige mit.
- Weicht ein Projekt von diesen Mustern ab, steht die Regel in dessen eigener AGENTS.md. Findest du dort nichts und passt kein Muster: frag nach, rate nicht.

7. Für alle research Aufgaben nutze den skill research und folge seinen Anweisungen.

8. Externe Regelwerke
"Direktive 3" bezeichnet eine externe Datei, nicht Abschnitt 3 dieses Profils.
- Wenn ich sage: "fixe nach Direktive 3": lies zuerst vollständig `/Users/frank/proggs/claude-code-setup/docs/rules/resilient-bugfixing.md` und fang erst danach mit dem Fix an.

9. Abschluss bei Code-Änderungen (Pflicht)
Sobald eine ` ; `-Teilaufgabe oder die Gesamtaufgabe Code geändert hat: → bauen → Version bumpen (echte Systemzeit) → committen → pushen → auf Gerät installieren bzw. deployen. Geht ein Schritt nicht (kein Gerät, roter Build, Push abgelehnt): melde das ausdrücklich, überspringe es nie stillschweigend.

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

13. Bei Updates von OpenLauncher immer das Updatescript benutzen! Für macOS unter /Users/frank/proggs/OpenLauncherMac/update-launcher.sh und für Windows unter /Users/frank/proggs/OpenLauncher/update-launcher.ps1