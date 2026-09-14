# Codex Kompass

Eigenständige Android-Variante von Claude Kompass: gleiche Oberfläche, Navigation und App-Einstellungen, mit einer deutschen Referenz für Codex CLI.

## Inhalt

- 59 dokumentierte Slash-Einträge einschließlich Aliasnamen, Stand Codex CLI 0.153.3.
- Deutsche Erklärungen, Beispiele und Hinweise zur Verfügbarkeit, sofort offline lesbar.
- Config-Bereich mit dem vollständigen Schlüssel-Katalog aus der offiziellen Konfigurations-Referenz — über vierhundert Schlüssel aus `config.toml` und `requirements.toml`, jeder mit seinem Typ.
- 10 auf Codex zugeschnittene Praxisartikel.
- Vorlesen, Rückfragen, Vertiefen und Zurücknehmen, mehrere Chats, Suche, Hell-/Dunkelmodus, App-Sperre und Sicherung wie in Claude Kompass.

Die gesamte Einstellungsoberfläche stammt aus der Vorlage: Codex-Anmeldung per Gerätecode, Modelle und Denktiefe, Google-/Edge-/Qwen-Stimmen, eigene Stimme, Groq-Diktat mit Filtern, Protokolle und Sicherung. Zugangsdaten werden nicht in die App eingebaut. Als eigenständige App benötigt Codex Kompass eine eigene Anmeldung und eigene Schlüssel in den Einstellungen; die privaten Daten von Claude Kompass werden nicht ausgelesen.

Paketkennung: `de.frank.codexkompass`. Beide Apps können nebeneinander installiert sein. Sicherungen werden anhand ihrer App-Kennung getrennt.

## Quellen und Grenzen

- [Offizielle CLI-Befehle](https://learn.chatgpt.com/docs/developer-commands?surface=cli)
- [Offizielles Änderungsprotokoll](https://learn.chatgpt.com/docs/changelog)
- [Konfiguration](https://learn.chatgpt.com/docs/config-file/config-basic)

Die Übersicht dokumentiert nicht alle neuen Befehle sofort. /cd, /pwd, /cwd, /export und /recap kommen ergänzend aus dem Änderungsprotokoll; /clean steht als Alias im Fließtext. Weitere Aliasse sind /subagents, /btw und /pet. Unbelegte Einführungsfassungen werden als unbekannt angezeigt. Eine Erwähnung in einer Fehlerkorrektur ist kein Beleg für die Einführung. Die genaue Syntax von /cwd ist in diesen Quellen nicht erklärt und wird deshalb nicht erfunden.

## Aktualisierung

Der Aktualisieren-Knopf ruft ausschließlich OpenAI-Unterlagen ab. Er liest den abgegrenzten Abschnitt „Built-in slash commands“ samt Aliasnamen deterministisch aus. Bei einer unlesbaren Version oder weniger als 35 Tabelleneinträgen bricht er vor der Bestandsänderung ab. Neue Namen und jede nachgeholte Erklärung werden einzeln gespeichert. Die bekannten Ergänzungen aus Release Notes werden nicht allein wegen ihrer Abwesenheit in der Übersicht als entfernt markiert.

Seit Fassung 0.6.9 gleicht derselbe Lauf auch die Einstellungen ab. Vorher kannte der Abgleich nur den Befehlsbereich: Die Einstellungsliste hing an gar keiner Quelle und stand mit einem einzigen Platzhalter da, während die Referenz über vierhundert Schlüssel führt. Gelesen wird die Komponente `<ConfigTable>` der Referenzseite — mit beiden Sorten Anführungszeichen, weil ein Typ mit einem Anführungszeichen darin die Seite auf einfache ausweichen lässt, und über Zeilengrenzen hinweg, weil lange Beschreibungen umbrechen. Unter zweihundert gelesenen Schlüsseln bricht der Lauf ab, statt den Bestand zu leeren. Eine Angabe „seit Fassung X“ bekommen Einstellungen bewusst nicht: Das Release-Fenster reicht nur ein Dutzend Fassungen zurück, ein Schlüssel wie `model` ist weit älter.

Eine neue, nur im Änderungsprotokoll erwähnte Ergänzung muss redaktionell ergänzt werden, solange OpenAI sie noch nicht in die Übersicht aufgenommen hat. Die App behauptet daher keine automatische Vollständigkeit gegenüber undokumentierten oder internen Debug-Befehlen. Die deutsche Ausgangserklärung bleibt beim Quellenabgleich erhalten; geänderte englische Angaben werden nachgeführt, wie in der Vorlage.

## Bauen und installieren

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Zielgerät wie bei Claude Kompass: Galaxy Z Fold 8 (SM-F971B). Eine Installation setzt ein per ADB erreichbares Gerät voraus.

Die Offline-Daten werden mit `python tools/baue_assets.py` erzeugt. Der Generator nutzt die offiziellen Quellen und `tools/erklaerungen.txt`. Fehlt für einen neuen Befehl die deutsche Erklärung, bricht er ab, statt eine unvollständige Auslieferung zu schreiben. Alternativ kann `--quellen` auf einen lokalen Ordner mit `commands.md` und `changelog.html` zeigen.

Die Versionslinie wird aus der Vorlage 0.4.2 fortgeführt. Version und VERSION_BUMPED_AT stehen gemeinsam in app/build.gradle.kts und werden im Einstellungsbildschirm angezeigt.

## Performance ab 0.4.6

Die funktionserhaltenden Overlay-Optimierungen sind auf die Android-Pfade übertragen:

- Der Chat-Nachrichtenfluss wechselt nur bei einer anderen Sitzung. Tippen, Menüwechsel und Statusmeldungen starten keine neue Room-Beobachtung derselben Sitzung mehr.
- Referenzlisten werden nur bei Datenänderungen neu aufbereitet, außerhalb des UI-Threads. Auf-/Zuklappen und andere Bedienaktionen verwenden die vorhandenen Listen weiter. Room-Aktualisierungen, Bereichs-Cache und die bestehende Lebenszyklussteuerung bleiben erhalten.
- Nach Aufnahmeende entsteht die WAV direkt aus dem geschützten PCM-Puffer in einer einzigen Ergebnis-Allokation. Eine vollständige zusätzliche PCM-Kopie entfällt; Header, Sample-Rate, Audiodaten, Puffergrenze und Stop-Schutz bleiben unverändert. Das gilt auch für die Referenzaufnahme der eigenen Stimme.

Bereits vorhanden und unverändert: gemeinsame Audioanalyse für Vor-/Nachfilter, langlebiger OkHttp-Verbindungspool, Hintergrund-Transkription, vier Halluzinationsfilter und Größenaufteilung. Die Windows-spezifischen curl-, Clipboard- und Gemini-Live-Abbaupfade existieren hier nicht. Keine Modell-/Qualitätsänderung oder zusätzlichen Transkriptionsanfragen. Im Schnellmodus keine neuen Funktionstests oder Benchmarks; keine gemessene End-to-End-Beschleunigung zugesagt.
