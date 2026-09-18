# Puffer sparsam lesen

Nur für einen beauftragten Dialog oder Prüflauf laden. `tmux_bridge.py read` liest das bestätigte Pane mit `capture-pane -p`, standardmäßig nur den aktuellen Bildschirm; History mit `--lines` gezielt ergänzen. tmux liefert seine gerenderte Zellansicht ohne angeforderte Farbsequenzen; der Helfer ist **kein ANSI-Parser und kein verlustloser Transkriptleser**.

## Kleine Auszüge vor der Modellausgabe

Der Helfer vergleicht den Hash der begrenzten Textsicht vor Ausgabe. Bei Gleichheit erscheinen nur `event: unchanged` und der Beobachtungstoken. Metadaten werden nur bei Änderung oder mit `--verbose` ausgegeben. `unchanged` bedeutet ausschließlich „dieser Textausschnitt ist gleich“. Cursor- und Modusdaten können trotzdem wechseln. Rohtext bleibt nur während des Aufrufs im Speicher; in der privaten State-Datei steht lediglich der Vergleichshash.

Bei Änderung liefert er eine begrenzte **Momentaufnahme**, kein behauptetes Delta. Standardlimit 6.000 Zeichen: bei Überschreitung Anfang und Ende mit ausdrücklich markiertem ausgelassenem Mittelteil. Für relevante Lücken einmal gezielt `--lines` (0–500) und `--max-chars` (1.000–30.000) anpassen; `--force-view` zeigt auch gleiche Inhalte wieder. Ein gezielter `capture-pane`-Ausschnitt innerhalb der bestätigten Bindung ist ebenfalls möglich. Fehlenden Antwortanfang oder eine gekürzte Rückfrage nicht erraten.

Keine festen Scrollback-Zeilennummern als dauerhaften Cursor verwenden: TUI-Redraw, alternative Bildschirme, Resize, Reflow und History-Limits verschieben sie. Keine globalen Zeilen-Sets zum Deduplizieren; identischer Text kann zu einem neuen Auftrag gehören. Spinneränderungen sind kein fachlicher Fortschritt, unbekannte Fehlerzeilen dürfen aber nicht mit pauschalen Filtern verschwinden. Inhaltlich nur relevante neue Aussagen bearbeiten, stets mit Auftragskennung und nötigem Kontext.

Den verfügbaren flüchtigen Gesprächszustand klein halten: gebundenes Ziel, aktueller Auftrag, letzter Transportstatus, offene Rückfrage, zuletzt geprüfte Dateien. Wiederkehrende Tool-Beschreibungen und bereits geklärte Details nicht erneut einlesen. Neue Rohpuffer nicht zusätzlich dauerhaft protokollieren. Weniger Modellkontext ist keine belegte prozentuale Kontingentersparnis.

## Aktiv und unterbrechbar mitlesen

Ein beauftragter Dialog oder eine begleitete Programmierung enthält einen aktiven Beobachtungszyklus. Nach Absenden weiter lesen, relevante Aussagen/Rückfragen verarbeiten und Dateien an Meilensteinen prüfen. Nach einer Antwort nicht sofort aufhören, solange der konkrete Dialogauftrag weiterläuft. Keine neuen Prompts in laufende Generierung geben; keine überlappenden Umsetzungen ohne ausdrücklichen Auftrag.

```sh
python3 "$BRIDGE" read --state "$BRIDGE_RUN/target.json" \
  --wait 3 --cancel-file "$BRIDGE_RUN/cancel-aktueller-aufruf"
```

`--wait` akzeptiert 0–10 Sekunden lokale Wartezeit. Bei Änderung oder Zustandswechsel kehrt der Aufruf zurück; andernfalls liefert er nach Ablauf `unchanged` und `waited_ms`. Lokal wird ungefähr alle 250 ms geprüft, einschließlich Identität. Es gibt keinen Hintergrundprozess und keine Fertigerkennung. Lokale Unterprozesse brauchen zusätzliche Laufzeit und besitzen eigene Timeouts; die Warteangabe ist keine Garantie einer millisekundengenauen Gesamtdauer.

Für den äußeren Werkzeugaufruf kurze Yield-Zeit verwenden (beispielsweise 1 Sekunde), sodass neue Nutzersteuerung vor dem Abschluss verarbeitet werden kann. Während ein `read --wait` läuft, keine zweite Zustellung starten. Bei Stopp die aktuelle private Cancel-Datei erzeugen oder den eindeutig zugehörigen eigenen Leseprozess abbrechen. Abbruch wird zwischen lokalen Prüfschritten erkannt, nicht während eines blockierenden Betriebssystemaufrufs. Pro Aufruf neuen Cancel-Dateinamen verwenden; keinen alten Abbruchmarker löschen und dann unbemerkt weitermachen.

Zunächst 2–3 Sekunden, bei wiederholt unverändertem Stand 5–10 Sekunden wählen. Nach relevanter Aktivität wieder kürzer reagieren. Spinner/Zähler können weiterhin Änderungen auslösen: keine pauschalen Filter, die Fehler/Rückfragen verschlucken. Nur die exakt erkannte numerische Launcher-Laufzeit im Footer wird beim Vergleich normalisiert; Rohansicht bleibt abrufbar. Dies ist kein allgemeiner semantischer Filter und keine garantierte Kontingentersparnis.

Zwischen begrenzten Aufrufen neue Nutzerbeiträge prüfen. „Stopp“ priorisieren; danach keine neuen Sends/Edits starten. Eine einmalige Statusfrage autorisiert keine Schleife. Ein aktiver Auftrag erzeugt keine geplante Dauerautomation und garantiert keine weitere Beobachtung nach Turn-/App-Ende. Bei fehlender sinnvoller nächster Handlung konkrete Sättigung oder Blockade melden statt leere Wiederholungsrunden zu erzeugen.

## Abschluss und Dateien

Stiller Puffer, gleicher Hash, Shell-Prompt oder ein leer wirkendes Eingabefeld beweisen keinen Abschluss. `C17 fertig` zählt nur als neue Claude-Antwort zum aktuellen Auftrag; das Echo der Aufforderung oder ein alter Marker zählt nicht. Ein eindeutiger Selbstbericht kann den Dialog beenden, belegt aber nicht die Codekorrektheit.

Bei beauftragter Programmierbegleitung zu Dateimeilensteinen echte Dateien und Diffs im bestätigten Worktree prüfen. Bei laufendem Umbau Zwischenstand markieren; keine halbfertige Refaktorierung vorschnell korrigieren. Normale Git-Diffs enthalten keine unverfolgten Dateien. Inhalte gegen zuletzt gelesenen Stand vergleichen und fremde Voränderungen schützen. Ein stilles Terminal bedeutet nicht unveränderte Dateien.

Optional ein **bereits eindeutig derselben Sitzung zugeordnetes** strukturiertes Log für eine konkrete Textlücke verwenden. Niemals pauschal das neueste Claude-Log wählen. Format, Rotation und Kürzung beachten; keine neuen Hooks oder Logger installieren. Tool-Auftrag, Tool-Ergebnis und beobachtete Dateiänderung bleiben unterschiedliche Belege. Fehlt der Beleg, die Unsicherheit benennen.
