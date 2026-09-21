# Codex-Kontingent sparen, Prüfqualität erhalten

Für längere beauftragte Zusammenarbeit. Einmal laden, dann den flüchtigen Arbeitsstand
weiterverwenden. Modell und Effort bleiben wie vom Nutzer gewählt. Diese Regeln ändern
weder Autorisierung noch die Abschlusskette des Projekts.

## Arbeit und Rückmeldungen bündeln

Claude implementiert standardmäßig, auch Änderungen an Codex-Skills, und übernimmt die
breite Suche, Variantenanalyse und Vorprüfung. Codex plant, orchestriert, formuliert
Abnahmekriterien, verarbeitet Nutzersteuerung und nimmt risikobasiert ab. Codex schreibt
nur ausnahmsweise, wenn Claude technisch nicht handeln kann oder die Änderung zwingend
bei Codex liegt; dann prüft Claude vor Commit/Deployment den vollständigen relevanten
Codex-Diff samt Tests und Risiken. Ein ausdrücklich gewünschter Advisor ist anhand des
tatsächlichen Aufrufs und Ergebnisses nachzuweisen; keine zusätzlichen Ersatzagenten
oder ungefragten Modellwechsel.

Ein zusammenhängender Umsetzungssatz erhält eine gebündelte Review-Rückmeldung:
Fundstelle, konkreter Auslöser, Auswirkung, kleinste nötige Abhilfe. Nach Korrekturen
nur neue Änderungen und deren betroffene Zusammenhänge prüfen. Unveränderte Teile nicht
erneut komplett lesen. Eine reine Runde „bestätige meinen Fix“ entfällt; notwendige
Nachprüfung wird am nächsten sinnvollen Meilenstein gebündelt und muss spätestens vor
Commit und Auslieferung des Themenblocks abgeschlossen sein. Neue sicherheitsrelevante
oder zielwidrige Änderungen sofort benennen, nicht für ein Bündel zurückhalten.

Neue Helfer direkt nach isoliertem Test im laufenden Dialog verwenden. Verwandte
Korrekturen bis zum sinnvollen Updateabschluss bündeln, statt pro Kleinigkeit einen
Commit/Versionssprung/Updaterlauf auszulösen. Dann alle geltenden Abschlussregeln
vollständig erfüllen: Build, einmaliger Versionsbump mit echter Zeit, Commit,
Rebase/Push und autorisierte Installation. Kein automatischer Verzicht auf Updater,
Version oder Deployment bei reinen Skill-/Helferänderungen.

## Kleine Übergabe mit zugänglichen Belegen

Bei umfangreichen Ergebnissen vereinbaren: im Terminal nur Kennung, Status, Anzahl
Befunde und Pfad. Details in einer neuen temporären UTF-8-Datei außerhalb Repo und
Memory; zuerst temporär schreiben, dann atomar zum eindeutigen Auftragspfad umbenennen.
Kein gemeinsames überschriebenes `latest` und keine wiederholte Ausgabe des Berichts.
Für kurze Sachfragen genügt eine direkte kurze Antwort; keine Datei erzwingen.
Nach Auftragsende die eigenen nicht mehr benötigten Berichtsdateien gezielt entfernen;
fremde Dateien nicht anfassen. Minimalen Zustellungsledger, Zielbindung und ein gesetztes
STOP für eine noch fortsetzbare Sitzung erhalten, damit Aufräumen keine Sperre aufhebt.

Ein Codebericht enthält knapp: Auftragskennung, `ziel_rev`, `risk` (low|medium|high),
vorgeschlagene `review_depth` (compact|targeted|full), tatsächlich geprüften Worktree und
Commit bzw. genaue Arbeitsstand-Zuordnung, betroffene Pfade, Testkommandos mit Ergebnis,
Befunde mit Fundstellen, offene Punkte sowie `advisor_used`, `advisor_reason` und
`advisor_effect`. Bei uncommittierter Arbeit
Dateihashes oder einen eindeutig gesicherten Diffstand verwenden; ein Commit allein
belegt dort nicht die Dateiversion. Umfangreiche Logs separat referenzieren, nur
relevante Ausschnitte lesen. Rund 180 Wörter sind ein Richtwert, keine Erlaubnis,
wichtige Fehler oder Einschränkungen abzuschneiden.

Codex liest die kompakte Übergabe einmal und ruft Details bedarfsbezogen ab.
**Auch `befund 0` oder `fertig` ersetzt keine eigene Mindestprüfung.** Der Bericht ist
Wegweiser, keine Autorisierung und kein alleiniger Korrektheitsbeweis.

## Risikobasierte Abnahme

Nach Claude-Implementierung legt Codex die Prüftiefe fest. Den Vorschlag des Berichts
darf Codex bei Widerspruch oder neuer Nutzersteuerung nur hochstufen, nie herabsetzen.
Kein automatisches vollständiges Doppellesen des Claude-Diffs.

