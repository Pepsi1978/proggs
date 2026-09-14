# Während der Programmierung mitlesen

Nur während eines beauftragten Prüflaufs anwenden. Den tatsächlichen Worktree bestimmen, Ziel und Ausgangsstand festhalten. Bereits vorhandene Änderungen und parallele Bearbeiter berücksichtigen; ein Diff allein identifiziert seinen Autor nicht.

## Kleiner wiederholbarer Ablauf

1. Den verfügbaren Terminalpuffer lesen und bereits bekannte Inhalte überspringen. Neue Dateihinweise, Rückfragen und Ergebnisse herausarbeiten. TUI-Neuzeichnen, Spinner und Statuszähler sind keine neuen fachlichen Antworten. Auch Ablaufhinweise können unvollständig sein; bei Unklarheit einen Screenshot lesen.
2. Eine kleine Menge relevanter Dateien aus Auftrag, tatsächlichen Änderungen und CLI-Hinweisen beobachten. Bei neuen Hinweisen direkt deren Inhalt bzw. gezielten Diff lesen, auch vor Abschluss der CLI-Antwort. Den zuletzt gelesenen Inhalt flüchtig behalten und die neue Änderung dagegen vergleichen; der gesamte Diff gegen HEAD enthält auch ältere Arbeit.
3. Während aktiver Arbeit auch ohne neue Terminalmeldung in angemessenen Abständen erneut prüfen. Identischer Git-Status, identische Diff-Statistik oder unveränderte Dateigröße beweisen keinen gleichen Inhalt. Metadaten dürfen Leseprioritäten setzen, ersetzen aber keinen Inhaltsvergleich. Nicht jede Runde das ganze Repository hashen oder sämtliche Diffs ausgeben.
4. Gelegentlich einen kompakten Überblick über den gesamten bestätigten Worktree nehmen, nicht nur den erwarteten Auftragsbereich: neue/unverfolgte, gelöschte und umbenannte Dateien, staged/unstaged Änderungen und einen veränderten HEAD berücksichtigen. Dadurch die beobachtete Dateimenge erweitern. Neue Dateien müssen gesondert gelesen werden; normale Git-Diffs enthalten unverfolgte Inhalte nicht. Ignorierte relevante Ausgaben bei Bedarf gezielt prüfen.
5. Befunde als gelesenen Zwischenstand kennzeichnen. Bei erkennbar gleichzeitigem Schreiben erneut lesen; mehrere nacheinander gelesene Dateien bilden keinen garantierten atomaren Projektzustand. Konkrete Zielabweichungen früh nennen, normale unvollständige Umbauten nicht als fertigen Fehler darstellen. Nach Abschluss eine eigene Ergebnisprüfung durchführen.

Die Abstände nach Umfang, tatsächlicher Aktivität und Lesekosten wählen. Ein stiller Puffer ist kein Beweis für stillstehende Dateiänderungen. Eine schnelle Änderung mit sofortiger Rücknahme kann zwischen zwei Momentaufnahmen unbemerkt bleiben. Weder vollständige Ereigniserfassung noch feste Reaktionszeiten versprechen.

## Optional vorhandene strukturierte Sitzungslogs

Wenn ein lesbares Log der konkreten CLI bereits vorhanden ist, kann es gezieltere Hinweise als der ANSI-Puffer liefern. Erst Sitzung und Arbeitsordner zuordnen; niemals automatisch die neueste Logdatei als richtige Sitzung behandeln. Format und tatsächlich vorhandene Felder anhand der lokalen Quelle prüfen, keine modellübergreifende Struktur voraussetzen.

Nur neue vollständige Einträge ab einer flüchtig gemerkten Leseposition verarbeiten. Dateiaustausch, Kürzung und Rotation erkennen; bei unklarer Fortsetzung neu zuordnen. Keine vollständigen fremden Gesprächsverläufe einlesen, wenn wenige relevante Ereignisse genügen. Tool-Auftrag, Tool-Ergebnis und tatsächliche Dateiänderung unterscheiden. Ein angekündigter Schreibaufruf beweist keine erfolgreiche Änderung. Auch strukturierte Logs ersetzen die Datei- und Ergebnisprüfung nicht.

Fehlt ein eindeutig zugeordnetes Log, beim Puffer und Dateizugriff bleiben. Neue Hooks, dauerhafte Watcher oder ein Umbau der CLI sind keine Voraussetzung für diesen Ablauf und werden durch bloßes Mitlesen nicht eingerichtet.
