# Puffer sparsam lesen

Nur für einen beauftragten Dialog oder Prüflauf laden. `tmux_bridge.py read` liest das bestätigte Pane mit `capture-pane -p`, standardmäßig nur den aktuellen Bildschirm; History mit `--lines` gezielt ergänzen. tmux liefert seine gerenderte Zellansicht ohne angeforderte Farbsequenzen; der Helfer ist **kein ANSI-Parser und kein verlustloser Transkriptleser**.

## Kleine Auszüge vor der Modellausgabe

`read --compact` liefert nach der ersten vollständigen Ansicht einen **positionsgebundenen
Ersetzungsauszug**: `edits` enthält geordnete Ersetzungen. `old_rows` ist das halboffene,
nullbasierte Zeilenintervall der vorigen Ansicht, `new_rows` das der neuen Ansicht;
`text` ersetzt dieses Intervall. Alle alten Indizes beziehen sich auf dieselbe Basis
(beim Rekonstruieren von hinten anwenden). Ein leeres `text` mit nichtleerem `new_rows`
steht für eine Leerzeile, ein leeres neues Intervall für Löschung. `base_view` und
`view_hash` ordnen die Ansichten zu. Auch gescrollte gleiche Blöcke werden erkannt. Wiederholte Zeilen
werden niemals als globale Menge entfernt; auch Löschungen erscheinen als Ersetzung.
Ist die strukturierte Ausgabe größer als die Vollansicht, wird die Vollansicht geliefert.
Es handelt sich weder um ein vollständiges Transkript noch um eine neue Antwort allein.
Bei Reflow kann der Auszug groß sein. Unbekannte Fehler und Rückfragen werden nicht
herausgefiltert. Footer bleiben sichtbar, wenn sie sich ändern.

Gespeichert werden ausschließlich begrenzte Zeilenhashes im privaten Dialogzustand.
Nach gekürzter Ausgabe oder geänderter History-Tiefe kommt zuerst eine Vollansicht.
`--force-view` liefert unabhängig vom Kompaktmodus die ganze begrenzte Momentaufnahme.
Nach Kontextverlust oder unklarer Basis diese Vollansicht lesen. Der unveränderte
Beobachtungstoken für Eingaben bezieht sich weiterhin auf die gesamte aktuelle Ansicht,
nicht allein auf den Auszug. Keine Sicherheitsentscheidung allein aus fehlenden Zeilen.

Der Helfer vergleicht den Hash der begrenzten Textsicht vor Ausgabe. Bei Gleichheit erscheinen nur `event: unchanged` und der Beobachtungstoken. Metadaten werden nur bei Änderung oder mit `--verbose` ausgegeben. `unchanged` bedeutet ausschließlich „dieser Textausschnitt ist gleich“. Cursor- und Modusdaten können trotzdem wechseln. Rohtext bleibt nur während des Aufrufs im Speicher; in der privaten State-Datei steht lediglich der Vergleichshash.

Bei Änderung liefert er eine begrenzte **Momentaufnahme**, kein behauptetes Delta. Standardlimit 6.000 Zeichen: bei Überschreitung Anfang und Ende mit ausdrücklich markiertem ausgelassenem Mittelteil. Für relevante Lücken einmal gezielt `--lines` (0–500) und `--max-chars` (1.000–30.000) anpassen; `--force-view` zeigt auch gleiche Inhalte wieder. Ein gezielter `capture-pane`-Ausschnitt innerhalb der bestätigten Bindung ist ebenfalls möglich. Fehlenden Antwortanfang oder eine gekürzte Rückfrage nicht erraten.

Keine festen Scrollback-Zeilennummern als dauerhaften Cursor verwenden: TUI-Redraw, alternative Bildschirme, Resize, Reflow und History-Limits verschieben sie. Keine globalen Zeilen-Sets zum Deduplizieren; identischer Text kann zu einem neuen Auftrag gehören. Spinneränderungen sind kein fachlicher Fortschritt, unbekannte Fehlerzeilen dürfen aber nicht mit pauschalen Filtern verschwinden. Inhaltlich nur relevante neue Aussagen bearbeiten, stets mit Auftragskennung und nötigem Kontext.

