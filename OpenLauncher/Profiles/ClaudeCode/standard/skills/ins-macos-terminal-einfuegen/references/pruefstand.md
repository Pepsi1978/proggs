# Prüfstand der ersten nutzbaren Fassung

Skill-Fassung 1.0.0, erstellt am 18.09.2026, 11:57 Uhr (Systemzeit per PowerShell gelesen). Diese Versionsangabe gehört zum Skill, nicht zur OpenLauncher-App.

## Lokale technische Prüfung

- Python-Syntax, YAML-Frontmatter, Codex-Metadaten und lokale Referenzlinks geprüft.
- Helfer auf einem eigens erzeugten temporären tmux-Server mit einem lokalen Python-Empfänger ausgeführt. Kein Claude-Modellaufruf und keine Eingaben in die bestehenden Nutzersitzungen.
- Zielbindung, lesende Momentaufnahme und Unterdrückung gleicher Textauszüge erfolgreich.
- UTF-8, Anführungszeichen, Apostroph, Shell-Metazeichen, LF und Tab unverändert übertragen; Bracketed-Paste-Klammern und separates Enter bytegenau beim Empfänger beobachtet.
- Falscher Beobachtungstoken, wiederholtes Einfügen, wiederholtes Enter und ausgetauschte Prozessidentität zurückgewiesen.
- Temporären Testserver danach beendet und Testdateien entfernt. Keine Änderungen an tmux-Konfiguration oder Nutzer-Sitzungen.

Die vorher im Nutzerdialog nachgewiesene Verbindung zu Claude in Codex-Terminal und Terminal.app belegt den grundlegenden Schreibweg. Der lokale Helfertest belegt den Transport, **nicht** automatische Erkennung von Ghost-Suggestions, Eingabebereitschaft oder Codekorrektheit. Kein neuer kostenpflichtiger Claude-End-to-End-Test für diese Skill-Fassung.

## Drei kurze Anwendungsszenarien

1. „Gib Claude diesen Auftrag … warte, ich ergänze noch … und schick ihn ab.“ Erst den vollständigen Beitrag mit letzter Korrektur zusammenführen, dann genau eine Kennung und eine Übergabe; keine zusätzliche Bestätigungsrunde.
2. „Füge den mehrzeiligen Text nur ein.“ Ziel bestätigen, freien Claude-Eingabebereich prüfen, Paste ausführen und lesen; kein Enter. Bei echtem fremdem Entwurf oder unklarer Ghost-Suggestion nichts schreiben.
3. „Begleite die Umsetzung.“ Adaptive, unterbrechbare Momentaufnahmen und gezielte reale Dateimeilensteine lesen. Unveränderter Hash ist kein Abschluss; neuer Marker ist ein Selbstbericht. Timeout führt zu Lesen, nicht Wiederholung; „Stopp“ beendet die Begleitung.

Dies sind dokumentierte Sollabläufe, keine behaupteten Modell-Benchmarks oder gemessenen Einsparquoten.

## Fassung 1.1.0 — 18.09.2026, 12:16 Uhr

Drei substanzielle sichtbare Diskussionsrunden mit der bereits bestätigten Claude-Sitzung: Architektur/Aufwandsquellen, Risiken eines schnellen Submit samt Alternativen, konkrete Guard-/Cursor- und Korrelationsgrenzen. Claude änderte dabei keine Dateien. Ausgewählt: Versuchstatus direkt vor Mutation, zusammenhängendes geprüftes Submit, kompakte Zustandsausgabe, Bildschirm als Standardausschnitt, eng verankerte Laufzeitnormalisierung und abbrechbares begrenztes Mitlesen. Keine breite Spinnerfilterung, keine Ruhe-Fertigerkennung, kein sofortiger Snapshot-Diff.

Kontrollierter Vergleich gegen den Helfer aus Commit `d0b0352a8`, auf demselben isolierten lokalen tmux-Testpane:

| Messgröße | Ausgangsfassung | Fassung 1.1.0 |
|---|---:|---:|
| Sechs identische Leseabfragen: erste Ansicht + fünf Wiederholungen, gesamte UTF-8-JSON-Ausgabe | 3.642 Byte | 1.700 Byte |
| Modellseitige Werkzeugaufrufe für Lesen → vollständige autorisierte Übergabe, Bindung bereits vorhanden | 4: read/paste/read/enter | 2: read/submit |

Zusätzliche lokale Beobachtungen: `read --wait 1` lieferte nach 1,123 Sekunden 122 Byte; privater Cancel-Marker nach 0,25 Sekunden wurde bis 0,334 Sekunden erkannt. Vollständiger Test-Submit einschließlich Helferstart dauerte 0,204 Sekunden. Zwei vorherige echte Diskussionsübergaben mit derselben lokalen Prüffolge dauerten 0,331 beziehungsweise 0,332 Sekunden Paste-bis-Enter. Diese Zeitwerte sind Einzelbeobachtungen, keine Latenzgarantie.

Bytegenauer Mehrzeiler-/Sonderzeichen-Transport, separates internes Enter, veraltete Tokens ohne verbrauchte Kennung, Duplikat-/Identitäts-/Fremdentwurfschutz und abbrechbares Warten wurden lokal geprüft. Die eng verankerte Normalisierung erhält gleichlautenden Antworttext sowie Preisänderungen. Reproduzierbarer Smokecheck: `python3 <Skillordner>/tests/check_bridge.py`; er nutzt ausschließlich einen eigenen temporären tmux-Server und keinen Modellaufruf. Messwerte schwanken mit Version, Fensterinhalt und Host; JSON-Bytes sind weder Modelltoken noch Konto-Kontingent.

## Fassung 1.1.1 — 18.09.2026, 12:20 Uhr

Nutzerwunsch und Diskussionsrunde O4 dauerhaft übernommen: verständliche deutsche Phasenberichte, belegter aktueller Stand, eigene Arbeit gegenüber Arbeit anderer Aufgaben und passivem Warten unterscheiden, kurze Berichte an den autorisierten Sprach-/Ursprungskanal, keine erfundene Tätigkeit und kein Versprechen, technische Warteanzeigen zu steuern.

Korrelationsgrenze konkretisiert: ein fachlich aktiver Umsetzungsauftrag, Zwischenfragen lokal beantworten. Claudes Vorschlag eines Pasteblock-Ankers wurde anhand der tatsächlichen Darstellung verworfen: Die Blocknummer verschwand nach Enter zugunsten des ausgeschriebenen Auftrags. Keine automatische Abtrennung hinter der letzten Kennungsfundstelle. Kein zusätzlicher Ack-Modellaufruf eingeführt.

Der neue `submit`-Helfer hat die echte Diskussionsnachricht O5 in derselben bestätigten Claude-Sitzung als `enter_sent` übergeben; gemessene Helferlaufzeit 0,206 Sekunden. Die Antwort steht zum Zeitpunkt dieses Dokumentationsschritts noch aus. Diese Fassung ändert nur Anweisungen; keine erneuten Transporttests erforderlich.
