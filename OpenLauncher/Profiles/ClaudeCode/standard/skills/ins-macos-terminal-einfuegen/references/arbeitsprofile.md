# Arbeitsprofile: schnell, normal, gründlich

Gilt für beauftragte **Programmierung von Apps und Software** im Dreierdialog. Das Profil
legt Umfang pro Update, Plantiefe und Prüfbreite fest. Es ändert weder Autorisierung noch
Transport, Identität, Stopp oder die Abschlusskette.

## Geltung und Rangfolge

1. AGENTS.md, Projektregeln und ausdrückliche Nutzerregeln (inklusive Abschlusskette).
2. Transport- und Identitätsschutz dieses Skills; dieselbe sichtbare Claude-Sitzung bleibt.
3. Das aktive Skill-Profil, im Auftrag an Claude genannt.
4. Der OpenLauncher-Startmodus der Claude-Sitzung (`WorkModes/*.md` in deren CLAUDE.md).

Das Profil wirkt über den Auftragstext, nicht über einen Neustart: Der Launcher-Modus steht
fest in der laufenden Sitzung und wird nie durch Neustart gewechselt. Ein abweichender
Startmodus wird für die Runde ausdrücklich übersteuert, soweit Rang 1 nichts anderes verlangt.

**Effektives Profil ehrlich benennen:** Verlangt eine Regel aus Rang 1 mehr oder weniger als
das gewählte Profil (etwa Pflichtbuild und Installation trotz schnell, oder ein vom Nutzer
fest vorgegebener Modus), gilt diese Regel. Auftrag und Zwischenstand nennen dann das
effektiv angewandte Profil samt Abweichung, zum Beispiel „schnell, Build und Installation
nach AGENTS.md“.

**Wissensaustausch** (Fragen, Erklärungen, Konzeptdiskussion ohne Umsetzungsauftrag) läuft
ohne Profil: keine Programmierplanung, keine Tests, keine Lieferkette, kein Zielvertrag.
Erst ein ausdrücklicher Umsetzungsauftrag aktiviert das Profil; eine Diskussion wird nicht
still zur Umsetzung. Wissensfragen während eines laufenden Programmierloops werden
beantwortet, ohne eine neue Runde oder Lieferkette auszulösen.

## Profile

| | schnell | normal (Standard) | gründlich |
|---|---|---|---|
| Umfang pro Update | möglichst viele sinnvolle, vom Auftrag gedeckte Verbesserungen in einem zusammenhängenden Update | ein zusammenhängender Änderungssatz | kleinster nutzbarer vertikaler Teil |
| Planung | Abdeckungsliste nur bei mehreren Varianten; direkt gebündelter Auftrag, Diskussion nur bei echtem Risiko | kurzer Plan, Konsens bei Abwägungen | Plan mit Abdeckungsliste, Alternativen und Risiken vor der Umsetzung |
| Claudes Prüfung | Build plus je gebündelter Verbesserung ein knapper Wirknachweis des geänderten Pfads, soweit Rang 1 ihn zulässt oder verlangt | passende Tests, naheliegende Regressionen, relevante Randfälle | vollständige relevante Tests, Randfälle, Aufrufer, verwandte Fehlerklassen, Gerätepfad |
| Mindest-`review_depth` des Koordinators | compact | compact, nach Risiko targeted | targeted, nach Risiko full |
| Advisor Fable | bestehende Auslöser | bestehende Auslöser | bestehende Auslöser, zusätzlich folgenreiche Architekturentscheidungen mit echter Unsicherheit |

**Prüfumfang und Stoppkriterium, alle Profile:** Das Profil bestimmt die Breite, das Risiko
die Mindesttiefe. Eine nach Risiko, Projektregel oder Kriterium erforderliche Prüfung
entfällt nie wegen des Profils oder einer Zahl. Wiederholt wird nur nach einem konkreten
Fehler, einer neuen Änderung an den geprüften Pfaden oder einem benannten Risiko, nicht
routinemäßig. Die Prüfung ist beendet, wenn alle erforderlichen Prüfungen für den
aktuellen Stand grün sind und kein Befund offen ist. Nach zwei Fehlversuchen ohne neue
Evidenz Strategiewechsel gemäß [Programmierloop](../../ins-terminal-einfuegen/references/programmier-loop.md);
ein Blocker wird gemeldet, nicht durch weitere Wiederholungen verdeckt.

