# OpenCode Kompass

Eigenständige Android-Variante von Claude Kompass und Codex Kompass: gleiche Oberfläche,
Navigation und App-Einstellungen, mit einer deutschen Referenz für die OpenCode CLI.

## Inhalt

- 23 dokumentierte Slash-Einträge einschließlich Aliasnamen, Stand OpenCode 1.18.30.
- Deutsche Erklärungen, Beispiele und Hinweise zur Bedienung, sofort offline lesbar.
- Config-Bereich mit 27 Einträgen zu `opencode.json`, `tui.json` und den Umgebungsvariablen.
- 10 auf OpenCode zugeschnittene Praxisartikel.
- Vorlesen, Rückfragen, Vertiefen und Zurücknehmen, mehrere Chats, Suche, Hell-/Dunkelmodus,
  App-Sperre und Sicherung wie in den Schwester-Apps.

Der gemeinsame Code liegt in `../KompassKern`. Eigen sind nur `update/`, `AppProfil` und die
Beigaben. Paketkennung: `de.frank.opencodekompass`. Alle drei Apps können nebeneinander
installiert sein; Datenbank, Ablagen und Sicherungen sind anhand der App-Kennung getrennt.

Die KI-Funktionen (Vertiefen, Rückfragen, Erklärungen nachziehen) laufen wie in den Schwester-Apps
über die Anmeldung per Gerätecode bei OpenAI. Das ist der Zugang der App zum Modell und hat mit
der Anbieterwahl innerhalb der OpenCode CLI nichts zu tun.

## Quellen und Grenzen

- [Befehle in der TUI](https://opencode.ai/docs/tui/) — Quelldatei `packages/web/src/content/docs/tui.mdx`
- [Konfiguration](https://opencode.ai/docs/config/) — Quelldatei `config.mdx`
- [Fassungen](https://github.com/anomalyco/opencode/releases)

Dokumentiert sind die eingebauten Befehle. Eigene Befehle aus dem Ordner `commands/` oder aus dem
Schlüssel `command` der Konfiguration kennt nur die jeweilige Installation; sie können hier nicht
stehen. Der Zweig im Projektarchiv heißt `dev`, nicht `main` — ein Abruf von `main` läuft ins
Leere.

## Aktualisierung

Der Aktualisieren-Knopf liest ausschließlich den abgegrenzten Abschnitt „Commands" aus `tui.mdx`
und die Fassungen aus den GitHub-Releases. Bei einer unlesbaren Version oder weniger als zehn
gelesenen Befehlen bricht er vor der Bestandsänderung ab. Die deutsche Ausgangserklärung bleibt
beim Quellenabgleich erhalten; geänderte englische Angaben werden nachgeführt.

## Bauen und installieren

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Die Offline-Daten werden mit `python tools/baue_assets.py` erzeugt. Der Generator liest dieselben
Quellen und dieselben Abschnittsregeln wie der Aktualisieren-Knopf; die deutschen Erklärungen
kommen aus `tools/erklaerungen.txt`. Fehlt für einen neuen Befehl die deutsche Erklärung, bricht er
ab, statt eine unvollständige Auslieferung zu schreiben.

Die Versionslinie wird aus Codex Kompass 0.4.14 fortgeführt. Version und `VERSION_BUMPED_AT` stehen
gemeinsam in `app/build.gradle.kts` und werden im Einstellungsbildschirm angezeigt.

## Sicherung

Was in die Sicherung kommt, wird im Einstellungsbildschirm angehakt: die drei Wissensbereiche
jeweils vollständig, dazu die eigenen Fragen und die Gespräche. Beim Wiederherstellen wird nur
ergänzt, was fehlt — Vorhandenes bleibt unverändert.

Die Sicherung schreibt in einen einmal gewählten Ordner — in der Praxis ein Google-Drive-Ordner.
Nach der einmaligen Freigabe schreibt „Jetzt sichern" ohne weitere Rückfrage dorthin; es liegen
immer nur die aktuelle Sicherung und die eine davor darin. Der Dateiname trägt den Zeitpunkt:
`12-09-2026-1224-opencode-kompass.json`. Das Modul liegt im gemeinsamen Kern und gilt damit für
alle drei Kompass-Apps.