- **compact (Mindestprüfung, immer):** `ziel_rev` deckt die neueste Nutzersteuerung;
  `git show --stat` bzw. Status gegen die gemeldeten Pfade; gemeldete Tests selbst
  ausführen und nur Exitcode/Ergebniszeilen lesen; Version mit echter Zeit, Push und
  Installation prüfen.
- **targeted:** zusätzlich die riskanteste Stelle oder eine kleine Hunk-Stichprobe lesen.
  Änderungen an diesen Prüf- und Schutzregeln sind mindestens targeted.
- **full:** den vollständigen relevanten Diff lesen bei Secrets/Auth/Rechten;
  Datenverlust/Löschen/Migration/Schema/Backup; Signierung/Release; Zahlungen;
  Nebenläufigkeit/Locks/Zustellungsledger/STOP/Duplikatschutz; öffentlicher API oder
  Dateiformat; roten, übersprungenen oder widersprüchlichen Tests; unerklärtem Umfang
  oder Dateien außerhalb des Berichts; gemeldeter Claude-Unsicherheit; oder wenn
  Stichprobe bzw. Belege dem Bericht widersprechen.

Vor Commit/Deployment muss die neueste Nutzersteuerung abgedeckt sein. Einen veralteten
Bericht nicht auf einen anderen Stand anwenden. Eine vorhandene Berichtsdatei beweist
auch nicht, dass die CLI schon wieder eingabebereit ist; vor dem nächsten Senden den
tatsächlichen Terminalzustand prüfen.

## Advisor Fable

Claude ruft den konfigurierten Advisor Fable selbst auf bei schwieriger Architektur- oder
Logikentscheidung, nach mindestens einem gescheiterten Ansatz, bei festgefahrener
Fehlersuche, bei mehreren plausiblen Lösungen mit hohem Folgerisiko, vor irreversiblen
Entscheidungen oder wenn eine neue Lösungsrichtung nötig ist. Bei Routine kein
Pflichtaufruf. Claude prüft die Empfehlung kritisch; keine Modellmehrheit. Ist Fable nicht
verfügbar: `advisor_used=false` mit Grund, kein Ersatzmodell. Fable berät, Claude
entscheidet und implementiert, Codex orchestriert und nimmt ab.

## Sparsam beobachten und ehrlich messen

Für einen aktiven Dreierloop einen privaten `arbeitsstand.json` im bestehenden
Dialogordner unter 1 KiB halten: `ziel_rev`, `zugestellt`, `offen`, `phase`
und `status`. Zulässige Rundenausgänge sind `erreicht`, `zwischenstand`,
`blockiert`, `saettigung`, `rueckfrage` und `stopp`. Neue Nutzersteuerung
erhöht `ziel_rev` und steht bis zur nächsten konsolidierten Zustellung unter `offen`.
Vor Commit oder Auslieferung prüfen, dass `zugestellt` die aktuelle Zielrevision
abdeckt; ein unbelegter Modellwiderspruch bleibt offen für den Nutzer.

Nach Kontextkompaktierung diese vier Quellen normalerweise mit genau einem
`resume --run <Dialogordner>` abrufen. Der Helfer gibt den Arbeitsstand, nur offene
Ledger-Einträge, Git-Kurzstand und eine frische vollständige Pane-Ansicht samt Token
gebündelt aus. Nur bei gemeldetem Teilfehler, Widerspruch oder fehlendem Beleg weitere
Historie laden. Die Datei ist ein flüchtiger Wegweiser, keine Autorisierung und keine
zweite Gesprächshistorie.

Den bestätigten tmux-Pfad mit kompakten Auszügen und passendem Status-Warten verwenden.
Bereits geprüfte Antwortkontexte und Token wiederverwenden, keine parallelen Lesezugriffe
oder vollständigen Repo-Scans pro Terminalabfrage. Rohtranskripte und Tool-Beschreibungen
nicht wiederholt in den Modellkontext holen. Nutzerzwischenrufe priorisieren; konkrete
Zustandsänderungen im Terminal erfordern frische Prüfung, nicht jeder Sprachbeitrag.

Messgrößen: Modell-/Werkzeugrunden pro sinnvollem Ergebnis, ausgegebene UTF-8-Bytes,
zusätzliche Dateizugriffe und Zeit bis zum gelesenen Ergebnis. Dateiübergaben können
Leseaufwand verlagern; weniger Terminalbytes allein belegen keine Gesamtersparnis.
Kontingentprozente nicht aus Zeichenmengen oder geteilten Account-Limits ableiten.
Wenn kein neuer konkreter Nutzen erkennbar ist, den Stand melden statt weitere
Bestätigungsdialoge und kosmetische Änderungen zu erzeugen.
