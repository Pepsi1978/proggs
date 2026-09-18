# Prüfstand der ersten nutzbaren Fassung

Skill-Fassung 1.0.0, erstellt am 18.09.2026, 11:57 Uhr (Systemzeit per PowerShell gelesen). Diese Versionsangabe gehört zum Skill, nicht zur OpenLauncher-App.

## Lokale technische Prüfung

- Python-Syntax, YAML-Frontmatter, Codex-Metadaten und lokale Referenzlinks geprüft.
- Helfer auf einem eigens erzeugten temporären tmux-Server mit einem lokalen Python-Empfänger ausgeführt. Kein Claude-Modellaufruf und keine Eingaben in die bestehenden Nutzersitzungen.
- Zielbindung, lesende Momentaufnahme und Unterdrückung gleicher Textauszüge erfolgreich.
- UTF-8, Anführungszeichen, Apostroph, Shell-Metazeichen, LF und Tab unverändert übertragen; Bracketed-Paste-Klammern und separates Enter bytegenau beim Empfänger beobachtet.
- Falscher Beobachtungstoken, wiederholtes Einfügen, wiederholtes Enter und ausgetauschte Prozessidentität zurückgewiesen.
- Temporären Testserver danach beendet und Testdateien entfernt. Keine Änderungen an tmux-Konfiguration oder Nutzer-Sitzungen.

Die vorher im Nutzerdialog nachgewiesene Verbindung zu Claude in Codex-Terminal und Terminal.app belegt den grundlegenden Schreibweg. Der lokale Helfertest belegt den Transport, **nicht** automatische Erkennung von Ghost-Suggestions, Eingabebereitschaft oder Codekorrektheit. Kein neuer kostenpflichtiger Claude-End-to-End-Test für diese Skill-Fassung.

## Drei kurze Anwendungsszenarien

1. „Gib Claude diesen Auftrag … warte, ich ergänze noch … und schick ihn ab.“ Erst den vollständigen Beitrag mit letzter Korrektur zusammenführen, dann genau eine Kennung und eine Übergabe; keine zusätzliche Bestätigungsrunde.
2. „Füge den mehrzeiligen Text nur ein.“ Ziel bestätigen, freien Claude-Eingabebereich prüfen, Paste ausführen und lesen; kein Enter. Bei echtem fremdem Entwurf oder unklarer Ghost-Suggestion nichts schreiben.
3. „Begleite die Umsetzung.“ Adaptive, unterbrechbare Momentaufnahmen und gezielte reale Dateimeilensteine lesen. Unveränderter Hash ist kein Abschluss; neuer Marker ist ein Selbstbericht. Timeout führt zu Lesen, nicht Wiederholung; „Stopp“ beendet die Begleitung.

Dies sind dokumentierte Sollabläufe, keine behaupteten Modell-Benchmarks oder gemessenen Einsparquoten.
