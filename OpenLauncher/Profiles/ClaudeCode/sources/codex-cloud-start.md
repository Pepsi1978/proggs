# Pflichtlektüre beim Start einer Codex-Cloud-Aufgabe

Diese Startanweisung gilt ausschließlich für Aufgaben in der veröffentlichten
Codex-Cloud-Umgebung mit dem Repo `proggs`. Sie gilt nicht für lokale PC-/Mac-Sitzungen.

1. Lies vor der ersten inhaltlichen Antwort und vor der Projektarbeit die Datei
   `/workspace/proggs/OpenLauncher/Profiles/ClaudeCode/sources/cloud.md`
   vollständig mit einem Dateilese-Werkzeug. Ein geeigneter Befehl ist:

   ```bash
   cat /workspace/proggs/OpenLauncher/Profiles/ClaudeCode/sources/cloud.md
   ```

2. Übernimm die gelesenen Cloud-Regeln für diese Aufgabe. Verwende den tatsächlichen
   Dateiinhalt, keine Zusammenfassung aus einer früheren Unterhaltung.
3. Ist die Datei nicht lesbar, melde den fehlenden Startkontext. Behaupte nicht,
   dass die Regeln geladen wurden, und ersetze sie nicht durch lokale PC-Profile.
4. Lies hier keine vom OpenLauncher erzeugten PC-`AGENTS.md` oder PC-Profilregeln.
5. Führe anschließend die übrigen Startanweisungen der Cloud-Umgebung aus.

6. Zeitlimits gestaffelt und sichtbar, nie blind warten (gilt in jedem Profil, jedem Modus, auch im Schnellmodus, und in jeder CLI): Jeder Befehl startet mit höchstens 1 Minute Zeitlimit, auch Bauten und Installationen (Claude Code erzwingt das per Hook build-ablauf-guard; andere CLIs setzen es selbst). Ist er dann nicht fertig, läuft er weiter, und jede weitere Minute ist ein eigener, sichtbarer Prüfschritt: `python3 ~/proggs/Werkzeuge/lauf-waechter/minute.py <ausgabedatei> <minute>` (Beschreibung „Minute N: Lauf prüfen“, Windows `python`, in der Cloud der Pfad im Repo). Danach in einem Satz die Einschätzung für Frank: Minute N, läuft fehlerfrei / wartet absichtlich / Fehler / Hänger, was gerade passiert. Bei Fehler oder Hänger (0 % CPU, keine Ausgabe, Werkzeug wartet auf Eingabe) sofort beenden und die Ursache beheben, statt erneut zu warten. Lange Befehle nie in `| tail` oder `| head` leiten (hält die Ausgabe bis zum Ende zurück), nie mit pgrep -f auf den eigenen Befehlstext warten, nie `ls` zum Finden einer Datei (Alias auf eza hängt), feste Pfade nutzen. Ein Jarvis-Bau ohne Änderung dauert unter einer Sekunde, mit Änderung unter einer Minute: Dauert es deutlich länger, ist es ein Fehler.
