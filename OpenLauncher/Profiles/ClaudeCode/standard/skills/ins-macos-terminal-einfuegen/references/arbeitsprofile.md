# Arbeitsprofile: schnell, normal, gründlich

Gilt für beauftragte **Programmierung von Apps und Software** im Dreierdialog. Das Profil
legt Umfang pro Update, Plantiefe und Prüfbreite fest. Es ändert weder Autorisierung noch
Transport, Identität, Stopp oder die Abschlusskette.

## Geltung und Rangfolge

1. Verbindliche Projektpflichten aus AGENTS.md und Projektregeln sowie ausdrückliche
   Nutzerregeln (inklusive Abschlusskette, Freigaben, Version und Commit-Regeln).
2. Transport- und Identitätsschutz dieses Skills; dieselbe sichtbare Claude-Sitzung bleibt.
3. Das aktive Skill-Profil, im Auftrag an Claude genannt, in dieser Reihenfolge: die
   jüngste ausdrückliche Nutzerwahl, sonst `profil` im Zielvertrag, sonst der Startwert
   (siehe „Wahl und Wechsel“).
4. Der Text eines bestätigten OpenLauncher-Startmodus der Claude-Sitzung
   (`Profiles/WorkModes/<id>.md`, von Windows und macOS geteilt).

**Herkunft des Startmodus:** Der Mac-Launcher hängt den Modustext an die Sitzungs-CLAUDE.md
im Profilordner `~/proggs/OpenLauncher/Profiles/ClaudeCodeMac/<id>/` (`CLAUDE_CONFIG_DIR`,
Kopfzeile „# Claude-Code-Profil:“, Modusblock ab „AKTIVER ARBEITSMODUS:“) und schreibt
diese Datei bei jedem Start des Profils neu. **Bestätigt** ist ein Startmodus nur aus dem
aktuellen Startkontext der Zielsitzung: Die Datei trägt diese Kopfzeile, ihr Modusblock
stimmt nach Trimmen genau mit einer `WorkModes/<id>.md` überein, und sie ist nicht jünger
als der Start der Zielsitzung (tmux `session_created`). Fehlt ein Blockanhang und passt
`frei.md` (leer), ist der Startmodus `frei`. Sonst gilt er als unbestätigt.

Das Profil wirkt über den Auftragstext, nicht über einen Neustart: Der Launcher-Modus steht
fest in der laufenden Sitzung und wird nie durch Neustart gewechselt. Ein bestätigter
abweichender Startmodus wird für die Runde ausdrücklich übersteuert, soweit Rang 1 nichts
anderes verlangt; Auftrag und Zwischenstand nennen die Herkunft, etwa „gründlich (Nutzerwahl
R4); bestätigter Launcher-Startmodus schnell übersteuert“.

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

- **Startwert:** Ohne ausdrückliche Nutzerwahl und ohne gespeichertes `profil` bestimmt ein
  bestätigter Launcher-Startmodus `schnell`, `normal` oder `gruendlich` das anfängliche
  Profil (`gruendlich` ergibt gründlich). Bei `frei`, unbestätigtem, unbekanntem oder
  widersprüchlichem Startmodus gilt `normal`; betrifft die Unklarheit einen folgenreichen
  Schritt, vorher kurz klären. Den abgeleiteten Startwert beim Anlegen von `ziel.json` als
  `profil` speichern und im ersten Auftrag samt Herkunft nennen. Eine spätere ausdrückliche
  Nutzerwahl hat Vorrang; ein späterer Launcher-Neustart ändert ein gespeichertes `profil`
  nicht.
- **Nur eindeutige Formulierungen** wählen oder wechseln das Profil, etwa „Profil schnell“,
  „Schnellmodus“, „Normalmodus“, „Standardmodus“, „in den Gründlichkeitsmodus“, jeweils
  bezogen auf die Arbeitsweise. Bloße Dringlichkeit („mach schnell“, „zügig bitte“) oder
  Sorgfaltswünsche zu einem Einzelpunkt sind kein Wechsel. Bei echter Mehrdeutigkeit kurz
  nachfragen.
- **„Launcher-Modus“** meint ausdrücklich den Startmodus im OpenLauncher, kein Profil. Der
  Skill schaltet ihn nicht um und startet nichts neu; kurz melden, dass er erst beim nächsten
  Start der Sitzung wirkt und das Arbeitsprofil unverändert bleibt.
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

> Profil: schnell (gilt für diese Runde vor einem bestätigten Launcher-Startmodus;
> verbindliche Projektpflichten aus AGENTS.md und Projektregeln haben Vorrang).

Der Zwischenstand nennt das effektiv angewandte Profil samt Herkunft und Abweichung, zum
Beispiel „Profil: schnell (Startwert aus bestätigtem Launcher-Startmodus); Build nach
AGENTS.md“ oder „Profil: normal (Standard); Startmodus unbestätigt“. Der Koordinator prüft, dass Umfang
und Prüfbelege zum Profil passen, und stuft bei Risiko hoch.
