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

6. Zeitlimits gestaffelt und sichtbar, nie blind warten (jedes Profil, jeder Modus, auch Schnellmodus; gilt für Codex, OpenCode, Kimi, Antigravity und Gemini): Hier bricht ein Zeitlimit den Befehl ab. Befehle, die länger als eine Minute dauern können (Bau, Installation, Update-Skripte), deshalb selbst im Hintergrund starten: `<befehl> > /tmp/lauf-<name>.log 2>&1 &` (Windows PowerShell: Start-Process mit -RedirectStandardOutput). Dann jede Minute ein eigener, sichtbarer Prüfschritt: `python3 ~/proggs/Werkzeuge/lauf-waechter/minute.py /tmp/lauf-<name>.log <minute>` (Windows `python -X utf8`); FERTIG erkennt das Werkzeug, wenn kein Prozess mehr in die Datei schreibt. Danach in einem Satz die Einschätzung für Frank: Minute N, läuft fehlerfrei / wartet absichtlich / Fehler / Hänger, was gerade passiert. WARTET (Ja/Nein-Dialog von update-launcher.sh oder rebuild-overlay.sh, Overlay von jarvis-install.sh, sleep) nie beenden: Frank entscheidet gerade; das ersetzt bei diesen Skripten das „höchste Zeitlimit“ aus den Regeln oben. Bei FEHLER oder HÄNGER (keine CPU-Zeit, keine Ausgabe, Werkzeug wartet auf Eingabe) sofort beenden und die Ursache beheben, statt erneut zu warten. Lange Befehle nie in `| tail` oder `| head` leiten (hält die Ausgabe bis zum Ende zurück), nie mit sleep beginnen, nie mit pgrep -f auf den eigenen Befehlstext warten, nie `ls` zum Finden einer Datei, feste Pfade nutzen. Ein Jarvis-Bau ohne Änderung dauert unter einer Sekunde, mit Änderung unter einer Minute: Dauert es deutlich länger, ist es ein Fehler.
