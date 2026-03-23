# Codex Resilient Bugfixing

Diese Regel ist die dritte Systemdirektive unter der `## Oberste Direktive`.
Sie gilt fuer jeden Bugfix in der Codex-Programmierumgebung selbst:

- Hooks
- Regeln
- Settings
- MCP-Server
- Validierung
- Skripte
- Agents
- Skills

Sie gilt nicht fuer normale Projekt- oder App-Bugs des Benutzers.

## Kernprinzip

- Ein Umgebungsfehler ist erst dann wirklich gefixt, wenn der Fix zukunftssicher ist.
- Derselbe Fehler soll in dieser Programmierumgebung niemals ein zweites Mal auftreten.
- Bugfixing ist erst fertig, wenn Root Cause, verwandte Fehlerquellen, Zukunftssicherheit, Schadensfreiheit und Dokumentation abgesichert sind.

## Zusammenspiel mit Direktive 1 und 2

- Direktive 1 setzt das Ziel: mehr Intelligenz pro Session.
- Direktive 2 erkennt waehrend der Arbeit, wo Fehler und Reibung liegen.
- Direktive 3 sorgt dafuer, dass erkannte Umgebungsfehler so gefixt werden, dass sie nicht wiederkommen.

Ohne resilientes Bugfixing wuerde Codex dieselben Umweltfehler immer wieder reparieren statt echte Intelligenz aufzubauen.

## Die 5 Pflichtschritte

### Schritt 1: Root Cause finden

- Nicht das Symptom fixen, sondern die eigentliche Ursache.
- Vor jedem Umgebungsfix mindestens 3x `Warum?` fragen, bevor der Fix gebaut wird.
- Root Cause bedeutet: die tiefste technische Ursache finden, nicht nur das sichtbare Fehlersymptom.
- Wenn plattformabhaengiges Default-Verhalten die eigentliche Ursache ist, muss dieses Verhalten explizit erzwungen oder neutralisiert werden.

## Schritt 2: Verwandte Fehlerquellen suchen

Vor dem Fix muessen drei Fragen beantwortet werden:

1. Gleiche Fehlerklasse: Kann derselbe Fehlertyp an anderer Stelle auftreten?
2. Gleiche Komponente: Kann dieselbe Komponente auf andere Weise ebenfalls versagen?
3. Gleiche Abhaengigkeit: Welche anderen Teile haengen von derselben Ursache ab?

Ein Fix ist unvollstaendig, wenn nur die sichtbare Instanz gefixt wird, aber die verwandten Oberflaechen ungeprueft bleiben.

## Schritt 3: Zukunftssicheren Fix implementieren

Ein Umgebungsfix ist nur dann vollstaendig, wenn er diese Eigenschaften hat:

1. `Self-Healing`: Er stellt sich nach Updates, Neustarts oder Plattformwechseln wieder her oder bleibt automatisch korrekt.
2. `Defensiv`: Er faengt die Fehlerklasse ab, nicht nur den Einzelfall.
3. `Ueberlebbar`: Er uebersteht Plugin-, CLI-, Config- und OS-Aenderungen moeglichst ohne manuelle Nacharbeit.
4. `Erweiterbar`: Er ist fuer aehnliche kuenftige Faelle ausbaubar.
5. `Dokumentiert`: Ursache, Fixmuster und Vermeidungsregel stehen in der Fix-Datenbank.
6. `Schadensfrei`: Der Fix selbst fuehrt keine neuen Fehler ein.

### Schritt 3b: Fix-Induced-Failure-Pruefung

Vor jedem Commit eines Umgebungsfixes muessen diese 8 Fragen beantwortet werden:

1. `Abhaengigkeiten`: Was haengt vom geaenderten Code oder Verhalten ab?
2. `Fehlszenarien`: Was passiert, wenn der Fix-Code selbst scheitert?
3. `Zustandsaenderungen`: Veraendert der Fix einen dauerhaften Systemzustand?
4. `Race Conditions`: Kann der Fix mit anderem Code kollidieren?
5. `Rueckwaertskompatibilitaet`: Bricht der Fix etwas, das vorher funktionierte?
6. `Plattform-Effekte`: Funktioniert der Fix auf macOS und Windows?
7. `Update-Resistenz`: Ueberlebt der Fix spaetere Updates?
8. `Graceful Degradation`: Gibt es einen sicheren Fallback, wenn Voraussetzungen fehlen?

Die Leitfrage dahinter lautet:

- Was ist das Schlimmste, das passieren kann, wenn dieser Fix deployt wird und danach 6 Monate niemand hinschaut?

## Schritt 4: Defense in Depth

Nie nur eine einzige Schutzschicht bauen. Gute Umgebungsfixes kombinieren mindestens zwei oder drei der folgenden Ebenen:

- `Praeventiv`: Problem verhindern, bevor es auftritt
- `Reaktiv`: Problem im Fehlerfall abfangen und degradiert weiterlaufen
- `Selbstheilend`: Nach Updates oder Drift automatisch wiederherstellen
- `Upstream` optional: Den eigentlichen Verursacher melden oder upstream beheben

## Schritt 5: Fix dokumentieren und teilen

Jeder Umgebungsfix gehoert in `codex-setup/state/environment-fixes.json`.

Jeder neue oder aktualisierte Eintrag muss fuer andere CLIs ohne Session-Kontext verstaendlich sein und mindestens enthalten:

- Kontext
- exakten oder wiedererkennbaren Symptomtext
- Root Cause
- 3x-Warum- bzw. Why-Chain-Zusammenfassung
- gepruefte verwandte Fehlerquellen
- falsches Muster und richtiges Muster
- klare Vermeidungsregel im Wenn-Dann-Stil
- Resilienz-Zusammenfassung
- Fix-Induced-Failure-Review
- Verifikation

## Bekannte plattformuebergreifende Fehlerquellen

- File Encoding: auf Windows nie auf implizites Default-Encoding verlassen
- Atomares Schreiben: kritische Konfigurationsdateien nicht direkt mit destruktivem `w` ueberschreiben
- Temp-Verzeichnisse: immer plattformgerechte Temp-Pfade verwenden
- Hook-Shells: `.sh` und `.ps1` als Paar denken
- Home- und Tool-Pfade: keine fragilen Hardcodes
- Locale- und Tool-Unterschiede: Shell- und Regex-Verhalten nicht blind uebertragen

## Was niemals passieren darf

- Nur das Symptom fixen, ohne die Root Cause zu verstehen
- Einen Fix deployen, der beim naechsten Update oder Neustart sofort wieder bricht
- Nur die sichtbare Instanz fixen, ohne verwandte Fehlerquellen zu pruefen
- Einen Umgebungsfix ohne Eintrag in der Fix-Datenbank committen
- Einen Fix deployen, der selbst neue Fehler einfuehrt
- Die 8-Punkte-Pruefung ueberspringen
- Einen Fehler ein zweites Mal machen, obwohl er schon einmal erkannt und gefixt wurde
