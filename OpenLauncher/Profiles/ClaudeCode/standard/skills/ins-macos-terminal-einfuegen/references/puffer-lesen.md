# Puffer sparsam lesen

Nur für einen beauftragten Dialog oder Prüflauf laden. `tmux_bridge.py read` liest das bestätigte Pane mit `capture-pane -p`, standardmäßig aktuellen Bildschirm und höchstens 80 vorherige Zeilen. tmux liefert seine gerenderte Zellansicht ohne angeforderte Farbsequenzen; der Helfer ist **kein ANSI-Parser und kein verlustloser Transkriptleser**.

## Kleine Auszüge vor der Modellausgabe

Der Helfer vergleicht den Hash der begrenzten Textsicht vor Ausgabe. Bei Gleichheit erscheinen nur kurze Zustandsdaten, kein wiederholter Puffer. `changed=false` bedeutet ausschließlich „dieser Textausschnitt ist gleich“. Cursor- und Modusdaten können trotzdem wechseln. Rohtext bleibt nur während des Aufrufs im Speicher; in der privaten State-Datei steht lediglich der Vergleichshash.

Bei Änderung liefert er eine begrenzte **Momentaufnahme**, kein behauptetes Delta. Standardlimit 9.000 Zeichen: bei Überschreitung Anfang und Ende mit ausdrücklich markiertem ausgelassenem Mittelteil. Für relevante Lücken einmal gezielt `--lines` (0–500) und `--max-chars` (1.000–30.000) anpassen; `--force-view` zeigt auch gleiche Inhalte wieder. Ein gezielter `capture-pane`-Ausschnitt innerhalb der bestätigten Bindung ist ebenfalls möglich. Fehlenden Antwortanfang oder eine gekürzte Rückfrage nicht erraten.

Keine festen Scrollback-Zeilennummern als dauerhaften Cursor verwenden: TUI-Redraw, alternative Bildschirme, Resize, Reflow und History-Limits verschieben sie. Keine globalen Zeilen-Sets zum Deduplizieren; identischer Text kann zu einem neuen Auftrag gehören. Spinneränderungen sind kein fachlicher Fortschritt, unbekannte Fehlerzeilen dürfen aber nicht mit pauschalen Filtern verschwinden. Inhaltlich nur relevante neue Aussagen bearbeiten, stets mit Auftragskennung und nötigem Kontext.

Den verfügbaren flüchtigen Gesprächszustand klein halten: gebundenes Ziel, aktueller Auftrag, letzter Transportstatus, offene Rückfrage, zuletzt geprüfte Dateien. Wiederkehrende Tool-Beschreibungen und bereits geklärte Details nicht erneut einlesen. Neue Rohpuffer nicht zusätzlich dauerhaft protokollieren. Weniger Modellkontext ist keine belegte prozentuale Kontingentersparnis.

## Unterbrechbar warten

Nach erwarteter kurzer Antwort etwa 3–5 Sekunden warten, bei unverändertem Stand auf 10–15 Sekunden, bei längeren Builds bis etwa 30 Sekunden verlängern. Bei relevanter Aktivität wieder kürzer reagieren. Dies sind Startwerte, keine gemessenen Optima. Ist eine Antwort bereits sichtbar, sofort bearbeiten.

`clock.sleep` oder einen entsprechend unterbrechbaren Werkzeugaufruf zwischen einzelnen Abrufen nutzen, keine endlose Schleife in einer Tool-Zelle und kein Shell-`sleep` als Dauerüberwachung. Nach jedem Rücksprung neue Nutzerbeiträge beachten. Eine einmalige Statusfrage autorisiert keine fortlaufende Beobachtung. Bei längerer Unklarheit konkret den fehlenden Beleg nennen und keine Endlosschleife starten. Kein geplanter Hintergrundjob ohne ausdrücklichen Nutzerauftrag.

## Abschluss und Dateien

Stiller Puffer, gleicher Hash, Shell-Prompt oder ein leer wirkendes Eingabefeld beweisen keinen Abschluss. `C17 fertig` zählt nur als neue Claude-Antwort zum aktuellen Auftrag; das Echo der Aufforderung oder ein alter Marker zählt nicht. Ein eindeutiger Selbstbericht kann den Dialog beenden, belegt aber nicht die Codekorrektheit.

Bei beauftragter Programmierbegleitung zu Dateimeilensteinen echte Dateien und Diffs im bestätigten Worktree prüfen. Bei laufendem Umbau Zwischenstand markieren; keine halbfertige Refaktorierung vorschnell korrigieren. Normale Git-Diffs enthalten keine unverfolgten Dateien. Inhalte gegen zuletzt gelesenen Stand vergleichen und fremde Voränderungen schützen. Ein stilles Terminal bedeutet nicht unveränderte Dateien.

Optional ein **bereits eindeutig derselben Sitzung zugeordnetes** strukturiertes Log für eine konkrete Textlücke verwenden. Niemals pauschal das neueste Claude-Log wählen. Format, Rotation und Kürzung beachten; keine neuen Hooks oder Logger installieren. Tool-Auftrag, Tool-Ergebnis und beobachtete Dateiänderung bleiben unterschiedliche Belege. Fehlt der Beleg, die Unsicherheit benennen.
