# Projektregeln

## Automatisierung & Workflow
- Maximale Automatisierung: Nie nach Erlaubnis fragen fuer Standardaktionen (Build, Test, Commit, Push).
- Committen und Pushen immer direkt machen — nicht vorher fragen.
- Immer so viele Aufgaben wie moeglich **parallel** ausfuehren (Agent Teams nutzen).
- Nach jeder Code-Aenderung automatisch: Build → Test → Review → Verbessern (Schleife, bis Qualitaet stimmt).
- Bei Fehlern: Selbststaendig debuggen und fixen, nicht den Benutzer fragen.
- Ergebnisse ausfuehrlich erklaeren — der Benutzer ist kein Programmierer und will verstehen, was passiert ist.

## Qualitaetsschleife
- Jedes fertige Feature durchlaeuft mindestens 3 Pruefungen:
  1. Build & Test (kompiliert es, funktioniert es?) → Custom Agent: `tester`
  2. Code Review (ist der Code sauber und sicher?) → Custom Agent: `code-reviewer`
  3. Verbesserung (kann es eleganter, schneller, schoener sein?) → Custom Agents: `optimizer` + `ui-polisher`
- Erst wenn alle 3 bestanden sind, wird committed und gepusht.
- Bei neuen Projekten zuerst den `architect` Agent nutzen, um die Architektur zu planen.
- Bei Bugs den `debugger` Agent nutzen fuer systematische Diagnose.

## Skill-Erstellung
- Wenn ein neuer Skill erstellt, bearbeitet oder getestet werden soll, MUSS immer der `/skill-creator:skill-creator` Skill verwendet werden.
- Niemals Skills manuell erstellen oder bearbeiten, ohne den Skill Creator zu nutzen.

## Cross-Platform-Entwicklung & Config-Sync
- Alle Projekte muessen reibungslos auf macOS und Windows funktionieren.
- macOS: Swift/AppKit, Windows: C#/WPF.
- Bei Cross-Platform-Features immer beide Plattformen beruecksichtigen.
- Claude Code Konfiguration lebt in `~/proggs/claude-code-setup/` (GitHub: Pepsi1978/proggs/claude-code-setup/).
- NIEMALS neue GitHub-Repos erstellen. ALLES gehoert in `Pepsi1978/proggs`.
- Bei Aenderungen an Regeln, Agents, Commands oder Hooks: Immer auch nach `~/proggs/claude-code-setup/` kopieren und zu `Pepsi1978/proggs` pushen.
- UI-Design: Modern, poliert, professionell — muss aussehen wie gekaufte Software aus dem Laden.
- KEIN Python fuer GUIs oder Desktop-Apps. Python so weit wie moeglich vermeiden.
- Wenn Python unvermeidbar ist (z.B. ML-Backend): IMMER vorher den Benutzer fragen und erklaeren ob der Python-Code sichtbar oder unsichtbar (Backend) ist. Nie stillschweigend Python einsetzen.
- Bevorzugte Sprachen: Swift, C#, TypeScript, Rust, Go. In dieser Reihenfolge.
- Auslieferung: Eine einzelne .app (macOS) oder .exe (Windows) — keine Installationsabhaengigkeiten fuer den Endnutzer.

## Agent Teams
- Bei komplexen Aufgaben mit 3+ unabhaengigen Teilschritten: Agent Teams nutzen (3-5 Teammates optimal).
- Jeder Teammate braucht klaren Kontext: Was ist das Projekt, welche Dateien gehoeren ihm, welche Konventionen gelten.
- Datei-Ownership beachten: Verschiedene Teammates arbeiten an verschiedenen Dateien, um Konflikte zu vermeiden.
- Qualitaetspruefungen (Schritt 2+3 der Qualitaetsschleife) koennen parallel laufen.

## Sprache
- Kommunikation mit dem Benutzer auf Deutsch.
- Code-Kommentare und Commit-Messages auf Englisch.