Den verfügbaren flüchtigen Gesprächszustand klein halten: gebundenes Ziel, aktueller Auftrag, letzter Transportstatus, offene Rückfrage, zuletzt geprüfte Dateien. Wiederkehrende Tool-Beschreibungen und bereits geklärte Details nicht erneut einlesen. Neue Rohpuffer nicht zusätzlich dauerhaft protokollieren. Weniger Modellkontext ist keine belegte prozentuale Kontingentersparnis.

## Aktiv und unterbrechbar mitlesen

Ein beauftragter Dialog oder eine begleitete Programmierung enthält einen aktiven Beobachtungszyklus. Nach Absenden weiter lesen, relevante Aussagen/Rückfragen verarbeiten und Dateien an Meilensteinen prüfen. Nach einer Antwort nicht sofort aufhören, solange der konkrete Dialogauftrag weiterläuft. Keine neuen Prompts in laufende Generierung geben; keine überlappenden Umsetzungen ohne ausdrücklichen Auftrag.

```sh
python3 "$BRIDGE" read --run "$BRIDGE_RUN" \
  --wait 3 --cancel-file "$BRIDGE_RUN/cancel-aktueller-aufruf"
```

`--wait` akzeptiert 0–10 Sekunden lokale Wartezeit. Bei Änderung oder Zustandswechsel kehrt der Aufruf zurück; andernfalls liefert er nach Ablauf `unchanged` und `waited_ms`. Lokal wird ungefähr alle 250 ms geprüft, einschließlich Identität. Es gibt keinen Hintergrundprozess und keine Fertigerkennung. Lokale Unterprozesse brauchen zusätzliche Laufzeit und besitzen eigene Timeouts; die Warteangabe ist keine Garantie einer millisekundengenauen Gesamtdauer.

Nach live bestätigtem Titelverhalten der konkreten Sitzung kann während einer laufenden
Arbeit `--wait-mode status --wait 10 --compact` Modellrunden bündeln. Früh geweckt wird
bei geändertem Pane-Titel, Eingabefeld oder Scroll-/Eingabemodus sowie fehlendem Rahmen.
Die zwei live beobachteten Titelanimationen `◐`/`◑` gelten dabei als derselbe Zustand;
Originaltitel und Schreibtoken bleiben unverändert.
Bei anderen Titeln wecken zusätzlich Inhalts-/Metadatenänderungen, damit eine bereits
vor Beginn des Aufrufs fertig gewordene Antwort nicht unnötig wartet. Die aktuelle Ansicht kommt
spätestens nach der begrenzten Wartezeit plus Werkzeuglaufzeit. `wake_reason` beschreibt
nur den Auslöser, niemals Annahme, Bereitschaft oder Fertigstellung. Bei neuem/unklarem
Titelverhalten den Standardmodus `activity` verwenden. Auch der Statusmodus ist eine
Momentaufnahme, kein verlustloses Log: kurz eingeblendete und wieder verschwundene
Meldungen sind nicht garantiert enthalten. Relevante Lücken gezielt nachlesen und
Codeergebnisse unabhängig prüfen. Keine Fehlerzeilen pauschal filtern.

Neue Helferänderungen nach isolierten Tests bereits im laufenden autorisierten Dialog
verwenden und dort belegen, bevor eine Wirkung für künftige Sitzungen behauptet wird.
Geprüfte Pfade und Aufrufvorlagen im flüchtigen Sitzungskontext wiederverwenden;
keine wiederholte Tool-/Skill-Erkundung pro Nachricht. Kürzere Aufrufe sparen Kontext,
ersetzen aber weder frische Beobachtung noch Stopppunkte.