**Schnell heißt Menge bei guter Qualität:** tatsächlich implementierte, nutzerwirksame
Verbesserungen innerhalb des Auftrags. Keine rein kosmetischen Änderungen, keine
Scheinfunktionen und keine halben Umbauten, um die Menge zu erhöhen. Ein begrenzt
riskanter, klar abgrenzbarer Kandidat darf nach kurzer, risikogerechter Absicherung ins
Bündel; `full`-Auslöser gelten dabei unverändert. Zurückgestellt wird nur, was sich im
verfügbaren Update nicht sauber implementieren und angemessen absichern lässt oder nicht
eindeutig vom Auftrag gedeckt ist; es kommt als Vorschlag in den Bericht, statt das
Bündel zu blockieren. Der Zwischenstand listet umgesetzte Verbesserungen je mit Beleg und
zurückgestellte getrennt. Mehr Menge rechtfertigt kein Absenken der Kriterien.

## In jedem Profil unverändert

- Abschlusskette nach Projektregel: Die geltenden Nutzer- und Projektregeln bestimmen, welche
  Schritte für die Art der Änderung Pflicht sind. Bei Codeänderungen vollständig: Build,
  Versionsbump mit echter Zeit, Commit, Rebase/Push, autorisierte Installation
  beziehungsweise Deployment.
- Die `full`-Auslöser aus
  [Kontingent sparsam nutzen](../../ins-terminal-einfuegen/references/kontingent-sparen.md)
  gelten immer; ebenso „Kriterien nie absenken“ und das Stoppkriterium oben.
- Review-Checkpoint, Steuerdatei, `STOP`, Zustellungsledger und `submit`-Prüfungen.

## Wahl und Wechsel

- **Standard:** `normal`, soweit weder der Nutzer noch eine höherrangige Regel oder der
  Zielvertrag (`profil`) ein anderes Profil festlegt.
- **Nur eindeutige Formulierungen** wählen oder wechseln das Profil, etwa „Profil schnell“,
  „Schnellmodus“, „Standardmodus“, „in den Gründlichkeitsmodus“. Bloße Dringlichkeit
  („mach schnell“, „zügig bitte“) oder Sorgfaltswünsche zu einem Einzelpunkt sind kein
  Wechsel. Bei echter Mehrdeutigkeit kurz nachfragen.
- Ein Wechsel ist eine verbindliche Nutzerkorrektur: `profil` in `ziel.json` setzen,
  `ziel_rev` erhöhen, Steuerdatei atomar ersetzen. Das Profil steht im Zielvertrag,
  nicht in `arbeitsstand.json` (dessen fünf Schlüssel bleiben). `resume` liefert den
  Zielvertrag nicht mit: Nach Kontextverdichtung `ziel.json` einmal lesen, bevor der
  nächste Umsetzungsauftrag ein Profil nennt. Ohne Zielvertrag bleibt das Profil im
  flüchtigen Arbeitsstand und steht in jedem Auftrag.
- Wirksam ab der **nächsten sicheren Runde**, zugestellt am nächsten freien Prompt, nie in
  eine laufende Generation. Eine laufende oder bereits geprüfte Runde behält die erreichte
  Prüftiefe bis zu ihrem Abschluss. Eine spätere neue Runde arbeitet nach dem ausdrücklich
  gewählten schwächeren Profil, soweit Risiko und Projektregeln es erlauben. Den Wechsel
  knapp bestätigen: „Profil ab R5: schnell“.

## Im Auftrag an Claude

Jeder Umsetzungsauftrag nennt eine Profilzeile, zum Beispiel:

> Profil: schnell (gilt für diese Runde vor deinem Startmodus; AGENTS.md und Projektregeln
> haben Vorrang).

Der Zwischenstand nennt das effektiv angewandte Profil. Der Koordinator prüft, dass Umfang
und Prüfbelege zum Profil passen, und stuft bei Risiko hoch.
