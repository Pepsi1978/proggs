# Codex Cloud: gemeinsame Startregeln ohne PC-AGENTS.md

Geprüft: 05.10.2026, 19:31 Uhr.

## Unterstützter Cloud-Einstieg

Die aktuelle Codex-Cloud-Umgebung hat ein Feld **Start skill** für Startanweisungen.
Dieses Feld ist der einzurichtende Träger der Pflichtlektüre. Eine beliebige
`cloud.md` im Repo wird nicht allein durch ihren Namen zum Startkontext.

Die vorbereiteten Anweisungen liegen in
`OpenLauncher/Profiles/ClaudeCode/sources/codex-cloud-start.md`.
Die eigentlichen Regeln bleiben in `OpenLauncher/Profiles/ClaudeCode/sources/cloud.md`.
Der Starttext liest diese Quelle vollständig; Regeln werden nicht dupliziert.

## Einrichten und veröffentlichen

1. Im Web oder in der Desktop-App **Settings → Codex Cloud → Environments** öffnen.
2. Bei der vorhandenen `proggs`-Umgebung **… → Edit** wählen.
3. Im Einrichtungs-Chat anweisen, den Inhalt von `codex-cloud-start.md` am Anfang
   des vorhandenen **Start skill** zu ergänzen. Bestehende Service-Startanweisungen
   erhalten. Keine globale Personalisierung und keine lokalen PC-Dateien ändern.
4. Den Lesevorgang in der Einrichtung prüfen, die Konfiguration speichern und
   **Republish** wählen. Nur die Repo-Datei zu committen reicht hierfür nicht.
5. Eine wirklich neue Aufgabe in der Handy-App aus der aktualisierten Umgebung
   starten. Prüfen, dass die Datei vor der ersten inhaltlichen Antwort vollständig
   gelesen wurde; den vollständigen Pfad und den GitHub-Schlüsselort abfragen.

Eine automatisch und zwingend ausgeführte Pflichtlektüre vor jeder ersten Antwort
ist mit dieser Anleitung noch nicht Ende zu Ende nachgewiesen. Die Dokumentation
beschreibt den Start skill als Cloud-Startanweisungen; die konkrete Einbindung
und ihr Verhalten müssen im Einrichtungsdialog und einer neuen Handy-Aufgabe
geprüft werden. Ein CLI-Test allein ersetzt diese Prüfung nicht.

## Aufgaben und veröffentlichte Vorlage unterscheiden

Neue Aufgaben erhalten jeweils einen eigenen Arbeitsbereich aus der veröffentlichten
Umgebung. Eine laufende Aufgabe behält ihre eigenen Dateien. Änderungen an deren
`/workspace/AGENTS.md` ändern die veröffentlichte Vorlage nicht automatisch.
Ein aktualisiertes Repo und eine erneut veröffentlichte Umgebung sind verschiedene Schritte.

Am PC/Mac bleibt die vom OpenLauncher erzeugte `AGENTS.md` maßgeblich.
Keinen versionierten Repo-Override verwenden, um die Cloud-Einbindung zu ersetzen.
Lokale CLI-Konfigurationsbeispiele sind kein Nachweis für Cloud-Orchestrierung.

## Quellen

- [OpenAI: Cloud environments](https://learn.chatgpt.com/docs/environments/cloud-environments)
  — Start skill, Edit/Republish, getrennte Arbeitsbereiche und mobile Nutzung.
- [OpenAI: Using Codex Cloud](https://help.openai.com/en/articles/20001545-using-codex-cloud)
  — neue Aufgaben aus der veröffentlichten Einrichtung; Einrichtung im Web/Desktop.
- [OpenAI: Custom instructions with AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
  — Dateisuche relativ zur Arbeitswurzel, getrennt von der Cloud-Einrichtung.