Für den äußeren Werkzeugaufruf kurze Yield-Zeit verwenden (beispielsweise 1 Sekunde), sodass neue Nutzersteuerung vor dem Abschluss verarbeitet werden kann. Während ein `read --wait` läuft, keine zweite Zustellung starten. Bei Stopp `STOP` im selben Dialogordner erzeugen; zusätzlich kann die aktuelle private Cancel-Datei gesetzt oder der eindeutig zugehörige eigene Leseprozess abgebrochen werden. `STOP` bleibt auch für nachfolgende Aufrufe wirksam, bis der Nutzer ausdrücklich fortsetzen lässt. Abbruch wird zwischen lokalen Prüfschritten erkannt, nicht während eines blockierenden Betriebssystemaufrufs. Pro Aufruf neuen Cancel-Dateinamen verwenden; keinen alten Abbruchmarker löschen und dann unbemerkt weitermachen.

Zunächst 2–3 Sekunden, bei wiederholt unverändertem Stand 5–10 Sekunden wählen. Nach relevanter Aktivität wieder kürzer reagieren. Spinner/Zähler können weiterhin Änderungen auslösen: keine pauschalen Filter, die Fehler/Rückfragen verschlucken. Nur die exakt erkannte Launcher-Laufzeit sowie Uhrzeit und verbleibenden Resetzeiten in der vollständigen ctx-Fußzeile werden beim Vergleich normalisiert; Rohansicht bleibt abrufbar. Prozentwerte, Modell, Effort, Tempoanzeige und Antworttext bleiben unverändert geprüft. Dies ist kein allgemeiner semantischer Filter und keine garantierte Kontingentersparnis.

Zwischen begrenzten Aufrufen neue Nutzerbeiträge prüfen. „Stopp“ priorisieren; danach keine neuen Sends/Edits starten. Eine einmalige Statusfrage autorisiert keine Schleife. Ein aktiver Auftrag erzeugt keine geplante Dauerautomation und garantiert keine weitere Beobachtung nach Turn-/App-Ende. Bei fehlender sinnvoller nächster Handlung konkrete Sättigung oder Blockade melden statt leere Wiederholungsrunden zu erzeugen.

## Abschluss und Dateien

Ein Review-Halt ist ein Zwischenstand mit noch folgenden Commit-/Push-Schritten, kein `fertig`. Führe die Begleitung nach dem Review ohne erneute Nutzeraufforderung bis zur anwendbaren Abschlusskette aus SKILL.md fort: Build und Versionsartefakt, Commit, Rebase/Push und autorisierte Installation konkret belegen. Zurückgestellte oder fehlgeschlagene Schritte ausdrücklich nennen; bei ausstehender Installation die APK bereitstellen. Ein Build allein beendet kein beauftragtes Update.

Stiller Puffer, gleicher Hash, Shell-Prompt oder ein leer wirkendes Eingabefeld beweisen keinen Abschluss. `C17 fertig` zählt nur als neue Claude-Antwort zum aktuellen Auftrag; das Echo der Aufforderung oder ein alter Marker zählt nicht. Ein eindeutiger Selbstbericht kann den Dialog beenden, belegt aber nicht die Codekorrektheit.

Bei beauftragter Programmierbegleitung zu Dateimeilensteinen echte Dateien und Diffs im bestätigten Worktree prüfen. Bei laufendem Umbau Zwischenstand markieren; keine halbfertige Refaktorierung vorschnell korrigieren. Normale Git-Diffs enthalten keine unverfolgten Dateien. Inhalte gegen zuletzt gelesenen Stand vergleichen und fremde Voränderungen schützen. Ein stilles Terminal bedeutet nicht unveränderte Dateien.

Optional ein **bereits eindeutig derselben Sitzung zugeordnetes** strukturiertes Log für eine konkrete Textlücke verwenden. Niemals pauschal das neueste Claude-Log wählen. Format, Rotation und Kürzung beachten; keine neuen Hooks oder Logger installieren. Tool-Auftrag, Tool-Ergebnis und beobachtete Dateiänderung bleiben unterschiedliche Belege. Fehlt der Beleg, die Unsicherheit benennen.
