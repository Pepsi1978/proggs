# Sehr ausführliches Review AQTU6: Vorlese-Lautstärken

## Umfang

Geprüft wurden die Änderungen aus `4ef4159b6` (1.8.545) und ihre Korrekturen: gemeinsamer Benachrichtigungsbildschirm, Profile und alte Einstellungsschlüssel, Einstellungskatalog, Ansage mit Vorleser/Einschüben, Erinnerungen mit Datei- und Systemstimme, Installationsfrage und Timer sowie Funktionsdatei und Versionslog. Keine unabhängigen Änderungen anderer Sitzungen sind Teil dieses Reviews.

## Befunde und Korrekturen

| Nr. | Priorität | Befund und nachvollziehbarer Fehlerpfad | Korrektur |
|---|---|---|---|
| 1 | P2 | Bei „Wie Handy“ steht der Regler intern auf 20. Material Slider meldet am bereits angezeigten Endpunkt keine Wertänderung; damit lässt sich 20 nicht unmittelbar als eigener Wert wählen. | Explizite Auswahl „Eigene Lautstärke“; erst dann ist der Prozentregler aktiv. |
| 2 | P3 | Ein früher per Sprache gespeicherter Wert unter 20 bleibt korrekt gespeichert, während der Zeiger wegen des neuen Bereichs bei 20 steht. Ohne Erklärung widersprechen sich Anzeige und Zeiger. | Tatsächlicher Prozentwert und zusätzlicher Bestandsschutz-Hinweis; keine automatische Änderung alter Werte. |
| 3 | P2 | Der Katalog beschreibt „Wie Handy“, sein allgemeiner Zahlenparser lehnt den wörtlichen Wert aber ab und liest 0 als „0 Prozent“. | Spezieller Vorlese-Lautstärke-Eintrag nimmt „Wie Handy“ und die bisher gültigen Zahlen an und liest 0 als „Wie Handy“. Alle Meldungsarten einschließlich alter Ansage-/Abfahrtschlüssel verwenden ihn. |
| 4 | P1 | Ein einzelner Aktivzähler stellt nach einem kurzen Einschub nicht die Lautstärke der längeren Ansage wieder her. Ein nicht registrierter Lauf mit 0 kann außerdem eine vorübergehend gesetzte fremde Lautstärke übernehmen oder während seines Vorlesens zurückgestellt werden. | Identitätsgebundene Wahlen je Tonweg, geordnete Rückstellung auch bei vertauschtem Ende, Beteiligung von „Wie Handy“ und Erhalt manueller Änderungen. |
| 5 | P2 | Das Lesen von maximaler/aktueller Stream-Lautstärke liegt vor dem geschützten Sprechblock. Wirft Android dort eine Ausnahme, bleibt insbesondere die direkt aufrufende Erinnerung stumm. | Geschützte Ermittlung mit protokolliertem Rückfall aufs Vorlesen ohne Lautstärkeänderung; Geräte-Minimalpegel wird berücksichtigt. |
| 6 | P1 | `warteAufEnde` kann nach seiner Frist null liefern, während die eigene Vorleser-Quelle weiterläuft. Der Lautstärkeschutz endet dann zu früh. | Nach Fristablauf nur die eigene, tatsächlich noch aktive Quelle stoppen, bevor der Tonweg zurückgestellt wird. |
| 7 | P1 | `einschub` startet seinen Player in einem getrennten Coroutine-Bereich. `cancel()` allein wartet dessen finally nicht ab; der Aufrufer kann die Lautstärke schon zurückstellen, bevor der Einschub verstummt. | Beim Abbruch auf das Ende des abgebrochenen Einschubs warten; das Aufräumen bleibt selbst bei Abbruch geschützt. |
| 8 | P2 | „Erinnerungen“ hat `sprichtImmer`, nicht `vorlesen`. Der allgemeine Probe-Zweig zeigt deshalb eine Meldung, spricht aber nicht. Die neue Reglerwahl ist dort nicht hörbar probierbar. | Eigene Probe über die echte Erinnerungs-Wiedergabe mit denselben Datei-/Systemstimmenwegen; keine neue Erinnerung und kein gespeicherter Termin. |
| 9 | P3 | Der gemeinsame Hilfetext spricht immer von Medienlautstärke. Erinnerungswecker verwenden ausdrücklich den Alarm-Tonweg. | Hilfetext für Erinnerungen unterscheidet Medien- und Wecker-Lautstärke; keine Änderung der Tonwege. |

Alle neun Befunde sind im Code behoben. Die Einstufungen beschreiben konkrete Fehlerpfade und ihre Folgen, keine gemessene Auftretenshäufigkeit.

## Zusätzlich betrachtete Grenzen

- Jede der zehn gewünschten Meldungsarten nutzt den gemeinsamen Regler: Agent, Programmieren, Rückfrage, Ablage, Kontingent, Abfahrt, Timer, Erinnerung, Update-Ansage und Installationsfrage.
- Alte Schlüssel für Ansagen und Losfahren sowie `meldung_timer_lautstaerke` für die Timer-Stimme bleiben erhalten. `timer_lautstaerke` bleibt der getrennte Weckton.
- „Wie Handy“ bleibt der Wert 0; alte Werte von 1 bis 19 werden weder migriert noch beim Anzeigen überschrieben. Neue Reglerwerte sind ganzzahlig von 20 bis 100.
- Stimmen, Tempo, Vorleseschalter und Ruhezeit wurden nicht umgestellt. Der Erinnerungs-Cache benötigt bei reiner Lautstärkeänderung keine neue Synthese.
- Die gemeinsame Verwaltung ist je Tonweg synchronisiert; manueller Pegelwechsel, verschachteltes und vertauschtes Ende sowie Fehler beim Setzen/Zurückstellen wurden anhand des Kontrollflusses durchgegangen.
- Die tatsächliche Android-Systemlautstärke bleibt auf die vom Gerät angebotenen Lautstärkeschritte begrenzt. Gleichzeitige Wiedergaben auf demselben Android-Tonweg teilen dessen Systempegel, wie bisher; dieses Review führt keinen eigenen Audiomixer ein.

## Abnahme

Statische Prüfung des gesamten Sitzungsdiffs und der genannten Aufrufer. Die korrigierte Version wird mit `:app:assembleDebug` gebaut und danach über das vorgeschriebene Jarvis-Installationsskript installiert. Im Schnellmodus wurden keine automatisierten oder akustischen Gerätetests ausgeführt; Frank übernimmt die praktische Prüfung. Ein erfolgreicher Build belegt Kompilierung und Paketierung, nicht die akustische Wirkung jedes Gerätezustands.
