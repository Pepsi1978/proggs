# Codex Cloud: CLI-Test bestätigt den Handy-Startkontext nicht

Dokumentiert: 05.10.2026, 19:31 Uhr.

## Beobachtung

In PR #162 wurden ein Repo-`AGENTS.override.md` und in der laufenden Cloud-Aufgabe
ein `/workspace/AGENTS.md` angelegt. `codex debug prompt-input` zeigte den Verweis
bei separat gestarteten CLI-Prozessen. Daraufhin wurde fälschlich zugesagt, dass
auch jede neue Handy-Cloud-Aufgabe die `cloud.md` vollständig liest.

Frank meldete anschließend, dass in einer neuen Handy-Session keine Regeldateien
gelesen worden waren. Der CLI-Test belegt den tatsächlichen mobilen Ablauf nicht.

## Ursache und Grenzen des Nachweises

Die Cloud-Konfiguration wurde nicht im Einrichtungsdialog aktualisiert und erneut
veröffentlicht. Neue Aufgaben verwenden einen eigenen Arbeitsbereich aus der
veröffentlichten Einrichtung; sie erben nicht automatisch die Änderungen der
gerade laufenden Aufgabe. Das ist eine mögliche Erklärung, kein nachträglich
bewiesener Ablauf der von Frank getesteten Sitzung.

## Korrektur

- Den zusätzlichen Repo-Override entfernen, damit PC-Profile regulär geladen werden.
- Die Pflichtlektüre als Startanweisung für das Cloud-Feld **Start skill** vorbereiten.
- Über **Edit → Save → Republish** in die Cloud-Konfiguration übernehmen.
- Eine wirklich neue Handy-Aufgabe prüfen, bevor eine automatische Einbindung zugesagt wird.

Der Starttext allein ist keine aktive Cloud-Konfiguration. Die normale Coding-Sitzung
hat hier nur das lesende Werkzeug `cloud_environment_environment_status`, kein
Werkzeug zum Setzen oder Veröffentlichen des Start skill.

Einrichtung und Quellen: `best-practices/agents/codex-cloud-startregeln.md`.
